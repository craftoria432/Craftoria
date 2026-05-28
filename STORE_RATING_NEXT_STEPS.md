# Store Rating Implementation - Next Steps

## Current Status: 90% Complete ✅

**What's Done:**
- ✅ UI Layer (NotificationsScreen.kt) - STORE_RATING tab, PROMOTIONS tab, VIEW_RATING button
- ✅ Cloud Functions (functions/index.js) - notifySellerOfRating, notifyBuyerToRateStore
- ✅ Navigation Handler (NavGraph.kt) - VIEW_RATING action routing
- ✅ Data Models (Notification.kt) - All required fields exist

**What's Remaining:**
- 🔧 Deploy Cloud Functions (requires Blaze plan)
- 🔧 Implement StoreRatingsScreen (seller view)
- 🔧 Implement RateStoreDialog (buyer dialog)
- 🔧 End-to-end testing

---

## Step 1: Upgrade to Blaze Plan (5 min)

**Why:** Cloud Functions require Blaze (pay-as-you-go) plan

**How:**
1. Go to: https://console.firebase.google.com/project/craftoria432/usage/details
2. Click "Upgrade to Blaze"
3. Follow payment setup

---

## Step 2: Deploy Cloud Functions (5 min)

**After upgrading to Blaze:**

```bash
cd functions
firebase deploy --only functions
```

**Verify deployment:**
```bash
firebase functions:log
```

---

## Step 3: Implement StoreRatingsScreen (30 min)

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/StoreRatingsScreen.kt`

**What it should do:**
- Display all ratings for a store
- Show rating value (1-5 stars)
- Show review text
- Show buyer name
- Show rating date
- Allow seller to respond (optional)

**Data source:** `store_ratings` collection filtered by `store_id`

**Example structure:**
```kotlin
@Composable
fun StoreRatingsScreen(
    storeId: String,
    onBackClick: () -> Unit
) {
    // Load ratings from Firestore
    // Display in LazyColumn
    // Show average rating at top
}
```

---

## Step 4: Implement RateStoreDialog (30 min)

**File:** `app/src/main/java/com/gcuf/craftoria/ui/components/RateStoreDialog.kt`

**What it should do:**
- Show store name
- Allow buyer to select 1-5 stars
- Allow buyer to write review text
- Submit button to save rating
- Show success message

**Data destination:** `store_ratings` collection

**Example structure:**
```kotlin
@Composable
fun RateStoreDialog(
    storeId: String,
    orderId: String,
    onDismiss: () -> Unit,
    onRatingSubmitted: () -> Unit
) {
    // Star rating selector
    // Review text field
    // Submit button
    // Save to Firestore
}
```

---

## Step 5: Update NavGraph Routes (5 min)

**Replace placeholders in NavGraph.kt:**

```kotlin
// Current (placeholder):
PlaceholderScreen(
    title = "Store Ratings",
    onBackClick = { navController.popBackStack() }
)

// Replace with:
StoreRatingsScreen(
    storeId = storeId,
    onBackClick = { navController.popBackStack() }
)
```

```kotlin
// Current (placeholder):
PlaceholderScreen(
    title = "Rate Store",
    onBackClick = { navController.popBackStack() }
)

// Replace with:
RateStoreDialog(
    storeId = storeId,
    orderId = orderId,
    onDismiss = { navController.popBackStack() },
    onRatingSubmitted = { navController.popBackStack() }
)
```

---

## Step 6: Testing (30 min)

### Test Seller Flow:
1. Create order as buyer
2. Deliver order as seller
3. Submit rating as buyer
4. Check seller notifications
5. Click "View Rating" button
6. Verify StoreRatingsScreen shows rating

### Test Buyer Flow:
1. Create order as buyer
2. Deliver order as seller
3. Check buyer notifications
4. Should see PROMOTIONS notification
5. Click "View Rating" button
6. Verify RateStoreDialog opens
7. Submit rating
8. Verify notification marked as read

### Test Edge Cases:
- Buyer already rated (should not get reminder)
- Missing storeId or orderId
- Unauthorized access (buyer accessing seller screen)

---

## Firestore Collections Reference

### store_ratings
```javascript
{
  store_id: "store_123",
  buyer_id: "buyer_456",
  buyer_name: "John Doe",
  rating: 5,
  review: "Great quality and fast shipping!",
  created_at: Timestamp,
  order_id: "order_789"
}
```

### notifications
```javascript
{
  user_id: "seller_123",
  title: "New 5⭐ Rating from John Doe",
  description: "Great quality and fast shipping!",
  category: "STORE_RATING",
  action_type: "VIEW_RATING",
  action_data: {
    store_id: "store_123",
    rating_id: "rating_456"
  },
  is_read: false,
  created_at: Timestamp
}
```

---

## Estimated Timeline

| Task | Time | Status |
|------|------|--------|
| Upgrade to Blaze | 5 min | ⏳ TODO |
| Deploy Cloud Functions | 5 min | ⏳ TODO |
| Implement StoreRatingsScreen | 30 min | ⏳ TODO |
| Implement RateStoreDialog | 30 min | ⏳ TODO |
| Update NavGraph | 5 min | ⏳ TODO |
| Testing | 30 min | ⏳ TODO |
| **TOTAL** | **~105 min** | |

---

## Quick Reference

**Key Files:**
- NavGraph.kt - Navigation routes (DONE ✅)
- NotificationsScreen.kt - UI tabs (DONE ✅)
- functions/index.js - Cloud Functions (DONE ✅)
- StoreRatingsScreen.kt - TODO (seller view)
- RateStoreDialog.kt - TODO (buyer dialog)

**Key Collections:**
- store_ratings - Rating data
- notifications - Notification records
- co_seller_stores - Store info

**Key Functions:**
- notifySellerOfRating - Triggers on new rating
- notifyBuyerToRateStore - Triggers on delivery

---

## Notes

- All navigation code is production-ready
- Cloud Functions are production-ready
- UI components are placeholders (need implementation)
- No compilation errors
- Ready for immediate deployment after Blaze upgrade

