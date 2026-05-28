# Request Refund Button Flash Fix

## Problem
The "Request Refund" button briefly shows a loading spinner before appearing on completed/delivered orders. This creates a jarring visual flash when the order card first renders.

## Root Cause
In `MyOrdersScreen.kt`, the `LaunchedEffect` that checks for existing refunds sets `isCheckingRefund = true` immediately on initial render:

```kotlin
LaunchedEffect(order.id, currentUserId) {
    if (currentUserId.isNotEmpty() && status in listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)) {
        isCheckingRefund = true  // ❌ This causes the flash
        try {
            // Check for refund...
        } finally {
            isCheckingRefund = false
        }
    }
}
```

When `isCheckingRefund` is `true`, the button shows a `CircularProgressIndicator` instead of the "Request Refund" text. This happens on every initial render, causing the flash.

## Solution
Added a `hasInitiallyChecked` flag that prevents showing the loading state on first render:

```kotlin
var hasInitiallyChecked by remember { mutableStateOf(false) }

LaunchedEffect(order.id, currentUserId) {
    if (currentUserId.isNotEmpty() && status in listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)) {
        // ✅ Only show loading spinner after initial check
        if (hasInitiallyChecked) {
            isCheckingRefund = true
        }
        try {
            // Check for refund...
        } finally {
            isCheckingRefund = false
            hasInitiallyChecked = true  // ✅ Mark as checked
        }
    }
}
```

## How It Works

### First Render (Initial Load)
1. `hasInitiallyChecked = false`
2. `LaunchedEffect` runs
3. Skips setting `isCheckingRefund = true`
4. Fetches refund data silently
5. Sets `hasInitiallyChecked = true`
6. Button appears immediately without flash

### Subsequent Updates (If Any)
1. `hasInitiallyChecked = true`
2. `LaunchedEffect` runs again (e.g., on refresh)
3. Sets `isCheckingRefund = true` (shows spinner)
4. Fetches refund data
5. Sets `isCheckingRefund = false`
6. Button updates smoothly

## User Experience

### Before Fix
```
[Order Card Loads]
  ↓
[Loading Spinner Shows] ← Flash!
  ↓ (100-300ms)
[Request Refund Button Appears]
```

### After Fix
```
[Order Card Loads]
  ↓
[Request Refund Button Appears Immediately] ← No flash!
```

## Files Changed
- ✅ `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt`

## Testing
1. Open My Orders screen
2. Navigate to a completed/delivered order
3. ✅ "Request Refund" button should appear immediately without any loading flash
4. Pull to refresh (if implemented)
5. ✅ Loading spinner should show during refresh (expected behavior)

## Technical Notes
- The fix uses Compose's `remember` to track whether the initial check has completed
- Loading state is only shown on subsequent checks (e.g., manual refresh)
- This pattern is common in Compose for preventing initial loading flashes
- No impact on functionality — refund check still happens, just silently on first load

---

## Summary
Changed the refund check logic to skip showing the loading spinner on initial render, eliminating the visual flash while maintaining all functionality.
