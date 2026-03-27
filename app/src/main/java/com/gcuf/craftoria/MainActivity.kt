package com.gcuf.craftoria

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.services.ThemeInitializationService
import com.gcuf.craftoria.ui.navigation.NavGraph
import com.gcuf.craftoria.ui.theme.CraftoriaTheme
import com.gcuf.craftoria.ui.theme.ThemeManager
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.utils.SampleDataHelper
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var productsAdded = false
        private val lock = Any()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ═══════════════════════════════════════════════════════════════
        // 🔧 RESET FLAGS - ENABLED FOR FRESH START
        // ═══════════════════════════════════════════════════════════════

        // val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        //  prefs.edit().putBoolean("sample_products_added", false).apply()
       // prefs.edit().putBoolean("seller_activities_added", false).apply()
       //  Log.d("Craftoria", "⚠️ RESET: Flags cleared - Fresh data will be added")


        // ─────────────────────────────────────────────
        // ⭐ CLOUDINARY INIT
        // ─────────────────────────────────────────────
        try {
            CloudinaryManager.initialize(applicationContext)
            Log.d("Craftoria", "✅ Cloudinary initialized")
        } catch (e: Exception) {
            Log.e("Craftoria", "❌ Cloudinary init failed", e)
            // Don't crash the app if Cloudinary fails
        }

        // ─────────────────────────────────────────────
        // ⭐ FIREBASE INIT
        // ─────────────────────────────────────────────
        val isFirebaseReady = try {
            // If google-services.json is missing/misconfigured, auto-init may fail and any Firebase usage can crash.
            FirebaseApp.initializeApp(applicationContext) != null
        } catch (e: Exception) {
            Log.e("Craftoria", "❌ FirebaseApp.initializeApp failed", e)
            false
        }

        if (isFirebaseReady) {
            try {
                Firebase.auth
                Log.d("Craftoria", "✅ Firebase initialized")
            } catch (e: Exception) {
                Log.e("Craftoria", "❌ Firebase auth init failed", e)
            }
        } else {
            Log.e("Craftoria", "❌ Firebase not ready (missing google-services.json?)")
        }

        // ─────────────────────────────────────────────
        // ⭐ THEME INITIALIZATION
        // ─────────────────────────────────────────────
        if (isFirebaseReady) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val themeManager = ThemeManager.getInstance()
                    val themeInitService = ThemeInitializationService(
                        firebaseAuth = Firebase.auth,
                        firestore = Firebase.firestore,
                        themeManager = themeManager
                    )
                    themeInitService.initializeTheme()
                    Log.d("Craftoria", "✅ Theme initialized on app startup")
                } catch (e: Exception) {
                    Log.e("Craftoria", "❌ Theme initialization failed", e)
                }
            }
        }

        // ─────────────────────────────────────────────
        // ⭐ COMPOSE UI
        // ─────────────────────────────────────────────
        try {
            setContent {
                CraftoriaTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (isFirebaseReady) {
                            val authViewModel: AuthViewModel = viewModel()
                            NavGraph(authViewModel = authViewModel)
                        } else {
                            FirebaseNotConfiguredScreen()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Craftoria", "❌ Compose UI initialization failed", e)
            // Fallback or crash gracefully
        }


        // Seed learning data (one-time, comment out after first run)
       // lifecycleScope.launch {
       // LearningDataSeeder.seedInitialData() }


        // ═══════════════════════════════════════════════════════════════
        // 🛒 SELLER PRODUCTS + ACTIVITIES SETUP (Runs after login)
        // ═══════════════════════════════════════════════════════════════

        CoroutineScope(Dispatchers.IO).launch {
            delay(3000) // Wait for authentication

            if (!isFirebaseReady) {
                Log.w("Craftoria", "⚠️ Firebase not ready - skipping seller setup")
                return@launch
            }

            val currentUser = try {
                Firebase.auth.currentUser
            } catch (e: Exception) {
                Log.e("Craftoria", "❌ Firebase auth unavailable", e)
                null
            }

            if (currentUser != null) {
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

                // ─────────────────────────────────────────
                // Step 1: Add Products (Once)
                // ─────────────────────────────────────────
                val productsAdded = prefs.getBoolean("sample_products_added", false)
                if (!productsAdded) {
                    Log.d("Craftoria", "")
                    Log.d("Craftoria", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d("Craftoria", "🛒 ADDING SELLER PRODUCTS")
                    Log.d("Craftoria", "   Seller: ${currentUser.displayName}")
                    Log.d("Craftoria", "   ID: ${currentUser.uid}")
                    Log.d("Craftoria", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    try {
                        SampleDataHelper.addSampleProducts()
                        prefs.edit { putBoolean("sample_products_added", true) }
                        Log.d("Craftoria", "✅ 4 Products added successfully")
                    } catch (e: Exception) {
                        Log.e("Craftoria", "❌ Failed to add products", e)
                    }
                } else {
                    Log.d("Craftoria", "ℹ️ Products already exist")
                }

                // ─────────────────────────────────────────
                // Step 2: Add Activities (Once)
                // ─────────────────────────────────────────
                /*   val activitiesAdded = prefs.getBoolean("seller_activities_added", false)
                if (!activitiesAdded) {
                    Log.d("Craftoria", "")
                    Log.d("Craftoria", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d("Craftoria", "📊 ADDING SELLER ACTIVITIES")
                    Log.d("Craftoria", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    try {
                        DashboardDataHelper.addSellerActivities(currentUser.uid)
                        prefs.edit { putBoolean("seller_activities_added", true) }
                        Log.d("Craftoria", "✅ 8 Activities added successfully")
                    } catch (e: Exception) {
                        Log.e("Craftoria", "❌ Failed to add activities", e)
                    }
                } else {
                    Log.d("Craftoria", "ℹ️ Activities already exist")
                }

                Log.d("Craftoria", "")
                Log.d("Craftoria", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d("Craftoria", "✅ SETUP COMPLETE!")
                Log.d("Craftoria", "   → 4 Products added")
                Log.d("Craftoria", "   → 8 Activities added")
                Log.d("Craftoria", "   → Dashboard ready to use")
                Log.d("Craftoria", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                */
            } else {
                Log.w("Craftoria", "⚠️ No user logged in - skipping seller setup")
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 📌 HOW IT WORKS:
        // ═══════════════════════════════════════════════════════════════
        //
        // 1️⃣ SELLER LOGIN (First Time):
        //    → 4 products add (YOUR products with YOUR seller_id)
        //    → 8 activities add
        //    → Dashboard shows: 4 products, PKR 0 sales
        //
        // 2️⃣ BUYER LOGIN:
        //    → Home: Shows YOUR 4 products
        //    → Can add to cart, checkout normally
        //
        // 3️⃣ BUYER PLACES ORDER:
        //    → Order created with YOUR seller_id
        //    → Order automatically appears in YOUR dashboard
        //
        // 4️⃣ SELLER DASHBOARD (YOU):
        //    → Sales stats update from real buyer orders
        //    → No fake/test data needed!
        //
        // ═══════════════════════════════════════════════════════════════

        // 📌 AFTER FIRST RUN: Comment out the reset flags at the top!
        // ═══════════════════════════════════════════════════════════════
    }
}

@Composable
private fun FirebaseNotConfiguredScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Firebase is not configured",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Add a valid google-services.json in the app module and rebuild.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
