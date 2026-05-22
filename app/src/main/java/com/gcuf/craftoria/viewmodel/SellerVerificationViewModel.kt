package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.services.MLKitFaceDetectionService
import com.gcuf.craftoria.services.FaceVerificationResult
import com.gcuf.craftoria.utils.CloudinaryManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class SellerVerificationViewModel(private val context: Context) : ViewModel() {
    
    private val faceDetectionService = MLKitFaceDetectionService(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState
    
    private val _verificationResult = MutableStateFlow<FaceVerificationResult?>(null)
    val verificationResult: StateFlow<FaceVerificationResult?> = _verificationResult
    
    companion object {
        private const val TAG = "SellerVerificationVM"
    }
    
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
     * ✅ PRODUCTION-READY: Save ML Kit verification result to Firestore with Cloudinary upload
     * This uploads the verification photo to Cloudinary so admin can view it from web dashboard
     */
    suspend fun saveVerificationResultToFirestore(
        imageUri: Uri,
        result: FaceVerificationResult
    ): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📤 Uploading verification photo to Cloudinary...")
            
            // ✅ CRITICAL FIX: Upload image to Cloudinary instead of storing local URI
            val cloudinaryUrl = CloudinaryManager.uploadImage(
                context = context,
                imageUri = imageUri,
                folder = "craftoria/verifications"  // Separate folder for verification photos
            )
            
            Log.d(TAG, "✅ Verification photo uploaded successfully")
            Log.d(TAG, "🔗 Cloudinary URL: $cloudinaryUrl")
            
            val verificationData = mapOf(
                "userId" to userId,
                "imageUrl" to cloudinaryUrl,  // ✅ Now stores Cloudinary URL (accessible from web)
                "verificationStatus" to "pending",
                "timestamp" to Timestamp.now(),
                "mlKitResult" to mapOf(
                    "isValid" to result.isValid,
                    "confidence" to result.confidence,
                    "faceCount" to result.faceCount,
                    "message" to result.message
                ),
                "submittedAt" to Timestamp.now(),
                "submittedBy" to (auth.currentUser?.email ?: "unknown")
            )
            
            firestore.collection("seller_verifications")
                .document(userId)
                .set(verificationData)
                .await()
            
            Log.d(TAG, "✅ Verification data saved to Firestore")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save verification result", e)
            false
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
