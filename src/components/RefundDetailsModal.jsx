import React from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button,
  Grid, Typography, Chip, Box,
} from '@mui/material';
import { LocalAtm as LocalAtmIcon } from '@mui/icons-material';

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

const getStatusColor = (status) => {
  const colors = {
    requested: { bg: '#fff3cd', color: '#856404' },
    approved: { bg: '#d1ecf1', color: '#0c5460' },
    processing: { bg: '#cce5ff', color: '#004085' },
    completed: { bg: '#d4edda', color: '#155724' },
    rejected: { bg: '#f8d7da', color: '#721c24' },
    failed: { bg: '#f8d7da', color: '#721c24' },
  };
  return colors[status] || { bg: '#e0e0e0', color: '#666' };
};

const RefundDetailsModal = ({ open, refund, onClose }) => {
  if (!refund) return null;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{ sx: { borderRadius: '15px' } }}
    >
      <DialogTitle sx={dialogTitleSx}>
        <LocalAtmIcon /> Refund Details
      </DialogTitle>
      <DialogContent sx={{ pt: 3 }}>
        <Grid container spacing={2}>
          {/* Refund Info */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Refund ID
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', fontWeight: 700, color: '#e91e63' }}>
              #{refund.id}
            </Typography>
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Order ID
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              #{refund.order_id || 'N/A'}
            </Typography>
          </Grid>

          {/* Amount Info */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Original Amount
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
              PKR {(refund.original_amount || 0).toLocaleString()}
            </Typography>
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Refund Amount
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#e91e63' }}>
              PKR {(refund.refund_amount || 0).toLocaleString()}
            </Typography>
          </Grid>

          {/* Buyer & Seller */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Buyer
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.buyer_name || 'N/A'}
            </Typography>
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Seller
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.seller_name || 'N/A'}
            </Typography>
          </Grid>

          {/* Reason */}
          <Grid item xs={12}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Reason
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.reason || 'N/A'}
            </Typography>
            {refund.reason_details && (
              <Typography sx={{ fontSize: '0.8rem', color: '#666', mt: 0.5 }}>
                {refund.reason_details}
              </Typography>
            )}
          </Grid>

          {/* Status & Dates */}
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Status
            </Typography>
            <Chip
              label={refund.status || 'unknown'}
              size="small"
              sx={{
                background: getStatusColor(refund.status).bg,
                color: getStatusColor(refund.status).color,
                fontWeight: 600,
                textTransform: 'capitalize',
              }}
            />
          </Grid>
          <Grid item xs={6}>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 0.75 }}>
              Requested
            </Typography>
            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>
              {refund.requested_at?.toLocaleString?.() || 'N/A'}
            </Typography>
          </Grid>

          {/* Audit Trail */}
          {refund.audit_trail?.length > 0 && (
            <Grid item xs={12}>
              <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', mb: 1 }}>
                Audit Trail
              </Typography>
              <Box sx={{ background: '#fafafa', border: '2px solid #e0e0e0', borderRadius: '10px', p: 2 }}>
                {refund.audit_trail.map((entry, idx) => (
                  <Box key={idx} sx={{ mb: idx < refund.audit_trail.length - 1 ? 1.5 : 0, pb: 1.5, borderBottom: idx < refund.audit_trail.length - 1 ? '1px solid #e0e0e0' : 'none' }}>
                    <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333' }}>
                      {entry.action?.toUpperCase()} by {entry.actor_name || 'System'}
                    </Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#666', mt: 0.25 }}>
                      {entry.timestamp ? new Date(entry.timestamp).toLocaleString() : 'N/A'}
                    </Typography>
                    {entry.notes && (
                      <Typography sx={{ fontSize: '0.75rem', color: '#666', mt: 0.5 }}>
                        {entry.notes}
                      </Typography>
                    )}
                  </Box>
                ))}
              </Box>
            </Grid>
          )}
        </Grid>
      </DialogContent>
      <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
        <Button onClick={onClose} sx={cancelBtnSx}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default RefundDetailsModal;
