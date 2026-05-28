# Chat Profile Pictures - Visual Reference Guide

## 🎨 UI Components Overview

### 1. Chat Screen Header (ChatScreen.kt)

```
╔═══════════════════════════════════════════════════════════╗
║  ←    [●]  Ahmed                                    ⋮     ║
║           Active now                                      ║
╚═══════════════════════════════════════════════════════════╝
```

**Components:**
- **Back Button**: Circular, semi-transparent white background
- **Profile Picture**: 38dp circular avatar
  - Shows real uploaded profile picture
  - Fallback: First letter of name on colored background
- **Online Indicator**: 11dp green circle
  - Positioned at bottom-right of avatar
  - 2dp white border
  - Only shows when user is online and not blocked
- **Name**: Bold, 14sp, white text
- **Status**: "Active now" / "Offline" / "Blocked", 11sp, semi-transparent white
- **Menu Button**: Three dots, same style as back button

**Color Scheme:**
- Background: Pink gradient (Primary → PrimaryLight)
- Text: White
- Online indicator: #4CAF50 (Green)
- Avatar fallback: White text on semi-transparent white background

---

### 2. Seller Messages List (SellerMessagesScreen.kt)

```
╔═══════════════════════════════════════════════════════════╗
║  Messages                                                 ║
║  Buyer conversations                                      ║
╠═══════════════════════════════════════════════════════════╣
║                                                           ║
║  [●]  Ahmed                                    2:30 PM    ║
║       Hey, is this product still available?        [3]   ║
║  ─────────────────────────────────────────────────────    ║
║  [●]  Sarah                                    1:15 PM    ║
║       Thank you for the quick delivery!                  ║
║  ─────────────────────────────────────────────────────    ║
║  [A]  Ali Khan                              Yesterday    ║
║       Can you ship to Lahore?                            ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
```

**Components:**
- **Profile Picture**: 48dp circular avatar
  - Real profile picture from user document
  - Fallback: Initials on colored background
- **Unread Badge**: Red circle with count
  - Positioned at top-right of avatar
  - Only shows when unread messages exist
- **Name**: Bold, 14sp, dark text
- **Last Message**: 13sp, gray text, truncated
- **Timestamp**: 12sp, gray text, right-aligned
- **Divider**: Thin gray line between items

**States:**
- **With Profile Picture**: Shows uploaded image
- **Without Profile Picture**: Shows first letter of name
- **Unread Messages**: Badge overlay on avatar
- **Blocked User**: Grayed out appearance

---

### 3. Avatar Fallback Design

When no profile picture exists:

```
┌─────────┐
│         │
│    A    │  ← First letter of user's name
│         │     (Uppercase, bold, white)
└─────────┘
```

**Specifications:**
- Size: Same as profile picture (38dp or 48dp)
- Background: Semi-transparent white (alpha 0.25)
- Text: First letter of name, uppercase
- Font: Bold, 15sp
- Color: White

---

## 🔄 State Transitions

### Profile Picture Loading States

```
1. LOADING
   ┌─────────┐
   │         │
   │    ⟳    │  ← Loading spinner
   │         │
   └─────────┘

2. LOADED (With Picture)
   ┌─────────┐
   │  [IMG]  │  ← User's profile picture
   │  [IMG]  │
   │  [IMG]  │
   └─────────┘

3. FALLBACK (No Picture)
   ┌─────────┐
   │         │
   │    A    │  ← First letter
   │         │
   └─────────┘

4. ERROR
   ┌─────────┐
   │         │
   │    A    │  ← Falls back to initials
   │         │
   └─────────┘
```

---

## 📐 Dimensions & Spacing

### Chat Header
```
┌─────────────────────────────────────────┐
│ ← 10dp [●] 10dp Name                    │
│   34dp  38dp      14sp                  │
│                                         │
│         Status (11sp)                   │
└─────────────────────────────────────────┘
```

### Chat List Item
```
┌─────────────────────────────────────────┐
│ 14dp [●] 12dp Name        Time          │
│      48dp     14sp        12sp          │
│                                         │
│      Message preview (13sp)             │
│      14dp padding                       │
└─────────────────────────────────────────┘
```

### Online Indicator
```
┌─────────┐
│    [●]  │ ← 11dp circle
│    ●●   │   2dp white border
│   ●●●   │   Positioned at bottom-right
└─────────┘
```

---

## 🎨 Color Palette

### Chat Header
- **Background Gradient**: `Primary (#E91E63)` → `PrimaryLight (#F48FB1)`
- **Text**: `White (#FFFFFF)`
- **Status Text**: `White 80% opacity`
- **Button Background**: `White 18% opacity`
- **Online Indicator**: `#4CAF50` (Green)

