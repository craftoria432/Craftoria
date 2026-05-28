# EmailJS Quick Code Examples - Copy & Paste Ready

## 🚀 Ready-to-Use Code Snippets

---

## 1. Basic Setup

### Install Package
```bash
npm install @emailjs/browser
```

### Initialize EmailJS
```javascript
import emailjs from '@emailjs/browser';

// Initialize with your Public Key
emailjs.init('YOUR_PUBLIC_KEY_HERE');
```

---

## 2. Send Order Confirmation Email

### Simple Function
```javascript
function sendOrderConfirmation(email, name, orderId, price, method, address, date) {
    emailjs.send('YOUR_SERVICE_ID', 'order_confirmation', {
        to_email: email,
        to_name: name,
        order_id: orderId,
        total_price: price,
        payment_method: method,
        delivery_address: address,
        order_date: date
    })
    .then(() => console.log('Email sent!'))
    .catch(error => console.error('Error:', error));
}

// Usage
sendOrderConfirmation(
    'customer@example.com',
    'John Doe',
    'ORD-12345',
    '99.99',
    'Credit Card',
    '123 Main St, City, State 12345',
    '2026-03-20'
);
```

### With Error Handling
```javascript
async function sendOrderConfirmation(orderData) {
    try {
        const response = await emailjs.send(
            'YOUR_SERVICE_ID',
            'order_confirmation',
            {
                to_email: orderData.email,
                to_name: orderData.name,
                order_id: orderData.orderId,
                total_price: orderData.price,
                payment_method: orderData.method,
                delivery_address: orderData.address,
                order_date: orderData.date
            }
        );
        console.log('Success:', response);
        return { success: true, message: 'Email sent successfully' };
    } catch (error) {
        console.error('Error:', error);
        return { success: false, message: error.text };
    }
}

// Usage
const result = await sendOrderConfirmation({
    email: 'customer@example.com',
    name: 'John Doe',
    orderId: 'ORD-12345',
    price: '99.99',
    method: 'Credit Card',
    address: '123 Main St, City, State 12345',
    date: '2026-03-20'
});

if (result.success) {
    alert('Email sent!');
} else {
    alert('Error: ' + result.message);
}
```

---

## 3. Send Order Shipped Email

```javascript
function sendOrderShipped(email, name, orderId, trackingNumber, estimatedDelivery) {
    emailjs.send('YOUR_SERVICE_ID', 'order_shipped', {
        to_email: email,
        to_name: name,
        order_id: orderId,
        tracking_number: trackingNumber,
        estimated_delivery: estimatedDelivery
    })
    .then(() => console.log('Shipped email sent!'))
    .catch(error => console.error('Error:', error));
}

// Usage
sendOrderShipped(
    'customer@example.com',
    'John Doe',
    'ORD-12345',
    'TRK123456789',
    '2026-03-27'
);
```

---

## 4. Send Order Delivered Email

```javascript
function sendOrderDelivered(email, name, orderId, deliveryDate) {
    emailjs.send('YOUR_SERVICE_ID', 'order_delivered', {
        to_email: email,
        to_name: name,
        order_id: orderId,
        delivery_date: deliveryDate
    })
    .then(() => console.log('Delivered email sent!'))
    .catch(error => console.error('Error:', error));
}

// Usage
sendOrderDelivered(
    'customer@example.com',
    'John Doe',
    'ORD-12345',
    '2026-03-25'
);
```

---

## 5. Send Payment Receipt Email

```javascript
function sendPaymentReceipt(email, name, orderId, totalPrice, method, subtotal, shipping, tax) {
    emailjs.send('YOUR_SERVICE_ID', 'payment_receipt', {
        to_email: email,
        to_name: name,
        order_id: orderId,
        total_price: totalPrice,
        payment_method: method,
        subtotal: subtotal,
        shipping_cost: shipping,
        tax: tax
    })
    .then(() => console.log('Receipt email sent!'))
    .catch(error => console.error('Error:', error));
}

// Usage
sendPaymentReceipt(
    'customer@example.com',
    'John Doe',
    'ORD-12345',
    '99.99',
    'Credit Card',
    '89.99',
    '10.00',
    '8.00'
);
```

