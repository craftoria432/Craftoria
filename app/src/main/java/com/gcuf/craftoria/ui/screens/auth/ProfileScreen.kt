package com.gcuf.craftoria.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.data.model.SellerApplicationStatus
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.gcuf.craftoria.ui.components.CraftoriaButton
import com.gcuf.craftoria.ui.components.CraftoriaTextField
import com.gcuf.craftoria.viewmodel.UnreadMessageViewModel
import com.gcuf.craftoria.ui.theme.*
import coil.compose.AsyncImage
import com.gcuf.craftoria.viewmodel.AuthState
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateTo: (String) -> Unit,
    unreadMessageViewModel: UnreadMessageViewModel = viewModel()
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser by viewModel.currentUser.collectAsState()
    val displayUser = currentUser ?: user
    val unreadMessageCount by unreadMessageViewModel.unreadCount.collectAsState()

    LaunchedEffect(displayUser.id) {
        if (displayUser.id.isNotEmpty()) unreadMessageViewModel.startListening(displayUser.id)
    }
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> { snackbarHostState.showSnackbar(state.message); viewModel.resetAuthState() }
            is AuthState.Error -> { snackbarHostState.showSnackbar(state.message); viewModel.resetAuthState() }
            else -> {}
        }
    }
    
    // ✅ REMOVED: listenToUserUpdates() is deprecated - real-time listener is already active in AuthViewModel.observeAuthState()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { imageUri = uri; viewModel.updateProfilePhoto(context, uri, displayUser.id) }
    }

    var isEditMode by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(displayUser.name) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showBecomeSellerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(displayUser.name) { if (!isEditMode) editedName = displayUser.name }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundSecondary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = displayUser.name,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundSecondary)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Animated Profile Hero Header ──────────────────────────────────
            ProfileHeroBanner(
                displayUser = displayUser,
                onEditPhoto = { imagePickerLauncher.launch("image/*") },
                onEditName = { showEditNameDialog = true }
            )

            // ── Content ───────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(14.dp)) {
                ViewModeContent(
                    user = displayUser,
                    authViewModel = viewModel,
                    onNavigateTo = onNavigateTo,
                    onChangePassword = { showPasswordDialog = true },
                    onDeleteAccount = { showDeleteDialog = true },
                    unreadMessageCount = unreadMessageCount,
                    onLogout = { showLogoutDialog = true },
                    onBecomeSellerClick = { showBecomeSellerDialog = true }
                )
            }
        }
    }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = displayUser.name,
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateUserName(displayUser.id, newName)
                showEditNameDialog = false
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                viewModel.changePassword(currentPassword, newPassword)
                showPasswordDialog = false
            }
        )
    }
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { showDeleteDialog = false; viewModel.deleteAccount { onLogout() } }
        )
    }

    if (showBecomeSellerDialog) {
        BecomeSellerConfirmationDialog(
            onDismiss = { showBecomeSellerDialog = false },
            onConfirm = {
                showBecomeSellerDialog = false
                viewModel.upgradeToSeller(user.id)
                onNavigateTo("verification")
            }
        )
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }
}

