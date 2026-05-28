# Craftoria UI/UX Design System - Complete Reference

## 1. SPACING SCALE (8dp Grid System)
```
xs: 4dp    (minimal spacing)
sm: 8dp    (small spacing)
md: 12dp   (medium spacing)
lg: 16dp   (large spacing)
xl: 20dp   (extra large)
xxl: 24dp  (double extra large)
```

## 2. COMPONENT SIZING STANDARDS

### Buttons
- **Primary Button**: 48dp height, 16sp font, 12dp border radius
- **Secondary Button**: 48dp height, 16sp font, 12dp border radius
- **Small Button**: 36dp height, 14sp font, 8dp border radius
- **Icon Button**: 40dp size, 24dp icon
- **Padding**: Horizontal 16dp, Vertical 12dp

### Text Fields
- **Standard Height**: 48dp
- **Label Font**: 14sp, SemiBold
- **Input Font**: 14sp, Normal
- **Border Radius**: 10dp
- **Padding**: 12dp horizontal, 12dp vertical
- **Label Gap**: 4dp (below label)

### Cards
- **Border Radius**: 12dp
- **Elevation**: 0dp default, 4dp on press
- **Border**: 0.5dp, alpha 0.2
- **Padding**: 12dp
- **Gap Between Items**: 8dp

### Icons
- **Navigation Icons**: 24dp
- **Button Icons**: 18dp
- **Inline Icons**: 16dp
- **Large Icons (Empty States)**: 48dp-64dp

### Badges
- **Height**: 24dp
- **Padding**: 6dp horizontal, 4dp vertical
- **Font**: 10sp, SemiBold
- **Border Radius**: 20dp (pill shape)

### Top Bar
- **Height**: 64dp
- **Title Font**: 16sp, SemiBold
- **Subtitle Font**: 12sp, Normal
- **Padding**: 16dp horizontal, 12dp vertical

### Bottom Navigation
- **Height**: 64dp
- **Icon Size**: 24dp
- **Label Font**: 12sp, Medium
- **Badge Size**: 20dp

## 3. COLOR PALETTE

### Rose Theme (Default)
```
Primary:           #FFE91E63 (Pink)
PrimaryLight:      #FFF06292 (Light Pink)
PrimaryDark:       #FFC2185B (Dark Pink)
Secondary:         #FF625B71 (Gray-Purple)
SecondaryLight:    #FF8B7B8F (Light Gray-Purple)
Tertiary:          #FF7D5260 (Mauve)

Background:        #FFFFFFFF (White)
BackgroundSecondary: #FFF8F9FA (Light Gray)
BackgroundLight:   #FFFAFAFA (Very Light Gray)
SurfaceColor:      #FFFAFAFA (Surface)

TextPrimary:       #FF333333 (Dark Gray)
TextSecondary:     #FF666666 (Medium Gray)
TextLight:         #FFAAAAAA (Light Gray)

Success:           #FF4CAF50 (Green)
Warning:           #FFFFA726 (Orange)
Error:             #FFF44336 (Red)
Info:              #FF2196F3 (Blue)

BorderColor:       #FFFFFFFF (Light Border)
DividerColor:      #FFEEEEEE (Light Divider)
DisabledColor:     #FFBDBDBD (Disabled Gray)
```

### Ocean Theme
```
Primary:           #FF0288D1 (Light Blue)
PrimaryLight:      #FF03A9F4 (Lighter Blue)
PrimaryDark:       #FF0277BD (Dark Blue)
Secondary:         #FF0097A7 (Cyan)
SecondaryLight:    #FF00BCD4 (Light Cyan)
Tertiary:          #FF00897B (Teal)

Background:        #FFFFFFFF (White)
BackgroundSecondary: #FFF0F7FA (Light Blue-Gray)
BackgroundLight:   #FFF5F9FC (Very Light Blue)
SurfaceColor:      #FFF5F9FC (Surface)

TextPrimary:       #FF1A237E (Dark Blue)
TextSecondary:     #FF37474F (Blue-Gray)
TextLight:         #FF78909C (Light Blue-Gray)

Success:           #FF00897B (Teal)
Warning:           #FFFFA726 (Orange)
Error:             #FFFF5252 (Red)
Info:              #FF0288D1 (Light Blue)

BorderColor:       #FFBDE5FC (Light Blue Border)
DividerColor:      #FFFFFFFF (Very Light Blue Divider)
DisabledColor:     #FF90CAF9 (Disabled Light Blue)
```

## 4. TYPOGRAPHY SCALE

