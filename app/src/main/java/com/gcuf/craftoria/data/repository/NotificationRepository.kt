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
        private const val DEFAULT_MEMBER_COUNT = 1
    }

    suspend fun getUserNotifications(
        userId: String,
        category: NotificationCategory = NotificationCategory.ALL
    ): Result<List<Notification>> {
        return try {
            Log.d(TAG, "Fetching notifications for user: $userId, category: $category")

            var query: Query = notificationsCollection
                .whereEqualTo("user_id", userId)

            if (category != NotificationCategory.ALL) {
                query = query.whereEqualTo("category", category.name)
            }

            val snapshot = query
                .limit(MAX_FETCH_LIMIT)
                .get()
                .await()

            Log.d(TAG, "Raw documents fetched: ${snapshot.size()}")

            val notifications = snapshot.documents.mapNotNull { doc ->
                try {
                    Log.d(TAG, "Parsing document: ${doc.id}")
                    Log.d(TAG, "Document data: ${doc.data}")

                    val parsedNotification = doc.toObject(Notification::class.java)
                    if (parsedNotification == null) {
                        Log.w(TAG, "Notification object is null for doc: ${doc.id}")
                        return@mapNotNull null
                    }

                    var notification = parsedNotification.copy(id = doc.id)

                    // ✅ Enhanced member count handling for co-seller store notifications
                    if (notification.memberCount == 0 && notification.storeId.isNotEmpty()) {
                        try {
                            Log.d(TAG, "Fetching accurate member count for store: ${notification.storeId}")
                            val accurateMemberCount = com.gcuf.craftoria.utils.CoSellerMemberCountManager.getAccurateMemberCount(notification.storeId)
                            
                            notification = notification.copy(memberCount = accurateMemberCount)
                            Log.d(TAG, "✅ Updated member count for notification ${notification.id}: $accurateMemberCount")
                            
                            // ✅ Update the notification in Firestore for future use (retroactive fix)
                            try {
                                notificationsCollection.document(doc.id)
                                    .update("member_count", accurateMemberCount)
                                Log.d(TAG, "✅ Retroactively updated notification ${doc.id} with member count: $accurateMemberCount")
                            } catch (updateError: Exception) {
                                Log.w(TAG, "Could not update notification member count in Firestore", updateError)
                            }
                            
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not fetch accurate member count for ${notification.storeId}", e)
                            notification = notification.copy(memberCount = DEFAULT_MEMBER_COUNT)
                        }
                    }

                    Log.d(
                        TAG,
                        "Successfully parsed notification: ${notification.title}, memberCount: ${notification.memberCount}"
                    )

                    notification
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing notification: ${doc.id}, data: ${doc.data}", e)
                    null
                }
            }
                .sortedByDescending { it.createdAt }
                .take(MAX_RETURN_LIMIT)

            Log.d(TAG, "Fetched ${notifications.size} notifications")
            Result.success(notifications)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch notifications", e)
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(userId: String): Result<Int> {
        return try {
            Log.d(TAG, "Fetching unread count for user: $userId")

            val snapshot = notificationsCollection
                .whereEqualTo("user_id", userId)
                .whereEqualTo("is_read", false)
                .get()
                .await()

            val count = snapshot.size()
            Log.d(TAG, "Unread count: $count documents found")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get unread count", e)
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("is_read", true)
                .await()

            Log.d(TAG, "Notification marked as read: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification as read", e)
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
            Log.e(TAG, "Failed to mark all notifications as read", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .delete()
                .await()

            Log.d(TAG, "Notification deleted: $notificationId")
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
            val docRef = notificationsCollection
                .add(notification.toMap())
                .await()

            Log.d(TAG, "Notification created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification", e)
            Result.failure(e)
        }
    }
}