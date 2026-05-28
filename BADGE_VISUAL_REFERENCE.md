# Badge Visual Reference & Diagrams

## Buyer App - Visual Layout

### HomeScreen - Complete View
```
╔═══════════════════════════════════════════════════════════════╗
║                    BUYER APP - HOME SCREEN                    ║
╠═══════════════════════════════════════════════════════════════╣
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │ Craftoria  [🔍]  [🔔 3]  [💬 2]  [🛒 5]              │ ║
║  │ (Primary Navigation - All Badges Here)                 │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │                   BANNER CAROUSEL                       │ ║
║  │                                                         │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │              FEATURED STORES SECTION                   │ ║
║  │                                                         │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │              CATEGORY TABS                             │ ║
║  │  All | Textiles | Jewelry | Home | Embroidery | Pottery│ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │              PRODUCTS GRID                             │ ║
║  │  [Product 1]  [Product 2]                             │ ║
║  │  [Product 3]  [Product 4]                             │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
╠═══════════════════════════════════════════════════════════════╣
║  Home  │  Orders [4]  │  Wishlist [2]  │  Profile           ║
║        │   (ORANGE)   │    (RED)       │                    ║
║  (Bottom Navigation - 2 Badges Here)                         ║
╚═══════════════════════════════════════════════════════════════╝
```

### Badge Details - Buyer App

#### TopBar Badges
```
┌─────────────────────────────────────────────────────────┐
│ Craftoria  [🔍]  [🔔 3]  [💬 2]  [🛒 5]              │
│                      ▲      ▲      ▲                   │
│                      │      │      │                   │
│              RED    RED    RED                         │
│              #E53935 #E53935 #E53935                   │
│                                                         │
│  🔔 Notifications Badge                               │
│     - Shows unread notifications count                │
│     - Updates: Real-time listener                     │
│     - Hides: When count = 0                           │
│     - Shows: "9+" for counts > 9                      │
│                                                         │
│  💬 Messages Badge                                    │
│     - Shows unread messages count                     │
│     - Updates: Real-time listener                     │
│     - Hides: When count = 0                           │
│     - Shows: "9+" for counts > 9                      │
│                                                         │
│  🛒 Cart Badge                                        │
│     - Shows cart items count                          │
│     - Updates: Real-time (CartViewModel)              │
│     - Hides: When cart is empty                       │
│     - Shows: "9+" for counts > 9                      │
└─────────────────────────────────────────────────────────┘
```

#### BottomBar Badges
```
┌─────────────────────────────────────────────────────────┐
│  Home  │  Orders [4]  │  Wishlist [2]  │  Profile      │
│        │   (ORANGE)   │    (RED)       │               │
│        │   #FF9800    │   #E53935      │               │
│                                                         │
│  Orders Badge (ORANGE)                                │
│     - Shows pending/processing/shipped orders         │
│     - Updates: Real-time listener                     │
│     - Hides: When no pending orders                   │
│     - Shows: "9+" for counts > 9                      │
│                                                         │
│  Wishlist Badge (RED)                                 │
│     - Shows wishlist items count                      │
│     - Updates: Real-time (WishlistViewModel)          │
│     - Hides: When wishlist is empty                   │
│     - Shows: "9+" for counts > 9                      │
└─────────────────────────────────────────────────────────┘
```

---

## Seller App - Visual Layout

