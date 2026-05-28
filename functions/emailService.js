const functions = require('firebase-functions');
const admin = require('firebase-admin');
const emailjs = require('emailjs-com');

// Initialize EmailJS
const EMAILJS_PUBLIC_KEY = process.env.EMAILJS_PUBLIC_KEY;
const EMAILJS_SERVICE_ID = process.env.EMAILJS_SERVICE_ID;
const EMAILJS_TEMPLATE_ID = process.env.EMAILJS_TEMPLATE_ID;

// Initialize EmailJS if keys are available
if (EMAILJS_PUBLIC_KEY && EMAILJS_SERVICE_ID && EMAILJS_TEMPLATE_ID) {
  emailjs.init({
    publicKey: EMAILJS_PUBLIC_KEY,
    privateKey: process.env.EMAILJS_PRIVATE_KEY,
    limitRate: {
      id: 'craftoria_email_service',
      throttle: 300 // 300ms between emails
    }
  });
  console.log('[EmailJS] Initialized successfully');
} else {
  console.warn('[EmailJS] Configuration incomplete. Email sending will fail.');
}

/**
 * Validate email format
 * @param {string} email - Email address to validate
 * @returns {boolean} - True if valid email format
 */
const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Validate required order data
 * @param {object} orderData - Order data object
 * @returns {object} - { valid: boolean, error: string }
 */
const validateOrderData = (orderData) => {
  if (!orderData) {
    return { valid: false, error: 'Order data is required' };
  }

  const requiredFields = [
    'customerEmail',
    'customerName',
    'orderId',
    'orderDate',
    'paymentMethod',
    'totalPrice',
    'deliveryAddress'
  ];

  for (const field of requiredFields) {
    if (!orderData[field]) {
      return { valid: false, error: `Missing required field: ${field}` };
    }
  }

  if (!validateEmail(orderData.customerEmail)) {
    return { valid: false, error: 'Invalid email format' };
  }

  return { valid: true };
};

/**
 * Log email event to Firestore
 * @param {string} event - Event type
 * @param {object} data - Event data
 */
const logEmailEvent = async (event, data) => {
  try {
    const timestamp = new Date();
    await admin.firestore().collection('email_logs').add({
      event,
      data,
      timestamp,
      status: data.status || 'pending'
    });
  } catch (error) {
    console.error('[EmailJS] Failed to log event:', error);
  }
};

/**
 * Send order confirmation email via EmailJS
 * @param {object} orderData - Order data containing customer and order details
 * @returns {Promise<object>} - { success: boolean, messageId?: string, error?: string }
 */
const sendOrderConfirmationEmail = async (orderData) => {
  try {
    // Validate input
    const validation = validateOrderData(orderData);
    if (!validation.valid) {
      console.error('[EmailJS] Validation error:', validation.error);
      await logEmailEvent('email_validation_failed', {
        error: validation.error,
        orderId: orderData?.orderId
      });
      return { success: false, error: validation.error };
    }

    // Check if EmailJS is initialized
    if (!EMAILJS_PUBLIC_KEY || !EMAILJS_SERVICE_ID || !EMAILJS_TEMPLATE_ID) {
      console.error('[EmailJS] Configuration missing. Cannot send email.');
      await logEmailEvent('email_config_missing', {
        orderId: orderData.orderId
      });
      return {
        success: false,
        error: 'Email service not configured',
        orderCompleted: true
      };
    }

    // Log email send attempt
    console.log('[EmailJS] Sending order confirmation to:', orderData.customerEmail);
    await logEmailEvent('email_send_start', {
      email: orderData.customerEmail,
      orderId: orderData.orderId
    });

    // Prepare template parameters
    const templateParams = {
      to_email: orderData.customerEmail,
      to_name: orderData.customerName,
      order_id: orderData.orderId,
      order_date: orderData.orderDate,
      payment_method: orderData.paymentMethod,
      total_price: orderData.totalPrice,
      delivery_address: orderData.deliveryAddress
    };

    // Send email
    const response = await emailjs.send(
      EMAILJS_SERVICE_ID,
      EMAILJS_TEMPLATE_ID,
      templateParams
    );

    // Log success
    console.log('[EmailJS] Email sent successfully:', {
      email: orderData.customerEmail,
      orderId: orderData.orderId,
      messageId: response.status
    });

    await logEmailEvent('email_send_success', {
      email: orderData.customerEmail,
      orderId: orderData.orderId,
      messageId: response.status
    });

    return {
      success: true,
      messageId: response.status
    };
  } catch (error) {
    // Log error
    console.error('[EmailJS] Email send failed:', {
      email: orderData?.customerEmail,
      orderId: orderData?.orderId,
      error: error.text || error.message
    });

    await logEmailEvent('email_send_failed', {
      email: orderData?.customerEmail,
      orderId: orderData?.orderId,
      error: error.text || error.message
    });

    // Return error but don't block order completion
    return {
      success: false,
      error: error.text || error.message,
      orderCompleted: true
    };
  }
};

