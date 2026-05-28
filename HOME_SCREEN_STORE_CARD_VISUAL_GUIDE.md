# Home Screen Store Card - Visual Design Guide

## Professional Store Card Layout

### Card Dimensions
```
Width:  160dp
Height: 240dp
Radius: 16dp
Shadow: 6dp elevation
Border: 1.5dp, BorderColor
```

---

## Component Breakdown

### 1. Top Section (90dp) - Logo & Badge

```
┌─────────────────────────────┐
│  ┌──────────────┐  [NEW]    │
│  │              │           │
│  │   [Logo]     │           │
│  │  80×80dp     │           │
│  │  Radius 12dp │           │
│  └──────────────┘           │
└─────────────────────────────┘
```

**Logo Box:**
- Size: 80×80dp
- Shape: RoundedCornerShape(12dp)
- Background: Gradient (Primary → PrimaryLight)
- Content: Image or Store Initial

**NEW Badge:**
- Position: Top-right corner
- Shape: RoundedCornerShape(6dp)
- Background: Primary color
- Text: "NEW", 9sp, ExtraBold, White
- Padding: 6dp horizontal, 2dp vertical
- Visibility: Only if store created < 7 days ago

---

### 2. Middle Section - Store Information

```
┌─────────────────────────────┐
│                             │
│    Store Name               │
│    (13sp, SemiBold)         │
│                             │
│    X products               │
│    (11sp, Medium)           │
│                             │
└─────────────────────────────┘
```

