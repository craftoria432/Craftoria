# Real-Time Payment & Earnings Updates System

## Overview
When a seller completes a product or co-seller product, the payment and earnings update in **real-time** across all seller screens. This document explains the complete flow.

---

## 1. Real-Time Update Architecture

### Three Main Components:

#### A. **Dashboard Screen** (Seller Dashboard)
- **File**: `SellerDashboardScreen.kt`
- **ViewModel**: `DashboardViewModel.kt`
- **Real-Time Listener**: ✅ **ACTIVE**

#### B. **Seller Payments Screen** (Payment History)
- **File**: `SellerPaymentsScreen.kt`
- **ViewModel**: `SellerPaymentViewModel.kt`
- **Real-Time Listener**: ✅ **ACTIVE** (via repository)

#### C. **Co-Seller Payment Screen** (Store Payment Dashboard)
- **File**: `CoSellerStorePaymentScreen.kt`
- **ViewModel**: `CoSellerStorePaymentViewModel.kt`
- **Real-Time Listener**: ✅ **ACTIVE** (via repository)

---

## 2. How Real-Time Updates Work

### Step 1: Initial Load
When a seller opens any payment screen:

```kotlin
// Example: SellerPaymentsScreen
LaunchedEffect(sellerId) {
    viewModel.loadSellerPayments(sellerId)      // Initial load
    viewModel.loadPaymentStats(sellerId)        // Load stats
}
```

### Step 2: Real-Time Listener Activation
After initial load, a **Firestore snapshot listener** is activated:

```kotlin
// From DashboardViewModel.kt
fun startRealtimeDashboardListener(sellerId: String) {
    // Remove old listeners
    statsListenerRegistration?.remove()
    activitiesListenerRegistration?.remove()
    
    val db = FirebaseFirestore.getInstance()
    
    // ✅ Real-time listener for seller_payments
    statsListenerRegistration = db.collection("seller_payments")
        .whereEqualTo("seller_id", sellerId)
        .addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                Log.d("DashboardViewModel", "🔄 Real-time payment update received")
                // Fetch updated stats
                val statsResult = dashboardRepository.getDashboardStats(sellerId)
                if (statsResult.isSuccess) {
                    _dashboardStats.value = statsResult.getOrNull()
                    Log.d("DashboardViewModel", "✅ Dashboard stats updated in real-time")
                }
            }
        }
}
```

### Step 3: Automatic Updates When Product Completes
When a seller marks a product as completed:

1. **Order status changes** in Firestore
2. **Firestore triggers** payment calculation
3. **seller_payments collection** is updated
4. **Snapshot listener detects change** → Triggers callback
5. **ViewModel fetches new data** → Updates UI state
6. **Compose recomposes** → UI updates automatically

---

## 3. Real-Time Update Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ Seller Completes Product                                    │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Order Status Updated in Firestore                           │
│ (orders collection)                                         │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Cloud Function / Firestore Trigger                          │
│ Calculates Payment & Updates seller_payments                │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Firestore Snapshot Listener Detects Change                  │
│ (seller_payments collection)                                │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ ViewModel Callback Triggered                                │
│ - DashboardViewModel                                        │
│ - SellerPaymentViewModel                                    │
│ - CoSellerStorePaymentViewModel                             │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ Repository Fetches Updated Data                             │
│ - getDashboardStats()                                       │
│ - getSellerPayments()                                       │
│ - getStoreRevenue()                                         │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ StateFlow Updated                                           │
│ _dashboardStats.value = newStats                            │
│ _paymentState.value = newPayments                           │
│ _revenueState.value = newRevenue                            │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│ UI Recomposes Automatically                                 │
│ - Dashboard stats update                                    │
│ - Payment list refreshes                                    │
│ - Revenue cards update                                      │
│ - All screens show latest data                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Screens That Update in Real-Time

### A. Seller Dashboard Screen
**What Updates:**
- Total Earnings
- Completed Orders Count
- Pending Payments
- Recent Activities
- Sales Overview

**How It Works:**
```kotlin
// DashboardViewModel.kt
fun loadDashboardData(sellerId: String) {
    viewModelScope.launch {
        // Initial load
        val statsResult = dashboardRepository.getDashboardStats(sellerId)
        _dashboardStats.value = statsResult.getOrNull()
        
        // Start real-time listener
        startRealtimeDashboardListener(sellerId)
    }
}
```

### B. Seller Payments Screen
**What Updates:**
- Payment History List
- Total Earnings Card
- Completed Amount
- Pending Amount
- Payment Status Badges

**How It Works:**
```kotlin
// SellerPaymentsScreen.kt
LaunchedEffect(sellerId) {
    viewModel.loadSellerPayments(sellerId)    // Loads & starts listener
    viewModel.loadPaymentStats(sellerId)      // Loads & starts listener
}

// When payment updates:
// 1. Listener detects change
// 2. ViewModel fetches new payments
// 3. paymentState StateFlow updates
// 4. LazyColumn recomposes with new data
```

### C. Payment Detail Screen
**What Updates:**
- Payment Status
- Payment Amount
- Timeline Status
- Refund Status (if applicable)

