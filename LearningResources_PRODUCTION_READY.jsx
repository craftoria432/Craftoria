// src/pages/LearningResources.jsx - PRODUCTION READY VERSION
import React, { useState, useEffect, useRef } from 'react';
import {
  Box, Card, CardContent, Typography, Button, TextField, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Chip, Dialog, DialogTitle,
  DialogContent, DialogActions, CircularProgress, Select, MenuItem,
  FormControl, Switch, Tooltip, Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  School as SchoolIcon,
  VideoLibrary as VideoIcon,
  Article as ArticleIcon,
  Search as SearchIcon,
  OpenInNew as OpenIcon,
  Palette as PaletteIcon,
  Brush as BrushIcon,
  AutoStories as StoriesIcon,
  MenuBook as MenuBookIcon,
  LocalLibrary as LibraryIcon,
  EmojiObjects as IdeaIcon,
  Lightbulb as LightbulbIcon,
  Star as StarIcon,
  Favorite as FavoriteIcon,
  Build as BuildIcon,
  Code as CodeIcon,
  DesignServices as DesignIcon,
  Camera as CameraIcon,
  MusicNote as MusicIcon,
  Handyman as HandymanIcon,
  Layers as LayersIcon,
  Category as CategoryIcon,
  PlayCircle as PlayCircleIcon,
  OndemandVideo as OndemandVideoIcon,
  Draw as DrawIcon,
  ChevronRight as ChevronRightIcon,
  TrendingUp as TrendingUpIcon,
  WarningAmberRounded,
} from '@mui/icons-material';
import {
  collection, onSnapshot, addDoc, updateDoc, deleteDoc, doc, query, orderBy, serverTimestamp,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import { useAuth } from '../contexts/AuthContext';
import { usePermissions } from '../hooks/usePermissions';
import { PERMISSIONS } from '../config/permissions';
import toast from 'react-hot-toast';

// ─── Icon registry ────────────────────────────────────────────────────────────
const ICON_MAP = {
  school:    { icon: SchoolIcon,        label: 'School',    color: '#667eea' },
  palette:   { icon: PaletteIcon,       label: 'Palette',   color: '#E91E63' },
  brush:     { icon: BrushIcon,         label: 'Brush',     color: '#F06292' },
  stories:   { icon: StoriesIcon,       label: 'Stories',   color: '#9C27B0' },
  book:      { icon: MenuBookIcon,      label: 'Book',      color: '#3F51B5' },
  library:   { icon: LibraryIcon,       label: 'Library',   color: '#2196F3' },
  idea:      { icon: IdeaIcon,          label: 'Idea',      color: '#FF9800' },
  lightbulb: { icon: LightbulbIcon,     label: 'Lightbulb', color: '#FFC107' },
  star:      { icon: StarIcon,          label: 'Star',      color: '#FFD600' },
  favorite:  { icon: FavoriteIcon,      label: 'Favorite',  color: '#F44336' },
  build:     { icon: BuildIcon,         label: 'Build',     color: '#795548' },
  code:      { icon: CodeIcon,          label: 'Code',      color: '#607D8B' },
  design:    { icon: DesignIcon,        label: 'Design',    color: '#E91E63' },
  camera:    { icon: CameraIcon,        label: 'Camera',    color: '#00BCD4' },
  music:     { icon: MusicIcon,         label: 'Music',     color: '#9C27B0' },
  handyman:  { icon: HandymanIcon,      label: 'Handyman',  color: '#FF5722' },
  layers:    { icon: LayersIcon,        label: 'Layers',    color: '#009688' },
  category:  { icon: CategoryIcon,      label: 'Category',  color: '#673AB7' },
  play:      { icon: PlayCircleIcon,    label: 'Play',      color: '#4CAF50' },
  video:     { icon: OndemandVideoIcon, label: 'Video',     color: '#F44336' },
  draw:      { icon: DrawIcon,          label: 'Draw',      color: '#FF9800' },
  article:   { icon: ArticleIcon,       label: 'Article',   color: '#2196F3' },
};

const DEFAULT_CAT_ICON = 'school';
const DEFAULT_TUT_ICON = 'article';

const DynamicIcon = ({ iconKey, size = 22, color = 'white', sx = {} }) => {
  const entry = ICON_MAP[iconKey] || ICON_MAP[DEFAULT_CAT_ICON];
  const Icon = entry.icon;
  return <Icon sx={{ fontSize: size, color, ...sx }} />;
};

const iconGradient = (iconKey) => {
  const entry = ICON_MAP[iconKey] || ICON_MAP[DEFAULT_CAT_ICON];
  return `linear-gradient(135deg, ${entry.color}cc, ${entry.color}88)`;
};

// ─── Shared styles ────────────────────────────────────────────────────────────
const dialogPaper = { borderRadius: '15px' };
const titleSx = {
  background: 'linear-gradient(45deg, #E91E63, #F06292)',
  color: 'white', fontWeight: 600, fontSize: '1.15rem',
  padding: '20px', display: 'flex', alignItems: 'center', gap: 1,
};
const inputSx = {
  '& .MuiOutlinedInput-root': {
    borderRadius: '10px',
    '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' },
    '&:hover fieldset': { borderColor: '#e91e63' },
    '&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' },
  },
  '& input':    { fontSize: '0.85rem', padding: '10px 13px' },
  '& textarea': { fontSize: '0.85rem' },
};
const cancelBtnSx = {
  borderRadius: '10px', border: '2px solid #e0e0e0', color: '#666',
  fontWeight: 600, textTransform: 'none', px: 3,
  '&:hover': { borderColor: '#e91e63', color: '#e91e63' },
};
const primaryBtnSx = {
  background: 'linear-gradient(45deg, #E91E63, #F06292)', borderRadius: '10px',
  fontWeight: 600, textTransform: 'none', px: 3, boxShadow: 'none',
  '&:hover': { boxShadow: '0 5px 15px rgba(233,30,99,0.3)', transform: 'translateY(-2px)' },
};
const actionBtnSx = (bg, iconColor) => ({
  width: 32, height: 32, borderRadius: '8px', background: bg, cursor: 'pointer',
  transition: 'all 0.2s ease', display: 'flex', alignItems: 'center',
  justifyContent: 'center', border: 'none',
  '& svg': { color: iconColor, fontSize: 15 },
  '&:hover': { transform: 'translateY(-2px)', filter: 'brightness(0.92)' },
});
const getTypeColor = (isVideo) =>
  isVideo
    ? { bg: '#d4edda', color: '#155724' }
    : { bg: '#cce5ff', color: '#004085' };

