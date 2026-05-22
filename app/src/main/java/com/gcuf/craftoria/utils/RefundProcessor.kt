package com.gcuf.craftoria.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.getDeliveredAtLong
import android.util.Log
import com.gcuf.craftoria.data.model.PaymentStatus
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.math.BigDecimal
import java.math.RoundingMode

// ==================== ENUMS ====================
enum class RefundStatus {
    REQUESTED,
    APPROVED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    DISPUTED;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        REQUESTED -> "Requested"
        APPROVED -> "Approved"
        PROCESSING -> "Processing"
        COMPLETED -> "Completed"
        FAILED -> "Failed"
        CANCELLED -> "Cancelled"
        DISPUTED -> "Disputed"
    }

    fun getStatusColor(): String = when (this) {
        REQUESTED -> "#FFA500"
        APPROVED -> "#4169E1"
        PROCESSING -> "#1E90FF"
        COMPLETED -> "#28A745"
        FAILED -> "#DC3545"
        CANCELLED -> "#6C757D"
        DISPUTED -> "#FF6347"
    }
}

enum class RefundReason {
    BUYER_REQUEST,
    SELLER_INITIATED,
    ORDER_CANCELLED,
    PRODUCT_DEFECTIVE,
    PRODUCT_NOT_RECEIVED,
    WRONG_PRODUCT,
    CHARGEBACK,
    PAYMENT_ERROR,
    DUPLICATE_PAYMENT,
    OTHER;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        BUYER_REQUEST -> "Buyer Request"
        SELLER_INITIATED -> "Seller Initiated"
        ORDER_CANCELLED -> "Order Cancelled"
        PRODUCT_DEFECTIVE -> "Product Defective"
        PRODUCT_NOT_RECEIVED -> "Product Not Received"
        WRONG_PRODUCT -> "Wrong Product"
        CHARGEBACK -> "Chargeback"
        PAYMENT_ERROR -> "Payment Error"
        DUPLICATE_PAYMENT -> "Duplicate Payment"
        OTHER -> "Other"
    }
}

// ==================== DATA MODELS ====================
@IgnoreExtraProperties
data class RefundRecord(
    var id: String = "",
    @get:PropertyName("payment_id") @set:PropertyName("payment_id") var paymentId: String = "",
    @get:PropertyName("order_id") @set:PropertyName("order_id") var orderId: String = "",
    @get:PropertyName("seller_id") @set:PropertyName("seller_id") var sellerId: String = "",
    @get:PropertyName("buyer_id") @set:PropertyName("buyer_id") var buyerId: String = "",
    @get:PropertyName("refund_amount") @set:PropertyName("refund_amount") var refundAmount: Double = 0.0,
    @get:PropertyName("original_amount") @set:PropertyName("original_amount") var originalAmount: Double = 0.0,
    @get:PropertyName("reason") @set:PropertyName("reason") var reason: String = RefundReason.OTHER.toString(),
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("requested_by") @set:PropertyName("requested_by") var requestedBy: String = "",
    @get:PropertyName("approved_by") @set:PropertyName("approved_by") var approvedBy: String? = null,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = RefundStatus.REQUESTED.toString(),
    @get:PropertyName("transaction_id") @set:PropertyName("transaction_id") var transactionId: String = "",
    @get:PropertyName("payment_method") @set:PropertyName("payment_method") var paymentMethod: String = "",
    @get:PropertyName("refund_splits") @set:PropertyName("refund_splits") var refundSplits: List<RefundSplit> = emptyList(),
    @get:PropertyName("retry_count") @set:PropertyName("retry_count") var retryCount: Int = 0,
    @get:PropertyName("max_retries") @set:PropertyName("max_retries") var maxRetries: Int = 3,
    @get:PropertyName("last_retry_at") @set:PropertyName("last_retry_at") var lastRetryAt: Long? = null,
    @get:PropertyName("error_message") @set:PropertyName("error_message") var errorMessage: String = "",
    @get:PropertyName("created_at") @set:PropertyName("created_at") var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("approved_at") @set:PropertyName("approved_at") var approvedAt: Long? = null,
    @get:PropertyName("processed_at") @set:PropertyName("processed_at") var processedAt: Long? = null,
    @get:PropertyName("updated_at") @set:PropertyName("updated_at") var updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("idempotency_key") @set:PropertyName("idempotency_key") var idempotencyKey: String = ""
)

