# Sync Refund Statuses - Quick Start ⚡

## TL;DR

Fix existing refunds showing wrong payment status (e.g., "COMPLETED" instead of "REFUNDED").

---

## Quick Commands

### 1️⃣ Preview Changes (Safe - No Database Modifications)
```bash
node sync-refund-payment-statuses.mjs --dry-run
```

### 2️⃣ Apply Changes (After Reviewing Dry Run)
```bash
node sync-refund-payment-statuses.mjs
```

### 3️⃣ Detailed Output (Optional)
```bash
node sync-refund-payment-statuses.mjs --dry-run --verbose
```

---

## Prerequisites Checklist

- [ ] Node.js installed (v14+)
- [ ] Firebase Admin SDK installed (`npm install firebase-admin`)
- [ ] Service account key file exists:
  - `serviceAccountKey.json` (project root), OR
  - `app/serviceAccountKey.json`, OR
  - `functions/serviceAccountKey.json`

---

## What It Does

| Before | After |
|--------|-------|
| Payment status: `completed` | Payment status: `refunded` |
| Refund status: `completed` | Refund status: `completed` |
| Total Spent: PKR 5000 (includes refunded) | Total Spent: PKR 3000 (excludes refunded) |
| Badge: "Completed" (Green) | Badge: "Refunded" (Purple) |

---

## Status Mapping

```
requested, under_review          → refund_pending
approved_by_seller, approved_by_admin, processing → refund_processing
completed                        → refunded
rejected_by_seller, rejected_by_admin → refund_rejected
```

---

## Expected Output

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 Refund Payment Status Synchronization
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Found 15 refund records
✅ [1/15] Updated payment abc123: completed → refunded
✅ [2/15] Updated payment def456: completed → refund_processing
...

📊 Synchronization Summary
Total refunds:        15
Payments synced:      12
Skipped:              3
Errors:               0

✅ Synchronization complete!
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Could not find serviceAccountKey.json" | Download from Firebase Console → Project Settings → Service Accounts |
| "firebase-admin not found" | Run `npm install firebase-admin` |
| "Permission denied" | Ensure service account has Firestore read/write access |

---

## Verification Steps

After running the script:

1. **Open Payment History Screen**
   - Approved refunds → "Refund Processing" (Blue)
   - Completed refunds → "Refunded" (Purple)
   - Total Spent excludes refunded payments

2. **Open My Orders Screen**
   - Approved refund → "Refund Processing" button
   - Completed refund → "Refund Done" badge

3. **Check Order KNLW1MTK** (if it exists)
   - Should show correct refund status
   - Should be excluded from Total Spent

---

## Safety Notes

✅ **Safe to run multiple times** - Skips already-correct payments
✅ **Dry run available** - Preview before applying
✅ **Batch processing** - Handles large datasets efficiently
✅ **Error handling** - Continues even if individual updates fail

---

## When to Run

✅ **Run once** after deploying refund workflow fixes
✅ **Only if** you have existing refunds created before the fix
❌ **Not needed** for new refunds (they auto-sync)

---

## Full Documentation

For detailed information, see: `SYNC_REFUND_STATUSES_GUIDE.md`

---

**Ready? Start with a dry run:**

```bash
node sync-refund-payment-statuses.mjs --dry-run
```
