# Sync Refund Payment Statuses - Execution Guide

## Overview

This script synchronizes payment statuses with their corresponding refund statuses for all existing refunds in your database. This is a **one-time migration** to fix the issue where approved/completed refunds still show as "COMPLETED" in payment history.

---

## Prerequisites

### 1. Node.js & Firebase Admin SDK

Ensure you have Node.js installed and Firebase Admin SDK available:

```bash
# Check Node.js version (should be 14+)
node --version

# Install Firebase Admin SDK if not already installed
npm install firebase-admin
```

### 2. Service Account Key

The script needs your Firebase service account key file. It will automatically search for:
- `serviceAccountKey.json` (project root)
- `app/serviceAccountKey.json`
- `functions/serviceAccountKey.json`

**If you don't have it yet:**
1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate New Private Key"
3. Save as `serviceAccountKey.json` in your project root

---

## Usage

### Step 1: Dry Run (Recommended First)

**Always run with `--dry-run` first** to preview changes without modifying the database:

```bash
node sync-refund-payment-statuses.mjs --dry-run
```

**Expected Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 Refund Payment Status Synchronization
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔍 DRY RUN MODE - No changes will be made to the database

✅ Firebase initialized using: ./serviceAccountKey.json
📥 Fetching refunds from database...
✅ Found 15 refund records

🔄 Processing refunds...

✅ [1/15] Would update payment abc123: completed → refunded
✅ [2/15] Would update payment def456: completed → refund_processing
⚠️  [3/15] Skipping refund xyz789: Payment already has correct status
...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Synchronization Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Total refunds:        15
Payments synced:      12
Skipped:              3
Errors:               0

By Payment Status:
  refunded             5
  refund_processing    4
  refund_pending       2
  refund_rejected      1

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ℹ️  This was a dry run. No changes were made to the database.
   Run without --dry-run to apply changes.
```

### Step 2: Review the Output

Check the dry run output carefully:
- ✅ **Payments synced**: Number of payments that will be updated
- ⚠️ **Skipped**: Payments already correct or missing
- ❌ **Errors**: Any issues encountered

### Step 3: Apply Changes

If the dry run looks good, run without `--dry-run`:

```bash
node sync-refund-payment-statuses.mjs
```

**Expected Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 Refund Payment Status Synchronization
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Firebase initialized using: ./serviceAccountKey.json
📥 Fetching refunds from database...
✅ Found 15 refund records

🔄 Processing refunds...

✅ [1/15] Updated payment abc123: completed → refunded
✅ [2/15] Updated payment def456: completed → refund_processing
✅ Committed batch of 500 updates
...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Synchronization Summary
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Total refunds:        15
Payments synced:      12
Skipped:              3
Errors:               0

By Payment Status:
  refunded             5
  refund_processing    4
  refund_pending       2
  refund_rejected      1

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Synchronization complete!
```

---

## Command Options

### `--dry-run`
Preview changes without modifying the database. **Always use this first!**

```bash
node sync-refund-payment-statuses.mjs --dry-run
```

### `--verbose`
Show detailed processing information for each refund.

```bash
node sync-refund-payment-statuses.mjs --dry-run --verbose
```

**Verbose Output Example:**
```
[1/15] Processing refund: refund_abc123
   Payment ID: payment_xyz789
   Refund Status: approved_by_seller
   ℹ️  Payment already has correct status: refund_processing
```

### `--help` or `-h`
Show usage information.

```bash
node sync-refund-payment-statuses.mjs --help
```

---

## Status Mapping

The script maps refund statuses to payment statuses as follows:

| Refund Status | Payment Status | Description |
|--------------|----------------|-------------|
| `requested` | `refund_pending` | Buyer submitted refund request |
| `under_review` | `refund_pending` | Admin reviewing request |
| `approved_by_seller` | `refund_processing` | Seller approved refund |
| `approved_by_admin` | `refund_processing` | Admin approved refund |
| `processing` | `refund_processing` | Refund being processed |
| `completed` | `refunded` | Refund completed successfully |
| `rejected_by_seller` | `refund_rejected` | Seller rejected request |
| `rejected_by_admin` | `refund_rejected` | Admin rejected request |
| `failed` | `refund_rejected` | Processing failed |

---

## What Gets Updated

For each refund, the script updates the corresponding payment record:

### Basic Update (All Statuses)
```javascript
{
  status: "refund_processing",  // Updated based on refund status
  updated_at: 1234567890         // Current timestamp
}
```

### Completed Refunds (Additional Fields)
```javascript
{
  status: "refunded",
  refund_amount: 2000,           // From refund record
  refund_reason: "Defective product",
  refund_date: 1234567890,       // From refund.completed_at
  updated_at: 1234567890
}
```

