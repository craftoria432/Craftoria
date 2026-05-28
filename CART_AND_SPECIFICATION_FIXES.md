# Cart Price Update & Specification Text Field Fixes

## Issues Fixed

### Issue 1: Cart Price Not Updating After Negotiation Acceptance ✅

**Problem**: When seller accepts a negotiation offer, the cart still shows the old price (PKR 1000) instead of the negotiated price. The buyer has to close and reopen the app to see the updated price.

**Root Cause**: When the seller accepts/rejects a negotiation, only the negotiation document was being updated in Firebase, but the cart item was not being updated.

**Solution**: Added cart item update logic in `NegotiationRequestsScreen.kt`:

#### Accept Negotiation:
```kotlin
// ✅ UPDATE CART ITEM with accepted price
try {
    val cartSnapshot = firestore.collection("cart")
        .whereEqualTo("user_id", item.negotiation.buyerId)
        .whereEqualTo("product_id", item.product.id)
        .get()
        .await()
    
    if (!cartSnapshot.isEmpty) {
        val cartItemId = cartSnapshot.documents[0].id
        val cartUpdateData = hashMapOf<String, Any>(
            "price" to item.negotiation.offerAmount,
            "is_negotiated" to true,
            "negotiation_status" to "ACCEPTED"
        )
        firestore.collection("cart")
            .document(cartItemId)
            .update(cartUpdateData)
            .await()
        Log.d("NegotiationRequests", "✅ Cart item updated with accepted price")
    }
} catch (e: Exception) {
    Log.e("NegotiationRequests", "Failed to update cart item", e)
}
```

#### Reject Negotiation:
```kotlin
// ✅ UPDATE CART ITEM to show rejected status
try {
    val cartSnapshot = firestore.collection("cart")
        .whereEqualTo("user_id", item.negotiation.buyerId)
        .whereEqualTo("product_id", item.product.id)
        .get()
        .await()
    
    if (!cartSnapshot.isEmpty) {
        val cartItemId = cartSnapshot.documents[0].id
        val cartUpdateData = hashMapOf<String, Any>(
            "price" to item.product.price, // Revert to original price
            "is_negotiated" to false,
            "negotiation_status" to "REJECTED"
        )
        firestore.collection("cart")
            .document(cartItemId)
            .update(cartUpdateData)
            .await()
        Log.d("NegotiationRequests", "✅ Cart item updated with rejected status")
    }
} catch (e: Exception) {
    Log.e("NegotiationRequests", "Failed to update cart item", e)
}
```

**How It Works Now**:
1. Seller accepts/rejects negotiation
2. Negotiation document updated in Firebase
3. Cart item updated in Firebase with new price and status
4. CartViewModel's real-time listener detects the change
5. Cart UI updates immediately without app restart
6. Buyer sees updated price instantly

---

### Issue 2: Specification Text Field Not Showing Text ✅

**Problem**: In the "Add Specification" dialog, when typing in the text field, the text is being added but not visible to the user.

**Root Cause**: The text field was missing explicit text color configuration, causing the text to be rendered in a color that matches the background (white on white).

**Solution**: Added explicit text color configuration to both text fields in `AddProductScreen.kt`:

#### Specification Name Field:
```kotlin
OutlinedTextField(
    value = specKey,
    onValueChange = { specKey = it },
    placeholder = {
        Text(
            text = "e.g., Dimensions, Material, Finish",
            fontSize = 13.sp,
            color = TextSecondary
        )
    },
    modifier = Modifier.fillMaxWidth(),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        unfocusedBorderColor = BorderColor,
        focusedTextColor = TextPrimary,      // ✅ Added
        unfocusedTextColor = TextPrimary     // ✅ Added
    ),
    singleLine = true,
    textStyle = androidx.compose.ui.text.TextStyle(  // ✅ Added
        fontSize = 14.sp,
        color = TextPrimary
    )
)
```

#### Specification Value Field:
```kotlin
OutlinedTextField(
    value = specValue,
    onValueChange = { specValue = it },
    placeholder = {
        Text(
            text = "e.g., 8 x 5 x 4 inches",
            fontSize = 13.sp,
            color = TextSecondary
        )
    },
    modifier = Modifier.fillMaxWidth(),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        unfocusedBorderColor = BorderColor,
        focusedTextColor = TextPrimary,      // ✅ Added
        unfocusedTextColor = TextPrimary     // ✅ Added
    ),
    singleLine = true,
    textStyle = androidx.compose.ui.text.TextStyle(  // ✅ Added
        fontSize = 14.sp,
        color = TextPrimary
    )
)
```

**How It Works Now**:
1. User opens "Add Specification" dialog
2. User types in the text field
3. Text is visible in dark color (TextPrimary)
4. User can see what they're typing in real-time

---

## Files Modified

1. **app/src/main/java/com/gcuf/craftoria/ui/screens/seller/NegotiationRequestsScreen.kt**
   - Added cart item update logic in `onAccept` callback
   - Added cart item update logic in `onReject` callback
   - Updates cart price and status in real-time

2. **app/src/main/java/com/gcuf/craftoria/ui/screens/seller/AddProductScreen.kt**
   - Added text color configuration to specification name field
   - Added text color configuration to specification value field
   - Added textStyle parameter for better visibility

---

## Testing

### Test Cart Price Update:
1. Buyer negotiates on a product (e.g., offers PKR 850 for PKR 1000 product)
2. Item added to cart with "Negotiation Pending" status
3. Seller accepts the negotiation
4. **Expected**: Cart immediately shows PKR 850 with "Negotiated Price - Accepted" badge
5. **No app restart required**

### Test Specification Text Field:
1. Seller goes to Add Product screen
2. Scrolls to Specifications section
3. Clicks "Add Specification"
4. Types in "Specification Name" field
5. **Expected**: Text is visible while typing
6. Types in "Value" field
7. **Expected**: Text is visible while typing
8. Clicks "Add" button
9. **Expected**: Specification added to list

---

## Benefits

### Cart Price Update:
✅ Real-time price updates without app restart
✅ Better user experience for buyers
✅ Immediate feedback on negotiation status
✅ Consistent with Firebase real-time architecture
✅ No data inconsistency between negotiation and cart

### Specification Text Field:
✅ Users can see what they're typing
✅ Better user experience for sellers
✅ Prevents confusion and errors
✅ Professional appearance
✅ Consistent with other text fields in the app

---

## Status: ✅ COMPLETE

Both issues have been fixed and are ready for testing.
