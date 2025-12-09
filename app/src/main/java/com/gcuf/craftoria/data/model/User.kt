package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class User(

    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.BUYER,
    val phone: String = "",
    val address: String = "",

    @get:PropertyName("profile_image")
    @set:PropertyName("profile_image")
    var profileImage: String = "",

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("store_name")
    @set:PropertyName("store_name")
    var storeName: String = "",

    @get:PropertyName("store_description")
    @set:PropertyName("store_description")
    var storeDescription: String = "",

    var verified: Boolean = false,

    @get:PropertyName("verification_status")
    @set:PropertyName("verification_status")
    var verificationStatus: VerificationStatus = VerificationStatus.NOT_SUBMITTED,

    @get:PropertyName("verification_photo_url")
    @set:PropertyName("verification_photo_url")
    var verificationPhotoUrl: String = "",

    @get:PropertyName("rejection_reason")
    @set:PropertyName("rejection_reason")
    var rejectionReason: String = "",

    @get:PropertyName("main_seller_id")
    @set:PropertyName("main_seller_id")
    var mainSellerId: String = ""
)

enum class UserRole {
    BUYER, SELLER, CO_SELLER;

    companion object {
        fun fromString(value: String?) =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BUYER
    }
}

enum class VerificationStatus {
    NOT_SUBMITTED, PENDING, APPROVED, REJECTED;

    companion object {
        fun fromString(value: String?) =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NOT_SUBMITTED
    }
}

fun User.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "email" to email,
    "name" to name,
    "role" to role.name,
    "phone" to phone,
    "address" to address,
    "profile_image" to profileImage,
    "created_at" to createdAt,
    "store_name" to storeName,
    "store_description" to storeDescription,
    "verified" to verified,
    "verification_status" to verificationStatus.name,
    "verification_photo_url" to verificationPhotoUrl,
    "rejection_reason" to rejectionReason,
    "main_seller_id" to mainSellerId
)

