# Filter Tabs & Status Badges - Quick Reference

## 🎯 Quick Start

### Using Filter Tabs
```kotlin
import com.gcuf.craftoria.ui.components.FilterTabRow

// In your screen
Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
    FilterTabRow(
        tabs = listOf("All", "Pending", "Completed"),
        selectedIndex = selectedIndex,
        onTabSelected = { index -> onFilterSelected(index) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    )
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}
```

### Using Status Badges
```kotlin
import com.gcuf.craftoria.ui.components.*

// Order Status
StatusBadge(status = OrderStatus.DELIVERED)

// Payment Status
PaymentStatusBadge(status = "completed")

// Refund Status
RefundStatusBadge(status = "approved")

// Generic State
StateBadge(label = "Active", state = BadgeState.SUCCESS)
```

---

## 📐 Specifications

### Filter Tabs
| Property | Value |
|----------|-------|
| Height | 40dp |
| Padding | 12dp H, 8dp V |
| Font | 12sp Medium |
| Radius | 8dp |
| Gap | 8dp |
| Active BG | Primary (#E91E63) |
| Active Text | White |
| Inactive BG | White |
| Inactive Text | TextSecondary (#757575) |
| Border | 0.8dp |

### Status Badges
| Property | Value |
|----------|-------|
| Height | 24dp |
| Padding | 8dp H, 5dp V |
| Font | 10sp SemiBold |
| Radius | 20dp |
| Max Lines | 1 |

---

## 🎨 Color Palette

### Order Status
- **PENDING/NEW**: Yellow bg (#FFF3CD), Dark text (#856404)
- **PROCESSING**: Blue bg (#D1ECF1), Dark text (#0C5460)
- **SHIPPED**: Purple bg (#E2D5F3), Dark text (#5A2D82)
- **DELIVERED**: Green bg (#D4EDDA), Dark text (#155724)
- **CANCELLED**: Red bg (#F8D7DA), Dark text (#721C24)

### Payment Status
- **COMPLETED**: Green bg (#D4EDDA), Dark text (#155724)
- **PENDING**: Yellow bg (#FFF3CD), Dark text (#856404)
- **PROCESSING**: Blue bg (#D1ECF1), Dark text (#0C5460)
- **FAILED**: Red bg (#F8D7DA), Dark text (#721C24)
- **REFUNDED**: Purple bg (#E2D5F3), Dark text (#5A2D82)

### Refund Status
- **PENDING**: Yellow bg (#FFF3CD), Dark text (#856404)
- **APPROVED**: Green bg (#D4EDDA), Dark text (#155724)
- **REJECTED**: Red bg (#F8D7DA), Dark text (#721C24)
- **COMPLETED**: Green bg (#D4EDDA), Dark text (#155724)

---

## ✅ Screens Using Standard Components

### Filter Tabs
- ✅ Buyer Home (CategoryTabs)
- ✅ My Orders (OrderFilterTabs)
- ✅ Notifications (NotificationCategoryFilterTabs)
- ✅ Buyer Payment History (PaymentStatusFilterTabs)
- ✅ Seller Payments (PaymentStatusFilterTabs)
- ✅ Seller Orders (SellerOrderFilterTabs)
- ✅ Co-Seller Store Payment (CoSellerPaymentFilterTabs)
- ✅ Seller Refund Management (FilterTabRow)
- ✅ Manage Products (FilterTabRow)

### Status Badges
- ✅ My Orders (OrderStatusBadge)
- ✅ Seller Orders (OrderStatusBadge)
- ✅ Buyer Payment History (PaymentStatusBadge)
- ✅ Seller Payments (PaymentStatusBadge)
- ✅ Co-Seller Store Payment (PaymentStatusBadge)
- ✅ Refund Screens (RefundStatusBadge)
- ✅ Manage Products (ProductActiveBadge)

---

## 🔧 Common Patterns

### Pattern 1: Simple Filter Tabs
```kotlin
Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
    FilterTabRow(
        tabs = listOf("All", "Active", "Inactive"),
        selectedIndex = selectedIndex,
        onTabSelected = { index -> onFilterSelected(index) },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    )
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}
```

### Pattern 2: Filter Tabs with Counts
```kotlin
Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
    FilterTabRow(
        tabs = listOf("All", "Pending", "Completed"),
        selectedIndex = selectedIndex,
        onTabSelected = { index -> onFilterSelected(index) },
        badgeCounts = listOf(totalCount, pendingCount, completedCount),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    )
    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
}
```

### Pattern 3: Pre-built Filter Tabs
```kotlin
PaymentStatusFilterTabs(
    selectedStatus = selectedStatus,
    onFilterSelected = { status -> onFilterSelected(status) }
)
```

### Pattern 4: Status Badge in List Item
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Order #12345")
    PaymentStatusBadge(status = "completed")
}
```

---

## 🚀 Implementation Checklist

When adding filter tabs to a new screen:
- [ ] Import FilterTabRow
- [ ] Create Column with white background
- [ ] Add FilterTabRow with tabs list
- [ ] Add HorizontalDivider below tabs
- [ ] Set contentPadding to 16dp H, 12dp V
- [ ] Add badge counts if needed
- [ ] Test on multiple screen sizes
- [ ] Verify colors match design system

When adding status badges to a new screen:
- [ ] Import appropriate badge component
- [ ] Use correct badge type (Status, Payment, Refund, etc.)
- [ ] Verify color palette matches
- [ ] Test badge sizing
- [ ] Verify text doesn't overflow
- [ ] Test on multiple screen sizes

---

## 🐛 Troubleshooting

### Issue: Filter tabs not scrolling
**Solution**: Ensure FilterTabRow is inside a scrollable container or use horizontal scroll

### Issue: Badge colors wrong
**Solution**: Check exact color values in UnifiedBadgeComponent.kt

### Issue: Filter tabs misaligned
**Solution**: Verify contentPadding is set correctly (16dp H, 12dp V)

### Issue: Badge text overflowing
**Solution**: Badges have maxLines = 1, text should be short

### Issue: Divider not showing
**Solution**: Ensure HorizontalDivider is added after FilterTabRow

---

## 📚 Component Reference

### FilterTabRow
```kotlin
@Composable
fun FilterTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    badgeCounts: List<Int>? = null
)
```

### StatusBadge
```kotlin
@Composable
fun StatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier
)
```

### PaymentStatusBadge
```kotlin
@Composable
fun PaymentStatusBadge(
    status: String,
    modifier: Modifier = Modifier
)
```

### RefundStatusBadge
```kotlin
@Composable
fun RefundStatusBadge(
    status: String,
    modifier: Modifier = Modifier
)
```

### StateBadge
```kotlin
@Composable
fun StateBadge(
    label: String,
    state: BadgeState = BadgeState.DEFAULT,
    modifier: Modifier = Modifier
)
```

---

## 🎓 Learning Resources

1. **Design System Guide**: `FILTER_TABS_AND_BADGES_CONSISTENCY_GUIDE.md`
2. **Implementation Details**: `FILTER_TABS_BADGES_CONSISTENCY_IMPLEMENTATION.md`
3. **Component Source**: `FilterTabComponent.kt`, `UnifiedBadgeComponent.kt`
4. **Example Screens**: 
   - `MyOrdersScreen.kt` (filter tabs + badges)
   - `PaymentHistoryScreen.kt` (payment filter + badges)
   - `NotificationsScreen.kt` (notification filter)

---

## 💡 Pro Tips

1. **Always wrap FilterTabRow in Column with white background**
2. **Always add HorizontalDivider after FilterTabRow**
3. **Use badgeCounts for real-time count updates**
4. **Pre-built filters (PaymentStatusFilterTabs, etc.) handle all styling**
5. **Badges automatically handle text overflow with ellipsis**
6. **Use StateBadge for generic states (Success, Warning, Error, Info)**
7. **Keep filter tab labels short (1-2 words)**
8. **Test badges on different text lengths**

---

## 📞 Support

For questions or issues:
1. Check this quick reference
2. Review the design system guide
3. Look at existing screen implementations
4. Check component source code
5. Contact the development team

---

**Last Updated**: May 27, 2026
**Version**: 1.0
**Status**: ✅ Production Ready

