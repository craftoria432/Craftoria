package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary

/**
 * Standardized OutlinedTextField wrapper that matches CraftoriaTextField specifications.
 * 
 * Standardized specifications:
 * - Height: 48.dp minimum (or custom via minHeight)
 * - Border radius: 10.dp
 * - Padding: 12.dp (internal)
 * - Border: 0.5.dp (unselected) / 1.5.dp (selected)
 * - Font Size: 14.sp
 * - Font Weight: Medium (unselected) / SemiBold (selected)
 * 
 * Used for:
 * - Multi-line text fields (descriptions, notes, etc.)
 * - Search fields
 * - Any OutlinedTextField that needs standardization
 */
@Composable
fun StandardizedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    placeholder: String = "",
    minHeight: Int = 48,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Show label if needed
        if (showLabel && label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    color = TextSecondary
                )
            },
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderColor,
                disabledBorderColor = BorderColor,
                disabledTextColor = Color.Gray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color(0xFFF8F9FA)
            ),
            shape = RoundedCornerShape(10.dp),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.dp),
            minLines = minLines,
            maxLines = maxLines,
            singleLine = singleLine
        )
    }
}

/**
 * Compact variant without label (for inline usage)
 */
@Composable
fun StandardizedOutlinedTextFieldCompact(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minHeight: Int = 48,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = TextSecondary
            )
        },
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = BorderColor,
            disabledBorderColor = BorderColor,
            disabledTextColor = Color.Gray,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(10.dp),
        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp),
        minLines = minLines,
        maxLines = maxLines,
        singleLine = singleLine
    )
}
