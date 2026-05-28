# Refund Audit — Quick Reference

## 4 Critical Bugs Fixed

### 1. PaymentDetailScreen — Refunded Status Color
- **File:** `PaymentDetailScreen.kt` line ~175
- **Change:** `"refunded" -> TextSecondary to Icons.AutoMirrored.Filled.Undo` → `"refunded" -> Success to Icons.Default.CheckCircle`
- **Why:** Refunded payments should show green (success), not gray (pending)

### 2. MyOrdersScreen — Missing Refund Button
- **File:** `MyOrdersScreen.kt` line ~898
- **Change:** Added fallback: `val effectiveDate = if (deliveredAt > 0) deliveredAt else order.getCreatedAtLong()`
- **Why:** When deliveredAt is 0, use createdAt to calculate the 30-day refund window

### 3. RefundRepository.approveRefund() — Error Handling
- **File:** `RefundRepository.kt` line ~280
- **Change:** Return `Result.failure(err)` instead of `Result.success(refund)` when completeRefund() fails
- **Why:** Errors must be propagated so the UI can show error states and trigger retries

### 4. CoSellerStorePaymentScreen — Missing Refund Status Cases
- **File:** `CoSellerStorePaymentScreen.kt` line ~280
- **Change:** Added 4 new cases: `"refunded"`, `"refund_pending"`, `"refund_processing"`, `"refund_rejected"`
- **Why:** All 8 refund statuses need explicit color mappings

## 2 Warnings Fixed

### 5. Extract Shared docPriority() Function
- **New File:** `RefundStateUtils.kt`
- **Updated:** `MyOrdersScreen.kt`, `SellerOrdersScreen.kt`
- **Why:** Eliminate 40+ lines of duplicated code

### 6. Add Firestore Composite Index
- **File:** `firestore.indexes.json`
- **Added:** Index for `refunds` collection on `(status, retry_count, last_retry_at)`
- **Why:** `getFailedRefundsForRetry()` query requires this index

## Files Modified

```
✅ app/src/main/java/com/gcuf/craftoria/ui/screens/seller/PaymentDetailScreen.kt
✅ app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt
✅ app/src/main/java/com/gcuf/craftoria/data/repository/RefundRepository.kt
✅ app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/CoSellerStorePaymentScreen.kt
✅ app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerOrdersScreen.kt
✅ app/src/main/java/com/gcuf/craftoria/utils/RefundStateUtils.kt (NEW)
✅ firestore.indexes.json
```

## Deployment

1. Deploy code changes
2. Run: `firebase deploy --only firestore:indexes`
3. Test all 6 fixes
4. Monitor logs for index creation

## Status

✅ All diagnostics pass — no compilation errors
✅ All 4 bugs fixed
✅ All 2 warnings resolved
✅ Ready for deployment
