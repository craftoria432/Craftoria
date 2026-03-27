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
    var showFilterMenu by remember { mutableStateOf(false) }

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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Your earnings overview",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 12.sp
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
                actions = {
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            when (statsState) {
                is PaymentStatsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is PaymentStatsUiState.Success -> {
                    PaymentStatsCards((statsState as PaymentStatsUiState.Success).stats)
                }
                is PaymentStatsUiState.Error -> {
                    Text(text = "Failed to load stats", modifier = Modifier.padding(14.dp), color = Error)
                }
            }

            if (showFilterMenu) {
                PaymentFilterMenu(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { status -> viewModel.setStatusFilter(status); showFilterMenu = false },
                    onClearFilters = { viewModel.clearFilters(); showFilterMenu = false }
                )
            }

            when (paymentState) {
                is PaymentUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is PaymentUiState.Success -> {
                    val payments = (paymentState as PaymentUiState.Success).payments
                    val filteredPayments = viewModel.getFilteredPayments(payments)
                    if (filteredPayments.isEmpty()) {
                        EmptyPaymentsState()
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredPayments) { payment ->
                                PaymentCard(payment = payment, onClick = { onPaymentClick(payment.id) })
                            }
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                        }
                    }
                }
                is PaymentUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(48.dp)) {
                            Box(modifier = Modifier.size(80.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Error.copy(alpha = 0.60f), modifier = Modifier.size(38.dp))
                            }
                            Text(text = (paymentState as PaymentUiState.Error).message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
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
                        RealtimeNameDisplay(userId = payment.buyerId, fallbackName = payment.buyerName, fontSize = 11.sp, color = TextSecondary)
                        Text(text = "· ${payment.itemsCount} item${if (payment.itemsCount > 1) "s" else ""}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                PaymentStatusBadge(payment.status)
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Success)
                Text(text = formatSellerPaymentDate(payment.createdAt), fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// ── Status Badge — theme tokens replacing hardcoded hex ───────────────────────

@Composable
private fun PaymentStatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "completed"  -> Success.copy(alpha = 0.10f) to Success
        "pending"    -> Warning.copy(alpha = 0.15f) to Warning
        "processing" -> Color(0xFF2196F3).copy(alpha = 0.10f) to Color(0xFF2196F3)
        "failed"     -> Error.copy(alpha = 0.10f) to Error
        "refunded"   -> BorderColor to TextSecondary
        else         -> BackgroundSecondary to TextSecondary
    }
    Surface(shape = RoundedCornerShape(6.dp), color = backgroundColor) {
        Text(text = status.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

// ── Filter Menu ───────────────────────────────────────────────────────────────

@Composable
private fun PaymentFilterMenu(selectedStatus: PaymentStatus?, onStatusSelected: (PaymentStatus) -> Unit, onClearFilters: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(text = "Filter by Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(bottom = 10.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 8.dp))
            PaymentStatus.values().forEach { status ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onStatusSelected(status) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedStatus == status, onClick = { onStatusSelected(status) }, colors = RadioButtonDefaults.colors(selectedColor = Primary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = status.getDisplayName(), fontSize = 13.sp, color = TextPrimary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onClearFilters, modifier = Modifier.fillMaxWidth().height(38.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp)) {
                Text("Clear Filters", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyPaymentsState() {
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(80.dp).background(Primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = Primary.copy(alpha = 0.50f), modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = "No Payments Yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Your payments will appear here", fontSize = 13.sp, color = TextSecondary)
    }
}

private fun formatSellerPaymentDate(timestamp: Long): String = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(timestamp))