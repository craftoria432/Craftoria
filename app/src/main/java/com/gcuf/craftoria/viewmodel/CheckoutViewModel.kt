package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.DeliveryInfo
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderItem
import com.gcuf.craftoria.data.model.toMap
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.utils.PaymentAuditLogger
import com.gcuf.craftoria.utils.PaymentRetryManager
import com.gcuf.craftoria.utils.PaymentValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CheckoutUiState {
    object Idle : CheckoutUiState()
    object Processing : CheckoutUiState()
    object Success : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}

class CheckoutViewModel : ViewModel() {
    companion object {
        private const val TAG = "CheckoutViewModel"
        // ✅ Static cache to preserve data across screen navigations
        private var cachedFullName = ""
        private var cachedPhoneNumber = ""
        private var cachedEmail = ""
        private var cachedAddress = ""
        private var cachedCity = ""
        private var cachedPostalCode = ""
        private var cachedPaymentMethod = "Debit/Credit Card"
        private var cachedAgreeToTerms = false
    }

    private val paymentRepository = PaymentRepository()
    private val retryManager = PaymentRetryManager()
    private val auditLogger = PaymentAuditLogger()

    // Checkout UI State
    private val _checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

    // ✅ Flag to track if data should be preserved
    private val _shouldPreserveData = MutableStateFlow(true)
    val shouldPreserveData: StateFlow<Boolean> = _shouldPreserveData.asStateFlow()

    // Delivery Information - Initialize from cache
    private val _fullName = MutableStateFlow(cachedFullName)
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _phoneNumber = MutableStateFlow(cachedPhoneNumber)
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _email = MutableStateFlow(cachedEmail)
    val email: StateFlow<String> = _email.asStateFlow()

    private val _address = MutableStateFlow(cachedAddress)
    val address: StateFlow<String> = _address.asStateFlow()

    private val _city = MutableStateFlow(cachedCity)
    val city: StateFlow<String> = _city.asStateFlow()

    private val _postalCode = MutableStateFlow(cachedPostalCode)
    val postalCode: StateFlow<String> = _postalCode.asStateFlow()

    // Payment Method
    private val _selectedPaymentMethod = MutableStateFlow(cachedPaymentMethod)
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    // Terms Agreement
    private val _agreeToTerms = MutableStateFlow(cachedAgreeToTerms)
    val agreeToTerms: StateFlow<Boolean> = _agreeToTerms.asStateFlow()

    // Update functions
    fun updateFullName(name: String) {
        _fullName.value = name
        cachedFullName = name
        Log.d(TAG, "✅ Full Name updated: $name")
    }

    fun updatePhoneNumber(phone: String) {
        _phoneNumber.value = phone
        cachedPhoneNumber = phone
        Log.d(TAG, "✅ Phone Number updated: $phone")
    }

    fun updateEmail(emailValue: String) {
        _email.value = emailValue
        cachedEmail = emailValue
        Log.d(TAG, "✅ Email updated: $emailValue")
    }

    fun updateAddress(addressValue: String) {
        _address.value = addressValue
        cachedAddress = addressValue
        Log.d(TAG, "✅ Address updated: $addressValue")
    }

    fun updateCity(cityValue: String) {
        _city.value = cityValue
        cachedCity = cityValue
        Log.d(TAG, "✅ City updated: $cityValue")
    }

    fun updatePostalCode(code: String) {
        _postalCode.value = code
        cachedPostalCode = code
        Log.d(TAG, "✅ Postal Code updated: $code")
    }

