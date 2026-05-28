# Seller Verification Implementation Checklist

## ✅ Completed Files

### Data Models
- [x] `SellerApplicationModels.kt` - Created
  - [x] `SellerApplication` data class
  - [x] `SellerApplicationStatus` enum
  - [x] `toMap()` extension function

### Repository
- [x] `SellerApplicationRepository.kt` - Created
  - [x] `createSellerApplication()` - Instant creation
  - [x] `getSellerApplicationByUserId()` - Fetch status
  - [x] `listenToSellerApplicationStatus()` - Real-time listener
  - [x] `getApplicationStatus()` - Get status string

### ViewModel
- [x] `SellerApplicationViewModel.kt` - Created
  - [x] `createSellerApplication()` - Create app
  - [x] `fetchApplicationStatus()` - Fetch status
  - [x] `listenToApplicationStatus()` - Listen for changes
  - [x] State flows for status, loading, errors

---

## 📝 Files to Update

### 1. User Model (User.kt)
```kotlin
// ADD these fields:
@PropertyName("seller_application_id")
val sellerApplicationId: String = "",

@PropertyName("seller_application_status")
val sellerApplicationStatus: String = "none",

@PropertyName("seller_application_created_at")
val sellerApplicationCreatedAt: Long = 0L
```

**Status:** ⏳ TODO

---

### 2. SellerVerificationScreen.kt
```kotlin
// CHANGES NEEDED:

// 1. Add ViewModel
val sellerApplicationViewModel: SellerApplicationViewModel = viewModel()

// 2. Update submitVerification() function:
fun submitVerification() {
    viewModelScope.launch {
        try {
            // ✅ 1. IMMEDIATELY show loading screen
            _uiState.value = VerificationState.Submitting
            
            // ✅ 2. Upload photo to Cloudinary
            val photoUrl = uploadPhotoToCloudinary(capturedImageUri)
            
            // ✅ 3. Create seller application (instant)
            sellerApplicationViewModel.createSellerApplication(
                userId = currentUser.id,
                userName = currentUser.name,
                userEmail = currentUser.email,
                verificationPhotoUrl = photoUrl
            )
            
            // ✅ 4. Navigate to review screen (don't wait for backend)
            onNavigateToReviewScreen()
            
        } catch (e: Exception) {
            _uiState.value = VerificationState.Error(e.message ?: "Failed to submit")
        }
    }
}

// 3. Add navigation to SellerApplicationUnderReviewScreen
```

**Status:** ⏳ TODO

---

### 3. Create SellerApplicationUnderReviewScreen.kt
```kotlin
// NEW FILE - Copy from SELLER_VERIFICATION_INSTANT_RESPONSE_IMPLEMENTATION.md
// Section: "Step 5: Create SellerApplicationUnderReviewScreen"

// Key components:
// - Loading spinner
// - "Seller Application Under Review" title
// - Estimated review time (24-48 hours)
// - "What happens next" steps
// - Real-time listener for status updates
```

**Status:** ⏳ TODO

---

### 4. ProfileScreen.kt
```kotlin
// CHANGES NEEDED:

// 1. Add ViewModel
val sellerApplicationViewModel: SellerApplicationViewModel = viewModel()

// 2. Collect application status
val applicationStatus by sellerApplicationViewModel.applicationStatus.collectAsState()

// 3. Add badges section
@Composable
fun ProfileBadgesSection(
    userRole: UserRole,
    applicationStatus: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Always show Buyer badge
        Badge(
            text = "Buyer",
            backgroundColor = Color(0xFFE3F2FD),
            textColor = Color(0xFF1976D2)
        )

        // Show Seller badge if approved
        if (userRole == UserRole.SELLER) {
            Badge(
                text = "Seller",
                backgroundColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF2E7D32)
            )
        }

        // Show Pending Seller badge if under review
        if (applicationStatus == "pending") {
            Badge(
                text = "Pending Seller",
                backgroundColor = Color(0xFFFFF8E1),
                textColor = Color(0xFFF57F17)
            )
        }
    }
}

// 4. Add Check Verification Status button
@Composable
fun CheckVerificationStatusButton(
    applicationStatus: String,
    onCheckStatus: () -> Unit
) {
    if (applicationStatus == "pending") {
        Button(
            onClick = onCheckStatus,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Check Verification Status")
        }
    }
}

// 5. Add click handler
LaunchedEffect(Unit) {
    sellerApplicationViewModel.fetchApplicationStatus(currentUser.id)
}

// 6. Add button click
onCheckStatusClick = {
    navController.navigate("seller_application_under_review")
}
```

**Status:** ⏳ TODO

---

### 5. Navigation (NavGraph.kt)
```kotlin
// ADD route:
composable("seller_application_under_review") {
    SellerApplicationUnderReviewScreen(
        currentUserId = currentUser.id,
        onBackClick = { navController.popBackStack() }
    )
}
```

**Status:** ⏳ TODO

---

### 6. Firestore Rules (firestore.rules)
```firestore
// ADD these rules:

match /seller_applications/{applicationId} {
  allow read: if request.auth.uid == resource.data.user_id || isAdmin();
  allow create: if request.auth.uid == request.resource.data.user_id;
  allow update: if isAdmin();
  allow delete: if isAdmin();
}

match /users/{userId} {
  allow update: if request.auth.uid == userId
    && request.resource.data.diff(resource.data).affectedKeys()
      .hasOnly(['seller_application_id', 'seller_application_status', 'seller_application_created_at']);
}
```

**Status:** ⏳ TODO

---

## 🧪 Testing Checklist

