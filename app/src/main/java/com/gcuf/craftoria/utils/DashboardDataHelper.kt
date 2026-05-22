package com.gcuf.craftoria.utils

import android.util.Log
import com.gcuf.craftoria.data.model.Activity
import com.gcuf.craftoria.data.model.ActivityType
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.toMap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

object DashboardDataHelper {

    /**
     * Creates a new activity with proper type mapping for consistent icon display
     */
    fun createActivity(
        sellerId: String,
        type: ActivityType,
        title: String,
        description: String,
        orderId: String = "",
        productId: String = "",
        timestamp: Any? = null
    ): Activity {
        return Activity(
            sellerId = sellerId,
            type = type.toString(),
            title = title,
            description = description,
            orderId = orderId,
            productId = productId,
            timestamp = timestamp ?: com.google.firebase.Timestamp.now()
        )
    }

    /**
     * Helper function to create common activity types with predefined titles
     */
    fun createOrderActivity(
        sellerId: String,
        orderId: String,
        activityType: ActivityType,
        productName: String = "",
        customDescription: String = ""
    ): Activity {
        val (title, description) = when (activityType) {
            ActivityType.NEW_ORDER -> Pair(
                "New Order",
                if (customDescription.isNotEmpty()) customDescription 
                else "Order #$orderId${if (productName.isNotEmpty()) " - $productName" else ""}"
            )
            ActivityType.ORDER_SHIPPED -> Pair(
                "Order Shipped",
                if (customDescription.isNotEmpty()) customDescription 
                else "Order #$orderId has been shipped via TCS"
            )
            ActivityType.ORDER_DELIVERED -> Pair(
                "Order Delivered",
                if (customDescription.isNotEmpty()) customDescription 
                else "Order #$orderId has been successfully delivered"
            )
            ActivityType.ORDER_PROCESSING -> Pair(
                "Order Processing",
                if (customDescription.isNotEmpty()) customDescription 
                else "Order #$orderId is now being processed"
            )
            else -> Pair(
                activityType.toString().replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                customDescription.ifEmpty { "Order #$orderId" }
            )
        }

        return createActivity(
            sellerId = sellerId,
            type = activityType,
            title = title,
            description = description,
            orderId = orderId
        )
    }

    /**
     * Helper function to create product-related activities
     */
    fun createProductActivity(
        sellerId: String,
        productId: String,
        activityType: ActivityType,
        productName: String,
        customDescription: String = ""
    ): Activity {
        val (title, description) = when (activityType) {
            ActivityType.PRODUCT_ADDED -> Pair(
                "Product Added",
                if (customDescription.isNotEmpty()) customDescription 
                else "Added $productName to your store"
            )
            ActivityType.PRODUCT_UPDATED -> Pair(
                "Product Updated",
                if (customDescription.isNotEmpty()) customDescription 
                else "$productName has been updated"
            )
            ActivityType.PRODUCT_APPROVED -> Pair(
                "Product Approved",
                if (customDescription.isNotEmpty()) customDescription 
                else "$productName has been approved and is now live"
            )
            ActivityType.LOW_STOCK_ALERT -> Pair(
                "Low Stock Alert",
                if (customDescription.isNotEmpty()) customDescription 
                else "$productName is running low on stock"
            )
            else -> Pair(
                activityType.toString().replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                customDescription.ifEmpty { productName }
            )
        }

        return createActivity(
            sellerId = sellerId,
            type = activityType,
            title = title,
            description = description,
            productId = productId
        )
    }

    /**
     * Saves an activity to Firestore
     */
    suspend fun saveActivity(activity: Activity): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val activityData = mapOf(
                "seller_id" to activity.sellerId,
                "type" to activity.type,
                "title" to activity.title,
                "description" to activity.description,
                "timestamp" to (activity.timestamp ?: com.google.firebase.Timestamp.now()),
                "order_id" to activity.orderId,
                "product_id" to activity.productId
            )

