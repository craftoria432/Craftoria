import {
  collection,
  doc,
  addDoc,
  updateDoc,
  deleteDoc,
  getDocs,
  query,
  where,
  orderBy,
  limit,
  writeBatch,
  serverTimestamp,
  onSnapshot,
  Timestamp,
} from 'firebase/firestore';
import { db, isFirebaseConfigured } from './firebase';

const NOTIFICATIONS_COLLECTION = 'notifications';
const ADMIN_ACTIVITIES_COLLECTION = 'admin_activities';
const ADMINS_COLLECTION = 'admins';

// Retry configuration
const RETRY_CONFIG = {
  maxRetries: 3,
  initialDelay: 1000, // 1 second
  maxDelay: 10000, // 10 seconds
  backoffMultiplier: 2,
};

/**
 * Retry wrapper for async operations
 * @param {Function} operation - Async operation to retry
 * @param {string} operationName - Name for logging
 * @returns {Promise} Operation result
 */
const withRetry = async (operation, operationName = 'Operation') => {
  let lastError;
  let delay = RETRY_CONFIG.initialDelay;

  for (let attempt = 0; attempt <= RETRY_CONFIG.maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error;
      
      // Don't retry on auth errors
      if (error.code === 'permission-denied' || error.code === 'unauthenticated') {
        throw error;
      }

      if (attempt < RETRY_CONFIG.maxRetries) {
        console.warn(
          `${operationName} failed (attempt ${attempt + 1}/${RETRY_CONFIG.maxRetries + 1}). Retrying in ${delay}ms...`,
          error.message
        );
        await new Promise(resolve => setTimeout(resolve, delay));
        delay = Math.min(delay * RETRY_CONFIG.backoffMultiplier, RETRY_CONFIG.maxDelay);
      }
    }
  }

  throw lastError;
};

/**
 * Create a single notification with retry logic
 */
export const createNotification = async (notification) => {
  if (!isFirebaseConfigured) {
    return { success: false, error: 'Firebase not configured' };
  }

  return withRetry(async () => {
    const { user_id, title, description, category, action_type, action_data } = notification;

    if (!user_id || !title || !category) {
      throw new Error('Missing required fields: user_id, title, category');
    }

    const notificationRef = collection(db, NOTIFICATIONS_COLLECTION);
    const docRef = await addDoc(notificationRef, {
      user_id,
      title,
      description: description || '',
      category,
      action_type: action_type || '',
      action_data: action_data || {},
      is_read: false,
      created_at: serverTimestamp(),
      updated_at: serverTimestamp(),
    });

    return {
      success: true,
      data: {
        id: docRef.id,
        message: 'Notification created successfully',
      },
    };
  }, 'createNotification');
};

/**
 * Create broadcast notification with batch operations and retry
 */
export const createBroadcastNotification = async (notification) => {
  if (!isFirebaseConfigured) {
    return { success: false, error: 'Firebase not configured' };
  }

  return withRetry(async () => {
    const { title, description, category, action_type, action_data } = notification;

    if (!title || !category) {
      throw new Error('Missing required fields: title, category');
    }

    const adminsRef = collection(db, ADMINS_COLLECTION);
    const adminsSnapshot = await getDocs(adminsRef);

    if (adminsSnapshot.empty) {
      console.warn('No admins found for broadcast notification');
      return {
        success: true,
        data: {
          count: 0,
          message: 'No admins found',
        },
      };
    }

    const batch = writeBatch(db);
    const notificationsRef = collection(db, NOTIFICATIONS_COLLECTION);

    adminsSnapshot.docs.forEach(adminDoc => {
      const newNotifRef = doc(notificationsRef);
      batch.set(newNotifRef, {
        user_id: adminDoc.id,
        title,
        description: description || '',
        category,
        action_type: action_type || '',
        action_data: action_data || {},
        is_read: false,
        created_at: serverTimestamp(),
        updated_at: serverTimestamp(),
      });
    });

    await batch.commit();

    return {
      success: true,
      data: {
        count: adminsSnapshot.docs.length,
        message: `Broadcast notification created for ${adminsSnapshot.docs.length} admins`,
      },
    };
  }, 'createBroadcastNotification');
};

