# Batch 3 — UI/UX Polish & Code Quality — Implementation Plan

**Status:** Ready for execution  
**Priority:** Optional (after Batch 2 deployment)  
**Estimated scope:** 4–6 hours  

---

## What's Already Done (Batch 2)

✅ **Placeholder routes implemented:**
- `all_activity` → redirects to Notifications
- `messages` → routes to MyChatsScreen
- `store_ratings/{storeId}` → StoreRatingsScreen
- `rate_store/{storeId}/{orderId}` → RateStoreScreen

✅ **Refund enums consolidated** — RefundProcessor uses canonical enums from RefundModels.kt

✅ **UI components in place:**
- `EmptyStateComponent.kt` (88dp circles, ready to adopt)
- `FilterTabComponent.kt` (40dp tabs with `Role.Tab`, accessibility)
- `UnifiedBadgeComponent.kt` (standardized badges)

---

## Batch 3 Tasks (Non-Blocking Polish)

### Task 1: SellerOrdersScreen Migration (Medium Priority)
**File:** `SellerOrdersScreen.kt`  
**Current:** Uses custom `SellerOrderFilterTabs` with 32dp height, 8.dp spacing  
**Target:** Replace with `FilterTabRow` component (40dp, consistent design)

**Changes:**
1. Remove `SellerOrderFilterTabs()` function (73 lines)
2. Replace with `FilterTabRow()` call in compose
3. Update imports to include FilterTabComponent
4. Map status filters to FilterTab data structure

**Impact:** +1 file unified, -73 lines of duplication

---

### Task 2: Empty State Standardization (Medium Priority)
**Screens to migrate (5 total):**
- [ ] WishlistScreen — custom empty state (icon size: 90dp)
- [ ] SearchScreen — custom empty state (icon size: 80dp)
- [ ] CartScreen — custom empty state (icon size: 88dp ✓ already matches)
- [ ] NotificationsScreen — custom "no unread" message
- [ ] SellerOrdersScreen — custom empty state on filter

**Pattern:** Replace local Column + Icon + Text with `EmptyStateComponent(message, icon)`

**File edits:** 5 screens × ~10–15 lines each = ~50–75 lines removed  
**Benefit:** Consistency + maintainability

---

### Task 3: Badge Component Unification (Low Priority)
**Current duplicates identified:**
1. **OrderStatusBadge** — custom Composable (appears in Order Cards)
2. **RefundStatusBadge** — custom Composable (Refund Details)
3. **PaymentStatusBadge** — custom Composable (Payment History)
4. **NegotiationBadge** — custom Composable (Dashboard)
5. **NewOrderBadge** — custom Composable (Tab counter)
6. **UnifiedBadgeComponent** ✓ — canonical (not adopted yet)

**Plan:** Migrate all to `UnifiedBadgeComponent` (single source, 88dp circles)  
**Impact:** +10 screens simplified, ~200 lines removed

---

### Task 4: Performance — LazyColumn Keys (High Priority)
**Issue:** Missing `key { it.id }` on list items causing unnecessary recomposition

**Screens to fix:**
- [ ] HomeScreen (Featured Stores list)
- [ ] ManageProductsScreen (Products list)
- [ ] SellerOrdersScreen (Orders list)
- [ ] CartScreen (Cart items)
- [ ] MyChatsScreen (Chat list)
- [ ] NotificationsScreen (Notifications list)
- [ ] SellerPaymentsScreen (Payments list)

**Pattern:**
```kotlin
items(items.size, key = { items[it].id }) { index ->
    // content
}
```

**Impact:** Reduced recomposition on real-time updates, smoother scrolling

---

### Task 5: Dead Code Removal (Low Priority)
**Files to remove or gate behind BuildConfig.DEBUG:**
1. [ ] `LegacyOrderStatusBadge.kt` — replaced by newer badge system
2. [ ] `RealTestScreen.kt` — orphaned test screen

**Files to deprecate (keep for now, mark @Deprecated):**
1. `OrderDialogs.kt` (1153 lines) — duplicate of `seller/OrderDialogs.kt` (638 lines)

---

## Recommended Execution Order

**Turn 1:** Task 4 (Performance — 30 min)  
→ Add `key` to 7 LazyColumn lists, verify build

**Turn 2:** Task 1 (SellerOrdersScreen — 45 min)  
→ Migrate filter tabs, test filtering works

**Turn 3:** Task 2 (Empty States — 60 min)  
→ Migrate 5 screens to EmptyStateComponent, verify visuals

**Turn 4:** Task 3 (Badge Unification — optional, 90 min)  
→ If time: consolidate 6 badge variants to UnifiedBadgeComponent

**Turn 5:** Task 5 (Cleanup — 15 min)  
→ Mark obsolete files, remove RealTestScreen

---

## Build Verification After Each Task

```bash
./gradlew assembleDebug
```

Ensure:
- ✓ No compilation errors
- ✓ No import issues
- ✓ Compose previews still generate

---

## Optional Future Work (Post-Batch 3)

- Merge duplicate OrderDialogs files (requires careful refactoring)
- Remove `PaymentIntegrityMonitor` full-collection scans (scope to user's docs)
- Add skeleton/shimmer loaders on Home, Payments, Orders screens
- Migrate remaining payment & order badge badges to UnifiedBadgeComponent

---

## Success Criteria

After Batch 3:
- ✅ No inline empty-state reimplementations across 5+ screens
- ✅ All LazyColumn items keyed for efficient recomposition
- ✅ SellerOrdersScreen uses FilterTabRow component
- ✅ Clean build with no deprecation warnings
- ✅ UI/UX score: 7.5/10 (up from 6/10)

---

**Ready to execute?** Start with Turn 1 (Performance).

