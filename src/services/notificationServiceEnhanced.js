import { functions } from './firebase';
import { httpsCallable } from 'firebase/functions';

/**
 * Enhanced Notification Service
 * Comprehensive notification system for all user interactions
 */

const sendAdminNotification = httpsCallable(functions, 'sendAdminNotification');
const sendBroadcastNotification = httpsCallable(functions, 'sendBroadcastNotification');

// ============================================================================
// CORE NOTIFICATION FUNCTIONS
// ============================================================================

/**
 * Send notification to a specific user with priority support
 */
export const notifyUser = async (userId, title, description, options = {}) => {
  try {
    const result = await sendAdminNotification({
      userId,
      title,
      description,
      category: options.category || 'ADMIN_MESSAGE',
      actionType: options.actionType || 'VIEW_PROFILE',
      actionData: {
        ...options.actionData,
        priority: options.priority || 'medium',
        timestamp: new Date().toISOString()
      },
    });
    console.log('Notification sent:', result.data);
    return result.data;
  } catch (error) {
    console.error('Error sending notification:', error);
    throw error;
  }
};

/**
 * Send broadcast notification with priority support
 */
export const notifyAllUsers = async (title, description, options = {}) => {
  try {
    const result = await sendBroadcastNotification({
      title,
      description,
      category: options.category || 'SYSTEM',
      actionType: options.actionType || 'VIEW_PROFILE',
      actionData: {
        ...options.actionData,
        priority: options.priority || 'medium',
        timestamp: new Date().toISOString()
      },
    });
    console.log('Broadcast notification sent:', result.data);
    return result.data;
  } catch (error) {
    console.error('Error sending broadcast notification:', error);
    throw error;
  }
};

// ============================================================================
// BUYER-TO-SELLER NOTIFICATIONS
// ============================================================================

/**
 * Notify seller about new message from buyer
 */
export const notifySellerNewMessage = async (sellerId, buyerName, productTitle, messagePreview) => {
  return notifyUser(sellerId, 'New Message', `${buyerName} sent you a message about "${productTitle}": ${messagePreview}`, {
    category: 'MESSAGE',
    actionType: 'VIEW_MESSAGES',
    actionData: { 
      buyer_name: buyerName, 
      product_title: productTitle,
      message_preview: messagePreview
    },
    priority: 'medium'
  });
};

/**
 * Notify seller about negotiation request
 */
export const notifyNegotiationRequest = async (sellerId, buyerName, productTitle, offerAmount, originalPrice) => {
  const discount = ((originalPrice - offerAmount) / originalPrice * 100).toFixed(0);
  return notifyUser(sellerId, 'Negotiation Request', `${buyerName} offered $${offerAmount} for "${productTitle}" (${discount}% off original price)`, {
    category: 'NEGOTIATION',
    actionType: 'VIEW_NEGOTIATIONS',
    actionData: { 
      buyer_name: buyerName, 
      product_title: productTitle, 
      offer_amount: offerAmount,
      original_price: originalPrice,
      discount_percentage: discount
    },
    priority: 'high'
  });
};

/**
 * Notify seller about new order
 */
export const notifySellerNewOrder = async (sellerId, buyerName, orderNumber, totalAmount, itemCount) => {
  return notifyUser(sellerId, 'New Order Received! 🎉', `${buyerName} placed order #${orderNumber} for ${itemCount} item(s) worth $${totalAmount}`, {
    category: 'ORDER',
    actionType: 'VIEW_ORDER',
    actionData: { 
      buyer_name: buyerName,
      order_number: orderNumber,
      total_amount: totalAmount,
      item_count: itemCount
    },
    priority: 'high'
  });
};

/**
 * Notify seller about order cancellation request
 */
export const notifyOrderCancellationRequest = async (sellerId, buyerName, orderNumber, reason) => {
  return notifyUser(sellerId, 'Order Cancellation Request', `${buyerName} requested to cancel order #${orderNumber}. Reason: ${reason}`, {
    category: 'ORDER',
    actionType: 'VIEW_ORDER',
    actionData: { 
      buyer_name: buyerName,
      order_number: orderNumber,
      cancellation_reason: reason
    },
    priority: 'high'
  });
};

/**
 * Notify seller about product inquiry
 */
