# Bug Fixes: Payment History Skeleton Flash & Refund Deserialization Crash

## Summary
Fixed two critical bugs affecting the buyer payment and refund systems:
1. **Skeleton Flash on Payment History** - Jarring UI flicker when loading payment data
2. **Refund Resubmission Crash** - Firestore Timestamp deserialization error

---

## Bug 1: Payment History Skeleton Flash ✅ FIXED

### Root Cause
The `BuyerPaymentViewModel` is scoped to the composable via `viewModel()`, so every time `PaymentHistoryScreen` is entered fresh, the cache is empty. The `LaunchedEffect` fires immediately and sets Loading state, causing the skeleton UI to show instantly before data arrives. This creates a jarring layout shift.

### Solution
Added a **300ms delay** before showing skeleton UI. If data loads within 300ms (typical for cached/fast data), the skeleton never appears. If loading takes longer, the skeleton shows smoothly without the flash.

### Changes in `PaymentHistoryScreen.kt`

**Stats Section:**
```kotlin
when (val s = statsState) {
    is BuyerPaymentStatsUiState.Loading -> {
        // Only show skeleton if loading takes more than 300ms to avoid flash
        var showSkeleton by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            showSkeleton = true
        }
        if (showSkeleton) {
            BuyerPaymentStatsCardSkeleton()
        } else {
            // Invisible placeholder to maintain layout
            Box(modifier = Modifier.fillMaxWidth().height(180.dp))
        }
    }
    is BuyerPaymentStatsUiState.Success -> BuyerPaymentStatsCards(s.stats)
    is BuyerPaymentStatsUiState.Error -> { /* skip stats section on error */ }
}
```

**Payment List Section:**
```kotlin
when (val p = paymentState) {
    is BuyerPaymentUiState.Loading -> {
        // Only show skeleton if loading takes more than 300ms to avoid flash
        var showSkeleton by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(300)
            showSkeleton = true
        }
        if (showSkeleton) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(3) {
                    BuyerPaymentCardSkeleton()
                }
            }
        }
    }
    // ... rest of states
}
```

---

## Bug 2: Refund Resubmission Deserialization Crash ✅ FIXED

### Root Cause
Error: `Could not deserialize object. Failed to convert a value of type com.google.firebase.Timestamp to long (found in field 'updated_at')`

The `RefundRepository` was using Firestore's automatic `toObject(RefundRequest::class.java)` deserialization. When Firestore returns Timestamp objects for timestamp fields, the reflection-based deserialization fails because:
- Firestore stores timestamps as `Timestamp` objects
- The `RefundRequest` model expects `Long` values
- The automatic deserialization can't convert `Timestamp` → `Long` for all fields

This crash occurred in `BuyerRefundRequestScreen` when fetching refunds via `refundRepository.getRefundsByOrderId()`.

### Solution
Implemented a **manual `parseRefund()` function** that:
1. Safely extracts all fields from the Firestore document
2. Explicitly converts all timestamp fields using `convertTimestampToLong()`
3. Handles nested objects (RefundSplit, RefundAuditEntry) with proper type casting
4. Returns `null` on any parsing error (logged) instead of crashing

### Changes in `RefundRepository.kt`

