# Notification Integration - Quick Start Guide

## Overview

The notification system is now fully integrated into the Craftoria app. All 16 notification types are implemented and ready to use.

---

## Quick Integration Examples

### 1. Send Product Approval Notification

**When**: Admin approves or rejects a product

**Code**:
```kotlin
val productRepository = ProductRepository()

// Approve product
productRepository.updateProductApprovalStatus(
    productId = "product_id_here",
    approved = true
)

// Reject product with reason
productRepository.updateProductApprovalStatus(
    productId = "product_id_here",
    approved = false,
    reason = "Images do not meet quality standards"
)
```

**Result**: Seller receives notification in real-time

---

### 2. Send Seller Verification Notification

**When**: Admin approves or rejects seller verification

**Code**:
```kotlin
val authRepository = AuthRepository()

// Approve verification
authRepository.updateSellerVerificationStatus(
    sellerId = "seller_id_here",
    approved = true
)

// Reject verification with reason
authRepository.updateSellerVerificationStatus(
    sellerId = "seller_id_here",
    approved = false,
    reason = "Documents do not match provided information"
)
```

**Result**: Seller receives notification in real-time

---

### 3. Send Co-Seller Invitation Notification

**When**: Store owner invites sellers to co-seller store

**Code**:
```kotlin
val coSellerRepository = CoSellerStoreRepository()

coSellerRepository.createStore(
    context = context,
    store = coSellerStore,
    logoUri = logoUri,
    bannerUri = bannerUri,
    invitedEmails = listOf(
        "seller1@email.com",
        "seller2@email.com"
    )
)
```

**Result**: Each invited seller receives invitation notification in real-time

---

### 4. Send Order Delivery Notification

**When**: Seller marks order as delivered

**Code**:
```kotlin
val orderRepository = OrderRepository()

orderRepository.markAsDelivered(orderId = "order_id_here")
```

**Result**: Buyer receives "Order Delivered" notification in real-time

---

### 5. Send Order Cancellation Notification

**When**: Buyer cancels order

**Code**:
```kotlin
val orderRepository = OrderRepository()

orderRepository.cancelOrder(orderId = "order_id_here")
```

**Result**: Seller receives "Cancellation Request" notification in real-time

---

### 6. Send Payment Notification

**When**: Payment is processed for order

**Code**:
```kotlin
val paymentRepository = PaymentRepository()

paymentRepository.sendPaymentNotification(
    sellerId = "seller_id_here",
    orderId = "order_id_here",
    amount = 5000.0
)
```

**Result**: Seller receives "Payment Received" notification in real-time

---

### 7. Send Store Rating Notification

**When**: Buyer rates store

**Code**:
```kotlin
val storeRatingRepository = StoreRatingRepository()

storeRatingRepository.sendRatingNotification(
    sellerId = "seller_id_here",
    storeId = "store_id_here",
    storeName = "My Store",
    buyerName = "John Doe",
    rating = 5,
    review = "Great quality!"
)
```

**Result**: Seller receives "Store Rated" notification in real-time

---

## Using NotificationHelper Directly

For custom notifications, you can use NotificationHelper directly:

```kotlin
// Buyer Notifications
NotificationHelper.notifyOrderDelivered(
    buyerId = "buyer123",
    orderId = "order456",
    storeName = "My Store",
    orderNumber = "order456".take(8)
)

NotificationHelper.notifyRefundProcessed(
    buyerId = "buyer123",
    orderId = "order456",
    amount = 5000.0,
    orderNumber = "order456".take(8)
)

NotificationHelper.notifyPromotionalOffer(
    buyerId = "buyer123",
    title = "Special Offer",
    description = "Get 50% off on all items",
    discount = 50,
    productId = "product123"
)

// Seller Notifications
NotificationHelper.notifyNewOrderReceived(
    sellerId = "seller123",
    orderId = "order456",
    orderNumber = "order456".take(8),
    buyerName = "John Doe",
    totalAmount = 5000.0
)

NotificationHelper.notifyPaymentReceived(
    sellerId = "seller123",
    orderId = "order456",
    orderNumber = "order456".take(8),
    amount = 5000.0
)

NotificationHelper.notifyStoreRatingReceived(
    sellerId = "seller123",
    storeId = "store456",
    storeName = "My Store",
    buyerName = "John Doe",
    rating = 5,
    review = "Great quality!"
)
```

---

## Notification Categories

All notifications are categorized for filtering:

- `ORDERS` - Order-related notifications
- `MESSAGES` - Chat and negotiation messages
- `PAYMENTS` - Payment and payout notifications
- `PROMOTIONS` - Promotional offers and alerts
- `STORE_RATING` - Store rating notifications
- `REPORT` - Product report notifications
- `SYSTEM` - System notifications (verification, approval, invitations)
- `ADMIN_MESSAGE` - Admin messages

---

## Action Types

Notifications include action types for navigation:

- `VIEW_ORDER` - Opens order details
- `TRACK_ORDER` - Opens order tracking
- `VIEW_PAYMENT` - Opens payment details
- `RATE_ORDER` - Opens rating dialog
- `VIEW_PRODUCT` - Opens product details
- `VIEW_PROMOTIONS` - Opens promotions
- `VIEW_RATING` - Opens store rating
- `VIEW_REPORT` - Opens report details
- `ACCEPT_INVITATION` - Accepts co-seller invitation
- `VIEW_PROFILE` - Opens user profile

---

## Real-Time Updates

All notifications update in real-time:

1. Notification created in Firestore
2. Real-time listener detects change
3. Badge count updates immediately
4. Notification appears in NotificationsScreen
5. User sees notification without refreshing

---

## Error Handling

All methods return `Result<Unit>` for proper error handling:

```kotlin
val result = productRepository.updateProductApprovalStatus(
    productId = "product123",
    approved = true
)

if (result.isSuccess) {
    Log.d("TAG", "Notification sent successfully")
} else {
    Log.e("TAG", "Failed to send notification", result.exceptionOrNull())
}
```

---

## Logging

All operations are logged for debugging:

```
✅ Product approval status updated: product123 -> approved
✅ Product approval notification sent to seller: seller123
```

---

## Best Practices

1. **Always use repository methods** - Don't create notifications directly
2. **Include relevant IDs** - Always pass product_id, order_id, etc.
3. **Use descriptive titles** - Make notifications clear and actionable
4. **Include action data** - Help users navigate to relevant screens
5. **Handle errors gracefully** - Check Result for success/failure
6. **Log operations** - Use Log.d() for debugging

---

## Testing

To test notifications:

1. Perform action that triggers notification (e.g., approve product)
2. Check NotificationsScreen - notification should appear
3. Check badge count - should increase
4. Check Firestore - notification should be in `notifications` collection
5. Check logs - should see ✅ success messages

---

## Troubleshooting

### Notification not appearing?
- Check Firestore security rules allow read access
- Check user_id is correct
- Check logs for error messages
- Verify NotificationsScreen is listening to real-time updates

### Badge not updating?
- Check BadgeManager is properly initialized
- Check real-time listener is active
- Check Firestore has notification document
- Verify user_id matches current user

### Notification appearing but action not working?
- Check action_type is correct
- Check action_data has required IDs
- Check navigation routes are set up
- Verify screen exists for action type

---

## Summary

The notification system is production-ready and fully integrated. Use the repository methods to send notifications, and they will automatically appear in real-time with badge updates.

