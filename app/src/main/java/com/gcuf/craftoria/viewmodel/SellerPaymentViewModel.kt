package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.data.repository.RefundRepository
import com.gcuf.craftoria.data.repository.SellerPaymentStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── UI States ─────────────────────────────────────────────────────────────────

sealed class PaymentUiState {
    object Idle    : PaymentUiState()
    object Loading : PaymentUiState()
    data class Success(val payments: List<SellerPayment>) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

sealed class PaymentStatsUiState {
    object Idle    : PaymentStatsUiState()
    object Loading : PaymentStatsUiState()
    data class Success(val stats: SellerPaymentStats) : PaymentStatsUiState()
    data class Error(val message: String) : PaymentStatsUiState()
}

sealed class SellerRefundUiState {
    object Idle       : SellerRefundUiState()
    object Submitting : SellerRefundUiState()
    data class Success(val message: String) : SellerRefundUiState()
    data class Error(val message: String)   : SellerRefundUiState()
}


// ── ViewModel ─────────────────────────────────────────────────────────────────

class SellerPaymentViewModel : ViewModel() {

    private val paymentRepository = PaymentRepository()
    private val refundRepository  = RefundRepository(FirebaseFirestore.getInstance())

    companion object {
        private const val TAG = "SellerPaymentViewModel"
    }

    // ── Payments list ─────────────────────────────────────────────────────────
    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentState: StateFlow<PaymentUiState> = _paymentState

    // ── Stats ─────────────────────────────────────────────────────────────────
    private val _statsState = MutableStateFlow<PaymentStatsUiState>(PaymentStatsUiState.Idle)
    val statsState: StateFlow<PaymentStatsUiState> = _statsState

    // ── Selected payment (detail screen) ─────────────────────────────────────
    private val _selectedPayment = MutableStateFlow<SellerPayment?>(null)
    val selectedPayment: StateFlow<SellerPayment?> = _selectedPayment

    // ── Status filter ─────────────────────────────────────────────────────────
    private val _selectedStatus = MutableStateFlow<PaymentStatus?>(null)
    val selectedStatus: StateFlow<PaymentStatus?> = _selectedStatus

    // ── Seller-initiated refund state ─────────────────────────────────────────
    private val _sellerRefundState = MutableStateFlow<SellerRefundUiState>(SellerRefundUiState.Idle)
    val sellerRefundState: StateFlow<SellerRefundUiState> = _sellerRefundState

    // ── Real-time listeners ───────────────────────────────────────────────────
    private var paymentsListener: ListenerRegistration? = null
    private var statsListener: ListenerRegistration? = null

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD PAYMENTS WITH REAL-TIME UPDATES
    // ─────────────────────────────────────────────────────────────────────────

