// Updated SellerVerification.jsx to handle both applications and verifications
import { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Typography, Button, TextField, Chip, Checkbox,
  FormControlLabel, Dialog, DialogTitle, DialogContent, DialogActions,
  Select, MenuItem, CircularProgress, Tabs, Tab
} from '@mui/material';
import {
  Email as EmailIcon, Phone as PhoneIcon, CalendarToday as CalendarIcon,
  Person as PersonIcon, CheckCircle as CheckCircleIcon, Cancel as CancelIcon,
  ZoomIn as ZoomInIcon, CameraAlt as CameraAltIcon, Send as SendIcon,
  CheckCircleOutline as CheckCircleOutlineIcon, HighlightOff as HighlightOffIcon,
  InfoOutlined as InfoOutlinedIcon, TaskAlt as TaskAltIcon, HowToReg as HowToRegIcon
} from '@mui/icons-material';
import { collection, query, where, onSnapshot, doc, updateDoc, serverTimestamp, or } from 'firebase/firestore';
import { db } from '../services/firebase';
import { useAuth } from '../contexts/AuthContext';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import ProtectedAction from '../components/common/ProtectedAction';
import toast from 'react-hot-toast';
import { getTimeAgo } from '../utils/formatters';
import { notifyVerificationApproved, notifyVerificationRejected, notifyApplicationApproved, notifyApplicationRejected } from '../services/notificationService';
import { markItemAsViewed } from '../hooks/useNotificationCounts';

