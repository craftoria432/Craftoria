# Refund System: Production Edge Cases & Critical Fixes

## Overview
Four critical production issues have been identified and fixed in the refund system before shipping. These are silent failures that would cause misleading errors or data corruption in production.

---

## Fix 1: validateRefundEligibility() — 30-Day Window Edge Case

### Problem
When `deliveredAt` is 0 (order not yet delivered or field missing), the fallback is `payment.paymentDate ?: 0L`. If `paymentDate` is also null, `referenceDate` becomes 0, and `daysSinceReference` becomes an enormous number (~55 years since epoch). Every refund on an undelivered/unfound order would fail with **"refund window expired"** instead of a meaningful error like **"order not yet delivered."**

### Root Cause
```kotlin
val referenceDate = if (deliveredAt > 0) deliveredAt else (payment.paymentDate ?: 0L)
val daysSinceReference = (System.currentTimeMillis() - referenceDate) / (1000 * 60 * 60 * 24)
if (daysSinceReference > REFUND_WINDOW_DAYS) {
    errors.add("Refund window expired (30 days from delivery)")  // ❌ Misleading!
}
```

When `referenceDate == 0L`:
- `daysSinceReference = (now - 0) / (1000 * 60 * 60 * 24) ≈ 55 years`
- Always exceeds 30-day window
- User sees "window expired" instead of "order not delivered"

### Solution
```kotlin
if (referenceDate == 0L) {
    // Order not yet delivered and no payment date found — this is a data integrity issue
    errors.add("Order not yet delivered or payment date missing. Refunds can only be requested after delivery.")
} else {
    val daysSinceReference = (System.currentTimeMillis() - referenceDate) / (1000 * 60 * 60 * 24)
    if (daysSinceReference > REFUND_WINDOW_DAYS) {
        errors.add("Refund window expired (30 days from delivery)")
    }
}
```

### Impact
- ✅ Clear, actionable error messages
- ✅ Prevents false "window expired" rejections
- ✅ Helps identify data integrity issues early

---

## Fix 2: processRefund() — Non-Atomic State Sequence

### Problem
Three separate Firestore writes in sequence:
1. Set refund to PROCESSING
2. Update payment to REFUNDED
3. Update order's refund_status to COMPLETED
4. Set refund to COMPLETED

If the process crashes between any of these, the system ends up in a **partial state**:
- Payment marked REFUNDED but order's refund_status still REQUESTED
- Refund stuck at PROCESSING permanently
- Buyer sees "Completed" badge instead of "Refunded"
- Seller payment records become inconsistent

### Root Cause
```kotlin
// ❌ Three separate writes — not atomic
refundsCollection.document(refundId).update(...).await()  // 1. Refund → PROCESSING
paymentsCollection.document(refund.paymentId).update(...).await()  // 2. Payment → REFUNDED
db.collection("orders").document(refund.orderId).update(...).await()  // 3. Order → refund_status=completed
refundsCollection.document(refundId).update(...).await()  // 4. Refund → COMPLETED
```

If crash occurs between steps 2 and 3:
- Payment is REFUNDED ✓
- Order's refund_status is still REQUESTED ✗
- Badge logic shows "Completed" instead of "Refunded"

### Solution
Use **Firestore batch write** for atomic updates:
```kotlin
val batch = db.batch()

// 1. Update refund to PROCESSING
batch.update(refundsCollection.document(refundId), mapOf(
    "status" to RefundStatus.PROCESSING.toString(),
    "transaction_id" to finalTransactionId,
    "updated_at" to System.currentTimeMillis()
))

// 2. Update payment to REFUNDED
batch.update(paymentsCollection.document(refund.paymentId), mapOf(
    "status" to PaymentStatus.REFUNDED.toString(),
    "refund_amount" to refund.refundAmount,
    "refund_date" to System.currentTimeMillis(),
    "updated_at" to System.currentTimeMillis()
))

// 3. Update order's refund_status to COMPLETED
batch.update(db.collection("orders").document(refund.orderId), mapOf(
    "refund_status" to "completed",
    "updated_at" to System.currentTimeMillis()
))

// 4. Commit all atomically
batch.commit().await()  // ✅ All succeed or all fail together
```

### Impact
- ✅ All three writes succeed together or fail together
- ✅ No partial state corruption
- ✅ Consistent badge display across all screens
- ✅ Seller payment records remain accurate

---

## Fix 3: getFailedRefunds() — Missing Composite Index

### Problem
The query combines `whereEqualTo("status", ...)` with `orderBy("retry_count", ...)`:
```kotlin
refundsCollection
    .whereEqualTo("status", RefundStatus.FAILED.toString())
    .whereGreaterThan("retry_count", 0)
    .orderBy("retry_count", Query.Direction.DESCENDING)
    .get().await()
```

Firestore requires a **composite index** for this query pattern. Without it:
- Query silently fails at runtime
- No compile error (Firestore is dynamic)
- Admin's failed-refund queue always returns an error
- Refunds stuck in FAILED state are never retried

