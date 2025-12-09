package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.toMap
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
     * Create new product
     */
    suspend fun createProduct(product: Product): Result<String> {
        return try {
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
     * Update product
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
            Result.success(product)
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
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to get products", error)
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(Product::class.java)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get products by seller
     */
    fun getProductsBySeller(sellerId: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("seller_id", sellerId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(Product::class.java)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Search products
     */
    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val snapshot = productsCollection
                .whereEqualTo("is_active", true)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .await()

            val products = snapshot.documents.mapNotNull {
                it.toObject(Product::class.java)
            }.filter { product ->
                product.title.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true) ||
                        product.tags.any { it.contains(query, ignoreCase = true) }
            }

            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search products", e)
            Result.failure(e)
        }
    }

    /**
     * Get products by category
     */
    fun getProductsByCategory(category: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("category", category)
            .whereEqualTo("is_active", true)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull {
                    it.toObject(Product::class.java)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }
}
