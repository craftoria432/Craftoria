# Notification Implementation Roadmap

## Current State
- ✅ Badge system: Fully functional and real-time
- ✅ Notification screen: Fully functional with filtering and management
- ✅ 4 notification types implemented (Chat, Orders, Negotiation, Product Shared)
- ❌ 16 additional notification types missing

---

## Phase 1: Critical Notifications (Week 1)

### 1. New Order Received (Seller)
**Priority**: CRITICAL
**Trigger**: When buyer places order
**Recipient**: Seller
**Badge**: Yes
**Implementation Location**: OrderRepository or OrderViewModel

```kotlin
// In OrderRepository when order is created
suspend fun createNotification(order: Order) {
    val notification = Notification(
        userId = order.sellerId,
        title = "New Order Received",
        description = "Order #${order.id.take(8)} from ${order.buyerName}",
        category = NotificationCategory.ORDERS.name,
        actionType = NotificationActionType.VIEW_ORDER.name,
        actionData = mapOf("order_id" to order.id),
        orderId = order.id,
        buyerName = order.buyerName
    )
    notificationRepository.createNotification(notification)
}
```

### 2. Order Delivery Confirmation (Buyer)
**Priority**: CRITICAL
**Trigger**: When seller marks order as delivered
**Recipient**: Buyer
**Badge**: Yes
**Implementation Location**: OrderRepository

```kotlin
// When order status changes to DELIVERED
suspend fun notifyOrderDelivered(order: Order) {
    val notification = Notification(
        userId = order.buyerId,
        title = "Order Delivered",
        description = "Your order #${order.id.take(8)} has been delivered",
        category = NotificationCategory.ORDERS.name,
        actionType = NotificationActionType.TRACK_ORDER.name,
        actionData = mapOf("order_id" to order.id),
        orderId = order.id
    )
    notificationRepository.createNotification(notification)
}
```

### 3. Payment Received (Seller)
**Priority**: CRITICAL
**Trigger**: When payment is processed
**Recipient**: Seller
**Badge**: Yes
**Implementation Location**: PaymentRepository

```kotlin
// When payment is confirmed
suspend fun notifyPaymentReceived(payment: SellerPayment) {
    val notification = Notification(
        userId = payment.sellerId,
        title = "Payment Received",
        description = "Payment of PKR ${payment.amount} received for order #${payment.orderId.take(8)}",
        category = NotificationCategory.PAYMENTS.name,
        actionType = NotificationActionType.VIEW_PAYMENT.name,
        actionData = mapOf("payment_id" to payment.id),
        orderId = payment.orderId
    )
    notificationRepository.createNotification(notification)
}
```

### 4. Order Cancellation (Both)
**Priority**: CRITICAL
**Trigger**: When order is cancelled
**Recipient**: Both buyer and seller
**Badge**: Yes
**Implementation Location**: OrderRepository

```kotlin
// When order is cancelled
suspend fun notifyOrderCancelled(order: Order, cancelledBy: String) {
    // Notify seller
    val sellerNotification = Notification(
        userId = order.sellerId,
        title = "Order Cancelled",
        description = "Order #${order.id.take(8)} has been cancelled",
        category = NotificationCategory.ORDERS.name,
        actionType = NotificationActionType.VIEW_ORDER.name,
        orderId = order.id
    )
    
    // Notify buyer
    val buyerNotification = Notification(
        userId = order.buyerId,
        title = "Order Cancelled",
        description = "Your order #${order.id.take(8)} has been cancelled",
        category = NotificationCategory.ORDERS.name,
        actionType = NotificationActionType.VIEW_ORDER.name,
        orderId = order.id
    )
    
    notificationRepository.createNotification(sellerNotification)
    notificationRepository.createNotification(buyerNotification)
}
```

---

## Phase 2: Important Notifications (Week 2)

### 5. Store Rating Reminder (Buyer)
**Priority**: HIGH
**Trigger**: 3 days after order delivery
**Recipient**: Buyer
**Badge**: Yes
**Implementation**: Scheduled task or Cloud Function

```kotlin
// Triggered by Cloud Function or scheduled job
suspend fun notifyStoreRatingReminder(order: Order) {
    val notification = Notification(
        userId = order.buyerId,
        title = "Rate Your Experience",
        description = "How was your experience with ${order.storeName}?",
        category = NotificationCategory.STORE_RATING.name,
        actionType = NotificationActionType.RATE_ORDER.name,
        actionData = mapOf("order_id" to order.id),
        orderId = order.id,
        storeName = order.storeName
    )
    notificationRepository.createNotification(notification)
}
```

### 6. Product Approval Status (Seller)
**Priority**: HIGH
**Trigger**: When admin approves/rejects product
**Recipient**: Seller
**Badge**: Yes
**Implementation Location**: ProductRepository

