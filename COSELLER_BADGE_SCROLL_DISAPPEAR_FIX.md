# Co-Seller Store Badge Scroll Disappear Fix

## Problem
The co-seller store badge on the Seller Orders screen disappears when scrolling down and reappears when scrolling back up. This creates a confusing user experience where badges flash in and out during scroll.

## Root Cause
The `CoSellerStoreBadge` composable had two critical issues:

### Issue 1: Unkeyed State
```kotlin
var storeName by remember { mutableStateOf("Store") }
var isLoading by remember { mutableStateOf(true) }
```

When using `remember` without a key in a LazyColumn, Compose may reuse the composable slot for different items during scroll. Without a key, the state doesn't follow the specific `storeId`, causing:
- State loss when items are recycled
- Wrong state being shown for different orders
- Badge disappearing because `isLoading` resets to `true`

### Issue 2: Conditional Rendering
```kotlin
if (!isLoading) {
    Surface { /* badge content */ }
}
```

The badge only rendered when `!isLoading`. During scroll:
1. Item scrolls off screen → composable disposed
2. Item scrolls back → composable recomposed
3. `isLoading` resets to `true` (unkeyed state)
4. Badge doesn't render (conditional check fails)
5. `LaunchedEffect` runs again
6. Badge appears after delay

## Solution

### Fix 1: Key State by storeId
```kotlin
// ✅ FIX: Key state by storeId to prevent loss during scroll
var storeName by remember(storeId) { mutableStateOf("Store") }
var isLoading by remember(storeId) { mutableStateOf(true) }
```

Now the state is tied to the specific `storeId`, ensuring:
- Each order's badge maintains its own state
- State persists across recompositions
- No state loss during scroll

### Fix 2: Always Render Badge
```kotlin
// ✅ FIX: Always show badge, use placeholder while loading
Surface {
    Row {
        Icon(...)
        if (isLoading) {
            Text("From: Loading...")  // Placeholder
        } else {
            Text("From: $storeName")  // Actual name
        }
    }
}
```

Benefits:
- Badge always visible (no disappearing)
- Shows loading state gracefully
- Smooth transition from placeholder to actual name
- Consistent layout (no layout shift)

## Technical Details

### Compose Recomposition in LazyColumn
LazyColumn optimizes performance by:
1. Only composing visible items
2. Recycling composable slots for off-screen items
3. Recomposing items when they scroll back into view

Without keyed state, `remember` creates new state on each recomposition, losing previous values.

### Why remember(key) Works
```kotlin
remember(storeId) { mutableStateOf(...) }
```

This tells Compose:
- "Remember this state for this specific storeId"
- "If storeId changes, create new state"
- "If storeId is the same, reuse existing state"

This ensures state survives:
- Scroll (item leaves and returns to screen)
- Recomposition (parent updates)
- Configuration changes (with proper state hoisting)

## User Experience

### Before Fix
```
[Scroll Down]
  ↓
[Badge Disappears] ← Confusing!
  ↓
[Scroll Up]
  ↓
[Badge Reappears] ← Inconsistent!
```

### After Fix
```
[Scroll Down]
  ↓
[Badge Stays Visible] ← Consistent!
  ↓
[Scroll Up]
  ↓
[Badge Still Visible] ← Reliable!
```

## Files Changed
- ✅ `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerOrdersScreen.kt`

## Testing
1. Open Seller Orders screen
2. Find orders with co-seller store badges
3. Scroll down slowly
4. ✅ Badges should remain visible throughout scroll
5. Scroll back up
6. ✅ Badges should still be visible
7. Scroll rapidly up and down
8. ✅ No flickering or disappearing badges

## Additional Benefits
- **Performance**: Reduces unnecessary Firestore calls (state is cached)
- **UX**: Smooth loading state with placeholder text
- **Reliability**: Consistent badge visibility across all scroll scenarios
- **Maintainability**: Clear state ownership with keyed remember

---

## Summary
Fixed co-seller store badge disappearing on scroll by:
1. Keying state with `remember(storeId)` to persist across recompositions
2. Always rendering badge with loading placeholder instead of conditional rendering

The badge now stays visible and consistent during all scroll operations.
