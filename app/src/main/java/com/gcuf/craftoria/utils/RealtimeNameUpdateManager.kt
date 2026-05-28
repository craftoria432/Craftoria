package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Manages real-time name updates across all screens
 * When a user changes their full name, all screens listening to this manager
 * will instantly update without requiring navigation or screen refresh
 */
object RealtimeNameUpdateManager {
    
    private val firestore = Firebase.firestore
    private val userNameCache = mutableMapOf<String, String>()
    private val nameUpdateListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
    
    // StateFlow for each user ID to track name changes
    private val userNameFlows = mutableMapOf<String, MutableStateFlow<String>>()
    
    /**
     * Get or create a StateFlow for a specific user's name
     * This allows screens to observe name changes in real-time
     */
    fun getUserNameFlow(userId: String): StateFlow<String> {
        return userNameFlows.getOrPut(userId) {
            MutableStateFlow("")
        }.asStateFlow()
    }
    
    /**
     * Start listening to real-time name updates for a specific user
     * Call this when a screen needs to display a user's name
     */
    fun startListeningToUserName(userId: String) {
        if (userId.isEmpty()) return
        
        // If already listening, don't create duplicate listener
        if (nameUpdateListeners.containsKey(userId)) {
            Log.d("RealtimeNameUpdateManager", "Already listening to user: $userId")
            return
        }
        
        Log.d("RealtimeNameUpdateManager", "Starting to listen to user name: $userId")
        
        val listener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RealtimeNameUpdateManager", "❌ Error listening to user $userId: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("name") ?: ""
                    
                    // Update cache
                    userNameCache[userId] = name
                    
                    // Update StateFlow
                    userNameFlows.getOrPut(userId) { MutableStateFlow("") }.value = name
                    
                    Log.d("RealtimeNameUpdateManager", "✅ Name updated for $userId: $name")
                }
            }
        
        nameUpdateListeners[userId] = listener
    }
    
    /**
     * Stop listening to real-time name updates for a specific user
     * Call this when a screen is destroyed or no longer needs updates
     */
    fun stopListeningToUserName(userId: String) {
        if (userId.isEmpty()) return
        
        nameUpdateListeners[userId]?.remove()
        nameUpdateListeners.remove(userId)
        
        Log.d("RealtimeNameUpdateManager", "Stopped listening to user: $userId")
    }
    
    /**
     * Get the cached name for a user (instant, no network call)
     */
    fun getCachedName(userId: String): String {
        return userNameCache[userId] ?: ""
    }
    
    /**
     * Update participant names in a chat document
     * This is called when a user changes their name
     */
    suspend fun updateChatParticipantNames(
        chatId: String,
        userId: String,
        newName: String
    ) {
        try {
            firestore.collection("chats")
                .document(chatId)
                .update("participant_names.$userId", newName)
                .await()
            
            Log.d("RealtimeNameUpdateManager", "✅ Updated chat participant name: $chatId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating chat participant name: ${e.message}")
        }
    }
    
    /**
     * Update all messages from a user with their new name
     * This ensures historical messages also show the updated name
     */
    suspend fun updateUserMessagesName(
        userId: String,
        newName: String
    ) {
        try {
            // Get all messages from this user
            val messagesSnapshot = firestore.collectionGroup("messages")
                .whereEqualTo("sender_id", userId)
                .get()
                .await()
            
            // Update each message in a batch
            val batch = firestore.batch()
            for (doc in messagesSnapshot.documents) {
                batch.update(doc.reference, "sender_name", newName)
            }
            batch.commit().await()
            
            Log.d("RealtimeNameUpdateManager", "✅ Updated all messages for user: $userId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating user messages: ${e.message}")
        }
    }
    
    /**
     * Update seller name in all their products
     */
    suspend fun updateProductSellerName(
        sellerId: String,
        newName: String
    ) {
        try {
            firestore.collection("products")
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()
                .let { snapshot ->
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.update(doc.reference, "seller_name", newName)
                    }
                    batch.commit().await()
                }
            
            Log.d("RealtimeNameUpdateManager", "✅ Updated seller name in all products: $sellerId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating product seller name: ${e.message}")
        }
    }
    
    /**
     * Update seller name in all their orders
     */
    suspend fun updateOrderSellerName(
        sellerId: String,
        newName: String
    ) {
        try {
            firestore.collection("orders")
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()
                .let { snapshot ->
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.update(doc.reference, "seller_name", newName)
                    }
                    batch.commit().await()
                }
            
            Log.d("RealtimeNameUpdateManager", "✅ Updated seller name in all orders: $sellerId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating order seller name: ${e.message}")
        }
    }
    
    /**
     * Update buyer name in all their orders
     */
    suspend fun updateOrderBuyerName(
        buyerId: String,
        newName: String
    ) {
        try {
            firestore.collection("orders")
                .whereEqualTo("buyer_id", buyerId)
                .get()
                .await()
                .let { snapshot ->
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.update(doc.reference, "buyer_name", newName)
                    }
                    batch.commit().await()
                }
            
            Log.d("RealtimeNameUpdateManager", "✅ Updated buyer name in all orders: $buyerId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating order buyer name: ${e.message}")
        }
    }
    
    /**
     * Update co-seller store name when a member updates their name
     * This ensures notifications show the updated store name in real-time
     */
    suspend fun updateCoSellerStoreNames(
        memberId: String,
        newName: String
    ) {
        try {
            // Find all co-seller stores where this user is a member
            val storesSnapshot = firestore.collection("co_seller_stores")
                .whereArrayContains("member_ids", memberId)
                .get()
                .await()
            
            // Update store_name in each store (assuming store_name is the member's name)
            val batch = firestore.batch()
            for (doc in storesSnapshot.documents) {
                // Update the store name if this member is the owner/primary member
                batch.update(doc.reference, "store_name", newName)
            }
            batch.commit().await()
            
            Log.d("RealtimeNameUpdateManager", "✅ Updated co-seller store names for member: $memberId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating co-seller store names: ${e.message}")
        }
    }
    
    /**
     * Update payment record names
     */
    suspend fun updatePaymentNames(
        paymentId: String,
        buyerName: String? = null,
        sellerName: String? = null
    ) {
        try {
            val updates = mutableMapOf<String, Any>()
            buyerName?.let { updates["buyer_name"] = it }
            sellerName?.let { updates["seller_name"] = it }
            
            if (updates.isNotEmpty()) {
                firestore.collection("payments")
                    .document(paymentId)
                    .update(updates)
                    .await()
                
                Log.d("RealtimeNameUpdateManager", "✅ Updated payment names: $paymentId")
            }
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating payment names: ${e.message}")
        }
    }

    /**
     * Update seller name across payment documents, including split records for co-seller payouts.
     * This keeps denormalized payment data aligned with the latest profile name.
     */
    suspend fun updateSellerPaymentNames(
        sellerId: String,
        newName: String
    ) {
        try {
            suspend fun updateCollection(collectionName: String) {
                val snapshot = firestore.collection(collectionName)
                    .whereArrayContains("involved_seller_ids", sellerId)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val updates = mutableMapOf<String, Any>()

                    if (doc.getString("seller_id") == sellerId) {
                        updates["seller_name"] = newName

                        // Original-seller payments mirror the seller name into store_name.
                        if ((doc.getString("co_seller_store_id") ?: "").isEmpty()) {
                            updates["store_name"] = newName
                        }
                    }

                    val splitMaps = (doc.get("payment_splits") as? List<*>)?.map { split ->
                        val splitMap = (split as? Map<*, *>)?.toMutableMap() ?: mutableMapOf<Any?, Any?>()
                        if (splitMap["seller_id"] == sellerId) {
                            splitMap["seller_name"] = newName
                        }
                        splitMap
                    }

                    if (splitMaps != null && splitMaps.isNotEmpty()) {
                        @Suppress("UNCHECKED_CAST")
                        updates["payment_splits"] = splitMaps as List<Map<String, Any?>>
                    }

                    if (updates.isNotEmpty()) {
                        doc.reference.update(updates).await()
                    }
                }
            }

            updateCollection("payments")
            // Note: seller_payments is legacy collection, no longer updated

            Log.d("RealtimeNameUpdateManager", "✅ Updated seller name in payment records: $sellerId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating seller payment names: ${e.message}")
        }
    }

    /**
     * Update buyer name across payment documents.
     */
    suspend fun updateBuyerPaymentNames(
        buyerId: String,
        newName: String
    ) {
        try {
            suspend fun updateCollection(collectionName: String) {
                val snapshot = firestore.collection(collectionName)
                    .whereEqualTo("buyer_id", buyerId)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    doc.reference.update("buyer_name", newName).await()
                }
            }

            updateCollection("payments")
            // Note: seller_payments is legacy collection, no longer updated

            Log.d("RealtimeNameUpdateManager", "✅ Updated buyer name in payment records: $buyerId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating buyer payment names: ${e.message}")
        }
    }
    
    /**
     * Update notification names
     */
    suspend fun updateNotificationNames(
        userId: String,
        newName: String
    ) {
        try {
            firestore.collection("notifications")
                .whereEqualTo("from_user_id", userId)
                .get()
                .await()
                .let { snapshot ->
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.update(doc.reference, "from_user_name", newName)
                    }
                    batch.commit().await()
                }
            
            Log.d("RealtimeNameUpdateManager", "✅ Updated notification names: $userId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error updating notification names: ${e.message}")
        }
    }
    
    /**
     * Comprehensive name update - call this when user changes their name
     * Updates all related documents across the app
     */
    suspend fun updateUserNameEverywhere(
        userId: String,
        newName: String,
        userRole: String
    ) {
        try {
            Log.d("RealtimeNameUpdateManager", "🔄 Starting comprehensive name update for $userId")
            
            // Update user document
            firestore.collection("users")
                .document(userId)
                .update("name", newName)
                .await()
            
            // Update cache and StateFlow
            userNameCache[userId] = newName
            userNameFlows.getOrPut(userId) { MutableStateFlow("") }.value = newName
            
            // Update all related documents
            updateUserMessagesName(userId, newName)
            updateNotificationNames(userId, newName)
            updateCoSellerStoreNames(userId, newName)  // ✅ NEW: Update co-seller stores
            
            // Role-specific updates
            if (userRole == "SELLER" || userRole == "CO_SELLER") {
                updateProductSellerName(userId, newName)
                updateOrderSellerName(userId, newName)
                updateSellerPaymentNames(userId, newName)
            }
            
            if (userRole == "BUYER") {
                updateOrderBuyerName(userId, newName)
                updateBuyerPaymentNames(userId, newName)
            }
            
            Log.d("RealtimeNameUpdateManager", "✅ Comprehensive name update completed for $userId")
        } catch (e: Exception) {
            Log.e("RealtimeNameUpdateManager", "❌ Error in comprehensive name update: ${e.message}")
        }
    }
    
    /**
     * Clean up all listeners (call on app shutdown)
     */
    fun cleanup() {
        nameUpdateListeners.values.forEach { it.remove() }
        nameUpdateListeners.clear()
        userNameFlows.clear()
        userNameCache.clear()
        Log.d("RealtimeNameUpdateManager", "✅ Cleaned up all listeners")
    }
}
