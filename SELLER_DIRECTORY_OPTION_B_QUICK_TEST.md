# Quick Test Guide: Seller Directory Option B

## What to Test

### 1. Directory Access
```
ManageCoSellerStoreScreen → MembersTab → [Browse Sellers] button
```
✅ Should open SellerDirectoryScreen with list of sellers

### 2. Search Functionality
```
Type in search field: "john" or "john@email.com"
```
✅ Should filter sellers by name or email in real-time

### 3. Profile View (NEW)
```
Click [Profile] button on any seller card
```
✅ Should open SellerPublicProfileScreen
✅ Should show seller info, products, verification status
✅ Should show [Chat] and [Invite] buttons

### 4. Invite from Profile (NEW)
```
On SellerPublicProfileScreen → Click [Invite] button
```
✅ Should send invitation
✅ Should return to directory
✅ Seller should no longer appear in list (if now a member)

### 5. Invite from Directory
```
On SellerDirectoryScreen → Click [Invite] button on card
```
✅ Should send invitation
✅ Should return to directory
✅ Seller should no longer appear in list (if now a member)

### 6. Back Navigation
```
From profile: Click back arrow
From directory: Click back arrow
```
✅ Should return to previous screen
✅ Search query should be preserved in directory

---

## Expected Behavior

### Directory Screen
- Shows all sellers except current user and store members
- Search works in real-time
- Each card has two buttons: [Profile] and [Invite]
- [Profile] button is outlined (secondary style)
- [Invite] button is filled (primary style)

### Profile Screen
- Shows full seller profile
- Shows seller products in grid
- Shows verification badges
- Shows [Chat] button (white, left side)
- Shows [Invite] button (primary, right side) - ONLY when called from directory
- Back button returns to directory

### After Invitation
- Seller is added to store members
- Seller no longer appears in directory
- Success message shown
- Directory refreshes automatically

---

## Compilation Check

Run diagnostics on:
```
✅ app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/SellerDirectoryScreen.kt
✅ app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerPublicProfileScreen.kt
```

Both should show: **No diagnostics found**

---

## Key Files

- **SellerDirectoryScreen.kt** - Directory with search and two-button cards
- **SellerPublicProfileScreen.kt** - Profile with optional invite button
- **ManageCoSellerStoreScreen.kt** - Browse Sellers button integration

---

## What's New in Option B

| Feature | Status |
|---------|--------|
| Browse sellers | ✅ |
| Search by name/email | ✅ |
| View seller profile | ✅ NEW |
| View seller products | ✅ NEW |
| See verification status | ✅ NEW |
| Chat with seller | ✅ NEW |
| Invite from profile | ✅ NEW |
| Invite from directory | ✅ |
| Auto-populate email | ✅ |

---

## Troubleshooting

**Profile screen doesn't open?**
- Check that `onViewProfile` callback is passed to SellerDirectoryCard
- Verify `selectedSellerForProfile` state is being set

**Invite button not showing on profile?**
- Check that `onInviteClick` parameter is passed to SellerPublicProfileScreen
- Verify it's not null

**Seller still appears after invitation?**
- Check that store members list is being refreshed
- Verify invitation was actually sent

**Search not working?**
- Check that search query is being updated
- Verify filter logic in `filteredSellers`

---

## Performance Notes

- Sellers list is loaded once on screen open
- Search is done in-memory (no Firebase queries)
- Profile loads on demand when clicked
- No unnecessary re-compositions

---

## UI/UX Notes

- Profile button: Outlined style (secondary)
- Invite button: Filled style (primary)
- Both buttons same height (36dp on card, 44dp on profile)
- Icons included for better UX
- Proper spacing between buttons
- Responsive layout on different screen sizes
