# Seller Verification Security Implementation Guide

## Executive Summary

Your codebase has **4 critical security gaps** that allow unverified sellers to publish products by bypassing UI gates. This document provides professional, production-ready fixes with implementation priority and testing guidance.

---

## Critical Security Gaps

### Gap #1: Missing Client-Side Verification Gate (AddProductScreen.kt)
**Severity:** HIGH | **Impact:** UI bypass possible  
**Current State:** Publish button enabled regardless of seller verification status

### Gap #2: Missing Server-Side Verification Check (AddProductViewModel.publishProduct)
**Severity:** CRITICAL | **Impact:** Direct API bypass possible  
**Current State:** ViewModel trusts client-passed `sellerVerified` boolean

### Gap #3: Missing is_removed Filter (ProductRepository.getAllProducts)
**Severity:** MEDIUM | **Impact:** Admin-removed products still visible  
**Current State:** Products with `is_removed=true` still returned if `is_active=true`

### Gap #4: Field Name Mismatch (AddProductViewModel.loadProductForEditing)
**Severity:** MEDIUM | **Impact:** Editing loses co-seller store and negotiation data  
**Current State:** Reading `coSellerStoreId` but Firestore stores as `co_seller_store_id`

### Gap #5: Uninitialized Verification Status (AuthRepository.signUp)
**Severity:** HIGH | **Impact:** New sellers default to undefined verification state  
**Current State:** `verification_status` not explicitly set during email signup

---

## Implementation Plan

### Priority 1: Server-Side Verification Check (CRITICAL)

**File:** `AddProductViewModel.kt` → `publishProduct()` function

**Why This First:** This is the most critical fix. Without it, a technically-savvy user can bypass the UI and call the ViewModel directly.

```kotlin
fun publishProduct(
    context: Context,
    title: String,
    description: String,
    category: String,
    price: Double,
    stock: Int,
    weightKg: Double,
    coSellerStoreId: String,
    minimumPrice: Double,
    autoAcceptDiscount: Int,
    sellerId: String,
    sellerName: String,
    sellerVerified: Boolean = false  // ⚠️ NEVER TRUST THIS
) {
    viewModelScope.launch {
        try {
            _productState.value = ProductState.Loading

            // ✅ CRITICAL FIX: Verify seller status directly from Firestore
            // Never trust the client-passed sellerVerified boolean
            val sellerDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(sellerId)
                .get()
                .await()

            val verificationStatus = sellerDoc.getString("verification_status") ?: ""
            val userStatus = sellerDoc.getString("status") ?: ""

            // Block unverified and deleted sellers
            if (verificationStatus != "approved" || userStatus == "deleted") {
                _productState.value = ProductState.Error(
                    "Only verified sellers can publish products. " +
                    "Please complete seller verification first."
                )
                Log.w("AddProductViewModel", 
                    "⚠️ SECURITY: Unverified seller attempted to publish: $sellerId (status: $verificationStatus)")
                return@launch
            }

            // ✅ Continue with existing validation...
            if (title.isBlank() || description.isBlank() || category.isBlank()) {
                _productState.value = ProductState.Error("Please fill all required fields")
                return@launch
            }

            if (price <= 0) {
                _productState.value = ProductState.Error("Price must be greater than 0")
                return@launch
            }

            if (stock <= 0) {
                _productState.value = ProductState.Error("Stock must be at least 1")
                return@launch
            }

            if (_selectedImages.value.isEmpty()) {
                _productState.value = ProductState.Error("Please add at least one product image")
                return@launch
            }

            // Calculate auto-accept price from discount
            val autoAcceptPrice = if (_isNegotiationEnabled.value && autoAcceptDiscount > 0) {
                price * (1 - autoAcceptDiscount / 100.0)
            } else {
                0.0
            }

            // Create product
            val product = Product(
                sellerId = sellerId,
                sellerName = sellerName,
                sellerVerified = true,  // ✅ NOW SAFE: We verified it server-side
                title = title,
                description = description,
                category = category,
                price = price,
                stock = stock,
                weightKg = weightKg,
                coSellerStoreId = coSellerStoreId,
                isNegotiable = _isNegotiationEnabled.value,
                minimumPrice = if (_isNegotiationEnabled.value) minimumPrice else 0.0,
                autoAcceptPrice = autoAcceptPrice,
                autoAcceptDiscount = autoAcceptDiscount,
                specifications = _specifications.value,
                isDraft = false,
                isActive = true,
                approvalStatus = "pending"
            )

            // Upload and create
            val result = productRepository.createProduct(
                context = context,
                product = product,
                imageUris = _selectedImages.value
            )

            if (result.isSuccess) {
                val productId = result.getOrNull() ?: ""
                
                try {
                    val activityData = mapOf(
                        "seller_id" to sellerId,
                        "type" to "PRODUCT_ADDED",
                        "title" to "Product Added",
                        "description" to "Added $title to your store",
                        "timestamp" to com.google.firebase.Timestamp.now(),
                        "order_id" to "",
                        "product_id" to productId
                    )
                    
                    FirebaseFirestore.getInstance()
                        .collection("activities")
                        .add(activityData)
                        .await()
                    
                    Log.d("AddProductViewModel", "✅ Activity logged for product: $productId")
                } catch (e: Exception) {
                    Log.e("AddProductViewModel", "Failed to log activity", e)
                }
                
                _productState.value = ProductState.Success(
                    "Product published successfully!",
                    productId
                )
            } else {
                _productState.value = ProductState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to publish product"
                )
            }

        } catch (e: Exception) {
            Log.e("AddProductViewModel", "Publish product error", e)
            _productState.value = ProductState.Error(e.message ?: "Failed to publish product")
        }
    }
}
```

