package com.gcuf.craftoria.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for providing theme colors throughout the app
 * Use LocalThemeColors.current to access the current theme's colors
 */
val LocalThemeColors = compositionLocalOf<ThemeColors> {
    error("ThemeColors not provided. Ensure ThemeProvider wraps your content.")
}

/**
 * CompositionLocal for providing the current theme type
 * Use LocalCurrentTheme.current to access the current theme type
 */
val LocalCurrentTheme = compositionLocalOf<ThemeType> {
    error("CurrentTheme not provided. Ensure ThemeProvider wraps your content.")
}