**How It Works:**
```kotlin
// PaymentDetailScreen.kt
LaunchedEffect(paymentId) {
    viewModel.loadPaymentDetail(paymentId)    // Loads & starts listener
}

// Real-time updates via selectedPayment StateFlow
val selectedPayment by viewModel.selectedPayment.collectAsState()
```

### D. Co-Seller Store Payment Screen
**What Updates:**
- Total Revenue
- Completed Revenue
- Pending Revenue
- Order Count
- Payment List
- Payment Split Details

**How It Works:**
```kotlin
// CoSellerStorePaymentScreen.kt
LaunchedEffect(storeId) {
    viewModel.loadStorePayments(storeId)      // Loads & starts listener
    viewModel.loadStoreRevenue(storeId, startDate, endDate)
}

// Real-time updates via:
// - paymentState StateFlow
// - revenueState StateFlow
```

---

## 5. Real-Time Listener Details

### Firestore Listeners Active:

#### Dashboard ViewModel:
```kotlin
// Listener 1: seller_payments collection
db.collection("seller_payments")
    .whereEqualTo("seller_id", sellerId)
    .addSnapshotListener { snapshot, error -> ... }

// Listener 2: activities collection
db.collection("activities")
    .whereEqualTo("seller_id", sellerId)
    .orderBy("created_at", DESCENDING)
    .limit(15)
    .addSnapshotListener { snapshot, error -> ... }
```

#### Seller Payment ViewModel:
```kotlin
// Listener: seller_payments collection
db.collection("seller_payments")
    .whereEqualTo("seller_id", sellerId)
    .addSnapshotListener { snapshot, error -> ... }
```

#### Co-Seller Payment ViewModel:
```kotlin
// Listener: seller_payments collection (filtered by store)
db.collection("seller_payments")
    .whereEqualTo("co_seller_store_id", storeId)
    .addSnapshotListener { snapshot, error -> ... }

// Listener: store_revenue collection
db.collection("store_revenue")
    .whereEqualTo("store_id", storeId)
    .addSnapshotListener { snapshot, error -> ... }
```

---

## 6. Update Frequency & Performance

### Real-Time Characteristics:
- **Latency**: ~100-500ms (depends on network)
- **Frequency**: Immediate on any change
- **Scope**: Only affected seller/store
- **Efficiency**: Snapshot listeners only trigger on actual changes

### Listener Cleanup:
```kotlin
// Listeners are automatically removed when:
override fun onCleared() {
    super.onCleared()
    statsListenerRegistration?.remove()
    activitiesListenerRegistration?.remove()
    Log.d("DashboardViewModel", "🔴 Real-time listeners removed")
}
```

---

## 7. What Happens When Product Completes

### Sequence of Events:

1. **Seller marks product as completed**
   - Order status → "COMPLETED"
   - Firestore updates order document

2. **Firestore Trigger/Cloud Function**
   - Detects order completion
   - Calculates payment amount
   - Deducts platform fees
   - Creates/updates seller_payment document

3. **Snapshot Listener Fires**
   - All active listeners detect the change
   - Callbacks execute in ViewModels

4. **ViewModel Updates**
   ```kotlin
   // Example from DashboardViewModel
   val statsResult = dashboardRepository.getDashboardStats(sellerId)
   _dashboardStats.value = statsResult.getOrNull()
   ```

5. **UI Recomposes**
   - Compose detects StateFlow change
   - Affected composables recompose
   - New data displays immediately

6. **All Screens Update Simultaneously**
   - Dashboard: Total earnings updated
   - Payments Screen: New payment appears
   - Payment Detail: Status changes
   - Co-Seller Screen: Revenue updated

---

## 8. Testing Real-Time Updates

### How to Verify:

1. **Open Seller Dashboard** (or any payment screen)
2. **Complete a product** (from another device/browser)
3. **Observe automatic updates** on the dashboard:
   - Total earnings increases
   - Completed amount increases
   - New payment appears in list
   - Status badges update

### Debug Logs:
```
🔴 Starting real-time dashboard listener for: seller123
🔄 Real-time payment update received
✅ Dashboard stats updated in real-time
```

---

## 9. Key Files Involved

| File | Purpose |
|------|---------|
| `DashboardViewModel.kt` | Dashboard real-time updates |
| `SellerPaymentViewModel.kt` | Payment history real-time updates |
| `CoSellerStorePaymentViewModel.kt` | Co-seller payment real-time updates |
| `PaymentRepository.kt` | Fetches payment data |
| `DashboardRepository.kt` | Fetches dashboard stats |
| `CoSellerStorePaymentRepository.kt` | Fetches co-seller payment data |
| `SellerPaymentsScreen.kt` | Displays payment history |
| `PaymentDetailScreen.kt` | Displays payment details |
| `CoSellerStorePaymentScreen.kt` | Displays co-seller payments |
| `SellerDashboardScreen.kt` | Displays dashboard stats |

---

## 10. Summary

✅ **Real-time updates are FULLY IMPLEMENTED**

When a seller completes a product:
- Payment data updates in **real-time** (100-500ms)
- All payment screens refresh automatically
- Dashboard stats update immediately
- Co-seller payment screens show new revenue
- No manual refresh needed
- Listeners remain active as long as screen is open
- Listeners clean up when screen closes

The system uses **Firestore Snapshot Listeners** to detect changes and **StateFlow** to propagate updates to the UI, ensuring a seamless real-time experience.
