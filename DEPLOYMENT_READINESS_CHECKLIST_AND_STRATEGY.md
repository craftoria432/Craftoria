# Deployment Readiness Checklist & Long-Term Strategy

**Date:** May 20, 2026  
**Status:** Pre-deployment verification guide  
**Priority:** Critical for production launch

---

## Pre-Deployment Checklist (Before Going Live)

### ✅ CRITICAL: Firestore Security Rules Verification

**What to verify:**
Your Firestore security rules must enforce the same access control that your Kotlin code assumes. If rules are permissive, users can bypass the app and read other sellers' payments directly.

**Current code assumes:**
- Sellers can only read their own payments
- Buyers can only read their own payments
- Co-seller members can only read their store's payments
- Admins have elevated access

**Verification steps:**

1. **Check seller_payments collection rules:**
   ```
   ❌ BAD (too permissive):
   match /seller_payments/{document=**} {
     allow read, write: if request.auth != null;
   }
   
   ✅ GOOD (enforces access control):
   match /seller_payments/{paymentId} {
     allow read: if 
       request.auth.uid == resource.data.seller_id ||
       request.auth.uid == resource.data.buyer_id ||
       request.auth.uid in resource.data.involved_seller_ids ||
       isAdmin(request.auth.uid);
     
     allow write: if isAdmin(request.auth.uid);
   }
   ```

2. **Test with curl or Firestore emulator:**
   ```bash
   # Try to read another seller's payment as a different user
   # Should fail with permission denied
   ```

3. **Verify all payment-related collections:**
   - seller_payments
   - commissions
   - refunds
   - payment_splits (if separate collection)

**Risk if not done:**
- Determined user could read all sellers' payment data
- Buyer could see other buyers' payment history
- Commission data could be exposed
- **Data breach + regulatory violation**

**Effort:** 30 minutes  
**Blocker:** YES — do not deploy without this

---

### ✅ CRITICAL: Firestore Composite Indexes

**What to create:**
Real-time listeners use compound queries that require composite indexes. Without them, Firestore will reject queries in production with a vague error and a link to create the index.

**Queries that need indexes:**

1. **seller_payments collection:**
   - Index 1: `seller_id` + `created_at` (descending)
   - Index 2: `co_seller_store_id` + `created_at` (descending)
   - Index 3: `buyer_id` + `created_at` (descending)

2. **Why these specific indexes:**
   ```kotlin
   // BuyerPaymentViewModel real-time listener
   db.collection("seller_payments")
       .whereEqualTo("buyer_id", buyerId)
       .addSnapshotListener { ... }  // ← Needs buyer_id index
   
   // CoSellerStorePaymentRepository real-time listener
   db.collection("seller_payments")
       .whereEqualTo("co_seller_store_id", storeId)
       .orderBy("created_at", Query.Direction.DESCENDING)
       .addSnapshotListener { ... }  // ← Needs co_seller_store_id + created_at index
   ```

**How to create indexes:**

**Option A: Firebase Console (manual)**
1. Go to Firebase Console → Firestore Database → Indexes
2. Click "Create Index"
3. Collection: `seller_payments`
4. Fields:
   - `seller_id` (Ascending)
   - `created_at` (Descending)
5. Repeat for other two indexes

**Option B: Deploy via firestore.indexes.json (automated)**
```json
{
  "indexes": [
    {
      "collectionGroup": "seller_payments",
      "queryScope": "Collection",
      "fields": [
        { "fieldPath": "seller_id", "order": "ASCENDING" },
        { "fieldPath": "created_at", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "seller_payments",
      "queryScope": "Collection",
      "fields": [
        { "fieldPath": "co_seller_store_id", "order": "ASCENDING" },
        { "fieldPath": "created_at", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "seller_payments",
      "queryScope": "Collection",
      "fields": [
        { "fieldPath": "buyer_id", "order": "ASCENDING" },
        { "fieldPath": "created_at", "order": "DESCENDING" }
      ]
    }
  ]
}
```

