// src/components/seller/SellerModals.jsx
// ✅ PRODUCTION-READY with Cloudinary image support
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button,
  TextField, Typography, Select, MenuItem, Box,
} from '@mui/material';
import {
  TaskAlt as TaskAltIcon,
  Cancel as CancelIcon,
  CameraAlt as CameraAltIcon,
} from '@mui/icons-material';

// ── Shared tokens ───────────────────────────────────────────────────────────────
const dialogPaperSx = {
  borderRadius: '14px',
  maxWidth: '480px',
  width: '100%',
  border: '1px solid #eef0f4',
  boxShadow: '0 20px 60px rgba(0,0,0,0.12)',
};

const titleSx = {
  background: 'linear-gradient(135deg, #E91E63 0%, #F06292 100%)',
  color: '#fff',
  fontWeight: 700,
  fontSize: '0.95rem',
  py: '16px',
  px: '22px',
  display: 'flex',
  alignItems: 'center',
  gap: '10px',
};

const contentSx = { px: '22px', pt: '20px !important', pb: '8px' };

const fieldSx = {
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
};

const selectSx = {
  borderRadius: '9px',
  fontSize: '0.83rem',
  background: '#fafbfc',
  mb: '16px',
  '& fieldset': { borderColor: '#e8eaed', borderWidth: '1px' },
  '&:hover fieldset': { borderColor: '#E91E63' },
  '&.Mui-focused fieldset': {
    borderColor: '#E91E63',
    borderWidth: '1.5px',
    boxShadow: '0 0 0 3px rgba(233,30,99,0.08)',
  },
  '& .MuiSelect-select': { padding: '10px 13px', fontSize: '0.83rem' },
};

const actionsSx = { px: '22px', pb: '20px', pt: '14px', gap: '10px' };

const cancelBtnSx = {
  borderRadius: '8px',
  border: '1px solid #e2e8f0',
  color: '#64748b',
  fontWeight: 600,
  textTransform: 'none',
  fontSize: '0.83rem',
  flex: 1,
  py: '10px',
  '&:hover': { borderColor: '#E91E63', color: '#E91E63', background: 'rgba(233,30,99,0.04)' },
};

const confirmBtnSx = {
  background: 'linear-gradient(135deg, #E91E63 0%, #F06292 100%)',
  borderRadius: '8px',
  fontWeight: 600,
  textTransform: 'none',
  fontSize: '0.83rem',
  flex: 1,
  py: '10px',
  boxShadow: 'none',
  '&:hover': { boxShadow: '0 6px 18px rgba(233,30,99,0.28)', transform: 'translateY(-1px)' },
};

const fieldLabelSx = {
  display: 'block',
  fontSize: '0.72rem',
  fontWeight: 700,
  color: '#4a5568',
  letterSpacing: '0.4px',
  textTransform: 'uppercase',
  mb: '7px',
};

const bodyTextSx = {
  fontSize: '0.83rem',
  color: '#64748b',
  lineHeight: 1.7,
  mb: '18px',
};

// ── ApproveApplicationModal ────────────────────────────────────────────────────
export const ApproveApplicationModal = ({
  open, user, welcomeMessage, setWelcomeMessage, onClose, onConfirm,
}) => (
  <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: dialogPaperSx }}>
    <DialogTitle sx={titleSx}>
      <TaskAltIcon sx={{ fontSize: 20 }} />
      Approve Seller Application
    </DialogTitle>
    <DialogContent sx={contentSx}>
      <Typography sx={bodyTextSx}>
        Are you sure you want to approve{' '}
        <Box component="span" sx={{ fontWeight: 700, color: '#E91E63' }}>
          {user?.name}
        </Box>
        's seller application? They will be granted the seller role and can proceed with identity verification.
      </Typography>
      <Typography sx={fieldLabelSx}>Welcome Message (Optional)</Typography>
      <TextField
        fullWidth multiline rows={4}
        placeholder="Enter a welcome message for the seller..."
        value={welcomeMessage}
        onChange={(e) => setWelcomeMessage(e.target.value)}
        sx={fieldSx}
      />
    </DialogContent>
    <DialogActions sx={actionsSx}>
      <Button onClick={onClose} sx={cancelBtnSx}>Cancel</Button>
      <Button onClick={onConfirm} variant="contained" sx={confirmBtnSx}>Confirm Approval</Button>
    </DialogActions>
  </Dialog>
);