            db.collection("activities").add(activityData).await()
            Log.d("DashboardData", "✅ Activity saved: ${activity.title}")
            true
        } catch (e: Exception) {
            Log.e("DashboardData", "❌ Failed to save activity: ${activity.title}", e)
            false
        }
    }

    /**
     * Adds sample products for a specific seller to test the dashboard
     */
    suspend fun addSellerProducts(
        sellerId: String,
        sellerName: String,
        isVerified: Boolean = true
    ) {
        val db = FirebaseFirestore.getInstance()

        val sellerProducts = listOf(
            // High-performing product
            Product(
                title = "Premium Handwoven Silk Scarf",
                description = "Luxurious handwoven silk scarf with traditional motifs. Each piece takes 3 days to complete. Perfect for formal occasions or as a thoughtful gift.",
                price = 3500.0,
                category = "Textiles",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "January 2024",
                isNegotiable = true,
                minimumPrice = 3000.0,      // 14% off minimum
                autoAcceptPrice = 3150.0,   // 10% off auto-accept
                stock = 15,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1601924994987-69e26d50dc26"
                ),
                specifications = mapOf(
                    "Material" to "100% Silk",
                    "Size" to "72 x 28 inches",
                    "Pattern" to "Traditional Paisley",
                    "Care" to "Dry clean only",
                    "Handmade" to "Yes"
                )
            ),

            // Medium stock item
            Product(
                title = "Handcrafted Leather Wallet",
                description = "Genuine leather wallet with multiple card slots and coin pocket. Hand-stitched with premium thread for durability.",
                price = 1800.0,
                category = "Accessories",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "January 2024",
                isNegotiable = true,
                minimumPrice = 1500.0,
                autoAcceptPrice = 1620.0,
                stock = 8,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1627123424574-724758594e93"
                ),
                specifications = mapOf(
                    "Material" to "Genuine Leather",
                    "Color" to "Brown",
                    "Dimensions" to "4.5 x 3.5 inches",
                    "Card Slots" to "8",
                    "Warranty" to "1 Year"
                )
            ),

            // Low stock alert item
            Product(
                title = "Artisan Clay Dinner Plate Set",
                description = "Set of 6 handmade clay dinner plates with unique glaze patterns. Microwave and dishwasher safe. Each plate is one-of-a-kind.",
                price = 4500.0,
                category = "Home Décor",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "January 2024",
                isNegotiable = false,
                minimumPrice = 4500.0,
                autoAcceptPrice = 4500.0,
                stock = 2, // Low stock!
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61"
                ),
                specifications = mapOf(
                    "Material" to "Ceramic Clay",
                    "Set Includes" to "6 Plates",
                    "Diameter" to "10 inches",
                    "Microwave Safe" to "Yes",
                    "Dishwasher Safe" to "Yes"
                )
            ),

            // Popular negotiable item
            Product(
                title = "Hand-Embroidered Cushion Covers (Pair)",
                description = "Beautifully hand-embroidered cushion covers featuring floral designs. Sold as a pair. Adds charm to any living space.",
                price = 1200.0,
                category = "Textiles",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "January 2024",
                isNegotiable = true,
                minimumPrice = 1000.0,
                autoAcceptPrice = 1080.0,
                stock = 20,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1555041469-a586c61ea9bc"
                ),
                specifications = mapOf(
                    "Material" to "Cotton Blend",
                    "Size" to "18 x 18 inches",
                    "Pattern" to "Floral",
                    "Quantity" to "2 pieces",
                    "Zipper" to "Hidden"
                )
            ),

            // New product (just added)
            Product(
                title = "Handmade Macramé Wall Hanging",
                description = "Boho-style macramé wall hanging perfect for modern interiors. Made with natural cotton rope. Great for bedrooms or living rooms.",
                price = 2200.0,
                category = "Home Décor",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "January 2024",
                isNegotiable = true,
                minimumPrice = 1800.0,
                autoAcceptPrice = 1980.0,
                stock = 5,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1595815771614-ade9d652a65d"
                ),
                specifications = mapOf(
                    "Material" to "Cotton Rope",
                    "Size" to "36 x 24 inches",
                    "Style" to "Bohemian",
                    "Color" to "Natural Beige",
                    "Hanging Rod" to "Included"
                )
            ),

            // High-value item
            Product(
                title = "Traditional Brass Tea Set",
                description = "Authentic brass tea set with intricate engravings. Includes teapot, 6 cups, and serving tray. Perfect for traditional tea ceremonies.",
                price = 8500.0,
                category = "Handicrafts",
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = isVerified,
                sellerMemberSince = "January 2024",
                isNegotiable = true,
                minimumPrice = 7500.0,
                autoAcceptPrice = 7650.0,
                stock = 3,
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1564890369478-c89ca6d9cde9"
                ),
                specifications = mapOf(
                    "Material" to "Solid Brass",
                    "Set Includes" to "Teapot + 6 Cups + Tray",
                    "Finish" to "Polished",
                    "Engravings" to "Hand-carved",
                    "Maintenance" to "Polish regularly"
                )
            )
        )

        try {
            sellerProducts.forEach { product ->
                db.collection("products").add(product.toMap()).await()
                Log.d("DashboardData", "✅ Added seller product: ${product.title}")
            }
            Log.d("DashboardData", "✅ All ${sellerProducts.size} seller products added successfully")
        } catch (e: Exception) {
            Log.e("DashboardData", "❌ Failed to add seller products", e)
        }
    }

    /**
     * Adds sample activities for the seller dashboard with enhanced icons
     */
    suspend fun addSellerActivities(sellerId: String) {
        val db = FirebaseFirestore.getInstance()

        // Create activities using the helper functions for consistency
        val sampleActivities = listOf(
            // Recent orders (Blue - Shopping Cart 🛒)
            createOrderActivity(
                sellerId = sellerId,
                orderId = "ORD12350",
                activityType = ActivityType.NEW_ORDER,
                productName = "Premium Handwoven Silk Scarf"
            ),
            createOrderActivity(
                sellerId = sellerId,
                orderId = "ORD12349",
                activityType = ActivityType.NEW_ORDER,
                productName = "Handcrafted Leather Wallet"
            ),
            
            // Product activities (Green - Add Box ➕)
            createProductActivity(
                sellerId = sellerId,
                productId = "PROD_NEW_123",
                activityType = ActivityType.PRODUCT_ADDED,
                productName = "Handmade Macramé Wall Hanging"
            ),
            createProductActivity(
                sellerId = sellerId,
                productId = "PROD456",
                activityType = ActivityType.PRODUCT_UPDATED,
                productName = "Hand-Embroidered Cushion Covers",
                customDescription = "Stock quantity updated to 20 pieces"
            ),
            
            // Shipping activities (Purple - Local Shipping 📦)
            createOrderActivity(
                sellerId = sellerId,
                orderId = "ORD12348",
                activityType = ActivityType.ORDER_SHIPPED,
                customDescription = "Order for Hand Made Painting has been shipped via TCS"
            ),
            createOrderActivity(
                sellerId = sellerId,
                orderId = "ORD12347",
                activityType = ActivityType.ORDER_DELIVERED,
                customDescription = "Order for Hand Made Painting has been successfully delivered"
            ),
            createOrderActivity(
                sellerId = sellerId,
                orderId = "ORD12346",
                activityType = ActivityType.ORDER_PROCESSING,
                customDescription = "Order for Hand Made Painting is now being processed"
            ),
            
            // More orders
            createOrderActivity(
                sellerId = sellerId,
                orderId = "ORD12345",
                activityType = ActivityType.NEW_ORDER,
                productName = "Traditional Brass Tea Set"
            ),
            
            // System activities (Gray - Info ℹ️)
            createActivity(
                sellerId = sellerId,
                type = ActivityType.ACCOUNT_VERIFIED,
                title = "Account Verified",
                description = "Your seller account has been successfully verified"
            ),
            
            // Stock alerts (Orange - Info ℹ️)
            createProductActivity(
                sellerId = sellerId,
                productId = "PROD789",
                activityType = ActivityType.LOW_STOCK_ALERT,
                productName = "Artisan Clay Dinner Plate Set",
                customDescription = "Only 2 items remaining in stock"
            )
        )

        try {
            sampleActivities.forEach { activity ->
                val activityData = mapOf(
                    "seller_id" to activity.sellerId,
                    "type" to activity.type,
                    "title" to activity.title,
                    "description" to activity.description,
                    "timestamp" to (activity.timestamp ?: com.google.firebase.Timestamp.now()),
                    "order_id" to activity.orderId,
                    "product_id" to activity.productId
                )

                db.collection("activities").add(activityData).await()
            }

            Log.d("DashboardData", "✅ ${sampleActivities.size} enhanced activities added successfully")
        } catch (e: Exception) {
            Log.e("DashboardData", "❌ Failed to add activities", e)
        }
    }

    /**
     * Adds sample payment data for testing payment history
     */
    /**
     * ⚠️ DEPRECATED: This function should NOT be used in production.
     * It was causing deleted payments to reappear with new IDs.
     * Payments should only be created from actual orders, not sample data.
     * 
     * Kept for reference only - do not call this function.
     */
    @Deprecated("Do not use - causes fake payments to reappear. Payments should only come from real orders.")
    suspend fun addSamplePaymentData(sellerId: String, sellerName: String) {
        val db = FirebaseFirestore.getInstance()
        
        try {
            Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d("DashboardData", "💳 Adding sample payment data for: $sellerName")
            Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            val samplePayments = listOf(
                // Completed payments
                mapOf(
                    "id" to "PAY001",
                    "seller_id" to sellerId,
                    "seller_name" to sellerName,
                    "order_id" to "ORD12350",
                    "co_seller_store_id" to sellerId,
                    "store_name" to sellerName,
                    "buyer_id" to "buyer123",
                    "buyer_name" to "Ahmad Ali",
                    "amount" to 3500.0,
                    "payment_method" to "Credit Card",
                    "status" to "COMPLETED",
                    "items_count" to 1,
                    "items_details" to listOf(
                        mapOf(
                            "product_id" to "PROD001",
                            "product_title" to "Premium Handwoven Silk Scarf",
                            "quantity" to 1,
                            "price" to 3500.0,
                            "item_total" to 3500.0
                        )
                    ),
                    "created_at" to (System.currentTimeMillis() - 86400000 * 2), // 2 days ago
                    "updated_at" to (System.currentTimeMillis() - 86400000 * 1), // 1 day ago
                    "payment_date" to (System.currentTimeMillis() - 86400000 * 1),
                    "transaction_id" to "TXN001",
                    "involved_seller_ids" to listOf(sellerId)
                ),
                mapOf(
                    "id" to "PAY002",
                    "seller_id" to sellerId,
                    "seller_name" to sellerName,
                    "order_id" to "ORD12349",
                    "co_seller_store_id" to sellerId,
                    "store_name" to sellerName,
                    "buyer_id" to "buyer456",
                    "buyer_name" to "Fatima Khan",
                    "amount" to 1800.0,
                    "payment_method" to "JazzCash",
                    "status" to "COMPLETED",
                    "items_count" to 1,
                    "items_details" to listOf(
                        mapOf(
                            "product_id" to "PROD002",
                            "product_title" to "Handcrafted Leather Wallet",
                            "quantity" to 1,
                            "price" to 1800.0,
                            "item_total" to 1800.0
                        )
                    ),
                    "created_at" to (System.currentTimeMillis() - 86400000 * 5), // 5 days ago
                    "updated_at" to (System.currentTimeMillis() - 86400000 * 4), // 4 days ago
                    "payment_date" to (System.currentTimeMillis() - 86400000 * 4),
                    "transaction_id" to "TXN002",
                    "involved_seller_ids" to listOf(sellerId)
                ),
                mapOf(
                    "id" to "PAY003",
                    "seller_id" to sellerId,
                    "seller_name" to sellerName,
                    "order_id" to "ORD12348",
                    "co_seller_store_id" to sellerId,
                    "store_name" to sellerName,
                    "buyer_id" to "buyer789",
                    "buyer_name" to "Hassan Ahmed",
                    "amount" to 2400.0,
                    "payment_method" to "EasyPaisa",
                    "status" to "COMPLETED",
                    "items_count" to 2,
                    "items_details" to listOf(
                        mapOf(
                            "product_id" to "PROD003",
                            "product_title" to "Hand-Embroidered Cushion Covers",
                            "quantity" to 2,
                            "price" to 1200.0,
                            "item_total" to 2400.0
                        )
                    ),
                    "created_at" to (System.currentTimeMillis() - 86400000 * 7), // 1 week ago
                    "updated_at" to (System.currentTimeMillis() - 86400000 * 6), // 6 days ago
                    "payment_date" to (System.currentTimeMillis() - 86400000 * 6),
                    "transaction_id" to "TXN003",
                    "involved_seller_ids" to listOf(sellerId)
                ),
                
                // Pending payments
                mapOf(
                    "id" to "PAY004",
                    "seller_id" to sellerId,
                    "seller_name" to sellerName,
                    "order_id" to "ORD12347",
                    "co_seller_store_id" to sellerId,
                    "store_name" to sellerName,
                    "buyer_id" to "buyer101",
                    "buyer_name" to "Zara Sheikh",
                    "amount" to 4500.0,
                    "payment_method" to "Bank Transfer",
                    "status" to "PENDING",
                    "items_count" to 1,
                    "items_details" to listOf(
                        mapOf(
                            "product_id" to "PROD004",
                            "product_title" to "Artisan Clay Dinner Plate Set",
                            "quantity" to 1,
                            "price" to 4500.0,
                            "item_total" to 4500.0
                        )
                    ),
                    "created_at" to (System.currentTimeMillis() - 86400000 * 1), // 1 day ago
                    "updated_at" to (System.currentTimeMillis() - 86400000 * 1),
                    "involved_seller_ids" to listOf(sellerId)
                ),
                mapOf(
                    "id" to "PAY005",
                    "seller_id" to sellerId,
                    "seller_name" to sellerName,
                    "order_id" to "ORD12346",
                    "co_seller_store_id" to sellerId,
                    "store_name" to sellerName,
                    "buyer_id" to "buyer202",
                    "buyer_name" to "Omar Malik",
                    "amount" to 2200.0,
                    "payment_method" to "Credit Card",
                    "status" to "PENDING",
                    "items_count" to 1,
                    "items_details" to listOf(
                        mapOf(
                            "product_id" to "PROD005",
                            "product_title" to "Handmade Macramé Wall Hanging",
                            "quantity" to 1,
                            "price" to 2200.0,
                            "item_total" to 2200.0
                        )
                    ),
                    "created_at" to System.currentTimeMillis(), // Today
                    "updated_at" to System.currentTimeMillis(),
                    "involved_seller_ids" to listOf(sellerId)
                )
            )

            samplePayments.forEach { paymentData ->
                db.collection("seller_payments").add(paymentData).await()
                Log.d("DashboardData", "✅ Added payment: ${paymentData["order_id"]} - PKR ${paymentData["amount"]}")
            }

            Log.d("DashboardData", "✅ All ${samplePayments.size} sample payments added successfully")
            Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e("DashboardData", "❌ Failed to add sample payment data", e)
        }
    }
    /**
     * Convenience function to add both products and activities
     * ⚠️ NOTE: Removed addSamplePaymentData() call - payments should only come from real orders
     */
    suspend fun setupSellerDashboard(
        sellerId: String,
        sellerName: String,
        isVerified: Boolean = true
    ) {
        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("DashboardData", "🚀 Setting up dashboard for: $sellerName")
        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        addSellerProducts(sellerId, sellerName, isVerified)
        addSellerActivities(sellerId)
        // ✅ REMOVED: addSamplePaymentData(sellerId, sellerName) - payments should only come from real orders

        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("DashboardData", "✅ Dashboard setup completed!")
        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * ⚠️ DEPRECATED: This function should NOT be used in production.
     * It was causing deleted payments to reappear with new IDs.
     * Payments should only be created from actual orders, not sample data.
     * 
     * Kept for reference only - do not call this function.
     */
    @Deprecated("Do not use - causes fake payments to reappear. Payments should only come from real orders.")
    suspend fun setupPaymentDataOnly(sellerId: String, sellerName: String) {
        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("DashboardData", "💳 Setting up payment data for: $sellerName")
        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        addSamplePaymentData(sellerId, sellerName)

        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("DashboardData", "✅ Payment data setup completed!")
        Log.d("DashboardData", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}