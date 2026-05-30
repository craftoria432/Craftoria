package com.gcuf.craftoria.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages theme state and provides theme colors to the application
 * Handles theme initialization, switching, and distribution
 * 
 * Singleton pattern ensures a single instance is used throughout the app
 */
class ThemeManager private constructor() {
    private val _currentTheme = MutableStateFlow<ThemeType>(ThemeType.ROSE)
    val currentTheme: StateFlow<ThemeType> = _currentTheme.asStateFlow()
    
    private val _themeColors = MutableStateFlow<ThemeColors>(getRoseColors())
    val themeColors: StateFlow<ThemeColors> = _themeColors.asStateFlow()
    
    init {
        android.util.Log.d("ThemeManager", "🎨 Initialized with Rose theme (default)")
    }
    
    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()
    
    companion object {
        @Volatile
        private var instance: ThemeManager? = null
        
        fun getInstance(): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager().also { instance = it }
            }
        }
    }
    
    /**
     * Initialize theme for a user (typically on app startup)
     */
    fun initializeTheme(themeType: ThemeType) {
        setTheme(themeType)
    }
    
    /**
     * Set the current theme and update all theme colors
     */
    fun setTheme(themeType: ThemeType) {
        _isTransitioning.value = true
        try {
            _currentTheme.value = themeType
            _themeColors.value = getThemeColors(themeType)
            android.util.Log.d("ThemeManager", "✅ Theme changed to: ${themeType.name}")
        } finally {
            _isTransitioning.value = false
        }
    }
    
    /**
     * Get the color palette for a specific theme type
     */
    fun getThemeColors(themeType: ThemeType): ThemeColors {
        return when (themeType) {
            ThemeType.ROSE -> getRoseColors()
            ThemeType.OCEAN -> getOceanColors()
            ThemeType.PURPLE -> getPurpleColors()
        }
    }
    
    /**
     * Rose Theme - Pink color palette (default)
     */
    private fun getRoseColors(): ThemeColors = ThemeColors(
        // Primary colors
        primary = Color(0xFFE91E63),           // Pink
        primaryLight = Color(0xFFF06292),      // Light Pink
        primaryDark = Color(0xFFC2185B),       // Dark Pink
        
        // Secondary colors
        secondary = Color(0xFF625B71),         // Gray-Purple
        secondaryLight = Color(0xFF8B7B8F),    // Light Gray-Purple
        
        // Background colors
        background = Color(0xFFFFFFFF),        // White
        backgroundSecondary = Color(0xFFF8F9FA), // Light Gray
        backgroundLight = Color(0xFFFAFAFA),   // Very Light Gray
        
        // Text colors
        textPrimary = Color(0xFF333333),       // Dark Gray
        textSecondary = Color(0xFF666666),     // Medium Gray
        textLight = Color(0xFFAAAAAA),         // Light Gray
        
        // State colors
        success = Color(0xFF4CAF50),           // Green
        warning = Color(0xFFFFA726),           // Orange
        error = Color(0xFFF44336),             // Red
        info = Color(0xFF2196F3),              // Blue
        
        // Border and UI
        borderColor = Color(0xFFE0E0E0),       // Light Border
        dividerColor = Color(0xFFEEEEEE),      // Light Divider
        surfaceColor = Color(0xFFFAFAFA),      // Surface
        
        // Additional theme-specific colors
        accentColor = Color(0xFFE91E63),       // Pink Accent
        disabledColor = Color(0xFFBDBDBD)      // Disabled Gray
    )
    
    /**
     * Ocean Theme - Blue color palette
     */
    private fun getOceanColors(): ThemeColors = ThemeColors(
        // Primary colors
        primary = Color(0xFF0288D1),           // Light Blue
        primaryLight = Color(0xFF03A9F4),      // Lighter Blue
        primaryDark = Color(0xFF0277BD),       // Dark Blue
        
        // Secondary colors
        secondary = Color(0xFF0097A7),         // Cyan
        secondaryLight = Color(0xFF00BCD4),    // Light Cyan
        
        // Background colors
        background = Color(0xFFFFFFFF),        // White
        backgroundSecondary = Color(0xFFF0F7FA), // Light Blue-Gray
        backgroundLight = Color(0xFFF5F9FC),   // Very Light Blue
        
        // Text colors
        textPrimary = Color(0xFF1A237E),       // Dark Blue
        textSecondary = Color(0xFF37474F),     // Blue-Gray
        textLight = Color(0xFF78909C),         // Light Blue-Gray
        
        // State colors
        success = Color(0xFF00897B),           // Teal
        warning = Color(0xFFFFA726),           // Orange
        error = Color(0xFFD32F2F),             // Red
        info = Color(0xFF0288D1),              // Light Blue
        
        // Border and UI
        borderColor = Color(0xFFB3E5FC),       // Light Blue Border
        dividerColor = Color(0xFFE1F5FE),      // Very Light Blue Divider
        surfaceColor = Color(0xFFF5F9FC),      // Surface
        
        // Additional theme-specific colors
        accentColor = Color(0xFF0288D1),       // Blue Accent
        disabledColor = Color(0xFF90CAF9)      // Disabled Light Blue
    )
    
    /**
     * Purple Theme - Purple color palette
     */
    private fun getPurpleColors(): ThemeColors = ThemeColors(
        // Primary colors
        primary = Color(0xFF9C27B0),           // Purple
        primaryLight = Color(0xFFBA68C8),      // Light Purple
        primaryDark = Color(0xFF7B1FA2),       // Dark Purple
        
        // Secondary colors
        secondary = Color(0xFF673AB7),         // Deep Purple
        secondaryLight = Color(0xFF9575CD),    // Light Deep Purple
        
        // Background colors
        background = Color(0xFFFFFFFF),        // White
        backgroundSecondary = Color(0xFFF3E5F5), // Light Purple-Gray
        backgroundLight = Color(0xFFF8F5FA),   // Very Light Purple
        
        // Text colors
        textPrimary = Color(0xFF4A148C),       // Dark Purple
        textSecondary = Color(0xFF6A1B9A),     // Medium Purple
        textLight = Color(0xFF9C27B0),         // Light Purple
        
        // State colors
        success = Color(0xFF66BB6A),           // Green
        warning = Color(0xFFFFA726),           // Orange
        error = Color(0xFFEF5350),             // Red
        info = Color(0xFF42A5F5),              // Blue
        
        // Border and UI
        borderColor = Color(0xFFE1BEE7),       // Light Purple Border
        dividerColor = Color(0xFFF3E5F5),      // Very Light Purple Divider
        surfaceColor = Color(0xFFF8F5FA),      // Surface
        
        // Additional theme-specific colors
        accentColor = Color(0xFF9C27B0),       // Purple Accent
        disabledColor = Color(0xFFCE93D8)      // Disabled Light Purple
    )
    
}
