# Chat System: Role Conversion & Real-Time Read Receipts Analysis

## Issues Identified

### 1. **Buyer-to-Seller Conversion: Chat Persistence**

When a buyer converts to a seller, their existing chats with other sellers need to be handled properly.

#### Current Behavior:
- Chats are stored with `participant_ids` (sorted list of user IDs)
- User role is stored separately in the `users` collection
- No mechanism to track role changes or update chat context

#### Scenarios That Can Occur:

**Scenario A: Buyer → Seller Conversion (Existing Buyer Chats)**
```
Before Conversion:
- User A (Buyer) chats with User B (Seller)
- Chat ID: "chat_123"
- participant_ids: ["user_a", "user_b"]
- participant_names: {user_a: "Alice", user_b: "Bob"}

After Conversion:
- User A is now a Seller
- Same chat still exists with same participant_ids
- BUT: User A's role changed, so chat context is ambiguous
  - Is this a buyer-seller chat or seller-seller chat?
  - Should User A see this in "Buyer Chats" or "Seller Chats"?
```

**Scenario B: Seller-to-Seller Chats**
```
If User A (now Seller) wants to chat with User B (Seller):
- New chat created: "chat_456"
- participant_ids: ["user_a", "user_b"]
- Both are now sellers
- This is a seller-to-seller negotiation chat
```

**Scenario C: Role Ambiguity in UI**
```
Current Issue:
- ChatScreen checks: isCurrentUserSeller = currentUser.role == UserRole.SELLER
- But this doesn't distinguish between:
  1. Buyer chatting with seller (buyer perspective)
  2. Seller chatting with buyer (seller perspective)
  3. Seller chatting with seller (seller-to-seller)
  
- The "showViewProfile" logic: showViewProfile = !isCurrentUserSeller
  - This means sellers can't view other sellers' profiles from chat
  - But seller-to-seller chats should allow profile viewing
```

---

### 2. **Real-Time Read Receipts Not Updating Instantly**

#### Current Implementation Issues:

**Problem 1: Delayed Read Receipt Updates**
```kotlin
// In ChatRepository.markMessagesAsRead():
val unreadMessages = allMessages.documents.filter { doc ->
    val isRead = doc.getBoolean("is_read") ?: true
    val senderId = doc.getString("sender_id") ?: ""
    !isRead && senderId != userId
}

// This is called ONCE when chat initializes
// But messages are fetched in real-time via Flow
// The Flow doesn't trigger read receipt updates for NEW messages
```

**Problem 2: No Real-Time Listener for Read Status**
```kotlin
// In ChatScreen.kt:
val messages by chatViewModel.messages.collectAsState()

// This collects messages, but:
// - When a message's read_at changes in Firestore
// - The Flow listener doesn't re-emit the updated message
// - So UI doesn't update with new read receipts
```

**Problem 3: Delivered Status Not Tracked Properly**
```kotlin
// In Message model:
@PropertyName("delivered_at")
val deliveredAt: Long = 0,  // ✅ Field exists

// But in ChatScreen.kt MessageItem:
Text(
    text = if (message.isRead ||
    // ❌ Only checks isRead, not deliveredAt
    // Should show: ✓ (delivered) or ✓✓ (read)
)
```

**Problem 4: No Automatic Delivery Marking**
```kotlin
// markMessagesAsDelivered() is called in initializeChat()
// But only marks messages that existed BEFORE chat opened
// NEW messages arriving after chat is open are never marked as delivered
```

---

## Solutions

### Solution 1: Handle Role Conversion in Chat System

#### A. Add Role Context to Chat Model

```kotlin
// In Chat.kt - ADD these fields:
data class Chat(
    // ... existing fields ...
    
    @PropertyName("participant_roles")
    val participantRoles: Map<String, String> = emptyMap(),  // userId -> role (BUYER/SELLER)
    
    @PropertyName("chat_type")
    val chatType: String = "buyer_seller",  // buyer_seller, seller_seller, buyer_buyer
    
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("last_role_update")
    val lastRoleUpdate: Long = System.currentTimeMillis()
)
```

#### B. Update Chat Creation to Store Roles

