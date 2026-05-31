package com.gcuf.craftoria.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.gcuf.craftoria.ui.theme.ThemeType

/**
 * Local SharedPreferences cache for the user's theme preference.
 *
 * Firebase is the source of truth, but reading from Firestore on every cold start
 * causes a Rose-flash on the first frame because setContent runs before the async
 * Firestore call resolves.  This cache lets us seed ThemeManager synchronously in
 * onCreate() so the very first frame already uses the correct theme.
 *
 * Write path:  ThemeRepository.updateUserThemePreference  →  also calls saveTheme()
 * Read path:   MainActivity.onCreate (before setContent)  →  reads getSavedTheme()
 */
object ThemePreferenceCache {

    private const val PREFS_NAME = "craftoria_theme_prefs"
    private const val KEY_THEME = "theme_type"
    private const val KEY_USER_ID = "cached_user_id"
    private const val TAG = "ThemePreferenceCache"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Persist the theme for [userId] locally.
     * Called every time the user picks a theme or after a successful Firebase load.
     */
    fun saveTheme(context: Context, userId: String, theme: ThemeType) {
        prefs(context).edit()
            .putString(KEY_THEME, theme.name)
            .putString(KEY_USER_ID, userId)
            .apply()
        Log.d(TAG, "💾 Cached theme ${theme.name} for user $userId")
    }

    /**
     * Return the cached theme for [userId], or null if nothing is cached
     * (first install, different user, or cache was cleared).
     */
    fun getSavedTheme(context: Context, userId: String): ThemeType? {
        val p = prefs(context)
        val cachedUserId = p.getString(KEY_USER_ID, null)
        if (cachedUserId != userId) {
            Log.d(TAG, "ℹ️ No cache for user $userId (cached user: $cachedUserId)")
            return null
        }
        val name = p.getString(KEY_THEME, null) ?: return null
        return ThemeType.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
            .also { Log.d(TAG, "✅ Cache hit: ${it?.name} for user $userId") }
    }

    /** Clear the cache (e.g. on logout). */
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        Log.d(TAG, "🗑️ Theme cache cleared")
    }
}
