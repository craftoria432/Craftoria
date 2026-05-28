# Code Review Fixes Applied

## Summary
Four critical issues identified in code review have been fixed:

1. ✅ **Removed unhelpful logging from PaymentRepository** - The authorization error was not coming from getPaymentById()
2. ✅ **Fixed OrderDetailsDialog timeline** - Now appends "Refunded" step instead of replacing the last step
3. ✅ **Fixed RefundViewModel.getOrderForRefund()** - Replaced callback-based pattern with coroutine-native await()
4. ✅ **Fixed PaymentStatusBadge in SellerPaymentsScreen** - Now handles all refund statuses like BuyerPaymentStatusBadge

---

## Issue 1: Removed Unhelpful Logging from PaymentRepository

### Problem
The logging added to `getPaymentById()` was not helpful because:
- PaymentDetailScreen uses its own DisposableEffect Firestore listener directly
- It never calls `viewModel.loadPaymentDetail()` or `getPaymentById()`
- The "Unauthorized access" error is coming from somewhere else
- The logging would never be reached

### Solution
Removed the detailed logging from:
- `PaymentRepository.getPaymentById()` - Reverted to simple logging
- `PaymentRepository.listenToSellerPayments()` - Removed unnecessary debug logs

### Files Modified
- `app/src/main/java/com/gcuf/craftoria/data/repository/PaymentRepository.kt`

### Impact
- Cleaner code without misleading debug logs
- Actual authorization error must be investigated elsewhere (likely a different screen/ViewModel)

---

## Issue 2: Fixed OrderDetailsDialog Timeline

### Problem
The timeline fix had a subtle gap:
- It replaced the last timeline step if it contained "Delivered" or "Completed"
- If the last step was something else (e.g., "Out for delivery"), the refund wouldn't show
- This lost the full order history

### Solution
Changed from **replacing** the last step to **appending** a new "Refunded" step:

**Before:**
```kotlin
order.timeline.mapIndexed { index, item ->
    if (index == order.timeline.lastIndex && 
        (item.title.contains("Delivered", ignoreCase = true) || 
         item.title.contains("Completed", ignoreCase = true))) {
        item.copy(title = "Refunded", isCompleted = true)
    } else {
        item
    }
}
```

**After:**
```kotlin
order.timeline + OrderTimeline(
    title = "Refunded",
    isCompleted = true,
    timestamp = System.currentTimeMillis()
)
```

### Benefits
- Preserves full order history
- Works regardless of what the last timeline step is
- Shows complete refund flow: Ordered → Confirmed → Shipped → Delivered → Refunded

### Files Modified
- `app/src/main/java/com/gcuf/craftoria/ui/components/OrderDialogs.kt`

---

## Issue 3: Fixed RefundViewModel.getOrderForRefund()

### Problem
The method used callback-based pattern inside a flow {} builder:
```kotlin
flow {
    firestore.collection("orders")
        .document(orderId)
        .get()
        .addOnSuccessListener { doc ->
            viewModelScope.launch {
                emit(order)  // ❌ Wrong context!
            }
        }
}
```

This is incorrect because:
- `emit()` must be called from the flow's own coroutine context
- Calling it from a separate `viewModelScope.launch` can cause:
  - Silent emission failures
  - IllegalStateException
  - Race conditions

### Solution
Replaced with coroutine-native `await()` pattern:

**Before:**
```kotlin
fun getOrderForRefund(orderId: String): Flow<Order?> = flow {
    firestore.collection("orders")
        .document(orderId)
        .get()
        .addOnSuccessListener { doc ->
            viewModelScope.launch {
                emit(doc.toObject(Order::class.java))
            }
        }
}
```

**After:**
```kotlin
fun getOrderForRefund(orderId: String): Flow<Order?> = flow {
    try {
        val doc = firestore.collection("orders")
            .document(orderId)
            .get()
            .await()  // ✅ Correct: await() in flow context
        
        if (doc.exists()) {
            emit(doc.toObject(Order::class.java))
        } else {
            emit(null)
        }
    } catch (e: Exception) {
        emit(null)
    }
}
```