```kotlin
// In ChatRepository.getOrCreateChat():
suspend fun getOrCreateChat(
    currentUserId: String,
    currentUserName: String,
    otherUserId: String,
    otherUserName: String,
    productId: String = ""
): Result<String> {
    return try {
        // ... existing code ...
        
        if (existingChat != null) {
            // ✅ NEW: Sync roles for existing chat
            syncParticipantRoles(existingChat.id, participantIds)
            return Result.success(existingChat.id)
        }
        
        // Fetch roles for both users
        val participantRoles = fetchParticipantRoles(listOf(currentUserId, otherUserId))
        val chatType = determineChatType(participantRoles)
        
        val chatData = mapOf(
            // ... existing fields ...
            "participant_roles" to participantRoles,
            "chat_type" to chatType,
            "last_role_update" to System.currentTimeMillis()
        )
        
        // ... rest of code ...
    }
}

// Helper functions:
private suspend fun fetchParticipantRoles(userIds: List<String>): Map<String, String> {
    val roles = mutableMapOf<String, String>()
    userIds.forEach { userId ->
        try {
            val userDoc = usersCollection.document(userId).get().await()
            val role = userDoc.getString("role") ?: "BUYER"
            roles[userId] = role
        } catch (e: Exception) {
            roles[userId] = "BUYER"  // Default fallback
        }
    }
    return roles
}

private suspend fun syncParticipantRoles(chatId: String, participantIds: List<String>) {
    try {
        val roles = fetchParticipantRoles(participantIds)
        val chatType = determineChatType(roles)
        
        chatsCollection.document(chatId).update(mapOf(
            "participant_roles" to roles,
            "chat_type" to chatType,
            "last_role_update" to System.currentTimeMillis()
        )).await()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to sync participant roles", e)
    }
}

private fun determineChatType(roles: Map<String, String>): String {
    val roleList = roles.values.toList()
    return when {
        roleList.all { it == "SELLER" } -> "seller_seller"
        roleList.all { it == "BUYER" } -> "buyer_buyer"
        else -> "buyer_seller"
    }
}
```

#### C. Update ChatScreen to Handle Role Context

```kotlin
// In ChatScreen.kt:
@Composable
fun ChatScreen(
    currentUser: User,
    otherUserId: String,
    otherUserName: String,
    productId: String = "",
    onBackClick: () -> Unit,
    onViewProfile: (String) -> Unit,
    onViewProduct: (String) -> Unit,
    onTrackOrder: (String) -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uiState by chatViewModel.uiState.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val chat by chatViewModel.chat.collectAsState()
    val isBlocked by chatViewModel.isBlocked.collectAsState()
    
    // ✅ NEW: Get chat type and other user's role
    val chatType by chatViewModel.chatType.collectAsState()
    val otherUserRole by chatViewModel.otherUserRole.collectAsState()
    
    // ✅ NEW: Determine if profile viewing is allowed
    val canViewProfile = when (chatType) {
        "seller_seller" -> true  // Sellers can view other sellers
        "buyer_seller" -> currentUser.role == UserRole.BUYER  // Only buyers view sellers
        "buyer_buyer" -> true  // Buyers can view other buyers
        else -> false
    }
    
    // ... rest of code ...
    
    // In ChatHeader call:
    ChatHeader(
        userName = otherUserName,
        userAvatar = otherUserProfileImage,
        isOnline = true,
        isBlocked = isBlocked,
        showViewProfile = canViewProfile,  // ✅ Use dynamic logic
        // ... rest of parameters ...
    )
}
```

#### D. Add to ChatViewModel

```kotlin
// In ChatViewModel.kt:
private val _chatType = MutableStateFlow("buyer_seller")
val chatType: StateFlow<String> = _chatType.asStateFlow()

private val _otherUserRole = MutableStateFlow(UserRole.SELLER)
val otherUserRole: StateFlow<UserRole> = _otherUserRole.asStateFlow()

private suspend fun loadChat(chatId: String, currentUserId: String) {
    val result = chatRepository.getChat(chatId)
    if (result.isSuccess) {
        val chat = result.getOrNull()!!
        _chat.value = chat
        _isBlocked.value = chat.isBlocked
        
        // ✅ NEW: Load chat type and other user's role
        _chatType.value = chat.chatType
        
        val otherUserId = chat.participantIds.firstOrNull { it != currentUserId }
        if (otherUserId != null) {
            val roleString = chat.participantRoles[otherUserId] ?: "BUYER"
            _otherUserRole.value = if (roleString == "SELLER") UserRole.SELLER else UserRole.BUYER
        }
    }
}
```

