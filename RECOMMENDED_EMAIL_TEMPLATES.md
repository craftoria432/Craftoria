# Recommended Email Templates for Craftoria

## 📊 Template Strategy

For a complete e-commerce platform like Craftoria, you should create **10-12 core templates** covering all customer touchpoints.

---

## 🎯 ESSENTIAL TEMPLATES (Must Have)

### 1. **Order Confirmation** ✅ CREATED
- **When:** Immediately after order placement
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{order_id}}`, `{{total_price}}`, `{{payment_method}}`, `{{delivery_address}}`, `{{order_date}}`
- **Purpose:** Confirm order received and provide order details

### 2. **Order Shipped** ✅ CREATED
- **When:** When seller ships the order
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{order_id}}`, `{{tracking_number}}`, `{{estimated_delivery}}`
- **Purpose:** Notify buyer that order is on the way

### 3. **Order Delivered** ✅ CREATED
- **When:** When package is delivered
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{order_id}}`, `{{delivery_date}}`
- **Purpose:** Confirm delivery and request review

### 4. **Payment Receipt** ✅ CREATED
- **When:** After successful payment
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{order_id}}`, `{{total_price}}`, `{{payment_method}}`, `{{subtotal}}`, `{{shipping_cost}}`, `{{tax}}`
- **Purpose:** Provide detailed payment breakdown

### 5. **Order Cancelled** ✅ CREATED
- **When:** When order is cancelled
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{order_id}}`, `{{refund_amount}}`, `{{cancellation_reason}}`
- **Purpose:** Notify cancellation and refund details

---

## 🔄 IMPORTANT TEMPLATES (Highly Recommended)

### 6. **Seller Order Notification** 🆕
- **When:** New order received
- **Recipient:** Seller
- **Variables:** `{{seller_email}}`, `{{seller_name}}`, `{{order_id}}`, `{{buyer_name}}`, `{{total_items}}`, `{{order_total}}`
- **Purpose:** Alert seller of new order to fulfill

### 7. **Seller Payment Notification** 🆕
- **When:** Seller receives payment
- **Recipient:** Seller
- **Variables:** `{{seller_email}}`, `{{seller_name}}`, `{{order_id}}`, `{{seller_earnings}}`, `{{payment_date}}`
- **Purpose:** Notify seller of payment received

### 8. **Refund Processed** 🆕
- **When:** Refund is processed
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{order_id}}`, `{{refund_amount}}`, `{{refund_date}}`
- **Purpose:** Confirm refund has been processed

### 9. **Review Request** 🆕
- **When:** After delivery (3-5 days)
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{order_id}}`, `{{product_name}}`, `{{review_link}}`
- **Purpose:** Request product review

### 10. **Negotiation Request** 🆕
- **When:** Buyer sends negotiation offer
- **Recipient:** Seller
- **Variables:** `{{seller_email}}`, `{{seller_name}}`, `{{product_name}}`, `{{offered_price}}`, `{{original_price}}`
- **Purpose:** Notify seller of price negotiation

---

## 📱 OPTIONAL TEMPLATES (Nice to Have)

### 11. **Wishlist Alert** 🆕
- **When:** Wishlist item back in stock or price drop
- **Recipient:** Buyer
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{product_name}}`, `{{new_price}}`, `{{old_price}}`
- **Purpose:** Notify about wishlist items

### 12. **Account Verification** 🆕
- **When:** New account signup
- **Recipient:** User
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{verification_link}}`, `{{expiry_time}}`
- **Purpose:** Verify email address

### 13. **Password Reset** 🆕
- **When:** User requests password reset
- **Recipient:** User
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{reset_link}}`, `{{expiry_time}}`
- **Purpose:** Allow user to reset password

### 14. **Seller Application Status** 🆕
- **When:** Seller application approved/rejected
- **Recipient:** Seller
- **Variables:** `{{to_email}}`, `{{to_name}}`, `{{status}}`, `{{reason}}`
- **Purpose:** Notify seller of application decision

---

## 📋 COMPLETE TEMPLATE CHECKLIST

```
BUYER EMAILS:
✅ 1. Order Confirmation
✅ 2. Order Shipped
✅ 3. Order Delivered
✅ 4. Payment Receipt
✅ 5. Order Cancelled
⭕ 6. Refund Processed
⭕ 7. Review Request
⭕ 8. Wishlist Alert
⭕ 9. Account Verification
⭕ 10. Password Reset

SELLER EMAILS:
⭕ 11. Seller Order Notification
⭕ 12. Seller Payment Notification
⭕ 13. Negotiation Request
⭕ 14. Seller Application Status

TOTAL: 14 Templates
✅ Created: 5
⭕ Recommended: 9
```

---

