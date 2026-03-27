package com.gcuf.craftoria.utils

import android.content.Context
import android.content.Intent
import com.gcuf.craftoria.data.model.Order
import com.gcuf.craftoria.data.model.getCreatedAtLong
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceUtils {

    fun shareInvoice(context: Context, order: Order) {
        val orderId = order.id.take(8).uppercase()
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val orderDate = dateFormat.format(Date(order.getCreatedAtLong()))

        val invoiceText = buildString {
            appendLine("╔══════════════════════════════╗")
            appendLine("║         CRAFTORIA             ║")
            appendLine("║    Handcrafted with love      ║")
            appendLine("╚══════════════════════════════╝")
            appendLine()
            appendLine("INVOICE #$orderId")
            appendLine("Date: $orderDate")
            appendLine("Status: ${order.status.replaceFirstChar { it.uppercase() }}")
            appendLine("Payment: ${order.paymentMethod}")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("CUSTOMER INFO")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Name:  ${order.buyerName}")
            appendLine("Phone: ${order.buyerPhone}")
            val address = buildString {
                append(order.deliveryInfo.address)
                if (order.deliveryInfo.city.isNotEmpty()) append(", ${order.deliveryInfo.city}")
                if (order.deliveryInfo.postalCode.isNotEmpty()) append(", ${order.deliveryInfo.postalCode}")
                append(", Pakistan")
            }
            appendLine("Address: $address")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("ORDER ITEMS")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            order.items.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.productTitle}")
                appendLine("   Qty: ${item.quantity}  x  PKR ${item.price.toInt()}  =  PKR ${(item.quantity * item.price).toInt()}")
            }
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("ORDER SUMMARY")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Subtotal:     PKR ${order.subtotal.toInt()}")
            if (order.shipping > 0)
                appendLine("Delivery:     PKR ${order.shipping.toInt()}")
            else
                appendLine("Delivery:     Free")
            if (order.discount > 0)
                appendLine("Discount:    -PKR ${order.discount.toInt()}")
            appendLine()
            appendLine("TOTAL:        PKR ${order.totalPrice.toInt()}")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Thank you for shopping with Craftoria!")
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Invoice #$orderId - Craftoria")
            putExtra(Intent.EXTRA_TEXT, invoiceText)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share Invoice via")
        )
    }
}