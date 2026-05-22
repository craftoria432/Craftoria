package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Activity
import com.gcuf.craftoria.data.model.DashboardStats
import com.gcuf.craftoria.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository = DashboardRepository()
) : ViewModel() {

    private val _dashboardStats = MutableStateFlow<DashboardStats?>(null)
    val dashboardStats: StateFlow<DashboardStats?> = _dashboardStats.asStateFlow()

    private val _recentActivities = MutableStateFlow<List<Activity>>(emptyList())
    val recentActivities: StateFlow<List<Activity>> = _recentActivities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _notificationCount = MutableStateFlow(3)
    val notificationCount: StateFlow<Int> = _notificationCount.asStateFlow()

    // ✅ Real-time metric updates
    private val _productCount = MutableStateFlow(0)
    val productCount: StateFlow<Int> = _productCount.asStateFlow()

    // ✅ NEW: Total orders count (all statuses) for welcome banner
    private val _totalOrdersCount = MutableStateFlow(0)
    val totalOrdersCount: StateFlow<Int> = _totalOrdersCount.asStateFlow()
    
    // ✅ NEW: Separate count for new/unviewed orders (for badge)
    private val _newOrdersCount = MutableStateFlow(0)
    val newOrdersCount: StateFlow<Int> = _newOrdersCount.asStateFlow()

    // ✅ REMOVED: _pendingOrdersCount was declared but never set
    // It was replaced by _totalOrdersCount (all orders) and _newOrdersCount (pending/new only)

    private val _totalEarnings = MutableStateFlow(0.0)
    val totalEarnings: StateFlow<Double> = _totalEarnings.asStateFlow()

    private val _pendingNegotiations = MutableStateFlow(0)
    val pendingNegotiations: StateFlow<Int> = _pendingNegotiations.asStateFlow()

    private val _unreadMessages = MutableStateFlow(0)
    val unreadMessages: StateFlow<Int> = _unreadMessages.asStateFlow()

    // ✅ Event notifications for UI animations
    private val _newProductAdded = MutableStateFlow(false)
    val newProductAdded: StateFlow<Boolean> = _newProductAdded.asStateFlow()

    private val _newOrderReceived = MutableStateFlow(false)
    val newOrderReceived: StateFlow<Boolean> = _newOrderReceived.asStateFlow()

    private val _paymentReceived = MutableStateFlow(false)
    val paymentReceived: StateFlow<Boolean> = _paymentReceived.asStateFlow()

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    
    // ✅ Single list to track all listeners for cleanup
    private val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    
    private var currentSellerId: String = ""

    companion object {
        private const val TAG = "DashboardViewModel"
    }



    fun loadDashboardData(sellerId: String) {
        // ✅ Guard: if already listening for this seller, skip re-setup
        if (sellerId == currentSellerId && listeners.isNotEmpty()) {
            Log.d(TAG, "Already listening for: $sellerId, skipping re-setup")
            return
        }

        currentSellerId = sellerId
        clearListeners()

        Log.d(TAG, "📥 loadDashboardData for: $sellerId")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Initial one-time fetch so screen isn't blank
                val statsResult = dashboardRepository.getDashboardStats(sellerId)
                if (statsResult.isSuccess) {
                    _dashboardStats.value = statsResult.getOrNull()
                }

                val activitiesResult = dashboardRepository.getRecentActivities(sellerId, 15)
                if (activitiesResult.isSuccess) {
                    _recentActivities.value = activitiesResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "✅ Loaded ${_recentActivities.value.size} activities")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initial load failed", e)
            } finally {
                _isLoading.value = false
            }
        }

        // Start real-time listeners after initial load
        startRealtimeListeners(sellerId)
    }

    // ✅ FIXED: Single stats refresh — no duplicates
    private fun refreshStats(sellerId: String) {
        viewModelScope.launch {
            try {
                val result = dashboardRepository.getDashboardStats(sellerId)
                if (result.isSuccess) {
                    _dashboardStats.value = result.getOrNull()
                    Log.d(TAG, "✅ Stats refreshed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Stats refresh failed", e)
            }
        }
    }

    // ✅ FIXED: Start all real-time listeners
    private fun startRealtimeListeners(sellerId: String) {
        Log.d(TAG, "🎧 Starting all real-time listeners")

        // ── 1. Products listener → updates totalProducts in stats ──────────
        val productsListener = db.collection("products")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Products listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val count = snapshot.documents.size
                    Log.d(TAG, "📦 Products updated: $count")
                    val old = _productCount.value
                    _productCount.value = count
                    if (count > old && old != 0) {
                        triggerEvent(_newProductAdded)
                    }
                    // Refresh full stats
                    refreshStats(sellerId)
                }
            }
        listeners.add(productsListener)

        // ── 2. Orders listener → updates activeOrders in stats ─────────────
        // ✅ FIXED: Count ALL orders for welcome banner, and PENDING/NEW orders for badge
        val ordersListener = db.collection("orders")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Orders listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // ✅ Count ALL orders (all statuses) for professional dashboard display
                    val totalOrders = snapshot.documents.size
                    
                    // ✅ FIXED: Count ONLY PENDING/NEW orders for badge (actionable items)
                    // Badge should show orders that need seller action (accept/reject)
                    // Once accepted/rejected, badge disappears even if order continues
                    val newOrders = snapshot.documents.count {
                        val status = it.getString("status")?.lowercase() ?: "pending"
                        status == "pending" || status == "new"
                    }
                    
                    Log.d(TAG, "📋 Orders snapshot: total=$totalOrders, badge=$newOrders")
                    val oldTotal = _totalOrdersCount.value
                    val oldNew = _newOrdersCount.value
                    
                    _totalOrdersCount.value = totalOrders // Show ALL orders in welcome banner (professional)
                    _newOrdersCount.value = newOrders // Show ONLY pending orders in badge (actionable)
                    
                    if (newOrders > oldNew && oldNew != 0) {
                        triggerEvent(_newOrderReceived)
                    }
                    // Refresh stats on any order change (status updates, new orders)
                    refreshStats(sellerId)
                }
            }
        listeners.add(ordersListener)

        // ── 3. Payments listener → updates totalSales, monthSales ──────────
        // ✅ FIX: Query "payments" collection — canonical collection where all payments are written
        // "seller_payments" is legacy and may be empty, causing PKR 0 on the dashboard
        val paymentsListener = db.collection("payments")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Payments listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // ✅ CRITICAL FIX: Process ALL documents, not just documentChanges
                    // This ensures we get both existing payments (retrospective) and new payments (prospective)
                    // ✅ ALSO FIX: Exclude refunded/refund_pending/refund_processing from earnings
                    val total = snapshot.documents.sumOf { doc ->
                        val status = doc.getString("status")?.lowercase() ?: "completed"
                        // Only count active payments (exclude refund-related statuses)
                        if (status !in listOf("refunded", "refund_pending", "refund_processing", "refund_rejected")) {
                            doc.getDouble("amount") ?: 0.0
                        } else {
                            0.0
                        }
                    }
                    Log.d(TAG, "💰 Payments updated: PKR $total (${snapshot.documents.size} total, active earnings only)")
                    val old = _totalEarnings.value
                    _totalEarnings.value = total
                    if (total > old && old != 0.0) {
                        triggerEvent(_paymentReceived)
                    }
                    refreshStats(sellerId)
                }
            }
        listeners.add(paymentsListener)

        // ── 4. Activities listener — ✅ FIXED: use "timestamp" field ──────────
        val activitiesListener = db.collection("activities")
            .whereEqualTo("seller_id", sellerId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(15)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Activities listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    viewModelScope.launch {
                        try {
                            val activitiesResult = dashboardRepository.getRecentActivities(sellerId, 15)
                            if (activitiesResult.isSuccess) {
                                _recentActivities.value = activitiesResult.getOrNull() ?: emptyList()
                                Log.d(TAG, "✅ Activities updated: ${_recentActivities.value.size}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating activities", e)
                        }
                    }
                }
            }
        listeners.add(activitiesListener)
    }

    private fun triggerEvent(flow: MutableStateFlow<Boolean>) {
        viewModelScope.launch {
            flow.value = true
            kotlinx.coroutines.delay(2000)
            flow.value = false
        }
    }

    private fun clearListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
        Log.d(TAG, "🔴 All listeners cleared")
    }
    fun refreshDashboard(sellerId: String) {
        refreshStats(sellerId)
        viewModelScope.launch {
            try {
                val result = dashboardRepository.getRecentActivities(sellerId, 15)
                if (result.isSuccess) {
                    _recentActivities.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Refresh activities failed", e)
            }
        }
    }
    
    fun markNotificationsRead() {
        _notificationCount.value = 0
    }
    
    override fun onCleared() {
        super.onCleared()
        clearListeners()
    }
}