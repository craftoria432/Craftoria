package com.gcuf.craftoria.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.ui.components.CraftoriaButton
import com.gcuf.craftoria.ui.components.CraftoriaTextField
import com.gcuf.craftoria.ui.components.CraftoriaTopBar
import com.gcuf.craftoria.ui.theme.*
import coil.compose.AsyncImage
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    user: User,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel()

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            viewModel.updateProfilePhoto(context, uri, user.id)
        }
    }

    var isEditMode by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(user.name) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CraftoriaTopBar(
            screenNumber = "03",
            title = "Profile",
            showBack = true,
            showEdit = !isEditMode,
            onBackClick = onBackClick,
            onEditClick = { isEditMode = true }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Primary, PrimaryLight)
                        )
                    )
                    .padding(bottom = 28.dp, top = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            if (user.profileImage.isNotEmpty()) {
                                AsyncImage(
                                    model = user.profileImage,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = user.name.firstOrNull()?.uppercase() ?: "U",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change photo",
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }


                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isEditMode) editedName else user.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (user.role == UserRole.BUYER) {
                            BadgeChip("🛍️ Buyer", backgroundColor = Color.White.copy(alpha = 0.3f))
                        }
                        if (user.role == UserRole.SELLER) {
                            BadgeChip("👩‍💼 Seller", backgroundColor = Color.White.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.width(8.dp))

                            when (user.verificationStatus) {
                                VerificationStatus.APPROVED -> BadgeChip(
                                    "✓ Verified",
                                    backgroundColor = Color(0xFFE8F5E8),
                                    textColor = Color(0xFF2E7D2E)
                                )
                                VerificationStatus.PENDING -> BadgeChip(
                                    "⏱ Pending",
                                    backgroundColor = Color(0xFFFFF3CD),
                                    textColor = Color(0xFF856404)
                                )
                                VerificationStatus.REJECTED, VerificationStatus.NOT_SUBMITTED -> BadgeChip(
                                    "❌ Not Verified",
                                    backgroundColor = Color(0xFFF8D7DA),
                                    textColor = Color(0xFF721C24)
                                )
                            }
                        }
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                if (isEditMode) {
                    EditModeContent(
                        editedName = editedName,
                        onNameChange = { editedName = it },
                        email = user.email,
                        phoneNumber = user.phone,
                        onSave = {
                            // TODO: Save changes
                            isEditMode = false
                        },
                        onCancel = {
                            editedName = user.name
                            isEditMode = false
                        }
                    )
                } else {
                    ViewModeContent(
                        user = user,
                        onNavigateTo = onNavigateTo,
                        onChangePassword = { showPasswordDialog = true },
                        onDeleteAccount = { showDeleteDialog = true },
                        onLogout = onLogout
                    )
                }
            }
        }
    }

    // Change Password Dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { currentPass, newPass ->
                // TODO: Change password
                showPasswordDialog = false
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                // TODO: Delete account
                showDeleteDialog = false
                onLogout()
            }
        )
    }
}

@Composable
fun BadgeChip(
    text: String,
    backgroundColor: Color,
    textColor: Color = Color.White
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = backgroundColor
    ) {
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
fun EditModeContent(
    editedName: String,
    onNameChange: (String) -> Unit,
    email: String,
    phoneNumber: String,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        CraftoriaTextField(
            value = editedName,
            onValueChange = onNameChange,
            label = "Full Name",
            modifier = Modifier.padding(bottom = 14.dp)
        )

        CraftoriaTextField(
            value = email,
            onValueChange = {},
            label = "Email",
            enabled = false,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        CraftoriaTextField(
            value = phoneNumber,
            onValueChange = {},
            label = "Phone Number",
            enabled = false,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            CraftoriaButton(
                text = "Save Changes",
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
            CraftoriaButton(
                text = "Cancel",
                onClick = onCancel,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ViewModeContent(
    user: User,
    onNavigateTo: (String) -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    Column {
        // Account Information
        Card(
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Email", user.email)
                InfoRow("Phone", user.phone)
                InfoRow("Member Since", "Nov 2024")
            }
        }

        // Become a Seller (if buyer)
        if (user.role == UserRole.BUYER) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👩‍💼 Want to sell your products?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Join as a seller and reach thousands of customers!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    CraftoriaButton(
                        text = "Become a Seller",
                        onClick = { onNavigateTo("verification") }
                    )
                }
            }
        }

        // Seller Features (if seller)
        if (user.role == UserRole.SELLER) {
            MenuSection(
                title = "Seller Features",
                items = listOf(
                    MenuItem("🛍", "Manage Products", "manage_products"),
                    MenuItem("📦", "Seller Orders", "seller_orders"),
                    MenuItem("👥", "My Co-Seller Stores", "co_seller_stores")
                ),
                onItemClick = onNavigateTo
            )
        }

        // General
        MenuSection(
            title = "General",
            items = listOf(
                MenuItem("📦", "My Orders", "my_orders"),
                MenuItem("🔔", "Notification Preferences", "notifications"),
                MenuItem("❓", "Help & Support", "help"),
                MenuItem("📄", "Terms & Conditions", "terms"),
                MenuItem("🔐", "Privacy Policy", "privacy")
            ),
            onItemClick = onNavigateTo
        )

        // Security
        MenuSection(
            title = "Security",
            items = listOf(
                MenuItem("🔒", "Change Password", "change_password")
            ),
            onItemClick = {
                if (it == "change_password") onChangePassword()
                else onNavigateTo(it)
            }
        )

        // Danger Zone
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onDeleteAccount,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Error
            ),
            border = BorderStroke(2.dp, Error),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete Account",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        CraftoriaButton(
            text = "🚪 Logout",
            onClick = onLogout
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

data class MenuItem(
    val icon: String,
    val text: String,
    val route: String
)

@Composable
fun MenuSection(
    title: String,
    items: List<MenuItem>,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        items.forEach { item ->
            MenuItemRow(
                icon = item.icon,
                text = item.text,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}

@Composable
fun MenuItemRow(
    icon: String,
    text: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, BorderColor),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 19.sp
                )
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Password",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                CraftoriaTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = "Current Password",
                    isPassword = true,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                CraftoriaTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "New Password",
                    isPassword = true,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                CraftoriaTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm New Password",
                    isPassword = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword == confirmPassword && newPassword.isNotBlank()) {
                        onConfirm(currentPassword, newPassword)
                    }
                },
                enabled = currentPassword.isNotBlank() &&
                        newPassword.isNotBlank() &&
                        confirmPassword.isNotBlank() &&
                        newPassword == confirmPassword
            ) {
                Text(
                    text = "Update",
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Error,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = "Delete Account?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently removed.",
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Error,
                    contentColor = Color.White
                )
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}