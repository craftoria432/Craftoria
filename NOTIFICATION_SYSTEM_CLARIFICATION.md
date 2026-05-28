# Notification System Clarification

## Issue Resolution: Stale Store Name & Member Count

### Problems Identified
1. **Stale Store Name**: Notifications showed "Zara Ahmed" even after the seller updated their name to "Zara Ali"
2. **Incorrect Member Count**: Notifications showed "0 Members" even though the co-seller store had 2 members

### Root Causes
1. **Notification Creation**: When an order is placed, the notification stores the seller's name and member count at that moment
2. **No Real-time Updates**: The stored values weren't being updated when the seller changed their name or members were added
3. **Incomplete Real-time Listeners**: The NotificationCard had listeners but they weren't properly handling all cases

### Solution Implemented

#### 1. Enhanced Real-time Listeners in NotificationCard
- **Store Name**: Real-time listener on `users/{storeId}` fetches the current name
- **Member Count**: Real-time listener on `co_seller_stores/{storeId}` fetches the current member count
- **Fallback Logic**: If listeners fail, displays the stored values
- **Logging**: Added detailed logging to track updates

#### 2. Improved Member Count Fetching in NotificationRepository
- Now fetches accurate member count for ALL co-seller store notifications (not just when memberCount == 0)
- Uses `CoSellerMemberCountManager.getAccurateMemberCount()` to get the real count
- Retroactively updates notifications in Firestore for future consistency

#### 3. Notification Creation Best Practices
- When creating notifications for co-seller orders, always include:
  - `storeId`: The co-seller store ID (required for real-time updates)
  - `storeName`: The current seller name (will be updated in real-time)
  - `memberCount`: The current member count (will be updated in real-time)

---

## Notification Screen: Categories & Filtering

### Filter Categories

The notification screen has 5 filter tabs:

#### 1. **All** (NotificationCategory.ALL)
- **Content**: All notifications for the user
- **Use Case**: View complete notification history
- **Includes**: Orders, Messages, Promotions, System, Payments, Store Ratings, Refunds

#### 2. **Orders** (NotificationCategory.ORDERS)
- **Content**: Order-related notifications
- **Examples**:
  - Order Delivered
  - Order Processing
  - Order Shipped
  - Order Cancelled
  - Cancellation Request (for sellers)
- **Action Types**: TRACK_ORDER, VIEW_ORDER
- **Icon**: Shopping Bag (pink)

#### 3. **Messages** (NotificationCategory.MESSAGES)
- **Content**: Chat and messaging notifications
- **Examples**:
  - New message from seller/buyer
  - Message replies
  - Chat notifications
- **Action Types**: REPLY_MESSAGE
- **Icon**: Message (blue)

#### 4. **Promotions** (NotificationCategory.PROMOTIONS)
- **Content**: Marketing and promotional content
- **Examples**:
  - Promotional offers
  - Discount alerts
  - Wishlist item back in stock
  - Price drop alerts
  - Special deals
- **Action Types**: VIEW_PROMOTIONS, VIEW_PRODUCT
- **Icon**: Campaign (orange)

#### 5. **System** (NotificationCategory.SYSTEM)
- **Content**: System and administrative notifications
- **Examples**:
  - Co-seller invitations
  - Invitation accepted
  - Product approval/rejection
  - Seller verification status
  - Admin messages
  - System alerts
- **Action Types**: ACCEPT_INVITATION, DECLINE_INVITATION, VIEW_STORE, VIEW_PROFILE
- **Icon**: Check Circle (green)

### Additional Categories (Not in UI Tabs)

These categories exist in the system but are not shown as separate tabs:

#### **Payments** (NotificationCategory.PAYMENTS)
- Payment received
- Payout processed
- Refund processed
- Payment-related alerts

#### **Store Rating** (NotificationCategory.STORE_RATING)
- Store rating reminders
- New store ratings received
- Rating-related notifications

#### **Refunds** (NotificationCategory.REFUNDS)
- Refund initiated
- Refund processed
- Refund status updates

#### **Report** (NotificationCategory.REPORT)
- Product reported
- Report status updates

#### **Admin Message** (NotificationCategory.ADMIN_MESSAGE)
- Direct messages from admin
- System announcements

---

## Notification Data Structure

```kotlin
data class Notification(
    val id: String,
    val userId: String,              // Recipient user ID
    val title: String,               // Notification title
    val description: String,         // Notification description
    val category: String,            // NotificationCategory (stored as uppercase string)
    val isRead: Boolean,             // Read status
    val createdAt: Long,             // Timestamp
    val actionType: String,          // NotificationActionType (stored as uppercase string)
    val actionData: Map<String, String>, // Additional data for actions
    
    // Order-related
    val orderId: String,
    
    // Store-related (for co-seller notifications)
    val storeId: String,             // ✅ REQUIRED for real-time updates
    val storeName: String,           // ✅ Will be updated in real-time
    val memberCount: Int,            // ✅ Will be updated in real-time
    
    // Seller/Buyer info
    val inviterName: String,
    val senderName: String,
    val buyerName: String,
    
    // Product-related
    val productId: String,
    val productName: String,
    
    // Rating-related
    val ratingValue: Int,
    val ratingReview: String,
    
    // Refund-related
    val refundId: String,
    val refundAmount: Double,
    val refundStatus: String,
    val refundReason: String
)
```

