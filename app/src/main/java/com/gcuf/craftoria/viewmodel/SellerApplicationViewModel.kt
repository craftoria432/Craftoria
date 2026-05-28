package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.SellerApplication
import com.gcuf.craftoria.data.repository.SellerApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SellerApplicationViewModel(
    private val repository: SellerApplicationRepository = SellerApplicationRepository()
) : ViewModel() {

    private val _applicationState = MutableStateFlow<SellerApplication?>(null)
    val applicationState: StateFlow<SellerApplication?> = _applicationState.asStateFlow()

    private val _applicationStatus = MutableStateFlow("none")  // pending, approved, rejected, none
    val applicationStatus: StateFlow<String> = _applicationStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    companion object {
        private const val TAG = "SellerApplicationVM"
    }

    // ✅ Create seller application (instant)
    fun createSellerApplication(
        userId: String,
        userName: String,
        userEmail: String,
        verificationPhotoUrl: String
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "📝 Creating seller application...")

                val result = repository.createSellerApplication(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    verificationPhotoUrl = verificationPhotoUrl
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Application created successfully")
                    _applicationStatus.value = "pending"
                    
                    // Start listening for status changes
                    listenToApplicationStatus(userId)
                } else {
                    Log.e(TAG, "❌ Failed to create application")
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to create application"
                }

                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception creating application", e)
                _errorMessage.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    // ✅ Fetch seller application status
    fun fetchApplicationStatus(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔍 Fetching application status...")

                val result = repository.getSellerApplicationByUserId(userId)

                if (result.isSuccess) {
                    val application = result.getOrNull()
                    if (application != null) {
                        _applicationState.value = application
                        _applicationStatus.value = application.status
                        Log.d(TAG, "✅ Status: ${application.status}")
                    } else {
                        _applicationStatus.value = "none"
                        Log.d(TAG, "📭 No application found")
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to fetch status"
                }

                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception fetching status", e)
                _errorMessage.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    // ✅ Listen to application status changes (real-time)
    fun listenToApplicationStatus(userId: String) {
        viewModelScope.launch {
            repository.listenToSellerApplicationStatus(userId)
                .catch { e ->
                    Log.e(TAG, "❌ Listener error", e)
                    _errorMessage.value = e.message ?: "Error listening to status"
                }
                .collect { application ->
                    if (application != null) {
                        _applicationState.value = application
                        _applicationStatus.value = application.status
                        Log.d(TAG, "📬 Status updated: ${application.status}")
                    } else {
                        _applicationStatus.value = "none"
                    }
                }
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}
