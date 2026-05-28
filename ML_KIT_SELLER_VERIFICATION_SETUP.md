# ML Kit Free Seller Verification Setup Guide

## Overview
This guide shows how to integrate Google ML Kit for free face detection and identity verification in your Craftoria seller verification system. ML Kit is completely free and runs on-device.

---

## Step 1: Add ML Kit Dependencies

Update your `app/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // ML Kit Face Detection (FREE - on-device)
    implementation("com.google.mlkit:face-detection:16.1.5")
    
    // ML Kit Pose Detection (optional - for liveness detection)
    implementation("com.google.mlkit:pose-detection:18.0.0-beta3")
}
```

**Why these are free:**
- All processing happens on-device
- No API calls to Google servers
- No quotas or billing required
- Works offline

---

## Step 2: Create ML Kit Face Detection Service

Create `app/src/main/java/com/gcuf/craftoria/services/MLKitFaceDetectionService.kt`:

```kotlin
package com.gcuf.craftoria.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import android.content.Context

class MLKitFaceDetectionService(private val context: Context) {
    
    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }
    
    /**
     * Detect faces in an image URI
     * Returns verification result with confidence score
     */
    suspend fun detectFaceInImage(imageUri: Uri): FaceVerificationResult {
        return try {
            val bitmap = loadBitmapFromUri(imageUri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            val faces = detector.process(inputImage).await()
            
            when {
                faces.isEmpty() -> FaceVerificationResult(
                    isValid = false,
                    confidence = 0f,
                    message = "No face detected. Please ensure your face is clearly visible.",
                    faceCount = 0
                )
                faces.size > 1 -> FaceVerificationResult(
                    isValid = false,
                    confidence = 0f,
                    message = "Multiple faces detected. Please take a selfie with only your face.",
                    faceCount = faces.size
                )
                else -> {
                    val face = faces[0]
                    val isValidFace = validateFaceQuality(face)
                    FaceVerificationResult(
                        isValid = isValidFace,
                        confidence = calculateConfidence(face),
                        message = if (isValidFace) 
                            "Face verified successfully!" 
                        else 
                            "Face quality is poor. Please retake the photo.",
                        faceCount = 1,
                        face = face
                    )
                }
            }
        } catch (e: Exception) {
            FaceVerificationResult(
                isValid = false,
                confidence = 0f,
                message = "Error processing image: ${e.message}",
                faceCount = 0
            )
        }
    }
    
    /**
     * Validate face quality based on multiple factors
     */
    private fun validateFaceQuality(face: Face): Boolean {
        // Check if face is large enough (at least 100x100 pixels)
        val faceBounds = face.boundingBox
        val faceWidth = faceBounds.width()
        val faceHeight = faceBounds.height()
        
        if (faceWidth < 100 || faceHeight < 100) {
            return false
        }
        
        // Check head rotation (should be relatively straight)
        val headEulerAngleY = face.headEulerAngleY // Left-right rotation
        val headEulerAngleZ = face.headEulerAngleZ // Tilt
        
        if (Math.abs(headEulerAngleY) > 30 || Math.abs(headEulerAngleZ) > 30) {
            return false
        }
        
        // Check if eyes are open (if classification is available)
        val leftEyeOpen = face.leftEyeOpenProbability
        val rightEyeOpen = face.rightEyeOpenProbability
        
        if (leftEyeOpen != null && rightEyeOpen != null) {
            if (leftEyeOpen < 0.5f || rightEyeOpen < 0.5f) {
                return false
            }
        }
        
        // Check if face is smiling (optional - can be removed)
        val smilingProbability = face.smilingProbability
        // We don't enforce smiling, just detect it
        
        return true
    }
    
    /**
     * Calculate confidence score (0-100)
     */
    private fun calculateConfidence(face: Face): Float {
        var confidence = 100f
        
        // Reduce confidence based on head rotation
        val headEulerAngleY = Math.abs(face.headEulerAngleY)
        val headEulerAngleZ = Math.abs(face.headEulerAngleZ)
        confidence -= (headEulerAngleY + headEulerAngleZ) / 2
        
        // Reduce confidence if eyes are not fully open
        val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 1f
        confidence -= (1f - (leftEyeOpen + rightEyeOpen) / 2) * 20
        
        return confidence.coerceIn(0f, 100f)
    }
    
    /**
     * Load bitmap from URI
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }
    
    /**
     * Release detector resources
     */
    fun close() {
        detector.close()
    }
}

/**
 * Result of face verification
 */
data class FaceVerificationResult(
    val isValid: Boolean,
    val confidence: Float, // 0-100
    val message: String,
    val faceCount: Int,
    val face: Face? = null
)
```

