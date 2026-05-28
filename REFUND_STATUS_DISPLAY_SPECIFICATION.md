# REFUND STATUS DISPLAY SPECIFICATION

**Status:** Complete specification for all screens  
**Date:** May 13, 2026  
**Scope:** Order and payment status display across all buyer/seller screens

---

## Scenario: Completed Order with Refund Approved

**Setup:**
- Seller marks order as Completed/Delivered
- Buyer requests refund
- Seller or Admin approves the refund

---

## Screen-by-Screen Status Display

### 1. BUYER'S MY ORDERS SCREEN

#### Order Card Display

```
┌─────────────────────────────────────────────────────────┐
│ Order #ABC123                                           │
│ Status: COMPLETED ✓                                     │
│ Badge: "Refunded" (green badge)                         │
│                                                         │
│ Product: Item Name                                      │
│ Amount: 500 PKR                                         │
│ Delivery Date: May 10, 2026                             │
│                                                         │
│ Buttons:                                                │
│ ├─ "Track Order" (clickable)                            │
│ ├─ "Reorder" (clickable)                                │
│ └─ "Refund Done" (disabled/informational)               │
│                                                         │
│ Refund Details (expandable section):                    │
│ ├─ Refund Amount: 500 PKR                               │
│ ├─ Refund Date: May 13, 2026                            │
│ └─ Status: Completed                                    │
└─────────────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ Order status: **COMPLETED** (NOT cancelled)
- ✅ Badge: **"Refunded"** (green, visible)
- ✅ Buttons: "Track Order" and "Reorder" available
- ✅ "Refund Done" button shown (disabled/informational)
- ✅ Refund details visible with amount and date
- ✅ Order stays in "Completed" tab (not hidden)

**Tab Placement:**
- Appears in: **"Completed"** tab
- NOT in: "Cancelled" tab
- NOT in: "Pending" tab

---

### 2. SELLER'S ORDERS SCREEN

#### Order Card Display

```
┌─────────────────────────────────────────────────────────┐
│ Order #ABC123                                           │
│ Status: COMPLETED ✓                                     │
│ Badge: "Refunded" (green badge)                         │
│                                                         │
│ Buyer: John Doe                                         │
│ Product: Item Name                                      │
│ Amount: 500 PKR                                         │
│ Delivery Date: May 10, 2026                             │
│                                                         │
│ Buttons:                                                │
│ ├─ "View Details" (clickable)                           │
│ ├─ "View Refund" (clickable - if refund exists)         │
│ └─ "Contact Buyer" (clickable)                          │
│                                                         │
│ Refund Status (if refund approved):                     │
│ ├─ Refund Amount: 500 PKR                               │
│ ├─ Refund Date: May 13, 2026                            │
│ └─ Status: Completed                                    │
└─────────────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ Order status: **COMPLETED** (NOT cancelled)
- ✅ Badge: **"Refunded"** (green, visible)
- ✅ "View Refund" button available (if refund exists)
- ✅ Refund details visible
- ✅ Order stays in "Completed" tab
- ✅ Seller can see refund was processed

**Tab Placement:**
- Appears in: **"Completed"** tab
- NOT in: "Cancelled" tab
- NOT in: "Pending" tab

---

### 3. BUYER'S PAYMENT HISTORY SCREEN

#### Payment Entry Display

