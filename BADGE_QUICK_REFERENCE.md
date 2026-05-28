# Count Badge System - Quick Reference

## 🎯 What Was Implemented

Professional count badge system for Craftoria app showing real-time counts for:
- Cart items (Buyer)
- Pending orders (Buyer & Seller)
- Wishlist items (Buyer)
- Unread messages (Buyer & Seller)
- Pending negotiations (Seller)

---

## 📍 Badge Locations

### Buyer App
```
┌─────────────────────────────────────┐
│  🔍  🔔  💬  🛒                      │  ← Top Bar
│  (Search) (Notif) (Messages) (Cart) │
│  Cart Badge: RED                    │
│  Messages Badge: RED                │
└─────────────────────────────────────┘
│                                     │
│         Home Screen Content         │
│                                     │
├─────────────────────────────────────┤
│ 🏠  📦  ❤️  👤                       │  ← Bottom Nav
│ Home Orders Wishlist Profile        │
│        ORANGE  RED                  │
│        Badge   Badge                │
└─────────────────────────────────────┘
```

### Seller App
```
┌─────────────────────────────────────┐
│  🔍  🔔  💬                          │  ← Top Bar
│  (Search) (Notif) (Messages)        │
│  Messages Badge: BLUE               │
└─────────────────────────────────────┘
│                                     │
│      Dashboard Screen Content       │
│                                     │
├─────────────────────────────────────┤
│ 📊  ➕  📦  👤                       │  ← Bottom Nav
│ Dashboard Add Orders Profile        │
│              RED   BLUE             │
│              Badge Badge            │
└─────────────────────────────────────┘
```

---

## 🎨 Badge Colors

| Color | Hex | Usage |
|-------|-----|-------|
| 🔴 Red | #E53935 | Urgent (New orders, Cart, Wishlist) |
| 🟠 Orange | #FF9800 | Pending (Processing orders) |
| 🔵 Blue | #2196F3 | Information (Messages, Negotiations) |

---

## 📂 Files Modified/Created

### Created Files
1. **BadgeManager.kt** - Centralized badge management
2. **COUNT_BADGE_IMPLEMENTATION_GUIDE.md** - Detailed guide
3. **BADGE_INTEGRATION_CHECKLIST.md** - Integration steps
4. **BADGE_SYSTEM_COMPLETE.md** - Complete documentation
5. **BADGE_QUICK_REFERENCE.md** - This file

### Modified Files
1. **CraftoriaTopBar.kt** - Added cart badge support
2. **Order.kt** - Added `isViewed` field
3. **OrderRepository.kt** - Added `markOrderAsViewed()` function
4. **HomeScreen.kt** - Integrated pending orders count
5. **SellerDashboardScreen.kt** - Updated new orders logic

### Already Had Badge Support
1. **BottomNavigationBar.kt** - Wishlist & pending orders badges
2. **SellerBottomNavigation.kt** - New orders & negotiations badges

---

## 🔄 How It Works

### Real-time Flow
```
1. User Action (Add to cart, New order arrives, etc.)
   ↓
2. Firebase Firestore Updated
   ↓
3. Repository Listener Triggered
   ↓
4. ViewModel StateFlow Updated
   ↓
5. Composable Recomposes
   ↓
6. Badge Count Updated on Screen
```

### Example: New Order Badge (Seller)
```kotlin
// 1. Firestore listener detects new order
.whereEqualTo("seller_id", user.id)
.whereIn("status", listOf("pending", "confirmed"))

// 2. Count unviewed orders
newOrdersCount = snapshot.documents.count { doc ->
    doc.getBoolean("is_viewed") != true
}

// 3. Badge updates automatically
SellerBottomNavigation(
    newOrdersCount = newOrdersCount  // ← Updates in real-time
)
```

---

## ✅ Implementation Status

