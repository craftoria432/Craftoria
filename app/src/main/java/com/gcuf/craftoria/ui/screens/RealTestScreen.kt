package com.gcuf.craftoria.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gcuf.craftoria.utils.CloudinaryManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RealTestScreen() {
    var testStep by remember { mutableStateOf("Ready to test") }
    var testResults by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Helper function to add results
    fun addResult(message: String) {
        testResults = testResults + message
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        addResult("• 📸 Image selected: ${uri?.lastPathSegment ?: "unknown"}")
    }

    suspend fun testAuthentication(): Boolean {
        return try {
            testStep = "Testing Firebase Authentication..."

            val auth = Firebase.auth
            val testEmail = "test${System.currentTimeMillis()}@craftoria.com"
            val testPassword = "Test@123456"

            addResult("• 🔐 Creating test user...")
            val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
            val user = authResult.user

            if (user != null) {
                addResult("• ✅ User created: ${user.uid}")
                addResult("• 📧 Email: $testEmail")

                auth.signOut()
                addResult("• 🚪 Signed out")

                val signInResult = auth.signInWithEmailAndPassword(testEmail, testPassword).await()
                addResult("• ✅ Signed in: ${signInResult.user?.uid}")

                true
            } else {
                addResult("• ❌ User creation failed")
                false
            }
        } catch (e: Exception) {
            addResult("• ❌ Auth error: ${e.message}")
            false
        }
    }

    suspend fun testFirestoreStorage(userId: String): Boolean {
        return try {
            testStep = "Testing Firestore Database..."

            val firestore = Firebase.firestore

            val userData = mapOf(
                "id" to userId,
                "name" to "Test User",
                "email" to "test@craftoria.com",
                "role" to "buyer",
                "createdAt" to System.currentTimeMillis(),
                "testData" to true
            )

            addResult("• 💾 Storing user data in Firestore...")
            firestore.collection("users")
                .document(userId)
                .set(userData)
                .await()

            addResult("• ✅ User data stored in Firestore")

            val productData = mapOf(
                "title" to "Test Product",
                "description" to "This is a test product",
                "price" to 99.99,
                "sellerId" to userId,
                "category" to "handicraft",
                "createdAt" to System.currentTimeMillis(),
                "testData" to true
            )

            addResult("• 📦 Creating test product...")
            val productRef = firestore.collection("products")
                .add(productData)
                .await()

            addResult("• ✅ Product created: ${productRef.id}")

            addResult("• 📖 Reading data back...")
            val userDoc = firestore.collection("users").document(userId).get().await()
            val productDoc = firestore.collection("products").document(productRef.id).get().await()

            if (userDoc.exists() && productDoc.exists()) {
                addResult("• ✅ Data verified in Firestore")
                addResult("•    - User: ${userDoc.getString("name")}")
                addResult("•    - Product: ${productDoc.getString("title")}")
                true
            } else {
                addResult("• ❌ Data verification failed")
                false
            }
        } catch (e: Exception) {
            addResult("• ❌ Firestore error: ${e.message}")
            false
        }
    }

    suspend fun testCloudinaryUpload(imageUri: Uri): Boolean {
        return try {
            testStep = "Testing Cloudinary Image Upload..."

            addResult("• ☁️ Uploading image to Cloudinary...")

            val imageUrl = CloudinaryManager.uploadImage(
                context = context,
                imageUri = imageUri,
                folder = "craftoria/test"
            )

            uploadedImageUrl = imageUrl
            addResult("• ✅ Image uploaded successfully!")
            addResult("• 🔗 URL: $imageUrl")

            val firestore = Firebase.firestore
            val userId = Firebase.auth.currentUser?.uid ?: "unknown"

            val imageData = mapOf(
                "imageUrl" to imageUrl,
                "uploadedBy" to userId,
                "uploadedAt" to System.currentTimeMillis(),
                "testData" to true
            )

            addResult("• 💾 Storing image URL in Firestore...")
            val imageRef = firestore.collection("uploaded_images")
                .add(imageData)
                .await()

            addResult("• ✅ Image URL stored: ${imageRef.id}")

            true
        } catch (e: Exception) {
            addResult("• ❌ Cloudinary error: ${e.message}")
            false
        }
    }

    fun runCompleteTest() {
        scope.launch {
            isLoading = true
            testResults = listOf()

            try {
                addResult("• 🚀 Starting Complete Integration Test...")
                addResult("• " + "=".repeat(40))

                val authSuccess = testAuthentication()
                if (!authSuccess) {
                    addResult("• ❌ Test stopped: Authentication failed")
                    isLoading = false
                    return@launch
                }

                val userId = Firebase.auth.currentUser?.uid ?: ""
                val firestoreSuccess = testFirestoreStorage(userId)
                if (!firestoreSuccess) {
                    addResult("• ❌ Test stopped: Firestore failed")
                    isLoading = false
                    return@launch
                }

                if (selectedImageUri != null) {
                    testCloudinaryUpload(selectedImageUri!!)
                } else {
                    addResult("• ⚠️ No image selected, skipping Cloudinary")
                }

                addResult("• " + "=".repeat(40))
                addResult("• 🎉 COMPLETE TEST FINISHED!")
                addResult("• ")
                addResult("• ✅ Firebase Authentication: WORKING")
                addResult("• ✅ Firestore Database: WORKING")
                addResult(
                    if (uploadedImageUrl != null)
                        "• ✅ Cloudinary Storage: WORKING"
                    else
                        "• ⚠️ Cloudinary: NOT TESTED (no image)"
                )
                addResult("• ")
                addResult("• 📊 Check Firebase Console to verify!")

                testStep = "Test Complete!"

            } catch (e: Exception) {
                addResult("• ❌ Unexpected error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🧪 Integration Test",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Firebase + Firestore + Cloudinary",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📸 Optional: Select Image")

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedImageUri == null) "Select" else "Change")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { runCompleteTest() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Testing...")
            } else {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Run Test")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("📊 Status", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(testStep)
            }
        }

        if (testResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📝 Results",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.height(12.dp))

                    testResults.forEach { result ->
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (uploadedImageUrl != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "✅ Uploaded from Cloudinary",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Image(
                        painter = rememberAsyncImagePainter(uploadedImageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
