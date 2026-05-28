# Web Dashboard Real-Time Integration - Action Plan

## 🎯 Current Status
You've completed 6 out of ~47 tasks for full real-time integration. The foundation is solid, now we need to continue building.

---

## 🚨 IMMEDIATE ACTION REQUIRED

### Task 7: User Orders Count Fix (IN-PROGRESS)

**Problem**: User Management screen shows "0 orders" and "N/A" dates

**Solution Document**: `USER_ORDERS_COUNT_FIX.md` (already created)

**What You Need to Do**:

1. **Locate your UserManagement.jsx file**
   - It should be in your web dashboard project
   - Likely in `src/pages/UserManagement.jsx` or similar

2. **Apply the fix** (copy from `USER_ORDERS_COUNT_FIX.md`):

```javascript
// Add this state at the top
const [userOrderCounts, setUserOrderCounts] = useState({});

// Add this useEffect after your users listener
useEffect(() => {
  const unsubscribe = onSnapshot(
    collection(db, 'orders'),
    (snapshot) => {
      try {
        const orderCounts = {};
        snapshot.docs.forEach(doc => {
          const order = doc.data();
          const buyerId = order.buyer_id || order.buyerId;
          if (buyerId) {
            orderCounts[buyerId] = (orderCounts[buyerId] || 0) + 1;
          }
        });
        setUserOrderCounts(orderCounts);
      } catch (error) {
        console.error('Error processing orders:', error);
      }
    }
  );
  return () => unsubscribe();
}, []);

// Update your table cell for orders
<TableCell>
  {userOrderCounts[user.id] || 0}
</TableCell>

// Update your table cell for date
<TableCell>
  {(() => {
    if (!user.created_at) return 'N/A';
    try {
      const date = convertTimestamp(user.created_at);
      if (!date || isNaN(date.getTime())) return 'N/A';
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });
    } catch (error) {
      return 'N/A';
    }
  })()}
</TableCell>
```

3. **Test it**:
   - Open User Management in web dashboard
   - Create an order in mobile app
   - Verify order count updates automatically

---

## 📋 NEXT 4 PRIORITY TASKS

### Priority 1: Create Analytics Service (CRITICAL)

**Why**: All charts and stats depend on this

**File to Create**: `src/services/analyticsService.js`

**What to Include**:

```javascript
import { collection, getDocs, query, where, orderBy, limit } from 'firebase/firestore';
import { db } from './firebase';

// 1. Sales data for chart (last 6 months)
export const getSalesData = async (months = 6) => {
  // Query orders, group by month, sum total_amount
  // Return: [{ month: 'Jan', sales: 45000 }, ...]
};

// 2. Category distribution for pie chart
export const getCategoryDistribution = async () => {
  // Query products, count per category
  // Return: [{ name: 'Handicrafts', value: 450 }, ...]
};

// 3. Top selling products
export const getTopSellingProducts = async (limit = 10) => {
  // Query orders, aggregate items, sort by quantity
  // Return: [{ name: 'Product', sales: 150 }, ...]
};

// 4. Calculate revenue
export const calculateRevenue = async (period = 'all') => {
  // Sum total_amount from delivered orders
  // Support: today, week, month, all
};

// 5. Growth metrics (month-over-month)
export const calculateGrowthMetrics = async () => {
  // Compare current vs previous month
  // Return: { users: 12.5, products: 8.3, orders: 15.2, revenue: 20.1 }
};

// 6. Recent activity feed
export const getRecentActivity = async (limit = 10) => {
  // Merge users, orders, products by created_at
  // Return unified activity array
};
```

**Time Estimate**: 2-3 hours

---

### Priority 2: Update Dashboard.jsx (HIGH)

**Why**: Main dashboard needs real-time data

**File to Update**: `src/pages/Dashboard.jsx`

**Changes Needed**:

1. **Import analytics service**:
```javascript
import { 
  getSalesData, 
  getCategoryDistribution, 
  calculateGrowthMetrics 
} from '../services/analyticsService';
```

2. **Add real-time listeners**:
```javascript
useEffect(() => {
  const unsubOrders = onSnapshot(collection(db, 'orders'), () => {
    loadDashboardData();
  });
  const unsubProducts = onSnapshot(collection(db, 'products'), () => {
    loadDashboardData();
  });
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

3. **Update loadDashboardData()**:
```javascript
const loadDashboardData = async () => {
  try {
    setLoading(true);
    const [salesData, categoryData, growthMetrics] = await Promise.all([
      getSalesData(6),
      getCategoryDistribution(),
      calculateGrowthMetrics()
    ]);
    
    // Update state with real data
    setSalesData(salesData);
    setCategoryData(categoryData);
    setStats(prevStats => ({
      ...prevStats,
      users: { ...prevStats.users, change: growthMetrics.users },
      products: { ...prevStats.products, change: growthMetrics.products },
      orders: { ...prevStats.orders, change: growthMetrics.orders }
    }));
  } catch (error) {
    console.error('Error loading dashboard:', error);
  } finally {
    setLoading(false);
  }
};
```

**Time Estimate**: 1-2 hours

---

### Priority 3: Update Chart Components (HIGH)

**Files to Update**:
- `src/components/dashboard/SalesChart.jsx`
- `src/components/dashboard/CategoryChart.jsx`

**SalesChart.jsx Changes**:
```javascript
import { useState, useEffect } from 'react';
import { getSalesData } from '../../services/analyticsService';

