# Learning Resources Integration Guide

## Files Created

### 1. src/pages/LearningResources.jsx
✅ Complete admin dashboard page for managing learning resources
- Create, edit, delete categories
- Create, edit, delete tutorials within categories
- Drag and drop ordering
- Video/article type indicators
- Real-time Firestore integration

## Integration Steps

### Step 1: Add Route to App.jsx or Router Configuration

Add this route to your routing configuration:

```jsx
import LearningResources from './pages/LearningResources';

// In your routes:
<Route path="/learning-resources" element={<LearningResources />} />
```

### Step 2: Update Sidebar.jsx

Add the import:
```jsx
import SchoolRoundedIcon from '@mui/icons-material/SchoolRounded';
```

Add this menu item to the `menuItems` array (after Reports & Complaints):
```jsx
{
  title: 'Learning Resources',
  path: '/learning-resources',
  icon: <SchoolRoundedIcon fontSize="small" />,
  color: '#7B1FA2',  // purple
},
```

### Step 3: Firestore Structure

The page works with this Firestore structure:

```
learning_categories/
  {categoryId}/
    - title: string
    - icon: string (emoji)
    - display_order: number
    - tutorials: array [
        {
          id: string
          title: string
          description: string
          duration: string
          icon: string (emoji)
          url: string
          category_id: string
          is_video: boolean
          created_at: number (timestamp)
        }
      ]
```

## Features

### Category Management
- ✅ Create new categories with title, icon (emoji), and display order
- ✅ Edit existing categories
- ✅ Delete categories (with confirmation)
- ✅ Expandable accordion view
- ✅ Tutorial count display

### Tutorial Management
- ✅ Add tutorials to categories
- ✅ Edit tutorial details
- ✅ Delete tutorials (with confirmation)
- ✅ Video/article type toggle
- ✅ Duration tracking
- ✅ External URL links
- ✅ Rich descriptions

### UI Features
- ✅ Statistics dashboard (total categories, tutorials, videos)
- ✅ Responsive grid layout
- ✅ Material Design components
- ✅ Toast notifications for all actions
- ✅ Loading states
- ✅ Empty states
- ✅ Context menus
- ✅ Confirmation dialogs

### Mobile App Integration
The mobile app (Android) already has:
- ✅ Complete UI for viewing learning resources
- ✅ Search functionality
- ✅ Bookmark system
- ✅ Category expansion
- ✅ External link warnings
- ✅ Real-time Firestore sync

## Usage

1. Navigate to "Learning Resources" in the admin sidebar
2. Click "Add Category" to create a new category
3. Enter category details (title, emoji icon, display order)
4. Click on a category to expand it
5. Click "Add Tutorial" within a category
6. Fill in tutorial details:
   - Title (required)
   - Description
   - Duration (e.g., "10 min")
   - Icon (emoji)
   - URL (required)
   - Toggle "This is a video tutorial" if applicable
7. Save and the content will be immediately available in the mobile app

## Example Data

### Sample Category:
```json
{
  "title": "Getting Started",
  "icon": "🚀",
  "display_order": 0,
  "tutorials": []
}
```

### Sample Tutorial:
```json
{
  "id": "tutorial_1234567890",
  "title": "How to Create Your First Product",
  "description": "Learn the basics of adding products to your store",
  "duration": "5 min",
  "icon": "📦",
  "url": "https://youtube.com/watch?v=example",
  "category_id": "category_id_here",
  "is_video": true,
  "created_at": 1234567890000
}
```

## Security Rules (Firestore)

Add these rules to your Firestore security rules:

```javascript
match /learning_categories/{categoryId} {
  // Admins can read/write
  allow read, write: if request.auth != null && 
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
  
  // Sellers can only read
  allow read: if request.auth != null && 
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'seller';
}

match /bookmarked_tutorials/{bookmarkId} {
  // Users can manage their own bookmarks
  allow read, write: if request.auth != null && 
    request.auth.uid == resource.data.user_id;
  
  // Allow create if user_id matches
  allow create: if request.auth != null && 
    request.auth.uid == request.resource.data.user_id;
}
```

## Testing Checklist

- [ ] Create a new category
- [ ] Edit category details
- [ ] Delete a category
- [ ] Add a tutorial to a category
- [ ] Edit tutorial details
- [ ] Toggle video/article type
- [ ] Delete a tutorial
- [ ] Verify data appears in mobile app
- [ ] Test search in mobile app
- [ ] Test bookmark functionality in mobile app
- [ ] Verify responsive design on different screen sizes

## Notes

- The mobile app automatically syncs with Firestore changes
- No app rebuild required when adding new content
- Emojis are used for icons for simplicity and cross-platform compatibility
- URLs can be YouTube videos, blog posts, documentation, or any web content
- Display order determines the sequence of categories (lower numbers first)
