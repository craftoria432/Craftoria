# ML Kit Web Dashboard Integration - Quick Start

## 3-Step Implementation

### Step 1: Add New Components (5 min)
Copy these 3 files to your project:
- ✅ `src/components/seller/MLKitQualityCard.jsx` (created)
- ✅ `src/components/dashboard/MLKitStatsPanel.jsx` (created)
- ✅ `src/services/mlKitAuditService.js` (created)

---

### Step 2: Update UserCard.jsx (2 min)
**File:** `src/components/seller/UserCard.jsx`

1. Add import at top:
```jsx
import MLKitQualityCard from './MLKitQualityCard';
```

2. Find the ML Kit Results section (around line 100)
3. Replace entire section with:
```jsx
{!isApplication && user.mlKitResult && (
  <MLKitQualityCard mlKitResult={user.mlKitResult} />
)}
```

4. Delete the `getConfidenceColor` function (no longer needed)

---

### Step 3: Update SellerVerification.jsx (5 min)
**File:** `src/pages/SellerVerification.jsx`

**3a. Add imports:**
```jsx
import MLKitStatsPanel from '../components/dashboard/MLKitStatsPanel';
import { logMLKitDecision } from '../services/mlKitAuditService';
```

**3b. Add state (after line ~30):**
```jsx
const [mlKitFilter, setMlKitFilter] = useState('all');
```

**3c. Add stats panel (after existing stats cards, around line ~150):**
```jsx
{activeTab === 1 && (
  <MLKitStatsPanel verifications={verifications} />
)}
```

**3d. Add ML Kit filters (in the filter section, around line ~180):**
```jsx
{/* ML Kit Quality Filters */}
<Box sx={{ display: 'flex', gap: '8px', flexWrap: 'wrap', ml: { xs: 0, sm: 'auto' } }}>
  {['all', 'high', 'low', 'invalid'].map((f) => (
    <Box
      key={f}
      onClick={() => setMlKitFilter(f)}
      sx={{
        px: '12px',
        py: '6px',
        borderRadius: '20px',
        border: '1px solid',
        borderColor: mlKitFilter === f ? '#E91E63' : '#e2e8f0',
        background: mlKitFilter === f ? '#E91E63' : '#fff',
        color: mlKitFilter === f ? '#fff' : '#64748b',
        fontSize: '0.75rem',
        fontWeight: 600,
        cursor: 'pointer',
        transition: 'all 0.2s ease',
        userSelect: 'none',
        '&:hover': { borderColor: '#E91E63' },
      }}
    >
      {f === 'all' && 'All Quality'}
      {f === 'high' && 'High (≥80%)'}
      {f === 'low' && 'Low (<50%)'}
      {f === 'invalid' && 'Invalid'}
    </Box>
  ))}
</Box>
```

**3e. Update filtering logic (around line ~220):**
Replace the entire `useEffect` with:
```jsx
useEffect(() => {
  let filtered = users;
  if (activeTab === 0) {
    filtered = users.filter(u => u.type === 'application');
  } else {
    filtered = users.filter(u => u.type === 'verification');
    if (activeFilter !== 'all') {
      filtered = filtered.filter(u => u.verification_status === activeFilter);
    }

    // Add ML Kit quality filtering
    if (mlKitFilter !== 'all') {
      filtered = filtered.filter(u => {
        const confidence = u.mlKitResult?.confidence || 0;
        if (mlKitFilter === 'high') return confidence >= 80;
        if (mlKitFilter === 'low') return confidence < 50;
        if (mlKitFilter === 'invalid') return !u.mlKitResult?.isValid;
        return true;
      });
    }
  }

  if (searchQuery) {
    filtered = filtered.filter(u =>
      u.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      u.email?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      String(u.phone || '').includes(searchQuery)
    );
  }

  setFilteredUsers(filtered);
}, [activeTab, activeFilter, mlKitFilter, searchQuery, users]);
```

**3f. Add audit logging to approval (around line ~280):**
After the `updateDoc` call in `handleApproveVerification`, add:
```jsx
// Log ML Kit decision
await logMLKitDecision(
  approveVerificationModal.user.id,
  'approved',
  approveVerificationModal.user.mlKitResult,
  currentUser?.email
);
```

**3g. Add audit logging to rejection (around line ~350):**
After the `updateDoc` call in `handleRejectVerification`, add:
```jsx
// Log ML Kit decision
await logMLKitDecision(
  rejectVerificationModal.user.id,
  'rejected',
  rejectVerificationModal.user.mlKitResult,
  currentUser?.email
);
```

---

### Step 4: Update Firestore Rules (1 min)
**File:** `firestore.rules`

Add this section:
```
match /ml_kit_audit_logs/{document=**} {
  allow read: if request.auth.uid != null && 
              get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
  allow create: if request.auth.uid != null && 
                get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
}
```

Deploy rules:
```bash
firebase deploy --only firestore:rules
```

---

## What You Get

✅ **ML Kit Quality Card** - Visual confidence score with color-coded quality levels
✅ **ML Kit Stats Panel** - Dashboard showing aggregate quality metrics
✅ **Quality Filters** - Filter verifications by confidence level
✅ **Audit Logging** - Track all ML Kit-based decisions
✅ **Recommendations** - Admin guidance based on ML Kit confidence

---

## Testing

1. **Mobile:** Submit seller verification with face
2. **Firebase:** Check `seller_verifications` collection
3. **Web Dashboard:**
   - Go to "Identity Verifications" tab
   - See ML Kit stats panel at top
   - See quality filters (High/Low/Invalid)
   - Click on a verification to see quality card
   - Approve/reject to log decision
4. **Verify:** Check `ml_kit_audit_logs` collection

---

## Files Modified Summary

| File | Changes |
|------|---------|
| `src/components/seller/UserCard.jsx` | Replace ML Kit section with component |
| `src/pages/SellerVerification.jsx` | Add imports, state, filters, logging |
| `firestore.rules` | Add audit log permissions |

**Total time:** ~15 minutes
**Lines of code:** ~200 (mostly UI)
**Breaking changes:** None
