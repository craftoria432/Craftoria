/**
 * Standardized Product Categories
 * 
 * IMPORTANT: These categories MUST match exactly with the mobile app
 * to ensure proper data synchronization and filtering across platforms.
 * 
 * DO NOT modify these values without updating:
 * - Mobile App: app/src/main/java/com/gcuf/craftoria/utils/ProductCategories.kt
 * - All web admin pages using categories
 * - Firestore database (migration required)
 */

export const PRODUCT_CATEGORIES = [
  'Pottery & Ceramics',
  'Textiles & Embroidery',
  'Jewelry & Accessories',
  'Woodwork',
  'Paintings & Art',
  'Handicrafts',
  'Home Décor',
];

/**
 * Validate if a category string is valid
 */
export const isValidCategory = (category) => {
  return PRODUCT_CATEGORIES.includes(category);
};

/**
 * Get display name for a category (same as the constant for now)
 */
export const getCategoryDisplayName = (category) => {
  return isValidCategory(category) ? category : 'Unknown Category';
};

/**
 * Category filter options for buyer screens (includes "All Products")
 */
export const CATEGORY_FILTER_OPTIONS = ['All Products', ...PRODUCT_CATEGORIES];
