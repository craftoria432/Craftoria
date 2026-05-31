package com.gcuf.craftoria.data.repository

import android.content.Context
import android.util.Log
import com.gcuf.craftoria.ui.theme.ThemeType
import com.gcuf.craftoria.utils.ThemePreferenceCache
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository for handling theme preference operations with Firebase.
 * Also writes to [ThemePreferenceCache] (SharedPreferences) so the theme
 * is available synchronously on the next cold start — eliminating the
 * Rose-flash that occurred while waiting for Firestore to respond.
 */
class ThemeRepository(
    private val firestore: FirebaseFirestore,
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "ThemeRepository"
        private const val USERS_COLLECTION = "users"
        private const val THEME_PREFERENCE_FIELD = "theme_preference"
    }

    /**
     * Retrieve the user's theme preference from Firebase.
     * Returns ROSE as default if the preference is missing or the call fails.
     */
    suspend fun getUserThemePreference(userId: String): ThemeType = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            val themeStr = doc.getString(THEME_PREFERENCE_FIELD) ?: "rose"
            val theme = stringToThemeType(themeStr)
            Log.d(TAG, "✅ Retrieved theme for user $userId: ${theme.name}")
            // Keep local cache in sync with what Firebase says
            context?.let { ThemePreferenceCache.saveTheme(it, userId, theme) }
            theme
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving theme preference for user $userId, defaulting to ROSE", e)
            ThemeType.ROSE
        }
    }

    /**
     * Update the user's theme preference in Firebase AND in the local cache.
     * Throws if the Firebase write fails (cache is only written on success).
     */
    suspend fun updateUserThemePreference(userId: String, theme: ThemeType) = withContext(Dispatchers.IO) {
        try {
            firestore.collection(USERS_COLLECTION).document(userId).update(
                mapOf(THEME_PREFERENCE_FIELD to themeTypeToString(theme))
            ).await()
            // Write to local cache so the next cold start uses this theme immediately
            context?.let { ThemePreferenceCache.saveTheme(it, userId, theme) }
            Log.d(TAG, "✅ Theme preference updated for user $userId to ${theme.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating theme preference for user $userId", e)
            throw e
        }
    }

    /**
     * Initialize theme preference for a new user.
     */
    suspend fun initializeThemeForNewUser(userId: String, theme: ThemeType) = withContext(Dispatchers.IO) {
        try {
            firestore.collection(USERS_COLLECTION).document(userId).update(
                mapOf(THEME_PREFERENCE_FIELD to themeTypeToString(theme))
            ).await()
            context?.let { ThemePreferenceCache.saveTheme(it, userId, theme) }
            Log.d(TAG, "✅ Theme preference initialized for new user $userId to ${theme.name}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing theme preference for user $userId", e)
            throw e
        }
    }

    private fun themeTypeToString(theme: ThemeType): String = theme.name.lowercase()

    private fun stringToThemeType(value: String): ThemeType =
        ThemeType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ThemeType.ROSE
}