**Testing:**
```
Test Case 1: Unverified seller attempts to publish
- Create seller account (verification_status = "not_submitted")
- Try to publish product
- Expected: Error "Only verified sellers can publish products"

Test Case 2: Deleted seller attempts to publish
- Create seller, verify, then soft-delete account
- Try to publish product
- Expected: Error "Only verified sellers can publish products"

Test Case 3: Verified seller publishes successfully
- Create seller, set verification_status = "approved"
- Publish product
- Expected: Success
```

---

### Priority 2: Client-Side Verification Gate (HIGH)

**File:** `AddProductScreen.kt` → Publish and Save as Draft buttons

**Why Second:** This prevents accidental bypasses and improves UX. Combined with server-side check, it's defense-in-depth.

```kotlin
// ✅ PUBLISH BUTTON - Add verification check
Button(
    onClick = {
        if (isEditMode) {
            addProductViewModel.updateProduct(
                context = context, 
                title = title, 
                description = description, 
                category = category,
                price = price.toDoubleOrNull() ?: 0.0, 
                stock = stock.toIntOrNull() ?: 0,
                weightKg = weightKg.toDoubleOrNull() ?: 0.0, 
                coSellerStoreId = selectedStoreId,
                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                sellerId = user.id, 
                sellerName = user.name
            )
        } else {
            addProductViewModel.publishProduct(
                context = context, 
                title = title, 
                description = description, 
                category = category,
                price = price.toDoubleOrNull() ?: 0.0, 
                stock = stock.toIntOrNull() ?: 0,
                weightKg = weightKg.toDoubleOrNull() ?: 0.0, 
                coSellerStoreId = selectedStoreId,
                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                sellerId = user.id, 
                sellerName = user.name,
                sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
            )
        }
    },
    // ✅ ADD THIS: Disable button if seller not verified
    enabled = productState !is ProductState.Loading 
        && productState !is ProductState.DraftSaved
        && user.verificationStatus == VerificationStatus.APPROVED,  // ✅ NEW
    modifier = Modifier.weight(1f).height(48.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    contentPadding = PaddingValues(0.dp),
    shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(Primary, PrimaryLight)), 
            RoundedCornerShape(12.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        if (productState is ProductState.Loading && productState !is ProductState.DraftSaved) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp), 
                    color = Color.White, 
                    strokeWidth = 2.dp
                )
                Text("Publishing...", fontSize = 13.sp, color = Color.White)
            }
        } else {
            Text(
                text = if (isEditMode) "Update Product" else "Publish Product", 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color.White
            )
        }
    }
}

// ✅ SAVE AS DRAFT BUTTON - Also block unverified sellers
OutlinedButton(
    onClick = {
        addProductViewModel.saveDraft(
            context = context, 
            title = title, 
            description = description, 
            category = category,
            price = price.toDoubleOrNull() ?: 0.0, 
            stock = stock.toIntOrNull() ?: 0,
            weightKg = weightKg.toDoubleOrNull() ?: 0.0, 
            coSellerStoreId = selectedStoreId,
            minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
            autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
            sellerId = user.id, 
            sellerName = user.name,
            sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
        )
    },
    // ✅ ADD THIS: Block drafts from unverified sellers
    enabled = user.verificationStatus == VerificationStatus.APPROVED,  // ✅ NEW
    modifier = Modifier.weight(1f).height(48.dp),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
    shape = RoundedCornerShape(12.dp)
) { 
    Text(text = "Save as Draft", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) 
}
```

