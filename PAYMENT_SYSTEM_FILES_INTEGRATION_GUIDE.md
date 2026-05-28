# Payment System Files Integration Guide

## 📋 Overview of All Payment-Related Files

This guide explains how all payment-related files work together in the complete payment system.

---

## 🗂️ File Inventory

### Existing Files (Earlier Implementation)

1. **PaymentRepository.kt** - Core payment processing
2. **CoSellerPaymentSplitScreen.kt** - UI for viewing payment splits
3. **PaymentDetailScreen.kt** - UI for payment details
4. **SellerPaymentsScreen.kt** - UI for seller payment list
5. **PaymentDataMigration.kt** - Data migration utility
6. **SellerPaymentViewModel.kt** - State management for seller payments

### New Files (Co-Seller Payment Split Implementation)

1. **CoSellerStorePaymentRepository.kt** - Store-level payment queries
2. **PaymentSplitProcessor.kt** - Payment split creation logic
3. **CoSellerStorePaymentViewModel.kt** - State management for store payments
4. **CoSellerStorePaymentScreen.kt** - UI for store payment dashboard

---

## 🔄 How They Work Together

### Architecture Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    ORDER PLACEMENT                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  CheckoutViewModel / OrderRepository                        │
│         ↓                                                    │
│  PaymentSplitProcessor.processOrderPaymentsWithSplits()    │
│  (NEW - Replaces PaymentRepository.processOrderPayments)   │
│         ↓                                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Group items by store                                 │  │
│  │ • Original seller → PaymentRepository.processOrder   │  │
│  │ • Co-seller store → Create split payments            │  │
│  └──────────────────────────────────────────────────────┘  │
│         ↓                                                    │
│  Save to Firestore                                          │
│         ↓                                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ DASHBOARD ACCESS                                     │  │
│  ├──────────────────────────────────────────────────────┤  │
│  │ Original Seller                                      │  │
│  │ ↓                                                    │  │
│  │ SellerPaymentViewModel                              │  │
│  │ ↓                                                    │  │
│  │ PaymentRepository.getSellerPayments()               │  │
│  │ (Filter: coSellerStoreId == "")                     │  │
│  │ ↓                                                    │  │
│  │ SellerPaymentsScreen                                │  │
│  │ ↓                                                    │  │
│  │ PaymentDetailScreen (on click)                      │  │
│  └──────────────────────────────────────────────────────┘  │
│         ↓                                                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Co-Seller Store Owner                               │  │
│  │ ↓                                                    │  │
│  │ CoSellerStorePaymentViewModel                       │  │
│  │ ↓                                                    │  │
│  │ CoSellerStorePaymentRepository.loadStorePayments()  │  │
│  │ (Filter: coSellerStoreId == "store_id")             │  │
│  │ ↓                                                    │  │
│  │ CoSellerStorePaymentScreen                          │  │
│  │ ↓                                                    │  │
│  │ Shows payment splits                                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📄 Detailed File Usage

### 1. PaymentRepository.kt (EXISTING)

**Purpose:** Core payment processing for original sellers

**Key Methods:**
```kotlin
suspend fun processOrderPayments(order: Order): Result<List<String>>
suspend fun getSellerPayments(sellerId: String, ...): Result<List<SellerPayment>>
suspend fun getPaymentDetail(paymentId: String): Result<SellerPayment>
```

**Usage:**
- Called when order is placed (for original seller items)
- Queries payments for original sellers
- Handles payment status updates

**Integration with New System:**
- Still used for original seller payments
- PaymentSplitProcessor calls it for original seller items
- Filters out co-seller store payments

**Example:**
```kotlin
// In PaymentSplitProcessor
if (storeKey.startsWith("original_seller_")) {
    // Use PaymentRepository for original sellers
    val payment = SellerPayment(...)
    paymentsCollection.add(payment.toMap())
}
```

---

### 2. PaymentSplitProcessor.kt (NEW)

**Purpose:** Orchestrates payment creation with splits

**Key Methods:**
```kotlin
suspend fun processOrderPaymentsWithSplits(
    order: Order,
    items: List<OrderItem>
): Result<List<String>>
```

