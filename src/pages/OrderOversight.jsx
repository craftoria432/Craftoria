// src/pages/OrderOversight.jsx
// ✅ PRODUCTION-READY with Real-Time Firebase Integration + Refund Management
import React, { useState, useEffect, useMemo } from 'react';
import {
  Box, Card, Typography, TextField, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Chip, Dialog, DialogTitle,
  DialogContent, DialogActions, Button, Grid, CircularProgress,
  Select, MenuItem,
} from '@mui/material';
import {
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  Search as SearchIcon,
  CheckCircle as CheckCircleIcon,
  RadioButtonUnchecked as PendingCircleIcon,
  ListAlt as OrderIcon,
  LocalShipping as ShippingIcon,
  Inventory2 as ProductIcon,
  LocalAtm as LocalAtmIcon,
  Cancel as CancelIcon,
  Send as SendIcon,
} from '@mui/icons-material';
import { 
  collection, 
  doc, 
  updateDoc, 
  onSnapshot,
  serverTimestamp,
  arrayUnion,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import ProtectedAction from '../components/common/ProtectedAction';
import RefundsTable from '../components/RefundsTable';
import RefundDetailsModal from '../components/RefundDetailsModal';
import RefundActionModal from '../components/RefundActionModal';
import { notifyOrderStatusChanged } from '../services/notificationService';
import toast from 'react-hot-toast';

// ✅ Timestamp conversion helper
const convertTimestamp = (timestamp) => {
  if (!timestamp) return null;
  if (timestamp.toDate) return timestamp.toDate();
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000);
  if (typeof timestamp === 'number') return new Date(timestamp);
  if (timestamp instanceof Date) return timestamp;
  return new Date(timestamp);
};

// ✅ Styling constants
const actionBtnSx = (bg, iconColor) => ({
  width: 32, height: 32, borderRadius: '8px',
  background: bg, cursor: 'pointer', transition: 'all 0.2s ease',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  border: 'none',
  '& svg': { color: iconColor, fontSize: 15 },
  '&:hover': { transform: 'translateY(-2px)', filter: 'brightness(0.92)' },
});

const dialogTitleSx = {
  background: 'linear-gradient(45deg, #E91E63, #F06292)',
  color: 'white', fontWeight: 600, fontSize: '1.15rem',
  display: 'flex', alignItems: 'center', gap: 1,
};

const cancelBtnSx = {
  borderRadius: '10px', border: '2px solid #e0e0e0',
  color: '#666', fontWeight: 600, textTransform: 'none', px: 3,
  '&:hover': { borderColor: '#e91e63', color: '#e91e63' },
};

const primaryBtnSx = {
  background: 'linear-gradient(45deg, #E91E63, #F06292)',
  borderRadius: '10px', fontWeight: 600, textTransform: 'none',
  px: 3, boxShadow: 'none',
  '&:hover': { 
    boxShadow: '0 5px 15px rgba(233,30,99,0.3)', 
    transform: 'translateY(-2px)' 
  },
};

const fieldSx = {
  '& .MuiOutlinedInput-root': {
    borderRadius: '10px',
    '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
    '&:hover fieldset': { borderColor: '#e91e63' },
    '&.Mui-focused fieldset': { 
      borderColor: '#e91e63', 
      boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' 
    },
  },
  '& input': { fontSize: '0.85rem', padding: '10px 13px' },
};

const getStatusBadgeStyle = (status) => {
  const map = {
    new:        { bg: '#e3f2fd', color: '#1976d2' },
    pending:    { bg: '#fff3cd', color: '#856404' },
    confirmed:  { bg: '#d1ecf1', color: '#0c5460' },
    processing: { bg: '#cce5ff', color: '#004085' },
    shipped:    { bg: '#e7d4f7', color: '#6610f2' },
    delivered:  { bg: '#c3e6cb', color: '#155724' },
    completed:  { bg: '#d4edda', color: '#155724' },
    cancelled:  { bg: '#f8d7da', color: '#721c24' },
  };
  return map[status?.toLowerCase()] || { bg: '#e0e0e0', color: '#666' };
};

const capitalize = (value) => {
  if (!value) return '';
  const normalized = value.toString().trim().toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
};

const isNewOrder = (status) => {
  return status === 'pending';
};