### SellerDashboardScreen - Complete View
```
╔═══════════════════════════════════════════════════════════════╗
║                  SELLER APP - DASHBOARD SCREEN                ║
╠═══════════════════════════════════════════════════════════════╣
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │ Dashboard  [💬 1]  [🔔 2]                             │ ║
║  │ (Primary Navigation - 2 Badges Here)                  │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │  Welcome back, [Seller Name] ✓ Verified Seller       │ ║
║  │  (Welcome Banner)                                     │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │  Quick Access                                         │ ║
║  │  ┌──────────────┐  ┌──────────────┐                 │ ║
║  │  │ Manage       │  │ Price Offers │                 │ ║
║  │  │ Products     │  │ [3]          │                 │ ║
║  │  │              │  │ (RED badge)  │                 │ ║
║  │  └──────────────┘  └──────────────┘                 │ ║
║  │  ┌──────────────┐  ┌──────────────┐                 │ ║
║  │  │ Co-Seller    │  │ Learning     │                 │ ║
║  │  │ Stores       │  │ Resources    │                 │ ║
║  │  └──────────────┘  └──────────────┘                 │ ║
║  │  (Quick Access Menu - 1 Badge Here)                 │ ║
║  └─────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │  Sales Overview                                       │ ║
║  │  [Total Sales] [Active Orders] [Total Products] ...  │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
║  ┌─────────────────────────────────────────────────────────┐ ║
║  │  Recent Activity                                      │ ║
║  │  [Activity 1]                                         │ ║
║  │  [Activity 2]                                         │ ║
║  └─────────────────────────────────────────────────────────┘ ║
║                                                               ║
╠═══════════════════════════════════════════════════════════════╣
║  Dashboard  │  Add Product  │  Orders [5]  │  Profile        ║
║             │               │   (RED)      │                 ║
║             │               │   #E53935    │                 ║
║  (Bottom Navigation - 1 Badge Here)                          ║
╚═══════════════════════════════════════════════════════════════╝
```

### Badge Details - Seller App

#### TopBar Badges
```
┌─────────────────────────────────────────────────────────┐
│ Dashboard  [💬 1]  [🔔 2]                             │
│                ▲      ▲                                │
│                │      │                                │
│              RED    RED                                │
│              #E53935 #E53935                           │
│                                                         │
│  💬 Messages Badge                                    │
│     - Shows unread messages from buyers               │
│     - Updates: Real-time listener                     │
│     - Hides: When no unread messages                  │
│     - Shows: "9+" for counts > 9                      │
│                                                         │
│  🔔 Notifications Badge                               │
│     - Shows unread notifications count                │
│     - Updates: Real-time listener                     │
│     - Hides: When count = 0                           │
│     - Shows: "9+" for counts > 9                      │
└─────────────────────────────────────────────────────────┘
```

#### QuickAccessMenu Badge
```
┌─────────────────────────────────────────────────────────┐
│  Quick Access                                           │
│  ┌──────────────┐  ┌──────────────┐                   │
│  │ Manage       │  │ Price Offers │                   │
│  │ Products     │  │ [3]          │                   │
│  │              │  │ (RED badge)  │                   │
│  │              │  │ ▲            │                   │
│  │              │  │ │            │                   │
│  │              │  │ RED #E53935  │                   │
│  └──────────────┘  └──────────────┘                   │
│  ┌──────────────┐  ┌──────────────┐                   │
│  │ Co-Seller    │  │ Learning     │                   │
│  │ Stores       │  │ Resources    │                   │
│  └──────────────┘  └──────────────┘                   │
│                                                         │
│  Price Offers Badge (RED)                             │
│     - Shows pending price negotiations                │
│     - Updates: Real-time listener                     │
│     - Hides: When no pending negotiations             │
│     - Shows: "9+" for counts > 9                      │
│     - Location: Top-right corner of card              │
└─────────────────────────────────────────────────────────┘
```

#### BottomBar Badge
```
┌─────────────────────────────────────────────────────────┐
│  Dashboard  │  Add Product  │  Orders [5]  │  Profile  │
│             │               │   (RED)      │           │
│             │               │   #E53935    │           │
│             │               │   ▲          │           │
│             │               │   │          │           │
│             │               │ RED #E53935  │           │
│                                                         │
│  Orders Badge (RED)                                   │
│     - Shows new unviewed orders count                 │
│     - Updates: Real-time listener                     │
│     - Hides: When no new orders                       │
│     - Shows: "9+" for counts > 9                      │
│     - Clears: When seller views order                 │
└─────────────────────────────────────────────────────────┘
```

