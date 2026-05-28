# Order Confirmation Template - Complete Setup Guide

## 🎯 Create ONLY Order Confirmation Template

This guide shows you exactly how to create the Order Confirmation template in EmailJS with all variables.

---

## STEP 1: Template Settings

### In EmailJS Dashboard:

```
Template Name: Order Confirmation
Template ID: order_confirmation
Subject: Order Confirmation - {{order_id}}
To Email: {{to_email}}
From Name: Craftoria Support
From Email: (auto-filled)
```

---

## STEP 2: Add HTML Content

### Click "Edit Content" and paste this HTML:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Order Confirmation</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background-color: #ffffff;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px 20px;
            text-align: center;
        }
        .header h1 {
            font-size: 28px;
            margin-bottom: 5px;
        }
        .header p {
            font-size: 14px;
            opacity: 0.9;
        }
        .content {
            padding: 40px 20px;
        }
        .greeting {
            font-size: 16px;
            margin-bottom: 20px;
            color: #333;
        }
        .order-details {
            background-color: #f9f9f9;
            border-left: 4px solid #667eea;
            padding: 20px;
            margin: 20px 0;
            border-radius: 4px;
        }
        .detail-row {
            display: flex;
            justify-content: space-between;
            padding: 12px 0;
            border-bottom: 1px solid #e0e0e0;
        }
        .detail-row:last-child {
            border-bottom: none;
        }
        .detail-label {
            font-weight: 600;
            color: #555;
            flex: 0 0 40%;
        }
        .detail-value {
            color: #333;
            flex: 1;
            text-align: right;
        }
        .total-section {
            background-color: #f0f0f0;
            padding: 20px;
            margin: 20px 0;
            border-radius: 4px;
            text-align: center;
        }
        .total-label {
            font-size: 14px;
            color: #666;
            margin-bottom: 10px;
        }
        .total-amount {
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
        }
        .message {
            background-color: #e8f5e9;
            border-left: 4px solid #4caf50;
            padding: 15px;
            margin: 20px 0;
            border-radius: 4px;
            color: #2e7d32;
        }
        .footer {
            background-color: #f5f5f5;
            padding: 20px;
            text-align: center;
            font-size: 12px;
            color: #999;
            border-top: 1px solid #e0e0e0;
        }
        .footer-links {
            margin: 10px 0;
        }
        .footer-links a {
            color: #667eea;
            text-decoration: none;
            margin: 0 10px;
        }
        .divider {
            height: 1px;
            background-color: #e0e0e0;
            margin: 20px 0;
        }
    </style>
</head>
<body>
    <div class="container">
        <!-- Header -->
        <div class="header">
            <h1>✓ Order Confirmed</h1>
            <p>Thank you for your purchase!</p>
        </div>

        <!-- Main Content -->
        <div class="content">
            <!-- Greeting -->
            <div class="greeting">
                <p>Hi {{to_name}},</p>
                <p>Your order has been successfully placed and confirmed. We're excited to get your items to you!</p>
            </div>

            <!-- Order Details -->
            <div class="order-details">
                <div class="detail-row">
                    <span class="detail-label">Order ID:</span>
                    <span class="detail-value"><strong>{{order_id}}</strong></span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Order Date:</span>
                    <span class="detail-value">{{order_date}}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Payment Method:</span>
                    <span class="detail-value">{{payment_method}}</span>
                </div>
            </div>

            <!-- Total Amount -->
            <div class="total-section">
                <div class="total-label">Order Total</div>
                <div class="total-amount">${{total_price}}</div>
            </div>

            <!-- Delivery Address -->
            <div class="order-details">
                <div class="detail-label" style="margin-bottom: 10px;">📍 Delivery Address</div>
                <div class="detail-value" style="text-align: left; color: #555;">
                    {{delivery_address}}
                </div>
            </div>

            <!-- Success Message -->
            <div class="message">
                <strong>✓ Payment Received</strong><br>
                Your payment has been processed successfully. Your order is now being prepared for shipment.
            </div>

            <div class="divider"></div>

            <!-- Additional Info -->
            <p style="font-size: 14px; color: #666; margin: 20px 0;">
                <strong>What's Next?</strong><br>
                • We'll prepare your order for shipment<br>
                • You'll receive a shipping notification with tracking details<br>
                • Estimated delivery: 5-7 business days<br>
                • Questions? Contact our support team anytime
            </p>
        </div>

        <!-- Footer -->
        <div class="footer">
            <p style="margin-bottom: 15px;">Thank you for shopping with Craftoria!</p>
            <div class="footer-links">
                <a href="#">Contact Support</a> | 
                <a href="#">Track Order</a> | 
                <a href="#">Return Policy</a>
            </div>
            <p style="margin-top: 15px; color: #bbb;">
                © 2026 Craftoria. All rights reserved.<br>
                This is an automated message, please do not reply to this email.
            </p>
        </div>
    </div>
