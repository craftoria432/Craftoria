# Seller Directory Search Box Alignment Fix

## Issue
The search box in the Seller Directory screen was not properly aligned and visually inconsistent with other screens. It was missing horizontal padding, causing it to extend edge-to-edge without proper spacing.

## Root Cause
The `StandardizedOutlinedTextFieldCompact` component was placed directly in a Column without any padding wrapper. The Surface container also didn't apply padding, resulting in:
- No left/right margins
- Misaligned with the seller cards below (which have 14dp padding)
- Inconsistent with other screens' search implementations

## Solution
Wrapped the search field in a Box with proper padding:

```kotlin
// Before:
Surface(color = Color.White, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
    Column {
        StandardizedOutlinedTextFieldCompact(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search by name or email",
            singleLine = true,
            minHeight = 48
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

// After:
Surface(color = Color.White, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            StandardizedOutlinedTextFieldCompact(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search by name or email",
                singleLine = true,
                minHeight = 48
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}
```

## Changes Made
**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/SellerDirectoryScreen.kt`

- Added Box wrapper around `StandardizedOutlinedTextFieldCompact`
- Applied horizontal padding: 14.dp (matches seller cards padding)
- Applied vertical padding: 10.dp (consistent spacing)
- Maintains fillMaxWidth() for proper layout

## Visual Improvements
✅ Search box now has proper left/right margins (14dp)
✅ Consistent with seller card padding below
✅ Proper vertical spacing (10dp top/bottom)
✅ Aligns with other screens' search implementations
✅ Professional, polished appearance

## Consistency
This alignment now matches:
- Buyer Search Screen
- All Stores Screen
- Other screens with search functionality

## Testing Checklist
- [ ] Search box displays with proper left/right margins
- [ ] Search box aligns with seller cards below
- [ ] Divider line extends full width
- [ ] Search functionality still works
- [ ] Placeholder text is visible and properly positioned
- [ ] Focus state works correctly
- [ ] Text input is properly padded

## Status
**COMPLETE** - Search box alignment has been fixed and is now visually consistent.
