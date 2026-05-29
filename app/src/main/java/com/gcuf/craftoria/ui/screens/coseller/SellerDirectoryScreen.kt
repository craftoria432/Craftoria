package com.gcuf.craftoria.ui.screens.coseller

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextField
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.ui.screens.seller.SellerPublicProfileScreen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextFieldCompact

data class SellerDirectoryItem(
    val userId: String,
    val name: String,
    val email: String,
    val profilePicture: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDirectoryScreen(
    currentStoreId: String,
    currentUserId: String,
    onSellerSelected: (SellerDirectoryItem) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> }, // ✅ Chat navigation callback
    onNavigateToProductPreview: (String) -> Unit = { _ -> } // ✅ NEW: Product preview navigation
) {
    var searchQuery by remember { mutableStateOf("") }
    var sellers by remember { mutableStateOf<List<SellerDirectoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var storeMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSellerForProfile by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentStoreId, currentUserId) {
        try {
            val db = FirebaseFirestore.getInstance()
            
            // Fetch current store members
            val storeDoc = db.collection("co_seller_stores").document(currentStoreId).get().await()
            val memberIds = (storeDoc.get("member_ids") as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
            storeMembers = memberIds
            
            // Fetch all sellers excluding current user and store members
            val sellersSnapshot = db.collection("users")
                .whereEqualTo("role", "seller")
                .get()
                .await()
            
            val sellersList = sellersSnapshot.documents.mapNotNull { doc ->
                val userId = doc.id
                // Exclude current user and existing store members
                if (userId == currentUserId || userId in memberIds) return@mapNotNull null
                
                // ✅ NEW: Exclude deleted users
                val status = doc.getString("status") ?: ""
                if (status == "deleted") return@mapNotNull null
                
                SellerDirectoryItem(
                    userId = userId,
                    name = doc.getString("name") ?: "Unknown",
                    email = doc.getString("email") ?: "",
                    profilePicture = doc.getString("profile_image") ?: ""
                )
            }.sortedBy { it.name }
            
            sellers = sellersList
            isLoading = false
        } catch (e: Exception) {
            Log.e("SellerDirectoryScreen", "Error loading sellers", e)
            isLoading = false
        }
    }

    val filteredSellers = sellers.filter { seller ->
        seller.name.contains(searchQuery, ignoreCase = true) ||
        seller.email.contains(searchQuery, ignoreCase = true)
    }

    // Show profile if seller selected
    if (selectedSellerForProfile != null) {
        // ✅ FIX: Prevent directory's BackHandler from interfering with profile navigation
        // The profile screen has its own BackHandler that will close the profile first
        SellerPublicProfileScreen(
            sellerId = selectedSellerForProfile!!,
            currentUserId = currentUserId,
            onBackClick = { 
                // ✅ Only close the profile, stay in directory
                selectedSellerForProfile = null 
            },
            onProductClick = { productId ->
                // ✅ NEW: Navigate to product preview in seller preview mode
                onNavigateToProductPreview(productId)
            },
            onChatWithSeller = { sellerId, sellerName ->
                // ✅ FIX: Navigate to chat screen
                selectedSellerForProfile = null
                onBackClick() // Close directory
                onNavigateToChat(sellerId, sellerName) // Navigate to chat
            },
            onAddToCart = {},
            onAddToWishlist = {},
            onNavigateToCart = {},
            onInviteClick = {
                // Find the seller and send invitation
                sellers.find { it.userId == selectedSellerForProfile }?.let { seller ->
                    onSellerSelected(seller)
                    selectedSellerForProfile = null
                }
            }
        )
        return
    }

    // ✅ Handle system back button to close seller directory overlay
    // Only active when profile is NOT showing
    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Browse Sellers",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field
            Surface(
                color = Color.White,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        StandardizedOutlinedTextFieldCompact(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search by name or email",
                            singleLine = true,
                            minHeight = 48
                        )
                    }
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                filteredSellers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (sellers.isEmpty()) "No sellers available" else "No results found",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            if (sellers.isNotEmpty()) {
                                Text(
                                    text = "Try a different search",
                                    fontSize = 12.sp,
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredSellers) { seller ->
                            SellerDirectoryCard(
                                seller = seller,
                                onSelect = { onSellerSelected(seller) },
                                onViewProfile = { selectedSellerForProfile = seller.userId }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerDirectoryCard(
    seller: SellerDirectoryItem,
    onSelect: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Avatar + Name + Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = seller.name.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Seller info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seller.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = seller.email,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onViewProfile,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Invite", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}
