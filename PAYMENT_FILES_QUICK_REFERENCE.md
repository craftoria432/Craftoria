# Payment System Files - Quick Reference

## 📋 File Usage at a Glance

### Existing Files (Still Used)

| File | Purpose | Status | Used By |
|------|---------|--------|---------|
| **PaymentRepository.kt** | Core payment processing | ✅ ACTIVE | PaymentSplitProcessor, SellerPaymentViewModel |
| **SellerPaymentViewModel.kt** | Seller payment state | ✅ UPDATED | SellerPaymentsScreen |
| **SellerPaymentsScreen.kt** | Seller payment list UI | ✅ ACTIVE | Navigation |
| **PaymentDetailScreen.kt** | Payment detail UI | ✅ ACTIVE | SellerPaymentsScreen |
| **PaymentDataMigration.kt** | Data migration | ✅ ACTIVE | App initialization |

### Deprecated Files

| File | Purpose | Status | Replacement |
|------|---------|--------|-------------|
| **CoSellerPaymentSplitScreen.kt** | Order payment splits | ⚠️ DEPRECATED | CoSellerStorePaymentScreen |

### New Files

| File | Purpose | Status | Used By |
|------|---------|--------|---------|
| **PaymentSplitProcessor.kt** | Payment split creation | ✅ NEW | Order placement |
| **CoSellerStorePaymentRepository.kt** | Store payment queries | ✅ NEW | CoSellerStorePaymentViewModel |
| **CoSellerStorePaymentViewModel.kt** | Store payment state | ✅ NEW | CoSellerStorePaymentScreen |
| **CoSellerStorePaymentScreen.kt** | Store payment dashboard | ✅ NEW | Navigation |

---

## 🔄 Data Flow Summary

### Original Seller Payment
```
Order → PaymentSplitProcessor → Create payment (no splits)
  → PaymentRepository → Save to Firestore
  → SellerPaymentViewModel → Load payments
  → SellerPaymentsScreen → Display
  → Click → PaymentDetailScreen
```

### Co-Seller Store Payment
```
Order → PaymentSplitProcessor → Create payment (with splits)
  → CoSellerStorePaymentRepository → Save to Firestore
  → CoSellerStorePaymentViewModel → Load payments
  → CoSellerStorePaymentScreen → Display with splits
```

---

## 🎯 When to Use Each File

### PaymentRepository.kt
**When:** Processing original seller payments  
**How:** Called by PaymentSplitProcessor for original seller items  
**Example:**
```kotlin
val payment = SellerPayment(...)
paymentsCollection.add(payment.toMap())
```

### PaymentSplitProcessor.kt
**When:** Order is placed  
**How:** Call instead of PaymentRepository.processOrderPayments()  
**Example:**
```kotlin
val processor = PaymentSplitProcessor(db)
val result = processor.processOrderPaymentsWithSplits(order, items)
```

### SellerPaymentViewModel.kt
**When:** Seller views their payment dashboard  
**How:** Automatically filters out co-seller store payments  
**Example:**
```kotlin
viewModel.loadSellerPayments(sellerId)
val filtered = viewModel.getFilteredPayments()
```

### CoSellerStorePaymentViewModel.kt
**When:** Store owner views store payment dashboard  
**How:** Validates access and loads store payments  
**Example:**
```kotlin
viewModel.loadStorePayments(storeId)
viewModel.loadStoreRevenue(storeId, startDate, endDate)
```

### PaymentDataMigration.kt
**When:** App initialization (once)  
**How:** Migrates existing payments to new format  
**Example:**
```kotlin
PaymentDataMigration.migrateExistingPayments()
```

---

## 🔐 Access Control

### Original Seller
```
Verify: currentUserId == sellerId
Query: seller_id == sellerId AND coSellerStoreId == ""
```

### Co-Seller Store
```
Verify: currentUserId == storeOwnerId OR currentUserId in storeMemberIds
Query: coSellerStoreId == storeId
```

---

## 📊 Payment Data Structure

### Original Seller Payment
```kotlin
SellerPayment(
    seller_id: "seller_1",
    coSellerStoreId: "",           // Empty for original sellers
    amount: 1000,
    paymentSplits: [],             // Empty for original sellers
    involvedSellerIds: ["seller_1"]
)
```

### Co-Seller Store Payment
```kotlin
SellerPayment(
    seller_id: "store_owner_id",
    coSellerStoreId: "store_1",    // Store ID
    amount: 2000,
    paymentSplits: [               // Split breakdown
        PaymentSplit(
            seller_id: "seller_2",
            split_percentage: 0.60,
            split_amount: 1200
        ),
        PaymentSplit(
            seller_id: "seller_3",
            split_percentage: 0.40,
            split_amount: 800
        )
    ],
    involvedSellerIds: ["seller_2", "seller_3"]
)
```

---

## 🧪 Testing Quick Guide

### Test Original Seller Payment
1. Create order with product from original seller
2. Verify payment created with `coSellerStoreId = ""`
3. Verify `paymentSplits = []`
4. Open SellerPaymentsScreen
5. Verify payment appears
6. Click payment → PaymentDetailScreen

### Test Co-Seller Store Payment
1. Create order with products from co-seller store
2. Verify payment created with `coSellerStoreId = "store_id"`
3. Verify `paymentSplits` populated
4. Open CoSellerStorePaymentScreen
5. Verify payment appears with splits
6. Verify revenue summary shows correct data

### Test Access Control
1. Try to access other seller's payments → Should fail
2. Try to access store payments as non-member → Should fail
3. Access own payments → Should succeed
4. Access store payments as member → Should succeed

