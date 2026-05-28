# Buyer Refund Authorization Debug Fix

## Issue
Buyers are getting "Unauthorized: Not involved in this order" error when trying to request refunds for their own orders.

## Root Cause Analysis

The authorization check in `PaymentRepository.getOrderPayments()` validates that the requesting user is either:
1. A seller in the payment split, OR
2. The buyer who placed the order

The check looks at:
- Payment records (`buyer_id` field in `seller_payments` collection)
- Order document (`buyer_id` field in `order