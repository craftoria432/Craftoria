# Text Fields Standardization - Quick Reference

## Component Usage

### For Single-Line Text Fields
```kotlin
CraftoriaTextField(
    value = email,
    onValueChange = { email = it },
    label = "Email Address",
    placeholder = "Enter your email",
    keyboardType = KeyboardType.Email,
    leadingIconVector = Icons.Default.Email
)
```

### For Multi-Line Text Fields (with label)
```kotlin
StandardizedOutlinedTextField(
    value = description,
    onValueChange = { description = it },
    label = "Description *",
    placeholder = "Describe your product...",
    minLines = 3,
    maxLines = 6,
    minHeight = 100
)
```

### For Multi-Line Text Fields (without label)
```kotlin
StandardizedOutlinedTextField(
    value = notes,
    onValueChange = { notes = it },
    label = "",
    placeholder = "Add notes...",
    minLines = 2,
    maxLines = 4,
    minHeight = 80,
    showLabel = false
)
```

### For Inline/Compact Text Fields
```kotlin
StandardizedOutlinedTextFieldCompact(
    value = searchQuery,
    onValueChange = { searchQuery = it },
    placeholder = "Search...",
    singleLine = true,
    minHeight = 48
)
```

## Standardized Specifications

| Property | Value |
|----------|-------|
| **Height** | 48.dp minimum (or custom) |
| **Border Radius** | 10.dp |
| **Internal Padding** | 12.dp |
| **Border Width (Unselected)** | 0.5.dp |
| **Border Width (Selected)** | 1.5.dp |
| **Font Size** | 14.sp |
| **Font Weight (Unselected)** | Medium |
| **Font Weight (Selected)** | SemiBold |
| **Line Height** | 18.sp |

## Selection Components

### Full Selection Button
```kotlin
SelectionButton(
    text = "Credit Card",
    isSelected = isSelected,
    onClick = { /* handle click */ },
    icon = Icons.Default.CreditCard,
    showSelectedBadge = true,
    minHeight = 48
)
```

### Selection Button with Icon
```kotlin
SelectionButtonWithIcon(
    text = "Debit Card",
    icon = Icons.Default.CreditCard,
    isSelected = isSelected,
    onClick = { /* handle click */ },
    minHeight = 48
)
```

### Compact Selection Button
```kotlin
SelectionButtonCompact(
    text = "Damaged Item",
    isSelected = isSelected,
    onClick = { /* handle click */ },
    minHeight = 48
)
```

## Screens Standardized

| Screen | Fields Updated | Status |
|--------|----------------|--------|
| AddProductScreen | Description, Spec Name, Spec Value | ✅ Complete |
| BuyerRefundRequestScreen | Other Reason Details | ✅ Complete |
| SellerRefundDetailScreen | Approval Notes, Rejection Reason | ✅ Complete |
| PaymentDetailScreen | Reason Details | ✅ Complete |
| LearningResourcesScreen | Search Bar | ✅ Complete |
| SellerDirectoryScreen | Search Field | ✅ Complete |
| ManageCoSellerStoreScreen | Invite Email, Store Name, Store Description | ✅ Complete |
| CoSellerOrderDetailScreen | Reason Details | ✅ Complete |
| LoginScreen | All Fields | ✅ Already Standardized |

## Common Heights

| Use Case | Height |
|----------|--------|
| Single-line text field | 48.dp |
| Search bar | 48.dp |
| Email/Name field | 48.dp |
| 2-line text area | 80.dp |
| 3-line text area | 100.dp |
| 4-6 line text area | 120.dp |

## Color Scheme

| State | Border Color | Container Color |
|-------|--------------|-----------------|
| Focused | Primary | White |
| Unfocused | BorderColor | White |
| Disabled | BorderColor | Light Gray (#F8F9FA) |

## Migration Checklist

When updating existing OutlinedTextField instances:

- [ ] Replace with StandardizedOutlinedTextField or StandardizedOutlinedTextFieldCompact
- [ ] Set appropriate minHeight (48.dp minimum)
- [ ] Set minLines/maxLines for multi-line fields
- [ ] Remove custom colors (handled by component)
- [ ] Remove custom shape (10.dp handled by component)
- [ ] Remove custom padding (12.dp handled by component)
- [ ] Test on different screen sizes
- [ ] Verify visual consistency with other fields
- [ ] Run getDiagnostics to check for errors

## Import Statements

```kotlin
import com.gcuf.craftoria.ui.components.CraftoriaTextField
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextField
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextFieldCompact
import com.gcuf.craftoria.ui.components.SelectionButton
import com.gcuf.craftoria.ui.components.SelectionButtonWithIcon
import com.gcuf.craftoria.ui.components.SelectionButtonCompact
```

## Best Practices

1. **Use CraftoriaTextField** for single-line fields with labels (auth screens, forms)
2. **Use StandardizedOutlinedTextField** for multi-line fields with labels (descriptions, notes)
3. **Use StandardizedOutlinedTextFieldCompact** for inline/search fields without labels
4. **Always set minHeight** to ensure consistent sizing
5. **Use appropriate minLines/maxLines** for multi-line fields
6. **Test on multiple screen sizes** to ensure responsive behavior
7. **Maintain consistent spacing** between fields (12-14.dp)

## Troubleshooting

### Text field appears too small
- Check minHeight parameter (should be at least 48.dp)
- Verify minLines is set correctly for multi-line fields

### Text field appears too large
- Reduce minHeight parameter
- Reduce minLines for multi-line fields

### Border color not changing on focus
- Ensure component is using StandardizedOutlinedTextField
- Check that Primary color is defined in theme

### Text not visible
- Check placeholder text color (should be TextSecondary)
- Verify font size is 14.sp
- Check container color (should be White)

## Performance Notes

- StandardizedOutlinedTextField is a lightweight wrapper
- No performance impact compared to OutlinedTextField
- Recomposition only occurs on value changes
- Safe to use in LazyColumn/LazyRow

## Accessibility

- All text fields include proper labels
- Font sizes are readable (14.sp minimum)
- Color contrast meets WCAG standards
- Keyboard navigation supported
- Screen reader compatible

## Version History

- **Phase 1**: Created SelectionButton component, standardized CheckoutScreen, BuyerRefundRequestScreen, SellerRefundDetailScreen
- **Phase 2**: Created StandardizedOutlinedTextField component, standardized 8 additional screens
- **Future**: Consider additional screens and enhancements
