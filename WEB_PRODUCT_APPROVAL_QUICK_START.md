# Web Product Approval - Quick Start Guide

## TL;DR - What You Need to Do

### 1. Copy-Paste These Imports
```javascript
import { CheckCircle as CheckCircleIcon } from '@mui/icons-material';
import { Clear as ClearIcon } from '@mui/icons-icons';
```

### 2. Add This State
```javascript
const [approvalFilter, setApprovalFilter] = useState('all');
```

### 3. Add This Filter Logic
```javascript
if (approvalFilter !== 'all') {
    f = f.filter(p => (p.approval_status || 'approved').toLowerCase() === approvalFilter);
}
```

### 4. Add This Helper Function
```javascript
const getApprovalStatusColor = (status) => {
    const map = {
        pending: { bg: '#fff3cd', color: '#856404' },
        approved: { bg: '#d4edda', color: '#155724' },
        rejected: { bg: '#f8d7da', color: '#721c24' },
    };
    return map[(status || 'approved').toLowerCase()] || { bg: '#e0e0e0', color: '#666' };
};
```

### 5. Add Filter Buttons (After Status Filters)
```javascript
<Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap', mb: 2 }}>
    {['all', 'pending', 'approved', 'rejected'].map(s => (
        <Box
            key={s}
            onClick={() => setApprovalFilter(s)}
            sx={{
                padding: '8px 18px',
                borderRadius: '20px',
                border: '2px solid #e0e0e0',
                background: approvalFilter === s ? 'linear-gradient(45deg, #E91E63, #F06292)' : 'white',
                color: approvalFilter === s ? 'white' : '#666',
                fontWeight: 600,
                fontSize: '0.8rem',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                '&:hover': { borderColor: '#e91e63' },
                boxShadow: approvalFilter === s ? '0 3px 10px rgba(233,30,99,0.3)' : 'none'
            }}
        >
            {s.charAt(0).toUpperCase() + s.slice(1)}
        </Box>
    ))}
</Box>
```

### 6. Add Table Column Header
```javascript
<TableCell sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666', textTransform: 'uppercase' }}>
    Approval Status
</TableCell>
```

### 7. Add Table Column Cell (After Status)
```javascript
<TableCell>
    <Chip
        label={product.approval_status || 'approved'}
        size="small"
        sx={{
            background: getApprovalStatusColor(product.approval_status).bg,
            color: getApprovalStatusColor(product.approval_status).color,
            fontWeight: 600,
            fontSize: '0.7rem'
        }}
    />
</TableCell>
```

### 8. Add Approve Handler
```javascript
const handleApproveProduct = async (product) => {
    if (!requireId(product?.id, 'approve')) return;
    
    try {
        await updateDoc(doc(db, 'products', product.id), {
            approval_status: 'approved',
            approved_at: serverTimestamp(),
            approved_by: currentUser.id,
            is_active: true
        });
        
        toast.success('Product approved successfully!');
    } catch (err) {
        toast.error(err.code === 'permission-denied' ? 'Permission denied' : `Approve failed: ${err.message}`);
    }
};
```

### 9. Update Reject Handler
```javascript
const handleRejectProduct = async () => {
    if (!flagForm.reason) {
        toast.error('Please select a reason');
        return;
    }
    
    if (!requireId(flagModal.product?.id, 'reject')) return;
    
    try {
        await updateDoc(doc(db, 'products', flagModal.product.id), {
            approval_status: 'rejected',
            rejection_reason: flagForm.reason,
            rejection_notes: flagForm.notes,
            is_active: false,
            rejected_at: serverTimestamp(),
            rejected_by: currentUser.id
        });
        
        toast.success('Product rejected!');
        setFlagModal({ open: false, product: null });
    } catch (err) {
        toast.error(err.code === 'permission-denied' ? 'Permission denied' : `Reject failed: ${err.message}`);
    }
};
```

### 10. Update Action Buttons
Replace the action buttons section with:

