# Email Templates Guide - Craftoria

Complete email template collection with all required variables for your order management system.

## 📧 Available Templates

### 1. Order Confirmation (`order-confirmation.html`)
**Purpose:** Sent immediately after order placement
**Variables:**
- `{{to_email}}` - Customer email address
- `{{to_name}}` - Customer name
- `{{order_id}}` - Unique order identifier
- `{{total_price}}` - Total order amount
- `{{payment_method}}` - Payment method used (Credit Card, PayPal, etc.)
- `{{delivery_address}}` - Full delivery address
- `{{order_date}}` - Date order was placed

**When to Send:** Immediately after successful payment

---

### 2. Order Shipped (`order-shipped.html`)
**Purpose:** Notify customer when order ships
**Variables:**
- `{{to_email}}` - Customer email address
- `{{to_name}}` - Customer name
- `{{order_id}}` - Order identifier
- `{{order_date}}` - Original order date
- `{{delivery_address}}` - Delivery address
- `{{total_price}}` - Order total
- `{{tracking_number}}` - Shipping tracking number
- `{{estimated_delivery}}` - Expected delivery date

**When to Send:** When order is dispatched from warehouse

---

### 3. Order Delivered (`order-delivered.html`)
**Purpose:** Confirm successful delivery
**Variables:**
- `{{to_email}}` - Customer email address
- `{{to_name}}` - Customer name
- `{{order_id}}` - Order identifier
- `{{order_date}}` - Original order date
- `{{delivery_address}}` - Delivery address
- `{{total_price}}` - Order total
- `{{delivery_date}}` - Date of delivery

**When to Send:** When carrier confirms delivery

---

### 4. Payment Receipt (`payment-receipt.html`)
**Purpose:** Detailed payment receipt for records
**Variables:**
- `{{to_email}}` - Customer email address
- `{{to_name}}` - Customer name
- `{{order_id}}` - Order identifier
- `{{order_date}}` - Order date
- `{{payment_date}}` - Payment processing date
- `{{payment_method}}` - Payment method
- `{{total_price}}` - Total amount paid
- `{{subtotal}}` - Subtotal before taxes/shipping
- `{{shipping_cost}}` - Shipping charge
- `{{tax}}` - Tax amount
- `{{delivery_address}}` - Delivery address
- `{{card_last_four}}` - Last 4 digits of card (if applicable)

**When to Send:** After successful payment processing

---

### 5. Order Cancelled (`order-cancelled.html`)
**Purpose:** Notify customer of order cancellation
**Variables:**
- `{{to_email}}` - Customer email address
- `{{to_name}}` - Customer name
- `{{order_id}}` - Order identifier
- `{{order_date}}` - Original order date
- `{{cancellation_date}}` - Date of cancellation
- `{{total_price}}` - Original order total
- `{{cancellation_reason}}` - Reason for cancellation
- `{{refund_amount}}` - Amount to be refunded
- `{{refund_status}}` - Status of refund (Processing, Completed, etc.)
- `{{refund_date}}` - Expected refund date
- `{{payment_method}}` - Original payment method

**When to Send:** When order is cancelled

---

## 🔧 Integration with EmailJS

### Setup Steps:

