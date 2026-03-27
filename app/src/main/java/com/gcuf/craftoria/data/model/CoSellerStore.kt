package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class CoSellerStore(
    val id: String = "",

    @get:PropertyName("store_name")
    @set:PropertyName("store_name")
    var storeName: String = "",

    @get:PropertyName("store_description")
    @set:PropertyName("store_description")
    var storeDescription: String = "",

    @get:PropertyName("store_logo")
    @set:PropertyName("store_logo")
    var storeLogo: String = "",

    @get:PropertyName("store_banner")
    @set:PropertyName("store_banner")
    var storeBanner: String = "",

    @get:PropertyName("owner_id")
    @set:PropertyName("owner_id")
    var ownerId: String = "",

    @get:PropertyName("owner_name")
    @set:PropertyName("owner_name")
    var ownerName: String = "",

    @get:PropertyName("member_ids")
    @set:PropertyName("member_ids")
    var memberIds: List<String> = emptyList(),

    // ✅ NEW: Payment split configuration (seller_id -> percentage)
    @get:PropertyName("payment_split_config")
    @set:PropertyName("payment_split_config")
    var paymentSplitConfig: Map<String, Double> = emptyMap(),

    @get:PropertyName("product_count")
    @set:PropertyName("product_count")
    var productCount: Int = 0,

    @get:PropertyName("member_count")
    @set:PropertyName("member_count")
    var memberCount: Int = 0,

    @get:PropertyName("average_rating")
    @set:PropertyName("average_rating")
    var averageRating: Double = 0.0,

    @get:PropertyName("rating_count")
    @set:PropertyName("rating_count")
    var ratingCount: Int = 0,

    @get:PropertyName("is_active")
    @set:PropertyName("is_active")
    var isActive: Boolean = true,

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Any? = null,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Any? = null
)

data class StoreMember(
    val id: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("user_name")
    @set:PropertyName("user_name")
    var userName: String = "",

    @get:PropertyName("user_email")
    @set:PropertyName("user_email")
    var userEmail: String = "",

    @get:PropertyName("user_avatar")
    @set:PropertyName("user_avatar")
    var userAvatar: String = "",

    @get:PropertyName("store_id")
    @set:PropertyName("store_id")
    var storeId: String = "",

    @get:PropertyName("is_owner")
    @set:PropertyName("is_owner")
    var isOwner: Boolean = false,

    @get:PropertyName("joined_at")
    @set:PropertyName("joined_at")
    var joinedAt: Long = System.currentTimeMillis()
)

data class StoreInvitation(
    val id: String = "",

    @get:PropertyName("store_id")
    @set:PropertyName("store_id")
    var storeId: String = "",

    @get:PropertyName("store_name")
    @set:PropertyName("store_name")
    var storeName: String = "",

    @get:PropertyName("inviter_id")
    @set:PropertyName("inviter_id")
    var inviterId: String = "",

    @get:PropertyName("inviter_name")
    @set:PropertyName("inviter_name")
    var inviterName: String = "",

    @get:PropertyName("invitee_email")
    @set:PropertyName("invitee_email")
    var inviteeEmail: String = "",

    @get:PropertyName("invitee_id")
    @set:PropertyName("invitee_id")
    var inviteeId: String = "",

    @get:PropertyName("invitee_name")
    @set:PropertyName("invitee_name")
    var inviteeName: String = "",  // ✅ ADD THIS

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: InvitationStatus = InvitationStatus.PENDING,

    @get:PropertyName("sent_at")
    @set:PropertyName("sent_at")
    var sentAt: Long = System.currentTimeMillis(),

    @get:PropertyName("responded_at")
    @set:PropertyName("responded_at")
    var respondedAt: Long = 0L,

    @get:PropertyName("is_registered_user")
    @set:PropertyName("is_registered_user")
    var isRegisteredUser: Boolean = false  // ✅ ADD THIS
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED;

    override fun toString(): String = when(this) {
        PENDING -> "Pending"
        ACCEPTED -> "Accepted"
        DECLINED -> "Declined"
    }
}

// Extension function to convert CoSellerStore to Map for Firestore
fun CoSellerStore.toMap(): Map<String, Any> = mapOf(
    "store_name" to storeName,
    "store_description" to storeDescription,
    "store_logo" to storeLogo,
    "store_banner" to storeBanner,
    "owner_id" to ownerId,
    "owner_name" to ownerName,
    "member_ids" to memberIds,
    "payment_split_config" to paymentSplitConfig,
    "product_count" to productCount,
    "member_count" to memberCount,
    "average_rating" to averageRating,
    "rating_count" to ratingCount,
    "is_active" to isActive,
    "created_at" to (createdAt ?: com.google.firebase.firestore.FieldValue.serverTimestamp()),
    "updated_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
)

// Extension function to convert StoreMember to Map for Firestore
fun StoreMember.toMap(): Map<String, Any> = mapOf(
    "user_id" to userId,
    "user_name" to userName,
    "user_email" to userEmail,
    "user_avatar" to userAvatar,
    "store_id" to storeId,
    "is_owner" to isOwner,
    "joined_at" to joinedAt
)

// Extension function to convert StoreInvitation to Map for Firestore
fun StoreInvitation.toMap(): Map<String, Any> = mapOf(
    "store_id" to storeId,
    "store_name" to storeName,
    "inviter_id" to inviterId,
    "inviter_name" to inviterName,
    "invitee_email" to inviteeEmail,
    "invitee_id" to inviteeId,
    "invitee_name" to inviteeName,  // ✅ ADD THIS
    "status" to status.name,
    "sent_at" to sentAt,
    "responded_at" to respondedAt,
    "is_registered_user" to isRegisteredUser  // ✅ ADD THIS
)