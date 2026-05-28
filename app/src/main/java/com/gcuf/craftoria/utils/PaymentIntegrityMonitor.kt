package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.SellerPayment
import kotlinx.coroutines.tasks.await
import com.gcuf.craftoria.data.model.getCreatedAtLong

/**
 * ✅ PAYMENT INTEGRITY MONITOR
 * 
 * Monitors and reports orders without payment records.
 * This helps detect and prevent the "Payment History showing nothing" issue.
 * 
 * Usage:
 * - Call checkPaymentIntegrity() periodically (e.g., on app start, daily)
 * - Monitor logs for warnings about missing payments
 * - Use getOrdersWithoutPayments() to get detailed list
 */
object PaymentIntegrityMonitor {
    private const val TAG = "PaymentIntegrityMonitor"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Check if all orders have corresponding payment records
     * Returns a report of any issues found
     */
    suspend fun checkPaymentIntegrity(): PaymentIntegrityReport {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔍 PAYMENT INTEGRITY CHECK")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Get all orders
            val ordersSnapshot = db.collection("orders").get().await()
            val orders = ordersSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing order ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "📦 Total orders: ${orders.size}")

            // Get all payments
            val paymentsSnapshot = db.collection("payments").get().await()
            val payments = paymentsSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing payment ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "💳 Total payments: ${payments.size}")

            // Create map of order IDs with payments
            val orderIdsWithPayments = payments.map { it.orderId }.toSet()

            // Find orders without payments
            val ordersWithoutPayments = orders.filter { order ->
                !orderIdsWithPayments.contains(order.id)
            }

            val report = PaymentIntegrityReport(
                totalOrders = orders.size,
                totalPayments = payments.size,
                ordersWithPayments = orders.size - ordersWithoutPayments.size,
                ordersWithoutPayments = ordersWithoutPayments.size,
                missingPaymentOrders = ordersWithoutPayments.map { order ->
                    MissingPaymentOrder(
                        orderId = order.id,
                        buyerId = order.buyerId,
                        buyerName = order.buyerName,
                        status = order.status,
                        totalAmount = order.totalPrice,
                        createdAt = order.getCreatedAtLong()
                    )
                }
            )

            // Log results
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📊 INTEGRITY REPORT")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "✅ Orders with payments: ${report.ordersWithPayments}")
            Log.d(TAG, "❌ Orders without payments: ${report.ordersWithoutPayments}")
            
            if (report.ordersWithoutPayments > 0) {
                Log.w(TAG, "⚠️  WARNING: ${report.ordersWithoutPayments} orders are missing payment records!")
                Log.w(TAG, "   This may cause Payment History to show incorrect data.")
                Log.w(TAG, "   Run create-missing-payments.mjs to fix this issue.")
                
                // Log details of first few missing payments
                report.missingPaymentOrders.take(5).forEach { missing ->
                    Log.w(TAG, "   - Order ${missing.orderId.take(8)}: ${missing.buyerName} (${missing.status})")
                }
                if (report.ordersWithoutPayments > 5) {
                    Log.w(TAG, "   ... and ${report.ordersWithoutPayments - 5} more")
                }
            } else {
                Log.d(TAG, "✅ All orders have payment records!")
            }
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            report
        } catch (e: Exception) {
            Log.e(TAG, "❌ Payment integrity check failed", e)
            PaymentIntegrityReport(
                totalOrders = 0,
                totalPayments = 0,
                ordersWithPayments = 0,
                ordersWithoutPayments = 0,
                missingPaymentOrders = emptyList(),
                error = e.message
            )
        }
    }

    /**
     * Get detailed list of orders without payment records
     */
    suspend fun getOrdersWithoutPayments(): List<MissingPaymentOrder> {
        val report = checkPaymentIntegrity()
        return report.missingPaymentOrders
    }

    /**
     * Check if a specific order has payment records
     */
    suspend fun hasPaymentRecords(orderId: String): Boolean {
        return try {
            val snapshot = db.collection("payments")
                .whereEqualTo("order_id", orderId)
                .get()
                .await()
            
            val hasPayments = !snapshot.isEmpty
            
            if (!hasPayments) {
                Log.w(TAG, "⚠️  Order $orderId has no payment records!")
            }
            
            hasPayments
        } catch (e: Exception) {
            Log.e(TAG, "Error checking payment records for order $orderId", e)
            false
        }
    }
}

/**
 * Report of payment integrity check
 */
data class PaymentIntegrityReport(
    val totalOrders: Int,
    val totalPayments: Int,
    val ordersWithPayments: Int,
    val ordersWithoutPayments: Int,
    val missingPaymentOrders: List<MissingPaymentOrder>,
    val error: String? = null
) {
    val isHealthy: Boolean
        get() = ordersWithoutPayments == 0 && error == null
    
    val healthPercentage: Double
        get() = if (totalOrders > 0) {
            (ordersWithPayments.toDouble() / totalOrders.toDouble()) * 100.0
        } else {
            100.0
        }
}

/**
 * Details of an order without payment records
 */
data class MissingPaymentOrder(
    val orderId: String,
    val buyerId: String,
    val buyerName: String,
    val status: String,
    val totalAmount: Double,
    val createdAt: Long
)
