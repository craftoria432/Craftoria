package com.gcuf.craftoria.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties
import com.gcuf.craftoria.data.model.SellerPayment
import android.util.Log
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.math.BigDecimal
import java.math.RoundingMode

// ==================== ENUMS ====================
enum class RefundStatus {
    PENDING,
    APPROVED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    DISPUTED;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        PENDING -> "Pending"
        APPROVED -> "Approved"
        PROCESSING -> "Processing"
        COMPLETED -> "Completed"
        FAILED -> "Failed"
        CANCELLED -> "Cancelled"
        DISPUTED -> "Disputed"
    }

    fun getStatusColor(): String = when (this) {
        PENDING -> "#FFA500"
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
    @get:PropertyName("payment_id")
    @set:PropertyName("payment_id")
    var paymentId: String = "",
    @get:PropertyName("order_id")
    @set:PropertyName("order_id")
    var orderId: String = "",
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",
    @get:PropertyName("buyer_id")
    @set:PropertyName("buyer_id")
    var buyerId: String = "",
    @get:PropertyName("refund_amount")
    @set:PropertyName("refund_amount")
    var refundAmount: Double = 0.0,
    @get:PropertyName("original_amount")
    @set:PropertyName("original_amount")
    var originalAmount: Double = 0.0,
    @get:PropertyName("reason")
    @set:PropertyName("reason")
    var reason: String = RefundReason.OTHER.toString(),
    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",
    @get:PropertyName("requested_by")
    @set:PropertyName("requested_by")
    var requestedBy: String = "",
    @get:PropertyName("approved_by")
    @set:PropertyName("approved_by")
    var approvedBy: String? = null,
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = RefundStatus.PENDING.toString(),
    @get:PropertyName("transaction_id")
    @set:PropertyName("transaction_id")
    var transactionId: String = "",
    @get:PropertyName("payment_method")
    @set:PropertyName("payment_method")
    var paymentMethod: String = "",
    @get:PropertyName("refund_splits")
    @set:PropertyName("refund_splits")
    var refundSplits: List<RefundSplit> = emptyList(),
    @get:PropertyName("retry_count")
    @set:PropertyName("retry_count")
    var retryCount: Int = 0,
    @get:PropertyName("max_retries")
    @set:PropertyName("max_retries")
    var maxRetries: Int = 3,
    @get:PropertyName("last_retry_at")
    @set:PropertyName("last_retry_at")
    var lastRetryAt: Long? = null,
    @get:PropertyName("error_message")
    @set:PropertyName("error_message")
    var errorMessage: String = "",
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),
    @get:PropertyName("approved_at")
    @set:PropertyName("approved_at")
    var approvedAt: Long? = null,
    @get:PropertyName("processed_at")
    @set:PropertyName("processed_at")
    var processedAt: Long? = null,
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis(),
    @get:PropertyName("idempotency_key")
    @set:PropertyName("idempotency_key")
    var idempotencyKey: String = UUID.randomUUID().toString()
)

data class RefundSplit(
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    @get:PropertyName("refund_amount")
    @set:PropertyName("refund_amount")
    var refundAmount: Double = 0.0,

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = RefundStatus.PENDING.toString()
)

