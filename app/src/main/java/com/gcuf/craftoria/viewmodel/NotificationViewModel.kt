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
 * CRITICAL BUG FIXES:
 * - Bug 3: Removed race condition between .get() and listener. Now uses ONLY real-time listener.
 * - Bug 4: Guard prevents listener re-attachment on every loadNotifications() call.
 * - Bug 5: Optimistic UI updates — update allNotifications immediately, revert on failure.
 * - Bug 1: UNREAD is UI-only filter (handled in Repository with is_read = false query).
 * - Bug 2: Removed blocking suspend calls from parsing (member count now in DisposableEffect).
 * 
 * Features:
 * - Real-time badge count via listeners
 * - Load all notifications with filtering
 * - Mark as read/unread with optimistic updates
 * - Delete notifications with optimistic updates
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
    private var badgeListenerUserId: String? = null
    
    // ==================== FULL NOTIFICATIONS (Screen) ====================
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Success)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(NotificationCategory.ALL)
    val currentFilter: StateFlow<NotificationCategory> = _currentFilter.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var notificationListenerUserId: String? = null
    private var notificationsListener: ListenerRegistration? = null
    
    // Backing list holds ALL notifications so filter can re-apply without re-querying Firestore
    private var allNotifications = listOf<Notification>()
    
    companion object {
        private const val TAG = "NotificationViewModel"
    }
    
    // ==================== BADGE COUNT METHODS ====================
    
    /**
     * Start listening for real-time unread notification count (for badge)
     * 
     * ✅ CRITICAL FIX: Removed overly strict blank check. Now logs warning but continues.
     */
    fun startListening(userId: String) {
        // ✅ Log warning if user ID is blank, but don't block listener setup
        if (userId.isBlank()) {
            Log.w(TAG, "startListening called with blank user ID - badge listener may not work correctly")
        }
        
        // ✅ Only skip if already listening for the SAME user
        if (badgeListenerUserId == userId && badgeListener != null) {
            Log.d(TAG, "Already listening for badge count: $userId")
            return
        }
        
        // ✅ If user changed, remove old badge listener
        if (badgeListenerUserId != userId && badgeListener != null) {
            Log.d(TAG, "User ID changed, removing old badge listener")
            badgeListener?.remove()
            badgeListener = null
        }
        
        badgeListenerUserId = userId
        
        Log.d(TAG, "Starting badge listener for user: $userId")
        
        // Real-time listener for unread count
        badgeListener = db.collection("notifications")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to badge count for user: $userId", error)
                    _error.value = error.message
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val count = snapshot.size()
                    Log.d(TAG, "Unread count updated for user: $userId, count: $count")
                    _unreadCount.value = count
                } else {
                    Log.w(TAG, "Snapshot is null for badge listener, user: $userId")
                }
            }
    }
    
    /**
     * Stop listening for badge count and notification updates
     */
    fun stopListening() {
        badgeListener?.remove()
        badgeListener = null
        badgeListenerUserId = null
        notificationsListener?.remove()
        notificationsListener = null
        notificationListenerUserId = null
        Log.d(TAG, "Stopped all listeners")
    }
    
    // ==================== FULL NOTIFICATIONS METHODS ====================
    
    /**
     * Load all notifications for the screen with real-time updates
     * 
     * BUG FIX 3 & 4: Uses ONLY real-time listener (no .get() race condition).
     * Guard prevents re-attachment if already listening for same userId.
     * Listener emits current state as first snapshot, so screen is never blank.
     * 
     * ✅ CRITICAL FIX: Removed overly strict blank check that prevented listener setup.
     * Now validates and logs but still attempts to set up listener.
     */
    fun loadNotifications(userId: String) {
        // ✅ Log warning if user ID is blank, but don't block listener setup
        if (userId.isBlank()) {
            Log.w(TAG, "loadNotifications called with blank user ID - listener may not work correctly")
        }
        
        // ✅ BUG FIX B: Guard prevents listener re-attachment on every call
        // BUT: Allow re-attachment if userId changed or listener was removed
        // Also: Reset if prior userId was blank and now we have a real ID
        if (notificationListenerUserId == userId && notificationsListener != null && userId.isNotBlank()) {
            Log.d(TAG, "Already listening for notifications: $userId")
            return
        }
        
        // ✅ If userId changed, remove old listener
        if (notificationListenerUserId != userId) {
            Log.d(TAG, "User ID changed from $notificationListenerUserId to $userId, removing old listener")
            notificationsListener?.remove()
            notificationsListener = null
        }
        
        notificationListenerUserId = userId
        
        Log.d(TAG, "Setting up real-time listener for notifications: $userId")
        _isLoading.value = true
        _uiState.value = NotificationUiState.Loading
        
        // ✅ BUG FIX 3: Use ONLY real-time listener (no .get() race condition)
        // Firestore delivers current state as first snapshot, so screen is never blank
        notificationsListener = db.collection("notifications")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to notifications for user: $userId", error)
                    _error.value = error.message
                    _uiState.value = NotificationUiState.Error(error.message ?: "Unknown error")
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        Log.d(TAG, "Snapshot received for user: $userId, document count: ${snapshot.size()}")
                        
                        // ✅ FIX: Helper function to safely convert Timestamp fields to Long
                        fun tsLong(value: Any?): Long = when (value) {
                            is Long -> value
                            is com.google.firebase.Timestamp -> value.toDate().time
                            is Number -> value.toLong()
                            is String -> value.toLongOrNull() ?: System.currentTimeMillis()
                            else -> System.currentTimeMillis()
                        }
                        
                        allNotifications = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data ?: return@mapNotNull null
                                
                                // ✅ FIX: Read every field manually — NEVER toObject()
                                // toObject() crashes when created_at is a Firestore Timestamp
                                // but Notification.createdAt is declared as Long.
                                Notification(
                                    id = doc.id,
                                    userId = doc.getString("user_id") ?: "",
                                    title = doc.getString("title") ?: "",
                                    description = doc.getString("description") ?: doc.getString("body") ?: "",
                                    category = doc.getString("category") ?: "SYSTEM",
                                    isRead = data["is_read"] as? Boolean ?: false,
                                    createdAt = tsLong(data["created_at"]),
                                    actionType = doc.getString("action_type") ?: "NONE",
                                    actionData = (data["action_data"] as? Map<String, String>) ?: emptyMap(),
                                    orderId = doc.getString("order_id") ?: "",
                                    storeId = doc.getString("store_id") ?: "",
                                    storeName = doc.getString("store_name") ?: "",
                                    inviterName = doc.getString("inviter_name") ?: "",
                                    memberCount = (data["member_count"] as? Number)?.toInt() ?: 0,
                                    productId = doc.getString("product_id") ?: "",
                                    productName = doc.getString("product_name") ?: "",
                                    senderName = doc.getString("sender_name") ?: "",
                                    negotiationPrice = (data["negotiation_price"] as? Number)?.toDouble() ?: 0.0,
                                    buyerName = doc.getString("buyer_name") ?: "",
                                    ratingValue = (data["rating_value"] as? Number)?.toInt() ?: 0,
                                    ratingReview = doc.getString("rating_review") ?: "",
                                    refundId = doc.getString("refund_id") ?: "",
                                    refundAmount = (data["refund_amount"] as? Number)?.toDouble() ?: 0.0,
                                    refundStatus = doc.getString("refund_status") ?: ""
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing notification: ${doc.id}", e)
                                null
                            }
                        }
                        
                        Log.d(TAG, "Real-time update: ${allNotifications.size} notifications loaded for user: $userId")
                        
                        if (allNotifications.isEmpty()) {
                            Log.w(TAG, "No notifications found for user: $userId")
                        }
                        
                        applyFilter(_currentFilter.value)
                        _isLoading.value = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing notification snapshot", e)
                        _error.value = e.message
                        _uiState.value = NotificationUiState.Error(e.message ?: "Unknown error")
                        _isLoading.value = false
                    }
                } else {
                    Log.w(TAG, "Snapshot is null for user: $userId")
                    _isLoading.value = false
                    // ✅ BUG FIX D: Set uiState to Empty when snapshot is null
                    _uiState.value = NotificationUiState.Empty
                }
            }
    }

    private fun applyFilter(category: NotificationCategory) {
        val filtered = when {
            category == NotificationCategory.UNREAD -> {
                // ✅ BUG FIX 1: UNREAD is UI-only filter — filter locally, not in Firestore query
                // (Repository handles is_read = false query when needed)
                val unreadNotifications = allNotifications.filter { !it.isRead }
                Log.d(TAG, "UNREAD filter: total=${allNotifications.size}, unread=${unreadNotifications.size}")
                allNotifications.forEach { n ->
                    Log.d(TAG, "  - Notification: ${n.id}, isRead=${n.isRead}, title=${n.title}")
                }
                unreadNotifications
            }
            category == NotificationCategory.ALL -> {
                Log.d(TAG, "ALL filter: showing ${allNotifications.size} notifications")
                allNotifications
            }
            else -> {
                val categoryFiltered = allNotifications.filter { it.categoryEnum == category }
                Log.d(TAG, "$category filter: total=${allNotifications.size}, filtered=${categoryFiltered.size}")
                categoryFiltered
            }
        }
        // Sort newest first — Firestore listener returns unsorted
        _notifications.value = filtered.sortedByDescending { it.createdAt }
        
        Log.d(TAG, "Applied filter: $category, results: ${filtered.size} notifications")
        
        // ✅ BUG FIX C: applyFilter Empty state logic is correct
        // Empty state only shows when allNotifications is truly empty (no data at all)
        // If filter is narrow but other categories have data, still show Success (empty filter message)
        _uiState.value = if (allNotifications.isEmpty()) {
            NotificationUiState.Empty   // truly no notifications
        } else {
            NotificationUiState.Success  // data exists, filter may just be narrow
        }
    }

    fun filterNotifications(category: NotificationCategory, userId: String) {
        _currentFilter.value = category
        applyFilter(category)
        Log.d(TAG, "Filtered to: $category (${_notifications.value.size} results)")
    }

    /**
     * Mark notification as read with optimistic update
     * ✅ BUG FIX 5: Update UI immediately, revert on failure
     */
    fun markAsRead(notificationId: String, userId: String) {
        // ✅ Optimistic update: update UI immediately
        val oldNotifications = allNotifications
        allNotifications = allNotifications.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        applyFilter(_currentFilter.value)
        
        viewModelScope.launch {
            try {
                val result = notificationRepository.markAsRead(notificationId)
                if (result.isSuccess) {
                    Log.d(TAG, "Marked as read: $notificationId")
                } else {
                    // ✅ Revert on failure
                    allNotifications = oldNotifications
                    applyFilter(_currentFilter.value)
                    _error.value = result.exceptionOrNull()?.message
                    Log.e(TAG, "Failed to mark as read, reverted: $notificationId")
                }
            } catch (e: Exception) {
                // ✅ Revert on failure
                allNotifications = oldNotifications
                applyFilter(_currentFilter.value)
                Log.e(TAG, "Failed to mark as read, reverted", e)
                _error.value = e.message
            }
        }
    }

    /**
     * Mark all notifications as read with optimistic update
     */
    fun markAllAsRead(userId: String) {
        // ✅ Optimistic update: update UI immediately
        val oldNotifications = allNotifications
        allNotifications = allNotifications.map { it.copy(isRead = true) }
        applyFilter(_currentFilter.value)
        _unreadCount.value = 0
        
        viewModelScope.launch {
            try {
                val result = notificationRepository.markAllAsRead(userId)
                if (result.isSuccess) {
                    _uiState.value = NotificationUiState.ActionSuccess("All notifications marked as read")
                    Log.d(TAG, "Marked all as read")
                } else {
                    // ✅ Revert on failure
                    allNotifications = oldNotifications
                    applyFilter(_currentFilter.value)
                    _uiState.value = NotificationUiState.Error("Failed to mark all as read")
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                // ✅ Revert on failure
                allNotifications = oldNotifications
                applyFilter(_currentFilter.value)
                Log.e(TAG, "Failed to mark all as read", e)
                _uiState.value = NotificationUiState.Error(e.message ?: "Failed to mark all as read")
                _error.value = e.message
            }
        }
    }

    /**
     * Delete notification with optimistic update
     * ✅ BUG FIX 5: Update UI immediately, revert on failure
     */
    fun deleteNotification(notificationId: String, userId: String) {
        // ✅ Optimistic update: update UI immediately
        val oldNotifications = allNotifications
        allNotifications = allNotifications.filter { it.id != notificationId }
        applyFilter(_currentFilter.value)
        
        viewModelScope.launch {
            try {
                val result = notificationRepository.deleteNotification(notificationId)
                if (result.isSuccess) {
                    _uiState.value = NotificationUiState.ActionSuccess("Notification deleted")
                    Log.d(TAG, "Deleted: $notificationId")
                } else {
                    // ✅ Revert on failure
                    allNotifications = oldNotifications
                    applyFilter(_currentFilter.value)
                    _error.value = result.exceptionOrNull()?.message
                    Log.e(TAG, "Failed to delete, reverted: $notificationId")
                }
            } catch (e: Exception) {
                // ✅ Revert on failure
                allNotifications = oldNotifications
                applyFilter(_currentFilter.value)
                Log.e(TAG, "Failed to delete, reverted", e)
                _uiState.value = NotificationUiState.Error("Failed to delete notification")
                _error.value = e.message
            }
        }
    }

    /**
     * Delete multiple notifications with optimistic update
     */
    fun deleteMultipleNotifications(notificationIds: List<String>, userId: String) {
        // ✅ Optimistic update: update UI immediately
        val oldNotifications = allNotifications
        allNotifications = allNotifications.filter { it.id !in notificationIds }
        applyFilter(_currentFilter.value)
        
        viewModelScope.launch {
            try {
                _uiState.value = NotificationUiState.Loading
                val result = notificationRepository.deleteMultipleNotifications(notificationIds)
                if (result.isSuccess) {
                    _uiState.value = NotificationUiState.ActionSuccess("${notificationIds.size} notifications deleted")
                    Log.d(TAG, "Deleted ${notificationIds.size} notifications")
                } else {
                    // ✅ Revert on failure
                    allNotifications = oldNotifications
                    applyFilter(_currentFilter.value)
                    _uiState.value = NotificationUiState.Error("Failed to delete notifications")
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                // ✅ Revert on failure
                allNotifications = oldNotifications
                applyFilter(_currentFilter.value)
                Log.e(TAG, "Failed to delete multiple notifications", e)
                _uiState.value = NotificationUiState.Error("Failed to delete notifications")
                _error.value = e.message
            }
        }
    }

    fun clearError() { _error.value = null }

    fun resetState() { _uiState.value = NotificationUiState.Success }

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