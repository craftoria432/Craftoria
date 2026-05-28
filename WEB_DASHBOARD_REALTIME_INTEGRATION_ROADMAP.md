# Web Dashboard Real-Time Integration Roadmap

## Executive Summary

This roadmap outlines the complete implementation plan to transform the web admin dashboard from mock data to production-ready real-time integration with the Craftoria mobile app Firebase backend.

---

## Phase 1: Foundation & Data Models (Priority: CRITICAL)

### 1.1 Create Shared Data Type Definitions

**File**: `src/types/firebaseModels.js`

```javascript
// Mirror Kotlin data models from mobile app
export const FirebaseCollections = {
  USERS: 'users',
  PRODUCTS: 'products',
  ORDERS: 'orders',
  CHATS: 'chats',
  NOTIFICATIONS: 'notifications',
  REPORTS: 'reports',
  LEARNING_RESOURCES: 'learning_resources',
  CART: 'cart',
  WISHLIST: 'wishlist',
  CO_SELLER_STORES: 'co_seller_stores'
};

export const OrderStatus = {
  PENDING: 'pending',
  CONFIRMED: 'confirmed',
  SHIPPED: 'shipped',
  DELIVERED: 'delivered',
  CANCELLED: 'cancelled'
};

export const UserRole = {
  BUYER: 'buyer',
  SELLER: 'seller',
  ADMIN: 'admin'
};

export const ProductCategory = {
  HANDICRAFTS: 'Handicrafts',
  JEWELRY: 'Jewelry',
  CLOTHING: 'Clothing',
  HOME_DECOR: 'Home Decor',
  POTTERY: 'Pottery',
  OTHER: 'Other'
};
```

**Why**: Ensures field name consistency between web and mobile (e.g., `created_at`, `buyer_name`, `total_amount`)

---

## Phase 2: Analytics Service Layer (Priority: CRITICAL)

### 2.1 Create Analytics Service

**File**: `src/services/analyticsService.js`

**Purpose**: Centralized service for all dashboard analytics calculations

**Functions to Implement**:

```javascript
// 1. Sales Over Time (Monthly)
export const getSalesData = async (months = 6) => {
  // Query orders collection
  // Filter by date range and status='delivered'
  // Group by month
  // Calculate total_amount per month
  // Return array: [{ month: 'Jan', sales: 45000 }, ...]
}

// 2. Category Distribution
export const getCategoryDistribution = async () => {
  // Query products collection
  // Count products per category
  // Return array: [{ name: 'Handicrafts', value: 450 }, ...]
}

// 3. Top Selling Products
export const getTopSellingProducts = async (limit = 5) => {
  // Query all orders
  // Aggregate items by product_id
  // Sum quantities
  // Sort by quantity descending
  // Return top N products with sales count
}

// 4. Revenue Calculation
export const calculateRevenue = async (period = 'all') => {
  // Query orders with status='delivered'
  // Sum total_amount field
  // Support filters: today, week, month, all
}

// 5. Growth Metrics
export const calculateGrowthMetrics = async () => {
  // Compare current month vs previous month
  // Calculate percentage change for:
  //   - Users (new registrations)
  //   - Products (new listings)
  //   - Orders (completed orders)
  //   - Revenue
}

// 6. Recent Activity Feed
export const getRecentActivity = async (limit = 10) => {
  // Merge recent data from:
  //   - users (registrations)
  //   - orders (new orders)
  //   - products (new listings)
  // Sort by created_at descending
  // Return unified activity feed
}
```

**Dependencies**:
- Firebase Firestore SDK
- Date manipulation library (date-fns recommended)

---

## Phase 3: Real-Time Listeners (Priority: HIGH)

### 3.1 Create Real-Time Hook

**File**: `src/hooks/useRealtimeData.js`

```javascript
import { useEffect, useState } from 'react';
import { onSnapshot, collection, query, where } from 'firebase/firestore';
import { db } from '../services/firebase';

export const useRealtimeCollection = (collectionName, queryConstraints = []) => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const q = query(collection(db, collectionName), ...queryConstraints);
    
    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const items = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setData(items);
        setLoading(false);
      },
      (err) => {
        setError(err);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [collectionName]);

  return { data, loading, error };
};
```

