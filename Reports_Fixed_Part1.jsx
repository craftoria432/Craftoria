// PART 1: Imports and Configuration (Lines 1-80)
import React, { useState, useEffect, useCallback } from 'react';
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
  Flag as FlagIcon,
  Search as InvestigateIcon,
  BoltRounded as ActionIcon,
  Cancel as DismissIcon,
  Email as ContactIcon,
  Send as SendIcon,
  CheckCircle as ResolvedIcon,
  Image as EvidenceIcon,
} from '@mui/icons-material';
import { collection, query, getDocs } from 'firebase/firestore';
import { db } from '../services/firebase';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import ProtectedAction from '../components/common/ProtectedAction';
import toast from 'react-hot-toast';

// Styles remain the same
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
