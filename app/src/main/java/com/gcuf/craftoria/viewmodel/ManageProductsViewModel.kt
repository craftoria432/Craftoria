package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.repository.ProductFilter
import com.gcuf.craftoria.data.repository.ProductRepository
import com.gcuf.craftoria.data.repository.ProductSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ManageProductsViewModel(
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManageProductsState>(ManageProductsState.Loading)
    val uiState: StateFlow<ManageProductsState> = _uiState.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _currentFilter = MutableStateFlow(ProductFilter.ALL)
    val currentFilter: StateFlow<ProductFilter> = _currentFilter.asStateFlow()

    private val _currentSort = MutableStateFlow(ProductSort.NEWEST)
    val currentSort: StateFlow<ProductSort> = _currentSort.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    companion object {
        private const val TAG = "ManageProductsViewModel"
    }

    fun loadProducts(sellerId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ManageProductsState.Loading

                val result = productRepository.getSellerProducts(
                    sellerId = sellerId,
                    filter = _currentFilter.value,
                    sortBy = _currentSort.value
                )

                if (result.isSuccess) {
                    var products = result.getOrNull() ?: emptyList()
                    
                    // ✅ DEFENSIVE FILTER: Ensure no products without seller_id are shown
                    products = products.filter { product ->
                        if (product.sellerId.isBlank()) {
                            Log.w(TAG, "Filtering out product without seller_id: ${product.id}")
                            false
                        } else if (product.sellerId != sellerId) {
                            Log.w(TAG, "Filtering out product with mismatched seller_id: ${product.id}")
                            false
                        } else {
                            true
                        }
                    }
                    
                    _products.value = products
                    _uiState.value = if (products.isEmpty()) {
                        ManageProductsState.Empty
                    } else {
                        ManageProductsState.Success
                    }
                } else {
                    _uiState.value = ManageProductsState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load products"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load products", e)
                _uiState.value = ManageProductsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun filterProducts(filter: ProductFilter, sellerId: String) {
        _currentFilter.value = filter
        loadProducts(sellerId)
    }

    fun sortProducts(sort: ProductSort, sellerId: String) {
        _currentSort.value = sort
        loadProducts(sellerId)
    }

    fun searchProducts(query: String, sellerId: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            loadProducts(sellerId)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = ManageProductsState.Loading

                val result = productRepository.searchSellerProducts(sellerId, query)

                if (result.isSuccess) {
                    var products = result.getOrNull() ?: emptyList()
                    
                    // ✅ DEFENSIVE FILTER: Ensure no products without seller_id are shown
                    products = products.filter { product ->
                        if (product.sellerId.isBlank()) {
                            Log.w(TAG, "Filtering out product without seller_id: ${product.id}")
                            false
                        } else if (product.sellerId != sellerId) {
                            Log.w(TAG, "Filtering out product with mismatched seller_id: ${product.id}")
                            false
                        } else {
                            true
                        }
                    }
                    
                    _products.value = products
                    _uiState.value = if (products.isEmpty()) {
                        ManageProductsState.Empty
                    } else {
                        ManageProductsState.Success
                    }
                } else {
                    _uiState.value = ManageProductsState.Error(
                        result.exceptionOrNull()?.message ?: "Search failed"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _uiState.value = ManageProductsState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun toggleProductStatus(productId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            try {
                val result = productRepository.updateProductStatus(productId, !currentStatus)

                if (result.isSuccess) {
                    // Update local list
                    _products.value = _products.value.map { product ->
                        if (product.id == productId) {
                            product.copy(isActive = !currentStatus)
                        } else {
                            product
                        }
                    }
                } else {
                    _uiState.value = ManageProductsState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to update status"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle status", e)
                _uiState.value = ManageProductsState.Error(e.message ?: "Failed to update status")
            }
        }
    }

    fun updateStock(productId: String, newStock: Int) {
        viewModelScope.launch {
            try {
                val result = productRepository.updateProductStock(productId, newStock)

                if (result.isSuccess) {
                    // Update local list
                    _products.value = _products.value.map { product ->
                        if (product.id == productId) {
                            product.copy(stock = newStock)
                        } else {
                            product
                        }
                    }
                } else {
                    _uiState.value = ManageProductsState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to update stock"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update stock", e)
                _uiState.value = ManageProductsState.Error(e.message ?: "Failed to update stock")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ManageProductsState.Loading

                val result = productRepository.deleteProduct(productId)

                if (result.isSuccess) {
                    // Remove from local list
                    _products.value = _products.value.filter { it.id != productId }

                    _uiState.value = if (_products.value.isEmpty()) {
                        ManageProductsState.Empty
                    } else {
                        ManageProductsState.Success
                    }
                } else {
                    _uiState.value = ManageProductsState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to delete product"
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete product", e)
                _uiState.value = ManageProductsState.Error(e.message ?: "Failed to delete product")
            }
        }
    }
    fun resetState() {
        _uiState.value = ManageProductsState.Success
    }
}

sealed class ManageProductsState {
    object Loading : ManageProductsState()
    object Success : ManageProductsState()
    object Empty : ManageProductsState()
    data class Error(val message: String) : ManageProductsState()
}