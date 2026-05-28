# Payment System - Critical Patterns & Best Practices

**Last Updated:** May 20, 2026

---

## 🚨 CRITICAL RULE: Never Use toObject() for SellerPayment

### ❌ WRONG - Will crash:
```kotlin
val payment = doc.toObject(SellerPayment::class.java)  // ❌ CRASH with mixed timestamps!
```

### ✅ CORRECT - Always use parsePayment():
```kotlin
val payment = PaymentRepository.parsePayment(doc)  // ✅ SAFE
```

**Why:** Firestore's reflective deserializer crashes when a field is declared as `Long?` but Firestore has stored a `Timestamp`. The `parsePayment()` function reads each field manually, avoiding this crash entirely.

---

## 🔑 Key Pattern: Safe Deserialization

All SellerPayment objects must be created through `PaymentRepository.parsePayment()`:

```kotlin
// ✅ CORRECT - In any repository
val payments = snapshot.documents.mapNotNull { 
    PaymentRepository.parsePayment(it)  // Single source of truth
}

// ✅ CORRECT - For single documents
val payment = PaymentRepository.parsePayment(doc)
    ?: throw Exception("Payment not found")
```

**Why:** This is the only place where SellerPayment deserialization happens. It handles:
- Mixed timestamp types (Long, Firestore Timestamp, Map, String)
- Safe field casting
- Comprehensive error logging
- Graceful null returns

---

## 📝 Firestore Field Names vs Kotlin Properties

When using `.update()` or `.set()`, **always use Firestore field names** (from @PropertyName annotations):

### ❌ WRONG - Uses Kotlin property names:
```kotlin
db.collection("seller_payments")
    .document(paymentId)
    .update(
        "paymentSplits", updatedSplits,  // ❌ WRONG - Kotlin name
        "updatedAt", System.currentTimeMillis()  // ❌ WRONG - Kotlin name
    )
```

### ✅ CORRECT - Uses Firestore field names:
```kotlin
db.collection("seller_payments")
    .document(paymentId)
    .update(
        "payment_splits", updatedSplits.map { it.toMap() },  // ✅ Correct
        "updated_at", System.currentTimeMillis()  // ✅ Correct
    )
```

**Why:** Firestore doesn't know about Kotlin property names. It only knows about the field names in the document. Using Kotlin names results in silent failures where the update doesn't actually persist.

---

## 🎯 Query Optimization: Use Filters, Not Full Scans

### ❌ WRONG - Full collection scan:
```kotlin
val snapshot = db.collection("seller_payments").get().await()
val payments = snapshot.documents.mapNotNull { parsePayment(it) }
// Reads ALL seller_payments documents!
```

### ✅ CORRECT - Filtered query:
```kotlin
val snapshot = db.collection("seller_payments")
    .whereArrayContains("involved_seller_ids", memberId)  // Filter!
    .get()
    .await()
val payments = snapshot.documents.mapNotNull { parsePayment(it) }
// Reads only payments involving this member
```

**Performance Impact:**
- Full scan: Reads 1000+ documents (expensive!)
- Filtered query: Reads ~10 documents (efficient!)
- **Improvement: ~90% reduction in Firestore reads**

**When to use filters:**
- ✅ Query by seller_id
- ✅ Query by buyer_id
- ✅ Query by order_id
- ✅ Query by co_seller_store_id
- ✅ Query by status
- ✅ Query by date range
- ❌ Never do full collection scans

---

## 🔄 Real-Time Updates: Proper Listener Management

### ✅ CORRECT - In ViewModel:
```kotlin
class SellerPaymentViewModel : ViewModel() {
    private var paymentsListener: ListenerRegistration? = null
    private var statsListener: ListenerRegistration? = null

    fun loadSellerPayments(sellerId: String) {
        viewModelScope.launch {
            // Remove old listener if exists
            paymentsListener?.remove()
            
            // Set up real-time listener
            paymentsListener = paymentRepository.listenToSellerPayments(
                sellerId = sellerId,
                requestingUserId = requestingUserId,
                onUpdate = { payments ->
                    _paymentState.value = PaymentUiState.Success(payments)
                },
                onError = { error ->
                    _paymentState.value = PaymentUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        paymentsListener?.remove()  // ✅ CRITICAL: Clean up listeners
        statsListener?.remove()     // ✅ CRITICAL: Clean up listeners
    }
}
```

**Why:**
- Prevents memory leaks from unclosed listeners
- Ensures listeners are removed when ViewModel is destroyed
- Prevents unnecessary Firestore connections

---

## 🛡️ Access Control: Always Verify User Authorization

### ✅ CORRECT - Check authorization before returning data:
```kotlin
suspend fun getSellerPayments(
    sellerId: String,
    requestingUserId: String
): Result<List<SellerPayment>> {
    return try {
        // ✅ Verify user is authorized
        if (sellerId != requestingUserId) {
            return Result.failure(
                UnauthorizedAccessException("Cannot access other seller's payments")
            )
        }
        
        // Now safe to fetch
        val snapshot = paymentsCollection
            .whereEqualTo("seller_id", sellerId)
            .get()
            .await()
        
        val payments = snapshot.documents.mapNotNull { parsePayment(it) }
        Result.success(payments)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Why:**
- Prevents unauthorized access to payment data
- Ensures sellers can only see their own payments
- Buyers can only see payments they made

---

## 📊 Commission System: Automatic Application

### How Commission Works:

1. **Automatic Application:** Commission is applied to every payment automatically
   - Default rate: 5% (configurable in `commission_settings` document)
   - Applied at payment creation time

2. **Calculation:**
   ```
   Commission = Amount × Commission Rate
   Seller Payout = Amount - Commission
   ```

3. **On Refund:**
   - Full refund: Commission is reversed (seller gets full amount back)
   - Partial refund: Commission is proportionally reversed

### Example:
```
Order Amount: PKR 1000
Commission Rate: 5%
Commission: PKR 50
Seller Payout: PKR 950

