# Co-Seller Invitation System - Implementation Details

## Overview
The co-seller invitation system allows sellers to invite other sellers to join their co-seller stores. The system is already implemented in the mobile app with the following components.

---

## Data Models

### 1. StoreInvitation Model
**Location**: `app/src/main/java/com/gcuf/craftoria/data/model/CoSellerStore.kt`

```kotlin
data class StoreInvitation(
    val id: String = "",
    
    @PropertyName("store_id")
    var storeId: String = "",
    
    @PropertyName("store_name")
    var storeName: String = "",
    
    @PropertyName("inviter_id")
    var inviterId: String = "",
    
    @PropertyName("inviter_name")
    var inviterName: String = "",
    
    @PropertyName("invitee_email")
    var inviteeEmail: String = "",
    
    @PropertyName("invitee_id")
    var inviteeId: String = "",
    
    @PropertyName("invitee_name")
    var inviteeName: String = "",
    
    @PropertyName("status")
    var status: InvitationStatus = InvitationStatus.PENDING,
    
    @PropertyName("sent_at")
    var sentAt: Long = System.currentTimeMillis(),
    
    @PropertyName("responded_at")
    var respondedAt: Long = 0L,
    
    @PropertyName("is_registered_user")
    var isRegisteredUser: Boolean = false
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
```

### 2. StoreMember Model
**Location**: `app/src/main/java/com/gcuf/craftoria/data/model/CoSellerStore.kt`

```kotlin
data class StoreMember(
    val id: String = "",
    
    @PropertyName("user_id")
    var userId: String = "",
    
    @PropertyName("user_name")
    var userName: String = "",
    
    @PropertyName("user_email")
    var userEmail: String = "",
    
    @PropertyName("user_avatar")
    var userAvatar: String = "",
    
    @PropertyName("store_id")
    var storeId: String = "",
    
    @PropertyName("is_owner")
    var isOwner: Boolean = false,
    
    @PropertyName("joined_at")
    var joinedAt: Long = System.currentTimeMillis()
)
```

### 3. CoSellerStore Model
**Location**: `app/src/main/java/com/gcuf/craftoria/data/model/CoSellerStore.kt`

```kotlin
data class CoSellerStore(
    val id: String = "",
    
    @PropertyName("store_name")
    var storeName: String = "",
    
    @PropertyName("owner_id")
    var ownerId: String = "",
    
    @PropertyName("member_ids")
    var memberIds: List<String> = emptyList(),
    
    @PropertyName("member_count")
    var memberCount: Int = 0,
    
    // ... other fields
)
```

---

## Repository Methods

### CoSellerStoreRepository
**Location**: `app/src/main/java/com/gcuf/craftoria/data/repository/CoSellerStoreRepository.kt`

#### 1. Send Invitation
```kotlin
suspend fun sendInvitation(invitation: StoreInvitation): Result<String> {
    // Checks if user exists
    // Creates invitation document in Firestore
    // Sends notification to invitee
    // Returns invitation ID
}
```

**Usage**:
```kotlin
val invitation = StoreInvitation(
    storeId = store.id,
    storeName = store.storeName,
    inviterId = currentUserId,
    inviterName = currentUserName,
    inviteeEmail = "seller@example.com"
)

val result = repository.sendInvitation(invitation)
```

#### 2. Accept Invitation
```kotlin
suspend fun acceptInvitation(
    invitationId: String,
    userId: String,
    userName: String
): Result<Unit> {
    // Updates invitation status to ACCEPTED
    // Adds user to store members
    // Updates store member count
    // Sends notification to inviter
}
```

**Usage**:
```kotlin
val result = repository.acceptInvitation(
    invitationId = "inv_123",
    userId = "seller_456",
    userName = "Seller Name"
)
```

#### 3. Decline Invitation
```kotlin
suspend fun declineInvitation(invitationId: String): Result<Unit> {
    // Updates invitation status to DECLINED
    // Sends notification to inviter
}
```

**Usage**:
```kotlin
val result = repository.declineInvitation(invitationId = "inv_123")
```

#### 4. Get User Invitations
```kotlin
suspend fun getUserInvitations(userId: String): Result<List<StoreInvitation>> {
    // Fetches all pending invitations for user
    // Returns list of StoreInvitation objects
}
```

---

## ViewModel Methods

### CoSellerStoreViewModel
**Location**: `app/src/main/java/com/gcuf/craftoria/viewmodel/CoSellerStoreViewModel.kt`

#### 1. Send Invitation
```kotlin
fun sendInvitation(invitation: StoreInvitation) {
    viewModelScope.launch {
        try {
            val result = storeRepository.sendInvitation(invitation)
            
            if (result.isSuccess) {
                _uiState.value = CoSellerUiState.ActionSuccess(
                    "Invitation sent to ${invitation.inviteeEmail}"
                )
            } else {
                _uiState.value = CoSellerUiState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to send invitation"
                )
            }
        } catch (e: Exception) {
            _uiState.value = CoSellerUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

#### 2. Accept Invitation
```kotlin
fun acceptInvitation(invitationId: String, userId: String, userName: String) {
    viewModelScope.launch {
        acceptInvitationAsync(invitationId, userId, userName)
    }
}

