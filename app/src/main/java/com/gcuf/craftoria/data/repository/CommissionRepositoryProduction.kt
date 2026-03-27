package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.AdminCommission
import com.gcuf.craftoria.data.model.AdminEarnings
import com.gcuf.craftoria.utils.FirebaseRetryHelper
import com.gcuf.craftoria.utils.RetryConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Production-ready Commission Repository with retry logic and error handling
 */
class CommissionRepositoryProduction(private val db: FirebaseFirestore) {
    
    private val commissionsCollection = "admin_commissions"
    private val retryConfig = RetryConfig(
        maxRetries = 3,
        initialDelayMs = 1000,
        maxDelayMs = 10000,
        backoffMultiplier = 2.0
    )
    
    /**
     * Get admin earnings with retry
     */
    fun getAdminEarnings(): Flow<Result<AdminEarnings>> = flow {
        try {
            val result = FirebaseRetryHelper.withRetry(
                "getAdminEarnings",
                retryConfig
            ) {
                val snapshot = db.collection(commissionsCollection)
                    .get()
                    .await()
                
                var totalCommissions = 0.0
                var pendingCommissions = 0.0
                var paidCommissions = 0.0
                var totalOrders = 0
                
                snapshot.documents.forEach { doc ->
                    val amount = doc.getDouble("commission_amount") ?: 0.0
                    val status = doc.getString("status") ?: "pending"
                    
                    totalCommissions += amount
                    
                    when (status) {
                        "pending" -> pendingCommissions += amount
                        "paid" -> paidCommissions += amount
                    }
                    
                    totalOrders++
                }
                
                AdminEarnings(
                    totalCommissions = totalCommissions,
                    pendingCommissions = pendingCommissions,
                    paidCommissions = paidCommissions,
                    totalOrders = totalOrders
                )
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e("CommissionRepository", "Error fetching admin earnings", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Get pending commissions with retry
     */
    fun getPendingCommissions(): Flow<Result<List<AdminCommission>>> = flow {
        try {
            val result = FirebaseRetryHelper.withRetry(
                "getPendingCommissions",
                retryConfig
            ) {
                val snapshot = db.collection(commissionsCollection)
                    .whereEqualTo("status", "pending")
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                snapshot.documents.mapNotNull { doc ->
                    try {
                        AdminCommission(
                            id = doc.id,
                            orderId = doc.getString("order_id") ?: "",
                            sellerId = doc.getString("seller_id") ?: "",
                            sellerName = doc.getString("seller_name") ?: "",
                            commissionAmount = doc.getDouble("commission_amount") ?: 0.0,
                            status = doc.getString("status") ?: "pending",
                            createdAt = doc.getLong("created_at") ?: System.currentTimeMillis(),
                            paidAt = doc.getLong("paid_at")
                        )
                    } catch (e: Exception) {
                        Log.e("CommissionRepository", "Error parsing commission document", e)
                        null
                    }
                }
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e("CommissionRepository", "Error fetching pending commissions", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Mark commission as paid with retry
     */
    suspend fun markCommissionAsPaid(
        commissionId: String,
        paymentNotes: String = ""
    ): Result<Unit> = try {
        FirebaseRetryHelper.withRetry(
            "markCommissionAsPaid",
            retryConfig
        ) {
            db.collection(commissionsCollection)
                .document(commissionId)
                .update(
                    mapOf(
                        "status" to "paid",
                        "paid_at" to com.google.firebase.Timestamp.now(),
                        "payment_notes" to paymentNotes,
                        "updated_at" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("CommissionRepository", "Error marking commission as paid", e)
        Result.failure(e)
    }
    
    /**
     * Get commission statistics with retry
     */
    fun getCommissionStats(
        startDate: Long,
        endDate: Long
    ): Flow<Result<Map<String, Any>>> = flow {
        try {
            val result = FirebaseRetryHelper.withRetry(
                "getCommissionStats",
                retryConfig
            ) {
                val snapshot = db.collection(commissionsCollection)
                    .whereGreaterThanOrEqualTo("created_at", com.google.firebase.Timestamp(startDate / 1000, 0))
                    .whereLessThanOrEqualTo("created_at", com.google.firebase.Timestamp(endDate / 1000, 0))
                    .get()
                    .await()
                
                var totalCommissions = 0.0
                var pendingAmount = 0.0
                var paidAmount = 0.0
                var totalOrders = 0
                
                snapshot.documents.forEach { doc ->
                    val amount = doc.getDouble("amount") ?: 0.0
                    val status = doc.getString("status") ?: "pending"
                    
                    totalCommissions += amount
                    
                    when (status) {
                        "pending" -> pendingAmount += amount
                        "paid" -> paidAmount += amount
                    }
                    
                    totalOrders++
                }
                
                mapOf(
                    "totalCommissions" to totalCommissions,
                    "pendingAmount" to pendingAmount,
                    "paidAmount" to paidAmount,
                    "totalOrders" to totalOrders,
                    "startDate" to startDate,
                    "endDate" to endDate
                )
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e("CommissionRepository", "Error fetching commission stats", e)
            emit(Result.failure(e))
        }
    }
    
    /**
     * Subscribe to commission updates with error handling
     */
    fun subscribeToCommissionUpdates(
        onUpdate: (List<AdminCommission>) -> Unit,
        onError: (Exception) -> Unit
    ): () -> Unit {
        val listener = db.collection(commissionsCollection)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CommissionRepository", "Error in commission listener", error)
                    onError(error)
                    
                    // Fallback to polling
                    startPolling(onUpdate, onError)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val commissions = snapshot.documents.mapNotNull { doc ->
                        try {
                            AdminCommission(
                                id = doc.id,
                                orderId = doc.getString("order_id") ?: "",
                                sellerId = doc.getString("seller_id") ?: "",
                                sellerName = doc.getString("seller_name") ?: "",
                                commissionAmount = doc.getDouble("commission_amount") ?: 0.0,
                                status = doc.getString("status") ?: "pending",
                                createdAt = doc.getLong("created_at") ?: System.currentTimeMillis(),
                                paidAt = doc.getLong("paid_at")
                            )
                        } catch (e: Exception) {
                            Log.e("CommissionRepository", "Error parsing commission", e)
                            null
                        }
                    }
                    onUpdate(commissions)
                }
            }
        
        return { listener.remove() }
    }
    
    /**
     * Fallback polling mechanism
     */
    private fun startPolling(
        onUpdate: (List<AdminCommission>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Implement polling every 30 seconds
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    delay(30000)
                    val snapshot = db.collection(commissionsCollection)
                        .orderBy("created_at", Query.Direction.DESCENDING)
                        .limit(50)
                        .get()
                        .await()
                    
                    val commissions = snapshot.documents.mapNotNull { doc ->
                        try {
                            AdminCommission(
                                id = doc.id,
                                orderId = doc.getString("order_id") ?: "",
                                sellerId = doc.getString("seller_id") ?: "",
                                sellerName = doc.getString("seller_name") ?: "",
                                commissionAmount = doc.getDouble("commission_amount") ?: 0.0,
                                status = doc.getString("status") ?: "pending",
                                createdAt = doc.getLong("created_at") ?: System.currentTimeMillis(),
                                paidAt = doc.getLong("paid_at")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    onUpdate(commissions)
                } catch (e: Exception) {
                    Log.e("CommissionRepository", "Polling error", e)
                    onError(e)
                }
            }
        }
    }
}
