package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manages real-time listeners for seller dashboard metrics
 * Provides instant updates for products, orders, payments, etc.
 */
class DashboardRealtimeManager {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "DashboardRealtimeManager"

    // ✅ Real-time listener for product count
    fun listenToProductCount(sellerId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🎧 Starting product count listener for: $sellerId")

        val listener = db.collection("products")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Product listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val productCount = snapshot.documents.size
                    Log.d(TAG, "📦 Product count: $productCount")
                    trySend(productCount)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing product listener")
            listener.remove()
        }
    }

    // ✅ Real-time listener for pending orders
    fun listenToPendingOrders(sellerId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🎧 Starting pending orders listener for: $sellerId")

        val listener = db.collection("orders")
            .whereEqualTo("seller_id", sellerId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Orders listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orderCount = snapshot.documents.size
                    Log.d(TAG, "📋 Pending orders: $orderCount")
                    trySend(orderCount)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing orders listener")
            listener.remove()
        }
    }

    // ✅ Real-time listener for total earnings
    fun listenToTotalEarnings(sellerId: String): Flow<Double> = callbackFlow {
        Log.d(TAG, "🎧 Starting earnings listener for: $sellerId")

        val listener = db.collection("payments")
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Earnings listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var totalEarnings = 0.0
                    snapshot.documents.forEach { doc ->
                        val amount = doc.getDouble("amount") ?: 0.0
                        totalEarnings += amount
                    }
                    Log.d(TAG, "💰 Total earnings: $totalEarnings")
                    trySend(totalEarnings)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing earnings listener")
            listener.remove()
        }
    }

    // ✅ Real-time listener for pending negotiations
    fun listenToPendingNegotiations(sellerId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🎧 Starting negotiations listener for: $sellerId")

        val listener = db.collection("messages")
            .whereEqualTo("receiver_id", sellerId)
            .whereEqualTo("type", "negotiation")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Negotiations listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val pendingCount = snapshot.documents.count { doc ->
                        doc.getString("negotiation_status") == "pending"
                    }
                    Log.d(TAG, "💬 Pending negotiations: $pendingCount")
                    trySend(pendingCount)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing negotiations listener")
            listener.remove()
        }
    }

    // ✅ Real-time listener for unread messages
    fun listenToUnreadMessages(sellerId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🎧 Starting unread messages listener for: $sellerId")

        val listener = db.collection("messages")
            .whereEqualTo("receiver_id", sellerId)
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Messages listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messageCount = snapshot.documents.size
                    Log.d(TAG, "💌 Unread messages: $messageCount")
                    trySend(messageCount)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing messages listener")
            listener.remove()
        }
    }

    // ✅ Real-time listener for completed orders (for activity feed)
    fun listenToCompletedOrders(sellerId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🎧 Starting completed orders listener for: $sellerId")

        val listener = db.collection("orders")
            .whereEqualTo("seller_id", sellerId)
            .whereEqualTo("status", "completed")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Completed orders listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val completedCount = snapshot.documents.size
                    Log.d(TAG, "✅ Completed orders: $completedCount")
                    trySend(completedCount)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing completed orders listener")
            listener.remove()
        }
    }

    // ✅ Real-time listener for pending approvals (co-seller products)
    fun listenToPendingApprovals(sellerId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "🎧 Starting pending approvals listener for: $sellerId")

        val listener = db.collection("products")
            .whereEqualTo("seller_id", sellerId)
            .whereEqualTo("approval_status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Approvals listener error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val approvalCount = snapshot.documents.size
                    Log.d(TAG, "⏳ Pending approvals: $approvalCount")
                    trySend(approvalCount)
                }
            }

        awaitClose {
            Log.d(TAG, "🔌 Closing approvals listener")
            listener.remove()
        }
    }
}
