package com.gcuf.craftoria.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Wrapper composable that provides smooth theme transitions
 * Animates color changes when theme is updated
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
@Composable
fun ThemeTransitionWrapper(
    themeColors: ThemeColors,
    content: @Composable () -> Unit
) {
    // Animate primary color as a representative of the theme change
    val animatedPrimaryColor = animateColorAsState(
        targetValue = themeColors.primary,
        animationSpec = tween(
            durationMillis = 300,  // 300ms smooth transition
            easing = EaseInOutCubic
        ),
        label = "ThemePrimaryColorTransition"
    )
    
    // Animate secondary color
    val animatedSecondaryColor = animateColorAsState(
        targetValue = themeColors.secondary,
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInOutCubic
        ),
        label = "ThemeSecondaryColorTransition"
    )
    
    // Animate background color
    val animatedBackgroundColor = animateColorAsState(
        targetValue = themeColors.background,
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInOutCubic
        ),
        label = "ThemeBackgroundColorTransition"
    )
    
    // Animate text primary color
    val animatedTextPrimaryColor = animateColorAsState(
        targetValue = themeColors.textPrimary,
        animationSpec = tween(
            durationMillis = 300,
            easing = EaseInOutCubic
        ),
        label = "ThemeTextPrimaryColorTransition"
    )
    
    // Create animated theme colors
    val animatedThemeColors = themeColors.copy(
        primary = animatedPrimaryColor.value,
        secondary = animatedSecondaryColor.value,
        background = animatedBackgroundColor.value,
        textPrimary = animatedTextPrimaryColor.value
    )
    
    // Provide animated colors to all child composables
    CompositionLocalProvider(
        LocalThemeColors provides animatedThemeColors
    ) {
        content()
    }
}

/**
 * Extension function to copy ThemeColors with updated values
 */
fun ThemeColors.copy(
    primary: Color = this.primary,
    primaryLight: Color = this.primaryLight,
    primaryDark: Color = this.primaryDark,
    secondary: Color = this.secondary,
    secondaryLight: Color = this.secondaryLight,
    background: Color = this.background,
    backgroundSecondary: Color = this.backgroundSecondary,
    backgroundLight: Color = this.backgroundLight,
    textPrimary: Color = this.textPrimary,
    textSecondary: Color = this.textSecondary,
    textLight: Color = this.textLight,
    success: Color = this.success,
    warning: Color = this.warning,
    error: Color = this.error,
    info: Color = this.info,
    borderColor: Color = this.borderColor,
    dividerColor: Color = this.dividerColor,
    surfaceColor: Color = this.surfaceColor,
    accentColor: Color = this.accentColor,
    disabledColor: Color = this.disabledColor
): ThemeColors = ThemeColors(
    primary = primary,
    primaryLight = primaryLight,
    primaryDark = primaryDark,
    secondary = secondary,
    secondaryLight = secondaryLight,
    background = background,
    backgroundSecondary = backgroundSecondary,
    backgroundLight = backgroundLight,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textLight = textLight,
    success = success,
    warning = warning,
    error = error,
    info = info,
    borderColor = borderColor,
    dividerColor = dividerColor,
    surfaceColor = surfaceColor,
    accentColor = accentColor,
    disabledColor = disabledColor
)
