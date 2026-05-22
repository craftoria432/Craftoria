package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.RefundRequest
import com.gcuf.craftoria.data.repository.RefundRepository
import com.gcuf.craftoria.services.RefundNotificationService
import com.gcuf.craftoria.utils.RefundProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RefundViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val refundRepository = RefundRepository(firestore)
    private val refundProcessor = RefundProcessor(firestore)
    private val notificationService = RefundNotificationService(firestore)

    companion object {
        private const val TAG = "RefundViewModel"
    }

    private val _refundState = MutableStateFlow<RefundUiState>(RefundUiState.Idle)
    val refundState: StateFlow<RefundUiState> = _refundState

    private val _refundList = MutableStateFlow<List<RefundRequest>>(emptyList())
    val refundList: StateFlow<List<RefundRequest>> = _refundList

    private val _currentRefund = MutableStateFlow<RefundRequest?>(null)
    val currentRefund: StateFlow<RefundRequest?> = _currentRefund

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ==================== INITIATE REFUND ====================
    // FIX 2: RefundProcessor.initiateRefund only accepts:
    //   (paymentId, refundAmount, reason, description, requestedBy) → Result<String>
    // So we use RefundRepository.createRefundRequest for the full-parameter version
    fun initiateRefund(
        orderId: String,
        paymentId: String,
        buyerId: String,
        buyerName: String,
        sellerId: String,
        sellerName: String,
        refundType: String,
        originalAmount: Double,
        refundAmount: Double,
        reason: String,
        reasonDetails: String,
        paymentMethod: String,
        transactionId: String,
        initiatedBy: String
    ) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundRepository.createRefundRequest(
                    orderId, paymentId, buyerId, buyerName, sellerId, sellerName,
                    refundType, originalAmount, refundAmount, reason, reasonDetails,
                    paymentMethod, transactionId, initiatedBy
                )

                if (result.isSuccess) {
                    // FIX 3: createRefundRequest returns Result<RefundRequest>, not Result<String>
                    val refund = result.getOrNull()!!
                    _currentRefund.value = refund
                    _refundState.value = RefundUiState.RefundInitiated(refund)
                    _errorMessage.value = null
                    
                    // Trigger notification
                    notificationService.notifyRefundRequested(refund)
                    
                    Log.d(TAG, "Refund initiated successfully: ${refund.id}")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _errorMessage.value = error
                    _refundState.value = RefundUiState.Error(error)
                    Log.e(TAG, "Failed to initiate refund: $error")
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                _errorMessage.value = error
                _refundState.value = RefundUiState.Error(error)
                Log.e(TAG, "Exception initiating refund", e)
            }
        }
    }

    // ==================== APPROVE REFUND ====================
    fun approveRefund(
        refundId: String,
        approvedBy: String,
        approverName: String,
        approvalNotes: String = ""
    ) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundRepository.approveRefund(
                    refundId, approvedBy, approverName, approvalNotes
                )

                if (result.isSuccess) {
                    // FIX 4: approveRefund returns Result<RefundRequest>
                    val refund = result.getOrNull()!!
                    _currentRefund.value = refund
                    _refundState.value = RefundUiState.RefundApproved(refund)
                    _errorMessage.value = null
                    Log.d(TAG, "Refund approved: $refundId")
                    
                    // ✅ NOTE: completeRefund() is already called by RefundRepository.approveRefund()
                    // for buyer-initiated refunds (Case 3) and seller-initiated refunds approved by admin (Case 1).
                    // No need to call it again here — it would cause duplicate Firestore writes.
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _errorMessage.value = error
                    _refundState.value = RefundUiState.Error(error)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                _errorMessage.value = error
                _refundState.value = RefundUiState.Error(error)
                Log.e(TAG, "Exception approving refund", e)
            }
        }
    }

    // ==================== REJECT REFUND ====================
    fun rejectRefund(
        refundId: String,
        rejectedBy: String,
        rejectorName: String,
        rejectionReason: String
    ) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundRepository.rejectRefund(
                    refundId, rejectedBy, rejectorName, rejectionReason
                )

                if (result.isSuccess) {
                    val refund = result.getOrNull()!!
                    _currentRefund.value = refund
                    _refundState.value = RefundUiState.RefundRejected(refund)
                    _errorMessage.value = null
                    Log.d(TAG, "Refund rejected: $refundId")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _errorMessage.value = error
                    _refundState.value = RefundUiState.Error(error)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                _errorMessage.value = error
                _refundState.value = RefundUiState.Error(error)
                Log.e(TAG, "Exception rejecting refund", e)
            }
        }
    }

    // ==================== PROCESS REFUND ====================
    // FIX 5: RefundProcessor has no processApprovedRefund() method.
    // Use RefundProcessor.processRefund(refundId, transactionId, actorId) instead.
    fun processRefund(refundId: String, paymentGateway: String = "system") {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundProcessor.processRefund(
                    refundId = refundId,
                    actorId = paymentGateway
                )

                if (result.isSuccess) {
                    // processRefund returns Result<Unit>, so fetch the updated refund separately
                    val refundResult = refundRepository.getRefundById(refundId)
                    val refund = refundResult.getOrNull() ?: RefundRequest()
                    _currentRefund.value = refund
                    _refundState.value = RefundUiState.RefundProcessed(refund)
                    _errorMessage.value = null
                    Log.d(TAG, "Refund processed: $refundId")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _errorMessage.value = error
                    _refundState.value = RefundUiState.Error(error)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                _errorMessage.value = error
                _refundState.value = RefundUiState.Error(error)
                Log.e(TAG, "Exception processing refund", e)
            }
        }
    }

    // ==================== GET REFUND ====================
    fun getRefund(refundId: String) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundRepository.getRefundById(refundId)

                if (result.isSuccess) {
                    val refund = result.getOrNull()!!
                    _currentRefund.value = refund
                    _refundState.value = RefundUiState.RefundLoaded(refund)
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
                Log.e(TAG, "Exception getting refund", e)
            }
        }
    }

    // ==================== GET REFUNDS BY BUYER ====================
    fun getRefundsByBuyer(buyerId: String) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundRepository.getRefundsByBuyerId(buyerId)

                if (result.isSuccess) {
                    val refunds = result.getOrNull() ?: emptyList()
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

    // ==================== GET REFUNDS BY SELLER ====================
    fun getRefundsBySeller(sellerId: String) {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundRepository.getRefundsBySellerId(sellerId)

                if (result.isSuccess) {
                    val refunds = result.getOrNull() ?: emptyList()
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
                Log.e(TAG, "Exception getting seller refunds", e)
            }
        }
    }

    // ==================== GET PENDING REFUNDS ====================
    fun getPendingRefunds() {
        viewModelScope.launch {
            try {
                _refundState.value = RefundUiState.Loading

                val result = refundRepository.getPendingRefunds()

                if (result.isSuccess) {
                    val refunds = result.getOrNull() ?: emptyList()
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
                Log.e(TAG, "Exception getting pending refunds", e)
            }
        }
    }

    // ==================== CLEAR ERROR ====================
    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== CLEAR STATE ====================
    fun clearState() {
        _refundState.value = RefundUiState.Idle
        _currentRefund.value = null
    }

    // ==================== GET REFUND BY ID (FLOW) ====================
    fun getRefundByIdFlow(refundId: String): kotlinx.coroutines.flow.Flow<RefundRequest?> {
        return kotlinx.coroutines.flow.flow {
            val result = refundRepository.getRefundById(refundId)
            emit(result.getOrNull())
        }
    }

    // ==================== GET ORDER FOR REFUND ====================
    fun getOrderForRefund(orderId: String): kotlinx.coroutines.flow.Flow<com.gcuf.craftoria.data.model.Order?> {
        if (orderId.isBlank()) return kotlinx.coroutines.flow.flowOf(null)
        return kotlinx.coroutines.flow.flow {
            try {
                val doc = firestore.collection("orders")
                    .document(orderId)
                    .get()
                    .await()
                
                if (doc.exists()) {
                    val order = doc.toObject(com.gcuf.craftoria.data.model.Order::class.java)
                    emit(order)
                } else {
                    emit(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching order for refund", e)
                emit(null)
            }
        }
    }
}

sealed class RefundUiState {
    object Idle : RefundUiState()
    object Loading : RefundUiState()
    data class RefundInitiated(val refund: RefundRequest) : RefundUiState()
    data class RefundApproved(val refund: RefundRequest) : RefundUiState()
    data class RefundRejected(val refund: RefundRequest) : RefundUiState()
    data class RefundProcessed(val refund: RefundRequest) : RefundUiState()
    data class RefundLoaded(val refund: RefundRequest) : RefundUiState()
    data class RefundsLoaded(val refunds: List<RefundRequest>) : RefundUiState()
    data class Error(val message: String) : RefundUiState()
}