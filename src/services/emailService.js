import emailjs from '@emailjs/browser';

// Initialize EmailJS with public key
const PUBLIC_KEY = process.env.REACT_APP_EMAILJS_PUBLIC_KEY;
const SERVICE_ID = process.env.REACT_APP_EMAILJS_SERVICE_ID;
const TEMPLATE_ID = process.env.REACT_APP_EMAILJS_TEMPLATE_ID;

// Initialize on module load
if (PUBLIC_KEY) {
  emailjs.init(PUBLIC_KEY);
  console.log('[EmailJS] Initialized successfully');
} else {
  console.warn('[EmailJS] Public key not configured. Email sending will fail.');
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
 * Send order confirmation email
 * @param {object} orderData - Order data containing customer and order details
 * @returns {Promise<object>} - { success: boolean, messageId?: string, error?: string }
 */
export const sendOrderConfirmation = async (orderData) => {
  try {
    // Validate input
    const validation = validateOrderData(orderData);
    if (!validation.valid) {
      console.error('[EmailJS] Validation error:', validation.error);
      return { success: false, error: validation.error };
    }

    // Check if EmailJS is initialized
    if (!PUBLIC_KEY || !SERVICE_ID || !TEMPLATE_ID) {
      console.error('[EmailJS] Configuration missing. Cannot send email.');
      return {
        success: false,
        error: 'Email service not configured',
        orderCompleted: true // Don't block order
      };
    }

    // Log email send attempt
    console.log('[EmailJS] Sending order confirmation to:', orderData.customerEmail);

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
    const response = await emailjs.send(SERVICE_ID, TEMPLATE_ID, templateParams);

    // Log success
    console.log('[EmailJS] Email sent successfully:', {
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

    // Return error but don't block order completion
    return {
      success: false,
      error: error.text || error.message,
      orderCompleted: true // Order is still completed even if email fails
    };
  }
};

/**
 * Send email with retry logic
 * @param {object} orderData - Order data
 * @param {number} maxRetries - Maximum number of retries (default: 3)
 * @returns {Promise<object>} - { success: boolean, attempts: number, error?: string }
 */
export const sendOrderConfirmationWithRetry = async (orderData, maxRetries = 3) => {
  let lastError = null;
  let attempts = 0;

  for (let i = 0; i < maxRetries; i++) {
    attempts++;
    console.log(`[EmailJS] Attempt ${attempts}/${maxRetries}`);

    const result = await sendOrderConfirmation(orderData);

    if (result.success) {
      console.log('[EmailJS] Email sent successfully after', attempts, 'attempt(s)');
      return { success: true, attempts };
    }

    lastError = result.error;

    // Wait before retrying (exponential backoff)
    if (i < maxRetries - 1) {
      const delay = Math.pow(2, i) * 1000; // 1s, 2s, 4s
      console.log(`[EmailJS] Retrying in ${delay}ms...`);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  console.error('[EmailJS] Failed after', attempts, 'attempts');
  return {
    success: false,
    attempts,
    error: lastError,
    orderCompleted: true // Order still completes
  };
};

/**
 * Test email service configuration
 * @returns {Promise<object>} - { configured: boolean, message: string }
 */
export const testEmailService = async () => {
  console.log('[EmailJS] Testing email service configuration...');

  if (!PUBLIC_KEY) {
    return { configured: false, message: 'Public key not configured' };
  }

  if (!SERVICE_ID) {
    return { configured: false, message: 'Service ID not configured' };
  }

  if (!TEMPLATE_ID) {
    return { configured: false, message: 'Template ID not configured' };
  }

  try {
    // Try to send a test email
    const testData = {
      customerEmail: 'test@example.com',
      customerName: 'Test User',
      orderId: 'TEST-001',
      orderDate: new Date().toLocaleDateString(),
      paymentMethod: 'Test',
      totalPrice: '0.00',
      deliveryAddress: 'Test Address'
    };

    const result = await sendOrderConfirmation(testData);

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
    return {
      configured: false,
      message: `Email service test error: ${error.message}`
    };
  }
};

/**
 * Get email service status
 * @returns {object} - Service status information
 */
export const getEmailServiceStatus = () => {
  return {
    initialized: !!PUBLIC_KEY,
    serviceConfigured: !!SERVICE_ID,
    templateConfigured: !!TEMPLATE_ID,
    ready: !!(PUBLIC_KEY && SERVICE_ID && TEMPLATE_ID),
    publicKeyLength: PUBLIC_KEY ? PUBLIC_KEY.length : 0,
    serviceId: SERVICE_ID ? SERVICE_ID.substring(0, 10) + '...' : 'Not set',
    templateId: TEMPLATE_ID || 'Not set'
  };
};

export default {
  sendOrderConfirmation,
  sendOrderConfirmationWithRetry,
  testEmailService,
  getEmailServiceStatus
};
