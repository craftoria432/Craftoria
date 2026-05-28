package com.gcuf.craftoria.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.RefundStatus
import kotlinx.coroutines.tasks.await

/**
 * One-time migration to update old refunds stuck in PROCESSING state to COMPLETED.
 *
 * Background:
 * - Old refunds were created before the auto-complete logic was added to approveRefund()
 * - They're stuck in PROCESSING state because completeRefund() was never called
 * - This migration finds all PROCESSING refunds and marks them as COMPLETED
 *
 * This should be called once during app initialization (e.g., in MainActivity or a startup service).
 */
object RefundStatusMigration {
    private const val TAG = "RefundStatusMigration"
    private const val PREFS_NAME = "craftoria_migrations"
    private const val MIGRATION_KEY = "refund_status_migration_v1_completed"

    suspend fun migrateOldRefunds(context: Context, firestore: FirebaseFirestore): Boolean {
        return try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            if (sharedPrefs.getBoolean(MIGRATION_KEY, false)) {
                Log.d(TAG, "ℹ️ Migration already completed, skipping")
                return true
            }

            Log.d(TAG, "🔄 Starting refund status migration...")
            Log.d(TAG, "   Looking for refunds with status = '${RefundStatus.PROCESSING.toString()}'")

            // Find all PROCESSING refunds
            val snapshot = firestore.collection("refunds")
                .whereEqualTo("status", RefundStatus.PROCESSING.toString())
                .get()
                .await()

            if (snapshot.documents.isEmpty()) {
                Log.d(TAG, "✅ No PROCESSING refunds found, migration complete")
                sharedPrefs.edit().putBoolean(MIGRATION_KEY, true).apply()
                return true
            }

            Log.d(TAG, "🔍 Found ${snapshot.documents.size} PROCESSING refunds to migrate")

            val now = System.currentTimeMillis()
            var successCount = 0
            var failureCount = 0

            // Update each PROCESSING refund to COMPLETED
            for (doc in snapshot.documents) {
                try {
                    val refundId = doc.id
                    val orderId = doc.getString("order_id") ?: "unknown"
                    val paymentId = doc.getString("payment_id") ?: "unknown"
                    val completedAt = doc.getLong("completed_at")
                        ?.takeIf { it > 0L } ?: now

                    Log.d(TAG, "   📝 Migrating refund: $refundId (order: $orderId, payment: $paymentId)")

                    // Update refund to COMPLETED
                    firestore.collection("refunds").document(refundId).update(
                        mapOf(
                            "status"       to RefundStatus.COMPLETED.toString(),
                            "completed_at" to completedAt,
                            "updated_at"   to now
                        )
                    ).await()
                    Log.d(TAG, "      ✅ Refund status updated to COMPLETED")

                    // Update associated payment to REFUNDED
                    val paymentIdStr = doc.getString("payment_id")
                    if (!paymentIdStr.isNullOrEmpty()) {
                        val refundAmount = (doc.get("refund_amount") as? Number)?.toDouble() ?: 0.0
                        val refundReason = doc.getString("reason") ?: ""
                        // ✅ CRITICAL: Using correct collection name "payments" (not "seller_payments")
                        firestore.collection("payments").document(paymentIdStr).update(
                            mapOf(
                                "status"        to "refunded",
                                "refund_amount" to refundAmount,
                                "refund_reason" to refundReason,
                                "refund_date"   to completedAt,
                                "updated_at"    to now
                            )
                        ).await()
                        Log.d(TAG, "      ✅ Payment status updated to 'refunded'")
                    }

                    // Mark order as refunded
                    val orderIdStr = doc.getString("order_id")
                    if (!orderIdStr.isNullOrEmpty()) {
                        firestore.collection("orders").document(orderIdStr).update(
                            mapOf(
                                "is_refunded" to true,
                                "updated_at"  to now
                            )
                        ).await()
                        Log.d(TAG, "      ✅ Order marked as refunded")
                    }

                    successCount++
                    Log.d(TAG, "   ✅ Successfully migrated refund: $refundId")
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "   ❌ Failed to migrate refund ${doc.id}: ${e.message}", e)
                }
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "Migration complete: $successCount succeeded, $failureCount failed")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Only mark as done if all succeeded — allows retry on next launch if some failed
            if (failureCount == 0) {
                sharedPrefs.edit().putBoolean(MIGRATION_KEY, true).apply()
            }

            return failureCount == 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed: ${e.message}", e)
            return false
        }
    }
}
