# Badge System Quick Reference Guide

## 🎯 How to Use the Professional Badge System

### Pre-Built Badge Components

#### For Bottom Navigation
```kotlin
// Buyer Navigation
BottomNavigationBar(
    items = navItems,
    selectedRoute = selectedRoute,
    wishlistCount = wishlistCount,        // ✅ Shows pink badge when > 0
    pendingOrdersCount = pendingOrdersCount, // ✅ Shows orange pulsing badge
    onItemClick = { route -> /* handle */ }
)

// Seller Navigation  
SellerBottomNavigation(
    selectedRoute = selectedRoute,
    newOrdersCount = newOrdersCount,      // ✅ Shows deep orange pulsing badge
    onNavigate = { route -> /* handle */ }
)
```

#### For Top Bars
```kotlin
CraftoriaTopBar(
    title = "Screen Title",
    showCart = true,                      // ✅ Shows green cart badge
    showNotifications = true,             // ✅ Shows priority-based notification badge
    onCartClick = { /* handle */ },
    onNotificationsClick = { /* handle */ }
)
```

### Custom Badge Usage

#### Simple Custom Badge
```kotlin
CustomBadge(
    count = 5,
    color = Color(0xFFE91E63),           // Pink
    modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(x = 8.dp, y = (-8).dp)
)
```

#### Animated Custom Badge
```kotlin
CustomBadge(
    count = 3,
    color = Color(0xFFFF5722),           // Deep Orange
    shouldPulse = true,                  // ✅ Enables pulsing animation
    priority = BadgeManager.BadgePriority.HIGH,
    modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(x = 8.dp, y = (-8).dp)
)
```

## 🎨 Professional Color Palette

### Badge Colors by Category
```kotlin
// Shopping & Commerce
val cartColor = Color(0xFF4CAF50)        // Green
val wishlistColor = Color(0xFFE91E63)    // Pink

// Orders & Activity
val ordersColor = Color(0xFFFF9800)      // Orange  
val sellerOrdersColor = Color(0xFFFF5722) // Deep Orange

// Communication
val messagesColor = Color(0xFF2196F3)    // Blue
val notificationsColor = /* Dynamic */   // Based on priority

// Business
val productsColor = Color(0xFF9C27B0)    // Purple
val paymentsColor = Color(0xFF4CAF50)    // Green
```

### Priority Colors
```kotlin
val urgentColor = Color(0xFFD32F2F)      // Red
val highColor = Color(0xFFFF5722)        // Deep Orange
val mediumColor = Color(0xFFFF9800)      // Orange
val lowColor = Color(0xFF2196F3)         // Blue
```

## ⚡ Animation Guidelines

### When to Use Pulsing
- **Orders**: When user action needed (pending, processing)
- **Messages**: When unread messages exist
- **Notifications**: For recent notifications (< 30 seconds)
- **Payments**: For payment-related notifications
- **Seller Orders**: For new orders requiring attention

### When NOT to Use Pulsing
- **Wishlist**: Static count display
- **Cart**: Static count display  
- **Profile**: No badges typically needed
- **Completed Items**: No urgency needed

## 📱 Positioning Guidelines

### Bottom Navigation
```kotlin
modifier = Modifier
    .align(Alignment.TopEnd)
    .offset(x = 8.dp, y = (-8).dp)      // Standard positioning
```

### Top Bar Icons
```kotlin
modifier = Modifier
    .align(Alignment.TopEnd)
    .offset(x = (-4).dp, y = 4.dp)      // Adjusted for top bar
```

### Card/List Items
```kotlin
modifier = Modifier
    .align(Alignment.TopEnd)
    .offset(x = (-8).dp, y = 8.dp)      // Inside card boundaries
```

## 🔧 Troubleshooting

### Wishlist Badge Not Showing
1. Ensure `wishlistCount` parameter is passed correctly
2. Check that `WishlistViewModel.initForUser(userId)` is called
3. Verify user ID is not empty
4. Check debug logs: "Wishlist count updated: X"

### Badge Not Animating
1. Verify `shouldPulse = true` is set
2. Check that trigger condition is met (count > 0)
3. Ensure proper priority is set for animation type

### Wrong Colors
1. Use the professional color palette above
2. Match colors to badge category/purpose
3. Consider accessibility and contrast

## 🎯 Best Practices

### Do's ✅
- Use consistent positioning (8dp offset for bottom nav)
- Apply appropriate colors for each category
- Enable pulsing for attention-needed items
- Keep badge counts accurate and real-time
- Use priority-based styling

### Don'ts ❌
- Don't use random colors - stick to the palette
- Don't animate everything - only when needed
- Don't make badges too large or intrusive
- Don't forget to handle zero counts (hide badge)
- Don't use badges for decorative purposes only

## 📊 Implementation Checklist

### For New Screens
- [ ] Identify what needs badges (orders, messages, etc.)
- [ ] Choose appropriate colors from palette
- [ ] Determine if animation is needed
- [ ] Position badges consistently
- [ ] Test with various count values (0, 1, 10, 99+)
- [ ] Verify real-time updates work

### For Existing Screens
- [ ] Replace old badge implementations
- [ ] Update colors to professional palette
- [ ] Add animations where appropriate
- [ ] Test badge positioning and sizing
- [ ] Ensure consistent behavior across app

This badge system provides a professional, consistent, and user-friendly experience across the entire application.