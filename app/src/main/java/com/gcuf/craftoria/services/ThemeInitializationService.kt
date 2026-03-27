package com.gcuf.craftoria.services

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
 * Service for initializing theme on app startup
 * Handles theme retrieval from Firebase and migration for existing users
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4
 */
class ThemeInitializationService(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val themeManager: ThemeManager
) {
    companion object {
        private const val TAG = "ThemeInitializationService"
    }
    
    private val themeRepository = ThemeRepository(firestore)
    private val themeMigration = ThemeMigration(firestore)
    
    /**
     * Initialize theme on app startup
     * - If user is authenticated: retrieve their theme preference from Firebase
     * - If user is not authenticated: apply default Rose theme
     * - For existing users: run migration to add theme_preference field if missing
     */
    suspend fun initializeTheme() = withContext(Dispatchers.IO) {
        try {
            val currentUser = firebaseAuth.currentUser
            
            if (currentUser != null) {
                Log.d(TAG, "User authenticated: ${currentUser.uid}")
                
                // Run migration for existing users
                themeMigration.migrateUserIfNeeded(currentUser.uid)
                
                // Retrieve user's theme preference
                val theme = themeRepository.getUserThemePreference(currentUser.uid)
                Log.d(TAG, "✅ Retrieved theme preference: ${theme.name}")
                
                // Apply theme
                themeManager.initializeTheme(theme)
            } else {
                Log.d(TAG, "No authenticated user - applying default Rose theme")
                
                // Apply default Rose theme for unauthenticated users
                themeManager.initializeTheme(ThemeType.ROSE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing theme", e)
            
            // Fallback to Rose theme on error
            themeManager.initializeTheme(ThemeType.ROSE)
        }
    }
}
