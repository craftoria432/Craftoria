package com.gcuf.craftoria.ui.screens.coseller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.CoSellerStore
import com.gcuf.craftoria.ui.components.EmptyStateComponent
import com.gcuf.craftoria.ui.components.RateStoreDialog
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.StoreRatingState
import com.gcuf.craftoria.viewmodel.StoreRatingViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateStoreScreen(
    storeId: String,
    orderId: String,
    buyerId: String,
    buyerName: String,
    onBackClick: () -> Unit,
    onRated: () -> Unit = onBackClick,
    viewModel: StoreRatingViewModel = viewModel()
) {
    var store by remember { mutableStateOf<CoSellerStore?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val ratingState by viewModel.ratingState.collectAsState()
    val buyerRating by viewModel.buyerRating.collectAsState()

    LaunchedEffect(storeId, buyerId) {
        isLoading = true
        loadError = null
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("co_seller_stores")
                .document(storeId)
                .get()
                .await()
            store = doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)
            if (store == null) loadError = "Store not found"
            else viewModel.loadBuyerRating(storeId, buyerId)
        } catch (e: Exception) {
            loadError = e.message ?: "Failed to load store"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(ratingState) {
        if (ratingState is StoreRatingState.Success) {
            onRated()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Rate Store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                loadError != null || store == null -> {
                    EmptyStateComponent(
                        title = "Store unavailable",
                        message = loadError ?: "This store could not be loaded.",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    RateStoreDialog(
                        store = store!!,
                        currentRating = buyerRating?.rating ?: 0,
                        currentReview = buyerRating?.review ?: "",
                        onDismiss = onBackClick,
                        onSubmit = { rating, review ->
                            viewModel.submitRating(
                                storeId = storeId,
                                buyerId = buyerId,
                                rating = rating,
                                review = review,
                                buyerName = buyerName
                            )
                        },
                        isLoading = ratingState is StoreRatingState.Loading
                    )
                }
            }
        }
    }
}
