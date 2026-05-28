# Seller & Buyer Orders - Exact Code Changes

## File 1: MyOrdersScreen.kt

### Change 1: Updated TrackOrderButton Signature
**Location**: Line ~770

```kotlin
// BEFORE
@Composable
fun TrackOrderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Pink hover colors
    val hoverPink = Color(0xFFFFE4E1)  // Light pink for hover background
    val hoverPinkBorder = Color(0xFFFFB6C1)  // Pink border for hover
    
    Button(
        onClick = onClick,
        modifier = modifier
            .hoverable(interactionSource = interactionSource),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(10.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isHovered) {
                        Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                    } else {
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                    },
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Track Order",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHovered) Primary else Color.White
            )
        }
    }
}

// AFTER
@Composable
fun TrackOrderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false  // ← NEW PARAMETER
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Pink colors - consistent across app
    val hoverPink = Color(0xFFFFE4E1)  // Light pink for hover background
    val hoverPinkBorder = Color(0xFFE91E8C)  // Pink border for hover
    
    Button(
        onClick = onClick,
        modifier = modifier
            .hoverable(interactionSource = interactionSource),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(10.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = when {
                        isHighlighted -> Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                        isHovered -> Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                        else -> Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                    },
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Track Order",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isHighlighted -> Primary
                    isHovered -> Primary
                    else -> Color.White
                }
            )
        }
    }
}
```

### Change 2: Updated OrderActionButtons Signature
**Location**: Line ~700

```kotlin
// BEFORE
@Composable
fun OrderActionButtons(
    order: Order,
    onViewDetails: () -> Unit,
    onTrackOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onReorder: () -> Unit
) {
    // ... implementation
    TrackOrderButton(
        onClick = onTrackOrder,
        modifier = Modifier.weight(1f).height(38.dp)
    )
}

// AFTER
@Composable
fun OrderActionButtons(
    order: Order,
    isHighlighted: Boolean = false,  // ← NEW PARAMETER
    onViewDetails: () -> Unit,
    onTrackOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onReorder: () -> Unit
) {
    // ... implementation
    TrackOrderButton(
        onClick = onTrackOrder,
        modifier = Modifier.weight(1f).height(38.dp),
        isHighlighted = isHighlighted  // ← PASSED HERE
    )
}
```

### Change 3: Updated OrderCard to Pass isHighlighted
**Location**: Line ~645

```kotlin
// BEFORE
OrderActionButtons(
    order = order,
    onViewDetails = onViewDetails,
    onTrackOrder = onTrackOrder,
    onCancelOrder = onCancelOrder,
    onReorder = onReorder
)

// AFTER
OrderActionButtons(
    order = order,
    isHighlighted = isHighlighted,  // ← NEW
    onViewDetails = onViewDetails,
    onTrackOrder = onTrackOrder,
    onCancelOrder = onCancelOrder,
    onReorder = onReorder
)
```

---

## File 2: SellerOrdersScreen.kt

### Change 1: Added LazyListState
**Location**: Line ~97

```kotlin
// BEFORE
var highlightedOrderId by remember { mutableStateOf(highlightOrderId) }
var shouldScrollToHighlighted by remember { mutableStateOf(highlightOrderId.isNotEmpty()) }

val snackbarHostState = remember { SnackbarHostState() }

// AFTER
var highlightedOrderId by remember { mutableStateOf(highlightOrderId) }
var shouldScrollToHighlighted by remember { mutableStateOf(highlightOrderId.isNotEmpty()) }

val snackbarHostState = remember { SnackbarHostState() }

// ✅ LazyListState for autoscroll functionality
val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
```

### Change 2: Updated LazyColumn with State
**Location**: Line ~220

```kotlin
// BEFORE
LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    items(orders) { order ->
        SellerOrderCard(
            order = order,
            isSelectionMode = isSelectionMode,
            isSelected = selectedOrders.contains(order.id),
            isHighlighted = highlightedOrderId == order.id,
            onSelectionToggle = {
                selectedOrders = if (selectedOrders.contains(order.id))
                    selectedOrders - order.id else selectedOrders + order.id
            },
            onViewDetails = {
                if (!isSelectionMode) {
                    selectedOrder = order
                    showOrderDetails = true
                    onOrderClick(order)
                }
            },
            // ... other callbacks
        )
    }
}

// AFTER
LazyColumn(
    state = lazyListState,  // ← NEW
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    items(orders) { order ->
        SellerOrderCard(
            order = order,
            isSelectionMode = isSelectionMode,
            isSelected = selectedOrders.contains(order.id),
            isHighlighted = highlightedOrderId == order.id,
            onSelectionToggle = {
                selectedOrders = if (selectedOrders.contains(order.id))
                    selectedOrders - order.id else selectedOrders + order.id
            },
            onViewDetails = {
                if (!isSelectionMode) {
                    selectedOrder = order
                    showOrderDetails = true
                    onOrderClick(order)
                    // ✅ Autoscroll to this order
                    coroutineScope.launch {
                        val index = orders.indexOf(order)
                        if (index >= 0) {
                            lazyListState.animateScrollToItem(index)
                        }
                    }
                }
            },
            // ... other callbacks
        )
    }
}
```

### Change 3: Updated Action Buttons to Use SellerActionButton
**Location**: Line ~560

