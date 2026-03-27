package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.services.MLKitFaceDetectionService
import com.gcuf.craftoria.services.FaceVerificationResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class SellerVerificationViewModel(context: Context) : ViewModel() {
    
    private val faceDetectionService = MLKitFaceDetectionService(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
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
     * Save ML Kit verification result to Firestore
     */
    suspend fun saveVerificationResultToFirestore(
        imageUrl: String,
        result: FaceVerificationResult
    ): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            val verificationData = mapOf(
                "userId" to userId,
                "imageUrl" to imageUrl,
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
            
            true
        } catch (e: Exception) {
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
