# Refund Approval: Payment Status & Display Specification

## Overview

When a refund is **approved by the seller or admin**, the payment record and order status should reflect the refund state accurately across both the **Payment History** and **My Orders** screens.

---

## Current Data Model

### Payment Record (SellerPayment)
```kotlin
data class SellerPayment(
    var status: String = PaymentStatus.PENDING.toString(),  // pending, processing, completed, failed, refunded
    var refundAmount: Double = 0.0,
    var refundReason: String = "",
    var refundDate: Long? = null
)

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED  // ← This is the key status for refunded payments
}
```

### Refund Record (RefundRequest)
```kotlin
data class RefundRequest(
    var status: String = RefundStatus.REQUESTED.toString(),
    var approvedAt: Any? = null,
    var completedAt: Any? = null,
    var refundAmount: Double = 0.0
)

enum class RefundStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED_BY_SELLER,      // ← Approval stage
    APPROVED_BY_ADMIN,       // ← Approval stage
    PROCESSING,              // ← Processing stage
    COMPLETED,               // ← Final stage
    FAILED,
    REJECTED_BY_SELLER,
    REJECTED_BY_ADMIN
}
```

---

## Refund Approval Flow & Payment Status Updates

### Stage 1: Refund Requested (Initial State)
```
Buyer submits refund request
    ↓
RefundRequest.status = "REQUESTED"
SellerPayment.status = "COMPLETED"  (unchanged - still completed)
SellerPayment.refundAmount = 0.0    (no refund yet)
```

### Stage 2: Refund Approved by Seller/Admin ⭐ **KEY STAGE**
```
Seller/Admin approves the refund
    ↓
RefundRequest.status = "APPROVED_BY_SELLER" or "APPROVED_BY_ADMIN"
RefundRequest.approvedAt = System.currentTimeMillis()
    ↓
SellerPayment.status = "PROCESSING"  ← CHANGE: Mark as processing
SellerPayment.refundAmount = refund_amount
SellerPayment.refundReason = reason
SellerPayment.refundDate = null      (not yet completed)
```

**Why this change?**
- The payment is no longer "completed" in the traditional sense
- It's now in a "processing" state where the refund is being handled
- This signals to the buyer that their refund is being processed

### Stage 3: Refund Processing
```
Payment gateway processes the refund
    ↓
RefundRequest.status = "PROCESSING"
    ↓
SellerPayment.status = "PROCESSING"  (unchanged)
SellerPayment.refundAmount = refund_amount
```

### Stage 4: Refund Completed ✅ **FINAL STAGE**
```
Refund successfully processed
    ↓
RefundRequest.status = "COMPLETED"
RefundRequest.completedAt = System.currentTimeMillis()
    ↓
SellerPayment.status = "REFUNDED"    ← CHANGE: Mark as refunded
SellerPayment.refundAmount = refund_amount
SellerPayment.refundDate = System.currentTimeMillis()
```

---

## Display Behavior by Screen

### Payment History Screen

#### When Payment Status = "COMPLETED" (No Refund)
```
Order #ABC123
Amount: PKR 5,000
Status: ✅ Completed
Date: Jan 15, 2025
```

#### When Payment Status = "PROCESSING" (Refund Approved/Processing)
```
Order #ABC123
Amount: PKR 5,000
Status: ⏳ Refund Processing
Refund Amount: PKR 5,000
Date: Jan 15, 2025
```

**Display Logic:**
```kotlin
when (payment.status) {
    "completed" -> {
        if (payment.refundAmount > 0) {
            // Refund is being processed
            showStatus("Refund Processing")
            showRefundAmount(payment.refundAmount)
        } else {
            // Normal completed payment
            showStatus("Completed")
        }
    }
    "refunded" -> {
        showStatus("Refunded")
        showRefundAmount(payment.refundAmount)
    }
}
```

#### When Payment Status = "REFUNDED" (Refund Completed)
```
Order #ABC123
Amount: PKR 5,000
Status: ✅ Refunded
Refund Amount: PKR 5,000
Date: Jan 15, 2025
```

### My Orders Screen

#### When Order Status = "DELIVERED" + Refund Approved
```
Order #ABC123
Status: Delivered
Refund Button: "Refund Processing" (blue badge with spinner)
```

#### When Order Status = "DELIVERED" + Refund Completed
```
Order #ABC123
Status: Delivered
Refund Button: "Refund Done" (green badge with checkmark)
```

---

## Implementation Requirements

### 1. When Refund is Approved (Seller/Admin Action)

**Cloud Function or Backend Logic:**
```javascript
// When seller/admin approves refund
async function approveRefund(refundId, approverId) {
    const refundRef = db.collection('refunds').doc(refundId);
    const refund = await refundRef.get();
    
    // Update refund status
    await refundRef.update({
        status: 'APPROVED_BY_SELLER',  // or APPROVED_BY_ADMIN
        approved_by: approverId,
        approved_at: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // ✅ UPDATE PAYMENT STATUS
    const paymentRef = db.collection('payments').doc(refund.data().payment_id);
    await paymentRef.update({
        status: 'PROCESSING',           // ← KEY CHANGE
        refund_amount: refund.data().refund_amount,
        refund_reason: refund.data().reason,
        updated_at: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // Create notification for buyer
    await createNotification(refund.data().buyer_id, {
        title: 'Refund Approved',
        description: `Your refund of PKR ${refund.data().refund_amount} has been approved and is being processed.`,
        category: 'PAYMENTS'
    });
}
```

### 2. When Refund is Completed (Payment Gateway Callback)

