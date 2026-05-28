# 💳 PAYMENT COLLECTION NAMING - QUICK REFERENCE

## 🎯 Collection Names (CANONICAL)

| Collection | Purpose | Status | Usage |
|-----------|---------|--------|-------|
| `payments` | Payment records | ✅ Active | **USE THIS** for all new payment operations |
| `refunds` | Refund requests | ✅ Active | **USE THIS** for refund operations |
| `admin_commissions` | Commission tracking | ✅ Active | **USE THIS** for commission operations |
| `seller_payments` | Legacy payments | ⚠️ Read-only | **DO NOT WRITE** - backward compatibility only |

---

## ✅ WHAT'S BEEN UPDATED

### All Write Operations Now Use "payments"
```kotlin
// ✅ CORRECT - All new code uses this
db.collection("payments").add(paymentData)
db.collection("payments").document(paymentId).update(...)

// ❌ WRONG - Never write to seller_payments
db.collection("seller_payments").add(paymentData)  // DON'T DO THIS
```

### Backward Compatibility (Read-Only)
```kotlin
// ✅ OK - Reading legacy data for migration
val legacyPayments = db.collection("seller_payments")
    .whereEqualTo("co_seller_store_id", storeId)
    .get()
    .await()
```

---

## 📝 FILES UPDATED

### Kotlin (7 files)
- PaymentDataMigration.kt
- RealtimeNameUpdateManager.kt
- RefundStatusMigration.kt
- PaymentIntegrityMonitor.kt
- DashboardRealtimeManager.kt
- DashboardDataHelper.kt
- PaymentRepository.kt

### JavaScript/Node.js (5 files)
- sync-orders-to-payments.mjs
- fix-payment-amounts.mjs
- create-missing-payments.mjs
- check-payment-data.mjs
- check-user-payments.mjs

### Configuration (1 file)
- firestore.rules

---

## 🔍 VERIFICATION

**Grep Search Result:** ✅ VERIFIED
- 0 write operations to "seller_payments"
- All writes now target "payments" collection
- Backward compatibility maintained for reads

---

## 💡 BEST PRACTICES

### When Creating Payments
```kotlin
// ✅ CORRECT
db.collection("payments").add(paymentData)
```

### When Creating Refunds
```kotlin
// ✅ CORRECT
db.collection("refunds").add(refundData)
```

### When Creating Commissions
```kotlin
// ✅ CORRECT
db.collection("admin_commissions").add(commissionData)
```

### When Reading Legacy Data (Migration Only)
```kotlin
// ✅ OK - For backward compatibility during migration
val legacyPayments = db.collection("seller_payments").get()
```

---

## ⚠️ IMPORTANT REMINDERS

1. **Never write to "seller_payments"** - It's read-only for backward compatibility
2. **Always use "payments"** for new payment operations
3. **Use "refunds"** for refund requests (not in payments collection)
4. **Use "admin_commissions"** for commission tracking (not in payments collection)
5. **Link refunds to payments** using `payment_id` field

---

## 🚀 DEPLOYMENT CHECKLIST

- [x] All source code updated
- [x] All migration scripts updated
- [x] Firestore rules updated
- [x] Backward compatibility verified
- [x] No breaking changes
- [x] Ready for production

---

**Last Updated:** May 23, 2026  
**Status:** ✅ COMPLETE
