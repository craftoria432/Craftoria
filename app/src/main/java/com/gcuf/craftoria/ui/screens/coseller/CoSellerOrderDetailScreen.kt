package com.gcuf.craftoria.ui.screens.coseller

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
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.model.getRefundDateLong
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.components.StateBadge
import com.gcuf.craftoria.ui.components.BadgeState
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.SellerPaymentViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoSellerOrderDetailScreen(
    paymentId: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    viewModel: SellerPaymentViewModel = viewModel()
) {
    var payment  by remember { mutableStateOf<SellerPayment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error     by remember { mutableStateOf<String?>(null) }

    var showRefundDialog   by remember { mutableStateOf(false) }
    var isSubmittingRefund by remember { mutableStateOf(false) }
    var refundSuccessMsg   by remember { mutableStateOf<String?>(null) }
    var refundErrorMsg     by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // ── Real-time Firestore listener ──────────────────────────────────────────
    // Listen to both collections during migration so existing co-seller payments
    // created under seller_payments still open correctly from the detail screen.
    DisposableEffect(paymentId) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        Log.d("CoSellerOrderDetail", "Starting real-time listener for payment: $paymentId")

        var currentSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
        var legacySnapshot: com.google.firebase.firestore.DocumentSnapshot? = null

        suspend fun hasStoreAccess(payment: SellerPayment): Boolean {
            val isMainSeller = payment.sellerId == currentUserId
            val isSplitSeller = payment.paymentSplits.any { it.sellerId == currentUserId }
            val isInvolvedUser = payment.involvedSellerIds.contains(currentUserId)

            if (isMainSeller || isSplitSeller || isInvolvedUser) return true

            if (payment.coSellerStoreId.isEmpty()) return false

            return try {
                val storeSnapshot = db.collection("co_seller_stores")
                    .document(payment.coSellerStoreId)
                    .get()
                    .await()

                if (!storeSnapshot.exists()) {
                    false
                } else {
                    val ownerId = storeSnapshot.getString("owner_id") ?: ""
                    val memberIds = (storeSnapshot.get("member_ids") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()

                    currentUserId == ownerId || currentUserId in memberIds
                }
            } catch (e: Exception) {
                Log.e("CoSellerOrderDetail", "Failed to verify store membership for ${payment.coSellerStoreId}", e)
                false
            }
        }

        suspend fun applySnapshotState(
            source: String,
            snapshot: com.google.firebase.firestore.DocumentSnapshot?
        ) {
            if (snapshot == null || !snapshot.exists()) return

            val fetchedPayment = PaymentRepository.parsePayment(snapshot)

            if (fetchedPayment == null) {
                payment = null
                error = "Failed to parse payment data"
                Log.e("CoSellerOrderDetail", "parsePayment returned null for $paymentId from $source")
                return
            }

            if (hasStoreAccess(fetchedPayment)) {
                payment = fetchedPayment
                error = null
                Log.d(
                    "CoSellerOrderDetail",
                    "Loaded payment from $source: status=${fetchedPayment.status}, amount=${fetchedPayment.amount}"
                )
            } else {
                payment = null
                error = "Unauthorized access"
                Log.w(
                    "CoSellerOrderDetail",
                    "Unauthorized payment access: user=$currentUserId paymentId=$paymentId sellerId=${fetchedPayment.sellerId}"
                )
            }
        }

        suspend fun resolveCurrentState() {
            when {
                currentSnapshot?.exists() == true -> applySnapshotState("payments", currentSnapshot)
                legacySnapshot?.exists() == true -> applySnapshotState("seller_payments", legacySnapshot)
                else -> {
                    payment = null
                    error = "Payment not found"
                    Log.w("CoSellerOrderDetail", "Payment not found in payments or seller_payments: $paymentId")
                }
            }
            isLoading = false
        }

        fun registerListener(collectionName: String, onSnapshotUpdate: (com.google.firebase.firestore.DocumentSnapshot?) -> Unit) =
            db.collection(collectionName)
                .document(paymentId)
                .addSnapshotListener { snapshot, firestoreError ->
                    if (firestoreError != null) {
                        Log.e("CoSellerOrderDetail", "Listener error in $collectionName", firestoreError)
                        payment = null
                        error = firestoreError.message
                        isLoading = false
                        return@addSnapshotListener
                    }

                    onSnapshotUpdate(snapshot)
                    coroutineScope.launch {
                        resolveCurrentState()
                    }
                }

        val paymentsListener = registerListener("payments") { currentSnapshot = it }
        val legacyListener = registerListener("seller_payments") { legacySnapshot = it }

        onDispose {
            paymentsListener.remove()
            legacyListener.remove()
            Log.d("CoSellerOrderDetail", "Listener removed for payment: $paymentId")
        }
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
                            text = "Order Payment Split",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = if (payment != null)
                                "Order #${payment!!.orderId.take(8).uppercase()}"
                            else "Loading…",
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
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }

            error != null -> OrderDetailErrorView(
                error = error!!,
                onBackClick = onBackClick,
                modifier = Modifier.padding(paddingValues)
            )

            payment != null -> {
                val p = payment!!

                // ── Refund eligibility for main seller only ───────────────────
                // Co-sellers (split-only) cannot initiate refunds — they don't own
                // the product. Only the main seller (sellerId == currentUserId) can.
                val isMainSeller = p.sellerId == currentUserId
                val isRefundEligible = isMainSeller &&
                        p.status.lowercase() in listOf("completed", "pending") &&
                        !p.status.lowercase().startsWith("refund")

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OrderAmountCard(p)
                    OrderInfoCard(p)
                    OrderItemsCard(p)
                    if (p.paymentSplits.isNotEmpty()) {
                        PaymentSplitCard(p, currentUserId)
                    }
                    OrderTimelineCard(p)

                    // ── Refund button — main seller only ──────────────────────
                    if (isRefundEligible) {
                        CoSellerInitiateRefundButton(
                            onRefund = { showRefundDialog = true }
                        )
                    } else if (!isMainSeller && p.paymentSplits.any { it.sellerId == currentUserId }) {
                        // Co-seller info notice
                        CoSellerRefundInfoNotice()
                    } else if (p.status.lowercase().startsWith("refund")) {
                        CoSellerRefundStatusNotice(p.status)
                    }

                    // Success / error notices
                    refundSuccessMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Success.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp, Success.copy(alpha = 0.30f)
                            ),
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
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    color = Success,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    refundErrorMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Error.copy(alpha = 0.06f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp, Error.copy(alpha = 0.25f)
                            ),
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
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    color = Error,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // ── Seller Refund Dialog ──────────────────────────────────────
                if (showRefundDialog) {
                    CoSellerRefundDialog(
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
                                    showRefundDialog = false
                                    refundSuccessMsg = "Refund request submitted. Admin will review and approve it."
                                } catch (e: Exception) {
                                    Log.e("CoSellerOrderDetail", "Seller refund failed", e)
                                    showRefundDialog = false
                                    refundErrorMsg   = e.message ?: "Failed to submit refund"
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

// ── Order Amount Hero Card ────────────────────────────────────────────────────

@Composable
private fun OrderAmountCard(payment: SellerPayment) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(statusColor.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = payment.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PKR ${String.format(java.util.Locale.US, "%,.0f", payment.amount)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Order #${payment.orderId.take(8).uppercase()}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            if (payment.status.lowercase() == "refunded" && payment.refundAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Error.copy(alpha = 0.07f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, Error.copy(alpha = 0.20f)
                    )
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

// ── Section Card helper ───────────────────────────────────────────────────────

@Composable
private fun SectionCard(
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
private fun InfoRow(label: String, icon: ImageVector, value: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
            Text(text = label, fontSize = 13.sp, color = TextSecondary)
        }
        value()
    }
}

// ── Order Info Card ───────────────────────────────────────────────────────────

@Composable
private fun OrderInfoCard(payment: SellerPayment) {
    SectionCard(title = "Order Information", icon = Icons.Default.Info) {
        InfoRow(label = "Buyer", icon = Icons.Default.Person) {
            RealtimeNameDisplay(
                userId = payment.buyerId,
                fallbackName = payment.buyerName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
        InfoRow(label = "Payment Method", icon = Icons.Default.CreditCard) {
            Text(
                text = payment.paymentMethod,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
        InfoRow(label = "Items", icon = Icons.Default.ShoppingCart) {
            Text(
                text = "${payment.itemsCount} item${if (payment.itemsCount > 1) "s" else ""}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
        InfoRow(label = "Shipping Fee", icon = Icons.Default.LocalShipping) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Included",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Text(
                    text = "Part of total amount",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
        InfoRow(label = "Date", icon = Icons.Default.DateRange) {
            Text(
                text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                    .format(java.util.Date(payment.getCreatedAtLong())),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        if (payment.refundReason.isNotEmpty()) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
            InfoRow(label = "Refund Reason", icon = Icons.Default.Info) {
                Text(
                    text = payment.refundReason,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Error
                )
            }
        }
    }
}

// ── Items Card ────────────────────────────────────────────────────────────────

@Composable
private fun OrderItemsCard(payment: SellerPayment) {
    if (payment.itemsDetails.isEmpty()) return
    SectionCard(title = "Items in This Order", icon = Icons.Default.Inventory) {
        payment.itemsDetails.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Qty: ${item.quantity}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = "PKR ${String.format(java.util.Locale.US, "%,.0f", item.itemTotal)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
            }
            if (index < payment.itemsDetails.size - 1) {
                HorizontalDivider(
                    color = BorderColor,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

// ── Payment Split Card ────────────────────────────────────────────────────────

@Composable
private fun PaymentSplitCard(payment: SellerPayment, currentUserId: String) {
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
                        listOf(
                            Color(0xFF7B1FA2).copy(alpha = 0.07f),
                            Color(0xFF7B1FA2).copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF7B1FA2).copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = Color(0xFF7B1FA2),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Earnings Split",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            payment.paymentSplits.forEach { split ->
                val isCurrentUser = split.sellerId == currentUserId
                val splitPct = split.splitPercentage
                val splitAmt = split.splitAmount

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCurrentUser) Primary.copy(alpha = 0.06f) else BackgroundSecondary,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isCurrentUser) Primary.copy(alpha = 0.18f) else BorderColor
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
                            RealtimeNameDisplay(
                                userId = split.sellerId,
                                fallbackName = split.sellerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${String.format("%.1f", splitPct * 100)}% share",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            if (isCurrentUser) {
                                Surface(
                                    color = Primary.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Your earnings",
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
                                text = "PKR ${String.format(java.util.Locale.US, "%,.0f", splitAmt)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentUser) Primary else TextPrimary
                            )
                            SplitStatusBadge(split.status)
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Total",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "PKR ${String.format(java.util.Locale.US, "%,.0f", payment.amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
            }
        }
    }
}

@Composable
private fun SplitStatusBadge(status: String) {
    // ✅ UNIFIED: Uses StateBadge with consistent 20dp pill shape
    val badgeState = when (status.lowercase()) {
        "completed"  -> BadgeState.SUCCESS
        "pending"    -> BadgeState.WARNING
        "processing" -> BadgeState.INFO
        "failed"     -> BadgeState.ERROR
        else         -> BadgeState.DEFAULT
    }
    StateBadge(
        label = status.replaceFirstChar { it.uppercase() },
        state = badgeState
    )
}

// ── Timeline Card ─────────────────────────────────────────────────────────────

@Composable
private fun OrderTimelineCard(payment: SellerPayment) {
    SectionCard(title = "Timeline", icon = Icons.Default.Timeline) {
        TimelineRow(label = "Order Placed", date = formatTs(payment.getCreatedAtLong()), done = true)
        if (payment.paymentDate != null) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineRow(label = "Payment Completed", date = formatTs(payment.paymentDate), done = true)
        }
        val refundTs = payment.getRefundDateLong()
        if (refundTs > 0L) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineRow(
                label = when (payment.status.lowercase()) {
                    "refunded"          -> "Refund Completed"
                    "refund_pending"    -> "Refund Requested"
                    "refund_processing" -> "Refund Processing"
                    "refund_rejected"   -> "Refund Rejected"
                    else                -> "Refund"
                },
                date = formatTs(refundTs),
                done = true
            )
        } else if (payment.paymentDate == null) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineRow(label = "Awaiting Payment", date = "Pending", done = false)
        }
    }
}

@Composable
private fun TimelineRow(label: String, date: String, done: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (done) Success else BorderColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = date, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

// ── Co-Seller Refund Button (main seller only) ────────────────────────────────

@Composable
private fun CoSellerInitiateRefundButton(onRefund: () -> Unit) {
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
        Text(
            text = "Initiate Refund for Buyer",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
    Text(
        text = "Seller-initiated refunds require admin approval. Refund splits will be reversed proportionally.",
        fontSize = 11.sp,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    )
}

// ── Co-Seller Info Notice (shown to split sellers, not main seller) ───────────

@Composable
private fun CoSellerRefundInfoNotice() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp, Color(0xFF2196F3).copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(16.dp).padding(top = 1.dp)
            )
            Text(
                text = "Refund requests can only be initiated by the main seller of this order. " +
                        "If a refund is approved, your split will be reversed automatically.",
                fontSize = 12.sp,
                color = Color(0xFF1565C0),
                lineHeight = 18.sp
            )
        }
    }
}

// ── Refund Status Notice ──────────────────────────────────────────────────────

@Composable
private fun CoSellerRefundStatusNotice(status: String) {
    val (bg, fg, msg) = when (status.lowercase()) {
        "refund_pending" -> Triple(
            Warning.copy(alpha = 0.08f),
            Warning,
            "A refund is pending admin review. Earnings split will be reversed if approved."
        )
        "refund_processing" -> Triple(
            Color(0xFF2196F3).copy(alpha = 0.08f),
            Color(0xFF2196F3),
            "Refund is being processed. Split earnings will be reversed."
        )
        "refunded" -> Triple(
            Color(0xFF9C27B0).copy(alpha = 0.08f),
            Color(0xFF9C27B0),
            "This payment has been fully refunded. Split earnings have been reversed."
        )
        "refund_rejected" -> Triple(
            Error.copy(alpha = 0.08f),
            Error,
            "The refund request was rejected. Earnings splits remain unchanged."
        )
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, fg.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
            Text(text = msg, fontSize = 12.sp, color = fg, lineHeight = 18.sp)
        }
    }
}

// ── Co-Seller Refund Dialog ───────────────────────────────────────────────────

@Composable
private fun CoSellerRefundDialog(
    payment: SellerPayment,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, reasonDetails: String) -> Unit
) {
    val reasons = listOf(
        "out_of_stock"    to "Out of Stock",
        "wrong_item_sent" to "Wrong Item Sent",
        "item_damaged"    to "Item Damaged Before Dispatch",
        "duplicate_order" to "Duplicate Order",
        "pricing_error"   to "Pricing Error",
        "other"           to "Other"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var reasonDetails  by remember { mutableStateOf("") }

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Amount + split info
                Surface(
                    color = BackgroundSecondary,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Refund Amount", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        if (payment.paymentSplits.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Split earnings will be reversed proportionally for all co-sellers.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Admin approval notice
                Surface(
                    color = Color(0xFF2196F3).copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, Color(0xFF2196F3).copy(alpha = 0.25f)
                    ),
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
                            modifier = Modifier
                                .size(15.dp)
                                .padding(top = 1.dp)
                        )
                        Text(
                            text = "Requires admin approval before processing.",
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

// ── Error View ────────────────────────────────────────────────────────────────

@Composable
private fun OrderDetailErrorView(
    error: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
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
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Error.copy(alpha = 0.60f),
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Unable to Load Payment",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = error, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .height(46.dp)
                .defaultMinSize(minWidth = 160.dp)
                .background(
                    Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                    RoundedCornerShape(12.dp)
                ),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Go Back",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTs(ts: Long?): String {
    if (ts == null) return "N/A"
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US)
        .format(java.util.Date(ts))
}
