# Unified Components - Quick Reference Guide

**Last Updated**: May 27, 2026  
**Status**: ✅ All screens compliant

---

## 🎯 Quick Start

### Using Filter Tabs

```kotlin
// Simple filter tabs
FilterTabRow(
    tabs = listOf("All", "Pending", "Completed"),
    selectedIndex = selectedIndex,
    onTabSelected = { index -> selectedIndex = index }
)

// With badge counts
FilterTabRow(
    tabs = listOf("All", "Pending", "Completed"),
    selectedIndex = selectedIndex,
    onTabSelected = { index -> selectedIndex = index },
    badgeCounts = listOf(10, 5, 3)
)

// With custom padding
FilterTabRow(
    tabs = listOf("All", "Pending", "Completed"),
    selectedIndex = selectedIndex,
    onTabSelected = { index -> selectedIndex = index },
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
)
```

### Using Specialized Filter Tabs

```kotlin
// Payment status filter
PaymentStatusFilterTabs(
    selectedStatus = selectedStatus,
    onFilterSelected = { status -> selectedStatus = status }
)

// Notification category filter
NotificationCategoryFilterTabs(
    currentFilter = currentFilter,
    onFilterSelected = { filter -> currentFilter = filter },
    userRole = "buyer" // or "seller"
)

// Co-seller payment filter
CoSellerPaymentFilterTabs(
    selectedStatus = selectedStatus,
    onFilterSelected = { status -> selectedStatus = status }
)
```

### Using Status Badges

```kotlin
// Order status badge
StatusBadge(status = OrderStatus.DELIVERED)

// Payment status badge
PaymentStatusBadge(status = "completed")

// Refund status badge
RefundStatusBadge(status = "approved")

// Product active/inactive badge
ProductActiveBadge(isActive = true)

// Generic state badge
StateBadge(label = "Active", state = BadgeState.SUCCESS)
```

---

## 📐 Design Specifications

