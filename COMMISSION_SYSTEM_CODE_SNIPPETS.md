# Commission System — Code Snippets & Quick Reference

## Quick Integration Snippets

### 1. Add Route to Admin Router

```jsx
// src/routes/AdminRoutes.jsx or similar

import Commissions from '../pages/Commissions';

const adminRoutes = [
  // ... other routes
  {
    path: '/admin/commissions',
    element: <Commissions />,
    name: 'Commission Management',
    icon: '💰'
  }
];

export default adminRoutes;
```

---

### 2. Add Navigation Link

```jsx
// src/components/AdminSidebar.jsx or similar

import { NavLink } from 'react-router-dom';

export const AdminSidebar = () => {
  return (
    <nav className="admin-sidebar">
      {/* ... other links */}
      <NavLink 
        to="/admin/commissions" 
        className={({ isActive }) => isActive ? 'active' : ''}
      >
        <span className="icon">💰</span>
        <span>Commission Management</span>
      </NavLink>
    </nav>
  );
};
```

---

### 3. Use Commission Service

```jsx
// Example: Using commission service in a component

import { 
  getAdminEarnings, 
  getCommissionStats,
  subscribeToPendingCommissions 
} from '../services/commissionService';

// Fetch admin earnings
const earnings = await getAdminEarnings();
console.log(`Total earnings: PKR ${earnings.totalEarnings}`);

// Get stats for date range
const stats = await getCommissionStats(
  new Date('2024-01-01'),
  new Date('2024-01-31')
);
console.log(`Total commissions: PKR ${stats.totalCommissions}`);

// Subscribe to real-time updates
const unsubscribe = subscribeToPendingCommissions(
  (commissions) => {
    console.log('Pending commissions:', commissions);
  },
  (error) => {
    console.error('Error:', error);
  }
);

// Cleanup
return () => unsubscribe();
```

---

### 4. Use Commission Hook

```jsx
// Example: Using useCommissions hook

import { useAllCommissions } from '../hooks/useCommissions';

export const MyCommissionsComponent = () => {
  const {
    commissions,
    loading,
    error,
    hasMore,
    loadMore,
    refetch
  } = useAllCommissions(20);

  return (
    <div>
      {loading && <p>Loading...</p>}
      {error && <p>Error: {error}</p>}
      
      <ul>
        {commissions.map(commission => (
          <li key={commission.id}>
            {commission.seller_name}: PKR {commission.commission_amount}
          </li>
        ))}
      </ul>

      {hasMore && (
        <button onClick={loadMore} disabled={loading}>
          Load More
        </button>
      )}

      <button onClick={refetch}>Refresh</button>
    </div>
  );
};
```

---

## Firestore Setup Snippets

### 1. Firestore Rules

```javascript
// firestore.rules

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Admin commissions collection
    match /admin_commissions/{document=**} {
      allow read: if request.auth.token.admin == true;
      allow write: if request.auth.token.admin == true;
    }
    
    // Admin earnings collection
    match /admin_earnings/{document=**} {
      allow read: if request.auth.token.admin == true;
      allow write: if request.auth.token.admin == true;
    }
  }
}
```

---

### 2. Firestore Indexes

```json
// firestore.indexes.json

{
  "indexes": [
    {
      "collectionGroup": "admin_commissions",
      "queryScope": "Collection",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "created_at", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "admin_commissions",
      "queryScope": "Collection",
      "fields": [
        { "fieldPath": "created_at", "order": "DESCENDING" }
      ]
    }
  ]
}
```

---

### 3. Sample Commission Document

```javascript
// admin_commissions collection document

{
  id: "commission-123",
  seller_id: "seller-456",
  seller_name: "John's Store",
  order_id: "order-789",
  commission_amount: 500,  // ✅ Use this field name
  status: "pending",       // or "paid"
  created_at: Timestamp.fromDate(new Date()),
  paid_at: null,
  updated_at: Timestamp.fromDate(new Date())
}
```

