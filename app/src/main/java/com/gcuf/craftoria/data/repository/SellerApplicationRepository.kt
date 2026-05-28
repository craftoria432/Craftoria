package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.SellerApplication
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SellerApplicationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val applicationsCollection = db.collection("seller_applications")
    private val usersCollection = db.collection("users")

    companion object {
        private const val TAG = "SellerApplicationRepo"
    }

    // ✅ Create seller application (instant)
    suspend fun createSellerApplication(
        userId: String,
        userName: String,
        userEmail: String,
        verificationPhotoUrl: String
    ): Result<String> {
        return try {
            Log.d(TAG, "📝 Creating seller application for user: $userId")

            val applicationData = mapOf(
                "user_id" to userId,
                "user_name" to userName,
                "user_email" to userEmail,
                "verification_photo_url" to verificationPhotoUrl,
                "status" to "pending",
                "admin_notes" to "",
                "created_at" to System.currentTimeMillis(),
                "reviewed_at" to 0L,
                "estimated_review_time" to "24 - 48 hours"
            )

            val docRef = applicationsCollection.add(applicationData).await()
            Log.d(TAG, "✅ Seller application created: ${docRef.id}")

            // Update user document to mark as pending seller
            usersCollection.document(userId)
                .update(mapOf(
                    "seller_application_id" to docRef.id,
                    "seller_application_status" to "pending",
                    "seller_application_created_at" to System.currentTimeMillis()
                ))
                .await()

            Log.d(TAG, "✅ User document updated with application ID")

            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create seller application", e)
            Result.failure(e)
        }
    }

    // ✅ Get seller application by user ID
    suspend fun getSellerApplicationByUserId(userId: String): Result<SellerApplication?> {
        return try {
            Log.d(TAG, "🔍 Fetching seller application for user: $userId")

            val snapshot = applicationsCollection
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.d(TAG, "📭 No seller application found for user: $userId")
                return Result.success(null)
            }

            val doc = snapshot.documents.first()
            val application = doc.toObject(SellerApplication::class.java)?.copy(id = doc.id)

            Log.d(TAG, "✅ Seller application found: ${application?.status}")
            Result.success(application)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch seller application", e)
            Result.failure(e)
        }
    }

    // ✅ Listen to seller application status changes (real-time)
    fun listenToSellerApplicationStatus(userId: String): Flow<SellerApplication?> = callbackFlow {
        Log.d(TAG, "🎧 Starting listener for seller application status: $userId")

        val listener = applicationsCollection
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listener error: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "📭 No application found")
                    trySend(null)
                    return@addSnapshotListener
                }

                try {
                    val doc = snapshot.documents.first()
                    val application = doc.toObject(SellerApplication::class.java)?.copy(id = doc.id)
                    
                    Log.d(TAG, "📬 Application status: ${application?.status}")
                    trySend(application)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing application", e)
                    trySend(null)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing seller application listener")
            listener.remove()
        }
    }

    // ✅ Get application status
    suspend fun getApplicationStatus(userId: String): Result<String> {
        return try {
            val result = getSellerApplicationByUserId(userId)
            if (result.isSuccess) {
                val application = result.getOrNull()
                val status = application?.status ?: "none"
                Result.success(status)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get application status", e)
            Result.failure(e)
        }
    }
}
