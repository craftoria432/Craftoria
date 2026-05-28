# Refund Workflow Testing Guide

**Date**: May 13, 2026  
**Purpose**: Step-by-step guide to test all refund workflow scenarios

---

## Pre-Test Setup

### Requirements
- Android device or emulator with app installed
- Two test accounts: one buyer, one seller
- Test orders in COMPLETED or DELIVERED status
- Firestore database with test data

### Test Data Preparation
1. Create buyer account (e.g., buyer@test.com)
2. Create seller account (e.g., seller@test.com)
3. Create test order with status COMPLETED or DELIVERED
4. Ensure order is within 30-day refund window

---

## Test Scenario 1: Happy Path (Refund Approval)

### Objective
Verify that a refund request flows from REQUESTED → APPROVED → PROCESSING → COMPLETED

### Steps

**Step 1: Buyer Requests Refund**
1. Open app as buyer
2. Navigate to "My Orders"
3. Find completed order
4. Tap "Request Refund" button
5. Fill in refund reason
6. Tap "Submit"

**Expected Result**:
- ✅ Button changes to "Refund Pending" (orange, disabled)
- ✅ No page refresh needed
- ✅ Button state persists after app restart

**Step 2: Seller Approves Refund**
1. Open app as seller
2. Navigate to "Orders" or admin dashboard
3. Find the order with refund request
4. Tap "Approve Refund" button
5. Confirm approval

**Expected Result**:
- ✅ Seller sees confirmation message
- ✅ Refund status changes to "APPROVED_BY_SELLER"
- ✅ Backend starts processing

**Step 3: Verify Real-Time Update (Buyer)**
1. Keep buyer app open on "My Orders"
2. Watch the button for the order
3. Within 5-30 seconds, button should update

**Expected Result**:
- ✅ Button changes to "Refund Approved" (blue, disabled)
- ✅ No page refresh needed
- ✅ Update happens in real-time

**Step 4: Verify Processing State**
1. Continue watching the button
2. Within 5-30 seconds, button should update again

**Expected Result**:
- ✅ Button changes to "Processing" (blue with sync icon)
- ✅ Indicates backend is processing refund

**Step 5: Verify Completion**
1. Continue watching the button
2. Within 5-30 seconds, button should update to final state

**Expected Result**:
- ✅ Button changes to "Refund Done" (green, disabled)
- ✅ Purple "Refunded" badge appears next to order status
- ✅ Order stays in "Completed" tab (NOT moved to cancelled)

**Step 6: Verify Payment History Update**
1. Navigate to "Payment History"
2. Find the payment for this order

**Expected Result**:
- ✅ Payment status shows "Refunded" (purple)
- ✅ Refund amount displayed: "↶ Refunded: PKR [amount]"
- ✅ Refund date displayed

**Step 7: Verify Seller Payment Details**
1. Open app as seller
2. Navigate to "Payments"
3. Find the payment for this order
4. Tap to view details

**Expected Result**:
- ✅ Payment status shows "REFUNDED" (purple)
- ✅ Refund amount displayed
- ✅ Seller earnings adjusted (reduced by refund amount)

---

## Test Scenario 2: Rejection & Resubmission

### Objective
Verify that buyer can resubmit after first rejection

### Steps

**Step 1: Buyer Requests Refund**
1. Open app as buyer
2. Navigate to "My Orders"
3. Find completed order
4. Tap "Request Refund" button
5. Fill in refund reason
6. Tap "Submit"

**Expected Result**:
- ✅ Button changes to "Refund Pending" (orange, disabled)

**Step 2: Seller Rejects Refund**
1. Open app as seller
2. Navigate to "Orders" or admin dashboard
3. Find the order with refund request
4. Tap "Reject Refund" button
5. Enter rejection reason
6. Confirm rejection

**Expected Result**:
- ✅ Seller sees confirmation message
- ✅ Refund status changes to "REJECTED_BY_SELLER"

**Step 3: Verify Buyer Sees Rejection**
1. Keep buyer app open on "My Orders"
2. Watch the button for the order

**Expected Result**:
- ✅ Button changes to "Resubmit" (orange, clickable)
- ✅ Buyer can click to resubmit

**Step 4: Buyer Resubmits Refund**
1. Tap "Resubmit" button
2. Fill in new refund reason
3. Tap "Submit"

**Expected Result**:
- ✅ Button changes to "Refund Pending" (orange, disabled)
- ✅ Refund request is resubmitted

