package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.utils.PaymentValidator
import kotlinx.coroutines.tasks.await
import java.util.UUID

// FIX: Removed broken line: `private val Order.coSellerStoreId: String`
// An extension property requires a getter body (get() = ...) or must be abstract.
// If coSellerStoreId is a field on Order, access it directly as order.coSellerStoreId.
// If it doesn't exist on Order yet, add it as a regular property to the Order data class.

class PaymentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val paymentsCollection = db.collection("seller_payments")
    // ordersCollection removed — not used in this file
    private val notificationsCollection = db.collection("notifications")

    companion object {
        private const val TAG = "PaymentRepository"
        
        // ✅ Security Exception for access control
        class UnauthorizedAccessException(message: String) : SecurityException(message)
    }

    /* ==================== PAYMENT PROCESSING ==================== */

    /**
     * Process order and create individual seller payments
     * This is called when an order is successfully placed
     * Supports both new orders (with items array) and legacy orders (single product)
     */
    suspend fun processOrderPayments(order: Order): Result<List<String>> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💳 Processing payments for order: ${order.id}")
            
            val paymentIds = mutableListOf<String>()

            // ✅ FIX: Handle both new format (items array) and legacy format (single product)
            val itemsToProcess = if (order.items.isNotEmpty()) {
                // New format: items array is populated
                Log.d(TAG, "📦 New format order with ${order.items.size} items")
                order.items
            } else if (order.productId.isNotEmpty()) {
                // Legacy format: convert single product to items array
                Log.d(TAG, "📦 Legacy format order - converting to new format")
                listOf(
                    OrderItem(
                        productId = order.productId,
                        sellerId = order.sellerId,
                        sellerName = order.sellerName,
                        productTitle = order.productTitle,
                        productImage = order.productImage,
                        quantity = order.quantity,
                        price = order.productPrice
                    )
                )
            } else {
                Log.w(TAG, "⚠️ Order has no items and no legacy product data")
                emptyList()
            }

            Log.d(TAG, "📦 Total items to process: ${itemsToProcess.size}")

            // Group items by seller
            val itemsBySellerMap = itemsToProcess.groupBy { it.sellerId }
            Log.d(TAG, "👥 Sellers involved: ${itemsBySellerMap.size}")

            // ✅ NEW: Track all involved sellers for access control
            val involvedSellerIds = itemsBySellerMap.keys.toList()
            Log.d(TAG, "🔐 Involved sellers: $involvedSellerIds")

            itemsBySellerMap.forEach { (sellerId, sellerItems) ->
                try {
                    val sellerAmount = sellerItems.sumOf { it.price * it.quantity }
                    val itemsCount = sellerItems.sumOf { it.quantity }

                    Log.d(TAG, "")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "💰 Seller: $sellerId")
                    Log.d(TAG, "💵 Amount: PKR $sellerAmount")
                    Log.d(TAG, "📦 Items: $itemsCount")

                    // Create payment record
                    val payment = SellerPayment(
                        sellerId = sellerId,
                        sellerName = sellerItems.first().sellerName,
                        orderId = order.id,
                        coSellerStoreId = sellerId,  // FIX: Order has no coSellerStoreId field; use the seller's own ID
                        storeName = order.sellerName,
                        buyerId = order.buyerId,
                        buyerName = order.buyerName,
                        amount = sellerAmount,
                        paymentMethod = order.paymentMethod,
                        status = PaymentStatus.PENDING.toString(),
                        itemsCount = itemsCount,
                        itemsDetails = sellerItems.map { item ->
                            PaymentItemDetail(
                                productId = item.productId,
                                productTitle = item.productTitle,
                                quantity = item.quantity,
                                price = item.price,
                                itemTotal = item.price * item.quantity
                            )
                        },
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        involvedSellerIds = involvedSellerIds  // ✅ NEW: Store all involved sellers
                    )

                    // Add to Firestore
                    val docRef = paymentsCollection.add(payment.toMap()).await()
                    val paymentId = docRef.id
                    paymentIds.add(paymentId)

                    // Update payment with ID
                    paymentsCollection.document(paymentId).update("id", paymentId).await()

                    Log.d(TAG, "✅ Payment created: $paymentId")

                    // Send notification to seller
                    sendPaymentNotification(
                        sellerId = sellerId,
                        sellerName = sellerItems.first().sellerName,
                        orderId = order.id,
                        amount = sellerAmount,
                        itemsCount = itemsCount,
                        buyerName = order.buyerName
                    )

                    Log.d(TAG, "📬 Notification sent to seller")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing payment for seller: $sellerId", e)
                }
            }

            Log.d(TAG, "✅ All payments processed: ${paymentIds.size} payments created")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(paymentIds)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process order payments", e)
            Result.failure(e)
        }
    }

    /* ==================== SELLER PAYMENT QUERIES ==================== */

    /**
     * Get all payments for a seller with access control
     * ✅ SECURITY: Only the seller can view their own payments
     */
    suspend fun getSellerPayments(
        sellerId: String,
        requestingUserId: String,
        status: PaymentStatus? = null
    ): Result<List<SellerPayment>> {
        return try {
            // ✅ SECURITY CHECK: Verify requesting user is the seller
            if (sellerId != requestingUserId) {
                Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to access payments for seller $sellerId")
                return Result.failure(
                    UnauthorizedAccessException(
                        "Unauthorized: Cannot access other seller's payments"
                    )
                )
            }

            Log.d(TAG, "📊 Fetching payments for seller: $sellerId")

            var query: Query = paymentsCollection
                .whereEqualTo("seller_id", sellerId)

            if (status != null) {
                query = query.whereEqualTo("status", status.toString())
            }

            val snapshot = query
                .get()
                .await()

            val payments = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing payment ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.createdAt }

            Log.d(TAG, "✅ Fetched ${payments.size} payments")
            Result.success(payments)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch seller payments", e)
            Result.failure(e)
        }
    }

    /**
     * Get payment by ID with access control
     * ✅ SECURITY: Only the seller who owns this payment can view it
     */
    suspend fun getPaymentById(
        paymentId: String,
        requestingUserId: String
    ): Result<SellerPayment?> {
        return try {
            val doc = paymentsCollection.document(paymentId).get().await()
            val payment = doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)

            if (payment == null) {
                return Result.success(null)
            }

            // ✅ SECURITY CHECK: Verify requesting user owns this payment
            if (payment.sellerId != requestingUserId) {
                Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to access payment $paymentId (owner: ${payment.sellerId})")
                return Result.failure(
                    UnauthorizedAccessException(
                        "Unauthorized: Cannot access other seller's payment"
                    )
                )
            }

            Result.success(payment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get payment", e)
            Result.failure(e)
        }
    }

    /**
     * Get payments for an order with access control
     * ✅ SECURITY: Only sellers involved in the order can view the payment split
     */
    suspend fun getOrderPayments(
        orderId: String,
        requestingUserId: String
    ): Result<List<SellerPayment>> {
        return try {
            Log.d(TAG, "📋 Fetching payments for order: $orderId")

            val snapshot = paymentsCollection
                .whereEqualTo("order_id", orderId)
                .get()
                .await()

            val payments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.createdAt }

            // ✅ SECURITY CHECK: Verify requesting user is involved in this order
            val isUserInvolved = payments.any { it.sellerId == requestingUserId }
            if (!isUserInvolved) {
                Log.w(TAG, "🚫 UNAUTHORIZED: User $requestingUserId attempted to view payment split for order $orderId (not involved)")
                return Result.failure(
                    UnauthorizedAccessException(
                        "Unauthorized: Not involved in this order"
                    )
                )
            }

            Log.d(TAG, "✅ Fetched ${payments.size} payments for order")
            Result.success(payments)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch order payments", e)
            Result.failure(e)
        }
    }

    /* ==================== PAYMENT STATUS UPDATES ==================== */

    /**
     * Update payment status
     */
    suspend fun updatePaymentStatus(
        paymentId: String,
        newStatus: PaymentStatus,
        transactionId: String = ""
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating payment status: $paymentId -> $newStatus")

            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.toString(),
                "updated_at" to System.currentTimeMillis()
            )

            if (transactionId.isNotEmpty()) {
                updates["transaction_id"] = transactionId
            }

            if (newStatus == PaymentStatus.COMPLETED) {
                updates["payment_date"] = System.currentTimeMillis()
            }

            paymentsCollection.document(paymentId).update(updates).await()

            Log.d(TAG, "✅ Payment status updated")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update payment status", e)
            Result.failure(e)
        }
    }

    /**
     * Mark payment as completed
     */
    suspend fun markPaymentCompleted(paymentId: String, transactionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "✅ Marking payment as completed: $paymentId")

            paymentsCollection.document(paymentId).update(
                mapOf(
                    "status" to PaymentStatus.COMPLETED.toString(),
                    "transaction_id" to transactionId,
                    "payment_date" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Payment marked as completed")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark payment as completed", e)
            Result.failure(e)
        }
    }

    /**
     * Process refund for a payment
     */
    suspend fun processRefund(
        paymentId: String,
        refundAmount: Double,
        reason: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Processing refund for payment: $paymentId")
            Log.d(TAG, "💰 Refund amount: PKR $refundAmount")
            Log.d(TAG, "📝 Reason: $reason")

            paymentsCollection.document(paymentId).update(
                mapOf(
                    "status" to PaymentStatus.REFUNDED.toString(),
                    "refund_amount" to refundAmount,
                    "refund_reason" to reason,
                    "refund_date" to System.currentTimeMillis(),
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()

            Log.d(TAG, "✅ Refund processed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process refund", e)
            Result.failure(e)
        }
    }

    /* ==================== BUYER PAYMENT QUERIES ==================== */

    /**
     * Get all payments for a buyer (payments they made)
     */
    suspend fun getBuyerPayments(buyerId: String): Result<List<SellerPayment>> {
        return try {
            Log.d(TAG, "📊 Fetching payments for buyer: $buyerId")

            val snapshot = paymentsCollection
                .whereEqualTo("buyer_id", buyerId)
                .get()
                .await()

            val payments = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing payment ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.createdAt } // ✅ Sort in memory instead of Firestore

            Log.d(TAG, "✅ Fetched ${payments.size} payments for buyer")
            Result.success(payments)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch buyer payments", e)
            Result.failure(e)
        }
    }

    /**
     * Get buyer payment statistics
     */
    suspend fun getBuyerPaymentStats(buyerId: String): Result<com.gcuf.craftoria.viewmodel.BuyerPaymentStats> {
        return try {
            Log.d(TAG, "📊 Calculating payment stats for buyer: $buyerId")

            val snapshot = paymentsCollection
                .whereEqualTo("buyer_id", buyerId)
                .get()
                .await()

            val payments = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing payment ${doc.id}", e)
                    null
                }
            }

            val totalSpent = payments.sumOf { it.amount }
            val completedPayments = payments.filter { it.status == PaymentStatus.COMPLETED.toString() }
            val completedAmount = completedPayments.sumOf { it.amount }
            val pendingAmount = payments
                .filter { it.status == PaymentStatus.PENDING.toString() }
                .sumOf { it.amount }
            val totalOrders = payments.map { it.orderId }.distinct().size
            val totalSellers = payments.map { it.sellerId }.distinct().size

            val stats = com.gcuf.craftoria.viewmodel.BuyerPaymentStats(
                totalSpent = totalSpent,
                completedAmount = completedAmount,
                pendingAmount = pendingAmount,
                totalPayments = payments.size,
                completedPayments = completedPayments.size,
                totalOrders = totalOrders,
                totalSellers = totalSellers
            )

            Log.d(TAG, "✅ Stats calculated:")
            Log.d(TAG, "   Total Spent: PKR $totalSpent")
            Log.d(TAG, "   Completed: PKR $completedAmount")
            Log.d(TAG, "   Pending: PKR $pendingAmount")
            Log.d(TAG, "   Total Orders: $totalOrders")
            Log.d(TAG, "   Total Sellers: $totalSellers")

            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to calculate buyer payment stats", e)
            Result.failure(e)
        }
    }

    /* ==================== PAYMENT STATISTICS ==================== */

    /**
     * Get seller payment statistics
     */
    suspend fun getSellerPaymentStats(sellerId: String): Result<SellerPaymentStats> {
        return try {
            Log.d(TAG, "📊 Calculating payment stats for seller: $sellerId")

            val snapshot = paymentsCollection
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()

            val payments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SellerPayment::class.java)?.copy(id = doc.id)
            }

            val totalEarnings = payments.sumOf { it.amount }
            val completedPayments = payments.filter { it.status == PaymentStatus.COMPLETED.toString() }
            val completedAmount = completedPayments.sumOf { it.amount }
            val pendingAmount = payments
                .filter { it.status == PaymentStatus.PENDING.toString() }
                .sumOf { it.amount }
            val totalOrders = payments.map { it.orderId }.distinct().size

            val stats = SellerPaymentStats(
                totalEarnings = totalEarnings,
                completedAmount = completedAmount,
                pendingAmount = pendingAmount,
                totalPayments = payments.size,
                completedPayments = completedPayments.size,
                totalOrders = totalOrders
            )

            Log.d(TAG, "✅ Stats calculated:")
            Log.d(TAG, "   Total Earnings: PKR $totalEarnings")
            Log.d(TAG, "   Completed: PKR $completedAmount")
            Log.d(TAG, "   Pending: PKR $pendingAmount")

            Result.success(stats)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to calculate payment stats", e)
            Result.failure(e)
        }
    }

    /* ==================== NOTIFICATIONS ==================== */

    private suspend fun sendPaymentNotification(
        sellerId: String,
        @Suppress("UNUSED_PARAMETER") sellerName: String,
        orderId: String,
        amount: Double,
        itemsCount: Int,
        buyerName: String
    ) {
        try {
            // ✅ Use NotificationHelper for production-ready notification
            com.gcuf.craftoria.utils.NotificationHelper.notifyPaymentReceived(
                sellerId = sellerId,
                orderId = orderId,
                orderNumber = orderId.take(8),
                amount = amount
            )
            Log.d(TAG, "✅ Payment notification sent to seller: $sellerId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send payment notification", e)
        }
    }

    /* ==================== IDEMPOTENCY & VALIDATION ==================== */

    /**
     * Process order payments with idempotency key to prevent duplicates
     */
    suspend fun processOrderPaymentsWithIdempotency(
        order: Order,
        idempotencyKey: String
    ): Result<List<String>> {
        return try {
            // Check if payment already processed
            val existingPayment = paymentsCollection
                .whereEqualTo("order_id", order.id)
                .whereEqualTo("idempotency_key", idempotencyKey)
                .get()
                .await()

            if (!existingPayment.isEmpty) {
                Log.d(TAG, "✅ Idempotent request - returning existing payment")
                val paymentIds = existingPayment.documents.map { it.id }
                return Result.success(paymentIds)
            }

            // Process new payment
            val result = processOrderPayments(order)
            if (result.isSuccess) {
                val paymentIds = result.getOrNull() ?: emptyList()
                
                // Store idempotency key with payments
                paymentIds.forEach { paymentId ->
                    paymentsCollection.document(paymentId).update(
                        mapOf(
                            "idempotency_key" to idempotencyKey,
                            "request_id" to UUID.randomUUID().toString()
                        )
                    ).await()
                }
                
                Log.d(TAG, "✅ Payments processed with idempotency key: $idempotencyKey")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process payment with idempotency", e)
            Result.failure(e)
        }
    }

    /**
     * Validate order payment before processing
     */
    suspend fun validateOrderPayment(order: Order, items: List<OrderItem>): Result<Unit> {
        return try {
            val validation = PaymentValidator.validateOrderPayment(order, items)
            if (!validation.isValid) {
                return Result.failure(Exception(validation.errors.joinToString(", ")))
            }
            Log.d(TAG, "✅ Order payment validation passed")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Payment validation failed", e)
            Result.failure(e)
        }
    }
}

data class SellerPaymentStats(
    val totalEarnings: Double = 0.0,
    val completedAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val totalPayments: Int = 0,
    val completedPayments: Int = 0,
    val totalOrders: Int = 0
)