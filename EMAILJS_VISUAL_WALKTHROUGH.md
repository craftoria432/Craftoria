# EmailJS Visual Walkthrough - Step by Step with Screenshots

## 🎯 Complete Visual Guide

---

## STEP 1: Create Account & Get Keys

### 1.1 Sign Up
```
URL: https://www.emailjs.com/
┌─────────────────────────────────┐
│  EmailJS Dashboard              │
│                                 │
│  [Sign Up Free] [Sign In]       │
│                                 │
│  Enter Email: ___________       │
│  Enter Password: ___________    │
│  [Create Account]               │
└─────────────────────────────────┘
```

### 1.2 Get Public Key
```
Dashboard → Account (top right) → API Keys
┌─────────────────────────────────┐
│  API Keys                       │
│                                 │
│  Public Key:                    │
│  abc123def456ghi789jkl...      │
│  [Copy]                         │
│                                 │
│  Private Key:                   │
│  (Keep this secret!)            │
└─────────────────────────────────┘

SAVE THIS: abc123def456ghi789jkl...
```

---

## STEP 2: Connect Email Service

### 2.1 Add Email Service
```
Dashboard → Email Services (left sidebar)
┌─────────────────────────────────┐
│  Email Services                 │
│                                 │
│  [+ Add Service]                │
│                                 │
│  Connected Services:            │
│  (none yet)                     │
└─────────────────────────────────┘
```

### 2.2 Choose Provider
```
┌─────────────────────────────────┐
│  Select Email Service           │
│                                 │
│  ☐ Gmail                        │
│  ☐ Outlook                      │
│  ☐ Yahoo                        │
│  ☐ Custom SMTP                  │
│                                 │
│  [Select Gmail]                 │
└─────────────────────────────────┘
```

### 2.3 Connect Gmail
```
┌─────────────────────────────────┐
│  Connect Gmail                  │
│                                 │
│  [Connect with Gmail]           │
│                                 │
│  (Opens Google login)           │
│  Sign in with your Gmail        │
│  Allow EmailJS access           │
│                                 │
│  Service ID: gmail_service_123  │
│  [Copy]                         │
└─────────────────────────────────┘

SAVE THIS: gmail_service_123
```

### 2.4 Test Connection
```
┌─────────────────────────────────┐
│  Test Email Service             │
│                                 │
│  Email: your-email@gmail.com    │
│  [Send Test Email]              │
│                                 │
│  ✓ Test email sent successfully!│
└─────────────────────────────────┘
```

---

## STEP 3: Create Email Template

### 3.1 Go to Templates
```
Dashboard → Email Templates (left sidebar)
┌─────────────────────────────────┐
│  Email Templates                │
│                                 │
│  [+ Create New Template]        │
│                                 │
│  My Templates:                  │
│  (none yet)                     │
└─────────────────────────────────┘
```

### 3.2 Create New Template
```
[+ Create New Template]
┌─────────────────────────────────┐
│  New Email Template             │
│                                 │
│  Template Name:                 │
│  Order Confirmation             │
│                                 │
│  Template ID:                   │
│  order_confirmation             │
│  (auto-generated)               │
│                                 │
│  [Create]                       │
└─────────────────────────────────┘
```

### 3.3 Template Settings
```
┌─────────────────────────────────┐
│  Template Settings              │
│                                 │
│  Subject:                       │
│  Order Confirmation - {{order_id}}
│                                 │
│  To Email:                      │
│  {{to_email}}                   │
│                                 │
│  From Name:                     │
│  Craftoria Support              │
│                                 │
│  From Email:                    │
│  (auto-filled)                  │
│                                 │
│  [Save]                         │
└─────────────────────────────────┘
```

---

## STEP 4: Add Email Content with Variables

### 4.1 Edit Content
```
Template Editor
┌─────────────────────────────────┐
│  [Desktop] [Mobile]             │
│                                 │
│  [Edit Content]                 │
│                                 │
│  HTML Editor:                   │
│  ┌─────────────────────────────┐│
│  │ <html>                      ││
│  │ <body>                      ││
│  │ <p>Hi {{to_name}},</p>     ││
│  │ <p>Order: {{order_id}}</p> ││
│  │ </body>                     ││
│  │ </html>                     ││
│  └─────────────────────────────┘│
│                                 │
│  [Save]                         │
└─────────────────────────────────┘
```

