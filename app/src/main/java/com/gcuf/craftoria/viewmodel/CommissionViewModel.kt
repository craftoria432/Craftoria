package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.data.repository.CommissionRepository
import com.gcuf.craftoria.data.repository.CommissionStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ✅ PRODUCTION-READY: Commission management ViewModel
 * Handles commission data and operations for web admin dashboard
 * Note: Commission management is admin-only and handled through web dashboard
 */
class CommissionViewModel : ViewModel() {

    companion object {
        private const val TAG = "CommissionViewModel"
    }

    private val db = FirebaseFirestore.getInstance()
    private val commissionRepository = CommissionRepository(db)

    // ✅ Commission Settings State
    private val _commissionSettings = MutableStateFlow<CommissionSettings?>(null)
    val commissionSettings: StateFlow<CommissionSettings?> = _commissionSettings.asStateFlow()

    // ✅ Admin Earnings State
    private val _adminEarnings = MutableStateFlow<AdminEarnings?>(null)
    val adminEarnings: StateFlow<AdminEarnings?> = _adminEarnings.asStateFlow()

    // ✅ Commission List State
    private val _commissions = MutableStateFlow<List<AdminCommission>>(emptyList())
    val commissions: StateFlow<List<AdminCommission>> = _commissions.asStateFlow()

    // ✅ Pending Commissions State
    private val _pendingCommissions = MutableStateFlow<List<AdminCommission>>(emptyList())
    val pendingCommissions: StateFlow<List<AdminCommission>> = _pendingCommissions.asStateFlow()

    // ✅ Commission Stats State
    private val _commissionStats = MutableStateFlow<CommissionStats?>(null)
    val commissionStats: StateFlow<CommissionStats?> = _commissionStats.asStateFlow()

    // ✅ Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ✅ Error State
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * ✅ Load commission settings
     */
    fun loadCommissionSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepository.getCommissionSettings()
                if (result.isSuccess) {
                    _commissionSettings.value = result.getOrNull()
                    Log.d(TAG, "✅ Commission settings loaded")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load settings"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading commission settings", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Update commission settings (admin only)
     */
    fun updateCommissionSettings(settings: CommissionSettings) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepository.updateCommissionSettings(settings)
                if (result.isSuccess) {
                    _commissionSettings.value = settings
                    Log.d(TAG, "✅ Commission settings updated")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to update settings"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating commission settings", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Load admin earnings
     */
    fun loadAdminEarnings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepository.getAdminEarnings()
                if (result.isSuccess) {
                    _adminEarnings.value = result.getOrNull()
                    Log.d(TAG, "✅ Admin earnings loaded")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load earnings"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading admin earnings", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Load pending commissions
     */
    fun loadPendingCommissions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepository.getPendingCommissions()
                if (result.isSuccess) {
                    _pendingCommissions.value = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "✅ Pending commissions loaded: ${_pendingCommissions.value.size}")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load pending"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading pending commissions", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Load commissions by seller
     */
    fun loadCommissionsBySeller(sellerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepository.getCommissionsBySeller(sellerId)
                if (result.isSuccess) {
                    _commissions.value = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "✅ Seller commissions loaded: ${_commissions.value.size}")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load commissions"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading seller commissions", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Load commission statistics
     */
    fun loadCommissionStats(startDate: Long, endDate: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepository.getCommissionStats(startDate, endDate)
                if (result.isSuccess) {
                    _commissionStats.value = result.getOrNull()
                    Log.d(TAG, "✅ Commission stats loaded")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load stats"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading commission stats", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✅ Mark commission as paid
     */
    fun markCommissionAsPaid(commissionId: String) {
        viewModelScope.launch {
            try {
                val result = commissionRepository.updateCommissionStatus(
                    commissionId,
                    CommissionStatus.PAID
                )
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Commission marked as paid")
                    // Reload data
                    loadPendingCommissions()
                    loadAdminEarnings()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to mark as paid"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error marking commission as paid", e)
                _error.value = e.message
            }
        }
    }

    /**
     * ✅ Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