const SalesChart = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      const salesData = await getSalesData(6);
      setData(salesData);
      setLoading(false);
    };
    loadData();
  }, []);

  if (loading) return <CircularProgress />;

  return (
    <ResponsiveContainer width="100%" height={250}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="month" />
        <YAxis />
        <Tooltip />
        <Line type="monotone" dataKey="sales" stroke="#E91E63" strokeWidth={2} />
      </LineChart>
    </ResponsiveContainer>
  );
};
```

**CategoryChart.jsx Changes**:
```javascript
import { useState, useEffect } from 'react';
import { getCategoryDistribution } from '../../services/analyticsService';

const CategoryChart = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      const categoryData = await getCategoryDistribution();
      setData(categoryData);
      setLoading(false);
    };
    loadData();
  }, []);

  if (loading) return <CircularProgress />;

  return (
    <ResponsiveContainer width="100%" height={250}>
      <PieChart>
        <Pie data={data} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} fill="#8884d8" label>
          {data.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip />
      </PieChart>
    </ResponsiveContainer>
  );
};
```

**Time Estimate**: 1 hour

---

### Priority 4: Create Top Selling Products Chart (MEDIUM)

**File to Create**: `src/components/dashboard/TopSellingChart.jsx`

**Complete Implementation**:
```javascript
import React, { useState, useEffect } from 'react';
import { Card, CardContent, Typography, CircularProgress } from '@mui/material';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { getTopSellingProducts } from '../../services/analyticsService';

const TopSellingChart = () => {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      try {
        const products = await getTopSellingProducts(10);
        setData(products);
      } catch (error) {
        console.error('Error loading top products:', error);
      } finally {
        setLoading(false);
      }
    };
    loadData();
  }, []);

  if (loading) {
    return (
      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0' }}>
        <CardContent sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0' }}>
      <CardContent sx={{ p: 2.5 }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
          Top Selling Products
        </Typography>
        <ResponsiveContainer width="100%" height={250}>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" angle={-45} textAnchor="end" height={100} />
            <YAxis />
            <Tooltip />
            <Bar dataKey="sales" fill="#E91E63" />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
};

export default TopSellingChart;
```

**Then add to Dashboard.jsx**:
```javascript
import TopSellingChart from '../components/dashboard/TopSellingChart';

// In your dashboard grid
<Grid item xs={12} md={6}>
  <TopSellingChart />
</Grid>
```

**Time Estimate**: 30 minutes

---

## 📅 SUGGESTED TIMELINE

### Week 1
- **Day 1**: Complete Task 7 (User Orders Count Fix)
- **Day 2-3**: Create analyticsService.js with all 6 functions
- **Day 4**: Update Dashboard.jsx with real-time listeners
- **Day 5**: Update SalesChart and CategoryChart

### Week 2
- **Day 1**: Create TopSellingChart
- **Day 2**: Create useRealtimeData hook
- **Day 3**: Create firebaseModels.js type definitions
- **Day 4**: Update service layer (userService, orderService)
- **Day 5**: Testing and bug fixes

### Week 3
- **Day 1-2**: Implement caching (cacheManager.js)
- **Day 3**: Create error boundaries and loading skeletons
- **Day 4-5**: Performance optimization and final testing

---

## 🧪 TESTING CHECKLIST

After each priority task:

### Test Dashboard Real-Time Updates
1. Open web dashboard
2. Create order in mobile app
3. Verify dashboard stats update automatically
4. Check charts update with new data

### Test User Management
1. Open User Management
2. Create user in mobile app
3. Verify user appears in list
4. Check order count updates when order is placed

### Test Product Management
1. Open Product Management
2. Add product in mobile app
3. Verify product appears in list
4. Check seller name displays correctly

### Test Charts
1. Verify SalesChart shows real monthly data
2. Verify CategoryChart shows real product distribution
3. Verify TopSellingChart shows actual best sellers
4. Check all charts update when data changes

---

## 🔧 TROUBLESHOOTING

### If orders count still shows 0:
1. Check Firebase Console - verify orders collection has `buyer_id` field
2. Check browser console for errors
3. Verify onSnapshot listener is running
4. Check field name: `buyer_id` vs `buyerId`

### If dates show N/A:
1. Check Firebase Console - verify users have `created_at` field
2. Check timestamp format (Firestore Timestamp vs ISO string)
3. Verify convertTimestamp() helper function exists
4. Add console.log to see actual timestamp value

### If charts don't update:
1. Verify analyticsService functions are called
2. Check data format matches chart expectations
3. Verify real-time listeners are active
4. Check browser console for errors

### If performance is slow:
1. Add indexes in Firestore for common queries
2. Implement caching (Phase 8)
3. Use pagination for large datasets
4. Optimize query filters

---

## 📞 NEED HELP?

If you encounter issues:

1. **Check existing fix documents** - most solutions are already documented
2. **Check browser console** - errors will show there
3. **Check Firebase Console** - verify data structure
4. **Review field names** - must match Kotlin models exactly

---

## 🎯 SUCCESS CRITERIA

You'll know you're done when:

- [ ] All charts show real Firebase data (no mock data)
- [ ] Dashboard updates automatically when mobile app changes data
- [ ] User orders count shows correct numbers
- [ ] All dates display properly
- [ ] Growth percentages calculate from real data
- [ ] No console errors
- [ ] Performance is acceptable (<2 second load time)

---

**Document Version**: 1.0  
**Created**: 2026-03-09  
**Status**: Active - Follow this plan sequentially
