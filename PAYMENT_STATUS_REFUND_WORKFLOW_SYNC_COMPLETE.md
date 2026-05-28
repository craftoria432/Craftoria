# Payment Status & Refund Workflow Synchronization - Complete ✅

## Critical Issue Resolved

**Problem:** When a refund was approved, the payment status remained "COMPLETED" instead of updating to reflect the refund workflow. Refunded amounts were still included in the buyer's "Total Spent" calculation.

**Solution:** Implemented automatic payment status synchronization throughout the refund workflow, and updated the stats calculation to exclude refund-related payments.

---

## Refund Workflow & Payment Status Mapping

### Complete Status Flow

| Refund Action | Refund Status | Payment Status | Buyer Sees | Total Spent |
|--------------|---------------|----------------|------------|-------------|
| Buyer submits request | `REQUESTED` | `REFUND_PENDING` | "Refund Pending" (Orange) | ❌ Excluded |
| Seller/Admin approves | `APPROVED_BY_SELLER` / `APPROVED_BY_ADMIN` | `REFUND_PROCESSING` | "Refund Processing" (Blue) | ❌ Excluded |
| System processes | `PROCESSING` | `REFUND_PROCESSING` | "Refund Processing" (Blue) | ❌ Excluded |
| Refund completes | `COMPLETED` | `REFUNDED` | "Refunded" (Purple) | ❌ Excluded |
| Seller/Admin rejects | `REJECTED_BY_SELLER` / `REJECTED_BY_ADMIN` | `REFUND_REJECTED` | "Refund Rejected" (Gray) | ❌ Excluded |

### Payment Status Values

```kotlin
enum class PaymentStatus {
    PENDING,              // Order placed, payment pending
    PROCESSING,           // Payment being processed
    COMPLETED,            // Payment successful ✅ COUNTED in Total Spent
    FAILED,               // Payment failed
    REFUND_PENDING,       // Buyer submitted refund request ❌ NOT counted
    REFUND_PROCESSING,    // Seller/admin approved refund ❌ NOT counted
    REFUNDED,             // Refund completed ❌ NOT counted
    REFUND_REJECTED       // Refund rejected ❌ NOT counted
}
```

---

## Implementation Details

### 1. RefundRepository - Automatic Payment Status Updates

**File:** `app/src/main/java/com/gcuf/craftoria/data/repository/RefundRepository.kt`

#### When Refund is Requested
```kotlin
suspend fun createRefundRequest(...): Result<RefundRequest> {
    // Create refund record
    firestore.collection(REFUNDS_COLLECTION).document(refundId).set(refundRequest.toMap()).await()
    
    // ✅ Update payment status to REFUND_PENDING
    updatePaymentRefundStatus(paymentId, PaymentStatus.REFUND_PENDING.toString())
    
    return Result.success(refundRequest)
}
```

#### When Refund is Approved
```kotlin
suspend fun approveRefund(...): Result<RefundRequest> {
    // Update refund status to APPROVED_BY_SELLER or APPROVED_BY_ADMIN
    firestore.collection(REFUNDS_COLLECTION).document(refundId).update(...)
    
    // ✅ Update payment status to REFUND_PROCESSING
    updatePaymentRefundStatus(refund.paymentId, PaymentStatus.REFUND_PROCESSING.toString())
    
    return Result.success(refund)
}
```

#### When Refund is Completed
```kotlin
suspend fun completeRefund(...): Result<RefundRequest> {
    // Update refund status to COMPLETED
    firestore.collection(REFUNDS_COLLECTION).document(refundId).update(...)
    
    // ✅ Update payment status to REFUNDED with refund details
    updatePaymentRefundStatus(
        refund.paymentId, 
        PaymentStatus.REFUNDED.toString(),
        refund.refundAmount,
        refund.reason,
        now
    )
    
    return Result.success(refund)
}
```

