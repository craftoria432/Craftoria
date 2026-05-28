# Complete EmailJS Setup Guide - Step by Step

## 📋 Table of Contents
1. [Account Setup](#account-setup)
2. [Email Service Configuration](#email-service-configuration)
3. [Creating Email Templates](#creating-email-templates)
4. [Adding Variables](#adding-variables)
5. [Testing Templates](#testing-templates)
6. [Integration Code](#integration-code)

---

## 1. Account Setup

### Step 1: Create EmailJS Account
1. Go to https://www.emailjs.com/
2. Click **"Sign Up Free"**
3. Enter your email and password
4. Verify your email
5. Log in to dashboard

### Step 2: Get Your Public Key
1. Go to **Account** (top right)
2. Click **API Keys**
3. Copy your **Public Key** (you'll need this for frontend code)
4. Save it somewhere safe

---

## 2. Email Service Configuration

### Step 1: Add Email Service
1. In dashboard, click **Email Services** (left sidebar)
2. Click **Add Service**
3. Choose your email provider:
   - **Gmail** (recommended)
   - **Outlook**
   - **Yahoo**
   - **Custom SMTP**

### Step 2: Gmail Setup (Example)
1. Select **Gmail**
2. Click **Connect with Gmail**
3. Sign in with your Gmail account
4. Allow EmailJS to access your account
5. Copy your **Service ID** (you'll need this)

### Step 3: Verify Service
1. Click **Send Test Email**
2. Enter your email
3. Check if you received the test email
4. If yes, service is working!

---

## 3. Creating Email Templates

### Step 1: Go to Email Templates
1. Click **Email Templates** (left sidebar)
2. Click **Create New Template**

### Step 2: Basic Template Setup
Fill in these fields:

```
Template Name: Order Confirmation
Template ID: order_confirmation (auto-generated, you can customize)
Subject: Order Confirmation - {{order_id}}
To Email: {{to_email}}
From Name: Craftoria Support
From Email: (auto-filled from service)
```

### Step 3: Add Email Content
1. Click **Edit Content** button
2. Choose **Desktop** tab
3. Paste your HTML template (see below)
4. Click **Save**

---

## 4. Adding Variables

### Understanding Variables
Variables are placeholders that get replaced with real data when you send the email.

**Format:** `{{variable_name}}`

### Example Variables
```
{{to_email}}          → customer@example.com
{{to_name}}           → John Doe
{{order_id}}          → ORD-12345
{{total_price}}       → 99.99
{{payment_method}}    → Credit Card
{{delivery_address}}  → 123 Main St, City, State 12345
{{order_date}}        → 2026-03-20
```

### How to Use Variables in Template

**In Subject Line:**
```
Order Confirmation - {{order_id}}
```

**In HTML Content:**
```html
<p>Hi {{to_name}},</p>
<p>Your order {{order_id}} has been confirmed.</p>
<p>Total: ${{total_price}}</p>
<p>Delivery to: {{delivery_address}}</p>
```

### Complete Template Example

```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; }
        .container { max-width: 600px; margin: 0 auto; }
        .header { background: #667eea; color: white; padding: 20px; }
        .content { padding: 20px; }
        .detail { margin: 10px 0; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Order Confirmed!</h1>
        </div>
        <div class="content">
            <p>Hi {{to_name}},</p>
            
            <div class="detail">
                <strong>Order ID:</strong> {{order_id}}
            </div>
            <div class="detail">
                <strong>Order Date:</strong> {{order_date}}
            </div>
            <div class="detail">
                <strong>Total Price:</strong> ${{total_price}}
            </div>
            <div class="detail">
                <strong>Payment Method:</strong> {{payment_method}}
            </div>
            <div class="detail">
                <strong>Delivery Address:</strong><br>
                {{delivery_address}}
            </div>
            
            <p style="margin-top: 20px;">
                Thank you for your order!
            </p>
        </div>
    </div>
</body>
</html>
```

---

## 5. Testing Templates

### Step 1: Send Test Email
1. In template editor, click **Test It**
2. Fill in test values for each variable:
   ```
   to_email: your-email@gmail.com
   to_name: John Doe
   order_id: ORD-12345
   total_price: 99.99
   payment_method: Credit Card
   delivery_address: 123 Main St, City, State 12345
   order_date: 2026-03-20
   ```
3. Click **Send**
4. Check your email inbox

### Step 2: Verify Email
- Check if all variables are replaced correctly
- Check if formatting looks good
- Check if links work
- Check on mobile view

---

## 6. Integration Code

### Step 1: Install EmailJS
```bash
npm install @emailjs/browser
```

### Step 2: Initialize EmailJS
```javascript
import emailjs from '@emailjs/browser';

// Initialize with your Public Key
emailjs.init('YOUR_PUBLIC_KEY');
```

### Step 3: Send Email Function

```javascript
// Function to send order confirmation email
function sendOrderConfirmationEmail(orderData) {
    const templateParams = {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        total_price: orderData.totalPrice,
        payment_method: orderData.paymentMethod,
        delivery_address: orderData.deliveryAddress,
        order_date: new Date().toLocaleDateString()
    };

    emailjs.send(
        'YOUR_SERVICE_ID',           // Service ID
        'order_confirmation',         // Template ID
        templateParams
    )
    .then((response) => {
        console.log('Email sent successfully!', response);
        alert('Order confirmation email sent!');
    })
    .catch((error) => {
        console.error('Failed to send email:', error);
        alert('Failed to send email');
    });
}
```

### Step 4: Call the Function
```javascript
// Example usage
const orderData = {
    customerEmail: 'customer@example.com',
    customerName: 'John Doe',
    orderId: 'ORD-12345',
    totalPrice: '99.99',
    paymentMethod: 'Credit Card',
    deliveryAddress: '123 Main St, New York, NY 10001'
};

sendOrderConfirmationEmail(orderData);
```

---

## Complete Working Example

### HTML Form
```html
<form id="orderForm">
    <input type="email" id="email" placeholder="Email" required>
    <input type="text" id="name" placeholder="Name" required>
    <input type="text" id="orderId" placeholder="Order ID" required>
    <input type="number" id="price" placeholder="Total Price" required>
    <input type="text" id="method" placeholder="Payment Method" required>
    <textarea id="address" placeholder="Delivery Address" required></textarea>
    <button type="submit">Send Confirmation Email</button>
</form>
```

### JavaScript Code
```javascript
import emailjs from '@emailjs/browser';

// Initialize
emailjs.init('YOUR_PUBLIC_KEY');

// Handle form submission
document.getElementById('orderForm').addEventListener('submit', function(e) {
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

    emailjs.send(
        'YOUR_SERVICE_ID',
        'order_confirmation',
        templateParams
    )
    .then(() => {
        alert('Email sent successfully!');
        document.getElementById('orderForm').reset();
    })
    .catch((error) => {
        alert('Error: ' + error.text);
    });
});
```

---

## Quick Reference: All Variables

| Variable | Example | Where to Use |
|----------|---------|--------------|
| `{{to_email}}` | user@example.com | To Email field |
| `{{to_name}}` | John Doe | Email body |
| `{{order_id}}` | ORD-12345 | Subject, body |
| `{{total_price}}` | 99.99 | Email body |
| `{{payment_method}}` | Credit Card | Email body |
| `{{delivery_address}}` | 123 Main St... | Email body |
| `{{order_date}}` | 2026-03-20 | Email body |
| `{{tracking_number}}` | TRK123456 | Shipped email |
| `{{estimated_delivery}}` | 2026-03-27 | Shipped email |
| `{{refund_amount}}` | 99.99 | Cancelled email |

---

## Troubleshooting

### Email Not Sending?
1. Check Service ID is correct
2. Check Public Key is correct
3. Check email service is connected
4. Check all required variables are provided
5. Check browser console for errors

### Variables Not Replacing?
1. Check variable names match exactly (case-sensitive)
2. Check variable format: `{{variable_name}}`
3. Check variable is in templateParams object
4. Check template has the variable

### Email Looks Wrong?
1. Check HTML syntax
2. Check CSS is inline (not in separate file)
3. Test on different email clients
4. Check images load properly

---

## Security Tips

1. **Never hardcode API keys** in frontend code
2. Use environment variables:
   ```javascript
   const PUBLIC_KEY = process.env.REACT_APP_EMAILJS_PUBLIC_KEY;
   const SERVICE_ID = process.env.REACT_APP_EMAILJS_SERVICE_ID;
   ```

3. **Create .env file:**
   ```
   REACT_APP_EMAILJS_PUBLIC_KEY=your_public_key
   REACT_APP_EMAILJS_SERVICE_ID=your_service_id
   ```

4. **Use backend for sensitive operations** (recommended for production)

---

## Next Steps

1. ✅ Create EmailJS account
2. ✅ Connect email service
3. ✅ Create templates with variables
4. ✅ Test templates
5. ✅ Integrate into your app
6. ✅ Deploy to production

---

**Last Updated:** March 20, 2026
**Version:** 1.0
