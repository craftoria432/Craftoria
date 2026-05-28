# SendGrid Integration Setup Guide

## Overview
This guide walks you through setting up SendGrid to automatically send order confirmation emails to buyers.

---

## Step 1: Create SendGrid Account

1. Go to [SendGrid](https://sendgrid.com)
2. Click "Sign Up" (free tier available)
3. Complete the registration
4. Verify your email address
5. Log in to your SendGrid dashboard

---

## Step 2: Get Your API Key

1. In SendGrid dashboard, go to **Settings** → **API Keys**
2. Click **Create API Key**
3. Name it: `Craftoria Order Emails`
4. Select **Full Access** (or at minimum: Mail Send)
5. Click **Create & View**
6. **Copy the API key** (you'll only see it once!)
7. Save it somewhere safe

**Your API Key looks like**: `SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

---

## Step 3: Verify Sender Email

1. In SendGrid dashboard, go to **Settings** → **Sender Authentication**
2. Click **Verify a Single Sender**
3. Fill in:
   - **From Email**: `noreply@craftoria.app` (or your domain)
   - **From Name**: `Craftoria`
   - **Reply To Email**: `support@craftoria.app`
4. Click **Create**
5. Check your email and click the verification link
6. Once verified, you can send emails from this address

---

## Step 4: Set Up Firebase Functions

### Option A: Using Firebase CLI (Recommended)

```bash
# 1. Navigate to your project root
cd your-project-root

# 2. Install Firebase CLI if not already installed
npm install -g firebase-tools

# 3. Set the SendGrid API key in Firebase config
firebase functions:config:set sendgrid.key="YOUR_API_KEY_HERE"

# 4. Verify it was set
firebase functions:config:get

# 5. Deploy functions
firebase deploy --only functions
```

### Option B: Using Environment Variables

```bash
# 1. Create .env file in functions directory
cd functions
echo "SENDGRID_API_KEY=YOUR_API_KEY_HERE" > .env

# 2. Install dependencies
npm install

# 3. Deploy
firebase deploy --only functions
```

---

## Step 5: Install Dependencies

```bash
# Navigate to functions directory
cd functions

# Install npm packages
npm install

# This installs:
# - @sendgrid/mail (SendGrid SDK)
# - firebase-admin (Firebase Admin SDK)
# - firebase-functions (Cloud Functions SDK)
```

---

## Step 6: Deploy Cloud Functions

```bash
# From project root
firebase deploy --only functions

# Or from functions directory
npm run deploy

# Watch the deployment logs
firebase functions:log
```

---

## Step 7: Test Email Sending

### Test 1: Place a Test Order
1. Open your Craftoria app
2. Add a product to cart
3. Go to checkout
4. Fill in delivery info with your email
5. Place the order
6. Check your email inbox (and spam folder)

### Test 2: Check Cloud Function Logs
```bash
# View real-time logs
firebase functions:log

# Or in Firebase Console:
# 1. Go to Firebase Console
# 2. Functions → Logs
# 3. Filter by sendOrderConfirmationEmail
```

### Test 3: Verify Email Content
Check that the email contains:
- ✅ Order ID
- ✅ Order date
- ✅ All products with quantities and prices
- ✅ Order total
- ✅ Delivery address
- ✅ Payment method
- ✅ Track order link

---

## Troubleshooting

### Issue: "API key not found"
**Solution**: Make sure you set the API key correctly
```bash
firebase functions:config:set sendgrid.key="YOUR_KEY"
firebase functions:config:get  # Verify it's set
firebase deploy --only functions  # Redeploy
```

### Issue: "Email not sending"
**Solution**: Check the following:
1. SendGrid API key is correct
2. Sender email is verified in SendGrid
3. Buyer email is valid
4. Check Cloud Function logs for errors

### Issue: "Email going to spam"
**Solution**: 
1. Set up SPF and DKIM records (SendGrid provides these)
2. Use a verified domain email instead of noreply@
3. Add unsubscribe link (optional but recommended)

### Issue: "Emails sent but not received"
**Solution**:
1. Check spam/junk folder
2. Verify recipient email is correct
3. Check SendGrid Activity Feed for bounce/drop reasons
4. Contact SendGrid support if issue persists

---

## Monitoring & Maintenance

### View Email Activity
1. Go to SendGrid dashboard
2. Click **Mail Activity** or **Activity Feed**
3. See all sent, delivered, opened, clicked emails

### Check Bounce Rate
1. Go to **Suppressions** → **Bounces**
2. Review any hard bounces
3. Remove invalid emails from your system

### Monitor Costs
- SendGrid free tier: 100 emails/day
- For production: Upgrade to paid plan
- Pricing: ~$10-20/month for small volume

---

## Production Checklist

- [ ] SendGrid account created
- [ ] API key generated and saved securely
- [ ] Sender email verified in SendGrid
- [ ] Firebase functions config set with API key
- [ ] Dependencies installed (`npm install`)
- [ ] Cloud Functions deployed (`firebase deploy --only functions`)
- [ ] Test order placed and email received
- [ ] Email content verified
- [ ] Cloud Function logs checked
- [ ] SPF/DKIM records configured (optional but recommended)
- [ ] Monitoring set up in SendGrid dashboard

---

## Code Changes Made

### functions/index.js
- Added SendGrid import: `const sgMail = require('@sendgrid/mail');`
- Initialized SendGrid with API key
- Updated `sendOrderConfirmationEmail` function to actually send emails
- Added error handling for email sending

### functions/package.json
- Created with all required dependencies
- Added scripts for deployment and logging

### functions/.env.example
- Created as reference for environment variables

---

## Email Template

The email includes:
- Professional header with Craftoria branding
- Order confirmation message
- Order ID and date
- Itemized products with quantities and prices
- Order summary (subtotal, shipping, total)
- Delivery address
- Payment method
- Track order button
- Footer with contact info

---

## Next Steps

1. **Immediate**: Complete the setup steps above
2. **Testing**: Place test orders and verify emails
3. **Monitoring**: Check SendGrid dashboard regularly
4. **Optimization**: Monitor delivery rates and adjust as needed
5. **Scaling**: Upgrade SendGrid plan if needed for higher volume

---

## Support

### SendGrid Support
- Documentation: https://docs.sendgrid.com
- Support: https://support.sendgrid.com
- Status: https://status.sendgrid.com

### Firebase Support
- Documentation: https://firebase.google.com/docs/functions
- Console: https://console.firebase.google.com

---

## Security Notes

⚠️ **Important**: 
- Never commit your API key to version control
- Use Firebase config or environment variables
- Rotate API keys periodically
- Use IP whitelisting if available
- Monitor SendGrid activity for suspicious usage

---

## Cost Estimate

| Volume | Plan | Cost |
|--------|------|------|
| 0-100/day | Free | $0 |
| 100-10k/month | Essentials | $10/month |
| 10k-100k/month | Pro | $80/month |
| 100k+/month | Enterprise | Custom |

For Craftoria, start with Free tier and upgrade as needed.

---

## Status: ✅ READY FOR SETUP

All code is in place. Follow the steps above to activate email sending.

**Last Updated**: March 16, 2026
**Status**: Ready for Implementation
