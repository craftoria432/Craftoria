package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Exclude

@IgnoreExtraProperties
data class Order(
    var id: String = "",

    // Buyer Information
    @get:PropertyName("buyer_id")
    @set:PropertyName("buyer_id")
    var buyerId: String = "",

    @get:PropertyName("buyer_name")
    @set:PropertyName("buyer_name")
    var buyerName: String = "",

    @get:PropertyName("buyer_phone")
    @set:PropertyName("buyer_phone")
    var buyerPhone: String = "",

    @get:PropertyName("buyer_avatar")
    @set:PropertyName("buyer_avatar")
    var buyerAvatar: String = "",

    // Seller Information
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    // Product Information (Legacy single product fields - kept for backward compatibility)
    @get:PropertyName("product_id")
    @set:PropertyName("product_id")
    var productId: String = "",

    @get:PropertyName("product_title")
    @set:PropertyName("product_title")
    var productTitle: String = "",

    @get:PropertyName("product_image")
    @set:PropertyName("product_image")
    var productImage: String = "",

    @get:PropertyName("product_price")
    @set:PropertyName("product_price")
    var productPrice: Double = 0.0,

    var quantity: Int = 1,

    // Order Items (Multi-product support)
    @get:PropertyName("items")
    @set:PropertyName("items")
    var items: List<OrderItem> = emptyList(),

    // Pricing
    var subtotal: Double = 0.0,
    var shipping: Double = 0.0,
    var discount: Double = 0.0,

    @get:PropertyName("total_price")
    @set:PropertyName("total_price")
    var totalPrice: Double = 0.0,

    // Additional pricing fields for compatibility
    @get:PropertyName("total_amount")
    @set:PropertyName("total_amount")
    var totalAmount: Double = 0.0,

    @get:PropertyName("shipping_cost")
    @set:PropertyName("shipping_cost")
    var shippingCost: Double? = null,

    // Co-Seller Store Information
    @get:PropertyName("co_seller_store_id")
    @set:PropertyName("co_seller_store_id")
    var coSellerStoreId: String = "",

    // Status
    var status: String = OrderStatus.NEW.toString(),

    // Refund Status (tracks refund lifecycle independently from order status)
    @get:PropertyName("refund_status")
    @set:PropertyName("refund_status")
    var refundStatus: String = OrderRefundStatus.NONE.toString(),
    
    // Viewed Status (for badge counts)
    @get:PropertyName("is_viewed")
    @set:PropertyName("is_viewed")
    var isViewed: Boolean = false,

    // Delivery Information
    @get:PropertyName("shipping_address")
    @set:PropertyName("shipping_address")
    var shippingAddress: String = "",

    @get:PropertyName("full_address")
    @set:PropertyName("full_address")
    var fullAddress: String = "",

    @get:PropertyName("delivery_info")
    @set:PropertyName("delivery_info")
    var deliveryInfo: DeliveryInfo = DeliveryInfo(),

    @get:PropertyName("payment_method")
    @set:PropertyName("payment_method")
    var paymentMethod: String = "Cash on Delivery",

    // Timestamps - Accept both Long and Timestamp for backward compatibility
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Any? = System.currentTimeMillis(),

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Any? = System.currentTimeMillis(),

    @get:PropertyName("order_placed_at")
    @set:PropertyName("order_placed_at")
    var orderPlacedAt: Any? = System.currentTimeMillis(),

    @get:PropertyName("processing_at")
    @set:PropertyName("processing_at")
    var processingAt: Any? = null,

    @get:PropertyName("shipped_at")
    @set:PropertyName("shipped_at")
    var shippedAt: Any? = null,

    @get:PropertyName("delivered_at")
    @set:PropertyName("delivered_at")
    var deliveredAt: Any? = null,

    @get:PropertyName("cancelled_at")
    @set:PropertyName("cancelled_at")
    var cancelledAt: Any? = null,

    // Shipping Details
    @get:PropertyName("tracking_id")
    @set:PropertyName("tracking_id")
    var trackingId: String = "",

    @get:PropertyName("tracking_number")
    @set:PropertyName("tracking_number")
    var trackingNumber: String = "",

    @get:PropertyName("courier_name")
    @set:PropertyName("courier_name")
    var courierName: String = "",

    @get:PropertyName("courier_contact")
    @set:PropertyName("courier_contact")
    var courierContact: String = "",

    @get:PropertyName("estimated_delivery")
    @set:PropertyName("estimated_delivery")
    var estimatedDelivery: Any? = null,

    @get:PropertyName("expected_delivery_date")
    @set:PropertyName("expected_delivery_date")
    var expectedDeliveryDate: Any? = null,

    // Rejection Details
    @get:PropertyName("rejection_reason")
    @set:PropertyName("rejection_reason")
    var rejectionReason: String = "",

    @get:PropertyName("rejection_details")
    @set:PropertyName("rejection_details")
    var rejectionDetails: String = "",

    // Timeline
    @get:PropertyName("timeline")
    @set:PropertyName("timeline")
    var timeline: List<OrderTimeline> = emptyList()
)