**Cloud Function or Backend Logic:**
```javascript
// When payment gateway confirms refund completion
async function completeRefund(refundId, gatewayRefundId) {
    const refundRef = db.collection('refunds').doc(refundId);
    const refund = await refundRef.get();
    
    // Update refund status
    await refundRef.update({
        status: 'COMPLETED',
        gateway_refund_id: gatewayRefundId,
        completed_at: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // ✅ UPDATE PAYMENT STATUS TO REFUNDED
    const paymentRef = db.collection('payments').doc(refund.data().payment_id);
    await paymentRef.update({
        status: 'REFUNDED',             // ← KEY CHANGE
        refund_date: admin.firestore.FieldValue.serverTimestamp(),
        updated_at: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // Create notification for buyer
    await createNotification(refund.data().buyer_id, {
        title: 'Refund Completed',
        description: `Your refund of PKR ${refund.data().refund_amount} has been successfully processed.`,
        category: 'PAYMENTS'
    });
}
```

### 3. Mobile App: Update Payment History Display

**PaymentHistoryScreen.kt:**
```kotlin
@Composable
fun BuyerPaymentCard(payment: SellerPayment) {
    val statusDisplay = when {
        payment.status == "completed" && payment.refundAmount > 0 -> {
            // Refund is being processed
            Pair("Refund Processing", Warning)
        }
        payment.status == "refunded" -> {
            Pair("Refunded", Success)
        }
        payment.status == "completed" -> {
            Pair("Completed", Success)
        }
        else -> Pair(payment.status.uppercase(), Primary)
    }
    
    Card {
        Row {
            Text(text = "PKR ${payment.amount.toInt()}")
            Text(text = statusDisplay.first, color = statusDisplay.second)
            
            // Show refund amount if applicable
            if (payment.refundAmount > 0) {
                Text(text = "Refund: PKR ${payment.refundAmount.toInt()}")
            }
        }
    }
}
```

### 4. Mobile App: Update My Orders Display

**MyOrdersScreen.kt (Already Implemented):**
```kotlin
// The refund button logic already handles this correctly:
when (refundState) {
    OrderRefundState.APPROVED, OrderRefundState.PROCESSING -> {
        // Show "Refund Processing" badge
        OutlinedButton(
            onClick = onViewDetails,
            enabled = false
        ) {
            Row {
                CircularProgressIndicator(modifier = Modifier.size(14.dp))
                Text(text = "Refund Processing")
            }
        }
    }
    OrderRefundState.COMPLETED -> {
        // Show "Refund Done" badge
        OutlinedButton(
            onClick = onViewDetails,
            enabled = false
        ) {
            Row {
                Icon(Icons.Default.CheckCircle)
                Text(text = "Refund Done")
            }
        }
    }
}
```

---

## State Transition Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    REFUND APPROVAL FLOW                         │
└─────────────────────────────────────────────────────────────────┘

BUYER SUBMITS REFUND REQUEST
    ↓
RefundRequest.status = REQUESTED
SellerPayment.status = COMPLETED (unchanged)
    ↓
    ├─→ SELLER/ADMIN REJECTS
    │   RefundRequest.status = REJECTED_BY_SELLER
    │   SellerPayment.status = COMPLETED (unchanged)
    │
    └─→ SELLER/ADMIN APPROVES ⭐
        RefundRequest.status = APPROVED_BY_SELLER
        SellerPayment.status = PROCESSING ← KEY CHANGE
        SellerPayment.refundAmount = amount
            ↓
            PAYMENT GATEWAY PROCESSES
            RefundRequest.status = PROCESSING
            SellerPayment.status = PROCESSING (unchanged)
                ↓
                ├─→ REFUND FAILS
                │   RefundRequest.status = FAILED
                │   SellerPayment.status = COMPLETED (revert)
                │
                └─→ REFUND SUCCEEDS ✅
                    RefundRequest.status = COMPLETED
                    SellerPayment.status = REFUNDED ← KEY CHANGE
                    SellerPayment.refundDate = now
```

---

## Summary of Changes

| Stage | RefundRequest Status | SellerPayment Status | Display (Payment History) | Display (My Orders) |
|-------|----------------------|----------------------|---------------------------|---------------------|
| Initial | REQUESTED | COMPLETED | Completed | Request Refund |
| **Approved** | **APPROVED_BY_SELLER** | **PROCESSING** | **Refund Processing** | **Refund Processing** |
| Processing | PROCESSING | PROCESSING | Refund Processing | Refund Processing |
| **Completed** | **COMPLETED** | **REFUNDED** | **Refunded** | **Refund Done** |
| Rejected | REJECTED_BY_SELLER | COMPLETED | Completed | Resubmit Refund |

---

## Key Points

1. **Payment Status Changes**: When refund is approved, payment status should change from `COMPLETED` to `PROCESSING`
2. **Refund Amount Tracking**: `refundAmount` field should be populated when refund is approved
3. **Refund Date**: `refundDate` should only be set when refund is actually completed
4. **Display Logic**: Both screens should check both payment status AND refund amount to determine what to show
5. **Notifications**: Send notifications to buyer at approval and completion stages
6. **Consistency**: Ensure payment history and my orders screens show consistent information

---

## Testing Checklist

- [ ] Buyer submits refund → Payment shows "Completed", refund button shows "Request Refund"
- [ ] Seller approves refund → Payment shows "Refund Processing", refund button shows "Refund Processing"
- [ ] Refund completes → Payment shows "Refunded", refund button shows "Refund Done"
- [ ] Refund fails → Payment reverts to "Completed", refund button shows "Resubmit Refund"
- [ ] Buyer receives notification when refund is approved
- [ ] Buyer receives notification when refund is completed
- [ ] Payment history screen updates in real-time
- [ ] My orders screen updates in real-time