### Chat List
- **Background**: `BackgroundSecondary (#F5F5F5)`
- **Card Background**: `White (#FFFFFF)`
- **Name Text**: `TextPrimary (#212121)`
- **Message Text**: `TextSecondary (#757575)`
- **Timestamp**: `TextSecondary (#757575)`
- **Divider**: `BorderColor (#E0E0E0)`
- **Unread Badge**: `Error (#F44336)`

### Avatar Fallback
- **Background**: `White 25% opacity` (in header)
- **Background**: `Primary 8% opacity` (in list)
- **Text**: `White` (in header) / `Primary` (in list)

---

## 📱 Responsive Behavior

### Different Screen Sizes

**Small Screens (< 360dp width)**
- Avatar: 36dp (slightly smaller)
- Font sizes: -1sp
- Padding: -2dp

**Medium Screens (360-480dp width)**
- Avatar: 38dp (header) / 48dp (list)
- Standard font sizes
- Standard padding

**Large Screens (> 480dp width)**
- Avatar: 40dp (header) / 52dp (list)
- Font sizes: +1sp
- Padding: +2dp

---

## 🔍 Accessibility

### Content Descriptions
```kotlin
AsyncImage(
    model = userAvatar,
    contentDescription = "Profile picture of $userName",
    // ...
)
```

### Minimum Touch Targets
- Avatar: 48dp minimum (meets accessibility guidelines)
- Buttons: 48dp minimum
- List items: 56dp minimum height

### Color Contrast
- Text on gradient: WCAG AA compliant
- Online indicator: High contrast green
- Unread badge: High contrast red

---

## 🎭 Animation States

### Avatar Loading
```
Frame 1: [   ]  Empty
Frame 2: [ ⟳ ]  Spinner
Frame 3: [IMG]  Fade in image
```

### Online Status
```
State 1: No indicator (offline)
State 2: Green dot appears (online)
State 3: Pulsing animation (typing)
```

### Unread Badge
```
State 1: No badge (all read)
State 2: Badge appears with count
State 3: Badge animates on new message
```

---

## 🖼️ Image Specifications

### Profile Picture Requirements
- **Format**: JPG, PNG, WebP
- **Size**: 200x200 to 1000x1000 pixels
- **Aspect Ratio**: 1:1 (square)
- **File Size**: < 5MB recommended
- **Storage**: Firebase Storage

### Optimization
- Coil library handles caching
- Automatic downsampling for display
- Memory-efficient loading
- Disk cache for offline access

---

## 📊 Layout Hierarchy

### Chat Screen Header
```
TopAppBar
├── NavigationIcon (Back Button)
├── Title (Row)
│   ├── Box (Avatar Container)
│   │   ├── AsyncImage (Profile Picture)
│   │   └── Box (Online Indicator)
│   └── Column (Name & Status)
│       ├── Text (Name)
│       └── Text (Status)
└── Actions (Menu Button)
```

### Chat List Item
```
Card
└── Row
    ├── Box (Avatar Container)
    │   ├── AsyncImage (Profile Picture)
    │   └── Badge (Unread Count)
    ├── Column (Content)
    │   ├── Row (Name & Time)
    │   │   ├── Text (Name)
    │   │   └── Text (Time)
    │   └── Text (Last Message)
    └── Icon (Chevron)
```

---

## ✨ Professional Polish

### Shadows & Elevation
- Chat header: No shadow (flat design)
- Chat list cards: 1dp elevation
- Avatar: No shadow
- Unread badge: 2dp elevation

### Borders & Outlines
- Avatar: No border (clean look)
- Online indicator: 2dp white border
- Cards: No border (elevation only)
- Dividers: 0.5dp thickness

### Corner Radius
- Avatar: CircleShape (fully rounded)
- Cards: 12dp rounded corners
- Buttons: 10dp rounded corners
- Badges: CircleShape

---

## 🎯 Best Practices

### Do's ✅
- Use CircleShape for avatars
- Provide content descriptions
- Handle loading states
- Show fallback initials
- Maintain consistent sizing

### Don'ts ❌
- Don't use square avatars
- Don't skip loading states
- Don't ignore accessibility
- Don't use low-quality images
- Don't block UI during loading

---

## 📸 Screenshot Locations

For reference screenshots, check:
- `docs/screenshots/chat-header-with-avatar.png`
- `docs/screenshots/chat-list-with-avatars.png`
- `docs/screenshots/avatar-fallback-example.png`
- `docs/screenshots/online-indicator-detail.png`

---

**Visual Design Status**: ✅ Complete and Professional

All UI components follow Material Design 3 guidelines and maintain consistency across the app.
