# Co-Seller Payment Split Integration Checklist

## ✅ Step-by-Step Integration Guide

This checklist ensures proper integration of the payment split system into your existing codebase.

---

## Step 1: Verify Data Models ✅

### 1.1 Check PaymentModels.kt

**Verify these fields exist:**
```kotlin
data class SellerPayment(
    // ... existing fields ...
    
    // ✅ NEW: Payment splits
    @get:PropertyName("payment_splits")
    @set:PropertyName("payment_splits")
    var paymentSplits: List<PaymentSplit> = emptyList(),
    
    // ✅ NEW: Involved sellers for access control
    @get:PropertyName("involved_seller_ids")
    @set:PropertyName("involved_seller_ids")
    var involvedSellerIds: List<String> = emptyList(),
)

// ✅ NEW: Payment split data class
data class PaymentSplit(
    @get:PropertyName("seller_id")
    @set:PropertyName("seller_id")
    var sellerId: String = "",
    
    @get:PropertyName("seller_name")
    @set:PropertyName("seller_name")
    var sellerName: String = "",
    
    @get:PropertyName("split_percentage")
    @set:PropertyName("split_percentage")
    var splitPercentage: Double = 0.0,
    
    @get:PropertyName("split_amount")
    @set:PropertyName("split_amount")
    var splitAmount: Double = 0.0,
    
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = PaymentStatus.PENDING.toString()
)
```

**Status:** ✅ DONE

---

## Step 2: Verify CoSellerStore Model ✅

### 2.1 Check CoSellerStore.kt

**Verify this field exists:**
```kotlin
data class CoSellerStore(
    // ... existing fields ...
    
    // ✅ NEW: Payment split configuration
    @get:PropertyName("payment_split_config")
    @set:PropertyName("payment_split_config")
    var paymentSplitConfig: Map<String, Double> = emptyMap(),
)
```

**Status:** ✅ DONE

---

## Step 3: Verify Repository Layer ✅

### 3.1 Check CoSellerStorePaymentRepository.kt

**File should exist at:**
```
app/src/main/java/com/gcuf/craftoria/data/repository/CoSellerStorePaymentRepository.kt
```

**Verify these methods exist:**
- `loadStorePayments()` - Load payments with access control
- `getPaymentWithSplits()` - Get payment details
- `getMemberEarningsBreakdown()` - Calculate member earnings
- `getStoreRevenueSummary()` - Calculate store revenue
- `updatePaymentSplitStatus()` - Update split status
- `getMemberPayments()` - Get member's all payments

**Status:** ✅ DONE

### 3.2 Check PaymentSplitProcessor.kt

**File should exist at:**
```
app/src/main/java/com/gcuf/craftoria/utils/PaymentSplitProcessor.kt
```

**Verify this method exists:**
- `processOrderPaymentsWithSplits()` - Main payment creation logic

**Status:** ✅ DONE

---

## Step 4: Verify ViewModel Layer ✅

### 4.1 Check CoSellerStorePaymentViewModel.kt

**File should exist at:**
```
app/src/main/java/com/gcuf/craftoria/viewmodel/CoSellerStorePaymentViewModel.kt
```

**Verify these methods exist:**
- `loadStorePayments()` - Load payments
- `loadPaymentDetail()` - Load payment details
- `loadMemberEarnings()` - Load member earnings
- `loadStoreRevenue()` - Load store revenue
- `filterByStatus()` - Filter payments
- `getFilteredPayments()` - Get filtered list

**Status:** ✅ DONE

### 4.2 Check SellerPaymentViewModel.kt Updates

**Verify filtering logic exists:**
```kotlin
// Filter out co-seller store payments
val filteredPayments = allPayments.filter { payment ->
    payment.coSellerStoreId.isEmpty()
}
```

**Status:** ✅ DONE

---

## Step 5: Verify UI Layer ✅

### 5.1 Check CoSellerStorePaymentScreen.kt

**File should exist at:**
```
app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/CoSellerStorePaymentScreen.kt
```

**Verify these composables exist:**
- `CoSellerStorePaymentScreen()` - Main screen
- `StoreRevenueSummaryCards()` - Revenue display
- `PaymentCard()` - Payment item display

**Status:** ✅ DONE

---

## Step 6: Integration - Order Processing

### 6.1 Update CheckoutViewModel or OrderRepository

**Find where orders are created and payments are processed.**

**Current code (example):**
```kotlin
// In CheckoutViewModel or OrderRepository
suspend fun placeOrder(order: Order) {
    // ... create order ...
    
    // OLD: Process payments
    val paymentResult = paymentRepository.processOrderPayments(order)
}
```

