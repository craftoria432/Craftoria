# 🔴 CRITICAL SECURITY AUDIT - DEPLOYMENT BLOCKERS

## Executive Summary
**Status:** ⚠️ **CRITICAL ISSUES FOUND** - Do NOT deploy to production until these are resolved.

Three security vulnerabilities have been identified that could allow unauthorized access, data manipulation, and payment fraud.

---

## 🔴 ISSUE #1: Payment Creation Happens on Android Client (CRITICAL)

### The Problem
```kotlin
// CheckoutViewModel.kt - Line 223
val result = retryManager.executeWithRetry(maxRetries = 3) {
    paymentRepository.processOrderPaymentsWithIdempotency(order, idempotencyKey)
}
```

**This is called from the Android app**, not from Cloud Functions.

### Why This Is Critical
1. **Firestore Rule:** `allow write: if false` on `seller_payments` collection
2. **What happens:** Every payment creation from the app will silently fail with a permission denied error
3. **User Impact:** Buyers think their order succeeded, but no payment record is created
4. **Seller Impact:** Sellers never receive payment notifications
5. **Business Impact:** Orders are placed but payments are never recorded

### The Fix
**Move to Cloud Functions (REQUIRED)**

⚠️ **CRITICAL: Idempotency Key Handling**

When moving to Cloud Functions, the idempotency key becomes MORE important, not less:
- Firestore triggers automatically retry on failure
- Without idempotency enforcement, a failed payment creation that retries could create duplicate payment records
- The Cloud Function MUST receive the idempotency key from the order and enforce it

```javascript
// functions/index.js
exports.onOrderCreated = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const order = snap.data();
    const orderId = context.params.orderId;
    
    try {
      // ✅ Check for existing payments with this idempotency key
      const existing = await db.collection("seller_payments")
        .where("order_id", "==", orderId)
        .where("idempotency_key", "==", order.idempotencyKey)
        .get();
      
      if (!existing.empty) {
        console.log(`✅ Idempotent: Payments already exist for order ${orderId}`);
        return;
      }
      
      // ✅ Server-side payment processing
      const paymentIds = await processOrderPayments(order);
      
      // ✅ Store idempotency key on each payment
      for (const paymentId of paymentIds) {
        await db.collection("seller_payments").doc(paymentId).update({
          idempotency_key: order.idempotencyKey,
          request_id: admin.firestore.FieldValue.serverTimestamp()
        });
      }
      
      // Send notifications
      await notifySellerOfPayment(order.sellerId, paymentIds);
    } catch (error) {
      console.error(`❌ Error processing payments for order ${orderId}:`, error);
      // Firestore will retry automatically
      throw error;
    }
  });
```

⚠️ **SEPARATE CALLABLE FUNCTION FOR REPLAY**

For failed orders that need payment replay:
```javascript
exports.replayOrderPayments = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'User not authenticated');
  
  const { orderId } = data;
  const order = await db.collection("orders").doc(orderId).get();
  
  if (!order.exists) throw new functions.https.HttpsError('not-found', 'Order not found');
  
  // Only admin or the seller can replay
  if (context.auth.uid !== order.data().sellerId && !isAdmin(context.auth.uid)) {
    throw new functions.https.HttpsError('permission-denied', 'Not authorized');
  }
  
  // Delete old payments for this order
  const oldPayments = await db.collection("seller_payments")
    .where("order_id", "==", orderId)
    .get();
  
  for (const doc of oldPayments.docs) {
    await doc.ref.delete();
  }
  
  // Reprocess
  const paymentIds = await processOrderPayments(order.data());
  return { success: true, paymentIds };
});
```

### Verification Steps
1. ✅ Create `onOrderCreated` trigger with idempotency enforcement
2. ✅ Create `replayOrderPayments` callable function
3. ✅ Remove payment creation from `CheckoutViewModel`
4. ✅ Test: Place an order and verify payment record is created
5. ✅ Test: Verify seller receives payment notification
6. ✅ Test: Simulate trigger retry and verify no duplicate payments
7. ✅ Test: Call replay function and verify old payments are deleted

---

## 🔴 ISSUE #2: Orders Update Rule Allows Field Manipulation (CRITICAL)

### The Problem
```firestore
allow update: if isAuthenticated() && 
  (request.auth.uid == resource.data.buyer_id ||
   request.auth.uid == resource.data.seller_id ||
   isAdmin());
```

