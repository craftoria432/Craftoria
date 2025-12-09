package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.ui.screens.buyer.OrderStatusBadge
import com.gcuf.craftoria.ui.screens.buyer.formatDateTime
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager

@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Primary, PrimaryLight)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Order #${order.id.take(8).uppercase()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    // Order Information
                    DetailSection(title = "Order Information") {
                        DetailRow(
                            label = "Order ID:",
                            value = "#${order.id.take(8).uppercase()}"
                        )
                        DetailRow(
                            label = "Order Date:",
                            value = formatDateTime(order.createdAt)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status:",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            OrderStatusBadge(status = order.status)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Products
                    DetailSection(title = "Products") {
                        order.items.forEach { item ->
                            ProductListItem(
                                thumbnail = item.productImage,
                                name = item.productTitle,
                                quantity = item.quantity,
                                price = item.price
                            )
                            Spacer(modifier = Modifier.height(9.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delivery Address
                    DetailSection(title = "Delivery Address") {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = BackgroundLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatAddress(order.deliveryInfo),
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Payment Method
                    DetailSection(title = "Payment Method") {
                        DetailRow(
                            label = "Method:",
                            value = order.paymentMethod
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Order Timeline
                    if (order.timeline.isNotEmpty()) {
                        DetailSection(title = "Order Timeline") {
                            OrderTimeline(timeline = order.timeline)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Order Summary
                    DetailSection(title = "Order Summary") {
                        DetailRow(
                            label = "Subtotal:",
                            value = "PKR ${order.subtotal.toInt()}"
                        )
                        DetailRow(
                            label = "Delivery Fee:",
                            value = if (order.shipping == 0.0) "Free Delivery" else "PKR ${order.shipping.toInt()}",
                            valueColor = if (order.shipping == 0.0) Success else TextPrimary
                        )
                        if (order.discount > 0) {
                            DetailRow(
                                label = "Discount:",
                                value = "-PKR ${order.discount.toInt()}",
                                valueColor = Success
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = BorderColor,
                            thickness = 2.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "PKR ${order.totalPrice.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        content()
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun ProductListItem(
    thumbnail: String,
    name: String,
    quantity: Int,
    price: Double
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = BackgroundLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Thumbnail
            Surface(
                modifier = Modifier.size(50.dp),
                shape = MaterialTheme.shapes.small,
                color = BackgroundSecondary
            ) {
                if (thumbnail.isNotEmpty()) {
                    AsyncImage(
                        model = CloudinaryManager.getOptimizedUrl(
                            url = thumbnail,
                            width = 150,
                            quality = 75
                        ),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = getProductEmoji(""), fontSize = 24.sp)
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2
                )
                Text(
                    text = "Qty: $quantity × PKR ${price.toInt()}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // Price
            Text(
                text = "PKR ${(quantity * price).toInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        }
    }
}

@Composable
fun OrderTimeline(timeline: List<com.gcuf.craftoria.data.model.OrderTimeline>) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        timeline.forEachIndexed { index, item ->
            TimelineItem(
                title = item.title,
                time = if (item.isCompleted) formatDateTime(item.timestamp) else "Pending",
                isCompleted = item.isCompleted,
                isLast = index == timeline.lastIndex
            )
        }
    }
}

@Composable
fun TimelineItem(
    title: String,
    time: String,
    isCompleted: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isCompleted) Success else BackgroundSecondary,
                border = if (!isCompleted) androidx.compose.foundation.BorderStroke(2.dp, BorderColor) else null
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCompleted) "✓" else "⏳",
                        fontSize = 14.sp,
                        color = if (isCompleted) Color.White else TextSecondary
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(if (isCompleted) Success else BorderColor)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = time,
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun formatAddress(deliveryInfo: com.gcuf.craftoria.data.model.DeliveryInfo): String {
    return buildString {
        append(deliveryInfo.address)
        append("\n")
        append(deliveryInfo.city)
        if (deliveryInfo.postalCode.isNotEmpty()) {
            append(", ")
            append(deliveryInfo.postalCode)
        }
        append("\nPakistan")
    }
}