package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class SellerApplication(
    val id: String = "",
    @PropertyName("user_id")
    val userId: String = "",
    @PropertyName("user_name")
    val userName: String = "",
    @PropertyName("user_email")
    val userEmail: String = "",
    @PropertyName("verification_photo_url")
    val verificationPhotoUrl: String = "",
    @PropertyName("status")
    val status: String = "pending",  // pending, approved, rejected
    @PropertyName("admin_notes")
    val adminNotes: String = "",
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("reviewed_at")
    val reviewedAt: Long = 0L,
    @PropertyName("estimated_review_time")
    val estimatedReviewTime: String = "24 - 48 hours"
)



fun SellerApplication.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "user_id" to userId,
    "user_name" to userName,
    "user_email" to userEmail,
    "verification_photo_url" to verificationPhotoUrl,
    "status" to status,
    "admin_notes" to adminNotes,
    "created_at" to createdAt,
    "reviewed_at" to reviewedAt,
    "estimated_review_time" to estimatedReviewTime
)
