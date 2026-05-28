# Chat System: Quick Reference Guide

## Problem Summary

### Issue 1: Buyer-to-Seller Conversion
When a buyer converts to a seller, their existing chats with other sellers become ambiguous:
- Should they see it in "Buyer Chats" or "Seller Chats"?
- Can they view the other seller's profile?
- Is this now a seller-to-seller chat?

### Issue 2: Read Receipts Not Updating Instantly
Messages show no indication of delivery/read status, or updates are delayed:
- No ✓ (delivered) tick
- No ✓✓ (read) tick
- Updates don't appear in real-time

---

## Solution Overview

### Part 1: Role Conversion Handling

**What Changed:**
- Chats now store `participant_roles` (who is buyer/seller)
- Chats now have `chat_type` (buyer_seller, seller_seller, buyer_buyer)
- Roles are synced every time chat is opened

**How It Works:**
```
User opens chat
    ↓
syncParticipantRoles() called
    ↓
Fetch current roles from Firestore
    ↓
Determine new chat_type
    ↓
Update chat document
    ↓
UI adapts profile viewing rules
```

**Result:**
- Buyer converts to seller → chat type updates to seller_seller
- Profile viewing rules automatically adapt
- No manual intervention needed

---

### Part 2: Real-Time Read Receipts

**What Changed:**
- Added `startContinuousReadReceiptUpdates()` listener
- Automatically marks messages as delivered
- Automatically marks messages as read
- Updates happen in real-time

**How It Works:**
```
Chat opens
    ↓
startContinuousReadReceiptUpdates() starts listening
    ↓
Listener detects undelivered messages
    ↓
Updates: deliveredAt = now
    ↓
UI shows ✓ (gray)
    ↓
Listener detects unread messages
    ↓
Updates: isRead = true, readAt = now
    ↓
UI shows ✓✓ (blue)
```

**Result:**
- Messages show ✓ immediately when chat opens
- Messages show ✓✓ automatically when read
- Both participants see updates in real-time

---

## Key Code Changes

### 1. Chat Model
```kotlin
// NEW FIELDS
participant_roles: Map<String, String>  // userId -> "BUYER" or "SELLER"
chat_type: String  // "buyer_seller", "seller_seller", or "buyer_buyer"
last_role_update: Long  // When roles were last synced
```

### 2. ChatRepository
```kotlin
// NEW FUNCTIONS
fetchParticipantRoles()  // Get roles from Firestore
syncParticipantRoles()  // Update roles in chat
determineChatType()  // Calculate chat type from roles
startContinuousReadReceiptUpdates()  // Real-time listener
```

### 3. ChatViewModel
```kotlin
// NEW STATE FLOWS
chatType: StateFlow<String>
otherUserRole: StateFlow<UserRole>

// NEW FUNCTION
startReadReceiptUpdates()  // Start the listener
```

### 4. ChatScreen
```kotlin
// NEW LOGIC
canViewProfile = when (chatType) {
    "seller_seller" -> true
    "buyer_seller" -> !isCurrentUserSeller
    "buyer_buyer" -> true
    else -> false
}

// UPDATED DISPLAY
✓ = delivered (gray)
✓✓ = read (blue)
```

---

## Testing Scenarios

### Scenario 1: Buyer Converts to Seller
```
1. Create buyer account
2. Chat with seller
3. Convert to seller (in settings)
4. Open the chat again
5. Verify: chat_type changed to seller_seller
6. Verify: can now view seller's profile
```

### Scenario 2: Real-Time Read Receipts
```
1. User A sends message
2. Verify: no tick appears
3. User B opens chat
4. Verify: ✓ appears (delivered)
5. Wait a moment
6. Verify: ✓✓ appears (read)
7. User A sees ✓✓ in real-time
```

### Scenario 3: Seller-to-Seller Chat
```
1. Create two seller accounts
2. Start chat between them
3. Verify: chat_type is seller_seller
4. Verify: both can view each other's profiles
5. Verify: read receipts work
```

---

## Firestore Structure

### Chat Document
```json
{
  "participant_ids": ["user1", "user2"],
  "participant_roles": {
    "user1": "BUYER",
    "user2": "SELLER"
  },
  "chat_type": "buyer_seller",
  "last_role_update": 1234567890,
  "participant_names": {...},
  "participant_avatars": {...},
  "last_message": "...",
  "unread_count": {...}
}
```

### Message Document
```json
{
  "chat_id": "chat123",
  "sender_id": "user1",
  "content": "Hello",
  "is_read": true,
  "read_at": 1234567890,
  "delivered_at": 1234567880,
  "created_at": 1234567870
}
```

---

## Logs to Watch For

### Role Sync
```
✅ Synced 2 participant roles for chat chat123 (type: seller_seller)
👤 Fetched role for userId: SELLER
```

### Read Receipts
```
🔄 Starting continuous read receipt updates for chat: chat123
✅ Marked 3 messages as delivered (continuous)
✅ Marked 3 messages as read (continuous)
```

### Errors
```
❌ Failed to sync participant roles for chat chat123
❌ Read receipt listener error: [error message]
```

---

## Performance Impact

### Firestore Reads
- **Per Chat Open:** 2 reads (fetch roles, sync avatars)
- **Per Message:** 0 reads (listener only)
- **Continuous:** ~1 read per 5 seconds (listener polling)

### Firestore Writes
- **Per Chat Open:** 1 write (sync roles)
- **Per Message Delivery:** 1 write (mark delivered)
- **Per Message Read:** 1 write (mark read)

### Optimization
- Batch operations used where possible
- Listener stops when chat closes
- Role sync only on chat open
- No unnecessary Firestore calls

---

## Troubleshooting

### Issue: Chat type not updating after role conversion
**Solution:**
- Close and reopen chat
- Check Firestore rules allow updates
- Verify user role was actually updated in users collection

### Issue: Read receipts not appearing
**Solution:**
- Verify `deliveredAt` field exists in messages
- Check Firestore rules allow message updates
- Verify listener is running (check logs)
- Try closing and reopening chat

### Issue: Profile viewing not working
**Solution:**
- Verify chat_type is correct in Firestore
- Check canViewProfile logic in ChatScreen
- Verify user roles are correct in users collection

---

## Files to Review

1. **Chat.kt** - Data model with new fields
2. **ChatRepository.kt** - Role handling and continuous listener
3. **ChatViewModel.kt** - State management
4. **ChatScreen.kt** - UI logic and display

---

## Deployment Checklist

- [ ] All code changes merged
- [ ] Firestore rules updated
- [ ] Test role conversion scenario
- [ ] Test read receipts scenario
- [ ] Monitor logs for errors
- [ ] Check Firestore usage
- [ ] User acceptance testing
- [ ] Deploy to production

---

## Summary

✅ **Buyer-to-Seller Conversion:** Handled automatically with role tracking
✅ **Real-Time Read Receipts:** Continuous listener marks messages instantly
✅ **Profile Viewing:** Adapts based on chat type
✅ **Performance:** Optimized with batch operations and efficient listeners
