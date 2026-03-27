package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude

data class Product(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    // -------------------------------
    //       SELLER INFO
    // -------------------------------

    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    @get:PropertyName("seller_verified")
    @set:PropertyName("seller_verified")
    var sellerVerified: Boolean = false,

    @get:PropertyName("seller_member_since")
    @set:PropertyName("seller_member_since")
    var sellerMemberSince: String = "March 2024",

    // -------------------------------
    //       MEDIA
    // -------------------------------

    @get:PropertyName("image_urls")
    @set:PropertyName("image_urls")
    var imageUrls: List<String> = emptyList(),

    // -------------------------------
    //       NEGOTIATION
    // -------------------------------

    @get:PropertyName("is_negotiable")
    @set:PropertyName("is_negotiable")
    var isNegotiable: Boolean = false,

    @get:PropertyName("is_negotiated")
    @set:PropertyName("is_negotiated")
    var isNegotiated: Boolean = false,

    @get:PropertyName("negotiated_price")
    @set:PropertyName("negotiated_price")
    var negotiatedPrice: Double = 0.0,

    @get:PropertyName("minimum_price")
    @set:PropertyName("minimum_price")
    var minimumPrice: Double = 0.0,

    @get:PropertyName("auto_accept_price")
    @set:PropertyName("auto_accept_price")
    var autoAcceptPrice: Double = 0.0,

    @get:PropertyName("auto_accept_discount")
    @set:PropertyName("auto_accept_discount")
    var autoAcceptDiscount: Int = 0,

    // -------------------------------
    //       INVENTORY
    // -------------------------------

    val stock: Int = 0,

    @get:PropertyName("weight_kg")
    @set:PropertyName("weight_kg")
    var weightKg: Double = 0.0,

    val specifications: Map<String, String> = emptyMap(),

    // -------------------------------
    //       STATUS & METADATA
    // -------------------------------

    @get:PropertyName("is_draft")
    @set:PropertyName("is_draft")
    var isDraft: Boolean = false,

    @get:PropertyName("is_active")
    @set:PropertyName("is_active")
    var isActive: Boolean = true,

    // ✅ APPROVAL SYSTEM
    @get:PropertyName("approval_status")
    @set:PropertyName("approval_status")
    var approvalStatus: String = "pending", // pending, approved, rejected

    @get:PropertyName("rejection_reason")
    @set:PropertyName("rejection_reason")
    var rejectionReason: String? = null,

    @get:PropertyName("approved_at")
    @set:PropertyName("approved_at")
    var approvedAt: Any? = null,

    @get:PropertyName("approved_by")
    @set:PropertyName("approved_by")
    var approvedBy: String? = null,

    // ✅ NEW: Admin can remove products via web dashboard
    @get:PropertyName("is_removed")
    @set:PropertyName("is_removed")
    var isRemoved: Boolean? = false,

    @get:PropertyName("removed_reason")
    @set:PropertyName("removed_reason")
    var removedReason: String? = null,

    @get:PropertyName("removed_at")
    @set:PropertyName("removed_at")
    var removedAt: Any? = null,

    @get:PropertyName("removed_by")
    @set:PropertyName("removed_by")
    var removedBy: String? = null,

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Any? = null,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Any? = null,

    @get:PropertyName("view_count")
    @set:PropertyName("view_count")
    var viewCount: Int = 0,

    @get:PropertyName("sold_count")
    @set:PropertyName("sold_count")
    var soldCount: Int = 0,

    // Store ID for co-seller products (not in Firestore)
    @get:Exclude @set:Exclude
    var storeId: String = "",

    // ✅ NEW — SCREEN 10 STATS
    @get:PropertyName("like_count")
    @set:PropertyName("like_count")
    var likeCount: Int = 0,

    @get:PropertyName("share_count")
    @set:PropertyName("share_count")
    var shareCount: Int = 0,

    val tags: List<String> = emptyList(),

    // -------------------------------
    //       CO-SELLER
    // -------------------------------

    @get:PropertyName("co_seller_store_id")
    @set:PropertyName("co_seller_store_id")
    var coSellerStoreId: String = ""
)

// -----------------------------------------------------------
//     MAP CONVERTER (UPDATED for all fields)
// -----------------------------------------------------------
fun Product.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "description" to description,
    "price" to price,
    "category" to category,

    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "seller_verified" to sellerVerified,
    "seller_member_since" to sellerMemberSince,

    "image_urls" to imageUrls,

    "is_negotiable" to isNegotiable,
    "is_negotiated" to isNegotiated,
    "negotiated_price" to negotiatedPrice,
    "minimum_price" to minimumPrice,
    "auto_accept_price" to autoAcceptPrice,
    "auto_accept_discount" to autoAcceptDiscount,

    "stock" to stock,
    "weight_kg" to weightKg,
    "specifications" to specifications,

    "is_draft" to isDraft,
    "is_active" to isActive,
    "approval_status" to approvalStatus,
    "rejection_reason" to rejectionReason,
    "approved_at" to approvedAt,
    "approved_by" to approvedBy,
    "is_removed" to (isRemoved ?: false),
    "removed_reason" to removedReason,
    "removed_at" to removedAt,
    "removed_by" to removedBy,
    "created_at" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp()),
    "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
    "view_count" to viewCount,
    "sold_count" to soldCount,

    // ✅ NEW
    "like_count" to likeCount,
    "share_count" to shareCount,

    "tags" to tags,
    "co_seller_store_id" to coSellerStoreId
)



// -----------------------------------------------------------
//     NEGOTIATION OFFER MODEL
// -----------------------------------------------------------
data class NegotiationOffer(
    val id: String = "",

    @get:PropertyName("product_id")
    @set:PropertyName("product_id")
    var productId: String = "",

    @get:PropertyName("buyer_id")
    @set:PropertyName("buyer_id")
    var buyerId: String = "",

    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("offer_amount")
    @set:PropertyName("offer_amount")
    var offerAmount: Double = 0.0,

    @get:PropertyName("original_price")
    @set:PropertyName("original_price")
    var originalPrice: Double = 0.0,

    var status: NegotiationStatus = NegotiationStatus.PENDING,
    var message: String = "",

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("responded_at")
    @set:PropertyName("responded_at")
    var respondedAt: Long? = null
)

enum class NegotiationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    REJECTED,
    AUTO_ACCEPTED;

    override fun toString(): String = name.lowercase()
}


// Helper to convert Product timestamp fields
fun Product.getCreatedAtLong(): Long = when (val ts = createdAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    else -> System.currentTimeMillis()
}

fun Product.getUpdatedAtLong(): Long = when (val ts = updatedAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    else -> System.currentTimeMillis()
}

fun Product.getApprovedAtLong(): Long? = when (val ts = approvedAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    else -> null
}

fun Product.getRemovedAtLong(): Long? = when (val ts = removedAt) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    else -> null
}
