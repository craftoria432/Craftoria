import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Box, Card, Typography, Dialog, DialogTitle, DialogContent,
  DialogActions, Button, CircularProgress, Select, MenuItem,
  FormControl, TextField, Chip, Avatar,
} from '@mui/material';
import {
  Inventory2 as ProductIcon,
  Store as SellerIcon,
  Person as BuyerIcon,
  Build as TechnicalIcon,
  Search as InvestigateIcon,
  BoltRounded as ActionIcon,
  Cancel as DismissIcon,
  Email as ContactIcon,
  Send as SendIcon,
  CheckCircle as ResolvedIcon,
  Image as EvidenceIcon,
} from '@mui/icons-material';
import {
  collection,
  query,
  onSnapshot,
  orderBy,
  doc,
  updateDoc,
  addDoc,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { usePermissions } from '../hooks/usePermissions';
import { useAuth } from '../contexts/AuthContext';
import { PERMISSIONS } from '../config/permissions';
import ProtectedAction from '../components/common/ProtectedAction';
import toast from 'react-hot-toast';

const cancelBtnSx = {
  flex: 1, borderRadius: '10px', border: '2px solid #e0e0e0',
  color: '#666', fontWeight: 600, textTransform: 'none', py: 1.5,
  '&:hover': { borderColor: '#e91e63', color: '#e91e63' },
};
const primaryBtnSx = {
  flex: 1, background: 'linear-gradient(45deg, #E91E63, #F06292)',
  borderRadius: '10px', fontWeight: 600, textTransform: 'none',
  py: 1.5, boxShadow: 'none',
  '&:hover': { boxShadow: '0 5px 15px rgba(233,30,99,0.3)', transform: 'translateY(-2px)' },
};
const textareaSx = {
  '& .MuiOutlinedInput-root': {
    borderRadius: '10px',
    '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
    '&:hover fieldset': { borderColor: '#e91e63' },
    '&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' },
  },
  '& textarea': { fontSize: '0.85rem' },
};

const TYPE_CONFIG = {
  product:   { icon: <ProductIcon   sx={{ fontSize: 20 }} />, gradient: 'linear-gradient(135deg, #ffecd2, #fcb69f)', color: '#FB8C00' },
  seller:    { icon: <SellerIcon    sx={{ fontSize: 20 }} />, gradient: 'linear-gradient(135deg, #a8edea, #fed6e3)', color: '#E91E63' },
  buyer:     { icon: <BuyerIcon     sx={{ fontSize: 20 }} />, gradient: 'linear-gradient(135deg, #d299c2, #fef9d7)', color: '#8E24AA' },
  technical: { icon: <TechnicalIcon sx={{ fontSize: 20 }} />, gradient: 'linear-gradient(135deg, #cce5ff, #bfdbfe)', color: '#039BE5' },
};

const TYPE_FILTERS = [
  { key: 'all',       label: 'All Types',               icon: null },
  { key: 'product',   label: 'Inappropriate Products',  icon: <ProductIcon   sx={{ fontSize: 14 }} /> },
  { key: 'seller',    label: 'Seller Misconduct',       icon: <SellerIcon    sx={{ fontSize: 14 }} /> },
  { key: 'buyer',     label: 'Buyer Complaints',        icon: <BuyerIcon     sx={{ fontSize: 14 }} /> },
  { key: 'technical', label: 'Technical Issues',        icon: <TechnicalIcon sx={{ fontSize: 14 }} /> },
];

const getTypeLabel = (type) => ({
  product: 'Inappropriate Products',
  seller: 'Seller Misconduct',
  buyer: 'Buyer Complaints',
  technical: 'Technical Issues',
}[type] || 'Inappropriate Products');

