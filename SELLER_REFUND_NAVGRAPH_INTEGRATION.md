# 🔗 Seller Refund Screens - NavGraph Integration

## Step 1: Add Screen Routes

**File:** `app/src/main/java/com/gcuf/craftoria/ui/navigation/NavGraph.kt`

### Add these routes in the `Screen` sealed class (around line 133, after SellerPaymentDetail):

```kotlin
// Seller Refund Management
object SellerRefundManagement : Screen("seller_refund_management")
object SellerRefundDetail : Screen("seller_refund_detail/{refundId}") {
    fun createRoute(refundId: String) = "seller_refund_detail/$refundId"
}
```

---

## Step 2: Add Composable Routes

**File:** `app/src/main/java/com/gcuf/craftoria/ui/navigation/NavGraph.kt`

### Add these composables in the `NavHost` block (after SellerPaymentDetail composable):

```kotlin
// ══════════════════════════════════════════════════════════════════════════════
// Seller Refund Management
// ══════════════════════════════════════════════════════════════════════════════

composable(Screen.SellerRefundManagement.route) {
    // Guard: only sellers can access
    if (currentUser?.role == UserRole.SELLER) {
        SellerRefundManagementScreen(
            onBackClick = { navController.popBackStack() },
            onRefundClick = { refundId ->
                navController.navigate(Screen.SellerRefundDetail.createRoute(refundId))
            }
        )
    } else {
        // Redirect unauthorized users
        LaunchedEffect(Unit) { navController.popBackStack() }
    }
}

composable(
    route = Screen.SellerRefundDetail.route,
    arguments = listOf(
        navArgument("refundId") { type = NavType.StringType }
    )
) { backStackEntry ->
    val refundId = backStackEntry.arguments?.getString("refundId") ?: return@composable
    
    if (currentUser?.role == UserRole.SELLER) {
        SellerRefundDetailScreen(
            refundId = refundId,
            onBackClick = { navController.popBackStack() },
            onContactBuyer = { buyerId ->
                // Navigate to chat with buyer
                navController.navigate(Screen.Chat.createRoute(buyerId))
            }
        )
    } else {
        LaunchedEffect(Unit) { navController.popBackStack() }
    }
}
```

---

## Step 3: Add Imports

**File:** `app/src/main/java/com/gcuf/craftoria/ui/navigation/NavGraph.kt`

### Add these imports at the top:

```kotlin
import com.gcuf.craftoria.ui.screens.seller.SellerRefundManagementScreen
import com.gcuf.craftoria.ui.screens.seller.SellerRefundDetailScreen
```

---

## Step 4: Add Dashboard Card

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerDashboardScreen.kt`

### Add state for pending refunds count (at the top of SellerDashboardScreen composable):

```kotlin
// Real-time pending refunds count
var pendingRefundsCount by remember { mutableStateOf(0) }

DisposableEffect(currentUserId) {
    if (currentUserId.isEmpty()) return@DisposableEffect onDispose { }
    
    val db = FirebaseFirestore.getInstance()
    val registration = db.collection("refunds")
        .whereEqualTo("seller_id", currentUserId)
        .whereEqualTo("status", RefundStatus.REQUESTED.toString())
        .addSnapshotListener { snapshot, _ ->
            pendingRefundsCount = snapshot?.size() ?: 0
        }
    
    onDispose { registration.remove() }
}
```

### Add Refund Management Card (in the dashboard grid, after Payments card):

```kotlin
// ══════════════════════════════════════════════════════════════════════════════
// Refund Management Card
// ══════════════════════════════════════════════════════════════════════════════

