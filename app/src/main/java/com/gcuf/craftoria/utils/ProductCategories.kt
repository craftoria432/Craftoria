package com.gcuf.craftoria.utils

/**
 * Standardized Product Categories
 * 
 * IMPORTANT: These categories MUST match exactly with the web admin dashboard
 * to ensure proper data synchronization and filtering across platforms.
 * 
 * DO NOT modify these values without updating:
 * - Web Admin: src/constants/categories.js
 * - All mobile screens using categories
 * - Firestore database (migration required)
 */
object ProductCategories {
    const val POTTERY_CERAMICS = "Pottery & Ceramics"
    const val TEXTILES_EMBROIDERY = "Textiles & Embroidery"
    const val JEWELRY_ACCESSORIES = "Jewelry & Accessories"
    const val WOODWORK = "Woodwork"
    const val PAINTINGS_ART = "Paintings & Art"
    const val HANDICRAFTS = "Handicrafts"
    const val HOME_DECOR = "Home Décor"
    
    /**
     * Complete list of all product categories
     * Use this for dropdowns, filters, and validation
     */
    val ALL = listOf(
        POTTERY_CERAMICS,
        TEXTILES_EMBROIDERY,
        JEWELRY_ACCESSORIES,
        WOODWORK,
        PAINTINGS_ART,
        HANDICRAFTS,
        HOME_DECOR
    )
    
    /**
     * Validate if a category string is valid
     */
    fun isValid(category: String): Boolean {
        return ALL.contains(category)
    }
    
    /**
     * Get display name for a category (same as the constant for now)
     */
    fun getDisplayName(category: String): String {
        return if (isValid(category)) category else "Unknown Category"
    }
}
