# EmailJS Complete Deployment Guide

## 🎯 Overview

Complete step-by-step guide to deploy EmailJS email system to production.

---

## 📋 Pre-Deployment Checklist

### EmailJS Setup
- [ ] EmailJS account created
- [ ] Email service connected
- [ ] Order Confirmation template created
- [ ] Template tested with 7 variables
- [ ] Public Key obtained
- [ ] Service ID obtained
- [ ] Template ID obtained
- [ ] Private Key obtained (for Node.js)

### Backend Setup
- [ ] `functions/emailService.js` created
- [ ] `functions/index.js` updated
- [ ] `functions/package.json` updated
- [ ] `npm install` completed in functions/
- [ ] `.env` file created in functions/
- [ ] All 4 EmailJS keys added to `.env`

### Frontend Setup
- [ ] `src/services/emailService.js` created
- [ ] `npm install @emailjs/browser` completed
- [ ] `.env` file created in project root
- [ ] All 3 EmailJS keys added to `.env`
- [ ] Checkout component ready for integration

### Documentation
- [ ] All guides read and understood
- [ ] Integration points identified
- [ ] Testing plan prepared

---

## 🚀 Deployment Steps

### Phase 1: Backend Deployment (30 Minutes)

#### Step 1: Verify Backend Files

```bash
# Check files exist
ls functions/emailService.js
ls functions/index.js
cat functions/package.json | grep emailjs
```

#### Step 2: Install Dependencies

```bash
cd functions
npm install
cd ..
```

#### Step 3: Create `.env` File

In `functions/` directory, create `.env`:

```env
EMAILJS_PUBLIC_KEY=your_public_key_here
EMAILJS_SERVICE_ID=your_service_id_here
EMAILJS_TEMPLATE_ID=order_confirmation
EMAILJS_PRIVATE_KEY=your_private_key_here
```

**Get values from:**
- Public Key: EmailJS Dashboard → Account → API Keys
- Service ID: EmailJS Dashboard → Email Services
- Template ID: `order_confirmation`
- Private Key: EmailJS Dashboard → Account → API Keys (Node.js)

#### Step 4: Test Locally

```bash
# Start emulator
firebase emulators:start --only functions

# In another terminal, test
firebase functions:shell

# In shell:
> const emailService = require('./emailService');
> emailService.validateEmail('test@example.com')
true
```

#### Step 5: Deploy Functions

```bash
firebase deploy --only functions
```

Expected output:
```
✔  Deploy complete!

Function URL (sendOrderConfirmation): https://...
Function URL (testEmailService): https://...
Function URL (getEmailServiceStatus): https://...
Function URL (getEmailLogs): https://...
```

#### Step 6: Verify Deployment

```bash
# View logs
firebase functions:log

# Should show:
# [EmailJS] Initialized successfully
# ✅ Cloud Functions initialized successfully
```

---

### Phase 2: Frontend Deployment (30 Minutes)

#### Step 1: Verify Frontend Files

```bash
# Check files exist
ls src/services/emailService.js
cat package.json | grep emailjs
```

#### Step 2: Install Dependencies

```bash
npm install @emailjs/browser
```

#### Step 3: Create `.env` File

In project root, create `.env`:

```env
REACT_APP_EMAILJS_PUBLIC_KEY=your_public_key_here
REACT_APP_EMAILJS_SERVICE_ID=your_service_id_here
REACT_APP_EMAILJS_TEMPLATE_ID=order_confirmation
```

#### Step 4: Add to Checkout Component

Find your checkout component and add:

```javascript
import { sendOrderConfirmation } from '../services/emailService';

// After successful payment
const emailResult = await sendOrderConfirmation({
  customerEmail: user.email,
  customerName: user.name,
  orderId: order.id,
  orderDate: new Date().toLocaleDateString(),
  paymentMethod: order.paymentMethod,
  totalPrice: order.totalPrice.toString(),
  deliveryAddress: order.deliveryAddress
});

if (emailResult.success) {
  console.log('✓ Email sent');
} else {
  console.warn('⚠ Email failed but order completed');
}
```

#### Step 5: Test Locally

```bash
# Start dev server
npm run dev

# Place test order
# Check browser console for [EmailJS] logs
# Verify email received
```

#### Step 6: Build for Production

```bash
npm run build
```

Expected output:
```
✔ Build successful
✔ No errors
✔ Ready to deploy
```

#### Step 7: Deploy Frontend

```bash
# Deploy to your hosting
# (Firebase Hosting, Vercel, Netlify, etc.)

# For Firebase Hosting:
firebase deploy --only hosting
```

---

### Phase 3: Testing (30 Minutes)

#### Test 1: Backend Functions

```bash
# Check function status
firebase functions:log

# Should show:
# [EmailJS] Initialized successfully
```

#### Test 2: Email Service

```javascript
// In browser console
import { testEmailService } from '../services/emailService';

const result = await testEmailService();
console.log(result);
// { configured: true, message: "..." }
```

#### Test 3: Send Test Email

```javascript
import { sendOrderConfirmation } from '../services/emailService';

const result = await sendOrderConfirmation({
  customerEmail: 'your-email@gmail.com',
  customerName: 'Test User',
  orderId: 'TEST-001',
  orderDate: '2026-03-20',
  paymentMethod: 'Test',
  totalPrice: '99.99',
  deliveryAddress: 'Test Address'
});

console.log(result);
// { success: true, messageId: 200 }
```

#### Test 4: Place Test Order

