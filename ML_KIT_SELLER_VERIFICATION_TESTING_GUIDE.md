# 🧪 ML Kit Seller Verification - Complete Testing Guide

## Testing Overview

This guide covers all testing scenarios for the ML Kit seller verification system across Android app and web dashboard.

---

## 📱 ANDROID APP TESTING

### Setup
```bash
# 1. Sync Gradle
# 2. Build debug APK
# 3. Install on test device
# 4. Ensure camera permission is granted
```

### Test 1: Camera Permission Handling
**Objective**: Verify camera permission is requested and handled correctly

**Steps**:
1. Open app
2. Navigate to Seller Verification Screen
3. Click "Take Selfie for Verification"
4. If permission not granted, grant it
5. Camera should open

**Expected Result**: ✅ Camera opens successfully

**Failure Handling**:
- If permission denied: App should show error message
- If permission revoked: App should request again

---

### Test 2: Valid Face Detection
**Objective**: Verify ML Kit detects a valid face correctly

**Steps**:
1. Open camera
2. Position face clearly in frame
3. Ensure good lighting
4. Face should be at least 100x100 pixels
5. Take photo

**Expected Result**: ✅ 
- ML Kit detects face
- Confidence score > 80%
- "Face detected successfully" message
- "Submit Verification" button enabled

**Metrics to Check**:
- Confidence score (should be 80-100%)
- Face count (should be 1)
- Detection time (should be < 500ms)

---

### Test 3: Multiple Faces Detection
**Objective**: Verify ML Kit rejects multiple faces

**Steps**:
1. Open camera
2. Position 2+ faces in frame
3. Take photo

**Expected Result**: ✅
- ML Kit detects multiple faces
- Error message: "Multiple faces detected. Please take a selfie with only your face."
- "Retry" button shown

**Metrics to Check**:
- Face count (should be > 1)
- Error message displayed

---

### Test 4: No Face Detection
**Objective**: Verify ML Kit rejects when no face is detected

**Steps**:
1. Open camera
2. Point at non-face object (wall, object, etc.)
3. Take photo

**Expected Result**: ✅
- ML Kit detects no face
- Error message: "No face detected. Please take a clear selfie."
- "Retry" button shown

**Metrics to Check**:
- Face count (should be 0)
- Error message displayed

---

### Test 5: Poor Quality Face
**Objective**: Verify ML Kit rejects poor quality faces

**Steps**:
1. Open camera
2. Take photo with:
   - Poor lighting
   - Motion blur
   - Face too small
   - Face partially visible

**Expected Result**: ✅
- ML Kit detects face but quality is poor
- Error message: "Face quality is poor. Please ensure good lighting and clear visibility."
- "Retry" button shown

**Metrics to Check**:
- Confidence score (should be < 60%)
- Error message displayed

---

### Test 6: Firebase Submission Success
**Objective**: Verify verification data is saved to Firebase

**Steps**:
1. Take valid selfie
2. ML Kit confirms success
3. Click "Submit Verification"
4. Wait for submission

**Expected Result**: ✅
- Success message: "Verification submitted successfully"
- Data saved to Firestore `seller_verifications` collection
- Real-time status updates work

**Verification in Firebase**:
```
seller_verifications/{verificationId}
├── userId: "user_id"
├── verificationStatus: "pending"
├── mlKitResults: {
│   ├── confidence: 95
│   ├── faceCount: 1
│   └── timestamp: 1234567890
├── photoUrl: "gs://..."
└── submittedAt: 1234567890
```

---

### Test 7: Firebase Submission Failure
**Objective**: Verify error handling when Firebase submission fails

**Steps**:
1. Disable network connection
2. Take valid selfie
3. Click "Submit Verification"
4. Wait for error

**Expected Result**: ✅
- Error message: "Failed to submit verification. Please check your connection."
- "Retry" button shown
- Data not lost

**Verification**:
- Enable network
- Click "Retry"
- Submission should succeed

---

### Test 8: Real-time Status Updates
**Objective**: Verify status updates in real-time

**Steps**:
1. Submit verification from Android app
2. Open web dashboard on another device
3. Admin approves verification
4. Check Android app for status update

**Expected Result**: ✅
- Android app shows updated status
- Notification received (if enabled)
- Status changes from "pending" to "approved"

---

### Test 9: Permission Denied Scenario
**Objective**: Verify app handles permission denial

