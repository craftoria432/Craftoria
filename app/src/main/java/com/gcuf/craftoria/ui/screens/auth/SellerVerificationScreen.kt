package com.gcuf.craftoria.ui.screens.auth

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.data.model.SellerApplicationStatus
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.ui.components.FaceVerificationIndicator
import com.gcuf.craftoria.ui.components.VerificationProcessingIndicator
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.gcuf.craftoria.viewmodel.SellerVerificationViewModel
import com.gcuf.craftoria.viewmodel.VerificationState
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerVerificationScreen(
    verificationStatus: VerificationStatus = VerificationStatus.NOT_SUBMITTED,
    rejectionReason: String? = null,
    sellerName: String = "",
    sellerEmail: String = "",
    sellerPhone: String = "",
    onBackClick: () -> Unit,
    onNavigateToSellerDashboard: () -> Unit,
    onSubmitVerification: (Uri) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ML Kit Verification ViewModel
    val mlKitViewModel = remember {
        SellerVerificationViewModel(context)
    }
    val mlKitVerificationState by mlKitViewModel.verificationState.collectAsState()
    val mlKitVerificationResult by mlKitViewModel.verificationResult.collectAsState()
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var verificationState by remember { mutableStateOf(verificationStatus) }

    val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHelpSheet by remember { mutableStateOf(false) }

    val imageSourceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showImageSourceSheet by remember { mutableStateOf(false) }

    // ✅ REMOVED: listenToVerificationStatus() is deprecated - real-time listener is already active in AuthViewModel.observeAuthState()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { 
            cameraImageUri?.let { uri ->
                imageUri = uri
                // Automatically trigger ML Kit verification
                mlKitViewModel.verifySellerIdentity(uri)
            }
        }
    }
    
    // Handle verification submission with ML Kit results
    val handleSubmitVerification: (Uri) -> Unit = { uri ->
        scope.launch {
            mlKitVerificationResult?.let { result ->
                // Save ML Kit result to Firestore before submitting
                val saved = mlKitViewModel.saveVerificationResultToFirestore(
                    imageUri = uri,
                    result = result
                )
                if (saved) {
                    verificationState = VerificationStatus.PENDING
                    showImageSourceSheet = false
                    onSubmitVerification(uri)
                }
            } ?: run {
                // Fallback if no result (should not happen in success state)
                verificationState = VerificationStatus.PENDING
                showImageSourceSheet = false
                onSubmitVerification(uri)
            }
        }
    }

    val currentUser by viewModel.currentUser.collectAsState()
    LaunchedEffect(currentUser?.verificationStatus) {
        currentUser?.verificationStatus?.let { liveStatus ->
            if (liveStatus != verificationState) verificationState = liveStatus
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val tempFile = File.createTempFile("verification_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }
    
    val launchCamera: () -> Unit = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                val tempFile = File.createTempFile("verification_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSecondary)
    ) {
        // ── Top Bar ───────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Seller Verification",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Verify your identity to start selling",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 12.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.background(
                Brush.horizontalGradient(listOf(Primary, PrimaryLight))
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Check if user is still BUYER with PENDING seller application
            if (currentUser?.role == UserRole.BUYER &&
                currentUser?.sellerApplicationStatus == SellerApplicationStatus.PENDING
            ) {
                SellerApplicationPendingContent()
            } else {
                when (verificationState) {
                    VerificationStatus.NOT_SUBMITTED -> VerifNotSubmittedContent(
                        sellerName = sellerName,
                        sellerEmail = sellerEmail,
                        sellerPhone = sellerPhone,
                        imageUri = imageUri,
                        onStartVerification = { showImageSourceSheet = true },
                        onSubmit = {
                            imageUri?.let { uri ->
                                if (mlKitVerificationResult != null) {
                                    handleSubmitVerification(uri)
                                } else {
                                    verificationState = VerificationStatus.PENDING
                                    onSubmitVerification(uri)
                                }
                            }
                        },
                        onHelpClick = { showHelpSheet = true }
                    )
                    VerificationStatus.PENDING -> VerifPendingContent()
                    VerificationStatus.APPROVED -> VerifApprovedContent(
                        onContinue = onNavigateToSellerDashboard
                    )
                    VerificationStatus.REJECTED -> VerifRejectedContent(
                        reason = rejectionReason
                            ?: "Your verification was rejected. Please try again.",
                        onRetry = {
                            imageUri = null
                            mlKitViewModel.resetVerification()
                            verificationState = VerificationStatus.NOT_SUBMITTED
                        },
                        onContactSupport = { showHelpSheet = true }
                    )
                }
            }
        }
    }

    // ── Image Source Bottom Sheet ───────────────────────────────────────────
    if (showImageSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageSourceSheet = false },
            sheetState = imageSourceSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
            ) {
                if (imageUri != null && mlKitVerificationState != VerificationState.Idle) {
                    // Show ML Kit verification result
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (mlKitVerificationState) {
                            is VerificationState.Processing -> {
                                VerificationProcessingIndicator()
                            }
                            is VerificationState.Success -> {
                                mlKitVerificationResult?.let {
                                    FaceVerificationIndicator(it)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        imageUri?.let { uri ->
                                            handleSubmitVerification(uri)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Submit Verification")
                                }
                                Button(
                                    onClick = {
                                        imageUri = null
                                        mlKitViewModel.resetVerification()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Take Another Photo")
                                }
                            }
                            is VerificationState.Failed -> {
                                val error = (mlKitVerificationState as VerificationState.Failed).message
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    border = BorderStroke(0.5.dp, Color(0xFFF44336).copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = Color(0xFFF44336),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Verification Failed",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = error,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = {
                                        imageUri = null
                                        mlKitViewModel.resetVerification()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Try Again")
                                }
                            }
                            else -> {}
                        }
                    }
                } else {
                    // Show camera button
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 20.dp)
                    ) {
                        Button(
                            onClick = { launchCamera() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                            Text("Take Selfie for Verification", fontSize = 16.sp)
                        }
                    }
                }
                
                TextButton(
                    onClick = { showImageSourceSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }

    if (showHelpSheet) {
        VerifHelpBottomSheet(
            sheetState = helpSheetState,
            onDismiss = { showHelpSheet = false }
        )
    }
}

// ── Seller Application Pending ────────────────────────────────────────────────

@Composable
private fun SellerApplicationPendingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Spinner in tinted circle — consistent with VerifPendingContent
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Color(0xFF856404).copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = Color(0xFF856404),
                strokeWidth = 5.dp
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Seller Application Under Review",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Your seller application is being reviewed by our admin team. You'll be notified once approved to proceed with verification.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        // Review time card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(
                0.5.dp, Color(0xFF856404).copy(alpha = 0.20f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Estimated Review Time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Text(
                    "24 – 48 hours",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF856404)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "You'll receive a notification once your application is reviewed.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
        // Steps card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, BorderColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "WHAT HAPPENS NEXT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                VerifInfoStep("1", "Admin reviews your seller application")
                VerifInfoStep("2", "If approved, you can proceed with identity verification")
                VerifInfoStep("3", "Once verified, you can start selling immediately")
            }
        }
    }
}

