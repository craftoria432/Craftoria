package com.gcuf.craftoria.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.WishlistViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun ProductCard(
    product: Product,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
    wishlistViewModel: WishlistViewModel? = null,
    onAddToWishlist: ((Product) -> Unit)? = null,
    showActions: Boolean = true
) {
    val wishlistIds by (wishlistViewModel?.wishlistIds
        ?: kotlinx.coroutines.flow.MutableStateFlow(emptySet())).collectAsState()

    var localFavorite by remember { mutableStateOf(false) }
    val displayFavorite = if (wishlistViewModel != null) wishlistIds.contains(product.id) else localFavorite

    // ✅ Real-time seller name listener (properly cleaned up)
    var currentSellerName by remember(product.sellerId) { mutableStateOf(product.sellerName) }

    DisposableEffect(product.sellerId) {
        if (product.sellerId.isEmpty()) return@DisposableEffect onDispose {}
        var registration: ListenerRegistration? = null
        try {
            registration = Firebase.firestore
                .collection("users")
                .document(product.sellerId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        val name = snapshot.getString("name") ?: product.sellerName
                        currentSellerName = name
                    }
                }
        } catch (e: Exception) {
            Log.e("ProductCard", "Error listening to seller name: ${e.message}")
        }
        onDispose { registration?.remove() }
    }

    Card(
        onClick = { onProductClick(product) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Image Section ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(BackgroundSecondary)
            ) {
                if (product.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrls.first(),
                        contentDescription = product.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop,
                        onError = {
                            Log.e("ProductCard", "Image failed: ${product.imageUrls.first()}")
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = TextLight,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Out-of-stock overlay
                if (product.stock == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "Out of Stock",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Wishlist button
                if (showActions) {
                    Surface(
                        onClick = {
                            when {
                                wishlistViewModel != null -> wishlistViewModel.toggleWishlist(product)
                                onAddToWishlist != null -> onAddToWishlist(product)
                                else -> localFavorite = !localFavorite
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(30.dp),
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (displayFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (displayFavorite) Primary else TextLight,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // ── Content Section ───────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Title
                Text(
                    text = product.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp)
                )

                // Seller info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "by ${currentSellerName.ifBlank { "Unknown" }}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (product.sellerVerified) {
                        Surface(
                            color = Primary.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "✓ verified",
                                fontSize = 8.sp,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Price
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "PKR", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary.copy(alpha = 0.65f))
                    Text(text = formatPrice(product.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Primary, letterSpacing = (-0.3).sp)
                }

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (product.stock > 0) {
                        ProductBadge(label = "${product.stock} in stock", bgColor = Color(0xFFE8F5E9), textColor = Color(0xFF2E7D32))
                    } else {
                        ProductBadge(label = "Out of stock", bgColor = Color(0xFFFFEBEE), textColor = Color(0xFFC62828))
                    }
                    if (product.isNegotiable) {
                        ProductBadge(label = "Negotiable", bgColor = Color(0xFFE3F2FD), textColor = Color(0xFF1565C0))
                    }
                }

                // Add to Cart
                if (showActions) {
                    Button(
                        onClick = { onAddToCart(product) },
                        enabled = product.stock > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White,
                            disabledContainerColor = BackgroundSecondary,
                            disabledContentColor = TextLight
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (product.stock > 0) "Add to Cart" else "Out of Stock",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductBadge(label: String, bgColor: Color, textColor: Color) {
    Surface(color = bgColor, shape = RoundedCornerShape(20.dp)) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

fun getProductEmoji(category: String): String = when (category) {
    "Textiles" -> "🧶"
    "Jewelry" -> "💍"
    "Home Décor" -> "🏺"
    "Embroidery" -> "🎨"
    "Pottery" -> "🏺"
    else -> "🧵"
}

fun formatPrice(price: Double): String =
    String.format(java.util.Locale.US, "%,.0f", price)