**Why Block Drafts:** Drafts from unverified sellers shouldn't exist. They create confusion and potential loopholes.

---

### Priority 3: Initialize Verification Status (HIGH)

**File:** `AuthRepository.kt` → `signUp()` function

**Why Third:** This ensures new sellers start with explicit, correct verification status.

```kotlin
suspend fun signUp(
    email: String,
    password: String,
    name: String,
    role: UserRole
): Result<User> {
    return try {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("User creation failed")

        val user = User(
            id = firebaseUser.uid,
            email = email,
            name = name,
            role = role,
            createdAt = System.currentTimeMillis(),
            // ✅ CRITICAL: Explicitly set verification status for sellers
            verificationStatus = if (role == UserRole.SELLER) {
                VerificationStatus.NOT_SUBMITTED
            } else {
                VerificationStatus.NOT_SUBMITTED  // Buyers also get this, but it's ignored
            },
            // ✅ CRITICAL: Explicitly set seller application status
            sellerApplicationStatus = SellerApplicationStatus.NONE,
            // ✅ CRITICAL: Explicitly set status (not deleted)
            status = ""
        )

        usersCollection.document(firebaseUser.uid).set(user.toMap()).await()

        Log.d(TAG, "✅ User created with explicit verification status: ${firebaseUser.uid}")
        Result.success(user)

    } catch (e: Exception) {
        Log.e(TAG, "Sign up failed", e)
        Result.failure(e)
    }
}
```

---

### Priority 4: Add is_removed Filter (MEDIUM)

**File:** `ProductRepository.kt` → `getAllProducts()`, `getProductsByCategory()`, and `CoSellerStoreRepository.getStoreProducts()`

**Why Fourth:** Prevents admin-removed products from appearing in search/browse.

```kotlin
/**
 * Get all active products (real-time)
 */
fun getAllProducts(): Flow<List<Product>> = callbackFlow {
    val listener = productsCollection
        .whereEqualTo("is_active", true)
        .whereEqualTo("is_draft", false)
        .whereEqualTo("approval_status", "approved")
        .whereEqualTo("is_removed", false)  // ✅ ADD THIS
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Failed to get products", error)
                close(error)
                return@addSnapshotListener
            }

            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing product: ${doc.id}", e)
                    null
                }
            } ?: emptyList()

            Log.d(TAG, "✅ Loaded ${products.size} products")
            trySend(products)
        }

    awaitClose { listener.remove() }
}

/**
 * Get products by category
 */
fun getProductsByCategory(category: String): Flow<List<Product>> = callbackFlow {
    val listener = productsCollection
        .whereEqualTo("category", category)
        .whereEqualTo("is_active", true)
        .whereEqualTo("is_draft", false)
        .whereEqualTo("is_removed", false)  // ✅ ADD THIS
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing product: ${doc.id}", e)
                    null
                }
            } ?: emptyList()

            Log.d(TAG, "Loaded ${products.size} products for category: $category")
            trySend(products)
        }

    awaitClose { listener.remove() }
}
```

**For CoSellerStoreRepository.getStoreProducts():**
```kotlin
// In CoSellerStoreRepository.kt
fun getStoreProducts(storeId: String): Flow<List<Product>> = callbackFlow {
    val listener = firestore
        .collection("co_seller_stores")
        .document(storeId)
        .collection("products")
        .whereEqualTo("is_active", true)
        .whereEqualTo("is_draft", false)
        .whereEqualTo("is_removed", false)  // ✅ ADD THIS
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing product: ${doc.id}", e)
                    null
                }
            } ?: emptyList()

            trySend(products)
        }

    awaitClose { listener.remove() }
}
```

---

### Priority 5: Fix Field Name Mismatch (MEDIUM)

**File:** `AddProductViewModel.kt` → `loadProductForEditing()` function

**Why Fifth:** Ensures editing preserves co-seller store and negotiation settings.

