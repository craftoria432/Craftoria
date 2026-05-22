package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.data.repository.CommissionRepository
import com.gcuf.craftoria.data.repository.CommissionRepositoryProduction
import com.gcuf.craftoria.data.repository.CommissionStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Commission management ViewModel.
 *
 * Wires [CommissionRepository] (direct Firestore reads) and
 * [CommissionRepositoryProduction] (retry + real-time listener) together.
 *
 * Bugs fixed vs original:
 *  - Bug 6: [subscribeToCommissionUpdates] now receives [viewModelScope] so the
 *           polling fallback coroutine is bound to the ViewModel's lifecycle and
 *           cancelled automatically in [onCleared].
 *  - Bug 6: [removeCommissionListener] is stored and called in [onCleared] so the
 *           Firestore snapshot listener is also removed when the ViewModel is destroyed.
 *  - Bonus: [loadAdminEarnings], [loadPendingCommissions], and [loadCommissionStats]
 *           now use the Flow-based production repository (with retry) instead of the
 *           plain repository, making the app resilient to transient network errors.
 */
class CommissionViewModel : ViewModel() {

    companion object {
        private const val TAG = "CommissionViewModel"
    }

    private val db = FirebaseFirestore.getInstance()

    // Two repositories co-exist:
    //  - commissionRepo        : direct await()-based calls (create, update, settings)
    //  - commissionRepoProd    : Flow-based calls with retry + real-time listener
    private val commissionRepo     = CommissionRepository(db)
    private val commissionRepoProd = CommissionRepositoryProduction(db)

    // ─────────────────────────────────────────────────────────────────────────
    // State flows (observed by the UI layer)
    // ─────────────────────────────────────────────────────────────────────────

    private val _commissionSettings = MutableStateFlow<CommissionSettings?>(null)
    val commissionSettings: StateFlow<CommissionSettings?> = _commissionSettings.asStateFlow()

    private val _adminEarnings = MutableStateFlow<AdminEarnings?>(null)
    val adminEarnings: StateFlow<AdminEarnings?> = _adminEarnings.asStateFlow()

    private val _commissions = MutableStateFlow<List<AdminCommission>>(emptyList())
    val commissions: StateFlow<List<AdminCommission>> = _commissions.asStateFlow()

    private val _pendingCommissions = MutableStateFlow<List<AdminCommission>>(emptyList())
    val pendingCommissions: StateFlow<List<AdminCommission>> = _pendingCommissions.asStateFlow()

    private val _commissionStats = MutableStateFlow<CommissionStats?>(null)
    val commissionStats: StateFlow<CommissionStats?> = _commissionStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Listener handle — must be cancelled in onCleared (Bug 6 fix)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stored so [onCleared] can remove the Firestore snapshot listener.
     * Without this, the listener keeps firing (and allocating) after the
     * ViewModel is destroyed.
     */
    private var removeCommissionListener: (() -> Unit)? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Commission settings
    // ─────────────────────────────────────────────────────────────────────────

    /** Load commission settings from Firestore (returns defaults on error). */
    fun loadCommissionSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepo.getCommissionSettings()
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

    /** Persist updated commission settings (admin-only). */
    fun updateCommissionSettings(settings: CommissionSettings) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepo.updateCommissionSettings(settings)
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

