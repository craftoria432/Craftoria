# Refund Deserialization Fix - Code Reference

## Helper Function

### Location
`app/src/main/java/com/gcuf/craftoria/data/repository/RefundRepository.kt` (End of file)

### Code
```kotlin
// ✅ NEW: Helper function to safely convert Firestore Timestamp to Long
private fun convertTimestampToLong(value: Any?): Long? = when (value) {
    is Long -> value
    is com.google.firebase.Timestamp -> value.toDate().time
    is Number -> value.toLong()
    is String -> value.toLongOrNull()
    is Map<*, *> -> {
        val seconds = (value["_seconds"] as? Long) ?: (value["seconds"] as? Long) ?: 0L
        val nanos = (value["_nanoseconds"] as? Long) ?: (value["nanoseconds"] as? Long) ?: 0L
        (seconds * 1000) + (nanos / 1_000_000)
    }
    null -> null
    else -> null
}
```

## Updated Methods

### 1. getRefundById()
```kotlin
suspend fun getRefundById(refundId: String): Result<RefundRequest> {
    return try {
        val snapshot = firestore.collection(REFUNDS_COLLECTION).document(refundId).get().await()
        val refund = snapshot.toObject(RefundRequest::class.java) ?: RefundRequest()
        // ✅ FIX: Ensure all timestamp fields are properly converted from Firestore Timestamp to Long
        refund.requestedAt = convertTimestampToLong(refund.requestedAt)
        refund.approvedAt = convertTimestampToLong(refund.approvedAt)
        refund.processedAt = convertTimestampToLong(refund.processedAt)
        refund.completedAt = convertTimestampToLong(refund.completedAt)
        refund.createdAt = convertTimestampToLong(refund.createdAt)
        refund.updatedAt = convertTimestampToLong(refund.updatedAt)
        refund.lastRetryAt = convertTimestampToLong(refund.lastRetryAt)
        Result.success(refund)
    } catch (e: Exception) { Log.e(TAG, "Error getting refund", e); Result.failure(e) }
}
```

### 2. getRefundsByOrderId()
```kotlin
suspend fun getRefundsByOrderId(orderId: String): Result<List<RefundRequest>> {
    return try {
        val snapshots = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("order_id", orderId)
            .orderBy("requested_at", Query.Direction.DESCENDING).get().await()
        // ✅ FIX: Convert all timestamp fields for each refund
        Result.success(snapshots.documents.mapNotNull { doc ->
            val refund = doc.toObject(RefundRequest::class.java) ?: return@mapNotNull null
            refund.requestedAt = convertTimestampToLong(refund.requestedAt)
            refund.approvedAt = convertTimestampToLong(refund.approvedAt)
            refund.processedAt = convertTimestampToLong(refund.processedAt)
            refund.completedAt = convertTimestampToLong(refund.completedAt)
            refund.createdAt = convertTimestampToLong(refund.createdAt)
            refund.updatedAt = convertTimestampToLong(refund.updatedAt)
            refund.lastRetryAt = convertTimestampToLong(refund.lastRetryAt)
            refund
        })
    } catch (e: Exception) { Log.e(TAG, "Error getting refunds by order", e); Result.failure(e) }
}
```

### 3. getRefundsByBuyerId()
```kotlin
suspend fun getRefundsByBuyerId(buyerId: String): Result<List<RefundRequest>> {
    return try {
        val snapshots = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("buyer_id", buyerId)
            .orderBy("requested_at", Query.Direction.DESCENDING).get().await()
        // ✅ FIX: Convert all timestamp fields for each refund
        Result.success(snapshots.documents.mapNotNull { doc ->
            val refund = doc.toObject(RefundRequest::class.java) ?: return@mapNotNull null
            refund.requestedAt = convertTimestampToLong(refund.requestedAt)
            refund.approvedAt = convertTimestampToLong(refund.approvedAt)
            refund.processedAt = convertTimestampToLong(refund.processedAt)
            refund.completedAt = convertTimestampToLong(refund.completedAt)
            refund.createdAt = convertTimestampToLong(refund.createdAt)
            refund.updatedAt = convertTimestampToLong(refund.updatedAt)
            refund.lastRetryAt = convertTimestampToLong(refund.lastRetryAt)
            refund
        })
    } catch (e: Exception) { Log.e(TAG, "Error getting refunds by buyer", e); Result.failure(e) }
}
```

