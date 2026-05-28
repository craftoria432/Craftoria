# Notification Badge Implementation

## 📌 Why Two ViewModels?

### NotificationViewModel (Singular) ✅
**Purpose**: Real-time badge count for notifications
**Location**: `app/src/main/java/com/gcuf/craftoria/viewmodel/NotificationViewModel.kt`

**Features**:
- Real-time listener for unread notifications
- Lightweight - only tracks unread count
- Used for badge display in top bar
- Minimal data - just the count
- Efficient for continuous listening

**Key Methods**:
```kotlin
fun startListening(userId: String)  // Start real-time listener
fun stopListening()                 // Stop listener
val unreadCount: StateFlow<Int>     // Badge count
```

**Use Case**: Show notification badge count in top bar (real-time)

---

### NotificationsViewModel (Plural) ✅
**Purpose**: Full notification management and UI state
**Location**: `app/src/main/java/com/gcuf/craftoria/viewmodel/NotificationsViewModel.kt`

**Features**:
- Load all notifications with filtering
- Mark as read/unread
- Delete notifications
- UI state management (Loading, Success, Error, Empty)
- Category filtering
- Batch operations

**Key Methods**:
```kotlin
fun loadNotifications(userId: String)           // Load all notifications
fun filterNotifications(category, userId)       // Filter by category
fun markAsRead(notificationId, userId)          // Mark single as read
fun markAllAsRead(userId)                       // Mark all as read
fun deleteNotification(notificationId, userId)  // Delete single
fun deleteMultipleNotifications(ids, userId)    // Delete batch
```

**Use Case**: Full notifications screen with filtering, marking, and deletion

---

## 🎯 Comparison

| Feature | NotificationViewModel | NotificationsViewModel |
|---------|----------------------|----------------------|
| **Purpose** | Badge count | Full management |
| **Real-time** | ✅ Yes | ❌ No |
| **Unread count** | ✅ Yes | ✅ Yes |
| **Load all** | ❌ No | ✅ Yes |
| **Filtering** | ❌ No | ✅ Yes |
| **Mark as read** | ❌ No | ✅ Yes |
| **Delete** | ❌ No | ✅ Yes |
| **UI State** | ❌ No | ✅ Yes |
| **Listener** | ✅ Yes | ❌ No |
| **Use Case** | Badge | Screen |

---

## 🔴 Notification Badge Implementation

### Buyer App - Notification Badge

#### HomeScreen Top Bar
```kotlin
// Add NotificationViewModel
val notificationViewModel: NotificationViewModel = viewModel()

// In LaunchedEffect
LaunchedEffect(currentUserId) {
    if (currentUserId.isNotBlank()) {
        notificationViewModel.startListening(currentUserId)
    }
}

// Collect unread count
val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

// In TopAppBar actions
BadgedBox(
    badge = {
        if (unreadNotificationCount > 0) {
            Badge(
                containerColor = Color(0xFFE53935),  // Red
                contentColor = Color.White
            ) {
                Text(
                    text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
) {
    IconButton(onClick = onNavigateToNotifications) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = Color.White
        )
    }
}
```

---

### Seller App - Notification Badge

#### SellerDashboardScreen Top Bar
```kotlin
// Add NotificationViewModel
val notificationViewModel: NotificationViewModel = viewModel()

// In LaunchedEffect
LaunchedEffect(user.id) {
    if (user.id.isNotBlank()) {
        notificationViewModel.startListening(user.id)
    }
}

// Collect unread count
val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

// In TopAppBar actions
BadgedBox(
    badge = {
        if (unreadNotificationCount > 0) {
            Badge(
                containerColor = Color(0xFFE53935),  // Red
                contentColor = Color.White
            ) {
                Text(
                    text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
) {
    IconButton(onClick = onNavigateToNotifications) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = Color.White
        )
    }
}
```

---

## 📊 Updated Badge System (8 Total)

### Buyer App (5 Badges)
| Badge | Location | Color | Shows |
|-------|----------|-------|-------|
| Cart | Top Bar | Red | Items |
| Messages | Top Bar | Red | Unread |
| **Notifications** | **Top Bar** | **Red** | **Unread** |
| Orders | Bottom Nav | Orange | Pending |
| Wishlist | Bottom Nav | Red | Items |

