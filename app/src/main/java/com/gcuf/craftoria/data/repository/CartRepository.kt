package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.CartItem
import com.gcuf.craftoria.data.model.NegotiationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CartRepository {
    private val db = FirebaseFirestore.getInstance()
    private val cartCollection = db.collection("cart")
    
    companion object {
        private const val TAG = "CartRepository"
    }

    /**
     * Get user's cart items as a Flow (real-time updates)
     */
    fun getCartItems(userId: String): Flow<List<CartItem>> = callbackFlow {
        Log.d(TAG, "📦 Setting up real-time cart listener for user: $userId")
        
        val listener = cartCollection
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to cart", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(CartItem::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing cart item ${doc.id}", e)
                            null
                        }
                    }
                    Log.d(TAG, "✅ Cart updated: ${items.size} items")
                    trySend(items)
                }
            }
        
        awaitClose {
            Log.d(TAG, "🔌 Removing cart listener")
            listener.remove()
        }
    }

    /**
     * Add item to cart
     */
    suspend fun addToCart(cartItem: CartItem): Result<String> {
        return try {
            Log.d(TAG, "➕ Adding item to cart: ${cartItem.productId}")
            
            // Check if item already exists
            val existing = cartCollection
                .whereEqualTo("user_id", cartItem.userId)
                .whereEqualTo("product_id", cartItem.productId)
                .get()
                .await()
            
            if (!existing.isEmpty) {
                // Update existing item
                val docId = existing.documents[0].id
                cartCollection.document(docId).set(cartItem.toMap()).await()
                Log.d(TAG, "✅ Updated existing cart item: $docId")
                Result.success(docId)
            } else {
                // Add new item
                val doc = cartCollection.add(cartItem.toMap()).await()
                Log.d(TAG, "✅ Added new cart item: ${doc.id}")
                Result.success(doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add to cart", e)
            Result.failure(e)
        }
    }

    /**
     * Update cart item quantity
     */
    suspend fun updateQuantity(cartItemId: String, quantity: Int): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating quantity for $cartItemId to $quantity")
            cartCollection.document(cartItemId)
                .update("quantity", quantity)
                .await()
            Log.d(TAG, "✅ Quantity updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update quantity", e)
            Result.failure(e)
        }
    }

    /**
     * Update negotiation status and price
     */
    suspend fun updateNegotiationStatus(
        productId: String,
        userId: String,
        newPrice: Double,
        isNegotiated: Boolean,
        negotiationStatus: NegotiationStatus?
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating negotiation status for product: $productId")
            
            val snapshot = cartCollection
                .whereEqualTo("user_id", userId)
                .whereEqualTo("product_id", productId)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                val docId = snapshot.documents[0].id
                val updates = hashMapOf<String, Any>(
                    "price" to newPrice,
                    "is_negotiated" to isNegotiated
                )
                
                if (negotiationStatus != null) {
                    updates["negotiation_status"] = negotiationStatus.name
                }
                
                cartCollection.document(docId).update(updates).await()
                Log.d(TAG, "✅ Negotiation status updated")
                Result.success(Unit)
            } else {
                Log.w(TAG, "⚠️ Cart item not found")
                Result.failure(Exception("Cart item not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update negotiation status", e)
            Result.failure(e)
        }
    }

    /**
     * Remove item from cart
     */
    suspend fun removeFromCart(cartItemId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Removing cart item: $cartItemId")
            cartCollection.document(cartItemId).delete().await()
            Log.d(TAG, "✅ Cart item removed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove from cart", e)
            Result.failure(e)
        }
    }

    /**
     * Clear entire cart for user
     */
    suspend fun clearCart(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Clearing cart for user: $userId")
            
            val snapshot = cartCollection
                .whereEqualTo("user_id", userId)
                .get()
                .await()
            
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Log.d(TAG, "✅ Cart cleared: ${snapshot.size()} items removed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear cart", e)
            Result.failure(e)
        }
    }

    /**
     * Remove items after successful order
     */
    suspend fun removeOrderedItems(userId: String, productIds: List<String>): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Removing ordered items: ${productIds.size}")
            
            val snapshot = cartCollection
                .whereEqualTo("user_id", userId)
                .get()
                .await()
            
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                val productId = doc.getString("product_id")
                if (productId in productIds) {
                    batch.delete(doc.reference)
                }
            }
            batch.commit().await()
            
            Log.d(TAG, "✅ Ordered items removed from cart")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to remove ordered items", e)
            Result.failure(e)
        }
    }
}
