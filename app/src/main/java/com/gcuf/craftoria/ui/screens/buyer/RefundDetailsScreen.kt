package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.RefundStatus
import com.gcuf.craftoria.data.model.getRequestedAtLong
import com.gcuf.craftoria.data.model.getApprovedAtLong
import com.gcuf.craftoria.data.model.getUpdatedAtLong
import com.gcuf.craftoria.data.model.getProcessedAtLong
import com.gcuf.craftoria.data.model.getCompletedAtLong
import com.gcuf.craftoria.ui.components.FilterTabRow
import com.gcuf.craftoria.ui.components.OrderDetailsDialog
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.formatDateTime
import com.gcuf.craftoria.viewmodel.RefundViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

// ✅ NEW: StatusDisplay data class to replace Tuple4
private data class StatusDisplay(
    val backgroundColor: Color,
    val textColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val statusText: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundDetailsScreen(
    refundId: String,
    onBackClick: () -> Unit,
    onContactSupport: () -> Unit,
    onViewOrderDetails: (String) -> Unit,
    viewModel: RefundViewModel = viewModel()
) {
    val refund by viewModel.getRefundByIdFlow(refundId).collectAsState(initial = null)
    val order by viewModel.getOrderForRefund(refund?.orderId ?: "").collectAsState(initial = null)
    var selectedTab by remember { mutableStateOf(0) }
    var showOrderDetailsDialog by remember { mutableStateOf(false) }
    var isInitialLoad by remember { mutableStateOf(true) }

    // ✅ Track initial load to prevent brief loading flash
    LaunchedEffect(refund) {
        if (refund != null && isInitialLoad) {
            isInitialLoad = false
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
                            text = "Refund Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        if (refund != null) {
                            Text(
                                text = "Order #${refund!!.orderId.take(8).uppercase()}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 13.sp
                            )
                        }
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
        // ✅ Only show loading on initial load, not on subsequent updates
        if (refund == null && isInitialLoad) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (refund != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ✅ Filter tabs — pill style, matching NotificationsScreen
                RefundDetailsTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                // Content area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Overview tab
                            RefundStatusCard(status = refund!!.status, refundAmount = refund!!.refundAmount)
                            RefundInfoSection(
                                title = "Order Information",
                                icon = Icons.Default.Receipt,
                                items = listOf(
                                    "Order ID" to "#${refund!!.orderId.take(8).uppercase()}",
                                    "Order Date" to (order?.createdAt?.let {
                                        formatDateTime(if (it is Long) it else System.currentTimeMillis())
                                    } ?: "N/A"),
                                    "Order Amount" to "PKR ${order?.totalPrice?.toInt() ?: 0}"
                                )
                            )
                            RefundInfoSection(
                                title = "Refund Information",
                                icon = Icons.Default.Info,
                                items = buildList {
                                    add("Refund Amount" to "PKR ${refund!!.refundAmount.toInt()}")
                                    add("Refund Type" to refund!!.refundType.replaceFirstChar { it.uppercase() })
                                    add("Reason" to refund!!.reason.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                                    if (refund!!.reasonDetails.isNotBlank()) {
                                        add("Description" to refund!!.reasonDetails)
                                    }
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showOrderDetailsDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.70f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("View Order", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = onContactSupport,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Contact Support",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Timeline tab
                            RefundTimeline(refund = refund!!)
                        }
                        2 -> {
                            // Breakdown tab
                            PaymentBreakdown(
                                originalAmount = order?.totalPrice ?: 0.0,
                                refundAmount = refund!!.refundAmount
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    // ── Order Details Dialog ──────────────────────────────────────────────────
    if (showOrderDetailsDialog && order != null) {
        OrderDetailsDialog(
            order = order!!,
            onDismiss = { showOrderDetailsDialog = false }
        )
    }
}

// ✅ Filter tabs using unified FilterTabRow component
@Composable
private fun RefundDetailsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Overview", "Timeline", "Breakdown")

    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            FilterTabRow(
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = onTabSelected,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        }
    }
}

// ── Detail Card helper — matches DetailCard / SectionCard from PaymentDetailScreen ──

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}

// ── Refund Status Card — matches PaymentStatusCard centered hero layout ────────

@Composable
private fun RefundStatusCard(status: String, refundAmount: Double) {
    val statusEnum = try {
        RefundStatus.valueOf(status.uppercase())
    } catch (e: Exception) {
        RefundStatus.REQUESTED
    }

    val display = when (statusEnum) {
        RefundStatus.REQUESTED, RefundStatus.UNDER_REVIEW -> StatusDisplay(
            Warning,
            Color.White,
            Icons.Default.Schedule,
            if (statusEnum == RefundStatus.UNDER_REVIEW) "Under Review" else "Refund Requested"
        )
        RefundStatus.APPROVED_BY_SELLER, RefundStatus.APPROVED_BY_ADMIN -> StatusDisplay(
            Color(0xFF2196F3),
            Color.White,
            Icons.Default.CheckCircle,
            "Refund Approved"
        )
        RefundStatus.PROCESSING -> StatusDisplay(
            Color(0xFF2196F3),
            Color.White,
            Icons.Default.Sync,
            "Refund Processing"
        )
        RefundStatus.COMPLETED -> StatusDisplay(
            Success,
            Color.White,
            Icons.Default.CheckCircle,
            "Refund Completed"
        )
        RefundStatus.REJECTED_BY_SELLER, RefundStatus.REJECTED_BY_ADMIN -> StatusDisplay(
            Error,
            Color.White,
            Icons.Default.Cancel,
            "Refund Rejected"
        )
        RefundStatus.FAILED -> StatusDisplay(
            Error,
            Color.White,
            Icons.Default.Error,
            "Refund Failed"
        )
        RefundStatus.CANCELLED -> StatusDisplay(
            TextSecondary,
            Color.White,
            Icons.Default.Cancel,
            "Refund Cancelled"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(display.backgroundColor.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = display.icon,
                    contentDescription = null,
                    tint = display.backgroundColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = display.statusText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = display.backgroundColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PKR ${String.format(java.util.Locale.US, "%.0f", refundAmount)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

// ── Info Section — matches PaymentInfoSection row pattern ─────────────────────

@Composable
private fun RefundInfoSection(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, String>>
) {
    DetailCard(title = title, icon = icon) {
        items.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            if (index < items.size - 1) {
                HorizontalDivider(
                    color = BorderColor,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ── Timeline — matches OrderTimelineCard dot-row pattern ──────────────────────

@Composable
private fun RefundTimeline(refund: com.gcuf.craftoria.data.model.RefundRequest) {
    val statusEnum = try {
        RefundStatus.valueOf(refund.status.uppercase())
    } catch (e: Exception) {
        RefundStatus.REQUESTED
    }

    DetailCard(title = "Timeline", icon = Icons.Default.Timeline) {
        TimelineRow(
            title = "Requested",
            date = formatDateTime(refund.getRequestedAtLong()),
            subtitle = "By: You",
            isCompleted = true
        )

        when (statusEnum) {
            RefundStatus.APPROVED_BY_SELLER -> {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                TimelineRow(
                    title = "Approved by Seller",
                    date = formatDateTime(refund.getApprovedAtLong()),
                    subtitle = refund.approvalNotes.takeIf { it.isNotBlank() }?.let { "Note: $it" },
                    isCompleted = true
                )
            }
            RefundStatus.APPROVED_BY_ADMIN -> {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                TimelineRow(
                    title = "Approved by Admin",
                    date = formatDateTime(refund.getApprovedAtLong()),
                    subtitle = refund.approvalNotes.takeIf { it.isNotBlank() }?.let { "Note: $it" },
                    isCompleted = true
                )
            }
            RefundStatus.REJECTED_BY_SELLER, RefundStatus.REJECTED_BY_ADMIN -> {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                TimelineRow(
                    title = if (statusEnum == RefundStatus.REJECTED_BY_SELLER) "Rejected by Seller" else "Rejected by Admin",
                    date = formatDateTime(refund.getUpdatedAtLong()),
                    isCompleted = true,
                    isError = true
                )
            }
            else -> {}
        }

        if (statusEnum == RefundStatus.PROCESSING || statusEnum == RefundStatus.COMPLETED) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineRow(
                title = "Processing Started",
                date = formatDateTime(refund.getProcessedAtLong()),
                isCompleted = true
            )
        }

        if (statusEnum == RefundStatus.COMPLETED) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineRow(
                title = "Refund Completed",
                date = formatDateTime(refund.getCompletedAtLong()),
                subtitle = "Amount: PKR ${refund.refundAmount.toInt()} · Original Payment Method",
                isCompleted = true
            )
        }

        if (statusEnum == RefundStatus.FAILED) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
            TimelineRow(
                title = "Refund Failed",
                date = formatDateTime(refund.getUpdatedAtLong()),
                subtitle = refund.errorMessage.takeIf { it.isNotBlank() },
                isCompleted = true,
                isError = true
            )
        }
    }
}

// ── Timeline Row — matches TimelineRow / TimelineItemRow dot style ─────────────

@Composable
private fun TimelineRow(
    title: String,
    date: String?,
    subtitle: String? = null,
    isCompleted: Boolean = false,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    when {
                        isError -> Error
                        isCompleted -> Success
                        else -> BorderColor
                    },
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (date != null) {
                Text(
                    text = date,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

// ── Payment Breakdown — matches PaymentItemsSection row pattern ───────────────

@Composable
private fun PaymentBreakdown(
    originalAmount: Double,
    refundAmount: Double
) {
    DetailCard(title = "Payment Breakdown", icon = Icons.Default.AccountBalance) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Original Payment", fontSize = 12.sp, color = TextSecondary)
            Text(
                text = "PKR ${originalAmount.toInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Refund Amount", fontSize = 12.sp, color = TextSecondary)
            Text(
                text = "PKR ${refundAmount.toInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Processing Fee", fontSize = 12.sp, color = TextSecondary)
            Text(
                text = "PKR 0",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Net Refund",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "PKR ${refundAmount.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Success
            )
        }
    }
}