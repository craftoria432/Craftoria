# Refund Processing System - Quick Reference

## Core Components

| Component | Purpose |
|-----------|---------|
| `RefundModels.kt` | RefundRequest, RefundSplit, RefundAuditEntry, enums |
| `RefundRepository.kt` | Firestore CRUD operations |
| `RefundProcessor.kt` | Business logic, payment gateway integration |
| `RefundViewModel.kt` | UI state management |

## Key Classes

### RefundRequest
```kotlin
data class RefundRequest(
    val id: String,
    val orderId: String,
    val paymentId: String,
    val buyerId: String,
    val sellerId: String,
    val refundType: String,        // FULL, PARTIAL, RETURN
    val originalAmount: Double,
    val refundAmount: Double,
    val reason: String,            // DEFECTIVE_PRODUCT, etc.
    val status: String,            // requested, approved, processing, completed, rejected, failed
    val refundSplits: List<RefundSplit>,
    val auditTrail: List<RefundAuditEntry>
)
```

### RefundStatus Enum
```kotlin
enum class RefundStatus {
    REQUESTED,    // Initial state
    APPROVED,     // Approved by admin/seller
    PROCESSING,   // Payment gateway processing
    COMPLETED,    // Successfully refunded
    REJECTED,     // Denied
    FAILED,       // Failed (can retry)
    CANCELLED     // Cancelled
}
```

### RefundType Enum
```kotlin
enum class RefundType {
    FULL,      // Full refund
    PARTIAL,   // Partial refund
    RETURN     // Refund after return
}
```

## Common Operations

### 1. Initiate Refund
```kotlin
viewModel.initiateRefund(
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
    reasonDetails = "Product arrived damaged",
    paymentMethod = "stripe",
    transactionId = "ch_1234567890",
    initiatedBy = "buyer"
)
```

### 2. Approve Refund
```kotlin
viewModel.approveRefund(
    refundId = "refund_123",
    approvedBy = "admin_456",
    approverName = "Admin User",
    approvalNotes = "Verified damage, approved"
)
```

### 3. Reject Refund
```kotlin
viewModel.rejectRefund(
    refundId = "refund_123",
    rejectedBy = "seller_101",
    rejectorName = "Store ABC",
    rejectionReason = "Item was not damaged"
)
```

### 4. Process Refund
```kotlin
viewModel.processRefund(
    refundId = "refund_123",
    paymentGateway = "stripe"  // or "paypal", "cod"
)
```

### 5. Get Refund Status
```kotlin
viewModel.getRefund(refundId = "refund_123")
// Observe currentRefund StateFlow
```

### 6. Get Buyer's Refunds
```kotlin
viewModel.getRefundsByBuyer(buyerId = "buyer_789")
// Observe refundList StateFlow
```

### 7. Get Seller's Refunds
```kotlin
viewModel.getRefundsBySeller(sellerId = "seller_101")
// Observe refundList StateFlow
```

### 8. Get Pending Refunds (Admin)
```kotlin
viewModel.getPendingRefunds()
// Observe refundList StateFlow
```

## UI State Handling

```kotlin
// Observe refund state
viewModel.refundState.collect { state ->
    when (state) {
        is RefundUIState.Idle -> { /* Initial state */ }
        is RefundUIState.Loading -> { /* Show loading */ }
        is RefundUIState.RefundInitiated -> { /* Show success */ }
        is RefundUIState.RefundApproved -> { /* Show approved */ }
        is RefundUIState.RefundProcessed -> { /* Show processed */ }
        is RefundUIState.RefundsLoaded -> { /* Show list */ }
        is RefundUIState.Error -> { /* Show error */ }
    }
}

// Observe error messages
viewModel.errorMessage.collect { error ->
    if (error != null) {
        showErrorDialog(error)
    }
}
```

## Refund Reasons

```kotlin
enum class RefundReason {
    BUYER_REQUEST,
    SELLER_APPROVAL,
    DEFECTIVE_PRODUCT,
    WRONG_ITEM,
    NOT_AS_DESCRIBED,
    DAMAGED_IN_TRANSIT,
    LOST_IN_TRANSIT,
    BUYER_CHANGED_MIND,
    DUPLICATE_ORDER,
    PAYMENT_ERROR,
    CHARGEBACK,
    OTHER
}
```

## Status Flow Diagram

