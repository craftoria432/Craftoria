package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.*

@Composable
fun OrderSuccessScreen(
    orderIds: String,
    onTrackOrder: () -> Unit,
    onContinueShopping: () -> Unit,
    orderStatus: String = "new"
) {
    val orderIdList = orderIds.split(",").filter { it.isNotEmpty() }
    val firstOrderId = orderIdList.firstOrNull() ?: ""
    val orderCount = orderIdList.size

    val isEmailCompleted = true
    val isSellerNotifiedCompleted = orderStatus in listOf("confirmed", "processing", "shipped", "delivered", "completed")
    val isOutForDeliveryCompleted = orderStatus in listOf("shipped", "delivered", "completed")
    val isDeliveredCompleted = orderStatus in listOf("delivered", "completed")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Hero gradient band ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Primary, PrimaryLight, Primary.copy(alpha = 0.15f)),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(
                                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Order Placed!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Thank you for shopping with Craftoria",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Order ID card — overlaps gradient ─────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-28).dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ORDER ID${if (orderCount > 1) "S" else ""}",
                            fontSize = 10.sp,
                            color = TextLight,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        // Confirmed badge with green dot
                        Surface(
                            color = Success.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Success, CircleShape)
                                )
                                Text(
                                    text = "Confirmed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Success
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    orderIdList.forEach { orderId ->
                        Text(
                            text = "#${orderId.take(8).uppercase()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // ── What happens next card ────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-14).dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Section header — matches AddProductSectionCard / DialogSectionCard pattern
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Primary.copy(alpha = 0.02f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "What happens next?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Primary.copy(alpha = 0.02f))
                            .padding(14.dp)
                    ) {
                        OrderTimelineItem(
                            icon = Icons.Default.Email,
                            title = "Order confirmation sent",
                            subtitle = "Check your email for full details",
                            isLast = false,
                            isCompleted = isEmailCompleted
                        )
                        OrderTimelineItem(
                            icon = Icons.Default.Person,
                            title = "Seller notified",
                            subtitle = "Seller is preparing your order",
                            isLast = false,
                            isCompleted = isSellerNotifiedCompleted
                        )
                        OrderTimelineItem(
                            icon = Icons.Default.LocalShipping,
                            title = "Wait for delivery",
                            subtitle = "Track live in \"My Orders\"",
                            isLast = true,
                            isCompleted = isOutForDeliveryCompleted
                        )
                    }
                }
            }

            // ── Action Buttons ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .offset(y = (-8).dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Track My Order — gradient fill with pink hover effect
                TrackOrderButtonLarge(
                    onClick = onTrackOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                // Continue Shopping — 0.5.dp Primary border (was 1.5.dp)
                OutlinedButton(
                    onClick = onContinueShopping,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue Shopping",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Timeline Item ─────────────────────────────────────────────────────────────

@Composable
private fun OrderTimelineItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLast: Boolean,
    isCompleted: Boolean = false,
    iconTint: Color = Primary,
    iconBg: Color = Primary.copy(alpha = 0.10f)
) {
    val displayIconTint = if (isCompleted) Success else Primary
    val displayIconBg = if (isCompleted) Success.copy(alpha = 0.10f) else Primary.copy(alpha = 0.10f)
    val displayTitleColor = if (isCompleted) Success else TextPrimary
    val connectorColor = if (isCompleted) Success.copy(alpha = 0.30f) else Primary.copy(alpha = 0.15f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(displayIconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = displayIconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(20.dp)
                        .background(connectorColor)
                )
            }
        }

        Column(
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = if (isLast) 0.dp else 8.dp
            )
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = displayTitleColor
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Legacy InfoItem ───────────────────────────────────────────────────────────

@Composable
fun InfoItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = Primary.copy(alpha = 0.08f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun TrackOrderButtonLarge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Pink hover colors
    val hoverPink = Color(0xFFFFE4E1)  // Light pink for hover background
    val hoverPinkBorder = Color(0xFFFFB6C1)  // Pink border for hover
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .hoverable(interactionSource = interactionSource),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isHovered) {
                        Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                    } else {
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                    },
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = if (isHovered) Primary else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Track My Order",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHovered) Primary else Color.White,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}