**This allows either party to update ANY field on an order.**

### Attack Scenarios
1. **Buyer modifies seller_id:** Buyer changes order to point to a different seller, then requests refund
2. **Buyer modifies amount:** Buyer reduces order total, pays less, seller gets less
3. **Buyer modifies status:** Buyer marks order as "delivered" without paying
4. **Seller modifies buyer_id:** Seller reassigns order to a different buyer

### The Fix - Field-Level Access Control

⚠️ **CRITICAL: Status Transitions Must Be Constrained**

The proposed fix allows buyers to update status freely. This is dangerous — a buyer being able to set status = "delivered" or status = "completed" themselves is as dangerous as modifying amount.

**Solution: Restrict status values AND use Cloud Functions for transitions**

```firestore
allow update: if isAuthenticated() && 
  (request.auth.uid == resource.data.buyer_id ||
   request.auth.uid == resource.data.seller_id ||
   isAdmin()) &&
  // ✅ Buyer can only update: delivery_address, notes, and ONLY cancel status
  (request.auth.uid == resource.data.buyer_id && 
   request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'delivery_address', 'notes', 'updated_at']) &&
   // ✅ Buyer can ONLY set status to 'cancelled'
   (request.resource.data.status == resource.data.status || request.resource.data.status == 'cancelled') ||
  // ✅ Seller can only update: status, tracking_number, notes
   request.auth.uid == resource.data.seller_id && 
   request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status', 'tracking_number', 'notes', 'updated_at']) &&
   // ✅ Seller can only transition through valid states
   (request.resource.data.status in ['processing', 'shipped', 'delivered'] ||
    request.resource.data.status == resource.data.status) ||
  // ✅ Admin can update anything
   isAdmin());
```

**Better: Use Cloud Functions for Status Transitions**

```javascript
// functions/index.js
exports.updateOrderStatus = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'User not authenticated');
  
  const { orderId, newStatus } = data;
  const userId = context.auth.uid;
  
  const order = await db.collection("orders").doc(orderId).get();
  if (!order.exists) throw new functions.https.HttpsError('not-found', 'Order not found');
  
  const orderData = order.data();
  const validTransitions = {
    'pending': ['processing', 'cancelled'],
    'processing': ['shipped', 'cancelled'],
    'shipped': ['delivered'],
    'delivered': [],
    'cancelled': []
  };
  
  // ✅ Verify user is buyer or seller
  const isBuyer = userId === orderData.buyer_id;
  const isSeller = userId === orderData.seller_id;
  
  if (!isBuyer && !isSeller) {
    throw new functions.https.HttpsError('permission-denied', 'Not involved in this order');
  }
  
  // ✅ Verify status transition is valid
  if (!validTransitions[orderData.status]?.includes(newStatus)) {
    throw new functions.https.HttpsError('invalid-argument', 
      `Cannot transition from ${orderData.status} to ${newStatus}`);
  }
  
  // ✅ Verify role can make this transition
  if (isBuyer && newStatus !== 'cancelled') {
    throw new functions.https.HttpsError('permission-denied', 'Buyer can only cancel orders');
  }
  
  if (isSeller && !['processing', 'shipped', 'delivered'].includes(newStatus)) {
    throw new functions.https.HttpsError('permission-denied', 'Seller cannot set this status');
  }
  
  // ✅ Update order
  await db.collection("orders").doc(orderId).update({
    status: newStatus,
    updated_at: admin.firestore.FieldValue.serverTimestamp()
  });
  
  return { success: true };
});
```

### Verification Steps
1. ✅ Update Firestore rules with field-level access control
2. ✅ Create Cloud Function for status transitions
3. ✅ Test: Buyer tries to modify seller_id → Should fail
4. ✅ Test: Buyer tries to modify amount → Should fail
5. ✅ Test: Buyer can update delivery_address → Should succeed
6. ✅ Test: Buyer tries to set status to 'delivered' → Should fail
7. ✅ Test: Buyer can set status to 'cancelled' → Should succeed
8. ✅ Test: Seller can set status to 'shipped' → Should succeed
9. ✅ Test: Seller tries to set status to 'cancelled' → Should fail

---

## 🔴 ISSUE #3: isAdmin() Implementation Uses User-Controlled Data (CRITICAL)

