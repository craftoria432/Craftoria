package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Notification
import com.gcuf.craftoria.data.model.NotificationCategory
import com.gcuf.craftoria.data.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Unified NotificationViewModel for both badge count and full notification management
 * 
 * Features:
 * - Real-time badge count via listeners
 * - Load all notifications with filtering
 * - Mark as read/unread
 * - Delete notifications
 * - UI state management
 */
class NotificationViewModel(
    private val notificationRepository: NotificationRepository = NotificationRepository()
) : ViewModel() {
    
    private val db = FirebaseFirestore.getInstance()
    
    // ==================== BADGE COUNT (Real-time) ====================
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private var badgeListener: ListenerRegistration? = null
    
    // ==================== FULL NOTIFICATIONS (Screen) ====================
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(NotificationCategory.ALL)
    val currentFilter: StateFlow<NotificationCategory> = _currentFilter.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var currentUserId: String? = null
    private var notificationsListener: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "NotificationViewModel"
    }
    
    // ==================== BADGE COUNT METHODS ====================
    
    /**
     * Start listening for real-time unread notification count (for badge)
     */
    fun startListening(userId: String) {
        if (currentUserId == userId && badgeListener != null) {
            Log.d(TAG, "Already listening for badge count: $userId")
            return
        }
        
        stopListening()
        currentUserId = userId
        
        Log.d(TAG, "Starting badge listener for user: $userId")
        
        // Real-time listener for unread count
        badgeListener = db.collection("notifications")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to badge count", error)
                    _error.value = error.message
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val count = snapshot.size()
                    Log.d(TAG, "Unread count updated: $count")
                    _unreadCount.value = count
                }
            }
    }
    
    /**
     * Stop listening for badge count and notification updates
     */
    fun stopListening() {
        badgeListener?.remove()
        badgeListener = null
        notificationsListener?.remove()
        notificationsListener = null
        Log.d(TAG, "Stopped all listeners")
    }
    
    // ==================== FULL NOTIFICATIONS METHODS ====================
    
    /**
     * Load all notifications for the screen with real-time updates
     * Implements real-time listener for member count and store name changes
     */
    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading notifications for user: $userId with real-time listener")
                _uiState.value = NotificationUiState.Loading
                _isLoading.value = true
                
                // Remove old listener if exists
                notificationsListener?.remove()
                
                // Set up real-time listener for notifications
                notificationsListener = db.collection("notifications")
                    .whereEqualTo("user_id", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error listening to notifications", error)
                            _error.value = error.message
                            _uiState.value = NotificationUiState.Error(error.message ?: "Unknown error")
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null) {
                            try {
                                val fetchedNotifications = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        val notification = doc.toObject(Notification::class.java)
                                        notification?.copy(id = doc.id)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing notification: ${doc.id}", e)
                                        null
                                    }
                                }
                                
                                // Apply current filter
                                val filteredNotifications = if (_currentFilter.value == NotificationCategory.ALL) {
                                    fetchedNotifications
                                } else {
                                    fetchedNotifications.filter { 
                                        it.categoryEnum == _currentFilter.value 
                                    }
                                }
                                
                                Log.d(TAG, "Real-time update: ${filteredNotifications.size} notifications (filter: ${_currentFilter.value})")
                                _notifications.value = filteredNotifications
                                
                                _uiState.value = if (filteredNotifications.isEmpty()) {
                                    NotificationUiState.Empty
                                } else {
                                    NotificationUiState.Success
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing notification snapshot", e)
                                _error.value = e.message
                                _uiState.value = NotificationUiState.Error(e.message ?: "Unknown error")
                            }
                        }
                    }
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception while setting up notification listener", e)
                _uiState.value = NotificationUiState.Error(e.message ?: "Unknown error")
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Filter notifications by category (applies to real-time listener)
     */
    fun filterNotifications(category: NotificationCategory, userId: String) {
        _currentFilter.value = category
        
        // Apply filter to current notifications
        val currentNotifications = _notifications.value
        val filteredNotifications = if (category == NotificationCategory.ALL) {
            currentNotifications
        } else {
            currentNotifications.filter { it.categoryEnum == category }
        }
        
        _notifications.value = filteredNotifications
        
        _uiState.value = if (filteredNotifications.isEmpty()) {
            NotificationUiState.Empty
        } else {
            NotificationUiState.Success
        }
        
        Log.d(TAG, "Filtered notifications to category: $category (${filteredNotifications.size} results)")
    }
    
    /**
     * Mark single notification as read
     */
    fun markAsRead(notificationId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = notificationRepository.markAsRead(notificationId)
                
                if (result.isSuccess) {
                    // Update local list
                    _notifications.value = _notifications.value.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }
                    
                    Log.d(TAG, "Marked notification as read: $notificationId")
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as read", e)
                _error.value = e.message
            }
        }
    }
    
    /**
     * Mark all notifications as read
     */
    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = NotificationUiState.Loading
                
                val result = notificationRepository.markAllAsRead(userId)
                
                if (result.isSuccess) {
                    // Update local list
                    _notifications.value = _notifications.value.map { notification ->
                        notification.copy(isRead = true)
                    }
                    
                    _unreadCount.value = 0
                    _uiState.value = NotificationUiState.ActionSuccess("All notifications marked as read")
                    Log.d(TAG, "Marked all notifications as read")
                } else {
                    _uiState.value = NotificationUiState.Error("Failed to mark all as read")
                    _error.value = result.exceptionOrNull()?.message
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark all as read", e)
                _uiState.value = NotificationUiState.Error(e.message ?: "Failed to mark all as read")
                _error.value = e.message
            }
        }
    }
    
    /**
     * Delete single notification
     */
    fun deleteNotification(notificationId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = notificationRepository.deleteNotification(notificationId)
                
                if (result.isSuccess) {
                    // Remove from local list
                    _notifications.value = _notifications.value.filter { it.id != notificationId }
                    
                    _uiState.value = NotificationUiState.ActionSuccess("Notification deleted")
                    Log.d(TAG, "Deleted notification: $notificationId")
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete notification", e)
                _uiState.value = NotificationUiState.Error("Failed to delete notification")
                _error.value = e.message
            }
        }
    }
    
    /**
     * Delete multiple notifications
     */
    fun deleteMultipleNotifications(notificationIds: List<String>, userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = NotificationUiState.Loading
                
                val result = notificationRepository.deleteMultipleNotifications(notificationIds)
                
                if (result.isSuccess) {
                    // Remove from local list
                    _notifications.value = _notifications.value.filter { it.id !in notificationIds }
                    
                    _uiState.value = NotificationUiState.ActionSuccess("${notificationIds.size} notifications deleted")
                    Log.d(TAG, "Deleted ${notificationIds.size} notifications")
                } else {
                    _uiState.value = NotificationUiState.Error("Failed to delete notifications")
                    _error.value = result.exceptionOrNull()?.message
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete multiple notifications", e)
                _uiState.value = NotificationUiState.Error("Failed to delete notifications")
                _error.value = e.message
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = NotificationUiState.Success
    }
    
    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}

/**
 * UI State for notification screen
 */
sealed class NotificationUiState {
    object Loading : NotificationUiState()
    object Success : NotificationUiState()
    object Empty : NotificationUiState()
    data class ActionSuccess(val message: String) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}