const Reports = () => {
  const [loading, setLoading] = useState(true);
  const [reports, setReports] = useState([]);
  const [filteredReports, setFilteredReports] = useState([]);
  const [statusFilter, setStatusFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [actionModal,  setActionModal]  = useState({ open: false, report: null });
  const [dismissModal, setDismissModal] = useState({ open: false, report: null });
  const [contactModal, setContactModal] = useState({ open: false, report: null });
  const [actionType, setActionType] = useState('');
  const [actionNotes, setActionNotes] = useState('');
  const [dismissReason, setDismissReason] = useState('');
  const [contactMessage, setContactMessage] = useState('');

  const { can } = usePermissions();
  const { currentUser } = useAuth();
  const canInvestigate = can(PERMISSIONS.INVESTIGATE_REPORTS);
  const canTakeAction  = can(PERMISSIONS.TAKE_ACTION_REPORTS);
  const canDismiss     = can(PERMISSIONS.DISMISS_REPORTS);

  const unsubRef = useRef(null);

  const mapDocToReport = useCallback((d) => {
    const docData = d.data();
    return {
      id: d.id,
      type: getTypeLabel(docData.type || 'product'),
      typeKey: docData.type || 'product',
      icon: docData.type || 'product',
      reporter: {
        id: docData.reporter_id || '',
        name: docData.reporter_name || 'Unknown User',
        avatar: (docData.reporter_name || 'U').substring(0, 2).toUpperCase(),
      },
      reportedEntity: {
        id: docData.reported_entity_id || '',
        name: docData.reported_entity_name || 'Unknown Entity',
      },
      reason: docData.reason || 'No reason provided',
      description: docData.description || 'No description provided',
      status: docData.status || 'New',
      date: docData.created_at
        ? new Date(docData.created_at).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })
        : new Date().toLocaleDateString(),
      evidence: docData.evidence || [],
      source: docData.source || 'mobile app',
    };
  }, []);

  useEffect(() => {
    setLoading(true);
    const q = query(collection(db, 'reports'), orderBy('created_at', 'desc'));
    unsubRef.current = onSnapshot(q, (snap) => {
      const data = snap.docs.map(mapDocToReport);
      setReports(data);
      setLoading(false);
    }, (err) => {
      console.error('Error loading reports:', err);
      toast.error('Failed to load reports');
      setReports([]);
      setLoading(false);
    });

    return () => {
      if (unsubRef.current) unsubRef.current();
    };
  }, [mapDocToReport]);

  useEffect(() => {
    let filtered = reports;
    const statusMap = { new: 'New', review: 'Under Review', resolved: 'Resolved' };
    if (statusFilter !== 'all') filtered = filtered.filter(r => r.status === statusMap[statusFilter]);
    if (typeFilter !== 'all')   filtered = filtered.filter(r => r.typeKey === typeFilter);
    setFilteredReports(filtered);
  }, [statusFilter, typeFilter, reports]);

  const handleInvestigate = async (report) => {
    if (!canInvestigate) { toast.error('You do not have permission to investigate reports'); return; }
    try {
      await updateDoc(doc(db, 'reports', report.id), {
        status: 'Under Review',
        investigated_at: Date.now(),
        investigated_by: currentUser?.uid || 'unknown',
        updated_at: Date.now(),
      });

      await addDoc(collection(db, 'notifications'), {
        user_id: report.reporter?.id || '',
        title: 'Report Under Investigation',
        description: `Your report about "${report.reportedEntity?.name || 'the reported entity'}" is now being investigated by our team.`,
        category: 'REPORT',
        action_type: 'VIEW_REPORT',
        action_data: { report_id: report.id },
        is_read: false,
        created_at: Date.now(),
      });

      toast.success(`Investigation started for report #${report.id}`);
    } catch (error) {
      console.error('Error investigating report:', error);
      toast.error('Failed to start investigation');
    }
  };

  const handleTakeAction = (report) => {
    if (!canTakeAction) { toast.error('You do not have permission to take action on reports'); return; }
    setActionType(''); setActionNotes('');
    setActionModal({ open: true, report });
  };

  const handleDismiss = (report) => {
    if (!canDismiss) { toast.error('You do not have permission to dismiss reports'); return; }
    setDismissReason('');
    setDismissModal({ open: true, report });
  };

  const handleContact = (report) => {
    setContactMessage('');
    setContactModal({ open: true, report });
  };

  const confirmAction = async () => {
    if (!actionType) { toast.error('Please select an action type'); return; }
    if (!actionNotes.trim()) { toast.error('Please provide action notes'); return; }

    try {
      const report = actionModal.report;
      const actionLog = {
        status: 'Resolved',
        action_taken: actionType,
        action_notes: actionNotes,
        action_by: currentUser?.uid || 'unknown',
        resolved_at: Date.now(),
        updated_at: Date.now(),
      };

      if (actionType === 'Remove Content') {
        if (report.typeKey === 'product' && report.reportedEntity?.id) {
          await updateDoc(doc(db, 'products', report.reportedEntity.id), {
            is_active: false,
            is_removed: true,
            removed_reason: actionNotes,
            removed_at: Date.now(),
            removed_by: currentUser?.uid || 'unknown',
          });
          toast.success('Product removed and deactivated');
        }
      } else if (actionType === 'Suspend User Account') {
        if (report.reportedEntity?.id) {
          await updateDoc(doc(db, 'users', report.reportedEntity.id), {
            is_suspended: true,
            suspension_reason: actionNotes,
            suspended_at: Date.now(),
            suspended_by: currentUser?.uid || 'unknown',
            suspension_until: Date.now() + (30 * 24 * 60 * 60 * 1000),
          });
          toast.success('User account suspended for 30 days');
        }
      } else if (actionType === 'Ban User') {
        if (report.reportedEntity?.id) {
          await updateDoc(doc(db, 'users', report.reportedEntity.id), {
            is_banned: true,
            is_active: false,
            ban_reason: actionNotes,
            banned_at: Date.now(),
            banned_by: currentUser?.uid || 'unknown',
          });

          await addDoc(collection(db, 'notifications'), {
            user_id: report.reportedEntity.id,
            title: 'Account Banned',
            description: `Your account has been permanently banned. Reason: ${actionNotes}`,
            category: 'ADMIN_MESSAGE',
            action_type: 'VIEW_PROFILE',
            is_read: false,
            created_at: Date.now(),
          });
          toast.success('User banned permanently');
        }
      } else if (actionType === 'Send Warning') {
        if (report.reportedEntity?.id) {
          await addDoc(collection(db, 'notifications'), {
            user_id: report.reportedEntity.id,
            title: 'Warning from Admin',
            description: actionNotes,
            category: 'ADMIN_MESSAGE',
            action_type: 'VIEW_PROFILE',
            is_read: false,
            created_at: Date.now(),
          });
          toast.success('Warning sent to user');
        }
      }

      await updateDoc(doc(db, 'reports', report.id), actionLog);

      await addDoc(collection(db, 'notifications'), {
        user_id: report.reporter?.id || '',
        title: 'Report Resolved - Action Taken',
        description: `Action taken on your report: ${actionType}. ${actionNotes}`,
        category: 'REPORT',
        action_type: 'VIEW_REPORT',
        action_data: { report_id: report.id, action_type: actionType },
        is_read: false,
        created_at: Date.now(),
      });

      toast.success(`Action taken on report #${report.id}`);
      setActionModal({ open: false, report: null });
      setActionType('');
      setActionNotes('');
    } catch (error) {
      console.error('Error taking action:', error);
      toast.error(`Failed to take action: ${error.message || 'Unknown error'}`);
    }
  };

  const confirmDismiss = async () => {
    if (!dismissReason.trim()) { toast.error('Please provide a reason for dismissal'); return; }

    try {
      const report = dismissModal.report;

      await updateDoc(doc(db, 'reports', report.id), {
        status: 'Resolved',
        dismissed: true,
        dismiss_reason: dismissReason,
        dismissed_by: currentUser?.uid || 'unknown',
        resolved_at: Date.now(),
        updated_at: Date.now(),
      });

      await addDoc(collection(db, 'notifications'), {
        user_id: report.reporter?.id || '',
        title: 'Report Reviewed',
        description: `Your report has been reviewed by our moderation team. ${dismissReason}`,
        category: 'REPORT',
        action_type: 'VIEW_REPORT',
        action_data: { report_id: report.id },
        is_read: false,
        created_at: Date.now(),
      });

      toast.success(`Report #${report.id} has been dismissed`);
      setDismissModal({ open: false, report: null });
    } catch (error) {
      console.error('Error dismissing report:', error);
      toast.error('Failed to dismiss report');
    }
  };

  const sendContactMessage = async () => {
    if (!contactMessage.trim()) { toast.error('Please type a message'); return; }

    try {
      const report = contactModal.report;

      await addDoc(collection(db, 'notifications'), {
        user_id: report.reporter?.id || '',
        title: 'Message from Admin',
        description: contactMessage,
        category: 'ADMIN_MESSAGE',
        action_type: 'VIEW_REPORT',
        action_data: { report_id: report.id, can_reply: true },
        is_read: false,
        created_at: Date.now(),
      });

      await updateDoc(doc(db, 'reports', report.id), {
        admin_contacted: true,
        last_contact_at: Date.now(),
        last_contact_by: currentUser?.uid || 'unknown',
        last_contact_message: contactMessage,
        updated_at: Date.now(),
      });

      toast.success(`Message sent to ${report.reporter?.name || 'Reporter'}`);
      setContactModal({ open: false, report: null });
      setContactMessage('');
    } catch (error) {
      console.error('Error sending message:', error);
      toast.error('Failed to send message');
    }
  };

  const getStatusStyle = useCallback((status) => ({
    New:          { bg: '#fff3cd', color: '#856404' },
    'Under Review': { bg: '#cce5ff', color: '#004085' },
    Resolved:     { bg: '#d4edda', color: '#155724' },
  }[status] || { bg: '#fff3cd', color: '#856404' }), []);

  const visibleReports = useMemo(() => filteredReports, [filteredReports]);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '70vh' }}>
        <CircularProgress sx={{ color: '#E91E63' }} />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography sx={{ fontSize: '1.5rem', fontWeight: 700, color: '#333', mb: 0.5 }}>Reports & Complaints</Typography>
        <Typography sx={{ fontSize: '0.85rem', color: '#666' }}>Review and manage user reports and complaints</Typography>
      </Box>

      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', mb: 2.5, p: 2.5 }}>
        <Box sx={{ display: 'flex', gap: 1.25, mb: 2, flexWrap: 'wrap' }}>
          {['all', 'new', 'review', 'resolved'].map(status => (
            <Chip key={status}
              label={status === 'all' ? 'All' : status === 'new' ? 'New' : status === 'review' ? 'Under Review' : 'Resolved'}
              onClick={() => setStatusFilter(status)}
              sx={{ padding: '8px 18px', borderRadius: '20px', border: '2px solid #e0e0e0', background: statusFilter === status ? 'linear-gradient(45deg, #E91E63, #F06292)' : 'white', color: statusFilter === status ? 'white' : '#666', fontWeight: 600, fontSize: '0.8rem', cursor: 'pointer', px: 2.25, height: '40px', transition: 'all 0.3s ease', '&:hover': { borderColor: '#e91e63' } }}
            />
          ))}
        </Box>

        <Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap' }}>
          {TYPE_FILTERS.map(type => (
            <Chip key={type.key}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {type.icon}
                  {type.label}
                </Box>
              }
              onClick={() => setTypeFilter(type.key)}
              sx={{ borderRadius: '20px', border: '2px solid #e0e0e0', background: typeFilter === type.key ? '#fafafa' : 'white', color: typeFilter === type.key ? '#e91e63' : '#666', borderColor: typeFilter === type.key ? '#e91e63' : '#e0e0e0', fontWeight: 600, fontSize: '0.8rem', cursor: 'pointer', px: 2.25, height: '40px', transition: 'all 0.3s ease', '&:hover': { borderColor: '#e91e63' } }}
            />
          ))}
        </Box>
      </Card>

      <Box sx={{ display: 'grid', gap: 2.5 }}>
        {visibleReports.length === 0 ? (
          <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', p: 5, textAlign: 'center' }}>
            <Typography sx={{ color: '#999' }}>No reports found</Typography>
          </Card>
        ) : visibleReports.map((report) => {
          const isResolved = report.status === 'Resolved';
          const isUnderReview = report.status === 'Under Review';
          const typeConfig = TYPE_CONFIG[report.icon] || TYPE_CONFIG.product;

          return (
            <Card key={report.id} sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', p: 2.5, opacity: isResolved ? 0.7 : 1, transition: 'all 0.3s ease', '&:hover': { borderColor: '#e91e63', boxShadow: '0 5px 15px rgba(233,30,99,0.15)' } }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                  <Box sx={{ width: 40, height: 40, borderRadius: '10px', background: typeConfig.gradient, display: 'flex', alignItems: 'center', justifyContent: 'center', color: typeConfig.color }}>
                    {typeConfig.icon}
                  </Box>
                  <Box>
                    <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666' }}>{report.type}</Typography>
                    <Typography sx={{ fontSize: '0.75rem', color: '#999' }}>#{report.id}</Typography>
                  </Box>
                </Box>
                <Chip label={report.status} size="small" sx={{ background: getStatusStyle(report.status).bg, color: getStatusStyle(report.status).color, fontWeight: 600, fontSize: '0.7rem', height: '24px', borderRadius: '20px' }} />
              </Box>

              <Box sx={{ mb: 2, p: 2, background: 'linear-gradient(135deg, #e3f2fd, #f3e5f5)', borderRadius: '12px', border: '2px solid #ce93d8' }}>
                <Typography sx={{ fontSize: '1rem', fontWeight: 700, color: '#333', textAlign: 'center', lineHeight: 1.6 }}>
                  <span style={{ color: '#e91e63', fontWeight: 800 }}>{report.reporter?.name || 'Unknown User'}</span>
                  {' reported '}
                  <span style={{ color: '#f57c00', fontWeight: 800 }}>{report.reportedEntity?.name || 'Unknown Entity'}</span>
                </Typography>
                <Typography sx={{ fontSize: '0.75rem', color: '#666', textAlign: 'center', mt: 0.5 }}>
                  Report Type: <strong style={{ textTransform: 'capitalize' }}>{report.typeKey}</strong>
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, mb: 1.5, p: 1.5, background: 'linear-gradient(135deg, #fff5f8, #ffe8f0)', border: '2px solid #f8bbd0', borderRadius: '12px' }}>
                  <Avatar sx={{ width: 40, height: 40, background: 'linear-gradient(135deg, #667eea, #764ba2)', fontSize: '0.9rem', fontWeight: 700 }}>
                    {report.reporter?.avatar || 'U'}
                  </Avatar>
                  <Box sx={{ flex: 1 }}>
                    <Typography sx={{ fontSize: '0.7rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 0.25 }}>Reporter (Who Reported)</Typography>
                    <Typography sx={{ fontSize: '0.95rem', fontWeight: 700, color: '#e91e63' }}>{report.reporter?.name || 'Unknown User'}</Typography>
                    {report.reporter?.id && (
                      <Typography sx={{ fontSize: '0.65rem', color: '#999', fontFamily: 'monospace', wordBreak: 'break-all' }}>
                        ID: {report.reporter.id}
                      </Typography>
                    )}
                  </Box>
                  <Typography sx={{ fontSize: '0.7rem', color: '#999' }}>{report.date}</Typography>
                </Box>

                <Box sx={{ mb: 1.5, p: 1.5, background: 'linear-gradient(135deg, #fff8e1, #ffecb3)', border: '2px solid #ffe082', borderRadius: '12px' }}>
                  <Typography sx={{ fontSize: '0.7rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 0.5 }}>Reported Entity (What Was Reported)</Typography>
                  <Typography sx={{ fontSize: '0.95rem', fontWeight: 700, color: '#f57c00', mb: 0.25 }}>{report.reportedEntity?.name || 'Unknown Entity'}</Typography>
                  {report.reportedEntity?.id && (
                    <Typography sx={{ fontSize: '0.65rem', color: '#999', fontFamily: 'monospace', wordBreak: 'break-all', mb: 0.75 }}>
                      ID: {report.reportedEntity.id}
                    </Typography>
                  )}
                  <Chip label={report.typeKey} size="small" sx={{ background: '#fff', border: '1px solid #ffe082', fontSize: '0.7rem', fontWeight: 600, textTransform: 'capitalize' }} />
                </Box>

                <Box sx={{ mb: 1.5 }}>
                  <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 0.75 }}>Reason</Typography>
                  <Typography sx={{ fontSize: '0.85rem', color: '#333', fontWeight: 600 }}>{report.reason}</Typography>
                </Box>

                <Typography sx={{ fontSize: '0.85rem', color: '#666', lineHeight: 1.6, mb: 1.5 }}>{report.description}</Typography>

                {report.evidence?.length > 0 && (
                  <Box sx={{ mb: 2 }}>
                    <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 1 }}>Attached Evidence</Typography>
                    <Box sx={{ display: 'flex', gap: 1.25 }}>
                      {report.evidence.map((_, index) => (
                        <Box key={index} sx={{ width: 80, height: 80, background: 'rgba(233,30,99,0.06)', border: '2px solid #f8bbd0', borderRadius: '10px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', transition: 'all 0.3s ease', gap: 0.5, '&:hover': { borderColor: '#e91e63', transform: 'scale(1.05)', background: 'rgba(233,30,99,0.1)' } }}>
                          <EvidenceIcon sx={{ fontSize: 24, color: '#e91e63' }} />
                          <Typography sx={{ fontSize: '0.6rem', color: '#e91e63', fontWeight: 600 }}>Photo {index + 1}</Typography>
                        </Box>
                      ))}
                    </Box>
                  </Box>
                )}
              </Box>

              <Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap' }}>
                {isResolved ? (
                  <Button disabled startIcon={<ResolvedIcon />}
                    sx={{ flex: 1, background: '#d4edda', color: '#155724', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, cursor: 'not-allowed', '&.Mui-disabled': { background: '#d4edda', color: '#155724' } }}>
                    Report Resolved
                  </Button>
                ) : isUnderReview ? (
                  <>
                    <ProtectedAction permission={PERMISSIONS.TAKE_ACTION_REPORTS} hideIfNoAccess={true}>
                      <Button onClick={() => handleTakeAction(report)} startIcon={<ActionIcon />}
                        sx={{ flex: 1, minWidth: '150px', background: '#FF9800', color: 'white', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, '&:hover': { background: '#fb8c00', boxShadow: '0 5px 15px rgba(255,152,0,0.3)', transform: 'translateY(-2px)' } }}>
                        Take Action
                      </Button>
                    </ProtectedAction>
                    <Button onClick={() => handleDismiss(report)} startIcon={<DismissIcon />}
                      sx={{ flex: 1, minWidth: '150px', background: 'white', color: '#666', border: '2px solid #e0e0e0', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, '&:hover': { borderColor: '#e91e63', color: '#e91e63' } }}>
                      Dismiss
                    </Button>
                    <Button onClick={() => handleContact(report)} startIcon={<ContactIcon />}
                      sx={{ flex: 1, minWidth: '150px', background: '#2196f3', color: 'white', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, '&:hover': { background: '#1976d2', boxShadow: '0 5px 15px rgba(33,150,243,0.3)', transform: 'translateY(-2px)' } }}>
                      Contact
                    </Button>
                  </>
                ) : (
                  <>
                    <Button onClick={() => handleInvestigate(report)} startIcon={<InvestigateIcon />}
                      sx={{ flex: 1, minWidth: '120px', background: 'linear-gradient(45deg, #E91E63, #F06292)', color: 'white', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, boxShadow: 'none', '&:hover': { boxShadow: '0 5px 15px rgba(233,30,99,0.3)', transform: 'translateY(-2px)' } }}>
                      Investigate
                    </Button>
                    <ProtectedAction permission={PERMISSIONS.TAKE_ACTION_REPORTS} hideIfNoAccess={true}>
                      <Button onClick={() => handleTakeAction(report)} startIcon={<ActionIcon />}
                        sx={{ flex: 1, minWidth: '120px', background: '#FF9800', color: 'white', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, '&:hover': { background: '#fb8c00', boxShadow: '0 5px 15px rgba(255,152,0,0.3)', transform: 'translateY(-2px)' } }}>
                        Take Action
                      </Button>
                    </ProtectedAction>
                    <Button onClick={() => handleDismiss(report)} startIcon={<DismissIcon />}
                      sx={{ flex: 1, minWidth: '120px', background: 'white', color: '#666', border: '2px solid #e0e0e0', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, '&:hover': { borderColor: '#e91e63', color: '#e91e63' } }}>
                      Dismiss
                    </Button>
                    <Button onClick={() => handleContact(report)} startIcon={<ContactIcon />}
                      sx={{ flex: 1, minWidth: '120px', background: '#2196f3', color: 'white', borderRadius: '10px', fontSize: '0.8rem', fontWeight: 600, textTransform: 'none', py: 1.25, '&:hover': { background: '#1976d2', boxShadow: '0 5px 15px rgba(33,150,243,0.3)', transform: 'translateY(-2px)' } }}>
                      Contact
                    </Button>
                  </>
                )}
              </Box>
            </Card>
          );
        })}
      </Box>

      <Dialog open={actionModal.open} onClose={() => setActionModal({ open: false, report: null })} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: '15px' } }}>
        <DialogTitle sx={{ background: 'linear-gradient(45deg, #E91E63, #F06292)', color: 'white', fontWeight: 600, fontSize: '1.15rem', p: 2.5, display: 'flex', alignItems: 'center', gap: 1 }}>
          <ActionIcon />Take Action on Report
        </DialogTitle>
        <DialogContent sx={{ pt: 3, pb: 3 }}>
          <Typography sx={{ fontSize: '0.85rem', color: '#666', mb: 2 }}>Report ID: <strong>#{actionModal.report?.id}</strong></Typography>
          <Box sx={{ mb: 2 }}>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>Action to Take</Typography>
            <FormControl fullWidth>
              <Select value={actionType} onChange={(e) => setActionType(e.target.value)} displayEmpty
                sx={{ borderRadius: '10px', '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' }, '&:hover fieldset': { borderColor: '#e91e63' }, '&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' }, fontSize: '0.85rem' }}>
                <MenuItem value="">Select an action</MenuItem>
                <MenuItem value="Remove Content">Remove Content</MenuItem>
                <MenuItem value="Suspend User Account">Suspend User Account</MenuItem>
                <MenuItem value="Send Warning">Send Warning</MenuItem>
                <MenuItem value="Ban User">Ban User</MenuItem>
                <MenuItem value="Other">Other</MenuItem>
              </Select>
            </FormControl>
          </Box>
          <Box>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>Action Notes</Typography>
            <TextField fullWidth multiline rows={4} placeholder="Describe what action was taken and why..." value={actionNotes} onChange={(e) => setActionNotes(e.target.value)} sx={textareaSx} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
          <Button onClick={() => setActionModal({ open: false, report: null })} sx={cancelBtnSx}>Cancel</Button>
          <Button onClick={confirmAction} variant="contained" startIcon={<ActionIcon />} sx={primaryBtnSx}>Confirm Action</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={dismissModal.open} onClose={() => setDismissModal({ open: false, report: null })} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: '15px' } }}>
        <DialogTitle sx={{ background: 'linear-gradient(45deg, #E91E63, #F06292)', color: 'white', fontWeight: 600, fontSize: '1.15rem', p: 2.5, display: 'flex', alignItems: 'center', gap: 1 }}>
          <DismissIcon />Dismiss Report
        </DialogTitle>
        <DialogContent sx={{ pt: 3, pb: 3 }}>
          <Typography sx={{ fontSize: '0.85rem', color: '#666', mb: 2 }}>Are you sure you want to dismiss report <strong>#{dismissModal.report?.id}</strong>?</Typography>
          <Box>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>Reason for Dismissal</Typography>
            <TextField fullWidth multiline rows={4} placeholder="Explain why this report is being dismissed..." value={dismissReason} onChange={(e) => setDismissReason(e.target.value)} sx={textareaSx} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
          <Button onClick={() => setDismissModal({ open: false, report: null })} sx={cancelBtnSx}>Cancel</Button>
          <Button onClick={confirmDismiss} variant="contained" startIcon={<DismissIcon />} sx={primaryBtnSx}>Dismiss Report</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={contactModal.open} onClose={() => setContactModal({ open: false, report: null })} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: '15px' } }}>
        <DialogTitle sx={{ background: 'linear-gradient(45deg, #E91E63, #F06292)', color: 'white', fontWeight: 600, fontSize: '1.15rem', p: 2.5, display: 'flex', alignItems: 'center', gap: 1 }}>
          <ContactIcon />Contact Reporter
        </DialogTitle>
        <DialogContent sx={{ pt: 3, pb: 3 }}>
          <Typography sx={{ fontSize: '0.85rem', color: '#666', mb: 2 }}>Send a message to <strong>{contactModal.report?.reporter?.name || 'Reporter'}</strong></Typography>
          <Box>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>Message</Typography>
            <TextField fullWidth multiline rows={4} placeholder="Type your message to the reporter..." value={contactMessage} onChange={(e) => setContactMessage(e.target.value)} sx={textareaSx} />
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 0, gap: 1 }}>
          <Button onClick={() => setContactModal({ open: false, report: null })} sx={cancelBtnSx}>Cancel</Button>
          <Button onClick={sendContactMessage} variant="contained" startIcon={<SendIcon />} sx={primaryBtnSx}>Send Message</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Reports;

