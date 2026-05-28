# Co-Seller Payment Real-Time Updates & Store Identification Fixes

## Issues Identified

### Issue 1: Store Payments Not Updating in Real-Time ❌
**Problem:** When a co-seller completes an order, the payment doesn't appear instantly in the Store Payments screen.

**Root Causes:**
1. **Listener Filter Mismatch** — Listener filters by `co_seller_store_id`, but payment records might not have this field populated correctly when created
2. **Missing Listener Trigger** — When order status changes to "completed", no trigger updates the payment record
3. **Listener Scope Too Narrow** — Only listening to `seller_payments` collection, not considering all payment creation scenarios

### Issue 2: Missing Store Identification in Seller's Order Screen ❌
**Problem:** When a seller views orders from a co-seller store, there's no clear indication which store the product belongs to.

**Root Cause:** Order items don't display the store name/ID, making it unclear which co-seller store the product came from.

---

## Solution 1: Fix Real-Time Payment Updates

### Step 1: Ensure Payment Records Have co_seller_store_id

When an order is completed and payment is created, the `co_seller_store_id` MUST be set:

```kotlin
// In OrderRepository or PaymentRepository when creating payment
val payment = SellerPayment(
    id = paymentId,
    orderId = order.id,
    sellerId = order.sellerId,
    buyerId = order.buyerId,
    amount = order.totalPrice,
    status = "completed",
    coSellerStoreId = order.coSellerStoreId,  // ← CRITICAL: Must be set!
    createdAt = System.currentTimeMillis()
)
```

### Step 2: Add Listener for ALL Payment Changes

Modify `CoSellerStorePaymentViewModel.startRealtimePaymentListener()` to listen to ALL payments and filter in code:

```kotlin
fun startRealtimePaymentListener(storeId: String) {
    Log.d(TAG, "🔴 Starting real-time payment listener for store: $storeId")
    
    paymentListenerRegistration?.remove()
    
    // ✅ FIXED: Listen to ALL seller_payments, filter in code
    paymentListenerRegistration = db.collection("seller_payments")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Error listening to payments", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                Log.d(TAG, "🔄 Real-time payment update received: ${snapshot.documentChanges.size} changes")
                
                // ✅ Filter for this store's payments
                val storePayments = snapshot.documents
                    .mapNotNull { doc ->
                        try {
                            val payment = doc.toObject(SellerPayment::class.java)
                            if (payment?.coSellerStoreId == storeId) payment else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                
                if (storePayments.isNotEmpty()) {
                    Log.d(TAG, "📦 Found ${storePayments.size} payments for store: $storeId")
                    viewModelScope.launch {
                        try {
                            val currentUserId = auth.currentUser?.uid ?: return@launch
                            val result = paymentRepository.loadStorePayments(
                                storeId = storeId,
                                currentUserId = currentUserId,
                                storeMemberIds = emptyList(),
                                storeOwnerId = ""
                            )
                            
                            if (result.isSuccess) {
                                val payments = result.getOrNull() ?: emptyList()
                                _paymentState.value = CoSellerPaymentUiState.Success(payments)
                                Log.d(TAG, "✅ Payments updated in real-time: ${payments.size}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating payments", e)
                        }
                    }
                }
            }
        }
}
```

### Step 3: Add Revenue Listener with Same Pattern

```kotlin
fun startRealtimeRevenueListener(storeId: String, startDate: Long, endDate: Long) {
    Log.d(TAG, "🔴 Starting real-time revenue listener for store: $storeId")
    
    revenueListenerRegistration?.remove()
    
    // ✅ FIXED: Listen to ALL seller_payments, filter in code
    revenueListenerRegistration = db.collection("seller_payments")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Error listening to revenue", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                Log.d(TAG, "🔄 Real-time revenue update received")
                
                // ✅ Filter for this store's payments
                val storePayments = snapshot.documents
                    .mapNotNull { doc ->
                        try {
                            val payment = doc.toObject(SellerPayment::class.java)
                            if (payment?.coSellerStoreId == storeId) payment else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                
                if (storePayments.isNotEmpty()) {
                    viewModelScope.launch {
                        try {
                            val result = paymentRepository.getStoreRevenueSummary(
                                storeId = storeId,
                                startDate = startDate,
                                endDate = endDate
                            )
                            
                            if (result.isSuccess) {
                                val summary = result.getOrNull() ?: throw Exception("No data")
                                _storeRevenueState.value = StoreRevenueUiState.Success(summary)
                                Log.d(TAG, "✅ Revenue updated in real-time")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating revenue", e)
                        }
                    }
                }
            }
        }
}
```

---

## Solution 2: Add Store Identification to Seller's Order Screen

### Step 1: Create Store Badge Component