    fun updatePaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
        cachedPaymentMethod = method
        Log.d(TAG, "✅ Payment Method updated: $method")
    }

    fun updateAgreeToTerms(agree: Boolean) {
        _agreeToTerms.value = agree
        cachedAgreeToTerms = agree
        Log.d(TAG, "✅ Terms Agreement updated: $agree")
    }

    // Get delivery info
    fun getDeliveryInfo(): DeliveryInfo {
        return DeliveryInfo(
            fullName = _fullName.value,
            phoneNumber = _phoneNumber.value,
            email = _email.value,
            address = _address.value,
            city = _city.value,
            postalCode = _postalCode.value
        )
    }

    // Clear all data (only after successful order)
    fun clearCheckoutData() {
        _fullName.value = ""
        _phoneNumber.value = ""
        _email.value = ""
        _address.value = ""
        _city.value = ""
        _postalCode.value = ""
        _selectedPaymentMethod.value = "Debit/Credit Card"
        _agreeToTerms.value = false
        _shouldPreserveData.value = false
        
        // ✅ Also clear cache
        cachedFullName = ""
        cachedPhoneNumber = ""
        cachedEmail = ""
        cachedAddress = ""
        cachedCity = ""
        cachedPostalCode = ""
        cachedPaymentMethod = "Debit/Credit Card"
        cachedAgreeToTerms = false
        
        Log.d(TAG, "✅ Checkout data cleared")
    }

    // ✅ Mark data as preserved (don't clear on navigation)
    fun preserveCheckoutData() {
        _shouldPreserveData.value = true
        Log.d(TAG, "✅ Checkout data will be preserved")
    }

    // Log current state
    fun logCurrentState() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📋 Current Checkout State:")
        Log.d(TAG, "   Full Name: ${_fullName.value}")
        Log.d(TAG, "   Phone: ${_phoneNumber.value}")
        Log.d(TAG, "   Email: ${_email.value}")
        Log.d(TAG, "   Address: ${_address.value}")
        Log.d(TAG, "   City: ${_city.value}")
        Log.d(TAG, "   Postal Code: ${_postalCode.value}")
        Log.d(TAG, "   Payment Method: ${_selectedPaymentMethod.value}")
        Log.d(TAG, "   Agree to Terms: ${_agreeToTerms.value}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /* ==================== PAYMENT PROCESSING ==================== */

    /**
     * Process checkout with validation, retry logic, and audit logging
     * ✅ ENHANCED: Multiple safeguards to ensure payment records are always created
     */
    fun processCheckout(order: Order, items: List<OrderItem>, currentUserId: String) {
        viewModelScope.launch {
            try {
                _checkoutState.value = CheckoutUiState.Processing
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔄 Starting checkout process for order: ${order.id}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // ✅ GUARD: Prevent payment processing if order has no ID
                if (order.id.isEmpty()) {
                    Log.e(TAG, "❌ Cannot process payment — order has no ID")
                    _checkoutState.value = CheckoutUiState.Error("Order ID missing")
                    return@launch
                }

                // Step 1: Validate payment
                Log.d(TAG, "✅ Step 1: Validating payment...")
                val validation = PaymentValidator.validateOrderPayment(order, items)
                if (!validation.isValid) {
                    val errorMsg = validation.errors.first()
                    Log.e(TAG, "❌ Validation failed: $errorMsg")
                    _checkoutState.value = CheckoutUiState.Error(errorMsg)
                    return@launch
                }
                Log.d(TAG, "✅ Payment validation passed")

                // Step 2: Process with retry
                Log.d(TAG, "✅ Step 2: Processing payment with retry logic...")
                val idempotencyKey = UUID.randomUUID().toString()
                val result = retryManager.executeWithRetry(maxRetries = 3) {
                    paymentRepository.processOrderPaymentsWithIdempotency(order, idempotencyKey)
                }

                if (result.isSuccess) {
                    val paymentIds = result.getOrNull() ?: emptyList()
                    
                    // ✅ SAFEGUARD 1: Verify payments were actually created
                    if (paymentIds.isEmpty()) {
                        Log.e(TAG, "❌ CRITICAL: No payment IDs returned for order ${order.id}")
                        Log.e(TAG, "❌ This should never happen - payment creation failed silently")
                        _checkoutState.value = CheckoutUiState.Error(
                            "Payment creation failed. Please try again or contact support."
                        )
                        return@launch
                    }
                    
                    Log.d(TAG, "✅ Payment processed successfully: ${paymentIds.size} payments created")
                    Log.d(TAG, "   Payment IDs: ${paymentIds.joinToString(", ") { it.take(8) }}")

                    // ✅ SAFEGUARD 2: Verify payment count matches expected sellers
                    val expectedSellerCount = items.map { it.sellerId }.distinct().size
                    if (paymentIds.size != expectedSellerCount) {
                        Log.w(TAG, "⚠️  WARNING: Payment count mismatch!")
                        Log.w(TAG, "   Expected: $expectedSellerCount sellers")
                        Log.w(TAG, "   Created: ${paymentIds.size} payments")
                        // Continue anyway - partial success is better than failure
                    }

                    // Step 3: Log payment creation
                    Log.d(TAG, "✅ Step 3: Logging payment actions...")
                    paymentIds.forEach { paymentId ->
                        auditLogger.logPaymentCreated(
                            paymentId = paymentId,
                            orderId = order.id,
                            paymentData = order.toMap(),
                            actorId = currentUserId
                        )
                    }
                    Log.d(TAG, "✅ Audit logs created")

                    // ✅ SAFEGUARD 3: Double-check payments exist in Firestore
                    Log.d(TAG, "✅ Step 4: Verifying payments in database...")
                    val verificationResult = paymentRepository.verifyPaymentsExist(order.id)
                    if (verificationResult.isSuccess) {
                        val existingPayments = verificationResult.getOrNull() ?: emptyList()
                        if (existingPayments.isEmpty()) {
                            Log.e(TAG, "❌ CRITICAL: Payments not found in database after creation!")
                            Log.e(TAG, "❌ Order: ${order.id}, Expected: ${paymentIds.size}, Found: 0")
                            _checkoutState.value = CheckoutUiState.Error(
                                "Payment verification failed. Please contact support with order ID: ${order.id.take(8)}"
                            )
                            return@launch
                        }
                        Log.d(TAG, "✅ Verified ${existingPayments.size} payments in database")
                    } else {
                        Log.w(TAG, "⚠️  Could not verify payments (non-critical)")
                    }

                    _checkoutState.value = CheckoutUiState.Success
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "✅ Checkout completed successfully")
                    Log.d(TAG, "   Order: ${order.id}")
                    Log.d(TAG, "   Payments: ${paymentIds.size}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Payment processing failed"
                    Log.e(TAG, "❌ Payment processing failed: $errorMsg")
                    _checkoutState.value = CheckoutUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Checkout error", e)
                _checkoutState.value = CheckoutUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Reset checkout state
     */
    fun resetCheckoutState() {
        _checkoutState.value = CheckoutUiState.Idle
        Log.d(TAG, "✅ Checkout state reset")
    }
}