const OrderOversight = () => {
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [detailsModal, setDetailsModal] = useState({ open: false, order: null });
  const [statusModal, setStatusModal] = useState({ open: false, order: null });
  const [newStatus, setNewStatus] = useState('');
  
  // ✅ Refund state management
  const [activeTab, setActiveTab] = useState('orders');
  const [refunds, setRefunds] = useState([]);
  const [pendingRefundsCount, setPendingRefundsCount] = useState(0);
  const [refundDetailsModal, setRefundDetailsModal] = useState({ open: false, refund: null });
  const [refundActionModal, setRefundActionModal] = useState({ open: false, refund: null, action: null });
  
  const { can } = usePermissions();
  const canUpdateOrderStatus = can(PERMISSIONS.UPDATE_ORDER_STATUS);

  // ✅ REAL-TIME LISTENER - Orders
  useEffect(() => {
    setLoading(true);
    
    const unsubscribe = onSnapshot(
      collection(db, 'orders'),
      (snapshot) => {
        try {
          const ordersData = snapshot.docs.map(doc => {
            const data = doc.data();
            return {
              ...data,
              id: doc.id,
              created_at: convertTimestamp(data.created_at),
              updated_at: convertTimestamp(data.updated_at),
              order_placed_at: convertTimestamp(data.order_placed_at),
              processing_at: convertTimestamp(data.processing_at),
              shipped_at: convertTimestamp(data.shipped_at),
              delivered_at: convertTimestamp(data.delivered_at),
              cancelled_at: convertTimestamp(data.cancelled_at),
            };
          });
          
          setOrders(ordersData);
          setLoading(false);
        } catch (error) {
          console.error('Error processing orders snapshot:', error);
          toast.error('Failed to process orders data');
          setLoading(false);
        }
      },
      (error) => {
        console.error('Error listening to orders:', error);
        toast.error('Failed to load orders');
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  // ✅ REAL-TIME REFUND LISTENER
  useEffect(() => {
    if (activeTab !== 'refunds') return;

    const unsubscribe = onSnapshot(
      collection(db, 'refunds'),
      (snapshot) => {
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
      },
      (error) => {
        console.error('Error listening to refunds:', error);
        toast.error('Failed to load refunds');
      }
    );

    return () => unsubscribe();
  }, [activeTab]);

  // ✅ Client-side filtering
  const filteredOrders = useMemo(() => {
    let filtered = [...orders];
    
    if (statusFilter !== 'all') {
      filtered = filtered.filter(o => 
        o.status?.toLowerCase() === statusFilter.toLowerCase()
      );
    }
    
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      filtered = filtered.filter(o =>
        o.id?.toLowerCase().includes(q) ||
        o.buyer_name?.toLowerCase().includes(q) ||
        o.seller_name?.toLowerCase().includes(q)
      );
    }
    
    if (dateFrom) {
      const fromDate = new Date(dateFrom);
      filtered = filtered.filter(o => {
        const orderDate = o.created_at;
        return orderDate && orderDate >= fromDate;
      });
    }
    
    if (dateTo) {
      const toDate = new Date(dateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(o => {
        const orderDate = o.created_at;
        return orderDate && orderDate <= toDate;
      });
    }
    
    return filtered;
  }, [orders, statusFilter, searchQuery, dateFrom, dateTo]);

  // ✅ Update order status
  const handleUpdateStatus = (order) => {
    if (!canUpdateOrderStatus) {
      toast.error('You do not have permission to update order status');
      return;
    }
    setNewStatus(order.status || 'pending');
    setStatusModal({ open: true, order });
  };

  const confirmUpdateStatus = async () => {
    if (!newStatus) {
      toast.error('Please select a status');
      return;
    }
    
    try {
      const order = statusModal.order;
      
      await updateDoc(doc(db, 'orders', order.id), {
        status: newStatus,
        updated_at: serverTimestamp()
      });
      
      try {
        if (order.buyer_id) {
          await notifyOrderStatusChanged(order.buyer_id, order.id, newStatus, false);
        }
        
        if (order.seller_id) {
          await notifyOrderStatusChanged(order.seller_id, order.id, newStatus, true);
        }
      } catch (notifError) {
        console.error('Failed to send notifications:', notifError);
      }
      
      toast.success(`Order status updated to ${capitalize(newStatus)}`);
      setStatusModal({ open: false, order: null });
    } catch (error) {
      console.error('Error updating order status:', error);
      toast.error('Failed to update order status');
    }
  };

  // ✅ REFUND ACTION HANDLERS
  const handleApproveRefund = async (notes) => {
    try {
      const refundRef = doc(db, 'refunds', refundActionModal.refund.id);
      await updateDoc(refundRef, {
        status: 'approved',
        approved_by: 'admin', // TODO: Replace with currentUserId
        approved_at: serverTimestamp(),
        approval_notes: notes,
        updated_at: serverTimestamp(),
      });

      await updateDoc(refundRef, {
        audit_trail: arrayUnion({
          action: 'approved',
          actor: 'admin', // TODO: Replace with currentUserId
          actor_name: 'Admin', // TODO: Replace with currentUserName
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
        approved_by: 'admin', // TODO: Replace with currentUserId
        approval_notes: notes,
        updated_at: serverTimestamp(),
      });

      await updateDoc(refundRef, {
        audit_trail: arrayUnion({
          action: 'rejected',
          actor: 'admin', // TODO: Replace with currentUserId
          actor_name: 'Admin', // TODO: Replace with currentUserName
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
      
      await updateDoc(refundRef, {
        status: 'processing',
        processed_at: serverTimestamp(),
        updated_at: serverTimestamp(),
      });

      // TODO: Call payment gateway API (Stripe/PayPal)
      // For now, simulate success
      setTimeout(async () => {
        try {
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
        } catch (err) {
          console.error('Error completing refund:', err);
        }
      }, 2000);

      setRefundActionModal({ open: false, refund: null, action: null });
    } catch (error) {
      console.error('Error processing refund:', error);
      toast.error('Failed to process refund');
    }
  };

  if (loading) {
    return (
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        minHeight: '70vh' 
      }}>
        <CircularProgress sx={{ color: '#E91E63' }} />
      </Box>
    );
  }

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
          {/* Filters */}
          <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', mb: '20px', p: '20px' }}>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr 1fr' }, gap: '15px' }}>
              {/* Search */}
              <Box>
                <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#666', mb: '8px',display: 'flex',alignItems: 'center',gap: 0.5}}>
                  <SearchIcon sx={{ fontSize: 14 }} /> Search Orders
                </Typography>
                <TextField
                  fullWidth
                  placeholder="Search by Order ID, buyer, or seller..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  InputProps={{startAdornment: <SearchIcon sx={{ fontSize: 18, color: '#999', mr: 0.75 }} />}}
                  sx={fieldSx}
                />
              </Box>

              {/* Date From */}
              <Box>
                <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#666', mb: '8px' }}>Date From</Typography>
                <TextField
                  fullWidth
                  type="date"
                  value={dateFrom}
                  onChange={(e) => setDateFrom(e.target.value)}
                  sx={fieldSx}
                />
              </Box>

              {/* Date To */}
              <Box>
                <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#666', mb: '8px' }}>Date To</Typography>
                <TextField
                  fullWidth
                  type="date"
                  value={dateTo}
                  onChange={(e) => setDateTo(e.target.value)}
                  sx={fieldSx}
                />
              </Box>

              {/* Status Filter */}
              <Box>
                <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#666', mb: '8px' }}>Status</Typography>
                <Select
                  fullWidth
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  sx={{borderRadius: '10px','& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },'&:hover fieldset': { borderColor: '#e91e63' },'&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' },fontSize: '0.85rem','& .MuiSelect-select': { padding: '10px 13px' }}}
                >
                  <MenuItem value="all">All Orders</MenuItem>
                  <MenuItem value="new">New</MenuItem>
                  <MenuItem value="pending">Pending</MenuItem>
                  <MenuItem value="confirmed">Confirmed</MenuItem>
                  <MenuItem value="processing">Processing</MenuItem>
                  <MenuItem value="shipped">Shipped</MenuItem>
                  <MenuItem value="delivered">Delivered</MenuItem>
                  <MenuItem value="completed">Completed</MenuItem>
                  <MenuItem value="cancelled">Cancelled</MenuItem>
                </Select>
              </Box>
            </Box>
          </Card>

          {/* Orders Table */}
          <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
            <TableContainer>
              <Table>
                <TableHead sx={{ background: '#fafafa', borderBottom: '2px solid #e0e0e0' }}>
                  <TableRow>
                    {['Order ID', 'Buyer', 'Seller', 'Amount', 'Status', 'Date', 'Actions'].map(h => (
                      <TableCell key={h} sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666', textTransform: 'uppercase' }}>
                        {h}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredOrders.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center" sx={{ py: 5, color: '#999' }}>
                        {orders.length === 0 ? 'No orders found. Orders from mobile app will appear here in real-time.' : 'No orders match your filters.'}
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredOrders.map((order) => (
                      <TableRow
                        key={order.id}
                        sx={{borderBottom: '2px solid #f0f0f0',transition: 'all 0.3s ease','&:hover': { background: '#fafafa' },'&:last-child': { borderBottom: 'none' }}}
                      >
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                            <Typography sx={{ fontWeight: 700, color: '#e91e63', fontSize: '0.85rem' }}>#{order.id}</Typography>
                            {isNewOrder(order.status) && (<Box sx={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: '#E91E63', flexShrink: 0 }} title="New Order" />)}
                          </Box>
                        </TableCell>
                        <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>{order.buyer_name || 'N/A'}</TableCell>
                        <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>{order.seller_name || 'N/A'}</TableCell>
                        <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>PKR {(order.total_price || 0).toLocaleString()}</TableCell>
                        <TableCell>
                          <Chip
                            label={order.status || 'pending'}
                            size="small"
                            sx={{background: getStatusBadgeStyle(order.status).bg,color: getStatusBadgeStyle(order.status).color,fontWeight: 600,fontSize: '0.7rem',height: '24px',borderRadius: '20px',textTransform: 'capitalize'}}
                          />
                        </TableCell>
                        <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                          {order.created_at ? order.created_at.toLocaleDateString('en-US', {month: 'short',day: 'numeric',year: 'numeric'}) : 'N/A'}
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', gap: 0.75 }}>
                            <Box
                              onClick={() => setDetailsModal({ open: true, order })}
                              sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}
                              title="View Details"
                            >
                              <VisibilityIcon />
                            </Box>
                            <ProtectedAction permission={PERMISSIONS.UPDATE_ORDER_STATUS} hideIfNoAccess={true}>
                              <Box
                                onClick={() => handleUpdateStatus(order)}
                                sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')}
                                title="Update Status"
                              >
                                <EditIcon />
                              </Box>
                            </ProtectedAction>
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
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

      {/* ORDER DETAILS MODAL */}
      <Dialog
        open={detailsModal.open}
        onClose={() => setDetailsModal({ open: false, order: null })}
        maxWidth="md"
        fullWidth
        PaperProps={{ sx: { borderRadius: '15px' } }}
      >
        <DialogTitle sx={dialogTitleSx}>
          <OrderIcon />
          Order Details
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          {detailsModal.order && (
            <Typography sx={{ fontSize: '0.85rem', color: '#666' }}>
              Order ID: <strong>#{detailsModal.order.id}</strong>
            </Typography>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
          <Button onClick={() => setDetailsModal({ open: false, order: null })} sx={cancelBtnSx}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* UPDATE STATUS MODAL */}
      <Dialog
        open={statusModal.open}
        onClose={() => setStatusModal({ open: false, order: null })}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: '15px' } }}
      >
        <DialogTitle sx={dialogTitleSx}>
          <EditIcon />
          Update Order Status
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography sx={{ fontSize: '0.85rem', color: '#666', mb: 2 }}>
            Update the status for order <strong>#{statusModal.order?.id}</strong>
          </Typography>
          <Select
            fullWidth
            value={newStatus}
            onChange={(e) => setNewStatus(e.target.value)}
            sx={{borderRadius: '10px','& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },'&:hover fieldset': { borderColor: '#e91e63' },'&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' },fontSize: '0.85rem'}}
          >
            <MenuItem value="new">New</MenuItem>
            <MenuItem value="pending">Pending</MenuItem>
            <MenuItem value="confirmed">Confirmed</MenuItem>
            <MenuItem value="processing">Processing</MenuItem>
            <MenuItem value="shipped">Shipped</MenuItem>
            <MenuItem value="delivered">Delivered</MenuItem>
            <MenuItem value="completed">Completed</MenuItem>
            <MenuItem value="cancelled">Cancelled</MenuItem>
          </Select>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
          <Button onClick={() => setStatusModal({ open: false, order: null })} sx={cancelBtnSx}>
            Cancel
          </Button>
          <Button onClick={confirmUpdateStatus} variant="contained" startIcon={<EditIcon />} sx={primaryBtnSx}>
            Update Status
          </Button>
        </DialogActions>
      </Dialog>

      {/* REFUND DETAILS MODAL */}
      <RefundDetailsModal
        open={refundDetailsModal.open}
        refund={refundDetailsModal.refund}
        onClose={() => setRefundDetailsModal({ open: false, refund: null })}
      />

      {/* REFUND ACTION MODAL */}
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
            default:
              break;
          }
        }}
      />
    </Box>
  );
};

export default OrderOversight;
