# Order Tracking & Badge Animation - Quick Test Guide

## Testing the Autoscroll Feature

### Step 1: Open Order Tracking Dialog
1. Navigate to "My Orders" screen (Buyer)
2. Click on any order
3. Click "Track Order" button
4. Dialog opens

### Step 2: Verify Autoscroll
- **Expected**: Dialog automatically scrolls to first incomplete/pending status
- **Timing**: Scroll happens ~300ms after dialog opens
- **Animation**: Smooth scroll animation (not instant)
- **Result**: First pending item should be visible in center of dialog

### Step 3: Test Hover Effects
1. Move mouse/touch over timeline items
2. **Expected Hover Effects**:
   - Background tints with primary color (subtle)
   - Item scales up slightly (1.02x)
   - Smooth 200ms transition
3. Move away from item
4. **Expected**: Effects fade smoothly back to normal

### Step 4: Manual Scroll
1. After autoscroll completes, manually scroll
2. **Expected**: Smooth scrolling works normally
3. Scroll to top and bottom
4. **Expected**: All timeline items visible and readable

---

## Testing the Seller Orders Badge

### Step 1: Setup
1. Login as Seller
2. Go to Seller Dashboard
3. Look at bottom navigation "Orders" icon
4. **Expected**: No badge initially (if no pending orders)

### Step 2: Create New Order (as Buyer)
1. Open another device/browser as Buyer
2. Place an order from this seller
3. **Expected**: Badge appears on seller's Orders icon with count "1"

### Step 3: Verify Badge Animation
1. Watch the badge on Orders icon
2. **Expected Pulsing Animation**:
   - Badge scales: 1.0 → 1.15 → 1.0 (repeats)
   - Badge fades: 1.0 → 0.7 → 1.0 (repeats)
   - Duration: ~1.2 seconds per cycle
   - Smooth, continuous animation

### Step 4: Multiple Orders
1. Create 2-3 more orders from buyer
2. **Expected**: Badge count updates to "2", "3", etc.
3. **Expected**: Animation continues smoothly
4. **Expected**: Badge color remains Deep Orange (0xFFFF5722)

### Step 5: View Order
1. Click on Orders in bottom navigation
2. View/click on an order
3. Go back to dashboard
4. **Expected**: Badge count decreases (order marked as viewed)
5. **Expected**: Animation continues if count > 0

### Step 6: All Orders Viewed
1. View all pending orders
2. **Expected**: Badge disappears when count reaches 0
3. **Expected**: No animation when badge is hidden

---

## Visual Verification Checklist

### Autoscroll Dialog
- [ ] Dialog opens with gradient header
- [ ] Autoscroll happens automatically (not manual)
- [ ] Scroll targets first incomplete item
- [ ] Scroll animation is smooth (not instant)
- [ ] Hover effects work on all timeline items
- [ ] Hover background tint is subtle (not harsh)
- [ ] Hover scale is minimal (1.02x, not 1.5x)
- [ ] Manual scrolling works after autoscroll
- [ ] All items are readable and accessible

### Seller Orders Badge
- [ ] Badge appears when count > 0
- [ ] Badge disappears when count = 0
- [ ] Badge count is accurate
- [ ] Badge color is Deep Orange
- [ ] Badge position is top-right of Orders icon
- [ ] Pulsing animation is smooth
- [ ] Animation scale: 1.0 → 1.15 → 1.0
- [ ] Animation alpha: 1.0 → 0.7 → 1.0
- [ ] Animation duration: ~1.2 seconds per cycle
- [ ] Animation is continuous (no stuttering)

---

## Performance Testing

### Autoscroll Performance
1. Open tracking dialog multiple times
2. **Expected**: No lag or jank
3. **Expected**: Smooth 60fps animation
4. **Expected**: Dialog opens quickly

### Badge Animation Performance
1. Create 5+ orders
2. **Expected**: Badge animates smoothly
3. **Expected**: No frame drops
4. **Expected**: No battery drain from animation
5. **Expected**: Animation stops when app is backgrounded

---

## Troubleshooting

### Autoscroll Not Working
- **Check**: Is there an incomplete item in timeline?
- **Check**: Is dialog opening properly?
- **Check**: Try waiting 300ms after dialog opens
- **Solution**: Refresh order data and try again

### Hover Effects Not Visible
- **Check**: Are you hovering over timeline items?
- **Check**: Is the background tint too subtle?
- **Solution**: Look for slight color change and scale effect
- **Note**: Hover effects may not work on touch devices

### Badge Not Animating
- **Check**: Is badge count > 0?
- **Check**: Is the badge visible?
- **Check**: Try creating a new order
- **Solution**: Refresh dashboard or restart app

### Badge Count Wrong
- **Check**: Are all orders marked as viewed?
- **Check**: Are there pending/confirmed orders?
- **Solution**: Check Firestore console for order status
- **Solution**: Manually mark orders as viewed

---

## Expected Behavior Summary

| Feature | Expected Behavior | Animation |
|---------|-------------------|-----------|
| **Autoscroll** | Scrolls to first incomplete item on dialog open | Smooth 300ms delay, then animate scroll |
| **Hover Effect** | Background tint + scale on hover | 200ms smooth transition |
| **Badge Count** | Shows number of pending orders | Updates in real-time |
| **Badge Animation** | Continuous pulsing when count > 0 | 1.2s cycle: scale 1.0→1.15→1.0, alpha 1.0→0.7→1.0 |
| **Badge Visibility** | Appears when count > 0, disappears when count = 0 | Instant appear/disappear |

---

## Notes

- Autoscroll delay (300ms) prevents jank during dialog render
- Hover effects use Compose's `MutableInteractionSource` for proper detection
- Badge animation uses `infiniteRepeatable` with `EaseInOutCubic` easing
- All animations are GPU-accelerated for smooth performance
- Real-time updates use Firestore snapshot listeners

---

## Quick Commands

### View Order Tracking Dialog
```
Navigate to: My Orders → Select Order → Track Order
```

### View Seller Orders Badge
```
Navigate to: Seller Dashboard → Look at bottom navigation Orders icon
```

### Create Test Order
```
As Buyer: Browse products → Add to cart → Checkout → Place order
As Seller: Check dashboard for new order badge
```

---

## Support

If you encounter any issues:
1. Check the troubleshooting section above
2. Verify all files are properly saved
3. Rebuild the app
4. Clear app cache and try again
5. Check Firestore console for data consistency
