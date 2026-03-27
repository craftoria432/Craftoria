package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.SellerOrdersState
import com.gcuf.craftoria.viewmodel.SellerOrdersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersScreen(
    user: User,
    highlightOrderId: String = "",
    onBackClick: () -> Unit,
    onOrderClick: (Order) -> Unit,
    navController: androidx.navigation.NavHostController? = null,
    sellerOrdersViewModel: SellerOrdersViewModel = viewModel()
) {
    val uiState by sellerOrdersViewModel.uiState.collectAsState()
    val orders by sellerOrdersViewModel.orders.collectAsState()
    val currentFilter by sellerOrdersViewModel.currentFilter.collectAsState()
    val newOrdersCount by sellerOrdersViewModel.newOrdersCount.collectAsState()

    var showOrderDetails by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var showAcceptDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showShippedDialog by remember { mutableStateOf(false) }
    var showDeliveredDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedOrders by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var highlightedOrderId by remember { mutableStateOf(highlightOrderId) }

    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(user.id) {
        sellerOrdersViewModel.loadOrders(user.id)
    }

    LaunchedEffect(highlightedOrderId) {
        if (highlightedOrderId.isNotEmpty()) {
            kotlinx.coroutines.delay(10000)
            highlightedOrderId = ""
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SellerOrdersState.ActionSuccess -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                sellerOrdersViewModel.resetState()
            }
            is SellerOrdersState.Error -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                sellerOrdersViewModel.resetState()
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
                            text = "Orders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Manage incoming orders",
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
                            TextButton(
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
                        TextButton(
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
                        if (orders.any { it.status.uppercase() in listOf("COMPLETED", "CANCELLED", "DELIVERED") }) {
                            IconButton(onClick = { isSelectionMode = true }) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SellerOrderFilterTabs(
                currentFilter = currentFilter,
                newOrdersCount = newOrdersCount,
                onFilterSelected = { sellerOrdersViewModel.filterOrders(it, user.id) }
            )

            if (orders.isEmpty()) {
                SellerEmptyOrdersState()
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        SellerOrderCard(
                            order = order,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedOrders.contains(order.id),
                            isHighlighted = highlightedOrderId == order.id,
                            onSelectionToggle = {
                                selectedOrders = if (selectedOrders.contains(order.id))
                                    selectedOrders - order.id else selectedOrders + order.id
                            },
                            onViewDetails = {
                                if (!isSelectionMode) {
                                    selectedOrder = order
                                    showOrderDetails = true
                                    onOrderClick(order)
                                    coroutineScope.launch {
                                        val index = orders.indexOf(order)
                                        if (index >= 0) {
                                            lazyListState.animateScrollToItem(index)
                                        }
                                    }
                                }
                            },
                            onAccept = { selectedOrder = order; showAcceptDialog = true },
                            onReject = { selectedOrder = order; showRejectDialog = true },
                            onMarkShipped = { selectedOrder = order; showShippedDialog = true },
                            onMarkDelivered = { selectedOrder = order; showDeliveredDialog = true }
                        )
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        if (showOrderDetails) OrderDetailsDialog(order = order, onDismiss = { showOrderDetails = false })
        if (showAcceptDialog) AcceptOrderDialog(order = order, onConfirm = { sellerOrdersViewModel.acceptOrder(order.id, user.id); showAcceptDialog = false }, onDismiss = { showAcceptDialog = false })
        if (showRejectDialog) RejectOrderDialog(order = order, onConfirm = { reason, details -> sellerOrdersViewModel.rejectOrder(order.id, reason, details, user.id); showRejectDialog = false }, onDismiss = { showRejectDialog = false })
        if (showShippedDialog) MarkShippedDialog(onConfirm = { courier, tracking, date -> sellerOrdersViewModel.markAsShipped(orderId = order.id, courierName = courier, trackingNumber = tracking, expectedDeliveryDate = date, sellerId = user.id); showShippedDialog = false }, onDismiss = { showShippedDialog = false })
        if (showDeliveredDialog) MarkDeliveredDialog(onConfirm = { sellerOrdersViewModel.markAsDelivered(order.id, user.id); showDeliveredDialog = false }, onDismiss = { showDeliveredDialog = false })
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
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { sellerOrdersViewModel.deleteMultipleOrders(selectedOrders.toList(), user.id); selectedOrders = emptySet(); isSelectionMode = false; showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    border = BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun SellerOrderFilterTabs(
    currentFilter: OrderStatus?,
    newOrdersCount: Int,
    onFilterSelected: (OrderStatus?) -> Unit
) {
    val filters = listOf(
        null to "All",
        OrderStatus.PENDING to "Pending",
        OrderStatus.PROCESSING to "Processing",
        OrderStatus.SHIPPED to "Shipped",
        OrderStatus.DELIVERED to "Delivered",
        OrderStatus.CANCELLED to "Cancelled"
    )
    
    val selectedIndex = filters.indexOfFirst { it.first == currentFilter }.coerceAtLeast(0)
    
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.White,
        contentColor = Primary,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = Primary
            )
        }
    ) {
        filters.forEach { (status, label) ->
            Tab(
                selected = currentFilter == status,
                onClick = { onFilterSelected(status) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 13.sp, fontWeight = if (currentFilter == status) FontWeight.Bold else FontWeight.Medium)
                        if (status == OrderStatus.PENDING && newOrdersCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(containerColor = Primary) { Text(newOrdersCount.toString(), color = Color.White) }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SellerOrderCard(
    order: Order,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onSelectionToggle: () -> Unit,
    onViewDetails: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onMarkShipped: () -> Unit,
    onMarkDelivered: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails)
            .hoverable(interactionSource),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Primary.copy(alpha = 0.05f) 
                            else if (isHovered) Color.White.copy(alpha = 0.95f)
                            else Color.White
        ),
        border = BorderStroke(
            width = if (isHighlighted) 1.dp else 0.5.dp,
            color = if (isHighlighted) Primary else if (isSelected) Primary else BorderColor
        ),
        elevation = CardDefaults.cardElevation(if (isHovered) 4.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectionToggle() },
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = "Order #${order.id.take(8).uppercase()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = formatDateTime(order.getCreatedAtLong()),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundSecondary)
                ) {
                    val imageUrl = if (order.items.isNotEmpty()) order.items.first().productImage else order.productImage
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = CloudinaryManager.getOptimizedUrl(imageUrl, 100, 100),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val title = if (order.items.isNotEmpty()) order.items.first().productTitle else order.productTitle
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Total: PKR ${String.format(Locale.getDefault(), "%,.0f", order.totalPrice)}",
                        fontSize = 12.sp,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isSelectionMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (order.status.lowercase()) {
                        "pending" -> {
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Success),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = onReject,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                                border = BorderStroke(1.dp, Error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        "processing" -> {
                            Button(
                                onClick = onMarkShipped,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Mark as Shipped", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        "shipped" -> {
                            Button(
                                onClick = onMarkDelivered,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Mark as Delivered", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status.lowercase()) {
        "pending" -> Color(0xFFFFA500)
        "processing" -> Color(0xFF1E90FF)
        "shipped" -> Color(0xFF9370DB)
        "delivered", "completed" -> Success
        "cancelled", "rejected" -> Error
        else -> TextSecondary
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SellerEmptyOrdersState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Primary.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.4f),
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No orders found",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Try changing your filters or check back later",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

fun formatDateTime(timestamp: Long): String = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(timestamp))
