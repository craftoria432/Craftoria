package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class DashboardStats(
    @get:PropertyName("total_products")
    @set:PropertyName("total_products")
    var totalProducts: Int = 0,

    @get:PropertyName("active_products")
    @set:PropertyName("active_products")
    var activeProducts: Int = 0,

    @get:PropertyName("total_orders")
    @set:PropertyName("total_orders")
    var totalOrders: Int = 0,

    @get:PropertyName("pending_orders")
    @set:PropertyName("pending_orders")
    var pendingOrders: Int = 0,

    @get:PropertyName("processing_orders")
    @set:PropertyName("processing_orders")
    var processingOrders: Int = 0,

    @get:PropertyName("active_orders")
    @set:PropertyName("active_orders")
    var activeOrders: Int = 0,

    @get:PropertyName("total_revenue")
    @set:PropertyName("total_revenue")
    var totalRevenue: Double = 0.0,

    @get:PropertyName("monthly_revenue")
    @set:PropertyName("monthly_revenue")
    var monthlyRevenue: Double = 0.0,

    @get:PropertyName("total_sales")
    @set:PropertyName("total_sales")
    var totalSales: Double = 0.0,

    @get:PropertyName("month_sales")
    @set:PropertyName("month_sales")
    var monthSales: Double = 0.0,

    @get:PropertyName("sales_growth")
    @set:PropertyName("sales_growth")
    var salesGrowth: Double = 0.0,

    @get:PropertyName("products_this_week")
    @set:PropertyName("products_this_week")
    var productsThisWeek: Int = 0,

    @get:PropertyName("total_customers")
    @set:PropertyName("total_customers")
    var totalCustomers: Int = 0,

    @get:PropertyName("low_stock_count")
    @set:PropertyName("low_stock_count")
    var lowStockCount: Int = 0
)

data class Activity(
    var id: String = "",
    
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",
    
    var type: String = "",
    var title: String = "",
    var description: String = "",
    
    var timestamp: Any? = null,
    
    @get:PropertyName("order_id")
    @set:PropertyName("order_id")
    var orderId: String = "",
    
    @get:PropertyName("product_id")
    @set:PropertyName("product_id")
    var productId: String = ""
)

enum class ActivityType {
    // Order-related activities (Blue - Shopping Cart 🛒)
    NEW_ORDER,
    ORDER_CONFIRMED,
    ORDER_CANCELLED,
    
    // Product-related activities (Green - Add Box ➕)
    PRODUCT_ADDED,
    PRODUCT_UPDATED,
    PRODUCT_APPROVED,
    PRODUCT_REJECTED,
    
    // Shipping/Delivery activities (Purple - Local Shipping 📦)
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_PROCESSING,
    ORDER_OUT_FOR_DELIVERY,
    
    // Stock/Inventory activities (Orange - Info ℹ️)
    PRODUCT_SOLD_OUT,
    LOW_STOCK_ALERT,
    STOCK_REPLENISHED,
    
    // Payment activities (Green - Add Box ➕)
    PAYMENT_RECEIVED,
    PAYOUT_PROCESSED,
    
    // Store activities (Blue - Shopping Cart 🛒)
    STORE_RATING_RECEIVED,
    NEGOTIATION_REQUEST,
    
    // System activities (Gray - Info ℹ️)
    ACCOUNT_VERIFIED,
    PROFILE_UPDATED,
    SETTINGS_CHANGED;

    override fun toString(): String = name
}

// Helper to convert Activity timestamp
fun Activity.getTimestampLong(): Long = when (val ts = timestamp) {
    is Long -> ts
    is com.google.firebase.Timestamp -> ts.toDate().time
    else -> System.currentTimeMillis()
}
