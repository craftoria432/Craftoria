# Filter Tabs & Status Badges Consistency Guide

## Overview
This document ensures all filter tabs and status badges across Craftoria follow the same design system standards established in the Buyer Home screen and My Orders screen.

---

## 1. FILTER TABS DESIGN SYSTEM

### Standard Specifications
- **Height**: 40dp
- **Padding**: 12dp horizontal, 8dp vertical
- **Font Size**: 12sp
- **Font Weight**: Medium
- **Border Radius**: 8dp
- **Gap Between Tabs**: 8dp
- **Animation**: Smooth color transition on selection

### Active State
- **Background**: Primary color (#E91E63)
- **Text Color**: White
- **Border**: Primary color

### Inactive State
- **Background**: White
- **Text Color**: TextSecondary (#757575)
- **Border**: BorderColor (#E0E0E0)

### Container
- **Background**: White
- **Bottom Divider**: 0.5dp, BorderColor (#E0E0E0)
- **Padding**: 16dp horizontal, 12dp vertical
- **Horizontal Scroll**: Enabled for overflow

### Badge Count (Optional)
- **Shape**: Circle
- **Size**: 20dp
- **Font Size**: 8sp
- **Font Weight**: Bold
- **Active Badge Background**: White with 30% alpha
- **Inactive Badge Background**: Primary with 12% alpha

---

## 2. STATUS BADGES DESIGN SYSTEM

### Standard Specifications
- **Height**: 24dp (auto-calculated from padding)
- **Padding**: 8dp horizontal, 5dp vertical
- **Font Size**: 10sp
- **Font Weight**: SemiBold
- **Border Radius**: 20dp (pill shape)
- **Max Lines**: 1 with ellipsis overflow

### Color Palette

#### Order Status Badges
| Status | Background | Text Color |
|--------|-----------|-----------|
| PENDING / NEW | #FFF3CD | #856404 |
| PROCESSING / CONFIRMED | #D1ECF1 | #0C5460 |
| SHIPPED | #E2D5F3 | #5A2D82 |
| DELIVERED / COMPLETED | #D4EDDA | #155724 |
| CANCELLED | #F8D7DA | #721C24 |

#### Payment Status Badges
| Status | Background | Text Color |
|--------|-----------|-----------|
| COMPLETED | #D4EDDA | #155724 |
| PENDING | #FFF3CD | #856404 |
| PROCESSING | #D1ECF1 | #0C5460 |
| FAILED | #F8D7DA | #721C24 |
| REFUND_PENDING | #FFF3CD | #856404 |
| REFUND_PROCESSING | #D1ECF1 | #0C5460 |
| REFUNDED | #E2D5F3 | #5A2D82 |
| REFUND_REJECTED | #E2E3E5 | #383D41 |

#### Refund Status Badges
| Status | Background | Text Color |
|--------|-----------|-----------|
| PENDING | #FFF3CD | #856404 |
| APPROVED | #D4EDDA | #155724 |
| REJECTED | #F8D7DA | #721C24 |
| COMPLETED | #D4EDDA | #155724 |

---

## 3. SCREENS REQUIRING CONSISTENCY

### ✅ Already Compliant
- **Buyer Home Screen**: Uses CategoryTabs (similar structure)
- **My Orders Screen**: Uses OrderFilterTabs with FilterTabRow
- **Notifications Screen**: Uses NotificationCategoryFilterTabs
- **Buyer Payment History Screen**: Uses PaymentStatusFilterTabs
- **Seller Payments Screen**: Uses PaymentStatusFilterTabs
- **Seller Orders Screen**: Uses SellerOrderFilterTabs
- **Co-Seller Store Payment Screen**: Uses CoSellerPaymentFilterTabs

### 🔄 Needs Verification/Updates

#### Payment-Related Screens
1. **Seller Payment Detail Screen** (`SellerPaymentDetailScreen.kt`)
   - Verify PaymentStatusBadge sizing and styling
   - Ensure consistent filter tabs if present

2. **Buyer Payment History Screen** (`PaymentHistoryScreen.kt`)
   - ✅ Already uses PaymentStatusFilterTabs
   - Verify PaymentStatusBadge consistency

#### Refund Screens
1. **Buyer Refund Request Screen** (`BuyerRefundRequestScreen.kt`)
   - Add RefundStatusBadge if displaying refund status
   - Ensure consistent styling

2. **Seller Refund Management Screen** (`SellerRefundManagementScreen.kt`)
   - Verify RefundStatusBadge sizing
   - Ensure filter tabs follow standard (currently uses custom RefundFilter enum)
   - **ACTION**: Update to use FilterTabRow with standard styling

3. **Seller Refund Detail Screen** (`SellerRefundDetailScreen.kt`)
   - Verify RefundStatusBadge consistency
   - Check timeline display styling

4. **Buyer Refund Details Screen** (`RefundDetailsScreen.kt`)
   - Verify RefundStatusBadge sizing
   - Check "Overview", "Timeline", "Breakdown" tabs styling

#### Product Management Screens
1. **Manage Products Screen** (`ManageProductsScreen.kt`)
   - ✅ Uses FilterTabs (custom implementation)
   - **ACTION**: Verify uses FilterTabRow with standard styling
   - Verify ProductActiveBadge sizing

2. **Add Product Screen** (`AddProductScreen.kt`)
   - Check if any badges are displayed
   - Verify consistency

#### Co-Seller Screens
1. **Manage Co-Seller Store Screen** (`ManageCoSellerStoreScreen.kt`)
   - Verify any badges displayed
   - Check filter tabs if present

2. **Co-Seller Order Detail Screen** (`CoSellerOrderDetailScreen.kt`)
   - Verify SplitStatusBadge sizing (currently custom)
   - **ACTION**: Consider using unified badge system

#### Notification Screen
1. **Notifications Screen** (`NotificationsScreen.kt`)
   - ✅ Uses NotificationCategoryFilterTabs
   - Verify badge consistency

---

## 4. IMPLEMENTATION CHECKLIST

### Phase 1: Audit & Documentation
- [x] Document standard specifications
- [x] Identify all screens using filters/badges
- [x] Create color palette reference

### Phase 2: Standardize Filter Tabs
- [ ] Update Seller Refund Management Screen to use FilterTabRow
- [ ] Update Manage Products Screen to use FilterTabRow
- [ ] Verify all filter tabs use consistent padding/spacing
- [ ] Ensure all filter tabs have white background with 0.5dp divider

### Phase 3: Standardize Status Badges
- [ ] Verify all StatusBadge instances use correct sizing (24dp height)
- [ ] Verify all PaymentStatusBadge instances use correct sizing
- [ ] Verify all RefundStatusBadge instances use correct sizing
- [ ] Update SplitStatusBadge to use unified badge system if possible
- [ ] Ensure all badges use 10sp font size

### Phase 4: Verification
- [ ] Test all screens visually
- [ ] Verify alignment across screens
- [ ] Check responsive behavior on different screen sizes
- [ ] Verify animations are smooth

---

## 5. CODE EXAMPLES

### Using FilterTabRow (Standard)
```kotlin
FilterTabRow(
    tabs = listOf("All", "Pending", "Completed"),
    selectedIndex = selectedIndex,
    onTabSelected = { index -> onFilterSelected(index) },
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
)
```

### Using PaymentStatusFilterTabs (Pre-built)
```kotlin
PaymentStatusFilterTabs(
    selectedStatus = selectedStatus,
    onFilterSelected = { status -> onFilterSelected(status) }
)
```

### Using StatusBadge
```kotlin
StatusBadge(status = OrderStatus.DELIVERED)
```

### Using PaymentStatusBadge
```kotlin
PaymentStatusBadge(status = "completed")
```

### Using RefundStatusBadge
```kotlin
RefundStatusBadge(status = "approved")
```

---

## 6. SCREENS BREAKDOWN

### Buyer Screens
| Screen | Filter Tabs | Status Badges | Status |
|--------|------------|---------------|--------|
| Home | CategoryTabs | None | ✅ |
| My Orders | OrderFilterTabs | OrderStatusBadge | ✅ |
| Payment History | PaymentStatusFilterTabs | PaymentStatusBadge | ✅ |
| Refund Request | None | RefundStatusBadge | 🔄 |
| Refund Details | Custom tabs | RefundStatusBadge | 🔄 |
| Wishlist | None | None | ✅ |
| Search | None | None | ✅ |

### Seller Screens
| Screen | Filter Tabs | Status Badges | Status |
|--------|------------|---------------|--------|
| Dashboard | None | None | ✅ |
| Orders | SellerOrderFilterTabs | OrderStatusBadge | ✅ |
| Payments | PaymentStatusFilterTabs | PaymentStatusBadge | ✅ |
| Payment Detail | None | PaymentStatusBadge | 🔄 |
| Refund Management | Custom RefundFilter | RefundStatusBadge | 🔄 |
| Refund Detail | None | RefundStatusBadge | 🔄 |
| Products | FilterTabs | ProductActiveBadge | 🔄 |
| Add Product | None | None | ✅ |

### Co-Seller Screens
| Screen | Filter Tabs | Status Badges | Status |
|--------|------------|---------------|--------|
| Store Payment | CoSellerPaymentFilterTabs | PaymentStatusBadge | ✅ |
| Order Detail | None | SplitStatusBadge | 🔄 |
| Store Directory | None | None | ✅ |

### Notification Screens
| Screen | Filter Tabs | Status Badges | Status |
|--------|------------|---------------|--------|
| Notifications | NotificationCategoryFilterTabs | None | ✅ |

---

## 7. NEXT STEPS

1. **Review** this document with the team
2. **Prioritize** screens marked as 🔄
3. **Update** each screen to use standard components
4. **Test** visually on multiple devices
5. **Document** any deviations with justification

---

## 8. COMPONENT REFERENCES

### FilterTabComponent.kt
- `FilterTab()` - Single tab
- `FilterTabRow()` - Horizontal scrollable tabs
- `FilterTabColumn()` - Vertical tabs
- `PaymentStatusFilterTabs()` - Pre-built payment filter
- `NotificationCategoryFilterTabs()` - Pre-built notification filter
- `CoSellerPaymentFilterTabs()` - Pre-built co-seller filter

### UnifiedBadgeComponent.kt
- `StatusBadge()` - Order status
- `PaymentStatusBadge()` - Payment status
- `RefundStatusBadge()` - Refund status
- `StateBadge()` - Generic state badge
- `CountBadge()` - Count display
- `ProductActiveBadge()` - Product active/inactive
- `StockBadge()` - Stock status
- `NegotiableBadge()` - Negotiable indicator
- `VerificationBadge()` - Verification status

---

## 9. COMMON ISSUES & SOLUTIONS

### Issue: Badge sizing inconsistent
**Solution**: Ensure all badges use padding(horizontal = 8.dp, vertical = 5.dp) and fontSize = 10.sp

### Issue: Filter tabs not scrolling
**Solution**: Wrap FilterTabRow in horizontalScroll(rememberScrollState())

### Issue: Filter tabs not visible during loading
**Solution**: Place filter tabs outside of loading state check

### Issue: Badge colors not matching
**Solution**: Use exact color values from UnifiedBadgeComponent.kt

---

## 10. MAINTENANCE

- Review this document quarterly
- Update when new screens are added
- Maintain color palette consistency
- Document any approved deviations