Then deploy:
```bash
firebase deploy --only firestore:indexes
```

**Risk if not done:**
- Real-time listeners will fail silently or with cryptic errors
- Payment screens won't load in production
- Users see blank screens or "Error loading payments"
- **Production outage**

**Effort:** 15 minutes  
**Blocker:** YES — do not deploy without this

---

## Sprint Prioritization (Next Sprint)

### Priority 1: Gap 2 - CommissionRepository Safe Deserialization

**Why first:**
- Timestamp crash is a **certainty** if web dashboard or admin tools write `Timestamp.now()`
- Silent failure: commission tracking breaks with no obvious cause
- Affects financial data (commissions)

**Effort:** Medium (2-3 hours)

**Implementation:**
1. Create `CommissionRepository.parseCommission()` function (copy pattern from `PaymentRepository.parsePayment()`)
2. Update AdminCommission model: `createdAt: Any?` and `updatedAt: Any?`
3. Add helper functions: `getCreatedAtLong()`, `getUpdatedAtLong()`
4. Replace all `toObject()/toObjects()` calls with `parseCommission()`
5. Test with mixed timestamp types

**Files to modify:**
- `app/src/main/java/com/gcuf/craftoria/data/model/CommissionModels.kt`
- `app/src/main/java/com/gcuf/craftoria/data/repository/CommissionRepository.kt`

---

### Priority 2: Gap 3 - updatePaymentSplitStatus() Race Condition

**Why second:**
- Requires specific scenario (two admins, same payment, same moment)
- Lower probability than timestamp crash
- Can be addressed after Gap 2

**Effort:** Medium (2-3 hours)

**Implementation:**
1. Wrap read-modify-write in Firestore transaction
2. Use `db.runTransaction { transaction → ... }`
3. Read payment within transaction
4. Modify splits in memory
5. Write back within transaction (atomic)
6. Add unit tests for concurrent scenarios

**Files to modify:**
- `app/src/main/java/com/gcuf/craftoria/data/repository/CoSellerStorePaymentRepository.kt`

---

## Long-Term Strategy: Timestamp Standardization

### The Root Cause

Mixed timestamp formats have caused the most trouble across the codebase:
- Android writes epoch milliseconds (Long)
- Web dashboard might write Firestore Timestamps
- Admin SDK might write Timestamps
- Migrations might use different formats
- Result: Deserialization crashes, silent failures, data corruption

### Team Rule: Establish Timestamp Standard

**Rule: All Firestore writes from any platform must use epoch milliseconds as Long**

**Implementation:**

**Android (Kotlin):**
```kotlin
// ✅ CORRECT
val timestamp = System.currentTimeMillis()  // Long
db.collection("payments").document(id).set(mapOf(
    "created_at" to timestamp
))

// ❌ WRONG
val timestamp = Timestamp.now()  // Firestore Timestamp
db.collection("payments").document(id).set(mapOf(
    "created_at" to timestamp
))
```

**Web (JavaScript/Node):**
```javascript
// ✅ CORRECT
const timestamp = Date.now();  // milliseconds
await db.collection('payments').doc(id).set({
    created_at: timestamp
});

// ❌ WRONG
const timestamp = admin.firestore.Timestamp.now();
await db.collection('payments').doc(id).set({
    created_at: timestamp
});
```

**Admin SDK (Node):**
```javascript
// ✅ CORRECT
const timestamp = Date.now();
await admin.firestore().collection('payments').doc(id).set({
    created_at: timestamp
});

// ❌ WRONG
const timestamp = admin.firestore.Timestamp.now();
```

### Benefits

1. **Eliminates defensive code:** No need for `Any?` types or complex conversion helpers
2. **Simplifies deserialization:** Direct `Long` type, no type checking
3. **Prevents crashes:** No mixed format surprises
4. **Easier migrations:** Consistent format across all platforms
5. **Better performance:** No runtime type checking

### Implementation Steps

1. **Document the rule:**
   - Add to team coding standards
   - Include in code review checklist
   - Add to onboarding docs

