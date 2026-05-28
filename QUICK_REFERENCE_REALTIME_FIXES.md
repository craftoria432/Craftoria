# Quick Reference - Real-Time Integration Fixes

## 🚀 Common Patterns

### 1. Replace One-Time Fetch with Real-Time Listener

**❌ WRONG (One-time fetch)**:
```javascript
useEffect(() => {
  const fetchData = async () => {
    const snapshot = await getDocs(collection(db, 'products'));
    const data = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    setProducts(data);
  };
  fetchData();
}, []);
```

**✅ CORRECT (Real-time listener)**:
```javascript
useEffect(() => {
  const unsubscribe = onSnapshot(
    collection(db, 'products'),
    (snapshot) => {
      const data = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setProducts(data);
    },
    (error) => {
      console.error('Error listening to products:', error);
    }
  );
  
  return () => unsubscribe(); // Cleanup
}, []);
```

---

### 2. Convert Firestore Timestamps

**Helper Function**:
```javascript
export const convertTimestamp = (timestamp) => {
  if (!timestamp) return null;
  if (timestamp.toDate) return timestamp.toDate(); // Firestore Timestamp
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000); // Plain object
  return new Date(timestamp); // ISO string or Date
};
```

**Usage**:
```javascript
const date = convertTimestamp(user.created_at);
const formatted = date?.toLocaleDateString('en-US', {
  month: 'short',
  day: 'numeric',
  year: 'numeric'
});
```

---

### 3. Field Name Consistency

**Always use snake_case** (matches Kotlin models):

```javascript
// ✅ CORRECT
order.buyer_id
order.total_amount
order.created_at
product.seller_name
user.created_at

// ❌ WRONG
order.buyerId
order.totalAmount
order.createdAt
product.sellerName
user.createdAt
```

**With Fallback** (for backward compatibility):
```javascript
const buyerId = order.buyer_id || order.buyerId;
const sellerName = product.seller_name || product.seller;
```

---

### 4. Remove Manual State Updates After CRUD

**❌ WRONG**:
```javascript
const deleteProduct = async (id) => {
  await deleteDoc(doc(db, 'products', id));
  setProducts(prev => prev.filter(p => p.id !== id)); // ❌ Manual update
};
```

**✅ CORRECT**:
```javascript
const deleteProduct = async (id) => {
  await deleteDoc(doc(db, 'products', id));
  // ✅ Real-time listener will update automatically
};
```

---

### 5. Calculate Counts from Related Collections

**Example: User Order Count**

```javascript
const [userOrderCounts, setUserOrderCounts] = useState({});

useEffect(() => {
  const unsubscribe = onSnapshot(
    collection(db, 'orders'),
    (snapshot) => {
      const counts = {};
      snapshot.docs.forEach(doc => {
        const buyerId = doc.data().buyer_id;
        if (buyerId) {
          counts[buyerId] = (counts[buyerId] || 0) + 1;
        }
      });
      setUserOrderCounts(counts);
    }
  );
  return () => unsubscribe();
}, []);

// Display
<TableCell>{userOrderCounts[user.id] || 0}</TableCell>
```

---

### 6. Use serverTimestamp() for All Timestamps

**❌ WRONG**:
```javascript
await setDoc(doc(db, 'products', id), {
  ...productData,
  created_at: new Date().toISOString() // ❌ Client timestamp
});
```

**✅ CORRECT**:
```javascript
import { serverTimestamp } from 'firebase/firestore';

await setDoc(doc(db, 'products', id), {
  ...productData,
  created_at: serverTimestamp() // ✅ Server timestamp
});
```

---

### 7. Error Handling Pattern

```javascript
useEffect(() => {
  const unsubscribe = onSnapshot(
    collection(db, 'products'),
    (snapshot) => {
      try {
        const data = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setProducts(data);
        setError(null);
      } catch (err) {
        console.error('Error processing products:', err);
        setError('Failed to load products');
      } finally {
        setLoading(false);
      }
    },
    (err) => {
      console.error('Error listening to products:', err);
      setError('Failed to connect to database');
      setLoading(false);
    }
  );

  return () => unsubscribe();
}, []);
```

