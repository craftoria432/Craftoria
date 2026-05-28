# Firebase Cart Persistence - Testing Guide

## Quick Testing Checklist

### Test 1: Basic Cart Persistence (App Restart)
**Steps:**
1. Login to the app
2. Add 2-3 products to cart
3. Close the app completely (swipe away from recent apps)
4. Reopen the app
5. Navigate to cart

**Expected Result:** ✅ All cart items should still be there

---

### Test 2: Logout/Login Persistence
**Steps:**
1. Login as a buyer
2. Add products to cart
3. Logout from the app
4. Login again with the same account
5. Navigate to cart

**Expected Result:** ✅ Cart items should be restored

---

### Test 3: Negotiation Pending Items
**Steps:**
1. Login as buyer
2. Find a negotiable product
3. Make an offer that requires seller review (between minimum and auto-accept)
4. Item should be added to cart with "Negotiation Pending" status
5. Close the app
6. Reopen the app
7. Check cart

**Expected Result:** ✅ Item should still be in cart with PENDING status

---

### Test 4: Negotiation Approval Flow
**Steps:**
1. Buyer makes negotiation offer (item in cart with PENDING)
2. Seller approves the negotiation
3. Buyer reopens app or refreshes cart
4. Check cart item

**Expected Result:** ✅ Item should show "Negotiated Price - Accepted" with new price

---

### Test 5: Cart Operations
**Steps:**
1. Add 3 items to cart
2. Update quantity of one item
3. Remove one item
4. Close and reopen app
5. Check cart

**Expected Result:** ✅ Quantity changes and removals should persist

---

### Test 6: Order Placement
**Steps:**
1. Add items to cart
2. Proceed to checkout
3. Complete order
4. Check cart

**Expected Result:** ✅ Cart should be empty after successful order

---

### Test 7: Reorder Functionality
**Steps:**
1. Go to My Orders
2. Select a completed order
3. Click "Reorder"
4. Check cart

**Expected Result:** ✅ Order items should be added back to cart with current prices

---

### Test 8: Multiple Devices (Advanced)
**Steps:**
1. Login on Device A
2. Add items to cart
3. Login on Device B with same account
4. Check cart on Device B

**Expected Result:** ✅ Same cart items should appear on Device B

---

## Firebase Console Verification

### Check Cart Collection
1. Open Firebase Console
2. Go to Firestore Database
3. Look for `cart` collection
4. Verify documents have these fields:
   - `user_id` (string)
   - `product_id` (string)
   - `quantity` (number)
   - `price` (number)
   - `original_price` (number)
   - `is_negotiated` (boolean)
   - `negotiation_status` (string - optional)

### Sample Cart Document
```json
{
  "user_id": "abc123xyz",
  "product_id": "prod456",
  "quantity": 2,
  "price": 850,
  "original_price": 1000,
  "is_negotiated": true,
  "negotiation_status": "AUTO_ACCEPTED"
}
```

---

## Debugging Tips

### Check Logs
Look for these log tags in Logcat:
- `CartRepository` - Firebase operations
- `CartViewModel` - Cart state management
- `NegotiationDialog` - Negotiation flow

### Common Issues

**Issue: Cart not loading**
- Check if `initializeCart(userId)` is called in NavGraph
- Verify userId is not empty
- Check Firebase connection

**Issue: Items not persisting**
- Verify `addToCart()` includes userId parameter
- Check Firebase write permissions
- Look for errors in Logcat

**Issue: Negotiation status not updating**
- Check if `updateNegotiationStatus()` is called
- Verify Firebase listener is active
- Check negotiation status field in Firebase

---

## Performance Monitoring

### What to Monitor
- Cart load time (should be < 1 second)
- Real-time update latency (should be instant)
- Firebase read/write operations count
- Memory usage (Flow should not leak)

### Expected Behavior
- Cart loads immediately on screen open
- Changes reflect in real-time
- No duplicate items
- Smooth scrolling in cart list

---

## Edge Cases to Test

1. **Empty Cart**: Navigate to cart with no items
2. **Large Cart**: Add 20+ items and test performance
3. **Network Loss**: Turn off internet, try to add items (should queue)
4. **Concurrent Updates**: Update cart on two devices simultaneously
5. **Invalid Product**: Add item, then delete product from Firebase
6. **Price Changes**: Change product price, check if cart updates

---

## Success Criteria

✅ Cart persists across app restarts
✅ Cart persists across logout/login
✅ Negotiation items remain until resolved
✅ Real-time updates work correctly
✅ No duplicate items in cart
✅ Order placement clears cart
✅ Reorder adds items correctly
✅ All operations complete without errors

---

## Rollback Plan (If Issues Found)

If critical issues are discovered:
1. Revert CartViewModel to use local state
2. Remove Firebase operations from CartRepository
3. Remove `initializeCart()` calls
4. Remove userId parameters from `addToCart()` calls

**Note**: Keep CartRepository.kt for future use - it's production-ready.
