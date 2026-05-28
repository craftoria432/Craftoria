# Production-Ready Payment Screens Architecture & Co-Seller Data Access Policy

## Executive Summary

This document provides comprehensive architectural guidance for payment screens in a multi-seller e-commerce platform with co-seller support. It covers screen hierarchy, responsibilities, data access control, and professional recommendations for handling sensitive financial information in co-seller scenarios.

---

## Part 1: Payment Screens Architecture

### 1.1 Screen Hierarchy & Navigation Flow

```
SellerDashboardScreen
    ↓
SellerPaymentsScreen (Main Hub)
    ├─→ PaymentDetailScreen (Single Payment Details)
    │   └─→ CoSellerPaymentSplitScreen (Multi-Seller Breakdown)
    │
    └─→ Filter & Statistics View
```

### 1.2 Screen Responsibilities & Functionalities

#### **SellerPaymentsScreen** (Main Payment Hub)
**Purpose**: Centralized payment history and overview for sellers

**Key Responsibilities**:
- Display payment history with comprehensive filtering
- Show payment statistics and earnings summary
- Provide quick access to individual payment details
- Enable status-based filtering (Pending, Processing, Completed, Failed, Refunded)

**Displayed Information**:
- **Statistics Cards**:
  - Total Earnings (all-time)
  - Completed Amount (successfully processed)
  - Pending Amount (awaiting processing)
  - Total Payments Count
  - Total Orders Count

- **Payment List**:
  - Order ID (truncated for privacy)
  - Buyer Name
  - Payment Status (with color-coded badge)
  - Item Count
  - Amount
  - Payment Date

**User Actions**:
- Filter payments by status
- Click to view payment details
- Clear filters

**Data Access Pattern**:
```kotlin
// Only fetch payments where seller_id == currentUserId
paymentsCollection
    .whereEqualTo("seller_id", sellerId)
    .get()
```

---

#### **PaymentDetailScreen** (Individual Payment Details)
**Purpose**: Comprehensive view of a single payment transaction

**Key Responsibilities**:
- Display complete payment information
- Show itemized breakdown of products
- Display payment timeline
- Enable refund processing (if applicable)
- Show co-seller information (if multi-seller order)

**Displayed Information**:
- **Status Card**:
  - Payment status with icon
  - Total amount
  - Order reference

- **Payment Information**:
  - Buyer name
  - Payment method
  - Item count
  - Payment date
  - Transaction ID (if available)

- **Items Details**:
  - Product title
  - Quantity
  - Unit price
  - Item total

- **Timeline**:
  - Payment created date
  - Payment completed date (if applicable)
  - Refund processed date (if applicable)

- **Co-Seller Indicator** (if applicable):
  - Store name
  - Note about multi-seller order
  - Link to view complete payment split

**User Actions**:
- Process refund (for pending payments)
- View co-seller payment split (if multi-seller)
- Navigate back to payment list

**Data Access Pattern**:
```kotlin
// Fetch specific payment by ID
paymentsCollection.document(paymentId).get()

// Verify ownership before displaying
if (payment.sellerId == currentUserId) {
    // Display payment details
}
```

---

#### **CoSellerPaymentSplitScreen** (Multi-Seller Payment Breakdown)
**Purpose**: Transparent view of how payment is split among multiple sellers in a single order

**Key Responsibilities**:
- Display total order amount
- Show platform fee deduction
- Break down individual seller payouts
- Display each seller's items and amounts
- Show payment status for each seller

**Displayed Information**:
- **Order Information**:
  - Order ID
  - Number of sellers involved
  - Co-seller order indicator

- **Financial Summary**:
  - Total Order Amount
  - Platform Fee (5%)
  - Total Payout (after fees)

- **Seller Breakdown** (for each seller):
  - Seller name
  - Store name
  - Payment status (with badge)
  - Items list with quantities and prices
  - Seller's payout amount

- **Information Note**:
  - Explanation that each seller receives their portion
  - Note that payments are processed separately

**User Actions**:
- View complete breakdown
- Navigate back

**Data Access Pattern**:
```kotlin
// Fetch all payments for an order
paymentsCollection
    .whereEqualTo("order_id", orderId)
    .get()

// Verify current user is one of the sellers
val isCurrentUserInvolved = payments.any { 
    it.sellerId == currentUserId 
}
```

---

## Part 2: Co-Seller Data Access Policy

### 2.1 Professional Recommendation: Data Isolation Model

**RECOMMENDED APPROACH**: **Strict Data Isolation**

In a co-seller store, **each seller should ONLY see their own payment and earnings records**. Other sellers' financial data should remain completely restricted.

### 2.2 Rationale for Data Isolation

#### **Security & Privacy**
- Financial information is sensitive and confidential
- Sellers should not have visibility into competitors' earnings
- Prevents unauthorized access to business intelligence
- Complies with data protection regulations (GDPR, CCPA, etc.)

