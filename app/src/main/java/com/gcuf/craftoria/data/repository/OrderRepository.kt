package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.toMap
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.model.NotificationActionType
import com.gcuf.craftoria.data.model.NotificationCategory
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val db = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    companion object {
        private const val TAG = "OrderRepository"
    }

    /* ==================== CREATE ORDER WITH NOTIFICATION ==================== */

    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val docRef = if (order.id.isNotEmpty()) {
                ordersCollection.document(order.id)
            } else {
                ordersCollection.document()
            }

            val orderWithId = order.copy(id = docRef.id)
            docRef.set(orderWithId.toMap()).await()

            Log.d(TAG, "✅ Order created: ${docRef.id}")

            // ✅ Process payments for all sellers in the order
            val paymentRepository = PaymentRepository()
            val paymentResult = paymentRepository.processOrderPayments(orderWithId)

            if (paymentResult.isSuccess) {
                Log.d(TAG, "✅ Payments processed successfully")
            } else {
                Log.e(TAG, "⚠️ Payment processing failed: ${paymentResult.exceptionOrNull()?.message}")
            }

            // ✅ Send notification to seller about new order (for backward compatibility)
            sendNewOrderNotification(
                sellerId = order.sellerId,
                orderId = docRef.id,
                productTitle = order.productTitle,
                buyerName = order.buyerName,
                totalPrice = order.totalPrice
            )

            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create order", e)
            Result.failure(e)
        }
    }

    private suspend fun sendNewOrderNotification(
        sellerId: String,
        orderId: String,
        productTitle: String,
        buyerName: String,
        totalPrice: Double
    ) {
        try {
            val notificationData = hashMapOf(
                "user_id" to sellerId,
                "title" to "🎉 New Order Received!",
                "description" to "$buyerName ordered \"$productTitle\" for PKR ${String.format("%.0f", totalPrice)}",
                "category" to NotificationCategory.ORDERS.toString(),
                "action_type" to NotificationActionType.VIEW_ORDER.toString(),
                "order_id" to orderId,
                "product_id" to "",
                "store_id" to "",
                "store_name" to "",
                "member_count" to 0,
                "created_at" to System.currentTimeMillis(),
                "is_read" to false
            )

            db.collection("notifications")
                .add(notificationData)
                .await()

            Log.d(TAG, "✅ Notification sent to seller: $sellerId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send notification", e)
        }
    }

    /* ==================== BUYER OPERATIONS ==================== */

    suspend fun getUserOrders(userId: String): Result<List<Order>> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📦 Fetching orders for buyer: $userId")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            val snapshot = ordersCollection
                .whereEqualTo("buyer_id", userId)
                .get()
                .await()

            Log.d(TAG, "📄 Found ${snapshot.documents.size} documents")

            val orders = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data

                    Log.d(TAG, "")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "📄 Document: ${doc.id}")
                    Log.d(TAG, "Raw Firestore data:")
                    Log.d(TAG, "  buyer_id: ${data?.get("buyer_id")}")
                    Log.d(TAG, "  seller_name: ${data?.get("seller_name")}")
                    Log.d(TAG, "  product_title: ${data?.get("product_title")}")
                    Log.d(TAG, "  total_price: ${data?.get("total_price")}")
                    Log.d(TAG, "  status: ${data?.get("status")}")
                    Log.d(TAG, "  items: ${data?.get("items")}")
                    Log.d(TAG, "  created_at type: ${data?.get("created_at")?.javaClass?.simpleName}")
                    Log.d(TAG, "  created_at value: ${data?.get("created_at")}")

                    // Try to parse with better error handling
                    val order = try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (deserializeError: Exception) {
                        Log.e(TAG, "⚠️ Deserialization error for ${doc.id}, trying manual parsing", deserializeError)

                        // Manual parsing as fallback
                        try {
                            Order(
                                id = doc.id,
                                buyerId = data?.get("buyer_id") as? String ?: "",
                                buyerName = data?.get("buyer_name") as? String ?: "",
                                buyerPhone = data?.get("buyer_phone") as? String ?: "",
                                buyerAvatar = data?.get("buyer_avatar") as? String ?: "",
                                sellerId = data?.get("seller_id") as? String ?: "",
                                sellerName = data?.get("seller_name") as? String ?: "",
                                productId = data?.get("product_id") as? String ?: "",
                                productTitle = data?.get("product_title") as? String ?: "",
                                productImage = data?.get("product_image") as? String ?: "",
                                productPrice = (data?.get("product_price") as? Number)?.toDouble() ?: 0.0,
                                quantity = (data?.get("quantity") as? Number)?.toInt() ?: 1,
                                items = emptyList(), // Will be populated if exists
                                subtotal = (data?.get("subtotal") as? Number)?.toDouble() ?: 0.0,
                                shipping = (data?.get("shipping") as? Number)?.toDouble() ?: 0.0,
                                discount = (data?.get("discount") as? Number)?.toDouble() ?: 0.0,
                                totalPrice = (data?.get("total_price") as? Number)?.toDouble() ?: 0.0,
                                status = data?.get("status") as? String ?: "pending",
                                shippingAddress = data?.get("shipping_address") as? String ?: "",
                                fullAddress = data?.get("full_address") as? String ?: "",
                                paymentMethod = data?.get("payment_method") as? String ?: "Cash on Delivery",
                                createdAt = data?.get("created_at") ?: System.currentTimeMillis(),
                                updatedAt = data?.get("updated_at") ?: System.currentTimeMillis(),
                                orderPlacedAt = data?.get("order_placed_at") ?: System.currentTimeMillis()
                            )
                        } catch (manualError: Exception) {
                            Log.e(TAG, "❌ Manual parsing also failed for ${doc.id}", manualError)
                            null
                        }
                    }

                    if (order != null) {
                        Log.d(TAG, "✅ Parsed Order:")
                        Log.d(TAG, "  id: ${order.id}")
                        Log.d(TAG, "  productTitle: '${order.productTitle}'")
                        Log.d(TAG, "  totalPrice: ${order.totalPrice}")
                        Log.d(TAG, "  status: ${order.status}")
                        Log.d(TAG, "  items count: ${order.items.size}")
                    } else {
                        Log.e(TAG, "❌ Failed to parse order")
                    }

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    order
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.getCreatedAtLong() }

            Log.d(TAG, "✅ Total loaded: ${orders.size} orders")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(orders)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch buyer orders", e)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> {
        return try {
            // Get order details for notifications
            val orderDoc = ordersCollection.document(orderId).get().await()
            val order = orderDoc.toObject(Order::class.java)?.copy(id = orderDoc.id)

            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.CANCELLED.toString(),
                        "cancelled_at" to System.currentTimeMillis(),
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Order cancelled by buyer: $orderId")

            // ✅ Send notifications
            if (order != null) {
                // Notify seller about cancellation request
                com.gcuf.craftoria.utils.NotificationHelper.notifyOrderCancellationRequest(
                    sellerId = order.sellerId,
                    orderId = orderId,
                    orderNumber = orderId.take(8),
                    buyerName = order.buyerName
                )

                Log.d(TAG, "✅ Cancellation request notification sent to seller")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel order: $orderId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).delete().await()

            Log.d(TAG, "Order deleted: $orderId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete order: $orderId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMultipleOrders(orderIds: List<String>): Result<Unit> {
        return try {
            val batch = db.batch()
            orderIds.forEach { id ->
                batch.delete(ordersCollection.document(id))
            }
            batch.commit().await()

            Log.d(TAG, "Deleted ${orderIds.size} orders")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete multiple orders", e)
            Result.failure(e)
        }
    }

    /* ==================== SELLER OPERATIONS ==================== */

    suspend fun getSellerOrders(
        sellerId: String,
        status: OrderStatus? = null
    ): Result<List<Order>> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📦 Fetching orders for seller: $sellerId")
            if (status != null) Log.d(TAG, "📋 Filter: status = $status")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            var query: Query = ordersCollection
                .whereEqualTo("seller_id", sellerId)

            if (status != null) {
                query = query.whereEqualTo("status", status.toString())
            }

            val snapshot = query.get().await()

            Log.d(TAG, "📄 Found ${snapshot.documents.size} documents")

            val orders = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data

                    Log.d(TAG, "")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "📄 Document: ${doc.id}")
                    Log.d(TAG, "Raw Firestore data:")
                    Log.d(TAG, "  seller_id: ${data?.get("seller_id")}")
                    Log.d(TAG, "  buyer_name: ${data?.get("buyer_name")}")
                    Log.d(TAG, "  product_title: ${data?.get("product_title")}")
                    Log.d(TAG, "  total_price: ${data?.get("total_price")}")
                    Log.d(TAG, "  status: ${data?.get("status")}")
                    Log.d(TAG, "  created_at type: ${data?.get("created_at")?.javaClass?.simpleName}")
                    Log.d(TAG, "  created_at value: ${data?.get("created_at")}")

                    // Try to parse with better error handling
                    val order = try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (deserializeError: Exception) {
                        Log.e(TAG, "⚠️ Deserialization error for ${doc.id}, trying manual parsing", deserializeError)

                        // Manual parsing as fallback
                        try {
                            Order(
                                id = doc.id,
                                buyerId = data?.get("buyer_id") as? String ?: "",
                                buyerName = data?.get("buyer_name") as? String ?: "",
                                buyerPhone = data?.get("buyer_phone") as? String ?: "",
                                buyerAvatar = data?.get("buyer_avatar") as? String ?: "",
                                sellerId = data?.get("seller_id") as? String ?: "",
                                sellerName = data?.get("seller_name") as? String ?: "",
                                productId = data?.get("product_id") as? String ?: "",
                                productTitle = data?.get("product_title") as? String ?: "",
                                productImage = data?.get("product_image") as? String ?: "",
                                productPrice = (data?.get("product_price") as? Number)?.toDouble() ?: 0.0,
                                quantity = (data?.get("quantity") as? Number)?.toInt() ?: 1,
                                items = emptyList(), // Will be populated if exists
                                subtotal = (data?.get("subtotal") as? Number)?.toDouble() ?: 0.0,
                                shipping = (data?.get("shipping") as? Number)?.toDouble() ?: 0.0,
                                discount = (data?.get("discount") as? Number)?.toDouble() ?: 0.0,
                                totalPrice = (data?.get("total_price") as? Number)?.toDouble() ?: 0.0,
                                status = data?.get("status") as? String ?: "pending",
                                shippingAddress = data?.get("shipping_address") as? String ?: "",
                                fullAddress = data?.get("full_address") as? String ?: "",
                                paymentMethod = data?.get("payment_method") as? String ?: "Cash on Delivery",
                                createdAt = data?.get("created_at") ?: System.currentTimeMillis(),
                                updatedAt = data?.get("updated_at") ?: System.currentTimeMillis(),
                                orderPlacedAt = data?.get("order_placed_at") ?: System.currentTimeMillis()
                            )
                        } catch (manualError: Exception) {
                            Log.e(TAG, "❌ Manual parsing also failed for ${doc.id}", manualError)
                            null
                        }
                    }

                    if (order != null) {
                        Log.d(TAG, "✅ Parsed Order:")
                        Log.d(TAG, "  id: ${order.id}")
                        Log.d(TAG, "  productTitle: '${order.productTitle}'")
                        Log.d(TAG, "  totalPrice: ${order.totalPrice}")
                        Log.d(TAG, "  status: ${order.status}")
                    } else {
                        Log.e(TAG, "❌ Failed to parse order")
                    }

                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    order
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.getCreatedAtLong() }

            Log.d(TAG, "✅ Fetched ${orders.size} orders")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(orders)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch seller orders", e)
            Result.failure(e)
        }
    }

    suspend fun getNewOrdersCount(sellerId: String): Result<Int> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("seller_id", sellerId)
                .whereEqualTo("status", OrderStatus.NEW.toString())
                .get()
                .await()

            Log.d(TAG, "New orders count for seller $sellerId: ${snapshot.size()}")
            Result.success(snapshot.size())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get new orders count", e)
            Result.failure(e)
        }
    }

    suspend fun acceptOrder(orderId: String): Result<Unit> {
        return try {
            // Get order details first for activity logging
            val orderDoc = ordersCollection.document(orderId).get().await()
            val order = orderDoc.toObject(Order::class.java)?.copy(id = orderDoc.id)
            val sellerId = orderDoc.getString("seller_id") ?: ""
            val productTitle = orderDoc.getString("product_title") ?: "Product"

            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.PROCESSING.toString(),
                        "processing_at" to System.currentTimeMillis(),
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "✅ Order accepted: $orderId")

            // ✅ Send processing notification to buyer
            if (order != null) {
                com.gcuf.craftoria.utils.NotificationHelper.notifyOrderProcessing(
                    buyerId = order.buyerId,
                    orderId = orderId,
                    storeName = order.sellerName,
                    orderNumber = orderId.take(8)
                )
                Log.d(TAG, "✅ Processing notification sent to buyer")
            }

            // Log activity
            try {
                val activityData = mapOf(
                    "seller_id" to sellerId,
                    "type" to "ORDER_SHIPPED",
                    "title" to "Order Processing",
                    "description" to "Order for $productTitle is now being processed",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "order_id" to orderId,
                    "product_id" to ""
                )

                FirebaseFirestore.getInstance()
                    .collection("activities")
                    .add(activityData)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log activity", e)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to accept order", e)
            Result.failure(e)
        }
    }

    suspend fun rejectOrder(
        orderId: String,
        reason: String,
        details: String
    ): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.CANCELLED.toString(),
                        "rejection_reason" to reason,
                        "rejection_details" to details,
                        "cancelled_at" to System.currentTimeMillis(),
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "✅ Order rejected: $orderId, reason: $reason")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to reject order", e)
            Result.failure(e)
        }
    }

    suspend fun markAsShipped(
        orderId: String,
        courierName: String,
        trackingNumber: String,
        expectedDeliveryDate: Long
    ): Result<Unit> {
        return try {
            // Get order details first for activity logging
            val orderDoc = ordersCollection.document(orderId).get().await()
            val order = orderDoc.toObject(Order::class.java)?.copy(id = orderDoc.id)
            val sellerId = orderDoc.getString("seller_id") ?: ""
            val productTitle = orderDoc.getString("product_title") ?: "Product"

            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.SHIPPED.toString(),
                        "courier_name" to courierName,
                        "tracking_number" to trackingNumber,
                        "tracking_id" to trackingNumber,
                        "expected_delivery_date" to expectedDeliveryDate,
                        "estimated_delivery" to expectedDeliveryDate,
                        "shipped_at" to System.currentTimeMillis(),
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "✅ Order marked as shipped: $orderId")

            // ✅ Send shipped notification to buyer
            if (order != null) {
                com.gcuf.craftoria.utils.NotificationHelper.notifyOrderShipped(
                    buyerId = order.buyerId,
                    orderId = orderId,
                    storeName = order.sellerName,
                    orderNumber = orderId.take(8),
                    courierName = courierName,
                    trackingNumber = trackingNumber
                )
                Log.d(TAG, "✅ Shipped notification sent to buyer")
            }

            // Log activity
            try {
                val activityData = mapOf(
                    "seller_id" to sellerId,
                    "type" to "ORDER_SHIPPED",
                    "title" to "Order Shipped",
                    "description" to "Order for $productTitle has been shipped via $courierName",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "order_id" to orderId,
                    "product_id" to ""
                )

                FirebaseFirestore.getInstance()
                    .collection("activities")
                    .add(activityData)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log activity", e)
            }
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark order as shipped", e)
            Result.failure(e)
        }
    }

    suspend fun markAsDelivered(orderId: String): Result<Unit> {
        return try {
            // Get order details first for activity logging and notifications
            val orderDoc = ordersCollection.document(orderId).get().await()
            val order = orderDoc.toObject(Order::class.java)?.copy(id = orderDoc.id)
            val sellerId = orderDoc.getString("seller_id") ?: ""
            val productTitle = orderDoc.getString("product_title") ?: "Product"

            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.COMPLETED.toString(),
                        "delivered_at" to System.currentTimeMillis(),
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "✅ Order marked as delivered: $orderId")

            // ✅ Send delivery notification to buyer - FIXED: Use sellerName instead of storeName
            if (order != null) {
                com.gcuf.craftoria.utils.NotificationHelper.notifyOrderDelivered(
                    buyerId = order.buyerId,
                    orderId = orderId,
                    storeName = order.sellerName,
                    orderNumber = orderId.take(8)
                )
                Log.d(TAG, "✅ Delivery notification sent to buyer")
            }

            // Log activity
            try {
                val activityData = mapOf(
                    "seller_id" to sellerId,
                    "type" to "ORDER_DELIVERED",
                    "title" to "Order Delivered",
                    "description" to "Order for $productTitle has been successfully delivered",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "order_id" to orderId,
                    "product_id" to ""
                )

                FirebaseFirestore.getInstance()
                    .collection("activities")
                    .add(activityData)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log activity", e)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark order as delivered", e)
            Result.failure(e)
        }
    }

    /* ==================== COMMON OPERATIONS ==================== */

    suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val doc = ordersCollection.document(orderId).get().await()
            val order = doc.toObject(Order::class.java)?.copy(id = doc.id)
                ?: throw Exception("Order not found")

            Log.d(TAG, "✅ Fetched order: $orderId")
            Result.success(order)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch order: $orderId", e)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        return try {
            val updateMap = mutableMapOf<String, Any>(
                "status" to newStatus.toString(),
                "updated_at" to System.currentTimeMillis()
            )

            when (newStatus) {
                OrderStatus.PROCESSING -> updateMap["processing_at"] = System.currentTimeMillis()
                OrderStatus.SHIPPED -> updateMap["shipped_at"] = System.currentTimeMillis()
                OrderStatus.DELIVERED, OrderStatus.COMPLETED -> updateMap["delivered_at"] = System.currentTimeMillis()
                OrderStatus.CANCELLED -> updateMap["cancelled_at"] = System.currentTimeMillis()
                else -> {}
            }

            ordersCollection.document(orderId)
                .update(updateMap)
                .await()

            Log.d(TAG, "✅ Order status updated: $orderId -> $newStatus")

            // ✅ FIX: When order is marked as COMPLETED or DELIVERED, update payment status to COMPLETED
            if (newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.DELIVERED) {
                try {
                    Log.d(TAG, "💳 Updating payment status for order: $orderId")
                    
                    // Find all payments for this order
                    val paymentsSnapshot = db.collection("seller_payments")
                        .whereEqualTo("order_id", orderId)
                        .get()
                        .await()

                    Log.d(TAG, "Found ${paymentsSnapshot.documents.size} payments for order: $orderId")

                    // Update each payment to COMPLETED
                    paymentsSnapshot.documents.forEach { paymentDoc ->
                        try {
                            paymentDoc.reference.update(
                                mapOf(
                                    "status" to "completed",
                                    "payment_date" to System.currentTimeMillis(),
                                    "updated_at" to System.currentTimeMillis()
                                )
                            ).await()
                            
                            Log.d(TAG, "✅ Payment ${paymentDoc.id} marked as COMPLETED")
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Failed to update payment ${paymentDoc.id}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to update payment statuses for order $orderId", e)
                    // Don't fail the order update if payment update fails
                }
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update order status", e)
            Result.failure(e)
        }
    }

    /* ==================== MARK ORDER AS VIEWED (FOR BADGE COUNT) ==================== */

    suspend fun markOrderAsViewed(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "is_viewed" to true,
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "✅ Order marked as viewed: $orderId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark order as viewed", e)
            Result.failure(e)
        }
    }
}
