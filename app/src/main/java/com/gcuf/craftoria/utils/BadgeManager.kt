package com.gcuf.craftoria.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.Notification
import com.gcuf.craftoria.viewmodel.CartViewModel
import com.gcuf.craftoria.viewmodel.OrderViewModel
import com.gcuf.craftoria.viewmodel.UnreadMessageViewModel
import com.gcuf.craftoria.viewmodel.WishlistViewModel
import com.gcuf.craftoria.viewmodel.NotificationViewModel

/**
 * Enhanced Badge Manager with pulsing animations and priority-based styling
 */
object BadgeManager {
    
    // Badge animation states
    enum class BadgeAnimationState {
        STATIC,
        PULSING,
        URGENT_PULSING
    }
    
    // Badge priority levels
    enum class BadgePriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }
    
    // Badge configuration data class
    data class BadgeConfig(
        val count: Int,
        val priority: BadgePriority,
        val animationState: BadgeAnimationState,
        val color: Color,
        val shouldPulse: Boolean = false,
        val pulseStartTime: Long = 0L
    )
    
    /**
     * Get cart items count for buyer
     */
    @Composable
    fun getCartCount(): Int {
        val cartViewModel: CartViewModel = viewModel()
        val cartCount by cartViewModel.cartCount.collectAsState()
        return cartCount
    }
    
    /**
     * Get wishlist items count for buyer
     */
    @Composable
    fun getWishlistCount(): Int {
        val wishlistViewModel: WishlistViewModel = viewModel()
        val wishlistCount by wishlistViewModel.wishlistCount.collectAsState()
        
        // Debug logging to help identify issues
        LaunchedEffect(wishlistCount) {
            android.util.Log.d("BadgeManager", "Wishlist count updated: $wishlistCount")
        }
        
        return wishlistCount
    }
    
    /**
     * Get pending orders count for buyer (orders that need attention)
     */
    @Composable
    fun getBuyerPendingOrdersCount(): Int {
        val orderViewModel: OrderViewModel = viewModel()
        val orders by orderViewModel.orders.collectAsState()
        
        // Count orders that are pending or processing (need buyer attention)
        return orders.count { order ->
            order.status in listOf("pending", "processing", "shipped")
        }
    }
    
    /**
     * Get new orders count for seller (orders that need seller action)
     * This should be called with proper seller context
     */
    @Composable
    fun getSellerNewOrdersCount(): Int {
        // This now uses SellerOrdersViewModel for accurate seller-specific order count
        // The count is managed by SellerOrdersViewModel.loadNewOrdersCount()
        // which queries orders with status "pending" or "confirmed" and isViewed = false
        return 0 // Default - should be overridden by SellerOrdersViewModel
    }
    
    /**
     * Get unread messages count
     */
    @Composable
    fun getUnreadMessagesCount(): Int {
        val unreadViewModel: UnreadMessageViewModel = viewModel()
        val unreadCount by unreadViewModel.unreadCount.collectAsState()
        return unreadCount
    }
    
    /**
     * Get pending negotiations count for seller
     */
    @Composable
    fun getPendingNegotiationsCount(): Int {
        // This would connect to a NegotiationViewModel when implemented
        // For now, return 0
        return 0
    }

    /**
     * Get unread notifications count
     */
    @Composable
    fun getUnreadNotificationsCount(): Int {
        // Use NotificationViewModel for real-time badge count
        val notificationViewModel: NotificationViewModel = viewModel()
        val unreadCount by notificationViewModel.unreadCount.collectAsState()
        return unreadCount
    }

    /**
     * Get seller pending products count (products awaiting approval)
     */
    @Composable
    fun getSellerPendingProductsCount(): Int {
        // This would connect to ProductViewModel for seller's pending products
        // For now, return 0 - implement when needed
        return 0
    }

    /**
     * Get seller payment notifications count
     */
    @Composable
    fun getSellerPaymentNotificationsCount(): Int {
        // This would connect to PaymentViewModel for payment-related notifications
        // For now, return 0 - implement when needed
        return 0
    }
    
    // ============================================================================
    // ENHANCED BADGE CONFIGURATION METHODS
    // ============================================================================
    
    @Composable
    fun getNotificationBadgeConfig(): BadgeConfig {
        val notificationViewModel: NotificationViewModel = viewModel()
        val notifications by notificationViewModel.notifications.collectAsState()
        val unreadNotifications = notifications.filter { !it.isRead }
        val count = unreadNotifications.size
        
        if (count == 0) {
            return BadgeConfig(
                count = 0,
                priority = BadgePriority.LOW,
                animationState = BadgeAnimationState.STATIC,
                color = Color.Transparent
            )
        }
        
        // Determine priority based on notification types and recency
        val priority = determineNotificationPriority(unreadNotifications)
        val shouldPulse = shouldShowPulseAnimation(unreadNotifications)
        
        return BadgeConfig(
            count = count,
            priority = priority,
            animationState = if (shouldPulse) {
                if (priority == BadgePriority.URGENT) BadgeAnimationState.URGENT_PULSING
                else BadgeAnimationState.PULSING
            } else BadgeAnimationState.STATIC,
            color = getBadgeColor(priority),
            shouldPulse = shouldPulse,
            pulseStartTime = System.currentTimeMillis()
        )
    }
    
    @Composable
    fun getCartBadgeConfig(): BadgeConfig {
        val count = getCartCount()
        return BadgeConfig(
            count = count,
            priority = if (count > 0) BadgePriority.MEDIUM else BadgePriority.LOW,
            animationState = BadgeAnimationState.STATIC,
            color = Color(0xFF4CAF50) // Green for cart
        )
    }
    
    @Composable
    fun getWishlistBadgeConfig(): BadgeConfig {
        val count = getWishlistCount()
        return BadgeConfig(
            count = count,
            priority = BadgePriority.LOW,
            animationState = BadgeAnimationState.STATIC,
            color = Color(0xFFE91E63) // Pink for wishlist
        )
    }
    
    @Composable
    fun getOrdersBadgeConfig(): BadgeConfig {
        val count = getBuyerPendingOrdersCount()
        val shouldPulse = count > 0 // Pulse for pending orders
        
        return BadgeConfig(
            count = count,
            priority = if (count > 0) BadgePriority.HIGH else BadgePriority.LOW,
            animationState = if (shouldPulse) BadgeAnimationState.PULSING else BadgeAnimationState.STATIC,
            color = Color(0xFFFF9800) // Orange for orders
        )
    }
    
    @Composable
    fun getMessagesBadgeConfig(): BadgeConfig {
        val count = getUnreadMessagesCount()
        val shouldPulse = count > 0 // Pulse for unread messages
        
        return BadgeConfig(
            count = count,
            priority = if (count > 0) BadgePriority.MEDIUM else BadgePriority.LOW,
            animationState = if (shouldPulse) BadgeAnimationState.PULSING else BadgeAnimationState.STATIC,
            color = Color(0xFF2196F3) // Blue for messages
        )
    }

    @Composable
    fun getSellerOrdersBadgeConfig(): BadgeConfig {
        val count = getSellerNewOrdersCount()
        val shouldPulse = count > 0 // Pulse for new orders
        
        return BadgeConfig(
            count = count,
            priority = if (count > 0) BadgePriority.HIGH else BadgePriority.LOW,
            animationState = if (shouldPulse) BadgeAnimationState.PULSING else BadgeAnimationState.STATIC,
            color = Color(0xFFFF5722) // Deep Orange for seller orders
        )
    }

    @Composable
    fun getSellerProductsBadgeConfig(): BadgeConfig {
        val count = getSellerPendingProductsCount()
        
        return BadgeConfig(
            count = count,
            priority = if (count > 0) BadgePriority.MEDIUM else BadgePriority.LOW,
            animationState = BadgeAnimationState.STATIC,
            color = Color(0xFF9C27B0) // Purple for products
        )
    }

    @Composable
    fun getSellerPaymentsBadgeConfig(): BadgeConfig {
        val count = getSellerPaymentNotificationsCount()
        val shouldPulse = count > 0 // Pulse for payment notifications
        
        return BadgeConfig(
            count = count,
            priority = if (count > 0) BadgePriority.HIGH else BadgePriority.LOW,
            animationState = if (shouldPulse) BadgeAnimationState.PULSING else BadgeAnimationState.STATIC,
            color = Color(0xFF4CAF50) // Green for payments
        )
    }
    
    private fun determineNotificationPriority(notifications: List<Notification>): BadgePriority {
        val now = System.currentTimeMillis()
        val recentThreshold = 5 * 60 * 1000L // 5 minutes
        
        // Check for urgent notifications (orders, payments, admin actions)
        val hasUrgent = notifications.any { notification ->
            val isRecent = (now - notification.createdAt) < recentThreshold
            val isUrgentType = when (notification.category.lowercase()) {
                "order", "payment", "admin_message", "system_error" -> true
                "negotiation" -> notification.actionData?.get("priority") == "high"
                else -> false
            }
            isRecent && isUrgentType
        }
        
        if (hasUrgent) return BadgePriority.URGENT
        
        // Check for high priority notifications
        val hasHigh = notifications.any { notification ->
            when (notification.category.lowercase()) {
                "order", "payment", "negotiation" -> true
                "system" -> notification.actionData?.get("priority") == "high"
                else -> false
            }
        }
        
        if (hasHigh) return BadgePriority.HIGH
        
        // Check for medium priority
        val hasMedium = notifications.any { notification ->
            when (notification.category.lowercase()) {
                "message", "shipping", "product" -> true
                else -> false
            }
        }
        
        return if (hasMedium) BadgePriority.MEDIUM else BadgePriority.LOW
    }
    
    private fun shouldShowPulseAnimation(notifications: List<Notification>): Boolean {
        val now = System.currentTimeMillis()
        val pulseThreshold = 30 * 1000L // 30 seconds
        
        return notifications.any { notification ->
            (now - notification.createdAt) < pulseThreshold
        }
    }
    
    private fun getBadgeColor(priority: BadgePriority): Color {
        return when (priority) {
            BadgePriority.URGENT -> Color(0xFFD32F2F) // Red
            BadgePriority.HIGH -> Color(0xFFFF5722) // Deep Orange
            BadgePriority.MEDIUM -> Color(0xFFFF9800) // Orange
            BadgePriority.LOW -> Color(0xFF2196F3) // Blue
        }
    }
}

