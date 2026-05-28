# Badge Consistency Quick Reference

## ✅ Status: All Badges Are Visually Consistent

All badges across payment screens, co-seller screens, and order screens use the unified badge system with consistent styling.

---

## Badge Components to Use

### For Order Statuses
```kotlin
import com.gcuf.craftoria.ui.components.OrderStatusBadge

OrderStatusBadge(status = orderStatus)
```
**Supported Statuses**: PENDING, NEW, PROCESSING, CONFIRMED, SHIPPED, DELIVERED, COMPLETED, CANCELLED

**Colors**:
- PENDING/NEW: Yellow (#FFF3CD) text (#856404)
- PROCESSING/CONFIRMED: Blue (#E3F2FD) text (#1976D2)
- SHIPPED: Purple (#F3E5F5) text (#7B1FA2)
- DELIVERED/COMPLETED: Green (#E8F5E8) text (#2E7D2E)
- CANCELLED: Red (#F8D7DA) text (#721C24)

---

### For Payment Statuses
```kotlin
import com.gcuf.craftoria.ui.components.PaymentStatusBadge

PaymentStatusBadge(status = payment.status)
```
**Supported Statuses**: COMPLETED, PENDING, PROCESSING, FAILED, REFUND_PENDING, REFUND_PROCESSING, REFUNDED, REFUND_REJECTED

**Colors**:
- COMPLETED: Green (#E8F5E8) text (Success)
- PENDING: Yellow (#FFF3CD) text (Warning)
- PROCESSING: Blue (#E3F2FD) text (#1976D2)
- FAILED: Red (#F8D7DA) text (Error)
- REFUNDED: Purple (#F3E5F5) text (#7B1FA2)
- REFUND_PENDING: Yellow (#FFF3CD) text (Warning)
- REFUND_PROCESSING: Blue (#E3F2FD) text (#1976D2)
- REFUND_REJECTED: Gray (#F5F5F5) text (#666666)

---

### For Generic States
```kotlin
import com.gcuf.craftoria.ui.components.StateBadge
import com.gcuf.craftoria.ui.components.BadgeState

StateBadge(label = "Active", state = BadgeState.SUCCESS)
```
**Available States**: DEFAULT, PRIMARY, SUCCESS, WARNING, ERROR, INFO

---

### For Member Roles
```kotlin
Surface(shape = RoundedCornerShape(4.dp), color = Primary.copy(alpha = 0.10f)) {
    Text(
        text = "Owner",
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = Primary,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
```

---

### For Invitation Status
```kotlin
val (bgColor, textColor) = when (invitation.status) {
    InvitationStatus.PENDING  -> Warning.copy(alpha = 0.12f) to Color(0xFF856404)
    InvitationStatus.ACCEPTED -> Success.copy(alpha = 0.10f) to Success
    InvitationStatus.DECLINED -> Error.copy(alpha = 0.10f) to Error
}

Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
    Text(
        text = invitation.status.toString(),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
```

---

## Design System Standards

| Property | Value |
|----------|-------|
| Height | 24dp |
| Padding | 6dp horizontal, 4dp vertical |
| Font Size | 10sp (9sp for role badges) |
| Font Weight | SemiBold (Bold for role badges) |
| Border Radius | 20dp (6dp for compact badges) |
| Line Height | 12sp |

---

## Color Palette

| Token | Color | Usage |
|-------|-------|-------|
| Primary | #E91E63 (Pink) | Primary actions, role badges |
| Success | #4CAF50 (Green) | Completed, accepted states |
| Warning | #FF9800 (Orange) | Pending, warning states |
| Error | #F44336 (Red) | Failed, error states |
| Info | #2196F3 (Blue) | Processing, info states |

---

## Screens Using Badges

### ✅ Seller Orders Screen
- **File**: `SellerOrdersScreen.kt`
- **Badge Type**: OrderStatusBadge
- **Location**: Line 500
- **Status**: COMPLIANT

### ✅ Seller Payments Screen
- **File**: `SellerPaymentsScreen.kt`
- **Badge Type**: PaymentStatusBadge
- **Location**: Line 257
- **Status**: COMPLIANT

### ✅ Buyer Payment History Screen
- **File**: `PaymentHistoryScreen.kt`
- **Badge Type**: PaymentStatusBadge
- **Location**: Line 277
- **Status**: COMPLIANT

### ✅ Co-Seller Store Payment Screen
- **File**: `CoSellerStorePaymentScreen.kt`
- **Badge Type**: PaymentStatusBadge
- **Location**: Line 452
- **Status**: COMPLIANT

### ✅ Manage Co-Seller Store Screen
- **File**: `ManageCoSellerStoreScreen.kt`
- **Badge Types**: Member role badge, Invitation status badge
- **Locations**: Lines 697-730
- **Status**: COMPLIANT

---

## Best Practices

### ✅ DO:
- Use `OrderStatusBadge` for order statuses
- Use `PaymentStatusBadge` for payment statuses
- Use theme tokens (Primary, Success, Warning, Error) instead of hardcoded hex values
- Keep badge text short and descriptive
- Use consistent padding and spacing
- Apply proper color semantics (green for success, red for error, etc.)

### ❌ DON'T:
- Create custom badge components
- Use hardcoded hex colors
- Mix badge styles across screens
- Use inconsistent font sizes
- Deviate from the design system standards
- Use badges for non-status information

---

## Refund Badge Handling

### Seller Orders Screen
When a refund is completed:
- **Suppress** the order status badge
- **Show** the "Refunded" badge with purple color and undo icon
- Implementation: Lines 495-502 in SellerOrdersScreen.kt

### Payment Screens
When a payment is refunded:
- **Show** the PaymentStatusBadge with "Refunded" status
- **Show** refund notice with undo icon and refund amount
- Implementation: Lines 257-259 in SellerPaymentsScreen.kt

---

## Component Location

**File**: `app/src/main/java/com/gcuf/craftoria/ui/components/UnifiedBadgeComponent.kt`

All badge components are defined in this single file for easy maintenance and consistency.

---

## Summary

✅ **All badges are visually consistent**
✅ **Using unified component system**
✅ **Theme tokens for colors**
✅ **Standard sizing and spacing**
✅ **Professional appearance**

**Status**: Production-ready. No changes required.
