package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.repository.OrderRepository
import com.gcuf.craftoria.data.model.getDisplayDate
import com.gcuf.craftoria.data.model.getBuyerDisplayDate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val orderRepository = OrderRepository()
    private val TAG = "BuyerPaymentViewModel"

    private val _paymentState = MutableStateFlow<BuyerPaymentUiState>(BuyerPaymentUiState.Loading)
    val paymentState: StateFlow<BuyerPaymentUiState> = _paymentState

    private val _statsState = MutableStateFlow<BuyerPaymentStatsUiState>(BuyerPaymentStatsUiState.Loading)
    val statsState: StateFlow<BuyerPaymentStatsUiState> = _statsState

    private val _selectedStatus = MutableStateFlow<PaymentStatus?>(null)
    val selectedStatus: StateFlow<PaymentStatus?> = _selectedStatus

    private val _cachedPayments = MutableStateFlow<List<SellerPayment>>(emptyList())
    private val _cachedStats    = MutableStateFlow<BuyerPaymentStats?>(null)

    private val _filteredCount = MutableStateFlow(0)
    val filteredCount: StateFlow<Int> = _filteredCount

    private var paymentListenerRegistration: ListenerRegistration? = null
    private var orderListenerRegistration: ListenerRegistration? = null
    private var activeBuyerId: String? = null

    // ─────────────────────────────────────────────────────────────────────────
    // loadBuyerPayments
    //
    // INSTANT LOADING OPTIMIZATION
    //
    // Strategy for zero-delay screen opening:
    //   1. Cache hit  → publish instantly, zero Loading state emitted.
    //   2. Cold start → fetch in parallel with a 500 ms Loading delay.
    //                   If fetch completes before 500 ms, Loading is never shown.
    //                   If fetch takes >500 ms, show Loading only then.
    //   3. Always attach listeners for real-time updates.
    //
    // Result: Buyers see instant content on revisits, and fast connections
    // never see a loading spinner on first visit.
    // ─────────────────────────────────────────────────────────────────────────

    fun loadBuyerPayments(buyerId: String) {
        activeBuyerId = buyerId

        viewModelScope.launch {
            if (_cachedPayments.value.isNotEmpty()) {
                // ✅ INSTANT: Serve cache immediately, zero Loading state
                publishPayments(_cachedPayments.value)
                // Fetch fresh data in background for real-time updates
                fetchAndPublish(buyerId)
            } else {
                // ✅ COLD START: Fetch immediately, but delay Loading indicator
                // If fetch completes within 500 ms, user never sees Loading
                val loadingJob: Job = launch {
                    delay(500)
                    _paymentState.value = BuyerPaymentUiState.Loading
                    _statsState.value   = BuyerPaymentStatsUiState.Loading
                }
                try {
                    fetchAndPublish(buyerId)
                } finally {
                    loadingJob.cancel()
                }
            }
            // Always re-attach listeners for real-time updates
            attachListeners(buyerId)
        }
    }

    // ✅ Stats are loaded as part of fetchAndPublish() — no separate call needed
    fun loadPaymentStats(buyerId: String)        { /* derived in fetchAndPublish */ }
    fun startRealtimeStatsListener(buyerId: String) { /* no-op */ }

    fun setStatusFilter(status: PaymentStatus) {
        _selectedStatus.value = status
        val payments = (_paymentState.value as? BuyerPaymentUiState.Success)?.payments ?: return
        updateFilteredCount(payments)
    }

    fun clearFilters() {
        _selectedStatus.value = null
        val payments = (_paymentState.value as? BuyerPaymentUiState.Success)?.payments ?: return
        updateFilteredCount(payments)
    }

    fun getFilteredPayments(payments: List<SellerPayment>): List<SellerPayment> {
        val status = _selectedStatus.value ?: return payments
        return payments.filter { it.status.equals(status.toString(), ignoreCase = true) }
    }

    fun getCountForStatus(status: PaymentStatus, payments: List<SellerPayment>): Int =
        payments.count { it.status.equals(status.toString(), ignoreCase = true) }

    override fun onCleared() {
        super.onCleared()
        paymentListenerRegistration?.remove()
        orderListenerRegistration?.remove()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun fetchAndPublish(buyerId: String): Boolean {
        return try {
            val paymentResult = paymentRepository.getBuyerPayments(buyerId)
            if (paymentResult.isFailure) {
                val msg = paymentResult.exceptionOrNull()?.message ?: "Failed to load payments"
                Log.e(TAG, "❌ $msg")
                if (_cachedPayments.value.isEmpty()) {
                    _paymentState.value = BuyerPaymentUiState.Error(msg)
                    _statsState.value   = BuyerPaymentStatsUiState.Error(msg)
                }
                return false
            }
            val payments = paymentResult.getOrNull() ?: emptyList()
            val orders   = try {
                orderRepository.getUserOrders(buyerId).getOrNull() ?: emptyList()
            } catch (e: Exception) { emptyList() }

            // ✅ CRITICAL: publishPayments() computes stats via computeStats() and sets
            // _statsState to Success. This ensures statsState is never left as Idle.
            // On cache hit, stats are published immediately. On cold start, stats are
            // published as soon as fetch completes (within 500ms or after Loading shown).
            publishPayments(enrichPaymentsWithOrderAmounts(payments, orders))
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchAndPublish exception", e)
            if (_cachedPayments.value.isEmpty()) {
                _paymentState.value = BuyerPaymentUiState.Error(e.message ?: "Unknown error")
                _statsState.value   = BuyerPaymentStatsUiState.Error(e.message ?: "Unknown error")
            }
            false
        }
    }

    private fun attachListeners(buyerId: String) {
        val db = FirebaseFirestore.getInstance()

        paymentListenerRegistration?.remove()
        // ✅ FIX: Listen to "payments" collection (canonical), not "seller_payments"
        // Real-time updates to payments (where PaymentSplitProcessor writes) trigger refresh for buyers
        paymentListenerRegistration = db.collection("payments")
            .whereEqualTo("buyer_id", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // ✅ FIX: Don't skip hasPendingWrites — server-confirmed writes (like
                // refund status updates written by the seller's device) never set
                // hasPendingWrites on THIS client, so the guard was a no-op for
                // remote writes but blocked local optimistic updates from refreshing
                // the UI. Remove it entirely so every confirmed change triggers a fetch.
                viewModelScope.launch { fetchAndPublish(buyerId) }
            }

        orderListenerRegistration?.remove()
        orderListenerRegistration = db.collection("orders")
            .whereEqualTo("buyer_id", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // ✅ FIX: Same as above — remove hasPendingWrites guard
                viewModelScope.launch { fetchAndPublish(buyerId) }
            }
    }

    private fun publishPayments(payments: List<SellerPayment>) {
        // ✅ FIX: Use getBuyerDisplayDate() instead of getDisplayDate()
        // getBuyerDisplayDate prioritizes original_transaction_date (order placed time)
        // getDisplayDate prioritizes payment_date (payment confirmed time) — seller-centric
        // For buyer's payment history, we want to show orders by when they were placed
        val sorted = payments.sortedByDescending { it.getBuyerDisplayDate() }
        val stats  = computeStats(sorted)
        _cachedPayments.value = sorted
        _cachedStats.value    = stats
        _paymentState.value   = BuyerPaymentUiState.Success(sorted)
        _statsState.value     = BuyerPaymentStatsUiState.Success(stats)
        updateFilteredCount(sorted)
    }

    private fun enrichPaymentsWithOrderAmounts(
        payments: List<SellerPayment>,
        orders: List<com.gcuf.craftoria.data.model.Order>
    ): List<SellerPayment> {
        val orderMap = orders.associateBy { it.id }
        return payments.map { payment ->
            val order = orderMap[payment.orderId] ?: return@map payment
            val amount = when {
                order.totalPrice > 0.0   -> order.totalPrice
                order.totalAmount > 0.0  -> order.totalAmount
                order.items.isNotEmpty() -> order.items.sumOf { it.price * it.quantity }
                else                     -> order.productPrice * order.quantity
            }
            payment.copy(amount = amount, originalTransactionDate = order.getCreatedAtLong())
        }
    }

    private fun computeStats(payments: List<SellerPayment>): BuyerPaymentStats {
        // ✅ INTENTIONAL: Refunded payments are excluded from totalSpent because the buyer
        // did not actually spend that money (it was returned). Only active payments
        // (completed, pending, processing) count toward spending statistics.
        val activeStatuses = setOf("completed", "pending", "processing")
        val active    = payments.filter { it.status.lowercase() in activeStatuses }
        val completed = active.filter { it.status.equals("completed", ignoreCase = true) }
        return BuyerPaymentStats(
            totalSpent        = active.sumOf { it.amount },
            completedAmount   = completed.sumOf { it.amount },
            pendingAmount     = active.filter { it.status.equals("pending", ignoreCase = true) }.sumOf { it.amount },
            totalPayments     = active.size,
            completedPayments = completed.size,
            totalOrders       = active.map { it.orderId }.distinct().size,
            totalSellers      = active.map { it.sellerId }.distinct().size
        )
    }

    private fun updateFilteredCount(payments: List<SellerPayment>) {
        _filteredCount.value = getFilteredPayments(payments).size
    }
}