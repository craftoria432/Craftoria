# Seller Verification: Instant Response & Status Checking Implementation

## Naming Recommendation

**Use: "Seller Application Under Review"**

**Reasoning:**
- More professional and clear
- Distinguishes between identity verification (ML Kit face detection) and seller application (business eligibility)
- Aligns with e-commerce terminology
- Users understand they're applying to become a seller, not just verifying identity
- Better for notifications: "Your seller application is under review"

---

## Requirements Overview

### 1. Instant Button Response
When user clicks "Submit Verification", the screen should:
- Immediately show loading state
- Navigate to "Seller Application Under Review" screen
- NOT wait for backend processing

### 2. Live Status Checking
From Profile Screen, user can:
- See "Check Verification Status" button
- Click to see current status (Approved/Rejected/Under Review)
- Get real-time updates

### 3. Dual Badges Until Approval
Profile displays:
- "Buyer" badge (always)
- "Pending Seller" badge (while under review)
- Badges disappear when approved/rejected

---

## Implementation Architecture

```
SellerVerificationScreen
    ↓
[Submit Verification Button]
    ↓
1. Immediately show loading screen
2. Upload photo to Cloudinary
3. Create seller_applications document
4. Navigate to SellerApplicationUnderReviewScreen
5. Backend processes asynchronously
```

---

## File Structure

```
app/src/main/java/com/gcuf/craftoria/
├── data/model/
│   └── SellerApplicationModels.kt (NEW)
├── data/repository/
│   └── SellerApplicationRepository.kt (NEW)
├── viewmodel/
│   └── SellerApplicationViewModel.kt (NEW)
└── ui/screens/auth/
    ├── SellerVerificationScreen.kt (UPDATED)
    └── SellerApplicationUnderReviewScreen.kt (NEW)
```

---

## Step 1: Create Data Models

### SellerApplicationModels.kt

```kotlin
package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class SellerApplication(
    val id: String = "",
    @PropertyName("user_id")
    val userId: String = "",
    @PropertyName("user_name")
    val userName: String = "",
    @PropertyName("user_email")
    val userEmail: String = "",
    @PropertyName("verification_photo_url")
    val verificationPhotoUrl: String = "",
    @PropertyName("status")
    val status: String = "pending",  // pending, approved, rejected
    @PropertyName("admin_notes")
    val adminNotes: String = "",
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("reviewed_at")
    val reviewedAt: Long = 0L,
    @PropertyName("estimated_review_time")
    val estimatedReviewTime: String = "24 - 48 hours"
)

enum class SellerApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED;

    override fun toString(): String = name.lowercase()
}

fun SellerApplication.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "user_id" to userId,
    "user_name" to userName,
    "user_email" to userEmail,
    "verification_photo_url" to verificationPhotoUrl,
    "status" to status,
    "admin_notes" to adminNotes,
    "created_at" to createdAt,
    "reviewed_at" to reviewedAt,
    "estimated_review_time" to estimatedReviewTime
)
```

---

## Step 2: Create Repository

### SellerApplicationRepository.kt

```kotlin
package com.gcuf.craftoria.data.repository

import android.util.Log
import com.gcuf.craftoria.data.model.SellerApplication
import com.gcuf.craftoria.data.model.SellerApplicationStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SellerApplicationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val applicationsCollection = db.collection("seller_applications")

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
            db.collection("users").document(userId)
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
```

---

## Step 3: Create ViewModel

### SellerApplicationViewModel.kt

