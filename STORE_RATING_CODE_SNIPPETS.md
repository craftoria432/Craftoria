# Store Rating Implementation - Code Snippets

## 1. Cloud Functions (functions/index.js)

### Function 1: Notify Seller When Buyer Rates Store

```javascript
/**
 * Trigger: When a new rating is submitted to store_ratings collection
 * Action: Create STORE_RATING notification for store owner
 * 
 * Notification Fields:
 * - category: STORE_RATING (seller-only tab)
 * - action_type: VIEW_RATING
 * - rating_value: Star rating (1-5)
 * - rating_review: Review text
 * - buyer_name: Name of buyer who rated
 */
exports.notifySellerOfRating = functions.firestore
  .document('store_ratings/{ratingId}')
  .onCreate(async (snap, context) => {
    try {
      const rating = snap.data();
      const ratingId = context.params.ratingId;
      
      // Extract rating data
      const storeId = rating.store_id;
      const buyerId = rating.buyer_id;
      const buyerName = rating.buyer_name || 'A buyer';
      const ratingValue = rating.rating || 0;
      const ratingReview = rating.review || '';

      console.log(`New rating received for store: ${storeId}, rating: ${ratingValue}⭐`);

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
          rating_id: ratingId,
          buyer_id: buyerId
        },
        store_id: storeId,
        store_name: storeName,
        rating_value: ratingValue,
        rating_review: ratingReview,
        buyer_name: buyerName
      };

      // Add notification to Firestore
      const notifRef = await admin.firestore()
        .collection('notifications')
        .add(notification);

      console.log('✅ Seller rating notification created:', notifRef.id);
      console.log('   Seller ID:', storeOwnerId);
      console.log('   Store:', storeName);
      console.log('   Rating:', `${ratingValue}⭐ from ${buyerName}`);

    } catch (error) {
      console.error('Error in notifySellerOfRating:', error);
    }
  });
```

### Function 2: Remind Buyer to Rate Store After Delivery

```javascript
/**
 * Trigger: When order status changes to DELIVERED
 * Action: Create PROMOTIONS notification for buyer (rating reminder)
 * 
 * Notification Fields:
 * - category: PROMOTIONS (engagement/feedback)
 * - action_type: VIEW_RATING
 * - Skips if buyer already rated this store
 */
exports.notifyBuyerToRateStore = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    try {
      const oldData = change.before.data();
      const newData = change.after.data();
      const orderId = context.params.orderId;

      // Only trigger when status changes to DELIVERED
      if (oldData.status !== 'DELIVERED' && newData.status === 'DELIVERED') {
        
        const buyerId = newData.buyer_id;
        const storeId = newData.store_id;
        const storeName = newData.store_name || 'Store';
        const productName = newData.items?.[0]?.product_name || 'Your order';

        console.log(`Order ${orderId} delivered. Checking if buyer should be reminded to rate...`);

        // Check if buyer has already rated this store
        const existingRating = await admin.firestore()
          .collection('store_ratings')
          .where('store_id', '==', storeId)
          .where('buyer_id', '==', buyerId)
          .limit(1)
          .get();

        if (!existingRating.empty) {
          console.log(`✅ Buyer ${buyerId} already rated store ${storeId}. Skipping reminder.`);
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

        // Add notification to Firestore
        const notifRef = await admin.firestore()
          .collection('notifications')
          .add(notification);

        console.log('✅ Buyer rating reminder created:', notifRef.id);
        console.log('   Buyer ID:', buyerId);
        console.log('   Store:', storeName);
        console.log('   Order:', orderId);

      } else if (oldData.status === 'DELIVERED' && newData.status !== 'DELIVERED') {
        console.log(`Order ${orderId} status changed away from DELIVERED. No action needed.`);
      }

    } catch (error) {
      console.error('Error in notifyBuyerToRateStore:', error);
    }
  });
```

---

## 2. Navigation Handler (NavGraph.kt)

Add this to your notification action handler:

```kotlin
// In NotificationsScreen or wherever you handle notification actions
when (notification.actionTypeEnum) {
    NotificationActionType.VIEW_RATING -> {
        // Different navigation based on user role
        if (user.role == "seller") {
            // Seller: Navigate to store ratings view
            // Shows all ratings received for this store
            navController.navigate(
                "store_ratings/${notification.storeId}"
            )
        } else {
            // Buyer: Navigate to rate store dialog
            // Opens dialog to submit rating
            navController.navigate(
                "rate_store/${notification.storeId}/${notification.orderId}"
            )
        }
        
        // Mark notification as read
        if (!notification.isRead) {
            notificationViewModel.markAsRead(notification.id, user.id)
        }
    }
    // ... other action types
}
```

---

## 3. Firestore Rules (firestore.rules)

Ensure these rules allow proper access:

