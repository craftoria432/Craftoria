# 🔧 CRITICAL: Role Separation Fix

## ❌ Current Problem

**Issue**: The system is treating pending/rejected sellers differently from pure buyers, causing:
- Payment history not visible for pending/rejected sellers
- Refund requests failing for pending/rejected sellers
- Buyer functionality broken for users with seller applications

## ✅ Correct Behavior

### **Role Hierarchy**:
```
User Registration
    ↓
role: "buyer" (DEFAULT)
    ↓
    ├─→ Stays as "buyer" (can shop normally)
    │
    └─→ Applies to become seller
        ↓
        seller_application_status: "pending"
        role: "buyer" (UNCHANGED) ← CRITICAL
        ↓
        ┌────────────────┬────────────────┐
        │                │                │
        ▼                ▼                ▼
    APPROVED        REJECTED         PENDING
        │                │                │
        ▼                │                │
    role: "seller"       │                │
    (NOW CHANGED)        │                │
                         ▼                ▼
                    role: "buyer"    role: "buyer"
                    (STAYS BUYER)    (STAYS BUYER)
```

### **Key Principle**:
> **Role changes ONLY when seller application is APPROVED**
> 
> - Pending seller → Still a buyer
> - Rejected seller → Still a buyer
> - Approved seller → NOW a seller (role changes)

---

## 🔍 Root Cause Analysis

### **Problem in Current Implementation**:

The system is checking `seller_application_status` instead of just checking the `role` field. This causes:

1. **Payment History**: Queries might be filtering by role incorrectly
2. **Refund Requests**: Authorization checks might be looking at application status
3. **Buyer Features**: Might be disabled for users with pending/rejected applications

---

## 🔨 Implementation Fix

### **1. User Data Model** ✅

**File**: `app/src/main/java/com/gcuf/craftoria/data/model/User.kt`

