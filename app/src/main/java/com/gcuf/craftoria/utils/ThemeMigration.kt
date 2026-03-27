package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles migration of existing users to include theme_preference field
 * Initializes theme_preference to "rose" for users missing the field
 */
class ThemeMigration(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ThemeMigration"
        private const val USERS_COLLECTION = "users"
        private const val THEME_PREFERENCE_FIELD = "theme_preference"
        private const val DEFAULT_THEME = "rose"
    }
    
    /**
     * Check if user has theme_preference field and initialize if missing
     * Called on app startup for the current user
     */
    suspend fun migrateUserIfNeeded(userId: String) = withContext(Dispatchers.IO) {
        try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            
            // Check if theme_preference field exists
            if (!userDoc.contains(THEME_PREFERENCE_FIELD)) {
                Log.d(TAG, "Migrating user $userId - adding theme_preference field")
                
                // Initialize theme_preference to default rose theme
                firestore.collection(USERS_COLLECTION).document(userId).update(
                    mapOf(THEME_PREFERENCE_FIELD to DEFAULT_THEME)
                ).await()
                
                Log.d(TAG, "Successfully migrated user $userId")
            } else {
                Log.d(TAG, "User $userId already has theme_preference field")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating user $userId", e)
            // Don't throw - migration failure shouldn't break app startup
        }
    }
    
    /**
     * Batch migrate all users without theme_preference field
     * Use with caution - this is a heavy operation
     */
    suspend fun batchMigrateAllUsers() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting batch migration of all users")
            
            val usersSnapshot = firestore.collection(USERS_COLLECTION).get().await()
            var migratedCount = 0
            
            for (userDoc in usersSnapshot.documents) {
                if (!userDoc.contains(THEME_PREFERENCE_FIELD)) {
                    userDoc.reference.update(
                        mapOf(THEME_PREFERENCE_FIELD to DEFAULT_THEME)
                    ).await()
                    migratedCount++
                }
            }
            
            Log.d(TAG, "Batch migration complete. Migrated $migratedCount users")
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch migration", e)
            throw e
        }
    }
}