| Component | Status | Location |
|-----------|--------|----------|
| Cart Badge | ✅ Complete | HomeScreen Top Bar |
| Messages Badge | ✅ Complete | HomeScreen Top Bar |
| Orders Badge (Buyer) | ✅ Complete | HomeScreen Bottom Nav |
| Wishlist Badge | ✅ Complete | HomeScreen Bottom Nav |
| Orders Badge (Seller) | ✅ Complete | SellerDashboardScreen |
| Negotiations Badge | ✅ Complete | SellerDashboardScreen |
| Mark Order Viewed | ✅ Complete | OrderRepository |
| Real-time Updates | ✅ Complete | All ViewModels |

---

## 🚀 Quick Start

### For Developers

1. **View Badge Manager**
   ```
   app/src/main/java/com/gcuf/craftoria/utils/BadgeManager.kt
   ```

2. **Check Badge Locations**
   - Buyer: HomeScreen.kt
   - Seller: SellerDashboardScreen.kt

3. **Add New Badge**
   - Add function to BadgeManager.kt
   - Add parameter to component
   - Pass count from ViewModel

### For Testing

1. **Cart Badge**
   - Add items to cart → Badge updates
   - Remove items → Badge updates
   - Clear cart → Badge disappears

2. **Orders Badge**
   - Create new order → Badge appears
   - View order → Badge clears
   - Complete order → Badge disappears

3. **Messages Badge**
   - Receive message → Badge appears
   - Open chat → Badge clears
   - New message → Badge updates

---

## 🔧 Common Tasks

### Mark Order as Viewed
```kotlin
// In OrderRepository
suspend fun markOrderAsViewed(orderId: String): Result<Unit> {
    return try {
        ordersCollection.document(orderId)
            .update(mapOf("is_viewed" to true))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// In Screen
LaunchedEffect(orderId) {
    orderRepository.markOrderAsViewed(orderId)
}
```

### Get Badge Count
```kotlin
// Using BadgeManager
val cartCount = BadgeManager.getCartCount()
val pendingOrders = BadgeManager.getBuyerPendingOrdersCount()
val unreadMessages = BadgeManager.getUnreadMessagesCount()
```

### Update Badge in Real-time
```kotlin
// All badges use StateFlow
val count by viewModel.count.collectAsState()

// Automatically updates when data changes
Badge(containerColor = Color.Red) {
    Text(count.toString())
}
```

---

## 📊 Badge Count Logic

### Buyer Orders Badge
```kotlin
orders.count { it.status in listOf("pending", "processing", "shipped") }
```

### Seller Orders Badge
```kotlin
orders.count { 
    it.status in listOf("pending", "confirmed") && !it.isViewed 
}
```

### Cart Badge
```kotlin
cartItems.size
```

### Wishlist Badge
```kotlin
wishlistItems.size
```

### Messages Badge
```kotlin
unreadMessages.count()
```

---

## 🎯 Key Features

✅ **Real-time Updates** - Badges update instantly
✅ **No Manual Refresh** - Automatic via Firebase listeners
✅ **Clean UI** - Badges hide when count = 0
✅ **Smart Display** - Shows "9+" for counts > 9
✅ **Color Coded** - Different colors for different priorities
✅ **Accessible** - Proper content descriptions
✅ **Performance** - Optimized with StateFlow
✅ **Maintainable** - Centralized in BadgeManager

---

## 📞 Troubleshooting

### Badge Not Showing
- Check if count > 0
- Verify ViewModel is initialized
- Check StateFlow is being collected

### Badge Shows Wrong Count
- Verify filter logic
- Check Firestore query
- Debug data loading

### Badge Not Updating
- Check listener is active
- Verify data is changing
- Check for listener cleanup

---

## 📚 Documentation Files

1. **COUNT_BADGE_IMPLEMENTATION_GUIDE.md** - Detailed implementation
2. **BADGE_INTEGRATION_CHECKLIST.md** - Integration steps
3. **BADGE_SYSTEM_COMPLETE.md** - Complete documentation
4. **BADGE_QUICK_REFERENCE.md** - This quick reference

---

## ✨ Status: PRODUCTION READY

All badge components are implemented, tested, and ready for production deployment.

**Version**: 1.0.0
**Last Updated**: March 12, 2026
**Status**: ✅ Complete & Ready
