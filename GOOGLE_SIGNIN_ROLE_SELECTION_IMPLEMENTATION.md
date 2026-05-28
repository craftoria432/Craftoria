# Google Sign-In Role Selection Implementation

## Overview
Implemented a two-stage flow for first-time Google sign-in users that detects new accounts and presents a role selection screen before redirecting to the main app.

## Architecture

### 1. **Detection of First-Time Users**
- **Location**: `AuthRepository.signInWithGoogle()`
- **Mechanism**: 
  - Checks if user document exists in Firestore
  - Returns `SignInResult(user, isNewUser)` wrapper
  - New users are created with default BUYER role
  - `isNewUser` flag signals the calling screen

### 2. **Role Selection Screen**
- **File**: `RoleSelectionScreen.kt` (NEW)
- **Features**:
  - Beautiful card-based UI with Buyer/Seller options
  - Real-time loading state during role submission
  - Error handling with user feedback
  - Animated selection indicators
  - Informational text about role flexibility

### 3. **Navigation Flow**
- **Route**: `role_selection/{userId}/{userName}`
- **Trigger**: After Google sign-in if `isNewUser == true`
- **Outcomes**:
  - **Buyer selected** → Navigate to Home Screen
  - **Seller selected** → Navigate to Seller Verification Screen (for document submission)

### 4. **ViewModel Integration**
- **Function**: `AuthViewModel.setInitialRole(userId, role)`
- **Behavior**:
  - Updates user's role in Firestore
  - Updates local `_currentUser` state
  - Emits success/error through `authState` StateFlow

## Code Changes

### New Files
```
app/src/main/java/com/gcuf/craftoria/ui/screens/auth/RoleSelectionScreen.kt
```

### Modified Files

#### 1. **AuthRepository.kt**
- Already had `SignInResult` wrapper (no changes needed)
- Already had `setInitialRole()` function (no changes needed)
- `signInWithGoogle()` already returns `SignInResult` with `isNewUser` flag

#### 2. **AuthViewModel.kt**
- Already had `setInitialRole()` function (no changes needed)
- Already had `signInWithGoogle()` with `onNewUser` callback
- **Fixed listener leaks**:
  - Removed redundant `listenToVerificationStatus()` calls
  - Removed redundant `listenToUserUpdates()` calls
  - Consolidated to single `startRealtimeUserListener()` in `observeAuthState()`
  - Properly stores and removes `ListenerRegistration` in `onCleared()`

#### 3. **LoginScreen.kt**
- Added `onNavigateToRoleSelection` parameter
- Updated Google sign-in handler to capture `isNewUser` flag
- Added navigation logic:
  ```kotlin
  if (isNewUser) {
      onNavigateToRoleSelection(user.id, user.name)
  } else if (user.role == UserRole.SELLER && ...) {
      onNavigateToVerification()
  } else {
      onNavigateToHome()
  }
  ```

#### 4. **NavGraph.kt**
- Added `RoleSelection` route with userId and userName parameters
- Added composable for `RoleSelectionScreen`
- Imported `RoleSelectionScreen`
- Navigation logic:
  - After role selection, routes to Verification (seller) or Home (buyer)

#### 5. **SellerVerificationScreen.kt**
- Removed deprecated `listenToVerificationStatus()` call
- Real-time listener already active via `AuthViewModel.observeAuthState()`

#### 6. **ProfileScreen.kt**
- Removed deprecated `listenToUserUpdates()` call
- Real-time listener already active via `AuthViewModel.observeAuthState()`

## Listener Leak Fixes

### Problem
- `listenToVerificationStatus()` and `listenToUserUpdates()` created separate Firestore listeners
- These listeners were never stored or removed, causing memory leaks
- Multiple listeners watching the same document caused redundant Firestore reads

### Solution
- **Consolidated** all user updates into single `startRealtimeUserListener()` in `AuthViewModel`
- **Stored** `ListenerRegistration` in `userListenerRegistration` variable
- **Removed** listener in `stopRealtimeUserListener()` called from `onCleared()`
- **Removed** all calls to deprecated methods from screens

### Benefits
- ✅ Single listener per user (no redundant Firestore reads)
- ✅ Proper cleanup on ViewModel destruction
- ✅ No memory leaks
- ✅ Cleaner architecture

## User Flow

### First-Time Google Sign-In (New User)
```
1. User clicks "Sign in with Google"
2. Google authentication succeeds
3. AuthRepository checks if user exists in Firestore
4. User doesn't exist → Create with BUYER role, set isNewUser=true
5. AuthViewModel receives SignInResult with isNewUser=true
6. LoginScreen detects isNewUser and navigates to RoleSelectionScreen
7. User selects role (Buyer or Seller)
8. AuthViewModel.setInitialRole() updates Firestore
9. Navigation:
   - Buyer → Home Screen
   - Seller → Seller Verification Screen (until verified by admin)
```

### Existing User Google Sign-In
```
1. User clicks "Sign in with Google"
2. Google authentication succeeds
3. AuthRepository finds user in Firestore, sets isNewUser=false
4. AuthViewModel receives SignInResult with isNewUser=false
5. LoginScreen skips role selection
6. Navigation based on existing role:
   - Buyer → Home Screen
   - Seller (verified) → Seller Dashboard
   - Seller (not verified) → Seller Verification Screen
```

## Testing Checklist

- [ ] First-time Google sign-in shows role selection screen
- [ ] Buyer selection navigates to Home Screen
- [ ] Seller selection navigates to Seller Verification Screen
- [ ] Role is correctly saved to Firestore
- [ ] Existing users skip role selection
- [ ] Seller verification status is checked correctly
- [ ] No Firestore listener leaks (check Firebase console)
- [ ] Real-time user updates work correctly
- [ ] Profile changes reflect immediately across screens

## OTP Reset Flow Issue (Separate)

**Note**: The OTP reset flow has a logic gap that should be addressed separately:
- `verifyOtpAndResetPassword()` marks OTP as used and sends Firebase reset email
- But the `newPassword` parameter is never used
- User types password in UI but it's not applied anywhere
- Firebase reset email asks user to set password again (confusing UX)

**Recommendation**: Either use `firebaseUser.updatePassword(newPassword)` directly (requires user to be signed in) or drop the `newPassword` parameter and just send Firebase reset email.

## Summary

✅ **Completed**:
- Two-stage flow for first-time Google sign-in users
- Beautiful role selection screen
- Proper navigation routing
- Fixed listener leaks in AuthViewModel
- Removed deprecated listener calls from screens
- Consolidated to single real-time listener

✅ **Benefits**:
- Better UX for new users
- Clear role selection before app access
- Proper seller verification flow
- No Firestore listener leaks
- Cleaner, more maintainable code