```
Display Large:     32sp, Bold, 40sp line height
Display Medium:    28sp, Bold, 36sp line height
Display Small:     24sp, Bold, 32sp line height

Headline Large:    22sp, Bold, 28sp line height
Headline Medium:   20sp, SemiBold, 26sp line height
Headline Small:    18sp, SemiBold, 24sp line height

Title Large:       16sp, SemiBold, 24sp line height
Title Medium:      14sp, SemiBold, 20sp line height
Title Small:       12sp, SemiBold, 18sp line height

Body Large:        16sp, Normal, 24sp line height
Body Medium:       14sp, Normal, 20sp line height
Body Small:        12sp, Normal, 18sp line height

Label Large:       14sp, Medium, 20sp line height
Label Medium:      12sp, Medium, 18sp line height
Label Small:       10sp, Medium, 16sp line height

Caption:           10sp, Normal, 14sp line height
```

## 5. BORDER RADIUS STANDARDS

```
xs: 4dp    (minimal rounding)
sm: 8dp    (small rounding)
md: 10dp   (medium rounding - text fields)
lg: 12dp   (large rounding - cards)
xl: 16dp   (extra large rounding)
full: 50dp (pill shape - badges)
```

## 6. ELEVATION/SHADOW STANDARDS

```
Level 0:   No shadow (default)
Level 1:   0dp elevation (flat)
Level 2:   2dp elevation (subtle)
Level 3:   4dp elevation (pressed/hover)
Level 4:   8dp elevation (modal)
Level 5:   12dp elevation (dialog)
```

## 7. COMPONENT SPECIFICATIONS

### Button Component
- **Primary**: Pink background, white text, 48dp height
- **Secondary**: Transparent background, pink border, pink text, 48dp height
- **Disabled**: Gray background, gray text, 48dp height
- **Loading**: Show spinner, disable interaction
- **Icon Support**: 18dp icon + text with 8dp gap
- **Border Radius**: 12dp
- **Font**: 15sp, SemiBold

### Text Field Component
- **Height**: 48dp
- **Border Radius**: 10dp
- **Border**: 1dp, alpha 0.5 (unfocused), 1.5dp primary (focused)
- **Padding**: 12dp horizontal, 12dp vertical
- **Label**: 14sp, SemiBold, 4dp gap below
- **Input**: 14sp, Normal
- **Placeholder**: 13sp, alpha 0.6
- **Icon Support**: 20dp leading icon, password toggle trailing icon
- **Error State**: 1.5dp red border

### Card Component
- **Border Radius**: 12dp
- **Border**: 0.5dp, alpha 0.2
- **Elevation**: 0dp default, 4dp on press
- **Padding**: 12dp
- **Background**: White
- **Clickable**: Elevation increases on press

### Badge Component
- **Height**: 24dp
- **Padding**: 6dp horizontal, 4dp vertical
- **Border Radius**: 20dp (pill)
- **Font**: 10sp, SemiBold
- **Status Colors**:
  - Pending: Yellow background, dark text
  - Processing: Blue background, white text
  - Shipped: Purple background, white text
  - Delivered: Green background, white text
  - Cancelled: Red background, white text
  - Completed: Green background, white text

### Top Bar Component
- **Height**: 64dp
- **Background**: Gradient (Primary → PrimaryLight)
- **Title**: 16sp, SemiBold, white
- **Subtitle**: 12sp, Normal, white (alpha 0.85)
- **Icons**: 24dp, white
- **Padding**: 16dp horizontal, 12dp vertical
- **Back Button**: Left side
- **Actions**: Right side (Cart, Notifications, Edit)

### Bottom Navigation Component
- **Height**: 64dp
- **Icon Size**: 24dp
- **Label Font**: 12sp, Medium
- **Badge**: 20dp, positioned top-right
- **Active Color**: Primary
- **Inactive Color**: TextSecondary
- **Background**: White with subtle border

### Empty State Component
- **Icon Size**: 64dp
- **Icon Color**: TextSecondary
- **Title Font**: 16sp, SemiBold, TextPrimary
- **Message Font**: 14sp, Normal, TextSecondary
- **Padding**: 24dp
- **Vertical Gap**: 16dp between elements

### Dialog Component
- **Border Radius**: 12dp
- **Elevation**: 12dp
- **Padding**: 24dp
- **Title Font**: 18sp, SemiBold
- **Content Font**: 14sp, Normal
- **Button Height**: 48dp
- **Button Gap**: 12dp

### Filter Tab Component
- **Height**: 40dp
- **Padding**: 12dp horizontal, 8dp vertical
- **Font**: 12sp, Medium
- **Border Radius**: 8dp
- **Active**: Primary background, white text
- **Inactive**: BackgroundSecondary, TextSecondary
- **Gap Between Tabs**: 8dp

## 8. RESPONSIVE LAYOUT GUIDELINES

### Screen Sizes
- **Small**: < 360dp (phones)
- **Medium**: 360-600dp (tablets)
- **Large**: > 600dp (large tablets)

### Padding by Screen Size
- **Small**: 12dp horizontal
- **Medium**: 16dp horizontal
- **Large**: 20dp horizontal

