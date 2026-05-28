# 🔧 Refund System Complete Fix

## 📋 Issue Summary

**Problem**: Buyer (who was rejected seller) cannot:
1. See payment history
2. Submit refund requests
3. Gets "Unauthorized: Not involved in this order" error

**Root Cause**: 
- Authorization logic doesn't properly handle users with multiple roles
- Refund routing unclear (should go to seller AND admin)
- Payment visibility restricted incorrectly

---

## 🎯 Solution Overview

### **Refund Flow Decision**:
```
Buyer Initiates Refund
        ↓
   Status: REQUESTED
        ↓
    ┌───────────────┐
    │  Notification │
    │   Sent To:    │
    │  • Seller     │ ← Seller sees refund request
    │  • Admin      │ ← Admin can approve/reject
    └───────────────┘
        ↓
Admin Reviews & Approves
        ↓
   Status: APPROVED
        ↓
System Processes Payment
        ↓
   Status: COMPLETED
```

**Answer**: Refund goes to **BOTH seller AND admin**
- Seller is notified (transparency)
- Admin has approval authority
- System processes after admin approval

---

## 🔨 Implementation Fixes

### **Fix 1: Payment Repository Authorization**

**File**: `app/src/main/java/com/gcuf/craftoria/data/repository/PaymentRepository.kt`

**Problem**: `getOrderPayments()` only checks if user is seller, not buyer

**Solution**:
```kotlin
suspend fun getOrderPayments(
    orderId: String,
    requestingUserId: String
): Result<List<SellerPayment>> {
    return try {
        // ✅ FIX: First verify user is involved in this order
        val orderResult = orderRepository.getOrderById(orderId)
        if (orderResult.isFailure) {
            return Result.failure(Exception("Order not found"))
        }
        
        val order = orderResult.getOrNull()!!
        
        // ✅ CRITICAL: Check if user is BUYER or SELLER
        val isAuthorized = order.buyerId == requestingUserId || 
                          order.sellerId == requestingUserId ||
                          order.items.any { it.sellerId == requestingUserId }
        
        if (!isAuthorized) {
            return Result.failure(Exception("Unauthorized: Not involved in this order"))
        }

        // Fetch payments
        val snapshot = firestore.collection(PAYMENTS_COLLECTION)
            .whereEqualTo("order_id", orderId)
            .get()
            .await()

        val payments = snapshot.documents.mapNotNull { 
            it.toObject(SellerPayment::class.java) 
        }

        Result.success(payments)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching order payments", e)
        Result.failure(e)
    }
}
```

---

### **Fix 2: Buyer Payment History Screen**

**File**: `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/PaymentHistoryScreen.kt`

**Problem**: Screen might not be fetching buyer's payment records correctly

**Solution**: Ensure proper query
```kotlin
// In BuyerPaymentViewModel
fun loadBuyerPayments(buyerId: String) {
    viewModelScope.launch {
        try {
            _isLoading.value = true
            
            // ✅ Query payments where buyer_id matches
            val snapshot = firestore.collection("seller_payments")
                .whereEqualTo("buyer_id", buyerId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val payments = snapshot.documents.mapNotNull { 
                it.toObject(SellerPayment::class.java) 
            }

            _payments.value = payments
            _isLoading.value = false
        } catch (e: Exception) {
            _errorMessage.value = e.message
            _isLoading.value = false
        }
    }
}
```

---

### **Fix 3: Web Dashboard Refund Service**

**File**: `src/services/refundService.js`

**Add notification to both seller and admin**:

