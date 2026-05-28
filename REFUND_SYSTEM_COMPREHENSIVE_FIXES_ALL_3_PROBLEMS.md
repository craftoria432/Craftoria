# 🔧 Refund System - Comprehensive Fixes (All 3 Problems)

## 📋 Problems Identified

### Problem 1: Buyer/Requested Columns Empty in Web Dashboard
**Root Cause:** Two different code paths create refunds:
1. `RefundProcessor.initiateRefund()` - uses `toMapEnhanced()` ✅ (already fixed)
2. `RefundRepository.createRefundRequest()` - uses `RefundRequest.toMap()` ❌ (missing fields)

**Impact:** Web dashboard expects `buyer_name`, `seller_name`, `requested_at` but they're missing from repository path.

---

### Problem 2: Rejection Badge/Notification Missing
**Root Cause:** 
- `MyOrdersScreen` OrderCard only checks `hasExistingRefund` (boolean)
- Doesn't check refund **status** (REJECTED, APPROVED, etc.)
- `RefundNotificationService` missing Android implementation

**Impact:** Buyer doesn't see rejection badge or get notification when seller rejects refund.

---

### Problem 3: Payment Screens Don't Show Refund Status
**Root Cause:**
- `SellerPaymentsScreen` - already handles "refunded" ✅
- `BuyerPaymentHistoryScreen` - missing "Refunded" badge ❌
- `CoSellerStorePaymentScreen` - missing refund indicator ❌

**Impact:** Buyers and co-sellers don't see refund status in payment history.

---

## 🔧 PATCH 1: RefundProcessor.kt - Unified toMap()

### File: `app/src/main/java/com/gcuf/craftoria/utils/RefundProcessor.kt`

**Replace both `toMap()` and `toMapEnhanced()` with single unified method:**

```kotlin
// ✅ UNIFIED: Single toMap() with optional parameters for web dashboard compatibility
private fun RefundRecord.toMap(buyerName: String = "", sellerName: String = ""): Map<String, Any> = mapOf(
    "id"              to id,
    "payment_id"      to paymentId,
    "order_id"        to orderId,
    "seller_id"       to sellerId,
    "seller_name"     to sellerName,      // ✅ web dashboard needs this
    "buyer_id"        to buyerId,
    "buyer_name"      to buyerName,       // ✅ web dashboard "Buyer" column
    "refund_amount"   to refundAmount,
    "original_amount" to originalAmount,
    "reason"          to reason,
    "description"     to description,
    "requested_by"    to requestedBy,
    "approved_by"     to (approvedBy ?: ""),
    "status"          to status,
    "transaction_id"  to transactionId,
    "payment_method"  to paymentMethod,
    "refund_splits"   to refundSplits.map { it.toRefundSplitMap() },
    "retry_count"     to retryCount,
    "max_retries"     to maxRetries,
    "last_retry_at"   to (lastRetryAt ?: 0L),
    "error_message"   to errorMessage,
    "created_at"      to createdAt,
    "requested_at"    to createdAt,       // ✅ web dashboard "Requested" column
    "approved_at"     to (approvedAt ?: 0L),
    "processed_at"    to (processedAt ?: 0L),
    "updated_at"      to updatedAt,
    "idempotency_key" to idempotencyKey
)

private fun RefundSplit.toRefundSplitMap(): Map<String, Any> = mapOf(
    "seller_id"     to sellerId,
    "seller_name"   to sellerName,
    "refund_amount" to refundAmount,
    "status"        to status
)
```

**Update `initiateRefund()` to fetch both buyer and seller names:**

```kotlin
// Inside initiateRefund(), replace refundMap creation:

// ✅ Fetch buyer name
val buyerDoc  = db.collection("users").document(payment.buyerId).get().await()
val buyerName = buyerDoc.getString("name") ?: buyerDoc.getString("full_name") ?: "Unknown Buyer"

// ✅ Get seller name from payment
val sellerName = payment.sellerName

// ✅ Use unified toMap() with both names
val refundMap = refund.toMap(buyerName = buyerName, sellerName = sellerName)
val refundDoc = refundsCollection.add(refundMap).await()
```

---

