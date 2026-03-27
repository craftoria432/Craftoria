package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.CoSellerStore
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AddProductViewModel(
    private val productRepository: ProductRepository = ProductRepository()
) : ViewModel() {
    private val _editingProductId = MutableStateFlow<String?>(null)
    val editingProductId: StateFlow<String?> = _editingProductId.asStateFlow()

    private val _existingImageUrls = MutableStateFlow<List<String>>(emptyList())
    val existingImageUrls: StateFlow<List<String>> = _existingImageUrls.asStateFlow()

    // Form pre-fill states
    val titleState = mutableStateOf("")
    val descriptionState = mutableStateOf("")
    val priceState = mutableStateOf("")
    val stockState = mutableStateOf("")
    val categoryState = mutableStateOf("")
    val weightKgState = mutableStateOf("")
    val minimumPriceState = mutableStateOf("")
    val autoAcceptDiscountState = mutableStateOf("")
    val selectedStoreIdState = mutableStateOf("")

    private val _productState = MutableStateFlow<ProductState>(ProductState.Idle)
    val productState: StateFlow<ProductState> = _productState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _userStores = MutableStateFlow<List<CoSellerStore>>(emptyList())
    val userStores: StateFlow<List<CoSellerStore>> = _userStores.asStateFlow()

    private val _isNegotiationEnabled = MutableStateFlow(false)
    val isNegotiationEnabled: StateFlow<Boolean> = _isNegotiationEnabled.asStateFlow()

    private val _specifications = MutableStateFlow<Map<String, String>>(emptyMap())
    val specifications: StateFlow<Map<String, String>> = _specifications.asStateFlow()

    companion object {
        const val MAX_IMAGES = 5
    }

    fun addSpecification(key: String, value: String) {
        val currentSpecs = _specifications.value.toMutableMap()
        currentSpecs[key] = value
        _specifications.value = currentSpecs
    }

    fun removeSpecification(key: String) {
        val currentSpecs = _specifications.value.toMutableMap()
        currentSpecs.remove(key)
        _specifications.value = currentSpecs
    }

    fun loadUserStores(userId: String) {
        viewModelScope.launch {
            try {
                val result = productRepository.getUserStores(userId)
                if (result.isSuccess) {
                    _userStores.value = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("AddProductViewModel", "Failed to load stores", e)
            }
        }
    }

    fun addImage(uri: Uri) {
        val currentImages = _selectedImages.value
        if (currentImages.size < MAX_IMAGES) {
            _selectedImages.value = currentImages + uri
        } else {
            _productState.value = ProductState.Error("Maximum $MAX_IMAGES images allowed")
        }
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value.filter { it != uri }
    }

    fun toggleNegotiation(enabled: Boolean) {
        _isNegotiationEnabled.value = enabled
    }

    fun setAutoAcceptDiscount(discount: String, currentPrice: String) {
        autoAcceptDiscountState.value = discount
        
        // Auto-calculate minimum price if discount is set and price is available
        val discountValue = discount.toIntOrNull()
        val priceValue = currentPrice.toDoubleOrNull()
        
        if (discountValue != null && priceValue != null && discountValue > 0) {
            // Set minimum price to be 5-10% more discount than auto-accept
            // For example: if auto-accept is 10%, minimum could be 20-25%
            val minimumDiscountPercent = when {
                discountValue <= 10 -> discountValue + 10  // 10% -> 20%
                discountValue <= 15 -> discountValue + 10  // 15% -> 25%
                else -> discountValue + 5                   // 20% -> 25%
            }
            
            val minimumPrice = priceValue * (1 - minimumDiscountPercent / 100.0)
            minimumPriceState.value = minimumPrice.toInt().toString()
        }
    }

    fun publishProduct(
        context: Context,
        title: String,
        description: String,
        category: String,
        price: Double,
        stock: Int,
        weightKg: Double,
        coSellerStoreId: String,
        minimumPrice: Double,
        autoAcceptDiscount: Int,
        sellerId: String,
        sellerName: String,
        sellerVerified: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                // Validate
                if (title.isBlank() || description.isBlank() || category.isBlank()) {
                    _productState.value = ProductState.Error("Please fill all required fields")
                    return@launch
                }

                if (price <= 0) {
                    _productState.value = ProductState.Error("Price must be greater than 0")
                    return@launch
                }

                if (stock <= 0) {
                    _productState.value = ProductState.Error("Stock must be at least 1")
                    return@launch
                }

                if (_selectedImages.value.isEmpty()) {
                    _productState.value = ProductState.Error("Please add at least one product image")
                    return@launch
                }

                // Calculate auto-accept price from discount
                val autoAcceptPrice = if (_isNegotiationEnabled.value && autoAcceptDiscount > 0) {
                    price * (1 - autoAcceptDiscount / 100.0)
                } else {
                    0.0
                }

                // Create product
                val product = Product(
                    sellerId = sellerId,
                    sellerName = sellerName,
                    sellerVerified = sellerVerified,
                    title = title,
                    description = description,
                    category = category,
                    price = price,
                    stock = stock,
                    weightKg = weightKg,
                    coSellerStoreId = coSellerStoreId,
                    isNegotiable = _isNegotiationEnabled.value,
                    minimumPrice = if (_isNegotiationEnabled.value) minimumPrice else 0.0,
                    autoAcceptPrice = autoAcceptPrice,
                    autoAcceptDiscount = autoAcceptDiscount,
                    specifications = _specifications.value,
                    isDraft = false,
                    isActive = true,
                    approvalStatus = "pending"  // ✅ NEW: Set to pending for approval
                )

                // Upload and create
                val result = productRepository.createProduct(
                    context = context,
                    product = product,
                    imageUris = _selectedImages.value
                )

                if (result.isSuccess) {
                    val productId = result.getOrNull() ?: ""
                    
                    // ✅ Log activity for seller dashboard
                    try {
                        val activityData = mapOf(
                            "seller_id" to sellerId,
                            "type" to "PRODUCT_ADDED",
                            "title" to "Product Added",
                            "description" to "Added $title to your store",
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "order_id" to "",
                            "product_id" to productId
                        )
                        
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("activities")
                            .add(activityData)
                            .await()
                        
                        Log.d("AddProductViewModel", "✅ Activity logged for product: $productId")
                    } catch (e: Exception) {
                        Log.e("AddProductViewModel", "Failed to log activity", e)
                        // Don't fail the product creation if activity logging fails
                    }
                    
                    _productState.value = ProductState.Success(
                        "Product published successfully!",
                        productId
                    )
                } else {
                    _productState.value = ProductState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to publish product"
                    )
                }

            } catch (e: Exception) {
                Log.e("AddProductViewModel", "Publish product error", e)
                _productState.value = ProductState.Error(e.message ?: "Failed to publish product")
            }
        }
    }

    fun saveDraft(
        context: Context,
        title: String,
        description: String,
        category: String,
        price: Double,
        stock: Int,
        weightKg: Double,
        coSellerStoreId: String,
        minimumPrice: Double,
        autoAcceptDiscount: Int,
        sellerId: String,
        sellerName: String,
        sellerVerified: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading

                val product = Product(
                    sellerId = sellerId,
                    sellerName = sellerName,
                    sellerVerified = sellerVerified,
                    title = title,
                    description = description,
                    category = category,
                    price = price,
                    stock = stock,
                    weightKg = weightKg,
                    coSellerStoreId = coSellerStoreId,
                    isNegotiable = _isNegotiationEnabled.value,
                    minimumPrice = minimumPrice,
                    autoAcceptDiscount = autoAcceptDiscount,
                    specifications = _specifications.value,
                    isDraft = true,
                    isActive = false
                )

                val result = productRepository.saveDraft(
                    context = context,
                    product = product,
                    imageUris = _selectedImages.value
                )

                if (result.isSuccess) {
                    _productState.value = ProductState.DraftSaved(
                        "Draft saved successfully!",
                        result.getOrNull() ?: ""
                    )
                } else {
                    _productState.value = ProductState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to save draft"
                    )
                }

            } catch (e: Exception) {
                Log.e("AddProductViewModel", "Save draft error", e)
                _productState.value = ProductState.Error(e.message ?: "Failed to save draft")
            }
        }
    }
    fun loadProductForEditing(productId: String) {
        viewModelScope.launch {
            try {
                _editingProductId.value = productId
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("products")
                    .document(productId)
                    .get()
                    .await()

                if (doc.exists()) {
                    titleState.value = doc.getString("title") ?: ""
                    descriptionState.value = doc.getString("description") ?: ""
                    priceState.value = (doc.getDouble("price") ?: 0.0).toInt().toString()
                    stockState.value = (doc.getLong("stock") ?: 0L).toString()
                    categoryState.value = doc.getString("category") ?: ""
                    weightKgState.value = (doc.getDouble("weightKg") ?: 0.0).toString()
                    selectedStoreIdState.value = doc.getString("coSellerStoreId") ?: ""
                    minimumPriceState.value = (doc.getDouble("minimumPrice") ?: 0.0).let {
                        if (it > 0) it.toInt().toString() else ""
                    }
                    autoAcceptDiscountState.value = (doc.getLong("autoAcceptDiscount") ?: 0L).let {
                        if (it > 0) it.toString() else ""
                    }
                    _isNegotiationEnabled.value = doc.getBoolean("isNegotiable") ?: false
                    _existingImageUrls.value = (doc.get("imageUrls") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()
                    
                    // Load specifications
                    @Suppress("UNCHECKED_CAST")
                    _specifications.value = (doc.get("specifications") as? Map<String, String>) ?: emptyMap()
                }
            } catch (e: Exception) {
                Log.e("AddProductViewModel", "Failed to load product for editing", e)
            }
        }
    }

    // ✅ ADD THIS FUNCTION for updating existing product
    fun updateProduct(
        context: Context,
        title: String,
        description: String,
        category: String,
        price: Double,
        stock: Int,
        weightKg: Double,
        coSellerStoreId: String,
        minimumPrice: Double,
        autoAcceptDiscount: Int,
        sellerId: String,
        sellerName: String
    ) {
        viewModelScope.launch {
            try {
                _productState.value = ProductState.Loading
                val productId = _editingProductId.value ?: return@launch

                val autoAcceptPrice = if (_isNegotiationEnabled.value && autoAcceptDiscount > 0) {
                    price * (1 - autoAcceptDiscount / 100.0)
                } else 0.0

                val updates = mapOf(
                    "title" to title,
                    "description" to description,
                    "category" to category,
                    "price" to price,
                    "stock" to stock,
                    "weightKg" to weightKg,
                    "coSellerStoreId" to coSellerStoreId,
                    "isNegotiable" to _isNegotiationEnabled.value,
                    "minimumPrice" to if (_isNegotiationEnabled.value) minimumPrice else 0.0,
                    "autoAcceptPrice" to autoAcceptPrice,
                    "autoAcceptDiscount" to autoAcceptDiscount,
                    "specifications" to _specifications.value,
                    "updatedAt" to System.currentTimeMillis()
                )

                val result = if (_selectedImages.value.isNotEmpty()) {
                    // Upload new images + update
                    productRepository.updateProductWithImages(
                        context = context,
                        productId = productId,
                        updates = updates,
                        newImageUris = _selectedImages.value,
                        existingImageUrls = _existingImageUrls.value
                    )
                } else {
                    // Just update fields, keep existing images
                    productRepository.updateProductFields(productId, updates)
                }

                if (result.isSuccess) {
                    _productState.value = ProductState.Success("Product updated successfully!", productId)
                } else {
                    _productState.value = ProductState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to update product"
                    )
                }
            } catch (e: Exception) {
                _productState.value = ProductState.Error(e.message ?: "Failed to update product")
            }
        }
    }

    fun resetState() {
        _productState.value = ProductState.Idle
    }

    fun clearForm() {
        _selectedImages.value = emptyList()
        _isNegotiationEnabled.value = false
        _productState.value = ProductState.Idle
        _specifications.value = emptyMap()
        // ✅ ADD THESE:
        _editingProductId.value = null
        _existingImageUrls.value = emptyList()
        titleState.value = ""
        descriptionState.value = ""
        priceState.value = ""
        stockState.value = ""
        categoryState.value = ""
        weightKgState.value = ""
        minimumPriceState.value = ""
        autoAcceptDiscountState.value = ""
        selectedStoreIdState.value = ""
    }
}

