# ⚡ INSTANT SCREEN LOADING - QUICK START GUIDE

## 🎯 Goal
Eliminate screen loading delays - screens open instantly with cached data

---

## 🚀 Quick Integration (3 Steps)

### **Step 1: Add to ViewModel**

```kotlin
import com.gcuf.craftoria.utils.ScreenLoadingOptimizer

class MyViewModel : ViewModel() {
    // ✅ Create optimizer
    private lateinit var optimizer: ScreenLoadingOptimizer<List<YourDataType>>
    
    // ✅ Expose state flows
    val data: StateFlow<List<YourDataType>> get() = optimizer.data
    val isFirstLoad: StateFlow<Boolean> get() = optimizer.isFirstLoad
    val isRefreshing: StateFlow<Boolean> get() = optimizer.isRefreshing
    
    // ✅ Load data function
    fun loadData(userId: String) {
        viewModelScope.launch {
            // Initialize optimizer with user-specific cache key
            optimizer = ScreenLoadingOptimizer(
                cacheKey = "my_data_$userId",
                emptyValue = emptyList()
            )
            
            // Load data (instant if cached!)
            optimizer.loadData {
                try {
                    val result = repository.getData(userId)
                    Result.success(result)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }
}
```

### **Step 2: Update Screen Composable**

```kotlin
@Composable
fun MyScreen(userId: String, viewModel: MyViewModel) {
    val data by viewModel.data.collectAsState()
    val isFirstLoad by viewModel.isFirstLoad.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // ✅ Load data
    LaunchedEffect(userId) {
        viewModel.loadData(userId)
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("My Screen") })
                // ✅ Show subtle progress bar during refresh
                if (isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ✅ Always show content
            ContentList(data)
            
            // ✅ Only show full loading on first visit with no data
            if (isFirstLoad && data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
```

### **Step 3: Clear Cache on Logout**

```kotlin
// In AuthViewModel or logout function
fun logout() {
    ScreenLoadingOptimizer.clearAllCaches()
    // ... rest of logout
}
```

---

## 📋 Common Patterns

### **Pattern 1: List Screen (Orders, Products, etc.)**
```kotlin
if (isFirstLoad && items.isEmpty()) {
    LoadingScreen()
} else if (items.isEmpty()) {
    EmptyStateScreen()
} else {
    LazyColumn {
        items(items) { item ->
            ItemCard(item)
        }
    }
}
```

### **Pattern 2: Detail Screen (Order Details, Product Details)**
```kotlin
if (isFirstLoad && item == null) {
    LoadingScreen()
} else if (item != null) {
    DetailContent(item)
} else {
    ErrorScreen()
}
```

### **Pattern 3: Dashboard with Stats**
```kotlin
Column {
    // Stats cards
    if (isFirstLoad && stats == null) {
        StatsLoadingSkeleton()
    } else {
        StatsCards(stats)
    }
    
    // Recent items
    if (isFirstLoad && items.isEmpty()) {
        ItemsLoadingSkeleton()
    } else {
        ItemsList(items)
    }
}
```

---

## 🎨 UI Components

### **Top Progress Bar (Recommended)**
```kotlin
if (isRefreshing) {
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = Primary,
        trackColor = Primary.copy(alpha = 0.2f)
    )
}
```

### **Pull-to-Refresh**
```kotlin
val pullRefreshState = rememberPullRefreshState(
    refreshing = isRefreshing,
    onRefresh = { viewModel.refreshData() }
)

Box(Modifier.pullRefresh(pullRefreshState)) {
    Content()
    PullRefreshIndicator(
        refreshing = isRefreshing,
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
```

### **Floating Refresh Button**
```kotlin
if (isRefreshing) {
    FloatingActionButton(
        onClick = { /* Already refreshing */ },
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color.White
        )
    }
}
```

---

## 🔧 Advanced Features

### **Preload Before Navigation**
```kotlin
// Before navigating to screen
Button(onClick = {
    scope.launch {
        ScreenTransitionOptimizer.preloadScreen("orders") {
            orderViewModel.loadOrders(userId)
        }
        navController.navigate("orders")
    }
}) {
    Text("View Orders")
}
```

### **Custom Cache Duration**
```kotlin
ScreenLoadingOptimizer(
    cacheKey = "my_data",
    emptyValue = emptyList(),
    cacheDurationMs = 10 * 60 * 1000 // 10 minutes
)
```

