package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.*
import kotlinx.coroutines.tasks.await

object TestOrderHelper {

    /**
     * Creates test orders for BUYERS (to show in My Orders screen)
     */
    suspend fun createBuyerTestOrders(userId: String, userName: String) {
        val db = FirebaseFirestore.getInstance()

        val testOrders = listOf(
            Order(
                id = "",
                buyerId = userId,
                buyerName = userName,
                sellerId = "seller1",
                sellerName = "Sarah Ahmed",
                productId = "product_1",
                productTitle = "Handmade Embroidered Cushion Cover",
                productImage = "https://res.cloudinary.com/demo/image/upload/samples/ecommerce/accessories-bag.jpg",
                quantity = 1,
                totalPrice = 1000.0,
                status = OrderStatus.PENDING.toString(),
                shippingAddress = "123 Test Street, Lahore",
                buyerPhone = "+92 300 1234567",
                items = listOf(
                    OrderItem(
                        productId = "product_1",
                        productTitle = "Handmade Embroidered Cushion Cover",
                        productImage = "https://res.cloudinary.com/demo/image/upload/samples/ecommerce/accessories-bag.jpg",
                        sellerName = "Sarah Ahmed",
                        quantity = 1,
                        price = 850.0,
                        isNegotiated = false
                    )
                ),
                subtotal = 850.0,
                shipping = 150.0,
                discount = 0.0,
                deliveryInfo = DeliveryInfo(
                    fullName = userName,
                    phoneNumber = "+92 300 1234567",
                    email = "test@example.com",
                    address = "123 Test Street",
                    city = "Lahore",
                    postalCode = "54000"
                ),
                paymentMethod = "Cash on Delivery",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            Order(
                id = "",
                buyerId = userId,
                buyerName = userName,
                sellerId = "seller2",
                sellerName = "Ayesha Khan",
                productId = "product_2",
                productTitle = "Traditional Beaded Necklace Set",
                productImage = "https://res.cloudinary.com/demo/image/upload/samples/ecommerce/leather-bag-gray.jpg",
                quantity = 1,
                totalPrice = 1350.0,
                status = OrderStatus.PROCESSING.toString(),
                shippingAddress = "123 Test Street, Lahore",
                buyerPhone = "+92 300 1234567",
                items = listOf(
                    OrderItem(
                        productId = "product_2",
                        productTitle = "Traditional Beaded Necklace Set",
                        productImage = "https://res.cloudinary.com/demo/image/upload/samples/ecommerce/leather-bag-gray.jpg",
                        sellerName = "Ayesha Khan",
                        quantity = 1,
                        price = 1200.0,
                        isNegotiated = false
                    )
                ),
                subtotal = 1200.0,
                shipping = 150.0,
                discount = 0.0,
                deliveryInfo = DeliveryInfo(
                    fullName = userName,
                    phoneNumber = "+92 300 1234567",
                    email = "test@example.com",
                    address = "123 Test Street",
                    city = "Lahore",
                    postalCode = "54000"
                ),
                paymentMethod = "Cash on Delivery",
                createdAt = System.currentTimeMillis() - 86400000,
                updatedAt = System.currentTimeMillis()
            )
        )

        try {
            testOrders.forEachIndexed { index, order ->
                val orderMap = order.toMap()
                val docRef = db.collection("orders").add(orderMap).await()
                Log.d("TestOrder", "✅ Buyer order ${index + 1} created: ${docRef.id}")
            }
        } catch (e: Exception) {
            Log.e("TestOrder", "❌ Failed to create buyer orders", e)
        }
    }

    /**
     * Creates test orders for SELLERS (to show in Dashboard stats and Seller Orders screen)
     * These orders are FROM buyers TO this seller
     */
    suspend fun createSellerTestOrders(sellerId: String, sellerName: String) {
        val db = FirebaseFirestore.getInstance()

        val buyerNames = listOf("Ahmed Khan", "Fatima Ali", "Hassan Ahmed", "Aisha Malik")
        val currentTime = System.currentTimeMillis()

        // Get first 4 products from this seller
        val productsSnapshot = db.collection("products")
            .whereEqualTo("seller_id", sellerId)
            .limit(4)
            .get()
            .await()

        val products = productsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Product::class.java)?.copy(id = doc.id)
        }

        if (products.isEmpty()) {
            Log.w("TestOrder", "⚠️ No products found for seller. Run DashboardDataHelper first!")
            return
        }

        val testOrders = products.mapIndexed { index, product ->
            val status = when (index) {
                0 -> OrderStatus.PENDING
                1 -> OrderStatus.CONFIRMED
                2 -> OrderStatus.SHIPPED
                else -> OrderStatus.DELIVERED
            }

            val dayOffset = when (index) {
                0 -> 0L // Today
                1 -> 1 * 24 * 60 * 60 * 1000L // 1 day ago
                2 -> 3 * 24 * 60 * 60 * 1000L // 3 days ago
                else -> 7 * 24 * 60 * 60 * 1000L // 1 week ago
            }

            Order(
                id = "",
                buyerId = "buyer_${index + 1}",
                buyerName = buyerNames[index],
                sellerId = sellerId,
                sellerName = sellerName,
                productId = product.id,
                productTitle = product.title,
                productImage = product.imageUrls.firstOrNull() ?: "",
                quantity = 1,
                totalPrice = product.price + 150.0, // +150 shipping
                status = status.toString(),
                shippingAddress = "123 Test Street, Lahore",
                buyerPhone = "+92 300 123456${index}",
                items = listOf(
                    OrderItem(
                        productId = product.id,
                        productTitle = product.title,
                        productImage = product.imageUrls.firstOrNull() ?: "",
                        sellerName = sellerName,
                        quantity = 1,
                        price = product.price,
                        isNegotiated = false
                    )
                ),
                subtotal = product.price,
                shipping = 150.0,
                discount = 0.0,
                deliveryInfo = DeliveryInfo(
                    fullName = buyerNames[index],
                    phoneNumber = "+92 300 123456${index}",
                    email = "buyer${index + 1}@test.com",
                    address = "123 Test Street",
                    city = "Lahore",
                    postalCode = "54000"
                ),
                paymentMethod = "Cash on Delivery",
                createdAt = currentTime - dayOffset,
                updatedAt = currentTime
            )
        }

        try {
            Log.d("TestOrder", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d("TestOrder", "🧪 Creating ${testOrders.size} seller test orders...")
            Log.d("TestOrder", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            testOrders.forEachIndexed { index, order ->
                val orderMap = order.toMap()
                val docRef = db.collection("orders").add(orderMap).await()

                Log.d("TestOrder", "✅ Order ${index + 1}/${testOrders.size} created:")
                Log.d("TestOrder", "   ID: ${docRef.id}")
                Log.d("TestOrder", "   Product: ${order.productTitle}")
                Log.d("TestOrder", "   Buyer: ${order.buyerName}")
                Log.d("TestOrder", "   Total: PKR ${order.totalPrice}")
                Log.d("TestOrder", "   Status: ${order.status}")
                Log.d("TestOrder", "")
            }

            Log.d("TestOrder", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d("TestOrder", "✅ All seller test orders created!")
            Log.d("TestOrder", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e("TestOrder", "❌ Failed to create seller test orders", e)
        }
    }
}