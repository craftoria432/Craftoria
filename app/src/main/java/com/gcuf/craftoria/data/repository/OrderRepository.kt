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
        
        /**
         * Helper function to convert Firestore Timestamp or Long to Long milliseconds
         * Handles backward compatibility with both data types
         */
        private fun convertTimestamp(value: Any?): Long {
            return when (value) {
                is Long -> value
                is com.google.firebase.Timestamp -> value.toDate().time
                is Number -> value.toLong()
                else -> System.currentTimeMillis()
            }
        }
        
        /**
         * Manual parsing of Order document with proper Timestamp handling
         * Used as fallback when automatic deserialization fails
         */
        private fun parseOrderManually(doc: com.google.firebase.firestore.DocumentSnapshot): Order? {
            return try {
                val data = doc.data ?: return null
                
                Order(
                    id = doc.id,
                    buyerId = data["buyer_id"] as? String ?: "",
                    buyerName = data["buyer_name"] as? String ?: "",
                    buyerPhone = data["buyer_phone"] as? String ?: "",
                    buyerAvatar = data["buyer_avatar"] as? String ?: "",
                    sellerId = data["seller_id"] as? String ?: "",
                    sellerName = data["seller_name"] as? String ?: "",
                    productId = data["product_id"] as? String ?: "",
                    productTitle = data["product_title"] as? String ?: "",
                    productImage = data["product_image"] as? String ?: "",
                    productPrice = (data["product_price"] as? Number)?.toDouble() ?: 0.0,
                    quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
                    items = emptyList(), // Will be populated if exists
                    subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                    shipping = (data["shipping"] as? Number)?.toDouble() ?: 0.0,
                    discount = (data["discount"] as? Number)?.toDouble() ?: 0.0,
                    totalPrice = (data["total_price"] as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (data["total_amount"] as? Number)?.toDouble() ?: 0.0,
                    coSellerStoreId = data["co_seller_store_id"] as? String ?: "",
                    status = data["status"] as? String ?: "pending",
                    shippingAddress = data["shipping_address"] as? String ?: "",
                    fullAddress = data["full_address"] as? String ?: "",
                    paymentMethod = data["payment_method"] as? String ?: "Cash on Delivery",
                    createdAt = convertTimestamp(data["created_at"]),
                    updatedAt = convertTimestamp(data["updated_at"]),
                    orderPlacedAt = convertTimestamp(data["order_placed_at"]),
                    processingAt = data["processing_at"]?.let { convertTimestamp(it) },
                    shippedAt = data["shipped_at"]?.let { convertTimestamp(it) },
                    deliveredAt = data["delivered_at"]?.let { convertTimestamp(it) },
                    cancelledAt = data["cancelled_at"]?.let { convertTimestamp(it) },
                    trackingId = data["tracking_id"] as? String ?: "",
                    trackingNumber = data["tracking_number"] as? String ?: "",
                    courierName = data["courier_name"] as? String ?: "",
                    courierContact = data["courier_contact"] as? String ?: "",
                    estimatedDelivery = data["estimated_delivery"]?.let { convertTimestamp(it) },
                    expectedDeliveryDate = data["expected_delivery_date"]?.let { convertTimestamp(it) },
                    rejectionReason = data["rejection_reason"] as? String ?: "",
                    rejectionDetails = data["rejection_details"] as? String ?: ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Manual parsing failed for ${doc.id}", e)
                null
            }
        }
    }

    /* ==================== CREATE ORDER WITH NOTIFICATION ==================== */

    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val docRef = if (order.id.isNotEmpty()) {
                ordersCollection.document(order.id)
            } else {
                ordersCollection.document()
            }

            val involvedSellerIds = resolveInvolvedSellerIds(order)
            val orderWithId = order.copy(
                id = docRef.id,
                involvedSellerIds = involvedSellerIds
            )
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
                totalPrice = order.totalPrice,
                coSellerStoreId = order.coSellerStoreId
            )

            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create order", e)
            Result.failure(e)
        }
    }

    /** Primary seller, line-item sellers, and co-seller store members who can view the order. */
    private suspend fun resolveInvolvedSellerIds(order: Order): List<String> {
        val sellerIds = linkedSetOf<String>()
        if (order.sellerId.isNotEmpty()) sellerIds.add(order.sellerId)
        order.items.mapNotNullTo(sellerIds) { item ->
            item.sellerId.takeIf { it.isNotEmpty() }
        }
        if (order.coSellerStoreId.isNotEmpty()) {
            try {
                val storeDoc = db.collection("co_seller_stores")
                    .document(order.coSellerStoreId)
                    .get()
                    .await()
                (storeDoc.get("member_ids") as? List<*>)?.filterIsInstance<String>()?.let {
                    sellerIds.addAll(it)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load store members for order access: ${e.message}")
            }
        }
        return sellerIds.toList()
    }

    private suspend fun sendNewOrderNotification(
        sellerId: String,
        orderId: String,
        productTitle: String,
        buyerName: String,
        totalPrice: Double,
        coSellerStoreId: String = ""
    ) {
        try {
            // ✅ Fetch store name and member count from co-seller store if available
            var storeName = ""
            var memberCount = 0
            
            if (coSellerStoreId.isNotEmpty()) {
                try {
                    val storeDoc = db.collection("co_seller_stores")
                        .document(coSellerStoreId)
                        .get()
                        .await()
                    
                    if (storeDoc.exists()) {
                        storeName = storeDoc.getString("store_name") ?: ""
                        // Prioritize member_ids array over member_count field
                        val memberIds = storeDoc.get("member_ids") as? List<*>
                        memberCount = memberIds?.size ?: (storeDoc.getLong("member_count")?.toInt() ?: 0)
                        Log.d(TAG, "✅ Fetched store data: $storeName with $memberCount members")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to fetch store data for notification", e)
                }
            }
            
            val notificationData = hashMapOf(
                "user_id" to sellerId,
                "title" to "🎉 New Order Received!",
                "description" to "$buyerName ordered \"$productTitle\" for PKR ${String.format("%.0f", totalPrice)}",
                "category" to NotificationCategory.ORDERS.toString(),
                "action_type" to NotificationActionType.VIEW_ORDER.toString(),
                "order_id" to orderId,
                "product_id" to "",
                "store_id" to coSellerStoreId,
                "store_name" to storeName,
                "member_count" to memberCount,
                "created_at" to System.currentTimeMillis(),
                "is_read" to false
            )

            db.collection("notifications")
                .add(notificationData)
                .await()

            Log.d(TAG, "✅ Notification sent to seller: $sellerId with store: $storeName ($memberCount members)")

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
            // Count pending orders (status buyers create when placing an order)
            val snapshot = ordersCollection
                .whereEqualTo("seller_id", sellerId)
                .whereIn("status", listOf(OrderStatus.PENDING.toString(), OrderStatus.PENDING.name))
                .get()
                .await()

            Log.d(TAG, "New orders count for seller $sellerId: ${snapshot.size()}")
            Result.success(snapshot.size())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get new orders count", e)
            Result.failure(e)
        }
    }

    fun observeUserOrders(userId: String, onUpdate: (List<Order>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return ordersCollection
            .whereEqualTo("buyer_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Real-time buyer orders error", error)
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing order ${doc.id}", e)
                        null
                    }
                }?.sortedByDescending { it.getCreatedAtLong() } ?: emptyList()
                Log.d(TAG, "Real-time update: ${orders.size} buyer orders")
                onUpdate(orders)
            }
    }

    fun observeNewOrdersCount(sellerId: String, onUpdate: (Int) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        // Include legacy uppercase status values written before the checkout fix
        return ordersCollection
            .whereEqualTo("seller_id", sellerId)
            .whereIn("status", listOf(OrderStatus.PENDING.toString(), OrderStatus.PENDING.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe new orders count", error)
                    return@addSnapshotListener
                }
                val count = snapshot?.size() ?: 0
                Log.d(TAG, "Real-time new orders count for seller $sellerId: $count")
                onUpdate(count)
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
                // ✅ FIXED: Fetch current seller name (not stale order name)
                var currentSellerName = order.sellerName
                try {
                    val sellerDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(order.sellerId)
                        .get()
                        .await()
                    if (sellerDoc.exists()) {
                        currentSellerName = sellerDoc.getString("name") ?: order.sellerName
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch current seller name: ${e.message}, using order name")
                }
                
                com.gcuf.craftoria.utils.NotificationHelper.notifyOrderProcessing(
                    buyerId = order.buyerId,
                    orderId = orderId,
                    storeName = currentSellerName,
                    orderNumber = orderId.take(8),
                    storeId = order.coSellerStoreId,
                    memberCount = 0  // Will be fetched real-time in UI if co-seller
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

            // ✅ FIX: Create simplified timeline - only show current status, not future pending statuses
            val currentTime = System.currentTimeMillis()
            val timeline = listOf(
                mapOf(
                    "title" to "Order Confirmed",
                    "timestamp" to currentTime,
                    "is_completed" to true
                ),
                mapOf(
                    "title" to "Shipped",
                    "timestamp" to currentTime,
                    "is_completed" to true
                )
            )

            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.SHIPPED.toString(),
                        "courier_name" to courierName,
                        "tracking_number" to trackingNumber,
                        "tracking_id" to trackingNumber,
                        "expected_delivery_date" to expectedDeliveryDate,
                        "estimated_delivery" to expectedDeliveryDate,
                        "shipped_at" to currentTime,
                        "updated_at" to currentTime,
                        "timeline" to timeline
                    )
                )
                .await()

            Log.d(TAG, "✅ Order marked as shipped: $orderId")

            // ✅ Send shipped notification to buyer
            if (order != null) {
                // ✅ FIXED: Fetch current seller name (not stale order name)
                var currentSellerName = order.sellerName
                try {
                    val sellerDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(order.sellerId)
                        .get()
                        .await()
                    if (sellerDoc.exists()) {
                        currentSellerName = sellerDoc.getString("name") ?: order.sellerName
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch current seller name: ${e.message}, using order name")
                }
                
                com.gcuf.craftoria.utils.NotificationHelper.notifyOrderShipped(
                    buyerId = order.buyerId,
                    orderId = orderId,
                    storeName = currentSellerName,
                    orderNumber = orderId.take(8),
                    courierName = courierName,
                    trackingNumber = trackingNumber,
                    storeId = order.coSellerStoreId,
                    memberCount = 0  // Will be fetched real-time in UI if co-seller
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

            val currentTime = System.currentTimeMillis()

            // Build update data with null-safe timeline handling
            val updateData = mutableMapOf<String, Any>(
                "status" to OrderStatus.DELIVERED.toString(),
                "delivered_at" to currentTime,
                "updated_at" to currentTime
            )

            // Only update timeline if we have one to update
            val updatedTimeline = (order?.timeline ?: emptyList()).map { item ->
                item.copy(isCompleted = true, timestamp = currentTime)
            }

            if (updatedTimeline.isNotEmpty()) {
                updateData["timeline"] = updatedTimeline.map { it.toMap() }
            }

            ordersCollection.document(orderId)
                .update(updateData)
                .await()

            Log.d(TAG, "✅ Order marked as delivered: $orderId")
            
            // ✅ CRITICAL FIX: Also update payment status to completed
            // This ensures that when an order is delivered, the payment status is also updated
            try {
                Log.d(TAG, "💳 Updating payment status for delivered order: $orderId")
                
                // Find all payments for this order
                val paymentsSnapshot = db.collection("payments")
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
                                "payment_date" to currentTime,
                                "updated_at" to currentTime
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

            // ✅ Send delivery notification to buyer
            if (order != null) {
                // ✅ FIXED: Fetch current seller name (not stale order name)
                var currentSellerName = order.sellerName
                try {
                    val sellerDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(order.sellerId)
                        .get()
                        .await()
                    if (sellerDoc.exists()) {
                        currentSellerName = sellerDoc.getString("name") ?: order.sellerName
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch current seller name: ${e.message}, using order name")
                }
                
                com.gcuf.craftoria.utils.NotificationHelper.notifyOrderDelivered(
                    buyerId = order.buyerId,
                    orderId = orderId,
                    storeName = currentSellerName,
                    orderNumber = orderId.take(8),
                    storeId = order.coSellerStoreId,
                    memberCount = 0  // Will be fetched real-time in UI if co-seller
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
                    // ✅ CRITICAL: Using correct collection name "payments" (not "seller_payments")
                    val paymentsSnapshot = db.collection("payments")
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
