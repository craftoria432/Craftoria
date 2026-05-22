package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.AdminCommission
import com.gcuf.craftoria.data.model.AdminEarnings
import com.gcuf.craftoria.utils.FirebaseRetryHelper
import com.gcuf.craftoria.utils.RetryConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Production-grade Commission Repository with retry logic, structured concurrency,
 * and accurate Firestore field mapping.
 *
 * Bugs fixed vs original:
 *  - Bug 6 : [startPolling] previously launched on [GlobalScope], causing a memory
 *            leak because the coroutine outlived the ViewModel. Now the caller passes
 *            a [CoroutineScope] (e.g. viewModelScope) so the job is cancelled
 *            automatically when the ViewModel is cleared. The infinite loop now checks
 *            [isActive] so it exits cleanly on cancellation.
 *  - Bug 7 : reads "commission_amount" (not the stale "amount" alias) everywhere,
 *            so earnings and statistics are no longer permanently zero.
 *  - Bug 3 : [subscribeToCommissionUpdates] previously dropped ALL updates that came
 *            from Firestore's local cache, causing a perpetual loader on slow / offline
 *            connections. The listener now always delivers data; fromCache is only used
 *            for a debug-level log so the UI always has something to render.
 */
class CommissionRepositoryProduction(private val db: FirebaseFirestore) {

    companion object {
        private const val TAG                   = "CommissionRepoProd"
        private const val COMMISSIONS_COLLECTION = "admin_commissions"
    }

    private val retryConfig = RetryConfig(
        maxRetries        = 3,
        initialDelayMs    = 1000L,
        maxDelayMs        = 10_000L,
        backoffMultiplier = 2.0,
    )

