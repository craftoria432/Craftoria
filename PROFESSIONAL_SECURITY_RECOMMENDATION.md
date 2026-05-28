# Professional Security Recommendation: Seller Verification Enforcement

## Executive Summary

Your Craftoria application has **5 security gaps** that allow unverified sellers to publish products. I've identified the exact locations and provided production-ready fixes with implementation priority.

**Recommendation:** Implement all 5 fixes in the order specified. Total implementation time: ~50 minutes. Risk level: LOW.

---

## The Security Problem

### Current State
- **UI Gate:** Publish button has no verification check
- **Server Gate:** ViewModel trusts client-passed `sellerVerified` boolean
- **Data Gate:** Drafts can be created by unverified sellers
- **Data Integrity:** Editing products loses co-seller store and negotiation data
- **Product Visibility:** Admin-removed products still appear in search

### Attack Scenarios

**Scenario 1: UI Bypass**
```
1. Unverified seller opens AddProductScreen
2. Publish button is enabled (no verification check)
3. Seller clicks Publish
4. Product is published without verification
```

**Scenario 2: Direct API Bypass**
```
1. Unverified seller decompiles APK or inspects network traffic
2. Calls AddProductViewModel.publishProduct() directly
3. Passes sellerVerified=true
4. ViewModel trusts the boolean and publishes
5. No server-side verification occurs
```

**Scenario 3: Draft Loophole**
```
1. Unverified seller creates draft (no verification required)
2. Later, seller's verification is approved
3. Seller publishes draft
4. Product appears as if it was created by verified seller
```

---

## Professional Implementation Strategy

### Why This Order?

**Priority 1: Server-Side Verification (CRITICAL)**
- Blocks the most dangerous attack (direct API bypass)
- Protects against technically-savvy users
- Must be done first because it's the foundation

**Priority 2: Client-Side Gate (HIGH)**
- Prevents accidental bypasses
- Improves UX by disabling buttons
- Defense-in-depth with server check

**Priority 3: Verification Initialization (HIGH)**
- Ensures new sellers start with correct status
- Prevents undefined state bugs
- Simplifies debugging

**Priority 4: is_removed Filter (MEDIUM)**
- Prevents admin-removed products from appearing
- Improves data integrity
- Relatively low risk

**Priority 5: Field Name Mismatch (MEDIUM)**
- Preserves editing data
- Prevents data loss
- Low risk, high value

---

## Implementation Details

### Fix #1: Server-Side Verification (CRITICAL)

**File:** `AddProductViewModel.kt` → `publishProduct()`

**What it does:**
- Before creating product, queries Firestore for seller's verification_status
- Blocks if status ≠ "approved" or account is deleted
- Logs security warning for audit trail

**Code location:** Right after `_productState.value = ProductState.Loading`

**Impact:** Prevents direct API bypass. This is the most important fix.