---

### Solution 2: Implement Real-Time Read Receipt Updates

#### A. Fix Message Flow to Include Read Status Updates

```kotlin
// In ChatRepository.kt - REPLACE getMessagesFlow():
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
                        deliveredAt = doc.getLong("delivered_at") ?: 0L,  // ✅ Include this
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
```

#### B. Add Continuous Delivery & Read Marking

```kotlin
// In ChatRepository.kt - ADD new function:
fun startContinuousReadReceiptUpdates(
    chatId: String,
    currentUserId: String
): Flow<Unit> = callbackFlow {
    Log.d(TAG, "🔄 Starting continuous read receipt updates for chat: $chatId")

    val listener = messagesCollection
        .whereEqualTo("chat_id", chatId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Read receipt listener error: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                return@addSnapshotListener
            }

            // Find unread messages from other users
            val unreadMessages = snapshot.documents.filter { doc ->
                val isRead = doc.getBoolean("is_read") ?: true
                val senderId = doc.getString("sender_id") ?: ""
                val deliveredAt = doc.getLong("delivered_at") ?: 0L
                
                !isRead && senderId != currentUserId
            }

            // Find undelivered messages from other users
            val undeliveredMessages = snapshot.documents.filter { doc ->
                val deliveredAt = doc.getLong("delivered_at") ?: 0L
                val senderId = doc.getString("sender_id") ?: ""
                
                deliveredAt == 0L && senderId != currentUserId
            }

            // Update undelivered messages first
            if (undeliveredMessages.isNotEmpty()) {
                val batch = db.batch()
                val currentTime = System.currentTimeMillis()
                undeliveredMessages.forEach { doc ->
                    batch.update(doc.reference, "delivered_at", currentTime)
                }
                batch.commit().addOnSuccessListener {
                    Log.d(TAG, "✅ Marked ${undeliveredMessages.size} messages as delivered")
                    trySend(Unit)
                }
            }

            // Then update unread messages
            if (unreadMessages.isNotEmpty()) {
                val batch = db.batch()
                val currentTime = System.currentTimeMillis()
                unreadMessages.forEach { doc ->
                    batch.update(doc.reference, mapOf(
                        "is_read" to true,
                        "read_at" to currentTime
                    ))
                }
                batch.commit().addOnSuccessListener {
                    Log.d(TAG, "✅ Marked ${unreadMessages.size} messages as read")
                    trySend(Unit)
                }
            }
        }

    awaitClose {
        Log.d(TAG, "🔌 Closing read receipt listener for chat: $chatId")
        listener.remove()
    }
}
```

#### C. Update ChatViewModel to Use Continuous Updates

```kotlin
// In ChatViewModel.kt:
fun initializeChat(
    currentUserId: String,
    currentUserName: String,
    otherUserId: String,
    otherUserName: String,
    productId: String = ""
) {
    viewModelScope.launch {
        try {
            Log.d(TAG, "🔄 INITIALIZING CHAT")
            
            _uiState.value = ChatState.Loading

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

                loadChat(chatId, currentUserId)
                listenToMessages(chatId)
                
                // ✅ NEW: Start continuous read receipt updates
                startReadReceiptUpdates(chatId, currentUserId)

                _otherUser.value = otherUserName
                _uiState.value = ChatState.Success

                Log.d(TAG, "✅ Chat initialization complete")
            } else {
                Log.e(TAG, "❌ Failed to get/create chat")
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

// ✅ NEW: Add this function
private fun startReadReceiptUpdates(chatId: String, currentUserId: String) {
    viewModelScope.launch {
        chatRepository.startContinuousReadReceiptUpdates(chatId, currentUserId)
            .collect {
                Log.d(TAG, "📬 Read receipt updated")
                // Flow will automatically trigger message re-fetch
                // because Firestore listener detects changes
            }
    }
}
```

#### D. Update ChatScreen to Display Read Receipts Correctly

