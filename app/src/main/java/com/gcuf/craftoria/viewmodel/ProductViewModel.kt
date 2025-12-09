package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.repository.ProductRepository
import com.gcuf.craftoria.utils.CloudinaryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel(
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val _productState = MutableStateFlow<ProductState>(ProductState.Idle)
    val productState: StateFlow<ProductState> = _productState.asStateFlow()

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts.asStateFlow()

    // 👉 HomeScreen expects products → so we expose allProducts as products
    val products: StateFlow<List<Product>> get() = allProducts

    private val _sellerProducts = MutableStateFlow<List<Product>>(emptyList())
    val sellerProducts: StateFlow<List<Product>> = _sellerProducts.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    // 👉 HomeScreen expects isLoading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAllProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            productRepository.getAllProducts().collect { products ->
                _allProducts.value = products
                _isLoading.value = false
            }
        }
    }

    fun loadSellerProducts(sellerId: String) {
        viewModelScope.launch {
            productRepository.getProductsBySeller(sellerId).collect { products ->
                _sellerProducts.value = products
            }
        }
    }

    fun createProduct(
        context: Context,
        product: Product,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                val imageUrls = mutableListOf<String>()
                imageUris.forEachIndexed { index, uri ->
                    _uploadProgress.value = ((index + 1) * 100 / imageUris.size)
                    val url = CloudinaryManager.uploadImage(context, uri)
                    imageUrls.add(url)
                }

                val productWithImages = product.copy(images = imageUrls)
                val result = productRepository.createProduct(productWithImages)

                _productState.value = when {
                    result.isSuccess -> ProductState.Success("Product created!")
                    else -> ProductState.Error(result.exceptionOrNull()?.message ?: "Failed")
                }

            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "Upload failed")
            } finally {
                _uploadProgress.value = 0
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            val result = productRepository.updateProduct(product)

            _productState.value = when {
                result.isSuccess -> ProductState.Success("Product updated!")
                else -> ProductState.Error(result.exceptionOrNull()?.message ?: "Update failed")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading

            val result = productRepository.deleteProduct(productId)

            _productState.value = when {
                result.isSuccess -> ProductState.Success("Product deleted!")
                else -> ProductState.Error(result.exceptionOrNull()?.message ?: "Delete failed")
            }
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            val result = productRepository.searchProducts(query)
            result.onSuccess { products ->
                _allProducts.value = products
            }
        }
    }

    fun resetProductState() {
        _productState.value = ProductState.Idle
    }
}

sealed class ProductState {
    object Idle : ProductState()
    object Loading : ProductState()
    data class Success(val message: String) : ProductState()
    data class Error(val message: String) : ProductState()
}
