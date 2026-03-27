package com.gcuf.craftoria.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Filled.Home, "Home")
    object Categories : BottomNavItem("categories", Icons.Filled.List, "Categories")
    object Cart : BottomNavItem("cart", Icons.Filled.ShoppingCart, "Cart")
    object Profile : BottomNavItem("profile", Icons.Filled.Person, "Profile")
}