#### **Business Logic**
- Each seller operates independently
- Co-seller stores are partnerships, not shared ownership
- Earnings are seller-specific, not store-specific
- Payment processing is individual per seller

#### **Platform Integrity**
- Prevents disputes over payment calculations
- Reduces support tickets about "why seller X earned more"
- Maintains clear accountability
- Simplifies audit trails

### 2.3 Data Access Control Implementation

#### **Rule 1: Seller Payment Visibility**
```kotlin
// ✅ ALLOWED: Seller viewing their own payments
if (payment.sellerId == currentUserId) {
    displayPaymentDetails(payment)
}

// ❌ DENIED: Seller viewing another seller's payments
if (payment.sellerId != currentUserId) {
    showAccessDeniedError()
}
```

#### **Rule 2: Co-Seller Order Payment Split Visibility**
```kotlin
// ✅ ALLOWED: Seller viewing payment split for their own payment
val orderPayments = getOrderPayments(orderId)
if (orderPayments.any { it.sellerId == currentUserId }) {
    // Seller is involved in this order
    displayPaymentSplit(orderPayments)
} else {
    // Seller is not involved
    showAccessDeniedError()
}

// ❌ DENIED: Seller viewing payment split for orders they're not involved in
```

#### **Rule 3: Statistics & Analytics**
```kotlin
// ✅ ALLOWED: Seller viewing their own statistics
val stats = getSellerPaymentStats(currentUserId)
displayStats(stats)

// ❌ DENIED: Seller viewing another seller's statistics
// No cross-seller analytics access
```

### 2.4 Firestore Security Rules

```javascript
// Firestore Rules for seller_payments collection
match /seller_payments/{document=**} {
  // Read: Only the seller who owns this payment can view it
  allow read: if request.auth.uid == resource.data.seller_id;
  
  // Write: Only admin or system can create/update payments
  allow create, update, delete: if false;
  
  // Exception: Allow reading payment split for involved sellers
  allow read: if request.auth.uid in resource.data.involved_seller_ids;
}
```

### 2.5 Repository-Level Access Control

```kotlin
// PaymentRepository.kt - Add access control layer

suspend fun getSellerPayments(
    sellerId: String,
    requestingUserId: String  // Add this parameter
): Result<List<SellerPayment>> {
    return try {
        // ✅ Verify requesting user is the seller
        if (sellerId != requestingUserId) {
            return Result.failure(
                SecurityException("Unauthorized: Cannot access other seller's payments")
            )
        }
        
        // Proceed with query
        val snapshot = paymentsCollection
            .whereEqualTo("seller_id", sellerId)
            .get()
            .await()
        
        // ... rest of implementation
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun getOrderPayments(
    orderId: String,
    requestingUserId: String  // Add this parameter
): Result<List<SellerPayment>> {
    return try {
        val snapshot = paymentsCollection
            .whereEqualTo("order_id", orderId)
            .get()
            .await()
        
        val payments = snapshot.documents.mapNotNull { doc ->
            doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
        }
        
        // ✅ Verify requesting user is involved in this order
        val isUserInvolved = payments.any { it.sellerId == requestingUserId }
        if (!isUserInvolved) {
            return Result.failure(
                SecurityException("Unauthorized: Not involved in this order")
            )
        }
        
        // Return only the payments (user can see split for their own payment)
        Result.success(payments)
        
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 2.6 ViewModel-Level Access Control

```kotlin
// SellerPaymentViewModel.kt - Add access control

