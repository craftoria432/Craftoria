# Craftoria Payment & Refund System - Integration Testing Guide
## Complete Test Cases & Validation Procedures

---

## TABLE OF CONTENTS
1. [Test Environment Setup](#test-environment-setup)
2. [Critical Path Tests](#critical-path-tests)
3. [Edge Case Tests](#edge-case-tests)
4. [Co-Seller Payment Tests](#co-seller-payment-tests)
5. [Security Tests](#security-tests)
6. [Data Integrity Tests](#data-integrity-tests)

---

## TEST ENVIRONMENT SETUP

### Prerequisites
- Test Firebase project with staging/development database
- Test users: 1 buyer, 2 sellers, 1 admin, 1 co-seller store with 2 members
- Test products: 3 items from different sellers, 2 items from co-seller store
- Test database clean: No production orders

### Test Data Creation

```sql
-- Firestore Test Users
users/{buyerUID}: { name: "Test Buyer", role: "BUYER", email: "buyer@test.com" }
users/{seller1UID}: { name: "Seller One", role: "SELLER", email: "seller1@test.com" }
users/{seller2UID}: { name: "Seller Two", role: "SELLER", email: "seller2@test.com" }
users/{adminUID}: { name: "Admin", role: "ADMIN", email: "admin@test.com" }
users/{cosellerMemberUID}: { name: "CoSeller Member", role: "CO_SELLER_MEMBER" }

-- Test Products
products/{product1}: { seller_id: seller1UID, title: "Item 1", price: 100, stock: 10 }
products/{product2}: { seller_id: seller2UID, title: "Item 2", price: 200, stock: 10 }
products/{product3}: { seller_id: seller1UID, title: "Item 3", price: 50, stock: 10 }

-- Test Co-Seller Store
co_seller_stores/{storeID}: { 
    owner_id: seller1UID, 
    store_name: "Test Store",
    member_ids: [seller1UID, cosellerMemberUID],
    created_at: now()
}
```

---

## CRITICAL PATH TESTS

### TEST 1: BUYER → SELLER PAYMENT FLOW (Happy Path)

**Objective**: Verify complete payment journey from order placement to seller receiving payment.

**Steps**:
1. Buyer creates cart with Product 1 (100 PKR)
2. Buyer proceeds to checkout
3. Buyer selects "Cash on Delivery"
4. Buyer enters shipping address
5. Buyer confirms order
6. **Verification Point 1**: Order created in Firestore
   ```
   orders/{orderID}: {
     buyer_id: buyerUID,
     seller_id: seller1UID,
     items: [{ product_id, quantity, price }],
     status: "new",
     total_price: 100,
     created_at: timestamp
   }
   ```
7. **Verification Point 2**: Payment created with commission deducted
   ```
   payments/{paymentID}: {
     seller_id: seller1UID,
     buyer_id: buyerUID,
     amount: 95,  // 100 - 5% commission (5)
     status: "pending",
     created_at: timestamp
   }
   ```
8. **Verification Point 3**: Admin commission recorded
   ```
   admin_commissions/{commissionID}: {
     order_id: orderID,
     payment_id: paymentID,
     subtotal: 100,
     commission_rate: 0.05,
     commission_amount: 5.0,
     seller_payout: 95.0
   }
   ```
9. Seller confirms order received
10. Seller marks order as "Shipped"
11. **Verification Point 4**: Order status updated
    ```
    orders/{orderID}/status: "shipped"
    ```
12. Seller marks order as "Delivered"
13. **Verification Point 5**: Payment marked as completed
    ```
    payments/{paymentID}/status: "completed"
    payments/{paymentID}/payment_date: timestamp (now)
    ```
14. **Verification Point 6**: Seller sees payment in "Payments" screen
    - Opens Payments screen
    - Verifies payment shows: "100 PKR" with commission breakdown
    - Status shows: "Completed"
    - Can click to see order details

**Expected Result**: ✅ PASS
- Payment flows from pending → completed
- Commission correctly deducted (5% from 100 = 95)
- All statuses synchronized between order and payment
- Seller can view payment with full details

---

### TEST 2: BUYER-INITIATED REFUND (Complete Flow)

**Objective**: Verify buyer can request refund, seller approves, refund completes.

**Prerequisite**: Complete TEST 1 first (order delivered, payment completed)

**Steps**:
1. Buyer opens "My Orders" screen
2. Buyer finds delivered order
3. Buyer clicks "Request Refund" button
4. Refund request form appears
   - Refund type: Full (100 PKR)
   - Reason: "Product not as expected"
   - Details: "Wrong color received"
5. Buyer confirms refund request
6. **Verification Point 1**: Refund created in Firestore
   ```
   refunds/{refundID}: {
     order_id: orderID,
     payment_id: paymentID,
     buyer_id: buyerUID,
     seller_id: seller1UID,
     refund_type: "FULL",
     original_amount: 100,
     refund_amount: 100,
     status: "REQUESTED",
     initiated_by: "buyer",
     requested_at: timestamp,
     audit_trail: [{
       action: "requested",
       actor: buyerUID,
       actor_name: "Test Buyer",
       timestamp: timestamp
     }]
   }
   ```
7. **Verification Point 2**: Payment status changes to REFUND_PENDING
   ```
   payments/{paymentID}/status: "refund_pending"
   ```
8. **Verification Point 3**: Order refund_status changes to REQUESTED
   ```
   orders/{orderID}/refund_status: "requested"
   ```
9. **Verification Point 4**: Seller receives notification
   - Log in as seller1
   - Open notifications
   - See: "Refund Request Received: Buyer requested refund of 100 PKR"
   - Notification includes payment details
10. Seller opens "Refund Management" screen
11. Seller sees pending refund request
12. Seller clicks "Approve Refund"
13. **Verification Point 5**: Refund status becomes APPROVED_BY_SELLER
    ```
    refunds/{refundID}/status: "approved_by_seller"
    refunds/{refundID}/approved_at: timestamp
    refunds/{refundID}/audit_trail: [..., {
      action: "approved",
      actor: seller1UID,
      timestamp: timestamp
    }]
    ```
14. **Verification Point 6**: Refund auto-completes (COD flow)
    ```
    refunds/{refundID}/status: "completed"
    refunds/{refundID}/completed_at: timestamp
    payments/{paymentID}/status: "refunded"
    payments/{paymentID}/refund_amount: 100
    payments/{paymentID}/refund_date: timestamp
    orders/{orderID}/refund_status: "completed"
    ```
15. **Verification Point 7**: Buyer receives approval notification
    - Log in as buyer
    - See: "Refund Approved: Your refund of 100 PKR has been processed"
16. **Verification Point 8**: Order removed from "Completed" tab
    - Buyer's "My Orders" → "Completed" tab
    - Order NOT listed (moved to refunded)
17. **Verification Point 9**: Order shows "Refunded" badge
    - Find order in appropriate tab
    - Badge shows: "Refunded"
    - Clicking shows refund timeline with dates

**Expected Result**: ✅ PASS
- Refund progresses through all states correctly
- Seller notified of buyer request
- Buyer notified of approval
- Order removed from completed orders
- Payment status fully synchronized
- Audit trail has 3+ entries

---

### TEST 3: SELLER-INITIATED REFUND (Fraud Prevention)

**Objective**: Verify seller cannot approve own refund; admin must approve.

**Prerequisite**: TEST 1 completed (order delivered, payment completed)

**Steps**:
1. Log in as Seller One
2. Open Payments screen
3. Find the completed payment
4. Click payment details
5. Click "Request Refund" (seller's option to initiate refund)
6. **Verification Point 1**: Refund created with initiated_by="seller"
   ```
   refunds/{refundID}/initiated_by: "seller"
   ```
7. **Verification Point 2**: Admin receives notification (NOT seller)
   - Log in as admin
   - Check admin-only notification queue
   - See: "Seller-Initiated Refund: Seller One requests refund review"
8. **Verification Point 3**: Seller CANNOT approve own refund
   - Seller sees: "Awaiting Admin Review" (no approval button)
9. **Verification Point 4**: Admin receives approval UI
   - Admin dashboard shows pending seller-initiated refunds
   - Admin clicks "Review"
   - Admin clicks "Approve"
10. **Verification Point 5**: Refund auto-completes upon admin approval
    ```
    refunds/{refundID}/status: "completed"
    refunds/{refundID}/approved_by: adminUID
    payments/{paymentID}/status: "refunded"
    ```

**Expected Result**: ✅ PASS
- Seller cannot approve own refund
- Admin gated all seller-initiated refunds
- Fraud prevention works

---

## EDGE CASE TESTS

### TEST 4: REFUND OUTSIDE 30-DAY WINDOW

**Objective**: Verify refund rejected after 30 days from delivery.

**Setup**: Manually set order delivery_at to 35 days ago

```firestore
orders/{oldOrderID}: {
  delivered_at: 35 days ago (in milliseconds),
  status: "completed"
}
```

**Steps**:
1. Buyer tries to request refund for 35-day-old order
2. Click "Request Refund"
3. **Expected Error**: "Refund window expired. Orders can only be refunded within 30 days of delivery."
4. **Verification**: Refund request not created
   - Query: `db.collection("refunds").where("order_id", "==", oldOrderID)`
   - Result: Empty (no refund created)

**Expected Result**: ✅ PASS
- Refund rejected with clear message
- No refund record created

---

### TEST 5: PARTIAL REFUND CALCULATION

**Objective**: Verify partial refunds calculated correctly.

**Prerequisite**: TEST 1 completed with 100 PKR order

**Steps**:
1. Buyer requests partial refund: 60 PKR
2. Refund form shows:
   - Original amount: 100 PKR
   - Refund amount: 60 PKR
   - Remaining: 40 PKR
3. Buyer confirms
4. **Verification Point 1**: Partial refund created
   ```
   refunds/{refundID}/refund_amount: 60
   ```
5. Seller approves
6. **Verification Point 2**: Payment records partial refund
   ```
   payments/{paymentID}/refund_amount: 60
   orders/{orderID}/refund_status: "completed"
   ```
7. **Verification Point 3**: Next refund prevented
   - Buyer tries to request 2nd refund for same order
   - **Expected**: "Refund already in progress for this order"

**Expected Result**: ✅ PASS
- Partial refund processed correctly
- Prevents duplicate refund requests

---

### TEST 6: DUPLICATE REFUND PREVENTION

**Objective**: Verify refund idempotency - same request creates same refund.

**Steps**:
1. Buyer requests refund
2. Network error (simulated)
3. Buyer clicks "Request Refund" again (idempotency key same)
4. **Expected**: Same refund document returned (not duplicate)
   ```
   refunds/{refundID1} = refunds/{refundID2}  // Same ID
   ```
5. **Verification**: Only one refund exists
   ```
   db.collection("refunds").where("order_id", "==", orderID).get().length === 1
   ```

**Expected Result**: ✅ PASS
- Idempotent refund creation
- No duplicates on retry

---

## CO-SELLER PAYMENT TESTS

### TEST 7: CO-SELLER FAIR PAYMENT SPLIT

**Objective**: Verify payment split proportional to each member's sales.

**Setup**:
- Create order with items from 2 members of Test Store:
  - Member A (seller1): Item worth 600 PKR (60%)
  - Member B (cosellerMember): Item worth 400 PKR (40%)
  - Total: 1000 PKR
  - Commission (5%): 50 PKR
  - Seller payout: 950 PKR

**Expected Split**:
- Member A: (600/1000) × 950 = 570 PKR
- Member B: (400/1000) × 950 = 380 PKR
- Total: 950 PKR ✅

**Steps**:
1. Buyer creates order with both members' products
2. Order placed, payment splits created
3. **Verification Point 1**: Payment created
   ```
   payments/{paymentID}: {
     co_seller_store_id: storeID,
     amount: 950,  // After commission
     payment_splits: [
       {
         seller_id: seller1UID,
         seller_name: "Seller One",
         split_percentage: 60,
         split_amount: 570
       },
       {
         seller_id: cosellerMemberUID,
         seller_name: "CoSeller Member",
         split_percentage: 40,
         split_amount: 380
       }
     ]
   }
   ```
4. **Verification Point 2**: Store owner sees payment
   - Log in as seller1 (store owner)
   - Open store payments
   - See: 950 PKR with split breakdown
5. **Verification Point 3**: Member B sees their split
   - Log in as cosellerMember
   - Open store payments
   - See: 380 PKR for their share
6. **Verification Point 4**: Math verified
   - 570 + 380 = 950 ✅
   - No rounding loss ✅

**Expected Result**: ✅ PASS
- Fair product-based split applied
- No rounding errors
- Both members see correct amounts

---

### TEST 8: CO-SELLER REFUND SPLIT

**Objective**: Verify refund splits apply to partial refunds.

**Prerequisite**: TEST 7 payment created (950 split between 60/40)

**Steps**:
1. Buyer requests partial refund: 500 PKR (approx 52.6% of 950)
2. **Verification Point 1**: Refund splits calculated
   ```
   refunds/{refundID}: {
     refund_amount: 500,
     refund_splits: [
       {
         seller_id: seller1UID,
         refund_amount: 300  // 60% of 500
       },
       {
         seller_id: cosellerMemberUID,
         refund_amount: 200  // 40% of 500
       }
     ]
   }
   ```
3. **Verification Point 2**: Sum verified
   - 300 + 200 = 500 ✅
   - No rounding loss ✅
4. Seller/admin approves
5. **Verification Point 3**: Payment updated
   ```
   payments/{paymentID}: {
     refund_amount: 500,
     status: "refunded"
   }
   ```

**Expected Result**: ✅ PASS
- Refund splits calculated proportionally
- No rounding errors
- Correct split amounts

---

### TEST 9: CO-SELLER ACCESS CONTROL

**Objective**: Verify non-members cannot access store payments.

**Steps**:
1. Create User X (not member of Test Store)
2. User X attempts to query store payments via API
   ```
   CoSellerStorePaymentRepository.loadStorePayments(
     storeId=storeID,
     currentUserId=userXUID,
     storeMemberIds=[],  // Empty = not a member
     storeOwnerId=seller1UID
   )
   ```
3. **Expected**: SecurityException with message "Access denied"
4. **Verification**: Firestore rules reject query
   ```
   Error: User X does not have permission to query this store
   ```
5. Authorized member (seller1) queries same store
   ```
   loadStorePayments(..., currentUserId=seller1UID, storeMemberIds=[seller1, cosellerMember])
   ```
6. **Expected**: SUCCESS - returns payments

**Expected Result**: ✅ PASS
- Unauthorized access rejected
- Authorized members can access

---

## SECURITY TESTS

### TEST 10: PAYMENT AMOUNT PRECISION

**Objective**: Verify financial calculations use BigDecimal, no loss.

**Steps**:
1. Create 100 orders with various amounts:
   ```
   Orders: 123.45, 234.56, 345.67, 456.78, 567.89, ...
   ```
2. Calculate total expected: 56,000.00 PKR (example)
3. Sum all payments via API: 56,000.00 PKR
4. **Verification**: No precision loss
   ```
   Expected Total === Actual Total
   (no 0.01, 0.02 discrepancies)
   ```
5. Commission calculations:
   ```
   Each order commissionAmount should be exact to 2 decimals
   No floating-point errors like 5.0000000001
   ```

**Expected Result**: ✅ PASS
- All amounts exact to 2 decimals
- No floating-point errors
- Financial reconciliation perfect

---

### TEST 11: REFUND AUDIT TRAIL

**Objective**: Verify complete audit trail for compliance.

**Steps**:
1. Create refund and progress through all states
2. **Expected audit trail**:
   ```
   audit_trail: [
     { action: "requested", actor: buyer, timestamp: T1 },
     { action: "approved", actor: seller, timestamp: T2 },
     { action: "completed", actor: system, timestamp: T3 }
   ]
   ```
3. **Verification**: No actions can be missing
   - Each state transition recorded
   - Actor ID captured
   - Timestamp accurate

**Expected Result**: ✅ PASS
- Complete audit trail
- No missing entries
- Compliance ready

---

## DATA INTEGRITY TESTS

### TEST 12: ORDER-PAYMENT-REFUND SYNCHRONIZATION

**Objective**: Verify all three collections stay synchronized.

**States Tested**:
1. Order: NEW → Payment: PENDING
2. Order: DELIVERED → Payment: COMPLETED
3. Order: (REFUND_STATUS=REQUESTED) → Payment: REFUND_PENDING
4. Order: (REFUND_STATUS=COMPLETED) → Payment: REFUNDED

**Steps**:
1. Track order through full lifecycle
2. At each state, verify:
   - Order status ✓
   - Payment status ✓
   - Refund status (if applicable) ✓
   - Timestamps match ✓

**Verification Script**:
```javascript
async function verifySync(orderId, paymentId, refundId) {
  const order = await getOrder(orderId);
  const payment = await getPayment(paymentId);
  const refund = refundId ? await getRefund(refundId) : null;
  
  console.assert(order.status, "Order missing status");
  console.assert(payment.status, "Payment missing status");
  console.assert(!payment.status.includes("refund") || order.refund_status, 
    "Order missing refund_status when payment has refund");
  console.assert(payment.refund_status === order.refund_status || 
    !payment.refund_status, "Refund status mismatch");
}
```

**Expected Result**: ✅ PASS
- All collections synchronized
- No stale data
- Consistency maintained

---

## TEST EXECUTION CHECKLIST

- [ ] TEST 1: Basic buyer→seller payment
- [ ] TEST 2: Buyer-initiated refund
- [ ] TEST 3: Seller-initiated refund
- [ ] TEST 4: Refund outside window
- [ ] TEST 5: Partial refund
- [ ] TEST 6: Duplicate prevention
- [ ] TEST 7: Co-seller fair split
- [ ] TEST 8: Co-seller refund split
- [ ] TEST 9: Access control
- [ ] TEST 10: Amount precision
- [ ] TEST 11: Audit trail
- [ ] TEST 12: Synchronization

---

## MONITORING & ROLLBACK

### Post-Deployment Monitoring (24 hours)

**Alerts to Watch**:
- ❌ Payment deserialization errors (crash on parsePayment)
- ❌ Refund status mismatch (payment vs order)
- ❌ Amount precision loss (1 paisa errors)
- ❌ Unauthorized payment access (security)
- ❌ Refund stuck in REQUESTED (no seller notification)

### Rollback Plan

If critical issue detected:
1. Stop all payment processing
2. Revert code to previous version
3. Verify payment/refund queries work
4. Notify users of temporary maintenance
5. Root cause analysis
6. Re-fix and re-deploy

---

## SUCCESS CRITERIA

✅ **Production Ready** when:
- All 12 tests PASS
- No precision loss (BigDecimal used)
- All statuses synchronized (order + payment + refund)
- Security checks enforced (access control, audit trail)
- Notifications working (seller, admin, buyer)
- <0.1% error rate observed in first 24h

---

## SIGN-OFF

**Test Date**: _____________  
**Tester**: _____________  
**Status**: ⬜ PENDING / 🟡 IN PROGRESS / 🟢 PASSED / 🔴 FAILED  
**Critical Issues Found**: _____________  
**Ready for Production**: YES / NO  

