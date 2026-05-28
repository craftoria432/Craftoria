# Product Card Visual Guide

## Buyer Home Screen - ProductCard

### Card Dimensions
```
Width: Match parent (2-column grid with 12dp spacing)
Height: Wraps content (approximately 380-420dp)
Corners: 12dp rounded
Border: 1dp light gray (#E8E8E8)
Shadow: 1dp elevation (4dp on press)
```

### Layout Structure
```
┌─────────────────────────────────────┐
│                                     │
│   Product Image (180dp height)      │
│   [Wishlist ❤️ button - top right]  │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  Product Title (2 lines max)        │
│  by Seller Name ✓ verified          │
│                                     │
│  PKR 5,999                          │
│                                     │
│  [In Stock] [Negotiable]            │
│                                     │
│  [Add to Cart] ────────────────────│
│                                     │
└─────────────────────────────────────┘
```

### Color Palette
- **Card Background**: White (#FFFFFF)
- **Border**: Light Gray (#E8E8E8)
- **Image Background**: Off-white (#F8F8F8)
- **Title Text**: Dark (#1A1A1A)
- **Seller Text**: Medium Gray (#888888)
- **Price**: Primary Pink (#E91E63)
- **Stock Badge**: Light Green (#E8F5E9) with Green text (#2E7D32)
- **Negotiable Badge**: Light Blue (#E3F2FD) with Blue text (#1565C0)
- **Button**: Primary Pink (#E91E63)

### Typography
- **Title**: 13sp, SemiBold, 2 lines, 34dp min height
- **Seller**: 11sp, Regular, Gray
- **Price**: 16sp, Bold, Pink
- **Badge Text**: 9sp, SemiBold
- **Button Text**: 12sp, SemiBold

### Spacing
- **Image Height**: 180dp
- **Content Padding**: 12dp all sides
- **Section Gaps**: 8dp
- **Badge Gap**: 6dp
- **Button Height**: 38dp

---

## Seller Manage Products - ManageProductCard

### Card Dimensions
```
Width: Match parent (2-column grid with 12dp spacing)
Height: Fixed 340dp (ensures perfect alignment)
Corners: 12dp rounded
Border: 1dp light gray (#E8E8E8)
Shadow: 1dp elevation (4dp on press)
```

### Layout Structure
```
┌─────────────────────────────────────┐
│                                     │
│   Product Image (160dp height)      │
│   [Menu ⋮ button - top right]       │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  Product Title (2 lines max)        │
│  PKR 5,999                          │
│                                     │
│  [In Stock] [Active] [Pending]      │
│                                     │
├─────────────────────────────────────┤
│                                     │
│  [Toggle] ────── [−] 12 [+]        │
│                                     │
└─────────────────────────────────────┘
```

### Color Palette
- **Card Background**: White (#FFFFFF)
- **Border**: Light Gray (#E8E8E8)
- **Image Background**: Off-white (#F8F8F8)
- **Title Text**: Dark (#1A1A1A)
- **Price**: Primary Pink (#E91E63)
- **Stock Badge**: Light Green (#E8F5E9) with Green text (#2E7D32)
- **Active Badge**: Light Green (#E8F5E9) with Green text (#2E7D32)
- **Pending Badge**: Light Yellow (#FFF3CD) with Orange text (#856404)
- **Rejected Badge**: Light Red (#F8D7DA) with Red text (#721C24)
- **Toggle**: Green when active (#4CAF50)
- **Counter Buttons**: Light Gray (#F0F0F0)

### Typography
- **Title**: 13sp, SemiBold, 2 lines, 34dp min height
- **Price**: 15sp, Bold, Pink
- **Badge Text**: 10sp, SemiBold
- **Stock Number**: 12sp, SemiBold

### Spacing
- **Image Height**: 160dp
- **Content Padding**: 12dp all sides
- **Section Gaps**: 8dp
- **Badge Gap**: 6dp
- **Divider**: 1dp
- **Control Row Height**: 32dp

---

## Grid Layout

### Both Screens
```
Columns: 2 (GridCells.Fixed(2))
Horizontal Spacing: 12dp
Vertical Spacing: 12dp
Horizontal Padding: 15dp
Vertical Padding: 10dp
```

### Example Grid
```
┌──────────────────────────────────────────────────┐
│  [Card 1]  12dp  [Card 2]                        │
│                                                  │
│  12dp                                            │
│                                                  │
│  [Card 3]  12dp  [Card 4]                        │
│                                                  │
│  12dp                                            │
│                                                  │
│  [Card 5]  12dp  [Card 6]                        │
└──────────────────────────────────────────────────┘
```

---

## Interactive Elements

### Buyer Card
- **Wishlist Button**: 36dp circle, white background, shadow
  - Unfilled: Gray heart (#CCCCCC)
  - Filled: Pink heart (#E91E63)
  - Tap: Toggle wishlist status

- **Add to Cart Button**: Full width, 38dp height
  - Enabled: Pink background, white text
  - Disabled: Gray background, gray text
  - Tap: Add product to cart

- **Card Click**: Navigate to product details

### Seller Card
- **Menu Button**: 32dp circle, white background, shadow
  - Options: Edit, View as Buyer, Delete
  - Tap: Show dropdown menu

- **Toggle Switch**: Enable/disable product
  - On: Green (#4CAF50)
  - Off: Gray (#E0E0E0)
  - Tap: Toggle product status

- **Stock Counter**: +/- buttons
  - Minus: Decrease stock (min 0)
  - Plus: Increase stock
  - Display: Current stock number

---

## Responsive Behavior

### Phone (Small)
- 2 columns with 12dp spacing
- Cards fit comfortably
- Touch targets: 36-38dp minimum

### Tablet (Large)
- 2 columns (can be extended to 3-4 in future)
- Same spacing maintained
- Better use of screen real estate

---

## Professional Features

✅ **Consistent Spacing**: All gaps and padding follow 4dp grid
✅ **Visual Hierarchy**: Title > Price > Badges > Actions
✅ **Color Consistency**: Primary pink throughout
✅ **Touch Targets**: All buttons 32dp+ for easy interaction
✅ **Image Optimization**: Proper aspect ratios and loading
✅ **Text Overflow**: Proper ellipsis and line limits
✅ **Accessibility**: Sufficient color contrast
✅ **Performance**: Optimized image loading with Cloudinary

---

## Comparison with E-commerce Leaders

### Amazon Style Elements
✅ Clean card design
✅ Prominent product image
✅ Clear pricing
✅ Stock indicators
✅ Quick action buttons
✅ Seller information

### Etsy Style Elements
✅ Artisan focus (seller verification)
✅ Negotiable pricing option
✅ Wishlist/favorites
✅ Warm, inviting colors
✅ Professional spacing
✅ Clear typography

### Craftoria Unique
✅ Primary pink branding
✅ Seller verification badges
✅ Negotiation support
✅ Seller management tools
✅ Approval status tracking
