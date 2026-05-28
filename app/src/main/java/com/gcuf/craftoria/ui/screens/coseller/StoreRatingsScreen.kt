package com.gcuf.craftoria.ui.screens.coseller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
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
import com.gcuf.craftoria.data.model.StoreRating
import com.gcuf.craftoria.ui.components.EmptyStateComponent
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.StoreRatingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreRatingsScreen(
    storeId: String,
    onBackClick: () -> Unit,
    viewModel: StoreRatingViewModel = viewModel()
) {
    val ratings by viewModel.storeRatings.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(storeId) {
        isLoading = true
        viewModel.loadStoreRatings(storeId)
        isLoading = false
    }

    val average = if (ratings.isNotEmpty()) ratings.map { it.rating }.average() else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Store Ratings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (ratings.isNotEmpty()) {
                            Text(
                                "${String.format("%.1f", average)} avg · ${ratings.size} reviews",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
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
                ratings.isEmpty() -> {
                    EmptyStateComponent(
                        icon = Icons.Default.Star,
                        title = "No ratings yet",
                        message = "Customer reviews will appear here once buyers rate your store.",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ratings, key = { it.id.ifEmpty { "${it.buyerId}_${it.createdAt}" } }) { rating ->
                            StoreRatingCard(rating = rating)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreRatingCard(rating: StoreRating) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Primary.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("★", color = Primary, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("${rating.rating}/5", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(rating.createdAt)),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            if (rating.review.isNotBlank()) {
                Text(rating.review, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
            }
        }
    }
}