### Root Cause
Firestore requires composite indexes when:
- Combining equality filters (`whereEqualTo`) with ordering on a different field
- Combining inequality filters with ordering

The query has:
- Equality: `status = "failed"`
- Ordering: `retry_count DESC`

These are different fields → composite index required.

### Solution
Add composite index to `firestore.indexes.json`:
```json
{
  "collectionGroup": "refunds",
  "queryScope": "COLLECTION",
  "fields": [
    {
      "fieldPath": "status",
      "order": "ASCENDING"
    },
    {
      "fieldPath": "retry_count",
      "order": "DESCENDING"
    }
  ]
}
```

Also add error logging to catch this issue:
```kotlin
suspend fun getFailedRefunds(): Result<List<RefundRecord>> = try {
    val refunds = refundsCollection
        .whereEqualTo("status", RefundStatus.FAILED.toString())
        .orderBy("retry_count", Query.Direction.DESCENDING)
        .get().await().documents.mapNotNull { deserializeRefundRecord(it) }
    Result.success(refunds)
} catch (e: Exception) {
    Log.e(TAG, "❌ getFailedRefunds() query failed. This likely means the composite index is missing. " +
        "Add this index to firestore.indexes.json: " +
        "Collection=refunds, Fields=[status (Asc), retry_count (Desc)]", e)
    Result.failure(e)
}
```

### Deployment Checklist
Before deploying to production:
1. ✅ Add composite index to `firestore.indexes.json`
2. ✅ Deploy indexes: `firebase deploy --only firestore:indexes`
3. ✅ Wait for index to build (usually 5-10 minutes)
4. ✅ Verify index is active in Firebase Console
5. ✅ Test `getFailedRefunds()` query

### Impact
- ✅ Failed refunds can be queried and retried
- ✅ Admin dashboard shows accurate failed-refund queue
- ✅ Automatic retry system works correctly

---

## Fix 4: RefundRecord.idempotencyKey — Phantom Key Generation

### Problem
```kotlin
@get:PropertyName("idempotency_key")
@set:PropertyName("idempotency_key")
var idempotencyKey: String = UUID.randomUUID().toString()  // ❌ TRAP!
```

If Firestore's reflective deserializer ever touches this class (e.g., in a future code path someone adds), every deserialized `RefundRecord` gets a **fresh random key**, silently discarding the one stored in the database.

### Root Cause
Default values in Kotlin data classes are evaluated at **construction time**, not at field declaration. If a future code path uses `toObject(RefundRecord::class.java)` instead of manual deserialization:
```kotlin
// ❌ This would generate a new UUID for every deserialized record
val refund = doc.toObject(RefundRecord::class.java)
// refund.idempotencyKey is now a fresh UUID, not the one from Firestore!
```

This breaks idempotency guarantees:
- Duplicate refund requests are not detected
- Same refund processed multiple times
- Payment records become corrupted

### Solution
Change default to empty string:
```kotlin
@get:PropertyName("idempotency_key")
@set:PropertyName("idempotency_key")
var idempotencyKey: String = ""  // ✅ Safe default
```

Current code uses manual deserialization (safe):
```kotlin
idempotencyKey = doc.getString("idempotency_key") ?: ""
```

But the default value is a trap for future maintainers. Changing it to `""` prevents accidental phantom key generation.

### Impact
- ✅ Idempotency guarantees remain intact
- ✅ Prevents accidental duplicate refunds
- ✅ Safe for future code changes
- ✅ No behavioral change (already using manual deserialization)

---

## Deployment Checklist

### Before Shipping
- [ ] Review all four fixes
- [ ] Test validateRefundEligibility() with edge cases:
  - [ ] Order not yet delivered (deliveredAt = 0)
  - [ ] Missing payment date (paymentDate = null)
  - [ ] Verify error message is clear
- [ ] Test processRefund() with network interruption:
  - [ ] Simulate crash between batch writes
  - [ ] Verify no partial state corruption
- [ ] Deploy Firestore indexes:
  - [ ] Add composite index to firestore.indexes.json
  - [ ] Run `firebase deploy --only firestore:indexes`
  - [ ] Wait for index to build
  - [ ] Verify in Firebase Console
- [ ] Test getFailedRefunds() query:
  - [ ] Create failed refunds
  - [ ] Query should return results (not error)
  - [ ] Verify retry system works

### Production Monitoring
- [ ] Monitor logs for "getFailedRefunds() query failed" errors
- [ ] Monitor for partial state corruption (payment REFUNDED but order not)
- [ ] Monitor refund success rate
- [ ] Alert on any "refund window expired" errors for undelivered orders

---

## Files Modified
1. `RefundModels.kt` — Fixed idempotencyKey default value
2. `RefundProcessor.kt` — Fixed validateRefundEligibility(), processRefund(), getFailedRefunds()
3. `firestore.indexes.json` — Added composite index for failed refunds query

---

## Summary
These four fixes address silent failures that would cause:
- Misleading error messages (Fix 1)
- Data corruption (Fix 2)
- Failed admin operations (Fix 3)
- Broken idempotency (Fix 4)

All fixes are **backward compatible** and require no database migration.
