# Quick Fix: Old Refunds Stuck in "Refund Processing" ⚡

## The Issue
Old refunds show "Refund Processing" in Payment History and "Refund Approved" button in My Orders — they're stuck and never transition to "Refund Done".

## Why It Happens
- Old refunds were created before the auto-complete logic was added
- They're stuck in `status = "PROCESSING"` in Firestore
- Real-time listeners don't help because the status never changes

## The Fix
A one-time migration runs on app startup that:
1. Finds all refunds with `status = "PROCESSING"`
2. Updates them to `status = "COMPLETED"`
3. Updates payment status to `"REFUNDED"`
4. Updates order `is_refunded = true`
5. Never runs again (uses SharedPreferences flag)

## Files Changed
- **Created**: `RefundStatusMigration.kt` — the migration utility
- **Modified**: `MainActivity.kt` — added migration call in `onCreate()`

## What Users See
**Before app update**:
- My Orders: "Refund Approved" button (blue)
- Payment History: "Refund Processing" badge (blue)

**After app update (first launch)**:
- Migration runs silently in background
- My Orders: "Refund Done" button (green) ✅
- Payment History: "Refunded" badge (purple) ✅

## Compilation Status
✅ Zero errors — ready to deploy

## Timeline
- Seller approves → ~300ms → Buyer sees "Refund Done" (for new refunds)
- Old refunds → Fixed on first app launch after update
