package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.Chat
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerMessagesScreen(
    user: User,
    onBackClick: () -> Unit,
    onChatClick: (String, String) -> Unit,
    chatViewModel: com.gcuf.craftoria.viewmodel.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val uiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is com.gcuf.craftoria.viewmodel.ChatState.ActionSuccess -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                chatViewModel.resetState()
                isLoading = true
            }
            is com.gcuf.craftoria.viewmodel.ChatState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                chatViewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(user.id) {
        isLoading = true
        try {
            Log.d("SellerMessages", "═══════════════════════════════════════")
            Log.d("SellerMessages", "🔄 LaunchedEffect STARTED")
            Log.d("SellerMessages", "🔍 Loading chats for seller: ${user.id}")

            if (user.id.isBlank()) {
                Log.e("SellerMessages", "❌ User ID is blank!")
                isLoading = false
                return@LaunchedEffect
            }

            Log.d("SellerMessages", "📡 Querying Firestore...")
            val snapshot = FirebaseFirestore.getInstance()
                .collection("chats")
                .whereArrayContains("participant_ids", user.id)
                .get()
                .await()

            Log.d("SellerMessages", "📬 Found ${snapshot.documents.size} chat documents")

            val parsedChats = snapshot.documents.mapNotNull { doc ->
                try {
                    Log.d("SellerMessages", "   Processing chat: ${doc.id}")

                    @Suppress("UNCHECKED_CAST")
                    val participantIds = (doc.get("participant_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val participantNames = doc.get("participant_names") as? Map<String, String> ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    val unreadCount = doc.get("unread_count") as? Map<String, Any> ?: emptyMap()
                    val unreadCountInt = unreadCount.mapValues { (it.value as? Long)?.toInt() ?: 0 }

                    val chat = Chat(
                        id = doc.id,
                        participantIds = participantIds,
                        participantNames = participantNames,
                        lastMessage = doc.getString("last_message") ?: "",
                        lastMessageTime = doc.getLong("last_message_time") ?: 0L,
                        lastMessageSenderId = doc.getString("last_message_sender_id") ?: "",
                        unreadCount = unreadCountInt,
                        isBlocked = doc.getBoolean("is_blocked") ?: false,
                        blockedBy = doc.getString("blocked_by") ?: "",
                        createdAt = doc.getLong("created_at") ?: 0L
                    )

                    Log.d("SellerMessages", "   ✅ Chat parsed: participants=$participantIds")
                    chat
                } catch (e: Exception) {
                    Log.e("SellerMessages", "   ❌ Error parsing chat ${doc.id}", e)
                    null
                }
            }

            val sortedChats = parsedChats.sortedByDescending { it.lastMessageTime }

            chats = sortedChats.distinctBy { chat ->
                chat.participantIds.sorted().joinToString("-")
            }

            Log.d("SellerMessages", "✅ Final chat list: ${chats.size} chats")
            Log.d("SellerMessages", "═══════════════════════════════════════")

        } catch (e: Exception) {
            Log.e("SellerMessages", "❌ Failed to load chats", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundSecondary,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "Messages",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Buyer conversations",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                chats.isEmpty() -> {
                    EmptyMessagesState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = chats,
                            key = { it.id }
                        ) { chat ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        chatToDelete = chat
                                        showDeleteDialog = true
                                        false
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Error)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = "Delete",
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true
                            ) {
                                ChatListItem(
                                    chat = chat,
                                    currentUserId = user.id,
                                    onClick = {
                                        val otherUserId = chat.participantIds.firstOrNull { it != user.id }
                                        if (otherUserId != null) {
                                            val otherUserName = chat.participantNames[otherUserId] ?: "Unknown"
                                            onChatClick(otherUserId, otherUserName)
                                        } else {
                                            Log.e("SellerMessages", "❌ Could not find other user ID in chat ${chat.id}")
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = BorderColor
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Delete Chat Dialog ────────────────────────────────────────────────────
    if (showDeleteDialog && chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Error.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete Chat",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this chat? All messages will be permanently deleted from both sides.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        chatToDelete?.let { chat ->
                            chatViewModel.deleteChat(chat.id)
                        }
                        showDeleteDialog = false
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        "Delete",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false; chatToDelete = null },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        "Cancel",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }
}

@Composable
fun ChatListItem(
    chat: Chat,
    currentUserId: String,
    onClick: () -> Unit
) {
    val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: return
    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown"
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    val isUnread = unreadCount > 0
    
    // Real-time profile picture listener
    var currentProfileImage by remember { mutableStateOf(chat.participantAvatars[otherUserId] ?: "") }
    
    DisposableEffect(otherUserId) {
        if (otherUserId.isEmpty()) return@DisposableEffect onDispose {}
        val listener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(otherUserId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    currentProfileImage = snapshot.getString("profile_image") ?: ""
                }
            }
        onDispose { listener.remove() }
    }

    Surface(
        onClick = onClick,
        color = if (isUnread) Color(0xFFFFF8F0) else Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with unread badge
            Box {
                if (currentProfileImage.isNotEmpty()) {
                    AsyncImage(
                        model = currentProfileImage,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCE4EC)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Gradient initials avatar
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Brush.linearGradient(listOf(Primary, PrimaryLight)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = otherUserName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Unread count badge
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .background(Primary, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Chat content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RealtimeNameDisplay(
                        userId = otherUserId,
                        fallbackName = otherUserName,
                        fontSize = 14.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = formatChatTime(chat.lastMessageTime),
                        fontSize = 11.sp,
                        color = if (isUnread) Primary else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        fontSize = 12.sp,
                        color = if (isUnread) TextPrimary else TextSecondary,
                        fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Primary, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMessagesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "No messages",
                tint = Primary.copy(alpha = 0.5f),
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No messages yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Messages from buyers will appear here",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

fun formatChatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> {
            val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
        diff < 604800000 -> {
            val formatter = SimpleDateFormat("EEE", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
        else -> {
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}