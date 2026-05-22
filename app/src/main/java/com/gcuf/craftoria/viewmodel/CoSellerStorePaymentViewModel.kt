package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.repository.CoSellerStorePaymentRepository
import com.gcuf.craftoria.data.repository.MemberEarningsBreakdown
import com.gcuf.craftoria.data.repository.StoreRevenueSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

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

enum class CoSellerPaymentDateRange(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days")
}

class CoSellerStorePaymentViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val paymentRepository = CoSellerStorePaymentRepository(db)

    companion object {
        private const val TAG = "CoSellerStorePaymentVM"
    }

    private val _paymentState = MutableStateFlow<CoSellerPaymentUiState>(CoSellerPaymentUiState.Loading)
    val paymentState: StateFlow<CoSellerPaymentUiState> = _paymentState

    private val _paymentDetailState = MutableStateFlow<PaymentDetailUiState>(PaymentDetailUiState.Loading)
    val paymentDetailState: StateFlow<PaymentDetailUiState> = _paymentDetailState

    private val _memberEarningsState = MutableStateFlow<MemberEarningsUiState>(MemberEarningsUiState.Loading)
    val memberEarningsState: StateFlow<MemberEarningsUiState> = _memberEarningsState

    private val _storeRevenueState = MutableStateFlow<StoreRevenueUiState>(StoreRevenueUiState.Loading)
    val storeRevenueState: StateFlow<StoreRevenueUiState> = _storeRevenueState

    private val _selectedStatus = MutableStateFlow("all")
    val selectedStatus: StateFlow<String> = _selectedStatus

    private val _selectedDateRange = MutableStateFlow(CoSellerPaymentDateRange.ALL_TIME)
    val selectedDateRange: StateFlow<CoSellerPaymentDateRange> = _selectedDateRange

    private var paymentListenerRegistration: ListenerRegistration? = null
    private var activeStoreId: String? = null
    private var allStorePayments: List<SellerPayment> = emptyList()

    private fun publishDerivedState(storeId: String) {
        val rangeFilteredPayments = filterPaymentsByDateRange(
            payments = allStorePayments,
            dateRange = _selectedDateRange.value
        )

        _paymentState.value = CoSellerPaymentUiState.Success(rangeFilteredPayments)
        _storeRevenueState.value = StoreRevenueUiState.Success(
            buildRevenueSummary(storeId, rangeFilteredPayments)
        )
    }

    private fun filterPaymentsByDateRange(
        payments: List<SellerPayment>,
        dateRange: CoSellerPaymentDateRange
    ): List<SellerPayment> {
        if (dateRange == CoSellerPaymentDateRange.ALL_TIME) return payments

        val now = System.currentTimeMillis()
        val startDate = when (dateRange) {
            CoSellerPaymentDateRange.ALL_TIME -> Long.MIN_VALUE
            CoSellerPaymentDateRange.THIS_MONTH -> {
                Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            CoSellerPaymentDateRange.LAST_30_DAYS -> now - (30L * 24 * 60 * 60 * 1000)
        }

        return payments.filter { payment ->
            payment.getCreatedAtLong() in startDate..now
        }
    }

    private fun buildRevenueSummary(
        storeId: String,
        payments: List<SellerPayment>
    ): StoreRevenueSummary {
        var totalRevenue = 0.0
        var completedRevenue = 0.0
        var pendingRevenue = 0.0

        payments.forEach { payment ->
            totalRevenue += payment.amount
            when (payment.status.lowercase()) {
                "completed" -> completedRevenue += payment.amount
                "pending" -> pendingRevenue += payment.amount
            }
        }

        return StoreRevenueSummary(
            storeId = storeId,
            totalRevenue = totalRevenue,
            completedRevenue = completedRevenue,
            pendingRevenue = pendingRevenue,
            orderCount = payments.size,
            period = _selectedDateRange.value.displayName
        )
    }

    fun startRealtimePaymentListener(storeId: String) {
        Log.d(TAG, "Starting real-time payment listener for store: $storeId")

        paymentListenerRegistration?.remove()

        val currentUserId = auth.currentUser?.uid ?: run {
            _paymentState.value = CoSellerPaymentUiState.Error("User not authenticated")
            _storeRevenueState.value = StoreRevenueUiState.Error("User not authenticated")
            return
        }

        paymentListenerRegistration = paymentRepository.listenToStorePayments(
            storeId = storeId,
            currentUserId = currentUserId,
            onUpdate = { payments ->
                Log.d(TAG, "Real-time payment update: ${payments.size} payments for store $storeId")
                allStorePayments = payments
                publishDerivedState(storeId)
            },
            onError = { error ->
                Log.e(TAG, "Error listening to co-seller payments", error)
                val message = error.message ?: "Unknown error"
                _paymentState.value = CoSellerPaymentUiState.Error(message)
                _storeRevenueState.value = StoreRevenueUiState.Error(message)
            }
        )
    }

    fun loadStorePayments(storeId: String) {
        activeStoreId = storeId
        _paymentState.value = CoSellerPaymentUiState.Loading
        _storeRevenueState.value = StoreRevenueUiState.Loading
        startRealtimePaymentListener(storeId)

        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

                val result = paymentRepository.loadStorePayments(
                    storeId = storeId,
                    currentUserId = currentUserId,
                    storeMemberIds = emptyList(),
                    storeOwnerId = ""
                )

                if (result.isSuccess) {
                    val payments = result.getOrNull() ?: emptyList()
                    allStorePayments = payments
                    publishDerivedState(storeId)
                    Log.d(TAG, "Loaded ${payments.size} payments for store: $storeId")
                } else {
                    val message = result.exceptionOrNull()?.message ?: "Unknown error"
                    _paymentState.value = CoSellerPaymentUiState.Error(message)
                    _storeRevenueState.value = StoreRevenueUiState.Error(message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading store payments", e)
                val message = e.message ?: "Unknown error"
                _paymentState.value = CoSellerPaymentUiState.Error(message)
                _storeRevenueState.value = StoreRevenueUiState.Error(message)
            }
        }
    }

    fun loadPaymentDetail(paymentId: String) {
        viewModelScope.launch {
            try {
                _paymentDetailState.value = PaymentDetailUiState.Loading

                val result = paymentRepository.getPaymentWithSplits(paymentId)

                if (result.isSuccess) {
                    val payment = result.getOrNull() ?: throw Exception("Payment not found")
                    _paymentDetailState.value = PaymentDetailUiState.Success(payment)
                    Log.d(TAG, "Loaded payment detail: $paymentId")
                } else {
                    _paymentDetailState.value =
                        PaymentDetailUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading payment detail", e)
                _paymentDetailState.value = PaymentDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

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
                    Log.d(TAG, "Loaded member earnings: $memberId")
                } else {
                    _memberEarningsState.value =
                        MemberEarningsUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading member earnings", e)
                _memberEarningsState.value = MemberEarningsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadStoreRevenue(
        storeId: String,
        startDate: Long,
        endDate: Long
    ) {
        activeStoreId = storeId
        val inferredRange = when {
            startDate <= 0L -> CoSellerPaymentDateRange.ALL_TIME
            endDate - startDate in (27L * 24 * 60 * 60 * 1000)..(31L * 24 * 60 * 60 * 1000) ->
                CoSellerPaymentDateRange.LAST_30_DAYS
            else -> CoSellerPaymentDateRange.THIS_MONTH
        }
        setDateRange(inferredRange)
    }

    fun filterByStatus(status: String) {
        _selectedStatus.value = status
    }

    fun setDateRange(dateRange: CoSellerPaymentDateRange) {
        if (_selectedDateRange.value == dateRange) return
        _selectedDateRange.value = dateRange
        activeStoreId?.let { storeId ->
            if (_paymentState.value is CoSellerPaymentUiState.Success) {
                publishDerivedState(storeId)
            }
        }
    }

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

    fun getStatusColor(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "#FFA500"
            "processing" -> "#4169E1"
            "completed" -> "#28A745"
            "failed" -> "#DC3545"
            "refunded" -> "#6C757D"
            else -> "#999999"
        }
    }

    fun getStatusDisplayName(status: String): String {
        return when (status.lowercase()) {
            "pending" -> "Pending"
            "processing" -> "Processing"
            "completed" -> "Completed"
            "failed" -> "Failed"
            "refunded" -> "Refunded"
            "refund_pending" -> "Refund Pending"
            "refund_processing" -> "Refund Processing"
            "refund_rejected" -> "Refund Rejected"
            else -> status
        }
    }

    override fun onCleared() {
        super.onCleared()
        paymentListenerRegistration?.remove()
        Log.d(TAG, "Real-time listeners removed")
    }
}
