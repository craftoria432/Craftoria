package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.layout.*
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                BottomNavItem(
                    label = item.label,
                    icon = item.icon,
                    isSelected = item.route == selectedRoute,
                    onClick = { onItemClick(item.route) }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Primary else TextSecondary
        )
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    )
}