#### When Refund is Rejected
```kotlin
suspend fun rejectRefund(...): Result<RefundRequest> {
    // Update refund status to REJECTED_BY_SELLER or REJECTED_BY_ADMIN
    firestore.collection(REFUNDS_COLLECTION).document(refundId).update(...)
    
    // ✅ Update payment status to REFUND_REJECTED
    updatePaymentRefundStatus(refund.paymentId, PaymentStatus.REFUND_REJECTED.toString())
    
    return Result.success(refund)
}
```

#### Helper Function
```kotlin
private suspend fun updatePaymentRefundStatus(
    paymentId: String, 
    status: String,
    refundAmount: Double = 0.0,
    refundReason: String = "",
    refundDate: Long = 0L
): Result<Unit> {
    val updateMap = mutableMapOf<String, Any>(
        "status" to status,
        "updated_at" to System.currentTimeMillis()
    )
    if (refundAmount > 0) updateMap["refund_amount"] = refundAmount
    if (refundReason.isNotEmpty()) updateMap["refund_reason"] = refundReason
    if (refundDate > 0) updateMap["refund_date"] = refundDate
    
    firestore.collection(PAYMENTS_COLLECTION).document(paymentId).update(updateMap).await()
    return Result.success(Unit)
}
```

---

### 2. BuyerPaymentViewModel - Exclude Refund Payments from Total Spent

**File:** `app/src/main/java/com/gcuf/craftoria/viewmodel/BuyerPaymentViewModel.kt`

#### Updated Stats Calculation
```kotlin
private fun computeStats(payments: List<SellerPayment>): BuyerPaymentStats {
    // ✅ CRITICAL FIX: Only count COMPLETED, PENDING, and PROCESSING payments
    // Exclude all refund-related statuses
    val activePayments = payments.filter { payment ->
        val status = payment.status.uppercase()
        status == PaymentStatus.COMPLETED.toString().uppercase() ||
        status == PaymentStatus.PENDING.toString().uppercase() ||
        status == PaymentStatus.PROCESSING.toString().uppercase()
    }
    
    // REFUND_PENDING, REFUND_PROCESSING, REFUNDED, REFUND_REJECTED are excluded
    
    return BuyerPaymentStats(
        totalSpent = activePayments.sumOf { it.amount },
        completedAmount = completed.sumOf { it.amount },
        pendingAmount = activePayments.filter { 
            it.status.equals(PaymentStatus.PENDING.toString(), ignoreCase = true) 
        }.sumOf { it.amount },
        totalPayments = activePayments.size,
        completedPayments = completed.size,
        totalOrders = activePayments.map { it.orderId }.distinct().size,
        totalSellers = activePayments.map { it.sellerId }.distinct().size
    )
}
```

**Before:**
- Total Spent: PKR 5000 (includes refunded order of PKR 2000)
- Refunded order still shows as "COMPLETED"

**After:**
- Total Spent: PKR 3000 (refunded order excluded)
- Refunded order shows as "REFUNDED" with purple badge

---

