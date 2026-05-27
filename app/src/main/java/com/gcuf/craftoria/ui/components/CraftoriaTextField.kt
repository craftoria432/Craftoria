package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                .heightIn(min = minHeight.dp), // ✅ FIX: Use heightIn for minimum constraint, not fixed height

            singleLine = true
        )
    }
}


// ✅ PROFESSIONAL SUCCESS MESSAGE
@Composable
fun SuccessAlert(
    message: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.CheckCircle
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0FDF4), // Softer, more professional green background
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Color(0xFF86EFAC) // Vibrant but professional green border
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon container with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF16A34A), // Professional green
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF166534), // Darker green for better readability
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ✅ PROFESSIONAL INFO MESSAGE
@Composable
fun InfoAlert(
    message: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0F9FF), // Professional light blue background
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Color(0xFF93C5FD) // Vibrant blue border
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon container with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFDEEBF7), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF1E40AF), // Professional blue
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E3A8A), // Darker blue for readability
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF1E40AF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ✅ PROFESSIONAL WARNING MESSAGE
@Composable
fun WarningAlert(
    message: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEF3C7), // Professional warm amber background
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Color(0xFFFCD34D) // Vibrant amber border
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon container with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFEF08A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFB45309), // Professional amber/orange
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF92400E), // Darker amber for readability
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ✅ PROFESSIONAL ERROR MESSAGE
@Composable
fun ErrorAlert(
    message: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEE2E2), // Professional light red background
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Color(0xFFFCA5A5) // Vibrant red border
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon container with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFECACA), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFDC2626), // Professional red
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF7F1D1D), // Darker red for readability
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
