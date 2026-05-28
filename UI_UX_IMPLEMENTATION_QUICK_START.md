# UI/UX Implementation Quick Start Guide

## Overview
This guide provides quick reference for implementing design system changes across all screens.

---

## COMMON REPLACEMENTS

### 1. Button Replacement
**OLD**:
```kotlin
Button(
    onClick = { },
    modifier = Modifier.height(40.dp)
) {
    Text("Click Me")
}
```

**NEW**:
```kotlin
CraftoriaButton(
    text = "Click Me",
    onClick = { },
    modifier = Modifier.fillMaxWidth()
)
```

---

### 2. Text Field Replacement
**OLD**:
```kotlin
TextField(
    value = text,
    onValueChange = { text = it },
    modifier = Modifier.height(40.dp)
)
```

**NEW**:
```kotlin
CraftoriaTextField(
    value = text,
    onValueChange = { text = it },
    label = "Enter text",
    modifier = Modifier.fillMaxWidth()
)
```

---

### 3. Badge Replacement
**OLD**:
```kotlin
Badge(
    containerColor = Color.Red,
    modifier = Modifier.size(20.dp)
) {
    Text("5")
}
```

**NEW**:
```kotlin
UnifiedBadgeComponent.StatusBadge(
    status = "Pending",
    modifier = Modifier
)
```

---

### 4. Dialog Replacement
**OLD**:
```kotlin
AlertDialog(
    onDismissRequest = { },
    title = { Text("Title") },
    text = { Text("Message") },
    confirmButton = { Button(onClick = { }) { Text("OK") } }
)
```

**NEW**:
```kotlin
UnifiedDialogComponent.ConfirmationDialog(
    title = "Title",
    message = "Message",
    onConfirm = { },
    onDismiss = { }
)
```

---

### 5. Filter Tab Replacement
**OLD**:
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    categories.forEach { category ->
        Button(
            onClick = { selectedCategory = category },
            modifier = Modifier.height(40.dp)
        ) {
            Text(category)
        }
    }
}
```

**NEW**:
```kotlin
FilterTabComponent(
    items = categories,
    selectedItem = selectedCategory,
    onItemSelected = { selectedCategory = it },
    modifier = Modifier.fillMaxWidth()
)
```

---

### 6. Empty State Replacement
**OLD**:
```kotlin
Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    Icon(Icons.Default.ShoppingCart, contentDescription = null)
    Text("No products")
}
```

**NEW**:
```kotlin
EmptyStateComponent.NoProducts(
    modifier = Modifier.fillMaxSize()
)
```

---

## SPACING STANDARDS

### Apply 8dp Grid
```kotlin
// Padding
Modifier.padding(8.dp)   // xs
Modifier.padding(12.dp)  // sm
Modifier.padding(16.dp)  // md
Modifier.padding(24.dp)  // lg

// Spacing
Spacer(modifier = Modifier.height(8.dp))
Spacer(modifier = Modifier.height(16.dp))
Spacer(modifier = Modifier.height(24.dp))

// Arrangement
Arrangement.spacedBy(8.dp)
Arrangement.spacedBy(12.dp)
Arrangement.spacedBy(16.dp)
```

---

## BORDER RADIUS STANDARDS

### Apply Consistent Radius
```kotlin
// Cards
RoundedCornerShape(12.dp)

// Text Fields
RoundedCornerShape(10.dp)

// Buttons
RoundedCornerShape(12.dp)

// Tabs
RoundedCornerShape(8.dp)

// Badges
RoundedCornerShape(20.dp)  // Pill shape
```

---

## TYPOGRAPHY STANDARDS

### Use Consistent Text Styles
```kotlin
// Titles
Text(
    text = "Title",
    fontSize = 16.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 24.sp
)

// Body
Text(
    text = "Body text",
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 20.sp
)

// Labels
Text(
    text = "Label",
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 18.sp
)
```

---

## COLOR STANDARDS

### Use Theme Colors
```kotlin
// Primary
color = Primary          // #FFE91E63
color = PrimaryLight     // #FFF06292
color = PrimaryDark      // #FFC2185B

