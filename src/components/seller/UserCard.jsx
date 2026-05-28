// src/components/seller/UserCard.jsx
// ✅ PRODUCTION-READY: Displays seller applications and verifications with ML Kit data
import {
  Card, CardContent, Box, Typography, Button, Chip, Avatar,
} from '@mui/material';
import {
  Person as PersonIcon,
  Email as EmailIcon,
  Phone as PhoneIcon,
  CalendarToday as CalendarIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  CameraAlt as CameraAltIcon,
  Psychology as PsychologyIcon,
} from '@mui/icons-material';

const UserCard = ({
  user,
  isApplication,
  onApproveApplication,
  onRejectApplication,
  onApproveVerification,
  onRejectVerification,
  onViewImage,
  canApprove,
  canReject,
}) => {
  // Helper function to get ML Kit confidence color
  const getConfidenceColor = (confidence) => {
    if (!confidence) return '#9E9E9E';
    if (confidence >= 80) return '#4CAF50'; // Green
    if (confidence >= 60) return '#FF9800'; // Orange
    return '#F44336'; // Red
  };

  // Helper function to format timestamp
  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    try {
      const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return 'N/A';
    }
  };

  return (
    <Card
      sx={{
        borderRadius: '12px',
        border: '1px solid #eef0f4',
        boxShadow: '0 1px 4px rgba(0,0,0,0.05)',
        background: '#fff',
        mb: '16px',
        transition: 'all 0.2s ease',
        '&:hover': {
          boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
          transform: 'translateY(-2px)',
        },
      }}
    >
      <CardContent sx={{ p: '20px !important' }}>
        {/* Header Row */}
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: '16px', mb: '16px' }}>
          <Avatar
            sx={{
              width: 56,
              height: 56,
              background: 'linear-gradient(135deg, #E91E63 0%, #F06292 100%)',
              fontSize: '1.3rem',
              fontWeight: 700,
            }}
          >
            {user.name?.charAt(0).toUpperCase() || 'U'}
          </Avatar>

          <Box sx={{ flex: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: '10px', mb: '6px' }}>
              <Typography sx={{ fontSize: '1rem', fontWeight: 700, color: '#1a1d23' }}>
                {user.name || 'Unknown User'}
              </Typography>
              {!isApplication && (
                <Chip
                  label={user.verification_status || 'pending'}
                  size="small"
                  sx={{
                    fontSize: '0.68rem',
                    fontWeight: 700,
                    height: '22px',
                    textTransform: 'uppercase',
                    letterSpacing: '0.3px',
                    background:
                      user.verification_status === 'approved'
                        ? '#E8F5E9'
                        : user.verification_status === 'rejected'
                        ? '#FFEBEE'
                        : '#FFF3E0',
                    color:
                      user.verification_status === 'approved'
                        ? '#2E7D32'
                        : user.verification_status === 'rejected'
                        ? '#C62828'
                        : '#E65100',
                    border: '1px solid',
                    borderColor:
                      user.verification_status === 'approved'
                        ? '#A5D6A7'
                        : user.verification_status === 'rejected'
                        ? '#EF9A9A'
                        : '#FFCC80',
                  }}
                />
              )}
            </Box>

            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: '12px', mb: '8px' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <EmailIcon sx={{ fontSize: 14, color: '#8b919e' }} />
                <Typography sx={{ fontSize: '0.78rem', color: '#64748b' }}>
                  {user.email || 'No email'}
                </Typography>
              </Box>
              {user.phone && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <PhoneIcon sx={{ fontSize: 14, color: '#8b919e' }} />
                  <Typography sx={{ fontSize: '0.78rem', color: '#64748b' }}>
                    {user.phone}
                  </Typography>
                </Box>
              )}
            </Box>

            <Box sx={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <CalendarIcon sx={{ fontSize: 13, color: '#8b919e' }} />
              <Typography sx={{ fontSize: '0.72rem', color: '#8b919e' }}>
                Submitted: {formatDate(user.created_at || user.submitted_at)}
              </Typography>
            </Box>
          </Box>
        </Box>

        {/* ML Kit Results - Only for Verifications */}
        {!isApplication && user.mlKitResult && (
          <Box
            sx={{
              background: '#fafbfc',
              border: '1px solid #eef0f4',
              borderRadius: '10px',
              p: '14px',
              mb: '16px',
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px', mb: '12px' }}>
              <PsychologyIcon sx={{ fontSize: 18, color: '#E91E63' }} />
              <Typography sx={{ fontSize: '0.78rem', fontWeight: 700, color: '#1a1d23' }}>
                ML Kit Face Detection Results
              </Typography>
            </Box>

            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px' }}>
              {/* Confidence Score */}
              <Box>
                <Typography sx={{ fontSize: '0.68rem', color: '#8b919e', mb: '4px', fontWeight: 600 }}>
                  Confidence Score
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Box
                    sx={{
                      width: 10,
                      height: 10,
                      borderRadius: '50%',
                      background: getConfidenceColor(user.mlKitResult.confidence),
                    }}
                  />
                  <Typography
                    sx={{
                      fontSize: '0.88rem',
                      fontWeight: 700,
                      color: getConfidenceColor(user.mlKitResult.confidence),
                    }}
                  >
                    {user.mlKitResult.confidence?.toFixed(1) || 0}%
                  </Typography>
                </Box>
              </Box>

              {/* Face Count */}
              <Box>
                <Typography sx={{ fontSize: '0.68rem', color: '#8b919e', mb: '4px', fontWeight: 600 }}>
                  Faces Detected
                </Typography>
                <Typography sx={{ fontSize: '0.88rem', fontWeight: 700, color: '#1a1d23' }}>
                  {user.mlKitResult.faceCount || 0}
                </Typography>
              </Box>

              {/* Validation Status */}
              <Box>
                <Typography sx={{ fontSize: '0.68rem', color: '#8b919e', mb: '4px', fontWeight: 600 }}>
                  ML Kit Status
                </Typography>
                <Chip
                  label={user.mlKitResult.isValid ? 'Valid' : 'Invalid'}
                  size="small"
                  sx={{
                    fontSize: '0.68rem',
                    fontWeight: 700,
                    height: '20px',
                    background: user.mlKitResult.isValid ? '#E8F5E9' : '#FFEBEE',
                    color: user.mlKitResult.isValid ? '#2E7D32' : '#C62828',
                    border: '1px solid',
                    borderColor: user.mlKitResult.isValid ? '#A5D6A7' : '#EF9A9A',
                  }}
                />
              </Box>
            </Box>

            {/* ML Kit Message */}
            {user.mlKitResult.message && (
              <Box
                sx={{
                  mt: '12px',
                  pt: '12px',
                  borderTop: '1px solid #eef0f4',
                }}
              >
                <Typography sx={{ fontSize: '0.72rem', color: '#64748b', fontStyle: 'italic' }}>
                  "{user.mlKitResult.message}"
                </Typography>
              </Box>
            )}
          </Box>
        )}

        {/* Verification Image - Only for Verifications */}
        {!isApplication && (user.verification_photo_url || user.verificationImageUrl) && (
          <Box sx={{ mb: '16px' }}>
            <Button
              startIcon={<CameraAltIcon />}
              onClick={() => onViewImage(user.verification_photo_url || user.verificationImageUrl)}
              sx={{
                borderRadius: '8px',
                border: '1px solid #e2e8f0',
                color: '#E91E63',
                fontWeight: 600,
                textTransform: 'none',
                fontSize: '0.78rem',
                py: '8px',
                px: '14px',
                '&:hover': {
                  borderColor: '#E91E63',
                  background: 'rgba(233,30,99,0.04)',
                },
              }}
            >
              View Verification Photo
            </Button>
          </Box>
        )}

        {/* Action Buttons */}
        <Box sx={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          {isApplication ? (
            <>
              {canApprove && (
                <Button
                  startIcon={<CheckCircleIcon />}
                  onClick={onApproveApplication}
                  variant="contained"
                  sx={{
                    flex: 1,
                    minWidth: '140px',
                    background: 'linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%)',
                    borderRadius: '8px',
                    fontWeight: 600,
                    textTransform: 'none',
                    fontSize: '0.83rem',
                    py: '10px',
                    boxShadow: 'none',
                    '&:hover': {
                      boxShadow: '0 6px 18px rgba(76,175,80,0.28)',
                      transform: 'translateY(-1px)',
                    },
                  }}
                >
                  Approve Application
                </Button>
              )}
              {canReject && (
                <Button
                  startIcon={<CancelIcon />}
                  onClick={onRejectApplication}
                  variant="outlined"
                  sx={{
                    flex: 1,
                    minWidth: '140px',
                    borderRadius: '8px',
                    border: '1px solid #F44336',
                    color: '#F44336',
                    fontWeight: 600,
                    textTransform: 'none',
                    fontSize: '0.83rem',
                    py: '10px',
                    '&:hover': {
                      borderColor: '#D32F2F',
                      background: 'rgba(244,67,54,0.04)',
                    },
                  }}
                >
                  Reject Application
                </Button>
              )}
            </>
          ) : (
            <>
              {canApprove && user.verification_status === 'pending' && (
                <Button
                  startIcon={<CheckCircleIcon />}
                  onClick={onApproveVerification}
                  variant="contained"
                  sx={{
                    flex: 1,
                    minWidth: '140px',
                    background: 'linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%)',
                    borderRadius: '8px',
                    fontWeight: 600,
                    textTransform: 'none',
                    fontSize: '0.83rem',
                    py: '10px',
                    boxShadow: 'none',
                    '&:hover': {
                      boxShadow: '0 6px 18px rgba(76,175,80,0.28)',
                      transform: 'translateY(-1px)',
                    },
                  }}
                >
                  Approve Verification
                </Button>
              )}
              {canReject && user.verification_status === 'pending' && (
                <Button
                  startIcon={<CancelIcon />}
                  onClick={onRejectVerification}
                  variant="outlined"
                  sx={{
                    flex: 1,
                    minWidth: '140px',
                    borderRadius: '8px',
                    border: '1px solid #F44336',
                    color: '#F44336',
                    fontWeight: 600,
                    textTransform: 'none',
                    fontSize: '0.83rem',
                    py: '10px',
                    '&:hover': {
                      borderColor: '#D32F2F',
                      background: 'rgba(244,67,54,0.04)',
                    },
                  }}
                >
                  Reject Verification
                </Button>
              )}
            </>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

export default UserCard;