</body>
</html>
```

---

## STEP 3: Variables Used

These are the variables in the template:

| Variable | Example | Where Used |
|----------|---------|-----------|
| `{{to_email}}` | customer@example.com | To Email field |
| `{{to_name}}` | John Doe | Greeting |
| `{{order_id}}` | ORD-12345 | Order ID row |
| `{{order_date}}` | 2026-03-20 | Order Date row |
| `{{payment_method}}` | Credit Card | Payment Method row |
| `{{total_price}}` | 99.99 | Total Amount section |
| `{{delivery_address}}` | 123 Main St, City, State 12345 | Delivery Address section |

---

## STEP 4: Test the Template

### In EmailJS:

1. Click **"Test It"** button
2. Fill in test values:

```
to_email: your-email@gmail.com
to_name: John Doe
order_id: ORD-12345
order_date: 2026-03-20
payment_method: Credit Card
total_price: 99.99
delivery_address: 123 Main St, New York, NY 10001
```

3. Click **"Send"**
4. Check your email inbox
5. Verify all variables are replaced correctly

---

## STEP 5: Save Template

1. Click **"Save"** button
2. Copy your **Template ID**: `order_confirmation`
3. Save it for later use

---

## STEP 6: Integration Code

### Install Package:
```bash
npm install @emailjs/browser
```

### Create .env file:
```
REACT_APP_EMAILJS_PUBLIC_KEY=your_public_key_here
REACT_APP_EMAILJS_SERVICE_ID=your_service_id_here
REACT_APP_EMAILJS_TEMPLATE_ID=order_confirmation
```

### Create emailService.js:
```javascript
import emailjs from '@emailjs/browser';

const PUBLIC_KEY = process.env.REACT_APP_EMAILJS_PUBLIC_KEY;
const SERVICE_ID = process.env.REACT_APP_EMAILJS_SERVICE_ID;
const TEMPLATE_ID = process.env.REACT_APP_EMAILJS_TEMPLATE_ID;

// Initialize
emailjs.init(PUBLIC_KEY);

// Send Order Confirmation
export const sendOrderConfirmation = (orderData) => {
    const templateParams = {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        order_date: orderData.orderDate,
        payment_method: orderData.paymentMethod,
        total_price: orderData.totalPrice,
        delivery_address: orderData.deliveryAddress
    };

    return emailjs.send(SERVICE_ID, TEMPLATE_ID, templateParams);
};
```

### Use in Component:
```javascript
import { sendOrderConfirmation } from './emailService';

function OrderSuccess() {
    const handleSendEmail = async () => {
        try {
            await sendOrderConfirmation({
                customerEmail: 'customer@example.com',
                customerName: 'John Doe',
                orderId: 'ORD-12345',
                orderDate: '2026-03-20',
                paymentMethod: 'Credit Card',
                totalPrice: '99.99',
                deliveryAddress: '123 Main St, New York, NY 10001'
            });
            alert('Order confirmation email sent!');
        } catch (error) {
            alert('Error: ' + error.text);
        }
    };

    return (
        <button onClick={handleSendEmail}>
            Send Confirmation Email
        </button>
    );
}

export default OrderSuccess;
```

---

## STEP 7: Send Real Email

### When Order is Placed:

```javascript
// After successful payment
const orderData = {
    customerEmail: user.email,
    customerName: user.name,
    orderId: order.id,
    orderDate: new Date().toLocaleDateString(),
    paymentMethod: order.paymentMethod,
    totalPrice: order.totalPrice,
    deliveryAddress: order.deliveryAddress
};

await sendOrderConfirmation(orderData);
```

---

## ✅ Checklist

- [ ] Created template in EmailJS
- [ ] Set template name: "Order Confirmation"
- [ ] Set template ID: "order_confirmation"
- [ ] Added HTML content
- [ ] Added all 7 variables
- [ ] Tested template with sample data
- [ ] Received test email
- [ ] Verified all variables replaced correctly
- [ ] Saved template
- [ ] Got Template ID
- [ ] Created .env file
- [ ] Created emailService.js
- [ ] Integrated into component
- [ ] Tested sending real email

---

## 📊 Variables Summary

```
7 Variables Total:
1. {{to_email}}           → Recipient email
2. {{to_name}}            → Recipient name
3. {{order_id}}           → Order identifier
4. {{order_date}}         → Order date
5. {{payment_method}}     → Payment type
6. {{total_price}}        → Total amount
7. {{delivery_address}}   → Shipping address
```

---

## 🚀 Quick Reference

| Step | Action |
|------|--------|
| 1 | Create template in EmailJS |
| 2 | Add HTML content |
| 3 | Test with sample data |
| 4 | Save template |
| 5 | Get Template ID |
| 6 | Create .env file |
| 7 | Create emailService.js |
| 8 | Use in component |
| 9 | Send real emails |

---

## 💡 Tips

- Always test template before using in production
- Use environment variables for API keys
- Handle errors gracefully
- Log email sending for debugging
- Keep template ID consistent

---

**Last Updated:** March 20, 2026
**Version:** 1.0