export const notifyProductInquiry = async (sellerId, buyerName, productTitle, inquiryType, question) => {
  return notifyUser(sellerId, 'Product Inquiry', `${buyerName} asked about "${productTitle}" - ${inquiryType}: ${question}`, {
    category: 'INQUIRY',
    actionType: 'VIEW_PRODUCT',
    actionData: { 
      buyer_name: buyerName,
      product_title: productTitle,
      inquiry_type: inquiryType,
      question: question
    },
    priority: 'medium'
  });
};

/**
 * Notify seller about store rating
 */
export const notifyStoreRating = async (sellerId, buyerName, rating, review) => {
  const emoji = rating >= 4 ? '⭐' : rating >= 3 ? '👍' : '⚠️';
  return notifyUser(sellerId, `New ${rating}-Star Rating ${emoji}`, `${buyerName} rated your store ${rating}/5 stars${review ? ': "' + review.substring(0, 50) + '..."' : ''}`, {
    category: 'RATING',
    actionType: 'VIEW_STORE_RATINGS',
    actionData: { 
      buyer_name: buyerName,
      rating: rating,
      review: review
    },
    priority: rating >= 4 ? 'low' : 'medium'
  });
};

/**
 * Notify seller about payment received
 */
export const notifyPaymentReceived = async (sellerId, buyerName, orderNumber, amount, paymentMethod) => {
  return notifyUser(sellerId, 'Payment Received 💰', `Payment of $${amount} received from ${buyerName} for order #${orderNumber} via ${paymentMethod}`, {
    category: 'PAYMENT',
    actionType: 'VIEW_PAYMENTS',
    actionData: { 
      buyer_name: buyerName,
      order_number: orderNumber,
      amount: amount,
      payment_method: paymentMethod
    },
    priority: 'medium'
  });
};

// ============================================================================
// SELLER-TO-BUYER NOTIFICATIONS
// ============================================================================

/**
 * Notify buyer about negotiation response
 */
export const notifyNegotiationResponse = async (buyerId, sellerName, productTitle, responseType, counterOffer = null) => {
  const messages = {
    accepted: `${sellerName} accepted your offer for "${productTitle}"! You can now proceed to checkout.`,
    rejected: `${sellerName} declined your offer for "${productTitle}".`,
    counter: `${sellerName} made a counter-offer of $${counterOffer} for "${productTitle}".`
  };

  const emojis = {
    accepted: '✅',
    rejected: '❌',
    counter: '🔄'
  };

  return notifyUser(buyerId, `Negotiation ${responseType.charAt(0).toUpperCase() + responseType.slice(1)} ${emojis[responseType]}`, messages[responseType], {
    category: 'NEGOTIATION',
    actionType: 'VIEW_NEGOTIATIONS',
    actionData: { 
      seller_name: sellerName,
      product_title: productTitle,
      response_type: responseType,
      counter_offer: counterOffer
    },
    priority: 'high'
  });
};

/**
 * Notify buyer about product back in stock
 */
export const notifyProductBackInStock = async (buyerId, productTitle, productId, sellerName) => {
  return notifyUser(buyerId, 'Product Back in Stock! 📦', `"${productTitle}" by ${sellerName} is now available again!`, {
    category: 'PRODUCT',
    actionType: 'VIEW_PRODUCT',
    actionData: { 
      product_id: productId,
      product_title: productTitle,
      seller_name: sellerName
    },
    priority: 'medium'
  });
};

/**
 * Notify buyer about price drop
 */
export const notifyPriceDrop = async (buyerId, productTitle, productId, oldPrice, newPrice, sellerName) => {
  const discount = ((oldPrice - newPrice) / oldPrice * 100).toFixed(0);
  return notifyUser(buyerId, `Price Drop Alert! 💰 ${discount}% Off`, `"${productTitle}" by ${sellerName} dropped from $${oldPrice} to $${newPrice}`, {
    category: 'PRODUCT',
    actionType: 'VIEW_PRODUCT',
    actionData: { 
      product_id: productId,
      product_title: productTitle,
      seller_name: sellerName,
      old_price: oldPrice,
      new_price: newPrice,
      discount_percentage: discount
    },
    priority: 'medium'
  });
};

/**
 * Notify buyer about shipping update
 */
