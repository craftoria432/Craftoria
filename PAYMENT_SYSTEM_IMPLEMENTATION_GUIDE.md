# Payment System Implementation Guide

**Status**: Ready for Integration  
**Priority**: HIGH - Production Deployment  
**Timeline**: 1-2 weeks

---

## Quick Start

### Step 1: Add New Utility Classes (Already Created)

✅ **Files Created**:
- `PaymentValidator.kt` - Comprehensive payment validation
- `PaymentAuditLogger.kt` - Complete audit trail logging
- `RefundProcessor.kt` - Refund workflow management

### Step 2: Update Payment Models

Add these fields to `SellerPayment` model:

```kotlin
@get:PropertyName("idempotency_key")
@set:PropertyName("idempotency_key")
var idempotencyKey: String = "",

@get:PropertyName("request_id")
@set:PropertyName("request_id")
var requestId: String = ""
```

### Step 3: Update PaymentRepository

Integrate validation and audit logging:

```kotlin
suspend fun processOrderPayments(order: Order): Result<List<String>> {
    return try {
        // ✅ NEW: Validate payment before processing
        val validation = PaymentValidator.validateOrderPayment(order, order.items)
        if (!validation.isValid) {
            Log.e(TAG, "❌ Payment validation failed: ${validation.getErrorMessage()}")
            return Result.failure(Exception(validation.getErrorMessage()))
        }
        
        // ... existing payment processing logic ...
        
        // ✅ NEW: Log payment creation
        paymentIds.forEach { paymentId ->
            auditLogger.logPaymentCreated(
                paymentId = paymentId,
                orderId = order.id,
                sellerId = sellerId,
                amount = sellerAmount
            )
        }
        
        Result.success(paymentIds)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to process order payments", e)
        Result.failure(e)
    }
}
```

### Step 4: Update CheckoutViewModel

Add validation before order creation:

```kotlin
fun createOrder(cartItems: List<CartItem>) {
    viewModelScope.launch {
        try {
            // ✅ NEW: Validate checkout data
            val validation = PaymentValidator.validateOrderPayment(order, items)
            if (!validation.isValid) {
                _orderState.value = OrderState.Error(validation.getErrorMessage())
                return@launch
            }
            
            // ... existing order creation logic ...
        } catch (e: Exception) {
            _orderState.value = OrderState.Error(e.message ?: "Unknown error")
        }
    }
}
```

### Step 5: Add Refund UI (Optional)

Create a refund screen for sellers:

```kotlin
@Composable
fun RefundRequestScreen(
    paymentId: String,
    onBackClick: () -> Unit,
    viewModel: RefundViewModel = viewModel()
) {
    var refundAmount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Refund amount input
        CraftoriaTextField(
            value = refundAmount,
            onValueChange = { refundAmount = it },
            label = "Refund Amount (PKR)"
        )
        
        // Reason input
        CraftoriaTextField(
            value = reason,
            onValueChange = { reason = it },
            label = "Refund Reason",
            minLines = 3
        )
        
        // Submit button
        CraftoriaButton(
            text = "Request Refund",
            onClick = {
                viewModel.initiateRefund(
                    paymentId = paymentId,
                    refundAmount = refundAmount.toDoubleOrNull() ?: 0.0,
                    reason = reason
                )
            }
        )
    }
}
```

---

## Integration Checklist

### Phase 1: Core Integration (Day 1-2)

- [ ] Copy `PaymentValidator.kt` to project
- [ ] Copy `PaymentAuditLogger.kt` to project
- [ ] Copy `RefundProcessor.kt` to project
- [ ] Update `SellerPayment` model with new fields
- [ ] Update `PaymentRepository` with validation
- [ ] Run unit tests for validators

### Phase 2: Payment Processing (Day 3-4)

- [ ] Update `CheckoutViewModel` with validation
- [ ] Update `CartViewModel` order creation
- [ ] Add audit logging to payment creation
- [ ] Test payment flow end-to-end
- [ ] Verify audit logs are created

### Phase 3: Refund Workflow (Day 5-6)

- [ ] Create `RefundViewModel`
- [ ] Create refund UI screens
- [ ] Integrate `RefundProcessor` into seller screens
- [ ] Test refund flow
- [ ] Verify refund audit logs

### Phase 4: Testing & Deployment (Day 7-10)

- [ ] Unit tests for all validators
- [ ] Integration tests for payment flows
- [ ] Load testing (100+ concurrent payments)
- [ ] Staging deployment
- [ ] Production deployment with monitoring

---

## Usage Examples

### Example 1: Validate Payment Before Processing

```kotlin
val validation = PaymentValidator.validateOrderPayment(order, items)
if (!validation.isValid) {
    showError(validation.getErrorMessage())
    return
}
// Process payment
```

### Example 2: Log Payment Action

```kotlin
val auditLogger = PaymentAuditLogger()
auditLogger.logPaymentCreated(
    paymentId = "pay_123",
    orderId = "ord_456",
    sellerId = "seller_789",
    amount = 5000.0
)
```

### Example 3: Initiate Refund

```kotlin
val refundProcessor = RefundProcessor()
val result = refundProcessor.initiateRefund(
    paymentId = "pay_123",
    refundAmount = 5000.0,
    reason = "Customer requested cancellation",
    requestedBy = "admin_001"
)

if (result.isSuccess) {
    val refundId = result.getOrNull()
    println("Refund initiated: $refundId")
}
```

### Example 4: Get Audit Trail

```kotlin
val auditLogger = PaymentAuditLogger()
val result = auditLogger.getPaymentAuditTrail("pay_123")

if (result.isSuccess) {
    val logs = result.getOrNull() ?: emptyList()
    logs.forEach { log ->
        println("${log.action} by ${log.actorId} at ${log.timestamp}")
    }
}
```

