# 🎯 REFUND REQUEST LIMIT - QUICK REFERENCE

## The Rule: **2 STRIKES AND YOU'RE OUT**

```
┌─────────────────────────────────────────────────────────┐
│  ATTEMPT 1: Initial Request                             │
│  ✅ Allowed                                             │
│  → If rejected: Can resubmit once                       │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  ATTEMPT 2: Resubmission (if first was rejected)        │
│  ✅ Allowed (LAST CHANCE)                               │
│  → If rejected: FINAL DECISION - No more attempts       │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  ATTEMPT 3+: BLOCKED                                    │
│  ❌ Not allowed - Form hidden, error shown              │
└─────────────────────────────────────────────────────────┘
```

---

## Key Fields

| Field | Purpose | Values |
|-------|---------|--------|
| `rejection_count` | Tracks total rejections | 0, 1, 2 |
| `can_resubmit` | Whether buyer can try again | true (if count < 2), false (if count >= 2) |
| `final_decision` | Permanent block flag | false (can try), true (blocked forever) |

---

## Enforcement Points

### 1. Database (RefundRepository)
```kotlin
// Automatically sets on rejection:
rejection_count = currentCount + 1
can_resubmit = (rejection_count < 2)
final_decision = (rejection_count >= 2)
```

### 2. UI (BuyerRefundRequestScreen)
```kotlin
// Checks before showing form:
if (mostRecentRefund?.finalDecision == true) {
    // ❌ BLOCK: Show error, hide form
}
```

---

## User Messages

### After First Rejection:
```
⚠️ Your refund request was rejected.

You have ONE more chance to resubmit with 
improved reason and evidence.

[ Resubmit Refund Request ]
```

### After Second Rejection:
```
🚫 FINAL DECISION

Your refund request has been rejected twice.
No further refund requests can be submitted 
for this order.

[ Contact Support ]  [ Go Back ]
```

### Trying to Submit After Final Decision:
```
❌ Cannot Submit Refund Request

Refund request denied (FINAL DECISION)

Your refund request has been rejected twice.
No further refund requests can be submitted.

[ Go Back ]
```

---

## Testing Checklist

- [ ] Submit refund → Reject → Can resubmit ✅
- [ ] Submit refund → Reject → Resubmit → Reject → BLOCKED ✅
- [ ] Try to submit while pending → BLOCKED ✅
- [ ] Try to submit after refunded → BLOCKED ✅
- [ ] Try to submit after final decision → BLOCKED ✅

---

## Quick Debug

```javascript
// Check refund status in Firestore Console:
db.collection('refunds')
  .where('order_id', '==', 'ORDER_ID')
  .orderBy('requested_at', 'desc')
  .limit(1)
  .get()
  .then(doc => {
    console.log({
      status: doc.data().status,
      rejectionCount: doc.data().rejection_count,
      canResubmit: doc.data().can_resubmit,
      finalDecision: doc.data().final_decision
    });
  });
```

---

**Remember:** The system checks the **most recent refund** for the order, not just any refund!
