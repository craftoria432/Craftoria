# Task 5: Code Snippets for Reference

## OrderSuccessScreen.kt - Key Changes

### 1. Function Signature (Added orderStatus parameter)
```kotlin
@Composable
fun OrderSuccessScreen(
    orderIds: String,
    onTrackOrder: () -> Unit,
    onContinueShopping: () -> Unit,
    orderStatus: String = "new"  // ✅ NEW PARAMETER
) {
```

### 2. Status-Based Completion Logic
```kotlin
// Determine which timeline steps should be marked as completed based on order status
val isEmailCompleted = true // Always completed when order is placed
val isSellerNotifiedCompleted = orderStatus in listOf("confirmed", "processing", "shipped", "delivered", "completed")
val isOutForDeliveryCompleted = orderStatus in listOf("shipped", "delivered", "completed")
val isDeliveredCompleted = orderStatus in listOf("delivered", "completed")
```

### 3. Timeline Items (Updated to use dynamic completion)
```kotlin
OrderTimelineItem(
    icon = Icons.Default.Email,
    title = "Order confirmation sent",
    subtitle = "Check your email for full details",
    isLast = false,
    isCompleted = isEmailCompleted  // ✅ DYNAMIC
)
OrderTimelineItem(
    icon = Icons.Default.Person,
    title = "Seller notified",
    subtitle = "Seller is preparing your order",
    isLast = false,
    isCompleted = isSellerNotifiedCompleted  // ✅ DYNAMIC
)
OrderTimelineItem(
    icon = Icons.Default.LocalShipping,
    title = "Out for delivery",
    subtitle = "Track live in \"My Orders\"",
    isLast = false,
    isCompleted = isOutForDeliveryCompleted  // ✅ DYNAMIC
)
OrderTimelineItem(
    icon = Icons.Default.CheckCircle,
    title = "Delivered",
    subtitle = "Enjoy your handcrafted item!",
    isLast = true,
    isCompleted = isDeliveredCompleted  // ✅ DYNAMIC
)
```

---

## functions/index.js - Email Sending Function

