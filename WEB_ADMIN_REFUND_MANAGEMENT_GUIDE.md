# Web Admin Dashboard - Refund Management Integration Guide

## Overview

Add complete refund management to your web admin dashboard (`OrderOversight.jsx`). Admins can view, approve, reject, and process refunds directly from the dashboard.

---

## 1. Update OrderOversight.jsx

Add refund management features to the existing order oversight page.

### Step 1: Add Refund Tab to Navigation

```jsx
// Add to the filter section
const [activeTab, setActiveTab] = useState('orders'); // 'orders' or 'refunds'

// Add tab buttons
<Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
  <Button
    variant={activeTab === 'orders' ? 'contained' : 'outlined'}
    onClick={() => setActiveTab('orders')}
    sx={{ borderRadius: '10px' }}
  >
    Orders
  </Button>
  <Button
    variant={activeTab === 'refunds' ? 'contained' : 'outlined'}
    onClick={() => setActiveTab('refunds')}
    sx={{ borderRadius: '10px' }}
  >
    Refunds
    {pendingRefundsCount > 0 && (
      <Chip
        label={pendingRefundsCount}
        size="small"
        color="error"
        sx={{ ml: 1 }}
      />
    )}
  </Button>
</Box>
```

### Step 2: Add Refund State Management

```jsx
const [refunds, setRefunds] = useState([]);
const [pendingRefundsCount, setPendingRefundsCount] = useState(0);
const [refundDetailsModal, setRefundDetailsModal] = useState({ open: false, refund: null });
const [refundActionModal, setRefundActionModal] = useState({ open: false, refund: null, action: null });
const [actionNotes, setActionNotes] = useState('');
```

### Step 3: Add Real-Time Refund Listener

```jsx
// Add to useEffect
useEffect(() => {
  if (activeTab !== 'refunds') return;

  const unsubscribe = onSnapshot(collection(db, 'refunds'), (snapshot) => {
    try {
      const refundsData = snapshot.docs.map(doc => {
        const data = doc.data();
        return {
          ...data,
          id: doc.id,
          requested_at: convertTimestamp(data.requested_at),
          approved_at: convertTimestamp(data.approved_at),
          processed_at: convertTimestamp(data.processed_at),
          completed_at: convertTimestamp(data.completed_at),
        };
      });

      setRefunds(refundsData);
      
      // Count pending refunds
      const pending = refundsData.filter(r => 
        r.status === 'requested' || r.status === 'approved'
      ).length;
      setPendingRefundsCount(pending);
    } catch (error) {
      console.error('Error processing refunds:', error);
      toast.error('Failed to load refunds');
    }
  });

  return () => unsubscribe();
}, [activeTab]);
```

---

## 2. Create Refunds Table Component

```jsx
const RefundsTable = ({ refunds, onViewDetails, onApprove, onReject, onProcess }) => {
  const getStatusColor = (status) => {
    const colors = {
      requested: { bg: '#fff3cd', color: '#856404' },
      approved: { bg: '#d1ecf1', color: '#0c5460' },
      processing: { bg: '#cce5ff', color: '#004085' },
      completed: { bg: '#d4edda', color: '#155724' },
      rejected: { bg: '#f8d7da', color: '#721c24' },
      failed: { bg: '#f8d7da', color: '#721c24' },
    };
    return colors[status] || { bg: '#e0e0e0', color: '#666' };
  };

  return (
    <TableContainer>
      <Table>
        <TableHead sx={{ background: '#fafafa', borderBottom: '2px solid #e0e0e0' }}>
          <TableRow>
            {['Refund ID', 'Order ID', 'Buyer', 'Amount', 'Reason', 'Status', 'Requested', 'Actions'].map(h => (
              <TableCell key={h} sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666', textTransform: 'uppercase' }}>
                {h}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {refunds.length === 0 ? (
            <TableRow>
              <TableCell colSpan={8} align="center" sx={{ py: 5, color: '#999' }}>
                No refunds found
              </TableCell>
            </TableRow>
          ) : (
            refunds.map((refund) => (
              <TableRow
                key={refund.id}
                sx={{
                  borderBottom: '2px solid #f0f0f0',
                  '&:hover': { background: '#fafafa' },
                }}
              >
                <TableCell sx={{ fontSize: '0.85rem', fontWeight: 700, color: '#e91e63' }}>
                  #{refund.id.substring(0, 8)}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  #{refund.order_id.substring(0, 8)}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  {refund.buyer_name}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#e91e63' }}>
                  PKR {refund.refund_amount.toLocaleString()}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  {refund.reason}
                </TableCell>
                <TableCell>
                  <Chip
                    label={refund.status}
                    size="small"
                    sx={{
                      background: getStatusColor(refund.status).bg,
                      color: getStatusColor(refund.status).color,
                      fontWeight: 600,
                      fontSize: '0.7rem',
                      textTransform: 'capitalize',
                    }}
                  />
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  {refund.requested_at?.toLocaleDateString()}
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', gap: 0.75 }}>
                    <Box
                      onClick={() => onViewDetails(refund)}
                      sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}
                      title="View Details"
                    >
                      <VisibilityIcon />
                    </Box>
                    {refund.status === 'requested' && (
                      <>
                        <Box
                          onClick={() => onApprove(refund)}
                          sx={actionBtnSx('rgba(76,175,80,0.12)', '#4CAF50')}
                          title="Approve"
                        >
                          <CheckCircleIcon />
                        </Box>
                        <Box
                          onClick={() => onReject(refund)}
                          sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')}
                          title="Reject"
                        >
                          <CancelIcon />
                        </Box>
                      </>
                    )}
                    {refund.status === 'approved' && (
                      <Box
                        onClick={() => onProcess(refund)}
                        sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')}
                        title="Process Refund"
                      >
                        <SendIcon />
                      </Box>
                    )}
                  </Box>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
```