**Store Name:**
- Font Size: 13sp
- Font Weight: SemiBold
- Color: TextPrimary (#333333)
- Max Lines: 1
- Overflow: Ellipsis
- Text Align: Center

**Product Count:**
- Font Size: 11sp
- Font Weight: Medium
- Color: TextSecondary (#666666)
- Text Align: Center

**Spacing:** 6dp between elements

---

### 3. Bottom Section - Rating Display

```
┌─────────────────────────────┐
│ ┌───────────────────────┐   │
│ │  ⭐ 4.5 (23)          │   │
│ │  Light Orange BG      │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

**Rating Container:**
- Shape: RoundedCornerShape(8dp)
- Background: Color(0xFFFFF3E0) - Light Orange
- Padding: 8dp all sides
- Width: Fill max width

**Rating Content:**
- Star Icon: 14sp
- Rating Value: 12sp, Bold, TextPrimary
- Rating Count: 10sp, Medium, TextSecondary
- Format: "⭐ 4.5 (23)"
- Alignment: Center

**For Unrated Stores:**
- Shows: "⭐ New"
- Same styling
- Indicates no ratings yet

---

## Color Palette

| Element | Color | Hex Code |
|---------|-------|----------|
| Card Background | White | #FFFFFF |
| Card Border | BorderColor | #E0E0E0 |
| Logo Gradient Start | Primary | #E91E63 |
| Logo Gradient End | PrimaryLight | #F06292 |
| Store Name | TextPrimary | #333333 |
| Product Count | TextSecondary | #666666 |
| Rating Background | Light Orange | #FFF3E0 |
| NEW Badge | Primary | #E91E63 |
| NEW Badge Text | White | #FFFFFF |

---

## Typography

| Element | Size | Weight | Color |
|---------|------|--------|-------|
| Store Name | 13sp | SemiBold | TextPrimary |
| Product Count | 11sp | Medium | TextSecondary |
| Rating Value | 12sp | Bold | TextPrimary |
| Rating Count | 10sp | Medium | TextSecondary |
| NEW Badge | 9sp | ExtraBold | White |

---

## Spacing & Padding

```
Card Padding:           12dp all sides
Logo Size:              80×80dp
Logo Radius:            12dp
Top Section Height:     90dp
Middle Section Gap:     6dp between items
Bottom Section Padding: 8dp all sides
Card Radius:            16dp
Card Elevation:         6dp
```

---

## State Variations

### 1. Store with High Rating
```
┌─────────────────────────────┐
│  [Logo]                     │
│                             │
│  Premium Store              │
│  15 products                │
│                             │
│ ┌───────────────────────┐   │
│ │ ⭐ 4.8 (156)          │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

### 2. Store with Medium Rating
```
┌─────────────────────────────┐
│  [Logo]                     │
│                             │
│  Test Store                 │
│  2 products                 │
│                             │
│ ┌───────────────────────┐   │
│ │ ⭐ 4.5 (23)           │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

### 3. New Store (No Rating)
```
┌─────────────────────────────┐
│  [Logo]  [NEW]              │
│                             │
│  Wedding Collection         │
│  1 product                  │
│                             │
│ ┌───────────────────────┐   │
│ │ ⭐ New                │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

### 4. Store with Low Rating
```
┌─────────────────────────────┐
│  [Logo]                     │
│                             │
│  New Store                  │
│  5 products                 │
│                             │
│ ┌───────────────────────┐   │
│ │ ⭐ 2.5 (8)            │   │
│ └───────────────────────┘   │
└─────────────────────────────┘
```

---

## Responsive Behavior

### Mobile (320dp - 480dp)
```
Screen Width: 320dp
Card Width: 160dp
Cards per Row: 2
Horizontal Spacing: 14dp
Horizontal Padding: 16dp

Layout:
[Card] [Card]
[Card] [Card]
```

### Tablet (600dp+)
```
Screen Width: 600dp+
Card Width: 160dp
Cards per Row: 3-4
Horizontal Spacing: 14dp
Horizontal Padding: 16dp

Layout:
[Card] [Card] [Card] [Card]
[Card] [Card] [Card] [Card]
```

---

## Interaction States

### Default State
- Card visible with all content
- Proper spacing and alignment
- Rating displayed clearly

### Hover State (if applicable)
- Slight elevation increase
- Subtle shadow enhancement
- Cursor changes to pointer

### Pressed State
- Card click navigates to store details
- Smooth transition
- No visual feedback needed (handled by Card component)

---

## Accessibility Features

✅ **Color Contrast**
- Text on white background: WCAG AA compliant
- Rating background provides visual separation
- Clear visual hierarchy

✅ **Text Sizing**
- Store name: 13sp (readable)
- Product count: 11sp (secondary info)
- Rating: 12sp (prominent)

✅ **Touch Targets**
- Card clickable area: 160×240dp (sufficient)
- Easy to tap on mobile devices

✅ **Content Clarity**
- Clear store information
- Obvious rating display
- NEW badge clearly visible

---

## Animation & Transitions

### Card Click
- Smooth navigation to store details
- No loading delay
- Instant response

### Badge Appearance
- NEW badge visible immediately
- No animation needed
- Static display

### Rating Display
- Displays immediately on load
- No animation needed
- Static display

---

## Best Practices

### ✅ DO
- Keep card dimensions consistent (160×240dp)
- Show rating with count ("4.5 (23)")
- Display NEW badge for new stores
- Use light orange background for rating
- Maintain proper spacing (12dp padding)
- Use rounded corners (16dp card, 12dp logo)

### ❌ DON'T
- Change card dimensions
- Hide rating information
- Remove NEW badge
- Use different background colors
- Reduce spacing
- Use sharp corners

---

## Implementation Checklist

- [x] Card dimensions: 160×240dp
- [x] Logo: 80×80dp with 12dp radius
- [x] NEW badge: Top-right corner
- [x] Store name: 13sp, SemiBold
- [x] Product count: 11sp, Medium
- [x] Rating display: "4.5 (23)" format
- [x] Light orange background: #FFF3E0
- [x] Card radius: 16dp
- [x] Card elevation: 6dp
- [x] Proper spacing throughout

---

## Visual Hierarchy

```
1. Logo (Most Important)
   ↓
2. Store Name (Important)
   ↓
3. Product Count (Secondary)
   ↓
4. Rating Display (Important - Highlighted)
   ↓
5. NEW Badge (Accent - If applicable)
```

---

## Summary

Professional store card design with:
- ✅ Fixed, consistent dimensions
- ✅ Integrated rating display with count
- ✅ NEW badge for new stores
- ✅ Professional styling and spacing
- ✅ Proper visual hierarchy
- ✅ Accessible and responsive
- ✅ Production ready

---

**Status:** ✅ COMPLETE
**Quality:** Enterprise Grade
**Ready for Implementation:** YES 🚀