```javascript
// Store ratings collection
match /store_ratings/{document=**} {
  // Buyers can read ratings for stores they're viewing
  allow read: if request.auth != null;
  
  // Buyers can create ratings for stores
  allow create: if request.auth != null 
    && request.resource.data.buyer_id == request.auth.uid;
  
  // Buyers can update their own ratings
  allow update: if request.auth != null 
    && resource.data.buyer_id == request.auth.uid;
  
  // Sellers can read ratings for their stores
  allow read: if request.auth != null;
}

// Notifications collection
match /notifications/{document=**} {
  // Users can read their own notifications
  allow read: if request.auth != null 
    && resource.data.user_id == request.auth.uid;
  
  // Cloud Functions can write notifications
  allow write: if request.auth.uid == null;  // Service account
  
  // Users can update their own notifications (mark as read, etc.)
  allow update: if request.auth != null 
    && resource.data.user_id == request.auth.uid;
  
  // Users can delete their own notifications
  allow delete: if request.auth != null 
    && resource.data.user_id == request.auth.uid;
}
```

---

## 4. Testing Cloud Functions Locally

### Test notifySellerOfRating

```javascript
// Simulate a new rating being submitted
const testRating = {
  store_id: 'store_123',
  buyer_id: 'buyer_456',
  buyer_name: 'John Doe',
  rating: 5,
  review: 'Great quality and fast shipping!',
  created_at: new Date()
};

// Add to store_ratings collection
db.collection('store_ratings').add(testRating);

// Check Cloud Functions logs
firebase functions:log
```

### Test notifyBuyerToRateStore

```javascript
// Simulate order status change to DELIVERED
const testOrder = {
  buyer_id: 'buyer_456',
  store_id: 'store_123',
  store_name: 'My Store',
  status: 'DELIVERED',
  items: [{ product_name: 'Test Product' }]
};

// Update order status
db.collection('orders').doc('order_789').update({
  status: 'DELIVERED'
});

// Check Cloud Functions logs
firebase functions:log
```

---

## 5. Verify Notifications Created

### Query Seller Notifications

```javascript
// In Firebase Console or via script
db.collection('notifications')
  .where('user_id', '==', 'seller_id')
  .where('category', '==', 'STORE_RATING')
  .orderBy('created_at', 'desc')
  .get()
  .then(snapshot => {
    snapshot.forEach(doc => {
      console.log(doc.data());
    });
  });
```

### Query Buyer Notifications

```javascript
db.collection('notifications')
  .where('user_id', '==', 'buyer_id')
  .where('category', '==', 'PROMOTIONS')
  .where('action_type', '==', 'VIEW_RATING')
  .orderBy('created_at', 'desc')
  .get()
  .then(snapshot => {
    snapshot.forEach(doc => {
      console.log(doc.data());
    });
  });
```

---

## 6. Android Navigation Routes

Add these routes to your NavGraph:

```kotlin
// In NavGraph.kt
composable("store_ratings/{storeId}") { backStackEntry ->
    val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
    StoreRatingsScreen(
        storeId = storeId,
        onBackClick = { navController.popBackStack() }
    )
}

composable("rate_store/{storeId}/{orderId}") { backStackEntry ->
    val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
    RateStoreDialog(
        storeId = storeId,
        orderId = orderId,
        onDismiss = { navController.popBackStack() },
        onRatingSubmitted = { navController.popBackStack() }
    )
}
```

---

## 7. Deployment Commands

```bash
# Deploy Cloud Functions
cd functions
firebase deploy --only functions:notifySellerOfRating,functions:notifyBuyerToRateStore

# Or deploy all functions
firebase deploy --only functions

# View logs
firebase functions:log

# View specific function logs
firebase functions:log --function=notifySellerOfRating
firebase functions:log --function=notifyBuyerToRateStore
```

---

## 8. Troubleshooting

### Function Not Triggering

```javascript
// Add logging to verify trigger
console.log('Document created:', snap.id);
console.log('Data:', snap.data());

// Check if conditions are met
if (!storeId) {
  console.error('Missing store_id');
  return;
}
```

### Notification Not Appearing

```javascript
// Verify notification was created
db.collection('notifications')
  .where('user_id', '==', userId)
  .get()
  .then(snapshot => {
    console.log('Total notifications:', snapshot.size);
    snapshot.forEach(doc => {
      console.log(doc.data());
    });
  });
```

### Wrong Category/Action Type

```javascript
// Verify correct values
console.log('Category:', notification.category);  // Should be 'STORE_RATING' or 'PROMOTIONS'
console.log('Action Type:', notification.action_type);  // Should be 'VIEW_RATING'
```

---

## Summary

**Seller Flow:**
1. Buyer submits rating → Cloud Function triggers
2. Creates STORE_RATING notification for seller
3. Seller sees in STORE_RATING tab
4. Clicks "View Rating" → navigates to store ratings

**Buyer Flow:**
1. Order delivered → Cloud Function triggers
2. Checks if buyer already rated
3. If not, creates PROMOTIONS notification
4. Buyer sees in PROMOTIONS tab
5. Clicks "View Rating" → opens rate dialog

**Key Points:**
- ✅ UI already implemented (NotificationsScreen.kt)
- 🔧 Cloud Functions need to be deployed
- 🔧 Navigation handler needs to be added
- ✅ Data models already exist (Notification.kt)
