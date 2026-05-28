# Store Rating Tab & Buyer Rating Reminders Implementation

## Overview
This implementation adds:
1. **STORE_RATING tab for sellers only** - Sellers see notifications when they receive ratings from buyers
2. **Rating reminders under PROMOTIONS for buyers** - Engagement/feedback requests to encourage buyers to rate stores

---

## ✅ COMPLETED: UI Changes

### 1. Notification Filter Tabs (NotificationsScreen.kt)
**Status:** ✅ IMPLEMENTED

The filter tabs now correctly show:

**Buyer Tabs:**
- Unread · All · Orders · Payments · Refunds · Messages · **Promotions** · System

**Seller Tabs:**
- Unread · All · Orders · Payments · Refunds · Messages · **Store Rating** · System · Reports

**Key Points:**
- Sellers see `STORE_RATING` tab (for ratings they receive)
- Buyers see `PROMOTIONS` tab (for engagement/feedback requests)
- Both tabs already exist in `NotificationCategory` enum
- Role-based filtering via `userRole` parameter

### 2. Notification Action Type (NotificationsScreen.kt)
**Status:** ✅ IMPLEMENTED

Added `VIEW_RATING` action button with:
- Orange gradient background (Color(0xFFFFA500) to Color(0xFFFFB84D))
- "View Rating" button text
- Consistent with other action buttons

---

## 🔧 NEXT STEPS: Backend Implementation

### Step 1: Create Store Rating Notifications (Cloud Functions)
**File:** `functions/index.js`

Add a trigger when a store receives a new rating:

```javascript
// Trigger: When a new rating is submitted to store_ratings collection
exports.notifySellerOfRating = functions.firestore
  .document('store_ratings/{ratingId}')
  .onCreate(async (snap, context) => {
    const rating = snap.data();
    const storeId = rating.store_id;
    const buyerId = rating.buyer_id;
    const buyerName = rating.buyer_name || 'A buyer';
    const ratingValue = rating.rating || 0;
    const ratingReview = rating.review || '';

    // Get store owner ID from co_seller_stores
    const storeDoc = await admin.firestore()
      .collection('co_seller_stores')
      .doc(storeId)
      .get();

    if (!storeDoc.exists) {
      console.log('Store not found:', storeId);
      return;
    }

    const storeOwnerId = storeDoc.data().owner_id;
    const storeName = storeDoc.data().store_name || 'Your Store';

    // Create notification for store owner
    const notification = {
      user_id: storeOwnerId,
      title: `New ${ratingValue}⭐ Rating from ${buyerName}`,
      description: ratingReview || `${buyerName} rated your store ${ratingValue} stars`,
      category: 'STORE_RATING',  // ✅ Seller-only category
      is_read: false,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
      action_type: 'VIEW_RATING',
      action_data: {
        store_id: storeId,
        rating_id: context.params.ratingId,
        buyer_id: buyerId
      },
      store_id: storeId,
      store_name: storeName,
      rating_value: ratingValue,
      rating_review: ratingReview,
      buyer_name: buyerName
    };

    await admin.firestore()
      .collection('notifications')
      .add(notification);

    console.log('Seller rating notification created for:', storeOwnerId);
  });
```

### Step 2: Create Buyer Rating Reminders (Cloud Functions)
**File:** `functions/index.js`

Add a trigger when an order is delivered to remind buyer to rate:

```javascript
// Trigger: When order status changes to DELIVERED
exports.notifyBuyerToRateStore = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    const oldData = change.before.data();
    const newData = change.after.data();

    // Only trigger when status changes to DELIVERED
    if (oldData.status !== 'DELIVERED' && newData.status === 'DELIVERED') {
      const orderId = context.params.orderId;
      const buyerId = newData.buyer_id;
      const storeId = newData.store_id;
      const storeName = newData.store_name || 'Store';
      const productName = newData.items?.[0]?.product_name || 'Your order';

      // Check if buyer has already rated this store
      const existingRating = await admin.firestore()
        .collection('store_ratings')
        .where('store_id', '==', storeId)
        .where('buyer_id', '==', buyerId)
        .limit(1)
        .get();

      if (!existingRating.empty) {
        console.log('Buyer already rated this store');
        return;
      }

      // Create rating reminder notification
      const notification = {
        user_id: buyerId,
        title: `Rate ${storeName}`,
        description: `How was your experience with ${storeName}? Your feedback helps us improve.`,
        category: 'PROMOTIONS',  // ✅ Buyer engagement/feedback
        is_read: false,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
        action_type: 'VIEW_RATING',
        action_data: {
          store_id: storeId,
          order_id: orderId
        },
        store_id: storeId,
        store_name: storeName,
        order_id: orderId
      };

      await admin.firestore()
        .collection('notifications')
        .add(notification);

      console.log('Buyer rating reminder created for:', buyerId);
    }
  });
```

