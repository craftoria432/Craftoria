# ML Kit + Firebase Integration Guide

## Overview
This guide shows how to integrate ML Kit face verification results with Firebase for seller verification workflow.

---

## Step 1: Update Firestore Schema

Add to your Firestore database structure:

```
users/{userId}
├── role: "SELLER" | "BUYER"
├── verificationStatus: "NOT_SUBMITTED" | "PENDING" | "APPROVED" | "REJECTED"
├── verificationDetails: {
│   ├── imageUrl: "gs://..."
│   ├── confidence: 85.5
│   ├── faceCount: 1
│   ├── timestamp: 1234567890
│   ├── mlKitResult: {
│   │   ├── isValid: true
│   │   ├── headEulerAngleY: 5.2
│   │   ├── headEulerAngleZ: 2.1
│   │   ├── leftEyeOpenProbability: 0.95
│   │   └── rightEyeOpenProbability: 0.92
│   └── adminNotes: "Approved by admin"
└── verificationHistory: [...]
```

---

## Step 2: Create Verification Repository

Create `app/src/main/java/com/gcuf/craftoria/data/repository/SellerVerificationRepository.kt`:

```kotlin
package com.gcuf.craftoria.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.gcuf.craftoria.services.FaceVerificationResult
import kotlinx.coroutines.tasks.await
import java.util.*

class SellerVerificationRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    
    /**
     * Submit seller verification with ML Kit results
     */
    suspend fun submitSellerVerification(
        imageUri: Uri,
        verificationResult: FaceVerificationResult
    ): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            // Upload image to Firebase Storage
            val imageUrl = uploadVerificationImage(userId, imageUri)
            
            // Create verification document
            val verificationData = mapOf(
                "userId" to userId,
                "imageUrl" to imageUrl,
                "verificationStatus" to "PENDING",
                "mlKitResult" to mapOf(
                    "isValid" to verificationResult.isValid,
                    "confidence" to verificationResult.confidence,
                    "faceCount" to verificationResult.faceCount,
                    "message" to verificationResult.message
                ),
                "timestamp" to System.currentTimeMillis(),
                "adminReviewed" to false,
                "adminNotes" to ""
            )
            
            // Save to Firestore
            db.collection("seller_verifications")
                .document(userId)
                .set(verificationData)
                .await()
            
            // Update user verification status
            db.collection("users")
                .document(userId)
                .update(
                    "verificationStatus" to "PENDING",
                    "verificationDetails" to verificationData
                )
                .await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Upload verification image to Firebase Storage
     */
    private suspend fun uploadVerificationImage(
        userId: String,
        imageUri: Uri
    ): String {
        val fileName = "seller_verification_${userId}_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference
            .child("seller_verifications")
            .child(userId)
            .child(fileName)
        
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }
    
    /**
     * Get verification status for current user
     */
    suspend fun getVerificationStatus(): String? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val doc = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            doc.getString("verificationStatus")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get verification details for current user
     */
    suspend fun getVerificationDetails(): Map<String, Any>? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val doc = db.collection("seller_verifications")
                .document(userId)
                .get()
                .await()
            
            doc.data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Listen to verification status changes
     */
    fun listenToVerificationStatus(
        userId: String,
        onStatusChanged: (String) -> Unit
    ) {
        db.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                
                snapshot?.getString("verificationStatus")?.let {
                    onStatusChanged(it)
                }
            }
    }
    
    /**
     * Retry verification (for rejected cases)
     */
    suspend fun retryVerification(
        imageUri: Uri,
        verificationResult: FaceVerificationResult
    ): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            // Upload new image
            val imageUrl = uploadVerificationImage(userId, imageUri)
            
            // Update verification document
            val verificationData = mapOf(
                "userId" to userId,
                "imageUrl" to imageUrl,
                "verificationStatus" to "PENDING",
                "mlKitResult" to mapOf(
                    "isValid" to verificationResult.isValid,
                    "confidence" to verificationResult.confidence,
                    "faceCount" to verificationResult.faceCount,
                    "message" to verificationResult.message
                ),
                "timestamp" to System.currentTimeMillis(),
                "adminReviewed" to false,
                "adminNotes" to "",
                "retryCount" to (getRetryCount(userId) + 1)
            )
            
            db.collection("seller_verifications")
                .document(userId)
                .set(verificationData)
                .await()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get retry count for user
     */
    private suspend fun getRetryCount(userId: String): Int {
        return try {
            val doc = db.collection("seller_verifications")
                .document(userId)
                .get()
                .await()
            
            doc.getLong("retryCount")?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
```

