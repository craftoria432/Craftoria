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

    private var statsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var activitiesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // ✅ Real-time listener for dashboard stats
    fun startRealtimeDashboardListener(sellerId: String) {
        Log.d("DashboardViewModel", "🔴 Starting real-time dashboard listener for: $sellerId")
        
        // Remove old listeners
        statsListenerRegistration?.remove()
        activitiesListenerRegistration?.remove()
        
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        // ✅ Real-time listener for seller_payments (for sales overview)
        statsListenerRegistration = db.collection("seller_payments")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DashboardViewModel", "❌ Error listening to payments", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    Log.d("DashboardViewModel", "🔄 Real-time payment update received")
                    viewModelScope.launch {
                        try {
                            val statsResult = dashboardRepository.getDashboardStats(sellerId)
                            if (statsResult.isSuccess) {
                                _dashboardStats.value = statsResult.getOrNull()
                                Log.d("DashboardViewModel", "✅ Dashboard stats updated in real-time")
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardViewModel", "Error updating dashboard stats", e)
                        }
                    }
                }
            }
        
        // ✅ Real-time listener for activities
        activitiesListenerRegistration = db.collection("activities")
            .whereEqualTo("seller_id", sellerId)
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(15)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DashboardViewModel", "❌ Error listening to activities", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    Log.d("DashboardViewModel", "🔄 Real-time activity update received")
                    viewModelScope.launch {
                        try {
                            val activitiesResult = dashboardRepository.getRecentActivities(sellerId, 15)
                            if (activitiesResult.isSuccess) {
                                _recentActivities.value = activitiesResult.getOrNull() ?: emptyList()
                                Log.d("DashboardViewModel", "✅ Activities updated in real-time: ${_recentActivities.value.size}")
                            }
                        } catch (e: Exception) {
                            Log.e("DashboardViewModel", "Error updating activities", e)
                        }
                    }
                }
            }
    }

    fun loadDashboardData(sellerId: String) {
        Log.d("DashboardViewModel", "📥 loadDashboardData called for: $sellerId")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("DashboardViewModel", "🔄 Loading started...")

                // Load stats
                val statsResult = dashboardRepository.getDashboardStats(sellerId)
                if (statsResult.isSuccess) {
                    _dashboardStats.value = statsResult.getOrNull()
                }

                // ✅ Load only latest 15 activities (not 100)
                val activitiesResult = dashboardRepository.getRecentActivities(sellerId, 15)
                if (activitiesResult.isSuccess) {
                    _recentActivities.value = activitiesResult.getOrNull() ?: emptyList()
                    Log.d("DashboardViewModel", "✅ Loaded ${_recentActivities.value.size} activities")
                }
                
                // ✅ Start real-time listener after initial load
                startRealtimeDashboardListener(sellerId)

            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Failed to load dashboard data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshDashboard(sellerId: String) {
        loadDashboardData(sellerId)
    }
    
    override fun onCleared() {
        super.onCleared()
        statsListenerRegistration?.remove()
        activitiesListenerRegistration?.remove()
        Log.d("DashboardViewModel", "🔴 Real-time listeners removed")
    }

    fun markNotificationsRead() {
        _notificationCount.value = 0
    }
}