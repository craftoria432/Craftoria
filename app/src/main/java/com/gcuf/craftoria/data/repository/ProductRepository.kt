package com.gcuf.craftoria.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.CoSellerStore
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.toMap
import com.gcuf.craftoria.utils.CloudinaryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val productsCollection = firestore.collection("products")

    companion object {
        private const val TAG = "ProductRepository"
    }

    /**
     * Create new product with image upload
     */
    suspend fun createProduct(
        context: Context,
        product: Product,
        imageUris: List<Uri>
    ): Result<String> {
        return try {
            // Hard block: never create a product without a valid seller_id
            if (product.sellerId.isBlank()) {
                return Result.failure(Exception("Cannot create product: seller_id is missing"))
            }

            Log.d(TAG, "Creating product with ${imageUris.size} images")

            // Upload images to Cloudinary
            val imageUrls = if (imageUris.isNotEmpty()) {
                CloudinaryManager.uploadMultipleImages(
                    context = context,
                    imageUris = imageUris,
                    folder = "craftoria/products"
                )
            } else {
                emptyList()
            }

            Log.d(TAG, "Uploaded ${imageUrls.size} images to Cloudinary")

            // Create product document reference
            val docRef = productsCollection.document()

            // Create product with image URLs and ID
            val productWithImages = product.copy(
                id = docRef.id,
                imageUrls = imageUrls,
                createdAt = null,  // Will be set by toMap() as serverTimestamp
                updatedAt = null   // Will be set by toMap() as serverTimestamp
            )

            // Save to Firestore
            docRef.set(productWithImages.toMap()).await()

            // ✅ Update store product count if linked to co-seller store
            if (product.coSellerStoreId.isNotEmpty()) {
                try {
                    val storeRef = firestore.collection("co_seller_stores").document(product.coSellerStoreId)
                    val storeDoc = storeRef.get().await()
                    if (storeDoc.exists()) {
                        val currentCount = storeDoc.getLong("product_count")?.toInt() ?: 0
                        storeRef.update("product_count", currentCount + 1).await()
                        Log.d(TAG, "Updated store product count: ${currentCount + 1}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update store product count", e)
                    // Don't fail the whole operation if count update fails
                }
            }

            Log.d(TAG, "Product created successfully: ${docRef.id}")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create product", e)
            Result.failure(e)
        }
    }

    /**
     * Create product without image upload (for simple creation)
     */
    suspend fun createProduct(product: Product): Result<String> {
        return try {
            if (product.sellerId.isBlank()) {
                return Result.failure(Exception("Cannot create product: seller_id is missing"))
            }

            val docRef = productsCollection.document()
            val productWithId = product.copy(id = docRef.id)
            docRef.set(productWithId.toMap()).await()
            Log.d(TAG, "Product created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create product", e)
            Result.failure(e)
        }
    }

    /**
     * Save product as draft
     */
    suspend fun saveDraft(
        context: Context,
        product: Product,
        imageUris: List<Uri>
    ): Result<String> {
        return try {
            if (product.sellerId.isBlank()) {
                return Result.failure(Exception("Cannot save draft: seller_id is missing"))
            }

            Log.d(TAG, "Saving draft with ${imageUris.size} images")

            // Upload images if any
            val imageUrls = if (imageUris.isNotEmpty()) {
                CloudinaryManager.uploadMultipleImages(
                    context = context,
                    imageUris = imageUris,
                    folder = "craftoria/products/drafts"
                )
            } else {
                emptyList()
            }

            // Create document reference
            val docRef = productsCollection.document()

            // Save as draft
            val draft = product.copy(
                id = docRef.id,
                imageUrls = imageUrls,
                isDraft = true,
                isActive = false,
                createdAt = null,  // Will be set by toMap() as serverTimestamp
                updatedAt = null   // Will be set by toMap() as serverTimestamp
            )

            docRef.set(draft.toMap()).await()

            Log.d(TAG, "Draft saved successfully: ${docRef.id}")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save draft", e)
            Result.failure(e)
        }
    }

    /**
     * Update product with optional new images
     */
    suspend fun updateProduct(
        context: Context,
        productId: String,
        product: Product,
        newImageUris: List<Uri>
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating product $productId with ${newImageUris.size} new images")

            // Upload new images if any
            val newImageUrls = if (newImageUris.isNotEmpty()) {
                CloudinaryManager.uploadMultipleImages(
                    context = context,
                    imageUris = newImageUris,
                    folder = "craftoria/products"
                )
            } else {
                emptyList()
            }

            // Merge with existing images
            val allImages = product.imageUrls + newImageUrls

            // Update product
            val updatedProduct = product.copy(
                imageUrls = allImages,
                updatedAt = null  // Will be set by toMap() as serverTimestamp
            )

            productsCollection.document(productId).set(updatedProduct.toMap()).await()

            Log.d(TAG, "Product updated successfully: $productId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product", e)
            Result.failure(e)
        }
    }

    /**
     * Update product (simple version without image upload)
     */
    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            productsCollection.document(product.id).set(product.toMap()).await()
            Log.d(TAG, "Product updated: ${product.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product", e)
            Result.failure(e)
        }
    }

    /**
     * Delete product
     */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            productsCollection.document(productId).delete().await()
            Log.d(TAG, "Product deleted: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete product", e)
            Result.failure(e)
        }
    }

    /**
     * Get product by ID
     */
    suspend fun getProduct(productId: String): Result<Product> {
        return try {
            val doc = productsCollection.document(productId).get().await()
            val product = doc.toObject(Product::class.java)
                ?: throw Exception("Product not found")
            Result.success(product.copy(id = doc.id))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get product", e)
            Result.failure(e)
        }
    }

    /**
     * Get all active products (real-time)
     */
    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("is_active", true)
            .whereEqualTo("is_draft", false)
            .whereEqualTo("approval_status", "approved")  // ✅ NEW: Only show approved products
            .whereEqualTo("is_removed", false)  // ✅ ADD THIS
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to get products", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing product: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "✅ Loaded ${products.size} products")
                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get products by seller (including drafts)
     */
    fun getProductsBySeller(sellerId: String): Flow<List<Product>> = callbackFlow {
        if (sellerId.isBlank()) {
            close(Exception("sellerId cannot be blank"))
            return@callbackFlow
        }

        val listener = productsCollection
            .whereEqualTo("seller_id", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing product: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Loaded ${products.size} products for seller: $sellerId")
                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get draft products by seller
     */
    fun getDraftsBySeller(sellerId: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("seller_id", sellerId)
            .whereEqualTo("is_draft", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val drafts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing draft: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Loaded ${drafts.size} drafts for seller: $sellerId")
                trySend(drafts)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get products by category
     */
    fun getProductsByCategory(category: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("category", category)
            .whereEqualTo("is_active", true)
            .whereEqualTo("is_draft", false)
            .whereEqualTo("is_removed", false)  // ✅ ADD THIS
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing product: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.d(TAG, "Loaded ${products.size} products for category: $category")
                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get user's co-seller stores (both owned and member of)
     */
    suspend fun getUserStores(userId: String): Result<List<CoSellerStore>> {
        return try {
            // Get stores where user is owner
            val ownerStores = firestore.collection("co_seller_stores")
                .whereEqualTo("owner_id", userId)
                .get()
                .await()

            // Get stores where user is a member
            val memberStores = firestore.collection("co_seller_stores")
                .whereArrayContains("member_ids", userId)
                .get()
                .await()

            val allStores = mutableListOf<CoSellerStore>()

            // Add owner stores
            ownerStores.documents.forEach { doc ->
                try {
                    doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)?.let {
                        allStores.add(it)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing owner store: ${doc.id}", e)
                }
            }

            // Add member stores (avoid duplicates)
            memberStores.documents.forEach { doc ->
                try {
                    val store = doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)
                    if (store != null && allStores.none { it.id == store.id }) {
                        allStores.add(store)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing member store: ${doc.id}", e)
                }
            }

            Log.d(TAG, "Fetched ${allStores.size} stores for user: $userId (${ownerStores.size()} owned, ${memberStores.size()} member)")
            Result.success(allStores.sortedByDescending { 
                when (val timestamp = it.createdAt) {
                    is com.google.firebase.Timestamp -> timestamp.toDate().time
                    is Long -> timestamp
                    else -> 0L
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stores", e)
            Result.failure(e)
        }
    }

    /**
     * Publish a draft (convert draft to active product)
     */
    suspend fun publishDraft(productId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "is_draft" to false,
                "is_active" to true,
                "updated_at" to System.currentTimeMillis()
            )

            productsCollection.document(productId).update(updates).await()

            Log.d(TAG, "Draft published successfully: $productId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish draft", e)
            Result.failure(e)
        }
    }

    // ========== MANAGE PRODUCTS METHODS ==========

    /**
     * Get seller products with filtering and sorting (No composite index required)
     */
    suspend fun getSellerProducts(
        sellerId: String,
        filter: ProductFilter = ProductFilter.ALL,
        sortBy: ProductSort = ProductSort.NEWEST
    ): Result<List<Product>> {
        return try {
            if (sellerId.isBlank()) {
                return Result.failure(Exception("sellerId cannot be blank"))
            }

            Log.d(TAG, "Fetching products for seller: $sellerId, filter: $filter, sort: $sortBy")

            val snapshot = productsCollection
                .whereEqualTo("seller_id", sellerId)
                .get()
                .await()

            // Parse all products
            var products = snapshot.documents.mapNotNull { doc ->
                // Double-check: skip any doc whose seller_id doesn't exactly match
                val docSellerId = doc.getString("seller_id")
                if (docSellerId.isNullOrBlank() || docSellerId != sellerId) {
                    Log.w(TAG, "Skipping orphan product: ${doc.id}")
                    return@mapNotNull null
                }

                val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
                // ✅ FIX: If approval_status is missing (old products), set to "approved"
                if (product != null && !doc.contains("approval_status")) {
                    product.copy(approvalStatus = "approved")
                } else {
                    product
                }
            }

            // Apply filter in memory
            products = when (filter) {
                ProductFilter.ACTIVE -> {
                    products.filter { it.isActive }
                }
                ProductFilter.INACTIVE -> {
                    products.filter { !it.isActive }
                }
                ProductFilter.OUT_OF_STOCK -> {
                    products.filter { it.stock == 0 && !it.isDraft }
                }
                ProductFilter.DRAFTS -> {
                    products.filter { it.isDraft }
                }
                ProductFilter.PENDING -> {
                    // ✅ NEW: Show products awaiting approval
                    products.filter { it.approvalStatus == "pending" && !it.isDraft }
                }
                ProductFilter.ALL -> {
                    products.filter { !it.isDraft }
                }
            }

            // Apply sort in memory
            products = when (sortBy) {
                ProductSort.NEWEST -> products.sortedByDescending { 
                    when (val timestamp = it.createdAt) {
                        is com.google.firebase.Timestamp -> timestamp.toDate().time
                        is Long -> timestamp
                        else -> 0L
                    }
                }
                ProductSort.OLDEST -> products.sortedBy { 
                    when (val timestamp = it.createdAt) {
                        is com.google.firebase.Timestamp -> timestamp.toDate().time
                        is Long -> timestamp
                        else -> 0L
                    }
                }
                ProductSort.PRICE_HIGH -> products.sortedByDescending { it.price }
                ProductSort.PRICE_LOW -> products.sortedBy { it.price }
                ProductSort.NAME -> products.sortedBy { it.title }
            }

            Log.d(TAG, "Fetched and filtered ${products.size} products")
            Result.success(products)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch seller products", e)
            Result.failure(e)
        }
    }

    /**
     * Get all active products including co-seller store products (real-time)
     */
    fun getAllProductsIncludingCoSeller(): Flow<List<Product>> = callbackFlow {
        val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

        try {
            // Listen to main products
            val mainProductsListener = productsCollection
                .whereEqualTo("is_active", true)
                .whereEqualTo("is_draft", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Failed to get main products", error)
                        return@addSnapshotListener
                    }

                    val mainProducts = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Product::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing product: ${doc.id}", e)
                            null
                        }
                    } ?: emptyList()

                    Log.d(TAG, "✅ Loaded ${mainProducts.size} main products")

                    // Get co-seller products
                    getCoSellerProducts { coSellerProducts ->
                        val allProducts = mainProducts + coSellerProducts
                        Log.d(TAG, "✅ Total products (main + co-seller): ${allProducts.size}")
                        trySend(allProducts)
                    }
                }

            listeners.add(mainProductsListener)

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up product listeners", e)
            close(e)
        }

        awaitClose {
            listeners.forEach { it.remove() }
        }
    }

    /**
     * Helper: Get products from all co-seller stores
     */
    private fun getCoSellerProducts(onResult: (List<Product>) -> Unit) {
        firestore.collection("co_seller_stores")
            .whereEqualTo("is_active", true)
            .get()
            .addOnSuccessListener { storesSnapshot ->
                val allCoSellerProducts = mutableListOf<Product>()

                if (storesSnapshot.isEmpty) {
                    Log.d(TAG, "No active co-seller stores found")
                    onResult(emptyList())
                    return@addOnSuccessListener
                }

                var processedStores = 0
                val totalStores = storesSnapshot.size()

                storesSnapshot.documents.forEach { storeDoc ->
                    val storeId = storeDoc.id

                    // Get products from this co-seller store
                    firestore.collection("co_seller_stores")
                        .document(storeId)
                        .collection("products")
                        .whereEqualTo("is_active", true)
                        .whereEqualTo("is_draft", false)
                        .get()
                        .addOnSuccessListener { productsSnapshot ->
                            val storeProducts = productsSnapshot.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Product::class.java)?.copy(
                                        id = doc.id,
                                        // Mark as co-seller product for reference
                                        storeId = storeId
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing co-seller product: ${doc.id}", e)
                                    null
                                }
                            }

                            allCoSellerProducts.addAll(storeProducts)
                            processedStores++

                            Log.d(TAG, "✅ Loaded ${storeProducts.size} products from store: $storeId")

                            // When all stores are processed, return results
                            if (processedStores == totalStores) {
                                Log.d(TAG, "✅ Total co-seller products: ${allCoSellerProducts.size}")
                                onResult(allCoSellerProducts)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to load products from store: $storeId", e)
                            processedStores++

                            if (processedStores == totalStores) {
                                onResult(allCoSellerProducts)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load co-seller stores", e)
                onResult(emptyList())
            }
    }
    /**
     * Update product active status
     */
    suspend fun updateProductStatus(
        productId: String,
        isActive: Boolean
    ): Result<Unit> {
        return try {
            productsCollection.document(productId)
                .update(
                    mapOf(
                        "is_active" to isActive,
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Product status updated: $productId -> $isActive")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product status", e)
            Result.failure(e)
        }
    }

    /**
     * Update product stock quantity
     */
    suspend fun updateProductStock(
        productId: String,
        stock: Int
    ): Result<Unit> {
        return try {
            productsCollection.document(productId)
                .update(
                    mapOf(
                        "stock" to stock,
                        "updated_at" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Product stock updated: $productId -> $stock")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product stock", e)
            Result.failure(e)
        }
    }

    /**
     * Search seller products by query
     */
    suspend fun searchSellerProducts(
        sellerId: String,
        searchQuery: String
    ): Result<List<Product>> {
        return try {
            if (sellerId.isBlank()) {
                return Result.failure(Exception("sellerId cannot be blank"))
            }

            // First get all seller products
            val allProducts = getSellerProducts(sellerId, ProductFilter.ALL).getOrNull() ?: emptyList()

            // Filter by search query locally (Firestore doesn't support full-text search)
            val filtered = allProducts.filter { product ->
                product.title.contains(searchQuery, ignoreCase = true) ||
                        product.description.contains(searchQuery, ignoreCase = true) ||
                        product.category.contains(searchQuery, ignoreCase = true)
            }

            Log.d(TAG, "Search found ${filtered.size} products for query: $searchQuery")
            Result.success(filtered)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to search products", e)
            Result.failure(e)
        }
    }

     // Update product fields only (no image upload)
    suspend fun updateProductFields(
        productId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            productsCollection.document(productId)
                .update(updates)
                .await()
            Log.d(TAG, "Product fields updated: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product fields", e)
            Result.failure(e)
        }
    }


     // Update product with new images merged with existing ones

    suspend fun updateProductWithImages(
        context: Context,
        productId: String,
        updates: Map<String, Any>,
        newImageUris: List<Uri>,
        existingImageUrls: List<String>
    ): Result<Unit> {
        return try {
            val newUrls = if (newImageUris.isNotEmpty()) {
                CloudinaryManager.uploadMultipleImages(
                    context = context,
                    imageUris = newImageUris,
                    folder = "craftoria/products"
                )
            } else emptyList()

            val allImageUrls = existingImageUrls + newUrls
            val updatesWithImages = updates + mapOf("image_urls" to allImageUrls)

            productsCollection.document(productId)
                .update(updatesWithImages)
                .await()

            Log.d(TAG, "Product updated with images: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product with images", e)
            Result.failure(e)
        }
    }

    /**
     * Update product approval status and send notification to seller
     */
    suspend fun updateProductApprovalStatus(
        productId: String,
        approved: Boolean,
        reason: String = ""
    ): Result<Unit> {
        return try {
            // Get product details first
            val productDoc = productsCollection.document(productId).get().await()
            val product = productDoc.toObject(Product::class.java)?.copy(id = productDoc.id)
            val sellerId = productDoc.getString("seller_id") ?: ""
            val productName = productDoc.getString("title") ?: "Product"

            // Update approval status
            val status = if (approved) "approved" else "rejected"
            val updates = mapOf(
                "approval_status" to status,
                "approved_at" to System.currentTimeMillis(),
                "rejection_reason" to reason,
                "updated_at" to System.currentTimeMillis()
            )

            productsCollection.document(productId).update(updates).await()

            Log.d(TAG, "✅ Product approval status updated: $productId -> $status")

            // ✅ Send notification to seller
            if (product != null) {
                com.gcuf.craftoria.utils.NotificationHelper.notifyProductApprovalStatus(
                    sellerId = sellerId,
                    productId = productId,
                    productName = productName,
                    approved = approved,
                    reason = reason
                )
                Log.d(TAG, "✅ Product approval notification sent to seller: $sellerId")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product approval status", e)
            Result.failure(e)
        }
    }
}

/**
 * Product filtering options
 */
enum class ProductFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK,
    DRAFTS,
    PENDING  // ✅ NEW: For products awaiting approval
}

/**
 * Product sorting options
 */
enum class ProductSort {
    NEWEST,
    OLDEST,
    PRICE_HIGH,
    PRICE_LOW,
    NAME
}