```javascript
export const createRefund = async (refundData) => {
  try {
    const {
      order_id,
      payment_id,
      buyer_id,
      buyer_name,
      seller_id,
      seller_name,
      refund_type,
      original_amount,
      refund_amount,
      reason,
      reason_details,
      payment_method,
      transaction_id,
      initiated_by,
    } = refundData;

    // Validate required fields
    if (!order_id || !buyer_id || !seller_id || !refund_amount) {
      throw new Error('Missing required refund fields');
    }

    // Create refund document
    const refundDoc = {
      order_id,
      payment_id,
      buyer_id,
      buyer_name,
      seller_id,
      seller_name,
      refund_type: refund_type || 'FULL',
      original_amount,
      refund_amount,
      reason,
      reason_details,
      payment_method,
      transaction_id,
      initiated_by,
      status: 'requested',
      requested_at: serverTimestamp(),
      created_at: serverTimestamp(),
      updated_at: serverTimestamp(),
      audit_trail: [{
        action: 'requested',
        actor: buyer_id,
        actor_name: buyer_name,
        notes: `Refund requested by ${initiated_by}`,
        timestamp: Date.now(),
      }],
      retry_count: 0,
      retry_attempts: [],
    };

    // Add to Firestore
    const docRef = await addDoc(collection(db, 'refunds'), refundDoc);

    // ✅ FIX: Notify BOTH seller AND admin
    await notifyRefundRequested({
      refund_id: docRef.id,
      order_id,
      buyer_id,
      buyer_name,
      seller_id,
      seller_name,
      refund_amount,
      reason,
      payment_id,
    });

    // ✅ NEW: Notify admin for approval
    await notifyAdminPendingRefund({
      refund_id: docRef.id,
      order_id,
      buyer_name,
      refund_amount,
      reason,
      payment_id,
    });

    return { success: true, refund_id: docRef.id };
  } catch (error) {
    console.error('Error creating refund:', error);
    return { success: false, error: error.message };
  }
};
```

---

### **Fix 4: Order Repository Authorization**

**File**: `app/src/main/java/com/gcuf/craftoria/data/repository/OrderRepository.kt`

**Ensure buyer can access their orders**:

```kotlin
suspend fun getOrderById(orderId: String): Result<Order> {
    return try {
        val snapshot = firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .get()
            .await()

        if (!snapshot.exists()) {
            return Result.failure(Exception("Order not found"))
        }

        val order = snapshot.toObject(Order::class.java) ?: Order()
        Result.success(order)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching order", e)
        Result.failure(e)
    }
}

// ✅ NEW: Add method to verify user involvement
suspend fun isUserInvolvedInOrder(orderId: String, userId: String): Result<Boolean> {
    return try {
        val orderResult = getOrderById(orderId)
        if (orderResult.isFailure) {
            return Result.success(false)
        }

        val order = orderResult.getOrNull()!!
        val isInvolved = order.buyerId == userId || 
                        order.sellerId == userId ||
                        order.items.any { it.sellerId == userId }

        Result.success(isInvolved)
    } catch (e: Exception) {
        Log.e(TAG, "Error checking user involvement", e)
        Result.failure(e)
    }
}
```

---

## 📱 Android Implementation

### **Update BuyerRefundRequestScreen.kt**

The current implementation already has the fix in place (lines 280-310), but ensure this logic is correct:

```kotlin
// ✅ CRITICAL FIX: Fetch payment IDs from order
val paymentsResult = paymentRepository.getOrderPayments(
    orderId = orderId,
    requestingUserId = currentUserId
)

if (paymentsResult.isFailure) {
    submitErrorMessage = paymentsResult.exceptionOrNull()?.message
        ?: "Failed to fetch payment information"
    showErrorDialog = true
    isSubmitting = false
    return@launch
}

val payments = paymentsResult.getOrNull() ?: emptyList()

if (payments.isEmpty()) {
    submitErrorMessage = "No payment records found for this order"
    showErrorDialog = true
    isSubmitting = false
    return@launch
}

// ✅ Create refund for each payment (handles multi-seller orders)
payments.forEach { payment ->
    val description = if (selectedReason == RefundReason.OTHER) {
        otherReasonDetails
    } else {
        selectedReason!!.getDisplayName()
    }

    val result = refundProcessor.initiateRefund(
        paymentId = payment.id,  // ✅ FIXED: Use actual payment ID
        refundAmount = payment.amount,
        reason = selectedReason!!.toString(),
        description = description,
        requestedBy = currentUserId
    )

    if (result.isFailure) {
        allSuccess = false
        failureMessage = result.exceptionOrNull()?.message
            ?: "Failed to create refund"
    }
}
```

---

## 🌐 Web Dashboard Implementation

### **Update OrderOversight.jsx**

The refund tab is already implemented. Ensure proper permissions:

```javascript
// In OrderOversight.jsx
const handleApproveRefund = async (notes) => {
  try {
    const result = await approveRefund(refundActionModal.refund.id, {
      approved_by: currentUser?.id,
      approver_name: currentUser?.name || currentUser?.email,
      approval_notes: notes,
    });

    if (result.success) {
      toast.success('Refund approved successfully');
      setRefundActionModal({ open: false, refund: null, action: null });
    } else {
      toast.error(result.error || 'Failed to approve refund');
    }
  } catch (err) {
    console.error(err);
    toast.error('Failed to approve refund');
  }
};
```

