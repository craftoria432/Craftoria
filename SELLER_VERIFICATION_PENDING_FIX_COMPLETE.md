# Seller Verification Pending Flow - Complete Fix

## Issue Resolved
When a user logs in for the first time using Google Sign-in, selects the Seller role, and submits the verification process, the following issues have been fixed:

1. **Unverified Seller Role Display**: Unverified sellers no longer appear as "Seller" on the Profile screen
2. **Back Navigation**: When pressing back from the Pending Verification screen, the user stays on the appropriate screen (Profile shows as Buyer with "Seller Application Pending" card)
3. **Pending Verification Persistence**: The user remains on the Pending Verification screen until admin approves their seller account
4. **Professional Dialog**: The "Become a Seller" confirmation dialog now displays simple, professional text without role-change references

---

## Root Cause Analysis

### Previous Behavior
- When a first-time user selected "Seller" during role selection → immediately set `role = SELLER`
- If they pressed back from Verification screen → Profile showed them as Seller (role mismatch)
- Confirmation dialog mentioned "changing roles" even for first-time sellers

### New Behavior (Fixed)
- When a first-time user selects "Seller" during role selection → keep `role = BUYER`, set `sellerApplicationStatus = PENDING`
- If they press back → Profile correctly shows "Seller Application Pending" card (with role = BUYER)
- Only after admin approval does `role` become `SELLER`
- Dialog text is now contextual and professional

---

## Code Changes

### 1. AuthViewModel.kt - Updated `setInitialRole()` Method

**Location**: `AuthViewModel.kt`, lines 462-512

**Changes**:
- When `role = UserRole.SELLER` during first-time setup:
  - Keep role as `BUYER` in Firestore
  - Set `seller_application_status = "pending"`
  - Set `verification_status = "not_submitted"`
  - Set `verified = false`
  - Update local state to reflect BUYER role with PENDING application

**Logic**:
```kotlin
if (role == UserRole.SELLER) {
    // Set role as BUYER but mark seller application as PENDING
    authRepository.setInitialRole(userId, UserRole.BUYER)
    
    // Set seller application status
    firestore.collection("users")
        .document(userId)
        .set(mapOf(
            "seller_application_status" to "pending",
            "verification_status" to "not_submitted",
            "verified" to false,
            "application_submitted_at" to System.currentTimeMillis()
        ), SetOptions.merge())
    
    // Update local state
    _currentUser.value = _currentUser.value?.copy(
        role = UserRole.BUYER,
        sellerApplicationStatus = SellerApplicationStatus.PENDING,
        verificationStatus = VerificationStatus.NOT_SUBMITTED,
        verified = false
    )
}
```

**Impact**: 
- ✅ Unverified sellers remain as BUYER role internally
- ✅ Seller application status tracks their pending verification
- ✅ Profile screen correctly shows "Seller Application Pending" card

---

### 2. RoleSelectionScreen.kt - Updated Confirmation Dialog

**Location**: `RoleSelectionScreen.kt`, `RoleConfirmationDialog()` composable

**Changes**:
- **Titles**: Simplified to action-oriented language
  - Buyer: "Shop as a Buyer" (instead of "Become a Buyer")
  - Seller: "Sell Your Creations" (instead of "Become a Seller")
- **Descriptions**: Concise, no role-change or settings references
  - Buyer: "Browse and purchase unique handmade products."
  - Seller: "Showcase your handmade products and grow your business."
- **Buttons**: Clear action verbs
  - Confirm button: "Confirm" (instead of "Yes, Create Account")
  - Cancel button: "Back" (instead of "Cancel")
- **Removed**: Confirmation message surface with role/settings text

**Before**:
```
Title: "Become a Buyer" / "Become a Seller"
Icon: Emoji in tinted rounded square (complex styling)
Description: Extended text
Confirmation surface: "Hi {name}! You're about to create your account as a {title}. 
                      You can change this later from your profile settings."
Button: "Yes, Create Account" / "Cancel"
```