/**
 * Professional Badge Component with pulsing animation
 */
@Composable
fun ProfessionalBadge(
    config: BadgeManager.BadgeConfig,
    modifier: Modifier = Modifier
) {
    if (config.count <= 0) return
    
    // Animation for pulsing effect
    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (config.animationState == BadgeManager.BadgeAnimationState.URGENT_PULSING) 1.3f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (config.animationState == BadgeManager.BadgeAnimationState.URGENT_PULSING) 800 else 1200,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (config.animationState == BadgeManager.BadgeAnimationState.URGENT_PULSING) 800 else 1200,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    // Apply animation only if pulsing
    val scale = when (config.animationState) {
        BadgeManager.BadgeAnimationState.PULSING,
        BadgeManager.BadgeAnimationState.URGENT_PULSING -> pulseScale
        else -> 1f
    }
    
    val alpha = when (config.animationState) {
        BadgeManager.BadgeAnimationState.PULSING,
        BadgeManager.BadgeAnimationState.URGENT_PULSING -> pulseAlpha
        else -> 1f
    }
    
    Box(
        modifier = modifier
            .scale(scale)
            .size(
                width = if (config.count > 99) 28.dp else if (config.count > 9) 24.dp else 20.dp,
                height = 20.dp
            )
            .clip(CircleShape)
            .background(config.color.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (config.count > 99) "99+" else config.count.toString(),
            color = Color.White,
            fontSize = if (config.count > 99) 10.sp else if (config.count > 9) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Notification Badge with automatic pulsing
 */
@Composable
fun NotificationBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getNotificationBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Cart Badge
 */
@Composable
fun CartBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getCartBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Wishlist Badge
 */
@Composable
fun WishlistBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getWishlistBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Orders Badge
 */
@Composable
fun OrdersBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getOrdersBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Messages Badge
 */
@Composable
fun MessagesBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getMessagesBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Seller Orders Badge
 */
@Composable
fun SellerOrdersBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getSellerOrdersBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Seller Products Badge
 */
@Composable
fun SellerProductsBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getSellerProductsBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Seller Payments Badge
 */
@Composable
fun SellerPaymentsBadge(
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.getSellerPaymentsBadgeConfig()
    ProfessionalBadge(config = config, modifier = modifier)
}

/**
 * Generic Badge with custom configuration
 */
@Composable
fun CustomBadge(
    count: Int,
    color: Color,
    shouldPulse: Boolean = false,
    priority: BadgeManager.BadgePriority = BadgeManager.BadgePriority.MEDIUM,
    modifier: Modifier = Modifier
) {
    val config = BadgeManager.BadgeConfig(
        count = count,
        priority = priority,
        animationState = if (shouldPulse) {
            if (priority == BadgeManager.BadgePriority.URGENT) BadgeManager.BadgeAnimationState.URGENT_PULSING
            else BadgeManager.BadgeAnimationState.PULSING
        } else BadgeManager.BadgeAnimationState.STATIC,
        color = color
    )
    ProfessionalBadge(config = config, modifier = modifier)
}