If refunded:
Refund to Buyer: PKR 1000
Commission Reversed: PKR 50
Seller Receives: PKR 50 (the reversed commission)
```

---

## 🔍 Timestamp Handling: The anyToMillis() Pattern

### ✅ CORRECT - Handle all timestamp formats:
```kotlin
private fun anyToMillis(value: Any?): Long = when (value) {
    is Long      -> value
    is Timestamp -> value.toDate().time
    is Number    -> value.toLong()
    is String    -> value.toLongOrNull() ?: 0L
    is Map<*, *> -> {
        val s = (value["_seconds"] as? Long) ?: (value["seconds"] as? Long) ?: 0L
        val n = (value["_nanoseconds"] as? Long) ?: (value["nanoseconds"] as? Long) ?: 0L
        s * 1_000 + n / 1_000_000
    }
    null         -> 0L
    else         -> 0L
}
```

**Why:** Firestore can store timestamps in multiple formats:
- Long (epoch milliseconds)
- Firestore Timestamp object
- Map with _seconds and _nanoseconds
- String representation

This function safely converts any format to milliseconds.

---

## 📋 Checklist: Adding New Payment Operations

When adding new payment operations, follow this checklist:

- [ ] Use `PaymentRepository.parsePayment()` for all SellerPayment deserialization
- [ ] Use Firestore field names (snake_case) in `.update()` and `.set()` calls
- [ ] Use filtered queries (`.whereEqualTo()`, `.whereArrayContains()`, etc.)
- [ ] Never do full collection scans
- [ ] Add access control checks (verify user authorization)
- [ ] Use `Result<T>` for error handling
- [ ] Add comprehensive logging with Log.d() and Log.e()
- [ ] For real-time listeners, store `ListenerRegistration` and clean up in `onCleared()`
- [ ] Handle null/empty cases gracefully
- [ ] Test with mixed timestamp formats

---

## 🚀 Common Operations

### Get Seller Payments:
```kotlin
val result = paymentRepository.getSellerPayments(
    sellerId = sellerId,
    requestingUserId = currentUserId
)
```

### Get Buyer Payments:
```kotlin
val result = paymentRepository.getBuyerPayments(buyerId = buyerId)
```

### Get Payment by ID:
```kotlin
val result = paymentRepository.getPaymentById(
    paymentId = paymentId,
    requestingUserId = currentUserId
)
```

### Update Payment Status:
```kotlin
val result = paymentRepository.updatePaymentStatus(
    paymentId = paymentId,
    newStatus = PaymentStatus.COMPLETED,
    transactionId = "txn_123"
)
```

### Process Refund:
```kotlin
val result = paymentRepository.processRefund(
    paymentId = paymentId,
    refundAmount = 1000.0,
    reason = "Customer requested"
)
```

### Set Up Real-Time Listener:
```kotlin
val listener = paymentRepository.listenToSellerPayments(
    sellerId = sellerId,
    requestingUserId = currentUserId,
    onUpdate = { payments ->
        // Update UI with new payments
    },
    onError = { error ->
        // Handle error
    }
)

// Later: clean up
listener.remove()
```

---

## 🐛 Debugging Tips

### If you see: "Failed to convert a value of type com.google.firebase.Timestamp to long"
**Solution:** You're using `toObject()` instead of `parsePayment()`. Replace with:
```kotlin
val payment = PaymentRepository.parsePayment(doc)
```

### If updates aren't persisting:
**Solution:** You're using Kotlin property names instead of Firestore field names. Replace:
```kotlin
// ❌ WRONG
.update("paymentSplits", value, "updatedAt", time)

// ✅ CORRECT
.update("payment_splits", value, "updated_at", time)
```

### If queries are slow:
**Solution:** You're doing a full collection scan. Add a filter:
```kotlin
// ❌ WRONG
db.collection("seller_payments").get()

// ✅ CORRECT
db.collection("seller_payments")
    .whereEqualTo("seller_id", sellerId)
    .get()
```

### If listeners aren't cleaning up:
**Solution:** You're not calling `.remove()` in `onCleared()`. Add:
```kotlin
override fun onCleared() {
    super.onCleared()
    listener?.remove()  // ✅ CRITICAL
}
```

---

## 📚 Related Files

- `PaymentRepository.kt` - Core payment operations
- `PaymentModels.kt` - Data structures and helpers
- `SellerPaymentViewModel.kt` - UI state management
- `CoSellerStorePaymentRepository.kt` - Co-seller payment operations
- `CommissionRepository.kt` - Commission management
- `SellerPaymentsScreen.kt` - Seller payment UI

---

**Last Updated:** May 20, 2026  
**Status:** Production Ready