### Column Layout
- **Small**: Single column
- **Medium**: 2 columns
- **Large**: 3+ columns

## 9. ANIMATION STANDARDS

### Transitions
- **Standard**: 300ms ease-in-out
- **Quick**: 150ms ease-in-out
- **Slow**: 500ms ease-in-out

### Common Animations
- **Button Press**: Scale 0.95, 150ms
- **Fade In**: Alpha 0 → 1, 300ms
- **Slide In**: Translate + Fade, 300ms
- **Theme Change**: Cross-fade, 300ms
- **Badge Pulse**: Scale 1 → 1.2 → 1, 600ms loop

## 10. ACCESSIBILITY STANDARDS

### Color Contrast
- **Text on Background**: Minimum 4.5:1 ratio
- **Interactive Elements**: Minimum 3:1 ratio
- **Large Text**: Minimum 3:1 ratio

### Touch Targets
- **Minimum Size**: 48dp × 48dp
- **Minimum Spacing**: 8dp between targets

### Text
- **Minimum Font Size**: 12sp
- **Maximum Line Length**: 80 characters
- **Line Height**: 1.5x font size minimum

### Icons
- **Meaningful Icons**: Always have text labels
- **Icon Size**: Minimum 24dp for touch targets
- **Color**: Not the only differentiator

## 11. DARK MODE CONSIDERATIONS

### Text Colors (Dark Mode)
- **Primary Text**: #FFFFFFFF (White)
- **Secondary Text**: #FFCCCCCC (Light Gray)
- **Tertiary Text**: #FF999999 (Medium Gray)

### Background Colors (Dark Mode)
- **Primary Background**: #FF121212 (Very Dark Gray)
- **Secondary Background**: #FF1E1E1E (Dark Gray)
- **Tertiary Background**: #FF2A2A2A (Lighter Dark Gray)

### Elevation (Dark Mode)
- **Surfaces**: Lighter backgrounds for elevation
- **Cards**: #FF1E1E1E with 1dp border

## 12. LOADING STATES

### Skeleton Loading
- **Height**: Match component height
- **Background**: BackgroundSecondary
- **Animation**: Shimmer effect, 1000ms loop
- **Border Radius**: Match component

### Progress Indicator
- **Size**: 24dp (small), 48dp (large)
- **Color**: Primary
- **Stroke Width**: 4dp

### Loading Button
- **Show Spinner**: 20dp, white color
- **Disable Interaction**: true
- **Text**: Hidden or "Loading..."

## 13. ERROR STATES

### Error Text
- **Font**: 12sp, Normal
- **Color**: Error
- **Margin Top**: 4dp

### Error Border
- **Width**: 1.5dp
- **Color**: Error
- **Border Radius**: Match component

### Error Icon
- **Size**: 16dp
- **Color**: Error
- **Position**: Trailing icon

## 14. FORM VALIDATION

### Valid State
- **Border**: 1dp, Success color
- **Icon**: Checkmark, Success color

### Invalid State
- **Border**: 1.5dp, Error color
- **Icon**: Error icon, Error color
- **Error Message**: 12sp, Error color, 4dp below field

### Disabled State
- **Background**: BackgroundSecondary
- **Text**: TextLight
- **Border**: 1dp, BorderColor
- **Cursor**: Not allowed

## 15. NOTIFICATION/SNACKBAR STANDARDS

### Snackbar
- **Height**: 48dp
- **Padding**: 16dp
- **Border Radius**: 4dp
- **Font**: 14sp, Normal
- **Duration**: 3000ms (short), 5000ms (long)
- **Position**: Bottom, 16dp from bottom

### Toast
- **Font**: 14sp, Normal
- **Duration**: 2000ms
- **Position**: Center or Bottom

### Alert Dialog
- **Title**: 18sp, SemiBold
- **Message**: 14sp, Normal
- **Buttons**: 48dp height
- **Padding**: 24dp

## 16. CONSISTENCY CHECKLIST

- [ ] All buttons are 48dp height (except small variants)
- [ ] All text fields are 48dp height
- [ ] All cards use 12dp border radius
- [ ] All icons follow size standards (24dp nav, 18dp button, 16dp inline)
- [ ] All spacing uses 8dp grid (4, 8, 12, 16, 20, 24)
- [ ] All text uses defined typography scale
- [ ] All colors from theme palette
- [ ] All borders use BorderStyles object
- [ ] All empty states use EmptyStateComponent
- [ ] All loading states use consistent pattern
- [ ] All dialogs use standard dimensions
- [ ] All badges use standard styling
- [ ] All animations use standard durations
- [ ] All touch targets minimum 48dp
- [ ] All text contrast minimum 4.5:1

---

**Last Updated**: May 23, 2026
**Version**: 1.0
**Status**: Production Ready