```kotlin
package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.SellerApplication
import com.gcuf.craftoria.data.repository.SellerApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SellerApplicationViewModel(
    private val repository: SellerApplicationRepository = SellerApplicationRepository()
) : ViewModel() {

    private val _applicationState = MutableStateFlow<SellerApplication?>(null)
    val applicationState: StateFlow<SellerApplication?> = _applicationState.asStateFlow()

    private val _applicationStatus = MutableStateFlow("none")  // pending, approved, rejected, none
    val applicationStatus: StateFlow<String> = _applicationStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    companion object {
        private const val TAG = "SellerApplicationVM"
    }

    // ✅ Create seller application (instant)
    fun createSellerApplication(
        userId: String,
        userName: String,
        userEmail: String,
        verificationPhotoUrl: String
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "📝 Creating seller application...")

                val result = repository.createSellerApplication(
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    verificationPhotoUrl = verificationPhotoUrl
                )

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Application created successfully")
                    _applicationStatus.value = "pending"
                    
                    // Start listening for status changes
                    listenToApplicationStatus(userId)
                } else {
                    Log.e(TAG, "❌ Failed to create application")
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to create application"
                }

                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception creating application", e)
                _errorMessage.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    // ✅ Fetch seller application status
    fun fetchApplicationStatus(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "🔍 Fetching application status...")

                val result = repository.getSellerApplicationByUserId(userId)

                if (result.isSuccess) {
                    val application = result.getOrNull()
                    if (application != null) {
                        _applicationState.value = application
                        _applicationStatus.value = application.status
                        Log.d(TAG, "✅ Status: ${application.status}")
                    } else {
                        _applicationStatus.value = "none"
                        Log.d(TAG, "📭 No application found")
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to fetch status"
                }

                _isLoading.value = false

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception fetching status", e)
                _errorMessage.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    // ✅ Listen to application status changes (real-time)
    fun listenToApplicationStatus(userId: String) {
        viewModelScope.launch {
            repository.listenToSellerApplicationStatus(userId)
                .catch { e ->
                    Log.e(TAG, "❌ Listener error", e)
                    _errorMessage.value = e.message ?: "Error listening to status"
                }
                .collect { application ->
                    if (application != null) {
                        _applicationState.value = application
                        _applicationStatus.value = application.status
                        Log.d(TAG, "📬 Status updated: ${application.status}")
                    } else {
                        _applicationStatus.value = "none"
                    }
                }
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}
```

---

## Step 4: Update SellerVerificationScreen

### Key Changes:

```kotlin
// In SellerVerificationScreen.kt

fun submitVerification() {
    viewModelScope.launch {
        try {
            // ✅ 1. IMMEDIATELY show loading screen
            _uiState.value = VerificationState.Submitting
            
            // ✅ 2. Upload photo to Cloudinary
            val photoUrl = uploadPhotoToCloudinary(capturedImageUri)
            
            // ✅ 3. Create seller application (instant)
            sellerApplicationViewModel.createSellerApplication(
                userId = currentUser.id,
                userName = currentUser.name,
                userEmail = currentUser.email,
                verificationPhotoUrl = photoUrl
            )
            
            // ✅ 4. Navigate to review screen (don't wait for backend)
            onNavigateToReviewScreen()
            
        } catch (e: Exception) {
            _uiState.value = VerificationState.Error(e.message ?: "Failed to submit")
        }
    }
}
```

---

## Step 5: Create SellerApplicationUnderReviewScreen

### SellerApplicationUnderReviewScreen.kt

```kotlin
package com.gcuf.craftoria.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.SellerApplicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerApplicationUnderReviewScreen(
    currentUserId: String,
    onBackClick: () -> Unit,
    sellerApplicationViewModel: SellerApplicationViewModel = viewModel()
) {
    val applicationStatus by sellerApplicationViewModel.applicationStatus.collectAsState()
    val applicationState by sellerApplicationViewModel.applicationState.collectAsState()
    val isLoading by sellerApplicationViewModel.isLoading.collectAsState()

    LaunchedEffect(currentUserId) {
        sellerApplicationViewModel.listenToApplicationStatus(currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Seller Application",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundSecondary)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ✅ Loading spinner
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            color = Primary,
                            strokeWidth = 4.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Under Review",
                            modifier = Modifier.size(40.dp),
                            tint = Primary
                        )
                    }
                }

                // ✅ Title
                Text(
                    text = "Seller Application Under Review",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                // ✅ Description
                Text(
                    text = "Your seller application is being reviewed by our admin team. You'll be notified once approved to proceed with verification.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // ✅ Estimated review time
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFF57F17)
                            )
                            Text(
                                text = "Estimated Review Time",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF57F17)
                            )
                        }

                        Text(
                            text = applicationState?.estimatedReviewTime ?: "24 - 48 hours",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57F17)
                        )

                        Text(
                            text = "You'll receive a notification once your application is reviewed.",
                            fontSize = 12.sp,
                            color = Color(0xFFF57F17),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ✅ What happens next
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "WHAT HAPPENS NEXT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )

                        // Step 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Admin reviews your seller application",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Step 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "2",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "If approved, you can proceed with identity verification",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Step 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "3",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Once verified, you can start selling immediately",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
```

