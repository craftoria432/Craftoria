# Real-Time Name Updates - Quick Start Guide

## What Was Implemented

When a buyer or seller changes their full name in the ProfileScreen, the name now updates **instantly** across all screens without requiring navigation or screen refresh.

## How It Works (Simple Explanation)

1. **User changes name** → Clicks Save in ProfileScreen
2. **System updates everywhere** → All documents with that user's name are updated in Firestore
3. **All screens listening** → Screens automatically show the new name in real-time
4. **No refresh needed** → UI updates instantly without navigation

## Screens That Update in Real-Time

✅ **Chat Screens**
- Chat list (MyChatsScreen, SellerMessagesScreen)
- Individual messages (ChatScreen)

✅ **Product Screens**
- Product details (ProductDetailsScreen)
- Manage products (ManageProductsScreen)

✅ **Order Screens**
- Buyer's orders (MyOrdersScreen)
- Seller's orders (SellerOrdersScreen)

✅ **Dashboard**
- Seller dashboard welcome banner (SellerDashboardScreen)

✅ **Co-Seller Screens**
- Store members (ManageCoSellerStoreScreen)
- Payment splits (CoSellerStorePaymentScreen)

✅ **Profile Screens**
- User's own profile (ProfileScreen)
- Seller's public profile (SellerPublicProfileScreen)

✅ **Payment & Notification Screens**
- Payment history (PaymentHistoryScreen)
- Seller payments (SellerPaymentsScreen)
- Notifications (NotificationsScreen)

## Technical Implementation

### New Files Created

1. **RealtimeNameUpdateManager.kt** - Core manager for real-time updates
   - Manages Firestore listeners
   - Caches names locally
   - Provides StateFlow for reactive updates

2. **RealtimeNameDisplay.kt** - Composable for displaying names
   - Automatically listens to name changes
   - Updates UI in real-time
   - Cleans up listeners when disposed

### Files Modified

1. **AuthViewModel.kt** - Updated `updateUserName()` method
   - Now calls RealtimeNameUpdateManager
   - Updates all related documents

2. **ChatScreen.kt** - Added real-time sender names
3. **SellerDashboardScreen.kt** - Added real-time seller name
4. **SellerOrdersScreen.kt** - Added real-time buyer names
5. **ManageCoSellerStoreScreen.kt** - Added real-time member names

## How to Use RealtimeNameDisplay

In any screen where you need to display a user's name with real-time updates:

```kotlin
RealtimeNameDisplay(
    userId = "user123",
    fallbackName = "Unknown User",
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
    color = Color.Black
)
```

The composable will:
- Start listening to the user's name changes
- Display the name with real-time updates
- Clean up listeners when the screen is disposed

## What Gets Updated When Name Changes

When a user changes their name, the following Firestore documents are updated:

- **users/{userId}** - Main user document
- **messages/{messageId}** - All messages from this user
- **notifications/{notificationId}** - All notifications from this user
- **products/{productId}** - All products (if seller)
- **orders/{orderId}** - All orders (if buyer/seller)
- **payments/{paymentId}** - All payments (if involved)
- **co_seller_stores/{storeId}/members** - Co-seller members

## Performance

- **Efficient**: Uses Firestore batch operations
- **Cached**: Names are cached locally for instant access
- **Optimized**: Listeners are cleaned up when screens are disposed
- **Selective**: Only updates documents relevant to user's role

## Testing

To test real-time name updates:

1. Open the app on two devices/emulators
2. Device 1: Login as seller, go to ProfileScreen
3. Device 2: Login as buyer, open ChatScreen with seller
4. Device 1: Change seller's name
5. Device 2: Watch the name update instantly in chat list and messages

## Troubleshooting

**Names not updating?**
- Check Firestore rules allow read/write access
- Verify the screen is using RealtimeNameDisplay
- Check browser console for errors

**Performance issues?**
- Check number of active listeners
- Verify batch operations complete successfully
- Monitor Firestore read/write operations

**Stale data?**
- Clear app cache and restart
- Check Firestore rules for read permissions
- Verify listener is not being removed prematurely

## Architecture Diagram

```
User Changes Name
    ↓
ProfileScreen → AuthViewModel.updateUserName()
    ↓
RealtimeNameUpdateManager.updateUserNameEverywhere()
    ↓
Updates all Firestore documents:
├─ users/{userId}
├─ messages/{messageId}
├─ notifications/{notificationId}
├─ products/{productId}
├─ orders/{orderId}
├─ payments/{paymentId}
└─ co_seller_stores/{storeId}/members
    ↓
Firestore Listeners Triggered
    ↓
StateFlow Updated
    ↓
RealtimeNameDisplay Composables Recompose
    ↓
UI Shows Updated Name Instantly ✅
```

## Key Features

✅ **Instant Updates** - No delay, no refresh needed
✅ **Comprehensive** - Updates across all screens
✅ **Efficient** - Batch operations, local caching
✅ **Clean** - Automatic listener cleanup
✅ **Reactive** - Uses Kotlin StateFlow
✅ **Composable** - Easy to use in any screen

## Future Enhancements

- Real-time profile picture updates
- Real-time store name updates
- Real-time verification status updates
- Batch listener management for optimization