## 🔧 PATCH 2: RefundModels.kt - Fix RefundRequest.toMap()

### File: `app/src/main/java/com/gcuf/craftoria/data/model/RefundModels.kt`

**Replace `RefundRequest.toMap()` with:**

```kotlin
fun RefundRequest.toMap(): Map<String, Any> = mapOf(
    "id"              to id,
    "order_id"        to orderId,
    "payment_id"      to paymentId,
    "buyer_id"        to buyerId,
    "buyer_name"      to buyerName,          // ✅ was missing
    "seller_id"       to sellerId,
    "seller_name"     to sellerName,         // ✅ was missing
    "refund_type"     to refundType,
    "original_amount" to originalAmount,
    "refund_amount"   to refundAmount,
    "reason"          to reason,
    "reason_details"  to reasonDetails,
    "status"          to status,
    "initiated_by"    to initiatedBy,
    "approved_by"     to approvedBy,
    "approval_notes"  to approvalNotes,
    "payment_method"  to paymentMethod,
    "transaction_id"  to transactionId,
    "gateway_refund_id" to gatewayRefundId,
    "refund_splits"   to refundSplits.map { it.toMap() },
    "retry_count"     to retryCount,
    "last_retry_at"   to (getLastRetryAtLong()),
    "error_message"   to errorMessage,
    "requested_at"    to getRequestedAtLong(),   // ✅ was missing
    "approved_at"     to (getApprovedAtLong()),
    "processed_at"    to (getProcessedAtLong()),
    "completed_at"    to (getCompletedAtLong()),
    "created_at"      to getCreatedAtLong(),
    "updated_at"      to getUpdatedAtLong(),
    "idempotency_key" to idempotencyKey,
    "audit_trail"     to auditTrail.map { it.toMap() }
)
```

---

## 🔧 PATCH 3: MyOrdersScreen.kt - Refund Status Tracking

### File: `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt`

**Step 1: Add OrderRefundState enum (add before OrderCard composable):**

```kotlin
// Represents all possible refund states for a delivered/completed order
private enum class OrderRefundState {
    NONE,           // No refund exists
    REQUESTED,      // Buyer submitted, awaiting seller/admin action
    APPROVED,       // Approved, processing will begin
    PROCESSING,     // In progress
    COMPLETED,      // Refund done
    REJECTED,       // Seller/admin rejected
    FAILED,         // Processing failed
    CHECKING        // Still loading from Firestore
}
```

**Step 2: Inside OrderCard, replace refund state variables:**

Replace these 3 variables + LaunchedEffect:
```kotlin
var hasExistingRefund by remember { mutableStateOf(false) }
var isCheckingRefund by remember { mutableStateOf(false) }
var hasInitiallyChecked by remember { mutableStateOf(false) }
LaunchedEffect(order.id, currentUserId) { ... }
```

With this:
```kotlin
var refundState by remember { mutableStateOf(OrderRefundState.NONE) }

LaunchedEffect(order.id, currentUserId) {
    if (currentUserId.isEmpty()) return@LaunchedEffect
    val status = order.getStatusEnum()
    if (status !in listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)) return@LaunchedEffect
    
    refundState = OrderRefundState.CHECKING
    
    try {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        // Cheap targeted query — only this order's refunds for this buyer
        val snapshot = db.collection("refunds")
            .whereEqualTo("order_id", order.id)
            .whereEqualTo("buyer_id", currentUserId)
            .get().await()
        
        val refundDoc = snapshot.documents.firstOrNull()
        refundState = if (refundDoc == null) {
            OrderRefundState.NONE
        } else {
            when (refundDoc.getString("status")?.uppercase()) {
                "REQUESTED"  -> OrderRefundState.REQUESTED
                "APPROVED"   -> OrderRefundState.APPROVED
                "PROCESSING" -> OrderRefundState.PROCESSING
                "COMPLETED"  -> OrderRefundState.COMPLETED
                "REJECTED"   -> OrderRefundState.REJECTED
                "FAILED"     -> OrderRefundState.FAILED
                else         -> OrderRefundState.REQUESTED
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("OrderCard", "Error checking refund", e)
        refundState = OrderRefundState.NONE
    }
}
```

