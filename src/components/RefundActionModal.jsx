import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button,
  TextField, Typography,
} from '@mui/material';

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
  '& textarea': { fontSize: '0.85rem', padding: '10px 13px' },
};

const RefundActionModal = ({ open, refund, action, onClose, onConfirm }) => {
  const [notes, setNotes] = useState('');

  const handleConfirm = () => {
    onConfirm(notes);
    setNotes('');
  };

  const getTitle = () => {
    switch (action) {
      case 'approve':
        return 'Approve Refund';
      case 'reject':
        return 'Reject Refund';
      case 'process':
        return 'Process Refund';
      default:
        return 'Refund Action';
    }
  };

  const getDescription = () => {
    switch (action) {
      case 'approve':
        return `Approve refund of PKR ${(refund?.refund_amount || 0).toLocaleString()} for ${refund?.buyer_name}?`;
      case 'reject':
        return `Reject refund of PKR ${(refund?.refund_amount || 0).toLocaleString()} for ${refund?.buyer_name}?`;
      case 'process':
        return `Process refund of PKR ${(refund?.refund_amount || 0).toLocaleString()} to ${refund?.buyer_name}?`;
      default:
        return '';
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{ sx: { borderRadius: '15px' } }}
    >
      <DialogTitle sx={dialogTitleSx}>
        {getTitle()}
      </DialogTitle>
      <DialogContent sx={{ pt: 3 }}>
        <Typography sx={{ fontSize: '0.85rem', color: '#666', mb: 2 }}>
          {getDescription()}
        </Typography>
        <TextField
          fullWidth
          multiline
          rows={3}
          placeholder="Add notes (optional)"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          sx={fieldSx}
        />
      </DialogContent>
      <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
        <Button onClick={onClose} sx={cancelBtnSx}>
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          sx={primaryBtnSx}
        >
          Confirm
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default RefundActionModal;
