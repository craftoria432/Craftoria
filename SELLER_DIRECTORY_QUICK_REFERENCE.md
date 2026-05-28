# Seller Directory - Quick Reference

## What Was Built
A searchable seller directory that allows co-seller store owners to discover and invite other sellers without knowing their email addresses.

## Files
- **NEW**: `SellerDirectoryScreen.kt` - Browse and search sellers
- **MODIFIED**: `ManageCoSellerStoreScreen.kt` - Added Browse Sellers button

## User Flow
```
Members Tab
    ↓
[Browse Sellers] Button
    ↓
SellerDirectoryScreen (Search & Select)
    ↓
Select Seller → Auto-populate Email → Send Invitation
    ↓
Back to Members Tab
```

## Key Features
✅ Search by name or email (real-time)
✅ Excludes current user and existing members
✅ Professional card UI with avatars
✅ Loading and empty states
✅ Auto-sends invitation on selection
✅ No breaking changes

## Code Locations

### SellerDirectoryScreen.kt
```kotlin
// Main screen
@Composable
fun SellerDirectoryScreen(
    currentStoreId: String,
    currentUserId: String,
    onSellerSelected: (SellerDirectoryItem) -> Unit,
    onBackClick: () -> Unit
)

// Data class
data class SellerDirectoryItem(
    val userId: String,
    val name: String,
    val email: String,
    val profilePicture: String = ""
)
```

### ManageCoSellerStoreScreen.kt
```kotlin
// Added state
var showSellerDirectory by remember { mutableStateOf(false) }

// Updated MembersTab call
MembersTab(
    // ... existing params ...
    onBrowseSellers = { showSellerDirectory = true }
)

// Navigation
if (showSellerDirectory && currentStore != null) {
    SellerDirectoryScreen(
        currentStoreId = storeId,
        currentUserId = user.id,
        onSellerSelected = { seller ->
            // Send invitation with seller.email
        },
        onBackClick = { showSellerDirectory = false }
    )
}
```

## Firestore Queries
```kotlin
// Get store members
db.collection("co_seller_stores").document(storeId).get()

// Get all sellers
db.collection("users")
    .whereEqualTo("role", "seller")
    .get()
```

## UI Components
- **SellerDirectoryScreen**: Main screen with search and list
- **SellerDirectoryCard**: Individual seller card with avatar and select button
- **Browse Sellers Button**: Outlined button in MembersTab

## Testing
1. Open Manage Co-Seller Store → Members tab
2. Click "Browse Sellers" button
3. Search for a seller by name or email
4. Tap seller card
5. Verify invitation sent and email auto-populated

## No Breaking Changes
- New parameter has default value
- Existing invitation flow unchanged
- No data model modifications
- Fully backward compatible

## Next Steps
1. Test the feature end-to-end
2. Verify Firestore queries work
3. Check UI/UX on different screen sizes
4. Update SRS with FR-XX
5. Deploy to production
