package com.gcuf.craftoria.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.ui.screens.buyer.formatDateTime
import com.gcuf.craftoria.ui.theme.*

@Composable
fun CancelOrderDialog(
    orderId: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Text(
                    text = "Cancel Order?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Are you sure you want to cancel this order? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Error
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Error),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = "Yes, Cancel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, BorderColor),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = "No, Keep Order",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTrackingDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Primary, PrimaryLight)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Track Order",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    DetailSection(title = "Delivery Status") {
                        if (order.timeline.isNotEmpty()) {
                            OrderTimeline(timeline = order.timeline)
                        } else {
                            // Default timeline
                            TimelineItem(
                                title = "Order Confirmed",
                                time = formatDateTime(order.createdAt),
                                isCompleted = true,
                                isLast = false
                            )
                            TimelineItem(
                                title = "Picked Up by Courier",
                                time = "Pending",
                                isCompleted = false,
                                isLast = false
                            )
                            TimelineItem(
                                title = "In Transit",
                                time = "Pending",
                                isCompleted = false,
                                isLast = false
                            )
                            TimelineItem(
                                title = "Out for Delivery",
                                time = "Pending",
                                isCompleted = false,
                                isLast = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (order.estimatedDelivery != null && order.estimatedDelivery > 0) {
                        DetailSection(title = "Estimated Delivery") {
                            DetailRow(
                                label = "Expected by:",
                                value = formatDateTime(order.estimatedDelivery)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (order.trackingId.isNotEmpty()) {
                        DetailSection(title = "Courier Information") {
                            DetailRow(
                                label = "Courier:",
                                value = order.courierName.ifEmpty { "TCS Express" }
                            )
                            DetailRow(
                                label = "Tracking ID:",
                                value = order.trackingId
                            )
                            if (order.courierContact.isNotEmpty()) {
                                DetailRow(
                                    label = "Contact:",
                                    value = order.courierContact
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}