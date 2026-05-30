package com.gcuf.craftoria.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    
    // Animate all color transitions for smooth theme switching
    val colors = themeColors.value
    val animatedPrimary by animateColorAsState(
        targetValue = colors.primary,
        animationSpec = tween(durationMillis = 300),
        label = "primary"
    )
    val animatedPrimaryLight by animateColorAsState(
        targetValue = colors.primaryLight,
        animationSpec = tween(durationMillis = 300),
        label = "primaryLight"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = colors.secondary,
        animationSpec = tween(durationMillis = 300),
        label = "secondary"
    )
    val animatedSecondaryLight by animateColorAsState(
        targetValue = colors.secondaryLight,
        animationSpec = tween(durationMillis = 300),
        label = "secondaryLight"
    )
    val animatedBackground by animateColorAsState(
        targetValue = colors.background,
        animationSpec = tween(durationMillis = 300),
        label = "background"
    )
    val animatedBackgroundSecondary by animateColorAsState(
        targetValue = colors.backgroundSecondary,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundSecondary"
    )
    val animatedBackgroundLight by animateColorAsState(
        targetValue = colors.backgroundLight,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundLight"
    )
    val animatedTextPrimary by animateColorAsState(
        targetValue = colors.textPrimary,
        animationSpec = tween(durationMillis = 300),
        label = "textPrimary"
    )
    val animatedTextSecondary by animateColorAsState(
        targetValue = colors.textSecondary,
        animationSpec = tween(durationMillis = 300),
        label = "textSecondary"
    )
    val animatedTextLight by animateColorAsState(
        targetValue = colors.textLight,
        animationSpec = tween(durationMillis = 300),
        label = "textLight"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = colors.borderColor,
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )
    val animatedSuccess by animateColorAsState(
        targetValue = colors.success,
        animationSpec = tween(durationMillis = 300),
        label = "success"
    )
    val animatedWarning by animateColorAsState(
        targetValue = colors.warning,
        animationSpec = tween(durationMillis = 300),
        label = "warning"
    )
    val animatedError by animateColorAsState(
        targetValue = colors.error,
        animationSpec = tween(durationMillis = 300),
        label = "error"
    )
    val animatedInfo by animateColorAsState(
        targetValue = colors.info,
        animationSpec = tween(durationMillis = 300),
        label = "info"
    )
    val animatedAccentColor by animateColorAsState(
        targetValue = colors.accentColor,
        animationSpec = tween(durationMillis = 300),
        label = "accentColor"
    )
    val animatedSurfaceColor by animateColorAsState(
        targetValue = colors.surfaceColor,
        animationSpec = tween(durationMillis = 300),
        label = "surfaceColor"
    )
    
    // Create Material3 color scheme with animated colors
    val colorScheme = lightColorScheme(
        primary = animatedPrimary,
        primaryContainer = animatedPrimaryLight,
        onPrimary = Color.White,
        secondary = animatedSecondary,
        secondaryContainer = animatedSecondaryLight,
        onSecondary = Color.White,
        tertiary = animatedAccentColor,
        background = animatedBackground,
        surface = animatedSurfaceColor,
        error = animatedError,
        onError = Color.White
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalPrimary provides animatedPrimary,
        LocalPrimaryLight provides animatedPrimaryLight,
        LocalSecondary provides animatedSecondary,
        LocalTertiary provides animatedAccentColor,
        LocalBackgroundWhite provides animatedBackground,
        LocalBackgroundSecondary provides animatedBackgroundSecondary,
        LocalBackgroundLight provides animatedBackgroundLight,
        LocalTextPrimary provides animatedTextPrimary,
        LocalTextSecondary provides animatedTextSecondary,
        LocalTextLight provides animatedTextLight,
        LocalBorderColor provides animatedBorderColor,
        LocalSuccess provides animatedSuccess,
        LocalWarning provides animatedWarning,
        LocalError provides animatedError,
        LocalInfo provides animatedInfo,
        LocalCraftoriaGreen provides animatedSuccess,
        LocalCraftoriaOrange provides animatedWarning
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                // Apply background color to the entire content with animation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(animatedBackgroundSecondary)
                ) {
                    content()
                }
            }
        )
    }
}