**Step 5: Seller Approves Second Request**
1. Open app as seller
2. Find the order with new refund request
3. Tap "Approve Refund" button
4. Confirm approval

**Expected Result**:
- ✅ Seller sees confirmation message
- ✅ Refund status changes to "APPROVED_BY_SELLER"

**Step 6: Verify Completion**
1. Keep buyer app open on "My Orders"
2. Watch the button for the order

**Expected Result**:
- ✅ Button changes to "Refund Done" (green, disabled)
- ✅ Purple "Refunded" badge appears
- ✅ Refund completes successfully

---

## Test Scenario 3: Final Decision (Two Rejections)

### Objective
Verify that after 2 rejections, buyer cannot resubmit

### Steps

**Step 1: First Rejection**
1. Buyer requests refund
2. Seller rejects
3. Buyer sees "Resubmit" button

**Expected Result**:
- ✅ Button shows "Resubmit" (orange, clickable)

**Step 2: Buyer Resubmits**
1. Tap "Resubmit" button
2. Fill in refund reason
3. Tap "Submit"

**Expected Result**:
- ✅ Button changes to "Refund Pending" (orange, disabled)

**Step 3: Second Rejection**
1. Seller rejects the second request
2. Seller sees confirmation message

**Expected Result**:
- ✅ Refund status changes to "REJECTED_BY_SELLER"
- ✅ `final_decision` flag set to true

**Step 4: Verify Final Decision**
1. Keep buyer app open on "My Orders"
2. Watch the button for the order

**Expected Result**:
- ✅ Button changes to "Refund Denied" (gray, disabled)
- ✅ Button is NOT clickable
- ✅ Buyer cannot resubmit

---

## Test Scenario 4: Real-Time Sync (No Refresh)

### Objective
Verify that screens update in real-time without page refresh

### Steps

**Step 1: Open Multiple Screens**
1. Open app as buyer
2. Navigate to "My Orders"
3. Find completed order
4. Keep this screen open

**Step 2: Trigger Refund Approval from Another Device**
1. On another device/browser, open admin dashboard
2. Find the same order
3. Approve the refund

**Expected Result**:
- ✅ Buyer's "My Orders" screen updates automatically
- ✅ Button changes from "Refund Pending" to "Refund Approved"
- ✅ No page refresh needed
- ✅ Update happens within 5 seconds

**Step 3: Verify Payment History Updates**
1. Keep buyer app open
2. Navigate to "Payment History"
3. Find the payment for this order

**Expected Result**:
- ✅ Payment status shows "Refund Processing" (blue)
- ✅ Updates in real-time as backend processes

**Step 4: Verify Completion Updates**
1. Keep watching "Payment History"
2. Within 5-30 seconds, status should update

**Expected Result**:
- ✅ Payment status changes to "Refunded" (purple)
- ✅ Refund amount displayed
- ✅ No page refresh needed

---

## Test Scenario 5: Co-Seller Refund Split

### Objective
Verify that co-seller refunds are calculated and displayed correctly

### Steps

**Step 1: Create Order with Co-Seller**
1. Create order with multiple sellers
2. Ensure co-seller is assigned
3. Order should be in COMPLETED status

**Expected Result**:
- ✅ Order has co-seller information
- ✅ Payment split information stored

**Step 2: Buyer Requests Refund**
1. Open app as buyer
2. Navigate to "My Orders"
3. Find order with co-seller
4. Tap "Request Refund" button
5. Fill in refund reason
6. Tap "Submit"

**Expected Result**:
- ✅ Button changes to "Refund Pending" (orange, disabled)

**Step 3: Seller Approves Refund**
1. Open app as seller
2. Find the order with refund request
3. Tap "Approve Refund" button
4. Confirm approval

**Expected Result**:
- ✅ Seller sees confirmation message
- ✅ Backend calculates co-seller split

**Step 4: Verify Refund Completion**
1. Keep buyer app open on "My Orders"
2. Watch the button for the order

**Expected Result**:
- ✅ Button changes to "Refund Done" (green, disabled)
- ✅ Purple "Refunded" badge appears

**Step 5: Verify Main Seller Payment Details**
1. Open app as main seller
2. Navigate to "Payments"
3. Find the payment for this order
4. Tap to view details

**Expected Result**:
- ✅ Payment status shows "REFUNDED" (purple)
- ✅ Original amount displayed
- ✅ Refund amount displayed
- ✅ Seller earnings adjusted (reduced by refund amount)

