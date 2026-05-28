# Notification Filter Tabs — Professional Analysis & Implementation Plan

## Current State Analysis

### NotificationCategory Enum (9 values)
```kotlin
ALL, UNREAD, ORDERS, MESSAGES, PROMOTIONS, SYSTEM, REPORT, ADMIN_MESSAGE, PAYMENTS, STORE_RATING, REFUNDS
```

### Current Filter Tabs in NotificationsScreen.kt
```kotlin
val filters = listOf(
    NotificationCategory.UNREAD to "Unread",
    NotificationCategory.ALL to "All",
    NotificationCategory.ORDERS to "Orders",
    NotificationCategory.MESSAGES to "Messages",
    NotificationCategory.PROMOTIONS to "Promotions",
    NotificationCategory.PAYMENTS to "Payments",
    NotificationCategory.SYSTEM to "System"
)
```

### Missing Tabs (3 categories with no filter)
- ❌ **REFUNDS** — Used by RefundNotificationService for all refund workflow notifications
- ❌ **STORE_RATING** — Used by NotificationHelper.notifyStoreRatingReceived() and notifyStoreRatingReminder()
- ❌ **REPORT** — Used by NotificationHelper.notifyProductReported()

### Issues Identified

#### Issue 1: REFUNDS Category Not Visible
**Problem**: RefundNotificationService sends notifications with `category = NotificationCategory.REFUNDS.name`, but there's no REFUNDS tab. These notifications only appear under "All" or "Unread".

**Current Usage in RefundNotificationService**:
- `notifyRefundRequested()` → REFUNDS
- `notifyRefundApproved()` → REFUNDS
- `notifyRefundRejected()` → REFUNDS
- `notifyRefundProcessing()` → REFUNDS
- `notifyRefundCompleted()` → REFUNDS
- `notifyRefundFailed()` → REFUNDS
- `notifyAutoApprovedRefund()` → REFUNDS

**Professional Recommendation**: 
- Keep refund workflow notifications (requested, approved, rejected, processing, failed) under REFUNDS category
- This is correct — refunds are a distinct workflow from payments
- Add REFUNDS tab to both buyer and seller sides

#### Issue 2: STORE_RATING Category Not Visible
**Problem**: NotificationHelper sends store rating notifications with `category = NotificationCategory.STORE_RATING.name`, but there's no STORE_RATING tab.

**Current Usage in NotificationHelper**:
- `notifyStoreRatingReminder()` → STORE_RATING (buyer-only)
- `notifyStoreRatingReceived()` → STORE_RATING (seller-only)