// ── RejectApplicationModal ─────────────────────────────────────────────────────
export const RejectApplicationModal = ({
  open, user, rejectReason, setRejectReason, rejectMessage, setRejectMessage, onClose, onConfirm,
}) => (
  <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: dialogPaperSx }}>
    <DialogTitle sx={titleSx}>
      <CancelIcon sx={{ fontSize: 20 }} />
      Reject Seller Application
    </DialogTitle>
    <DialogContent sx={contentSx}>
      <Typography sx={bodyTextSx}>
        You are about to reject the application from{' '}
        <Box component="span" sx={{ fontWeight: 700, color: '#E91E63' }}>
          {user?.name}
        </Box>
        . They can apply again after addressing the issues.
      </Typography>
      <Typography sx={fieldLabelSx}>Reason for Rejection *</Typography>
      <Select fullWidth value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} displayEmpty sx={selectSx}>
        <MenuItem value="" disabled sx={{ fontSize: '0.83rem', color: '#adb5bd' }}>Select a reason</MenuItem>
        <MenuItem value="incomplete_info" sx={{ fontSize: '0.83rem' }}>Incomplete information</MenuItem>
        <MenuItem value="suspicious_activity" sx={{ fontSize: '0.83rem' }}>Suspicious activity</MenuItem>
        <MenuItem value="policy_violation" sx={{ fontSize: '0.83rem' }}>Policy violation</MenuItem>
        <MenuItem value="other" sx={{ fontSize: '0.83rem' }}>Other</MenuItem>
      </Select>
      <Typography sx={fieldLabelSx}>Rejection Message *</Typography>
      <TextField
        fullWidth multiline rows={4}
        placeholder="Explain why the application was rejected..."
        value={rejectMessage}
        onChange={(e) => setRejectMessage(e.target.value)}
        sx={fieldSx}
      />
    </DialogContent>
    <DialogActions sx={actionsSx}>
      <Button onClick={onClose} sx={cancelBtnSx}>Cancel</Button>
      <Button onClick={onConfirm} variant="contained" sx={confirmBtnSx}>Confirm Rejection</Button>
    </DialogActions>
  </Dialog>
);

// ── ApproveVerificationModal ───────────────────────────────────────────────────
export const ApproveVerificationModal = ({
  open, user, welcomeMessage, setWelcomeMessage, onClose, onConfirm,
}) => (
  <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: dialogPaperSx }}>
    <DialogTitle sx={titleSx}>
      <TaskAltIcon sx={{ fontSize: 20 }} />
      Approve Identity Verification
    </DialogTitle>
    <DialogContent sx={contentSx}>
      <Typography sx={bodyTextSx}>
        Are you sure you want to approve{' '}
        <Box component="span" sx={{ fontWeight: 700, color: '#E91E63' }}>
          {user?.name}
        </Box>
        's identity verification? They will be fully verified and can start selling.
      </Typography>
      <Typography sx={fieldLabelSx}>Welcome Message (Optional)</Typography>
      <TextField
        fullWidth multiline rows={4}
        placeholder="Enter a welcome message for the verified seller..."
        value={welcomeMessage}
        onChange={(e) => setWelcomeMessage(e.target.value)}
        sx={fieldSx}
      />
    </DialogContent>
    <DialogActions sx={actionsSx}>
      <Button onClick={onClose} sx={cancelBtnSx}>Cancel</Button>
      <Button onClick={onConfirm} variant="contained" sx={confirmBtnSx}>Confirm Approval</Button>
    </DialogActions>
  </Dialog>
);

