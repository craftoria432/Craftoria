# Co-Seller Order Detail Screen - Real-Time Update Fix Required

## ⚠️ Issue Identified

The `CoSellerOrderDetailScreen.kt` currently does **NOT** have real-time updates implemented. It uses a one-time fetch approach that requires manual screen refresh to see payment status changes.

## Current Implementation (Problematic)

```kotlin
// CoSellerOrderDetailScreen.kt - Line 42
LaunchedEffect(paymentId) {
    try {
        val repo = PaymentRepository()
        val result = repo.getPaymentById(paymentId, currentUserId)
        if (result.isSuccess) {
            payment = result.getOrNull()  // ❌ ONE-TIME fetch only
        }
    } catch (e: Exception) {
        error = e.message
    } finally {
        isLoading = false
    }
}
```

### Problem:
- `LaunchedEffect(paymentId)` runs **ONCE** when screen opens
- `getPaymentById()` is a one-time Firestore `.get()` call
- No snapshot listener attached
- When payment status changes in Firestore (Pending → Completed), screen does NOT update
- User must close and reopen screen to see changes

## ✅ Recommended Fix: Add Real-Time Listener

Replace the one-time fetch with a Firestore snapshot listener:

```kotlin
// ✅ RECOMMENDED: Real-time implementation
@Composable
fun CoSellerOrderDetailScreen(
    paymentId: String,
    currentUserId: String,
    onBackClick: () -> Unit
) {
    var payment by remember { mutableStateOf<SellerPayment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // ✅ Real-time listener using DisposableEffect
    DisposableEffect(paymentId) {
        val db = FirebaseFirestore.getInstance()
        
        // Attach snapshot listener
        val listener = db.collection("seller_payments")
            .document(paymentId)
            .addSnapshotListener { snapshot, firestoreError ->
                if (firestoreError != null) {
                    Log.e("CoSellerOrderDetail", "❌ Listener error", firestoreError)
                    error = firestoreError.message
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val fetchedPayment = snapshot.toObject(SellerPayment::class.java)?.copy(id = snapshot.id)
                        
                        // ✅ Security check: Verify user owns this payment
                        if (fetchedPayment?.sellerId == currentUserId) {
                            payment = fetchedPayment
                            error = null
                            Log.d("CoSellerOrderDetail", "✅ Real-time update: ${fetchedPayment.status}")
                        } else {
                            error = "Unauthorized access"
                            Log.w("CoSellerOrderDetail", "🚫 Unauthorized: User $currentUserId attempted to access payment $paymentId")
                        }
                    } catch (e: Exception) {
                        Log.e("CoSellerOrderDetail", "❌ Parse error", e)
                        error = e.message
                    }
                } else {
                    error = "Payment not found"
                }
                
                isLoading = false
            }
        
        // ✅ Clean up listener when screen closes
        onDispose {
            listener.remove()
            Log.d("CoSellerOrderDetail", "🔴 Listener removed")
        }
    }

    // Rest of the UI code remains the same...
    Scaffold(
        // ... existing UI code
    ) { paddingValues ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }

            error != null -> OrderDetailErrorView(
                error = error!!,
                onBackClick = onBackClick,
                modifier = Modifier.padding(paddingValues)
            )

            payment != null -> {
                // ✅ UI automatically updates when payment changes
                val p = payment!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrderAmountCard(p)
                    OrderInfoCard(p)
                    OrderItemsCard(p)
                    if (p.paymentSplits.isNotEmpty()) {
                        PaymentSplitCard(p, currentUserId)
                    }
                    OrderTimelineCard(p)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
```

## Key Changes:

1. **Replace `LaunchedEffect` with `DisposableEffect`**
   - `DisposableEffect` allows cleanup when screen closes
   - Prevents memory leaks from active listeners

2. **Use `addSnapshotListener()` instead of `get()`**
   - Firestore snapshot listener detects document changes automatically
   - Triggers callback whenever payment status updates

3. **Add `onDispose` cleanup**
   - Removes listener when screen closes
   - Prevents unnecessary background listeners

4. **Security check remains**
   - Verify `sellerId == currentUserId` before displaying data
   - Prevents unauthorized access

## Benefits After Fix:

✅ **Real-time updates**: Payment status changes appear instantly
✅ **No manual refresh**: User doesn't need to close/reopen screen
✅ **Consistent with other screens**: Matches `BuyerPaymentViewModel` and `SellerPaymentViewModel` patterns
✅ **Better UX**: Co-seller sees order completion immediately

## Testing After Fix:

1. Open co-seller order detail screen (shows "Pending")
2. From another device/session, mark order as completed
3. ✅ Screen should update automatically to show "Completed" status
4. Payment amount, timeline, and badges should all update instantly
5. No manual refresh required

## Alternative: ViewModel Approach

If you prefer using a ViewModel (more scalable for complex screens):

```kotlin
class CoSellerOrderDetailViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "CoSellerOrderDetailVM"
    
    private val _paymentState = MutableStateFlow<SellerPayment?>(null)
    val paymentState: StateFlow<SellerPayment?> = _paymentState
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private var paymentListener: ListenerRegistration? = null
    
    fun startRealtimePaymentListener(paymentId: String, currentUserId: String) {
        paymentListener?.remove()
        
        paymentListener = db.collection("seller_payments")
            .document(paymentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listener error", error)
                    _error.value = error.message
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val payment = snapshot.toObject(SellerPayment::class.java)?.copy(id = snapshot.id)
                        
                        if (payment?.sellerId == currentUserId) {
                            _paymentState.value = payment
                            _error.value = null
                            Log.d(TAG, "✅ Real-time update: ${payment.status}")
                        } else {
                            _error.value = "Unauthorized access"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Parse error", e)
                        _error.value = e.message
                    }
                } else {
                    _error.value = "Payment not found"
                }
                
                _isLoading.value = false
            }
    }
    
    override fun onCleared() {
        super.onCleared()
        paymentListener?.remove()
        Log.d(TAG, "🔴 Listener removed")
    }
}
```

Then in the screen:

```kotlin
@Composable
fun CoSellerOrderDetailScreen(
    paymentId: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    viewModel: CoSellerOrderDetailViewModel = viewModel()
) {
    val payment by viewModel.paymentState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(paymentId) {
        viewModel.startRealtimePaymentListener(paymentId, currentUserId)
    }
    
    // Rest of UI code...
}
```

## Priority: HIGH

This fix is important for:
- **User Experience**: Co-sellers expect to see order updates immediately
- **Consistency**: Other screens (Buyer Payment History, Seller Payments) already have real-time updates
- **Production Readiness**: Real-time updates are a core feature of modern apps

## Estimated Implementation Time: 30-45 minutes

---

**Status**: ⚠️ Fix Required
**Impact**: Medium-High (affects co-seller user experience)
**Complexity**: Low (straightforward implementation following existing patterns)
