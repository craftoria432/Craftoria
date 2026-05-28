# Store Rating Tab & Buyer Reminders - Quick Implementation Guide

## ✅ What's Already Done

### UI Layer (NotificationsScreen.kt)
- ✅ STORE_RATING tab added to seller filter tabs
- ✅ PROMOTIONS tab already exists for buyers
- ✅ VIEW_RATING action button with orange gradient
- ✅ Icon and color styling for both categories

### Data Models (Notification.kt)
- ✅ STORE_RATING category enum
- ✅ VIEW_RATING action type enum
- ✅ Rating fields: ratingValue, ratingReview, buyerName

---

## 🔧 What You Need to Implement

### 1. Cloud Functions (functions/index.js)

**Add these two functions:**

#### Function A: Notify Seller When Rated
```javascript
exports.notifySellerOfRating = functions.firestore
  .document('store_ratings/{ratingId}')
  .onCreate(async (snap, context) => {
    // Trigger: New rating submitted
    // Action: Create STORE_RATING notification for seller
    // Fields: rating_value, rating_review, buyer_name
  });
```

#### Function B: Remind Buyer to Rate
```javascript
exports.notifyBuyerToRateStore = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    // Trigger: Order status → DELIVERED
    // Action: Create PROMOTIONS notification for buyer
    // Check: Skip if buyer already rated this store
  });
```

### 2. Navigation Handler (NavGraph.kt)

Add case for VIEW_RATING action:
```kotlin
NotificationActionType.VIEW_RATING -> {
  if (user.role == "seller") {
    // Navigate to seller's store ratings view
  } else {
    // Navigate to buyer's rate store dialog
  }
}
```

### 3. Firestore Rules (firestore.rules)

Ensure these collections are accessible:
- `store_ratings` - readable by store owner and buyer
- `notifications` - writable by Cloud Functions

---

## 📋 Notification Structure

### Seller Receives Rating
```json
{
  "user_id": "seller_id",
  "title": "New 5⭐ Rating from John",
  "description": "Great quality and fast shipping!",
  "category": "STORE_RATING",
  "action_type": "VIEW_RATING",
  "store_id": "store_123",
  "store_name": "My Store",
  "rating_value": 5,
  "rating_review": "Great quality and fast shipping!",
  "buyer_name": "John"
}
```

### Buyer Gets Rating Reminder
```json
{
  "user_id": "buyer_id",
  "title": "Rate My Store",
  "description": "How was your experience? Your feedback helps us improve.",
  "category": "PROMOTIONS",
  "action_type": "VIEW_RATING",
  "store_id": "store_123",
  "store_name": "My Store",
  "order_id": "order_456"
}
```

---

## 🎯 Key Implementation Points

### For Sellers
- Notification appears in **STORE_RATING tab only**
- Shows buyer name and rating value in title
- Shows review text in description
- "View Rating" button navigates to store ratings

### For Buyers
- Notification appears in **PROMOTIONS tab**
- Grouped with other engagement/feedback requests
- Only sent if order is DELIVERED
- Only sent if buyer hasn't already rated this store
- "View Rating" button opens rate store dialog

### Category vs Action Type
| Aspect | Category | Action Type |
|--------|----------|-------------|
| **Purpose** | Determines which tab | Determines button shown |
| **Seller Rating** | STORE_RATING | VIEW_RATING |
| **Buyer Reminder** | PROMOTIONS | VIEW_RATING |

---

## 🧪 Testing Scenarios

### Scenario 1: Seller Receives Rating
1. Buyer submits 5-star rating for store
2. Cloud Function triggers
3. Seller receives notification
4. Notification appears in STORE_RATING tab
5. Seller clicks "View Rating"
6. Navigates to store ratings screen

### Scenario 2: Buyer Gets Reminder
1. Order status changes to DELIVERED
2. Cloud Function checks if buyer rated store
3. If not rated, creates PROMOTIONS notification
4. Buyer sees reminder in PROMOTIONS tab
5. Buyer clicks "View Rating"
6. Opens rate store dialog

### Scenario 3: No Duplicate Reminders
1. Buyer already rated store
2. Order delivered
3. Cloud Function checks existing ratings
4. No notification created (already rated)

---

## 📱 UI Flow

### Seller Notification Flow
```
Notification received
    ↓
Appears in STORE_RATING tab
    ↓
Shows: "New 5⭐ Rating from John"
       "Great quality and fast shipping!"
    ↓
Click "View Rating" button
    ↓
Navigate to store ratings screen
```

### Buyer Notification Flow
```
Order delivered
    ↓
Appears in PROMOTIONS tab
    ↓
Shows: "Rate My Store"
       "How was your experience?"
    ↓
Click "View Rating" button
    ↓
Open rate store dialog
```

---

## 🔍 Debugging Tips

### If Seller Doesn't See Notification
- Check Cloud Function logs
- Verify store_id is correct
- Ensure seller is owner of store
- Check notification category is STORE_RATING

### If Buyer Doesn't See Reminder
- Check order status is DELIVERED
- Verify buyer hasn't already rated
- Check notification category is PROMOTIONS
- Ensure order has store_id field

### If Button Doesn't Work
- Verify action_type is VIEW_RATING
- Check navigation handler exists
- Ensure store_id/order_id in action_data
- Test with hardcoded navigation first

---

## 📊 Database Queries

### Check Seller Ratings Notifications
```javascript
db.collection('notifications')
  .where('user_id', '==', 'seller_id')
  .where('category', '==', 'STORE_RATING')
  .get()
```

### Check Buyer Rating Reminders
```javascript
db.collection('notifications')
  .where('user_id', '==', 'buyer_id')
  .where('category', '==', 'PROMOTIONS')
  .where('action_type', '==', 'VIEW_RATING')
  .get()
```

### Check Store Ratings
```javascript
db.collection('store_ratings')
  .where('store_id', '==', 'store_id')
  .get()
```

---

## ✅ Completion Checklist

- [ ] Cloud Function: notifySellerOfRating deployed
- [ ] Cloud Function: notifyBuyerToRateStore deployed
- [ ] Navigation handler for VIEW_RATING added
- [ ] Seller can see STORE_RATING tab
- [ ] Buyer can see PROMOTIONS tab
- [ ] Seller receives notification when rated
- [ ] Buyer receives reminder after delivery
- [ ] "View Rating" button works for both
- [ ] No duplicate reminders for already-rated stores
- [ ] Notifications persist across app restarts
- [ ] Real-time updates work correctly

---

## 🚀 Deployment Order

1. Deploy Cloud Functions first
2. Rebuild Android app
3. Test seller rating flow
4. Test buyer reminder flow
5. Monitor logs for errors
6. Adjust as needed

---

## 💬 Questions?

Refer to:
- `STORE_RATING_TAB_AND_BUYER_REMINDERS_IMPLEMENTATION.md` - Full details
- `NotificationsScreen.kt` - UI implementation
- `Notification.kt` - Data model
- `NotificationRepository.kt` - Data access
