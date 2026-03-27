package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility class to manage accurate co-seller store member counts
 * Ensures notifications always show correct member counts retroactively and prospectively
 */
object CoSellerMemberCountManager {
    private const val TAG = "CoSellerMemberCountManager"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Get accurate member count for a co-seller store
     * Uses multiple fallback strategies to ensure accuracy
     */
    suspend fun getAccurateMemberCount(storeId: String): Int {
        return try {
            Log.d(TAG, "Fetching accurate member count for store: $storeId")
            
            val storeDoc = db.collection("co_seller_stores")
                .document(storeId)
                .get()
                .await()

            if (!storeDoc.exists()) {
                Log.w(TAG, "Store not found: $storeId")
                return 1 // Default to 1 (owner)
            }

            // Strategy 1: Use member_count field if it's valid (> 0)
            val memberCountField = storeDoc.getLong("member_count")?.toInt() ?: 0
            if (memberCountField > 0) {
                Log.d(TAG, "Using member_count field: $memberCountField")
                return memberCountField
            }

            // Strategy 2: Count member_ids array
            val memberIds = storeDoc.get("member_ids") as? List<*>
            if (!memberIds.isNullOrEmpty()) {
                val count = memberIds.size
                Log.d(TAG, "Using member_ids array size: $count")
                
                // Update the member_count field for future use
                try {
                    db.collection("co_seller_stores")
                        .document(storeId)
                        .update("member_count", count)
                        .await()
                    Log.d(TAG, "Updated member_count field to: $count")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update member_count field", e)
                }
                
                return count
            }

            // Strategy 3: Count from store_members collection
            val membersSnapshot = db.collection("store_members")
                .whereEqualTo("store_id", storeId)
                .get()
                .await()
            
            val membersCount = membersSnapshot.size()
            if (membersCount > 0) {
                Log.d(TAG, "Using store_members collection count: $membersCount")
                
                // Update both member_count and member_ids for consistency
                try {
                    val memberUserIds = membersSnapshot.documents.mapNotNull { 
                        it.getString("user_id") 
                    }
                    
                    db.collection("co_seller_stores")
                        .document(storeId)
                        .update(
                            mapOf(
                                "member_count" to membersCount,
                                "member_ids" to memberUserIds
                            )
                        )
                        .await()
                    Log.d(TAG, "Updated store with member_count: $membersCount and member_ids: $memberUserIds")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update store member data", e)
                }
                
                return membersCount
            }

            // Strategy 4: Default to 1 (at least the owner)
            Log.w(TAG, "No member data found, defaulting to 1 for store: $storeId")
            return 1

        } catch (e: Exception) {
            Log.e(TAG, "Error getting member count for store: $storeId", e)
            return 1 // Safe default
        }
    }

    /**
     * Update notification with accurate member count
     * Used for retroactive fixes
     */
    suspend fun updateNotificationMemberCount(notificationId: String, storeId: String): Boolean {
        return try {
            val accurateCount = getAccurateMemberCount(storeId)
            
            db.collection("notifications")
                .document(notificationId)
                .update("member_count", accurateCount)
                .await()
            
            Log.d(TAG, "Updated notification $notificationId with member count: $accurateCount")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification member count", e)
            false
        }
    }

    /**
     * Batch update all notifications for a store with accurate member count
     * Used for retroactive fixes when store membership changes
     */
    suspend fun updateAllStoreNotifications(storeId: String): Int {
        return try {
            val accurateCount = getAccurateMemberCount(storeId)
            
            val notificationsSnapshot = db.collection("notifications")
                .whereEqualTo("store_id", storeId)
                .get()
                .await()

            val batch = db.batch()
            var updateCount = 0

            notificationsSnapshot.documents.forEach { doc ->
                val currentCount = doc.getLong("member_count")?.toInt() ?: 0
                if (currentCount != accurateCount) {
                    batch.update(doc.reference, "member_count", accurateCount)
                    updateCount++
                }
            }

            if (updateCount > 0) {
                batch.commit().await()
                Log.d(TAG, "Updated $updateCount notifications for store $storeId with member count: $accurateCount")
            } else {
                Log.d(TAG, "No notifications needed updating for store: $storeId")
            }

            updateCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to batch update store notifications", e)
            0
        }
    }

    /**
     * Validate and fix member count for a store
     * Ensures consistency across all store data
     */
    suspend fun validateAndFixStoreMemberCount(storeId: String): Boolean {
        return try {
            Log.d(TAG, "Validating member count for store: $storeId")
            
            val storeDoc = db.collection("co_seller_stores")
                .document(storeId)
                .get()
                .await()

            if (!storeDoc.exists()) {
                Log.w(TAG, "Store not found during validation: $storeId")
                return false
            }

            // Get actual member count from store_members collection
            val membersSnapshot = db.collection("store_members")
                .whereEqualTo("store_id", storeId)
                .get()
                .await()
            
            val actualMemberCount = membersSnapshot.size()
            val memberUserIds = membersSnapshot.documents.mapNotNull { 
                it.getString("user_id") 
            }

            // Get current stored values
            val storedMemberCount = storeDoc.getLong("member_count")?.toInt() ?: 0
            val storedMemberIds = storeDoc.get("member_ids") as? List<*> ?: emptyList<String>()

            // Check if update is needed
            val needsUpdate = storedMemberCount != actualMemberCount || 
                             storedMemberIds.size != memberUserIds.size ||
                             !storedMemberIds.containsAll(memberUserIds)

            if (needsUpdate) {
                // Update store document
                db.collection("co_seller_stores")
                    .document(storeId)
                    .update(
                        mapOf(
                            "member_count" to actualMemberCount,
                            "member_ids" to memberUserIds,
                            "updated_at" to System.currentTimeMillis()
                        )
                    )
                    .await()

                Log.d(TAG, "Fixed store member data - Count: $actualMemberCount, IDs: $memberUserIds")

                // Update all related notifications
                updateAllStoreNotifications(storeId)
                
                return true
            } else {
                Log.d(TAG, "Store member count is already accurate: $actualMemberCount")
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate/fix store member count", e)
            false
        }
    }

    /**
     * Run comprehensive member count audit for all stores
     * Used for system-wide retroactive fixes
     */
    suspend fun auditAllStoresMemberCounts(): Map<String, Int> {
        val results = mutableMapOf<String, Int>()
        
        try {
            Log.d(TAG, "Starting comprehensive member count audit")
            
            val storesSnapshot = db.collection("co_seller_stores")
                .whereEqualTo("is_active", true)
                .get()
                .await()

            Log.d(TAG, "Auditing ${storesSnapshot.size()} active stores")

            storesSnapshot.documents.forEach { storeDoc ->
                val storeId = storeDoc.id
                try {
                    val wasFixed = validateAndFixStoreMemberCount(storeId)
                    results[storeId] = if (wasFixed) 1 else 0
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to audit store: $storeId", e)
                    results[storeId] = -1 // Error indicator
                }
            }

            val fixedCount = results.values.count { it == 1 }
            val errorCount = results.values.count { it == -1 }
            
            Log.d(TAG, "Audit complete - Fixed: $fixedCount, Errors: $errorCount, Total: ${results.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to run comprehensive audit", e)
        }

        return results
    }
}