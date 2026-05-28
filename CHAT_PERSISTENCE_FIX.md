# Chat Message Persistence Issue - Diagnosis & Fix

## Problem
Messages show in chat screen when sent, but disappear when navigating back or to profile. Chat history not showing in "My Chats" and badge not appearing.

## Root Cause Analysis

Based on the code review, the issue is likely:

1. **Messages ARE being saved to Firestore** (sendMessage function works correctly)
2. **Real-time listener IS set up correctly** (getMessagesFlow uses snapshot listener)
3. **Chat initialization IS correct** (LaunchedEffect with otherUserId dependency)

The problem is likely one of these:

### Issue 1: ViewModel Not Retained
When navigating away and back, the ChatViewModel might be recreated, losing the message flow subscription.

### Issue 2: Chat ID Mismatch
The chat might be created with different IDs for the same conversation, causing messages to be saved in one chat but loaded from another.

### Issue 3: Firestore Listener Not Triggering
The snapshot listener might not be triggering updates after the initial load.

## Solution

### Fix 1: Add Logging to Diagnose
Add comprehensive logging to track:
- When chat is created/retrieved
- When messages are sent
- When listener receives updates
- Chat ID consistency

### Fix 2: Ensure ViewModel Persistence
Make sure ChatViewModel is scoped correctly so it persists across navigation.

### Fix 3: Force Refresh on Navigation
Add a mechanism to force refresh messages when returning to chat screen.

## Implementation

See the updated ChatViewModel and ChatRepository with enhanced logging.

## Testing Steps

1. Send a message from buyer to seller
2. Check Firestore console - verify message is saved
3. Check logs - verify chat ID is consistent
4. Navigate away and back - check if same chat ID is used
5. Check if listener receives snapshot updates
