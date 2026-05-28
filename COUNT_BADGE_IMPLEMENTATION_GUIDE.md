# Count Badge Implementation Guide

## Overview
Professional count badge system for Craftoria app showing real-time counts for important user actions.

---

## Badge Locations & Colors

### BUYER APP

#### 1. Cart Icon (Top Bar) - RED BADGE
- **Location**: Top right corner of screen
- **Shows**: Total cart items count
- **Color**: Red (#E53935)
- **When to show**: When cart has items (count > 0)
- **Max display**: "9+" for counts > 9

#### 2. Orders Tab (Bottom Navigation) - ORANGE BADGE
- **Location**: Bottom navigation "Orders" tab
- **Shows**: Pending/Processing/Shipped orders count
- **Color**: Orange (#FF9800)
- **When to show**: When buyer has active orders needing attention
- **Logic**: Count orders with status = "pending", "processing", or "shipped"

#### 3. Wishlist Tab (Bottom Navigation) - RED BADGE
- **Location**: Bottom navigation "Wishlist" tab
- **Shows**: Total wishlist items count
- **Color**: Red (#E53935)
- **When to show**: When wishlist has items (count > 0)
- **Max display**: "9+" for counts > 9

#### 4. Messages/Chat - BLUE BADGE
- **Location**: Chat icon or Messages screen
- **Shows**: Unread messages count
- **Color**: Blue (#2196F3)
- **When to show**: When there are unread messages
- **Updates**: Real-time via UnreadMessageViewModel

---

### SELLER APP

#### 1. Orders Tab (Bottom Navigation) - RED BADGE
- **Location**: Bottom navigation "Orders" tab
- **Shows**: New unviewed orders count
- **Color**: Red (#E53935)
- **When to show**: When seller has new orders (status = "pending" or "confirmed" AND isViewed = false)
- **Clears**: When seller opens order details

#### 2. Messages - BLUE BADGE
- **Location**: Messages/Chat icon
- **Shows**: Unread messages from buyers
- **Color**: Blue (#2196F3)
- **When to show**: When there are unread messages
- **Updates**: Real-time via UnreadMessageViewModel

#### 3. Negotiations - BLUE BADGE
- **Location**: Profile tab or dedicated negotiations screen
- **Shows**: Pending negotiation requests count
- **Color**: Blue (#2196F3)
- **When to show**: When there are pending negotiations needing response
- **Note**: Currently returns 0 (implement when NegotiationViewModel is ready)

---

## Badge Color System

| Color | Hex Code | Usage | Priority |
|-------|----------|-------|----------|
| **Red** | #E53935 | Urgent actions (New orders, Cart) | High |
| **Orange** | #FF9800 | Pending actions (Processing orders) | Medium |
| **Blue** | #2196F3 | Information (Messages, Negotiations) | Normal |
| **Green** | #4CAF50 | Positive actions (Completed) | Low |

---

## Implementation Files

### 1. BadgeManager.kt ✅
**Location**: `app/src/main/java/com/gcuf/craftoria/utils/BadgeManager.kt`

Centralized badge count management with composable functions:
- `getCartCount()` - Cart items
- `getWishlistCount()` - Wishlist items
- `getBuyerPendingOrdersCount()` - Buyer pending orders
- `getSellerNewOrdersCount()` - Seller new orders
- `getUnreadMessagesCount()` - Unread messages
- `getPendingNegotiationsCount()` - Pending negotiations

### 2. CraftoriaTopBar.kt ✅
**Updated**: Added cart badge support
- New parameters: `showCart`, `cartCount`, `onCartClick`
- Shows red badge with cart count
- Positioned in top right corner

### 3. BottomNavigationBar.kt ✅
**Already has**:
- `wishlistCount` - Wishlist badge (red)
- `pendingOrdersCount` - Orders badge (orange)

### 4. SellerBottomNavigation.kt ✅
**Already has**:
- `newOrdersCount` - New orders badge (red)
- `pendingNegotiationsCount` - Negotiations badge (blue)

### 5. Order.kt ✅
**Updated**: Added `isViewed` field
- Tracks if seller has viewed the order
- Used for new orders badge count
- Defaults to `false` for new orders

---

## Usage Examples

### Buyer Home Screen
```kotlin
@Composable
fun HomeScreen(navController: NavController) {
    val cartViewModel: CartViewModel = viewModel()
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartCount = cartItems.size
    
    Scaffold(
        topBar = {
            CraftoriaTopBar(
                showLogo = true,
                showCart = true,
                cartCount = cartCount,
                onCartClick = { navController.navigate("cart") }
            )
        },
        bottomBar = {
            val wishlistViewModel: WishlistViewModel = viewModel()
            val wishlistItems by wishlistViewModel.wishlistItems.collectAsState()
            
            val orderViewModel: OrderViewModel = viewModel()
            val orders by orderViewModel.buyerOrders.collectAsState()
            val pendingCount = orders.count { 
                it.status in listOf("pending", "processing", "shipped") 
            }
            
            BottomNavigationBar(
                items = navItems,
                selectedRoute = currentRoute,
                onItemClick = { route -> navController.navigate(route) },
                wishlistCount = wishlistItems.size,
                pendingOrdersCount = pendingCount
            )
        }
    ) { paddingValues ->
        // Screen content
    }
}
```

### Seller Dashboard
```kotlin
@Composable
fun SellerDashboardScreen(navController: NavController) {
    val orderViewModel: OrderViewModel = viewModel()
    val orders by orderViewModel.sellerOrders.collectAsState()
    
    // Count new unviewed orders
    val newOrdersCount = orders.count { 
        it.status in listOf("pending", "confirmed") && !it.isViewed 
    }
    
    Scaffold(
        bottomBar = {
            SellerBottomNavigation(
                selectedRoute = "seller_dashboard",
                onNavigate = { route -> navController.navigate(route) },
                newOrdersCount = newOrdersCount,
                pendingNegotiationsCount = 0 // Implement when ready
            )
        }
    ) { paddingValues ->
        // Screen content
    }
}
```

### Mark Order as Viewed
```kotlin
// In OrderRepository.kt
suspend fun markOrderAsViewed(orderId: String) {
    try {
        firestore.collection("orders")
            .document(orderId)
            .update("is_viewed", true)
            .await()
    } catch (e: Exception) {
        Log.e("OrderRepository", "Failed to mark order as viewed", e)
    }
}

// Call when seller opens order details
LaunchedEffect(orderId) {
    orderRepository.markOrderAsViewed(orderId)
}
```

---

## Badge Behavior Rules

### When to Show Badge
1. **Always show** when count > 0
2. **Never show** when count = 0 (badge disappears completely)
3. **Show "9+"** when count > 9 (keeps UI clean)

### When to Clear Badge
1. **Cart**: When items are removed or checkout completed
2. **Wishlist**: When items are removed
3. **Orders (Buyer)**: When order status changes to "delivered" or "completed"
4. **Orders (Seller)**: When seller views order details (isViewed = true)
5. **Messages**: When user opens chat and messages are marked as read
6. **Negotiations**: When seller responds to negotiation

### Real-time Updates
- All badges update in real-time via StateFlow/LiveData
- Firebase listeners ensure instant updates
- No manual refresh needed

---

## Testing Checklist

### Buyer App
- [ ] Cart badge shows correct count
- [ ] Cart badge updates when adding/removing items
- [ ] Cart badge shows "9+" for 10+ items
- [ ] Orders badge shows pending orders count
- [ ] Orders badge clears when orders complete
- [ ] Wishlist badge shows correct count
- [ ] Wishlist badge updates in real-time

### Seller App
- [ ] Orders badge shows new orders count
- [ ] Orders badge clears when order is viewed
- [ ] Orders badge shows red color for urgency
- [ ] Messages badge shows unread count
- [ ] Negotiations badge ready for future implementation

---

## Future Enhancements

1. **Notifications Badge**: Add to profile tab for unread notifications
2. **Dot Badge**: Use simple dot instead of count for less critical items
3. **Animated Badge**: Add pulse animation for new items
4. **Sound/Vibration**: Alert user when new badge appears
5. **Badge History**: Track badge interaction analytics

---

## Notes

- Badge counts are calculated in real-time from ViewModels
- No additional API calls needed (uses existing data streams)
- Badges automatically hide when count reaches 0
- Color scheme follows Material Design guidelines
- Accessible with proper content descriptions
- Performance optimized with StateFlow

---

## Status: ✅ READY FOR IMPLEMENTATION

All core files updated. Next steps:
1. Update individual screens to pass badge counts
2. Test badge visibility and counts
3. Verify real-time updates
4. Add mark-as-viewed functionality for orders
