package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.StoreRating
import com.gcuf.craftoria.data.model.toMap
import com.gcuf.craftoria.data.model.Notification
import com.gcuf.craftoria.data.model.NotificationCategory
import com.gcuf.craftoria.data.model.NotificationActionType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StoreRatingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ratingsCollection = db.collection("store_ratings")
    private val storesCollection = db.collection("co_seller_stores")
    private val notificationsCollection = db.collection("notifications")
    private val usersCollection = db.collection("users")

    companion object {
        private const val TAG = "StoreRatingRepository"
    }

    suspend fun submitRating(
        storeId: String,
        buyerId: String,
        rating: Int,
        review: String,
        buyerName: String = ""
    ): Result<String> {
        return try {
            // Validate rating
            if (rating < 1 || rating > 5) {
                return Result.failure(Exception("Rating must be between 1 and 5"))
            }

            // Check for existing rating from same buyer
            val existingRating = ratingsCollection
                .whereEqualTo("store_id", storeId)
                .whereEqualTo("buyer_id", buyerId)
                .get()
                .await()

            val ratingData = StoreRating(
                storeId = storeId,
                buyerId = buyerId,
                rating = rating,
                review = review,
                createdAt = System.currentTimeMillis()
            )

            val docRef = if (existingRating.isEmpty) {
                // New rating
                ratingsCollection.add(ratingData.toMap()).await()
            } else {
                // Update existing rating
                val docId = existingRating.documents.first().id
                ratingsCollection.document(docId).set(ratingData.toMap()).await()
                existingRating.documents.first().reference
            }

            // Recalculate average rating
            recalculateStoreRating(storeId)

            // Send notification to store owners
            sendRatingNotification(storeId, buyerId, buyerName, rating, review)

            Log.d(TAG, "Rating submitted successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit rating", e)
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
                // No ratings yet, set to 0
                storesCollection.document(storeId).update(
                    mapOf(
                        "average_rating" to 0.0,
                        "rating_count" to 0,
                        "updated_at" to System.currentTimeMillis()
                    )
                ).await()
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

            Log.d(TAG, "Updated store rating: $averageRating (count: $ratingCount)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recalculate rating", e)
        }
    }

    private suspend fun sendRatingNotification(
        storeId: String,
        buyerId: String,
        buyerName: String,
        rating: Int,
        review: String
    ) {
        try {
            // Get store details
            val storeDoc = storesCollection.document(storeId).get().await()
            val storeName = storeDoc.getString("store_name") ?: "Store"
            val ownerId = storeDoc.getString("owner_id") ?: return
            val memberIds = storeDoc.get("member_ids") as? List<String> ?: emptyList()
            
            // Calculate actual member count (owner + members)
            val actualMemberCount = 1 + memberIds.size

            // Get buyer name if not provided
            val finalBuyerName = if (buyerName.isNotEmpty()) {
                buyerName
            } else {
                try {
                    usersCollection.document(buyerId).get().await().getString("name") ?: "A buyer"
                } catch (e: Exception) {
                    "A buyer"
                }
            }

            // ✅ Notify store owner with member count
            com.gcuf.craftoria.utils.NotificationHelper.notifyStoreRatingReceived(
                sellerId = ownerId,
                storeId = storeId,
                storeName = storeName,
                buyerName = finalBuyerName,
                rating = rating,
                review = review,
                memberCount = actualMemberCount
            )

            // Also notify other store members if they exist
            memberIds.forEach { memberId ->
                if (memberId != ownerId) {
                    com.gcuf.craftoria.utils.NotificationHelper.notifyStoreRatingReceived(
                        sellerId = memberId,
                        storeId = storeId,
                        storeName = storeName,
                        buyerName = finalBuyerName,
                        rating = rating,
                        review = review,
                        memberCount = actualMemberCount
                    )
                }
            }

            Log.d(TAG, "✅ Rating notification sent to store owners with member count: $actualMemberCount")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send rating notification", e)
            // Don't fail the rating submission if notification fails
        }
    }

    suspend fun getStoreRatings(storeId: String): Result<List<StoreRating>> {
        return try {
            val ratings = ratingsCollection
                .whereEqualTo("store_id", storeId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val ratingsList = ratings.documents.mapNotNull { doc ->
                doc.toObject(StoreRating::class.java)?.copy(id = doc.id)
            }

            Result.success(ratingsList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ratings", e)
            Result.failure(e)
        }
    }

    suspend fun getBuyerRating(storeId: String, buyerId: String): Result<StoreRating?> {
        return try {
            val rating = ratingsCollection
                .whereEqualTo("store_id", storeId)
                .whereEqualTo("buyer_id", buyerId)
                .get()
                .await()

            val ratingData = if (rating.isEmpty) {
                null
            } else {
                rating.documents.first().toObject(StoreRating::class.java)?.copy(id = rating.documents.first().id)
            }

            Result.success(ratingData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch buyer rating", e)
            Result.failure(e)
        }
    }
}
