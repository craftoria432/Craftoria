# Seller Preview - Quick Reference Guide

## What Changed?

When a seller views their own product from Manage Products screen, they now see a **seller preview mode** with disabled buttons.

## How to Test

### Step 1: Open Manage Products
- Login as a seller
- Navigate to Seller Dashboard
- Tap "Manage Products"

### Step 2: View Product as Buyer
- Find any product you created
- Tap "View as Buyer" button

### Step 3: See Seller Preview
You should see:
- ✅ Yellow warning banner: "Seller Preview — Buttons are disabled"
- ✅ "Add to Cart" button is grayed out and unclickable
- ✅ "Negotiate" button is faded and unclickable
- ✅ "Chat" button is hidden
- ✅ "View Store" button is hidden
- ✅ All product details are visible

## What's Different from Buyer View?

| Feature | Buyer View | Seller Preview |
|---------|-----------|-----------------|
| Add to Cart | ✅ Active | ❌ Disabled |
| Negotiate | ✅ Active | ❌ Disabled |
| Chat Button | ✅ Visible | ❌ Hidden |
| View Store | ✅ Visible | ❌ Hidden |
| Warning Banner | ❌ None | ✅ Yellow Banner |
| Product Details | ✅ Visible | ✅ Visible |
| Seller Info | ✅ Visible | ✅ Visible |

## Code Changes

### File: ProductDetailsScreen.kt

**1. SellerCard Function**
- Added `isSellerPreview: Boolean = false` parameter
- Hide "Chat" button when `isSellerPreview = true`
- Hide "View Store" button when `isSellerPreview = true`

**2. SellerCard Call**
- Pass `isSellerPreview = isSellerPreview` parameter

## Navigation Flow

```
ManageProductsScreen
    ↓
onProductClick = { product ->
    navController.navigate(
        Screen.ProductDetails.createSellerPreviewRoute(product.id)
    )
}
    ↓
URL: "product/{productId}?sellerPreview=true"
    ↓
NavGraph extracts sellerPreview=true
    ↓
ProductDetailsScreen displays seller preview mode
```

## Visual Elements

### Yellow Warning Banner
```
┌─────────────────────────────────────────┐
│ 👁️ Seller Preview — Buttons are disabled │
└─────────────────────────────────────────┘
```
- Background: #FFF8E1 (Light Yellow)
- Icon Color: #856404 (Dark Brown)
- Text Color: #856404 (Dark Brown)

### Disabled Buttons
```
┌──────────────────┐  ┌──────────────────┐
│  Add to Cart     │  │    Negotiate     │
│  (Grayed Out)    │  │    (Faded)       │
└──────────────────┘  └──────────────────┘
```

## Features

✅ Professional appearance
✅ Clear visual indication
✅ Disabled buttons are non-functional
✅ Hidden interactive elements
✅ All product info visible
✅ Seller info visible
✅ Production ready

## Troubleshooting

### Issue: Seller preview not showing
**Solution**: Make sure you're tapping "View as Buyer" from Manage Products screen

### Issue: Buttons are still clickable
**Solution**: This shouldn't happen - check if you're in seller preview mode (yellow banner should be visible)

### Issue: Chat/View Store buttons still visible
**Solution**: These should be hidden in seller preview mode - verify the isSellerPreview parameter is being passed correctly

## Files Modified

- `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/ProductDetailsScreen.kt`

## No Breaking Changes

✅ Backward compatible
✅ Existing buyer view unchanged
✅ All other features work as before
✅ No database changes needed
✅ No API changes needed

## Production Status

✅ Complete
✅ Tested
✅ No compilation errors
✅ Ready to deploy
