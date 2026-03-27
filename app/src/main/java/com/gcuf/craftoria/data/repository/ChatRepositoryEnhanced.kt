package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Enhanced ChatRepository with production-ready features:
 * - Message deletion
 * - Chat deletion
 * - Typing indicators
 * - Message search
 * - Pagination
 * - Better error handling
 */
class ChatRepositoryEnhanced {
    private val db = FirebaseFirestore.getInstance()
    private val chatsCollection = db.collection("chats")
    private val messagesCollection = db.collection("messages")

    companion object {
        private const val TAG = "ChatRepositoryEnhanced"
        private const val MESSAGES_PAGE_SIZE = 50
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MESSAGE DELETION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Delete message for everyone (only sender can do this)
     */
    suspend fun deleteMessage(messageId: String, senderId: String): Result<Unit> {
        return try {
            val message = messagesCollection.document(messageId).get().await()
                .toObject(Message::class.java)

            if (message?.senderId != senderId) {
                return Result.failure(Exception("Only sender can delete message"))
            }

            messagesCollection.document(messageId).delete().await()
            Log.d(TAG, "Message deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message", e)
            Result.failure(e)
        }
    }

    /**
     * Delete message for me only (soft delete)
     */
    suspend fun deleteMessageForMe(messageId: String, userId: String): Result<Unit> {
        return try {
            messagesCollection.document(messageId)
                .update(
                    "deleted_for",
                    com.google.firebase.firestore.FieldValue.arrayUnion(userId)
                )
                .await()

            Log.d(TAG, "Message deleted for user")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message for user", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHAT DELETION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Delete entire chat (removes all messages and chat document)
     */
    suspend fun deleteChat(chatId: String, userId: String): Result<Unit> {
        return try {
            // Verify user is participant
            val chat = chatsCollection.document(chatId).get().await()
                .toObject(Chat::class.java)

            if (chat == null || !chat.participantIds.contains(userId)) {
                return Result.failure(Exception("User is not a participant"))
            }

            // Delete all messages in batches
            var deletedCount = 0
            do {
                val messages = messagesCollection
                    .whereEqualTo("chat_id", chatId)
                    .limit(500) // Firestore batch limit
                    .get()
                    .await()

                if (messages.isEmpty) break

                val batch = db.batch()
                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()

                deletedCount += messages.size()
            } while (messages.size() >= 500)

            // Delete chat document
            chatsCollection.document(chatId).delete().await()

            Log.d(TAG, "Chat deleted: $deletedCount messages removed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete chat", e)
            Result.failure(e)
        }
    }

    /**
     * Clear chat (delete all messages but keep chat document)
     */
    suspend fun clearChat(chatId: String, userId: String): Result<Unit> {
        return try {
            // Verify user is participant
            val chat = chatsCollection.document(chatId).get().await()
                .toObject(Chat::class.java)

            if (chat == null || !chat.participantIds.contains(userId)) {
                return Result.failure(Exception("User is not a participant"))
            }

            // Delete all messages
            var deletedCount = 0
            do {
                val messages = messagesCollection
                    .whereEqualTo("chat_id", chatId)
                    .limit(500)
                    .get()
                    .await()

                if (messages.isEmpty) break

                val batch = db.batch()
                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()

                deletedCount += messages.size()
            } while (messages.size() >= 500)

            // Reset chat metadata
            chatsCollection.document(chatId).update(
                mapOf(
                    "last_message" to "",
                    "last_message_time" to System.currentTimeMillis(),
                    "last_message_sender_id" to "",
                    "unread_count" to mapOf<String, Int>()
                )
            ).await()

            Log.d(TAG, "Chat cleared: $deletedCount messages removed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear chat", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TYPING INDICATORS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Set typing status for current user
     */
    suspend fun setTypingStatus(
        chatId: String,
        userId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return try {
            chatsCollection.document(chatId)
                .update("typing_$userId", isTyping)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update typing status", e)
            Result.failure(e)
        }
    }

    /**
     * Listen to other user's typing status
     */
    fun getTypingStatusFlow(chatId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        val listener = chatsCollection.document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Typing status listener error", error)
                    trySend(false)
                    return@addSnapshotListener
                }

                val isTyping = snapshot?.getBoolean("typing_$otherUserId") ?: false
                trySend(isTyping)
            }

        awaitClose {
            listener.remove()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MESSAGE SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Search messages in a specific chat
     */
    suspend fun searchMessages(chatId: String, query: String): Result<List<Message>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }

            val messages = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                .filter { message ->
                    message.content.contains(query, ignoreCase = true) ||
                    message.productName.contains(query, ignoreCase = true) ||
                    message.senderName.contains(query, ignoreCase = true)
                }
                .sortedByDescending { it.createdAt }

            Log.d(TAG, "Search found ${messages.size} messages")
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search messages", e)
            Result.failure(e)
        }
    }

    /**
     * Search across all user's chats
     */
    suspend fun searchAllChats(userId: String, query: String): Result<List<Message>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }

            // Get user's chats
            val userChats = chatsCollection
                .whereArrayContains("participant_ids", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.id }

            // Search messages in all chats
            val allMessages = mutableListOf<Message>()
            userChats.forEach { chatId ->
                val messages = messagesCollection
                    .whereEqualTo("chat_id", chatId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                    .filter { message ->
                        message.content.contains(query, ignoreCase = true) ||
                        message.productName.contains(query, ignoreCase = true)
                    }

                allMessages.addAll(messages)
            }

            val sortedMessages = allMessages.sortedByDescending { it.createdAt }
            Log.d(TAG, "Global search found ${sortedMessages.size} messages")
            Result.success(sortedMessages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search all chats", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAGINATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get messages with pagination
     */
    suspend fun getMessagesPaginated(
        chatId: String,
        limit: Int = MESSAGES_PAGE_SIZE,
        lastMessageTime: Long? = null
    ): Result<List<Message>> {
        return try {
            var query = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (lastMessageTime != null) {
                query = query.startAfter(lastMessageTime)
            }

            val messages = query.get().await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                .reversed() // Show oldest first

            Log.d(TAG, "Loaded ${messages.size} messages")
            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load paginated messages", e)
            Result.failure(e)
        }
    }

    /**
     * Get messages with pagination (Flow for real-time updates)
     */
    fun getMessagesPaginatedFlow(
        chatId: String,
        limit: Int = MESSAGES_PAGE_SIZE
    ): Flow<List<Message>> = callbackFlow {
        val listener = messagesCollection
            .whereEqualTo("chat_id", chatId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Paginated messages listener error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }.reversed()

                trySend(messages)
            }

        awaitClose {
            listener.remove()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHAT STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get chat statistics
     */
    suspend fun getChatStats(chatId: String): Result<ChatStats> {
        return try {
            val messages = messagesCollection
                .whereEqualTo("chat_id", chatId)
                .get()
                .await()

            val totalMessages = messages.size()
            val textMessages = messages.documents.count { 
                it.getString("type") == "TEXT" 
            }
            val imageMessages = messages.documents.count { 
                it.getString("type") == "IMAGE" 
            }
            val productShares = messages.documents.count { 
                it.getString("type") == "PRODUCT" 
            }

            val stats = ChatStats(
                totalMessages = totalMessages,
                textMessages = textMessages,
                imageMessages = imageMessages,
                productShares = productShares
            )

            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get chat stats", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MUTE/UNMUTE CHAT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Mute chat notifications for a user
     */
    suspend fun muteChat(chatId: String, userId: String): Result<Unit> {
        return try {
            chatsCollection.document(chatId)
                .update("muted_by.$userId", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mute chat", e)
            Result.failure(e)
        }
    }

    /**
     * Unmute chat notifications for a user
     */
    suspend fun unmuteChat(chatId: String, userId: String): Result<Unit> {
        return try {
            chatsCollection.document(chatId)
                .update("muted_by.$userId", false)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unmute chat", e)
            Result.failure(e)
        }
    }
}

/**
 * Chat statistics data class
 */
data class ChatStats(
    val totalMessages: Int = 0,
    val textMessages: Int = 0,
    val imageMessages: Int = 0,
    val productShares: Int = 0
)