```kotlin
fun loadProductForEditing(productId: String) {
    viewModelScope.launch {
        try {
            _editingProductId.value = productId
            val doc = FirebaseFirestore.getInstance()
                .collection("products")
                .document(productId)
                .get()
                .await()

            if (doc.exists()) {
                titleState.value = doc.getString("title") ?: ""
                descriptionState.value = doc.getString("description") ?: ""
                priceState.value = (doc.getDouble("price") ?: 0.0).toInt().toString()
                stockState.value = (doc.getLong("stock") ?: 0L).toString()
                categoryState.value = doc.getString("category") ?: ""
                weightKgState.value = (doc.getDouble("weight_kg") ?: 0.0).toString()  // ✅ FIX: weight_kg
                
                // ✅ FIX: Read co_seller_store_id (snake_case)
                selectedStoreIdState.value = doc.getString("co_seller_store_id") ?: ""
                
                minimumPriceState.value = (doc.getDouble("minimum_price") ?: 0.0).let {
                    if (it > 0) it.toInt().toString() else ""
                }
                autoAcceptDiscountState.value = (doc.getLong("auto_accept_discount") ?: 0L).let {
                    if (it > 0) it.toString() else ""
                }
                
                // ✅ FIX: Read is_negotiable (snake_case)
                _isNegotiationEnabled.value = doc.getBoolean("is_negotiable") ?: false
                
                _existingImageUrls.value = (doc.get("image_urls") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                
                @Suppress("UNCHECKED_CAST")
                _specifications.value = (doc.get("specifications") as? Map<String, String>) ?: emptyMap()
            }
        } catch (e: Exception) {
            Log.e("AddProductViewModel", "Failed to load product for editing", e)
        }
    }
}
```

---

## Firestore Security Rules (Bonus)

Add these rules to prevent direct product creation bypassing the ViewModel:

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ✅ Products: Only verified sellers can create
    match /products/{productId} {
      allow create: if request.auth != null 
        && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.verification_status == "approved"
        && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.status != "deleted";
      
      allow update: if request.auth != null 
        && resource.data.seller_id == request.auth.uid
        && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.verification_status == "approved";
      
      allow read: if resource.data.is_removed != true 
        && resource.data.is_active == true 
        && resource.data.is_draft == false;
    }
    
    // ✅ Users: Prevent direct verification_status modification
    match /users/{userId} {
      allow update: if request.auth.uid == userId 
        && !("verification_status" in request.writeFields)
        && !("verified" in request.writeFields)
        && !("status" in request.writeFields);
    }
  }
}
```

---

## Testing Checklist

### Unit Tests
- [ ] Unverified seller cannot publish (server-side check)
- [ ] Deleted seller cannot publish
- [ ] Verified seller can publish
- [ ] Draft button disabled for unverified sellers
- [ ] Publish button disabled for unverified sellers
- [ ] Field names correctly read from Firestore (co_seller_store_id, is_negotiable, etc.)

### Integration Tests
- [ ] New seller signup initializes verification_status = "not_submitted"
- [ ] Admin-removed products don't appear in getAllProducts()
- [ ] Admin-removed products don't appear in getProductsByCategory()
- [ ] Editing product preserves co-seller store and negotiation settings

### Security Tests
- [ ] Direct ViewModel call with sellerVerified=true is rejected
- [ ] Firestore rules prevent direct product creation
- [ ] Deleted account cannot publish even if verification_status was "approved"

---

## Deployment Checklist

- [ ] Implement Priority 1 (Server-side check) first
- [ ] Deploy and test in staging
- [ ] Implement Priority 2 (Client-side gate)
- [ ] Implement Priority 3 (Verification status initialization)
- [ ] Implement Priority 4 (is_removed filter)
- [ ] Implement Priority 5 (Field name mismatch)
- [ ] Update Firestore security rules
- [ ] Run full test suite
- [ ] Deploy to production
- [ ] Monitor logs for security warnings

---

## Summary

| Gap | Priority | Fix | Impact |
|-----|----------|-----|--------|
| Server-side verification | CRITICAL | Add Firestore check in publishProduct() | Prevents direct API bypass |
| Client-side gate | HIGH | Disable buttons for unverified sellers | Prevents accidental bypass |
| Verification initialization | HIGH | Set explicit status in signUp() | Ensures correct initial state |
| is_removed filter | MEDIUM | Add whereEqualTo filter | Prevents removed products appearing |
| Field name mismatch | MEDIUM | Use snake_case field names | Preserves editing data |

**Total Implementation Time:** ~2-3 hours  
**Testing Time:** ~1-2 hours  
**Risk Level:** LOW (all changes are additive, no breaking changes)
