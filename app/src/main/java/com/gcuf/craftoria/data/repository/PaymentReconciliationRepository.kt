package com.gcuf.craftoria.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log
import kotlinx.coroutines.tasks.await

data class PaymentReconciliation(
    var id: String = "",
    var paymentId: String = "",
    var orderId: String = "",
    var expectedAmount: Double = 0.0,
    var actualAmount: Double = 0.0,
    var discrepancy: Double = 0.0,
    var status: String = "pending", // pending, resolved, escalated
    var notes: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var resolvedAt: Long? = null
)

class PaymentReconciliationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val reconciliationCollection = db.collection("payment_reconciliation")
    
    companion object {
        private const val TAG = "PaymentReconciliationRepo"
    }

    suspend fun createReconciliation(
        paymentId: String,
        orderId: String,
        expectedAmount: Double,
        actualAmount: Double,
        notes: String = ""
    ): Result<String> {
        return try {
            Log.d(TAG, "🔄 Creating reconciliation for payment: $paymentId")

            val discrepancy = expectedAmount - actualAmount
            val reconciliation = PaymentReconciliation(
                paymentId = paymentId,
                orderId = orderId,
                expectedAmount = expectedAmount,
                actualAmount = actualAmount,
                discrepancy = discrepancy,
                status = if (discrepancy == 0.0) "resolved" else "pending",
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            val doc = reconciliationCollection.add(reconciliation.toMap()).await()
            val reconciliationId = doc.id

            // Update with ID
            reconciliationCollection.document(reconciliationId)
                .update("id", reconciliationId).await()

            if (discrepancy == 0.0) {
                Log.d(TAG, "✅ Reconciliation created (no discrepancy): $reconciliationId")
            } else {
                Log.w(TAG, "⚠️ Reconciliation created with discrepancy: $discrepancy")
            }

            Result.success(reconciliationId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create reconciliation", e)
            Result.failure(e)
        }
    }

    suspend fun resolveReconciliation(
        reconciliationId: String,
        resolution: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Resolving reconciliation: $reconciliationId")

            reconciliationCollection.document(reconciliationId).update(
                mapOf(
                    "status" to "resolved",
                    "notes" to resolution,
                    "resolvedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Reconciliation resolved: $reconciliationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to resolve reconciliation", e)
            Result.failure(e)
        }
    }

    suspend fun escalateReconciliation(
        reconciliationId: String,
        escalationReason: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Escalating reconciliation: $reconciliationId")

            reconciliationCollection.document(reconciliationId).update(
                mapOf(
                    "status" to "escalated",
                    "notes" to escalationReason
                )
            ).await()

            Log.d(TAG, "✅ Reconciliation escalated: $reconciliationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to escalate reconciliation", e)
            Result.failure(e)
        }
    }

    suspend fun getReconciliation(reconciliationId: String): Result<PaymentReconciliation> {
        return try {
            val doc = reconciliationCollection.document(reconciliationId).get().await()
            val reconciliation = doc.toObject(PaymentReconciliation::class.java)
                ?: return Result.failure(Exception("Reconciliation not found"))
            Result.success(reconciliation)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get reconciliation", e)
            Result.failure(e)
        }
    }

    suspend fun getReconciliationsForPayment(paymentId: String): Result<List<PaymentReconciliation>> {
        return try {
            val reconciliations = reconciliationCollection
                .whereEqualTo("paymentId", paymentId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(PaymentReconciliation::class.java)

            Log.d(TAG, "✅ Retrieved ${reconciliations.size} reconciliations for payment: $paymentId")
            Result.success(reconciliations)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get reconciliations for payment", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingReconciliations(): Result<List<PaymentReconciliation>> {
        return try {
            val reconciliations = reconciliationCollection
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(PaymentReconciliation::class.java)

            Log.d(TAG, "✅ Retrieved ${reconciliations.size} pending reconciliations")
            Result.success(reconciliations)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get pending reconciliations", e)
            Result.failure(e)
        }
    }

    suspend fun getEscalatedReconciliations(): Result<List<PaymentReconciliation>> {
        return try {
            val reconciliations = reconciliationCollection
                .whereEqualTo("status", "escalated")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(PaymentReconciliation::class.java)

            Log.d(TAG, "✅ Retrieved ${reconciliations.size} escalated reconciliations")
            Result.success(reconciliations)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get escalated reconciliations", e)
            Result.failure(e)
        }
    }

    suspend fun getReconciliationsByStatus(status: String): Result<List<PaymentReconciliation>> {
        return try {
            val reconciliations = reconciliationCollection
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(PaymentReconciliation::class.java)

            Log.d(TAG, "✅ Retrieved ${reconciliations.size} reconciliations with status: $status")
            Result.success(reconciliations)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get reconciliations by status", e)
            Result.failure(e)
        }
    }

    private fun PaymentReconciliation.toMap(): Map<String, Any> {
        return mapOf(
            "paymentId" to paymentId,
            "orderId" to orderId,
            "expectedAmount" to expectedAmount,
            "actualAmount" to actualAmount,
            "discrepancy" to discrepancy,
            "status" to status,
            "notes" to notes,
            "createdAt" to createdAt,
            "resolvedAt" to (resolvedAt ?: 0L)
        )
    }
}