**Step 3: Update OrderActionButtons signature:**

Change from:
```kotlin
fun OrderActionButtons(
    order: Order,
    isHighlighted: Boolean = false,
    hasExistingRefund: Boolean = false,
    isCheckingRefund: Boolean = false,
    ...
)
```

To:
```kotlin
fun OrderActionButtons(
    order: Order,
    isHighlighted: Boolean = false,
    refundState: OrderRefundState = OrderRefundState.NONE,  // ✅ single param
    ...
)
```

**Step 4: Update call site in OrderCard:**

Change from:
```kotlin
OrderActionButtons(
    order = order,
    isHighlighted = isHighlighted,
    hasExistingRefund = hasExistingRefund,
    isCheckingRefund = isCheckingRefund,
    ...
)
```

To:
```kotlin
OrderActionButtons(
    order = order,
    isHighlighted = isHighlighted,
    refundState = refundState,  // ✅ single param
    ...
)
```

**Step 5: Replace DELIVERED/COMPLETED branch in OrderActionButtons:**

This is a large replacement - see separate file `MYORDERSSCREEN_REFUND_BUTTONS_PATCH.kt` for complete code.

Key changes:
- ✅ Shows "Refund Pending" badge when REQUESTED
- ✅ Shows "Refund Processing" badge when APPROVED/PROCESSING  
- ✅ Shows "Refund Done" badge when COMPLETED
- ✅ Shows "Refund Rejected" badge (red, tappable) when REJECTED
- ✅ Shows "Refund Failed" badge when FAILED
- ✅ Shows loading spinner when CHECKING

---

## 🔧 PATCH 4: PaymentHistoryScreen.kt - Buyer Refund Badge

### File: `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/PaymentHistoryScreen.kt`

**Replace `BuyerPaymentStatusBadge`:**

```kotlin
@Composable
private fun BuyerPaymentStatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "completed"  -> Success.copy(alpha = 0.10f)         to Success
        "pending"    -> Warning.copy(alpha = 0.15f)         to Warning
        "processing" -> Color(0xFF2196F3).copy(alpha = 0.10f) to Color(0xFF2196F3)
        "failed"     -> Error.copy(alpha = 0.10f)           to Error
        "refunded"   -> Color(0xFF9C27B0).copy(alpha = 0.10f) to Color(0xFF9C27B0)  // ✅ purple
        else         -> BorderColor                          to TextSecondary
    }
    
    Surface(shape = RoundedCornerShape(6.dp), color = backgroundColor) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
```

**Add refund amount indicator in BuyerPaymentCard:**

```kotlin
// Inside BuyerPaymentCard, after payment method row, add:
if (payment.status.lowercase() == "refunded" && payment.refundAmount > 0) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Undo,
            contentDescription = null,
            tint = Color(0xFF9C27B0),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "Refunded: PKR ${String.format(java.util.Locale.US, "%.0f", payment.refundAmount)}",
            fontSize = 11.sp,
            color = Color(0xFF9C27B0),
            fontWeight = FontWeight.SemiBold
        )
    }
}
```

---

## 🔧 PATCH 5: SellerPaymentsScreen.kt - Refund Indicator

### File: `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerPaymentsScreen.kt`

**Add refund notice in PaymentCard:**

```kotlin
// Inside PaymentCard, after PKR amount row, add:
if (payment.status.lowercase() == "refunded" && payment.refundAmount > 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(Error.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "Refunded PKR ${String.format(java.util.Locale.US, "%.0f", payment.refundAmount)} to buyer",
            fontSize = 11.sp,
            color = Error,
            fontWeight = FontWeight.Medium
        )
    }
}
```

---

## 🔧 PATCH 6: CoSellerStorePaymentScreen.kt - Refund Impact

### File: `app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/CoSellerStorePaymentScreen.kt`

**Add refund notice in CoSellerPaymentCard:**

```kotlin
// Inside CoSellerPaymentCard, after amount row, add:
if (payment.status.lowercase() == "refunded") {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Error.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Error.copy(alpha = 0.20f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "This payment was refunded. Splits reversed.",
                fontSize = 11.sp,
                color = Error
            )
        }
    }
}
```

