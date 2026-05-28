# 🔔 Notification System Quick Reference

## 🚀 Quick Start

### 1. Send a Notification (Web Admin)
```javascript
import { notifySellerNewOrder } from './services/notificationServiceEnhanced';

// Notify seller about new order
await notifySellerNewOrder(
    "seller123",      // sellerId
    "John Doe",       // buyerName  
    "ORD-12345",      // orderNumber
    299.99,           // totalAmount
    3                 // itemCount
);
```

### 2. Add Badge to UI Component
```kotlin
@Composable
fun MyComponent() {
    Box {
        Icon(Icons.Default.ShoppingCart, "Cart")
        CartBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
        )
    }
}
```

### 3. Custom Badge with Pulsing
```kotlin
CustomBadge(
    count = 5,
    color = Color.Red,
    shouldPulse = true,
    priority = BadgeManager.BadgePriority.HIGH
)
```

## 📋 Available Notification Functions

### Buyer → Seller
```javascript
notifySellerNewMessage(sellerId, buyerName, productTitle, messagePreview)
notifyNegotiationRequest(sellerId, buyerName, productTitle, offerAmount, originalPrice)
notifySellerNewOrder(sellerId, buyerName, orderNumber, totalAmount, itemCount)
notifyOrderCancellationRequest(sellerId, buyerName, orderNumber, reason)
notifyProductInquiry(sellerId, buyerName, productTitle, inquiryType, question)
notifyStoreRating(sellerId, buyerName, rating, review)
notifyPaymentReceived(sellerId, buyerName, orderNumber, amount, paymentMethod)
```

### Seller → Buyer
```javascript
notifyNegotiationResponse(buyerId, sellerName, productTitle, responseType, counterOffer)
notifyProductBackInStock(buyerId, productTitle, productId, sellerName)
notifyPriceDrop(buyerId, productTitle, productId, oldPrice, newPrice, sellerName)
notifyShippingUpdate(buyerId, orderNumber, status, trackingNumber, estimatedDelivery)
notifyBuyerSellerResponse(buyerId, sellerName, productTitle, messagePreview)
notifyOrderConfirmation(buyerId, sellerName, orderNumber, totalAmount, estimatedDelivery)
```

### Admin → Users
```javascript
notifyProductApproved(sellerId, productTitle, productId)
notifyProductRejected(sellerId, productTitle, productId, reason)
notifyVerificationApproved(sellerId)
notifyVerificationRejected(sellerId, reason)
notifyAccountSuspended(userId, reason)
notifyAccountReactivated(userId)
notifyStoreFlagged(ownerId, storeName, reason)
notifyStoreFlagRemoved(ownerId, storeName)
broadcastPolicyUpdate(policyType, summary)
broadcastSystemMaintenance(maintenanceDate, duration, affectedServices)
```

### System → Admin
```javascript
notifyAdminNewSellerApplication(applicantName, applicantEmail, applicantId)
notifyAdminReportedContent(reportType, reportedItemId, reporterName, reason)
notifyAdminPaymentDispute(orderNumber, buyerName, sellerName, disputeAmount, reason)
notifyAdminSystemError(errorType, errorMessage, affectedUsers, severity)
notifyAdminSuspiciousActivity(userId, userName, activityType, details, riskLevel)
notifyAdminHighValueTransaction(orderNumber, buyerName, sellerName, amount, paymentMethod)
```

## 🎨 Available Badge Components

### Pre-built Badges
```kotlin
NotificationBadge()     // Auto-configured with pulsing
CartBadge()            // Green, static
WishlistBadge()        // Pink, static  
OrdersBadge()          // Orange, pulses when pending
MessagesBadge()        // Blue, pulses when unread
```

### Badge Priorities
```kotlin
BadgePriority.URGENT   // Red, fast pulse
BadgePriority.HIGH     // Deep Orange, normal pulse
BadgePriority.MEDIUM   // Orange, normal pulse
BadgePriority.LOW      // Blue, static
```

## 🔧 Configuration Options

### Badge Configuration
```kotlin
BadgeConfig(
    count = 5,                              // Badge count
    priority = BadgePriority.HIGH,          // Priority level
    animationState = BadgeAnimationState.PULSING,  // Animation type
    color = Color.Red,                      // Badge color
    shouldPulse = true,                     // Enable pulsing
    pulseStartTime = System.currentTimeMillis()    // Animation start
)
```

### Animation States
```kotlin
BadgeAnimationState.STATIC          // No animation
BadgeAnimationState.PULSING         // Normal pulse (1.15x, 1200ms)
BadgeAnimationState.URGENT_PULSING  // Fast pulse (1.3x, 800ms)
```

## 📱 Integration Examples

### Navigation Bar with Badges
```kotlin
BottomNavigationBar(
    items = navItems,
    selectedRoute = currentRoute,
    onItemClick = { route -> navigateTo(route) }
    // Badges are automatically handled
)
```

### Top Bar with Cart Badge
```kotlin
CraftoriaTopBar(
    title = "Products",
    showCart = true,
    onCartClick = { navigateToCart() }
    // Cart badge automatically shows with pulsing
)
```

### Custom Notification Icon
```kotlin
Box {
    IconButton(onClick = { openNotifications() }) {
        Icon(Icons.Default.Notifications, "Notifications")
    }
    
    NotificationBadge(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-4).dp)
    )
}
```

## 🎯 Priority Guidelines

### When to Use Each Priority

**URGENT** (Red, Fast Pulse)
- System errors affecting users
- Account suspensions
- Payment disputes
- Security alerts

**HIGH** (Deep Orange, Normal Pulse)  
- New orders for sellers
- Order cancellations
- Negotiation responses
- Product rejections

**MEDIUM** (Orange, Normal Pulse)
- Messages and replies
- Shipping updates  
- Product inquiries
- Price drops

**LOW** (Blue, Static)
- Policy updates
- System maintenance
- General announcements
- Low-priority ratings

## 🔍 Debugging Tips

### Check Badge Configuration
```kotlin
val config = BadgeManager.getNotificationBadgeConfig()
Log.d("Badge", "Count: ${config.count}, Priority: ${config.priority}")
```

### Verify Notification Delivery
```javascript
try {
    const result = await notifyUser(userId, title, description, options);
    console.log('Notification sent:', result);
} catch (error) {
    console.error('Notification failed:', error);
}
```

### Test Animation States
```kotlin
// Force pulsing for testing
CustomBadge(
    count = 1,
    color = Color.Red,
    shouldPulse = true,
    priority = BadgePriority.URGENT
)
```

## 🚨 Common Issues & Solutions

### Badge Not Showing
- Check if count > 0
- Verify badge positioning with `offset()`
- Ensure proper `Box` alignment

### Animation Not Working
- Confirm `shouldPulse = true`
- Check animation state configuration
- Verify compose version compatibility

### Notification Not Received
- Check Firebase configuration
- Verify user ID exists
- Check network connectivity
- Review error logs

## 📊 Performance Tips

### Optimize Badge Rendering
- Use `remember` for expensive calculations
- Avoid recreating configs unnecessarily
- Implement proper state management

### Efficient Notifications
- Batch multiple notifications when possible
- Use appropriate priority levels
- Implement proper error handling

---

**Need Help?** Check the full documentation in `COMPREHENSIVE_NOTIFICATION_SYSTEM_IMPLEMENTATION.md`