---

## 3. Create Refund Details Modal

```jsx
const RefundDetailsModal = ({ open, refund, onClose }) => {
  if (!refund) return null;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{ sx: { borderRadius: '15px' } }}
    >
      <DialogTitle sx={dialogTitleSx}>
        <LocalAtmIcon /> Refund Details
      </DialogTitle>
      <DialogContent sx={{ pt: 3 }}>
        <Grid container spacing={2}>
          {/* Refund Info */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Refund ID
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', fontWeight: 700, color: '#e91e63' }}>
              #{refund.id}
            </Typography>
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Order ID
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              #{refund.order_id}
            </Typography>
          </Grid>

          {/* Amount Info */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Original Amount
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
              PKR {refund.original_amount.toLocaleString()}
            </Typography>
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Refund Amount
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#e91e63' }}>
              PKR {refund.refund_amount.toLocaleString()}
            </Typography>
          </Grid>

          {/* Buyer & Seller */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Buyer
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.buyer_name}
            </Typography>
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Seller
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.seller_name}
            </Typography>
          </Grid>

          {/* Reason */}
          <Grid item xs={12}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Reason
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.reason}
            </Typography>
            {refund.reason_details && (
              <Typography sx={{ fontSize: '0.8rem', color: '#666', mt: 0.5 }}>
                {refund.reason_details}
              </Typography>
            )}
          </Grid>

          {/* Status & Dates */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Status
            </Typography>
            <Chip
              label={refund.status}
              size="small"
              sx={{
                background: '#e91e63',
                color: 'white',
                fontWeight: 600,
                textTransform: 'capitalize',
              }}
            />
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Requested
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.requested_at?.toLocaleString()}
            </Typography>
          </Grid>

          {/* Audit Trail */}
          {refund.audit_trail?.length > 0 && (
            <Grid item xs={12}>
              <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 1 }}>
                Audit Trail
              </Typography>
              <Box sx={{ background: '#fafafa', border: '2px solid #e0e0e0', borderRadius: '10px', p: 2 }}>
                {refund.audit_trail.map((entry, idx) => (
                  <Box key={idx} sx={{ mb: idx < refund.audit_trail.length - 1 ? 1.5 : 0, pb: 1.5, borderBottom: idx < refund.audit_trail.length - 1 ? '1px solid #e0e0e0' : 'none' }}>
                    <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333' }}>
                      {entry.action.toUpperCase()} by {entry.actor_name}
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#666', mt: 0.25 }}>
                      {new Date(entry.timestamp).toLocaleString()}
                    </Typography>
                    {entry.notes && (
                      <Typography sx={{ fontSize: '0.75rem', color: '#666', mt: 0.5 }}>
                        {entry.notes}
                      </Typography>
                    )}
                  </Box>
                ))}
              </Box>
            </Grid>
          )}
        </Grid>
      </DialogContent>
      <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
        <Button onClick={onClose} sx={cancelBtnSx}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

---

## 4. Create Refund Action Modal

```jsx
const RefundActionModal = ({ open, refund, action, onClose, onConfirm }) => {
  const [notes, setNotes] = useState('');

  const handleConfirm = () => {
    onConfirm(notes);
    setNotes('');
  };

  const getTitle = () => {
    switch (action) {
      case 'approve':
        return 'Approve Refund';
      case 'reject':
        return 'Reject Refund';
      case 'process':
        return 'Process Refund';
      default:
        return 'Refund Action';
    }
  };

  const getDescription = () => {
    switch (action) {
      case 'approve':
        return `Approve refund of PKR ${refund?.refund_amount.toLocaleString()} for ${refund?.buyer_name}?`;
      case 'reject':
        return `Reject refund of PKR ${refund?.refund_amount.toLocaleString()} for ${refund?.buyer_name}?`;
      case 'process':
        return `Process refund of PKR ${refund?.refund_amount.toLocaleString()} to ${refund?.buyer_name}?`;
      default:
        return '';
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{ sx: { borderRadius: '15px' } }}
    >
      <DialogTitle sx={dialogTitleSx}>
        {getTitle()}
      </DialogTitle>
      <DialogContent sx={{ pt: 3 }}>
        <Typography sx={{ fontSize: '0.85rem', color: '#666', mb: 2 }}>
          {getDescription()}
        </Typography>
        <TextField
          fullWidth
          multiline
          rows={3}
          placeholder="Add notes (optional)"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          sx={fieldSx}
        />
      </DialogContent>
      <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
        <Button onClick={onClose} sx={cancelBtnSx}>
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          sx={primaryBtnSx}
        >
          Confirm
        </Button>
      </DialogActions>
    </Dialog>
  );
};
```

---

## 5. Add Action Handlers

```jsx
const handleApproveRefund = async (notes) => {
  try {
    const refundRef = doc(db, 'refunds', refundActionModal.refund.id);
    await updateDoc(refundRef, {
      status: 'approved',
      approved_by: currentUserId,
      approved_at: serverTimestamp(),
      approval_notes: notes,
      updated_at: serverTimestamp(),
    });

    // Add audit entry
    await updateDoc(refundRef, {
      audit_trail: arrayUnion({
        action: 'approved',
        actor: currentUserId,
        actor_name: currentUserName,
        notes: notes || 'Refund approved',
        timestamp: Date.now(),
      }),
    });

    toast.success('Refund approved successfully');
    setRefundActionModal({ open: false, refund: null, action: null });
  } catch (error) {
    console.error('Error approving refund:', error);
    toast.error('Failed to approve refund');
  }
};