data class RefundSplit(
    @get:PropertyName("seller_id") @set:PropertyName("seller_id") var sellerId: String = "",
    @get:PropertyName("seller_name") @set:PropertyName("seller_name") var sellerName: String = "",
    @get:PropertyName("refund_amount") @set:PropertyName("refund_amount") var refundAmount: Double = 0.0,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = RefundStatus.REQUESTED.toString()
)

// ==================== REFUND PROCESSOR ====================
class RefundProcessor(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    // ✅ CRITICAL: Using correct collection name "payments" (not "seller_payments")
    private val paymentsCollection = db.collection("payments")
    private val refundsCollection = db.collection("refunds")
    private val auditLogger = PaymentAuditLogger(db)
    // Note: PaymentRetryManager (transient in-process retries with backoff) is intentionally
    // not used here. Refund retries are persistent — tracked via retry_count in Firestore
    // and re-queued through retryFailedRefund(). The two strategies don't overlap.

    companion object {
        private const val TAG = "RefundProcessor"
        private const val REFUND_WINDOW_DAYS = 30
    }

    // ✅ Helper function to safely deserialize RefundRecord with Timestamp conversion
    // ✅ CRITICAL: Avoid toObject() entirely — it crashes on mixed Timestamp types
    // Read every field manually instead
    private fun deserializeRefundRecord(doc: com.google.firebase.firestore.DocumentSnapshot): RefundRecord? {
        return try {
            val data = doc.data ?: return null
            
            // ✅ Helper: Convert any timestamp representation to Long milliseconds
            fun tsLong(value: Any?): Long = when (value) {
                is Long -> value
                is com.google.firebase.Timestamp -> value.toDate().time
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
            
            @Suppress("UNCHECKED_CAST")
            val refundSplits = (data["refund_splits"] as? List<*>)?.mapNotNull { split ->
                (split as? Map<*, *>)?.let { m ->
                    RefundSplit(
                        sellerId            = m["seller_id"] as? String ?: "",
                        sellerName          = m["seller_name"] as? String ?: "",
                        refundAmount        = (m["refund_amount"] as? Number)?.toDouble() ?: 0.0,
                        status              = m["status"] as? String ?: RefundStatus.REQUESTED.toString()
                    )
                }
            } ?: emptyList()
            
            RefundRecord(
                id              = doc.id,
                paymentId       = doc.getString("payment_id") ?: "",
                orderId         = doc.getString("order_id") ?: "",
                sellerId        = doc.getString("seller_id") ?: "",
                buyerId         = doc.getString("buyer_id") ?: "",
                refundAmount    = (data["refund_amount"] as? Number)?.toDouble() ?: 0.0,
                originalAmount  = (data["original_amount"] as? Number)?.toDouble() ?: 0.0,
                reason          = doc.getString("reason") ?: RefundReason.OTHER.toString(),
                description     = doc.getString("description") ?: "",
                requestedBy     = doc.getString("requested_by") ?: "",
                approvedBy      = doc.getString("approved_by"),
                status          = doc.getString("status") ?: RefundStatus.REQUESTED.toString(),
                transactionId   = doc.getString("transaction_id") ?: "",
                paymentMethod   = doc.getString("payment_method") ?: "",
                refundSplits    = refundSplits,
                retryCount      = (data["retry_count"] as? Number)?.toInt() ?: 0,
                maxRetries      = (data["max_retries"] as? Number)?.toInt() ?: 3,
                lastRetryAt     = tsLong(data["last_retry_at"]).takeIf { it > 0L },
                errorMessage    = doc.getString("error_message") ?: "",
                createdAt       = tsLong(data["created_at"]).takeIf { it > 0L } ?: System.currentTimeMillis(),
                approvedAt      = tsLong(data["approved_at"]).takeIf { it > 0L },
                processedAt     = tsLong(data["processed_at"]).takeIf { it > 0L },
                updatedAt       = tsLong(data["updated_at"]).takeIf { it > 0L } ?: System.currentTimeMillis(),
                idempotencyKey  = doc.getString("idempotency_key") ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing RefundRecord", e)
            null
        }
    }

    // ==================== INITIATE REFUND ====================
    suspend fun initiateRefund(
        paymentId: String,
        refundAmount: Double,
        reason: String,
        description: String = "",
        requestedBy: String
    ): Result<String> {
        return try {
            Log.d(TAG, "🔄 Initiating refund for payment: $paymentId, amount: $refundAmount")

            val paymentDoc = paymentsCollection.document(paymentId).get().await()
            if (!paymentDoc.exists()) {
                return Result.failure(Exception("Payment not found"))
            }

            // ✅ FIX: Never call toObject(SellerPayment::class.java) — it crashes when
            // any timestamp field in Firestore is a Timestamp object but the Kotlin
            // data class declares it as Long. Read every field manually instead.
            fun tsLong(value: Any?): Long = when (value) {
                is Long -> value
                is com.google.firebase.Timestamp -> value.toDate().time
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }

            val data = paymentDoc.data ?: return Result.failure(Exception("Payment data is null"))
            val sellerId = paymentDoc.getString("seller_id") ?: ""
            val sellerName = paymentDoc.getString("seller_name") ?: ""
            val buyerId = paymentDoc.getString("buyer_id") ?: ""
            val orderId = paymentDoc.getString("order_id") ?: ""
            val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
            val paymentMethod = paymentDoc.getString("payment_method") ?: "Cash on Delivery"
            val status = paymentDoc.getString("status") ?: "pending"
            val paymentDate = tsLong(data["payment_date"]).takeIf { it > 0L }

            // Build a lightweight SellerPayment just for validation — no toObject()
            val payment = com.gcuf.craftoria.data.model.SellerPayment(
                id = paymentDoc.id,
                sellerId = sellerId,
                sellerName = sellerName,
                buyerId = buyerId,
                orderId = orderId,
                amount = amount,
                paymentMethod = paymentMethod,
                status = status,
                paymentDate = paymentDate,
                createdAt = tsLong(data["created_at"]).let { if (it > 0L) it else System.currentTimeMillis() },
                updatedAt = tsLong(data["updated_at"]).let { if (it > 0L) it else System.currentTimeMillis() }
            )

            // ✅ FIX: Read delivered_at manually (same Timestamp issue exists here)
            val orderDoc = db.collection("orders").document(payment.orderId).get().await()
            val deliveredAt: Long = if (orderDoc.exists()) {
                tsLong(orderDoc.get("delivered_at"))
            } else 0L

            // Fetch buyer name from users collection
            val buyerDoc = db.collection("users").document(payment.buyerId).get().await()
            val buyerName = buyerDoc.getString("name") ?: buyerDoc.getString("full_name") ?: "Unknown Buyer"

            val validation = validateRefundEligibility(payment, refundAmount, deliveredAt)
            if (!validation.isValid) {
                val errorMsg = validation.errors.joinToString(", ")
                Log.e(TAG, "❌ Refund validation failed: $errorMsg")
                return Result.failure(Exception(errorMsg))
            }

            // ✅ FIX 7: 30-day window check already done in validateRefundEligibility()
            // No need to check again here — the validation function handles it completely

            val refund = RefundRecord(
                paymentId = paymentId,
                orderId = payment.orderId,
                sellerId = payment.sellerId,
                buyerId = payment.buyerId,
                refundAmount = refundAmount,
                originalAmount = payment.amount,
                reason = reason,
                description = description,
                requestedBy = requestedBy,
                paymentMethod = payment.paymentMethod,
                status = RefundStatus.REQUESTED.toString(),
                refundSplits = calculateRefundSplits(payment, refundAmount),
                createdAt = System.currentTimeMillis()
            )

            val refundMap = refund.toMap(buyerName = buyerName, sellerName = sellerName)
            val refundDoc = refundsCollection.add(refundMap).await()
            val refundId = refundDoc.id
            refundsCollection.document(refundId).update("id", refundId).await()

            auditLogger.logRefundInitiated(
                paymentId = paymentId,
                orderId = payment.orderId,
                refundAmount = refundAmount,
                reason = reason,
                actorId = requestedBy
            )

            // ✅ FIX 6: CRITICAL - Notify seller of buyer-initiated refund request
            if (requestedBy == "buyer") {
                Log.d(TAG, "Notifying seller of buyer-initiated refund request...")
                // Seller needs to approve the buyer's refund request
                db.collection("notifications").add(
                    mapOf(
                        "user_id" to payment.sellerId,
                        "type" to "refund_requested",
                        "title" to "Refund Request Received",
                        "message" to "Buyer requested a refund of PKR $refundAmount for order ${payment.orderId}",
                        "order_id" to payment.orderId,
                        "payment_id" to paymentId,
                        "refund_id" to refundDoc.id,
                        "buyer_name" to buyerName,
                        "created_at" to System.currentTimeMillis(),
                        "read" to false
                    )
                ).await()
                Log.d(TAG, "✅ Seller notification sent for buyer refund request")
            }

            Log.d(TAG, "✅ Refund initiated: $refundId")
            Result.success(refundId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initiate refund", e)
            Result.failure(e)
        }
    }

    // ==================== APPROVE REFUND ====================
    suspend fun approveRefund(refundId: String, approvedBy: String): Result<Unit> {
        return try {
            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = deserializeRefundRecord(refundDoc)
                ?: return Result.failure(Exception("Refund not found"))

            if (refund.status != RefundStatus.REQUESTED.toString()) {
                return Result.failure(Exception("Only requested refunds can be approved"))
            }

            refundsCollection.document(refundId).update(
                mapOf(
                    "status" to RefundStatus.APPROVED.toString(),
                    "approved_by" to approvedBy,
                    "approved_at" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            auditLogger.logRefundApproved(
                paymentId = refund.paymentId,
                orderId = refund.orderId,
                refundAmount = refund.refundAmount,
                approverName = approvedBy,
                actorId = approvedBy
            )

            Log.d(TAG, "✅ Refund approved: $refundId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to approve refund", e)
            Result.failure(e)
        }
    }

    // ==================== PROCESS REFUND ====================
    suspend fun processRefund(
        refundId: String,
        transactionId: String = "",
        actorId: String = "system"
    ): Result<Unit> {
        return try {
            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = deserializeRefundRecord(refundDoc)
                ?: return Result.failure(Exception("Refund not found"))

            if (refund.status !in listOf(
                    RefundStatus.APPROVED.toString(),
                    RefundStatus.REQUESTED.toString()
                )
            ) {
                return Result.failure(Exception("Refund cannot be processed in ${refund.status} status"))
            }

            val finalTransactionId = transactionId.ifEmpty { UUID.randomUUID().toString() }
            
            // ✅ CRITICAL FIX: Use Firestore batch write for atomic updates
            // This ensures all three writes (refund, payment, order) succeed together or fail together
            // Prevents partial state where payment is REFUNDED but order's refund_status is still REQUESTED
            val batch = db.batch()
            
            // 1. Update refund to PROCESSING first
            refundsCollection.document(refundId).let { ref ->
                batch.update(ref, "status", RefundStatus.PROCESSING.toString())
                batch.update(ref, "transaction_id", finalTransactionId)
                batch.update(ref, "updated_at", System.currentTimeMillis())
            }
            
            // 2. Update payment to REFUNDED
            paymentsCollection.document(refund.paymentId).let { ref ->
                batch.update(ref, "status", PaymentStatus.REFUNDED.toString())
                batch.update(ref, "refund_amount", refund.refundAmount)
                batch.update(ref, "refund_date", System.currentTimeMillis())
                batch.update(ref, "updated_at", System.currentTimeMillis())
            }
            
            // 3. Update order's refund_status to COMPLETED
            db.collection("orders").document(refund.orderId).let { ref ->
                batch.update(ref, "refund_status", "completed")
                batch.update(ref, "updated_at", System.currentTimeMillis())
            }
            
            // 4. Commit all updates atomically
            batch.commit().await()
            Log.d(TAG, "✅ Atomic batch write completed: refund=$refundId, payment=${refund.paymentId}, order=${refund.orderId}")

            // ⚠️ processRefundSplits() is currently a logging stub.
            // Before implementing real logic (balance updates, ledger entries),
            // evaluate whether it belongs inside the batch above.
            // Real work here with no failure handling = silent financial inconsistency.
            if (refund.refundSplits.isNotEmpty()) processRefundSplits(refund)

            // 5. Update refund to COMPLETED (separate write after splits are processed)
            // ⚠️ RISK: If this write fails, refund stays at PROCESSING while payment is REFUNDED and order is completed
            // This is a known partial state. Recovery logic should handle PROCESSING refunds whose payment is already REFUNDED.
            refundsCollection.document(refundId).update(
                mapOf(
                    "status" to RefundStatus.COMPLETED.toString(),
                    "processed_at" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            auditLogger.logRefundProcessed(
                paymentId = refund.paymentId,
                orderId = refund.orderId,
                refundAmount = refund.refundAmount,
                transactionId = finalTransactionId,
                actorId = actorId
            )

            Log.d(TAG, "✅ Refund processed: $refundId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process refund", e)
            handleRefundFailure(refundId, e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    // ==================== CANCEL REFUND ====================
    suspend fun cancelRefund(refundId: String, reason: String, actorId: String = "system"): Result<Unit> {
        return try {
            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = deserializeRefundRecord(refundDoc)
                ?: return Result.failure(Exception("Refund not found"))

            if (refund.status !in listOf(
                    RefundStatus.REQUESTED.toString(),
                    RefundStatus.APPROVED.toString()
                )
            ) {
                return Result.failure(Exception("Cannot cancel refund in ${refund.status} status"))
            }

            refundsCollection.document(refundId).update(
                mapOf(
                    "status" to RefundStatus.CANCELLED.toString(),
                    "description" to reason,
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            auditLogger.logRefundCancelled(
                paymentId = refund.paymentId,
                orderId = refund.orderId,
                refundAmount = refund.refundAmount,
                reason = reason,
                actorId = actorId
            )

            Log.d(TAG, "✅ Refund cancelled: $refundId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cancel refund", e)
            Result.failure(e)
        }
    }

    // ==================== RETRY FAILED REFUND ====================
    suspend fun retryFailedRefund(refundId: String): Result<Unit> {
        return try {
            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = deserializeRefundRecord(refundDoc)
                ?: return Result.failure(Exception("Refund not found"))

            if (refund.status != RefundStatus.FAILED.toString()) {
                return Result.failure(Exception("Only failed refunds can be retried"))
            }

            if (refund.retryCount >= refund.maxRetries) {
                return Result.failure(Exception("Max retries exceeded"))
            }

            refundsCollection.document(refundId).update(
                mapOf(
                    "status" to RefundStatus.PROCESSING.toString(),
                    "retry_count" to (refund.retryCount + 1),
                    "last_retry_at" to System.currentTimeMillis(),
                    "error_message" to "",
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            return processRefund(refundId, refund.transactionId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to retry refund", e)
            Result.failure(e)
        }
    }

    // ==================== QUERY REFUNDS ====================
    suspend fun getRefund(refundId: String): Result<RefundRecord> {
        return try {
            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = deserializeRefundRecord(refundDoc)
                ?: return Result.failure(Exception("Refund not found"))
            Result.success(refund)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRefundsForPayment(paymentId: String): Result<List<RefundRecord>> = try {
        val refunds = refundsCollection.whereEqualTo("payment_id", paymentId)
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull { deserializeRefundRecord(it) }
        Result.success(refunds)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getRefundsForBuyer(buyerId: String): Result<List<RefundRecord>> = try {
        val refunds = refundsCollection.whereEqualTo("buyer_id", buyerId)
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull { deserializeRefundRecord(it) }
        Result.success(refunds)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getRefundsForSeller(sellerId: String): Result<List<RefundRecord>> = try {
        val refunds = refundsCollection.whereEqualTo("seller_id", sellerId)
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull { deserializeRefundRecord(it) }
        Result.success(refunds)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPendingRefunds(): Result<List<RefundRecord>> = try {
        val refunds = refundsCollection.whereEqualTo("status", RefundStatus.REQUESTED.toString())
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get().await().documents.mapNotNull { deserializeRefundRecord(it) }
        Result.success(refunds)
    } catch (e: Exception) { Result.failure(e) }

    // includeFirstAttempt=true (default): returns all failed refunds, including those that
    // failed on their first attempt. This is the safe default — a retry queue that silently
    // drops first-attempt failures is a bug waiting to be filed.
    // includeFirstAttempt=false: returns only refunds with retry_count > 0, useful for an
    // admin view that specifically shows "failed despite retries".
    // Both paths are covered by the existing [status ASC, retry_count DESC] composite index.
    suspend fun getFailedRefunds(includeFirstAttempt: Boolean = true): Result<List<RefundRecord>> = try {
        var query = refundsCollection
            .whereEqualTo("status", RefundStatus.FAILED.toString())

        if (!includeFirstAttempt) {
            query = query.whereGreaterThan("retry_count", 0)
        }

        // orderBy("retry_count") satisfies Firestore's rule that the inequality field (retry_count)
        // must be the first orderBy when whereGreaterThan is active (includeFirstAttempt=false).
        // If you ever add a secondary orderBy on a different field (e.g. created_at), Firestore
        // will reject the query at runtime when includeFirstAttempt=false. Add a new composite
        // index covering [status, retry_count, <new field>] before doing so.
        val refunds = query
            .orderBy("retry_count", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await().documents.mapNotNull { deserializeRefundRecord(it) }
        Result.success(refunds)
    } catch (e: Exception) {
        Log.e(TAG, "getFailedRefunds() failed (includeFirstAttempt=$includeFirstAttempt)", e)
        Result.failure(e)
    }

    // ==================== PRIVATE HELPERS ====================
    private fun validateRefundEligibility(
        payment: SellerPayment,
        refundAmount: Double,
        deliveredAt: Long = 0L
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // ✅ FIX: Allow refund requests for completed, pending, and refund_rejected statuses
        // Reject duplicate refund requests (already refunded, processing, or pending)
        val status = payment.status.lowercase()
        val duplicateRefundStatuses = listOf("refunded", "refund_processing", "refund_pending")
        
        if (status in duplicateRefundStatuses) {
            errors.add("A refund request already exists for this payment")
        } else if (status !in listOf("completed", "pending", "refund_rejected")) {
            errors.add("Payment must be completed to initiate refund")
        }
        
        if (refundAmount <= 0) errors.add("Refund amount must be greater than 0")
        if (refundAmount > payment.amount) errors.add("Refund amount cannot exceed original payment amount")

        // ✅ CRITICAL FIX: Handle edge case where deliveredAt is 0 (order not yet delivered or field missing)
        // If referenceDate becomes 0, the 30-day window check produces a misleading error
        val referenceDate = if (deliveredAt > 0) deliveredAt else (payment.paymentDate ?: 0L)
        
        if (referenceDate == 0L) {
            // Order not yet delivered and no payment date found — this is a data integrity issue
            errors.add("Order not yet delivered or payment date missing. Refunds can only be requested after delivery.")
        } else {
            val daysSinceReference = (System.currentTimeMillis() - referenceDate) / (1000 * 60 * 60 * 24)
            if (daysSinceReference > REFUND_WINDOW_DAYS) {
                errors.add("Refund window expired (30 days from delivery)")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun calculateRefundSplits(payment: SellerPayment, refundAmount: Double): List<RefundSplit> {
        if (payment.paymentSplits.isEmpty()) return emptyList()
        
        // ✅ FIX 5: Use BigDecimal for precise refund split calculations
        val refundBD = BigDecimal(refundAmount)
        val splits = payment.paymentSplits.map { split ->
            // ✅ CRITICAL: splitPercentage is already a ratio (0.0–1.0), not a percentage (0–100)
            // It's set by createPaymentSplits() as: splitPercentage = percentage
            // where percentage = sellerSales / totalSales (already divided)
            // DO NOT divide by 100 again!
            val splitRatioBD = BigDecimal(split.splitPercentage)
            val splitRefundAmountBD = refundBD
                .multiply(splitRatioBD)
                .setScale(2, RoundingMode.HALF_UP)
            
            RefundSplit(
                sellerId = split.sellerId,
                sellerName = split.sellerName,
                refundAmount = splitRefundAmountBD.toDouble(),
                status = RefundStatus.REQUESTED.toString()
            )
        }
        
        // ✅ VERIFICATION: Ensure splits sum to refund amount (within 1 paisa tolerance)
        val totalRefund = splits.sumOf { BigDecimal(it.refundAmount) }
        val difference = refundBD.minus(totalRefund).abs()
        if (difference > BigDecimal("0.01")) {
            Log.w(TAG, "⚠️ Refund split precision warning: Expected $refundAmount, got ${totalRefund.toDouble()}, diff=${difference.toDouble()}")
        } else {
            Log.d(TAG, "✅ Refund splits calculated precisely: ${splits.size} members, total=$refundAmount")
        }
        
        return splits
    }

    private suspend fun processRefundSplits(refund: RefundRecord) {
        try {
            refund.refundSplits.forEach { split ->
                Log.d(TAG, "Processing split for seller: ${split.sellerId}, amount: ${split.refundAmount}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing refund splits", e)
        }
    }

    private suspend fun handleRefundFailure(refundId: String, errorMessage: String) {
        try {
            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = deserializeRefundRecord(refundDoc) ?: return
            val updates = if (refund.retryCount < refund.maxRetries) {
                mapOf(
                    "status" to RefundStatus.FAILED.toString(),
                    "error_message" to errorMessage,
                    "retry_count" to (refund.retryCount + 1),
                    "last_retry_at" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            } else {
                mapOf(
                    "status" to RefundStatus.FAILED.toString(),
                    "error_message" to "Max retries exceeded: $errorMessage",
                    "updated_at" to System.currentTimeMillis()
                )
            }
            refundsCollection.document(refundId).update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling refund failure", e)
        }
    }

    // ✅ UNIFIED: Single toMap() with optional parameters for web dashboard compatibility
    private fun RefundRecord.toMap(buyerName: String = "", sellerName: String = ""): Map<String, Any> = mapOf(
        "id" to id,
        "payment_id" to paymentId,
        "order_id" to orderId,
        "seller_id" to sellerId,
        "seller_name" to sellerName,      // ✅ web dashboard needs this
        "buyer_id" to buyerId,
        "buyer_name" to buyerName,        // ✅ web dashboard "Buyer" column - NOW INCLUDED
        "refund_amount" to refundAmount,
        "original_amount" to originalAmount,
        "reason" to reason,
        "description" to description,
        "requested_by" to requestedBy,
        "approved_by" to (approvedBy ?: ""),
        "status" to status,
        "transaction_id" to transactionId,
        "payment_method" to paymentMethod,
        "refund_splits" to refundSplits.map { it.toRefundSplitMap() },
        "retry_count" to retryCount,
        "max_retries" to maxRetries,
        "last_retry_at" to (lastRetryAt ?: 0L),
        "error_message" to errorMessage,
        "created_at" to createdAt,
        "requested_at" to createdAt,      // ✅ web dashboard "Requested" column - NOW INCLUDED
        "approved_at" to (approvedAt ?: 0L),
        "processed_at" to (processedAt ?: 0L),
        "updated_at" to updatedAt,
        "idempotency_key" to idempotencyKey
    )

    private fun RefundSplit.toRefundSplitMap(): Map<String, Any> = mapOf(
        "seller_id" to sellerId,
        "seller_name" to sellerName,
        "refund_amount" to refundAmount,
        "status" to status
    )
}

// ==================== VALIDATION RESULT ====================
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)