**Step 6: Verify Co-Seller Payment Details**
1. Open app as co-seller
2. Navigate to "Store Payments"
3. Find the payment for this order
4. Tap to view details

**Expected Result**:
- ✅ Payment status shows "REFUNDED" (purple)
- ✅ Co-seller's original split amount displayed
- ✅ Co-seller's refund split amount displayed
- ✅ Co-seller earnings adjusted (reduced by their refund split)

---

## Test Scenario 6: Error Handling

### Objective
Verify that errors are handled gracefully

### Steps

**Step 1: Network Error During Refund Request**
1. Open app as buyer
2. Disable network connection
3. Navigate to "My Orders"
4. Tap "Request Refund" button
5. Fill in refund reason
6. Tap "Submit"

**Expected Result**:
- ✅ Error message displayed
- ✅ Button remains in original state
- ✅ No partial state saved

**Step 2: Re-enable Network**
1. Re-enable network connection
2. Tap "Request Refund" button again
3. Fill in refund reason
4. Tap "Submit"

**Expected Result**:
- ✅ Refund request succeeds
- ✅ Button changes to "Refund Pending"

**Step 3: Verify No Duplicate Requests**
1. Check Firestore database
2. Verify only one refund request exists

**Expected Result**:
- ✅ No duplicate refund requests
- ✅ Idempotency key prevents duplicates

---

## Test Scenario 7: Edge Cases

### Objective
Verify edge cases are handled correctly

### Steps

**Step 1: Refund Outside 30-Day Window**
1. Create order with delivery date > 30 days ago
2. Open app as buyer
3. Navigate to "My Orders"
4. Find the old order

**Expected Result**:
- ✅ Button shows "View Details" (not "Request Refund")
- ✅ Refund not allowed outside window

**Step 2: Multiple Refund Requests for Same Order**
1. Create order
2. Buyer requests refund
3. Seller rejects
4. Buyer resubmits
5. Seller rejects again
6. Buyer tries to resubmit

**Expected Result**:
- ✅ Button shows "Refund Denied" (gray, disabled)
- ✅ Buyer cannot resubmit after 2 rejections

**Step 3: Partial Refund**
1. Create order with multiple items
2. Buyer requests partial refund
3. Seller approves partial refund

**Expected Result**:
- ✅ Refund amount is less than order total
- ✅ Payment status shows "REFUNDED"
- ✅ Refund amount displayed correctly

---

## Verification Checklist

After running all scenarios, verify:

- ✅ All button states display correctly
- ✅ All colors match specification
- ✅ All icons display correctly
- ✅ Real-time updates work without refresh
- ✅ Refund badge appears when completed
- ✅ Payment status updates correctly
- ✅ Seller earnings adjusted correctly
- ✅ Co-seller earnings adjusted correctly
- ✅ No memory leaks (check device memory)
- ✅ No crashes or errors
- ✅ No duplicate refund requests
- ✅ Idempotency works correctly
- ✅ Listener cleanup works (no orphaned listeners)

---

## Troubleshooting

### Issue: Button doesn't update in real-time
**Solution**:
1. Check Firestore listener is active
2. Verify network connection
3. Check Firestore rules allow read access
4. Restart app and try again

### Issue: Refund badge doesn't appear
**Solution**:
1. Verify refund status is "COMPLETED"
2. Check Firestore document has correct status
3. Verify order status is COMPLETED or DELIVERED
4. Restart app and try again

### Issue: Earnings not adjusted
**Solution**:
1. Check backend refund processing completed
2. Verify payment document updated
3. Check co-seller split calculations
4. Review backend logs for errors

### Issue: Duplicate refund requests
**Solution**:
1. Check idempotency key implementation
2. Verify request ID is unique
3. Review backend deduplication logic
4. Check Firestore for duplicate documents

---

## Performance Metrics

Expected performance:
- Button state update: < 5 seconds
- Refund completion: 5-30 seconds
- Real-time sync: < 2 seconds
- Payment history update: < 5 seconds
- Listener initialization: < 1 second

---

## Success Criteria

All scenarios pass when:
- ✅ All button states display correctly
- ✅ All colors match specification
- ✅ Real-time updates work without refresh
- ✅ Refund badge appears when completed
- ✅ Payment status updates correctly
- ✅ Seller earnings adjusted correctly
- ✅ Co-seller earnings adjusted correctly
- ✅ No crashes or errors
- ✅ No memory leaks
- ✅ All edge cases handled

---

**Status**: Ready for testing