1. Go to checkout
2. Fill in order details
3. Complete payment
4. Check email inbox
5. Verify email received with all variables replaced

#### Test 5: Check Logs

```bash
# View Cloud Function logs
firebase functions:log

# View Firestore logs
# Firebase Console → Firestore → email_logs collection
```

---

## ✅ Post-Deployment Verification

### Backend Verification

- [ ] Cloud Functions deployed successfully
- [ ] `firebase functions:log` shows no errors
- [ ] `[EmailJS] Initialized successfully` appears in logs
- [ ] Email logs appear in Firestore

### Frontend Verification

- [ ] Build successful with no errors
- [ ] `.env` file has all 3 keys
- [ ] Checkout component has email sending code
- [ ] `[EmailJS]` logs appear in browser console

### Email Verification

- [ ] Test email sent successfully
- [ ] Email received in inbox
- [ ] All 7 variables replaced correctly
- [ ] Email formatting looks good
- [ ] Links work correctly

### Monitoring Verification

- [ ] Firestore `email_logs` collection has entries
- [ ] Firestore `admin_activities` collection has entries
- [ ] Cloud Function logs show email events
- [ ] No errors in logs

---

## 🔍 Troubleshooting

### Backend Issues

| Issue | Solution |
|-------|----------|
| Functions won't deploy | Check Node.js version (18+), run `npm install` |
| `[EmailJS] not initialized` | Check `.env` file has all 4 keys |
| Email not sending | Check EmailJS dashboard, verify service connected |
| Logs not appearing | Check Firestore permissions, verify collection exists |

### Frontend Issues

| Issue | Solution |
|-------|----------|
| Build fails | Check `.env` file, run `npm install` |
| Email not sending | Check `.env` file has all 3 keys, check browser console |
| Variables not replaced | Check template in EmailJS dashboard |
| Logs not appearing | Check browser console, verify authentication |

### Email Issues

| Issue | Solution |
|-------|----------|
| Email not received | Check spam folder, verify email address |
| Variables not replaced | Check template variables in EmailJS |
| Email looks wrong | Check HTML in template, test on different clients |
| Going to spam | Check sender reputation, add SPF/DKIM records |

---

## 📊 Monitoring Setup

### Real-Time Logs

```bash
# Watch Cloud Function logs
firebase functions:log --follow
```

### Firestore Monitoring

1. Firebase Console
2. Firestore Database
3. Collections:
   - `email_logs` - All email events
   - `admin_activities` - Admin actions

### Email Dashboard

1. EmailJS Dashboard
2. View email statistics
3. Monitor bounce rates
4. Check failed emails

---

## 🔒 Security Verification

- [ ] No hardcoded keys in code
- [ ] `.env` files in `.gitignore`
- [ ] Environment variables used
- [ ] Authentication required for functions
- [ ] Admin-only logs protected
- [ ] Input validation in place
- [ ] Error handling complete

---

## 📈 Performance Monitoring

### Metrics to Track

- Email delivery rate (target: >95%)
- Email open rate (target: >20%)
- Email bounce rate (target: <2%)
- Email complaint rate (target: <0.1%)
- Function execution time (target: <5s)
- Function error rate (target: <1%)

### How to Monitor

1. **EmailJS Dashboard**
   - Email statistics
   - Bounce rates
   - Complaint rates

2. **Firebase Console**
   - Function execution time
   - Error rates
   - Logs

3. **Firestore**
   - Email logs
   - Admin activities

---

## 🎯 Success Criteria

✅ **Backend**
- Functions deployed successfully
- Logs show no errors
- Email service initialized

✅ **Frontend**
- Build successful
- Checkout integration complete
- Email sending works

✅ **Email**
- Test email sent successfully
- Email received in inbox
- All variables replaced
- Formatting looks good

✅ **Monitoring**
- Logs appear in Firestore
- Cloud Function logs working
- Admin activities logged

✅ **Security**
- No hardcoded keys
- Environment variables used
- Authentication required
- Permissions enforced

---

## 🚀 Go Live Checklist

```
BACKEND:
✅ Functions deployed
✅ Logs verified
✅ Email service initialized
✅ Firestore logging working

FRONTEND:
✅ Build successful
✅ Checkout integration complete
✅ Email sending works
✅ Logs appearing

EMAIL:
✅ Test email sent
✅ Email received
✅ Variables replaced
✅ Formatting correct

MONITORING:
✅ Firestore logs working
✅ Cloud Function logs working
✅ Admin activities logged
✅ Metrics tracked

SECURITY:
✅ No hardcoded keys
✅ Environment variables used
✅ Authentication required
✅ Permissions enforced

READY FOR PRODUCTION:
✅ All systems verified
✅ All tests passed
✅ Monitoring in place
✅ Documentation complete
```

---

## 📞 Support

- **EmailJS Docs**: https://www.emailjs.com/docs/
- **Firebase Functions**: https://firebase.google.com/docs/functions
- **Firebase Firestore**: https://firebase.google.com/docs/firestore

---

## 🎉 Deployment Complete!

Your EmailJS email system is now **live in production** with:

- ✅ Automatic email on order creation
- ✅ Complete error handling
- ✅ Firestore logging
- ✅ Admin dashboard access
- ✅ Real-time monitoring
- ✅ Security best practices

**Status:** Production Ready ✅  
**All 7 Variables:** Configured ✅  
**Deployed:** Yes ✅  
**Monitoring:** Active ✅

---

**Last Updated:** March 20, 2026  
**Version:** 1.0  
**Status:** Production Ready ✅

