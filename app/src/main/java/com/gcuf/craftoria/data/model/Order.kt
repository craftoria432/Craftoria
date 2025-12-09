package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class Order(
    val id: String = "",

    @PropertyName("buyer_id")
    val buyerId: String = "",
    @PropertyName("buyer_name")
    val buyerName: String = "",
    @PropertyName("seller_id")
    val sellerId: String = "",
    @PropertyName("seller_name")
    val sellerName: String = "",
    @PropertyName("product_id")
    val productId: String = "",
    @PropertyName("product_title")
    val productTitle: String = "",
    @PropertyName("product_image")
    val productImage: String = "",

    val quantity: Int = 1,

    @PropertyName("total_price")
    val totalPrice: Double = 0.0,

    val status: OrderStatus = OrderStatus.PENDING,

    @PropertyName("shipping_address")
    val shippingAddress: String = "",
    @PropertyName("buyer_phone")
    val buyerPhone: String = "",

    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // ---------------- ADDED FIELDS (SAFE) ------------------

    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val shipping: Double = 0.0,
    val discount: Double = 0.0,

    @PropertyName("delivery_info")
    val deliveryInfo: DeliveryInfo = DeliveryInfo(),

    @PropertyName("payment_method")
    val paymentMethod: String = "",

    @PropertyName("tracking_id")
    val trackingId: String = "",
    @PropertyName("courier_name")
    val courierName: String = "",
    @PropertyName("courier_contact")
    val courierContact: String = "",
    @PropertyName("estimated_delivery")
    val estimatedDelivery: Long? = null,

    @PropertyName("timeline")
    val timeline: List<OrderTimeline> = emptyList()
)

data class OrderItem(
    @PropertyName("product_id")
    val productId: String = "",
    @PropertyName("product_title")
    val productTitle: String = "",
    @PropertyName("product_image")
    val productImage: String = "",
    @PropertyName("seller_name")
    val sellerName: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    @PropertyName("is_negotiated")
    val isNegotiated: Boolean = false
)

data class DeliveryInfo(
    @PropertyName("full_name")
    val fullName: String = "",
    @PropertyName("phone_number")
    val phoneNumber: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    @PropertyName("postal_code")
    val postalCode: String = ""
)

data class OrderTimeline(
    val title: String = "",
    val timestamp: Long = 0L,
    @PropertyName("is_completed")
    val isCompleted: Boolean = false
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    override fun toString(): String = name.lowercase()
}

fun Order.toMap(): Map<String, Any> = mapOf(
    // ORIGINAL FIELDS (UNCHANGED)
    "id" to id,
    "buyer_id" to buyerId,
    "buyer_name" to buyerName,
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "product_id" to productId,
    "product_title" to productTitle,
    "product_image" to productImage,
    "quantity" to quantity,
    "total_price" to totalPrice,
    "status" to status.toString(),
    "shipping_address" to shippingAddress,
    "buyer_phone" to buyerPhone,
    "created_at" to createdAt,
    "updated_at" to updatedAt,

    // NEW SAFE FIELDS ADDED
    "items" to items.map { it.toMap() },
    "subtotal" to subtotal,
    "shipping" to shipping,
    "discount" to discount,
    "delivery_info" to deliveryInfo.toMap(),
    "payment_method" to paymentMethod,

    "tracking_id" to trackingId,
    "courier_name" to courierName,
    "courier_contact" to courierContact,
    "estimated_delivery" to (estimatedDelivery ?: 0L),
    "timeline" to timeline.map { it.toMap() }
)

fun OrderItem.toMap(): Map<String, Any> = mapOf(
    "product_id" to productId,
    "product_title" to productTitle,
    "product_image" to productImage,
    "seller_name" to sellerName,
    "quantity" to quantity,
    "price" to price,
    "is_negotiated" to isNegotiated
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
    "timestamp" to timestamp,
    "is_completed" to isCompleted
)
