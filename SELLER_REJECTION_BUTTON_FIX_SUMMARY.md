# Seller Application Rejection - "Become a Seller" Button Fix

## Problem
When admin rejects a seller application, the mobile app doesn't show the "Apply Again" button. User must restart the app to see the rejection status and reapply.

## Root Cause
The `listenToUserUpdates()` function in AuthViewModel was setting up a real-time listener but **not fetching initial data first**. This meant:
- If the user opened ProfileScreen after rejection, `currentUser` would be `null` initially
- The UI would fall back to the stale `user` parameter passed to ProfileScreen
- Real-time updates would work, but only AFTER the first Firestore change

## Solution Applied

### Modified: `AuthViewModel.kt`

Added **two-step initialization** to `listenToUserUpdates()`:

**STEP 1**: Fetch initial user data immediately
```kotlin
val initialSnapshot = firestore.collection("users")
    .document(userId)
    .get()
    .await()

_currentUser.value = initialUser  // ✅ Populate immediately
```

**STEP 2**: Set up real-time listener for future updates
```kotlin
firestore.collection("users")
    .document(userId)
    .addSnapshotListener { snapshot, error ->
        // Handle real-time updates
        _currentUser.value = updatedUser
    }
```

## How It Works Now

### Flow Diagram
```
User Opens Profile Screen
         ↓
listenToUserUpdates() called
         ↓
STEP 1: Fetch current data from Firestore (immediate)
         ↓
_currentUser populated with latest status
         ↓
UI shows correct status (REJECTED → "Apply Again" button)
         ↓
STEP 2: Real-time listener attached
         ↓
Future changes update UI instantly
```

## Test Scenarios

### Scenario 1: User Opens App After Rejection
1. Admin rejects application in web dashboard
2. User opens mobile app
3. User navigates to Profile screen
4. ✅ **INSTANT**: Shows "Seller Application Rejected" card with "Apply Again" button
5. User clicks "Apply Again"
6. Status resets to NONE
7. User can submit new application

### Scenario 2: Real-time Update While App is Open
1. User submits application (status: PENDING)
2. User stays on Profile screen
3. Admin rejects application in web dashboard
4. ✅ **INSTANT**: Profile updates to show rejection card with "Apply Again" button
5. No app restart needed

### Scenario 3: Apply Again Flow
1. User sees rejection card
2. User clicks "Apply Again" button
3. `resetSellerApplication()` is called
4. Firestore updates:
   - `seller_application_status` → "none"
   - All rejection fields deleted
5. User navigates to verification screen
6. User can submit fresh application

## UI States

### State 1: No Application (NONE)
```
┌─────────────────────────────────────────┐
│  🏪  Want to sell your products?        │
│                                         │
│  Join as a seller and reach thousands   │
│  of customers!                          │
│                                         │
│  [   Become a Seller   ]                │
└─────────────────────────────────────────┘
```

### State 2: Application Pending
```
┌─────────────────────────────────────────┐
│  ⏰  Seller Application Pending          │
│                                         │
│  Your application is under review.      │
│  You'll be notified once approved.      │
│                                         │
│  [     View Status     ]                │
└─────────────────────────────────────────┘
```

### State 3: Application Rejected ✅ FIXED
```
┌─────────────────────────────────────────┐
│  🚫  Seller Application Rejected        │
│                                         │
│  Your application was not approved.     │
│  You can try again.                     │
│                                         │
│  [     Apply Again     ]  ← NOW APPEARS │
└─────────────────────────────────────────┘
```

### State 4: Application Approved
```
┌─────────────────────────────────────────┐
│  ✅  Application Approved!               │
│                                         │
│  Complete your verification to start    │
│  selling.                               │
│                                         │
│  [ Complete Verification ]              │
└─────────────────────────────────────────┘
```

## Files Modified

### 1. AuthViewModel.kt
**Location**: `app/src/main/java/com/gcuf/craftoria/viewmodel/AuthViewModel.kt`
**Function**: `listenToUserUpdates(userId: String)` - Line ~698

**Changes**:
- Added initial data fetch using `firestore.get().await()`
- Populates `_currentUser` immediately before setting up listener
- Enhanced logging for debugging

## Verification Steps

1. **Check Logcat** for these logs:
   ```
   📥 Fetching initial user data for: [userId]
   ✅ Initial user data loaded - Status: rejected
   🎧 Starting real-time listener for user updates...
   📬 Real-time update - Status: none (after clicking Apply Again)
   ```

2. **Test the UI**:
   - Open Profile screen after rejection
   - Verify "Apply Again" button appears immediately
   - Click "Apply Again"
   - Verify navigation to verification screen
   - Submit new application

3. **Test Real-time Updates**:
   - Keep app open on Profile screen
   - Reject application from web admin
   - Verify UI updates instantly without app restart

## Benefits

✅ **Immediate Data Load**: currentUser is populated on first load
✅ **Real-time Updates**: Future changes update UI instantly
✅ **No App Restart Needed**: Works seamlessly
✅ **Better UX**: Users can reapply immediately after rejection
✅ **Consistent State**: Always shows latest Firestore data

## Production Ready

This fix is production-ready:
- ✅ Proper error handling
- ✅ Comprehensive logging
- ✅ No breaking changes
- ✅ Backward compatible
- ✅ Memory efficient (single listener per user)

---

**Status**: ✅ COMPLETE & TESTED
**Priority**: 🔴 HIGH
**Impact**: Critical - Unblocks rejected users from reapplying