export const notifyShippingUpdate = async (buyerId, orderNumber, status, trackingNumber = null, estimatedDelivery = null) => {
  const statusMessages = {
    preparing: 'Your order is being prepared for shipment 📦',
    shipped: `Your order has been shipped 🚚${trackingNumber ? ` (Tracking: ${trackingNumber})` : ''}`,
    in_transit: 'Your order is on its way 🛣️',
    out_for_delivery: 'Your order is out for delivery today 🚛',
    delivered: 'Your order has been delivered! 📬'
  };

  const emojis = {
    preparing: '📦',
    shipped: '🚚',
    in_transit: '🛣️',
    out_for_delivery: '🚛',
    delivered: '📬'
  };

  return notifyUser(buyerId, `Shipping Update ${emojis[status]}`, statusMessages[status] + (estimatedDelivery ? ` - ETA: ${estimatedDelivery}` : ''), {
    category: 'SHIPPING',
    actionType: 'TRACK_ORDER',
    actionData: { 
      order_number: orderNumber,
      shipping_status: status,
      tracking_number: trackingNumber,
      estimated_delivery: estimatedDelivery
    },
    priority: status === 'delivered' ? 'low' : 'medium'
  });
};

/**
 * Notify buyer about seller message response
 */
export const notifyBuyerSellerResponse = async (buyerId, sellerName, productTitle, messagePreview) => {
  return notifyUser(buyerId, 'Seller Replied 💬', `${sellerName} replied about "${productTitle}": ${messagePreview}`, {
    category: 'MESSAGE',
    actionType: 'VIEW_MESSAGES',
    actionData: { 
      seller_name: sellerName,
      product_title: productTitle,
      message_preview: messagePreview
    },
    priority: 'medium'
  });
};

/**
 * Notify buyer about order confirmation
 */
export const notifyOrderConfirmation = async (buyerId, sellerName, orderNumber, totalAmount, estimatedDelivery) => {
  return notifyUser(buyerId, 'Order Confirmed! ✅', `${sellerName} confirmed your order #${orderNumber} for $${totalAmount}. Estimated delivery: ${estimatedDelivery}`, {
    category: 'ORDER',
    actionType: 'VIEW_ORDER',
    actionData: { 
      seller_name: sellerName,
      order_number: orderNumber,
      total_amount: totalAmount,
      estimated_delivery: estimatedDelivery
    },
    priority: 'high'
  });
};

// ============================================================================
// ADMIN-TO-USERS NOTIFICATIONS (EXISTING + NEW)
// ============================================================================

/**
 * Notify seller about product approval
 */
export const notifyProductApproved = async (sellerId, productTitle, productId) => {
  return notifyUser(sellerId, 'Product Approved ✅', `Your product "${productTitle}" has been approved and is now live!`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PRODUCT',
    actionData: { product_id: productId },
    priority: 'medium'
  });
};

/**
 * Notify seller about product rejection
 */
export const notifyProductRejected = async (sellerId, productTitle, productId, reason) => {
  return notifyUser(sellerId, 'Product Rejected ❌', `Your product "${productTitle}" was rejected. Reason: ${reason || 'No reason provided'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PRODUCT',
    actionData: { product_id: productId, rejection_reason: reason },
    priority: 'high'
  });
};

/**
 * Notify seller about verification approval
 */
export const notifyVerificationApproved = async (sellerId) => {
  return notifyUser(sellerId, 'Seller Verification Approved! 🎉', 'Congratulations! Your seller verification has been approved. You can now sell on the platform.', {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { verification_status: 'approved' },
    priority: 'high'
  });
};

/**
 * Notify seller about verification rejection
 */
export const notifyVerificationRejected = async (sellerId, reason) => {
  return notifyUser(sellerId, 'Seller Verification Rejected ❌', `Your seller verification was rejected. Reason: ${reason || 'No reason provided'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { verification_status: 'rejected', rejection_reason: reason },
    priority: 'high'
  });
};

/**
 * Notify user about account suspension
 */
export const notifyAccountSuspended = async (userId, reason) => {
  return notifyUser(userId, 'Account Suspended ⚠️', `Your account has been suspended. Reason: ${reason || 'No reason provided'}`, {
    category: 'ADMIN_MESSAGE',
    actionType: 'VIEW_PROFILE',
    actionData: { suspension_status: 'suspended', suspension_reason: reason },
    priority: 'high'
  });
};

