# Real-Time Negotiation Cart Update - Quick Test Guide

## What Changed?
When a seller accepts or rejects an offer in chat, the buyer's cart now updates **instantly** without requiring navigation.

## How to Test

### Setup
1. Have 2 devices/emulators ready (or 2 browser windows for web)
2. One logged in as **Buyer**, one as **Seller**

### Test Case 1: Accept Offer ✅

**Buyer Side:**
1. Open app → Home Screen
2. Find a product → Click it
3. Click "Negotiate" button
4. Enter offer price (e.g., PKR 800 for PKR 1000 product)
5. Send negotiation message
6. **Keep cart screen open** (or navigate to cart)
7. Watch for update...

**Seller Side:**
1. Open app → Messages
2. Find buyer's chat
3. See negotiation message: "Offer PKR 800"
4. Click "Accept" button
5. Confirm acceptance

**Buyer Side - Expected Result:**
- ✅ Cart item price changes from PKR 1000 → PKR 800
- ✅ Status badge changes from "Pending" → "Negotiated"
- ✅ **No navigation required** - update happens on current screen
- ✅ Subtotal and total update automatically

### Test Case 2: Reject Offer ❌

**Buyer Side:**
1. Open app → Home Screen
2. Find a product → Click it
3. Click "Negotiate" button
4. Enter offer price (e.g., PKR 500)
5. Send negotiation message
6. **Keep cart screen open**
7. Watch for update...

**Seller Side:**
1. Open app → Messages
2. Find buyer's chat
3. See negotiation message: "Offer PKR 500"
4. Click "Decline" button
5. Confirm rejection

**Buyer Side - Expected Result:**
- ✅ Cart item price stays PKR 1000 (unchanged)
- ✅ Status badge changes from "Pending" → "Rejected"
- ✅ **No navigation required** - update happens on current screen
- ✅ Subtotal and total remain the same

### Test Case 3: Multiple Items

**Buyer Side:**
1. Add 2 products to cart
2. Send negotiation offers for both
3. **Keep cart screen open**

**Seller Side:**
1. Accept offer for product 1
2. Reject offer for product 2

**Buyer Side - Expected Result:**
- ✅ Product 1: Price updated, status "Negotiated"
- ✅ Product 2: Price unchanged, status "Rejected"
- ✅ Both update independently in real-time
- ✅ Totals recalculate correctly

### Test Case 4: Multiple Sellers

**Buyer Side:**
1. Add products from 2 different sellers
2. Send negotiation offers for both
3. **Keep cart screen open**

**Seller 1 Side:**
1. Accept offer

**Seller 2 Side:**
1. Reject offer

**Buyer Side - Expected Result:**
- ✅ Seller 1 item: Updated with accepted price
- ✅ Seller 2 item: Unchanged with rejected status
- ✅ Shipping calculated correctly for both
- ✅ Total reflects all changes

## What to Look For

### ✅ Success Indicators
- [ ] Price updates without page refresh
- [ ] Status badge appears/changes instantly
- [ ] Subtotal recalculates
- [ ] Total recalculates
- [ ] No navigation required
- [ ] No loading spinner
- [ ] Smooth animation (if any)

### ❌ Failure Indicators
- [ ] Price doesn't update
- [ ] Status badge doesn't appear
- [ ] Need to navigate away and back to see update
- [ ] Need to refresh page
- [ ] Error message appears
- [ ] Cart becomes empty unexpectedly

## Debug Logs to Check

Open Android Studio Logcat and filter for:
```
CartRepository
ChatRepository
CartViewModel
```

**Expected logs when seller accepts:**
```
✅ Cart item updated with negotiation status: ACCEPTED
✅ Cart updated: X items
```

**Expected logs when seller rejects:**
```
✅ Cart item updated with negotiation status: REJECTED
✅ Cart updated: X items
```

## Troubleshooting

### Issue: Update doesn't appear
**Check:**
1. Is CartScreen still open? (It should be)
2. Is Firebase connection active?
3. Check logcat for errors
4. Try refreshing the app

### Issue: Price shows old value
**Check:**
1. Is negotiationPrice set in the message?
2. Is the cart item being found in Firebase?
3. Check Firestore console to verify cart item was updated

### Issue: Status badge doesn't show
**Check:**
1. Is negotiationStatus being set?
2. Is CartItemCard rendering the badge?
3. Check if NegotiationStatus enum value is correct

## Performance Notes

- Real-time updates should appear within **1-2 seconds**
- No lag or stuttering expected
- Multiple items update smoothly
- No battery drain from continuous listening

## Files Modified

1. **ChatRepository.kt**
   - `updateNegotiationStatus()` - Now updates cart item
   - `updateCartItemNegotiationStatus()` - New helper function

2. **No changes needed in:**
   - CartRepository.kt (already supports real-time)
   - CartViewModel.kt (already listening)
   - CartScreen.kt (already displaying)

## Next Steps After Testing

1. ✅ Verify all test cases pass
2. ✅ Check performance on real devices
3. ✅ Test with slow network (Settings → Developer Options → Network throttling)
4. ✅ Test with Firebase offline mode
5. ✅ Deploy to production

## Questions?

If updates don't appear:
1. Check Firebase Firestore console
2. Verify cart collection has the updated item
3. Check Android Studio Logcat for errors
4. Verify Firestore rules allow cart updates
