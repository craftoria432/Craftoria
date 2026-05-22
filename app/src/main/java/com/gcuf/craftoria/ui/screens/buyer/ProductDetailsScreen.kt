package com.gcuf.craftoria.ui.screens.buyer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.NegotiationStatus
import com.gcuf.craftoria.ui.components.NegotiationDialog
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.NegotiationViewModel
import com.gcuf.craftoria.viewmodel.NegotiationState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    product: Product,
    currentUserId: String,
    onBackClick: () -> Unit,
    onAddToCart: (Product, Double, Boolean, NegotiationStatus?) -> Unit,
    onNavigateToCart: () -> Unit = {},
    onChatWithSeller: ((String, String) -> Unit)? = null,
    onNavigateToStore: ((String) -> Unit)? = null,
    isSellerPreview: Boolean = false,
    cartViewModel: com.gcuf.craftoria.viewmodel.CartViewModel? = null,
    negotiationViewModel: NegotiationViewModel = viewModel(),
    wishlistViewModel: com.gcuf.craftoria.viewmodel.WishlistViewModel? = null
) {
    val context = LocalContext.current
    var showNegotiationDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var currentSelectedPrice by remember { mutableDoubleStateOf(product.price) }
    var isProductNegotiated by remember { mutableStateOf(product.isNegotiated) }
    var currentNegotiationStatus by remember { mutableStateOf<NegotiationStatus?>(null) }

    val negotiationState by negotiationViewModel.negotiationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Wishlist state from ViewModel
    val wishlistIds by (wishlistViewModel?.wishlistIds ?: kotlinx.coroutines.flow.MutableStateFlow(emptySet())).collectAsState()
    val isWishlisted = wishlistIds.contains(product.id)

    val cartItems by (cartViewModel?.cartItems
        ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val productInCart = cartItems.find { it.product.id == product.id }
    val isInCart = productInCart != null

    LaunchedEffect(negotiationState) {
        when (val state = negotiationState) {
            is NegotiationState.AutoAccepted -> {
                currentSelectedPrice = state.offerAmount
                isProductNegotiated = true
                currentNegotiationStatus = NegotiationStatus.AUTO_ACCEPTED
                if (isInCart && cartViewModel != null) {
                    cartViewModel.updateCartItemPrice(
                        productId = product.id,
                        newPrice = state.offerAmount,
                        isNegotiated = true,
                        negotiationStatus = NegotiationStatus.AUTO_ACCEPTED
                    )
                }
                kotlinx.coroutines.delay(3000)
                showNegotiationDialog = false
                negotiationViewModel.resetState()
            }
            is NegotiationState.Pending -> {
                currentSelectedPrice = product.price
                isProductNegotiated = false
                currentNegotiationStatus = NegotiationStatus.PENDING
                if (isInCart && cartViewModel != null) {
                    cartViewModel.updateCartItemPrice(
                        productId = product.id,
                        newPrice = product.price,
                        isNegotiated = false,
                        negotiationStatus = NegotiationStatus.PENDING
                    )
                }
                kotlinx.coroutines.delay(3000)
                showNegotiationDialog = false
                negotiationViewModel.resetState()
            }
            else -> {}
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
                            text = "Product Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = product.category,
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
                    // Wishlist toggle in tinted circle - hidden in seller preview mode
                    if (!isSellerPreview) {
                        IconButton(
                            onClick = { 
                                wishlistViewModel?.toggleWishlist(product)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                                    contentDescription = "Add to Wishlist",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 88.dp)
            ) {
                // ── Image Gallery ─────────────────────────────────────────────
                ImageGallery(images = product.imageUrls)

                // ── Title + Price + Badges + Seller (white card) ──────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title + Price block
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = product.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                lineHeight = 24.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PKR",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Primary.copy(alpha = 0.65f)
                                )
                                Text(
                                    text = currentSelectedPrice.toInt().toString(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    letterSpacing = (-0.5).sp
                                )
                                if (isProductNegotiated) {
                                    Text(
                                        text = "PKR ${product.price.toInt()}",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                }
                            }
                        }

                        // Badges row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (currentNegotiationStatus) {
                                NegotiationStatus.PENDING ->
                                    BadgeChip(
                                        "Negotiation Pending",
                                        Color(0xFFFFA500).copy(alpha = 0.15f),
                                        Color(0xFFFFA500)
                                    )
                                NegotiationStatus.AUTO_ACCEPTED ->
                                    BadgeChip(
                                        "Negotiated",
                                        Success.copy(alpha = 0.12f),
                                        Success,
                                        Icons.Default.CheckCircle
                                    )
                                NegotiationStatus.REJECTED ->
                                    BadgeChip("Rejected", Error.copy(alpha = 0.12f), Error)
                                else -> {
                                    if (isProductNegotiated)
                                        BadgeChip(
                                            "Negotiated",
                                            Success.copy(alpha = 0.12f),
                                            Success,
                                            Icons.Default.CheckCircle
                                        )
                                    else if (product.isNegotiable)
                                        BadgeChip(
                                            "Negotiable",
                                            Primary.copy(alpha = 0.08f),
                                            Primary,
                                            Icons.Default.CheckCircle
                                        )
                                }
                            }
                            if (product.stock > 0)
                                BadgeChip("In Stock", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            BadgeChip(product.category, BackgroundSecondary, TextSecondary)
                        }

                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                        // Seller card (inside the white surface)
                        SellerCard(
                            sellerId = product.sellerId,
                            sellerName = product.sellerName,
                            isVerified = product.sellerVerified,
                            memberSince = product.sellerMemberSince,
                            currentUserId = currentUserId,
                            storeId = product.coSellerStoreId,
                            isSellerPreview = isSellerPreview,
                            onViewStoreClick = {
                                if (product.coSellerStoreId.isNotEmpty())
                                    onNavigateToStore?.invoke(product.coSellerStoreId)
                            },
                            onChatClick = {
                                if (currentUserId != product.sellerId)
                                    onChatWithSeller?.invoke(product.sellerId, product.sellerName)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Description Card ──────────────────────────────────────────
                ProductInfoCard(
                    title = "Product Description",
                    icon = Icons.Default.Description
                ) {
                    Text(
                        text = product.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.2.sp
                    )
                }

                // ── Specifications Card ───────────────────────────────────────
                if (product.specifications.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ProductInfoCard(
                        title = "Specifications",
                        icon = Icons.Default.Checklist
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            product.specifications.entries.forEachIndexed { index, (k, v) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index % 2 == 0) Color.White
                                            else BackgroundSecondary
                                        )
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = k,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = v,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (index < product.specifications.size - 1) {
                                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Fixed Bottom Action Bar ───────────────────────────────────────
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                if (isSellerPreview) {
                    // Seller preview — disabled buttons with warning banner
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Warning banner — tinted BackgroundSecondary yellow
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF8E1)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF856404),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Seller Preview — Buttons are disabled",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF856404)
                                )
                            }
                        }
                        // Disabled action row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Disabled Add to Cart
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                border = BorderStroke(0.5.dp, BorderColor)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Add to Cart",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            // Disabled Negotiate
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Primary.copy(alpha = 0.35f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Negotiate",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Live buyer action bar
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isInCart) {
                                // View Cart — gradient button
                                Button(
                                    onClick = { onNavigateToCart() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "View Cart (${productInCart?.quantity ?: 0})",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                // Negotiate — 0.5.dp Primary border
                                if (product.isNegotiable) {
                                    OutlinedButton(
                                        onClick = { showNegotiationDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                                        border = BorderStroke(0.5.dp, Primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Negotiate",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            } else {
                                // Add to Cart — 0.5.dp Primary outlined
                                OutlinedButton(
                                    onClick = {
                                        onAddToCart(
                                            product,
                                            currentSelectedPrice,
                                            isProductNegotiated,
                                            currentNegotiationStatus
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                                    border = BorderStroke(0.5.dp, Primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add to Cart",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                // Negotiate — gradient Box fill
                                if (product.isNegotiable) {
                                    Button(
                                        onClick = { showNegotiationDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Negotiate",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Secure checkout note
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        }
                    }
                }
            }
        }
    }

    if (showNegotiationDialog) {
        NegotiationDialog(
            product = product,
            currentUserId = currentUserId,
            onDismiss = {
                showNegotiationDialog = false
                negotiationViewModel.resetState()
            },
            negotiationViewModel = negotiationViewModel
        )
    }

    if (showReportDialog) {
        ReportProductDialog(
            productId = product.id,
            productName = product.title,
            currentUserId = currentUserId,
            onDismiss = { showReportDialog = false },
            onReportSubmitted = {
                showReportDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Report submitted successfully",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
}

// ── Seller Card ───────────────────────────────────────────────────────────────

@Composable
fun SellerCard(
    sellerId: String,
    sellerName: String,
    isVerified: Boolean,
    memberSince: String,
    currentUserId: String,
    storeId: String = "",
    onViewStoreClick: () -> Unit,
    onChatClick: () -> Unit,
    isSellerPreview: Boolean = false
) {
    val isOwnProduct = currentUserId == sellerId
    var currentSellerProfileImage by remember { mutableStateOf("") }

    // Real-time seller profile picture + name listener
    DisposableEffect(sellerId) {
        if (sellerId.isEmpty()) return@DisposableEffect onDispose {}
        var registration: ListenerRegistration? = null
        try {
            registration = Firebase.firestore
                .collection("users")
                .document(sellerId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        currentSellerProfileImage = snapshot.getString("profile_image") ?: ""
                    }
                }
        } catch (e: Exception) {
            Log.e("SellerCard", "Error listening to seller data: ${e.message}")
        }
        onDispose { registration?.remove() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = BackgroundSecondary
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile picture or gradient initials avatar
            if (currentSellerProfileImage.isNotEmpty()) {
                AsyncImage(
                    model = currentSellerProfileImage,
                    contentDescription = "Seller Profile Picture",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFCE4EC)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sellerName.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RealtimeNameDisplay(
                        userId = sellerId,
                        fallbackName = sellerName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    if (isVerified) {
                        Surface(
                            color = Primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "✓ verified",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Member since $memberSince",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                // View Store button — 0.5.dp Primary border
                if (storeId.isNotEmpty() && !isSellerPreview) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onViewStoreClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border = BorderStroke(0.5.dp, Primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("View Store", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Chat button — 0.5.dp Primary border
            if (!isOwnProduct && !isSellerPreview) {
                OutlinedButton(
                    onClick = onChatClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = BorderStroke(0.5.dp, Primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Image Gallery ─────────────────────────────────────────────────────────────

@Composable
fun ImageGallery(images: List<String>) {
    var selected by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .height(280.dp)
            .fillMaxWidth()
            .background(BackgroundSecondary)
    ) {
        if (images.isNotEmpty()) {
            AsyncImage(
                model = CloudinaryManager.getOptimizedUrl(images[selected], 800, 85),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        // Thumbnail strip — shown only when multiple images
        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                images.take(5).forEachIndexed { index, url ->
                    Surface(
                        onClick = { selected = index },
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.90f),
                        border = BorderStroke(
                            width = if (selected == index) 2.dp else 0.5.dp,
                            color = if (selected == index) Primary else Color.White
                        )
                    ) {
                        AsyncImage(
                            model = CloudinaryManager.getOptimizedUrl(url, 120, 70),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

// ── Product Info Card ─────────────────────────────────────────────────────────

@Composable
fun ProductInfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tinted gradient section header — matches AddProductSectionCard pattern
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Primary.copy(alpha = 0.06f),
                                Primary.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                content = content
            )
        }
    }
}

// ── Badge Chip ────────────────────────────────────────────────────────────────

@Composable
fun BadgeChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(shape = RoundedCornerShape(8.dp), color = backgroundColor) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Section (backward compat) ─────────────────────────────────────────────────

@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

// ── Specification Item (backward compat) ──────────────────────────────────────

@Composable
fun SpecificationItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

// ── Report Product Dialog ─────────────────────────────────────────────────────

@Composable
fun ReportProductDialog(
    productId: String,
    productName: String,
    currentUserId: String,
    onDismiss: () -> Unit,
    onReportSubmitted: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val reportReasons = listOf(
        "Counterfeit or fake product",
        "Misleading description",
        "Inappropriate content",
        "Prohibited item",
        "Price manipulation",
        "Other"
    )

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            // Flag icon in tinted circle — consistent with all alert dialogs
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Error.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Report Product",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Help us understand what's wrong with this product",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                // Product name pill — BackgroundSecondary surface
                Surface(
                    color = BackgroundSecondary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = productName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Text(
                    "Select a reason *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(reason, fontSize = 13.sp, color = TextPrimary)
                    }
                }
                Text(
                    "Additional Details (Optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    placeholder = {
                        Text(
                            "Provide more details about the issue...",
                            fontSize = 13.sp,
                            color = TextLight
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedReason.isNotEmpty()) {
                        isSubmitting = true
                        scope.launch {
                            try {
                                val reportRepository =
                                    com.gcuf.craftoria.data.repository.ReportRepository()
                                val userDoc =
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(currentUserId)
                                        .get()
                                        .await()
                                val userName = userDoc.getString("name") ?: "Unknown User"
                                val result = reportRepository.submitReport(
                                    reportType = com.gcuf.craftoria.data.model.ReportType.PRODUCT,
                                    reporterId = currentUserId,
                                    reporterName = userName,
                                    reporterRole = "buyer",
                                    reportedEntityId = productId,
                                    reportedEntityName = productName,
                                    reason = selectedReason,
                                    description = description
                                )
                                if (result.isSuccess) {
                                    onReportSubmitted()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to submit report",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    }
                },
                enabled = selectedReason.isNotEmpty() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit Report", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        }
    )
}