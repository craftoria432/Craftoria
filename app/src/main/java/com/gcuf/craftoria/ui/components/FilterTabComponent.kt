package com.gcuf.craftoria.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.gcuf.craftoria.data.model.NotificationCategory
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.ui.theme.BorderColor
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.TextSecondary

/**
 * Unified Filter Tab Component
 * Enforces design system standards:
 * - Height: 36dp (compact)
 * - Padding: 12dp horizontal, 6dp vertical
 * - Font: 12sp Medium
 * - Border Radius: 20dp (pill-shaped)
 * - Active: Primary background, white text
 * - Inactive: White background, TextSecondary text, BorderColor border
 * - Gap: 8dp between tabs
 * - Smooth animation on selection
 * - Professional sizing: Not too long, not too short
 * - Min width: 60dp, Max width: 140dp for optimal readability
 *
 * Fix: onClick moved to Surface (proper ripple + single tap target).
 * Fix: semantics on Surface — no layered accessibility targets.
 */

@Composable
fun FilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color.White,
        label = "FilterTabBackground"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextSecondary,
        label = "FilterTabText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Primary else BorderColor,
        label = "FilterTabBorder"
    )

    // onClick is on Surface so it gets the correct Material ripple and a single
    // accessibility tap target — no separate clickable on the inner Box.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, borderColor),
        modifier = modifier
            .height(36.dp)
            .widthIn(min = 60.dp, max = 140.dp)
            .semantics {
                role = Role.Tab
                selected = isSelected
                contentDescription = if (isSelected) "$label, selected" else label
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    lineHeight = 16.sp
                )
                if (badgeCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color.White.copy(alpha = 0.30f) else Primary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            if (isSelected) Color.White.copy(alpha = 0.40f) else Primary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontal scrollable filter tabs
 */
@Composable
fun FilterTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    badgeCounts: List<Int>? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            FilterTab(
                label = tab,
                isSelected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                badgeCount = badgeCounts?.getOrNull(index) ?: 0
            )
        }
    }
}

/**
 * Vertical filter tabs (for sidebar/drawer)
 */
@Composable
fun FilterTabColumn(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            FilterTab(
                label = tab,
                isSelected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Predefined filter tab sets for common scenarios
 */
object FilterTabs {
    val ORDER_STATUS = listOf(
        "All", "Pending", "Processing", "Shipped", "Delivered", "Cancelled"
    )
    val PAYMENT_STATUS = listOf(
        "All", "Pending", "Completed", "Failed"
    )
    val REFUND_STATUS = listOf(
        "All", "Pending", "Approved", "Rejected", "Completed"
    )
    val NOTIFICATION_FILTER = listOf(
        "All", "Orders", "Refunds", "Promotions", "Unread"
    )
    val PRODUCT_STATUS = listOf(
        "All", "Active", "Pending", "Rejected"
    )
    val STORE_STATUS = listOf(
        "All", "Active", "Inactive"
    )
    val CATEGORY = listOf(
        "All", "Textiles", "Jewelry", "Home Décor", "Embroidery", "Pottery"
    )
    val PRICE_RANGE = listOf(
        "All", "Under 1000", "1000-5000", "5000-10000", "10000+"
    )
    val RATING = listOf(
        "All", "5 Stars", "4+ Stars", "3+ Stars", "2+ Stars"
    )
}

/**
 * Payment history filter tabs — always shows all statuses (even when list is empty).
 */
@Composable
fun PaymentStatusFilterTabs(
    selectedStatus: PaymentStatus?,
    onFilterSelected: (PaymentStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusEntries = PaymentStatus.entries
    val tabLabels = listOf("All") + statusEntries.map { it.getDisplayName() }
    val selectedIndex = when (selectedStatus) {
        null -> 0
        else -> statusEntries.indexOf(selectedStatus).let { if (it >= 0) it + 1 else 0 }
    }

    Column(modifier = modifier.fillMaxWidth().background(Color.White)) {
        FilterTabRow(
            tabs = tabLabels,
            selectedIndex = selectedIndex,
            onTabSelected = { index ->
                if (index == 0) onFilterSelected(null)
                else onFilterSelected(statusEntries[index - 1])
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

/**
 * Notification filter tabs — role-aware (buyer vs seller categories).
 */
@Composable
fun NotificationCategoryFilterTabs(
    currentFilter: NotificationCategory,
    onFilterSelected: (NotificationCategory) -> Unit,
    userRole: String = "buyer",
    modifier: Modifier = Modifier
) {
    val buyerFilters = listOf(
        NotificationCategory.UNREAD to "Unread",
        NotificationCategory.ALL to "All",
        NotificationCategory.ORDERS to "Orders",
        NotificationCategory.PAYMENTS to "Payments",
        NotificationCategory.REFUNDS to "Refunds",
        NotificationCategory.MESSAGES to "Messages",
        NotificationCategory.PROMOTIONS to "Promotions",
        NotificationCategory.SYSTEM to "System"
    )
    val sellerFilters = listOf(
        NotificationCategory.UNREAD to "Unread",
        NotificationCategory.ALL to "All",
        NotificationCategory.ORDERS to "Orders",
        NotificationCategory.PAYMENTS to "Payments",
        NotificationCategory.REFUNDS to "Refunds",
        NotificationCategory.MESSAGES to "Messages",
        NotificationCategory.STORE_RATING to "Store Rating",
        NotificationCategory.SYSTEM to "System",
        NotificationCategory.REPORT to "Reports"
    )
    val filters = if (userRole == "seller") sellerFilters else buyerFilters
    val selectedIndex = filters.indexOfFirst { it.first == currentFilter }.coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth().background(Color.White)) {
        FilterTabRow(
            tabs = filters.map { it.second },
            selectedIndex = selectedIndex,
            onTabSelected = { index -> onFilterSelected(filters[index].first) },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

/**
 * Co-seller store payment filter tabs — always visible during loading.
 */
@Composable
fun CoSellerPaymentFilterTabs(
    selectedStatus: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("all" to "All", "pending" to "Pending", "completed" to "Completed")
    val selectedIndex = options.indexOfFirst { it.first == selectedStatus }.coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth().background(Color.White)) {
        FilterTabRow(
            tabs = options.map { it.second },
            selectedIndex = selectedIndex,
            onTabSelected = { index -> onFilterSelected(options[index].first) },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}