```javascript
<TableCell>
    <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
        {/* View */}
        <Box onClick={() => setViewModal({ open: true, product })} 
             sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')} 
             title="View Details">
            <VisibilityIcon />
        </Box>

        {/* Edit */}
        <ProtectedAction permission={PERMISSIONS.EDIT_PRODUCTS} hideIfNoAccess={true}>
            <Box onClick={() => handleEditProduct(product)} 
                 sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')} 
                 title="Edit Product">
                <EditIcon />
            </Box>
        </ProtectedAction>

        {/* Delete */}
        <ProtectedAction permission={PERMISSIONS.DELETE_PRODUCTS} hideIfNoAccess={true}>
            <Box onClick={() => handleDeleteModal(product)} 
                 sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')} 
                 title="Delete Product">
                <DeleteIcon />
            </Box>
        </ProtectedAction>

        {/* Approve - for pending */}
        {product.approval_status === 'pending' && (
            <Box onClick={() => handleApproveProduct(product)} 
                 sx={actionBtnSx('rgba(67,160,71,0.12)', '#43A047')} 
                 title="Approve Product">
                <CheckCircleIcon />
            </Box>
        )}

        {/* Reject - for pending */}
        {product.approval_status === 'pending' && (
            <Box onClick={() => { 
                setFlagForm({ reason: '', notes: '' }); 
                setFlagModal({ open: true, product }); 
            }} 
                 sx={actionBtnSx('rgba(255,152,0,0.12)', '#FF9800')} 
                 title="Reject Product">
                <ClearIcon />
            </Box>
        )}

        {/* Re-approve - for rejected */}
        {product.approval_status === 'rejected' && (
            <Box onClick={() => handleApproveProduct(product)} 
                 sx={actionBtnSx('rgba(67,160,71,0.12)', '#43A047')} 
                 title="Approve Product">
                <CheckCircleIcon />
            </Box>
        )}
    </Box>
</TableCell>
```

### 11. Update Flag Modal Title
```javascript
<DialogTitle sx={titleSx}>
    <FlagIcon />
    {flagModal.product?.approval_status === 'pending' ? 'Reject Product' : 'Flag Product for Review'}
</DialogTitle>
```

### 12. Update Flag Modal Button
```javascript
<Button 
    onClick={flagModal.product?.approval_status === 'pending' ? handleRejectProduct : confirmFlag} 
    variant="contained" 
    startIcon={<FlagIcon />} 
    sx={primaryBtnSx}
>
    {flagModal.product?.approval_status === 'pending' ? 'Reject Product' : 'Flag Product'}
</Button>
```

### 13. Update Rejection Reasons
```javascript
<MenuItem value="">Select a reason</MenuItem>
<MenuItem value="not_handicraft">Not a Handicraft</MenuItem>
<MenuItem value="poor_quality">Poor Quality</MenuItem>
<MenuItem value="inappropriate">Inappropriate Content</MenuItem>
<MenuItem value="pricing">Pricing Concerns</MenuItem>
<MenuItem value="copyright">Copyright Violation</MenuItem>
<MenuItem value="other">Other</MenuItem>
```

### 14. Add Rejection Info to View Modal
```javascript
{viewModal.product?.approval_status === 'rejected' && (
    <Box sx={{ mb: 2, p: 2, background: '#f8d7da', borderRadius: '10px', border: '1px solid #f5c6cb' }}>
        <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#721c24', textTransform: 'uppercase', mb: 0.75 }}>
            Rejection Reason
        </Typography>
        <Typography sx={{ fontSize: '0.85rem', color: '#721c24', fontWeight: 500, mb: 1 }}>
            {viewModal.product.rejection_reason || 'No reason provided'}
        </Typography>
        {viewModal.product.rejection_notes && (
            <>
                <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#721c24', textTransform: 'uppercase', mb: 0.75 }}>
                    Notes
                </Typography>
                <Typography sx={{ fontSize: '0.85rem', color: '#721c24' }}>
                    {viewModal.product.rejection_notes}
                </Typography>
            </>
        )}
    </Box>
)}
```

---

## Implementation Order

1. Add imports
2. Add state
3. Add helper function
4. Add filter buttons
5. Add table column
6. Add handlers
7. Update action buttons
8. Update modals
9. Test everything

---

## Testing

```javascript
// Test 1: Create pending product
// Expected: approval_status = "pending"

// Test 2: Click Approve
// Expected: approval_status = "approved", is_active = true

// Test 3: Click Reject
// Expected: approval_status = "rejected", is_active = false

// Test 4: Filter by Pending
// Expected: Only pending products shown

// Test 5: Filter by Approved
// Expected: Only approved products shown

// Test 6: Filter by Rejected
// Expected: Only rejected products shown
```

---

## Common Mistakes to Avoid

❌ Forgetting to import icons
❌ Not updating the flag modal logic
❌ Forgetting to set `is_active: true` on approval
❌ Not handling rejection reason properly
❌ Forgetting to update filter logic
❌ Not showing rejection info in view modal

---

