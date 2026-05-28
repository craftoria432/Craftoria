# Empty State Messages — Quick Reference

## Summary
Simplified empty state messages across two screens by removing verbose text and using concise, filter-specific titles.

## Changes at a Glance

### Seller Orders Screen
```
BEFORE: "No Orders Yet" + "Orders from buyers will appear here"
AFTER:  "No orders yet" + (empty message)
```

### Notifications Screen
```
BEFORE: "No Notifications Yet" + "When you get notifications, they'll show up here"
        "No [Filter] Notifications" + "Nothing here for this filter"

AFTER:  "No notifications yet" + (empty message)
        "No [filter] notifications yet" + (empty message)
```

## Filter-Specific Messages (Notifications)

| Filter | Message |
|--------|---------|
| All | "No notifications yet" |
| Unread | "No unread notifications yet" |
| Orders | "No order notifications yet" |
| Payments | "No payment notifications yet" |
| Refunds | "No refund notifications yet" |
| Messages | "No message notifications yet" |
| Promotions | "No promotion notifications yet" |
| System | "No system notifications yet" |
| Store Rating | "No store rating notifications yet" |
| Report | "No report notifications yet" |

## Files Modified

1. **SellerOrdersScreen.kt**
   - Function: `SellerEmptyOrdersState()`
   - Line: ~625

2. **NotificationsScreen.kt**
   - Function: `EmptyNotificationUiState()`
   - Line: ~990

## Key Improvements

✅ Removed verbose explanatory text
✅ Consistent lowercase formatting
✅ Filter-aware messages in Notifications
✅ Cleaner UI with icon + title only
✅ Professional, concise tone

---

**Status:** ✅ Complete
**Date:** May 27, 2026
