package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.OrderRepository
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.SellerPaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    paymentId: String,
    onBackClick: () -> Unit,
    viewModel: SellerPaymentViewModel = viewModel()
) {
    val selectedPayment by viewModel.selectedPayment.collectAsState()
    var showRefundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(paymentId) { viewModel.loadPaymentDetail(paymentId) }

    Scaffold(
        containerColor = BackgroundSecondary,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "Payment Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Order information",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(
                            modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        if (selectedPayment != null) {
            val payment = selectedPayment ?: return@Scaffold
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentStatusCard(payment)
                PaymentInfoSection(payment)
                PaymentItemsSection(payment)
                if (payment.coSellerStoreId.isNotEmpty()) CoSellerPaymentSplitInfo(payment)
                PaymentTimelineSection(payment)
                if (payment.status == PaymentStatus.PENDING.toString()) {
                    PaymentActionButtons(onRefund = { showRefundDialog = true })
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (showRefundDialog) {
                RefundDialog(
                    payment = payment,
                    onDismiss = { showRefundDialog = false },
                    onConfirm = { reason ->
                        viewModel.processRefund(payment.id, payment.amount, reason)
                        showRefundDialog = false
                    }
                )
            }
        } else {
            // Handle legacy orders
            LegacyOrderPaymentSplitView(
                orderId = paymentId,
                onBackClick = onBackClick,
                viewModel = viewModel
            )
        }
    }
}

// ── Payment Status Card ───────────────────────────────────────────────────────

@Composable
private fun PaymentStatusCard(payment: SellerPayment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val (statusColor, statusIcon) = when (payment.status.lowercase()) {
                "completed"  -> Success to Icons.Default.CheckCircle
                "pending"    -> Warning to Icons.Default.Schedule
                "processing" -> Color(0xFF2196F3) to Icons.Default.HourglassEmpty
                "failed"     -> Error to Icons.Default.Error
                "refunded"   -> TextSecondary to Icons.AutoMirrored.Filled.Undo
                else         -> TextSecondary to Icons.Default.Info
            }
            Box(
                modifier = Modifier.size(64.dp).background(statusColor.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = payment.status.replaceFirstChar { it.uppercase() }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = statusColor)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Order #${payment.orderId.take(8).uppercase()}", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ── Detail Card helper — tinted gradient header ───────────────────────────────

@Composable
private fun DetailCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Primary.copy(alpha = 0.06f), Primary.copy(alpha = 0.02f))))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            }
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
private fun PaymentInfoSection(payment: SellerPayment) {
    DetailCard(title = "Payment Information", icon = Icons.Default.Info) {
        PaymentInfoRow("Buyer", payment.buyerName, Icons.Default.Person)
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        PaymentInfoRow("Payment Method", payment.paymentMethod, Icons.Default.CreditCard)
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        PaymentInfoRow("Items", "${payment.itemsCount} item(s)", Icons.Default.ShoppingCart)
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        PaymentInfoRow("Date", formatPaymentDate(payment.createdAt), Icons.Default.DateRange)
        if (payment.transactionId.isNotEmpty()) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            PaymentInfoRow("Transaction ID", payment.transactionId, Icons.Default.Receipt)
        }
    }
}

