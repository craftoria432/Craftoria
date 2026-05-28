// src/pages/SellerVerification.jsx
import { useState, useEffect } from 'react';
import {
  Box, Card, CardContent, Typography, CircularProgress,
  Tabs, Tab, TextField,
} from '@mui/material';
import {
  HowToReg as HowToRegIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import {
  collection, query, where, onSnapshot, doc,
  updateDoc, serverTimestamp, getDocs,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { useAuth } from '../contexts/AuthContext';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import toast from 'react-hot-toast';
import {
  notifyVerificationApproved,
  notifyVerificationRejected,
  notifyApplicationApproved,
  notifyApplicationRejected,
} from '../services/notificationService';
import {
  sendApplicationApprovalEmail,
  sendVerificationApprovalEmail,
} from '../services/emailNotificationService';
import { deleteVerificationPhotoAfterReview } from '../services/verificationPhotoService';
import { logAdminAction } from '../services/adminAuditService';
import { markItemAsViewed } from '../hooks/useNotificationCounts';
import StatCard from '../components/common/StatCard';
import UserCard from '../components/seller/UserCard';
import {
  ApproveApplicationModal,
  RejectApplicationModal,
  ApproveVerificationModal,
  RejectVerificationModal,
  ImageModal,
} from '../components/seller/SellerModals';

const SellerApplicationsAndVerifications = () => {
  const [loading, setLoading] = useState(true);
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [activeTab, setActiveTab] = useState(0);
  const [activeFilter, setActiveFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [approveApplicationModal, setApproveApplicationModal] = useState({ open: false, user: null });
  const [rejectApplicationModal, setRejectApplicationModal] = useState({ open: false, user: null });
  const [approveVerificationModal, setApproveVerificationModal] = useState({ open: false, user: null });
  const [rejectVerificationModal, setRejectVerificationModal] = useState({ open: false, user: null });
  const [imageModal, setImageModal] = useState({ open: false, imageUrl: null });
  const [welcomeMessage, setWelcomeMessage] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [rejectMessage, setRejectMessage] = useState('');

  const { currentUser } = useAuth();
  const { can } = usePermissions();
  const canApproveSellers = can(PERMISSIONS.APPROVE_SELLERS);
  const canRejectSellers = can(PERMISSIONS.REJECT_SELLERS);

  useEffect(() => {
    setLoading(true);
    const applicationsQuery = query(
      collection(db, 'users'),
      where('seller_application_status', '==', 'pending')
    );
    const verificationsQuery = query(
      collection(db, 'users'),
      where('role', '==', 'seller')
    );

    const unsubscribeApplications = onSnapshot(applicationsQuery, (snapshot) => {
      const applications = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data(), type: 'application' }));

      const unsubscribeVerifications = onSnapshot(verificationsQuery, async (snapshot) => {
        const verificationDocs = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data(), type: 'verification' }));

        // Enrich verifications with ML Kit data
        const enrichedVerifications = await Promise.all(
          verificationDocs.map(async (verification) => {
            try {
              const sellerVerQuery = query(
                collection(db, 'seller_verifications'),
                where('userId', '==', verification.id)
              );
              const sellerVerDocs = await getDocs(sellerVerQuery);
              if (!sellerVerDocs.empty) {
                const mlKitData = sellerVerDocs.docs[0].data();
                return {
                  ...verification,
                  mlKitResult: mlKitData.mlKitResult || {
                    confidence: 0,
                    faceCount: 0,
                    isValid: false,
                    message: 'No ML Kit data available',
                  },
                  verificationImageUrl: mlKitData.imageUrl,  // ✅ Now contains Cloudinary URL
                  verification_photo_url: mlKitData.imageUrl,  // ✅ Add for UserCard compatibility
                };
              }
            } catch (error) {
              console.error('Error fetching ML Kit data:', error);
            }
            return {
              ...verification,
              mlKitResult: {
                confidence: 0,
                faceCount: 0,
                isValid: false,
                message: 'No ML Kit data available',
              },
            };
          })
        );

        const allUsers = [...applications, ...enrichedVerifications];
        setUsers(allUsers);
        setFilteredUsers(allUsers);
        setLoading(false);

        allUsers.forEach(user => {
          if (currentUser?.id) markItemAsViewed('users', user.id, currentUser.id);
        });
      });

      return () => unsubscribeVerifications();
    });

    return () => unsubscribeApplications();
  }, [currentUser?.id]);

  useEffect(() => {
    let filtered = users;
    if (activeTab === 0) {
      filtered = users.filter(u => u.type === 'application');
    } else {
      filtered = users.filter(u => u.type === 'verification');
      if (activeFilter !== 'all') {
        filtered = filtered.filter(u => u.verification_status === activeFilter);
      }
    }

    if (searchQuery) {
      filtered = filtered.filter(u =>
        u.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        u.email?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        String(u.phone || '').includes(searchQuery)
      );
    }

    setFilteredUsers(filtered);
  }, [activeTab, activeFilter, searchQuery, users]);

  const handleApproveApplication = async () => {
    if (!canApproveSellers) {
      toast.error('You do not have permission to approve applications');
      return;
    }

    try {
      await updateDoc(doc(db, 'users', approveApplicationModal.user.id), {
        role: 'seller',
        seller_application_status: 'approved',
        verification_status: 'not_submitted',
        verified: false,
        application_approved_at: serverTimestamp(),
        application_approved_by: currentUser?.email || 'unknown',
        welcome_message: welcomeMessage,
      });

      // Send in-app notification
      try {
        await notifyApplicationApproved(approveApplicationModal.user.id, welcomeMessage);
      } catch (notifError) {
        console.error('Failed to send in-app notification:', notifError);
      }

      // Send email notification
      try {
        const emailResult = await sendApplicationApprovalEmail(
          approveApplicationModal.user.email,
          approveApplicationModal.user.name,
          welcomeMessage
        );
        if (emailResult.success) {
          console.log('✅ Application approval email sent successfully');
        } else {
          console.warn('⚠️ Email failed but application approved:', emailResult.error);
        }
      } catch (emailError) {
        console.error('Failed to send email:', emailError);
      }

      toast.success(`${approveApplicationModal.user.name}'s seller application approved!`);
      setApproveApplicationModal({ open: false, user: null });
      setWelcomeMessage('');
    } catch (error) {
      toast.error('Failed to approve application');
      console.error(error);
    }
  };

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
        application_rejected_by: currentUser?.email || 'unknown',
      });

      try {
        await notifyApplicationRejected(rejectApplicationModal.user.id, rejectReason);
      } catch {}

      toast.success('Seller application rejected');
      setRejectApplicationModal({ open: false, user: null });
      setRejectReason('');
      setRejectMessage('');
    } catch (error) {
      toast.error('Failed to reject application');
      console.error(error);
    }
  };

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
        verified_by: currentUser?.email || 'unknown',
      });

      // Send in-app notification
      try {
        await notifyVerificationApproved(approveVerificationModal.user.id);
      } catch (notifError) {
        console.error('Failed to send in-app notification:', notifError);
      }

      // Send email notification
      try {
        const emailResult = await sendVerificationApprovalEmail(
          approveVerificationModal.user.email,
          approveVerificationModal.user.name,
          welcomeMessage
        );
        if (emailResult.success) {
          console.log('✅ Verification approval email sent successfully');
        } else {
          console.warn('⚠️ Email failed but verification approved:', emailResult.error);
        }
      } catch (emailError) {
        console.error('Failed to send email:', emailError);
      }

      toast.success(`${approveVerificationModal.user.name}'s identity verification approved!`);
      setApproveVerificationModal({ open: false, user: null });
      setWelcomeMessage('');
    } catch (error) {
      toast.error('Failed to approve verification');
      console.error(error);
    }
  };

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
        verification_rejected_by: currentUser?.email || 'unknown',
      });

      try {
        await notifyVerificationRejected(rejectVerificationModal.user.id, rejectReason);
      } catch {}

      toast.success('Identity verification rejected');
      setRejectVerificationModal({ open: false, user: null });
      setRejectReason('');
      setRejectMessage('');
    } catch (error) {
      toast.error('Failed to reject verification');
      console.error(error);
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

  // ML Kit stats for verification tab
  const verifications = users.filter(u => u.type === 'verification');
  const pendingVerifications = verifications.filter(u => u.verification_status === 'pending');
  const approvedVerifications = verifications.filter(u => u.verification_status === 'approved').length;
  const rejectedVerifications = verifications.filter(u => u.verification_status === 'rejected').length;

  const filterOptions = ['all', 'pending', 'approved', 'rejected'];

  return (
    <Box>
      {/* Page Header */}
      <Box sx={{ mb: '28px' }}>
        <Typography sx={{ fontSize: '1.35rem', fontWeight: 700, color: '#1a1d23', mb: '5px', letterSpacing: '-0.3px' }}>
          Seller Management
        </Typography>
        <Typography sx={{ fontSize: '0.82rem', color: '#8b919e' }}>
          Manage seller applications and identity verifications
        </Typography>
      </Box>

      {/* Stats Cards - Only on Verifications Tab */}
      {activeTab === 1 && (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' }, gap: '16px', mb: '24px' }}>
          <StatCard title="Pending Verifications" value={pendingVerifications.length} change={0} iconType="pending" />
          <StatCard title="Approved" value={approvedVerifications} change={0} iconType="approved" />
          <StatCard title="Rejected" value={rejectedVerifications} change={0} iconType="rejected" />
          <StatCard title="Total Verifications" value={verifications.length} change={0} iconType="verified" />
        </Box>
      )}

      {/* Tab + filter card */}
      <Card
        sx={{
          borderRadius: '12px',
          border: '1px solid #eef0f4',
          boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
          background: '#fff',
          mb: '20px',
        }}
      >
        {/* Tabs */}
        <Tabs
          value={activeTab}
          onChange={(_, v) => setActiveTab(v)}
          sx={{
            borderBottom: '1px solid #f0f2f5',
            px: '4px',
            '& .MuiTab-root': {
              textTransform: 'none',
              fontSize: '0.83rem',
              fontWeight: 600,
              color: '#8b919e',
              minHeight: '52px',
              py: 0,
              '&.Mui-selected': { color: '#E91E63' },
            },
            '& .MuiTabs-indicator': { background: '#E91E63', height: '2px', borderRadius: '1px' },
          }}
        >
          <Tab
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <HowToRegIcon sx={{ fontSize: 17 }} />
                Seller Applications
                {applicationCount > 0 && (
                  <Box
                    sx={{
                      minWidth: '20px',
                      height: '20px',
                      px: '6px',
                      borderRadius: '10px',
                      background: '#E91E63',
                      color: '#fff',
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {applicationCount}
                  </Box>
                )}
              </Box>
            }
          />
          <Tab
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <CheckCircleIcon sx={{ fontSize: 17 }} />
                Identity Verifications
                {verificationCount > 0 && (
                  <Box
                    sx={{
                      minWidth: '20px',
                      height: '20px',
                      px: '6px',
                      borderRadius: '10px',
                      background: '#E91E63',
                      color: '#fff',
                      fontSize: '0.65rem',
                      fontWeight: 700,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {verificationCount}
                  </Box>
                )}
              </Box>
            }
          />
        </Tabs>

        <CardContent sx={{ p: '18px 20px !important' }}>
          <Box sx={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
            {/* Filter chips — only on verifications tab */}
            {activeTab === 1 && (
              <Box sx={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                {filterOptions.map((f) => (
                  <Box
                    key={f}
                    onClick={() => setActiveFilter(f)}
                    sx={{
                      px: '14px',
                      py: '7px',
                      borderRadius: '20px',
                      border: '1px solid',
                      borderColor: activeFilter === f ? '#E91E63' : '#e2e8f0',
                      background: activeFilter === f ? '#E91E63' : '#fff',
                      color: activeFilter === f ? '#fff' : '#64748b',
                      fontSize: '0.78rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      userSelect: 'none',
                      '&:hover': { borderColor: '#E91E63' },
                    }}
                  >
                    {f.charAt(0).toUpperCase() + f.slice(1)}
                  </Box>
                ))}
              </Box>
            )}

            {/* Search */}
            <TextField
              placeholder="Search by name or email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              size="small"
              sx={{
                flex: 1,
                minWidth: '220px',
                '& .MuiOutlinedInput-root': {
                  borderRadius: '9px',
                  fontSize: '0.83rem',
                  background: '#fafbfc',
                  '& fieldset': { borderColor: '#e8eaed', borderWidth: '1px' },
                  '&:hover fieldset': { borderColor: '#E91E63' },
                  '&.Mui-focused fieldset': {
                    borderColor: '#E91E63',
                    borderWidth: '1.5px',
                    boxShadow: '0 0 0 3px rgba(233,30,99,0.08)',
                  },
                },
                '& input': { fontSize: '0.83rem', py: '9px' },
              }}
            />
          </Box>
        </CardContent>
      </Card>

      {/* User cards */}
      {filteredUsers.length === 0 ? (
        <Card
          sx={{
            borderRadius: '12px',
            border: '1px dashed #e2e8f0',
            boxShadow: 'none',
            background: '#fafbfc',
          }}
        >
          <CardContent sx={{ py: '48px', textAlign: 'center' }}>
            <Typography sx={{ fontSize: '0.83rem', color: '#8b919e' }}>
              {activeTab === 0 ? 'No seller applications found' : 'No seller verifications found'}
            </Typography>
          </CardContent>
        </Card>
      ) : (
        filteredUsers.map((user) => (
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

      {/* Modals */}
      <ApproveApplicationModal
        open={approveApplicationModal.open}
        user={approveApplicationModal.user}
        welcomeMessage={welcomeMessage}
        setWelcomeMessage={setWelcomeMessage}
        onClose={() => { setApproveApplicationModal({ open: false, user: null }); setWelcomeMessage(''); }}
        onConfirm={handleApproveApplication}
      />

      <RejectApplicationModal
        open={rejectApplicationModal.open}
        user={rejectApplicationModal.user}
        rejectReason={rejectReason}
        setRejectReason={setRejectReason}
        rejectMessage={rejectMessage}
        setRejectMessage={setRejectMessage}
        onClose={() => { setRejectApplicationModal({ open: false, user: null }); setRejectReason(''); setRejectMessage(''); }}
        onConfirm={handleRejectApplication}
      />

      <ApproveVerificationModal
        open={approveVerificationModal.open}
        user={approveVerificationModal.user}
        welcomeMessage={welcomeMessage}
        setWelcomeMessage={setWelcomeMessage}
        onClose={() => { setApproveVerificationModal({ open: false, user: null }); setWelcomeMessage(''); }}
        onConfirm={handleApproveVerification}
      />

      <RejectVerificationModal
        open={rejectVerificationModal.open}
        user={rejectVerificationModal.user}
        rejectReason={rejectReason}
        setRejectReason={setRejectReason}
        rejectMessage={rejectMessage}
        setRejectMessage={setRejectMessage}
        onClose={() => { setRejectVerificationModal({ open: false, user: null }); setRejectReason(''); setRejectMessage(''); }}
        onConfirm={handleRejectVerification}
      />

      <ImageModal
        open={imageModal.open}
        imageUrl={imageModal.imageUrl}
        onClose={() => setImageModal({ open: false, imageUrl: null })}
      />
    </Box>
  );
};

export default SellerApplicationsAndVerifications;
