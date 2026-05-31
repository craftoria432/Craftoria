package com.gcuf.craftoria.services

import android.content.Context
import android.util.Log
import com.gcuf.craftoria.data.repository.ThemeRepository
import com.gcuf.craftoria.ui.theme.ThemeManager
import com.gcuf.craftoria.ui.theme.ThemeType
import com.gcuf.craftoria.utils.ThemeMigration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for initializing theme on app startup.
 * Loads the user's saved theme from Firebase and applies it via [ThemeManager].
 * Also writes the result to [ThemePreferenceCache] so subsequent cold starts
 * can apply the correct theme synchronously before Firestore responds.
 */
class ThemeInitializationService(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val themeManager: ThemeManager,
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "ThemeInitializationService"
    }

    private val themeRepository = ThemeRepository(firestore, context)
    private val themeMigration = ThemeMigration(firestore)

    /**
     * Initialize theme on app startup:
     * - Authenticated user  → load their preference from Firebase, cache it locally
     * - Unauthenticated     → apply default Rose theme
     */
    suspend fun initializeTheme() = withContext(Dispatchers.IO) {
        try {
            val currentUser = firebaseAuth.currentUser

            if (currentUser != null) {
                Log.d(TAG, "User authenticated: ${currentUser.uid}")

                // Run migration for existing users (adds theme_preference field if missing)
                themeMigration.migrateUserIfNeeded(currentUser.uid)

                // Retrieve user's theme preference (also updates local cache)
                val theme = themeRepository.getUserThemePreference(currentUser.uid)
                Log.d(TAG, "✅ Retrieved theme preference: ${theme.name}")

                themeManager.initializeTheme(theme)
            } else {
                Log.d(TAG, "No authenticated user — applying default Rose theme")
                themeManager.initializeTheme(ThemeType.ROSE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing theme, falling back to Rose", e)
            themeManager.initializeTheme(ThemeType.ROSE)
        }
    }
}
