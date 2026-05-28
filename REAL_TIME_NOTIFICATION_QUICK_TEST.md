# Real-Time Notification Updates - Quick Test Guide

## What Was Fixed
Member count and store name now update in real-time in notifications without requiring manual refresh.

## How It Works
- Firestore snapshot listener monitors all notifications for the user
- When member_count or store_name changes, notifications update instantly
- Filter is applied client-side to real-time updates

## Quick Test Scenarios

### Scenario 1: Member Joins Store (Existing Notification)
**Steps:**
1. Open NotificationsScreen
2. Find a notification with `member_count = 2`
3. In another session/device, add a member to that store
4. **Expected**: Notification member_count updates to 3 in real-time

**What to look for:**
- No manual refresh needed
- Member count changes instantly
- Notification stays in same position

### Scenario 2: Store Name Changes
**Steps:**
1. Open NotificationsScreen
2. Find a notification with `store_name = "Original Name"`
3. Update the store name to "New Name"
4. **Expected**: Notification shows "New Name" in real-time

**What to look for:**
- Store name updates instantly
- No refresh needed
- Other notification fields unchanged

### Scenario 3: Filter with Real-Time Updates
**Steps:**
1. Open NotificationsScreen with mixed notification types
2. Apply filter (e.g., ORDERS only)
3. Have a member join a store
4. **Expected**: Filtered notifications update member_count in real-time

**What to look for:**
- Filter remains active
- Real-time updates still work
- No performance issues

### Scenario 4: New Notification with Correct Member Count
**Steps:**
1. Create a co-seller store with 3 members
2. Create a new member invitation notification
3. **Expected**: Notification shows member_count = 3 immediately

**What to look for:**
- New notification appears with correct count
- No delay in showing member count
- Count matches actual store members

## Debugging

### Check Logs
```
NotificationViewModel: Real-time update: X notifications (filter: ALL)
NotificationViewModel: Filtered notifications to category: ORDERS (X results)
```

### Verify Listener is Active
- Open NotificationsScreen
- Check logcat for "Real-time update" messages
- Should see updates when data changes

### Test Member Count Accuracy
- Use CoSellerMemberCountManager to verify store member count
- Compare with notification member_count
- Should match exactly

## Performance Checklist

✅ No lag when notifications update
✅ Filter changes are instant
✅ Multiple notifications update smoothly
✅ No memory leaks (listener stops on screen close)
✅ Works with 10+ notifications

## Common Issues & Solutions

**Issue**: Member count not updating
- **Solution**: Check if listener is active (check logs)
- **Solution**: Verify store member count is correct
- **Solution**: Restart app to reset listener

**Issue**: Filter not working with real-time updates
- **Solution**: Filter is applied client-side, should work instantly
- **Solution**: Check if notification category is correct

**Issue**: Notifications disappear after update
- **Solution**: This is normal if notification is deleted
- **Solution**: Check Firestore to verify notification exists

## Success Indicators

✅ Member count updates without refresh
✅ Store name updates without refresh
✅ Filter works with real-time updates
✅ New notifications show correct data
✅ No crashes or errors
✅ Smooth performance

## Files to Monitor

- `app/src/main/java/com/gcuf/craftoria/viewmodel/NotificationViewModel.kt` - Real-time listener
- `app/src/main/java/com/gcuf/craftoria/ui/screens/notifications/NotificationsScreen.kt` - Display
- Firestore `notifications` collection - Data source

## Next Steps

1. Deploy to staging
2. Run through all test scenarios
3. Monitor logs for errors
4. Deploy to production
5. Monitor user feedback
