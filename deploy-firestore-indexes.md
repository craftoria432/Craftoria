# 🔥 Firestore Indexes Deployment Guide

## Quick Fix for Payment Screen Error

The "FAILED PRECONDITION" error occurs because Firestore needs composite indexes for complex queries in the CoSellerStorePaymentScreen.

### 📋 Required Steps:

1. **Deploy Indexes** (Run in terminal):
```bash
firebase deploy --only firestore:indexes
```

2. **Alternative: Create via Console**:
   - Go to: https://console.firebase.google.com/v1/r/project/craftoria432/firestore/indexes/create_composite
   - Create these composite indexes:

### 🔍 Required Indexes:

#### Index 1: Store Payments by Date
- **Collection**: `seller_payments`
- **Fields**: 
  - `co_seller_store_id` (Ascending)
  - `created_at` (Descending)

#### Index 2: Store Payments by Status & Date  
- **Collection**: `seller_payments`
- **Fields**:
  - `co_seller_store_id` (Ascending)
  - `status` (Ascending) 
  - `created_at` (Descending)

#### Index 3: Payments by Involved Sellers
- **Collection**: `seller_payments`
- **Fields**:
  - `involved_seller_ids` (Array-contains)
  - `created_at` (Descending)

#### Index 4: Seller Payments by Date
- **Collection**: `seller_payments`
- **Fields**:
  - `seller_id` (Ascending)
  - `created_at` (Descending)

#### Index 5: Order Payments by Date
- **Collection**: `seller_payments`
- **Fields**:
  - `order_id` (Ascending)
  - `created_at` (Descending)

### ⚡ Quick Deploy Command:
```bash
# If you have Firebase CLI installed
firebase deploy --only firestore:indexes

# Wait 2-5 minutes for indexes to build
# Then refresh the Payments tab
```

### 🎯 Expected Result:
- ✅ Payments tab loads without errors
- ✅ Revenue summary cards display correctly
- ✅ Payment list shows with proper filtering
- ✅ All payment queries work smoothly

### 📱 Test After Deployment:
1. Open co-seller store management
2. Navigate to Payments tab
3. Verify revenue cards load
4. Test filter buttons (All, Pending, Completed)
5. Confirm payment list displays properly