### 3. PaymentHistoryScreen - Display Refund Statuses

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/PaymentHistoryScreen.kt`

#### Status Badge with Refund States
```kotlin
@Composable
private fun BuyerPaymentStatusBadge(status: String) {
    val (backgroundColor, textColor, displayText) = when (status.lowercase()) {
        "completed"  -> Triple(Success.copy(alpha = 0.10f), Success, "Completed")
        "pending"    -> Triple(Warning.copy(alpha = 0.15f), Warning, "Pending")
        "processing" -> Triple(Color(0xFF2196F3).copy(alpha = 0.10f), Color(0xFF2196F3), "Processing")
        "failed"     -> Triple(Error.copy(alpha = 0.10f), Error, "Failed")
        "refund_pending" -> Triple(Warning.copy(alpha = 0.15f), Warning, "Refund Pending")
        "refund_processing" -> Triple(Color(0xFF2196F3).copy(alpha = 0.10f), Color(0xFF2196F3), "Refund Processing")
        "refunded"   -> Triple(Color(0xFF9C27B0).copy(alpha = 0.10f), Color(0xFF9C27B0), "Refunded")
        "refund_rejected" -> Triple(Color(0xFF757575).copy(alpha = 0.10f), Color(0xFF757575), "Refund Rejected")
        else         -> Triple(BorderColor, TextSecondary, status.replaceFirstChar { it.uppercase() })
    }
    // ... render badge
}
```

#### Refund Information Display
```kotlin
val paymentStatus = payment.status.lowercase()
when {
    paymentStatus == "refunded" && payment.refundAmount > 0 -> {
        // Show "Refunded: PKR X" with purple icon
    }
    paymentStatus == "refund_processing" -> {
        // Show "Refund Processing: PKR X" with blue icon
    }
    paymentStatus == "refund_pending" -> {
        // Show "Refund Pending: PKR X" with orange icon
    }
    paymentStatus == "refund_rejected" -> {
        // Show "Refund Rejected" with gray icon
    }
}
```

---

## User Experience Flow

### Scenario: Order KNLW1MTK Refund Approved

#### Before Fix:
1. Buyer submits refund request
2. Seller approves refund
3. **Payment History shows:** "COMPLETED" (Green badge)
4. **Total Spent:** PKR 5000 (includes this order)
5. **Problem:** Status doesn't reflect refund approval

#### After Fix:
1. Buyer submits refund request
   - Payment status: `REFUND_PENDING`
   - Badge: "Refund Pending" (Orange)
   - Total Spent: PKR 3000 (excluded)

2. Seller approves refund
   - Payment status: `REFUND_PROCESSING`
   - Badge: "Refund Processing" (Blue)
   - Shows: "Refund Processing: PKR 2000"
   - Total Spent: PKR 3000 (still excluded)

3. System completes refund
   - Payment status: `REFUNDED`
   - Badge: "Refunded" (Purple)
   - Shows: "Refunded: PKR 2000"
   - Total Spent: PKR 3000 (permanently excluded)

---

## My Orders Screen Integration

The My Orders screen already handles refund button states correctly. The payment status synchronization ensures:

1. **No Button Flashing:** Refund state loads once, shows correct button
2. **Correct Status Display:** Button reflects actual refund workflow status
3. **No Loading Indicators:** Smooth transitions between states

### Button States Match Payment Status

| Payment Status | My Orders Button |
|---------------|------------------|
| `COMPLETED` (no refund) | "Request Refund" |
| `REFUND_PENDING` | "Refund Pending" (Orange badge) |
| `REFUND_PROCESSING` | "Refund Processing" (Blue badge with spinner) |
| `REFUNDED` | "Refund Done" (Green badge) |
| `REFUND_REJECTED` (can resubmit) | "Resubmit Refund" (Orange button) |
| `REFUND_REJECTED` (final) | "Refund Denied" (Gray badge) |

---

## Database Schema

### seller_payments Collection

```javascript
{
  id: "payment_id",
  order_id: "KNLW1MTK",
  buyer_id: "buyer_123",
  seller_id: "seller_456",
  amount: 2000,
  status: "refund_processing",  // ✅ Updated automatically
  refund_amount: 2000,           // ✅ Set when refund completes
  refund_reason: "Defective product",
  refund_date: 1234567890,
  payment_date: 1234567800,
  updated_at: 1234567890
}
```

### refunds Collection

```javascript
{
  id: "refund_id",
  order_id: "KNLW1MTK",
  payment_id: "payment_id",
  buyer_id: "buyer_123",
  seller_id: "seller_456",
  status: "approved_by_seller",  // ✅ Refund workflow status
  refund_amount: 2000,
  reason: "Defective product",
  requested_at: 1234567800,
  approved_at: 1234567850,
  approved_by: "seller_456"
}
```

---

## Testing Checklist

### Payment History Screen
- [ ] Order with `REFUND_PENDING` shows orange "Refund Pending" badge
- [ ] Order with `REFUND_PROCESSING` shows blue "Refund Processing" badge
- [ ] Order with `REFUNDED` shows purple "Refunded" badge
- [ ] Order with `REFUND_REJECTED` shows gray "Refund Rejected" badge
- [ ] Total Spent excludes all refund-related payments
- [ ] Completed Amount only counts `COMPLETED` payments
- [ ] Filter tabs work correctly with refund statuses

### My Orders Screen
- [ ] Approved refund shows "Refund Processing" button (blue)
- [ ] No button flashing when scrolling
- [ ] No temporary incorrect buttons appear
- [ ] Button state matches payment status exactly

### Refund Workflow
- [ ] Submit refund → Payment status becomes `REFUND_PENDING`
- [ ] Approve refund → Payment status becomes `REFUND_PROCESSING`
- [ ] Complete refund → Payment status becomes `REFUNDED`
- [ ] Reject refund → Payment status becomes `REFUND_REJECTED`
- [ ] All status changes happen automatically

---

## Files Modified

### Android App
1. **app/src/main/java/com/gcuf/craftoria/data/repository/RefundRepository.kt**
   - Added `updatePaymentRefundStatus()` helper function
   - Call payment status update in `createRefundRequest()`
   - Call payment status update in `approveRefund()`
   - Call payment status update in `rejectRefund()`
   - Call payment status update in `completeRefund()`
   - Call payment status update in `processRefund()`

2. **app/src/main/java/com/gcuf/craftoria/viewmodel/BuyerPaymentViewModel.kt**
   - Updated `computeStats()` to exclude refund-related payments
   - Only count `COMPLETED`, `PENDING`, `PROCESSING` in Total Spent
   - Added logging for stats calculation

3. **app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/PaymentHistoryScreen.kt**
   - Updated `BuyerPaymentStatusBadge()` with refund status colors
   - Updated payment card refund info display logic
   - Show refund status based on payment status, not just refund amount

---

## Production Deployment Notes

### Database Migration
**No migration needed!** The payment status will update automatically when:
1. New refunds are created (status → `REFUND_PENDING`)
2. Existing refunds are approved/rejected/completed (status updates)

### Existing Orders
- Orders with completed refunds: Status will remain `COMPLETED` until next refund action
- **Optional:** Run a one-time script to sync existing refund statuses:

```javascript
// Firebase Admin SDK script
const admin = require('firebase-admin');
const db = admin.firestore();

