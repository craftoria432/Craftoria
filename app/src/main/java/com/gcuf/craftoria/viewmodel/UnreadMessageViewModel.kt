package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.repository.UnreadMessageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UnreadMessageViewModel(
    private val unreadMessageRepository: UnreadMessageRepository = UnreadMessageRepository()
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    companion object {
        private const val TAG = "UnreadMessageVM"
    }

    fun startListening(userId: String) {
        if (userId.isBlank()) {
            Log.w(TAG, "⚠️ Cannot start listening with blank userId")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "🎧 Starting unread count listener for: $userId")
                
                unreadMessageRepository.getUnreadCountFlow(userId)
                    .catch { e ->
                        Log.e(TAG, "❌ Unread count flow error", e)
                        _unreadCount.value = 0
                    }
                    .collect { count ->
                        Log.d(TAG, "📬 Unread count updated: $count")
                        _unreadCount.value = count
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start listening", e)
                _unreadCount.value = 0
            }
        }
    }

    fun stopListening() {
        Log.d(TAG, "🔌 Stopping unread count listener")
        _unreadCount.value = 0
    }

    // Get current unread count synchronously (for one-time checks)
    suspend fun getCurrentUnreadCount(userId: String): Int {
        return try {
            unreadMessageRepository.getTotalUnreadCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get current unread count", e)
            0
        }
    }
}