---

## Step 3: Update ViewModel with Firebase

Update `SellerVerificationViewModel.kt`:

```kotlin
package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.services.MLKitFaceDetectionService
import com.gcuf.craftoria.services.FaceVerificationResult
import com.gcuf.craftoria.data.repository.SellerVerificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SellerVerificationViewModel(
    context: Context,
    private val verificationRepository: SellerVerificationRepository
) : ViewModel() {
    
    private val faceDetectionService = MLKitFaceDetectionService(context)
    
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState
    
    private val _verificationResult = MutableStateFlow<FaceVerificationResult?>(null)
    val verificationResult: StateFlow<FaceVerificationResult?> = _verificationResult
    
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState
    
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
     * Submit verification to Firebase
     */
    fun submitVerificationToFirebase(imageUri: Uri) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading
            
            try {
                val result = _verificationResult.value
                    ?: throw Exception("No verification result available")
                
                val success = verificationRepository.submitSellerVerification(
                    imageUri,
                    result
                )
                
                _uploadState.value = if (success) {
                    UploadState.Success
                } else {
                    UploadState.Failed("Failed to submit verification")
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Failed(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset verification state
     */
    fun resetVerification() {
        _verificationState.value = VerificationState.Idle
        _verificationResult.value = null
        _uploadState.value = UploadState.Idle
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

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Failed(val message: String) : UploadState()
}
```

---

## Step 4: Firestore Security Rules

Update your `firestore.rules`:

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Seller Verifications - Only user can read/write their own
    match /seller_verifications/{userId} {
      allow read: if request.auth.uid == userId;
      allow write: if request.auth.uid == userId && 
                      request.resource.data.timestamp == request.time.toMillis();
      
      // Admin can read all for review
      allow read: if isAdmin();
    }
    
    // Users collection - verification status
    match /users/{userId} {
      allow read: if request.auth.uid == userId || isAdmin();
      allow update: if request.auth.uid == userId && 
                       !request.resource.data.diff(resource.data).affectedKeys()
                         .hasAny(['role', 'verificationStatus']);
      allow update: if isAdmin();
    }
    
    function isAdmin() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid))
        .data.role == 'ADMIN';
    }
  }
}
```

---

## Step 5: Storage Security Rules

Update your `storage.rules`:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Seller verification images
    match /seller_verifications/{userId}/{allPaths=**} {
      allow read: if request.auth.uid == userId || isAdmin();
      allow write: if request.auth.uid == userId && 
                      request.resource.size < 5 * 1024 * 1024 && // 5MB max
                      request.resource.contentType.matches('image/.*');
    }
    
    function isAdmin() {
      return firestore.get(/databases/(default)/documents/users/$(request.auth.uid))
        .data.role == 'ADMIN';
    }
  }
}
```

---

## Step 6: Cloud Function for Admin Review

Create `functions/verifySellerIdentity.js`:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.notifyVerificationPending = functions.firestore
  .document('seller_verifications/{userId}')
  .onCreate(async (snap, context) => {
    const userId = context.params.userId;
    const verificationData = snap.data();
    
    try {
      // Get user email
      const userDoc = await admin.firestore()
        .collection('users')
        .doc(userId)
        .get();
      
      const userEmail = userDoc.data().email;
      const userName = userDoc.data().name;
      
      // Send notification to user
      await admin.firestore()
        .collection('notifications')
        .add({
          userId: userId,
          type: 'VERIFICATION_SUBMITTED',
          title: 'Verification Submitted',
          message: 'Your seller verification has been submitted. We will review it within 24-48 hours.',
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          read: false
        });
      
      // Send notification to admins
      const admins = await admin.firestore()
        .collection('users')
        .where('role', '==', 'ADMIN')
        .get();
      
      const adminNotifications = admins.docs.map(doc => ({
        userId: doc.id,
        type: 'VERIFICATION_PENDING_REVIEW',
        title: 'New Seller Verification',
        message: `${userName} has submitted verification. Confidence: ${verificationData.mlKitResult.confidence}%`,
        verificationId: userId,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        read: false
      }));
      
      const batch = admin.firestore().batch();
      adminNotifications.forEach(notification => {
        batch.add(admin.firestore().collection('notifications'), notification);
      });
      
      await batch.commit();
      
      console.log(`Verification submitted for user: ${userId}`);
    } catch (error) {
      console.error('Error processing verification:', error);
    }
  });

