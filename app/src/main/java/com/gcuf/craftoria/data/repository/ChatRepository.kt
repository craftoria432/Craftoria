package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.Chat
import com.gcuf.craftoria.data.model.Message
import com.gcuf.craftoria.data.model.MessageType
import com.gcuf.craftoria.data.model.NegotiationStatus
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.toMap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatsCollection = db.collection("chats")
    private val messagesCollection = db.collection("messages")
    private val usersCollection = db.collection("users")

    companion object {
        private const val TAG = "ChatRepository"
    }

    suspend fun getOrCreateChat(
        currentUserId: String,
        currentUserName: String,
        otherUserId: String,
        otherUserName: String
    ): Result<String> {
        return try {
            val participantIds = listOf(currentUserId, otherUserId).sorted()

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🔍 GET OR CREATE CHAT")
            Log.d(TAG, "   Current User: $currentUserId ($currentUserName)")
            Log.d(TAG, "   Other User: $otherUserId ($otherUserName)")
            Log.d(TAG, "   Sorted IDs: $participantIds")
            Log.d(TAG, "═══════════════════════════════════════")

            val snapshot = chatsCollection
                .whereArrayContains("participant_ids", currentUserId)
                .get()
                .await()

            Log.d(TAG, "📦 Query returned ${snapshot.documents.size} chats containing user $currentUserId")

            val existingChat = snapshot.documents.firstOrNull { doc ->
                try {
                    val docParticipants = doc.get("participant_ids") as? List<*>
                    val sortedDocParticipants = docParticipants?.map { it.toString() }?.sorted()

                    Log.d(TAG, "   Checking chat ${doc.id}")
                    Log.d(TAG, "      Doc participants: $sortedDocParticipants")
                    Log.d(TAG, "      Target participants: $participantIds")

                    val matches = sortedDocParticipants == participantIds

                    if (matches) {
                        Log.d(TAG, "      ✅ MATCH! This is the chat we're looking for")
                    } else {
                        Log.d(TAG, "      ❌ No match")
                    }

                    matches
                } catch (e: Exception) {
                    Log.e(TAG, "      ❌ Error checking chat ${doc.id}", e)
                    false
                }
            }

            val chatId = if (existingChat != null) {
                Log.d(TAG, "✅ EXISTING CHAT FOUND: ${existingChat.id}")
                existingChat.id
            } else {
                Log.d(TAG, "📝 NO EXISTING CHAT - CREATING NEW ONE")

                // Fetch profile pictures for both users
                val participantAvatars = fetchParticipantAvatars(listOf(currentUserId, otherUserId))

                val chatData = mapOf(
                    "participant_ids" to participantIds,
                    "participant_names" to mapOf(
                        currentUserId to currentUserName,
                        otherUserId to otherUserName
                    ),
                    "participant_avatars" to participantAvatars,
                    "unread_count" to mapOf(
                        currentUserId to 0,
                        otherUserId to 0
                    ),
                    "last_message" to "",
                    "last_message_time" to System.currentTimeMillis(),
                    "last_message_sender_id" to "",
                    "is_blocked" to false,
                    "blocked_by" to "",
                    "created_at" to System.currentTimeMillis()
                )

                Log.d(TAG, "   Chat data: $chatData")

                val docRef = chatsCollection.add(chatData).await()
                Log.d(TAG, "✅ NEW CHAT CREATED: ${docRef.id}")
                docRef.id
            }

            // Always sync profile pictures for existing chats (in case they were updated)
            syncParticipantAvatars(chatId, participantIds)

            Log.d(TAG, "═══════════════════════════════════════")
            Result.success(chatId)

        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED TO GET/CREATE CHAT", e)
            Log.e(TAG, "═══════════════════════════════════════")
            Result.failure(e)
        }
    }

    /**
     * Fetches profile pictures for all participants
     * This is called when creating a new chat
     */
    private suspend fun fetchParticipantAvatars(userIds: List<String>): Map<String, String> {
        return try {
            val avatars = mutableMapOf<String, String>()
            userIds.forEach { userId ->
                try {
                    val userDoc = usersCollection.document(userId).get().await()
                    val profileImage = userDoc.getString("profile_image") ?: ""
                    if (profileImage.isNotEmpty()) {
                        avatars[userId] = profileImage
                        Log.d(TAG, "   📸 Fetched avatar for $userId: $profileImage")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Failed to fetch avatar for $userId", e)
                }
            }
            avatars
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch participant avatars", e)
            emptyMap()
        }
    }

    /**
     * Syncs profile pictures for all participants in a chat
     * This is called every time a chat is opened to ensure avatars are up-to-date
     */
    private suspend fun syncParticipantAvatars(chatId: String, participantIds: List<String>) {
        try {
            val avatars = fetchParticipantAvatars(participantIds)
            if (avatars.isNotEmpty()) {
                chatsCollection.document(chatId)
                    .update("participant_avatars", avatars)
                    .await()
                Log.d(TAG, "✅ Synced ${avatars.size} participant avatars for chat $chatId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to sync participant avatars for chat $chatId", e)
        }
    }

    /**
     * Updates a single participant's avatar in all their chats
     * Call this when a user updates their profile picture
     */
    suspend fun updateParticipantAvatar(userId: String, newAvatarUrl: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating avatar for user $userId in all chats")

            val userChats = chatsCollection
                .whereArrayContains("participant_ids", userId)
                .get()
                .await()

            val batch = db.batch()
            userChats.documents.forEach { doc ->
                val currentAvatars = doc.get("participant_avatars") as? Map<*, *> ?: emptyMap<String, String>()
                val updatedAvatars = currentAvatars.toMutableMap()
                updatedAvatars[userId] = newAvatarUrl

                batch.update(doc.reference, "participant_avatars", updatedAvatars)
            }

            batch.commit().await()
            Log.d(TAG, "✅ Updated avatar in ${userChats.size()} chats")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update participant avatar", e)
            Result.failure(e)
        }
    }

    suspend fun getChat(chatId: String): Result<Chat> {
        return try {
            val doc = chatsCollection.document(chatId).get().await()
            val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)
                ?: throw Exception("Chat not found")
            Result.success(chat)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chat", e)
            Result.failure(e)
        }
    }

    fun getMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        Log.d(TAG, "🎧 Starting messages listener for chat: $chatId")

        val listener = messagesCollection
            .whereEqualTo("chat_id", chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Messages listener error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "📭 No messages in chat yet")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(TAG, "📬 Snapshot has ${snapshot.documents.size} documents")

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val typeString = doc.getString("type") ?: "text"
                        val messageType = when (typeString.lowercase()) {
                            "text" -> MessageType.TEXT
                            "image" -> MessageType.IMAGE
                            "product" -> MessageType.PRODUCT
                            "order_update" -> MessageType.ORDER_UPDATE
                            "negotiation" -> MessageType.NEGOTIATION
                            else -> MessageType.TEXT
                        }

                        val negotiationStatusString = doc.getString("negotiation_status") ?: "pending"
                        val negotiationStatus = when (negotiationStatusString.lowercase()) {
                            "accepted" -> NegotiationStatus.ACCEPTED
                            "declined" -> NegotiationStatus.DECLINED
                            else -> NegotiationStatus.PENDING
                        }

                        Message(
                            id = doc.id,
                            chatId = doc.getString("chat_id") ?: "",
                            senderId = doc.getString("sender_id") ?: "",
                            senderName = doc.getString("sender_name") ?: "",
                            content = doc.getString("content") ?: "",
                            type = messageType,
                            isRead = doc.getBoolean("is_read") ?: false,
                            readAt = doc.getLong("read_at") ?: 0L,
                            deliveredAt = doc.getLong("delivered_at") ?: 0L,
                            createdAt = doc.getLong("created_at") ?: 0L,
                            productId = doc.getString("product_id") ?: "",
                            productName = doc.getString("product_name") ?: "",
                            productPrice = doc.getDouble("product_price") ?: 0.0,
                            productImage = doc.getString("product_image") ?: "",
                            orderId = doc.getString("order_id") ?: "",
                            orderStatus = doc.getString("order_status") ?: "",
                            negotiationPrice = doc.getDouble("negotiation_price") ?: 0.0,
                            negotiationStatus = negotiationStatus,
                            imageUrl = doc.getString("image_url") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "   ❌ Error parsing message ${doc.id}", e)
                        null
                    }
                }

                val sortedMessages = messages.sortedBy { it.createdAt }
                Log.d(TAG, "✅ Sending ${sortedMessages.size} messages to ViewModel")
                trySend(sortedMessages)
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing messages listener for chat: $chatId")
            listener.remove()
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String
    ): Result<String> {
        return try {
            Log.d(TAG, "📤 Sending message - chatId: $chatId, sender: $senderId")

            val timestamp = System.currentTimeMillis()

            val messageData = hashMapOf(
                "chat_id" to chatId,
                "sender_id" to senderId,
                "sender_name" to senderName,
                "content" to content,
                "type" to "text",
                "is_read" to false,
                "read_at" to 0L,
                "delivered_at" to 0L,
                "created_at" to timestamp,
                "product_id" to "",
                "product_name" to "",
                "product_price" to 0.0,
                "product_image" to "",
                "order_id" to "",
                "order_status" to "",
                "negotiation_price" to 0.0,
                "negotiation_status" to "pending",
                "image_url" to ""
            )

            val docRef = messagesCollection.add(messageData).await()
            Log.d(TAG, "✅ Message saved with ID: ${docRef.id}")

            updateLastMessage(chatId, senderId, content)

            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send message", e)
            Result.failure(e)
        }
    }

    suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        imageUrl: String
    ): Result<String> {
        return try {
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = "📷 Photo",
                type = MessageType.IMAGE,
                imageUrl = imageUrl
            )

            val docRef = messagesCollection.add(message.toMap()).await()
            updateLastMessage(chatId, senderId, "📷 Photo")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send image", e)
            Result.failure(e)
        }
    }

    suspend fun shareProduct(
        chatId: String,
        senderId: String,
        senderName: String,
        product: Product
    ): Result<String> {
        return try {
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = "Shared a product",
                type = MessageType.PRODUCT,
                productId = product.id,
                productName = product.title,
                productPrice = product.price,
                productImage = product.imageUrls.firstOrNull() ?: ""
            )

            val docRef = messagesCollection.add(message.toMap()).await()
            updateLastMessage(chatId, senderId, "📦 Shared a product")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to share product", e)
            Result.failure(e)
        }
    }

    suspend fun sendOrderUpdate(
        chatId: String,
        senderId: String,
        senderName: String,
        orderId: String,
        orderStatus: String
    ): Result<String> {
        return try {
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = "Order update: $orderStatus",
                type = MessageType.ORDER_UPDATE,
                orderId = orderId,
                orderStatus = orderStatus
            )

            val docRef = messagesCollection.add(message.toMap()).await()
            updateLastMessage(chatId, senderId, "📦 Order update")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send order update", e)
            Result.failure(e)
        }
    }

    suspend fun sendNegotiation(
        chatId: String,
        senderId: String,
        senderName: String,
        productName: String,
        offeredPrice: Double
    ): Result<String> {
        return try {
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                content = "Negotiation request for $productName",
                type = MessageType.NEGOTIATION,
                productName = productName,
                negotiationPrice = offeredPrice,
                negotiationStatus = NegotiationStatus.PENDING
            )

            val docRef = messagesCollection.add(message.toMap()).await()
            updateLastMessage(chatId, senderId, "💰 Negotiation request")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send negotiation", e)
            Result.failure(e)
        }
    }

    suspend fun updateNegotiationStatus(
        messageId: String,
        status: NegotiationStatus
    ): Result<Unit> {
        return try {
            messagesCollection.document(messageId)
                .update("negotiation_status", status.toString())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update negotiation", e)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "📖 Marking messages as read for chat: $chatId, user: $userId")

            val allMessages = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .get()
                .await()

            val unreadMessages = allMessages.documents.filter { doc ->
                val isRead = doc.getBoolean("is_read") ?: true
                val senderId = doc.getString("sender_id") ?: ""
                !isRead && senderId != userId
            }

            if (unreadMessages.isNotEmpty()) {
                val batch = db.batch()
                val currentTime = System.currentTimeMillis()
                unreadMessages.forEach { doc ->
                    batch.update(doc.reference, mapOf(
                        "is_read" to true,
                        "read_at" to currentTime,
                        "delivered_at" to currentTime
                    ))
                }
                batch.commit().await()
                Log.d(TAG, "   ✅ Marked ${unreadMessages.size} messages as read")
            }

            chatsCollection.document(chatId)
                .update("unread_count.$userId", 0)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark messages as read", e)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsDelivered(chatId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "📬 Marking messages as delivered for chat: $chatId, user: $userId")

            val allMessages = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .get()
                .await()

            val undeliveredMessages = allMessages.documents.filter { doc ->
                val deliveredAt = doc.getLong("delivered_at") ?: 0L
                val senderId = doc.getString("sender_id") ?: ""
                deliveredAt == 0L && senderId != userId
            }

            if (undeliveredMessages.isNotEmpty()) {
                val batch = db.batch()
                val currentTime = System.currentTimeMillis()
                undeliveredMessages.forEach { doc ->
                    batch.update(doc.reference, mapOf("delivered_at" to currentTime))
                }
                batch.commit().await()
                Log.d(TAG, "   ✅ Marked ${undeliveredMessages.size} messages as delivered")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark messages as delivered", e)
            Result.failure(e)
        }
    }

    suspend fun blockUser(chatId: String, blockerId: String): Result<Unit> {
        return try {
            chatsCollection.document(chatId)
                .update(mapOf("is_blocked" to true, "blocked_by" to blockerId))
                .await()
            Log.d(TAG, "User blocked")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block user", e)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(chatId: String): Result<Unit> {
        return try {
            chatsCollection.document(chatId)
                .update(mapOf("is_blocked" to false, "blocked_by" to ""))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unblock user", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting message: $messageId")
            messagesCollection.document(messageId).delete().await()
            Log.d(TAG, "✅ Message deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete message", e)
            Result.failure(e)
        }
    }

    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting entire chat: $chatId")

            val messagesSnapshot = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .get()
                .await()

            Log.d(TAG, "   Found ${messagesSnapshot.documents.size} messages to delete")

            messagesSnapshot.documents.forEach { messageDoc ->
                messageDoc.reference.delete().await()
            }

            chatsCollection.document(chatId).delete().await()

            Log.d(TAG, "✅ Chat and all messages deleted successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete chat", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAllChats(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting all chats for user: $userId")

            val chatsSnapshot = chatsCollection
                .whereArrayContains("participant_ids", userId)
                .get()
                .await()

            Log.d(TAG, "   Found ${chatsSnapshot.documents.size} chats to delete")

            var deletedCount = 0
            var failedCount = 0

            chatsSnapshot.documents.forEach { chatDoc ->
                try {
                    val chatId = chatDoc.id

                    val messagesSnapshot = messagesCollection
                        .whereEqualTo("chat_id", chatId)
                        .get()
                        .await()

                    messagesSnapshot.documents.forEach { messageDoc ->
                        messageDoc.reference.delete().await()
                    }

                    chatDoc.reference.delete().await()
                    deletedCount++
                    Log.d(TAG, "   ✅ Deleted chat: $chatId")

                } catch (e: Exception) {
                    failedCount++
                    Log.e(TAG, "   ❌ Failed to delete chat: ${chatDoc.id}", e)
                }
            }

            Log.d(TAG, "✅ Deleted $deletedCount chats, $failedCount failed")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete all chats", e)
            Result.failure(e)
        }
    }

    private suspend fun updateLastMessage(chatId: String, senderId: String, content: String) {
        try {
            Log.d(TAG, "📝 Updating last message for chat: $chatId")

            val chatDoc = chatsCollection.document(chatId).get().await()

            if (!chatDoc.exists()) {
                Log.e(TAG, "❌ Chat document not found: $chatId")
                return
            }

            val participantIds = chatDoc.get("participant_ids") as? List<*>
            val unreadCountMap = chatDoc.get("unread_count") as? Map<*, *>

            if (participantIds == null || participantIds.isEmpty()) {
                Log.e(TAG, "❌ No participants found in chat")
                return
            }

            val otherUserId = participantIds.firstOrNull { it.toString() != senderId }?.toString()

            if (otherUserId == null) {
                Log.e(TAG, "❌ Could not find other user in participants")
                return
            }

            val currentUnreadCount = (unreadCountMap?.get(otherUserId) as? Long)?.toInt() ?: 0
            val newUnreadCount = currentUnreadCount + 1

            chatsCollection.document(chatId).update(mapOf(
                "last_message" to content,
                "last_message_time" to System.currentTimeMillis(),
                "last_message_sender_id" to senderId,
                "unread_count.$otherUserId" to newUnreadCount
            )).await()

            Log.d(TAG, "✅ Last message updated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update last message", e)
            e.printStackTrace()
        }
    }
}