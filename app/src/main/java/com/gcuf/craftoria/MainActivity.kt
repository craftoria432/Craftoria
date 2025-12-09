package com.gcuf.craftoria

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.ui.navigation.NavGraph
import com.gcuf.craftoria.ui.theme.CraftoriaTheme
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cloudinary initialization
        try {
            CloudinaryManager.initialize(applicationContext)
            Log.d("Craftoria", "Cloudinary initialized")
        } catch (e: Exception) {
            Log.e("Craftoria", "Cloudinary init failed", e)
        }

        // Firebase init
        Firebase.auth
        Firebase.firestore

        setContent {
            CraftoriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    NavGraph(authViewModel = authViewModel)
                }
            }
        }
    }
}
