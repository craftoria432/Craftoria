# MyOrdersScreen Preview - Quick Start

## View the Preview

1. Open `MyOrdersScreen.kt`
2. Scroll to bottom
3. Click "Preview" button
4. Done!

---

## Preview Code

```kotlin
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyOrdersScreenPreview() {
    CraftoriaTheme {
        MyOrdersScreen(
            userId = "buyer123",
            cartViewModel = viewModel(),
            onBackClick = {},
            onNavigateToProduct = {},
            onNavigateToCart = {},
            onNavigateToRefundRequest = {}
        )
    }
}
```

---

## What You'll See

✅ Gradient header with "My Orders"  
✅ Filter tabs (All, Pending, Processing, Shipped, Delivered, Completed, Cancelled)  
✅ Order cards with professional styling  
✅ Unified badge sizing (10dp h-padding, 6dp v-padding, 11sp font)  
✅ Professional filter tab sizing (60dp-140dp width)  
✅ All order statuses and refund badges  

---

## File Location

```
app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyOrdersScreen.kt
```

---

## Key Features

- Simple and clean
- Follows LoginScreen pattern
- Shows system UI
- Fast to render
- Easy to maintain

---

## Related Documentation

- `MYORDERS_SIMPLE_PREVIEW_COMPLETE.md` - Full details
- `TASK_3_FINAL_SUMMARY.md` - Task completion summary
- `BADGE_AND_FILTER_TABS_PROFESSIONAL_SIZING_COMPLETE.md` - Sizing specs
