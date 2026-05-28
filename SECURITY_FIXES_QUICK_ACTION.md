# Seller Verification Security Fixes - Quick Action Guide

## What's the Problem?

Unverified sellers can publish products by:
1. **Bypassing UI** - Publish button has no verification check
2. **Calling ViewModel directly** - Server doesn't verify seller status
3. **Creating drafts** - No verification required for drafts either
4. **Editing products** - Field name mismatches lose co-seller data

## Professional Implementation (Do This Order)

### Step 1: Server-Side Verification (CRITICAL - Do First)
**File:** `AddProductViewModel.kt` → `publishProduct()` function

**What to add:** Before creating the product, query Firestore to verify seller status:

```kotlin
// Add this at the START of publishProduct() after _productState.value = ProductState.Loading

// ✅ CRITICAL: Verify seller status directly from Firestore
val sellerDoc = FirebaseFirestore.getInstance()
    .collection("users")
    .document(sellerId)
    .get()
    .await()

val verificationStatus = sellerDoc.getString("verification_status") ?: ""
val userStatus = sellerDoc.getString("status") ?: ""

if (verificationStatus != "approved" || userStatus == "deleted") {
    _productState.value = ProductState.Error(
        "Only verified sellers can publish products. Please complete seller verification first."
    )
    Log.w("AddProductViewModel", 
        "⚠️ SECURITY: Unverified seller attempted to publish: $sellerId (status: $verificationStatus)")
    return@launch
}
```

**Why:** This is the most critical fix. Without it, anyone can bypass the UI and publish.

---

### Step 2: Client-Side Verification Gate (HIGH)
**File:** `AddProductScreen.kt` → Publish and Save as Draft buttons

**What to change:**

For **Publish Button**, change:
```kotlin
enabled = productState !is ProductState.Loading && productState !is ProductState.DraftSaved
```

To:
```kotlin
enabled = productState !is ProductState.Loading 
    && productState !is ProductState.DraftSaved
    && user.verificationStatus == VerificationStatus.APPROVED  // ✅ ADD THIS
```

For **Save as Draft Button**, change:
```kotlin
// Currently no enabled check
```

To:
```kotlin
enabled = user.verificationStatus == VerificationStatus.APPROVED  // ✅ ADD THIS
```

**Why:** Defense-in-depth. Prevents accidental bypasses and improves UX.

---

### Step 3: Initialize Verification Status (HIGH)
**File:** `AuthRepository.kt` → `signUp()` function

**What to change:**

From:
```kotlin
val user = User(
    id = firebaseUser.uid,
    email = email,
    name = name,
    role = role,
    createdAt = System.currentTimeMillis()
)
```

To:
```kotlin
val user = User(
    id = firebaseUser.uid,
    email = email,
    name = name,
    role = role,
    createdAt = System.currentTimeMillis(),
    verificationStatus = VerificationStatus.NOT_SUBMITTED,  // ✅ ADD THIS
    sellerApplicationStatus = SellerApplicationStatus.NONE,  // ✅ ADD THIS
    status = ""  // ✅ ADD THIS
)
```

**Why:** Ensures new sellers start with explicit, correct verification status.

---

### Step 4: Add is_removed Filter (MEDIUM)
**File:** `ProductRepository.kt` → `getAllProducts()` and `getProductsByCategory()`

**What to change:**

In `getAllProducts()`, from:
```kotlin
val listener = productsCollection
    .whereEqualTo("is_active", true)
    .whereEqualTo("is_draft", false)
    .whereEqualTo("approval_status", "approved")
```

To:
```kotlin
val listener = productsCollection
    .whereEqualTo("is_active", true)
    .whereEqualTo("is_draft", false)
    .whereEqualTo("approval_status", "approved")
    .whereEqualTo("is_removed", false)  // ✅ ADD THIS
```

Same for `getProductsByCategory()`.

**Why:** Prevents admin-removed products from appearing in search.