// ── Help Sheet ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifHelpBottomSheet(sheetState: SheetState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Primary.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    "Help & Support",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "Having trouble with verification? We're here to help.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )
            Text(
                text = "Common Issues",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            VerifHelpItem("💡", "Photo too dark or blurry", "Find a well-lit area near a window. Avoid flash as it can wash out your face.")
            VerifHelpItem("👓", "Face not detected", "Remove glasses, hats, or face coverings. Make sure your full face is visible.")
            VerifHelpItem("📐", "Photo angle issue", "Hold your phone at eye level and look directly at the camera.")
            VerifHelpItem("⏳", "Still pending after 48 hours", "Our team may be experiencing high volume. Contact us directly below.")
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = BorderColor,
                thickness = 0.5.dp
            )
            Text(
                text = "Contact Us",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Card(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@craftoria.pk"))
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Seller Verification Help")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Hi Craftoria Support,\n\nI need help with my seller verification.\n\nIssue: ")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle case where no email app is found
                    }
                    onDismiss()
                },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                border = BorderStroke(0.5.dp, Color(0xFF90CAF9)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1976D2).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Email Support",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(text = "support@craftoria.pk", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = "Response within 24 hours",
                            fontSize = 11.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun VerifHelpItem(icon: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.padding(top = 1.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(body, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
        }
    }
}

// ── Not Submitted ─────────────────────────────────────────────────────────────

