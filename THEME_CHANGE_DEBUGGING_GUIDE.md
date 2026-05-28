# Theme Change - Debugging Guide

## Problem
Theme selection UI works (checkmark appears, theme saves to Firebase), but app colors don't change across all screens.

## Root Cause Analysis

The issue is likely one of these:

1. **ThemeManager instance mismatch** - Different instances being used
2. **CraftoriaTheme not recomposing** - StateFlow changes not triggering recomposition
3. **Theme colors not being applied to all screens** - Screens using hardcoded colors instead of theme colors

## Debugging Steps

### Step 1: Check Logs
After selecting a theme in Settings, check Logcat for these messages:

```
✅ Theme changed to: OCEAN
🎨 Recomposing with theme: OCEAN
```

**If you see these logs:**
- Theme is being set correctly
- CraftoriaTheme is recomposing
- Problem is likely in individual screens not using theme colors

**If you DON'T see these logs:**
- Theme is not being set
- Check if selectTheme() is being called
- Check if themeManager.setTheme() is being called

### Step 2: Verify Singleton Instance
The ThemeManager uses a singleton pattern. Verify it's the same instance:

```kotlin
// In SettingsScreen
val themeManager1 = ThemeManager.getInstance()

// In CraftoriaTheme
val themeManager2 = ThemeManager.getInstance()

// These should be the same object
assert(themeManager1 === themeManager2)
```

### Step 3: Check Screen Color Usage

Each screen should use theme colors from `Color.kt` or Material3 theme:

**Good - Uses theme colors:**
```kotlin
Box(
    modifier = Modifier.background(Primary)  // Uses Primary from Color.kt
)
```

**Bad - Uses hardcoded colors:**
```kotlin
Box(
    modifier = Modifier.background(Color(0xFFE91E63))  // Hardcoded pink
)
```

### Step 4: Verify Material3 Integration

Screens should use `MaterialTheme.colorScheme` for colors:

```kotlin
// Good - Uses Material3 theme
Box(
    modifier = Modifier.background(MaterialTheme.colorScheme.primary)
)

// Also good - Uses Color.kt which is updated by CraftoriaTheme
Box(
    modifier = Modifier.background(Primary)
)
```

## Files Modified for Debugging

### 1. ThemeManager.kt
Added logging when theme changes:
```kotlin
android.util.Log.d("ThemeManager", "✅ Theme changed to: ${themeType.name}")
```

### 2. Theme.kt
Added logging when recomposing:
```kotlin
android.util.Log.d("CraftoriaTheme", "🎨 Recomposing with theme: ${currentTheme.value.name}")
```

### 3. NavGraph.kt
Ensured singleton instance is used:
```kotlin
val themeManager = com.gcuf.craftoria.ui.theme.ThemeManager.getInstance()
```

## Expected Behavior

### When selecting a theme:
1. ✅ SettingsScreen shows loading indicator
2. ✅ Theme saves to Firebase
3. ✅ Checkmark appears on selected theme
4. ✅ Log shows: "✅ Theme changed to: OCEAN"
5. ✅ Log shows: "🎨 Recomposing with theme: OCEAN"
6. ✅ All screens recompose with new colors
7. ✅ App colors change immediately

### When navigating away and back:
1. ✅ Theme persists
2. ✅ Colors remain the same
3. ✅ No flash of old colors

### When restarting app:
1. ✅ Theme loads from Firebase
2. ✅ App starts with correct colors
3. ✅ No flash of default theme

## Common Issues & Solutions

### Issue: Logs show theme changed but colors don't update
**Solution**: Check if screens are using hardcoded colors instead of theme colors
- Search for `Color(0xFF...)` in screen files
- Replace with `Primary`, `Secondary`, etc. from Color.kt
- Or use `MaterialTheme.colorScheme.primary`

### Issue: Logs don't show theme changed
**Solution**: Check if selectTheme() is being called
- Add log in SettingsScreen: `Log.d("SettingsScreen", "selectTheme called with: $theme")`
- Verify onClick handler is working
- Check if themeViewModel is null

### Issue: Logs show recomposing but with old theme
**Solution**: Check if StateFlow is updating
- Verify `_currentTheme.value = themeType` is being executed
- Check if collectAsState() is working correctly
- Verify no exceptions in setTheme()

### Issue: Only some screens change color
**Solution**: Some screens might be using hardcoded colors
- Check each screen for hardcoded Color() values
- Replace with theme colors from Color.kt
- Ensure all screens use MaterialTheme.colorScheme

## Testing Checklist

- [ ] Select Rose theme → Check logs for "✅ Theme changed to: ROSE"
- [ ] Check logs for "🎨 Recomposing with theme: ROSE"
- [ ] Verify all screens show pink colors
- [ ] Select Ocean theme → Check logs
- [ ] Verify all screens show blue colors
- [ ] Select Midnight theme → Check logs
- [ ] Verify all screens show purple colors
- [ ] Close and reopen app → Verify theme persists
- [ ] Check Logcat for any errors

## Next Steps

1. **Run the app and select a theme**
2. **Check Logcat for the debug messages**
3. **If logs show theme changed but colors don't update:**
   - Identify which screens have hardcoded colors
   - Replace hardcoded colors with theme colors
4. **If logs don't show theme changed:**
   - Add more logging to selectTheme()
   - Verify themeViewModel is not null
   - Check if onClick is being called

## Files to Check for Hardcoded Colors

Search for `Color(0xFF` in these files:
- HomeScreen.kt
- ProductDetailsScreen.kt
- CartScreen.kt
- CheckoutScreen.kt
- MyOrdersScreen.kt
- SellerDashboardScreen.kt
- ProfileScreen.kt
- And all other screen files

Replace with appropriate theme colors from Color.kt:
- `Primary` (main theme color)
- `PrimaryLight` (lighter variant)
- `Secondary`
- `Background`
- `TextPrimary`
- `TextSecondary`
- `Error`
- `Success`
- `Warning`