---

## Badge Color Palette

### Color Specifications
```
┌─────────────────────────────────────────────────────────┐
│  RED - Urgent/Important                                │
│  ┌─────────────────────────────────────────────────────┐
│  │ Hex: #E53935                                        │
│  │ RGB: (229, 57, 53)                                  │
│  │ Usage: Messages, Notifications, Cart, Wishlist,    │
│  │        Negotiations, New Orders (Seller)           │
│  │ Appearance: Bright red, high contrast              │
│  └─────────────────────────────────────────────────────┘
│
│  ORANGE - Pending/Attention                            │
│  ┌─────────────────────────────────────────────────────┐
│  │ Hex: #FF9800                                        │
│  │ RGB: (255, 152, 0)                                  │
│  │ Usage: Buyer Orders (pending/processing/shipped)   │
│  │ Appearance: Warm orange, moderate contrast         │
│  └─────────────────────────────────────────────────────┘
│
│  BLUE - Information (Reserved)                         │
│  ┌─────────────────────────────────────────────────────┐
│  │ Hex: #2196F3                                        │
│  │ RGB: (33, 150, 243)                                 │
│  │ Usage: Reserved for future use                      │
│  │ Appearance: Bright blue, high contrast             │
│  └─────────────────────────────────────────────────────┘
└─────────────────────────────────────────────────────────┘
```

---

## Badge Display States

### State 1: No Badge (Count = 0)
```
┌─────────────────────────────────────────────────────────┐
│  [🔔]  [💬]  [🛒]                                      │
│                                                         │
│  No badges shown - clean UI                            │
│  Count = 0 → Badge hidden                              │
└─────────────────────────────────────────────────────────┘
```

### State 2: Single Digit (Count 1-9)
```
┌─────────────────────────────────────────────────────────┐
│  [🔔 3]  [💬 2]  [🛒 5]                                │
│      ▲       ▲       ▲                                 │
│      │       │       │                                 │
│    RED     RED     RED                                 │
│  #E53935 #E53935 #E53935                              │
│                                                         │
│  Count 1-9 → Show exact count                          │
└─────────────────────────────────────────────────────────┘
```

### State 3: Double Digit (Count 10+)
```
┌─────────────────────────────────────────────────────────┐
│  [🔔 9+]  [💬 9+]  [🛒 9+]                             │
│       ▲        ▲        ▲                              │
│       │        │        │                              │
│     RED      RED      RED                              │
│   #E53935  #E53935  #E53935                            │
│                                                         │
│  Count 10+ → Show "9+"                                 │
└─────────────────────────────────────────────────────────┘
```

---

## Badge Animation & Interaction

### Badge Appearance
```
Timeline: 0ms → 300ms
┌─────────────────────────────────────────────────────────┐
│  0ms:    [🔔]           (No badge)                      │
│  100ms:  [🔔 ●]         (Badge appears, scale 0.5)     │
│  200ms:  [🔔 ●●]        (Badge grows, scale 0.75)      │
│  300ms:  [🔔 3]         (Badge fully visible, scale 1)  │
│                                                         │
│  Animation: Smooth scale-in with fade                  │
│  Duration: 300ms                                        │
│  Easing: EaseInOutQuad                                 │
└─────────────────────────────────────────────────────────┘
```

### Badge Disappearance
```
Timeline: 0ms → 300ms
┌─────────────────────────────────────────────────────────┐
│  0ms:    [🔔 3]         (Badge visible, scale 1)        │
│  100ms:  [🔔 ●●]        (Badge shrinks, scale 0.75)     │
│  200ms:  [🔔 ●]         (Badge fades, scale 0.5)        │
│  300ms:  [🔔]           (Badge hidden)                  │
│                                                         │
│  Animation: Smooth scale-out with fade                 │
│  Duration: 300ms                                        │
│  Easing: EaseInOutQuad                                 │
└─────────────────────────────────────────────────────────┘
```

