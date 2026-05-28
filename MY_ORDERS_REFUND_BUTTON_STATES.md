# 🎯 MY ORDERS SCREEN - REFUND BUTTON STATES

## Professional Button/Badge States for Order Cards

### **STATE 1: Eligible for Refund** ✅
**Condition:** Order is delivered/completed, within 30-day window, no existing refund

**Display:**
```kotlin
OutlinedButton(
    onClick = { /* Navigate to refund request screen */ },
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = Primary
    ),
    border = BorderStroke(1.dp, Primary)
) {
    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(6.dp))
    Text("Request Refund", fontSize = 12.sp, fontWeight = FontWeight.Medium)
}
```

---

### **STATE 2: Refund Requested (Pending)** ⏳
**Condition:** Refund status = "requested" or "under_review"

**Display:**
```kotlin
Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFFFF3E0), // Amber background
    border = BorderStroke(1.dp, Color(0xFFFF9800))
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Refund Pending",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFF9800)
        )
    }
}
```

---

### **STATE 3: Refund Approved (Processing)** 🔄
**Condition:** Refund status = "approved_by_seller" or "approved_by_admin" or "processing"

**Display:**
```kotlin
Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFE3F2FD), // Blue background
    border = BorderStroke(1.dp, Color(0xFF2196F3))
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Refund Approved",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2196F3)
        )
    }
}
```

---

### **STATE 4: Refund Completed** ✅
**Condition:** Refund status = "completed"

**Display:**
```kotlin
Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFE8F5E9), // Green background
    border = BorderStroke(1.dp, Success)
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Refunded",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Success
        )
    }
}
```

---

### **STATE 5: First Rejection (Can Resubmit)** ⚠️
**Condition:** Refund status = "rejected_by_seller" or "rejected_by_admin", can_resubmit = true

**Display:**
```kotlin
OutlinedButton(
    onClick = { /* Navigate to refund request screen for resubmission */ },
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = Color(0xFFFF9800) // Amber/Warning color
    ),
    border = BorderStroke(1.dp, Color(0xFFFF9800))
) {
    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(6.dp))
    Text("Resubmit Refund", fontSize = 12.sp, fontWeight = FontWeight.Medium)
}
```

---

### **STATE 6: Final Decision (Rejected Twice)** 🚫 **← YOUR QUESTION**
**Condition:** final_decision = true (rejected twice)

**Display:**
```kotlin
Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFFFEBEE), // Light red background
    border = BorderStroke(1.dp, Error.copy(alpha = 0.5f)),
    modifier = Modifier.clickable(enabled = false) { } // Non-clickable
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            tint = Error,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Refund Denied",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Error
        )
    }
}
```

**Alternative (More Informative):**
```kotlin
Surface(
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFFAFAFA), // Gray background
    border = BorderStroke(1.dp, Color(0xFF9E9E9E))
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Refund Not Available",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF757575)
            )
        }
        Text(
            text = "Request denied (final decision)",
            fontSize = 10.sp,
            color = Color(0xFF9E9E9E)
        )
    }
}
```

---

## 📱 VISUAL COMPARISON