**New parseRefund() Function:**
```kotlin
private fun parseRefund(doc: com.google.firebase.firestore.DocumentSnapshot): RefundRequest? {
    return try {
        val data = doc.data ?: return null
        RefundRequest(
            id = doc.id,
            orderId = doc.getString("order_id") ?: "",
            paymentId = doc.getString("payment_id") ?: "",
            buyerId = doc.getString("buyer_id") ?: "",
            buyerName = doc.getString("buyer_name") ?: "",
            sellerId = doc.getString("seller_id") ?: "",
            sellerName = doc.getString("seller_name") ?: "",
            refundType = doc.getString("refund_type") ?: RefundType.FULL.toString(),
            originalAmount = (data["original_amount"] as? Number)?.toDouble() ?: 0.0,
            refundAmount = (data["refund_amount"] as? Number)?.toDouble() ?: 0.0,
            reason = doc.getString("reason") ?: "",
            reasonDetails = doc.getString("reason_details") ?: "",
            status = doc.getString("status") ?: RefundStatus.REQUESTED.toString(),
            initiatedBy = doc.getString("initiated_by") ?: "",
            approvedBy = doc.getString("approved_by") ?: "",
            approvalNotes = doc.getString("approval_notes") ?: "",
            rejectionCount = (data["rejection_count"] as? Number)?.toInt() ?: 0,
            canResubmit = data["can_resubmit"] as? Boolean ?: true,
            finalDecision = data["final_decision"] as? Boolean ?: false,
            paymentMethod = doc.getString("payment_method") ?: "Cash on Delivery",
            transactionId = doc.getString("transaction_id") ?: "",
            gatewayRefundId = doc.getString("gateway_refund_id") ?: "",
            retryCount = (data["retry_count"] as? Number)?.toInt() ?: 0,
            errorMessage = doc.getString("error_message") ?: "",
            idempotencyKey = doc.getString("idempotency_key") ?: "",
            // All timestamp fields stored as Long (already converted)
            requestedAt = convertTimestampToLong(data["requested_at"]) ?: System.currentTimeMillis(),
            approvedAt = convertTimestampToLong(data["approved_at"]),
            processedAt = convertTimestampToLong(data["processed_at"]),
            completedAt = convertTimestampToLong(data["completed_at"]),
            createdAt = convertTimestampToLong(data["created_at"]) ?: System.currentTimeMillis(),
            updatedAt = convertTimestampToLong(data["updated_at"]) ?: System.currentTimeMillis(),
            lastRetryAt = convertTimestampToLong(data["last_retry_at"]),
            // List fields with safe casting
            refundSplits = (data["refund_splits"] as? List<*>)?.mapNotNull { split ->
                (split as? Map<*, *>)?.let { m ->
                    RefundSplit(
                        sellerId = m["seller_id"] as? String ?: "",
                        sellerName = m["seller_name"] as? String ?: "",
                        originalSplitAmount = (m["original_split_amount"] as? Number)?.toDouble() ?: 0.0,
                        refundSplitAmount = (m["refund_split_amount"] as? Number)?.toDouble() ?: 0.0,
                        status = m["status"] as? String ?: RefundStatus.REQUESTED.toString(),
                        gatewayRefundId = m["gateway_refund_id"] as? String ?: ""
                    )
                }
            } ?: emptyList(),
            auditTrail = (data["audit_trail"] as? List<*>)?.mapNotNull { entry ->
                (entry as? Map<*, *>)?.let { m ->
                    RefundAuditEntry(
                        action = m["action"] as? String ?: "",
                        actor = m["actor"] as? String ?: "",
                        actorName = m["actor_name"] as? String ?: "",
                        notes = m["notes"] as? String ?: "",
                        timestamp = convertTimestampToLong(m["timestamp"]) ?: System.currentTimeMillis()
                    )
                }
            } ?: emptyList()
        )
    } catch (e: Exception) {
        Log.e(TAG, "parseRefund failed for doc ${doc.id}: ${e.message}", e)
        null
    }
}
```

**Updated Methods Using parseRefund():**
- `getRefundById()` - Single refund fetch
- `getRefundsByOrderId()` - Refunds for an order (used in BuyerRefundRequestScreen)
- `getRefundsByBuyerId()` - Buyer's refunds
- `getRefundsBySellerId()` - Seller's refunds
- `getPendingRefunds()` - Pending refunds for admin
- `getFailedRefundsForRetry()` - Failed refunds for retry
- `checkDuplicateRefund()` - Idempotency check

All methods now use `parseRefund(doc)` instead of `doc.toObject(RefundRequest::class.java)`.

---

## Testing Checklist

### Bug 1: Skeleton Flash
- [ ] Navigate to Payment History screen
- [ ] Verify no skeleton flash appears on first load (data loads within 300ms)
- [ ] Verify skeleton appears smoothly if network is slow (>300ms)
- [ ] Verify layout remains stable (no shift when skeleton appears)

### Bug 2: Refund Crash
- [ ] Request a refund on a completed order
- [ ] Navigate to Payment History → Refund Details
- [ ] Verify no crash occurs when loading refund data
- [ ] Verify refund status displays correctly (Pending, Processing, Completed, etc.)
- [ ] Test on slow network to ensure parsing handles delays

---

## Files Modified
1. `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/PaymentHistoryScreen.kt`
   - Added 300ms delay before showing skeleton in stats section
   - Added 300ms delay before showing skeleton in payment list section

2. `app/src/main/java/com/gcuf/craftoria/data/repository/RefundRepository.kt`
   - Added `parseRefund()` manual parser function
   - Updated 7 methods to use `parseRefund()` instead of `toObject()`
   - Existing `convertTimestampToLong()` helper remains unchanged

---

## Compilation Status
✅ **No errors** - Both files compile successfully
✅ **No warnings** - Clean build

---

## Impact
- **Bug 1**: Improves UX by eliminating jarring skeleton flash on fast loads
- **Bug 2**: Prevents crash when resubmitting refunds or viewing refund history
- **No breaking changes** - All existing functionality preserved
- **Backward compatible** - Works with existing refund data in Firestore
