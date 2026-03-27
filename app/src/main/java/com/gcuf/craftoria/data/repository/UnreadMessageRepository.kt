package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UnreadMessageRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatsCollection = db.collection("chats")

    companion object {
        private const val TAG = "UnreadMessageRepo"
    }

    // Get total unread message count for a user
    suspend fun getTotalUnreadCount(userId: String): Int {
        return try {
            Log.d(TAG, "🔍 Getting unread count for user: $userId")
            
            val snapshot = chatsCollection
                .whereArrayContains("participant_ids", userId)
                .get()
                .await()

            var totalUnread = 0
            snapshot.documents.forEach { doc ->
                try {
                    val unreadCount = doc.get("unread_count.$userId") as? Long ?: 0L
                    totalUnread += unreadCount.toInt()
                    Log.d(TAG, "   Chat ${doc.id}: $unreadCount unread")
                } catch (e: Exception) {
                    Log.e(TAG, "   Error reading unread count for chat ${doc.id}", e)
                }
            }

            Log.d(TAG, "✅ Total unread messages for $userId: $totalUnread")
            totalUnread

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get unread count for $userId", e)
            0
        }
    }

    // Listen to unread message count changes in real-time
    fun getUnreadCountFlow(userId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🎧 Starting unread count listener for user: $userId")

        val listener = chatsCollection
            .whereArrayContains("participant_ids", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Unread count listener error: ${error.message}", error)
                    trySend(0)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "⚠️ Snapshot is null")
                    trySend(0)
                    return@addSnapshotListener
                }

                var totalUnread = 0
                snapshot.documents.forEach { doc ->
                    try {
                        val unreadCount = doc.get("unread_count.$userId") as? Long ?: 0L
                        totalUnread += unreadCount.toInt()
                    } catch (e: Exception) {
                        Log.e(TAG, "   Error reading unread count for chat ${doc.id}", e)
                    }
                }

                Log.d(TAG, "📬 Unread count updated for $userId: $totalUnread")
                trySend(totalUnread)
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing unread count listener for user: $userId")
            listener.remove()
        }
    }
}