1. **Create EmailJS Account**
   - Go to [EmailJS Dashboard](https://dashboard.emailjs.com)
   - Sign up or log in

2. **Add Email Service**
   - Navigate to "Email Services"
   - Connect your email provider (Gmail, Outlook, etc.)
   - Note your Service ID

3. **Create Templates**
   - Go to "Email Templates"
   - Click "Create New Template"
   - Copy content from the HTML files
   - Replace variables with EmailJS template syntax

4. **Get Template IDs**
   - Each template will have a unique Template ID
   - Save these IDs for your backend

### Example EmailJS Setup:

```javascript
// Initialize EmailJS
emailjs.init('YOUR_PUBLIC_KEY');

// Send Order Confirmation
function sendOrderConfirmation(orderData) {
    emailjs.send('YOUR_SERVICE_ID', 'order_confirmation_template', {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        total_price: orderData.totalPrice,
        payment_method: orderData.paymentMethod,
        delivery_address: orderData.deliveryAddress,
        order_date: new Date().toLocaleDateString()
    });
}

// Send Order Shipped
function sendOrderShipped(orderData) {
    emailjs.send('YOUR_SERVICE_ID', 'order_shipped_template', {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        order_date: orderData.orderDate,
        delivery_address: orderData.deliveryAddress,
        total_price: orderData.totalPrice,
        tracking_number: orderData.trackingNumber,
        estimated_delivery: orderData.estimatedDelivery
    });
}

// Send Order Delivered
function sendOrderDelivered(orderData) {
    emailjs.send('YOUR_SERVICE_ID', 'order_delivered_template', {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        order_date: orderData.orderDate,
        delivery_address: orderData.deliveryAddress,
        total_price: orderData.totalPrice,
        delivery_date: new Date().toLocaleDateString()
    });
}

// Send Payment Receipt
function sendPaymentReceipt(orderData) {
    emailjs.send('YOUR_SERVICE_ID', 'payment_receipt_template', {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        order_date: orderData.orderDate,
        payment_date: new Date().toLocaleDateString(),
        payment_method: orderData.paymentMethod,
        total_price: orderData.totalPrice,
        subtotal: orderData.subtotal,
        shipping_cost: orderData.shippingCost,
        tax: orderData.tax,
        delivery_address: orderData.deliveryAddress,
        card_last_four: orderData.cardLastFour
    });
}

// Send Order Cancelled
function sendOrderCancelled(orderData) {
    emailjs.send('YOUR_SERVICE_ID', 'order_cancelled_template', {
        to_email: orderData.customerEmail,
        to_name: orderData.customerName,
        order_id: orderData.orderId,
        order_date: orderData.orderDate,
        cancellation_date: new Date().toLocaleDateString(),
        total_price: orderData.totalPrice,
        cancellation_reason: orderData.cancellationReason,
        refund_amount: orderData.refundAmount,
        refund_status: orderData.refundStatus,
        refund_date: orderData.refundDate,
        payment_method: orderData.paymentMethod
    });
}
```

---

## 📋 Variable Reference

| Variable | Type | Example | Used In |
|----------|------|---------|---------|
| `{{to_email}}` | String | user@example.com | All |
| `{{to_name}}` | String | John Doe | All |
| `{{order_id}}` | String | ORD-12345 | All |
| `{{total_price}}` | Number | 99.99 | All |
| `{{payment_method}}` | String | Credit Card | All |
| `{{delivery_address}}` | String | 123 Main St... | All |
| `{{order_date}}` | Date | 2026-03-20 | All |
| `{{tracking_number}}` | String | TRK123456 | Shipped |
| `{{estimated_delivery}}` | Date | 2026-03-27 | Shipped |
| `{{delivery_date}}` | Date | 2026-03-25 | Delivered |
| `{{payment_date}}` | Date | 2026-03-20 | Receipt |
| `{{subtotal}}` | Number | 89.99 | Receipt |
| `{{shipping_cost}}` | Number | 10.00 | Receipt |
| `{{tax}}` | Number | 8.00 | Receipt |
| `{{card_last_four}}` | String | 4242 | Receipt |
| `{{cancellation_date}}` | Date | 2026-03-21 | Cancelled |
| `{{cancellation_reason}}` | String | Out of stock | Cancelled |
| `{{refund_amount}}` | Number | 99.99 | Cancelled |
| `{{refund_status}}` | String | Processing | Cancelled |
| `{{refund_date}}` | Date | 2026-03-27 | Cancelled |

---

## 🎨 Customization

### Colors Used:
- **Primary:** `#667eea` (Purple)
- **Success:** `#4caf50` (Green)
- **Info:** `#2196f3` (Blue)
- **Warning:** `#ff9800` (Orange)
- **Error:** `#f44336` (Red)

### To Change Colors:
1. Open the HTML template
2. Find the `<style>` section
3. Replace color codes with your brand colors
4. Update gradient colors in `.header` class

### To Add Logo:
Add this after the opening `<div class="header">`:
```html
<img src="YOUR_LOGO_URL" alt="Craftoria" style="max-width: 150px; margin-bottom: 20px;">
```

---

## ✅ Testing Checklist

- [ ] All variables are correctly mapped
- [ ] Email displays correctly on mobile devices
- [ ] Links are functional
- [ ] Images load properly
- [ ] Colors match brand guidelines
- [ ] Text is readable and properly formatted
- [ ] Footer information is accurate
- [ ] Contact information is correct

---

## 📞 Support

For issues or questions about email templates:
1. Check variable names match exactly
2. Ensure all required variables are provided
3. Test with sample data first
4. Verify email service credentials

---

**Last Updated:** March 20, 2026
**Version:** 1.0
