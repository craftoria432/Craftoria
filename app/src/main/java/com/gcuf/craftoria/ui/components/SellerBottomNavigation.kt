package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AddCircle
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

data class SellerNavItem(
    val label: String,
    val icon: String,
    val route: String
)

@Composable
fun SellerBottomNavigation(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    newOrdersCount: Int = 0,
    pendingNegotiationsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Primary,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0.dp),
        modifier = modifier
    ) {
        // Dashboard
        NavigationBarItem(
            selected = selectedRoute == "seller_dashboard",
            onClick = { onNavigate("seller_dashboard") },
            icon = {
                Icon(
                    imageVector = if (selectedRoute == "seller_dashboard")
                        Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Dashboard"
                )
            },
            label = {
                Text(
                    text = "Dashboard",
                    fontSize = 11.sp,
                    fontWeight = if (selectedRoute == "seller_dashboard") FontWeight.Bold else FontWeight.Normal
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

        // Add Product
        NavigationBarItem(
            selected = selectedRoute == "add_product",
            onClick = { onNavigate("add_product") },
            icon = {
                Icon(
                    imageVector = if (selectedRoute == "add_product")
                        Icons.Filled.Add else Icons.Outlined.AddCircle,
                    contentDescription = "Add Product"
                )
            },
            label = {
                Text(
                    text = "Add New",
                    fontSize = 11.sp,
                    fontWeight = if (selectedRoute == "add_product") FontWeight.Bold else FontWeight.Normal
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

        // Orders with professional pulsing badge
        NavigationBarItem(
            selected = selectedRoute == "orders",
            onClick = { onNavigate("orders") },
            icon = {
                Box {
                    Icon(
                        imageVector = if (selectedRoute == "orders")
                            Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                        contentDescription = "Orders"
                    )
                    
                    // Professional seller orders badge with pulsing animation
                    if (newOrdersCount > 0) {
                        com.gcuf.craftoria.utils.CustomBadge(
                            count = newOrdersCount,
                            color = Color(0xFFFF5722), // Deep Orange for seller orders
                            shouldPulse = true,
                            priority = com.gcuf.craftoria.utils.BadgeManager.BadgePriority.HIGH,
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

        // Profile — no badge per requirements
        NavigationBarItem(
            selected = selectedRoute == "profile",
            onClick = { onNavigate("profile") },
            icon = {
                Icon(
                    imageVector = if (selectedRoute == "profile")
                        Icons.Filled.Person else Icons.Outlined.Person,
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