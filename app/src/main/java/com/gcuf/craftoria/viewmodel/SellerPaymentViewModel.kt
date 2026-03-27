package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.AuthRepository
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.data.repository.SellerPaymentStats
import com.gcuf.craftoria.utils.RefundProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PaymentUiState {
    object Loading : PaymentUiState()
    data class Success(val payments: List<SellerPayment>) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

sealed class PaymentStatsUiState {
    object Loading : PaymentStatsUiState()
    data class Success(val stats: SellerPaymentStats) : PaymentStatsUiState()
    data class Error(val message: String) : PaymentStatsUiState()
}

sealed class RefundUiState {
    object Idle : RefundUiState()
    object Processing : RefundUiState()
    data class Success(val refundId: String) : RefundUiState()
    data class Error(val message: String) : RefundUiState()
}

class SellerPaymentViewModel : ViewModel() {
    private val paymentRepository = PaymentRepository()
    private val authRepository = AuthRepository()
    private val refundProcessor = RefundProcessor()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "SellerPaymentViewModel"

    // ✅ NEW: Track current user for access control
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // Payment list state
    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val paymentState: StateFlow<PaymentUiState> = _paymentState

    // Payment stats state
    private val _statsState = MutableStateFlow<PaymentStatsUiState>(PaymentStatsUiState.Loading)
    val statsState: StateFlow<PaymentStatsUiState> = _statsState

    // ✅ NEW: Refund state
    private val _refundState = MutableStateFlow<RefundUiState>(RefundUiState.Idle)
    val refundState: StateFlow<RefundUiState> = _refundState

    // Selected payment detail
    private val _selectedPayment = MutableStateFlow<SellerPayment?>(null)
    val selectedPayment: StateFlow<SellerPayment?> = _selectedPayment

    // Filter state
    private val _selectedStatus = MutableStateFlow<PaymentStatus?>(null)
    val selectedStatus: StateFlow<PaymentStatus?> = _selectedStatus

    private var paymentListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var statsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    /**
     * ✅ Start real-time listener for seller payments
     */
    fun startRealtimePaymentListener(sellerId: String) {
        Log.d(TAG, "🔴 Starting real-time payment listener for seller: $sellerId")
        
        // Remove old listener
        paymentListenerRegistration?.remove()
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        paymentListenerRegistration = db.collection("seller_payments")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to payments", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                    Log.d(TAG, "🔄 Real-time payment update received: ${snapshot.documentChanges.size} changes")
                    viewModelScope.launch {
                        try {
                            val result = paymentRepository.getSellerPayments(
                                sellerId = sellerId,
                                requestingUserId = currentUserId,
                                status = null
                            )
                            
                            if (result.isSuccess) {
                                val allPayments = result.getOrNull() ?: emptyList()
                                // Filter out co-seller store payments
                                val filteredPayments = allPayments.filter { payment ->
                                    payment.coSellerStoreId.isEmpty()
                                }
                                _paymentState.value = PaymentUiState.Success(filteredPayments)
                                Log.d(TAG, "✅ Payments updated in real-time: ${filteredPayments.size}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating payments", e)
                        }
                    }
                }
            }
    }

    /**
     * ✅ Start real-time listener for seller payment stats
     */
    fun startRealtimeStatsListener(sellerId: String) {
        Log.d(TAG, "🔴 Starting real-time stats listener for seller: $sellerId")
        
        // Remove old listener
        statsListenerRegistration?.remove()
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        statsListenerRegistration = db.collection("seller_payments")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to stats", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                    Log.d(TAG, "🔄 Real-time stats update received")
                    viewModelScope.launch {
                        try {
                            val result = paymentRepository.getSellerPaymentStats(sellerId)
                            if (result.isSuccess) {
                                val stats = result.getOrNull() ?: return@launch
                                _statsState.value = PaymentStatsUiState.Success(stats)
                                Log.d(TAG, "✅ Stats updated in real-time")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating stats", e)
                        }
                    }
                }
            }
    }

    /* ==================== PAYMENT QUERIES ==================== */

    /**
     * Load seller payments with access control
     * ✅ SECURITY: Only the seller can view their own payments
     * ✅ FILTER: Exclude co-seller store payments (those are on store dashboard)
     */
    fun loadSellerPayments(sellerId: String, status: PaymentStatus? = null) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentUiState.Loading
                Log.d(TAG, "Loading payments for seller: $sellerId")

                // ✅ SECURITY CHECK: Verify user is requesting their own payments
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

                // ✅ NEW: Filter out co-seller store payments
                // Original sellers should only see payments for their own products
                // Co-seller store payments are shown on the store dashboard
                if (result.isSuccess) {
                    val allPayments = result.getOrNull() ?: emptyList()
                    val filteredPayments = allPayments.filter { payment ->
                        // Only include payments where coSellerStoreId is empty
                        // This means it's an original seller payment, not a co-seller store payment
                        payment.coSellerStoreId.isEmpty()
                    }
                    _paymentState.value = PaymentUiState.Success(filteredPayments)
                    Log.d(TAG, "✅ Loaded ${filteredPayments.size} original seller payments (filtered from ${allPayments.size} total)")
                    
                    // ✅ Start real-time listener after initial load
                    startRealtimePaymentListener(sellerId)
                } else {
                    _paymentState.value = PaymentUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }

                result.onSuccess { payments ->
                    Log.d(TAG, "✅ Loaded ${payments.size} payments")
                    _paymentState.value = PaymentUiState.Success(payments)
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load payments", error)
                    _paymentState.value = PaymentUiState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading payments", e)
                _paymentState.value = PaymentUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadPaymentStats(sellerId: String) {
        viewModelScope.launch {
            try {
                _statsState.value = PaymentStatsUiState.Loading
                Log.d(TAG, "Loading payment stats for seller: $sellerId")

                // ✅ SECURITY CHECK: Verify user is requesting their own stats
                if (sellerId != currentUserId) {
                    Log.w(TAG, "🚫 UNAUTHORIZED: User $currentUserId attempted to access stats for seller $sellerId")
                    _statsState.value = PaymentStatsUiState.Error(
                        "Unauthorized: Cannot access other seller's statistics"
                    )
                    return@launch
                }

                val result = paymentRepository.getSellerPaymentStats(sellerId)

                result.onSuccess { stats ->
                    Log.d(TAG, "✅ Loaded payment stats")
                    _statsState.value = PaymentStatsUiState.Success(stats)
                    
                    // ✅ Start real-time listener after initial load
                    startRealtimeStatsListener(sellerId)
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load stats", error)
                    _statsState.value = PaymentStatsUiState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading stats", e)
                _statsState.value = PaymentStatsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load payment detail with access control
     * ✅ SECURITY: Only the seller who owns this payment can view it
     */
    fun loadPaymentDetail(paymentId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading payment detail: $paymentId")

                val result = paymentRepository.getPaymentById(
                    paymentId = paymentId,
                    requestingUserId = currentUserId
                )

                result.onSuccess { payment ->
                    if (payment == null) {
                        Log.w(TAG, "Payment not found: $paymentId")
                        _selectedPayment.value = null
                        return@onSuccess
                    }
                    Log.d(TAG, "✅ Loaded payment detail")
                    _selectedPayment.value = payment
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load payment detail", error)
                    _selectedPayment.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading payment detail", e)
                _selectedPayment.value = null
            }
        }
    }

    /**
     * Load order payments with access control
     * ✅ SECURITY: Only sellers involved in the order can view the payment split
     */
    fun loadOrderPayments(orderId: String) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentUiState.Loading
                Log.d(TAG, "Loading payments for order: $orderId")

                val result = paymentRepository.getOrderPayments(
                    orderId = orderId,
                    requestingUserId = currentUserId
                )

                result.onSuccess { payments ->
                    Log.d(TAG, "✅ Loaded ${payments.size} payments for order")
                    _paymentState.value = PaymentUiState.Success(payments)
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load order payments", error)
                    _paymentState.value = PaymentUiState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading order payments", e)
                _paymentState.value = PaymentUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /* ==================== PAYMENT ACTIONS ==================== */

    fun updatePaymentStatus(paymentId: String, newStatus: PaymentStatus, transactionId: String = "") {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating payment status: $paymentId -> $newStatus")

                val result = paymentRepository.updatePaymentStatus(paymentId, newStatus, transactionId)

                result.onSuccess {
                    Log.d(TAG, "✅ Payment status updated")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to update payment status", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating payment status", e)
            }
        }
    }

    fun markPaymentCompleted(paymentId: String, transactionId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Marking payment as completed: $paymentId")

                val result = paymentRepository.markPaymentCompleted(paymentId, transactionId)

                result.onSuccess {
                    Log.d(TAG, "✅ Payment marked as completed")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to mark payment as completed", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception marking payment as completed", e)
            }
        }
    }

    fun processRefund(paymentId: String, refundAmount: Double, reason: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Processing refund for payment: $paymentId")

                val result = paymentRepository.processRefund(paymentId, refundAmount, reason)

                result.onSuccess {
                    Log.d(TAG, "✅ Refund processed")
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to process refund", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception processing refund", e)
            }
        }
    }

    /* ==================== FILTER OPERATIONS ==================== */

    fun setStatusFilter(status: PaymentStatus?) {
        _selectedStatus.value = status
    }

    fun clearFilters() {
        _selectedStatus.value = null
    }

    fun getFilteredPayments(payments: List<SellerPayment>): List<SellerPayment> {
        val status = _selectedStatus.value ?: return payments
        return payments.filter { it.status == status.toString() }
    }

    /* ==================== UTILITY FUNCTIONS ==================== */

    fun getTotalEarnings(payments: List<SellerPayment>): Double {
        return payments.sumOf { it.amount }
    }

    fun getCompletedEarnings(payments: List<SellerPayment>): Double {
        return payments
            .filter { it.status == PaymentStatus.COMPLETED.toString() }
            .sumOf { it.amount }
    }

    fun getPendingEarnings(payments: List<SellerPayment>): Double {
        return payments
            .filter { it.status == PaymentStatus.PENDING.toString() }
            .sumOf { it.amount }
    }

    fun getPaymentsByStatus(payments: List<SellerPayment>, status: PaymentStatus): List<SellerPayment> {
        return payments.filter { it.status == status.toString() }
    }

    /* ==================== REFUND PROCESSING ==================== */

    /**
     * Initiate refund for a payment
     * ✅ SECURITY: Only the seller who owns this payment can initiate refund
     */
    fun initiateRefund(paymentId: String, refundAmount: Double, reason: String) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Processing
                Log.d(TAG, "🔄 Initiating refund for payment: $paymentId")
                Log.d(TAG, "💰 Refund amount: PKR $refundAmount")
                Log.d(TAG, "📝 Reason: $reason")

                val result = refundProcessor.initiateRefund(
                    paymentId = paymentId,
                    refundAmount = refundAmount,
                    reason = reason,
                    requestedBy = currentUserId
                )

                if (result.isSuccess) {
                    val refundId = result.getOrNull() ?: ""
                    Log.d(TAG, "✅ Refund initiated: $refundId")
                    _refundState.value = RefundUiState.Success(refundId)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Refund initiation failed"
                    Log.e(TAG, "❌ Refund initiation failed: $errorMsg")
                    _refundState.value = RefundUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception initiating refund", e)
                _refundState.value = RefundUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Process refund with transaction ID
     * ✅ SECURITY: Only admin can process refunds
     */
    fun processRefundWithTransaction(refundId: String, transactionId: String) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Processing
                Log.d(TAG, "🔄 Processing refund: $refundId")
                Log.d(TAG, "🔗 Transaction ID: $transactionId")

                val result = refundProcessor.processRefund(
                    refundId = refundId,
                    transactionId = transactionId,
                    actorId = currentUserId
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Refund processed successfully")
                    _refundState.value = RefundUiState.Success(refundId)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Refund processing failed"
                    Log.e(TAG, "❌ Refund processing failed: $errorMsg")
                    _refundState.value = RefundUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception processing refund", e)
                _refundState.value = RefundUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Cancel pending refund
     */
    fun cancelRefund(refundId: String, reason: String) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Processing
                Log.d(TAG, "🔄 Cancelling refund: $refundId")

                val result = refundProcessor.cancelRefund(
                    refundId = refundId,
                    reason = reason,
                    actorId = currentUserId
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Refund cancelled")
                    _refundState.value = RefundUiState.Success(refundId)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Refund cancellation failed"
                    Log.e(TAG, "❌ Refund cancellation failed: $errorMsg")
                    _refundState.value = RefundUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception cancelling refund", e)
                _refundState.value = RefundUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Reset refund state
     */
    fun resetRefundState() {
        _refundState.value = RefundUiState.Idle
        Log.d(TAG, "✅ Refund state reset")
    }

    override fun onCleared() {
        super.onCleared()
        paymentListenerRegistration?.remove()
        statsListenerRegistration?.remove()
        Log.d(TAG, "🔴 Real-time listeners removed")
    }
}
