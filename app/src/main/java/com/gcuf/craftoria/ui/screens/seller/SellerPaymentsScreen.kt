package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.getDisplayDate
import com.gcuf.craftoria.ui.components.EmptyStates
import com.gcuf.craftoria.ui.components.PaymentStatusBadge
import com.gcuf.craftoria.ui.components.PaymentStatusFilterTabs
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.PaymentStatsUiState
import com.gcuf.craftoria.viewmodel.PaymentUiState
import com.gcuf.craftoria.viewmodel.SellerPaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerPaymentsScreen(
    sellerId: String,
    onBackClick: () -> Unit,
    onPaymentClick: (String) -> Unit = {},
    viewModel: SellerPaymentViewModel = viewModel()
) {
    val paymentState by viewModel.paymentState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    LaunchedEffect(sellerId) {
        viewModel.loadSellerPayments(sellerId)
        viewModel.loadPaymentStats(sellerId)
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
                            text = "Payment History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Your earnings overview",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            if (paymentState !is PaymentUiState.Error) {
                PaymentStatusFilterTabs(
                    selectedStatus = selectedStatus,
                    onFilterSelected = { status ->
                        if (status == null) viewModel.clearFilters()
                        else viewModel.setStatusFilter(status)
                    }
                )
            }

            when (val p = paymentState) {
                is PaymentUiState.Loading -> {
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
                is PaymentUiState.Success -> {
                    // ── Stats card ────────────────────────────────────────────────────
                    when (val s = statsState) {
                        is PaymentStatsUiState.Loading -> { /* stats load in background */ }
                        is PaymentStatsUiState.Success -> PaymentStatsCards(s.stats)
                        is PaymentStatsUiState.Error   -> { /* omit stats on error */ }
                        is PaymentStatsUiState.Idle -> { /* no-op */ }
                    }

                    // ── Payment list ──────────────────────────────────────────────────
                    val filtered = viewModel.getFilteredPayments(p.payments)
                    if (filtered.isEmpty()) {
                        val currentStatus = selectedStatus
                        if (currentStatus != null) {
                            EmptyStates.NoPaymentsFiltered(filterName = currentStatus.getDisplayName())
                        } else {
                            EmptyStates.NoPaymentsYet(forBuyer = false)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { payment ->
                                PaymentCard(payment = payment, onClick = { onPaymentClick(payment.id) })
                            }
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                        }
                    }
                }
                is PaymentUiState.Error -> {
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
                is PaymentUiState.Idle -> { /* no-op */ }
            }
        }
    }
}

// ── Stats Cards ───────────────────────────────────────────────────────────────

@Composable
private fun PaymentStatsCards(stats: com.gcuf.craftoria.data.repository.SellerPaymentStats) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tinted gradient section header — Total Earnings hero
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Success.copy(alpha = 0.06f), Success.copy(alpha = 0.02f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(36.dp).background(Success.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(text = "Total Earnings", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", stats.totalEarnings)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Success)
                    }
                }

                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatMiniCard("Completed", "PKR ${String.format(java.util.Locale.US, "%.0f", stats.completedAmount)}", Icons.Default.CheckCircle, Color(0xFFE8F5E9), Success, Modifier.weight(1f))
                        StatMiniCard("Pending", "PKR ${String.format(java.util.Locale.US, "%.0f", stats.pendingAmount)}", Icons.Default.Schedule, Color(0xFFFFF3E0), Warning, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatMiniCard("Payments", "${stats.totalPayments}", Icons.Default.Receipt, Color(0xFFE3F2FD), Color(0xFF2196F3), Modifier.weight(1f))
                        StatMiniCard("Orders", "${stats.totalOrders}", Icons.Default.ShoppingCart, Color(0xFFF3E5F5), Color(0xFF9C27B0), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(title: String, amount: String, icon: ImageVector, backgroundColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = backgroundColor, modifier = modifier) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = title, fontSize = 10.sp, color = textColor.copy(alpha = 0.80f), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
            Icon(imageVector = icon, contentDescription = title, tint = textColor.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
        }
    }
}

// ── Payment Card ──────────────────────────────────────────────────────────────

@Composable
private fun PaymentCard(payment: SellerPayment, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(BackgroundSecondary).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Order #${payment.orderId.take(8).uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 1.dp)) {
                        // ✅ REALTIME: Display buyer name with real-time updates from Firebase
                        RealtimeNameDisplay(userId = payment.buyerId, fallbackName = payment.buyerName, fontSize = 11.sp, color = TextSecondary)
                        Text(text = "· ${payment.itemsCount} item${if (payment.itemsCount > 1) "s" else ""}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                PaymentStatusBadge(payment.status)
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            // ✅ PATCH 5: Show refund notice if refunded
            if (payment.status.lowercase() == "refunded" && payment.refundAmount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(Error.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Refunded PKR ${String.format(java.util.Locale.US, "%.0f", payment.refundAmount)} to buyer",
                        fontSize = 11.sp,
                        color = Error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Success)
                Text(text = formatSellerPaymentDate(payment.getDisplayDate()), fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

private fun formatSellerPaymentDate(timestamp: Long): String = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(timestamp))