exports.approveSellerVerification = functions.https.onCall(async (data, context) => {
  // Verify admin
  const adminDoc = await admin.firestore()
    .collection('users')
    .doc(context.auth.uid)
    .get();
  
  if (adminDoc.data().role !== 'ADMIN') {
    throw new functions.https.HttpsError('permission-denied', 'Only admins can approve verifications');
  }
  
  const { userId, adminNotes } = data;
  
  try {
    // Update verification status
    await admin.firestore()
      .collection('seller_verifications')
      .doc(userId)
      .update({
        verificationStatus: 'APPROVED',
        adminReviewed: true,
        adminNotes: adminNotes,
        reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
        reviewedBy: context.auth.uid
      });
    
    // Update user role and status
    await admin.firestore()
      .collection('users')
      .doc(userId)
      .update({
        role: 'SELLER',
        verificationStatus: 'APPROVED'
      });
    
    // Send notification to user
    await admin.firestore()
      .collection('notifications')
      .add({
        userId: userId,
        type: 'VERIFICATION_APPROVED',
        title: 'Verification Approved',
        message: 'Congratulations! Your seller verification has been approved. You can now start selling.',
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        read: false
      });
    
    return { success: true };
  } catch (error) {
    throw new functions.https.HttpsError('internal', error.message);
  }
});

exports.rejectSellerVerification = functions.https.onCall(async (data, context) => {
  // Verify admin
  const adminDoc = await admin.firestore()
    .collection('users')
    .doc(context.auth.uid)
    .get();
  
  if (adminDoc.data().role !== 'ADMIN') {
    throw new functions.https.HttpsError('permission-denied', 'Only admins can reject verifications');
  }
  
  const { userId, rejectionReason } = data;
  
  try {
    // Update verification status
    await admin.firestore()
      .collection('seller_verifications')
      .doc(userId)
      .update({
        verificationStatus: 'REJECTED',
        adminReviewed: true,
        adminNotes: rejectionReason,
        reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
        reviewedBy: context.auth.uid
      });
    
    // Update user status
    await admin.firestore()
      .collection('users')
      .doc(userId)
      .update({
        verificationStatus: 'REJECTED'
      });
    
    // Send notification to user
    await admin.firestore()
      .collection('notifications')
      .add({
        userId: userId,
        type: 'VERIFICATION_REJECTED',
        title: 'Verification Rejected',
        message: `Your verification was rejected. Reason: ${rejectionReason}. You can retry.`,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        read: false
      });
    
    return { success: true };
  } catch (error) {
    throw new functions.https.HttpsError('internal', error.message);
  }
});
```

---

## Step 7: Deploy Cloud Functions

```bash
cd functions
firebase deploy --only functions:notifyVerificationPending,functions:approveSellerVerification,functions:rejectSellerVerification
```

---

## Testing Workflow

1. **User submits verification**
   - ML Kit validates face
   - Image uploaded to Storage
   - Verification document created in Firestore
   - Status set to PENDING

2. **Admin reviews**
   - Admin sees pending verification in dashboard
   - Admin approves/rejects with notes
   - Cloud Function updates user role
   - User receives notification

3. **User notified**
   - Notification sent to user
   - User can see verification status
   - If rejected, user can retry

---

## Cost Summary

| Service | Cost |
|---------|------|
| ML Kit | FREE |
| Firestore | Pay-as-you-go (~$0.06/100k reads) |
| Cloud Storage | Pay-as-you-go (~$0.02/GB) |
| Cloud Functions | Pay-as-you-go (~$0.40/million invocations) |

**Estimated monthly cost for 1000 sellers**: $5-10

---

## Next Steps

1. Deploy Cloud Functions
2. Create admin dashboard for verification review
3. Add email notifications
4. Implement appeal process
5. Add analytics dashboard
