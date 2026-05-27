package com.gcuf.craftoria.utils

import android.util.Log
import com.gcuf.craftoria.data.model.Notification
import com.gcuf.craftoria.data.model.NotificationActionType
import com.gcuf.craftoria.data.model.NotificationCategory
import com.gcuf.craftoria.data.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Production-ready notification helper for creating all notification types
 * Handles both buyer and seller notifications with proper error handling and logging
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private val notificationRepository = NotificationRepository()

    // ==================== BUYER NOTIFICATIONS ====================

    /**
     * Order Delivery Confirmation - Sent to buyer when order is delivered
     */
    fun notifyOrderDelivered(
        buyerId: String,
        orderId: String,
        storeName: String,
        orderNumber: String,
        storeId: String = "",
        memberCount: Int = 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = buyerId,
                    title = "Order Delivered",
                    description = "Your order #$orderNumber from $storeName has been delivered",
                    category = NotificationCategory.ORDERS.name,
                    actionType = NotificationActionType.TRACK_ORDER.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId,
                    storeName = storeName,
                    storeId = storeId,
                    memberCount = memberCount
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Order delivery notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create order delivery notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order delivery notification", e)
            }
        }
    }

    /**
     * Order Processing - Sent to buyer when order starts processing
     */
    fun notifyOrderProcessing(
        buyerId: String,
        orderId: String,
        storeName: String,
        orderNumber: String,
        storeId: String = "",
        memberCount: Int = 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = buyerId,
                    title = "Order Processing",
                    description = "Your order #$orderNumber from $storeName is now being processed",
                    category = NotificationCategory.ORDERS.name,
                    actionType = NotificationActionType.TRACK_ORDER.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId,
                    storeName = storeName,
                    storeId = storeId,
                    memberCount = memberCount
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Order processing notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create order processing notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order processing notification", e)
            }
        }
    }

    /**
     * Order Shipped - Sent to buyer when order is shipped
     */
    fun notifyOrderShipped(
        buyerId: String,
        orderId: String,
        storeName: String,
        orderNumber: String,
        courierName: String = "",
        trackingNumber: String = "",
        storeId: String = "",
        memberCount: Int = 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val description = if (courierName.isNotEmpty() && trackingNumber.isNotEmpty()) {
                    "Your order #$orderNumber from $storeName has been shipped via $courierName. Tracking: $trackingNumber"
                } else {
                    "Your order #$orderNumber from $storeName has been shipped"
                }

                val notification = Notification(
                    userId = buyerId,
                    title = "Order Shipped",
                    description = description,
                    category = NotificationCategory.ORDERS.name,
                    actionType = NotificationActionType.TRACK_ORDER.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId,
                    storeName = storeName,
                    storeId = storeId,
                    memberCount = memberCount
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Order shipped notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create order shipped notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order shipped notification", e)
            }
        }
    }

    /**
     * Order Cancellation - Sent to buyer when seller cancels order
     */
    fun notifyOrderCancelledBuyer(
        buyerId: String,
        orderId: String,
        orderNumber: String,
        reason: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val description = if (reason.isNotEmpty()) {
                    "Your order #$orderNumber has been cancelled. Reason: $reason"
                } else {
                    "Your order #$orderNumber has been cancelled by the seller"
                }

                val notification = Notification(
                    userId = buyerId,
                    title = "Order Cancelled",
                    description = description,
                    category = NotificationCategory.ORDERS.name,
                    actionType = NotificationActionType.VIEW_ORDER.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Order cancellation notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create order cancellation notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating order cancellation notification", e)
            }
        }
    }

    /**
     * Refund Processed - Sent to buyer when refund is completed
     */
    fun notifyRefundProcessed(
        buyerId: String,
        orderId: String,
        amount: Double,
        orderNumber: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = buyerId,
                    title = "Refund Processed",
                    description = "Refund of PKR $amount has been processed for order #$orderNumber",
                    category = NotificationCategory.REFUNDS.name,
                    actionType = NotificationActionType.VIEW_PAYMENT.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Refund notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create refund notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating refund notification", e)
            }
        }
    }

    /**
     * Store Rating Reminder - Sent to buyer 3 days after delivery
     */
    fun notifyStoreRatingReminder(
        buyerId: String,
        orderId: String,
        storeName: String,
        storeId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = buyerId,
                    title = "Rate Your Experience",
                    description = "How was your experience with $storeName? Your feedback helps us improve.",
                    category = NotificationCategory.STORE_RATING.name,
                    actionType = NotificationActionType.RATE_ORDER.name,
                    actionData = mapOf("order_id" to orderId, "store_id" to storeId),
                    orderId = orderId,
                    storeId = storeId,
                    storeName = storeName
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Store rating reminder created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create store rating reminder", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating store rating reminder", e)
            }
        }
    }

    /**
     * Promotional Offer - Sent to buyer for special offers
     */
    fun notifyPromotionalOffer(
        buyerId: String,
        title: String,
        description: String,
        discount: Int,
        productId: String = "",
        storeId: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = buyerId,
                    title = title,
                    description = "$description - $discount% off",
                    category = NotificationCategory.PROMOTIONS.name,
                    actionType = NotificationActionType.VIEW_PROMOTIONS.name,
                    actionData = mapOf(
                        "product_id" to productId,
                        "store_id" to storeId,
                        "discount" to discount.toString()
                    ),
                    productId = productId,
                    storeId = storeId
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Promotional offer created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create promotional offer", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating promotional offer", e)
            }
        }
    }

    /**
     * Wishlist Item Back in Stock - Sent to buyer when wishlist item becomes available
     */
    fun notifyWishlistItemAvailable(
        buyerId: String,
        productId: String,
        productName: String,
        price: Double
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = buyerId,
                    title = "Back in Stock",
                    description = "\"$productName\" is back in stock at PKR $price",
                    category = NotificationCategory.PROMOTIONS.name,
                    actionType = NotificationActionType.VIEW_PRODUCT.name,
                    actionData = mapOf("product_id" to productId),
                    productId = productId,
                    productName = productName
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Wishlist availability notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create wishlist notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating wishlist notification", e)
            }
        }
    }

    /**
     * Price Drop Alert - Sent to buyer when wishlist item price drops
     */
    fun notifyPriceDropped(
        buyerId: String,
        productId: String,
        productName: String,
        oldPrice: Double,
        newPrice: Double
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val discount = ((oldPrice - newPrice) / oldPrice * 100).toInt()
                val notification = Notification(
                    userId = buyerId,
                    title = "Price Dropped",
                    description = "\"$productName\" is now PKR $newPrice (was PKR $oldPrice) - Save $discount%",
                    category = NotificationCategory.PROMOTIONS.name,
                    actionType = NotificationActionType.VIEW_PRODUCT.name,
                    actionData = mapOf("product_id" to productId),
                    productId = productId,
                    productName = productName
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Price drop notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create price drop notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating price drop notification", e)
            }
        }
    }

    // ==================== SELLER NOTIFICATIONS ====================

    /**
     * New Order Received - Sent to seller when buyer places order
     */
    fun notifyNewOrderReceived(
        sellerId: String,
        orderId: String,
        orderNumber: String,
        buyerName: String,
        totalAmount: Double
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = sellerId,
                    title = "New Order Received",
                    description = "Order #$orderNumber from $buyerName for PKR $totalAmount",
                    category = NotificationCategory.ORDERS.name,
                    actionType = NotificationActionType.VIEW_ORDER.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId,
                    buyerName = buyerName
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "New order notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create new order notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating new order notification", e)
            }
        }
    }

    /**
     * Order Cancellation Request - Sent to seller when buyer requests cancellation
     */
    fun notifyOrderCancellationRequest(
        sellerId: String,
        orderId: String,
        orderNumber: String,
        buyerName: String,
        reason: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val description = if (reason.isNotEmpty()) {
                    "Cancellation request for order #$orderNumber from $buyerName. Reason: $reason"
                } else {
                    "Cancellation request for order #$orderNumber from $buyerName"
                }

                val notification = Notification(
                    userId = sellerId,
                    title = "Cancellation Request",
                    description = description,
                    category = NotificationCategory.ORDERS.name,
                    actionType = NotificationActionType.VIEW_ORDER.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId,
                    buyerName = buyerName
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Order cancellation request notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create cancellation request notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating cancellation request notification", e)
            }
        }
    }

    /**
     * Payment Received - Sent to seller when payment is processed
     */
    fun notifyPaymentReceived(
        sellerId: String,
        orderId: String,
        orderNumber: String,
        amount: Double
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = sellerId,
                    title = "Payment Received",
                    description = "Payment of PKR $amount received for order #$orderNumber",
                    category = NotificationCategory.PAYMENTS.name,
                    actionType = NotificationActionType.VIEW_PAYMENT.name,
                    actionData = mapOf("order_id" to orderId),
                    orderId = orderId
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Payment received notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create payment notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating payment notification", e)
            }
        }
    }

    /**
     * Payout Processed - Sent to seller when monthly payout is transferred
     */
    fun notifyPayoutProcessed(
        sellerId: String,
        amount: Double,
        payoutDate: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = sellerId,
                    title = "Payout Processed",
                    description = "Payout of PKR $amount has been transferred to your account on $payoutDate",
                    category = NotificationCategory.PAYMENTS.name,
                    actionType = NotificationActionType.VIEW_PAYMENT.name,
                    actionData = mapOf("payout_date" to payoutDate)
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Payout notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create payout notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating payout notification", e)
            }
        }
    }

    /**
     * Product Reported - Sent to seller when product is reported
     */
    fun notifyProductReported(
        sellerId: String,
        productId: String,
        productName: String,
        reportReason: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = sellerId,
                    title = "Product Reported",
                    description = "Your product \"$productName\" has been reported. Reason: $reportReason",
                    category = NotificationCategory.REPORT.name,
                    actionType = NotificationActionType.VIEW_REPORT.name,
                    actionData = mapOf("product_id" to productId),
                    productId = productId,
                    productName = productName
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Product report notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create product report notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating product report notification", e)
            }
        }
    }

    /**
     * Store Rating Received - Sent to seller when buyer rates store
     */
    fun notifyStoreRatingReceived(
        sellerId: String,
        storeId: String,
        storeName: String,
        buyerName: String,
        rating: Int,
        review: String = "",
        memberCount: Int = 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val description = if (review.isNotEmpty()) {
                    "$buyerName rated your store $rating stars: \"$review\""
                } else {
                    "$buyerName rated your store $rating stars"
                }

                val notification = Notification(
                    userId = sellerId,
                    title = "New Store Rating",
                    description = description,
                    category = NotificationCategory.STORE_RATING.name,
                    actionType = NotificationActionType.VIEW_RATING.name,
                    actionData = mapOf("store_id" to storeId),
                    storeId = storeId,
                    storeName = storeName,
                    buyerName = buyerName,
                    ratingValue = rating,
                    ratingReview = review,
                    memberCount = memberCount
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Store rating notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create store rating notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating store rating notification", e)
            }
        }
    }

    /**
     * Co-Seller Invitation - Sent to seller when invited to co-seller store
     */
    fun notifyCoSellerInvitation(
        inviteeId: String,
        storeId: String,
        storeName: String,
        inviterName: String,
        memberCount: Int = 0  // Will be fetched accurately if 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ Get accurate member count
                val accurateMemberCount = if (memberCount > 0) {
                    memberCount
                } else {
                    com.gcuf.craftoria.utils.CoSellerMemberCountManager.getAccurateMemberCount(storeId)
                }

                val notification = Notification(
                    userId = inviteeId,
                    title = "Store Invitation",
                    description = "$inviterName invited you to join $storeName ($accurateMemberCount members)",
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.ACCEPT_INVITATION.name,
                    actionData = mapOf("store_id" to storeId),
                    storeId = storeId,
                    storeName = storeName,
                    inviterName = inviterName,
                    memberCount = accurateMemberCount
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Co-seller invitation created for seller: $inviteeId with accurate member count: $accurateMemberCount")
                } else {
                    Log.e(TAG, "Failed to create co-seller invitation", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating co-seller invitation", e)
            }
        }
    }

    /**
     * Invitation Accepted - Sent to inviter when invitation is accepted
     */
    fun notifyInvitationAccepted(
        inviterId: String,
        storeId: String,
        storeName: String,
        accepterName: String,
        memberCount: Int = 0  // Will be fetched accurately if 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ Get accurate member count
                val accurateMemberCount = if (memberCount > 0) {
                    memberCount
                } else {
                    com.gcuf.craftoria.utils.CoSellerMemberCountManager.getAccurateMemberCount(storeId)
                }

                val notification = Notification(
                    userId = inviterId,
                    title = "Invitation Accepted",
                    description = "$accepterName accepted your invitation to join $storeName ($accurateMemberCount members)",
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.VIEW_STORE.name,
                    actionData = mapOf("store_id" to storeId),
                    storeId = storeId,
                    storeName = storeName,
                    inviterName = accepterName,
                    memberCount = accurateMemberCount
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Invitation accepted notification created for inviter: $inviterId with accurate member count: $accurateMemberCount")
                } else {
                    Log.e(TAG, "Failed to create invitation accepted notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating invitation accepted notification", e)
            }
        }
    }

    /**
     * Admin Message - Sent to seller from admin
     */
    fun notifyAdminMessage(
        sellerId: String,
        title: String,
        message: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = Notification(
                    userId = sellerId,
                    title = title,
                    description = message,
                    category = NotificationCategory.ADMIN_MESSAGE.name,
                    actionType = NotificationActionType.VIEW_PROFILE.name,
                    actionData = mapOf("message_type" to "admin")
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Admin message created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create admin message", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating admin message", e)
            }
        }
    }

    /**
     * Product Approval Status - Sent to seller when product is approved/rejected
     */
    fun notifyProductApprovalStatus(
        sellerId: String,
        productId: String,
        productName: String,
        approved: Boolean,
        reason: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = if (approved) "Approved" else "Rejected"
                val description = if (reason.isNotEmpty() && !approved) {
                    "Your product \"$productName\" has been $status. Reason: $reason"
                } else {
                    "Your product \"$productName\" has been $status"
                }

                val notification = Notification(
                    userId = sellerId,
                    title = "Product $status",
                    description = description,
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.VIEW_PRODUCT.name,
                    actionData = mapOf("product_id" to productId),
                    productId = productId,
                    productName = productName
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Product approval notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create product approval notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating product approval notification", e)
            }
        }
    }

    /**
     * Seller Verification Status - Sent to seller when verification is approved/rejected
     */
    fun notifySellerVerificationStatus(
        sellerId: String,
        approved: Boolean,
        reason: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = if (approved) "Approved" else "Rejected"
                val description = if (reason.isNotEmpty() && !approved) {
                    "Your seller verification has been $status. Reason: $reason"
                } else {
                    "Your seller verification has been $status"
                }

                val notification = Notification(
                    userId = sellerId,
                    title = "Verification $status",
                    description = description,
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.VIEW_PROFILE.name,
                    actionData = mapOf("verification_status" to status.lowercase())
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Seller verification notification created for seller: $sellerId")
                } else {
                    Log.e(TAG, "Failed to create verification notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating verification notification", e)
            }
        }
    }

    /**
     * Seller Application Status - Sent to user when seller application is approved/rejected
     */
    fun notifySellerApplicationStatus(
        userId: String,
        approved: Boolean,
        reason: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = if (approved) "Approved" else "Rejected"
                val description = if (approved) {
                    "Congratulations! Your seller application has been approved. You can now complete your verification to start selling."
                } else if (reason.isNotEmpty()) {
                    "Your seller application has been rejected. Reason: $reason"
                } else {
                    "Your seller application has been rejected. You can apply again."
                }

                val notification = Notification(
                    userId = userId,
                    title = "Seller Application $status",
                    description = description,
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.VIEW_PROFILE.name,
                    actionData = mapOf("application_status" to status.lowercase())
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Seller application notification created for user: $userId")
                } else {
                    Log.e(TAG, "Failed to create application notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating application notification", e)
            }
        }
    }

    /**
     * Notify admins when new seller application is submitted
     */
    fun notifyAdminNewSellerApplication(
        userId: String,
        userName: String,
        userEmail: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // For now, we'll create a general admin notification
                // In a real implementation, you'd query for admin users and send to each
                val notification = Notification(
                    userId = "admin", // This would be replaced with actual admin user IDs
                    title = "New Seller Application",
                    description = "$userName ($userEmail) has applied to become a seller. Review their application in the admin panel.",
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.VIEW_PROFILE.name,
                    actionData = mapOf(
                        "applicant_id" to userId,
                        "applicant_name" to userName,
                        "applicant_email" to userEmail
                    )
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Admin notification created for new seller application: $userId")
                } else {
                    Log.e(TAG, "Failed to create admin notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating admin notification", e)
            }
        }
    }

    /**
     * Member Left Store - Sent to store owner when a member leaves the store
     */
    fun notifyMemberLeftStore(
        ownerId: String,
        storeId: String,
        storeName: String,
        memberName: String,
        memberCount: Int = 0
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ Get accurate member count if not provided
                val accurateMemberCount = if (memberCount > 0) {
                    memberCount
                } else {
                    com.gcuf.craftoria.utils.CoSellerMemberCountManager.getAccurateMemberCount(storeId)
                }

                val notification = Notification(
                    userId = ownerId,
                    title = "Member Left Store",
                    description = "$memberName left $storeName ($accurateMemberCount members remaining)",
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.VIEW_STORE.name,
                    actionData = mapOf("store_id" to storeId),
                    storeId = storeId,
                    storeName = storeName,
                    inviterName = memberName,
                    memberCount = accurateMemberCount
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Member left store notification created for owner: $ownerId with member count: $accurateMemberCount")
                } else {
                    Log.e(TAG, "Failed to create member left notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating member left notification", e)
            }
        }
    }

    /**
     * Report Action Taken - Sent to buyer when admin takes action on their report
     * Informs buyer that their report has been reviewed and action taken against the seller
     */
    fun notifyBuyerReportActionTaken(
        buyerId: String,
        reportId: String,
        reportedSellerName: String,
        actionTaken: String,
        details: String = ""
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val description = if (details.isNotEmpty()) {
                    "Action taken on your report against $reportedSellerName: $actionTaken. Details: $details"
                } else {
                    "Action taken on your report against $reportedSellerName: $actionTaken"
                }

                val notification = Notification(
                    userId = buyerId,
                    title = "Report Action Taken",
                    description = description,
                    category = NotificationCategory.SYSTEM.name,
                    actionType = NotificationActionType.VIEW_PROFILE.name,
                    actionData = mapOf(
                        "report_id" to reportId,
                        "action_type" to "report_action"
                    )
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Report action notification created for buyer: $buyerId")
                } else {
                    Log.e(TAG, "Failed to create report action notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating report action notification", e)
            }
        }
    }

    /**
     * New Message - Sent to recipient when new message is received
     * Populates MESSAGES tab in notifications
     */
    fun notifyNewMessage(
        recipientId: String,
        senderId: String,
        senderName: String,
        messageContent: String,
        chatId: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Truncate message if too long
                val displayContent = if (messageContent.length > 100) {
                    messageContent.substring(0, 100) + "..."
                } else {
                    messageContent
                }

                val notification = Notification(
                    userId = recipientId,
                    title = "New Message from $senderName",
                    description = displayContent,
                    category = NotificationCategory.MESSAGES.name,
                    actionType = NotificationActionType.REPLY_MESSAGE.name,
                    actionData = mapOf(
                        "chat_id" to chatId,
                        "sender_id" to senderId
                    ),
                    storeName = senderName  // Reuse storeName field for sender name
                )
                val result = notificationRepository.createNotification(notification)
                if (result.isSuccess) {
                    Log.d(TAG, "Message notification created for recipient: $recipientId from sender: $senderId")
                } else {
                    Log.e(TAG, "Failed to create message notification", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating message notification", e)
            }
        }
    }
}