### Seller App (4 Badges)
| Badge | Location | Color | Shows |
|-------|----------|-------|-------|
| **Notifications** | **Top Bar** | **Red** | **Unread** |
| Messages | Top Bar | Blue | Unread |
| Orders | Bottom Nav | Red | New |
| Negotiations | Bottom Nav | Blue | Pending |

---

## 🔄 Real-time Flow

### Notification Badge Update Flow
```
1. New notification created in Firebase
   ↓
2. NotificationViewModel listener triggered
   ↓
3. unreadCount StateFlow updated
   ↓
4. HomeScreen/SellerDashboardScreen recomposes
   ↓
5. Notification badge updates in real-time
```

---

## 🧹 Cleanup

### In onCleared()
```kotlin
override fun onCleared() {
    super.onCleared()
    notificationViewModel.stopListening()  // Stop listener
}
```

---

## 📝 Implementation Checklist

### Buyer App
- [ ] Add NotificationViewModel to HomeScreen
- [ ] Initialize listener in LaunchedEffect
- [ ] Collect unreadNotificationCount
- [ ] Add BadgedBox to TopAppBar
- [ ] Show red badge with count
- [ ] Add cleanup in onCleared()

### Seller App
- [ ] Add NotificationViewModel to SellerDashboardScreen
- [ ] Initialize listener in LaunchedEffect
- [ ] Collect unreadNotificationCount
- [ ] Add BadgedBox to TopAppBar
- [ ] Show red badge with count
- [ ] Add cleanup in onCleared()

---

## 🎨 Badge Design

### Notification Badge
```
┌─────────────────────┐
│  🔔 Notifications   │
│      ┌───┐          │
│      │ 3 │ ← RED    │
│      └───┘          │
│                     │
│  Color: #E53935     │
│  Font: Bold 10sp    │
│  Max: "9+"          │
└─────────────────────┘
```

---

## 🚀 Complete Badge System (8 Badges)

### Buyer App
1. ✅ Cart Badge (Top Bar) - RED
2. ✅ Messages Badge (Top Bar) - RED
3. 🆕 Notifications Badge (Top Bar) - RED
4. ✅ Orders Badge (Bottom Nav) - ORANGE
5. ✅ Wishlist Badge (Bottom Nav) - RED

### Seller App
1. 🆕 Notifications Badge (Top Bar) - RED
2. ✅ Messages Badge (Top Bar) - BLUE
3. ✅ Orders Badge (Bottom Nav) - RED
4. ✅ Negotiations Badge (Bottom Nav) - BLUE

---

## 📊 ViewModel Usage Summary

### For Badge Count (Real-time)
```kotlin
// Use NotificationViewModel (singular)
val notificationViewModel: NotificationViewModel = viewModel()
notificationViewModel.startListening(userId)
val unreadCount by notificationViewModel.unreadCount.collectAsState()
```

### For Full Notifications Screen
```kotlin
// Use NotificationsViewModel (plural)
val notificationsViewModel: NotificationsViewModel = viewModel()
notificationsViewModel.loadNotifications(userId)
val notifications by notificationsViewModel.notifications.collectAsState()
val uiState by notificationsViewModel.uiState.collectAsState()
```

---

## ✅ Status

- [x] NotificationViewModel - Real-time badge count
- [x] NotificationsViewModel - Full notification management
- [x] Notification badge design
- [x] Implementation guide
- [ ] Buyer app integration (Next)
- [ ] Seller app integration (Next)
- [ ] Testing (Next)

---

## 📝 Notes

1. **Two ViewModels for Different Purposes**
   - NotificationViewModel: Lightweight, real-time badge
   - NotificationsViewModel: Full-featured, UI management

2. **Real-time Updates**
   - Notification badge updates instantly
   - No manual refresh needed
   - Efficient listener pattern

3. **Cleanup Important**
   - Always call stopListening() in onCleared()
   - Prevents memory leaks
   - Stops unnecessary Firebase reads

4. **Color Consistency**
   - Notification badge: RED (#E53935)
   - Same as cart and messages
   - Indicates urgent/important

---

**Version**: 1.0.0
**Status**: Ready for Integration
