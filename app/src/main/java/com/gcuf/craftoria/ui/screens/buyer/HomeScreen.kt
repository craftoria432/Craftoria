package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.ui.components.*
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.ProductViewModel

@Composable
fun HomeScreen(
    onNavigateToProduct: (Product) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: ProductViewModel = viewModel()
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedCategory by remember { mutableStateOf("All Products") }
    var cartCount by remember { mutableStateOf(0) }

    val categories = listOf(
        "All Products",
        "Textiles",
        "Jewelry",
        "Home Décor",
        "Embroidery",
        "Pottery"
    )

    val navItems = listOf(
        NavItem("Home", "🏠", "home"),
        NavItem("Cart", "🛒", "cart"),
        NavItem("Orders", "📦", "orders"),
        NavItem("Profile", "👤", "profile")
    )

    var selectedNavRoute by remember { mutableStateOf("home") }

    // Filter products by category
    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "All Products") products
        else products.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                cartCount = cartCount,
                onCartClick = onNavigateToCart,
                onSearchClick = { /* TODO: Navigate to search */ }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                items = navItems,
                selectedRoute = selectedNavRoute,
                onItemClick = { route ->
                    selectedNavRoute = route
                    when (route) {
                        "cart" -> onNavigateToCart()
                        "orders" -> onNavigateToOrders()
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
                .background(Color.White)
        ) {
            // Banner Carousel
            BannerCarousel()

            // Category Tabs
            CategoryTabs(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            // Product Grid
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                filteredProducts.isEmpty() -> {
                    EmptyProductsState()
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(15.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProducts) { product ->
                            ProductCard(
                                product = product,
                                onProductClick = onNavigateToProduct,
                                onAddToCart = {
                                    cartCount++
                                    // TODO: Add to cart in ViewModel
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    cartCount: Int,
    onCartClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧵 Craftoria",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                SearchBar(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 15.dp)
                )

                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = Error,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = cartCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    IconButton(onClick = onCartClick) {
                        Text(text = "🛒", fontSize = 22.sp)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Primary, PrimaryLight)
                )
            )
            .height(60.dp)
    )
}

@Composable
fun BannerCarousel() {
    var currentBanner by remember { mutableStateOf(0) }
    val banners = listOf(
        "🎨 Featured Products",
        "✨ New Arrivals",
        "🔥 Hot Deals"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Primary, PrimaryLight)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = banners[currentBanner],
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(banners.size) { index ->
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (index == currentBanner) Color.White
                    else Color.White.copy(alpha = 0.4f)
                ) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        currentBanner = (currentBanner + 1) % banners.size
    }
}

@Composable
fun EmptyProductsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📦", fontSize = 80.sp, modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = "No products found",
            fontSize = 16.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Button(
            onClick = { /* TODO: Refresh */ },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Refresh")
        }
    }
}