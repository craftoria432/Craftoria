# Refund System Integration Guide

## Step 1: Add Firestore Rules

Update `firestore.rules` to include refunds collection:

```firestore
// Refunds collection
match /refunds/{refundId} {
  // Buyers can read their own refunds
  allow read: if request.auth.uid == resource.data.buyer_id;
  
  // Sellers can read refunds for their orders
  allow read: if request.auth.uid == resource.data.seller_id;
  
  // Admins can read all refunds
  allow read: if request.auth.token.admin == true;
  
  // Buyers can create refund requests
  allow create: if request.auth.uid == request.resource.data.buyer_id
    && request.resource.data.status == 'requested'
    && request.resource.data.initiated_by == 'buyer';
  
  // Sellers can create refund requests
  allow create: if request.auth.uid == request.resource.data.seller_id
    && request.resource.data.status == 'requested'
    && request.resource.data.initiated_by == 'seller';
  
  // Only admins can update refund status
  allow update: if request.auth.token.admin == true;
  
  // Prevent deletion
  allow delete: if false;
}
```

## Step 2: Create Firestore Indexes

Add to `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "refunds",
      "queryScope": "Collection",
      "fields": [
        {"fieldPath": "buyer_id", "order": "ASCENDING"},
        {"fieldPath": "requested_at", "order": "DESCENDING"}
      ]
    },
    {
      "collectionGroup": "refunds",
      "queryScope": "Collection",
      "fields": [
        {"fieldPath": "seller_id", "order": "ASCENDING"},
        {"fieldPath": "requested_at", "order": "DESCENDING"}
      ]
    },
    {
      "collectionGroup": "refunds",
      "queryScope": "Collection",
      "fields": [
        {"fieldPath": "status", "order": "ASCENDING"},
        {"fieldPath": "requested_at", "order": "ASCENDING"}
      ]
    },
    {
      "collectionGroup": "refunds",
      "queryScope": "Collection",
      "fields": [
        {"fieldPath": "status", "order": "ASCENDING"},
        {"fieldPath": "retry_count", "order": "ASCENDING"}
      ]
    }
  ]
}
```

## Step 3: Update Order Model

Add refund-related fields to Order.kt:

```kotlin
@IgnoreExtraProperties
data class Order(
    // ... existing fields ...
    
    // Refund tracking
    @get:PropertyName("refund_requested")
    @set:PropertyName("refund_requested")
    var refundRequested: Boolean = false,

    @get:PropertyName("refund_id")
    @set:PropertyName("refund_id")
    var refundId: String = "",

    @get:PropertyName("refund_status")
    @set:PropertyName("refund_status")
    var refundStatus: String = "",

    @get:PropertyName("refund_amount")
    @set:PropertyName("refund_amount")
    var refundAmount: Double = 0.0
)
```

## Step 4: Create Refund UI Screens

### Buyer Refund Request Screen

```kotlin
@Composable
fun BuyerRefundRequestScreen(
    orderId: String,
    viewModel: RefundViewModel = hiltViewModel()
) {
    var selectedReason by remember { mutableStateOf("") }
    var reasonDetails by remember { mutableStateOf("") }
    var refundAmount by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Request Refund", style = MaterialTheme.typography.headlineSmall)
        
        // Reason dropdown
        DropdownMenu(
            label = "Reason",
            options = RefundReason.values().map { it.getDisplayName() },
            selectedOption = selectedReason,
            onOptionSelected = { selectedReason = it }
        )
        
        // Details text field
        CraftoriaTextField(
            value = reasonDetails,
            onValueChange = { reasonDetails = it },
            label = "Additional Details",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        
        // Refund amount
        CraftoriaTextField(
            value = refundAmount,
            onValueChange = { refundAmount = it },
            label = "Refund Amount",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        // Submit button
        CraftoriaButton(
            text = "Request Refund",
            onClick = {
                viewModel.initiateRefund(
                    orderId = orderId,
                    paymentId = "...",
                    buyerId = currentUserId,
                    buyerName = currentUserName,
                    sellerId = order.sellerId,
                    sellerName = order.sellerName,
                    refundType = "FULL",
                    originalAmount = order.totalPrice,
                    refundAmount = refundAmount.toDouble(),
                    reason = selectedReason,
                    reasonDetails = reasonDetails,
                    paymentMethod = order.paymentMethod,
                    transactionId = payment.transactionId,
                    initiatedBy = "buyer"
                )
            }
        )
    }
}
```

