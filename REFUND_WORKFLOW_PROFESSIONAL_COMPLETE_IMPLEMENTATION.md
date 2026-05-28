# Professional Refund Workflow - Complete Implementation Guide

**Date**: May 13, 2026  
**Status**: IMPLEMENTATION IN PROGRESS  
**Scope**: Complete refund workflow with all states, transitions, and screen updates

---

## Complete Refund Workflow States

### State Definitions

```
REFUND_PENDING
├─ Duration: 0-5 minutes
├─ Trigger: Buyer submits refund request
├─ UI: "Request Refund" button → "Refund Pending" (orange, disabled)
├─ Seller View: Sees refund request notification
└─ Next State: REFUND_APPROVED (if seller approves) or REFUND_REJECTED (if seller rejects)

REFUND_APPROVED (or APPROVED_BY_SELLER / APPROVED_BY_ADMIN)
├─ Duration: 5-30 seconds
├─ Trigger: Seller or admin approves the refund request
├─ UI: "Refund Approved" (blue, disabled)
├─ Backend: Prepares refund processing
├─ Seller View: Sees approval confirmation
└─ Next State: REFUND_PROCESSING (automatic)

REFUND_PROCESSING
├─ Duration: 5-30 seconds
├─ Trigger: Backend starts processing refund
├─ UI: "Processing" (blue with spinner)
├─ Backend Actions:
│  ├─ Deduct from seller earnings
│  ├─ Credit buyer wallet
│  ├─ Calculate co-seller splits
│  ├─ Update payment records
│  ├─ Create audit logs
│  └─ Update Firestore documents
├─ Seller View: Sees "Processing" status
├─ Buyer View: Sees "Processing" button
└─ Next State: REFUNDED (automatic on success) or FAILED (on error)

REFUNDED (Final State)
├─ Duration: Permanent
├─ Trigger: Backend completes all refund operations
├─ UI: "Refund Done" (green, disabled) + [↶ Refunded] badge (purple)
├─ Backend: All updates complete
├─ Seller View: Sees "REFUNDED" status, earnings adjusted
├─ Buyer View: Sees "Refund Done" button + badge
├─ Payment Status: REFUNDED (green)
└─ Order Status: COMPLETED (NOT cancelled, stays in Completed tab)

REFUND_REJECTED (Terminal State)
├─ Duration: Permanent (unless can_resubmit = true)
├─ Trigger: Seller or admin rejects refund request
├─ UI: "Resubmit" button (orange) if can_resubmit = true
├─ UI: "Refund Denied" (gray, disabled) if can_resubmit = false
├─ Seller View: Sees rejection confirmation
├─ Buyer View: Can resubmit if allowed
└─ Next State: REFUND_PENDING (if resubmitted) or FINAL_DECISION (if rejected twice)

FINAL_DECISION (Terminal State)
├─ Duration: Permanent
├─ Trigger: Refund rejected twice (rejection_count >= 2)
├─ UI: "Refund Denied" (gray, disabled)
├─ Seller View: Sees final decision
├─ Buyer View: Cannot resubmit
└─ Next State: None (final)

REFUND_FAILED (Error State)
├─ Duration: Temporary (can retry)
├─ Trigger: Backend processing failed
├─ UI: "Refund Failed" (red, disabled)
├─ Backend: Error logged, can retry
├─ Seller View: Sees error status
├─ Buyer View: Sees error status
└─ Next State: REFUND_PROCESSING (on retry)
```

---

## Screen-by-Screen Implementation

### 1. Buyer's My Orders Screen

**Order Card Header**:
```
Order #ABC12345          [↶ Refunded] [Completed]  ← Badge only when REFUNDED
Placed on May 13, 2:30 PM
```

**Button States**:
- **No Refund (within 30 days)**: "Request Refund" (orange)
- **REFUND_PENDING**: "Refund Pending" (orange, disabled)
- **REFUND_APPROVED**: "Refund Approved" (blue, disabled)
- **REFUND_PROCESSING**: "Processing" (blue with spinner)
- **REFUNDED**: "Refund Done" (green, disabled) + Badge
- **REFUND_REJECTED (can_resubmit=true)**: "Resubmit" (orange)
- **FINAL_DECISION**: "Refund Denied" (gray, disabled)
- **REFUND_FAILED**: "Refund Failed" (red, disabled)

