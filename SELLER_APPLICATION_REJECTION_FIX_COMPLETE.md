# Seller Application Rejection - Button Not Reappearing Fix

## Problem Report

**Issue**: Once a seller application is rejected by admin in mobile, the "Become a Seller" button does not reappear. User cannot apply again.

## Root Cause Analysis

The issue occurs because:

1. ✅ **Real-time listener is implemented** - `listenToUserUpdates()` exists
2. ✅ **ProfileScreen calls the listener** - `viewModel.listenToUserUpdates(user.id)` is present
3. ❌ **Potential Issue**: The ProfileScreen might be using the initial `user` parameter instead of the real-time updated `currentUser` from the ViewModel

### The Bug

```kotlin
// ProfileScreen.kt - Line 65
@Composable
fun ProfileScreen(
    user: User,  // ❌ This is the INITIAL user passed as parameter
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateTo: (String) -> Unit,
    unreadMessageViewModel: UnreadMessageViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()  // ✅ This gets real-time updates
    val displayUser = currentUser ?: user  // ✅ Falls back to initial user
    
    // ... rest of the code uses displayUser
}
```

**The issue**: If `currentUser` is `null` initially, `displayUser` will use the stale `user` parameter, and when the status changes, the UI might not update properly.

## Solution

### Fix 1: Ensure currentUser is Always Updated

The `listenToUserUpdates()` function should be called immediately and should always populate `_currentUser`:

```kotlin
// AuthViewModel.kt
fun listenToUserUpdates(userId: String) {
    viewModelScope.launch {
        try {
            Log.d("AuthViewModel", "🎧 Starting real-time listener for user: $userId")
            
            firestore.collection("users")
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AuthViewModel", "❌ Error listening to user updates", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val data = snapshot.data ?: return@addSnapshotListener
                            
                            val updatedUser = User(
                                id = userId,
                                email = data["email"] as? String ?: "",
                                name = data["name"] as? String ?: "",
                                role = UserRole.fromString(data["role"] as? String),
                                phone = data["phone"] as? String ?: "",
                                address = data["address"] as? String ?: "",
                                profileImage = data["profile_image"] as? String ?: "",
                                createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time 
                                    ?: (data["created_at"] as? Long) ?: 0L,
                                storeName = data["store_name"] as? String ?: "",
                                storeDescription = data["store_description"] as? String ?: "",
                                verified = data["verified"] as? Boolean ?: false,
                                verificationStatus = VerificationStatus.fromString(data["verification_status"] as? String),
                                verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                                rejectionReason = data["rejection_reason"] as? String ?: "",
                                mainSellerId = data["main_seller_id"] as? String ?: "",
                                sellerApplicationStatus = SellerApplicationStatus.fromString(
                                    data["seller_application_status"] as? String
                                ),
                                themePreference = data["theme_preference"] as? String ?: "rose"
                            )
                            
                            _currentUser.value = updatedUser
                            
                            Log.d("AuthViewModel", """
                                ✅ User data updated in real-time:
                                - Name: ${updatedUser.name}
                                - Role: ${updatedUser.role}
                                - Seller Application Status: ${updatedUser.sellerApplicationStatus}
                                - Verification Status: ${updatedUser.verificationStatus}
                            """.trimIndent())
                            
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "❌ Error parsing user data", e)
                        }
                    } else {
                        Log.w("AuthViewModel", "⚠️ User document does not exist")
                    }
                }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "❌ Exception setting up user listener", e)
        }
    }
}
```

### Fix 2: Initialize currentUser Immediately

Add an initial fetch before setting up the listener:

