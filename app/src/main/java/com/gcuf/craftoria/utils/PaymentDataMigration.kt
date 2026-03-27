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
}
