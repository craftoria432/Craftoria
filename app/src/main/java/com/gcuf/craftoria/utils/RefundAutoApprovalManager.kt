package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.RefundRequest
import com.gcuf.craftoria.data.model.RefundStatus
import com.gcuf.craftoria.data.model.getRequestedAtLong
import com.gcuf.craftoria.data.repository.RefundRepository
import com.gcuf.craftoria.services.RefundNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefundAutoApprovalManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val refundRepository = RefundRepository(firestore)
    private val refundProcessor = RefundProcessor(firestore)
    private val notificationService = RefundNotificationService(firestore)

    companion object {
        private const val TAG = "RefundAutoApproval"
        private const val AUTO_APPROVAL_HOURS = 24
        private const val CHECK_INTERVAL_HOURS = 1
    }

    suspend fun checkAndProcessPendingRefunds(): Result<Int> {
        return try {
            Log.d(TAG, "⏰ Checking pending refunds for auto-approval...")

            val pendingRefundsResult = refundRepository.getPendingRefunds()
            if (pendingRefundsResult.isFailure) {
                return Result.failure(pendingRefundsResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val pendingRefunds = pendingRefundsResult.getOrNull() ?: emptyList()
            Log.d(TAG, "📋 Found ${pendingRefunds.size} pending refunds")

            var autoApprovedCount = 0
            pendingRefunds.forEach { refund ->
                if (shouldAutoApprove(refund)) {
                    val approvalResult = autoApproveRefund(refund)
                    if (approvalResult.isSuccess) autoApprovedCount++
                }
            }

            Log.d(TAG, "✅ Auto-approval complete: $autoApprovedCount refunds processed")
            Result.success(autoApprovedCount)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in auto-approval check", e)
            Result.failure(e)
        }
    }

    private fun shouldAutoApprove(refund: RefundRequest): Boolean {
        if (refund.status != RefundStatus.REQUESTED.toString()) return false
        val hoursSinceRequest = (System.currentTimeMillis() - refund.getRequestedAtLong()) / (1000 * 60 * 60)
        return hoursSinceRequest >= AUTO_APPROVAL_HOURS
    }

    private suspend fun autoApproveRefund(refund: RefundRequest): Result<Unit> {
        return try {
            val approvalResult = refundRepository.approveRefund(
                refundId = refund.id,
                approvedBy = "system",
                approverName = "Auto-Approval System",
                approvalNotes = "Automatically approved after $AUTO_APPROVAL_HOURS hours of no response"
            )
            if (approvalResult.isFailure) {
                return Result.failure(approvalResult.exceptionOrNull() ?: Exception("Approval failed"))
            }

            refundProcessor.processRefund(refundId = refund.id, actorId = "system")

            // ✅ FIX: notifyRefundApproved takes RefundRequest, not separate params
            notificationService.notifyRefundApproved(refund)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in auto-approval", e)
            Result.failure(e)
        }
    }

    fun startPeriodicChecks(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (true) {
                checkAndProcessPendingRefunds()
                kotlinx.coroutines.delay(CHECK_INTERVAL_HOURS * 60 * 60 * 1000L)
            }
        }
    }
}