/**
 * Get notifications with retry and error handling
 */
export const getNotifications = async (userId, limitCount = 50) => {
  if (!isFirebaseConfigured) {
    return { success: false, error: 'Firebase not configured' };
  }

  return withRetry(async () => {
    if (!userId) {
      throw new Error('userId is required');
    }

    const q = query(
      collection(db, NOTIFICATIONS_COLLECTION),
      where('user_id', '==', userId),
      orderBy('created_at', 'desc'),
      limit(limitCount)
    );

    const snapshot = await getDocs(q);
    const notifications = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    return {
      success: true,
      data: notifications,
    };
  }, 'getNotifications');
};

/**
 * Subscribe to real-time notifications with error handling
 * @param {string} userId - User ID
 * @param {Function} callback - Callback function
 * @param {Function} errorCallback - Error callback
 * @returns {Function} Unsubscribe function
 */
export const subscribeToNotifications = (userId, callback, errorCallback) => {
  if (!isFirebaseConfigured) {
    errorCallback?.('Firebase not configured');
    return () => {};
  }

  if (!userId) {
    errorCallback?.('userId is required');
    return () => {};
  }

  const q = query(
    collection(db, NOTIFICATIONS_COLLECTION),
    where('user_id', '==', userId),
    orderBy('created_at', 'desc'),
    limit(50)
  );

  let retryCount = 0;
  const maxRetries = 3;

  const unsubscribe = onSnapshot(
    q,
    (snapshot) => {
      retryCount = 0; // Reset on success
      const notifications = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data(),
      }));
      callback(notifications);
    },
    (error) => {
      console.error('Error subscribing to notifications:', error);

      // Attempt fallback to getDocs
      if (retryCount < maxRetries) {
        retryCount++;
        console.warn(`Falling back to polling (attempt ${retryCount}/${maxRetries})`);

        getDocs(q)
          .then(snapshot => {
            const notifications = snapshot.docs.map(doc => ({
              id: doc.id,
              ...doc.data(),
            }));
            callback(notifications);
          })
          .catch(fallbackError => {
            errorCallback?.(fallbackError.message);
          });
      } else {
        errorCallback?.(error.message);
      }
    }
  );

  return unsubscribe;
};

/**
 * Mark notification as read with retry
 */
export const markAsRead = async (notificationId, userId) => {
  if (!isFirebaseConfigured) {
    return { success: false, error: 'Firebase not configured' };
  }

  return withRetry(async () => {
    if (!notificationId || !userId) {
      throw new Error('notificationId and userId are required');
    }

    const notifRef = doc(db, NOTIFICATIONS_COLLECTION, notificationId);
    await updateDoc(notifRef, {
      is_read: true,
      updated_at: serverTimestamp(),
    });

    return {
      success: true,
      message: 'Notification marked as read',
    };
  }, 'markAsRead');
};

/**
 * Delete notification with retry
 */
export const deleteNotification = async (notificationId) => {
  if (!isFirebaseConfigured) {
    return { success: false, error: 'Firebase not configured' };
  }

  return withRetry(async () => {
    if (!notificationId) {
      throw new Error('notificationId is required');
    }

    const notifRef = doc(db, NOTIFICATIONS_COLLECTION, notificationId);
    await deleteDoc(notifRef);

    return {
      success: true,
      message: 'Notification deleted successfully',
    };
  }, 'deleteNotification');
};

/**
 * Log admin activity with non-blocking retry
 */
export const logAdminActivity = async (activity) => {
  if (!isFirebaseConfigured) {
    console.warn('Firebase not configured, skipping activity log');
    return { success: false, error: 'Firebase not configured' };
  }

  // Fire and forget with retry
  withRetry(async () => {
    const {
      action,
      resource_id,
      resource_type,
      details = {},
      status = 'success',
      error_message = null,
      admin_id,
      admin_email,
    } = activity;

    if (!action || !resource_id || !resource_type || !admin_id || !admin_email) {
      throw new Error('Missing required fields');
    }

    const activitiesRef = collection(db, ADMIN_ACTIVITIES_COLLECTION);
    await addDoc(activitiesRef, {
      action,
      resource_id,
      resource_type,
      details,
      status,
      error_message,
      admin_id,
      admin_email,
      timestamp: serverTimestamp(),
    });

    return { success: true };
  }, 'logAdminActivity').catch(error => {
    console.error('Error logging admin activity:', error);
  });
};