### Step 3: Handle VIEW_RATING Action in Navigation
**File:** `app/src/main/java/com/gcuf/craftoria/ui/navigation/NavGraph.kt`

Add navigation handling for rating notifications:

```kotlin
// In the notification action handler
when (notification.actionTypeEnum) {
    NotificationActionType.VIEW_RATING -> {
        // For sellers: Navigate to store ratings view
        if (user.role == "seller") {
            navController.navigate("store_ratings/${notification.storeId}")
        }
        // For buyers: Navigate to rate store dialog or screen
        else {
            navController.navigate("rate_store/${notification.storeId}/${notification.orderId}")
        }
    }
    // ... other cases
}
```

---

## 📊 Data Flow

### Seller Receives Rating
```
Buyer submits rating
    ↓
store_ratings collection updated
    ↓
Cloud Function: notifySellerOfRating triggered
    ↓
Notification created with:
  - category: STORE_RATING
  - action_type: VIEW_RATING
  - Seller sees in STORE_RATING tab
    ↓
Seller clicks "View Rating"
    ↓
Navigate to store ratings screen
```

### Buyer Gets Rating Reminder
```
Order status → DELIVERED
    ↓
Cloud Function: notifyBuyerToRateStore triggered
    ↓
Check if buyer already rated this store
    ↓
If not rated, create notification with:
  - category: PROMOTIONS
  - action_type: VIEW_RATING
  - Buyer sees in PROMOTIONS tab
    ↓
Buyer clicks "View Rating"
    ↓
Navigate to rate store dialog
```

---

## 🎨 UI Behavior

### Seller View - STORE_RATING Tab
- **Icon:** ⭐ (Star)
- **Color:** Orange (0xFFFFA500)
- **Background:** Light orange (0xFFFFF3E0)
- **Content:** Shows all ratings received
- **Action Button:** "View Rating" (orange gradient)

### Buyer View - PROMOTIONS Tab
- **Icon:** 📢 (Campaign)
- **Color:** Yellow (0xFFF57F17)
- **Background:** Light yellow (0xFFFFF9C4)
- **Content:** Shows rating reminders + other promotions
- **Action Button:** "View Rating" (orange gradient)

---

## ✅ Testing Checklist

### Seller Testing
- [ ] Seller receives notification when buyer submits rating
- [ ] Notification appears in STORE_RATING tab only
- [ ] Notification shows buyer name and rating value
- [ ] "View Rating" button navigates to store ratings
- [ ] Unread badge updates correctly

### Buyer Testing
- [ ] Buyer receives reminder after order delivered
- [ ] Reminder appears in PROMOTIONS tab
- [ ] Reminder doesn't appear if already rated
- [ ] "View Rating" button opens rate store dialog
- [ ] Can dismiss or rate from notification

### General Testing
- [ ] Filter tabs show correct role-based options
- [ ] Notifications persist across app restarts
- [ ] Real-time updates work correctly
- [ ] Mark as read/delete works for rating notifications

---

## 📝 Implementation Notes

### Why STORE_RATING for Sellers?
- Sellers need dedicated tab to track customer feedback
- Separate from ORDERS/PAYMENTS for better organization
- Encourages sellers to monitor and respond to ratings

### Why PROMOTIONS for Buyers?
- Rating reminders are engagement/feedback requests, not critical
- Grouped with other promotional content
- Less intrusive than separate tab
- Buyers can dismiss if not interested

### Category vs Action Type
- **Category:** Determines which tab notification appears in
- **Action Type:** Determines what button/action is shown
- Both can be different (e.g., PROMOTIONS category with VIEW_RATING action)

---

## 🔗 Related Files

**Already Implemented:**
- ✅ `Notification.kt` - STORE_RATING category exists
- ✅ `NotificationActionType.kt` - VIEW_RATING action exists
- ✅ `NotificationsScreen.kt` - Filter tabs and action buttons
- ✅ `NotificationRepository.kt` - Filtering logic

**Need to Implement:**
- 🔧 `functions/index.js` - Cloud Functions for notifications
- 🔧 `NavGraph.kt` - Navigation handling
- 🔧 Store ratings screen/dialog (if not exists)

---

## 🚀 Deployment Steps

1. **Deploy Cloud Functions:**
   ```bash
   cd functions
   firebase deploy --only functions
   ```

2. **Update Android App:**
   - Rebuild with latest code
   - Test on device/emulator

3. **Monitor:**
   - Check Cloud Functions logs
   - Verify notifications appear in Firestore
   - Test end-to-end flow

---

## 💡 Future Enhancements

- Add email notifications for seller ratings
- Implement rating response system (sellers reply to reviews)
- Add rating analytics dashboard
- Implement rating-based seller badges
- Auto-generate rating reminders at specific intervals