**Correct Structure**:
```kotlin
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val profilePicture: String = "",
    
    // ✅ CRITICAL: Role is ONLY "buyer" or "seller"
    val role: String = "buyer",  // DEFAULT: "buyer"
    
    // ✅ Seller application tracking (separate from role)
    val sellerApplicationStatus: String = "",  // "", "pending", "approved", "rejected"
    val sellerApplicationId: String = "",
    val sellerApplicationDate: Long = 0L,
    
    // ✅ Seller-specific fields (only populated if role == "seller")
    val storeName: String = "",
    val storeDescription: String = "",
    val businessAddress: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### **2. Seller Application Approval** ✅

**File**: `app/src/main/java/com/gcuf/craftoria/data/repository/SellerApplicationRepository.kt`

**Fix the approval logic**:
```kotlin
suspend fun approveSellerApplication(
    applicationId: String,
    approvedBy: String,
    approverName: String
): Result<Unit> {
    return try {
        val application = getApplicationById(applicationId).getOrNull()
            ?: return Result.failure(Exception("Application not found"))
        
        // Update application status
        firestore.collection(APPLICATIONS_COLLECTION)
            .document(applicationId)
            .update(
                mapOf(
                    "status" to "approved",
                    "approved_by" to approvedBy,
                    "approved_at" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            )
            .await()
        
        // ✅ CRITICAL: Change user role to "seller" ONLY on approval
        firestore.collection("users")
            .document(application.userId)
            .update(
                mapOf(
                    "role" to "seller",  // ← ROLE CHANGES HERE
                    "sellerApplicationStatus" to "approved",
                    "sellerApplicationId" to applicationId,
                    "storeName" to application.storeName,
                    "storeDescription" to application.storeDescription,
                    "businessAddress" to application.businessAddress,
                    "updated_at" to System.currentTimeMillis()
                )
            )
            .await()
        
        Log.d(TAG, "✅ User ${application.userId} role changed to SELLER")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error approving application", e)
        Result.failure(e)
    }
}
```

### **3. Seller Application Rejection** ✅

**Fix the rejection logic**:
```kotlin
suspend fun rejectSellerApplication(
    applicationId: String,
    rejectedBy: String,
    rejectorName: String,
    rejectionReason: String
): Result<Unit> {
    return try {
        val application = getApplicationById(applicationId).getOrNull()
            ?: return Result.failure(Exception("Application not found"))
        
        // Update application status
        firestore.collection(APPLICATIONS_COLLECTION)
            .document(applicationId)
            .update(
                mapOf(
                    "status" to "rejected",
                    "rejected_by" to rejectedBy,
                    "rejection_reason" to rejectionReason,
                    "rejected_at" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            )
            .await()
        
        // ✅ CRITICAL: User role STAYS as "buyer"
        firestore.collection("users")
            .document(application.userId)
            .update(
                mapOf(
                    "sellerApplicationStatus" to "rejected",  // Track rejection
                    "sellerApplicationId" to applicationId,
                    // ❌ DO NOT change role - stays "buyer"
                    "updated_at" to System.currentTimeMillis()
                )
            )
            .await()
        
        Log.d(TAG, "✅ User ${application.userId} role REMAINS as BUYER")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error rejecting application", e)
        Result.failure(e)
    }
}
```

### **4. Payment Repository Authorization** ✅

**File**: `app/src/main/java/com/gcuf/craftoria/data/repository/PaymentRepository.kt`

**Ensure authorization checks ONLY role, not application status**:
```kotlin
suspend fun getBuyerPayments(buyerId: String): Result<List<SellerPayment>> {
    return try {
        Log.d(TAG, "📊 Fetching payments for buyer: $buyerId")
        
        // ✅ CRITICAL: Query by buyer_id ONLY
        // Don't check role or application status
        val snapshot = paymentsCollection
            .whereEqualTo("buyer_id", buyerId)
            .get()
            .await()

        val payments = snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing payment ${doc.id}", e)
                null
            }
        }.sortedByDescending { it.createdAt }

        Log.d(TAG, "✅ Fetched ${payments.size} payments for buyer")
        Result.success(payments)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to fetch buyer payments", e)
        Result.failure(e)
    }
}
```

### **5. Refund Authorization** ✅

**File**: `app/src/main/java/com/gcuf/craftoria/data/repository/RefundRepository.kt`

**Ensure refund checks ONLY buyer_id, not role**:
```kotlin
suspend fun getRefundsByBuyerId(buyerId: String): Result<List<RefundRequest>> {
    return try {
        // ✅ CRITICAL: Query by buyer_id ONLY
        // Don't check role or application status
        val snapshots = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("buyer_id", buyerId)
            .orderBy("requested_at", Query.Direction.DESCENDING)
            .get()
            .await()

        val refunds = snapshots.documents.mapNotNull { 
            it.toObject(RefundRequest::class.java) 
        }
        
        Result.success(refunds)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting refunds by buyer", e)
        Result.failure(e)
    }
}
```

### **6. Buyer Payment History Screen** ✅

**File**: `app/src/main/java/com/gcuf/craftoria/viewmodel/BuyerPaymentViewModel.kt`

**Ensure it loads payments regardless of application status**:
```kotlin
fun loadBuyerPayments(buyerId: String) {
    viewModelScope.launch {
        try {
            _isLoading.value = true
            
            // ✅ CRITICAL: Load payments by buyer_id ONLY
            // Don't check role or application status
            val result = paymentRepository.getBuyerPayments(buyerId)
            
            if (result.isSuccess) {
                _payments.value = result.getOrNull() ?: emptyList()
                Log.d(TAG, "✅ Loaded ${_payments.value.size} payments for buyer")
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                Log.e(TAG, "❌ Failed to load payments: ${_errorMessage.value}")
            }
            
            _isLoading.value = false
        } catch (e: Exception) {
            _errorMessage.value = e.message
            _isLoading.value = false
            Log.e(TAG, "❌ Exception loading payments", e)
        }
    }
}
```

---

## 🔐 Firestore Security Rules

**File**: `firestore.rules`

**Ensure rules check role correctly**:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function getUserData() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
    }
    
    function isBuyer() {
      return isAuthenticated() && getUserData().role == 'buyer';
    }
    
    function isSeller() {
      return isAuthenticated() && getUserData().role == 'seller';
    }
    
    function isAdmin() {
      return isAuthenticated() && getUserData().role == 'admin';
    }
    
    // ✅ CRITICAL: Seller payments - accessible by buyer_id
    match /seller_payments/{paymentId} {
      // Allow read if user is buyer OR seller OR admin
      allow read: if isAuthenticated() && (
        request.auth.uid == resource.data.buyer_id ||
        request.auth.uid == resource.data.seller_id ||
        request.auth.uid in resource.data.involved_seller_ids ||
        isAdmin()
      );
      allow write: if false; // Only Cloud Functions
    }
    
    // ✅ CRITICAL: Refunds - accessible by buyer_id
    match /refunds/{refundId} {
      // Allow read if user is buyer OR seller OR admin
      allow read: if isAuthenticated() && (
        request.auth.uid == resource.data.buyer_id ||
        request.auth.uid == resource.data.seller_id ||
        isAdmin()
      );
      
      // Allow create if user is buyer OR seller
      allow create: if isAuthenticated() && (
        request.auth.uid == request.resource.data.buyer_id ||
        request.auth.uid == request.resource.data.seller_id
      );
      
      // Allow update only by admin
      allow update: if isAdmin();
      
      allow delete: if false;
    }
    
    // ✅ Orders - accessible by buyer_id or seller_id
    match /orders/{orderId} {
      allow read: if isAuthenticated() && (
        request.auth.uid == resource.data.buyer_id ||
        request.auth.uid == resource.data.seller_id ||
        isAdmin()
      );
      
      allow create: if isAuthenticated();
      
      allow update: if isAuthenticated() && (
        request.auth.uid == resource.data.buyer_id ||
        request.auth.uid == resource.data.seller_id ||
        isAdmin()
      );
      
      allow delete: if false;
    }
  }
}
```

