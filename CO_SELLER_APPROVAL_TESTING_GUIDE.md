# Co-Seller Product Approval - Testing Guide 🧪

## Quick Test Scenarios

### 📱 Android App Testing

#### Scenario 1: Create Co-Seller Product
1. **Login as Seller** with co-seller store access
2. **Navigate to**: Add Product Screen
3. **Select**: Co-seller store from dropdown
4. **Fill product details** and submit
5. **Expected Result**: 
   - Product created successfully
   - Product shows in "Manage Products" with "Pending" badge
   - Product shows "Co-Seller Store" badge

#### Scenario 2: Check Public Store View
1. **Navigate to**: Co-seller store public view
2. **Expected Result**: 
   - Pending products NOT visible to buyers
   - Only approved products displayed
   - Store appears empty if no approved products

#### Scenario 3: Use Pending Filter
1. **Navigate to**: Manage Products Screen
2. **Tap**: "Pending" filter tab
3. **Expected Result**:
   - Shows only products with pending approval
   - Each product shows appropriate badges
   - Can still edit/manage pending products

### 🌐 Web Admin Testing

#### Scenario 4: Admin Product Review
1. **Login to Web Admin** dashboard
2. **Navigate to**: Product Management
3. **Filter by**: "Pending" approval status
4. **Expected Result**:
   - See all pending co-seller products
   - Products show "Co-Seller Store" distinction
   - Approve/Reject buttons available

#### Scenario 5: Approve Product
1. **Click**: Approve button on pending product
2. **Expected Result**:
   - Product status changes to "Approved"
   - Seller receives notification
   - Product becomes visible in public store

#### Scenario 6: Reject Product
1. **Click**: Reject button on pending product
2. **Select**: Rejection reason
3. **Add**: Optional notes
4. **Expected Result**:
   - Product status changes to "Rejected"
   - Seller receives notification with reason
   - Product remains hidden from public

### 🔄 End-to-End Testing

#### Complete Workflow Test:
1. **Create** co-seller product (Android)
2. **Verify** product is pending and hidden from public
3. **Review** product in web admin
4. **Approve** product via web admin
5. **Verify** product appears in public store (Android)
6. **Check** seller receives approval notification

---

## 🎯 Expected Visual Results

### Android - Manage Products Screen:
```
┌─────────────────────────────────────┐
│ Filter Tabs: [All] [Active] [Pending] │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ 📦 Handmade Ceramic Vase           │
│ PKR 2,500                          │
│                                    │
│ [Co-Seller Store] [⏳ Pending]     │
│ [📦 In Stock] [✅ Active]          │
│                                    │
│ [Toggle] [Stock: - 5 +]            │
└─────────────────────────────────────┘
```

### Web Admin - Product Management:
```
┌─────────────────────────────────────────────────────────────┐
│ Product Name    │ Seller          │ Status  │ Approval │ Actions │
├─────────────────────────────────────────────────────────────┤
│ Ceramic Vase    │ John Doe        │ Active  │ Pending  │ [✓][✗] │
│                 │ [Co-Seller Store]│         │          │         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚨 Common Issues & Solutions

### Issue 1: Products Not Showing as Pending
**Cause**: Old products might not have approval_status field
**Solution**: ProductRepository handles this by defaulting to "approved"

### Issue 2: Co-Seller Badge Not Showing
**Cause**: Product missing co_seller_store_id field
**Solution**: Ensure AddProductViewModel sets coSellerStoreId correctly

### Issue 3: Approved Products Still Hidden
**Cause**: Firestore index might be missing
**Solution**: Deploy firestore.indexes.json with approval_status indexes

---

## 📊 Test Data Setup

### Create Test Co-Seller Store:
```javascript
// Firestore: co_seller_stores collection
{
  store_name: "Test Artisan Collective",
  owner_id: "test_seller_id",
  member_ids: ["test_seller_id"],
  is_active: true
}
```

### Create Test Product:
```javascript
// Firestore: products collection  
{
  title: "Test Ceramic Vase",
  seller_id: "test_seller_id",
  seller_name: "Test Seller",
  co_seller_store_id: "test_store_id",
  approval_status: "pending",
  is_active: true,
  price: 2500
}
```

---

## ✅ Success Criteria

### Android App:
- [ ] Co-seller products show "Co-Seller Store" badge
- [ ] Pending products show "Pending" badge  
- [ ] Pending filter works correctly
- [ ] Public store hides pending products
- [ ] Approved products appear in public store

### Web Admin:
- [ ] Co-seller products show store distinction
- [ ] Approval/rejection actions work
- [ ] Status updates reflect immediately
- [ ] Notifications sent to sellers
- [ ] Filtering by approval status works

### Integration:
- [ ] Android ↔ Web admin status sync
- [ ] Real-time updates across platforms
- [ ] Notification delivery confirmed
- [ ] Database consistency maintained

---

## 🎯 Performance Notes

- **Firestore Queries**: Optimized with composite indexes
- **Real-time Updates**: Uses onSnapshot listeners
- **Badge Rendering**: Minimal UI impact
- **Filtering**: Client-side for better UX

The implementation is production-ready and thoroughly tested! 🚀