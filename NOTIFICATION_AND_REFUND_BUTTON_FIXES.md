# Notification Screen & Refund Button Issues - FIXES

## Issue 1: Unread Notifications Not Showing (5 unread but only old/read showing)

### Root Cause
The `NotificationViewModel.loadNotifications()` loads **ALL** notifications (both read and unread) without filtering. The badge listener correctly counts unread (5), but the screen displays all notifications mixed together, making unread ones hard to find.

### Solution
Add a default filter to show **unread notifications first** when the screen opens, then allow users to switch to "All" or other categories.

**File:** `app/src/main/java/com/gcuf/craftoria/viewmodel/NotificationViewModel.kt`

**Changes:**
1. Add a new state to track if this is the first load
2. Auto-filter to show unread notifications on initial load
3. Allow users to switch to "All" to see everything

```kotlin
// Add this property:
private val _isFirstLoad = MutableStateFlow(true)

// Modify loadNotifications():
fun loadNotifications(userId: String) {
    Log.d(TAG, "Setting up real-time listener for notifications: $userId")
    notificationsListener?.remove()

    // ── Immediate one-time fetch so screen is never blank on open ─────────
    viewModelScope.launch {
        try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("user_id", userId)
                .get()
                .await()
            allNotifications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notification: ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "Immediate fetch: ${allNotifications.size} notifications")
            
            // ✅ FIX: Auto-filter to unread on first load
            if (_isFirstLoad.value) {
                _currentFilter.value = NotificationCategory.UNREAD  // NEW: Show unread first
                _isFirstLoad.value = false
            }
            applyFilter(_currentFilter.value)
        } catch (e: Exception) {
            Log.e(TAG, "Immediate fetch failed", e)
        }
    }

    // ── Real-time listener keeps data fresh after initial load ────────────
    notificationsListener = db.collection("notifications")
        .whereEqualTo("user_id", userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to notifications", error)
                _error.value = error.message
                _uiState.value = NotificationUiState.Error(error.message ?: "Unknown error")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    allNotifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Notification::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing notification: ${doc.id}", e)
                            null
                        }
                    }
                    Log.d(TAG, "Real-time update: ${allNotifications.size} notifications")
                    applyFilter(_currentFilter.value)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing notification snapshot", e)
                    _error.value = e.message
                    _uiState.value = NotificationUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
}

// Modify applyFilter() to handle UNREAD:
private fun applyFilter(category: NotificationCategory) {
    val filtered = when {
        category == NotificationCategory.UNREAD -> {
            // ✅ NEW: Filter to show only unread notifications
            allNotifications.filter { !it.isRead }
        }
        category == NotificationCategory.ALL -> {
            allNotifications
        }
        else -> {
            allNotifications.filter { it.categoryEnum == category }
        }
    }
    // Sort newest first — Firestore listener returns unsorted
    _notifications.value = filtered.sortedByDescending { it.createdAt }
    _uiState.value = when {
        filtered.isNotEmpty() -> NotificationUiState.Success
        category != NotificationCategory.ALL && allNotifications.isNotEmpty() -> NotificationUiState.Success
        else -> NotificationUiState.Empty
    }
}
```

**File:** `app/src/main/java/com/gcuf/craftoria/data/model/Notification.kt`

Add UNREAD to NotificationCategory enum:
```kotlin
enum class NotificationCategory {
    ALL,
    UNREAD,  // ✅ NEW
    ORDERS,
    MESSAGES,
    PROMOTIONS,
    PAYMENTS,
    SYSTEM
}
```

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/notifications/NotificationsScreen.kt`

Update filter tabs to include UNREAD:
```kotlin
@Composable
fun NotificationFilterTabs(
    currentFilter: NotificationCategory,
    onFilterSelected: (NotificationCategory) -> Unit
) {
    val filters = listOf(
        NotificationCategory.UNREAD to "Unread",  // ✅ NEW: Show first
        NotificationCategory.ALL to "All",
        NotificationCategory.ORDERS to "Orders",
        NotificationCategory.MESSAGES to "Messages",
        NotificationCategory.PROMOTIONS to "Promotions",
        NotificationCategory.PAYMENTS to "Payments",
        NotificationCategory.SYSTEM to "System"
    )
    // ... rest of code
}
```

---

## Issue 2: Refund Pending Button Loading Briefly Then Showing

### Root Cause
The "Request Refund" button doesn't have a loading state indicator. When clicked, it navigates to the refund screen which may take a moment to load, creating a brief visual delay.

### Solution
Add a loading state to the button and disable it while processing.

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt`

**Changes:**
```kotlin
// Add state for refund button loading:
var isRequestingRefund by remember { mutableStateOf(false) }

// Update the Request Refund button:
if (daysSinceDelivery <= 30) {
    // Within 30 days - show Request Refund button
    OutlinedButton(
        onClick = {
            isRequestingRefund = true
            onRequestRefund()
            // Reset after navigation
            isRequestingRefund = false
        },
        modifier = Modifier.weight(1f).height(38.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isRequestingRefund) Color(0xFFFF6B35).copy(alpha = 0.6f) else Color(0xFFFF6B35)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, 
            if (isRequestingRefund) Color(0xFFFF6B35).copy(alpha = 0.6f) else Color(0xFFFF6B35)
        ),
        shape = RoundedCornerShape(10.dp),
        enabled = !isRequestingRefund  // ✅ Disable while loading
    ) {
        if (isRequestingRefund) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 1.5.dp,
                color = Color(0xFFFF6B35)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = if (isRequestingRefund) "Processing..." else "Request Refund",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    OutlinedButton(
        onClick = onReorder,
        modifier = Modifier.weight(1f).height(38.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
        shape = RoundedCornerShape(10.dp),
        enabled = !isRequestingRefund  // ✅ Disable while refund is processing
    ) { Text(text = "Reorder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}
```

---

## Summary of Changes

### Issue 1: Unread Notifications
- ✅ Add `UNREAD` category to `NotificationCategory` enum
- ✅ Add `_isFirstLoad` state to track first load
- ✅ Auto-filter to unread notifications on screen open
- ✅ Update filter tabs to show "Unread" first
- ✅ Modify `applyFilter()` to handle unread filtering

### Issue 2: Refund Button Loading
- ✅ Add `isRequestingRefund` state to track button state
- ✅ Show loading indicator (spinner) while processing
- ✅ Disable button while loading
- ✅ Change text to "Processing..." during load
- ✅ Disable Reorder button while refund is processing

---

**Status:** READY FOR IMPLEMENTATION ✅
**Date:** May 11, 2026
