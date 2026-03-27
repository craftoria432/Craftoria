package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.data.model.SellerApplicationStatus
import com.gcuf.craftoria.data.repository.AuthRepository
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.utils.NotificationHelper
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val firestore = Firebase.firestore
    private var userListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { firebaseUser ->
                if (firebaseUser != null) {
                    loadCurrentUser()
                    startRealtimeUserListener(firebaseUser.uid)
                } else {
                    _currentUser.value = null
                    stopRealtimeUserListener()
                }
            }
        }
    }

    /**
     * Start real-time listener for user changes
     * Updates currentUser StateFlow whenever user data changes in Firebase
     */
    private fun startRealtimeUserListener(userId: String) {
        // Remove existing listener if any
        stopRealtimeUserListener()
        
        userListenerRegistration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AuthViewModel", "❌ Real-time listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val data = snapshot.data ?: return@addSnapshotListener
                        
                        val user = User(
                            id = userId,
                            email = data["email"] as? String ?: "",
                            name = data["name"] as? String ?: "",
                            role = com.gcuf.craftoria.data.model.UserRole.fromString(data["role"] as? String),
                            phone = data["phone"] as? String ?: "",
                            address = data["address"] as? String ?: "",
                            profileImage = data["profile_image"] as? String ?: "",
                            createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: (data["created_at"] as? Long) ?: 0L,
                            storeName = data["store_name"] as? String ?: "",
                            storeDescription = data["store_description"] as? String ?: "",
                            verified = data["verified"] as? Boolean ?: false,
                            verificationStatus = VerificationStatus.fromString(data["verification_status"] as? String),
                            verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                            rejectionReason = data["rejection_reason"] as? String ?: "",
                            mainSellerId = data["main_seller_id"] as? String ?: "",
                            sellerApplicationStatus = SellerApplicationStatus.fromString(data["seller_application_status"] as? String),
                            themePreference = data["theme_preference"] as? String ?: "rose"
                        )
                        
                        _currentUser.value = user
                        Log.d("AuthViewModel", "✅ Real-time user update: ${user.name}")
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "❌ Error parsing user data: ${e.message}")
                    }
                }
            }
    }

    /**
     * Stop real-time listener for user changes
     */
    private fun stopRealtimeUserListener() {
        userListenerRegistration?.remove()
        userListenerRegistration = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeUserListener()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val result = authRepository.getCurrentUser()
            result.onSuccess { user ->
                _currentUser.value = user
            }.onFailure {
                _currentUser.value = null
                Log.w("AuthViewModel", "Failed to load user: ${it.message}")
            }
        }
    }

    fun signUp(email: String, password: String, name: String, phone: String, role: UserRole) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signUp(email, password, name, role)

            _authState.value = if (result.isSuccess) {
                val createdUser = result.getOrNull()
                createdUser?.let { user ->
                    try {
                        val updates = mapOf("phone" to phone)
                        firestore.collection("users")
                            .document(user.id)
                            .set(updates, SetOptions.merge())
                            .await()

                        _currentUser.value = user.copy(phone = phone)
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Failed to update phone: ${e.message}")
                    }
                }
                AuthState.Success("Account created successfully!")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signIn(email, password)

            _authState.value = if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                AuthState.Success("Welcome back!")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    fun login(email: String, password: String) = signIn(email, password)

    fun uploadVerificationPhoto(context: Context, userId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val photoUrl = CloudinaryManager.uploadImage(
                    context = context,
                    imageUri = imageUri,
                    folder = "craftoria/verification_photos"
                )

                val updates = mapOf(
                    "verification_status" to VerificationStatus.PENDING.name.lowercase(),
                    "verification_photo_url" to photoUrl,
                    "verified" to false
                )

                firestore.collection("users")
                    .document(userId)
                    .set(updates, SetOptions.merge())
                    .await()

                _currentUser.value = _currentUser.value?.copy(
                    verificationStatus = VerificationStatus.PENDING,
                    verificationPhotoUrl = photoUrl,
                    verified = false
                )

                _authState.value = AuthState.Success("Verification submitted successfully!")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Upload error", e)
                _authState.value = AuthState.Error(e.message ?: "Upload failed")
            }
        }
    }
    fun updateProfilePhoto(context: Context, imageUri: Uri, userId: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                // Upload to Cloudinary
                val uploadedUrl = CloudinaryManager.uploadImage(
                    context = context,
                    imageUri = imageUri,
                    folder = "craftoria/profile_photos"
                )

                // Update Firestore
                Firebase.firestore.collection("users")
                    .document(userId)
                    .update("profile_image", uploadedUrl)
                    .await()

                // Update locally
                _currentUser.value = _currentUser.value?.copy(
                    profileImage = uploadedUrl
                )

                _authState.value = AuthState.Success("Profile photo updated!")

            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to update photo: ${e.message}")
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.resetPassword(email)

            if (result.isSuccess) {
                onResult(true, null)
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                onResult(false, errorMessage)
            }

            _authState.value = AuthState.Idle
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signInWithGoogle(idToken)

            _authState.value = if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                AuthState.Success("Welcome!")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Google sign-in failed")
            }
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.deleteAccount()
            if (result.isSuccess) {
                _currentUser.value = null
                onComplete()
            } else {
                _authState.value = AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to delete account"
                )
            }
        }
    }
    fun setAuthError(message: String) {
        _authState.value = AuthState.Error(message)
    }
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Idle
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    ?: return@launch
                val email = firebaseUser.email ?: return@launch

                // Re-authenticate first (Firebase requires this before password change)
                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(email, currentPassword)

                firebaseUser.reauthenticate(credential).await()
                firebaseUser.updatePassword(newPassword).await()

                _authState.value = AuthState.Success("Password updated successfully!")
                Log.d("AuthViewModel", "Password changed successfully")

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to change password", e)
                _authState.value = AuthState.Error(e.message ?: "Failed to change password")
            }
        }
    }
    fun listenToVerificationStatus() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance()
            .currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val newStatus = VerificationStatus.fromString(
                    snapshot.getString("verification_status")
                )
                val newVerified = snapshot.getBoolean("verified") ?: false
                val newRole = UserRole.fromString(snapshot.getString("role"))
                val newRejectionReason = snapshot.getString("rejection_reason") ?: ""
                val newSellerApplicationStatus = SellerApplicationStatus.fromString(
                    snapshot.getString("seller_application_status")
                )

                val current = _currentUser.value
                if (current != null) {
                    _currentUser.value = current.copy(
                        role = newRole,
                        verificationStatus = newStatus,
                        verified = newVerified,
                        rejectionReason = newRejectionReason,
                        sellerApplicationStatus = newSellerApplicationStatus
                    )
                    Log.d("AuthViewModel", "Live update → role:$newRole status:$newStatus verified:$newVerified appStatus:$newSellerApplicationStatus")
                }
            }
    }
    fun upgradeToSeller(userId: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                // Only set seller application status to PENDING, keep role as BUYER
                val updates = mapOf(
                    "seller_application_status" to "pending",
                    "verification_status" to "not_submitted",
                    "verified" to false,
                    "application_submitted_at" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(userId)
                    .set(updates, SetOptions.merge())
                    .await()

                // ✅ Update local state - role stays BUYER, only application status changes
                _currentUser.value = _currentUser.value?.copy(
                    sellerApplicationStatus = SellerApplicationStatus.PENDING,
                    verificationStatus = VerificationStatus.NOT_SUBMITTED,
                    verified = false
                )

                // ✅ Notify admins about new seller application
                try {
                    val currentUserData = _currentUser.value
                    if (currentUserData != null) {
                        NotificationHelper.notifyAdminNewSellerApplication(
                            userId = userId,
                            userName = currentUserData.name,
                            userEmail = currentUserData.email
                        )
                    }
                } catch (notifError: Exception) {
                    Log.e("AuthViewModel", "Failed to notify admins", notifError)
                    // Don't fail the operation if notification fails
                }

                _authState.value = AuthState.Success("Seller application submitted! You'll be notified once reviewed.")

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to submit seller application")
            }
        }
    }

    fun resetSellerApplication(userId: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                // Reset seller application completely - start fresh
                val updates = mapOf(
                    "seller_application_status" to "none",
                    "verification_status" to "not_submitted",
                    "verified" to false,
                    "application_reset_at" to System.currentTimeMillis(),
                    "application_rejected_at" to FieldValue.delete(),
                    "application_rejection_reason" to FieldValue.delete(),
                    "application_rejection_message" to FieldValue.delete(),
                    "verification_rejected_at" to FieldValue.delete(),
                    "verification_rejection_reason" to FieldValue.delete(),
                    "verification_rejection_message" to FieldValue.delete()
                )

                firestore.collection("users")
                    .document(userId)
                    .set(updates, SetOptions.merge())
                    .await()

                // Update local state
                _currentUser.value = _currentUser.value?.copy(
                    sellerApplicationStatus = SellerApplicationStatus.NONE,
                    verificationStatus = VerificationStatus.NOT_SUBMITTED,
                    verified = false
                )

                _authState.value = AuthState.Success("Application reset. You can start fresh!")

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to reset application")
            }
        }
    }

    fun revertToBuyer(userId: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                // Revert seller back to buyer
                val updates = mapOf(
                    "role" to "buyer",
                    "seller_application_status" to "none",
                    "verification_status" to "not_submitted",
                    "verified" to false,
                    "reverted_to_buyer_at" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(userId)
                    .set(updates, SetOptions.merge())
                    .await()

                // ✅ Update local state
                _currentUser.value = _currentUser.value?.copy(
                    role = UserRole.BUYER,
                    sellerApplicationStatus = SellerApplicationStatus.NONE,
                    verificationStatus = VerificationStatus.NOT_SUBMITTED,
                    verified = false
                )

                _authState.value = AuthState.Success("Successfully reverted to buyer account!")

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to revert to buyer")
            }
        }
    }

    fun updateUserName(userId: String, newName: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading

                // Update in Firestore
                firestore.collection("users")
                    .document(userId)
                    .update("name", newName)
                    .await()

                // Update Firebase Auth display name
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    ?.updateProfile(
                        com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build()
                    )?.await()

                // ✅ Update local StateFlow immediately so UI recomposes
                _currentUser.value = _currentUser.value?.copy(name = newName)

                _authState.value = AuthState.Success("Name updated successfully!")
                Log.d("AuthViewModel", "Name updated: $newName")

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to update name", e)
                _authState.value = AuthState.Error(e.message ?: "Failed to update name")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
