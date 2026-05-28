# 📋 Copy-Paste Ready - EmailJS Template

## ✅ All 7 Variables Included

Your HTML has all 7 variables ready to paste:

```
1. {{to_email}}           ← In "To Email" field
2. {{to_name}}            ← In greeting
3. {{order_id}}           ← In Order ID
4. {{order_date}}         ← In Order Date
5. {{payment_method}}     ← In Payment Method
6. {{total_price}}        ← In Total Price
7. {{delivery_address}}   ← In Delivery Address
```

---

## 🎯 STEP 1: Create Template

Go to: https://dashboard.emailjs.com

Click: **Email Templates** → **[+ Create New Template]**

---

## 🎯 STEP 2: Fill Template Settings

Copy-paste these exact values:

```
Template Name:    Order Confirmation
Template ID:      order_confirmation
Subject:          Order Confirmation - {{order_id}}
To Email:         {{to_email}}
From Name:        Craftoria Support
```

Click: **[Create]**

---

## 🎯 STEP 3: Add HTML Content

Click: **[Edit Content]**

Clear all existing HTML and paste this (minified, ready to go):

```html
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Order Confirmation</title><style>* {margin: 0;padding: 0;box-sizing: border-box;}body {font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;line-height: 1.6;color: #333;background-color: #f5f5f5;}.container {max-width: 600px;margin: 0 auto;background-color: #ffffff;box-shadow: 0 2px 4px rgba(0,0,0,0.1);}.header {background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);color: white;padding: 40px 20px;text-align: center;}.header h1 {font-size: 28px;margin-bottom: 5px;}.header p {font-size: 14px;opacity: 0.9;}.content {padding: 40px 20px;}.greeting {font-size: 16px;margin-bottom: 20px;color: #333;}.order-details {background-color: #f9f9f9;border-left: 4px solid #667eea;padding: 20px;margin: 20px 0;border-radius: 4px;}.detail-row {display: flex;justify-content: space-between;padding: 12px 0;border-bottom: 1px solid #e0e0e0;}.detail-row:last-child {border-bottom: none;}.detail-label {font-weight: 600;color: #555;flex: 0 0 40%;}.detail-value {color: #333;flex: 1;text-align: right;}.total-section {background-color: #f0f0f0;padding: 20px;margin: 20px 0;border-radius: 4px;text-align: center;}.total-label {font-size: 14px;color: #666;margin-bottom: 10px;}.total-amount {font-size: 32px;font-weight: bold;color: #667eea;}.message {background-color: #e8f5e9;border-left: 4px solid #4caf50;padding: 15px;margin: 20px 0;border-radius: 4px;color: #2e7d32;}.footer {background-color: #f5f5f5;padding: 20px;text-align: center;font-size: 12px;color: #999;border-top: 1px solid #e0e0e0;}.footer-links {margin: 10px 0;}.footer-links a {color: #667eea;text-decoration: none;margin: 0 10px;}.divider {height: 1px;background-color: #e0e0e0;margin: 20px 0;}@media (max-width: 600px) {.container {width: 100%;}.detail-row {flex-direction: column;}.detail-value {text-align: left;margin-top: 5px;}.header h1 {font-size: 24px;}.total-amount {font-size: 28px;}}</style></head><body><div class="container"><div class="header"><h1>✓ Order Confirmed</h1><p>Thank you for your purchase!</p></div><div class="content"><div class="greeting"><p>Hi {{to_name}},</p><p>Your order has been successfully placed and confirmed. We're excited to get your items to you!</p></div><div class="order-details"><div class="detail-row"><span class="detail-label">Order ID:</span><span class="detail-value"><strong>{{order_id}}</strong></span></div><div class="detail-row"><span class="detail-label">Order Date:</span><span class="detail-value">{{order_date}}</span></div><div class="detail-row"><span class="detail-label">Payment Method:</span><span class="detail-value">{{payment_method}}</span></div></div><div class="total-section"><div class="total-label">Order Total</div><div class="total-amount">${{total_price}}</div></div><div class="order-details"><div class="detail-label" style="margin-bottom: 10px;">📍 Delivery Address</div><div class="detail-value" style="text-align: left; color: #555;">{{delivery_address}}</div></div><div class="message"><strong>✓ Payment Received</strong><br>Your payment has been processed successfully. Your order is now being prepared for shipment.</div><center><a href="#" class="cta-button">Track Your Order</a></center><div class="divider"></div><p style="font-size: 14px; color: #666; margin: 20px 0;"><strong>What's Next?</strong><br>• We'll prepare your order for shipment<br>• You'll receive a shipping notification with tracking details<br>• Estimated delivery: 5-7 business days<br>• Questions? Contact our support team anytime</p></div><div class="footer"><p style="margin-bottom: 15px;">Thank you for shopping with Craftoria!</p><div class="footer-links"><a href="#">Contact Support</a> | <a href="#">Track Order</a> | <a href="#">Return Policy</a></div><p style="margin-top: 15px; color: #bbb;">© 2026 Craftoria. All rights reserved.<br>This is an automated message, please do not reply to this email.</p></div></div></body></html>
```

Click: **[Save]**

---

## 🎯 STEP 4: Test Template

Click: **[Test It]**

Fill in test values:

```
to_email:          your-email@gmail.com
to_name:           John Doe
order_id:          ORD-12345
order_date:        2026-03-20
payment_method:    Credit Card
total_price:       99.99
delivery_address:  123 Main St, New York, NY 10001
```

Click: **[Send]**

---

## ✅ STEP 5: Verify

Check your email inbox:

- ✅ Email received?
- ✅ Name shows "John Doe"?
- ✅ Order ID shows "ORD-12345"?
- ✅ Date shows "2026-03-20"?
- ✅ Payment method shows "Credit Card"?
- ✅ Price shows "$99.99"?
- ✅ Address shows full address?

---

## 📝 Save This Info

After template is created, save:

```
Service ID:    (from Email Services)
Template ID:   order_confirmation
Public Key:    (from Account → API Keys)
```

---

## 🎯 Next: Production Integration

After template works, follow: **EMAILJS_PRODUCTION_READY_CHECKLIST.md** Phase 2

---

**Status:** Ready to paste  
**Time:** ~3 minutes  
**All 7 variables:** ✅ Included