// ─── Icon Picker ──────────────────────────────────────────────────────────────
const IconPicker = ({ value, onChange, label = 'Icon' }) => (
  <Box>
    <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>
      {label}
    </Typography>
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5, p: 1.5, background: '#fafafa', borderRadius: '10px', border: '2px solid #e0e0e0' }}>
      <Box sx={{ width: 36, height: 36, borderRadius: '8px', background: iconGradient(value), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <DynamicIcon iconKey={value} size={20} />
      </Box>
      <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>
        {ICON_MAP[value]?.label || 'School'}
      </Typography>
    </Box>
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
      {Object.entries(ICON_MAP).map(([key, entry]) => {
        const Icon = entry.icon;
        const selected = value === key;
        return (
          <Tooltip key={key} title={entry.label} placement="top">
            <Box
              onClick={() => onChange(key)}
              sx={{
                width: 36, height: 36, borderRadius: '8px',
                background: selected ? iconGradient(key) : `${entry.color}18`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                cursor: 'pointer', transition: 'all 0.2s ease',
                border: selected ? `2px solid ${entry.color}` : '2px solid transparent',
                '&:hover': { background: iconGradient(key), transform: 'scale(1.1)' },
              }}
            >
              <Icon sx={{ fontSize: 18, color: selected ? 'white' : entry.color }} />
            </Box>
          </Tooltip>
        );
      })}
    </Box>
  </Box>
);

// ─── Main Component ───────────────────────────────────────────────────────────
const LearningResources = () => {
  const [categories, setCategories] = useState([]);
  const [filteredCategories, setFilteredCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [expandedCategories, setExpandedCategories] = useState({});

  const [viewModal,   setViewModal]   = useState({ open: false, item: null, type: null });
  const [editModal,   setEditModal]   = useState({ open: false, item: null, type: null });
  const [deleteModal, setDeleteModal] = useState({ open: false, item: null, type: null, categoryId: null });
  const [addModal,    setAddModal]    = useState({ open: false, type: 'category', categoryId: null });

  const [editForm, setEditForm] = useState({
    title: '', description: '', icon: DEFAULT_CAT_ICON,
    displayOrder: 0, duration: '', url: '', isVideo: false,
  });

  const unsubscribeRef = useRef(null);
  const { currentUser } = useAuth();
  const { can } = usePermissions();

  // ✅ PERMISSION CHECKS
  const canView = can(PERMISSIONS.VIEW_LEARNING_RESOURCES);
  const canCreate = can(PERMISSIONS.CREATE_LEARNING_RESOURCES);
  const canEdit = can(PERMISSIONS.EDIT_LEARNING_RESOURCES);
  const canDelete = can(PERMISSIONS.DELETE_LEARNING_RESOURCES);

  // Auto-expand first category on load
  useEffect(() => {
    if (categories.length > 0 && Object.keys(expandedCategories).length === 0) {
      setExpandedCategories({ [categories[0].id]: true });
    }
  }, [categories]); // eslint-disable-line react-hooks/exhaustive-deps

  // ✅ Real-time onSnapshot listener
  useEffect(() => {
    if (!canView) {
      setLoading(false);
      return;
    }

    setLoading(true);
    const q = query(collection(db, 'learning_categories'), orderBy('display_order'));

    unsubscribeRef.current = onSnapshot(
      q,
      (snapshot) => {
        const data = snapshot.docs.map((d) => ({
          id: d.id,
          ...d.data(),
          tutorials: d.data().tutorials || [],
        }));
        setCategories(data);
        setLoading(false);
      },
      (error) => {
        console.error('Error fetching learning categories:', error);
        toast.error('Failed to load learning resources');
        setLoading(false);
      }
    );

    return () => {
      if (unsubscribeRef.current) unsubscribeRef.current();
    };
  }, [canView]);

  // Filter & search
  useEffect(() => {
    let filtered = [...categories];
    if (categoryFilter !== 'all') filtered = filtered.filter((c) => c.id === categoryFilter);
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      filtered = filtered.filter((c) =>
        c.title?.toLowerCase().includes(q) ||
        c.description?.toLowerCase().includes(q) ||
        c.tutorials?.some((t) =>
          t.title?.toLowerCase().includes(q) ||
          t.description?.toLowerCase().includes(q)
        )
      );
    }
    setFilteredCategories(filtered);
  }, [searchQuery, categoryFilter, categories]);

  // Handlers
  const toggleCategory = (id) =>
    setExpandedCategories((p) => ({ ...p, [id]: !p[id] }));

  const handleAddCategory = () => {
    if (!canCreate) {
      toast.error('You do not have permission to create learning resources');
      return;
    }
    setEditForm({
      title: '', description: '', icon: DEFAULT_CAT_ICON,
      displayOrder: Math.max(...categories.map((c) => c.display_order || 0), 0) + 1,
      duration: '', url: '', isVideo: false,
    });
    setAddModal({ open: true, type: 'category', categoryId: null });
  };

  const handleAddTutorial = (categoryId) => {
    if (!canCreate) {
      toast.error('You do not have permission to create tutorials');
      return;
    }
    setEditForm({
      title: '', description: '', icon: DEFAULT_TUT_ICON,
      displayOrder: 0, duration: '', url: '', isVideo: false,
    });
    setAddModal({ open: true, type: 'tutorial', categoryId });
  };

  const handleSaveEdit = async () => {
    if (!canEdit) {
      toast.error('You do not have permission to edit learning resources');
      return;
    }

    if (!editForm.title.trim()) { toast.error('Title is required'); return; }
    if (editModal.type === 'tutorial' && !editForm.url.trim()) { toast.error('URL is required'); return; }

    try {
      setSaving(true);

      if (editModal.type === 'category') {
        await updateDoc(doc(db, 'learning_categories', editModal.item.id), {
          title: editForm.title.trim(),
          description: editForm.description.trim(),
          icon: editForm.icon,
          display_order: parseInt(editForm.displayOrder) || 0,
          updatedAt: serverTimestamp(),
          updatedBy: currentUser?.email || 'unknown',
        });
        toast.success('Category updated successfully');
      } else {
        const cat = categories.find((c) => c.id === editModal.item.categoryId);
        const updated = cat.tutorials.map((t) =>
          t.id === editModal.item.id
            ? {
                ...t,
                title:       editForm.title.trim(),
                description: editForm.description.trim(),
                duration:    editForm.duration.trim(),
                icon:        editForm.icon,
                url:         editForm.url.trim(),
                is_video:    editForm.isVideo,
                category_id: editModal.item.categoryId,
                updatedAt:   Date.now(),
                updatedBy:   currentUser?.email || 'unknown',
              }
            : t
        );
        await updateDoc(doc(db, 'learning_categories', editModal.item.categoryId), { 
          tutorials: updated,
          updatedAt: serverTimestamp(),
        });
        toast.success('Tutorial updated successfully');
      }
      setEditModal({ open: false, item: null, type: null });
    } catch (err) {
      console.error('Edit error:', err);
      toast.error(`Failed to update ${editModal.type}: ` + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleSaveAdd = async () => {
    if (!canCreate) {
      toast.error('You do not have permission to create learning resources');
      return;
    }

    if (!editForm.title.trim()) { toast.error('Title is required'); return; }
    if (addModal.type === 'tutorial' && !editForm.url.trim()) { toast.error('URL is required'); return; }

    try {
      setSaving(true);

      if (addModal.type === 'category') {
        await addDoc(collection(db, 'learning_categories'), {
          title:         editForm.title.trim(),
          description:   editForm.description.trim(),
          icon:          editForm.icon,
          display_order: parseInt(editForm.displayOrder) || 0,
          tutorials:     [],
          createdAt:     serverTimestamp(),
          createdBy:     currentUser?.email || 'unknown',
          updatedAt:     serverTimestamp(),
        });
        toast.success('Category created successfully');
      } else {
        const cat = categories.find((c) => c.id === addModal.categoryId);
        const newTutorial = {
          id:          `tutorial_${Date.now()}`,
          title:       editForm.title.trim(),
          description: editForm.description.trim(),
          duration:    editForm.duration.trim(),
          icon:        editForm.icon,
          url:         editForm.url.trim(),
          is_video:    editForm.isVideo,
          category_id: addModal.categoryId,
          created_at:  Date.now(),
          createdBy:   currentUser?.email || 'unknown',
        };
        await updateDoc(doc(db, 'learning_categories', addModal.categoryId), {
          tutorials: [...(cat.tutorials || []), newTutorial],
          updatedAt: serverTimestamp(),
        });
        toast.success('Tutorial created successfully');
      }
      setAddModal({ open: false, type: 'category', categoryId: null });
    } catch (err) {
      console.error('Add error:', err);
      toast.error(`Failed to create ${addModal.type}: ` + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!canDelete) {
      toast.error('You do not have permission to delete learning resources');
      return;
    }

    try {
      setSaving(true);

      if (deleteModal.type === 'category') {
        await deleteDoc(doc(db, 'learning_categories', deleteModal.item.id));
        toast.success('Category deleted successfully');
      } else {
        const cat = categories.find((c) => c.id === deleteModal.categoryId);
        await updateDoc(doc(db, 'learning_categories', deleteModal.categoryId), {
          tutorials: cat.tutorials.filter((t) => t.id !== deleteModal.item.id),
          updatedAt: serverTimestamp(),
        });
        toast.success('Tutorial deleted successfully');
      }
      setDeleteModal({ open: false, item: null, type: null, categoryId: null });
    } catch (err) {
      console.error('Delete error:', err);
      toast.error('Failed to delete: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const totalCategories = categories.length;
  const totalTutorials  = categories.reduce((s, c) => s + (c.tutorials?.length || 0), 0);
  const totalVideos     = categories.reduce((s, c) => s + (c.tutorials?.filter((t) => t.is_video).length || 0), 0);
  const totalArticles   = totalTutorials - totalVideos;

  if (loading) return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '70vh' }}>
      <CircularProgress sx={{ color: '#E91E63' }} />
    </Box>
  );

  if (!canView) {
    return (
      <Box sx={{ p: 4 }}>
        <Alert severity="error">
          <Typography sx={{ fontWeight: 600 }}>Access Denied</Typography>
          <Typography sx={{ fontSize: '0.85rem', mt: 1 }}>
            You do not have permission to view learning resources.
          </Typography>
        </Alert>
      </Box>
    );
  }

  const renderFormFields = (isAdd = false) => {
    const type = isAdd ? addModal.type : editModal.type;
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Box>
          <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '1px', mb: 2, display: 'flex', alignItems: 'center', gap: 1, '&::after': { content: '""', flex: 1, height: '1px', background: 'linear-gradient(90deg, #E91E6330, transparent)' } }}>
            Basic Information
          </Typography>
          <Box sx={{ mb: 2 }}>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 0.75 }}>Title *</Typography>
            <TextField fullWidth value={editForm.title} onChange={(e) => setEditForm({ ...editForm, title: e.target.value })} placeholder="Enter a descriptive title" sx={inputSx} />
          </Box>
          <Box>
            <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 0.75 }}>Description</Typography>
            <TextField fullWidth multiline rows={3} value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} placeholder="Briefly describe what this covers…" sx={inputSx} />
          </Box>
        </Box>

        {type === 'category' && (
          <Box>
            <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '1px', mb: 2, display: 'flex', alignItems: 'center', gap: 1, '&::after': { content: '""', flex: 1, height: '1px', background: 'linear-gradient(90deg, #E91E6330, transparent)' } }}>
              Category Settings
            </Typography>
            <Box sx={{ maxWidth: '50%' }}>
              <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 0.75 }}>Display Order</Typography>
              <TextField fullWidth type="number" value={editForm.displayOrder} onChange={(e) => setEditForm({ ...editForm, displayOrder: e.target.value })} placeholder="0" sx={inputSx} />
            </Box>
          </Box>
        )}

        {type === 'tutorial' && (
          <Box>
            <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '1px', mb: 2, display: 'flex', alignItems: 'center', gap: 1, '&::after': { content: '""', flex: 1, height: '1px', background: 'linear-gradient(90deg, #E91E6330, transparent)' } }}>
              Content Details
            </Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: 2, mb: 2.5 }}>
              <Box>
                <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 0.75 }}>Duration</Typography>
                <TextField fullWidth value={editForm.duration} onChange={(e) => setEditForm({ ...editForm, duration: e.target.value })} placeholder="e.g. 10 min read" sx={inputSx} />
              </Box>
              <Box>
                <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 0.75 }}>URL *</Typography>
                <TextField fullWidth value={editForm.url} onChange={(e) => setEditForm({ ...editForm, url: e.target.value })} placeholder="https://example.com/resource" sx={inputSx} />
              </Box>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: '14px 18px', background: 'linear-gradient(135deg, rgba(233,30,99,0.04), rgba(240,98,146,0.04))', borderRadius: '10px', border: '2px solid #f0e0e8' }}>
              <Box>
                <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>Video Tutorial</Typography>
                <Typography sx={{ fontSize: '0.75rem', color: '#999', mt: 0.3 }}>Mark as video if this is a video resource</Typography>
              </Box>
              <Switch checked={editForm.isVideo} onChange={(e) => setEditForm({ ...editForm, isVideo: e.target.checked })} sx={{ '& .MuiSwitch-switchBase.Mui-checked': { color: '#E91E63' }, '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': { backgroundColor: '#E91E63' } }} />
            </Box>
          </Box>
        )}

        <Box>
          <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '1px', mb: 2, display: 'flex', alignItems: 'center', gap: 1, '&::after': { content: '""', flex: 1, height: '1px', background: 'linear-gradient(90deg, #E91E6330, transparent)' } }}>
            Appearance
          </Typography>
          <Box sx={{ p: 2.5, background: '#fafafa', borderRadius: '10px', border: '2px solid #e0e0e0' }}>
            <IconPicker value={editForm.icon} onChange={(key) => setEditForm({ ...editForm, icon: key })} label="Icon" />
          </Box>
        </Box>
      </Box>
    );
  };

  return (
    <Box>
      <Box sx={{ mb: 3.5, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography sx={{ fontSize: '1.5rem', fontWeight: 700, color: '#333', mb: 0.5 }}>
            Learning Resources
          </Typography>
          <Typography sx={{ fontSize: '0.85rem', color: '#666' }}>
            Manage educational content and tutorials for sellers
          </Typography>
        </Box>
        {canCreate && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleAddCategory}
            disabled={saving}
            sx={{
              background: 'linear-gradient(45deg, #E91E63, #F06292)',
              borderRadius: '10px',
              fontWeight: 600,
              fontSize: '0.85rem',
              textTransform: 'none',
              py: 1.4,
              px: 3,
              boxShadow: 'none',
              '&:hover': { boxShadow: '0 5px 15px rgba(233,30,99,0.3)', transform: 'translateY(-2px)' },
              '&:disabled': { opacity: 0.7 },
            }}
          >
            Add Category
          </Button>
        )}
      </Box>

      {/* Stats Cards */}
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' }, gap: '20px', mb: '25px' }}>
        {[
          { label: 'Total Categories', value: totalCategories, Icon: SchoolIcon, bg: 'rgba(233,30,99,0.1)', color: '#E91E63', trend: 'Learning categories' },
          { label: 'Total Tutorials', value: totalTutorials, Icon: ArticleIcon, bg: 'rgba(156,39,176,0.1)', color: '#9C27B0', trend: 'Across all categories' },
          { label: 'Video Tutorials', value: totalVideos, Icon: VideoIcon, bg: 'rgba(3,155,229,0.1)', color: '#039BE5', trend: 'Video resources' },
          { label: 'Article Tutorials', value: totalArticles, Icon: ArticleIcon, bg: 'rgba(251,140,0,0.1)', color: '#FB8C00', trend: 'Written resources' },
        ].map(({ label, value, Icon, bg, color, trend }) => (
          <Card key={label} sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', p: '20px', transition: 'all 0.3s ease', '&:hover': { borderColor: '#e91e63', boxShadow: '0 5px 15px rgba(233,30,99,0.15)', transform: 'translateY(-2px)' } }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: '12px' }}>
              <Typography sx={{ fontSize: '0.8rem', color: '#666', fontWeight: 500 }}>{label}</Typography>
              <Box sx={{ width: 40, height: 40, borderRadius: '10px', background: bg, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Icon sx={{ fontSize: 20, color }} />
              </Box>
            </Box>
            <Typography sx={{ fontSize: '2rem', fontWeight: 700, color: '#333', mb: '6px' }}>{value}</Typography>
            <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#4caf50', display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <TrendingUpIcon sx={{ fontSize: 14 }} /> {trend}
            </Typography>
          </Card>
        ))}
      </Box>

      {/* Search and Filter */}
      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', mb: '20px' }}>
        <CardContent sx={{ p: 2.5 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' }, gap: 1.5 }}>
            <TextField
              fullWidth
              placeholder="Search by category or tutorial name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{ startAdornment: <SearchIcon sx={{ fontSize: 18, color: '#999', mr: 0.75 }} /> }}
              sx={{ '& .MuiOutlinedInput-root': { height: '42px', borderRadius: '10px', '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' }, '&:hover fieldset': { borderColor: '#e91e63' }, '&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' } }, '& input': { fontSize: '0.85rem', padding: '10px 13px' } }}
            />
            <FormControl fullWidth>
              <Select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                displayEmpty
                sx={{ height: '42px', borderRadius: '10px', fontSize: '0.85rem', '& .MuiOutlinedInput-notchedOutline': { borderColor: '#e0e0e0', borderWidth: '2px' }, '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#e91e63' }, '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' }, '& .MuiSelect-select': { padding: '10px 13px' } }}
              >
                <MenuItem value="all">All Categories</MenuItem>
                {categories.map((cat) => {
                  const IconComp = (ICON_MAP[cat.icon] || ICON_MAP[DEFAULT_CAT_ICON]).icon;
                  return (
                    <MenuItem key={cat.id} value={cat.id} sx={{ display: 'flex', alignItems: 'center', gap: 1, fontSize: '0.85rem' }}>
                      <IconComp sx={{ fontSize: 16, color: (ICON_MAP[cat.icon] || ICON_MAP[DEFAULT_CAT_ICON]).color }} />
                      {cat.title}
                    </MenuItem>
                  );
                })}
              </Select>
            </FormControl>
          </Box>
        </CardContent>
      </Card>

      {/* Table - Continuing in next part due to length */}
      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ background: '#fafafa', borderBottom: '2px solid #e0e0e0' }}>
              <TableRow>
                {['Resource', 'Description', 'Details', 'Type', 'Status', 'Actions'].map((h) => (
                  <TableCell key={h} sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                    {h}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredCategories.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 6, color: '#999' }}>
                    <SchoolIcon sx={{ fontSize: 48, color: '#e0e0e0', mb: 1.5, display: 'block', mx: 'auto' }} />
                    <Typography sx={{ fontSize: '0.95rem', color: '#999', fontWeight: 500 }}>
                      {categories.length === 0 ? 'No learning categories yet. Create one to get started.' : 'No categories match your search.'}
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                filteredCategories.map((category) => (
                  <React.Fragment key={category.id}>
                    {/* Category Row */}
                    <TableRow
                      onClick={() => toggleCategory(category.id)}
                      sx={{ borderBottom: '2px solid #f0f0f0', '&:hover': { background: '#fafafa' }, backgroundColor: '#fafafa', cursor: 'pointer' }}
                    >
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                          <Box sx={{ color: '#bbb', display: 'flex', transition: 'transform 0.2s ease', transform: expandedCategories[category.id] ? 'rotate(90deg)' : 'rotate(0deg)', minWidth: '18px' }}>
                            <ChevronRightIcon sx={{ fontSize: 18 }} />
                          </Box>
                          <Box sx={{ width: 44, height: 44, borderRadius: '10px', background: iconGradient(category.icon), display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, boxShadow: '0 2px 8px rgba(0,0,0,0.08)' }}>
                            <DynamicIcon iconKey={category.icon} size={22} />
                          </Box>
                          <Box>
                            <Typography sx={{ fontWeight: 600, color: '#333', fontSize: '0.9rem' }}>{category.title}</Typography>
                            <Typography sx={{ fontSize: '0.75rem', color: '#999' }}>{category.tutorials.length} tutorial{category.tutorials.length !== 1 ? 's' : ''}</Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      <TableCell sx={{ fontSize: '0.85rem', color: '#666' }}>
                        {category.description ? category.description.substring(0, 50) + (category.description.length > 50 ? '…' : '') : '—'}
                      </TableCell>
                      <TableCell>
                        <Chip icon={<ArticleIcon sx={{ fontSize: '14px !important' }} />} label={`${category.tutorials.length}`} size="small" sx={{ background: 'rgba(102,126,234,0.12)', color: '#667eea', fontWeight: 600, fontSize: '0.75rem', height: '24px', borderRadius: '20px' }} />
                      </TableCell>
                      <TableCell>
                        <Chip label="Category" size="small" sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', color: 'white', fontWeight: 600, fontSize: '0.7rem', height: '24px', borderRadius: '20px' }} />
                      </TableCell>
                      <TableCell sx={{ fontSize: '0.85rem', color: '#333', fontWeight: 600 }}>Order #{category.display_order}</TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 0.75, justifyContent: 'flex-end' }}>
                          <Tooltip title="View details">
                            <Box onClick={(e) => { e.stopPropagation(); setViewModal({ open: true, item: category, type: 'category' }); }} sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}>
                              <VisibilityIcon />
                            </Box>
                          </Tooltip>
                          {canEdit && (
                            <Tooltip title="Edit category">
                              <Box
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setEditForm({ title: category.title || '', description: category.description || '', icon: category.icon || DEFAULT_CAT_ICON, displayOrder: category.display_order || 0, duration: '', url: '', isVideo: false });
                                  setEditModal({ open: true, item: category, type: 'category' });
                                }}
                                sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')}
                              >
                                <EditIcon />
                              </Box>
                            </Tooltip>
                          )}
                          {canDelete && (
                            <Tooltip title="Delete category">
                              <Box onClick={(e) => { e.stopPropagation(); setDeleteModal({ open: true, item: category, type: 'category', categoryId: null }); }} sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')}>
                                <DeleteIcon />
                              </Box>
                            </Tooltip>
                          )}
                        </Box>
                      </TableCell>
                    </TableRow>

                    {/* Tutorial Rows */}
                    {expandedCategories[category.id] && category.tutorials.map((tutorial) => {
                      const tutIconEntry = ICON_MAP[tutorial.icon] || ICON_MAP[DEFAULT_TUT_ICON];
                      const TutIcon = tutIconEntry.icon;
                      return (
                        <TableRow key={tutorial.id} sx={{ borderBottom: '2px solid #f0f0f0', '&:hover': { background: '#fafafa' }, '&:last-child': { borderBottom: 'none' }, background: '#fff' }}>
                          <TableCell sx={{ pl: 10 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                              <Box sx={{ width: 40, height: 40, borderRadius: '8px', background: `${tutIconEntry.color}12`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, border: `1px solid ${tutIconEntry.color}20` }}>
                                <TutIcon sx={{ fontSize: 18, color: tutIconEntry.color }} />
                              </Box>
                              <Box>
                                <Typography sx={{ fontWeight: 600, color: '#333', fontSize: '0.85rem' }}>{tutorial.title}</Typography>
                                <Typography sx={{ fontSize: '0.7rem', color: '#999' }}>Tutorial</Typography>
                              </Box>
                            </Box>
                          </TableCell>
                          <TableCell sx={{ fontSize: '0.85rem', color: '#666' }}>
                            {tutorial.description ? tutorial.description.substring(0, 50) + (tutorial.description.length > 50 ? '…' : '') : '—'}
                          </TableCell>
                          <TableCell>
                            <Typography sx={{ fontSize: '0.85rem', color: '#333' }}>{tutorial.duration || '—'}</Typography>
                          </TableCell>
                          <TableCell>
                            <Chip icon={tutorial.is_video ? <VideoIcon sx={{ fontSize: '14px !important' }} /> : <ArticleIcon sx={{ fontSize: '14px !important' }} />} label={tutorial.is_video ? 'Video' : 'Article'} size="small" sx={{ ...getTypeColor(tutorial.is_video), fontWeight: 600, fontSize: '0.7rem', height: '24px', borderRadius: '20px' }} />
                          </TableCell>
                          <TableCell>
                            <Chip label="Active" size="small" sx={{ background: 'rgba(76,175,80,0.12)', color: '#4CAF50', fontWeight: 600, fontSize: '0.7rem', height: '24px', borderRadius: '20px' }} />
                          </TableCell>
                          <TableCell>
                            <Box sx={{ display: 'flex', gap: 0.75, justifyContent: 'flex-end' }}>
                              <Tooltip title="Open in new tab">
                                <Box onClick={(e) => { e.stopPropagation(); window.open(tutorial.url, '_blank'); }} sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}>
                                  <OpenIcon />
                                </Box>
                              </Tooltip>
                              {canEdit && (
                                <Tooltip title="Edit tutorial">
                                  <Box
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setEditForm({ title: tutorial.title || '', description: tutorial.description || '', icon: tutorial.icon || DEFAULT_TUT_ICON, displayOrder: 0, duration: tutorial.duration || '', url: tutorial.url || '', isVideo: tutorial.is_video || false });
                                      setEditModal({ open: true, item: { ...tutorial, categoryId: category.id }, type: 'tutorial' });
                                    }}
                                    sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')}
                                  >
                                    <EditIcon />
                                  </Box>
                                </Tooltip>
                              )}
                              {canDelete && (
                                <Tooltip title="Delete tutorial">
                                  <Box onClick={(e) => { e.stopPropagation(); setDeleteModal({ open: true, item: tutorial, type: 'tutorial', categoryId: category.id }); }} sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')}>
                                    <DeleteIcon />
                                  </Box>
                                </Tooltip>
                              )}
                            </Box>
                          </TableCell>
                        </TableRow>
                      );
                    })}

                    {/* Add Tutorial Button Row */}
                    {expandedCategories[category.id] && canCreate && (
                      <TableRow sx={{ background: 'rgba(233,30,99,0.05)', borderBottom: '2px solid #f0f0f0' }}>
                        <TableCell colSpan={6} sx={{ p: 2, textAlign: 'center' }}>
                          <Button
                            size="small"
                            startIcon={<AddIcon sx={{ fontSize: '15px !important' }} />}
                            onClick={() => handleAddTutorial(category.id)}
                            disabled={saving}
                            sx={{ textTransform: 'none', fontWeight: 600, color: '#E91E63', fontSize: '0.8rem', borderRadius: '8px', px: 2.5, py: 1, '&:hover': { background: 'rgba(233,30,99,0.1)' }, '&:disabled': { opacity: 0.5 } }}
                          >
                            Add Tutorial
                          </Button>
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* Modals will be added in final part */}
    </Box>
  );
};

export default LearningResources;
