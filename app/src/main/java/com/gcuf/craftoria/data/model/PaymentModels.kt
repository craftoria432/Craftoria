package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class SellerPayment(
    var id: String = "",

    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    @get:PropertyName("order_id")
    @set:PropertyName("order_id")
    var orderId: String = "",

    @get:PropertyName("co_seller_store_id")
    @set:PropertyName("co_seller_store_id")
    var coSellerStoreId: String = "",

    @get:PropertyName("store_name")
    @set:PropertyName("store_name")
    var storeName: String = "",

    @get:PropertyName("buyer_id")
    @set:PropertyName("buyer_id")
    var buyerId: String = "",

    @get:PropertyName("buyer_name")
    @set:PropertyName("buyer_name")
    var buyerName: String = "",

    // ✅ Track all sellers involved in this order for access control
    @get:PropertyName("involved_seller_ids")
    @set:PropertyName("involved_seller_ids")
    var involvedSellerIds: List<String> = emptyList(),

    // ✅ NEW: Payment split details (for co-seller stores)
    @get:PropertyName("payment_splits")
    @set:PropertyName("payment_splits")
    var paymentSplits: List<PaymentSplit> = emptyList(),

    // Payment Details
    var amount: Double = 0.0,

    @get:PropertyName("payment_method")
    @set:PropertyName("payment_method")
    var paymentMethod: String = "Cash on Delivery",

    @get:PropertyName("transaction_id")
    @set:PropertyName("transaction_id")
    var transactionId: String = "",

    // Status: pending, processing, completed, failed, refunded
    var status: String = PaymentStatus.PENDING.toString(),

    @get:PropertyName("payment_date")
    @set:PropertyName("payment_date")
    var paymentDate: Long? = null,

    // ✅ NEW: Original order/transaction date for accurate history display
    @get:PropertyName("original_transaction_date")
    @set:PropertyName("original_transaction_date")
    var originalTransactionDate: Long? = null,

    @get:PropertyName("items_count")
    @set:PropertyName("items_count")
    var itemsCount: Int = 0,

    @get:PropertyName("items_details")
    @set:PropertyName("items_details")
    var itemsDetails: List<PaymentItemDetail> = emptyList(),

    // Timestamps
    // ✅ DESIGN NOTE: createdAt and updatedAt are typed as Any? to serve as a safety net
    // for mixed timestamp formats (Long, Firestore Timestamp, Map, String). However,
    // this is NOT the primary deserialization path.
    //
    // PRIMARY PATH: PaymentRepository.parsePayment() reads these fields manually and
    // converts them to Long via anyToMillis(). This is the ONLY safe way to deserialize
    // SellerPayment from Firestore. Never use toObject() or toObjects() — they will
    // crash with mixed timestamp types.
    //
    // The Any? type is a defensive measure: if code somehow bypasses parsePayment()
    // and calls toObject() directly, the Any? type prevents the reflective deserializer
    // from crashing. But this should never happen in production — all deserialization
    // must go through parsePayment().
    //
    // Helper functions (getCreatedAtLong(), getUpdatedAtLong()) exist for code that
    // needs to convert the Any? value to Long, but they duplicate the logic in
    // parsePayment(). Ideally, all code should use parsePayment() and never need these
    // helpers. They exist only for defensive compatibility.
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Any? = System.currentTimeMillis(),

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Any? = System.currentTimeMillis(),

    // Refund info
    @get:PropertyName("refund_amount")
    @set:PropertyName("refund_amount")
    var refundAmount: Double = 0.0,

    @get:PropertyName("refund_reason")
    @set:PropertyName("refund_reason")
    var refundReason: String = "",

    @get:PropertyName("refund_date")
    @set:PropertyName("refund_date")
    var refundDate: Any? = null,  // ✅ Changed to Any? to safely handle both Long and Firestore Timestamp

    // Idempotency & Request Tracking
    @get:PropertyName("idempotency_key")
    @set:PropertyName("idempotency_key")
    var idempotencyKey: String = "",

    @get:PropertyName("request_id")
    @set:PropertyName("request_id")
    var requestId: String = ""
)

