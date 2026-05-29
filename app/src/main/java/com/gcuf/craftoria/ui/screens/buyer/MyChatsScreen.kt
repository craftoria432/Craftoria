package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Chat
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyChatsScreen(
    userId: String,
    onBackClick: () -> Unit,
    onChatClick: (String, String) -> Unit,
    chatViewModel: com.gcuf.craftoria.viewmodel.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by chatViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is com.gcuf.craftoria.viewmodel.ChatState.ActionSuccess -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                chatViewModel.resetState()
            }
            is com.gcuf.craftoria.viewmodel.ChatState.Error -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                chatViewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(userId) {
        isLoading = true
        android.util.Log.d("MyChatsScreen", "🔍 Loading chats for buyer: $userId")
        try {
            if (userId.isBlank()) { isLoading = false; return@LaunchedEffect }
            val snapshot = FirebaseFirestore.getInstance().collection("chats").whereArrayContains("participant_ids", userId).get().await()
            val parsedChats = snapshot.documents.mapNotNull { doc ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val participantIds = (doc.get("participant_ids") as? List<*>)?.map { it.toString() } ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val participantNames = doc.get("participant_names") as? Map<String, String> ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    val unreadCount = doc.get("unread_count") as? Map<String, Any> ?: emptyMap()
                    val unreadCountInt = unreadCount.mapValues { (it.value as? Long)?.toInt() ?: 0 }
                    Chat(id = doc.id, participantIds = participantIds, participantNames = participantNames, lastMessage = doc.getString("last_message") ?: "", lastMessageTime = doc.getLong("last_message_time") ?: 0L, lastMessageSenderId = doc.getString("last_message_sender_id") ?: "", unreadCount = unreadCountInt, isBlocked = doc.getBoolean("is_blocked") ?: false, blockedBy = doc.getString("blocked_by") ?: "", createdAt = doc.getLong("created_at") ?: 0L)
                } catch (e: Exception) { null }
            }
            chats = parsedChats.sortedByDescending { it.lastMessageTime }.distinctBy { it.participantIds.sorted().joinToString("-") }
        } catch (e: Exception) {
            android.util.Log.e("MyChatsScreen", "❌ Failed to load chats", e)
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
                            text = "My Chats",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Messages with sellers",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 12.sp
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
                actions = {
                    if (chats.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Delete All",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                chats.isEmpty() -> EmptyChatsState()
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        items(items = chats, key = { it.id }) { chat ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        chatToDelete = chat
                                        showDeleteDialog = true
                                        false
                                    } else false
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
                                BuyerChatListItem(
                                    chat = chat, currentUserId = userId,
                                    onClick = {
                                        val sellerId = chat.participantIds.firstOrNull { it != userId }
                                        if (sellerId != null) {
                                            val sellerName = chat.participantNames[sellerId] ?: "Seller"
                                            onChatClick(sellerId, sellerName)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }



    // ── Delete Single Chat Dialog ────────────────────────────────────────────
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
                        contentDescription = null,
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
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        chatToDelete?.let { chat ->
                            // ✅ INSTANT: Remove from UI immediately
                            chats = chats.filter { it.id != chat.id }
                            
                            // ✅ Background: Delete from backend
                            chatViewModel.deleteChat(chat.id)
                        }
                        showDeleteDialog = false
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false; chatToDelete = null },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
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
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    "Delete All Chats",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete ALL ${chats.size} conversations? This cannot be undone.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // ✅ INSTANT: Clear UI immediately
                        chats = emptyList()
                        
                        // ✅ Background: Delete from backend
                        chatViewModel.deleteAllChats(userId)
                        
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Delete All", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteAllDialog = false },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
fun BuyerChatListItem(chat: Chat, currentUserId: String, onClick: () -> Unit) {
    val sellerId = chat.participantIds.firstOrNull { it != currentUserId } ?: return
    val sellerName = chat.participantNames[sellerId] ?: "Seller"
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    val isUnread = unreadCount > 0
    
    // Real-time profile picture listener
    var currentProfileImage by remember { mutableStateOf(chat.participantAvatars[sellerId] ?: "") }
    
    DisposableEffect(sellerId) {
        if (sellerId.isEmpty()) return@DisposableEffect onDispose {}
        val listener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(sellerId)
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
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture or gradient initials avatar with unread badge
                Box(modifier = Modifier.size(46.dp)) {
                    if (currentProfileImage.isNotEmpty()) {
                        AsyncImage(
                            model = currentProfileImage,
                            contentDescription = "Seller Profile Picture",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFCE4EC)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sellerName.take(1).uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.TopEnd)
                                .background(Error, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Chat info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RealtimeNameDisplay(
                                userId = sellerId,
                                fallbackName = sellerName,
                                fontSize = 14.sp,
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Surface(
                                color = Primary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "✓",
                                    fontSize = 9.sp,
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = formatChatTime(chat.lastMessageTime),
                            fontSize = 11.sp,
                            color = if (isUnread) Primary else TextSecondary,
                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
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
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Primary)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 71.dp),
                color = BorderColor,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
fun EmptyChatsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "No conversations yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Start chatting with sellers about products you're interested in",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

fun formatChatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60000 -> "Now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        diff < 604800000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}