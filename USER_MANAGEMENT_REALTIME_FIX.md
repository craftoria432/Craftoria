# User Management Real-Time Integration Fix

## Current Status: ❌ NOT Real-Time

Your `UserManagement` component uses `getDocs()` for one-time fetches. Changes from the mobile app won't appear until manual page refresh.

---

## Critical Issues

### 1. One-Time Data Fetch
```javascript
// ❌ CURRENT - Not real-time
const loadUsers = useCallback(async () => {
  const snapshot = await getDocs(query(collection(db, 'users')));
  const usersData = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
  setUsers(usersData);
}, []);
```

### 2. Manual State Updates
```javascript
// ❌ After suspend - manually updates state
await updateDoc(doc(db, 'users', user.id), { status: 'suspended' });
setUsers(prev => prev.map(u => u.id === user.id ? { ...u, status: 'suspended' } : u));
```

### 3. Wrong Timestamp Format
```javascript
// ❌ Uses ISO string instead of Firestore Timestamp
suspendedAt: new Date().toISOString()
```

---

## ✅ Complete Fixed Implementation

### Step 1: Add Required Imports

```javascript
import {
  collection,
  doc,
  updateDoc,
  onSnapshot,        // ✅ Add this
  serverTimestamp    // ✅ Add this
} from 'firebase/firestore';
```

### Step 2: Replace loadUsers with Real-Time Listener

```javascript
// ❌ REMOVE THIS:
const loadUsers = useCallback(async () => {
  try {
    setLoading(true);
    const snapshot = await getDocs(query(collection(db, 'users')));
    const usersData = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
    setUsers(usersData);
    setFilteredUsers(usersData);
  } catch (error) {
    console.error('Error loading users:', error);
    toast.error('Failed to load users');
  } finally {
    setLoading(false);
  }
}, []);

useEffect(() => { loadUsers(); }, [loadUsers]);

// ✅ REPLACE WITH THIS:
useEffect(() => {
  setLoading(true);
  
  const unsubscribe = onSnapshot(
    collection(db, 'users'),
    (snapshot) => {
      try {
        const usersData = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data(),
          created_at: doc.data().created_at?.toDate?.() || 
                      (doc.data().created_at?.seconds ? 
                        new Date(doc.data().created_at.seconds * 1000) : 
                        new Date(doc.data().created_at))
        }));
        
        setUsers(usersData);
        setLoading(false);
      } catch (error) {
        console.error('Error processing users snapshot:', error);
        toast.error('Failed to process users data');
        setLoading(false);
      }
    },
    (error) => {
      console.error('Error listening to users:', error);
      toast.error('Failed to load users');
      setLoading(false);
    }
  );

  return () => unsubscribe();
}, []);
```

### Step 3: Remove Manual State Updates

#### Suspend User
```javascript
const confirmSuspend = async () => {
  if (!suspendReason.trim()) {
    toast.error('Please provide a reason for suspension');
    return;
  }
  
  try {
    await updateDoc(doc(db, 'users', suspendModal.user.id), {
      status: 'suspended',
      suspend_reason: suspendReason,      // ✅ Use snake_case
      suspended_at: serverTimestamp()     // ✅ Use serverTimestamp()
    });
    
    // ❌ REMOVE THIS LINE:
    // setUsers(prev => prev.map(u => u.id === suspendModal.user.id ? { ...u, status: 'suspended' } : u));
    
    // ✅ onSnapshot will automatically update state
    toast.success(`${suspendModal.user.name} has been suspended`);
    setSuspendModal({ open: false, user: null });
    setSuspendReason('');
  } catch (error) {
    console.error('Error suspending user:', error);
    toast.error('Failed to suspend user');
  }
};
```

#### Activate User
```javascript
const handleActivateUser = async (user) => {
  if (!canSuspendUsers) {
    toast.error('You do not have permission to activate users');
    return;
  }
  
  try {
    await updateDoc(doc(db, 'users', user.id), {
      status: 'active',
      activated_at: serverTimestamp()  // ✅ Use serverTimestamp()
    });
    
    // ❌ REMOVE THIS LINE:
    // setUsers(prev => prev.map(u => u.id === user.id ? { ...u, status: 'active' } : u));
    
    // ✅ onSnapshot will automatically update state
    toast.success(`${user.name} has been activated`);
    setViewModal(prev => ({ open: false, user: prev.user }));
  } catch (error) {
    console.error('Error activating user:', error);
    toast.error('Failed to activate user');
  }
};
```

#### Delete User
```javascript
const confirmDelete = async () => {
  try {
    const userId = deleteModal.user.id;
    
    await updateDoc(doc(db, 'users', userId), {
      status: 'deleted',
      deleted_at: serverTimestamp()  // ✅ Use serverTimestamp()
    });
    
    // ❌ REMOVE THIS LINE:
    // setUsers(prev => prev.map(u => u.id === userId ? { ...u, status: 'deleted' } : u));
    
    // ✅ onSnapshot will automatically update state
    toast.success('User account deleted successfully');
    setDeleteModal({ open: false, user: null });
  } catch (error) {
    console.error('Error deleting user:', error);
    toast.error('Failed to delete user');
  }
};
```

---

## Mobile App Field Verification

Ensure your Kotlin User model matches these field names:

### User.kt (Mobile App)
```kotlin
data class User(
    val name: String,
    val email: String,
    val phone: String?,
    val role: String,              // "buyer", "seller", "admin"
    val status: String,            // "active", "suspended", "deleted"
    val created_at: Timestamp,     // ✅ Must be created_at (not createdAt)
    val suspend_reason: String?,   // ✅ Must be suspend_reason (not suspendReason)
    val suspended_at: Timestamp?,  // ✅ Must be suspended_at (not suspendedAt)
    val activated_at: Timestamp?,
    val deleted_at: Timestamp?,
    val orders: Int = 0
)
```

