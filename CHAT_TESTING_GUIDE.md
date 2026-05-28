# 🧪 Chat System Testing Guide

## Test Environment Setup

### Prerequisites
- 2+ Android devices or emulators
- Firebase project configured
- Test user accounts created
- Firestore rules deployed

### Test Users
Create these test accounts:
1. **Buyer Test Account**
   - Email: buyer.test@craftoria.com
   - Role: buyer

2. **Seller Test Account**
   - Email: seller.test@craftoria.com
   - Role: seller

3. **Admin Test Account**
   - Email: admin.test@craftoria.com
   - Role: admin

## 📱 Functional Testing

### 1. Chat Creation
- [ ] **Test 1.1**: Buyer initiates chat with seller from product page
  - Expected: New chat created successfully
  - Expected: Chat appears in both users' chat lists

- [ ] **Test 1.2**: Seller initiates chat with buyer
  - Expected: New chat created successfully
  
- [ ] **Test 1.3**: Try to create duplicate chat
  - Expected: Existing chat is opened, no duplicate created

### 2. Text Messaging
- [ ] **Test 2.1**: Send simple text message
  - Expected: Message appears immediately for sender
  - Expected: Message appears in real-time for receiver
  
- [ ] **Test 2.2**: Send long message (500+ characters)
  - Expected: Message displays correctly with proper wrapping
  
- [ ] **Test 2.3**: Send message with emojis
  - Expected: Emojis display correctly
  
- [ ] **Test 2.4**: Send message with special characters
  - Expected: Special characters display correctly
  
- [ ] **Test 2.5**: Send multiple messages rapidly
  - Expected: All messages delivered in correct order
  - Expected: No messages lost

### 3. Image Sharing
- [ ] **Test 3.1**: Share image from gallery
  - Expected: Image uploads successfully
  - Expected: Thumbnail displays in chat
  - Expected: Full image opens on tap
  
- [ ] **Test 3.2**: Share large image (>5MB)
  - Expected: Image compresses automatically
  - Expected: Upload completes successfully
  
- [ ] **Test 3.3**: Share multiple images
  - Expected: All images upload successfully
  - Expected: Images display in correct order

### 4. Product Sharing
- [ ] **Test 4.1**: Share product in chat
  - Expected: Product card displays with image, name, price
  - Expected: Tapping product opens product details
  
- [ ] **Test 4.2**: Share out-of-stock product
  - Expected: Product shares with "Out of Stock" indicator

### 5. Order Updates
- [ ] **Test 5.1**: Send order status update
  - Expected: Order update message displays correctly
  - Expected: Tapping opens order details
  
- [ ] **Test 5.2**: Multiple order updates
  - Expected: All updates display in chronological order

### 6. Negotiation System
- [ ] **Test 6.1**: Send negotiation request
  - Expected: Negotiation card displays with offered price
  - Expected: Accept/Reject buttons visible to seller
  
- [ ] **Test 6.2**: Accept negotiation
  - Expected: Status updates to "Accepted"
  - Expected: Buyer receives notification
  
- [ ] **Test 6.3**: Reject negotiation
  - Expected: Status updates to "Rejected"
  - Expected: Buyer receives notification
  
- [ ] **Test 6.4**: Counter-offer
  - Expected: Counter-offer displays correctly
  - Expected: Buyer can accept/reject counter-offer

### 7. Read Receipts
- [ ] **Test 7.1**: Send message and check read status
  - Expected: Message shows as "Sent" initially
  - Expected: Message shows as "Read" when receiver opens chat
  
- [ ] **Test 7.2**: Unread count updates
  - Expected: Badge shows correct unread count
  - Expected: Count resets when chat is opened

### 8. Block/Unblock
- [ ] **Test 8.1**: Block user
  - Expected: User is blocked successfully
  - Expected: Blocked user cannot send messages
  - Expected: "User blocked" message displays
  
- [ ] **Test 8.2**: Unblock user
  - Expected: User is unblocked successfully
  - Expected: Chat functionality restored
  