@Composable
private fun VerifNotSubmittedContent(
    sellerName: String,
    sellerEmail: String,
    sellerPhone: String,
    imageUri: Uri?,
    onStartVerification: () -> Unit,
    onSubmit: () -> Unit,
    onHelpClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Instructions card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, BorderColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera icon in tinted circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Primary.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Identity Verification",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "We need to verify your identity to ensure a trusted marketplace",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
                VerifCheckItem("Take a live selfie using your front camera")
                VerifCheckItem("Ensure good lighting and clear visibility")
                VerifCheckItem("Face the camera directly and remove glasses if possible")
                VerifCheckItem("This is a one-time verification process")
            }
        }

        // Seller info card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, BorderColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "YOUR DETAILS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                VerifInfoRow(label = "Name", value = sellerName.ifEmpty { "Sarah Ahmed" })
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                VerifInfoRow(label = "Email", value = sellerEmail.ifEmpty { "sarah@example.com" })
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                VerifInfoRow(label = "Phone", value = sellerPhone.ifEmpty { "+92 300 1234567" })
            }
        }

        // Photo selected confirmation banner
        if (imageUri != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                border = BorderStroke(
                    0.5.dp, Success.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Photo selected! Tap Submit to proceed.",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D2E)
                    )
                }
            }
        }

        // Gradient CTA button
        Button(
            onClick = { if (imageUri == null) onStartVerification() else onSubmit() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (imageUri == null) Icons.Outlined.CameraAlt
                        else Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (imageUri == null) "Start Camera Verification"
                        else "Submit Verification",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // Help link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHelpClick() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Need help with verification?",
                fontSize = 12.sp,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun VerifCheckItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Success.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VerifInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ── Pending ───────────────────────────────────────────────────────────────────

@Composable
private fun VerifPendingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Spinner in tinted circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = Primary,
                strokeWidth = 5.dp
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Verification in Progress",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Your request has been submitted. Our admin team is reviewing your profile.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        // Review time card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(
                0.5.dp, Primary.copy(alpha = 0.20f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Estimated Review Time",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Text(
                    "24 – 48 hours",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "You'll receive a notification once your verification is complete.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }
        // Steps card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, BorderColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "WHAT HAPPENS NEXT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                VerifInfoStep("1", "Our admin team reviews your submission")
                VerifInfoStep("2", "If approved, you can start selling immediately")
                VerifInfoStep("3", "If rejected, you can retry with a new photo")
            }
        }
    }
}

@Composable
private fun VerifInfoStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Primary.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
        Text(text, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
    }
}

// ── Approved ──────────────────────────────────────────────────────────────────

@Composable
private fun VerifApprovedContent(onContinue: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Success icon in tinted circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Success.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(50.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Verification Approved!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Success,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Congratulations! You're now a verified seller on Craftoria. Start adding your products and reach customers.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        // Next steps card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(
                0.5.dp, Success.copy(alpha = 0.30f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "NEXT STEPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                VerifNextStep("Add your first product")
                VerifNextStep("Set up your seller profile")
                VerifNextStep("Explore co-seller stores")
                VerifNextStep("Learn from our resources")
            }
        }
        // Gradient CTA button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Continue to Dashboard",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun VerifNextStep(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Success.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(text, fontSize = 13.sp, color = TextSecondary)
    }
}

// ── Rejected ──────────────────────────────────────────────────────────────────

@Composable
private fun VerifRejectedContent(
    reason: String,
    onRetry: () -> Unit,
    onContactSupport: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Error icon in tinted circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Error.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(48.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Verification Rejected",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Unfortunately, your verification was not successful. Please review the reason below and try again.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        // Rejection reason card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(
                0.5.dp, Error.copy(alpha = 0.30f)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "REJECTION REASON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(reason, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
            }
        }
        // Tips card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.5.dp, BorderColor),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "TIPS FOR SUCCESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
                listOf(
                    "Ensure good lighting",
                    "Face should be clearly visible",
                    "Remove any obstructions",
                    "Use a recent photo"
                ).forEach {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Primary, CircleShape)
                        )
                        Text(it, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
        }
        // Retry gradient button
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Retry Verification",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        TextButton(
            onClick = onContactSupport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Need help? Contact Support",
                fontSize = 13.sp,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, name = "Verification – Not Submitted")
@Composable
fun PreviewVerificationNotSubmitted() {
    CraftoriaTheme {
        SellerVerificationScreen(
            verificationStatus = VerificationStatus.NOT_SUBMITTED,
            sellerName = "Sarah Ahmed", sellerEmail = "sarah@example.com",
            sellerPhone = "+92 300 1234567",
            onBackClick = {}, onNavigateToSellerDashboard = {}, onSubmitVerification = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Verification – Pending")
@Composable
fun PreviewVerificationPending() {
    CraftoriaTheme {
        SellerVerificationScreen(
            verificationStatus = VerificationStatus.PENDING,
            sellerName = "Sarah Ahmed", sellerEmail = "sarah@example.com",
            sellerPhone = "+92 300 1234567",
            onBackClick = {}, onNavigateToSellerDashboard = {}, onSubmitVerification = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Verification – Approved")
@Composable
fun PreviewVerificationApproved() {
    CraftoriaTheme {
        SellerVerificationScreen(
            verificationStatus = VerificationStatus.APPROVED,
            sellerName = "Sarah Ahmed", sellerEmail = "sarah@example.com",
            sellerPhone = "+92 300 1234567",
            onBackClick = {}, onNavigateToSellerDashboard = {}, onSubmitVerification = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Verification – Rejected")
@Composable
fun PreviewVerificationRejected() {
    CraftoriaTheme {
        SellerVerificationScreen(
            verificationStatus = VerificationStatus.REJECTED,
            rejectionReason = "Photo was blurry. Please retake in good lighting with your full face visible.",
            sellerName = "Sarah Ahmed", sellerEmail = "sarah@example.com",
            sellerPhone = "+92 300 1234567",
            onBackClick = {}, onNavigateToSellerDashboard = {}, onSubmitVerification = {}
        )
    }
}
