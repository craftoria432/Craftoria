# Notifications Not Displaying - Root Cause & Fix

## Problem
The Notifications screen shows "5 unread" in the header but displays "No notifications yet" in the content area. This indicates:
- Badge count listener is working (showing unread count)
- Full notifications listener is NOT working (showing empty state)

## Root Cause Analysis

### Issue 1: Overly Strict User ID Validation
The `loadNotifications()` and `startListening()` functions had this check:

```kotlin
if (userId.isBlank()) {
    Log.e(TAG, "Cannot load notifications: user ID is empty or blank")
    _error.value = "User ID is empty"
    _uiState.value = NotificationUiState.Error("User ID is empty")
    _isLoading.value = false
    return  // ❌ BLOCKS LISTENER SETUP
}
```

**Problem**: If the user ID is blank on the first call, the listener is never set up. Even if the user ID becomes valid later, the guard prevents re-attachment:

```kotlin
if (currentUserId == userId && notificationsListener != null) {
    Log.d(TAG, "Already listening for notifications: $userId")
    return  // ❌ PREVENTS RETRY
}
```

### Issue 2: Listener Guard Too Aggressive
The guard `if (currentUserId == userId && notificationsListener != null)` prevents re-attachment even when:
- The listener was removed due to an error
- The user ID changed
- The screen is being re-opened

### Issue 3: Separate Badge & Full Notification Listeners
- `startListening()` sets up badge count listener (working)
- `loadNotifications()` sets up full notifications listener (broken)
- They use the same `currentUserId` variable, causing conflicts

## Solution

### Fix 1: Remove Overly Strict Blank Check
Changed from blocking error to warning log:

```kotlin
if (userId.isBlank()) {
    Log.w(TAG, "loadNotifications called with blank user ID - listener may not work correctly")
    // Continue anyway - don't block listener setup
}
```

### Fix 2: Improve Listener Guard Logic
```kotlin
// ✅ Only skip if already listening for the SAME user
if (currentUserId == userId && notificationsListener != null) {
    Log.d(TAG, "Already listening for notifications: $userId")
    return
}

// ✅ If user changed, remove old listener
if (currentUserId != userId) {
    Log.d(TAG, "User ID changed from $currentUserId to $userId, removing old listener")
    notificationsListener?.remove()
    notificationsListener = null
}
```

### Fix 3: Set Loading State Immediately
```kotlin
_isLoading.value = true
_uiState.value = NotificationUiState.Loading  // ✅ Show loading state
```

This ensures the screen shows a loading indicator instead of empty state while the listener is being set up.

## Changes Made

### File: `app/src/main/java/com/gcuf/craftoria/viewmodel/NotificationViewModel.kt`

#### Change 1: `startListening()` function
- Removed `if (userId.isBlank())` error block
- Added warning log instead
- Improved listener guard to handle user ID changes
- Properly removes old listener when user changes

#### Change 2: `loadNotifications()` function
- Removed `if (userId.isBlank())` error block
- Added warning log instead
- Improved listener guard to handle user ID changes
- Added `_uiState.value = NotificationUiState.Loading` for better UX
- Properly removes old listener when user changes

## Testing

### Test Case 1: Initial Load
1. Open Notifications screen
2. Verify loading indicator appears
3. Verify notifications load after 1-2 seconds
4. Verify unread count matches displayed notifications

### Test Case 2: User ID Changes
1. Log out and log in as different user
2. Open Notifications screen
3. Verify old notifications are cleared
4. Verify new user's notifications load

### Test Case 3: Screen Re-open
1. Open Notifications screen
2. Close it
3. Re-open it
4. Verify notifications still display (no duplicate listeners)

### Test Case 4: Empty Notifications
1. Create a user with no notifications
2. Open Notifications screen
3. Verify "No notifications yet" message appears
4. Verify unread count is 0

## Expected Behavior After Fix

✅ Notifications screen always shows loading state initially
✅ Notifications load from Firestore real-time listener
✅ Badge count and full notifications stay in sync
✅ Switching users properly cleans up old listeners
✅ Re-opening screen doesn't create duplicate listeners
✅ Empty state only shows when truly no notifications exist

## Logs to Monitor

When debugging, check for these log messages:

```
D/NotificationViewModel: Setting up real-time listener for notifications: [userId]
D/NotificationViewModel: Snapshot received for user: [userId], document count: [count]
D/NotificationViewModel: Real-time update: [count] notifications loaded for user: [userId]
D/NotificationViewModel: Applied filter: ALL, results: [count] notifications
```

If you see these warnings, the user ID might be invalid:
```
W/NotificationViewModel: loadNotifications called with blank user ID - listener may not work correctly
```

## Related Files
- `NotificationRepository.kt` - Handles Firestore queries
- `NotificationsScreen.kt` - UI layer
- `Notification.kt` - Data model
