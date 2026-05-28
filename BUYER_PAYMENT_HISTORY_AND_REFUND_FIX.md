# Buyer Payment History & Refund Authorization Fix

## Issues Identified

### Issue 1: Payment History Not Updating After Order Completion
**Problem**: When an order is completed, the buyer's payment history screen doesn't show the new payments.

**Root Cause**: The `BuyerPaymentViewModel` uses `getBuyerPayments()` which queries by `buyer_id`, but when payments are created in `OrderRepository.createOrder()`, they might not have the `buyer_id` field set properly.

### Issue 2: Buyer Refund Request Fails with "Unauthorized: Not involved in this order"
**Problem**: When a buyer tries to request a refund, they get an error: "Unauthorized: Not involved in this order"

**Root Cause**: The `getOrderPayments()` function in `PaymentRepository` has a security check that only allows **sellers** to view payment splits:

```kotlin
// ❌ PROBLEM: Only checks if user is a SELLER
val isUserInvolved = payments.any { it.sellerId == requestingUserId }
if (!isUserInvolved) {
    return Result.failure(
        UnauthorizedAccessException("Unauthorized: Not involved in this order")
    )
}
```

This security check is designed for sellers viewing payment splits, but it blocks buyers from accessing their own order payments for refund requests.

## Solution

### Fix 1: Update PaymentRepository.getOrderPayments() to Allow Buyers

The function needs to check if the requesting user is either:
- A seller involved in the order, OR
- The buyer who placed the order

```kotlin
suspend fun getOrderPayments(
    orderId: String,
    requestingUserId: String
): Result<List<SellerPayment>> {
    return try {
        Log.d(TAG, "📋 Fetching payments for order: $orderId")

        val snapshot = paymentsCollection
            .whereEqualTo("order_id", orderId)
            .get()
            .await()

        val payments = snapshot.documents.mapNotNull { doc ->
            doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
        }.sortedByDescending { it.createdAt }

        if (payments.isEmpty()) {
            Log.w(TAG, "⚠️ No payments found for order: $orderId")
            return Result.success(emptyList())
        }

        // ✅ SECURITY CHECK: Verify requesting user is involved in this order
        // Allow access if user is either:
        // 1. A seller in the payment split
        // 2. The buyer who placed the order
        val isUserSeller = payments.any { it.sellerId == requestingUserId }
        val isUserBuyer = payments.any { it.buyerId == requestingUserId }
        
        if (!isUserSeller && !isUserBuyer) {
            Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to view payments for order $orderId (not involved)")
            return Result.failure(
                UnauthorizedAccessException(
                    "Unauthorized: Not involved in this order"
                )
            )
        }

        Log.d(TAG, "✅ Fetched ${payments.size} payments for order (user is ${if (isUserBuyer) "buyer" else "seller"})")
        Result.success(payments)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to fetch order payments", e)
        Result.failure(e)
    }
}
```

### Fix 2: Ensure buyer_id is Set When Creating Payments

Check `OrderRepository.createOrder()` to ensure all payments have the `buyer_id` field set:

```kotlin
// When creating payment in OrderRepository
val paymentData = mapOf(
    "order_id" to orderId,
    "buyer_id" to order.buyerId,  // ✅ CRITICAL: Must be set
    "seller_id" to sellerId,
    "amount" to sellerAmount,
    "status" to PaymentStatus.PENDING.toString(),
    "created_at" to System.currentTimeMillis(),
    // ... other fields
)
```

### Fix 3: Add getBuyerPayments() Query Index

The `getBuyerPayments()` function queries by `buyer_id`. Ensure Firestore has an index for this:

```json
{
  "collectionGroup": "seller_payments",
  "queryScope": "COLLECTION",
  "fields": [
    {
      "fieldPath": "buyer_id",
      "order": "ASCENDING"
    },
    {
      "fieldPath": "created_at",
      "order": "DESCENDING"
    }
  ]
}
```

## Testing Steps

### Test 1: Buyer Payment History
1. **Buyer**: Place an order
2. **Seller**: Mark order as shipped
3. **Seller**: Mark order as delivered
4. **Check**: Buyer's Payment History screen should show the payment immediately
5. **Verify**: Real-time listener updates the payment status

### Test 2: Buyer Refund Request
1. **Buyer**: Place an order and wait for delivery
2. **Buyer**: Navigate to "My Orders" → Select order → "Request Refund"
3. **Buyer**: Select refund reason and submit
4. **Expected**: Refund request should be submitted successfully
5. **Verify**: No "Unauthorized" error appears

### Test 3: Multi-Seller Order Refund
1. **Buyer**: Place order with products from multiple sellers
2. **Buyer**: Request refund after delivery
3. **Expected**: Refund should be created for all payments in the order
4. **Verify**: Each seller receives refund notification

## Files to Modify

### 1. PaymentRepository.kt
**Location**: `app/src/main/java/com/gcuf/craftoria/data/repository/PaymentRepository.kt`
**Function**: `getOrderPayments()` - Line ~251

**Change**: Update security check to allow both buyers and sellers

### 2. OrderRepository.kt (Verification)
**Location**: `app/src/main/java/com/gcuf/craftoria/data/repository/OrderRepository.kt`
**Function**: `createOrder()` 

**Verify**: All payment documents include `buyer_id` field

### 3. firestore.indexes.json (If needed)
**Location**: `firestore.indexes.json`

**Add**: Index for `buyer_id` + `created_at` query

## Expected Behavior After Fix

### Payment History Screen
```
✅ Shows all payments immediately after order completion
✅ Real-time updates when payment status changes
✅ Filters work correctly (Pending, Completed, etc.)
✅ Stats update in real-time
```

### Refund Request Screen
```
✅ Buyer can access their order payments
✅ Refund request submits successfully
✅ Multi-seller orders create multiple refunds
✅ No "Unauthorized" errors
```

## Security Considerations

The updated `getOrderPayments()` function maintains security by:
- ✅ Only allowing buyers to see their own orders
- ✅ Only allowing sellers to see orders they're involved in
- ✅ Preventing unauthorized access to payment data
- ✅ Logging all access attempts for audit

## Production Checklist

- [ ] Update `getOrderPayments()` security check
- [ ] Verify `buyer_id` is set in all payment documents
- [ ] Test buyer payment history with new orders
- [ ] Test buyer refund request flow
- [ ] Test multi-seller order refunds
- [ ] Deploy Firestore indexes if needed
- [ ] Monitor logs for unauthorized access attempts

---

**Status**: 🔧 FIX READY TO APPLY
**Priority**: 🔴 HIGH (Blocks buyer refund functionality)
**Impact**: Critical - Buyers cannot request refunds