---

## Step 3: Create Verification ViewModel

Create `app/src/main/java/com/gcuf/craftoria/viewmodel/SellerVerificationViewModel.kt`:

```kotlin
package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.services.MLKitFaceDetectionService
import com.gcuf.craftoria.services.FaceVerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SellerVerificationViewModel(context: Context) : ViewModel() {
    
    private val faceDetectionService = MLKitFaceDetectionService(context)
    
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState
    
    private val _verificationResult = MutableStateFlow<FaceVerificationResult?>(null)
    val verificationResult: StateFlow<FaceVerificationResult?> = _verificationResult
    
    /**
     * Verify seller identity using ML Kit face detection
     */
    fun verifySellerIdentity(imageUri: Uri) {
        viewModelScope.launch {
            _verificationState.value = VerificationState.Processing
            
            try {
                val result = faceDetectionService.detectFaceInImage(imageUri)
                _verificationResult.value = result
                
                _verificationState.value = if (result.isValid) {
                    VerificationState.Success(result.confidence)
                } else {
                    VerificationState.Failed(result.message)
                }
            } catch (e: Exception) {
                _verificationState.value = VerificationState.Failed(
                    "Verification failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reset verification state
     */
    fun resetVerification() {
        _verificationState.value = VerificationState.Idle
        _verificationResult.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        faceDetectionService.close()
    }
}

sealed class VerificationState {
    object Idle : VerificationState()
    object Processing : VerificationState()
    data class Success(val confidence: Float) : VerificationState()
    data class Failed(val message: String) : VerificationState()
}
```

---

## Step 4: Update AndroidManifest.xml

Add ML Kit model download declaration:

```xml
<application
    ...>
    
    <!-- ML Kit Face Detection Model -->
    <meta-data
        android:name="com.google.firebase.ml.vision.DEPENDENCIES"
        android:value="face" />
    
    <!-- Other meta-data and activities -->
    ...
</application>

<!-- Camera permission for selfie capture -->
<uses-permission android:name="android.permission.CAMERA" />
```

---

## Step 5: Integrate into SellerVerificationScreen

Update your `SellerVerificationScreen.kt` to use ML Kit:

```kotlin
// Add to imports
import com.gcuf.craftoria.viewmodel.SellerVerificationViewModel
import com.gcuf.craftoria.viewmodel.VerificationState
import androidx.lifecycle.viewmodel.compose.viewModel

// In the composable function
@Composable
fun SellerVerificationScreen(
    // ... existing parameters ...
) {
    val context = LocalContext.current
    val verificationViewModel = remember {
        SellerVerificationViewModel(context)
    }
    
    val verificationState by verificationViewModel.verificationState.collectAsState()
    val verificationResult by verificationViewModel.verificationResult.collectAsState()
    
    // ... rest of your code ...
    
    // When user submits photo
    val onSubmit: () -> Unit = {
        imageUri?.let { uri ->
            verificationViewModel.verifySellerIdentity(uri)
        }
    }
    
    // Handle verification states
    LaunchedEffect(verificationState) {
        when (verificationState) {
            is VerificationState.Success -> {
                // Show success message
                // Upload to Firebase
                onSubmitVerification(imageUri!!)
            }
            is VerificationState.Failed -> {
                // Show error message
                val errorMsg = (verificationState as VerificationState.Failed).message
                // Show toast or snackbar
            }
            else -> {}
        }
    }
}
```