**Usage:**
- Called instead of PaymentRepository.processOrderPayments()
- Groups items by store
- Creates appropriate payments (with or without splits)
- Replaces the old payment processing flow

**Integration:**
```kotlin
// In CheckoutViewModel or OrderRepository
val processor = PaymentSplitProcessor(db)
val result = processor.processOrderPaymentsWithSplits(order, items)
```

**Flow:**
1. Group items by store
2. For original sellers: Create simple payment
3. For co-seller stores: Create split payment
4. Save all to Firestore

---

### 3. SellerPaymentViewModel.kt (UPDATED)

**Purpose:** State management for original seller payments

**Key Methods:**
```kotlin
fun loadSellerPayments(sellerId: String)
fun filterByStatus(status: String)
fun getFilteredPayments(): List<SellerPayment>
```

**Changes:**
- Added filtering to exclude co-seller store payments
- Original sellers now only see their own product payments

**Usage:**
```kotlin
// In SellerPaymentsScreen
val filteredPayments = allPayments.filter { payment ->
    payment.coSellerStoreId.isEmpty()  // Only own products
}
```

**Integration:**
- Used by SellerPaymentsScreen
- Queries PaymentRepository
- Filters results

---

### 4. SellerPaymentsScreen.kt (EXISTING)

**Purpose:** UI for original seller payment list

**Features:**
- Shows payments for own products only
- Filter by status
- Payment cards with details
- Click to view details

**Integration:**
- Uses SellerPaymentViewModel
- Displays filtered payments
- Navigates to PaymentDetailScreen

**Data Flow:**
```
SellerPaymentViewModel
    ↓
PaymentRepository.getSellerPayments()
    ↓
Filter: coSellerStoreId == ""
    ↓
SellerPaymentsScreen displays
```

---

### 5. PaymentDetailScreen.kt (EXISTING)

**Purpose:** UI for viewing single payment details

**Features:**
- Shows payment information
- Shows items in payment
- Shows buyer details
- Shows payment status

**Integration:**
- Called from SellerPaymentsScreen
- Uses SellerPaymentViewModel
- Displays payment details

**Note:** For co-seller store payments, use CoSellerStorePaymentScreen instead

---

### 6. CoSellerPaymentSplitScreen.kt (EXISTING)

**Purpose:** UI for viewing payment splits (OLD - DEPRECATED)

**Status:** ⚠️ DEPRECATED - Use CoSellerStorePaymentScreen instead

**Why Deprecated:**
- Only showed splits for a single order
- Not a full dashboard
- Limited functionality

**Replacement:**
- CoSellerStorePaymentScreen (NEW)
- Shows all store payments
- Shows revenue summary
- Shows member earnings

**Migration:**
```kotlin
// OLD (Don't use)
CoSellerPaymentSplitScreen(orderId, payments)

// NEW (Use instead)
CoSellerStorePaymentScreen(storeId, storeName)
```

---

### 7. CoSellerStorePaymentRepository.kt (NEW)

**Purpose:** Store-level payment queries with access control

**Key Methods:**
```kotlin
suspend fun loadStorePayments(storeId, currentUserId, ...): Result<List<SellerPayment>>
suspend fun getMemberEarningsBreakdown(...): Result<MemberEarningsBreakdown>
suspend fun getStoreRevenueSummary(...): Result<StoreRevenueSummary>
suspend fun updatePaymentSplitStatus(...): Result<Unit>
```

**Usage:**
- Queries payments for a specific store
- Verifies user is store owner or member
- Calculates member earnings
- Calculates store revenue

**Integration:**
```kotlin
// In CoSellerStorePaymentViewModel
val repository = CoSellerStorePaymentRepository(db)
val result = repository.loadStorePayments(storeId, currentUserId, ...)
```

---

### 8. CoSellerStorePaymentViewModel.kt (NEW)

**Purpose:** State management for store payments

**Key Methods:**
```kotlin
fun loadStorePayments(storeId: String)
fun loadPaymentDetail(paymentId: String)
fun loadMemberEarnings(storeId, memberId, startDate, endDate)
fun loadStoreRevenue(storeId, startDate, endDate)
fun filterByStatus(status: String)
```

**Usage:**
- Manages store payment state
- Validates access control
- Handles errors gracefully

