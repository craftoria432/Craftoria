package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.CoSellerStore
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.ui.components.*
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.CartViewModel
import com.gcuf.craftoria.viewmodel.CoSellerStoreViewModel
import com.gcuf.craftoria.viewmodel.ProductViewModel
import com.gcuf.craftoria.viewmodel.WishlistViewModel
import com.gcuf.craftoria.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProduct: (Product) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToStore: (String) -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToWishlist: () -> Unit = {},
    onNavigateToAllStores: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    currentUserId: String = "",
    productViewModel: ProductViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    coSellerStoreViewModel: CoSellerStoreViewModel = viewModel(),
    wishlistViewModel: WishlistViewModel = viewModel(),
    unreadMessageViewModel: com.gcuf.craftoria.viewmodel.UnreadMessageViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel(),
    notificationViewModel: com.gcuf.craftoria.viewmodel.NotificationViewModel = viewModel()
) {
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            wishlistViewModel.initForUser(currentUserId)
            unreadMessageViewModel.startListening(currentUserId)
            orderViewModel.loadUserOrders(currentUserId)
            notificationViewModel.startListening(currentUserId)
        }
    }

    LaunchedEffect(Unit) {
        productViewModel.loadAllProducts()
        coSellerStoreViewModel.loadAllActiveStores()
    }

    val products by productViewModel.products.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val cartCount by cartViewModel.cartCount.collectAsState()
    val activeStores by coSellerStoreViewModel.activeStores.collectAsState()
    val wishlistCount by wishlistViewModel.wishlistCount.collectAsState()
    val unreadMessageCount by unreadMessageViewModel.unreadCount.collectAsState()
    val orders by orderViewModel.orders.collectAsState()
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

    var selectedCategory by remember { mutableStateOf("All Products") }
    var selectedNavRoute by remember { mutableStateOf("home") }

    val categories = listOf("All Products", "Textiles", "Jewelry", "Home Décor", "Embroidery", "Pottery")

    val pendingOrdersCount = remember(orders) {
        orders.count { it.status in listOf("pending", "processing", "shipped") }
    }

    val navItems = listOf(
        NavItem("Home", "", "home"),
        NavItem("Orders", "", "orders"),
        NavItem("Wishlist", "", "wishlist"),
        NavItem("Profile", "", "profile")
    )

    val filteredProducts = remember(products, selectedCategory) {
        val activeProducts = products.filter { product ->
            product.isActive && !(product.isRemoved ?: false)
        }
        if (selectedCategory == "All Products") activeProducts
        else activeProducts.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Craftoria",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    // Search
                    IconButton(onClick = onNavigateToSearch) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    // Notifications
                    BadgedBox(badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(containerColor = Error, contentColor = Color.White) {
                                Text(if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }) {
                        IconButton(
                            onClick = {
                                try {
                                    onNavigateToNotifications()
                                } catch (e: Exception) {
                                    android.util.Log.e("HomeScreen", "Navigation error to notifications", e)
                                }
                            }
                        ) {
                            Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    // Chats
                    BadgedBox(badge = {
                        if (unreadMessageCount > 0) {
                            Badge(containerColor = Error, contentColor = Color.White) {
                                Text(if (unreadMessageCount > 9) "9+" else unreadMessageCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }) {
                        IconButton(onClick = onNavigateToChats) {
                            Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Chats", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    // Cart
                    BadgedBox(badge = {
                        if (cartCount > 0) {
                            Badge(containerColor = Error, contentColor = Color.White) {
                                Text(cartCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }) {
                        IconButton(
                            onClick = onNavigateToCart,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White, modifier = Modifier.size(16.dp))
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
            BottomNavigationBar(
                items = navItems,
                selectedRoute = selectedNavRoute,
                wishlistCount = wishlistCount,
                pendingOrdersCount = pendingOrdersCount,
                onItemClick = { route ->
                    selectedNavRoute = route
                    when (route) {
                        "home" -> {}
                        "orders" -> {
                            try { onNavigateToOrders() } catch (e: Exception) {
                                android.util.Log.e("HomeScreen", "Navigation error to orders", e)
                            }
                        }
                        "wishlist" -> onNavigateToWishlist()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundSecondary)
                .verticalScroll(rememberScrollState())
        ) {
            // White card container for banner + stores + categories
            Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column {
                    BannerCarousel()

                    if (activeStores.isNotEmpty()) {
                        FeaturedStoresSection(
                            stores = activeStores.take(10),
                            onStoreClick = onNavigateToStore,
                            onViewAllClick = onNavigateToAllStores
                        )
                    }

                    CategoryTabs(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                filteredProducts.isEmpty() -> {
                    EmptyProductsState(onRefresh = { productViewModel.loadAllProducts() })
                }
                else -> {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredProducts.chunked(2).forEach { rowProducts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowProducts.forEach { product ->
                                    ProductCard(
                                        product = product,
                                        onProductClick = onNavigateToProduct,
                                        onAddToCart = { prod ->
                                            cartViewModel.addToCart(
                                                userId = currentUserId,
                                                product = prod,
                                                price = prod.price,
                                                isNegotiated = false,
                                                negotiationStatus = null
                                            )
                                        },
                                        wishlistViewModel = wishlistViewModel,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowProducts.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Featured Stores ───────────────────────────────────────────────────────────

@Composable
fun FeaturedStoresSection(
    stores: List<CoSellerStore>,
    onStoreClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Featured Stores", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.3).sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.clickable { onViewAllClick() }
            ) {
                Text(text = "View All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Primary, modifier = Modifier.size(10.dp))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stores) { store ->
                StoreCard(store = store, onClick = { onStoreClick(store.id) })
            }
        }
    }
}

fun isNewStore(createdAt: Any?): Boolean {
    if (createdAt == null) return false
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
    val createdTime = when (createdAt) { is Long -> createdAt; else -> 0L }
    return createdTime >= sevenDaysAgo
}

@Composable
fun StoreCard(store: CoSellerStore, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Store logo
            Box(
                modifier = Modifier.fillMaxWidth().height(70.dp).background(BackgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (store.storeLogo.isNotEmpty()) {
                    AsyncImage(
                        model = CloudinaryManager.getOptimizedUrl(store.storeLogo, 160, 120),
                        contentDescription = store.storeName,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(50.dp).background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)), CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = store.storeName.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                // NEW badge
                if (isNewStore(store.createdAt)) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
                    ) {
                        Text(text = "NEW", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }
            // Info
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = store.storeName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(text = "${store.productCount} products", fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                // Rating
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFF3E0), modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(5.dp)) {
                        Text("⭐", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        if (store.averageRating > 0) {
                            Text("%.1f".format(store.averageRating), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(" (${store.ratingCount})", fontSize = 9.sp, color = TextSecondary)
                        } else {
                            Text("New", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(cartCount: Int, onCartClick: () -> Unit, onSearchClick: () -> Unit) {
    TopAppBar(
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🧵 Craftoria", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                SearchBar(onClick = onSearchClick, modifier = Modifier.weight(1f).padding(horizontal = 15.dp))
                BadgedBox(badge = {
                    if (cartCount > 0) { Badge(containerColor = Error, contentColor = Color.White) { Text(text = cartCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                }) {
                    IconButton(onClick = onCartClick) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))).height(60.dp)
    )
}