async function syncExistingRefunds() {
  const refunds = await db.collection('refunds').get();
  
  for (const refundDoc of refunds.docs) {
    const refund = refundDoc.data();
    const paymentId = refund.payment_id;
    
    let paymentStatus;
    switch (refund.status) {
      case 'requested':
      case 'under_review':
        paymentStatus = 'refund_pending';
        break;
      case 'approved_by_seller':
      case 'approved_by_admin':
      case 'processing':
        paymentStatus = 'refund_processing';
        break;
      case 'completed':
        paymentStatus = 'refunded';
        break;
      case 'rejected_by_seller':
      case 'rejected_by_admin':
        paymentStatus = 'refund_rejected';
        break;
      default:
        continue;
    }
    
    await db.collection('seller_payments').doc(paymentId).update({
      status: paymentStatus,
      updated_at: Date.now()
    });
    
    console.log(`✅ Synced payment ${paymentId} to ${paymentStatus}`);
  }
}

syncExistingRefunds();
```

---

## Summary

✅ **Payment Status Sync:** Automatic updates throughout refund workflow
✅ **Total Spent Accuracy:** Refund-related payments excluded from calculation
✅ **Status Display:** Correct badges and colors for all refund states
✅ **No Button Flashing:** Smooth, consistent UI in My Orders screen
✅ **Real-time Updates:** Payment history reflects current refund status

**Result:** Payment history and refund workflow are now fully synchronized! 🚀
