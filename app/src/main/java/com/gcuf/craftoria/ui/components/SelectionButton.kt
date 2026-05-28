package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextPrimary

/**
 * Unified selection button component for consistent styling across all selection contexts.
 * 
 * Standardized specifications:
 * - Height: 48.dp (minimum, matches CraftoriaTextField)
 * - Border radius: 10.dp (matches CraftoriaTextField)
 * - Padding: 12.dp (internal)
 * - Border: 0.5.dp (unselected) / 1.5.dp (selected)
 * - Professional alignment and spacing
 * 
 * Used for:
 * - Payment method selection (CheckoutScreen)
 * - Refund reason selection (BuyerRefundRequestScreen)
 * - Any other selection/radio button contexts
 */
@Composable
fun SelectionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showSelectedBadge: Boolean = true,
    minHeight: Int = 48
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF5F8) else Color.White
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) Primary else BorderColor
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            // Radio button
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Primary)
            )

            // Icon (if provided)
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Primary else com.gcuf.craftoria.ui.theme.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Text
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Primary else TextPrimary
            )

            // Selected badge
            if (isSelected && showSelectedBadge) {
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = Primary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Selected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Variant with icon on the left side (for payment methods, etc.)
 */
@Composable
fun SelectionButtonWithIcon(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSelectedBadge: Boolean = true,
    minHeight: Int = 48
) {
    SelectionButton(
        text = text,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        showSelectedBadge = showSelectedBadge,
        minHeight = minHeight
    )
}

/**
 * Compact variant without selected badge (for refund reasons, etc.)
 */
@Composable
fun SelectionButtonCompact(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Int = 48
) {
    SelectionButton(
        text = text,
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
        icon = null,
        showSelectedBadge = false,
        minHeight = minHeight
    )
}
