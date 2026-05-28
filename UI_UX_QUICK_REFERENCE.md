# Craftoria UI/UX Quick Reference Guide

## 🎨 Design System at a Glance

### Spacing (8dp Grid)
```
xs: 4dp  |  sm: 8dp  |  md: 12dp  |  lg: 16dp  |  xl: 20dp  |  xxl: 24dp
```

### Component Heights
```
Button:      48dp (standard), 36dp (small)
TextField:   48dp
TopBar:      64dp
BottomNav:   64dp
Badge:       24dp
FilterTab:   40dp
```

### Border Radius
```
Cards:       12dp
TextFields:  10dp
Buttons:     12dp
Tabs:        8dp
Badges:      20dp (pill)
Dialogs:     12dp
```

### Colors (Rose Theme)
```
Primary:     #FFE91E63 (Pink)
Success:     #FF4CAF50 (Green)
Warning:     #FFFFA726 (Orange)
Error:       #FFF44336 (Red)
Info:        #FF2196F3 (Blue)
```

---

## 🧩 Component Quick Start

### Button
```kotlin
// Primary
CraftoriaButton("Add to Cart", onClick = { })

// Secondary
CraftoriaButton("Cancel", onClick = { }, isPrimary = false)

// Small
CraftoriaButton("Save", onClick = { }, isSmall = true)

// With Icon
CraftoriaButton("Save", onClick = { }, icon = Icons.Default.Save)

// Loading
CraftoriaButton("Processing", onClick = { }, isLoading = true, enabled = false)
```

### Text Field
```kotlin
CraftoriaTextField(
    value = text,
    onValueChange = { text = it },
    label = "Email",
    placeholder = "Enter email",
    isPassword = false,
    keyboardType = KeyboardType.Email
)
```

### Empty State
```kotlin
// Predefined
EmptyStates.NoProducts()
EmptyStates.NoOrders()
EmptyStates.NoPayments()
EmptyStates.NoMessages()

// Custom
EmptyStateComponent(
    icon = Icons.Default.Search,
    title = "No Results",
    message = "Try different keywords"
)
```

### Badge
```kotlin
// Status
StatusBadge(status = OrderStatus.DELIVERED)

// State
StateBadge("In Stock", BadgeState.SUCCESS)

// Count
CountBadge(count = 5)

// Verification
VerificationBadge(isVerified = true)

// Stock
StockBadge(stock = 10)

// Refund
RefundStatusBadge("APPROVED")
```

### Dialog
```kotlin
// Confirmation
ConfirmationDialog(
    title = "Delete?",
    message = "Cannot be undone",
    onConfirm = { },
    onCancel = { }
)

// Alert
AlertDialog(
    title = "Error",
    message = "Something went wrong",
    onDismiss = { }
)

// Loading
LoadingDialog("Processing...")

// Error with Retry
ErrorDialog(
    title = "Failed",
    message = "Network error",
    onDismiss = { },
    onRetry = { }
)

// Success
SuccessDialog(
    title = "Success",
    message = "Order placed",
    onDismiss = { }
)
```

### Filter Tabs
```kotlin
var selectedIndex by remember { mutableStateOf(0) }

// Horizontal
FilterTabRow(
    tabs = FilterTabs.ORDER_STATUS,
    selectedIndex = selectedIndex,
    onTabSelected = { selectedIndex = it }
)

// Vertical
FilterTabColumn(
    tabs = FilterTabs.CATEGORY,
    selectedIndex = selectedIndex,
    onTabSelected = { selectedIndex = it }
)
```

---

## 📐 Spacing Patterns

### Standard Padding
```kotlin
// Cards
modifier = Modifier.padding(12.dp)

// Screens
modifier = Modifier.padding(16.dp)

// Sections
modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
```

### Standard Gaps
```kotlin
// Between items
verticalArrangement = Arrangement.spacedBy(8.dp)

// Between sections
Spacer(modifier = Modifier.height(16.dp))

// Between buttons
verticalArrangement = Arrangement.spacedBy(12.dp)
```

---

## 🎯 Common Layouts

### List Item Card
```kotlin
Card(
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(0.5.dp, BorderColor),
    modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
) {
    Column(modifier = Modifier.padding(12.dp)) {
        // Content
    }
}
```

### Form Layout
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    CraftoriaTextField(...)
    CraftoriaTextField(...)
    CraftoriaButton("Submit", onClick = { })
}
```

### List with Empty State
```kotlin
if (items.isEmpty()) {
    EmptyStates.NoProducts()
} else {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(items) { item ->
            // Item card
        }
    }
}
```

### Filter + List
```kotlin
Column(modifier = Modifier.fillMaxSize()) {
    FilterTabRow(
        tabs = FilterTabs.ORDER_STATUS,
        selectedIndex = selectedIndex,
        onTabSelected = { selectedIndex = it },
        modifier = Modifier.padding(12.dp)
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(filteredItems) { item ->
            // Item card
        }
    }
}
```

---

## 🔄 State Management Patterns

### Loading State
```kotlin
if (isLoading) {
    LoadingDialog("Loading...")
} else {
    // Content
}
```

### Error State
```kotlin
if (error != null) {
    ErrorDialog(
        message = error,
        onDismiss = { error = null },
        onRetry = { retry() }
    )
}
```

### Empty State
```kotlin
if (items.isEmpty()) {
    EmptyStates.NoProducts()
} else {
    // List content
}
```

### Success State
```kotlin
if (showSuccess) {
    SuccessDialog(
        message = "Operation successful",
        onDismiss = { showSuccess = false }
    )
}
```

---

## 📱 Responsive Patterns

### Adaptive Padding
```kotlin
val padding = when {
    screenWidth < 360.dp -> 12.dp
    screenWidth < 600.dp -> 16.dp
    else -> 20.dp
}