### 4.2 Complete HTML Template
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial; }
        .container { max-width: 600px; margin: 0 auto; }
        .header { background: #667eea; color: white; padding: 20px; }
        .content { padding: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Order Confirmed!</h1>
        </div>
        <div class="content">
            <p>Hi {{to_name}},</p>
            
            <p><strong>Order ID:</strong> {{order_id}}</p>
            <p><strong>Order Date:</strong> {{order_date}}</p>
            <p><strong>Total Price:</strong> ${{total_price}}</p>
            <p><strong>Payment Method:</strong> {{payment_method}}</p>
            <p><strong>Delivery Address:</strong><br>{{delivery_address}}</p>
            
            <p>Thank you for your order!</p>
        </div>
    </div>
</body>
</html>
```

---

## STEP 5: Test Template

### 5.1 Send Test Email
```
Template Editor
┌─────────────────────────────────┐
│  [Test It]                      │
│                                 │
│  Fill in test values:           │
│                                 │
│  to_email:                      │
│  your-email@gmail.com           │
│                                 │
│  to_name:                       │
│  John Doe                       │
│                                 │
│  order_id:                      │
│  ORD-12345                      │
│                                 │
│  order_date:                    │
│  2026-03-20                     │
│                                 │
│  total_price:                   │
│  99.99                          │
│                                 │
│  payment_method:                │
│  Credit Card                    │
│                                 │
│  delivery_address:              │
│  123 Main St, City, State 12345 │
│                                 │
│  [Send]                         │
└─────────────────────────────────┘
```

### 5.2 Check Email
```
Gmail Inbox
┌─────────────────────────────────┐
│  From: Craftoria Support        │
│  Subject: Order Confirmation... │
│  Date: Today                    │
│                                 │
│  ✓ Email received!              │
│                                 │
│  Content:                       │
│  Hi John Doe,                   │
│  Order ID: ORD-12345            │
│  Order Date: 2026-03-20         │
│  Total Price: $99.99            │
│  Payment Method: Credit Card    │
│  Delivery Address:              │
│  123 Main St, City, State 12345 │
│                                 │
│  Thank you for your order!      │
└─────────────────────────────────┘
```

---

## STEP 6: Get Template ID

### 6.1 Find Template ID
```
Email Templates → Your Template
┌─────────────────────────────────┐
│  Order Confirmation             │
│                                 │
│  Template ID:                   │
│  order_confirmation             │
│  [Copy]                         │
│                                 │
│  Service ID:                    │
│  gmail_service_123              │
│  [Copy]                         │
└─────────────────────────────────┘

SAVE THESE:
- Service ID: gmail_service_123
- Template ID: order_confirmation
- Public Key: abc123def456ghi789jkl...
```

---

## STEP 7: Integrate into Your Code

### 7.1 Install Package
```bash
npm install @emailjs/browser
```

### 7.2 Create .env File
```
.env
─────────────────────────────────
REACT_APP_EMAILJS_PUBLIC_KEY=abc123def456ghi789jkl...
REACT_APP_EMAILJS_SERVICE_ID=gmail_service_123
REACT_APP_EMAILJS_TEMPLATE_ID=order_confirmation
```

### 7.3 Create Email Service
```javascript
// emailService.js
import emailjs from '@emailjs/browser';

const PUBLIC_KEY = process.env.REACT_APP_EMAILJS_PUBLIC_KEY;
const SERVICE_ID = process.env.REACT_APP_EMAILJS_SERVICE_ID;
const TEMPLATE_ID = process.env.REACT_APP_EMAILJS_TEMPLATE_ID;

// Initialize
emailjs.init(PUBLIC_KEY);

export const sendOrderConfirmation = (orderData) => {
    const templateParams = {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        order_date: orderData.orderDate,
        total_price: orderData.totalPrice,
        payment_method: orderData.paymentMethod,
        delivery_address: orderData.deliveryAddress
    };

    return emailjs.send(SERVICE_ID, TEMPLATE_ID, templateParams);
};
```

### 7.4 Use in Your App
```javascript
// OrderConfirmation.jsx
import { sendOrderConfirmation } from './emailService';

function OrderConfirmation() {
    const handleSendEmail = async () => {
        const orderData = {
            customerEmail: 'customer@example.com',
            customerName: 'John Doe',
            orderId: 'ORD-12345',
            orderDate: '2026-03-20',
            totalPrice: '99.99',
            paymentMethod: 'Credit Card',
            deliveryAddress: '123 Main St, City, State 12345'
        };

        try {
            await sendOrderConfirmation(orderData);
            alert('Email sent successfully!');
        } catch (error) {
            alert('Failed to send email: ' + error.text);
        }
    };

    return (
        <button onClick={handleSendEmail}>
            Send Confirmation Email
        </button>
    );
}

export default OrderConfirmation;
```

---

## STEP 8: Create Multiple Templates

### 8.1 Order Shipped Template
```
Template Name: Order Shipped
Template ID: order_shipped
Subject: Your Order {{order_id}} Has Shipped!
To Email: {{to_email}}

Content:
Hi {{to_name}},
Your order {{order_id}} has shipped!
Tracking: {{tracking_number}}
Estimated Delivery: {{estimated_delivery}}
```

### 8.2 Order Delivered Template
```
Template Name: Order Delivered
Template ID: order_delivered
Subject: Your Order {{order_id}} Has Been Delivered
To Email: {{to_email}}

Content:
Hi {{to_name}},
Your order {{order_id}} has been delivered!
Delivered to: {{delivery_address}}
```

### 8.3 Payment Receipt Template
```
Template Name: Payment Receipt
Template ID: payment_receipt
Subject: Payment Receipt - {{order_id}}
To Email: {{to_email}}

Content:
Hi {{to_name}},
Payment received for order {{order_id}}
Amount: ${{total_price}}
Method: {{payment_method}}
```

---

## Quick Checklist

- [ ] Create EmailJS account
- [ ] Get Public Key
- [ ] Connect email service (Gmail)
- [ ] Get Service ID
- [ ] Create template with variables
- [ ] Test template with sample data
- [ ] Get Template ID
- [ ] Install @emailjs/browser
- [ ] Create .env file with keys
- [ ] Create email service file
- [ ] Integrate into your app
- [ ] Test sending real emails

---

## Common Variables Reference

```
{{to_email}}           → Recipient email
{{to_name}}            → Recipient name
{{order_id}}           → Order identifier
{{order_date}}         → Order date
{{total_price}}        → Total amount
{{payment_method}}     → Payment type
{{delivery_address}}   → Shipping address
{{tracking_number}}    → Shipping tracking
{{estimated_delivery}} → Expected delivery date
{{refund_amount}}      → Refund amount
{{cancellation_reason}}→ Reason for cancellation
```

---

**Last Updated:** March 20, 2026
**Version:** 1.0
