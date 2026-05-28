# EmailJS Deployment - Blaze Plan Required

## Current Status
✅ Code is ready for deployment
❌ Firebase project is on Spark (free) plan
❌ Cloud Functions require Blaze (paid) plan

---

## What You Need to Do

### Step 1: Upgrade Firebase to Blaze Plan
1. Go to: https://console.firebase.google.com/project/craftoria432/usage/details
2. Click **"Upgrade to Blaze"** button
3. Add a payment method
4. Confirm upgrade

**Cost:** You only pay for what you use. Email sending is very cheap (~$0.50 per 100,000 emails)

### Step 2: After Upgrade, Deploy Functions

```bash
cd functions
npm install
firebase deploy --only functions
```

### Step 3: Set EmailJS Environment Variables

After deployment succeeds, set your EmailJS credentials:

```bash
firebase functions:config:set emailjs.public_key="YOUR_PUBLIC_KEY"
firebase functions:config:set emailjs.service_id="YOUR_SERVICE_ID"
firebase functions:config:set emailjs.template_id="order_confirmation"
firebase functions:config:set emailjs.private_key="YOUR_PRIVATE_KEY"
```

### Step 4: Redeploy with Environment Variables

```bash
firebase deploy --only functions
```

---

## What Happens After Deployment

✅ When buyer places order → Automatic email sent to their Gmail
✅ Email includes all 7 variables (Order ID, Date, Payment Method, etc.)
✅ All emails logged to Firestore for audit trail
✅ System handles failures gracefully (order completes even if email fails)

---

## Getting EmailJS Credentials

1. Go to https://www.emailjs.com/
2. Sign up or login
3. Create a service (Gmail recommended)
4. Create an email template named `order_confirmation`
5. Copy your:
   - Public Key
   - Service ID
   - Template ID
   - Private Key

---

## Cost Estimate

- **EmailJS**: Free tier includes 200 emails/month
- **Firebase Functions**: ~$0.40 per 1M invocations
- **Firestore**: Minimal cost for logging

**Total monthly cost**: Usually under $1 for small volume

---

## Troubleshooting

**npm install fails:**
- Clear npm cache: `npm cache clean --force`
- Delete `node_modules` and `package-lock.json`
- Run `npm install` again

**Deployment fails after upgrade:**
- Wait 5 minutes for APIs to enable
- Try deployment again

**Emails not sending:**
- Check Firebase Console → Functions → Logs
- Verify environment variables are set correctly
- Check EmailJS dashboard for errors

---

## Next Steps

1. Upgrade to Blaze plan
2. Run `npm install` in functions folder
3. Deploy with `firebase deploy --only functions`
4. Set environment variables
5. Redeploy functions
6. Test by creating an order in the app

Done! Automatic emails will now work.
