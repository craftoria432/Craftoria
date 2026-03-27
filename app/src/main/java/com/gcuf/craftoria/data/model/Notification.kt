package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class Notification(
    val id: String = "",

    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    var title: String = "",
    var description: String = "",

    var category: String = NotificationCategory.SYSTEM.name, // ✅ Store as String

    @get:PropertyName("is_read")
    @set:PropertyName("is_read")
    var isRead: Boolean = false,

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("action_type")
    @set:PropertyName("action_type")
    var actionType: String = NotificationActionType.NONE.name, // ✅ Store as String

    @get:PropertyName("action_data")
    @set:PropertyName("action_data")
    var actionData: Map<String, String> = emptyMap(),

    @get:PropertyName("order_id")
    @set:PropertyName("order_id")
    var orderId: String = "",

    @get:PropertyName("store_id")
    @set:PropertyName("store_id")
    var storeId: String = "",

    @get:PropertyName("store_name")
    @set:PropertyName("store_name")
    var storeName: String = "",

    @get:PropertyName("inviter_name")
    @set:PropertyName("inviter_name")
    var inviterName: String = "",

    @get:PropertyName("member_count")
    @set:PropertyName("member_count")
    var memberCount: Int = 0,

    @get:PropertyName("product_id")
    @set:PropertyName("product_id")
    var productId: String = "",

    @get:PropertyName("product_name")
    @set:PropertyName("product_name")
    var productName: String = "",

    @get:PropertyName("sender_name")
    @set:PropertyName("sender_name")
    var senderName: String = "",

    @get:PropertyName("negotiation_price")
    @set:PropertyName("negotiation_price")
    var negotiationPrice: Double = 0.0,

    @get:PropertyName("buyer_name")
    @set:PropertyName("buyer_name")
    var buyerName: String = "",

    @get:PropertyName("rating_value")
    @set:PropertyName("rating_value")
    var ratingValue: Int = 0,

    @get:PropertyName("rating_review")
    @set:PropertyName("rating_review")
    var ratingReview: String = ""
) {
    // ✅ Helper computed properties for UI use
    val categoryEnum: NotificationCategory
        get() = try { NotificationCategory.valueOf(category.uppercase()) }
        catch (e: Exception) { NotificationCategory.SYSTEM }

    val actionTypeEnum: NotificationActionType
        get() = try { NotificationActionType.valueOf(actionType.uppercase()) }
        catch (e: Exception) { NotificationActionType.NONE }
}
enum class NotificationCategory {
    ALL,
    ORDERS,
    MESSAGES,
    PROMOTIONS,
    SYSTEM,
    REPORT,          // For report-related notifications
    ADMIN_MESSAGE,   // For admin messages to users
    PAYMENTS,        // For payment-related notifications
    STORE_RATING;    // For store rating notifications

    override fun toString(): String = name.lowercase()
}

enum class NotificationActionType {
    NONE,
    VIEW_ORDER,
    TRACK_ORDER,
    ACCEPT_INVITATION,
    DECLINE_INVITATION,
    VIEW_STORE,
    REPLY_MESSAGE,
    VIEW_PRODUCT,
    RATE_ORDER,
    VIEW_PROMOTIONS,
    VIEW_REPORT,     // View report details
    VIEW_PROFILE,    // View user profile
    VIEW_PAYMENT,    // View payment details
    VIEW_RATING;     // View store rating details

    override fun toString(): String = name.lowercase()
}

fun Notification.toMap(): Map<String, Any> = mapOf(
    "user_id" to userId,
    "title" to title,
    "description" to description,
    "category" to category.uppercase(),       // Always UPPERCASE
    "is_read" to isRead,
    "created_at" to createdAt,
    "action_type" to actionType.uppercase(),  // Always UPPERCASE
    "action_data" to actionData,
    "order_id" to orderId,
    "store_id" to storeId,
    "store_name" to storeName,
    "inviter_name" to inviterName,
    "member_count" to memberCount,
    "product_id" to productId,
    "product_name" to productName,
    "sender_name" to senderName,
    "negotiation_price" to negotiationPrice,
    "buyer_name" to buyerName,
    "rating_value" to ratingValue,
    "rating_review" to ratingReview
)