**Real-Time Updates**:
- Listen to `refunds` collection for this order
- Update button state immediately when status changes
- Show badge when status = COMPLETED

---

### 2. Seller's Orders Screen

**Order Card Header**:
```
Order #ABC12345          [↶ Refunded] [Delivered]  ← Badge only when REFUNDED
May 13, 02:30 PM
```

**Display Logic**:
- Show badge when refund status = COMPLETED
- Show refund amount in order details
- Show "View Refund" button when refund exists

**Real-Time Updates**:
- Listen to `refunds` collection for this order
- Update badge immediately when status changes

---

### 3. Buyer's Payment History Screen

**Payment Card**:
```
Order #ABC12345                                    [Refunded]
Ahmed Khan

1 item(s)
PKR 5,000                                    May 13, 2:30 PM

↶ Refunded: PKR 5,000
Payment Method: Cash on Delivery
```

**Status Badge**:
- **REFUND_PENDING**: "Refund Pending" (orange)
- **REFUND_PROCESSING**: "Refund Processing" (blue)
- **REFUNDED**: "Refunded" (purple) ← Final state
- **REFUND_REJECTED**: "Refund Rejected" (gray)

**Display**:
- Show refund amount when status = REFUNDED
- Show refund date when available
- Show refund reason when available

---

### 4. Seller's Payment Details Screen

**Payment Status Card**:
```
Status: REFUNDED (Green)
Amount: PKR 5,000
Refund Amount: PKR 5,000
Refund Date: May 13, 2:30 PM
```

**Earnings Display**:
- Original Earnings: PKR 5,000
- Refund Deduction: -PKR 5,000
- Final Earnings: PKR 0

**Timeline Entry**:
- Shows refund completion with date/time
- Shows refund amount
- Shows refund reason

---

### 5. Co-Seller Payment Details Screen

**Payment Status Card**:
```
Status: REFUNDED (Green)
Amount: PKR 2,500 (co-seller's split)
Refund Amount: PKR 1,250 (co-seller's refund split)
Refund Date: May 13, 2:30 PM
```

**Earnings Display**:
- Original Earnings: PKR 2,500
- Refund Deduction: -PKR 1,250 (their split)
- Final Earnings: PKR 1,250

**Timeline Entry**:
- Shows refund completion with date/time
- Shows co-seller's refund split amount
- Shows refund reason

---

## Backend Processing Flow

### When Seller Approves Refund

```
1. Update refunds collection:
   - status: "APPROVED_BY_SELLER"
   - approved_by: seller_id
   - approved_at: current_timestamp

2. Trigger backend function:
   - Check payment exists
   - Validate refund amount
   - Calculate co-seller splits
   - Update payment status to "REFUND_PROCESSING"

3. Process refund:
   - Deduct from seller earnings
   - Credit buyer wallet
   - Update co-seller earnings
   - Create audit log entry

4. Mark complete:
   - Update refunds status: "COMPLETED"
   - Update payment status: "REFUNDED"
   - Update order: is_refunded = true
   - Create notification for buyer
   - Create notification for seller
```

### Real-Time Sync

```
Firestore Listeners:
├─ refunds collection (by order_id)
├─ payments collection (by payment_id)
├─ orders collection (by order_id)
└─ seller_payments collection (by seller_id)

Update Triggers:
├─ Refund status changes → Update button state
├─ Payment status changes → Update payment display
├─ Order is_refunded changes → Show badge
└─ Seller earnings changes → Update earnings display
```

---

## Implementation Checklist

### Phase 1: Backend (Already Done)
- ✅ Refund request creation
- ✅ Seller approval logic
- ✅ Refund processing
- ✅ Payment status updates
- ✅ Earnings adjustments
- ✅ Co-seller split calculations

