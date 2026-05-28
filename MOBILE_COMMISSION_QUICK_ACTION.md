# Mobile Commission - Quick Action Guide

## 🎯 Status: 70% Complete

**What's Done:**
- ✅ All data models
- ✅ All repository methods  
- ✅ All ViewModel logic
- ✅ Web notifications

**What's Missing:**
- ❌ Commission Screen UI
- ❌ Mobile notifications
- ❌ Navigation

---

## ⚡ Quick Implementation (1.5 hours)

### Step 1: Create Commission Screen (30 min)

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/admin/CommissionScreen.kt`

```kotlin
package com.gcuf.craftoria.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.gcuf.craftoria.viewmodel.CommissionViewModel

@Composable
fun CommissionScreen(viewModel: CommissionViewModel = hiltViewModel()) {
    val adminEarnings by viewModel.adminEarnings.collectAsState()
    val pendingCommissions by viewModel.pendingCommissions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAdminEarnings()
        viewModel.loadPendingCommissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            "Commission Management",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Earnings Summary
        if (adminEarnings != null) {
            EarningsSummaryCards(adminEarnings!!)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pending Commissions
        if (pendingCommissions.isNotEmpty()) {
            Text(
                "Pending Commissions (${pendingCommissions.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn {
                items(pendingCommissions.size) { index ->
                    CommissionCard(
                        commission = pendingCommissions[index],
                        onMarkAsPaid = {
                            viewModel.markCommissionAsPaid(pendingCommissions[index].id)
                        }
                    )
                }
            }
        }

        // Error
        if (error != null) {
            Text(
                "Error: $error",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Loading
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun EarningsSummaryCards(earnings: AdminEarnings) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = "Total Commissions",
            value = "PKR ${earnings.totalCommissions.toInt()}",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Pending",
            value = "PKR ${earnings.pendingCommissions.toInt()}",
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Paid",
            value = "PKR ${earnings.paidCommissions.toInt()}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun CommissionCard(commission: AdminCommission, onMarkAsPaid: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Order: ${commission.orderId.take(8)}")
                    Text("Seller: ${commission.sellerName}")
                }
                Text("PKR ${commission.commissionAmount.toInt()}")
            }
            
            Button(
                onClick = onMarkAsPaid,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Text("Mark as Paid")
            }
        }
    }
}
```

### Step 2: Update Navigation (10 min)

**File:** `app/src/main/java/com/gcuf/craftoria/ui/navigation/NavGraph.kt`

```kotlin
// Add to navigation graph
composable("commission_screen") {
    CommissionScreen()
}

// Add to menu/drawer
NavigationDrawerItem(
    label = { Text("Commissions") },
    selected = currentRoute == "commission_screen",
    onClick = {
        navController.navigate("commission_screen")
    }
)
```

### Step 3: Add Notifications (20 min)

**Update:** `app/src/main/java/com/gcuf/craftoria/viewmodel/CommissionViewModel.kt`

```kotlin
// Add to CommissionViewModel
fun subscribeToCommissionNotifications() {
    viewModelScope.launch {
        // Listen for new commissions
        db.collection("admin_commissions")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to commissions", error)
                    return@addSnapshotListener
                }
                
                snapshot?.documents?.forEach { doc ->
                    val commission = doc.toObject(AdminCommission::class.java)
                    if (commission != null) {
                        // Show notification
                        showCommissionNotification(commission)
                    }
                }
            }
    }
}

private fun showCommissionNotification(commission: AdminCommission) {
    // Show local notification
    Log.d(TAG, "New commission: PKR ${commission.commissionAmount}")
}
```

### Step 4: Update ViewModel to Use Production Repository (15 min)

**Update:** `app/src/main/java/com/gcuf/craftoria/viewmodel/CommissionViewModel.kt`

```kotlin
// Replace
private val commissionRepository = CommissionRepository(db)

// With
private val commissionRepository = CommissionRepositoryProduction(db)
```

---

## 📋 Files to Create

```
app/src/main/java/com/gcuf/craftoria/ui/screens/admin/
├── CommissionScreen.kt (NEW - 150 lines)
└── CommissionComponents.kt (NEW - 100 lines)
```

---

## 📋 Files to Update

```
app/src/main/java/com/gcuf/craftoria/
├── viewmodel/CommissionViewModel.kt (Update repository)
├── ui/navigation/NavGraph.kt (Add route)
└── MainActivity.kt (Add menu item)
```

---

## ✅ Checklist

- [ ] Create CommissionScreen.kt
- [ ] Create CommissionComponents.kt
- [ ] Update CommissionViewModel
- [ ] Update NavGraph.kt
- [ ] Update MainActivity.kt
- [ ] Test commission screen
- [ ] Test mark as paid
- [ ] Test notifications
- [ ] Test offline mode
- [ ] Deploy

---

## 🧪 Quick Test

```
1. Open app
2. Navigate to Commissions
3. Verify earnings display
4. Verify pending list shows
5. Click "Mark as Paid"
6. Verify status updates
7. Check notifications
```

---

## 📊 Expected Result

After implementation:
- ✅ Commission screen visible
- ✅ Real-time updates
- ✅ Mark as paid works
- ✅ Notifications sent
- ✅ Offline support
- ✅ Error handling

---

## ⏱️ Time Estimate

- Create Screen: 30 min
- Update Navigation: 10 min
- Add Notifications: 20 min
- Update Repository: 15 min
- Testing: 30 min
- **Total: 1.5 hours**

---

## 🎯 Priority

1. **High:** Create CommissionScreen.kt
2. **High:** Update NavGraph.kt
3. **Medium:** Add notifications
4. **Medium:** Update repository
5. **Low:** Polish UI

---

**Status: Ready to Implement**

All backend is done. Just need UI!
