package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.OrderRefundStatus
import kotlinx.coroutines.tasks.await

/**
 * Migration utility to populate refund_status field for existing orders.
 * 
 * This utility:
 * 1. Queries all orders that don't have refund_status field
 * 2. Checks if there's a completed refund for each order
 * 3. Sets refund_status to COMPLETED if refund exists, otherwise NONE
 * 4. Handles batch updates for performance
 */
class OrderRefundStatusMigration(private val firestore: FirebaseFirestore) {
    companion object {
        private const val TAG = "OrderRefundStatusMigration"
        private const val ORDERS_COLLECTION = "orders"
        private const val REFUNDS_COLLECTION = "refunds"
        private const val BATCH_SIZE = 100
    }

    /**
     * Execute the migration to populate refund_status for all existing orders.
     * Safe to run multiple times — only updates orders that need it.
     */
    suspend fun migrateOrderRefundStatuses(): Result<MigrationStats> {
        return try {
            Log.d(TAG, "Starting order refund status migration...")
            
            val stats = MigrationStats()
            var lastDocumentSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
            
            while (true) {
                val query = if (lastDocumentSnapshot == null) {
                    firestore.collection(ORDERS_COLLECTION)
                        .limit(BATCH_SIZE.toLong())
                } else {
                    firestore.collection(ORDERS_COLLECTION)
                        .startAfter(lastDocumentSnapshot)
                        .limit(BATCH_SIZE.toLong())
                }
                
                val snapshot = query.get().await()
                if (snapshot.documents.isEmpty()) break
                
                val batch = firestore.batch()
                var batchCount = 0
                
                for (doc in snapshot.documents) {
                    val orderId = doc.id
                    val currentRefundStatus = doc.getString("refund_status")
                    
                    // Skip if already has refund_status
                    if (currentRefundStatus != null) {
                        stats.skipped++
                        continue
                    }
                    
                    // Check if order has a completed refund
                    val refundSnapshot = firestore.collection(REFUNDS_COLLECTION)
                        .whereEqualTo("order_id", orderId)
                        .whereEqualTo("status", "completed")
                        .get()
                        .await()
                    
                    val newRefundStatus = if (refundSnapshot.documents.isNotEmpty()) {
                        OrderRefundStatus.COMPLETED.toString()
                    } else {
                        OrderRefundStatus.NONE.toString()
                    }
                    
                    batch.update(
                        firestore.collection(ORDERS_COLLECTION).document(orderId),
                        mapOf("refund_status" to newRefundStatus)
                    )
                    
                    batchCount++
                    stats.updated++
                }
                
                // Commit batch if there are updates
                if (batchCount > 0) {
                    batch.commit().await()
                    Log.d(TAG, "Committed batch of $batchCount orders")
                }
                
                lastDocumentSnapshot = snapshot.documents.lastOrNull()
                
                // If we got fewer documents than batch size, we're done
                if (snapshot.documents.size < BATCH_SIZE) break
            }
            
            Log.d(TAG, "Migration complete. Updated: ${stats.updated}, Skipped: ${stats.skipped}")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Verify migration by checking a sample of orders.
     */
    suspend fun verifyMigration(): Result<VerificationStats> {
        return try {
            Log.d(TAG, "Verifying migration...")
            
            val stats = VerificationStats()
            
            // Check orders without refund_status
            val missingSnapshot = firestore.collection(ORDERS_COLLECTION)
                .whereEqualTo("refund_status", null)
                .limit(10)
                .get()
                .await()
            
            stats.ordersWithoutRefundStatus = missingSnapshot.documents.size
            
            // Check total orders
            val totalSnapshot = firestore.collection(ORDERS_COLLECTION)
                .limit(1000)
                .get()
                .await()
            
            stats.totalOrdersChecked = totalSnapshot.documents.size
            
            // Check orders with refund_status
            val withRefundSnapshot = firestore.collection(ORDERS_COLLECTION)
                .whereNotEqualTo("refund_status", null)
                .limit(1000)
                .get()
                .await()
            
            stats.ordersWithRefundStatus = withRefundSnapshot.documents.size
            
            Log.d(TAG, "Verification: Total=${stats.totalOrdersChecked}, " +
                    "With refund_status=${stats.ordersWithRefundStatus}, " +
                    "Without=${stats.ordersWithoutRefundStatus}")
            
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Verification failed", e)
            Result.failure(e)
        }
    }

    data class MigrationStats(
        var updated: Int = 0,
        var skipped: Int = 0
    )

    data class VerificationStats(
        var totalOrdersChecked: Int = 0,
        var ordersWithRefundStatus: Int = 0,
        var ordersWithoutRefundStatus: Int = 0
    )
}