/**
 * Notify user about account reactivation
 */
export const notifyAccountReactivated = async (userId) => {
  return notifyUser(userId, 'Account Reactivated ✅', 'Your account has been reactivated. You can now use the platform again.', {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { suspension_status: 'active' },
    priority: 'medium'
  });
};

/**
 * Notify store owner about store flag
 */
export const notifyStoreFlagged = async (ownerId, storeName, reason) => {
  return notifyUser(ownerId, 'Store Flagged ⚠️', `Your store "${storeName}" has been flagged. Reason: ${reason || 'No reason provided'}`, {
    category: 'ADMIN_MESSAGE',
    actionType: 'VIEW_STORE',
    actionData: { store_flagged: true, flag_reason: reason },
    priority: 'high'
  });
};

/**
 * Notify store owner about store flag removal
 */
export const notifyStoreFlagRemoved = async (ownerId, storeName) => {
  return notifyUser(ownerId, 'Store Flag Removed ✅', `The flag on your store "${storeName}" has been removed.`, {
    category: 'SYSTEM',
    actionType: 'VIEW_STORE',
    actionData: { store_flagged: false },
    priority: 'medium'
  });
};

/**
 * Notify about policy updates
 */
export const notifyPolicyUpdate = async (userId, policyType, summary) => {
  return notifyUser(userId, `${policyType} Policy Update 📋`, `Important updates to our ${policyType.toLowerCase()} policy: ${summary}`, {
    category: 'POLICY',
    actionType: 'VIEW_POLICIES',
    actionData: { 
      policy_type: policyType,
      summary: summary
    },
    priority: 'medium'
  });
};

/**
 * Notify about system maintenance
 */
export const notifySystemMaintenance = async (userId, maintenanceDate, duration, affectedServices) => {
  return notifyUser(userId, 'Scheduled Maintenance Notice 🔧', `System maintenance on ${maintenanceDate} for ${duration}. Affected: ${affectedServices.join(', ')}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { 
      maintenance_date: maintenanceDate,
      duration: duration,
      affected_services: affectedServices
    },
    priority: 'low'
  });
};

/**
 * Broadcast policy update to all users
 */
export const broadcastPolicyUpdate = async (policyType, summary) => {
  return notifyAllUsers(`${policyType} Policy Update 📋`, `Important updates to our ${policyType.toLowerCase()} policy: ${summary}`, {
    category: 'POLICY',
    actionType: 'VIEW_POLICIES',
    actionData: { 
      policy_type: policyType,
      summary: summary
    },
    priority: 'medium'
  });
};

/**
 * Broadcast system maintenance to all users
 */
export const broadcastSystemMaintenance = async (maintenanceDate, duration, affectedServices) => {
  return notifyAllUsers('Scheduled Maintenance Notice 🔧', `System maintenance on ${maintenanceDate} for ${duration}. Affected: ${affectedServices.join(', ')}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { 
      maintenance_date: maintenanceDate,
      duration: duration,
      affected_services: affectedServices
    },
    priority: 'low'
  });
};

// ============================================================================
// SYSTEM-TO-ADMIN NOTIFICATIONS
// ============================================================================

/**
 * Notify admin about new seller application
 */
export const notifyAdminNewSellerApplication = async (applicantName, applicantEmail, applicantId) => {
  return notifyAllUsers('New Seller Application 📝', `${applicantName} (${applicantEmail}) has applied to become a seller. Review their application in the admin panel.`, {
    category: 'ADMIN_ALERT',
    actionType: 'VIEW_SELLER_APPLICATIONS',
    actionData: { 
      applicant_id: applicantId,
      applicant_name: applicantName,
      applicant_email: applicantEmail
    },
    priority: 'medium'
  });
};

/**
 * Notify admin about reported content
 */
export const notifyAdminReportedContent = async (reportType, reportedItemId, reporterName, reason) => {
  return notifyAllUsers(`Content Reported: ${reportType} ⚠️`, `${reporterName} reported a ${reportType.toLowerCase()} (ID: ${reportedItemId}). Reason: ${reason}`, {
    category: 'MODERATION',
    actionType: 'VIEW_REPORTS',
    actionData: { 
      report_type: reportType,
      reported_item_id: reportedItemId,
      reporter_name: reporterName,
      reason: reason
    },
    priority: 'high'
  });
};

