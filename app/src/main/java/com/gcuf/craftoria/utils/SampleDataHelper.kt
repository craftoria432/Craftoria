package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.Product
import kotlinx.coroutines.tasks.await
import com.gcuf.craftoria.data.model.toMap
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object SampleDataHelper {

    /**
     * Adds sample products that belong to the CURRENT LOGGED-IN SELLER
     * These products will show on Home screen for buyers AND in seller's dashboard
     */
    suspend fun addSampleProducts() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = Firebase.auth.currentUser

        // Use current seller's ID
        val sellerId = currentUser?.uid ?: "seller1"
        val sellerName = currentUser?.displayName ?: "Test Seller"
        val isVerified = true

        Log.d("SampleData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("SampleData", "🛒 Adding products for seller: $sellerName")
        Log.d("SampleData", "   Seller ID: $sellerId")
        Log.d("SampleData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val sampleProducts = listOf(
            Product(
                title = "Handmade Embroidered Cushion Cover",
                description = "Beautiful handmade embroidered cushion cover crafted with premium cotton fabric. Perfect for adding a touch of elegance to your living room or bedroom. Each piece is handmade by artisans.",
                price = 850.0,
                category = "Textiles",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "March 2024",
                isNegotiable = true,
                minimumPrice = 700.0,       // 17% off minimum
                autoAcceptPrice = 765.0,    // 10% off auto-accept
                stock = 10,
                imageUrls = listOf(
                    "https://res.cloudinary.com/demo/image/upload/samples/ecommerce/accessories-bag.jpg"
                ),
                specifications = mapOf(
                    "Material" to "100% Cotton",
                    "Size" to "16 x 16 inches",
                    "Color" to "Multi-color",
                    "Care" to "Hand wash",
                    "Origin" to "Pakistan"
                )
            ),

            Product(
                title = "Traditional Beaded Necklace Set",
                description = "Elegant handmade beaded necklace with matching earrings. Crafted with colorful beads.",
                price = 1200.0,
                category = "Jewelry",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "March 2024",
                isNegotiable = true,
                minimumPrice = 1000.0,      // 17% off minimum
                autoAcceptPrice = 1080.0,   // 10% off auto-accept
                stock = 5,
                imageUrls = listOf(
                    "https://res.cloudinary.com/demo/image/upload/samples/ecommerce/leather-bag-gray.jpg"
                ),
                specifications = mapOf(
                    "Material" to "Glass beads",
                    "Color" to "Multicolor",
                    "Includes" to "Necklace + Earrings"
                )
            ),

            Product(
                title = "Hand-Painted Ceramic Vase",
                description = "Beautiful hand-painted ceramic vase with traditional floral arts.",
                price = 2500.0,
                category = "Home Décor",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "March 2024",
                isNegotiable = false,
                minimumPrice = 2500.0,
                autoAcceptPrice = 2500.0,
                stock = 3,
                imageUrls = listOf(
                    "https://res.cloudinary.com/demo/image/upload/samples/food/pot-mussels.jpg"
                ),
                specifications = mapOf(
                    "Height" to "12 inches",
                    "Material" to "Ceramic",
                    "Design" to "Hand-painted"
                )
            ),

            Product(
                title = "Handcrafted Wooden Jewelry Box",
                description = "Beautiful hand-carved wooden jewelry box with intricate details. Perfect for storing your precious jewelry and accessories.",
                price = 1800.0,
                category = "Handicrafts",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "March 2024",
                isNegotiable = true,
                minimumPrice = 1500.0,
                autoAcceptPrice = 1620.0,
                stock = 7,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1616627981490-8e3e92ce409a"
                ),
                specifications = mapOf(
                    "Material" to "Sheesham Wood",
                    "Finish" to "Polished",
                    "Dimensions" to "8 x 5 x 4 inches",
                    "Weight" to "450g",
                    "Handmade" to "Yes"
                )
            )
        )

        try {
            sampleProducts.forEach { product ->
                db.collection("products").add(product.toMap()).await()
                Log.d("SampleData", "✅ Added product: ${product.title}")
                Log.d("SampleData", "   Price: ${product.price}, AutoAccept: ${product.autoAcceptPrice}, Minimum: ${product.minimumPrice}")
            }
            Log.d("SampleData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d("SampleData", "✅ All ${sampleProducts.size} sample products added successfully")
            Log.d("SampleData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            Log.e("SampleData", "❌ Failed to add sample products", e)
        }
    }
}