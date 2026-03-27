package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.data.model.SellerApplicationStatus
import com.gcuf.craftoria.ui.components.ProductCard
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.compose.ui.unit.times

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerPublicProfileScreen(
    sellerId: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onChatWithSeller: (String, String) -> Unit,
    onAddToCart: (Product) -> Unit,
    onAddToWishlist: (Product) -> Unit,
    onNavigateToCart: () -> Unit
) {
    var seller by remember { mutableStateOf<User?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sellerId) {
        try {
            Log.d("SellerPublicProfile", "🔍 Loading seller: $sellerId")
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users").document(sellerId).get().await()
            
            // ✅ Manual safe mapping with proper UserRole deserialization
            val data = userDoc.data
            if (data != null) {
                seller = User(
                    id = userDoc.id,
                    email = data["email"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    role = UserRole.fromString(data["role"] as? String), // ✅ Proper deserialization
                    phone = data["phone"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    profileImage = data["profile_image"] as? String ?: "",
                    createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time 
                        ?: (data["created_at"] as? Long) ?: System.currentTimeMillis(),
                    storeName = data["store_name"] as? String ?: "",
                    storeDescription = data["store_description"] as? String ?: "",
                    verified = data["verified"] as? Boolean ?: false,
                    verificationStatus = VerificationStatus.fromString(data["verification_status"] as? String),
                    verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                    rejectionReason = data["rejection_reason"] as? String ?: "",
                    mainSellerId = data["main_seller_id"] as? String ?: "",
                    sellerApplicationStatus = SellerApplicationStatus.fromString(data["seller_application_status"] as? String)
                )
                Log.d("SellerPublicProfile", "✅ Seller loaded: ${seller?.name} with role: ${seller?.role}")
            } else {
                Log.e("SellerPublicProfile", "❌ No data found for seller: $sellerId")
                errorMessage = "Seller not found"
            }

            val productsSnapshot = FirebaseFirestore.getInstance()
                .collection("products")
                .whereEqualTo("seller_id", sellerId)
                .whereEqualTo("is_active", true)
                .get().await()
            products = productsSnapshot.documents.mapNotNull { doc ->
                try { doc.toObject(Product::class.java)?.copy(id = doc.id) }
                catch (e: Exception) { Log.e("SellerPublicProfile", "Error parsing product: ${doc.id}", e); null }
            }
            Log.d("SellerPublicProfile", "✅ Loaded ${products.size} products")
        } catch (e: Exception) {
            Log.e("SellerPublicProfile", "❌ Failed to load seller profile", e)
            errorMessage = e.message
        } finally {
            isLoading = false
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
                            text = "Seller Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        seller?.let {
                            RealtimeNameDisplay(
                                userId = it.id,
                                fallbackName = it.name,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Error.copy(alpha = 0.10f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Failed to load profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = errorMessage ?: "Unknown error",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .widthIn(min = 140.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text("Go Back", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            seller != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundSecondary)
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Seller Hero Header ────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                            )
                            .padding(top = 24.dp, bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Avatar
                            Surface(
                                modifier = Modifier.size(84.dp),
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 6.dp
                            ) {
                                if (seller!!.profileImage.isNotEmpty()) {
                                    AsyncImage(
                                        model = seller!!.profileImage,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = seller!!.name.firstOrNull()?.uppercase() ?: "S",
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            RealtimeNameDisplay(
                                userId = seller!!.id,
                                fallbackName = seller!!.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Verification & Role Badges
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                if (seller!!.role == UserRole.SELLER) {
                                    SellerBadgeChip(
                                        text = "Seller",
                                        backgroundColor = Color.White.copy(alpha = 0.25f)
                                    )
                                    when (seller!!.verificationStatus) {
                                        VerificationStatus.APPROVED -> SellerBadgeChip(
                                            text = "✓ Verified",
                                            backgroundColor = Color(0xFFE8F5E8),
                                            textColor = Color(0xFF2E7D2E)
                                        )
                                        VerificationStatus.PENDING -> SellerBadgeChip(
                                            text = "Pending",
                                            backgroundColor = Color(0xFFFFF3CD),
                                            textColor = Color(0xFF856404)
                                        )
                                        VerificationStatus.REJECTED -> SellerBadgeChip(
                                            text = "Rejected",
                                            backgroundColor = Color(0xFFF8D7DA),
                                            textColor = Color(0xFF721C24)
                                        )
                                        VerificationStatus.NOT_SUBMITTED -> SellerBadgeChip(
                                            text = "Not Verified",
                                            backgroundColor = Color(0xFFF8D7DA),
                                            textColor = Color(0xFF721C24)
                                        )
                                    }
                                }
                            }

                            // Chat CTA button (only for other users)
                            if (currentUserId != sellerId) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onChatWithSeller(seller!!.id, seller!!.name) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp)
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chat with Seller",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // ── Products Section ──────────────────────────────────────
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Products",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                color = Primary.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${products.size} listed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (products.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(0.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Primary.copy(alpha = 0.08f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory,
                                            contentDescription = null,
                                            tint = Primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Text(
                                        text = "No products available",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.height((products.size / 2 + 1) * 280.dp)
                            ) {
                                items(products) { product ->
                                    ProductCard(
                                        product = product,
                                        onProductClick = { onProductClick(product.id) },
                                        onAddToCart = {
                                            if (currentUserId != sellerId) onAddToCart(product)
                                        },
                                        onAddToWishlist = {
                                            if (currentUserId != sellerId) onAddToWishlist(product)
                                        },
                                        showActions = currentUserId != sellerId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerBadgeChip(
    text: String,
    backgroundColor: Color,
    textColor: Color = Color.White
) {
    Surface(shape = RoundedCornerShape(20.dp), color = backgroundColor) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}