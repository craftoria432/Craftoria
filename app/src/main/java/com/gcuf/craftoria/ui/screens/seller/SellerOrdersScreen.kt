package com.gcuf.craftoria.ui.screens.seller

import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Delete
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
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
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.model.getRefundStatusEnum
import com.gcuf.craftoria.ui.components.FilterTabRow
import com.gcuf.craftoria.ui.components.OrderStatusBadge
import com.gcuf.craftoria.ui.components.EmptyStateComponent
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.utils.OrderRefundState
import com.gcuf.craftoria.utils.formatDateTime
import com.gcuf.craftoria.viewmodel.SellerOrdersState
import com.gcuf.craftoria.viewmodel.SellerOrdersViewModel
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
        // ✅ Run migration on first load to populate missing store IDs
        sellerOrdersViewModel.loadOrders(user.id, runMigration = true)
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
                // ✅ FIX #13: Clear selection state only after success
                selectedOrders = emptySet()
                isSelectionMode = false
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Manage incoming orders",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
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

            when {
                uiState is SellerOrdersState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                orders.isEmpty() && uiState !is SellerOrdersState.Loading -> {
                    SellerEmptyOrdersState(filterType = currentFilter)
                }
                else -> {
                    LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
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
                                // ✅ FIX #15: Remove double navigation - only show dialog OR call onOrderClick
                                if (!isSelectionMode) {
                                    selectedOrder = order
                                    showOrderDetails = true
                                    // onOrderClick removed to prevent double navigation
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
                    onClick = { 
                        sellerOrdersViewModel.deleteMultipleOrders(selectedOrders.toList(), user.id)
                        // ✅ FIX #13: Don't clear state here - let ActionSuccess handler do it
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
        OrderStatus.COMPLETED to "Completed",
        OrderStatus.CANCELLED to "Cancelled"
    )
    val selectedIndex = filters.indexOfFirst { it.first == currentFilter }.coerceAtLeast(0)
    val badgeCounts = filters.map { (status, _) ->
        if (status == OrderStatus.PENDING) newOrdersCount else 0
    }

    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterTabRow(
            tabs = filters.map { it.second },
            selectedIndex = selectedIndex,
            onTabSelected = { index -> onFilterSelected(filters[index].first) },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            badgeCounts = badgeCounts
        )
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

    // ✅ Track refund state for this order
    // ✅ FIX #12: Initialize with order's refund_status to prevent flash of wrong badge
    var refundState by remember(order.id) {
        mutableStateOf<OrderRefundState?>(
            when (order.getRefundStatusEnum()) {
                com.gcuf.craftoria.data.model.OrderRefundStatus.COMPLETED -> OrderRefundState.COMPLETED
                else -> OrderRefundState.NONE
            }
        )
    }

    var coSellerStoreName by remember(order.coSellerStoreId) {
        mutableStateOf<String?>(null)
    }

    // ✅ FIX #14: Hoist repository outside LaunchedEffect to prevent leaks
    val storeRepository = remember { com.gcuf.craftoria.data.repository.CoSellerStoreRepository() }

    LaunchedEffect(order.coSellerStoreId) {
        if (order.coSellerStoreId.isNotEmpty()) {
            try {
                val result = storeRepository.getStoreById(order.coSellerStoreId)
                if (result.isSuccess) {
                    coSellerStoreName = result.getOrNull()?.storeName ?: "Co-seller Store"
                } else {
                    coSellerStoreName = "Co-seller Store"
                }
            } catch (e: Exception) {
                Log.e("SellerOrderCard", "Error loading co-seller store name", e)
                coSellerStoreName = "Co-seller Store"
            }
        }
    }

    DisposableEffect(order.id) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val query = db.collection("refunds")
            .whereEqualTo("order_id", order.id)
            .limit(5)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                refundState = OrderRefundState.NONE
                return@addSnapshotListener
            }

            try {
                refundState = if (snapshot == null || snapshot.documents.isEmpty()) {
                    OrderRefundState.NONE
                } else {
                    val best = snapshot.documents.maxByOrNull { com.gcuf.craftoria.utils.docPriority(it) }
                    if (best == null) {
                        OrderRefundState.NONE
                    } else {
                        com.gcuf.craftoria.utils.docToRefundState(best)
                    }
                }
            } catch (e: Exception) {
                refundState = OrderRefundState.NONE
            }
        }

        onDispose {
            listener.remove()
        }
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ When refund is completed, show ONLY the refunded badge — suppress order status badge
                    if (refundState == OrderRefundState.COMPLETED) {
                        // ✅ FIXED: Use consistent purple color for Refunded badge
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE2D5F3)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Refunded",
                                    tint = Color(0xFF5A2D82),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Refunded",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF5A2D82),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                        // No StatusBadge shown when refund is completed
                    } else {
                        // Show order status badge only when NOT refunded
                        // ✅ FIX #1: Safe status conversion using when() instead of try/catch
                        val orderStatus = when (order.status.uppercase()) {
                            "PENDING"    -> OrderStatus.PENDING
                            "PROCESSING" -> OrderStatus.PROCESSING
                            "SHIPPED"    -> OrderStatus.SHIPPED
                            "DELIVERED"  -> OrderStatus.DELIVERED
                            "COMPLETED"  -> OrderStatus.COMPLETED
                            "CANCELLED"  -> OrderStatus.CANCELLED
                            else         -> OrderStatus.PENDING
                        }
                        OrderStatusBadge(status = orderStatus)
                    }
                }
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
                    // ✅ NEW: Display buyer name with real-time updates
                    com.gcuf.craftoria.ui.components.RealtimeNameDisplay(
                        userId = order.buyerId,
                        fallbackName = order.buyerName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    // ✅ Display store identification for co-seller orders
                    if (order.coSellerStoreId.isNotEmpty()) {
                        Log.d("SellerOrderCard", "Co-seller order detected: storeId=${order.coSellerStoreId}")
                        CoSellerStoreBadge(
                            storeId = order.coSellerStoreId,
                            storeName = coSellerStoreName,  // ✅ Pass pre-fetched store name
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Log.d("SellerOrderCard", "Regular order (no store): orderId=${order.id}")
                    }
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
fun SellerEmptyOrdersState(filterType: OrderStatus? = null) {
    // ✅ FIXED: Show specific empty state text based on filter type with professional subtext
    val (title, message, subtext) = when (filterType) {
        OrderStatus.PENDING -> Triple(
            "No Pending Orders",
            "New orders will appear here",
            "You'll be notified when customers place orders that need your attention"
        )
        OrderStatus.PROCESSING -> Triple(
            "No Processing Orders",
            "Orders being prepared will appear here",
            "Orders you've accepted will show up here while you prepare them"
        )
        OrderStatus.SHIPPED -> Triple(
            "No Shipped Orders",
            "Shipped orders will appear here",
            "Track orders that are on their way to customers"
        )
        OrderStatus.DELIVERED -> Triple(
            "No Delivered Orders",
            "Delivered orders will appear here",
            "Orders successfully delivered to customers will show up here"
        )
        OrderStatus.COMPLETED -> Triple(
            "No Completed Orders",
            "Completed orders will appear here",
            "Your order history and completed transactions will be listed here"
        )
        OrderStatus.CANCELLED -> Triple(
            "No Cancelled Orders",
            "Cancelled orders will appear here",
            "Orders that were cancelled will show up here for your records"
        )
        null -> Triple(
            "No Orders Yet",
            "Your orders will appear here",
            "Start selling your products and manage incoming orders from this screen"
        )
        else -> Triple(
            "No Orders Yet",
            "Your orders will appear here",
            "Start selling your products and manage incoming orders from this screen"
        )
    }
    
    // ✅ STANDARDIZED: Use unified EmptyStateComponent with consistent sizing and styling
    EmptyStateComponent(
        icon = Icons.Default.ShoppingBag,
        title = title,
        message = message,
        subtext = subtext
    )
}

/**
 * ✅ IMPROVED: Display store badge for co-seller orders
 * - Receives store name as parameter to eliminate loading state
 * - Professional styling with building icon
 * - No async loading - data is pre-fetched at order screen level
 */
@Composable
fun CoSellerStoreBadge(
    storeId: String,
    storeName: String? = null,
    modifier: Modifier = Modifier
) {
    // ✅ If storeName is provided, use it directly (no loading state)
    var displayName by remember(storeId, storeName) { 
        mutableStateOf(storeName ?: "Co-seller Store")
    }

    // ✅ Only fetch if storeName is not provided
    // ✅ Repository instantiated ONLY inside LaunchedEffect, not on every recomposition
    if (storeName == null) {
        LaunchedEffect(storeId) {
            try {
                val storeRepository = com.gcuf.craftoria.data.repository.CoSellerStoreRepository()
                val result = storeRepository.getStoreById(storeId)
                if (result.isSuccess) {
                    val store = result.getOrNull()
                    displayName = store?.storeName ?: "Co-seller Store"
                }
            } catch (e: Exception) {
                Log.e("CoSellerStoreBadge", "Error loading store name", e)
                displayName = "Co-seller Store"
            }
        }
    }

    // ✅ FIXED: Use Icons.Default.Store to match quick access section
    // ✅ Professional badge design with consistent styling
    Surface(
        color = Primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, Primary.copy(alpha = 0.20f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            // ✅ FIXED: Use Store icon to match quick access section
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = "Co-seller Store",
                tint = Primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
