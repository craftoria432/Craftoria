package com.gcuf.craftoria.services

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.Notification
import com.gcuf.craftoria.data.model.NotificationActionType
import com.gcuf.craftoria.data.model.NotificationCategory
import com.gcuf.craftoria.data.model.RefundRequest
import com.gcuf.craftoria.data.model.RefundStatus
import com.gcuf.craftoria.data.repository.NotificationRepository
import kotlinx.coroutines.tasks.await

class RefundNotificationService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {
    companion object {
        private const val TAG = "RefundNotificationService"
    }

    // ==================== NOTIFY REFUND REQUESTED ====================
    suspend fun notifyRefundRequested(refund: RefundRequest): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying refund requested: ${refund.id}")

            // Notify seller
            val sellerNotification = Notification(
                userId = refund.sellerId,
                title = "Refund Request Received",
                description = "A refund request has been submitted for order ${refund.orderId}. Amount: PKR ${refund.refundAmount}",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId,
                    "payment_id" to refund.paymentId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = refund.reason,
                buyerName = refund.buyerName
            )

            notificationRepository.createNotification(sellerNotification).getOrNull()

            // Notify buyer
            val buyerNotification = Notification(
                userId = refund.buyerId,
                title = "Refund Request Submitted",
                description = "Your refund request for PKR ${refund.refundAmount} has been submitted and is pending approval.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = refund.reason
            )

            notificationRepository.createNotification(buyerNotification).getOrNull()

            Log.d(TAG, "✅ Refund requested notifications sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify refund requested", e)
            Result.failure(e)
        }
    }

    // ==================== NOTIFY REFUND APPROVED ====================
    suspend fun notifyRefundApproved(refund: RefundRequest): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying refund approved: ${refund.id}")

            // Notify buyer
            val buyerNotification = Notification(
                userId = refund.buyerId,
                title = "Refund Approved",
                description = "Your refund of PKR ${refund.refundAmount} has been approved and is being processed.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = refund.reason
            )

            notificationRepository.createNotification(buyerNotification).getOrNull()

            // Notify seller
            val sellerNotification = Notification(
                userId = refund.sellerId,
                title = "Refund Approved",
                description = "The refund request for order ${refund.orderId} (PKR ${refund.refundAmount}) has been approved.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                buyerName = refund.buyerName
            )

            notificationRepository.createNotification(sellerNotification).getOrNull()

            Log.d(TAG, "✅ Refund approved notifications sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify refund approved", e)
            Result.failure(e)
        }
    }

    // ==================== NOTIFY REFUND REJECTED ====================
    suspend fun notifyRefundRejected(refund: RefundRequest, rejectionReason: String): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying refund rejected: ${refund.id}")

            // Notify buyer
            val buyerNotification = Notification(
                userId = refund.buyerId,
                title = "Refund Request Rejected",
                description = "Your refund request for PKR ${refund.refundAmount} has been rejected. Reason: $rejectionReason",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = rejectionReason
            )

            notificationRepository.createNotification(buyerNotification).getOrNull()

            // Notify seller
            val sellerNotification = Notification(
                userId = refund.sellerId,
                title = "Refund Request Rejected",
                description = "The refund request for order ${refund.orderId} has been rejected.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                buyerName = refund.buyerName
            )

            notificationRepository.createNotification(sellerNotification).getOrNull()

            Log.d(TAG, "✅ Refund rejected notifications sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify refund rejected", e)
            Result.failure(e)
        }
    }

    // ==================== NOTIFY REFUND PROCESSING ====================
    suspend fun notifyRefundProcessing(refund: RefundRequest): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying refund processing: ${refund.id}")

            // Notify buyer
            val buyerNotification = Notification(
                userId = refund.buyerId,
                title = "Refund Processing",
                description = "Your refund of PKR ${refund.refundAmount} is now being processed. You'll receive it within 3-5 business days.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = refund.reason
            )

            notificationRepository.createNotification(buyerNotification).getOrNull()

            Log.d(TAG, "✅ Refund processing notifications sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify refund processing", e)
            Result.failure(e)
        }
    }

    // ==================== NOTIFY REFUND COMPLETED ====================
    suspend fun notifyRefundCompleted(refund: RefundRequest): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying refund completed: ${refund.id}")

            // Notify buyer
            val buyerNotification = Notification(
                userId = refund.buyerId,
                title = "Refund Completed",
                description = "Your refund of PKR ${refund.refundAmount} has been successfully processed and credited to your account.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = refund.reason
            )

            notificationRepository.createNotification(buyerNotification).getOrNull()

            // Notify seller
            val sellerNotification = Notification(
                userId = refund.sellerId,
                title = "Refund Completed",
                description = "The refund for order ${refund.orderId} (PKR ${refund.refundAmount}) has been completed.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                buyerName = refund.buyerName
            )

            notificationRepository.createNotification(sellerNotification).getOrNull()

            Log.d(TAG, "✅ Refund completed notifications sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify refund completed", e)
            Result.failure(e)
        }
    }

    // ==================== NOTIFY REFUND FAILED ====================
    suspend fun notifyRefundFailed(refund: RefundRequest, errorMessage: String): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying refund failed: ${refund.id}")

            // Notify buyer
            val buyerNotification = Notification(
                userId = refund.buyerId,
                title = "Refund Failed - Retry Pending",
                description = "Your refund of PKR ${refund.refundAmount} encountered an issue. We're retrying automatically. Attempt ${refund.retryCount}/3",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = errorMessage
            )

            notificationRepository.createNotification(buyerNotification).getOrNull()

            Log.d(TAG, "✅ Refund failed notifications sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify refund failed", e)
            Result.failure(e)
        }
    }

    // ==================== NOTIFY AUTO-APPROVED REFUND ====================
    suspend fun notifyAutoApprovedRefund(refund: RefundRequest): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying auto-approved refund: ${refund.id}")

            // Notify buyer
            val buyerNotification = Notification(
                userId = refund.buyerId,
                title = "Refund Auto-Approved",
                description = "Your refund of PKR ${refund.refundAmount} has been automatically approved (within 24-hour grace period) and is being processed.",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = refund.reason
            )

            notificationRepository.createNotification(buyerNotification).getOrNull()

            Log.d(TAG, "✅ Auto-approved refund notifications sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify auto-approved refund", e)
            Result.failure(e)
        }
    }

    // ==================== NOTIFY ADMIN - PENDING REFUND ====================
    suspend fun notifyAdminPendingRefund(refund: RefundRequest, adminId: String): Result<Unit> {
        return try {
            Log.d(TAG, "📢 Notifying admin of pending refund: ${refund.id}")

            val adminNotification = Notification(
                userId = adminId,
                title = "Pending Refund Approval",
                description = "Refund request from ${refund.buyerName} for order ${refund.orderId}. Amount: PKR ${refund.refundAmount}. Reason: ${refund.reason}",
                category = NotificationCategory.REFUNDS.name,
                actionType = NotificationActionType.VIEW_PAYMENT.name,
                actionData = mapOf(
                    "refund_id" to refund.id,
                    "order_id" to refund.orderId,
                    "action" to "approve_refund"
                ),
                orderId = refund.orderId,
                refundId = refund.id,
                refundAmount = refund.refundAmount,
                refundStatus = refund.status,
                refundReason = refund.reason,
                buyerName = refund.buyerName
            )

            notificationRepository.createNotification(adminNotification).getOrNull()

            Log.d(TAG, "✅ Admin pending refund notification sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify admin of pending refund", e)
            Result.failure(e)
        }
    }
}
