package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.toMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val ordersCollection = firestore.collection("orders")

    companion object {
        private const val TAG = "OrderRepository"
    }

    // -------------------------------------------------------------------------
    // Create Order
    // -------------------------------------------------------------------------
    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val docRef = ordersCollection.document()
            val orderWithId = order.copy(id = docRef.id)
            docRef.set(orderWithId.toMap()).await()
            Log.d(TAG, "Order created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create order", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Update Order Status
    // -------------------------------------------------------------------------
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to status.toString(),
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Order status updated: $orderId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update order status", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Cancel Order
    // -------------------------------------------------------------------------
    suspend fun cancelOrder(orderId: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId)
                .update(
                    mapOf(
                        "status" to OrderStatus.CANCELLED.toString(),
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Order cancelled: $orderId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel order", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Get Orders By Buyer (REAL-TIME)
    // -------------------------------------------------------------------------
    fun getOrdersByBuyer(buyerId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("buyer_id", buyerId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull {
                    it.toObject(Order::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    // -------------------------------------------------------------------------
    // Get Orders By Seller (REAL-TIME)
    // -------------------------------------------------------------------------
    fun getOrdersBySeller(sellerId: String): Flow<List<Order>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("seller_id", sellerId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull {
                    it.toObject(Order::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    // -------------------------------------------------------------------------
    // Get Orders for User (NON REAL-TIME)
    // -------------------------------------------------------------------------
    suspend fun getUserOrders(userId: String): Result<List<Order>> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("buyer_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Fetched ${orders.size} orders for user: $userId")
            Result.success(orders)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch orders", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Get Single Order
    // -------------------------------------------------------------------------
    suspend fun getOrder(orderId: String): Result<Order> {
        return try {
            val doc = ordersCollection.document(orderId).get().await()
            val order = doc.toObject(Order::class.java)?.copy(id = doc.id)
                ?: throw Exception("Order not found")

            Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get order", e)
            Result.failure(e)
        }
    }
}