## 🚀 IMPLEMENTATION PRIORITY

### Phase 1 (Week 1) - CRITICAL
Create these 5 templates first:
1. Order Confirmation ✅
2. Order Shipped ✅
3. Order Delivered ✅
4. Payment Receipt ✅
5. Order Cancelled ✅

### Phase 2 (Week 2) - HIGH PRIORITY
Create these 4 templates:
6. Seller Order Notification
7. Seller Payment Notification
8. Refund Processed
9. Review Request

### Phase 3 (Week 3) - MEDIUM PRIORITY
Create these 3 templates:
10. Negotiation Request
11. Wishlist Alert
12. Account Verification

### Phase 4 (Week 4) - LOW PRIORITY
Create these 2 templates:
13. Password Reset
14. Seller Application Status

---

## 📊 TEMPLATE USAGE FREQUENCY

| Template | Frequency | Priority |
|----------|-----------|----------|
| Order Confirmation | Every order | CRITICAL |
| Payment Receipt | Every order | CRITICAL |
| Order Shipped | Every order | CRITICAL |
| Order Delivered | Every order | CRITICAL |
| Seller Order Notification | Every order | HIGH |
| Seller Payment Notification | Every order | HIGH |
| Order Cancelled | Occasional | HIGH |
| Refund Processed | Occasional | HIGH |
| Review Request | Every order (delayed) | MEDIUM |
| Negotiation Request | Occasional | MEDIUM |
| Wishlist Alert | Occasional | LOW |
| Account Verification | New signup | LOW |
| Password Reset | On demand | LOW |
| Seller Application Status | New seller | LOW |

---

## 💡 RECOMMENDED APPROACH

### Start with 5 (Already Created)
You have the essential 5 templates. This covers:
- ✅ Buyer order lifecycle
- ✅ Payment confirmation
- ✅ Cancellation handling

### Add 4 More (Next Priority)
Add these to complete seller notifications:
- Seller Order Notification
- Seller Payment Notification
- Refund Processed
- Review Request

### Total: 9 Templates (Recommended Minimum)

This gives you complete coverage for:
- Buyer experience
- Seller experience
- Payment handling
- Customer feedback

---

## 🎯 QUICK DECISION GUIDE

**If you want MINIMUM setup:**
- Use the 5 templates already created ✅

**If you want GOOD coverage:**
- Add 4 more = 9 templates total

**If you want COMPLETE system:**
- Create all 14 templates

---

## 📝 NEXT STEPS

### To Create Additional Templates:

1. **Seller Order Notification**
   ```
   Subject: New Order {{order_id}} - {{total_items}} items
   To: {{seller_email}}
   ```

2. **Seller Payment Notification**
   ```
   Subject: Payment Received - {{order_id}}
   To: {{seller_email}}
   ```

3. **Refund Processed**
   ```
   Subject: Refund Processed - {{order_id}}
   To: {{to_email}}
   ```

4. **Review Request**
   ```
   Subject: How was your order {{order_id}}?
   To: {{to_email}}
   ```

---

## 🔗 TEMPLATE RELATIONSHIPS

```
Customer Journey:
1. Signup → Account Verification Email
2. Browse → Wishlist Alert Email (optional)
3. Order → Order Confirmation Email
4. Payment → Payment Receipt Email
5. Seller Ships → Order Shipped Email
6. Delivery → Order Delivered Email
7. Review → Review Request Email

Seller Journey:
1. Apply → Seller Application Status Email
2. Order Received → Seller Order Notification Email
3. Payment → Seller Payment Notification Email
4. Negotiation → Negotiation Request Email

Support:
- Password Reset Email
- Refund Processed Email
- Order Cancelled Email
```

---

## 📊 RECOMMENDED MINIMUM: 9 TEMPLATES

For a production-ready system, create at least these 9:

**Buyer (5):**
1. Order Confirmation ✅
2. Order Shipped ✅
3. Order Delivered ✅
4. Payment Receipt ✅
5. Order Cancelled ✅

**Seller (3):**
6. Seller Order Notification 🆕
7. Seller Payment Notification 🆕
8. Negotiation Request 🆕

**Support (1):**
9. Refund Processed 🆕

---

## 💰 COST CONSIDERATION

EmailJS pricing:
- Free tier: 200 emails/month
- Paid: $9.99/month for 5,000 emails

**Recommendation:** Start with 5 templates, scale to 9 as you grow.

---

**Summary:**
- ✅ **Already Created:** 5 templates
- 🆕 **Recommended to Add:** 4 templates (total 9)
- 📈 **Optional:** 5 more templates (total 14)

Start with what you have, add the 4 recommended templates for complete coverage.

---

**Last Updated:** March 20, 2026
**Version:** 1.0
