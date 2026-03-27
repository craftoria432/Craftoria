package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility class for migrating existing chats to include participant avatars
 * This is a one-time migration utility that can be run to update all existing chats
 */
object ChatAvatarMigration {
    private const val TAG = "ChatAvatarMigration"
    private val db = FirebaseFirestore.getInstance()
    private val chatsCollection = db.collection("chats")
    private val usersCollection = db.collection("users")

    /**
     * Migrates all existing chats to include participant avatars
     * This should be called once after deploying the profile picture feature
     * 
     * @return Result with number of chats migrated
     */
    suspend fun migrateAllChats(): Result<Int> {
        return try {
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "🔄 Starting chat avatar migration")
            Log.d(TAG, "═══════════════════════════════════════")

            // Get all chats
            val chatsSnapshot = chatsCollection.get().await()
            Log.d(TAG, "📦 Found ${chatsSnapshot.documents.size} chats to migrate")

            var migratedCount = 0
            var skippedCount = 0
            var errorCount = 0

            for (chatDoc in chatsSnapshot.documents) {
                try {
                    val chatId = chatDoc.id
                    
                    // Check if already has avatars
                    @Suppress("UNCHECKED_CAST")
                    val existingAvatars = chatDoc.get("participant_avatars") as? Map<String, String>
                    
                    if (!existingAvatars.isNullOrEmpty()) {
                        Log.d(TAG, "   ⏭️ Chat $chatId already has avatars, skipping")
                        skippedCount++
                        continue
                    }

                    // Get participant IDs
                    @Suppress("UNCHECKED_CAST")
                    val participantIds = (chatDoc.get("participant_ids") as? List<*>)
                        ?.map { it.toString() } ?: emptyList()

                    if (participantIds.isEmpty()) {
                        Log.w(TAG, "   ⚠️ Chat $chatId has no participants, skipping")
                        skippedCount++
                        continue
                    }

                    // Fetch avatars for all participants
                    val avatars = mutableMapOf<String, String>()
                    for (userId in participantIds) {
                        try {
                            val userDoc = usersCollection.document(userId).get().await()
                            val profileImage = userDoc.getString("profile_image") ?: ""
                            if (profileImage.isNotEmpty()) {
                                avatars[userId] = profileImage
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "   ❌ Failed to fetch avatar for user $userId", e)
                        }
                    }

                    // Update chat with avatars
                    chatsCollection.document(chatId)
                        .update("participant_avatars", avatars)
                        .await()

                    Log.d(TAG, "   ✅ Migrated chat $chatId with ${avatars.size} avatars")
                    migratedCount++

                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Error migrating chat ${chatDoc.id}", e)
                    errorCount++
                }
            }

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "✅ Migration complete!")
            Log.d(TAG, "   Migrated: $migratedCount")
            Log.d(TAG, "   Skipped: $skippedCount")
            Log.d(TAG, "   Errors: $errorCount")
            Log.d(TAG, "═══════════════════════════════════════")

            Result.success(migratedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed", e)
            Result.failure(e)
        }
    }

    /**
     * Migrates a single user's chats to include their avatar
     * Useful for updating chats when a user updates their profile picture
     * 
     * @param userId The user ID to migrate chats for
     * @param avatarUrl The user's profile picture URL
     * @return Result with number of chats updated
     */
    suspend fun migrateUserChats(userId: String, avatarUrl: String): Result<Int> {
        return try {
            Log.d(TAG, "🔄 Migrating chats for user $userId")

            val userChats = chatsCollection
                .whereArrayContains("participant_ids", userId)
                .get()
                .await()

            val batch = db.batch()
            var updateCount = 0

            for (chatDoc in userChats.documents) {
                @Suppress("UNCHECKED_CAST")
                val currentAvatars = chatDoc.get("participant_avatars") as? Map<*, *> ?: emptyMap<String, String>()
                val updatedAvatars = currentAvatars.toMutableMap()
                updatedAvatars[userId] = avatarUrl

                batch.update(chatDoc.reference, "participant_avatars", updatedAvatars)
                updateCount++
            }

            batch.commit().await()
            Log.d(TAG, "✅ Updated $updateCount chats for user $userId")

            Result.success(updateCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to migrate user chats", e)
            Result.failure(e)
        }
    }
}
