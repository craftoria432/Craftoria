// src/pages/Settings.jsx - PRODUCTION READY VERSION
import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  CircularProgress,
  TextField,
  Switch,
  FormControl,
  Select,
  MenuItem,
  Avatar,
  IconButton,
  Alert,
} from '@mui/material';
import {
  Settings as SettingsIcon,
  Group as GroupIcon,
  Delete as DeleteIcon,
  PersonAdd as PersonAddIcon,
  Save as SaveIcon,
  BarChart as BarChartIcon,
  AttachMoney as MoneyIcon,
  LocalOffer as TagIcon,
  Email as EmailIcon,
  Build as BuildIcon,
} from '@mui/icons-material';
import { 
  collection, 
  query, 
  onSnapshot, 
  doc, 
  updateDoc, 
  setDoc,
  serverTimestamp,
  where,
  getDocs
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { useAuth } from '../contexts/AuthContext';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS, getRoleName } from '../config/permissions';
import toast from 'react-hot-toast';

const Settings = () => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [admins, setAdmins] = useState([]);
  const { currentUser } = useAuth();
  const { can } = usePermissions();

  // System settings
  const [settingsDocId, setSettingsDocId] = useState(null);
  const [commissionRate, setCommissionRate] = useState(5);
  const [minPrice, setMinPrice] = useState(100);
  const [maxDiscount, setMaxDiscount] = useState(30);
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [maintenanceMode, setMaintenanceMode] = useState(false);

  // Modal states
  const [addAdminModal, setAddAdminModal] = useState(false);

  // Form states
  const [adminName, setAdminName] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminRole, setAdminRole] = useState('admin');

  // Check permissions
  const canEditSettings = can(PERMISSIONS.EDIT_SYSTEM_SETTINGS);
  const canManageAdmins = can(PERMISSIONS.MANAGE_ADMINS);
  const canViewSettings = can(PERMISSIONS.VIEW_SETTINGS);

  // ✅ REAL-TIME SETTINGS LISTENER
  useEffect(() => {
    const settingsQuery = query(collection(db, 'settings'));
    
    const unsubscribe = onSnapshot(
      settingsQuery,
      (snapshot) => {
        if (snapshot.docs.length > 0) {
          const settingsData = snapshot.docs[0].data();
          const docId = snapshot.docs[0].id;
          
          setSettingsDocId(docId);
          setCommissionRate(settingsData.commissionRate || 5);
          setMinPrice(settingsData.minPrice || 100);
          setMaxDiscount(settingsData.maxDiscount || 30);
          setEmailNotifications(settingsData.emailNotifications !== false);
          setMaintenanceMode(settingsData.maintenanceMode || false);
        } else {
          // Create default settings if none exist
          const defaultSettings = {
            commissionRate: 5,
            minPrice: 100,
            maxDiscount: 30,
            emailNotifications: true,
            maintenanceMode: false,
            createdAt: serverTimestamp(),
            updatedAt: serverTimestamp()
          };
          
          setDoc(doc(db, 'settings', 'global'), defaultSettings)
            .then(() => {
              setSettingsDocId('global');
              console.log('Default settings created');
            })
            .catch(error => {
              console.error('Error creating default settings:', error);
            });
        }
        setLoading(false);
      },
      (error) => {
        console.error('Error listening to settings:', error);
        toast.error('Failed to load settings');
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, []);

  // ✅ REAL-TIME ADMIN USERS LISTENER
  useEffect(() => {
    const usersQuery = query(
      collection(db, 'users'),
      where('role', 'in', ['super_admin', 'admin', 'moderator'])
    );

    const unsubscribe = onSnapshot(
      usersQuery,
      (snapshot) => {
        const adminUsers = snapshot.docs.map(doc => {
          const data = doc.data();
          return {
            id: doc.id,
            name: data.name || data.email?.split('@')[0] || 'Unknown',
            email: data.email || 'No email',
            role: data.role,
            avatar: (data.name || data.email || 'U')
              .split(' ')
              .map(n => n[0])
              .join('')
              .toUpperCase()
              .slice(0, 2),
          };
        });
        
        console.log('Admin users updated (real-time):', adminUsers);
        setAdmins(adminUsers);
      },
      (error) => {
        console.error('Error listening to admin users:', error);
        setAdmins([]);
      }
    );

    return () => unsubscribe();
  }, []);

  // ✅ SAVE SYSTEM SETTINGS TO FIRESTORE
  const handleSaveSystemSettings = async () => {
    if (!canEditSettings) {
      toast.error('You do not have permission to edit system settings');
      return;
    }

    if (maintenanceMode) {
      const confirmed = window.confirm(
        'Are you sure you want to enable Maintenance Mode? All users will be unable to access the platform.'
      );
      if (!confirmed) {
        setMaintenanceMode(false);
        return;
      }
    }

    try {
      setSaving(true);
      
      const settingsData = {
        commissionRate,
        minPrice,
        maxDiscount,
        emailNotifications,
        maintenanceMode,
        updatedAt: serverTimestamp(),
        updatedBy: currentUser?.email || 'unknown'
      };

      if (settingsDocId) {
        await updateDoc(doc(db, 'settings', settingsDocId), settingsData);
      } else {
        await setDoc(doc(db, 'settings', 'global'), {
          ...settingsData,
          createdAt: serverTimestamp()
        });
        setSettingsDocId('global');
      }

      toast.success('Settings saved successfully!');
    } catch (error) {
      console.error('Error saving settings:', error);
      toast.error('Failed to save settings: ' + error.message);
    } finally {
      setSaving(false);
    }
  };

  // ✅ ADD NEW ADMIN USER
  const handleAddAdmin = async () => {
    if (!canManageAdmins) {
      toast.error('You do not have permission to add admins');
      return;
    }

    if (!adminName || !adminEmail) {
      toast.error('Please fill in all required fields');
      return;
    }

    try {
      setSaving(true);

      // Check if user exists
      const usersQuery = query(
        collection(db, 'users'),
        where('email', '==', adminEmail.toLowerCase())
      );
      const snapshot = await getDocs(usersQuery);

      if (snapshot.empty) {
        toast.error('User not found. They must register first.');
        setSaving(false);
        return;
      }

      const userDoc = snapshot.docs[0];
      const userId = userDoc.id;

      // Update user role
      await updateDoc(doc(db, 'users', userId), {
        role: adminRole,
        name: adminName,
        updatedAt: serverTimestamp(),
        promotedBy: currentUser?.email || 'unknown'
      });

      toast.success(`${adminName} has been promoted to ${getRoleName(adminRole)}`);
      setAddAdminModal(false);
      setAdminName('');
      setAdminEmail('');
      setAdminRole('admin');
    } catch (error) {
      console.error('Error adding admin:', error);
      toast.error('Failed to add admin: ' + error.message);
    } finally {
      setSaving(false);
    }
  };

  // ✅ REMOVE ADMIN USER
  const handleRemoveAdmin = async (adminId) => {
    if (!canManageAdmins) {
      toast.error('You do not have permission to remove admins');
      return;
    }

    const admin = admins.find(a => a.id === adminId);
    if (!admin) return;

    if (admin.email === currentUser?.email) {
      toast.error('You cannot remove yourself from admin users');
      return;
    }

    const confirmed = window.confirm(
      `Remove ${admin.name} from admin users?\n\nThis user will lose all admin access immediately.`
    );

    if (confirmed) {
      try {
        setSaving(true);
        
        await updateDoc(doc(db, 'users', adminId), {
          role: 'buyer',
          updatedAt: serverTimestamp(),
          demotedBy: currentUser?.email || 'unknown'
        });

        toast.success(`${admin.name} has been removed from the admin panel`);
      } catch (error) {
        console.error('Error removing admin:', error);
        toast.error('Failed to remove admin: ' + error.message);
      } finally {
        setSaving(false);
      }
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '70vh' }}>
        <CircularProgress sx={{ color: '#E91E63' }} />
      </Box>
    );
  }

  if (!canViewSettings) {
    return (
      <Box sx={{ p: 4 }}>
        <Alert severity="error">
          <Typography sx={{ fontWeight: 600 }}>Access Denied</Typography>
          <Typography sx={{ fontSize: '0.85rem', mt: 1 }}>
            You do not have permission to view this page.
          </Typography>
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      {/* Page Header */}
      <Box sx={{ mb: 3.5 }}>
        <Typography sx={{ fontSize: '1.5rem', fontWeight: 700, color: '#333', mb: 0.5 }}>
          System Settings
        </Typography>
        <Typography sx={{ fontSize: '0.85rem', color: '#666' }}>
          Configure platform settings and manage admin users
        </Typography>
        
        {/* Current User Role Badge */}
        <Box sx={{ mt: 2, display: 'inline-flex', alignItems: 'center', gap: 1 }}>
          <Typography sx={{ fontSize: '0.75rem', color: '#999' }}>Your Role:</Typography>
          <Box
            sx={{
              px: 1.5,
              py: 0.5,
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #667eea, #764ba2)',
              fontSize: '0.75rem',
              fontWeight: 600,
              color: 'white',
            }}
          >
            {getRoleName(currentUser?.role)}
          </Box>
        </Box>
      </Box>

      {/* Settings Grid */}
      <Box sx={{ display: 'grid', gap: 2.5 }}>
        {/* System Configuration */}
        {canEditSettings ? (
          // SUPER ADMIN - Full Edit Access
          <Card
            sx={{
              borderRadius: '15px',
              border: '2px solid #e0e0e0',
              boxShadow: 'none',
              p: 3,
              transition: 'all 0.3s ease',
              '&:hover': {
                borderColor: '#e91e63',
                boxShadow: '0 5px 15px rgba(233,30,99,0.15)',
              },
            }}
          >
            {/* Card Header */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2.5 }}>
              <Box
                sx={{
                  width: 45,
                  height: 45,
                  borderRadius: '12px',
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <SettingsIcon sx={{ fontSize: 22, color: 'white' }} />
              </Box>
              <Typography sx={{ fontSize: '1.15rem', fontWeight: 700, color: '#333' }}>
                System Configuration
              </Typography>
            </Box>

            {/* Setting Items */}
            <Box>
              {/* Commission Rate */}
              <Box sx={{ py: 2, borderBottom: '2px solid #f0f0f0' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box>
                    <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
                      Platform Commission Rate
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#666', lineHeight: 1.4, mt: 0.5 }}>
                      Percentage taken from each transaction (currently {commissionRate}%)
                    </Typography>
                  </Box>
                  <TextField
                    type="number"
                    value={commissionRate}
                    onChange={(e) => setCommissionRate(Number(e.target.value))}
                    inputProps={{ min: 0, max: 100 }}
                    sx={{
                      width: 150,
                      '& .MuiOutlinedInput-root': {
                        height: 38,
                        borderRadius: '10px',
                        '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                        '&:hover fieldset': { borderColor: '#e91e63' },
                        '&.Mui-focused fieldset': {
                          borderColor: '#e91e63',
                          boxShadow: '0 0 0 3px rgba(233,30,99,0.1)',
                        },
                      },
                      '& input': { fontSize: '0.85rem', padding: '8px 12px' },
                    }}
                  />
                </Box>
              </Box>

              {/* Minimum Price */}
              <Box sx={{ py: 2, borderBottom: '2px solid #f0f0f0' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box>
                    <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
                      Minimum Product Price
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#666', lineHeight: 1.4, mt: 0.5 }}>
                      Lowest price allowed for product listings (PKR)
                    </Typography>
                  </Box>
                  <TextField
                    type="number"
                    value={minPrice}
                    onChange={(e) => setMinPrice(Number(e.target.value))}
                    inputProps={{ min: 0 }}
                    sx={{
                      width: 150,
                      '& .MuiOutlinedInput-root': {
                        height: 38,
                        borderRadius: '10px',
                        '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                        '&:hover fieldset': { borderColor: '#e91e63' },
                        '&.Mui-focused fieldset': {
                          borderColor: '#e91e63',
                          boxShadow: '0 0 0 3px rgba(233,30,99,0.1)',
                        },
                      },
                      '& input': { fontSize: '0.85rem', padding: '8px 12px' },
                    }}
                  />
                </Box>
              </Box>

              {/* Maximum Discount */}
              <Box sx={{ py: 2, borderBottom: '2px solid #f0f0f0' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box>
                    <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
                      Maximum Negotiation Discount
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#666', lineHeight: 1.4, mt: 0.5 }}>
                      Maximum discount percentage sellers can offer
                    </Typography>
                  </Box>
                  <TextField
                    type="number"
                    value={maxDiscount}
                    onChange={(e) => setMaxDiscount(Number(e.target.value))}
                    inputProps={{ min: 0, max: 100 }}
                    sx={{
                      width: 150,
                      '& .MuiOutlinedInput-root': {
                        height: 38,
                        borderRadius: '10px',
                        '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                        '&:hover fieldset': { borderColor: '#e91e63' },
                        '&.Mui-focused fieldset': {
                          borderColor: '#e91e63',
                          boxShadow: '0 0 0 3px rgba(233,30,99,0.1)',
                        },
                      },
                      '& input': { fontSize: '0.85rem', padding: '8px 12px' },
                    }}
                  />
                </Box>
              </Box>

              {/* Email Notifications */}
              <Box sx={{ py: 2, borderBottom: '2px solid #f0f0f0' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box>
                    <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
                      Email Notifications
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#666', lineHeight: 1.4, mt: 0.5 }}>
                      Send email notifications for important events
                    </Typography>
                  </Box>
                  <Switch
                    checked={emailNotifications}
                    onChange={(e) => setEmailNotifications(e.target.checked)}
                    sx={{
                      width: 56,
                      height: 28,
                      padding: 0,
                      '& .MuiSwitch-switchBase': {
                        padding: 0,
                        margin: '3px',
                        '&.Mui-checked': {
                          transform: 'translateX(28px)',
                          '& + .MuiSwitch-track': {
                            background: 'linear-gradient(45deg, #e91e63, #f06292)',
                            opacity: 1,
                          },
                        },
                      },
                      '& .MuiSwitch-thumb': { width: 22, height: 22 },
                      '& .MuiSwitch-track': {
                        borderRadius: 28,
                        backgroundColor: '#e0e0e0',
                        opacity: 1,
                      },
                    }}
                  />
                </Box>
              </Box>

              {/* Maintenance Mode */}
              <Box sx={{ py: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Box>
                    <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
                      Maintenance Mode
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#666', lineHeight: 1.4, mt: 0.5 }}>
                      Temporarily disable platform access for maintenance
                    </Typography>
                  </Box>
                  <Switch
                    checked={maintenanceMode}
                    onChange={(e) => setMaintenanceMode(e.target.checked)}
                    sx={{
                      width: 56,
                      height: 28,
                      padding: 0,
                      '& .MuiSwitch-switchBase': {
                        padding: 0,
                        margin: '3px',
                        '&.Mui-checked': {
                          transform: 'translateX(28px)',
                          '& + .MuiSwitch-track': {
                            background: 'linear-gradient(45deg, #e91e63, #f06292)',
                            opacity: 1,
                          },
                        },
                      },
                      '& .MuiSwitch-thumb': { width: 22, height: 22 },
                      '& .MuiSwitch-track': {
                        borderRadius: 28,
                        backgroundColor: '#e0e0e0',
                        opacity: 1,
                      },
                    }}
                  />
                </Box>
              </Box>
            </Box>

            {/* Save Button */}
            <Button
              onClick={handleSaveSystemSettings}
              disabled={saving}
              startIcon={saving ? <CircularProgress size={18} sx={{ color: 'white' }} /> : <SaveIcon />}
              sx={{
                background: 'linear-gradient(45deg, #E91E63, #F06292)',
                color: 'white',
                borderRadius: '10px',
                fontSize: '0.85rem',
                fontWeight: 600,
                textTransform: 'none',
                px: 3.5,
                py: 1.5,
                mt: 2.5,
                boxShadow: 'none',
                '&:hover': {
                  boxShadow: '0 5px 15px rgba(233,30,99,0.3)',
                  transform: 'translateY(-2px)',
                },
                '&:disabled': {
                  opacity: 0.7,
                  background: 'linear-gradient(45deg, #E91E63, #F06292)',
                },
              }}
            >
              {saving ? 'Saving...' : 'Save System Settings'}
            </Button>
          </Card>
        ) : (
          // ADMIN & MODERATOR - View Only
          <Card
            sx={{
              borderRadius: '15px',
              border: '2px solid #e0e0e0',
              boxShadow: 'none',
              p: 3,
              opacity: 0.7,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2.5 }}>
              <Box
                sx={{
                  width: 45,
                  height: 45,
                  borderRadius: '12px',
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <SettingsIcon sx={{ fontSize: 22, color: 'white' }} />
              </Box>
              <Typography sx={{ fontSize: '1.15rem', fontWeight: 700, color: '#333' }}>
                System Configuration
              </Typography>
            </Box>

            <Alert severity="info" sx={{ mb: 2 }}>
              <Typography sx={{ fontWeight: 600, fontSize: '0.85rem' }}>View Only Access</Typography>
              <Typography sx={{ fontSize: '0.8rem', mt: 0.5 }}>
                Only Super Admins can edit system settings. You can view current configuration below.
              </Typography>
            </Alert>

            <Box sx={{ opacity: 0.6, pointerEvents: 'none' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <BarChartIcon sx={{ fontSize: 16, color: '#999' }} />
                <Typography sx={{ fontSize: '0.85rem', color: '#666', fontWeight: 600 }}>
                  Platform Commission Rate: {commissionRate}%
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <MoneyIcon sx={{ fontSize: 16, color: '#999' }} />
                <Typography sx={{ fontSize: '0.85rem', color: '#666', fontWeight: 600 }}>
                  Minimum Product Price: PKR {minPrice}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <TagIcon sx={{ fontSize: 16, color: '#999' }} />
                <Typography sx={{ fontSize: '0.85rem', color: '#666', fontWeight: 600 }}>
                  Maximum Discount: {maxDiscount}%
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <EmailIcon sx={{ fontSize: 16, color: '#999' }} />
                <Typography sx={{ fontSize: '0.85rem', color: '#666', fontWeight: 600 }}>
                  Email Notifications: {emailNotifications ? 'Enabled' : 'Disabled'}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <BuildIcon sx={{ fontSize: 16, color: '#999' }} />
                <Typography sx={{ fontSize: '0.85rem', color: '#666', fontWeight: 600 }}>
                  Maintenance Mode: {maintenanceMode ? 'Enabled' : 'Disabled'}
                </Typography>
              </Box>
            </Box>
          </Card>
        )}

        {/* Admin Management */}
        {canManageAdmins ? (
          // SUPER ADMIN - Full Admin Management
          <Card
            sx={{
              borderRadius: '15px',
              border: '2px solid #e0e0e0',
              boxShadow: 'none',
              p: 3,
              transition: 'all 0.3s ease',
              '&:hover': {
                borderColor: '#e91e63',
                boxShadow: '0 5px 15px rgba(233,30,99,0.15)',
              },
            }}
          >
            {/* Card Header */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2.5 }}>
              <Box
                sx={{
                  width: 45,
                  height: 45,
                  borderRadius: '12px',
                  background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <GroupIcon sx={{ fontSize: 22, color: 'white' }} />
              </Box>
              <Box sx={{ flex: 1 }}>
                <Typography sx={{ fontSize: '1.15rem', fontWeight: 700, color: '#333' }}>
                  Admin Management
                </Typography>
                <Typography sx={{ fontSize: '0.75rem', color: '#666', mt: 0.5 }}>
                  {admins.length} admin user{admins.length !== 1 ? 's' : ''} currently active
                </Typography>
              </Box>
            </Box>

            {/* Admin List */}
            <Box sx={{ mt: 2 }}>
              {admins.length === 0 ? (
                <Box sx={{ textAlign: 'center', py: 3, color: '#999' }}>
                  <Typography sx={{ fontSize: '0.85rem' }}>No admin users found</Typography>
                </Box>
              ) : (
                admins.map((admin) => (
                  <Box
                    key={admin.id}
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      p: 1.5,
                      background: '#fafafa',
                      border: '2px solid #e0e0e0',
                      borderRadius: '10px',
                      mb: 1.25,
                    }}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                      <Avatar
                        sx={{
                          width: 40,
                          height: 40,
                          background: 'linear-gradient(135deg, #667eea, #764ba2)',
                          fontSize: '0.8rem',
                          fontWeight: 700,
                        }}
                      >
                        {admin.avatar}
                      </Avatar>
                      <Box sx={{ flex: 1 }}>
                        <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333', mb: 0.25 }}>
                          {admin.name}
                        </Typography>
                        <Typography sx={{ fontSize: '0.75rem', color: '#666' }}>{admin.email}</Typography>
                      </Box>
                      <Box
                        sx={{
                          px: 1.25,
                          py: 0.5,
                          borderRadius: '12px',
                          background: 'linear-gradient(135deg, #ffecd2, #fcb69f)',
                          fontSize: '0.7rem',
                          fontWeight: 600,
                          color: '#333',
                        }}
                      >
                        {getRoleName(admin.role)}
                      </Box>
                    </Box>
                    {admin.email !== currentUser?.email && (
                      <IconButton
                        size="small"
                        onClick={() => handleRemoveAdmin(admin.id)}
                        disabled={saving}
                        sx={{
                          width: 32,
                          height: 32,
                          borderRadius: '8px',
                          border: '2px solid #f44336',
                          color: '#f44336',
                          transition: 'all 0.3s ease',
                          '&:hover': {
                            background: '#f44336',
                            color: 'white',
                          },
                          '&:disabled': {
                            opacity: 0.5,
                          },
                        }}
                        title="Remove Admin"
                      >
                        <DeleteIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                    )}
                  </Box>
                ))
              )}
            </Box>

            {/* Add Admin Button */}
            <Button
              onClick={() => setAddAdminModal(true)}
              disabled={saving}
              startIcon={<PersonAddIcon />}
              sx={{
                width: '100%',
                background: 'white',
                border: '2px solid #e91e63',
                color: '#e91e63',
                borderRadius: '10px',
                fontSize: '0.8rem',
                fontWeight: 600,
                textTransform: 'none',
                py: 1.25,
                mt: 1.5,
                '&:hover': {
                  background: 'rgba(233,30,99,0.1)',
                },
                '&:disabled': {
                  opacity: 0.5,
                },
              }}
            >
              Add New Admin
            </Button>
          </Card>
        ) : (
          // ADMIN & MODERATOR - Hidden Section
          <Card
            sx={{
              borderRadius: '15px',
              border: '2px solid #e0e0e0',
              boxShadow: 'none',
              p: 3,
              opacity: 0.7,
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2.5 }}>
              <Box
                sx={{
                  width: 45,
                  height: 45,
                  borderRadius: '12px',
                  background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <GroupIcon sx={{ fontSize: 22, color: 'white' }} />
              </Box>
              <Typography sx={{ fontSize: '1.15rem', fontWeight: 700, color: '#333' }}>
                Admin Management
              </Typography>
            </Box>

            <Alert severity="warning">
              <Typography sx={{ fontWeight: 600, fontSize: '0.85rem' }}>
                Super Admin Access Required
              </Typography>
              <Typography sx={{ fontSize: '0.8rem', mt: 0.5 }}>
                Only Super Admins can view and manage admin users. Contact your Super Admin if you need assistance.
              </Typography>
            </Alert>
          </Card>
        )}
      </Box>

      {/* Add Admin Modal */}
      <Dialog
        open={addAdminModal}
        onClose={() => !saving && setAddAdminModal(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: '15px' } }}
      >
        <DialogTitle
          sx={{
            background: 'linear-gradient(45deg, #E91E63, #F06292)',
            color: 'white',
            fontWeight: 600,
            fontSize: '1.15rem',
            p: 2.5,
          }}
        >
          Add New Admin
        </DialogTitle>
        <DialogContent sx={{ pt: 3, pb: 3 }}>
          <Alert severity="info" sx={{ mb: 2 }}>
            <Typography sx={{ fontSize: '0.8rem' }}>
              The user must already be registered on Craftoria. Enter their email to promote them to admin.
            </Typography>
          </Alert>

          <Box sx={{ mb: 2 }}>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>
              Full Name
            </Typography>
            <TextField
              fullWidth
              placeholder="Enter admin name"
              value={adminName}
              onChange={(e) => setAdminName(e.target.value)}
              disabled={saving}
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 42,
                  borderRadius: '10px',
                  '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                  '&:hover fieldset': { borderColor: '#e91e63' },
                  '&.Mui-focused fieldset': {
                    borderColor: '#e91e63',
                    boxShadow: '0 0 0 3px rgba(233,30,99,0.1)',
                  },
                },
                '& input': { fontSize: '0.85rem', padding: '10px 13px' },
              }}
            />
          </Box>

          <Box sx={{ mb: 2 }}>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>
              Email Address
            </Typography>
            <TextField
              fullWidth
              type="email"
              placeholder="admin@craftoria.com"
              value={adminEmail}
              onChange={(e) => setAdminEmail(e.target.value)}
              disabled={saving}
              sx={{
                '& .MuiOutlinedInput-root': {
                  height: 42,
                  borderRadius: '10px',
                  '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                  '&:hover fieldset': { borderColor: '#e91e63' },
                  '&.Mui-focused fieldset': {
                    borderColor: '#e91e63',
                    boxShadow: '0 0 0 3px rgba(233,30,99,0.1)',
                  },
                },
                '& input': { fontSize: '0.85rem', padding: '10px 13px' },
              }}
            />
          </Box>

          <Box>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>
              Role
            </Typography>
            <FormControl fullWidth>
              <Select
                value={adminRole}
                onChange={(e) => setAdminRole(e.target.value)}
                disabled={saving}
                sx={{
                  height: 42,
                  borderRadius: '10px',
                  '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
                  '&:hover fieldset': { borderColor: '#e91e63' },
                  '&.Mui-focused fieldset': {
                    borderColor: '#e91e63',
                    boxShadow: '0 0 0 3px rgba(233,30,99,0.1)',
                  },
                  fontSize: '0.85rem',
                }}
              >
                <MenuItem value="super_admin">Super Admin</MenuItem>
                <MenuItem value="admin">Admin</MenuItem>
                <MenuItem value="moderator">Moderator</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0 }}>
          <Button
            onClick={() => setAddAdminModal(false)}
            disabled={saving}
            sx={{
              flex: 1,
              borderRadius: '10px',
              border: '2px solid #e0e0e0',
              color: '#666',
              fontWeight: 600,
              textTransform: 'none',
              py: 1.375,
              '&:hover': { borderColor: '#e91e63', color: '#e91e63' },
              '&:disabled': { opacity: 0.5 },
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleAddAdmin}
            disabled={saving}
            variant="contained"
            sx={{
              flex: 1,
              background: 'linear-gradient(45deg, #E91E63, #F06292)',
              borderRadius: '10px',
              fontWeight: 600,
              textTransform: 'none',
              py: 1.375,
              boxShadow: 'none',
              '&:hover': {
                boxShadow: '0 5px 15px rgba(233,30,99,0.3)',
                transform: 'translateY(-2px)',
              },
              '&:disabled': {
                opacity: 0.7,
                background: 'linear-gradient(45deg, #E91E63, #F06292)',
              },
            }}
          >
            {saving ? <CircularProgress size={20} sx={{ color: 'white' }} /> : 'Add Admin'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Settings;