data class OrderItem(
    @get:PropertyName("product_id")
    @set:PropertyName("product_id")
    var productId: String = "",

    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    @get:PropertyName("product_title")
    @set:PropertyName("product_title")
    var productTitle: String = "",

    @get:PropertyName("product_image")
    @set:PropertyName("product_image")
    var productImage: String = "",

    var quantity: Int = 1,
    var price: Double = 0.0,

    @get:PropertyName("is_negotiated")
    @set:PropertyName("is_negotiated")
    var isNegotiated: Boolean = false,

    @get:PropertyName("payment_status")
    @set:PropertyName("payment_status")
    var paymentStatus: String = "pending"
)

data class DeliveryInfo(
    @get:PropertyName("full_name")
    @set:PropertyName("full_name")
    var fullName: String = "",

    @get:PropertyName("phone_number")
    @set:PropertyName("phone_number")
    var phoneNumber: String = "",

    var email: String = "",
    var address: String = "",
    var city: String = "",

    @get:PropertyName("postal_code")
    @set:PropertyName("postal_code")
    var postalCode: String = ""
)

data class OrderTimeline(
    var title: String = "",
    var timestamp: Any? = null,
    var date: String = "",

    @get:PropertyName("is_completed")
    @set:PropertyName("is_completed")
    var isCompleted: Boolean = false
)

enum class OrderStatus {
    NEW,
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        NEW -> "New Order"
        PENDING -> "Pending"
        CONFIRMED -> "Confirmed"
        PROCESSING -> "Processing"
        SHIPPED -> "Shipped"
        DELIVERED -> "Delivered"
        COMPLETED -> "Completed"
        CANCELLED -> "Cancelled"
    }
}

enum class OrderRefundStatus {
    NONE,
    REQUESTED,
    APPROVED,
    COMPLETED,
    REJECTED;

    override fun toString(): String = name.lowercase()

    fun getDisplayName(): String = when (this) {
        NONE -> "No Refund"
        REQUESTED -> "Refund Requested"
        APPROVED -> "Refund Approved"
        COMPLETED -> "Refunded"
        REJECTED -> "Refund Rejected"
    }
}

/* -------------------- Firestore Mappers -------------------- */
fun Order.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "buyer_id" to buyerId,
    "buyer_name" to buyerName,
    "buyer_phone" to buyerPhone,
    "buyer_avatar" to buyerAvatar,
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "product_id" to productId,
    "product_title" to productTitle,
    "product_image" to productImage,
    "product_price" to productPrice,
    "quantity" to quantity,
    "items" to items.map { it.toMap() },
    "subtotal" to subtotal,
    "shipping" to shipping,
    "discount" to discount,
    "total_price" to totalPrice,
    "total_amount" to totalAmount,
    "shipping_cost" to (shippingCost ?: 0.0),
    "co_seller_store_id" to coSellerStoreId,
    "status" to status,
    "refund_status" to refundStatus,
    "is_viewed" to isViewed,
    "shipping_address" to shippingAddress,
    "full_address" to fullAddress,
    "delivery_info" to deliveryInfo.toMap(),
    "payment_method" to paymentMethod,
    "created_at" to (createdAt ?: System.currentTimeMillis()),
    "updated_at" to (updatedAt ?: System.currentTimeMillis()),
    "order_placed_at" to (orderPlacedAt ?: System.currentTimeMillis()),
    "processing_at" to (processingAt ?: 0L),
    "shipped_at" to (shippedAt ?: 0L),
    "delivered_at" to (deliveredAt ?: 0L),
    "cancelled_at" to (cancelledAt ?: 0L),
    "tracking_id" to trackingId,
    "tracking_number" to trackingNumber,
    "courier_name" to courierName,
    "courier_contact" to courierContact,
    "estimated_delivery" to (estimatedDelivery ?: 0L),
    "expected_delivery_date" to (expectedDeliveryDate ?: 0L),
    "rejection_reason" to rejectionReason,
    "rejection_details" to rejectionDetails,
    "timeline" to timeline.map { it.toMap() }
)

