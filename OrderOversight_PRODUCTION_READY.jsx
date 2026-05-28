// src/pages/OrderOversight.jsx
// ✅ PRODUCTION-READY with Real-Time Firebase Integration
import React, { useState, useEffect } from 'react';
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
} from '@mui/icons-material';
import { 
  collection, 
  doc, 
  updateDoc, 
  onSnapshot,
  serverTimestamp 
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import ProtectedAction from '../components/common/ProtectedAction';
import toast from 'react-hot-toast';
import { notifyOrderStatusChanged } from '../services/notificationService';

// ✅ Timestamp conversion helper
const convertTimestamp = (timestamp) => {
  if (!timestamp) return null;
  
  // Firestore Timestamp object
  if (timestamp.toDate) return timestamp.toDate();
  
  // Firestore Timestamp as plain object
  if (timestamp.seconds) return new Date(timestamp.seconds * 1000);
  
  // Long timestamp (milliseconds) from mobile app
  if (typeof timestamp === 'number') return new Date(timestamp);
  
  // Already a Date object
  if (timestamp instanceof Date) return timestamp;
  
  // ISO string or other format
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
  
  const { can } = usePermissions();
  const canUpdateOrderStatus = can(PERMISSIONS.UPDATE_ORDER_STATUS);

  // ✅ REAL-TIME LISTENER - Replaces loadOrders callback
  useEffect(() => {
    setLoading(true);
    
    const unsubscribe = onSnapshot(
      collection(db, 'orders'),
      (snapshot) => {
        try {
          const ordersData = snapshot.docs.map(doc => {
            const data = doc.data();
            return {
              id: doc.id,
              ...data,
              // ✅ Convert timestamps
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

    // ✅ Cleanup listener on unmount
    return () => unsubscribe();
  }, []);

  // ✅ Client-side filtering (computed from real-time data)
  const filteredOrders = React.useMemo(() => {
    let filtered = [...orders];
    
    // Status filter
    if (statusFilter !== 'all') {
      filtered = filtered.filter(o => 
        o.status?.toLowerCase() === statusFilter.toLowerCase()
      );
    }
    
    // Search filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      filtered = filtered.filter(o =>
        o.id?.toLowerCase().includes(q) ||
        o.buyer_name?.toLowerCase().includes(q) ||
        o.seller_name?.toLowerCase().includes(q)
      );
    }
    
    // Date range filter
    if (dateFrom) {
      const fromDate = new Date(dateFrom);
      filtered = filtered.filter(o => {
        const orderDate = o.created_at;
        return orderDate && orderDate >= fromDate;
      });
    }
    
    if (dateTo) {
      const toDate = new Date(dateTo);
      toDate.setHours(23, 59, 59, 999); // End of day
      filtered = filtered.filter(o => {
        const orderDate = o.created_at;
        return orderDate && orderDate <= toDate;
      });
    }
    
    return filtered;
  }, [orders, statusFilter, searchQuery, dateFrom, dateTo]);

  // ✅ Update order status (no manual state update)
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
        updated_at: serverTimestamp() // ✅ Use serverTimestamp()
      });
      
      // Send notifications to both buyer and seller
      try {
        // Notify buyer about order status change
        if (order.buyer_id) {
          await notifyOrderStatusChanged(order.buyer_id, order.id, newStatus, false);
        }
        
        // Notify seller about order status change
        if (order.seller_id) {
          await notifyOrderStatusChanged(order.seller_id, order.id, newStatus, true);
        }
      } catch (notifError) {
        console.error('Failed to send notifications:', notifError);
        // Don't fail the status update if notifications fail
      }
      
      // ✅ NO manual state update - onSnapshot handles it automatically
      toast.success(`Order status updated to ${newStatus}`);
      setStatusModal({ open: false, order: null });
    } catch (error) {
      console.error('Error updating order status:', error);
      toast.error('Failed to update order status');
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
        <Typography sx={{ 
          fontSize: '1.5rem', 
          fontWeight: 700, 
          color: '#333', 
          mb: 0.5 
        }}>
          All Orders
          <Chip 
            label={orders.length} 
            size="small" 
            sx={{ ml: 1, background: '#E91E63', color: 'white' }} 
          />
        </Typography>
        <Typography variant="body2" sx={{ fontSize: '0.85rem', color: '#666' }}>
          Monitor and manage all orders on Craftoria • 
          <Box component="span" sx={{ color: '#4CAF50', ml: 0.5 }}>
            ● Live
          </Box>
        </Typography>
      </Box>

      {/* Filters */}
      <Card sx={{ 
        borderRadius: '15px', 
        border: '2px solid #e0e0e0', 
        boxShadow: 'none', 
        mb: '20px', 
        p: '20px' 
      }}>
        <Box sx={{ 
          display: 'grid', 
          gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr 1fr' }, 
          gap: '15px' 
        }}>
          {/* Search */}
          <Box>
            <Typography sx={{ 
              fontSize: '0.75rem', 
              fontWeight: 600, 
              color: '#666', 
              mb: '8px',
              display: 'flex',
              alignItems: 'center',
              gap: 0.5
            }}>
              <SearchIcon sx={{ fontSize: 14 }} /> Search Orders
            </Typography>
            <TextField
              fullWidth
              placeholder="Search by Order ID, buyer, or seller..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ fontSize: 18, color: '#999', mr: 0.75 }} />
              }}
              sx={fieldSx}
            />
          </Box>

          {/* Date From */}
          <Box>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#666', mb: '8px' }}>
              Date From
            </Typography>
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
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#666', mb: '8px' }}>
              Date To
            </Typography>
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
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#666', mb: '8px' }}>
              Status
            </Typography>
            <Select
              fullWidth
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              sx={{
                borderRadius: '10px',
                '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                '&:hover fieldset': { borderColor: '#e91e63' },
                '&.Mui-focused fieldset': { 
                  borderColor: '#e91e63', 
                  boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' 
                },
                fontSize: '0.85rem',
                '& .MuiSelect-select': { padding: '10px 13px' }
              }}
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

      {/* Table */}
      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ background: '#fafafa', borderBottom: '2px solid #e0e0e0' }}>
              <TableRow>
                {['Order ID', 'Buyer', 'Seller', 'Amount', 'Status', 'Date', 'Actions'].map(h => (
                  <TableCell 
                    key={h} 
                    sx={{ 
                      fontSize: '0.8rem', 
                      fontWeight: 600, 
                      color: '#666', 
                      textTransform: 'uppercase' 
                    }}
                  >
                    {h}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredOrders.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 5, color: '#999' }}>
                    {orders.length === 0 
                      ? 'No orders found. Orders from mobile app will appear here in real-time.' 
                      : 'No orders match your filters.'}
                  </TableCell>
                </TableRow>
              ) : (
                filteredOrders.map((order) => (
                  <TableRow
                    key={order.id}
                    sx={{
                      borderBottom: '2px solid #f0f0f0',
                      transition: 'all 0.3s ease',
                      '&:hover': { background: '#fafafa' },
                      '&:last-child': { borderBottom: 'none' }
                    }}
                  >
                    {/* Order ID */}
                    <TableCell>
                      <Typography sx={{ 
                        fontWeight: 700, 
                        color: '#e91e63', 
                        fontSize: '0.85rem' 
                      }}>
                        #{order.id}
                      </Typography>
                    </TableCell>

                    {/* ✅ Buyer - uses buyer_name */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                      {order.buyer_name || 'N/A'}
                    </TableCell>

                    {/* ✅ Seller - uses seller_name */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                      {order.seller_name || 'N/A'}
                    </TableCell>

                    {/* ✅ Amount - uses total_price */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                      PKR {(order.total_price || 0).toLocaleString()}
                    </TableCell>

                    {/* Status */}
                    <TableCell>
                      <Chip
                        label={order.status || 'pending'}
                        size="small"
                        sx={{
                          background: getStatusBadgeStyle(order.status).bg,
                          color: getStatusBadgeStyle(order.status).color,
                          fontWeight: 600,
                          fontSize: '0.7rem',
                          height: '24px',
                          borderRadius: '20px',
                          textTransform: 'capitalize'
                        }}
                      />
                    </TableCell>

                    {/* ✅ Date - uses created_at */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                      {order.created_at ? 
                        order.created_at.toLocaleDateString('en-US', {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric'
                        }) : 
                        'N/A'
                      }
                    </TableCell>

                    {/* Actions */}
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 0.75 }}>
                        {/* View */}
                        <Box
                          onClick={() => setDetailsModal({ open: true, order })}
                          sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}
                          title="View Details"
                        >
                          <VisibilityIcon />
                        </Box>

                        {/* Update Status */}
                        <ProtectedAction 
                          permission={PERMISSIONS.UPDATE_ORDER_STATUS} 
                          hideIfNoAccess={true}
                        >
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
            <>
              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={6}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 0.75 
                  }}>
                    Order ID
                  </Typography>
                  <Typography sx={{ 
                    fontSize: '0.85rem', 
                    fontWeight: 700, 
                    color: '#e91e63' 
                  }}>
                    #{detailsModal.order.id}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 0.75 
                  }}>
                    Order Date
                  </Typography>
                  <Typography sx={{ 
                    fontSize: '0.85rem', 
                    color: '#333', 
                    fontWeight: 500 
                  }}>
                    {detailsModal.order.created_at ? 
                      detailsModal.order.created_at.toLocaleDateString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      }) : 
                      'N/A'
                    }
                  </Typography>
                </Grid>
              </Grid>

              <Grid container spacing={2} sx={{ mb: 2 }}>
                <Grid item xs={6}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 0.75 
                  }}>
                    Status
                  </Typography>
                  <Chip
                    label={detailsModal.order.status || 'pending'}
                    size="small"
                    sx={{
                      background: getStatusBadgeStyle(detailsModal.order.status).bg,
                      color: getStatusBadgeStyle(detailsModal.order.status).color,
                      fontWeight: 600,
                      fontSize: '0.7rem',
                      textTransform: 'capitalize'
                    }}
                  />
                </Grid>
                <Grid item xs={6}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 0.75 
                  }}>
                    Total Amount
                  </Typography>
                  <Typography sx={{ 
                    fontSize: '0.85rem', 
                    color: '#e91e63', 
                    fontWeight: 700 
                  }}>
                    PKR {(detailsModal.order.total_price || 0).toLocaleString()}
                  </Typography>
                </Grid>
              </Grid>

              {/* Buyer Information */}
              <Box sx={{ mb: 1.5 }}>
                <Typography sx={{ 
                  fontSize: '0.75rem', 
                  fontWeight: 600, 
                  color: '#999', 
                  textTransform: 'uppercase', 
                  mb: 0.75 
                }}>
                  Buyer Information
                </Typography>
                <Typography sx={{ 
                  fontSize: '0.85rem', 
                  color: '#333', 
                  fontWeight: 500 
                }}>
                  <strong>{detailsModal.order.buyer_name || 'N/A'}</strong>
                  <br />
                  {detailsModal.order.buyer_phone || 'No phone'}
                </Typography>
              </Box>

              {/* Seller Information */}
              <Box sx={{ mb: 1.5 }}>
                <Typography sx={{ 
                  fontSize: '0.75rem', 
                  fontWeight: 600, 
                  color: '#999', 
                  textTransform: 'uppercase', 
                  mb: 0.75 
                }}>
                  Seller Information
                </Typography>
                <Typography sx={{ 
                  fontSize: '0.85rem', 
                  color: '#333', 
                  fontWeight: 500 
                }}>
                  <strong>{detailsModal.order.seller_name || 'N/A'}</strong>
                </Typography>
              </Box>

              {/* Products/Items */}
              {detailsModal.order.items?.length > 0 && (
                <Box sx={{ mb: 1.5 }}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 0.75 
                  }}>
                    Products
                  </Typography>
                  <Box sx={{ 
                    background: '#fafafa', 
                    border: '2px solid #e0e0e0', 
                    borderRadius: '10px', 
                    p: 2 
                  }}>
                    {detailsModal.order.items.map((item, index) => (
                      <Box
                        key={index}
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 1.5,
                          pb: index < detailsModal.order.items.length - 1 ? 1.25 : 0,
                          mb: index < detailsModal.order.items.length - 1 ? 1.25 : 0,
                          borderBottom: index < detailsModal.order.items.length - 1 
                            ? '1px solid #e0e0e0' 
                            : 'none'
                        }}
                      >
                        <Box sx={{ 
                          width: 50, 
                          height: 50, 
                          borderRadius: '10px', 
                          background: 'rgba(251,140,0,0.12)', 
                          display: 'flex', 
                          alignItems: 'center', 
                          justifyContent: 'center', 
                          flexShrink: 0 
                        }}>
                          <ProductIcon sx={{ fontSize: 24, color: '#FB8C00' }} />
                        </Box>
                        <Box sx={{ flex: 1 }}>
                          <Typography sx={{ 
                            fontSize: '0.85rem', 
                            fontWeight: 600, 
                            color: '#333', 
                            mb: 0.25 
                          }}>
                            {item.product_title || 'Unknown Product'}
                          </Typography>
                          <Typography sx={{ fontSize: '0.75rem', color: '#666' }}>
                            Qty: {item.quantity || 0} × PKR {(item.price || 0).toLocaleString()}
                          </Typography>
                        </Box>
                        <Typography sx={{ 
                          fontWeight: 700, 
                          color: '#333', 
                          fontSize: '0.85rem' 
                        }}>
                          PKR {((item.quantity || 0) * (item.price || 0)).toLocaleString()}
                        </Typography>
                      </Box>
                    ))}
                  </Box>
                </Box>
              )}

              {/* Shipping Address */}
              <Box sx={{ mb: 1.5 }}>
                <Typography sx={{ 
                  fontSize: '0.75rem', 
                  fontWeight: 600, 
                  color: '#999', 
                  textTransform: 'uppercase', 
                  mb: 0.75 
                }}>
                  Shipping Address
                </Typography>
                <Typography sx={{ 
                  fontSize: '0.85rem', 
                  color: '#333', 
                  fontWeight: 500 
                }}>
                  {detailsModal.order.shipping_address || 
                   detailsModal.order.full_address || 
                   'No address provided'}
                </Typography>
              </Box>

              {/* Payment & Delivery Info */}
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 0.75 
                  }}>
                    Payment Method
                  </Typography>
                  <Typography sx={{ 
                    fontSize: '0.85rem', 
                    color: '#333', 
                    fontWeight: 500 
                  }}>
                    {detailsModal.order.payment_method || 'Cash on Delivery'}
                  </Typography>
                </Grid>
                <Grid item xs={6}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 0.75 
                  }}>
                    Payment Status
                  </Typography>
                  {detailsModal.order.payment_method === 'Online Payment' ? (
                    <Chip
                      label="Paid"
                      size="small"
                      icon={<CheckCircleIcon sx={{ 
                        fontSize: '14px !important', 
                        color: '#155724 !important' 
                      }} />}
                      sx={{
                        background: '#d4edda',
                        color: '#155724',
                        fontWeight: 600,
                        fontSize: '0.7rem',
                        height: '24px',
                        borderRadius: '20px'
                      }}
                    />
                  ) : (
                    <Chip
                      label="Pending (COD)"
                      size="small"
                      icon={<ShippingIcon sx={{ 
                        fontSize: '14px !important', 
                        color: '#856404 !important' 
                      }} />}
                      sx={{
                        background: '#fff3cd',
                        color: '#856404',
                        fontWeight: 600,
                        fontSize: '0.7rem',
                        height: '24px',
                        borderRadius: '20px'
                      }}
                    />
                  )}
                </Grid>
              </Grid>

              {/* Timeline */}
              {detailsModal.order.timeline?.length > 0 && (
                <Box sx={{ mt: 2 }}>
                  <Typography sx={{ 
                    fontSize: '0.75rem', 
                    fontWeight: 600, 
                    color: '#999', 
                    textTransform: 'uppercase', 
                    mb: 1.5 
                  }}>
                    Order Timeline
                  </Typography>
                  <Box>
                    {detailsModal.order.timeline.map((item, index) => (
                      <Box
                        key={index}
                        sx={{
                          display: 'flex',
                          gap: 1.5,
                          mb: index < detailsModal.order.timeline.length - 1 ? 1.75 : 0,
                          position: 'relative'
                        }}
                      >
                        {index < detailsModal.order.timeline.length - 1 && (
                          <Box sx={{ 
                            position: 'absolute', 
                            left: '11px', 
                            top: '28px', 
                            width: '2px', 
                            height: 'calc(100% + 14px)', 
                            background: '#e0e0e0' 
                          }} />
                        )}
                        <Box sx={{ 
                          width: 24, 
                          height: 24, 
                          borderRadius: '50%', 
                          background: item.is_completed ? '#4caf50' : '#e0e0e0', 
                          display: 'flex', 
                          alignItems: 'center', 
                          justifyContent: 'center', 
                          flexShrink: 0, 
                          zIndex: 1 
                        }}>
                          {item.is_completed ? (
                            <CheckCircleIcon sx={{ fontSize: 16, color: 'white' }} />
                          ) : (
                            <PendingCircleIcon sx={{ fontSize: 16, color: '#999' }} />
                          )}
                        </Box>
                        <Box sx={{ flex: 1 }}>
                          <Typography sx={{ 
                            fontSize: '0.85rem', 
                            fontWeight: 600, 
                            color: '#333', 
                            mb: 0.25 
                          }}>
                            {item.title}
                          </Typography>
                          <Typography sx={{ fontSize: '0.75rem', color: '#666' }}>
                            {item.date}
                          </Typography>
                        </Box>
                      </Box>
                    ))}
                  </Box>
                </Box>
              )}
            </>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
          <Button
            onClick={() => setDetailsModal({ open: false, order: null })}
            sx={cancelBtnSx}
          >
            Close
          </Button>
          <ProtectedAction 
            permission={PERMISSIONS.UPDATE_ORDER_STATUS} 
            hideIfNoAccess={true}
          >
            <Button
              onClick={() => {
                setDetailsModal({ open: false, order: null });
                handleUpdateStatus(detailsModal.order);
              }}
              variant="contained"
              startIcon={<EditIcon />}
              sx={primaryBtnSx}
            >
              Update Status
            </Button>
          </ProtectedAction>
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
            sx={{
              borderRadius: '10px',
              '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
              '&:hover fieldset': { borderColor: '#e91e63' },
              '&.Mui-focused fieldset': { 
                borderColor: '#e91e63', 
                boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' 
              },
              fontSize: '0.85rem'
            }}
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
          <Button
            onClick={() => setStatusModal({ open: false, order: null })}
            sx={cancelBtnSx}
          >
            Cancel
          </Button>
          <Button
            onClick={confirmUpdateStatus}
            variant="contained"
            startIcon={<EditIcon />}
            sx={primaryBtnSx}
          >
            Update Status
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default OrderOversight;
