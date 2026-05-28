import { functions } from './firebase';
import { httpsCallable } from 'firebase/functions';

/**
 * Email Notification Service for Web Dashboard
 * Sends email notifications for seller applications and verifications
 */

const sendSellerApplicationApprovalEmail = httpsCallable(functions, 'sendSellerApplicationApprovalEmail');
const sendIdentityVerificationApprovalEmail = httpsCallable(functions, 'sendIdentityVerificationApprovalEmail');

/**
 * Send email when seller application is approved
 * @param {string} sellerEmail - Seller's email address
 * @param {string} sellerName - Seller's name
 * @param {string} welcomeMessage - Optional welcome message
 * @returns {Promise<object>} - Result of email send operation
 */
export const sendApplicationApprovalEmail = async (sellerEmail, sellerName, welcomeMessage = '') => {
  try {
    console.log('[Email] Sending application approval email to:', sellerEmail);
    
    const result = await sendSellerApplicationApprovalEmail({
      sellerEmail,
      sellerName,
      welcomeMessage
    });

    console.log('[Email] Application approval email sent successfully');
    return { success: true, data: result.data };
  } catch (error) {
    console.error('[Email] Failed to send application approval email:', error);
    // Don't throw - email failure shouldn't block the approval process
    return { success: false, error: error.message };
  }
};

/**
 * Send email when identity verification is approved
 * @param {string} sellerEmail - Seller's email address
 * @param {string} sellerName - Seller's name
 * @param {string} welcomeMessage - Optional welcome message
 * @returns {Promise<object>} - Result of email send operation
 */
export const sendVerificationApprovalEmail = async (sellerEmail, sellerName, welcomeMessage = '') => {
  try {
    console.log('[Email] Sending verification approval email to:', sellerEmail);
    
    const result = await sendIdentityVerificationApprovalEmail({
      sellerEmail,
      sellerName,
      welcomeMessage
    });

    console.log('[Email] Verification approval email sent successfully');
    return { success: true, data: result.data };
  } catch (error) {
    console.error('[Email] Failed to send verification approval email:', error);
    // Don't throw - email failure shouldn't block the approval process
    return { success: false, error: error.message };
  }
};