// ── RejectVerificationModal ────────────────────────────────────────────────────
export const RejectVerificationModal = ({
  open, user, rejectReason, setRejectReason, rejectMessage, setRejectMessage, onClose, onConfirm,
}) => (
  <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: dialogPaperSx }}>
    <DialogTitle sx={titleSx}>
      <CancelIcon sx={{ fontSize: 20 }} />
      Reject Identity Verification
    </DialogTitle>
    <DialogContent sx={contentSx}>
      <Typography sx={bodyTextSx}>
        You are about to reject the verification from{' '}
        <Box component="span" sx={{ fontWeight: 700, color: '#E91E63' }}>
          {user?.name}
        </Box>
        . They can resubmit after addressing the issues.
      </Typography>
      <Typography sx={fieldLabelSx}>Reason for Rejection *</Typography>
      <Select fullWidth value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} displayEmpty sx={selectSx}>
        <MenuItem value="" disabled sx={{ fontSize: '0.83rem', color: '#adb5bd' }}>Select a reason</MenuItem>
        <MenuItem value="image_quality" sx={{ fontSize: '0.83rem' }}>Image quality insufficient</MenuItem>
        <MenuItem value="face_not_clear" sx={{ fontSize: '0.83rem' }}>Face not clearly visible</MenuItem>
        <MenuItem value="suspicious_activity" sx={{ fontSize: '0.83rem' }}>Suspicious activity</MenuItem>
        <MenuItem value="other" sx={{ fontSize: '0.83rem' }}>Other</MenuItem>
      </Select>
      <Typography sx={fieldLabelSx}>Rejection Message *</Typography>
      <TextField
        fullWidth multiline rows={4}
        placeholder="Explain why the verification was rejected..."
        value={rejectMessage}
        onChange={(e) => setRejectMessage(e.target.value)}
        sx={fieldSx}
      />
    </DialogContent>
    <DialogActions sx={actionsSx}>
      <Button onClick={onClose} sx={cancelBtnSx}>Cancel</Button>
      <Button onClick={onConfirm} variant="contained" sx={confirmBtnSx}>Confirm Rejection</Button>
    </DialogActions>
  </Dialog>
);

// ── ImageModal ─────────────────────────────────────────────────────────────────
// ✅ PRODUCTION-READY: Now displays Cloudinary URLs correctly
export const ImageModal = ({ open, imageUrl, onClose }) => (
  <Dialog 
    open={open} 
    onClose={onClose} 
    maxWidth="md" 
    fullWidth
    PaperProps={{ sx: { ...dialogPaperSx, maxWidth: '760px' } }}
  >
    <DialogTitle sx={titleSx}>
      <CameraAltIcon sx={{ fontSize: 20 }} />
      Verification Image
    </DialogTitle>
    <DialogContent sx={{ px: '22px', pt: '20px !important', pb: '8px', textAlign: 'center' }}>
      {imageUrl ? (
        <Box 
          sx={{ 
            borderRadius: '10px', 
            overflow: 'hidden', 
            border: '1px solid #eef0f4', 
            background: '#fafbfc' 
          }}
        >
          <img
            src={imageUrl}
            alt="Verification selfie"
            style={{ 
              width: '100%', 
              maxHeight: '500px', 
              objectFit: 'contain', 
              display: 'block' 
            }}
            onError={(e) => {
              console.error('Failed to load verification image:', imageUrl);
              e.target.style.display = 'none';
              const parent = e.target.parentElement;
              parent.innerHTML = `
                <div style="padding: 48px; text-align: center;">
                  <div style="font-size: 0.83rem; color: #f44336; margin-bottom: 8px;">
                    Failed to load image
                  </div>
                  <div style="font-size: 0.72rem; color: #8b919e;">
                    The image URL may be invalid or the image was deleted
                  </div>
                </div>
              `;
            }}
          />
        </Box>
      ) : (
        <Box 
          sx={{ 
            py: 6, 
            border: '1px dashed #e2e8f0', 
            borderRadius: '10px', 
            background: '#fafbfc' 
          }}
        >
          <CameraAltIcon sx={{ fontSize: 36, color: '#d1d5db', mb: '8px' }} />
          <Typography sx={{ fontSize: '0.83rem', color: '#8b919e' }}>
            No image submitted yet
          </Typography>
        </Box>
      )}
    </DialogContent>
    <DialogActions sx={actionsSx}>
      <Button fullWidth onClick={onClose} sx={cancelBtnSx}>Close</Button>
    </DialogActions>
  </Dialog>
);
