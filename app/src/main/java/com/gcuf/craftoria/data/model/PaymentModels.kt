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

    @get:PropertyName("items_count")
    @set:PropertyName("items_count")
    var itemsCount: Int = 0,

    @get:PropertyName("items_details")
    @set:PropertyName("items_details")
    var itemsDetails: List<PaymentItemDetail> = emptyList(),

    // Timestamps
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis(),

    // Refund info
    @get:PropertyName("refund_amount")
    @set:PropertyName("refund_amount")
    var refundAmount: Double = 0.0,

    @get:PropertyName("refund_reason")
    @set:PropertyName("refund_reason")
    var refundReason: String = "",

    @get:PropertyName("refund_date")
    @set:PropertyName("refund_date")
    var refundDate: Long? = null,

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
    REFUNDED;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        PENDING -> "Pending"
        PROCESSING -> "Processing"
        COMPLETED -> "Completed"
        FAILED -> "Failed"
        REFUNDED -> "Refunded"
    }

    fun getStatusColor(): String = when (this) {
        PENDING -> "#FFA500"      // Orange
        PROCESSING -> "#4169E1"   // Royal Blue
        COMPLETED -> "#28A745"    // Green
        FAILED -> "#DC3545"       // Red
        REFUNDED -> "#6C757D"     // Gray
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
    "items_count" to itemsCount,
    "items_details" to itemsDetails.map { it.toMap() },
    "created_at" to createdAt,
    "updated_at" to updatedAt,
    "refund_amount" to refundAmount,
    "refund_reason" to refundReason,
    "refund_date" to (refundDate ?: 0L),
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

/* -------------------- Helpers -------------------- */
fun SellerPayment.getStatusEnum(): PaymentStatus =
    try {
        PaymentStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        PaymentStatus.PENDING
    }

fun SellerPayment.getCreatedAtLong(): Long = createdAt

fun SellerPayment.getUpdatedAtLong(): Long = updatedAt

fun SellerPayment.getPaymentDateLong(): Long = paymentDate ?: 0L