---

### 4. Sample Admin Earnings Document

```javascript
// admin_earnings collection document

{
  id: "admin_earnings",
  total_earnings: 50000,
  last_updated: Timestamp.fromDate(new Date()),
  currency: "PKR"
}
```

---

## Common Patterns

### Pattern 1: Fetch and Display Stats

```jsx
import { useState, useEffect } from 'react';
import { getCommissionStats } from '../services/commissionService';

export const CommissionStats = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const data = await getCommissionStats(
          new Date(new Date().setDate(new Date().getDate() - 30)),
          new Date()
        );
        setStats(data);
      } catch (error) {
        console.error('Error:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  if (loading) return <p>Loading...</p>;
  if (!stats) return <p>No data</p>;

  return (
    <div>
      <p>Total: PKR {stats.totalCommissions}</p>
      <p>Paid: PKR {stats.paidAmount}</p>
      <p>Pending: PKR {stats.pendingAmount}</p>
      <p>Count: {stats.count}</p>
    </div>
  );
};
```

---

### Pattern 2: Real-Time Pending Commissions

```jsx
import { useState, useEffect } from 'react';
import { subscribeToPendingCommissions } from '../services/commissionService';

export const PendingCommissions = () => {
  const [commissions, setCommissions] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    const unsubscribe = subscribeToPendingCommissions(
      (data) => {
        setCommissions(data);
        setError(null);
      },
      (err) => {
        setError(err.message);
      }
    );

    return () => unsubscribe();
  }, []);

  if (error) return <p>Error: {error}</p>;

  return (
    <div>
      <h2>Pending Commissions ({commissions.length})</h2>
      <ul>
        {commissions.map(commission => (
          <li key={commission.id}>
            <strong>{commission.seller_name}</strong>
            <br />
            PKR {commission.commission_amount}
            <br />
            <small>{new Date(commission.created_at).toLocaleDateString()}</small>
          </li>
        ))}
      </ul>
    </div>
  );
};
```

---

### Pattern 3: Paginated Commission List

```jsx
import { useAllCommissions } from '../hooks/useCommissions';

export const CommissionList = () => {
  const {
    commissions,
    loading,
    error,
    hasMore,
    loadMore
  } = useAllCommissions(20);

  return (
    <div>
      {error && <p className="error">{error}</p>}
      
      <div className="commission-list">
        {commissions.map(commission => (
          <div key={commission.id} className="commission-item">
            <div>
              <strong>{commission.seller_name}</strong>
              <p>PKR {commission.commission_amount}</p>
              <small>{new Date(commission.created_at).toLocaleDateString()}</small>
            </div>
            <span className={`status ${commission.status}`}>
              {commission.status}
            </span>
          </div>
        ))}
      </div>

      {hasMore && (
        <button 
          onClick={loadMore} 
          disabled={loading}
          className="load-more-btn"
        >
          {loading ? 'Loading...' : 'Load More'}
        </button>
      )}
    </div>
  );
};
```

---

### Pattern 4: Date Range Filter

```jsx
import { useState } from 'react';
import { getCommissionStats } from '../services/commissionService';

export const DateRangeFilter = () => {
  const [dateRange, setDateRange] = useState({
    startDate: new Date(new Date().setDate(new Date().getDate() - 30)),
    endDate: new Date()
  });
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleDateChange = (field, value) => {
    setDateRange(prev => ({
      ...prev,
      [field]: new Date(value)
    }));
  };

  const handleFilter = async () => {
    try {
      setLoading(true);
      const data = await getCommissionStats(
        dateRange.startDate,
        dateRange.endDate
      );
      setStats(data);
    } catch (error) {
      console.error('Error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="date-inputs">
        <input
          type="date"
          value={dateRange.startDate.toISOString().split('T')[0]}
          onChange={(e) => handleDateChange('startDate', e.target.value)}
        />
        <input
          type="date"
          value={dateRange.endDate.toISOString().split('T')[0]}
          onChange={(e) => handleDateChange('endDate', e.target.value)}
        />
        <button onClick={handleFilter} disabled={loading}>
          {loading ? 'Loading...' : 'Filter'}
        </button>
      </div>

      {stats && (
        <div className="stats">
          <p>Total: PKR {stats.totalCommissions}</p>
          <p>Paid: PKR {stats.paidAmount}</p>
          <p>Pending: PKR {stats.pendingAmount}</p>
        </div>
      )}
    </div>
  );
};
```

