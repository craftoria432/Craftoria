# 🚀 FIREBASE BLAZE PLAN UPGRADE GUIDE

**Status:** ⚠️ ACTION REQUIRED  
**Issue:** Cloud Functions deployment requires Blaze plan

---

## 🎯 WHAT'S HAPPENING

Your Firebase project `craftoria432` is currently on the **Spark (free) plan**, which doesn't support Cloud Functions deployment. You need to upgrade to the **Blaze (pay-as-you-go) plan**.

---

## ✅ STEP-BY-STEP UPGRADE

### Step 1: Open Firebase Console
Visit: https://console.firebase.google.com/project/craftoria432/usage/details

### Step 2: Click "Upgrade to Blaze"
- Look for the upgrade button on the usage/billing page
- Click "Upgrade to Blaze"

### Step 3: Add Payment Method
- Enter your credit card information
- Google will charge you only for what you use
- Most projects stay within the free tier limits

### Step 4: Confirm Upgrade
- Review the terms
- Click "Upgrade"
- Wait 2-3 minutes for the upgrade to complete

---

## 💰 BLAZE PLAN PRICING

### What You Get
- ✅ Cloud Functions (up to 2M invocations/month free)
- ✅ Firestore (up to 50K reads/day free)
- ✅ Cloud Storage (5GB free)
- ✅ All other Firebase services

### Typical Costs
- **Small app:** $0-5/month
- **Medium app:** $5-20/month
- **Large app:** $20-100+/month

### Free Tier Limits
- 2M Cloud Function invocations/month
- 50K Firestore reads/day
- 20K Firestore writes/day
- 5GB Cloud Storage

---

## 🔄 AFTER UPGRADE

Once upgraded, run:

```bash
firebase deploy --only functions:notifyOrderStatusChange
```

Expected output:
```
✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/craftoria432
```

---

## ⚠️ IMPORTANT NOTES

### Billing
- You'll only be charged for usage beyond free tier
- Set up billing alerts to monitor costs
- Most small projects stay free

### APIs Enabled
After upgrade, these APIs will be automatically enabled:
- ✅ Cloud Functions API
- ✅ Cloud Build API
- ✅ Artifact Registry API

### No Downtime
- Upgrade happens instantly
- No impact on existing app
- Existing data remains unchanged

---

## 🆘 TROUBLESHOOTING

### Issue: Upgrade button not showing
**Solution:** 
1. Log out and log back in
2. Refresh the page
3. Try a different browser

### Issue: Payment method rejected
**Solution:**
1. Verify card details are correct
2. Check if card is enabled for international transactions
3. Try a different payment method

### Issue: Still getting error after upgrade
**Solution:**
1. Wait 5 minutes for APIs to enable
2. Run: `firebase projects:list` to verify
3. Try deployment again

---

## 📋 VERIFICATION CHECKLIST

After upgrade, verify:
- [ ] Upgrade shows "Blaze" plan in Firebase Console
- [ ] Billing section shows payment method
- [ ] APIs are enabled (Cloud Functions, Cloud Build)
- [ ] Deployment command succeeds

---

## 🎯 NEXT STEPS

1. **Upgrade to Blaze plan** (5 minutes)
2. **Wait for APIs to enable** (2-3 minutes)
3. **Run deployment command** (2-3 minutes)
4. **Verify in Firebase Console** (1 minute)

---

## 📞 SUPPORT

### Firebase Support
- https://firebase.google.com/support

### Billing Questions
- https://console.firebase.google.com/project/craftoria432/settings/billing

### Documentation
- https://firebase.google.com/docs/functions/get-started

---

## ✅ DEPLOYMENT COMMAND

Once upgraded, use:

```bash
# From functions directory
firebase deploy --only functions:notifyOrderStatusChange
```

Or from project root:

```bash
firebase deploy --only functions:notifyOrderStatusChange
```

---

**Time to Complete:** 10-15 minutes  
**Cost:** Usually free (within free tier)  
**Difficulty:** Easy

---

*After upgrading, your Cloud Functions will deploy successfully and the order cancellation pink hover effect will be live!*
