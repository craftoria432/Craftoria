package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.OrderRefundStatus
import com.gcuf.craftoria.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.gcuf.craftoria.data.model.getStatusEnum
import com.gcuf.craftoria.data.model.getRefundStatusEnum
import com.gcuf.craftoria.data.model.getCreatedAtLong
import com.google.firebase.firestore.ListenerRegistration

class OrderViewModel : ViewModel() {
    private val orderRepository = OrderRepository()
    private var ordersListener: ListenerRegistration? = null

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

    private val _currentSort = MutableStateFlow("date_desc")
    val currentSort: StateFlow<String> = _currentSort.asStateFlow()

    fun loadUserOrders(userId: String) {
        if (userId.isEmpty()) {
            Log.w("OrderViewModel", "Empty userId provided")
            _isLoading.value = false
            return
        }

        // Cancel any existing listener before starting a new one
        ordersListener?.remove()

        _isLoading.value = true
        ordersListener = orderRepository.observeUserOrders(userId) { ordersList ->
            Log.d("OrderViewModel", "Real-time update: ${ordersList.size} orders")
            _orders.value = ordersList
            applyFilter(_currentFilter.value)
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
    }

    fun filterOrders(status: OrderStatus?) {
        _currentFilter.value = status
        applyFilter(status)
    }

    fun sortOrders(sortOption: String) {
        _currentSort.value = sortOption
        val sorted = when (sortOption) {
            "date_desc" -> _filteredOrders.value.sortedByDescending { it.getCreatedAtLong() }
            "date_asc" -> _filteredOrders.value.sortedBy { it.getCreatedAtLong() }
            "amount_desc" -> _filteredOrders.value.sortedByDescending { it.totalPrice }
            "amount_asc" -> _filteredOrders.value.sortedBy { it.totalPrice }
            else -> _filteredOrders.value
        }
        _filteredOrders.value = sorted
    }

    private fun applyFilter(status: OrderStatus?) {
        val filtered = if (status == null) {
            _orders.value
        } else {
            _orders.value.filter { order ->
                val orderStatus = order.getStatusEnum()
                val refundStatus = order.getRefundStatusEnum()
                
                // ✅ CRITICAL: Exclude refunded orders from ALL tabs
                // An order with refund status = COMPLETED should NOT appear in any tab
                if (refundStatus == com.gcuf.craftoria.data.model.OrderRefundStatus.COMPLETED) {
                    return@filter false
                }
                
                // Only show orders matching the selected status
                orderStatus == status
            }
        }

        // Apply current sort after filtering
        _filteredOrders.value = when (_currentSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.getCreatedAtLong() }
            "date_asc" -> filtered.sortedBy { it.getCreatedAtLong() }
            "amount_desc" -> filtered.sortedByDescending { it.totalPrice }
            "amount_asc" -> filtered.sortedBy { it.totalPrice }
            else -> filtered
        }
    }

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            try {
                _selectedOrder.value = null
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
                                status = OrderStatus.CANCELLED.toString(),
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

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                _orderActionState.value = OrderActionState.Loading

                val result = orderRepository.deleteOrder(orderId)

                if (result.isSuccess) {
                    // Remove from local list
                    _orders.value = _orders.value.filter { it.id != orderId }
                    applyFilter(_currentFilter.value)

                    _orderActionState.value = OrderActionState.Success("Order deleted successfully")
                } else {
                    _orderActionState.value = OrderActionState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to delete order"
                    )
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Delete order error", e)
                _orderActionState.value = OrderActionState.Error(e.message ?: "Failed to delete order")
            }
        }
    }

    fun deleteMultipleOrders(orderIds: List<String>) {
        viewModelScope.launch {
            try {
                _orderActionState.value = OrderActionState.Loading

                val result = orderRepository.deleteMultipleOrders(orderIds)

                if (result.isSuccess) {
                    // Remove from local list
                    _orders.value = _orders.value.filter { it.id !in orderIds }
                    applyFilter(_currentFilter.value)

                    _orderActionState.value = OrderActionState.Success("${orderIds.size} orders deleted successfully")
                } else {
                    _orderActionState.value = OrderActionState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to delete orders"
                    )
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Delete multiple orders error", e)
                _orderActionState.value = OrderActionState.Error(e.message ?: "Failed to delete orders")
            }
        }
    }

    // ✅ DELETED: reorder() function
    // The reorder function is now only in CartViewModel

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