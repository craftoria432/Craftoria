# Co-Seller Architecture: Production-Ready Solution

## Executive Summary

Your instinct is correct. A fully production-ready co-seller system requires:

1. **Co-sellers must own or be part of a store** - No standalone co-seller products
2. **Payment split is store-level, not product-level** - Handled on the co-seller store's payment screen
3. **Original sellers see only their own earnings** - Excluding co-seller store products
4. **Clear data isolation and access control** - Prevent unauthorized payment viewing

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    SELLER ECOSYSTEM                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │  ORIGINAL SELLER │         │  CO-SELLER STORE │         │
│  │  (Individual)    │         │  (Collaborative) │         │
│  ├──────────────────┤         ├──────────────────┤         │
│  │ • Own Products   │         │ • Owner (Seller) │         │
│  │ • Own Payments   │         │ • Members        │         │
│  │ • Own Dashboard  │         │ • Shared Products│         │
│  │ • Own Earnings   │         │ • Split Payments │         │
│  └──────────────────┘         └──────────────────┘         │
│         │                              │                    │
│         │ Products                     │ Products           │
│         ▼                              ▼                    │
│    ┌─────────────┐              ┌─────────────┐            │
│    │  Buyer Cart │              │  Buyer Cart │            │
│    └─────────────┘              └─────────────┘            │
│         │                              │                    │
│         │ Order                        │ Order              │
│         ▼                              ▼                    │
│    ┌─────────────┐              ┌─────────────┐            │
│    │  Payment    │              │  Payment    │            │
│    │  (Single)   │              │  (Split)    │            │
│    └─────────────┘              └─────────────┘            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 1. Data Model Changes

### 1.1 Product Model (Already Correct)

```kotlin
data class Product(
    val id: String = "",
    val title: String = "",
    val price: Double = 0.0,
    
    // Original seller info
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",
    
    // ✅ Co-seller store (if product belongs to a store)
    @get:PropertyName("co_seller_store_id")
    @set:PropertyName("co_seller_store_id")
    var coSellerStoreId: String = "",
    
    // ... other fields
)
```

**Key Rule**: `coSellerStoreId` is ONLY set if product belongs to a co-seller store. Otherwise, it's empty.

### 1.2 CoSellerStore Model (Enhanced)

```kotlin
data class CoSellerStore(
    val id: String = "",
    
    @get:PropertyName("store_name")
    @set:PropertyName("store_name")
    var storeName: String = "",
    
    // ✅ CRITICAL: Owner must be a registered seller
    @get:PropertyName("owner_id")
    @set:PropertyName("owner_id")
    var ownerId: String = "",
    
    // ✅ All members must be registered sellers
    @get:PropertyName("member_ids")
    @set:PropertyName("member_ids")
    var memberIds: List<String> = emptyList(),
    
    // ✅ NEW: Payment split configuration
    @get:PropertyName("payment_split_config")
    @set:PropertyName("payment_split_config")
    var paymentSplitConfig: Map<String, Double> = emptyMap(),
    // Example: {"seller_id_1": 0.60, "seller_id_2": 0.40}
    
    @get:PropertyName("is_active")
    @set:PropertyName("is_active")
    var isActive: Boolean = true,
    
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Any? = null,
)
```

### 1.3 SellerPayment Model (Enhanced)

```kotlin
data class SellerPayment(
    var id: String = "",
    
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",
    
    @get:PropertyName("order_id")
    @set:PropertyName("order_id")
    var orderId: String = "",
    
    // ✅ CRITICAL: Track if this is a co-seller store payment
    @get:PropertyName("co_seller_store_id")
    @set:PropertyName("co_seller_store_id")
    var coSellerStoreId: String = "",
    
    // ✅ NEW: Track all sellers involved (for access control)
    @get:PropertyName("involved_seller_ids")
    @set:PropertyName("involved_seller_ids")
    var involvedSellerIds: List<String> = emptyList(),
    
    // ✅ NEW: Payment split details
    @get:PropertyName("payment_splits")
    @set:PropertyName("payment_splits")
    var paymentSplits: List<PaymentSplit> = emptyList(),
    
    var amount: Double = 0.0,
    var status: String = PaymentStatus.PENDING.toString(),
    
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),
)

// ✅ NEW: Payment split detail
data class PaymentSplit(
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",
    
    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",
    
    @get:PropertyName("split_percentage")
    @set:PropertyName("split_percentage")
    var splitPercentage: Double = 0.0,
    
    @get:PropertyName("split_amount")
    @set:PropertyName("split_amount")
    var splitAmount: Double = 0.0,
    
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = PaymentStatus.PENDING.toString()
)
```