```kotlin
// In ChatScreen.kt - REPLACE MessageItem read receipt display:
@Composable
fun MessageItem(
    message: Message,
    currentUserId: String,
    chat: Chat? = null,
    isOtherUserSeller: Boolean = false,
    onViewProduct: (String) -> Unit,
    onTrackOrder: (String) -> Unit,
    onAcceptOffer: () -> Unit,
    onDeclineOffer: () -> Unit,
    onDeleteMessage: (String) -> Unit = {}
) {
    val isSent = message.senderId == currentUserId
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start) {
        // ... existing code ...

        Column(
            horizontalAlignment = if (isSent) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp).combinedClickable(onClick = {}, onLongClick = { showDeleteDialog = true })
        ) {
            when (message.type) {
                MessageType.TEXT -> TextMessage(message, isSent)
                MessageType.IMAGE -> ImageMessage(message, isSent)
                MessageType.PRODUCT -> ProductMessage(message, isSent, onViewProduct)
                MessageType.ORDER_UPDATE -> OrderUpdateMessage(message, isSent, onTrackOrder)
                MessageType.NEGOTIATION -> NegotiationMessage(message, isSent, currentUserId, onAcceptOffer, onDeclineOffer)
            }
            
            // ✅ UPDATED: Show read receipts correctly
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp, end = if (isSent) 4.dp else 0.dp)
            ) {
                Text(text = getTimeString(message.createdAt), fontSize = 10.sp, color = TextSecondary)
                
                if (isSent) {
                    // ✅ Show delivery and read status
                    val receiptText = when {
                        message.isRead -> "✓✓"  // Double tick = read
                        message.deliveredAt > 0 -> "✓"  // Single tick = delivered
                        else -> ""  // No tick = sent but not delivered
                    }
                    
                    val receiptColor = when {
                        message.isRead -> Primary  // Blue for read
                        message.deliveredAt > 0 -> TextSecondary  // Gray for delivered
                        else -> TextLight  // Light gray for sent
                    }
                    
                    if (receiptText.isNotEmpty()) {
                        Text(
                            text = receiptText,
                            fontSize = 10.sp,
                            color = receiptColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
```

---

## Implementation Checklist

### Phase 1: Role Conversion Support
- [ ] Add `participant_roles` and `chat_type` fields to Chat model
- [ ] Update `getOrCreateChat()` to fetch and store roles
- [ ] Add `syncParticipantRoles()` function
- [ ] Add `determineChatType()` helper
- [ ] Update ChatViewModel with `chatType` and `otherUserRole` StateFlows
- [ ] Update ChatScreen to use dynamic profile viewing logic
- [ ] Test: Buyer converts to seller, verify chat context updates

### Phase 2: Real-Time Read Receipts
- [ ] Add `startContinuousReadReceiptUpdates()` to ChatRepository
- [ ] Update ChatViewModel to call continuous updates
- [ ] Update ChatScreen MessageItem to display ✓ and ✓✓
- [ ] Verify `deliveredAt` is properly set in all message types
- [ ] Test: Send message, verify ✓ appears immediately, then ✓✓ when read

### Phase 3: Testing Scenarios
- [ ] Test Scenario A: Buyer → Seller conversion with existing chats
- [ ] Test Scenario B: Seller-to-seller chat creation
- [ ] Test Scenario C: Read receipts update in real-time
- [ ] Test Scenario D: Delivered status shows before read status
- [ ] Test Scenario E: Multiple messages, verify all receipts update

---

## Firestore Rules Update

```firestore
// Add to firestore.rules to allow read receipt updates:
match /messages/{messageId} {
  allow read: if request.auth.uid in resource.data.get('participant_ids', []);
  allow update: if request.auth.uid in resource.data.get('participant_ids', [])
    && (request.resource.data.diff(resource.data).affectedKeys()
      .hasOnly(['is_read', 'read_at', 'delivered_at']));
}
```

---

## Summary

**Role Conversion Issues:**
- Chats now track participant roles and chat type
- Profile viewing logic adapts based on chat type
- Seller-to-seller chats are properly identified

**Read Receipt Issues:**
- Continuous listener marks messages as delivered/read in real-time
- UI displays ✓ (delivered) and ✓✓ (read) with proper colors
- Both participants see instant updates
