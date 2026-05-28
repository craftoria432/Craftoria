package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.OrderStatus
import com.gcuf.craftoria.data.model.PaymentStatus
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.Success
import com.gcuf.craftoria.ui.theme.Warning
import com.gcuf.craftoria.ui.theme.Error
import com.gcuf.craftoria.ui.theme.Info

/**
 * Unified Badge Component System
 * Enforces design system standards:
 * - Height: 28dp (auto from padding)
 * - Padding: 10dp horizontal, 6dp vertical
 * - Font: 11sp SemiBold
 * - Border Radius: 20dp (pill shape)
 * - Consistent color palette
 * - Professional sizing across all screens
 */

// ── Status Badge ──────────────────────────────────────────────────────
@Composable
fun StatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING, OrderStatus.NEW -> 
            Pair(Color(0xFFFFF3CD), Color(0xFF856404))
        OrderStatus.PROCESSING, OrderStatus.CONFIRMED -> 
            Pair(Color(0xFFD1ECF1), Color(0xFF0C5460))
        OrderStatus.SHIPPED -> 
            Pair(Color(0xFFE2D5F3), Color(0xFF5A2D82))
        OrderStatus.DELIVERED, OrderStatus.COMPLETED -> 
            Pair(Color(0xFFD4EDDA), Color(0xFF155724))
        OrderStatus.CANCELLED -> 
            Pair(Color(0xFFF8D7DA), Color(0xFF721C24))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = status.getDisplayName(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// ── Product Active/Inactive Badge ─────────────────────────────────────
@Composable
fun ProductActiveBadge(isActive: Boolean, modifier: Modifier = Modifier) {
    StateBadge(
        label = if (isActive) "Active" else "Inactive",
        state = if (isActive) BadgeState.SUCCESS else BadgeState.DEFAULT,
        modifier = modifier
    )
}

// ── Payment Status Badge ──────────────────────────────────────────────
@Composable
fun PaymentStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val normalized = status.lowercase()
    val (backgroundColor, textColor, label) = when (normalized) {
        PaymentStatus.COMPLETED.toString() ->
            Triple(Color(0xFFD4EDDA), Color(0xFF155724), PaymentStatus.COMPLETED.getDisplayName())
        PaymentStatus.PENDING.toString() ->
            Triple(Color(0xFFFFF3CD), Color(0xFF856404), PaymentStatus.PENDING.getDisplayName())
        PaymentStatus.PROCESSING.toString() ->
            Triple(Color(0xFFD1ECF1), Color(0xFF0C5460), PaymentStatus.PROCESSING.getDisplayName())
        PaymentStatus.FAILED.toString() ->
            Triple(Color(0xFFF8D7DA), Color(0xFF721C24), PaymentStatus.FAILED.getDisplayName())
        PaymentStatus.REFUND_PENDING.toString() ->
            Triple(Color(0xFFFFF3CD), Color(0xFF856404), PaymentStatus.REFUND_PENDING.getDisplayName())
        PaymentStatus.REFUND_PROCESSING.toString() ->
            Triple(Color(0xFFD1ECF1), Color(0xFF0C5460), PaymentStatus.REFUND_PROCESSING.getDisplayName())
        PaymentStatus.REFUNDED.toString() ->
            Triple(Color(0xFFE2D5F3), Color(0xFF5A2D82), PaymentStatus.REFUNDED.getDisplayName())
        PaymentStatus.REFUND_REJECTED.toString() ->
            Triple(Color(0xFFE2E3E5), Color(0xFF383D41), PaymentStatus.REFUND_REJECTED.getDisplayName())
        else -> Triple(Color(0xFFE2E3E5), Color(0xFF383D41), status.replaceFirstChar { it.uppercase() })
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// Helper data class for Quadruple
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ── State Badge ───────────────────────────────────────────────────────
@Composable
fun StateBadge(
    label: String,
    state: BadgeState = BadgeState.DEFAULT,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (state) {
        BadgeState.SUCCESS -> Pair(Color(0xFFD4EDDA), Color(0xFF155724))
        BadgeState.WARNING -> Pair(Color(0xFFFFF3CD), Color(0xFF856404))
        BadgeState.ERROR -> Pair(Color(0xFFF8D7DA), Color(0xFF721C24))
        BadgeState.INFO -> Pair(Color(0xFFD1ECF1), Color(0xFF0C5460))
        BadgeState.DEFAULT -> Pair(Color(0xFFE2E3E5), Color(0xFF383D41))
        BadgeState.PRIMARY -> Pair(Color(0xFFE91E63).copy(alpha = 0.15f), Color(0xFFE91E63))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// ── Count Badge (for notifications, cart, etc.) ────────────────────────
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Primary,
    textColor: Color = Color.White
) {
    if (count <= 0) return

    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = modifier.size(20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 11.sp
            )
        }
    }
}

// ── Verification Badge ────────────────────────────────────────────────
@Composable
fun VerificationBadge(
    isVerified: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVerified) return

    Surface(
        color = Color(0xFFD4EDDA),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = "✓ Verified",
            fontSize = 8.sp,
            color = Color(0xFF155724),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            lineHeight = 10.sp
        )
    }
}

// ── Stock Badge ───────────────────────────────────────────────────────
@Composable
fun StockBadge(
    stock: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = if (stock > 0) {
        Pair(Color(0xFFD4EDDA), Color(0xFF155724))
    } else {
        Pair(Color(0xFFF8D7DA), Color(0xFF721C24))
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = if (stock > 0) "$stock in stock" else "Out of stock",
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            lineHeight = 11.sp
        )
    }
}

// ── Negotiable Badge ──────────────────────────────────────────────────
@Composable
fun NegotiableBadge(
    isNegotiable: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isNegotiable) return

    Surface(
        color = Color(0xFFD1ECF1),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = "Negotiable",
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0C5460),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            lineHeight = 11.sp
        )
    }
}

// ── Refund Status Badge ───────────────────────────────────────────────
@Composable
fun RefundStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "PENDING" -> Pair(Color(0xFFFFF3CD), Color(0xFF856404))
        "APPROVED" -> Pair(Color(0xFFD4EDDA), Color(0xFF155724))
        "REJECTED" -> Pair(Color(0xFFF8D7DA), Color(0xFF721C24))
        "COMPLETED" -> Pair(Color(0xFFD4EDDA), Color(0xFF155724))
        else -> Pair(Color(0xFFE2E3E5), Color(0xFF383D41))
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// ── Badge State Enum ──────────────────────────────────────────────────
enum class BadgeState {
    DEFAULT,
    PRIMARY,
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

// ── Badge Preset Styles ───────────────────────────────────────────────
object BadgeStyles {
    @Composable
    fun Success(label: String, modifier: Modifier = Modifier) {
        StateBadge(label, BadgeState.SUCCESS, modifier)
    }

    @Composable
    fun Warning(label: String, modifier: Modifier = Modifier) {
        StateBadge(label, BadgeState.WARNING, modifier)
    }

    @Composable
    fun Error(label: String, modifier: Modifier = Modifier) {
        StateBadge(label, BadgeState.ERROR, modifier)
    }

    @Composable
    fun Info(label: String, modifier: Modifier = Modifier) {
        StateBadge(label, BadgeState.INFO, modifier)
    }

    @Composable
    fun Primary(label: String, modifier: Modifier = Modifier) {
        StateBadge(label, BadgeState.PRIMARY, modifier)
    }
}
