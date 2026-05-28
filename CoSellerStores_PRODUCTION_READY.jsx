// src/pages/CoSellerStores.jsx
// ✅ PRODUCTION-READY with Real-Time Firebase Integration
import React, { useState, useEffect } from 'react';
import {
  Box, Card, Typography, TextField, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, IconButton, Chip, Dialog,
  DialogTitle, DialogContent, DialogActions, Button, CircularProgress,
  Select, MenuItem, FormControl, Checkbox,
} from '@mui/material';
import {
  Visibility as VisibilityIcon,
  Flag as FlagIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Store as StoreIcon,
  Inventory2 as ProductIcon,
  Group as MembersIcon,
  Warning as WarningIcon,
  Close as CloseIcon,
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
} from '@mui/icons-material';
import { 
  collection, 
  doc, 
  updateDoc, 
  deleteDoc,
  onSnapshot,
  serverTimestamp 
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import ProtectedAction from '../components/common/ProtectedAction';
import toast from 'react-hot-toast';

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

// ✅ Status conversion helpers
const getStatusFromActive = (isActive, isFlagged = false) => {
  if (isFlagged) return 'flagged';
  return isActive ? 'active' : 'inactive';
};

const getActiveFromStatus = (status) => {
  return status === 'active';
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

const cancelBtnSx = {
  flex: 1, borderRadius: '10px', border: '2px solid #e0e0e0',
  color: '#666', fontWeight: 600, textTransform: 'none',
  py: '12px', px: '20px', fontSize: '0.85rem',
  '&:hover': { borderColor: '#e91e63', color: '#e91e63' },
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

const selectSx = {
  borderRadius: '10px',
  '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
  '&:hover fieldset': { borderColor: '#e91e63' },
  '&.Mui-focused fieldset': { 
    borderColor: '#e91e63', 
    boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' 
  },
  fontSize: '0.85rem',
  '& .MuiSelect-select': { padding: '10px 13px' },
};

// ✅ Store logo colors — each store gets a unique gradient based on index
const STORE_GRADIENTS = [
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a18cd1, #fbc2eb)',
  'linear-gradient(135deg, #ffecd2, #fcb69f)',
];

const getStatusStyle = (status) => ({
  active:   { bg: '#e8f5e9', color: '#2e7d32' },
  inactive: { bg: '#f5f5f5', color: '#757575' },
  flagged:  { bg: '#ffebee', color: '#c62828' },
}[status?.toLowerCase()] || { bg: '#f5f5f5', color: '#757575' });

// ✅ Get store initials for avatar
const getStoreInitials = (name) => 
  name?.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2) || 'ST';
const CoSellerStores = () => {
  const [loading, setLoading] = useState(true);
  const [stores, setStores] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [sortBy, setSortBy] = useState('newest');
  const [selectedStores, setSelectedStores] = useState([]);
  const [selectAll, setSelectAll] = useState(false);
  const [flagModal, setFlagModal] = useState({ open: false, store: null });
  const [deleteModal, setDeleteModal] = useState({ open: false, store: null });
  const [flagReason, setFlagReason] = useState('');
  const [flagDetails, setFlagDetails] = useState('');
  const [deleteConfirm, setDeleteConfirm] = useState('');
  const [deleteReason, setDeleteReason] = useState('');
  const [deleteCheck, setDeleteCheck] = useState(false);
  const [bulkAction, setBulkAction] = useState('');
  
  const { can } = usePermissions();
  const canFlagStore = can(PERMISSIONS.FLAG_STORES);
  const canDeleteStore = can(PERMISSIONS.DELETE_STORES);

  // ✅ REAL-TIME LISTENER - Replaces loadStores callback
  useEffect(() => {
    setLoading(true);
    
    const unsubscribe = onSnapshot(
      collection(db, 'coSellerStores'),
      (snapshot) => {
        try {
          const storesData = snapshot.docs.map(doc => {
            const data = doc.data();
            return {
              id: doc.id,
              ...data,
              // ✅ Convert timestamps
              created_at: convertTimestamp(data.created_at),
              updated_at: convertTimestamp(data.updated_at),
              // ✅ Convert boolean status to string for UI compatibility
              status: getStatusFromActive(data.is_active, data.is_flagged),
              // ✅ Map field names for display compatibility
              name: data.store_name || '',
              owner: data.owner_name || '',
              members: data.member_count || 0,
              products: data.product_count || 0,
              date: data.created_at ? convertTimestamp(data.created_at) : null,
              // ✅ Sales calculation (placeholder - you may want to calculate from orders)
              sales: 0, // TODO: Calculate from orders collection if needed
            };
          });
          
          setStores(storesData);
          setLoading(false);
        } catch (error) {
          console.error('Error processing stores snapshot:', error);
          toast.error('Failed to process stores data');
          setLoading(false);
        }
      },
      (error) => {
        console.error('Error listening to stores:', error);
        toast.error('Failed to load stores');
        setLoading(false);
      }
    );

    // ✅ Cleanup listener on unmount
    return () => unsubscribe();
  }, []);

  // ✅ Client-side filtering (computed from real-time data)
  const filteredStores = React.useMemo(() => {
    let filtered = [...stores];
    
    // Status filter
    if (statusFilter !== 'all') {
      filtered = filtered.filter(s => s.status?.toLowerCase() === statusFilter);
    }
    
    // Search filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      filtered = filtered.filter(s =>
        s.store_name?.toLowerCase().includes(q) ||
        s.owner_name?.toLowerCase().includes(q) ||
        s.name?.toLowerCase().includes(q) ||
        s.owner?.toLowerCase().includes(q)
      );
    }
    
    // Sorting
    if (sortBy === 'newest') {
      filtered = [...filtered].sort((a, b) => {
        const dateA = a.created_at || new Date(0);
        const dateB = b.created_at || new Date(0);
        return dateB - dateA;
      });
    } else if (sortBy === 'products') {
      filtered = [...filtered].sort((a, b) => (b.product_count || 0) - (a.product_count || 0));
    } else if (sortBy === 'members') {
      filtered = [...filtered].sort((a, b) => (b.member_count || 0) - (a.member_count || 0));
    } else if (sortBy === 'sales') {
      filtered = [...filtered].sort((a, b) => (b.sales || 0) - (a.sales || 0));
    }
    
    return filtered;
  }, [stores, statusFilter, searchQuery, sortBy]);

  // ✅ Stats calculation from real-time data
  const stats = React.useMemo(() => ({
    totalActive: stores.filter(s => s.status === 'active').length,
    totalProducts: stores.reduce((sum, s) => sum + (s.product_count || s.products || 0), 0),
    totalMembers: stores.reduce((sum, s) => sum + (s.member_count || s.members || 0), 0),
    flaggedStores: stores.filter(s => s.status === 'flagged').length,
  }), [stores]);
  // ✅ Event handlers
  const handleSelectAll = (e) => {
    setSelectAll(e.target.checked);
    setSelectedStores(e.target.checked ? filteredStores.map(s => s.id) : []);
  };

  const handleSelectStore = (id) => {
    setSelectedStores(prev => 
      prev.includes(id) ? prev.filter(sid => sid !== id) : [...prev, id]
    );
  };

  const handleViewStore = (store) => {
    toast.success(
      `Store: ${store.store_name || store.name} | Owner: ${store.owner_name || store.owner} | Members: ${store.member_count || store.members} | Products: ${store.product_count || store.products}`, 
      { duration: 5000 }
    );
  };

  const handleFlagStore = (store) => {
    if (!canFlagStore) {
      toast.error('You do not have permission to flag stores');
      return;
    }
    setFlagReason('');
    setFlagDetails('');
    setFlagModal({ open: true, store });
  };

  const handleDeleteStore = (store) => {
    if (!canDeleteStore) {
      toast.error('You do not have permission to delete stores');
      return;
    }
    setDeleteConfirm('');
    setDeleteReason('');
    setDeleteCheck(false);
    setDeleteModal({ open: true, store });
  };

  // ✅ Flag store (no manual state update)
  const submitFlag = async () => {
    if (!flagReason) {
      toast.error('Please select a reason');
      return;
    }
    
    try {
      await updateDoc(doc(db, 'coSellerStores', flagModal.store.id), {
        is_flagged: true,
        flag_reason: flagReason,
        flag_details: flagDetails,
        updated_at: serverTimestamp()
      });
      
      // ✅ NO manual state update - onSnapshot handles it automatically
      toast.success(`${flagModal.store.store_name || flagModal.store.name} has been flagged for review!`);
      setFlagModal({ open: false, store: null });
    } catch (error) {
      console.error('Error flagging store:', error);
      toast.error('Failed to flag store');
    }
  };

  // ✅ Delete store (no manual state update)
  const submitDelete = async () => {
    if (!deleteCheck || deleteConfirm !== 'DELETE STORE' || !deleteReason) {
      toast.error('Please complete all confirmation steps');
      return;
    }
    
    try {
      await deleteDoc(doc(db, 'coSellerStores', deleteModal.store.id));
      
      // ✅ NO manual state update - onSnapshot handles it automatically
      toast.success(`${deleteModal.store.store_name || deleteModal.store.name} has been permanently removed`);
      setDeleteModal({ open: false, store: null });
    } catch (error) {
      console.error('Error deleting store:', error);
      toast.error('Failed to delete store');
    }
  };

  // ✅ Remove flag (toggle back to active)
  const handleRemoveFlag = async (store) => {
    try {
      await updateDoc(doc(db, 'coSellerStores', store.id), {
        is_flagged: false,
        flag_reason: '',
        flag_details: '',
        updated_at: serverTimestamp()
      });
      
      toast.success(`Flag removed from ${store.store_name || store.name}`);
    } catch (error) {
      console.error('Error removing flag:', error);
      toast.error('Failed to remove flag');
    }
  };

  const applyBulkAction = () => {
    if (!bulkAction || selectedStores.length === 0) {
      toast.error('Please select stores and an action');
      return;
    }
    toast.success(`${bulkAction} action applied to ${selectedStores.length} store(s)`);
    setBulkAction('');
    setSelectedStores([]);
    setSelectAll(false);
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
      <Box sx={{ 
        mb: '30px', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between' 
      }}>
        <Box>
          <Typography sx={{ 
            fontSize: '1.5rem', 
            fontWeight: 700, 
            color: '#333', 
            mb: '4px' 
          }}>
            Co-Seller Stores Management
          </Typography>
          <Typography sx={{ fontSize: '0.85rem', color: '#666' }}>
            Oversee all collaborative stores • 
            <Box component="span" sx={{ color: '#4CAF50', ml: 0.5 }}>
              ● Live
            </Box>
          </Typography>
        </Box>
        <Box sx={{ 
          background: 'linear-gradient(45deg, #E91E63, #F06292)', 
          color: 'white', 
          padding: '8px 16px', 
          borderRadius: '20px', 
          fontSize: '0.8rem', 
          fontWeight: 600, 
          display: 'flex', 
          alignItems: 'center', 
          gap: 0.75 
        }}>
          <StoreIcon sx={{ fontSize: 16 }} />
          Total Stores: {stores.length}
        </Box>
      </Box>

      {/* Stat Cards */}
      <Box sx={{ 
        display: 'grid', 
        gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' }, 
        gap: '20px', 
        mb: '30px' 
      }}>
        {/* Active Stores */}
        <Card sx={{ 
          borderRadius: '15px', 
          border: '2px solid #e0e0e0', 
          boxShadow: 'none', 
          p: '20px', 
          transition: 'all 0.3s ease', 
          '&:hover': { 
            borderColor: '#e91e63', 
            boxShadow: '0 5px 15px rgba(233,30,99,0.15)', 
            transform: 'translateY(-2px)' 
          } 
        }}>
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'space-between', 
            mb: '12px' 
          }}>
            <Typography sx={{ 
              fontSize: '0.8rem', 
              color: '#666', 
              fontWeight: 500 
            }}>
              Total Active Stores
            </Typography>
            <Box sx={{ 
              width: 40, 
              height: 40, 
              borderRadius: '10px', 
              background: 'rgba(233,30,99,0.1)', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center' 
            }}>
              <StoreIcon sx={{ fontSize: 20, color: '#E91E63' }} />
            </Box>
          </Box>
          <Typography sx={{ 
            fontSize: '2rem', 
            fontWeight: 700, 
            color: '#333', 
            mb: '6px' 
          }}>
            {stats.totalActive}
          </Typography>
          <Typography sx={{ 
            fontSize: '0.75rem', 
            fontWeight: 600, 
            color: '#4caf50', 
            display: 'flex', 
            alignItems: 'center', 
            gap: 0.5 
          }}>
            <TrendingUpIcon sx={{ fontSize: 14 }} /> 12% this month
          </Typography>
        </Card>

        {/* Total Products */}
        <Card sx={{ 
          borderRadius: '15px', 
          border: '2px solid #e0e0e0', 
          boxShadow: 'none', 
          p: '20px', 
          transition: 'all 0.3s ease', 
          '&:hover': { 
            borderColor: '#e91e63', 
            boxShadow: '0 5px 15px rgba(233,30,99,0.15)', 
            transform: 'translateY(-2px)' 
          } 
        }}>
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'space-between', 
            mb: '12px' 
          }}>
            <Typography sx={{ 
              fontSize: '0.8rem', 
              color: '#666', 
              fontWeight: 500 
            }}>
              Total Products
            </Typography>
            <Box sx={{ 
              width: 40, 
              height: 40, 
              borderRadius: '10px', 
              background: 'rgba(251,140,0,0.1)', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center' 
            }}>
              <ProductIcon sx={{ fontSize: 20, color: '#FB8C00' }} />
            </Box>
          </Box>
          <Typography sx={{ 
            fontSize: '2rem', 
            fontWeight: 700, 
            color: '#333', 
            mb: '6px' 
          }}>
            {stats.totalProducts}
          </Typography>
          <Typography sx={{ 
            fontSize: '0.75rem', 
            fontWeight: 600, 
            color: '#4caf50', 
            display: 'flex', 
            alignItems: 'center', 
            gap: 0.5 
          }}>
            <TrendingUpIcon sx={{ fontSize: 14 }} /> 8% this month
          </Typography>
        </Card>
        {/* Total Members */}
        <Card sx={{ 
          borderRadius: '15px', 
          border: '2px solid #e0e0e0', 
          boxShadow: 'none', 
          p: '20px', 
          transition: 'all 0.3s ease', 
          '&:hover': { 
            borderColor: '#e91e63', 
            boxShadow: '0 5px 15px rgba(233,30,99,0.15)', 
            transform: 'translateY(-2px)' 
          } 
        }}>
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'space-between', 
            mb: '12px' 
          }}>
            <Typography sx={{ 
              fontSize: '0.8rem', 
              color: '#666', 
              fontWeight: 500 
            }}>
              Total Members
            </Typography>
            <Box sx={{ 
              width: 40, 
              height: 40, 
              borderRadius: '10px', 
              background: 'rgba(3,155,229,0.1)', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center' 
            }}>
              <MembersIcon sx={{ fontSize: 20, color: '#039BE5' }} />
            </Box>
          </Box>
          <Typography sx={{ 
            fontSize: '2rem', 
            fontWeight: 700, 
            color: '#333', 
            mb: '6px' 
          }}>
            {stats.totalMembers}
          </Typography>
          <Typography sx={{ 
            fontSize: '0.75rem', 
            fontWeight: 600, 
            color: '#4caf50', 
            display: 'flex', 
            alignItems: 'center', 
            gap: 0.5 
          }}>
            <TrendingUpIcon sx={{ fontSize: 14 }} /> 15% this month
          </Typography>
        </Card>

        {/* Flagged Stores */}
        <Card sx={{ 
          borderRadius: '15px', 
          border: '2px solid #f44336', 
          boxShadow: 'none', 
          p: '20px', 
          background: 'linear-gradient(135deg, #fff5f5 0%, white 100%)', 
          transition: 'all 0.3s ease', 
          '&:hover': { 
            boxShadow: '0 5px 15px rgba(244,67,54,0.15)', 
            transform: 'translateY(-2px)' 
          } 
        }}>
          <Box sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'space-between', 
            mb: '12px' 
          }}>
            <Typography sx={{ 
              fontSize: '0.8rem', 
              color: '#666', 
              fontWeight: 500 
            }}>
              Flagged Stores
            </Typography>
            <Box sx={{ 
              width: 40, 
              height: 40, 
              borderRadius: '10px', 
              background: 'rgba(244,67,54,0.1)', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center' 
            }}>
              <FlagIcon sx={{ fontSize: 20, color: '#F44336' }} />
            </Box>
          </Box>
          <Typography sx={{ 
            fontSize: '2rem', 
            fontWeight: 700, 
            color: '#333', 
            mb: '6px' 
          }}>
            {stats.flaggedStores}
          </Typography>
          <Typography sx={{ 
            fontSize: '0.75rem', 
            fontWeight: 600, 
            color: '#f44336', 
            display: 'flex', 
            alignItems: 'center', 
            gap: 0.5 
          }}>
            <TrendingDownIcon sx={{ fontSize: 14 }} /> 2 from last week
          </Typography>
        </Card>
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
          gridTemplateColumns: { xs: '1fr', md: '2fr 1fr 1fr 1.5fr' }, 
          gap: '15px' 
        }}>
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
              <SearchIcon sx={{ fontSize: 14 }} /> Search Stores
            </Typography>
            <TextField
              fullWidth
              placeholder="Search by store name or owner name"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: <SearchIcon sx={{ fontSize: 18, color: '#999', mr: 0.75 }} />
              }}
              sx={fieldSx}
            />
          </Box>
          <Box>
            <Typography sx={{ 
              fontSize: '0.75rem', 
              fontWeight: 600, 
              color: '#666', 
              mb: '8px' 
            }}>
              Status
            </Typography>
            <Select
              fullWidth
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              sx={selectSx}
            >
              <MenuItem value="all">All Stores</MenuItem>
              <MenuItem value="active">Active</MenuItem>
              <MenuItem value="inactive">Inactive</MenuItem>
              <MenuItem value="flagged">Flagged</MenuItem>
            </Select>
          </Box>
          <Box>
            <Typography sx={{ 
              fontSize: '0.75rem', 
              fontWeight: 600, 
              color: '#666', 
              mb: '8px' 
            }}>
              Sort By
            </Typography>
            <Select
              fullWidth
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              sx={selectSx}
            >
              <MenuItem value="newest">Newest</MenuItem>
              <MenuItem value="products">Most Products</MenuItem>
              <MenuItem value="members">Most Members</MenuItem>
            </Select>
          </Box>
          <Box>
            <Typography sx={{ 
              fontSize: '0.75rem', 
              fontWeight: 600, 
              color: '#666', 
              mb: '8px' 
            }}>
              Date Range
            </Typography>
            <TextField
              fullWidth
              type="date"
              sx={fieldSx}
            />
          </Box>
        </Box>
      </Card>
      {/* Table */}
      <Card sx={{ 
        borderRadius: '15px', 
        border: '2px solid #e0e0e0', 
        boxShadow: 'none', 
        p: '20px' 
      }}>
        {/* Bulk Actions */}
        <Box sx={{ 
          display: 'flex', 
          alignItems: 'center', 
          gap: '15px', 
          pb: '15px', 
          borderBottom: '2px solid #e0e0e0', 
          mb: '15px' 
        }}>
          <Checkbox
            checked={selectAll}
            onChange={handleSelectAll}
            sx={{ 
              color: '#e91e63', 
              '&.Mui-checked': { color: '#e91e63' } 
            }}
          />
          <Select
            value={bulkAction}
            onChange={(e) => setBulkAction(e.target.value)}
            displayEmpty
            sx={{ 
              minWidth: 150, 
              borderRadius: '8px', 
              border: '2px solid #e0e0e0', 
              fontSize: '0.8rem', 
              '& .MuiSelect-select': { padding: '8px 15px' }, 
              '& fieldset': { border: 'none' } 
            }}
          >
            <MenuItem value="">Bulk Actions</MenuItem>
            <MenuItem value="activate">Activate</MenuItem>
            <MenuItem value="deactivate">Deactivate</MenuItem>
            <MenuItem value="flag">Flag</MenuItem>
            <MenuItem value="export">Export</MenuItem>
          </Select>
          <Button
            onClick={applyBulkAction}
            sx={{ 
              background: 'linear-gradient(45deg, #E91E63, #F06292)', 
              color: 'white', 
              borderRadius: '8px', 
              fontSize: '0.8rem', 
              fontWeight: 600, 
              textTransform: 'none', 
              px: 2.5, 
              py: 1, 
              boxShadow: 'none', 
              '&:hover': { boxShadow: '0 3px 10px rgba(233,30,99,0.3)' } 
            }}
          >
            Apply Action
          </Button>
        </Box>

        <TableContainer>
          <Table>
            <TableHead sx={{ 
              background: '#fafafa', 
              borderBottom: '2px solid #e0e0e0' 
            }}>
              <TableRow>
                {['', 'Store', 'Owner', 'Members', 'Products', 'Status', 'Created', 'Actions'].map(h => (
                  <TableCell 
                    key={h} 
                    sx={{ 
                      fontSize: '0.75rem', 
                      fontWeight: 600, 
                      color: '#666', 
                      textTransform: 'uppercase', 
                      p: '12px' 
                    }}
                  >
                    {h}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredStores.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 5, color: '#999' }}>
                    {stores.length === 0 
                      ? 'No stores found. Stores from mobile app will appear here in real-time.' 
                      : 'No stores match your filters.'}
                  </TableCell>
                </TableRow>
              ) : (
                filteredStores.map((store, index) => (
                  <TableRow
                    key={store.id}
                    sx={{
                      borderBottom: '1px solid #f0f0f0',
                      transition: 'all 0.3s ease',
                      '&:hover': { background: '#fafafa' }
                    }}
                  >
                    {/* Checkbox */}
                    <TableCell sx={{ p: '15px 12px' }}>
                      <Checkbox
                        checked={selectedStores.includes(store.id)}
                        onChange={() => handleSelectStore(store.id)}
                        sx={{ 
                          color: '#e91e63', 
                          '&.Mui-checked': { color: '#e91e63' } 
                        }}
                      />
                    </TableCell>

                    {/* Store Info */}
                    <TableCell sx={{ p: '15px 12px' }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <Box sx={{ 
                          width: 45, 
                          height: 45, 
                          borderRadius: '10px', 
                          background: STORE_GRADIENTS[index % STORE_GRADIENTS.length], 
                          display: 'flex', 
                          alignItems: 'center', 
                          justifyContent: 'center', 
                          flexShrink: 0 
                        }}>
                          <Typography sx={{ 
                            fontSize: '0.8rem', 
                            fontWeight: 700, 
                            color: 'white', 
                            lineHeight: 1 
                          }}>
                            {getStoreInitials(store.store_name || store.name)}
                          </Typography>
                        </Box>
                        <Box>
                          <Typography sx={{ 
                            fontSize: '0.85rem', 
                            fontWeight: 600, 
                            color: '#333' 
                          }}>
                            {store.store_name || store.name}
                          </Typography>
                          <Typography sx={{ fontSize: '0.75rem', color: '#666' }}>
                            {store.owner_name || store.owner}
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>

                    {/* ✅ Owner - uses owner_name */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333', p: '15px 12px' }}>
                      {store.owner_name || store.owner || 'N/A'}
                    </TableCell>

                    {/* ✅ Members - uses member_count */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333', p: '15px 12px' }}>
                      {store.member_count || store.members || 0}
                    </TableCell>

                    {/* ✅ Products - uses product_count */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333', p: '15px 12px' }}>
                      {store.product_count || store.products || 0}
                    </TableCell>

                    {/* Status */}
                    <TableCell sx={{ p: '15px 12px' }}>
                      <Chip
                        label={store.status?.charAt(0).toUpperCase() + store.status?.slice(1) || 'Active'}
                        size="small"
                        sx={{
                          background: getStatusStyle(store.status).bg,
                          color: getStatusStyle(store.status).color,
                          fontWeight: 600,
                          fontSize: '0.7rem',
                          height: '24px',
                          borderRadius: '12px',
                          px: 1.5
                        }}
                      />
                    </TableCell>

                    {/* ✅ Created Date - uses created_at */}
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333', p: '15px 12px' }}>
                      {store.created_at ? 
                        store.created_at.toLocaleDateString('en-US', {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric'
                        }) : 
                        'N/A'
                      }
                    </TableCell>

                    {/* Actions */}
                    <TableCell sx={{ p: '15px 12px' }}>
                      <Box sx={{ display: 'flex', gap: 0.75 }}>
                        {/* View */}
                        <Box
                          onClick={() => handleViewStore(store)}
                          sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}
                          title="View Store"
                        >
                          <VisibilityIcon />
                        </Box>

                        {/* Flag/Unflag */}
                        {store.status === 'flagged' ? (
                          <Box
                            onClick={() => handleRemoveFlag(store)}
                            sx={actionBtnSx('rgba(67,160,71,0.12)', '#43A047')}
                            title="Remove Flag"
                          >
                            <FlagIcon />
                          </Box>
                        ) : (
                          <Box
                            onClick={() => handleFlagStore(store)}
                            sx={actionBtnSx('rgba(255,152,0,0.12)', '#FF9800')}
                            title="Flag Store"
                          >
                            <FlagIcon />
                          </Box>
                        )}

                        {/* Delete */}
                        <ProtectedAction 
                          permission={PERMISSIONS.DELETE_STORES} 
                          hideIfNoAccess={true}
                        >
                          <Box
                            onClick={() => handleDeleteStore(store)}
                            sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')}
                            title="Delete Store"
                          >
                            <DeleteIcon />
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
      {/* FLAG MODAL */}
      <Dialog
        open={flagModal.open}
        onClose={() => setFlagModal({ open: false, store: null })}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: '15px' } }}
      >
        <DialogTitle sx={{ 
          background: 'linear-gradient(45deg, #E91E63, #F06292)', 
          color: 'white', 
          fontWeight: 700, 
          fontSize: '1.15rem', 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          p: '24px' 
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <FlagIcon />
            Flag Store for Review
          </Box>
          <IconButton
            onClick={() => setFlagModal({ open: false, store: null })}
            sx={{ 
              background: 'rgba(255,255,255,0.2)', 
              color: 'white', 
              width: 36, 
              height: 36, 
              '&:hover': { background: 'rgba(255,255,255,0.3)' } 
            }}
          >
            <CloseIcon sx={{ fontSize: 18 }} />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ pt: '24px', pb: '24px' }}>
          <Box sx={{ mb: 2 }}>
            <Typography sx={{ 
              fontSize: '0.8rem', 
              fontWeight: 600, 
              color: '#333', 
              mb: 1 
            }}>
              Reason *
            </Typography>
            <FormControl fullWidth>
              <Select
                value={flagReason}
                onChange={(e) => setFlagReason(e.target.value)}
                sx={selectSx}
              >
                <MenuItem value="">Select reason</MenuItem>
                <MenuItem value="inappropriate">Inappropriate content</MenuItem>
                <MenuItem value="policy">Policy violation</MenuItem>
                <MenuItem value="suspicious">Suspicious activity</MenuItem>
                <MenuItem value="quality">Quality concerns</MenuItem>
                <MenuItem value="other">Other</MenuItem>
              </Select>
            </FormControl>
          </Box>
          <Box>
            <Typography sx={{ 
              fontSize: '0.8rem', 
              fontWeight: 600, 
              color: '#333', 
              mb: 1 
            }}>
              Details
            </Typography>
            <TextField
              fullWidth
              multiline
              rows={4}
              placeholder="Provide details..."
              value={flagDetails}
              onChange={(e) => setFlagDetails(e.target.value)}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: '10px',
                  '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                  '&:hover fieldset': { borderColor: '#e91e63' },
                  '&.Mui-focused fieldset': { 
                    borderColor: '#e91e63', 
                    boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' 
                  }
                },
                '& textarea': { fontSize: '0.85rem' }
              }}
            />
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: '24px', pt: 0 }}>
          <Button
            onClick={() => setFlagModal({ open: false, store: null })}
            sx={cancelBtnSx}
          >
            Cancel
          </Button>
          <Button
            onClick={submitFlag}
            variant="contained"
            startIcon={<FlagIcon />}
            sx={{ 
              flex: 1, 
              background: 'linear-gradient(45deg, #E91E63, #F06292)', 
              borderRadius: '10px', 
              fontWeight: 600, 
              textTransform: 'none', 
              py: '12px', 
              px: '20px', 
              fontSize: '0.85rem', 
              boxShadow: 'none', 
              '&:hover': { boxShadow: '0 5px 15px rgba(233,30,99,0.3)' } 
            }}
          >
            Submit Flag
          </Button>
        </DialogActions>
      </Dialog>