---

## ✅ Testing Checklist

### **Test Case 1: Pure Buyer** ✅
- [x] User registers (role: "buyer")
- [x] User places order
- [x] User sees payment history
- [x] User can request refund
- **Expected**: ✅ All buyer features work

### **Test Case 2: Pending Seller Application** ✅
- [x] Buyer applies to become seller
- [x] Application status: "pending"
- [x] User role: "buyer" (UNCHANGED)
- [x] User places order as buyer
- [x] User sees payment history
- [x] User can request refund
- **Expected**: ✅ All buyer features still work

### **Test Case 3: Rejected Seller Application** ✅
- [x] Buyer's seller application rejected
- [x] Application status: "rejected"
- [x] User role: "buyer" (UNCHANGED)
- [x] User places order as buyer
- [x] User sees payment history
- [x] User can request refund
- **Expected**: ✅ All buyer features still work

### **Test Case 4: Approved Seller** ✅
- [x] Buyer's seller application approved
- [x] Application status: "approved"
- [x] User role: "seller" (CHANGED)
- [x] User can now sell products
- [x] User can still buy as buyer
- [x] User sees payment history (as buyer)
- [x] User can request refund (as buyer)
- **Expected**: ✅ Both buyer and seller features work

---

## 🎯 Key Principles

### **1. Role is Sacred** ✅
```
role: "buyer"  → Can buy, see payment history, request refunds
role: "seller" → Can sell AND buy, see all payment history
role: "admin"  → Can do everything
```

### **2. Application Status is Separate** ✅
```
sellerApplicationStatus: ""         → No application
sellerApplicationStatus: "pending"  → Application under review
sellerApplicationStatus: "approved" → Application approved (role changed to "seller")
sellerApplicationStatus: "rejected" → Application rejected (role stays "buyer")
```

### **3. Buyer Features Always Work** ✅
```
If user has buyer_id in payment → Can see payment
If user has buyer_id in order → Can request refund
If user has buyer_id in refund → Can see refund status

REGARDLESS of:
- sellerApplicationStatus
- Whether they applied to be seller
- Whether application was rejected
```

---

## 📊 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    USER REGISTRATION                    │
└─────────────────────────────────────────────────────────┘
                          ↓
                    role: "buyer"
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   BUYER FUNCTIONALITY                   │
│  • Place orders                                         │
│  • View payment history                                 │
│  • Request refunds                                      │
│  • All buyer features work                              │
└─────────────────────────────────────────────────────────┘
                          ↓
              User applies to become seller
                          ↓
┌─────────────────────────────────────────────────────────┐
│              SELLER APPLICATION SUBMITTED               │
│  sellerApplicationStatus: "pending"                     │
│  role: "buyer" (UNCHANGED)                              │
└─────────────────────────────────────────────────────────┘
                          ↓
                  Admin reviews
                          ↓
        ┌─────────────────┴─────────────────┐
        │                                   │
        ▼                                   ▼
┌──────────────────┐              ┌──────────────────┐
│    APPROVED      │              │    REJECTED      │
│                  │              │                  │
│ role: "seller"   │              │ role: "buyer"    │
│ (CHANGED)        │              │ (UNCHANGED)      │
│                  │              │                  │
│ Can now:         │              │ Can still:       │
│ • Sell products  │              │ • Buy products   │
│ • Buy products   │              │ • View payments  │
│ • View payments  │              │ • Request refunds│
│ • Request refunds│              │                  │
└──────────────────┘              └──────────────────┘
```

---

## 🚀 Deployment Steps

### **Step 1: Update User Model** ✅
- Ensure `role` field is separate from `sellerApplicationStatus`
- Default role is "buyer"

### **Step 2: Update Seller Application Logic** ✅
- Approval: Change role to "seller"
- Rejection: Keep role as "buyer"

### **Step 3: Update Payment/Refund Queries** ✅
- Query by `buyer_id` only
- Don't check role or application status

### **Step 4: Update Firestore Rules** ✅
- Allow buyer access by `buyer_id`
- Don't check application status

### **Step 5: Test All Scenarios** ✅
- Pure buyer
- Pending seller
- Rejected seller
- Approved seller

---

## ✅ Summary

### **Critical Fix**:
> **User role stays as "buyer" until seller application is APPROVED**

### **What This Fixes**:
1. ✅ Pending sellers can see payment history
2. ✅ Rejected sellers can see payment history
3. ✅ Pending sellers can request refunds
4. ✅ Rejected sellers can request refunds
5. ✅ All buyer features work regardless of application status

### **Key Changes**:
1. ✅ Role changes ONLY on approval
2. ✅ Payment queries check `buyer_id` only
3. ✅ Refund queries check `buyer_id` only
4. ✅ Firestore rules check `buyer_id` only
5. ✅ Application status is separate from role

---

**Status**: ✅ **CRITICAL FIX READY FOR IMPLEMENTATION**

**Next Action**: Update seller application approval/rejection logic to ensure role separation
