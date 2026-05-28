# Co-Seller Payment Real-Time Updates & Store Identification - Testing Guide

## Quick Test Scenarios

### Scenario 1: Real-Time Payment Updates

**Setup**:
1. Create a co-seller store with multiple members
2. Add products to the store
3. Have a buyer purchase from the co-seller store

**Test Steps**:
1. Open seller's payment screen for the co-seller store
2. Have buyer complete checkout
3. **Expected**: Payment appears instantly (within 1-2 seconds)
4. **Verify**: Payment shows correct amount and store ID

**What's Being Tested**:
- `startRealtimePaymentListener()` catches new payments
- Code-level filtering works correctly
- Real-time updates fire immediately

---

### Scenario 2: Store Identification on Orders

**Setup**:
1. Create a co-seller store
2. Add products to the store
3. Have a buyer purchase from the co-seller store

**Test Steps**:
1. Open seller's order screen
2. View the co-seller order
3. **Expected**: See "From: [Store Name]" badge below buyer name
4. **Verify**: Badge shows correct store name

**What's Being Tested**:
- `CoSellerStoreBadge` component loads store name
- Badge displays only for co-seller orders
- Store name loads from Firestore correctly

---

### Scenario 3: Multiple Co-Seller Orders

**Setup**:
1. Create 2 different co-seller stores
2. Add products to both stores
3. Have buyer purchase from both stores in same order

**Test Steps**:
1. Open seller's order screen
2. View the order with items from multiple stores
3. **Expected**: Each item shows its respective store badge
4. **Verify**: Each badge shows correct store name

**What's Being Tested**:
- Multiple store badges display correctly
- Each badge shows correct store information
- No badge conflicts or overlaps

---

### Scenario 4: Retrospective Data (Existing Orders)

**Setup**:
1. Have existing co-seller orders in database
2. Restart the app

**Test Steps**:
1. Open seller's order screen
2. Scroll through existing orders
3. **Expected**: All co-seller orders show store badges
4. **Verify**: Store names load correctly for all orders

**What's Being Tested**:
- Real-time listeners return existing data
- Code-level filtering applies to retrospective data
- Store identification works for old orders

---

### Scenario 5: Revenue Real-Time Updates

**Setup**:
1. Open co-seller store revenue screen
2. Have buyer complete purchase

**Test Steps**:
1. Watch revenue summary
2. **Expected**: Revenue updates instantly
3. **Verify**: New payment appears in revenue breakdown

**What's Being Tested**:
- `startRealtimeRevenueListener()` catches revenue changes
- Revenue summary updates in real-time
- Code-level filtering works for revenue

---

## Debugging Checklist

### If Payments Don't Update in Real-Time

**Check**:
1. ✅ Is `co_seller_store_id` set correctly in payment record?
   - Look in Firestore: `seller_payments` collection
   - Verify payment has `co_seller_store_id` field

2. ✅ Is listener registered?
   - Check logs for: "🔴 Starting real-time payment listener"
   - Check logs for: "🔄 Real-time payment update received"

3. ✅ Is code-level filtering working?
   - Check logs for: "📦 Found X payments for store"
   - Verify store ID matches

4. ✅ Is listener removed on cleanup?
   - Check logs for: "🔴 Real-time listeners removed"

---

### If Store Badge Doesn't Show

**Check**:
1. ✅ Is `coSellerStoreId` set on order?
   - Look in Firestore: `orders` collection
   - Verify order has `co_seller_store_id` field

2. ✅ Is store name loading?
   - Check logs for: "Error loading store name"
   - Verify store exists in `co_seller_stores` collection

3. ✅ Is badge only showing for co-seller orders?
   - Regular orders should NOT show badge
   - Only orders with `coSellerStoreId` should show badge

---

## Log Messages to Look For

### Success Indicators
```
✅ Payments updated in real-time: X
✅ Revenue updated in real-time
✅ Loaded X payments for store: [storeId]
✅ Loaded store revenue: [storeId]
```

### Error Indicators
```
❌ Error listening to payments
❌ Error listening to revenue
❌ Error loading store name
❌ Error updating payments
```

### Debug Indicators
```
🔴 Starting real-time payment listener for store: [storeId]
🔴 Starting real-time revenue listener for store: [storeId]
🔄 Real-time payment update received: X changes
🔄 Real-time revenue update received
📦 Found X payments for store: [storeId]
```

---

## Performance Metrics

### Expected Performance
- Payment appears in real-time: < 2 seconds
- Store name loads: < 1 second
- Badge renders: < 500ms
- Revenue updates: < 2 seconds

### What to Monitor
- Firestore read operations (should be minimal with code-level filtering)
- Memory usage (listeners should be cleaned up on screen exit)
- Battery usage (real-time listeners should not drain battery)

---

## Common Issues & Solutions

### Issue: Payment shows wrong store ID
**Solution**: Check `PaymentRepository.kt` - verify `coSellerStoreId` is set from `order.coSellerStoreId`

### Issue: Store badge shows "Store" instead of actual name
**Solution**: Check Firestore - verify store document exists and has `store_name` field

### Issue: Real-time updates lag
**Solution**: Check network connection - real-time listeners require active connection

### Issue: Badge doesn't appear for co-seller order
**Solution**: Check order document - verify `co_seller_store_id` field is populated

---

## Test Data Setup

### Create Test Co-Seller Store
1. Go to seller dashboard
2. Create new co-seller store
3. Add 2-3 members
4. Upload products

### Create Test Order
1. Switch to buyer account
2. Search for products from co-seller store
3. Add to cart
4. Complete checkout

### Verify in Firestore
1. Check `orders` collection - verify `co_seller_store_id` is set
2. Check `seller_payments` collection - verify payment has `co_seller_store_id`
3. Check `co_seller_stores` collection - verify store document exists

---

## Success Criteria

✅ **Real-Time Updates**:
- Payment appears instantly when order completes
- Revenue updates in real-time
- No lag or delays

✅ **Store Identification**:
- Store badge shows on all co-seller orders
- Badge displays correct store name
- Badge only shows for co-seller orders

✅ **Data Integrity**:
- All payments have correct `co_seller_store_id`
- All co-seller orders have `co_seller_store_id` set
- No data loss or corruption

✅ **Performance**:
- No excessive Firestore queries
- Listeners cleaned up properly
- No memory leaks

---

**Ready to Test!** 🚀
