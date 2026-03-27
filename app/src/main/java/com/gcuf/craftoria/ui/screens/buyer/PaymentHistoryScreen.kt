package com.gcuf.craftoria.ui.screens.buyer

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
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.ui.theme.Success
import com.gcuf.craftoria.ui.theme.Warning
import com.gcuf.craftoria.ui.theme.Error
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
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
    val paymentState by viewModel.paymentState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val filteredCount by viewModel.filteredCount.collectAsState()

    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(buyerId) {
        viewModel.loadBuyerPayments(buyerId)
        viewModel.loadPaymentStats(buyerId)
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
                actions = {
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = if (selectedStatus != null)
                                        Primary.copy(alpha = 0.30f)
                                    else
                                        Color.White.copy(alpha = 0.18f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (statsState) {
                is BuyerPaymentStatsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is BuyerPaymentStatsUiState.Success -> {
                    BuyerPaymentStatsCards((statsState as BuyerPaymentStatsUiState.Success).stats)
                }
                is BuyerPaymentStatsUiState.Error -> {
                    Text(text = "Failed to load stats", modifier = Modifier.padding(14.dp), color = Error)
                }
            }

            if (showFilterMenu) {
                BuyerPaymentFilterMenu(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { status -> viewModel.setStatusFilter(status); showFilterMenu = false },
                    onClearFilters = { viewModel.clearFilters(); showFilterMenu = false },
                    payments = if (paymentState is BuyerPaymentUiState.Success) (paymentState as BuyerPaymentUiState.Success).payments else emptyList(),
                    viewModel = viewModel
                )
            }

            val currentStatus = selectedStatus
            if (currentStatus != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Primary.copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                            Text(text = "Filtered: ${currentStatus.getDisplayName()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                            Text(text = "($filteredCount)", fontSize = 11.sp, color = Primary.copy(alpha = 0.70f))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearFilters() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("Clear", fontSize = 11.sp, color = Primary)
                    }
                }
            }

            when (paymentState) {
                is BuyerPaymentUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is BuyerPaymentUiState.Success -> {
                    val payments = (paymentState as BuyerPaymentUiState.Success).payments
                    val filteredPayments = viewModel.getFilteredPayments(payments)
                    if (filteredPayments.isEmpty()) {
                        BuyerEmptyPaymentsState(hasFilter = selectedStatus != null, filterName = selectedStatus?.getDisplayName() ?: "")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredPayments) { payment -> BuyerPaymentCard(payment = payment) }
                            item { Spacer(modifier = Modifier.height(4.dp)) }
                        }
                    }
                }
                is BuyerPaymentUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(48.dp)) {
                            Box(modifier = Modifier.size(80.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Error.copy(alpha = 0.60f), modifier = Modifier.size(38.dp))
                            }
                            Text(text = (paymentState as BuyerPaymentUiState.Error).message, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyerPaymentStatsCards(stats: com.gcuf.craftoria.viewmodel.BuyerPaymentStats) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tinted gradient header — Total Spent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary.copy(alpha = 0.06f), Primary.copy(alpha = 0.02f))))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(36.dp).background(Primary.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(text = "Total Spent", fontSize = 11.sp, color = TextSecondary)
                        Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", stats.totalSpent)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Primary)
                    }
                }
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuyerStatMiniCard("Completed", "PKR ${String.format(java.util.Locale.US, "%.0f", stats.completedAmount)}", Icons.Default.CheckCircle, Color(0xFFE8F5E9), Color(0xFF4CAF50), Modifier.weight(1f))
                        BuyerStatMiniCard("Pending", "PKR ${String.format(java.util.Locale.US, "%.0f", stats.pendingAmount)}", Icons.Default.Schedule, Color(0xFFFFF3E0), Warning, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuyerStatMiniCard("Payments", "${stats.totalPayments}", Icons.Default.Receipt, Color(0xFFE3F2FD), Color(0xFF2196F3), Modifier.weight(1f))
                        BuyerStatMiniCard("Sellers", "${stats.totalSellers}", Icons.Default.Store, Color(0xFFF3E5F5), Color(0xFF9C27B0), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyerStatMiniCard(title: String, amount: String, icon: ImageVector, backgroundColor: Color, textColor: Color, modifier: Modifier = Modifier) {
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

@Composable
private fun BuyerPaymentCard(payment: SellerPayment) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(BackgroundSecondary).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(text = "Order #${payment.orderId.take(8).uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    RealtimeNameDisplay(userId = payment.sellerId, fallbackName = payment.sellerName, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 1.dp))
                }
                BuyerPaymentStatusBadge(payment.status)
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(bottom = 7.dp)) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Text(text = "${payment.itemsCount} item(s)", fontSize = 11.sp, color = TextSecondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "PKR ${String.format(java.util.Locale.US, "%.0f", payment.amount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text(text = formatPaymentDate(payment.createdAt), fontSize = 11.sp, color = TextSecondary)
                }
                if (payment.paymentMethod.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                        Text(text = payment.paymentMethod, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyerPaymentStatusBadge(status: String) {
    val (backgroundColor, textColor) = when (status.lowercase()) {
        "completed"  -> Success.copy(alpha = 0.10f) to Success
        "pending"    -> Warning.copy(alpha = 0.15f) to Warning
        "processing" -> Color(0xFF2196F3).copy(alpha = 0.10f) to Color(0xFF2196F3)
        "failed"     -> Error.copy(alpha = 0.10f) to Error
        else         -> BorderColor to TextSecondary
    }
    Surface(shape = RoundedCornerShape(6.dp), color = backgroundColor) {
        Text(text = status.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun BuyerPaymentFilterMenu(selectedStatus: PaymentStatus?, onStatusSelected: (PaymentStatus) -> Unit, onClearFilters: () -> Unit, payments: List<SellerPayment> = emptyList(), viewModel: BuyerPaymentViewModel? = null) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(text = "Filter by Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(bottom = 10.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 8.dp))
            PaymentStatus.values().forEach { status ->
                val count = viewModel?.getCountForStatus(status, payments) ?: 0
                Row(modifier = Modifier.fillMaxWidth().clickable { onStatusSelected(status) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        RadioButton(selected = selectedStatus == status, onClick = { onStatusSelected(status) }, colors = RadioButtonDefaults.colors(selectedColor = Primary))
                        Text(text = status.getDisplayName(), fontSize = 13.sp, color = TextPrimary)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.08f)) {
                        Text(text = count.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onClearFilters, modifier = Modifier.fillMaxWidth().height(38.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) {
                Text("Clear Filters", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BuyerEmptyPaymentsState(hasFilter: Boolean = false, filterName: String = "") {
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(80.dp).background(Primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = if (hasFilter) Icons.Default.FilterList else Icons.Default.Receipt, contentDescription = null, tint = Primary.copy(alpha = 0.50f), modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = if (hasFilter) "No Payments Found" else "No Payments Yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = if (hasFilter) "No payments match the filter: $filterName" else "Your payment history will appear here", fontSize = 13.sp, color = TextSecondary)
    }
}

private fun formatPaymentDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
    return sdf.format(java.util.Date(timestamp))
}