package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.Notification
import com.gcuf.craftoria.data.model.NotificationCategory
import com.gcuf.craftoria.data.model.toMap
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    companion object {
        private const val TAG = "NotificationRepository"
        private const val MAX_FETCH_LIMIT = 100L
        private const val MAX_RETURN_LIMIT = 50
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserNotifications
    //
    // BUG FIX 1: UNREAD is a UI-only filter concept — no document ever has
    // category = "UNREAD" stored in Firestore (toMap() only writes real
    // categories like ORDERS, SYSTEM etc.).  Passing it to
    // whereEqualTo("category", "UNREAD") therefore always returns zero results.
    //
    // Fix: when category == UNREAD, query by is_read = false instead of by
    // category. When category == ALL, skip the category filter entirely.
    // For every other category, filter by the category name as before.
    //
    // BUG FIX 2: The original code called
    // CoSellerMemberCountManager.getAccurateMemberCount() — a suspend function
    // — inside mapNotNull, which is not a suspend context.  The call compiled
    // because it was inside a coroutine scope, but it executed synchronously
    // and blocked the coroutine for every single document in the batch, making
    // the fetch dramatically slower.  Worse, it also called
    // notificationsCollection.document(doc.id).update(...) without .await(),
    // meaning the Firestore write was fire-and-forget and could race with the
    // read.  Member count enrichment is now handled in the ViewModel after
    // parsing, and the retroactive Firestore update is removed — the real-time
    // listener in NotificationCard already keeps member counts live.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun getUserNotifications(
        userId: String,
        category: NotificationCategory = NotificationCategory.ALL
    ): Result<List<Notification>> {
        return try {
            Log.d(TAG, "Fetching notifications for user: $userId, category: $category")
            var query: Query = notificationsCollection.whereEqualTo("user_id", userId)

            when (category) {
                // ALL → no extra filter; return every notification for the user
                NotificationCategory.ALL -> { /* no additional where clause */ }
                // UNREAD → filter by is_read = false, not by a "UNREAD" category string
                // (UNREAD is not stored as a category value in any document)
                NotificationCategory.UNREAD -> {
                    query = query.whereEqualTo("is_read", false)
                }
                // Every real category (ORDERS, PAYMENTS, SYSTEM, etc.) → filter by name
                else -> {
                    // Documents are written with category in UPPERCASE (see toMap())
                    query = query.whereEqualTo("category", category.name.uppercase())
                }
            }

            val snapshot = query.limit(MAX_FETCH_LIMIT).get().await()
            Log.d(TAG, "Raw documents fetched: ${snapshot.size()}")

            val notifications = snapshot.documents.mapNotNull { doc ->
                try {
                    val parsed = doc.toObject(Notification::class.java)
                    if (parsed == null) {
                        Log.w(TAG, "Null notification for doc: ${doc.id}")
                        return@mapNotNull null
                    }
                    // Assign the Firestore document ID — the model field defaults to ""
                    parsed.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notification: ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.createdAt }.take(MAX_RETURN_LIMIT)

            Log.d(TAG, "Returning ${notifications.size} notifications (category=$category)")
            Result.success(notifications)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch notifications", e)
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("user_id", userId)
                .whereEqualTo("is_read", false)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unread count", e)
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).update("is_read", true).await()
            Log.d(TAG, "Marked as read: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as read", e)
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("user_id", userId)
                .whereEqualTo("is_read", false)
                .get()
                .await()
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "is_read", true)
            }
            batch.commit().await()
            Log.d(TAG, "All notifications marked as read for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all as read", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId).delete().await()
            Log.d(TAG, "Deleted: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMultipleNotifications(notificationIds: List<String>): Result<Unit> {
        return try {
            val batch = db.batch()
            notificationIds.forEach { id ->
                batch.delete(notificationsCollection.document(id))
            }
            batch.commit().await()
            Log.d(TAG, "Deleted ${notificationIds.size} notifications")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete multiple notifications", e)
            Result.failure(e)
        }
    }

    suspend fun createNotification(notification: Notification): Result<String> {
        return try {
            val docRef = notificationsCollection.add(notification.toMap()).await()
            Log.d(TAG, "Notification created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification", e)
            Result.failure(e)
        }
    }
}
