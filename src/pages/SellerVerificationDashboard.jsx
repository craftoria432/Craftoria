import { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  Chip,
  TextField,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Avatar,
  Grid,
  Alert,
  LinearProgress,
} from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  Schedule as ScheduleIcon,
  Image as ImageIcon,
  Verified as VerifiedIcon,
} from '@mui/icons-material';
import {
  collection,
  query,
  where,
  onSnapshot,
  doc,
  updateDoc,
  serverTimestamp,
  getDocs,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { useAuth } from '../contexts/AuthContext';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import toast from 'react-hot-toast';
import {
  notifyVerificationApproved,
  notifyVerificationRejected,
} from '../services/notificationService';
import {
  sendVerificationApprovalEmail,
} from '../services/emailNotificationService';

const SellerVerificationDashboard = () => {
  const [loading, setLoading] = useState(true);
  const [verifications, setVerifications] = useState([]);
  const [filteredVerifications, setFilteredVerifications] = useState([]);
  const [activeFilter, setActiveFilter] = useState('pending');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedVerification, setSelectedVerification] = useState(null);
  const [approveDialog, setApproveDialog] = useState(false);
  const [rejectDialog, setRejectDialog] = useState(false);
  const [imageDialog, setImageDialog] = useState(false);
  const [selectedImage, setSelectedImage] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectMessage, setRejectMessage] = useState('');
  const [approveMessage, setApproveMessage] = useState('');

  const { currentUser } = useAuth();
  const { can } = usePermissions();
  const canApproveSellers = can(PERMISSIONS.APPROVE_SELLERS);
  const canRejectSellers = can(PERMISSIONS.REJECT_SELLERS);

  // Helper function to get ML Kit confidence color
  const getConfidenceColor = (confidence) => {
    if (!confidence) return '#9E9E9E';
    if (confidence >= 80) return '#4CAF50'; // Green
    if (confidence >= 60) return '#FF9800'; // Orange
    return '#F44336'; // Red
  };

  // Fetch seller verifications
  useEffect(() => {
    setLoading(true);
    const verificationsQuery = query(
      collection(db, 'seller_verifications'),
      where('verificationStatus', '!=', null)
    );

    const unsubscribe = onSnapshot(verificationsQuery, async (snapshot) => {
      const verificationDocs = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      // Enrich with user data and ensure ML Kit data is properly formatted
      const enrichedVerifications = await Promise.all(
        verificationDocs.map(async (verification) => {
          try {
            const userDoc = await getDocs(
              query(
                collection(db, 'users'),
                where('__name__', '==', verification.userId)
              )
            );
            if (!userDoc.empty) {
              const userData = userDoc.docs[0].data();
              return {
                ...verification,
                userName: userData.name || 'Unknown',
                userEmail: userData.email || 'Unknown',
                userPhone: userData.phone || 'Unknown',
                // Ensure ML Kit result is properly structured
                mlKitResult: verification.mlKitResult || {
                  confidence: 0,
                  faceCount: 0,
                  isValid: false,
                  message: 'No ML Kit data available',
                },
              };
            }
          } catch (error) {
            console.error('Error fetching user data:', error);
          }
          return {
            ...verification,
            mlKitResult: verification.mlKitResult || {
              confidence: 0,
              faceCount: 0,
              isValid: false,
              message: 'No ML Kit data available',
            },
          };
        })
      );

      setVerifications(enrichedVerifications);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  // Filter verifications
  useEffect(() => {
    let filtered = verifications;

    if (activeFilter !== 'all') {
      filtered = filtered.filter(
        (v) => v.verificationStatus === activeFilter
      );
    }

    if (searchQuery) {
      filtered = filtered.filter(
        (v) =>
          v.userName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          v.userEmail?.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    setFilteredVerifications(filtered);
  }, [activeFilter, searchQuery, verifications]);

  const handleApproveVerification = async () => {
    if (!canApproveSellers) {
      toast.error('You do not have permission to approve verifications');
      return;
    }

    try {
      await updateDoc(
        doc(db, 'seller_verifications', selectedVerification.id),
        {
          verificationStatus: 'approved',
          verified_at: serverTimestamp(),
          verified: true,
          verification_welcome_message: approveMessage,
          verified_by: currentUser?.email || 'unknown',
        }
      );

      // Update user document
      await updateDoc(doc(db, 'users', selectedVerification.userId), {
        verification_status: 'approved',
        verified: true,
        verified_at: serverTimestamp(),
      });

      // Send in-app notification
      try {
        await notifyVerificationApproved(selectedVerification.userId);
      } catch (notifError) {
        console.error('Failed to send in-app notification:', notifError);
      }

      // Send email notification
      try {
        const emailResult = await sendVerificationApprovalEmail(
          selectedVerification.userEmail,
          selectedVerification.userName,
          approveMessage
        );
        if (emailResult.success) {
          console.log('✅ Verification approval email sent successfully');
        } else {
          console.warn('⚠️ Email failed but verification approved:', emailResult.error);
        }
      } catch (emailError) {
        console.error('Failed to send email:', emailError);
      }

      toast.success('Verification approved successfully!');
      setApproveDialog(false);
      setSelectedVerification(null);
      setApproveMessage('');
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
      await updateDoc(
        doc(db, 'seller_verifications', selectedVerification.id),
        {
          verificationStatus: 'rejected',
          verification_rejected_at: serverTimestamp(),
          verification_rejection_reason: rejectReason,
          verification_rejection_message: rejectMessage,
          verified: false,
          verification_rejected_by: currentUser?.email || 'unknown',
        }
      );

      // Update user document
      await updateDoc(doc(db, 'users', selectedVerification.userId), {
        verification_status: 'rejected',
        verified: false,
      });

      try {
        await notifyVerificationRejected(
          selectedVerification.userId,
          rejectReason
        );
      } catch {}

      toast.success('Verification rejected successfully!');
      setRejectDialog(false);
      setSelectedVerification(null);
      setRejectReason('');
      setRejectMessage('');
    } catch (error) {
      toast.error('Failed to reject verification');
      console.error(error);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'pending':
        return 'warning';
      case 'approved':
        return 'success';
      case 'rejected':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'approved':
        return <CheckCircleIcon sx={{ fontSize: 20 }} />;
      case 'rejected':
        return <CancelIcon sx={{ fontSize: 20 }} />;
      case 'pending':
        return <ScheduleIcon sx={{ fontSize: 20 }} />;
      default:
        return null;
    }
  };

  if (loading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '70vh',
        }}
      >
        <CircularProgress sx={{ color: '#E91E63' }} />
      </Box>
    );
  }

  const pendingCount = verifications.filter(
    (v) => v.verificationStatus === 'pending'
  ).length;
  const approvedCount = verifications.filter(
    (v) => v.verificationStatus === 'approved'
  ).length;
  const rejectedCount = verifications.filter(
    (v) => v.verificationStatus === 'rejected'
  ).length;

  const filterOptions = ['all', 'pending', 'approved', 'rejected'];

  return (
    <Box>
      {/* Page Header */}
      <Box sx={{ mb: '28px' }}>
        <Typography
          sx={{
            fontSize: '1.35rem',
            fontWeight: 700,
            color: '#1a1d23',
            mb: '5px',
            letterSpacing: '-0.3px',
          }}
        >
          Seller Identity Verification
        </Typography>
        <Typography sx={{ fontSize: '0.82rem', color: '#8b919e' }}>
          Review and manage seller identity verifications using ML Kit face
          detection
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={2} sx={{ mb: '20px' }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card
            sx={{
              borderRadius: '12px',
              border: '1px solid #eef0f4',
              boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
            }}
          >
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Pending
              </Typography>
              <Typography
                sx={{
                  fontSize: '2rem',
                  fontWeight: 700,
                  color: '#FF9800',
                }}
              >
                {pendingCount}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card
            sx={{
              borderRadius: '12px',
              border: '1px solid #eef0f4',
              boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
            }}
          >
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Approved
              </Typography>
              <Typography
                sx={{
                  fontSize: '2rem',
                  fontWeight: 700,
                  color: '#4CAF50',
                }}
              >
                {approvedCount}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card
            sx={{
              borderRadius: '12px',
              border: '1px solid #eef0f4',
              boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
            }}
          >
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Rejected
              </Typography>
              <Typography
                sx={{
                  fontSize: '2rem',
                  fontWeight: 700,
                  color: '#F44336',
                }}
              >
                {rejectedCount}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card
            sx={{
              borderRadius: '12px',
              border: '1px solid #eef0f4',
              boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
            }}
          >
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total
              </Typography>
              <Typography
                sx={{
                  fontSize: '2rem',
                  fontWeight: 700,
                  color: '#2196F3',
                }}
              >
                {verifications.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filter Card */}
      <Card
        sx={{
          borderRadius: '12px',
          border: '1px solid #eef0f4',
          boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
          background: '#fff',
          mb: '20px',
        }}
      >
        <CardContent sx={{ p: '18px 20px !important' }}>
          <Box
            sx={{
              display: 'flex',
              gap: '12px',
              flexWrap: 'wrap',
              alignItems: 'center',
            }}
          >
            {/* Filter chips */}
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
                    borderColor:
                      activeFilter === f ? '#E91E63' : '#e2e8f0',
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

      {/* Verifications List */}
      {filteredVerifications.length === 0 ? (
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
              No verifications found
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={2}>
          {filteredVerifications.map((verification) => (
            <Grid item xs={12} key={verification.id}>
              <Card
                sx={{
                  borderRadius: '12px',
                  border: '1px solid #eef0f4',
                  boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
                  '&:hover': {
                    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                  },
                }}
              >
                <CardContent>
                  <Box
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'flex-start',
                      gap: '16px',
                    }}
                  >
                    {/* User Info */}
                    <Box sx={{ flex: 1 }}>
                      <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '12px',
                          mb: '8px',
                        }}
                      >
                        <Avatar
                          sx={{
                            width: 40,
                            height: 40,
                            background: '#E91E63',
                          }}
                        >
                          {verification.userName
                            ?.charAt(0)
                            .toUpperCase()}
                        </Avatar>
                        <Box>
                          <Typography
                            sx={{
                              fontWeight: 600,
                              color: '#1a1d23',
                            }}
                          >
                            {verification.userName}
                          </Typography>
                          <Typography
                            sx={{
                              fontSize: '0.75rem',
                              color: '#8b919e',
                            }}
                          >
                            {verification.userEmail}
                          </Typography>
                        </Box>
                      </Box>

                      {/* Verification Details */}
                      <Box
                        sx={{
                          display: 'flex',
                          gap: '16px',
                          mt: '12px',
                          flexWrap: 'wrap',
                        }}
                      >
                        {/* ML Kit Confidence */}
                        <Box>
                          <Typography
                            sx={{
                              fontSize: '0.75rem',
                              color: '#8b919e',
                              mb: '4px',
                            }}
                          >
                            ML Kit Confidence
                          </Typography>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <Typography
                              sx={{
                                fontWeight: 600,
                                color: '#1a1d23',
                              }}
                            >
                              {verification.mlKitResult?.confidence
                                ? `${verification.mlKitResult.confidence.toFixed(1)}%`
                                : 'N/A'}
                            </Typography>
                            {verification.mlKitResult?.confidence && (
                              <LinearProgress
                                variant="determinate"
                                value={verification.mlKitResult.confidence}
                                sx={{
                                  width: '60px',
                                  height: '6px',
                                  borderRadius: '3px',
                                  backgroundColor: '#e0e0e0',
                                  '& .MuiLinearProgress-bar': {
                                    backgroundColor:
                                      verification.mlKitResult.confidence >= 80
                                        ? '#4CAF50'
                                        : verification.mlKitResult.confidence >= 60
                                        ? '#FF9800'
                                        : '#F44336',
                                  },
                                }}
                              />
                            )}
                          </Box>
                        </Box>

                        {/* Face Count */}
                        <Box>
                          <Typography
                            sx={{
                              fontSize: '0.75rem',
                              color: '#8b919e',
                              mb: '4px',
                            }}
                          >
                            Faces Detected
                          </Typography>
                          <Typography
                            sx={{
                              fontWeight: 600,
                              color: '#1a1d23',
                            }}
                          >
                            {verification.mlKitResult?.faceCount || 0}
                          </Typography>
                        </Box>

                        {/* ML Kit Status */}
                        {verification.mlKitResult?.isValid && (
                          <Box>
                            <Typography
                              sx={{
                                fontSize: '0.75rem',
                                color: '#8b919e',
                                mb: '4px',
                              }}
                            >
                              ML Kit Status
                            </Typography>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                              <VerifiedIcon
                                sx={{
                                  fontSize: '16px',
                                  color: '#4CAF50',
                                }}
                              />
                              <Typography
                                sx={{
                                  fontWeight: 600,
                                  color: '#4CAF50',
                                  fontSize: '0.85rem',
                                }}
                              >
                                Valid
                              </Typography>
                            </Box>
                          </Box>
                        )}

                        {/* Submitted Date */}
                        <Box>
                          <Typography
                            sx={{
                              fontSize: '0.75rem',
                              color: '#8b919e',
                              mb: '4px',
                            }}
                          >
                            Submitted
                          </Typography>
                          <Typography
                            sx={{
                              fontWeight: 600,
                              color: '#1a1d23',
                            }}
                          >
                            {new Date(
                              verification.timestamp?.toDate?.() ||
                                verification.timestamp
                            ).toLocaleDateString()}
                          </Typography>
                        </Box>
                      </Box>
                    </Box>

                    {/* Status & Actions */}
                    <Box
                      sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'flex-end',
                        gap: '12px',
                      }}
                    >
                      <Chip
                        icon={getStatusIcon(verification.verificationStatus)}
                        label={
                          verification.verificationStatus
                            .charAt(0)
                            .toUpperCase() +
                          verification.verificationStatus.slice(1)
                        }
                        color={getStatusColor(
                          verification.verificationStatus
                        )}
                        variant="outlined"
                      />

                      {verification.verificationStatus === 'pending' && (
                        <Box
                          sx={{
                            display: 'flex',
                            gap: '8px',
                          }}
                        >
                          <Button
                            size="small"
                            variant="contained"
                            color="success"
                            onClick={() => {
                              setSelectedVerification(verification);
                              setApproveDialog(true);
                            }}
                            disabled={!canApproveSellers}
                          >
                            Approve
                          </Button>
                          <Button
                            size="small"
                            variant="contained"
                            color="error"
                            onClick={() => {
                              setSelectedVerification(verification);
                              setRejectDialog(true);
                            }}
                            disabled={!canRejectSellers}
                          >
                            Reject
                          </Button>
                        </Box>
                      )}

                      <Button
                        size="small"
                        startIcon={<ImageIcon />}
                        onClick={() => {
                          setSelectedImage(verification.imageUrl);
                          setImageDialog(true);
                        }}
                      >
                        View Photo
                      </Button>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Approve Dialog */}
      <Dialog open={approveDialog} onClose={() => setApproveDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Approve Verification</DialogTitle>
        <DialogContent>
          {selectedVerification?.mlKitResult && (
            <Alert severity="info" sx={{ mb: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <VerifiedIcon sx={{ fontSize: '18px' }} />
                <Box>
                  <Typography sx={{ fontWeight: 600, fontSize: '0.9rem' }}>
                    ML Kit Confidence: {selectedVerification.mlKitResult.confidence?.toFixed(1)}%
                  </Typography>
                  <Typography sx={{ fontSize: '0.8rem', mt: '2px' }}>
                    Faces detected: {selectedVerification.mlKitResult.faceCount}
                  </Typography>
                </Box>
              </Box>
            </Alert>
          )}
          <TextField
            fullWidth
            multiline
            rows={4}
            label="Welcome Message (Optional)"
            value={approveMessage}
            onChange={(e) => setApproveMessage(e.target.value)}
            placeholder="Send a welcome message to the seller..."
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveDialog(false)}>Cancel</Button>
          <Button
            onClick={handleApproveVerification}
            variant="contained"
            color="success"
          >
            Approve
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reject Dialog */}
      <Dialog open={rejectDialog} onClose={() => setRejectDialog(false)}>
        <DialogTitle>Reject Verification</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Rejection Reason"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="e.g., Face quality too poor, Multiple faces detected"
            sx={{ mt: 2, mb: 2 }}
          />
          <TextField
            fullWidth
            multiline
            rows={4}
            label="Message to Seller"
            value={rejectMessage}
            onChange={(e) => setRejectMessage(e.target.value)}
            placeholder="Explain why the verification was rejected..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialog(false)}>Cancel</Button>
          <Button
            onClick={handleRejectVerification}
            variant="contained"
            color="error"
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>

      {/* Image Dialog */}
      <Dialog
        open={imageDialog}
        onClose={() => setImageDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Verification Photo</DialogTitle>
        <DialogContent>
          {selectedImage && (
            <Box
              component="img"
              src={selectedImage}
              sx={{
                width: '100%',
                borderRadius: '8px',
                mt: 2,
              }}
            />
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImageDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SellerVerificationDashboard;