---

## Step 6: Add Verification UI Components

Create `app/src/main/java/com/gcuf/craftoria/ui/components/FaceVerificationIndicator.kt`:

```kotlin
package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.services.FaceVerificationResult
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary

@Composable
fun FaceVerificationIndicator(result: FaceVerificationResult) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (result.isValid) 
                Color(0xFFE8F5E9) 
            else 
                Color(0xFFFFEBEE)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (result.isValid) 
                Color(0xFF4CAF50).copy(alpha = 0.3f)
            else
                Color(0xFFF44336).copy(alpha = 0.3f)
        ),
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (result.isValid) 
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else
                            Color(0xFFF44336).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (result.isValid) 
                        Icons.Default.CheckCircle 
                    else 
                        Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result.isValid) 
                        Color(0xFF4CAF50) 
                    else 
                        Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (result.isValid) "Face Verified" else "Verification Failed",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = result.message,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                if (result.isValid) {
                    Text(
                        text = "Confidence: ${result.confidence.toInt()}%",
                        fontSize = 11.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.paddingTop(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VerificationProcessingIndicator() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
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
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = Primary
            )
            Column {
                Text(
                    text = "Verifying your face...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "This may take a few seconds",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
```

---

## Step 7: Firebase Integration (Optional)

Store verification results in Firebase:

```kotlin
// In your repository
suspend fun submitSellerVerification(
    userId: String,
    imageUrl: String,
    verificationResult: FaceVerificationResult
) {
    val verificationData = mapOf(
        "userId" to userId,
        "imageUrl" to imageUrl,
        "isValid" to verificationResult.isValid,
        "confidence" to verificationResult.confidence,
        "faceCount" to verificationResult.faceCount,
        "timestamp" to System.currentTimeMillis(),
        "status" to if (verificationResult.isValid) "APPROVED" else "REJECTED"
    )
    
    db.collection("seller_verifications")
        .document(userId)
        .set(verificationData)
        .await()
}
```

---

## Step 8: Firestore Security Rules

Add to your `firestore.rules`:

```firestore
match /seller_verifications/{userId} {
  allow read: if request.auth.uid == userId;
  allow write: if request.auth.uid == userId && 
               request.resource.data.timestamp == request.time.toMillis();
}
```

---

## Testing Checklist

- [ ] ML Kit dependencies added to build.gradle.kts
- [ ] AndroidManifest.xml updated with camera permission
- [ ] Face detection service created and tested
- [ ] ViewModel handles verification states
- [ ] UI components display verification results
- [ ] Firebase integration working
- [ ] Test with various face angles and lighting
- [ ] Test with multiple faces (should reject)
- [ ] Test with no face detected (should reject)
- [ ] Test with poor image quality (should reject)

---

## Cost Analysis

**ML Kit Face Detection: FREE**
- ✅ On-device processing
- ✅ No API calls
- ✅ No quotas
- ✅ Works offline
- ✅ No billing required

**Total Cost: $0/month**

---

## Troubleshooting

### Face not detected
- Ensure good lighting
- Face should be at least 100x100 pixels
- Remove glasses/hats
- Face should be directly facing camera

### Multiple faces detected
- Only one face should be in the frame
- Ensure no one else is in the background

### Poor image quality
- Increase lighting
- Reduce motion blur
- Ensure clear focus on face

### Model download issues
- First run may take time to download model
- Ensure internet connection
- Check device storage space

---

## Next Steps

1. Integrate liveness detection (optional)
2. Add document scanning for ID verification
3. Implement admin dashboard for verification review
4. Add email notifications for verification status
5. Create appeal process for rejected verifications