---

### Step 5: Fix Field Name Mismatch (MEDIUM)
**File:** `AddProductViewModel.kt` → `loadProductForEditing()` function

**What to change:**

From:
```kotlin
selectedStoreIdState.value = doc.getString("coSellerStoreId") ?: ""
_isNegotiationEnabled.value = doc.getBoolean("isNegotiable") ?: false
```

To:
```kotlin
selectedStoreIdState.value = doc.getString("co_seller_store_id") ?: ""  // ✅ snake_case
_isNegotiationEnabled.value = doc.getBoolean("is_negotiable") ?: false  // ✅ snake_case
```

Also fix:
```kotlin
weightKgState.value = (doc.getDouble("weightKg") ?: 0.0).toString()
```

To:
```kotlin
weightKgState.value = (doc.getDouble("weight_kg") ?: 0.0).toString()  // ✅ snake_case
```

**Why:** Ensures editing preserves co-seller store and negotiation settings.

---

## Testing (Do This After Each Fix)

### Test 1: Unverified Seller Cannot Publish
1. Create new seller account (verification_status = "not_submitted")
2. Try to publish product
3. **Expected:** Error message "Only verified sellers can publish products"

### Test 2: Verified Seller Can Publish
1. Create seller, manually set verification_status = "approved" in Firestore
2. Try to publish product
3. **Expected:** Product published successfully

### Test 3: Deleted Seller Cannot Publish
1. Create seller, set verification_status = "approved"
2. Soft-delete account (set status = "deleted")
3. Try to publish product
4. **Expected:** Error message "Only verified sellers can publish products"

### Test 4: Publish Button Disabled for Unverified
1. Create unverified seller account
2. Open AddProductScreen
3. **Expected:** Publish button is disabled (grayed out)

### Test 5: Save as Draft Button Disabled for Unverified
1. Create unverified seller account
2. Open AddProductScreen
3. **Expected:** Save as Draft button is disabled (grayed out)

### Test 6: Editing Preserves Co-Seller Store
1. Create product linked to co-seller store
2. Edit product
3. **Expected:** Co-seller store is still selected

---

## Deployment Steps

1. **Backup Firestore** (just in case)
2. **Implement Step 1** (Server-side check) → Test
3. **Implement Step 2** (Client-side gate) → Test
4. **Implement Step 3** (Verification initialization) → Test
5. **Implement Step 4** (is_removed filter) → Test
6. **Implement Step 5** (Field name mismatch) → Test
7. **Run full test suite**
8. **Deploy to production**
9. **Monitor logs** for security warnings

---

## Firestore Security Rules (Optional but Recommended)

Add to your `firestore.rules`:

```firestore
// Only verified sellers can create products
match /products/{productId} {
  allow create: if request.auth != null 
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.verification_status == "approved"
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.status != "deleted";
}
```

---

## Summary

| Fix | Time | Impact | Priority |
|-----|------|--------|----------|
| Server-side verification | 15 min | Prevents API bypass | CRITICAL |
| Client-side gate | 10 min | Prevents UI bypass | HIGH |
| Verification initialization | 5 min | Correct initial state | HIGH |
| is_removed filter | 10 min | Removes admin-blocked products | MEDIUM |
| Field name mismatch | 10 min | Preserves editing data | MEDIUM |

**Total Time:** ~50 minutes  
**Risk:** LOW (all additive, no breaking changes)

---

## Questions?

- **Q: Will this break existing functionality?**  
  A: No. All changes are additive. Existing verified sellers will continue to work.

- **Q: What about existing unverified sellers?**  
  A: They'll see error messages when trying to publish. They need to complete verification first.

- **Q: Do I need to update Firestore rules?**  
  A: Recommended but not required. The ViewModel check is sufficient for now.

- **Q: What if a seller's verification status changes?**  
  A: The server-side check reads from Firestore every time, so it's always current.
