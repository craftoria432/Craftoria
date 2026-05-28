# Refund Button Loading Flicker Fix

## Problem
The refund buttons in MyOrdersScreen were showing a loading/skeleton placeholder for a few milliseconds before displaying the actual button state (Refund Pending, Refund Approved, Resubmit Refund Request). This caused a visible flicker/flash effect.

## Root Cause
The issue was in the `OrderActionButtons` composable:

1. When an order card loaded, `refundStateLoaded` was `false`
2. This set `isLoading = true`, which showed a placeholder skeleton button
3. A `LaunchedEffect` would query Firestore for the refund status
4. Once the query completed, `refundStateLoaded` became `true`, setting `isLoading = false`
5. The UI switched from the placeholder to the actual button state
6. This transition caused the visible flicker

## Solution
**Remove the placeholder button logic entirely.** Instead:

1. The `refundState` starts as `CHECKING` or `NONE` immediately
2. Render the actual button state directly without a placeholder
3. When the Firestore query completes, the state updates and the button content changes smoothly
4. The `CHECKING` state shows "View Details" as a neutral option while the query runs

### Changes Made

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt`

1. **Removed `isLoading` parameter** from `OrderActionButtons` function signature
2. **Removed placeholder button logic** - the `if (isLoading)` block that showed the skeleton
3. **Added `CHECKING` state handler** - shows "View Details" while refund status is being queried
4. **Simplified button rendering** - now directly renders the appropriate button based on `refundState`

### Before
```kotlin
if (isLoading) {
    // Show placeholder skeleton button
    OutlinedButton(...) { /* skeleton */ }
} else {
    when (refundState) {
        // Show actual button
    }
}
```

### After
```kotlin
when (refundState) {
    OrderRefundState.CHECKING -> {
        // Show "View Details" while checking
        OutlinedButton(...) { Text("View Details") }
    }
    OrderRefundState.REQUESTED -> {
        // Show "Refund Pending"
        OutlinedButton(...) { Text("Refund Pending") }
    }
    // ... other states
}
```

## Benefits
- ✅ No more loading flicker on refund buttons
- ✅ Buttons appear immediately with appropriate state
- ✅ Smooth state transitions as Firestore query completes
- ✅ Better UX - no jarring placeholder-to-content switch
- ✅ Cleaner code - removed unnecessary loading state logic

## Button States
The refund button now correctly shows:
- **CHECKING** → "View Details" (while querying Firestore)
- **REQUESTED** → "Refund Pending" (orange, disabled)
- **APPROVED** → "Refund Approved" (blue, disabled)
- **PROCESSING** → "Processing" (blue, disabled)
- **COMPLETED** → "Refund Done" (green, disabled)
- **REJECTED** → "Resubmit" (orange, clickable)
- **FINAL_DECISION** → "Refund Denied" (gray, disabled)
- **FAILED** → "Refund Failed" (red, disabled)
- **NONE** → "Request Refund" or "View Details" (depending on 30-day window)

## Testing
Test by:
1. Navigate to MyOrdersScreen as a buyer
2. View delivered/completed orders
3. Observe that refund buttons appear immediately without flicker
4. Verify button text matches the actual refund status
