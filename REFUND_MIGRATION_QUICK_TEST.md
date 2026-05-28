# Refund Migration: Quick Test Guide

## What Changed

The app now automatically migrates old refunds stuck in "Refund Processing" state to "Refunded" on first app launch after update.

## Quick Test (5 minutes)

### 1. Build & Run App
```bash
# Build and run on device/emulator
```

### 2. Check Logcat for Migration
```
Filter: "RefundStatusMigration"

Expected output:
🔄 Starting refund status migration...
   Looking for refunds with status = 'processing'
   📝 Migrating refund: [ID] (order: [ID], payment: [ID])
      ✅ Refund status updated to COMPLETED
      ✅ Payment status updated to 'refunded'
      ✅ Order marked as refunded
   ✅ Successfully migrated refund: [ID]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Migration complete: X succeeded, 0 failed
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 3. Open Payment History
1. Login as buyer
2. Tap "Payment History"
3. Look for old refunds
4. **Expected**: Status badge shows "Refunded" (purple)
5. **Expected**: Refund info shows "Refunded: PKR [amount]"

### 4. Open My Orders
1. Tap "My Orders"
2. Tap "Completed" tab
3. Look for refunded orders
4. **Expected**: Order status badge shows "Completed" (green)
5. **Expected**: Refund badge shows [↶ Refunded] (purple)
6. **Expected**: No "Refund Approved" button

### 5. Restart App
1. Close app completely
2. Reopen app
3. Check logcat
4. **Expected**: Migration logs show "ℹ️ Migration already completed, skipping"
5. **Expected**: App starts faster (no migration re-run)

## What to Look For

### ✅ Success Indicators
- Migration logs appear on first launch
- Old refunds show "Refunded" status in Payment History
- Refunded orders show purple badge in My Orders
- No "Refund Approved" button on refunded orders
- Migration skips on subsequent launches

### ❌ Failure Indicators
- Migration logs show "❌ Failed to migrate refund"
- Old refunds still show "Refund Processing"
- Payment History doesn't update after migration
- App crashes during migration

## Troubleshooting

### Migration Logs Don't Appear
1. Check if you have old refunds in Firestore
2. Verify Firebase connection is active
3. Check logcat filter is set to "RefundStatusMigration"

### Old Refunds Still Show "Refund Processing"
1. Check migration logs for errors
2. Verify payment status was updated to "refunded" (lowercase)
3. Force refresh Payment History screen
4. Restart app to trigger real-time listener

### App Crashes During Migration
1. Check logcat for exception details
2. Verify Firebase rules allow updates to refunds/payments/orders
3. Check if any refund documents are corrupted
4. Try clearing app data and reinstalling

## Manual Testing (Advanced)

### Force Re-run Migration
```kotlin
// In MainActivity or debug menu:
val prefs = getSharedPreferences("craftoria_migrations", MODE_PRIVATE)
prefs.edit().remove("refund_status_migration_v1_completed").apply()
// Restart app — migration will run again
```

### Check Migration Status
```kotlin
val prefs = getSharedPreferences("craftoria_migrations", MODE_PRIVATE)
val completed = prefs.getBoolean("refund_status_migration_v1_completed", false)
Log.d("DEBUG", "Migration completed: $completed")
```

### Verify Firestore Updates
1. Open Firebase Console
2. Go to Firestore Database
3. Check `refunds` collection
4. Find a refund that was "processing"
5. **Expected**: `status` field now shows "completed"
6. Check `seller_payments` collection
7. Find corresponding payment
8. **Expected**: `status` field now shows "refunded"

## Performance Notes

- Migration runs in background (IO thread)
- Doesn't block UI
- Typically completes in <1 second
- Works with 10-100+ refunds
- Retries on next launch if any fail

## Deployment Checklist

- [ ] Code compiles without errors
- [ ] Migration logs appear on first launch
- [ ] Old refunds show "Refunded" status
- [ ] Payment History updates correctly
- [ ] My Orders shows correct badges
- [ ] Migration skips on subsequent launches
- [ ] No crashes or exceptions
- [ ] Firebase rules allow updates
- [ ] Real-time listeners pick up changes