// Text
color = TextPrimary      // #FF333333
color = TextSecondary    // #FF666666
color = TextLight        // #FFAAAAAA

// Status
color = Success          // #FF4CAF50
color = Warning          // #FFFFA726
color = Error            // #FFF44336
color = Info             // #FF2196F3

// Background
color = Background       // #FFFFFFFF
color = BackgroundSecondary  // #FFF8F9FA
color = BackgroundLight  // #FFFAFAFA
```

---

## COMPONENT HEIGHT STANDARDS

### Standard Heights
```kotlin
// Buttons
Modifier.height(48.dp)   // Primary button
Modifier.height(36.dp)   // Small button

// Text Fields
Modifier.height(48.dp)   // Standard input

// Top Bar
Modifier.height(64.dp)   // Top navigation

// Bottom Navigation
Modifier.height(64.dp)   // Bottom navigation

// Filter Tabs
Modifier.height(40.dp)   // Tab height

// Badges
Modifier.height(24.dp)   // Badge height
```

---

## IMPLEMENTATION CHECKLIST FOR EACH SCREEN

### Step 1: Identify Components
- [ ] Find all Button components
- [ ] Find all TextField components
- [ ] Find all Badge components
- [ ] Find all Dialog components
- [ ] Find all Filter/Tab components
- [ ] Find all empty state sections

### Step 2: Replace Components
- [ ] Replace Button → CraftoriaButton
- [ ] Replace TextField → CraftoriaTextField
- [ ] Replace Badge → UnifiedBadgeComponent
- [ ] Replace Dialog → UnifiedDialogComponent
- [ ] Replace Tabs → FilterTabComponent
- [ ] Replace Empty States → EmptyStateComponent

### Step 3: Fix Spacing
- [ ] Apply 8dp grid spacing
- [ ] Fix padding (12dp, 16dp, 24dp)
- [ ] Fix gaps between items (8dp, 12dp, 16dp)
- [ ] Fix margins

### Step 4: Fix Border Radius
- [ ] Cards: 12dp
- [ ] Text Fields: 10dp
- [ ] Buttons: 12dp
- [ ] Tabs: 8dp
- [ ] Badges: 20dp

### Step 5: Fix Typography
- [ ] Titles: 16sp, SemiBold, 24sp line height
- [ ] Body: 14sp, Normal, 20sp line height
- [ ] Labels: 12sp, Medium, 18sp line height
- [ ] Captions: 10sp, Normal, 14sp line height

### Step 6: Fix Colors
- [ ] Use Primary for main actions
- [ ] Use TextPrimary for main text
- [ ] Use TextSecondary for secondary text
- [ ] Use Success/Warning/Error for status
- [ ] Use Background colors for surfaces

### Step 7: Verify
- [ ] Compile without errors
- [ ] Test on small screen (< 360dp)
- [ ] Test on medium screen (360-600dp)
- [ ] Test on large screen (> 600dp)
- [ ] Verify accessibility
- [ ] Check responsive layout

---

## QUICK REFERENCE: COMPONENT IMPORTS

```kotlin
import com.gcuf.craftoria.ui.components.CraftoriaButton
import com.gcuf.craftoria.ui.components.CraftoriaTextField
import com.gcuf.craftoria.ui.components.UnifiedBadgeComponent
import com.gcuf.craftoria.ui.components.UnifiedDialogComponent
import com.gcuf.craftoria.ui.components.FilterTabComponent
import com.gcuf.craftoria.ui.components.EmptyStateComponent
import com.gcuf.craftoria.ui.theme.*
```

---

## QUICK REFERENCE: EMPTY STATE TYPES

```kotlin
EmptyStateComponent.NoProducts()
EmptyStateComponent.NoOrders()
EmptyStateComponent.NoPayments()
EmptyStateComponent.NoRefunds()
EmptyStateComponent.NoMessages()
EmptyStateComponent.NoNotifications()
EmptyStateComponent.NoWishlist()
EmptyStateComponent.NoSearchResults(query = "search term")
EmptyStateComponent.NoStores()
```

---

## QUICK REFERENCE: BADGE TYPES

```kotlin
UnifiedBadgeComponent.StatusBadge(status = "Pending")
UnifiedBadgeComponent.StateBadge(state = "Active")
UnifiedBadgeComponent.CountBadge(count = 5)
UnifiedBadgeComponent.VerificationBadge(isVerified = true)
UnifiedBadgeComponent.StockBadge(inStock = true)
UnifiedBadgeComponent.NegotiableBadge()
UnifiedBadgeComponent.RefundStatusBadge(status = "Approved")
```

---

## QUICK REFERENCE: DIALOG TYPES

```kotlin
UnifiedDialogComponent.ConfirmationDialog(
    title = "Confirm",
    message = "Are you sure?",
    onConfirm = { },
    onDismiss = { }
)