**Update to:**
```kotlin
// In CheckoutViewModel or OrderRepository
suspend fun placeOrder(order: Order, items: List<OrderItem>) {
    // ... create order ...
    
    // ✅ NEW: Process payments with splits
    val processor = PaymentSplitProcessor(FirebaseFirestore.getInstance())
    val paymentResult = processor.processOrderPaymentsWithSplits(order, items)
    
    if (paymentResult.isSuccess) {
        Log.d(TAG, "✅ Payments created with splits")
    } else {
        Log.e(TAG, "❌ Payment processing failed", paymentResult.exceptionOrNull())
    }
}
```

**Files to check:**
- `CheckoutViewModel.kt`
- `OrderRepository.kt`
- Any other file that calls `processOrderPayments()`

**Status:** ⏳ TODO - Needs Integration

---

## Step 7: Integration - Navigation

### 7.1 Update NavGraph.kt

**Add new route for co-seller store payments:**

```kotlin
// In NavGraph.kt
composable(
    route = "co_seller_store_payment/{storeId}/{storeName}",
    arguments = listOf(
        navArgument("storeId") { type = NavType.StringType },
        navArgument("storeName") { type = NavType.StringType }
    )
) { backStackEntry ->
    val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
    val storeName = backStackEntry.arguments?.getString("storeName") ?: ""
    
    CoSellerStorePaymentScreen(
        storeId = storeId,
        storeName = storeName,
        onBackClick = { navController.popBackStack() },
        onPaymentClick = { paymentId ->
            // Navigate to payment detail if needed
            navController.navigate("payment_detail/$paymentId")
        }
    )
}
```

**Don't forget to import:**
```kotlin
import com.gcuf.craftoria.ui.screens.coseller.CoSellerStorePaymentScreen
```

**Status:** ⏳ TODO - Needs Integration

---

## Step 8: Integration - Store Dashboard

### 8.1 Update ManageCoSellerStoreScreen.kt

**Add button to view store payments:**

```kotlin
// In ManageCoSellerStoreScreen.kt
Button(
    onClick = {
        navController.navigate("co_seller_store_payment/$storeId/$storeName")
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Primary)
) {
    Icon(
        imageVector = Icons.Default.Receipt,
        contentDescription = "Payments",
        modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text("View Store Payments")
}
```

**Status:** ⏳ TODO - Needs Integration

---

## Step 9: Firestore Security Rules

### 9.1 Update firestore.rules

**Add access control rules:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ✅ Seller Payments - Access Control
    match /seller_payments/{paymentId} {
      allow read: if 
        request.auth.uid == resource.data.seller_id ||
        request.auth.uid in resource.data.involved_seller_ids;
      
      allow create: if request.auth.uid != null;
      allow update, delete: if request.auth.uid == resource.data.seller_id;
    }
    
    // ✅ Co-Seller Stores - Access Control
    match /co_seller_stores/{storeId} {
      allow read: if true; // Public read
      allow create: if request.auth.uid != null;
      allow update, delete: if 
        request.auth.uid == resource.data.owner_id ||
        request.auth.uid in resource.data.member_ids;
    }
    
  }
}
```

**Status:** ⏳ TODO - Needs Deployment

---

## Step 10: Testing

### 10.1 Manual Testing Checklist

**Test Case 1: Original Seller Payment**
- [ ] Create order with product from original seller
- [ ] Verify payment created with `coSellerStoreId = ""`
- [ ] Verify `paymentSplits = []`
- [ ] Verify seller sees payment in dashboard
- [ ] Verify amount is correct

**Test Case 2: Co-Seller Store Payment**
- [ ] Create order with products from co-seller store
- [ ] Verify payment created with `coSellerStoreId = "store_id"`
- [ ] Verify `paymentSplits` populated correctly
- [ ] Verify store owner sees payment in store dashboard
- [ ] Verify split amounts calculated correctly

**Test Case 3: Mixed Order**
- [ ] Create order with products from both original seller and co-seller store
- [ ] Verify 2 payments created
- [ ] Verify original seller payment has no splits
- [ ] Verify co-seller store payment has splits
- [ ] Verify both sellers see correct payments

**Test Case 4: Access Control**
- [ ] Verify seller cannot view other seller's payments
- [ ] Verify non-member cannot view store payments
- [ ] Verify store owner can view store payments
- [ ] Verify store member can view store payments

**Test Case 5: UI Display**
- [ ] Verify CoSellerStorePaymentScreen loads
- [ ] Verify revenue cards show correct data
- [ ] Verify payment list displays correctly
- [ ] Verify payment splits display correctly
- [ ] Verify filter buttons work

### 10.2 Automated Testing

**Create unit tests for:**
```kotlin
// Test PaymentSplitProcessor
class PaymentSplitProcessorTest {
    @Test
    fun testGroupItemsByStore() { }
    
