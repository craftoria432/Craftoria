# Notifications Not Displaying - Diagnosis & Fix

## Problem
No notifications are being displayed in the NotificationsScreen, neither read nor unread.

## Root Cause Analysis

### Issue 1: Real-time Listener Not Properly Initialized
The `loadNotifications()` method in NotificationViewModel sets up a real-time listener, but there's a potential issue:
- The listener is attached to `db.collection("notifications")` 
- The query filters by `user_id` 
- **BUT**: If the user ID is empty or null, the listener will never match any documents

### Issue 2: Missing Null Check on User ID
In NotificationsScreen.kt, the `LaunchedEffect` calls:
```kotlin
LaunchedEffect(user.id) {
    notificationViewModel.loadNotifications(user.id)
    notificationViewModel.startListening(user.id)
}
```

**Problem**: If `user.id` is empty or null, the listener will query for notifications with `user_id = ""` or `user_id = null`, which won't match any documents.

### Issue 3: Listener Not Handling Empty Results
The listener in `loadNotifications()` might be receiving an empty snapshot but not properly handling it. The code should:
1. Check if snapshot is empty
2. Set appropriate UI state
3. Log the issue for debugging

### Issue 4: Filter Application Issue
The `applyFilter()` method filters `allNotifications`, but if `allNotifications` is empty (because the listener didn't find any documents), the filtered list will also be empty.

## Solution

### Step 1: Add Null/Empty Check for User ID
Add validation to ensure user ID is not empty before setting up listeners.

### Step 2: Improve Listener Error Handling
Add better logging and error handling in the listener to diagnose why notifications aren't loading.

### Step 3: Add Debug Logging
Add comprehensive logging to track:
- When listener is attached
- When snapshots are received
- How many documents are found
- What the user ID is

### Step 4: Verify Firestore Data
Ensure that:
- Notifications are actually being written to Firestore
- They have the correct `user_id` field
- The field name matches exactly (case-sensitive)

## Implementation

The fix involves:
1. Adding null/empty checks for user ID
2. Adding comprehensive logging
3. Ensuring the listener properly handles empty snapshots
4. Verifying the Firestore query is correct

## Testing Checklist

- [ ] Verify user ID is not empty when screen loads
- [ ] Check Firestore console to see if notifications exist
- [ ] Check if notifications have correct `user_id` field
- [ ] Check Android logcat for debug messages
- [ ] Verify listener is being attached
- [ ] Verify listener is receiving snapshots
- [ ] Verify notifications are being parsed correctly
