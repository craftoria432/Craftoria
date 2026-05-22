package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ✅ PRODUCTION-READY: Payment Data Migration Utility
 * 
 * This utility handles migration of existing payment records to include
 * the new `involved_seller_ids` field for access control.
 * 
 * This ensures backward compatibility with existing data while enforcing
 * the new security model for all payments (past and future).
 */
object PaymentDataMigration {
    private const val TAG = "PaymentDataMigration"
    private val db = FirebaseFirestore.getInstance()
    private val paymentsCollection = db.collection("seller_payments")

    /**
     * Migrate existing payments to include involved_seller_ids
     * 
     * This should be called once during app initialization or as a background task.
     * It's safe to call multiple times - it only updates records that need migration.
     */
    suspend fun migrateExistingPayments(): Result<Int> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔄 Starting payment data migration...")
            
            // Fetch all payments that don't have involved_seller_ids
            val snapshot = paymentsCollection
                .whereEqualTo("involved_seller_ids", null)
                .get()
                .await()

            val documentsToUpdate = snapshot.documents.filter { doc ->
                // Check if field doesn't exist or is empty
                val involvedIds = doc.get("involved_seller_ids")
                involvedIds == null || (involvedIds is List<*> && (involvedIds as List<*>).isEmpty())
            }

            Log.d(TAG, "📊 Found ${documentsToUpdate.size} payments to migrate")

            var migratedCount = 0

            documentsToUpdate.forEach { doc ->
                try {
                    val sellerId = doc.getString("seller_id") ?: ""
                    val orderId = doc.getString("order_id") ?: ""

                    if (sellerId.isEmpty()) {
                        Log.w(TAG, "⚠️ Skipping payment ${doc.id} - no seller_id")
                        return@forEach
                    }

                    // Get all payments for this order to determine all involved sellers
                    val orderPaymentsSnapshot = paymentsCollection
                        .whereEqualTo("order_id", orderId)
                        .get()
                        .await()

                    val involvedSellerIds = orderPaymentsSnapshot.documents
                        .mapNotNull { it.getString("seller_id") }
                        .distinct()

                    Log.d(TAG, "📝 Migrating payment ${doc.id}")
                    Log.d(TAG, "   Order: $orderId")
                    Log.d(TAG, "   Seller: $sellerId")
                    Log.d(TAG, "   Involved sellers: $involvedSellerIds")

                    // Update the payment with involved_seller_ids
                    paymentsCollection.document(doc.id).update(
                        mapOf(
                            "involved_seller_ids" to involvedSellerIds,
                            "migrated_at" to System.currentTimeMillis()
                        )
                    ).await()

                    migratedCount++
                    Log.d(TAG, "✅ Payment ${doc.id} migrated successfully")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error migrating payment ${doc.id}", e)
                }
            }

