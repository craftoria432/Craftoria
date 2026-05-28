package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary

/**
 * Unified Dialog Component
 * Enforces design system standards:
 * - Border Radius: 12dp
 * - Elevation: 12dp
 * - Padding: 24dp
 * - Title: 18sp SemiBold
 * - Content: 14sp Normal
 * - Button Height: 48dp
 * - Button Gap: 12dp
 */

@Composable
fun CraftoriaDialog(
    title: String,
    content: @Composable () -> Unit,
    onDismiss: () -> Unit,
    primaryButton: DialogButton? = null,
    secondaryButton: DialogButton? = null,
    modifier: Modifier = Modifier,
    showCloseButton: Boolean = true,
    isScrollable: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = modifier
                .fillMaxWidth(0.9f)
                .background(BackgroundSecondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 24.sp,
                        modifier = Modifier.weight(1f)
                    )

                    if (showCloseButton) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                if (isScrollable) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        content()
                    }
                } else {
                    content()
                }

                // Buttons
                if (primaryButton != null || secondaryButton != null) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        primaryButton?.let {
                            CraftoriaButton(
                                text = it.text,
                                onClick = {
                                    it.onClick()
                                    onDismiss()
                                },
                                enabled = it.enabled,
                                isLoading = it.isLoading,
                                isPrimary = true
                            )
                        }

                        secondaryButton?.let {
                            CraftoriaButton(
                                text = it.text,
                                onClick = it.onClick,
                                enabled = it.enabled,
                                isLoading = it.isLoading,
                                isPrimary = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Confirmation Dialog
 * Simple yes/no dialog with title and message
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    isDestructive: Boolean = false
) {
    CraftoriaDialog(
        title = title,
        content = {
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        onDismiss = onCancel,
        primaryButton = DialogButton(
            text = confirmText,
            onClick = onConfirm,
            isPrimary = !isDestructive
        ),
        secondaryButton = DialogButton(
            text = cancelText,
            onClick = onCancel,
            isPrimary = isDestructive
        ),
        showCloseButton = true
    )
}

/**
 * Alert Dialog
 * Single action dialog with title and message
 */
@Composable
fun AlertDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = "OK"
) {
    CraftoriaDialog(
        title = title,
        content = {
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        onDismiss = onDismiss,
        primaryButton = DialogButton(
            text = buttonText,
            onClick = onDismiss
        ),
        showCloseButton = true
    )
}

/**
 * Loading Dialog
 * Shows loading indicator with message
 */
@Composable
fun LoadingDialog(
    message: String = "Loading...",
    onDismiss: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .background(BackgroundSecondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )

                Text(
                    text = message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Error Dialog
 * Shows error message with retry option
 */
@Composable
fun ErrorDialog(
    title: String = "Error",
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    CraftoriaDialog(
        title = title,
        content = {
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        onDismiss = onDismiss,
        primaryButton = if (onRetry != null) {
            DialogButton(
                text = "Retry",
                onClick = onRetry
            )
        } else {
            DialogButton(
                text = "OK",
                onClick = onDismiss
            )
        },
        secondaryButton = if (onRetry != null) {
            DialogButton(
                text = "Cancel",
                onClick = onDismiss,
                isPrimary = false
            )
        } else null,
        showCloseButton = true
    )
}

/**
 * Success Dialog
 * Shows success message with action
 */
@Composable
fun SuccessDialog(
    title: String = "Success",
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = "Continue"
) {
    CraftoriaDialog(
        title = title,
        content = {
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        onDismiss = onDismiss,
        primaryButton = DialogButton(
            text = buttonText,
            onClick = onDismiss
        ),
        showCloseButton = false
    )
}

/**
 * Dialog Button Configuration
 */
data class DialogButton(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isLoading: Boolean = false,
    val isPrimary: Boolean = true
)
