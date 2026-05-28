# 🧪 PAYMENT & REFUND SYSTEM - TESTING GUIDE

## Quick Test Scenarios

### **TEST 1: Payment Date Display** ✅

**Objective:** Verify payment dates show actual transaction dates, not record creation dates

**Steps:**
1. Open Payment History screen as buyer
2. Check dates on payment cards

**Expected Result:**
- Dates match order placement dates
- NOT all showing May 9, 2026
- Format: "May 08, 2026" or similar

**Pass Criteria:** ✅ Each payment shows its actual order date

---

### **TEST 2: Instant Screen Loading** ✅

**Objective:** Verify screen loads instantly on revisit

**Steps:**
1. Open Payment History screen (first time)
2. Wait for data to load
3. Go back
4. Open Payment History screen again

**Expected Result:**
- First load: Brief loading (acceptable)
- Second load: **INSTANT** - data appears immediately
- No loading spinner on revisit

**Pass Criteria:** ✅ Screen shows cached data instantly on second visit

---

### **TEST 3: Refund Request Limit** ✅

**Objective:** Verify buyer can only request refund twice (initial + 1 resubmission)

**Steps:**
1. Submit refund request for completed order
2. Seller rejects with reason
3. Submit refund request again (resubmission)
4. Seller rejects again
5. Try to submit refund request third time

**Expected Result:**
- First rejection: Shows "You can resubmit with improved reason"
- Second rejection: Shows "FINAL DECISION - No more resubmissions allowed"
- Third attempt: Button disabled or shows final decision message

**Pass Criteria:** ✅ Maximum 2 refund attempts allowed

---

### **TEST 4: Refund Status Visibility** ✅

**Objective:** Verify buyer can see who approved/rejected and when

**Steps:**
1. Submit refund request
2. Check refund status

**Expected Result:**
Status shows one of:
- "Refund Requested" (Orange)
- "Under Review" (Amber)
- "Approved by Seller" (Blue) + seller name + timestamp
- "Approved by Admin" (Blue) + admin name + timestamp
- "Rejected by Seller" (Red) + seller name + reason + timestamp
- "Rejected by Admin" (Red) + admin name + reason + timestamp
- "Processing" (Dodger Blue)
- "Refunded Successfully" (Green) + completion date

**Pass Criteria:** ✅ Status clearly shows actor and timestamp

---

### **TEST 5: 24-Hour Auto-Approval** ⏰

**Objective:** Verify refund auto-approves after 24 hours of no response

**Setup:**
```kotlin
// For testing, temporarily change AUTO_APPROVAL_HOURS to 1 minute
companion object {
    private const val AUTO_APPROVAL_HOURS = 0.0167 // 1 minute for testing
}
```

**Steps:**
1. Submit refund request
2. Wait 1 minute (or 24 hours in production)
3. Run auto-approval check manually or wait for periodic check

**Expected Result:**
- After 24 hours: Status changes to "Approved by Auto-Approval System"
- Refund is processed immediately
- Buyer receives notification
- Seller receives notification about auto-approval

**Pass Criteria:** ✅ Refund auto-approved and processed after threshold

---

### **TEST 6: Refund Amount Display** ✅

**Objective:** Verify refunded payments show refund amount

**Steps:**
1. Complete a refund successfully
2. Open Payment History screen
3. Find the refunded payment

**Expected Result:**
- Payment card shows "Refunded" badge (Purple)
- Shows "Refunded: PKR X" below payment amount
- Original amount still visible

**Pass Criteria:** ✅ Refund amount clearly displayed

---

## 🔍 Edge Cases to Test

### **Edge Case 1: Old Orders Without originalTransactionDate**
**Test:** Open payment history with orders created before this update
**Expected:** Falls back to `createdAt` gracefully

### **Edge Case 2: Multiple Refund Requests for Same Order**
**Test:** Try to submit second refund while first is pending
**Expected:** System prevents duplicate requests

### **Edge Case 3: Refund After Order Completion**
**Test:** Request refund for order completed 29 days ago
**Expected:** Allowed (within 30-day window)

### **Edge Case 4: Refund After 30-Day Window**
**Test:** Request refund for order completed 31 days ago
**Expected:** Rejected with "Refund window expired" message

---

## 📊 Data Validation Queries

### **Check Payment Dates:**
```javascript
// Firestore Console
db.collection('seller_payments')
  .where('buyer_id', '==', 'TEST_BUYER_ID')
  .get()
  .then(snapshot => {
    snapshot.forEach(doc => {
      const data = doc.data();
      console.log({
        id: doc.id,
        createdAt: new Date(data.created_at),
        originalTransactionDate: data.original_transaction_date ? new Date(data.original_transaction_date) : 'Not set',
        paymentDate: data.payment_date ? new Date(data.payment_date) : 'Not set'
      });
    });
  });
```

### **Check Refund Rejection Counts:**
```javascript
// Firestore Console
db.collection('refunds')
  .where('buyer_id', '==', 'TEST_BUYER_ID')
  .get()
  .then(snapshot => {
    snapshot.forEach(doc => {
      const data = doc.data();
      console.log({
        id: doc.id,
        status: data.status,
        rejectionCount: data.rejection_count || 0,
        canResubmit: data.can_resubmit !== false,
        finalDecision: data.final_decision || false
      });
    });
  });
```

### **Check Pending Refunds for Auto-Approval:**
```javascript
// Firestore Console
const now = Date.now();
const twentyFourHoursAgo = now - (24 * 60 * 60 * 1000);

db.collection('refunds')
  .where('status', '==', 'requested')
  .where('requested_at', '<', twentyFourHoursAgo)
  .get()
  .then(snapshot => {
    console.log(`Found ${snapshot.size} refunds eligible for auto-approval`);
    snapshot.forEach(doc => {
      const data = doc.data();
      const hoursSince = (now - data.requested_at) / (1000 * 60 * 60);
      console.log({
        id: doc.id,
        orderId: data.order_id,
        hoursSinceRequest: hoursSince.toFixed(2)
      });
    });
  });
```

---

## ✅ Test Checklist

### **Critical Tests:**
- [ ] Payment dates show actual transaction dates
- [ ] Screen loads instantly on revisit
- [ ] Refund request limited to 2 attempts
- [ ] Refund status shows actor and timestamp
- [ ] 24-hour auto-approval works

### **Important Tests:**
- [ ] Refund amount displayed for refunded payments
- [ ] Old orders without new fields work correctly
- [ ] Duplicate refund requests prevented
- [ ] 30-day refund window enforced

### **Edge Cases:**
- [ ] Backward compatibility with old data
- [ ] Multiple sellers (co-seller orders)
- [ ] Failed refund processing retry
- [ ] Network interruption during refund

---

## 🐛 Known Issues & Workarounds

### **Issue 1: First Load Still Shows Brief Loading**
**Status:** Expected behavior
**Reason:** No cached data on first visit
**Workaround:** None needed - instant loading works on revisit

### **Issue 2: Auto-Approval Requires Background Service**
**Status:** Implementation pending
**Reason:** Needs WorkManager or Cloud Function integration
**Workaround:** Manual trigger for testing

---

## 📞 Reporting Issues

When reporting issues, include:
1. Test scenario number
2. Expected vs actual result
3. Screenshots/screen recording
4. Firestore data snapshot
5. Logcat output (filter: "BuyerPaymentViewModel", "RefundRepository", "RefundAutoApproval")

---

**Last Updated:** May 10, 2026
**Test Coverage:** Phase 1-5 Complete
**Status:** Ready for QA Testing
