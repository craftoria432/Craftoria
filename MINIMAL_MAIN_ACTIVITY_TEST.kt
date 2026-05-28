// Minimal MainActivity for testing - replace the current one temporarily

package com.gcuf.craftoria

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gcuf.craftoria.ui.theme.CraftoriaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("Craftoria", "MainActivity onCreate started")
        
        try {
            setContent {
                CraftoriaTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        TestScreen()
                    }
                }
            }
            Log.d("Craftoria", "MainActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e("Craftoria", "MainActivity onCreate failed", e)
        }
    }
}

@Composable
fun TestScreen() {
    Text(text = "App is working!")
}