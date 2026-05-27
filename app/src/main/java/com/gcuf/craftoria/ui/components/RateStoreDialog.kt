package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.gcuf.craftoria.data.model.CoSellerStore
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary

@Composable
fun RateStoreDialog(
    store: CoSellerStore,
    currentRating: Int = 0,
    currentReview: String = "",
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, review: String) -> Unit,
    isLoading: Boolean = false
) {
    var rating by remember { mutableStateOf(currentRating) }
    var review by remember { mutableStateOf(currentReview) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Rate ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                RealtimeNameDisplay(
                    userId = store.ownerId,
                    fallbackName = store.storeName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Star row — centred, unselected stars use BorderColor
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star ${index + 1}",
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(enabled = !isLoading) { rating = index + 1 }
                                .padding(3.dp),
                            tint = if (index < rating) Color(0xFFFFB400) else BorderColor
                        )
                    }
                }

                // Rating label pill
                if (rating > 0) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            color = Primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = when (rating) {
                                    1 -> "Poor"; 2 -> "Fair"; 3 -> "Good"
                                    4 -> "Very Good"; 5 -> "Excellent"; else -> ""
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Review field — consistent OutlinedTextField styling
                OutlinedTextField(
                    value = review,
                    onValueChange = { if (it.length <= 500) review = it },
                    label = { Text("Review (optional)", fontSize = 12.sp) },
                    placeholder = {
                        Text("Share your experience...", fontSize = 13.sp, color = TextSecondary)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Text(
                    text = "${review.length}/500",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            // Gradient fill — consistent primary CTA pattern
            Button(
                onClick = { onSubmit(rating, review) },
                enabled = rating > 0 && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .background(
                        if (rating > 0 && !isLoading)
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                        else
                            Brush.horizontalGradient(listOf(BorderColor, BorderColor)),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (currentRating > 0) "Update Rating" else "Submit Rating",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}