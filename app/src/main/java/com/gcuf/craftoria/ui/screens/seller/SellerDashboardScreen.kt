package com.gcuf.craftoria.ui.screens.seller

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.Activity
import com.gcuf.craftoria.data.model.DashboardStats
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.getTimestampLong
import com.gcuf.craftoria.ui.components.SellerBottomNavigation
import com.gcuf.craftoria.ui.components.formatPrice
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Error
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.ui.theme.Success
import com.gcuf.craftoria.ui.theme.TextLight
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary
import com.gcuf.craftoria.viewmodel.DashboardViewModel
import com.gcuf.craftoria.data.model.NegotiationStatus
import com.gcuf.craftoria.data.model.RefundStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    user: User,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToNegotiations: () -> Unit,
    onNavigateToCoSeller: () -> Unit,
    onNavigateToLearning: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToPayments: () -> Unit = {},
    onNavigateToRefunds: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel(),
    notificationViewModel: com.gcuf.craftoria.viewmodel.NotificationViewModel = viewModel(),
    unreadMessagesCount: Int = 0
) {
    val dashboardStats by dashboardViewModel.dashboardStats.collectAsState()
    val recentActivities by dashboardViewModel.recentActivities.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()
    
    // ✅ NEW: Collect real-time metrics
    val productCount by dashboardViewModel.productCount.collectAsState()
    val totalOrdersCount by dashboardViewModel.totalOrdersCount.collectAsState()  // ✅ Changed from pendingOrdersCount
    val totalEarnings by dashboardViewModel.totalEarnings.collectAsState()
    val newOrdersCount by dashboardViewModel.newOrdersCount.collectAsState() // For badge

    var pendingNegotiationsCount by remember { mutableStateOf(0) }
    var pendingInvitationsCount by remember { mutableStateOf(0) }
    var pendingApprovalsCount by remember { mutableStateOf(0) }
    var pendingPayoutsCount by remember { mutableStateOf(0) }
    var pendingRefundsCount by remember { mutableStateOf(0) }
    var selectedRoute by remember { mutableStateOf("seller_dashboard") }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(user.id) {
        Log.d("SellerDashboard", "🚀 LaunchedEffect started for user: ${user.id}")

        dashboardViewModel.loadDashboardData(user.id)

        notificationViewModel.loadNotifications(user.id)
        notificationViewModel.startListening(user.id)

        launch {
            try {
                // ✅ REMOVED: Auto-adding fake payment data
                // This was causing deleted payments to reappear with new IDs
                // Payments should only be created from actual orders, not sample data
                
                val productSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("products")
                    .whereEqualTo("seller_id", user.id)
                    .limit(1)
                    .get()
                    .await()

                // ✅ FIX: Removed automatic sample product creation
                // Sellers must manually add their own products
                if (productSnapshot.isEmpty) {
                    Log.d("SellerDashboard", "No products found - seller needs to add products manually")
                }
            } catch (e: Exception) {
                Log.e("SellerDashboard", "Error checking/adding sample data", e)
            }
        }

        val negotiationsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("negotiations")
            .whereEqualTo("seller_id", user.id)
            .whereEqualTo("status", NegotiationStatus.PENDING.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) pendingNegotiationsCount = snapshot.size()
            }

        val payoutsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("payments")  // ✅ FIXED: Changed from "seller_payments" to "payments"
            .whereEqualTo("seller_id", user.id)
            .whereEqualTo("status", "processing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) pendingPayoutsCount = snapshot.size()
            }

        // ✅ Real-time pending refunds count listener
        val refundsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("refunds")
            .whereEqualTo("seller_id", user.id)
            .whereEqualTo("status", RefundStatus.REQUESTED.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) pendingRefundsCount = snapshot.size()
            }

        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            negotiationsListener.remove()
            payoutsListener.remove()
            refundsListener.remove()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            dashboardViewModel.refreshDashboard(user.id)
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    // Dashboard icon in tinted circle — consistent with all other top bars
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "My Dashboard",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Hello, ${user.name.split(" ").firstOrNull() ?: user.name}",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
                        )
                    }
                },
                actions = {
                    // Messages icon in 32.dp tinted circle with badge
                    BadgedBox(
                        badge = {
                            if (unreadMessagesCount > 0) {
                                Badge(containerColor = Error, contentColor = Color.White) {
                                    Text(
                                        text = if (unreadMessagesCount > 9) "9+" else unreadMessagesCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToMessages) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Message,
                                    contentDescription = "Messages",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    // Notifications icon in 32.dp tinted circle with badge
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(containerColor = Error, contentColor = Color.White) {
                                    Text(
                                        text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNavigateToNotifications,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
                )
            )
        },
        bottomBar = {
            SellerBottomNavigation(
                selectedRoute = selectedRoute,
                newOrdersCount = newOrdersCount,
                onNavigate = { route ->
                    selectedRoute = route
                    when (route) {
                        "seller_dashboard" -> {}
                        "add_product" -> onNavigateToAddProduct()
                        "orders" -> onNavigateToOrders()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    WelcomeBanner(
                        sellerId = user.id,
                        sellerName = user.name,
                        isVerified = user.verified,
                        onNavigateToPayments = onNavigateToPayments,
                        productCount = productCount,
                        totalOrdersCount = totalOrdersCount,  // ✅ Changed from pendingOrdersCount
                        totalEarnings = totalEarnings
                    )
                }
                item {
                    QuickAccessMenu(
                        onManageProducts = onNavigateToProducts,
                        onNegotiations = onNavigateToNegotiations,
                        onCoSeller = onNavigateToCoSeller,
                        onLearning = onNavigateToLearning,
                        onPayments = onNavigateToPayments,
                        onRefunds = onNavigateToRefunds,
                        pendingNegotiationsCount = pendingNegotiationsCount,
                        pendingRefundsCount = pendingRefundsCount
                    )
                }
                item {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    // ✅ REMOVED: Sales Overview section is redundant with Welcome Banner
                    // The Welcome Banner now displays real-time metrics (Products, Orders, This Month's Sales)
                    // which are the same metrics shown in Sales Overview. Removing this section reduces
                    // visual clutter and eliminates the real-time update lag issue.
                }
                item {
                    RecentActivitySection(
                        activities = recentActivities,
                        onViewAll = onNavigateToActivity
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeBanner(
    sellerId: String = "",
    sellerName: String,
    isVerified: Boolean,
    onNavigateToPayments: () -> Unit = {},
    productCount: Int = 0,
    totalOrdersCount: Int = 0,  // ✅ Changed from pendingOrdersCount
    totalEarnings: Double = 0.0
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFC2185B), Primary, PrimaryLight),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    RoundedCornerShape(16.dp)
                )
        ) {
            // ── Decorative crosshatch canvas ──────────────────────────────────
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxWidth().height(280.dp)
            ) {
                val lineColor = Color.White.copy(alpha = 0.07f)
                val strokeW = 1.2f
                val step = 28.dp.toPx()
                val w = size.width
                val h = size.height
                var x = -h
                while (x <= w + h) {
                    drawLine(color = lineColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x + h, h), strokeWidth = strokeW)
                    x += step
                }
                x = -h
                while (x <= w + h) {
                    drawLine(color = lineColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x - h, h), strokeWidth = strokeW)
                    x += step
                }
            }

            // ── Decorative orbs ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 35.dp, y = (-35).dp)
                    .background(Color.White.copy(alpha = 0.07f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-25).dp, y = 25.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )

            Column(modifier = Modifier.padding(18.dp)) {
                // ── Top row: greeting only (no store icon) ────────────────────
                Column {
                    Text(
                        text = "Welcome back,",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    // ✅ Display real-time name
                    if (sellerId.isNotEmpty()) {
                        com.gcuf.craftoria.ui.components.RealtimeNameDisplay(
                            userId = sellerId,
                            fallbackName = sellerName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = sellerName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (isVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.20f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Text(text = "Verified Seller", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))

                // ── Stats row — all 3 cards forced to same fixed height ────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Products
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f).height(80.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = productCount.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Products",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Orders
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f).height(80.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = totalOrdersCount.toString(),  // ✅ Changed from pendingOrdersCount
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Orders",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Earnings — PKR as micro-label above number, same height as others
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f).height(80.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "PKR",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatPrice(totalEarnings),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Total",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Payments CTA — white outline style ────────────────────────
                Surface(
                    onClick = onNavigateToPayments,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, Color.White.copy(alpha = 0.30f)
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Payments & Earnings",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "View your payment history",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(14.dp).rotate(180f)
                        )
                    }
                }


            }
        }
    }
}


// ── Quick Access ──────────────────────────────────────────────────────────────

@Composable
fun QuickAccessMenu(
    onManageProducts: () -> Unit,
    onNegotiations: () -> Unit,
    onCoSeller: () -> Unit,
    onLearning: () -> Unit,
    onPayments: () -> Unit = {},
    onRefunds: () -> Unit = {},
    pendingNegotiationsCount: Int = 0,
    pendingRefundsCount: Int = 0
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Quick Access",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAccessCardWithIcon(
                icon = Icons.Default.Inventory,
                text = "Manage Products",
                color = "pink",
                onClick = onManageProducts,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCardWithIcon(
                icon = Icons.Default.LocalOffer,
                text = "Price Offers",
                color = "blue",
                onClick = onNegotiations,
                modifier = Modifier.weight(1f),
                badgeCount = pendingNegotiationsCount
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAccessCardWithIcon(
                icon = Icons.Default.Store,
                text = "Co-Seller Stores",
                color = "gold",
                onClick = onCoSeller,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCardWithIcon(
                icon = Icons.Default.School,
                text = "Learning Resources",
                color = "cream",
                onClick = onLearning,
                modifier = Modifier.weight(1f)
            )
        }
        
        // ══════════════════════════════════════════════════════════════════════════════
        // Refund Management Card
        // ══════════════════════════════════════════════════════════════════════════════
        Card(
            onClick = onRefunds,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (pendingRefundsCount > 0) Error.copy(alpha = 0.10f)
                                else Primary.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (pendingRefundsCount > 0) Error else Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Refund Requests",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (pendingRefundsCount > 0)
                                if (pendingRefundsCount == 1) "$pendingRefundsCount Pending Action" else "$pendingRefundsCount Pending Actions"
                            else "No pending requests",
                            fontSize = 12.sp,
                            color = if (pendingRefundsCount > 0) Error else TextSecondary
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Red badge - only shown when there are pending refunds
                    if (pendingRefundsCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pendingRefundsCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAccessCardWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val backgroundColor = when (color) {
        "pink" -> Color(0xFFFFF5F8)
        "cream" -> Color(0xFFFFF8E1)
        "gold" -> Color(0xFFFFF9C4)
        "blue" -> Color(0xFFE3F2FD)
        "green" -> Color(0xFFE8F5E8)
        else -> Color.White
    }
    val borderColor = when (color) {
        "pink" -> Color(0xFFFCE4EC)
        "cream" -> Color(0xFFFFECB3)
        "gold" -> Color(0xFFFFF176)
        "blue" -> Color(0xFFBBDEFB)
        "green" -> Color(0xFFC8E6C9)
        else -> BorderColor
    }
    val iconBg = when (color) {
        "pink" -> Color(0xFFFCE4EC)
        "cream" -> Color(0xFFFFECB3)
        "gold" -> Color(0xFFFFF176)
        "blue" -> Color(0xFFBBDEFB)
        "green" -> Color(0xFFC8E6C9)
        else -> BackgroundSecondary
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.height(96.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Badge — top-right corner, 0.5.dp consistent with system
            if (badgeCount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Error,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Recent Activity ───────────────────────────────────────────────────────────

@Composable
fun RecentActivitySection(activities: List<Activity>, onViewAll: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayedActivities = if (isExpanded) activities else activities.take(5)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Recent Activity",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        if (activities.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Empty state: tinted circle icon — consistent with all other empties
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Primary.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "No recent activity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your recent orders, payments and product updates will appear here",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            Text(
                text = if (isExpanded) "Showing all ${activities.size} activities"
                else "Showing ${displayedActivities.size} of ${activities.size} activities",
                fontSize = 11.sp,
                color = TextLight
            )

            displayedActivities.forEach { activity ->
                ActivityItem(activity = activity)
            }

            if (activities.size > 5) {
                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show Less ▲" else "View All (${activities.size}) ▼",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: Activity) {
    val (bgColor, iconColor, icon) = when (activity.type) {
        "NEW_ORDER", "ORDER_CONFIRMED" -> Triple(
            Color(0xFFE3F2FD), Color(0xFF1976D2), Icons.Default.ShoppingCart
        )
        "PRODUCT_ADDED", "PRODUCT_UPDATED", "PRODUCT_APPROVED" -> Triple(
            Color(0xFFE8F5E8), Color(0xFF388E3C), Icons.Default.Inventory
        )
        "ORDER_SHIPPED", "ORDER_OUT_FOR_DELIVERY" -> Triple(
            Color(0xFFF3E5F5), Color(0xFF7B1FA2), Icons.Default.LocalShipping
        )
        "ORDER_PROCESSING" -> Triple(
            Color(0xFFFFF3E0), Color(0xFFFF9800), Icons.Default.Settings
        )
        "ORDER_DELIVERED" -> Triple(
            Color(0xFFE8F5E8), Color(0xFF4CAF50), Icons.Default.CheckCircle
        )
        "PAYMENT_RECEIVED", "PAYOUT_PROCESSED" -> Triple(
            Color(0xFFE0F2F1), Color(0xFF00796B), Icons.Default.Payment
        )
        "STORE_RATING_RECEIVED" -> Triple(
            Color(0xFFFFFDE7), Color(0xFFF57F17), Icons.Default.Star
        )
        "NEGOTIATION_REQUEST" -> Triple(
            Color(0xFFE1F5FE), Color(0xFF0277BD), Icons.Default.LocalOffer
        )
        "PRODUCT_SOLD_OUT", "LOW_STOCK_ALERT" -> Triple(
            Color(0xFFFFF3E0), Color(0xFFFF9800), Icons.Default.Warning
        )
        "STOCK_REPLENISHED" -> Triple(
            Color(0xFFE8F5E8), Color(0xFF4CAF50), Icons.Default.AddCircle
        )
        "PRODUCT_REJECTED" -> Triple(
            Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Default.Cancel
        )
        "ACCOUNT_VERIFIED", "PROFILE_UPDATED", "SETTINGS_CHANGED" -> Triple(
            Color(0xFFF5F5F5), Color(0xFF757575), Icons.Default.Info
        )
        else -> Triple(
            Color(0xFFF5F5F5), Color(0xFF757575), Icons.Default.Info
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color-coded activity icon in 40.dp tinted circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activity.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = formatActivityTime(activity.getTimestampLong()),
                    fontSize = 11.sp,
                    color = TextLight,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

fun formatActivityTime(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestampMillis))
    }
}