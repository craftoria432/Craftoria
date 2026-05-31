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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.services.ThemeInitializationService
import com.gcuf.craftoria.ui.navigation.NavGraph
import com.gcuf.craftoria.ui.theme.CraftoriaTheme
import com.gcuf.craftoria.ui.theme.ThemeManager
import com.gcuf.craftoria.BuildConfig
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var productsAdded = false
        private val lock = Any()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── Install OS splash screen FIRST — eliminates the blank white window ──
        // The splash stays visible until setContent renders its first frame.
        // This is the standard approach used by all major e-commerce apps.
        installSplashScreen()

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
        val themeManager = ThemeManager.getInstance()

        if (isFirebaseReady) {
            // ── Step 1: Apply cached theme SYNCHRONOUSLY so the first frame is correct ──
            // This eliminates the Rose-flash that happened while waiting for Firestore.
            val currentFirebaseUser = try { Firebase.auth.currentUser } catch (e: Exception) { null }
            if (currentFirebaseUser != null) {
                val cachedTheme = com.gcuf.craftoria.utils.ThemePreferenceCache
                    .getSavedTheme(applicationContext, currentFirebaseUser.uid)
                if (cachedTheme != null) {
                    themeManager.initializeTheme(cachedTheme)
                    Log.d("Craftoria", "✅ Theme pre-seeded from cache: ${cachedTheme.name}")
                }
            }

            // ── Step 2: Sync with Firebase in the background (keeps cache fresh) ──
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    withTimeoutOrNull(1000) {
                        withContext(Dispatchers.IO) {
                            val themeInitService = ThemeInitializationService(
                                firebaseAuth = Firebase.auth,
                                firestore = Firebase.firestore,
                                themeManager = themeManager,
                                context = applicationContext
                            )
                            themeInitService.initializeTheme()
                        }
                    }
                    Log.d("Craftoria", "✅ Theme synced with Firebase on startup")
                } catch (e: Exception) {
                    Log.e("Craftoria", "❌ Theme sync failed, using cached/default", e)
                }
            }

            // ─────────────────────────────────────────────
            // ⭐ PAYMENT DATA MIGRATION (One-time fix)
            // ─────────────────────────────────────────────
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    val migrationDone = prefs.getBoolean("payment_coseller_fix_applied", false)
                    
                    if (!migrationDone) {
                        Log.d("Craftoria", "🔄 Running payment co-seller store ID fix...")
                        val result = com.gcuf.craftoria.utils.PaymentDataMigration.fixCoSellerStoreIdField()
                        result.onSuccess { count ->
                            Log.d("Craftoria", "✅ Fixed $count payments")
                            prefs.edit { putBoolean("payment_coseller_fix_applied", true) }
                        }.onFailure { e ->
                            Log.e("Craftoria", "❌ Payment fix failed", e)
                        }
                    } else {
                        Log.d("Craftoria", "ℹ️ Payment co-seller fix already applied")
                    }
                } catch (e: Exception) {
                    Log.e("Craftoria", "❌ Payment migration error", e)
                }
            }

            // ─────────────────────────────────────────────
            // ⭐ ORDER CO-SELLER STORE ID MIGRATION (One-time fix)
            // ─────────────────────────────────────────────
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    val orderMigrationDone = prefs.getBoolean("order_coseller_store_id_migrated", false)
                    
                    if (!orderMigrationDone) {
                        Log.d("Craftoria", "🔄 Running order co-seller store ID migration...")
                        val result = com.gcuf.craftoria.utils.PaymentDataMigration.migrateOrderCoSellerStoreIds()
                        result.onSuccess { count ->
                            Log.d("Craftoria", "✅ Migrated $count orders with co-seller store IDs")
                            prefs.edit { putBoolean("order_coseller_store_id_migrated", true) }
                        }.onFailure { e ->
                            Log.e("Craftoria", "❌ Order migration failed", e)
                        }
                    } else {
                        Log.d("Craftoria", "ℹ️ Order co-seller store ID migration already applied")
                    }
                } catch (e: Exception) {
                    Log.e("Craftoria", "❌ Order migration error", e)
                }
            }

            // ─────────────────────────────────────────────
            // ⭐ PAYMENT SPLIT MIGRATIONS (One-time setup)
            // ─────────────────────────────────────────────
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("Craftoria", "🔄 Running payment split migrations...")
                    
                    // 1. Migrate existing payments to have payment_splits array
                    com.gcuf.craftoria.utils.PaymentDataMigration.migrateExistingPayments()
                    Log.d("Craftoria", "✅ Existing payments migrated")
                    
                    // 2. Populate payment_split_config for all stores
                    com.gcuf.craftoria.utils.PaymentDataMigration.migrateStorePaymentSplits()
                    Log.d("Craftoria", "✅ Store payment splits configured")
                    
                } catch (e: Exception) {
                    Log.e("Craftoria", "❌ Payment split migration error", e)
                }
            }

            // ─────────────────────────────────────────────
            // ⭐ REFUND STATUS MIGRATION (One-time fix)
            // ─────────────────────────────────────────────
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("Craftoria", "🔄 Running refund status migration...")
                    val success = com.gcuf.craftoria.utils.RefundStatusMigration.migrateOldRefunds(
                        context = applicationContext,
                        firestore = Firebase.firestore
                    )
                    if (success) {
                        Log.d("Craftoria", "✅ Refund status migration completed")
                    } else {
                        Log.w("Craftoria", "⚠️ Refund status migration had some failures")
                    }
                } catch (e: Exception) {
                    Log.e("Craftoria", "❌ Refund status migration error", e)
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

                // ✅ FIX: Removed automatic sample product creation
                // Sellers must manually add their own products
                Log.d("Craftoria", "ℹ️ User logged in - sellers should add products manually")

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