2. **Audit existing code:**
   - Search for `Timestamp.now()` across all platforms
   - Replace with `Date.now()` or `System.currentTimeMillis()`
   - Verify all writes use Long

3. **Update migrations:**
   - Any data migration scripts must convert to Long
   - Test with sample data before running

4. **Code review checklist:**
   ```
   ☐ All timestamp writes use epoch milliseconds (Long)
   ☐ No Firestore Timestamp objects written to database
   ☐ Deserialization assumes Long type
   ☐ No defensive Any? types needed for timestamps
   ```

---

## Pre-Deployment Verification Checklist

### Security
- [ ] Firestore security rules reviewed and tested
- [ ] Rules enforce seller/buyer/admin access control
- [ ] No permissive `allow read: if request.auth != null` rules
- [ ] Tested with curl/emulator to verify access denial

### Performance
- [ ] Firestore composite indexes created for all real-time queries
- [ ] Indexes deployed to production
- [ ] Real-time listeners tested in staging environment
- [ ] No "index not found" errors in logs

### Code Quality
- [ ] All 6 critical issues resolved and verified
- [ ] Zero compilation errors
- [ ] All critical pattern rules enforced
- [ ] Code review completed

### Data Integrity
- [ ] Timestamp format standardized (Long, not Firestore Timestamp)
- [ ] All deserialization uses safe parsing functions
- [ ] No direct `toObject()` calls on payment/commission models
- [ ] Test data includes mixed timestamp scenarios

### Testing
- [ ] Payment flow tested end-to-end
- [ ] Real-time updates tested with multiple clients
- [ ] Refund workflow tested
- [ ] Commission calculations verified
- [ ] Access control tested (seller can't see other seller's payments)

### Deployment
- [ ] Staging environment mirrors production
- [ ] All tests pass in staging
- [ ] Rollback plan documented
- [ ] Monitoring/alerting configured
- [ ] On-call support ready

---

## Post-Deployment Monitoring

### Critical Metrics to Watch

1. **Real-time listener errors:**
   - Monitor for "index not found" errors
   - Monitor for permission denied errors
   - Alert if error rate > 0.1%

2. **Deserialization failures:**
   - Monitor for timestamp conversion errors
   - Monitor for type mismatch errors
   - Alert if any occur

3. **Payment data integrity:**
   - Monitor for missing payments
   - Monitor for duplicate payments
   - Monitor for incorrect amounts

4. **Commission calculations:**
   - Monitor for calculation errors
   - Monitor for missing commissions
   - Verify totals match expected values

### Rollback Triggers

Rollback immediately if:
- Real-time listeners fail for >5% of users
- Payment deserialization errors occur
- Access control violations detected
- Commission calculations incorrect

---

## Summary

### Before Deployment
1. ✅ Verify Firestore security rules (30 min) — **BLOCKER**
2. ✅ Create composite indexes (15 min) — **BLOCKER**
3. ✅ Run full test suite
4. ✅ Verify all 6 critical issues resolved

### Next Sprint
1. Apply safe deserialization to CommissionRepository (Gap 2)
2. Add transaction support to updatePaymentSplitStatus() (Gap 3)

### Long-Term
1. Establish timestamp standardization rule across all platforms
2. Audit and migrate existing code to use Long timestamps
3. Update code review checklist to enforce rule
4. Remove defensive `Any?` types as codebase standardizes

### Current Status
- ✅ 6 critical issues resolved
- ✅ Zero compilation errors
- ✅ Core payment flow production-ready
- ⏳ Awaiting security rules verification and index creation
- ⏳ Awaiting deployment

**Estimated time to deployment:** 1-2 hours (security rules + indexes)

---

## Contact & Escalation

If issues arise during deployment:
1. Check real-time listener logs for index errors
2. Verify security rules are deployed
3. Check Firestore quota/limits
4. Review access control logs for permission denials
5. Escalate to database team if needed

**You're in a solid position. The core payment flow is well-structured. Just verify the two pre-deployment items and you're ready to go live.**
