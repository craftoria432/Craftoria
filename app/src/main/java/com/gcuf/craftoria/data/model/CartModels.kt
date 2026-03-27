package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class CartItem(

    val id: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("product_id")
    @set:PropertyName("product_id")
    var productId: String = "",

    var product: Product = Product(),

    var quantity: Int = 1,

    // Current price (can be negotiated)
    var price: Double = 0.0,

    @get:PropertyName("original_price")
    @set:PropertyName("original_price")
    var originalPrice: Double = 0.0,

    @get:PropertyName("is_negotiated")
    @set:PropertyName("is_negotiated")
    var isNegotiated: Boolean = false,

    @get:PropertyName("negotiation_status")
    @set:PropertyName("negotiation_status")
    var negotiationStatus: NegotiationStatus? = null,

    @get:PropertyName("added_at")
    @set:PropertyName("added_at")
    var addedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "user_id" to userId,
            "product_id" to productId,
            "quantity" to quantity,
            "price" to price,
            "original_price" to originalPrice,
            "is_negotiated" to isNegotiated,
            "added_at" to addedAt
        )
        
        if (negotiationStatus != null) {
            map["negotiation_status"] = negotiationStatus!!.name
        }
        
        return map
    }
}
