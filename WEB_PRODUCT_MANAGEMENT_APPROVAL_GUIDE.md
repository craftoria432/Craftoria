# Web Dashboard Product Management - Approval System

## Overview
The web dashboard needs to be updated to allow admins to approve/reject pending products. This ensures quality control for the Craftoria marketplace.

## Current Status
✅ Android app: Products created with `approval_status: "pending"`
✅ Android app: Sellers can see pending products
⏳ Web dashboard: Needs approval/rejection functionality

## Implementation Steps

### Step 1: Update ProductManagement.jsx - Add Approval Filter

Add approval status filter to the existing filters:

```javascript
// Add to filter state
const [approvalFilter, setApprovalFilter] = useState('all'); // all, pending, approved, rejected

// Add to filteredProducts logic
if (approvalFilter !== 'all') {
    f = f.filter(p => (p.approval_status || 'approved').toLowerCase() === approvalFilter);
}

// Add filter buttons in the UI (after status filters)
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

### Step 2: Add Approval Status Column to Table

Update the table header and rows:

```javascript
// Add to table header
<TableCell sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666', textTransform: 'uppercase' }}>
    Approval Status
</TableCell>

// Add to table row (after Status column)
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

### Step 3: Add Helper Function for Approval Status Colors

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

### Step 4: Update Action Buttons - Add Approve/Reject

```javascript
// Update the action buttons section in table
<TableCell>
    <Box sx={{ display: 'flex', gap: 0.75, flexWrap: 'wrap' }}>
        {/* View — always shown — blue */}
        <Box onClick={() => setViewModal({ open: true, product })} 
             sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')} 
             title="View Details">
            <VisibilityIcon />
        </Box>

        {/* Edit — always shown — indigo */}
        <ProtectedAction permission={PERMISSIONS.EDIT_PRODUCTS} hideIfNoAccess={true}>
            <Box onClick={() => handleEditProduct(product)} 
                 sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')} 
                 title="Edit Product">
                <EditIcon />
            </Box>
        </ProtectedAction>

        {/* Delete — always shown — red */}
        <ProtectedAction permission={PERMISSIONS.DELETE_PRODUCTS} hideIfNoAccess={true}>
            <Box onClick={() => handleDeleteModal(product)} 
                 sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')} 
                 title="Delete Product">
                <DeleteIcon />
            </Box>
        </ProtectedAction>

        {/* ✅ NEW: Approve button — only for pending products — green */}
        {product.approval_status === 'pending' && (
            <Box onClick={() => handleApproveProduct(product)} 
                 sx={actionBtnSx('rgba(67,160,71,0.12)', '#43A047')} 
                 title="Approve Product">
                <CheckCircleIcon />
            </Box>
        )}

        {/* ✅ NEW: Reject button — only for pending products — orange */}
        {product.approval_status === 'pending' && (
            <Box onClick={() => { setFlagModal({ open: true, product }); setFlagForm({ reason: '', notes: '' }); }} 
                 sx={actionBtnSx('rgba(255,152,0,0.12)', '#FF9800')} 
                 title="Reject Product">
                <ClearIcon />
            </Box>
        )}

        {/* ✅ NEW: Re-approve button — for rejected products — green */}
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

### Step 5: Add Approve Product Handler

```javascript
const handleApproveProduct = async (product) => {
    if (!requireId(product?.id, 'approve')) return;
    
    try {
        const approveData = {
            approval_status: 'approved',
            approved_at: serverTimestamp(),
            approved_by: currentUser.id,
            is_active: true  // Auto-activate on approval
        };
        
        await updateDoc(doc(db, 'products', product.id), approveData);
        
        // ✅ Log activity
        try {
            await addDoc(collection(db, 'admin_activities'), {
                admin_id: currentUser.id,
                action: 'PRODUCT_APPROVED',
                product_id: product.id,
                product_title: product.title,
                seller_id: product.seller_id,
                timestamp: serverTimestamp()
            });
        } catch (e) {
            console.error('Failed to log activity', e);
        }
        
        toast.success('Product approved successfully!');
    } catch (err) {
        toast.error(err.code === 'permission-denied' ? 'Permission denied' : `Approve failed: ${err.message}`);
    }
};
```

### Step 6: Update Reject/Flag Modal for Rejection

Modify the existing flag modal to work for rejection:

```javascript
// Update flag modal title and logic
const handleRejectProduct = async () => {
    if (!flagForm.reason) {
        toast.error('Please select a reason');
        return;
    }
    
    if (!requireId(flagModal.product?.id, 'reject')) return;
    
    try {
        const rejectData = {
            approval_status: 'rejected',
            rejection_reason: flagForm.reason,
            rejection_notes: flagForm.notes,
            is_active: false,
            rejected_at: serverTimestamp(),
            rejected_by: currentUser.id
        };
        
        await updateDoc(doc(db, 'products', flagModal.product.id), rejectData);
        
        // ✅ Log activity
        try {
            await addDoc(collection(db, 'admin_activities'), {
                admin_id: currentUser.id,
                action: 'PRODUCT_REJECTED',
                product_id: flagModal.product.id,
                product_title: flagModal.product.title,
                seller_id: flagModal.product.seller_id,
                rejection_reason: flagForm.reason,
                timestamp: serverTimestamp()
            });
        } catch (e) {
            console.error('Failed to log activity', e);
        }
        
        toast.success('Product rejected!');
        setFlagModal({ open: false, product: null });
    } catch (err) {
        toast.error(err.code === 'permission-denied' ? 'Permission denied' : `Reject failed: ${err.message}`);
    }
};

