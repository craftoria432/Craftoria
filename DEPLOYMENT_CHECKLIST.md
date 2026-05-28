# Cloud Functions Deployment Checklist

## Pre-Deployment (5 minutes)

- [ ] Firebase CLI installed: `npm install -g firebase-tools`
- [ ] Node.js 16+ installed: `node --version`
- [ ] Firebase project ID known: `firebase projects:list`
- [ ] Blaze plan enabled (pay-as-you-go)
- [ ] Read `CLOUD_FUNCTIONS_QUICK_START.md`

## Deployment (3 minutes)

```bash
# 1. Navigate to functions directory
cd functions

# 2. Install dependencies
npm install

# 3. Deploy functions
firebase deploy --only functions

# 4. Verify deployment
firebase functions:list
```

- [ ] Step 1: Navigated to functions directory
- [ ] Step 2: Ran `npm install` successfully
- [ ] Step 3: Ran `firebase deploy --only functions`
- [ ] Step 4: Ran `firebase functions:list`
- [ ] All 16 functions show status "OK"

## Post-Deployment Verification (5 minutes)

### Check Function Logs
```bash
firebase functions:log
```
- [ ] No error messages in logs
- [ ] Functions deployed successfully

### Verify Firestore Collections
- [ ] `notifications` collection exists
- [ ] `admin_activities` collection exists
- [ ] `users` collection has `fcm_token` field

### Check Mobile App
- [ ] Mobile app has FCMService.kt implemented
- [ ] FCM tokens are being saved to Firestore
- [ ] Mobile app can receive push notifications

## Testing (10 minutes)

### Test 1: Manual Notification Creation
```bash
# 1. Go to Firebase Console → Firestore
# 2. Create document in notifications collection:
{
  "user_id": "test-user-id",
  "title": "Test Notification",
  "message": "This is a test",
  "type": "general",
  "read": false
}
# 3. Check logs: firebase functions:log
```
- [ ] Document created in Firestore
- [ ] Function logs show "FCM sent"
- [ ] No errors in logs

### Test 2: Seller Verification
```bash
# 1. Go to web admin dashboard
# 2. Find a pending seller
# 3. Click "Approve"
# 4. Check mobile app for push notification
```
- [ ] Seller verification updated in Firestore
- [ ] Notification created in notifications collection
- [ ] Mobile app received push notification
- [ ] Push notification shows correct title/message

### Test 3: Product Approval
```bash
# 1. Go to web admin dashboard
# 2. Find a pending product
# 3. Click "Approve"
# 4. Check mobile app for push notification
```
- [ ] Product approval updated in Firestore
- [ ] Notification created for seller
- [ ] Mobile app received push notification
- [ ] Seller can see notification

### Test 4: Order Status Change
```bash
# 1. Go to web admin dashboard
# 2. Find an order
# 3. Update status (e.g., pending → confirmed)
# 4. Check mobile app for push notifications
```
- [ ] Order status updated in Firestore
- [ ] Notifications created for buyer and seller
- [ ] Both received push notifications
- [ ] Notifications show correct status

## Production Readiness (5 minutes)

### Code Quality
- [ ] No syntax errors in functions/index.js
- [ ] All error handling implemented
- [ ] Logging implemented for debugging
- [ ] Comments added for clarity

### Security
- [ ] Using Firebase Admin SDK
- [ ] Respecting Firestore security rules
- [ ] No hardcoded secrets or API keys
- [ ] User IDs validated before sending

### Performance
- [ ] Functions deploy successfully
- [ ] No timeout errors
- [ ] Logs show fast execution times
- [ ] Batch operations used where applicable

### Monitoring
- [ ] Function logs accessible: `firebase functions:log`
- [ ] Error messages clear and actionable
- [ ] Activity logging implemented
- [ ] Cleanup functions scheduled

### Scalability
- [ ] Can handle 100+ notifications/day
- [ ] No rate limiting issues
- [ ] Batch operations for efficiency
- [ ] Auto-cleanup prevents data bloat

## Troubleshooting

### If Deployment Fails

**Error: "Permission denied"**
```bash
firebase billing:set blaze
firebase deploy --only functions
```
- [ ] Blaze plan enabled
- [ ] Deployment successful

**Error: "Node version too old"**
```bash
node --version  # Should be 16+
# If not, update Node.js
```
- [ ] Node.js 16+ installed
- [ ] Deployment successful

**Error: "Firebase not initialized"**
```bash
firebase init
firebase use <project-id>
firebase deploy --only functions
```
- [ ] Firebase initialized
- [ ] Project selected
- [ ] Deployment successful

### If FCM Not Sending

**Check 1: FCM Token Exists**
- [ ] Go to Firebase Console → Firestore
- [ ] Check `users/{userId}/fcm_token` field
- [ ] Token should be a long string

**Check 2: Mobile App Saving Token**
- [ ] Review FCMService.kt
- [ ] Check `saveFCMToken()` is called on login
- [ ] Verify token is saved to Firestore

**Check 3: Function Logs**
```bash
firebase functions:log --follow
```
- [ ] Look for "FCM sent" messages
- [ ] Check for error messages
- [ ] Verify function is being triggered

**Check 4: Firestore Security Rules**
- [ ] Verify rules allow reading user documents
- [ ] Verify rules allow writing to notifications
- [ ] Check rules don't block function access

## Rollback Plan

If something goes wrong:

```bash
# Option 1: Delete all functions
firebase functions:delete

# Option 2: Delete specific function
firebase functions:delete sendNotificationOnCreate

# Option 3: Redeploy previous version
git checkout HEAD~1 functions/index.js
firebase deploy --only functions
```

- [ ] Understand rollback procedure
- [ ] Know how to delete functions if needed
- [ ] Have backup of previous version

## Documentation

- [ ] Read `CLOUD_FUNCTIONS_DEPLOYMENT_GUIDE.md`
- [ ] Read `CLOUD_FUNCTIONS_QUICK_START.md`
- [ ] Read `NOTIFICATION_SYSTEM_ARCHITECTURE.md`
- [ ] Understand data flow
- [ ] Know how to monitor logs

## Sign-Off

- [ ] All pre-deployment checks complete
- [ ] Deployment successful
- [ ] All tests passing
- [ ] Production ready
- [ ] Team notified

## Next Steps

After deployment:

1. **Monitor for 24 hours**
   - [ ] Check logs regularly
   - [ ] Verify notifications sending
   - [ ] Monitor for errors

2. **Phase 2: Deep Linking** (20 minutes)
   - [ ] Configure deep link URLs
   - [ ] Update FCM payload
   - [ ] Test navigation

3. **Phase 3: Notification Preferences** (25 minutes)
   - [ ] Add preferences collection
   - [ ] Create preferences UI
   - [ ] Filter notifications

## Support Contacts

- **Firebase Support:** https://firebase.google.com/support
- **Cloud Functions Docs:** https://firebase.google.com/docs/functions
- **FCM Docs:** https://firebase.google.com/docs/cloud-messaging

---

**Deployment Date:** _______________
**Deployed By:** _______________
**Verified By:** _______________
**Status:** ✅ READY TO DEPLOY

**Estimated Total Time:** 30 minutes
- Pre-deployment: 5 min
- Deployment: 3 min
- Verification: 5 min
- Testing: 10 min
- Production readiness: 5 min
- Documentation: 2 min