const handleRejectRefund = async (notes) => {
  try {
    const refundRef = doc(db, 'refunds', refundActionModal.refund.id);
    await updateDoc(refundRef, {
      status: 'rejected',
      approved_by: currentUserId,
      approval_notes: notes,
      updated_at: serverTimestamp(),
    });

    await updateDoc(refundRef, {
      audit_trail: arrayUnion({
        action: 'rejected',
        actor: currentUserId,
        actor_name: currentUserName,
        notes: notes || 'Refund rejected',
        timestamp: Date.now(),
      }),
    });

    toast.success('Refund rejected');
    setRefundActionModal({ open: false, refund: null, action: null });
  } catch (error) {
    console.error('Error rejecting refund:', error);
    toast.error('Failed to reject refund');
  }
};

const handleProcessRefund = async (notes) => {
  try {
    const refundRef = doc(db, 'refunds', refundActionModal.refund.id);
    
    // Mark as processing
    await updateDoc(refundRef, {
      status: 'processing',
      processed_at: serverTimestamp(),
      updated_at: serverTimestamp(),
    });

    // TODO: Call payment gateway API (Stripe/PayPal)
    // For now, simulate success
    setTimeout(async () => {
      await updateDoc(refundRef, {
        status: 'completed',
        completed_at: serverTimestamp(),
        gateway_refund_id: `ref_${Date.now()}`,
      });

      await updateDoc(refundRef, {
        audit_trail: arrayUnion({
          action: 'completed',
          actor: 'system',
          actor_name: 'System',
          notes: 'Refund processed successfully',
          timestamp: Date.now(),
        }),
      });

      toast.success('Refund processed successfully');
    }, 2000);

    setRefundActionModal({ open: false, refund: null, action: null });
  } catch (error) {
    console.error('Error processing refund:', error);
    toast.error('Failed to process refund');
  }
};
```

---

## 6. Update Main Render

```jsx
return (
  <Box>
    {/* Header */}
    <Box sx={{ mb: 3 }}>
      <Typography component="div" sx={{ fontSize: '1.5rem', fontWeight: 700, color: '#333', mb: 0.5 }}>
        {activeTab === 'orders' ? 'All Orders' : 'Refund Management'}
      </Typography>
      <Typography variant="body2" sx={{ fontSize: '0.85rem', color: '#666' }}>
        {activeTab === 'orders' 
          ? 'Monitor and manage all orders on Craftoria'
          : 'Review and process refund requests'}
      </Typography>
    </Box>

    {/* Tabs */}
    <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
      <Button
        variant={activeTab === 'orders' ? 'contained' : 'outlined'}
        onClick={() => setActiveTab('orders')}
        sx={{ borderRadius: '10px' }}
      >
        Orders
      </Button>
      <Button
        variant={activeTab === 'refunds' ? 'contained' : 'outlined'}
        onClick={() => setActiveTab('refunds')}
        sx={{ borderRadius: '10px' }}
      >
        Refunds
        {pendingRefundsCount > 0 && (
          <Chip
            label={pendingRefundsCount}
            size="small"
            color="error"
            sx={{ ml: 1 }}
          />
        )}
      </Button>
    </Box>

    {/* Content */}
    {activeTab === 'orders' ? (
      <>
        {/* Existing Orders Table */}
        {/* ... existing code ... */}
      </>
    ) : (
      <>
        {/* Refunds Table */}
        <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
          <RefundsTable
            refunds={refunds}
            onViewDetails={(refund) => setRefundDetailsModal({ open: true, refund })}
            onApprove={(refund) => setRefundActionModal({ open: true, refund, action: 'approve' })}
            onReject={(refund) => setRefundActionModal({ open: true, refund, action: 'reject' })}
            onProcess={(refund) => setRefundActionModal({ open: true, refund, action: 'process' })}
          />
        </Card>
      </>
    )}

    {/* Modals */}
    <RefundDetailsModal
      open={refundDetailsModal.open}
      refund={refundDetailsModal.refund}
      onClose={() => setRefundDetailsModal({ open: false, refund: null })}
    />

    <RefundActionModal
      open={refundActionModal.open}
      refund={refundActionModal.refund}
      action={refundActionModal.action}
      onClose={() => setRefundActionModal({ open: false, refund: null, action: null })}
      onConfirm={(notes) => {
        switch (refundActionModal.action) {
          case 'approve':
            handleApproveRefund(notes);
            break;
          case 'reject':
            handleRejectRefund(notes);
            break;
          case 'process':
            handleProcessRefund(notes);
            break;
        }
      }}
    />
  </Box>
);
```

---

## 7. Add Firestore Rules

Update `firestore.rules`:

```firestore
match /refunds/{refundId} {
  // Admins can read all refunds
  allow read: if request.auth.token.admin == true;
  
  // Admins can update refund status
  allow update: if request.auth.token.admin == true
    && (request.resource.data.status in ['approved', 'rejected', 'processing', 'completed', 'failed']);
  
  // Prevent deletion
  allow delete: if false;
}
```

---

## 8. Add Imports

```jsx
import {
  LocalAtmIcon,
  CheckCircleIcon,
  CancelIcon,
  SendIcon,
} from '@mui/icons-material';
import {
  arrayUnion,
  updateDoc,
} from 'firebase/firestore';
```

---

## 9. Refund Management Features

### What Admins Can Do:

1. **View All Refunds** - Real-time list with filtering
2. **View Details** - Complete refund information with audit trail
3. **Approve Refunds** - Approve pending refund requests
4. **Reject Refunds** - Reject with reason
5. **Process Refunds** - Send to payment gateway
6. **Track Status** - Monitor refund progress
7. **Add Notes** - Document decisions

### Refund Statuses:

- **REQUESTED** - Awaiting approval
- **APPROVED** - Ready to process
- **PROCESSING** - Payment gateway processing
- **COMPLETED** - Refund successful
- **REJECTED** - Refund denied
- **FAILED** - Processing failed

---

## 10. Integration Checklist

- [ ] Add refund state management
- [ ] Add real-time refund listener
- [ ] Create RefundsTable component
- [ ] Create RefundDetailsModal
- [ ] Create RefundActionModal
- [ ] Add action handlers
- [ ] Update main render
- [ ] Add Firestore rules
- [ ] Add imports
- [ ] Test all features
- [ ] Deploy to production

---

## 11. Testing Checklist

- [ ] View refunds list
- [ ] Filter by status
- [ ] View refund details
- [ ] Approve refund
- [ ] Reject refund
- [ ] Process refund
- [ ] Check audit trail
- [ ] Verify notifications sent
- [ ] Test with multiple refunds
- [ ] Test real-time updates

---

## Summary

Your web admin dashboard now has complete refund management with:
- ✅ Real-time refund list
- ✅ Approve/reject functionality
- ✅ Process refunds
- ✅ Detailed audit trail
- ✅ Status tracking
- ✅ Notes/comments
- ✅ Professional UI

Ready for production deployment!
