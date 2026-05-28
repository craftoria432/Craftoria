# Cloud Functions Deployment Guide

## Overview
This guide walks you through deploying Firebase Cloud Functions that enable push notifications across your Craftoria platform.

## What's Included

### 1. **FCM Push Notification Triggers** (11 functions)
- `sendNotificationOnCreate` - Sends FCM when notification is created
- `notifySellerVerified` - Seller verification approved
- `notifySellerRejected` - Seller verification rejected
- `notifyProductApproved` - Product approved for sale
- `notifyProductRejected` - Product rejected
- `notifyProductDeleted` - Product deleted by admin
- `notifyOrderStatusChange` - Order status updates (buyer + seller)
- `notifyReportCreated` - New report submitted (notify admins)
- `notifyUserSuspended` - Account suspended
- `notifyUserReactivated` - Account reactivated
- `notifyStoreFlagged` - Store flagged by admin
- `notifyStoreUnflagged` - Store unflagged
- `notifyLearningResourceCreated` - New learning resource (notify admins)
- `notifySettingsUpdated` - Settings changed (notify super admins)

### 2. **Maintenance Functions** (2 functions)
- `cleanupOldNotifications` - Deletes notifications older than 30 days (daily at 2 AM UTC)
- `cleanupOldActivities` - Deletes admin activities older than 90 days (daily at 3 AM UTC)

## Prerequisites

1. **Firebase CLI installed**
   ```bash
   npm install -g firebase-tools
   ```

2. **Node.js 16+** installed

3. **Firebase project initialized** in your workspace

4. **Blaze plan** (pay-as-you-go) - Cloud Functions require this

## Deployment Steps

### Step 1: Install Dependencies
```bash
cd functions
npm install
```

### Step 2: Verify Configuration
```bash
firebase projects:list
firebase use <your-project-id>
```

### Step 3: Deploy Functions
```bash
# Deploy all functions
firebase deploy --only functions

# Or deploy specific function
firebase deploy --only functions:sendNotificationOnCreate
```

### Step 4: Verify Deployment
```bash
firebase functions:list
```

You should see all 16 functions listed with status "OK".

## Testing the Deployment

### Test 1: Manual Notification Creation
1. Go to Firebase Console → Firestore
2. Create a test document in `notifications` collection:
   ```json
   {
     "user_id": "test-user-id",
     "title": "Test Notification",
     "message": "This is a test",
     "type": "general",
     "category": "TEST",
     "read": false,
     "createdAt": "2024-03-14T10:00:00Z"
   }
   ```
3. Check Firebase Functions logs for success

### Test 2: Seller Verification
1. Update a user document with `verification_status: "verified"`
2. Check that notification was created
3. Verify FCM was sent (check logs)

### Test 3: Product Approval
1. Update a product with `approval_status: "approved"`
2. Verify notification created for seller
3. Check FCM logs

## Monitoring & Logs

### View Function Logs
```bash
firebase functions:log
```

### View Specific Function Logs
```bash
firebase functions:log --function=sendNotificationOnCreate
```

### Real-time Monitoring
1. Firebase Console → Functions
2. Click on function name
3. View "Logs" tab

## Troubleshooting

### Issue: "Permission denied" error
**Solution:** Ensure your Firebase project has Blaze plan enabled
```bash
firebase billing:set blaze
```

### Issue: "FCM token not found"
**Solution:** 
- User hasn't installed mobile app yet
- Mobile app hasn't saved FCM token to Firestore
- Check `users/{userId}/fcm_token` field exists

### Issue: Functions not triggering
**Solution:**
1. Check Firestore security rules allow reads/writes
2. Verify function code has no syntax errors
3. Check function logs for errors
4. Redeploy: `firebase deploy --only functions`

### Issue: "Quota exceeded"
**Solution:**
- Blaze plan has generous free tier (2M invocations/month)
- Check if you're hitting rate limits
- Implement exponential backoff in retry logic

## Production Checklist

- [ ] Blaze plan enabled
- [ ] All 16 functions deployed successfully
- [ ] Tested seller verification notification
- [ ] Tested product approval notification
- [ ] Tested order status notification
- [ ] Verified FCM tokens are being saved in mobile app
- [ ] Checked function logs for errors
- [ ] Set up monitoring alerts
- [ ] Documented any custom configurations
- [ ] Tested cleanup functions (optional, runs automatically)

## Cost Estimation

**Free Tier (Blaze Plan):**
- 2,000,000 function invocations/month
- 400,000 GB-seconds/month
- 5GB Cloud Storage

**Typical Usage:**
- ~100 notifications/day = 3,000/month
- Well within free tier

## Next Steps

1. **Deploy Cloud Functions** (this guide)
2. **Configure Deep Linking** - Make notification taps navigate to correct screens
3. **Add Notification Preferences** - Let admins customize notifications
4. **Monitor & Optimize** - Track delivery rates and performance

## Support

For issues:
1. Check Firebase Console → Functions → Logs
2. Review error messages in function logs
3. Verify Firestore security rules
4. Check mobile app FCM token is being saved

---

**Last Updated:** March 14, 2024
**Status:** Production Ready
