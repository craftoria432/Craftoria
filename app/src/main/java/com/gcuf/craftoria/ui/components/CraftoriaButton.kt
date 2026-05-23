package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextLight

/**
 * Unified Craftoria Button Component
 * Enforces design system standards:
 * - Height: 48dp (standard), 36dp (small)
 * - Border Radius: 12dp
 * - Font: 15sp SemiBold (standard), 13sp Medium (small)
 * - Icon: 18dp
 * - Padding: 16dp horizontal, 12dp vertical
 */
@Composable
fun CraftoriaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isPrimary: Boolean = true,
    icon: ImageVector? = null,
    isSmall: Boolean = false
) {
    val buttonHeight = if (isSmall) 36.dp else 48.dp
    val fontSize = if (isSmall) 13.sp else 15.sp
    val fontWeight = if (isSmall) FontWeight.Medium else FontWeight.SemiBold
    val iconSize = if (isSmall) 16.dp else 18.dp
    val spacerWidth = if (isSmall) 6.dp else 8.dp

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        colors = if (isPrimary) {
            ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFBDBDBD),
                disabledContentColor = Color.White
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Primary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = TextLight
            )
        },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
        border = if (!isPrimary) BorderStroke(1.5.dp, if (enabled) Primary else BorderColor) else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (isPrimary) Color.White else Primary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(spacerWidth))
                }
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    lineHeight = fontSize * 1.2f
                )
            }
        }
    }
}