**Professional Recommendation**:
- Add STORE_RATING tab to seller side only (sellers receive ratings)
- Buyers see rating reminders under PROMOTIONS (they're engagement/feedback requests, not critical)
- OR add to buyer side as well for completeness

#### Issue 3: REPORT Category Not Visible
**Problem**: NotificationHelper sends report notifications with `category = NotificationCategory.REPORT.name`, but there's no REPORT tab.

**Current Usage in NotificationHelper**:
- `notifyProductReported()` → REPORT (seller-only)

**Professional Recommendation**:
- Add REPORT tab to seller side only (sellers receive product reports)
- Buyers never receive report notifications

#### Issue 4: MESSAGES Tab Always Empty
**Problem**: There's no `notifyNewMessage()` function in NotificationHelper. Messages are handled by ChatRepository/ChatViewModel, not NotificationHelper.

**Current State**:
- Chat messages don't create notifications in the notification system
- MESSAGES tab exists but will always be empty
- Users see unread message count in badge, but not in notifications

**Professional Recommendation**:
- Either: Remove MESSAGES tab (chat has its own badge system)
- Or: Implement `notifyNewMessage()` in NotificationHelper to create notifications for new messages
- Recommend: Keep MESSAGES tab but populate it via ChatRepository when new messages arrive

#### Issue 5: PROMOTIONS Tab on Seller Side
**Problem**: Sellers never receive PROMOTIONS notifications. The tab shows empty state.

**Current Usage in NotificationHelper**:
- `notifyPromotionalOffer()` → PROMOTIONS (buyer-only)
- `notifyWishlistItemAvailable()` → PROMOTIONS (buyer-only)
- `notifyPriceDropped()` → PROMOTIONS (buyer-only)

**Professional Recommendation**:
- Hide PROMOTIONS tab on seller side (pass userRole to NotificationFilterTabs)
- Only show on buyer side

---

## Professional Recommendations

### Recommended Tab Lists

#### BUYER SIDE (8 tabs)
```
Unread · All · Orders · Payments · Refunds · Messages · Promotions · System
```

**Rationale**:
- **Unread**: Quick access to unread notifications
- **All**: See everything
- **Orders**: Order status updates (processing, shipped, delivered, cancelled)
- **Payments**: Payment confirmations, payment-related events
- **Refunds**: Refund workflow (requested, approved, rejected, processing, completed)
- **Messages**: Chat messages (if implemented) or direct messages
- **Promotions**: Offers, price drops, wishlist alerts
- **System**: General system messages, admin messages

**NOT included**:
- STORE_RATING: Buyers see rating reminders under PROMOTIONS
- REPORT: Buyers never receive reports

#### SELLER SIDE (9 tabs)
```
Unread · All · Orders · Payments · Refunds · Messages · System · Store Rating · Reports
```

**Rationale**:
- **Unread**: Quick access to unread notifications
- **All**: See everything
- **Orders**: New orders, cancellation requests, order updates
- **Payments**: Payment received, payout processed
- **Refunds**: Refund workflow (requested, approved, rejected, completed)
- **Messages**: Chat messages (if implemented) or direct messages
- **System**: System messages, admin messages, product approval status, seller verification status, co-seller invitations
- **Store Rating**: Store ratings received from buyers
- **Reports**: Product reports from buyers

**NOT included**:
- PROMOTIONS: Sellers never receive promotions

---

## Implementation Changes Required

### 1. Update NotificationFilterTabs Composable
**File**: `NotificationsScreen.kt`

**Changes**:
- Add `userRole` parameter to determine which tabs to show
- Add REFUNDS, STORE_RATING, REPORT tabs
- Conditionally hide PROMOTIONS for sellers
- Conditionally hide STORE_RATING and REPORT for buyers

```kotlin
@Composable
fun NotificationFilterTabs(
    currentFilter: NotificationCategory,
    onFilterSelected: (NotificationCategory) -> Unit,
    userRole: String = "buyer"  // "buyer" or "seller"
) {
    val buyerFilters = listOf(
        NotificationCategory.UNREAD to "Unread",
        NotificationCategory.ALL to "All",
        NotificationCategory.ORDERS to "Orders",
        NotificationCategory.PAYMENTS to "Payments",
        NotificationCategory.REFUNDS to "Refunds",
        NotificationCategory.MESSAGES to "Messages",
        NotificationCategory.PROMOTIONS to "Promotions",
        NotificationCategory.SYSTEM to "System"
    )
    
    val sellerFilters = listOf(
        NotificationCategory.UNREAD to "Unread",
        NotificationCategory.ALL to "All",
        NotificationCategory.ORDERS to "Orders",
        NotificationCategory.PAYMENTS to "Payments",
        NotificationCategory.REFUNDS to "Refunds",
        NotificationCategory.MESSAGES to "Messages",
        NotificationCategory.SYSTEM to "System",
        NotificationCategory.STORE_RATING to "Store Rating",
        NotificationCategory.REPORT to "Reports"
    )
    
    val filters = if (userRole == "seller") sellerFilters else buyerFilters
    
    // ... rest of implementation
}
```

### 2. Update NotificationsScreen Call
**File**: `NotificationsScreen.kt`

**Changes**:
- Pass `user.role` to NotificationFilterTabs

```kotlin
NotificationFilterTabs(
    currentFilter = currentFilter,
    onFilterSelected = { filter ->
        notificationViewModel.filterNotifications(filter, user.id)
    },
    userRole = user.role  // "buyer" or "seller"
)
```

### 3. Add Category Icons & Colors
**File**: `NotificationsScreen.kt`

**Changes**:
- Add icon and color for REFUNDS category
- Add icon and color for STORE_RATING category
- Add icon and color for REPORT category

```kotlin
fun getCategoryIcon(category: NotificationCategory): ImageVector {
    return when (category) {
        NotificationCategory.REFUNDS -> Icons.Outlined.MoneyOff  // or Refund icon
        NotificationCategory.STORE_RATING -> Icons.Outlined.Star
        NotificationCategory.REPORT -> Icons.Outlined.Flag
        // ... existing cases
    }
}

fun getCategoryIconTint(category: NotificationCategory): Color {
    return when (category) {
        NotificationCategory.REFUNDS -> Color(0xFF2E7D32)  // Green
        NotificationCategory.STORE_RATING -> Color(0xFFFFA500)  // Orange
        NotificationCategory.REPORT -> Color(0xFFD32F2F)  // Red
        // ... existing cases
    }
}

fun getIconBackground(category: NotificationCategory): Color {
    return when (category) {
        NotificationCategory.REFUNDS -> Color(0xFFE8F5E9)  // Light green
        NotificationCategory.STORE_RATING -> Color(0xFFFFF3E0)  // Light orange
        NotificationCategory.REPORT -> Color(0xFFFFEBEE)  // Light red
        // ... existing cases
    }
}
```

### 4. Optional: Implement notifyNewMessage() in NotificationHelper
**File**: `NotificationHelper.kt`

**Changes**:
- Add method to create notifications for new messages
- Called from ChatRepository when new message arrives

```kotlin
/**
 * New Message - Sent to recipient when new message arrives
 */
fun notifyNewMessage(
    recipientId: String,
    senderId: String,
    senderName: String,
    messagePreview: String,
    chatId: String
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val notification = Notification(
                userId = recipientId,
                title = "New Message from $senderName",
                description = messagePreview,
                category = NotificationCategory.MESSAGES.name,
                actionType = NotificationActionType.REPLY_MESSAGE.name,
                actionData = mapOf("chat_id" to chatId, "sender_id" to senderId),
                senderName = senderName
            )
            val result = notificationRepository.createNotification(notification)
            if (result.isSuccess) {
                Log.d(TAG, "New message notification created for recipient: $recipientId")
            } else {
                Log.e(TAG, "Failed to create message notification", result.exceptionOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating message notification", e)
        }
    }
}
```

---

## Summary of Changes

| Category | Current | Recommended | Buyer | Seller | Notes |
|----------|---------|-------------|-------|--------|-------|
| UNREAD | ✅ Tab | ✅ Tab | ✅ | ✅ | Filter to unread only |
| ALL | ✅ Tab | ✅ Tab | ✅ | ✅ | Show all notifications |
| ORDERS | ✅ Tab | ✅ Tab | ✅ | ✅ | Order status updates |
| PAYMENTS | ✅ Tab | ✅ Tab | ✅ | ✅ | Payment events |
| REFUNDS | ❌ Missing | ✅ Add | ✅ | ✅ | Refund workflow |
| MESSAGES | ✅ Tab | ✅ Tab | ✅ | ✅ | Chat messages (needs implementation) |
| PROMOTIONS | ✅ Tab | ✅ Tab | ✅ | ❌ Hide | Buyer-only offers |
| SYSTEM | ✅ Tab | ✅ Tab | ✅ | ✅ | System messages |
| STORE_RATING | ❌ Missing | ✅ Add | ❌ | ✅ | Seller-only ratings |
| REPORT | ❌ Missing | ✅ Add | ❌ | ✅ | Seller-only reports |

---

## Implementation Priority

1. **High Priority** (Do First):
   - Add REFUNDS tab to both buyer and seller
   - Hide PROMOTIONS tab on seller side
   - Add STORE_RATING tab to seller side
   - Add REPORT tab to seller side

2. **Medium Priority** (Do Next):
   - Add category icons and colors for new tabs
   - Update NotificationFilterTabs to accept userRole parameter

3. **Low Priority** (Optional):
   - Implement notifyNewMessage() in NotificationHelper
   - Integrate message notifications with ChatRepository

---

## Testing Checklist

- [ ] Buyer sees: Unread, All, Orders, Payments, Refunds, Messages, Promotions, System
- [ ] Seller sees: Unread, All, Orders, Payments, Refunds, Messages, System, Store Rating, Reports
- [ ] Seller does NOT see: Promotions tab
- [ ] Buyer does NOT see: Store Rating, Reports tabs
- [ ] Refund notifications appear in REFUNDS tab
- [ ] Store rating notifications appear in STORE_RATING tab (seller only)
- [ ] Product report notifications appear in REPORT tab (seller only)
- [ ] Filter switching works smoothly
- [ ] Icons and colors display correctly for new categories
