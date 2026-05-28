# Product Approval System - Visual Guide

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CRAFTORIA PLATFORM                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────────┐         ┌──────────────────────┐         │
│  │   MOBILE APP         │         │   WEB DASHBOARD      │         │
│  │   (Android)          │         │   (Admin Panel)      │         │
│  ├──────────────────────┤         ├──────────────────────┤         │
│  │ • Add Product        │         │ • View Products      │         │
│  │ • Manage Products    │         │ • Approve Products   │         │
│  │ • See Status         │         │ • Reject Products    │         │
│  │ • Filter Pending     │         │ • View Rejection     │         │
│  │ • View Badges        │         │ • Track Activities   │         │
│  └──────────┬───────────┘         └──────────┬───────────┘         │
│             │                                 │                     │
│             └─────────────────┬───────────────┘                     │
│                               │                                     │
│                    ┌──────────▼──────────┐                         │
│                    │  FIRESTORE (DB)    │                         │
│                    ├────────────────────┤                         │
│                    │ • products         │                         │
│                    │ • admin_activities │                         │
│                    │ • notifications    │                         │
│                    └────────────────────┘                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Product Lifecycle

```
STEP 1: SELLER UPLOADS
┌─────────────────────────────────────────┐
│ Seller fills product form                │
│ • Title, Description, Price, Images     │
│ • Category, Stock, Specifications       │
└────────────────┬────────────────────────┘
                 │
                 ▼
STEP 2: PRODUCT CREATED
┌─────────────────────────────────────────┐
│ Product saved to Firestore              │
│ • approval_status = "pending"           │
│ • is_active = false                     │
│ • created_at = now                      │
└────────────────┬────────────────────────┘
                 │
                 ▼
STEP 3: SELLER NOTIFICATION
┌─────────────────────────────────────────┐
│ Seller sees "Pending" badge             │
│ • In Manage Products screen             │
│ • Can filter by "Pending"               │
│ • Can edit product                      │
└────────────────┬────────────────────────┘
                 │
                 ▼
STEP 4: ADMIN REVIEW
┌─────────────────────────────────────────┐
│ Admin views product on web dashboard    │
│ • Sees all product details              │
│ • Can approve or reject                 │
│ • Can add rejection reason              │
└────────────────┬────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
    APPROVE          REJECT
        │                 │
        ▼                 ▼
STEP 5A: APPROVED    STEP 5B: REJECTED
┌──────────────────┐ ┌──────────────────┐
│ • approval_status│ │ • approval_status│
│   = "approved"   │ │   = "rejected"   │
│ • is_active=true │ │ • is_active=false│
│ • approved_at    │ │ • rejection_reason
│ • approved_by    │ │ • rejection_notes│
└────────┬─────────┘ └────────┬─────────┘
         │                    │
         ▼                    ▼
STEP 6A: VISIBLE      STEP 6B: HIDDEN
┌──────────────────┐ ┌──────────────────┐
│ • Buyers see it  │ │ • Buyers don't   │
│ • In search      │ │   see it         │
│ • In categories  │ │ • Seller sees    │
│ • Can purchase   │ │   rejection info │
└──────────────────┘ └────────┬─────────┘
                              │
                              ▼
                        STEP 7: RESUBMIT
                        ┌──────────────────┐
                        │ Seller can:      │
                        │ • Edit product   │
                        │ • Resubmit       │
                        │ • Goes to pending│
                        └──────────────────┘
```

---

## UI Components

### Android App - Manage Products Screen

