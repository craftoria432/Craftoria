# Web Product Approval - Ready-to-Copy Code Snippets

## 1. Imports (Add to top of ProductManagement.jsx)

```javascript
import { CheckCircle as CheckCircleIcon } from '@mui/icons-material';
import { Clear as ClearIcon } from '@mui/icons-material';
```

## 2. State (Add to ProductManagement component)

```javascript
const [approvalFilter, setApprovalFilter] = useState('all');
```

## 3. Helper Function (Add before return statement)

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

## 4. Filter Logic (Add to filteredProducts useMemo)

```javascript
if (approvalFilter !== 'all') {
    f = f.filter(p => (p.approval_status || 'approved').toLowerCase() === approvalFilter);
}
```

## 5. Approve Handler (Add before return statement)

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

## 6. Reject Handler (Replace existing confirmFlag or add new)

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