### 3.2 Implement Dashboard Real-Time Updates

**File**: `src/pages/Dashboard.jsx`

**Changes Required**:

```javascript
// Replace one-time fetch with real-time listeners
useEffect(() => {
  // Listen to orders collection
  const unsubOrders = onSnapshot(collection(db, 'orders'), () => {
    loadDashboardData();
  });

  // Listen to products collection
  const unsubProducts = onSnapshot(collection(db, 'products'), () => {
    loadDashboardData();
  });

  // Listen to users collection
  const unsubUsers = onSnapshot(collection(db, 'users'), () => {
    loadDashboardData();
  });

  return () => {
    unsubOrders();
    unsubProducts();
    unsubUsers();
  };
}, []);
```

---

## Phase 4: Chart Components Integration (Priority: HIGH)

### 4.1 Update SalesChart Component

**File**: `src/components/dashboard/SalesChart.jsx`

**Current Issue**: Uses mock data
**Solution**: 

```javascript
import { useState, useEffect } from 'react';
import { getSalesData } from '../../services/analyticsService';

const SalesChart = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadSalesData = async () => {
      const salesData = await getSalesData(6); // Last 6 months
      setData(salesData);
      setLoading(false);
    };
    loadSalesData();
  }, []);

  if (loading) return <CircularProgress />;

  return (
    <ResponsiveContainer>
      <LineChart data={data}>
        {/* ... existing chart config */}
      </LineChart>
    </ResponsiveContainer>
  );
};
```

### 4.2 Update CategoryChart Component

**File**: `src/components/dashboard/CategoryChart.jsx`

**Current Issue**: Uses mock data
**Solution**:

```javascript
import { useState, useEffect } from 'react';
import { getCategoryDistribution } from '../../services/analyticsService';

const CategoryChart = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadCategoryData = async () => {
      const categoryData = await getCategoryDistribution();
      setData(categoryData);
      setLoading(false);
    };
    loadCategoryData();
  }, []);

  // ... rest of component
};
```

### 4.3 Implement Top Selling Products Chart

**File**: `src/components/dashboard/TopSellingChart.jsx` (NEW)

**Purpose**: Replace placeholder with real bar chart

```javascript
import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { getTopSellingProducts } from '../../services/analyticsService';

const TopSellingChart = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadTopProducts = async () => {
      const products = await getTopSellingProducts(10);
      setData(products);
      setLoading(false);
    };
    loadTopProducts();
  }, []);

  return (
    <ResponsiveContainer width="100%" height={250}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis />
        <Tooltip />
        <Bar dataKey="sales" fill="#E91E63" />
      </BarChart>
    </ResponsiveContainer>
  );
};

export default TopSellingChart;
```

---

## Phase 5: StatCard Real-Time Updates (Priority: MEDIUM)

### 5.1 Update Dashboard Stats Calculation

**File**: `src/pages/Dashboard.jsx`

**Current Issue**: Stats don't reflect real-time changes
**Solution**:

```javascript
const loadDashboardData = useCallback(async () => {
  try {
    setLoading(true);

    // Get real-time stats
    const [userStats, productStats, orderStats, growthMetrics] = await Promise.all([
      getUserStats(),
      getProductStats(),
      getOrderStats(),
      calculateGrowthMetrics() // NEW: Real growth calculation
    ]);

    setStats({
      users: {
        total: userStats.total || 0,
        buyers: userStats.buyers || 0,
        change: growthMetrics.users || 0, // Real percentage
      },
      sellers: {
        total: userStats.sellers || 0,
        verified: userStats.verified || 0,
        change: growthMetrics.sellers || 0, // Real percentage
      },
      products: {
        total: productStats.total || 0,
        active: productStats.active || 0,
        change: growthMetrics.products || 0, // Real percentage
      },
      orders: {
        total: orderStats.total || 0,
        pending: orderStats.pending || 0,
        delivered: orderStats.delivered || 0,
        change: growthMetrics.orders || 0, // Real percentage
      },
      revenue: orderStats.totalRevenue || 0,
    });
  } catch (error) {
    console.error('Error loading dashboard:', error);
  } finally {
    setLoading(false);
  }
}, []);
```