// ==================== REFUND PROCESSOR ====================
class RefundProcessor(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val paymentsCollection = db.collection("seller_payments")
    private val refundsCollection = db.collection("refunds")
    private val auditLogger = PaymentAuditLogger(db)
    private val retryManager = PaymentRetryManager()

    companion object {
        private const val TAG = "RefundProcessor"
        private const val REFUND_WINDOW_DAYS = 30
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

            // Get payment
            val paymentDoc = paymentsCollection.document(paymentId).get().await()
            val payment = paymentDoc.toObject(SellerPayment::class.java)
                ?: return Result.failure(Exception("Payment not found"))

            // Validate refund eligibility
            val validation = validateRefundEligibility(payment, refundAmount)
            if (!validation.isValid) {
                val errorMsg = validation.errors.joinToString(", ")
                Log.e(TAG, "❌ Refund validation failed: $errorMsg")
                return Result.failure(Exception(errorMsg))
            }

            // Create refund record
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
                status = RefundStatus.PENDING.toString(),
                refundSplits = calculateRefundSplits(payment, refundAmount),
                createdAt = System.currentTimeMillis()
            )

            val refundDoc = refundsCollection.add(refund.toMap()).await()
            val refundId = refundDoc.id

            // Update refund with ID
            refundsCollection.document(refundId).update("id", refundId).await()

            // Log audit
            auditLogger.logRefundInitiated(
                paymentId = paymentId,
                orderId = payment.orderId,
                refundAmount = refundAmount,
                reason = reason,
                actorId = requestedBy
            )

            Log.d(TAG, "✅ Refund initiated: $refundId")
            Result.success(refundId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initiate refund", e)
            Result.failure(e)
        }
    }

    // ==================== APPROVE REFUND ====================
    suspend fun approveRefund(
        refundId: String,
        approvedBy: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "✅ Approving refund: $refundId")

            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = refundDoc.toObject(RefundRecord::class.java)
                ?: return Result.failure(Exception("Refund not found"))

            if (refund.status != RefundStatus.PENDING.toString()) {
                return Result.failure(Exception("Only pending refunds can be approved"))
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
                actorId = approvedBy // Assuming the admin/approver is the actor
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
            Log.d(TAG, "🔄 Processing refund: $refundId")

            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = refundDoc.toObject(RefundRecord::class.java)
                ?: return Result.failure(Exception("Refund not found"))

            // Only process approved or pending refunds
            if (refund.status !in listOf(RefundStatus.APPROVED.toString(), RefundStatus.PENDING.toString())) {
                return Result.failure(Exception("Refund cannot be processed in ${refund.status} status"))
            }

            // Update refund status
            val finalTransactionId = transactionId.ifEmpty { UUID.randomUUID().toString() }
            refundsCollection.document(refundId).update(
                mapOf(
                    "status" to RefundStatus.PROCESSING.toString(),
                    "transaction_id" to finalTransactionId,
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            // Process refund splits for co-seller orders
            if (refund.refundSplits.isNotEmpty()) {
                processRefundSplits(refund)
            }

            // Update payment status
            paymentsCollection.document(refund.paymentId).update(
                mapOf(
                    "status" to "refunded",
                    "refund_amount" to refund.refundAmount,
                    "refund_date" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            // Mark as completed
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
    suspend fun cancelRefund(
        refundId: String,
        reason: String,
        actorId: String = "system"
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Cancelling refund: $refundId")

            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = refundDoc.toObject(RefundRecord::class.java)
                ?: return Result.failure(Exception("Refund not found"))

            // Can only cancel pending or approved refunds
            if (refund.status !in listOf(RefundStatus.PENDING.toString(), RefundStatus.APPROVED.toString())) {
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
            Log.d(TAG, "🔄 Retrying failed refund: $refundId")

            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = refundDoc.toObject(RefundRecord::class.java)
                ?: return Result.failure(Exception("Refund not found"))

            if (refund.status != RefundStatus.FAILED.toString()) {
                return Result.failure(Exception("Only failed refunds can be retried"))
            }

            if (refund.retryCount >= refund.maxRetries) {
                return Result.failure(Exception("Max retries exceeded"))
            }

            // Increment retry count
            val newRetryCount = refund.retryCount + 1
            refundsCollection.document(refundId).update(
                mapOf(
                    "status" to RefundStatus.PROCESSING.toString(),
                    "retry_count" to newRetryCount,
                    "last_retry_at" to System.currentTimeMillis(),
                    "error_message" to "",
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            // Attempt to process again
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
            val refund = refundDoc.toObject(RefundRecord::class.java)
                ?: return Result.failure(Exception("Refund not found"))
            Result.success(refund)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get refund", e)
            Result.failure(e)
        }
    }

    suspend fun getRefundsForPayment(paymentId: String): Result<List<RefundRecord>> {
        return try {
            val refunds = refundsCollection
                .whereEqualTo("payment_id", paymentId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RefundRecord::class.java)
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get refunds for payment", e)
            Result.failure(e)
        }
    }

    suspend fun getRefundsForBuyer(buyerId: String): Result<List<RefundRecord>> {
        return try {
            val refunds = refundsCollection
                .whereEqualTo("buyer_id", buyerId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RefundRecord::class.java)
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get refunds for buyer", e)
            Result.failure(e)
        }
    }

    suspend fun getRefundsForSeller(sellerId: String): Result<List<RefundRecord>> {
        return try {
            val refunds = refundsCollection
                .whereEqualTo("seller_id", sellerId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RefundRecord::class.java)
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get refunds for seller", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingRefunds(): Result<List<RefundRecord>> {
        return try {
            val refunds = refundsCollection
                .whereEqualTo("status", RefundStatus.PENDING.toString())
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(RefundRecord::class.java)
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get pending refunds", e)
            Result.failure(e)
        }
    }

    suspend fun getFailedRefunds(): Result<List<RefundRecord>> {
        return try {
            val refunds = refundsCollection
                .whereEqualTo("status", RefundStatus.FAILED.toString())
                .whereGreaterThan("retry_count", 0)
                .orderBy("retry_count", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(RefundRecord::class.java)
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get failed refunds", e)
            Result.failure(e)
        }
    }

    // ==================== PRIVATE HELPERS ====================
    private fun validateRefundEligibility(payment: SellerPayment, refundAmount: Double): ValidationResult {
        val errors = mutableListOf<String>()

        // Check if payment exists and is completed
        if (payment.status != "completed" && payment.status != "refunded") {
            errors.add("Payment must be completed to initiate refund")
        }

        // Check refund amount
        if (refundAmount <= 0) {
            errors.add("Refund amount must be greater than 0")
        }

        if (refundAmount > payment.amount) {
            errors.add("Refund amount cannot exceed original payment amount")
        }

        // Check refund window (30 days)
        val daysSincePayment = (System.currentTimeMillis() - (payment.paymentDate ?: 0L)) / (1000 * 60 * 60 * 24)
        if (daysSincePayment > REFUND_WINDOW_DAYS) {
            errors.add("Refund window expired (30 days)")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun calculateRefundSplits(payment: SellerPayment, refundAmount: Double): List<RefundSplit> {
        if (payment.paymentSplits.isEmpty()) {
            return emptyList()
        }

        return payment.paymentSplits.map { split ->
            val splitRefundAmount = BigDecimal(refundAmount)
                .multiply(BigDecimal(split.splitPercentage / 100))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            RefundSplit(
                sellerId = split.sellerId,
                sellerName = split.sellerName,
                refundAmount = splitRefundAmount,
                status = RefundStatus.PENDING.toString()
            )
        }
    }

    private suspend fun processRefundSplits(refund: RefundRecord) {
        try {
            refund.refundSplits.forEach { split ->
                // Update split status to processing
                Log.d(TAG, "Processing refund split for seller: ${split.sellerId}, amount: ${split.refundAmount}")
                // In production, integrate with payment gateway to process each split
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing refund splits", e)
        }
    }

    private suspend fun handleRefundFailure(refundId: String, errorMessage: String) {
        try {
            val refundDoc = refundsCollection.document(refundId).get().await()
            val refund = refundDoc.toObject(RefundRecord::class.java) ?: return

            if (refund.retryCount < refund.maxRetries) {
                refundsCollection.document(refundId).update(
                    mapOf(
                        "status" to RefundStatus.FAILED.toString(),
                        "error_message" to errorMessage,
                        "retry_count" to (refund.retryCount + 1),
                        "last_retry_at" to System.currentTimeMillis(),
                        "updated_at" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                refundsCollection.document(refundId).update(
                    mapOf(
                        "status" to RefundStatus.FAILED.toString(),
                        "error_message" to "Max retries exceeded: $errorMessage",
                        "updated_at" to System.currentTimeMillis()
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling refund failure", e)
        }
    }

    private fun RefundRecord.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "payment_id" to paymentId,
            "order_id" to orderId,
            "seller_id" to sellerId,
            "buyer_id" to buyerId,
            "refund_amount" to refundAmount,
            "original_amount" to originalAmount,
            "reason" to reason,
            "description" to description,
            "requested_by" to requestedBy,
            "approved_by" to (approvedBy ?: ""),
            "status" to status,
            "transaction_id" to transactionId,
            "payment_method" to paymentMethod,
            "refund_splits" to refundSplits.map { it.toMap() },
            "retry_count" to retryCount,
            "max_retries" to maxRetries,
            "last_retry_at" to (lastRetryAt ?: 0L),
            "error_message" to errorMessage,
            "created_at" to createdAt,
            "approved_at" to (approvedAt ?: 0L),
            "processed_at" to (processedAt ?: 0L),
            "updated_at" to updatedAt,
            "idempotency_key" to idempotencyKey
        )
    }

    private fun RefundSplit.toMap(): Map<String, Any> {
        return mapOf(
            "seller_id" to sellerId,
            "seller_name" to sellerName,
            "refund_amount" to refundAmount,
            "status" to status
        )
    }
}

// ==================== VALIDATION RESULT ====================
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)
