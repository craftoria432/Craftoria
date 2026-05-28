# Redundant Delivery Status Fix

## Problem
When an order is shipped by the seller, both the "Track Order" dialog and "Order Details" dialog show redundant delivery statuses that are all marked as "Pending":

- Order Confirmed ✓
- Picked Up by Courier (Pending)
- In Transit (Pending)
- Out for Delivery (Pending)

This creates visual clutter and confusion because these future statuses haven't happened yet and may never happen in that exact sequence.

## Root Cause
In `OrderRepository.kt`, the `markAsShipped()` function creates a timeline with 4 entries when the order is shipped:

```kotlin
val timeline = listOf(
    mapOf("title" to "Order Confirmed", "is_completed" to true),
    mapOf("title" to "Picked Up by Courier", "is_completed" to false),  // ❌ Redundant
    mapOf("title" to "In Transit", "is_completed" to false),            // ❌ Redundant
    mapOf("title" to "Out for Delivery", "is_completed" to false)       // ❌ Redundant
)
```

All three future statuses are shown as "Pending" immediately, even though they haven't occurred yet.

## Solution
Simplified the timeline to only show completed statuses. When an order is shipped, only show:

```kotlin
val timeline = listOf(
    mapOf("title" to "Order Confirmed", "is_completed" to true),
    mapOf("title" to "Shipped", "is_completed" to true)
)
```

Future delivery statuses (Picked Up, In Transit, Out for Delivery) should only be added when they actually occur, not preemptively.

## User Experience

### Before Fix
```
Track Order Dialog:
  ✓ Order Confirmed (May 06, 09:49 AM)
  ○ Picked Up by Courier (Pending)
  ○ In Transit (Pending)
  ○ Out for Delivery (Pending)
```

### After Fix
```
Track Order Dialog:
  ✓ Order Confirmed (May 06, 09:49 AM)
  ✓ Shipped (May 06, 10:30 AM)
```

## Benefits
1. **Cleaner UI**: No redundant "Pending" statuses cluttering the timeline
2. **Accurate Information**: Only shows what has actually happened
3. **Better UX**: Users see real progress, not placeholder statuses
4. **Flexibility**: Sellers can add specific delivery updates as they occur

## Implementation Details

### Files Changed
- ✅ `app/src/main/java/com/gcuf/craftoria/data/repository/OrderRepository.kt`

### What Changed
- Removed 3 redundant pending timeline entries from `markAsShipped()`
- Timeline now only includes "Order Confirmed" and "Shipped" statuses
- Future delivery updates can be added through separate functions as needed

## Future Enhancements (Optional)
If you want to track detailed delivery progress, you can add functions like:
- `markAsPickedUp()` - Adds "Picked Up by Courier" to timeline
- `markAsInTransit()` - Adds "In Transit" to timeline
- `markAsOutForDelivery()` - Adds "Out for Delivery" to timeline

These would only add statuses when they actually occur, not preemptively.

## Testing
1. Create a new order
2. Mark it as shipped from seller dashboard
3. Open "Track Order" from buyer's My Orders
4. ✅ Should only show "Order Confirmed" and "Shipped" (no pending statuses)
5. Open "Order Details" dialog
6. ✅ Timeline should match Track Order (no redundant entries)

---

## Summary
Removed redundant "Pending" delivery statuses from the order timeline. When an order is shipped, only "Order Confirmed" and "Shipped" statuses are shown, eliminating visual clutter and providing accurate tracking information.
