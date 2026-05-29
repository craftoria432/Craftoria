package com.gcuf.craftoria.ui.screens.seller

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextField
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
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.model.getRefundDateLong
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.SellerPaymentViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    paymentId: String,
    onBackClick: () -> Unit,
    viewModel: SellerPaymentViewModel = viewModel()
) {
    // ── Real-time state (replaces one-shot LaunchedEffect fetch) ──────────────
    // Using a real-time snapshot listener so the screen reflects refund status
    // changes instantly when admin approves/rejects from the web dashboard.
    var payment     by remember { mutableStateOf<SellerPayment?>(null) }
    var isLoading   by remember { mutableStateOf(true) }
    var loadError   by remember { mutableStateOf<String?>(null) }

    var showRefundDialog    by remember { mutableStateOf(false) }
    var isSubmittingRefund  by remember { mutableStateOf(false) }
    var refundSuccessMsg    by remember { mutableStateOf<String?>(null) }
    var refundErrorMsg      by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val currentUserId  = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ── Real-time Firestore listener ──────────────────────────────────────────
    DisposableEffect(paymentId) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val listener = db.collection("payments")
            .document(paymentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadError = error.message
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    // ✅ Use PaymentRepository.parsePayment() — avoids Timestamp→Long crash
                    val parsed = PaymentRepository.parsePayment(snapshot)
                    if (parsed != null) {
                        payment   = parsed
                        loadError = null
                    } else {
                        loadError = "Failed to parse payment data"
                    }
                } else {
                    loadError = "Payment not found"
                }
                isLoading = false
            }
        onDispose { listener.remove() }
    }

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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Order information",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
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
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Error.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Error.copy(alpha = 0.70f),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Text(
                            text = loadError ?: "Unknown error",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text(
                                "Go Back",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            payment != null -> {
                val p = payment!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentStatusCard(p)
                    PaymentInfoSection(p)
                    PaymentItemsSection(p)
                    PaymentTimelineSection(p)

                    // ── Refund eligibility check ──────────────────────────────
                    // Show "Initiate Refund" button only when:
                    //   1. Payment is completed or pending (not already refunded/processing)
                    //   2. Seller is the owner of this payment
                    //   3. No active refund already exists (checked via status field on payment)
                    val isRefundEligible = p.sellerId == currentUserId &&
                            p.status.lowercase() in listOf("completed", "pending") &&
                            !p.status.lowercase().startsWith("refund")

                    if (isRefundEligible) {
                        SellerInitiateRefundButton(
                            onRefund = { showRefundDialog = true }
                        )
                    } else if (p.status.lowercase().startsWith("refund")) {
                        // Show refund status notice if already in refund flow
                        RefundStatusNotice(status = p.status)
                    }

                    // Success / error snack-style notices
                    refundSuccessMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Success.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Success.copy(alpha = 0.30f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = msg, fontSize = 13.sp, color = Success, lineHeight = 18.sp)
                            }
                        }
                    }

                    refundErrorMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Error.copy(alpha = 0.06f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Error.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = msg, fontSize = 13.sp, color = Error, lineHeight = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // ── Seller Refund Dialog ──────────────────────────────────────
                if (showRefundDialog) {
                    SellerRefundDialog(
                        payment = p,
                        isSubmitting = isSubmittingRefund,
                        onDismiss = { showRefundDialog = false },
                        onConfirm = { reason, reasonDetails ->
                            coroutineScope.launch {
                                isSubmittingRefund = true
                                refundSuccessMsg   = null
                                refundErrorMsg     = null
                                try {
                                    viewModel.initiateSellerRefund(
                                        payment       = p,
                                        reason        = reason,
                                        reasonDetails = reasonDetails,
                                        sellerId      = currentUserId
                                    )
                                    showRefundDialog  = false
                                    refundSuccessMsg  = "Refund request submitted. Admin will review and approve it."
                                } catch (e: Exception) {
                                    Log.e("PaymentDetailScreen", "Seller refund failed", e)
                                    showRefundDialog = false
                                    refundErrorMsg   = e.message ?: "Failed to submit refund request"
                                } finally {
                                    isSubmittingRefund = false
                                }
                            }
                        }
                    )
                }
            }
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (statusColor, statusIcon) = when (payment.status.lowercase()) {
                "completed"         -> Success to Icons.Default.CheckCircle
                "pending"           -> Warning to Icons.Default.Schedule
                "processing"        -> Color(0xFF2196F3) to Icons.Default.HourglassEmpty
                "failed"            -> Error to Icons.Default.Error
                "refunded"          -> Color(0xFF9C27B0) to Icons.AutoMirrored.Filled.Undo
                "refund_pending"    -> Warning to Icons.Default.HourglassEmpty
                "refund_processing" -> Color(0xFF2196F3) to Icons.Default.Sync
                "refund_rejected"   -> Error to Icons.Default.Cancel
                else                -> TextSecondary to Icons.Default.Info
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(statusColor.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = payment.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Order #${payment.orderId.take(8).uppercase()}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            // Show refund amount if already refunded
            if (payment.status.lowercase() == "refunded" && payment.refundAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Error.copy(alpha = 0.07f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Error.copy(alpha = 0.20f))
                ) {
                    Text(
                        text = "Refunded: PKR ${String.format(java.util.Locale.US, "%.0f", payment.refundAmount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Error,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}



// ── Detail Card helper ────────────────────────────────────────────────────────

@Composable
private fun DetailCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
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
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            content = content
        )
    }
}

@Composable
private fun PaymentInfoSection(payment: SellerPayment) {
    DetailCard(title = "Payment Information", icon = Icons.Default.Info) {
        PaymentInfoRow("Buyer", Icons.Default.Person) {
            RealtimeNameDisplay(
                userId = payment.buyerId,
                fallbackName = payment.buyerName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        
        // ✅ REALTIME: Display seller name with real-time updates from Firebase
        PaymentInfoRow("Seller", Icons.Default.Store) {
            RealtimeNameDisplay(
                userId = payment.sellerId,
                fallbackName = payment.sellerName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        
        PaymentInfoRow("Payment Method", Icons.Default.CreditCard) {
            Text(
                text = payment.paymentMethod,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        PaymentInfoRow("Items", Icons.Default.ShoppingCart) {
            Text(
                text = "${payment.itemsCount} item(s)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        PaymentInfoRow("Date", Icons.Default.DateRange) {
            Text(
                text = formatPaymentDate(payment.getCreatedAtLong()),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        if (payment.transactionId.isNotEmpty()) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            PaymentInfoRow("Transaction ID", Icons.Default.Receipt) {
                Text(
                    text = payment.transactionId,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
        // Show refund reason if refunded
        if (payment.refundReason.isNotEmpty()) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            PaymentInfoRow("Refund Reason", Icons.Default.Info) {
                Text(
                    text = formatRefundReason(payment.refundReason),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Error,
                    modifier = Modifier.weight(0.6f, fill = false),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun PaymentItemsSection(payment: SellerPayment) {
    if (payment.itemsDetails.isEmpty()) return
    DetailCard(title = "Items in This Payment", icon = Icons.Default.Inventory) {
        payment.itemsDetails.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Qty: ${item.quantity}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Text(
                    text = "PKR ${String.format(java.util.Locale.US, "%.0f", item.itemTotal)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
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
        TimelineItemRow("Payment Created", formatPaymentDate(payment.getCreatedAtLong()), isCompleted = true)
        if (payment.status == PaymentStatus.COMPLETED.toString() && payment.paymentDate != null) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineItemRow("Payment Completed", formatPaymentDate(payment.paymentDate), isCompleted = true)
        }
        if (payment.status.lowercase() == "refunded" || payment.status.lowercase().startsWith("refund")) {
            val refundTs = payment.getRefundDateLong()
            if (refundTs > 0L) {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                TimelineItemRow(
                    title = when (payment.status.lowercase()) {
                        "refunded"          -> "Refund Completed"
                        "refund_pending"    -> "Refund Requested"
                        "refund_processing" -> "Refund Processing"
                        "refund_rejected"   -> "Refund Rejected"
                        else                -> "Refund"
                    },
                    date = formatPaymentDate(refundTs),
                    isCompleted = true
                )
            }
        } else if (payment.status != PaymentStatus.COMPLETED.toString()) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineItemRow("Awaiting Payment", "Pending", isCompleted = false)
        }
    }
}

// ── Seller Initiate Refund Button ─────────────────────────────────────────────

@Composable
private fun SellerInitiateRefundButton(onRefund: () -> Unit) {
    // Outlined style with error color — distinguishes this from primary CTAs
    // and signals a financial action that needs careful consideration
    OutlinedButton(
        onClick = onRefund,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
        border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.70f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "Initiate Refund for Buyer",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
    // Policy reminder beneath button
    Text(
        text = "Seller-initiated refunds require admin approval before processing.",
        fontSize = 11.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}

// ── Refund Status Notice (shown when refund already in progress) ──────────────

@Composable
private fun RefundStatusNotice(status: String) {
    val (bg, fg, icon, msg) = when (status.lowercase()) {
        "refund_pending" -> listOf(
            Warning.copy(alpha = 0.08f),
            Warning,
            Icons.Default.HourglassEmpty,
            "A refund request is pending admin review for this payment."
        )
        "refund_processing" -> listOf(
            Color(0xFF2196F3).copy(alpha = 0.08f),
            Color(0xFF2196F3),
            Icons.Default.Sync,
            "Refund is currently being processed."
        )
        "refunded" -> listOf(
            Color(0xFF9C27B0).copy(alpha = 0.08f),
            Color(0xFF9C27B0),
            Icons.Default.CheckCircle,
            "This payment has been refunded to the buyer."
        )
        "refund_rejected" -> listOf(
            Error.copy(alpha = 0.08f),
            Error,
            Icons.Default.Cancel,
            "The refund request was rejected. You may submit a new request."
        )
        else -> return
    }

    @Suppress("UNCHECKED_CAST")
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg as Color,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, (fg as Color).copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon as ImageVector,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = msg as String,
                fontSize = 12.sp,
                color = fg,
                lineHeight = 18.sp
            )
        }
    }
}

// ── Seller Refund Dialog ──────────────────────────────────────────────────────
//
// Key differences from the old RefundDialog:
//   • Shows a clear "Pending admin approval" notice so seller knows the flow
//   • Collects a structured reason (dropdown) + optional detail text
//   • Disabled while submitting

@Composable
private fun SellerRefundDialog(
    payment: SellerPayment,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, reasonDetails: String) -> Unit
) {
    val reasons = listOf(
        "out_of_stock"        to "Out of Stock",
        "wrong_item_sent"     to "Wrong Item Sent",
        "item_damaged"        to "Item Damaged Before Dispatch",
        "duplicate_order"     to "Duplicate Order",
        "pricing_error"       to "Pricing Error",
        "other"               to "Other"
    )
    var selectedReason  by remember { mutableStateOf(reasons.first()) }
    var reasonDetails   by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Error.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Initiate Refund for Buyer",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount display
                Surface(
                    color = BackgroundSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Refund Amount", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = "PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }

                // Admin approval notice
                Surface(
                    color = Color(0xFF2196F3).copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2196F3).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(15.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = "This request will be sent to admin for review. Refund is processed only after admin approval.",
                            fontSize = 11.sp,
                            color = Color(0xFF1565C0),
                            lineHeight = 17.sp
                        )
                    }
                }

                // Reason selection
                Text(
                    text = "Reason *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reasons.forEach { reason ->
                        val isSelected = selectedReason == reason
                        Surface(
                            onClick = { selectedReason = reason },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Primary.copy(alpha = 0.06f) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) Primary else BorderColor
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = reason.second,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Primary else TextPrimary
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Primary,
                                        unselectedColor = BorderColor
                                    )
                                )
                            }
                        }
                    }
                }

                // Optional detail text
                StandardizedOutlinedTextField(
                    value = reasonDetails,
                    onValueChange = { reasonDetails = it },
                    label = "Details",
                    placeholder = "Additional details (optional)…",
                    minLines = 2,
                    maxLines = 4,
                    minHeight = 80
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReason.first, reasonDetails) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(42.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submitting…", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                } else {
                    Text("Submit Refund", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        }
    )
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun PaymentInfoRow(
    label: String,
    icon: ImageVector,
    value: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
        }
        Row(
            modifier = Modifier.weight(0.5f),
            horizontalArrangement = Arrangement.End
        ) {
            value()
        }
    }
}

@Composable
private fun TimelineItemRow(title: String, date: String, isCompleted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (isCompleted) Success else BorderColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = date, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatPaymentDate(timestamp: Long): String =
    java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US)
        .format(java.util.Date(timestamp))

private fun formatPaymentDate(timestamp: Long?): String {
    if (timestamp == null) return "N/A"
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US)
        .format(java.util.Date(timestamp))
}

private fun formatRefundReason(reason: String): String {
    // Convert snake_case to sentence case
    // Examples: "lost_in_transit" → "Lost in transit", "defective_product" → "Defective product"
    return reason
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
