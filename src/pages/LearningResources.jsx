import React, { useState, useEffect, useRef } from 'react';
import {
  Box, Card, CardContent, Typography, Button, TextField, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, Chip, Dialog, DialogTitle,
  DialogContent, DialogActions, CircularProgress, Select, MenuItem,
  FormControl, Switch, Tooltip,
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
  collection, onSnapshot, addDoc, updateDoc, deleteDoc, doc, query, orderBy,
} from 'firebase/firestore';
import { db } from '../services/firebase';
import toast from 'react-hot-toast';
import { useAuth } from '../contexts/AuthContext';

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
    <Typography sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#333', mb: 1 }}>{label}</Typography>
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
            <Box onClick={() => onChange(key)} sx={{
              width: 36, height: 36, borderRadius: '8px',
              background: selected ? iconGradient(key) : `${entry.color}18`,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', transition: 'all 0.2s ease',
              border: selected ? `2px solid ${entry.color}` : '2px solid transparent',
              '&:hover': { background: iconGradient(key), transform: 'scale(1.1)' },
            }}>
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

  // Auto-expand first category on load
  useEffect(() => {
    if (categories.length > 0 && Object.keys(expandedCategories).length === 0) {
      setExpandedCategories({ [categories[0].id]: true });
    }
  }, [categories]); // eslint-disable-line react-hooks/exhaustive-deps

  // Real-time onSnapshot listener
  useEffect(() => {
    setLoading(true);
    const q = query(collection(db, 'learning_categories'), orderBy('display_order'));

    unsubscribeRef.current = onSnapshot(q, (snapshot) => {
      const data = snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
        tutorials: d.data().tutorials || [],
      }));
      setCategories(data);
      setLoading(false);
    }, (error) => {
      console.error('Error fetching learning categories:', error);
      toast.error('Failed to load learning resources');
      setLoading(false);
    });

    return () => {
      if (unsubscribeRef.current) unsubscribeRef.current();
    };
  }, []);

  // Filter & search
  useEffect(() => {
    let filtered = [...categories];
    if (categoryFilter !== 'all') filtered = filtered.filter((c) => c.id === categoryFilter);
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      filtered = filtered.filter(
        (c) =>
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
    setEditForm({
      title: '', description: '', icon: DEFAULT_CAT_ICON,
      displayOrder: Math.max(...categories.map((c) => c.display_order || 0), 0) + 1,
      duration: '', url: '', isVideo: false,
    });
    setAddModal({ open: true, type: 'category', categoryId: null });
  };

  const handleAddTutorial = (categoryId) => {
    setEditForm({
      title: '', description: '', icon: DEFAULT_TUT_ICON,
      displayOrder: 0, duration: '', url: '', isVideo: false,
    });
    setAddModal({ open: true, type: 'tutorial', categoryId });
  };

  const handleSaveEdit = async () => {
    if (!editForm.title.trim()) { toast.error('Title is required'); return; }
    if (editModal.type === 'tutorial' && !editForm.url.trim()) { toast.error('URL is required'); return; }
    try {
      if (editModal.type === 'category') {
        await updateDoc(doc(db, 'learning_categories', editModal.item.id), {
          title: editForm.title.trim(),
          description: editForm.description.trim(),
          icon: editForm.icon,
          display_order: parseInt(editForm.displayOrder) || 0,
        });
        
        // Notify all sellers about category update
        await addDoc(collection(db, 'notifications'), {
          user_id: 'broadcast_sellers',
          title: 'Learning Category Updated',
          description: `The learning category "${editForm.title.trim()}" has been updated with new information.`,
          category: 'SYSTEM',
          action_type: 'VIEW_LEARNING',
          action_data: { category_title: editForm.title.trim() },
          is_read: false,
          created_at: Date.now(),
          created_by: currentUser?.uid || 'admin',
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
              }
            : t
        );
        await updateDoc(doc(db, 'learning_categories', editModal.item.categoryId), { tutorials: updated });
        
        // Notify all sellers about tutorial update
        await addDoc(collection(db, 'notifications'), {
          user_id: 'broadcast_sellers',
          title: 'Tutorial Updated',
          description: `The tutorial "${editForm.title.trim()}" has been updated with new content.`,
          category: 'SYSTEM',
          action_type: 'VIEW_LEARNING',
          action_data: { tutorial_title: editForm.title.trim(), category_id: editModal.item.categoryId },
          is_read: false,
          created_at: Date.now(),
          created_by: currentUser?.uid || 'admin',
        });
        
        toast.success('Tutorial updated successfully');
      }
      setEditModal({ open: false, item: null, type: null });
    } catch (err) {
      console.error('Edit error:', err);
      toast.error(`Failed to update ${editModal.type}`);
    }
  };

  const handleSaveAdd = async () => {
    if (!editForm.title.trim()) { toast.error('Title is required'); return; }
    if (addModal.type === 'tutorial' && !editForm.url.trim()) { toast.error('URL is required'); return; }
    try {
      if (addModal.type === 'category') {
        await addDoc(collection(db, 'learning_categories'), {
          title:         editForm.title.trim(),
          description:   editForm.description.trim(),
          icon:          editForm.icon,
          display_order: parseInt(editForm.displayOrder) || 0,
          tutorials:     [],
        });
        
        // Notify all sellers about new learning category
        await addDoc(collection(db, 'notifications'), {
          user_id: 'broadcast_sellers',
          title: 'New Learning Category Available',
          description: `A new learning category "${editForm.title.trim()}" has been added. Check it out to improve your skills!`,
          category: 'SYSTEM',
          action_type: 'VIEW_LEARNING',
          action_data: { category_title: editForm.title.trim() },
          is_read: false,
          created_at: Date.now(),
          created_by: currentUser?.uid || 'admin',
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
        };
        await updateDoc(doc(db, 'learning_categories', addModal.categoryId), {
          tutorials: [...(cat.tutorials || []), newTutorial],
        });
        
        // Notify all sellers about new tutorial
        await addDoc(collection(db, 'notifications'), {
          user_id: 'broadcast_sellers',
          title: 'New Tutorial Added',
          description: `A new ${editForm.isVideo ? 'video' : 'article'} tutorial "${editForm.title.trim()}" has been added to the ${cat.title} category.`,
          category: 'SYSTEM',
          action_type: 'VIEW_LEARNING',
          action_data: { tutorial_title: editForm.title.trim(), category_id: addModal.categoryId },
          is_read: false,
          created_at: Date.now(),
          created_by: currentUser?.uid || 'admin',
        });
        
        toast.success('Tutorial created successfully');
      }
      setAddModal({ open: false, type: 'category', categoryId: null });
    } catch (err) {
      console.error('Add error:', err);
      toast.error(`Failed to create ${addModal.type}`);
    }
  };

  const handleDeleteConfirm = async () => {
    try {
      if (deleteModal.type === 'category') {
        const categoryTitle = deleteModal.item.title;
        await deleteDoc(doc(db, 'learning_categories', deleteModal.item.id));
        
        // Notify all sellers about category deletion
        await addDoc(collection(db, 'notifications'), {
          user_id: 'broadcast_sellers',
          title: 'Learning Category Removed',
          description: `The learning category "${categoryTitle}" has been removed from the platform.`,
          category: 'SYSTEM',
          action_type: 'VIEW_LEARNING',
          action_data: { category_title: categoryTitle },
          is_read: false,
          created_at: Date.now(),
          created_by: currentUser?.uid || 'admin',
        });
        
        toast.success('Category deleted successfully');
      } else {
        const tutorialTitle = deleteModal.item.title;
        const cat = categories.find((c) => c.id === deleteModal.categoryId);
        await updateDoc(doc(db, 'learning_categories', deleteModal.categoryId), {
          tutorials: cat.tutorials.filter((t) => t.id !== deleteModal.item.id),
        });
        
        // Notify all sellers about tutorial deletion
        await addDoc(collection(db, 'notifications'), {
          user_id: 'broadcast_sellers',
          title: 'Tutorial Removed',
          description: `The tutorial "${tutorialTitle}" has been removed from the platform.`,
          category: 'SYSTEM',
          action_type: 'VIEW_LEARNING',
          action_data: { tutorial_title: tutorialTitle, category_id: deleteModal.categoryId },
          is_read: false,
          created_at: Date.now(),
          created_by: currentUser?.uid || 'admin',
        });
        
        toast.success('Tutorial deleted successfully');
      }
      setDeleteModal({ open: false, item: null, type: null, categoryId: null });
    } catch (err) {
      console.error('Delete error:', err);
      toast.error('Failed to delete');
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
              <Switch checked={editForm.isVideo} onChange={(e) => setEditForm({ ...editForm, isVideo: e.target.checked })}
                sx={{ '& .MuiSwitch-switchBase.Mui-checked': { color: '#E91E63' }, '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': { backgroundColor: '#E91E63' } }} />
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
          <Typography sx={{ fontSize: '1.5rem', fontWeight: 700, color: '#333', mb: 0.5 }}>Learning Resources</Typography>
          <Typography sx={{ fontSize: '0.85rem', color: '#666' }}>Manage educational content and tutorials for sellers</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleAddCategory}
          sx={{ background: 'linear-gradient(45deg, #E91E63, #F06292)', borderRadius: '10px', fontWeight: 600, fontSize: '0.85rem', textTransform: 'none', py: 1.4, px: 3, boxShadow: 'none', '&:hover': { boxShadow: '0 5px 15px rgba(233,30,99,0.3)', transform: 'translateY(-2px)' } }}>
          Add Category
        </Button>
      </Box>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' }, gap: '20px', mb: '25px' }}>
        {[
          { label: 'Total Categories', value: totalCategories, Icon: SchoolIcon, bg: 'rgba(233,30,99,0.1)', color: '#E91E63', trend: 'Learning categories' },
          { label: 'Total Tutorials',  value: totalTutorials,  Icon: ArticleIcon, bg: 'rgba(156,39,176,0.1)', color: '#9C27B0', trend: 'Across all categories' },
          { label: 'Video Tutorials',  value: totalVideos,     Icon: VideoIcon,   bg: 'rgba(3,155,229,0.1)',  color: '#039BE5', trend: 'Video resources' },
          { label: 'Article Tutorials',value: totalArticles,   Icon: ArticleIcon, bg: 'rgba(251,140,0,0.1)',  color: '#FB8C00', trend: 'Written resources' },
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

      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none', mb: '20px' }}>
        <CardContent sx={{ p: 2.5 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' }, gap: 1.5 }}>
            <TextField
              fullWidth placeholder="Search by category or tutorial name..."
              value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{ startAdornment: <SearchIcon sx={{ fontSize: 18, color: '#999', mr: 0.75 }} /> }}
              sx={{ '& .MuiOutlinedInput-root': { height: '42px', borderRadius: '10px', '& fieldset': { borderColor: '#e0e0e0', borderWidth: '2px' }, '&:hover fieldset': { borderColor: '#e91e63' }, '&.Mui-focused fieldset': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' } }, '& input': { fontSize: '0.85rem', padding: '10px 13px' } }}
            />
            <FormControl fullWidth>
              <Select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} displayEmpty
                sx={{ height: '42px', borderRadius: '10px', fontSize: '0.85rem', '& .MuiOutlinedInput-notchedOutline': { borderColor: '#e0e0e0', borderWidth: '2px' }, '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: '#e91e63' }, '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#e91e63', boxShadow: '0 0 0 3px rgba(233,30,99,0.1)' }, '& .MuiSelect-select': { padding: '10px 13px' } }}>
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

      <Card sx={{ borderRadius: '15px', border: '2px solid #e0e0e0', boxShadow: 'none' }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ background: '#fafafa', borderBottom: '2px solid #e0e0e0' }}>
              <TableRow>
                {['Resource', 'Description', 'Details', 'Type', 'Status', 'Actions'].map((h) => (
                  <TableCell key={h} sx={{ fontSize: '0.8rem', fontWeight: 600, color: '#666', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{h}</TableCell>
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
              ) : filteredCategories.map((category) => (
                <React.Fragment key={category.id}>
                  <TableRow onClick={() => toggleCategory(category.id)}
                    sx={{ borderBottom: '2px solid #f0f0f0', '&:hover': { background: '#fafafa' }, backgroundColor: '#fafafa', cursor: 'pointer' }}>
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
                      <Chip icon={<ArticleIcon sx={{ fontSize: '14px !important' }} />} label={`${category.tutorials.length}`} size="small"
                        sx={{ background: 'rgba(102,126,234,0.12)', color: '#667eea', fontWeight: 600, fontSize: '0.75rem', height: '24px', borderRadius: '20px' }} />
                    </TableCell>
                    <TableCell>
                      <Chip label="Category" size="small" sx={{ background: 'linear-gradient(135deg, #667eea, #764ba2)', color: 'white', fontWeight: 600, fontSize: '0.7rem', height: '24px', borderRadius: '20px' }} />
                    </TableCell>
                    <TableCell sx={{ fontSize: '0.85rem', color: '#333', fontWeight: 600 }}>
                      Order #{category.display_order}
                    </TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 0.75, justifyContent: 'flex-end' }}>
                        <Tooltip title="View details">
                          <Box onClick={(e) => { e.stopPropagation(); setViewModal({ open: true, item: category, type: 'category' }); }} sx={actionBtnSx('rgba(3,155,229,0.12)', '#039BE5')}>
                            <VisibilityIcon />
                          </Box>
                        </Tooltip>
                        <Tooltip title="Edit category">
                          <Box onClick={(e) => {
                            e.stopPropagation();
                            setEditForm({ title: category.title || '', description: category.description || '', icon: category.icon || DEFAULT_CAT_ICON, displayOrder: category.display_order || 0, duration: '', url: '', isVideo: false });
                            setEditModal({ open: true, item: category, type: 'category' });
                          }} sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')}>
                            <EditIcon />
                          </Box>
                        </Tooltip>
                        <Tooltip title="Delete category">
                          <Box onClick={(e) => { e.stopPropagation(); setDeleteModal({ open: true, item: category, type: 'category', categoryId: null }); }} sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')}>
                            <DeleteIcon />
                          </Box>
                        </Tooltip>
                      </Box>
                    </TableCell>
                  </TableRow>

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
                          <Chip
                            icon={tutorial.is_video ? <VideoIcon sx={{ fontSize: '14px !important' }} /> : <ArticleIcon sx={{ fontSize: '14px !important' }} />}
                            label={tutorial.is_video ? 'Video' : 'Article'} size="small"
                            sx={{ ...getTypeColor(tutorial.is_video), fontWeight: 600, fontSize: '0.7rem', height: '24px', borderRadius: '20px' }}
                          />
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
                            <Tooltip title="Edit tutorial">
                              <Box onClick={(e) => {
                                e.stopPropagation();
                                setEditForm({ title: tutorial.title || '', description: tutorial.description || '', icon: tutorial.icon || DEFAULT_TUT_ICON, displayOrder: 0, duration: tutorial.duration || '', url: tutorial.url || '', isVideo: tutorial.is_video || false });
                                setEditModal({ open: true, item: { ...tutorial, categoryId: category.id }, type: 'tutorial' });
                              }} sx={actionBtnSx('rgba(92,107,192,0.12)', '#5C6BC0')}>
                                <EditIcon />
                              </Box>
                            </Tooltip>
                            <Tooltip title="Delete tutorial">
                              <Box onClick={(e) => { e.stopPropagation(); setDeleteModal({ open: true, item: tutorial, type: 'tutorial', categoryId: category.id }); }} sx={actionBtnSx('rgba(244,67,54,0.12)', '#F44336')}>
                                <DeleteIcon />
                              </Box>
                            </Tooltip>
                          </Box>
                        </TableCell>
                      </TableRow>
                    );
                  })}

                  {expandedCategories[category.id] && (
                    <TableRow sx={{ background: 'rgba(233,30,99,0.05)', borderBottom: '2px solid #f0f0f0' }}>
                      <TableCell colSpan={6} sx={{ p: 2, textAlign: 'center' }}>
                        <Button size="small" startIcon={<AddIcon sx={{ fontSize: '15px !important' }} />} onClick={() => handleAddTutorial(category.id)}
                          sx={{ textTransform: 'none', fontWeight: 600, color: '#E91E63', fontSize: '0.8rem', borderRadius: '8px', px: 2.5, py: 1, '&:hover': { background: 'rgba(233,30,99,0.1)' } }}>
                          Add Tutorial
                        </Button>
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* VIEW MODAL */}
      <Dialog open={viewModal.open} onClose={() => setViewModal({ open: false, item: null, type: null })} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: '18px', overflow: 'hidden' } }}>
        <Box sx={{ background: 'linear-gradient(135deg, #E91E63 0%, #F06292 100%)', px: 3, pt: 3, pb: 4, position: 'relative', overflow: 'hidden', '&::before': { content: '""', position: 'absolute', top: -30, right: -30, width: 130, height: 130, borderRadius: '50%', background: 'rgba(255,255,255,0.08)' }, '&::after': { content: '""', position: 'absolute', bottom: -20, left: '40%', width: 90, height: 90, borderRadius: '50%', background: 'rgba(255,255,255,0.06)' } }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, position: 'relative', zIndex: 1 }}>
            {viewModal.item && (
              <Box sx={{ width: 56, height: 56, borderRadius: '14px', background: 'rgba(255,255,255,0.2)', backdropFilter: 'blur(8px)', border: '1.5px solid rgba(255,255,255,0.35)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <DynamicIcon iconKey={viewModal.item.icon} size={28} color="white" />
              </Box>
            )}
            <Box>
              <Typography sx={{ fontSize: '0.7rem', fontWeight: 600, color: 'rgba(255,255,255,0.75)', textTransform: 'uppercase', letterSpacing: '1px', mb: 0.25 }}>
                {viewModal.type === 'category' ? 'Learning Category' : 'Tutorial'}
              </Typography>
              <Typography sx={{ fontSize: '1.2rem', fontWeight: 700, color: 'white', lineHeight: 1.2 }}>{viewModal.item?.title}</Typography>
            </Box>
          </Box>
        </Box>
        <DialogContent sx={{ px: 3, py: 3, background: '#fff' }}>
          {viewModal.item && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box sx={{ p: 2.5, background: '#fafafa', borderRadius: '12px', border: '1.5px solid #f0f0f0' }}>
                <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '0.8px', mb: 1 }}>Description</Typography>
                <Typography sx={{ fontSize: '0.88rem', color: '#555', lineHeight: 1.7 }}>
                  {viewModal.item.description || <span style={{ color: '#bbb', fontStyle: 'italic' }}>No description provided</span>}
                </Typography>
              </Box>

              {viewModal.type === 'category' && (
                <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                  <Box sx={{ p: 2, background: '#fafafa', borderRadius: '12px', border: '1.5px solid #f0f0f0' }}>
                    <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '0.8px', mb: 0.75 }}>Display Order</Typography>
                    <Typography sx={{ fontSize: '1.4rem', fontWeight: 700, color: '#333' }}>#{viewModal.item.display_order ?? 0}</Typography>
                  </Box>
                  <Box sx={{ p: 2, background: '#fafafa', borderRadius: '12px', border: '1.5px solid #f0f0f0' }}>
                    <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '0.8px', mb: 0.75 }}>Tutorials</Typography>
                    <Typography sx={{ fontSize: '1.4rem', fontWeight: 700, color: '#333' }}>
                      {viewModal.item.tutorials?.length ?? 0}
                      <Typography component="span" sx={{ fontSize: '0.78rem', color: '#999', fontWeight: 400, ml: 0.75 }}>resources</Typography>
                    </Typography>
                  </Box>
                </Box>
              )}

              {viewModal.type === 'tutorial' && (
                <>
                  <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                    <Box sx={{ p: 2, background: '#fafafa', borderRadius: '12px', border: '1.5px solid #f0f0f0' }}>
                      <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '0.8px', mb: 0.75 }}>Type</Typography>
                      <Chip
                        icon={viewModal.item.is_video ? <VideoIcon sx={{ fontSize: '14px !important' }} /> : <ArticleIcon sx={{ fontSize: '14px !important' }} />}
                        label={viewModal.item.is_video ? 'Video' : 'Article'} size="small"
                        sx={{ ...getTypeColor(viewModal.item.is_video), fontWeight: 600, fontSize: '0.75rem' }}
                      />
                    </Box>
                    <Box sx={{ p: 2, background: '#fafafa', borderRadius: '12px', border: '1.5px solid #f0f0f0' }}>
                      <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '0.8px', mb: 0.75 }}>Duration</Typography>
                      <Typography sx={{ fontSize: '0.9rem', fontWeight: 600, color: '#333' }}>{viewModal.item.duration || '—'}</Typography>
                    </Box>
                  </Box>
                  <Box onClick={() => window.open(viewModal.item.url, '_blank')}
                    sx={{ p: 2, background: 'linear-gradient(135deg, rgba(233,30,99,0.04), rgba(240,98,146,0.04))', borderRadius: '12px', border: '1.5px solid rgba(233,30,99,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, cursor: 'pointer', transition: 'all 0.2s ease', '&:hover': { borderColor: '#E91E63', background: 'rgba(233,30,99,0.07)' } }}>
                    <Box sx={{ overflow: 'hidden' }}>
                      <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#E91E63', textTransform: 'uppercase', letterSpacing: '0.8px', mb: 0.5 }}>Resource URL</Typography>
                      <Typography sx={{ fontSize: '0.82rem', color: '#E91E63', fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{viewModal.item.url}</Typography>
                    </Box>
                    <OpenIcon sx={{ fontSize: 18, color: '#E91E63', flexShrink: 0 }} />
                  </Box>
                </>
              )}

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 2, background: '#fafafa', borderRadius: '12px', border: '1.5px solid #f0f0f0' }}>
                <Box sx={{ width: 44, height: 44, borderRadius: '10px', background: iconGradient(viewModal.item.icon), display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
                  <DynamicIcon iconKey={viewModal.item.icon} size={22} />
                </Box>
                <Box>
                  <Typography sx={{ fontSize: '0.7rem', fontWeight: 700, color: '#999', textTransform: 'uppercase', letterSpacing: '0.8px' }}>Icon</Typography>
                  <Typography sx={{ fontSize: '0.85rem', fontWeight: 600, color: '#333' }}>{ICON_MAP[viewModal.item.icon]?.label || 'School'}</Typography>
                </Box>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3, pt: 0 }}>
          <Button fullWidth onClick={() => setViewModal({ open: false, item: null, type: null })} sx={{ ...cancelBtnSx, width: '100%' }}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* EDIT MODAL */}
      <Dialog open={editModal.open} onClose={() => setEditModal({ open: false, item: null, type: null })} maxWidth="sm" fullWidth PaperProps={{ sx: dialogPaper }}>
        <DialogTitle sx={titleSx}><EditIcon />Edit {editModal.type === 'category' ? 'Category' : 'Tutorial'}</DialogTitle>
        <DialogContent sx={{ pt: 3.5, pb: 3.5, px: 3 }}>{renderFormFields(false)}</DialogContent>
        <DialogActions sx={{ p: 2.5, gap: 1.5, borderTop: '1px solid #e0e0e0' }}>
          <Button onClick={() => setEditModal({ open: false, item: null, type: null })} sx={cancelBtnSx}>Cancel</Button>
          <Button onClick={handleSaveEdit} variant="contained" sx={primaryBtnSx}>Save Changes</Button>
        </DialogActions>
      </Dialog>

      {/* ADD MODAL */}
      <Dialog open={addModal.open} onClose={() => setAddModal({ open: false, type: 'category', categoryId: null })} maxWidth="sm" fullWidth PaperProps={{ sx: dialogPaper }}>
        <DialogTitle sx={titleSx}><AddIcon />Add {addModal.type === 'category' ? 'Category' : 'Tutorial'}</DialogTitle>
        <DialogContent sx={{ pt: 3.5, pb: 3.5, px: 3 }}>{renderFormFields(true)}</DialogContent>
        <DialogActions sx={{ p: 2.5, gap: 1.5, borderTop: '1px solid #e0e0e0' }}>
          <Button onClick={() => setAddModal({ open: false, type: 'category', categoryId: null })} sx={cancelBtnSx}>Cancel</Button>
          <Button onClick={handleSaveAdd} variant="contained" sx={primaryBtnSx}>Create {addModal.type === 'category' ? 'Category' : 'Tutorial'}</Button>
        </DialogActions>
      </Dialog>

      {/* DELETE MODAL */}
      <Dialog open={deleteModal.open} onClose={() => setDeleteModal({ open: false, item: null, type: null, categoryId: null })} maxWidth="xs" fullWidth PaperProps={{ sx: dialogPaper }}>
        <DialogTitle sx={titleSx}><DeleteIcon />Delete {deleteModal.type === 'category' ? 'Category' : 'Tutorial'}</DialogTitle>
        <DialogContent sx={{ pt: 4, pb: 0 }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <Box sx={{ width: 72, height: 72, borderRadius: '50%', background: 'rgba(244,67,54,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', mx: 'auto', mb: 2.5 }}>
              <WarningAmberRounded sx={{ fontSize: 36, color: '#F44336' }} />
            </Box>
            <Typography sx={{ fontSize: '1.05rem', fontWeight: 600, color: '#333', mb: 1 }}>
              Delete {deleteModal.type === 'category' ? 'Category' : 'Tutorial'}?
            </Typography>
            <Typography sx={{ fontSize: '0.9rem', color: '#666', lineHeight: 1.7 }}>
              Are you sure you want to delete <strong>{deleteModal.item?.title}</strong>?
              {deleteModal.type === 'category' && (
                <>
                  <br />
                  <Box sx={{ mt: 1.5, p: 1.5, background: 'rgba(244,67,54,0.08)', borderRadius: '8px', border: '1px solid rgba(244,67,54,0.2)' }}>
                    <Typography sx={{ fontSize: '0.8rem', color: '#F44336', fontWeight: 500 }}>
                      ⚠ All {deleteModal.item?.tutorials?.length || 0} tutorial{deleteModal.item?.tutorials?.length !== 1 ? 's' : ''} in this category will also be deleted.
                    </Typography>
                  </Box>
                </>
              )}
              <br />
              <Typography sx={{ fontSize: '0.8rem', color: '#999', mt: 1.5, fontStyle: 'italic' }}>This action cannot be undone.</Typography>
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2.5, gap: 1.5, borderTop: '1px solid #e0e0e0' }}>
          <Button onClick={() => setDeleteModal({ open: false, item: null, type: null, categoryId: null })} sx={cancelBtnSx}>Cancel</Button>
          <Button onClick={handleDeleteConfirm} variant="contained"
            sx={{ background: '#f44336', borderRadius: '10px', fontWeight: 600, textTransform: 'none', boxShadow: 'none', px: 3, '&:hover': { background: '#da190b', boxShadow: '0 5px 15px rgba(244,67,54,0.3)', transform: 'translateY(-2px)' } }}>
            Delete Forever
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default LearningResources;