- [ ] **Test 8.3**: Blocked user tries to send message
  - Expected: Error message displays
  - Expected: Message not delivered

### 9. Message Deletion
- [ ] **Test 9.1**: Delete own message
  - Expected: Message deleted for everyone
  - Expected: "Message deleted" placeholder shows
  
- [ ] **Test 9.2**: Try to delete other user's message
  - Expected: Delete option not available
  
- [ ] **Test 9.3**: Delete message for me only
  - Expected: Message removed from my view
  - Expected: Message still visible to other user

### 10. Chat Deletion
- [ ] **Test 10.1**: Delete chat
  - Expected: All messages deleted
  - Expected: Chat removed from list
  
- [ ] **Test 10.2**: Clear chat
  - Expected: All messages deleted
  - Expected: Chat remains in list with no messages

### 11. Typing Indicators
- [ ] **Test 11.1**: Start typing
  - Expected: "Typing..." indicator shows for other user
  
- [ ] **Test 11.2**: Stop typing
  - Expected: Indicator disappears after 3 seconds

### 12. Message Search
- [ ] **Test 12.1**: Search messages in chat
  - Expected: Matching messages highlighted
  - Expected: Search results accurate
  
- [ ] **Test 12.2**: Search with no results
  - Expected: "No results found" message displays

## 🔄 Real-time Testing

### 13. Real-time Sync
- [ ] **Test 13.1**: Send message from Device A
  - Expected: Message appears on Device B within 1 second
  
- [ ] **Test 13.2**: Multiple users typing simultaneously
  - Expected: All messages delivered correctly
  - Expected: No message loss or duplication
  
- [ ] **Test 13.3**: Network interruption
  - Expected: Messages queue when offline
  - Expected: Messages send when connection restored

## 📲 Push Notifications

### 14. Notification Testing
- [ ] **Test 14.1**: Receive message when app is closed
  - Expected: Push notification displays
  - Expected: Notification shows sender name and message
  
- [ ] **Test 14.2**: Tap notification
  - Expected: App opens to specific chat
  
- [ ] **Test 14.3**: Receive multiple messages
  - Expected: Notifications stack correctly
  
- [ ] **Test 14.4**: Muted chat
  - Expected: No notification for muted chats
  
- [ ] **Test 14.5**: Notification sound
  - Expected: Sound plays for new messages
  - Expected: Vibration works

## 🎨 UI/UX Testing

### 15. User Interface
- [ ] **Test 15.1**: Chat list displays correctly
  - Expected: Recent chats show at top
  - Expected: Last message preview visible
  - Expected: Timestamp displays correctly
  
- [ ] **Test 15.2**: Message bubbles
  - Expected: Own messages align right
  - Expected: Other messages align left
  - Expected: Colors distinguish sender/receiver
  
- [ ] **Test 15.3**: Scroll performance
  - Expected: Smooth scrolling with 100+ messages
  - Expected: No lag or stuttering
  
- [ ] **Test 15.4**: Image loading
  - Expected: Images load progressively
  - Expected: Placeholder shows while loading
  
- [ ] **Test 15.5**: Empty states
  - Expected: "No chats yet" message when list is empty
  - Expected: "No messages yet" in new chat

