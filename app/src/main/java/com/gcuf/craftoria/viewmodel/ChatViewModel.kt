package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.data.repository.ChatRepository
import com.gcuf.craftoria.utils.CloudinaryManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatState>(ChatState.Loading)
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat: StateFlow<Chat?> = _chat.asStateFlow()

    private val _otherUser = MutableStateFlow<String>("")
    val otherUser: StateFlow<String> = _otherUser.asStateFlow()

    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    // ✅ NEW: Chat type and other user's role
    private val _chatType = MutableStateFlow("buyer_seller")
    val chatType: StateFlow<String> = _chatType.asStateFlow()

    private val _otherUserRole = MutableStateFlow(UserRole.SELLER)
    val otherUserRole: StateFlow<UserRole> = _otherUserRole.asStateFlow()

    companion object {
        private const val TAG = "ChatViewModel"
    }

    fun initializeChat(
        currentUserId: String,
        currentUserName: String,
        otherUserId: String,
        otherUserName: String,
        productId: String = ""
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 INITIALIZING CHAT (OPTIMIZED)")
                Log.d(TAG, "   Current User: $currentUserId ($currentUserName)")
                Log.d(TAG, "   Other User: $otherUserId ($otherUserName)")

                // ✅ CRITICAL FIX: Show UI immediately with basic data
                _otherUser.value = otherUserName
                _uiState.value = ChatState.Success
                
                // ✅ Load chat data in background (non-blocking)
                val result = chatRepository.getOrCreateChat(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    productId = productId
                )

                if (result.isSuccess) {
                    val chatId = result.getOrNull()!!
                    Log.d(TAG, "✅ Chat ID obtained: $chatId")

                    // ✅ Load chat metadata (non-blocking)
                    loadChat(chatId, currentUserId)
                    
                    // ✅ Start listening to messages immediately (real-time updates)
                    listenToMessages(chatId)
                    
                    // ✅ Mark messages as delivered/read in background
                    markMessagesAsDelivered(chatId, currentUserId)
                    markMessagesAsRead(chatId, currentUserId)
                    
                    // ✅ Start continuous read receipt updates
                    startReadReceiptUpdates(chatId, currentUserId)

                    Log.d(TAG, "✅ Chat initialization complete")
                } else {
                    Log.e(TAG, "❌ Failed to get/create chat: ${result.exceptionOrNull()?.message}")
                    _uiState.value = ChatState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to initialize chat"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during chat initialization", e)
                _uiState.value = ChatState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun loadChat(chatId: String, currentUserId: String) {
        val result = chatRepository.getChat(chatId)
        if (result.isSuccess) {
            val chat = result.getOrNull()!!
            _chat.value = chat
            _isBlocked.value = chat.isBlocked
            
            // ✅ NEW: Load chat type and other user's role
            _chatType.value = chat.chatType

            Log.d(TAG, "📋 Chat participants: ${chat.participantIds}")
            Log.d(TAG, "📋 Current user: $currentUserId")
            Log.d(TAG, "📋 Chat type: ${chat.chatType}")

            val otherUserId = chat.participantIds.firstOrNull { it != currentUserId }
            if (otherUserId != null) {
                _otherUser.value = chat.participantNames[otherUserId] ?: ""
                
                // ✅ NEW: Load other user's role
                val roleString = chat.participantRoles[otherUserId] ?: "BUYER"
                _otherUserRole.value = if (roleString == "SELLER") UserRole.SELLER else UserRole.BUYER
                
                Log.d(TAG, "✅ Other user: $otherUserId, name: ${_otherUser.value}, role: $roleString")
            } else {
                Log.e(TAG, "❌ Could not find other user in participants")
            }
        } else {
            Log.e(TAG, "❌ Failed to load chat: ${result.exceptionOrNull()?.message}")
        }
    }

    private fun listenToMessages(chatId: String) {
        viewModelScope.launch {
            Log.d(TAG, "🎧 Starting to listen for messages in chat: $chatId")
            chatRepository.getMessagesFlow(chatId)
                .catch { e ->
                    Log.e(TAG, "❌ Messages flow error", e)
                    _uiState.value = ChatState.Error(e.message ?: "Failed to load messages")
                }
                .collect { messages ->
                    Log.d(TAG, "📬 Received ${messages.size} messages from flow")
                    messages.forEachIndexed { index, msg ->
                        Log.d(TAG, "  [$index] ${msg.senderName}: ${msg.content.take(30)}")
                    }
                    _messages.value = messages
                }
        }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String
    ) {
        if (content.isBlank()) {
            Log.w(TAG, "⚠️ Attempted to send blank message")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "📤 Sending message: chatId=$chatId, sender=$senderName")
                Log.d(TAG, "   Content: '$content'")

                val result = chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    content = content
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Message sent successfully: ${result.getOrNull()}")
                } else {
                    Log.e(TAG, "❌ Failed to send message: ${result.exceptionOrNull()?.message}")
                    _uiState.value = ChatState.Error("Failed to send message")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception sending message", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to send message")
            }
        }
    }

    fun sendImageMessage(
        context: Context,
        chatId: String,
        senderId: String,
        senderName: String,
        imageUri: Uri
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = ChatState.Loading

                val imageUrl = CloudinaryManager.uploadImage(
                    context = context,
                    imageUri = imageUri,
                    folder = "craftoria/chat"
                )

                val result = chatRepository.sendImageMessage(
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    imageUrl = imageUrl
                )

                if (result.isSuccess) {
                    _uiState.value = ChatState.Success
                } else {
                    _uiState.value = ChatState.Error("Failed to send image")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send image", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to send image")
            }
        }
    }

    fun shareProduct(
        chatId: String,
        senderId: String,
        senderName: String,
        product: Product
    ) {
        viewModelScope.launch {
            try {
                val result = chatRepository.shareProduct(
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    product = product
                )

                if (!result.isSuccess) {
                    _uiState.value = ChatState.Error("Failed to share product")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to share product", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to share product")
            }
        }
    }

    fun sendOrderUpdate(
        chatId: String,
        senderId: String,
        senderName: String,
        orderId: String,
        orderStatus: String
    ) {
        viewModelScope.launch {
            try {
                chatRepository.sendOrderUpdate(
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    orderId = orderId,
                    orderStatus = orderStatus
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send order update", e)
            }
        }
    }

    fun sendNegotiation(
        chatId: String,
        senderId: String,
        senderName: String,
        productName: String,
        offeredPrice: Double
    ) {
        viewModelScope.launch {
            try {
                val result = chatRepository.sendNegotiation(
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    productName = productName,
                    offeredPrice = offeredPrice
                )

                if (!result.isSuccess) {
                    _uiState.value = ChatState.Error("Failed to send negotiation")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send negotiation", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to send negotiation")
            }
        }
    }

    fun updateNegotiationStatus(
        messageId: String,
        status: NegotiationStatus,
        chatId: String,
        currentUserId: String
    ) {
        viewModelScope.launch {
            try {
                val result = chatRepository.updateNegotiationStatus(messageId, status)

                if (result.isSuccess) {
                    val statusText = if (status == NegotiationStatus.ACCEPTED) "accepted" else "declined"
                    _uiState.value = ChatState.ActionSuccess("Offer $statusText")
                } else {
                    _uiState.value = ChatState.Error("Failed to update negotiation")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update negotiation", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to update negotiation")
            }
        }
    }

    fun markMessagesAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(chatId, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as read", e)
            }
        }
    }

    fun markMessagesAsDelivered(chatId: String, userId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsDelivered(chatId, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as delivered", e)
            }
        }
    }

    fun blockUser(chatId: String, blockerId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ChatState.Loading

                val result = chatRepository.blockUser(chatId, blockerId)

                if (result.isSuccess) {
                    _isBlocked.value = true
                    _uiState.value = ChatState.ActionSuccess("User blocked successfully")
                } else {
                    _uiState.value = ChatState.Error("Failed to block user")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to block user", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to block user")
            }
        }
    }

    fun unblockUser(chatId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ChatState.Loading

                val result = chatRepository.unblockUser(chatId)

                if (result.isSuccess) {
                    _isBlocked.value = false
                    _uiState.value = ChatState.ActionSuccess("User unblocked successfully")
                } else {
                    _uiState.value = ChatState.Error("Failed to unblock user")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to unblock user", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to unblock user")
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Deleting message: $messageId")
                val result = chatRepository.deleteMessage(messageId)

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Message deleted successfully")
                    _uiState.value = ChatState.ActionSuccess("Message deleted")
                } else {
                    Log.e(TAG, "❌ Failed to delete message")
                    _uiState.value = ChatState.Error("Failed to delete message")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception deleting message", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to delete message")
            }
        }
    }

    // Delete entire chat (optimistic - instant UI update)
    fun deleteChat(chatId: String, onOptimisticDelete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                // ✅ INSTANT: Trigger optimistic UI update immediately
                onOptimisticDelete()
                
                // ✅ Background: Perform actual deletion without blocking UI
                val result = chatRepository.deleteChat(chatId)
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Chat deleted successfully: $chatId")
                    _uiState.value = ChatState.ActionSuccess("Chat deleted")
                } else {
                    Log.e(TAG, "❌ Failed to delete chat: ${result.exceptionOrNull()?.message}")
                    _uiState.value = ChatState.Error("Failed to delete chat")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception deleting chat", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to delete chat")
            }
        }
    }
    
    // Delete all chats for user (optimistic - instant UI update)
    fun deleteAllChats(userId: String, onOptimisticDelete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                // ✅ INSTANT: Trigger optimistic UI update immediately
                onOptimisticDelete()
                
                // ✅ Background: Perform actual deletion without blocking UI
                val result = chatRepository.deleteAllChats(userId)
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ All chats deleted successfully for user: $userId")
                    _uiState.value = ChatState.ActionSuccess("All chats deleted")
                } else {
                    Log.e(TAG, "❌ Failed to delete all chats: ${result.exceptionOrNull()?.message}")
                    _uiState.value = ChatState.Error("Failed to delete all chats")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception deleting all chats", e)
                _uiState.value = ChatState.Error(e.message ?: "Failed to delete all chats")
            }
        }
    }

    fun resetState() {
        _uiState.value = ChatState.Success
    }

    // ✅ NEW: Start continuous read receipt updates
    private fun startReadReceiptUpdates(chatId: String, currentUserId: String) {
        viewModelScope.launch {
            chatRepository.startContinuousReadReceiptUpdates(chatId, currentUserId)
                .catch { e ->
                    Log.e(TAG, "❌ Read receipt updates error", e)
                }
                .collect {
                    Log.d(TAG, "📬 Read receipt updated")
                    // Flow will automatically trigger message re-fetch
                    // because Firestore listener detects changes
                }
        }
    }
}

sealed class ChatState {
    object Loading : ChatState()
    object Success : ChatState()
    data class ActionSuccess(val message: String) : ChatState()
    data class Error(val message: String) : ChatState()
}
