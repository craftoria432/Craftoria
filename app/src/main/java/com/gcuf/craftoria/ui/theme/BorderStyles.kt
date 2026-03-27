package com.gcuf.craftoria.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized border styles for consistent UI across all screens
 * All border properties must be accessed within @Composable functions
 */
object BorderStyles {
    
    // ── Primary Borders ──────────────────────────────────────────────────────
    
    /** Primary card border - subtle, professional */
    @Composable
    fun cardBorder() = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
    
    /** Elevated card border - slightly more prominent */
    @Composable
    fun elevatedCardBorder() = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
    
    /** Interactive card border - for clickable cards */
    @Composable
    fun interactiveCardBorder() = BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f))
    
    // ── Input Field Borders ──────────────────────────────────────────────────
    
    /** Default input field border */
    @Composable
    fun inputBorder() = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
    
    /** Focused input field border */
    @Composable
    fun inputFocusedBorder() = BorderStroke(1.5.dp, Primary)
    
    /** Error input field border */
    @Composable
    fun inputErrorBorder() = BorderStroke(1.5.dp, Error)
    
    // ── Button Borders ───────────────────────────────────────────────────────
    
    /** Primary outlined button border */
    @Composable
    fun primaryButtonBorder() = BorderStroke(1.5.dp, Primary)
    
    /** Secondary outlined button border */
    @Composable
    fun secondaryButtonBorder() = BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f))
    
    /** Disabled button border */
    @Composable
    fun disabledButtonBorder() = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
    
    // ── Accent Borders ───────────────────────────────────────────────────────
    
    /** Success border - for positive states */
    @Composable
    fun successBorder() = BorderStroke(1.dp, Success)
    
    /** Warning border - for warning states */
    @Composable
    fun warningBorder() = BorderStroke(1.dp, Color(0xFFFFA500))
    
    /** Error border - for error states */
    @Composable
    fun errorBorder() = BorderStroke(1.dp, Error)
    
    /** Info border - for informational states */
    @Composable
    fun infoBorder() = BorderStroke(1.dp, Primary.copy(alpha = 0.7f))
    
    // ── Divider Styles ───────────────────────────────────────────────────────
    
    /** Subtle divider - for light separation */
    @Composable
    fun subtleDivider() = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.2f))
    
    /** Standard divider - for normal separation */
    @Composable
    fun standardDivider() = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
    
    /** Prominent divider - for strong separation */
    @Composable
    fun prominentDivider() = BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f))
    
    // ── Special Borders ──────────────────────────────────────────────────────
    
    /** Highlight border - for selected/highlighted items */
    @Composable
    fun highlightBorder() = BorderStroke(2.dp, Color(0xFFE91E63).copy(alpha = 0.6f))
    
    /** Premium border - for premium/featured items */
    @Composable
    fun premiumBorder() = BorderStroke(1.5.dp, Color(0xFFFFD700))
    
    /** Negotiation border - for negotiation-related items */
    @Composable
    fun negotiationBorder() = BorderStroke(1.dp, Color(0xFF4CAF50))
    
    // ── Helper Functions ─────────────────────────────────────────────────────
    
    /**
     * Create a custom border with specified width and color
     */
    fun custom(width: Dp, color: Color) = BorderStroke(width, color)
    
    /**
     * Create a border with custom opacity
     */
    fun withOpacity(border: BorderStroke, alpha: Float): BorderStroke {
        val originalColor = when (val brush = border.brush) {
            is SolidColor -> brush.value
            else -> Color.Gray
        }
        return BorderStroke(border.width, originalColor.copy(alpha = alpha))
    }
}

/**
 * Extension functions for easy border application
 * These must be used within @Composable functions
 */
object BorderExtensions {
    
    /**
     * Apply standard card border
     */
    @Composable
    fun Modifier.cardBorder() = this.border(
        BorderStyles.cardBorder(), 
        RoundedCornerShape(12.dp)
    )
    
    /**
     * Apply elevated card border
     */
    @Composable
    fun Modifier.elevatedCardBorder() = this.border(
        BorderStyles.elevatedCardBorder(), 
        RoundedCornerShape(12.dp)
    )
    
    /**
     * Apply interactive card border
     */
    @Composable
    fun Modifier.interactiveCardBorder() = this.border(
        BorderStyles.interactiveCardBorder(), 
        RoundedCornerShape(12.dp)
    )
}
