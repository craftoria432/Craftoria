package com.gcuf.craftoria.payment

import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderItem
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.data.repository.CoSellerStorePaymentRepository
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.utils.PaymentDataMigration
import com.gcuf.craftoria.utils.PaymentSplitProcessor

/**
 * ✅ FACADE PATTERN: Unified interface for payment system operations
 * 
 * This manager provides a single entry point for all payment operations
 * while keeping individual components separate and maintainable.
 */
class PaymentSystemManager(
    private val paymentRepository: PaymentRepository = PaymentRepository(),
    private val coSellerRepository: CoSellerStorePaymentRepository = CoSellerStorePaymentRepository(),
    private val paymentProcessor: PaymentSplitProcessor = PaymentSplitProcessor(com.google.firebase.firestore.FirebaseFirestore.getInstance()),
    private val dataMigration: PaymentDataMigration = PaymentDataMigration
) {

    /**
     * Process order payments with automatic split handling
     */
    suspend fun processOrderPayments(order: Order, items: List<OrderItem>): Result<List<String>> {
        return paymentProcessor.processOrderPaymentsWithSplits(order, items)
    }

    /**
     * Get seller payments (original seller only)
     */
    suspend fun getSellerPayments(sellerId: String, requestingUserId: String): Result<List<SellerPayment>> {
        return paymentRepository.getSellerPayments(sellerId, requestingUserId)
    }

    /**
     * Get store payments (co-seller store members only)
     */
    suspend fun getStorePayments(
        storeId: String, 
        currentUserId: String
    ): Result<List<SellerPayment>> {
        return coSellerRepository.loadStorePayments(storeId, currentUserId)
    }

    /**
     * Update payment split status
     */
    suspend fun updatePaymentSplitStatus(
        paymentId: String,
        sellerId: String,
        newStatus: String
    ): Result<Unit> {
        return coSellerRepository.updatePaymentSplitStatus(paymentId, sellerId, newStatus)
    }

    /**
     * Migrate existing payment data (run once)
     */
    suspend fun migratePaymentData(): Result<Int> {
        return dataMigration.migrateExistingPayments()
    }

    /**
     * Get payment details with splits
     */
    suspend fun getPaymentDetails(paymentId: String, requestingUserId: String): Result<SellerPayment> {
        return coSellerRepository.getPaymentWithSplits(paymentId)
    }
}