@Composable
fun BadgeChip(text: String, backgroundColor: Color, textColor: Color = Color.White) {
    Surface(shape = RoundedCornerShape(20.dp), color = backgroundColor) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ViewModeContent(
    user: User,
    authViewModel: AuthViewModel,
    onNavigateTo: (String) -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    unreadMessageCount: Int = 0,
    onLogout: () -> Unit,
    onBecomeSellerClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Account info card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            border = BorderStroke(0.5.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                InfoRow("Email", user.email)
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                InfoRow("Phone", user.phone)
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                InfoRow("Member Since", formatMemberSince(user.createdAt))
            }
        }

        // Seller application status card for buyers
        if (user.role == UserRole.BUYER) {
            when (user.sellerApplicationStatus) {
                SellerApplicationStatus.NONE -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                        border = BorderStroke(0.5.dp, Primary.copy(alpha = 0.20f)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Want to sell your products?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Join as a seller and reach thousands of customers!", fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                            }
                            CraftoriaButton(text = "Become a Seller", onClick = onBecomeSellerClick)
                        }
                    }
                }
                SellerApplicationStatus.PENDING -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD).copy(alpha = 0.3f)),
                        border = BorderStroke(0.5.dp, Color(0xFF856404).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF856404).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF856404),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Seller Application Pending", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Your application is under review. You'll be notified once approved.", fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                            }
                            CraftoriaButton(text = "View Status", onClick = { onNavigateTo("verification") }, isPrimary = false)
                        }
                    }
                }
                SellerApplicationStatus.REJECTED -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8D7DA).copy(alpha = 0.3f)),
                        border = BorderStroke(0.5.dp, Error.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Error.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = Error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Seller Application Rejected", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    if (user.rejectionReason.isNotEmpty()) {
                                        Text("Reason: ${user.rejectionReason}", fontSize = 11.sp, color = Error, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
                                    }
                                    Text("You can try again or revert to buyer account.", fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                            // ✅ Two action buttons: Try Again and Revert to Buyer
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        authViewModel.revertToBuyer(user.id)
                                    },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = TextSecondary
                                    ),
                                    border = BorderStroke(0.5.dp, BorderColor),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Revert to Buyer", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Button(
                                    onClick = { 
                                        authViewModel.resetSellerApplication(user.id)
                                        onNavigateTo("verification") 
                                    },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Primary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Try Again", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                SellerApplicationStatus.APPROVED -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8).copy(alpha = 0.3f)),
                        border = BorderStroke(0.5.dp, Success.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Success.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Application Approved!", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Complete your verification to start selling.", fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                                }
                            }
                            CraftoriaButton(text = "Complete Verification", onClick = { onNavigateTo("verification") })
                        }
                    }
                }
            }
        }

        // Seller-specific cards
        if (user.role == UserRole.SELLER) {
            if (!user.verified && user.verificationStatus == VerificationStatus.NOT_SUBMITTED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD).copy(alpha = 0.3f)),
                    border = BorderStroke(0.5.dp, Color(0xFF856404).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF856404).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFF856404),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text("Seller Account Not Verified", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("You can revert back to buyer or complete verification", fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { authViewModel.revertToBuyer(user.id) },
                                border = BorderStroke(0.5.dp, BorderColor),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Revert to Buyer",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    lineHeight = 15.sp
                                )
                            }
                            Button(
                                onClick = { onNavigateTo("verification") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Complete Verification",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                MenuSection(
                    title = "Seller Features",
                    items = listOf(
                        IconMenuItem(Icons.Outlined.Inventory2, "Manage Products", "manage_products"),
                        IconMenuItem(Icons.Outlined.ShoppingBag, "Seller Orders", "orders"),
                        IconMenuItem(Icons.Outlined.Group, "My Co-Seller Stores", "co_seller_stores"),
                        IconMenuItem(Icons.AutoMirrored.Outlined.Message, "Messages", "messages")
                    ),
                    onItemClick = onNavigateTo,
                    getBadgeCount = { route -> if (route == "messages") unreadMessageCount else 0 }
                )
            }
        }

        val generalItems = if (user.role == UserRole.BUYER) {
            listOf(
                IconMenuItem(Icons.Outlined.ShoppingBag, "My Orders", "my_orders"),
                IconMenuItem(Icons.Outlined.Receipt, "Payment History", "payment_history"),
                IconMenuItem(Icons.AutoMirrored.Outlined.Message, "My Chats", "chats"),
                IconMenuItem(Icons.Outlined.Notifications, "Notification Preferences", "notifications"),
                IconMenuItem(Icons.AutoMirrored.Outlined.Help, "Help & Support", "help"),
                IconMenuItem(Icons.Outlined.Description, "Terms & Conditions", "terms"),
                IconMenuItem(Icons.Outlined.Lock, "Privacy Policy", "privacy")
            )
        } else {
            listOf(
                IconMenuItem(Icons.Outlined.Notifications, "Notification Preferences", "notifications"),
                IconMenuItem(Icons.AutoMirrored.Outlined.Help, "Help & Support", "help"),
                IconMenuItem(Icons.Outlined.Description, "Terms & Conditions", "terms"),
                IconMenuItem(Icons.Outlined.Lock, "Privacy Policy", "privacy")
            )
        }

        MenuSection(
            title = "General",
            items = generalItems,
            onItemClick = onNavigateTo,
            getBadgeCount = { route -> if (route in listOf("chats", "messages")) unreadMessageCount else 0 }
        )

        MenuSection(
            title = "Security",
            items = listOf(IconMenuItem(Icons.Outlined.Lock, "Change Password", "change_password")),
            onItemClick = { if (it == "change_password") onChangePassword() else onNavigateTo(it) }
        )

        // Settings
        MenuSection(
            title = "Preferences",
            items = listOf(IconMenuItem(Icons.Outlined.Palette, "Appearance & Theme", "settings")),
            onItemClick = onNavigateTo
        )

        // Account / Danger zone
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            border = BorderStroke(0.5.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ACCOUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                // Logout — solid filled red, clearly a primary destructive action
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Delete Account — tinted surface, visually softer / secondary destructive
                Surface(
                    onClick = onDeleteAccount,
                    shape = RoundedCornerShape(10.dp),
                    color = Error.copy(alpha = 0.06f),
                    border = BorderStroke(0.5.dp, Error.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Error.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Error.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

fun formatMemberSince(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

data class IconMenuItem(val icon: ImageVector, val text: String, val route: String)
data class MenuItem(val icon: String, val text: String, val route: String)

@Composable
fun MenuSection(
    title: String,
    items: List<IconMenuItem>,
    onItemClick: (String) -> Unit,
    getBadgeCount: (String) -> Int = { 0 }
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            items.forEachIndexed { index, item ->
                MenuItemRow(
                    icon = item.icon,
                    text = item.text,
                    onClick = { onItemClick(item.route) },
                    badgeCount = getBadgeCount(item.route)
                )
                if (index < items.lastIndex) HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun MenuItemRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (badgeCount > 0) {
                    Badge(containerColor = Error, contentColor = Color.White) {
                        Text(
                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isEnabled = currentPassword.isNotBlank() && newPassword.isNotBlank() &&
            confirmPassword.isNotBlank() && newPassword == confirmPassword

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Gradient header band ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Change Password",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Update your account password",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }
                }

                // ── Form fields ──────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CraftoriaTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Current Password",
                        isPassword = true
                    )
                    CraftoriaTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "New Password",
                        isPassword = true
                    )
                    CraftoriaTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm New Password",
                        isPassword = true
                    )

                    // ── Action buttons ───────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cancel
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            border = BorderStroke(0.5.dp, BorderColor),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = TextSecondary
                            )
                        ) {
                            Text(
                                "Cancel",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Update — gradient fill, disabled state uses TextLight
                        Button(
                            onClick = {
                                if (isEnabled) onConfirm(currentPassword, newPassword)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            enabled = isEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isEnabled)
                                            Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                                        else
                                            Brush.horizontalGradient(listOf(TextLight, TextLight)),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Update",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Delete Account?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Are you sure? This action cannot be undone and all your data will be permanently removed.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextSecondary
                )
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
}


@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    val isValid = newName.isNotBlank() && newName != currentName

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Edit Full Name",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            CraftoriaTextField(
                value = newName,
                onValueChange = { newName = it },
                label = "Full Name",
                showLabel = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName); onDismiss() },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextSecondary
                )
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
}


