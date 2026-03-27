package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class StoreRating(
    val id: String = "",

    @get:PropertyName("store_id")
    @set:PropertyName("store_id")
    var storeId: String = "",

    @get:PropertyName("buyer_id")
    @set:PropertyName("buyer_id")
    var buyerId: String = "",

    val rating: Int = 0,  // 1-5
    val review: String = "",

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = 0
)

fun StoreRating.toMap(): Map<String, Any?> = mapOf(
    "store_id" to storeId,
    "buyer_id" to buyerId,
    "rating" to rating,
    "review" to review,
    "created_at" to (createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()),
    "updated_at" to System.currentTimeMillis()
)
