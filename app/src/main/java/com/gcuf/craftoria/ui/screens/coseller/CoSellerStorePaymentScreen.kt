package com.gcuf.craftoria.ui.screens.coseller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.CoSellerPaymentUiState
import com.gcuf.craftoria.viewmodel.CoSellerStorePaymentViewModel
import com.gcuf.craftoria.viewmodel.StoreRevenueUiState
import java.text.SimpleDateFormat
import java.util.*

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

    LaunchedEffect(storeId) {
        viewModel.loadStorePayments(storeId)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.timeInMillis
        val endDate = System.currentTimeMillis()
        viewModel.loadStoreRevenue(storeId, startDate, endDate)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showHeader) BackgroundSecondary else Color.Transparent)
    ) {
        // ── Gradient header + revenue cards ──────────────────────────────────
        if (showHeader) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            ) {
                // Store identity row
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
                        Text(text = storeName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Payment Dashboard", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                }

                when (revenueState) {
                    is StoreRevenueUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is StoreRevenueUiState.Success -> {
                        val summary = (revenueState as StoreRevenueUiState.Success).summary
                        StoreRevenueSummaryCards(summary, showHeader = true)
                    }
                    is StoreRevenueUiState.Error -> { /* silent fail */ }
                }
            }
        } else {
            when (revenueState) {
                is StoreRevenueUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is StoreRevenueUiState.Success -> {
                    val summary = (revenueState as StoreRevenueUiState.Success).summary
                    StoreRevenueSummaryCards(summary, showHeader = false)
                }
                is StoreRevenueUiState.Error -> { /* silent fail */ }
            }
        }

        // ── White content panel ───────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            CoSellerFilterTabs(
                selectedStatus = selectedStatus,
                onFilterSelected = { viewModel.filterByStatus(it) }
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
                    if (filteredPayments.isEmpty()) {
                        CoSellerEmptyPaymentsState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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

// ── Revenue Summary Cards ─────────────────────────────────────────────────────

@Composable
private fun StoreRevenueSummaryCards(
    summary: com.gcuf.craftoria.data.repository.StoreRevenueSummary,
    showHeader: Boolean = true
) {
    val bgModifier = if (showHeader)
        Modifier.background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
    else
        Modifier.background(BackgroundSecondary)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(bgModifier)
            .padding(horizontal = 14.dp)
            .padding(top = 0.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero total card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (showHeader) Color.White.copy(alpha = 0.18f) else Color.White,
            border = if (!showHeader) androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Revenue",
                        fontSize = 11.sp,
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
                        .size(34.dp)
                        .background(
                            if (showHeader) Color.White.copy(alpha = 0.20f)
                            else Primary.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (showHeader) Color.White else Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Three mini stats
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

// ── Filter Tabs ───────────────────────────────────────────────────────────────

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

// ── Payment Card ──────────────────────────────────────────────────────────────

@Composable
private fun CoSellerPaymentCard(
    payment: SellerPayment,
    viewModel: CoSellerStorePaymentViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column {
            // BackgroundSecondary header band
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
                        text = "${formatDate(payment.createdAt)} · ${payment.itemsCount} item${if (payment.itemsCount > 1) "s" else ""}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Status badge — theme tokens replacing hardcoded hex
                CoSellerStatusBadge(viewModel.getStatusDisplayName(payment.status), payment.status)
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                        Box(modifier = Modifier.size(7.dp).background(Primary, CircleShape))
                                        RealtimeNameDisplay(
                                            userId = split.sellerId,
                                            fallbackName = split.sellerName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Text(text = "(${(split.splitPercentage * 100).toInt()}%)", fontSize = 10.sp, color = TextSecondary)
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

// ── Status Badge — theme tokens ───────────────────────────────────────────────

@Composable
private fun CoSellerStatusBadge(label: String, status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "completed"  -> Success.copy(alpha = 0.10f) to Success
        "pending"    -> Warning.copy(alpha = 0.15f) to Warning
        "processing" -> Color(0xFF2196F3).copy(alpha = 0.10f) to Color(0xFF2196F3)
        "failed"     -> Error.copy(alpha = 0.10f) to Error
        else         -> BackgroundSecondary to TextSecondary
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

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun CoSellerEmptyPaymentsState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = Primary.copy(alpha = 0.50f), modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = "No payments found", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Payments will appear here once orders are placed",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

// ── Error State ───────────────────────────────────────────────────────────────

@Composable
private fun CoSellerErrorState(errorMessage: String, onRetry: () -> Unit) {
    val isIndexError = errorMessage.contains("FAILED_PRECONDITION") ||
            errorMessage.contains("index") ||
            errorMessage.contains("composite")

    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Error.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Error.copy(alpha = 0.50f), modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = "Unable to load payments", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isIndexError)
                "Database indexes are being created.\nThis may take a few minutes."
            else errorMessage,
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
            Text("Retry", fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

// ── Date helper ───────────────────────────────────────────────────────────────

private fun formatDate(timestamp: Long): String {
    return try {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    } catch (e: Exception) {
        "Unknown date"
    }
}