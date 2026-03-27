package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BuyerPaymentUiState {
    object Loading : BuyerPaymentUiState()
    data class Success(val payments: List<SellerPayment>) : BuyerPaymentUiState()
    data class Error(val message: String) : BuyerPaymentUiState()
}

sealed class BuyerPaymentStatsUiState {
    object Loading : BuyerPaymentStatsUiState()
    data class Success(val stats: BuyerPaymentStats) : BuyerPaymentStatsUiState()
    data class Error(val message: String) : BuyerPaymentStatsUiState()
}

data class BuyerPaymentStats(
    val totalSpent: Double = 0.0,
    val completedAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val totalPayments: Int = 0,
    val completedPayments: Int = 0,
    val totalOrders: Int = 0,
    val totalSellers: Int = 0
)

class BuyerPaymentViewModel : ViewModel() {
    private val paymentRepository = PaymentRepository()
    private val TAG = "BuyerPaymentViewModel"

    private val _paymentState = MutableStateFlow<BuyerPaymentUiState>(BuyerPaymentUiState.Loading)
    val paymentState: StateFlow<BuyerPaymentUiState> = _paymentState

    private val _statsState = MutableStateFlow<BuyerPaymentStatsUiState>(BuyerPaymentStatsUiState.Loading)
    val statsState: StateFlow<BuyerPaymentStatsUiState> = _statsState

    private val _selectedStatus = MutableStateFlow<PaymentStatus?>(null)
    val selectedStatus: StateFlow<PaymentStatus?> = _selectedStatus

    // ✅ Filter count tracking for UI feedback
    private val _filteredCount = MutableStateFlow(0)
    val filteredCount: StateFlow<Int> = _filteredCount

    private var paymentListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var statsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    /**
     * ✅ Start real-time listener for buyer payments
     */
    fun startRealtimePaymentListener(buyerId: String) {
        Log.d(TAG, "🔴 Starting real-time payment listener for buyer: $buyerId")
        
        // Remove old listener
        paymentListenerRegistration?.remove()
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        paymentListenerRegistration = db.collection("seller_payments")
            .whereEqualTo("buyer_id", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to payments", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                    Log.d(TAG, "🔄 Real-time payment update received: ${snapshot.documentChanges.size} changes")
                    viewModelScope.launch {
                        try {
                            val result = paymentRepository.getBuyerPayments(buyerId)
                            if (result.isSuccess) {
                                val payments = result.getOrNull() ?: emptyList()
                                _paymentState.value = BuyerPaymentUiState.Success(payments)
                                updateFilteredCount(payments)
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
     * ✅ Start real-time listener for buyer payment stats
     */
    fun startRealtimeStatsListener(buyerId: String) {
        Log.d(TAG, "🔴 Starting real-time stats listener for buyer: $buyerId")
        
        // Remove old listener
        statsListenerRegistration?.remove()
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        statsListenerRegistration = db.collection("seller_payments")
            .whereEqualTo("buyer_id", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to stats", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.documentChanges.isNotEmpty()) {
                    Log.d(TAG, "🔄 Real-time stats update received")
                    viewModelScope.launch {
                        try {
                            val result = paymentRepository.getBuyerPaymentStats(buyerId)
                            if (result.isSuccess) {
                                val stats = result.getOrNull() ?: return@launch
                                _statsState.value = BuyerPaymentStatsUiState.Success(stats)
                                Log.d(TAG, "✅ Stats updated in real-time")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating stats", e)
                        }
                    }
                }
            }
    }

    fun loadBuyerPayments(buyerId: String) {
        viewModelScope.launch {
            try {
                _paymentState.value = BuyerPaymentUiState.Loading
                val result = paymentRepository.getBuyerPayments(buyerId)
                result.onSuccess { payments ->
                    Log.d(TAG, "✅ Loaded ${payments.size} payments for buyer: $buyerId")
                    _paymentState.value = BuyerPaymentUiState.Success(payments)
                    updateFilteredCount(payments)
                    
                    // ✅ Start real-time listener after initial load
                    startRealtimePaymentListener(buyerId)
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load payments", error)
                    _paymentState.value = BuyerPaymentUiState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception loading payments", e)
                _paymentState.value = BuyerPaymentUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadPaymentStats(buyerId: String) {
        viewModelScope.launch {
            try {
                _statsState.value = BuyerPaymentStatsUiState.Loading
                val result = paymentRepository.getBuyerPaymentStats(buyerId)
                result.onSuccess { stats ->
                    Log.d(TAG, "✅ Loaded payment stats for buyer: $buyerId")
                    _statsState.value = BuyerPaymentStatsUiState.Success(stats)
                    
                    // ✅ Start real-time listener after initial load
                    startRealtimeStatsListener(buyerId)
                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load stats", error)
                    _statsState.value = BuyerPaymentStatsUiState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception loading stats", e)
                _statsState.value = BuyerPaymentStatsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setStatusFilter(status: PaymentStatus) {
        _selectedStatus.value = status
        Log.d(TAG, "✅ Filter applied: ${status.getDisplayName()}")
        
        // Update filtered count
        if (_paymentState.value is BuyerPaymentUiState.Success) {
            val payments = (_paymentState.value as BuyerPaymentUiState.Success).payments
            updateFilteredCount(payments)
        }
    }

    fun clearFilters() {
        _selectedStatus.value = null
        Log.d(TAG, "✅ Filters cleared")
        
        // Update filtered count
        if (_paymentState.value is BuyerPaymentUiState.Success) {
            val payments = (_paymentState.value as BuyerPaymentUiState.Success).payments
            updateFilteredCount(payments)
        }
    }

    fun getFilteredPayments(payments: List<SellerPayment>): List<SellerPayment> {
        val status = _selectedStatus.value ?: return payments
        val filtered = payments.filter { it.status.equals(status.toString(), ignoreCase = true) }
        Log.d(TAG, "📊 Filtered: ${filtered.size} of ${payments.size} payments")
        return filtered
    }

    // ✅ Helper function to update filtered count
    private fun updateFilteredCount(payments: List<SellerPayment>) {
        val filtered = getFilteredPayments(payments)
        _filteredCount.value = filtered.size
    }

    // ✅ Get count for specific status
    fun getCountForStatus(status: PaymentStatus, payments: List<SellerPayment>): Int {
        return payments.count { it.status.equals(status.toString(), ignoreCase = true) }
    }

    override fun onCleared() {
        super.onCleared()
        paymentListenerRegistration?.remove()
        statsListenerRegistration?.remove()
        Log.d(TAG, "🔴 Real-time listeners removed")
    }
}
