package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * ✅ PRODUCTION-READY: Admin commission record
 * Tracks all commission earnings from orders
 */
@IgnoreExtraProperties
data class AdminCommission(
    var id: String = "",

    @get:PropertyName("order_id")
    @set:PropertyName("order_id")
    var orderId: String = "",

    @get:PropertyName("payment_id")
    @set:PropertyName("payment_id")
    var paymentId: String = "",

    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",

    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",

    @get:PropertyName("co_seller_store_id")
    @set:PropertyName("co_seller_store_id")
    var coSellerStoreId: String = "",

    @get:PropertyName("store_name")
    @set:PropertyName("store_name")
    var storeName: String = "",

    // Commission Details
    @get:PropertyName("subtotal")
    @set:PropertyName("subtotal")
    var subtotal: Double = 0.0,

    @get:PropertyName("commission_rate")
    @set:PropertyName("commission_rate")
    var commissionRate: Double = 0.05,  // 5% default

    @get:PropertyName("commission_amount")
    @set:PropertyName("commission_amount")
    var commissionAmount: Double = 0.0,

    @get:PropertyName("seller_payout")
    @set:PropertyName("seller_payout")
    var sellerPayout: Double = 0.0,

    // Status
    var status: String = CommissionStatus.PENDING.toString(),

    // Timestamps
    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis(),

    @get:PropertyName("paid_at")
    @set:PropertyName("paid_at")
    var paidAt: Long? = null
)

/**
 * ✅ PRODUCTION-READY: Admin earnings summary
 * Aggregated commission data for dashboard
 */
@IgnoreExtraProperties
data class AdminEarnings(
    var id: String = "admin_earnings",

    @get:PropertyName("total_commissions")
    @set:PropertyName("total_commissions")
    var totalCommissions: Double = 0.0,

    @get:PropertyName("pending_commissions")
    @set:PropertyName("pending_commissions")
    var pendingCommissions: Double = 0.0,

    @get:PropertyName("paid_commissions")
    @set:PropertyName("paid_commissions")
    var paidCommissions: Double = 0.0,

    @get:PropertyName("total_orders")
    @set:PropertyName("total_orders")
    var totalOrders: Int = 0,

    @get:PropertyName("average_commission_rate")
    @set:PropertyName("average_commission_rate")
    var averageCommissionRate: Double = 0.05,

    @get:PropertyName("last_updated")
    @set:PropertyName("last_updated")
    var lastUpdated: Long = System.currentTimeMillis()
)

/**
 * ✅ PRODUCTION-READY: Commission settings
 * Extends the global settings with commission-specific config
 */
@IgnoreExtraProperties
data class CommissionSettings(
    var id: String = "commission_settings",

    @get:PropertyName("commission_rate")
    @set:PropertyName("commission_rate")
    var commissionRate: Double = 5.0,  // Percentage (5%)

    @get:PropertyName("apply_to_shipping")
    @set:PropertyName("apply_to_shipping")
    var applyToShipping: Boolean = false,

    @get:PropertyName("apply_to_negotiated_prices")
    @set:PropertyName("apply_to_negotiated_prices")
    var applyToNegotiatedPrices: Boolean = true,

    @get:PropertyName("payment_settlement_days")
    @set:PropertyName("payment_settlement_days")
    var paymentSettlementDays: Int = 7,

    @get:PropertyName("enabled")
    @set:PropertyName("enabled")
    var enabled: Boolean = true,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis(),

    @get:PropertyName("updated_by")
    @set:PropertyName("updated_by")
    var updatedBy: String = ""
)

enum class CommissionStatus {
    PENDING,
    PROCESSING,
    PAID,
    FAILED;

    override fun toString(): String = name.lowercase()
}

/* ────────────────────────────────────────────────────────────────────────── */
/* FIRESTORE MAPPERS                                                          */
/* ────────────────────────────────────────────────────────────────────────── */

fun AdminCommission.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "order_id" to orderId,
    "payment_id" to paymentId,
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "co_seller_store_id" to coSellerStoreId,
    "store_name" to storeName,
    "subtotal" to subtotal,
    "commission_rate" to commissionRate,
    "commission_amount" to commissionAmount,
    "seller_payout" to sellerPayout,
    "status" to status,
    "created_at" to createdAt,
    "updated_at" to updatedAt,
    "paid_at" to (paidAt ?: 0L)
)

fun AdminEarnings.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "total_commissions" to totalCommissions,
    "pending_commissions" to pendingCommissions,
    "paid_commissions" to paidCommissions,
    "total_orders" to totalOrders,
    "average_commission_rate" to averageCommissionRate,
    "last_updated" to lastUpdated
)

fun CommissionSettings.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "commission_rate" to commissionRate,
    "apply_to_shipping" to applyToShipping,
    "apply_to_negotiated_prices" to applyToNegotiatedPrices,
    "payment_settlement_days" to paymentSettlementDays,
    "enabled" to enabled,
    "updated_at" to updatedAt,
    "updated_by" to updatedBy
)
