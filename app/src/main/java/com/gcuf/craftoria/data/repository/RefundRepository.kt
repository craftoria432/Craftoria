package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RefundRepository(private val firestore: FirebaseFirestore) {

    companion object {
        private const val TAG = "RefundRepository"
        private const val REFUNDS_COLLECTION = "refunds"
        private const val ORDERS_COLLECTION = "orders"
        private const val PAYMENTS_COLLECTION = "seller_payments"
    }

    // ==================== CREATE REFUND REQUEST ====================
    suspend fun createRefundRequest(
        orderId: String,
        paymentId: String,
        buyerId: String,
        buyerName: String,
        sellerId: String,
        sellerName: String,
        refundType: String,
        originalAmount: Double,
        refundAmount: Double,
        reason: String,
        reasonDetails: String,
        paymentMethod: String,
        transactionId: String,
        initiatedBy: String
    ): Result<RefundRequest> {
        return try {
            val refundId = UUID.randomUUID().toString()
            val idempotencyKey = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val refundRequest = RefundRequest(
                id = refundId,
                orderId = orderId,
                paymentId = paymentId,
                buyerId = buyerId,
                buyerName = buyerName,
                sellerId = sellerId,
                sellerName = sellerName,
                refundType = refundType,
                originalAmount = originalAmount,
                refundAmount = refundAmount,
                reason = reason,
                reasonDetails = reasonDetails,
                paymentMethod = paymentMethod,
                transactionId = transactionId,
                initiatedBy = initiatedBy,
                status = RefundStatus.REQUESTED.toString(),
                requestedAt = now,
                createdAt = now,
                updatedAt = now,
                idempotencyKey = idempotencyKey,
                auditTrail = listOf(
                    RefundAuditEntry(
                        action = "requested",
                        actor = if (initiatedBy == "buyer") buyerId else sellerId,
                        actorName = if (initiatedBy == "buyer") buyerName else sellerName,
                        notes = "Refund request initiated",
                        timestamp = now
                    )
                )
            )

            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .set(refundRequest.toMap())
                .await()

            Log.d(TAG, "Refund request created: $refundId")
            Result.success(refundRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating refund request", e)
            Result.failure(e)
        }
    }

    // ==================== APPROVE REFUND ====================
    suspend fun approveRefund(
        refundId: String,
        approvedBy: String,
        approverName: String,
        approvalNotes: String = ""
    ): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()

            // FIX: Remove `as Map<String, Any>` cast — mapOf() already returns Map<String, Any>
            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .update(
                    mapOf(
                        "status" to RefundStatus.APPROVED.toString(),
                        "approved_by" to approvedBy,
                        "approved_at" to now,
                        "updated_at" to now,
                        "approval_notes" to approvalNotes
                    )
                )
                .await()

            addAuditEntry(
                refundId,
                "approved",
                approvedBy,
                approverName,
                "Refund approved: $approvalNotes"
            )

            val refund = getRefundById(refundId).getOrNull()
            Log.d(TAG, "Refund approved: $refundId")
            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error approving refund", e)
            Result.failure(e)
        }
    }

    // ==================== REJECT REFUND ====================
    suspend fun rejectRefund(
        refundId: String,
        rejectedBy: String,
        rejectorName: String,
        rejectionReason: String
    ): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()

            // FIX: Remove `as Map<String, Any>` cast
            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .update(
                    mapOf(
                        "status" to RefundStatus.REJECTED.toString(),
                        "approved_by" to rejectedBy,
                        "approval_notes" to rejectionReason,
                        "updated_at" to now
                    )
                )
                .await()

            addAuditEntry(
                refundId,
                "rejected",
                rejectedBy,
                rejectorName,
                "Refund rejected: $rejectionReason"
            )

            val refund = getRefundById(refundId).getOrNull()
            Log.d(TAG, "Refund rejected: $refundId")
            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting refund", e)
            Result.failure(e)
        }
    }

    // ==================== PROCESS REFUND ====================
    suspend fun processRefund(
        refundId: String,
        gatewayRefundId: String = ""
    ): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()

            // FIX: Remove `as Map<String, Any>` cast
            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .update(
                    mapOf(
                        "status" to RefundStatus.PROCESSING.toString(),
                        "gateway_refund_id" to gatewayRefundId,
                        "processed_at" to now,
                        "updated_at" to now
                    )
                )
                .await()

            addAuditEntry(
                refundId,
                "processing",
                "system",
                "System",
                "Refund processing initiated with gateway ID: $gatewayRefundId"
            )

            val refund = getRefundById(refundId).getOrNull()
            Log.d(TAG, "Refund processing started: $refundId")
            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error processing refund", e)
            Result.failure(e)
        }
    }

    // ==================== COMPLETE REFUND ====================
    suspend fun completeRefund(
        refundId: String,
        gatewayRefundId: String = ""
    ): Result<RefundRequest> {
        return try {
            val now = System.currentTimeMillis()

            // FIX: Remove `as Map<String, Any>` cast
            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .update(
                    mapOf(
                        "status" to RefundStatus.COMPLETED.toString(),
                        "gateway_refund_id" to gatewayRefundId,
                        "completed_at" to now,
                        "updated_at" to now
                    )
                )
                .await()

            addAuditEntry(
                refundId,
                "completed",
                "system",
                "System",
                "Refund completed successfully"
            )

            val refund = getRefundById(refundId).getOrNull()
            if (refund != null) {
                updateOrderRefundStatus(refund.orderId, true)
            }

            Log.d(TAG, "Refund completed: $refundId")
            Result.success(refund ?: RefundRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error completing refund", e)
            Result.failure(e)
        }
    }

    // ==================== HANDLE REFUND FAILURE ====================
    suspend fun markRefundFailed(
        refundId: String,
        errorMessage: String
    ): Result<RefundRequest> {
        return try {
            val refund = getRefundById(refundId).getOrNull()
                ?: return Result.failure(Exception("Refund not found"))

            val now = System.currentTimeMillis()
            val newRetryCount = refund.retryCount + 1

            // FIX: Remove `as Map<String, Any>` cast
            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .update(
                    mapOf(
                        "status" to RefundStatus.FAILED.toString(),
                        "error_message" to errorMessage,
                        "retry_count" to newRetryCount,
                        "last_retry_at" to now,
                        "updated_at" to now
                    )
                )
                .await()

            addAuditEntry(
                refundId,
                "failed",
                "system",
                "System",
                "Refund failed (attempt $newRetryCount): $errorMessage"
            )

            Log.d(TAG, "Refund marked as failed: $refundId (attempt $newRetryCount)")
            Result.success(refund)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking refund as failed", e)
            Result.failure(e)
        }
    }

    // ==================== RETRY REFUND ====================
    suspend fun retryRefund(refundId: String): Result<RefundRequest> {
        return try {
            val refund = getRefundById(refundId).getOrNull()
                ?: return Result.failure(Exception("Refund not found"))

            if (!refund.canRetry()) {
                return Result.failure(Exception("Refund cannot be retried (max attempts reached)"))
            }

            val now = System.currentTimeMillis()

            // FIX: Remove `as Map<String, Any>` cast
            firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .update(
                    mapOf(
                        "status" to RefundStatus.PROCESSING.toString(),
                        "last_retry_at" to now,
                        "updated_at" to now
                    )
                )
                .await()

            addAuditEntry(
                refundId,
                "retried",
                "system",
                "System",
                "Refund retry initiated (attempt ${refund.retryCount + 1})"
            )

            Log.d(TAG, "Refund retry initiated: $refundId")
            Result.success(refund)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrying refund", e)
            Result.failure(e)
        }
    }

    // ==================== GET REFUND ====================
    suspend fun getRefundById(refundId: String): Result<RefundRequest> {
        return try {
            val snapshot = firestore.collection(REFUNDS_COLLECTION)
                .document(refundId)
                .get()
                .await()

            val refund = snapshot.toObject(RefundRequest::class.java) ?: RefundRequest()
            Result.success(refund)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refund", e)
            Result.failure(e)
        }
    }

    // ==================== GET REFUNDS BY ORDER ====================
    suspend fun getRefundsByOrderId(orderId: String): Result<List<RefundRequest>> {
        return try {
            val snapshots = firestore.collection(REFUNDS_COLLECTION)
                .whereEqualTo("order_id", orderId)
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val refunds = snapshots.documents.mapNotNull { it.toObject(RefundRequest::class.java) }
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refunds by order", e)
            Result.failure(e)
        }
    }

    // ==================== GET REFUNDS BY BUYER ====================
    suspend fun getRefundsByBuyerId(buyerId: String): Result<List<RefundRequest>> {
        return try {
            val snapshots = firestore.collection(REFUNDS_COLLECTION)
                .whereEqualTo("buyer_id", buyerId)
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val refunds = snapshots.documents.mapNotNull { it.toObject(RefundRequest::class.java) }
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refunds by buyer", e)
            Result.failure(e)
        }
    }

    // ==================== GET REFUNDS BY SELLER ====================
    suspend fun getRefundsBySellerId(sellerId: String): Result<List<RefundRequest>> {
        return try {
            val snapshots = firestore.collection(REFUNDS_COLLECTION)
                .whereEqualTo("seller_id", sellerId)
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val refunds = snapshots.documents.mapNotNull { it.toObject(RefundRequest::class.java) }
            Result.success(refunds)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting refunds by seller", e)
            Result.failure(e)
        }
    }

    // ==================== GET PENDING REFUNDS ====================
    suspend fun getPendingRefunds(): Result<List<RefundRequest>> = try {
        val snapshots = firestore.collection(REFUNDS_COLLECTION)
            .whereIn("status", listOf(RefundStatus.REQUESTED.toString(), RefundStatus.APPROVED.toString()))
            .orderBy("requested_at", Query.Direction.ASCENDING)
            .get()
            .await()

        val refunds = snapshots.documents.mapNotNull { it.toObject(RefundRequest::class.java) }
        Result.success(refunds)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting pending refunds", e)
        Result.failure(e)
    }

    // ==================== GET FAILED REFUNDS FOR RETRY ====================
    suspend fun getFailedRefundsForRetry(): Result<List<RefundRequest>> = try {
        val snapshots = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("status", RefundStatus.FAILED.toString())
            .whereLessThan("retry_count", 3)
            .orderBy("retry_count", Query.Direction.ASCENDING)
            .orderBy("last_retry_at", Query.Direction.ASCENDING)
            .get()
            .await()

        val refunds = snapshots.documents.mapNotNull { it.toObject(RefundRequest::class.java) }
        Result.success(refunds)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting failed refunds", e)
        Result.failure(e)
    }

    // ==================== UPDATE REFUND SPLITS ====================
    suspend fun updateRefundSplits(
        refundId: String,
        splits: List<RefundSplit>
    ): Result<Unit> = try {
        // FIX: `update()` requires Map<String, Any>, not a Pair via `to` infix
        firestore.collection(REFUNDS_COLLECTION)
            .document(refundId)
            .update(mapOf("refund_splits" to splits.map { it.toMap() }))
            .await()

        Log.d(TAG, "Refund splits updated: $refundId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating refund splits", e)
        Result.failure(e)
    }

    // ==================== ADD AUDIT ENTRY ====================
    private suspend fun addAuditEntry(
        refundId: String,
        action: String,
        actor: String,
        actorName: String,
        notes: String
    ) = try {
        val entry = RefundAuditEntry(
            action = action,
            actor = actor,
            actorName = actorName,
            notes = notes,
            timestamp = System.currentTimeMillis()
        )

        // FIX: `update()` requires Map<String, Any>, not a Pair via `to` infix
        firestore.collection(REFUNDS_COLLECTION)
            .document(refundId)
            .update(
                mapOf(
                    "audit_trail" to com.google.firebase.firestore.FieldValue.arrayUnion(entry.toMap())
                )
            )
            .await()
    } catch (e: Exception) {
        Log.e(TAG, "Error adding audit entry", e)
    }

    // ==================== UPDATE ORDER REFUND STATUS ====================
    private suspend fun updateOrderRefundStatus(orderId: String, isRefunded: Boolean) = try {
        val status = if (isRefunded) OrderStatus.CANCELLED.toString() else OrderStatus.COMPLETED.toString()
        // FIX: vararg Pair overload causes type inference issues — use mapOf() instead
        firestore.collection(ORDERS_COLLECTION)
            .document(orderId)
            .update(
                mapOf(
                    "status" to status,
                    "updated_at" to System.currentTimeMillis()
                )
            )
            .await()
    } catch (e: Exception) {
        Log.e(TAG, "Error updating order refund status", e)
    }

    // ==================== UPDATE PAYMENT REFUND INFO ====================
    suspend fun updatePaymentRefundInfo(
        paymentId: String,
        refundAmount: Double,
        refundReason: String,
        refundDate: Long
    ): Result<Unit> = try {
        // FIX: vararg Pair overload causes type inference issues — use mapOf() instead
        firestore.collection(PAYMENTS_COLLECTION)
            .document(paymentId)
            .update(
                mapOf(
                    "refund_amount" to refundAmount,
                    "refund_reason" to refundReason,
                    "refund_date" to refundDate,
                    "status" to PaymentStatus.REFUNDED.toString(),
                    "updated_at" to System.currentTimeMillis()
                )
            )
            .await()

        Log.d(TAG, "Payment refund info updated: $paymentId")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error updating payment refund info", e)
        Result.failure(e)
    }

    // ==================== CHECK DUPLICATE REFUND ====================
    suspend fun checkDuplicateRefund(idempotencyKey: String): Result<RefundRequest?> = try {
        val snapshot = firestore.collection(REFUNDS_COLLECTION)
            .whereEqualTo("idempotency_key", idempotencyKey)
            .get()
            .await()

        val refund = snapshot.documents.firstOrNull()?.toObject(RefundRequest::class.java)
        Result.success(refund)
    } catch (e: Exception) {
        Log.e(TAG, "Error checking duplicate refund", e)
        Result.failure(e)
    }
}