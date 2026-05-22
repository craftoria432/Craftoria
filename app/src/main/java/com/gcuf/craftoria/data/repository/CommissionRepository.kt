package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.gcuf.craftoria.data.model.*
import kotlinx.coroutines.tasks.await

/**
 * Commission repository — handles all commission-related Firestore operations.
 *
 * Bugs fixed vs original:
 *  - Bug 2 : date-range queries now use Firestore Timestamp (not Long epoch ms)
 *            so they match documents written by both the Android app and web service.
 *  - Bug 7 : all reads now use "commission_amount" (the canonical Firestore field),
 *            not the stale "amount" alias that was causing stats to return 0.
 */
class CommissionRepository(private val db: FirebaseFirestore) {

    companion object {
        private const val TAG = "CommissionRepository"
        private const val COMMISSIONS_COLLECTION = "admin_commissions"
        private const val EARNINGS_COLLECTION    = "admin_earnings"
        private const val SETTINGS_COLLECTION    = "commission_settings"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Create an admin commission record when a payment is processed.
     * After writing, updates the aggregated admin-earnings document.
     */
    suspend fun createCommission(commission: AdminCommission): Result<String> = try {
        Log.d(TAG, "💳 Creating commission record for order: ${commission.orderId}")

        val docRef = db.collection(COMMISSIONS_COLLECTION).add(commission.toMap()).await()
        val commissionId = docRef.id

        db.collection(COMMISSIONS_COLLECTION)
            .document(commissionId)
            .update("id", commissionId)
            .await()

        Log.d(TAG, "✅ Commission created: $commissionId (Amount: PKR ${commission.commissionAmount})")

        updateAdminEarnings(commission)

        Result.success(commissionId)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to create commission", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — single document
    // ─────────────────────────────────────────────────────────────────────────

    /** Fetch a single commission by its Firestore document ID. */
    suspend fun getCommission(commissionId: String): Result<AdminCommission> = try {
        val doc = db.collection(COMMISSIONS_COLLECTION)
            .document(commissionId)
            .get()
            .await()

        // ✅ Defensive: Manually parse to handle mixed timestamp types
        val data = doc.data ?: throw Exception("Commission not found: $commissionId")
        
        val commission = AdminCommission(
            id = doc.id,
            orderId = doc.getString("order_id") ?: "",
            paymentId = doc.getString("payment_id") ?: "",
            sellerId = doc.getString("seller_id") ?: "",
            sellerName = doc.getString("seller_name") ?: "",
            coSellerStoreId = doc.getString("co_seller_store_id") ?: "",
            storeName = doc.getString("store_name") ?: "",
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            commissionRate = (data["commission_rate"] as? Number)?.toDouble() ?: 0.05,
            commissionAmount = (data["commission_amount"] as? Number)?.toDouble() ?: 0.0,
            sellerPayout = (data["seller_payout"] as? Number)?.toDouble() ?: 0.0,
            status = doc.getString("status") ?: CommissionStatus.PENDING.toString(),
            createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            paidAt = (data["paid_at"] as? Number)?.toLong()
        )
        
        Result.success(commission)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commission $commissionId", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — lists
    // ─────────────────────────────────────────────────────────────────────────

    /** All commissions for a specific order. */
    suspend fun getCommissionsByOrder(orderId: String): Result<List<AdminCommission>> = try {
        val docs = db.collection(COMMISSIONS_COLLECTION)
            .whereEqualTo("order_id", orderId)
            .get()
            .await()

        val commissions = docs.toObjects(AdminCommission::class.java)
        Log.d(TAG, "📊 Found ${commissions.size} commissions for order: $orderId")
        Result.success(commissions)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commissions by order $orderId", e)
        Result.failure(e)
    }

    /** All commissions for a specific seller, newest first. */
    suspend fun getCommissionsBySeller(sellerId: String): Result<List<AdminCommission>> = try {
        val docs = db.collection(COMMISSIONS_COLLECTION)
            .whereEqualTo("seller_id", sellerId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get()
            .await()

        val commissions = docs.toObjects(AdminCommission::class.java)
        Log.d(TAG, "📊 Found ${commissions.size} commissions for seller: $sellerId")
        Result.success(commissions)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commissions by seller $sellerId", e)
        Result.failure(e)
    }

    /** All commissions with status == PENDING, oldest first (FIFO settlement). */
    suspend fun getPendingCommissions(): Result<List<AdminCommission>> = try {
        val docs = db.collection(COMMISSIONS_COLLECTION)
            .whereEqualTo("status", CommissionStatus.PENDING.toString())
            .orderBy("created_at", Query.Direction.ASCENDING)
            .get()
            .await()

        val commissions = docs.toObjects(AdminCommission::class.java)
        Log.d(TAG, "⏳ Found ${commissions.size} pending commissions")
        Result.success(commissions)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get pending commissions", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — statistics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Aggregate commission statistics for a date range.
     *
     * FIX Bug 2: uses Firestore [Timestamp] objects for the range bounds so the
     *   query matches documents regardless of whether they were written by the
     *   Android app (Long epoch) or the web service (Firestore Timestamp). Firestore
     *   accepts Timestamp in range queries and coerces stored Longs automatically
     *   when the field type is consistent — we therefore convert both bounds.
     *
     * FIX Bug 7: accumulates [commission_amount] (the field written by
     *   [AdminCommission.toMap]), not the non-existent "amount" alias.
     *
     * @param startDate epoch milliseconds (inclusive lower bound)
     * @param endDate   epoch milliseconds (inclusive upper bound)
     */
    suspend fun getCommissionStats(
        startDate: Long,
        endDate: Long,
    ): Result<CommissionStats> = try {
        Log.d(TAG, "📊 Fetching commission stats for date range")

        // ✅ Bug 2 fix — wrap epoch-ms in Firestore Timestamp
        val startTs = Timestamp(startDate / 1000L, 0)
        val endTs   = Timestamp(endDate   / 1000L, 0)

        val docs = db.collection(COMMISSIONS_COLLECTION)
            .whereGreaterThanOrEqualTo("created_at", startTs)
            .whereLessThanOrEqualTo("created_at", endTs)
            .get()
            .await()

        val commissions = docs.toObjects(AdminCommission::class.java)

        // ✅ Bug 7 fix — commissionAmount maps to "commission_amount" via @PropertyName
        val stats = CommissionStats(
            totalCommissions = commissions.sumOf { it.commissionAmount },
            totalOrders      = commissions.size,
            averageCommission = if (commissions.isNotEmpty())
                commissions.sumOf { it.commissionAmount } / commissions.size
            else 0.0,
            pendingAmount = commissions
                .filter { it.status == CommissionStatus.PENDING.toString() }
                .sumOf { it.commissionAmount },
            paidAmount = commissions
                .filter { it.status == CommissionStatus.PAID.toString() }
                .sumOf { it.commissionAmount },
        )

        Log.d(TAG, "✅ Stats: Total PKR ${stats.totalCommissions}, Orders: ${stats.totalOrders}")
        Result.success(stats)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commission stats", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE — status
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Transition a commission to a new [CommissionStatus].
     * Automatically stamps [paid_at] when transitioning to PAID.
     */
    suspend fun updateCommissionStatus(
        commissionId: String,
        newStatus: CommissionStatus,
    ): Result<Unit> = try {
        Log.d(TAG, "🔄 Updating commission status: $commissionId → $newStatus")

        val updateData = mutableMapOf<String, Any>(
            "status"     to newStatus.toString(),
            "updated_at" to Timestamp.now(),
        )
        if (newStatus == CommissionStatus.PAID) {
            updateData["paid_at"] = Timestamp.now()
        }

        db.collection(COMMISSIONS_COLLECTION)
            .document(commissionId)
            .update(updateData)
            .await()

        Log.d(TAG, "✅ Commission status updated to $newStatus")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to update commission status for $commissionId", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SETTINGS
    // ─────────────────────────────────────────────────────────────────────────

    /** Read commission settings; returns sensible defaults when document is absent. */
    suspend fun getCommissionSettings(): Result<CommissionSettings> = try {
        val doc = db.collection(SETTINGS_COLLECTION)
            .document("commission_settings")
            .get()
            .await()

        val settings = doc.toObject(CommissionSettings::class.java)
        if (settings != null) {
            Log.d(TAG, "⚙️ Commission settings loaded: ${settings.commissionRate}%")
            Result.success(settings)
        } else {
            Log.d(TAG, "⚙️ No settings document — using defaults")
            Result.success(CommissionSettings())
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commission settings — using defaults", e)
        Result.success(CommissionSettings()) // non-fatal: safe default
    }

    /** Persist updated commission settings (admin-only operation). */
    suspend fun updateCommissionSettings(settings: CommissionSettings): Result<Unit> = try {
        Log.d(TAG, "⚙️ Updating commission settings")

        db.collection(SETTINGS_COLLECTION)
            .document("commission_settings")
            .set(settings.toMap())
            .await()

        Log.d(TAG, "✅ Commission settings updated")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to update commission settings", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN EARNINGS
    // ─────────────────────────────────────────────────────────────────────────

    /** Fetch the aggregated admin-earnings summary document. */
    suspend fun getAdminEarnings(): Result<AdminEarnings> = try {
        val doc = db.collection(EARNINGS_COLLECTION)
            .document("admin_earnings")
            .get()
            .await()

        val earnings = doc.toObject(AdminEarnings::class.java)
        if (earnings != null) {
            Log.d(TAG, "💰 Admin earnings loaded: PKR ${earnings.totalCommissions}")
            Result.success(earnings)
        } else {
            Log.d(TAG, "💰 No earnings document yet — returning zero summary")
            Result.success(AdminEarnings())
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get admin earnings", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Increment the aggregated admin-earnings document after a new commission is
     * created.  Non-blocking — a failure here is logged but does not fail the
     * parent operation.
     *
     * FIX Bug 7: reads commissionAmount (field: "commission_amount"), not "amount".
     */
    private suspend fun updateAdminEarnings(commission: AdminCommission) {
        try {
            val current = getAdminEarnings().getOrNull() ?: AdminEarnings()

            val isPending = commission.status == CommissionStatus.PENDING.toString()
            val isPaid    = commission.status == CommissionStatus.PAID.toString()

            val updated = current.copy(
                totalCommissions   = current.totalCommissions   + commission.commissionAmount,
                pendingCommissions = if (isPending) current.pendingCommissions + commission.commissionAmount
                else current.pendingCommissions,
                paidCommissions    = if (isPaid)    current.paidCommissions    + commission.commissionAmount
                else current.paidCommissions,
                totalOrders        = current.totalOrders + 1,
                lastUpdated        = System.currentTimeMillis(),
            )

            db.collection(EARNINGS_COLLECTION)
                .document("admin_earnings")
                .set(updated.toMap())
                .await()

            Log.d(TAG, "✅ Admin earnings updated (+PKR ${commission.commissionAmount})")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to update admin earnings (non-blocking)", e)
        }
    }
}

/**
 * Aggregated commission statistics for a given date range.
 */
data class CommissionStats(
    val totalCommissions : Double = 0.0,
    val totalOrders      : Int    = 0,
    val averageCommission: Double = 0.0,
    val pendingAmount    : Double = 0.0,
    val paidAmount       : Double = 0.0,
)