---

## Troubleshooting

### Error: "Could not find serviceAccountKey.json"

**Solution:** Download your service account key from Firebase Console and place it in one of these locations:
- `./serviceAccountKey.json` (project root)
- `./app/serviceAccountKey.json`
- `./functions/serviceAccountKey.json`

### Error: "firebase-admin not found"

**Solution:** Install Firebase Admin SDK:
```bash
npm install firebase-admin
```

### Warning: "Skipping refund: No payment_id"

**Explanation:** Some refund records don't have a `payment_id` field. This is expected for incomplete refund records. They will be skipped.

### Warning: "Skipping refund: Payment not found"

**Explanation:** The payment record referenced by the refund doesn't exist. This could happen if:
- Payment was deleted
- `payment_id` is incorrect
- Payment is in a different collection

**Action:** Review the refund record manually to determine if it should be deleted or fixed.

### Error: "Permission denied"

**Solution:** Ensure your service account key has the necessary permissions:
- Firestore: Read/Write access to `refunds` and `seller_payments` collections

---

## Safety Features

### 1. Batch Processing
- Updates are committed in batches of 500 (Firestore limit)
- Prevents timeout errors for large datasets

### 2. Dry Run Mode
- Preview all changes before applying
- No database modifications in dry run mode

### 3. Validation
- Checks if payment exists before updating
- Skips updates if payment already has correct status
- Validates refund status before mapping

### 4. Error Handling
- Continues processing even if individual updates fail
- Reports all errors in summary

---

## Verification

After running the script, verify the changes:

### 1. Check Payment History Screen
Open the buyer payment history screen and verify:
- ✅ Approved refunds show "Refund Processing" (blue badge)
- ✅ Completed refunds show "Refunded" (purple badge)
- ✅ Pending refunds show "Refund Pending" (orange badge)
- ✅ Rejected refunds show "Refund Rejected" (gray badge)

### 2. Check Total Spent
Verify that refunded payments are excluded from "Total Spent":
- Before: PKR 5000 (includes refunded order)
- After: PKR 3000 (refunded order excluded)

### 3. Check My Orders Screen
Verify that order buttons match payment status:
- Approved refund → "Refund Processing" button (blue)
- Completed refund → "Refund Done" badge (green)

---

## Example: Order KNLW1MTK

**Before Script:**
```javascript
// Payment record
{
  id: "payment_123",
  order_id: "KNLW1MTK",
  status: "completed",  // ❌ Wrong - refund was approved
  amount: 2000
}

// Refund record
{
  id: "refund_456",
  order_id: "KNLW1MTK",
  payment_id: "payment_123",
  status: "approved_by_seller",  // ✅ Correct
  refund_amount: 2000
}
```

**After Script:**
```javascript
// Payment record
{
  id: "payment_123",
  order_id: "KNLW1MTK",
  status: "refund_processing",  // ✅ Fixed - matches refund status
  amount: 2000,
  updated_at: 1234567890
}

// Refund record (unchanged)
{
  id: "refund_456",
  order_id: "KNLW1MTK",
  payment_id: "payment_123",
  status: "approved_by_seller",
  refund_amount: 2000
}
```

**Result:**
- Payment history shows "Refund Processing" (blue badge)
- Total Spent excludes this payment
- My Orders shows "Refund Processing" button

---

## When to Run This Script

### Required:
- ✅ **After deploying the refund workflow fixes** (first time only)
- ✅ **If you have existing refunds** that were created before the fix

### Not Required:
- ❌ For new refunds (they will have correct status automatically)
- ❌ If you have no existing refunds in your database

---

## Performance

- **Small datasets** (<100 refunds): ~5 seconds
- **Medium datasets** (100-1000 refunds): ~30 seconds
- **Large datasets** (1000+ refunds): ~2-5 minutes

The script processes refunds in batches of 500 to optimize performance and avoid Firestore limits.

---

## Summary

1. **Always run with `--dry-run` first** to preview changes
2. **Review the output** to ensure changes are correct
3. **Run without `--dry-run`** to apply changes
4. **Verify in the app** that payment statuses are correct
5. **This is a one-time migration** - new refunds will have correct status automatically

---

## Support

If you encounter any issues:
1. Check the troubleshooting section above
2. Run with `--verbose` flag for detailed output
3. Review the error messages in the summary
4. Ensure your service account key has correct permissions

---

**Ready to sync?** Start with a dry run:

```bash
node sync-refund-payment-statuses.mjs --dry-run
```
