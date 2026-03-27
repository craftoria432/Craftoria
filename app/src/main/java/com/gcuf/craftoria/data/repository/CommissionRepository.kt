package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.*
import kotlinx.coroutines.tasks.await

/**
 * ✅ PRODUCTION-READY: Commission repository
 * Handles all commission-related database operations
 */
class CommissionRepository(private val db: FirebaseFirestore) {

    companion object {
        private const val TAG = "CommissionRepository"
        private const val COMMISSIONS_COLLECTION = "admin_commissions"
        private const val EARNINGS_COLLECTION = "admin_earnings"
        private const val SETTINGS_COLLECTION = "commission_settings"
    }

    /**
     * ✅ Create admin commission record
     * Called when a payment is processed
     */
    suspend fun createCommission(commission: AdminCommission): Result<String> = try {
        Log.d(TAG, "💳 Creating commission record for order: ${commission.orderId}")

        val docRef = db.collection(COMMISSIONS_COLLECTION).add(commission.toMap()).await()
        val commissionId = docRef.id

        // Update document with ID
        db.collection(COMMISSIONS_COLLECTION).document(commissionId)
            .update("id", commissionId).await()

        Log.d(TAG, "✅ Commission created: $commissionId (Amount: PKR ${commission.commissionAmount})")

        // Update admin earnings (non-blocking)
        updateAdminEarnings(commission)

        Result.success(commissionId)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to create commission", e)
        Result.failure(e)
    }

    /**
     * ✅ Get commission by ID
     */
    suspend fun getCommission(commissionId: String): Result<AdminCommission> = try {
        val doc = db.collection(COMMISSIONS_COLLECTION)
            .document(commissionId)
            .get()
            .await()

        val commission = doc.toObject(AdminCommission::class.java)
        if (commission != null) {
            Result.success(commission)
        } else {
            Result.failure(Exception("Commission not found"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commission", e)
        Result.failure(e)
    }

    /**
     * ✅ Get all commissions for a specific order
     */
    suspend fun getCommissionsByOrder(orderId: String): Result<List<AdminCommission>> = try {
        val docs = db.collection(COMMISSIONS_COLLECTION)
            .whereEqualTo("order_id", orderId)
            .get()
            .await()

        val commissions = docs.toObjects(AdminCommission::class.java)
        Log.d(TAG, "📊 Found ${commissions.size} commissions for order: $orderId")
        Result.success(commissions)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commissions by order", e)
        Result.failure(e)
    }

    /**
     * ✅ Get all commissions for a specific seller
     */
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
        Log.e(TAG, "❌ Failed to get commissions by seller", e)
        Result.failure(e)
    }

    /**
     * ✅ Get pending commissions
     */
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

    /**
     * ✅ Update commission status
     */
    suspend fun updateCommissionStatus(
        commissionId: String,
        newStatus: CommissionStatus
    ): Result<Unit> = try {
        Log.d(TAG, "🔄 Updating commission status: $commissionId -> $newStatus")

        val updateData = mutableMapOf<String, Any>(
            "status" to newStatus.toString(),
            "updated_at" to System.currentTimeMillis()
        )

        if (newStatus == CommissionStatus.PAID) {
            updateData["paid_at"] = System.currentTimeMillis()
        }

        db.collection(COMMISSIONS_COLLECTION)
            .document(commissionId)
            .update(updateData)
            .await()

        Log.d(TAG, "✅ Commission status updated")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to update commission status", e)
        Result.failure(e)
    }

    /**
     * ✅ Get commission settings
     */
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
            // Return default settings if not found
            val defaultSettings = CommissionSettings()
            Log.d(TAG, "⚙️ Using default commission settings: ${defaultSettings.commissionRate}%")
            Result.success(defaultSettings)
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commission settings", e)
        // Return default settings on error
        Result.success(CommissionSettings())
    }

    /**
     * ✅ Update commission settings (admin only)
     */
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

    /**
     * ✅ Get admin earnings summary
     */
    suspend fun getAdminEarnings(): Result<AdminEarnings> = try {
        val doc = db.collection(EARNINGS_COLLECTION)
            .document("admin_earnings")
            .get()
            .await()

        val earnings = doc.toObject(AdminEarnings::class.java)
        if (earnings != null) {
            Log.d(TAG, "💰 Admin earnings: PKR ${earnings.totalCommissions}")
            Result.success(earnings)
        } else {
            // Return default if not found
            Result.success(AdminEarnings())
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get admin earnings", e)
        Result.failure(e)
    }

    /**
     * ✅ Get commission statistics for date range
     */
    suspend fun getCommissionStats(
        startDate: Long,
        endDate: Long
    ): Result<CommissionStats> = try {
        Log.d(TAG, "📊 Fetching commission stats for date range")

        val docs = db.collection(COMMISSIONS_COLLECTION)
            .whereGreaterThanOrEqualTo("created_at", startDate)
            .whereLessThanOrEqualTo("created_at", endDate)
            .get()
            .await()

        val commissions = docs.toObjects(AdminCommission::class.java)

        val stats = CommissionStats(
            totalCommissions = commissions.sumOf { it.commissionAmount },
            totalOrders = commissions.size,
            averageCommission = if (commissions.isNotEmpty()) {
                commissions.sumOf { it.commissionAmount } / commissions.size
            } else {
                0.0
            },
            pendingAmount = commissions
                .filter { it.status == CommissionStatus.PENDING.toString() }
                .sumOf { it.commissionAmount },
            paidAmount = commissions
                .filter { it.status == CommissionStatus.PAID.toString() }
                .sumOf { it.commissionAmount }
        )

        Log.d(TAG, "✅ Stats: Total PKR ${stats.totalCommissions}, Orders: ${stats.totalOrders}")
        Result.success(stats)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get commission stats", e)
        Result.failure(e)
    }

    /**
     * ✅ Update admin earnings (internal)
     */
    private suspend fun updateAdminEarnings(commission: AdminCommission) {
        try {
            val currentEarnings = getAdminEarnings().getOrNull() ?: AdminEarnings()

            val updatedEarnings = currentEarnings.copy(
                totalCommissions = currentEarnings.totalCommissions + commission.commissionAmount,
                pendingCommissions = if (commission.status == CommissionStatus.PENDING.toString()) {
                    currentEarnings.pendingCommissions + commission.commissionAmount
                } else {
                    currentEarnings.pendingCommissions
                },
                paidCommissions = if (commission.status == CommissionStatus.PAID.toString()) {
                    currentEarnings.paidCommissions + commission.commissionAmount
                } else {
                    currentEarnings.paidCommissions
                },
                totalOrders = currentEarnings.totalOrders + 1,
                lastUpdated = System.currentTimeMillis()
            )

            db.collection(EARNINGS_COLLECTION)
                .document("admin_earnings")
                .set(updatedEarnings.toMap())
                .await()

            Log.d(TAG, "✅ Admin earnings updated")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to update admin earnings (non-blocking)", e)
        }
    }
}

/**
 * ✅ Commission statistics data class
 */
data class CommissionStats(
    val totalCommissions: Double = 0.0,
    val totalOrders: Int = 0,
    val averageCommission: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val paidAmount: Double = 0.0
)
