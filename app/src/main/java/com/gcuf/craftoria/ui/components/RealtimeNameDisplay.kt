package com.gcuf.craftoria.ui.components

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Displays a user's name with real-time updates from Firestore
 * Falls back to the provided name if listener fails or user not found
 */
@Composable
fun RealtimeNameDisplay(
    userId: String,
    fallbackName: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    color: Color = Color.Black
) {
    var currentName by remember { mutableStateOf(fallbackName) }
    
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            try {
                val db = Firebase.firestore
                db.collection("users").document(userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists()) {
                            val name = snapshot.getString("name") ?: fallbackName
                            currentName = name
                            Log.d("RealtimeNameDisplay", "✅ Updated name for $userId: $name")
                        }
                    }
            } catch (e: Exception) {
                Log.e("RealtimeNameDisplay", "❌ Error listening to name: ${e.message}")
            }
        }
    }
    
    Text(
        text = currentName,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    )
}
