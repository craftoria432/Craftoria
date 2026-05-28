# Notification System - Quick Reference

## Problem & Solution

### Issue: Stale Data in Notifications
**Problem**: Notifications showed outdated seller names and incorrect member counts
- Seller name: "Zara Ahmed" (should be "Zara Ali")
- Member count: "0 Members" (should be "2 Members")

**Root Cause**: Notifications stored data at creation time but didn't update when data changed

**Solution**: Implemented real-time listeners that fetch current data from Firestore

---

## Notification Categories

### 5 Main Filter Tabs

| Tab | Category | Content | Examples |
|-----|----------|---------|----------|
| **All** | ALL | All notifications | Everything |
| **Orders** | ORDERS | Order updates | Delivered, Shipped, Processing, Cancelled |
| **Messages** | MESSAGES | Chat messages | New messages, Replies |
| **Promotions** | PROMOTIONS | Marketing | Offers, Discounts, Back in Stock, Price Drops |
| **System** | SYSTEM | Admin & System | Invitations, Approvals, Verifications |

### Additional Categories (Not in Tabs)

- **PAYMENTS**: Payment received, Payout processed
- **STORE_RATING**: Store ratings, Rating reminders
- **REFUNDS**: Refund status updates
- **REPORT**: Product reports
- **ADMIN_MESSAGE**: Admin announcements

---

## Real-time Updates

### How Store Name Updates Work
```
1. Seller changes name in Profile
2. users/{sellerId} document updated in Firestore
3. Real-time listener in NotificationCard detects change
4. UI updates immediately (no refresh needed)
```

### How Member Count Updates Work
```
1. New member joins co-seller store
2. co_seller_stores/{storeId} document updated
3. Real-time listener detects change
4. UI updates immediately (no refresh needed)
```

---

## Creating Notifications for Co-Seller Orders

### ✅ CORRECT WAY
```kotlin
NotificationHelper.notifyOrderDelivered(
    buyerId = buyerId,
    orderId = orderId,
    storeName = currentStoreName,
    orderNumber = orderNumber,
    storeId = storeId  // ✅ REQUIRED for real-time updates
)
```

### ❌ WRONG WAY
```kotlin
NotificationHelper.notifyOrderDelivered(
    buyerId = buyerId,
    orderId = orderId,
    storeName = storeName,
    orderNumber = orderNumber
    // Missing storeId - real-time updates won't work!
)
```

---

## Notification Data Flow

### When Order is Placed
```
1. Order created in Firestore
2. Notification created with:
   - storeName (current name)
   - storeId (for real-time updates)
   - memberCount (current count)
3. Notification sent to buyer
```

### When Seller Updates Name
```
1. Seller updates name in Profile
2. users/{sellerId} updated in Firestore
3. Real-time listener in NotificationCard fires
4. Notification UI updates to show new name
5. No notification refresh needed
```

### When Member Joins Store
```
1. New member joins co-seller store
2. co_seller_stores/{storeId} updated
3. Real-time listener in NotificationCard fires
4. Notification UI updates member count
5. No notification refresh needed
```

---

## Notification UI Elements

### Notification Card Layout
```
┌─────────────────────────────────────┐
│ [Icon] Title              [Delete]   │ ← Unread dot (pink)
│        Description                   │
│        Store Pill (name · members)   │
│        2 minutes ago                 │
│                                      │
│ [Action Button]                      │
└─────────────────────────────────────┘
```

### Store Pill
```
🏪 Zara Ali · 👥 2 Members
```
- Updates in real-time when name or member count changes
- Shows current data from Firestore

---

## Filtering Logic

### In Code
```kotlin
// Get all notifications for user
var query = notificationsCollection.whereEqualTo("user_id", userId)

// Filter by category if not ALL
if (category != NotificationCategory.ALL) {
    query = query.whereEqualTo("category", category.name)
}

// Fetch, parse, and return
val notifications = query.limit(100).get()
    .sortedByDescending { it.createdAt }  // Newest first
    .take(50)  // Max 50 returned
```

### In UI
- User taps filter tab (All, Orders, Messages, etc.)
- ViewModel calls `filterNotifications(category, userId)`
- Repository fetches notifications for that category
- UI displays filtered list

---

## Real-time Listener Implementation

### In NotificationCard
```kotlin
DisposableEffect(notification.storeId) {
    // Set up listeners when card is created
    
    // Listener 1: Store name from users collection
    userRegistration = db.collection("users")
        .document(notification.storeId)
        .addSnapshotListener { snapshot, error ->
            if (snapshot?.exists() == true) {
                val name = snapshot.getString("name")
                realtimeStoreName = name  // Update UI
            }
        }
    
    // Listener 2: Member count from co_seller_stores
    storeRegistration = db.collection("co_seller_stores")
        .document(notification.storeId)
        .addSnapshotListener { snapshot, error ->
            if (snapshot?.exists() == true) {
                val count = snapshot.getLong("member_count")?.toInt()
                    ?: (snapshot.get("member_ids") as? List<*>)?.size
                realtimeMemberCount = count  // Update UI
            }
        }
    
    // Clean up when card is disposed
    onDispose {
        userRegistration?.remove()
        storeRegistration?.remove()
    }
}
```

---

## Troubleshooting

### Store Name Not Updating
- ✅ Check that `storeId` is set in notification
- ✅ Verify seller name is updated in `users/{sellerId}` document
- ✅ Check Firestore rules allow reading user names
- ✅ Check browser console for listener errors

### Member Count Showing 0
- ✅ Check that `storeId` is set in notification
- ✅ Verify `co_seller_stores/{storeId}` has `member_ids` or `member_count`
- ✅ Check that members are properly added to store
- ✅ Verify Firestore rules allow reading store data

### Notifications Not Filtering
- ✅ Check that `category` field is set correctly (uppercase)
- ✅ Verify user has notifications in that category
- ✅ Check that filter tab is being clicked
- ✅ Check ViewModel is calling `filterNotifications()`

---

## Key Files

| File | Purpose |
|------|---------|
| `NotificationsScreen.kt` | UI with real-time listeners |
| `NotificationRepository.kt` | Fetches notifications from Firestore |
| `NotificationViewModel.kt` | Manages notification state |
| `NotificationHelper.kt` | Creates notifications |
| `Notification.kt` | Data model |

---

## Testing Checklist

- [ ] Create order from co-seller store
- [ ] Verify notification shows correct store name
- [ ] Verify notification shows correct member count
- [ ] Change seller name in Profile
- [ ] Verify notification updates in real-time
- [ ] Add new member to co-seller store
- [ ] Verify member count updates in real-time
- [ ] Test all 5 filter tabs
- [ ] Verify notifications are categorized correctly
- [ ] Test notification actions (Track Order, Accept Invitation, etc.)

