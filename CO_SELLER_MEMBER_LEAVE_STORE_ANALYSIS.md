# Co-Seller Store Member Leave Functionality - Analysis & Recommendation

## Current Status: ❌ NOT IMPLEMENTED

There is **NO functionality** for co-seller store members to leave the store voluntarily. Currently, only the store owner can remove members.

---

## Current Functionality

### What EXISTS:
- ✅ Store owner can remove members via `removeMember()` function
- ✅ Members can accept/decline store invitations
- ✅ Members can view store details and products
- ✅ Members can manage products (if permissions allow)
- ✅ Members receive real-time member count updates

### What's MISSING:
- ❌ Members cannot leave the store themselves
- ❌ No "Leave Store" button in UI
- ❌ No confirmation dialog for leaving
- ❌ No notification to store owner when member leaves
- ❌ No option to rejoin after leaving

---

## Current Architecture

### Files Involved:
1. **CoSellerStoreRepository.kt** - Data layer
   - `removeMember(storeId, userId)` - Owner removes member
   - `acceptInvitation()` - Member accepts invitation
   - `declineInvitation()` - Member declines invitation

2. **CoSellerStoreViewModel.kt** - Business logic
   - `removeMember()` - Calls repository

3. **ManageCoSellerStoreScreen.kt** - UI
   - Shows members list
   - Owner can remove members
   - No leave option for members

4. **CoSellerMemberCountManager.kt** - Member count management
   - Updates member counts retroactively
   - Updates notifications

---

## Recommended Implementation

### Step 1: Add Repository Method

**File:** `CoSellerStoreRepository.kt`

```kotlin
// Member leaves store
suspend fun leaveStore(storeId: String, userId: String): Result<Unit> {
    return try {
        Log.d(TAG, "Member $userId leaving store: $storeId")
        
        // Remove from members collection
        val members = membersCollection
            .whereEqualTo("store_id", storeId)
            .whereEqualTo("user_id", userId)
            .get()
            .await()

        members.documents.forEach { it.reference.delete().await() }

        // Update store member_ids
        val store = getStoreById(storeId).getOrNull()
        store?.let {
            val updatedMemberIds = it.memberIds.filter { id -> id != userId }
            storesCollection.document(storeId).update(
                mapOf(
                    "member_ids" to updatedMemberIds,
                    "member_count" to updatedMemberIds.size,
                    "updated_at" to System.currentTimeMillis()
                )
            ).await()
            
            // Update all existing notifications for this store
            com.gcuf.craftoria.utils.CoSellerMemberCountManager.updateAllStoreNotifications(storeId)
        }

        Log.d(TAG, "Member left store successfully")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to leave store", e)
        Result.failure(e)
    }
}
```

### Step 2: Add ViewModel Method

**File:** `CoSellerStoreViewModel.kt`

```kotlin
fun leaveStore(storeId: String, userId: String) {
    viewModelScope.launch {
        try {
            val result = storeRepository.leaveStore(storeId, userId)

            if (result.isSuccess) {
                _uiState.value = CoSellerStoreState.ActionSuccess("You have left the store")
                // Navigate back to stores list
            } else {
                _uiState.value = CoSellerStoreState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to leave store"
                )
            }
        } catch (e: Exception) {
            _uiState.value = CoSellerStoreState.Error(e.message ?: "Unknown error")
        }
    }
}
```

### Step 3: Add UI Components

**File:** `ManageCoSellerStoreScreen.kt`

Add "Leave Store" button in the header or members section:

```kotlin
// In the top bar or action menu
if (user.id != currentStore?.ownerId) {
    IconButton(onClick = { showLeaveStoreDialog = true }) {
        Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = "Leave Store",
            tint = Error
        )
    }
}

// Leave Store Confirmation Dialog
if (showLeaveStoreDialog) {
    AlertDialog(
        onDismissRequest = { showLeaveStoreDialog = false },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Error.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Leave Store",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Text(
                "Are you sure you want to leave ${currentStore?.storeName}? You can rejoin if invited again.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    coSellerStoreViewModel.leaveStore(storeId, user.id)
                    showLeaveStoreDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Leave", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { showLeaveStoreDialog = false },
                border = BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        }
    )
}
```

### Step 4: Add Notification (Optional)

Send notification to store owner when member leaves:

```kotlin
// In leaveStore() method after successful removal
com.gcuf.craftoria.utils.NotificationHelper.notifyMemberLeft(
    ownerId = store.ownerId,
    storeId = storeId,
    storeName = store.storeName,
    memberName = userName,
    memberCount = updatedMemberIds.size
)
```

---

## User Experience Flow

### Current (Owner Removes Member):
```
Owner views Members tab
    ↓
Clicks remove icon on member
    ↓
Confirmation dialog
    ↓
Member removed
    ↓
Member count updated
```

### Proposed (Member Leaves):
```
Member views Store details
    ↓
Clicks "Leave Store" button
    ↓
Confirmation dialog: "Are you sure you want to leave?"
    ↓
Member confirms
    ↓
Member removed from store
    ↓
Member count updated
    ↓
Member redirected to stores list
    ↓
Owner receives notification (optional)
```

---

## Database Changes

### No new collections needed
- Uses existing `store_members` collection
- Uses existing `co_seller_stores` collection
- Updates existing `member_ids` array
- Updates existing `member_count` field

### Firestore Operations:
1. Delete member document from `store_members`
2. Update `co_seller_stores.member_ids` array
3. Update `co_seller_stores.member_count`
4. Update all notifications for the store

---

## Security Considerations

### Permissions:
- ✅ Only the member themselves can leave
- ✅ Owner cannot be removed by themselves (only by admin)
- ✅ Cannot leave if it's the only member (owner)

### Validation:
```kotlin
// Prevent owner from leaving
if (userId == store.ownerId) {
    return Result.failure(Exception("Store owner cannot leave"))
}

// Prevent leaving if only member
if (store.memberIds.size <= 1) {
    return Result.failure(Exception("Cannot leave - you are the only member"))
}
```

---

## Notification System Integration

### Optional: Notify Owner
When a member leaves, send notification to store owner:

```kotlin
// New notification type
fun notifyMemberLeft(
    ownerId: String,
    storeId: String,
    storeName: String,
    memberName: String,
    memberCount: Int
)
```

---

## Testing Checklist

- [ ] Member can see "Leave Store" button
- [ ] Confirmation dialog appears
- [ ] Member can cancel leaving
- [ ] Member can confirm leaving
- [ ] Member is removed from store
- [ ] Member count updates correctly
- [ ] Member no longer sees store in their stores list
- [ ] Owner sees updated member count
- [ ] Owner receives notification (if implemented)
- [ ] Member can rejoin if invited again
- [ ] Owner cannot leave store

---

## Estimated Implementation Time

- **Repository method:** 15 minutes
- **ViewModel method:** 10 minutes
- **UI components:** 20 minutes
- **Testing:** 15 minutes
- **Total:** ~1 hour

---

## Priority: MEDIUM

This feature improves user experience by giving members control over their store membership, but it's not critical for core functionality since owners can already remove members.

---

## Related Features

- ✅ Accept/Decline Invitations
- ✅ Remove Members (Owner only)
- ✅ Member Count Management
- ✅ Real-time Notifications
- ❌ Leave Store (MISSING)
- ❌ Rejoin Store (MISSING)
