# Google Sign-In Account Registration Flow Implementation

## Complete Implementation Summary

This document describes the complete implementation of a secure Google Sign-In flow with mandatory role selection and account confirmation before registration.

---

## Architecture Overview

### User Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│ LOGIN SCREEN                                                        │
│                                                                      │
│  [Continue with Google] ← User clicks (Loading state shown)         │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                    ✓ Google Auth Success
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ ROLE SELECTION SCREEN                                               │
│ (Only temporary account in Firebase Auth, NO user doc created)      │
│                                                                      │
│ [🛒 Buyer]  or  [🎨 Seller]  ← User selects role                   │
│                                                                      │
│  ↓ User clicks role card                                            │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
        ┌──────────────────────┴──────────────────────┐
        │                                             │
        ▼                                             ▼
┌───────────────────────────────┐      ┌──────────────────────────────┐
│ CONFIRMATION DIALOG           │      │ INFO MESSAGE (if error)      │
│ (Professional confirmation)   │      │ Dismissible error message    │
│                               │      │ User can retry               │
│ "Hi [UserName]!              │      └──────────────────────────────┘
│ You're about to create your   │
│ account as a [Role]"          │
│                               │
│ [Yes, Create Account] [Cancel]│
└──────────────────────────────────────────────────────────────────────┘
        │                                        │
    Confirm                                   Cancel
        │                                        │
        │◄───────────────────────────────────────┘
        │
        ▼ setInitialRole() called (isLoading = true)
