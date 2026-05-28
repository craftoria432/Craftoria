# ML Kit Web Dashboard Integration - Code Changes

## Files Created ✅
1. `src/components/seller/MLKitQualityCard.jsx` - Quality assessment visualization
2. `src/components/dashboard/MLKitStatsPanel.jsx` - Statistics dashboard
3. `src/services/mlKitAuditService.js` - Audit logging service

---

## Files to Modify

### 1. `src/components/seller/UserCard.jsx`
**Change:** Replace ML Kit Results section with new component

**FIND THIS:**
```jsx
{/* ML Kit Results - Only for Verifications */}
{!isApplication && user.mlKitResult && (
  <Box
    sx={{
      background: '#fafbfc',
      border: '1px solid #eef0f4',
      borderRadius: '10px',
      p: '14px',
      mb: '16px',
    }}
  >
    <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', mb: '12px' }}>
      <PsychologyIcon sx={{ fontSize: 18, color: '#E91E63' }} />
      <Typography sx={{ fontSize: '0.78rem', fontWeight: 700, color: '#1a1d23' }}>
        ML Kit Face Detection Results
      </Typography>
    </Box>

    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px' }}>
      {/* Confidence Score */}
      <Box>
        <Typography sx={{ fontSize: '0.68rem', color: '#8b919e', mb: '4px', fontWeight: 600 }}>
          Confidence Score
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Box
            sx={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              background: getConfidenceColor(user.mlKitResult.confidence),
            }}
          />
          <Typography
            sx={{
              fontSize: '0.88rem',
              fontWeight: 700,
              color: getConfidenceColor(user.mlKitResult.confidence),
            }}
          >
            {user.mlKitResult.confidence?.toFixed(1) || 0}%
          </Typography>
        </Box>
      </Box>

      {/* Face Count */}
      <Box>
        <Typography sx={{ fontSize: '0.68rem', color: '#8b919e', mb: '4px', fontWeight: 600 }}>
          Faces Detected
        </Typography>
        <Typography sx={{ fontSize: '0.88rem', fontWeight: 700, color: '#1a1d23' }}>
          {user.mlKitResult.faceCount || 0}
        </Typography>
      </Box>

      {/* Validation Status */}
      <Box>
        <Typography sx={{ fontSize: '0.68rem', color: '#8b919e', mb: '4px', fontWeight: 600 }}>
          ML Kit Status
        </Typography>
        <Chip
          label={user.mlKitResult.isValid ? 'Valid' : 'Invalid'}
          size="small"
          sx={{
            fontSize: '0.68rem',
            fontWeight: 700,
            height: '20px',
            background: user.mlKitResult.isValid ? '#E8F5E9' : '#FFEBEE',
            color: user.mlKitResult.isValid ? '#2E7D32' : '#C62828',
            border: '1px solid',
            borderColor: user.mlKitResult.isValid ? '#A5D6A7' : '#EF9A9A',
          }}
        />
      </Box>
    </Box>

    {/* ML Kit Message */}
    {user.mlKitResult.message && (
      <Box
        sx={{
          mt: '12px',
          pt: '12px',
          borderTop: '1px solid #eef0f4',
        }}
      >
        <Typography sx={{ fontSize: '0.72rem', color: '#64748b', fontStyle: 'italic' }}>
          "{user.mlKitResult.message}"
        </Typography>
      </Box>
    )}
  </Box>
)}
```

**REPLACE WITH:**
```jsx
import MLKitQualityCard from './MLKitQualityCard';

// Inside UserCard component:
{!isApplication && user.mlKitResult && (
  <MLKitQualityCard mlKitResult={user.mlKitResult} />
)}
```

**REMOVE:** The `getConfidenceColor` function (no longer needed)

---

### 2. `src/pages/SellerVerification.jsx`
**Change 1:** Add imports at top

**ADD AFTER existing imports:**
```jsx
import MLKitStatsPanel from '../components/dashboard/MLKitStatsPanel';
import { logMLKitDecision } from '../services/mlKitAuditService';
```

---

**Change 2:** Add ML Kit filter state

**FIND THIS:**
```jsx
const [activeFilter, setActiveFilter] = useState('all');
const [searchQuery, setSearchQuery] = useState('');
```

**ADD AFTER:**
```jsx
const [mlKitFilter, setMlKitFilter] = useState('all'); // all, high, low, invalid
```

---

**Change 3:** Add ML Kit stats panel to render

**FIND THIS:**
```jsx
{/* Stats Cards - Only on Verifications Tab */}
{activeTab === 1 && (
  <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' }, gap: '16px', mb: '24px' }}>
    <StatCard title="Pending Verifications" value={pendingVerifications.length} change={0} iconType="pending" />
    <StatCard title="Approved" value={approvedVerifications} change={0} iconType="approved" />
    <StatCard title="Rejected" value={rejectedVerifications} change={0} iconType="rejected" />
    <StatCard title="Total Verifications" value={verifications.length} change={0} iconType="verified" />
  </Box>
)}
```