modifier = Modifier.padding(padding)
```

### Adaptive Columns
```kotlin
val columns = when {
    screenWidth < 360.dp -> 1
    screenWidth < 600.dp -> 2
    else -> 3
}

LazyVerticalGrid(
    columns = GridCells.Fixed(columns),
    modifier = Modifier.fillMaxSize()
) {
    // Items
}
```

---

## 🎨 Theme Usage

### Using Colors
```kotlin
// Primary color
Box(modifier = Modifier.background(Primary))

// Success color
Text(text = "Success", color = Success)

// Error color
Text(text = "Error", color = Error)
```

### Using Typography
```kotlin
// Title
Text(text = "Title", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

// Body
Text(text = "Body", fontSize = 14.sp, fontWeight = FontWeight.Normal)

// Caption
Text(text = "Caption", fontSize = 10.sp, fontWeight = FontWeight.Normal)
```

---

## ✅ Implementation Checklist

### For Each Screen
- [ ] Use CraftoriaButton for all buttons (48dp)
- [ ] Use CraftoriaTextField for all inputs (48dp)
- [ ] Use 12dp border radius for cards
- [ ] Use 8dp grid spacing
- [ ] Add empty state (EmptyStateComponent)
- [ ] Use unified badges (UnifiedBadgeComponent)
- [ ] Use unified dialogs (UnifiedDialogComponent)
- [ ] Use filter tabs (FilterTabComponent)
- [ ] Consistent typography
- [ ] Consistent colors from theme
- [ ] Proper error handling
- [ ] Loading states
- [ ] Responsive layout

### For Each Component
- [ ] Follows design system standards
- [ ] Consistent with other components
- [ ] Proper spacing (8dp grid)
- [ ] Proper sizing (48dp buttons, etc.)
- [ ] Proper colors (from theme)
- [ ] Proper typography
- [ ] Proper border radius
- [ ] Proper elevation/shadow
- [ ] Accessible (touch targets, contrast)
- [ ] Responsive

---

## 🚀 Common Mistakes to Avoid

❌ **Don't**: Use hardcoded colors
✅ **Do**: Use theme colors (Primary, Success, Error, etc.)

❌ **Don't**: Use inconsistent button heights
✅ **Do**: Use CraftoriaButton (48dp standard, 36dp small)

❌ **Don't**: Use mixed spacing values
✅ **Do**: Use 8dp grid (4, 8, 12, 16, 20, 24dp)

❌ **Don't**: Create custom dialogs
✅ **Do**: Use UnifiedDialogComponent variants

❌ **Don't**: Use inconsistent border radius
✅ **Do**: Use 12dp for cards, 10dp for fields, 8dp for tabs

❌ **Don't**: Skip empty states
✅ **Do**: Use EmptyStateComponent with predefined scenarios

❌ **Don't**: Use inconsistent badges
✅ **Do**: Use UnifiedBadgeComponent variants

❌ **Don't**: Use custom filter tabs
✅ **Do**: Use FilterTabComponent with predefined sets

---

## 📚 Documentation References

- **Design System**: `UI_UX_DESIGN_SYSTEM.md`
- **Implementation Guide**: `UI_UX_IMPLEMENTATION_GUIDE.md`
- **Complete Package**: `UI_UX_REFINEMENT_COMPLETE.md`

---

## 🔗 Component Files

- `CraftoriaButton.kt` - Button component
- `CraftoriaTextField.kt` - Text field component
- `CraftoriaTopBar.kt` - Top bar component
- `EmptyStateComponent.kt` - Empty state component
- `UnifiedBadgeComponent.kt` - Badge system
- `UnifiedDialogComponent.kt` - Dialog system
- `FilterTabComponent.kt` - Filter tab component

---

## 💡 Pro Tips

1. **Always use 8dp grid** - Makes layouts look professional
2. **Use predefined empty states** - Faster development
3. **Use unified components** - Ensures consistency
4. **Reference design system** - When in doubt
5. **Test on multiple screens** - Ensure responsiveness
6. **Check accessibility** - Color contrast, touch targets
7. **Use theme colors** - Easy theme switching
8. **Follow patterns** - Consistent user experience

---

## 🆘 Quick Help

**Q: What button height should I use?**
A: 48dp for standard, 36dp for small

**Q: What spacing should I use?**
A: 8dp grid (4, 8, 12, 16, 20, 24dp)

**Q: What border radius for cards?**
A: 12dp

**Q: What border radius for text fields?**
A: 10dp

**Q: What border radius for buttons?**
A: 12dp

**Q: What border radius for tabs?**
A: 8dp

**Q: What border radius for badges?**
A: 20dp (pill shape)

**Q: What color for primary action?**
A: Primary (#FFE91E63)

**Q: What color for success?**
A: Success (#FF4CAF50)

**Q: What color for error?**
A: Error (#FFF44336)

**Q: How to show empty state?**
A: Use EmptyStateComponent or predefined EmptyStates

**Q: How to show dialog?**
A: Use UnifiedDialogComponent or preset variants

**Q: How to show filter tabs?**
A: Use FilterTabComponent with predefined sets

---

**Last Updated**: May 23, 2026
**Version**: 1.0
**Status**: Ready to Use
