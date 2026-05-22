package com.gcuf.craftoria.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.utils.CloudinaryManager
import kotlinx.coroutines.tasks.await

class CoSellerStoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storesCollection = db.collection("co_seller_stores")
    private val membersCollection = db.collection("store_members")
    private val invitationsCollection = db.collection("store_invitations")
    private val productsCollection = db.collection("products")
    private val usersCollection = db.collection("users")
    private val notificationsCollection = db.collection("notifications")

    companion object {
        private const val TAG = "CoSellerStoreRepository"
    }

    // Get all stores where user is OWNER or MEMBER
    suspend fun getUserStores(userId: String): Result<List<CoSellerStore>> {
        return try {
            Log.d(TAG, "Fetching stores for user (owner + member): $userId")

            // Get stores where user is owner
            val ownerStores = storesCollection
                .whereEqualTo("owner_id", userId)
                .whereEqualTo("is_active", true)
                .get()
                .await()
            Log.d(TAG, "Owner stores found: ${ownerStores.size()}")

            // Get stores where user is a member (using array-contains)
            val memberStores = storesCollection
                .whereArrayContains("member_ids", userId)
                .whereEqualTo("is_active", true)
                .get()
                .await()
            Log.d(TAG, "Member stores found: ${memberStores.size()}")

            val allStores = mutableListOf<CoSellerStore>()

            // Add owner stores
            ownerStores.documents.forEach { doc ->
                doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)?.let {
                    Log.d(TAG, "Adding owner store: ${it.storeName} (${it.id})")
                    allStores.add(it)
                }
            }

            // Add member stores (avoid duplicates)
            memberStores.documents.forEach { doc ->
                val store = doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)
                if (store != null && allStores.none { it.id == store.id }) {
                    Log.d(TAG, "Adding member store: ${store.storeName} (${store.id})")
                    allStores.add(store)
                } else if (store != null) {
                    Log.d(TAG, "Skipping duplicate store: ${store.storeName}")
                }
            }

            Log.d(TAG, "Total stores fetched: ${allStores.size} (${ownerStores.size()} owned, ${memberStores.size()} member)")
            Result.success(allStores.sortedByDescending { 
                when (val timestamp = it.createdAt) {
                    is com.google.firebase.Timestamp -> timestamp.toDate().time
                    is Long -> timestamp
                    else -> 0L
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user stores", e)
            Result.failure(e)
        }
    }

    // Create new store
    suspend fun createStore(
        context: Context,
        store: CoSellerStore,
        logoUri: Uri?,
        bannerUri: Uri?,
        invitedEmails: List<String> = emptyList()  // ✅ ADD THIS PARAMETER
    ): Result<String> {
        return try {
            Log.d(TAG, "Creating new store: ${store.storeName}")

            // Upload images if provided
            val logoUrl = logoUri?.let {
                CloudinaryManager.uploadImage(context, it, "craftoria/stores/logos")
            } ?: ""

            val bannerUrl = bannerUri?.let {
                CloudinaryManager.uploadImage(context, it, "craftoria/stores/banners")
            } ?: ""

            val storeData = store.copy(
                storeLogo = logoUrl,
                storeBanner = bannerUrl,
                memberIds = listOf(store.ownerId),  // Owner is first member
                memberCount = 1,
                createdAt = null,  // Will be set by toMap() as serverTimestamp
                updatedAt = null   // Will be set by toMap() as serverTimestamp
            )

            // Use toMap() to ensure proper field name conversion
            val docRef = storesCollection.add(storeData.toMap()).await()
            val storeId = docRef.id

            // Add owner as first member
            val member = StoreMember(
                userId = store.ownerId,
                userName = store.ownerName,
                storeId = storeId,
                isOwner = true,
                joinedAt = System.currentTimeMillis()
            )
            membersCollection.add(member.toMap()).await()

            // Auto-initialize equal split with owner as sole member
            recalculateAndSaveEqualSplits(storeId, listOf(store.ownerId))

            // ✅ Send invitations to all invited emails
            invitedEmails.forEach { email ->
                sendInvitationToEmail(
                    storeId = storeId,
                    storeName = store.storeName,
                    ownerId = store.ownerId,
                    ownerName = store.ownerName,
                    inviteeEmail = email.trim()
                )
            }

            Log.d(TAG, "✅ Store created successfully: $storeId")
            Result.success(storeId)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create store", e)
            Result.failure(e)
        }
    }

    // ✅ FIXED: Update sendInvitationToEmail function
    private suspend fun sendInvitationToEmail(
        storeId: String,
        storeName: String,
        ownerId: String,
        ownerName: String,
        inviteeEmail: String
    ) {
        try {
            Log.d(TAG, "Sending invitation to: $inviteeEmail")

            // Check if user exists with this email
            val userSnapshot = usersCollection
                .whereEqualTo("email", inviteeEmail)
                .get()
                .await()

            if (userSnapshot.documents.isNotEmpty()) {
                // ✅ CASE 1: User is registered - Send in-app notification
                val inviteeUser = userSnapshot.documents.first()
                val inviteeId = inviteeUser.id
                val inviteeName = inviteeUser.getString("name") ?: "User"

                // Check if invitation already exists
                val existingInvitation = invitationsCollection
                    .whereEqualTo("store_id", storeId)
                    .whereEqualTo("invitee_email", inviteeEmail)
                    .whereIn("status", listOf(
                        InvitationStatus.PENDING.name,
                        InvitationStatus.ACCEPTED.name
                    ))
                    .get()
                    .await()

                if (existingInvitation.documents.isEmpty()) {
                    // Get store to fetch current member count
                    val storeDoc = storesCollection.document(storeId).get().await()
                    val currentMemberCount = storeDoc.getLong("member_count")?.toInt() ?: 1
                    
                    // Create invitation
                    val invitation = StoreInvitation(
                        storeId = storeId,
                        storeName = storeName,
                        inviterId = ownerId,
                        inviterName = ownerName,
                        inviteeId = inviteeId,
                        inviteeName = inviteeName,
                        inviteeEmail = inviteeEmail,
                        status = InvitationStatus.PENDING,
                        sentAt = System.currentTimeMillis(),
                        isRegisteredUser = true
                    )

                    invitationsCollection.add(invitation.toMap()).await()

                    // ✅ Send notification using NotificationHelper with accurate member count
                    com.gcuf.craftoria.utils.NotificationHelper.notifyCoSellerInvitation(
                        inviteeId = inviteeId,
                        storeId = storeId,
                        storeName = storeName,
                        inviterName = ownerName,
                        memberCount = 0  // Will be fetched accurately by NotificationHelper
                    )

                    Log.d(TAG, "✅ In-app invitation sent to registered user: $inviteeEmail")
                } else {
                    Log.w(TAG, "⚠️ Invitation already exists for: $inviteeEmail")
                    throw Exception("Invitation already sent to this user")
                }
            } else {
                // ✅ CASE 2: User not registered - Throw error
                Log.w(TAG, "⚠️ User not registered: $inviteeEmail")
                throw Exception("User with email $inviteeEmail is not registered on Craftoria")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send invitation to $inviteeEmail: ${e.message}")
            throw e  // Re-throw to show error to user
        }
    }
    // Get store by ID
    suspend fun getStoreById(storeId: String): Result<CoSellerStore?> {
        return try {
            val doc = storesCollection.document(storeId).get().await()
            val store = doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)
            
            // ✅ Ensure memberCount is synced with actual memberIds length
            if (store != null) {
                val syncedStore = if (store.memberIds.isNotEmpty()) {
                    store.copy(memberCount = store.memberIds.size)
                } else {
                    store.copy(memberCount = 1) // At least owner
                }
                
                // ✅ Update Firestore if memberCount was 0 (migration for old stores)
                if (store.memberCount == 0 && syncedStore.memberCount > 0) {
                    try {
                        storesCollection.document(storeId).update(
                            "member_count", syncedStore.memberCount
                        ).await()
                        Log.d(TAG, "✅ Migrated member_count for store: $storeId to ${syncedStore.memberCount}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to migrate member_count", e)
                    }
                }
                
                Result.success(syncedStore)
            } else {
                Result.success(store)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get store", e)
            Result.failure(e)
        }
    }

    // Update store
    suspend fun updateStore(
        context: Context,
        storeId: String,
        store: CoSellerStore,
        newLogoUri: Uri?,
        newBannerUri: Uri?
    ): Result<Unit> {
        return try {
            var logoUrl = store.storeLogo
            var bannerUrl = store.storeBanner

            // Upload new images if provided
            newLogoUri?.let {
                logoUrl = CloudinaryManager.uploadImage(context, it, "craftoria/stores/logos")
            }

            newBannerUri?.let {
                bannerUrl = CloudinaryManager.uploadImage(context, it, "craftoria/stores/banners")
            }

            // Use snake_case field names for updates
            val updates = mapOf(
                "store_name" to store.storeName,
                "store_description" to store.storeDescription,
                "store_logo" to logoUrl,
                "store_banner" to bannerUrl,
                "updated_at" to System.currentTimeMillis()
            )

            storesCollection.document(storeId).update(updates).await()

            Log.d(TAG, "Store updated successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update store", e)
            Result.failure(e)
        }
    }

    // Delete store
    suspend fun deleteStore(storeId: String): Result<Unit> {
        return try {
            // Mark as inactive instead of deleting
            storesCollection.document(storeId)
                .update(
                    "is_active", false,
                    "updated_at", System.currentTimeMillis()
                )
                .await()

            Log.d(TAG, "Store deleted successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete store", e)
            Result.failure(e)
        }
    }

    // Get store members
    suspend fun getStoreMembers(storeId: String): Result<List<StoreMember>> {
        return try {
            val snapshot = membersCollection
                .whereEqualTo("store_id", storeId)
                .get()
                .await()

            val members = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoreMember::class.java)?.copy(id = doc.id)
            }.sortedBy { it.joinedAt }  // Sort in memory

            Result.success(members)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get store members", e)
            Result.failure(e)
        }
    }

    // Send invitation (public method for manual invites)
    // Send invitation (public method for manual invites)
    suspend fun sendInvitation(invitation: StoreInvitation): Result<String> {
        return try {
            // Check if user exists
            val userSnapshot = usersCollection
                .whereEqualTo("email", invitation.inviteeEmail)
                .get()
                .await()

            val invitationData = if (userSnapshot.documents.isNotEmpty()) {
                val user = userSnapshot.documents.first()
                invitation.copy(
                    inviteeId = user.id,
                    inviteeName = user.getString("name") ?: "",
                    isRegisteredUser = true
                )
            } else {
                invitation.copy(isRegisteredUser = false)
            }

            val docRef = invitationsCollection.add(invitationData.toMap()).await()

            // Create notification if user exists
            if (invitationData.inviteeId.isNotEmpty()) {
                // ✅ Use NotificationHelper for consistent member count handling
                com.gcuf.craftoria.utils.NotificationHelper.notifyCoSellerInvitation(
                    inviteeId = invitationData.inviteeId,
                    storeId = invitation.storeId,
                    storeName = invitation.storeName,
                    inviterName = invitation.inviterName,
                    memberCount = 0  // Will be fetched accurately by NotificationHelper
                )
            }

            Log.d(TAG, "Invitation sent successfully")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send invitation", e)
            Result.failure(e)
        }
    }
    // Get store invitations
    suspend fun getStoreInvitations(storeId: String): Result<List<StoreInvitation>> {
        return try {
            val snapshot = invitationsCollection
                .whereEqualTo("store_id", storeId)
                .get()
                .await()

            val invitations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoreInvitation::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.sentAt }  // Sort in memory

            Result.success(invitations)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get invitations", e)
            Result.failure(e)
        }
    }

    // ✅ Get user invitations (for notification screen)
    suspend fun getUserInvitations(userId: String): Result<List<StoreInvitation>> {
        return try {
            val snapshot = invitationsCollection
                .whereEqualTo("invitee_id", userId)
                .whereEqualTo("status", InvitationStatus.PENDING.name)
                .orderBy("sent_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val invitations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StoreInvitation::class.java)?.copy(id = doc.id)
            }

            Result.success(invitations)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user invitations", e)
            Result.failure(e)
        }
    }

    // ✅ Accept invitation
    suspend fun acceptInvitation(invitationId: String, userId: String, userName: String): Result<Unit> {
        return try {
            Log.d(TAG, "Accepting invitation: $invitationId for user: $userId")
            
            val invitationDoc = invitationsCollection.document(invitationId).get().await()
            val invitation = invitationDoc.toObject(StoreInvitation::class.java)
                ?: return Result.failure(Exception("Invitation not found"))

            Log.d(TAG, "Invitation found for store: ${invitation.storeId}")

            // Add user as member
            val member = StoreMember(
                userId = userId,
                userName = userName,
                storeId = invitation.storeId,
                isOwner = false,
                joinedAt = System.currentTimeMillis()
            )
            membersCollection.add(member.toMap()).await()
            Log.d(TAG, "Member added to members collection")

            // Update invitation status
            invitationsCollection.document(invitationId).update(
                mapOf(
                    "status" to InvitationStatus.ACCEPTED.name,
                    "responded_at" to System.currentTimeMillis()
                )
            ).await()
            Log.d(TAG, "Invitation status updated to ACCEPTED")

            // Update store member count
            val store = getStoreById(invitation.storeId).getOrNull()
            if (store != null) {
                Log.d(TAG, "Current member_ids: ${store.memberIds}")
                val updatedMemberIds = store.memberIds + userId
                Log.d(TAG, "Updated member_ids: $updatedMemberIds")
                
                storesCollection.document(invitation.storeId).update(
                    mapOf(
                        "member_ids" to updatedMemberIds,
                        "member_count" to updatedMemberIds.size,
                        "updated_at" to System.currentTimeMillis()
                    )
                ).await()
                Log.d(TAG, "Store member_ids and count updated successfully")
                
                // ✅ Update all existing notifications for this store with new member count
                com.gcuf.craftoria.utils.CoSellerMemberCountManager.updateAllStoreNotifications(invitation.storeId)
                
                // ✅ Recalculate equal splits with new member included
                recalculateAndSaveEqualSplits(invitation.storeId, updatedMemberIds)
                
            } else {
                Log.e(TAG, "Store not found: ${invitation.storeId}")
            }

            // ✅ Send notification to inviter that invitation was accepted with accurate member count
            val updatedMemberCount = (store?.memberCount ?: 0) + 1
            com.gcuf.craftoria.utils.NotificationHelper.notifyInvitationAccepted(
                inviterId = invitation.inviterId,
                storeId = invitation.storeId,
                storeName = invitation.storeName,
                accepterName = userName,
                memberCount = updatedMemberCount
            )

            Log.d(TAG, "✅ Invitation accepted successfully and notification sent to inviter")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to accept invitation", e)
            Result.failure(e)
        }
    }

    // ✅ Decline invitation
    suspend fun declineInvitation(invitationId: String): Result<Unit> {
        return try {
            invitationsCollection.document(invitationId).update(
                mapOf(
                    "status" to InvitationStatus.DECLINED.name,
                    "responded_at" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Invitation declined")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to decline invitation", e)
            Result.failure(e)
        }
    }

    // Remove member (Owner removes member)
    suspend fun removeMember(storeId: String, userId: String): Result<Unit> {
        return try {
            // Remove from members collection
            val members = membersCollection
                .whereEqualTo("store_id", storeId)
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            members.documents.forEach { it.reference.delete().await() }

            // Update store member_ids
            val store = getStoreById(storeId).getOrNull()
            store?.let {
                val updatedMemberIds = it.memberIds.filter { id -> id != userId }
                storesCollection.document(storeId).update(
                    mapOf(
                        "member_ids" to updatedMemberIds,
                        "member_count" to updatedMemberIds.size,
                        "updated_at" to System.currentTimeMillis()
                    )
                ).await()
                
                // ✅ Update all existing notifications for this store with new member count
                com.gcuf.craftoria.utils.CoSellerMemberCountManager.updateAllStoreNotifications(storeId)
                
                // ✅ Recalculate equal splits after member removal
                recalculateAndSaveEqualSplits(storeId, updatedMemberIds)
            }

            Log.d(TAG, "Member removed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove member", e)
            Result.failure(e)
        }
    }

    // ✅ NEW: Member leaves store (Member voluntarily leaves)
    suspend fun leaveStore(storeId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Member $userId leaving store: $storeId")
            
            // Get store to check if user is owner
            val store = getStoreById(storeId).getOrNull()
                ?: return Result.failure(Exception("Store not found"))

            // Prevent owner from leaving
            if (store.ownerId == userId) {
                Log.w(TAG, "Owner cannot leave store")
                return Result.failure(Exception("Store owner cannot leave the store"))
            }

            // Prevent leaving if only member (shouldn't happen, but safety check)
            if (store.memberIds.size <= 1) {
                Log.w(TAG, "Cannot leave - only member")
                return Result.failure(Exception("Cannot leave - you are the only member"))
            }

            // Remove from members collection
            val members = membersCollection
                .whereEqualTo("store_id", storeId)
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            members.documents.forEach { it.reference.delete().await() }
            Log.d(TAG, "Member removed from members collection")

            // Update store member_ids
            val updatedMemberIds = store.memberIds.filter { id -> id != userId }
            storesCollection.document(storeId).update(
                mapOf(
                    "member_ids" to updatedMemberIds,
                    "member_count" to updatedMemberIds.size,
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()
            Log.d(TAG, "Store member_ids and count updated")

            // ✅ Update all existing notifications for this store with new member count
            com.gcuf.craftoria.utils.CoSellerMemberCountManager.updateAllStoreNotifications(storeId)

            // ✅ Recalculate equal splits after member leaves
            recalculateAndSaveEqualSplits(storeId, updatedMemberIds)

            // ✅ Notify store owner that member left
            val memberName = usersCollection.document(userId).get().await().getString("name") ?: "A member"
            com.gcuf.craftoria.utils.NotificationHelper.notifyMemberLeftStore(
                ownerId = store.ownerId,
                storeId = storeId,
                storeName = store.storeName,
                memberName = memberName,
                memberCount = updatedMemberIds.size
            )

            Log.d(TAG, "✅ Member left store successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to leave store", e)
            Result.failure(e)
        }
    }

    // Update payment split configuration for a store
    suspend fun updatePaymentSplitConfig(
        storeId: String,
        splitConfig: Map<String, Double>
    ): Result<Unit> {
        return try {
            // Validate total equals 100%
            val total = splitConfig.values.sum()
            if (kotlin.math.abs(total - 1.0) > 0.01) {
                return Result.failure(Exception("Split percentages must add up to 100%"))
            }

            storesCollection.document(storeId).update(
                mapOf(
                    "payment_split_config" to splitConfig,
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Payment split config updated for store: $storeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update payment split config", e)
            Result.failure(e)
        }
    }

    // Auto-initialize equal splits when member list changes
    private suspend fun recalculateAndSaveEqualSplits(
        storeId: String,
        memberIds: List<String>
    ) {
        try {
            if (memberIds.isEmpty()) return
            val equalShare = 1.0 / memberIds.size
            val splitConfig = memberIds.associateWith { equalShare }
            storesCollection.document(storeId).update(
                "payment_split_config", splitConfig
            ).await()
            Log.d(TAG, "✅ Equal splits recalculated for ${memberIds.size} members")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to recalculate splits", e)
        }
    }

    // Delete product from store
    suspend fun deleteProduct(productId: String, storeId: String): Result<Unit> {
        return try {
            // Delete the product from Firestore
            productsCollection.document(productId).delete().await()

            // Decrement store product count
            val storeRef = storesCollection.document(storeId)
            val storeDoc = storeRef.get().await()
            
            if (storeDoc.exists()) {
                val currentCount = storeDoc.getLong("product_count")?.toInt() ?: 0
                if (currentCount > 0) {
                    storeRef.update("product_count", currentCount - 1).await()
                    Log.d(TAG, "Updated store product count: ${currentCount - 1}")
                }
            }

            Log.d(TAG, "Product deleted successfully: $productId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete product", e)
            Result.failure(e)
        }
    }

    // Get store products
    suspend fun getStoreProducts(storeId: String): Result<List<Product>> {
        return try {
            val snapshot = productsCollection
                .whereEqualTo("co_seller_store_id", storeId)
                .whereEqualTo("is_active", true)
                .whereEqualTo("approval_status", "approved")  // ✅ Only show approved products in public view
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }.sortedByDescending { 
                when (val timestamp = it.createdAt) {
                    is com.google.firebase.Timestamp -> timestamp.toDate().time
                    is Long -> timestamp
                    else -> 0L
                }
            }  // Sort in memory

            Result.success(products)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get store products", e)
            Result.failure(e)
        }
    }

    // ✅ NEW: Get ALL store products including pending (for manage products screen)
    suspend fun getAllStoreProducts(storeId: String): Result<List<Product>> {
        return try {
            val snapshot = productsCollection
                .whereEqualTo("co_seller_store_id", storeId)
                .whereEqualTo("is_active", true)
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }.sortedByDescending { 
                when (val timestamp = it.createdAt) {
                    is com.google.firebase.Timestamp -> timestamp.toDate().time
                    is Long -> timestamp
                    else -> 0L
                }
            }  // Sort in memory

            Result.success(products)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all store products", e)
            Result.failure(e)
        }
    }

    // Get public stores (for buyers)
    suspend fun getPublicStores(): Result<List<CoSellerStore>> {
        return try {
            val snapshot = storesCollection
                .whereEqualTo("is_active", true)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val stores = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)
            }

            Result.success(stores)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get public stores", e)
            Result.failure(e)
        }
    }

    // ✅ NEW: Run retroactive member count fixes for all stores and notifications
    suspend fun runRetroactiveMemberCountFix(): Result<Map<String, Any>> {
        return try {
            Log.d(TAG, "Starting retroactive member count fix for all stores")
            
            val results = com.gcuf.craftoria.utils.CoSellerMemberCountManager.auditAllStoresMemberCounts()
            
            val summary = mapOf(
                "total_stores_audited" to results.size,
                "stores_fixed" to results.values.count { it == 1 },
                "stores_with_errors" to results.values.count { it == -1 },
                "stores_already_accurate" to results.values.count { it == 0 }
            )
            
            Log.d(TAG, "Retroactive fix complete: $summary")
            Result.success(summary)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run retroactive member count fix", e)
            Result.failure(e)
        }
    }
}