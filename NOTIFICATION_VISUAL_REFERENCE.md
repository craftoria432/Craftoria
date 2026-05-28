# Notification System - Visual Reference

## Badge Display

### Buyer (HomeScreen)
```
┌─────────────────────────────────────────────────────────┐
│                                                           │
│  🏠 Home    💬 Chat    ❤️ Wishlist    🛒 Cart    🔔[3]  │
│                                                           │
│  ✅ Badge shows: 3 unread notifications                  │
│  ✅ Color: Red (0xFFE53935)                              │
│  ✅ Updates: Real-time                                   │
│  ✅ Max display: "9+"                                    │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### Seller (SellerDashboardScreen)
```
┌─────────────────────────────────────────────────────────┐
│                                                           │
│  My Dashboard    💬[2]    🔔[5]                          │
│                                                           │
│  ✅ Badge shows: 5 unread notifications                  │
│  ✅ Color: Red (Error color)                             │
│  ✅ Updates: Real-time                                   │
│  ✅ Max display: "9+"                                    │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## Notification Screen Layout

```
┌─────────────────────────────────────────────────────────┐
│  ← Notifications                    🗑️  Mark all read   │
├─────────────────────────────────────────────────────────┤
│  [All] [Orders] [Messages] [Promotions] [System]        │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 🛍️  New Order Received                    🗑️   │   │
│  │ Order #ABC123 from John Doe                     │   │
│  │ 🏪 Store Name | 👥 5 Members                    │   │
│  │ 2 hours ago                                     │   │
│  │                                                 │   │
│  │ [View Order]                                    │   │
│  └─────────────────────────────────────────────────┘   │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 💬  New Message from Sarah                 🗑️   │   │
│  │ "Hi, is this product still available?"          │   │
│  │ 4 hours ago                                     │   │
│  │                                                 │   │
│  │ [View & Reply]                                  │   │
│  └─────────────────────────────────────────────────┘   │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │ ✅  Order Delivered                        🗑️   │   │
│  │ Your order #XYZ789 has been delivered           │   │
│  │ 1 day ago                                       │   │
│  │                                                 │   │
│  │ [Track Order]                                   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## Notification Categories & Icons

```
┌──────────────────────────────────────────────────────────┐
│ Category         │ Icon  │ Color      │ Examples          │
├──────────────────────────────────────────────────────────┤
│ ORDERS           │ 🛍️   │ Pink       │ New order, delivery│
│ MESSAGES         │ 💬   │ Blue       │ Chat, negotiation  │
│ PROMOTIONS       │ 📢   │ Orange     │ Offers, discounts  │
│ SYSTEM           │ ✅   │ Green      │ Verification       │
│ REPORT           │ 🚩   │ Red        │ Product reported   │
│ ADMIN_MESSAGE    │ ⚙️   │ Dark Red   │ Admin messages     │
│ PAYMENTS         │ 💳   │ Purple     │ Payment, payout    │
│ STORE_RATING     │ ⭐   │ Gold       │ Store ratings      │
└──────────────────────────────────────────────────────────┘
```

---

## Real-Time Update Flow

```
Timeline: User receives new notification

T=0s: Seller places order
      ↓
T=0.1s: Order saved to Firestore
      ↓
T=0.2s: FCM sends push to buyer
      ↓
T=0.3s: Notification created in Firestore
      ↓
T=0.4s: Firestore listener detects change
      ↓
T=0.5s: _unreadCount StateFlow updates
      ↓
T=0.6s: Badge recomposes with new count
      ↓
T=0.7s: User sees badge update (🔔[1] → 🔔[2])
      ↓
Total: ~700ms from order to badge update
```

---

## Badge Behavior Examples

### Scenario 1: New Notification Arrives
```
Before:  🔔[2]
         ↓ (new notification arrives)
After:   🔔[3]  ← Updates immediately
```

### Scenario 2: Mark as Read
```
Before:  🔔[3]
         ↓ (user marks notification as read)
After:   🔔[2]  ← Decreases immediately
```

### Scenario 3: Mark All as Read
```
Before:  🔔[5]
         ↓ (user taps "Mark all read")
After:   🔔[0]  ← Badge disappears
```

### Scenario 4: Delete Notification
```
Before:  🔔[4]
         ↓ (user deletes notification)
After:   🔔[3]  ← Decreases immediately
```

### Scenario 5: Large Count
```
Before:  🔔[8]
         ↓ (more notifications arrive)
After:   🔔[9+]  ← Shows "9+" for counts ≥ 10
```

---

## Notification Card Components

```
┌─────────────────────────────────────────────────────────┐
│                                                           │
│  ┌────┐                                                  │
│  │ 🛍️ │  Title: "New Order Received"                    │
│  └────┘  Description: "Order #ABC123 from John Doe"     │
│          Store: 🏪 Store Name | 👥 5 Members            │
│          Time: 2 hours ago                              │
│                                                           │
│          [View Order]                                    │
│                                                           │
│          🔵 (Unread indicator)                           │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### Components Breakdown
- **Icon**: Category-specific icon in colored circle
- **Title**: Main notification title (bold)
- **Description**: Detailed message
- **Store Info**: (Optional) Store name and member count
- **Time**: "X hours/days ago" format
- **Action Button**: Context-specific action
- **Unread Dot**: Blue dot if unread
- **Delete Button**: Appears on hover/long-press

