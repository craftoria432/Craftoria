# How to Add All 7 Variables - Super Simple

## 🎯 The 7 Variables You Need to Add

```
1. {{to_email}}
2. {{to_name}}
3. {{order_id}}
4. {{order_date}}
5. {{payment_method}}
6. {{total_price}}
7. {{delivery_address}}
```

---

## 📍 WHERE TO ADD EACH VARIABLE

### LOCATION 1: {{to_email}}
**Where:** In the "To Email" field at the top of template settings

```
┌─────────────────────────────────┐
│ To Email:                       │
│ {{to_email}}  ← ADD HERE        │
└─────────────────────────────────┘
```

---

### LOCATION 2: {{to_name}}
**Where:** In the greeting line in HTML content

**Current:**
```html
<p>Hi {{to_name}},</p>
```

**Already there!** ✅

---

### LOCATION 3: {{order_id}}
**Where:** In the "Order ID" line

**Current:**
```html
<div class="detail">
    <strong>Order ID:</strong> {{order_id}}
</div>
```

**Already there!** ✅

---

### LOCATION 4: {{order_date}}
**Where:** In the "Order Date" line

**Current:**
```html
<div class="detail">
    <strong>Order Date:</strong> {{order_date}}
</div>
```

**Already there!** ✅

---

### LOCATION 5: {{payment_method}}
**Where:** In the "Payment Method" line

**Current:**
```html
<div class="detail">
    <strong>Payment Method:</strong> {{payment_method}}
</div>
```

**Already there!** ✅

---

### LOCATION 6: {{total_price}}
**Where:** In the "Total Price" line

**Current:**
```html
<div class="detail">
    <strong>Total Price:</strong> ${{total_price}}
</div>
```

**Already there!** ✅

---

### LOCATION 7: {{delivery_address}}
**Where:** In the "Delivery Address" section

**Current:**
```html
<div class="detail">
    <strong>Delivery Address:</strong><br>
    {{delivery_address}}
</div>
```

**Already there!** ✅

---

## ✅ COMPLETE HTML WITH ALL 7 VARIABLES

Copy this entire HTML and paste in "Edit Content":

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

---

## 🎯 STEP BY STEP TO ADD VARIABLES

### Step 1: Click "Edit Content"
```
In your template, click [Edit Content] button
```

### Step 2: Clear existing HTML
```
Select all content (Ctrl+A)
Delete it
```

### Step 3: Paste the HTML above
```
Copy the complete HTML from above
Paste it in the editor
```

### Step 4: Click Save
```
Click [Save] button
```

### Step 5: Test
```
Click [Test It]
Fill in test values:
  to_email: your-email@gmail.com
  to_name: John Doe
  order_id: ORD-12345
  order_date: 2026-03-20
  payment_method: Credit Card
  total_price: 99.99
  delivery_address: 123 Main St, New York, NY 10001
Click [Send]
```

### Step 6: Verify
```
Check your email
All 7 variables should be replaced with test values
```

---

## 📊 VISUAL MAP OF ALL 7 VARIABLES

```
EMAIL PREVIEW:
┌─────────────────────────────────────────────────┐
│                                                 │
│  ✓ Order Confirmed                              │
│  Thank you for your purchase!                   │
│                                                 │
│  Hi {{to_name}},  ← VARIABLE 2                  │
│  Your order has been successfully placed!       │
│                                                 │
│  Order ID: {{order_id}}  ← VARIABLE 3           │
│  Order Date: {{order_date}}  ← VARIABLE 4       │
│  Payment Method: {{payment_method}}  ← VAR 5    │
│  Total Price: ${{total_price}}  ← VARIABLE 6    │
│  Delivery Address:  ← VARIABLE 7                │
│  {{delivery_address}}                           │
│                                                 │
│  Thank you for shopping with Craftoria!         │
│                                                 │
└─────────────────────────────────────────────────┘

TEMPLATE SETTINGS:
┌─────────────────────────────────────────────────┐
│ To Email: {{to_email}}  ← VARIABLE 1            │
└─────────────────────────────────────────────────┘
```

---

## ✅ FINAL CHECKLIST

- [ ] Variable 1: {{to_email}} in "To Email" field
- [ ] Variable 2: {{to_name}} in greeting
- [ ] Variable 3: {{order_id}} in Order ID line
- [ ] Variable 4: {{order_date}} in Order Date line
- [ ] Variable 5: {{payment_method}} in Payment Method line
- [ ] Variable 6: {{total_price}} in Total Price line
- [ ] Variable 7: {{delivery_address}} in Delivery Address section
- [ ] HTML saved
- [ ] Test email sent
- [ ] All variables replaced in test email

---

## 🚀 DONE!

All 7 variables are now in your template!

Next: Go to **EMAILJS_PRODUCTION_READY_CHECKLIST.md** Phase 2

---

**Last Updated:** March 20, 2026
**Version:** 1.0
