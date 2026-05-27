package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import com.gcuf.craftoria.ui.components.FilterTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.repository.ProductFilter
import com.gcuf.craftoria.data.repository.ProductSort
import com.gcuf.craftoria.ui.components.EmptyStates
import com.gcuf.craftoria.ui.components.ManageProductCard
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Error
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.ui.theme.Success
import com.gcuf.craftoria.ui.theme.TextLight
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary
import com.gcuf.craftoria.viewmodel.ManageProductsState
import com.gcuf.craftoria.viewmodel.ManageProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProductsScreen(
    user: User,
    onBackClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (Product) -> Unit,
    onProductClick: (Product) -> Unit,
    manageProductsViewModel: ManageProductsViewModel = viewModel()
) {
    val context = LocalContext.current

    val uiState by manageProductsViewModel.uiState.collectAsState()
    val products by manageProductsViewModel.products.collectAsState()
    val currentFilter by manageProductsViewModel.currentFilter.collectAsState()
    val currentSort by manageProductsViewModel.currentSort.collectAsState()
    val searchQuery by manageProductsViewModel.searchQuery.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var statsProduct by remember { mutableStateOf<Product?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.id) {
        manageProductsViewModel.loadProducts(user.id)
    }

    LaunchedEffect(uiState) {
        if (uiState is ManageProductsState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as ManageProductsState.Error).message,
                duration = SnackbarDuration.Short
            )
            manageProductsViewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "My Products",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        if (products.isNotEmpty()) {
                            Text(
                                text = "Manage your listings",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 13.sp
                            )
                        }
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
                    // Product count pill — consistent top bar action style
                    if (products.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "${products.size} products",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                .background(BackgroundSecondary)
                .padding(paddingValues)
        ) {
            // ── Add Product Button — gradient fill ────────────────────────
            Button(
                onClick = onAddProductClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .height(46.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add New Product",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // ── Filter Tabs — white surface with 0.5.dp bottom divider ───
            Surface(
                color = Color.White,
                shadowElevation = 0.dp,
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    FilterTabs(
                        currentFilter = currentFilter,
                        onFilterSelected = { filter ->
                            manageProductsViewModel.filterProducts(filter, user.id)
                        }
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Search and Sort ───────────────────────────────────────────
            SearchAndSort(
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    manageProductsViewModel.searchProducts(query, user.id)
                },
                currentSort = currentSort,
                onSortSelected = { sort ->
                    manageProductsViewModel.sortProducts(sort, user.id)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Product Grid or State ─────────────────────────────────────
            when (uiState) {
                is ManageProductsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                is ManageProductsState.Empty -> {
                    EmptyStates.NoSellerProducts(onAddProductClick = onAddProductClick)
                }

                is ManageProductsState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 4.dp,
                            bottom = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(products, key = { it.id }) { product ->
                            ManageProductCard(
                                product = product,
                                onToggleStatus = {
                                    manageProductsViewModel.toggleProductStatus(
                                        product.id,
                                        product.isActive
                                    )
                                },
                                onStockIncrement = {
                                    manageProductsViewModel.updateStock(
                                        product.id,
                                        product.stock + 1
                                    )
                                },
                                onStockDecrement = {
                                    if (product.stock > 0) {
                                        manageProductsViewModel.updateStock(
                                            product.id,
                                            product.stock - 1
                                        )
                                    }
                                },
                                onEdit = { onEditProductClick(product) },
                                onViewAsBuyer = { onProductClick(product) },
                                onViewStats = {
                                    statsProduct = product
                                    showStatsDialog = true
                                },
                                onDelete = {
                                    productToDelete = product
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }

    // ── Delete Product Dialog ─────────────────────────────────────────────
    if (showDeleteDialog && productToDelete != null) {
        DeleteProductDialog(
            product = productToDelete!!,
            onConfirm = {
                manageProductsViewModel.deleteProduct(productToDelete!!.id)
                showDeleteDialog = false
                productToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                productToDelete = null
            }
        )
    }

    // ── Product Stats Dialog ──────────────────────────────────────────────
    if (showStatsDialog && statsProduct != null) {
        ProductStatsDialog(
            product = statsProduct!!,
            onDismiss = {
                showStatsDialog = false
                statsProduct = null
            }
        )
    }
}

// ── Filter Tabs ───────────────────────────────────────────────────────────────
// ✅ STANDARDIZED: Uses FilterTabRow with consistent styling

@Composable
fun FilterTabs(
    currentFilter: ProductFilter,
    onFilterSelected: (ProductFilter) -> Unit
) {
    val filters = listOf(
        ProductFilter.ALL to "All",
        ProductFilter.ACTIVE to "Active",
        ProductFilter.INACTIVE to "Inactive",
        ProductFilter.OUT_OF_STOCK to "Out of Stock",
        ProductFilter.DRAFTS to "Drafts",
        ProductFilter.PENDING to "Pending"
    )

    val selectedIndex = filters.indexOfFirst { it.first == currentFilter }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        FilterTabRow(
            tabs = filters.map { it.second },
            selectedIndex = selectedIndex,
            onTabSelected = { index -> onFilterSelected(filters[index].first) },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

// ── Search and Sort ───────────────────────────────────────────────────────────

@Composable
fun SearchAndSort(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentSort: ProductSort,
    onSortSelected: (ProductSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Bar — 0.5.dp BorderColor
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(0.5.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(15.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search your products",
                            fontSize = 13.sp,
                            color = TextLight
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(Primary)
                    )
                }
            }
        }

        // Sort Dropdown — 0.5.dp BorderColor
        var expanded by remember { mutableStateOf(false) }
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(0.5.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (currentSort) {
                            ProductSort.NEWEST -> "Newest"
                            ProductSort.OLDEST -> "Oldest"
                            ProductSort.PRICE_HIGH -> "Price ↓"
                            ProductSort.PRICE_LOW -> "Price ↑"
                            ProductSort.NAME -> "Name"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ProductSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (sort) {
                                    ProductSort.NEWEST -> "Newest First"
                                    ProductSort.OLDEST -> "Oldest First"
                                    ProductSort.PRICE_HIGH -> "Price: High to Low"
                                    ProductSort.PRICE_LOW -> "Price: Low to High"
                                    ProductSort.NAME -> "Name A-Z"
                                },
                                fontSize = 13.sp
                            )
                        },
                        onClick = {
                            onSortSelected(sort)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ── Badges ────────────────────────────────────────────────────────────────────

@Composable
fun StockBadge(stock: Int) {
    val (text, backgroundColor, textColor) = when {
        stock == 0 -> Triple("Out of Stock", Error.copy(alpha = 0.10f), Error)
        stock <= 5 -> Triple("Low Stock", Color(0xFFFFA500).copy(alpha = 0.15f), Color(0xFFFFA500))
        else -> Triple("In Stock", Success.copy(alpha = 0.10f), Success)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        modifier = Modifier
            .wrapContentSize()
            .heightIn(min = 20.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ApprovalBadge(status: String) {
    val (backgroundColor, textColor, label) = when (status) {
        "pending" -> Triple(
            Color(0xFFFFA500).copy(alpha = 0.15f),
            Color(0xFFFFA500),
            "Pending"
        )
        "rejected" -> Triple(Error.copy(alpha = 0.10f), Error, "Rejected")
        else -> Triple(Success.copy(alpha = 0.10f), Success, "Approved")
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        modifier = Modifier
            .wrapContentSize()
            .heightIn(min = 20.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal data class ApprovalBadgeData(
    val text: String,
    val backgroundColor: Color,
    val textColor: Color,
    val emoji: String
)

// ── Delete Dialog ─────────────────────────────────────────────────────────────

@Composable
fun DeleteProductDialog(
    product: Product,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Error.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete Product?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"${product.title}\"? This action cannot be undone.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        }
    )
}

// ── Stats Dialog ──────────────────────────────────────────────────────────────

@Composable
fun ProductStatsDialog(
    product: Product,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Product Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatRow(
                    label = "Views",
                    value = product.viewCount.toString(),
                    iconBg = Primary.copy(alpha = 0.08f),
                    valueColor = TextPrimary
                )
                StatRow(
                    label = "Likes",
                    value = product.likeCount.toString(),
                    iconBg = Primary.copy(alpha = 0.08f),
                    valueColor = TextPrimary
                )
                StatRow(
                    label = "Shares",
                    value = product.shareCount.toString(),
                    iconBg = Primary.copy(alpha = 0.08f),
                    valueColor = TextPrimary
                )
                StatRow(
                    label = "Sold",
                    value = product.soldCount.toString(),
                    iconBg = Success.copy(alpha = 0.08f),
                    valueColor = Success
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    )
}

// ── Stat Row ──────────────────────────────────────────────────────────────────

@Composable
fun StatRow(
    label: String,
    value: String,
    iconBg: Color = Primary.copy(alpha = 0.08f),
    valueColor: Color = TextPrimary
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = BackgroundSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}