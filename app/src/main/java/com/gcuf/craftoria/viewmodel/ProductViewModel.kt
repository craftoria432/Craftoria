package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.repository.ProductRepository
import com.gcuf.craftoria.utils.CloudinaryManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductViewModel(
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _productState = MutableStateFlow<ProductState>(ProductState.Idle)
    val productState: StateFlow<ProductState> = _productState.asStateFlow()

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts.asStateFlow()

    val products: StateFlow<List<Product>> get() = allProducts

    private val _sellerProducts = MutableStateFlow<List<Product>>(emptyList())
    val sellerProducts: StateFlow<List<Product>> = _sellerProducts.asStateFlow()

    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    init {
        loadAllProducts()
    }

    fun loadAllProducts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d("ProductViewModel", "🔍 LOADING PRODUCTS FROM FIREBASE...")
                Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                val snapshot = db.collection("products")
                    .whereEqualTo("is_active", true)
                    .whereEqualTo("approval_status", "approved")  // ✅ NEW: Only approved products
                    .get()
                    .await()

                Log.d("ProductViewModel", "📦 Total documents found: ${snapshot.size()}")
                Log.d("ProductViewModel", "")

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val rawData = doc.data

                        Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d("ProductViewModel", "📄 Document ID: ${doc.id}")
                        Log.d("ProductViewModel", "   Raw data keys: ${rawData?.keys}")
                        Log.d("ProductViewModel", "   title: ${rawData?.get("title")}")
                        Log.d("ProductViewModel", "   seller_name: ${rawData?.get("seller_name")}")
                        Log.d("ProductViewModel", "   seller_verified: ${rawData?.get("seller_verified")}")
                        Log.d("ProductViewModel", "   is_negotiable: ${rawData?.get("is_negotiable")}")
                        Log.d("ProductViewModel", "   stock: ${rawData?.get("stock")}")
                        Log.d("ProductViewModel", "   image_urls: ${rawData?.get("image_urls")}")

                        val product = doc.toObject(Product::class.java)?.copy(id = doc.id)

                        if (product != null) {
                            Log.d("ProductViewModel", "")
                            Log.d("ProductViewModel", "✅ PARSED PRODUCT:")
                            Log.d("ProductViewModel", "   title: ${product.title}")
                            Log.d("ProductViewModel", "   sellerName: '${product.sellerName}'")
                            Log.d("ProductViewModel", "   sellerVerified: ${product.sellerVerified}")
                            Log.d("ProductViewModel", "   isNegotiable: ${product.isNegotiable}")
                            Log.d("ProductViewModel", "   stock: ${product.stock}")
                            Log.d("ProductViewModel", "   imageUrls: ${product.imageUrls}")
                        } else {
                            Log.e("ProductViewModel", "   ❌ FAILED TO PARSE")
                        }

                        Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d("ProductViewModel", "")

                        product
                    } catch (e: Exception) {
                        Log.e("ProductViewModel", "❌ Error parsing doc ${doc.id}", e)
                        null
                    }
                }

                _allProducts.value = list
                _filteredProducts.value = list

                Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d("ProductViewModel", "✅ TOTAL LOADED: ${list.size} products")
                Log.d("ProductViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e("ProductViewModel", "❌ loadAllProducts error", e)
                tryLoadWithoutFilter()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun tryLoadWithoutFilter() {
        try {
            Log.d("ProductViewModel", "⚠️ Trying fallback - loading all products...")

            val snapshot = db.collection("products").get().await()

            Log.d("ProductViewModel", "📦 Fallback found ${snapshot.size()} documents")

            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ProductViewModel", "Error parsing doc ${doc.id}", e)
                    null
                }
            }

            _allProducts.value = list
            _filteredProducts.value = list

            Log.d("ProductViewModel", "✅ Fallback loaded ${list.size} products!")

        } catch (e: Exception) {
            Log.e("ProductViewModel", "❌ Fallback also failed", e)
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

                val productWithImages = product.copy(imageUrls = imageUrls)
                val result = productRepository.createProduct(productWithImages)

                _productState.value =
                    if (result.isSuccess) ProductState.Success("Product created!", result.getOrNull() ?: "")
                    else ProductState.Error(result.exceptionOrNull()?.message ?: "Failed")

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
            _productState.value =
                if (result.isSuccess) ProductState.Success("Product updated!")
                else ProductState.Error(result.exceptionOrNull()?.message ?: "Update failed")
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _productState.value = ProductState.Loading
            val result = productRepository.deleteProduct(productId)
            _productState.value =
                if (result.isSuccess) ProductState.Success("Product deleted!")
                else ProductState.Error(result.exceptionOrNull()?.message ?: "Delete failed")
        }
    }

    fun searchProducts(query: String) {
        val q = query.trim()

        if (q.isEmpty()) {
            _filteredProducts.value = _allProducts.value
            return
        }

        _filteredProducts.value = _allProducts.value.filter { product ->
            product.title.contains(q, ignoreCase = true) ||
                    product.description.contains(q, ignoreCase = true) ||
                    product.category.contains(q, ignoreCase = true) ||
                    product.sellerName.contains(q, ignoreCase = true)
        }

        Log.d("ProductViewModel", "🔍 Search '$q' → ${_filteredProducts.value.size} results")
    }

    fun resetProductState() {
        _productState.value = ProductState.Idle
    }
}

sealed class ProductState {
    object Idle : ProductState()
    object Loading : ProductState()
    data class Success(val message: String, val productId: String = "") : ProductState()
    data class DraftSaved(val message: String, val draftId: String) : ProductState()
    data class Error(val message: String) : ProductState()
}