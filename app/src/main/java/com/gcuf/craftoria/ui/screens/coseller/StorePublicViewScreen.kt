package com.gcuf.craftoria.ui.screens.coseller

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory2
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
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.StoreMember
import com.gcuf.craftoria.ui.components.RateStoreDialog
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.CoSellerStoreState
import com.gcuf.craftoria.viewmodel.CoSellerStoreViewModel
import com.gcuf.craftoria.viewmodel.StoreRatingState
import com.gcuf.craftoria.viewmodel.StoreRatingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorePublicViewScreen(
    storeId: String,
    onBackClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    currentUserId: String = "",
    currentUserName: String = "",
    coSellerStoreViewModel: CoSellerStoreViewModel = viewModel(),
    storeRatingViewModel: StoreRatingViewModel = viewModel()
) {
    val uiState by coSellerStoreViewModel.uiState.collectAsState()
    val currentStore by coSellerStoreViewModel.currentStore.collectAsState()
    val storeMembers by coSellerStoreViewModel.storeMembers.collectAsState()
    val storeProducts by coSellerStoreViewModel.storeProducts.collectAsState()

    val ratingState by storeRatingViewModel.ratingState.collectAsState()
    val buyerRating by storeRatingViewModel.buyerRating.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRatingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(storeId) {
        coSellerStoreViewModel.loadStoreDetails(storeId)
        if (currentUserId.isNotEmpty()) storeRatingViewModel.loadBuyerRating(storeId, currentUserId)
    }

    LaunchedEffect(uiState) {
        if (uiState is CoSellerStoreState.Error) {
            snackbarHostState.showSnackbar(message = (uiState as CoSellerStoreState.Error).message, duration = SnackbarDuration.Short)
            coSellerStoreViewModel.resetState()
        }
    }

    LaunchedEffect(ratingState) {
        when (ratingState) {
            is StoreRatingState.Success -> {
                snackbarHostState.showSnackbar(message = (ratingState as StoreRatingState.Success).message, duration = SnackbarDuration.Short)
                showRatingDialog = false
                coSellerStoreViewModel.loadStoreDetails(storeId)
                storeRatingViewModel.resetState()
            }
            is StoreRatingState.Error -> {
                snackbarHostState.showSnackbar(message = (ratingState as StoreRatingState.Error).message, duration = SnackbarDuration.Short)
                storeRatingViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundSecondary,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = currentStore?.storeName ?: "Store", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is CoSellerStoreState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            else -> {
                currentStore?.let { store ->
                    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        StoreBanner(store = store)

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BackgroundSecondary)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Store identity
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp, bottom = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Color.White).padding(3.dp)
                                ) {
                                    if (store.storeLogo.isNotEmpty()) {
                                        AsyncImage(model = CloudinaryManager.getOptimizedUrl(store.storeLogo, 150, 80), contentDescription = "Store Logo", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))), contentAlignment = Alignment.Center) {
                                            Text(text = store.storeName.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = store.storeName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                if (store.storeDescription.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = store.storeDescription, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                                }
                            }

                            Column(modifier = Modifier.padding(horizontal = 14.dp).padding(top = 2.dp, bottom = 12.dp)) {
                                StoreInfoBar(productCount = store.productCount, memberCount = store.memberCount, rating = store.averageRating, ratingCount = store.ratingCount)
                            }

                            // Rate store button
                            Box(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp)) {
                                Button(
                                    onClick = { if (currentUserId.isNotEmpty()) showRatingDialog = true },
                                    enabled = currentUserId.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (currentUserId.isNotEmpty())
                                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                                                else Brush.horizontalGradient(listOf(Color(0xFFCCCCCC), Color(0xFFCCCCCC))),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = if (buyerRating != null) "Update Your Rating" else "Rate This Store",
                                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            if (storeMembers.isNotEmpty()) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 16.dp)) {
                                    TeamMembersSection(members = storeMembers)
                                }
                            }

                            Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 20.dp)) {
                                Text(text = "Store Products", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(bottom = 10.dp))

                                if (storeProducts.isEmpty()) {
                                    // Primary tinted circle — consistent with all empty states
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .background(Primary.copy(alpha = 0.08f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Inventory2,
                                                contentDescription = null,
                                                tint = Primary.copy(alpha = 0.50f),
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text("No products available", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Products will appear here once added", fontSize = 12.sp, color = TextSecondary)
                                    }
                                } else {
                                    ProductGrid(products = storeProducts, onProductClick = onProductClick, onAddToCart = onAddToCart)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRatingDialog && currentStore != null) {
        val store = currentStore ?: return
        RateStoreDialog(
            store = store,
            currentRating = buyerRating?.rating ?: 0,
            currentReview = buyerRating?.review ?: "",
            onDismiss = { showRatingDialog = false },
            onSubmit = { rating, review ->
                storeRatingViewModel.submitRating(storeId = storeId, buyerId = currentUserId, rating = rating, review = review, buyerName = currentUserName)
            },
            isLoading = ratingState is StoreRatingState.Loading
        )
    }
}

// ── Store Info Bar ────────────────────────────────────────────────────────────

@Composable
fun StoreInfoBar(productCount: Int, memberCount: Int, rating: Double, ratingCount: Int = 0) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceAround) {
            InfoItem(value = productCount.toString(), label = "Products")
            Box(modifier = Modifier.width(0.5.dp).height(36.dp).background(BorderColor))
            InfoItem(value = memberCount.toString(), label = "Sellers")
            Box(modifier = Modifier.width(0.5.dp).height(36.dp).background(BorderColor))
            InfoItem(value = if (rating > 0) "${"%.1f".format(rating)}⭐" else "New", label = if (ratingCount > 0) "($ratingCount)" else "Rating")
        }
    }
}