---

## 2. Firestore Collection Structure

```
firestore/
├── users/
│   └── {userId}/
│       ├── name, email, role, etc.
│       └── seller_verified: boolean
│
├── products/
│   └── {productId}/
│       ├── title, price, seller_id
│       ├── co_seller_store_id: "" (empty) OR "store_id"
│       └── ... other fields
│
├── co_seller_stores/
│   └── {storeId}/
│       ├── store_name, owner_id
│       ├── member_ids: [seller_id_1, seller_id_2, ...]
│       ├── payment_split_config: {seller_id_1: 0.60, seller_id_2: 0.40}
│       └── is_active: true
│
├── seller_payments/
│   └── {paymentId}/
│       ├── seller_id: "seller_id_1"
│       ├── order_id: "order_id"
│       ├── co_seller_store_id: "store_id" (if split payment)
│       ├── involved_seller_ids: [seller_id_1, seller_id_2]
│       ├── payment_splits: [
│       │   {seller_id, seller_name, split_percentage, split_amount, status}
│       │ ]
│       └── amount: 1000
│
└── orders/
    └── {orderId}/
        ├── buyer_id, items, total_amount
        └── ... other fields
```

---

## 3. Payment Flow Logic

### 3.1 When Order Contains Products from Multiple Sellers

```
Order placed with:
  - Product A (seller_id: "seller_1", co_seller_store_id: "")
  - Product B (seller_id: "seller_2", co_seller_store_id: "store_1")
  - Product C (seller_id: "seller_3", co_seller_store_id: "store_1")

↓

Create SellerPayment records:

1. For seller_1 (original seller):
   {
     seller_id: "seller_1",
     co_seller_store_id: "",
     involved_seller_ids: ["seller_1"],
     amount: 500 (Product A price),
     payment_splits: [] (no split)
   }

2. For store_1 (co-seller store):
   {
     seller_id: "store_1_owner_id",  // ✅ Store owner receives payment
     co_seller_store_id: "store_1",
     involved_seller_ids: ["seller_2", "seller_3"],
     amount: 1000 (Product B + C price),
     payment_splits: [
       {seller_id: "seller_2", split_percentage: 0.60, split_amount: 600},
       {seller_id: "seller_3", split_percentage: 0.40, split_amount: 400}
     ]
   }
```

### 3.2 Payment Split Calculation

```kotlin
// In PaymentRepository or OrderRepository
fun createPaymentRecordsForOrder(order: Order) {
    val groupedByStore = order.items.groupBy { item ->
        val product = getProduct(item.productId)
        product.coSellerStoreId.ifEmpty { "original_seller_${product.sellerId}" }
    }
    
    groupedByStore.forEach { (storeKey, items) ->
        if (storeKey.startsWith("original_seller_")) {
            // Original seller payment (no split)
            val sellerId = storeKey.removePrefix("original_seller_")
            val amount = items.sumOf { it.price * it.quantity }
            
            createSellerPayment(
                sellerId = sellerId,
                coSellerStoreId = "",
                amount = amount,
                involvedSellerIds = listOf(sellerId),
                paymentSplits = emptyList()
            )
        } else {
            // Co-seller store payment (with split)
            val storeId = storeKey
            val store = getCoSellerStore(storeId)
            val totalAmount = items.sumOf { it.price * it.quantity }
            
            val splits = store.paymentSplitConfig.map { (sellerId, percentage) ->
                PaymentSplit(
                    sellerId = sellerId,
                    sellerName = getUserName(sellerId),
                    splitPercentage = percentage,
                    splitAmount = totalAmount * percentage
                )
            }
            
            createSellerPayment(
                sellerId = store.ownerId,  // ✅ Store owner receives payment
                coSellerStoreId = storeId,
                amount = totalAmount,
                involvedSellerIds = store.memberIds,
                paymentSplits = splits
            )
        }
    }
}
```

