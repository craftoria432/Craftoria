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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
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
    dashboardViewModel: DashboardViewModel = viewModel(),
    notificationViewModel: com.gcuf.craftoria.viewmodel.NotificationViewModel = viewModel(),
    unreadMessagesCount: Int = 0
) {
    val dashboardStats by dashboardViewModel.dashboardStats.collectAsState()
    val recentActivities by dashboardViewModel.recentActivities.collectAsState()
    val isLoading by dashboardViewModel.isLoading.collectAsState()
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

    var newOrdersCount by remember { mutableStateOf(0) }
    var pendingNegotiationsCount by remember { mutableStateOf(0) }
    var pendingInvitationsCount by remember { mutableStateOf(0) }
    var pendingApprovalsCount by remember { mutableStateOf(0) }
    var pendingPayoutsCount by remember { mutableStateOf(0) }
    var selectedRoute by remember { mutableStateOf("seller_dashboard") }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(user.id) {
        Log.d("SellerDashboard", "🚀 LaunchedEffect started for user: ${user.id}")

        dashboardViewModel.loadDashboardData(user.id)

        notificationViewModel.loadNotifications(user.id)
        notificationViewModel.startListening(user.id)

        launch {
            try {
                val paymentSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("seller_payments")
                    .whereEqualTo("seller_id", user.id)
                    .limit(1)
                    .get()
                    .await()

                if (paymentSnapshot.isEmpty) {
                    Log.d("SellerDashboard", "No payment data found, adding sample data...")
                    com.gcuf.craftoria.utils.DashboardDataHelper.setupPaymentDataOnly(user.id, user.name)
                    dashboardViewModel.loadDashboardData(user.id)
                }

                val productSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("products")
                    .whereEqualTo("seller_id", user.id)
                    .limit(1)
                    .get()
                    .await()

                if (productSnapshot.isEmpty) {
                    Log.d("SellerDashboard", "No products found, adding sample products...")
                    com.gcuf.craftoria.utils.DashboardDataHelper.addSellerProducts(user.id, user.name, user.verified)
                    dashboardViewModel.loadDashboardData(user.id)
                }
            } catch (e: Exception) {
                Log.e("SellerDashboard", "Error checking/adding sample data", e)
            }
        }

        val ordersListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("orders")
            .whereEqualTo("seller_id", user.id)
            .whereIn("status", listOf("pending", "confirmed"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    newOrdersCount = snapshot.documents.count { doc ->
                        doc.getBoolean("is_viewed") != true
                    }
                }
            }

        val negotiationsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("negotiations")
            .whereEqualTo("seller_id", user.id)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) pendingNegotiationsCount = snapshot.size()
            }

        val invitationsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("store_invitations")
            .whereEqualTo("invitee_id", user.id)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) pendingInvitationsCount = snapshot.size()
            }

        val approvalsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("products")
            .whereEqualTo("seller_id", user.id)
            .whereEqualTo("approval_status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) pendingApprovalsCount = snapshot.size()
            }

        val payoutsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("seller_payments")
            .whereEqualTo("seller_id", user.id)
            .whereEqualTo("status", "processing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) pendingPayoutsCount = snapshot.size()
            }

        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            ordersListener.remove()
            negotiationsListener.remove()
            invitationsListener.remove()
            approvalsListener.remove()
            payoutsListener.remove()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            dashboardViewModel.loadDashboardData(user.id)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Hello, ${user.name.split(" ").firstOrNull() ?: user.name}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 12.sp
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
                    top = 16.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WelcomeBanner(
                        sellerName = user.name,
                        isVerified = user.verified,
                        onNavigateToPayments = onNavigateToPayments
                    )
                }
                item {
                    QuickAccessMenu(
                        onManageProducts = onNavigateToProducts,
                        onNegotiations = onNavigateToNegotiations,
                        onCoSeller = onNavigateToCoSeller,
                        onLearning = onNavigateToLearning,
                        onPayments = onNavigateToPayments,
                        pendingNegotiationsCount = pendingNegotiationsCount
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
                    } else {
                        val stats = dashboardStats ?: DashboardStats(
                            totalSales = 0.0,
                            activeOrders = 0,
                            pendingOrders = 0,
                            processingOrders = 0,
                            totalProducts = 0,
                            monthSales = 0.0,
                            salesGrowth = 0.0,
                            productsThisWeek = 0
                        )
                        SalesOverview(stats = stats)
                    }
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
    sellerName: String,
    isVerified: Boolean,
    stats: DashboardStats? = null,
    onNavigateToPayments: () -> Unit = {}
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
                // ── Top row: greeting + store icon ────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Welcome back,",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.82f),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(
                            text = sellerName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Stats mini-cards row — 3 columns ─────────────────────────
                if (stats != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(
                            Triple("Orders",   stats.activeOrders.toString(),   Icons.Default.ShoppingCart),
                            Triple("Products", stats.totalProducts.toString(),  Icons.Default.Inventory),
                            Triple("This Mo.", "PKR ${formatPrice(stats.monthSales)}", Icons.Default.Payment)
                        ).forEach { (label, value, icon) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.78f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Payments CTA ──────────────────────────────────────────────
                Surface(
                    onClick = onNavigateToPayments,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.20f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Payments & Earnings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(text = "View payment history and earnings", fontSize = 11.sp, color = Color.White.copy(alpha = 0.80f))
                        }
                        // Correct direction: use rotate instead of wrong ArrowBack
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(16.dp).rotate(180f)
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
    pendingNegotiationsCount: Int = 0
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
    }
}

@Composable
fun QuickAccessCard(
    icon: String,
    text: String,
    color: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (color) {
        "pink" -> Color(0xFFFFF5F8)
        "cream" -> Color(0xFFFFF8E1)
        "gold" -> Color(0xFFFFF9C4)
        "blue" -> Color(0xFFE3F2FD)
        else -> Color.White
    }
    val borderColor = when (color) {
        "pink" -> Color(0xFFFCE4EC)
        "cream" -> Color(0xFFFFECB3)
        "gold" -> Color(0xFFFFF176)
        "blue" -> Color(0xFFBBDEFB)
        else -> BorderColor
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.height(96.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 28.sp, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
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

// ── Sales Overview ────────────────────────────────────────────────────────────

@Composable
fun SalesOverview(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Sales Overview",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SalesCard(
                    label = "Total Sales",
                    value = "PKR ${formatPrice(stats.totalSales)}",
                    indicator = "↑ ${stats.salesGrowth.toInt()}% vs last month",
                    indicatorType = "positive"
                )
            }
            item {
                SalesCard(
                    label = "Active Orders",
                    value = stats.activeOrders.toString(),
                    indicator = "${stats.pendingOrders} pending, ${stats.processingOrders} processing",
                    indicatorType = "neutral"
                )
            }
            item {
                SalesCard(
                    label = "Total Products",
                    value = stats.totalProducts.toString(),
                    indicator = "↑ ${stats.productsThisWeek} added this week",
                    indicatorType = if (stats.productsThisWeek > 0) "positive" else "neutral"
                )
            }
            item {
                SalesCard(
                    label = "This Month's Sale",
                    value = "PKR ${formatPrice(stats.monthSales)}",
                    indicator = "↑ 18% growth",
                    indicatorType = "positive"
                )
            }
        }
    }
}

