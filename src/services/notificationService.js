import { functions } from './firebase';
import { httpsCallable } from 'firebase/functions';

/**
 * Web Admin Notification Service
 * Provides methods to send notifications from web dashboard to mobile app
 */

const sendAdminNotification = httpsCallable(functions, 'sendAdminNotification');
const sendBroadcastNotification = httpsCallable(functions, 'sendBroadcastNotification');

/**
 * Send notification to a specific user
 */
export const notifyUser = async (userId, title, description, options = {}) => {
  try {
    const result = await sendAdminNotification({
      userId,
      title,
      description,
      category: options.category || 'ADMIN_MESSAGE',
      actionType: options.actionType || 'VIEW_PROFILE',
      actionData: options.actionData || {},
    });
    console.log('Notification sent:', result.data);
    return result.data;
  } catch (error) {
    console.error('Error sending notification:', error);
    throw error;
  }
};

/**
 * Send broadcast notification to all sellers
 */
export const notifyAllSellers = async (title, description, options = {}) => {
  try {
    const result = await sendBroadcastNotification({
      title,
      description,
      category: options.category || 'SYSTEM',
      actionType: options.actionType || 'VIEW_PROFILE',
      actionData: options.actionData || {},
    });
    console.log('Broadcast notification sent:', result.data);
    return result.data;
  } catch (error) {
    console.error('Error sending broadcast notification:', error);
    throw error;
  }
};

/**
 * Notify seller about product approval
 */
export const notifyProductApproved = async (sellerId, productTitle, productId) => {
  return notifyUser(sellerId, 'Product Approved', `Your product "${productTitle}" has been approved and is now live.`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PRODUCT',
    actionData: { product_id: productId },
  });
};

/**
 * Notify seller about product rejection
 */
export const notifyProductRejected = async (sellerId, productTitle, productId, reason) => {
  return notifyUser(sellerId, 'Product Rejected', `Your product "${productTitle}" was rejected. Reason: ${reason || 'No reason provided'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PRODUCT',
    actionData: { product_id: productId },
  });
};

/**
 * Notify seller about verification approval
 */
export const notifyVerificationApproved = async (sellerId) => {
  return notifyUser(sellerId, 'Seller Verification Approved', 'Congratulations! Your seller verification has been approved. You can now sell on the platform.', {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { verification_status: 'approved' },
  });
};

/**
 * Notify seller about verification rejection
 */
export const notifyVerificationRejected = async (sellerId, reason) => {
  return notifyUser(sellerId, 'Seller Verification Rejected', `Your seller verification was rejected. Reason: ${reason || 'No reason provided'}`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { verification_status: 'rejected' },
  });
};

/**
 * Notify user about account suspension
 */
export const notifyAccountSuspended = async (userId, reason) => {
  return notifyUser(userId, 'Account Suspended', `Your account has been suspended. Reason: ${reason || 'No reason provided'}`, {
    category: 'ADMIN_MESSAGE',
    actionType: 'VIEW_PROFILE',
    actionData: { suspension_status: 'suspended' },
  });
};

/**
 * Notify user about account reactivation
 */
export const notifyAccountReactivated = async (userId) => {
  return notifyUser(userId, 'Account Reactivated', 'Your account has been reactivated. You can now use the platform again.', {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { suspension_status: 'active' },
  });
};

/**
 * Notify store owner about store flag
 */
export const notifyStoreFlagged = async (ownerId, storeName, reason) => {
  return notifyUser(ownerId, 'Store Flagged', `Your store "${storeName}" has been flagged. Reason: ${reason || 'No reason provided'}`, {
    category: 'ADMIN_MESSAGE',
    actionType: 'VIEW_PROFILE',
    actionData: { store_flagged: true },
  });
};

/**
 * Notify store owner about store flag removal
 */
export const notifyStoreFlagRemoved = async (ownerId, storeName) => {
  return notifyUser(ownerId, 'Store Flag Removed', `The flag on your store "${storeName}" has been removed.`, {
    category: 'SYSTEM',
    actionType: 'VIEW_PROFILE',
    actionData: { store_flagged: false },
  });
};

/**
 * Notify seller about order status change
 */
export const notifyOrderStatusChanged = async (sellerId, orderNumber, newStatus) => {
  const statusMessages = {
    'pending': 'Your order is pending',
    'confirmed': 'Your order has been confirmed',
    'shipped': 'Your order has been shipped',
    'delivered': 'Your order has been delivered',
    'cancelled': 'Your order has been cancelled',
  };

  const message = statusMessages[newStatus] || `Order status updated to ${newStatus}`;

  return notifyUser(sellerId, 'Order Status Updated', `Order #${orderNumber}: ${message}`, {
    category: 'ORDERS',
    actionType: 'VIEW_ORDER',
    actionData: { order_number: orderNumber },
  });
};