---

## Real-Time Flow

### Scenario: User Registers in Mobile App

```
Mobile App (Kotlin)              Firebase                    Web Dashboard
-------------------              --------                    -------------
User registers       ──────>    users collection    ──────>  onSnapshot fires
with created_at                 document added                ↓
                                                             setUsers(newData)
                                                             ↓
                                                             UI updates (1-2 sec)
                                                             ↓
                                                             User appears in table
```

### Scenario: Admin Suspends User in Web Dashboard

```
Web Dashboard                    Firebase                    Mobile App
-------------                    --------                    ----------
Admin clicks Suspend ──────>    users collection    ──────>  Real-time listener
updateDoc()                     document updated              ↓
                                                             User status changes
                                                             ↓
                                                             App blocks access
```

### Scenario: User Updates Profile in Mobile App

```
Mobile App                       Firebase                    Web Dashboard
----------                       --------                    -------------
Edit profile         ──────>    users collection    ──────>  onSnapshot fires
updateDoc()                     document updated              ↓
                                                             setUsers(newData)
                                                             ↓
                                                             Table row updates (1-2 sec)
```

---

## Additional Improvements

### 1. Add Timestamp Conversion Helper

```javascript
// At top of file, after imports
const convertTimestamp = (timestamp) => {
  if (!timestamp) return null;
  if (timestamp.toDate) return timestamp.toDate();
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000);
  return new Date(timestamp);
};
```

### 2. Update Date Display

```javascript
// In table cell
<TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
  {user.created_at ? 
    (user.created_at instanceof Date ? 
      user.created_at : 
      convertTimestamp(user.created_at)
    ).toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric', 
      year: 'numeric' 
    }) : 
    'N/A'
  }
</TableCell>
```

### 3. Fix Field Names for Consistency

```javascript
// When displaying suspend reason
{viewModal.user?.suspend_reason || viewModal.user?.suspendReason}

// When displaying suspended date
{convertTimestamp(viewModal.user?.suspended_at || viewModal.user?.suspendedAt)}
```

---

## Complete Fixed Code

### Full useEffect with Real-Time Listener

```javascript
useEffect(() => {
  setLoading(true);
  
  const unsubscribe = onSnapshot(
    collection(db, 'users'),
    (snapshot) => {
      try {
        const usersData = snapshot.docs.map(doc => {
          const data = doc.data();
          return {
            id: doc.id,
            ...data,
            created_at: convertTimestamp(data.created_at),
            suspended_at: convertTimestamp(data.suspended_at),
            activated_at: convertTimestamp(data.activated_at),
            deleted_at: convertTimestamp(data.deleted_at)
          };
        });
        
        setUsers(usersData);
        setLoading(false);
      } catch (error) {
        console.error('Error processing users snapshot:', error);
        toast.error('Failed to process users data');
        setLoading(false);
      }
    },
    (error) => {
      console.error('Error listening to users:', error);
      toast.error('Failed to load users');
      setLoading(false);
    }
  );

  return () => unsubscribe();
}, []);
```

### Helper Function for Timestamp Conversion

```javascript
const convertTimestamp = (timestamp) => {
  if (!timestamp) return null;
  if (timestamp.toDate) return timestamp.toDate();
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000);
  if (timestamp instanceof Date) return timestamp;
  return new Date(timestamp);
};
```

---

## Testing Checklist

### Real-Time Integration Tests

- [ ] Open web dashboard, register user in mobile app → appears in web dashboard
- [ ] Suspend user in web dashboard → status changes in mobile app
- [ ] Activate user in web dashboard → status changes in mobile app
- [ ] Delete user in web dashboard → status changes in mobile app
- [ ] Update user profile in mobile app → changes appear in web dashboard
- [ ] Multiple admins viewing dashboard → all see same updates
- [ ] Filter/search still works with real-time updates
- [ ] Timestamps display correctly

### Field Name Verification

- [ ] Check `created_at` field in Firestore console
- [ ] Check `suspend_reason` field when user is suspended
- [ ] Check `suspended_at` field when user is suspended
- [ ] Verify mobile app uses same field names

### Performance Tests

- [ ] Dashboard loads within 2 seconds
- [ ] Updates appear within 1-2 seconds
- [ ] No memory leaks (check browser dev tools)
- [ ] Listener cleanup on unmount

---

## Performance Metrics

### Firestore Reads
- **Initial load**: 1 read per user (e.g., 100 users = 100 reads)
- **Per update**: 1 read (only changed document)
- **Daily estimate**: ~200-500 reads
- **Free tier**: 50,000 reads/day
- **Verdict**: ✅ Within limits

### Optimization Tips

1. **Pagination**: For large user bases (>500 users), implement pagination
2. **Indexes**: Ensure Firestore indexes for `role` and `status` filters
3. **Caching**: Consider client-side caching for frequently accessed data

---

## Summary of Changes

| Component | Before | After |
|-----------|--------|-------|
| **Data Fetch** | `getDocs()` one-time | `onSnapshot()` real-time |
| **State Updates** | Manual after actions | Automatic via listener |
| **Timestamps** | `new Date().toISOString()` | `serverTimestamp()` |
| **Field Names** | Mixed case | snake_case (mobile app compatible) |
| **Mobile App Sync** | ❌ No | ✅ Yes |
| **Multi-Admin Support** | ❌ No | ✅ Yes |

---

## Next Steps

1. ✅ Apply all changes from this document
2. ✅ Verify field names match mobile app
3. ✅ Test real-time updates with mobile app
4. ✅ Create Firestore indexes if prompted
5. ✅ Monitor performance in Firebase Console
6. ✅ Deploy to production

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-09  
**Status**: Ready for Implementation
