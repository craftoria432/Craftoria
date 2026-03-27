package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.CoSellerStorePaymentRepository
import com.gcuf.craftoria.data.repository.CoSellerStoreRepository
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.repository.MemberEarningsBreakdown
import com.gcuf.craftoria.data.repository.StoreRevenueSummary
import com.gcuf.craftoria.data.repository.MemberPaymentRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CoSellerPaymentUiState {
    object Loading : CoSellerPaymentUiState()
    data class Success(val payments: List<SellerPayment>) : CoSellerPaymentUiState()
    data class Error(val message: String) : CoSellerPaymentUiState()
}

sealed class PaymentDetailUiState {
    object Loading : PaymentDetailUiState()
    data class Success(val payment: SellerPayment) : PaymentDetailUiState()
    data class Error(val message: String) : PaymentDetailUiState()
}

sealed class MemberEarningsUiState {
    object Loading : MemberEarningsUiState()
    data class Success(val breakdown: MemberEarningsBreakdown) : MemberEarningsUiState()
    data class Error(val message: String) : MemberEarningsUiState()
}

sealed class StoreRevenueUiState {
    object Loading : StoreRevenueUiState()
    data class Success(val summary: StoreRevenueSummary) : StoreRevenueUiState()
    data class Error(val message: String) : StoreRevenueUiState()
}

class CoSellerStorePaymentViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val paymentRepository = CoSellerStorePaymentRepository(db)
    private val storeRepository = CoSellerStoreRepository()

    companion object {
        private const val TAG = "CoSellerStorePaymentVM"
    }

    // ✅ Payment list state
    private val _paymentState = MutableStateFlow<CoSellerPaymentUiState>(CoSellerPaymentUiState.Loading)
    val paymentState: StateFlow<CoSellerPaymentUiState> = _paymentState

    // ✅ Payment detail state
    private val _paymentDetailState = MutableStateFlow<PaymentDetailUiState>(PaymentDetailUiState.Loading)
    val paymentDetailState: StateFlow<PaymentDetailUiState> = _paymentDetailState

    // ✅ Member earnings state
    private val _memberEarningsState = MutableStateFlow<MemberEarningsUiState>(MemberEarningsUiState.Loading)
    val memberEarningsState: StateFlow<MemberEarningsUiState> = _memberEarningsState

    // ✅ Store revenue state
    private val _storeRevenueState = MutableStateFlow<StoreRevenueUiState>(StoreRevenueUiState.Loading)
    val storeRevenueState: StateFlow<StoreRevenueUiState> = _storeRevenueState

    // ✅ Selected status filter
    private val _selectedStatus = MutableStateFlow<String>("all")
    val selectedStatus: StateFlow<String> = _selectedStatus

    private var paymentListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var revenueListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    /**
     * ✅ Start real-time listener for store payments
     */
    fun startRealtimePaymentListener(storeId: String) {
        Log.d(TAG, "🔴 Starting real-time payment listener for store: $storeId")
        
        // Remove old listener
        paymentListenerRegistration?.remove()
        
        paymentListenerRegistration = db.collection("seller_payments")
            .whereEqualTo("co_seller_store_id", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to payments", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                    Log.d(TAG, "🔄 Real-time payment update received: ${snapshot.documentChanges.size} changes")
                    viewModelScope.launch {
                        try {
                            val currentUserId = auth.currentUser?.uid ?: return@launch
                            val result = paymentRepository.loadStorePayments(
                                storeId = storeId,
                                currentUserId = currentUserId,
                                storeMemberIds = emptyList(),
                                storeOwnerId = ""
                            )
                            
                            if (result.isSuccess) {
                                val payments = result.getOrNull() ?: emptyList()
                                _paymentState.value = CoSellerPaymentUiState.Success(payments)
                                Log.d(TAG, "✅ Payments updated in real-time: ${payments.size}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating payments", e)
                        }
                    }
                }
            }
    }

    /**
     * ✅ Start real-time listener for store revenue
     */
    fun startRealtimeRevenueListener(storeId: String, startDate: Long, endDate: Long) {
        Log.d(TAG, "🔴 Starting real-time revenue listener for store: $storeId")
        
        // Remove old listener
        revenueListenerRegistration?.remove()
        
        revenueListenerRegistration = db.collection("seller_payments")
            .whereEqualTo("co_seller_store_id", storeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to revenue", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                    Log.d(TAG, "🔄 Real-time revenue update received")
                    viewModelScope.launch {
                        try {
                            val result = paymentRepository.getStoreRevenueSummary(
                                storeId = storeId,
                                startDate = startDate,
                                endDate = endDate
                            )
                            
                            if (result.isSuccess) {
                                val summary = result.getOrNull() ?: throw Exception("No data")
                                _storeRevenueState.value = StoreRevenueUiState.Success(summary)
                                Log.d(TAG, "✅ Revenue updated in real-time")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating revenue", e)
                        }
                    }
                }
            }
    }

    /**
     * Load all payments for a co-seller store
     * ✅ SECURITY: Validates user is store owner or member
     */
    fun loadStorePayments(storeId: String) {
        viewModelScope.launch {
            try {
                _paymentState.value = CoSellerPaymentUiState.Loading

                val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                // ✅ Load payments with access control
                val result = paymentRepository.loadStorePayments(
                    storeId = storeId,
                    currentUserId = currentUserId,
                    storeMemberIds = emptyList(),  // Will be validated in repository
                    storeOwnerId = ""  // Will be validated in repository
                )

                if (result.isSuccess) {
                    val payments = result.getOrNull() ?: emptyList()
                    _paymentState.value = CoSellerPaymentUiState.Success(payments)
                    Log.d(TAG, "✅ Loaded ${payments.size} payments for store: $storeId")
                    
                    // ✅ Start real-time listener after initial load
                    startRealtimePaymentListener(storeId)
                } else {
                    _paymentState.value = CoSellerPaymentUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading store payments", e)
                _paymentState.value = CoSellerPaymentUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load payment details with split breakdown
     */
    fun loadPaymentDetail(paymentId: String) {
        viewModelScope.launch {
            try {
                _paymentDetailState.value = PaymentDetailUiState.Loading

                val result = paymentRepository.getPaymentWithSplits(paymentId)

                if (result.isSuccess) {
                    val payment = result.getOrNull() ?: throw Exception("Payment not found")
                    _paymentDetailState.value = PaymentDetailUiState.Success(payment)
                    Log.d(TAG, "✅ Loaded payment detail: $paymentId")
                } else {
                    _paymentDetailState.value = PaymentDetailUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading payment detail", e)
                _paymentDetailState.value = PaymentDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load member earnings breakdown for a specific period
     */
    fun loadMemberEarnings(
        storeId: String,
        memberId: String,
        startDate: Long,
        endDate: Long
    ) {
        viewModelScope.launch {
            try {
                _memberEarningsState.value = MemberEarningsUiState.Loading

                val result = paymentRepository.getMemberEarningsBreakdown(
                    storeId = storeId,
                    memberId = memberId,
                    startDate = startDate,
                    endDate = endDate
                )

                if (result.isSuccess) {
                    val breakdown = result.getOrNull() ?: throw Exception("No data")
                    _memberEarningsState.value = MemberEarningsUiState.Success(breakdown)
                    Log.d(TAG, "✅ Loaded member earnings: $memberId")
                } else {
                    _memberEarningsState.value = MemberEarningsUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading member earnings", e)
                _memberEarningsState.value = MemberEarningsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load store revenue summary for a specific period
     */
    fun loadStoreRevenue(
        storeId: String,
        startDate: Long,
        endDate: Long
    ) {
        viewModelScope.launch {
            try {
                _storeRevenueState.value = StoreRevenueUiState.Loading

                val result = paymentRepository.getStoreRevenueSummary(
                    storeId = storeId,
                    startDate = startDate,
                    endDate = endDate
                )

                if (result.isSuccess) {
                    val summary = result.getOrNull() ?: throw Exception("No data")
                    _storeRevenueState.value = StoreRevenueUiState.Success(summary)
                    Log.d(TAG, "✅ Loaded store revenue: $storeId")
                    
                    // ✅ Start real-time listener after initial load
                    startRealtimeRevenueListener(storeId, startDate, endDate)
                } else {
                    _storeRevenueState.value = StoreRevenueUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading store revenue", e)
                _storeRevenueState.value = StoreRevenueUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Filter payments by status
     */
    fun filterByStatus(status: String) {
        _selectedStatus.value = status
    }

    /**
     * Get filtered payments based on selected status
     */
    fun getFilteredPayments(): List<SellerPayment> {
        val currentState = _paymentState.value
        if (currentState !is CoSellerPaymentUiState.Success) return emptyList()

        val status = _selectedStatus.value
        return if (status == "all") {
            currentState.payments
        } else {
            currentState.payments.filter { it.status.lowercase() == status.lowercase() }
        }
    }

    /**
     * Get payment status color for UI
     */
    fun getStatusColor(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "#FFA500"      // Orange
            "processing" -> "#4169E1"   // Royal Blue
            "completed" -> "#28A745"    // Green
            "failed" -> "#DC3545"       // Red
            "refunded" -> "#6C757D"     // Gray
            else -> "#999999"           // Default gray
        }
    }

    /**
     * Get payment status display name
     */
    fun getStatusDisplayName(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "Pending"
            "processing" -> "Processing"
            "completed" -> "Completed"
            "failed" -> "Failed"
            "refunded" -> "Refunded"
            else -> status
        }
    }

    override fun onCleared() {
        super.onCleared()
        paymentListenerRegistration?.remove()
        revenueListenerRegistration?.remove()
        Log.d(TAG, "🔴 Real-time listeners removed")
    }
}
