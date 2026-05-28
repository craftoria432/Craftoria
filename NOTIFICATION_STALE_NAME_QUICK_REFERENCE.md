# Notification Stale Name - Quick Reference

## What Was Fixed

| Issue | Root Cause | Fix |
|-------|-----------|-----|
| Stale seller name in notification | Store ID not passed to notification | Pass `coSellerStoreId` from order |
| "0 Members" in notification | Wrong member count source | Prioritize `member_ids` array |
| Name not updating in real-time | Store name not synced when seller updates | Add `updateCoSellerStoreNames()` |
| Cart showing blank screen | CartScreen creating new ViewModel | Use passed `cartViewModel` |

## Files Changed

### 1. OrderRepository.kt
```kotlin
// BEFORE: store_id = ""
// AFTER: store_id = coSellerStoreId (fetched from co_seller_stores)
sendNewOrderNotification(..., coSellerStoreId = order.coSellerStoreId)
```

### 2. NotificationsScreen.kt
```kotlin
// ALREADY CORRECT: Listens to co_seller_stores/{storeId}
// Prioritizes member_ids array over member_count field
```

### 3. RealtimeNameUpdateManager.kt
```kotlin
// NEW: updateCoSellerStoreNames() function
// UPDATED: updateUserNameEverywhere() calls updateCoSellerStoreNames()
```

### 4. CartScreen.kt
```kotlin
// BEFORE: cartViewModel: CartViewModel = viewModel()
// AFTER: cartViewModel: CartViewModel (no default)
```

## How to Test

### Test 1: Create Order
1. Buyer creates order from co-seller store
2. Check notification shows correct store name + member count
3. ✅ Should show "Zara's Store" + "2 Members"

### Test 2: Update Name
1. Seller updates profile name
2. Check notification in real-time
3. ✅ Should update instantly

### Test 3: Add Member
1. Add member to store
2. Check notification member count
3. ✅ Should increase

## Logging to Monitor

```
✅ Fetched store data: Zara's Store with 2 members
✅ Updated store name to: Zara Ali
✅ Updated member count to: 2
✅ Updated co-seller store names for member: user123
```

## Key Points

- **Store ID**: Now populated at notification creation time
- **Real-Time**: Listener on `co_seller_stores/{storeId}` updates UI instantly
- **Member Count**: Uses `member_ids` array (always accurate)
- **Name Sync**: When seller updates name, all co-seller stores are updated
- **Cleanup**: Listener is properly disposed when notification is removed

## Status

✅ All fixes implemented
✅ Ready for testing
✅ No breaking changes
