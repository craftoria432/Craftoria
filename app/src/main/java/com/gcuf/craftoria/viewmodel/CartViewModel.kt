package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.data.repository.CartRepository
import com.gcuf.craftoria.data.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch

class CartViewModel(
    private val cartRepository: CartRepository = CartRepository(),
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {

    companion object {
        const val SHIPPING_COST = 150.0
        private const val TAG = "CartViewModel"
    }

    // ✅ NEW: User ID for Firebase cart
    private val _currentUserId = MutableStateFlow<String?>(null)
    
    // ✅ NEW: Cart items from Firebase (real-time)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartCount: StateFlow<Int> = cartItems
        .map { items -> items.sumOf { it.quantity } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private val _orderState = MutableStateFlow<OrderState>(OrderState.Idle)
    val orderState: StateFlow<OrderState> = _orderState.asStateFlow()

    // ✅ NEW: Initialize cart for user
    fun initializeCart(userId: String) {
        if (_currentUserId.value == userId) {
            Log.d(TAG, "Cart already initialized for user: $userId")
            return
        }
        
        _currentUserId.value = userId
        Log.d(TAG, "🛒 Initializing Firebase cart for user: $userId")
        
        viewModelScope.launch {
            cartRepository.getCartItems(userId)
                .catch { e ->
                    Log.e(TAG, "❌ Error loading cart", e)
                    emit(emptyList())
                }
                .collect { items ->
                    // Load product details for each cart item
                    val itemsWithProducts = items.map { cartItem ->
                        try {
                            val productDoc = FirebaseFirestore.getInstance()
                                .collection("products")
                                .document(cartItem.productId)
                                .get()
                                .await()
                            
                            val product = productDoc.toObject(Product::class.java)?.copy(id = productDoc.id)
                            if (product != null) {
                                cartItem.copy(product = product)
                            } else {
                                cartItem
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to load product for cart item", e)
                            cartItem
                        }
                    }
                    _cartItems.value = itemsWithProducts
                    Log.d(TAG, "✅ Cart loaded: ${itemsWithProducts.size} items")
                }
        }
    }

    fun addToCart(
        userId: String,
        product: Product,
        price: Double,
        isNegotiated: Boolean,
        negotiationStatus: NegotiationStatus?
    ) {
        viewModelScope.launch {
            try {
                val cartItem = CartItem(
                    userId = userId,
                    productId = product.id,
                    product = product,
                    quantity = 1,
                    price = price,
                    originalPrice = product.price,
                    isNegotiated = isNegotiated,
                    negotiationStatus = negotiationStatus
                )
                
                val result = cartRepository.addToCart(cartItem)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Added to Firebase cart: ${product.title}")
                } else {
                    Log.e(TAG, "❌ Failed to add to cart: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error adding to cart", e)
            }
        }
    }

    fun updateCartItemPrice(
        productId: String,
        newPrice: Double,
        isNegotiated: Boolean,
        negotiationStatus: NegotiationStatus?
    ) {
        val userId = _currentUserId.value ?: return
        
        viewModelScope.launch {
            try {
                val result = cartRepository.updateNegotiationStatus(
                    productId = productId,
                    userId = userId,
                    newPrice = newPrice,
                    isNegotiated = isNegotiated,
                    negotiationStatus = negotiationStatus
                )
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Updated cart item price in Firebase")
                } else {
                    Log.e(TAG, "❌ Failed to update price: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating price", e)
            }
        }
    }

    fun updateQuantity(itemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(itemId)
            return
        }

        viewModelScope.launch {
            try {
                val result = cartRepository.updateQuantity(itemId, newQuantity)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Updated quantity in Firebase")
                } else {
                    Log.e(TAG, "❌ Failed to update quantity: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating quantity", e)
            }
        }
    }

    fun removeFromCart(itemId: String) {
        viewModelScope.launch {
            try {
                val result = cartRepository.removeFromCart(itemId)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Removed from Firebase cart")
                } else {
                    Log.e(TAG, "❌ Failed to remove: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error removing from cart", e)
            }
        }
    }

    fun clearCart() {
        val userId = _currentUserId.value ?: return
        
        viewModelScope.launch {
            try {
                val result = cartRepository.clearCart(userId)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Cleared Firebase cart")
                } else {
                    Log.e(TAG, "❌ Failed to clear cart: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error clearing cart", e)
            }
        }
    }

    fun getSubtotal(): Double {
        return _cartItems.value.sumOf { it.price * it.quantity }
    }

    fun getTotal(): Double {
        val subtotal = getSubtotal()
        val shipping = SHIPPING_COST
        return subtotal + shipping
    }

    fun placeOrder(
        userId: String,
        userName: String,
        deliveryInfo: DeliveryInfo,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            try {
                _orderState.value = OrderState.Loading

                val items = _cartItems.value
                if (items.isEmpty()) {
                    _orderState.value = OrderState.Error("Cart is empty")
                    return@launch
                }

                // Group items by seller
                val ordersBySeller = items.groupBy { it.product.sellerId }

                val allOrderIds = mutableListOf<String>()

                // Create separate order for each seller
                ordersBySeller.forEach { (sellerId, sellerItems) ->
                    val firstItem = sellerItems.first()

                    // ✅ FIX: Use cart item prices (negotiated or original)
                    val subtotal = sellerItems.sumOf { it.price * it.quantity }
                    val shipping = SHIPPING_COST
                    val discount = 0.0
                    val total = subtotal + shipping

                    val order = Order(
                        id = "",
                        buyerId = userId,
                        buyerName = userName,
                        sellerId = sellerId,
                        sellerName = firstItem.product.sellerName,
                        productId = firstItem.product.id,
                        productTitle = firstItem.product.title,
                        productImage = firstItem.product.imageUrls.firstOrNull() ?: "",
                        quantity = sellerItems.sumOf { it.quantity },
                        totalPrice = total,
                        status = OrderStatus.PENDING.name,
                        shippingAddress = "${deliveryInfo.address}, ${deliveryInfo.city}",
                        buyerPhone = deliveryInfo.phoneNumber,
                        items = sellerItems.map { cartItem ->
                            OrderItem(
                                productId = cartItem.product.id,
                                productTitle = cartItem.product.title,
                                productImage = cartItem.product.imageUrls.firstOrNull() ?: "",
                                sellerName = cartItem.product.sellerName,
                                quantity = cartItem.quantity,
                                price = cartItem.price,
                                isNegotiated = cartItem.isNegotiated
                            )
                        },
                        subtotal = subtotal,
                        shipping = shipping,
                        discount = discount,
                        deliveryInfo = deliveryInfo,
                        paymentMethod = paymentMethod,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    // ✅ Debug logging
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "📦 Creating order:")
                    Log.d(TAG, "   Product: ${order.productTitle}")
                    Log.d(TAG, "   Seller: ${order.sellerName}")
                    Log.d(TAG, "   Quantity: ${order.quantity}")
                    Log.d(TAG, "   Subtotal: ${order.subtotal}")
                    Log.d(TAG, "   Shipping: ${order.shipping}")
                    Log.d(TAG, "   Discount: ${order.discount}")
                    Log.d(TAG, "   Total: ${order.totalPrice}")
                    Log.d(TAG, "   Status: ${order.status}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    // Save to Firestore
                    val orderMap = order.toMap()

                    Log.d(TAG, "📤 Firestore map:")
                    orderMap.forEach { (key, value) ->
                        Log.d(TAG, "   $key: $value")
                    }

                    val docRef = FirebaseFirestore.getInstance()
                        .collection("orders")
                        .add(orderMap)
                        .await()

                    allOrderIds.add(docRef.id)
                    Log.d(TAG, "✅ Order created: ${docRef.id}")
                    
                    // ✅ Log activity for seller dashboard
                    try {
                        val activityData = mapOf(
                            "seller_id" to sellerId,
                            "type" to "NEW_ORDER",
                            "title" to "New Order Received",
                            "description" to "Order for ${firstItem.product.title} (${sellerItems.sumOf { it.quantity }} items)",
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "order_id" to docRef.id,
                            "product_id" to firstItem.product.id
                        )
                        
                        FirebaseFirestore.getInstance()
                            .collection("activities")
                            .add(activityData)
                            .await()
                        
                        Log.d(TAG, "✅ Activity logged for seller: $sellerId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to log activity", e)
                        // Don't fail the order if activity logging fails
                    }
                }

                // Clear cart after successful order
                clearCart()

                // ✅ Pass all order IDs as comma-separated string
                val orderIdsString = allOrderIds.joinToString(",")
                Log.d(TAG, "✅ All orders created: $orderIdsString")
                _orderState.value = OrderState.Success(orderId = orderIdsString)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to place order", e)
                _orderState.value = OrderState.Error(e.message ?: "Failed to place order")
            }
        }
    }

    fun resetOrderState() {
        _orderState.value = OrderState.Idle
    }
    // 🔁 Reorder: Add previous order items back to cart
    fun reorder(userId: String, order: Order) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔁 Reordering order: ${order.id}")
                
                // ✅ OPTIMIZED: Don't clear cart, just add items
                // This allows users to add reorder items to existing cart if they want
                
                val addedItems = mutableListOf<String>()
                
                // ✅ INSTANT FEEDBACK: Create temporary cart items immediately
                val tempCartItems = order.items.map { item ->
                    CartItem(
                        id = "temp_${item.productId}_${System.currentTimeMillis()}",
                        userId = userId,
                        productId = item.productId,
                        quantity = item.quantity,
                        price = item.price,
                        isNegotiated = false,
                        negotiationStatus = null,
                        addedAt = System.currentTimeMillis(),
                        product = Product(
                            id = item.productId,
                            title = item.productTitle,
                            price = item.price,
                            imageUrls = listOf(item.productImage),
                            sellerId = item.sellerId,
                            sellerName = item.sellerName
                        )
                    )
                }
                
                // Update cart state immediately for instant UI feedback
                _cartItems.value = _cartItems.value + tempCartItems
                
                order.items.forEach { item ->
                    // Fetch the actual product from Firestore to get latest data
                    try {
                        val productDoc = FirebaseFirestore.getInstance()
                            .collection("products")
                            .document(item.productId)
                            .get()
                            .await()
                        
                        if (productDoc.exists()) {
                            val product = productDoc.toObject(Product::class.java)?.copy(id = productDoc.id)
                            
                            if (product != null) {
                                // Add to cart with original quantity
                                repeat(item.quantity) {
                                    addToCart(
                                        userId = userId,
                                        product = product,
                                        price = product.price, // Use current price, not old price
                                        isNegotiated = false,
                                        negotiationStatus = null
                                    )
                                }
                                addedItems.add(item.productTitle)
                                Log.d(TAG, "✅ Reordered: ${item.productTitle} x${item.quantity}")
                            } else {
                                Log.e(TAG, "Product not found: ${item.productId}")
                            }
                        } else {
                            Log.e(TAG, "Product document doesn't exist: ${item.productId}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch product: ${item.productId}", e)
                    }
                }
                
                // Remove temporary items and reload cart with actual data
                initializeCart(userId)
                
                Log.d(TAG, "✅ Reorder complete. Cart size: ${_cartItems.value.size}, Added: ${addedItems.size} items")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reorder", e)
            }
        }
    }
}

sealed class OrderState {
    object Idle : OrderState()
    object Loading : OrderState()
    data class Success(val orderId: String) : OrderState()
    data class Error(val message: String) : OrderState()
}