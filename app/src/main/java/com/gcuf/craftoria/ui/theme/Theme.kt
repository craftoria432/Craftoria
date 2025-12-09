package com.gcuf.craftoria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}