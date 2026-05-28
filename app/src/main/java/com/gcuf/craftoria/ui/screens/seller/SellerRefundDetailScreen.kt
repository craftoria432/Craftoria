package com.gcuf.craftoria.ui.screens.seller

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.RefundViewModel
import com.gcuf.craftoria.viewmodel.RefundUiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRefundDetailScreen(
    refundId: String,
    onBackClick: () -> Unit,
    onContactBuyer: (String) -> Unit = {}
) {
    val viewModel: RefundViewModel = viewModel()
    val refundState by viewModel.refundState.collectAsState()
    val currentRefund by viewModel.currentRefund.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid ?: ""
    val currentUserName = currentUser?.displayName ?: "Seller"

    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReason by remember { mutableStateOf("") }
    var approvalNotes by remember { mutableStateOf("") }

    // Load refund details
    LaunchedEffect(refundId) {
        viewModel.getRefund(refundId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Refund Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
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
        when (refundState) {
            is RefundUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            is RefundUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Error.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Error.copy(alpha = 0.70f),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Text(
                            text = (refundState as RefundUiState.Error).message,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text("Go Back", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }

            else -> {
                currentRefund?.let { refund ->
                    val status = refund.getStatusEnum()
                    val isPending = status == RefundStatus.REQUESTED

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status Card
                        RefundStatusHeaderCard(refund = refund)

                        // Order Information
                        RefundOrderInfoCard(refund = refund)

                        // Buyer Information
                        RefundBuyerInfoCard(
                            refund = refund,
                            onContactBuyer = { onContactBuyer(refund.buyerId) }
                        )

                        // Refund Details
                        RefundDetailsCard(refund = refund)

                        // Action Buttons (only for pending refunds)
                        if (isPending) {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Approve Button
                            Button(
                                onClick = { showApproveDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        Brush.horizontalGradient(listOf(Success, Success.copy(alpha = 0.8f))),
                                        RoundedCornerShape(14.dp)
                                    ),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Approve Refund",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            // Reject Button
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Error
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Reject Refund",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Approve Dialog
                    if (showApproveDialog) {
                        AlertDialog(
                            onDismissRequest = { showApproveDialog = false },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Success.copy(alpha = 0.10f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Approve Refund?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "PKR ${refund.refundAmount.toInt()} will be refunded to ${refund.buyerName}. This action cannot be undone.",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                    StandardizedOutlinedTextField(
                                        value = approvalNotes,
                                        onValueChange = { approvalNotes = it },
                                        label = "",
                                        placeholder = "Add notes (optional)",
                                        minLines = 2,
                                        minHeight = 80,
                                        showLabel = false
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.approveRefund(
                                                refundId = refund.id,
                                                approvedBy = currentUserId,
                                                approverName = currentUserName,
                                                approvalNotes = approvalNotes.ifEmpty { "Approved by seller" }
                                            )
                                            showApproveDialog = false
                                            approvalNotes = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Approve", fontWeight = FontWeight.SemiBold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showApproveDialog = false }) {
                                    Text("Cancel", color = TextSecondary)
                                }
                            }
                        )
                    }

                    // Reject Dialog
                    if (showRejectDialog) {
                        AlertDialog(
                            onDismissRequest = { showRejectDialog = false },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Error.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = Error,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Reject Refund?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Please provide a reason for rejecting this refund request. The buyer will be notified.",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                    StandardizedOutlinedTextField(
                                        value = rejectionReason,
                                        onValueChange = { rejectionReason = it },
                                        label = "",
                                        placeholder = "Rejection reason *",
                                        minLines = 3,
                                        minHeight = 100,
                                        showLabel = false
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (rejectionReason.isNotBlank()) {
                                            coroutineScope.launch {
                                                viewModel.rejectRefund(
                                                    refundId = refund.id,
                                                    rejectedBy = currentUserId,
                                                    rejectorName = currentUserName,
                                                    rejectionReason = rejectionReason
                                                )
                                                showRejectDialog = false
                                                rejectionReason = ""
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = rejectionReason.isNotBlank()
                                ) {
                                    Text("Reject", fontWeight = FontWeight.SemiBold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRejectDialog = false }) {
                                    Text("Cancel", color = TextSecondary)
                                }
                            }
                        )
                    }

                    // Success/Error Snackbar
                    LaunchedEffect(refundState) {
                        when (refundState) {
                            is RefundUiState.RefundApproved -> {
                                // Show success and go back
                                kotlinx.coroutines.delay(500)
                                onBackClick()
                            }
                            is RefundUiState.RefundRejected -> {
                                // Show success and go back
                                kotlinx.coroutines.delay(500)
                                onBackClick()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RefundStatusHeaderCard(refund: RefundRequest) {
    val status = refund.getStatusEnum()
    val statusColor = when (status) {
        RefundStatus.REQUESTED -> Warning
        RefundStatus.UNDER_REVIEW -> Warning
        RefundStatus.APPROVED_BY_SELLER -> Color(0xFF2196F3)
        RefundStatus.APPROVED_BY_ADMIN -> Color(0xFF2196F3)
        RefundStatus.PROCESSING -> Color(0xFF2196F3)
        RefundStatus.COMPLETED -> Success
        RefundStatus.REJECTED_BY_SELLER -> Error
        RefundStatus.REJECTED_BY_ADMIN -> Error
        RefundStatus.FAILED -> Error
        RefundStatus.CANCELLED -> TextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(statusColor.copy(alpha = 0.10f), statusColor.copy(alpha = 0.03f))
                    )
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (status) {
                        RefundStatus.REQUESTED -> Icons.Default.HourglassEmpty
                        RefundStatus.UNDER_REVIEW -> Icons.Default.HourglassEmpty
                        RefundStatus.APPROVED_BY_SELLER -> Icons.Default.CheckCircleOutline
                        RefundStatus.APPROVED_BY_ADMIN -> Icons.Default.CheckCircleOutline
                        RefundStatus.PROCESSING -> Icons.Default.Sync
                        RefundStatus.COMPLETED -> Icons.Default.CheckCircle
                        RefundStatus.REJECTED_BY_SELLER -> Icons.Default.Cancel
                        RefundStatus.REJECTED_BY_ADMIN -> Icons.Default.Cancel
                        RefundStatus.FAILED -> Icons.Default.Error
                        RefundStatus.CANCELLED -> Icons.Default.Cancel
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.getDisplayName(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = "Requested ${formatTimeAgo(refund.getRequestedAtLong())}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RefundOrderInfoCard(refund: RefundRequest) {
    SectionCard(
        title = "Order Information",
        icon = Icons.Default.Receipt
    ) {
        DetailRow(label = "Order ID", value = "#${refund.orderId.take(8).uppercase()}")
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        DetailRow(
            label = "Refund Amount",
            value = "PKR ${refund.refundAmount.toInt()}",
            valueColor = Primary,
            valueFontWeight = FontWeight.Bold
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        DetailRow(label = "Original Amount", value = "PKR ${refund.originalAmount.toInt()}")
    }
}

@Composable
private fun RefundBuyerInfoCard(
    refund: RefundRequest,
    onContactBuyer: () -> Unit
) {
    SectionCard(
        title = "Buyer Information",
        icon = Icons.Default.Person
    ) {
        DetailRow(label = "Name", value = refund.buyerName)
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        DetailRow(label = "Buyer ID", value = refund.buyerId.take(12))
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onContactBuyer,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Contact Buyer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RefundDetailsCard(refund: RefundRequest) {
    SectionCard(
        title = "Refund Details",
        icon = Icons.Default.Info
    ) {
        DetailRow(label = "Reason", value = refund.reason.replace("_", " ").lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) })
        if (refund.reasonDetails.isNotEmpty()) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Description",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = refund.reasonDetails,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        DetailRow(label = "Payment Method", value = refund.paymentMethod)
        if (refund.transactionId.isNotEmpty()) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            DetailRow(label = "Transaction ID", value = refund.transactionId)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Primary.copy(alpha = 0.06f), Primary.copy(alpha = 0.02f))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    valueFontWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = valueFontWeight,
            color = valueColor,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
