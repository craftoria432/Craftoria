package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextSecondary
import com.gcuf.craftoria.utils.BadgeManager
import com.gcuf.craftoria.utils.CustomBadge

data class NavItem(
    val label: String,
    val icon: String,
    val route: String
)

@Composable
fun BottomNavigationBar(
    items: List<NavItem>,
    selectedRoute: String,
    onItemClick: (String) -> Unit,
    cartCount: Int = 0,
    wishlistCount: Int = 0,
    pendingOrdersCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Primary,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0.dp),
        modifier = modifier
    ) {
        // Home
        NavigationBarItem(
            selected = selectedRoute == "home",
            onClick = { onItemClick("home") },
            icon = {
                Icon(
                    imageVector = if (selectedRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = {
                Text(
                    text = "Home",
                    fontSize = 11.sp,
                    fontWeight = if (selectedRoute == "home") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Primary.copy(alpha = 0.10f)
            )
        )

        // Orders with badge
        NavigationBarItem(
            selected = selectedRoute == "orders",
            onClick = { onItemClick("orders") },
            icon = {
                Box {
                    Icon(
                        imageVector = if (selectedRoute == "orders") Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                        contentDescription = "Orders"
                    )
                    if (pendingOrdersCount > 0) {
                        CustomBadge(
                            count = pendingOrdersCount,
                            color = Color(0xFFFF9800),
                            shouldPulse = true,
                            priority = BadgeManager.BadgePriority.HIGH,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                        )
                    }
                }
            },
            label = {
                Text(
                    text = "Orders",
                    fontSize = 11.sp,
                    fontWeight = if (selectedRoute == "orders") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Primary.copy(alpha = 0.10f)
            )
        )

        // Wishlist with badge
        NavigationBarItem(
            selected = selectedRoute == "wishlist",
            onClick = { onItemClick("wishlist") },
            icon = {
                Box {
                    Icon(
                        imageVector = if (selectedRoute == "wishlist") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist"
                    )
                    if (wishlistCount > 0) {
                        CustomBadge(
                            count = wishlistCount,
                            color = Color(0xFFE91E63),
                            shouldPulse = false,
                            priority = BadgeManager.BadgePriority.LOW,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                        )
                    }
                }
            },
            label = {
                Text(
                    text = "Wishlist",
                    fontSize = 11.sp,
                    fontWeight = if (selectedRoute == "wishlist") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Primary.copy(alpha = 0.10f)
            )
        )

        // Profile
        NavigationBarItem(
            selected = selectedRoute == "profile",
            onClick = { onItemClick("profile") },
            icon = {
                Icon(
                    imageVector = if (selectedRoute == "profile") Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text(
                    text = "Profile",
                    fontSize = 11.sp,
                    fontWeight = if (selectedRoute == "profile") FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Primary,
                selectedTextColor = Primary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Primary.copy(alpha = 0.10f)
            )
        )
    }
}