### Phase 2: UI State Management (In Progress)
- [ ] MyOrdersScreen: Add refund state tracking
- [ ] MyOrdersScreen: Update button states
- [ ] MyOrdersScreen: Show refund badge
- [ ] SellerOrdersScreen: Add refund state tracking
- [ ] SellerOrdersScreen: Show refund badge
- [ ] PaymentHistoryScreen: Update status display
- [ ] PaymentDetailScreen: Update earnings display
- [ ] CoSellerPaymentScreen: Update earnings display

### Phase 3: Real-Time Updates (In Progress)
- [ ] Add Firestore listeners to all screens
- [ ] Implement state priority algorithm
- [ ] Handle listener cleanup
- [ ] Test real-time sync

### Phase 4: Testing
- [ ] Test refund request flow
- [ ] Test seller approval
- [ ] Test refund processing
- [ ] Test real-time updates
- [ ] Test co-seller splits
- [ ] Test error handling
- [ ] Test edge cases

---

## Key Implementation Details

### State Priority Algorithm

When multiple refund documents exist, use this priority:

```kotlin
fun docPriority(doc: DocumentSnapshot): Int {
    val isFinal  = doc.getBoolean("final_decision") ?: false
    val statusUp = doc.getString("status")?.uppercase() ?: "REQUESTED"
    return when {
        statusUp == "COMPLETED"                                                     -> 100
        isFinal                                                                     -> 90
        statusUp in listOf("APPROVED", "APPROVED_BY_SELLER", "APPROVED_BY_ADMIN")  -> 80
        statusUp == "PROCESSING"                                                    -> 70
        statusUp in listOf("REQUESTED", "UNDER_REVIEW")                            -> 60
        statusUp in listOf("REJECTED", "REJECTED_BY_SELLER", "REJECTED_BY_ADMIN")  -> 50
        statusUp == "FAILED"                                                        -> 40
        else                                                                        -> 10
    }
}
```

### Real-Time Listener Pattern

```kotlin
DisposableEffect(order.id) {
    val listener = db.collection("refunds")
        .whereEqualTo("order_id", order.id)
        .addSnapshotListener { snapshot, error ->
            // Update state based on snapshot
        }
    
    onDispose {
        listener.remove()
    }
}
```

### Button State Mapping

```kotlin
when (refundState) {
    OrderRefundState.NONE -> {
        if (withinWindow) "Request Refund" else "View Details"
    }
    OrderRefundState.REQUESTED -> "Refund Pending"
    OrderRefundState.APPROVED -> "Refund Approved"
    OrderRefundState.PROCESSING -> "Processing"
    OrderRefundState.COMPLETED -> "Refund Done"
    OrderRefundState.REJECTED -> "Resubmit"
    OrderRefundState.FINAL_DECISION -> "Refund Denied"
    OrderRefundState.FAILED -> "Refund Failed"
}
```

---

## Testing Scenarios

### Scenario 1: Happy Path
1. Buyer requests refund
2. Seller approves
3. Backend processes
4. Refund completes
5. ✅ Badge appears, button shows "Refund Done"

### Scenario 2: Rejection & Resubmission
1. Buyer requests refund
2. Seller rejects
3. Buyer resubmits
4. Seller approves
5. ✅ Refund completes

### Scenario 3: Multiple Refund Requests
1. Buyer requests refund
2. Seller rejects
3. Buyer resubmits
4. Seller rejects again
5. ✅ Button shows "Refund Denied", no more resubmit

### Scenario 4: Real-Time Sync
1. Buyer opens My Orders
2. Seller approves refund from admin dashboard
3. ✅ Button updates immediately (no refresh needed)
4. ✅ Badge appears instantly

### Scenario 5: Co-Seller Refund Split
1. Order has co-seller
2. Refund approved
3. ✅ Co-seller earnings adjusted by their split
4. ✅ Seller earnings adjusted by their split

---

## Deployment Checklist

- [ ] All screens compile without errors
- [ ] Real-time listeners work correctly
- [ ] Button states update properly
- [ ] Badge displays when refund completed
- [ ] Earnings adjusted correctly
- [ ] Co-seller splits calculated correctly
- [ ] No memory leaks from listeners
- [ ] All edge cases handled
- [ ] Error handling implemented
- [ ] Tested on actual device/emulator

---

**Status**: Ready for implementation
