# Badge Integration Checklist

## ✅ COMPLETED

### Core Infrastructure
- [x] BadgeManager.kt - Centralized badge count management
- [x] CraftoriaTopBar.kt - Updated with cart badge support
- [x] Order.kt - Added `isViewed` field for tracking viewed orders
- [x] OrderRepository.kt - Added `markOrderAsViewed()` function
- [x] BottomNavigationBar.kt - Already has wishlist and pending orders badges
- [x] SellerBottomNavigation.kt - Already has new orders and negotiations badges

### Buyer App - HomeScreen
- [x] Cart badge in top bar (RED - #E53935)
- [x] Unread messages badge in top bar (RED - #E53935)
- [x] Wishlist badge in bottom nav (RED - #E53935)
- [x] Orders badge in bottom nav (ORANGE - #FF9800)

---

## 🔄 IN PROGRESS - INTEGRATION STEPS

### Step 1: Update HomeScreen with Pending Orders Count
**File**: `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/HomeScreen.kt`

```kotlin
// Add OrderViewModel
val orderViewModel: OrderViewModel = viewModel()

// In LaunchedEffect, load buyer orders
LaunchedEffect(currentUserId) {
    if (currentUserId.isNotBlank()) {
        orderViewModel.loadUserOrders(currentUserId)
    }
}

// Collect pending orders count
val orders by orderViewModel.orders.collectAsState()
val pendingOrdersCount = orders.count { 
    it.status in listOf("pending", "processing", "shipped") 
}

// Pass to BottomNavigationBar
BottomNavigationBar(
    items = navItems,
    selectedRoute = selectedNavRoute,
    wishlistCount = wishlistCount,
    pendingOrdersCount = pendingOrdersCount,  // ← UPDATE THIS
    onItemClick = { route -> ... }
)
```

### Step 2: Update MyOrdersScreen to Mark Orders as Viewed
**File**: `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt`

When opening order details:
```kotlin
LaunchedEffect(selectedOrderId) {
    if (selectedOrderId != null) {
        orderRepository.markOrderAsViewed(selectedOrderId)
    }
}
```

### Step 3: Update SellerDashboardScreen with New Orders Count
**File**: `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerDashboardScreen.kt`

```kotlin
// Add OrderViewModel
val orderViewModel: OrderViewModel = viewModel()

// In LaunchedEffect, load seller orders
LaunchedEffect(currentUserId) {
    if (currentUserId.isNotBlank()) {
        orderRepository.getSellerOrders(currentUserId)
    }
}

// Count new unviewed orders
val newOrdersCount = orders.count { 
    it.status in listOf("pending", "confirmed") && !it.isViewed 
}

// Pass to SellerBottomNavigation
SellerBottomNavigation(
    selectedRoute = "seller_dashboard",
    onNavigate = { route -> ... },
    newOrdersCount = newOrdersCount,  // ← UPDATE THIS
    pendingNegotiationsCount = 0
)
```

### Step 4: Update SellerMessagesScreen with Unread Count
**File**: `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerMessagesScreen.kt`

```kotlin
// Add UnreadMessageViewModel
val unreadViewModel: UnreadMessageViewModel = viewModel()

// In LaunchedEffect
LaunchedEffect(currentUserId) {
    if (currentUserId.isNotBlank()) {
        unreadViewModel.startListening(currentUserId)
    }
}

// Collect unread count
val unreadCount by unreadViewModel.unreadCount.collectAsState()

// Show badge on messages icon
BadgedBox(
    badge = {
        if (unreadCount > 0) {
            Badge(containerColor = Color(0xFF2196F3), contentColor = Color.White) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
) {
    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Messages")
}
```

### Step 5: Update ChatScreen to Mark Messages as Read
**File**: `app/src/main/java/com/gcuf/craftoria/ui/screens/chat/ChatScreen.kt`

When user opens chat:
```kotlin
LaunchedEffect(chatId) {
    if (chatId.isNotBlank()) {
        chatRepository.markMessagesAsRead(chatId, currentUserId)
    }
}
```

---

## 📋 REMAINING TASKS

### Buyer App Screens
- [ ] **HomeScreen** - Add pending orders count to bottom nav badge
- [ ] **MyOrdersScreen** - Mark order as viewed when opening details
- [ ] **CartScreen** - Already shows cart count in top bar
- [ ] **WishlistScreen** - Already shows wishlist count in bottom nav
- [ ] **ChatScreen** - Mark messages as read when opened

### Seller App Screens
- [ ] **SellerDashboardScreen** - Add new orders count to bottom nav badge
- [ ] **SellerMessagesScreen** - Add unread messages count
- [ ] **NegotiationRequestsScreen** - Add pending negotiations count
- [ ] **ManageProductsScreen** - No badge needed

### Notifications
- [ ] **NotificationsScreen** - Add unread notifications badge (future)
- [ ] **ProfileScreen** - Show notifications badge if needed

---

## 🎨 Badge Color Reference

| Component | Color | Hex | Usage |
|-----------|-------|-----|-------|
| Cart | Red | #E53935 | Items in cart |
| Orders (Buyer) | Orange | #FF9800 | Pending/Processing orders |
| Orders (Seller) | Red | #E53935 | New unviewed orders |
| Wishlist | Red | #E53935 | Wishlist items |
| Messages | Blue | #2196F3 | Unread messages |
| Negotiations | Blue | #2196F3 | Pending negotiations |
| Notifications | Red | #E53935 | Unread notifications |

---

## 🧪 Testing Checklist

### Buyer App
- [ ] Cart badge shows/hides correctly
- [ ] Cart badge updates in real-time
- [ ] Orders badge shows pending count
- [ ] Orders badge clears when order completes
- [ ] Wishlist badge shows/hides correctly
- [ ] Messages badge shows unread count
- [ ] All badges show "9+" for counts > 9

### Seller App
- [ ] Orders badge shows new unviewed orders
- [ ] Orders badge clears when order is viewed
- [ ] Messages badge shows unread count
- [ ] Negotiations badge ready for implementation
- [ ] All badges update in real-time

---

## 📝 Implementation Notes

1. **Real-time Updates**: All badges use StateFlow/LiveData for real-time updates
2. **No Manual Refresh**: Badges update automatically via Firebase listeners
3. **Performance**: Badge counts calculated efficiently without extra queries
4. **Accessibility**: All badges have proper content descriptions
5. **Responsive**: Badges hide when count = 0, show "9+" for large counts

---

## 🚀 Deployment Order

1. Update HomeScreen with pending orders count
2. Update MyOrdersScreen to mark orders as viewed
3. Update SellerDashboardScreen with new orders count
4. Update ChatScreen to mark messages as read
5. Test all badges in both buyer and seller apps
6. Deploy to production

---

## Status: 🔄 READY FOR INTEGRATION

All core infrastructure is complete. Next: Integrate badges into individual screens following the steps above.