```
┌─────────────────────────────────────────────────────┐
│  ← My Products                                      │
├─────────────────────────────────────────────────────┤
│  ⊕ Add New Product                                  │
├─────────────────────────────────────────────────────┤
│  [All] [Active] [Inactive] [Out of Stock] [Pending] │
├─────────────────────────────────────────────────────┤
│  🔍 Search your products        [Newest ▼]          │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────────┐  ┌──────────────────┐        │
│  │ [Product Image]  │  │ [Product Image]  │        │
│  │ ⋮ (menu)         │  │ ⋮ (menu)         │        │
│  ├──────────────────┤  ├──────────────────┤        │
│  │ Handmade Flowers │  │ Handmade WallArt │        │
│  │ PKR 1,500        │  │ PKR 1,000        │        │
│  │                  │  │                  │        │
│  │ [In Stock]       │  │ [In Stock]       │        │
│  │ [Active]         │  │ [Active]         │        │
│  │ [Pending] ◄──────┼──┼─ NEW BADGE      │        │
│  │                  │  │                  │        │
│  │ ◯ – 14 +         │  │ ◯ – 7 +          │        │
│  └──────────────────┘  └──────────────────┘        │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Web Dashboard - Product Management

```
┌─────────────────────────────────────────────────────────────────┐
│ All Products                                                    │
│ Manage all products listed on Craftoria                         │
│                                                    [+ Add Featured]
├─────────────────────────────────────────────────────────────────┤
│ 🔍 Search by product name...  [Category ▼] [Price Range ▼]    │
├─────────────────────────────────────────────────────────────────┤
│ [All] [Active] [Inactive] [Flagged]                            │
│ [All] [Pending] [Approved] [Rejected] ◄─── NEW FILTERS        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Product │ Seller │ Price │ Category │ Status │ Approval │ Act. │
├─────────┼────────┼───────┼──────────┼────────┼──────────┼─────┤
│ Vase    │ Ayesha │ 2500  │ Pottery  │ Active │ Pending  │ ✓ ✎ │
│         │        │       │          │        │          │ ✗ ✓ │
├─────────┼────────┼───────┼──────────┼────────┼──────────┼─────┤
│ Painting│ Ahmed  │ 5000  │ Art      │ Active │ Approved │ ✓ ✎ │
│         │        │       │          │        │          │ ✗   │
├─────────┼────────┼───────┼──────────┼────────┼──────────┼─────┤
│ Jewelry │ Fatima │ 1500  │ Jewelry  │ Inactive│ Rejected │ ✓ ✎ │
│         │        │       │          │        │          │ ✗ ✓ │
└─────────┴────────┴───────┴──────────┴────────┴──────────┴─────┘

Legend:
✓ = Approve (green)
✗ = Reject (orange)
✎ = Edit (blue)
🗑 = Delete (red)
👁 = View (blue)
```

---

## Badge Colors & Meanings

### Approval Status Badges

```
┌─────────────────────────────────────────────────────┐
│ PENDING                                             │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Background: #FFF3CD (Light Yellow)              │ │
│ │ Text: #856404 (Dark Yellow)                     │ │
│ │ Meaning: Awaiting admin review                  │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ APPROVED                                            │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Background: #D4EDDA (Light Green)               │ │
│ │ Text: #155724 (Dark Green)                      │ │
│ │ Meaning: Admin approved, visible to buyers      │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ REJECTED                                            │
│ ┌─────────────────────────────────────────────────┐ │
│ │ Background: #F8D7DA (Light Red)                 │ │
│ │ Text: #721C24 (Dark Red)                        │ │
│ │ Meaning: Admin rejected, hidden from buyers     │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## Action Button Colors

```
┌──────────────────────────────────────────────────────┐
│ APPROVE (Green)                                      │
│ ┌────────────────────────────────────────────────┐   │
│ │ Background: rgba(67, 160, 71, 0.12)            │   │
│ │ Icon: #43A047 (Green)                          │   │
│ │ Action: Set approval_status = "approved"       │   │
│ │ Shows: Only for pending products               │   │
│ └────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ REJECT (Orange)                                      │
│ ┌────────────────────────────────────────────────┐   │
│ │ Background: rgba(255, 152, 0, 0.12)            │   │
│ │ Icon: #FF9800 (Orange)                         │   │
│ │ Action: Set approval_status = "rejected"       │   │
│ │ Shows: Only for pending products               │   │
│ └────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ RE-APPROVE (Green)                                   │
│ ┌────────────────────────────────────────────────┐   │
│ │ Background: rgba(67, 160, 71, 0.12)            │   │
│ │ Icon: #43A047 (Green)                          │   │
│ │ Action: Set approval_status = "approved"       │   │
│ │ Shows: Only for rejected products              │   │
│ └────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    SELLER MOBILE APP                        │
│                                                             │
│  1. Upload Product                                          │
│     └─> Product created with approval_status = "pending"   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ POST /products
                     │
                     ▼
        ┌────────────────────────────┐
        │   FIRESTORE DATABASE       │
        │                            │
        │  products collection       │
        │  ├─ id                     │
        │  ├─ title                  │
        │  ├─ approval_status ◄──────┼─ "pending"
        │  ├─ is_active              │
        │  └─ ...                    │
        └────────────────────────────┘
                     │
                     │ Real-time sync
                     │
                     ▼
        ┌────────────────────────────┐
        │   SELLER MOBILE APP        │
        │                            │
        │  2. See Pending Badge      │
        │     └─> Filter by Pending  │
        └────────────────────────────┘
                     │
                     │ (Seller waits for approval)
                     │
                     ▼
        ┌────────────────────────────┐
        │   WEB DASHBOARD (ADMIN)    │
        │                            │
        │  3. View Pending Products  │
        │     └─> Filter by Pending  │
        └────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
    APPROVE              REJECT
        │                         │
        │ UPDATE                  │ UPDATE
        │ approval_status         │ approval_status
        │ = "approved"            │ = "rejected"
        │ is_active = true        │ rejection_reason
        │                         │ is_active = false
        │                         │
        ▼                         ▼
    ┌─────────────┐          ┌─────────────┐
    │ FIRESTORE   │          │ FIRESTORE   │
    │ (Updated)   │          │ (Updated)   │
    └──────┬──────┘          └──────┬──────┘
           │                        │
           │ Real-time sync         │ Real-time sync
           │                        │
           ▼                        ▼
    ┌─────────────┐          ┌─────────────┐
    │ SELLER APP  │          │ SELLER APP  │
    │ Sees        │          │ Sees        │
    │ "Approved"  │          │ "Rejected"  │
    │ Badge       │          │ Badge       │
    └─────────────┘          └─────────────┘
           │                        │
           ▼                        ▼
    ┌─────────────┐          ┌─────────────┐
    │ BUYER APP   │          │ SELLER CAN  │
    │ Can see &   │          │ EDIT &      │
    │ purchase    │          │ RESUBMIT    │
    └─────────────┘          └─────────────┘
```

