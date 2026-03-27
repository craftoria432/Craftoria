package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.BookmarkedTutorial
import com.gcuf.craftoria.data.model.LearningCategory
import com.gcuf.craftoria.data.model.Tutorial
import com.gcuf.craftoria.data.model.toMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LearningRepository {
    private val db = FirebaseFirestore.getInstance()
    private val categoriesCollection = db.collection("learning_categories")
    private val bookmarksCollection = db.collection("bookmarked_tutorials")

    companion object {
        private const val TAG = "LearningRepository"
    }

    fun observeAllCategories(): Flow<Result<List<LearningCategory>>> = callbackFlow {
        Log.d(TAG, "Subscribing to learning_categories real-time updates")
        val registration: ListenerRegistration = categoriesCollection
            .orderBy("display_order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Snapshot listener error", e)
                    trySend(Result.failure(e))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(Result.success(emptyList()))
                    return@addSnapshotListener
                }

                val categories = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val title = doc.getString("title") ?: ""
                        val icon = doc.getString("icon") ?: ""
                        val displayOrder = doc.getLong("display_order")?.toInt() ?: 0
                        val description = doc.getString("description") ?: ""

                        @Suppress("UNCHECKED_CAST")
                        val tutorialsData = doc.get("tutorials") as? List<Map<String, Any>> ?: emptyList()
                        val tutorials = tutorialsData.mapNotNull { tutorialMap ->
                            try {
                                Tutorial(
                                    id = tutorialMap["id"] as? String ?: "",
                                    title = tutorialMap["title"] as? String ?: "",
                                    description = tutorialMap["description"] as? String ?: "",
                                    duration = tutorialMap["duration"] as? String ?: "",
                                    icon = tutorialMap["icon"] as? String ?: "",
                                    url = tutorialMap["url"] as? String ?: "",
                                    categoryId = tutorialMap["category_id"] as? String ?: id,
                                    isVideo = tutorialMap["is_video"] as? Boolean ?: false,
                                    createdAt = (tutorialMap["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                        ?: (tutorialMap["created_at"] as? Number)?.toLong()
                                        ?: System.currentTimeMillis()
                                )
                            } catch (ex: Exception) {
                                Log.e(TAG, "Error parsing tutorial", ex)
                                null
                            }
                        }

                        LearningCategory(
                            id = id,
                            title = title,
                            description = description,
                            icon = icon,
                            displayOrder = displayOrder,
                            tutorials = tutorials
                        )
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error parsing category", ex)
                        null
                    }
                }

                Log.d(TAG, "Realtime: fetched ${categories.size} categories")
                trySend(Result.success(categories))
            }

        awaitClose {
            Log.d(TAG, "Unsubscribing from learning_categories updates")
            registration.remove()
        }
    }

    suspend fun getUserBookmarks(userId: String): Result<List<BookmarkedTutorial>> {
        return try {
            val snapshot = bookmarksCollection
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            val bookmarks = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BookmarkedTutorial::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Fetched ${bookmarks.size} bookmarks for user")
            Result.success(bookmarks)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch bookmarks", e)
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(
        userId: String,
        tutorialId: String,
        categoryId: String
    ): Result<Boolean> {
        return try {
            // Check if already bookmarked
            val existing = bookmarksCollection
                .whereEqualTo("user_id", userId)
                .whereEqualTo("tutorial_id", tutorialId)
                .get()
                .await()

            if (existing.isEmpty) {
                // Add bookmark
                val bookmark = BookmarkedTutorial(
                    userId = userId,
                    tutorialId = tutorialId,
                    categoryId = categoryId,
                    bookmarkedAt = System.currentTimeMillis()
                )
                bookmarksCollection.add(bookmark.toMap()).await()
                Log.d(TAG, "Bookmark added")
                Result.success(true) // Now bookmarked
            } else {
                // Remove bookmark
                existing.documents.forEach { it.reference.delete().await() }
                Log.d(TAG, "Bookmark removed")
                Result.success(false) // Now not bookmarked
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle bookmark", e)
            Result.failure(e)
        }
    }

    suspend fun searchTutorials(query: String): Result<List<LearningCategory>> {
        return try {
            // For search we still use a one-shot fetch to avoid keeping an extra listener
            val snapshot = categoriesCollection
                .orderBy("display_order")
                .get()
                .await()

            val allCategories = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val icon = doc.getString("icon") ?: ""
                    val displayOrder = doc.getLong("display_order")?.toInt() ?: 0
                    val description = doc.getString("description") ?: ""

                    @Suppress("UNCHECKED_CAST")
                    val tutorialsData = doc.get("tutorials") as? List<Map<String, Any>> ?: emptyList()
                    val tutorials = tutorialsData.mapNotNull { tutorialMap ->
                        try {
                            Tutorial(
                                id = tutorialMap["id"] as? String ?: "",
                                title = tutorialMap["title"] as? String ?: "",
                                description = tutorialMap["description"] as? String ?: "",
                                duration = tutorialMap["duration"] as? String ?: "",
                                icon = tutorialMap["icon"] as? String ?: "",
                                url = tutorialMap["url"] as? String ?: "",
                                categoryId = tutorialMap["category_id"] as? String ?: id,
                                isVideo = tutorialMap["is_video"] as? Boolean ?: false,
                                createdAt = (tutorialMap["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                    ?: (tutorialMap["created_at"] as? Number)?.toLong()
                                    ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing tutorial", e)
                            null
                        }
                    }

                    LearningCategory(
                        id = id,
                        title = title,
                        description = description,
                        icon = icon,
                        displayOrder = displayOrder,
                        tutorials = tutorials
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing category for search", e)
                    null
                }
            }

            val filtered = allCategories.mapNotNull { category ->
                val matchingTutorials = category.tutorials.filter { tutorial ->
                    tutorial.title.contains(query, ignoreCase = true) ||
                            tutorial.description.contains(query, ignoreCase = true)
                }

                if (matchingTutorials.isNotEmpty()) {
                    category.copy(tutorials = matchingTutorials)
                } else {
                    null
                }
            }

            Log.d(TAG, "Search found ${filtered.size} categories with matching tutorials")
            Result.success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(e)
        }
    }
}