# Seller Application Rejection - Real-time Update Fix

## Problem
When an admin rejects a buyer's seller application on the web dashboard, the mobile app doesn't update to show the rejection status. The user continues to see "Application Pending" instead of seeing options to "Try Again" or "Revert to Buyer".

## Root Causes

### Issue 1: Duplicate Listeners
ProfileScreen was calling both `listenToVerificationStatus()` and `listenToUserUpdates()`:

```kotlin
LaunchedEffect(user.id) {
    viewModel.listenToVerificationStatus()      // ❌ Redundant
    viewModel.listenToUserUpdates(user.id)      // ✅ Complete listener
}
```

Both listeners update `_currentUser`, which can cause race conditions and state conflicts. The `listenToUserUpdates()` already handles all user fields including `sellerApplicationStatus`, making `listenToVerificationStatus()` redundant.

### Issue 2: Limited User Actions
The rejected state only showed "Apply Again" button, with no option to revert back to buyer status. Users who changed their mind had no clear path to return to their original buyer account.

## Solutions

### Fix 1: Remove Duplicate Listener
```kotlin
LaunchedEffect(user.id) {
    // ✅ FIX: Only use listenToUserUpdates - handles all fields
    viewModel.listenToUserUpdates(user.id)
}
```

Benefits:
- Single source of truth for user state
- No race conditions between listeners
- Cleaner, more maintainable code
- Guaranteed real-time updates for all user fields

### Fix 2: Enhanced Rejection UI with Two Actions
```kotlin
SellerApplicationStatus.REJECTED -> {
    // Show rejection reason if available
    if (user.rejectionReason.isNotEmpty()) {
        Text("Reason: ${user.rejectionReason}")
    }
    
    // ✅ Two action buttons
    Row {
        OutlinedButton("Revert to Buyer") {
            authViewModel.revertToBuyer(user.id)
        }
        Button("Try Again") {
            authViewModel.resetSellerApplication(user.id)
            navigate("verification")
        }
    }
}
```

## User Experience

### Before Fix
```
[Admin Rejects Application]
  ↓
[Mobile App Still Shows "Pending"] ← Stuck!
  ↓
[User Confused - No Update]
```

### After Fix
```
[Admin Rejects Application]
  ↓
[Mobile App Updates Immediately] ← Real-time!
  ↓
[Shows Rejection + Reason]
  ↓
[Two Clear Options:]
  • Try Again → Reapply
  • Revert to Buyer → Cancel application
```

## Technical Details

### Real-time Listener Flow
```kotlin
listenToUserUpdates(userId) {
    // 1. Fetch initial data immediately
    val initialUser = fetchUser(userId)
    _currentUser.value = initialUser
    
    // 2. Set up snapshot listener
    firestore.collection("users")
        .document(userId)
        .addSnapshotListener { snapshot ->
            val updatedUser = parseUser(snapshot)
            _currentUser.value = updatedUser  // ✅ UI updates automatically
        }
}
```

### State Flow
1. Admin rejects application on web dashboard
2. Firestore updates `seller_application_status` to "rejected"
3. Snapshot listener fires on mobile
4. `_currentUser` StateFlow emits new value
5. ProfileScreen recomposes with REJECTED status
6. UI shows rejection card with two action buttons

### Why Single Listener Works Better
- **Atomic Updates**: All user fields update together
- **No Conflicts**: Single write to `_currentUser`
- **Predictable**: One listener = one update path
- **Efficient**: Fewer Firestore reads

## New Features

### 1. Rejection Reason Display
Shows admin's rejection reason if provided:
```
"Reason: Verification photo quality is too low. Please retake with better lighting."
```

### 2. Revert to Buyer Action
Allows users to cancel their seller application and return to pure buyer status:
- Clears `seller_application_status` to "none"
- Resets verification fields
- Removes rejection reason
- User returns to original buyer state

### 3. Try Again Action
Resets the application and navigates to verification:
- Clears rejection data
- Sets status back to "none"
- Opens verification screen for fresh application

## Files Changed
- ✅ `app/src/main/java/com/gcuf/craftoria/ui/screens/auth/ProfileScreen.kt`

## Testing

### Test Scenario 1: Real-time Rejection Update
1. Buyer submits seller application on mobile
2. Admin rejects application on web dashboard
3. ✅ Mobile app should update immediately to show rejection
4. ✅ Should display rejection reason if provided
5. ✅ Should show both "Try Again" and "Revert to Buyer" buttons

### Test Scenario 2: Try Again Flow
1. User sees rejection status
2. Clicks "Try Again"
3. ✅ Should navigate to verification screen
4. ✅ Application status should reset to "none"
5. ✅ User can submit new application

### Test Scenario 3: Revert to Buyer Flow
1. User sees rejection status
2. Clicks "Revert to Buyer"
3. ✅ Application status should reset to "none"
4. ✅ Should show "Become a Seller" card again
5. ✅ User returns to pure buyer state

### Test Scenario 4: Multiple Rejections
1. User applies → gets rejected → tries again
2. Gets rejected again
3. ✅ Each rejection should show updated reason
4. ✅ Real-time updates should work for all attempts

## Additional Benefits

### Better User Communication
- Clear rejection reasons help users understand what went wrong
- Two action paths give users control over their account
- No confusion about application status

### Improved Admin Workflow
- Rejection reasons are immediately visible to users
- Reduces support tickets about "stuck" applications
- Users can self-serve by trying again or reverting

### Cleaner Codebase
- Single listener reduces complexity
- Easier to debug state issues
- More maintainable long-term

---

## Summary
Fixed seller application rejection real-time updates by:
1. Removing duplicate listener that caused state conflicts
2. Adding "Revert to Buyer" option alongside "Try Again"
3. Displaying rejection reason when provided
4. Ensuring immediate UI updates when admin rejects application

Users now see rejection status instantly and have clear options to either reapply or return to buyer status.
