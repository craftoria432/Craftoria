package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager

@Composable
fun ProductCard(
    product: Product,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }

    Card(
        onClick = { onProductClick(product) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, BorderColor),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Product Image with Wishlist Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(BackgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                // Load Cloudinary image
                if (product.images.isNotEmpty()) {
                    AsyncImage(
                        model = CloudinaryManager.getOptimizedUrl(
                            url = product.images.first(),
                            width = 400,
                            quality = 80
                        ),
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback emoji
                    Text(
                        text = getProductEmoji(product.category),
                        fontSize = 48.sp
                    )
                }

                // Wishlist Icon
                Surface(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Error else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Product Info
            Column(modifier = Modifier.padding(10.dp)) {
                // Title
                Text(
                    text = product.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Seller Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "By ${product.sellerName}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    // Note: Add verified field to Product model if needed
                }

                // Price
                Text(
                    text = "PKR ${formatPrice(product.price)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Stock Badge
                if (product.stock > 0) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(0xFFE8F5E8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "${product.stock} in stock",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D2E),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Add to Cart Button
                Button(
                    onClick = { onAddToCart(product) },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Add to Cart",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

fun getProductEmoji(category: String): String {
    return when (category) {
        "Textiles" -> "🧶"
        "Jewelry" -> "💍"
        "Home Décor" -> "🏺"
        "Embroidery" -> "🎨"
        "Pottery" -> "🏺"
        else -> "🧵"
    }
}

fun formatPrice(price: Double): String {
    return String.format("%,.0f", price)
}