### Filter Tabs
| Property | Value |
|----------|-------|
| Height | 40dp |
| Padding | 12dp horizontal, 8dp vertical |
| Font Size | 12sp |
| Font Weight | Medium |
| Border Radius | **20dp (pill shape)** |
| Gap | 8dp |
| Active Background | Primary (#E91E63) |
| Active Text | White |
| Inactive Background | White |
| Inactive Text | TextSecondary (#757575) |
| Border | 0.8dp BorderColor |

### Status Badges
| Property | Value |
|----------|-------|
| Height | 24dp |
| Padding | 8dp horizontal, 5dp vertical |
| Font Size | 10sp |
| Font Weight | SemiBold |
| Border Radius | **20dp (pill shape)** |
| Max Lines | 1 with ellipsis |

---

## 🎨 Badge Colors

### StatusBadge (Order Status)
- **Pending/New**: Yellow (#FFF3CD)
- **Processing/Confirmed**: Blue (#D1ECF1)
- **Shipped**: Purple (#E2D5F3)
- **Delivered/Completed**: Green (#D4EDDA)
- **Cancelled**: Red (#F8D7DA)

### PaymentStatusBadge
- **Completed**: Green (#D4EDDA)
- **Pending**: Yellow (#FFF3CD)
- **Processing**: Blue (#D1ECF1)
- **Failed**: Red (#F8D7DA)
- **Refund Pending**: Yellow (#FFF3CD)
- **Refund Processing**: Blue (#D1ECF1)
- **Refunded**: Purple (#E2D5F3)
- **Refund Rejected**: Gray (#E2E3E5)

### RefundStatusBadge
- **Pending**: Yellow (#FFF3CD)
- **Approved**: Green (#D4EDDA)
- **Rejected**: Red (#F8D7DA)
- **Completed**: Green (#D4EDDA)

### StateBadge States
- **SUCCESS**: Green (#D4EDDA)
- **WARNING**: Yellow (#FFF3CD)
- **ERROR**: Red (#F8D7DA)
- **INFO**: Blue (#D1ECF1)
- **DEFAULT**: Gray (#E2E3E5)
- **PRIMARY**: Pink (#E91E63 with 15% alpha)

---

## 📋 Screens Using Unified Components

### Filter Tabs (9 screens)
1. MyOrdersScreen - OrderFilterTabs
2. PaymentHistoryScreen - PaymentStatusFilterTabs
3. SellerOrdersScreen - SellerOrderFilterTabs
4. SellerPaymentsScreen - PaymentStatusFilterTabs
5. SellerRefundManagementScreen - FilterTabRow
6. ManageProductsScreen - FilterTabs
7. NotificationsScreen - NotificationCategoryFilterTabs
8. CoSellerStorePaymentScreen - CoSellerPaymentFilterTabs
9. RefundDetailsScreen - FilterTabRow

### Status Badges (12 screens)
1. MyOrdersScreen - StatusBadge
2. PaymentHistoryScreen - PaymentStatusBadge
3. SellerOrdersScreen - StatusBadge
4. SellerPaymentsScreen - PaymentStatusBadge
5. SellerRefundManagementScreen - RefundStatusBadge
6. SellerRefundDetailScreen - RefundStatusBadge
7. ManageProductsScreen - ProductActiveBadge
8. PaymentDetailScreen - PaymentStatusBadge
9. CoSellerStorePaymentScreen - PaymentStatusBadge
10. CoSellerOrderDetailScreen - StateBadge
11. BuyerRefundRequestScreen - RefundStatusBadge
12. RefundDetailsScreen - RefundStatusBadge

---

## ✅ Compliance Checklist

When adding new filter tabs or badges:

- [ ] Use `FilterTabRow` for filter tabs (not custom implementation)
- [ ] Use unified badge components (StatusBadge, PaymentStatusBadge, etc.)
- [ ] Verify border radius is 20dp (pill shape)
- [ ] Verify height is 40dp for tabs, 24dp for badges
- [ ] Verify font size is 12sp for tabs, 10sp for badges
- [ ] Verify colors match the design palette
- [ ] Test on multiple device sizes
- [ ] Test on multiple screen orientations
- [ ] Verify no compilation errors

---

## 🔧 Common Patterns

### Filter Tabs with White Background
```kotlin
Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
    FilterTabRow(
        tabs = tabs,
        selectedIndex = selectedIndex,
        onTabSelected = onTabSelected,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    )
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}
```

### Badge in Row
```kotlin
Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Text("Order Status")
    StatusBadge(status = order.status)
}
```

### Multiple Badges
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp)
) {
    StatusBadge(status = order.status)
    PaymentStatusBadge(status = payment.status)
    RefundStatusBadge(status = refund.status)
}
```

---

## 📚 Component Files

- **FilterTabComponent.kt**: All filter tab components
- **UnifiedBadgeComponent.kt**: All badge components

---

## 🚀 Best Practices

1. **Always use unified components** - Don't create custom implementations
2. **Maintain consistency** - Use the same component across similar screens
3. **Follow the design system** - Respect the 20dp border radius and color palette
4. **Test thoroughly** - Verify on multiple devices and orientations
5. **Document changes** - Update this guide if adding new components

---

## ❓ FAQ

**Q: Can I customize the border radius?**  
A: No. The 20dp pill shape is part of the design system and must be consistent across all screens.

**Q: Can I use different colors?**  
A: No. Use the predefined color palette defined in the badge components.

**Q: Can I change the font size?**  
A: No. The 12sp for tabs and 10sp for badges are standardized.

**Q: What if I need a custom badge?**  
A: Use `StateBadge` with the appropriate `BadgeState` enum value.

**Q: Can I add more filter tabs?**  
A: Yes, just add more strings to the `tabs` list in `FilterTabRow`.

---

## 📞 Support

For questions or issues with unified components:
1. Check this quick reference guide
2. Review the component implementation files
3. Look at existing screen implementations for examples
4. Refer to the comprehensive implementation document

---
