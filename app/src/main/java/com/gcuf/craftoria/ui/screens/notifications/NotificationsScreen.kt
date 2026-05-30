package com.gcuf.craftoria.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.gcuf.craftoria.data.model.UserRole
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.Notification
import com.gcuf.craftoria.data.model.NotificationActionType
import com.gcuf.craftoria.data.model.NotificationCategory
import com.gcuf.craftoria.ui.components.NotificationCategoryFilterTabs
import com.gcuf.craftoria.ui.components.EmptyStateComponent
import androidx.compose.foundation.BorderStroke
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.NotificationUiState
import com.gcuf.craftoria.viewmodel.NotificationViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.compose.material.icons.filled.Notifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    user: User,
    onBackClick: () -> Unit,
    onNotificationAction: (Notification) -> Unit,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val uiState by notificationViewModel.uiState.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val currentFilter by notificationViewModel.currentFilter.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmDialogData by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNotifications by remember { mutableStateOf(setOf<String>()) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.id) {
        notificationViewModel.loadNotifications(user.id)
        notificationViewModel.startListening(user.id)
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is NotificationUiState.ActionSuccess -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                notificationViewModel.resetState()
            }
            is NotificationUiState.Error -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                notificationViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount unread",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 13.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Delete selected — Error tinted pill
                        if (selectedNotifications.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    confirmDialogData = "Delete Selected" to {
                                        notificationViewModel.deleteMultipleNotifications(
                                            selectedNotifications.toList(), user.id
                                        )
                                        selectedNotifications = emptySet()
                                        isSelectionMode = false
                                    }
                                    showConfirmDialog = true
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = Error.copy(alpha = 0.9f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Delete (${selectedNotifications.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        // Cancel selection
                        TextButton(
                            onClick = { isSelectionMode = false; selectedNotifications = emptySet() },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        // Delete icon — tinted circle
                        if (notifications.isNotEmpty()) {
                            IconButton(
                                onClick = { isSelectionMode = true },
                                modifier = Modifier.padding(end = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        // Mark all read — white pill with Primary icon + text
                        if (unreadCount > 0) {
                            Surface(
                                onClick = { notificationViewModel.markAllAsRead(user.id) },
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Mark All Read",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
                )
            )
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter tabs — pill style, consistent with all other filter tabs in project
            NotificationCategoryFilterTabs(
                currentFilter = currentFilter,
                onFilterSelected = { filter ->
                    notificationViewModel.filterNotifications(filter, user.id)
                },
                userRole = if (user.role == UserRole.SELLER) "seller" else "buyer"
            )

            when (uiState) {
                is NotificationUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is NotificationUiState.Empty -> {
                    // ✅ BUG FIX 7: Drive empty state off notifications.isEmpty() directly
                    // This prevents empty-state flash when filter changes but uiState lags
                    if (notifications.isEmpty()) {
                        EmptyNotificationUiState(currentFilter = currentFilter)
                    } else {
                        NotificationList(
                            notifications = notifications,
                            isSelectionMode = isSelectionMode,
                            selectedNotifications = selectedNotifications,
                            onSelectionToggle = { id ->
                                selectedNotifications =
                                    if (selectedNotifications.contains(id))
                                        selectedNotifications - id
                                    else selectedNotifications + id
                            },
                            onMarkAsRead = { id -> notificationViewModel.markAsRead(id, user.id) },
                            onDelete = { id ->
                                confirmDialogData = "Delete Notification" to {
                                    notificationViewModel.deleteNotification(id, user.id)
                                }
                                showConfirmDialog = true
                            },
                            onAction = { action, notification ->
                                when (action) {
                                    "accept_invitation" -> {
                                        confirmDialogData = "Accept Invitation" to {
                                            onNotificationAction(notification)
                                            notificationViewModel.markAsRead(notification.id, user.id)
                                        }
                                        showConfirmDialog = true
                                    }
                                    "decline_invitation" -> {
                                        confirmDialogData = "Decline Invitation" to {
                                            notificationViewModel.deleteNotification(notification.id, user.id)
                                        }
                                        showConfirmDialog = true
                                    }
                                    else -> {
                                        onNotificationAction(notification)
                                        if (!notification.isRead)
                                            notificationViewModel.markAsRead(notification.id, user.id)
                                    }
                                }
                            }
                        )
                    }
                }
                else -> {
                    // ✅ BUG FIX 7: Drive content area off notifications.isEmpty() directly
                    if (notifications.isEmpty()) {
                        EmptyNotificationUiState(currentFilter = currentFilter)
                    } else {
                        NotificationList(
                            notifications = notifications,
                            isSelectionMode = isSelectionMode,
                            selectedNotifications = selectedNotifications,
                            onSelectionToggle = { id ->
                                selectedNotifications =
                                    if (selectedNotifications.contains(id))
                                        selectedNotifications - id
                                    else selectedNotifications + id
                            },
                            onMarkAsRead = { id -> notificationViewModel.markAsRead(id, user.id) },
                            onDelete = { id ->
                                confirmDialogData = "Delete Notification" to {
                                    notificationViewModel.deleteNotification(id, user.id)
                                }
                                showConfirmDialog = true
                            },
                            onAction = { action, notification ->
                                when (action) {
                                    "accept_invitation" -> {
                                        confirmDialogData = "Accept Invitation" to {
                                            onNotificationAction(notification)
                                            notificationViewModel.markAsRead(notification.id, user.id)
                                        }
                                        showConfirmDialog = true
                                    }
                                    "decline_invitation" -> {
                                        confirmDialogData = "Decline Invitation" to {
                                            notificationViewModel.deleteNotification(notification.id, user.id)
                                        }
                                        showConfirmDialog = true
                                    }
                                    else -> {
                                        onNotificationAction(notification)
                                        if (!notification.isRead)
                                            notificationViewModel.markAsRead(notification.id, user.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Confirm Dialog ────────────────────────────────────────────────────────
    if (showConfirmDialog && confirmDialogData != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    confirmDialogData!!.first,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "Are you sure you want to proceed?",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                // Gradient confirm — consistent with all confirm dialogs
                Button(
                    onClick = {
                        confirmDialogData!!.second()
                        showConfirmDialog = false
                        confirmDialogData = null
                    },
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
                        "Confirm",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false; confirmDialogData = null },
                    border = BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                }
            }
        )
    }
}

// ── Notification Card ─────────────────────────────────────────────────────────

@Composable
fun NotificationCard(
    notification: Notification,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectionToggle: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onAction: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFFFF5F8)
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = when {
                isSelected -> Primary
                !notification.isRead -> PrimaryLight.copy(alpha = 0.6f)
                else -> BorderColor
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (isSelectionMode) onSelectionToggle()
            else if (!notification.isRead) onMarkAsRead()
        }
    ) {
        Box {
            Row(modifier = Modifier.padding(13.dp)) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Primary,
                            uncheckedColor = TextSecondary
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Category icon — tinted circle
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    getIconBackground(notification.categoryEnum),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(notification.categoryEnum),
                                contentDescription = null,
                                tint = getCategoryIconTint(notification.categoryEnum),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notification.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 2.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // ✅ FIXED: Style amount text inline with primary pink color, bold, and dark
                            val displayDescription = if (notification.categoryEnum == NotificationCategory.REFUNDS) {
                                // Create annotated string with styled amount
                                buildAnnotatedString {
                                    val description = notification.description.replace("₹", "₨")
                                    val amountPattern = Regex("(PKR|Rs|₨)\\s*([0-9,]+)")
                                    val match = amountPattern.find(description)
                                    
                                    if (match != null) {
                                        // Add text before amount
                                        append(description.substring(0, match.range.first))
                                        
                                        // Add styled amount text
                                        withStyle(
                                            style = SpanStyle(
                                                color = Primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        ) {
                                            append(match.value)
                                        }
                                        
                                        // Add text after amount
                                        append(description.substring(match.range.last + 1))
                                    } else {
                                        append(description)
                                    }
                                }
                            } else {
                                AnnotatedString(notification.description.replace("₹", "₨"))
                            }
                            
                            Text(
                                text = displayDescription,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(bottom = 5.dp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Store pill — 0.5.dp BorderColor surface with real-time updates
                            // ✅ FIXED: Only show if co-seller store ID is present (not empty)
                            if ((notification.storeName.isNotEmpty() || notification.storeId.isNotEmpty()) && notification.storeId.isNotEmpty()) {
                                var realtimeStoreName by remember(notification.storeId) { mutableStateOf(notification.storeName) }
                                var realtimeMemberCount by remember(notification.storeId) { mutableStateOf(notification.memberCount) }
                                
                                DisposableEffect(notification.storeId) {
                                    if (notification.storeId.isEmpty()) return@DisposableEffect onDispose {}
                                    
                                    var storeRegistration: ListenerRegistration? = null
                                    
                                    try {
                                        val db = FirebaseFirestore.getInstance()
                                        
                                        // ✅ FIXED: Single listener on co_seller_stores document
                                        // Store name from store_name field, member count from member_ids or member_count
                                        storeRegistration = db.collection("co_seller_stores").document(notification.storeId)
                                            .addSnapshotListener { snapshot, error ->
                                                if (error == null && snapshot != null && snapshot.exists()) {
                                                    // Get store name from store_name field
                                                    val name = snapshot.getString("store_name") ?: notification.storeName
                                                    realtimeStoreName = name
                                                    Log.d("NotificationCard", "✅ Updated store name to: $name")
                                                    
                                                    // Get member count from member_ids array or member_count field
                                                    val memberCount = (snapshot.get("member_ids") as? List<*>)?.size
                                                        ?: snapshot.getLong("member_count")?.toInt()
                                                        ?: notification.memberCount
                                                    realtimeMemberCount = memberCount
                                                    Log.d("NotificationCard", "✅ Updated member count to: $memberCount")
                                                } else if (error != null) {
                                                    Log.e("NotificationCard", "Error fetching store data: ${error.message}")
                                                }
                                            }
                                    } catch (e: Exception) {
                                        Log.e("NotificationCard", "Error setting up listener: ${e.message}")
                                    }
                                    
                                    onDispose {
                                        storeRegistration?.remove()
                                    }
                                }
                                
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BackgroundSecondary,
                                    border = BorderStroke(0.5.dp, BorderColor),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp, vertical = 5.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Store,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = realtimeStoreName,
                                            fontSize = 11.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = "·",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.Group,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "$realtimeMemberCount Members",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Text(
                                text = getTimeAgo(notification.createdAt),
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        // Delete icon — only when read and not in selection mode
                        if (!isSelectionMode && notification.isRead) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = TextLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (notification.actionTypeEnum != NotificationActionType.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NotificationActions(
                            actionType = notification.actionTypeEnum,
                            onAction = onAction
                        )
                    }
                }
            }

            // Unread dot — Primary circle top-end
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(13.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary)
                )
            }
        }
    }
}

// ── Category helpers ──────────────────────────────────────────────────────────

fun getCategoryIcon(category: NotificationCategory): ImageVector {
    return when (category) {
        NotificationCategory.UNREAD -> Icons.Outlined.MailOutline  // ✅ NEW
        NotificationCategory.ORDERS -> Icons.Outlined.ShoppingBag
        NotificationCategory.MESSAGES -> Icons.AutoMirrored.Outlined.Message
        NotificationCategory.PROMOTIONS -> Icons.Outlined.Campaign
        NotificationCategory.SYSTEM -> Icons.Outlined.CheckCircle
        NotificationCategory.REPORT -> Icons.Outlined.Flag
        NotificationCategory.ADMIN_MESSAGE -> Icons.Outlined.AdminPanelSettings
        NotificationCategory.STORE_RATING -> Icons.Outlined.Star  // ✅ NEW
        NotificationCategory.PAYMENTS -> Icons.Outlined.ShoppingBag
        NotificationCategory.REFUNDS -> Icons.Outlined.CurrencyExchange  // ✅ FIXED: Professional refund icon (currency exchange)
        else -> Icons.Outlined.Notifications
    }
}

fun getCategoryIconTint(category: NotificationCategory): Color {
    return when (category) {
        NotificationCategory.UNREAD -> Color(0xFF1976D2)  // ✅ NEW: Blue for unread
        NotificationCategory.ORDERS -> Color(0xFFE91E8C)
        NotificationCategory.MESSAGES -> Color(0xFF1976D2)
        NotificationCategory.PROMOTIONS -> Color(0xFFF57F17)
        NotificationCategory.SYSTEM -> Color(0xFF2E7D32)
        NotificationCategory.REPORT -> Color(0xFFD32F2F)  // ✅ NEW: Red for reports
        NotificationCategory.ADMIN_MESSAGE -> Color(0xFFD32F2F)
        NotificationCategory.STORE_RATING -> Color(0xFFFFA500)  // ✅ NEW: Orange for ratings
        NotificationCategory.PAYMENTS -> Color(0xFF2E7D32)
        NotificationCategory.REFUNDS -> Color(0xFF5A2D82)  // ✅ FIXED: Purple for refunds (consistent with refund badges)
        else -> Color(0xFF757575)
    }
}

fun getIconBackground(category: NotificationCategory): Color {
    return when (category) {
        NotificationCategory.UNREAD -> Color(0xFFE3F2FD)  // ✅ NEW: Light blue background
        NotificationCategory.ORDERS -> Color(0xFFFFF5F8)
        NotificationCategory.MESSAGES -> Color(0xFFE3F2FD)
        NotificationCategory.PROMOTIONS -> Color(0xFFFFF9C4)
        NotificationCategory.SYSTEM -> Color(0xFFE8F5E8)
        NotificationCategory.REPORT -> Color(0xFFFFEBEE)  // ✅ NEW: Light red background
        NotificationCategory.ADMIN_MESSAGE -> Color(0xFFFFEBEE)
        NotificationCategory.STORE_RATING -> Color(0xFFFFF3E0)  // ✅ NEW: Light orange background
        NotificationCategory.PAYMENTS -> Color(0xFFE8F5E9)
        NotificationCategory.REFUNDS -> Color(0xFFE2D5F3)  // ✅ FIXED: Light purple background (consistent with refund badges)
        else -> Color(0xFFF5F5F5)
    }
}

// ── Notification Actions ──────────────────────────────────────────────────────

@Composable
fun NotificationActions(actionType: NotificationActionType, onAction: (String) -> Unit) {
    when (actionType) {
        NotificationActionType.ACCEPT_INVITATION -> {
            // Accept + Decline side by side
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAction("accept_invitation") },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { onAction("decline_invitation") },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Decline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        NotificationActionType.VIEW_ORDER -> {
            // Gradient fill — primary action with hover effect
            var isHovered by remember { mutableStateOf(false) }
            Button(
                onClick = { onAction("view_order") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                isHovered = interaction is androidx.compose.foundation.interaction.HoverInteraction.Enter
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isHovered) {
                                Brush.horizontalGradient(listOf(Color(0xFFE91E8C), Color(0xFFF06292)))
                            } else {
                                Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                            },
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "View Order",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        NotificationActionType.TRACK_ORDER -> {
            // Gradient fill with hover effect
            var isHovered by remember { mutableStateOf(false) }
            Button(
                onClick = { onAction("track_order") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                isHovered = interaction is androidx.compose.foundation.interaction.HoverInteraction.Enter
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isHovered) {
                                Brush.horizontalGradient(listOf(Color(0xFFE91E8C), Color(0xFFF06292)))
                            } else {
                                Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                            },
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Track Order",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        NotificationActionType.REPLY_MESSAGE -> {
            // Blue solid — distinct from Primary for messaging context
            Button(
                onClick = { onAction("reply_message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View & Reply", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        NotificationActionType.VIEW_STORE -> {
            Button(
                onClick = { onAction("view_store") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "View Store",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        NotificationActionType.VIEW_PROMOTIONS -> {
            Button(
                onClick = { onAction("view_promotions") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Browse Offers",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        NotificationActionType.RATE_ORDER -> {
            Button(
                onClick = { onAction("rate_order") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Rate Order",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        NotificationActionType.VIEW_PRODUCT -> {
            Button(
                onClick = { onAction("view_product") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "View Product",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        NotificationActionType.VIEW_RATING -> {
            // ✅ NEW: View store rating details — orange gradient for ratings
            Button(
                onClick = { onAction("view_rating") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFA500), Color(0xFFFFB84D))),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "View Rating",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        else -> {}
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
fun EmptyNotificationUiState(currentFilter: NotificationCategory = NotificationCategory.ALL) {
    val (title, message) = when (currentFilter) {
        NotificationCategory.UNREAD -> Pair(
            "No unread notifications",
            "All notifications have been read"
        )
        NotificationCategory.ORDERS -> Pair(
            "No order notifications",
            "Order updates and status changes will appear here"
        )
        NotificationCategory.PAYMENTS -> Pair(
            "No payment notifications",
            "Payment confirmations and transaction updates will appear here"
        )
        NotificationCategory.REFUNDS -> Pair(
            "No refund notifications",
            "Refund requests and status updates will appear here"
        )
        NotificationCategory.MESSAGES -> Pair(
            "No message notifications",
            "New messages from buyers and sellers will appear here"
        )
        NotificationCategory.PROMOTIONS -> Pair(
            "No promotion notifications",
            "Special offers and promotional updates will appear here"
        )
        NotificationCategory.SYSTEM -> Pair(
            "No system notifications",
            "Important system updates and announcements will appear here"
        )
        NotificationCategory.STORE_RATING -> Pair(
            "No store rating notifications",
            "Customer reviews and ratings will appear here"
        )
        NotificationCategory.REPORT -> Pair(
            "No report notifications",
            "Report updates and resolutions will appear here"
        )
        NotificationCategory.ADMIN_MESSAGE -> Pair(
            "No admin messages",
            "Important messages from administrators will appear here"
        )
        NotificationCategory.ALL -> Pair(
            "No notifications yet",
            "You're all caught up! New notifications will appear here"
        )
    }
    
    EmptyStateComponent(
        icon = Icons.Default.Notifications,
        title = title,
        message = message
    )
}

@Composable
private fun NotificationList(
    notifications: List<Notification>,
    isSelectionMode: Boolean,
    selectedNotifications: Set<String>,
    onSelectionToggle: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAction: (String, Notification) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ✅ BUG FIX 6: Added key = { it.id } for stable identity
        // Prevents card recycling flicker when list updates
        items(notifications, key = { it.id }) { notification ->
            NotificationCard(
                notification = notification,
                isSelectionMode = isSelectionMode,
                isSelected = selectedNotifications.contains(notification.id),
                onSelectionToggle = { onSelectionToggle(notification.id) },
                onMarkAsRead = { onMarkAsRead(notification.id) },
                onDelete = { onDelete(notification.id) },
                onAction = { action -> onAction(action, notification) }
            )
        }
    }
}
// ── Time helper ───────────────────────────────────────────────────────────────

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
        days < 30 -> "${days / 7} week${if (days / 7 > 1) "s" else ""} ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}