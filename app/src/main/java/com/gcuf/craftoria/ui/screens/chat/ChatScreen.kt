package com.gcuf.craftoria.ui.screens.chat

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Chat
import com.gcuf.craftoria.data.model.Message
import com.gcuf.craftoria.data.model.MessageType
import com.gcuf.craftoria.data.model.NegotiationStatus
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.ChatState
import com.gcuf.craftoria.viewmodel.ChatViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUser: User,
    otherUserId: String,
    otherUserName: String,
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
    val isCurrentUserSeller = currentUser.role == UserRole.SELLER

    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var otherUserProfileImage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val capturedImageUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            chat?.let { currentChat ->
                scope.launch {
                    try {
                        chatViewModel.sendImageMessage(context = context, chatId = currentChat.id, senderId = currentUser.id, senderName = currentUser.name, imageUri = uri)
                        snackbarHostState.showSnackbar("Image sent successfully")
                    } catch (e: Exception) { snackbarHostState.showSnackbar("Failed to send image: ${e.message}") }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val imageUri = capturedImageUri.value
            if (imageUri != null) {
                chat?.let { currentChat ->
                    scope.launch {
                        try {
                            chatViewModel.sendImageMessage(context = context, chatId = currentChat.id, senderId = currentUser.id, senderName = currentUser.name, imageUri = imageUri)
                            snackbarHostState.showSnackbar("Photo sent successfully")
                        } catch (e: Exception) { snackbarHostState.showSnackbar("Failed to send photo: ${e.message}") }
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val imageFile = java.io.File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.jpg")
            capturedImageUri.value = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
            capturedImageUri.value?.let { cameraLauncher.launch(it) }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Camera permission denied") }
        }
    }

    LaunchedEffect(otherUserId) {
        chatViewModel.initializeChat(currentUserId = currentUser.id, currentUserName = currentUser.name, otherUserId = otherUserId, otherUserName = otherUserName)
        // Real-time listener for other user's profile image
        if (otherUserId.isNotEmpty()) {
            try {
                val db = Firebase.firestore
                db.collection("users").document(otherUserId)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists()) {
                            val profileImage = snapshot.getString("profile_image") ?: ""
                            otherUserProfileImage = profileImage
                            Log.d("ChatScreen", "✅ Updated user profile image: ${if (profileImage.isNotEmpty()) "loaded" else "empty"}")
                        }
                    }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Error listening to user profile image: ${e.message}")
            }
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ChatState.ActionSuccess -> { snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short); chatViewModel.resetState() }
            is ChatState.Error -> { snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short); chatViewModel.resetState() }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatHeader(
                userName = otherUserName,
                userAvatar = otherUserProfileImage,
                isOnline = true,
                isBlocked = isBlocked,
                showViewProfile = !isCurrentUserSeller,
                onBackClick = onBackClick,
                onMenuClick = { showMenu = !showMenu },
                showMenu = showMenu,
                onViewProfile = { showMenu = false; Log.d("ChatScreen", "Navigating to profile: $otherUserId"); onViewProfile(otherUserId) },
                onBlockUser = { showBlockDialog = true; showMenu = false },
                onReportUser = { showReportDialog = true; showMenu = false }
            )
        },
        bottomBar = {
            if (!isBlocked) {
                ChatInput(
                    messageText = messageText, onMessageChange = { messageText = it },
                    onSendClick = {
                        if (messageText.isNotBlank() && chat != null) {
                            chatViewModel.sendMessage(chatId = chat!!.id, senderId = currentUser.id, senderName = currentUser.name, content = messageText)
                            messageText = ""
                        }
                    },
                    onAttachmentClick = { showAttachmentMenu = !showAttachmentMenu },
                    showAttachmentMenu = showAttachmentMenu,
                    onCloseAttachmentMenu = { showAttachmentMenu = false },
                    onCameraClick = {
                        showAttachmentMenu = false
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val imageFile = java.io.File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.jpg")
                            capturedImageUri.value = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
                            capturedImageUri.value?.let { cameraLauncher.launch(it) }
                        } else { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) }
                    },
                    onGalleryClick = { showAttachmentMenu = false; galleryLauncher.launch("image/*") }
                )
            } else { BlockedUserBar() }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is ChatState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(BackgroundSecondary),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(64.dp).background(Primary.copy(alpha = 0.08f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Primary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                                        }
                                        Text("Start a conversation", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("Send a message to begin chatting", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(messages) { message ->
                                MessageItem(
                                    message = message,
                                    currentUserId = currentUser.id,
                                    chat = chat,
                                    isOtherUserSeller = !isCurrentUserSeller,
                                    onViewProduct = onViewProduct,
                                    onTrackOrder = onTrackOrder,
                                    onAcceptOffer = {
                                        chat?.let { currentChat ->
                                            chatViewModel.updateNegotiationStatus(messageId = message.id, status = NegotiationStatus.ACCEPTED, chatId = currentChat.id, currentUserId = currentUser.id)
                                        }
                                    },
                                    onDeclineOffer = {
                                        chat?.let { currentChat ->
                                            chatViewModel.updateNegotiationStatus(messageId = message.id, status = NegotiationStatus.DECLINED, chatId = currentChat.id, currentUserId = currentUser.id)
                                        }
                                    },
                                    onDeleteMessage = { messageId -> chatViewModel.deleteMessage(messageId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Block User", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to block $otherUserName? You won't receive messages from this user.", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp) },
            confirmButton = {
                Button(onClick = { chat?.let { chatViewModel.blockUser(it.id, currentUser.id) }; showBlockDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Error), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Block", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBlockDialog = false }, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    if (showReportDialog) {
        ReportDialog(
            userName = otherUserName, onDismiss = { showReportDialog = false },
            onReport = { reason ->
                showReportDialog = false
                scope.launch {
                    try {
                        val reportRepository = com.gcuf.craftoria.data.repository.ReportRepository()
                        val result = reportRepository.submitReport(
                            reportType = if (isCurrentUserSeller) com.gcuf.craftoria.data.model.ReportType.BUYER else com.gcuf.craftoria.data.model.ReportType.SELLER,
                            reporterId = currentUser.id, reporterName = currentUser.name,
                            reportedEntityId = otherUserId, reportedEntityName = otherUserName,
                            reason = when (reason) { "spam" -> "Spam"; "harassment" -> "Harassment"; "inappropriate" -> "Inappropriate content"; "scam" -> "Scam or fraud"; else -> "Other" },
                            description = "Reported from chat"
                        )
                        snackbarHostState.showSnackbar(if (result.isSuccess) "Report submitted successfully" else "Failed to submit report", duration = SnackbarDuration.Short)
                    } catch (e: Exception) { snackbarHostState.showSnackbar("Error: ${e.message}", duration = SnackbarDuration.Short) }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHeader(
    userName: String,
    userAvatar: String = "",
    isOnline: Boolean,
    isBlocked: Boolean,
    showViewProfile: Boolean = true,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onViewProfile: () -> Unit,
    onBlockUser: () -> Unit,
    onReportUser: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                Box {
                    if (userAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = userAvatar,
                            contentDescription = "User Profile Picture",
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = userName.take(1).uppercase(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    if (isOnline && !isBlocked) {
                        Box(
                            modifier = Modifier.size(11.dp).align(Alignment.BottomEnd).clip(CircleShape)
                                .background(Color(0xFF4CAF50)).border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = userName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = if (isBlocked) "Blocked" else if (isOnline) "Active now" else "Offline",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 12.sp
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        },
        actions = {
            Box {
                IconButton(onClick = onMenuClick) {
                    Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = onMenuClick, containerColor = Color.White) {
                    if (showViewProfile) {
                        DropdownMenuItem(
                            text = { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp)); Text("View Profile", fontSize = 13.sp) } },
                            onClick = { onMenuClick(); onViewProfile() }
                        )
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    }
                    DropdownMenuItem(
                        text = { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = Error, modifier = Modifier.size(18.dp)); Text("Block User", fontSize = 13.sp, color = Error) } },
                        onClick = { onMenuClick(); onBlockUser() }
                    )
                    DropdownMenuItem(
                        text = { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Warning, modifier = Modifier.size(18.dp)); Text("Report", fontSize = 13.sp, color = Warning) } },
                        onClick = { onMenuClick(); onReportUser() }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
    )
}

@Composable
fun ChatInput(
    messageText: String, onMessageChange: (String) -> Unit, onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit, showAttachmentMenu: Boolean,
    onCloseAttachmentMenu: () -> Unit, onCameraClick: () -> Unit, onGalleryClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showAttachmentMenu) {
            Surface(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp, color = Color.White) {
                Column(modifier = Modifier.padding(6.dp)) {
                    AttachmentOption(icon = Icons.Default.CameraAlt, text = "Camera", onClick = onCameraClick)
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    AttachmentOption(icon = Icons.Default.Image, text = "Gallery", onClick = onGalleryClick)
                }
            }
        }
        Surface(color = Color.White, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(if (showAttachmentMenu) Primary.copy(alpha = 0.12f) else BackgroundSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onAttachmentClick, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = if (showAttachmentMenu) Icons.Default.Close else Icons.Default.AttachFile, contentDescription = null, tint = if (showAttachmentMenu) Primary else TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                OutlinedTextField(
                    value = messageText, onValueChange = onMessageChange,
                    placeholder = { Text("Type a message...", fontSize = 13.sp, color = TextSecondary) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                    singleLine = true
                )
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(
                        if (messageText.isNotBlank()) Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                        else Brush.horizontalGradient(listOf(BackgroundSecondary, BackgroundSecondary))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onSendClick, enabled = messageText.isNotBlank(), modifier = Modifier.size(38.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (messageText.isNotBlank()) Color.White else TextLight, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(30.dp).background(Primary.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = text, tint = Primary, modifier = Modifier.size(15.dp))
            }
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
fun BlockedUserBar() {
    Surface(color = Color(0xFFFFF3E0), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "You cannot message this user", fontSize = 13.sp, color = Color(0xFFE65100), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
        if (!isSent) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(
                    if (isOtherUserSeller) Brush.horizontalGradient(listOf(Color(0xFF1976D2), Color(0xFF42A5F5)))
                    else Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                ),
                contentAlignment = Alignment.Center
            ) {
                val senderAvatar = chat?.participantAvatars?.get(message.senderId) ?: ""
                if (senderAvatar.isNotEmpty()) {
                    AsyncImage(model = senderAvatar, contentDescription = "Sender Avatar", modifier = Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Text(text = message.senderName.take(1).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp, end = if (isSent) 4.dp else 0.dp)
            ) {
                Text(text = getTimeString(message.createdAt), fontSize = 10.sp, color = TextSecondary)
                if (isSent) {
                    Text(
                        text = if (message.isRead || message.deliveredAt > 0) "✓✓" else "✓",
                        fontSize = 10.sp,
                        color = if (message.isRead) Color(0xFF2196F3) else TextSecondary
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Delete Message", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure? This action cannot be undone.", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp) },
            confirmButton = {
                Button(onClick = { onDeleteMessage(message.id); showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Error), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
fun TextMessage(message: Message, isSent: Boolean) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isSent) 16.dp else 4.dp, bottomEnd = if (isSent) 4.dp else 16.dp),
        color = if (isSent) Primary else Color.White,
        shadowElevation = if (isSent) 0.dp else 1.dp
    ) {
        Text(text = message.content, fontSize = 13.sp, color = if (isSent) Color.White else TextPrimary, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
    }
}

@Composable
fun ImageMessage(message: Message, isSent: Boolean) {
    Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
        AsyncImage(model = message.imageUrl, contentDescription = "Image", modifier = Modifier.widthIn(max = 220.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
    }
}

@Composable
fun ProductMessage(message: Message, isSent: Boolean, onViewProduct: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), modifier = Modifier.width(220.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(BackgroundSecondary), contentAlignment = Alignment.Center) {
                if (message.productImage.isNotEmpty()) AsyncImage(model = message.productImage, contentDescription = message.productName, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentScale = ContentScale.Crop)
                else Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = TextLight, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = message.productName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
                Text(text = "PKR ${String.format("%,.0f", message.productPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
                Button(onClick = { onViewProduct(message.productId) }, modifier = Modifier.fillMaxWidth().height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("View Product", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderUpdateMessage(message: Message, isSent: Boolean, onTrackOrder: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), modifier = Modifier.width(220.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).background(Primary.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                }
                Text(text = "Order Update", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Text(text = message.content, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
            Button(onClick = { onTrackOrder(message.orderId) }, modifier = Modifier.fillMaxWidth().height(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("Track Order", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun NegotiationMessage(message: Message, isSent: Boolean, currentUserId: String, onAcceptOffer: () -> Unit, onDeclineOffer: () -> Unit) {
    val (bgColor, borderColor) = when (message.negotiationStatus) {
        NegotiationStatus.ACCEPTED -> Color(0xFFE8F5E9) to Success
        NegotiationStatus.DECLINED -> Color(0xFFFFEBEE) to Error
        else -> Color(0xFFFFF8E1) to Color(0xFFFFA726)
    }
    Card(colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor.copy(alpha = 0.5f)), modifier = Modifier.width(220.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (message.negotiationStatus) {
                NegotiationStatus.PENDING -> {
                    Text(text = "Offer for ${message.productName}", fontSize = 11.sp, color = TextSecondary)
                    Text(text = "PKR ${String.format("%,.0f", message.negotiationPrice)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    if (!isSent) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = onAcceptOffer, modifier = Modifier.weight(1f).height(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Success), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)) { Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                            OutlinedButton(onClick = onDeclineOffer, modifier = Modifier.weight(1f).height(30.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) { Text("Decline", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
                NegotiationStatus.ACCEPTED -> Text(text = "✓ Accepted — PKR ${String.format("%,.0f", message.negotiationPrice)}", fontSize = 12.sp, color = Success, fontWeight = FontWeight.SemiBold)
                NegotiationStatus.DECLINED -> Text(text = "✗ Offer Declined", fontSize = 12.sp, color = Error, fontWeight = FontWeight.SemiBold)
                else -> Text(text = "Negotiation status: ${message.negotiationStatus}", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun EmptyChatState() {
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(72.dp).background(Primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = Primary.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Start a conversation", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Send a message to begin chatting", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun ReportDialog(userName: String, onDismiss: () -> Unit, onReport: (String) -> Unit) {
    var selectedReason by remember { mutableStateOf("spam") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(modifier = Modifier.size(56.dp).background(Warning.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Warning, modifier = Modifier.size(28.dp))
            }
        },
        title = { Text("Report User", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text("Select a reason for reporting $userName:", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))
                listOf("spam" to "Spam", "harassment" to "Harassment", "inappropriate" to "Inappropriate content", "scam" to "Scam or fraud", "other" to "Other").forEach { (value, label) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedReason == value, onClick = { selectedReason = value }, colors = RadioButtonDefaults.colors(selectedColor = Primary))
                        Text(text = label, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(40.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Text(
                    "Submit Report",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
}

fun getTimeString(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < 86400000 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}