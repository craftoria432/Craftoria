package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.NegotiationState
import com.gcuf.craftoria.viewmodel.NegotiationViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun NegotiationDialog(
    product: Product,
    currentUserId: String,
    onDismiss: () -> Unit,
    negotiationViewModel: NegotiationViewModel
) {
    var offerAmount by remember { mutableStateOf("") }
    val negotiationState by negotiationViewModel.negotiationState.collectAsState()
    val scope = rememberCoroutineScope()

    var freshProduct by remember { mutableStateOf(product) }
    var isLoadingProduct by remember { mutableStateOf(true) }

    LaunchedEffect(product.id) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val doc = db.collection("products").document(product.id).get().await()
            if (doc.exists()) {
                val updatedProduct = doc.toObject(Product::class.java)?.copy(id = doc.id)
                if (updatedProduct != null) {
                    freshProduct = updatedProduct
                    android.util.Log.d("NegotiationDialog", "✅ Fresh product loaded")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NegotiationDialog", "❌ Failed to load fresh product", e)
        } finally {
            isLoadingProduct = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text(text = "Price Negotiation", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Make your best offer", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Box(
                            modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bot avatar row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).background(Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text(text = "Craftoria Assistant", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    // Opening message bubble
                    ChatBubble(
                        message = "Hi! I can help you negotiate the price. The current price is PKR ${freshProduct.price.toInt()}.",
                        type = ChatBubbleType.DEFAULT
                    )

                    // Input
                    Column {
                        Text(text = "Enter your offer price (PKR)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = offerAmount,
                            onValueChange = { offerAmount = it },
                            placeholder = { Text(text = "e.g., 1,500", fontSize = 14.sp, color = TextLight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Suggestion chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(label = "10% off", sublabel = "PKR ${(freshProduct.price * 0.90).toInt()}", onClick = { offerAmount = (freshProduct.price * 0.90).toInt().toString() }, modifier = Modifier.weight(1f))
                        SuggestionChip(label = "15% off", sublabel = "PKR ${(freshProduct.price * 0.85).toInt()}", onClick = { offerAmount = (freshProduct.price * 0.85).toInt().toString() }, modifier = Modifier.weight(1f))
                        SuggestionChip(label = "20% off", sublabel = "PKR ${(freshProduct.price * 0.80).toInt()}", onClick = { offerAmount = (freshProduct.price * 0.80).toInt().toString() }, modifier = Modifier.weight(1f))
                    }

                    // Submit button
                    Button(
                        onClick = {
                            val offer = offerAmount.toDoubleOrNull()
                            if (offer != null) {
                                scope.launch {
                                    try {
                                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        val doc = db.collection("products").document(freshProduct.id).get().await()
                                        if (doc.exists()) {
                                            val latestMinimumPrice = doc.getDouble("minimum_price") ?: freshProduct.minimumPrice
                                            val latestAutoAcceptPrice = doc.getDouble("auto_accept_price") ?: freshProduct.autoAcceptPrice
                                            negotiationViewModel.submitOffer(
                                                productId = freshProduct.id,
                                                buyerId = currentUserId,
                                                sellerId = freshProduct.sellerId,
                                                offerAmount = offer,
                                                originalPrice = freshProduct.price,
                                                minimumPrice = latestMinimumPrice,
                                                autoAcceptPrice = latestAutoAcceptPrice
                                            )
                                        }
                                    } catch (e: Exception) {
                                        negotiationViewModel.submitOffer(
                                            productId = freshProduct.id,
                                            buyerId = currentUserId,
                                            sellerId = freshProduct.sellerId,
                                            offerAmount = offer,
                                            originalPrice = freshProduct.price,
                                            minimumPrice = freshProduct.minimumPrice,
                                            autoAcceptPrice = freshProduct.autoAcceptPrice
                                        )
                                    }
                                }
                            }
                        },
                        enabled = offerAmount.isNotBlank() && !isLoadingProduct && negotiationState !is NegotiationState.Loading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                RoundedCornerShape(12.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isLoadingProduct || negotiationState is NegotiationState.Loading -> {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                }
                                else -> {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Text(text = "Send Offer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Response
                    when (val state = negotiationState) {
                        is NegotiationState.AutoAccepted -> {
                            ChatBubble(
                                message = "Great! Your offer of PKR ${state.offerAmount.toInt()} is accepted!\n\nThe price has been updated. You can now add this product to cart at the negotiated price.",
                                type = ChatBubbleType.SUCCESS,
                                icon = Icons.Default.CheckCircle
                            )
                        }
                        is NegotiationState.Pending -> {
                            ChatBubble(
                                message = "Your offer of PKR ${state.offerAmount.toInt()} has been sent to the seller for review.\n\nYou'll be notified within 24 hours.",
                                type = ChatBubbleType.INFO,
                                icon = Icons.Default.Send
                            )
                        }
                        is NegotiationState.BelowMinimum -> {
                            ChatBubble(
                                message = "Sorry, the seller's minimum acceptable price is PKR ${state.minimumPrice.toInt()}.\n\nPlease enter an offer above this amount.",
                                type = ChatBubbleType.ERROR,
                                icon = Icons.Default.Error
                            )
                        }
                        is NegotiationState.Error -> {
                            ChatBubble(message = state.message, type = ChatBubbleType.ERROR, icon = Icons.Default.Info)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

// ── Chat Bubble ───────────────────────────────────────────────────────────────

enum class ChatBubbleType { DEFAULT, SUCCESS, ERROR, INFO }

@Composable
fun ChatBubble(
    message: String,
    type: ChatBubbleType,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val (bgColor, textColor) = when (type) {
        ChatBubbleType.DEFAULT -> BackgroundSecondary to TextPrimary
        ChatBubbleType.SUCCESS -> Color(0xFFE8F5E8) to Color(0xFF2E7D2E)
        ChatBubbleType.ERROR -> Color(0xFFF8D7DA) to Color(0xFF721C24)
        ChatBubbleType.INFO -> Color(0xFFE3F2FD) to Color(0xFF1976D2)
    }

    Surface(shape = RoundedCornerShape(10.dp), color = bgColor, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp).padding(top = 1.dp))
            }
            Text(text = message, fontSize = 13.sp, color = textColor, lineHeight = 19.sp, modifier = Modifier.weight(1f))
        }
    }
}

// ── Suggestion Chip ───────────────────────────────────────────────────────────

@Composable
fun SuggestionChip(
    label: String,
    sublabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = sublabel, fontSize = 10.sp, color = Primary)
        }
    }
}