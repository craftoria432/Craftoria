package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.ui.theme.ThemeType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository for handling theme preference operations with Firebase
 * Manages persistence and retrieval of user theme selections
 */
class ThemeRepository(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ThemeRepository"
        private const val USERS_COLLECTION = "users"
        private const val THEME_PREFERENCE_FIELD = "theme_preference"
    }
    
    /**
     * Retrieve the user's theme preference from Firebase
     * Returns ROSE theme as default if preference is missing or invalid
     */
    suspend fun getUserThemePreference(userId: String): ThemeType = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            val themeStr = doc.getString(THEME_PREFERENCE_FIELD) ?: "rose"
            val theme = stringToThemeType(themeStr)
            Log.d(TAG, "✅ Retrieved theme for user $userId: ${theme.name}")
            theme
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving theme preference for user $userId, defaulting to ROSE", e)
            ThemeType.ROSE  // Default fallback
        }
    }
    
    /**
     * Update the user's theme preference in Firebase
     * Throws exception if update fails
     */
    suspend fun updateUserThemePreference(userId: String, theme: ThemeType) = withContext(Dispatchers.IO) {
        try {
            firestore.collection(USERS_COLLECTION).document(userId).update(
                mapOf(THEME_PREFERENCE_FIELD to themeTypeToString(theme))
            ).await()
            Log.d(TAG, "Theme preference updated for user $userId to ${theme.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating theme preference for user $userId", e)
            throw e
        }
    }
    
    /**
     * Initialize theme preference for a new user
     * Called during user creation to set initial theme
     */
    suspend fun initializeThemeForNewUser(userId: String, theme: ThemeType) = withContext(Dispatchers.IO) {
        try {
            firestore.collection(USERS_COLLECTION).document(userId).update(
                mapOf(THEME_PREFERENCE_FIELD to themeTypeToString(theme))
            ).await()
            Log.d(TAG, "Theme preference initialized for new user $userId to ${theme.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing theme preference for user $userId", e)
            throw e
        }
    }
    
    /**
     * Convert ThemeType enum to string for Firebase storage
     */
    private fun themeTypeToString(theme: ThemeType): String = theme.name.lowercase()
    
    /**
     * Convert string from Firebase to ThemeType enum
     * Returns ROSE as default if string is invalid
     */
    private fun stringToThemeType(value: String): ThemeType {
        return ThemeType.entries.firstOrNull { 
            it.name.equals(value, ignoreCase = true) 
        } ?: ThemeType.ROSE
    }
}