### **Force Refresh**
```kotlin
fun refreshData() {
    viewModelScope.launch {
        optimizer.loadData(forceRefresh = true) {
            repository.getData()
        }
    }
}
```

### **Manual Cache Update (for Realtime)**
```kotlin
// When receiving realtime update
fun onRealtimeUpdate(newData: List<Item>) {
    optimizer.updateData(newData)
}
```

---

## ✅ Checklist for Each Screen

- [ ] Add `ScreenLoadingOptimizer` to ViewModel
- [ ] Expose `data`, `isFirstLoad`, `isRefreshing` StateFlows
- [ ] Update screen to show cached data instantly
- [ ] Only show full loading on first visit
- [ ] Add subtle refresh indicator for background updates
- [ ] Handle empty state separately from loading state
- [ ] Test first visit (should show loading)
- [ ] Test second visit (should show cached data instantly)
- [ ] Test background refresh (should show subtle indicator)

---

## 🎯 Priority Screens to Update

### **Must Update** (High Traffic):
1. MyOrdersScreen
2. SellerOrdersScreen
3. PaymentHistoryScreen
4. SellerPaymentsScreen
5. HomeScreen
6. CartScreen
7. MyChatsScreen

### **Should Update** (Medium Traffic):
8. ManageProductsScreen
9. WishlistScreen
10. NotificationsScreen
11. SellerDashboardScreen

### **Nice to Update** (Low Traffic):
12. AllStoresScreen
13. SearchScreen
14. LearningResourcesScreen

---

## 🐛 Troubleshooting

### **Problem**: Data not showing instantly
**Solution**: Check cache key is consistent between loads

### **Problem**: Loading shows every time
**Solution**: Ensure `isFirstLoad` is used, not a custom loading state

### **Problem**: Stale data showing
**Solution**: Reduce `cacheDurationMs` or call `clearCache()` when needed

### **Problem**: Memory issues
**Solution**: Cache automatically expires after duration, or call `clearAllCaches()` on logout

---

## 📊 Before vs After

### **Before**:
```
Click Screen → Empty State (100ms) → Loading (500-2000ms) → Content
User sees: Delay, flickering, poor UX
```

### **After**:
```
Click Screen → Cached Content (0ms) → Background Refresh → Updated Content
User sees: Instant, smooth, professional
```

---

## 💡 Pro Tips

1. **Use unique cache keys per user**: `"orders_$userId"` not just `"orders"`
2. **Clear cache on logout**: Prevents data leaks between users
3. **Preload before navigation**: Makes transitions instant
4. **Use subtle indicators**: Top progress bar > full screen loading
5. **Handle empty state**: Show empty state, not loading, when no data
6. **Test both scenarios**: First visit AND subsequent visits

---

## 📝 Example: Complete Integration

```kotlin
// ViewModel
class OrderViewModel : ViewModel() {
    private lateinit var optimizer: ScreenLoadingOptimizer<List<Order>>
    
    val orders: StateFlow<List<Order>> get() = optimizer.data
    val isFirstLoad: StateFlow<Boolean> get() = optimizer.isFirstLoad
    val isRefreshing: StateFlow<Boolean> get() = optimizer.isRefreshing
    
    fun loadOrders(userId: String) {
        viewModelScope.launch {
            optimizer = ScreenLoadingOptimizer(
                cacheKey = "orders_$userId",
                emptyValue = emptyList()
            )
            
            optimizer.loadData {
                try {
                    val orders = repository.getOrders(userId)
                    Result.success(orders)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }
}

// Screen
@Composable
fun OrdersScreen(userId: String, viewModel: OrderViewModel) {
    val orders by viewModel.orders.collectAsState()
    val isFirstLoad by viewModel.isFirstLoad.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    LaunchedEffect(userId) {
        viewModel.loadOrders(userId)
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Orders") })
                if (isRefreshing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isFirstLoad && orders.isEmpty() -> LoadingScreen()
                orders.isEmpty() -> EmptyOrdersScreen()
                else -> OrdersList(orders)
            }
        }
    }
}
```

---

**Status**: ✅ Ready to use  
**File**: `app/src/main/java/com/gcuf/craftoria/utils/ScreenLoadingOptimizer.kt`  
**Documentation**: `INSTANT_SCREEN_LOADING_IMPLEMENTATION_COMPLETE.md`
