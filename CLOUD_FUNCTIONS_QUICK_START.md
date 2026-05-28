# Cloud Functions Quick Start

## 30-Second Deployment

```bash
# 1. Install Firebase CLI (if not already installed)
npm install -g firebase-tools

# 2. Navigate to functions directory
cd functions

# 3. Install dependencies
npm install

# 4. Deploy
firebase deploy --only functions

# 5. Verify
firebase functions:list
```

## What Just Happened?

✅ **14 notification triggers** are now active:
- Seller verification (approved/rejected)
- Product approval/rejection/deletion
- Order status changes
- Account suspension/reactivation
- Store flagging/unflagging
- Report creation
- Learning resource creation
- Settings updates

✅ **2 maintenance functions** scheduled:
- Auto-delete old notifications (30+ days)
- Auto-delete old activities (90+ days)

✅ **FCM push notifications** now send to mobile devices when:
- Admin creates a notification
- Any of the 14 triggers fire

## Testing

### Quick Test: Create a Notification
1. Firebase Console → Firestore
2. Create document in `notifications` collection:
   ```json
   {
     "user_id": "any-user-id",
     "title": "Test",
     "message": "Hello",
     "type": "general",
     "read": false
   }
   ```
3. Check Firebase Functions logs → should see "FCM sent"

### Check Logs
```bash
firebase functions:log
```

## Common Issues

| Issue | Solution |
|-------|----------|
| "Permission denied" | Enable Blaze plan: `firebase billing:set blaze` |
| "FCM token not found" | Mobile app hasn't saved token yet |
| Functions not triggering | Check Firestore security rules |
| "Quota exceeded" | You're using more than free tier (unlikely) |

## What's Next?

1. ✅ **Cloud Functions deployed** (DONE)
2. ⏭️ **Configure Deep Linking** - Make notification taps navigate to screens
3. ⏭️ **Add Notification Preferences** - Let admins customize notifications

## Monitoring

```bash
# View all function logs
firebase functions:log

# View specific function
firebase functions:log --function=sendNotificationOnCreate

# Real-time monitoring
firebase functions:log --follow
```

## Rollback (if needed)

```bash
# Delete all functions
firebase functions:delete

# Or delete specific function
firebase functions:delete sendNotificationOnCreate
```

---

**Status:** ✅ Production Ready
**Deployment Time:** ~2 minutes
**Cost:** Free (within Blaze free tier)
