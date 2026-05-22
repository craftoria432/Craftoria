package com.gcuf.craftoria.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.utils.RealtimeNameUpdateManager

/**
 * Composable that displays a user's name with real-time updates
 * When the user changes their name, this automatically updates without screen refresh
 * 
 * Usage:
 * RealtimeNameDisplay(
 *     userId = "user123",
 *     fallbackName = "Unknown User",
 *     fontSize = 16.sp,
 *     fontWeight = FontWeight.Bold
 * )
 */
@Composable
fun RealtimeNameDisplay(
    userId: String,
    fallbackName: String = "Unknown User",
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Black,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle? = null
) {
    // Start listening when this composable is first composed
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            RealtimeNameUpdateManager.startListeningToUserName(userId)
        }
    }
    
    // Observe name changes
    val nameFlow = remember(userId) {
        RealtimeNameUpdateManager.getUserNameFlow(userId)
    }
    val displayName by nameFlow.collectAsState()
    
    // Use cached name as fallback if StateFlow is empty
    val nameToDisplay = if (displayName.isNotEmpty()) {
        displayName
    } else {
        RealtimeNameUpdateManager.getCachedName(userId).ifEmpty { fallbackName }
    }
    
    // Stop listening when composable is disposed
    DisposableEffect(userId) {
        onDispose {
            if (userId.isNotEmpty()) {
                RealtimeNameUpdateManager.stopListeningToUserName(userId)
            }
        }
    }
    
    if (style != null) {
        Text(
            text = nameToDisplay,
            modifier = modifier,
            style = style
        )
    } else {
        Text(
            text = nameToDisplay,
            modifier = modifier,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            lineHeight = lineHeight
        )
    }
}