```kotlin
// AuthViewModel.kt
fun listenToUserUpdates(userId: String) {
    viewModelScope.launch {
        try {
            // ✅ STEP 1: Fetch initial data immediately
            val initialSnapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (initialSnapshot.exists()) {
                val data = initialSnapshot.data ?: return@launch
                val initialUser = User(
                    id = userId,
                    email = data["email"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    role = UserRole.fromString(data["role"] as? String),
                    phone = data["phone"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    profileImage = data["profile_image"] as? String ?: "",
                    createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time 
                        ?: (data["created_at"] as? Long) ?: 0L,
                    storeName = data["store_name"] as? String ?: "",
                    storeDescription = data["store_description"] as? String ?: "",
                    verified = data["verified"] as? Boolean ?: false,
                    verificationStatus = VerificationStatus.fromString(data["verification_status"] as? String),
                    verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                    rejectionReason = data["rejection_reason"] as? String ?: "",
                    mainSellerId = data["main_seller_id"] as? String ?: "",
                    sellerApplicationStatus = SellerApplicationStatus.fromString(
                        data["seller_application_status"] as? String
                    ),
                    themePreference = data["theme_preference"] as? String ?: "rose"
                )
                
                _currentUser.value = initialUser
                Log.d("AuthViewModel", "✅ Initial user data loaded: ${initialUser.sellerApplicationStatus}")
            }
            
            // ✅ STEP 2: Set up real-time listener for future updates
            Log.d("AuthViewModel", "🎧 Starting real-time listener for user: $userId")
            
            firestore.collection("users")
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AuthViewModel", "❌ Error listening to user updates", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val data = snapshot.data ?: return@addSnapshotListener
                            
                            val updatedUser = User(
                                id = userId,
                                email = data["email"] as? String ?: "",
                                name = data["name"] as? String ?: "",
                                role = UserRole.fromString(data["role"] as? String),
                                phone = data["phone"] as? String ?: "",
                                address = data["address"] as? String ?: "",
                                profileImage = data["profile_image"] as? String ?: "",
                                createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time 
                                    ?: (data["created_at"] as? Long) ?: 0L,
                                storeName = data["store_name"] as? String ?: "",
                                storeDescription = data["store_description"] as? String ?: "",
                                verified = data["verified"] as? Boolean ?: false,
                                verificationStatus = VerificationStatus.fromString(data["verification_status"] as? String),
                                verificationPhotoUrl = data["verification_photo_url"] as? String ?: "",
                                rejectionReason = data["rejection_reason"] as? String ?: "",
                                mainSellerId = data["main_seller_id"] as? String ?: "",
                                sellerApplicationStatus = SellerApplicationStatus.fromString(
                                    data["seller_application_status"] as? String
                                ),
                                themePreference = data["theme_preference"] as? String ?: "rose"
                            )
                            
                            _currentUser.value = updatedUser
                            
                            Log.d("AuthViewModel", """
                                📬 Real-time update received:
                                - Seller Application Status: ${updatedUser.sellerApplicationStatus}
                                - Verification Status: ${updatedUser.verificationStatus}
                            """.trimIndent())
                            
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "❌ Error parsing user data", e)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "❌ Exception setting up user listener", e)
        }
    }
}
```

## Testing Steps

### Test 1: Rejection Flow
1. **Mobile**: User submits seller application
2. **Mobile**: Profile shows "Seller Application Pending" card
3. **Web Admin**: Admin rejects the application
4. **Mobile**: Profile should INSTANTLY show "Seller Application Rejected" card with "Apply Again" button
5. **Mobile**: Click "Apply Again"
6. **Mobile**: Should navigate to verification screen
7. **Mobile**: Submit new application

### Test 2: Approval Flow
1. **Mobile**: User submits seller application
2. **Mobile**: Profile shows "Seller Application Pending" card
3. **Web Admin**: Admin approves the application
4. **Mobile**: Profile should INSTANTLY show "Application Approved!" card with "Complete Verification" button

### Test 3: App Restart
1. **Mobile**: User submits seller application
2. **Web Admin**: Admin rejects the application
3. **Mobile**: Close and restart the app
4. **Mobile**: Open Profile screen
5. **Mobile**: Should show "Seller Application Rejected" card with "Apply Again" button

## Verification Checklist

- [ ] Real-time listener is called in ProfileScreen LaunchedEffect
- [ ] currentUser StateFlow is properly collected
- [ ] displayUser uses currentUser when available
- [ ] Rejection status shows "Apply Again" button
- [ ] "Apply Again" button calls resetSellerApplication()
- [ ] resetSellerApplication() clears all rejection data
- [ ] User can navigate to verification screen after clicking "Apply Again"
- [ ] Logs show real-time updates in Logcat

## Expected Behavior

### When Application is Rejected

**ProfileScreen should display**:
```
┌─────────────────────────────────────────┐
│  🚫  Seller Application Rejected        │
│                                         │
│  Your application was not approved.     │
│  You can try again.                     │
│                                         │
│  [     Apply Again     ]                │
└─────────────────────────────────────────┘
```

### When "Apply Again" is Clicked

1. `resetSellerApplication(userId)` is called
2. Firestore updates:
   - `seller_application_status` → "none"
   - `verification_status` → "not_submitted"
   - All rejection fields deleted
3. User navigates to verification screen
4. User can submit fresh application

## Files to Check

1. **AuthViewModel.kt** - Line 698-750
   - Verify `listenToUserUpdates()` implementation
   - Check if initial fetch is present
   - Verify real-time listener setup

2. **ProfileScreen.kt** - Line 85-95
   - Verify `listenToUserUpdates()` is called
   - Check if `currentUser` is collected
   - Verify `displayUser` logic

3. **ProfileScreen.kt** - Line 350-400
   - Verify rejection card UI
   - Check "Apply Again" button implementation
   - Verify `resetSellerApplication()` call

## Common Issues & Solutions

### Issue 1: Button Still Not Appearing
**Cause**: currentUser is null
**Solution**: Add initial fetch before listener (Fix 2 above)

### Issue 2: Updates Not Real-time
**Cause**: Listener not properly attached
**Solution**: Check Logcat for listener setup logs

### Issue 3: "Apply Again" Does Nothing
**Cause**: resetSellerApplication() not clearing data
**Solution**: Verify Firestore updates in resetSellerApplication()

## Production Deployment

Before deploying:
1. Test all three scenarios above
2. Check Logcat for proper logging
3. Verify Firestore data is updated correctly
4. Test on multiple devices
5. Verify no memory leaks from listeners

---

**Status**: 🔧 FIX READY TO APPLY
**Priority**: 🔴 HIGH
**Impact**: Critical user flow - users cannot reapply after rejection

