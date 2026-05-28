# Security Fixes - Code Ready to Paste

Copy-paste these exact code blocks into your files. All changes are marked with ✅.

---

## Fix #1: AddProductViewModel.kt - Server-Side Verification

**Location:** `publishProduct()` function, right after `_productState.value = ProductState.Loading`

**Replace this section:**
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
    sellerVerified: Boolean = false
) {
    viewModelScope.launch {
        try {
            _productState.value = ProductState.Loading

            // Validate
            if (title.isBlank() || description.isBlank() || category.isBlank()) {
```

**With this:**
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
    sellerVerified: Boolean = false
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

            // Validate
            if (title.isBlank() || description.isBlank() || category.isBlank()) {
```

---

## Fix #2: AddProductScreen.kt - Publish Button

**Location:** Publish button in the Row with OutlinedButton

**Find this:**
```kotlin
Button(
    onClick = {
        if (isEditMode) {
            addProductViewModel.updateProduct(
                context = context, title = title, description = description, category = category,
                price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
                weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                sellerId = user.id, sellerName = user.name
            )
        } else {
            addProductViewModel.publishProduct(
                context = context, title = title, description = description, category = category,
                price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
                weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                sellerId = user.id, sellerName = user.name,
                sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
            )
        }
    },
    enabled = productState !is ProductState.Loading && productState !is ProductState.DraftSaved,
```

**Replace with:**
```kotlin
Button(
    onClick = {
        if (isEditMode) {
            addProductViewModel.updateProduct(
                context = context, title = title, description = description, category = category,
                price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
                weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                sellerId = user.id, sellerName = user.name
            )
        } else {
            addProductViewModel.publishProduct(
                context = context, title = title, description = description, category = category,
                price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
                weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                sellerId = user.id, sellerName = user.name,
                sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
            )
        }
    },
    // ✅ ADD THIS: Disable button if seller not verified
    enabled = productState !is ProductState.Loading 
        && productState !is ProductState.DraftSaved
        && user.verificationStatus == VerificationStatus.APPROVED,
```

---

## Fix #3: AddProductScreen.kt - Save as Draft Button

**Location:** OutlinedButton for "Save as Draft"

**Find this:**
```kotlin
OutlinedButton(
    onClick = {
        addProductViewModel.saveDraft(
            context = context, title = title, description = description, category = category,
            price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
            weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
            minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
            autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
            sellerId = user.id, sellerName = user.name,
            sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
        )
    },
    modifier = Modifier.weight(1f).height(48.dp),
```

**Replace with:**
```kotlin
OutlinedButton(
    onClick = {
        addProductViewModel.saveDraft(
            context = context, title = title, description = description, category = category,
            price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
            weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
            minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
            autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
            sellerId = user.id, sellerName = user.name,
            sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
        )
    },
    // ✅ ADD THIS: Block drafts from unverified sellers
    enabled = user.verificationStatus == VerificationStatus.APPROVED,
    modifier = Modifier.weight(1f).height(48.dp),
```

---

## Fix #4: AuthRepository.kt - Initialize Verification Status

**Location:** `signUp()` function

**Find this:**
```kotlin
val user = User(
    id = firebaseUser.uid,
    email = email,
    name = name,
    role = role,
    createdAt = System.currentTimeMillis()
)
```

**Replace with:**
```kotlin
val user = User(
    id = firebaseUser.uid,
    email = email,
    name = name,
    role = role,
    createdAt = System.currentTimeMillis(),
    // ✅ CRITICAL: Explicitly set verification status for sellers
    verificationStatus = VerificationStatus.NOT_SUBMITTED,
    // ✅ CRITICAL: Explicitly set seller application status
    sellerApplicationStatus = SellerApplicationStatus.NONE,
    // ✅ CRITICAL: Explicitly set status (not deleted)
    status = ""
)
```

---

## Fix #5: ProductRepository.kt - Add is_removed Filter to getAllProducts

**Location:** `getAllProducts()` function

**Find this:**
```kotlin
fun getAllProducts(): Flow<List<Product>> = callbackFlow {
    val listener = productsCollection
        .whereEqualTo("is_active", true)
        .whereEqualTo("is_draft", false)
        .whereEqualTo("approval_status", "approved")
        .addSnapshotListener { snapshot, error ->
```

**Replace with:**
```kotlin
fun getAllProducts(): Flow<List<Product>> = callbackFlow {
    val listener = productsCollection
        .whereEqualTo("is_active", true)
        .whereEqualTo("is_draft", false)
        .whereEqualTo("approval_status", "approved")
        .whereEqualTo("is_removed", false)  // ✅ ADD THIS
        .addSnapshotListener { snapshot, error ->
```

---

## Fix #6: ProductRepository.kt - Add is_removed Filter to getProductsByCategory

**Location:** `getProductsByCategory()` function

**Find this:**
```kotlin
fun getProductsByCategory(category: String): Flow<List<Product>> = callbackFlow {
    val listener = productsCollection
        .whereEqualTo("category", category)
        .whereEqualTo("is_active", true)
        .whereEqualTo("is_draft", false)
        .addSnapshotListener { snapshot, error ->
```

**Replace with:**
```kotlin
fun getProductsByCategory(category: String): Flow<List<Product>> = callbackFlow {
    val listener = productsCollection
        .whereEqualTo("category", category)
        .whereEqualTo("is_active", true)
        .whereEqualTo("is_draft", false)
        .whereEqualTo("is_removed", false)  // ✅ ADD THIS
        .addSnapshotListener { snapshot, error ->
```

---

## Fix #7: AddProductViewModel.kt - Fix Field Name Mismatch

**Location:** `loadProductForEditing()` function

**Find this:**
```kotlin
if (doc.exists()) {
    titleState.value = doc.getString("title") ?: ""
    descriptionState.value = doc.getString("description") ?: ""
    priceState.value = (doc.getDouble("price") ?: 0.0).toInt().toString()
    stockState.value = (doc.getLong("stock") ?: 0L).toString()
    categoryState.value = doc.getString("category") ?: ""
    weightKgState.value = (doc.getDouble("weightKg") ?: 0.0).toString()
    selectedStoreIdState.value = doc.getString("coSellerStoreId") ?: ""
    minimumPriceState.value = (doc.getDouble("minimumPrice") ?: 0.0).let {
        if (it > 0) it.toInt().toString() else ""
    }
    autoAcceptDiscountState.value = (doc.getLong("autoAcceptDiscount") ?: 0L).let {
        if (it > 0) it.toString() else ""
    }
    _isNegotiationEnabled.value = doc.getBoolean("isNegotiable") ?: false
    _existingImageUrls.value = (doc.get("imageUrls") as? List<*>)
        ?.filterIsInstance<String>() ?: emptyList()
```

**Replace with:**
```kotlin
if (doc.exists()) {
    titleState.value = doc.getString("title") ?: ""
    descriptionState.value = doc.getString("description") ?: ""
    priceState.value = (doc.getDouble("price") ?: 0.0).toInt().toString()
    stockState.value = (doc.getLong("stock") ?: 0L).toString()
    categoryState.value = doc.getString("category") ?: ""
    weightKgState.value = (doc.getDouble("weight_kg") ?: 0.0).toString()  // ✅ FIX: snake_case
    selectedStoreIdState.value = doc.getString("co_seller_store_id") ?: ""  // ✅ FIX: snake_case
    minimumPriceState.value = (doc.getDouble("minimum_price") ?: 0.0).let {  // ✅ FIX: snake_case
        if (it > 0) it.toInt().toString() else ""
    }
    autoAcceptDiscountState.value = (doc.getLong("auto_accept_discount") ?: 0L).let {  // ✅ FIX: snake_case
        if (it > 0) it.toString() else ""
    }
    _isNegotiationEnabled.value = doc.getBoolean("is_negotiable") ?: false  // ✅ FIX: snake_case
    _existingImageUrls.value = (doc.get("image_urls") as? List<*>)  // ✅ FIX: snake_case
        ?.filterIsInstance<String>() ?: emptyList()
```

---

## Verification Checklist

After pasting each fix, verify:

- [ ] Fix #1: Server-side check added to publishProduct()
- [ ] Fix #2: Publish button has verification check
- [ ] Fix #3: Save as Draft button has verification check
- [ ] Fix #4: signUp() initializes verification_status
- [ ] Fix #5: getAllProducts() has is_removed filter
- [ ] Fix #6: getProductsByCategory() has is_removed filter
- [ ] Fix #7: loadProductForEditing() uses snake_case field names

---

## Build & Test

After pasting all fixes:

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Check for compilation errors
./gradlew lint
```

---

## Deployment

1. Commit changes: `git commit -m "Security: Add seller verification checks"`
2. Push to staging branch
3. Test in staging environment
4. Merge to main
5. Deploy to production
6. Monitor logs for security warnings

---

## Need Help?

If you get compilation errors:
- Make sure you have the correct imports (FirebaseFirestore, VerificationStatus, etc.)
- Check that field names match your Firestore schema
- Verify that all classes are properly imported

If tests fail:
- Run individual test cases
- Check logs for specific error messages
- Verify Firestore test data is correct
