# Payment System - Quick Reference Card

**Print this and keep it handy!**

---

## 🚨 THE GOLDEN RULE

### ❌ NEVER DO THIS:
```kotlin
val payment = doc.toObject(SellerPayment::class.java)  // CRASH!
```

### ✅ ALWAYS DO THIS:
```kotlin
val payment = PaymentRepository.parsePayment(doc)  // SAFE
```

---

## 📋 Firestore Field Names Cheat Sheet

| Kotlin | Firestore |
|---|---|
| `sellerId` | `seller_id` |
| `sellerName` | `seller_name` |
| `orderId` | `order_id` |
| `coSellerStoreId` | `co_seller_store_id` |
| `storeName` | `store_name` |
| `buyerId` | `buyer_id` |
| `buyerName` | `buyer_name` |
| `paymentMethod` | `payment_method` |
| `transactionId` | `transaction_id` |
| `paymentDate` | `payment_date` |
| `itemsCount` | `items_count` |
| `itemsDetails` | `items_details` |
| `createdAt` | `created_at` |
| `updatedAt` | `updated_at` |
| `refundAmount` | `refund_amount` |
| `refundReason` | `refund_reason` |
| `refundDate` | `refund_date` |
| `involvedSellerIds` | `involved_seller_ids` |
| `paymentSplits` | `payment_splits` |
| `idempotencyKey` | `idempotency_key` |
| `requestId` | `request_id` |

---

## 🔍 Query Patterns

### ✅ Get Seller Payments:
```kotlin
db.collection("seller_payments")
    .whereEqualTo("seller_id", sellerId)
    .orderBy("created_at", Query.Direction.DESCENDING)
    .get()
```

### ✅ Get Buyer Payments:
```kotlin
db.collection("seller_payments")
    .whereEqualTo("buyer_id", buyerId)
    .orderBy("created_at", Query.Direction.DESCENDING)
    .get()
```

### ✅ Get Order Payments:
```kotlin
db.collection("seller_payments")
    .whereEqualTo("order_id", orderId)
    .get()
```

### ✅ Get Member Payments (Co-Seller):
```kotlin
db.collection("seller_payments")
    .whereArrayContains("involved_seller_ids", memberId)
    .get()
```

### ❌ DON'T DO THIS:
```kotlin
db.collection("seller_payments").get()  // Full scan - EXPENSIVE!
```

---

## 🔄 Real-Time Listeners

### ✅ Set Up Listener:
```kotlin
private var listener: ListenerRegistration? = null

fun setupListener() {
    listener = db.collection("seller_payments")
        .whereEqualTo("seller_id", sellerId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error", error)
                return@addSnapshotListener
            }
            val payments = snapshot?.documents?.mapNotNull { 
                PaymentRepository.parsePayment(it) 
            } ?: emptyList()
            updateUI(payments)
        }
}

override fun onCleared() {
    super.onCleared()
    listener?.remove()  // ✅ CRITICAL!
}
```

---

## 📝 Update Operations

### ✅ Update Payment Status:
```kotlin
db.collection("seller_payments")
    .document(paymentId)
    .update(
        "status", PaymentStatus.COMPLETED.toString(),
        "updated_at", System.currentTimeMillis()
    )
```

### ✅ Update Payment Splits:
```kotlin
db.collection("seller_payments")
    .document(paymentId)
    .update(
        "payment_splits", updatedSplits.map { it.toMap() },
        "updated_at", System.currentTimeMillis()
    )
```

### ❌ DON'T DO THIS:
```kotlin
.update(
    "paymentSplits", value,  // ❌ Wrong field name!
    "updatedAt", time        // ❌ Wrong field name!
)
```

---

## 💰 Commission System

**Automatic 5% commission on every payment:**

```
Order Amount: PKR 1000
Commission: PKR 50 (5%)
Seller Payout: PKR 950
```

**On Refund:**
```
Refund to Buyer: PKR 1000
Commission Reversed: PKR 50
Seller Receives: PKR 50
```

---

## 🛡️ Access Control

### ✅ Always Check Authorization:
```kotlin
if (sellerId != requestingUserId) {
    return Result.failure(
        UnauthorizedAccessException("Unauthorized")
    )
}
```

---

## 🐛 Common Errors & Fixes

| Error | Cause | Fix |
|---|---|---|
| "Failed to convert Timestamp to long" | Using `toObject()` | Use `parsePayment()` |
| Update not persisting | Wrong field names | Use Firestore field names |
| Slow queries | Full collection scan | Add `.whereEqualTo()` filter |
| Memory leak | Listener not removed | Call `.remove()` in `onCleared()` |
| Null payments | Parse error | Check logs, use `parsePayment()` |

---

## 📚 Key Files

| File | Purpose |
|---|---|
| `PaymentRepository.kt` | Core payment operations |
| `PaymentModels.kt` | Data structures |
| `SellerPaymentViewModel.kt` | UI state management |
| `CoSellerStorePaymentRepository.kt` | Co-seller operations |
| `CommissionRepository.kt` | Commission management |

---

## ✅ Pre-Commit Checklist

Before committing payment-related code:

- [ ] Using `parsePayment()` for all SellerPayment deserialization?
- [ ] Using Firestore field names in `.update()` calls?
- [ ] Using filtered queries (not full collection scans)?
- [ ] Cleaning up listeners in `onCleared()`?
- [ ] Added access control checks?
- [ ] Added error logging?
- [ ] Tested with mixed timestamp formats?
- [ ] No compilation errors?

---

## 🚀 Common Operations

```kotlin
// Get seller payments
val result = paymentRepository.getSellerPayments(sellerId, userId)

// Get buyer payments
val result = paymentRepository.getBuyerPayments(buyerId)

// Get payment by ID
val result = paymentRepository.getPaymentById(paymentId, userId)

// Update status
val result = paymentRepository.updatePaymentStatus(paymentId, status)

// Process refund
val result = paymentRepository.processRefund(paymentId, amount, reason)

// Set up real-time listener
val listener = paymentRepository.listenToSellerPayments(
    sellerId, userId, onUpdate, onError
)

// Clean up listener
listener.remove()
```

---

## 📞 Need Help?

1. **Deserialization issues?** → Use `PaymentRepository.parsePayment()`
2. **Update not working?** → Check field names (use Firestore names)
3. **Slow queries?** → Add filters (`.whereEqualTo()`, etc.)
4. **Memory leaks?** → Clean up listeners in `onCleared()`
5. **Access denied?** → Add authorization checks

---

**Last Updated:** May 20, 2026  
**Status:** Production Ready ✅
