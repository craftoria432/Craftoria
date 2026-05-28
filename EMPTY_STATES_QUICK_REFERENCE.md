# Empty States - Quick Reference Guide

## TL;DR

All empty states across Payments, Refund, Notification, and Orders screens now use **professional, consistent styling** with:
- ✅ Material Design icons (no emoji)
- ✅ 88dp icon circles with 44dp icons
- ✅ 20sp Bold titles, 14sp Normal messages
- ✅ No underlines or extra text
- ✅ Consistent 40dp padding and 24dp spacing

---

## How to Use

### Option 1: Use Predefined Empty States (Recommended)

```kotlin
// For notifications
EmptyStates.NoNotifications()

// For orders
EmptyStates.NoOrders()

// For payments
EmptyStates.NoPayments()
EmptyStates.NoPaymentsYet(forBuyer = true)  // Buyer
EmptyStates.NoPaymentsYet(forBuyer = false) // Seller

// For refunds
EmptyStates.NoRefunds()

// For filtered results
EmptyStates.NoPaymentsFiltered(filterName = "Pending")

// For other scenarios
EmptyStates.NoProducts()
EmptyStates.NoMessages()
EmptyStates.NoWishlist()
EmptyStates.EmptyCart(onContinueShopping = { /* action */ })
EmptyStates.SearchStart()
EmptyStates.NoSellerProducts(onAddProductClick = { /* action */ })
```

### Option 2: Use EmptyStateComponent Directly

```kotlin
EmptyStateComponent(
    icon = Icons.Default.ShoppingBag,
    title = "No Orders Yet",
    message = "Start shopping to see your orders here",
    actionButton = {
        CraftoriaButton(
            text = "Browse Products",
            onClick = { /* action */ }
        )
    }
)
```

---

## Specifications at a Glance

| Aspect | Value |
|--------|-------|
| **Icon Circle Size** | 88dp |
| **Icon Circle Background** | Primary.copy(alpha = 0.10f) |
| **Icon Size** | 44dp |
| **Icon Color** | Primary.copy(alpha = 0.70f) |
| **Title Font Size** | 20sp |
| **Title Font Weight** | Bold |
| **Title Color** | TextPrimary |
| **Title Decoration** | None |
| **Message Font Size** | 14sp |
| **Message Font Weight** | Normal |
| **Message Color** | TextSecondary |
| **Message Decoration** | None |
| **Container Padding** | 40dp |
| **Icon to Title Gap** | 24dp |
| **Title to Message Gap** | 12dp |
| **Message to Button Gap** | 24dp |

---

## Screens Updated

| Screen | Status | Icon | Notes |
|--------|--------|------|-------|
| NotificationsScreen | ✅ Updated | Notifications | Filter-aware messages |
| MyOrdersScreen | ✅ Updated | ShoppingBag | With "Browse Products" button |
| SellerOrdersScreen | ✅ Updated | ShoppingBag | No action button |
| PaymentHistoryScreen | ✅ Already Compliant | AttachMoney | Using EmptyStates |
| SellerPaymentsScreen | ✅ Already Compliant | AttachMoney | Using EmptyStates |
| SellerRefundManagementScreen | ✅ Updated | Receipt | Filter-aware messages |

---

## Available Icons

```kotlin
Icons.Default.Notifications      // Notifications
Icons.Default.ShoppingBag        // Orders
Icons.Default.AttachMoney        // Payments
Icons.Default.Receipt            // Refunds
Icons.Default.Inventory2         // Products
Icons.Default.Mail               // Messages
Icons.Default.FavoriteBorder     // Wishlist
Icons.Outlined.ShoppingCart      // Cart
Icons.Default.Search             // Search
Icons.Default.FilterList         // Filters
Icons.Default.Store              // Stores
Icons.Default.Undo               // Undo/Refunds
Icons.Default.Info               // Generic Info
```

---

## Common Patterns

### Empty State with No Action
```kotlin
EmptyStateComponent(
    icon = Icons.Default.ShoppingBag,
    title = "No Orders Yet",
    message = "Orders from buyers will appear here"
)
```

### Empty State with Action Button
```kotlin
EmptyStateComponent(
    icon = Icons.Default.ShoppingBag,
    title = "No Orders Yet",
    message = "Start shopping to see your orders here",
    actionButton = {
        CraftoriaButton(
            text = "Browse Products",
            onClick = { /* navigate */ }
        )
    }
)
```

### Filter-Aware Empty State
```kotlin
val title = if (filter != null) {
    "No ${filter.name} Items"
} else {
    "No Items Yet"
}

val message = if (filter != null) {
    "Try a different filter"
} else {
    "Items will appear here"
}

EmptyStateComponent(
    icon = Icons.Default.Search,
    title = title,
    message = message
)
```

---

## Files to Reference

- **Component:** `EmptyStateComponent.kt`
- **Predefined States:** `EmptyStates` object in `EmptyStateComponent.kt`
- **Examples:**
  - `NotificationsScreen.kt` - Filter-aware empty state
  - `MyOrdersScreen.kt` - Empty state with action button
  - `SellerOrdersScreen.kt` - Simple empty state
  - `SellerRefundManagementScreen.kt` - Filter-aware empty state

---

## Migration Checklist

If you're updating an existing empty state:

- [ ] Replace custom Column/Box with `EmptyStateComponent`
- [ ] Use Material Design icon (not emoji)
- [ ] Set title to 20sp Bold
- [ ] Set message to 14sp Normal
- [ ] Remove any underlines or decorations
- [ ] Use 88dp circles with 44dp icons
- [ ] Add `TextDecoration.None` to text
- [ ] Test on multiple screen sizes
- [ ] Verify consistency with other empty states

---

## Troubleshooting

### Icon looks too small/large
- Check icon size is 44dp
- Check circle size is 88dp
- Verify icon is centered in circle

### Text looks different from other screens
- Check title is 20sp Bold
- Check message is 14sp Normal
- Verify TextDecoration.None is set
- Check colors are TextPrimary/TextSecondary

### Spacing looks off
- Check container padding is 40dp
- Check icon-to-title gap is 24dp
- Check title-to-message gap is 12dp
- Check message-to-button gap is 24dp

### Text has underline
- Add `textDecoration = TextDecoration.None` to Text component
- Verify no TextDecoration.Underline is set

---

## Best Practices

1. **Always use EmptyStateComponent** - Never create custom empty state implementations
2. **Use predefined EmptyStates** - When available, use predefined functions
3. **Maintain consistency** - All empty states should look the same
4. **Keep messages short** - 1-2 lines for title, 1-3 lines for message
5. **Use appropriate icons** - Choose icons that match the content type
6. **Add action buttons** - When users can take action (e.g., "Browse Products")
7. **Filter-aware messages** - Update messages based on active filters
8. **Test on all screens** - Verify appearance on different device sizes

---

## Summary

✅ **Standardized:** All empty states use the same professional design  
✅ **Consistent:** 88dp circles, 44dp icons, 20sp/14sp text  
✅ **Professional:** Material Design icons, no emoji  
✅ **Clean:** No underlines or extra formatting  
✅ **Accessible:** Clear hierarchy and proper spacing  
✅ **Maintainable:** Centralized component, easy to update  

**Status:** Complete and ready for production