---

## Phase 6: Recent Activity Real-Time Feed (Priority: MEDIUM)

### 6.1 Update RecentActivity Component

**File**: `src/components/dashboard/RecentActivity.jsx`

**Current Issue**: One-time fetch, no real-time updates
**Solution**:

```javascript
useEffect(() => {
  // Real-time listener for recent activity
  const unsubscribe = onSnapshot(
    query(
      collection(db, 'orders'),
      orderBy('created_at', 'desc'),
      limit(3)
    ),
    () => {
      loadRecentActivity(); // Refresh activity feed
    }
  );

  return () => unsubscribe();
}, []);
```

---

## Phase 7: Service Layer Updates (Priority: HIGH)

### 7.1 Update User Service

**File**: `src/services/userService.js`

**Add Functions**:

```javascript
// Get user growth metrics
export const getUserGrowthMetrics = async () => {
  const now = new Date();
  const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  
  const currentMonthQuery = query(
    collection(db, 'users'),
    where('created_at', '>=', Timestamp.fromDate(lastMonth))
  );
  
  const previousMonthQuery = query(
    collection(db, 'users'),
    where('created_at', '<', Timestamp.fromDate(lastMonth))
  );
  
  const [currentSnapshot, previousSnapshot] = await Promise.all([
    getDocs(currentMonthQuery),
    getDocs(previousMonthQuery)
  ]);
  
  const currentCount = currentSnapshot.size;
  const previousCount = previousSnapshot.size;
  
  const change = previousCount > 0 
    ? ((currentCount - previousCount) / previousCount) * 100 
    : 0;
  
  return { current: currentCount, previous: previousCount, change };
};
```

### 7.2 Update Product Service

**File**: `src/services/productService.js`

**Add Functions**:

```javascript
// Get product growth metrics
export const getProductGrowthMetrics = async () => {
  // Similar to user growth metrics
  // Calculate month-over-month product additions
};

// Get products by category
export const getProductsByCategory = async () => {
  const snapshot = await getDocs(collection(db, 'products'));
  const categoryMap = {};
  
  snapshot.docs.forEach(doc => {
    const product = doc.data();
    const category = product.category || 'Other';
    categoryMap[category] = (categoryMap[category] || 0) + 1;
  });
  
  return categoryMap;
};
```

### 7.3 Update Order Service

**File**: `src/services/orderService.js`

**Add Functions**:

```javascript
// Get order growth metrics
export const getOrderGrowthMetrics = async () => {
  // Calculate month-over-month order growth
};

// Get revenue by period
export const getRevenueBySalesPeriod = async (startDate, endDate) => {
  const ordersQuery = query(
    collection(db, 'orders'),
    where('created_at', '>=', Timestamp.fromDate(startDate)),
    where('created_at', '<=', Timestamp.fromDate(endDate)),
    where('status', '==', 'delivered')
  );
  
  const snapshot = await getDocs(ordersQuery);
  let totalRevenue = 0;
  
  snapshot.docs.forEach(doc => {
    const order = doc.data();
    totalRevenue += order.total_amount || 0;
  });
  
  return totalRevenue;
};

// Get top selling products
export const getTopSellingProductsFromOrders = async (limit = 10) => {
  const ordersSnapshot = await getDocs(collection(db, 'orders'));
  const productSales = {};
  
  ordersSnapshot.docs.forEach(doc => {
    const order = doc.data();
    order.items?.forEach(item => {
      if (!productSales[item.product_id]) {
        productSales[item.product_id] = {
          id: item.product_id,
          name: item.product_name,
          quantity: 0,
          revenue: 0
        };
      }
      productSales[item.product_id].quantity += item.quantity;
      productSales[item.product_id].revenue += item.price * item.quantity;
    });
  });
  
  return Object.values(productSales)
    .sort((a, b) => b.quantity - a.quantity)
    .slice(0, limit);
};
```

---

## Phase 8: Data Caching & Performance (Priority: MEDIUM)