---

## Error Handling Patterns

### Pattern 1: Try-Catch with Toast

```jsx
import { toast } from 'react-toastify';

const handleRefresh = async () => {
  try {
    const data = await getCommissionStats(startDate, endDate);
    setStats(data);
    toast.success('Data refreshed successfully');
  } catch (error) {
    console.error('Error:', error);
    toast.error('Failed to refresh data');
  }
};
```

---

### Pattern 2: Error Boundary

```jsx
import { Component } from 'react';

export class CommissionErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Commission error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-container">
          <h2>Something went wrong</h2>
          <p>{this.state.error?.message}</p>
          <button onClick={() => window.location.reload()}>
            Reload Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
```

---

## Testing Snippets

### Test 1: Commission Stats

```javascript
// __tests__/commissionService.test.js

import { getCommissionStats } from '../services/commissionService';

describe('getCommissionStats', () => {
  it('should return stats with correct amounts', async () => {
    const stats = await getCommissionStats(
      new Date('2024-01-01'),
      new Date('2024-01-31')
    );

    expect(stats).toHaveProperty('totalCommissions');
    expect(stats).toHaveProperty('paidAmount');
    expect(stats).toHaveProperty('pendingAmount');
    expect(stats.totalCommissions).toBeGreaterThanOrEqual(0);
  });
});
```

---

### Test 2: useCommissions Hook

```javascript
// __tests__/useCommissions.test.js

import { renderHook, act } from '@testing-library/react';
import { useAllCommissions } from '../hooks/useCommissions';

describe('useAllCommissions', () => {
  it('should load commissions', async () => {
    const { result } = renderHook(() => useAllCommissions(20));

    expect(result.current.loading).toBe(true);

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 1000));
    });

    expect(result.current.loading).toBe(false);
    expect(Array.isArray(result.current.commissions)).toBe(true);
  });
});
```

---

## Debugging Tips

### 1. Check Firestore Data

```javascript
// In browser console
import { collection, getDocs } from 'firebase/firestore';
import { db } from './firebase';

const docs = await getDocs(collection(db, 'admin_commissions'));
docs.forEach(doc => console.log(doc.data()));
```

---

### 2. Monitor Real-Time Updates

```javascript
// In browser console
import { subscribeToPendingCommissions } from './services/commissionService';

const unsubscribe = subscribeToPendingCommissions(
  (data) => console.log('Updated:', data),
  (error) => console.error('Error:', error)
);

// Stop monitoring
unsubscribe();
```

---

### 3. Check Network Requests

```javascript
// In DevTools Network tab
// Look for requests to Firestore
// Check response payloads for correct field names
// Verify timestamps are Firestore Timestamp objects
```

---

## Performance Optimization Tips

1. **Use Pagination:** Load 20-50 items at a time, not all
2. **Debounce Filters:** Delay API calls while user is typing
3. **Memoize Components:** Use React.memo for list items
4. **Lazy Load:** Load commission details on demand
5. **Cache Results:** Store stats in state to avoid refetching

---

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Stats showing 0 | Check field name is `commission_amount` |
| Infinite loader | Clear cache, reload page |
| Pagination loops | Check useCallback dependencies |
| Refresh not working | Verify all service functions imported |
| Real-time not updating | Check Firestore rules and indexes |

---

