# SelectionButton Component - Quick Reference

## Overview
Unified selection button component for consistent styling across all selection contexts in the application.

**Location**: `app/src/main/java/com/gcuf/craftoria/ui/components/SelectionButton.kt`

---

## Specifications

| Property | Value | Notes |
|----------|-------|-------|
| Height | 48.dp (min) | Matches CraftoriaTextField |
| Border Radius | 10.dp | Matches CraftoriaTextField |
| Padding | 12.dp | Internal padding |
| Border (unselected) | 0.5.dp | BorderColor |
| Border (selected) | 1.5.dp | Primary color |
| Font Size | 14.sp | Label text |
| Font Weight | Medium (unselected) / SemiBold (selected) | Professional |

---

## Usage Examples

### 1. Payment Method Selection (with Icon)
```kotlin
SelectionButtonWithIcon(
    text = "Debit/Credit Card",
    icon = Icons.Default.CreditCard,
    isSelected = selectedPaymentMethod == "Debit/Credit Card",
    onClick = { updatePaymentMethod("Debit/Credit Card") }
)
```

### 2. Refund Reason Selection (Compact)
```kotlin
SelectionButtonCompact(
    text = reason.getDisplayName(),
    isSelected = selectedReason == reason,
    onClick = { onReasonSelected(reason) },
    minHeight = 48
)
```

### 3. Generic Selection (Full Featured)
```kotlin
SelectionButton(
    text = "Option Text",
    isSelected = isSelected,
    onClick = { onSelected() },
    icon = Icons.Default.SomeIcon,
    showSelectedBadge = true,
    minHeight = 48
)
```

---

## Component Variants

### SelectionButton (Full Featured)
- **Use for**: Generic selection with optional icon and badge
- **Parameters**:
  - `text`: String - Button label
  - `isSelected`: Boolean - Selection state
  - `onClick`: () -> Unit - Click handler
  - `modifier`: Modifier - Optional styling
  - `icon`: ImageVector? - Optional icon
  - `showSelectedBadge`: Boolean - Show "Selected" badge
  - `minHeight`: Int - Minimum height in dp (default: 48)

### SelectionButtonWithIcon
- **Use for**: Payment methods, options with icons
- **Parameters**:
  - `text`: String - Button label
  - `icon`: ImageVector - Icon to display
  - `isSelected`: Boolean - Selection state
  - `onClick`: () -> Unit - Click handler
  - `modifier`: Modifier - Optional styling
  - `showSelectedBadge`: Boolean - Show "Selected" badge
  - `minHeight`: Int - Minimum height in dp (default: 48)

### SelectionButtonCompact
- **Use for**: Refund reasons, simple selections
- **Parameters**:
  - `text`: String - Button label
  - `isSelected`: Boolean - Selection state
  - `onClick`: () -> Unit - Click handler
  - `modifier`: Modifier - Optional styling
  - `minHeight`: Int - Minimum height in dp (default: 48)

---

## Visual States

### Unselected State
- Background: White
- Border: 0.5.dp BorderColor
- Text Color: TextPrimary
- Font Weight: Medium
- Badge: Hidden

### Selected State
- Background: Color(0xFFFFF5F8) (light pink)
- Border: 1.5.dp Primary
- Text Color: Primary
- Font Weight: SemiBold
- Badge: "Selected" label with Primary background

---

## Screens Using SelectionButton

| Screen | Component | Type | Status |
|--------|-----------|------|--------|
| CheckoutScreen | Payment Methods | SelectionButtonWithIcon | ✅ Active |
| BuyerRefundRequestScreen | Refund Reasons | SelectionButtonCompact | ✅ Active |

---

## Integration Checklist

When adding SelectionButton to a new screen:

- [ ] Import SelectionButton variant
- [ ] Create state for selected item
- [ ] Create list of options
- [ ] Map options to SelectionButton components
- [ ] Handle onClick to update state
- [ ] Test selected/unselected states
- [ ] Verify height is 48.dp
- [ ] Verify border radius is 10.dp
- [ ] Test on different screen sizes

---

## Styling Customization

### Custom Height
```kotlin
SelectionButton(
    text = "Custom Height",
    isSelected = isSelected,
    onClick = { },
    minHeight = 56  // Custom height
)
```

### Custom Modifier
```kotlin
SelectionButton(
    text = "Custom Styling",
    isSelected = isSelected,
    onClick = { },
    modifier = Modifier.padding(horizontal = 8.dp)
)
```

### Without Badge
```kotlin
SelectionButton(
    text = "No Badge",
    isSelected = isSelected,
    onClick = { },
    showSelectedBadge = false
)
```

---

## Color Reference

| Element | Color | Usage |
|---------|-------|-------|
| Selected Border | Primary | Active selection |
| Unselected Border | BorderColor | Inactive state |
| Selected Background | Color(0xFFFFF5F8) | Light pink tint |
| Unselected Background | Color.White | Clean appearance |
| Text (Selected) | Primary | Emphasis |
| Text (Unselected) | TextPrimary | Standard |
| Badge Background | Primary | Highlight |
| Badge Text | Color.White | Contrast |

---

## Performance Notes

- Component uses `remember` for interaction source
- No heavy recompositions on state changes
- Efficient border and background rendering
- Suitable for lists with many items

---

## Accessibility

- ✅ RadioButton for selection indication
- ✅ Sufficient touch target size (48.dp minimum)
- ✅ Clear visual feedback for selected state
- ✅ Proper color contrast ratios
- ✅ Semantic meaning preserved

---

## Related Components

- **CraftoriaTextField**: Text input with same height/radius standards
- **FilterTabComponent**: Tab selection with similar styling
- **UnifiedBadgeComponent**: Badge display component

---

## Troubleshooting

### Button appears too small
- Check `minHeight` parameter (should be 48)
- Verify parent container has sufficient space

### Text is cut off
- Increase `minHeight` parameter
- Check text length and font size

### Border not visible
- Verify `isSelected` state is correct
- Check color theme is applied

### Badge not showing
- Verify `showSelectedBadge = true`
- Check `isSelected` state is true

---

## Future Enhancements

1. **SelectionButtonGroup**: Radio button group wrapper
2. **Animations**: Smooth transitions for state changes
3. **Variants**: Different sizes (small, medium, large)
4. **Themes**: Dark mode support
5. **Loading State**: Disabled state with loading indicator

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | May 27, 2026 | Initial release |

---

## Support

For issues or questions about SelectionButton:
1. Check this quick reference
2. Review component implementation in SelectionButton.kt
3. Check usage examples in CheckoutScreen or BuyerRefundRequestScreen