### Benefits
- Correct coroutine pattern
- No race conditions
- Proper error handling
- Emissions guaranteed to work

### Files Modified
- `app/src/main/java/com/gcuf/craftoria/viewmodel/RefundViewModel.kt`
- Added import: `kotlinx.coroutines.tasks.await`

---

## Issue 4: Fixed PaymentStatusBadge in SellerPaymentsScreen

### Problem
The badge only handled "refunded" status:
```kotlin
when (status.lowercase()) {
    "completed"  -> ...
    "pending"    -> ...
    "processing" -> ...
    "failed"     -> ...
    "refunded"   -> BorderColor to TextSecondary  // ❌ Only this one
    else         -> BackgroundSecondary to TextSecondary
}
```

Missing statuses fell through to the else branch and showed as unstyled gray:
- `refund_pending` - Should be Warning (yellow)
- `refund_processing` - Should be Processing (blue)
- `refund_rejected` - Should be Rejected (gray)

The BuyerPaymentStatusBadge already handled all four correctly.

### Solution
Updated PaymentStatusBadge to match BuyerPaymentStatusBadge:

```kotlin
when (status.lowercase()) {
    "completed"         -> Triple(Success.copy(alpha = 0.10f),           Success,           "Completed")
    "pending"           -> Triple(Warning.copy(alpha = 0.15f),           Warning,           "Pending")
    "processing"        -> Triple(Color(0xFF2196F3).copy(alpha = 0.10f), Color(0xFF2196F3), "Processing")
    "failed"            -> Triple(Error.copy(alpha = 0.10f),             Error,             "Failed")
    "refund_pending"    -> Triple(Warning.copy(alpha = 0.15f),           Warning,           "Refund Pending")
    "refund_processing" -> Triple(Color(0xFF2196F3).copy(alpha = 0.10f), Color(0xFF2196F3), "Refund Processing")
    "refunded"          -> Triple(Color(0xFF9C27B0).copy(alpha = 0.10f), Color(0xFF9C27B0), "Refunded")
    "refund_rejected"   -> Triple(Color(0xFF757575).copy(alpha = 0.10f), Color(0xFF757575), "Refund Rejected")
    else                -> Triple(BorderColor, TextSecondary, status.replaceFirstChar { it.uppercase() })
}
```

### Benefits
- Consistent styling across buyer and seller payment screens
- All refund statuses properly color-coded
- Better UX with clear visual distinction for each status

### Files Modified
- `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerPaymentsScreen.kt`

---

## Compilation Status

All modified files compile without errors:
- ✅ PaymentRepository.kt
- ✅ OrderDialogs.kt
- ✅ RefundViewModel.kt
- ✅ SellerPaymentsScreen.kt

---

## Testing Recommendations

### Issue 1: Authorization Error
- The "Unauthorized access" error is likely coming from a different code path
- Search for other callers of `getPaymentById()` or similar authorization checks
- Check if there's a different screen/ViewModel calling the old path

### Issue 2: Timeline Display
- Test with a refunded order
- Verify the timeline shows all steps: Ordered → Confirmed → Shipped → Delivered → Refunded
- Test with orders that have custom timeline steps

### Issue 3: Order Fetching
- Test RefundDetailsScreen with various order IDs
- Verify order data loads correctly
- Check that errors are handled gracefully

### Issue 4: Payment Status Badges
- Test seller payment screen with payments in each status:
  - completed, pending, processing, failed
  - refund_pending, refund_processing, refunded, refund_rejected
- Verify colors match the design system
- Compare with buyer payment screen for consistency

---

## Summary of Changes

| Issue | File | Change | Status |
|-------|------|--------|--------|
| 1 | PaymentRepository.kt | Removed unhelpful logging | ✅ Complete |
| 2 | OrderDialogs.kt | Append "Refunded" step instead of replacing | ✅ Complete |
| 3 | RefundViewModel.kt | Use await() instead of callbacks in flow | ✅ Complete |
| 4 | SellerPaymentsScreen.kt | Handle all refund statuses in badge | ✅ Complete |

All changes are production-ready and compile without errors.

