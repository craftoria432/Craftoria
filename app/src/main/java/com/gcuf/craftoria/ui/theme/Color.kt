package com.gcuf.craftoria.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// CompositionLocal for theme colors - these will be provided by CraftoriaTheme
val LocalPrimary = compositionLocalOf { Color(0xFFE91E63) }
val LocalPrimaryLight = compositionLocalOf { Color(0xFFF06292) }
val LocalSecondary = compositionLocalOf { Color(0xFF625B71) }
val LocalTertiary = compositionLocalOf { Color(0xFF7D5260) }

val LocalBackgroundWhite = compositionLocalOf { Color(0xFFFFFFFF) }
val LocalBackgroundSecondary = compositionLocalOf { Color(0xFFF8F9FA) }
val LocalBackgroundLight = compositionLocalOf { Color(0xFFFAFAFA) }

val LocalTextPrimary = compositionLocalOf { Color(0xFF333333) }
val LocalTextSecondary = compositionLocalOf { Color(0xFF666666) }
val LocalTextLight = compositionLocalOf { Color(0xFFAAAAAA) }
val LocalBorderColor = compositionLocalOf { Color(0xFFE0E0E0) }

val LocalSuccess = compositionLocalOf { Color(0xFF4CAF50) }
val LocalWarning = compositionLocalOf { Color(0xFFFFA726) }
val LocalError = compositionLocalOf { Color(0xFFF44336) }
val LocalInfo = compositionLocalOf { Color(0xFF2196F3) }

val LocalCraftoriaGreen = compositionLocalOf { Color(0xFF4CAF50) }
val LocalCraftoriaOrange = compositionLocalOf { Color(0xFFFFA726) }

// Backward compatibility - use these in @Composable functions
// Example: Box(modifier = Modifier.background(Primary))
val Primary: Color
    @androidx.compose.runtime.Composable
    get() = LocalPrimary.current

val PrimaryLight: Color
    @androidx.compose.runtime.Composable
    get() = LocalPrimaryLight.current

val Secondary: Color
    @androidx.compose.runtime.Composable
    get() = LocalSecondary.current

val Tertiary: Color
    @androidx.compose.runtime.Composable
    get() = LocalTertiary.current

val BackgroundWhite: Color
    @androidx.compose.runtime.Composable
    get() = LocalBackgroundWhite.current

val BackgroundSecondary: Color
    @androidx.compose.runtime.Composable
    get() = LocalBackgroundSecondary.current

val BackgroundLight: Color
    @androidx.compose.runtime.Composable
    get() = LocalBackgroundLight.current

val TextPrimary: Color
    @androidx.compose.runtime.Composable
    get() = LocalTextPrimary.current

val TextSecondary: Color
    @androidx.compose.runtime.Composable
    get() = LocalTextSecondary.current

val TextLight: Color
    @androidx.compose.runtime.Composable
    get() = LocalTextLight.current

val BorderColor: Color
    @androidx.compose.runtime.Composable
    get() = LocalBorderColor.current

val Success: Color
    @androidx.compose.runtime.Composable
    get() = LocalSuccess.current

val Warning: Color
    @androidx.compose.runtime.Composable
    get() = LocalWarning.current

val Error: Color
    @androidx.compose.runtime.Composable
    get() = LocalError.current

val Info: Color
    @androidx.compose.runtime.Composable
    get() = LocalInfo.current

val CraftoriaGreen: Color
    @androidx.compose.runtime.Composable
    get() = LocalCraftoriaGreen.current

val CraftoriaOrange: Color
    @androidx.compose.runtime.Composable
    get() = LocalCraftoriaOrange.current