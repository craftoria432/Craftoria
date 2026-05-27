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
    var refundType: String = RefundType.FULL.toString(),

    @get:PropertyName("original_amount")
    @set:PropertyName("original_amount")
    var originalAmount: Double = 0.0,

    @get:PropertyName("refund_amount")
    @set:PropertyName("refund_amount")
    var refundAmount: Double = 0.0,

    @get:PropertyName("reason")
    @set:PropertyName("reason")
    var reason: String = "",

    @get:PropertyName("reason_details")
    @set:PropertyName("reason_details")
    var reasonDetails: String = "",

    var status: String = RefundStatus.REQUESTED.toString(),

    @get:PropertyName("initiated_by")
    @set:PropertyName("initiated_by")
    var initiatedBy: String = "",

    @get:PropertyName("approved_by")
    @set:PropertyName("approved_by")
    var approvedBy: String = "",

    @get:PropertyName("approval_notes")
    @set:PropertyName("approval_notes")
    var approvalNotes: String = "",

    // ✅ NEW: Rejection tracking for request limits
    @get:PropertyName("rejection_count")
    @set:PropertyName("rejection_count")
    var rejectionCount: Int = 0,

    @get:PropertyName("can_resubmit")
    @set:PropertyName("can_resubmit")
    var canResubmit: Boolean = true,

    @get:PropertyName("final_decision")
    @set:PropertyName("final_decision")
    var finalDecision: Boolean = false,

    // Payment Gateway Info
    @get:PropertyName("payment_method")
    @set:PropertyName("payment_method")
    var paymentMethod: String = "Cash on Delivery",

    @get:PropertyName("transaction_id")
    @set:PropertyName("transaction_id")
    var transactionId: String = "",

    @get:PropertyName("gateway_refund_id")
    @set:PropertyName("gateway_refund_id")
    var gatewayRefundId: String = "",

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
    var lastRetryAt: Any? = null,   // ✅ Any? — Firestore may return Timestamp or Long

    @get:PropertyName("error_message")
    @set:PropertyName("error_message")
    var errorMessage: String = "",

    // ✅ ALL timestamps changed to Any? to safely handle both Long and Firestore Timestamp
    @get:PropertyName("requested_at")
    @set:PropertyName("requested_at")
    var requestedAt: Any? = System.currentTimeMillis(),

    @get:PropertyName("approved_at")
    @set:PropertyName("approved_at")
    var approvedAt: Any? = null,

    @get:PropertyName("processed_at")
    @set:PropertyName("processed_at")
    var processedAt: Any? = null,

    @get:PropertyName("completed_at")
    @set:PropertyName("completed_at")
    var completedAt: Any? = null,

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Any? = System.currentTimeMillis(),

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Any? = System.currentTimeMillis(),

    // Idempotency
    @get:PropertyName("idempotency_key")
    @set:PropertyName("idempotency_key")
    var idempotencyKey: String = "",  // ✅ FIX: Default to "" instead of UUID.randomUUID() to prevent phantom keys on deserialization

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

// ✅ Also changed timestamp to Any? for same reason
data class RefundAuditEntry(
    var action: String = "",
    var actor: String = "",
    @get:PropertyName("actor_name")
    @set:PropertyName("actor_name")
    var actorName: String = "",
    var notes: String = "",
    var timestamp: Any? = System.currentTimeMillis()  // ✅ Any? instead of Long
)

enum class RefundType {
    FULL,
    PARTIAL,
    RETURN;

    override fun toString(): String = name.lowercase()
}