@Composable
fun SalesCard(
    label: String,
    value: String,
    indicator: String,
    indicatorType: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .width(148.dp)
            .heightIn(min = 108.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 5.dp)
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 6.dp))
            Text(
                text = indicator,
                fontSize = 11.sp,
                color = when (indicatorType) {
                    "positive" -> Success
                    "negative" -> Error
                    else -> TextSecondary
                },
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
    val (bgColor, iconColor, icon, emoji) = when (activity.type) {
        "NEW_ORDER", "ORDER_CONFIRMED" -> Quadruple(
            Color(0xFFE3F2FD), Color(0xFF1976D2), Icons.Default.ShoppingCart, "🛒"
        )
        "PRODUCT_ADDED", "PRODUCT_UPDATED", "PRODUCT_APPROVED" -> Quadruple(
            Color(0xFFE8F5E8), Color(0xFF388E3C), Icons.Default.Inventory, "📦"
        )
        "ORDER_SHIPPED", "ORDER_OUT_FOR_DELIVERY" -> Quadruple(
            Color(0xFFF3E5F5), Color(0xFF7B1FA2), Icons.Default.LocalShipping, "🚚"
        )
        "ORDER_PROCESSING" -> Quadruple(
            Color(0xFFFFF3E0), Color(0xFFFF9800), Icons.Default.Settings, "⚙️"
        )
        "ORDER_DELIVERED" -> Quadruple(
            Color(0xFFE8F5E8), Color(0xFF4CAF50), Icons.Default.CheckCircle, "✅"
        )
        "PAYMENT_RECEIVED", "PAYOUT_PROCESSED" -> Quadruple(
            Color(0xFFE0F2F1), Color(0xFF00796B), Icons.Default.Payment, "💰"
        )
        "STORE_RATING_RECEIVED" -> Quadruple(
            Color(0xFFFFFDE7), Color(0xFFF57F17), Icons.Default.Star, "⭐"
        )
        "NEGOTIATION_REQUEST" -> Quadruple(
            Color(0xFFE1F5FE), Color(0xFF0277BD), Icons.Default.LocalOffer, "💬"
        )
        "PRODUCT_SOLD_OUT", "LOW_STOCK_ALERT" -> Quadruple(
            Color(0xFFFFF3E0), Color(0xFFFF9800), Icons.Default.Warning, "⚠️"
        )
        "STOCK_REPLENISHED" -> Quadruple(
            Color(0xFFE8F5E8), Color(0xFF4CAF50), Icons.Default.AddCircle, "➕"
        )
        "PRODUCT_REJECTED" -> Quadruple(
            Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Default.Cancel, "❌"
        )
        "ACCOUNT_VERIFIED", "PROFILE_UPDATED", "SETTINGS_CHANGED" -> Quadruple(
            Color(0xFFF5F5F5), Color(0xFF757575), Icons.Default.Info, "ℹ️"
        )
        else -> Quadruple(
            Color(0xFFF5F5F5), Color(0xFF757575), Icons.Default.Info, "ℹ️"
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

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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