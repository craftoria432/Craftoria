package com.gcuf.craftoria.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.PaymentSplit
import kotlinx.coroutines.tasks.await
import java.util.*

class CoSellerStorePaymentRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /**
     * Load all payments for a co-seller store with access control
     * ✅ SECURITY: Verify user is store owner or member
     */
    suspend fun loadStorePayments(
        storeId: String,
        currentUserId: String,
        storeMemberIds: List<String> = emptyList(),
        storeOwnerId: String = ""
    ): Result<List<SellerPayment>> {
        return try {
            // ✅ Access control: User must be owner or member (if provided)
            if (storeOwnerId.isNotEmpty() && storeMemberIds.isNotEmpty()) {
                if (currentUserId != storeOwnerId && currentUserId !in storeMemberIds) {
                    return Result.failure(SecurityException("Access denied: Not a store member"))
                }
            }

            val payments = db.collection("seller_payments")
                .whereEqualTo("co_seller_store_id", storeId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(SellerPayment::class.java)

            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load payment details with split breakdown
     */
    suspend fun getPaymentWithSplits(paymentId: String): Result<SellerPayment> {
        return try {
            val payment = db.collection("seller_payments")
                .document(paymentId)
                .get()
                .await()
                .toObject(SellerPayment::class.java)
                ?: throw Exception("Payment not found")

            Result.success(payment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get member earnings breakdown for a specific period
     */
    suspend fun getMemberEarningsBreakdown(
        storeId: String,
        memberId: String,
        startDate: Long,
        endDate: Long
    ): Result<MemberEarningsBreakdown> {
        return try {
            val payments = db.collection("seller_payments")
                .whereEqualTo("co_seller_store_id", storeId)
                .whereGreaterThanOrEqualTo("created_at", startDate)
                .whereLessThanOrEqualTo("created_at", endDate)
                .get()
                .await()
                .toObjects(SellerPayment::class.java)

            var totalEarnings = 0.0
            var completedEarnings = 0.0
            var pendingEarnings = 0.0
            val orderCount = payments.size

            payments.forEach { payment ->
                val split = payment.paymentSplits.find { it.sellerId == memberId }
                if (split != null) {
                    totalEarnings += split.splitAmount
                    when (split.status.lowercase()) {
                        "completed" -> completedEarnings += split.splitAmount
                        "pending" -> pendingEarnings += split.splitAmount
                    }
                }
            }

            val breakdown = MemberEarningsBreakdown(
                memberId = memberId,
                totalEarnings = totalEarnings,
                completedEarnings = completedEarnings,
                pendingEarnings = pendingEarnings,
                orderCount = orderCount,
                period = "custom"
            )

            Result.success(breakdown)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get store revenue summary
     */
    suspend fun getStoreRevenueSummary(
        storeId: String,
        startDate: Long,
        endDate: Long
    ): Result<StoreRevenueSummary> {
        return try {
            val payments = db.collection("seller_payments")
                .whereEqualTo("co_seller_store_id", storeId)
                .whereGreaterThanOrEqualTo("created_at", startDate)
                .whereLessThanOrEqualTo("created_at", endDate)
                .get()
                .await()
                .toObjects(SellerPayment::class.java)

            var totalRevenue = 0.0
            var completedRevenue = 0.0
            var pendingRevenue = 0.0
            val orderCount = payments.size

            payments.forEach { payment ->
                totalRevenue += payment.amount
                when (payment.status.lowercase()) {
                    "completed" -> completedRevenue += payment.amount
                    "pending" -> pendingRevenue += payment.amount
                }
            }

            val summary = StoreRevenueSummary(
                storeId = storeId,
                totalRevenue = totalRevenue,
                completedRevenue = completedRevenue,
                pendingRevenue = pendingRevenue,
                orderCount = orderCount,
                period = "custom"
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update payment split status (e.g., when payment is completed)
     */
    suspend fun updatePaymentSplitStatus(
        paymentId: String,
        sellerId: String,
        newStatus: String
    ): Result<Unit> {
        return try {
            val payment = db.collection("seller_payments")
                .document(paymentId)
                .get()
                .await()
                .toObject(SellerPayment::class.java)
                ?: throw Exception("Payment not found")

            // Update the specific split
            val updatedSplits = payment.paymentSplits.map { split ->
                if (split.sellerId == sellerId) {
                    split.copy(status = newStatus)
                } else {
                    split
                }
            }

            db.collection("seller_payments")
                .document(paymentId)
                .update(
                    "paymentSplits", updatedSplits, // Use the actual field name in your model
                    "updatedAt", System.currentTimeMillis() // Match your model's property name
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all payments for a member across all stores
     */
    suspend fun getMemberPayments(
        memberId: String
    ): Result<List<MemberPaymentRecord>> {
        return try {
            val payments = db.collection("seller_payments")
                .get()
                .await()
                .toObjects(SellerPayment::class.java)

            val memberPayments = mutableListOf<MemberPaymentRecord>()

            payments.forEach { payment ->
                val split = payment.paymentSplits.find { it.sellerId == memberId }
                if (split != null) {
                    memberPayments.add(
                        MemberPaymentRecord(
                            paymentId = payment.id,
                            orderId = payment.orderId,
                            storeId = payment.coSellerStoreId,
                            storeName = payment.storeName,
                            amount = split.splitAmount,
                            percentage = split.splitPercentage,
                            status = split.status,
                            createdAt = payment.createdAt
                        )
                    )
                }
            }

            Result.success(memberPayments.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ✅ Data classes for reporting
data class MemberEarningsBreakdown(
    val memberId: String = "",
    val totalEarnings: Double = 0.0,
    val completedEarnings: Double = 0.0,
    val pendingEarnings: Double = 0.0,
    val orderCount: Int = 0,
    val period: String = ""
)

data class StoreRevenueSummary(
    val storeId: String = "",
    val totalRevenue: Double = 0.0,
    val completedRevenue: Double = 0.0,
    val pendingRevenue: Double = 0.0,
    val orderCount: Int = 0,
    val period: String = ""
)

data class MemberPaymentRecord(
    val paymentId: String = "",
    val orderId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val amount: Double = 0.0,
    val percentage: Double = 0.0,
    val status: String = "",
    val createdAt: Long = 0L
)
