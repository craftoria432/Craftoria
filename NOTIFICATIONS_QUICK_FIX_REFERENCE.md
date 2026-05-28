# Notifications Not Displaying - Quick Fix Reference

## What Was Fixed
The Notifications screen was showing "5 unread" but displaying "No notifications yet" because the real-time listener wasn't being set up properly.

## The Problem in 3 Points

1. **Overly Strict Validation**: If user ID was blank on first load, the listener was never created
2. **Aggressive Guard**: Even if user ID became valid later, the guard prevented listener re-attachment
3. **No Loading State**: Screen showed empty state immediately instead of loading indicator

## The Solution

### Before (Broken)
```kotlin
fun loadNotifications(userId: String) {
    if (userId.isBlank()) {
        _uiState.value = NotificationUiState.Error("User ID is empty")
        return  // ❌ Blocks listener setup
    }
    
    if (currentUserId == userId && notificationsListener != null) {
        return  // ❌ Prevents retry
    }
    
    // Set up listener...
}
```

### After (Fixed)
```kotlin
fun loadNotifications(userId: String) {
    if (userId.isBlank()) {
        Log.w(TAG, "loadNotifications called with blank user ID")
        // ✅ Continue anyway - don't block
    }
    
    if (currentUserId == userId && notificationsListener != null) {
        return  // ✅ Only skip if already listening for same user
    }
    
    if (currentUserId != userId) {
        notificationsListener?.remove()  // ✅ Clean up old listener
        notificationsListener = null
    }
    
    _uiState.value = NotificationUiState.Loading  // ✅ Show loading state
    
    // Set up listener...
}
```

## Key Changes

| Aspect | Before | After |
|--------|--------|-------|
| Blank User ID | Blocks listener | Logs warning, continues |
| Listener Guard | Prevents retry | Allows retry on user change |
| Loading State | Not set | Set to Loading |
| Old Listener | Not removed | Properly removed |

## Testing Checklist

- [ ] Open Notifications screen → shows loading indicator
- [ ] Notifications appear after 1-2 seconds
- [ ] Unread count matches displayed notifications
- [ ] Switch users → old notifications cleared
- [ ] Re-open screen → no duplicate listeners
- [ ] Empty notifications → shows "No notifications yet"

## Files Modified

- `app/src/main/java/com/gcuf/craftoria/viewmodel/NotificationViewModel.kt`
  - `startListening()` function
  - `loadNotifications()` function

## Verification

Check logs for:
```
D/NotificationViewModel: Setting up real-time listener for notifications: [userId]
D/NotificationViewModel: Snapshot received for user: [userId], document count: [count]
D/NotificationViewModel: Real-time update: [count] notifications loaded
```

If notifications still don't show, check:
1. User ID is not blank (check logs for warning)
2. Firestore has notifications for this user
3. Firestore rules allow read access
4. Network connection is active