@Composable
fun InfoItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}

// ── Team Members Section ──────────────────────────────────────────────────────

@Composable
fun TeamMembersSection(members: List<StoreMember>) {
    Column {
        Text(text = "Team Members", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(bottom = 10.dp))
        val chunkedMembers = members.chunked(3)
        chunkedMembers.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { member -> TeamMemberItem(member = member, modifier = Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun TeamMemberItem(member: StoreMember, modifier: Modifier = Modifier) {
    var currentMemberName by remember { mutableStateOf(member.userName) }

    LaunchedEffect(member.userId) {
        if (member.userId.isNotEmpty()) {
            try {
                val db = Firebase.firestore
                db.collection("users").document(member.userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists()) {
                            val name = snapshot.getString("name") ?: member.userName
                            currentMemberName = name
                            Log.d("TeamMemberItem", "✅ Updated member name: $name")
                        }
                    }
            } catch (e: Exception) {
                Log.e("TeamMemberItem", "❌ Error listening to member name: ${e.message}")
            }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = currentMemberName.take(1).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = currentMemberName, fontSize = 10.sp, color = TextPrimary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

// ── Product Grid ──────────────────────────────────────────────────────────────

@Composable
fun ProductGrid(products: List<Product>, onProductClick: (Product) -> Unit, onAddToCart: (Product) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        products.chunked(2).forEach { rowProducts ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowProducts.forEach { product ->
                    ProductGridItem(product = product, onClick = { onProductClick(product) }, onAddToCart = { onAddToCart(product) }, modifier = Modifier.weight(1f))
                }
                if (rowProducts.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ProductGridItem(product: Product, onClick: () -> Unit, onAddToCart: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(BackgroundSecondary), contentAlignment = Alignment.Center) {
                if (product.imageUrls.isNotEmpty()) {
                    AsyncImage(model = CloudinaryManager.getOptimizedUrl(product.imageUrls.first(), 300, 80), contentDescription = product.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(imageVector = Icons.Outlined.Inventory2, contentDescription = null, tint = TextLight, modifier = Modifier.size(36.dp))
                }
            }
            Column(modifier = Modifier.padding(9.dp)) {
                Text(text = product.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp, modifier = Modifier.height(30.dp).padding(bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(text = "PKR", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary.copy(alpha = 0.65f))
                    Text(text = String.format(java.util.Locale.US, "%,.0f", product.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Primary, letterSpacing = (-0.3).sp)
                }
                // Add to Cart — gradient fill replacing flat Primary button
                Button(
                    onClick = { onAddToCart() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(7.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(7.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Cart", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}