fun OrderItem.toMap(): Map<String, Any> = mapOf(
    "product_id" to productId,
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "product_title" to productTitle,
    "product_image" to productImage,
    "quantity" to quantity,
    "price" to price,
    "is_negotiated" to isNegotiated,
    "payment_status" to paymentStatus
)

fun DeliveryInfo.toMap(): Map<String, Any> = mapOf(
    "full_name" to fullName,
    "phone_number" to phoneNumber,
    "email" to email,
    "address" to address,
    "city" to city,
    "postal_code" to postalCode
)

fun OrderTimeline.toMap(): Map<String, Any> = mapOf(
    "title" to title,
    "timestamp" to (timestamp ?: 0L),
    "date" to date,
    "is_completed" to isCompleted
)


/* -------------------- Helpers -------------------- */
fun Order.getStatusEnum(): OrderStatus =
    try {
        OrderStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        OrderStatus.PENDING
    }

fun Order.getRefundStatusEnum(): OrderRefundStatus =
    try {
        OrderRefundStatus.valueOf(refundStatus.uppercase())
    } catch (e: Exception) {
        OrderRefundStatus.NONE
    }

// Helper to convert Any? timestamp to Long
fun Order.getCreatedAtLong(): Long = when (val ts = createdAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> {
        // Try to parse string as Long
        try {
            ts.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    is Map<*, *> -> {
        // Handle Firestore Timestamp object format
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    else -> System.currentTimeMillis()
}

fun Order.getUpdatedAtLong(): Long = when (val ts = updatedAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> {
        try {
            ts.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    else -> System.currentTimeMillis()
}

fun Order.getOrderPlacedAtLong(): Long = when (val ts = orderPlacedAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> {
        try {
            ts.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    else -> System.currentTimeMillis()
}

fun Order.getProcessingAtLong(): Long = when (val ts = processingAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> ts.toLongOrNull() ?: 0L
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            0L
        }
    }
    else -> 0L
}

fun Order.getShippedAtLong(): Long = when (val ts = shippedAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> ts.toLongOrNull() ?: 0L
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            0L
        }
    }
    else -> 0L
}

fun Order.getDeliveredAtLong(): Long = when (val ts = deliveredAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> ts.toLongOrNull() ?: 0L
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            0L
        }
    }
    else -> 0L
}

fun Order.getCancelledAtLong(): Long = when (val ts = cancelledAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> ts.toLongOrNull() ?: 0L
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            0L
        }
    }
    else -> 0L
}

fun Order.getEstimatedDeliveryLong(): Long = when (val ts = estimatedDelivery) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> ts.toLongOrNull() ?: 0L
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            0L
        }
    }
    else -> 0L
}

fun Order.getExpectedDeliveryDateLong(): Long = when (val ts = expectedDeliveryDate) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    is String -> ts.toLongOrNull() ?: 0L
    is Map<*, *> -> {
        try {
            val seconds = (ts["_seconds"] as? Long) ?: (ts["seconds"] as? Long) ?: 0L
            val nanoseconds = (ts["_nanoseconds"] as? Long) ?: (ts["nanoseconds"] as? Long) ?: 0L
            (seconds * 1000) + (nanoseconds / 1000000)
        } catch (e: Exception) {
            0L
        }
    }
    else -> 0L
}