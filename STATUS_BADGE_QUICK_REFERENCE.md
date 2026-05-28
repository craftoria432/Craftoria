# Status Badge Consistency — Quick Reference

## Summary
Status badges across the **Cart Screen** and **Manage Co-Seller Store Screen** are now unified with consistent styling, colors, and typography.

## Key Changes

### Badge Specification
| Property | Value |
|----------|-------|
| Shape | Pill (20dp border radius) |
| Font Size | 11sp |
| Font Weight | SemiBold |
| Padding | 10dp horizontal, 6dp vertical |
| Border | None (solid background) |

### Color Palette
```
PENDING:   Background #FFF3CD, Text #856404
ACCEPTED:  Background #D4EDDA, Text #155724
REJECTED:  Background #F8D7DA, Text #721C24
```

## Files Modified

### 1. CartScreen.kt
**Path:** `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/CartScreen.kt`

**Changes:**
- Updated `CartItemCard()` negotiation status badges
- Changed from 6dp to 20dp border radius
- Changed from 10sp to 11sp font size
- Removed borders, using solid colors
- Updated padding from 7dp×2dp to 10dp×6dp

**Statuses Affected:**
- `NegotiationStatus.PENDING` → Yellow badge
- `NegotiationStatus.ACCEPTED` / `AUTO_ACCEPTED` → Green badge
- `NegotiationStatus.REJECTED` / `DECLINED` → Red badge

### 2. ManageCoSellerStoreScreen.kt
**Path:** `app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/ManageCoSellerStoreScreen.kt`

**Changes:**
- Updated `InvitationCard()` status badges
- Changed from 6dp to 20dp border radius
- Changed from 10sp to 11sp font size
- Removed alpha transparency, using solid colors
- Updated padding from 10dp×4dp to 10dp×6dp

**Statuses Affected:**
- `InvitationStatus.PENDING` → Yellow badge
- `InvitationStatus.ACCEPTED` → Green badge
- `InvitationStatus.DECLINED` → Red badge

## Code Pattern

All badges now follow this pattern:

```kotlin
Surface(
    color = Color(0xFFFFF3CD),  // Background
    shape = RoundedCornerShape(20.dp)  // Pill shape
) {
    Text(
        text = "Pending",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF856404),  // Text
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
```

## Color Reference

### Hex Codes
- **Yellow (Pending):** Background `#FFF3CD`, Text `#856404`
- **Green (Accepted):** Background `#D4EDDA`, Text `#155724`
- **Red (Rejected):** Background `#F8D7DA`, Text `#721C24`

### Kotlin Color Objects
```kotlin
// Pending
Color(0xFFFFF3CD)  // Background
Color(0xFF856404)  // Text

// Accepted
Color(0xFFD4EDDA)  // Background
Color(0xFF155724)  // Text

// Rejected
Color(0xFFF8D7DA)  // Background
Color(0xFF721C24)  // Text
```

## Testing

### Visual Verification
- [ ] Cart screen badges display correctly
- [ ] Invitation badges match Cart badges
- [ ] All badges are pill-shaped (20dp radius)
- [ ] Font size is 11sp
- [ ] Colors match specification

### Functional Testing
- [ ] Pending badges appear for pending negotiations
- [ ] Accepted badges appear for accepted negotiations
- [ ] Rejected badges appear for rejected negotiations
- [ ] Invitation badges show correct status
- [ ] Badges are readable on all backgrounds

## Related Documentation

- **Full Implementation:** `STATUS_BADGE_CONSISTENCY_COMPLETE.md`
- **Visual Guide:** `STATUS_BADGE_VISUAL_REFERENCE.txt`
- **Component Reference:** `UnifiedBadgeComponent.kt`

## Notes

- All colors use direct hex values for consistency
- No theme token dependencies
- Badges are display-only (non-interactive)
- Meets WCAG AA contrast requirements
- Compatible with light and dark themes

---

**Status:** ✅ Complete
**Last Updated:** May 27, 2026