    @Test
    fun testCreatePaymentSplits() { }
    
    @Test
    fun testProcessOrderPaymentsWithSplits() { }
}

// Test CoSellerStorePaymentRepository
class CoSellerStorePaymentRepositoryTest {
    @Test
    fun testAccessControl() { }
    
    @Test
    fun testMemberEarningsCalculation() { }
    
    @Test
    fun testStoreRevenueCalculation() { }
}
```

**Status:** ⏳ TODO - Needs Implementation

---

## Step 11: Deployment

### 11.1 Pre-Deployment Checklist

- [ ] All code changes reviewed
- [ ] All tests passing
- [ ] No compilation errors
- [ ] Firestore security rules reviewed
- [ ] Staging environment tested
- [ ] Performance tested with multiple orders
- [ ] Access control verified
- [ ] Error handling verified

### 11.2 Deployment Steps

1. **Backup Firestore data**
   ```bash
   # Use Firebase Console to export data
   ```

2. **Deploy code changes**
   - Deploy data models
   - Deploy repositories
   - Deploy ViewModels
   - Deploy UI screens

3. **Deploy Firestore security rules**
   ```bash
   firebase deploy --only firestore:rules
   ```

4. **Monitor logs**
   - Check for errors
   - Verify payments created correctly
   - Check access control working

### 11.3 Post-Deployment Checklist

- [ ] Monitor error logs for 24 hours
- [ ] Verify payments created correctly
- [ ] Verify access control working
- [ ] Check performance metrics
- [ ] Gather user feedback
- [ ] Document any issues

**Status:** ⏳ TODO - Needs Execution

---

## Step 12: Rollback Plan

**If issues occur:**

1. **Revert code changes**
   ```bash
   git revert <commit-hash>
   ```

2. **Revert Firestore rules**
   ```bash
   firebase deploy --only firestore:rules
   ```

3. **Restore from backup**
   - Use Firebase Console to restore data

4. **Notify users**
   - Explain the issue
   - Provide timeline for fix

---

## Integration Status Summary

| Component | Status | Notes |
|-----------|--------|-------|
| Data Models | ✅ DONE | PaymentModels.kt, CoSellerStore.kt |
| Repositories | ✅ DONE | CoSellerStorePaymentRepository.kt, PaymentSplitProcessor.kt |
| ViewModels | ✅ DONE | CoSellerStorePaymentViewModel.kt, SellerPaymentViewModel.kt |
| UI Screens | ✅ DONE | CoSellerStorePaymentScreen.kt |
| Order Processing | ⏳ TODO | Integrate PaymentSplitProcessor |
| Navigation | ⏳ TODO | Add route to NavGraph.kt |
| Store Dashboard | ⏳ TODO | Add button to ManageCoSellerStoreScreen.kt |
| Firestore Rules | ⏳ TODO | Deploy security rules |
| Testing | ⏳ TODO | Manual and automated tests |
| Deployment | ⏳ TODO | Follow deployment steps |

---

## Quick Integration Commands

### Add to build.gradle.kts (if needed)
```kotlin
dependencies {
    // Already included in your project
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services")
}
```

### Import statements needed
```kotlin
import com.gcuf.craftoria.data.repository.CoSellerStorePaymentRepository
import com.gcuf.craftoria.utils.PaymentSplitProcessor
import com.gcuf.craftoria.viewmodel.CoSellerStorePaymentViewModel
import com.gcuf.craftoria.ui.screens.coseller.CoSellerStorePaymentScreen
```

---

## Troubleshooting

### Issue: Compilation errors
**Solution:**
- Verify all files created in correct locations
- Check imports are correct
- Rebuild project: `Build > Rebuild Project`

### Issue: Payments not showing splits
**Solution:**
- Verify PaymentSplitProcessor is being called
- Check store has paymentSplitConfig set
- Check items have coSellerStoreId set

### Issue: Access denied errors
**Solution:**
- Verify user is store member
- Check Firestore security rules deployed
- Check involvedSellerIds includes user

### Issue: UI not displaying
**Solution:**
- Verify CoSellerStorePaymentScreen imported correctly
- Check navigation route added to NavGraph
- Verify ViewModel initialized correctly

---

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review implementation guide: `CO_SELLER_PAYMENT_SPLIT_IMPLEMENTATION_GUIDE.md`
3. Review architecture: `CO_SELLER_ARCHITECTURE_PRODUCTION_READY.md`
4. Check logs for error messages

---

**Status:** ✅ IMPLEMENTATION COMPLETE - READY FOR INTEGRATION

**Next Steps:** Follow Step 6-12 to integrate into your codebase
