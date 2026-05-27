package com.gcuf.craftoria.ui.screens.seller

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.NegotiationOffer
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.ui.components.RealtimeNameDisplay
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegotiationRequestsScreen(
    user: User,
    onBackClick: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    var negotiations by remember { mutableStateOf<List<NegotiationWithDetails>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.id) {
        isLoading = true
        try {
            val snapshot = firestore.collection("negotiations")
                .whereEqualTo("seller_id", user.id)
                .whereEqualTo("status", com.gcuf.craftoria.data.model.NegotiationStatus.PENDING.toString())
                .get().await()
            val negotiationsList = mutableListOf<NegotiationWithDetails>()
            for (doc in snapshot.documents) {
                try {
                    val negotiation = doc.toObject(NegotiationOffer::class.java)?.copy(id = doc.id)
                    if (negotiation != null) {
                        val productDoc = firestore.collection("products").document(negotiation.productId).get().await()
                        val product = productDoc.toObject(Product::class.java)?.copy(id = productDoc.id)
                        val buyerDoc = firestore.collection("users").document(negotiation.buyerId).get().await()
                        val buyerName = buyerDoc.getString("name") ?: "Unknown Buyer"
                        if (product != null) negotiationsList.add(NegotiationWithDetails(negotiation = negotiation, product = product, buyerName = buyerName))
                    }
                } catch (e: Exception) { /* skip */ }
            }
            negotiations = negotiationsList.sortedByDescending { it.negotiation.createdAt }
        } catch (e: Exception) { /* handle */ }
        isLoading = false
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
                            text = "Negotiation Requests",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        if (!isLoading && negotiations.isNotEmpty()) {
                            Text(
                                text = "${negotiations.size} pending offer${if (negotiations.size > 1) "s" else ""}",
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
                            modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            negotiations.isEmpty() -> EmptyNegotiationsState(modifier = Modifier.padding(paddingValues))
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(paddingValues)
                ) {
                    items(negotiations) { item ->
                        NegotiationRequestCard(
                            negotiationWithDetails = item,
                            onAccept = {
                                scope.launch {
                                    try {
                                        firestore.collection("negotiations").document(item.negotiation.id)
                                            .update(hashMapOf<String, Any>("status" to "ACCEPTED", "responded_at" to System.currentTimeMillis())).await()
                                        try {
                                            val cartSnapshot = firestore.collection("cart")
                                                .whereEqualTo("user_id", item.negotiation.buyerId)
                                                .whereEqualTo("product_id", item.product.id)
                                                .get().await()
                                            if (!cartSnapshot.isEmpty) {
                                                firestore.collection("cart").document(cartSnapshot.documents[0].id)
                                                    .update(hashMapOf<String, Any>("price" to item.negotiation.offerAmount, "is_negotiated" to true, "negotiation_status" to "ACCEPTED")).await()
                                                Log.d("NegotiationRequests", "✅ Cart item updated with accepted price")
                                            }
                                        } catch (e: Exception) { Log.e("NegotiationRequests", "Failed to update cart item", e) }
                                        firestore.collection("notifications").add(
                                            hashMapOf<String, Any>(
                                                "user_id" to item.negotiation.buyerId, "title" to "Offer Accepted!",
                                                "description" to "Your offer of PKR ${item.negotiation.offerAmount.toInt()} for ${item.product.title} has been accepted",
                                                "category" to "ORDER", "action_type" to "VIEW_PRODUCT",
                                                "product_id" to item.product.id, "order_id" to "", "store_id" to "",
                                                "action_data" to hashMapOf<String, Any>(), "is_read" to false,
                                                "created_at" to System.currentTimeMillis()
                                            )
                                        ).await()
                                        negotiations = negotiations.filter { it.negotiation.id != item.negotiation.id }
                                        snackbarHostState.showSnackbar("Offer accepted!")
                                    } catch (e: Exception) { snackbarHostState.showSnackbar("Failed to accept offer") }
                                }
                            },
                            onReject = {
                                scope.launch {
                                    try {
                                        firestore.collection("negotiations").document(item.negotiation.id)
                                            .update(hashMapOf<String, Any>("status" to "REJECTED", "responded_at" to System.currentTimeMillis())).await()
                                        try {
                                            val cartSnapshot = firestore.collection("cart")
                                                .whereEqualTo("user_id", item.negotiation.buyerId)
                                                .whereEqualTo("product_id", item.product.id)
                                                .get().await()
                                            if (!cartSnapshot.isEmpty) {
                                                firestore.collection("cart").document(cartSnapshot.documents[0].id)
                                                    .update(hashMapOf<String, Any>("price" to item.product.price, "is_negotiated" to false, "negotiation_status" to "REJECTED")).await()
                                                Log.d("NegotiationRequests", "✅ Cart item updated with rejected status")
                                            }
                                        } catch (e: Exception) { Log.e("NegotiationRequests", "Failed to update cart item", e) }
                                        firestore.collection("notifications").add(
                                            hashMapOf<String, Any>(
                                                "user_id" to item.negotiation.buyerId, "title" to "Offer Declined",
                                                "description" to "Your offer for ${item.product.title} was declined by the seller",
                                                "category" to "ORDER", "action_type" to "VIEW_PRODUCT",
                                                "product_id" to item.product.id, "order_id" to "", "store_id" to "",
                                                "action_data" to hashMapOf<String, Any>(), "is_read" to false,
                                                "created_at" to System.currentTimeMillis()
                                            )
                                        ).await()
                                        negotiations = negotiations.filter { it.negotiation.id != item.negotiation.id }
                                        snackbarHostState.showSnackbar("Offer rejected")
                                    } catch (e: Exception) { snackbarHostState.showSnackbar("Failed to reject offer") }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Negotiation Request Card ──────────────────────────────────────────────────

@Composable
fun NegotiationRequestCard(
    negotiationWithDetails: NegotiationWithDetails,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val negotiation = negotiationWithDetails.negotiation
    val product = negotiationWithDetails.product
    val buyerName = negotiationWithDetails.buyerName
    val discount = ((negotiation.originalPrice - negotiation.offerAmount) / negotiation.originalPrice * 100).toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Product header — BackgroundSecondary band
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundSecondary)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BorderColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrls.isNotEmpty()) {
                        AsyncImage(
                            model = CloudinaryManager.getOptimizedUrl(product.imageUrls.first(), 150, 80),
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = product.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Text(text = "From: ", fontSize = 11.sp, color = TextSecondary)
                        RealtimeNameDisplay(
                            userId = negotiationWithDetails.negotiation.buyerId,
                            fallbackName = buyerName,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Price comparison row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Original", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "PKR ${negotiation.originalPrice.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textDecoration = TextDecoration.LineThrough
                    )
                }

                // Arrow in tinted circle — was a bare Icon
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Offered", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        text = "PKR ${negotiation.offerAmount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                // Discount badge — Warning tinted
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Warning.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$discount% off",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
fun EmptyNegotiationsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.50f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("No Pending Requests", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("You'll see buyer negotiation requests here", fontSize = 13.sp, color = TextSecondary)
    }
}

data class NegotiationWithDetails(
    val negotiation: NegotiationOffer,
    val product: Product,
    val buyerName: String
)