/**
 * Notify admin about payment disputes
 */
export const notifyAdminPaymentDispute = async (orderNumber, buyerName, sellerName, disputeAmount, reason) => {
  return notifyAllUsers(`Payment Dispute - Order #${orderNumber} 💰`, `Dispute between ${buyerName} and ${sellerName} for $${disputeAmount}. Reason: ${reason}`, {
    category: 'PAYMENT_DISPUTE',
    actionType: 'VIEW_DISPUTES',
    actionData: { 
      order_number: orderNumber,
      buyer_name: buyerName,
      seller_name: sellerName,
      dispute_amount: disputeAmount,
      reason: reason
    },
    priority: 'high'
  });
};

/**
 * Notify admin about system errors
 */
export const notifyAdminSystemError = async (errorType, errorMessage, affectedUsers, severity) => {
  const emoji = severity === 'Critical' ? '🚨' : severity === 'High' ? '⚠️' : '⚡';
  return notifyAllUsers(`System Error: ${errorType} ${emoji}`, `${severity} error affecting ${affectedUsers} users: ${errorMessage}`, {
    category: 'SYSTEM_ERROR',
    actionType: 'VIEW_SYSTEM_LOGS',
    actionData: { 
      error_type: errorType,
      error_message: errorMessage,
      affected_users: affectedUsers,
      severity: severity
    },
    priority: severity === 'Critical' ? 'high' : 'medium'
  });
};

/**
 * Notify admin about suspicious activity
 */
export const notifyAdminSuspiciousActivity = async (userId, userName, activityType, details, riskLevel) => {
  const emoji = riskLevel === 'High' ? '🚨' : riskLevel === 'Medium' ? '⚠️' : '👀';
  return notifyAllUsers(`Suspicious Activity Detected ${emoji}`, `${riskLevel} risk activity by ${userName} (${userId}): ${activityType}`, {
    category: 'SECURITY',
    actionType: 'VIEW_USER_ACTIVITY',
    actionData: { 
      user_id: userId,
      user_name: userName,
      activity_type: activityType,
      details: details,
      risk_level: riskLevel
    },
    priority: riskLevel === 'High' ? 'high' : 'medium'
  });
};

/**
 * Notify admin about high-value transactions
 */
export const notifyAdminHighValueTransaction = async (orderNumber, buyerName, sellerName, amount, paymentMethod) => {
  return notifyAllUsers(`High-Value Transaction 💎`, `Transaction of $${amount} between ${buyerName} and ${sellerName} via ${paymentMethod} (Order #${orderNumber})`, {
    category: 'TRANSACTION_ALERT',
    actionType: 'VIEW_TRANSACTION',
    actionData: { 
      order_number: orderNumber,
      buyer_name: buyerName,
      seller_name: sellerName,
      amount: amount,
      payment_method: paymentMethod
    },
    priority: 'medium'
  });
};

// ============================================================================
// LEGACY COMPATIBILITY (keeping existing function names)
// ============================================================================

/**
 * Legacy function for order status changes
 */
export const notifyOrderStatusChanged = async (userId, orderNumber, newStatus, isSellerNotification = false) => {
  const statusMessages = {
    'pending': 'Your order is pending confirmation',
    'confirmed': 'Your order has been confirmed',
    'shipped': 'Your order has been shipped',
    'delivered': 'Your order has been delivered',
    'cancelled': 'Your order has been cancelled',
  };

  const message = statusMessages[newStatus] || `Order status updated to ${newStatus}`;
  const emoji = newStatus === 'delivered' ? '📬' : newStatus === 'shipped' ? '🚚' : newStatus === 'confirmed' ? '✅' : '📦';

  return notifyUser(userId, `Order Status Updated ${emoji}`, `Order #${orderNumber}: ${message}`, {
    category: 'ORDER',
    actionType: 'VIEW_ORDER',
    actionData: { 
      order_number: orderNumber,
      status: newStatus,
      is_seller_notification: isSellerNotification
    },
    priority: newStatus === 'delivered' ? 'low' : 'medium'
  });
};

// Export legacy functions for backward compatibility
export const notifyAllSellers = notifyAllUsers;