### 16. Responsive Design
- [ ] **Test 16.1**: Different screen sizes
  - Expected: UI adapts to small screens (5")
  - Expected: UI adapts to large screens (7"+)
  
- [ ] **Test 16.2**: Landscape orientation
  - Expected: Layout adjusts correctly
  - Expected: All features accessible
  
- [ ] **Test 16.3**: Keyboard behavior
  - Expected: Input field stays above keyboard
  - Expected: Messages scroll to show latest

## ⚡ Performance Testing

### 17. Load Testing
- [ ] **Test 17.1**: Chat with 100 messages
  - Expected: Loads within 2 seconds
  - Expected: Smooth scrolling
  
- [ ] **Test 17.2**: Chat with 500 messages
  - Expected: Pagination works correctly
  - Expected: Only recent messages loaded initially
  
- [ ] **Test 17.3**: Chat with 50 images
  - Expected: Images load on-demand
  - Expected: Memory usage stays reasonable
  
- [ ] **Test 17.4**: 10 active chats
  - Expected: Chat list loads quickly
  - Expected: Switching between chats is smooth

### 18. Network Testing
- [ ] **Test 18.1**: Slow network (2G)
  - Expected: Messages send eventually
  - Expected: Loading indicators show
  
- [ ] **Test 18.2**: No network
  - Expected: Error message displays
  - Expected: Messages queue for later
  
- [ ] **Test 18.3**: Network switches (WiFi to Mobile)
  - Expected: Connection maintains
  - Expected: No message loss

## 🔒 Security Testing

### 19. Access Control
- [ ] **Test 19.1**: Try to access other user's chat
  - Expected: Access denied
  - Expected: Error message displays
  
- [ ] **Test 19.2**: Unauthenticated user
  - Expected: Cannot access any chats
  - Expected: Redirected to login
  
- [ ] **Test 19.3**: Firestore rules
  - Expected: Rules prevent unauthorized reads
  - Expected: Rules prevent unauthorized writes

### 20. Data Validation
- [ ] **Test 20.1**: Send empty message
  - Expected: Send button disabled
  - Expected: Error message displays
  
- [ ] **Test 20.2**: Send very long message (10,000+ chars)
  - Expected: Message truncated or rejected
  - Expected: Warning message displays
  
- [ ] **Test 20.3**: Upload invalid file type
  - Expected: Error message displays
  - Expected: Only images accepted

## 💰 Cost Testing

### 21. Firestore Usage
- [ ] **Test 21.1**: Monitor read operations
  - Expected: Reads stay within budget
  - Expected: Pagination reduces reads
  
- [ ] **Test 21.2**: Monitor write operations
  - Expected: Writes are optimized
  - Expected: Batch operations used where possible
  
- [ ] **Test 21.3**: Storage usage
  - Expected: Images compressed
  - Expected: Old images cleaned up

## 🐛 Edge Cases

### 22. Edge Case Testing
- [ ] **Test 22.1**: User deletes account
  - Expected: Chats remain for other user
  - Expected: Deleted user shows as "Deleted User"
  
- [ ] **Test 22.2**: Product deleted after sharing
  - Expected: Product card shows "Product unavailable"
  
- [ ] **Test 22.3**: Order cancelled after update
  - Expected: Order status reflects cancellation
  
- [ ] **Test 22.4**: Simultaneous message deletion
  - Expected: No conflicts
  - Expected: Correct message deleted
  
- [ ] **Test 22.5**: Chat with blocked user
  - Expected: Cannot send messages
  - Expected: Previous messages still visible

## 📊 Test Results Template

```
Test Date: ___________
Tester: ___________
Device: ___________
OS Version: ___________

| Test ID | Test Name | Status | Notes |
|---------|-----------|--------|-------|
| 1.1 | Chat Creation | ✅ PASS | |
| 1.2 | Seller Chat | ✅ PASS | |
| ... | ... | ... | ... |

Issues Found:
1. [Issue description]
2. [Issue description]

Overall Status: ✅ PASS / ⚠️ PARTIAL / ❌ FAIL
```

## 🚀 Pre-Launch Checklist

- [ ] All functional tests passed
- [ ] All real-time tests passed
- [ ] Push notifications working
- [ ] UI/UX tests passed
- [ ] Performance acceptable
- [ ] Security tests passed
- [ ] Edge cases handled
- [ ] Firestore costs within budget
- [ ] Error handling implemented
- [ ] Loading states added
- [ ] Empty states designed
- [ ] Analytics integrated
- [ ] User feedback collected
- [ ] Documentation complete

## 📝 Notes

- Test with real users before launch
- Monitor Firestore usage in production
- Collect user feedback continuously
- Update tests as features are added
- Keep test data separate from production

## 🎯 Success Criteria

✅ **Ready for Production if:**
- 95%+ tests passing
- No critical bugs
- Performance acceptable
- Security verified
- User feedback positive
- Costs within budget

Good luck with testing! 🚀