---

## 6. Send Order Cancelled Email

```javascript
function sendOrderCancelled(email, name, orderId, refundAmount, reason) {
    emailjs.send('YOUR_SERVICE_ID', 'order_cancelled', {
        to_email: email,
        to_name: name,
        order_id: orderId,
        refund_amount: refundAmount,
        cancellation_reason: reason
    })
    .then(() => console.log('Cancellation email sent!'))
    .catch(error => console.error('Error:', error));
}

// Usage
sendOrderCancelled(
    'customer@example.com',
    'John Doe',
    'ORD-12345',
    '99.99',
    'Out of stock'
);
```

---

## 7. Email Service Module (Recommended)

### emailService.js
```javascript
import emailjs from '@emailjs/browser';

// Configuration
const CONFIG = {
    PUBLIC_KEY: process.env.REACT_APP_EMAILJS_PUBLIC_KEY,
    SERVICE_ID: process.env.REACT_APP_EMAILJS_SERVICE_ID,
    TEMPLATES: {
        ORDER_CONFIRMATION: 'order_confirmation',
        ORDER_SHIPPED: 'order_shipped',
        ORDER_DELIVERED: 'order_delivered',
        PAYMENT_RECEIPT: 'payment_receipt',
        ORDER_CANCELLED: 'order_cancelled'
    }
};

// Initialize
emailjs.init(CONFIG.PUBLIC_KEY);

// Send Order Confirmation
export const sendOrderConfirmation = (orderData) => {
    return emailjs.send(CONFIG.SERVICE_ID, CONFIG.TEMPLATES.ORDER_CONFIRMATION, {
        to_email: orderData.email,
        to_name: orderData.name,
        order_id: orderData.orderId,
        total_price: orderData.totalPrice,
        payment_method: orderData.paymentMethod,
        delivery_address: orderData.deliveryAddress,
        order_date: orderData.orderDate
    });
};

// Send Order Shipped
export const sendOrderShipped = (orderData) => {
    return emailjs.send(CONFIG.SERVICE_ID, CONFIG.TEMPLATES.ORDER_SHIPPED, {
        to_email: orderData.email,
        to_name: orderData.name,
        order_id: orderData.orderId,
        tracking_number: orderData.trackingNumber,
        estimated_delivery: orderData.estimatedDelivery
    });
};

// Send Order Delivered
export const sendOrderDelivered = (orderData) => {
    return emailjs.send(CONFIG.SERVICE_ID, CONFIG.TEMPLATES.ORDER_DELIVERED, {
        to_email: orderData.email,
        to_name: orderData.name,
        order_id: orderData.orderId,
        delivery_date: orderData.deliveryDate
    });
};

// Send Payment Receipt
export const sendPaymentReceipt = (orderData) => {
    return emailjs.send(CONFIG.SERVICE_ID, CONFIG.TEMPLATES.PAYMENT_RECEIPT, {
        to_email: orderData.email,
        to_name: orderData.name,
        order_id: orderData.orderId,
        total_price: orderData.totalPrice,
        payment_method: orderData.paymentMethod,
        subtotal: orderData.subtotal,
        shipping_cost: orderData.shippingCost,
        tax: orderData.tax
    });
};

// Send Order Cancelled
export const sendOrderCancelled = (orderData) => {
    return emailjs.send(CONFIG.SERVICE_ID, CONFIG.TEMPLATES.ORDER_CANCELLED, {
        to_email: orderData.email,
        to_name: orderData.name,
        order_id: orderData.orderId,
        refund_amount: orderData.refundAmount,
        cancellation_reason: orderData.cancellationReason
    });
};

// Generic send function
export const sendEmail = (templateId, templateParams) => {
    return emailjs.send(CONFIG.SERVICE_ID, templateId, templateParams);
};
```