---

## Notification Action Buttons

```
┌─────────────────────────────────────────────────────────┐
│ Action Type          │ Button Text      │ Color         │
├─────────────────────────────────────────────────────────┤
│ VIEW_ORDER           │ View Order       │ Primary       │
│ TRACK_ORDER          │ Track Order      │ Primary       │
│ ACCEPT_INVITATION    │ Accept / Decline │ Green / Gray  │
│ REPLY_MESSAGE        │ View & Reply     │ Blue          │
│ VIEW_STORE           │ View Store       │ Primary       │
│ VIEW_PROMOTIONS      │ Browse Offers    │ Primary       │
│ RATE_ORDER           │ Rate Order       │ Primary       │
│ VIEW_PRODUCT         │ View Product     │ Primary       │
│ VIEW_PAYMENT         │ View Payment     │ Primary       │
│ VIEW_REPORT          │ View Report      │ Primary       │
│ VIEW_RATING          │ View Rating      │ Primary       │
│ VIEW_PROFILE         │ View Profile     │ Primary       │
└─────────────────────────────────────────────────────────┘
```

---

## Filter Tabs

```
┌─────────────────────────────────────────────────────────┐
│  [All] [Orders] [Messages] [Promotions] [System]        │
│   ↑                                                      │
│   └─ Selected tab (Primary color background)            │
│                                                           │
│  Unselected tabs: White background with border          │
└─────────────────────────────────────────────────────────┘
```

---

## Empty State

```
┌─────────────────────────────────────────────────────────┐
│                                                           │
│                      🔔                                  │
│                                                           │
│              No Notifications                            │
│                                                           │
│         You're all caught up!                            │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## Selection Mode

```
Before Selection:
┌─────────────────────────────────────────────────────────┐
│  ← Notifications                    🗑️  Mark all read   │
└─────────────────────────────────────────────────────────┘

After Tapping Delete:
┌─────────────────────────────────────────────────────────┐
│  ← Notifications    [Delete (2)] [Cancel]               │
└─────────────────────────────────────────────────────────┘

Notification Cards:
┌─────────────────────────────────────────────────────────┐
│  ☑️  🛍️  New Order Received                             │
│      Order #ABC123 from John Doe                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  ☐  💬  New Message from Sarah                          │
│      "Hi, is this product still available?"             │
└─────────────────────────────────────────────────────────┘
```

---

## Current vs Missing Notifications

### Currently Implemented (4)
```
✅ Chat Messages
   └─ When: New message in chat
   └─ Badge: Yes
   └─ Screen: MESSAGES category

✅ Order Updates
   └─ When: Order status changes
   └─ Badge: Yes
   └─ Screen: ORDERS category

✅ Negotiation Requests
   └─ When: Buyer makes price offer
   └─ Badge: Yes
   └─ Screen: MESSAGES category

✅ Product Shared
   └─ When: Product shared in chat
   └─ Badge: Yes
   └─ Screen: MESSAGES category
```

### Missing for Buyers (7)
```
❌ Order Delivery Confirmation
❌ Order Cancellation by Seller
❌ Refund Processed
❌ Store Rating Reminder
❌ Promotional Offers
❌ Wishlist Item Back in Stock
❌ Price Drop Alert
```

### Missing for Sellers (9)
```
❌ New Order Received
❌ Order Cancellation Request
❌ Payment Received
❌ Payout Processed
❌ Product Reported
❌ Store Rating Received
❌ Co-Seller Invitation
❌ Admin Message
❌ Product Approval Status
❌ Seller Verification Status
```

---

## Badge Count Examples

```
No notifications:     (no badge shown)
1 notification:       🔔[1]
5 notifications:      🔔[5]
9 notifications:      🔔[9]
10+ notifications:    🔔[9+]
```

---

## Color Scheme

```
Badge Background:     Red (0xFFE53935) for buyer
                      Error color (red) for seller

Badge Text:           White

Category Icons:
  - Orders:           Pink (0xFFE91E8C)
  - Messages:         Blue (0xFF1976D2)
  - Promotions:       Orange (0xFFF57F17)
  - System:           Green (0xFF2E7D32)
  - Report:           Pink (0xFFE91E63)
  - Admin:            Dark Red (0xFFD32F2F)

Action Buttons:
  - Primary:          Primary color
  - Success:          Green
  - Error:            Red
  - Secondary:        Gray
```

---

## Summary

| Feature | Status | Buyer | Seller |
|---------|--------|-------|--------|
| Badge Display | ✅ | Yes | Yes |
| Real-time Updates | ✅ | Yes | Yes |
| Notification Screen | ✅ | Yes | Yes |
| Filtering | ✅ | Yes | Yes |
| Mark as Read | ✅ | Yes | Yes |
| Delete | ✅ | Yes | Yes |
| Selection Mode | ✅ | Yes | Yes |
| Action Buttons | ✅ | Yes | Yes |
| Chat Notifications | ✅ | Yes | Yes |
| Order Notifications | ✅ | Yes | Yes |
| Additional Types | ❌ | 7 missing | 9 missing |