---

## Real-time Update Flow

### For Store Name Updates
```
1. Seller updates their name in Profile
2. User document in Firestore is updated
3. Real-time listener in NotificationCard detects change
4. UI updates to show new name immediately
5. No page refresh needed
```

### For Member Count Updates
```
1. New member joins co-seller store
2. co_seller_stores document is updated with new member_ids
3. Real-time listener in NotificationCard detects change
4. UI updates to show new member count immediately
5. No page refresh needed
```

---

## Best Practices for Creating Notifications

### For Co-Seller Orders
```kotlin
// ✅ CORRECT: Include storeId for real-time updates
NotificationHelper.notifyOrderDelivered(
    buyerId = buyerId,
    orderId = orderId,
    storeName = currentStoreName,  // Current name at time of creation
    orderNumber = orderNumber,
    storeId = storeId              // ✅ REQUIRED
)

// ❌ WRONG: Missing storeId
NotificationHelper.notifyOrderDelivered(
    buyerId = buyerId,
    orderId = orderId,
    storeName = storeName,
    orderNumber = orderNumber
    // Missing storeId - real-time updates won't work
)
```

### For Store Ratings
```kotlin
// ✅ CORRECT: Include accurate member count
NotificationHelper.notifyStoreRatingReceived(
    sellerId = sellerId,
    storeId = storeId,
    storeName = storeName,
    buyerName = buyerName,
    rating = rating,
    review = review,
    memberCount = accurateMemberCount  // ✅ Accurate count
)
```

---

## Notification Filtering Logic

### In NotificationRepository.getUserNotifications()
```kotlin
// Fetch notifications for user
var query = notificationsCollection.whereEqualTo("user_id", userId)

// If specific category requested (not ALL)
if (category != NotificationCategory.ALL) {
    query = query.whereEqualTo("category", category.name)
}

// Fetch and parse
val notifications = query.limit(100).get().await()
    .documents.mapNotNull { doc ->
        // Parse notification
        // Fetch accurate member count if needed
        // Return notification
    }
    .sortedByDescending { it.createdAt }  // Newest first
    .take(50)  // Return max 50
```

---

## UI Display Rules

### Notification Card Display
- **Unread Notifications**: Pink background (#FFF5F8), Primary border
- **Read Notifications**: White background, BorderColor border
- **Unread Indicator**: Small pink dot in top-right corner
- **Store Pill**: Shows store name + member count with icons
- **Timestamp**: "2 minutes ago" format

### Category Icons & Colors
| Category | Icon | Color | Background |
|----------|------|-------|------------|
| Orders | Shopping Bag | Pink (#E91E8C) | #FFF5F8 |
| Messages | Message | Blue (#1976D2) | #E3F2FD |
| Promotions | Campaign | Orange (#F57F17) | #FFF9C4 |
| System | Check Circle | Green (#2E7D32) | #E8F5E8 |
| Payments | Shopping Bag | Green (#2E7D32) | #E8F5E9 |
| Store Rating | Store | Pink (#E91E8C) | #FFF5F8 |

---

## Action Types & Navigation

| Action Type | Behavior | Navigation |
|------------|----------|-----------|
| TRACK_ORDER | Track order status | Order tracking screen |
| VIEW_ORDER | View order details | Order details screen |
| ACCEPT_INVITATION | Accept co-seller invite | Store join flow |
| DECLINE_INVITATION | Decline invite | Delete notification |
| VIEW_STORE | View store details | Store public view |
| REPLY_MESSAGE | Reply to message | Chat screen |
| VIEW_PRODUCT | View product | Product details |
| RATE_ORDER | Rate store | Rating dialog |
| VIEW_PROMOTIONS | View promotions | Promotions screen |
| VIEW_REPORT | View report details | Report details |
| VIEW_PROFILE | View user profile | Profile screen |
| VIEW_PAYMENT | View payment details | Payment history |
| VIEW_RATING | View store rating | Store rating screen |

---

## Summary of Fixes

### ✅ Fixed Issues
1. **Store name now updates in real-time** when seller changes their name
2. **Member count now displays correctly** with real-time updates
3. **Notifications properly categorized** with correct filtering
4. **Real-time listeners** properly set up for all co-seller notifications
5. **Retroactive member count updates** for existing notifications

### ✅ Implementation Details
- Real-time listeners use `DisposableEffect` for proper lifecycle management
- Listeners are removed when notification card is disposed
- Fallback to stored values if listeners fail
- Detailed logging for debugging
- Retroactive Firestore updates for consistency

