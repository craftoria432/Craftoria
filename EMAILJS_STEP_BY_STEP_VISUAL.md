# EmailJS Order Confirmation Template - Step by Step Visual Guide

## 🎯 ONLY 1 TEMPLATE - Order Confirmation

---

## STEP 1: Create Template in EmailJS

### Go to EmailJS Dashboard:
```
https://dashboard.emailjs.com
```

### Click "Email Templates" (left sidebar)
```
┌─────────────────────────────────┐
│ Email Templates                 │
│ [+ Create New Template]         │
└─────────────────────────────────┘
```

### Click [+ Create New Template]

---

## STEP 2: Fill Template Settings

### You will see this form:

```
┌─────────────────────────────────────────────────┐
│ Template Name:                                  │
│ Order Confirmation                              │
│                                                 │
│ Template ID:                                    │
│ order_confirmation                              │
│                                                 │
│ Subject:                                        │
│ Order Confirmation - {{order_id}}               │
│                                                 │
│ To Email:                                       │
│ {{to_email}}                                    │
│                                                 │
│ From Name:                                      │
│ Craftoria Support                               │
│                                                 │
│ From Email:                                     │
│ (auto-filled)                                   │
│                                                 │
│ [Create]                                        │
└─────────────────────────────────────────────────┘
```

### Fill in exactly:
- **Template Name:** `Order Confirmation`
- **Template ID:** `order_confirmation`
- **Subject:** `Order Confirmation - {{order_id}}`
- **To Email:** `{{to_email}}`
- **From Name:** `Craftoria Support`

### Click [Create]

---

## STEP 3: Add HTML Content

### You will see this screen:

```
┌─────────────────────────────────────────────────┐
│ Order Confirmation                              │
│                                                 │
│ [Desktop] [Mobile]                              │
│                                                 │
│ [Edit Content]                                  │
│                                                 │
│ Preview of email...                             │
└─────────────────────────────────────────────────┘
```

### Click [Edit Content]

### You will see HTML editor:

```
┌─────────────────────────────────────────────────┐
│ HTML Editor                                     │
│                                                 │
│ ┌─────────────────────────────────────────────┐ │
│ │ <html>                                      │ │
│ │ <body>                                      │ │
│ │ ...                                         │ │
│ │ </body>                                     │ │
│ │ </html>                                     │ │
│ └─────────────────────────────────────────────┘ │
│                                                 │
│ [Save]                                          │
└─────────────────────────────────────────────────┘
```

### Clear all content and paste this HTML:

```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; }
        .container { max-width: 600px; margin: 0 auto; }
        .header { background: #667eea; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; }
        .detail { margin: 10px 0; }
        .total { font-size: 24px; font-weight: bold; color: #667eea; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>✓ Order Confirmed</h1>
            <p>Thank you for your purchase!</p>
        </div>
        <div class="content">
            <p>Hi {{to_name}},</p>
            <p>Your order has been successfully placed!</p>
            
            <div class="detail">
                <strong>Order ID:</strong> {{order_id}}
            </div>
            <div class="detail">
                <strong>Order Date:</strong> {{order_date}}
            </div>
            <div class="detail">
                <strong>Payment Method:</strong> {{payment_method}}
            </div>
            <div class="detail">
                <strong>Total Price:</strong> ${{total_price}}
            </div>
            <div class="detail">
                <strong>Delivery Address:</strong><br>
                {{delivery_address}}
            </div>
            
            <p style="margin-top: 20px;">Thank you for shopping with Craftoria!</p>
        </div>
    </div>
</body>
</html>
```

### Click [Save]

---

## STEP 4: Where Are the 7 Variables?

### The 7 variables are placed in the HTML:

```
1. {{to_email}}           → In "To Email" field (top)
2. {{to_name}}            → In greeting: "Hi {{to_name}},"
3. {{order_id}}           → In "Order ID:" line
4. {{order_date}}         → In "Order Date:" line
5. {{payment_method}}     → In "Payment Method:" line
6. {{total_price}}        → In "Total Price:" line
7. {{delivery_address}}   → In "Delivery Address:" section
```

### Visual placement:

```
┌─────────────────────────────────────────────────┐
│ To Email: {{to_email}}  ← VARIABLE 1            │
│                                                 │
│ Hi {{to_name}},  ← VARIABLE 2                   │
│                                                 │
│ Order ID: {{order_id}}  ← VARIABLE 3            │
│ Order Date: {{order_date}}  ← VARIABLE 4        │
│ Payment Method: {{payment_method}}  ← VAR 5     │
│ Total Price: ${{total_price}}  ← VARIABLE 6     │
│ Delivery Address: {{delivery_address}}  ← VAR 7 │
└─────────────────────────────────────────────────┘
```

---

## STEP 5: Test Template

### Click [Test It] button

### Fill in test values:

```
to_email: your-email@gmail.com
to_name: John Doe
order_id: ORD-12345
order_date: 2026-03-20
payment_method: Credit Card
total_price: 99.99
delivery_address: 123 Main St, New York, NY 10001
```

### Click [Send]

### Check your email inbox

### Verify all variables are replaced:
- ✅ Email received
- ✅ Name shows "John Doe"
- ✅ Order ID shows "ORD-12345"
- ✅ Date shows "2026-03-20"
- ✅ Payment method shows "Credit Card"
- ✅ Price shows "$99.99"
- ✅ Address shows full address

---

## STEP 6: Save & Get Template ID

### After testing, click [Save]

### Your Template ID is: `order_confirmation`

### Save this information:
```
Service ID: (from Email Services)
Template ID: order_confirmation
Public Key: (from Account → API Keys)
```

---

## 📋 Summary - What You Did

| Step | Action | Result |
|------|--------|--------|
| 1 | Created template | Template created |
| 2 | Filled settings | Settings saved |
| 3 | Added HTML | Content added |
| 4 | Added 7 variables | Variables placed in HTML |
| 5 | Tested | Email sent & verified |
| 6 | Saved | Template ready |

---

## ✅ Checklist

- [ ] Template name: "Order Confirmation"
- [ ] Template ID: "order_confirmation"
- [ ] Subject: "Order Confirmation - {{order_id}}"
- [ ] To Email: "{{to_email}}"
- [ ] HTML content added
- [ ] All 7 variables in HTML
- [ ] Test email sent
- [ ] Test email received
- [ ] All variables replaced correctly
- [ ] Template saved

---

## 🎯 Next Step

After completing this, you have:
- ✅ 1 template created
- ✅ 7 variables added
- ✅ Template tested

Now go to: **EMAILJS_PRODUCTION_READY_CHECKLIST.md**

Follow Phase 2 to integrate into your code.

---

**That's it! Only 1 template, 7 variables, done!** 🚀

---

**Last Updated:** March 20, 2026
**Version:** 1.0