### 8.1 Implement Caching Strategy

**File**: `src/utils/cacheManager.js`

```javascript
class CacheManager {
  constructor() {
    this.cache = new Map();
    this.ttl = 5 * 60 * 1000; // 5 minutes
  }

  set(key, value) {
    this.cache.set(key, {
      value,
      timestamp: Date.now()
    });
  }

  get(key) {
    const item = this.cache.get(key);
    if (!item) return null;
    
    if (Date.now() - item.timestamp > this.ttl) {
      this.cache.delete(key);
      return null;
    }
    
    return item.value;
  }

  clear() {
    this.cache.clear();
  }
}

export const dashboardCache = new CacheManager();
```

### 8.2 Update Analytics Service with Caching

```javascript
export const getSalesData = async (months = 6) => {
  const cacheKey = `sales_data_${months}`;
  const cached = dashboardCache.get(cacheKey);
  
  if (cached) return cached;
  
  // Fetch from Firebase
  const data = await fetchSalesDataFromFirebase(months);
  
  dashboardCache.set(cacheKey, data);
  return data;
};
```

---

## Phase 9: Error Handling & Loading States (Priority: MEDIUM)

### 9.1 Create Error Boundary

**File**: `src/components/common/ErrorBoundary.jsx`

```javascript
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Dashboard Error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Box sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="h6" color="error">
            Something went wrong loading the dashboard
          </Typography>
          <Button onClick={() => window.location.reload()}>
            Reload Page
          </Button>
        </Box>
      );
    }

    return this.props.children;
  }
}
```

### 9.2 Add Loading Skeletons

**File**: `src/components/common/StatCardSkeleton.jsx`

```javascript
const StatCardSkeleton = () => (
  <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0' }}>
    <CardContent sx={{ p: 2.5 }}>
      <Skeleton variant="rectangular" width="100%" height={60} />
      <Skeleton variant="text" width="60%" sx={{ mt: 1 }} />
    </CardContent>
  </Card>
);
```

---

## Phase 10: Testing & Validation (Priority: HIGH)

### 10.1 Data Consistency Tests

**File**: `src/tests/dataConsistency.test.js`

```javascript
describe('Firebase Data Consistency', () => {
  test('Order fields match mobile app schema', async () => {
    const order = await getOrderById('test_order_id');
    expect(order).toHaveProperty('created_at');
    expect(order).toHaveProperty('buyer_name');
    expect(order).toHaveProperty('total_amount');
    expect(order).toHaveProperty('status');
  });

  test('Product fields match mobile app schema', async () => {
    const product = await getProductById('test_product_id');
    expect(product).toHaveProperty('title');
    expect(product).toHaveProperty('price');
    expect(product).toHaveProperty('category');
    expect(product).toHaveProperty('seller_id');
  });
});
```

### 10.2 Real-Time Sync Tests

```javascript
describe('Real-Time Synchronization', () => {
  test('Dashboard updates when new order is created', async () => {
    // Create order in Firebase
    // Wait for real-time listener to trigger
    // Verify dashboard stats updated
  });

  test('Charts update when product is added', async () => {
    // Add product in Firebase
    // Verify category chart updates
  });
});
```

---

## Phase 11: Mobile App Verification (Priority: CRITICAL)

### 11.1 Field Name Audit

**Action**: Compare web dashboard field names with mobile app Kotlin models

**Files to Check**:
- `app/src/main/java/com/gcuf/craftoria/data/model/Order.kt`
- `app/src/main/java/com/gcuf/craftoria/data/model/Product.kt`
- `app/src/main/java/com/gcuf/craftoria/data/model/User.kt`

**Verification Checklist**:
- [ ] Order model: `created_at`, `buyer_name`, `total_amount`, `status`, `items`
- [ ] Product model: `title`, `price`, `category`, `seller_id`, `seller_name`
- [ ] User model: `name`, `email`, `role`, `created_at`

### 11.2 Timestamp Handling

**Issue**: Kotlin uses Firebase Timestamp, JavaScript needs conversion