enum class RefundStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED_BY_SELLER,
    APPROVED_BY_ADMIN,
    REJECTED_BY_SELLER,
    REJECTED_BY_ADMIN,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        REQUESTED -> "Refund Requested"
        UNDER_REVIEW -> "Under Review"
        APPROVED_BY_SELLER -> "Approved by Seller"
        APPROVED_BY_ADMIN -> "Approved by Admin"
        REJECTED_BY_SELLER -> "Rejected by Seller"
        REJECTED_BY_ADMIN -> "Rejected by Admin"
        PROCESSING -> "Processing"
        COMPLETED -> "Refunded Successfully"
        FAILED -> "Failed"
        CANCELLED -> "Cancelled"
    }

    fun getStatusColor(): String = when (this) {
        REQUESTED -> "#FFA500"           // Orange
        UNDER_REVIEW -> "#FF9800"        // Amber
        APPROVED_BY_SELLER -> "#4169E1"  // Royal Blue
        APPROVED_BY_ADMIN -> "#2196F3"   // Blue
        REJECTED_BY_SELLER -> "#DC3545"  // Red
        REJECTED_BY_ADMIN -> "#C62828"   // Dark Red
        PROCESSING -> "#1E90FF"          // Dodger Blue
        COMPLETED -> "#28A745"           // Green
        FAILED -> "#FF6347"              // Tomato
        CANCELLED -> "#6C757D"           // Gray
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

/* -------------------- Timestamp Converter -------------------- */

/**
 * Safely converts Any? (Long, Firestore Timestamp, Map, String) to Long milliseconds.
 * This is the root cause of "Failed to convert com.google.firebase.Timestamp to long".
 */
private fun convertRefundTimestamp(value: Any?): Long = when (value) {
    is Long -> value
    is com.google.firebase.Timestamp -> value.toDate().time
    is Number -> value.toLong()
    is String -> value.toLongOrNull() ?: System.currentTimeMillis()
    is Map<*, *> -> {
        val seconds = (value["_seconds"] as? Long) ?: (value["seconds"] as? Long) ?: 0L
        val nanos = (value["_nanoseconds"] as? Long) ?: (value["nanoseconds"] as? Long) ?: 0L
        (seconds * 1000) + (nanos / 1_000_000)
    }
    else -> System.currentTimeMillis()
}

/* -------------------- Firestore Mappers -------------------- */

fun RefundRequest.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "order_id" to orderId,
    "payment_id" to paymentId,
    "buyer_id" to buyerId,
    "buyer_name" to buyerName,          // ✅ WEB DASHBOARD: "Buyer" column
    "seller_id" to sellerId,
    "seller_name" to sellerName,        // ✅ WEB DASHBOARD: "Seller" column
    "refund_type" to refundType,
    "original_amount" to originalAmount,
    "refund_amount" to refundAmount,
    "reason" to reason,
    "reason_details" to reasonDetails,
    "status" to status,
    "initiated_by" to initiatedBy,
    "approved_by" to approvedBy,
    "approval_notes" to approvalNotes,
    "rejection_count" to rejectionCount,
    "can_resubmit" to canResubmit,
    "final_decision" to finalDecision,
    "payment_method" to paymentMethod,
    "transaction_id" to transactionId,
    "gateway_refund_id" to gatewayRefundId,
    "refund_splits" to refundSplits.map { it.toMap() },
    "retry_count" to retryCount,
    "last_retry_at" to (getLastRetryAtLong()),
    "error_message" to errorMessage,
    "requested_at" to getRequestedAtLong(),   // ✅ WEB DASHBOARD: "Requested" column (payment date)
    "approved_at" to (getApprovedAtLong()),
    "processed_at" to (getProcessedAtLong()),
    "completed_at" to (getCompletedAtLong()),
    "created_at" to getCreatedAtLong(),
    "updated_at" to getUpdatedAtLong(),
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
    "timestamp" to convertRefundTimestamp(timestamp)
)

/* -------------------- Timestamp Helpers -------------------- */

fun RefundRequest.getRequestedAtLong(): Long = convertRefundTimestamp(requestedAt)
fun RefundRequest.getCreatedAtLong(): Long = convertRefundTimestamp(createdAt)
fun RefundRequest.getUpdatedAtLong(): Long = convertRefundTimestamp(updatedAt)
fun RefundRequest.getApprovedAtLong(): Long = convertRefundTimestamp(approvedAt).let { if (approvedAt == null) 0L else it }
fun RefundRequest.getProcessedAtLong(): Long = convertRefundTimestamp(processedAt).let { if (processedAt == null) 0L else it }
fun RefundRequest.getCompletedAtLong(): Long = convertRefundTimestamp(completedAt).let { if (completedAt == null) 0L else it }
fun RefundRequest.getLastRetryAtLong(): Long = convertRefundTimestamp(lastRetryAt).let { if (lastRetryAt == null) 0L else it }

/* -------------------- Helpers -------------------- */

fun RefundRequest.getStatusEnum(): RefundStatus = parseRefundStatus(status)

/** Maps Firestore status strings (including legacy processor values) to canonical RefundStatus. */
fun parseRefundStatus(value: String): RefundStatus = when (value.lowercase()) {
    "approved" -> RefundStatus.APPROVED_BY_SELLER
    "rejected" -> RefundStatus.REJECTED_BY_SELLER
    "disputed" -> RefundStatus.UNDER_REVIEW
    else -> try {
        RefundStatus.valueOf(value.uppercase())
    } catch (_: Exception) {
        RefundStatus.REQUESTED
    }
}

fun RefundRequest.getTypeEnum(): RefundType =
    try { RefundType.valueOf(refundType.uppercase()) } catch (e: Exception) { RefundType.FULL }

fun RefundRequest.isEligibleForAutoApproval(): Boolean {
    val hoursSinceOrder = (System.currentTimeMillis() - getRequestedAtLong()) / (1000 * 60 * 60)
    return initiatedBy == "buyer" && hoursSinceOrder <= 24
}

fun RefundRequest.canRetry(): Boolean =
    status == RefundStatus.FAILED.toString() && retryCount < 3
