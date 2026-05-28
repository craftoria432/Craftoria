# Payment Backfill Migration Guide

## Problem Solved

**Root Cause:** Orders completed before the payment system code existed have no payment documents in Firestore. When you mark these orders as complete now, the app can't find payments to update, so Seller Payments screen shows "No Payments Yet" and PKR 0.

**Solution:** This migration script creates payment documents for all existing completed orders.

---

## Quick Start (3 Steps)

### Step 1: Get Your Firebase Service Account Key

1. Go to **Firebase Console** → Your Project
2. Click ⚙️ **Settings** (top-left corner)
3. Go to **Service Accounts** tab
4. Click **Generate New Private Key** (yellow button)
5. A JSON file downloads automatically
6. **Rename it to `firebaseServiceKey.json`** and save in project root

**Important:** Keep this file secret! Don't commit it to Git.

### Step 2: Run the Migration Script

```bash
# From project root directory
node backfill-payments-migration.mjs
```

**First time? Use DRY RUN to preview:**
1. Open `backfill-payments-migration.mjs`
2. Find line: `const DRY_RUN = false;`
3. Change to: `const DRY_RUN = true;`
4. Run: `node backfill-payments-migration.mjs`
5. Review output (won't modify Firestore)
6. Change back to `DRY_RUN = false`
7. Run again to actually create payments

### Step 3: Verify in Seller Payments Screen

1. **Firebase Console Check:**
   - Go to **Firestore** → **payments** collection
   - You should see new documents with your order IDs
   - Check amounts are correct

2. **App Check:**
   - Open Seller Payments screen
   - Payments should appear automatically (2-3 second delay)
   - Amount shows correctly (e.g., PKR 1500)

---

## What The Script Does

✅ **Finds** all completed/delivered orders in Firestore  
✅ **Checks** which orders already have payment documents  
✅ **Creates** missing payment documents in the correct format  
✅ **Handles** both single-seller and multi-seller orders  
✅ **Sets** payment status to "completed" (since orders are complete)  
✅ **Groups** items by seller correctly

---

## Step-by-Step Walkthrough

### Terminal Output Explanation

```
🔧 Payment Backfill Migration Script
────────────────────────────────────────────────────

Step 1: Fetching completed orders...
✅ Found 5 completed/delivered orders

Step 2: Checking for existing payments...
✅ Found 3 existing payment documents

📌 Orders needing payments: 2

Step 3: Creating payment documents...

✅ Prepared: Order order_abc123 → seller_xyz (PKR 1500)
✅ Prepared: Order order_def456 → seller_abc (PKR 2000)

📝 Writing 2 payment documents to Firestore...
✅ Batch write complete!

────────────────────────────────────────────────────
📊 MIGRATION SUMMARY
────────────────────────────────────────────────────
✅ Payments created: 2
❌ Errors: 0
📌 Orders processed: 2
────────────────────────────────────────────────────
```

**What this means:**
- Found 5 old completed orders
- 3 already have payment documents (already fixed)
- 2 orders needed payments
- Both payments created successfully
- 0 errors

---

## Verification Checklist

After running the script:

- [ ] No errors in terminal output
- [ ] "Payments created" > 0
- [ ] Firebase Console shows new documents in `payments` collection
- [ ] New payments have correct `order_id` field
- [ ] New payments have correct `seller_id` field
- [ ] New payments have correct `amount` (order total)
- [ ] New payments have status = "completed"
- [ ] Seller Payments screen shows the payments
- [ ] Dashboard shows correct PKR total

---

## Troubleshooting

### "Service account key not found"
**Error:** `Service account key not found at: firebaseServiceKey.json`

**Fix:**
1. You didn't download the key from Firebase Console
2. You didn't rename it to exactly `firebaseServiceKey.json`
3. It's not in the project root directory

**Solution:** Follow Step 1 again, carefully.

### "Permission denied" or "Unauthorized"
**Error:** `Permission denied on firestore document`

**Fix:**
1. Your Firebase Firestore rules might be too restrictive
2. The service account doesn't have access

**Solution:**
1. Go to **Firebase Console** → **Firestore** → **Rules**
2. Ensure rules allow writes for service accounts (they should by default)
3. Or temporarily make rules more permissive:
   ```
   match /{document=**} {
     allow read, write;
   }
   ```

### "orders" collection doesn't exist
**Error:** No results when querying orders

**Fix:**
1. You might not have any completed orders yet
2. Orders are stored in a different collection name

**Solution:**
1. Go to **Firebase Console** → **Firestore**
2. Check if you see an `orders` collection
3. Look for documents with status "Completed" or "Delivered"
4. If collection has different name, edit the script (line with `db.collection('orders')`)

### Script runs but creates 0 payments
**Error:** "Payments created: 0"

**Reasons:**
1. No completed orders exist
2. All orders already have payments
3. Your orders collection is empty

**What to do:**
1. Check Firebase Console → Firestore → orders collection
2. Manually place a test order and mark it complete
3. Run the script again

---

## How to Fix Future Orders

The code already handles future orders correctly. When users place new orders:

1. Order is created in Firestore
2. `OrderRepository.createOrder()` is called
3. `paymentRepository.processOrderPayments()` is called immediately
4. Payment document is created with correct structure
5. When order marked complete → payment status updated

**No more manual backfilling needed for new orders.**

---

## What If I Only Have ONE Old Order to Fix?

Instead of running the full migration script, you can manually create one payment in Firebase Console:

**Steps:**
1. Go to **Firestore** → **payments** collection
2. Click **Add document**
3. Use this document ID: `payment_` + first 8 chars of order ID (e.g., `payment_abc12345`)
4. Add these fields:

| Field | Value | Type |
|-------|-------|------|
| `id` | (same as document ID) | String |
| `seller_id` | from order doc | String |
| `seller_name` | from order doc | String |
| `order_id` | your order ID | String |
| `buyer_id` | from order doc | String |
| `buyer_name` | from order doc | String |
| `amount` | order total_price | Number |
| `status` | `completed` | String |
| `payment_method` | from order doc | String |
| `items_count` | order item count | Number |
| `created_at` | now (timestamp) | Number (ms) |
| `updated_at` | now (timestamp) | Number (ms) |
| `payment_date` | now (timestamp) | Number (ms) |

5. Click **Save**
6. Check Seller Payments screen (2-3 sec delay)

---

## Code Changes Made

The following collection name fixes were applied to the codebase:

**Before:** `db.collection("seller_payments")`  
**After:** `db.collection("payments")`

**Files Updated:**
- `PaymentRepository.kt` (line 16)
- `OrderRepository.kt` (line 815)
- `RefundRepository.kt` (line 23)
- `RefundProcessor.kt` (line 115)
- `CoSellerStorePaymentRepository.kt` (multiple lines)

**Impact:** All future payment operations now use the correct collection name.

---

## Next Steps

1. **Run the migration** (follow Quick Start above)
2. **Verify payments appear** in Seller Payments screen
3. **Test new orders** to ensure they work correctly
4. **Check Dashboard** shows correct earnings

---

## Need Help?

If payments still don't appear after migration:

1. ✅ Check Firebase Console → Firestore → payments collection
2. ✅ Verify new documents exist with correct order IDs
3. ✅ Check SellerPaymentViewModel logs in Logcat
4. ✅ Verify sellerId matches your user ID

---

## Reference

**Migration Script:** `backfill-payments-migration.mjs`  
**Mode:** Node.js (requires firebase-admin)  
**Batch Size:** All orders processed in single batch write (efficient)  
**Idempotent:** Safe to run multiple times (won't create duplicates)

---

**Last Updated:** May 20, 2026  
**Status:** ✅ Complete - Ready to Deploy