### The Problem
```firestore
function isAdmin() {
  return isAuthenticated() && 
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
}
```

**This reads the `role` field from the user's own document.**

### Attack Scenario
1. User creates account with role = 'USER'
2. User modifies their own user document to set role = 'ADMIN'
3. User now has admin access to all protected resources

### The Fix - Firebase Custom Claims (Server-Side Only)

⚠️ **CRITICAL: Admin Claims Must Be Immutable**

Custom claims are set by Cloud Functions, not the client. They cannot be modified by the user and are verified by Firebase on every request.

**Step 1: Create Admin Management Functions**

```javascript
// functions/index.js

// ✅ Promote user to admin (only existing admins can do this)
exports.promoteUserToAdmin = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'User not authenticated');
  
  // ✅ Verify caller is already an admin
  const callerClaims = context.auth.token;
  if (!callerClaims.admin) {
    throw new functions.https.HttpsError('permission-denied', 'Only admins can promote users');
  }
  
  const { userId } = data;
  
  // ✅ Set custom claim server-side (cannot be modified by client)
  await admin.auth().setCustomUserClaims(userId, { admin: true });
  
  // ✅ Log the action to audit collection
  await db.collection("admin_audit_logs").add({
    action: 'promote_to_admin',
    targetUserId: userId,
    performedBy: context.auth.uid,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
    ipAddress: context.rawRequest.ip
  });
  
  return { success: true, message: `User ${userId} promoted to admin` };
});

// ✅ Demote admin (only existing admins can do this)
exports.demoteAdmin = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError('unauthenticated', 'User not authenticated');
  
  // ✅ Verify caller is already an admin
  const callerClaims = context.auth.token;
  if (!callerClaims.admin) {
    throw new functions.https.HttpsError('permission-denied', 'Only admins can demote admins');
  }
  
  const { userId } = data;
  
  // ✅ Prevent self-demotion
  if (userId === context.auth.uid) {
    throw new functions.https.HttpsError('invalid-argument', 'Cannot demote yourself');
  }
  
  // ✅ Remove custom claim server-side
  await admin.auth().setCustomUserClaims(userId, { admin: false });
  
  // ✅ Log the action to audit collection
  await db.collection("admin_audit_logs").add({
    action: 'demote_admin',
    targetUserId: userId,
    performedBy: context.auth.uid,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
    ipAddress: context.rawRequest.ip
  });
  
  return { success: true, message: `User ${userId} demoted from admin` };
});
```

**Step 2: Update Firestore Rules**

```firestore
function isAdmin() {
  return isAuthenticated() && request.auth.token.admin == true;
}
```

**Step 3: Update Admin Audit Logs Rule**

```firestore
match /admin_audit_logs/{logId} {
  allow read: if isAdmin();
  allow write: if false; // Only Cloud Functions can write
}
```

### Why This Works
- Custom claims are set by Cloud Functions, not the client
- Client cannot modify their own custom claims
- Custom claims are verified by Firebase on every request
- All admin changes are logged to audit collection
- Audit logs are immutable (only Cloud Functions can write)

⚠️ **IMPORTANT: Token Refresh Timing**

After setting custom claims, existing sessions won't see the new claim until the user's ID token refreshes (up to 1 hour). Factor this into your admin onboarding flow:

```kotlin
// Android - After promoting user to admin
// Force token refresh
val user = FirebaseAuth.getInstance().currentUser
user?.getIdToken(true)?.addOnSuccessListener { result ->
    // New token now has admin claim
    Log.d("Admin", "Token refreshed with new claims")
}
```

### Verification Steps
1. ✅ Create `promoteUserToAdmin` Cloud Function
2. ✅ Create `demoteAdmin` Cloud Function
3. ✅ Update Firestore rules to use `request.auth.token.admin`
4. ✅ Test: Regular user tries to read admin_activities → Should fail
5. ✅ Test: Admin user can read admin_activities → Should succeed
6. ✅ Test: User tries to modify their own role field → Should not grant admin access
7. ✅ Test: Promote user to admin and verify audit log is created
8. ✅ Test: Demote admin and verify audit log is created
9. ✅ Test: Verify self-demotion is prevented
10. ✅ Test: Force token refresh after promotion and verify new claims are visible

---

