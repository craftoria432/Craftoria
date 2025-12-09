package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.data.repository.AuthRepository
import com.gcuf.craftoria.utils.CloudinaryManager
import com.google.firebase.firestore.SetOptions
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

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { firebaseUser ->
                if (firebaseUser != null) {
                    loadCurrentUser()
                } else {
                    _currentUser.value = null
                }
            }
        }
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
                    "verification_status" to VerificationStatus.PENDING.name,
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
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
