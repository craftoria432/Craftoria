# StatusBadge Parameter Fix - Seller Order Dialogs

## Problem
Compilation error at line 122 in `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/OrderDialogs.kt`:
- Error: "Cannot find a parameter with this name: status"
- Error: "No value passed for parameter 'isActive'"

The code was calling:
```kotlin
StatusBadge(status = order.status)
```

But `StatusBadge` only accepts a boolean `isActive` parameter and displays either "Active" or "Inactive" status.

## Root Cause Analysis
The `StatusBadge` composable is designed for simple product status (active/inactive) with only two states. It doesn't support showing order statuses like "pending", "processing", "shipped", "delivered", etc.

## Solution Implemented

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/OrderDialogs.kt`

**Changes Made:**

### 1. Added Missing Imports
```kotlin
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.ui.screens.buyer.OrderStatusBadge
```

### 2. Replaced StatusBadge with OrderStatusBadge
**Before:**
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 5.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(text = "Status", fontSize = 12.sp, color = TextSecondary)
    StatusBadge(status = order.status)  // ❌ WRONG
}
```

**After:**
```kotlin
// ✅ Convert status string to OrderStatus enum safely (outside composable)
val orderStatus = try {
    OrderStatus.valueOf(order.status.uppercase())
} catch (e: Exception) {
    OrderStatus.PENDING // Fallback if status is not valid
}
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 5.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(text = "Status", fontSize = 12.sp, color = TextSecondary)
    OrderStatusBadge(status = orderStatus)  // ✅ CORRECT
}
```

## Key Design Decisions

1. **Moved enum conversion outside composable** - Converts `order.status` (String) to `OrderStatus` enum before passing to composable. Try-catch is used outside the composable call.

2. **Safe fallback to PENDING** - If the status string doesn't match any OrderStatus enum value, defaults to PENDING instead of crashing.

3. **Used OrderStatusBadge** - The proper component that handles all order statuses with color-coded badges:
   - PENDING → Yellow
   - PROCESSING → Blue
   - SHIPPED → Purple
   - DELIVERED → Green
   - CANCELLED → Red
   - etc.

## Verification
✅ No compilation errors
✅ Imports properly added
✅ Enum conversion pattern matches existing codebase (see SellerOrdersScreen.kt line 535)

## Pattern Consistency
This fix follows the same pattern already used in `SellerOrdersScreen.kt` at line 535-536:
```kotlin
val orderStatus = try {
    OrderStatus.valueOf(order.status.uppercase())
} catch (e: Exception) {
    OrderStatus.PENDING
}
OrderStatusBadge(status = orderStatus)
```

## Status
**COMPLETE AND VERIFIED** - Ready for deployment