Card(
    onClick = { navController.navigate(Screen.SellerRefundManagement.route) },
    colors = CardDefaults.cardColors(containerColor = Color.White),
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
    modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (pendingRefundsCount > 0) Error.copy(alpha = 0.10f)
                        else Primary.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = if (pendingRefundsCount > 0) Error else Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "Refund Requests",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (pendingRefundsCount > 0)
                        "$pendingRefundsCount pending action(s)"
                    else "No pending requests",
                    fontSize = 12.sp,
                    color = if (pendingRefundsCount > 0) Error else TextSecondary
                )
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Red badge - only shown when there are pending refunds
            if (pendingRefundsCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Error, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pendingRefundsCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
```

### Add required imports in SellerDashboardScreen.kt:

```kotlin
import com.gcuf.craftoria.data.model.RefundStatus
import com.google.firebase.firestore.FirebaseFirestore
```

---

## 🎨 Visual Reference

### Dashboard Card States:

**No Pending Refunds:**
```
┌─────────────────────────────────────────────────┐
│  📄  Refund Requests                         ›  │
│      No pending requests                        │
└─────────────────────────────────────────────────┘
```

**With Pending Refunds:**
```
┌─────────────────────────────────────────────────┐
│  🔴  Refund Requests                    (3)  ›  │
│      3 pending action(s)                        │
└─────────────────────────────────────────────────┘
```

---

## 🧪 Testing Steps

### 1. Test Navigation
```
SellerDashboard → Refund Management → Refund Detail → Back
```

### 2. Test Real-time Badge
- Buyer creates refund request
- Seller dashboard badge updates instantly
- Count shows correct number

### 3. Test Role Guard
- Try accessing as buyer (should redirect)
- Try accessing as seller (should work)

### 4. Test Contact Buyer
- Click "Contact Buyer" button
- Should navigate to chat screen

---

## 📊 Complete Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Seller Dashboard                          │
│                                                              │
│  ┌──────────────────────────────────────────────────┐      │
│  │  📄 Refund Requests              (3) ›           │      │
│  │     3 pending action(s)                          │      │
│  └──────────────────────────────────────────────────┘      │
│                         │                                    │
│                         │ Click                              │
│                         ▼                                    │
│  ┌──────────────────────────────────────────────────┐      │
│  │        Refund Management Screen                  │      │
│  │                                                   │      │
│  │  Tabs: All | Pending(3) | Approved | Rejected   │      │
│  │                                                   │      │
│  │  ┌────────────────────────────────────────┐     │      │
│  │  │ REQUESTED • 2 hours ago                │     │      │
│  │  │ Unknown Buyer • Order #ABC123          │     │      │
│  │  │ PKR 1500                               │     │      │
│  │  │ Reason: Defective product              │     │      │
│  │  └────────────────────────────────────────┘     │      │
│  │                         │                         │      │
│  │                         │ Click                   │      │
│  │                         ▼                         │      │
│  │  ┌────────────────────────────────────────┐     │      │
│  │  │      Refund Detail Screen              │     │      │
│  │  │                                         │     │      │
│  │  │  Status: REQUESTED                     │     │      │
│  │  │  Order Info, Buyer Info, Details       │     │      │
│  │  │                                         │     │      │
│  │  │  [Approve Refund]  [Reject Refund]    │     │      │
│  │  └────────────────────────────────────────┘     │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Integration Checklist

- [ ] Screen routes added to NavGraph
- [ ] Composable routes added with role guards
- [ ] Imports added
- [ ] Dashboard card added
- [ ] Real-time badge listener added
- [ ] Firestore composite index created
- [ ] Tested navigation flow
- [ ] Tested real-time updates
- [ ] Tested role-based access
- [ ] Verified no compilation errors

---

## 🚨 Common Issues

### Issue: "Unresolved reference: SellerRefundManagementScreen"
**Solution:** Add import:
```kotlin
import com.gcuf.craftoria.ui.screens.seller.SellerRefundManagementScreen
```

### Issue: "Unresolved reference: RefundStatus"
**Solution:** Add import in SellerDashboardScreen:
```kotlin
import com.gcuf.craftoria.data.model.RefundStatus
```

### Issue: Badge not updating
**Solution:** Check:
- Firestore listener properly set up
- DisposableEffect cleanup working
- Refund status is "REQUESTED"

### Issue: Navigation not working
**Solution:** Verify:
- Routes added to Screen sealed class
- Composables added to NavHost
- navController passed correctly

---

**Status:** ✅ Ready for Integration

**Priority:** 🟢 Normal (after Firestore index)

**Testing:** Required before production
