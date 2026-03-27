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
