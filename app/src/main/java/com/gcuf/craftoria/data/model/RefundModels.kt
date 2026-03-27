package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RefundRequest(
    var id: String = "",

    @get:PropertyName("order_id")
    @set:PropertyName("order_id")
    var orderId: String = "",

    @get:PropertyName("payment_id")
    @set:PropertyName("payment_id")
    var paymentId: String = "",

    @get:PropertyName("buyer_id")
    @set:PropertyName("buyer_id")
    var buyerId: String = "",

    @get:PropertyName("buyer_name")
    @set:PropertyName("buyer_name")
    var buyerName: String = "",

    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    // Refund Details
    @get:PropertyName("refund_type")
    @set:PropertyName("refund_type")
    var refundType: String = RefundType.FULL.toString(), // FULL, PARTIAL, RETURN

    @get:PropertyName("original_amount")
    @set:PropertyName("original_amount")
    var originalAmount: Double = 0.0,

    @get:PropertyName("refund_amount")
    @set:PropertyName("refund_amount")
    var refundAmount: Double = 0.0,

    @get:PropertyName("reason")
    @set:PropertyName("reason")
    var reason: String = "", // BUYER_REQUEST, SELLER_APPROVAL, RETURN, DEFECTIVE, WRONG_ITEM, etc.

    @get:PropertyName("reason_details")
    @set:PropertyName("reason_details")
    var reasonDetails: String = "",

    // Status: requested, approved, processing, completed, rejected, failed
    var status: String = RefundStatus.REQUESTED.toString(),

    @get:PropertyName("initiated_by")
    @set:PropertyName("initiated_by")
    var initiatedBy: String = "", // buyer or seller

    @get:PropertyName("approved_by")
    @set:PropertyName("approved_by")
    var approvedBy: String = "", // admin or seller

    @get:PropertyName("approval_notes")
    @set:PropertyName("approval_notes")
    var approvalNotes: String = "",

    // Payment Gateway Info
    @get:PropertyName("payment_method")
    @set:PropertyName("payment_method")
    var paymentMethod: String = "Cash on Delivery",

    @get:PropertyName("transaction_id")
    @set:PropertyName("transaction_id")
    var transactionId: String = "",

    @get:PropertyName("gateway_refund_id")
    @set:PropertyName("gateway_refund_id")
    var gatewayRefundId: String = "", // Stripe refund ID, PayPal refund ID, etc.

    // Co-Seller Refund Splits
    @get:PropertyName("refund_splits")
    @set:PropertyName("refund_splits")
    var refundSplits: List<RefundSplit> = emptyList(),

    // Retry Info
    @get:PropertyName("retry_count")
    @set:PropertyName("retry_count")
    var retryCount: Int = 0,

    @get:PropertyName("last_retry_at")
    @set:PropertyName("last_retry_at")
    var lastRetryAt: Long? = null,

    @get:PropertyName("error_message")
    @set:PropertyName("error_message")
    var errorMessage: String = "",

    // Timestamps
    @get:PropertyName("requested_at")
    @set:PropertyName("requested_at")
    var requestedAt: Long = System.currentTimeMillis(),

    @get:PropertyName("approved_at")
    @set:PropertyName("approved_at")
    var approvedAt: Long? = null,

    @get:PropertyName("processed_at")
    @set:PropertyName("processed_at")
    var processedAt: Long? = null,

    @get:PropertyName("completed_at")
    @set:PropertyName("completed_at")
    var completedAt: Long? = null,

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis(),

    // Idempotency
    @get:PropertyName("idempotency_key")
    @set:PropertyName("idempotency_key")
    var idempotencyKey: String = "",

    // Audit Trail
    @get:PropertyName("audit_trail")
    @set:PropertyName("audit_trail")
    var auditTrail: List<RefundAuditEntry> = emptyList()
)

data class RefundSplit(
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    @get:PropertyName("original_split_amount")
    @set:PropertyName("original_split_amount")
    var originalSplitAmount: Double = 0.0,

    @get:PropertyName("refund_split_amount")
    @set:PropertyName("refund_split_amount")
    var refundSplitAmount: Double = 0.0,

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = RefundStatus.REQUESTED.toString(),

    @get:PropertyName("gateway_refund_id")
    @set:PropertyName("gateway_refund_id")
    var gatewayRefundId: String = ""
)