```
┌─────────────────────────────────────────────────────────┐
│ Payment #PAY123                                         │
│ Status: REFUNDED ✓ (green)                              │
│                                                         │
│ Seller: Store Name                                      │
│ Order: #ABC123                                          │
│ Original Amount: 500 PKR                                │
│ Refund Amount: 500 PKR ✓                                │
│ Refund Date: May 13, 2026 ✓                             │
│ Refund Reason: [Buyer's reason]                         │
│                                                         │
│ Timeline:                                               │
│ ├─ Paid: May 5, 2026                                    │
│ ├─ Delivered: May 10, 2026                              │
│ ├─ Refund Requested: May 12, 2026                       │
│ ├─ Refund Approved: May 13, 2026                        │
│ └─ Refund Completed: May 13, 2026 ✓                     │
│                                                         │
│ Buttons:                                                │
│ ├─ "View Details" (clickable)                           │
│ └─ "View Refund Details" (clickable)                    │
└─────────────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ Payment status: **REFUNDED** (green, final state)
- ✅ Refund amount: **500 PKR** (full amount)
- ✅ Refund date: **May 13, 2026** (completion date)
- ✅ Refund reason: **Visible** (buyer's reason)
- ✅ Complete timeline: **All dates visible**
- ✅ Tab placement: **"Refunded"** tab (if exists) or "Completed" tab

**Tab Placement:**
- Appears in: **"Refunded"** tab (if separate tab exists)
- OR: **"Completed"** tab (if no separate refund tab)
- NOT in: "Pending" tab
- NOT in: "Processing" tab

---

### 4. SELLER'S PAYMENT DETAILS SCREEN

#### Payment Entry Display

```
┌─────────────────────────────────────────────────────────┐
│ Payment #PAY123                                         │
│ Status: REFUNDED ✓ (green)                              │
│                                                         │
│ Buyer: John Doe                                         │
│ Order: #ABC123                                          │
│ Original Amount: 500 PKR                                │
│ Refund Amount: 500 PKR ✓                                │
│ Refund Date: May 13, 2026 ✓                             │
│ Refund Reason: [Buyer's reason]                         │
│                                                         │
│ Payment Timeline:                                       │
│ ├─ Paid: May 5, 2026                                    │
│ ├─ Delivered: May 10, 2026                              │
│ ├─ Refund Requested: May 12, 2026                       │
│ ├─ Refund Approved: May 13, 2026                        │
│ └─ Refund Completed: May 13, 2026 ✓                     │
│                                                         │
│ Seller Earnings:                                        │
│ ├─ Original Commission: [amount]                        │
│ ├─ Refund Deduction: [amount]                           │
│ └─ Net Earnings: [amount]                               │
│                                                         │
│ Buttons:                                                │
│ ├─ "View Details" (clickable)                           │
│ ├─ "View Refund Details" (clickable)                    │
│ └─ "Download Invoice" (clickable)                       │
└─────────────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ Payment status: **REFUNDED** (green, final state)
- ✅ Refund amount: **500 PKR** (full amount)
- ✅ Refund date: **May 13, 2026** (completion date)
- ✅ Seller earnings: **Adjusted for refund**
- ✅ Complete timeline: **All dates visible**
- ✅ Tab placement: **"Refunded"** tab (if exists) or "Completed" tab

**Tab Placement:**
- Appears in: **"Refunded"** tab (if separate tab exists)
- OR: **"Completed"** tab (if no separate refund tab)
- NOT in: "Pending" tab
- NOT in: "Processing" tab

---

### 5. CO-SELLER PAYMENT DETAILS SCREEN

#### Payment Entry Display (if co-seller involved)

```
┌─────────────────────────────────────────────────────────┐
│ Payment #PAY123 (Co-Seller Split)                       │
│ Status: REFUNDED ✓ (green)                              │
│                                                         │
│ Store: Co-Seller Store Name                             │
│ Order: #ABC123                                          │
│ Original Amount: 250 PKR (50% split)                    │
│ Refund Amount: 250 PKR ✓ (50% split)                    │
│ Refund Date: May 13, 2026 ✓                             │
│                                                         │
│ Payment Timeline:                                       │
│ ├─ Paid: May 5, 2026                                    │
│ ├─ Delivered: May 10, 2026                              │
│ ├─ Refund Requested: May 12, 2026                       │
│ ├─ Refund Approved: May 13, 2026                        │
│ └─ Refund Completed: May 13, 2026 ✓                     │
│                                                         │
│ Co-Seller Earnings:                                     │
│ ├─ Original Commission: [amount]                        │
│ ├─ Refund Deduction: [amount]                           │
│ └─ Net Earnings: [amount]                               │
│                                                         │
│ Buttons:                                                │
│ ├─ "View Details" (clickable)                           │
│ └─ "Download Invoice" (clickable)                       │
└─────────────────────────────────────────────────────────┘
```

**Key Points:**
- ✅ Payment status: **REFUNDED** (green, final state)
- ✅ Refund amount: **Proportional split** (e.g., 50%)
- ✅ Refund date: **May 13, 2026** (completion date)
- ✅ Co-seller earnings: **Adjusted for refund**
- ✅ Complete timeline: **All dates visible**
- ✅ Tab placement: **"Refunded"** tab (if exists) or "Completed" tab

**Tab Placement:**
- Appears in: **"Refunded"** tab (if separate tab exists)
- OR: **"Completed"** tab (if no separate refund tab)
- NOT in: "Pending" tab
- NOT in: "Processing" tab

---

## Status Summary Table

| Screen | Order Status | Badge | Payment Status | Visible | Tab |
|--------|--------------|-------|----------------|---------|-----|
| Buyer My Orders | COMPLETED | "Refunded" | N/A | ✅ | Completed |
| Seller Orders | COMPLETED | "Refunded" | N/A | ✅ | Completed |
| Buyer Payment History | N/A | N/A | REFUNDED | ✅ | Refunded/Completed |
| Seller Payment Details | N/A | N/A | REFUNDED | ✅ | Refunded/Completed |
| Co-Seller Payment Details | N/A | N/A | REFUNDED | ✅ | Refunded/Completed |

---

## Key Design Principles

### 1. Order Status Preservation
- ✅ Order remains **COMPLETED** (not cancelled)
- ✅ Refund is a **financial event**, not an order cancellation
- ✅ Order history is **preserved** for buyer reference

### 2. Clear Visual Indication
- ✅ "Refunded" badge: **Green color** (success state)
- ✅ Payment status: **REFUNDED** (final state)
- ✅ Refund details: **Always visible** (amount, date, reason)

### 3. Consistent Across Screens
- ✅ All screens show **same refund status**
- ✅ All screens show **same refund amount**
- ✅ All screens show **same refund date**
- ✅ Real-time sync ensures **consistency**

### 4. User Experience
- ✅ Buyers see: Order completed + refund processed
- ✅ Sellers see: Order completed + refund deducted
- ✅ Co-sellers see: Order completed + refund split deducted
- ✅ No confusion about order status

---

## Data Fields Required

### Order Document
```
{
  id: "ABC123",
  status: "COMPLETED",           // NOT "CANCELLED"
  is_refunded: true,             // NEW: Refund marker
  refund_amount: 500,            // NEW: Refund amount
  refund_date: 1715587200000,    // NEW: Refund completion date
  refund_reason: "...",          // NEW: Buyer's reason
  updated_at: 1715587200000
}
```

### Payment Document
```
{
  id: "PAY123",
  status: "REFUNDED",            // Final state
  refund_amount: 500,            // Refund amount
  refund_date: 1715587200000,    // Refund completion date
  refund_reason: "...",          // Buyer's reason
  updated_at: 1715587200000
}
```

### Refund Document
```
{
  id: "REF123",
  status: "COMPLETED",           // Final state
  refund_amount: 500,
  completed_at: 1715587200000,
  is_refunded: true
}
```

---

## Implementation Checklist

- [x] Order status: COMPLETED (not cancelled)
- [x] Order has `is_refunded: true` flag
- [x] Payment status: REFUNDED
- [x] Refund amount: Visible on all screens
- [x] Refund date: Visible on all screens
- [x] Refund reason: Visible on all screens
- [x] "Refunded" badge: Green, visible on order cards
- [x] Order stays in "Completed" tab
- [x] Payment appears in "Refunded" tab
- [x] Real-time sync: All screens update together
- [x] Co-seller split: Proportional refund deduction

---

## Testing Scenarios

### Scenario 1: Full Refund
1. Create order → Deliver → Request refund → Approve
2. ✅ Buyer My Orders: COMPLETED + "Refunded" badge
3. ✅ Seller Orders: COMPLETED + "Refunded" badge
4. ✅ Buyer Payment History: REFUNDED status
5. ✅ Seller Payment Details: REFUNDED status

### Scenario 2: Partial Refund (if supported)
1. Create order → Deliver → Request partial refund → Approve
2. ✅ Buyer My Orders: COMPLETED + "Partially Refunded" badge
3. ✅ Seller Orders: COMPLETED + "Partially Refunded" badge
4. ✅ Buyer Payment History: Shows partial refund amount
5. ✅ Seller Payment Details: Shows partial refund deduction

### Scenario 3: Co-Seller Order
1. Create order with co-seller → Deliver → Request refund → Approve
2. ✅ Buyer My Orders: COMPLETED + "Refunded" badge
3. ✅ Seller Orders: COMPLETED + "Refunded" badge
4. ✅ Co-Seller Orders: COMPLETED + "Refunded" badge
5. ✅ Buyer Payment History: REFUNDED status
6. ✅ Seller Payment Details: REFUNDED status (full deduction)
7. ✅ Co-Seller Payment Details: REFUNDED status (split deduction)

---

## Status Transitions

```
Order Lifecycle:
PENDING → PROCESSING → SHIPPED → DELIVERED → COMPLETED
                                                    ↓
                                            (if refund approved)
                                                    ↓
                                            COMPLETED + is_refunded=true

Payment Lifecycle:
PENDING → COMPLETED
              ↓
        (if refund requested)
              ↓
        REFUND_PENDING
              ↓
        (if refund approved)
              ↓
        REFUND_PROCESSING
              ↓
        (auto-complete)
              ↓
        REFUNDED (final state)
```

---

## Summary

**For a completed order with approved refund:**

| Aspect | Status |
|--------|--------|
| Order Status | COMPLETED ✓ |
| Order Visibility | Stays in "Completed" tab ✓ |
| Order Badge | "Refunded" (green) ✓ |
| Payment Status | REFUNDED ✓ |
| Refund Amount | Visible ✓ |
| Refund Date | Visible ✓ |
| Refund Reason | Visible ✓ |
| Buyer Experience | Clear refund completion ✓ |
| Seller Experience | Clear refund deduction ✓ |
| Co-Seller Experience | Clear refund split deduction ✓ |

---

**Status:** ✅ SPECIFICATION COMPLETE  
**Ready for Implementation:** YES  
**Ready for Testing:** YES
