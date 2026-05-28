// src/services/adminAuditService.js
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';
import { db } from './firebase';

/**
 * Log admin actions for verification reviews
 * @param {string} action - Action type
 * @param {object} details - Action details
 */
export const logAdminAction = async (action, details) => {
  try {
    await addDoc(collection(db, 'admin_audit_logs'), {
      action, // 'approve_application', 'reject_application', 'approve_verification', 'reject_verification'
      ...details,
      timestamp: serverTimestamp(),
    });
    console.log('✅ Admin action logged:', action);
  } catch (error) {
    console.error('❌ Failed to log admin action:', error);
    // Don't throw - logging failure shouldn't break the main flow
  }
};

/**
 * Get audit logs for a specific user
 * @param {string} userId - User ID
 * @returns {Promise<Array>} - Array of audit log entries
 */
export const getUserAuditLogs = async (userId) => {
  try {
    const { query, where, getDocs, orderBy } = await import('firebase/firestore');
    const logsQuery = query(
      collection(db, 'admin_audit_logs'),
      where('userId', '==', userId),
      orderBy('timestamp', 'desc')
    );
    const snapshot = await getDocs(logsQuery);
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
  } catch (error) {
    console.error('Error fetching audit logs:', error);
    return [];
  }
};
