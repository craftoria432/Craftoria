package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Migration utility to populate missing coSellerStoreId in orders
 * 
 * This fixes orders that were created before the co-seller store system
 * was fully implemented, or from products that didn't have store IDs set.
 */
object OrderStoreMigration {
    private const val TAG = "OrderStoreMigration"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Migrate orders for a specific seller to populate missing coSellerStoreId
     * 
     * @param sellerId The seller whose orders need migration
     * @return Result with count of migrated orders
     */
    suspend fun migrateSellerOrders(sellerId: String): Result<Int> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔄 Starting order store migration for seller: $sellerId")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Get all orders for this seller where coSellerStoreId is empty
            val ordersSnapshot = db.collection("orders")
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()

            var migratedCount = 0
            val batch = db.batch()
            var batchCount = 0

            for (orderDoc in ordersSnapshot.documents) {
                val coSellerStoreId = orderDoc.getString("co_seller_store_id") ?: ""
                val productId = orderDoc.getString("product_id") ?: ""

                // Skip if already has store ID or no product ID
                if (coSellerStoreId.isNotEmpty() || productId.isEmpty()) {
                    continue
                }

                Log.d(TAG, "📦 Processing order: ${orderDoc.id}")
                Log.d(TAG, "   Product ID: $productId")

                // Look up the product to get its coSellerStoreId
                try {
                    val productDoc = db.collection("products")
                        .document(productId)
                        .get()
                        .await()

                    if (productDoc.exists()) {
                        val productStoreId = productDoc.getString("co_seller_store_id") ?: ""
                        
                        if (productStoreId.isNotEmpty()) {
                            // Update the order with the store ID
                            batch.update(
                                orderDoc.reference,
                                mapOf(
                                    "co_seller_store_id" to productStoreId,
                                    "updated_at" to System.currentTimeMillis()
                                )
                            )
                            
                            batchCount++
                            migratedCount++
                            
                            Log.d(TAG, "   ✅ Will update with store ID: $productStoreId")

                            // Commit batch every 500 operations (Firestore limit)
                            if (batchCount >= 500) {
                                batch.commit().await()
                                Log.d(TAG, "   💾 Committed batch of $batchCount updates")
                                batchCount = 0
                            }
                        } else {
                            Log.d(TAG, "   ⚠️ Product has no store ID")
                        }
                    } else {
                        Log.d(TAG, "   ⚠️ Product not found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Error processing product: ${e.message}")
                }
            }

            // Commit remaining batch
            if (batchCount > 0) {
                batch.commit().await()
                Log.d(TAG, "💾 Committed final batch of $batchCount updates")
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "✅ Migration complete: $migratedCount orders updated")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(migratedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Migrate all orders in the system (admin function)
     * Use with caution - this processes ALL orders
     */
    suspend fun migrateAllOrders(): Result<Int> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔄 Starting GLOBAL order store migration")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Get all orders where coSellerStoreId is empty
            val ordersSnapshot = db.collection("orders")
                .get()
                .await()

            var migratedCount = 0
            val batch = db.batch()
            var batchCount = 0

            for (orderDoc in ordersSnapshot.documents) {
                val coSellerStoreId = orderDoc.getString("co_seller_store_id") ?: ""
                val productId = orderDoc.getString("product_id") ?: ""

                // Skip if already has store ID or no product ID
                if (coSellerStoreId.isNotEmpty() || productId.isEmpty()) {
                    continue
                }

                // Look up the product to get its coSellerStoreId
                try {
                    val productDoc = db.collection("products")
                        .document(productId)
                        .get()
                        .await()

                    if (productDoc.exists()) {
                        val productStoreId = productDoc.getString("co_seller_store_id") ?: ""
                        
                        if (productStoreId.isNotEmpty()) {
                            // Update the order with the store ID
                            batch.update(
                                orderDoc.reference,
                                mapOf(
                                    "co_seller_store_id" to productStoreId,
                                    "updated_at" to System.currentTimeMillis()
                                )
                            )
                            
                            batchCount++
                            migratedCount++

                            // Commit batch every 500 operations
                            if (batchCount >= 500) {
                                batch.commit().await()
                                Log.d(TAG, "💾 Committed batch of $batchCount updates")
                                batchCount = 0
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing order ${orderDoc.id}: ${e.message}")
                }
            }

            // Commit remaining batch
            if (batchCount > 0) {
                batch.commit().await()
                Log.d(TAG, "💾 Committed final batch of $batchCount updates")
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "✅ Global migration complete: $migratedCount orders updated")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(migratedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Global migration failed", e)
            Result.failure(e)
        }
    }
}
