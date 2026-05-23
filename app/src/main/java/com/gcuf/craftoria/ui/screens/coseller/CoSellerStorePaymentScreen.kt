package com.gcuf.craftoria.ui.screens.coseller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.repository.StoreRevenueSummary
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Error
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.ui.theme.Success
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary
import com.gcuf.craftoria.ui.theme.Warning
import com.gcuf.craftoria.viewmodel.CoSellerPaymentDateRange
import com.gcuf.craftoria.viewmodel.CoSellerPaymentUiState
import com.gcuf.craftoria.viewmodel.CoSellerStorePaymentViewModel
import com.gcuf.craftoria.viewmodel.StoreRevenueUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoSellerStorePaymentScreen(
    storeId: String,
    storeName: String,
    onBackClick: () -> Unit = {},
    onPaymentClick: (String) -> Unit = {},
    showHeader: Boolean = true,
    viewModel: CoSellerStorePaymentViewModel = viewModel()
) {
    val paymentState by viewModel.paymentState.collectAsState()
    val revenueState by viewModel.storeRevenueState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()

    LaunchedEffect(storeId) {
        viewModel.loadStorePayments(storeId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showHeader) BackgroundSecondary else Color.Transparent)
    ) {
        if (showHeader) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = storeName.take(1).uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = storeName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Payment Dashboard",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                when (revenueState) {
                    is StoreRevenueUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is StoreRevenueUiState.Success -> {
                        val summary = (revenueState as StoreRevenueUiState.Success).summary
                        StoreRevenueSummaryCards(
                            summary = summary,
                            rangeLabel = selectedDateRange.displayName,
                            showHeader = true
                        )
                    }
                    is StoreRevenueUiState.Error -> Unit
                }
            }
        } else {
            when (revenueState) {
                is StoreRevenueUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is StoreRevenueUiState.Success -> {
                    val summary = (revenueState as StoreRevenueUiState.Success).summary
                    StoreRevenueSummaryCards(
                        summary = summary,
                        rangeLabel = selectedDateRange.displayName,
                        showHeader = false
                    )
                }
                is StoreRevenueUiState.Error -> Unit
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            CoSellerDateRangeSelector(
                selectedDateRange = selectedDateRange,
                onDateRangeSelected = viewModel::setDateRange
            )

            CoSellerFilterTabs(
                selectedStatus = selectedStatus,
                onFilterSelected = viewModel::filterByStatus
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            when (paymentState) {
                is CoSellerPaymentUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is CoSellerPaymentUiState.Success -> {
                    val filteredPayments = viewModel.getFilteredPayments()
                    PaymentsSectionHeader(
                        rangeLabel = selectedDateRange.displayName,
                        paymentCount = filteredPayments.size
                    )

                    if (filteredPayments.isEmpty()) {
                        CoSellerEmptyPaymentsState(rangeLabel = selectedDateRange.displayName)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredPayments) { payment ->
                                CoSellerPaymentCard(
                                    payment = payment,
                                    viewModel = viewModel,
                                    onClick = { onPaymentClick(payment.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }
                    }
                }
                is CoSellerPaymentUiState.Error -> {
                    CoSellerErrorState(
                        errorMessage = (paymentState as CoSellerPaymentUiState.Error).message,
                        onRetry = { viewModel.loadStorePayments(storeId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreRevenueSummaryCards(
    summary: StoreRevenueSummary,
    rangeLabel: String,
    showHeader: Boolean = true
) {
    val bgModifier = if (showHeader) {
        Modifier.background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
    } else {
        Modifier.background(BackgroundSecondary)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(bgModifier)
            .padding(horizontal = 16.dp)
            .padding(top = 0.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (showHeader) Color.White.copy(alpha = 0.18f) else Color.White,
            border = if (!showHeader) androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Revenue · $rangeLabel",
                        fontSize = 12.sp,
                        color = if (showHeader) Color.White.copy(alpha = 0.80f) else TextSecondary
                    )
                    Text(
                        text = "PKR ${String.format("%,.2f", summary.totalRevenue)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showHeader) Color.White else Primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (showHeader) Color.White.copy(alpha = 0.20f) else Primary.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (showHeader) Color.White else Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(
                Triple("Completed", "PKR ${String.format("%,.0f", summary.completedRevenue)}", Success),
                Triple("Pending", "PKR ${String.format("%,.0f", summary.pendingRevenue)}", Warning),
                Triple("Orders", summary.orderCount.toString(), if (showHeader) Color.White else Primary)
            ).forEach { (label, value, valueColor) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (showHeader) Color.White.copy(alpha = 0.15f) else Color.White,
                    border = if (!showHeader) androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor) else null,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(9.dp)) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            color = if (showHeader) Color.White.copy(alpha = 0.75f) else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = value,
                            fontSize = if (label == "Orders") 16.sp else 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showHeader) Color.White else valueColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoSellerDateRangeSelector(
    selectedDateRange: CoSellerPaymentDateRange,
    onDateRangeSelected: (CoSellerPaymentDateRange) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Time Range",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoSellerPaymentDateRange.entries.forEach { range ->
                val isSelected = selectedDateRange == range
                Surface(
                    onClick = { onDateRangeSelected(range) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Primary else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 0.dp else 0.5.dp,
                        color = if (isSelected) Primary else BorderColor
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = range.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoSellerFilterTabs(selectedStatus: String, onFilterSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("all" to "All", "pending" to "Pending", "completed" to "Completed").forEach { (key, label) ->
            val isSelected = selectedStatus == key
            Surface(
                onClick = { onFilterSelected(key) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Primary else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 0.dp else 0.5.dp,
                    color = if (isSelected) Primary else BorderColor
                ),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentsSectionHeader(rangeLabel: String, paymentCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Payments · $rangeLabel",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Showing $paymentCount payment${if (paymentCount == 1) "" else "s"}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CoSellerPaymentCard(
    payment: SellerPayment,
    viewModel: CoSellerStorePaymentViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundSecondary)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${payment.orderId.take(8).uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${formatDate(payment.getCreatedAtLong())} · ${payment.itemsCount} item${if (payment.itemsCount > 1) "s" else ""}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                CoSellerStatusBadge(viewModel.getStatusDisplayName(payment.status), payment.status)
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (payment.status.lowercase() == "refunded") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Error.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Error.copy(alpha = 0.20f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "This payment was refunded. Splits reversed.",
                                fontSize = 11.sp,
                                color = Error
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Total Amount", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = "PKR ${String.format("%,.2f", payment.amount)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Buyer", fontSize = 10.sp, color = TextSecondary)
                        RealtimeNameDisplay(
                            userId = payment.buyerId,
                            fallbackName = payment.buyerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }

                if (payment.paymentSplits.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BackgroundSecondary,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PAYMENT SPLIT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                letterSpacing = 0.4.sp
                            )
                            payment.paymentSplits.forEach { split ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(Primary, CircleShape)
                                        )
                                        RealtimeNameDisplay(
                                            userId = split.sellerId,
                                            fallbackName = split.sellerName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "(${(split.splitPercentage * 100).toInt()}%)",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        text = "PKR ${String.format("%,.2f", split.splitAmount)}",
                                        fontSize = 11.sp,
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
    }
}

@Composable
private fun CoSellerStatusBadge(label: String, status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "completed" -> Success.copy(alpha = 0.10f) to Success
        "pending" -> Warning.copy(alpha = 0.15f) to Warning
        "processing" -> Color(0xFF2196F3).copy(alpha = 0.10f) to Color(0xFF2196F3)
        "failed" -> Error.copy(alpha = 0.10f) to Error
        "refunded" -> Color(0xFF9C27B0).copy(alpha = 0.10f) to Color(0xFF9C27B0)
        "refund_pending" -> Warning.copy(alpha = 0.15f) to Warning
        "refund_processing" -> Color(0xFF2196F3).copy(alpha = 0.10f) to Color(0xFF2196F3)
        "refund_rejected" -> Color(0xFF757575).copy(alpha = 0.10f) to Color(0xFF757575)
        else -> BackgroundSecondary to TextSecondary
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CoSellerEmptyPaymentsState(rangeLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSecondary)
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Professional empty state icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.60f),
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main heading
        Text(
            text = "No Payments Found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description text
        Text(
            text = "No payments found for the $rangeLabel date range.\n\nTry selecting a different date range to view your earnings.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun CoSellerErrorState(errorMessage: String, onRetry: () -> Unit) {
    val isIndexError = errorMessage.contains("FAILED_PRECONDITION") ||
        errorMessage.contains("index") ||
        errorMessage.contains("composite")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Error.copy(alpha = 0.50f),
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Unable to load payments",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isIndexError) {
                "Database indexes are being created.\nThis may take a few minutes."
            } else {
                errorMessage
            },
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .height(40.dp)
                .background(
                    Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                    RoundedCornerShape(10.dp)
                )
        ) {
            Text(
                text = "Retry",
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        "Unknown date"
    }
}
