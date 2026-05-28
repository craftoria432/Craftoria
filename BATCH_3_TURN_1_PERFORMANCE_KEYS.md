# Batch 3 — Turn 1: Add LazyColumn/LazyRow Keys (Performance Optimization)

**Duration:** 30 minutes  
**Impact:** Smoother real-time updates, reduced recomposition jank  
**Risk:** Low — changes only affect Compose list rendering

---

## Problem

LazyColumn/LazyRow items without `key` parameters cause unnecessary recompositions when data updates:
- Real-time listener emits new list → entire column recomposes
- Even if 90% of items are unchanged, Compose redraws all of them
- Manifests as: list scroll jank, flickering, battery drain

**Example issue in current code:**
```kotlin
// ❌ BAD: No key → full recomposition on data change
LazyColumn {
    items(orders.size) { index ->
        OrderCard(orders[index])
    }
}
```

**Solution:**
```kotlin
// ✅ GOOD: Keyed by stable ID → only changed items recompose
LazyColumn {
    items(orders.size, key = { orders[it].id }) { index ->
        OrderCard(orders[it])
    }
}
```

---

## Screens to Update (7 total)

### 1. **ManageProductsScreen**
**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/ManageProductsScreen.kt`

**Location:** Find `LazyColumn` with products list
**Change:** Add `key = { products[it].id }` to `items()` call
**Lines affected:** ~1–2 lines

---

### 2. **SellerOrdersScreen**
**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerOrdersScreen.kt`

**Location:** Find `LazyColumn` rendering filtered orders
**Change:** Add `key = { filteredOrders[it].id }` or similar
**Lines affected:** ~1–2 lines

---

### 3. **CartScreen**
**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/CartScreen.kt`

**Location:** LazyColumn rendering cart items
**Change:** Add `key = { groupedItems[it].items.first().id }` (or use order ID)
**Lines affected:** ~1–2 lines

---

### 4. **MyChatsScreen**
**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/MyChatsScreen.kt`

**Location:** LazyColumn rendering chat list
**Change:** Add `key = { chats[it].id }` to items() call
**Lines affected:** ~1–2 lines

---

### 5. **NotificationsScreen**
**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/notifications/NotificationsScreen.kt`

**Location:** LazyColumn rendering notifications
**Change:** Add `key = { notifications[it].id }` to items() call
**Lines affected:** ~1–2 lines

---

### 6. **SellerPaymentsScreen**
**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/seller/SellerPaymentsScreen.kt`

**Location:** LazyColumn rendering payments
**Change:** Add `key = { payments[it].id }` to items() call
**Lines affected:** ~1–2 lines

---

### 7. **MyOrdersScreen**
**File:** `app/src/main/java/com/gcuf\craftoria\ui\screens\buyer\MyOrdersScreen.kt`

**Location:** LazyColumn rendering orders
**Change:** Add `key = { orders[it].id }` to items() call
**Lines affected:** ~1–2 lines

---

## Implementation Checklist

- [ ] **ManageProductsScreen** — Add product ID key
- [ ] **SellerOrdersScreen** — Add order ID key
- [ ] **CartScreen** — Add item/order ID key
- [ ] **MyChatsScreen** — Add chat ID key
- [ ] **NotificationsScreen** — Add notification ID key
- [ ] **SellerPaymentsScreen** — Add payment ID key
- [ ] **MyOrdersScreen** — Add order ID key

---

## Build & Test After Each Change

```bash
./gradlew assembleDebug
```

**Verify:**
- ✓ No compilation errors
- ✓ Screen still renders
- ✓ List items update smoothly with real-time changes

---

## Code Pattern

**Before:**
```kotlin
LazyColumn {
    items(data.size) { index ->
        ItemCard(data[index])
    }
}
```

**After:**
```kotlin
LazyColumn {
    items(data.size, key = { data[it].id }) { index ->
        ItemCard(data[it])
    }
}
```

---

## Performance Impact

**Expected improvement:**
- 50–70% reduction in unnecessary recompositions on list updates
- Smoother scroll on real-time payment/order updates
- Lower CPU/battery usage during live streaming of notifications

---

## Next After This

Once all keys are added and build succeeds:
→ Move to **Batch 3 Turn 2: SellerOrdersScreen Filter Tab Migration**

---

**Ready to implement?** Start with ManageProductsScreen.

