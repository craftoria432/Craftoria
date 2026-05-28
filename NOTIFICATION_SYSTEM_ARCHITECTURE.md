# Notification System Architecture

## Complete System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CRAFTORIA NOTIFICATION SYSTEM                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│ LAYER 1: TRIGGER SOURCES                                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  WEB ADMIN DASHBOARD          MOBILE APP              SYSTEM EVENTS          │
│  ├─ Approve Product           ├─ Place Order          ├─ Scheduled Tasks     │
│  ├─ Reject Product            ├─ Update Profile       ├─ Cleanup Jobs        │
│  ├─ Verify Seller             ├─ Rate Store           ├─ Maintenance         │
│  ├─ Suspend Account           ├─ Submit Report        │                      │
│  ├─ Flag Store                └─ Send Message         │                      │
│  ├─ Create Learning Resource                          │                      │
│  └─ Update Settings                                   │                      │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│ LAYER 2: FIRESTORE COLLECTIONS                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  users/                    products/              orders/                    │
│  ├─ verification_status    ├─ approval_status     ├─ status                 │
│  ├─ account_status         ├─ seller_id           ├─ buyer_id               │
│  ├─ fcm_token              └─ title               ├─ seller_id              │
│  └─ viewed_by_admins                              └─ total_price            │
│                                                                               │
│  co_seller_stores/         reports/               notifications/             │
│  ├─ is_flagged             ├─ status              ├─ user_id                │
│  ├─ owner_id               ├─ reason              ├─ title                  │
│  └─ store_name             └─ reporter_id         ├─ message                │
│                                                    ├─ type                   │
│  learning_resources/       settings/              ├─ read                   │
│  ├─ title                  ├─ key                 ├─ createdAt              │
│  ├─ category               └─ value               └─ action_data            │
│  └─ created_at                                                              │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│ LAYER 3: CLOUD FUNCTIONS (TRIGGERS)                                          │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  DOCUMENT TRIGGERS                          SCHEDULED TRIGGERS               │
│  ├─ onUpdate(users)                         ├─ cleanupOldNotifications     │
│  │  ├─ notifySellerVerified                 │  (daily 2 AM UTC)            │
│  │  ├─ notifySellerRejected                 │                              │
│  │  ├─ notifyUserSuspended                  ├─ cleanupOldActivities        │
│  │  └─ notifyUserReactivated                │  (daily 3 AM UTC)            │
│  │                                           │                              │
│  ├─ onUpdate(products)                      │                              │
│  │  ├─ notifyProductApproved                │                              │
│  │  ├─ notifyProductRejected                │                              │
│  │  └─ onDelete(products)                   │                              │
│  │     └─ notifyProductDeleted              │                              │
│  │                                           │                              │
│  ├─ onUpdate(orders)                        │                              │
│  │  └─ notifyOrderStatusChange              │                              │
│  │                                           │                              │
│  ├─ onCreate(reports)                       │                              │
│  │  └─ notifyReportCreated                  │                              │
│  │                                           │                              │
│  ├─ onUpdate(co_seller_stores)              │                              │
│  │  ├─ notifyStoreFlagged                   │                              │
│  │  └─ notifyStoreUnflagged                 │                              │
│  │                                           │                              │
│  ├─ onCreate(learning_resources)            │                              │
│  │  └─ notifyLearningResourceCreated        │                              │
│  │                                           │                              │
│  └─ onUpdate(settings)                      │                              │
│     └─ notifySettingsUpdated                │                              │
│                                              │                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│ LAYER 4: NOTIFICATION CREATION                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  createNotification()                                                        │
│  ├─ Validates required fields                                               │
│  ├─ Adds server timestamp                                                   │
│  ├─ Sets read: false                                                        │
│  └─ Writes to notifications collection                                      │
│                                                                               │
│  This triggers: sendNotificationOnCreate                                    │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│ LAYER 5: FCM PUSH NOTIFICATION                                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  sendNotificationOnCreate()                                                  │
│  ├─ Get user_id from notification                                           │
│  ├─ Fetch user document                                                     │
│  ├─ Get fcm_token from user                                                 │
│  ├─ Build FCM payload                                                       │
│  │  ├─ notification: { title, body }                                        │
│  │  └─ data: { type, notificationId, actionData, ... }                      │
│  ├─ Send via Firebase Messaging                                             │
│  ├─ Log activity                                                            │
│  └─ Handle errors gracefully                                                │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│ LAYER 6: MOBILE DEVICE                                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Firebase Cloud Messaging (FCM)                                              │
│  ├─ Receives push notification                                              │
│  ├─ Wakes up app (if needed)                                                │
│  └─ Calls FCMService.onMessageReceived()                                    │
│                                                                               │
│  FCMService.kt                                                               │
│  ├─ Parse notification data                                                 │
│  ├─ Route by type:                                                          │
│  │  ├─ chat_message → handleChatMessage()                                   │
│  │  ├─ order_update → handleOrderUpdate()                                   │
│  │  ├─ negotiation → handleNegotiation()                                    │
│  │  ├─ product_shared → handleProductShared()                               │
│  │  └─ default → handleGeneralNotification()                                │
│  ├─ Create notification channels                                            │
│  ├─ Build notification UI                                                   │
│  ├─ Show system notification                                                │
│  └─ Handle tap → Deep link to screen                                        │
│                                                                               │
│  User sees notification in system tray                                       │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────────────┐
│ LAYER 7: WEB DASHBOARD (REAL-TIME)                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Header.jsx                                                                  │
│  ├─ Real-time Firestore listener                                            │
│  ├─ Watches notifications collection                                        │
│  ├─ Shows last 20 notifications                                             │
│  ├─ Displays unread count badge                                             │
│  ├─ Toast notification on new message                                       │
│  ├─ Mark as read / Mark all read                                            │
│  └─ Navigate to relevant page on click                                      │
│                                                                               │
│  useNotificationCounts.js                                                    │
│  ├─ Real-time pending item counts                                           │
│  ├─ Pending sellers, products, reports, etc.                                │
│  ├─ Separate from notifications (these are pending items)                   │
│  └─ Shows in sidebar badges                                                 │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Data Flow Examples

