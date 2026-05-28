# Store Rating Feature - Implementation Guide

## Quick Summary

**Status**: ⚠️ Display works, but submission is missing
**Priority**: HIGH - Feature is incomplete
**Effort**: 3-4 days
**Impact**: Critical for user engagement

---

## What's Missing

1. ❌ No rating submission dialog
2. ❌ No rating storage
3. ❌ No rating calculation
4. ❌ No rating triggers
5. ❌ No rating management

---

## Implementation Steps

### Step 1: Create StoreRating Model

Create file: `app/src/main/java/com/gcuf/craftoria/data/model/StoreRating.kt`

```kotlin
package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class StoreRating(
    val id: String = "",
    
    @get:PropertyName("store_id")
    @set:PropertyName("store_id")
    var storeId: String = "",
    
    @get:PropertyName("buyer_id")
    @set:PropertyName("buyer_id")
    var buyerId: String = "",
    
    val rating: Int = 0,  // 1-5
    val review: String = "",
    
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = 0,
    
    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = 0
)

fun StoreRating.toMap(): Map<String, Any?> = mapOf(
    "store_id" to storeId,
    "buyer_id" to buyerId,
    "rating" to rating,
    "review" to review,
    "created_at" to (createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()),
    "updated_at" to System.currentTimeMillis()
)
```

### Step 2: Create Rating Dialog Component

Create file: `app/src/main/java/com/gcuf/craftoria/ui/components/RateStoreDialog.kt`

```kotlin
package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.CoSellerStore
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary

@Composable
fun RateStoreDialog(
    store: CoSellerStore,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, review: String) -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    var review by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rate ${store.storeName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Star Rating Selector
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = index + 1 }
                                .padding(4.dp),
                            tint = if (index < rating) Color(0xFFFFB400) else Color.LightGray
                        )
                    }
                }
                
                // Rating Text
                if (rating > 0) {
                    Text(
                        text = when (rating) {
                            1 -> "Poor"
                            2 -> "Fair"
                            3 -> "Good"
                            4 -> "Very Good"
                            5 -> "Excellent"
                            else -> ""
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                
                // Review Text Field
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Review (optional)") },
                    placeholder = { Text("Share your experience...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, review) },
                enabled = rating > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Step 3: Create Rating Repository

Create file: `app/src/main/java/com/gcuf/craftoria/data/repository/StoreRatingRepository.kt`

```kotlin
package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.StoreRating
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StoreRatingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ratingsCollection = db.collection("store_ratings")
    private val storesCollection = db.collection("co_seller_stores")
    
    suspend fun submitRating(
        storeId: String,
        buyerId: String,
        rating: Int,
        review: String
    ): Result<String> {
        return try {
            // Validate rating
            if (rating < 1 || rating > 5) {
                return Result.failure(Exception("Rating must be between 1 and 5"))
            }
            
            // Create rating
            val ratingData = StoreRating(
                storeId = storeId,
                buyerId = buyerId,
                rating = rating,
                review = review,
                createdAt = System.currentTimeMillis()
            )
            
            // Add to Firestore
            val docRef = ratingsCollection.add(ratingData.toMap()).await()
            
            // Recalculate average rating
            recalculateStoreRating(storeId)
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("StoreRatingRepository", "Failed to submit rating", e)
            Result.failure(e)
        }
    }
    
    private suspend fun recalculateStoreRating(storeId: String) {
        try {
            val ratings = ratingsCollection
                .whereEqualTo("store_id", storeId)
                .get()
                .await()
            
            if (ratings.isEmpty) {
                return
            }
            
            val ratingsList = ratings.documents.mapNotNull { doc ->
                doc.getLong("rating")?.toInt()
            }
            
            val averageRating = ratingsList.average()
            val ratingCount = ratingsList.size
            
            // Update store
            storesCollection.document(storeId).update(
                mapOf(
                    "average_rating" to averageRating,
                    "rating_count" to ratingCount,
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()
            
            Log.d("StoreRatingRepository", "Updated store rating: $averageRating")
        } catch (e: Exception) {
            Log.e("StoreRatingRepository", "Failed to recalculate rating", e)
        }
    }
}
```

### Step 4: Add Rating to StorePublicViewScreen

Update `StorePublicViewScreen.kt` to add rating button:

```kotlin
// Add this to the store info section
Button(
    onClick = { showRatingDialog = true },
    colors = ButtonDefaults.buttonColors(
        containerColor = Primary
    ),
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp)
) {
    Text("Rate This Store")
}

// Add rating dialog
if (showRatingDialog) {
    RateStoreDialog(
        store = currentStore!!,
        onDismiss = { showRatingDialog = false },
        onSubmit = { rating, review ->
            // Handle rating submission
            submitStoreRating(rating, review)
            showRatingDialog = false
        }
    )
}
```

---

## Firestore Schema Updates

### Add to CoSellerStore

```json
{
    "average_rating": 4.5,
    "rating_count": 12,
    "last_rated_at": 1234567890
}
```

### New Collection: store_ratings

```json
{
    "id": "rating_123",
    "store_id": "store_456",
    "buyer_id": "buyer_789",
    "rating": 5,
    "review": "Great store, excellent products!",
    "created_at": 1234567890,
    "updated_at": 1234567890
}
```

---

## Testing Checklist

- [ ] Can open rating dialog
- [ ] Can select 1-5 stars
- [ ] Can add review text
- [ ] Can submit rating
- [ ] Rating saves to Firestore
- [ ] Average rating updates
- [ ] Rating displays in store card
- [ ] Rating displays in store detail
- [ ] Can't submit invalid rating
- [ ] Error handling works

---

## Timeline

- **Day 1**: Create models and repository
- **Day 2**: Create UI components
- **Day 3**: Integrate with screens
- **Day 4**: Testing and fixes

---

## Priority

🔴 **HIGH** - This feature is incomplete and needs to be finished before production release.