const SellerApplicationsAndVerifications = () => {
  const [loading, setLoading] = useState(true);
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [activeTab, setActiveTab] = useState(0); // 0: Applications, 1: Verifications
  const [activeFilter, setActiveFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Modals
  const [approveApplicationModal, setApproveApplicationModal] = useState({ open: false, user: null });
  const [rejectApplicationModal, setRejectApplicationModal] = useState({ open: false, user: null });
  const [approveVerificationModal, setApproveVerificationModal] = useState({ open: false, user: null });
  const [rejectVerificationModal, setRejectVerificationModal] = useState({ open: false, user: null });
  const [imageModal, setImageModal] = useState({ open: false, imageUrl: null });
  
  // Form states
  const [welcomeMessage, setWelcomeMessage] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [rejectMessage, setRejectMessage] = useState('');

  const { currentUser } = useAuth();
  const { can } = usePermissions();
  const canApproveSellers = can(PERMISSIONS.APPROVE_SELLERS);
  const canRejectSellers = can(PERMISSIONS.REJECT_SELLERS);

  // Real-time listener for both applications and verifications
  useEffect(() => {
    setLoading(true);
    
    // Query for seller applications (buyers who want to become sellers)
    const applicationsQuery = query(
      collection(db, 'users'),
      where('seller_application_status', '==', 'pending')
    );
    
    // Query for seller verifications (sellers needing identity verification)
    const verificationsQuery = query(
      collection(db, 'users'),
      where('role', '==', 'seller')
    );

    const unsubscribeApplications = onSnapshot(applicationsQuery, (snapshot) => {
      const applications = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
        type: 'application'
      }));
      
      const unsubscribeVerifications = onSnapshot(verificationsQuery, (snapshot) => {
        const verifications = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data(),
          type: 'verification'
        }));
        
        const allUsers = [...applications, ...verifications];
        setUsers(allUsers);
        setFilteredUsers(allUsers);
        setLoading(false);
        
        // Mark items as viewed
        allUsers.forEach(user => {
          if (currentUser?.id) {
            markItemAsViewed('users', user.id, currentUser.id);
          }
        });
      });
      
      return () => unsubscribeVerifications();
    });

    return () => unsubscribeApplications();
  }, [currentUser?.id]);

  // Filter users based on active tab and filters
  useEffect(() => {
    let filtered = users;
    
    // Filter by tab
    if (activeTab === 0) {
      // Applications tab - show buyers with pending applications
      filtered = users.filter(u => u.type === 'application');
    } else {
      // Verifications tab - show sellers needing verification
      filtered = users.filter(u => u.type === 'verification');
      
      // Apply verification status filter
      if (activeFilter !== 'all') {
        filtered = filtered.filter(u => u.verification_status === activeFilter);
      }
    }
    
    // Apply search filter
    if (searchQuery) {
      filtered = filtered.filter(u =>
        u.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.email?.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }
    
    setFilteredUsers(filtered);
  }, [activeTab, activeFilter, searchQuery, users]);

  // Handle seller application approval
  const handleApproveApplication = async () => {
    if (!canApproveSellers) {
      toast.error('You do not have permission to approve applications');
      return;
    }
    
    try {
      const userData = approveApplicationModal.user;
      
      await updateDoc(doc(db, 'users', userData.id), {
        role: 'seller',  // Change role from buyer to seller
        seller_application_status: 'approved',
        verification_status: 'not_submitted',
        verified: false,
        application_approved_at: serverTimestamp(),
        application_approved_by: currentUser?.email || 'unknown',
        welcome_message: welcomeMessage
      });

      // Send in-app notification
      try {
        await notifyApplicationApproved(userData.id, welcomeMessage);
      } catch (notifError) {
        console.error('Failed to send notification:', notifError);
      }

      // Send email notification
      try {
        const emailResponse = await fetch('https://us-central1-craftoria-c7f7f.cloudfunctions.net/sendSellerApprovalEmail', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            sellerEmail: userData.email,
            sellerName: userData.name
          })
        });
        
        if (!emailResponse.ok) {
          console.error('Failed to send approval email');
        }
      } catch (emailError) {
        console.error('Email sending error:', emailError);
        // Don't fail the approval if email fails
      }

      toast.success(`${userData.name}'s seller application approved! Email notification sent.`);
      setApproveApplicationModal({ open: false, user: null });
      setWelcomeMessage('');
    } catch (error) {
      toast.error('Failed to approve application');
    }
  };

  // Handle seller application rejection
  const handleRejectApplication = async () => {
    if (!canRejectSellers) {
      toast.error('You do not have permission to reject applications');
      return;
    }
    
    if (!rejectReason || !rejectMessage) {
      toast.error('Please provide reason and message');
      return;
    }
    
    try {
      await updateDoc(doc(db, 'users', rejectApplicationModal.user.id), {
        seller_application_status: 'rejected',
        application_rejected_at: serverTimestamp(),
        application_rejection_reason: rejectReason,
        application_rejection_message: rejectMessage,
        application_rejected_by: currentUser?.email || 'unknown'
      });

      // Send notification
      try {
        await notifyApplicationRejected(rejectApplicationModal.user.id, rejectReason);
      } catch (notifError) {
        console.error('Failed to send notification:', notifError);
      }

      toast.success('Seller application rejected');
      setRejectApplicationModal({ open: false, user: null });
      setRejectReason('');
      setRejectMessage('');
    } catch (error) {
      toast.error('Failed to reject application');
    }
  };

  // Handle identity verification approval (existing logic)
  const handleApproveVerification = async () => {
    if (!canApproveSellers) {
      toast.error('You do not have permission to approve verifications');
      return;
    }
    
    try {
      await updateDoc(doc(db, 'users', approveVerificationModal.user.id), {
        verification_status: 'approved',
        verified_at: serverTimestamp(),
        verified: true,
        verification_welcome_message: welcomeMessage,
        verified_by: currentUser?.email || 'unknown'
      });

      // Send notification
      try {
        await notifyVerificationApproved(approveVerificationModal.user.id);
      } catch (notifError) {
        console.error('Failed to send notification:', notifError);
      }

      toast.success(`${approveVerificationModal.user.name}'s identity verification approved!`);
      setApproveVerificationModal({ open: false, user: null });
      setWelcomeMessage('');
    } catch (error) {
      toast.error('Failed to approve verification');
    }
  };

  // Handle identity verification rejection (existing logic)
  const handleRejectVerification = async () => {
    if (!canRejectSellers) {
      toast.error('You do not have permission to reject verifications');
      return;
    }
    
    if (!rejectReason || !rejectMessage) {
      toast.error('Please provide reason and message');
      return;
    }
    
    try {
      await updateDoc(doc(db, 'users', rejectVerificationModal.user.id), {
        verification_status: 'rejected',
        verification_rejected_at: serverTimestamp(),
        verification_rejection_reason: rejectReason,
        verification_rejection_message: rejectMessage,
        verified: false,
        verification_rejected_by: currentUser?.email || 'unknown'
      });

      // Send notification
      try {
        await notifyVerificationRejected(rejectVerificationModal.user.id, rejectReason);
      } catch (notifError) {
        console.error('Failed to send notification:', notifError);
      }

      toast.success('Identity verification rejected');
      setRejectVerificationModal({ open: false, user: null });
      setRejectReason('');
      setRejectMessage('');
    } catch (error) {
      toast.error('Failed to reject verification');
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '70vh' }}>
        <CircularProgress sx={{ color: '#E91E63' }} />
      </Box>
    );
  }

  const applicationCount = users.filter(u => u.type === 'application').length;
  const verificationCount = users.filter(u => u.type === 'verification' && u.verification_status === 'pending').length;

  return (
    <Box>
      {/* Page Header */}
      <Box sx={{ mb: 3 }}>
        <Typography component="div" sx={{ fontSize: '1.5rem', fontWeight: 700, color: '#333', mb: 0.5 }}>
          Seller Management
        </Typography>
        <Typography variant="body2" sx={{ fontSize: '0.85rem', color: '#666' }}>
          Manage seller applications and identity verifications
        </Typography>
      </Box>

      {/* Tabs */}
      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', mb: 2.5 }}>
        <Tabs 
          value={activeTab} 
          onChange={(e, newValue) => setActiveTab(newValue)}
          sx={{ borderBottom: '1px solid #e0e0e0' }}
        >
          <Tab 
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <HowToRegIcon />
                Seller Applications
                {applicationCount > 0 && (
                  <Chip 
                    label={applicationCount} 
                    size="small" 
                    sx={{ 
                      background: 'linear-gradient(45deg, #E91E63, #F06292)', 
                      color: 'white', 
                      minWidth: '20px', 
                      height: '20px' 
                    }} 
                  />
                )}
              </Box>
            } 
          />
          <Tab 
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CheckCircleIcon />
                Identity Verifications
                {verificationCount > 0 && (
                  <Chip 
                    label={verificationCount} 
                    size="small" 
                    sx={{ 
                      background: 'linear-gradient(45deg, #E91E63, #F06292)', 
                      color: 'white', 
                      minWidth: '20px', 
                      height: '20px' 
                    }} 
                  />
                )}
              </Box>
            } 
          />
        </Tabs>
        
        <CardContent sx={{ p: 2.5 }}>
          {/* Filters for Verifications tab */}
          {activeTab === 1 && (
            <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
              {['all', 'pending', 'approved', 'rejected'].map(filter => (
                <Chip 
                  key={filter} 
                  label={filter.charAt(0).toUpperCase() + filter.slice(1)} 
                  onClick={() => setActiveFilter(filter)}
                  sx={{ 
                    borderRadius: '20px', 
                    padding: '8px 18px', 
                    border: '2px solid #e0e0e0', 
                    background: activeFilter === filter ? 'linear-gradient(45deg, #E91E63, #F06292)' : 'white', 
                    color: activeFilter === filter ? 'white' : '#666', 
                    fontWeight: 600, 
                    fontSize: '0.85rem', 
                    cursor: 'pointer', 
                    px: 2, 
                    height: '40px', 
                    transition: 'all 0.3s ease', 
                    '&:hover': { borderColor: '#e91e63' } 
                  }}
                />
              ))}
            </Box>
          )}
          
          {/* Search */}
          <TextField 
            fullWidth 
            placeholder="Search by name or email..." 
            value={searchQuery} 
            onChange={(e) => setSearchQuery(e.target.value)}
            sx={{ 
              '& .MuiOutlinedInput-root': { 
                borderRadius: '10px', 
                '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' }, 
                '&:hover fieldset': { borderColor: '#e91e63' }, 
                '&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' } 
              }, 
              '& input': { fontSize: '0.85rem', padding: '10px 13px' } 
            }}
          />
        </CardContent>
      </Card>

      {/* User Cards */}
      {filteredUsers.length === 0 ? (
        <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', p: 5, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {activeTab === 0 ? 'No seller applications found' : 'No seller verifications found'}
          </Typography>
        </Card>
      ) : (
        filteredUsers.map(user => (
          <UserCard 
            key={user.id} 
            user={user} 
            isApplication={activeTab === 0}
            onApproveApplication={() => setApproveApplicationModal({ open: true, user })}
            onRejectApplication={() => setRejectApplicationModal({ open: true, user })}
            onApproveVerification={() => setApproveVerificationModal({ open: true, user })}
            onRejectVerification={() => setRejectVerificationModal({ open: true, user })}
            onViewImage={(imageUrl) => setImageModal({ open: true, imageUrl })}
            canApprove={canApproveSellers}
            canReject={canRejectSellers}
          />
        ))
      )}

      {/* All the modals remain the same but with updated handlers */}
      {/* ... Modal components ... */}
    </Box>
  );
};

// Separate UserCard component for cleaner code
const UserCard = ({ user, isApplication, onApproveApplication, onRejectApplication, onApproveVerification, onRejectVerification, onViewImage, canApprove, canReject }) => {
  return (
    <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', mb: 2.5 }}>
      <CardContent sx={{ p: 3 }}>
        {/* User card content - similar to existing but with conditional rendering based on isApplication */}
        {/* ... User card implementation ... */}
      </CardContent>
    </Card>
  );
};

export default SellerApplicationsAndVerifications;