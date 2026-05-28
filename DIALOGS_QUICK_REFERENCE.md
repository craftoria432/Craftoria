# Dialogs Quick Reference Guide

## ✅ Professional Dialog Standards

### Dialog Structure
```
┌─────────────────────────────────────┐
│  [Close] Header Title               │  48-56dp
├─────────────────────────────────────┤
│                                     │
│  Content Area (Scrollable)          │  Flexible
│                                     │
├─────────────────────────────────────┤
│  [Primary Button]  [Secondary]      │  40-46dp
└─────────────────────────────────────┘
```

---

## Button Heights

| Type | Height | Pattern | Example |
|------|--------|---------|---------|
| Dialog buttons | 40dp | `heightIn(min = 40.dp)` | Confirm/Cancel |
| Action buttons | 46dp | `heightIn(min = 46.dp)` | Primary actions |
| Compact buttons | 42dp | `heightIn(min = 42.dp)` | Secondary |

---

## Creating Professional Dialogs

### Option 1: Use CraftoriaDialog (Recommended)
```kotlin
CraftoriaDialog(
    title = "Confirm Action",
    content = {
        Text("Are you sure?", fontSize = 14.sp)
    },
    onDismiss = { /* close */ },
    primaryButton = DialogButton(
        text = "Confirm",
        onClick = { /* action */ }
    ),
    secondaryButton = DialogButton(
        text = "Cancel",
        onClick = { /* close */ },
        isPrimary = false
    )
)
```

### Option 2: Use ConfirmationDialog
```kotlin
ConfirmationDialog(
    title = "Delete Item?",
    message = "This action cannot be undone.",
    onConfirm = { /* delete */ },
    onCancel = { /* close */ },
    isDestructive = true
)
```

### Option 3: Use AlertDialog (Material3)
```kotlin
AlertDialog(
    onDismissRequest = { /* close */ },
    containerColor = Color.White,
    shape = RoundedCornerShape(20.dp),
    title = { Text("Title") },
    text = { Text("Message") },
    confirmButton = {
        Button(
            onClick = { /* action */ },
            modifier = Modifier.heightIn(min = 40.dp)
        ) {
            Text("OK")
        }
    }
)
```

---

## Button Styling Patterns

### ✅ Primary Button (Gradient)
```kotlin
Button(
    onClick = { /* action */ },
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 46.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    contentPadding = PaddingValues(0.dp),
    shape = RoundedCornerShape(10.dp)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("Action", fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
```

### ✅ Secondary Button (Outlined)
```kotlin
OutlinedButton(
    onClick = { /* action */ },
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 40.dp),
    border = BorderStroke(0.5.dp, BorderColor),
    shape = RoundedCornerShape(10.dp)
) {
    Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Medium)
}
```

### ✅ Destructive Button (Error)
```kotlin
OutlinedButton(
    onClick = { /* delete */ },
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 40.dp),
    border = BorderStroke(0.5.dp, Error),
    shape = RoundedCornerShape(10.dp),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
) {
    Text("Delete", fontWeight = FontWeight.SemiBold)
}
```

---

## Spacing Standards

| Element | Spacing |
|---------|---------|
| Header padding | 16dp horizontal |
| Content padding | 14-24dp |
| Between elements | 12-14dp |
| Between buttons | 8-12dp |
| Top/bottom margins | 12-16dp |

---

## Typography Standards

| Element | Size | Weight | Color |
|---------|------|--------|-------|
| Title | 16-18sp | SemiBold | TextPrimary |
| Body text | 13-14sp | Normal | TextSecondary |
| Button text | 13sp | SemiBold | Varies |
| Label | 12sp | Medium | TextSecondary |

---

## Color Standards

| Element | Color | Usage |
|---------|-------|-------|
| Primary button | Primary → PrimaryLight | Positive actions |
| Secondary button | BorderColor | Neutral actions |
| Destructive button | Error | Delete/Cancel |
| Text | TextPrimary/Secondary | Content |
| Border | BorderColor | Outlined buttons |

---

## Common Dialog Types

### Confirmation Dialog
```kotlin
ConfirmationDialog(
    title = "Confirm?",
    message = "Are you sure?",
    onConfirm = { /* yes */ },
    onCancel = { /* no */ }
)
```

### Alert Dialog
```kotlin
AlertDialog(
    title = "Alert",
    message = "Something happened",
    onDismiss = { /* close */ },
    buttonText = "OK"
)
```

### Error Dialog
```kotlin
ErrorDialog(
    title = "Error",
    message = "Operation failed",
    onDismiss = { /* close */ },
    onRetry = { /* retry */ }
)
```

### Success Dialog
```kotlin
SuccessDialog(
    title = "Success",
    message = "Operation completed",
    onDismiss = { /* close */ },
    buttonText = "Continue"
)
```

### Loading Dialog
```kotlin
LoadingDialog(
    message = "Processing...",
    onDismiss = null  // Non-dismissible
)
```

---

## ❌ Common Mistakes to Avoid

### ❌ WRONG: Fixed Heights
```kotlin
Button(
    modifier = Modifier.height(46.dp)  // ❌ Fixed
)
```

### ❌ WRONG: Inconsistent Spacing
```kotlin
Column {
    Button()
    Button()  // No spacing
}
```

### ❌ WRONG: Missing Border Radius
```kotlin
Button(
    shape = RectangleShape  // ❌ No rounding
)
```

### ❌ WRONG: Inconsistent Colors
```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Blue  // ❌ Not from theme
    )
)
```

### ❌ WRONG: Poor Typography
```kotlin
Text(
    text = "Button",
    fontSize = 10.sp,  // ❌ Too small
    fontWeight = FontWeight.Light  // ❌ Too light
)
```

---

## ✅ Best Practices

1. **Always use `heightIn(min = XXdp)`** instead of `.height()`
2. **Use CraftoriaDialog** for consistency
3. **Maintain 8-12dp spacing** between buttons
4. **Use theme colors** (Primary, Error, etc.)
5. **Keep button text short** (1-2 words)
6. **Use proper typography** (13sp SemiBold for buttons)
7. **Test on different screen sizes**
8. **Ensure touch targets** are at least 40dp

---

## Testing Checklist

- [ ] Buttons are uniform height
- [ ] Buttons expand with longer text
- [ ] Spacing is consistent
- [ ] Colors match theme
- [ ] Typography is readable
- [ ] Close button works
- [ ] All buttons are clickable
- [ ] Dialog dismisses properly
- [ ] Loading states work
- [ ] Error states display

---

## Files to Reference

- `UnifiedDialogComponent.kt` - Base dialog component
- `RateStoreDialog.kt` - Example: Rating dialog
- `OrderDialogs.kt` - Example: Complex dialogs
- `NegotiationDialog.kt` - Example: Custom dialog

---

## Status: ✅ COMPLETE

All dialogs are professional, consistent, and follow design standards.

**Last Updated:** May 27, 2026