**Solution**:
```javascript
// Utility function
export const convertTimestamp = (timestamp) => {
  if (!timestamp) return null;
  if (timestamp.toDate) return timestamp.toDate(); // Firestore Timestamp
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000); // Plain object
  return new Date(timestamp); // Already a date
};
```

---

## Phase 12: Deployment Checklist (Priority: HIGH)

### 12.1 Pre-Deployment Verification

- [ ] All mock data removed from components
- [ ] Real-time listeners implemented for all collections
- [ ] Error boundaries in place
- [ ] Loading states for all async operations
- [ ] Field names match mobile app exactly
- [ ] Timestamp conversions working
- [ ] Caching implemented for performance
- [ ] Analytics calculations tested with real data
- [ ] Growth metrics calculating correctly
- [ ] Charts rendering real Firebase data

### 12.2 Performance Optimization

- [ ] Implement pagination for large datasets
- [ ] Add indexes in Firestore for common queries
- [ ] Optimize real-time listener queries with `where` clauses
- [ ] Implement debouncing for rapid updates
- [ ] Add service worker for offline support

### 12.3 Security Rules Verification

**File**: `firestore.rules`

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Admin can read all dashboard data
    match /{document=**} {
      allow read: if request.auth != null && 
                     get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
  }
}
```

---

## Implementation Timeline

### Week 1: Foundation
- Day 1-2: Create data models and type definitions
- Day 3-4: Build analytics service layer
- Day 5: Implement real-time hooks

### Week 2: Integration
- Day 1-2: Update SalesChart and CategoryChart
- Day 3: Implement TopSellingChart
- Day 4-5: Update StatCards with real metrics

### Week 3: Polish
- Day 1-2: Implement caching and performance optimization
- Day 3: Add error handling and loading states
- Day 4-5: Testing and mobile app verification

### Week 4: Deployment
- Day 1-2: Final testing and bug fixes
- Day 3: Security rules verification
- Day 4: Staging deployment
- Day 5: Production deployment

---

## File Structure Summary

```
src/
├── services/
│   ├── analyticsService.js          ← NEW (Phase 2)
│   ├── userService.js               ← UPDATE (Phase 7)
│   ├── productService.js            ← UPDATE (Phase 7)
│   └── orderService.js              ← UPDATE (Phase 7)
├── hooks/
│   └── useRealtimeData.js           ← NEW (Phase 3)
├── types/
│   └── firebaseModels.js            ← NEW (Phase 1)
├── utils/
│   ├── cacheManager.js              ← NEW (Phase 8)
│   └── formatters.js                ← UPDATE (Phase 11)
├── components/
│   ├── common/
│   │   ├── ErrorBoundary.jsx        ← NEW (Phase 9)
│   │   └── StatCardSkeleton.jsx     ← NEW (Phase 9)
│   └── dashboard/
│       ├── SalesChart.jsx           ← UPDATE (Phase 4)
│       ├── CategoryChart.jsx        ← UPDATE (Phase 4)
│       ├── TopSellingChart.jsx      ← NEW (Phase 4)
│       └── RecentActivity.jsx       ← UPDATE (Phase 6)
└── pages/
    └── Dashboard.jsx                ← UPDATE (Phase 3, 5)
```

---

## Critical Success Factors

1. **Field Name Consistency**: Ensure exact match with Kotlin models
2. **Timestamp Handling**: Proper conversion between Firebase and JavaScript
3. **Real-Time Listeners**: Implement for all critical collections
4. **Error Handling**: Graceful degradation when Firebase is unavailable
5. **Performance**: Caching and query optimization to reduce Firestore reads
6. **Testing**: Verify data flows correctly from mobile app to web dashboard

---

## Next Steps

1. Review this roadmap with your team
2. Prioritize phases based on business needs
3. Set up development environment
4. Begin Phase 1 implementation
5. Test each phase before moving to the next

---

## Support & Resources

- Firebase Documentation: https://firebase.google.com/docs/firestore
- Recharts Documentation: https://recharts.org/
- Material-UI Documentation: https://mui.com/
- React Query (optional for caching): https://tanstack.com/query/latest

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-09  
**Status**: Ready for Implementation
