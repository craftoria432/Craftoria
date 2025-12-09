package com.gcuf.craftoria.data.model
import com.google.firebase.firestore.PropertyName

data class Product(
    val id: String = "",
    @PropertyName("seller_id")
    val sellerId: String = "",
    @PropertyName("seller_name")
    val sellerName: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val images: List<String> = emptyList(),  // Cloudinary URLs
    val stock: Int = 0,
    @PropertyName("is_active")
    val isActive: Boolean = true,
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // Additional fields
    val tags: List<String> = emptyList(),
    @PropertyName("view_count")
    val viewCount: Int = 0,
    @PropertyName("sold_count")
    val soldCount: Int = 0,
)

fun Product.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "seller_id" to sellerId,
    "seller_name" to sellerName,
    "title" to title,
    "description" to description,
    "price" to price,
    "category" to category,
    "images" to images,
    "stock" to stock,
    "is_active" to isActive,
    "created_at" to createdAt,
    "tags" to tags,
    "view_count" to viewCount,
    "sold_count" to soldCount
)