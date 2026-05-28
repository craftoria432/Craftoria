# EmailJS Checkout Integration - Copy & Paste Ready

## 🎯 Where to Add Email Sending

Add email sending **after successful payment** in your checkout component.

---

## 📍 Location: Order Success Handler

### Find This Function

Look for where you handle successful orders (usually in CheckoutScreen or OrderSuccessScreen):

```javascript
// Your existing code
async function handleOrderSuccess(orderData) {
  // Order creation logic
  const order = await createOrder(orderData);
  
  // ← ADD EMAIL SENDING HERE
  
  // Show success screen
  navigateToSuccessScreen();
}
```

---

## 🔧 Add Email Sending

### Step 1: Import Email Service

Add at the top of your component file:

```javascript
import { sendOrderConfirmation } from '../services/emailService';
```

### Step 2: Add Email Sending Code

Add this after order is created:

```javascript
// Send confirmation email (don't block order if it fails)
const emailResult = await sendOrderConfirmation({
  customerEmail: user.email,
  customerName: user.name,
  orderId: order.id,
  orderDate: new Date().toLocaleDateString(),
  paymentMethod: order.paymentMethod,
  totalPrice: order.totalPrice.toString(),
  deliveryAddress: order.deliveryAddress
});

// Log result
if (emailResult.success) {
  console.log('✓ Order confirmation email sent');
} else {
  console.warn('⚠ Email failed but order completed:', emailResult.error);
}
```

---

## 📝 Complete Example

### Before (Without Email)

```javascript
async function handleOrderSuccess(orderData) {
  try {
    // Create order
    const order = await createOrder({
      userId: user.id,
      items: cartItems,
      totalPrice: totalPrice,
      paymentMethod: paymentMethod,
      deliveryAddress: deliveryAddress
    });

    // Show success
    showSuccessMessage('Order placed successfully!');
    navigateToOrderSuccess(order.id);

  } catch (error) {
    showErrorMessage('Failed to place order');
  }
}
```

### After (With Email)

```javascript
import { sendOrderConfirmation } from '../services/emailService';

async function handleOrderSuccess(orderData) {
  try {
    // Create order
    const order = await createOrder({
      userId: user.id,
      items: cartItems,
      totalPrice: totalPrice,
      paymentMethod: paymentMethod,
      deliveryAddress: deliveryAddress
    });

    // Send confirmation email
    const emailResult = await sendOrderConfirmation({
      customerEmail: user.email,
      customerName: user.name,
      orderId: order.id,
      orderDate: new Date().toLocaleDateString(),
      paymentMethod: paymentMethod,
      totalPrice: totalPrice.toString(),
      deliveryAddress: deliveryAddress
    });

    // Log email result (don't block order)
    if (emailResult.success) {
      console.log('✓ Order confirmation email sent');
    } else {
      console.warn('⚠ Email failed but order completed:', emailResult.error);
    }

    // Show success
    showSuccessMessage('Order placed successfully!');
    navigateToOrderSuccess(order.id);

  } catch (error) {
    showErrorMessage('Failed to place order');
  }
}
```

---

## 🔄 With Retry Logic (Optional)

For more reliability, use retry logic:

```javascript
import { sendOrderConfirmationWithRetry } from '../services/emailService';

async function handleOrderSuccess(orderData) {
  try {
    // Create order
    const order = await createOrder(orderData);

    // Send email with automatic retry (up to 3 times)
    const emailResult = await sendOrderConfirmationWithRetry({
      customerEmail: user.email,
      customerName: user.name,
      orderId: order.id,
      orderDate: new Date().toLocaleDateString(),
      paymentMethod: paymentMethod,
      totalPrice: totalPrice.toString(),
      deliveryAddress: deliveryAddress
    });

    console.log(`Email sent after ${emailResult.attempts} attempt(s)`);

    // Show success
    showSuccessMessage('Order placed successfully!');
    navigateToOrderSuccess(order.id);

  } catch (error) {
    showErrorMessage('Failed to place order');
  }
}
```

---

## 📊 Data Mapping

Map your order data to email template variables:

| Order Data | Email Variable | Example |
|-----------|----------------|---------|
| `user.email` | `to_email` | `john@example.com` |
| `user.name` | `to_name` | `John Doe` |
| `order.id` | `order_id` | `ORD-12345` |
| `new Date().toLocaleDateString()` | `order_date` | `3/20/2026` |
| `order.paymentMethod` | `payment_method` | `Credit Card` |
| `order.totalPrice` | `total_price` | `99.99` |
| `order.deliveryAddress` | `delivery_address` | `123 Main St, NY` |

---

## ✅ Checklist

- [ ] Import `sendOrderConfirmation` in checkout component
- [ ] Add email sending after order creation
- [ ] Map all 7 variables correctly
- [ ] Test with sample order
- [ ] Verify email received
- [ ] Check email formatting
- [ ] Verify all variables replaced
- [ ] Error handling in place
- [ ] Ready for production

---

## 🧪 Quick Test

### Test 1: Check Configuration

```javascript
import { getEmailServiceStatus } from '../services/emailService';

const status = getEmailServiceStatus();
console.log('Email service status:', status);
// Should show: ready: true
```

### Test 2: Send Test Email

```javascript
import { sendOrderConfirmation } from '../services/emailService';

const result = await sendOrderConfirmation({
  customerEmail: 'your-email@gmail.com',
  customerName: 'Test User',
  orderId: 'TEST-001',
  orderDate: '2026-03-20',
  paymentMethod: 'Test',
  totalPrice: '99.99',
  deliveryAddress: 'Test Address'
});

console.log('Email result:', result);
// Should show: { success: true, messageId: 200 }
```

---

## 🚀 Deployment

1. **Add `.env` file** with EmailJS keys
2. **Install package**: `npm install @emailjs/browser`
3. **Add import** to checkout component
4. **Add email sending code** after order creation
5. **Test** with sample order
6. **Deploy** to production

---

## 📞 Troubleshooting

### Email Not Sending?

1. Check `.env` file has all 3 keys
2. Run `getEmailServiceStatus()` to verify configuration
3. Check browser console for errors
4. Verify EmailJS dashboard has service connected

### Email Looks Wrong?

1. Check template in EmailJS dashboard
2. Verify all variables are replaced
3. Test on different email clients

### Order Blocked by Email Error?

This shouldn't happen - email failures don't block orders. If it does:

1. Check error handling code
2. Make sure you're not throwing on email error
3. Verify `orderCompleted: true` in response

---

## 📝 Notes

- Email sending is **non-blocking** - order completes even if email fails
- All errors are **logged to console** with `[EmailJS]` prefix
- Retry logic is **optional** but recommended
- All 7 variables are **required** for email to send

---

**Status:** Ready to integrate  
**Time:** ~5 minutes  
**Difficulty:** Easy