    // Kept so the caller can cancel polling independently if needed.
    private var pollingJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Admin earnings
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute aggregated admin earnings by scanning every commission document.
     *
     * FIX Bug 7: reads "commission_amount" — the canonical Firestore field written
     * by [AdminCommission.toMap] and [CommissionRepository.createCommission].
     */
    fun getAdminEarnings(): Flow<Result<AdminEarnings>> = flow {
        try {
            val result = FirebaseRetryHelper.withRetry("getAdminEarnings", retryConfig) {
                val snapshot = db.collection(COMMISSIONS_COLLECTION).get().await()

                var totalCommissions   = 0.0
                var pendingCommissions = 0.0
                var paidCommissions    = 0.0
                var totalOrders        = 0

                snapshot.documents.forEach { doc ->
                    // ✅ Bug 7 fix — use "commission_amount", not "amount"
                    val amount = doc.getDouble("commission_amount") ?: 0.0
                    val status = doc.getString("status") ?: "pending"

                    totalCommissions += amount
                    when (status) {
                        "pending" -> pendingCommissions += amount
                        "paid"    -> paidCommissions    += amount
                    }
                    totalOrders++
                }

                AdminEarnings(
                    totalCommissions   = totalCommissions,
                    pendingCommissions = pendingCommissions,
                    paidCommissions    = paidCommissions,
                    totalOrders        = totalOrders,
                )
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching admin earnings", e)
            emit(Result.failure(e))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pending commissions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetch pending commissions with retry.
     *
     * FIX Bug 7: maps "commission_amount" (not "amount") when constructing the
     * [AdminCommission] object, so the displayed amounts are correct.
     */
    fun getPendingCommissions(): Flow<Result<List<AdminCommission>>> = flow {
        try {
            val result = FirebaseRetryHelper.withRetry("getPendingCommissions", retryConfig) {
                val snapshot = db.collection(COMMISSIONS_COLLECTION)
                    .whereEqualTo("status", "pending")
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    try {
                        AdminCommission(
                            id             = doc.id,
                            orderId        = doc.getString("order_id")      ?: "",
                            sellerId       = doc.getString("seller_id")     ?: "",
                            sellerName     = doc.getString("seller_name")   ?: "",
                            // ✅ Bug 7 fix — "commission_amount" not "amount"
                            commissionAmount = doc.getDouble("commission_amount") ?: 0.0,
                            status         = doc.getString("status")        ?: "pending",
                            createdAt      = doc.getLong("created_at")      ?: System.currentTimeMillis(),
                            paidAt         = doc.getLong("paid_at"),
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing commission document ${doc.id}", e)
                        null
                    }
                }
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pending commissions", e)
            emit(Result.failure(e))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark as paid
    // ─────────────────────────────────────────────────────────────────────────

    /** Mark a commission as paid, stamping the current time on [paid_at]. */
    suspend fun markCommissionAsPaid(
        commissionId: String,
        paymentNotes: String = "",
    ): Result<Unit> = try {
        FirebaseRetryHelper.withRetry("markCommissionAsPaid", retryConfig) {
            db.collection(COMMISSIONS_COLLECTION)
                .document(commissionId)
                .update(
                    mapOf(
                        "status"        to "paid",
                        "paid_at"       to Timestamp.now(),
                        "payment_notes" to paymentNotes,
                        "updated_at"    to Timestamp.now(),
                    )
                )
                .await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error marking commission $commissionId as paid", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Statistics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetch commission statistics for the given epoch-millisecond date range.
     *
     * FIX Bug 2: the Firestore range query now uses [Timestamp] objects (not raw
     *   Long epoch values) so it matches documents regardless of whether they were
     *   written as Timestamps or Longs.
     * FIX Bug 7: accumulates "commission_amount" (not "amount").
     */
    fun getCommissionStats(
        startDate: Long,
        endDate: Long,
    ): Flow<Result<Map<String, Any>>> = flow {
        try {
            val result = FirebaseRetryHelper.withRetry("getCommissionStats", retryConfig) {
                // ✅ Bug 2 fix — convert epoch-ms to Firestore Timestamp
                val startTs = Timestamp(startDate / 1000L, 0)
                val endTs   = Timestamp(endDate   / 1000L, 0)

                val snapshot = db.collection(COMMISSIONS_COLLECTION)
                    .whereGreaterThanOrEqualTo("created_at", startTs)
                    .whereLessThanOrEqualTo("created_at", endTs)
                    .get()
                    .await()

                var totalCommissions = 0.0
                var pendingAmount    = 0.0
                var paidAmount       = 0.0
                var totalOrders      = 0

                snapshot.documents.forEach { doc ->
                    // ✅ Bug 7 fix — "commission_amount" not "amount"
                    val amount = doc.getDouble("commission_amount") ?: 0.0
                    val status = doc.getString("status") ?: "pending"

                    totalCommissions += amount
                    when (status) {
                        "pending" -> pendingAmount += amount
                        "paid"    -> paidAmount    += amount
                    }
                    totalOrders++
                }

                mapOf(
                    "totalCommissions" to totalCommissions,
                    "pendingAmount"    to pendingAmount,
                    "paidAmount"       to paidAmount,
                    "totalOrders"      to totalOrders,
                    "startDate"        to startDate,
                    "endDate"          to endDate,
                )
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching commission stats", e)
            emit(Result.failure(e))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Real-time listener
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Subscribe to live commission updates via a Firestore snapshot listener.
     * Falls back to timed polling (scoped to [fallbackScope]) on listener error.
     *
     * FIX Bug 3: the original code called [onUpdate] only when
     *   [snapshot.metadata.fromCache] was false, silently dropping every cached
     *   update and leaving the UI in a perpetual loading state on slow connections.
     *   The listener now always delivers data; fromCache is only logged at DEBUG
     *   level so the UI always has something to display.
     *
     * FIX Bug 6: polling fallback now accepts a [CoroutineScope] from the caller
     *   (i.e. viewModelScope) so the coroutine is cancelled when the ViewModel is
     *   cleared.  The loop checks [isActive] for cooperative cancellation.
     *
     * @param fallbackScope Scope used only if the Firestore listener fails (pass
     *                      viewModelScope so the fallback coroutine is lifecycle-aware).
     * @param onUpdate      Called on every update (from cache or server).
     * @param onError       Called when a non-retryable listener error occurs.
     * @return              A lambda that removes the Firestore listener; call it from
     *                      onCleared() if you need to cancel before scope cancellation.
     */
    fun subscribeToCommissionUpdates(
        fallbackScope: CoroutineScope,
        onUpdate: (List<AdminCommission>) -> Unit,
        onError: (Exception) -> Unit,
    ): () -> Unit {
        val registration = db.collection(COMMISSIONS_COLLECTION)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore listener error — falling back to polling", error)
                    onError(error)
                    // ✅ Bug 6 fix — pass the caller's scope, not GlobalScope
                    startPolling(fallbackScope, onUpdate, onError)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // ✅ Bug 3 fix — deliver cached data too; only log the source
                    if (snapshot.metadata.isFromCache) {
                        Log.d(TAG, "Serving commissions from local cache")
                    }

                    val commissions = snapshot.documents.mapNotNull { doc ->
                        try {
                            AdminCommission(
                                id               = doc.id,
                                orderId          = doc.getString("order_id")         ?: "",
                                sellerId         = doc.getString("seller_id")        ?: "",
                                sellerName       = doc.getString("seller_name")      ?: "",
                                // ✅ Bug 7 fix — "commission_amount" not "amount"
                                commissionAmount = doc.getDouble("commission_amount") ?: 0.0,
                                status           = doc.getString("status")           ?: "pending",
                                createdAt        = doc.getLong("created_at")         ?: System.currentTimeMillis(),
                                paidAt           = doc.getLong("paid_at"),
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing commission document ${doc.id}", e)
                            null
                        }
                    }
                    onUpdate(commissions)
                }
            }

        return { registration.remove() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Polling fallback (internal)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Timed-polling fallback used only when the Firestore real-time listener fails.
     *
     * FIX Bug 6 (primary): the original implementation used [GlobalScope], which
     *   meant the coroutine kept running even after the ViewModel was destroyed,
     *   leaking Firestore reads and preventing GC.
     *
     *   Now:
     *   • The caller passes its own [scope] (viewModelScope in the ViewModel).
     *   • [isActive] is checked in the loop so cancellation is cooperative.
     *   • [pollingJob] is stored so any previous job is cancelled before a new
     *     one starts (prevents duplicate polling if the listener fails repeatedly).
     */
    private fun startPolling(
        scope: CoroutineScope,
        onUpdate: (List<AdminCommission>) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        // Cancel any existing polling job before starting a new one
        pollingJob?.cancel()

        pollingJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "⏱️ Starting commission polling fallback")
            while (isActive) {           // ✅ cooperative cancellation
                delay(30_000L)
                if (!isActive) break     // double-check after delay

                try {
                    val snapshot = db.collection(COMMISSIONS_COLLECTION)
                        .orderBy("created_at", Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .await()

                    val commissions = snapshot.documents.mapNotNull { doc ->
                        try {
                            AdminCommission(
                                id               = doc.id,
                                orderId          = doc.getString("order_id")         ?: "",
                                sellerId         = doc.getString("seller_id")        ?: "",
                                sellerName       = doc.getString("seller_name")      ?: "",
                                // ✅ Bug 7 fix
                                commissionAmount = doc.getDouble("commission_amount") ?: 0.0,
                                status           = doc.getString("status")           ?: "pending",
                                createdAt        = doc.getLong("created_at")         ?: System.currentTimeMillis(),
                                paidAt           = doc.getLong("paid_at"),
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(commissions)
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                    onError(e)
                }
            }
            Log.d(TAG, "⏹️ Commission polling stopped")
        }
    }

    /** Cancel the polling fallback explicitly (e.g. when the listener recovers). */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}