/**
 * Cloud Function: Send order confirmation email
 * Triggered by HTTP request from checkout
 */
exports.sendOrderConfirmation = functions.https.onCall(async (data, context) => {
  try {
    // Verify user is authenticated
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated to send emails'
      );
    }

    // Send email
    const result = await sendOrderConfirmationEmail(data);

    if (!result.success) {
      // Log but don't throw - order should still complete
      console.warn('[EmailJS] Email failed but order completed:', result.error);
    }

    return result;
  } catch (error) {
    console.error('[EmailJS] Cloud function error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

/**
 * Cloud Function: Test email service configuration
 */
exports.testEmailService = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated'
      );
    }

    console.log('[EmailJS] Testing email service configuration...');

    if (!EMAILJS_PUBLIC_KEY) {
      return { configured: false, message: 'Public key not configured' };
    }

    if (!EMAILJS_SERVICE_ID) {
      return { configured: false, message: 'Service ID not configured' };
    }

    if (!EMAILJS_TEMPLATE_ID) {
      return { configured: false, message: 'Template ID not configured' };
    }

    // Try to send a test email
    const testData = {
      customerEmail: context.auth.token.email,
      customerName: context.auth.token.name || 'Test User',
      orderId: 'TEST-' + Date.now(),
      orderDate: new Date().toLocaleDateString(),
      paymentMethod: 'Test',
      totalPrice: '0.00',
      deliveryAddress: 'Test Address'
    };

    const result = await sendOrderConfirmationEmail(testData);

    if (result.success) {
      return {
        configured: true,
        message: 'Email service is properly configured and working'
      };
    } else {
      return {
        configured: false,
        message: `Email service test failed: ${result.error}`
      };
    }
  } catch (error) {
    console.error('[EmailJS] Test error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

/**
 * Cloud Function: Get email service status
 */
exports.getEmailServiceStatus = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated'
      );
    }

    return {
      initialized: !!EMAILJS_PUBLIC_KEY,
      serviceConfigured: !!EMAILJS_SERVICE_ID,
      templateConfigured: !!EMAILJS_TEMPLATE_ID,
      ready: !!(EMAILJS_PUBLIC_KEY && EMAILJS_SERVICE_ID && EMAILJS_TEMPLATE_ID),
      publicKeyLength: EMAILJS_PUBLIC_KEY ? EMAILJS_PUBLIC_KEY.length : 0,
      serviceId: EMAILJS_SERVICE_ID ? EMAILJS_SERVICE_ID.substring(0, 10) + '...' : 'Not set',
      templateId: EMAILJS_TEMPLATE_ID || 'Not set'
    };
  } catch (error) {
    console.error('[EmailJS] Status error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

/**
 * Cloud Function: Get email logs
 */
exports.getEmailLogs = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated'
      );
    }

    // Only admins can view logs
    const userDoc = await admin.firestore().collection('users').doc(context.auth.uid).get();
    if (!userDoc.exists || userDoc.data().role !== 'admin') {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only admins can view email logs'
      );
    }

    const limit = data.limit || 50;
    const snapshot = await admin.firestore()
      .collection('email_logs')
      .orderBy('timestamp', 'desc')
      .limit(limit)
      .get();

    const logs = [];
    snapshot.forEach(doc => {
      logs.push({
        id: doc.id,
        ...doc.data(),
        timestamp: doc.data().timestamp?.toDate?.() || doc.data().timestamp
      });
    });

    return { success: true, logs };
  } catch (error) {
    console.error('[EmailJS] Logs error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

/**
 * Send seller approval email via EmailJS
 * @param {object} sellerData - Seller data containing email and name
 * @returns {Promise<object>} - { success: boolean, messageId?: string, error?: string }
 */
const sendSellerApprovalEmail = async (sellerData) => {
  try {
    // Validate input
    if (!sellerData || !sellerData.sellerEmail || !sellerData.sellerName) {
      console.error('[EmailJS] Missing seller data');
      await logEmailEvent('seller_email_validation_failed', {
        error: 'Missing seller email or name'
      });
      return { success: false, error: 'Missing seller email or name' };
    }

    if (!validateEmail(sellerData.sellerEmail)) {
      console.error('[EmailJS] Invalid seller email format');
      await logEmailEvent('seller_email_validation_failed', {
        error: 'Invalid email format',
        email: sellerData.sellerEmail
      });
      return { success: false, error: 'Invalid email format' };
    }

    // Check if EmailJS is initialized
    if (!EMAILJS_PUBLIC_KEY || !EMAILJS_SERVICE_ID || !EMAILJS_TEMPLATE_ID) {
      console.error('[EmailJS] Configuration missing. Cannot send email.');
      await logEmailEvent('seller_email_config_missing', {
        email: sellerData.sellerEmail
      });
      return {
        success: false,
        error: 'Email service not configured'
      };
    }

    // Log email send attempt
    console.log('[EmailJS] Sending seller approval email to:', sellerData.sellerEmail);
    await logEmailEvent('seller_email_send_start', {
      email: sellerData.sellerEmail,
      name: sellerData.sellerName
    });

    // Prepare template parameters for seller approval
    const templateParams = {
      to_email: sellerData.sellerEmail,
      to_name: sellerData.sellerName,
      subject: '🎉 Your Seller Account Has Been Approved!',
      message: `Congratulations ${sellerData.sellerName}! Your seller application has been approved. You can now start selling on Craftoria.`
    };

    // Send email
    const response = await emailjs.send(
      EMAILJS_SERVICE_ID,
      EMAILJS_TEMPLATE_ID,
      templateParams
    );

    // Log success
    console.log('[EmailJS] Seller approval email sent successfully:', {
      email: sellerData.sellerEmail,
      messageId: response.status
    });

    await logEmailEvent('seller_email_send_success', {
      email: sellerData.sellerEmail,
      name: sellerData.sellerName,
      messageId: response.status
    });

    return {
      success: true,
      messageId: response.status
    };
  } catch (error) {
    // Log error
    console.error('[EmailJS] Seller approval email send failed:', {
      email: sellerData?.sellerEmail,
      error: error.text || error.message
    });

    await logEmailEvent('seller_email_send_failed', {
      email: sellerData?.sellerEmail,
      error: error.text || error.message
    });

    return {
      success: false,
      error: error.text || error.message
    };
  }
};

/**
 * Cloud Function: Send seller application approval email
 */
exports.sendSellerApplicationApprovalEmail = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated'
      );
    }

    const { sellerEmail, sellerName, welcomeMessage } = data;

    if (!sellerEmail || !sellerName) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Seller email and name are required'
      );
    }

    const result = await sendSellerApprovalEmail({
      sellerEmail,
      sellerName,
      welcomeMessage
    });

    return result;
  } catch (error) {
    console.error('[EmailJS] Seller approval email error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

/**
 * Cloud Function: Send identity verification approval email
 */
exports.sendIdentityVerificationApprovalEmail = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated'
      );
    }

    const { sellerEmail, sellerName, welcomeMessage } = data;

    if (!sellerEmail || !sellerName) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Seller email and name are required'
      );
    }

    // Use the same approval email template
    const result = await sendSellerApprovalEmail({
      sellerEmail,
      sellerName,
      welcomeMessage: welcomeMessage || 'Your identity verification has been approved! You can now start selling on Craftoria.'
    });

    return result;
  } catch (error) {
    console.error('[EmailJS] Identity verification email error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

module.exports = {
  sendOrderConfirmationEmail,
  sendSellerApprovalEmail,
  validateOrderData,
  validateEmail
};