```kotlin
// ✅ CRITICAL FIX: Verify seller status directly from Firestore
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

---

### Fix #2: Client-Side Verification Gate (HIGH)

**File:** `AddProductScreen.kt` → Publish and Save as Draft buttons

**What it does:**
- Disables Publish button if seller not verified
- Disables Save as Draft button if seller not verified
- Provides visual feedback to user

**Code changes:**
- Publish button: Add `&& user.verificationStatus == VerificationStatus.APPROVED` to enabled condition
- Save as Draft button: Add `enabled = user.verificationStatus == VerificationStatus.APPROVED`

**Impact:** Prevents UI bypass and improves UX.

---

### Fix #3: Verification Status Initialization (HIGH)

**File:** `AuthRepository.kt` → `signUp()`

**What it does:**
- Explicitly sets `verification_status = "not_submitted"` for new sellers
- Explicitly sets `sellerApplicationStatus = "none"`
- Explicitly sets `status = ""` (not deleted)

**Code change:**
```kotlin
val user = User(
    id = firebaseUser.uid,
    email = email,
    name = name,
    role = role,
    createdAt = System.currentTimeMillis(),
    verificationStatus = VerificationStatus.NOT_SUBMITTED,  // ✅ ADD
    sellerApplicationStatus = SellerApplicationStatus.NONE,  // ✅ ADD
    status = ""  // ✅ ADD
)
```

**Impact:** Ensures new sellers start with correct, explicit verification status.

---

### Fix #4: Add is_removed Filter (MEDIUM)

**File:** `ProductRepository.kt` → `getAllProducts()` and `getProductsByCategory()`

**What it does:**
- Adds `.whereEqualTo("is_removed", false)` to Firestore queries
- Prevents admin-removed products from appearing in search/browse

**Code change:**
```kotlin
val listener = productsCollection
    .whereEqualTo("is_active", true)
    .whereEqualTo("is_draft", false)
    .whereEqualTo("approval_status", "approved")
    .whereEqualTo("is_removed", false)  // ✅ ADD THIS
    .addSnapshotListener { snapshot, error ->
```

**Impact:** Improves data integrity and prevents removed products from appearing.

---

### Fix #5: Fix Field Name Mismatch (MEDIUM)

**File:** `AddProductViewModel.kt` → `loadProductForEditing()`

**What it does:**
- Changes field names from camelCase to snake_case to match Firestore schema
- Ensures editing preserves co-seller store and negotiation settings

**Code changes:**
```kotlin
// FROM:
selectedStoreIdState.value = doc.getString("coSellerStoreId") ?: ""
_isNegotiationEnabled.value = doc.getBoolean("isNegotiable") ?: false
weightKgState.value = (doc.getDouble("weightKg") ?: 0.0).toString()

// TO:
selectedStoreIdState.value = doc.getString("co_seller_store_id") ?: ""  // ✅ snake_case
_isNegotiationEnabled.value = doc.getBoolean("is_negotiable") ?: false  // ✅ snake_case
weightKgState.value = (doc.getDouble("weight_kg") ?: 0.0).toString()  // ✅ snake_case
```

**Impact:** Prevents data loss when editing products.

---

## Testing Strategy

### Unit Tests
```kotlin
// Test 1: Unverified seller cannot publish
@Test
fun testUnverifiedSellerCannotPublish() {
    // Create seller with verification_status = "not_submitted"
    // Call publishProduct()
    // Assert: Error message returned
}

// Test 2: Verified seller can publish
@Test
fun testVerifiedSellerCanPublish() {
    // Create seller with verification_status = "approved"
    // Call publishProduct()
    // Assert: Product created successfully
}

// Test 3: Deleted seller cannot publish
@Test
fun testDeletedSellerCannotPublish() {
    // Create seller, set status = "deleted"
    // Call publishProduct()
    // Assert: Error message returned
}
```

### Integration Tests
```kotlin
// Test 4: New seller signup initializes verification_status
@Test
fun testNewSellerSignupInitializesVerificationStatus() {
    // Call signUp()
    // Assert: verification_status = "not_submitted"
}

// Test 5: Editing preserves co-seller store
@Test
fun testEditingPreservesCoSellerStore() {
    // Create product with co-seller store
    // Call loadProductForEditing()
    // Assert: selectedStoreIdState contains correct store ID
}
```

### Security Tests
```kotlin
// Test 6: Direct ViewModel call with sellerVerified=true is rejected
@Test
fun testDirectViewModelCallIsRejected() {
    // Create unverified seller
    // Call publishProduct() with sellerVerified=true
    // Assert: Error message returned (server check overrides client value)
}
```

---

## Deployment Checklist

- [ ] **Code Review:** Have another developer review all changes
- [ ] **Unit Tests:** All tests pass locally
- [ ] **Integration Tests:** Test with real Firestore data
- [ ] **Security Tests:** Verify all attack scenarios are blocked
- [ ] **Staging Deployment:** Deploy to staging environment
- [ ] **Staging Testing:** Run full test suite in staging
- [ ] **Production Deployment:** Deploy to production
- [ ] **Monitoring:** Watch logs for security warnings
- [ ] **Documentation:** Update API documentation if needed

---

## Risk Assessment

| Fix | Risk | Mitigation |
|-----|------|-----------|
| Server-side verification | LOW | Additive check, doesn't break existing flow |
| Client-side gate | LOW | UI-only change, no data impact |
| Verification initialization | LOW | Only affects new signups |
| is_removed filter | LOW | Additive filter, doesn't affect existing data |
| Field name mismatch | LOW | Fixes existing bug, improves data integrity |

**Overall Risk:** LOW  
**Rollback Plan:** Simple - revert commits, no data migration needed

---

## Performance Impact

| Fix | Query Impact | Latency Impact |
|-----|--------------|----------------|
| Server-side verification | +1 Firestore read | +50-100ms per publish |
| Client-side gate | None | None |
| Verification initialization | None | None |
| is_removed filter | +1 where clause | Negligible |
| Field name mismatch | None | None |

**Total Latency Impact:** ~50-100ms per product publish (acceptable)

---

## Compliance & Audit Trail

### Security Logging
```kotlin
Log.w("AddProductViewModel", 
    "⚠️ SECURITY: Unverified seller attempted to publish: $sellerId (status: $verificationStatus)")
```

This creates an audit trail for:
- Compliance reviews
- Security investigations
- Fraud detection

### Firestore Security Rules (Optional)
```firestore
// Add to firestore.rules for defense-in-depth
match /products/{productId} {
  allow create: if request.auth != null 
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.verification_status == "approved"
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.status != "deleted";
}
```

---

## Summary

| Aspect | Details |
|--------|---------|
| **Total Fixes** | 5 security gaps |
| **Implementation Time** | ~50 minutes |
| **Testing Time** | ~1-2 hours |
| **Risk Level** | LOW |
| **Breaking Changes** | None |
| **Performance Impact** | ~50-100ms per publish |
| **Rollback Difficulty** | Easy (simple revert) |

---

## Next Steps

1. **Review** this document with your team
2. **Implement** fixes in priority order (1-5)
3. **Test** each fix before moving to next
4. **Deploy** to staging first
5. **Monitor** logs for security warnings
6. **Deploy** to production
7. **Document** changes in your security log

---

## Questions?

**Q: Will this break existing functionality?**  
A: No. All changes are additive. Existing verified sellers will continue to work normally.

**Q: What about existing unverified sellers?**  
A: They'll see error messages when trying to publish. They need to complete verification first.

**Q: Do I need to update Firestore rules?**  
A: Recommended but not required. The ViewModel check is sufficient for now.

**Q: What if a seller's verification status changes?**  
A: The server-side check reads from Firestore every time, so it's always current.

**Q: How do I test this?**  
A: See "Testing Strategy" section above. Run unit tests, integration tests, and security tests.

---

## Conclusion

These 5 fixes provide **defense-in-depth** security for seller verification:

1. **Server-side check** blocks direct API bypass
2. **Client-side gate** prevents UI bypass
3. **Verification initialization** ensures correct initial state
4. **is_removed filter** improves data integrity
5. **Field name fix** prevents data loss

Together, they ensure that **only verified sellers can publish products**, regardless of how the request originates.

**Recommendation:** Implement all 5 fixes. The effort is minimal (~50 minutes), the risk is low, and the security benefit is significant.
