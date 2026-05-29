package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.ui.components.formatPrice
import com.gcuf.craftoria.ui.screens.seller.ApprovalBadge
import com.gcuf.craftoria.ui.screens.seller.StockBadge
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductCard(
    product: Product,
    onToggleStatus: () -> Unit,
    onStockIncrement: () -> Unit,
    onStockDecrement: () -> Unit,
    onEdit: () -> Unit,
    onViewAsBuyer: () -> Unit,
    onViewStats: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Image Section ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(BackgroundSecondary)
            ) {
                if (product.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = CloudinaryManager.getOptimizedUrl(
                            product.imageUrls.first(), 400, 160
                        ),
                        contentDescription = product.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = TextLight,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Three-dot menu — white circle with shadow
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Product", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("View as Buyer", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showMenu = false; onViewAsBuyer() }
                        )
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = { Text("Delete", fontSize = 13.sp, color = Error) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Error,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            // ── Content Section ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title — 2 lines fixed height
                Text(
                    text = product.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp, max = 32.dp)
                )

                // Price — PKR label + bold amount
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "PKR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary.copy(alpha = 0.65f)
                    )
                    Text(
                        text = formatPrice(product.price),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        letterSpacing = (-0.3).sp
                    )
                }

                // Badges — Two rows layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: Stock + Status (always visible)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StockBadge(stock = product.stock)
                        ProductActiveBadge(isActive = product.isActive)
                    }

                    // Row 2: Additional badges (Pending/Rejected + Co-Seller) — only if applicable
                    val hasPendingOrRejected = product.approvalStatus == "pending" || product.approvalStatus == "rejected"
                    val hasCoSeller = product.coSellerStoreId.isNotEmpty()

                    if (hasPendingOrRejected || hasCoSeller) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Approval status badge — pending/rejected only (pill-rounded)
                            if (product.approvalStatus == "pending") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFF3CD)
                                ) {
                                    Text(
                                        text = "Pending",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF856404),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (product.approvalStatus == "rejected") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF8D7DA)
                                ) {
                                    Text(
                                        text = "Rejected",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF721C24),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Co-seller badge — info blue tinted (pill-rounded)
                            if (product.coSellerStoreId.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFD1ECF1)
                                ) {
                                    Text(
                                        text = "Co-Seller",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0C5460),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 0.5.dp divider — consistent with all cards
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                // ── Toggle + Stock Counter ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active/Inactive toggle
                    Switch(
                        checked = product.isActive,
                        onCheckedChange = { onToggleStatus() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Success,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = BorderColor
                        ),
                        modifier = Modifier.height(22.dp)
                    )

                    // Stock stepper — 0.5.dp BorderColor circles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onStockDecrement,
                            modifier = Modifier.size(22.dp),
                            shape = CircleShape,
                            color = BackgroundSecondary,
                            border = BorderStroke(0.5.dp, BorderColor)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Text(
                            text = product.stock.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.widthIn(min = 18.dp),
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            onClick = onStockIncrement,
                            modifier = Modifier.size(22.dp),
                            shape = CircleShape,
                            color = BackgroundSecondary,
                            border = BorderStroke(0.5.dp, BorderColor)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.scale(scale: Float): Modifier {
    return this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
                placeable.place(0, 0)
            }
        }
    )
}