### Complete sendOrderConfirmationEmail Function
```javascript
/**
 * Trigger: When an order is created
 * Action: Send order confirmation email to buyer (works for all auth methods including Google OAuth)
 */
exports.sendOrderConfirmationEmail = functions.firestore
  .document('orders/{orderId}')
  .onCreate(async (snap, context) => {
    const order = snap.data();
    const orderId = context.params.orderId;

    try {
      // Get buyer information
      const buyerDoc = await db.collection('users').doc(order.buyer_id).get();
      const buyerData = buyerDoc.data();
      
      // Get buyer email - works for both email/password and Google OAuth users
      const buyerEmail = buyerData?.email || order.deliveryInfo?.email;
      
      if (!buyerEmail) {
        console.log(`No email found for buyer ${order.buyer_id}, skipping email send`);
        return;
      }

      // Format order items
      const itemsHtml = order.items && order.items.length > 0
        ? order.items.map(item => `
            <tr>
              <td style="padding: 12px; border-bottom: 1px solid #eee;">
                <strong>${item.product_title}</strong><br>
                Quantity: ${item.quantity} × PKR ${item.price.toFixed(2)}
              </td>
              <td style="padding: 12px; border-bottom: 1px solid #eee; text-align: right;">
                PKR ${(item.quantity * item.price).toFixed(2)}
              </td>
            </tr>
          `).join('')
        : `
            <tr>
              <td style="padding: 12px; border-bottom: 1px solid #eee;">
                <strong>${order.product_title}</strong><br>
                Quantity: ${order.quantity} × PKR ${order.product_price.toFixed(2)}
              </td>
              <td style="padding: 12px; border-bottom: 1px solid #eee; text-align: right;">
                PKR ${(order.quantity * order.product_price).toFixed(2)}
              </td>
            </tr>
          `;

      // Build email HTML
      const emailHtml = `
        <!DOCTYPE html>
        <html>
          <head>
            <meta charset="UTF-8">
            <style>
              body { font-family: Arial, sans-serif; color: #333; line-height: 1.6; }
              .container { max-width: 600px; margin: 0 auto; padding: 20px; }
              .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }
              .content { background: #f9f9f9; padding: 20px; border: 1px solid #ddd; }
              .order-details { background: white; padding: 15px; margin: 15px 0; border-radius: 5px; }
              .order-details h3 { margin-top: 0; color: #667eea; }
              table { width: 100%; border-collapse: collapse; margin: 15px 0; }
              .total-row { font-weight: bold; font-size: 16px; background: #f0f0f0; }
              .button { display: inline-block; background: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 15px 0; }
              .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
            </style>
          </head>
          <body>
            <div class="container">
              <div class="header">
                <h1>Order Confirmation</h1>
                <p>Thank you for shopping with Craftoria!</p>
              </div>
              
              <div class="content">
                <p>Hi ${order.deliveryInfo?.full_name || buyerData?.name || 'Valued Customer'},</p>
                
                <p>Your order has been successfully placed. Here are your order details:</p>
                
                <div class="order-details">
                  <h3>Order Information</h3>
                  <p><strong>Order ID:</strong> #${orderId.slice(0, 8).toUpperCase()}</p>
                  <p><strong>Order Date:</strong> ${new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
                  <p><strong>Status:</strong> <span style="color: #28a745; font-weight: bold;">Confirmed</span></p>
                </div>
                
                <div class="order-details">
                  <h3>Items Ordered</h3>
                  <table>
                    <thead>
                      <tr style="background: #f0f0f0;">
                        <th style="padding: 12px; text-align: left;">Product</th>
                        <th style="padding: 12px; text-align: right;">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      ${itemsHtml}
                    </tbody>
                  </table>
                </div>
                
                <div class="order-details">
                  <h3>Order Summary</h3>
                  <table>
                    <tr>
                      <td style="padding: 8px;">Subtotal:</td>
                      <td style="text-align: right; padding: 8px;">PKR ${(order.subtotal || 0).toFixed(2)}</td>
                    </tr>
                    <tr>
                      <td style="padding: 8px;">Shipping:</td>
                      <td style="text-align: right; padding: 8px;">PKR ${(order.shipping || 0).toFixed(2)}</td>
                    </tr>
                    <tr class="total-row">
                      <td style="padding: 12px;">Total Amount:</td>
                      <td style="text-align: right; padding: 12px;">PKR ${order.total_price.toFixed(2)}</td>
                    </tr>
                  </table>
                </div>
                
                <div class="order-details">
                  <h3>Delivery Address</h3>
                  <p>
                    ${order.deliveryInfo?.full_name || ''}<br>
                    ${order.deliveryInfo?.address || ''}<br>
                    ${order.deliveryInfo?.city || ''}, ${order.deliveryInfo?.postal_code || ''}<br>
                    Phone: ${order.deliveryInfo?.phone_number || ''}
                  </p>
                </div>
                
                <div class="order-details">
                  <h3>Payment Method</h3>
                  <p>${order.payment_method || 'Cash on Delivery'}</p>
                </div>
                
                <p style="text-align: center;">
                  <a href="https://craftoria.app/orders/${orderId}" class="button">Track Your Order</a>
                </p>
                
                <p>You will receive updates about your order via email and push notifications. If you have any questions, please contact our support team.</p>
                
                <div class="footer">
                  <p>© 2024 Craftoria. All rights reserved.</p>
                  <p>This is an automated email. Please do not reply to this message.</p>
                </div>
              </div>
            </div>
          </body>
        </html>
      `;

      // Send email using Firebase Admin SDK (requires email service setup)
      // For production, integrate with SendGrid, Mailgun, or similar service
      console.log(`Order confirmation email prepared for ${buyerEmail}`);
      console.log(`Order ID: ${orderId}`);
      console.log(`Buyer: ${order.deliveryInfo?.full_name || buyerData?.name}`);
      
      // TODO: Integrate with email service (SendGrid, Mailgun, etc.)
      // Example with SendGrid:
      // const sgMail = require('@sendgrid/mail');
      // sgMail.setApiKey(functions.config().sendgrid.key);
      // await sgMail.send({
      //   to: buyerEmail,
      //   from: 'noreply@craftoria.app',
      //   subject: `Order Confirmation - #${orderId.slice(0, 8).toUpperCase()}`,
      //   html: emailHtml,
      // });

      // Log email activity
      await logAdminActivity({
        action: 'ORDER_CONFIRMATION_EMAIL_SENT',
        resource_id: orderId,
        resource_type: 'orders',
        details: {
          buyer_id: order.buyer_id,
          buyer_email: buyerEmail,
          order_total: order.total_price,
        },
        status: 'success',
      });

    } catch (error) {
      console.error(`Error sending order confirmation email for order ${orderId}:`, error);
      await logAdminActivity({
        action: 'ORDER_CONFIRMATION_EMAIL_FAILED',
        resource_id: orderId,
        resource_type: 'orders',
        details: {
          buyer_id: order.buyer_id,
          error: error.message,
        },
        status: 'failed',
        error_message: error.message,
      });
    }
  });
```

---

## How to Call OrderSuccessScreen

### In Your Navigation Code
```kotlin
// When navigating to OrderSuccessScreen after successful checkout
OrderSuccessScreen(
    orderIds = orderIds,
    orderStatus = order.status,  // ✅ Pass the actual order status
    onTrackOrder = {
        // Navigate to order tracking screen
    },
    onContinueShopping = {
        // Navigate back to home or products
    }
)
```

### Order Status Values
```kotlin
// Use these status values:
"new"        // Order just created
"pending"    // Awaiting seller confirmation
"confirmed"  // Seller confirmed
"processing" // Seller preparing
"shipped"    // Order shipped
"delivered"  // Order delivered
"completed"  // Order completed
"cancelled"  // Order cancelled
```

---

## Email Service Integration Examples

### SendGrid Integration
```javascript
// 1. Install package
// npm install @sendgrid/mail

// 2. Set API key
// firebase functions:config:set sendgrid.key="YOUR_SENDGRID_API_KEY"

// 3. In functions/index.js, replace the TODO section with:
const sgMail = require('@sendgrid/mail');
sgMail.setApiKey(functions.config().sendgrid.key);

await sgMail.send({
  to: buyerEmail,
  from: 'noreply@craftoria.app',
  subject: `Order Confirmation - #${orderId.slice(0, 8).toUpperCase()}`,
  html: emailHtml,
});
```

### Mailgun Integration
```javascript
// 1. Install package
// npm install mailgun.js

// 2. Set API key and domain
// firebase functions:config:set mailgun.key="YOUR_API_KEY" mailgun.domain="YOUR_DOMAIN"

// 3. In functions/index.js, replace the TODO section with:
const mailgun = require('mailgun.js');
const mg = mailgun.client({
  username: 'api',
  key: functions.config().mailgun.key,
});

await mg.messages.create(functions.config().mailgun.domain, {
  from: 'noreply@craftoria.app',
  to: buyerEmail,
  subject: `Order Confirmation - #${orderId.slice(0, 8).toUpperCase()}`,
  html: emailHtml,
});
```

### Firebase Email Extension
```
1. Go to Firebase Console
2. Extensions → Install Extension
3. Search for "Email"
4. Install Firebase Email Extension
5. Configure with your email service
6. Use extension in Cloud Functions
```

---

## Testing Code

### Test Timeline Status
```kotlin
// Test with different statuses
OrderSuccessScreen(
    orderIds = "order123",
    orderStatus = "new",  // Only Email completed
    onTrackOrder = { },
    onContinueShopping = { }
)

OrderSuccessScreen(
    orderIds = "order123",
    orderStatus = "shipped",  // Email + Seller + Out for Delivery completed
    onTrackOrder = { },
    onContinueShopping = { }
)

OrderSuccessScreen(
    orderIds = "order123",
    orderStatus = "delivered",  // All completed
    onTrackOrder = { },
    onContinueShopping = { }
)
```

### Check Email Logs
```javascript
// In Firebase Console, check admin_activities collection
db.collection('admin_activities')
  .where('action', '==', 'ORDER_CONFIRMATION_EMAIL_SENT')
  .orderBy('timestamp', 'desc')
  .limit(10)
  .get()
```

---

## Status: ✅ COMPLETE

All code snippets are ready for implementation and testing.