```kotlin
@Composable
fun CoSellerStoreBadge(
    storeId: String,
    storeName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Primary.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, Primary),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = storeName,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

### Step 2: Update Order Card in SellerOrdersScreen

Add store identification to each order item:

```kotlin
@Composable
fun SellerOrderCard(
    order: Order,
    isHighlighted: Boolean = false,
    onOrderClick: (Order) -> Unit = {},
    onAcceptClick: (Order) -> Unit = {},
    onRejectClick: (Order) -> Unit = {},
    onShippedClick: (Order) -> Unit = {},
    onDeliveredClick: (Order) -> Unit = {},
    onDeleteClick: (Order) -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSelectionMode) { onOrderClick(order) }
            .background(
                if (isHighlighted) Primary.copy(alpha = 0.08f) else Color.Transparent
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(
            width = if (isHighlighted) 1.5.dp else 0.5.dp,
            color = if (isHighlighted) Primary else BorderColor
        )
    ) {
        Column {
            // ── Header with order info and store badge ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundSecondary)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order #${order.id.take(8).uppercase()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        
                        // ✅ NEW: Store badge for co-seller orders
                        if (order.coSellerStoreId.isNotEmpty()) {
                            CoSellerStoreBadge(
                                storeId = order.coSellerStoreId,
                                storeName = order.items.firstOrNull()?.sellerName ?: "Store"
                            )
                        }
                    }
                    
                    Text(
                        text = formatDate(order.getCreatedAtLong()),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Status badge
                OrderStatusBadge(order.status)
            }
            
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            
            // ── Order items with store info ────────────────────────────────
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                order.items.forEach { item ->
                    OrderItemRow(
                        item = item,
                        showStoreInfo = order.coSellerStoreId.isNotEmpty()
                    )
                }
                
                // ── Buyer info ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Buyer", fontSize = 10.sp, color = TextSecondary)
                        RealtimeNameDisplay(
                            userId = order.buyerId,
                            fallbackName = order.buyerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Total", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = "PKR ${String.format("%,.2f", order.totalPrice)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemRow(
    item: OrderItem,
    showStoreInfo: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // ✅ NEW: Show seller/store name for co-seller items
                if (showStoreInfo && item.sellerName.isNotEmpty()) {
                    Text(
                        text = "From: ${item.sellerName}",
                        fontSize = 10.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Text(
                text = "x${item.quantity}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        
        Text(
            text = "PKR ${String.format("%,.2f", item.price)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary
        )
    }
}
```

---

## Implementation Checklist

### Real-Time Payment Updates
- [ ] Verify `co_seller_store_id` is set when payment records are created
- [ ] Update `startRealtimePaymentListener()` to listen to ALL payments and filter in code
- [ ] Update `startRealtimeRevenueListener()` with same pattern
- [ ] Test: Complete order as co-seller → payment appears instantly in Store Payments
- [ ] Test: Multiple co-seller stores → each sees only their payments
- [ ] Verify no excessive Firestore queries

### Store Identification in Orders
- [ ] Create `CoSellerStoreBadge` component
- [ ] Update `SellerOrderCard` to show store badge for co-seller orders
- [ ] Create `OrderItemRow` component with seller name display
- [ ] Update order items display to show "From: [Seller Name]"
- [ ] Test: View co-seller orders → store/seller name clearly visible
- [ ] Test: Regular orders → no store badge shown
- [ ] Verify styling matches design system

---

## Testing Scenarios

### Scenario 1: Real-Time Payment Update
1. Seller A opens Store Payments screen
2. Buyer places order from Store A
3. Seller A completes order
4. ✅ Payment appears instantly (no refresh needed)
5. ✅ Revenue summary updates instantly

### Scenario 2: Multiple Co-Seller Stores
1. Seller B is member of Store A and Store B
2. Opens Store A Payments → sees only Store A payments
3. Opens Store B Payments → sees only Store B payments
4. ✅ No cross-contamination of data

### Scenario 3: Store Identification
1. Seller views orders from co-seller store
2. ✅ Each order shows store badge
3. ✅ Each item shows "From: [Seller Name]"
4. ✅ Regular orders don't show store badge

---

## Files to Modify

1. **CoSellerStorePaymentViewModel.kt**
   - Update `startRealtimePaymentListener()` method
   - Update `startRealtimeRevenueListener()` method

2. **SellerOrdersScreen.kt**
   - Add `CoSellerStoreBadge` component
   - Update `SellerOrderCard` to show store badge
   - Add `OrderItemRow` component with seller name

3. **PaymentRepository.kt** (or where payments are created)
   - Ensure `co_seller_store_id` is set when creating payment records

---

## Expected Results

✅ **Real-Time Updates:** Payments appear instantly when order is completed
✅ **Store Identification:** Clear indication of which store/seller the product is from
✅ **Professional UI:** Store badges and seller names displayed professionally
✅ **No Data Leakage:** Each store only sees their own payments
✅ **Efficient Queries:** Minimal Firestore usage with proper filtering
