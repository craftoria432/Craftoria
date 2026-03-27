package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.OrderRepository
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.SellerPaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSplitDetailScreen(
    orderId: String,
    currentUserId: String,
    isStoreOwner: Boolean = false,
    isStoreMember: Boolean = false,
    isBuyer: Boolean = false,
    onBackClick: () -> Unit,
    viewModel: SellerPaymentViewModel = viewModel()
) {
    var paymentData by remember { mutableStateOf<SellerPayment?>(null) }
    var orderData by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLegacyOrder by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        try {
            val paymentRepository = PaymentRepository()

            // FIX: getPaymentById requires paymentId AND requestingUserId
            val paymentResult = paymentRepository.getPaymentById(
                paymentId = orderId,
                requestingUserId = currentUserId
            )

            if (paymentResult.isSuccess && paymentResult.getOrNull() != null) {
                paymentData = paymentResult.getOrNull()
                isLegacyOrder = false
            } else {
                val orderRepository = OrderRepository()
                val orderResult = orderRepository.getOrderById(orderId)

                if (orderResult.isSuccess && orderResult.getOrNull() != null) {
                    orderData = orderResult.getOrNull()
                    isLegacyOrder = true
                } else {
                    error = "Order or payment not found"
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load payment split data"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Payment Split Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Order: #${orderId.take(8).uppercase()}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
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
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                error != null -> {
                    PaymentSplitErrorView(
                        orderId = orderId,
                        error = error!!,
                        onBackClick = onBackClick
                    )
                }

                isLegacyOrder && orderData != null -> {
                    LegacyOrderPaymentSplitView(
                        order = orderData!!,
                        currentUserId = currentUserId,
                        onBackClick = onBackClick
                    )
                }

                !isLegacyOrder && paymentData != null -> {
                    ModernPaymentSplitView(
                        payment = paymentData!!,
                        currentUserId = currentUserId,
                        isStoreOwner = isStoreOwner,
                        isStoreMember = isStoreMember,
                        isBuyer = isBuyer,
                        onBackClick = onBackClick
                    )
                }

                else -> {
                    PaymentSplitErrorView(
                        orderId = orderId,
                        error = "No payment split data available",
                        onBackClick = onBackClick
                    )
                }
            }
        }
    }
}

// ── Error / unavailable state ─────────────────────────────────────────────────

@Composable
private fun PaymentSplitErrorView(
    orderId: String,
    error: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Error.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = Error.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Payment Split Unavailable",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Order: #${orderId.take(8).uppercase()}",
            fontSize = 12.sp,
            color = TextLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(46.dp)
                .defaultMinSize(minWidth = 160.dp)
                .background(
                    Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                    RoundedCornerShape(12.dp)
                )
        ) {
            Text(
                text = "Back to Orders",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ── Legacy order view ─────────────────────────────────────────────────────────

@Composable
private fun LegacyOrderPaymentSplitView(
    order: Order,
    currentUserId: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF856404).copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp, Color(0xFF856404).copy(alpha = 0.20f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF856404).copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Legacy Order Payment Split",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF856404)
                    )
                    Text(
                        text = "Generated from order data (retroactive)",
                        fontSize = 11.sp,
                        color = Color(0xFF856404).copy(alpha = 0.80f)
                    )
                }
            }
        }

        PaymentSplitOrderSummaryCard(order)
        PaymentSplitBreakdownCard(order)

        if (order.coSellerStoreId.isNotEmpty()) {
            CoSellerSplitCard(order, currentUserId)
        } else {
            SingleSellerPaymentCard(order, currentUserId)
        }
    }
}

// ── Modern payment view ───────────────────────────────────────────────────────

@Composable
private fun ModernPaymentSplitView(
    payment: SellerPayment,
    currentUserId: String,
    isStoreOwner: Boolean,
    isStoreMember: Boolean,
    isBuyer: Boolean,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Success.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp, Success.copy(alpha = 0.20f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Success.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Modern Payment Split",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Real-time payment tracking (prospective)",
                        fontSize = 11.sp,
                        color = Color(0xFF2E7D32).copy(alpha = 0.80f)
                    )
                }
            }
        }

        ModernPaymentSummaryCard(payment)

        if (payment.paymentSplits.isNotEmpty()) {
            ModernPaymentSplitsCard(payment, currentUserId)
        }
    }
}

// ── Shared section card helper ────────────────────────────────────────────────

