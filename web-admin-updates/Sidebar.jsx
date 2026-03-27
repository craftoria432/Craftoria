// src/components/layout/Sidebar.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText,
  Box, Badge, useMediaQuery, useTheme, Dialog, DialogTitle, DialogContent,
  DialogContentText, DialogActions, Button,
} from '@mui/material';
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded';
import VerifiedUserRoundedIcon from '@mui/icons-material/VerifiedUserRounded';
import Inventory2RoundedIcon from '@mui/icons-material/Inventory2Rounded';
import GroupRoundedIcon from '@mui/icons-material/GroupRounded';
import ListAltRoundedIcon from '@mui/icons-material/ListAltRounded';
import StoreRoundedIcon from '@mui/icons-material/StoreRounded';
import FlagRoundedIcon from '@mui/icons-material/FlagRounded';
import SchoolRoundedIcon from '@mui/icons-material/SchoolRounded';
import SettingsRoundedIcon from '@mui/icons-material/SettingsRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import { useAuth } from '../../contexts/AuthContext';
import { collection, query, where, getDocs, onSnapshot } from 'firebase/firestore';
import { db } from '../../services/firebase';
import toast from 'react-hot-toast';

const DRAWER_WIDTH = 260;

const Sidebar = ({ mobileOpen, onDrawerToggle }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const { logout } = useAuth();
  const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);
  const [pendingSellers, setPendingSellers] = useState(0);
  const [pendingReports, setPendingReports] = useState(0);

  useEffect(() => {
    const q = query(collection(db, 'users'), where('role', '==', 'seller'), where('verification_status', '==', 'pending'));
    const unsubscribe = onSnapshot(q, (snap) => setPendingSellers(snap.size), (err) => {
      console.error(err);
      getDocs(q).then((snap) => setPendingSellers(snap.size));
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    // Reports in Firestore use: "New" | "Under Review" | "Resolved"
    // Pending badge should count "New" items.
    const q = query(collection(db, 'reports'), where('status', '==', 'New'));
    const unsubscribe = onSnapshot(q, (snap) => setPendingReports(snap.size), (err) => {
      console.error(err);
      getDocs(q).then((snap) => setPendingReports(snap.size));
    });
    return () => unsubscribe();
  }, []);

  // ✅ Each icon has its own color - Professional order
  const menuItems = [
    {
      title: 'Dashboard',
      path: '/dashboard',
      icon: <DashboardRoundedIcon fontSize="small" />,
      color: '#5C6BC0',  // indigo
      emoji: '📊',
    },
    {
      title: 'Seller Verification',
      path: '/sellers',
      icon: <VerifiedUserRoundedIcon fontSize="small" />,
      color: '#43A047',  // green
      badge: pendingSellers > 0 ? pendingSellers : null,
      emoji: '✅',
    },
    {
      title: 'ML Kit Verification',
      path: '/seller-verification',
      icon: <VerifiedUserRoundedIcon fontSize="small" />,
      color: '#00BCD4',  // cyan
      badge: pendingSellers > 0 ? pendingSellers : null,
      emoji: '🔍',
    },
    {
      title: 'Product Management',
      path: '/products',
      icon: <Inventory2RoundedIcon fontSize="small" />,
      color: '#FB8C00',  // orange
      emoji: '📦',
    },
    {
      title: 'User Management',
      path: '/users',
      icon: <GroupRoundedIcon fontSize="small" />,
      color: '#039BE5',  // blue
      emoji: '👥',
    },
    {
      title: 'Order Oversight',
      path: '/orders',
      icon: <ListAltRoundedIcon fontSize="small" />,
      color: '#8E24AA',  // purple
      emoji: '📋',
    },
    {
      title: 'Co-Seller Stores',
      path: '/co-seller-stores',
      icon: <StoreRoundedIcon fontSize="small" />,
      color: '#E91E63',  // pink (brand)
      emoji: '🏪',
    },
    {
      title: 'Learning Resources',
      path: '/learning-resources',
      icon: <SchoolRoundedIcon fontSize="small" />,
      color: '#7B1FA2',  // deep purple
      emoji: '📚',
    },
    {
      title: 'Reports & Complaints',
      path: '/reports',
      icon: <FlagRoundedIcon fontSize="small" />,
      color: '#E53935',  // red
      badge: pendingReports > 0 ? pendingReports : null,
      emoji: '🚩',
    },
    {
      title: 'Settings',
      path: '/settings',
      icon: <SettingsRoundedIcon fontSize="small" />,
      color: '#546E7A',  // blue-grey
      emoji: '⚙️',
    },
  ];

  const handleNavigate = (path) => {
    navigate(path);
    if (isMobile && onDrawerToggle) onDrawerToggle();
  };

  const handleLogoutConfirm = async () => {
    try {
      await logout();
      toast.success('Logged out successfully');
      setLogoutDialogOpen(false);
      if (isMobile && onDrawerToggle) onDrawerToggle();
      navigate('/login', { replace: true });
    } catch (error) {
      console.error('Logout error:', error);
      toast.error('Failed to logout. Please try again.');
      setLogoutDialogOpen(false);
    }
  };

  const drawerContent = (
    <>
      <List sx={{ px: 0, py: 2.5, mt: '70px' }}>
        {menuItems.map((item) => {
          const isActive = location.pathname === item.path;
          const iconColor = isActive ? '#E91E63' : item.color;
          return (
            <ListItem
              key={item.path}
              disablePadding
              sx={{ borderLeft: '3px solid', borderLeftColor: isActive ? '#E91E63' : 'transparent' }}
            >
              <ListItemButton
                onClick={() => handleNavigate(item.path)}
                sx={{
                  py: 1.5, px: 3,
                  backgroundColor: isActive ? 'rgba(233,30,99,0.05)' : 'transparent',
                  '&:hover': {
                    backgroundColor: '#fafafa',
                    '& .nav-icon': { color: '#E91E63' },
                    '& .MuiListItemText-primary': { color: '#E91E63' },
                  },
                }}
              >
                {/* ✅ Colored icon pill background */}
                <Box
                  sx={{
                    width: 32, height: 32,
                    borderRadius: '8px',
                    backgroundColor: isActive
                      ? 'rgba(233,30,99,0.12)'
                      : `${item.color}18`,  // 18 = ~10% opacity hex
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    mr: 1.5, flexShrink: 0,
                    transition: 'background-color 0.2s',
                  }}
                >
                  <Box className="nav-icon" sx={{ color: iconColor, display: 'flex' }}>
                    {item.icon}
                  </Box>
                </Box>
                <ListItemText
                  primary={item.title}
                  primaryTypographyProps={{
                    fontSize: '0.85rem',
                    fontWeight: isActive ? 600 : 500,
                    color: isActive ? '#E91E63' : '#555',
                  }}
                />
                {item.badge && (
                  <Badge
                    badgeContent={item.badge}
                    sx={{
                      '& .MuiBadge-badge': {
                        backgroundColor: '#f44336', color: 'white',
                        fontSize: '0.7rem', fontWeight: 700,
                        height: '20px', minWidth: '20px', borderRadius: '12px',
                      },
                    }}
                  />
                )}
              </ListItemButton>
            </ListItem>
          );
        })}

        {/* ✅ Logout — red icon with pill background */}
        <ListItem disablePadding sx={{ mt: 2.5, borderLeft: '3px solid transparent' }}>
          <ListItemButton
            onClick={() => setLogoutDialogOpen(true)}
            sx={{
              py: 1.5, px: 3,
              '&:hover': {
                backgroundColor: '#fafafa',
                '& .logout-icon, & .MuiListItemText-primary': { color: '#f44336' },
              },
            }}
          >
            <Box
              sx={{
                width: 32, height: 32, borderRadius: '8px',
                backgroundColor: 'rgba(244,67,54,0.10)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                mr: 1.5, flexShrink: 0,
              }}
            >
              <Box className="logout-icon" sx={{ color: '#f44336', display: 'flex' }}>
                <LogoutRoundedIcon fontSize="small" />
              </Box>
            </Box>
            <ListItemText
              primary="Logout"
              primaryTypographyProps={{ fontSize: '0.85rem', fontWeight: 500, color: '#f44336' }}
            />
          </ListItemButton>
        </ListItem>
      </List>

      {/* Logout Dialog */}
      <Dialog
        open={logoutDialogOpen}
        onClose={() => setLogoutDialogOpen(false)}
        PaperProps={{ sx: { borderRadius: '15px', minWidth: '320px' } }}
      >
        <DialogTitle sx={{ background: 'linear-gradient(45deg, #E91E63, #F06292)', color: 'white', fontWeight: 600, fontSize: '1.15rem' }}>
          Confirm Logout
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <DialogContentText sx={{ fontSize: '0.9rem', color: '#333' }}>
            Are you sure you want to logout? You'll need to sign in again to access the admin panel.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ p: 2.5, pt: 0 }}>
          <Button onClick={() => setLogoutDialogOpen(false)} sx={{ borderRadius: '10px', border: '2px solid #e0e0e0', color: '#666', fontWeight: 600, textTransform: 'none', px: 3, '&:hover': { borderColor: '#e91e63', color: '#e91e63' } }}>
            Cancel
          </Button>
          <Button onClick={handleLogoutConfirm} variant="contained" sx={{ background: '#f44336', borderRadius: '10px', fontWeight: 600, textTransform: 'none', px: 3, boxShadow: 'none', '&:hover': { background: '#d32f2f', boxShadow: '0 5px 15px rgba(244,67,54,0.3)' } }}>
            Logout
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );

  const paperStyles = {
    width: DRAWER_WIDTH, boxSizing: 'border-box', backgroundColor: 'white',
    borderRight: '2px solid #f0f0f0', overflowY: 'auto',
    '&::-webkit-scrollbar': { width: '4px' },
    '&::-webkit-scrollbar-thumb': { background: '#F06292', borderRadius: '10px' },
  };

  return (
    <>
      {isMobile ? (
        <Drawer variant="temporary" open={mobileOpen} onClose={onDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: 'block', sm: 'none' }, '& .MuiDrawer-paper': paperStyles }}
        >
          {drawerContent}
        </Drawer>
      ) : (
        <Drawer variant="permanent"
          sx={{ display: { xs: 'none', sm: 'block' }, width: DRAWER_WIDTH, flexShrink: 0, '& .MuiDrawer-paper': paperStyles }}
        >
          {drawerContent}
        </Drawer>
      )}
    </>
  );
};

export default Sidebar;