```
┌─────────────────────────────────────────────────────────────┐
│  Order #3BD2RW93                                            │
│  PKR 1500 • Delivered May 8, 2026                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  STATE 1: [ 🔄 Request Refund ]  ← Clickable button       │
│                                                             │
│  STATE 2: [ ⏳ Refund Pending ]  ← Read-only badge         │
│                                                             │
│  STATE 3: [ ✓ Refund Approved ]  ← Read-only badge         │
│                                                             │
│  STATE 4: [ ✓ Refunded ]  ← Read-only badge                │
│                                                             │
│  STATE 5: [ 🔄 Resubmit Refund ]  ← Clickable button       │
│                                                             │
│  STATE 6: [ 🚫 Refund Denied ]  ← Read-only badge          │
│           Final decision                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎨 RECOMMENDED DESIGN (STATE 6)

### **Option A: Simple Badge (Recommended)**
```
┌──────────────────────────┐
│  🚫 Refund Denied        │
└──────────────────────────┘
```
- **Background:** Light red (#FFEBEE)
- **Border:** Red with 50% opacity
- **Icon:** Block icon
- **Text:** "Refund Denied"
- **Non-clickable**

### **Option B: Informative Badge**
```
┌──────────────────────────────────┐
│  🚫 Refund Not Available         │
│  Request denied (final decision) │
└──────────────────────────────────┘
```
- **Background:** Light gray (#FAFAFA)
- **Border:** Gray
- **Icon:** Block icon
- **Text:** Two lines with explanation
- **Non-clickable**

### **Option C: With Support Link**
```
┌──────────────────────────────────┐
│  🚫 Refund Denied                │
│  [ Contact Support ]             │
└──────────────────────────────────┘
```
- Same as Option A but with a small "Contact Support" link below
- Support link opens help/support screen

---

## 💡 PROFESSIONAL RECOMMENDATION

**Use Option B (Informative Badge)** because:

1. ✅ **Clear Communication** - Buyer understands it's not just "denied" but a "final decision"
2. ✅ **Non-Clickable** - Visual design makes it clear this is a status, not an action
3. ✅ **Professional** - Doesn't feel harsh, just informative
4. ✅ **Consistent** - Matches the style of other status badges
5. ✅ **Space-Efficient** - Doesn't take up too much space on the order card

**Add a "Contact Support" button separately** (not in the badge) if the buyer wants to dispute the decision.

---

## 🔧 IMPLEMENTATION CODE

```kotlin
@Composable
fun RefundButtonOrBadge(
    order: Order,
    mostRecentRefund: RefundRequest?,
    onRequestRefund: () -> Unit,
    onContactSupport: () -> Unit
) {
    when {
        // STATE 6: Final Decision - Refund Denied
        mostRecentRefund?.finalDecision == true -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Read-only badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFAFAFA),
                    border = BorderStroke(1.dp, Color(0xFF9E9E9E))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Refund Not Available",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF757575)
                            )
                        }
                        Text(
                            text = "Request denied (final decision)",
                            fontSize = 10.sp,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
                
                // Optional: Contact Support link
                TextButton(
                    onClick = onContactSupport,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Contact Support", fontSize = 11.sp)
                }
            }
        }
        
        // STATE 5: First Rejection - Can Resubmit
        mostRecentRefund?.status in listOf("rejected_by_seller", "rejected_by_admin") 
        && mostRecentRefund?.canResubmit == true -> {
            OutlinedButton(
                onClick = onRequestRefund,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF9800)
                ),
                border = BorderStroke(1.dp, Color(0xFFFF9800))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Resubmit Refund", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        // STATE 4: Refund Completed
        mostRecentRefund?.status == "completed" -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Success)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Refunded",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Success
                    )
                }
            }
        }
        
        // STATE 3: Refund Approved/Processing
        mostRecentRefund?.status in listOf("approved_by_seller", "approved_by_admin", "processing") -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE3F2FD),
                border = BorderStroke(1.dp, Color(0xFF2196F3))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Refund Approved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }
        
        // STATE 2: Refund Pending
        mostRecentRefund?.status in listOf("requested", "under_review") -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF3E0),
                border = BorderStroke(1.dp, Color(0xFFFF9800))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Refund Pending",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
        
        // STATE 1: Eligible for Refund
        order.isEligibleForRefund() -> {
            OutlinedButton(
                onClick = onRequestRefund,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                ),
                border = BorderStroke(1.dp, Primary)
            ) {
                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Request Refund", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
```

---

## ✅ SUMMARY

**After 2 rejections (final decision), show:**

```
┌──────────────────────────────────┐
│  🚫 Refund Not Available         │
│  Request denied (final decision) │
└──────────────────────────────────┘
```

**Key Features:**
- ✅ **Read-only badge** (not a button)
- ✅ **Clear messaging** ("final decision")
- ✅ **Professional styling** (gray, non-threatening)
- ✅ **Non-clickable** (visual design makes this obvious)
- ✅ **Optional support link** below the badge

This provides a professional, clear, and user-friendly way to communicate that refund requests are no longer possible for this order.
