package com.gcuf.craftoria.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.AuthState
import com.gcuf.craftoria.viewmodel.AuthViewModel

/**
 * Role Selection Screen for first-time Google sign-in users
 * Allows users to choose between Buyer and Seller roles
 */
@Composable
fun RoleSelectionScreen(
    userId: String,
    userName: String,
    onRoleSelected: (UserRole) -> Unit,
    onBackClick: () -> Unit,
    viewModel: AuthViewModel? = null
) {
    val vm = viewModel ?: viewModel<AuthViewModel>()
    val authState by vm.authState.collectAsState()
    
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var pendingRole by remember { mutableStateOf<UserRole?>(null) }

    // Handle role selection submission - show confirmation first
    fun showRoleConfirmation(role: UserRole) {
        pendingRole = role
        showConfirmationDialog = true
    }

    // Confirm and submit role
    fun confirmAndSubmitRole(role: UserRole) {
        selectedRole = role  // Store the intended role for navigation
        isLoading = true
        errorMessage = null
        showConfirmationDialog = false
        vm.setInitialRole(userId, role)
    }

    // Monitor auth state for role setting result
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                isLoading = false
                // Pass the intended role for navigation
                selectedRole?.let { intendedRole ->
                    onRoleSelected(intendedRole)
                }
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = (authState as AuthState.Error).message
            }
            is AuthState.Loading -> {
                isLoading = true
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSecondary)
    ) {
        // ── Gradient header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Primary, PrimaryLight)))
                .padding(top = 40.dp, bottom = 36.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Craftoria Logo - Same as Splash Screen
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Craftoria Logo",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to Craftoria",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hi, $userName!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.90f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Pill subtitle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "Select your role to get started",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.90f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // ── Body ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Error banner
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF5F5),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFE53935).copy(alpha = 0.60f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFE53935).copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFC62828),
                            modifier = Modifier.weight(1f),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Step label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
                Text(
                    text = "Choose how you'd like to use Craftoria",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            // Buyer Card
            RoleCard(
                title = "Buyer",
                description = "Discover and purchase unique handmade products from talented artisans worldwide",
                icon = Icons.Default.ShoppingCart,
                backgroundColor = Color(0xFFF3E5F5),
                accentColor = Color(0xFF7B1FA2),
                isSelected = selectedRole == UserRole.BUYER,
                isLoading = isLoading && selectedRole == UserRole.BUYER,
                onClick = { showRoleConfirmation(UserRole.BUYER) }
            )

            // Seller Card
            RoleCard(
                title = "Seller",
                description = "Showcase your handmade creations and build your business with our platform",
                icon = Icons.Default.Palette,
                backgroundColor = Color(0xFFE8F5E9),
                accentColor = Color(0xFF2E7D32),
                isSelected = selectedRole == UserRole.SELLER,
                isLoading = isLoading && selectedRole == UserRole.SELLER,
                onClick = { showRoleConfirmation(UserRole.SELLER) }
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // Role Confirmation Dialog
    if (showConfirmationDialog && pendingRole != null) {
        RoleConfirmationDialog(
            role = pendingRole!!,
            userName = userName,
            onConfirm = { confirmAndSubmitRole(pendingRole!!) },
            onCancel = { showConfirmationDialog = false },
            isLoading = isLoading
        )
    }
}

/**
 * Individual role selection card with professional styling
 */
@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    accentColor: Color,
    isSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) accentColor else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(16.dp)
            ),
        color = if (isSelected) backgroundColor else Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isSelected) 6.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon box — tinted square
            Surface(
                modifier = Modifier.size(56.dp),
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Text block
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 0.2.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp,
                    maxLines = 3
                )
            }

            // Trailing: selection indicator or chevron
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                    isSelected -> Surface(
                        modifier = Modifier.size(28.dp),
                        color = accentColor,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp)
                        )
                    }
                    else -> Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Professional confirmation dialog for role selection
 */
@Composable
private fun RoleConfirmationDialog(
    role: UserRole,
    userName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    val (title, description, icon) = when (role) {
        UserRole.BUYER -> Triple(
            "Browse & Purchase",
            "Discover handmade products from talented artisans and add them to your collection.",
            Icons.Default.ShoppingCart
        )
        UserRole.SELLER -> Triple(
            "Showcase & Sell",
            "Display your handmade creations and connect with buyers worldwide.",
            Icons.Default.Palette
        )
        else -> Triple("Get Started", "Continue with your selection", Icons.Default.AutoAwesome)
    }

    val accentColor = when (role) {
        UserRole.BUYER -> Color(0xFF7B1FA2)
        UserRole.SELLER -> Color(0xFF2E7D32)
        else -> Primary
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onCancel() },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Creating Account…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                } else {
                    Text(
                        "Confirm",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text(
                    "Back",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        },
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Material icon in tinted circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Professional description
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(0.90f)
    )
}