```kotlin
// BEFORE
when {
    statusToCheck in listOf("NEW", "PENDING") -> {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f).height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.weight(1f).height(38.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Error),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
    statusToCheck == "PROCESSING" -> {
        Button(
            onClick = onMarkShipped,
            modifier = Modifier.fillMaxWidth().height(38.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text("Mark as Shipped", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        }
    }
    statusToCheck == "SHIPPED" -> {
        Button(
            onClick = onMarkDelivered,
            modifier = Modifier.fillMaxWidth().height(38.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Success),
            shape = RoundedCornerShape(10.dp)
        ) { Text("Mark as Delivered", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}

// AFTER
when {
    statusToCheck in listOf("NEW", "PENDING") -> {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SellerActionButton(  // ← NEW
                onClick = onAccept,
                label = "Accept",
                backgroundColor = Success,
                modifier = Modifier.weight(1f).height(38.dp),
                isHighlighted = isHighlighted
            )
            SellerActionButton(  // ← NEW
                onClick = onReject,
                label = "Reject",
                backgroundColor = Error,
                modifier = Modifier.weight(1f).height(38.dp),
                isHighlighted = isHighlighted
            )
        }
    }
    statusToCheck == "PROCESSING" -> {
        SellerActionButton(  // ← NEW
            onClick = onMarkShipped,
            label = "Mark as Shipped",
            backgroundColor = Primary,
            modifier = Modifier.fillMaxWidth().height(38.dp),
            isHighlighted = isHighlighted,
            isGradient = true
        )
    }
    statusToCheck == "SHIPPED" -> {
        SellerActionButton(  // ← NEW
            onClick = onMarkDelivered,
            label = "Mark as Delivered",
            backgroundColor = Success,
            modifier = Modifier.fillMaxWidth().height(38.dp),
            isHighlighted = isHighlighted
        )
    }
}
```

### Change 4: Added SellerActionButton Composable
**Location**: End of file (Line ~680)

```kotlin
// ── Seller Action Button with Pink Hover ──────────────────────────────────────

@Composable
fun SellerActionButton(
    onClick: () -> Unit,
    label: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    isGradient: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Pink colors - consistent across app
    val hoverPink = Color(0xFFFFE4E1)  // Light pink for hover background
    val hoverPinkBorder = Color(0xFFE91E8C)  // Pink border for hover
    
    Button(
        onClick = onClick,
        modifier = modifier.hoverable(interactionSource = interactionSource),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(10.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = when {
                        isHighlighted || isHovered -> {
                            if (isGradient) {
                                Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                            } else {
                                Brush.horizontalGradient(listOf(hoverPink, hoverPinkBorder))
                            }
                        }
                        isGradient -> Brush.horizontalGradient(listOf(backgroundColor, PrimaryLight))
                        else -> Brush.horizontalGradient(listOf(backgroundColor, backgroundColor))
                    },
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isHighlighted || isHovered -> Primary
                    else -> Color.White
                }
            )
        }
    }
}
```

### Change 5: Added Imports
**Location**: Top of file

```kotlin
// ADDED
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
```

---

## Summary of Changes

### MyOrdersScreen.kt
- **Lines Modified**: ~50
- **New Parameters**: `isHighlighted` in TrackOrderButton and OrderActionButtons
- **Breaking Changes**: None (all new parameters have default values)

### SellerOrdersScreen.kt
- **Lines Modified**: ~100
- **New Features**: LazyListState, autoscroll, SellerActionButton composable
- **New Imports**: 3 imports for hover interaction
- **Breaking Changes**: None (all changes are additive)

### Total Changes
- **Files Modified**: 2
- **Total Lines Changed**: ~150
- **New Composables**: 1 (SellerActionButton)
- **Breaking Changes**: 0
- **Backward Compatibility**: 100%

---

## Testing the Changes

### Test 1: Buyer Order Highlight
1. Navigate to My Orders
2. Receive notification with "Track Order" button
3. Click "Track Order"
4. Verify order highlights with pink background
5. Verify Track Order button shows pink gradient
6. Verify autoscroll brings order into view
7. Wait 10 seconds and verify highlight clears

### Test 2: Seller Order Highlight
1. Navigate to Orders
2. Receive notification with "View Order" button
3. Click "View Order"
4. Verify order highlights with pink background
5. Verify action buttons show pink gradient
6. Verify autoscroll brings order into view
7. Wait 10 seconds and verify highlight clears

### Test 3: Manual Hover
1. Navigate to My Orders
2. Hover over "Track Order" button
3. Verify button shows pink gradient
4. Move mouse away
5. Verify button returns to normal

### Test 4: Seller Button Hover
1. Navigate to Orders
2. Hover over "Accept" button
3. Verify button shows pink gradient
4. Move mouse away
5. Verify button returns to normal

---

## Deployment Instructions

1. **Backup Current Code**
   ```bash
   git commit -m "Backup before seller/buyer orders update"
   ```

2. **Apply Changes**
   - Replace MyOrdersScreen.kt
   - Replace SellerOrdersScreen.kt

3. **Verify Compilation**
   ```bash
   ./gradlew build
   ```

4. **Run Tests**
   - Test buyer order flow
   - Test seller order flow
   - Test notification navigation
   - Test hover effects

5. **Deploy**
   ```bash
   ./gradlew assembleRelease
   ```

---

## Rollback Instructions

If needed, rollback is simple since all changes are additive:

1. Remove `isHighlighted` parameter from TrackOrderButton calls
2. Remove `isHighlighted` parameter from OrderActionButtons calls
3. Remove LazyListState from SellerOrdersScreen
4. Replace SellerActionButton calls with original Button/OutlinedButton
5. Remove SellerActionButton composable

All changes are backward compatible and can be safely reverted.
