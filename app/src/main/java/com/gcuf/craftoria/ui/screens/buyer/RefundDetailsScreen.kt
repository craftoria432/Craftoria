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
        if (refund == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Overview tab
                            RefundStatusBanner(status = refund!!.status)
                            InfoSection(
                                title = "Order Information",
                                items = listOf(
                                    "Order ID" to "#${refund!!.orderId.takeLast(8).uppercase()}",
                                    "Order Date" to (order?.createdAt?.let {
                                        formatDateTime(if (it is Long) it else System.currentTimeMillis())
                                    } ?: "N/A"),
                                    "Order Amount" to "PKR ${order?.totalPrice?.toInt() ?: 0}"
                                )
                            )
                            InfoSection(
                                title = "Refund Information",
                                items = listOf(
                                    "Refund Amount" to "PKR ${refund!!.refundAmount.toInt()}",
                                    "Refund Type" to refund!!.refundType,
                                    "Reason" to refund!!.reason,
                                    "Description" to (refund!!.reasonDetails.takeIf { it.isNotBlank() } ?: "N/A")
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onViewOrderDetails(refund!!.orderId) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("View Order")
                                }
                                Button(
                                    onClick = onContactSupport,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Support")
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ✅ NEW: Filter tabs using unified FilterTabRow component
@Composable
private fun RefundDetailsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Overview", "Timeline", "Breakdown")

    // White surface with 0.5.dp bottom divider — consistent with NotificationFilterTabs
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

@Composable
private fun RefundStatusBanner(status: String) {
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
        colors = CardDefaults.cardColors(containerColor = display.backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = display.icon,
                contentDescription = null,
                tint = display.textColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = display.statusText,
                color = display.textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = value,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RefundTimeline(refund: com.gcuf.craftoria.data.model.RefundRequest) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Timeline",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Requested
            TimelineItem(
                icon = Icons.Default.Schedule,
                title = "Requested",
                timestamp = formatDateTime(refund.getRequestedAtLong()),
                description = "By: You",
                isCompleted = true
            )

            // Approval/Rejection
            val statusEnum = try {
                RefundStatus.valueOf(refund.status.uppercase())
            } catch (e: Exception) {
                RefundStatus.REQUESTED
            }
            
            when (statusEnum) {
                RefundStatus.APPROVED_BY_SELLER -> {
                    TimelineItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Approved by Seller",
                        timestamp = formatDateTime(refund.getApprovedAtLong()),
                        description = refund.approvalNotes.takeIf { it.isNotBlank() }?.let { "Note: $it" },
                        isCompleted = true
                    )
                }
                RefundStatus.APPROVED_BY_ADMIN -> {
                    TimelineItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Approved by Admin",
                        timestamp = formatDateTime(refund.getApprovedAtLong()),
                        description = refund.approvalNotes.takeIf { it.isNotBlank() }?.let { "Note: $it" },
                        isCompleted = true
                    )
                }
                RefundStatus.REJECTED_BY_SELLER, RefundStatus.REJECTED_BY_ADMIN -> {
                    TimelineItem(
                        icon = Icons.Default.Cancel,
                        title = if (statusEnum == RefundStatus.REJECTED_BY_SELLER) "Rejected by Seller" else "Rejected by Admin",
                        timestamp = formatDateTime(refund.getUpdatedAtLong()),
                        description = null,
                        isCompleted = true,
                        isError = true
                    )
                }
                else -> {}
            }

            // Processing
            if (statusEnum == RefundStatus.PROCESSING || statusEnum == RefundStatus.COMPLETED) {
                TimelineItem(
                    icon = Icons.Default.Sync,
                    title = "Processing Started",
                    timestamp = formatDateTime(refund.getProcessedAtLong()),
                    isCompleted = true
                )
            }

            // Completed
            if (statusEnum == RefundStatus.COMPLETED) {
                TimelineItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Refund Completed",
                    timestamp = formatDateTime(refund.getCompletedAtLong()),
                    description = "Amount: PKR ${refund.refundAmount.toInt()}\nMethod: Original Payment Method",
                    isCompleted = true
                )
            }

            // Failed
            if (statusEnum == RefundStatus.FAILED) {
                TimelineItem(
                    icon = Icons.Default.Error,
                    title = "Refund Failed",
                    timestamp = formatDateTime(refund.getUpdatedAtLong()),
                    description = refund.errorMessage.takeIf { it.isNotBlank() },
                    isCompleted = true,
                    isError = true
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    timestamp: String?,
    description: String? = null,
    isCompleted: Boolean = false,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                isError -> Error
                isCompleted -> Success
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            if (timestamp != null) {
                Text(
                    text = timestamp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PaymentBreakdown(
    originalAmount: Double,
    refundAmount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Payment Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Original Payment",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "PKR ${originalAmount.toInt()}",
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Refund Amount",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "PKR ${refundAmount.toInt()}",
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Processing Fee",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "PKR 0",
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Net Refund",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "PKR ${refundAmount.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Success
                )
            }
        }
    }
}
