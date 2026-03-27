package com.gcuf.craftoria.utils

import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderItem
import com.gcuf.craftoria.data.model.SellerPayment
import android.util.Log

data class PaymentValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

object PaymentValidator {
    private const val TAG = "PaymentValidator"
    private const val MAX_PAYMENT_AMOUNT = 1_000_000.0

    fun validateOrderPayment(order: Order, items: List<OrderItem>): PaymentValidationResult {
        val errors = mutableListOf<String>()

        // Validate order
        if (order.id.isEmpty()) errors.add("Order ID is empty")
        if (order.buyerId.isEmpty()) errors.add("Buyer ID is empty")
        if (order.buyerName.isEmpty()) errors.add("Buyer name is empty")

        // Validate items
        if (items.isEmpty()) errors.add("No items in order")
        items.forEach { item ->
            if (item.productId.isEmpty()) errors.add("Product ID is empty")
            if (item.sellerId.isEmpty()) errors.add("Seller ID is empty")
            if (item.quantity <= 0) errors.add("Invalid quantity: ${item.quantity}")
            if (item.price < 0) errors.add("Invalid price: ${item.price}")
        }

        // Validate amounts
        val totalAmount = items.sumOf { it.price * it.quantity }
        if (totalAmount <= 0) errors.add("Total amount must be positive")
        if (totalAmount > MAX_PAYMENT_AMOUNT) errors.add("Amount exceeds maximum limit: $MAX_PAYMENT_AMOUNT")

        // Validate payment method
        val validMethods = listOf("Cash on Delivery", "Debit/Credit Card", "Bank Transfer")
        if (order.paymentMethod !in validMethods) {
            errors.add("Invalid payment method: ${order.paymentMethod}")
        }

        if (errors.isNotEmpty()) {
            Log.w(TAG, "❌ Payment validation failed: ${errors.joinToString(", ")}")
        }

        return PaymentValidationResult(isValid = errors.isEmpty(), errors = errors)
    }

    fun validateRefund(payment: SellerPayment, refundAmount: Double): PaymentValidationResult {
        val errors = mutableListOf<String>()

        if (refundAmount <= 0) errors.add("Refund amount must be positive")
        if (refundAmount > payment.amount) errors.add("Refund exceeds payment amount")
        if (payment.status != "COMPLETED") {
            errors.add("Can only refund completed payments. Current status: ${payment.status}")
        }

        if (errors.isNotEmpty()) {
            Log.w(TAG, "❌ Refund validation failed: ${errors.joinToString(", ")}")
        }

        return PaymentValidationResult(isValid = errors.isEmpty(), errors = errors)
    }

    fun validatePaymentAmount(amount: Double): PaymentValidationResult {
        val errors = mutableListOf<String>()

        if (amount <= 0) errors.add("Payment amount must be positive")
        if (amount > MAX_PAYMENT_AMOUNT) errors.add("Payment amount exceeds maximum limit")

        return PaymentValidationResult(isValid = errors.isEmpty(), errors = errors)
    }

    fun validateSellerPayment(payment: SellerPayment): PaymentValidationResult {
        val errors = mutableListOf<String>()

        if (payment.sellerId.isEmpty()) errors.add("Seller ID is empty")
        if (payment.buyerId.isEmpty()) errors.add("Buyer ID is empty")
        if (payment.orderId.isEmpty()) errors.add("Order ID is empty")
        if (payment.amount <= 0) errors.add("Payment amount must be positive")
        if (payment.paymentMethod.isEmpty()) errors.add("Payment method is empty")

        return PaymentValidationResult(isValid = errors.isEmpty(), errors = errors)
    }
}