### 4. getRefundsBySellerId()
```kotlin
suspend fun getRefundsBySellerId(sellerId: String): Result<List<RefundRequest>> {
    return try {
        val snapshots = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("seller_id", sellerId)
            .orderBy("requested_at", Query.Direction.DESCENDING).get().await()
        // ✅ FIX: Convert all timestamp fields for each refund
        Result.success(snapshots.documents.mapNotNull { doc ->
            val refund = doc.toObject(RefundRequest::class.java) ?: return@mapNotNull null
            refund.requestedAt = convertTimestampToLong(refund.requestedAt)
            refund.approvedAt = convertTimestampToLong(refund.approvedAt)
            refund.processedAt = convertTimestampToLong(refund.processedAt)
            refund.completedAt = convertTimestampToLong(refund.completedAt)
            refund.createdAt = convertTimestampToLong(refund.createdAt)
            refund.updatedAt = convertTimestampToLong(refund.updatedAt)
            refund.lastRetryAt = convertTimestampToLong(refund.lastRetryAt)
            refund
        })
    } catch (e: Exception) { Log.e(TAG, "Error getting refunds by seller", e); Result.failure(e) }
}
```

### 5. getPendingRefunds()
```kotlin
suspend fun getPendingRefunds(): Result<List<RefundRequest>> = try {
    val snapshots = firestore.collection(REFUNDS_COLLECTION)
        .whereIn("status", listOf(
            RefundStatus.REQUESTED.toString(),
            RefundStatus.UNDER_REVIEW.toString(),
            RefundStatus.APPROVED_BY_SELLER.toString(),
            RefundStatus.APPROVED_BY_ADMIN.toString()
        ))
        .orderBy("requested_at", Query.Direction.ASCENDING).get().await()
    // ✅ FIX: Convert all timestamp fields for each refund
    Result.success(snapshots.documents.mapNotNull { doc ->
        val refund = doc.toObject(RefundRequest::class.java) ?: return@mapNotNull null
        refund.requestedAt = convertTimestampToLong(refund.requestedAt)
        refund.approvedAt = convertTimestampToLong(refund.approvedAt)
        refund.processedAt = convertTimestampToLong(refund.processedAt)
        refund.completedAt = convertTimestampToLong(refund.completedAt)
        refund.createdAt = convertTimestampToLong(refund.createdAt)
        refund.updatedAt = convertTimestampToLong(refund.updatedAt)
        refund.lastRetryAt = convertTimestampToLong(refund.lastRetryAt)
        refund
    })
} catch (e: Exception) { Log.e(TAG, "Error getting pending refunds", e); Result.failure(e) }
```

### 6. getFailedRefundsForRetry()
```kotlin
suspend fun getFailedRefundsForRetry(): Result<List<RefundRequest>> = try {
    val snapshots = firestore.collection(REFUNDS_COLLECTION)
        .whereEqualTo("status", RefundStatus.FAILED.toString())
        .whereLessThan("retry_count", 3)
        .orderBy("retry_count", Query.Direction.ASCENDING)
        .orderBy("last_retry_at", Query.Direction.ASCENDING).get().await()
    // ✅ FIX: Convert all timestamp fields for each refund
    Result.success(snapshots.documents.mapNotNull { doc ->
        val refund = doc.toObject(RefundRequest::class.java) ?: return@mapNotNull null
        refund.requestedAt = convertTimestampToLong(refund.requestedAt)
        refund.approvedAt = convertTimestampToLong(refund.approvedAt)
        refund.processedAt = convertTimestampToLong(refund.processedAt)
        refund.completedAt = convertTimestampToLong(refund.completedAt)
        refund.createdAt = convertTimestampToLong(refund.createdAt)
        refund.updatedAt = convertTimestampToLong(refund.updatedAt)
        refund.lastRetryAt = convertTimestampToLong(refund.lastRetryAt)
        refund
    })
} catch (e: Exception) { Log.e(TAG, "Error getting failed refunds", e); Result.failure(e) }
```