```kotlin
// When product is approved/rejected
suspend fun notifyProductApprovalStatus(product: Product, approved: Boolean) {
    val status = if (approved) "approved" else "rejected"
    val notification = Notification(
        userId = product.sellerId,
        title = "Product ${status.capitalize()}",
        description = "Your product \"${product.name}\" has been $status",
        category = NotificationCategory.SYSTEM.name,
        actionType = NotificationActionType.VIEW_PRODUCT.name,
        actionData = mapOf("product_id" to product.id),
        productId = product.id,
        productName = product.name
    )
    notificationRepository.createNotification(notification)
}
```

### 7. Seller Verification Status (Seller)
**Priority**: HIGH
**Trigger**: When admin approves/rejects verification
**Recipient**: Seller
**Badge**: Yes
**Implementation Location**: AuthRepository

```kotlin
// When seller verification is processed
suspend fun notifyVerificationStatus(user: User, approved: Boolean) {
    val status = if (approved) "approved" else "rejected"
    val notification = Notification(
        userId = user.id,
        title = "Verification ${status.capitalize()}",
        description = "Your seller verification has been $status",
        category = NotificationCategory.SYSTEM.name,
        actionType = NotificationActionType.VIEW_PROFILE.name,
        actionData = mapOf("user_id" to user.id)
    )
    notificationRepository.createNotification(notification)
}
```

### 8. Co-Seller Invitation (Seller)
**Priority**: HIGH
**Trigger**: When invited to co-seller store
**Recipient**: Seller
**Badge**: Yes
**Implementation Location**: CoSellerStoreRepository

```kotlin
// When seller is invited to co-seller store
suspend fun notifyCoSellerInvitation(invitation: CoSellerInvitation) {
    val notification = Notification(
        userId = invitation.inviteeId,
        title = "Store Invitation",
        description = "${invitation.inviterName} invited you to join ${invitation.storeName}",
        category = NotificationCategory.SYSTEM.name,
        actionType = NotificationActionType.ACCEPT_INVITATION.name,
        actionData = mapOf("invitation_id" to invitation.id),
        storeId = invitation.storeId,
        storeName = invitation.storeName,
        inviterName = invitation.inviterName,
        memberCount = invitation.memberCount
    )
    notificationRepository.createNotification(notification)
}
```

---

## Phase 3: Enhancement Notifications (Week 3)

### 9. Refund Processed (Buyer)
```kotlin
suspend fun notifyRefundProcessed(refund: Refund) {
    val notification = Notification(
        userId = refund.buyerId,
        title = "Refund Processed",
        description = "Refund of PKR ${refund.amount} has been processed",
        category = NotificationCategory.PAYMENTS.name,
        actionType = NotificationActionType.VIEW_PAYMENT.name,
        orderId = refund.orderId
    )
    notificationRepository.createNotification(notification)
}
```

### 10. Payout Processed (Seller)
```kotlin
suspend fun notifyPayoutProcessed(payout: SellerPayout) {
    val notification = Notification(
        userId = payout.sellerId,
        title = "Payout Processed",
        description = "Payout of PKR ${payout.amount} has been transferred",
        category = NotificationCategory.PAYMENTS.name,
        actionType = NotificationActionType.VIEW_PAYMENT.name
    )
    notificationRepository.createNotification(notification)
}
```

### 11. Product Reported (Seller)
```kotlin
suspend fun notifyProductReported(report: Report) {
    val notification = Notification(
        userId = report.productSellerId,
        title = "Product Reported",
        description = "Your product \"${report.productName}\" has been reported",
        category = NotificationCategory.REPORT.name,
        actionType = NotificationActionType.VIEW_REPORT.name,
        actionData = mapOf("report_id" to report.id),
        productId = report.productId,
        productName = report.productName
    )
    notificationRepository.createNotification(notification)
}
```

### 12. Store Rating Received (Seller)
```kotlin
suspend fun notifyStoreRatingReceived(rating: StoreRating) {
    val notification = Notification(
        userId = rating.storeId,
        title = "Store Rated",
        description = "${rating.buyerName} rated your store ${rating.rating} stars",
        category = NotificationCategory.STORE_RATING.name,
        actionType = NotificationActionType.VIEW_RATING.name,
        actionData = mapOf("rating_id" to rating.id),
        storeId = rating.storeId,
        buyerName = rating.buyerName,
        ratingValue = rating.rating
    )
    notificationRepository.createNotification(notification)
}
```

