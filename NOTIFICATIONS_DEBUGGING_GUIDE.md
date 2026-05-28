# Notifications Not Displaying - Debugging Guide

## If Notifications Still Don't Show After Fix

Follow these steps to diagnose the issue:

## Step 1: Check Logs

### Enable Verbose Logging
In `NotificationViewModel.kt`, the logging is already comprehensive. Check logcat for:

```
D/NotificationViewModel: Setting up real-time listener for notifications: [userId]
D/NotificationViewModel: Snapshot received for user: [userId], document count: [count]
D/NotificationViewModel: Real-time update: [count] notifications loaded for user: [userId]
D/NotificationViewModel: Applied filter: ALL, results: [count] notifications
```

### Look for Error Logs
```
E/NotificationViewModel: Error listening to notifications for user: [userId]
E/NotificationViewModel: Error parsing notification: [docId]
E/NotificationViewModel: Error processing notification snapshot
```

### Check for Warnings
```
W/NotificationViewModel: loadNotifications called with blank user ID
W/NotificationViewModel: No notifications found for user: [userId]
W/NotificationViewModel: Snapshot is null for user: [userId]
```

## Step 2: Verify User ID

Add this debug code to NotificationsScreen:

```kotlin
LaunchedEffect(user.id) {
    Log.d("NotificationsScreen", "User ID: '${user.id}'")
    Log.d("NotificationsScreen", "User ID is blank: ${user.id.isBlank()}")
    Log.d("NotificationsScreen", "User ID length: ${user.id.length}")
    notificationViewModel.loadNotifications(user.id)
    notificationViewModel.startListening(user.id)
}
```

Expected output:
```
D/NotificationsScreen: User ID: 'abc123def456'
D/NotificationsScreen: User ID is blank: false
D/NotificationsScreen: User ID length: 12
```

If user ID is blank or empty, the issue is in the authentication layer.

## Step 3: Check Firestore Data

### Verify Notifications Exist
1. Open Firebase Console
2. Go to Firestore Database
3. Check `notifications` collection
4. Filter by `user_id` = [your user ID]
5. Verify documents exist

Expected structure:
```json
{
  "user_id": "abc123def456",
  "title": "Order Confirmed",
  "description": "Your order has been confirmed",
  "category": "ORDERS",
  "is_read": false,
  "created_at": 1234567890000,
  "action_type": "VIEW_ORDER",
  ...
}
```

### Check Firestore Rules
Verify read access is allowed:

```
match /notifications/{document=**} {
  allow read: if request.auth.uid == resource.data.user_id;
  allow write: if request.auth.uid == resource.data.user_id || request.auth.uid == resource.data.sender_id;
}
```

## Step 4: Check Network Connection

Add this to NotificationsScreen:

```kotlin
LaunchedEffect(Unit) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    Log.d("NotificationsScreen", "Network connected: $isConnected")
}
```

## Step 5: Check Listener Attachment

Add this debug code to NotificationViewModel:

```kotlin
fun loadNotifications(userId: String) {
    Log.d(TAG, "loadNotifications called with userId: '$userId'")
    Log.d(TAG, "currentUserId: '$currentUserId'")
    Log.d(TAG, "notificationsListener is null: ${notificationsListener == null}")
    
    // ... rest of function
    
    notificationsListener = db.collection("notifications")
        .whereEqualTo("user_id", userId)
        .addSnapshotListener { snapshot, error ->
            Log.d(TAG, "Listener callback triggered")
            Log.d(TAG, "Error: $error")
            Log.d(TAG, "Snapshot size: ${snapshot?.size()}")
            // ... rest of callback
        }
}
```

Expected output:
```
D/NotificationViewModel: loadNotifications called with userId: 'abc123def456'
D/NotificationViewModel: currentUserId: 'null'
D/NotificationViewModel: notificationsListener is null: true
D/NotificationViewModel: Listener callback triggered
D/NotificationViewModel: Error: null
D/NotificationViewModel: Snapshot size: 5
```

## Step 6: Check Filter Logic

If notifications load but don't display, the filter might be wrong:

```kotlin
private fun applyFilter(category: NotificationCategory) {
    Log.d(TAG, "applyFilter called with category: $category")
    Log.d(TAG, "allNotifications size: ${allNotifications.size}")
    
    val filtered = when {
        category == NotificationCategory.UNREAD -> {
            Log.d(TAG, "Filtering for UNREAD")
            allNotifications.filter { !it.isRead }
        }
        category == NotificationCategory.ALL -> {
            Log.d(TAG, "Filtering for ALL")
            allNotifications
        }
        else -> {
            Log.d(TAG, "Filtering for category: $category")
            allNotifications.filter { it.categoryEnum == category }
        }
    }
    
    Log.d(TAG, "Filtered results: ${filtered.size}")
    _notifications.value = filtered.sortedByDescending { it.createdAt }
}
```

## Step 7: Check UI State

If data loads but doesn't display, check UI state:

```kotlin
LaunchedEffect(uiState) {
    Log.d("NotificationsScreen", "UI State: $uiState")
}

LaunchedEffect(notifications) {
    Log.d("NotificationsScreen", "Notifications count: ${notifications.size}")
    notifications.forEach { notif ->
        Log.d("NotificationsScreen", "  - ${notif.title} (${notif.category})")
    }
}
```

## Common Issues & Solutions

### Issue: "No notifications found for user"
**Cause**: Firestore has no documents for this user
**Solution**: 
1. Check Firestore console
2. Verify user ID matches exactly
3. Create test notification manually

### Issue: "Snapshot is null"
**Cause**: Firestore listener not receiving data
**Solution**:
1. Check network connection
2. Verify Firestore rules
3. Check user authentication

### Issue: "Error parsing notification"
**Cause**: Notification document has invalid structure
**Solution**:
1. Check Firestore document structure
2. Verify all required fields exist
3. Check field types match model

### Issue: Listener never called
**Cause**: Listener not attached or user ID is blank
**Solution**:
1. Check user ID is not blank
2. Verify loadNotifications() is called
3. Check Firestore rules allow read

### Issue: Notifications load but filter shows empty
**Cause**: Filter category doesn't match notification categories
**Solution**:
1. Check notification category values in Firestore
2. Verify category enum matches
3. Try "ALL" filter first

## Advanced Debugging

### Enable Firestore Logging
```kotlin
FirebaseFirestore.setLoggingEnabled(true)
```

### Check Firestore Cache
```kotlin
db.collection("notifications")
    .whereEqualTo("user_id", userId)
    .get(Source.SERVER)  // Force server fetch, ignore cache
    .addOnSuccessListener { snapshot ->
        Log.d(TAG, "Server fetch: ${snapshot.size()} documents")
    }
```

### Monitor Listener Lifecycle
```kotlin
notificationsListener = db.collection("notifications")
    .whereEqualTo("user_id", userId)
    .addSnapshotListener { snapshot, error ->
        Log.d(TAG, "Listener active: ${System.currentTimeMillis()}")
        // ...
    }

// Later, when stopping:
notificationsListener?.remove()
Log.d(TAG, "Listener removed: ${System.currentTimeMillis()}")
```

## Contact Support

If you've followed all steps and notifications still don't display:

1. Collect all logs from steps 1-7
2. Check Firestore console for data
3. Verify user authentication is working
4. Check network connectivity
5. Review Firestore security rules
6. Test with a different user account
