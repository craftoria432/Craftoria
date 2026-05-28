# Order Highlight Color Reference

## Pink Highlight When Order Opens from Notification

### Color Specification
- **Hex Code**: `#FFF5F8`
- **RGB**: (255, 245, 248)
- **Name**: Light Pink / Blush Pink
- **Opacity**: 100% (fully opaque)

### Visual Styling

**When Order is Highlighted:**
```
┌─────────────────────────────────────┐
│ Order #ABC123                       │  ← 2dp Primary Pink Border
│ ─────────────────────────────────── │
│ Product: Handmade Ceramic Mug       │
│ Price: PKR 1,500                    │  ← Light Pink Background (#FFF5F8)
│ Status: Shipped                     │
│ ─────────────────────────────────── │
│ [Track Order]  [View Details]       │
└─────────────────────────────────────┘
     ↑ 4dp Shadow Elevation
```

### Implementation Details

**OrderCard Composable:**
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = if (isHighlighted) Color(0xFFFFF5F8) else Color.White
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = if (isHighlighted) 4.dp else 0.dp
    ),
    border = BorderStroke(
        width = if (isHighlighted) 2.dp else 0.5.dp,
        color = if (isHighlighted) Primary else BorderColor
    )
)
```

### User Experience Flow

1. **Buyer receives notification** → "Track Order" button visible
2. **Buyer clicks "Track Order"** → Navigates to MyOrdersScreen
3. **Order appears with pink highlight** → Immediately visible
4. **Pink highlight persists** → 10 seconds
5. **Auto-clear** → Highlight fades, order shows normally

### Color Contrast & Accessibility

- **Background**: Light Pink `#FFF5F8`
- **Text**: Dark Gray/Black (TextPrimary)
- **Contrast Ratio**: ✅ WCAG AA compliant
- **Visibility**: ✅ Clearly distinguishable from white background

### Comparison

| State | Background | Border | Elevation |
|-------|-----------|--------|-----------|
| Normal | White | 0.5dp Gray | 0dp |
| Highlighted | Light Pink `#FFF5F8` | 2dp Primary | 4dp |
| Selected | White | 1.5dp Primary | 0dp |

### Duration

- **Highlight Duration**: 10 seconds
- **Auto-clear**: Yes, after 10 seconds
- **Manual Clear**: User can scroll away or interact with other orders

### Testing Checklist

- [x] Pink highlight appears when order opens from notification
- [x] Highlight visible for exactly 10 seconds
- [x] Auto-clears after 10 seconds
- [x] Border shows Primary pink color
- [x] Shadow elevation applied
- [x] Text remains readable on pink background
- [x] Works on all screen sizes
- [x] Accessible color contrast maintained