---

## 🚀 Integration Steps

### Step 1: Update Order Placement
```kotlin
// OLD
val result = paymentRepository.processOrderPayments(order)

// NEW
val processor = PaymentSplitProcessor(db)
val result = processor.processOrderPaymentsWithSplits(order, items)
```

### Step 2: Add Navigation Route
```kotlin
composable("co_seller_store_payment/{storeId}/{storeName}") { backStackEntry ->
    val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
    val storeName = backStackEntry.arguments?.getString("storeName") ?: ""
    
    CoSellerStorePaymentScreen(
        storeId = storeId,
        storeName = storeName,
        onBackClick = { navController.popBackStack() }
    )
}
```

### Step 3: Add Store Dashboard Button
```kotlin
Button(onClick = {
    navController.navigate("co_seller_store_payment/$storeId/$storeName")
}) {
    Text("View Store Payments")
}
```

### Step 4: Run Migration
```kotlin
// In MainActivity or App initialization
viewModelScope.launch {
    val result = PaymentDataMigration.migrateExistingPayments()
    if (result.isSuccess) {
        Log.d(TAG, "Migration complete")
    }
}
```

---

## 🔍 Debugging Tips

### Check Payment Creation
```kotlin
// In Firestore Console
db.collection("seller_payments")
  .where("order_id", "==", "order_123")
  .get()
```

### Check Store Config
```kotlin
// In Firestore Console
db.collection("co_seller_stores")
  .document("store_123")
  .get()
```

### Check Access Control
```kotlin
// In Logcat
Log.d(TAG, "User: $currentUserId, Store members: $storeMemberIds")
```

### Check Split Calculation
```kotlin
// In Logcat
Log.d(TAG, "Total: $totalAmount, Split: ${split.splitPercentage * 100}% = ${split.splitAmount}")
```

---

## 📞 Common Issues

### Issue: Payments not showing splits
**Solution:**
- Check `paymentSplitConfig` is set on store
- Check items are assigned to store (`coSellerStoreId`)
- Check PaymentSplitProcessor is being called

### Issue: Access denied errors
**Solution:**
- Check user is store member
- Check Firestore security rules deployed
- Check `involvedSellerIds` includes user

### Issue: Incorrect split amounts
**Solution:**
- Check split percentages sum to 1.0
- Check calculation: `amount * percentage`
- Check no rounding errors

### Issue: UI not displaying
**Solution:**
- Check CoSellerStorePaymentScreen imported
- Check navigation route added
- Check ViewModel initialized

---

## 📋 File Locations

```
app/src/main/java/com/gcuf/craftoria/
├── data/
│   ├── model/
│   │   ├── PaymentModels.kt (UPDATED)
│   │   └── CoSellerStore.kt (UPDATED)
│   └── repository/
│       ├── PaymentRepository.kt (EXISTING)
│       └── CoSellerStorePaymentRepository.kt (NEW)
├── utils/
│   ├── PaymentDataMigration.kt (EXISTING)
│   └── PaymentSplitProcessor.kt (NEW)
├── viewmodel/
│   ├── SellerPaymentViewModel.kt (UPDATED)
│   └── CoSellerStorePaymentViewModel.kt (NEW)
└── ui/screens/
    ├── seller/
    │   ├── SellerPaymentsScreen.kt (EXISTING)
    │   ├── PaymentDetailScreen.kt (EXISTING)
    │   └── CoSellerPaymentSplitScreen.kt (DEPRECATED)
    └── coseller/
        └── CoSellerStorePaymentScreen.kt (NEW)
```

---

## ✅ Checklist

### Before Integration
- [ ] Review all payment files
- [ ] Understand data flow
- [ ] Understand access control
- [ ] Plan integration timeline

### During Integration
- [ ] Update order placement
- [ ] Add navigation route
- [ ] Add store dashboard button
- [ ] Run PaymentDataMigration
- [ ] Test all scenarios

### After Integration
- [ ] Verify original seller payments
- [ ] Verify co-seller store payments
- [ ] Verify access control
- [ ] Verify UI displays
- [ ] Monitor logs

---

## 🎓 Learning Resources

1. **Architecture:** CO_SELLER_ARCHITECTURE_PRODUCTION_READY.md
2. **Implementation:** CO_SELLER_PAYMENT_SPLIT_IMPLEMENTATION_GUIDE.md
3. **Integration:** CO_SELLER_PAYMENT_SPLIT_INTEGRATION_CHECKLIST.md
4. **Files Guide:** PAYMENT_SYSTEM_FILES_INTEGRATION_GUIDE.md
5. **Diagram:** PAYMENT_SYSTEM_FILES_RELATIONSHIP_DIAGRAM.txt

---

## 🎯 Summary

**Files That Work Together:**
- PaymentSplitProcessor (NEW) → Entry point for payment creation
- PaymentRepository (EXISTING) → Used for original seller payments
- SellerPaymentViewModel (UPDATED) → Filters out co-seller payments
- CoSellerStorePaymentViewModel (NEW) → Manages store payments
- PaymentDataMigration (EXISTING) → Ensures backward compatibility

**Key Principle:**
- Original sellers see only their own payments
- Co-seller stores see all store payments with splits
- Access control enforced at all layers

**Integration Point:**
- Replace `PaymentRepository.processOrderPayments()` with `PaymentSplitProcessor.processOrderPaymentsWithSplits()`

---

**Status:** ✅ COMPLETE QUICK REFERENCE

**Version:** 1.0

**Date:** 2024
