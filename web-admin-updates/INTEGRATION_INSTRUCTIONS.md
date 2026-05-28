# Web Admin Dashboard - Learning Resources Integration

## Files to Update

### 1. Replace `src/App.jsx`
Copy the content from `web-admin-updates/App.jsx` to your `src/App.jsx`

**What changed:**
- Added `LearningResources` import
- Added route: `<Route path="learning-resources" element={<LearningResources />} />`

### 2. Replace `src/components/layout/Sidebar.jsx`
Copy the content from `web-admin-updates/Sidebar.jsx` to your `src/components/layout/Sidebar.jsx`

**What changed:**
- Added `SchoolRoundedIcon` import
- Added "Learning Resources" menu item with deep purple color (#7B1FA2)

### 3. Update Firestore Security Rules
Copy the content from `web-admin-updates/firestore.rules` to your Firebase Console

**How to update:**
1. Go to Firebase Console
2. Navigate to Firestore Database
3. Click on "Rules" tab
4. Replace the entire content with the new rules
5. Click "Publish"

**What changed:**
- Replaced `allow read, write: if true;` with proper role-based security
- Added specific rules for:
  - Users (admin full access, users can read/update own data)
  - Products (public read, sellers manage own products)
  - Orders (buyers/sellers see own orders, admins see all)
  - Learning Resources (admins manage, sellers/buyers read)
  - Bookmarked Tutorials (users manage own bookmarks)
  - And all other collections with proper permissions

## Quick Integration Steps

1. **Copy the LearningResources page:**
   ```bash
   # The file is already created at:
   src/pages/LearningResources.jsx
   ```

2. **Update App.jsx:**
   - Replace your `src/App.jsx` with `web-admin-updates/App.jsx`
   - Or manually add the route for learning resources

3. **Update Sidebar.jsx:**
   - Replace your `src/components/layout/Sidebar.jsx` with `web-admin-updates/Sidebar.jsx`
   - Or manually add the menu item

4. **Update Firestore Rules:**
   - Go to Firebase Console → Firestore → Rules
   - Copy content from `web-admin-updates/firestore.rules`
   - Paste and publish

5. **Test the integration:**
   - Login to admin dashboard
   - Click "Learning Resources" in sidebar
   - Create a category
   - Add tutorials to the category
   - Verify data appears in mobile app

## Security Rules Summary

### Admin Access
- Full read/write access to all collections
- Can manage users, products, orders, reports, learning resources

### Seller Access
- Can manage own products
- Can view and update own orders
- Can read learning resources
- Can create co-seller stores

### Buyer Access
- Can create orders
- Can view own orders
- Can manage cart and wishlist
- Can read learning resources (optional)

### Learning Resources Specific Rules
```javascript
match /learning_categories/{categoryId} {
  // Admins can read/write all learning resources
  allow read, write: if isAdmin();
  
  // Sellers can read learning resources
  allow read: if isSeller();
  
  // Buyers can also read (if you want to allow buyers access)
  allow read: if isBuyer();
}

match /bookmarked_tutorials/{bookmarkId} {
  // Users can read their own bookmarks
  allow read: if isAuthenticated() && isOwner(resource.data.user_id);
  
  // Users can create their own bookmarks
  allow create: if isAuthenticated() && 
    request.auth.uid == request.resource.data.user_id;
  
  // Users can delete their own bookmarks
  allow delete: if isAuthenticated() && isOwner(resource.data.user_id);
}
```

## Verification Checklist

- [ ] App.jsx updated with learning resources route
- [ ] Sidebar.jsx updated with menu item
- [ ] Firestore rules updated and published
- [ ] Can access /learning-resources page
- [ ] Can create categories
- [ ] Can add tutorials
- [ ] Can edit/delete categories and tutorials
- [ ] Mobile app can read the data
- [ ] Mobile app bookmark functionality works
- [ ] Security rules prevent unauthorized access

## Troubleshooting

### "Permission denied" errors
- Make sure Firestore rules are published
- Verify user has admin role in Firestore users collection
- Check browser console for specific error messages

### Menu item not showing
- Clear browser cache
- Verify Sidebar.jsx was updated correctly
- Check for JavaScript errors in console

### Mobile app not showing data
- Verify Firestore rules allow read access for sellers
- Check that data structure matches expected format
- Ensure mobile app is connected to same Firebase project

## Support

If you encounter any issues:
1. Check browser console for errors
2. Verify Firebase configuration
3. Ensure all files are in correct locations
4. Test with a fresh browser session