data class PaymentItemDetail(
    @get:PropertyName("product_id")
    @set:PropertyName("product_id")
    var productId: String = "",

    @get:PropertyName("product_title")
    @set:PropertyName("product_title")
    var productTitle: String = "",

    var quantity: Int = 1,
    var price: Double = 0.0,

    @get:PropertyName("item_total")
    @set:PropertyName("item_total")
    var itemTotal: Double = 0.0
)

// ✅ NEW: Payment split detail for co-seller stores
data class PaymentSplit(
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    @get:PropertyName("split_percentage")
    @set:PropertyName("split_percentage")
    var splitPercentage: Double = 0.0,

    @get:PropertyName("split_amount")
    @set:PropertyName("split_amount")
    var splitAmount: Double = 0.0,

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = PaymentStatus.PENDING.toString()
)

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUND_PENDING,      // ✅ NEW: Buyer submitted refund request
    REFUND_PROCESSING,   // ✅ NEW: Seller/admin approved, processing
    REFUNDED,            // ✅ NEW: Refund completed
    REFUND_REJECTED;     // ✅ NEW: Refund rejected

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        PENDING -> "Pending"
        PROCESSING -> "Processing"
        COMPLETED -> "Completed"
        FAILED -> "Failed"
        REFUND_PENDING -> "Refund Pending"
        REFUND_PROCESSING -> "Refund Processing"
        REFUNDED -> "Refunded"
        REFUND_REJECTED -> "Refund Rejected"
    }

    fun getStatusColor(): String = when (this) {
        PENDING -> "#FFA500"           // Orange
        PROCESSING -> "#4169E1"        // Royal Blue
        COMPLETED -> "#28A745"         // Green
        FAILED -> "#DC3545"            // Red
        REFUND_PENDING -> "#FFA500"    // Orange
        REFUND_PROCESSING -> "#2196F3" // Blue
        REFUNDED -> "#9C27B0"          // Purple
        REFUND_REJECTED -> "#757575"   // Gray
    }
}

/* -------------------- Firestore Mappers -------------------- */
fun SellerPayment.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "order_id" to orderId,
    "co_seller_store_id" to coSellerStoreId,
    "store_name" to storeName,
    "buyer_id" to buyerId,
    "buyer_name" to buyerName,
    "amount" to amount,
    "payment_method" to paymentMethod,
    "transaction_id" to transactionId,
    "status" to status,
    "payment_date" to (paymentDate ?: 0L),
    "original_transaction_date" to (originalTransactionDate ?: getCreatedAtLong()),
    "items_count" to itemsCount,
    "items_details" to itemsDetails.map { it.toMap() },
    "created_at" to getCreatedAtLong(),
    "updated_at" to getUpdatedAtLong(),
    "refund_amount" to refundAmount,
    "refund_reason" to refundReason,
    "refund_date" to getRefundDateLong(),  // ✅ Use safe conversion helper
    "involved_seller_ids" to involvedSellerIds,
    "payment_splits" to paymentSplits.map { it.toMap() },
    "idempotency_key" to idempotencyKey,
    "request_id" to requestId
)

fun PaymentItemDetail.toMap(): Map<String, Any> = mapOf(
    "product_id" to productId,
    "product_title" to productTitle,
    "quantity" to quantity,
    "price" to price,
    "item_total" to itemTotal
)

fun PaymentSplit.toMap(): Map<String, Any> = mapOf(
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "split_percentage" to splitPercentage,
    "split_amount" to splitAmount,
    "status" to status
)

// ✅ Extension function for PaymentSplit.copy() to support immutable updates
fun PaymentSplit.copy(
    sellerId: String = this.sellerId,
    sellerName: String = this.sellerName,
    splitPercentage: Double = this.splitPercentage,
    splitAmount: Double = this.splitAmount,
    status: String = this.status
): PaymentSplit = PaymentSplit(
    sellerId = sellerId,
    sellerName = sellerName,
    splitPercentage = splitPercentage,
    splitAmount = splitAmount,
    status = status
)

