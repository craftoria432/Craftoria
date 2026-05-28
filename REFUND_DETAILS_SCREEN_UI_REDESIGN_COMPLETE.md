# Refund Details Screen UI Redesign

## Overview
Updated the RefundDetailsScreen to match the professional NotificationsScreen UI layout with a gradient header and filter tabs.

## Changes Made

### 1. Professional Gradient Header
**Before:**
- Simple CraftoriaTopBar with just title

**After:**
- Gradient TopAppBar (Primary → PrimaryLight) matching NotificationsScreen
- Title with subtitle showing order ID
- Back button in circular background
- Consistent with app's premium design language

```kotlin
TopAppBar(
    title = {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = "Refund Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                lineHeight = 16.sp
            )
            if (refund != null) {
                Text(
                    text = "Order #${refund!!.orderId.take(8).uppercase()}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 12.sp
                )
            }
        }
    },
    navigationIcon = {
        IconButton(onClick = onBackClick) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    modifier = Modifier.background(
        brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
    )
)
```

### 2. Filter Tabs Section
**New:** Added RefundDetailsTabs composable with three tabs:
- **Overview** - Order info, refund details, action buttons
- **Timeline** - Refund status timeline
- **Breakdown** - Payment breakdown details

**Design Pattern:**
- Pill-style tabs matching NotificationFilterTabs
- Selected tab: Primary background, white text
- Unselected tab: White background, 0.5dp BorderColor outline
- White surface with 0.5dp bottom divider
- Horizontal scrollable LazyRow for future extensibility

```kotlin
@Composable
private fun RefundDetailsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Overview", "Timeline", "Breakdown")

    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tabs.size) { index ->
                    val isSelected = selectedTab == index
                    Surface(
                        onClick = { onTabSelected(index) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Primary else Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 0.dp else 0.5.dp,
                            color = if (isSelected) Primary else BorderColor
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = tabs[index],
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        }
    }
}
```

### 3. Tab-Based Content Organization
**Before:**
- All content displayed at once in scrollable column

**After:**
- Content organized into three tabs
- Only relevant section displayed based on selected tab
- Cleaner, more focused UX

```kotlin
when (selectedTab) {
    0 -> {
        // Overview: Status banner, order info, refund info, action buttons
        RefundStatusBanner(status = refund!!.status)
        InfoSection(...)
        InfoSection(...)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(...) { Text("View Order") }
            Button(...) { Text("Support") }
        }
    }
    1 -> {
        // Timeline: Full refund status timeline
        RefundTimeline(refund = refund!!)
    }
    2 -> {
        // Breakdown: Payment breakdown details
        PaymentBreakdown(...)
    }
}
```

### 4. Container Color
- Added `containerColor = BackgroundSecondary` to Scaffold for consistent background

## Visual Improvements

### Header
- ✅ Gradient background (Primary → PrimaryLight)
- ✅ Circular back button with semi-transparent background
- ✅ Title + subtitle layout
- ✅ Matches NotificationsScreen design

### Filter Tabs
- ✅ Pill-style buttons
- ✅ Selected state: Primary fill, white text
- ✅ Unselected state: White background, bordered
- ✅ Smooth transitions
- ✅ Consistent with app's filter tab pattern

### Content Area
- ✅ Tab-based organization
- ✅ Cleaner visual hierarchy
- ✅ Better information scannability
- ✅ Professional spacing and typography

## Files Modified
- `app/src/main/java/com/gcuf/craftoria/ui/screens/buyer/RefundDetailsScreen.kt`

## Compilation Status
✅ **No errors** - File compiles successfully
✅ **No warnings** - Clean build

## Design Consistency
- ✅ Matches NotificationsScreen header pattern
- ✅ Matches NotificationFilterTabs pill style
- ✅ Uses app theme colors (Primary, PrimaryLight, BorderColor, etc.)
- ✅ Consistent typography and spacing
- ✅ Professional gradient design

## User Experience Benefits
1. **Better Information Organization** - Content grouped into logical tabs
2. **Cleaner Interface** - Less visual clutter, focused content
3. **Consistent Design** - Matches other screens in the app
4. **Professional Appearance** - Gradient header elevates visual quality
5. **Improved Scannability** - Users can quickly find relevant information

## Testing Checklist
- [ ] Navigate to a refund details screen
- [ ] Verify gradient header displays correctly
- [ ] Click each tab (Overview, Timeline, Breakdown)
- [ ] Verify correct content displays for each tab
- [ ] Verify back button works
- [ ] Verify action buttons work (View Order, Support)
- [ ] Test on different screen sizes
- [ ] Verify smooth tab transitions
