import { functions } from './firebase';
import { httpsCallable } from 'firebase/functions';

/**
 * Web Admin Notification Service - FOCUSED VERSION
 * Only includes notifications that web admin actually needs to send
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
    console.log('✅ Notification sent:', result.data);
    return result.data;
  } catch (error) {
    console.error('❌ Error sending notification:', error);
    throw error;
  }
};

/**
 * Send broadcast notification to all users
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
    console.log('✅ Broadcast notification sent:', result.data);
    return result.data;
  } catch (error) {
    console.error('❌ Error sending broadcast notification:', error);
    throw error;
  }
};

// ============================================================================
// WEB ADMIN SPECIFIC NOTIFICATIONS
// ============================================================================

// 📝 SELLER APPLICATIONS (SellerApplicationsAndVerifications.jsx)
/**
 * Notify user about seller application approval
 */
export const notifyApplicationApproved = async (userId, welcomeMessage) => {
  return notifyUser(userId, 'Seller Application Approved! 🎉', `Congratulations! Your seller application has been approved. ${welcomeMessage || 'You can now start selling on Craftoria!'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { application_status: 'approved', welcome_message: welcomeMessage },
    priority: 'high'
  });
};

/**
 * Notify user about seller application rejection
 */
export const notifyApplicationRejected = async (userId, reason) => {
  return notifyUser(userId, 'Seller Application Rejected ❌', `Your seller application was rejected. Reason: ${reason || 'Please contact support for more details.'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { application_status: 'rejected', rejection_reason: reason },
    priority: 'high'
  });
};

// 📝 SELLER VERIFICATION (SellerVerification.jsx)
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
  return notifyUser(sellerId, 'Seller Verification Rejected ❌', `Your seller verification was rejected. Reason: ${reason || 'Please contact support for more details.'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { verification_status: 'rejected', rejection_reason: reason },
    priority: 'high'
  });
};

// 📦 PRODUCT MANAGEMENT (ProductManagement.jsx)
/**
 * Notify seller about product approval
 */
export const notifyProductApproved = async (sellerId, productTitle, productId) => {
  return notifyUser(sellerId, 'Product Approved ✅', `Your product "${productTitle}" has been approved and is now live!`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PRODUCT',
    actionData: { product_id: productId, product_title: productTitle },
    priority: 'medium'
  });
};

/**
 * Notify seller about product rejection
 */
export const notifyProductRejected = async (sellerId, productTitle, productId, reason) => {
  return notifyUser(sellerId, 'Product Rejected ❌', `Your product "${productTitle}" was rejected. Reason: ${reason || 'Please review our product guidelines.'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PRODUCT',
    actionData: { product_id: productId, product_title: productTitle, rejection_reason: reason },
    priority: 'high'
  });
};

// 👤 USER MANAGEMENT (UserManagement.jsx)
/**
 * Notify user about account suspension
 */
export const notifyAccountSuspended = async (userId, reason) => {
  return notifyUser(userId, 'Account Suspended ⚠️', `Your account has been suspended. Reason: ${reason || 'Violation of community guidelines'}`, {
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
  return notifyUser(userId, 'Account Reactivated ✅', 'Your account has been reactivated. Welcome back! You can now use all features of Craftoria.', {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { suspension_status: 'active' },
    priority: 'medium'
  });
};

// 📋 ORDER OVERSIGHT (OrderOversight.jsx)
/**
 * Notify about order status changes (for both buyer and seller)
 */
export const notifyOrderStatusChanged = async (userId, orderNumber, newStatus, isSellerNotification = false) => {
  const statusMessages = {
    'pending': 'Your order is pending confirmation',
    'confirmed': 'Your order has been confirmed',
    'shipped': 'Your order has been shipped',
    'delivered': 'Your order has been delivered',
    'cancelled': 'Your order has been cancelled',
    'refunded': 'Your order has been refunded'
  };

  const statusEmojis = {
    'pending': '⏳',
    'confirmed': '✅',
    'shipped': '🚚',
    'delivered': '📬',
    'cancelled': '❌',
    'refunded': '💰'
  };

  const message = statusMessages[newStatus] || `Order status updated to ${newStatus}`;
  const emoji = statusEmojis[newStatus] || '📦';

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

// 🏪 STORE MANAGEMENT
/**
 * Notify store owner about store flag
 */
export const notifyStoreFlagged = async (ownerId, storeName, reason) => {
  return notifyUser(ownerId, 'Store Flagged ⚠️', `Your store "${storeName}" has been flagged. Reason: ${reason || 'Violation of store policies'}`, {
    category: 'ADMIN_MESSAGE',
    actionType: 'VIEW_STORE',
    actionData: { store_flagged: true, flag_reason: reason, store_name: storeName },
    priority: 'high'
  });
};

/**
 * Notify store owner about store flag removal
 */
export const notifyStoreFlagRemoved = async (ownerId, storeName) => {
  return notifyUser(ownerId, 'Store Flag Removed ✅', `The flag on your store "${storeName}" has been removed. Your store is now in good standing.`, {
    category: 'SYSTEM',
    actionType: 'VIEW_STORE',
    actionData: { store_flagged: false, store_name: storeName },
    priority: 'medium'
  });
};

// ============================================================================
// SYSTEM-WIDE ADMIN NOTIFICATIONS
// ============================================================================

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
// LEGACY COMPATIBILITY (keeping existing function names)
// ============================================================================

// For backward compatibility with existing web admin code
export const notifyAllSellers = notifyAllUsers;

// ============================================================================
// USAGE EXAMPLES FOR WEB ADMIN PAGES
// ============================================================================

/*
// SellerVerification.jsx
await notifyVerificationApproved(sellerId);
await notifyVerificationRejected(sellerId, "Incomplete documents");

// ProductManagement.jsx  
await notifyProductApproved(sellerId, "Handmade Scarf", "prod_123");
await notifyProductRejected(sellerId, "Handmade Scarf", "prod_123", "Poor image quality");

// UserManagement.jsx
await notifyAccountSuspended(userId, "Spam behavior");
await notifyAccountReactivated(userId);

// OrderOversight.jsx
await notifyOrderStatusChanged(buyerId, "ORD-12345", "shipped", false);
await notifyOrderStatusChanged(sellerId, "ORD-12345", "shipped", true);

// System-wide
await broadcastPolicyUpdate("Privacy", "Updated data collection practices");
await broadcastSystemMaintenance("Dec 25, 2024", "2 hours", ["Payments", "Orders"]);
*/