---

### 8. Display with Fallback Values

```javascript
// For numbers
<TableCell>{user.order_count || 0}</TableCell>

// For strings
<TableCell>{product.seller_name || 'Unknown'}</TableCell>

// For dates
<TableCell>
  {user.created_at ? convertTimestamp(user.created_at).toLocaleDateString() : 'N/A'}
</TableCell>

// For arrays
<TableCell>{product.images?.length || 0} images</TableCell>
```

---

### 9. Query with Filters

```javascript
useEffect(() => {
  const q = query(
    collection(db, 'orders'),
    where('status', '==', 'pending'),
    orderBy('created_at', 'desc'),
    limit(10)
  );

  const unsubscribe = onSnapshot(q, (snapshot) => {
    const orders = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));
    setPendingOrders(orders);
  });

  return () => unsubscribe();
}, []);
```

---

### 10. Merge Multiple Collections

```javascript
useEffect(() => {
  const unsubUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
    const users = snapshot.docs.map(doc => ({
      id: doc.id,
      type: 'user',
      ...doc.data()
    }));
    setUsersData(users);
  });

  const unsubOrders = onSnapshot(collection(db, 'orders'), (snapshot) => {
    const orders = snapshot.docs.map(doc => ({
      id: doc.id,
      type: 'order',
      ...doc.data()
    }));
    setOrdersData(orders);
  });

  return () => {
    unsubUsers();
    unsubOrders();
  };
}, []);

// Merge and sort
useEffect(() => {
  const merged = [...usersData, ...ordersData].sort((a, b) => {
    const dateA = convertTimestamp(a.created_at);
    const dateB = convertTimestamp(b.created_at);
    return dateB - dateA; // Newest first
  });
  setActivities(merged);
}, [usersData, ordersData]);
```

---

## 🔍 Debugging Checklist

### Data Not Showing?
1. ✅ Check Firebase Console - is data there?
2. ✅ Check browser console - any errors?
3. ✅ Check field names - exact match with Firebase?
4. ✅ Check listener is running - add console.log in onSnapshot
5. ✅ Check data format - use console.log(snapshot.docs[0].data())

### Real-Time Not Working?
1. ✅ Using onSnapshot() not getDocs()?
2. ✅ Listener cleanup in return statement?
3. ✅ No manual state updates after CRUD?
4. ✅ Check Firestore rules - read permission granted?

### Timestamps Showing Wrong?
1. ✅ Using convertTimestamp() helper?
2. ✅ Check timestamp format in Firebase Console
3. ✅ Using serverTimestamp() when writing?
4. ✅ Handling null/undefined timestamps?

### Performance Issues?
1. ✅ Add indexes for queries with where/orderBy
2. ✅ Use limit() to reduce data fetched
3. ✅ Implement pagination for large lists
4. ✅ Add caching for expensive calculations

---

## 📋 Import Checklist

**Always import these from Firebase**:
```javascript
import { 
  collection, 
  doc, 
  getDocs,        // One-time fetch
  onSnapshot,     // Real-time listener
  query, 
  where, 
  orderBy, 
  limit,
  setDoc,
  updateDoc,
  deleteDoc,
  serverTimestamp // For timestamps
} from 'firebase/firestore';
import { db } from '../services/firebase';
```

---

## 🎯 Quick Wins

### Fix 1: Add Real-Time to Existing Component (5 min)
Replace `getDocs()` with `onSnapshot()` + cleanup

### Fix 2: Fix Timestamp Display (2 min)
Add `convertTimestamp()` helper + use it

### Fix 3: Fix Field Names (3 min)
Change camelCase to snake_case + add fallback

### Fix 4: Add Order Count (10 min)
Add orders listener + count by buyer_id

### Fix 5: Remove Manual Updates (1 min)
Delete setState() calls after CRUD operations

---

**Last Updated**: 2026-03-09  
**Use this for**: Quick copy-paste solutions