### Example 5: Get Refund Statistics

```kotlin
val refundProcessor = RefundProcessor()
val result = refundProcessor.getRefundStats("seller_123")

if (result.isSuccess) {
    val stats = result.getOrNull()
    println("Total Refunds: ${stats?.totalRefunds}")
    println("Completed: ${stats?.completedRefunds}")
    println("Total Refunded: PKR ${stats?.totalRefundedAmount}")
}
```

---

## Testing Guide

### Unit Tests

```kotlin
class PaymentValidatorTest {
    @Test
    fun testValidateOrderPayment_Success() {
        val order = Order(id = "ord_123", buyerId = "buyer_123")
        val items = listOf(
            OrderItem(productId = "prod_1", sellerId = "seller_1", quantity = 1, price = 1000.0)
        )
        
        val result = PaymentValidator.validateOrderPayment(order, items)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun testValidateOrderPayment_InvalidAmount() {
        val order = Order(id = "ord_123", buyerId = "buyer_123")
        val items = listOf(
            OrderItem(productId = "prod_1", sellerId = "seller_1", quantity = 1, price = -100.0)
        )
        
        val result = PaymentValidator.validateOrderPayment(order, items)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
    }
}
```

### Integration Tests

```kotlin
class PaymentFlowIntegrationTest {
    @Test
    fun testCompletePaymentFlow() = runBlocking {
        // Create order
        val order = createTestOrder()
        
        // Validate payment
        val validation = PaymentValidator.validateOrderPayment(order, order.items)
        assertTrue(validation.isValid)
        
        // Process payment
        val paymentResult = paymentRepository.processOrderPayments(order)
        assertTrue(paymentResult.isSuccess)
        
        // Verify audit logs
        val auditResult = auditLogger.getOrderAuditTrail(order.id)
        assertTrue(auditResult.isSuccess)
        val logs = auditResult.getOrNull() ?: emptyList()
        assertTrue(logs.isNotEmpty())
    }
}
```

---

## Monitoring & Alerts

### Key Metrics

```kotlin
// Track in Firebase Analytics or custom dashboard
analytics.logEvent("payment_created", bundleOf(
    "order_id" to orderId,
    "amount" to amount,
    "seller_count" to sellerCount
))

analytics.logEvent("payment_validated", bundleOf(
    "is_valid" to isValid,
    "error_count" to errorCount
))

analytics.logEvent("refund_initiated", bundleOf(
    "payment_id" to paymentId,
    "refund_amount" to refundAmount
))
```

### Alert Conditions

- Payment validation failure rate > 5%
- Refund processing time > 24 hours
- Audit log creation failures
- Reconciliation discrepancies detected

---

## Troubleshooting

### Issue: Validation always fails

**Solution**: Check that all required fields are populated:
```kotlin
// Ensure order has all required fields
order.id.isNotEmpty()
order.buyerId.isNotEmpty()
order.items.isNotEmpty()
```

### Issue: Audit logs not created

**Solution**: Verify Firestore collection exists and rules allow writes:
```javascript
match /payment_audit_logs/{logId} {
  allow create: if request.auth.token.admin == true;
  allow read: if request.auth.token.admin == true;
}
```

### Issue: Refund processing fails

**Solution**: Check payment status is COMPLETED:
```kotlin
if (payment.status != PaymentStatus.COMPLETED.toString()) {
    // Cannot refund non-completed payments
}
```

---

## Performance Optimization

### Batch Operations

```kotlin
// Process multiple refunds efficiently
val batch = db.batch()
refunds.forEach { refund ->
    batch.update(refundsCollection.document(refund.id), 
        "status" to RefundStatus.COMPLETED.toString()
    )
}
batch.commit().await()
```

### Caching

```kotlin
// Cache validation rules to avoid repeated lookups
private val validPaymentMethods = listOf(
    "Cash on Delivery",
    "Debit/Credit Card",
    "Bank Transfer"
)
```

### Indexing

Create Firestore indexes for common queries:
```
Collection: seller_payments
Fields: seller_id (Ascending), created_at (Descending)

Collection: refunds
Fields: seller_id (Ascending), status (Ascending)

Collection: payment_audit_logs
Fields: payment_id (Ascending), timestamp (Descending)
```

---

## Security Checklist

- [ ] All payment operations require authentication
- [ ] Access control verified for seller/buyer payments
- [ ] Audit logs immutable (no delete operations)
- [ ] Sensitive data encrypted in transit
- [ ] Rate limiting on payment endpoints
- [ ] Firestore rules enforce access control
- [ ] Regular security audits scheduled

---

## Deployment Checklist

- [ ] Backup current Firestore data
- [ ] Deploy code changes to staging
- [ ] Run full integration test suite
- [ ] Verify audit logs are created
- [ ] Monitor error rates for 24 hours
- [ ] Deploy to production
- [ ] Enable monitoring and alerts
- [ ] Document any issues encountered

---

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review audit logs for error details
3. Check Firestore rules and indexes
4. Contact development team with payment ID for investigation

---

## Next Steps

1. **Immediate** (This week):
   - Integrate validation and audit logging
   - Deploy to staging
   - Run integration tests

2. **Short-term** (Next 2 weeks):
   - Implement refund workflow
   - Add refund UI
   - Deploy to production

3. **Medium-term** (Next month):
   - Payment gateway integration (Stripe/PayPal)
   - Automated payouts
   - Advanced analytics dashboard

4. **Long-term** (Next quarter):
   - Multi-currency support
   - Subscription payments
   - Payment disputes workflow