**Integration:**
```kotlin
// In CoSellerStorePaymentScreen
val viewModel: CoSellerStorePaymentViewModel = viewModel()
viewModel.loadStorePayments(storeId)
```

---

### 9. CoSellerStorePaymentScreen.kt (NEW)

**Purpose:** Dashboard for store payments with splits

**Features:**
- Revenue summary cards
- Payment list with splits
- Filter by status
- Payment split breakdown
- Member earnings display

**Integration:**
- Uses CoSellerStorePaymentViewModel
- Queries CoSellerStorePaymentRepository
- Shows store payments with splits

**Data Flow:**
```
CoSellerStorePaymentViewModel
    ↓
CoSellerStorePaymentRepository.loadStorePayments()
    ↓
Filter: coSellerStoreId == "store_id"
    ↓
CoSellerStorePaymentScreen displays
```

---

### 10. PaymentDataMigration.kt (EXISTING)

**Purpose:** Migrate existing payments to new format

**Key Methods:**
```kotlin
suspend fun migrateExistingPayments(): Result<Int>
suspend fun getUnmigratedPaymentCount(): Result<Int>
suspend fun migrateSpecificPayment(paymentId: String): Result<Unit>
```

**Usage:**
- Called once during app initialization
- Adds `involved_seller_ids` to existing payments
- Ensures backward compatibility

**Integration:**
```kotlin
// In MainActivity or App initialization
viewModelScope.launch {
    val result = PaymentDataMigration.migrateExistingPayments()
    if (result.isSuccess) {
        Log.d(TAG, "Migration complete")
    }
}
```

**What It Does:**
1. Finds payments without `involved_seller_ids`
2. Gets all sellers involved in each order
3. Updates payments with seller list
4. Logs migration progress

---

## 🔗 Integration Points

### Point 1: Order Placement

**OLD:**
```kotlin
val result = paymentRepository.processOrderPayments(order)
```

**NEW:**
```kotlin
val processor = PaymentSplitProcessor(db)
val result = processor.processOrderPaymentsWithSplits(order, items)
```

**Location:** CheckoutViewModel or OrderRepository

---

### Point 2: Seller Dashboard

**Existing (Still Used):**
```kotlin
// SellerPaymentsScreen
val viewModel: SellerPaymentViewModel = viewModel()
viewModel.loadSellerPayments(sellerId)
```

**New (Added):**
```kotlin
// CoSellerStorePaymentScreen
val viewModel: CoSellerStorePaymentViewModel = viewModel()
viewModel.loadStorePayments(storeId)
```

---

### Point 3: Navigation

**Existing:**
```kotlin
composable("seller_payments/{sellerId}") { ... }
composable("payment_detail/{paymentId}") { ... }
```

**New (Add):**
```kotlin
composable("co_seller_store_payment/{storeId}/{storeName}") { ... }
```

---

### Point 4: Store Dashboard

**Add Button:**
```kotlin
// In ManageCoSellerStoreScreen
Button(onClick = {
    navController.navigate("co_seller_store_payment/$storeId/$storeName")
}) {
    Text("View Store Payments")
}
```

---

## 📊 Data Flow Comparison

### Original Seller Payment Flow

```
Order Placed
    ↓
PaymentSplitProcessor (groups items)
    ↓
Original seller items detected
    ↓
Create SellerPayment (no splits)
    ↓
Save to Firestore
    ↓
SellerPaymentViewModel.loadSellerPayments()
    ↓
PaymentRepository.getSellerPayments()
    ↓
Filter: coSellerStoreId == ""
    ↓
SellerPaymentsScreen displays
    ↓
Click → PaymentDetailScreen
```

### Co-Seller Store Payment Flow

```
Order Placed
    ↓
PaymentSplitProcessor (groups items)
    ↓
Co-seller store items detected
    ↓
Fetch store config
    ↓
Create SellerPayment (with splits)
    ↓
Save to Firestore
    ↓
CoSellerStorePaymentViewModel.loadStorePayments()
    ↓
CoSellerStorePaymentRepository.loadStorePayments()
    ↓
Filter: coSellerStoreId == "store_id"
    ↓
CoSellerStorePaymentScreen displays
    ↓
Shows revenue + splits
```

---

## 🔐 Access Control