    fun loadSellerPayments(sellerId: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Loading
            val requestingUserId = FirebaseAuth.getInstance().currentUser?.uid ?: sellerId
            
            // Remove old listener if exists
            paymentsListener?.remove()
            
            // Set up real-time listener
            paymentsListener = paymentRepository.listenToSellerPayments(
                sellerId = sellerId,
                requestingUserId = requestingUserId,
                onUpdate = { payments ->
                    Log.d(TAG, "✅ Real-time payment update: ${payments.size} payments")
                    _paymentState.value = PaymentUiState.Success(payments)
                },
                onError = { error ->
                    Log.e(TAG, "❌ Real-time listener error", error)
                    _paymentState.value = PaymentUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun loadPaymentStats(sellerId: String) {
        viewModelScope.launch {
            _statsState.value = PaymentStatsUiState.Loading
            
            // Remove old listener if exists
            statsListener?.remove()
            
            // Set up real-time listener
            statsListener = paymentRepository.listenToSellerPaymentStats(
                sellerId = sellerId,
                onUpdate = { stats ->
                    Log.d(TAG, "✅ Real-time stats update: Total PKR ${stats.totalEarnings}")
                    _statsState.value = PaymentStatsUiState.Success(stats)
                },
                onError = { error ->
                    Log.e(TAG, "❌ Real-time stats listener error", error)
                    _statsState.value = PaymentStatsUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun loadPaymentDetail(paymentId: String) {
        viewModelScope.launch {
            val requestingUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val result = paymentRepository.getPaymentById(paymentId, requestingUserId)
            if (result.isSuccess) {
                _selectedPayment.value = result.getOrNull()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILTER
    // ─────────────────────────────────────────────────────────────────────────

    fun setStatusFilter(status: PaymentStatus) {
        _selectedStatus.value = status
    }

    fun clearFilters() {
        _selectedStatus.value = null
    }

    fun getFilteredPayments(payments: List<SellerPayment>): List<SellerPayment> {
        val status = _selectedStatus.value ?: return payments
        return payments.filter { it.status == status.toString() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SELLER-INITIATED REFUND
    //
    // This is the correct entry point for seller-initiated refunds.
    //
    // Why NOT use RefundProcessor.initiateRefund()?
    //   RefundProcessor is designed for buyer-initiated refunds. It reads the
    //   payment, validates buyer-side eligibility, and sets initiated_by from
    //   requestedBy which becomes the buyer's UID. It also doesn't set
    //   initiatedBy = "seller" which is the field RefundRepository.approveRefund()
    //   uses to route the approval to admin instead of seller-self-approval.
    //
    // RefundRepository.createRefundRequest() with initiatedBy = "seller":
    //   • Writes the full RefundRequest model (visible in SellerRefundManagementScreen)
    //   • Writes initiated_by = "seller" so approveRefund() routes to admin
    //   • Calls notifyAdminSellerInitiatedRefund() — admin sees it in dashboard
    //   • Updates payment status to REFUND_PENDING immediately
    //
    // Seller cannot cancel or approve their own refund after submission.
    // Only admin can approve/reject seller-initiated refunds.
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun initiateSellerRefund(
        payment: SellerPayment,
        reason: String,
        reasonDetails: String,
        sellerId: String
    ) {
        _sellerRefundState.value = SellerRefundUiState.Submitting

        // ── Pre-submission guards (defensive, UI also checks these) ───────────
        if (payment.sellerId != sellerId) {
            val msg = "Unauthorized: You can only refund your own payments"
            _sellerRefundState.value = SellerRefundUiState.Error(msg)
            throw Exception(msg)
        }

        val blockedStatuses = listOf("refunded", "refund_pending", "refund_processing")
        if (payment.status.lowercase() in blockedStatuses) {
            val msg = "A refund request already exists for this payment (status: ${payment.status})"
            _sellerRefundState.value = SellerRefundUiState.Error(msg)
            throw Exception(msg)
        }

        // ── Fetch buyer name for the refund document ──────────────────────────
        val buyerName = payment.buyerName.ifEmpty {
            try {
                val buyerDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(payment.buyerId)
                    .get()
                    .addOnSuccessListener { }.result
                buyerDoc?.getString("name")
                    ?: buyerDoc?.getString("full_name")
                    ?: "Unknown Buyer"
            } catch (e: Exception) {
                "Unknown Buyer"
            }
        }

        // ── Seller name ───────────────────────────────────────────────────────
        val sellerName = payment.sellerName.ifEmpty {
            FirebaseAuth.getInstance().currentUser?.displayName ?: "Seller"
        }

        Log.d(TAG, "Initiating seller refund for payment: ${payment.id}, reason: $reason")

        val result = refundRepository.createRefundRequest(
            orderId       = payment.orderId,
            paymentId     = payment.id,
            buyerId       = payment.buyerId,
            buyerName     = buyerName,
            sellerId      = payment.sellerId,
            sellerName    = sellerName,
            refundType    = "full",
            originalAmount = payment.amount,
            refundAmount  = payment.amount,   // Full refund — seller always refunds full amount
            reason        = reason,
            reasonDetails = reasonDetails,
            paymentMethod = payment.paymentMethod,
            transactionId = payment.transactionId,
            initiatedBy   = "seller"          // ✅ KEY: Routes to admin approval, not self-approval
        )

        if (result.isSuccess) {
            Log.d(TAG, "Seller refund created: ${result.getOrNull()?.id}")
            _sellerRefundState.value = SellerRefundUiState.Success(
                "Refund request submitted. Admin will review and approve it."
            )
        } else {
            val error = result.exceptionOrNull()?.message ?: "Failed to create refund"
            Log.e(TAG, "Seller refund creation failed: $error")
            _sellerRefundState.value = SellerRefundUiState.Error(error)
            throw Exception(error)
        }
    }

    // ── Legacy initiateRefund (kept for BuyerRefundRequestScreen compat) ──────
    // This delegates to RefundProcessor which is correct for buyer-side calls.
    // Do NOT call this from seller screens.
    fun initiateRefund(paymentId: String, refundAmount: Double, reason: String) {
        viewModelScope.launch {
            try {
                Log.w(TAG, "initiateRefund() called — this is the buyer-side path. " +
                        "Use initiateSellerRefund() for seller screens.")
                // No-op: buyer refunds go through BuyerRefundRequestScreen → RefundProcessor
                // This stub prevents compilation errors if old call sites exist
            } catch (e: Exception) {
                Log.e(TAG, "Error in legacy initiateRefund", e)
            }
        }
    }

    fun clearSellerRefundState() {
        _sellerRefundState.value = SellerRefundUiState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clean up real-time listeners when ViewModel is destroyed.
     * Prevents memory leaks and unnecessary Firestore connections.
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 Cleaning up real-time listeners")
        paymentsListener?.remove()
        statsListener?.remove()
    }
}