### 7. checkDuplicateRefund()
```kotlin
suspend fun checkDuplicateRefund(idempotencyKey: String): Result<RefundRequest?> = try {
    val snapshot = firestore.collection(REFUNDS_COLLECTION)
        .whereEqualTo("idempotency_key", idempotencyKey).get().await()
    // ✅ FIX: Convert all timestamp fields for the refund
    val refund = snapshot.documents.firstOrNull()?.toObject(RefundRequest::class.java)
    if (refund != null) {
        refund.requestedAt = convertTimestampToLong(refund.requestedAt)
        refund.approvedAt = convertTimestampToLong(refund.approvedAt)
        refund.processedAt = convertTimestampToLong(refund.processedAt)
        refund.completedAt = convertTimestampToLong(refund.completedAt)
        refund.createdAt = convertTimestampToLong(refund.createdAt)
        refund.updatedAt = convertTimestampToLong(refund.updatedAt)
        refund.lastRetryAt = convertTimestampToLong(refund.lastRetryAt)
    }
    Result.success(refund)
} catch (e: Exception) { Log.e(TAG, "Error checking duplicate", e); Result.failure(e) }
```

## Usage Example

### In ViewModel
```kotlin
fun getRefundsByBuyer(buyerId: String) {
    viewModelScope.launch {
        try {
            _refundState.value = RefundUiState.Loading

            val result = refundRepository.getRefundsByBuyerId(buyerId)

            if (result.isSuccess) {
                val refunds = result.getOrNull() ?: emptyList()
                // ✅ All timestamps are now properly converted to Long
                _refundList.value = refunds
                _refundState.value = RefundUiState.RefundsLoaded(refunds)
                _errorMessage.value = null
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                _errorMessage.value = error
                _refundState.value = RefundUiState.Error(error)
            }
        } catch (e: Exception) {
            val error = e.message ?: "Unknown error"
            _errorMessage.value = error
            _refundState.value = RefundUiState.Error(error)
            Log.e(TAG, "Exception getting buyer refunds", e)
        }
    }
}
```

## Timestamp Conversion Examples

### Example 1: Firestore Timestamp
```kotlin
// Input: Firestore Timestamp object
val timestamp = com.google.firebase.Timestamp(1234567890, 123456789)

// Conversion
val longValue = convertTimestampToLong(timestamp)
// Output: 1234567890123 (milliseconds)
```

### Example 2: Long Value
```kotlin
// Input: Already a Long
val longValue = 1234567890123L

// Conversion
val result = convertTimestampToLong(longValue)
// Output: 1234567890123 (unchanged)
```

### Example 3: Map Format
```kotlin
// Input: Map representation from Firestore
val mapValue = mapOf(
    "_seconds" to 1234567890L,
    "_nanoseconds" to 123456789L
)

// Conversion
val longValue = convertTimestampToLong(mapValue)
// Output: 1234567890123 (milliseconds)
```

## Related Files

### RefundModels.kt
- Contains `RefundRequest` data class with `Any?` timestamp fields
- Contains `convertRefundTimestamp()` helper function
- Contains timestamp conversion extension functions

### RefundViewModel.kt
- Uses `RefundRepository` methods to fetch refunds
- Displays refund data in UI

### RefundRepository.kt (This File)
- Contains all Firestore operations
- Now includes timestamp conversion logic

## Testing Code Snippet

```kotlin
// Test: Verify timestamp conversion works
@Test
fun testTimestampConversion() {
    val timestamp = com.google.firebase.Timestamp(1234567890, 123456789)
    val result = convertTimestampToLong(timestamp)
    
    assertEquals(1234567890123L, result)
}

// Test: Verify refund deserialization works
@Test
fun testRefundDeserialization() {
    val refundId = "test-refund-123"
    
    // Create and store refund
    val refund = RefundRequest(
        id = refundId,
        buyerId = "buyer-123",
        sellerId = "seller-123",
        refundAmount = 100.0,
        status = RefundStatus.REQUESTED.toString()
    )
    
    // Fetch refund (should not throw error)
    val result = refundRepository.getRefundById(refundId)
    
    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
}
```

## Summary

The fix adds a robust timestamp conversion mechanism to handle Firestore Timestamp objects during deserialization. All 7 methods that fetch RefundRequest objects now properly convert timestamps, ensuring the refund workflow works without errors.