### Refund History Screen

```kotlin
@Composable
fun RefundHistoryScreen(
    buyerId: String,
    viewModel: RefundViewModel = hiltViewModel()
) {
    LaunchedEffect(buyerId) {
        viewModel.getRefundsByBuyer(buyerId)
    }

    val refunds by viewModel.refundList.collectAsState()
    val state by viewModel.refundState.collectAsState()

    when (state) {
        is RefundUIState.Loading -> {
            CircularProgressIndicator()
        }
        is RefundUIState.RefundsLoaded -> {
            LazyColumn {
                items(refunds) { refund ->
                    RefundHistoryCard(refund)
                }
            }
        }
        is RefundUIState.Error -> {
            Text("Error loading refunds")
        }
        else -> {}
    }
}

@Composable
fun RefundHistoryCard(refund: RefundRequest) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Refund #${refund.id.take(8)}")
                Text(
                    refund.getStatusEnum().getDisplayName(),
                    color = Color(android.graphics.Color.parseColor(refund.getStatusEnum().getStatusColor()))
                )
            }
            
            Text("Amount: ₹${refund.refundAmount}")
            Text("Reason: ${refund.reason}")
            Text("Requested: ${formatDate(refund.requestedAt)}")
            
            if (refund.getStatusEnum() == RefundStatus.COMPLETED) {
                Text("Completed: ${formatDate(refund.completedAt ?: 0L)}")
            }
        }
    }
}
```

### Admin Refund Management Screen

```kotlin
@Composable
fun AdminRefundManagementScreen(
    viewModel: RefundViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.getPendingRefunds()
    }

    val refunds by viewModel.refundList.collectAsState()
    var selectedRefund by remember { mutableStateOf<RefundRequest?>(null) }

    if (selectedRefund != null) {
        RefundApprovalDialog(
            refund = selectedRefund!!,
            onApprove = { notes ->
                viewModel.approveRefund(
                    selectedRefund!!.id,
                    currentUserId,
                    currentUserName,
                    notes
                )
                selectedRefund = null
            },
            onReject = { reason ->
                viewModel.rejectRefund(
                    selectedRefund!!.id,
                    currentUserId,
                    currentUserName,
                    reason
                )
                selectedRefund = null
            },
            onDismiss = { selectedRefund = null }
        )
    }

    LazyColumn {
        items(refunds) { refund ->
            RefundManagementCard(
                refund = refund,
                onClick = { selectedRefund = it }
            )
        }
    }
}
```

## Step 5: Integrate with Order Cancellation

Update order cancellation logic:

```kotlin
// In OrderRepository or OrderViewModel
suspend fun cancelOrder(orderId: String, reason: String) {
    try {
        // Update order status
        firestore.collection("orders")
            .document(orderId)
            .update(
                "status" to OrderStatus.CANCELLED.toString(),
                "cancelled_at" to System.currentTimeMillis()
            )
            .await()

        // Get order details
        val order = getOrderById(orderId).getOrNull() ?: return

        // Initiate refund
        refundProcessor.initiateRefund(
            orderId = orderId,
            paymentId = order.id,
            buyerId = order.buyerId,
            buyerName = order.buyerName,
            sellerId = order.sellerId,
            sellerName = order.sellerName,
            refundType = "FULL",
            originalAmount = order.totalPrice,
            refundAmount = order.totalPrice,
            reason = "ORDER_CANCELLED",
            reasonDetails = reason,
            paymentMethod = order.paymentMethod,
            transactionId = payment.transactionId,
            initiatedBy = "buyer"
        )
    } catch (e: Exception) {
        Log.e("OrderCancellation", "Error cancelling order", e)
    }
}
```

## Step 6: Add Notification Integration

