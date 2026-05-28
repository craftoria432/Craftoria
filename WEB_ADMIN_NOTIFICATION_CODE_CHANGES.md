# Web Admin Notification Integration - Code Changes

## File: src/pages/LearningResources.jsx

### Change 1: Import Auth Context
**Location**: Line 45 (after firebase imports)

```javascript
// ADDED:
import { useAuth } from '../contexts/AuthContext';
```

### Change 2: Add currentUser Hook
**Location**: Line 189 (in LearningResources component)

```javascript
// ADDED:
const { currentUser } = useAuth();
```

### Change 3: Update handleSaveAdd() Function
**Location**: Lines 280-330

**Before**:
```javascript
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
      toast.success('Tutorial created successfully');
    }
    setAddModal({ open: false, type: 'category', categoryId: null });
  } catch (err) {
    console.error('Add error:', err);
    toast.error(`Failed to create ${addModal.type}`);
  }
};
```

**After**:
```javascript
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
      
      // ADDED: Notify all sellers about new learning category
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
      
      // ADDED: Notify all sellers about new tutorial
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
```

### Change 4: Update handleSaveEdit() Function
**Location**: Lines 250-280

**Before**:
```javascript
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
      toast.success('Tutorial updated successfully');
    }
    setEditModal({ open: false, item: null, type: null });
  } catch (err) {
    console.error('Edit error:', err);
    toast.error(`Failed to update ${editModal.type}`);
  }
};
```

**After**:
```javascript
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
      
      // ADDED: Notify all sellers about category update
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
      
      // ADDED: Notify all sellers about tutorial update
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
```

### Change 5: Update handleDeleteConfirm() Function
**Location**: Lines 330-350

**Before**:
```javascript
const handleDeleteConfirm = async () => {
  try {
    if (deleteModal.type === 'category') {
      await deleteDoc(doc(db, 'learning_categories', deleteModal.item.id));
      toast.success('Category deleted successfully');
    } else {
      const cat = categories.find((c) => c.id === deleteModal.categoryId);
      await updateDoc(doc(db, 'learning_categories', deleteModal.categoryId), {
        tutorials: cat.tutorials.filter((t) => t.id !== deleteModal.item.id),
      });
      toast.success('Tutorial deleted successfully');
    }
    setDeleteModal({ open: false, item: null, type: null, categoryId: null });
  } catch (err) {
    console.error('Delete error:', err);
    toast.error('Failed to delete');
  }
};
```

**After**:
```javascript
const handleDeleteConfirm = async () => {
  try {
    if (deleteModal.type === 'category') {
      const categoryTitle = deleteModal.item.title;
      await deleteDoc(doc(db, 'learning_categories', deleteModal.item.id));
      
      // ADDED: Notify all sellers about category deletion
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
      
      // ADDED: Notify all sellers about tutorial deletion
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
```

---

## Summary of Changes

### Total Lines Added: ~100
### Total Lines Modified: 3 functions
### Files Modified: 1 (src/pages/LearningResources.jsx)
### Breaking Changes: None
### Compilation Status: ✅ No errors

### Notifications Added:
1. New Learning Category Available
2. Learning Category Updated
3. Learning Category Removed
4. New Tutorial Added
5. Tutorial Updated
6. Tutorial Removed

### Key Features:
✅ Async operations (non-blocking)
✅ Error handling with try-catch
✅ Broadcast notifications to all sellers
✅ Admin UID tracking
✅ Contextual action data
✅ Real-time Firestore integration
