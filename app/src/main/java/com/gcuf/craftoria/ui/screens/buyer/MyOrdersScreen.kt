package com.gcuf.craftoria.ui.screens.buyer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.viewmodel.CartViewModel
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.ui.components.OrderDetailsDialog
import com.gcuf.craftoria.ui.components.CancelOrderDialog
import com.gcuf.craftoria.ui.components.OrderTrackingDialog
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.data.model.getStatusEnum
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.ui.theme.BorderStyles
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.OrderViewModel
import com.gcuf.craftoria.viewmodel.OrderActionState
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    userId: String,
    cartViewModel: CartViewModel,
    highlightOrderId: String = "",
    onBackClick: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    orderViewModel: OrderViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()

    val orders by orderViewModel.filteredOrders.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val currentFilter by orderViewModel.currentFilter.collectAsState()
    val orderActionState by orderViewModel.orderActionState.collectAsState()

    var showOrderDetails by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showTrackingDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var currentSortOption by remember { mutableStateOf("date_desc") }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedOrders by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var highlightedOrderId by remember { mutableStateOf(highlightOrderId) }
    var shouldScrollToHighlighted by remember { mutableStateOf(highlightOrderId.isNotEmpty()) }
    
    // ✅ LazyListState for autoscroll functionality
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(highlightedOrderId) {
        if (highlightedOrderId.isNotEmpty()) {
            kotlinx.coroutines.delay(10000)
            highlightedOrderId = ""
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            Log.d("MyOrdersScreen", "Loading orders for userId: $userId")
            try {
                orderViewModel.loadUserOrders(userId)
            } catch (e: Exception) {
                Log.e("MyOrdersScreen", "Error loading orders", e)
            }
        } else {
            Log.w("MyOrdersScreen", "Empty userId provided")
        }
    }

    LaunchedEffect(orders) {
        if (orders.isNotEmpty()) {
            orderViewModel.sortOrders(currentSortOption)
        }
    }

    LaunchedEffect(orders, isLoading) {
        Log.d("MyOrdersScreen", "Orders count: ${orders.size}, Loading: $isLoading")
        orders.forEach { order ->
            Log.d("MyOrdersScreen", "Order: ${order.id}, Status: ${order.status}")
        }
    }

    LaunchedEffect(orderActionState) {
        when (orderActionState) {
            is OrderActionState.Success -> {
                showCancelDialog = false
                selectedOrder = null
                orderViewModel.resetActionState()
                if (userId.isNotEmpty()) orderViewModel.loadUserOrders(userId)
            }
            is OrderActionState.Error -> {
                showCancelDialog = false
                selectedOrder = null
                orderViewModel.resetActionState()
            }
            else -> {}
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
                            text = "My Orders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Track and manage your orders",
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
                    if (isSelectionMode) {
                        if (selectedOrders.isNotEmpty()) {
                            androidx.compose.material3.TextButton(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = Error.copy(alpha = 0.9f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete (${selectedOrders.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { isSelectionMode = false; selectedOrders = emptySet() },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        if (orders.any { it.getStatusEnum() in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.PROCESSING) }) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
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
                .background(BackgroundSecondary)
                .padding(paddingValues)
        ) {
            OrderFilterTabs(
                currentFilter = currentFilter,
                onFilterSelected = { status -> orderViewModel.filterOrders(status) }
            )

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                orders.isEmpty() -> {
                    EmptyOrdersState(
                        filterType = currentFilter,
                        onBrowseProducts = onBackClick
                    )
                }

                else -> {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(orders) { order ->
                            OrderCard(
                                order = order,
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedOrders.contains(order.id),
                                isHighlighted = order.id == highlightedOrderId,
                                onSelectionToggle = {
                                    selectedOrders = if (selectedOrders.contains(order.id))
                                        selectedOrders - order.id else selectedOrders + order.id
                                },
                                onViewDetails = {
                                    if (!isSelectionMode) {
                                        selectedOrder = order
                                        showOrderDetails = true
                                        // ✅ Autoscroll to this order
                                        coroutineScope.launch {
                                            val index = orders.indexOf(order)
                                            if (index >= 0) {
                                                lazyListState.animateScrollToItem(index)
                                            }
                                        }
                                    }
                                },
                                onTrackOrder = { 
                                    selectedOrder = order
                                    showTrackingDialog = true
                                    // ✅ Autoscroll to this order
                                    coroutineScope.launch {
                                        val index = orders.indexOf(order)
                                        if (index >= 0) {
                                            lazyListState.animateScrollToItem(index)
                                        }
                                    }
                                },
                                onCancelOrder = { selectedOrder = order; showCancelDialog = true },
                                onReorder = {
                                    coroutineScope.launch { cartViewModel.reorder(userId, order) }
                                    onNavigateToCart()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOrderDetails && selectedOrder != null) {
        OrderDetailsDialog(order = selectedOrder!!, onDismiss = { showOrderDetails = false; selectedOrder = null })
    }
    if (showCancelDialog && selectedOrder != null) {
        CancelOrderDialog(
            orderId = selectedOrder!!.id,
            onConfirm = { orderViewModel.cancelOrder(selectedOrder!!.id) },
            onDismiss = { showCancelDialog = false; selectedOrder = null }
        )
    }
    if (showTrackingDialog && selectedOrder != null) {
        OrderTrackingDialog(order = selectedOrder!!, onDismiss = { showTrackingDialog = false; selectedOrder = null })
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text(text = "Sort Orders", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        "date_desc" to "Newest First",
                        "date_asc" to "Oldest First",
                        "amount_desc" to "Amount: High to Low",
                        "amount_asc" to "Amount: Low to High"
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSortOption == value,
                                onClick = { currentSortOption = value; orderViewModel.sortOrders(value); showSortDialog = false },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Text(text = label, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Close", color = Primary)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Error.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Delete Orders", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete ${selectedOrders.size} order${if (selectedOrders.size > 1) "s" else ""}? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        orderViewModel.deleteMultipleOrders(selectedOrders.toList())
                        selectedOrders = emptySet()
                        isSelectionMode = false
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

// ── Filter Tabs ───────────────────────────────────────────────────────────────

@Composable
fun OrderFilterTabs(
    currentFilter: OrderStatus?,
    onFilterSelected: (OrderStatus?) -> Unit
) {
    val filters = listOf(
        Pair(null, "All"),
        Pair(OrderStatus.PENDING, "Pending"),
        Pair(OrderStatus.PROCESSING, "Processing"),
        Pair(OrderStatus.SHIPPED, "Shipped"),
        Pair(OrderStatus.DELIVERED, "Delivered"),
        Pair(OrderStatus.CANCELLED, "Cancelled")
    )

    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters.size) { index ->
                val (status, label) = filters[index]
                val isSelected = currentFilter == status
                Surface(
                    onClick = { onFilterSelected(status) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Primary else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 0.dp else 0.5.dp,
                        color = if (isSelected) Primary else BorderColor
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

// ── Order Card ────────────────────────────────────────────────────────────────

@Composable
fun OrderCard(
    order: Order,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    onSelectionToggle: () -> Unit = {},
    onViewDetails: () -> Unit,
    onTrackOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onReorder: () -> Unit
) {
    val status = order.getStatusEnum()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFFFFF5F8) else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 4.dp else 0.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isHighlighted) 2.dp else if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isHighlighted) Primary else if (isSelected) Primary else BorderColor
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (isSelectionMode && status in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.PROCESSING)) {
                onSelectionToggle()
            }
        }
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSelectionMode && status in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.PROCESSING)) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectionToggle() },
                            colors = CheckboxDefaults.colors(checkedColor = Primary, uncheckedColor = TextSecondary),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(text = "#${order.id.take(8).uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                        Text(text = "Placed on ${formatDate(order.getCreatedAtLong())}", fontSize = 11.sp, color = TextLight, modifier = Modifier.padding(top = 1.dp), maxLines = 1)
                    }
                }
                OrderStatusBadge(status = status)
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Product Row
            if (order.items.isNotEmpty()) {
                val firstItem = order.items.first()
                val extraCount = order.items.size - 1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BackgroundSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (firstItem.productImage.isNotEmpty()) {
                            AsyncImage(
                                model = CloudinaryManager.getOptimizedUrl(url = firstItem.productImage, width = 150, quality = 75),
                                contentDescription = firstItem.productTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = TextLight, modifier = Modifier.size(30.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = firstItem.productTitle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Qty: ${firstItem.quantity}", fontSize = 12.sp, color = TextSecondary)
                            Text(text = "PKR ${firstItem.price.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                        }
                        if (extraCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(color = Primary.copy(alpha = 0.08f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "+$extraCount more item${if (extraCount > 1) "s" else ""}",
                                    fontSize = 10.sp,
                                    color = Primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Seller row
                val sellerName = order.items.firstOrNull()?.sellerName ?: "Seller"
                val sellerId = order.sellerId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(BackgroundSecondary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Primary.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                    }
                    Text(text = "Sold by ", fontSize = 12.sp, color = TextSecondary)
                    RealtimeNameDisplay(
                        userId = sellerId,
                        fallbackName = sellerName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Shipped courier banner
            if (status == OrderStatus.SHIPPED && order.courierName.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .background(Color(0xFFF3E5F5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(14.dp))
                    Text(text = "${order.courierName} · ${order.trackingNumber}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF7B1FA2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Total row
            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = BorderColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Total Amount", fontSize = 12.sp, color = TextSecondary)
                Text(text = "PKR ${order.totalPrice.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            OrderActionButtons(
                order = order,
                isHighlighted = isHighlighted,
                onViewDetails = onViewDetails,
                onTrackOrder = onTrackOrder,
                onCancelOrder = onCancelOrder,
                onReorder = onReorder
            )
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING -> Pair(Color(0xFFFFF3CD), Color(0xFF856404))
        OrderStatus.PROCESSING, OrderStatus.CONFIRMED -> Pair(Color(0xFFE3F2FD), Color(0xFF1976D2))
        OrderStatus.SHIPPED -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        OrderStatus.DELIVERED -> Pair(Color(0xFFE8F5E8), Color(0xFF2E7D2E))
        OrderStatus.CANCELLED -> Pair(Color(0xFFF8D7DA), Color(0xFF721C24))
        OrderStatus.NEW -> Pair(Color(0xFFFFF3CD), Color(0xFF856404))
        OrderStatus.COMPLETED -> Pair(Color(0xFFE8F5E8), Color(0xFF2E7D2E))
    }
    Surface(shape = RoundedCornerShape(10.dp), color = backgroundColor) {
        Text(
            text = status.getDisplayName(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

// ── Action Buttons ────────────────────────────────────────────────────────────

@Composable
fun OrderActionButtons(
    order: Order,
    isHighlighted: Boolean = false,
    onViewDetails: () -> Unit,
    onTrackOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onReorder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (order.getStatusEnum()) {
            OrderStatus.PENDING, OrderStatus.NEW -> {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(text = "View Details", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                OutlinedButton(
                    onClick = onCancelOrder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Error),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(text = "Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }

            OrderStatus.PROCESSING, OrderStatus.CONFIRMED -> {
                TrackOrderButton(
                    onClick = onTrackOrder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    isHighlighted = isHighlighted
                )
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(text = "View Details", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }

            OrderStatus.SHIPPED -> {
                TrackOrderButton(
                    onClick = onTrackOrder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    isHighlighted = isHighlighted
                )
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(text = "View Details", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }

            OrderStatus.DELIVERED, OrderStatus.COMPLETED -> {
                TrackOrderButton(
                    onClick = onTrackOrder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    isHighlighted = isHighlighted
                )
                OutlinedButton(
                    onClick = onReorder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(text = "Reorder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }

            OrderStatus.CANCELLED -> {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(text = "View Details", fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
fun TrackOrderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Pink colors - consistent across app
    val hoverPink = Color(0xFFFFE4E1)  // Light pink for hover background
    val hoverPinkBorder = Color(0xFFE91E8C)  // Pink border for hover
    
    Button(
        onClick = onClick,
        modifier = modifier
            .hoverable(interactionSource = interactionSource),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(10.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = when {
                        isHighlighted -> Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                        isHovered -> Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                        else -> Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                    },
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Track Order",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isHighlighted -> Primary
                    isHovered -> Primary
                    else -> Color.White
                }
            )
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
fun EmptyOrdersState(filterType: OrderStatus?, onBrowseProducts: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Primary.copy(alpha = 0.5f), modifier = Modifier.size(46.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (filterType == null) "No orders yet" else "No ${filterType.getDisplayName().lowercase()} orders",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (filterType == null) "Start shopping to see your orders here" else "No orders with this status",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        if (filterType == null) {
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onBrowseProducts,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 180.dp)
                    .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(14.dp))
            ) {
                Text(text = "Browse Products", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ── Date Helpers ──────────────────────────────────────────────────────────────

fun formatDate(timestamp: Long): String = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
fun formatDateTime(timestamp: Long): String = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(timestamp))