```kotlin
// In RefundProcessor or RefundViewModel
private suspend fun sendRefundNotification(refund: RefundRequest) {
    when (refund.getStatusEnum()) {
        RefundStatus.REQUESTED -> {
            notificationService.sendNotification(
                userId = refund.sellerId,
                title = "Refund Request",
                body = "Refund request from ${refund.buyerName} for ₹${refund.refundAmount}",
                data = mapOf("refundId" to refund.id)
            )
        }
        RefundStatus.APPROVED -> {
            notificationService.sendNotification(
                userId = refund.buyerId,
                title = "Refund Approved",
                body = "Your refund of ₹${refund.refundAmount} has been approved",
                data = mapOf("refundId" to refund.id)
            )
        }
        RefundStatus.COMPLETED -> {
            notificationService.sendNotification(
                userId = refund.buyerId,
                title = "Refund Completed",
                body = "Your refund of ₹${refund.refundAmount} has been processed",
                data = mapOf("refundId" to refund.id)
            )
        }
        RefundStatus.REJECTED -> {
            notificationService.sendNotification(
                userId = refund.buyerId,
                title = "Refund Rejected",
                body = "Your refund request has been rejected",
                data = mapOf("refundId" to refund.id)
            )
        }
        else -> {}
    }
}
```

## Step 7: Add to Dependency Injection

If using Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RefundModule {

    @Provides
    @Singleton
    fun provideRefundRepository(
        firestore: FirebaseFirestore
    ): RefundRepository = RefundRepository(firestore)

    @Provides
    @Singleton
    fun provideRefundProcessor(
        refundRepository: RefundRepository
    ): RefundProcessor = RefundProcessor(refundRepository)
}

@HiltViewModel
class RefundViewModel @Inject constructor(
    private val refundRepository: RefundRepository,
    private val refundProcessor: RefundProcessor
) : ViewModel() {
    // ... implementation
}
```

## Step 8: Testing

```kotlin
class RefundProcessorTest {

    @Test
    fun testInitiateRefund() = runTest {
        val result = refundProcessor.initiateRefund(
            orderId = "order_123",
            paymentId = "payment_456",
            buyerId = "buyer_789",
            buyerName = "John Doe",
            sellerId = "seller_101",
            sellerName = "Store ABC",
            refundType = "FULL",
            originalAmount = 5000.0,
            refundAmount = 5000.0,
            reason = "DEFECTIVE_PRODUCT",
            reasonDetails = "Product damaged",
            paymentMethod = "stripe",
            transactionId = "ch_123",
            initiatedBy = "buyer"
        )

        assertTrue(result.isSuccess)
        val refund = result.getOrNull()!!
        assertEquals(RefundStatus.REQUESTED.toString(), refund.status)
    }

    @Test
    fun testAutoApproval() = runTest {
        val refund = RefundRequest(
            requestedAt = System.currentTimeMillis(),
            initiatedBy = "buyer"
        )

        assertTrue(refund.isEligibleForAutoApproval())
    }

    @Test
    fun testRefundAmountValidation() = runTest {
        val result = refundProcessor.initiateRefund(
            originalAmount = 5000.0,
            refundAmount = 6000.0,  // Invalid: more than original
            // ... other params
        )

        assertTrue(result.isFailure)
    }
}
```

## Deployment Checklist

- [ ] Add RefundModels.kt
- [ ] Add RefundRepository.kt
- [ ] Add RefundProcessor.kt
- [ ] Add RefundViewModel.kt
- [ ] Update firestore.rules
- [ ] Deploy firestore.indexes.json
- [ ] Update Order model
- [ ] Create UI screens
- [ ] Integrate with order cancellation
- [ ] Add notification service
- [ ] Set up dependency injection
- [ ] Write and run tests
- [ ] Update documentation
- [ ] Deploy to production

## Monitoring & Metrics

Track these metrics:
- Total refunds processed
- Refund success rate
- Average refund processing time
- Refund reasons distribution
- Failed refund retry success rate
- Co-seller refund split accuracy

## Support & Troubleshooting

For issues:
1. Check Firestore rules
2. Verify indexes are deployed
3. Check payment gateway integration
4. Review audit trail for errors
5. Check logs for exceptions
