package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.repository.ThemeRepository
import com.gcuf.craftoria.ui.theme.ThemeManager
import com.gcuf.craftoria.ui.theme.ThemeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing theme selection UI state and coordinating with repository
 * 
 * Requirements: 1.3, 12.1, 12.2, 12.3
 */
class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    private val themeManager: ThemeManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "ThemeViewModel"
    }
    
    private val _selectedTheme = MutableStateFlow<ThemeType>(ThemeType.ROSE)
    val selectedTheme: StateFlow<ThemeType> = _selectedTheme.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Select a new theme and update it in Firebase
     * Shows loading indicator and handles errors
     */
    fun selectTheme(theme: ThemeType, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Update theme in Firebase
                themeRepository.updateUserThemePreference(userId, theme)
                
                // Update theme in manager
                themeManager.setTheme(theme)
                
                // Update selected theme
                _selectedTheme.value = theme
                
                // Clear any previous errors
                _errorMessage.value = null
                
                Log.d(TAG, "✅ Theme updated successfully to ${theme.name}")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update theme: ${e.message}"
                Log.e(TAG, "❌ Error selecting theme", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load the user's current theme preference from Firebase
     */
    fun loadUserTheme(userId: String) {
        viewModelScope.launch {
            try {
                val theme = themeRepository.getUserThemePreference(userId)
                _selectedTheme.value = theme
                themeManager.setTheme(theme)
                Log.d(TAG, "✅ User theme loaded: ${theme.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading theme, defaulting to ROSE", e)
                _selectedTheme.value = ThemeType.ROSE
                themeManager.setTheme(ThemeType.ROSE)
            }
        }
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