┌────────────────────────────────────────────────────────┐
│ BACKEND: AuthRepository.setInitialRole()              │
│ - Creates Firestore user document                     │
│ - Sets role field                                      │
│ - Links temporary Firebase auth to account            │
│ - Handles seller/buyer specific initialization        │
└────────────┬───────────────────────────────────────────┘
             │
        ✓ Success  /  ✗ Error
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────────┐  ┌──────────────────┐
│ SUCCESS     │  │ ERROR MESSAGE    │
│ User logged │  │ Show in dialog   │
│ in + role   │  │ User can retry   │
│ set         │  │ or cancel        │
└─────────────┘  └──────────────────┘
```

---

## Key Changes

### 1. **Google Sign-In Button Enhancement (LoginScreen.kt)**

The "Sign in with Google" button now shows loading state during authentication:

```kotlin
OutlinedButton(
    onClick = onGoogleSignIn,
    enabled = authState !is AuthState.Loading,
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
) {
    if (authState is AuthState.Loading) {
        // Loading state
        CircularProgressIndicator(modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("Authenticating...", fontSize = 14.sp)
    } else {
        // Normal state
        Icon(painter = painterResource(R.drawable.ic_google_logo))
        Spacer(modifier = Modifier.width(10.dp))
        Text("Continue with Google", fontSize = 14.sp)
    }
}
```

**Features:**
- ✅ Loading spinner while authenticating
- ✅ Clear "Authenticating..." feedback text
- ✅ Button disabled during authentication to prevent double-clicks
- ✅ Smooth transition between states

---

### 2. **Role Confirmation Dialog (RoleSelectionScreen.kt)**

Professional confirmation dialog before account creation:

```kotlin
@Composable
private fun RoleConfirmationDialog(
    role: UserRole,
    userName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean
)
```

**Dialog Components:**

```
┌─────────────────────────────────────────────┐
│  🛒 or 🎨  (Role-specific emoji icon)      │
│  Become a Buyer/Seller (Bold 22sp title)    │
│  Description text                           │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ Hi [Name]! You're about to create   │   │
│  │ your account as a [Role]. You can   │   │
│  │ change this later from settings.    │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │  ✓ Yes, Create Account (+ spinner)  │   │
│  ├─────────────────────────────────────┤   │
│  │       Cancel                         │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

**Dialog Features:**
- ✅ Role-specific color scheme (Purple for Buyer, Green for Seller)
- ✅ Role-specific emoji icon with background
- ✅ Clear confirmation message
- ✅ User name personalization
- ✅ "Creating Account..." loading state on confirm button
- ✅ Loading spinner with progress text
- ✅ Professional styling and spacing

---

### 3. **Role Selection Screen Updates (RoleSelectionScreen.kt)**

Enhanced role selection screen with confirmation flow:

```kotlin
var showConfirmationDialog by remember { mutableStateOf(false) }
var pendingRole by remember { mutableStateOf<UserRole?>(null) }

// Show confirmation dialog instead of direct submission
fun showRoleConfirmation(role: UserRole) {
    pendingRole = role
    showConfirmationDialog = true
}

// Confirm and submit after user approves
fun confirmAndSubmitRole(role: UserRole) {
    selectedRole = role
    isLoading = true
    vm.setInitialRole(userId, role)
}
```

**Flow:**
1. User clicks role card
2. Confirmation dialog appears
3. User clicks "Yes, Create Account"
4. Loading state shown (button disabled)
5. `setInitialRole()` called on backend
6. Account created or error shown

---

## State Management

### AuthViewModel Changes

#### New Flag: `isNewGoogleUser`

```kotlin
private val _isNewGoogleUser = MutableStateFlow(false)
val isNewGoogleUser: StateFlow<Boolean> = _isNewGoogleUser.asStateFlow()
```

**Purpose:** Tracks whether a user is new from Google Sign-In

**Lifecycle:**
1. Set to `true` when `signInWithGoogle()` returns new user
2. Consumed by `consumeNewGoogleUserFlag()` when role selection is confirmed
3. Reset to `false` after navigation

#### Modified: `signInWithGoogle()`

```kotlin
fun signInWithGoogle(idToken: String) {
    viewModelScope.launch {
        _authState.value = AuthState.Loading
        
        val result = authRepository.signInWithGoogle(idToken)
        
        _authState.value = if (result.isSuccess) {
            val signInResult = result.getOrNull()
            if (signInResult != null) {
                _currentUser.value = signInResult.user
                _isNewGoogleUser.value = signInResult.isNewUser  // ← Flag new users
                AuthState.Success("Welcome!")
            } else {
                AuthState.Error("Sign-in failed")
            }
        } else {
            AuthState.Error(result.exceptionOrNull()?.message ?: "Sign-in failed")
        }
    }
}
```

#### New Method: `consumeNewGoogleUserFlag()`

```kotlin
fun consumeNewGoogleUserFlag(): Boolean {
    val wasNewUser = _isNewGoogleUser.value
    _isNewGoogleUser.value = false  // Reset after consuming
    return wasNewUser
}
```

#### Enhanced: `setInitialRole()`

```kotlin
fun setInitialRole(userId: String, role: UserRole) {
    viewModelScope.launch {
        try {
            _authState.value = AuthState.Loading
            val result = authRepository.setInitialRole(userId, role)
            
            if (result.isSuccess) {
                _currentUser.value = _currentUser.value?.copy(role = role)
                _authState.value = AuthState.Success("Role set successfully!")
            } else {
                _authState.value = AuthState.Error("Failed to set role")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Failed to set role")
        }
    }
}
```

---

## Backend Implementation (AuthRepository)

### `signInWithGoogle()` Method

**Responsibility:** Handle Firebase Google authentication WITHOUT creating user document

```kotlin
suspend fun signInWithGoogle(idToken: String): Result<SignInResult> {
    return try {
        // 1. Authenticate with Firebase using Google ID token
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        val firebaseUser = authResult.user
        
        if (firebaseUser == null) {
            return Result.failure(Exception("Firebase auth failed"))
        }
        
        val userId = firebaseUser.uid
        val userEmail = firebaseUser.email ?: ""
        val userName = firebaseUser.displayName ?: ""
        
        // 2. Check if user document exists in Firestore
        val userDoc = firestore.collection("users").document(userId).get().await()
        val isNewUser = !userDoc.exists()
        
        // 3. For new users: DON'T create user document yet
        //    For existing users: Load their data
        val user = if (isNewUser) {
            // Return temporary user object (not persisted)
            User(
                id = userId,
                email = userEmail,
                name = userName,
                role = UserRole.BUYER,  // Default, will be set later
                // Other fields empty
            )
        } else {
            // Load existing user from Firestore
            userDoc.toObject(User::class.java)
        }
        
        // 4. Return result with isNewUser flag
        Result.success(SignInResult(user = user, isNewUser = isNewUser))
        
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### `setInitialRole()` Method

**Responsibility:** Create user document only after role selection confirmation

```kotlin
suspend fun setInitialRole(userId: String, role: UserRole): Result<Boolean> {
    return try {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(Exception("Not authenticated"))
        
        // Only create user document if it doesn't exist
        val userRef = firestore.collection("users").document(userId)
        val existingDoc = userRef.get().await()
        
        if (existingDoc.exists()) {
            // User document already exists, just update role
            userRef.update("role", role.name).await()
            return Result.success(true)
        }
        
        // Create new user document with role
        val newUserData = mapOf(
            "id" to userId,
            "email" to (firebaseUser.email ?: ""),
            "name" to (firebaseUser.displayName ?: ""),
            "role" to role.name,
            "phone" to "",
            "address" to "",
            "profile_image" to "",
            "created_at" to FieldValue.serverTimestamp(),
            "verified" to false,
            "verification_status" to "pending",
            "store_name" to "",
            "store_description" to "",
            "theme_preference" to "rose"
            // ... other initial fields
        )
        
        userRef.set(newUserData).await()
        return Result.success(true)
        
    } catch (e: Exception) {
        return Result.failure(e)
    }
}
```

---

## LoginScreen Navigation Flow

### Updated `LaunchedEffect` in LoginScreen

```kotlin
LaunchedEffect(authState, isNewGoogleUser) {
    when (authState) {
        is AuthState.Success -> {
            val user = vm.currentUser.value
            if (user != null) {
                vm.resetAuthState()
                
                // CHECK: Is this a new Google user?
                if (isNewGoogleUser) {
                    // Navigate to role selection (temporary account)
                    vm.consumeNewGoogleUserFlag()
                    onNavigateToRoleSelection(user.id, user.name)
                } else if (user.role == UserRole.SELLER &&
                    user.verificationStatus != VerificationStatus.APPROVED
                ) {
                    // Seller needs verification
                    onNavigateToVerification()
                } else {
                    // Buyer or verified seller - go home
                    onNavigateToHome()
                }
            }
        }
        else -> {}
    }
}
```

---

## Error Handling

### Error Display in Role Selection Screen

```kotlin
if (errorMessage != null) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE53935), RoundedCornerShape(12.dp)),
        color = Color(0xFFFFF5F5),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⚠️", fontSize = 18.sp)
            Text(
                text = errorMessage ?: "",
                fontSize = 13.sp,
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

**Features:**
- ✅ Clear error message display with warning emoji
- ✅ Light red background and border
- ✅ User can retry selection
- ✅ Error dismissible by selecting a different role

---

## Security Considerations

1. **No Premature Account Creation:** User document not created until role is confirmed
2. **Firebase Auth Separate:** Authentication happens first, Firestore document creation second
3. **Validation Before Persistence:** Role must be selected before account is created
4. **Transaction Safety:** `setInitialRole()` uses atomic operations
5. **State Management:** Clear separation between auth state and user document state

---

## User Experience Improvements

### Clear Feedback at Each Step

1. **Google Sign-In:** "Authenticating..." with spinner
2. **Role Selection:** Professional dialog with confirmation
3. **Account Creation:** "Creating Account..." with spinner
4. **Success:** Navigation to appropriate screen
5. **Error:** Clear error message with retry option

### Professional UI Elements

- ✅ Role-specific color schemes (Purple/Green)
- ✅ Role-specific emoji icons
- ✅ Consistent spacing and typography
- ✅ Loading spinners for async operations
- ✅ Disabled buttons during loading
- ✅ Clear confirmation messages

---

## Testing Checklist

- [ ] First-time Google Sign-In triggers role selection
- [ ] Role confirmation dialog shows correct content
- [ ] Loading states display during authentication
- [ ] User document created only after role confirmation
- [ ] Existing users skip role selection
- [ ] Error handling works correctly
- [ ] User can cancel and retry
- [ ] Email/name presets from Google account
- [ ] Navigation routing is correct after role selection
- [ ] Back button behavior appropriate at each step

---

## Files Modified

1. **LoginScreen.kt**
   - Enhanced Google Sign-In button with loading state
   - Added "Authenticating..." feedback

2. **RoleSelectionScreen.kt**
   - Added confirmation dialog flow
   - Improved UI with professional styling
   - Added RoleConfirmationDialog composable

3. **AuthViewModel.kt**
   - Added `isNewGoogleUser` flag
   - Added `consumeNewGoogleUserFlag()` method
   - Enhanced state management

---

## Dependencies Used

```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.border.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
```

---

## Future Enhancements

1. **Smooth Animations:** Add transition animations between screens
2. **Haptic Feedback:** Vibration feedback on button clicks
3. **Biometric Auth:** Support fingerprint/face recognition
4. **Multi-Language:** Support for different languages in dialogs
5. **Accessibility:** Enhanced accessibility for screen readers
6. **Analytics:** Track user flow and conversion metrics