---

## Real-time Update Flow

### Message Received → Badge Update
```
┌──────────────────────────────────────────────────────────┐
│  1. Message sent by seller                              │
│     └─→ Firestore: messages collection updated          │
│                                                          │
│  2. Real-time listener triggered                        │
│     └─→ UnreadMessageViewModel.unreadCount updated      │
│                                                          │
│  3. StateFlow emits new value                           │
│     └─→ HomeScreen recomposes                           │
│                                                          │
│  4. Badge updates on UI                                 │
│     └─→ [💬 2] appears with animation                   │
│                                                          │
│  Total latency: ~100-500ms (network dependent)          │
└──────────────────────────────────────────────────────────┘
```

### Order Received → Badge Update (Seller)
```
┌──────────────────────────────────────────────────────────┐
│  1. Order placed by buyer                               │
│     └─→ Firestore: orders collection updated            │
│        └─→ seller_id = current seller                   │
│        └─→ status = "pending"                           │
│        └─→ is_viewed = false                            │
│                                                          │
│  2. Real-time listener triggered                        │
│     └─→ SellerDashboardScreen listener fires            │
│                                                          │
│  3. newOrdersCount updated                              │
│     └─→ SellerBottomNavigation recomposes               │
│                                                          │
│  4. Badge updates on UI                                 │
│     └─→ Orders [1] appears with animation               │
│                                                          │
│  5. Seller opens order                                  │
│     └─→ OrderRepository.markOrderAsViewed()             │
│     └─→ Firestore: is_viewed = true                     │
│                                                          │
│  6. Listener updates count                              │
│     └─→ Badge disappears (count = 0)                    │
│                                                          │
│  Total latency: ~100-500ms per update                   │
└──────────────────────────────────────────────────────────┘
```

---

## Badge Positioning Reference

### TopBar Badge Positioning
```
┌─────────────────────────────────────────────────────────┐
│ Craftoria  [🔍]  [🔔 3]  [💬 2]  [🛒 5]              │
│                   ▲      ▲      ▲                       │
│                   │      │      │                       │
│              Padding: 8dp between icons                 │
│              Badge offset: -4dp (top-right)             │
│              Badge size: 20dp diameter                  │
│              Font size: 10sp                            │
└─────────────────────────────────────────────────────────┘
```

### BottomBar Badge Positioning
```
┌─────────────────────────────────────────────────────────┐
│  Home  │  Orders [4]  │  Wishlist [2]  │  Profile      │
│        │      ▲       │       ▲        │               │
│        │      │       │       │        │               │
│        │  Badge offset: -2dp (top-right)               │
│        │  Badge size: 18dp diameter                    │
│        │  Font size: 10sp                              │
└─────────────────────────────────────────────────────────┘
```

### QuickAccessCard Badge Positioning
```
┌──────────────────────────────────────┐
│ Price Offers                         │
│ [3]                                  │
│ ▲                                    │
│ │                                    │
│ Badge offset: 8dp (top-right)        │
│ Badge size: 24dp diameter            │
│ Font size: 10sp                      │
│ Background: RED (#E53935)            │
│                                      │
│ [Icon]                               │
│ Price Offers                         │
└──────────────────────────────────────┘
```

---

## Summary

**Total Badges**: 9
- **Buyer App**: 5 (Cart, Messages, Notifications, Orders, Wishlist)
- **Seller App**: 4 (Messages, Notifications, Orders, Negotiations)

**Colors Used**: 2
- RED (#E53935) - 8 badges
- ORANGE (#FF9800) - 1 badge

**Update Mechanism**: Real-time Firebase listeners
**Display Logic**: Hide when 0, show "9+" for 10+
**Status**: ✅ COMPLETE AND PRODUCTION READY