data class RefundAuditEntry(
    var action: String = "", // requested, approved, rejected, processing, completed, failed, retried
    var actor: String = "", // user ID
    @get:PropertyName("actor_name")
    @set:PropertyName("actor_name")
    var actorName: String = "",
    var notes: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

enum class RefundType {
    FULL,      // Full refund of order
    PARTIAL,   // Partial refund (e.g., some items)
    RETURN;    // Refund after return

    override fun toString(): String = name.lowercase()
}

enum class RefundStatus {
    REQUESTED,   // Buyer/seller initiated refund
    APPROVED,    // Admin/seller approved
    PROCESSING,  // Payment gateway processing
    COMPLETED,   // Refund successful
    REJECTED,    // Refund denied
    FAILED,      // Refund failed (needs retry)
    CANCELLED;   // Refund cancelled

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        REQUESTED -> "Refund Requested"
        APPROVED -> "Approved"
        PROCESSING -> "Processing"
        COMPLETED -> "Completed"
        REJECTED -> "Rejected"
        FAILED -> "Failed"
        CANCELLED -> "Cancelled"
    }

    fun getStatusColor(): String = when (this) {
        REQUESTED -> "#FFA500"    // Orange
        APPROVED -> "#4169E1"     // Royal Blue
        PROCESSING -> "#1E90FF"   // Dodger Blue
        COMPLETED -> "#28A745"    // Green
        REJECTED -> "#DC3545"     // Red
        FAILED -> "#FF6347"       // Tomato
        CANCELLED -> "#6C757D"    // Gray
    }
}

enum class RefundReason {
    BUYER_REQUEST,
    SELLER_APPROVAL,
    DEFECTIVE_PRODUCT,
    WRONG_ITEM,
    NOT_AS_DESCRIBED,
    DAMAGED_IN_TRANSIT,
    LOST_IN_TRANSIT,
    BUYER_CHANGED_MIND,
    DUPLICATE_ORDER,
    PAYMENT_ERROR,
    CHARGEBACK,
    OTHER;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        BUYER_REQUEST -> "Buyer Request"
        SELLER_APPROVAL -> "Seller Approval"
        DEFECTIVE_PRODUCT -> "Defective Product"
        WRONG_ITEM -> "Wrong Item"
        NOT_AS_DESCRIBED -> "Not as Described"
        DAMAGED_IN_TRANSIT -> "Damaged in Transit"
        LOST_IN_TRANSIT -> "Lost in Transit"
        BUYER_CHANGED_MIND -> "Buyer Changed Mind"
        DUPLICATE_ORDER -> "Duplicate Order"
        PAYMENT_ERROR -> "Payment Error"
        CHARGEBACK -> "Chargeback"
        OTHER -> "Other"
    }
}

/* -------------------- Firestore Mappers -------------------- */
fun RefundRequest.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "order_id" to orderId,
    "payment_id" to paymentId,
    "buyer_id" to buyerId,
    "buyer_name" to buyerName,
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "refund_type" to refundType,
    "original_amount" to originalAmount,
    "refund_amount" to refundAmount,
    "reason" to reason,
    "reason_details" to reasonDetails,
    "status" to status,
    "initiated_by" to initiatedBy,
    "approved_by" to approvedBy,
    "approval_notes" to approvalNotes,
    "payment_method" to paymentMethod,
    "transaction_id" to transactionId,
    "gateway_refund_id" to gatewayRefundId,
    "refund_splits" to refundSplits.map { it.toMap() },
    "retry_count" to retryCount,
    "last_retry_at" to (lastRetryAt ?: 0L),
    "error_message" to errorMessage,
    "requested_at" to requestedAt,
    "approved_at" to (approvedAt ?: 0L),
    "processed_at" to (processedAt ?: 0L),
    "completed_at" to (completedAt ?: 0L),
    "created_at" to createdAt,
    "updated_at" to updatedAt,
    "idempotency_key" to idempotencyKey,
    "audit_trail" to auditTrail.map { it.toMap() }
)

fun RefundSplit.toMap(): Map<String, Any> = mapOf(
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "original_split_amount" to originalSplitAmount,
    "refund_split_amount" to refundSplitAmount,
    "status" to status,
    "gateway_refund_id" to gatewayRefundId
)

fun RefundAuditEntry.toMap(): Map<String, Any> = mapOf(
    "action" to action,
    "actor" to actor,
    "actor_name" to actorName,
    "notes" to notes,
    "timestamp" to timestamp
)

/* -------------------- Helpers -------------------- */
fun RefundRequest.getStatusEnum(): RefundStatus =
    try {
        RefundStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        RefundStatus.REQUESTED
    }

fun RefundRequest.getTypeEnum(): RefundType =
    try {
        RefundType.valueOf(refundType.uppercase())
    } catch (e: Exception) {
        RefundType.FULL
    }

fun RefundRequest.isEligibleForAutoApproval(): Boolean {
    // Auto-approve if within grace period (24 hours) and buyer-initiated
    val hoursSinceOrder = (System.currentTimeMillis() - requestedAt) / (1000 * 60 * 60)
    return initiatedBy == "buyer" && hoursSinceOrder <= 24
}

fun RefundRequest.canRetry(): Boolean {
    return status == RefundStatus.FAILED.toString() && retryCount < 3
}
