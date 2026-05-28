package com.gcuf.craftoria.utils

import com.google.firebase.firestore.DocumentSnapshot

// ── Refund State Enum ─────────────────────────────────────────────────────────
// Represents all possible refund states for a delivered/completed order
enum class OrderRefundState {
    NONE,           // No refund exists
    REQUESTED,      // Buyer submitted, awaiting seller/admin action
    APPROVED,       // Approved, processing will begin
    PROCESSING,     // In progress
    COMPLETED,      // Refund done
    REJECTED,       // Seller/admin rejected (can resubmit)
    FINAL_DECISION, // Rejected twice - no more requests allowed
    FAILED          // Processing failed
}

/**
 * Rank a refund document by its terminal state priority.
 * When multiple refund docs exist for the same order (e.g. a completed refund + a later-rejected resubmission),
 * this ensures we pick the one with the most significant status, not just the latest timestamp.
 */
fun docPriority(doc: DocumentSnapshot): Int {
    val isFinal  = doc.getBoolean("final_decision") ?: false
    val statusUp = doc.getString("status")?.uppercase() ?: "REQUESTED"
    return when {
        statusUp == "COMPLETED"                                                    -> 100
        isFinal                                                                    -> 90
        statusUp in listOf("APPROVED", "APPROVED_BY_SELLER", "APPROVED_BY_ADMIN") -> 80
        statusUp == "PROCESSING"                                                   -> 70
        statusUp in listOf("REQUESTED", "UNDER_REVIEW")                           -> 60
        statusUp in listOf("REJECTED", "REJECTED_BY_SELLER", "REJECTED_BY_ADMIN") -> 50
        statusUp == "FAILED"                                                       -> 40
        else                                                                       -> 10
    }
}

/**
 * Convert a Firestore refund document to an OrderRefundState enum.
 * Reads the status, final_decision, and can_resubmit fields to determine the UI state.
 */
fun docToRefundState(doc: DocumentSnapshot): OrderRefundState {
    val isFinal   = doc.getBoolean("final_decision") ?: false
    val canResub  = doc.getBoolean("can_resubmit")   ?: true
    val statusStr = doc.getString("status")?.uppercase() ?: "REQUESTED"
    return when {
        statusStr == "COMPLETED"                                                    -> OrderRefundState.COMPLETED
        isFinal                                                                     -> OrderRefundState.FINAL_DECISION
        statusStr in listOf("APPROVED", "APPROVED_BY_SELLER", "APPROVED_BY_ADMIN") -> OrderRefundState.APPROVED
        statusStr == "PROCESSING"                                                   -> OrderRefundState.PROCESSING
        statusStr in listOf("REQUESTED", "UNDER_REVIEW")                           -> OrderRefundState.REQUESTED
        statusStr in listOf("REJECTED", "REJECTED_BY_SELLER", "REJECTED_BY_ADMIN") ->
            if (canResub) OrderRefundState.REJECTED else OrderRefundState.FINAL_DECISION
        statusStr == "FAILED"                                                       -> OrderRefundState.FAILED
        else                                                                        -> OrderRefundState.REQUESTED
    }
}