### Usage in Component
```javascript
import { sendOrderConfirmation, sendOrderShipped } from './emailService';

function OrderPage() {
    const handleConfirmOrder = async () => {
        try {
            await sendOrderConfirmation({
                email: 'customer@example.com',
                name: 'John Doe',
                orderId: 'ORD-12345',
                totalPrice: '99.99',
                paymentMethod: 'Credit Card',
                deliveryAddress: '123 Main St, City, State 12345',
                orderDate: '2026-03-20'
            });
            alert('Confirmation email sent!');
        } catch (error) {
            alert('Error: ' + error.text);
        }
    };

    const handleShipOrder = async () => {
        try {
            await sendOrderShipped({
                email: 'customer@example.com',
                name: 'John Doe',
                orderId: 'ORD-12345',
                trackingNumber: 'TRK123456789',
                estimatedDelivery: '2026-03-27'
            });
            alert('Shipped email sent!');
        } catch (error) {
            alert('Error: ' + error.text);
        }
    };

    return (
        <div>
            <button onClick={handleConfirmOrder}>Send Confirmation</button>
            <button onClick={handleShipOrder}>Send Shipped Notification</button>
        </div>
    );
}

export default OrderPage;
```

---

## 8. Environment Variables Setup

### .env File
```
REACT_APP_EMAILJS_PUBLIC_KEY=abc123def456ghi789jkl...
REACT_APP_EMAILJS_SERVICE_ID=gmail_service_123
```

### .env.example File
```
REACT_APP_EMAILJS_PUBLIC_KEY=your_public_key_here
REACT_APP_EMAILJS_SERVICE_ID=your_service_id_here
```

### Access in Code
```javascript
const publicKey = process.env.REACT_APP_EMAILJS_PUBLIC_KEY;
const serviceId = process.env.REACT_APP_EMAILJS_SERVICE_ID;
```

---

## 9. Form Integration Example

### HTML Form
```html
<form id="emailForm">
    <div>
        <label>Email:</label>
        <input type="email" id="email" required>
    </div>
    <div>
        <label>Name:</label>
        <input type="text" id="name" required>
    </div>
    <div>
        <label>Order ID:</label>
        <input type="text" id="orderId" required>
    </div>
    <div>
        <label>Total Price:</label>
        <input type="number" id="price" required>
    </div>
    <div>
        <label>Payment Method:</label>
        <input type="text" id="method" required>
    </div>
    <div>
        <label>Delivery Address:</label>
        <textarea id="address" required></textarea>
    </div>
    <button type="submit">Send Email</button>
</form>
```

### JavaScript Handler
```javascript
import emailjs from '@emailjs/browser';

emailjs.init('YOUR_PUBLIC_KEY');

document.getElementById('emailForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const templateParams = {
        to_email: document.getElementById('email').value,
        to_name: document.getElementById('name').value,
        order_id: document.getElementById('orderId').value,
        total_price: document.getElementById('price').value,
        payment_method: document.getElementById('method').value,
        delivery_address: document.getElementById('address').value,
        order_date: new Date().toLocaleDateString()
    };

    try {
        await emailjs.send('YOUR_SERVICE_ID', 'order_confirmation', templateParams);
        alert('Email sent successfully!');
        document.getElementById('emailForm').reset();
    } catch (error) {
        alert('Error: ' + error.text);
    }
});
```

---

## 10. React Hook for Email Sending

### useEmailJS.js
```javascript
import { useState } from 'react';
import emailjs from '@emailjs/browser';

export const useEmailJS = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const sendEmail = async (templateId, templateParams) => {
        setLoading(true);
        setError(null);

        try {
            const response = await emailjs.send(
                process.env.REACT_APP_EMAILJS_SERVICE_ID,
                templateId,
                templateParams
            );
            setLoading(false);
            return { success: true, response };
        } catch (err) {
            setError(err.text);
            setLoading(false);
            return { success: false, error: err.text };
        }
    };

    return { sendEmail, loading, error };
};
```

