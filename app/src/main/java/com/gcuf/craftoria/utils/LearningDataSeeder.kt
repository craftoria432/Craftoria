package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.LearningCategory
import com.gcuf.craftoria.data.model.Tutorial
import com.gcuf.craftoria.data.model.toMap
import kotlinx.coroutines.tasks.await

object LearningDataSeeder {
    private const val TAG = "LearningDataSeeder"

    suspend fun seedInitialData() {
        val db = FirebaseFirestore.getInstance()

        // Check if data already exists
        val existing = db.collection("learning_categories").limit(1).get().await()
        if (!existing.isEmpty) {
            Log.d(TAG, "Learning data already exists, skipping seed")
            return
        }

        val categories = listOf(
            LearningCategory(
                id = "getting_started",
                title = "Getting Started",
                description = "Essential basics to set up your seller journey on Craftoria.",
                icon = "school",
                displayOrder = 1,
                tutorials = listOf(
                    Tutorial(
                        id = "101",
                        title = "How to Create Your First Product Listing",
                        description = "Step-by-step guide to listing your first handicraft product on Craftoria marketplace.",
                        duration = "5 min read",
                        icon = "article",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "getting_started",
                        isVideo = false
                    ),
                    Tutorial(
                        id = "102",
                        title = "Understanding the Craftoria Marketplace",
                        description = "Learn how the marketplace works and how to navigate all features effectively.",
                        duration = "7 min read",
                        icon = "library",
                        url = "https://example.com/craftoria-guide",
                        categoryId = "getting_started",
                        isVideo = false
                    ),
                    Tutorial(
                        id = "103",
                        title = "Setting Up Your Seller Profile",
                        description = "Complete guide to creating an attractive and professional seller profile.",
                        duration = "4 min read",
                        icon = "favorite",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "getting_started",
                        isVideo = true
                    )
                )
            ),
            LearningCategory(
                id = "product_photography",
                title = "Product Photography",
                description = "Take better product photos to boost trust and conversions.",
                icon = "camera",
                displayOrder = 2,
                tutorials = listOf(
                    Tutorial(
                        id = "201",
                        title = "Taking Great Photos with Your Phone",
                        description = "Professional photography tips using just your smartphone camera.",
                        duration = "8 min video",
                        icon = "video",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "product_photography",
                        isVideo = true
                    ),
                    Tutorial(
                        id = "202",
                        title = "Lighting Tips for Handicrafts",
                        description = "Master natural and artificial lighting to showcase your products beautifully.",
                        duration = "6 min video",
                        icon = "lightbulb",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "product_photography",
                        isVideo = true
                    ),
                    Tutorial(
                        id = "203",
                        title = "Showcasing Product Details",
                        description = "Learn to capture close-ups and highlight the unique features of your crafts.",
                        duration = "5 min read",
                        icon = "idea",
                        url = "https://example.com/product-details",
                        categoryId = "product_photography",
                        isVideo = false
                    )
                )
            ),
            LearningCategory(
                id = "pricing_negotiation",
                title = "Pricing & Negotiation",
                description = "Price confidently and handle offers professionally.",
                icon = "handyman",
                displayOrder = 3,
                tutorials = listOf(
                    Tutorial(
                        id = "301",
                        title = "How to Price Your Handicrafts",
                        description = "Calculate fair pricing considering materials, time, and market value.",
                        duration = "10 min read",
                        icon = "article",
                        url = "https://example.com/pricing-guide",
                        categoryId = "pricing_negotiation",
                        isVideo = false
                    ),
                    Tutorial(
                        id = "302",
                        title = "Using the Smart Negotiation Feature",
                        description = "Master the negotiation tool to get the best deals while maintaining profits.",
                        duration = "4 min video",
                        icon = "video",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "pricing_negotiation",
                        isVideo = true
                    ),
                    Tutorial(
                        id = "303",
                        title = "Understanding Profit Margins",
                        description = "Learn to calculate and maintain healthy profit margins for sustainable business.",
                        duration = "7 min read",
                        icon = "article",
                        url = "https://example.com/profit-margins",
                        categoryId = "pricing_negotiation",
                        isVideo = false
                    )
                )
            ),
            LearningCategory(
                id = "marketing_sales",
                title = "Marketing & Sales",
                description = "Promote your products and convert more visitors into buyers.",
                icon = "star",
                displayOrder = 4,
                tutorials = listOf(
                    Tutorial(
                        id = "401",
                        title = "Writing Compelling Product Descriptions",
                        description = "Craft descriptions that attract buyers and highlight product benefits.",
                        duration = "6 min read",
                        icon = "article",
                        url = "https://example.com/product-descriptions",
                        categoryId = "marketing_sales",
                        isVideo = false
                    ),
                    Tutorial(
                        id = "402",
                        title = "Marketing Your Products on Social Media",
                        description = "Effective social media strategies to promote your handicrafts online.",
                        duration = "12 min video",
                        icon = "video",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "marketing_sales",
                        isVideo = true
                    ),
                    Tutorial(
                        id = "403",
                        title = "Engaging with Your Customers",
                        description = "Build lasting relationships through effective customer communication.",
                        duration = "5 min read",
                        icon = "favorite",
                        url = "https://example.com/customer-engagement",
                        categoryId = "marketing_sales",
                        isVideo = false
                    )
                )
            ),
            LearningCategory(
                id = "order_management",
                title = "Order Management",
                description = "Manage, ship, and deliver orders smoothly end-to-end.",
                icon = "layers",
                displayOrder = 5,
                tutorials = listOf(
                    Tutorial(
                        id = "501",
                        title = "Processing Orders Efficiently",
                        description = "Streamline your order workflow for faster processing and happier customers.",
                        duration = "8 min read",
                        icon = "article",
                        url = "https://example.com/order-processing",
                        categoryId = "order_management",
                        isVideo = false
                    ),
                    Tutorial(
                        id = "502",
                        title = "Packaging and Shipping Best Practices",
                        description = "Learn professional packaging techniques to protect products during delivery.",
                        duration = "10 min video",
                        icon = "video",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "order_management",
                        isVideo = true
                    ),
                    Tutorial(
                        id = "503",
                        title = "Handling Customer Questions",
                        description = "Professional tips for answering inquiries and resolving concerns quickly.",
                        duration = "6 min read",
                        icon = "article",
                        url = "https://example.com/customer-questions",
                        categoryId = "order_management",
                        isVideo = false
                    )
                )
            ),
            LearningCategory(
                id = "co_seller_stores",
                title = "Co-Seller Stores",
                description = "Collaborate with partners to run a shared store successfully.",
                icon = "category",
                displayOrder = 6,
                tutorials = listOf(
                    Tutorial(
                        id = "601",
                        title = "Creating a Co-Seller Store with Partners",
                        description = "Step-by-step guide to starting a collaborative shop with other sellers.",
                        duration = "9 min read",
                        icon = "library",
                        url = "https://example.com/co-seller-guide",
                        categoryId = "co_seller_stores",
                        isVideo = false
                    ),
                    Tutorial(
                        id = "602",
                        title = "Managing Collaborative Shops",
                        description = "Best practices for managing inventory, orders, and profits with partners.",
                        duration = "7 min video",
                        icon = "video",
                        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                        categoryId = "co_seller_stores",
                        isVideo = true
                    ),
                    Tutorial(
                        id = "603",
                        title = "Benefits of Team Selling",
                        description = "Discover how collaborative selling can expand reach and increase sales.",
                        duration = "5 min read",
                        icon = "star",
                        url = "https://example.com/team-selling",
                        categoryId = "co_seller_stores",
                        isVideo = false
                    )
                )
            )
        )

        try {
            categories.forEach { category ->
                db.collection("learning_categories")
                    .document(category.id)
                    .set(category.toMap())
                    .await()
            }
            Log.d(TAG, "Successfully seeded ${categories.size} learning categories")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed learning data", e)
        }
    }
}