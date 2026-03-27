package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary
import com.gcuf.craftoria.ui.theme.ThemeType

@Composable
fun ThemePreferenceSelector(
    selectedTheme: ThemeType,
    onThemeSelected: (ThemeType) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose Your Theme",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        // Three-column tile grid — matches SettingsScreen.ThemeTile pattern
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemePreviewTile(
                primaryColor  = Color(0xFFE91E63),
                primaryLight  = Color(0xFFF06292),
                label         = "Rose",
                description   = "Pink theme",
                isSelected    = selectedTheme == ThemeType.ROSE,
                isLoading     = isLoading,
                modifier      = Modifier.weight(1f),
                onClick       = { onThemeSelected(ThemeType.ROSE) }
            )
            ThemePreviewTile(
                primaryColor  = Color(0xFF0277BD),
                primaryLight  = Color(0xFF29B6F6),
                label         = "Ocean",
                description   = "Blue theme",
                isSelected    = selectedTheme == ThemeType.OCEAN,
                isLoading     = isLoading,
                modifier      = Modifier.weight(1f),
                onClick       = { onThemeSelected(ThemeType.OCEAN) }
            )
        }
    }
}

@Composable
private fun ThemePreviewTile(
    primaryColor: Color,
    primaryLight: Color,
    label: String,
    description: String,
    isSelected: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (!isLoading) onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) primaryColor.copy(alpha = 0.05f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) primaryColor else BorderColor
        ),
        modifier = modifier
    ) {
        Column {
            // Gradient swatch band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Brush.linearGradient(listOf(primaryColor, primaryLight)))
            ) {
                if (isSelected && !isLoading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(17.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = primaryColor,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                if (isLoading && isSelected) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(13.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) primaryColor else TextPrimary
                )
                Text(text = description, fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}