## ⚠️ ISSUE #4: Commission Query Missing Composite Indexes

### The Problem

⚠️ **CRITICAL: Two Separate Queries, Two Separate Indexes**

The commission system has TWO different queries that each need their own composite index:

**Query 1: Get commissions by seller (with status filter)**
```kotlin
// CommissionRepository.kt - getCommissionsBySeller()
val snapshot = db.collection("admin_commissions")
  .whereEqualTo("seller_id", sellerId)
  .whereEqualTo("status", "pending")
  .orderBy("created_at", Query.Direction.DESCENDING)
  .get()
  .await()
```
**Requires Index:** `seller_id` (Asc) + `status` (Asc) + `created_at` (Desc)

**Query 2: Get pending commissions (no seller filter)**
```kotlin
// CommissionRepository.kt - getPendingCommissions()
val snapshot = db.collection("admin_commissions")
  .whereEqualTo("status", "pending")
  .orderBy("created_at", Query.Direction.DESCENDING)
  .get()
  .await()
```
**Requires Index:** `status` (Asc) + `created_at` (Desc)

### The Fix

**Verify firestore.indexes.json has BOTH indexes:**

```json
{
  "indexes": [
    {
      "collectionGroup": "admin_commissions",
      "queryScope": "Collection",
      "fields": [
        { "fieldPath": "seller_id", "order": "ASCENDING" },
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "created_at", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "admin_commissions",
      "queryScope": "Collection",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "created_at", "order": "DESCENDING" }
      ]
    }
  ]
}
```

### Verification Steps
1. ✅ Check `firestore.indexes.json` has BOTH indexes
2. ✅ Deploy indexes: `firebase deploy --only firestore:indexes`
3. ✅ Wait for indexes to build (usually 5-10 minutes)
4. ✅ Test: Query by seller_id + status + orderBy created_at → Should work
5. ✅ Test: Query by status + orderBy created_at → Should work
6. ✅ Verify in Firebase Console: Firestore → Indexes → All indexes show "Enabled"

---

## 📋 Pre-Deployment Checklist

### Critical (Must Fix Before Deployment)
- [ ] **Issue #1:** Move payment creation to Cloud Functions
  - [ ] Create `onOrderCreated` trigger in Cloud Functions
  - [ ] Remove payment creation from `CheckoutViewModel`
  - [ ] Test: Place order and verify payment is created
  - [ ] Test: Verify seller receives notification

- [ ] **Issue #2:** Add field-level access control to orders
  - [ ] Update Firestore rules with `affectedKeys()` checks
  - [ ] Test: Buyer cannot modify seller_id
  - [ ] Test: Buyer cannot modify amount
  - [ ] Test: Seller cannot modify buyer_id

- [ ] **Issue #3:** Implement admin claims
  - [ ] Create Cloud Function to set admin claims
  - [ ] Update Firestore rules to use `request.auth.token.admin`
  - [ ] Test: Regular user cannot access admin resources
  - [ ] Test: Admin user can access admin resources

### Important (Should Fix Before Deployment)
- [ ] **Issue #4:** Deploy composite indexes
  - [ ] Verify `firestore.indexes.json` has all required indexes
  - [ ] Deploy: `firebase deploy --only firestore:indexes`
  - [ ] Wait for indexes to build
  - [ ] Test: Commission queries work without errors

### Staging Test (Before Production)
- [ ] Create fresh test account with no special claims
- [ ] Verify account cannot read another seller's payments
- [ ] Verify account cannot modify orders
- [ ] Verify account cannot access admin resources
- [ ] Place test order and verify payment is created
- [ ] Verify seller receives payment notification

---

## 🚀 Deployment Timeline

**Estimated Time to Fix:** 2-3 hours

1. **Hour 1:** Fix Issue #1 (Payment Creation)
   - Move logic to Cloud Functions
   - Test thoroughly

2. **Hour 2:** Fix Issue #2 & #3 (Security Rules & Admin Claims)
   - Update Firestore rules
   - Create admin promotion function
   - Test all scenarios

3. **Hour 3:** Fix Issue #4 (Indexes) + Staging Test
   - Deploy indexes
   - Run comprehensive staging tests
   - Verify all security controls

**Then:** Deploy to production with confidence ✅

---

## Questions?

If you need clarification on any of these issues, let me know. These are critical for production security.