---

## Step 6: Update ProfileScreen for Dual Badges

### Key Changes:

```kotlin
// In ProfileScreen.kt

@Composable
fun ProfileBadgesSection(
    userRole: UserRole,
    applicationStatus: String  // pending, approved, rejected, none
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ Buyer badge (always)
        Badge(
            text = "Buyer",
            backgroundColor = Color(0xFFE3F2FD),
            textColor = Color(0xFF1976D2),
            icon = Icons.Default.ShoppingCart
        )

        // ✅ Seller badge (if approved)
        if (userRole == UserRole.SELLER) {
            Badge(
                text = "Seller",
                backgroundColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF2E7D32),
                icon = Icons.Default.Store
            )
        }

        // ✅ Pending Seller badge (if under review)
        if (applicationStatus == "pending") {
            Badge(
                text = "Pending Seller",
                backgroundColor = Color(0xFFFFF8E1),
                textColor = Color(0xFFF57F17),
                icon = Icons.Default.Schedule
            )
        }
    }
}

// ✅ Check Verification Status Button
@Composable
fun CheckVerificationStatusButton(
    applicationStatus: String,
    onCheckStatus: () -> Unit
) {
    if (applicationStatus == "pending") {
        Button(
            onClick = onCheckStatus,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Check Status",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Text(
                    text = "Check Verification Status",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
```

---

## Step 7: Update User Model

### Add to User.kt:

```kotlin
@PropertyName("seller_application_id")
val sellerApplicationId: String = "",

@PropertyName("seller_application_status")
val sellerApplicationStatus: String = "none",  // pending, approved, rejected, none

@PropertyName("seller_application_created_at")
val sellerApplicationCreatedAt: Long = 0L
```

---

## Step 8: Firestore Rules

```firestore
match /seller_applications/{applicationId} {
  allow read: if request.auth.uid == resource.data.user_id || isAdmin();
  allow create: if request.auth.uid == request.resource.data.user_id;
  allow update: if isAdmin();
  allow delete: if isAdmin();
}

match /users/{userId} {
  allow update: if request.auth.uid == userId
    && request.resource.data.diff(resource.data).affectedKeys()
      .hasOnly(['seller_application_id', 'seller_application_status', 'seller_application_created_at']);
}
```

---

## Implementation Flow

### User Journey:

```
1. User clicks "Submit Verification"
   ↓
2. Screen immediately shows loading state
   ↓
3. Photo uploaded to Cloudinary
   ↓
4. Seller application created in Firestore
   ↓
5. Navigate to "Seller Application Under Review" screen
   ↓
6. User sees:
   - Loading spinner
   - "Seller Application Under Review" title
   - Estimated review time (24-48 hours)
   - What happens next steps
   ↓
7. User goes to Profile
   ↓
8. Profile shows:
   - "Buyer" badge
   - "Pending Seller" badge
   - "Check Verification Status" button
   ↓
9. User clicks "Check Verification Status"
   ↓
10. Real-time listener shows current status
    - If approved: Navigate to next step
    - If rejected: Show rejection reason
    - If pending: Show review screen again
```

---

## Key Features

✅ **Instant Response:**
- Button click immediately shows loading
- No waiting for backend
- Smooth UX

✅ **Live Status Checking:**
- Real-time Firestore listener
- Status updates instantly
- No manual refresh needed

✅ **Dual Badges:**
- "Buyer" always visible
- "Pending Seller" while under review
- Badges update automatically

✅ **Professional Naming:**
- "Seller Application Under Review" is clear
- Distinguishes from identity verification
- Better for notifications

---

## Testing Checklist

- [ ] Click Submit Verification → Loading screen appears instantly
- [ ] Photo uploads to Cloudinary
- [ ] Seller application created in Firestore
- [ ] Navigate to review screen
- [ ] Profile shows "Buyer" + "Pending Seller" badges
- [ ] "Check Verification Status" button visible
- [ ] Click button → Shows current status
- [ ] Admin approves → Status updates in real-time
- [ ] User sees updated status without refresh
- [ ] Badges update when approved

---

## Summary

✅ **Instant Button Response:** Implemented with immediate loading state
✅ **Live Status Checking:** Real-time Firestore listener
✅ **Dual Badges:** "Buyer" + "Pending Seller" until approval
✅ **Professional Naming:** "Seller Application Under Review"