class SellerPaymentViewModel(
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val currentUserId = authRepository.getCurrentUserId()
    
    fun loadSellerPayments(sellerId: String) {
        viewModelScope.launch {
            // ✅ Verify user is requesting their own payments
            if (sellerId != currentUserId) {
                _paymentState.value = PaymentUiState.Error(
                    "Unauthorized: Cannot access other seller's payments"
                )
                return@launch
            }
            
            // Proceed with loading
            val result = paymentRepository.getSellerPayments(
                sellerId = sellerId,
                requestingUserId = currentUserId
            )
            
            _paymentState.value = when (result) {
                is Result.Success -> PaymentUiState.Success(result.getOrNull() ?: emptyList())
                is Result.Failure -> PaymentUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun loadPaymentDetail(paymentId: String) {
        viewModelScope.launch {
            val result = paymentRepository.getPaymentById(paymentId)
            
            when (result) {
                is Result.Success -> {
                    val payment = result.getOrNull()
                    
                    // ✅ Verify user owns this payment
                    if (payment?.sellerId != currentUserId) {
                        _selectedPayment.value = null
                        // Log security event
                        logSecurityEvent("Unauthorized payment access attempt: $paymentId")
                        return@launch
                    }
                    
                    _selectedPayment.value = payment
                }
                is Result.Failure -> {
                    _selectedPayment.value = null
                }
            }
        }
    }
}
```

---

## Part 3: Screen Display Hierarchy in Production

### 3.1 Navigation Flow for Sellers

```
Seller Dashboard
    ↓
[Payments Tab] ← User clicks
    ↓
SellerPaymentsScreen
    ├─ Display all seller's payments
    ├─ Show statistics
    ├─ Enable filtering
    │
    └─ User clicks on payment card
        ↓
        PaymentDetailScreen
            ├─ Display payment details
            ├─ Show items breakdown
            ├─ Show timeline
            │
            └─ If co-seller order:
                └─ User clicks "View Payment Split"
                    ↓
                    CoSellerPaymentSplitScreen
                        ├─ Display all sellers' payouts
                        ├─ Show platform fee
                        ├─ Show total breakdown
                        │
                        └─ User clicks back
                            ↓
                            PaymentDetailScreen
```

### 3.2 Access Control at Each Level

| Screen | Access Rule | Data Shown |
|--------|-------------|-----------|
| **SellerPaymentsScreen** | Only own payments | All payments where seller_id == currentUserId |
| **PaymentDetailScreen** | Only own payments | Single payment if seller_id == currentUserId |
| **CoSellerPaymentSplitScreen** | Only if involved | All sellers' payouts if currentUserId is in order |

---

## Part 4: Implementation Checklist

### 4.1 Security Implementation

- [ ] Add `requestingUserId` parameter to all payment repository methods
- [ ] Implement access control checks in repository layer
- [ ] Add access control checks in ViewModel layer
- [ ] Implement Firestore security rules
- [ ] Add logging for unauthorized access attempts
- [ ] Add error handling for access denied scenarios

### 4.2 Data Model Updates

- [ ] Add `involved_seller_ids` field to Order model (for co-seller orders)
- [ ] Add `involved_seller_ids` field to SellerPayment model
- [ ] Update payment creation to include involved sellers list

### 4.3 UI/UX Implementation

- [ ] Display "Access Denied" error gracefully
- [ ] Show co-seller indicator only for involved sellers
- [ ] Disable payment split view for uninvolved sellers
- [ ] Add security indicators in UI (e.g., "Your Payment" badge)

### 4.4 Testing

- [ ] Test seller can view own payments
- [ ] Test seller cannot view other seller's payments
- [ ] Test seller can view payment split for their orders
- [ ] Test seller cannot view payment split for other orders
- [ ] Test unauthorized access logging

---

## Part 5: Best Practices & Recommendations

### 5.1 Financial Data Handling

1. **Encryption**: Encrypt sensitive payment data in transit and at rest
2. **Audit Logging**: Log all payment access and modifications
3. **Rate Limiting**: Implement rate limiting on payment queries
4. **Data Retention**: Define clear data retention policies
5. **PCI Compliance**: Ensure compliance with payment card industry standards

### 5.2 Co-Seller Store Management

1. **Clear Roles**: Define clear roles (owner, member, viewer)
2. **Role-Based Access**: Implement role-based access control
3. **Activity Logging**: Log all financial activities
4. **Dispute Resolution**: Have clear process for payment disputes
5. **Transparency**: Show clear breakdown of fees and payouts

### 5.3 Error Handling

```kotlin
// Graceful error handling for access denied
when (result) {
    is Result.Failure -> {
        val error = result.exceptionOrNull()
        when (error) {
            is SecurityException -> {
                // Show user-friendly message
                showError("You don't have permission to view this payment")
                logSecurityEvent(error.message)
            }
            else -> {
                showError("Failed to load payment details")
            }
        }
    }
}
```

### 5.4 Performance Optimization

1. **Pagination**: Implement pagination for large payment lists
2. **Caching**: Cache seller's own payment statistics
3. **Indexing**: Create Firestore indexes on seller_id and order_id
4. **Lazy Loading**: Load payment details on demand

---

## Part 6: Summary Table

| Aspect | Recommendation | Rationale |
|--------|---|---|
| **Data Isolation** | Strict isolation per seller | Security, privacy, compliance |
| **Payment Visibility** | Only own payments | Prevent unauthorized access |
| **Co-Seller Split** | Only if involved | Transparency for involved parties |
| **Statistics** | Individual only | Prevent competitive intelligence leaks |
| **Access Control** | Multi-layer (Repo + VM + Firestore) | Defense in depth |
| **Error Handling** | Graceful with logging | Security + UX |
| **Audit Logging** | All payment access | Compliance + dispute resolution |

---

## Conclusion

This architecture ensures:
- ✅ **Security**: Strict data isolation prevents unauthorized access
- ✅ **Privacy**: Sellers' financial data remains confidential
- ✅ **Compliance**: Meets regulatory requirements
- ✅ **Transparency**: Clear payment breakdown for involved parties
- ✅ **Scalability**: Efficient queries with proper indexing
- ✅ **Maintainability**: Clear access control patterns

Implement this policy to build a trustworthy, secure payment system for your multi-seller platform.