// Update flag modal to handle both flagging and rejection
<Dialog open={flagModal.open} onClose={() => setFlagModal({ open: false, product: null })} maxWidth="sm" fullWidth PaperProps={{ sx: dialogPaper }}>
    <DialogTitle sx={titleSx}>
        <FlagIcon />
        {flagModal.product?.approval_status === 'pending' ? 'Reject Product' : 'Flag Product for Review'}
    </DialogTitle>
    <DialogContent sx={{ pt: 3 }}>
        <Box sx={{ mb: 2 }}>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>
                Reason *
            </Typography>
            <FormControl fullWidth>
                <Select value={flagForm.reason} onChange={e => setFlagForm({ ...flagForm, reason: e.target.value })} displayEmpty sx={selectSx}>
                    <MenuItem value="">Select a reason</MenuItem>
                    <MenuItem value="not_handicraft">Not a Handicraft</MenuItem>
                    <MenuItem value="poor_quality">Poor Quality</MenuItem>
                    <MenuItem value="inappropriate">Inappropriate Content</MenuItem>
                    <MenuItem value="pricing">Pricing Concerns</MenuItem>
                    <MenuItem value="copyright">Copyright Violation</MenuItem>
                    <MenuItem value="other">Other</MenuItem>
                </Select>
            </FormControl>
        </Box>
        <Box>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>
                Additional Notes
            </Typography>
            <TextField fullWidth multiline rows={4} placeholder="Provide details..." value={flagForm.notes} onChange={e => setFlagForm({ ...flagForm, notes: e.target.value })} sx={textareaSx} />
        </Box>
    </DialogContent>
    <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
        <Button onClick={() => setFlagModal({ open: false, product: null })} sx={cancelBtnSx}>
            Cancel
        </Button>
        <Button 
            onClick={flagModal.product?.approval_status === 'pending' ? handleRejectProduct : confirmFlag} 
            variant="contained" 
            startIcon={<FlagIcon />} 
            sx={primaryBtnSx}
        >
            {flagModal.product?.approval_status === 'pending' ? 'Reject Product' : 'Flag Product'}
        </Button>
    </DialogActions>
</Dialog>
```

### Step 7: Add Rejection Reason to View Modal

Update the view modal to show rejection details:

```javascript
{/* VIEW MODAL - Add rejection info */}
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

### Step 8: Add Approval Metadata to View Modal

```javascript
{/* Add to view modal grid */}
<Grid container spacing={2} sx={{ mb: 1.5 }}>
    <Grid item xs={6}>
        <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 0.75 }}>
            Approval Status
        </Typography>
        <Chip 
            label={viewModal.product.approval_status || 'approved'} 
            size="small" 
            sx={{ 
                background: getApprovalStatusColor(viewModal.product.approval_status).bg, 
                color: getApprovalStatusColor(viewModal.product.approval_status).color, 
                fontWeight: 600, 
                fontSize: '0.7rem' 
            }} 
        />
    </Grid>
    <Grid item xs={6}>
        <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 0.75 }}>
            Approved By
        </Typography>
        <Typography sx={{ fontSize: '0.85rem', color: '#333', fontWeight: 500 }}>
            {viewModal.product.approved_by ? `Admin: ${viewModal.product.approved_by}` : '—'}
        </Typography>
    </Grid>
</Grid>
```

## Firestore Schema Updates

Products collection will now have:

```json
{
    "id": "product_123",
    "title": "Handmade Ceramic Vase",
    "approval_status": "pending|approved|rejected",
    "rejection_reason": "not_handicraft|poor_quality|...",
    "rejection_notes": "Additional details from admin",
    "approved_at": 1234567890,
    "approved_by": "admin_user_id",
    "rejected_at": 1234567890,
    "rejected_by": "admin_user_id",
    "is_active": true,
    // ... other fields
}
```

## Admin Activity Logging

Create new collection `admin_activities` to track approvals/rejections:

```json
{
    "admin_id": "admin_123",
    "action": "PRODUCT_APPROVED|PRODUCT_REJECTED",
    "product_id": "product_456",
    "product_title": "Handmade Vase",
    "seller_id": "seller_789",
    "rejection_reason": "not_handicraft",
    "timestamp": 1234567890
}
```

## Required Imports

Add these to ProductManagement.jsx:

```javascript
import { CheckCircle as CheckCircleIcon } from '@mui/icons-material';
import { Clear as ClearIcon } from '@mui/icons-material';
```

## Testing Checklist

- [ ] Approval filter shows only pending products
- [ ] Approve button appears only for pending products
- [ ] Reject button appears only for pending products
- [ ] Clicking approve updates product to approved status
- [ ] Clicking reject opens modal with rejection reasons
- [ ] Rejection reason is saved to Firestore
- [ ] Approved products show in approved filter
- [ ] Rejected products show in rejected filter
- [ ] Rejection details appear in view modal
- [ ] Admin activities are logged
- [ ] Sellers see updated approval status in mobile app

## Permissions

Ensure admin has `EDIT_PRODUCTS` permission to approve/reject products.

## Next Steps

1. Implement bulk approve/reject for multiple products
2. Add auto-approval for verified sellers
3. Create admin dashboard for approval metrics
4. Add seller notifications for approval/rejection
5. Implement appeal system for rejected products
