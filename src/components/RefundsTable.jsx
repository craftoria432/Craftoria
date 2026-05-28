import React from 'react';
import {
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Chip, Box,
} from '@mui/material';
import {
  Visibility as VisibilityIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  Send as SendIcon,
} from '@mui/icons-material';

const actionBtnSx = (bg, iconColor) => ({
  width: 32, height: 32, borderRadius: '8px',
  background: bg, cursor: 'pointer', transition: 'all 0.2s ease',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  border: 'none',
  '& svg': { color: iconColor, fontSize: 15 },
  '&:hover': { transform: 'translateY(-2px)', filter: 'brightness(0.92)' },
});

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

const RefundsTable = ({ refunds, onViewDetails, onApprove, onReject, onProcess }) => {
  return (
    <TableContainer>
      <Table>
        <TableHead sx={{ background: '#fafafa', borderBottom: '2px solid #e0e0e0' }}>
          <TableRow>
            {['Refund ID', 'Order ID', 'Buyer', 'Amount', 'Reason', 'Status', 'Requested', 'Actions'].map(h => (
              <TableCell key={h} sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666', textTransform: 'uppercase' }}>
                {h}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {refunds.length === 0 ? (
            <TableRow>
              <TableCell colSpan={8} align="center" sx={{ py: 5, color: '#999' }}>
                No refunds found
              </TableCell>
            </TableRow>
          ) : (
            refunds.map((refund) => (
              <TableRow
                key={refund.id}
                sx={{
                  borderBottom: '2px solid #f0f0f0',
                  '&:hover': { background: '#fafafa' },
                }}
              >
                <TableCell sx={{ fontSize: '0.85rem', fontWeight: 700, color: '#e91e63' }}>
                  #{refund.id.substring(0, 8)}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  #{refund.order_id?.substring(0, 8) || 'N/A'}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  {refund.buyer_name || 'N/A'}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#e91e63' }}>
                  PKR {(refund.refund_amount || 0).toLocaleString()}
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  {refund.reason || 'N/A'}
                </TableCell>
                <TableCell>
                  <Chip
                    label={refund.status || 'unknown'}
                    size="small"
                    sx={{
                      background: getStatusColor(refund.status).bg,
                      color: getStatusColor(refund.status).color,
                      fontWeight: 600,
                      fontSize: '0.7rem',
                      textTransform: 'capitalize',
                    }}
                  />
                </TableCell>
                <TableCell sx={{ fontSize: '0.85rem', color: '#333' }}>
                  {refund.requested_at?.toLocaleDateString?.() || 'N/A'}
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', gap: 0.75 }}>
                    <Box
                      onClick={() => onViewDetails(refund)}
                      sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}
                      title="View Details"
                    >
                      <VisibilityIcon />
                    </Box>
                    {refund.status === 'requested' && (
                      <>
                        <Box
                          onClick={() => onApprove(refund)}
                          sx={actionBtnSx('rgba(76,175,80,0.12)', '#4CAF50')}
                          title="Approve"
                        >
                          <CheckCircleIcon />
                        </Box>
                        <Box
                          onClick={() => onReject(refund)}
                          sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')}
                          title="Reject"
                        >
                          <CancelIcon />
                        </Box>
                      </>
                    )}
                    {refund.status === 'approved' && (
                      <Box
                        onClick={() => onProcess(refund)}
                        sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')}
                        title="Process Refund"
                      >
                        <SendIcon />
                      </Box>
                    )}
                  </Box>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default RefundsTable;
