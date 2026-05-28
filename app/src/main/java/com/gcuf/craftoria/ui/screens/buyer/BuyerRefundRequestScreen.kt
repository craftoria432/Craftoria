package com.gcuf.craftoria.ui.screens.buyer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.getStatusEnum
import com.gcuf.craftoria.data.model.getDeliveredAtLong
import com.gcuf.craftoria.data.model.getRequestedAtLong
import com.gcuf.craftoria.data.model.getCompletedAtLong
import com.gcuf.craftoria.data.model.RefundReason
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.repository.OrderRepository
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.ui.components.SelectionButtonCompact
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextField
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.RefundProcessor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerRefundRequestScreen(
    orderId: String,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val orderRepository = remember { OrderRepository() }
    val paymentRepository = remember { PaymentRepository() }
    val refundProcessor = remember { RefundProcessor() }
    val refundRepository = remember { com.gcuf.craftoria.data.repository.RefundRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance()) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var order by remember { mutableStateOf<Order?>(null) }
    var existingRefund by remember { mutableStateOf<com.gcuf.craftoria.data.model.RefundRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedReason by remember { mutableStateOf<RefundReason?>(null) }
    var otherReasonDetails by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var submitErrorMessage by remember { mutableStateOf("") }

    // ✅ Load order data and check for existing refund
    LaunchedEffect(orderId) {
        try {
            isLoading = true
            val result = orderRepository.getOrderById(orderId)
            if (result.isSuccess) {
                order = result.getOrNull()

                // ✅ CRITICAL FIX: Check ALL refunds for this order to enforce 2-attempt limit
                val refundsResult = refundRepository.getRefundsByOrderId(orderId)
                if (refundsResult.isSuccess) {
                    val allRefunds = refundsResult.getOrNull() ?: emptyList()
                    
                    // Get the most recent refund (if any)
                    existingRefund = allRefunds.maxByOrNull { it.getRequestedAtLong() }
                    
                    // ✅ Check if buyer has reached rejection limit
                    if (existingRefund != null) {
                        val refund = existingRefund!!
                        
                        // Check if this is a final decision (2 rejections reached)
                        if (refund.finalDecision) {
                            errorMessage = "Refund request denied (FINAL DECISION)\n\nYour refund request has been rejected twice. No further refund requests can be submitted for this order."
                        }
                        // Check if refund is still pending/processing
                        else if (refund.status.lowercase() in listOf("requested", "under_review", "approved_by_seller", "approved_by_admin", "processing")) {
                            errorMessage = "A refund request is already pending for this order.\n\nStatus: ${refund.status.replaceFirstChar { it.uppercase() }}\n\nPlease wait for the current request to be processed."
                        }
                        // Check if refund was already completed
                        else if (refund.status.lowercase() == "completed") {
                            errorMessage = "This order has already been refunded successfully."
                        }
                        // If rejected but can resubmit (first rejection only)
                        else if (refund.status.lowercase() in listOf("rejected_by_seller", "rejected_by_admin") && refund.canResubmit) {
                            // Allow resubmission - show form
                            existingRefund = null  // Clear to show form
                        }
                    }
                }

                // ✅ Validate refund eligibility only if no blocking conditions
                if (existingRefund == null && errorMessage == null) {
                    order?.let { ord ->
                        val status = ord.getStatusEnum()
                        // ✅ FIX: Allow refunds for both DELIVERED and COMPLETED orders
                        if (status != OrderStatus.DELIVERED && status != OrderStatus.COMPLETED) {
                            errorMessage = "Refunds can only be requested for delivered orders"
                        } else {
                            val deliveredAt = ord.getDeliveredAtLong()
                            val effectiveDate = if (deliveredAt > 0) deliveredAt else ord.getCreatedAtLong()
                            val daysSinceDelivery = (System.currentTimeMillis() - effectiveDate) / (1000 * 60 * 60 * 24)
                            if (daysSinceDelivery > 30) {
                                errorMessage = "Refund window expired (30 days from delivery)"
                            }
                        }
                    }
                }
            } else {
                errorMessage = "Order not found"
            }
        } catch (e: Exception) {
            Log.e("BuyerRefundRequest", "Error loading order", e)
            errorMessage = e.message ?: "Failed to load order"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "Request Refund",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Order #${orderId.take(8).uppercase()}",
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
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            errorMessage != null -> {
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
                        // Tinted error circle — consistent with all empty/error states
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
                            text = errorMessage ?: "Unknown error",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .defaultMinSize(minWidth = 140.dp)
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

            order != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Order Summary Card
                    OrderSummaryCard(order = order!!)

                    // ✅ Show refund status if exists, otherwise show request form
                    if (existingRefund != null) {
                        RefundStatusCard(refund = existingRefund!!)
                    } else {
                        // Refund Policy Notice
                        RefundPolicyNotice()

                        // Refund Reason Selection
                        RefundReasonSection(
                            selectedReason = selectedReason,
                            onReasonSelected = { selectedReason = it },
                            otherReasonDetails = otherReasonDetails,
                            onOtherReasonDetailsChanged = { otherReasonDetails = it }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Submit Button — gradient CTA consistent with all primary actions
                        Button(
                            onClick = {
                                if (selectedReason != null) {
                                    if (selectedReason == RefundReason.OTHER && otherReasonDetails.isBlank()) {
                                        submitErrorMessage = "Please provide details for 'Other' reason"
                                        showErrorDialog = true
                                    } else {
                                        coroutineScope.launch {
                                            isSubmitting = true
                                            try {
                                                // ✅ CRITICAL FIX: Fetch payment IDs from order
                                                val paymentsResult = paymentRepository.getOrderPayments(
                                                    orderId = orderId,
                                                    requestingUserId = currentUserId
                                                )

                                                if (paymentsResult.isFailure) {
                                                    submitErrorMessage = paymentsResult.exceptionOrNull()?.message
                                                        ?: "Failed to fetch payment information"
                                                    showErrorDialog = true
                                                    isSubmitting = false
                                                    return@launch
                                                }

                                                val payments = paymentsResult.getOrNull() ?: emptyList()

                                                if (payments.isEmpty()) {
                                                    submitErrorMessage = "No payment records found for this order"
                                                    showErrorDialog = true
                                                    isSubmitting = false
                                                    return@launch
                                                }

                                                // ✅ Create refund for each payment (handles multi-seller orders)
                                                var allSuccess = true
                                                var failureMessage = ""

                                                payments.forEach { payment ->
                                                    val description = if (selectedReason == RefundReason.OTHER) {
                                                        otherReasonDetails
                                                    } else {
                                                        selectedReason!!.getDisplayName()
                                                    }

                                                    val result = refundProcessor.initiateRefund(
                                                        paymentId = payment.id,  // ✅ FIXED: Use actual payment ID
                                                        refundAmount = payment.amount,
                                                        reason = selectedReason!!.toString(),
                                                        description = description,
                                                        requestedBy = currentUserId
                                                    )

                                                    if (result.isFailure) {
                                                        allSuccess = false
                                                        failureMessage = result.exceptionOrNull()?.message
                                                            ?: "Failed to create refund"
                                                    }
                                                }

                                                if (allSuccess) {
                                                    // ✅ FIX: Add small delay to ensure Firestore write propagates
                                                    // before refreshing the UI state
                                                    kotlinx.coroutines.delay(500)
                                                    
                                                    // Refresh to show status card instead of form
                                                    val refundsResult = refundRepository.getRefundsByOrderId(orderId)
                                                    if (refundsResult.isSuccess) {
                                                        existingRefund = refundsResult.getOrNull()?.firstOrNull()
                                                    }
                                                    showSuccessDialog = true
                                                } else {
                                                    submitErrorMessage = failureMessage
                                                    showErrorDialog = true
                                                }
                                            } catch (e: Exception) {
                                                Log.e("BuyerRefundRequest", "Error submitting refund", e)
                                                submitErrorMessage = e.message ?: "Failed to submit refund request"
                                                showErrorDialog = true
                                            } finally {
                                                isSubmitting = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = selectedReason != null && !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(
                                    brush = if (selectedReason != null && !isSubmitting)
                                        Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                                    else
                                        Brush.horizontalGradient(listOf(TextLight, TextLight)),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Submitting…",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Submit Refund Request",
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

    // ── Success Dialog ────────────────────────────────────────────────────────
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Success.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Refund Request Submitted",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Your refund request has been submitted successfully. Our team will review it and process within 3–5 business days.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                }
            }
        )
    }

    // ── Error Dialog ──────────────────────────────────────────────────────────
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
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
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Request Failed",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = submitErrorMessage,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        )
    }
}

// ── Order Summary Card ────────────────────────────────────────────────────────

@Composable
fun OrderSummaryCard(order: Order) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tinted section header — matches AddProductSectionCard / CheckoutSectionCard pattern
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
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "Order Summary",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OrderSummaryRow(label = "Order ID", value = "#${order.id.take(8).uppercase()}")
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                OrderSummaryRow(
                    label = "Total Amount",
                    value = "PKR ${order.totalPrice.toInt()}",
                    valueColor = Primary,
                    valueFontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                OrderSummaryRow(
                    label = "Delivered On",
                    value = formatRefundDate(order.getDeliveredAtLong())
                )

                // ✅ ENHANCEMENT 2: Show all products in multi-seller orders
                if (order.items.isNotEmpty()) {
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "Items (${order.items.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    order.items.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BackgroundSecondary,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productTitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Qty: ${item.quantity} × PKR ${item.price.toInt()}",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Text(
                                    text = "PKR ${(item.price * item.quantity).toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary
                                )
                            }
                        }
                    }
                } else if (order.productTitle.isNotEmpty()) {
                    // Legacy single product
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BackgroundSecondary,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = order.productTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Qty: ${order.quantity}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                text = "PKR ${order.productPrice.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
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
private fun OrderSummaryRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    valueFontWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 13.sp, fontWeight = valueFontWeight, color = valueColor)
    }
}

// ── Refund Policy Notice ──────────────────────────────────────────────────────

@Composable
fun RefundPolicyNotice() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Warning.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Warning.copy(alpha = 0.30f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Warning.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(15.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Refund Policy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF57C00)
                )
                listOf(
                    "Refunds processed within 3–5 business days",
                    "Amount refunded via original payment method",
                    "Refund window: 30 days from delivery"
                ).forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color(0xFFF57C00), CircleShape)
                                .padding(top = 5.dp)
                        )
                        Text(
                            text = point,
                            fontSize = 11.sp,
                            color = Color(0xFF6D4C00),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Refund Reason Section ─────────────────────────────────────────────────────

@Composable
fun RefundReasonSection(
    selectedReason: RefundReason?,
    onReasonSelected: (RefundReason) -> Unit,
    otherReasonDetails: String,
    onOtherReasonDetailsChanged: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tinted section header
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
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Column {
                    Text(
                        text = "Select Refund Reason",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Required",
                        fontSize = 10.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val reasons = listOf(
                    RefundReason.DEFECTIVE_PRODUCT,
                    RefundReason.WRONG_ITEM,
                    RefundReason.LOST_IN_TRANSIT,
                    RefundReason.OTHER
                )

                reasons.forEach { reason ->
                    SelectionButtonCompact(
                        text = reason.getDisplayName(),
                        isSelected = selectedReason == reason,
                        onClick = { onReasonSelected(reason) },
                        minHeight = 40
                    )
                }

                // Show text field for "Other" reason
                if (selectedReason == RefundReason.OTHER) {
                    StandardizedOutlinedTextField(
                        value = otherReasonDetails,
                        onValueChange = onOtherReasonDetailsChanged,
                        label = "Reason Details *",
                        placeholder = "Please describe the issue in detail…",
                        minLines = 3,
                        maxLines = 5,
                        minHeight = 100
                    )
                }
            }
        }
    }
}

// ── Refund Reason Option ──────────────────────────────────────────────────────

// ── Date helper ───────────────────────────────────────────────────────────────

private fun formatRefundDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}

// ── Refund Status Card ────────────────────────────────────────────────────────

@Composable
fun RefundStatusCard(refund: com.gcuf.craftoria.data.model.RefundRequest) {
    val status = try {
        com.gcuf.craftoria.data.model.RefundStatus.valueOf(refund.status)
    } catch (e: Exception) {
        com.gcuf.craftoria.data.model.RefundStatus.REQUESTED
    }

    val (statusColor, statusIcon, statusTitle, statusMessage) = when (status) {
        com.gcuf.craftoria.data.model.RefundStatus.REQUESTED,
        com.gcuf.craftoria.data.model.RefundStatus.UNDER_REVIEW -> {
            Tuple4(
                Warning,
                Icons.Default.HourglassEmpty,
                "Refund Request Under Review",
                "Your refund request has been submitted and is currently being reviewed by our team. We'll notify you once a decision is made."
            )
        }
        com.gcuf.craftoria.data.model.RefundStatus.APPROVED_BY_SELLER,
        com.gcuf.craftoria.data.model.RefundStatus.APPROVED_BY_ADMIN -> {
            Tuple4(
                Color(0xFF2196F3),
                Icons.Default.CheckCircleOutline,
                "Refund Approved",
                "Great news! Your refund has been approved and is being processed. The amount will be credited to your original payment method within 3-5 business days."
            )
        }
        com.gcuf.craftoria.data.model.RefundStatus.PROCESSING -> {
            Tuple4(
                Color(0xFF2196F3),
                Icons.Default.Sync,
                "Refund Processing",
                "Your refund is currently being processed. The amount will be credited to your original payment method shortly."
            )
        }
        com.gcuf.craftoria.data.model.RefundStatus.COMPLETED -> {
            Tuple4(
                Success,
                Icons.Default.CheckCircle,
                "Refund Completed",
                "Your refund has been successfully processed and credited to your account on ${formatRefundDate(refund.getCompletedAtLong())}."
            )
        }
        com.gcuf.craftoria.data.model.RefundStatus.REJECTED_BY_SELLER,
        com.gcuf.craftoria.data.model.RefundStatus.REJECTED_BY_ADMIN -> {
            Tuple4(
                Error,
                Icons.Default.Cancel,
                "Refund Request Rejected",
                "Unfortunately, your refund request has been rejected. ${if (refund.approvalNotes.isNotEmpty()) "Reason: ${refund.approvalNotes}" else "Please contact support for more information."}"
            )
        }
        com.gcuf.craftoria.data.model.RefundStatus.FAILED -> {
            Tuple4(
                Error,
                Icons.Default.Error,
                "Refund Processing Failed",
                "There was an issue processing your refund. Our team has been notified and will resolve this shortly. ${if (refund.errorMessage.isNotEmpty()) "Error: ${refund.errorMessage}" else ""}"
            )
        }
        com.gcuf.craftoria.data.model.RefundStatus.CANCELLED -> {
            Tuple4(
                TextSecondary,
                Icons.Default.Cancel,
                "Refund Cancelled",
                "Your refund request has been cancelled. Please contact support if you have questions."
            )
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Status header with gradient background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(statusColor.copy(alpha = 0.10f), statusColor.copy(alpha = 0.03f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = "Submitted on ${formatRefundDate(refund.getRequestedAtLong())}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Status message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = statusMessage,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                // Refund details
                RefundDetailRow(label = "Refund Amount", value = "PKR ${refund.refundAmount.toInt()}")
                RefundDetailRow(label = "Reason", value = refund.reasonDetails.ifEmpty { refund.reason })
                
                if (refund.gatewayRefundId.isNotEmpty()) {
                    RefundDetailRow(label = "Transaction ID", value = refund.gatewayRefundId)
                }

                // Show contact support button for rejected/failed refunds
                if (status == com.gcuf.craftoria.data.model.RefundStatus.REJECTED_BY_SELLER ||
                    status == com.gcuf.craftoria.data.model.RefundStatus.REJECTED_BY_ADMIN ||
                    status == com.gcuf.craftoria.data.model.RefundStatus.FAILED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedButton(
                        onClick = { /* Navigate to support */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Support,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contact Support",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RefundDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

// Helper data class for status information
internal data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)