**Steps**:
1. Deny camera permission
2. Try to open camera
3. Check error handling

**Expected Result**: ✅
- Error message: "Camera permission is required for verification"
- "Grant Permission" button shown
- User can grant permission and retry

---

### Test 10: Network Error Handling
**Objective**: Verify app handles network errors gracefully

**Steps**:
1. Enable airplane mode
2. Try to submit verification
3. Check error handling

**Expected Result**: ✅
- Error message: "Network error. Please check your connection."
- "Retry" button shown
- Data preserved for retry

---

## 🌐 WEB DASHBOARD TESTING

### Setup
```bash
# 1. Update App.jsx with route
# 2. Update Sidebar.jsx with menu item
# 3. Deploy to staging
# 4. Login as admin
```

### Test 1: Dashboard Navigation
**Objective**: Verify dashboard is accessible from sidebar

**Steps**:
1. Login as admin
2. Look for "ML Kit Verification" in sidebar
3. Click menu item
4. Dashboard should load

**Expected Result**: ✅
- Menu item visible
- Route loads correctly
- Dashboard displays

---

### Test 2: Real-time Verification List
**Objective**: Verify verifications load in real-time

**Steps**:
1. Open dashboard
2. Submit verification from Android app
3. Check dashboard for new verification

**Expected Result**: ✅
- Pending verifications load
- New verification appears instantly
- Real-time listener works

**Metrics to Check**:
- Load time < 2 seconds
- Update latency < 1 second

---

### Test 3: View Verification Photo
**Objective**: Verify photo preview works

**Steps**:
1. Click on a verification
2. Click "View Photo" button
3. Photo should display in modal

**Expected Result**: ✅
- Photo loads correctly
- Modal displays
- Photo is clear and visible

---

### Test 4: ML Kit Metrics Display
**Objective**: Verify ML Kit metrics are displayed

**Steps**:
1. Click on a verification
2. Check for ML Kit metrics:
   - Confidence score
   - Face count
   - Detection time

**Expected Result**: ✅
- Confidence score displayed (0-100%)
- Face count displayed (should be 1)
- Detection time displayed (in ms)

**Example Display**:
```
ML Kit Metrics:
├── Confidence: 95%
├── Face Count: 1
└── Detection Time: 245ms
```

---

### Test 5: Filter by Status
**Objective**: Verify filtering works correctly

**Steps**:
1. Click "Pending" filter
2. Only pending verifications shown
3. Click "Approved" filter
4. Only approved verifications shown
5. Click "Rejected" filter
6. Only rejected verifications shown

**Expected Result**: ✅
- Filter works correctly
- List updates instantly
- Count badges update

---

### Test 6: Search by Name
**Objective**: Verify search by name works

**Steps**:
1. Type seller name in search box
2. Results should filter

**Expected Result**: ✅
- Search works correctly
- Results update instantly
- Partial matches work

---

### Test 7: Search by Email
**Objective**: Verify search by email works

**Steps**:
1. Type seller email in search box
2. Results should filter

**Expected Result**: ✅
- Search works correctly
- Results update instantly
- Partial matches work

---

### Test 8: Approve Verification
**Objective**: Verify approval workflow

**Steps**:
1. Click on a pending verification
2. Click "Approve" button
3. Add optional message
4. Click "Confirm"

**Expected Result**: ✅
- Verification status changes to "approved"
- Message saved
- Seller receives notification
- List updates in real-time

**Verification in Firebase**:
```
seller_verifications/{verificationId}
├── verificationStatus: "approved"
├── approvedAt: 1234567890
├── approvedBy: "admin_id"
└── approvalMessage: "Welcome to Craftoria!"
```

---

### Test 9: Reject Verification
**Objective**: Verify rejection workflow

**Steps**:
1. Click on a pending verification
2. Click "Reject" button
3. Select rejection reason
4. Add optional message
5. Click "Confirm"

**Expected Result**: ✅
- Verification status changes to "rejected"
- Reason and message saved
- Seller receives notification
- List updates in real-time

**Verification in Firebase**:
```
seller_verifications/{verificationId}
├── verificationStatus: "rejected"
├── rejectedAt: 1234567890
├── rejectedBy: "admin_id"
├── rejectionReason: "Poor image quality"
└── rejectionMessage: "Please retake with better lighting"
```

---

### Test 10: Statistics Dashboard
**Objective**: Verify statistics are accurate