### Original Seller Access

```
SellerPaymentViewModel
    ↓
Verify: currentUserId == sellerId
    ↓
PaymentRepository.getSellerPayments()
    ↓
Query: seller_id == sellerId AND coSellerStoreId == ""
    ↓
Return filtered payments
```

### Co-Seller Store Access

```
CoSellerStorePaymentViewModel
    ↓
Verify: currentUserId == storeOwnerId OR currentUserId in storeMemberIds
    ↓
CoSellerStorePaymentRepository.loadStorePayments()
    ↓
Query: coSellerStoreId == storeId
    ↓
Return store payments
```

---

## 🧪 Testing Scenarios

### Scenario 1: Original Seller Payment

```
1. Order with Product A (seller_1, no store)
2. PaymentSplitProcessor creates payment
3. SellerPaymentViewModel loads payments
4. SellerPaymentsScreen displays
5. Click → PaymentDetailScreen shows details
```

### Scenario 2: Co-Seller Store Payment

```
1. Order with Product B, C (seller_2, seller_3, store_1)
2. PaymentSplitProcessor creates split payment
3. CoSellerStorePaymentViewModel loads payments
4. CoSellerStorePaymentScreen displays with splits
5. Shows revenue summary and member earnings
```

### Scenario 3: Mixed Order

```
1. Order with Product A (seller_1) + Product B (seller_2, store_1)
2. PaymentSplitProcessor creates 2 payments
3. Seller_1 sees payment in SellerPaymentsScreen
4. Store_1 owner sees payment in CoSellerStorePaymentScreen
5. Both see correct amounts
```

---

## 📋 Migration Checklist

### Before Integration

- [ ] Review PaymentRepository.kt
- [ ] Review PaymentSplitProcessor.kt
- [ ] Review SellerPaymentViewModel.kt
- [ ] Review CoSellerStorePaymentViewModel.kt
- [ ] Understand data flow

### During Integration

- [ ] Update order placement to use PaymentSplitProcessor
- [ ] Update navigation to include new route
- [ ] Update store dashboard with button
- [ ] Run PaymentDataMigration
- [ ] Test all scenarios

### After Integration

- [ ] Verify original seller payments work
- [ ] Verify co-seller store payments work
- [ ] Verify access control works
- [ ] Verify UI displays correctly
- [ ] Monitor logs for errors

---

## 🎯 Summary

### Files That Still Work

- ✅ PaymentRepository.kt - Used for original seller payments
- ✅ SellerPaymentViewModel.kt - Updated with filtering
- ✅ SellerPaymentsScreen.kt - Shows original seller payments
- ✅ PaymentDetailScreen.kt - Shows payment details
- ✅ PaymentDataMigration.kt - Migrates existing data

### Files That Are Deprecated

- ⚠️ CoSellerPaymentSplitScreen.kt - Use CoSellerStorePaymentScreen instead

### New Files

- ✅ PaymentSplitProcessor.kt - New payment processing
- ✅ CoSellerStorePaymentRepository.kt - Store payment queries
- ✅ CoSellerStorePaymentViewModel.kt - Store payment state
- ✅ CoSellerStorePaymentScreen.kt - Store payment dashboard

### Integration Points

1. **Order Placement** - Use PaymentSplitProcessor
2. **Seller Dashboard** - Use SellerPaymentViewModel (existing)
3. **Store Dashboard** - Use CoSellerStorePaymentViewModel (new)
4. **Navigation** - Add new route for store payments
5. **Data Migration** - Run PaymentDataMigration once

---

## 📞 Quick Reference

**For Original Seller Payments:**
- ViewModel: SellerPaymentViewModel
- Repository: PaymentRepository
- Screen: SellerPaymentsScreen
- Detail: PaymentDetailScreen

**For Co-Seller Store Payments:**
- ViewModel: CoSellerStorePaymentViewModel
- Repository: CoSellerStorePaymentRepository
- Screen: CoSellerStorePaymentScreen
- Processor: PaymentSplitProcessor

**For Data Migration:**
- Utility: PaymentDataMigration
- Call once during app initialization

---

**Status:** ✅ COMPLETE INTEGRATION GUIDE

**Version:** 1.0

**Date:** 2024
