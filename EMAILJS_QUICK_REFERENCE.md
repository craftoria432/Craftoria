# EmailJS Quick Reference Card

## 🚀 Setup (5 Minutes)

### 1. Create `.env`
```env
REACT_APP_EMAILJS_PUBLIC_KEY=your_public_key
REACT_APP_EMAILJS_SERVICE_ID=your_service_id
REACT_APP_EMAILJS_TEMPLATE_ID=order_confirmation
```

### 2. Install
```bash
npm install @emailjs/browser
```

### 3. Import
```javascript
import { sendOrderConfirmation } from '../services/emailService';
```

### 4. Use
```javascript
const result = await sendOrderConfirmation({
  customerEmail: user.email,
  customerName: user.name,
  orderId: order.id,
  orderDate: new Date().toLocaleDateString(),
  paymentMethod: order.paymentMethod,
  totalPrice: order.totalPrice.toString(),
  deliveryAddress: order.deliveryAddress
});
```

---

## 📊 7 Variables

| Variable | Value | Example |
|----------|-------|---------|
| `to_email` | Customer email | `john@example.com` |
| `to_name` | Customer name | `John Doe` |
| `order_id` | Order ID | `ORD-12345` |
| `order_date` | Order date | `3/20/2026` |
| `payment_method` | Payment method | `Credit Card` |
| `total_price` | Total price | `99.99` |
| `delivery_address` | Delivery address | `123 Main St, NY` |

---

## 🧪 Testing

### Check Configuration
```javascript
import { getEmailServiceStatus } from '../services/emailService';
console.log(getEmailServiceStatus());
```

### Test Service
```javascript
import { testEmailService } from '../services/emailService';
const result = await testEmailService();
console.log(result);
```

### Send Test Email
```javascript
import { sendOrderConfirmation } from '../services/emailService';
const result = await sendOrderConfirmation({
  customerEmail: 'test@example.com',
  customerName: 'Test User',
  orderId: 'TEST-001',
  orderDate: '2026-03-20',
  paymentMethod: 'Test',
  totalPrice: '99.99',
  deliveryAddress: 'Test Address'
});
console.log(result);
```

---

## 🔄 With Retry

```javascript
import { sendOrderConfirmationWithRetry } from '../services/emailService';

const result = await sendOrderConfirmationWithRetry(orderData, 3);
// Retries up to 3 times with exponential backoff
```

---

## 📁 Files

| File | Purpose |
|------|---------|
| `src/services/emailService.js` | Main email service |
| `functions/.env.example` | Environment template |
| `email-templates/order-confirmation.html` | Email template |
| `EMAILJS_INTEGRATION_GUIDE.md` | Full setup guide |
| `EMAILJS_CHECKOUT_INTEGRATION.md` | Checkout code |

---

## ✅ Checklist

- [ ] `.env` created with 3 keys
- [ ] `npm install @emailjs/browser`
- [ ] Import in checkout component
- [ ] Add email sending after order
- [ ] Test with `testEmailService()`
- [ ] Send test email
- [ ] Verify email received
- [ ] Deploy to production

---

## 🔒 Security

✅ Keys in `.env` (not in code)  
✅ `.env` in `.gitignore`  
✅ Input validation  
✅ Error handling  
✅ Non-blocking  

---

## 🐛 Troubleshooting

| Problem | Solution |
|---------|----------|
| Email not sending | Check `.env` keys, run `testEmailService()` |
| Invalid email | Validate email format before sending |
| Missing variables | Check all 7 fields are provided |
| Network error | Use `sendOrderConfirmationWithRetry()` |
| Email looks wrong | Check template HTML in EmailJS dashboard |

---

## 📞 Resources

- **Docs**: https://www.emailjs.com/docs/
- **Dashboard**: https://dashboard.emailjs.com
- **Test Email**: https://www.emailonacid.com/

---

## 🎯 Key Points

1. **Non-blocking** - Email failures don't block orders
2. **Validated** - All input is validated before sending
3. **Logged** - All operations logged with `[EmailJS]` prefix
4. **Secure** - Keys in environment variables
5. **Tested** - Includes test functions
6. **Documented** - Complete guides included

---

**Status:** Production Ready ✅  
**All 7 Variables:** Configured ✅  
**Ready to Deploy:** Yes ✅

