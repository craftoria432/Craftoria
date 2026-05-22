package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.gcuf.craftoria.data.model.PaymentSplit
import kotlinx.coroutines.tasks.await
import com.gcuf.craftoria.data.model.toMap
import java.util.*

class CoSellerStorePaymentRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    companion object {
        private const val TAG = "CoSellerStorePaymentRepo"
    }

    private val paymentsCollection = db.collection("payments")
    private val legacyPaymentsCollection = db.collection("seller_payments")

    private fun mergePayments(vararg paymentLists: List<SellerPayment>): List<SellerPayment> {
        return paymentLists
            .asList()
            .flatten()
            .associateBy { it.id }
            .values
            .sortedByDescending { it.getCreatedAtLong() }
    }

    private suspend fun loadStorePaymentsFromCollection(
        collectionName: String,
        storeId: String
    ): List<SellerPayment> {
        val snapshot = db.collection(collectionName)
            .whereEqualTo("co_seller_store_id", storeId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { PaymentRepository.parsePayment(it) }
    }

    private suspend fun loadStorePaymentsAcrossCollections(storeId: String): List<SellerPayment> {
        val currentPayments = loadStorePaymentsFromCollection("payments", storeId)
        val legacyPayments = loadStorePaymentsFromCollection("seller_payments", storeId)

        if (legacyPayments.isNotEmpty()) {
            Log.d(
                TAG,
                "Loaded ${legacyPayments.size} legacy co-seller payments from seller_payments for store $storeId"
            )
        }

        return mergePayments(currentPayments, legacyPayments)
    }

    private suspend fun getPaymentFromCollection(
        collectionName: String,
        paymentId: String
    ): SellerPayment? {
        val doc = db.collection(collectionName)
            .document(paymentId)
            .get()
            .await()

        return if (doc.exists()) PaymentRepository.parsePayment(doc) else null
    }

    private suspend fun getPaymentWithSource(paymentId: String): Pair<String, SellerPayment>? {
        val currentPayment = getPaymentFromCollection("payments", paymentId)
        if (currentPayment != null) return "payments" to currentPayment

        val legacyPayment = getPaymentFromCollection("seller_payments", paymentId)
        if (legacyPayment != null) return "seller_payments" to legacyPayment

        return null
    }

    /**
     * Load all payments for a co-seller store with access control
     * ✅ SECURITY: Verify user is store owner or member
     * ✅ FIX 4: MANDATORY security check - no bypass allowed
     * ✅ FIX: Uses parsePayment() instead of toObjects() to handle mixed timestamp types
     */
    suspend fun loadStorePayments(
        storeId: String,
        currentUserId: String,
        storeMemberIds: List<String> = emptyList(),
        storeOwnerId: String = ""
    ): Result<List<SellerPayment>> {
        return try {
            // ✅ FIX 4: MANDATORY access control - no bypass even with empty parameters
            val isOwner = currentUserId == storeOwnerId
            val isMember = currentUserId in storeMemberIds
            
            // Store owner/member IDs should ALWAYS be provided (not empty)
            // If they are empty AND we can't verify, deny access
            if (storeOwnerId.isEmpty() || storeMemberIds.isEmpty()) {
                // If params are empty, we cannot verify access - DENY
                Log.w(TAG, "Store access denied: Missing owner or member IDs for store $storeId")
                return Result.failure(
                    SecurityException("Access denied: Store credentials not provided")
                )
            }
            
            if (!isOwner && !isMember) {
                Log.w(TAG, "Unauthorized access attempt: User $currentUserId tried to access store $storeId")
                return Result.failure(
                    SecurityException("Access denied: You are not authorized to view this store's payments")
                )
            }

            val payments = loadStorePaymentsAcrossCollections(storeId)
            Log.d(TAG, "✅ Store payments loaded for $currentUserId: ${payments.size} payments")
            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for co-seller store payments.
     * Automatically updates whenever payment data changes.
     */
    fun listenToStorePayments(
        storeId: String,
        currentUserId: String,
        storeMemberIds: List<String> = emptyList(),
        storeOwnerId: String = "",
        onUpdate: (List<SellerPayment>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        Log.d(TAG, "Setting up real-time listener for store: $storeId")
        
        // ✅ Access control check
        if (storeOwnerId.isNotEmpty() && storeMemberIds.isNotEmpty()) {
            if (currentUserId != storeOwnerId && currentUserId !in storeMemberIds) {
                val error = SecurityException("Access denied: Not a store member")
                onError(error)
                return object : ListenerRegistration {
                    override fun remove() {}
                }
            }
        }
        
        val currentPaymentsById = mutableMapOf<String, SellerPayment>()
        val legacyPaymentsById = mutableMapOf<String, SellerPayment>()

        fun dispatchMergedPayments() {
            onUpdate(mergePayments(currentPaymentsById.values.toList(), legacyPaymentsById.values.toList()))
        }

        fun registerCollectionListener(
            collectionName: String,
            targetMap: MutableMap<String, SellerPayment>
        ): ListenerRegistration {
            return db.collection(collectionName)
                .whereEqualTo("co_seller_store_id", storeId)
                .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Real-time listener error in $collectionName", error)
                    onError(error)
                    return@addSnapshotListener
                }
                
                try {
                    targetMap.clear()
                    snapshot?.documents
                        ?.mapNotNull { PaymentRepository.parsePayment(it) }
                        ?.forEach { payment -> targetMap[payment.id] = payment }

                    Log.d(
                        TAG,
                        "Real-time update from $collectionName: ${targetMap.size} payments for store $storeId"
                    )
                    dispatchMergedPayments()
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing real-time update from $collectionName", e)
                    onError(e)
                }
            }
        }

        val currentListener = registerCollectionListener("payments", currentPaymentsById)
        val legacyListener = registerCollectionListener("seller_payments", legacyPaymentsById)

        return object : ListenerRegistration {
            override fun remove() {
                currentListener.remove()
                legacyListener.remove()
            }
        }
    }

    /**
     * Load payment details with split breakdown
     * ✅ FIX: Uses parsePayment() instead of toObject() to handle mixed timestamp types
     */
    suspend fun getPaymentWithSplits(paymentId: String): Result<SellerPayment> {
        return try {
            val payment = getPaymentWithSource(paymentId)?.second
                ?: throw Exception("Payment not found")

            Result.success(payment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get member earnings breakdown for a specific period
     * ✅ FIX: Uses parsePayment() instead of toObjects() to handle mixed timestamp types
     */
    suspend fun getMemberEarningsBreakdown(
        storeId: String,
        memberId: String,
        startDate: Long,
        endDate: Long
    ): Result<MemberEarningsBreakdown> {
        return try {
            val payments = loadStorePaymentsAcrossCollections(storeId).filter {
                val createdAt = it.getCreatedAtLong()
                createdAt in startDate..endDate
            }

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
     * ✅ FIX: Uses parsePayment() instead of toObjects() to handle mixed timestamp types
     */
    suspend fun getStoreRevenueSummary(
        storeId: String,
        startDate: Long,
        endDate: Long
    ): Result<StoreRevenueSummary> {
        return try {
            val payments = loadStorePaymentsAcrossCollections(storeId).filter {
                val createdAt = it.getCreatedAtLong()
                createdAt in startDate..endDate
            }

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
     * ✅ FIX: Uses parsePayment() instead of toObject() to handle mixed timestamp types
     * ✅ FIX: Uses correct Firestore field names (payment_splits, updated_at)
     */
    suspend fun updatePaymentSplitStatus(
        paymentId: String,
        sellerId: String,
        newStatus: String
    ): Result<Unit> {
        return try {
            val (collectionName, payment) = getPaymentWithSource(paymentId)
                ?: throw Exception("Payment not found")

            // Update the specific split
            val updatedSplits = payment.paymentSplits.map { split ->
                if (split.sellerId == sellerId) {
                    split.copy(status = newStatus)
                } else {
                    split
                }
            }

            val splitMaps: List<Map<String, Any>> = updatedSplits.map { it.toMap() }
            db.collection(collectionName)
                .document(paymentId)
                .update(
                    mapOf(
                        "payment_splits" to splitMaps,
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Payment split status updated for $paymentId in $collectionName")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all payments for a member across all stores
     * ✅ FIX: Uses parsePayment() instead of toObjects() to handle mixed timestamp types
     * ✅ FIX: Filters by involved_seller_ids instead of full collection scan
     */
    suspend fun getMemberPayments(
        memberId: String
    ): Result<List<MemberPaymentRecord>> {
        return try {
            val currentSnapshot = paymentsCollection
                .whereArrayContains("involved_seller_ids", memberId)
                .get()
                .await()

            val legacySnapshot = legacyPaymentsCollection
                .whereArrayContains("involved_seller_ids", memberId)
                .get()
                .await()

            val payments = mergePayments(
                currentSnapshot.documents.mapNotNull { PaymentRepository.parsePayment(it) },
                legacySnapshot.documents.mapNotNull { PaymentRepository.parsePayment(it) }
            )

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
                            createdAt = payment.getCreatedAtLong()
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