@Composable
fun ProfileHeroBanner(displayUser: User, onEditPhoto: () -> Unit, onEditName: () -> Unit) {
    // Rotating animation for the dashed outer avatar ring
    val infiniteTransition = rememberInfiniteTransition(label = "ringRotation")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFC2185B), Primary, PrimaryLight),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // ── Decorative crosshatch canvas ──────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            val lineColor = Color.White.copy(alpha = 0.08f)
            val strokeW = 1.2f
            val step = 28.dp.toPx()
            val w = size.width
            val h = size.height
            var x = -h
            while (x <= w + h) {
                drawLine(color = lineColor, start = Offset(x, 0f), end = Offset(x + h, h), strokeWidth = strokeW)
                x += step
            }
            x = -h
            while (x <= w + h) {
                drawLine(color = lineColor, start = Offset(x, 0f), end = Offset(x - h, h), strokeWidth = strokeW)
                x += step
            }
        }

        // ── Decorative blurred orbs for depth ─────────────────────────────────
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-28).dp, y = 20.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Avatar with concentric rings ──────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(116.dp)
            ) {
                // Outer dashed ring — rotates slowly in production
                androidx.compose.foundation.Canvas(modifier = Modifier.size(116.dp)) {
                    val radius = size.minDimension / 2f - 4f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f),
                        radius = radius,
                        style = Stroke(
                            width = 1.5f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(8f, 6f), ringRotation
                            )
                        )
                    )
                }
                // Inner translucent ring
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                )
                // White avatar surface
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    if (displayUser.profileImage.isNotEmpty()) {
                        AsyncImage(
                            model = displayUser.profileImage,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().clickable { onEditPhoto() }
                        ) {
                            Text(
                                text = displayUser.name.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
                // Camera button
                Surface(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .clickable { onEditPhoto() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 3.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Name + edit button ────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(text = displayUser.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Surface(
                    modifier = Modifier.padding(start = 8.dp).size(28.dp).clickable { onEditName() },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Name", tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Role + verification badges ────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                when (displayUser.role) {
                    UserRole.BUYER -> {
                        BadgeChip("Buyer", backgroundColor = Color.White.copy(alpha = 0.25f))
                        when (displayUser.sellerApplicationStatus) {
                            SellerApplicationStatus.PENDING -> BadgeChip(
                                "Seller Pending",
                                backgroundColor = Color(0xFFFFF3CD),
                                textColor = Color(0xFF856404)
                            )
                            SellerApplicationStatus.REJECTED -> BadgeChip(
                                "Seller Rejected",
                                backgroundColor = Color(0xFFF8D7DA),
                                textColor = Color(0xFF721C24)
                            )
                            else -> {}
                        }
                    }
                    UserRole.SELLER -> {
                        BadgeChip("Seller", backgroundColor = Color.White.copy(alpha = 0.25f))
                        when (displayUser.verificationStatus) {
                            VerificationStatus.APPROVED -> BadgeChip("✓ Verified", backgroundColor = Color(0xFFE8F5E8), textColor = Color(0xFF2E7D2E))
                            VerificationStatus.PENDING -> BadgeChip("Pending Review", backgroundColor = Color(0xFFFFF3CD), textColor = Color(0xFF856404))
                            VerificationStatus.REJECTED -> BadgeChip("Rejected", backgroundColor = Color(0xFFF8D7DA), textColor = Color(0xFF721C24))
                            VerificationStatus.NOT_SUBMITTED -> BadgeChip("Not Verified", backgroundColor = Color(0xFFF8D7DA), textColor = Color(0xFF721C24))
                        }
                    }
                    UserRole.CO_SELLER -> BadgeChip("Co-Seller", backgroundColor = Color.White.copy(alpha = 0.25f))
                }
            }
        }
    }
}


@Composable
fun BecomeSellerConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Become a Seller?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "You're about to start your seller journey on Craftoria!",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Next steps:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        "• Complete face verification",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Text(
                        "• Wait for admin approval (24-48 hours)",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    Text(
                        "• Start selling your products",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Start Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextSecondary
                )
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
fun LogoutConfirmationDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Logout?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Are you sure you want to logout from your account?",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Logout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextSecondary
                )
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
}
