package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.TextSecondary

/**
 * Unified Empty State Component
 * Enforces design system standards for empty list/search states
 * 
 * Professional Design Standards:
 * - Icon Circle: 88dp with Primary.copy(alpha=0.10f) background
 * - Icon: 44dp, Primary.copy(alpha=0.70f) color
 * - Title: 20sp Bold, TextPrimary (no underline, no extra formatting)
 * - Message: 14sp Normal, TextSecondary (no underline, no extra formatting)
 * - Padding: 40dp
 * - Vertical Gap: 24dp between elements
 * - Optional Action Button
 * - No text decoration or underlines
 * - Clean, minimal design matching professional e-commerce apps
 */
@Composable
fun EmptyStateComponent(
    icon: ImageVector = Icons.Default.Inventory2,
    title: String = "No Items Found",
    message: String = "Try adjusting your search or filters",
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSecondary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Container - 88dp circle with tinted background
            Surface(
                shape = CircleShape,
                color = com.gcuf.craftoria.ui.theme.Primary.copy(alpha = 0.10f),
                modifier = Modifier.size(88.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = com.gcuf.craftoria.ui.theme.Primary.copy(alpha = 0.70f),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title - 20sp Bold, no decoration
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                textDecoration = TextDecoration.None,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Message - 14sp Normal, professional light black color
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary.copy(alpha = 0.70f), // Professional light black
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                textDecoration = TextDecoration.None,
                modifier = Modifier.fillMaxWidth(0.90f)
            )

            // Optional Action Button
            if (actionButton != null) {
                Spacer(modifier = Modifier.height(24.dp))
                actionButton()
            }
        }
    }
}

/**
 * Predefined empty states for common scenarios
 */
object EmptyStates {
    @Composable
    fun NoProducts(onRetry: (() -> Unit)? = null) {
        EmptyStateComponent(
            icon = Icons.Default.Inventory2,
            title = "No Products Found",
            message = "No products match your search. Try different keywords or filters.",
            actionButton = if (onRetry != null) {
                {
                    CraftoriaButton(
                        text = "Try Again",
                        onClick = onRetry,
                        isSmall = true,
                        modifier = Modifier.width(140.dp)
                    )
                }
            } else null
        )
    }

    @Composable
    fun NoOrders() {
        EmptyStateComponent(
            icon = Icons.Default.ShoppingCart,
            title = "No Orders Yet",
            message = "You haven't placed any orders. Start shopping now!"
        )
    }

    @Composable
    fun NoPayments() {
        EmptyStateComponent(
            icon = Icons.Default.AttachMoney,
            title = "No Payments",
            message = "Your payment history will appear here"
        )
    }

    @Composable
    fun NoRefunds() {
        EmptyStateComponent(
            icon = Icons.Default.Undo,
            title = "No Refunds",
            message = "Your refund requests will appear here"
        )
    }

    @Composable
    fun NoMessages() {
        EmptyStateComponent(
            icon = Icons.Default.Mail,
            title = "No Messages",
            message = "Start a conversation with sellers"
        )
    }

    @Composable
    fun NoNotifications() {
        EmptyStateComponent(
            icon = Icons.Default.Notifications,
            title = "No Notifications",
            message = "You're all caught up!"
        )
    }

    @Composable
    fun NoWishlist(modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.FavoriteBorder,
            title = "Your Wishlist is Empty",
            message = "Tap the heart icon on any product to save it here for later",
            modifier = modifier
        )
    }

    @Composable
    fun EmptyCart(modifier: Modifier = Modifier, onContinueShopping: () -> Unit) {
        EmptyStateComponent(
            icon = Icons.Outlined.ShoppingCart,
            title = "Your Cart is Empty",
            message = "Discover beautiful handcrafted items and add them to your cart",
            modifier = modifier,
            actionButton = {
                CraftoriaButton(
                    text = "Continue Shopping",
                    onClick = onContinueShopping,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        )
    }

    @Composable
    fun SearchStart(modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.Search,
            title = "Start searching",
            message = "Find beautiful handicrafts from talented sellers",
            modifier = modifier
        )
    }

    @Composable
    fun NoPaymentsFiltered(filterName: String, modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.FilterList,
            title = "No ${filterName.lowercase()} payments found",
            message = "Try adjusting your filters or date range to see more payments",
            modifier = modifier
        )
    }

    @Composable
    fun NoPaymentsYet(modifier: Modifier = Modifier, forBuyer: Boolean = true) {
        EmptyStateComponent(
            icon = Icons.Default.AccountBalanceWallet,
            title = if (forBuyer) "No Payments Yet" else "No Earnings Yet",
            message = if (forBuyer) {
                "Your purchase payment history will appear here after you place orders"
            } else {
                "Your earnings from completed orders will appear here"
            },
            modifier = modifier
        )
    }

    @Composable
    fun NoSellerProducts(onAddProductClick: () -> Unit, modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.Inventory2,
            title = "No products yet",
            message = "Add your first product to start selling",
            modifier = modifier,
            actionButton = {
                CraftoriaButton(
                    text = "Add Product",
                    onClick = onAddProductClick,
                    modifier = Modifier.widthIn(min = 180.dp)
                )
            }
        )
    }

    @Composable
    fun NoCoSellerPayments(rangeLabel: String, hasFilter: Boolean, modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.AccountBalanceWallet,
            title = "No Payments Found",
            message = if (hasFilter) {
                "No payments match this filter for $rangeLabel"
            } else {
                "Store payments for $rangeLabel will appear here"
            },
            modifier = modifier
        )
    }

    @Composable
    fun NoSellerRefunds(filterName: String = "All", modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.Receipt,
            title = "No $filterName Refunds",
            message = "No refund requests yet",
            modifier = modifier
        )
    }

    @Composable
    fun NoSellerOrders(filterName: String = "All", modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.ShoppingBag,
            title = "No $filterName Orders",
            message = "No orders to display",
            modifier = modifier
        )
    }

    @Composable
    fun NoBuyerOrders(filterName: String = "All", modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.ShoppingCart,
            title = "No $filterName Orders",
            message = "You haven't placed any orders yet",
            modifier = modifier
        )
    }

    @Composable
    fun NoSearchResults(query: String = "", modifier: Modifier = Modifier) {
        EmptyStateComponent(
            icon = Icons.Default.Search,
            title = "No Results",
            message = if (query.isNotEmpty()) {
                "No results found for \"$query\". Try different keywords or filters."
            } else {
                "Try searching for products"
            },
            modifier = modifier
        )
    }

    @Composable
    fun NoStores() {
        EmptyStateComponent(
            icon = Icons.Default.Store,
            title = "No Stores",
            message = "No co-seller stores available"
        )
    }

    @Composable
    fun NoData(title: String = "No Data", message: String = "No data available") {
        EmptyStateComponent(
            icon = Icons.Default.Info,
            title = title,
            message = message
        )
    }
}
