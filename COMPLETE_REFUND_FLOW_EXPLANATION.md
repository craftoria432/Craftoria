# Complete Refund Flow: From Request to "Refund Done" ✅

## Overview

This document explains the complete refund workflow in Craftoria, including how old refunds are fixed and how new refunds work.

## Refund Status States

```
REQUESTED
    ↓
UNDER_REVIEW (optional)
    ↓
APPROVED_BY_SELLER or APPROVED_BY_ADMIN
    ↓
PROCESSING (intermediate state)
    ↓
COMPLETED ← Final state (buyer sees "Refund Done")
    ↓
(or REJECTED_BY_SELLER / REJECTED_BY_ADMIN)
(or FAILED)
(or CANCELLED)
```

## Timeline: How Long Does "Refund Processing" Last?

### Answer: Practically Zero Time (~300ms total)

For **Cash on Delivery** system (no external payment gateway):

```
Seller taps "Approve" button
    ↓
approveRefund() called
    ├─ ~50ms: Firestore writes status = APPROVED_BY_SELLER
    ├─ ~50ms: completeRefund() called IMMEDIATELY (same function)
    │   ├─ ~50ms: Firestore writes status = COMPLETED
    │   ├─ ~50ms: Firestore writes payment status = REFUNDED
    │   ├─ ~50ms: Firestore writes order is_refunded = true
    │   └─ Notification sent to buyer
    ↓
~100-300ms: Real-time listener fires on buyer's device
    ↓
Buyer sees:
├─ My Orders: "Refund Done" button (green) ✅
├─ My Orders: [↶ Refunded] badge (purple) ✅
├─ Payment History: "Refunded" badge (purple) ✅
└─ Order card: ONLY [↶ Refunded] badge (no [Completed] badge)
```

**Total time from seller approval to buyer seeing "Refund Done": ~300-600ms on good connection**

The buyer will **almost never** see "Refund Approved" or "Processing" states — they flash for under a second or skip entirely if the listener fires after both writes land.

## Why Old Refunds Were Stuck

### Before the Fix
```
Old Refund Created (before auto-complete logic existed)
    ↓
Seller approved it
    ↓
Status set to APPROVED_BY_SELLER
    ↓
completeRefund() was NOT called (didn't exist yet)
    ↓
Status stuck at PROCESSING forever
    ↓
Buyer sees "Refund Processing" indefinitely
```

### After the Migration
```
Migration runs on first app launch
    ↓
Finds all refunds with status = PROCESSING
    ↓
Updates each to status = COMPLETED
    ↓
Updates payment status to REFUNDED
    ↓
Updates order is_refunded = true
    ↓
Buyer sees "Refund Done" immediately ✅
```

## How New Refunds Work (After Fix)

### Step 1: Buyer Requests Refund
```
BuyerRefundRequestScreen
    ↓
User fills form:
├─ Reason (e.g., "Damaged")
├─ Description
└─ Refund type (Full/Partial)
    ↓
User taps "Request Refund"
    ↓
RefundRepository.createRefundRequest() called
    ↓
Firestore creates refund document:
├─ status = REQUESTED
├─ requested_at = now
├─ buyer_id = current user
└─ order_id = order being refunded
    ↓
Notification sent to seller
```

### Step 2: Seller Reviews & Approves
```
SellerRefundManagementScreen
    ↓
Seller sees refund request
    ↓
Seller taps "Approve"
    ↓
RefundRepository.approveRefund() called
    ├─ Sets status = APPROVED_BY_SELLER
    ├─ Sets approved_at = now
    ├─ Calls completeRefund() IMMEDIATELY
    │   ├─ Sets status = COMPLETED
    │   ├─ Sets completed_at = now
    │   ├─ Updates payment status = REFUNDED
    │   ├─ Updates order is_refunded = true
    │   └─ Sends notification to buyer
    └─ Returns completed refund
    ↓
Notification sent to buyer: "Your refund has been completed"
```