### Unit Tests
- [ ] `SellerApplicationRepository.createSellerApplication()` creates document
- [ ] `SellerApplicationRepository.getSellerApplicationByUserId()` fetches correctly
- [ ] `SellerApplicationViewModel.createSellerApplication()` updates state
- [ ] `SellerApplicationViewModel.listenToApplicationStatus()` listens correctly

### Integration Tests
- [ ] User submits verification → Application created in Firestore
- [ ] User document updated with application ID
- [ ] Status listener receives updates
- [ ] Real-time updates work without refresh

### UI Tests
- [ ] Submit button shows loading immediately
- [ ] Navigate to review screen
- [ ] Review screen displays correctly
- [ ] Badges update based on status
- [ ] Check status button works
- [ ] Status updates in real-time

### End-to-End Tests
- [ ] Complete flow: Submit → Review → Check Status → Approve → Update
- [ ] Rejection flow: Submit → Review → Check Status → Reject
- [ ] Multiple users: Each has separate application
- [ ] Admin approval: User sees update instantly

---

## 📊 Implementation Progress

```
Phase 1: Data Layer
├─ [x] SellerApplicationModels.kt
├─ [x] SellerApplicationRepository.kt
└─ [x] SellerApplicationViewModel.kt

Phase 2: UI Layer
├─ [ ] SellerApplicationUnderReviewScreen.kt
├─ [ ] Update SellerVerificationScreen.kt
├─ [ ] Update ProfileScreen.kt
└─ [ ] Update NavGraph.kt

Phase 3: Data Model Updates
├─ [ ] Update User.kt
└─ [ ] Update Firestore rules

Phase 4: Testing
├─ [ ] Unit tests
├─ [ ] Integration tests
├─ [ ] UI tests
└─ [ ] End-to-end tests

Phase 5: Deployment
├─ [ ] Code review
├─ [ ] Merge to main
├─ [ ] Deploy to Firebase
└─ [ ] Monitor logs
```

---

## 🎯 Key Implementation Points

### Instant Response
```kotlin
// ✅ DO THIS:
1. Show loading immediately
2. Call createSellerApplication()
3. Navigate to review screen
4. Backend processes in background

// ❌ DON'T DO THIS:
1. Wait for backend
2. Then show loading
3. Then navigate
```

### Real-Time Updates
```kotlin
// ✅ DO THIS:
1. Use Firestore listener
2. Emit updates via Flow
3. UI collects and updates
4. No manual refresh needed

// ❌ DON'T DO THIS:
1. Fetch status once
2. Require manual refresh
3. Poll backend repeatedly
```

### Dual Badges
```kotlin
// ✅ DO THIS:
- Always show "Buyer" badge
- Show "Pending Seller" if status == "pending"
- Show "Seller" if role == SELLER
- Update automatically

// ❌ DON'T DO THIS:
- Hide "Buyer" badge
- Show only one badge
- Require manual refresh
```

---

## 📋 Code Review Checklist

- [ ] All files compile without errors
- [ ] No unused imports
- [ ] Proper error handling
- [ ] Logging in place
- [ ] Comments where needed
- [ ] Follows code style
- [ ] No hardcoded strings
- [ ] Proper null safety
- [ ] Firestore rules updated
- [ ] Navigation routes added

---

## 🚀 Deployment Steps

1. **Code Review**
   - [ ] Review all changes
   - [ ] Check for issues
   - [ ] Approve PR

2. **Testing**
   - [ ] Run unit tests
   - [ ] Run integration tests
   - [ ] Manual testing
   - [ ] QA approval

3. **Deployment**
   - [ ] Merge to main
   - [ ] Deploy to Firebase
   - [ ] Update Firestore rules
   - [ ] Monitor logs

4. **Post-Deployment**
   - [ ] Monitor error logs
   - [ ] Check Firestore usage
   - [ ] Verify real-time updates
   - [ ] User feedback

---

## 📞 Support & Troubleshooting

### Issue: Loading screen doesn't appear immediately
**Solution:** Ensure `_uiState.value = VerificationState.Submitting` is called BEFORE any async operations

### Issue: Status doesn't update in real-time
**Solution:** Verify Firestore listener is active and Flow is collecting

### Issue: Badges don't update
**Solution:** Ensure `applicationStatus` StateFlow is being collected in ProfileScreen

### Issue: Navigation doesn't work
**Solution:** Verify route is added to NavGraph and screen is composable

---

## 📚 Documentation Files

- [x] `SELLER_VERIFICATION_INSTANT_RESPONSE_IMPLEMENTATION.md` - Complete guide
- [x] `SELLER_VERIFICATION_QUICK_REFERENCE.md` - Quick reference
- [x] `SELLER_VERIFICATION_VISUAL_GUIDE.txt` - Visual diagrams
- [x] `SELLER_VERIFICATION_IMPLEMENTATION_CHECKLIST.md` - This file

---

## ✅ Summary

**Completed:**
- ✅ Data models created
- ✅ Repository with real-time listener
- ✅ ViewModel with state management
- ✅ All code compiles without errors

**To Do:**
- 📝 Update User model
- 📝 Update SellerVerificationScreen
- 📝 Create SellerApplicationUnderReviewScreen
- 📝 Update ProfileScreen
- 📝 Update NavGraph
- 📝 Update Firestore rules
- 🧪 Testing
- 🚀 Deployment

**Naming Decision:**
- ✅ "Seller Application Under Review" (recommended)

**Features:**
- ✅ Instant button response
- ✅ Live status checking
- ✅ Dual badges until approval
- ✅ Real-time updates