/**
 * Commission notifications with retry
 */
export const notifyAdminNewCommission = async (commissionData) => {
  return withRetry(async () => {
    const { amount, orderId, sellerId, sellerName } = commissionData;

    const adminsQuery = query(
      collection(db, 'users'),
      where('role', 'in', ['super_admin', 'admin'])
    );

    const adminSnapshot = await getDocs(adminsQuery);
    const batch = writeBatch(db);
    const notifRef = collection(db, 'notifications');

    adminSnapshot.docs.forEach(adminDoc => {
      const docRef = doc(notifRef);
      batch.set(docRef, {
        user_id: adminDoc.id,
        type: 'commission',
        title: 'New Commission Created',
        message: `Commission of PKR ${amount?.toLocaleString('en-PK')} from order #${orderId?.substring(0, 8)} (${sellerName})`,
        category: 'commission',
        action_type: 'VIEW_COMMISSION',
        action_data: {
          commission_amount: amount,
          order_id: orderId,
          seller_id: sellerId,
          seller_name: sellerName,
        },
        read: false,
        createdAt: serverTimestamp(),
        updated_at: serverTimestamp(),
      });
    });

    await batch.commit();
    console.log('✅ New commission notification sent to all admins');
  }, 'notifyAdminNewCommission');
};

export const notifyAdminCommissionPaid = async (commissionData) => {
  return withRetry(async () => {
    const { amount, orderId, sellerName } = commissionData;

    const adminsQuery = query(
      collection(db, 'users'),
      where('role', 'in', ['super_admin', 'admin'])
    );

    const adminSnapshot = await getDocs(adminsQuery);
    const batch = writeBatch(db);
    const notifRef = collection(db, 'notifications');

    adminSnapshot.docs.forEach(adminDoc => {
      const docRef = doc(notifRef);
      batch.set(docRef, {
        user_id: adminDoc.id,
        type: 'commission',
        title: 'Commission Paid',
        message: `Commission of PKR ${amount?.toLocaleString('en-PK')} from order #${orderId?.substring(0, 8)} has been marked as paid`,
        category: 'commission',
        action_type: 'VIEW_COMMISSION',
        action_data: {
          commission_amount: amount,
          order_id: orderId,
          seller_name: sellerName,
          status: 'paid',
        },
        read: false,
        createdAt: serverTimestamp(),
        updated_at: serverTimestamp(),
      });
    });

    await batch.commit();
    console.log('✅ Commission paid notification sent to all admins');
  }, 'notifyAdminCommissionPaid');
};

export const notifyAdminCommissionSettingsUpdated = async (settingsData) => {
  return withRetry(async () => {
    const { commissionRate, updatedBy } = settingsData;

    const adminsQuery = query(
      collection(db, 'users'),
      where('role', 'in', ['super_admin', 'admin'])
    );

    const adminSnapshot = await getDocs(adminsQuery);
    const batch = writeBatch(db);
    const notifRef = collection(db, 'notifications');

    adminSnapshot.docs.forEach(adminDoc => {
      const docRef = doc(notifRef);
      batch.set(docRef, {
        user_id: adminDoc.id,
        type: 'commission',
        title: 'Commission Settings Updated',
        message: `Commission rate has been updated to ${commissionRate}% by ${updatedBy}`,
        category: 'commission',
        action_type: 'VIEW_COMMISSION_SETTINGS',
        action_data: {
          commission_rate: commissionRate,
          updated_by: updatedBy,
        },
        read: false,
        createdAt: serverTimestamp(),
        updated_at: serverTimestamp(),
      });
    });

    await batch.commit();
    console.log('✅ Commission settings update notification sent to all admins');
  }, 'notifyAdminCommissionSettingsUpdated');
};