```
┌─────────────┐
│  REQUESTED  │ ← Buyer/Seller initiates
└──────┬──────┘
       │
       ├─→ ┌──────────┐
       │   │ REJECTED │ ← Admin/Seller denies
       │   └──────────┘
       │
       └─→ ┌──────────┐
           │ APPROVED │ ← Admin/Seller approves
           └────┬─────┘
                │
                └─→ ┌────────────┐
                    │ PROCESSING │ ← Payment gateway
                    └────┬───────┘
                         │
                         ├─→ ┌───────────┐
                         │   │ COMPLETED │ ✓
                         │   └───────────┘
                         │
                         └─→ ┌────────┐
                             │ FAILED │ → RETRY (max 3x)
                             └────────┘
```

## Firestore Queries

### Get refund by ID
```kotlin
refundRepository.getRefundById(refundId)
```

### Get refunds by order
```kotlin
refundRepository.getRefundsByOrderId(orderId)
```

### Get refunds by buyer
```kotlin
refundRepository.getRefundsByBuyerId(buyerId)
```

### Get refunds by seller
```kotlin
refundRepository.getRefundsBySellerId(sellerId)
```

### Get pending refunds
```kotlin
refundRepository.getPendingRefunds()
```

### Get failed refunds for retry
```kotlin
refundRepository.getFailedRefundsForRetry()
```

## Payment Gateway Support

| Gateway | Status | Notes |
|---------|--------|-------|
| Stripe | Ready | Implement API calls |
| PayPal | Ready | Implement API calls |
| COD | Ready | Manual processing |

## Co-Seller Refund Splits

```kotlin
// Calculate proportional refunds
val refundSplits = refundProcessor.calculateRefundSplits(
    refund = refund,
    paymentSplits = paymentSplits
)

// Process splits
refundProcessor.processRefundSplits(
    refundId = refund.id,
    splits = refundSplits
)
```

## Retry Logic

```kotlin
// Automatic retry for failed refunds
refundProcessor.retryFailedRefunds()
// Returns count of successful retries

// Check if refund can be retried
if (refund.canRetry()) {
    refundRepository.retryRefund(refundId)
}
```

## Audit Trail

Every refund action is logged:
```kotlin
auditTrail: [
    RefundAuditEntry(
        action = "requested",
        actor = "buyer_123",
        actorName = "John Doe",
        notes = "Refund request initiated",
        timestamp = 1234567890000
    ),
    RefundAuditEntry(
        action = "approved",
        actor = "admin_456",
        actorName = "Admin User",
        notes = "Verified damage, approved",
        timestamp = 1234567891000
    ),
    // ... more entries
]
```

## Error Handling

```kotlin
// Check for errors
viewModel.errorMessage.collect { error ->
    if (error != null) {
        // Handle error
        Log.e("RefundError", error)
        showErrorDialog(error)
        viewModel.clearError()
    }
}
```

## Best Practices

1. **Always use idempotency keys** to prevent duplicate refunds
2. **Validate refund amounts** before processing
3. **Log all actions** for audit trail
4. **Implement retry logic** for failed refunds
5. **Send notifications** on status changes
6. **Use transactions** for multi-step operations
7. **Test with all payment gateways**
8. **Monitor refund metrics** for trends

## Integration Checklist

- [ ] Add RefundModels.kt to project
- [ ] Add RefundRepository.kt to project
- [ ] Add RefundProcessor.kt to project
- [ ] Add RefundViewModel.kt to project
- [ ] Update Firestore rules for refunds collection
- [ ] Implement payment gateway API calls
- [ ] Create UI screens for refund management
- [ ] Add notification service integration
- [ ] Test all refund scenarios
- [ ] Deploy to production

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Refund not processing | Check payment gateway integration |
| Duplicate refunds | Verify idempotency key implementation |
| Audit trail missing | Ensure addAuditEntry is called |
| Co-seller splits incorrect | Verify calculateProportionalRefund logic |
| Retry not working | Check retry count and status |

## Performance Tips

1. Use indexes on frequently queried fields
2. Batch process failed refunds
3. Cache refund status in UI
4. Use pagination for large refund lists
5. Implement background job for retry processing

## Security Notes

- Firestore rules enforce access control
- Never expose payment details
- Validate all inputs
- Use HTTPS for API calls
- Implement rate limiting
- Log all sensitive operations
