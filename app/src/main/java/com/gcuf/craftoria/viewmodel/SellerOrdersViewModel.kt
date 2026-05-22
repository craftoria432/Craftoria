package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.repository.OrderRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SellerOrdersViewModel(
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SellerOrdersState>(SellerOrdersState.Loading)
    val uiState: StateFlow<SellerOrdersState> = _uiState.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _currentFilter = MutableStateFlow<OrderStatus?>(null)
    val currentFilter: StateFlow<OrderStatus?> = _currentFilter.asStateFlow()

    private val _newOrdersCount = MutableStateFlow(0)
    val newOrdersCount: StateFlow<Int> = _newOrdersCount.asStateFlow()

    private var newOrdersListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "SellerOrdersViewModel"
    }

    override fun onCleared() {
        super.onCleared()
        newOrdersListener?.remove()
    }

    fun loadOrders(sellerId: String, runMigration: Boolean = false) {
        // Only show Loading on very first load when list is truly empty
        // This prevents the "No orders found" flicker on tab switches
        if (_orders.value.isEmpty()) {
            _uiState.value = SellerOrdersState.Loading
        }

        viewModelScope.launch {
            try {
                // ✅ Run migration on first load to populate missing store IDs
                if (runMigration) {
                    Log.d(TAG, "🔄 Running order store migration...")
                    val migrationResult = com.gcuf.craftoria.utils.OrderStoreMigration.migrateSellerOrders(sellerId)
                    if (migrationResult.isSuccess) {
                        val count = migrationResult.getOrNull() ?: 0
                        if (count > 0) {
                            Log.d(TAG, "✅ Migration updated $count orders")
                        }
                    }
                }

                val result = orderRepository.getSellerOrders(
                    sellerId = sellerId,
                    status = _currentFilter.value
                )

                if (result.isSuccess) {
                    val orders = result.getOrNull() ?: emptyList()
                    _orders.value = orders
                    _uiState.value = if (orders.isEmpty()) {
                        SellerOrdersState.Empty
                    } else {
                        SellerOrdersState.Success
                    }
                    loadNewOrdersCount(sellerId)
                } else {
                    _uiState.value = SellerOrdersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load orders"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load orders", e)
                _uiState.value = SellerOrdersState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadNewOrdersCount(sellerId: String) {
        // Remove any existing listener before attaching a new one
        newOrdersListener?.remove()
        newOrdersListener = orderRepository.observeNewOrdersCount(sellerId) { count ->
            _newOrdersCount.value = count
        }
    }

    fun filterOrders(status: OrderStatus?, sellerId: String) {
        _currentFilter.value = status
        loadOrders(sellerId)
    }

    fun acceptOrder(orderId: String, sellerId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = SellerOrdersState.Loading

                val result = orderRepository.acceptOrder(orderId)

                if (result.isSuccess) {
                    // Reload orders
                    loadOrders(sellerId)
                    _uiState.value = SellerOrdersState.ActionSuccess("Order accepted successfully")
                } else {
                    _uiState.value = SellerOrdersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to accept order"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to accept order", e)
                _uiState.value = SellerOrdersState.Error(e.message ?: "Failed to accept order")
            }
        }
    }

    fun rejectOrder(
        orderId: String,
        reason: String,
        details: String,
        sellerId: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = SellerOrdersState.Loading

                val result = orderRepository.rejectOrder(orderId, reason, details)

                if (result.isSuccess) {
                    // Reload orders
                    loadOrders(sellerId)
                    _uiState.value = SellerOrdersState.ActionSuccess("Order rejected successfully")
                } else {
                    _uiState.value = SellerOrdersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to reject order"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to reject order", e)
                _uiState.value = SellerOrdersState.Error(e.message ?: "Failed to reject order")
            }
        }
    }

    fun markAsShipped(
        orderId: String,
        courierName: String,
        trackingNumber: String,
        expectedDeliveryDate: Long,
        sellerId: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = SellerOrdersState.Loading

                val result = orderRepository.markAsShipped(
                    orderId = orderId,
                    courierName = courierName,
                    trackingNumber = trackingNumber,
                    expectedDeliveryDate = expectedDeliveryDate
                )

                if (result.isSuccess) {
                    // Reload orders
                    loadOrders(sellerId)
                    _uiState.value = SellerOrdersState.ActionSuccess("Order marked as shipped")
                } else {
                    _uiState.value = SellerOrdersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to mark as shipped"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as shipped", e)
                _uiState.value = SellerOrdersState.Error(e.message ?: "Failed to mark as shipped")
            }
        }
    }

    fun markAsDelivered(orderId: String, sellerId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = SellerOrdersState.Loading

                val result = orderRepository.markAsDelivered(orderId)

                if (result.isSuccess) {
                    // Reload orders
                    loadOrders(sellerId)
                    _uiState.value = SellerOrdersState.ActionSuccess("Order marked as delivered")
                } else {
                    _uiState.value = SellerOrdersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to mark as delivered"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as delivered", e)
                _uiState.value = SellerOrdersState.Error(e.message ?: "Failed to mark as delivered")
            }
        }
    }

    fun deleteOrder(orderId: String, sellerId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = SellerOrdersState.Loading

                val result = orderRepository.deleteOrder(orderId)

                if (result.isSuccess) {
                    // Remove from local list
                    _orders.value = _orders.value.filter { it.id != orderId }
                    _uiState.value = SellerOrdersState.ActionSuccess("Order deleted successfully")
                } else {
                    _uiState.value = SellerOrdersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to delete order"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete order", e)
                _uiState.value = SellerOrdersState.Error(e.message ?: "Failed to delete order")
            }
        }
    }

    fun deleteMultipleOrders(orderIds: List<String>, sellerId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = SellerOrdersState.Loading

                val result = orderRepository.deleteMultipleOrders(orderIds)

                if (result.isSuccess) {
                    // Remove from local list
                    _orders.value = _orders.value.filter { it.id !in orderIds }
                    _uiState.value = SellerOrdersState.ActionSuccess("${orderIds.size} orders deleted successfully")
                } else {
                    _uiState.value = SellerOrdersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to delete orders"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete multiple orders", e)
                _uiState.value = SellerOrdersState.Error(e.message ?: "Failed to delete orders")
            }
        }
    }

    fun resetState() {
        _uiState.value = SellerOrdersState.Success
    }
}

sealed class SellerOrdersState {
    object Loading : SellerOrdersState()
    object Success : SellerOrdersState()
    object Empty : SellerOrdersState()
    data class ActionSuccess(val message: String) : SellerOrdersState()
    data class Error(val message: String) : SellerOrdersState()
}