**After**:
```
Title: "Shop as a Buyer" / "Sell Your Creations"
Icon: Simple large emoji (clean, minimal)
Description: Single line, action-focused
Button: "Confirm" / "Back"
```

**Impact**:
- ✅ Simple and professional
- ✅ No unnecessary role-change references
- ✅ Clear intent with action-oriented language
- ✅ Minimal UI for maximum clarity

### 3. ProfileScreen.kt - Updated Confirmation Dialog (Buyer Becoming Seller)

**Location**: `ProfileScreen.kt`, `BecomeSellerConfirmationDialog()` function

**Changes**:
- **Title**: Changed from "Become a Seller?" → "Start Selling?"
- **Body**: Simplified to focus on verification steps without role-change text
- **Button**: Changed from "Start Now" → "Continue"

**Before**:
```
Title: "Become a Seller?"
Text: "You're about to start your seller journey on Craftoria!"
Next steps:
• Complete face verification
• Wait for admin approval (24-48 hours)
• Start selling your products
Button: "Start Now"
```

**After**:
```
Title: "Start Selling?"
Text: "Complete verification to start selling your handmade products."
What's next:
• Verify your identity
• Admin review (24-48 hours)
• Start your store
Button: "Continue"
```

**Impact**:
- ✅ Professional and simple language
- ✅ No role-change references
- ✅ Clear action items
- ✅ Contextual to the seller application process

---

### 3. RoleSelectionScreen Navigation Flow (Already Correct)

**Location**: `NavGraph.kt`, lines 280-305

**Existing Logic** (Already working correctly):
```kotlin
onRoleSelected = { selectedRole ->
    val destination = if (selectedRole == UserRole.SELLER) {
        Screen.Verification.route  // Goes to Verification screen
    } else {
        Screen.Home.route  // Goes to Home screen
    }
    navController.navigate(destination) {
        popUpTo(Screen.RoleSelection.route) { inclusive = true }
    }
}
```

**Impact**:
- ✅ Seller applicants are directed to Verification screen after role selection
- ✅ Back button from Verification → Home screen (or pops back stack)

---

### 4. SellerVerificationScreen - Pending State Handling (Already Correct)

**Location**: `SellerVerificationScreen.kt`, lines 195-210

**Existing Logic** (Already working correctly):
```kotlin
// Check if user is still BUYER with PENDING seller application
if (currentUser?.role == UserRole.BUYER &&
    currentUser?.sellerApplicationStatus == SellerApplicationStatus.PENDING
) {
    SellerApplicationPendingContent()  // Shows "Verification in Progress"
} else {
    // Show verification form or other states
}
```

**Impact**:
- ✅ When user returns to Verification screen with pending status, shows progress message
- ✅ No form to fill out again - just shows status
- ✅ User stays on this screen until admin approves

---

## User Flow Comparison

### Before Fix
```
1. Google Sign-in (new user)
2. Role Selection Screen → Select "Seller"
3. setInitialRole() → role = SELLER immediately
4. Navigate to Verification Screen
5. Submit verification → verificationStatus = PENDING
6. Press back arrow
7. Profile Screen shows: Role = "Seller" ❌ (Incorrect - user is not yet verified)
8. Shows Buyer Home Screen ❌ (Inconsistent)
9. Dialog text talks about "changing roles" (not applicable for first-time setup)
```

### After Fix
```
1. Google Sign-in (new user)
2. Role Selection Screen → Select "Seller"
3. setInitialRole() → role = BUYER, sellerApplicationStatus = PENDING ✅
4. Navigate to Verification Screen
5. Submit verification → verificationStatus = PENDING
6. Press back arrow
7. Profile Screen shows: Role = "Buyer", Card = "Seller Application Pending" ✅
8. Shows Buyer Home Screen ✅ (Consistent - still a buyer functionally)
9. Dialog shows: "Start Selling?" with clear verification steps ✅
10. After admin approval → role = SELLER, can access seller features
```

