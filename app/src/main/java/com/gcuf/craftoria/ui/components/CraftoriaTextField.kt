package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary

@Composable
fun CraftoriaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    required: Boolean = false,
    placeholder: String = "",
    isPassword: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: String? = null,
    leadingIconVector: ImageVector? = null,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true, // ✅ NEW (important)
    minHeight: Int = 48 // ✅ NEW: Configurable height
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        // ✅ Only show label if needed
        if (showLabel && label.isNotEmpty()) {
            Row {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 18.sp // ✅ FIX: Explicit line height
                )
                if (required) {
                    Text(
                        text = " *",
                        fontSize = 14.sp,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp // ✅ FIX: Explicit line height
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp)) // 🔥 reduced from 8dp
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    lineHeight = 16.sp // ✅ FIX: Explicit line height
                )
            },
            enabled = enabled,
            leadingIcon = if (leadingIconVector != null) {
                {
                    Icon(
                        imageVector = leadingIconVector,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp) // slightly smaller
                    )
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible)
                PasswordVisualTransformation()
            else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = {
                if (isPassword) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                }
            },
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
                lineHeight = 18.sp // ✅ FIX: Explicit line height for input text
            ),

            // 🔥 KEY FIXES
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight.dp), // ✅ FIX: Use configurable height

            singleLine = true
        )
    }
}