# Payment Access Control - Code Reference

## Quick Code Snippets for Implementation

---

## 1. Data Model (PaymentModels.kt)

### New Field
```kotlin
@get:PropertyName("involved_seller_ids")
@set:PropertyName("involved_seller_ids")
var involvedSellerIds: List<String> = emptyList(),
```

### Updated toMap()
```kotlin
fun SellerPayment.toMap(): Map<String, Any> = mapOf(
    // ... existing fields ...
    "involved_seller_ids" to involvedSellerIds
)
```

---

## 2. Repository Access Control (PaymentRepository.kt)

### Exception Class
```kotlin
class UnauthorizedAccessException(message: String) : SecurityException(message)
```

### Get Seller Payments
```kotlin
suspend fun getSellerPayments(
    sellerId: String,
    requestingUserId: String,
    status: PaymentStatus? = null
): Result<List<SellerPayment>> {
    return try {
        // ✅ SECURITY CHECK
        if (sellerId != requestingUserId) {
            Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to access payments for seller $sellerId")
            return Result.failure(
                PaymentRepository.UnauthorizedAccessException(
                    "Unauthorized: Cannot access other seller's payments"
                )
            )
        }

        var query: Query = paymentsCollection
            .whereEqualTo("seller_id", sellerId)

        if (status != null) {
            query = query.whereEqualTo("status", status.toString())
        }

        val snapshot = query.get().await()
        val payments = snapshot.documents.mapNotNull { doc ->
            doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
        }.sortedByDescending { it.createdAt }

        Result.success(payments)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Get Payment By ID
```kotlin
suspend fun getPaymentById(
    paymentId: String,
    requestingUserId: String
): Result<SellerPayment?> {
    return try {
        val doc = paymentsCollection.document(paymentId).get().await()
        val payment = doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)

        if (payment == null) {
            return Result.success(null)
        }

        // ✅ SECURITY CHECK
        if (payment.sellerId != requestingUserId) {
            Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to access payment $paymentId (owner: ${payment.sellerId})")
            return Result.failure(
                PaymentRepository.UnauthorizedAccessException(
                    "Unauthorized: Cannot access other seller's payment"
                )
            )
        }

        Result.success(payment)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Get Order Payments
```kotlin
suspend fun getOrderPayments(
    orderId: String,
    requestingUserId: String
): Result<List<SellerPayment>> {
    return try {
        val snapshot = paymentsCollection
            .whereEqualTo("order_id", orderId)
            .get()
            .await()

        val payments = snapshot.documents.mapNotNull { doc ->
            doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
        }.sortedByDescending { it.createdAt }

        // ✅ SECURITY CHECK
        val isUserInvolved = payments.any { it.sellerId == requestingUserId }
        if (!isUserInvolved) {
            Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to view payment split for order $orderId (not involved)")
            return Result.failure(
                PaymentRepository.UnauthorizedAccessException(
                    "Unauthorized: Not involved in this order"
                )
            )
        }

        Result.success(payments)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Process Order Payments
```kotlin
// In processOrderPayments(), add:
val involvedSellerIds = itemsBySellerMap.keys.toList()

val payment = SellerPayment(
    // ... other fields ...
    involvedSellerIds = involvedSellerIds  // ✅ NEW
)
```

---

## 3. ViewModel Access Control (SellerPaymentViewModel.kt)

### Current User ID
```kotlin
private val authRepository = AuthRepository()

private val currentUserId: String
    get() = authRepository.getCurrentUserId() ?: ""
```

### Load Seller Payments
```kotlin
fun loadSellerPayments(sellerId: String, status: PaymentStatus? = null) {
    viewModelScope.launch {
        try {
            _paymentState.value = PaymentUiState.Loading

            // ✅ SECURITY CHECK
            if (sellerId != currentUserId) {
                Log.w(TAG, "🚫 UNAUTHORIZED: User $currentUserId attempted to access payments for seller $sellerId")
                _paymentState.value = PaymentUiState.Error(
                    "Unauthorized: Cannot access other seller's payments"
                )
                return@launch
            }

            val result = paymentRepository.getSellerPayments(
                sellerId = sellerId,
                requestingUserId = currentUserId,
                status = status
            )

            result.onSuccess { payments ->
                _paymentState.value = PaymentUiState.Success(payments)
            }.onFailure { error ->
                _paymentState.value = PaymentUiState.Error(error.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            _paymentState.value = PaymentUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

### Load Payment Detail
```kotlin
fun loadPaymentDetail(paymentId: String) {
    viewModelScope.launch {
        try {
            val result = paymentRepository.getPaymentById(
                paymentId = paymentId,
                requestingUserId = currentUserId
            )

            result.onSuccess { payment ->
                if (payment == null) {
                    _selectedPayment.value = null
                    return@onSuccess
                }
                _selectedPayment.value = payment
            }.onFailure { error ->
                _selectedPayment.value = null
            }
        } catch (e: Exception) {
            _selectedPayment.value = null
        }
    }
}
```

### Load Order Payments
```kotlin
fun loadOrderPayments(orderId: String) {
    viewModelScope.launch {
        try {
            _paymentState.value = PaymentUiState.Loading

            val result = paymentRepository.getOrderPayments(
                orderId = orderId,
                requestingUserId = currentUserId
            )

            result.onSuccess { payments ->
                _paymentState.value = PaymentUiState.Success(payments)
            }.onFailure { error ->
                _paymentState.value = PaymentUiState.Error(error.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            _paymentState.value = PaymentUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

---

## 4. UI Error Handling

### SellerPaymentsScreen
```kotlin
is PaymentUiState.Error -> {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (paymentState as PaymentUiState.Error).message,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

### PaymentDetailScreen
```kotlin
} else {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Payment not found or access denied",
                color = Color.Red,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBackClick) {
                Text("Go Back")
            }
        }
    )
}
```

### CoSellerPaymentSplitScreen
```kotlin
@Composable
fun CoSellerPaymentSplitScreen(
    orderId: String,
    payments: List<SellerPayment>,
    onBackClick: () -> Unit,
    currentUserId: String = ""
) {
    // ✅ SECURITY CHECK
    val isUserInvolved = payments.any { it.sellerId == currentUserId }
    
    if (!isUserInvolved && currentUserId.isNotEmpty()) {
        Scaffold(
            topBar = { /* ... */ }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Access Denied",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Access Denied",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You are not involved in this order",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }
    
    // ... rest of implementation
}
```

---

## 5. Data Migration (PaymentDataMigration.kt)

### Migrate Existing Payments
```kotlin
suspend fun migrateExistingPayments(): Result<Int> {
    return try {
        val snapshot = paymentsCollection
            .whereEqualTo("involved_seller_ids", null)
            .get()
            .await()

        val documentsToUpdate = snapshot.documents.filter { doc ->
            val involvedIds = doc.get("involved_seller_ids")
            involvedIds == null || (involvedIds is List<*> && (involvedIds as List<*>).isEmpty())
        }

        var migratedCount = 0

        documentsToUpdate.forEach { doc ->
            try {
                val sellerId = doc.getString("seller_id") ?: ""
                val orderId = doc.getString("order_id") ?: ""

                val orderPaymentsSnapshot = paymentsCollection
                    .whereEqualTo("order_id", orderId)
                    .get()
                    .await()

                val involvedSellerIds = orderPaymentsSnapshot.documents
                    .mapNotNull { it.getString("seller_id") }
                    .distinct()

                paymentsCollection.document(doc.id).update(
                    mapOf(
                        "involved_seller_ids" to involvedSellerIds,
                        "migrated_at" to System.currentTimeMillis()
                    )
                ).await()

                migratedCount++
            } catch (e: Exception) {
                Log.e(TAG, "Error migrating payment ${doc.id}", e)
            }
        }

        Result.success(migratedCount)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 6. Firestore Security Rules

```javascript
match /seller_payments/{document=**} {
  // Read: Only the seller who owns this payment can view it
  allow read: if request.auth.uid == resource.data.seller_id;
  
  // Read: Or if they're involved in the order
  allow read: if request.auth.uid in resource.data.involved_seller_ids;
  
  // Write: Only admin or system can create/update payments
  allow create, update, delete: if false;
}
```

---

## 7. Usage Examples

### Load Seller Payments
```kotlin
// In your screen or activity
viewModel.loadSellerPayments(
    sellerId = currentUserId,  // Must match current user
    status = null
)
```

### Load Payment Detail
```kotlin
// Access control verified automatically
viewModel.loadPaymentDetail(paymentId)
```

### Load Order Payment Split
```kotlin
// Only works if current user is involved in the order
viewModel.loadOrderPayments(orderId)
```

### Run Migration
```kotlin
// In MainActivity or App initialization
viewModelScope.launch {
    val result = PaymentDataMigration.migrateExistingPayments()
    result.onSuccess { count ->
        Log.d(TAG, "✅ Migrated $count payments")
    }
}
```

---

## 8. Error Handling

### Catch Authorization Errors
```kotlin
result.onFailure { error ->
    when (error) {
        is PaymentRepository.UnauthorizedAccessException -> {
            // Handle access denied
            showError("You don't have permission to view this payment")
            logSecurityEvent(error.message)
        }
        else -> {
            // Handle other errors
            showError("Failed to load payment")
        }
    }
}
```

---

## 9. Logging

### Log Unauthorized Access
```kotlin
Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to access payments for seller $sellerId")
```

### Log Migration Progress
```kotlin
Log.d(TAG, "🔄 Starting payment data migration...")
Log.d(TAG, "📊 Found ${documentsToUpdate.size} payments to migrate")
Log.d(TAG, "✅ Migration complete: $migratedCount payments updated")
```

---

## 10. Testing

### Test Seller Can View Own Payments
```kotlin
// User: Seller A
viewModel.loadSellerPayments(sellerId = "seller_a", requestingUserId = "seller_a")
// Expected: ✅ Success
```

### Test Seller Cannot View Other Seller's Payments
```kotlin
// User: Seller A
viewModel.loadSellerPayments(sellerId = "seller_b", requestingUserId = "seller_a")
// Expected: ❌ UnauthorizedAccessException
```

### Test Seller Can View Payment Split for Their Order
```kotlin
// User: Seller A
// Order: #12345 (Sellers A, B, C)
viewModel.loadOrderPayments(orderId = "order_12345", requestingUserId = "seller_a")
// Expected: ✅ Success
```

### Test Seller Cannot View Payment Split for Other Orders
```kotlin
// User: Seller A
// Order: #67890 (Sellers B, C, D)
viewModel.loadOrderPayments(orderId = "order_67890", requestingUserId = "seller_a")
// Expected: ❌ UnauthorizedAccessException
```

---

**Last Updated**: March 17, 2026
**Status**: ✅ Production Ready
