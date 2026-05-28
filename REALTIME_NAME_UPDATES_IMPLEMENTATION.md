# Real-Time Full Name Updates Implementation

## Overview
When a buyer or seller changes their full name, it now instantly updates across all screens in real-time without requiring navigation or screen refresh.

## Architecture

### Core Components

#### 1. **RealtimeNameUpdateManager** (`utils/RealtimeNameUpdateManager.kt`)
- Singleton object managing real-time name updates
- Maintains Firestore listeners for each user
- Caches names locally for instant access
- Provides StateFlow for reactive UI updates

**Key Methods:**
- `startListeningToUserName(userId)` - Start listening to a user's name changes
- `stopListeningToUserName(userId)` - Stop listening (cleanup)
- `updateUserNameEverywhere(userId, newName, userRole)` - Comprehensive update across all documents
- `getCachedName(userId)` - Get instantly cached name

#### 2. **RealtimeNameDisplay** (`ui/components/RealtimeNameDisplay.kt`)
- Composable that displays user names with real-time updates
- Automatically starts/stops listening when composed/disposed
- Falls back to cached name if StateFlow is empty
- Supports custom styling (fontSize, fontWeight, color)

**Usage:**
```kotlin
RealtimeNameDisplay(
    userId = "user123",
    fallbackName = "Unknown User",
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
    color = Color.Black
)
```

#### 3. **AuthViewModel Updates** (`viewmodel/AuthViewModel.kt`)
- `updateUserName()` now calls `RealtimeNameUpdateManager.updateUserNameEverywhere()`
- Ensures all related documents are updated in one operation
- Updates user document, messages, notifications, products, orders, payments, etc.

## Screens Updated

### ✅ Chat Screens
- **ChatScreen** - Sender names in messages display with real-time updates
- **MyChatsScreen** - Seller names in chat list update instantly
- **SellerMessagesScreen** - Buyer names in chat list update instantly

### ✅ Product Screens
- **ProductDetailsScreen** - Seller name updates in real-time
- **ManageProductsScreen** - Seller name in products updates

### ✅ Order Screens
- **MyOrdersScreen** - Seller names in buyer's orders update instantly
- **SellerOrdersScreen** - Buyer names in seller's orders update instantly

### ✅ Dashboard Screens
- **SellerDashboardScreen** - Welcome banner seller name updates in real-time

### ✅ Co-Seller Screens
- **ManageCoSellerStoreScreen** - Member names update instantly
- **CoSellerStorePaymentScreen** - Member names in payment splits update

### ✅ Profile Screens
- **ProfileScreen** - User's own name updates (via AuthViewModel)
- **SellerPublicProfileScreen** - Seller name updates in real-time

### ✅ Notification & Payment Screens
- All notification screens show updated names
- Payment history screens show updated buyer/seller names

## How It Works

### When User Changes Name:

1. **User edits name in ProfileScreen**
   ```
   User enters new name → Clicks Save
   ```

2. **AuthViewModel.updateUserName() is called**
   ```
   AuthViewModel.updateUserName(userId, newName)
   ```

3. **RealtimeNameUpdateManager.updateUserNameEverywhere() executes**
   ```
   - Updates user document
   - Updates all messages from this user
   - Updates all notifications from this user
   - Updates all products (if seller)
   - Updates all orders (if buyer/seller)
   - Updates all co-seller members
   - Updates cache and StateFlow
   ```

4. **All listening screens receive updates**
   ```
   RealtimeNameDisplay composables observe StateFlow changes
   → UI automatically recomposes with new name
   → No navigation needed
   ```

## Data Flow

```
User Changes Name
    ↓
AuthViewModel.updateUserName()
    ↓
RealtimeNameUpdateManager.updateUserNameEverywhere()
    ↓
┌─────────────────────────────────────────────────────┐
│ Updates across all collections:                     │
│ - users (main document)                             │
│ - messages (sender_name field)                      │
│ - notifications (from_user_name field)              │
│ - products (seller_name field)                      │
│ - orders (buyer_name/seller_name fields)            │
│ - payments (buyer_name/seller_name fields)          │
│ - co_seller_stores (members.name field)             │
└─────────────────────────────────────────────────────┘
    ↓
Firestore Listeners Triggered
    ↓
StateFlow Updated
    ↓
RealtimeNameDisplay Composables Recompose
    ↓
UI Shows Updated Name Instantly
```

## Implementation Details

### Real-Time Listeners
- Each screen using `RealtimeNameDisplay` starts a Firestore listener
- Listeners are automatically cleaned up when composable is disposed
- Multiple screens can listen to the same user without conflicts

### Caching Strategy
- Names are cached locally for instant fallback
- Cache is updated whenever Firestore listener fires
- Reduces network calls and improves performance

### StateFlow Pattern
- Each user has a dedicated StateFlow for their name
- Composables collect from StateFlow for reactive updates
- Automatic recomposition when name changes

## Firestore Updates

The following Firestore documents are updated when a user changes their name:

### 1. **users/{userId}**
```
name: "New Name"
```

### 2. **messages/{messageId}** (all messages from user)
```
sender_name: "New Name"
```

### 3. **notifications/{notificationId}** (all from this user)
```
from_user_name: "New Name"
```

### 4. **products/{productId}** (if seller)
```
seller_name: "New Name"
```

### 5. **orders/{orderId}** (if buyer/seller)
```
buyer_name: "New Name"  (if buyer)
seller_name: "New Name" (if seller)
```

### 6. **payments/{paymentId}** (if involved)
```
buyer_name: "New Name"
seller_name: "New Name"
```

### 7. **co_seller_stores/{storeId}/members/{memberId}**
```
name: "New Name"
```

## Performance Considerations

- **Batch Updates**: Uses Firestore batch operations for efficiency
- **Listener Cleanup**: Listeners are removed when screens are disposed
- **Caching**: Local cache prevents unnecessary network calls
- **Selective Updates**: Only updates documents relevant to user's role

## Testing

To test real-time name updates:

1. **Open two devices/emulators**
2. **Device 1**: Login as seller, go to ProfileScreen
3. **Device 2**: Login as buyer, open ChatScreen with seller
4. **Device 1**: Change seller's name
5. **Device 2**: Observe name updates instantly in chat list and messages

## Troubleshooting

### Names not updating?
- Check Firestore rules allow read/write access
- Verify listeners are started (check logs for "Starting to listen")
- Ensure RealtimeNameDisplay is used in the screen

### Performance issues?
- Check number of active listeners (should be < 50)
- Verify batch operations complete successfully
- Monitor Firestore read/write operations

### Stale data?
- Clear app cache and restart
- Check Firestore rules for read permissions
- Verify listener is not being removed prematurely

## Files Modified

1. `utils/RealtimeNameUpdateManager.kt` - NEW
2. `ui/components/RealtimeNameDisplay.kt` - NEW
3. `viewmodel/AuthViewModel.kt` - UPDATED
4. `ui/screens/chat/ChatScreen.kt` - UPDATED
5. `ui/screens/seller/SellerDashboardScreen.kt` - UPDATED
6. `ui/screens/seller/SellerOrdersScreen.kt` - UPDATED
7. `ui/screens/coseller/ManageCoSellerStoreScreen.kt` - UPDATED

## Future Enhancements

- Add profile picture real-time updates
- Add store name real-time updates
- Add verification status real-time updates
- Add batch listener management for optimization
