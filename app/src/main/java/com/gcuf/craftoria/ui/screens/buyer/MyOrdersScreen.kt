package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.ui.components.getProductEmoji
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.OrderViewModel
import com.gcuf.craftoria.viewmodel.OrderActionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(
    userId: String,
    onBackClick: () -> Unit,
    onNavigateToProduct: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    orderViewModel: OrderViewModel = viewModel()
) {
    val orders by orderViewModel.filteredOrders.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val currentFilter by orderViewModel.currentFilter.collectAsState()
    val orderActionState by orderViewModel.orderActionState.collectAsState()

    var showOrderDetails by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showTrackingDialog by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(userId) {
        orderViewModel.loadUserOrders(userId)
    }

    // Handle action states
    LaunchedEffect(orderActionState) {
        when (orderActionState) {
            is OrderActionState.Success -> {
                // Show success message
                showCancelDialog = false
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Orders",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Filter settings */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Primary, PrimaryLight)
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Tabs
            OrderFilterTabs(
                currentFilter = currentFilter,
                onFilterSelected = { status ->
                    orderViewModel.filterOrders(status)
                }
            )

            // Orders List
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                orders.isEmpty() -> {
                    EmptyOrdersState(
                        filterType = currentFilter,
                        onBrowseProducts = { /* Navigate to home */ }
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(orders) { order ->
                            OrderCard(
                                order = order,
                                onViewDetails = {
                                    selectedOrder = order
                                    showOrderDetails = true
                                },
                                onTrackOrder = {
                                    selectedOrder = order
                                    showTrackingDialog = true
                                },
                                onCancelOrder = {
                                    selectedOrder = order
                                    showCancelDialog = true
                                },
                                onReorder = {
                                    // Add to cart logic
                                    orderViewModel.reorder(order) { /* Add to cart */ }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Order Details Dialog
    if (showOrderDetails && selectedOrder != null) {
        OrderDetailsDialog(
            order = selectedOrder!!,
            onDismiss = {
                showOrderDetails = false
                selectedOrder = null
            }
        )
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog && selectedOrder != null) {
        CancelOrderDialog(
            orderId = selectedOrder!!.id,
            onConfirm = {
                orderViewModel.cancelOrder(selectedOrder!!.id)
            },
            onDismiss = {
                showCancelDialog = false
                selectedOrder = null
            }
        )
    }

    // Tracking Dialog
    if (showTrackingDialog && selectedOrder != null) {
        OrderTrackingDialog(
            order = selectedOrder!!,
            onDismiss = {
                showTrackingDialog = false
                selectedOrder = null
            }
        )
    }
}

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
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (status, label) ->
                FilterChip(
                    selected = currentFilter == status,
                    onClick = { onFilterSelected(status) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Primary,
                        labelColor = Color.Gray,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = BorderColor,
                        selectedBorderColor = Primary,
                        borderWidth = 2.dp
                    )
                )
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onViewDetails: () -> Unit,
    onTrackOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onReorder: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, BorderColor),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "#${order.id.take(8).uppercase()}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatDate(order.createdAt),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Preview
            if (order.items.isNotEmpty()) {
                val firstItem = order.items.first()

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = BackgroundLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Product Image
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = MaterialTheme.shapes.small,
                            color = BackgroundSecondary
                        ) {
                            if (firstItem.productImage.isNotEmpty()) {
                                AsyncImage(
                                    model = CloudinaryManager.getOptimizedUrl(
                                        url = firstItem.productImage,
                                        width = 200,
                                        quality = 75
                                    ),
                                    contentDescription = firstItem.productTitle,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getProductEmoji(""),
                                        fontSize = 28.sp
                                    )
                                }
                            }
                        }

                        // Product Details
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = firstItem.productTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Qty: ${firstItem.quantity}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "PKR ${firstItem.price.toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seller Info
            Surface(
                shape = MaterialTheme.shapes.small,
                color = BackgroundSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = PrimaryLight
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👤", fontSize = 14.sp)
                        }
                    }

                    Text(
                        text = "Sold by: ",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = order.sellerName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Text(
                    text = "PKR ${order.totalPrice.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = BorderColor)

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            OrderActionButtons(
                order = order,
                onViewDetails = onViewDetails,
                onTrackOrder = onTrackOrder,
                onCancelOrder = onCancelOrder,
                onReorder = onReorder
            )
        }
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING -> Pair(Color(0xFFFFF3CD), Color(0xFF856404))
        OrderStatus.PROCESSING, OrderStatus.CONFIRMED -> Pair(Color(0xFFE3F2FD), Color(0xFF1976D2))
        OrderStatus.SHIPPED -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        OrderStatus.DELIVERED -> Pair(Color(0xFFE8F5E8), Color(0xFF2E7D2E))
        OrderStatus.CANCELLED -> Pair(Color(0xFFF8D7DA), Color(0xFF721C24))
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = backgroundColor
    ) {
        Text(
            text = status.getDisplayName(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun OrderActionButtons(
    order: Order,
    onViewDetails: () -> Unit,
    onTrackOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onReorder: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        when (order.status) {
            OrderStatus.PENDING -> {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BorderColor),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onCancelOrder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Error
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Error),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "Cancel Order",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OrderStatus.PROCESSING, OrderStatus.CONFIRMED -> {
                Button(
                    onClick = onTrackOrder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "Track Order",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BorderColor),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OrderStatus.SHIPPED -> {
                Button(
                    onClick = onTrackOrder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "Track Order",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BorderColor),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OrderStatus.DELIVERED -> {
                Button(
                    onClick = onReorder,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "Reorder",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BorderColor),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OrderStatus.CANCELLED -> {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BorderColor),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyOrdersState(
    filterType: OrderStatus?,
    onBrowseProducts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📦",
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = if (filterType == null) "No orders yet" else "No ${filterType.getDisplayName().lowercase()} orders",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (filterType == null) "Start shopping to see your orders here" else "No orders with this status",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (filterType == null) {
            Button(
                onClick = onBrowseProducts,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Browse Products",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return "Placed on ${sdf.format(Date(timestamp))}"
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}