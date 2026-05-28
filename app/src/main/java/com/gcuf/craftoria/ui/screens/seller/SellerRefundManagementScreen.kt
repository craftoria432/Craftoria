package com.gcuf.craftoria.ui.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.RefundRequest
import com.gcuf.craftoria.data.model.RefundStatus
import com.gcuf.craftoria.data.model.getRequestedAtLong
import com.gcuf.craftoria.data.repository.RefundRepository
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.ui.components.EmptyStateComponent
import com.gcuf.craftoria.ui.components.FilterTabRow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Filter definition
// ─────────────────────────────────────────────────────────────────────────────
private enum class RefundFilter(val label: String, val statuses: List<String>?) {
    ALL("All", null),
    PENDING("Pending", listOf(
        RefundStatus.REQUESTED.toString(),
        RefundStatus.UNDER_REVIEW.toString()
    )),
    APPROVED("Approved", listOf(
        RefundStatus.APPROVED_BY_SELLER.toString(),
        RefundStatus.APPROVED_BY_ADMIN.toString(),
        RefundStatus.PROCESSING.toString(),
        RefundStatus.COMPLETED.toString()
    )),
    REJECTED("Rejected", listOf(
        RefundStatus.REJECTED_BY_SELLER.toString(),
        RefundStatus.REJECTED_BY_ADMIN.toString(),
        RefundStatus.FAILED.toString(),
        RefundStatus.CANCELLED.toString()
    ));
}

private fun List<RefundRequest>.countFor(filter: RefundFilter): Int = when (filter) {
    RefundFilter.ALL -> size
    else -> count { r -> filter.statuses?.contains(r.status) == true }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRefundManagementScreen(
    onBackClick: () -> Unit,
    onRefundClick: (String) -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var allRefunds     by remember { mutableStateOf<List<RefundRequest>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(RefundFilter.PENDING) }
    var selectedFilterIndex by remember { mutableStateOf(1) } // PENDING is at index 1

    // ── Real-time listener ────────────────────────────────────────────────────
    // FIX: replaced toObject(RefundRequest::class.java) with RefundRepository.parseRefund()
    // toObject() crashes when Firestore stores Timestamp but the data class field is Long.
    DisposableEffect(currentUserId) {
        if (currentUserId.isEmpty()) {
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        val db = FirebaseFirestore.getInstance()
        val registration: ListenerRegistration = db.collection("refunds")
            .whereEqualTo("seller_id", currentUserId)
            .orderBy("requested_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                // ✅ FIX: parseRefund() handles Timestamp/Long/Map safely
                allRefunds = snapshot?.documents?.mapNotNull { doc ->
                    RefundRepository.parseRefund(doc)
                } ?: emptyList()

                isLoading = false
            }

        onDispose { registration.remove() }
    }

    // ── Derived filtered list ─────────────────────────────────────────────────
    val displayedRefunds by remember {
        derivedStateOf {
            when (selectedFilter) {
                RefundFilter.ALL -> allRefunds
                else -> allRefunds.filter { r ->
                    selectedFilter.statuses?.contains(r.status) == true
                }
            }
        }
    }

    val pendingCount by remember { derivedStateOf { allRefunds.countFor(RefundFilter.PENDING) } }

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "Refund Management",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = if (pendingCount > 0)
                                if (pendingCount == 1) "$pendingCount Pending Action" else "$pendingCount Pending Actions"
                            else "Manage buyer refund requests",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
                        )
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
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Filter Tabs ───────────────────────────────────────────────────
            // ✅ STANDARDIZED: Uses FilterTabRow with consistent styling (no count badges)
            val filterLabels = RefundFilter.entries.map { it.label }
            
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                FilterTabRow(
                    tabs = filterLabels,
                    selectedIndex = selectedFilterIndex,
                    onTabSelected = { index ->
                        selectedFilterIndex = index
                        selectedFilter = RefundFilter.entries[index]
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                )
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                displayedRefunds.isEmpty() -> {
                    // ✅ STANDARDIZED: Use unified EmptyStateComponent with consistent sizing and styling
                    val title = "No ${selectedFilter.label} Refunds"
                    val message = "No refund requests yet"
                    
                    EmptyStateComponent(
                        icon = Icons.Default.Receipt,
                        title = title,
                        message = message
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = displayedRefunds,
                            key = { it.id }
                        ) { refund ->
                            SellerRefundCard(
                                refund = refund,
                                onClick = { onRefundClick(refund.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Refund Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SellerRefundCard(
    refund: RefundRequest,
    onClick: () -> Unit
) {
    val status = remember(refund.status) {
        runCatching { RefundStatus.valueOf(refund.status.uppercase()) }
            .getOrDefault(RefundStatus.REQUESTED)
    }

    // ── Derive whether this is a seller-initiated refund ──────────────────────
    // Seller-initiated refunds show a different label — "Initiated by you" —
    // so the seller understands it's their own request waiting on admin.
    val isSellerInitiated = refund.initiatedBy == "seller"

    val statusColor = when (status) {
        RefundStatus.REQUESTED,
        RefundStatus.UNDER_REVIEW         -> Warning
        RefundStatus.APPROVED_BY_SELLER,
        RefundStatus.APPROVED_BY_ADMIN,
        RefundStatus.PROCESSING           -> Color(0xFF2196F3)
        RefundStatus.COMPLETED            -> Success
        RefundStatus.REJECTED_BY_SELLER,
        RefundStatus.REJECTED_BY_ADMIN,
        RefundStatus.FAILED               -> Error
        RefundStatus.CANCELLED            -> TextSecondary
    }

    val statusIcon = when (status) {
        RefundStatus.REQUESTED,
        RefundStatus.UNDER_REVIEW         -> Icons.Default.HourglassEmpty
        RefundStatus.APPROVED_BY_SELLER,
        RefundStatus.APPROVED_BY_ADMIN    -> Icons.Default.CheckCircleOutline
        RefundStatus.PROCESSING           -> Icons.Default.Sync
        RefundStatus.COMPLETED            -> Icons.Default.CheckCircle
        RefundStatus.REJECTED_BY_SELLER,
        RefundStatus.REJECTED_BY_ADMIN    -> Icons.Default.Cancel
        RefundStatus.FAILED               -> Icons.Default.Error
        RefundStatus.CANCELLED            -> Icons.Default.Cancel
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Card header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(statusColor.copy(alpha = 0.08f), statusColor.copy(alpha = 0.02f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(statusColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = status.getDisplayName(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                        Text(
                            text = formatSellerRefundDate(refund.getRequestedAtLong()),
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Right-side badge
                when {
                    // Buyer-initiated pending → action needed from seller
                    status == RefundStatus.REQUESTED && !isSellerInitiated -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Warning.copy(alpha = 0.10f),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "Action needed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Warning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    // Seller-initiated → pending admin review
                    isSellerInitiated && status == RefundStatus.REQUESTED -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2196F3).copy(alpha = 0.10f),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "Awaiting admin",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // ── Card body ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Primary.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                // Seller-initiated shows a store icon instead of person
                                imageVector = if (isSellerInitiated) Icons.Default.Store
                                else Icons.Default.Person,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isSellerInitiated) "You initiated this refund"
                                else refund.buyerName.ifEmpty { "Unknown Buyer" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSellerInitiated) Color(0xFF2196F3) else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Order #${refund.orderId.take(8).uppercase()}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "PKR ${refund.refundAmount.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 1.dp)
                    )
                    Text(
                        text = refund.reasonDetails.ifEmpty {
                            refund.reason.replace("_", " ").lowercase()
                                .replaceFirstChar { it.titlecase() }
                        },
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun formatSellerRefundDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))