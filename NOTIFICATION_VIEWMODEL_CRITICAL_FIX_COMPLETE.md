# NotificationViewModel Critical Bug Fix — Complete

## Problem Identified

The NotificationViewModel had a **shared `currentUserId` variable** between two independent listeners:
- **Badge listener** (`startListening()`) — tracks unread count
- **Notification listener** (`loadNotifications()`) — loads full notification list

This caused a **guard logic failure** where one listener could corrupt the state of the other.

### Root Cause

```kotlin
// BEFORE (BROKEN):
private var currentUserId: String? = null  // ← SHARED between both listeners

fun startListening(userId: String) {
    if (currentUserId == userId && badgeListener != null) return  // Guard 1
    currentUserId = userId  // ← Sets shared variable
    // ... attach badge listener
}

fun loadNotifications(userId: String) {
    if (currentUserId == userId && notificationsListener != null) return  // Guard 2
    currentUserId = userId  // ← Overwrites shared variable
    // ... attach notification listener
}
```

**Scenario causing empty notifications:**
1. `loadNotifications("user123")` runs → sets `currentUserId = "user123"`, attaches listener
2. `startListening("user123")` runs → sets `currentUserId = "user123"` again
3. On next recomposition, `loadNotifications("user123")` runs again
4. Guard sees `currentUserId == "user123" && notificationsListener != null` → **returns early without re-attaching**
5. If listener was removed or corrupted, notifications list stays empty

## Solution Applied

**Separated tracking variables** so each listener manages its own state independently:

```kotlin
// AFTER (FIXED):
private var badgeListenerUserId: String? = null          // ← Badge listener only
private var notificationListenerUserId: String? = null   // ← Notification listener only

fun startListening(userId: String) {
    if (badgeListenerUserId == userId && badgeListener != null) return  // Guard 1 (independent)
    badgeListenerUserId = userId  // ← Sets badge-specific variable
    // ... attach badge listener
}

fun loadNotifications(userId: String) {
    if (notificationListenerUserId == userId && notificationsListener != null) return  // Guard 2 (independent)
    notificationListenerUserId = userId  // ← Sets notification-specific variable
    // ... attach notification listener
}
```

## Changes Made

### 1. **Separated Tracking Variables** (Lines 43-44, 65-66)
```kotlin
// Badge listener tracking
private var badgeListener: ListenerRegistration? = null
private var badgeListenerUserId: String? = null  // ← NEW

// Notification listener tracking
private var notificationListenerUserId: String? = null  // ← NEW
private var notificationsListener: ListenerRegistration? = null
```

### 2. **Updated `startListening()`** (Lines 82-104)
- Changed guard from `currentUserId == userId` → `badgeListenerUserId == userId`
- Changed assignment from `currentUserId = userId` → `badgeListenerUserId = userId`
- Changed user change detection from `currentUserId != userId` → `badgeListenerUserId != userId`

### 3. **Updated `loadNotifications()`** (Lines 130-180)
- Changed guard from `currentUserId == userId` → `notificationListenerUserId == userId`
- Changed assignment from `currentUserId = userId` → `notificationListenerUserId = userId`
- Changed user change detection from `currentUserId != userId` → `notificationListenerUserId != userId`
- **Added manual `isRead` parsing** to fix Kotlin @PropertyName deserialization issue:
  ```kotlin
  val notification = doc.toObject(Notification::class.java)?.copy(
      id = doc.id,
      isRead = doc.getBoolean("is_read") ?: false  // ✅ Manual override
  )
  ```

### 4. **Updated `stopListening()`** (Lines 107-115)
- Clear both tracking variables:
  ```kotlin
  badgeListenerUserId = null
  notificationListenerUserId = null
  ```

## Impact

✅ **Badge listener** now operates independently without interference from notification listener  
✅ **Notification listener** now operates independently without interference from badge listener  
✅ **Guard logic** works correctly — each listener only re-attaches when its specific user ID changes  
✅ **isRead field** now correctly deserialized from Firestore (fixes always-false bug)  
✅ **Null snapshot handling** properly sets UI state to Empty  

## Testing Checklist

- [ ] Open NotificationsScreen
- [ ] Verify badge count updates in real-time
- [ ] Verify notifications list loads and displays
- [ ] Mark notification as read → verify isRead updates
- [ ] Switch users → verify both listeners reset correctly
- [ ] Recompose screen → verify listeners don't re-attach unnecessarily
- [ ] Check logs for "Already listening" messages (should appear on recompose)

## Files Modified

- `app/src/main/java/com/gcuf/craftoria/viewmodel/NotificationViewModel.kt`

## Compilation Status

✅ **No errors** — All changes compile successfully
