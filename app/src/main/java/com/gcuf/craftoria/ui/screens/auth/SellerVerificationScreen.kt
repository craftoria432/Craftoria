package com.gcuf.craftoria.ui.screens.auth

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.ui.components.CraftoriaButton
import com.gcuf.craftoria.ui.components.CraftoriaTopBar
import com.gcuf.craftoria.ui.theme.*

@Composable
fun SellerVerificationScreen(
    verificationStatus: VerificationStatus = VerificationStatus.NOT_SUBMITTED,
    rejectionReason: String? = null,
    onBackClick: () -> Unit,
    onSubmitVerification: (Uri) -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var verificationState by remember { mutableStateOf(verificationStatus) }
    var showCamera by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(false) }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            showCamera = true
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CraftoriaTopBar(
            screenNumber = "02",
            title = "Seller Verification",
            showBack = true,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            when (verificationState) {
                VerificationStatus.NOT_SUBMITTED -> {
                    NotSubmittedContent(
                        onStartVerification = {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) -> {
                                    imagePickerLauncher.launch("image/*")
                                }
                                else -> {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        imageUri = imageUri,
                        onImageSelected = { imageUri = it },
                        onSubmit = {
                            imageUri?.let { uri ->
                                verificationState = VerificationStatus.PENDING
                                onSubmitVerification(uri)
                            }
                        }
                    )
                }

                VerificationStatus.PENDING -> {
                    PendingContent()
                }

                VerificationStatus.APPROVED -> {
                    ApprovedContent(onContinue = onBackClick)
                }

                VerificationStatus.REJECTED -> {
                    RejectedContent(
                        reason = rejectionReason ?: "Your verification was rejected. Please try again.",
                        onRetry = {
                            verificationState = VerificationStatus.NOT_SUBMITTED
                            imageUri = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotSubmittedContent(
    onStartVerification: () -> Unit,
    imageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon
        Surface(
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.large,
            color = Primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = Primary,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Face Verification Required",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "To ensure authenticity and build trust, we require all sellers to complete face verification using Google ML Kit.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Information Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📋 Requirements:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                VerificationRequirement("✓ Clear, well-lit photo of your face")
                VerificationRequirement("✓ Remove glasses or accessories if possible")
                VerificationRequirement("✓ Look directly at the camera")
                VerificationRequirement("✓ Ensure your full face is visible")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (imageUri != null) {
            MessageCard(
                message = "✅ Image selected! Click Submit to proceed.",
                type = MessageType.SUCCESS
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        CraftoriaButton(
            text = if (imageUri == null) "📷 Upload Face Photo" else "✅ Submit Verification",
            onClick = {
                if (imageUri == null) {
                    onStartVerification()
                } else {
                    onSubmit()
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your photo will be reviewed by our admin team within 24-48 hours.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun VerificationRequirement(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun PendingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Loading Animation
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = Primary,
            strokeWidth = 6.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verification in Progress",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your verification request has been submitted successfully. Our admin team is reviewing your profile.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏱️ Estimated Review Time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "24-48 hours",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Info
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You'll receive a notification once your verification is complete.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "💡 What happens next?",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        InfoStep("1", "Our admin team reviews your submission")
        InfoStep("2", "If approved, you can start selling immediately")
        InfoStep("3", "If rejected, you can retry with a new photo")
    }
}

@Composable
fun InfoStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.large,
            color = Primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
fun ApprovedContent(onContinue: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Approved",
            tint = Success,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verification Approved! 🎉",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Success,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Congratulations! You're now a verified seller on Craftoria. Start adding your products and reach customers.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎁 Next Steps:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                NextStep("Add your first product")
                NextStep("Set up your seller profile")
                NextStep("Explore co-seller stores")
                NextStep("Learn from our resources")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CraftoriaButton(
            text = "Continue to Dashboard",
            onClick = onContinue
        )
    }
}

@Composable
fun NextStep(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "✓",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Success,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun RejectedContent(
    reason: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Error Icon
        Surface(
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.large,
            color = Error.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "❌",
                    fontSize = 50.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verification Rejected",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Unfortunately, your verification was not successful. Please review the reason below and try again.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rejection Reason Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Rejection Reason:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = reason,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💡 Tips for Success:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                VerificationRequirement("• Ensure good lighting")
                VerificationRequirement("• Face should be clearly visible")
                VerificationRequirement("• Remove any obstructions")
                VerificationRequirement("• Use a recent photo")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CraftoriaButton(
            text = "🔄 Retry Verification",
            onClick = onRetry
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { /* TODO: Contact Support */ }) {
            Text(
                text = "Need help? Contact Support",
                fontSize = 13.sp,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}