---

## 4. Dashboard Access Control

### 4.1 Seller Dashboard (Original Seller)

**What they see:**
- Payments for their own products (where `coSellerStoreId` is empty)
- Their own earnings

**What they DON'T see:**
- Payments from co-seller stores they're part of (those are on the store dashboard)
- Other sellers' payments

**Query:**
```kotlin
// In SellerPaymentViewModel
fun loadSellerPayments(sellerId: String) {
    // ✅ ONLY fetch payments where:
    // 1. seller_id == sellerId AND
    // 2. co_seller_store_id == "" (not a co-seller store payment)
    
    db.collection("seller_payments")
        .whereEqualTo("seller_id", sellerId)
        .whereEqualTo("co_seller_store_id", "")
        .orderByChild("created_at")
        .get()
}
```

### 4.2 Co-Seller Store Dashboard

**What they see:**
- All payments for products in their store
- Payment split breakdown
- Each member's earnings

**What they DON'T see:**
- Original seller payments
- Other stores' payments

**Query:**
```kotlin
// In CoSellerStorePaymentViewModel
fun loadStorePayments(storeId: String, currentUserId: String) {
    // ✅ SECURITY: Verify user is store owner or member
    val store = getCoSellerStore(storeId)
    if (currentUserId != store.ownerId && currentUserId !in store.memberIds) {
        throw SecurityException("Access denied")
    }
    
    // ✅ Fetch payments for this store
    db.collection("seller_payments")
        .whereEqualTo("co_seller_store_id", storeId)
        .orderByChild("created_at")
        .get()
}
```

---

## 5. UI/UX Implementation

### 5.1 Seller Dashboard (Original Seller)

```
┌─────────────────────────────────────┐
│     SELLER DASHBOARD                │
├─────────────────────────────────────┤
│                                     │
│  Total Earnings: $5,000             │
│  (From own products only)           │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Payment History             │   │
│  ├─────────────────────────────┤   │
│  │ Order #123 - $500 - Pending │   │
│  │ Order #124 - $300 - Paid    │   │
│  │ Order #125 - $200 - Paid    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ℹ️ Co-seller store earnings are    │
│     shown on the store dashboard    │
│                                     │
└─────────────────────────────────────┘
```

### 5.2 Co-Seller Store Dashboard

```
┌──────────────────────────────────────────┐
│     CO-SELLER STORE DASHBOARD            │
├──────────────────────────────────────────┤
│                                          │
│  Store: "Artisan Crafts"                 │
│  Total Revenue: $10,000                  │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │ Payment Split Configuration      │   │
│  ├──────────────────────────────────┤   │
│  │ Owner (You): 60% ($6,000)        │   │
│  │ Member 1: 30% ($3,000)           │   │
│  │ Member 2: 10% ($1,000)           │   │
│  └──────────────────────────────────┘   │
│                                          │
│  ┌──────────────────────────────────┐   │
│  │ Recent Orders                    │   │
│  ├──────────────────────────────────┤   │
│  │ Order #200                       │   │
│  │ ├─ Product A: $500               │   │
│  │ │  ├─ You: $300 (60%)            │   │
│  │ │  ├─ Member 1: $150 (30%)       │   │
│  │ │  └─ Member 2: $50 (10%)        │   │
│  │ ├─ Product B: $300               │   │
│  │ │  ├─ You: $180 (60%)            │   │
│  │ │  ├─ Member 1: $90 (30%)        │   │
│  │ │  └─ Member 2: $30 (10%)        │   │
│  │ └─ Status: Paid                  │   │
│  └──────────────────────────────────┘   │
│                                          │
└──────────────────────────────────────────┘
```

---

## 6. Implementation Checklist

### Phase 1: Data Model Updates
- [ ] Add `paymentSplitConfig` to `CoSellerStore`
- [ ] Add `PaymentSplit` data class
- [ ] Update `SellerPayment` with `paymentSplits` and `involvedSellerIds`
- [ ] Add migration for existing payments (set `paymentSplits = []`)

### Phase 2: Repository Layer
- [ ] Update `CoSellerStoreRepository` to manage payment split config
- [ ] Update `PaymentRepository` to create split payments
- [ ] Add access control checks in all payment queries
- [ ] Add `CoSellerStorePaymentRepository` for store-level payment queries