@Composable
private fun PaymentSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color = Primary.copy(alpha = 0.10f),
    iconTint: Color = Primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Primary.copy(alpha = 0.06f), Primary.copy(alpha = 0.02f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconBg, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

// ── Status badge (private, renamed to avoid conflict with SellerOrdersScreen) ─

@Composable
private fun PaymentStatusBadge(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "COMPLETED" -> Success.copy(alpha = 0.12f) to Success
        "CANCELLED", "FAILED" -> Error.copy(alpha = 0.12f) to Error
        "SHIPPED" -> Color(0xFF6610F2).copy(alpha = 0.12f) to Color(0xFF6610F2)
        "PENDING" -> Color(0xFFFFA500).copy(alpha = 0.15f) to Color(0xFFFFA500)
        else -> Primary.copy(alpha = 0.10f) to Primary
    }
    Surface(color = bgColor, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = status.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ── Summary row ───────────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(
    label: String,
    value: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        value()
    }
}

// ── Order Summary Card ────────────────────────────────────────────────────────

@Composable
private fun PaymentSplitOrderSummaryCard(order: Order) {
    PaymentSectionCard(
        title = "Order Summary",
        icon = Icons.Default.Assignment
    ) {
        SummaryRow(label = "Order ID") {
            Text(
                text = "#${order.id.take(8).uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        SummaryRow(label = "Buyer") {
            Text(
                text = order.buyerName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        SummaryRow(label = "Status") {
            PaymentStatusBadge(order.status)
        }
    }
}

// ── Payment Breakdown Card ────────────────────────────────────────────────────

@Composable
private fun PaymentSplitBreakdownCard(order: Order) {
    PaymentSectionCard(
        title = "Payment Breakdown",
        icon = Icons.Default.CreditCard
    ) {
        SummaryRow(label = "Subtotal") {
            Text(
                text = "PKR ${String.format(java.util.Locale.getDefault(), "%,.0f", order.totalPrice ?: 0.0)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // FIX 1: Safe null check for comparison
        if ((order.shippingCost ?: 0.0) > 0) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            SummaryRow(label = "Shipping") {
                Text(
                    text = "PKR ${String.format(java.util.Locale.getDefault(), "%,.0f", order.shippingCost ?: 0.0)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        HorizontalDivider(color = Primary.copy(alpha = 0.15f), thickness = 0.5.dp)

        SummaryRow(label = "Total Amount") {
            // FIX 2: Summing nullable doubles
            val totalAmount = (order.totalPrice ?: 0.0) + (order.shippingCost ?: 0.0)
            Text(
                text = "PKR ${String.format(java.util.Locale.getDefault(), "%,.0f", totalAmount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}
// ── Co-Seller Split Card ──────────────────────────────────────────────────────

@Composable
private fun CoSellerSplitCard(order: Order, currentUserId: String) {
    PaymentSectionCard(
        title = "Co-Seller Store Payment Split",
        icon = Icons.Default.People
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE3F2FD)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 1.dp)
                )
                Text(
                    text = "This is a legacy order. Payment split is estimated based on current store configuration and may not reflect the actual split at the time of purchase.",
                    fontSize = 11.sp,
                    color = Color(0xFF1565C0),
                    lineHeight = 17.sp
                )
            }
        }

        Text(
            text = "Payment split calculation requires store configuration data.",
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )

        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

        SummaryRow(label = "Amount to Split") {
            Text(
                text = "PKR ${String.format(java.util.Locale.getDefault(), "%,.0f", order.totalPrice)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

// ── Single Seller Payment Card ────────────────────────────────────────────────

@Composable
private fun SingleSellerPaymentCard(order: Order, currentUserId: String) {
    PaymentSectionCard(
        title = "Seller Payment",
        icon = Icons.Default.Person,
        iconBg = Success.copy(alpha = 0.10f),
        iconTint = Success
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Full Payment Amount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Single seller product",
                    fontSize = 11.sp,
                    color = TextLight
                )
            }
            Text(
                text = "PKR ${String.format(java.util.Locale.getDefault(), "%,.0f", order.totalPrice)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Success
            )
        }
    }
}

// ── Modern Payment Summary Card ───────────────────────────────────────────────

@Composable
private fun ModernPaymentSummaryCard(payment: SellerPayment) {
    PaymentSectionCard(
        title = "Payment Summary",
        icon = Icons.Default.CreditCard
    ) {
        SummaryRow(label = "Payment ID") {
            Text(
                text = "#${payment.id.take(8).uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        SummaryRow(label = "Total Amount") {
            // FIX: SellerPayment field is `amount`, not `totalAmount`
            Text(
                text = "PKR ${String.format(java.util.Locale.getDefault(), "%,.0f", payment.amount)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        SummaryRow(label = "Status") {
            PaymentStatusBadge(payment.status)
        }
    }
}

// ── Modern Payment Splits Card ────────────────────────────────────────────────

@Composable
private fun ModernPaymentSplitsCard(payment: SellerPayment, currentUserId: String) {
    PaymentSectionCard(
        title = "Payment Splits",
        icon = Icons.Default.People
    ) {
        payment.paymentSplits.forEach { split ->
            val isCurrentUser = split.sellerId == currentUserId

            // FIX: Firestore may deserialize numeric fields as Double? at runtime
            // even when declared Double — safe-call with ?: 0.0 prevents NPE
            val splitPct = split.splitPercentage ?: 0.0
            val splitAmt = split.splitAmount ?: 0.0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isCurrentUser) Primary.copy(alpha = 0.06f) else BackgroundSecondary,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (isCurrentUser) Primary.copy(alpha = 0.15f) else BorderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = split.sellerName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${String.format("%.1f", splitPct)}% split",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (isCurrentUser) {
                            Surface(
                                color = Primary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Your share",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PKR ${String.format(java.util.Locale.getDefault(), "%,.0f", splitAmt)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentUser) Primary else TextPrimary
                        )
                        PaymentStatusBadge(split.status)
                    }
                }
            }
        }
    }
}