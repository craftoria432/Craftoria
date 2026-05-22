package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName
import com.gcuf.craftoria.data.model.NegotiationStatus

data class Chat(
    val id: String = "",
    @PropertyName("participant_ids")
    val participantIds: List<String> = emptyList(),
    @PropertyName("participant_names")
    val participantNames: Map<String, String> = emptyMap(),
    @PropertyName("participant_avatars")
    val participantAvatars: Map<String, String> = emptyMap(),
    @PropertyName("participant_roles")
    val participantRoles: Map<String, String> = emptyMap(),  // ✅ NEW: userId -> role (BUYER/SELLER)
    @PropertyName("chat_type")
    val chatType: String = "buyer_seller",  // ✅ NEW: buyer_seller, seller_seller, buyer_buyer
    @PropertyName("last_message")
    val lastMessage: String = "",
    @PropertyName("last_message_time")
    val lastMessageTime: Long = System.currentTimeMillis(),
    @PropertyName("last_message_sender_id")
    val lastMessageSenderId: String = "",
    @PropertyName("unread_count")
    val unreadCount: Map<String, Int> = emptyMap(),
    @PropertyName("is_blocked")
    val isBlocked: Boolean = false,
    @PropertyName("blocked_by")
    val blockedBy: String = "",
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("last_role_update")
    val lastRoleUpdate: Long = System.currentTimeMillis(),  // ✅ NEW: Track when roles were last synced
    // Product context for first-time chats
    @PropertyName("initial_product_id")
    val initialProductId: String = ""
)

data class Message(
    val id: String = "",
    @PropertyName("chat_id")
    val chatId: String = "",
    @PropertyName("sender_id")
    val senderId: String = "",
    @PropertyName("sender_name")
    val senderName: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    @PropertyName("is_read")
    val isRead: Boolean = false,
    @PropertyName("read_at")
    val readAt: Long = 0,
    @PropertyName("delivered_at")
    val deliveredAt: Long = 0,  // ✅ NEW: Track when message was delivered
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // For product sharing
    @PropertyName("product_id")
    val productId: String = "",
    @PropertyName("product_name")
    val productName: String = "",
    @PropertyName("product_price")
    val productPrice: Double = 0.0,
    @PropertyName("product_image")
    val productImage: String = "",

    // For order updates
    @PropertyName("order_id")
    val orderId: String = "",
    @PropertyName("order_status")
    val orderStatus: String = "",

    // For negotiation
    @PropertyName("negotiation_price")
    val negotiationPrice: Double = 0.0,
    @PropertyName("negotiation_status")
    val negotiationStatus: NegotiationStatus = NegotiationStatus.PENDING,

    // For images
    @PropertyName("image_url")
    val imageUrl: String = ""
)

enum class MessageType {
    TEXT,
    IMAGE,
    PRODUCT,
    ORDER_UPDATE,
    NEGOTIATION;

    override fun toString(): String = name.lowercase()
}

// REMOVED: This enum is already declared elsewhere in your codebase
// Keep only ONE declaration of NegotiationStatus (likely in a shared models file)
// If you need it here, make sure to remove the other declaration

fun Chat.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "participant_ids" to participantIds,
    "participant_names" to participantNames,
    "participant_avatars" to participantAvatars,
    "participant_roles" to participantRoles,  // ✅ NEW
    "chat_type" to chatType,  // ✅ NEW
    "last_message" to lastMessage,
    "last_message_time" to lastMessageTime,
    "last_message_sender_id" to lastMessageSenderId,
    "unread_count" to unreadCount,
    "is_blocked" to isBlocked,
    "blocked_by" to blockedBy,
    "created_at" to createdAt,
    "last_role_update" to lastRoleUpdate,  // ✅ NEW
    "initial_product_id" to initialProductId
)

fun Message.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "chat_id" to chatId,
    "sender_id" to senderId,
    "sender_name" to senderName,
    "content" to content,
    "type" to type.toString(),
    "is_read" to isRead,
    "read_at" to readAt,
    "delivered_at" to deliveredAt,  // ✅ Include delivered_at in toMap
    "created_at" to createdAt,
    "product_id" to productId,
    "product_name" to productName,
    "product_price" to productPrice,
    "product_image" to productImage,
    "order_id" to orderId,
    "order_status" to orderStatus,
    "negotiation_price" to negotiationPrice,
    "negotiation_status" to negotiationStatus.toString(),
    "image_url" to imageUrl
)