**Steps**:
1. Check statistics section
2. Verify counts:
   - Pending count
   - Approved count
   - Rejected count
   - Total count

**Expected Result**: ✅
- All counts accurate
- Update in real-time
- Percentages calculated correctly

**Example Statistics**:
```
Pending: 5 (25%)
Approved: 12 (60%)
Rejected: 3 (15%)
Total: 20
```

---

### Test 11: Permission-Based Access
**Objective**: Verify only admins can approve/reject

**Steps**:
1. Login as non-admin user
2. Try to access dashboard
3. Should be denied or buttons disabled

**Expected Result**: ✅
- Non-admin cannot access dashboard
- Or buttons are disabled
- Error message shown

---

### Test 12: Real-time Notifications
**Objective**: Verify seller receives notification on approval/rejection

**Steps**:
1. Approve/Reject verification from dashboard
2. Check seller's notifications
3. Notification should appear

**Expected Result**: ✅
- Notification received
- Message included
- Status updated in real-time

---

## 🔄 END-TO-END FLOW TESTING

### Complete Flow Test
**Objective**: Verify entire workflow from app to dashboard

**Steps**:
1. **Android App**:
   - Open Seller Verification Screen
   - Take selfie
   - ML Kit validates
   - Submit verification

2. **Web Dashboard**:
   - Open dashboard
   - See pending verification
   - View photo and metrics
   - Approve verification

3. **Android App**:
   - Receive notification
   - Check status update
   - Status shows "approved"

**Expected Result**: ✅
- All steps complete successfully
- Real-time updates work
- Notifications delivered
- Data consistent across platforms

---

## 📊 PERFORMANCE TESTING

### Metrics to Monitor

| Metric | Target | How to Test |
|--------|--------|------------|
| Face Detection Time | <500ms | Logcat / Performance Monitor |
| Dashboard Load Time | <2s | Web Performance Monitor |
| Real-time Update Latency | <1s | Firestore listener logs |
| Verification Success Rate | >90% | Test 20+ verifications |
| Photo Load Time | <1s | Network tab in DevTools |

### Load Testing
```
Test with 100+ pending verifications:
- Dashboard should load in <3s
- Filtering should work smoothly
- Real-time updates should not lag
- No memory leaks
```

---

## 🐛 DEBUGGING TIPS

### Android Debugging
```bash
# View logcat
adb logcat | grep MLKit

# Check Firebase connection
adb logcat | grep Firebase

# Monitor performance
adb shell dumpsys meminfo com.gcuf.craftoria
```

### Web Debugging
```javascript
// Check Firestore listener
console.log('Firestore listener active');

// Monitor real-time updates
db.collection('seller_verifications').onSnapshot(snapshot => {
  console.log('Update received:', snapshot.docs.length);
});

// Check performance
console.time('Dashboard Load');
// ... code ...
console.timeEnd('Dashboard Load');
```

---

## ✅ TEST CHECKLIST

### Android App
- [ ] Camera permission works
- [ ] Valid face detected
- [ ] Multiple faces rejected
- [ ] No face rejected
- [ ] Poor quality rejected
- [ ] Firebase submission succeeds
- [ ] Firebase submission failure handled
- [ ] Real-time status updates
- [ ] Permission denied handled
- [ ] Network error handled

### Web Dashboard
- [ ] Dashboard accessible
- [ ] Real-time list loads
- [ ] Photo preview works
- [ ] ML Kit metrics displayed
- [ ] Filter by status works
- [ ] Search by name works
- [ ] Search by email works
- [ ] Approve workflow works
- [ ] Reject workflow works
- [ ] Statistics accurate
- [ ] Permission-based access works
- [ ] Notifications sent

### End-to-End
- [ ] Complete flow works
- [ ] Real-time updates work
- [ ] Notifications delivered
- [ ] Data consistent

### Performance
- [ ] Face detection < 500ms
- [ ] Dashboard load < 2s
- [ ] Real-time update < 1s
- [ ] Success rate > 90%

---

## 🎯 SIGN-OFF

Once all tests pass, sign off:

- [ ] All tests passed
- [ ] No critical bugs
- [ ] Performance acceptable
- [ ] Security verified
- [ ] Ready for production

**Tested By**: _______________  
**Date**: _______________  
**Status**: ✅ APPROVED FOR PRODUCTION

---

**Last Updated**: March 25, 2026  
**Version**: 1.0  
**Status**: Ready for Testing

