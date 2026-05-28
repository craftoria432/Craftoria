package com.gcuf.craftoria.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.ui.text.style.TextOverflow
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.DeliveryInfo
import com.gcuf.craftoria.data.model.OrderTimeline
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.model.getRefundStatusEnum
import com.gcuf.craftoria.ui.components.OrderStatusBadge
import com.gcuf.craftoria.utils.formatDateTime
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.data.model.getStatusEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ── Order Details Dialog ──────────────────────────────────────────────────────

@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(Alignment.CenterVertically),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Gradient Header (Professional Compact) ────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Order Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    // Close button in tinted circle
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ── Scrollable Content ────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Order Information — tinted
                    DialogSectionCard(
                        icon = Icons.Default.ContentCopy,
                        title = "Order Information",
                        tinted = true
                    ) {
                        DetailRow("Order ID", "#${order.id.take(8).uppercase()}")
                        DetailRow("Order Date", formatDateTime(order.getCreatedAtLong()))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status", fontSize = 12.sp, color = TextSecondary)
                            // ✅ FIX: Check refund status first
                            // If order is refunded, show "Refunded" badge instead of order status
                            if (order.getRefundStatusEnum() == com.gcuf.craftoria.data.model.OrderRefundStatus.COMPLETED) {
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF9C27B0).copy(alpha = 0.10f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Refunded",
                                            tint = Color(0xFF9C27B0),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Refunded",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF9C27B0)
                                        )
                                    }
                                }
                            } else {
                                OrderStatusBadge(status = order.getStatusEnum())
                            }
                        }
                        DetailRow("Payment", order.paymentMethod)
                    }

                    // Products
                    DialogSectionCard(
                        icon = Icons.Default.ShoppingBag,
                        title = "Products"
                    ) {
                        if (order.items.isNotEmpty()) {
                            order.items.forEachIndexed { index, item ->
                                ProductListItem(
                                    thumbnail = item.productImage,
                                    name = item.productTitle,
                                    quantity = item.quantity,
                                    price = item.price
                                )
                                if (index < order.items.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } else {
                            // Legacy single product fallback
                            ProductListItem(
                                thumbnail = order.productImage,
                                name = order.productTitle,
                                quantity = order.quantity,
                                price = order.productPrice.takeIf { it > 0.0 }
                                    ?: if (order.quantity > 0) order.subtotal / order.quantity else order.totalPrice
                            )
                        }
                    }

                    // ✅ NEW: Store Information (for co-seller orders)
                    if (order.coSellerStoreId.isNotEmpty()) {
                        DialogSectionCard(
                            icon = Icons.Default.ShoppingBag,
                            title = "Store Information"
                        ) {
                            var coSellerStoreName by remember { mutableStateOf<String?>(null) }
                            var isLoadingStore by remember { mutableStateOf(true) }

                            LaunchedEffect(order.coSellerStoreId) {
                                try {
                                    val storeRepository = com.gcuf.craftoria.data.repository.CoSellerStoreRepository()
                                    val result = storeRepository.getStoreById(order.coSellerStoreId)
                                    if (result.isSuccess) {
                                        coSellerStoreName = result.getOrNull()?.storeName ?: "Co-seller Store"
                                    } else {
                                        coSellerStoreName = "Co-seller Store"
                                    }
                                } catch (e: Exception) {
                                    coSellerStoreName = "Co-seller Store"
                                } finally {
                                    isLoadingStore = false
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Primary.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = "Store",
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isLoadingStore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 1.5.dp,
                                            color = Primary
                                        )
                                    } else {
                                        Text(
                                            text = coSellerStoreName ?: "Co-seller Store",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Delivery Address
                    DialogSectionCard(
                        icon = Icons.Default.LocationOn,
                        title = "Delivery Address"
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundSecondary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatAddress(order.deliveryInfo),
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Order Timeline
                    if (order.timeline.isNotEmpty()) {
                        DialogSectionCard(
                            icon = Icons.Default.AccessTime,
                            title = "Order Timeline"
                        ) {
                            // ✅ FIX: Check refund status first
                            // If order is refunded, append a "Refunded" step to preserve full history
                            val displayTimeline = if (order.getRefundStatusEnum() == com.gcuf.craftoria.data.model.OrderRefundStatus.COMPLETED) {
                                // Append "Refunded" step to the end, preserving all previous steps
                                order.timeline + com.gcuf.craftoria.data.model.OrderTimeline(
                                    title = "Refunded",
                                    isCompleted = true,
                                    timestamp = System.currentTimeMillis()
                                )
                            } else {
                                order.timeline
                            }
                            OrderTimelineView(timeline = displayTimeline)
                        }
                    }

                    // Order Summary — tinted
                    DialogSectionCard(
                        icon = Icons.Default.Payment,
                        title = "Order Summary",
                        tinted = true
                    ) {
                        DetailRow("Subtotal", "PKR ${order.subtotal.toInt()}")
                        Spacer(modifier = Modifier.height(4.dp))
                        DetailRow(
                            label = "Delivery Fee",
                            value = if (order.shipping == 0.0) "Free Delivery"
                            else "PKR ${order.shipping.toInt()}",
                            valueColor = if (order.shipping == 0.0) Success else TextPrimary
                        )
                        if (order.discount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            DetailRow(
                                label = "Discount",
                                value = "-PKR ${order.discount.toInt()}",
                                valueColor = Success
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = Primary.copy(alpha = 0.15f),
                            thickness = 0.5.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "PKR ${order.totalPrice.toInt()}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }

                // ── Bottom Action Buttons ─────────────────────────────────────────
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Print button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch { printInvoice(context, order) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Print",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // Save button
                    Button(
                        onClick = {
                            coroutineScope.launch { saveInvoiceToGallery(context, order) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Save",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Cancel Order Dialog ───────────────────────────────────────────────────────

@Composable
fun CancelOrderDialog(
    orderId: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.93f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon in tinted circle — consistent with all dialogs
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF856404).copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Cancel Order?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to cancel this order? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Keep order — gradient (safe action first)
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp)
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
                                text = "No, Keep Order",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Confirm cancel — 0.5.dp Error border (destructive action second)
                    OutlinedButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Yes, Cancel Order",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Order Tracking Dialog ─────────────────────────────────────────────────────

@Composable
fun OrderTrackingDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hoveredItemIndex by remember { mutableStateOf(-1) }
    
    // Auto-scroll to first incomplete item on dialog open
    LaunchedEffect(Unit) {
        if (order.timeline.isNotEmpty()) {
            val firstIncompleteIndex = order.timeline.indexOfFirst { !it.isCompleted }
            if (firstIncompleteIndex >= 0) {
                kotlinx.coroutines.delay(300)
                scrollState.animateScrollTo(firstIncompleteIndex * 120)
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // ── Gradient Header (Professional Compact) ────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Track Order",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(scrollState)
                        .padding(14.dp)
                ) {
                    // Timeline with hover effects
                    DialogSectionCard(
                        icon = Icons.Default.LocalShipping,
                        title = "Delivery Status"
                    ) {
                        if (order.timeline.isNotEmpty()) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                order.timeline.forEachIndexed { index, item ->
                                    TimelineItemWithHover(
                                        title = item.title,
                                        time = if (item.isCompleted) {
                                            val ts = when (val timestamp = item.timestamp) {
                                                is Long -> timestamp
                                                is com.google.firebase.Timestamp -> timestamp.toDate().time
                                                else -> 0L
                                            }
                                            formatDateTime(ts)
                                        } else "Pending",
                                        isCompleted = item.isCompleted,
                                        isLast = index == order.timeline.lastIndex,
                                        isHovered = hoveredItemIndex == index,
                                        onHoverChange = { isHovered ->
                                            hoveredItemIndex = if (isHovered) index else -1
                                        }
                                    )
                                }
                            }
                        } else {
                            // Timeline not yet created — order not shipped yet
                            TimelineItemWithHover(
                                title = "Order Confirmed",
                                time = formatDateTime(order.getCreatedAtLong()),
                                isCompleted = true,
                                isLast = true,
                                isHovered = hoveredItemIndex == 0,
                                onHoverChange = { isHovered -> hoveredItemIndex = if (isHovered) 0 else -1 }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF3CD),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Color(0xFF856404),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Tracking details will appear once the seller ships your order",
                                        fontSize = 12.sp,
                                        color = Color(0xFF856404),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Estimated Delivery
                    val estimatedDelivery = order.estimatedDelivery
                    if (estimatedDelivery != null) {
                        val deliveryTime = when (estimatedDelivery) {
                            is Long -> estimatedDelivery
                            is com.google.firebase.Timestamp -> estimatedDelivery.toDate().time
                            else -> 0L
                        }
                        if (deliveryTime > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DialogSectionCard(
                                icon = Icons.Default.Schedule,
                                title = "Estimated Delivery"
                            ) {
                                DetailRow("Expected by", formatDateTime(deliveryTime))
                            }
                        }
                    }

                    // Courier Information
                    if (order.trackingId.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DialogSectionCard(
                            icon = Icons.Default.LocalShipping,
                            title = "Courier Information"
                        ) {
                            DetailRow("Courier", order.courierName.ifEmpty { "TCS Express" })
                            Spacer(modifier = Modifier.height(4.dp))
                            DetailRow(
                                label = "Tracking ID",
                                value = order.trackingId,
                                valueColor = Primary
                            )
                            if (order.courierContact.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                DetailRow("Contact", order.courierContact)
                            }
                        }
                    }

                    // Close — gradient button
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp)
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
                                text = "Close",
                                fontSize = 14.sp,
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

// ── Shared Section Card ───────────────────────────────────────────────────────

@Composable
private fun DialogSectionCard(
    icon: ImageVector,
    title: String,
    tinted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = if (tinted)
            androidx.compose.foundation.BorderStroke(0.5.dp, Primary.copy(alpha = 0.15f))
        else
            androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section header with icon in tinted box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (tinted) Primary.copy(alpha = 0.03f) else Color.White)
                .padding(horizontal = 14.dp, vertical = 11.dp),
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (tinted) Color(0xFFFFF8F9) else Color.White)
                .padding(14.dp),
            content = content
        )
    }
}

// ── Shared Components ─────────────────────────────────────────────────────────

@Composable
fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 0.4.sp,
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
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundSecondary),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail.isNotEmpty()) {
                AsyncImage(
                    model = CloudinaryManager.getOptimizedUrl(url = thumbnail, width = 150, quality = 75),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
            Text(
                text = "Qty: $quantity × PKR ${price.toInt()}",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Text(
            text = "PKR ${(quantity * price).toInt()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}

@Composable
fun OrderTimelineView(timeline: List<OrderTimeline>) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        timeline.forEachIndexed { index, item ->
            TimelineItem(
                title = item.title,
                time = if (item.isCompleted) {
                    val ts = when (val timestamp = item.timestamp) {
                        is Long -> timestamp
                        is com.google.firebase.Timestamp -> timestamp.toDate().time
                        else -> 0L
                    }
                    formatDateTime(ts)
                } else "Pending",
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
            .padding(bottom = if (isLast) 0.dp else 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isCompleted) Success else BackgroundSecondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isCompleted) Color.White else TextLight,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(if (isCompleted) Success.copy(alpha = 0.4f) else BorderColor)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) TextPrimary else TextLight
            )
            Text(
                text = time,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ── Timeline Item with Hover Effect ───────────────────────────────────────────

@Composable
fun TimelineItemWithHover(
    title: String,
    time: String,
    isCompleted: Boolean,
    isLast: Boolean,
    isHovered: Boolean = false,
    onHoverChange: (Boolean) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHoveredState by interactionSource.collectIsHoveredAsState()
    
    LaunchedEffect(isHoveredState) {
        onHoverChange(isHoveredState)
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered || isHoveredState) {
            Primary.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered || isHoveredState) 1.02f else 1f,
        animationSpec = tween(200)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 4.dp)
            .hoverable(interactionSource)
            .scale(scale)
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isCompleted) Success else BackgroundSecondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isCompleted) Color.White else TextLight,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(if (isCompleted) Success.copy(alpha = 0.4f) else BorderColor)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) TextPrimary else TextLight
            )
            Text(
                text = time,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

fun formatAddress(deliveryInfo: DeliveryInfo): String {
    return buildString {
        if (deliveryInfo.fullName.isNotEmpty()) appendLine(deliveryInfo.fullName)
        if (deliveryInfo.address.isNotEmpty()) appendLine(deliveryInfo.address)
        val cityLine = listOfNotNull(
            deliveryInfo.city.takeIf { it.isNotEmpty() },
            deliveryInfo.postalCode.takeIf { it.isNotEmpty() }
        ).joinToString(", ")
        if (cityLine.isNotEmpty()) appendLine(cityLine)
        append("Pakistan")
    }.trim()
}

// ── Invoice / Print / Save — logic unchanged ──────────────────────────────────

suspend fun printInvoice(context: Context, order: Order) {
    withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            var yPos = 50f

            paint.textSize = 24f; paint.isFakeBoldText = true
            paint.color = android.graphics.Color.parseColor("#E91E8C")
            canvas.drawText("INVOICE", 50f, yPos, paint); yPos += 40f

            paint.textSize = 14f; paint.isFakeBoldText = false
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("Order ID: #${order.id.take(8).uppercase()}", 50f, yPos, paint); yPos += 25f
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            canvas.drawText("Date: ${dateFormat.format(Date(order.getCreatedAtLong()))}", 50f, yPos, paint); yPos += 25f
            canvas.drawText("Status: ${order.status}", 50f, yPos, paint); yPos += 40f

            paint.isFakeBoldText = true
            canvas.drawText("Delivery Address:", 50f, yPos, paint); yPos += 25f
            paint.isFakeBoldText = false
            canvas.drawText(order.deliveryInfo.address, 50f, yPos, paint); yPos += 20f
            canvas.drawText("${order.deliveryInfo.city}, Pakistan", 50f, yPos, paint); yPos += 40f

            paint.isFakeBoldText = true
            canvas.drawText("Products:", 50f, yPos, paint); yPos += 30f
            paint.isFakeBoldText = false
            order.items.forEach { item ->
                canvas.drawText(item.productTitle, 50f, yPos, paint); yPos += 20f
                canvas.drawText("Qty: ${item.quantity} x PKR ${item.price.toInt()}", 70f, yPos, paint); yPos += 25f
            }
            yPos += 20f

            paint.isFakeBoldText = true
            canvas.drawText("Order Summary:", 50f, yPos, paint); yPos += 30f
            paint.isFakeBoldText = false
            canvas.drawText("Subtotal: PKR ${order.subtotal.toInt()}", 50f, yPos, paint); yPos += 25f
            canvas.drawText("Delivery Fee: PKR ${order.shipping.toInt()}", 50f, yPos, paint); yPos += 25f
            if (order.discount > 0) {
                canvas.drawText("Discount: -PKR ${order.discount.toInt()}", 50f, yPos, paint); yPos += 25f
            }
            yPos += 10f
            paint.isFakeBoldText = true; paint.textSize = 16f
            paint.color = android.graphics.Color.parseColor("#E91E8C")
            canvas.drawText("Total: PKR ${order.totalPrice.toInt()}", 50f, yPos, paint)

            pdfDocument.finishPage(page)
            val fileName = "Invoice_${order.id.take(8)}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            withContext(Dispatchers.Main) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
                val printAdapter = object : android.print.PrintDocumentAdapter() {
                    override fun onLayout(o: android.print.PrintAttributes?, n: android.print.PrintAttributes?, c: android.os.CancellationSignal?, cb: LayoutResultCallback?, e: android.os.Bundle?) {
                        if (c?.isCanceled == true) { cb?.onLayoutCancelled(); return }
                        cb?.onLayoutFinished(android.print.PrintDocumentInfo.Builder("Invoice_${order.id.take(8)}.pdf").setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(), true)
                    }
                    override fun onWrite(p: Array<out android.print.PageRange>?, d: android.os.ParcelFileDescriptor?, c: android.os.CancellationSignal?, cb: WriteResultCallback?) {
                        try {
                            if (c?.isCanceled == true) { cb?.onWriteCancelled(); return }
                            val i = java.io.FileInputStream(file); val o2 = java.io.FileOutputStream(d?.fileDescriptor)
                            val buf = ByteArray(1024); var n2: Int
                            while (i.read(buf).also { n2 = it } != -1) o2.write(buf, 0, n2)
                            i.close(); o2.close()
                            cb?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) { cb?.onWriteFailed(e.message) }
                    }
                }
                printManager.print("Invoice_${order.id.take(8)}", printAdapter, null)
                Toast.makeText(context, "Opening print dialog...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to print invoice: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

suspend fun saveInvoiceToGallery(context: Context, order: Order) {
    withContext(Dispatchers.IO) {
        try {
            val width = 595; val height = 842
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val paint = Paint()
            var yPos = 50f

            paint.textSize = 24f; paint.isFakeBoldText = true
            paint.color = android.graphics.Color.parseColor("#E91E8C")
            canvas.drawText("INVOICE", 50f, yPos, paint); yPos += 40f

            paint.textSize = 14f; paint.isFakeBoldText = false
            paint.color = android.graphics.Color.BLACK
            canvas.drawText("Order ID: #${order.id.take(8).uppercase()}", 50f, yPos, paint); yPos += 25f
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            canvas.drawText("Date: ${dateFormat.format(Date(order.getCreatedAtLong()))}", 50f, yPos, paint); yPos += 25f
            canvas.drawText("Status: ${order.status}", 50f, yPos, paint); yPos += 40f

            paint.isFakeBoldText = true
            canvas.drawText("Delivery Address:", 50f, yPos, paint); yPos += 25f
            paint.isFakeBoldText = false
            canvas.drawText(order.deliveryInfo.address, 50f, yPos, paint); yPos += 20f
            canvas.drawText("${order.deliveryInfo.city}, Pakistan", 50f, yPos, paint); yPos += 40f

            paint.isFakeBoldText = true
            canvas.drawText("Products:", 50f, yPos, paint); yPos += 30f
            paint.isFakeBoldText = false
            order.items.forEach { item ->
                canvas.drawText(item.productTitle, 50f, yPos, paint); yPos += 20f
                canvas.drawText("Qty: ${item.quantity} x PKR ${item.price.toInt()}", 70f, yPos, paint); yPos += 25f
            }
            yPos += 20f

            paint.isFakeBoldText = true
            canvas.drawText("Order Summary:", 50f, yPos, paint); yPos += 30f
            paint.isFakeBoldText = false
            canvas.drawText("Subtotal: PKR ${order.subtotal.toInt()}", 50f, yPos, paint); yPos += 25f
            canvas.drawText("Delivery Fee: PKR ${order.shipping.toInt()}", 50f, yPos, paint); yPos += 25f
            if (order.discount > 0) {
                canvas.drawText("Discount: -PKR ${order.discount.toInt()}", 50f, yPos, paint); yPos += 25f
            }
            yPos += 10f
            paint.isFakeBoldText = true; paint.textSize = 16f
            paint.color = android.graphics.Color.parseColor("#E91E8C")
            canvas.drawText("Total: PKR ${order.totalPrice.toInt()}", 50f, yPos, paint)

            val fileName = "Invoice_${order.id.take(8)}_${System.currentTimeMillis()}.png"
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Craftoria")
            }
            context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Invoice saved to Gallery/Craftoria", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save invoice: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
