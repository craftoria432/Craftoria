package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.data.model.SellerApplicationStatus
import com.gcuf.craftoria.data.model.toMap
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "AuthRepository"
    }

    // Current user as Flow
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    /**
     * Sign up with email and password
     */
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        role: UserRole
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")

            val user = User(
                id = firebaseUser.uid,
                email = email,
                name = name,
                role = role, // stored as uppercase
                createdAt = System.currentTimeMillis()
            )

            usersCollection.document(firebaseUser.uid).set(user.toMap()).await()

            Log.d(TAG, "User created successfully: ${firebaseUser.uid}")
            Result.success(user)

        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")

            val userDoc = usersCollection.document(firebaseUser.uid).get().await()

            if (!userDoc.exists()) throw Exception("User data not found")

            val data = userDoc.data ?: throw Exception("User data empty")
            
            // Block deleted accounts from logging in
            val status = data["status"] as? String ?: ""
            if (status == "deleted") throw Exception("This account has been deleted.")
            
            // ✅ CHECK IF USER IS BANNED
            val isBanned = data["is_banned"] as? Boolean ?: false
            if (isBanned) {
                val banReason = data["ban_reason"] as? String ?: "Your account has been permanently banned."
                auth.signOut() // Sign out immediately
                throw Exception("Account Banned: $banReason")
            }
            
            // ✅ CHECK IF USER IS SUSPENDED
            val isSuspended = data["is_suspended"] as? Boolean ?: false
            if (isSuspended) {
                val suspensionUntil = data["suspension_until"] as? Long
                if (suspensionUntil != null && suspensionUntil > System.currentTimeMillis()) {
                    val suspensionReason = data["suspension_reason"] as? String ?: "Your account is temporarily suspended."
                    val daysRemaining = ((suspensionUntil - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                    auth.signOut() // Sign out immediately
                    throw Exception("Account Suspended: $suspensionReason (${daysRemaining} days remaining)")
                } else {
                    // Suspension expired, remove suspension flag
                    usersCollection.document(firebaseUser.uid).update(
                        mapOf(
                            "is_suspended" to false,
                            "suspension_until" to null,
                            "suspension_reason" to null
                        )
                    ).await()
                }
            }
            
            // Manual safe mapping
            val user = User(
                id = firebaseUser.uid,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                role = UserRole.fromString(data["role"] as? String),
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                profileImage = data["profile_image"] as? String ?: "",
                createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: (data["created_at"] as? Long) ?: 0L,
                storeName = data["store_name"] as? String ?: "",
                storeDescription = data["store_description"] as? String ?: "",
                verified = data["verified"] as? Boolean ?: false,
                verificationStatus = VerificationStatus.fromString(data["verification_status"] as? String),
                verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                rejectionReason = data["rejection_reason"] as? String ?: "",
                mainSellerId = data["main_seller_id"] as? String ?: "",
                sellerApplicationStatus = SellerApplicationStatus.fromString(data["seller_application_status"] as? String)
            )

            Log.d(TAG, "User signed in: ${firebaseUser.uid}")
            Result.success(user)

        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("Google sign in failed")

            val userDoc = usersCollection.document(firebaseUser.uid).get().await()

            val user = if (userDoc.exists()) {
                // ✅ Manual mapping instead of toObject() — handles lowercase role/status
                val data = userDoc.data ?: throw Exception("User data empty")
                
                // Block deleted accounts
                val status = data["status"] as? String ?: ""
                if (status == "deleted") throw Exception("This account has been deleted.")
                
                // ✅ CHECK IF USER IS BANNED
                val isBanned = data["is_banned"] as? Boolean ?: false
                if (isBanned) {
                    val banReason = data["ban_reason"] as? String ?: "Your account has been permanently banned."
                    auth.signOut()
                    throw Exception("Account Banned: $banReason")
                }
                
                // ✅ CHECK IF USER IS SUSPENDED
                val isSuspended = data["is_suspended"] as? Boolean ?: false
                if (isSuspended) {
                    val suspensionUntil = data["suspension_until"] as? Long
                    if (suspensionUntil != null && suspensionUntil > System.currentTimeMillis()) {
                        val suspensionReason = data["suspension_reason"] as? String ?: "Your account is temporarily suspended."
                        val daysRemaining = ((suspensionUntil - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                        auth.signOut()
                        throw Exception("Account Suspended: $suspensionReason (${daysRemaining} days remaining)")
                    } else {
                        // Suspension expired
                        usersCollection.document(firebaseUser.uid).update(
                            mapOf(
                                "is_suspended" to false,
                                "suspension_until" to null,
                                "suspension_reason" to null
                            )
                        ).await()
                    }
                }
                
                User(
                    id = firebaseUser.uid,
                    email = data["email"] as? String ?: firebaseUser.email ?: "",
                    name = data["name"] as? String ?: firebaseUser.displayName ?: "",
                    role = UserRole.fromString(data["role"] as? String),
                    phone = data["phone"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    profileImage = data["profile_image"] as? String
                        ?: firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: (data["created_at"] as? Long) ?: 0L,
                    storeName = data["store_name"] as? String ?: "",
                    storeDescription = data["store_description"] as? String ?: "",
                    verified = data["verified"] as? Boolean ?: false,
                    verificationStatus = VerificationStatus.fromString(
                        data["verification_status"] as? String
                    ),
                    verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                    rejectionReason = data["rejection_reason"] as? String ?: "",
                    mainSellerId = data["main_seller_id"] as? String ?: "",
                    sellerApplicationStatus = SellerApplicationStatus.fromString(data["seller_application_status"] as? String)
                )
            } else {
                // New Google user — create as BUYER
                val newUser = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "",
                    role = UserRole.BUYER,
                    profileImage = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis()
                )
                usersCollection.document(firebaseUser.uid).set(newUser.toMap()).await()
                newUser
            }

            Log.d(TAG, "Google sign in successful: ${firebaseUser.uid}")
            Result.success(user)

        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val firebaseUser = auth.currentUser ?: return Result.success(null)

            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            if (!userDoc.exists()) return Result.success(null)

            val data = userDoc.data ?: return Result.success(null)

            // ✅ Manual mapping — same as signIn()
            val user = User(
                id = firebaseUser.uid,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                role = UserRole.fromString(data["role"] as? String),
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                profileImage = data["profile_image"] as? String ?: "",
                createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: (data["created_at"] as? Long) ?: 0L,
                storeName = data["store_name"] as? String ?: "",
                storeDescription = data["store_description"] as? String ?: "",
                verified = data["verified"] as? Boolean ?: false,
                verificationStatus = VerificationStatus.fromString(
                    data["verification_status"] as? String
                ),
                verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                rejectionReason = data["rejection_reason"] as? String ?: "",
                mainSellerId = data["main_seller_id"] as? String ?: "",
                sellerApplicationStatus = SellerApplicationStatus.fromString(data["seller_application_status"] as? String)
            )

            Result.success(user)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user", e)
            Result.failure(e)
        }
    }

    suspend fun getUserByIdAsync(userId: String): User? {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) return null

            val data = userDoc.data ?: return null

            User(
                id = userId,
                email = data["email"] as? String ?: "",
                name = data["name"] as? String ?: "",
                role = UserRole.fromString(data["role"] as? String),
                phone = data["phone"] as? String ?: "",
                address = data["address"] as? String ?: "",
                profileImage = data["profile_image"] as? String ?: "",
                createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: (data["created_at"] as? Long) ?: 0L,
                storeName = data["store_name"] as? String ?: "",
                storeDescription = data["store_description"] as? String ?: "",
                verified = data["verified"] as? Boolean ?: false,
                verificationStatus = VerificationStatus.fromString(data["verification_status"] as? String),
                verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                rejectionReason = data["rejection_reason"] as? String ?: "",
                mainSellerId = data["main_seller_id"] as? String ?: "",
                sellerApplicationStatus = SellerApplicationStatus.fromString(data["seller_application_status"] as? String)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user by ID: $userId", e)
            null
        }
    }

    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user.toMap()).await()
            
            // Sync profile picture to all chats if it was updated
            if (user.profileImage.isNotEmpty()) {
                try {
                    val chatRepository = ChatRepository()
                    chatRepository.updateParticipantAvatar(user.id, user.profileImage)
                    Log.d(TAG, "✅ Synced profile picture to chats for user: ${user.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to sync profile picture to chats (non-critical)", e)
                    // Don't fail the whole operation if chat sync fails
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser
                ?: return Result.failure(Exception("No user logged in"))
            val userId = firebaseUser.uid

            // Step 1: ALWAYS do Firestore soft-delete FIRST
            val updates = mapOf(
                "status" to "deleted",
                "deleted_at" to System.currentTimeMillis(),
                "email" to "deleted_${userId}@removed.com",
                "name" to "Deleted User",
                "phone" to "",
                "profile_image" to ""
            )
            usersCollection.document(userId).set(updates, SetOptions.merge()).await()
            Log.d(TAG, "Firestore soft-delete done for: $userId")

            // Step 2: Try to delete Firebase Auth — may fail for Google users
            try {
                firebaseUser.delete().await()
                Log.d(TAG, "Firebase Auth deleted for: $userId")
            } catch (e: Exception) {
                // For Google users — just sign out, Firestore is already anonymized
                Log.w(TAG, "Auth delete failed (likely Google user), signing out: ${e.message}")
                auth.signOut()
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Delete account failed", e)
            Result.failure(e)
        }
    }

    /**
     * Update seller verification status and send notification
     */
    suspend fun updateSellerVerificationStatus(
        sellerId: String,
        approved: Boolean,
        reason: String = ""
    ): Result<Unit> {
        return try {
            // Update verification status
            val status = if (approved) "approved" else "rejected"
            val updates = mapOf(
                "verification_status" to status,
                "verified" to approved,
                "rejection_reason" to reason,
                "updated_at" to System.currentTimeMillis()
            )

            usersCollection.document(sellerId).update(updates).await()

            Log.d(TAG, "✅ Seller verification status updated: $sellerId -> $status")

            // ✅ Send notification to seller
            com.gcuf.craftoria.utils.NotificationHelper.notifySellerVerificationStatus(
                sellerId = sellerId,
                approved = approved,
                reason = reason
            )
            Log.d(TAG, "✅ Seller verification notification sent: $sellerId")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update seller verification status", e)
            Result.failure(e)
        }
    }

    /**
     * Approve or reject seller application (changes role from BUYER to SELLER when approved)
     */
    suspend fun updateSellerApplicationStatus(
        userId: String,
        approved: Boolean,
        reason: String = ""
    ): Result<Unit> {
        return try {
            val updates = if (approved) {
                mapOf(
                    "role" to "seller",
                    "seller_application_status" to "approved",
                    "verification_status" to "not_submitted",
                    "verified" to false,
                    "updated_at" to System.currentTimeMillis()
                )
            } else {
                mapOf(
                    "seller_application_status" to "rejected",
                    "rejection_reason" to reason,
                    "updated_at" to System.currentTimeMillis()
                )
            }

            usersCollection.document(userId).update(updates).await()

            Log.d(TAG, "✅ Seller application status updated: $userId -> ${if (approved) "approved" else "rejected"}")

            // Send notification to user
            com.gcuf.craftoria.utils.NotificationHelper.notifySellerApplicationStatus(
                userId = userId,
                approved = approved,
                reason = reason
            )
            Log.d(TAG, "✅ Seller application notification sent: $userId")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update seller application status", e)
            Result.failure(e)
        }
    }
}