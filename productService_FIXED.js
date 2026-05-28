// src/services/productService.js
import {
  collection,
  doc,
  getDocs,
  getDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  limit,
  startAfter,
  serverTimestamp  // ✅ Add this
} from 'firebase/firestore';
import { db } from './firebase';

const PRODUCTS_COLLECTION = 'products';

// Get all products with pagination
export const getAllProducts = async (lastDoc = null, pageSize = 10) => {
  try {
    const productsRef = collection(db, PRODUCTS_COLLECTION);
    let q = query(
      productsRef,
      orderBy('created_at', 'desc'),
      limit(pageSize)
    );

    if (lastDoc) {
      q = query(q, startAfter(lastDoc));
    }

    const snapshot = await getDocs(q);
    const products = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));

    const lastVisible = snapshot.docs[snapshot.docs.length - 1];

    return {
      success: true,
      data: products,
      lastDoc: lastVisible,
      hasMore: snapshot.docs.length === pageSize
    };
  } catch (error) {
    console.error('Error fetching products:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Get product by ID
export const getProductById = async (productId) => {
  try {
    const productDoc = await getDoc(doc(db, PRODUCTS_COLLECTION, productId));

    if (!productDoc.exists()) {
      throw new Error('Product not found');
    }

    return {
      success: true,
      data: {
        id: productDoc.id,
        ...productDoc.data()
      }
    };
  } catch (error) {
    console.error('Error fetching product:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Get products by seller
export const getProductsBySeller = async (sellerId) => {
  try {
    const q = query(
      collection(db, PRODUCTS_COLLECTION),
      where('seller_id', '==', sellerId),
      orderBy('created_at', 'desc')
    );

    const snapshot = await getDocs(q);
    const products = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));

    return {
      success: true,
      data: products
    };
  } catch (error) {
    console.error('Error fetching seller products:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Update product
export const updateProduct = async (productId, updates) => {
  try {
    const productRef = doc(db, PRODUCTS_COLLECTION, productId);
    await updateDoc(productRef, {
      ...updates,
      updated_at: serverTimestamp()  // ✅ Use serverTimestamp()
    });

    return {
      success: true,
      message: 'Product updated successfully'
    };
  } catch (error) {
    console.error('Error updating product:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Delete product
export const deleteProduct = async (productId) => {
  try {
    await deleteDoc(doc(db, PRODUCTS_COLLECTION, productId));

    return {
      success: true,
      message: 'Product deleted successfully'
    };
  } catch (error) {
    console.error('Error deleting product:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Toggle product status (active/inactive)
export const toggleProductStatus = async (productId, currentStatus) => {
  try {
    const productRef = doc(db, PRODUCTS_COLLECTION, productId);
    
    // ✅ Use 'status' field to match mobile app
    const newStatus = currentStatus === 'active' ? 'inactive' : 'active';
    
    await updateDoc(productRef, {
      status: newStatus,  // ✅ Changed from is_active
      updated_at: serverTimestamp()  // ✅ Use serverTimestamp()
    });

    return {
      success: true,
      message: `Product ${newStatus === 'active' ? 'activated' : 'deactivated'} successfully`
    };
  } catch (error) {
    console.error('Error toggling product status:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Search products
export const searchProducts = async (searchTerm) => {
  try {
    const productsRef = collection(db, PRODUCTS_COLLECTION);
    const snapshot = await getDocs(productsRef);

    const products = snapshot.docs
      .map(doc => ({
        id: doc.id,
        ...doc.data()
      }))
      .filter(product =>
        product.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        product.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        product.seller_name?.toLowerCase().includes(searchTerm.toLowerCase())  // ✅ seller_name
      );

    return {
      success: true,
      data: products
    };
  } catch (error) {
    console.error('Error searching products:', error);
    return {
      success: false,
      error: error.message
    };
  }
};

// Get product statistics
export const getProductStats = async () => {
  try {
    const productsRef = collection(db, PRODUCTS_COLLECTION);
    const snapshot = await getDocs(productsRef);
    const products = snapshot.docs.map(doc => doc.data());

    const stats = {
      total: products.length,
      // ✅ Use 'status' field instead of 'is_active'
      active: products.filter(p => p.status === 'active').length,
      inactive: products.filter(p => p.status === 'inactive').length,
      flagged: products.filter(p => p.status === 'flagged').length,
      outOfStock: products.filter(p => p.stock === 0).length,
      lowStock: products.filter(p => p.stock > 0 && p.stock < 10).length,
      totalValue: products.reduce((sum, p) => sum + (p.price * p.stock), 0),
      byCategory: products.reduce((acc, p) => {
        acc[p.category] = (acc[p.category] || 0) + 1;
        return acc;
      }, {})
    };

    return {
      success: true,
      data: stats
    };
  } catch (error) {
    console.error('Error fetching product stats:', error);
    return {
      success: false,
      error: error.message
    };
  }
};
