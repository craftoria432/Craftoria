package com.gcuf.craftoria.ui.screens.buyer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.CartItem
import com.gcuf.craftoria.data.model.NegotiationStatus
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.ui.theme.BorderStyles
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onCheckout: () -> Unit,
    onContinueShopping: () -> Unit,
    onProductClick: (String) -> Unit = {},
    cartViewModel: CartViewModel
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val subtotal = remember(cartItems) { cartViewModel.getSubtotal() }
    val shipping = CartViewModel.SHIPPING_COST
    val total = remember(cartItems) { cartViewModel.getTotal() }
    var showClearDialog by remember { mutableStateOf(false) }
    
    // ✅ Track if we've ever loaded cart data to prevent empty state flash
    var hasLoadedOnce by remember { mutableStateOf(false) }
    
    LaunchedEffect(cartItems) {
        if (cartItems.isNotEmpty()) {
            hasLoadedOnce = true
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
                        Text(text = "My Cart", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 16.sp)
                        if (cartItems.isNotEmpty()) {
                            Text(text = "${cartItems.size} items · ${cartItems.map { it.product.sellerId }.distinct().size} sellers", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        // Clear all — consistent pill button matching other top bar actions
                        Surface(
                            onClick = { showClearDialog = true },
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(text = "Clear all", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        // ✅ Only show empty state if we've loaded and cart is truly empty
        // This prevents the flash of empty state when navigating to cart after adding items
        if (cartItems.isEmpty() && hasLoadedOnce) {
            EmptyCartState(modifier = Modifier.padding(paddingValues), onContinueShopping = onContinueShopping)
        } else if (cartItems.isEmpty()) {
            // Show loading state instead of empty state on first load
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundSecondary)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(BackgroundSecondary).padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(bottom = 88.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val itemsBySeller = cartItems.groupBy { it.product.sellerId }
                    val sellerEntries = itemsBySeller.entries.toList()
                    sellerEntries.forEachIndexed { sellerIndex, (sellerId, sellerItems) ->
                        item { SellerGroupHeader(sellerId = sellerId, sellerName = sellerItems.first().product.sellerName, itemCount = sellerItems.size) }
                        items(sellerItems) { item ->
                            CartItemCard(item = item, onQuantityChange = { newQty -> cartViewModel.updateQuantity(item.id, newQty) }, onRemove = { cartViewModel.removeFromCart(item.id) }, onClick = { onProductClick(item.product.id) })
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        if (sellerIndex < sellerEntries.size - 1) { item { SellerGroupDivider() } }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        PriceSummarySection(cartItems = cartItems, subtotal = subtotal, shipping = shipping, total = total)
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    CartCheckoutButton(total = total, itemCount = cartItems.size, onCheckout = onCheckout)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text(text = "Clear Cart?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary) },
            text = { Text("Are you sure you want to remove all items from your cart?", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp) },
            confirmButton = {
                Button(onClick = { cartViewModel.clearCart(); showClearDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Error), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Clear All", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
fun SellerGroupHeader(sellerId: String, sellerName: String, itemCount: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(26.dp).background(Primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Text(text = sellerName.take(2).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
        RealtimeNameDisplay(
            userId = sellerId,
            fallbackName = sellerName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(color = Primary.copy(alpha = 0.10f), shape = RoundedCornerShape(10.dp)) {
            Text(text = "$itemCount ${if (itemCount == 1) "item" else "items"}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}

@Composable
fun SellerGroupDivider() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor, thickness = 0.5.dp)
        Text(text = "Another seller", fontSize = 11.sp, color = TextLight)
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor, thickness = 0.5.dp)
    }
}

@Composable
fun CartItemCard(item: CartItem, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit, onClick: () -> Unit = {}) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    val itemSubtotal = item.price * item.quantity
    
    // ✅ Log negotiation status for debugging
    LaunchedEffect(item.negotiationStatus) {
        Log.d("CartItemCard", "🏷️ ${item.product.title}: negotiationStatus=${item.negotiationStatus}, price=${item.price}")
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(model = CloudinaryManager.getOptimizedUrl(item.product.imageUrls.firstOrNull() ?: "", 100, 100), contentDescription = item.product.title, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(modifier = Modifier.weight(1f).align(Alignment.Top)) {
                    Text(text = item.product.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "By ", fontSize = 11.sp, color = TextSecondary)
                        RealtimeNameDisplay(
                            userId = item.product.sellerId,
                            fallbackName = item.product.sellerName,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(text = "✓", fontSize = 10.sp, color = Success)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(text = "PKR ${String.format("%.0f", item.price)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
                        // ✅ Enhanced badge display with better visibility
                        when (item.negotiationStatus) {
                            NegotiationStatus.PENDING -> {
                                Surface(
                                    color = Warning.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Warning.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Pending",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Warning,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            NegotiationStatus.ACCEPTED, NegotiationStatus.AUTO_ACCEPTED -> {
                                Surface(
                                    color = Success.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Success.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Accepted",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Success,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            NegotiationStatus.REJECTED, NegotiationStatus.DECLINED -> {
                                Surface(
                                    color = Error.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Error.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Rejected",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Error,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            null -> {
                                // No badge for non-negotiated items
                            }
                        }
                    }
                }
                IconButton(onClick = { showRemoveDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Error, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.border(0.5.dp, BorderColor, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(30.dp, 28.dp).background(BackgroundSecondary).clickable { if (item.quantity > 1) onQuantityChange(item.quantity - 1) }, contentAlignment = Alignment.Center) {
                        Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(text = item.quantity.toString(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(horizontal = 14.dp))
                    Box(modifier = Modifier.size(30.dp, 28.dp).background(Primary).clickable { onQuantityChange(item.quantity + 1) }, contentAlignment = Alignment.Center) {
                        Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Text(text = "Subtotal: PKR ${String.format("%.0f", itemSubtotal)}", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Remove Item?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary) },
            text = { Text("Remove this item from your cart?", fontSize = 13.sp, color = TextSecondary) },
            confirmButton = {
                Button(onClick = { onRemove(); showRemoveDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Error), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDialog = false }, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(40.dp)) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
fun PriceSummarySection(cartItems: List<CartItem>, subtotal: Double, shipping: Double, total: Double) {
    val uniqueSellers = cartItems.map { it.product.sellerId }.distinct().size
    val totalShipping = CartViewModel.SHIPPING_COST * uniqueSellers
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Order summary", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.padding(bottom = 12.dp))
            PriceSummaryRow(label = "Subtotal (${cartItems.size} items)", value = "PKR ${String.format("%.0f", subtotal)}")
            Spacer(modifier = Modifier.height(8.dp))
            if (uniqueSellers > 1) {
                repeat(uniqueSellers) { index ->
                    PriceSummaryRow(label = "Shipping — Seller ${index + 1}", value = "PKR ${CartViewModel.SHIPPING_COST.toInt()}")
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                PriceSummaryRow(label = "Shipping", value = "PKR ${String.format("%.0f", shipping)}")
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Primary.copy(alpha = 0.12f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            PriceSummaryRow(label = "Total", value = "PKR ${String.format("%.0f", subtotal + totalShipping)}", labelFontSize = 15.sp, valueFontSize = 15.sp, fontWeight = FontWeight.Bold, valueColor = Primary)
        }
    }
}

@Composable
fun PriceSummaryRow(label: String, value: String, labelFontSize: androidx.compose.ui.unit.TextUnit = 13.sp, valueFontSize: androidx.compose.ui.unit.TextUnit = 13.sp, fontWeight: FontWeight = FontWeight.Normal, valueColor: Color = TextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = labelFontSize, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = valueFontSize, fontWeight = fontWeight, color = valueColor)
    }
}

@Composable
fun CartCheckoutButton(total: Double, itemCount: Int, onCheckout: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth().height(52.dp).background(brush = Brush.horizontalGradient(listOf(Primary, PrimaryLight)), shape = RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Proceed to Checkout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = 0.3.sp)
                    Text(text = "→", fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun EmptyCartState(modifier: Modifier = Modifier, onContinueShopping: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().background(BackgroundSecondary).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(100.dp).background(color = Primary.copy(alpha = 0.10f), shape = CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Outlined.ShoppingCart, contentDescription = null, tint = Primary.copy(alpha = 0.7f), modifier = Modifier.size(46.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Your Cart is Empty", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Discover beautiful handcrafted items\nand add them to your cart", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinueShopping,
            modifier = Modifier.fillMaxWidth().height(50.dp).background(brush = Brush.horizontalGradient(listOf(Primary, PrimaryLight)), shape = RoundedCornerShape(14.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Continue Shopping", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}