    // ─────────────────────────────────────────────────────────────────────────
    // Admin earnings
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Load admin earnings using the production repository (retry on failure).
     * The Flow emits a single Result and completes; errors are surfaced via [_error].
     */
    fun loadAdminEarnings() {
        _isLoading.value = true
        commissionRepoProd.getAdminEarnings()
            .onEach { result ->
                if (result.isSuccess) {
                    _adminEarnings.value = result.getOrNull()
                    Log.d(TAG, "✅ Admin earnings loaded")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load earnings"
                    Log.e(TAG, "❌ Error loading admin earnings", result.exceptionOrNull())
                }
                _isLoading.value = false
            }
            .catch { e ->
                _error.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Uncaught error in loadAdminEarnings", e)
            }
            .launchIn(viewModelScope)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pending commissions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Load pending commissions using the production repository (retry on failure).
     */
    fun loadPendingCommissions() {
        _isLoading.value = true
        commissionRepoProd.getPendingCommissions()
            .onEach { result ->
                if (result.isSuccess) {
                    _pendingCommissions.value = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "✅ Pending commissions loaded: ${_pendingCommissions.value.size}")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load pending"
                    Log.e(TAG, "❌ Error loading pending commissions", result.exceptionOrNull())
                }
                _isLoading.value = false
            }
            .catch { e ->
                _error.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Uncaught error in loadPendingCommissions", e)
            }
            .launchIn(viewModelScope)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Commissions by seller
    // ─────────────────────────────────────────────────────────────────────────

    /** Load all commissions for a specific seller (used in seller detail screens). */
    fun loadCommissionsBySeller(sellerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = commissionRepo.getCommissionsBySeller(sellerId)
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

    // ─────────────────────────────────────────────────────────────────────────
    // Commission statistics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Load commission statistics for the given epoch-millisecond date range.
     *
     * Uses the production repository (retry + Bug 2 Timestamp fix + Bug 7 field fix).
     * Stats are mapped from the raw [Map] returned by the repository into
     * [CommissionStats] for type safety.
     */
    fun loadCommissionStats(startDate: Long, endDate: Long) {
        _isLoading.value = true
        commissionRepoProd.getCommissionStats(startDate, endDate)
            .onEach { result ->
                if (result.isSuccess) {
                    val raw = result.getOrNull() ?: emptyMap()
                    _commissionStats.value = CommissionStats(
                        totalCommissions  = (raw["totalCommissions"]  as? Double) ?: 0.0,
                        totalOrders       = (raw["totalOrders"]       as? Int)    ?: 0,
                        pendingAmount     = (raw["pendingAmount"]      as? Double) ?: 0.0,
                        paidAmount        = (raw["paidAmount"]         as? Double) ?: 0.0,
                        averageCommission = if ((raw["totalOrders"] as? Int ?: 0) > 0)
                            ((raw["totalCommissions"] as? Double) ?: 0.0) /
                                    (raw["totalOrders"] as? Int ?: 1)
                        else 0.0,
                    )
                    Log.d(TAG, "✅ Commission stats loaded")
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load stats"
                    Log.e(TAG, "❌ Error loading commission stats", result.exceptionOrNull())
                }
                _isLoading.value = false
            }
            .catch { e ->
                _error.value = e.message
                _isLoading.value = false
                Log.e(TAG, "❌ Uncaught error in loadCommissionStats", e)
            }
            .launchIn(viewModelScope)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Real-time listener
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Attach a Firestore real-time listener for all commissions.
     *
     * FIX Bug 6: passes [viewModelScope] to [CommissionRepositoryProduction.subscribeToCommissionUpdates]
     *   so the polling fallback coroutine is scoped to this ViewModel and automatically
     *   cancelled in [onCleared] — no more GlobalScope leak.
     *
     * Safe to call multiple times: the previous listener is removed before
     * the new one is registered.
     */
    fun subscribeToCommissions() {
        // Remove any existing listener first to avoid duplicates
        removeCommissionListener?.invoke()

        // ✅ Bug 6 fix — pass viewModelScope (not GlobalScope) for the fallback coroutine
        removeCommissionListener = commissionRepoProd.subscribeToCommissionUpdates(
            fallbackScope = viewModelScope,
            onUpdate = { commissions ->
                _commissions.value = commissions
                Log.d(TAG, "📡 Commission update received: ${commissions.size} records")
            },
            onError = { error ->
                _error.value = error.message
                Log.e(TAG, "❌ Commission listener error", error)
            },
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark as paid
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Transition a commission to PAID status via [CommissionRepository] and
     * reload the affected lists so the UI reflects the change immediately.
     */
    fun markCommissionAsPaid(commissionId: String) {
        viewModelScope.launch {
            try {
                val result = commissionRepo.updateCommissionStatus(
                    commissionId,
                    CommissionStatus.PAID,
                )
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Commission $commissionId marked as paid")
                    // Refresh dependent data
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

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    /** Clear the current error state (call after the UI has shown the error). */
    fun clearError() {
        _error.value = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called by the framework when the ViewModel is being destroyed.
     *
     * FIX Bug 6: explicitly removes the Firestore snapshot listener and cancels
     *   the polling fallback job (via [viewModelScope] cancellation) so no
     *   Firestore reads or coroutines leak beyond the ViewModel's lifecycle.
     */
    override fun onCleared() {
        super.onCleared()
        removeCommissionListener?.invoke()
        commissionRepoProd.stopPolling()
        Log.d(TAG, "🧹 CommissionViewModel cleared — listener and polling stopped")
    }
}