**ADD AFTER:**
```jsx
{/* ML Kit Stats Panel */}
{activeTab === 1 && (
  <MLKitStatsPanel verifications={verifications} />
)}
```

---

**Change 4:** Add ML Kit quality filters

**FIND THIS:**
```jsx
{/* Filter chips — only on verifications tab */}
{activeTab === 1 && (
  <Box sx={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
    {filterOptions.map((f) => (
      <Box
        key={f}
        onClick={() => setActiveFilter(f)}
        sx={{
          px: '14px',
          py: '7px',
          borderRadius: '20px',
          border: '1px solid',
          borderColor: activeFilter === f ? '#E91E63' : '#e2e8f0',
          background: activeFilter === f ? '#E91E63' : '#fff',
          color: activeFilter === f ? '#fff' : '#64748b',
          fontSize: '0.78rem',
          fontWeight: 600,
          cursor: 'pointer',
          transition: 'all 0.2s ease',
          userSelect: 'none',
          '&:hover': { borderColor: '#E91E63' },
        }}
      >
        {f.charAt(0).toUpperCase() + f.slice(1)}
      </Box>
    ))}
  </Box>
)}
```

**REPLACE WITH:**
```jsx
{/* Filter chips — only on verifications tab */}
{activeTab === 1 && (
  <Box sx={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
    {/* Status Filters */}
    <Box sx={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
      {filterOptions.map((f) => (
        <Box
          key={f}
          onClick={() => setActiveFilter(f)}
          sx={{
            px: '14px',
            py: '7px',
            borderRadius: '20px',
            border: '1px solid',
            borderColor: activeFilter === f ? '#E91E63' : '#e2e8f0',
            background: activeFilter === f ? '#E91E63' : '#fff',
            color: activeFilter === f ? '#fff' : '#64748b',
            fontSize: '0.78rem',
            fontWeight: 600,
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            userSelect: 'none',
            '&:hover': { borderColor: '#E91E63' },
          }}
        >
          {f.charAt(0).toUpperCase() + f.slice(1)}
        </Box>
      ))}
    </Box>

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
  </Box>
)}
```

---

**Change 5:** Update filtering logic

**FIND THIS:**
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
  }

  if (searchQuery) {
    filtered = filtered.filter(u =>
      u.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      u.email?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      String(u.phone || '').includes(searchQuery)
    );
  }

  setFilteredUsers(filtered);
}, [activeTab, activeFilter, searchQuery, users]);
```

**REPLACE WITH:**
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

---

**Change 6:** Update approval handler

**FIND THIS:**
```jsx
const handleApproveVerification = async () => {
  if (!canApproveSellers) { toast.error('You do not have permission to approve verifications'); return; }

  try {
    await updateDoc(doc(db, 'users', approveVerificationModal.user.id), {
      verification_status: 'approved',
      verified_at: serverTimestamp(),
      verified: true,
      verification_welcome_message: welcomeMessage,
      verified_by: currentUser?.email || 'unknown',
    });

    // ... rest of code
```

**ADD AFTER updateDoc:**
```jsx
    // Log ML Kit decision
    await logMLKitDecision(
      approveVerificationModal.user.id,
      'approved',
      approveVerificationModal.user.mlKitResult,
      currentUser?.email
    );
```

---

**Change 7:** Update rejection handler

**FIND THIS:**
```jsx
const handleRejectVerification = async () => {
  if (!canRejectSellers) { toast.error('You do not have permission to reject verifications'); return; }
  if (!rejectReason || !rejectMessage) { toast.error('Please provide reason and message'); return; }

  try {
    await updateDoc(doc(db, 'users', rejectVerificationModal.user.id), {
      verification_status: 'rejected',
      verification_rejected_at: serverTimestamp(),
      verification_rejection_reason: rejectReason,
      verification_rejection_message: rejectMessage,
      verified: false,
      verification_rejected_by: currentUser?.email || 'unknown',
    });

    // ... rest of code
```

**ADD AFTER updateDoc:**
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

## Firestore Rules Update

**File:** `firestore.rules`

**ADD THIS SECTION:**
```
match /ml_kit_audit_logs/{document=**} {
  allow read: if request.auth.uid != null && 
              get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
  allow create: if request.auth.uid != null && 
                get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
}
```

---

## Testing Checklist

- [ ] Mobile app submits verification with face
- [ ] Firebase stores ML Kit data in `seller_verifications`
- [ ] Web dashboard loads ML Kit stats panel
- [ ] ML Kit quality filters work (high/low/invalid)
- [ ] Admin can view quality card with confidence score
- [ ] Approval logs ML Kit decision to `ml_kit_audit_logs`
- [ ] Rejection logs ML Kit decision to `ml_kit_audit_logs`
- [ ] Firestore rules prevent unauthorized access

---

## Result
✅ Full ML Kit integration with visual quality assessment
✅ Admin dashboard shows ML Kit statistics
✅ Quality-based filtering for verifications
✅ Audit trail of all ML Kit decisions
✅ Production-ready components