            Log.d(TAG, "✅ Migration complete: $migratedCount payments updated")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(migratedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Verify migration status
     * Returns the count of payments that still need migration
     */
    suspend fun getUnmigratedPaymentCount(): Result<Int> {
        return try {
            val snapshot = paymentsCollection
                .whereEqualTo("involved_seller_ids", null)
                .get()
                .await()

            val unmigratedCount = snapshot.documents.count { doc ->
                val involvedIds = doc.get("involved_seller_ids")
                involvedIds == null || (involvedIds is List<*> && (involvedIds as List<*>).isEmpty())
            }

            Result.success(unmigratedCount)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unmigrated payment count", e)
            Result.failure(e)
        }
    }

    /**
     * Fix existing payments where coSellerStoreId was incorrectly set to sellerId
     * Run this ONCE to clean up bad data created by the old PaymentRepository bug
     */
    suspend fun fixCoSellerStoreIdField(): Result<Int> {
        return try {
            Log.d(TAG, "🔄 Fixing co_seller_store_id field for regular seller payments...")

            val snapshot = paymentsCollection.get().await()
            var fixedCount = 0

            snapshot.documents.forEach { doc ->
                val sellerId = doc.getString("seller_id") ?: ""
                val coSellerStoreId = doc.getString("co_seller_store_id") ?: ""

                // If coSellerStoreId equals sellerId, it was set incorrectly — clear it
                if (coSellerStoreId.isNotEmpty() && coSellerStoreId == sellerId) {
                    try {
                        paymentsCollection.document(doc.id)
                            .update("co_seller_store_id", "")
                            .await()
                        fixedCount++
                        Log.d(TAG, "✅ Fixed payment ${doc.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to fix payment ${doc.id}", e)
                    }
                }
            }

            Log.d(TAG, "✅ Fixed $fixedCount payments")
            Result.success(fixedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fix failed", e)
            Result.failure(e)
        }
    }

    /**
     * Force migrate a specific payment
     * Useful for testing or manual fixes
     */
    suspend fun migrateSpecificPayment(paymentId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Migrating specific payment: $paymentId")

            val doc = paymentsCollection.document(paymentId).get().await()
            val sellerId = doc.getString("seller_id") ?: ""
            val orderId = doc.getString("order_id") ?: ""

            if (sellerId.isEmpty() || orderId.isEmpty()) {
                return Result.failure(Exception("Invalid payment data"))
            }

            // Get all payments for this order
            val orderPaymentsSnapshot = paymentsCollection
                .whereEqualTo("order_id", orderId)
                .get()
                .await()

            val involvedSellerIds = orderPaymentsSnapshot.documents
                .mapNotNull { it.getString("seller_id") }
                .distinct()

            paymentsCollection.document(paymentId).update(
                mapOf(
                    "involved_seller_ids" to involvedSellerIds,
                    "migrated_at" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Payment $paymentId migrated successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to migrate payment $paymentId", e)
            Result.failure(e)
        }
    }

    /**
     * Migrate existing co-seller stores that have no paymentSplitConfig set.
     * Sets equal splits based on current memberIds.
     * Safe to call multiple times — only updates stores with empty config.
     */
    suspend fun migrateStorePaymentSplits(): Result<Int> {
        return try {
            Log.d(TAG, "🔄 Migrating store payment split configs...")

            val storesCollection = db.collection("co_seller_stores")
            val snapshot = storesCollection.get().await()
            var migratedCount = 0

            snapshot.documents.forEach { doc ->
                try {
                    val memberIds = doc.get("member_ids") as? List<*>
                        ?: listOf(doc.getString("owner_id") ?: return@forEach)

                    val existingConfig = doc.get("payment_split_config") as? Map<*, *>

                    // Only migrate if config is missing or empty
                    if (existingConfig.isNullOrEmpty()) {
                        val memberIdStrings = memberIds.mapNotNull { it?.toString() }
                        if (memberIdStrings.isEmpty()) return@forEach

                        val equalShare = 1.0 / memberIdStrings.size
                        val splitConfig = memberIdStrings.associateWith { equalShare }

                        storesCollection.document(doc.id).update(
                            "payment_split_config", splitConfig
                        ).await()

                        migratedCount++
                        Log.d(TAG, "✅ Migrated store ${doc.id}: ${memberIdStrings.size} members × ${equalShare * 100}% each")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to migrate store ${doc.id}", e)
                }
            }

            Log.d(TAG, "✅ Store split migration complete: $migratedCount stores updated")
            Result.success(migratedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Store split migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Migrate existing orders to populate coSellerStoreId field.
     * Looks up each order's product and copies the coSellerStoreId from the product.
     * Safe to call multiple times — only updates orders with empty coSellerStoreId.
     * 
     * This fixes the issue where existing orders don't show the co-seller store badge
     * because they were created before the coSellerStoreId field was added to orders.
     */
    suspend fun migrateOrderCoSellerStoreIds(): Result<Int> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔄 Migrating order co_seller_store_id field...")

            val ordersCollection = db.collection("orders")
            val productsCollection = db.collection("products")
            
            // Get all orders that don't have coSellerStoreId set
            val snapshot = ordersCollection.get().await()
            var migratedCount = 0
            var skippedCount = 0

            snapshot.documents.forEach { orderDoc ->
                try {
                    val coSellerStoreId = orderDoc.getString("co_seller_store_id") ?: ""
                    
                    // Only migrate if coSellerStoreId is empty
                    if (coSellerStoreId.isEmpty()) {
                        val productId = orderDoc.getString("product_id") ?: ""
                        
                        if (productId.isEmpty()) {
                            Log.w(TAG, "⚠️ Skipping order ${orderDoc.id} - no product_id")
                            skippedCount++
                            return@forEach
                        }

                        // Look up the product to get its coSellerStoreId
                        val productDoc = productsCollection.document(productId).get().await()
                        
                        if (!productDoc.exists()) {
                            Log.w(TAG, "⚠️ Skipping order ${orderDoc.id} - product $productId not found")
                            skippedCount++
                            return@forEach
                        }

                        val productStoreId = productDoc.getString("co_seller_store_id") ?: ""
                        
                        if (productStoreId.isNotEmpty()) {
                            // Update the order with the store ID from the product
                            ordersCollection.document(orderDoc.id).update(
                                mapOf(
                                    "co_seller_store_id" to productStoreId,
                                    "store_id_migrated_at" to System.currentTimeMillis()
                                )
                            ).await()

                            migratedCount++
                            Log.d(TAG, "✅ Order ${orderDoc.id} migrated: storeId=$productStoreId")
                        } else {
                            // Product doesn't have a store ID (regular seller product)
                            skippedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to migrate order ${orderDoc.id}", e)
                }
            }

            Log.d(TAG, "✅ Order migration complete:")
            Log.d(TAG, "   - Migrated: $migratedCount orders")
            Log.d(TAG, "   - Skipped: $skippedCount orders (regular seller products)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(migratedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Order migration failed", e)
            Result.failure(e)
        }
    }
}