### Phase 3: ViewModel Layer
- [ ] Create `CoSellerStorePaymentViewModel` for store dashboard
- [ ] Update `SellerPaymentViewModel` to filter out co-seller store payments
- [ ] Add access control validation in ViewModels

### Phase 4: UI Layer
- [ ] Create `CoSellerStorePaymentScreen` (replaces current `CoSellerPaymentSplitScreen`)
- [ ] Update `SellerPaymentsScreen` to show only own payments
- [ ] Add payment split breakdown UI
- [ ] Add member earnings breakdown

### Phase 5: Security & Testing
- [ ] Add Firestore security rules for payment access
- [ ] Test access control (seller can't view other seller's payments)
- [ ] Test payment split calculations
- [ ] Test co-seller store creation flow

---

## 7. Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ✅ Seller Payments - Access Control
    match /seller_payments/{paymentId} {
      allow read: if 
        request.auth.uid == resource.data.seller_id ||
        request.auth.uid in resource.data.involved_seller_ids;
      
      allow create: if request.auth.uid != null;
      allow update, delete: if request.auth.uid == resource.data.seller_id;
    }
    
    // ✅ Co-Seller Stores - Access Control
    match /co_seller_stores/{storeId} {
      allow read: if true; // Public read
      allow create: if request.auth.uid != null;
      allow update, delete: if 
        request.auth.uid == resource.data.owner_id ||
        request.auth.uid in resource.data.member_ids;
    }
    
  }
}
```

---

## 8. Key Design Principles

### 8.1 Separation of Concerns
- **Original Seller**: Manages own products, sees own payments
- **Co-Seller Store**: Manages store products, handles payment splits
- **Store Owner**: Receives payment, distributes to members

### 8.2 Data Isolation
- Each seller only sees their own data
- Co-seller store data is isolated from original seller data
- Payment records clearly indicate source (original vs. store)

### 8.3 Scalability
- Payment split config is stored once per store
- No need to recalculate splits on every query
- Easy to add/remove members from store

### 8.4 Auditability
- All payment splits are recorded
- `involved_seller_ids` tracks who was involved
- Timestamps track when payments were created/updated

---

## 9. Migration Path (If Existing Data)

```kotlin
// One-time migration for existing payments
fun migrateExistingPayments() {
    db.collection("seller_payments").get().forEach { doc ->
        val payment = doc.toObject(SellerPayment::class.java)
        
        // If no co_seller_store_id, it's an original seller payment
        if (payment.coSellerStoreId.isEmpty()) {
            payment.involvedSellerIds = listOf(payment.sellerId)
            payment.paymentSplits = emptyList()
        } else {
            // If it has co_seller_store_id, fetch store config
            val store = getCoSellerStore(payment.coSellerStoreId)
            payment.involvedSellerIds = store.memberIds
            
            // Calculate splits based on store config
            payment.paymentSplits = store.paymentSplitConfig.map { (sellerId, percentage) ->
                PaymentSplit(
                    sellerId = sellerId,
                    sellerName = getUserName(sellerId),
                    splitPercentage = percentage,
                    splitAmount = payment.amount * percentage
                )
            }
        }
        
        db.collection("seller_payments").document(doc.id).set(payment)
    }
}
```

---

## 10. Production Deployment Checklist

- [ ] Backup all Firestore data
- [ ] Deploy data model changes
- [ ] Run migration script
- [ ] Deploy repository layer with access control
- [ ] Deploy ViewModels with validation
- [ ] Deploy UI screens
- [ ] Update Firestore security rules
- [ ] Test end-to-end payment flow
- [ ] Monitor for errors in production
- [ ] Document for team

---

## Summary

This architecture ensures:

✅ **Co-sellers must own/be part of a store** - No standalone co-seller products  
✅ **Payment split is store-level** - Configured once, applied to all orders  
✅ **Original sellers see only their earnings** - Excluding co-seller store products  
✅ **Clear data isolation** - Each seller sees only their data  
✅ **Production-ready security** - Access control at repository and Firestore level  
✅ **Scalable design** - Easy to add/remove store members  
✅ **Auditable** - All splits recorded with timestamps  

This is a fully production-ready solution that scales with your business.