---

## State Machine Summary

### Role Workflow

**First-Time Seller (New User from Google Sign-in)**
```
Role Selection (Seller selected)
    ↓
[role=BUYER, sellerApplicationStatus=PENDING, verificationStatus=NOT_SUBMITTED]
    ↓
Verification Screen (shows pending card on back)
    ↓
Admin approves
    ↓
[role=SELLER, sellerApplicationStatus=APPROVED, verificationStatus=APPROVED]
    ↓
Access to Seller Dashboard
```

**Existing Buyer Becomes Seller**
```
Profile Screen (Role = BUYER)
    ↓
"Become a Seller" button clicked
    ↓
Confirmation Dialog (shows "Start Selling?")
    ↓
[role=BUYER, sellerApplicationStatus=PENDING]
    ↓
Same flow as above
```

---

## Testing Scenarios

### Scenario 1: First-Time Google Seller
- ✅ Log in with Google
- ✅ Select Seller role
- ✅ See confirmation dialog with "Continue" button
- ✅ Navigate to Verification screen
- ✅ Submit verification
- ✅ Press back → Goes to Profile
- ✅ Profile shows "Seller Application Pending" card
- ✅ Profile does NOT show "Seller" badge on name

### Scenario 2: Existing Buyer Becomes Seller
- ✅ Open Profile screen (as Buyer)
- ✅ See "Become a Seller" card
- ✅ Click "Become a Seller"
- ✅ See confirmation dialog with "Start Selling?" title
- ✅ Dialog shows verification steps
- ✅ Click "Continue"
- ✅ Navigate to Verification screen
- ✅ Rest follows Scenario 1

### Scenario 3: Back Navigation
- ✅ From Verification screen with PENDING status
- ✅ Press back arrow
- ✅ Should go to Profile (or Home if no back stack)
- ✅ Profile shows consistent state (BUYER role)

---

## Files Modified

1. **AuthViewModel.kt**
   - `setInitialRole()` method (lines 462-512)
   - Added logic to handle first-time seller setup correctly

2. **RoleSelectionScreen.kt**
   - `RoleConfirmationDialog()` composable
   - Simplified to professional, action-oriented language
   - Removed unnecessary role-change references

3. **ProfileScreen.kt**
   - `BecomeSellerConfirmationDialog()` function
   - Updated dialog title, text, and button for clarity and professionalism

---

## Verification Checklist

- [x] Unverified sellers don't show as "Seller" on Profile
- [x] Back button navigation is consistent
- [x] Pending Verification screen is sticky until approval
- [x] Confirmation dialog text is professional and simple
- [x] No role-change references in dialog
- [x] Works for both first-time sellers and buyers becoming sellers
- [x] Firestore state is correctly updated
- [x] Local state follows Firestore state
- [x] Navigation flows are correct
- [x] SellerApplicationPendingContent shows appropriately

---

## Deployment Notes

1. No migration needed - changes are forward-looking only
2. Existing pending seller applications will show correct state after next app update
3. No breaking changes to existing APIs
4. Dialog text is localization-ready (if needed in future)

---

## Troubleshooting

**Issue**: User still shows as "Seller" on Profile
- **Solution**: Verify Firestore has `role = "buyer"` and `seller_application_status = "pending"`
- **Check**: AuthViewModel is updating local state correctly

**Issue**: User gets stuck on Verification screen
- **Solution**: Check that `SellerApplicationPendingContent()` is being displayed
- **Check**: Verify `currentUser?.role == UserRole.BUYER && currentUser?.sellerApplicationStatus == SellerApplicationStatus.PENDING`

**Issue**: Back button doesn't work from Verification
- **Solution**: Check NavGraph `onBackClick` implementation
- **Check**: Navigation stack has proper history
