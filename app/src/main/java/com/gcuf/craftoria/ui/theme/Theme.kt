package com.gcuf.craftoria.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Craftoria Brand Colors
private val CraftPrimary = Color(0xFF6750A4)
private val CraftSecondary = Color(0xFF625B71)
private val CraftTertiary = Color(0xFF7D5260)
private val CraftBackground = Color(0xFFFFFBFE)
private val CraftSurface = Color(0xFFFFFBFE)

private val DarkColorScheme = darkColorScheme(
    primary = CraftPrimary,
    secondary = CraftSecondary,
    tertiary = CraftTertiary,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F)
)

private val LightColorScheme = lightColorScheme(
    primary = CraftPrimary,
    secondary = CraftSecondary,
    tertiary = CraftTertiary,
    background = CraftBackground,
    surface = CraftSurface
)

@Composable
fun CraftoriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Get the current theme from ThemeManager
    val themeManager = ThemeManager.getInstance()
    val currentTheme = themeManager.currentTheme.collectAsState()
    val themeColors = themeManager.themeColors.collectAsState()
    
    android.util.Log.d("CraftoriaTheme", "🎨 Recomposing with theme: ${currentTheme.value.name}")
    
    // Create Material3 color scheme from the current theme colors
    val colors = themeColors.value
    val colorScheme = lightColorScheme(
        primary = colors.primary,
        primaryContainer = colors.primaryLight,
        onPrimary = Color.White,
        secondary = colors.secondary,
        secondaryContainer = colors.secondaryLight,
        onSecondary = Color.White,
        tertiary = colors.accentColor,
        background = colors.background,
        surface = colors.surfaceColor,
        error = colors.error,
        onError = Color.White
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalPrimary provides colors.primary,
        LocalPrimaryLight provides colors.primaryLight,
        LocalSecondary provides colors.secondary,
        LocalTertiary provides colors.accentColor,
        LocalBackgroundWhite provides colors.background,
        LocalBackgroundSecondary provides colors.backgroundSecondary,
        LocalBackgroundLight provides colors.backgroundLight,
        LocalTextPrimary provides colors.textPrimary,
        LocalTextSecondary provides colors.textSecondary,
        LocalTextLight provides colors.textLight,
        LocalBorderColor provides colors.borderColor,
        LocalSuccess provides colors.success,
        LocalWarning provides colors.warning,
        LocalError provides colors.error,
        LocalInfo provides colors.info,
        LocalCraftoriaGreen provides colors.success,
        LocalCraftoriaOrange provides colors.warning
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                // Apply background color to the entire content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.backgroundSecondary)
                ) {
                    content()
                }
            }
        )
    }
}