### Step 3: Buyer Sees "Refund Done"
```
Real-time listener fires on buyer's device
    ↓
MyOrdersScreen updates:
├─ Order card shows [↶ Refunded] badge (purple)
├─ Order status badge is SUPPRESSED (no [Completed] badge)
└─ Refund button changes to "Refund Done" (green)
    ↓
PaymentHistoryScreen updates:
├─ Payment shows "Refunded" badge (purple)
└─ Refund status shows "Refunded"
    ↓
RefundDetailsScreen shows:
├─ Status banner: "Refund Completed ✓" (green)
├─ Timeline: All steps completed
└─ Breakdown: Refund amount shown
```

## Key Differences: Old vs New Refunds

| Aspect | Old Refunds (Before Fix) | New Refunds (After Fix) |
|--------|--------------------------|------------------------|
| **Status** | Stuck in PROCESSING | Transitions to COMPLETED |
| **Buyer sees** | "Refund Processing" | "Refund Done" |
| **Button state** | "Refund Approved" (blue) | "Refund Done" (green) |
| **Badge** | "Refund Processing" (blue) | "Refunded" (purple) |
| **Fix applied** | Migration on first app launch | Auto-complete on approval |
| **Time to complete** | Never (stuck) | ~300ms |

## Real-Time Listeners

### MyOrdersScreen OrderCard
```kotlin
DisposableEffect(order.id) {
    val listener = firestore.collection("refunds")
        .whereEqualTo("order_id", order.id)
        .limit(5)
        .addSnapshotListener { snapshot, error ->
            // Updates refundState when refund status changes
            refundState = docToRefundState(best)
        }
    onDispose { listener.remove() }
}
```

**How it works**:
1. Listener queries for refunds matching this order
2. When refund status changes in Firestore, listener fires
3. `refundState` updates to new status
4. UI recomposes and shows new badge/button

**Why old refunds didn't update**:
- Listener fires once when screen loads
- Old refund status never changes (stuck in PROCESSING)
- Listener waits forever for an update that never comes

**Why migration fixes it**:
- Migration updates old refund status to COMPLETED
- Listener fires again (Firestore data changed)
- `refundState` updates to COMPLETED
- UI shows "Refund Done" ✅

## Badge Logic (Task 2)

When `refundState == COMPLETED`:
- Show ONLY [↶ Refunded] badge (purple)
- Suppress order status badge (no [Completed] badge)
- This eliminates redundancy

When `refundState != COMPLETED`:
- Show order status badge ([Completed], [Pending], etc.)
- Show refund badge if refund is in progress

## Payment Status Sync

When refund is completed:
```
Refund status = COMPLETED
    ↓
Payment status = REFUNDED
    ↓
Order is_refunded = true
    ↓
All three are in sync
```

This ensures:
- Payment History shows correct status
- My Orders shows correct badge
- Order details show refund info

## Notification Flow

When refund is completed:
```
completeRefund() called
    ↓
RefundNotificationService.notifyRefundCompleted() called
    ↓
Notification created:
├─ Title: "Refund Completed"
├─ Body: "Your refund of PKR X has been processed"
├─ Order ID: linked for navigation
└─ Type: REFUND_COMPLETED
    ↓
Notification sent to buyer
    ↓
Buyer taps notification
    ↓
Navigates to RefundDetailsScreen
```

## Summary

### For Old Refunds
- **Problem**: Stuck in PROCESSING state
- **Cause**: Created before auto-complete logic existed
- **Fix**: Migration updates them to COMPLETED on first app launch
- **Result**: Buyer sees "Refund Done" immediately

### For New Refunds
- **Flow**: REQUESTED → APPROVED → COMPLETED in ~300ms
- **Auto-complete**: `approveRefund()` calls `completeRefund()` immediately
- **Real-time**: Listener fires and UI updates
- **Result**: Buyer sees "Refund Done" within seconds

### Key Insight
The "Refund Processing" state is **practically invisible** in a Cash on Delivery system because the entire process (approval → completion) happens in ~300ms. The intermediate states only matter if you later add a real payment gateway (Stripe/PayPal) that introduces seconds-to-minutes delays.