---

## 🔐 Firestore Security Rules

**File**: `firestore.rules`

```javascript
// Refunds collection
match /refunds/{refundId} {
  // Allow read if user is buyer, seller, or admin
  allow read: if request.auth != null && (
    resource.data.buyer_id == request.auth.uid ||
    resource.data.seller_id == request.auth.uid ||
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin'
  );

  // Allow create if user is buyer or seller
  allow create: if request.auth != null && (
    request.resource.data.buyer_id == request.auth.uid ||
    request.resource.data.seller_id == request.auth.uid
  );

  // Allow update only by admin
  allow update: if request.auth != null &&
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';

  // No delete
  allow delete: if false;
}

// Seller payments collection
match /seller_payments/{paymentId} {
  // ✅ FIX: Allow buyer to read their payments
  allow read: if request.auth != null && (
    resource.data.buyer_id == request.auth.uid ||
    resource.data.seller_id == request.auth.uid ||
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin'
  );

  // Only system can create/update payments
  allow create, update: if request.auth != null &&
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';

  allow delete: if false;
}
```

---

## ✅ Testing Checklist

### **Test Case 1: Buyer with Rejected Seller Role**
- [ ] Login as buyer (who was rejected seller)
- [ ] Navigate to completed order
- [ ] Click "Request Refund"
- [ ] Select refund reason
- [ ] Submit refund request
- [ ] **Expected**: Success, no "Unauthorized" error

### **Test Case 2: Payment History Visibility**
- [ ] Login as buyer
- [ ] Navigate to Payment History
- [ ] **Expected**: See all payments for orders placed as buyer

### **Test Case 3: Refund Notifications**
- [ ] Buyer submits refund
- [ ] **Expected**: 
  - Seller receives notification
  - Admin receives notification
  - Both can see refund in dashboard

### **Test Case 4: Admin Approval**
- [ ] Login as admin
- [ ] Navigate to Refunds tab in Order Oversight
- [ ] See pending refund
- [ ] Approve refund
- [ ] **Expected**: 
  - Buyer notified
  - Seller notified
  - Status changes to "Approved"
  - System processes refund

### **Test Case 5: Multi-Seller Order Refund**
- [ ] Buyer places order with products from 2 sellers
- [ ] Order completed
- [ ] Buyer requests refund
- [ ] **Expected**: 
  - Separate refund created for each seller's payment
  - Both sellers notified
  - Admin sees all refunds

---

## 📊 Refund Status Flow

```
REQUESTED → APPROVED → PROCESSING → COMPLETED
     ↓           ↓
  REJECTED    FAILED
```

**Status Definitions**:
- **REQUESTED**: Buyer submitted, awaiting admin review
- **APPROVED**: Admin approved, ready for processing
- **PROCESSING**: Payment gateway processing refund
- **COMPLETED**: Refund successfully credited to buyer
- **REJECTED**: Admin rejected refund request
- **FAILED**: Payment processing failed (will retry)

---

## 🎯 Summary

### **Key Changes**:
1. ✅ Fixed `PaymentRepository.getOrderPayments()` to check buyer authorization
2. ✅ Added buyer payment history query
3. ✅ Refunds notify BOTH seller AND admin
4. ✅ Admin has approval authority
5. ✅ Updated Firestore security rules
6. ✅ Added proper authorization checks

### **Refund Routing Answer**:
**Refunds go to BOTH seller AND admin**:
- **Seller**: Receives notification (transparency)
- **Admin**: Has approval/rejection authority
- **System**: Processes after admin approval

This ensures:
- Sellers are informed of refund requests
- Admin maintains control over refunds
- Buyers get proper authorization
- Multi-role users (buyer + rejected seller) work correctly

---

## 🚀 Deployment Steps

1. Update `PaymentRepository.kt`
2. Update `firestore.rules`
3. Deploy Firestore rules: `firebase deploy --only firestore:rules`
4. Test with buyer account (rejected seller)
5. Verify payment history visibility
6. Test refund submission
7. Verify admin receives notification
8. Test admin approval flow

---

**Status**: ✅ Complete Fix Ready for Deployment