/* -------------------- Helpers -------------------- */
fun SellerPayment.getStatusEnum(): PaymentStatus =
    try {
        PaymentStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        PaymentStatus.PENDING
    }

fun SellerPayment.getCreatedAtLong(): Long = when (createdAt) {
    is Long -> createdAt as Long
    is com.google.firebase.Timestamp -> (createdAt as com.google.firebase.Timestamp).toDate().time
    is Number -> (createdAt as Number).toLong()
    is String -> (createdAt as String).toLongOrNull() ?: 0L
    is Map<*, *> -> {
        val map = createdAt as Map<*, *>
        val seconds = (map["_seconds"] as? Long) ?: (map["seconds"] as? Long) ?: 0L
        val nanos = (map["_nanoseconds"] as? Long) ?: (map["nanoseconds"] as? Long) ?: 0L
        (seconds * 1000) + (nanos / 1_000_000)
    }
    null -> System.currentTimeMillis()
    else -> 0L
}

fun SellerPayment.getUpdatedAtLong(): Long = when (updatedAt) {
    is Long -> updatedAt as Long
    is com.google.firebase.Timestamp -> (updatedAt as com.google.firebase.Timestamp).toDate().time
    is Number -> (updatedAt as Number).toLong()
    is String -> (updatedAt as String).toLongOrNull() ?: 0L
    is Map<*, *> -> {
        val map = updatedAt as Map<*, *>
        val seconds = (map["_seconds"] as? Long) ?: (map["seconds"] as? Long) ?: 0L
        val nanos = (map["_nanoseconds"] as? Long) ?: (map["nanoseconds"] as? Long) ?: 0L
        (seconds * 1000) + (nanos / 1_000_000)
    }
    null -> System.currentTimeMillis()
    else -> 0L
}

fun SellerPayment.getPaymentDateLong(): Long = paymentDate ?: 0L

// ✅ NEW: Get the most accurate transaction date for display (SELLER-CENTRIC)
// Priority: payment_date > original_transaction_date > created_at
// 
// Rationale for sellers:
//   - payment_date: When payment was actually confirmed/completed (most relevant for sellers)
//   - original_transaction_date: When order was placed (useful for historical context)
//   - created_at: When payment record was created (fallback)
//
// Sellers care about when they actually received/confirmed the payment, not when the
// order was originally placed.
fun SellerPayment.getDisplayDate(): Long {
    return when {
        paymentDate != null -> paymentDate!!
        originalTransactionDate != null -> originalTransactionDate!!
        else -> getCreatedAtLong()
    }
}

// ✅ NEW: Get the most accurate transaction date for display (BUYER-CENTRIC)
// Priority: original_transaction_date > payment_date > created_at
//
// Rationale for buyers:
//   - original_transaction_date: When order was placed (most relevant for buyers)
//   - payment_date: When payment was confirmed (useful for payment tracking)
//   - created_at: When payment record was created (fallback)
//
// Buyers typically care about when they placed the order, not the payment processing date.
fun SellerPayment.getBuyerDisplayDate(): Long {
    return when {
        originalTransactionDate != null -> originalTransactionDate!!
        paymentDate != null -> paymentDate!!
        else -> getCreatedAtLong()
    }
}

// ✅ NEW: Safely convert refundDate (Any?) to Long
fun SellerPayment.getRefundDateLong(): Long = when (refundDate) {
    is Long -> refundDate as Long
    is com.google.firebase.Timestamp -> (refundDate as com.google.firebase.Timestamp).toDate().time
    is Number -> (refundDate as Number).toLong()
    is String -> (refundDate as String).toLongOrNull() ?: 0L
    is Map<*, *> -> {
        val map = refundDate as Map<*, *>
        val seconds = (map["_seconds"] as? Long) ?: (map["seconds"] as? Long) ?: 0L
        val nanos = (map["_nanoseconds"] as? Long) ?: (map["nanoseconds"] as? Long) ?: 0L
        (seconds * 1000) + (nanos / 1_000_000)
    }
    null -> 0L
    else -> 0L
}
