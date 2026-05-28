# Seller Payment History - Index Fix 🔧

## PROBLEM IDENTIFIED
When opening **Seller Payment History** screen, Firebase throws:
```
FAILED_PRECONDITION: The query requires an index
```

## ROOT CAUSE
The query in `PaymentRepository.listenToSellerPayments()` (line 295-296) uses:
```kotlin
paymentsCollection
    .whereEqualTo("seller_id", sellerId)
    .orderBy("created_at", Query.Direction.DESCENDING)
```

This composite query (filter + sort) requires a Firestore composite index that hasn't been created yet.

---

## SOLUTION

### Create Composite Index in Firestore

Follow these steps to create the missing index:

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Select your project**: "craftoria432"
3. **Navigate to Firestore Database** → **Indexes** tab
4. **Click "Create Index"** and add:

   | Field | Order |
   |-------|-------|
   | seller_id | Ascending |
   | created_at | Descending |
   | Collection ID | payments |

5. **Wait for index to be built** (usually 2-5 minutes)
6. **Reopen Seller Payment History** - should work immediately

### Alternative: Auto-Create Index

Open the link provided in the error message:
```
https://console.firebase.google.com/v1/r/project/craftoria432/firestore/indexes?create_composite=...
```

This link is auto-populated by Firebase and will create the index in one click.

---

## AFFECTED QUERIES

The same composite index supports these queries:
- ✅ Load seller payments with real-time updates (listenToSellerPayments)
- ✅ Load seller payment stats (listenToSellerPaymentStats)
- ✅ List seller payments with status filter

---

## INDEX CONFIGURATION

**Collection**: `payments`
**Fields**:
- `seller_id` (Ascending) - Filter field
- `created_at` (Descending) - Sort field

**Query Scope**: Collection

---

## VERIFICATION

Once index is created, the screen should:
1. ✅ Load without errors
2. ✅ Display payment list in descending order by date
3. ✅ Show real-time updates when payments are added
4. ✅ Filter by status correctly

---

## NEXT STEPS

1. Create the index in Firebase Console
2. Wait for index to be built
3. Reopen Seller Payment History screen
4. Verify payments display correctly

The error should disappear immediately once the index is ready.
