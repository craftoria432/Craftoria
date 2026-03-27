package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WishlistRepository {

    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "WishlistRepository"
    }

    private fun wishlistCollection(userId: String) =
        firestore.collection("users").document(userId).collection("wishlist")

    /**
     * Add a product to wishlist
     */
    suspend fun addToWishlist(userId: String, product: Product): Result<Unit> {
        return try {
            val data = mapOf(
                "id"          to product.id,
                "title"       to product.title,
                "price"       to product.price,
                "category"    to product.category,
                "seller_id"   to product.sellerId,
                "seller_name" to product.sellerName,
                "seller_verified" to product.sellerVerified,
                "image_urls"  to product.imageUrls,
                "is_negotiable" to product.isNegotiable,
                "stock"       to product.stock,
                "is_active"   to product.isActive,
                "is_draft"    to product.isDraft,
                "added_at"    to System.currentTimeMillis()
            )
            wishlistCollection(userId).document(product.id).set(data).await()
            Log.d(TAG, "Added to wishlist: ${product.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add to wishlist", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a product from wishlist
     */
    suspend fun removeFromWishlist(userId: String, productId: String): Result<Unit> {
        return try {
            wishlistCollection(userId).document(productId).delete().await()
            Log.d(TAG, "Removed from wishlist: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove from wishlist", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a product is in wishlist (one-time check)
     */
    suspend fun isInWishlist(userId: String, productId: String): Boolean {
        return try {
            val doc = wishlistCollection(userId).document(productId).get().await()
            doc.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check wishlist", e)
            false
        }
    }

    /**
     * Get all wishlist products as real-time Flow
     */
    fun getWishlistProducts(userId: String): Flow<List<Product>> = callbackFlow {
        val listener = wishlistCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Wishlist listener error", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing wishlist product: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Wishlist updated: ${products.size} items")
                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get wishlist product IDs as real-time Flow (lightweight, for heart button state)
     */
    fun getWishlistIds(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = wishlistCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }

        awaitClose { listener.remove() }
    }
}