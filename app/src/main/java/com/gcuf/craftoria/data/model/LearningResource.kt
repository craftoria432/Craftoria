package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class LearningCategory(
    val id: String = "",
    val title: String = "",
    val description: String = "",   // ← ADD THIS
    val icon: String = "",
    @PropertyName("display_order")
    val displayOrder: Int = 0,
    val tutorials: List<Tutorial> = emptyList()
)

data class Tutorial(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val duration: String = "",
    val icon: String = "",
    val url: String = "",
    @PropertyName("category_id")
    val categoryId: String = "",
    @PropertyName("is_video")
    val isVideo: Boolean = false,
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis()
)

data class BookmarkedTutorial(
    val id: String = "",
    @PropertyName("user_id")
    val userId: String = "",
    @PropertyName("tutorial_id")
    val tutorialId: String = "",
    @PropertyName("category_id")
    val categoryId: String = "",
    @PropertyName("bookmarked_at")
    val bookmarkedAt: Long = System.currentTimeMillis()
)

fun LearningCategory.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "title" to title,
    "description" to description,
    "icon" to icon,
    "display_order" to displayOrder,
    "tutorials" to tutorials.map { it.toMap() }
)

fun Tutorial.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "title" to title,
    "description" to description,
    "duration" to duration,
    "icon" to icon,
    "url" to url,
    "category_id" to categoryId,
    "is_video" to isVideo,
    "created_at" to createdAt
)

fun BookmarkedTutorial.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "user_id" to userId,
    "tutorial_id" to tutorialId,
    "category_id" to categoryId,
    "bookmarked_at" to bookmarkedAt
)