### 13. Promotional Offers (Buyer)
```kotlin
suspend fun notifyPromotionalOffer(promotion: Promotion, buyerId: String) {
    val notification = Notification(
        userId = buyerId,
        title = "Special Offer",
        description = "${promotion.title} - ${promotion.discount}% off",
        category = NotificationCategory.PROMOTIONS.name,
        actionType = NotificationActionType.VIEW_PROMOTIONS.name,
        actionData = mapOf("promotion_id" to promotion.id)
    )
    notificationRepository.createNotification(notification)
}
```

### 14. Wishlist Item Back in Stock (Buyer)
```kotlin
suspend fun notifyWishlistItemAvailable(product: Product, buyerId: String) {
    val notification = Notification(
        userId = buyerId,
        title = "Back in Stock",
        description = "\"${product.name}\" is back in stock!",
        category = NotificationCategory.PROMOTIONS.name,
        actionType = NotificationActionType.VIEW_PRODUCT.name,
        actionData = mapOf("product_id" to product.id),
        productId = product.id,
        productName = product.name
    )
    notificationRepository.createNotification(notification)
}
```

### 15. Price Drop Alert (Buyer)
```kotlin
suspend fun notifyPriceDropped(product: Product, oldPrice: Double, buyerId: String) {
    val discount = ((oldPrice - product.price) / oldPrice * 100).toInt()
    val notification = Notification(
        userId = buyerId,
        title = "Price Dropped",
        description = "\"${product.name}\" is now PKR ${product.price} (was PKR $oldPrice)",
        category = NotificationCategory.PROMOTIONS.name,
        actionType = NotificationActionType.VIEW_PRODUCT.name,
        actionData = mapOf("product_id" to product.id),
        productId = product.id,
        productName = product.name
    )
    notificationRepository.createNotification(notification)
}
```

### 16. Admin Message (Seller)
```kotlin
suspend fun notifyAdminMessage(message: AdminMessage) {
    val notification = Notification(
        userId = message.recipientId,
        title = "Admin Message",
        description = message.content,
        category = NotificationCategory.ADMIN_MESSAGE.name,
        actionType = NotificationActionType.VIEW_PROFILE.name
    )
    notificationRepository.createNotification(notification)
}
```

---

## Implementation Strategy

### Step 1: Create Notification Helper
```kotlin
// NotificationHelper.kt
object NotificationHelper {
    suspend fun createNotification(
        userId: String,
        title: String,
        description: String,
        category: NotificationCategory,
        actionType: NotificationActionType = NotificationActionType.NONE,
        actionData: Map<String, String> = emptyMap(),
        orderId: String = "",
        storeId: String = "",
        productId: String = ""
    ) {
        val notification = Notification(
            userId = userId,
            title = title,
            description = description,
            category = category.name,
            actionType = actionType.name,
            actionData = actionData,
            orderId = orderId,
            storeId = storeId,
            productId = productId
        )
        NotificationRepository().createNotification(notification)
    }
}
```

### Step 2: Integrate into Repositories
Add notification creation calls to:
- OrderRepository (new order, delivery, cancellation)
- PaymentRepository (payment received, payout)
- ProductRepository (approval status)
- AuthRepository (verification status)
- CoSellerStoreRepository (invitations)
- StoreRatingRepository (ratings)

### Step 3: Test Each Notification
- Create test data
- Verify notification appears in Firestore
- Verify badge updates
- Verify notification screen displays correctly
- Verify action buttons work

---

## Testing Checklist

### Phase 1 Testing
- [ ] New order creates seller notification
- [ ] Order delivery creates buyer notification
- [ ] Payment creates seller notification
- [ ] Order cancellation creates both notifications
- [ ] All badges update correctly

### Phase 2 Testing
- [ ] Store rating reminder appears after 3 days
- [ ] Product approval/rejection notifications work
- [ ] Seller verification notifications work
- [ ] Co-seller invitations work with action buttons

### Phase 3 Testing
- [ ] Refund notifications appear
- [ ] Payout notifications appear
- [ ] Product report notifications appear
- [ ] Store rating notifications appear
- [ ] Promotional offers appear
- [ ] Wishlist alerts appear
- [ ] Price drop alerts appear
- [ ] Admin messages appear

---

## Estimated Timeline

| Phase | Notifications | Effort | Timeline |
|-------|---|---|---|
| Phase 1 | 4 critical | 8 hours | Week 1 |
| Phase 2 | 4 important | 8 hours | Week 2 |
| Phase 3 | 8 enhancement | 12 hours | Week 3 |
| **Total** | **16 notifications** | **28 hours** | **3 weeks** |

---

## Notes

- All notification infrastructure is ready
- Only notification generation logic needs to be added
- Badge system will automatically work for all new notifications
- Notification screen will automatically display all new types
- No UI changes needed
- Focus on data layer implementation
