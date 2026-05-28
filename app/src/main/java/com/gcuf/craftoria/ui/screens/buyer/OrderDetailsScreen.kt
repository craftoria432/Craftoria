package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.ui.components.OrderDetailsDialog
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    onBackClick: () -> Unit,
    orderViewModel: OrderViewModel = viewModel()
) {
    val selectedOrder by orderViewModel.selectedOrder.collectAsState()
    var isLoading by remember(orderId) { mutableStateOf(true) }
    var loadFailed by remember(orderId) { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        isLoading = true
        loadFailed = false
        orderViewModel.loadOrderDetails(orderId)
    }

    LaunchedEffect(selectedOrder, orderId) {
        when {
            selectedOrder?.id == orderId -> {
                isLoading = false
                loadFailed = false
            }
        }
    }

    LaunchedEffect(orderId) {
        kotlinx.coroutines.delay(10_000)
        if (selectedOrder?.id != orderId) {
            isLoading = false
            loadFailed = selectedOrder == null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Order Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                loadFailed || selectedOrder == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Order not found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This order may have been removed or you don't have access.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onBackClick) {
                            Text("Go back")
                        }
                    }
                }
                else -> {
                    OrderDetailsDialog(
                        order = selectedOrder!!,
                        onDismiss = onBackClick
                    )
                }
            }
        }
    }
}
