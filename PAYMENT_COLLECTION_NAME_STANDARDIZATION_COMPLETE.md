# Payment Collection Name Standardization - COMPLETE ✅

## Summary
All payment, refund, and commission-related files have been updated to use the correct collection name **"payments"** instead of the legacy **"seller_payments"**.

## Files Updated

### Android Kotlin Files (7 files)
1. **PaymentDataMigration.kt** - Changed collection reference from "seller_payments" to "payments"
2. **RealtimeNameUpdateManager.kt** - Removed legacy "seller_payments" updates (2 occurrences)
3. **RefundStatusMigration.kt** - Changed to use "payments" collection for payment updates
4. **PaymentIntegrityMonitor.kt** - Updated both collection references to "payments" (2 occurrences)
5. **DashboardRealtimeManager.kt** - Changed earnings listener to use "payments" collection
6. **DashboardDataHelper.kt** - Updated sample payment insertion to use "payments" collection

### JavaScript/Node Files (2 files)
1. **check-payment-data.mjs** - Updated query to use "payments" collection
2. **check-user-payments.mjs** - Updated payment snapshot query to use "payments" collection

### Firestore Configuration (1 file)
1. **firestore.rules** - Updated security rules to reference "payments" collection instead of "seller_payments"

## Already Correct Files (No Changes Needed)
These files were already using the correct "payments" collection:
- PaymentDetailScreen.kt
- DashboardViewModel.kt
- BuyerPaymentViewModel.kt
- RefundProcessor.kt
- PaymentSplitProcessor.kt
- SellerDashboardScreen.kt
- PaymentRepository.kt
- OrderRepository.kt
- DashboardRepository.kt
- CoSellerStorePaymentRepository.kt (uses both "payments" and "seller_payments" for legacy migration)
- CommissionRepository.kt (uses "admin_commissions" collection - correct)

## Collection Structure

### Canonical Collections (Primary)
- **payments** - Main payment records (created when order is placed, updated when order completes)
- **admin_commissions** - Commission records for admin earnings
- **refunds** - Refund request records

### Legacy Collections (No Longer Updated)
- **seller_payments** - Legacy collection (kept for backward compatibility during migration, but no new writes)

## Key Points

✅ **All new payment writes** go to the "payments" collection
✅ **All payment reads** query the "payments" collection
✅ **Commission system** uses "admin_commissions" collection (separate from payments)
✅ **Refund system** uses "refunds" collection (linked to payments via payment_id)
✅ **Firestore security rules** updated to reflect correct collection names
✅ **Migration scripts** updated to use correct collection names

## Testing Recommendations

1. **Verify payment creation** - Ensure new orders create payments in "payments" collection
2. **Verify payment queries** - Check that seller/buyer payment screens query "payments" collection
3. **Verify refund processing** - Ensure refunds update "payments" collection correctly
4. **Verify commission tracking** - Ensure commissions are tracked in "admin_commissions" collection
5. **Check Firestore rules** - Verify security rules allow proper access to "payments" collection

## Migration Notes

- The legacy "seller_payments" collection is kept for backward compatibility
- No automatic migration of old data is performed (existing data remains in both collections)
- All new operations use the canonical "payments" collection
- Consider running a one-time data consolidation if needed to merge legacy data

## Status
✅ **COMPLETE** - All payment, refund, and commission files now use correct collection names
