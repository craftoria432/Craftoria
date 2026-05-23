package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.getBuyerDisplayDate
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.BuyerPaymentStats
import com.gcuf.craftoria.viewmodel.BuyerPaymentStatsUiState
import com.gcuf.craftoria.viewmodel.BuyerPaymentUiState
import com.gcuf.craftoria.viewmodel.BuyerPaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    buyerId: String,
    onBackClick: () -> Unit,
    viewModel: BuyerPaymentViewModel = viewModel()
) {
    val paymentState  by viewModel.paymentState.collectAsState()
    val statsState    by viewModel.statsState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    LaunchedEffect(buyerId) {
        viewModel.loadBuyerPayments(buyerId)
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
                        Text("Payment History", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = Color.White, lineHeight = 18.sp)
                        Text("Your purchase history", fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f), lineHeight = 13.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(
                            modifier = Modifier.size(36.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                                tint = Color.White, modifier = Modifier.size(18.dp))
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // ── Main content ──────────────────────────────────────────────────
            // ✅ INSTANT LOADING: Show content immediately on cache hit,
            // only show Loading if fetch takes >500ms on cold start
            when (val p = paymentState) {
                is BuyerPaymentUiState.Loading -> {
                    // Only show loading if data is truly unavailable
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                is BuyerPaymentUiState.Success -> {
                    // ── Stats card ────────────────────────────────────────────────────
                    when (val s = statsState) {
                        is BuyerPaymentStatsUiState.Loading -> {
                            // Render nothing — stats will appear when ready
                        }
                        is BuyerPaymentStatsUiState.Success -> BuyerPaymentStatsCards(s.stats)
                        is BuyerPaymentStatsUiState.Error   -> { /* omit stats on error */ }
                    }

                    // ── Filter tabs ───────────────────────────────────────────────────
                    BuyerPaymentFilterTabs(
                        selectedStatus  = selectedStatus,
                        onFilterSelected = { status ->
                            if (status == null) viewModel.clearFilters()
                            else viewModel.setStatusFilter(status)
                        },
                        payments = p.payments
                    )

                    // ── Payment list ──────────────────────────────────────────────────
                    val filtered = viewModel.getFilteredPayments(p.payments)
                    if (filtered.isEmpty()) {
                        BuyerEmptyPaymentsState(
                            hasFilter  = selectedStatus != null,
                            filterName = selectedStatus?.getDisplayName() ?: ""
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { payment ->
                                BuyerPaymentCard(payment = payment)
                            }
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                        }
                    }
                }
                is BuyerPaymentUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(80.dp)
                                    .background(Error.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Warning, null,
                                    tint = Error.copy(alpha = 0.60f),
                                    modifier = Modifier.size(38.dp))
                            }
                            Text(p.message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Stats card ────────────────────────────────────────────────────────────────

@Composable
private fun BuyerPaymentStatsCards(stats: BuyerPaymentStats) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            shape     = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            border    = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(
                            listOf(Primary.copy(alpha = 0.06f), Primary.copy(alpha = 0.02f))))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp)
                            .background(Primary.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AttachMoney, null, tint = Primary,
                            modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Total Spent", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            "PKR ${String.format(java.util.Locale.US, "%.0f", stats.totalSpent)}",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary
                        )
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuyerStatMiniCard("Completed",
                            "PKR ${String.format(java.util.Locale.US, "%.0f", stats.completedAmount)}",
                            Icons.Default.CheckCircle, Color(0xFFE8F5E9), Color(0xFF4CAF50), Modifier.weight(1f))
                        BuyerStatMiniCard("Pending",
                            "PKR ${String.format(java.util.Locale.US, "%.0f", stats.pendingAmount)}",
                            Icons.Default.Schedule, Color(0xFFFFF3E0), Warning, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuyerStatMiniCard("Payments", "${stats.totalPayments}",
                            Icons.Default.Receipt, Color(0xFFE3F2FD), Color(0xFF2196F3), Modifier.weight(1f))
                        BuyerStatMiniCard("Sellers", "${stats.totalSellers}",
                            Icons.Default.Store, Color(0xFFF3E5F5), Color(0xFF9C27B0), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyerStatMiniCard(
    title: String, amount: String, icon: ImageVector,
    backgroundColor: Color, textColor: Color, modifier: Modifier = Modifier
) {
    Surface(shape = RoundedCornerShape(8.dp), color = backgroundColor, modifier = modifier) {
        Row(modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, fontSize = 10.sp, color = textColor.copy(alpha = 0.80f),
                    fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(3.dp))
                Text(amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            Icon(icon, title, tint = textColor.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
        }
    }
}

// ── Payment card ──────────────────────────────────────────────────────────────

@Composable
private fun BuyerPaymentCard(payment: SellerPayment) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(BackgroundSecondary)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Order #${payment.orderId.take(8).uppercase()}",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    RealtimeNameDisplay(userId = payment.sellerId, fallbackName = payment.sellerName,
                        fontSize = 11.sp, color = TextSecondary,
                        modifier = Modifier.padding(top = 1.dp))
                }
                BuyerPaymentStatusBadge(payment.status)
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(bottom = 7.dp)) {
                    Icon(Icons.Default.ShoppingCart, null, tint = TextSecondary,
                        modifier = Modifier.size(13.dp))
                    Text("${payment.itemsCount} item(s)", fontSize = 11.sp, color = TextSecondary)
                }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text(formatPaymentDate(payment.getBuyerDisplayDate()),
                        fontSize = 11.sp, color = TextSecondary)
                }

                // Refund status row
                val st = payment.status.lowercase()
                when {
                    st == "refunded" && payment.refundAmount > 0 ->
                        RefundInfoRow(Icons.AutoMirrored.Filled.Undo, Color(0xFF9C27B0),
                            "Refunded: PKR ${String.format(java.util.Locale.US, "%.0f", payment.refundAmount)}")
                    st == "refund_processing" ->
                        RefundInfoRow(Icons.Default.Schedule, Color(0xFF2196F3),
                            if (payment.refundAmount > 0)
                                "Refund Processing: PKR ${String.format(java.util.Locale.US, "%.0f", payment.refundAmount)}"
                            else "Refund Processing")
                    st == "refund_pending" ->
                        RefundInfoRow(Icons.Default.Schedule, Warning,
                            if (payment.refundAmount > 0)
                                "Refund Pending: PKR ${String.format(java.util.Locale.US, "%.0f", payment.refundAmount)}"
                            else "Refund Pending")
                    st == "refund_rejected" ->
                        RefundInfoRow(Icons.Default.Error, Color(0xFF757575), "Refund Rejected")
                }

                if (payment.paymentMethod.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 6.dp)) {
                        Icon(Icons.Default.Payment, null, tint = TextSecondary,
                            modifier = Modifier.size(13.dp))
                        Text(payment.paymentMethod, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RefundInfoRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 6.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
        Text(text, fontSize = 11.sp, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
private fun BuyerPaymentStatusBadge(status: String) {
    val (bg, fg, label) = when (status.lowercase()) {
        "completed"         -> Triple(Success.copy(alpha = 0.10f),           Success,           "Completed")
        "pending"           -> Triple(Warning.copy(alpha = 0.15f),           Warning,           "Pending")
        "processing"        -> Triple(Color(0xFF2196F3).copy(alpha = 0.10f), Color(0xFF2196F3), "Processing")
        "failed"            -> Triple(Error.copy(alpha = 0.10f),             Error,             "Failed")
        "refund_pending"    -> Triple(Warning.copy(alpha = 0.15f),           Warning,           "Refund Pending")
        "refund_processing" -> Triple(Color(0xFF2196F3).copy(alpha = 0.10f), Color(0xFF2196F3), "Refund Processing")
        "refunded"          -> Triple(Color(0xFF9C27B0).copy(alpha = 0.10f), Color(0xFF9C27B0), "Refunded")
        "refund_rejected"   -> Triple(Color(0xFF757575).copy(alpha = 0.10f), Color(0xFF757575), "Refund Rejected")
        else                -> Triple(BorderColor, TextSecondary,
            status.replaceFirstChar { it.uppercase() })
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

// ── Filter tabs ───────────────────────────────────────────────────────────────

@Composable
private fun BuyerPaymentFilterTabs(
    selectedStatus: PaymentStatus?,
    onFilterSelected: (PaymentStatus?) -> Unit,
    payments: List<SellerPayment>
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ ALL tab — always shown
            FilterTab("All", selectedStatus == null) { onFilterSelected(null) }

            // ✅ All payment statuses — ALWAYS shown regardless of data
            // This ensures consistent UI and helps buyers understand all possible states
            PaymentStatus.entries.forEach { status ->
                FilterTab(
                    label    = status.getDisplayName(),
                    selected = selectedStatus == status,
                    onClick  = { onFilterSelected(status) }
                )
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(20.dp),
        color   = if (selected) Primary else Color.White,
        border  = androidx.compose.foundation.BorderStroke(
            width = if (selected) 0.dp else 0.5.dp,
            color = if (selected) Primary else BorderColor
        ),
        modifier = Modifier.height(34.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (selected) Color.White else TextSecondary,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun BuyerEmptyPaymentsState(hasFilter: Boolean = false, filterName: String = "") {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(88.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (hasFilter) Icons.Default.FilterList else Icons.Default.Receipt,
                null, tint = Primary.copy(alpha = 0.50f), modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            if (hasFilter) "No Payments Found" else "No Payments Yet",
            fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (hasFilter) "No payments match the filter: $filterName"
            else "Your payment history will appear here",
            fontSize = 14.sp, color = TextSecondary
        )
    }
}

private fun formatPaymentDate(timestamp: Long): String =
    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
        .format(java.util.Date(timestamp))