@Composable
private fun PaymentItemsSection(payment: SellerPayment) {
    if (payment.itemsDetails.isEmpty()) return
    DetailCard(title = "Items in This Payment", icon = Icons.Default.Inventory) {
        payment.itemsDetails.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.productTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = "Qty: ${item.quantity}", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 1.dp))
                }
                Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", item.itemTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Success)
            }
            if (index < payment.itemsDetails.size - 1) {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun PaymentTimelineSection(payment: SellerPayment) {
    DetailCard(title = "Timeline", icon = Icons.Default.Timeline) {
        TimelineItemRow("Payment Created", formatPaymentDate(payment.createdAt), isCompleted = true)
        if (payment.status == PaymentStatus.COMPLETED.toString() && payment.paymentDate != null) {
            TimelineItemRow("Payment Completed", formatPaymentDate(payment.paymentDate), isCompleted = true)
        } else if (payment.status == PaymentStatus.REFUNDED.toString() && payment.refundDate != null) {
            TimelineItemRow("Refund Processed", formatPaymentDate(payment.refundDate), isCompleted = true)
        } else {
            TimelineItemRow("Awaiting Payment", "Pending", isCompleted = false)
        }
    }
}

@Composable
private fun PaymentActionButtons(onRefund: () -> Unit) {
    OutlinedButton(
        onClick = onRefund,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Error),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Process Refund", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CoSellerPaymentSplitInfo(payment: SellerPayment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCE93D8))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(36.dp).background(Color(0xFF7B1FA2).copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = "Co-Seller Order", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                Text(text = "Store: ${payment.storeName}", fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(top = 3.dp))
                Text(text = "This order involves multiple sellers. View the complete payment split to see all seller payouts.", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun PaymentInfoRow(label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun TimelineItemRow(title: String, date: String, isCompleted: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(if (isCompleted) Success else BorderColor, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = date, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun RefundDialog(payment: SellerPayment, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var refundReason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
            }
        },
        title = { Text("Process Refund", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = BackgroundSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Amount: PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Column {
                    Text(text = "Refund Reason *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(refundReason) },
                enabled = refundReason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Confirm Refund", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

private fun formatPaymentDate(timestamp: Long): String =
    java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))

private fun formatPaymentDate(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))
}

// ── Legacy order views ────────────────────────────────────────────────────────

@Composable
private fun LegacyOrderPaymentSplitView(orderId: String, onBackClick: () -> Unit, viewModel: SellerPaymentViewModel) {
    var orderData by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        try {
            val orderRepository = OrderRepository()
            val result = orderRepository.getOrderById(orderId)
            if (result.isSuccess) {
                orderData = result.getOrNull()
                if (orderData == null) error = "Order not found"
            } else {
                error = result.exceptionOrNull()?.message ?: "Failed to load order"
            }
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        error != null -> LegacyOrderErrorView(orderId = orderId, error = error ?: "Unknown error", onBackClick = onBackClick)
        orderData != null -> GeneratedPaymentSplitView(order = orderData ?: return, onBackClick = onBackClick)
    }
}

@Composable
private fun GeneratedPaymentSplitView(order: Order, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Legacy banner — Warning-tinted Surface replacing Card(0xFFFFF3CD, elevation=2.dp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF856404).copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF856404).copy(alpha = 0.20f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(32.dp).background(Color(0xFF856404).copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(15.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Legacy Order Payment Split", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF856404))
                    Text(text = "Generated from order data", fontSize = 11.sp, color = Color(0xFF856404).copy(alpha = 0.80f))
                }
            }
        }

        GeneratedOrderSummaryCard(order)
        GeneratedPaymentBreakdownCard(order)
        GeneratedItemsBreakdownCard(order)
        if (order.coSellerStoreId.isNotEmpty()) GeneratedCoSellerSplitCard(order)
        Spacer(modifier = Modifier.height(6.dp))
    }
}

// Generated cards — all use DetailCard (0.dp elevation + BorderStroke) replacing elevation=2.dp

@Composable
private fun GeneratedOrderSummaryCard(order: Order) {
    DetailCard(title = "Order Summary", icon = Icons.Default.Receipt) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Order ID", fontSize = 12.sp, color = TextSecondary)
            Text(text = "#${order.id.take(10)}...", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Customer", fontSize = 12.sp, color = TextSecondary)
            RealtimeNameDisplay(userId = order.buyerId, fallbackName = order.buyerName.ifEmpty { "Customer" }, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Total Amount", fontSize = 12.sp, color = TextSecondary)
            Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", order.totalPrice)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
    }
}

@Composable
private fun GeneratedPaymentBreakdownCard(order: Order) {
    DetailCard(title = "Payment Breakdown", icon = Icons.Default.CreditCard) {
        PaymentBreakdownRow(label = "Subtotal", amount = order.totalPrice - (order.shippingCost ?: 0.0), isSubtotal = true)
        if ((order.shippingCost ?: 0.0) > 0) {
            PaymentBreakdownRow(label = "Shipping", amount = order.shippingCost ?: 0.0, isSubtotal = true)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        val platformFee = order.totalPrice * 0.05
        PaymentBreakdownRow(label = "Platform Fee (5%)", amount = platformFee, isDeduction = true)
        PaymentBreakdownRow(label = "Your Earnings", amount = order.totalPrice - platformFee, isTotal = true)
    }
}

@Composable
private fun PaymentBreakdownRow(label: String, amount: Double, isSubtotal: Boolean = false, isDeduction: Boolean = false, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = if (isTotal) 14.sp else 13.sp, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, color = when { isTotal -> Primary; isDeduction -> Error; else -> TextSecondary })
        Text(text = "${if (isDeduction) "-" else ""}PKR ${String.format(java.util.Locale.US, "%.0f", amount)}", fontSize = if (isTotal) 14.sp else 13.sp, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium, color = when { isTotal -> Primary; isDeduction -> Error; else -> TextPrimary })
    }
    if (!isTotal) Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun GeneratedItemsBreakdownCard(order: Order) {
    DetailCard(title = "Items (${order.items.size})", icon = Icons.Default.Inventory) {
        order.items.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.productTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(text = "Qty: ${item.quantity} × PKR ${item.price.toInt()}", fontSize = 12.sp, color = TextSecondary)
                }
                Text(text = "PKR ${(item.quantity * item.price).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
            if (index < order.items.size - 1) HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun GeneratedCoSellerSplitCard(order: Order) {
    DetailCard(title = "Co-Seller Store Split", icon = Icons.Default.Store) {
        Text(text = "This order was part of a co-seller store. The payment would have been split among store members according to the store's payment configuration.", fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE3F2FD),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(14.dp))
                Text(text = "Exact split details not available for legacy orders", fontSize = 12.sp, color = Color(0xFF1976D2))
            }
        }
    }
}

@Composable
private fun LegacyOrderErrorView(orderId: String, error: String, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error.copy(0.08f) tinted circle — theme tokens replacing hardcoded 0xFFF8D7DA
        Box(
            modifier = Modifier.size(80.dp).background(Error.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = Error.copy(alpha = 0.60f), modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Unable to Load Order", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Order: $orderId", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error, fontSize = 13.sp, color = Error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(28.dp))

        // Gradient fill — consistent with all primary CTAs
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Back to Orders", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}