### Usage in Component
```javascript
import { useEmailJS } from './useEmailJS';

function OrderConfirmation() {
    const { sendEmail, loading, error } = useEmailJS();

    const handleSendEmail = async () => {
        const result = await sendEmail('order_confirmation', {
            to_email: 'customer@example.com',
            to_name: 'John Doe',
            order_id: 'ORD-12345',
            total_price: '99.99',
            payment_method: 'Credit Card',
            delivery_address: '123 Main St, City, State 12345',
            order_date: '2026-03-20'
        });

        if (result.success) {
            alert('Email sent!');
        } else {
            alert('Error: ' + result.error);
        }
    };

    return (
        <div>
            <button onClick={handleSendEmail} disabled={loading}>
                {loading ? 'Sending...' : 'Send Email'}
            </button>
            {error && <p style={{ color: 'red' }}>{error}</p>}
        </div>
    );
}

export default OrderConfirmation;
```

---

## 11. Error Handling Best Practices

```javascript
async function sendEmailWithRetry(templateId, templateParams, maxRetries = 3) {
    for (let i = 0; i < maxRetries; i++) {
        try {
            const response = await emailjs.send(
                process.env.REACT_APP_EMAILJS_SERVICE_ID,
                templateId,
                templateParams
            );
            console.log('Email sent successfully:', response);
            return { success: true, response };
        } catch (error) {
            console.error(`Attempt ${i + 1} failed:`, error);
            
            if (i === maxRetries - 1) {
                // Last attempt failed
                return { 
                    success: false, 
                    error: error.text,
                    message: 'Failed to send email after multiple attempts'
                };
            }
            
            // Wait before retrying
            await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
        }
    }
}

// Usage
const result = await sendEmailWithRetry('order_confirmation', {
    to_email: 'customer@example.com',
    to_name: 'John Doe',
    order_id: 'ORD-12345',
    total_price: '99.99',
    payment_method: 'Credit Card',
    delivery_address: '123 Main St, City, State 12345',
    order_date: '2026-03-20'
});

if (!result.success) {
    console.error(result.message);
}
```

---

## 12. Validation Before Sending

```javascript
function validateEmailData(data) {
    const errors = [];

    if (!data.to_email || !data.to_email.includes('@')) {
        errors.push('Invalid email address');
    }
    if (!data.to_name || data.to_name.trim() === '') {
        errors.push('Name is required');
    }
    if (!data.order_id || data.order_id.trim() === '') {
        errors.push('Order ID is required');
    }
    if (!data.total_price || isNaN(data.total_price)) {
        errors.push('Valid price is required');
    }
    if (!data.delivery_address || data.delivery_address.trim() === '') {
        errors.push('Delivery address is required');
    }

    return {
        isValid: errors.length === 0,
        errors
    };
}

// Usage
const validation = validateEmailData({
    to_email: 'customer@example.com',
    to_name: 'John Doe',
    order_id: 'ORD-12345',
    total_price: '99.99',
    delivery_address: '123 Main St, City, State 12345'
});

if (!validation.isValid) {
    console.error('Validation errors:', validation.errors);
} else {
    // Send email
}
```

---

## Quick Reference

| Task | Code |
|------|------|
| Initialize | `emailjs.init('PUBLIC_KEY')` |
| Send Email | `emailjs.send(SERVICE_ID, TEMPLATE_ID, params)` |
| Get Public Key | Dashboard → Account → API Keys |
| Get Service ID | Dashboard → Email Services |
| Get Template ID | Dashboard → Email Templates |
| Test Template | Click "Test It" in template editor |

---

**Last Updated:** March 20, 2026
**Version:** 1.0
