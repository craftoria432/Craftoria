package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.utils.PaymentValidator
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PaymentRepository {
    private val db = FirebaseFirestore.getInstance()
    // ✅ CRITICAL: Using correct collection name "payments" not "seller_payments"
    // Payments are created here when order is placed, queried when order completes
    private val paymentsCollection = db.collection("payments")
    private val notificationsCollection = db.collection("notifications")

    companion object {
        private const val TAG = "PaymentRepository"

        class UnauthorizedAccessException(message: String) : SecurityException(message)

        // ─────────────────────────────────────────────────────────────────────
        // ROOT CAUSE OF THE CRASH
        //
        // Firestore's toObject(SellerPayment::class.java) uses reflection to
        // map document fields to Kotlin properties by their @PropertyName
        // annotations. When a field is declared as Long? in the data class,
        // Firestore knows it must produce a Long and crashes with:
        //
        //   "Failed to convert a value of type com.google.firebase.Timestamp
        //    to long (found in field 'refund_date')"
        //
        // Changing the field to Any? does NOT fix this — Firestore's reflective
        // deserializer still looks at the declared Kotlin type at runtime via
        // the getter/setter method signatures (because of the @PropertyName
        // annotations). The setter for `refundDate` in the compiled bytecode
        // is `setRefund_date(Object)`, but the deserializer inspects the
        // property type and tries to call the setter with a cast Long — and
        // crashes when Firestore has stored a Timestamp.
        //
        // THE ONLY RELIABLE FIX: never call toObject() on documents that
        // contain mixed-type timestamp fields. Instead, read each field
        // manually from the DocumentSnapshot via getString/getLong/get() and
        // construct the data class ourselves. This is done in parsePayment()
        // below, which is now the single place where SellerPayment objects are
        // built from Firestore data.
        // ─────────────────────────────────────────────────────────────────────

        /**
         * Safely read any timestamp field (Long, Timestamp, Number, Map, String)
         * and return milliseconds. Returns 0L for null or unrecognised types.
         */
        private fun anyToMillis(value: Any?): Long = when (value) {
            is Long      -> value
            is Timestamp -> value.toDate().time
            is Number    -> value.toLong()
            is String    -> value.toLongOrNull() ?: 0L
            is Map<*, *> -> {
                val s = (value["_seconds"] as? Long) ?: (value["seconds"] as? Long) ?: 0L
                val n = (value["_nanoseconds"] as? Long) ?: (value["nanoseconds"] as? Long) ?: 0L
                s * 1_000 + n / 1_000_000
            }
            null         -> 0L
            else         -> 0L
        }

        /**
         * Manually parse a Firestore DocumentSnapshot into a SellerPayment.
         *
         * Every field is read with the appropriate typed accessor (getString,
         * getLong, getBoolean, get) so there is never an implicit type cast.
         * Timestamp fields use anyToMillis() which handles all storage formats.
         */
        fun parsePayment(doc: DocumentSnapshot): SellerPayment? {
            return try {
                val data = doc.data ?: return null

                // ── Timestamp fields (any of Long / Timestamp / Map) ──────────
                val createdAt  = anyToMillis(data["created_at"])
                val updatedAt  = anyToMillis(data["updated_at"])
                val paymentDateRaw = data["payment_date"]
                val paymentDate: Long? = if (paymentDateRaw == null) null
                    else anyToMillis(paymentDateRaw).takeIf { it > 0L }
                val origDateRaw = data["original_transaction_date"]
                val originalTransactionDate: Long? = if (origDateRaw == null) null
                    else anyToMillis(origDateRaw).takeIf { it > 0L }

                // refund_date is stored as a raw value so we keep it as Any?
                // but we read it here through anyToMillis to validate it parses:
                val refundDateRaw = data["refund_date"]
                // Store as Long? — already safe because we converted it:
                val refundDateLong: Long? = if (refundDateRaw == null) null
                    else anyToMillis(refundDateRaw).takeIf { it > 0L }

                // ── Scalar fields ─────────────────────────────────────────────
                val id         = doc.id
                val sellerId   = doc.getString("seller_id") ?: ""
                val sellerName = doc.getString("seller_name") ?: ""
                val orderId    = doc.getString("order_id") ?: ""
                val coSellerStoreId = doc.getString("co_seller_store_id") ?: ""
                val storeName  = doc.getString("store_name") ?: ""
                val buyerId    = doc.getString("buyer_id") ?: ""
                val buyerName  = doc.getString("buyer_name") ?: ""
                val amount     = (data["amount"] as? Number)?.toDouble() ?: 0.0
                val paymentMethod = doc.getString("payment_method") ?: "Cash on Delivery"
                val transactionId = doc.getString("transaction_id") ?: ""
                val status     = doc.getString("status") ?: PaymentStatus.PENDING.toString()
                val itemsCount = (data["items_count"] as? Number)?.toInt() ?: 0
                val refundAmount = (data["refund_amount"] as? Number)?.toDouble() ?: 0.0
                val refundReason = doc.getString("refund_reason") ?: ""
                val idempotencyKey = doc.getString("idempotency_key") ?: ""
                val requestId  = doc.getString("request_id") ?: ""

                // ── List fields ───────────────────────────────────────────────
                @Suppress("UNCHECKED_CAST")
                val involvedSellerIds = (data["involved_seller_ids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val itemsDetails = (data["items_details"] as? List<*>)?.mapNotNull { item ->
                    (item as? Map<*, *>)?.let { m ->
                        PaymentItemDetail(
                            productId    = m["product_id"] as? String ?: "",
                            productTitle = m["product_title"] as? String ?: "",
                            quantity     = (m["quantity"] as? Number)?.toInt() ?: 1,
                            price        = (m["price"] as? Number)?.toDouble() ?: 0.0,
                            itemTotal    = (m["item_total"] as? Number)?.toDouble() ?: 0.0
                        )
                    }
                } ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val paymentSplits = (data["payment_splits"] as? List<*>)?.mapNotNull { split ->
                    (split as? Map<*, *>)?.let { m ->
                        PaymentSplit(
                            sellerId         = m["seller_id"] as? String ?: "",
                            sellerName       = m["seller_name"] as? String ?: "",
                            splitPercentage  = (m["split_percentage"] as? Number)?.toDouble() ?: 0.0,
                            splitAmount      = (m["split_amount"] as? Number)?.toDouble() ?: 0.0,
                            status           = m["status"] as? String ?: PaymentStatus.PENDING.toString()
                        )
                    }
                } ?: emptyList()

                SellerPayment(
                    id                      = id,
                    sellerId                = sellerId,
                    sellerName              = sellerName,
                    orderId                 = orderId,
                    coSellerStoreId         = coSellerStoreId,
                    storeName               = storeName,
                    buyerId                 = buyerId,
                    buyerName               = buyerName,
                    involvedSellerIds       = involvedSellerIds,
                    paymentSplits           = paymentSplits,
                    amount                  = amount,
                    paymentMethod           = paymentMethod,
                    transactionId           = transactionId,
                    status                  = status,
                    paymentDate             = paymentDate,
                    originalTransactionDate = originalTransactionDate,
                    itemsCount              = itemsCount,
                    itemsDetails            = itemsDetails,
                    createdAt               = createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    updatedAt               = updatedAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    refundAmount            = refundAmount,
                    refundReason            = refundReason,
                    // Store as Long? wrapped in Any? so the model field type is satisfied
                    refundDate              = refundDateLong,
                    idempotencyKey          = idempotencyKey,
                    requestId               = requestId
                )
            } catch (e: Exception) {
                Log.e(TAG, "parsePayment failed for doc ${doc.id}: ${e.message}", e)
                null
            }
        }
    }

    /* ==================== PAYMENT PROCESSING ==================== */

    suspend fun processOrderPayments(order: Order): Result<List<String>> {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💳 Processing payments for order: ${order.id}")
            val paymentIds = mutableListOf<String>()
            val itemsToProcess = if (order.items.isNotEmpty()) {
                Log.d(TAG, "📦 New format order with ${order.items.size} items")
                order.items
            } else if (order.productId.isNotEmpty()) {
                Log.d(TAG, "📦 Legacy format order — converting")
                listOf(OrderItem(productId    = order.productId,
                    sellerId     = order.sellerId,
                    sellerName   = order.sellerName,
                    productTitle = order.productTitle,
                    productImage = order.productImage,
                    quantity     = order.quantity,
                    price        = order.productPrice))
            } else {
                Log.w(TAG, "⚠️ Order has no items and no legacy product data")
                emptyList()
            }
            val itemsBySellerMap   = itemsToProcess.groupBy { it.sellerId }
            val involvedSellerIds  = itemsBySellerMap.keys.toList()
            Log.d(TAG, "👥 Sellers involved: ${itemsBySellerMap.size}")
            itemsBySellerMap.forEach { (sellerId, sellerItems) ->
                try {
                    val sellerAmount = sellerItems.sumOf { it.price * it.quantity }
                    val itemsCount   = sellerItems.sumOf { it.quantity }
                    val paymentCoSellerStoreId =
                        if (order.coSellerStoreId.isNotEmpty()) order.coSellerStoreId else ""
                    val payment = SellerPayment(sellerId        = sellerId,
                        sellerName      = sellerItems.first().sellerName,
                        orderId         = order.id,
                        coSellerStoreId = paymentCoSellerStoreId,
                        storeName       = order.sellerName,
                        buyerId         = order.buyerId,
                        buyerName       = order.buyerName,
                        amount          = sellerAmount,
                        paymentMethod   = order.paymentMethod,
                        status          = PaymentStatus.PENDING.toString(),
                        itemsCount      = itemsCount,
                        itemsDetails    = sellerItems.map { item ->
                            PaymentItemDetail(productId    = item.productId,
                                productTitle = item.productTitle,
                                quantity     = item.quantity,
                                price        = item.price,
                                itemTotal    = item.price * item.quantity)
                        },
                        createdAt            = System.currentTimeMillis(),
                        updatedAt            = System.currentTimeMillis(),
                        involvedSellerIds    = involvedSellerIds)
                    val docRef    = paymentsCollection.add(payment.toMap()).await()
                    val paymentId = docRef.id
                    paymentIds.add(paymentId)
                    paymentsCollection.document(paymentId).update("id", paymentId).await()
                    Log.d(TAG, "✅ Payment created: $paymentId")
                    sendPaymentNotification(sellerId   = sellerId,
                        sellerName = sellerItems.first().sellerName,
                        orderId    = order.id,
                        amount     = sellerAmount,
                        itemsCount = itemsCount,
                        buyerName  = order.buyerName)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing payment for seller: $sellerId", e)
                }
            }
            Log.d(TAG, "✅ All payments processed: ${paymentIds.size}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.success(paymentIds)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process order payments", e)
            Result.failure(e)
        }
    }

    /* ==================== SELLER PAYMENT QUERIES ==================== */

    suspend fun getSellerPayments(sellerId: String,
        requestingUserId: String,
        status: PaymentStatus? = null): Result<List<SellerPayment>> {
        return try {
            if (sellerId != requestingUserId) {
                Log.w(TAG, "🚫 UNAUTHORIZED: $requestingUserId tried to access payments for $sellerId")
                return Result.failure(UnauthorizedAccessException("Unauthorized: Cannot access other seller's payments"))
            }
            var query: Query = paymentsCollection.whereEqualTo("seller_id", sellerId)
            if (status != null) query = query.whereEqualTo("status", status.toString())
            val snapshot = query.get().await()
            val payments = snapshot.documents.mapNotNull { parsePayment(it) }.sortedByDescending { it.getCreatedAtLong() }
            Log.d(TAG, "✅ Fetched ${payments.size} seller payments")
            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch seller payments", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for seller payments.
     * Automatically updates whenever payment data changes.
     */
    fun listenToSellerPayments(
        sellerId: String,
        requestingUserId: String,
        onUpdate: (List<SellerPayment>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        Log.d(TAG, "🔔 Setting up real-time listener for seller: $sellerId")
        
        return paymentsCollection
            .whereEqualTo("seller_id", sellerId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Real-time listener error", error)
                    onError(error)
                    return@addSnapshotListener
                }
                
                try {
                    val payments = snapshot?.documents?.mapNotNull { parsePayment(it) } ?: emptyList()
                    Log.d(TAG, "✅ Real-time update: ${payments.size} payments for seller $sellerId")
                    onUpdate(payments)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing real-time update", e)
                    onError(e)
                }
            }
    }

    suspend fun getPaymentById(paymentId: String, requestingUserId: String): Result<SellerPayment?> {
        return try {
            val doc     = paymentsCollection.document(paymentId).get().await()
            val payment = parsePayment(doc) ?: return Result.success(null)
            if (payment.sellerId != requestingUserId) {
                Log.w(TAG, "🚫 UNAUTHORIZED: $requestingUserId tried to access payment $paymentId")
                return Result.failure(UnauthorizedAccessException("Unauthorized: Cannot access other seller's payment"))
            }
            Result.success(payment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get payment", e)
            Result.failure(e)
        }
    }

    suspend fun getOrderPayments(orderId: String, requestingUserId: String): Result<List<SellerPayment>> {
        return try {
            val snapshot = paymentsCollection.whereEqualTo("order_id", orderId).get().await()
            val payments = snapshot.documents.mapNotNull { parsePayment(it) }.sortedByDescending { it.getCreatedAtLong() }
            if (payments.isEmpty()) return Result.success(emptyList())
            val isUserSeller          = payments.any { it.sellerId == requestingUserId }
            val isUserBuyerInPayments = payments.any { it.buyerId == requestingUserId }
            var isUserBuyerInOrder = false
            if (!isUserSeller && !isUserBuyerInPayments) {
                try {
                    val orderBuyerId = db.collection("orders").document(orderId).get().await().getString("buyer_id") ?: ""
                    isUserBuyerInOrder = orderBuyerId == requestingUserId
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to check order buyer", e)
                }
            }
            if (!isUserSeller && !isUserBuyerInPayments && !isUserBuyerInOrder) {
                return Result.failure(UnauthorizedAccessException("Unauthorized: Not involved in this order"))
            }
            Log.d(TAG, "✅ Fetched ${payments.size} order payments")
            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch order payments", e)
            Result.failure(e)
        }
    }

    /* ==================== BUYER PAYMENT QUERIES ==================== */

    /**
     * Get all payments made by a buyer.
     *
     * Previously used toObject(SellerPayment::class.java) which crashed with:
     *   "Failed to convert a value of type com.google.firebase.Timestamp to long
     *    (found in field 'refund_date')"
     *
     * Now uses parsePayment() which reads every field manually.
     */
    suspend fun getBuyerPayments(buyerId: String): Result<List<SellerPayment>> {
        return try {
            Log.d(TAG, "📊 Fetching payments for buyer: $buyerId")
            val snapshot = paymentsCollection.whereEqualTo("buyer_id", buyerId).get().await()
            val payments = snapshot.documents.mapNotNull { doc ->
                parsePayment(doc).also { result ->
                    if (result == null)
                        Log.w(TAG, "⚠️ Skipped unparseable payment doc: ${doc.id}")
                }
            }.sortedByDescending { it.getCreatedAtLong() }
            Log.d(TAG, "✅ Fetched ${payments.size} payments for buyer")
            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch buyer payments", e)
            Result.failure(e)
        }
    }

    suspend fun getBuyerPaymentStats(buyerId: String): Result<com.gcuf.craftoria.viewmodel.BuyerPaymentStats> {
        return try {
            val snapshot = paymentsCollection.whereEqualTo("buyer_id", buyerId).get().await()
            val payments = snapshot.documents.mapNotNull { parsePayment(it) }
            val activeStatuses = setOf(PaymentStatus.COMPLETED.toString(),
                PaymentStatus.PENDING.toString(),
                PaymentStatus.PROCESSING.toString())
            val active    = payments.filter { it.status in activeStatuses }
            val completed = active.filter { it.status == PaymentStatus.COMPLETED.toString() }
            Result.success(com.gcuf.craftoria.viewmodel.BuyerPaymentStats(
                totalSpent        = active.sumOf { it.amount },
                completedAmount   = completed.sumOf { it.amount },
                pendingAmount     = active.filter { it.status == PaymentStatus.PENDING.toString() }.sumOf { it.amount },
                totalPayments     = active.size,
                completedPayments = completed.size,
                totalOrders       = active.map { it.orderId }.distinct().size,
                totalSellers      = active.map { it.sellerId }.distinct().size))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to calculate buyer payment stats", e)
            Result.failure(e)
        }
    }

    /* ==================== PAYMENT STATUS UPDATES ==================== */

    suspend fun updatePaymentStatus(paymentId: String,
        newStatus: PaymentStatus,
        transactionId: String = ""): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>("status"     to newStatus.toString(),
                "updated_at" to System.currentTimeMillis())
            if (transactionId.isNotEmpty()) updates["transaction_id"] = transactionId
            if (newStatus == PaymentStatus.COMPLETED) updates["payment_date"] = System.currentTimeMillis()
            paymentsCollection.document(paymentId).update(updates).await()
            Log.d(TAG, "✅ Payment status updated: $paymentId → $newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update payment status", e)
            Result.failure(e)
        }
    }

    suspend fun markPaymentCompleted(paymentId: String, transactionId: String): Result<Unit> {
        return try {
            paymentsCollection.document(paymentId).update(mapOf("status"         to PaymentStatus.COMPLETED.toString(),
                "transaction_id" to transactionId,
                "payment_date"   to System.currentTimeMillis(),
                "updated_at"     to System.currentTimeMillis())).await()
            Log.d(TAG, "✅ Payment marked completed: $paymentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to mark payment completed", e)
            Result.failure(e)
        }
    }

    suspend fun processRefund(paymentId: String, refundAmount: Double, reason: String): Result<Unit> {
        return try {
            paymentsCollection.document(paymentId).update(mapOf("status"        to PaymentStatus.REFUNDED.toString(),
                "refund_amount" to refundAmount,
                "refund_reason" to reason,
                // Store as Long — avoids creating a Timestamp in Firestore
                // which would then crash older app versions reading it as Long
                "refund_date"   to System.currentTimeMillis(),
                "updated_at"    to System.currentTimeMillis())).await()
            Log.d(TAG, "✅ Refund processed: $paymentId, amount=$refundAmount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process refund", e)
            Result.failure(e)
        }
    }

    /* ==================== STATISTICS ==================== */

    suspend fun getSellerPaymentStats(sellerId: String): Result<SellerPaymentStats> {
        return try {
            val snapshot = paymentsCollection.whereEqualTo("seller_id", sellerId).get().await()
            val payments = snapshot.documents.mapNotNull { parsePayment(it) }
            
            // ✅ FIX: Exclude refunded payments from active payment count
            // Also exclude refund_pending and refund_processing from earnings (money hasn't moved yet)
            val activePayments = payments.filter { 
                it.status.lowercase() !in listOf("refunded", "refund_pending", "refund_processing", "refund_rejected")
            }
            val completed = activePayments.filter { it.status == PaymentStatus.COMPLETED.toString() }
            
            Result.success(SellerPaymentStats(
                totalEarnings     = activePayments.sumOf { it.amount },  // ✅ Only active payments
                completedAmount   = completed.sumOf { it.amount },
                pendingAmount     = activePayments.filter { it.status == PaymentStatus.PENDING.toString() }.sumOf { it.amount },
                totalPayments     = activePayments.size,  // ✅ Only count active payments
                completedPayments = completed.size,
                totalOrders       = activePayments.map { it.orderId }.distinct().size))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to calculate seller payment stats", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for seller payment statistics.
     * Automatically updates whenever payment data changes.
     */
    fun listenToSellerPaymentStats(
        sellerId: String,
        onUpdate: (SellerPaymentStats) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        Log.d(TAG, "🔔 Setting up real-time stats listener for seller: $sellerId")
        
        return paymentsCollection
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Real-time stats listener error", error)
                    onError(error)
                    return@addSnapshotListener
                }
                
                try {
                    val payments = snapshot?.documents?.mapNotNull { parsePayment(it) } ?: emptyList()
                    
                    // ✅ FIX: Exclude refunded payments from active payment count
                    // Also exclude refund_pending and refund_processing from earnings (money hasn't moved yet)
                    val activePayments = payments.filter { 
                        it.status.lowercase() !in listOf("refunded", "refund_pending", "refund_processing", "refund_rejected")
                    }
                    val completed = activePayments.filter { it.status == PaymentStatus.COMPLETED.toString() }
                    
                    val stats = SellerPaymentStats(
                        totalEarnings     = activePayments.sumOf { it.amount },  // ✅ Only active payments
                        completedAmount   = completed.sumOf { it.amount },
                        pendingAmount     = activePayments.filter { it.status == PaymentStatus.PENDING.toString() }.sumOf { it.amount },
                        totalPayments     = activePayments.size,  // ✅ Only count active payments
                        completedPayments = completed.size,
                        totalOrders       = activePayments.map { it.orderId }.distinct().size
                    )
                    
                    Log.d(TAG, "✅ Real-time stats update for seller $sellerId: Total PKR ${stats.totalEarnings}, Active Payments: ${stats.totalPayments}")
                    onUpdate(stats)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing real-time stats update", e)
                    onError(e)
                }
            }
    }

    /* ==================== NOTIFICATIONS ==================== */

    private suspend fun sendPaymentNotification(sellerId: String,
        @Suppress("UNUSED_PARAMETER") sellerName: String,
        orderId: String,
        amount: Double,
        itemsCount: Int,
        buyerName: String) {
        try {
            com.gcuf.craftoria.utils.NotificationHelper.notifyPaymentReceived(sellerId    = sellerId,
                orderId     = orderId,
                orderNumber = orderId.take(8),
                amount      = amount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send payment notification", e)
        }
    }

    /* ==================== IDEMPOTENCY ==================== */

    suspend fun processOrderPaymentsWithIdempotency(order: Order,
        idempotencyKey: String): Result<List<String>> {
        return try {
            // ✅ FIX: Check ONLY order_id to prevent duplicate payments
            // If we check both order_id AND idempotency_key, retries with different keys
            // will create new payments even though payments already exist for this order
            val existing = paymentsCollection.whereEqualTo("order_id", order.id).get().await()
            if (!existing.isEmpty) {
                Log.d(TAG, "✅ Payments already exist for order ${order.id} — skipping creation")
                return Result.success(existing.documents.map { it.id })
            }
            
            // Only create if no payments exist for this order
            val result = processOrderPayments(order)
            if (result.isSuccess) {
                result.getOrNull()?.forEach { paymentId ->
                    paymentsCollection.document(paymentId).update(mapOf("idempotency_key" to idempotencyKey,
                        "request_id"      to UUID.randomUUID().toString())).await()
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to process payment with idempotency", e)
            Result.failure(e)
        }
    }

    suspend fun validateOrderPayment(order: Order, items: List<OrderItem>): Result<Unit> {
        return try {
            val validation = PaymentValidator.validateOrderPayment(order, items)
            if (!validation.isValid)
                return Result.failure(Exception(validation.errors.joinToString(", ")))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Payment validation failed", e)
            Result.failure(e)
        }
    }

    suspend fun verifyPaymentsExist(orderId: String): Result<List<SellerPayment>> {
        return try {
            val snapshot = paymentsCollection.whereEqualTo("order_id", orderId).get().await()
            val payments = snapshot.documents.mapNotNull { parsePayment(it) }
            Log.d(TAG, "✅ Found ${payments.size} payment(s) for order $orderId")
            Result.success(payments)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to verify payments for order $orderId", e)
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