### Example 1: Seller Verification Approved

```
Admin clicks "Approve" on seller verification page
        ↓
Web updates users/{sellerId} with verification_status: "verified"
        ↓
Cloud Function: notifySellerVerified triggers
        ↓
Creates notification document:
{
  user_id: "seller-123",
  title: "Seller Verification Approved",
  message: "Congratulations! Your seller account has been verified.",
  type: "seller_verification",
  read: false,
  createdAt: <server-timestamp>
}
        ↓
Cloud Function: sendNotificationOnCreate triggers
        ↓
Fetches users/seller-123 → gets fcm_token
        ↓
Sends FCM push:
{
  notification: {
    title: "Seller Verification Approved",
    body: "Congratulations! Your seller account has been verified."
  },
  data: {
    type: "seller_verification",
    notificationId: "notif-456",
    actionData: { user_id: "seller-123", verification_status: "verified" }
  }
}
        ↓
Mobile app receives push → FCMService.onMessageReceived()
        ↓
Shows system notification
        ↓
User taps notification → Opens app to seller dashboard
```

### Example 2: Product Approval

```
Admin clicks "Approve" on product review page
        ↓
Web updates products/{productId} with approval_status: "approved"
        ↓
Cloud Function: notifyProductApproved triggers
        ↓
Creates notification for seller
        ↓
FCM sent to seller's mobile device
        ↓
Seller sees: "Your product 'Handmade Vase' has been approved!"
        ↓
Seller taps → Opens product details page
```

### Example 3: Order Status Update

```
Admin updates order status to "shipped"
        ↓
Cloud Function: notifyOrderStatusChange triggers
        ↓
Creates TWO notifications:
  1. For buyer: "Your order #abc123 has been shipped"
  2. For seller: "Buyer's order #abc123 has been shipped"
        ↓
FCM sent to both buyer and seller
        ↓
Both receive push notifications
        ↓
Both can tap to view order details
```

## Key Components

### 1. **Firestore Collections**
- `notifications` - All notifications (web + mobile)
- `admin_activities` - Audit trail of admin actions
- `users` - User data including fcm_token
- `products`, `orders`, `reports`, etc. - Domain data

### 2. **Cloud Functions**
- 14 notification triggers
- 2 maintenance functions
- Helper functions for creating notifications

### 3. **Mobile App (FCMService.kt)**
- Receives FCM messages
- Parses notification data
- Routes by type
- Shows system notifications
- Handles deep linking

### 4. **Web Dashboard**
- Real-time notification listener
- Displays notifications in header
- Shows unread count badge
- Mark as read functionality
- Navigate to relevant pages

## Notification Types

| Type | Trigger | Recipient | Action |
|------|---------|-----------|--------|
| seller_verification | Seller status changes | Seller | View profile |
| product | Product approval/rejection | Seller | View product |
| order | Order status changes | Buyer + Seller | View order |
| report | Report created | Admins | View report |
| account | Account suspended/reactivated | User | View profile |
| store | Store flagged/unflagged | Store owner | View store |
| learning | Learning resource created | Admins | View resource |
| settings | Settings updated | Super admins | View settings |

## Security

✅ **Firebase Admin SDK** - Secure server-side operations
✅ **Firestore Security Rules** - Control data access
✅ **FCM Token Validation** - Only send to valid tokens
✅ **User ID Verification** - Ensure notifications go to right user
✅ **Activity Logging** - Audit trail of all actions
✅ **Error Handling** - Graceful failures

## Performance

✅ **Real-time** - Instant notification delivery
✅ **Scalable** - Handles thousands of concurrent notifications
✅ **Efficient** - Batch operations where possible
✅ **Optimized** - Cleanup jobs prevent data bloat
✅ **Reliable** - Firebase handles retry logic

## Cost

**Free Tier (Blaze Plan):**
- 2,000,000 function invocations/month
- 400,000 GB-seconds/month
- Typical usage: ~3,000 notifications/month = **$0**

---

**Status:** ✅ Production Ready
**Last Updated:** March 14, 2024
