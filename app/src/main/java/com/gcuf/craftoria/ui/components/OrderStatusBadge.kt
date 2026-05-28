package com.gcuf.craftoria.ui.components

import androidx.compose.runtime.Composable
import com.gcuf.craftoria.data.model.OrderStatus

/** Order status pill — delegates to unified [StatusBadge]. */
@Composable
fun OrderStatusBadge(status: OrderStatus) {
    StatusBadge(status = status)
}
