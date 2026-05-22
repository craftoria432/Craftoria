package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.services.RefundNotificationService
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RefundRepository(
    private val firestore: FirebaseFirestore,
    private val notificationService: RefundNotificationService = RefundNotificationService(firestore)
) {
    companion object {
        private const val TAG = "RefundRepository"
        private const val REFUNDS_COLLECTION = "refunds"
        private const val ORDERS_COLLECTION = "orders"
        // ✅ CRITICAL: Using correct collection name "payments" (not "seller_payments")
        private const val PAYMENTS_COLLECTION = "payments"
        private const val NOTIFICATIONS_COLLECTION = "notifications"

        /** Convert any Firestore timestamp representation to Long milliseconds. */
        private fun tsToLong(value: Any?): Long = when (value) {
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

        /** Nullable variant — returns null when the field is absent/null. */
        private fun tsToLongOrNull(value: Any?): Long? =
            if (value == null) null else tsToLong(value).takeIf { it > 0L }

        /**
         * Manually deserialise a Firestore document into a RefundRequest.
         * Never uses toObject() — avoids Timestamp→Long coercion crash.
         */
        fun parseRefund(doc: DocumentSnapshot): RefundRequest? {
            return try {
                val data = doc.data ?: return null

                @Suppress("UNCHECKED_CAST")
                val refundSplits = (data["refund_splits"] as? List<*>)?.mapNotNull { split ->
                    (split as? Map<*, *>)?.let { m ->
                        RefundSplit(
                            sellerId            = m["seller_id"] as? String ?: "",
                            sellerName          = m["seller_name"] as? String ?: "",
                            originalSplitAmount = (m["original_split_amount"] as? Number)?.toDouble() ?: 0.0,
                            refundSplitAmount   = (m["refund_split_amount"] as? Number)?.toDouble() ?: 0.0,
                            status              = m["status"] as? String ?: RefundStatus.REQUESTED.toString(),
                            gatewayRefundId     = m["gateway_refund_id"] as? String ?: ""
                        )
                    }
                } ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                val auditTrail = (data["audit_trail"] as? List<*>)?.mapNotNull { entry ->
                    (entry as? Map<*, *>)?.let { m ->
                        RefundAuditEntry(
                            action    = m["action"] as? String ?: "",
                            actor     = m["actor"] as? String ?: "",
                            actorName = m["actor_name"] as? String ?: "",
                            notes     = m["notes"] as? String ?: "",
                            timestamp = tsToLong(m["timestamp"])
                        )
                    }
                } ?: emptyList()

                RefundRequest(
                    id              = doc.id,
                    orderId         = doc.getString("order_id") ?: "",
                    paymentId       = doc.getString("payment_id") ?: "",
                    buyerId         = doc.getString("buyer_id") ?: "",
                    buyerName       = doc.getString("buyer_name") ?: "",
                    sellerId        = doc.getString("seller_id") ?: "",
                    sellerName      = doc.getString("seller_name") ?: "",
                    refundType      = doc.getString("refund_type") ?: RefundType.FULL.toString(),
                    originalAmount  = (data["original_amount"] as? Number)?.toDouble() ?: 0.0,
                    refundAmount    = (data["refund_amount"] as? Number)?.toDouble() ?: 0.0,
                    reason          = doc.getString("reason") ?: "",
                    reasonDetails   = doc.getString("reason_details") ?: "",
                    status          = doc.getString("status") ?: RefundStatus.REQUESTED.toString(),
                    initiatedBy     = doc.getString("initiated_by") ?: "",
                    approvedBy      = doc.getString("approved_by") ?: "",
                    approvalNotes   = doc.getString("approval_notes") ?: "",
                    rejectionCount  = (data["rejection_count"] as? Number)?.toInt() ?: 0,
                    canResubmit     = data["can_resubmit"] as? Boolean ?: true,
                    finalDecision   = data["final_decision"] as? Boolean ?: false,
                    paymentMethod   = doc.getString("payment_method") ?: "Cash on Delivery",
                    transactionId   = doc.getString("transaction_id") ?: "",
                    gatewayRefundId = doc.getString("gateway_refund_id") ?: "",
                    retryCount      = (data["retry_count"] as? Number)?.toInt() ?: 0,
                    errorMessage    = doc.getString("error_message") ?: "",
                    idempotencyKey  = doc.getString("idempotency_key") ?: "",
                    requestedAt     = tsToLong(data["requested_at"]).takeIf { it > 0L }
                        ?: System.currentTimeMillis(),
                    approvedAt      = tsToLongOrNull(data["approved_at"]),
                    processedAt     = tsToLongOrNull(data["processed_at"]),
                    completedAt     = tsToLongOrNull(data["completed_at"])
                        ?: tsToLongOrNull(data["refund_date"]),
                    createdAt       = tsToLong(data["created_at"]).takeIf { it > 0L }
                        ?: System.currentTimeMillis(),
                    updatedAt       = tsToLong(data["updated_at"]).takeIf { it > 0L }
                        ?: System.currentTimeMillis(),
                    lastRetryAt     = tsToLongOrNull(data["last_retry_at"]),
                    refundSplits    = refundSplits,
                    auditTrail      = auditTrail
                )
            } catch (e: Exception) {
                Log.e(TAG, "parseRefund failed for ${doc.id}: ${e.message}", e)
                null
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun createRefundRequest(
        orderId: String, paymentId: String, buyerId: String, buyerName: String,
        sellerId: String, sellerName: String, refundType: String, originalAmount: Double,
        refundAmount: Double, reason: String, reasonDetails: String, paymentMethod: String,
        transactionId: String, initiatedBy: String
    ): Result<RefundRequest> {
        return try {
            // ✅ CRITICAL FIX #8: Idempotency key must be deterministic, not random.
            // Key = "${paymentId}_${buyerId}_${initiatedBy}" ensures:
            // 1. Same buyer can't create duplicate refund requests for same payment
            // 2. Seller can make independent refund of same payment
            // 3. On network retry, checkDuplicateRefund() finds the original record
            val idempotencyKey = "${paymentId}_${buyerId}_${initiatedBy}"
            val existingRefund = checkDuplicateRefund(idempotencyKey).getOrNull()
            if (existingRefund != null) {
                Log.w(TAG, "Duplicate refund attempt detected (idempotency key already exists): $idempotencyKey")
                return Result.success(existingRefund)
            }

            val refundId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val refundRequest = RefundRequest(
                id = refundId, orderId = orderId, paymentId = paymentId,
                buyerId = buyerId, buyerName = buyerName,
                sellerId = sellerId, sellerName = sellerName,
                refundType = refundType, originalAmount = originalAmount,
                refundAmount = refundAmount, reason = reason,
                reasonDetails = reasonDetails, paymentMethod = paymentMethod,
                transactionId = transactionId, initiatedBy = initiatedBy,
                status = RefundStatus.REQUESTED.toString(),
                requestedAt = now, createdAt = now, updatedAt = now,
                idempotencyKey = idempotencyKey,
                auditTrail = listOf(
                    RefundAuditEntry(
                        action    = "requested",
                        actor     = if (initiatedBy == "buyer") buyerId else sellerId,
                        actorName = if (initiatedBy == "buyer") buyerName else sellerName,
                        notes     = "Refund request initiated by $initiatedBy",
                        timestamp = now
                    )
                )
            )

            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .set(refundRequest.toMap())
                .await()

            updatePaymentRefundStatus(paymentId, PaymentStatus.REFUND_PENDING.toString())
            updateOrderRefundStatusToRequested(orderId)

            // ── Routing based on who initiated ────────────────────────────────
            if (initiatedBy == "seller") {
                // Seller-initiated: notify admin for approval (fraud gate)
                notifyAdminSellerInitiatedRefund(
                    refundId    = refundId,
                    orderId     = orderId,
                    sellerId    = sellerId,
                    sellerName  = sellerName,
                    buyerName   = buyerName,
                    refundAmount = refundAmount,
                    reason      = reason
                )
            } else if (initiatedBy == "buyer") {
                // ✅ FIX: Buyer-initiated refunds — notify seller for approval
                // This path (RefundRepository.createRefundRequest) is used by
                // SellerPaymentViewModel.initiateSellerRefund() and direct API calls.
                // RefundProcessor.initiateRefund() uses a different path but also
                // sends the same notification. Ensure both paths reach the seller.
                notificationService.notifyRefundRequested(refundRequest)
            }

            Log.d(TAG, "Refund request created: $refundId (initiatedBy=$initiatedBy)")
            Result.success(refundRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating refund request", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVE
    //
    // Business logic:
    //   • Buyer-initiated refund  → seller approves → auto-complete (COD flow)
    //   • Seller-initiated refund → admin approves  → complete
    //
    // The screen calling this function passes its own userId as `approvedBy`.
    // We inspect `refund.initiatedBy` to decide what happens next.
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun approveRefund(
        refundId: String, approvedBy: String, approverName: String, approvalNotes: String = ""
    ): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()

            // Fetch current refund to inspect initiatedBy
            val currentRefund = getRefundById(refundId).getOrNull()
                ?: return Result.failure(Exception("Refund not found"))

            val isAdminActor = approvedBy.contains("admin", ignoreCase = true) ||
                    approvedBy == "system"
            val isSellerInitiated = currentRefund.initiatedBy == "seller"

            val approvalStatus = when {
                isAdminActor -> RefundStatus.APPROVED_BY_ADMIN.toString()
                else         -> RefundStatus.APPROVED_BY_SELLER.toString()
            }

            firestore.collection(REFUNDS_COLLECTION).document(refundId).update(
                mapOf(
                    "status"         to approvalStatus,
                    "approved_by"    to approvedBy,
                    "approved_at"    to now,
                    "updated_at"     to now,
                    "approval_notes" to approvalNotes
                )
            ).await()

            addAuditEntry(refundId, "approved", approvedBy, approverName,
                "Refund approved: ${approvalNotes.ifEmpty { "No notes" }}")

            val refund = getRefundById(refundId).getOrNull()

            if (refund != null) {
                notificationService.notifyRefundApproved(refund)

                when {
                    // ── Case 1: Seller-initiated, approved by ADMIN ───────────
                    // Admin is the final gate → complete immediately
                    isSellerInitiated && isAdminActor -> {
                        Log.d(TAG, "Seller-initiated refund approved by admin → completing: $refundId")
                        val completeResult = completeRefund(refundId)
                        if (completeResult.isSuccess) {
                            val completedRefund = getRefundById(refundId).getOrNull()
                            return Result.success(completedRefund ?: RefundRequest())
                        } else {
                            val err = completeResult.exceptionOrNull()
                            Log.e(TAG, "Failed to complete seller-initiated refund: ${err?.message}")
                            return Result.failure(err ?: Exception("Completion failed"))
                        }
                    }

                    // ── Case 2: Seller-initiated, approved by SELLER ─────────
                    // This shouldn't happen (seller can't approve their own refund)
                    // but guard it: just leave at APPROVED_BY_SELLER, admin will act
                    isSellerInitiated && !isAdminActor -> {
                        Log.w(TAG, "Seller-initiated refund approved by seller — waiting for admin: $refundId")
                        // Notify admin to take final action
                        notifyAdminSellerRefundNeedsDecision(
                            refundId    = refundId,
                            orderId     = refund.orderId,
                            sellerId    = refund.sellerId,
                            sellerName  = refund.sellerName,
                            buyerName   = refund.buyerName,
                            refundAmount = refund.refundAmount,
                            reason      = refund.reason
                        )
                        return Result.success(refund)
                    }

                    // ── Case 3: Buyer-initiated, approved by SELLER ──────────
                    // Original flow: seller approves buyer's request → auto-complete
                    else -> {
                        Log.d(TAG, "Buyer-initiated refund approved by seller → auto-completing: $refundId")
                        val completeResult = completeRefund(refundId)
                        if (completeResult.isSuccess) {
                            val completedRefund = getRefundById(refundId).getOrNull()
                            return Result.success(completedRefund ?: RefundRequest())
                        } else {
                            val err = completeResult.exceptionOrNull()
                            Log.e(TAG, "Failed to auto-complete buyer-initiated refund: ${err?.message}")
                            return Result.failure(err ?: Exception("Auto-complete failed"))
                        }
                    }
                }
            }

            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error approving refund", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REJECT
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun rejectRefund(
        refundId: String, rejectedBy: String, rejectorName: String, rejectionReason: String
    ): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()
            val currentRefund = getRefundById(refundId).getOrNull()
                ?: return Result.failure(Exception("Refund not found"))

            val newRejectionCount = currentRefund.rejectionCount + 1
            val isFinalDecision   = newRejectionCount >= 2
            val canResubmit       = newRejectionCount < 2

            val rejectionStatus =
                if (rejectedBy.contains("admin", ignoreCase = true) || rejectedBy == "system")
                    RefundStatus.REJECTED_BY_ADMIN.toString()
                else
                    RefundStatus.REJECTED_BY_SELLER.toString()

            firestore.collection(REFUNDS_COLLECTION).document(refundId).update(
                mapOf(
                    "status"           to rejectionStatus,
                    "approved_by"      to rejectedBy,
                    "approval_notes"   to rejectionReason,
                    "rejection_count"  to newRejectionCount,
                    "can_resubmit"     to canResubmit,
                    "final_decision"   to isFinalDecision,
                    "updated_at"       to now
                )
            ).await()

            val auditMsg = if (isFinalDecision)
                "Refund rejected (FINAL): $rejectionReason"
            else
                "Refund rejected (attempt $newRejectionCount/2): $rejectionReason"
            addAuditEntry(refundId, "rejected", rejectedBy, rejectorName, auditMsg)

            val refund = getRefundById(refundId).getOrNull()
            if (refund != null) {
                updatePaymentRefundStatus(refund.paymentId, PaymentStatus.REFUND_REJECTED.toString())
                updateOrderRefundStatusToRejected(refund.orderId)
                notificationService.notifyRefundRejected(refund, rejectionReason)
            }

            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting refund", e)
            Result.failure(e)
        }
    }

    suspend fun processRefund(refundId: String, gatewayRefundId: String = ""): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()
            firestore.collection(REFUNDS_COLLECTION).document(refundId).update(
                mapOf(
                    "status"            to RefundStatus.PROCESSING.toString(),
                    "gateway_refund_id" to gatewayRefundId,
                    "processed_at"      to now,
                    "updated_at"        to now
                )
            ).await()

            addAuditEntry(refundId, "processing", "system", "System", "Processing: $gatewayRefundId")

            val refund = getRefundById(refundId).getOrNull()
            if (refund != null) {
                updatePaymentRefundStatus(refund.paymentId, PaymentStatus.REFUND_PROCESSING.toString())
                notificationService.notifyRefundProcessing(refund)
            }

            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error processing refund", e)
            Result.failure(e)
        }
    }

    suspend fun completeRefund(refundId: String, gatewayRefundId: String = ""): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()
            firestore.collection(REFUNDS_COLLECTION).document(refundId).update(
                mapOf(
                    "status"            to RefundStatus.COMPLETED.toString(),
                    "gateway_refund_id" to gatewayRefundId,
                    "completed_at"      to now,
                    "updated_at"        to now
                )
            ).await()

            addAuditEntry(refundId, "completed", "system", "System", "Refund completed")

            val refund = getRefundById(refundId).getOrNull()
            if (refund != null) {
                notificationService.notifyRefundCompleted(refund)
                updateOrderRefundStatus(refund.orderId, true)
                updatePaymentRefundStatus(
                    refund.paymentId, PaymentStatus.REFUNDED.toString(),
                    refund.refundAmount, refund.reason, now
                )
            }

            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error completing refund", e)
            Result.failure(e)
        }
    }

    suspend fun markRefundFailed(refundId: String, errorMessage: String): Result<RefundRequest> {
        return try {
            val refund = getRefundById(refundId).getOrNull()
                ?: return Result.failure(Exception("Refund not found"))
            val now           = System.currentTimeMillis()
            val newRetryCount = refund.retryCount + 1

            firestore.collection(REFUNDS_COLLECTION).document(refundId).update(
                mapOf(
                    "status"        to RefundStatus.FAILED.toString(),
                    "error_message" to errorMessage,
                    "retry_count"   to newRetryCount,
                    "last_retry_at" to now,
                    "updated_at"    to now
                )
            ).await()

            addAuditEntry(refundId, "failed", "system", "System",
                "Failed (attempt $newRetryCount): $errorMessage")

            val updatedRefund = getRefundById(refundId).getOrNull()
            if (updatedRefund != null) notificationService.notifyRefundFailed(updatedRefund, errorMessage)

            Result.success(updatedRefund ?: refund)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking refund failed", e)
            Result.failure(e)
        }
    }

    suspend fun retryRefund(refundId: String): Result<RefundRequest> {
        return try {
            val refund = getRefundById(refundId).getOrNull()
                ?: return Result.failure(Exception("Refund not found"))
            if (!refund.canRetry()) return Result.failure(Exception("Max attempts reached"))

            val now = System.currentTimeMillis()
            firestore.collection(REFUNDS_COLLECTION).document(refundId).update(
                mapOf(
                    "status"        to RefundStatus.PROCESSING.toString(),
                    "last_retry_at" to now,
                    "updated_at"    to now
                )
            ).await()

            addAuditEntry(refundId, "retried", "system", "System",
                "Retry attempt ${refund.retryCount + 1}")

            Result.success(refund)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying refund", e)
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERIES — all use parseRefund() not toObject()
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getRefundById(refundId: String): Result<RefundRequest> {
        return try {
            val doc    = firestore.collection(REFUNDS_COLLECTION).document(refundId).get().await()
            val refund = parseRefund(doc)
                ?: return Result.failure(Exception("Refund not found or failed to parse"))
            Result.success(refund)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refund", e)
            Result.failure(e)
        }
    }

    suspend fun getRefundsByOrderId(orderId: String): Result<List<RefundRequest>> {
        return try {
            val snap = firestore.collection(REFUNDS_COLLECTION)
                .whereEqualTo("order_id", orderId)
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.documents.mapNotNull { parseRefund(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refunds by order", e)
            Result.failure(e)
        }
    }

    suspend fun getRefundsByBuyerId(buyerId: String): Result<List<RefundRequest>> {
        return try {
            val snap = firestore.collection(REFUNDS_COLLECTION)
                .whereEqualTo("buyer_id", buyerId)
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.documents.mapNotNull { parseRefund(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refunds by buyer", e)
            Result.failure(e)
        }
    }

    suspend fun getRefundsBySellerId(sellerId: String): Result<List<RefundRequest>> {
        return try {
            val snap = firestore.collection(REFUNDS_COLLECTION)
                .whereEqualTo("seller_id", sellerId)
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snap.documents.mapNotNull { parseRefund(it) })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refunds by seller", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingRefunds(): Result<List<RefundRequest>> = try {
        val snap = firestore.collection(REFUNDS_COLLECTION)
            .whereIn(
                "status", listOf(
                    RefundStatus.REQUESTED.toString(),
                    RefundStatus.UNDER_REVIEW.toString(),
                    RefundStatus.APPROVED_BY_SELLER.toString(),
                    RefundStatus.APPROVED_BY_ADMIN.toString()
                )
            )
            .orderBy("requested_at", Query.Direction.ASCENDING)
            .get().await()
        Result.success(snap.documents.mapNotNull { parseRefund(it) })
    } catch (e: Exception) {
        Log.e(TAG, "Error getting pending refunds", e)
        Result.failure(e)
    }

    suspend fun getFailedRefundsForRetry(): Result<List<RefundRequest>> = try {
        val snap = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("status", RefundStatus.FAILED.toString())
            .whereLessThan("retry_count", 3)
            .orderBy("retry_count", Query.Direction.ASCENDING)
            .orderBy("last_retry_at", Query.Direction.ASCENDING)
            .get().await()
        Result.success(snap.documents.mapNotNull { parseRefund(it) })
    } catch (e: Exception) {
        Log.e(TAG, "Error getting failed refunds", e)
        Result.failure(e)
    }

    suspend fun checkDuplicateRefund(idempotencyKey: String): Result<RefundRequest?> = try {
        val snap = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("idempotency_key", idempotencyKey)
            .get().await()
        Result.success(snap.documents.firstOrNull()?.let { parseRefund(it) })
    } catch (e: Exception) {
        Log.e(TAG, "Error checking duplicate refund", e)
        Result.failure(e)
    }

    suspend fun updateRefundSplits(refundId: String, splits: List<RefundSplit>): Result<Unit> = try {
        firestore.collection(REFUNDS_COLLECTION).document(refundId)
            .update(mapOf("refund_splits" to splits.map { it.toMap() })).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating splits", e)
        Result.failure(e)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN NOTIFICATION HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes a Firestore notification document for all admin users.
     * Admin dashboard listens to notifications collection filtered by role="admin".
     *
     * Called when a seller initiates a refund — admin must approve before any
     * money moves. This is the fraud-prevention gate.
     */
    private suspend fun notifyAdminSellerInitiatedRefund(
        refundId: String,
        orderId: String,
        sellerId: String,
        sellerName: String,
        buyerName: String,
        refundAmount: Double,
        reason: String
    ) {
        try {
            firestore.collection(NOTIFICATIONS_COLLECTION).add(
                mapOf(
                    "type"          to "SELLER_REFUND_REQUEST",
                    "target_role"   to "admin",
                    "title"         to "Seller Refund Request",
                    "body"          to "$sellerName has initiated a refund of PKR ${refundAmount.toInt()} " +
                            "for buyer $buyerName. Order #${orderId.take(8).uppercase()}. " +
                            "Reason: $reason. Admin approval required.",
                    "refund_id"     to refundId,
                    "order_id"      to orderId,
                    "seller_id"     to sellerId,
                    "refund_amount" to refundAmount,
                    "action_type"   to "REVIEW_REFUND",
                    "read"          to false,
                    "created_at"    to System.currentTimeMillis()
                )
            ).await()
            Log.d(TAG, "Admin notified of seller-initiated refund: $refundId")
        } catch (e: Exception) {
            // Non-fatal — refund is already created, notification failure shouldn't block
            Log.e(TAG, "Failed to notify admin of seller refund: ${e.message}")
        }
    }

    /**
     * Called in the rare case where a seller somehow approves their own
     * seller-initiated refund — notifies admin that final decision is needed.
     */
    private suspend fun notifyAdminSellerRefundNeedsDecision(
        refundId: String,
        orderId: String,
        sellerId: String,
        sellerName: String,
        buyerName: String,
        refundAmount: Double,
        reason: String
    ) {
        try {
            firestore.collection(NOTIFICATIONS_COLLECTION).add(
                mapOf(
                    "type"          to "SELLER_REFUND_AWAITING_ADMIN",
                    "target_role"   to "admin",
                    "title"         to "Seller Refund Awaiting Final Approval",
                    "body"          to "Seller-initiated refund by $sellerName (PKR ${refundAmount.toInt()}) " +
                            "for order #${orderId.take(8).uppercase()} needs your final decision.",
                    "refund_id"     to refundId,
                    "order_id"      to orderId,
                    "seller_id"     to sellerId,
                    "refund_amount" to refundAmount,
                    "action_type"   to "REVIEW_REFUND",
                    "read"          to false,
                    "created_at"    to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify admin for final refund decision: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun addAuditEntry(
        refundId: String, action: String, actor: String, actorName: String, notes: String
    ) {
        try {
            val entry = RefundAuditEntry(
                action    = action,
                actor     = actor,
                actorName = actorName,
                notes     = notes,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection(REFUNDS_COLLECTION).document(refundId)
                .update(mapOf("audit_trail" to FieldValue.arrayUnion(entry.toMap())))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding audit entry", e)
        }
    }

    private suspend fun updateOrderRefundStatus(orderId: String, isRefunded: Boolean) {
        try {
            if (isRefunded) {
                firestore.collection(ORDERS_COLLECTION).document(orderId)
                    .update(mapOf(
                        "refund_status" to com.gcuf.craftoria.data.model.OrderRefundStatus.COMPLETED.toString(),
                        "updated_at"    to System.currentTimeMillis()
                    ))
                    .await()
                Log.d(TAG, "Order refund status updated to COMPLETED: $orderId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order refund status", e)
        }
    }

    private suspend fun updateOrderRefundStatusToRequested(orderId: String) {
        try {
            firestore.collection(ORDERS_COLLECTION).document(orderId)
                .update(mapOf(
                    "refund_status" to com.gcuf.craftoria.data.model.OrderRefundStatus.REQUESTED.toString(),
                    "updated_at"    to System.currentTimeMillis()
                ))
                .await()
            Log.d(TAG, "Order refund status updated to REQUESTED: $orderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order refund status to REQUESTED", e)
        }
    }

    private suspend fun updateOrderRefundStatusToRejected(orderId: String) {
        try {
            firestore.collection(ORDERS_COLLECTION).document(orderId)
                .update(mapOf(
                    "refund_status" to com.gcuf.craftoria.data.model.OrderRefundStatus.REJECTED.toString(),
                    "updated_at"    to System.currentTimeMillis()
                ))
                .await()
            Log.d(TAG, "Order refund status updated to REJECTED: $orderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order refund status to REJECTED", e)
        }
    }

    suspend fun updatePaymentRefundInfo(
        paymentId: String, refundAmount: Double, refundReason: String, refundDate: Long
    ): Result<Unit> = try {
        firestore.collection(PAYMENTS_COLLECTION).document(paymentId).update(
            mapOf(
                "refund_amount" to refundAmount,
                "refund_reason" to refundReason,
                "refund_date"   to refundDate,
                "status"        to PaymentStatus.REFUNDED.toString(),
                "updated_at"    to System.currentTimeMillis()
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating payment refund info", e)
        Result.failure(e)
    }

    private suspend fun updatePaymentRefundStatus(
        paymentId: String,
        status: String,
        refundAmount: Double = 0.0,
        refundReason: String = "",
        refundDate: Long = 0L
    ): Result<Unit> = try {
        val map = mutableMapOf<String, Any>(
            "status"     to status,
            "updated_at" to System.currentTimeMillis()
        )
        if (refundAmount > 0)          map["refund_amount"] = refundAmount
        if (refundReason.isNotEmpty()) map["refund_reason"] = refundReason
        if (refundDate > 0)            map["refund_date"]   = refundDate

        firestore.collection(PAYMENTS_COLLECTION).document(paymentId).update(map).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating payment refund status", e)
        Result.failure(e)
    }
}