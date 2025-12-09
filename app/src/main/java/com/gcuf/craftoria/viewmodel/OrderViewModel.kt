package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderViewModel(
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _filteredOrders = MutableStateFlow<List<Order>>(emptyList())
    val filteredOrders: StateFlow<List<Order>> = _filteredOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _orderActionState = MutableStateFlow<OrderActionState>(OrderActionState.Idle)
    val orderActionState: StateFlow<OrderActionState> = _orderActionState.asStateFlow()

    private val _currentFilter = MutableStateFlow<OrderStatus?>(null)
    val currentFilter: StateFlow<OrderStatus?> = _currentFilter.asStateFlow()

    fun loadUserOrders(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val result = orderRepository.getUserOrders(userId)

                if (result.isSuccess) {
                    _orders.value = result.getOrNull() ?: emptyList()
                    applyFilter(_currentFilter.value)
                } else {
                    Log.e("OrderViewModel", "Failed to load orders", result.exceptionOrNull())
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Load orders error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterOrders(status: OrderStatus?) {
        _currentFilter.value = status
        applyFilter(status)
    }

    private fun applyFilter(status: OrderStatus?) {
        _filteredOrders.value = if (status == null) {
            _orders.value
        } else {
            _orders.value.filter { it.status == status }
        }
    }

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            try {
                val result = orderRepository.getOrderById(orderId)

                if (result.isSuccess) {
                    _selectedOrder.value = result.getOrNull()
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Load order details error", e)
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _orderActionState.value = OrderActionState.Loading

                val result = orderRepository.cancelOrder(orderId)

                if (result.isSuccess) {
                    // Update local list
                    _orders.value = _orders.value.map { order ->
                        if (order.id == orderId) {
                            order.copy(
                                status = OrderStatus.CANCELLED,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else {
                            order
                        }
                    }
                    applyFilter(_currentFilter.value)

                    _orderActionState.value = OrderActionState.Success("Order cancelled successfully")
                } else {
                    _orderActionState.value = OrderActionState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to cancel order"
                    )
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Cancel order error", e)
                _orderActionState.value = OrderActionState.Error(e.message ?: "Failed to cancel order")
            }
        }
    }

    fun reorder(order: Order, onAddToCart: (Order) -> Unit) {
        viewModelScope.launch {
            try {
                _orderActionState.value = OrderActionState.Loading

                // Add order items back to cart
                onAddToCart(order)

                _orderActionState.value = OrderActionState.Success("Items added to cart")

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Reorder error", e)
                _orderActionState.value = OrderActionState.Error("Failed to add items to cart")
            }
        }
    }

    fun resetActionState() {
        _orderActionState.value = OrderActionState.Idle
    }

    fun clearSelectedOrder() {
        _selectedOrder.value = null
    }
}

sealed class OrderActionState {
    object Idle : OrderActionState()
    object Loading : OrderActionState()
    data class Success(val message: String) : OrderActionState()
    data class Error(val message: String) : OrderActionState()
}