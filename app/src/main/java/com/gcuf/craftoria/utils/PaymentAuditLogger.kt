package com.gcuf.craftoria.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.util.Log
import kotlinx.coroutines.tasks.await

data class PaymentAuditLog(
    var id: String = "",
    var paymentId: String = "",
    var orderId: String = "",
    var action: String = "", // created, updated, refunded, etc.
    var actorId: String = "",
    var actorType: String = "", // system, user, admin
    var oldValue: Map<String, Any> = emptyMap(),
    var newValue: Map<String, Any> = emptyMap(),
    var details: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

class PaymentAuditLogger(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val auditCollection = db.collection("payment_audit_logs")
    
    companion object {
        private const val TAG = "PaymentAuditLogger"
    }

    suspend fun logPaymentAction(
        paymentId: String,
        orderId: String,
        action: String,
        actorId: String,
        actorType: String = "system",
        oldValue: Map<String, Any> = emptyMap(),
        newValue: Map<String, Any> = emptyMap(),
        details: String = ""
    ): Result<String> {
        return try {
            val auditLog = PaymentAuditLog(
                paymentId = paymentId,
                orderId = orderId,
                action = action,
                actorId = actorId,
                actorType = actorType,
                oldValue = oldValue,
                newValue = newValue,
                details = details,
                timestamp = System.currentTimeMillis()
            )

            val doc = auditCollection.add(auditLog.toMap()).await()
            Log.d(TAG, "✅ Audit log created: ${doc.id} for action: $action")
            Result.success(doc.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create audit log", e)
            Result.failure(e)
        }
    }

    suspend fun getPaymentAuditTrail(paymentId: String): Result<List<PaymentAuditLog>> {
        return try {
            val logs = auditCollection
                .whereEqualTo("paymentId", paymentId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(PaymentAuditLog::class.java)

            Log.d(TAG, "✅ Retrieved ${logs.size} audit logs for payment: $paymentId")
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to retrieve audit trail", e)
            Result.failure(e)
        }
    }

    suspend fun getOrderAuditTrail(orderId: String): Result<List<PaymentAuditLog>> {
        return try {
            val logs = auditCollection
                .whereEqualTo("orderId", orderId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(PaymentAuditLog::class.java)

            Log.d(TAG, "✅ Retrieved ${logs.size} audit logs for order: $orderId")
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to retrieve order audit trail", e)
            Result.failure(e)
        }
    }

    suspend fun logPaymentCreated(
        paymentId: String,
        orderId: String,
        paymentData: Map<String, Any>,
        actorId: String = "system"
    ): Result<String> {
        return logPaymentAction(
            paymentId = paymentId,
            orderId = orderId,
            action = "CREATED",
            actorId = actorId,
            actorType = "system",
            newValue = paymentData,
            details = "Payment created for order"
        )
    }

    suspend fun logPaymentUpdated(
        paymentId: String,
        orderId: String,
        oldValue: Map<String, Any>,
        newValue: Map<String, Any>,
        actorId: String = "system"
    ): Result<String> {
        return logPaymentAction(
            paymentId = paymentId,
            orderId = orderId,
            action = "UPDATED",
            actorId = actorId,
            actorType = "system",
            oldValue = oldValue,
            newValue = newValue,
            details = "Payment updated"
        )
    }

    suspend fun logRefundInitiated(
        paymentId: String,
        orderId: String,
        refundAmount: Double,
        reason: String,
        actorId: String
    ): Result<String> {
        return logPaymentAction(
            paymentId = paymentId,
            orderId = orderId,
            action = "REFUND_INITIATED",
            actorId = actorId,
            actorType = "user",
            newValue = mapOf("refund_amount" to refundAmount, "reason" to reason),
            details = "Refund initiated: $refundAmount for reason: $reason"
        )
    }

    suspend fun logRefundProcessed(
        paymentId: String,
        orderId: String,
        refundAmount: Double,
        transactionId: String,
        actorId: String = "system"
    ): Result<String> {
        return logPaymentAction(
            paymentId = paymentId,
            orderId = orderId,
            action = "REFUND_PROCESSED",
            actorId = actorId,
            actorType = "system",
            newValue = mapOf("refund_amount" to refundAmount, "transaction_id" to transactionId),
            details = "Refund processed with transaction: $transactionId"
        )
    }

    suspend fun logRefundApproved(
        paymentId: String,
        orderId: String,
        refundAmount: Double,
        approverName: String,
        actorId: String
    ): Result<String> {
        return logPaymentAction(
            paymentId = paymentId,
            orderId = orderId,
            action = "REFUND_APPROVED",
            actorId = actorId,
            actorType = "admin",
            newValue = mapOf("refund_amount" to refundAmount, "approved_by" to approverName),
            details = "Refund approved by $approverName"
        )
    }

    suspend fun logRefundCancelled(
        paymentId: String,
        orderId: String,
        refundAmount: Double,
        reason: String,
        actorId: String
    ): Result<String> {
        return logPaymentAction(
            paymentId = paymentId,
            orderId = orderId,
            action = "REFUND_CANCELLED",
            actorId = actorId,
            actorType = "admin",
            newValue = mapOf("refund_amount" to refundAmount, "cancellation_reason" to reason),
            details = "Refund cancelled: $reason"
        )
    }

    private fun PaymentAuditLog.toMap(): Map<String, Any> {
        return mapOf(
            "paymentId" to paymentId,
            "orderId" to orderId,
            "action" to action,
            "actorId" to actorId,
            "actorType" to actorType,
            "oldValue" to oldValue,
            "newValue" to newValue,
            "details" to details,
            "timestamp" to timestamp
        )
    }
}