---

## State Transitions

```
                    ┌─────────────┐
                    │   CREATED   │
                    │  (pending)  │
                    └──────┬──────┘
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
            APPROVED              REJECTED
            (active)              (inactive)
                │                     │
                │                     │
                ▼                     ▼
            VISIBLE              HIDDEN
            TO BUYERS            TO BUYERS
                │                     │
                │                     │
                ▼                     ▼
            PURCHASABLE           CAN RESUBMIT
                                      │
                                      ▼
                                  PENDING
                                  (again)
```

---

## Firestore Document Structure

```
products/product_123
├── id: "product_123"
├── title: "Handmade Ceramic Vase"
├── description: "Beautiful handcrafted vase..."
├── price: 2500
├── category: "Pottery & Ceramics"
├── seller_id: "seller_456"
├── seller_name: "Ayesha Crafts"
├── is_active: true
├── stock: 10
│
├── ✅ APPROVAL FIELDS (NEW)
├── approval_status: "pending|approved|rejected"
├── rejection_reason: "not_handicraft|poor_quality|..."
├── rejection_notes: "Additional details from admin"
├── approved_at: 1234567890
├── approved_by: "admin_user_id"
├── rejected_at: 1234567890
├── rejected_by: "admin_user_id"
│
├── image_urls: ["url1", "url2", ...]
├── created_at: 1234567890
├── updated_at: 1234567890
└── ... other fields
```

---

## Filter Logic

```
┌─────────────────────────────────────────────────────┐
│ FILTER: ALL                                         │
│ Shows: All products except drafts                   │
│ Includes: pending, approved, rejected, active       │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ FILTER: PENDING                                     │
│ Shows: approval_status == "pending"                 │
│ Includes: Products awaiting admin review            │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ FILTER: APPROVED                                    │
│ Shows: approval_status == "approved"                │
│ Includes: Products approved by admin                │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ FILTER: REJECTED                                    │
│ Shows: approval_status == "rejected"                │
│ Includes: Products rejected by admin                │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ FILTER: ACTIVE                                      │
│ Shows: is_active == true                            │
│ Includes: All active products (approved only)       │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ FILTER: INACTIVE                                    │
│ Shows: is_active == false                           │
│ Includes: Inactive products (rejected, etc)         │
└─────────────────────────────────────────────────────┘
```

---

## Timeline Example

```
Day 1, 10:00 AM
├─ Seller uploads "Handmade Vase"
├─ approval_status = "pending"
└─ Seller sees "Pending" badge

Day 1, 2:00 PM
├─ Admin reviews product
├─ Admin clicks "Approve"
├─ approval_status = "approved"
├─ is_active = true
└─ approved_at = timestamp

Day 1, 2:05 PM
├─ Seller sees "Approved" badge
├─ Buyer can see product
└─ Buyer can purchase

Day 2, 10:00 AM
├─ First purchase made
├─ Product visible in search
└─ Seller receives order
```

---

## Error Scenarios

```
❌ SCENARIO 1: Admin tries to approve without permission
   └─ Error: "Permission denied"

❌ SCENARIO 2: Seller tries to change approval_status
   └─ Error: "Not authorized"

❌ SCENARIO 3: Admin rejects without reason
   └─ Error: "Rejection reason required"

❌ SCENARIO 4: Product not found
   └─ Error: "Product ID is missing"

✅ SCENARIO 5: All validations pass
   └─ Success: "Product approved successfully!"
```

---

## Summary

This visual guide shows:
- ✅ Complete system architecture
- ✅ Product lifecycle flow
- ✅ UI component layouts
- ✅ Badge colors and meanings
- ✅ Data flow between systems
- ✅ State transitions
- ✅ Database structure
- ✅ Filter logic
- ✅ Timeline example
- ✅ Error scenarios
