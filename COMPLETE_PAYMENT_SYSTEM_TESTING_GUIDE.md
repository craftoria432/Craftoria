# Complete Payment & Refund System Testing Guide

Comprehensive testing steps to verify buyer payments, seller payments, refunds, co-seller payments, and payment splits.

---

## TABLE OF CONTENTS
1. [Setup & Prerequisites](#setup--prerequisites)
2. [Buyer Payments Testing](#buyer-payments-testing)
3. [Seller Payments Testing](#seller-payments-testing)
4. [Refund System Testing](#refund-system-testing)
5. [Co-Seller Payments Testing](#co-seller-payments-testing)
6. [Co-Seller Payment Split Testing](#co-seller-payment-split-testing)
7. [Firebase Data Verification](#firebase-data-verification)
8. [Common Issues & Debug](#common-issues--debug)

---

## SETUP & PREREQUISITES

### Test Accounts Required
1. **Buyer Account** - For purchasing products
2. **Seller Account** - For selling single products
3. **Co-Seller Account** - For store owner
4. **Co-Seller Member** - For store member

### Test Data Setup
```
Create test products:
- Regular product (Seller): Price ₹500
- Co-seller store products: Price ₹1000 (multiple products)
- Add inventory to all products
```

### Launch App in Debug Mode
```bash
# Terminal
cd app
./gradlew installDebug

# Or in Android Studio: Run > Debug 'app'
```

---

## BUYER PAYMENTS TESTING

### Test 1: Single Order - Single Seller Payment
**Steps:**
1. Login as Buyer
2. Add product from regular seller to cart
3. Navigate to Checkout
4. Enter delivery address
5. Proceed to payment
6. Complete payment (use test card)
7. Verify Order Success Screen shows

**Expected Results:**
```
UI Verification:
✓ Order Success Screen displays
✓ Order ID shown
✓ Seller name and items displayed
✓ Total amount matches
✓ No "Payment Reappearing" issue

Data Verification (Firebase):
✓ Payment document created in /payments/{buyerId}
✓ Payment status = "Completed"
✓ paymentItems.count = 1
✓ paymentItems[0].amount = ₹500
✓ sellerID is populated (not null)
✓ timestamp is current
```

**Debug Steps:**
1. Open Firebase Console → Firestore
2. Navigate to `payments` collection
3. Find document with your buyerId
4. Check payment status and seller info
5. Verify no duplicate payments exist

---

### Test 2: Multi-Order - Multiple Sellers Payment
**Steps:**
1. Login as Buyer
2. Add products from 3 different sellers (₹300, ₹200, ₹500)
3. Go to Checkout
4. Complete payment
5. Verify Order Success Screen

**Expected Results:**
```
UI Verification:
✓ Shows all 3 seller names
✓ Total = ₹1000
✓ No duplicate orders displayed

Data Verification (Firebase):
✓ Single payment document created
✓ paymentItems.count = 3
✓ Each item has correct seller ID
✓ Sum of amounts = ₹1000
✓ Payment status = "Completed"
```

**Potential Issues to Check:**
- Data overlap (duplicate payment display)
- Missing seller IDs
- Incorrect total amount
- Multiple payment documents for single checkout

---

### Test 3: Payment History Screen - Data Display
**Steps:**
1. Login as Buyer
2. Navigate to "Payment History"
3. Verify all tabs visible (All, Pending, Completed)
4. Check filter tabs consistency

**Expected Results:**
```
✓ All tab shows all payments
✓ Completed tab shows only completed payments
✓ Pending tab shows only pending payments
✓ Tab filter always visible (not dynamic)
✓ Empty state professional (icon + text)
✓ No count badges on tabs

Data Verification:
✓ Payment items display correctly
✓ Seller names match Firebase data
✓ Amounts are accurate
✓ Timestamps display correctly
```

---

### Test 4: Payment History Realtime Updates
**Steps:**
1. Open Payment History on Device A (Buyer)
2. On Device B/Web, process refund for one of buyer's payments
3. Check Device A - Payment status should update

**Expected Results:**
```
✓ Payment status updates realtime
✓ Refund badge appears (if applicable)
✓ No page reload needed
✓ Smooth transition
```

---

## SELLER PAYMENTS TESTING

### Test 1: Seller Payment Screen - Basic Display
**Steps:**
1. Login as Seller
2. Complete order from buyer (ensure order delivered)
3. Navigate to Seller Payments screen
4. Verify payment appears

**Expected Results:**
```
UI Verification:
✓ All filter tabs visible (All, Completed, Pending)
✓ Tab filter always visible
✓ No count badges on tabs
✓ Payment displays in correct status
✓ Empty state professional

Data Verification (Firebase):
✓ Payment document in /payments/{sellerId}
✓ Status matches tab filter
✓ Buyer name populated (from order)
✓ Amount correct
✓ Timestamp current
```

**Debug Steps:**
```
Firebase Console:
1. Go to payments collection
2. Filter by sellerId
3. Verify document structure:
   {
     id: "payment_id",
     sellerId: "seller_id",
     buyerId: "buyer_id",
     amount: 500,
     status: "Completed",
     paymentItems: [{...}],
     timestamp: current,
     sellerName: "seller_name"
   }
```

---

### Test 2: Seller Payment Detail Screen
**Steps:**
1. Login as Seller
2. Open Seller Payments
3. Click on any payment
4. Verify Payment Detail Screen loads

**Expected Results:**
```
UI Verification:
✓ Buyer name displayed
✓ Order date shown
✓ Amount breakdown clear
✓ Status displayed
✓ Items listed with quantities

Data Verification:
✓ All payment details match Firebase
✓ Order items match original order
✓ No data missing or corrupted
```

---

### Test 3: Seller Dashboard - Pending Refunds Count
**Steps:**
1. Login as Seller
2. Go to Dashboard
3. Verify "Refund Requests" card text

**Expected Results:**
```
Text Verification:
✓ If count = 1: Shows "1 Pending Action" (singular)
✓ If count > 1: Shows "X Pending Actions" (plural)
✓ If count = 0: Shows "No pending requests"
✓ Professional icon displayed
✓ Icon tinted appropriately
```

---

### Test 4: Seller Payment Realtime Updates
**Steps:**
1. Keep Seller Payment screen open
2. Process a new order from buyer (admin process)
3. Observe Seller Payment screen

**Expected Results:**
```
✓ New payment appears instantly
✓ Status updates realtime
✓ No page refresh needed
✓ Payments don't disappear
✓ Count badges never appear on tabs
```

---

## REFUND SYSTEM TESTING

### Test 1: Buyer Request Refund - Basic Flow
**Steps:**
1. Login as Buyer
2. Go to My Orders
3. Find a Completed order
4. Click "Request Refund" button
5. Fill refund form with reason
6. Submit request

**Expected Results:**
```
UI Verification:
✓ Refund Request Screen loads
✓ Order details pre-populated
✓ Refund form fields present
✓ Submit button functional
✓ Success message shows

Data Verification (Firebase):
✓ Refund document created in /refunds
✓ Status = "Pending"
✓ buyerId populated
✓ sellerId populated
✓ orderId linked correctly
✓ Timestamp current
```

---

### Test 2: Seller Refund Management - Pending Tab
**Steps:**
1. Login as Seller
2. Go to Refund Management
3. Check "Pending" tab

**Expected Results:**
```
UI Verification:
✓ Pending filter tab always visible
✓ NO count badge on Pending tab
✓ Shows only pending refunds
✓ Empty state professional

Text Verification (from dashboard/screen):
✓ If pending = 1: "1 Pending Action"
✓ If pending > 1: "X Pending Actions"
✓ If pending = 0: "No pending requests"

Data Verification:
✓ Correct refunds filtered
✓ All fields populated
```

---

### Test 3: Seller Approve/Reject Refund
**Steps:**
1. Login as Seller
2. Go to Refund Management → Pending
3. Click on a pending refund
4. Click "Approve" or "Reject"
5. Add optional note

**Expected Results:**
```
UI Verification:
✓ Action completes successfully
✓ Toast/snackbar confirmation
✓ Screen updates

Data Verification (Firebase):
✓ Refund status changed to "Approved" or "Rejected"
✓ Updated timestamp added
✓ Seller note saved (if provided)
✓ Related payment status updated if approved
```

---

### Test 4: Refund Notification to Buyer
**Steps:**
1. Approve refund as Seller (from above test)
2. Switch to Buyer account
3. Check Notifications screen

**Expected Results:**
```
✓ Refund notification appears
✓ Shows seller name
✓ Status matches (Approved/Rejected)
✓ Click notification navigates to Refund Details
```

---

### Test 5: Refund Details Screen - Buyer View
**Steps:**
1. Login as Buyer
2. Go to Payment History
3. Find payment with refund
4. Click to see details

**Expected Results:**
```
UI Verification:
✓ Refund status shown (Approved/Rejected/Pending)
✓ Refund reason visible
✓ Seller response visible
✓ Timeline shows events

Data Verification:
✓ All refund info matches Firebase
✓ Timestamps accurate
✓ Status consistent
```

---

### Test 6: Multiple Refund Requests (Limit Enforcement)
**Steps:**
1. Login as Buyer
2. Try to request refund on same order twice
3. Verify limit enforcement

**Expected Results:**
```
✓ Cannot create duplicate refund requests
✓ Error message shown: "Already requested refund"
✓ Button disabled after first request
✓ Data integrity maintained
```

---

## CO-SELLER PAYMENTS TESTING

### Test 1: Co-Seller Store Setup & Member Order
**Steps:**
1. Login as Seller (Store Owner)
2. Create Co-Seller Store
3. Add Co-Seller Member
4. Invite member to store
5. Co-Seller accepts invitation

**Expected Results:**
```
✓ Co-seller store created
✓ Member added successfully
✓ Invitation sent and accepted
✓ Member count updates correctly
```

---

### Test 2: Co-Seller Payment Display
**Steps:**
1. Login as Co-Seller Member
2. Go to Store Payment screen (or dedicated co-seller payment screen)
3. Verify payments display

**Expected Results:**
```
UI Verification:
✓ Payments for co-seller store displayed
✓ Store name in payment label
✓ Filter tabs visible and working
✓ Empty state professional

Data Verification (Firebase):
✓ Payment document has storeId
✓ Status correct
✓ Amount correct (should be member's split, not full)
```

---

### Test 3: Co-Seller Refund Management
**Steps:**
1. Login as Co-Seller Member
2. Go to Refund Management
3. Process refund for co-seller store order

**Expected Results:**
```
✓ Only co-seller store refunds shown
✓ Cannot access store owner's refunds
✓ Refund processing works correctly
✓ Amounts reflect member's portion
```

---

## CO-SELLER PAYMENT SPLIT TESTING

### Test 1: Payment Split - Fair Product-Based Split
**Steps:**
1. Create Co-Seller Store with 2 members
2. Add 3 products:
   - Product A: ₹300 (Member 1 = 100%)
   - Product B: ₹200 (Member 2 = 100%)
   - Product C: ₹500 (Member 1 = 60%, Member 2 = 40%)
3. Buyer purchases all 3
4. Complete order

**Expected Results:**
```
Data Verification (Firebase):

Payment created in payments/{buyerId}:
✓ Total amount = ₹1000
✓ paymentItems count = 3

Split payments created in payments/{storeId}:

Member 1 payment:
✓ Amount = 300 + (500 × 0.6) + fee = ~380
✓ storeId = store_id
✓ memberId = member1_id
✓ productBreakdown accurate

Member 2 payment:
✓ Amount = 200 + (500 × 0.4) + fee = ~420
✓ storeId = store_id
✓ memberId = member2_id
✓ productBreakdown accurate

✓ No overlap in product assignment
✓ All amounts sum to buyer payment
✓ Commission deducted correctly
```

---

### Test 2: Payment Split - Store Owner Commission
**Steps:**
1. Setup co-seller store with commission config
2. Process order (same as Test 1)
3. Verify store owner receives commission

**Expected Results:**
```
Data Verification:

Store Owner Payment:
✓ Receives commission on member payments
✓ Commission = (member_payment × commission_rate)
✓ Document in payments/{storeOwnerId}
✓ storeBenefitAmount = total commission

Example:
Member 1 pays: ₹380 (before member fee)
Commission 10%: ₹38
Store Owner receives: ₹38
```

---

### Test 3: Payment Split - Order Detail Shows Store Badge
**Steps:**
1. Complete co-seller order
2. Go to My Orders (as Buyer)
3. Click on co-seller order

**Expected Results:**
```
UI Verification:
✓ Store badge/label displayed
✓ Shows "Store Name" with store indicator
✓ Visual distinction from regular seller orders
✓ Store name clickable/navigable
```

---

### Test 4: Legacy Order Retroactive Split
**Steps:**
1. Create order before payment split system implemented
2. Verify system handles legacy data

**Expected Results:**
```
✓ Legacy order processed correctly
✓ Split calculated retroactively
✓ No data corruption
✓ All parties receive correct amounts
```

---

### Test 5: Co-Seller Member Removal & Payment Cascade
**Steps:**
1. Setup co-seller store with 2 members
2. Process order with shared products
3. Remove one member
4. Verify payment handling

**Expected Results:**
```
✓ Removed member payment cleared
✓ Remaining member gets full product allocation
✓ No data orphaned
✓ Store owner receives updated commission
```

---

## FIREBASE DATA VERIFICATION

### Payment Document Structure Check

**Location:** `Firestore > payments > {userId}`

```javascript
// Buyer Payment Document
{
  id: "payment_xxxxx",
  buyerId: "buyer_id",
  sellerId: null, // null for multi-seller
  amount: 1000,
  status: "Completed",
  paymentMethod: "card",
  timestamp: Timestamp,
  paymentItems: [
    {
      productId: "prod_1",
      sellerId: "seller_1",
      sellerName: "Seller Name",
      amount: 300,
      quantity: 1,
      itemsCount: 1
    }
  ],
  // For co-seller
  storeId: null, // null unless co-seller payment
  storeName: null,
  memberId: null,
  storeBenefitAmount: 0
}

// Seller Payment Document (from orders)
{
  id: "payment_xxxxx",
  sellerId: "seller_id",
  buyerId: "buyer_id",
  buyerName: "Buyer Name",
  amount: 300,
  status: "Completed",
  orderId: "order_id",
  timestamp: Timestamp
}

// Co-Seller Payment Document
{
  id: "payment_xxxxx",
  storeId: "store_id",
  storeName: "Store Name",
  memberId: "member_id",
  amount: 380,
  status: "Completed",
  paymentItems: [...],
  storeBenefitAmount: 38, // Commission
  timestamp: Timestamp
}
```

### Refund Document Structure Check

```javascript
{
  id: "refund_xxxxx",
  orderId: "order_id",
  buyerId: "buyer_id",
  sellerId: "seller_id",
  reason: "Product damaged",
  status: "Pending|Approved|Rejected",
  amount: 500,
  createdAt: Timestamp,
  updatedAt: Timestamp,
  sellerResponse: "Refund approved",
  approvedDate: Timestamp
}
```

---

## COMMON ISSUES & DEBUG

### Issue 1: Payment Reappearing After Deletion

**Symptoms:**
```
- Deleted payment from Firebase reappears in app
- Same payment ID but different ID in Firebase
- Auto-generated sample data shows
```

**Root Cause:**
- `DashboardDataHelper.kt` has auto-generation enabled
- `addSamplePaymentData()` called automatically

**Fix:**
```kotlin
// In SellerDashboardScreen.kt - REMOVE these lines:
// addSamplePaymentData()

// In DashboardDataHelper.kt - REMOVE call from:
// setupSellerDashboard()
```

**Debug Steps:**
1. Open Firebase Console
2. Search for payment in collection
3. Check if document exists
4. If exists, payment is real (not auto-generated)
5. If reappears, check logcat for `addSamplePaymentData` call

---

### Issue 2: Missing Seller ID in Payment

**Symptoms:**
```
- Payment shows in Buyer Payment History
- But seller info missing or null
- "Unknown Seller" displayed
```

**Debug Steps:**
```
Logcat Check:
1. Run: adb logcat | grep -i "payment\|seller"
2. Look for NullPointerException on sellerID

Firebase Check:
1. Open payment document
2. Verify paymentItems[].sellerId populated
3. If null: Order didn't have seller assigned

Order Check:
1. Find related order
2. Verify order.sellerId populated
3. If null: Order corruption issue
```

---

### Issue 3: Payment Amount Mismatch

**Symptoms:**
```
- Checkout shows ₹1000
- Payment in Firebase shows ₹900
- Discrepancy in totals
```

**Debug Steps:**
```
Check Payment Items:
1. Firebase: Open payment document
2. Sum paymentItems[].amount
3. Should equal total amount

Check Commission:
1. If co-seller: Verify commission deducted
2. Formula: member_amount = (product_price × share) - commission

Check Cart:
1. LogCat: Search for cart total log
2. Verify shipping fee included
3. Verify no double-discounting
```

---

### Issue 4: Refund Status Not Syncing

**Symptoms:**
```
- Seller approved refund
- Buyer still sees "Pending"
- Related payment status unchanged
```

**Debug Steps:**
```
Firestore Check:
1. Find refund document
2. Check status field
3. Check updatedAt timestamp
4. Verify sellerId matches current seller

Payment Status Check:
1. Find related payment
2. Verify payment.status updated
3. If not: Sync mechanism failed
4. Check RefundProcessor for errors

Logcat Check:
adb logcat | grep -i "refund"
Look for: "RefundProcessor", "status update", "error"
```

---

### Issue 5: Co-Seller Split Calculation Error

**Symptoms:**
```
- Member gets ₹400 instead of ₹380
- Store owner commission wrong
- Product allocation unclear
```

**Debug Steps:**
```
Payment Split Verification:
1. Calculate expected split manually
2. Find payment documents for each member
3. Compare calculated vs actual

Firebase Check:
{
  member_1_amount: 300 + (500 × 0.6) - 10% comm = 380
  member_2_amount: 200 + (500 × 0.4) - 10% comm = 420
  store_owner_comm: 38 + 42 = 80
  Total: 380 + 420 - 80 = 720 (member portion)
}

Logcat Check:
adb logcat | grep -i "split\|commission"
Look for calculation logs
```

---

### Issue 6: Filter Tabs Not Always Visible

**Symptoms:**
```
- "Pending" tab hides when no pending payments
- "Completed" tab appears/disappears
- Inconsistent with other screens
```

**Debug Steps:**
```
Code Check:
1. Open SellerPaymentsScreen.kt
2. Find RefundFilter.entries.forEach
3. Should iterate ALL entries, not conditional

Verify:
✓ All tabs drawn for each filter in enum
✓ No condition like: if (hasPayments) showTab
✓ Tab always visible, shows empty state if no data
```

---

### Issue 7: Duplicate Payments Displayed

**Symptoms:**
```
- Same payment appears twice in list
- Different IDs in Firebase
- Data overlap
```

**Debug Steps:**
```
Logcat Check:
adb logcat | grep -i "duplicate\|overlap"

Firebase Check:
1. Count payments in collection
2. Find duplicates by orderId + buyerId + sellerId
3. Delete duplicates if found

Code Check:
1. Verify query doesn't fetch duplicates
2. Check LazyColumn doesn't render twice
3. Verify ViewModel deduplication logic
```

---

### Issue 8: Count Badge Issues

**Symptoms:**
```
- Badge shows on filter tabs (shouldn't)
- Badge shows incorrect count
- "1 pending action(s)" instead of "1 Pending Action"
```

**Debug Steps:**
```
Text Verification:
1. Dashboard Refund Requests card
2. Check: if (count == 1) "Action" else "Actions"
3. Should be: "1 Pending Action" or "2 Pending Actions"

Badge Verification:
1. Open SellerRefundManagementScreen
2. Find filter tabs rendering
3. Search for: if (filter == PENDING && count > 0)
4. Should be REMOVED entirely

Logcat Check:
Search for badge-related logs
```

---

## QUICK TEST CHECKLIST

```
BUYER PAYMENTS
☐ Single seller order completes
☐ Multi-seller order completes
☐ Payment History shows all payments
☐ Filter tabs always visible
☐ No count badges on tabs
☐ Empty state professional
☐ Realtime updates work

SELLER PAYMENTS
☐ Seller can view payments
☐ Filter tabs always visible
☐ No count badges on tabs
☐ Payment details accurate
☐ Dashboard shows pending count
☐ Singular/plural text correct
☐ Realtime updates work

REFUNDS
☐ Buyer can request refund
☐ Seller can approve/reject
☐ Status updates realtime
☐ Notifications sent correctly
☐ Multiple refunds limited
☐ Refund details display correctly
☐ Amount accuracy verified

CO-SELLER PAYMENTS
☐ Co-seller sees own payments
☐ Store name displayed
☐ Cannot access other store payments
☐ Filter tabs working

PAYMENT SPLIT
☐ Fair split calculated
☐ Commission deducted
☐ Store owner gets commission
☐ Store badge displayed
☐ Legacy orders handled
☐ Member removal cascades
```

---

## DEPLOYMENT VERIFICATION

Before deploying to production:

1. **Run all test scenarios above**
2. **Check Firebase data for corrupted payments**
3. **Verify commission system working**
4. **Test refund workflow end-to-end**
5. **Confirm no auto-generated payments**
6. **Validate payment split accuracy**
7. **Check realtime updates consistent**
8. **Verify filter tabs always visible**
9. **Confirm singular/plural text logic**
10. **Test edge cases (limits, cancellations, etc)**
