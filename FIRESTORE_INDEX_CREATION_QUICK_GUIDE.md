# Firestore Index Creation - Quick Guide

## The Problem
When sellers try to load their payment screen, they get this error:
```
FAILED PRECONDITION: The query requires an index
```

This happens because the query in `PaymentRepository.listenToSellerPayments()` uses two fields:
- `seller_id` (to filter payments for this seller)
- `created_at` (to sort by most recent first)

Firestore requires a composite index for queries that filter AND sort on multiple fields.

---

## The Solution: Create a Composite Index

### Step 1: Open Firebase Console
Go to: https://console.firebase.google.com

### Step 2: Select Your Project
Click on your **Craftoria** project

### Step 3: Navigate to Firestore Indexes
1. In the left sidebar, click **Firestore Database**
2. Click the **Indexes** tab (next to "Data" tab)

### Step 4: Create the Index
1. Click **Create Index** button
2. Fill in the form:
   - **Collection ID:** `seller_payments`
   - **Field 1:** `seller_id` (Ascending)
   - **Field 2:** `created_at` (Descending)
3. Click **Create**

### Step 5: Wait for Index to Build
- The index will show status: "Building..."
- Wait 2-5 minutes for it to complete
- Status will change to "Enabled" when ready

### Step 6: Test the Fix
1. Refresh your app
2. Open seller payment screen
3. The payments should now load without the index error

---

## Visual Reference

### Before Creating Index
```
❌ Error: FAILED PRECONDITION: The query requires an index
```

### After Creating Index
```
✅ Payments loaded successfully
- Order #QCR8NDHN - PKR 1,230.00
- Order #3BD2RW63 - PKR 1,150.00
```

---

## Index Details

| Field | Type | Direction |
|-------|------|-----------|
| seller_id | String | Ascending |
| created_at | Timestamp | Descending |

This index allows the query to:
1. Filter payments by `seller_id` (find this seller's payments)
2. Sort by `created_at` in descending order (most recent first)

---

## Troubleshooting

### Index Still Building?
- Wait a few more minutes
- Refresh Firebase Console to check status
- Indexes typically build within 2-5 minutes

### Still Getting Error After Index Created?
1. Make sure you created the index with the correct fields:
   - Collection: `seller_payments` (not `payments`)
   - Field 1: `seller_id` (Ascending)
   - Field 2: `created_at` (Descending)
2. Try clearing app cache and restarting
3. Check Logcat for any other errors

### Can't Find Indexes Tab?
1. Make sure you're in **Firestore Database** (not Realtime Database)
2. Look for tabs at the top: "Data", "Indexes", "Rules"
3. Click **Indexes** tab

---

## What Happens Next?

Once the index is created:
1. The seller payment screen will load without errors
2. Payments will display in real-time
3. Clicking on a payment will show details
4. The authorization check will verify the seller owns the payment

If you still see "Unauthorized access" error after the index is created, that's a separate issue related to seller ID verification. Check the logs for more details.

