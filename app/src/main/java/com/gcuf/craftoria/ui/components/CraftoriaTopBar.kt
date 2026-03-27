package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.utils.CartBadge
import com.gcuf.craftoria.utils.NotificationBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftoriaTopBar(
    screenNumber: String? = null,
    title: String? = null,
    subtitle: String? = null,
    showLogo: Boolean = false,
    showBack: Boolean = false,
    showEdit: Boolean = false,
    showCart: Boolean = false,
    showNotifications: Boolean = false,
    cartCount: Int = 0,
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            if (showLogo) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🧵 Craftoria",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Empowering Women through Handicrafts",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            } else if (title != null) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        lineHeight = 16.sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else if (screenNumber != null) {
                Surface(
                    modifier = Modifier
                        .padding(start = 18.dp)
                        .size(28.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = screenNumber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        actions = {
            if (showNotifications) {
                Box(
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Professional notification badge with pulsing animation
                    NotificationBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
            
            if (showCart) {
                Box(
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    IconButton(onClick = onCartClick) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Professional cart badge with pulsing animation
                    CartBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
            
            if (showEdit) {
                TextButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (!showBack && !showCart && screenNumber != null) {
                Spacer(modifier = Modifier.width(28.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Primary, PrimaryLight)
                )
            )
            .height(64.dp)
    )
}