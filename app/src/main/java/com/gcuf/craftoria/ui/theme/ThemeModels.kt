package com.gcuf.craftoria.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Enum representing the available theme options in Craftoria
 */
enum class ThemeType {
    ROSE,      // Pink theme (default)
    OCEAN      // Blue theme
}

/**
 * Data class containing all colors for a specific theme
 * Provides a complete color palette for UI components
 */
data class ThemeColors(
    // Primary colors
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    
    // Secondary colors
    val secondary: Color,
    val secondaryLight: Color,
    
    // Background colors
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundLight: Color,
    
    // Text colors
    val textPrimary: Color,
    val textSecondary: Color,
    val textLight: Color,
    
    // State colors
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    
    // Border and UI
    val borderColor: Color,
    val dividerColor: Color,
    val surfaceColor: Color,
    
    // Additional theme-specific colors
    val accentColor: Color,
    val disabledColor: Color
)
