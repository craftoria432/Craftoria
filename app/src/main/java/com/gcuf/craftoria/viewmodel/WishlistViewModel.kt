package com.gcuf.craftoria.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.repository.WishlistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WishlistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WishlistRepository()

    // Currently signed-in user ID — set this after login
    private var userId: String = ""

    // Full wishlist products (for WishlistScreen)
    private val _wishlistProducts = MutableStateFlow<List<Product>>(emptyList())
    val wishlistProducts: StateFlow<List<Product>> = _wishlistProducts.asStateFlow()

    // Set of wishlisted product IDs (for heart button state across cards)
    private val _wishlistIds = MutableStateFlow<Set<String>>(emptySet())
    val wishlistIds: StateFlow<Set<String>> = _wishlistIds.asStateFlow()

    // Loading & error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val wishlistCount: StateFlow<Int> = _wishlistIds
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Call this after user logs in to start listening to their wishlist
     */
    fun initForUser(uid: String) {
        if (uid.isBlank() || uid == userId) return
        userId = uid

        // Listen to IDs (lightweight — used by product cards for heart state)
        viewModelScope.launch {
            repository.getWishlistIds(userId).collect { ids ->
                _wishlistIds.value = ids
            }
        }

        // Listen to full products (used by WishlistScreen)
        viewModelScope.launch {
            repository.getWishlistProducts(userId).collect { products ->
                _wishlistProducts.value = products
            }
        }
    }

    /**
     * Toggle wishlist: adds if not present, removes if already there
     */
    fun toggleWishlist(product: Product) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            val isCurrentlyWishlisted = _wishlistIds.value.contains(product.id)
            if (isCurrentlyWishlisted) {
                repository.removeFromWishlist(userId, product.id)
            } else {
                repository.addToWishlist(userId, product)
            }
        }
    }

    /**
     * Check if a specific product is wishlisted
     */
    fun isWishlisted(productId: String): Boolean {
        return _wishlistIds.value.contains(productId)
    }

    /**
     * Remove a specific product (called from WishlistScreen swipe/delete)
     */
    fun removeFromWishlist(productId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.removeFromWishlist(userId, productId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}