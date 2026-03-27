package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.NegotiationOffer
import com.gcuf.craftoria.data.model.NegotiationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NegotiationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _negotiationState = MutableStateFlow<NegotiationState>(NegotiationState.Idle)
    val negotiationState: StateFlow<NegotiationState> = _negotiationState.asStateFlow()

    fun submitOffer(
        productId: String,
        buyerId: String,
        sellerId: String,
        offerAmount: Double,
        originalPrice: Double,
        minimumPrice: Double,
        autoAcceptPrice: Double
    ) {
        viewModelScope.launch {
            try {
                _negotiationState.value = NegotiationState.Loading

                // ✅ DEBUG LOGGING
                Log.d("Negotiation", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d("Negotiation", "📊 NEGOTIATION REQUEST")
                Log.d("Negotiation", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d("Negotiation", "Product ID: $productId")
                Log.d("Negotiation", "Offer Amount: PKR $offerAmount")
                Log.d("Negotiation", "Original Price: PKR $originalPrice")
                Log.d("Negotiation", "Auto Accept Price: PKR $autoAcceptPrice")
                Log.d("Negotiation", "Minimum Price: PKR $minimumPrice")
                Log.d("Negotiation", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                when {

                    // 1️⃣ Offer ≥ Original Price → No need for negotiation
                    offerAmount >= originalPrice -> {
                        Log.d("Negotiation", "❌ RESULT: Offer >= Original Price")
                        Log.d("Negotiation", "   ($offerAmount >= $originalPrice)")
                        _negotiationState.value = NegotiationState.Error(
                            "Your offer is equal to or higher than the current price. You can buy it directly."
                        )
                    }

                    // 2️⃣ Offer ≥ autoAcceptPrice → Auto Accept
                    offerAmount >= autoAcceptPrice -> {
                        Log.d("Negotiation", "✅ RESULT: AUTO-ACCEPTED")
                        Log.d("Negotiation", "   ($offerAmount >= $autoAcceptPrice)")

                        val offer = NegotiationOffer(
                            productId = productId,
                            buyerId = buyerId,
                            sellerId = sellerId,
                            offerAmount = offerAmount,
                            originalPrice = originalPrice,
                            status = NegotiationStatus.AUTO_ACCEPTED,
                            message = "Automatically accepted by system"
                        )

                        val doc = db.collection("negotiations").add(offer).await()
                        Log.d("Negotiation", "   Saved to Firebase: ${doc.id}")

                        // Send notification to seller about auto-accepted offer
                        try {
                            val productDoc = db.collection("products").document(productId).get().await()
                            val productTitle = productDoc.getString("title") ?: "Product"
                            
                            val buyerDoc = db.collection("users").document(buyerId).get().await()
                            val buyerName = buyerDoc.getString("name") ?: "A buyer"
                            
                            val notificationData = mapOf(
                                "user_id" to sellerId,
                                "title" to "Offer Auto-Accepted",
                                "description" to "$buyerName's offer of PKR ${offerAmount.toInt()} for $productTitle was automatically accepted",
                                "category" to "ORDER",
                                "action_type" to "VIEW_PRODUCT",
                                "product_id" to productId,
                                "order_id" to "",
                                "store_id" to "",
                                "action_data" to mapOf("negotiation_id" to doc.id),
                                "is_read" to false,
                                "created_at" to System.currentTimeMillis()
                            )
                            
                            db.collection("notifications").add(notificationData).await()
                            Log.d("Negotiation", "   Notification sent to seller")
                        } catch (e: Exception) {
                            Log.e("Negotiation", "Failed to send notification", e)
                        }

                        _negotiationState.value = NegotiationState.AutoAccepted(
                            offerId = doc.id,
                            offerAmount = offerAmount,
                            savedAmount = originalPrice - offerAmount
                        )
                    }

                    // 3️⃣ Offer between minimumPrice and autoAcceptPrice → seller review
                    offerAmount >= minimumPrice && offerAmount < autoAcceptPrice -> {
                        Log.d("Negotiation", "⏳ RESULT: PENDING (Sent to Seller)")
                        Log.d("Negotiation", "   ($offerAmount >= $minimumPrice AND $offerAmount < $autoAcceptPrice)")

                        val offer = NegotiationOffer(
                            productId = productId,
                            buyerId = buyerId,
                            sellerId = sellerId,
                            offerAmount = offerAmount,
                            originalPrice = originalPrice,
                            status = NegotiationStatus.PENDING,
                            message = "Waiting for seller review"
                        )

                        val doc = db.collection("negotiations").add(offer).await()
                        Log.d("Negotiation", "   Saved to Firebase: ${doc.id}")

                        // Send notification to seller
                        try {
                            val productDoc = db.collection("products").document(productId).get().await()
                            val productTitle = productDoc.getString("title") ?: "Product"
                            
                            val buyerDoc = db.collection("users").document(buyerId).get().await()
                            val buyerName = buyerDoc.getString("name") ?: "A buyer"
                            
                            val notificationData = mapOf(
                                "user_id" to sellerId,
                                "title" to "New Price Offer",
                                "description" to "$buyerName offered PKR ${offerAmount.toInt()} for $productTitle",
                                "category" to "ORDER",
                                "action_type" to "VIEW_NEGOTIATION",
                                "product_id" to productId,
                                "order_id" to "",
                                "store_id" to "",
                                "action_data" to mapOf("negotiation_id" to doc.id),
                                "is_read" to false,
                                "created_at" to System.currentTimeMillis()
                            )
                            
                            db.collection("notifications").add(notificationData).await()
                            Log.d("Negotiation", "   Notification sent to seller")
                        } catch (e: Exception) {
                            Log.e("Negotiation", "Failed to send notification", e)
                        }

                        _negotiationState.value = NegotiationState.Pending(
                            offerId = doc.id,
                            offerAmount = offerAmount
                        )
                    }

                    // 4️⃣ Offer < minimumPrice → Below Minimum
                    else -> {
                        Log.d("Negotiation", "❌ RESULT: BELOW MINIMUM")
                        Log.d("Negotiation", "   Offer: $offerAmount")
                        Log.d("Negotiation", "   Minimum: $minimumPrice")
                        Log.d("Negotiation", "   Condition: $offerAmount < $minimumPrice = ${offerAmount < minimumPrice}")

                        _negotiationState.value = NegotiationState.BelowMinimum(
                            minimumPrice = minimumPrice
                        )
                    }
                }

                Log.d("Negotiation", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e("Negotiation", "❌ ERROR: ${e.message}", e)
                _negotiationState.value = NegotiationState.Error(
                    e.message ?: "Failed to submit offer"
                )
            }
        }
    }

    fun resetState() {
        _negotiationState.value = NegotiationState.Idle
        Log.d("Negotiation", "🔄 State reset to Idle")
    }
}

sealed class NegotiationState {
    object Idle : NegotiationState()
    object Loading : NegotiationState()
    data class AutoAccepted(
        val offerId: String,
        val offerAmount: Double,
        val savedAmount: Double
    ) : NegotiationState()
    data class Pending(
        val offerId: String,
        val offerAmount: Double
    ) : NegotiationState()
    data class BelowMinimum(val minimumPrice: Double) : NegotiationState()
    data class Error(val message: String) : NegotiationState()
}