---

## 📊 Complete Flow Summary

### APPROVE REFUND Flow
```
refunds/{id}          → status: "approved" → "processing" → "completed"
seller_payments/{id}  → status: "refunded"
                        refund_amount: <amount>
                        refund_date: <timestamp>

BuyerPaymentHistoryScreen  → badge: "Refunded" (purple)
                              shows refund_amount row
SellerPaymentsScreen       → badge: "Refunded" (gray)
                              shows "Refunded PKR X to buyer" row
CoSellerStorePaymentScreen → badge: "Refunded"
                              shows "splits reversed" notice
MyOrdersScreen OrderCard   → badge: "Refund Done" (green)
```

### REJECT REFUND Flow
```
refunds/{id}          → status: "rejected"
seller_payments/{id}  → NO CHANGE (payment stays "completed")

BuyerPaymentHistoryScreen  → no change (payment still completed)
SellerPaymentsScreen       → no change (payment still completed)
MyOrdersScreen OrderCard   → badge: "Refund Rejected" (red, tappable)
                              tap → opens BuyerRefundRequestScreen
                              which shows RefundStatusCard with reason
```

---

## 🚨 Missing: RefundNotificationService Android Implementation

**Current Status:** `RefundNotificationService.kt` exists but may be missing `notifyRefundRejected()`.

**Required Method:**

```kotlin
suspend fun notifyRefundRejected(refund: RefundRequest, rejectionReason: String) {
    try {
        val notification = Notification(
            userId = refund.buyerId,
            title = "Refund Request Rejected",
            description = "Your refund for PKR ${refund.refundAmount.toInt()} was rejected. Reason: $rejectionReason",
            category = "REFUNDS",
            actionType = "VIEW_ORDER",
            actionData = mapOf(
                "order_id" to refund.orderId,
                "refund_id" to refund.id
            ),
            timestamp = System.currentTimeMillis()
        )
        
        db.collection("notifications").add(notification.toMap()).await()
        Log.d(TAG, "✅ Refund rejection notification sent to buyer: ${refund.buyerId}")
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to send refund rejection notification", e)
    }
}
```

---

## ✅ Implementation Checklist

### Problem 1: Web Dashboard Empty Columns
- [ ] PATCH 1: RefundProcessor.kt - Unified toMap()
- [ ] PATCH 2: RefundModels.kt - Fix RefundRequest.toMap()
- [ ] Test: Create refund from mobile, verify web dashboard shows buyer name

### Problem 2: Rejection Badge/Notification
- [ ] PATCH 3: MyOrdersScreen.kt - OrderRefundState enum
- [ ] PATCH 3: MyOrdersScreen.kt - Replace refund state tracking
- [ ] PATCH 3: MyOrdersScreen.kt - Update OrderActionButtons
- [ ] PATCH 3: MyOrdersScreen.kt - Replace DELIVERED/COMPLETED branch
- [ ] Verify: RefundNotificationService.notifyRefundRejected() exists
- [ ] Test: Seller rejects refund, buyer sees red badge + notification

### Problem 3: Payment Screens Refund Status
- [ ] PATCH 4: PaymentHistoryScreen.kt - Buyer refund badge
- [ ] PATCH 5: SellerPaymentsScreen.kt - Refund indicator
- [ ] PATCH 6: CoSellerStorePaymentScreen.kt - Refund impact notice
- [ ] Test: Approve refund, verify all payment screens show refund status

---

## 📁 Files to Modify

1. ✅ `app/src/main/java/com/gcuf/craftoria/utils/RefundProcessor.kt`
2. ✅ `app/src/main/java/com/gcuf/craftoria/data/model/RefundModels.kt`
3. ✅ `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt`
4. ✅ `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/PaymentHistoryScreen.kt`
5. ✅ `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerPaymentsScreen.kt`
6. ✅ `app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/CoSellerStorePaymentScreen.kt`
7. ⚠️ `app/src/main/java/com/gcuf/craftoria/services/RefundNotificationService.kt` (verify method exists)

---

**Status:** Ready for implementation  
**Priority:** High - affects user experience  
**Testing:** Required after each patch
