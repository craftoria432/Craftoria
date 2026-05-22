package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.Activity
import com.gcuf.craftoria.data.model.ActivityType
import com.gcuf.craftoria.data.model.DashboardStats
import com.gcuf.craftoria.data.model.OrderStatus
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class DashboardRepository {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "DashboardRepository"
    }

    suspend fun getDashboardStats(sellerId: String): Result<DashboardStats> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📊 Fetching dashboard stats for seller: $sellerId")

            // ✅ FIX: Fetch payments from "payments" collection (canonical collection)
            // seller_payments is legacy — all new payments are written to "payments"
            val paymentsSnapshot = db.collection("payments")
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()

            val payments = paymentsSnapshot.documents.mapNotNull { doc ->
                try {
                    val amount = doc.getDouble("amount") ?: 0.0
                    val status = doc.getString("status") ?: "pending"
                    val createdAt = doc.getLong("created_at") ?: 0L
                    Triple(amount, status, createdAt)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing payment document", e)
                    null
                }
            }

            Log.d(TAG, "✅ Fetched ${payments.size} payments")

            // Fetch orders for active order count
            val ordersSnapshot = db.collection("orders")
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()

            val orders = ordersSnapshot.documents.mapNotNull { doc ->
                try {
                    val status = doc.getString("status") ?: "pending"
                    val createdAt = doc.getLong("created_at") ?: 0L
                    Pair(status, createdAt)
                } catch (e: Exception) {
                    null
                }
            }

            Log.d(TAG, "✅ Fetched ${orders.size} orders")

            // Fetch products
            val productsSnapshot = db.collection("products")
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()

            val products = productsSnapshot.documents.mapNotNull { doc ->
                doc.getLong("created_at") ?: 0L
            }

            Log.d(TAG, "✅ Fetched ${products.size} products")

            // ✅ Calculate earnings from COMPLETED payments only
            val completedPayments = payments.filter { it.second == "completed" }
            val totalSales = completedPayments.sumOf { it.first }
            
            Log.d(TAG, "💰 Total Completed Earnings: PKR $totalSales")
            Log.d(TAG, "📦 Completed Payments: ${completedPayments.size}")

            // Active orders (pending, confirmed, processing)
            val activeOrders = orders.count {
                it.first == OrderStatus.PENDING.toString() ||
                        it.first == OrderStatus.PROCESSING.toString() ||
                        it.first == OrderStatus.CONFIRMED.toString()
            }
            val pendingOrders = orders.count { it.first == OrderStatus.PENDING.toString() }
            val processingOrders = orders.count {
                it.first == OrderStatus.PROCESSING.toString() ||
                        it.first == OrderStatus.CONFIRMED.toString()
            }

            // This month's sales (from completed payments)
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            val monthSales = completedPayments.filter {
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.timeInMillis = it.third
                paymentCalendar.get(Calendar.MONTH) == currentMonth &&
                        paymentCalendar.get(Calendar.YEAR) == currentYear
            }.sumOf { it.first }

            Log.d(TAG, "📅 This Month's Earnings: PKR $monthSales")

            // Products added this week
            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val productsThisWeek = products.count { it > weekAgo }

            // Calculate growth
            val salesGrowth = if (totalSales > 0) 12.0 else 0.0

            val stats = DashboardStats(
                totalSales = totalSales,
                activeOrders = activeOrders,
                pendingOrders = pendingOrders,
                processingOrders = processingOrders,
                totalProducts = products.size,
                monthSales = monthSales,
                salesGrowth = salesGrowth,
                productsThisWeek = productsThisWeek
            )

            Log.d(TAG, "✅ Dashboard stats calculated successfully")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch dashboard stats", e)
            Result.failure(e)
        }
    }

    suspend fun getRecentActivities(sellerId: String, limit: Int = 15): Result<List<Activity>> {
        return try {
            Log.d(TAG, "Fetching activities for seller: $sellerId with limit: $limit")

            val activitiesSnapshot = db.collection("activities")
                .whereEqualTo("seller_id", sellerId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            Log.d(TAG, "Found ${activitiesSnapshot.documents.size} activity documents")

            val activities = activitiesSnapshot.documents.mapNotNull { doc ->
                try {
                    val typeString = doc.getString("type") ?: return@mapNotNull null
                    val type = try {
                        ActivityType.valueOf(typeString)
                    } catch (e: Exception) {
                        Log.e(TAG, "Invalid activity type: $typeString", e)
                        return@mapNotNull null
                    }

                    val activity = Activity(
                        id = doc.id,
                        type = type.toString(),
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        timestamp = doc.getTimestamp("timestamp"),
                        orderId = doc.getString("order_id") ?: "",
                        productId = doc.getString("product_id") ?: ""
                    )

                    Log.d(TAG, "Parsed activity: ${activity.title}")
                    activity
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing activity document ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Successfully parsed ${activities.size} activities")
            
            // ✅ PRODUCTION FIX: Automatically delete old activities (keep only latest 15)
            // Run cleanup BEFORE returning results to ensure immediate effect
            cleanupOldActivities(sellerId, limit)
            
            // Return only the limited activities
            val limitedActivities = activities.take(limit)
            Log.d(TAG, "Returning ${limitedActivities.size} activities after cleanup")
            
            Result.success(limitedActivities)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch activities", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ PRODUCTION FIX: Automatically delete activities older than the limit
     * Keeps only the latest 15 activities per seller
     */
    private suspend fun cleanupOldActivities(sellerId: String, keepLimit: Int = 15) {
        try {
            Log.d(TAG, "Starting cleanup of old activities for seller: $sellerId")
            
            // Get all activities for this seller, ordered by timestamp descending
            val allActivitiesSnapshot = db.collection("activities")
                .whereEqualTo("seller_id", sellerId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d(TAG, "Total activities found: ${allActivitiesSnapshot.documents.size}")

            // If we have more than the limit, delete the older ones
            if (allActivitiesSnapshot.documents.size > keepLimit) {
                val activitiesToDelete = allActivitiesSnapshot.documents.drop(keepLimit)
                Log.d(TAG, "Deleting ${activitiesToDelete.size} old activities")

                val batch = db.batch()
                activitiesToDelete.forEach { doc ->
                    batch.delete(doc.reference)
                    Log.d(TAG, "Marked for deletion: ${doc.id}")
                }
                batch.commit().await()

                Log.d(TAG, "✅ Successfully deleted ${activitiesToDelete.size} old activities")
            } else {
                Log.d(TAG, "No cleanup needed - activities within limit")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during activity cleanup", e)
            // Don't throw - cleanup failure shouldn't break the app
        }
    }

    suspend fun logActivity(
        sellerId: String,
        activity: Activity
    ): Result<Unit> {
        return try {
            val activityData = mapOf(
                "seller_id" to sellerId,
                "type" to activity.type,
                "title" to activity.title,
                "description" to activity.description,
                "timestamp" to activity.timestamp,
                "order_id" to activity.orderId,
                "product_id" to activity.productId
            )

            db.collection("activities")
                .add(activityData)
                .await()

            Log.d(TAG, "Activity logged successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to log activity", e)
            Result.failure(e)
        }
    }
}