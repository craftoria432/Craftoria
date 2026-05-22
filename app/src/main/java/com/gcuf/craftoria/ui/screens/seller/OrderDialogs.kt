package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.gcuf.craftoria.utils.InvoiceUtils
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.model.getOrderPlacedAtLong
import com.gcuf.craftoria.data.model.getProcessingAtLong
import com.gcuf.craftoria.data.model.getShippedAtLong
import com.gcuf.craftoria.data.model.getDeliveredAtLong
import com.gcuf.craftoria.ui.components.OrderStatusBadge
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.utils.formatDateTime
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager

// ── Order Details Dialog (Seller) ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
                .fillMaxWidth(0.93f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Gradient Header (Professional Compact) ────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Order Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterStart)
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

                // ── Content ───────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Order Info — tinted
                    SellerDialogSectionCard(icon = Icons.Default.AccessTime, title = "Order Information", tinted = true) {
                        SellerDetailRow("Order ID", "#${order.id.take(8).uppercase()}")
                        SellerDetailRow("Order Date", formatDateTime(order.getCreatedAtLong()))
                        // ✅ Convert status string to OrderStatus enum safely (outside composable)
                        val orderStatus = try {
                            OrderStatus.valueOf(order.status.uppercase())
                        } catch (e: Exception) {
                            OrderStatus.PENDING // Fallback if status is not valid
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Status", fontSize = 12.sp, color = TextSecondary)
                            OrderStatusBadge(status = orderStatus)
                        }
                        SellerDetailRow("Payment", order.paymentMethod.ifEmpty { "Cash on Delivery" })
                    }

                    // Buyer Information
                    SellerDialogSectionCard(icon = Icons.Default.Person, title = "Buyer Information") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundSecondary, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = order.buyerName.take(2).uppercase(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                RealtimeNameDisplay(
                                    userId = order.buyerId,
                                    fallbackName = order.buyerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(text = "📞 ${order.buyerPhone}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Product Details
                    SellerDialogSectionCard(icon = Icons.Default.Inventory, title = "Product Details") {
                        if (order.items.isNotEmpty()) {
                            order.items.forEachIndexed { index, item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(BackgroundSecondary), contentAlignment = Alignment.Center) {
                                        if (item.productImage.isNotEmpty()) {
                                            AsyncImage(model = CloudinaryManager.getOptimizedUrl(item.productImage, 100, 80), contentDescription = item.productTitle, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(imageVector = Icons.Default.Inventory, contentDescription = null, tint = TextLight, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.productTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(text = "Qty: ${item.quantity}", fontSize = 11.sp, color = TextSecondary)
                                            Text(text = "PKR ${String.format(Locale.getDefault(), "%,.0f", item.price)}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                    Text(text = "PKR ${String.format(Locale.getDefault(), "%,.0f", item.quantity * item.price)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                                }
                                if (index < order.items.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(BackgroundSecondary), contentAlignment = Alignment.Center) {
                                    if (order.productImage.isNotEmpty()) {
                                        AsyncImage(model = CloudinaryManager.getOptimizedUrl(order.productImage, 100, 80), contentDescription = order.productTitle, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(imageVector = Icons.Default.Inventory, contentDescription = null, tint = TextLight, modifier = Modifier.size(26.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = order.productTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
                                    val actualPrice = if (order.productPrice > 0.0) order.productPrice
                                    else if (order.subtotal > 0.0 && order.quantity > 0) order.subtotal / order.quantity
                                    else { val p = order.totalPrice - order.shipping; if (order.quantity > 0) p / order.quantity else 0.0 }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(text = "Qty: ${order.quantity}", fontSize = 11.sp, color = TextSecondary)
                                        Text(text = "PKR ${String.format(Locale.getDefault(), "%,.0f", actualPrice)}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Delivery Address
                    SellerDialogSectionCard(icon = Icons.Default.LocationOn, title = "Delivery Address") {
                        Surface(shape = RoundedCornerShape(8.dp), color = BackgroundSecondary, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "📍 ${order.fullAddress.ifEmpty { order.shippingAddress }}",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Order Summary — tinted
                    SellerDialogSectionCard(icon = Icons.Default.Payment, title = "Order Summary", tinted = true) {
                        SellerDetailRow("Subtotal", "PKR ${order.subtotal.toInt()}")
                        Spacer(modifier = Modifier.height(4.dp))
                        SellerDetailRow(
                            label = "Delivery Fee",
                            value = if (order.shipping == 0.0) "Free Delivery" else "PKR ${order.shipping.toInt()}",
                            valueColor = if (order.shipping == 0.0) Success else TextPrimary
                        )
                        if (order.discount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SellerDetailRow("Discount", "-PKR ${order.discount.toInt()}", valueColor = Success)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Primary.copy(alpha = 0.15f), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Total", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = "PKR ${String.format(Locale.getDefault(), "%,.0f", order.totalPrice)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }

                    // Timeline
                    SellerDialogSectionCard(icon = Icons.Default.AccessTime, title = "Order Timeline") {
                        OrderTimeline(order = order)
                    }

                    // Share Invoice — 0.5.dp border matching system
                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = { InvoiceUtils.shareInvoice(context, order) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Invoice", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Shared section card ───────────────────────────────────────────────────────

@Composable
private fun SellerDialogSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            }
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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

@Composable
fun SellerDetailRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

// ── Order Timeline ────────────────────────────────────────────────────────────

@Composable
fun OrderTimeline(order: Order) {
    val timeline = listOf(
        Triple("Order Placed", if (order.getOrderPlacedAtLong() > 0) formatDateTime(order.getOrderPlacedAtLong()) else "Pending", order.getOrderPlacedAtLong() > 0),
        Triple("Processing", if (order.getProcessingAtLong() > 0) formatDateTime(order.getProcessingAtLong()) else "Pending", order.getProcessingAtLong() > 0),
        Triple("Shipped", if (order.getShippedAtLong() > 0) formatDateTime(order.getShippedAtLong()) else "Pending", order.getShippedAtLong() > 0),
        Triple("Delivered", if (order.getDeliveredAtLong() > 0) formatDateTime(order.getDeliveredAtLong()) else "Pending", order.getDeliveredAtLong() > 0)
    )

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        timeline.forEachIndexed { index, (title, date, completed) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (completed) Success else BackgroundSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (completed) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = TextLight, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (index < timeline.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(if (completed) Success.copy(alpha = 0.4f) else BorderColor)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(bottom = if (index < timeline.size - 1) 4.dp else 0.dp)) {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (completed) TextPrimary else TextLight)
                    Text(text = date, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 1.dp))
                }
            }
        }
    }
}

// ── Accept Order Dialog ───────────────────────────────────────────────────────

@Composable
fun AcceptOrderDialog(order: Order, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Success.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(30.dp))
            }
        },
        title = { Text(text = "Accept this order?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
        text = {
            Text(
                text = "The buyer will be notified and the order will move to Processing.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) { Text("Accept Order", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) { Text("Cancel", color = TextSecondary, fontSize = 13.sp) }
        }
    )
}

// ── Reject Order Dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RejectOrderDialog(order: Order, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var selectedReason by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val reasons = listOf(
        "" to "Select a reason",
        "out_of_stock" to "Out of Stock",
        "price_error" to "Price Error",
        "cannot_deliver" to "Cannot Deliver",
        "other" to "Other"
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text = "Reject Order", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.align(Alignment.CenterStart))
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd).size(32.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.15f))) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(text = "Reason for rejection", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = reasons.find { it.first == selectedReason }?.second ?: "Select a reason",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                reasons.forEach { (value, label) ->
                                    if (value.isNotEmpty()) {
                                        DropdownMenuItem(text = { Text(label, fontSize = 13.sp) }, onClick = { selectedReason = value; expanded = false })
                                    }
                                }
                            }
                        }
                    }
                    Column {
                        Text(text = "Additional details (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = details, onValueChange = { details = it },
                            placeholder = { Text("Provide more information...", fontSize = 13.sp) },
                            minLines = 3, maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = { if (selectedReason.isNotEmpty()) onConfirm(selectedReason, details) },
                        enabled = selectedReason.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Error),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Confirm Rejection", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ── Mark Shipped Dialog ───────────────────────────────────────────────────────

@Composable
fun MarkShippedDialog(onConfirm: (String, String, Long) -> Unit, onDismiss: () -> Unit) {
    var courierName by remember { mutableStateOf("") }
    var trackingNumber by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text = "Add Shipping Details", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.align(Alignment.CenterStart))
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd).size(32.dp), colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.15f))) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(text = "Courier Name (Optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(value = courierName, onValueChange = { courierName = it }, placeholder = { Text("e.g., TCS, Leopards", fontSize = 13.sp) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                    }
                    Column {
                        Text(text = "Tracking Number (Optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(value = trackingNumber, onValueChange = { trackingNumber = it }, placeholder = { Text("Enter tracking number", fontSize = 13.sp) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                    }
                    Column {
                        Text(text = "Expected Delivery Date *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(value = deliveryDate, onValueChange = { deliveryDate = it }, placeholder = { Text("YYYY-MM-DD", fontSize = 13.sp) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                        Text(text = "e.g., 2026-04-27", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                    Button(
                        onClick = {
                            if (deliveryDate.isNotEmpty()) {
                                try {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    val date = sdf.parse(deliveryDate)
                                    val timestamp = date?.time ?: System.currentTimeMillis()
                                    onConfirm(courierName, trackingNumber, timestamp)
                                } catch (e: Exception) {
                                    // Invalid date format - button won't be clicked if validation fails
                                }
                            }
                        },
                        enabled = deliveryDate.isNotEmpty() && isValidDateFormat(deliveryDate),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Confirm Shipment", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── Mark Delivered Dialog ─────────────────────────────────────────────────────

@Composable
fun MarkDeliveredDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Success.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = "Confirm delivery?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "Mark this order as delivered. The buyer will be notified and revenue will be credited to your account.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Confirm Delivery", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        }
    )
}


// ── Helper Functions ─────────────────────────────────────────────────────────

fun isValidDateFormat(dateString: String): Boolean {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.isLenient = false
        sdf.parse(dateString)
        true
    } catch (e: Exception) {
        false
    }
}