suspend fun acceptInvitationAsync(
    invitationId: String,
    userId: String,
    userName: String
): Result<Unit> {
    return try {
        val result = storeRepository.acceptInvitation(invitationId, userId, userName)
        
        if (result.isSuccess) {
            _uiState.value = CoSellerUiState.ActionSuccess("Invitation accepted!")
            loadUserStores(userId)  // Refresh stores list
        } else {
            _uiState.value = CoSellerUiState.Error(
                result.exceptionOrNull()?.message ?: "Failed to accept invitation"
            )
        }
        
        result
    } catch (e: Exception) {
        _uiState.value = CoSellerUiState.Error(e.message ?: "Unknown error")
        Result.failure(e)
    }
}
```

---

## UI Screens

### 1. Manage Members Screen
**Location**: `app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/ManageCoSellerStoreScreen.kt`

**Features**:
- Display active members
- Display pending invitations
- Send new invitations
- Remove members
- Resend invitations
- Cancel pending invitations

**Key Composables**:
```kotlin
@Composable
fun ManageMembersSection(
    store: CoSellerStore,
    members: List<StoreMember>,
    pendingInvitations: List<StoreInvitation>,
    currentUserId: String,
    onRemoveMember: (StoreMember) -> Unit,
    onSendInvitation: (String) -> Unit
)
```

### 2. Invitation Dialog
**Location**: `app/src/main/java/com/gcuf/craftoria/ui/screens/coseller/ManageCoSellerStoreScreen.kt`

**Features**:
- Email input field
- Email validation
- Send button
- Cancel button

**Code**:
```kotlin
var inviteEmail by remember { mutableStateOf("") }

// In UI:
TextField(
    value = inviteEmail,
    onValueChange = { inviteEmail = it },
    label = { Text("Enter seller email") },
    placeholder = { Text("seller@example.com") }
)

Button(
    onClick = {
        if (inviteEmail.isNotEmpty() && inviteEmail.contains("@")) {
            onSendInvitation(inviteEmail)
            inviteEmail = ""
        }
    }
) {
    Text("Send Invitation")
}
```

### 3. Invitations Received Screen
**Location**: `app/src/main/java/com/gcuf/craftoria/ui/navigation/NavGraph.kt`

**Features**:
- Display pending invitations
- Display accepted stores
- Accept/Decline buttons
- Store details preview

---

## Firebase Collections

### 1. store_invitations Collection
```
store_invitations/
├── invitation_123/
│   ├── store_id: "store_456"
│   ├── store_name: "Premium Crafts"
│   ├── inviter_id: "seller_a_id"
│   ├── inviter_name: "Seller A"
│   ├── invitee_email: "seller_b@example.com"
│   ├── invitee_id: "seller_b_id"
│   ├── invitee_name: "Seller B"
│   ├── status: "PENDING" | "ACCEPTED" | "DECLINED"
│   ├── sent_at: timestamp
│   ├── responded_at: timestamp
│   └── is_registered_user: boolean
│
└── invitation_124/
    └── ...
```

### 2. store_members Collection
```
store_members/
├── member_789/
│   ├── user_id: "seller_b_id"
│   ├── user_name: "Seller B"
│   ├── user_email: "seller_b@example.com"
│   ├── user_avatar: "url"
│   ├── store_id: "store_456"
│   ├── is_owner: false
│   └── joined_at: timestamp
│
└── member_790/
    └── ...
```

### 3. co_seller_stores Collection
```
co_seller_stores/
├── store_456/
│   ├── store_name: "Premium Crafts"
│   ├── owner_id: "seller_a_id"
│   ├── member_ids: ["seller_a_id", "seller_b_id", "seller_c_id"]
│   ├── member_count: 3
│   ├── product_count: 45
│   ├── average_rating: 4.8
│   ├── is_active: true
│   ├── created_at: timestamp
│   └── updated_at: timestamp
│
└── store_457/
    └── ...
```

---

## Firestore Rules

```javascript
// Store Invitations
match /store_invitations/{document=**} {
  allow read: if request.auth.uid in resource.data.inviter_id || 
                 request.auth.uid in resource.data.invitee_id;
  allow create: if request.auth.uid == request.resource.data.inviter_id;
  allow update: if request.auth.uid == resource.data.inviter_id ||
                   request.auth.uid == resource.data.invitee_id;
  allow delete: if request.auth.uid == resource.data.inviter_id;
}

// Store Members
match /store_members/{document=**} {
  allow read: if request.auth.uid == resource.data.user_id;
  allow create: if request.auth.uid == request.resource.data.user_id;
  allow update: if request.auth.uid == resource.data.user_id;
  allow delete: if request.auth.uid == resource.data.user_id;
}

// Co-Seller Stores
match /co_seller_stores/{document=**} {
  allow read: if request.auth.uid in resource.data.member_ids ||
                 request.auth.uid == resource.data.owner_id;
  allow update: if request.auth.uid == resource.data.owner_id;
}
```

---

## Notification System

### Invitation Sent Notification
**Type**: `CO_SELLER_INVITATION`

```kotlin
NotificationHelper.notifyCoSellerInvitation(
    inviteeId = "seller_b_id",
    storeId = "store_456",
    storeName = "Premium Crafts",
    inviterName = "Seller A",
    memberCount = 3
)
```

**Notification Content**:
- Title: "You're invited to join a co-seller store!"
- Message: "Seller A invited you to join 'Premium Crafts'"
- Action: Navigate to invitations screen

### Invitation Accepted Notification
**Type**: `INVITATION_ACCEPTED`

```kotlin
// Sent to inviter when invitation is accepted
NotificationHelper.notifyInvitationAccepted(
    inviterId = "