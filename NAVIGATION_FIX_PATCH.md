# Navigation Fix Patch for NavGraph.kt

## Problem
The app crashes when navigating to "my_orders" with the error:
```
Navigation destination that matches request NavDeepLinkRequest{ uri=android-app://androidx.navigation/my_orders } cannot be found in the navigation graph
```

## Solution
Replace the problematic navigation calls in NavGraph.kt with safer implementations.

## Changes Required

### 1. Add Import at the top of NavGraph.kt
```kotlin
import com.gcuf.craftoria.utils.NavigationHelper
```

### 2. Replace the onNavigateToOrders implementation (around line 257-259)

**FIND:**
```kotlin
onNavigateToOrders = {
    navController.navigate(Screen.MyOrders.route)
},
```

**REPLACE WITH:**
```kotlin
onNavigateToOrders = {
    NavigationHelper.safeNavigate(
        navController = navController,
        route = Screen.MyOrders.route,
        tag = "HomeScreen"
    )
},
```

### 3. Replace other problematic navigation calls

**FIND (around line 466):**
```kotlin
onTrackOrder = {
    navController.navigate(Screen.MyOrders.route) {
        popUpTo(Screen.Home.route)
    }
}
```

**REPLACE WITH:**
```kotlin
onTrackOrder = {
    NavigationHelper.safeNavigateWithPopUp(
        navController = navController,
        route = Screen.MyOrders.route,
        popUpToRoute = Screen.Home.route,
        tag = "OrderSuccess"
    )
}
```

**FIND (around line 519):**
```kotlin
"my_orders" -> {
    navController.navigate(Screen.MyOrders.route)
}
```

**REPLACE WITH:**
```kotlin
"my_orders" -> {
    NavigationHelper.safeNavigate(
        navController = navController,
        route = Screen.MyOrders.route,
        tag = "ProfileScreen"
    )
}
```

**FIND (around line 621):**
```kotlin
LaunchedEffect(Unit) {
    navController.navigate(Screen.MyOrders.route) {
        popUpTo(Screen.SellerOrders.route) { inclusive = true }
    }
}
```

**REPLACE WITH:**
```kotlin
LaunchedEffect(Unit) {
    NavigationHelper.safeNavigateWithPopUp(
        navController = navController,
        route = Screen.MyOrders.route,
        popUpToRoute = Screen.SellerOrders.route,
        inclusive = true,
        tag = "SellerOrders"
    )
}
```

### 4. Replace notification navigation calls (around lines 835, 842, 910)

**FIND:**
```kotlin
navController.navigate(Screen.MyOrders.route)
```

**REPLACE WITH:**
```kotlin
NavigationHelper.safeNavigate(
    navController = navController,
    route = Screen.MyOrders.route,
    tag = "NotificationHandler"
)
```

## Files Created
✅ NavigationHelper.kt - Safe navigation utility
✅ HomeScreen.kt - Added error handling (already applied)

## Status
⏳ Manual changes needed in NavGraph.kt (file not directly accessible)

## Testing After Fix
1. Click "Orders" in bottom navigation
2. Verify no crashes occur
3. Test all navigation paths to MyOrdersScreen
4. Check logs for any navigation errors