UnifiedDialogComponent.AlertDialog(
    title = "Alert",
    message = "Something happened",
    onDismiss = { }
)

UnifiedDialogComponent.LoadingDialog(
    message = "Loading..."
)

UnifiedDialogComponent.ErrorDialog(
    title = "Error",
    message = "Something went wrong",
    onDismiss = { }
)

UnifiedDialogComponent.SuccessDialog(
    title = "Success",
    message = "Operation completed",
    onDismiss = { }
)
```

---

## COMMON MISTAKES TO AVOID

❌ **WRONG**: Using different button heights
```kotlin
Button(modifier = Modifier.height(40.dp))  // Wrong
Button(modifier = Modifier.height(44.dp))  // Wrong
```

✅ **RIGHT**: Using standard button height
```kotlin
CraftoriaButton(modifier = Modifier.fillMaxWidth())  // 48dp height
```

---

❌ **WRONG**: Inconsistent spacing
```kotlin
Modifier.padding(10.dp)  // Wrong
Modifier.padding(15.dp)  // Wrong
Modifier.padding(18.dp)  // Wrong
```

✅ **RIGHT**: Using 8dp grid
```kotlin
Modifier.padding(8.dp)   // xs
Modifier.padding(12.dp)  // sm
Modifier.padding(16.dp)  // md
Modifier.padding(24.dp)  // lg
```

---

❌ **WRONG**: Inconsistent border radius
```kotlin
RoundedCornerShape(6.dp)   // Wrong
RoundedCornerShape(14.dp)  // Wrong
RoundedCornerShape(18.dp)  // Wrong
```

✅ **RIGHT**: Using standard radius
```kotlin
RoundedCornerShape(8.dp)   // Tabs
RoundedCornerShape(10.dp)  // Text fields
RoundedCornerShape(12.dp)  // Cards & Buttons
RoundedCornerShape(20.dp)  // Badges
```

---

❌ **WRONG**: Mixing custom colors
```kotlin
color = Color(0xFFE91E63)  // Wrong
color = Color.Red          // Wrong
color = Color(0xFF123456)  // Wrong
```

✅ **RIGHT**: Using theme colors
```kotlin
color = Primary            // #FFE91E63
color = TextPrimary        // #FF333333
color = Success            // #FF4CAF50
```

---

## PERFORMANCE TIPS

1. **Use LazyColumn/LazyRow** for long lists
2. **Avoid recomposition** with remember {}
3. **Use Modifier.fillMaxWidth()** instead of fixed widths
4. **Cache expensive computations** with remember
5. **Use proper image sizing** with Coil
6. **Minimize state updates** with collectAsState()

---

## ACCESSIBILITY TIPS

1. **Add contentDescription** to all icons
2. **Use proper text contrast** (minimum 4.5:1)
3. **Ensure touch targets** are 48dp × 48dp
4. **Use semantic modifiers** (clickable, toggleable)
5. **Add labels** to all input fields
6. **Test with screen reader** (TalkBack)

---

**Last Updated**: May 23, 2026
**Version**: 1.0

