# Complete Reports Integration - Web Dashboard ↔ Mobile App

## Current Status: ❌ NOT FULLY INTEGRATED

The current web dashboard code has these issues:

1. ✅ **Display**: Shows reports correctly from Firebase
2. ❌ **Investigate**: Only updates local state, doesn't save to Firebase
3. ❌ **Take Action**: Only updates local state, doesn't save to Firebase
4. ❌ **Dismiss**: Only updates local state, doesn't save to Firebase
5. ❌ **Contact**: Shows toast but doesn't send notification to mobile app

## Required Integration

### 1. INVESTIGATE Button
**What it should do:**
- Update report status to "Under Review" in Firebase
- Send notification to reporter (mobile app user)
- Log the action with admin details

### 2. TAKE ACTION Button
**What it should do:**
- Update report status to "Resolved" in Firebase
- Store action details (type, notes, admin who took action)
- Send notification to reporter about the action taken
- Optionally: Take actual action (remove product, suspend user, etc.)

### 3. DISMISS Button
**What it should do:**
- Update report status to "Resolved" in Firebase
- Store dismissal reason
- Send notification to reporter that report was reviewed but dismissed

### 4. CONTACT Button
**What it should do:**
- Send notification to reporter's mobile app
- Message appears in their Notifications screen
- Include admin message and option to reply

## Where Messages Will Show

When admin contacts reporter, the message will appear in:
- **Mobile App**: Notifications screen (NotificationsScreen.kt)
- **Notification Type**: "ADMIN_MESSAGE" or "REPORT_UPDATE"
- **User sees**: Push notification + in-app notification



## Complete Implementation for Reports.jsx

Replace the action handler functions with these Firebase-integrated versions:

### 1. Update handleInvestigate (with Firebase)

```javascript
const handleInvestigate = async (report) => {
  if (!canInvestigate) { 
    toast.error('You do not have permission to investigate reports'); 
    return; 
  }
  
  try {
    // Update report status in Firebase
    await updateDoc(doc(db, 'reports', report.id), {
      status: 'Under Review',
      investigated_at: Date.now(),
      investigated_by: 'admin_id_here', // Get from auth context
      updated_at: Date.now()
    });
    
    // Send notification to reporter
    await addDoc(collection(db, 'notifications'), {
      user_id: report.reporter.id,
      title: 'Report Under Investigation',
      description: `Your report about "${report.reportedEntity.name}" is now being investigated by our team.`,
      category: 'REPORT',
      action_type: 'VIEW_REPORT',
      action_data: { report_id: report.id },
      is_read: false,
      created_at: Date.now()
    });
    
    // Update local state
    setReports(prev => prev.map(r => 
      r.id === report.id ? { ...r, status: 'Under Review' } : r
    ));
    
    toast.success(`Investigation started for report #${report.id}`);
  } catch (error) {
    console.error('Error investigating report:', error);
    toast.error('Failed to start investigation');
  }
};
```

### 2. Update confirmAction (with Firebase)

```javascript
const confirmAction = async () => {
  if (!actionType) { 
    toast.error('Please select an action type'); 
    return; 
  }
  if (!actionNotes.trim()) { 
    toast.error('Please provide action notes'); 
    return; 
  }
  
  try {
    const report = actionModal.report;
    
    // Update report in Firebase
    await updateDoc(doc(db, 'reports', report.id), {
      status: 'Resolved',
      action_taken: actionType,
      action_notes: actionNotes,
      action_by: 'admin_id_here', // Get from auth context
      resolved_at: Date.now(),
      updated_at: Date.now()
    });
    
    // Send notification to reporter
    await addDoc(collection(db, 'notifications'), {
      user_id: report.reporter.id,
      title: 'Report Resolved',
      description: `Action taken on your report: ${actionType}. ${actionNotes}`,
      category: 'REPORT',
      action_type: 'VIEW_REPORT',
      action_data: { 
        report_id: report.id,
        action_type: actionType 
      },
      is_read: false,
      created_at: Date.now()
    });
    
    // Optional: Take actual action based on actionType
    if (actionType === 'Remove Content' && report.typeKey === 'product') {
      // Remove product from Firebase
      await updateDoc(doc(db, 'products', report.reportedEntity.id), {
        is_active: false,
        removed_reason: 'Admin action - Report resolved',
        removed_at: Date.now()
      });
    } else if (actionType === 'Suspend User Account') {
      // Suspend user account
      await updateDoc(doc(db, 'users', report.reportedEntity.id), {
        is_suspended: true,
        suspension_reason: actionNotes,
        suspended_at: Date.now()
      });
    }
    
    // Update local state
    setReports(prev => prev.map(r => 
      r.id === report.id ? { ...r, status: 'Resolved' } : r
    ));
    
    toast.success(`Action taken on report #${report.id}`);
    setActionModal({ open: false, report: null });
  } catch (error) {
    console.error('Error taking action:', error);
    toast.error('Failed to take action');
  }
};
```

### 3. Update confirmDismiss (with Firebase)

```javascript
const confirmDismiss = async () => {
  if (!dismissReason.trim()) { 
    toast.error('Please provide a reason for dismissal'); 
    return; 
  }
  
  try {
    const report = dismissModal.report;
    
    // Update report in Firebase
    await updateDoc(doc(db, 'reports', report.id), {
      status: 'Resolved',
      dismissed: true,
      dismiss_reason: dismissReason,
      dismissed_by: 'admin_id_here', // Get from auth context
      resolved_at: Date.now(),
      updated_at: Date.now()
    });
    
    // Send notification to reporter
    await addDoc(collection(db, 'notifications'), {
      user_id: report.reporter.id,
      title: 'Report Reviewed',
      description: `Your report has been reviewed. ${dismissReason}`,
      category: 'REPORT',
      action_type: 'VIEW_REPORT',
      action_data: { report_id: report.id },
      is_read: false,
      created_at: Date.now()
    });
    
    // Update local state
    setReports(prev => prev.map(r => 
      r.id === report.id ? { ...r, status: 'Resolved' } : r
    ));
    
    toast.success(`Report #${report.id} has been dismissed`);
    setDismissModal({ open: false, report: null });
  } catch (error) {
    console.error('Error dismissing report:', error);
    toast.error('Failed to dismiss report');
  }
};
```

### 4. Update sendContactMessage (with Firebase)

```javascript
const sendContactMessage = async () => {
  if (!contactMessage.trim()) { 
    toast.error('Please type a message'); 
    return; 
  }
  
  try {
    const report = contactModal.report;
    
    // Send notification to reporter (will appear in mobile app)
    await addDoc(collection(db, 'notifications'), {
      user_id: report.reporter.id,
      title: 'Message from Admin',
      description: contactMessage,
      category: 'ADMIN_MESSAGE',
      action_type: 'VIEW_REPORT',
      action_data: { 
        report_id: report.id,
        can_reply: true 
      },
      is_read: false,
      created_at: Date.now()
    });
    
    // Optionally: Log the contact in report
    await updateDoc(doc(db, 'reports', report.id), {
      admin_contacted: true,
      last_contact_at: Date.now(),
      last_contact_message: contactMessage,
      updated_at: Date.now()
    });
    
    toast.success(`Message sent to ${report.reporter.name}`);
    setContactModal({ open: false, report: null });
  } catch (error) {
    console.error('Error sending message:', error);
    toast.error('Failed to send message');
  }
};
```

### 5. Add Required Imports

Add these imports at the top of Reports.jsx:

```javascript
import { 
  collection, 
  query, 
  getDocs, 
  doc, 
  updateDoc, 
  addDoc 
} from 'firebase/firestore';
```



## Mobile App Integration

### Where Notifications Appear

When admin sends a message or takes action, the reporter will see it in:

**1. Push Notification (FCMService.kt)**
- Appears as system notification
- User can tap to open app

**2. In-App Notifications (NotificationsScreen.kt)**
- Shows in Notifications screen
- Categories: "REPORT", "ADMIN_MESSAGE"
- User can tap to view details

### Update Notification Model (if needed)

Your current Notification.kt should support these categories:

```kotlin
enum class NotificationCategory {
    ORDER,
    PRODUCT,
    STORE,
    LEARNING,
    REPORT,           // ✅ Add this
    ADMIN_MESSAGE,    // ✅ Add this
    SYSTEM
}
```

### Update NotificationsScreen.kt

Add handling for REPORT and ADMIN_MESSAGE categories:

```kotlin
when (notification.category) {
    NotificationCategory.REPORT -> {
        // Show report update notification
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = null,
            tint = Color(0xFFE91E63)
        )
    }
    NotificationCategory.ADMIN_MESSAGE -> {
        // Show admin message notification
        Icon(
            imageVector = Icons.Default.AdminPanelSettings,
            contentDescription = null,
            tint = Color(0xFF2196F3)
        )
    }
    // ... other categories
}
```

## Complete Flow Example

### Scenario: Admin Contacts Reporter

1. **Admin Action (Web Dashboard)**:
   - Admin clicks "Contact" button
   - Types message: "We need more information about this report"
   - Clicks "Send Message"

2. **Firebase Update**:
   - New document created in `notifications` collection:
   ```javascript
   {
     user_id: "reporter_user_id",
     title: "Message from Admin",
     description: "We need more information about this report",
     category: "ADMIN_MESSAGE",
     action_type: "VIEW_REPORT",
     action_data: { report_id: "RPT001", can_reply: true },
     is_read: false,
     created_at: 1234567890
   }
   ```

3. **Mobile App (Reporter's Phone)**:
   - **Push Notification**: "Message from Admin - We need more information..."
   - **Notifications Screen**: Shows new notification with admin icon
   - **User taps**: Opens notification details or report screen

4. **Reporter Can See**:
   - Notification title: "Message from Admin"
   - Message content: "We need more information about this report"
   - Related report: Link to their original report
   - Option to reply (if implemented)

## Testing Checklist

### Web Dashboard Tests:

1. [ ] Click "Investigate" → Report status changes to "Under Review" in Firebase
2. [ ] Click "Take Action" → Report marked as "Resolved" with action details
3. [ ] Click "Dismiss" → Report marked as "Resolved" with dismiss reason
4. [ ] Click "Contact" → Notification created in Firebase
5. [ ] All actions update `updated_at` timestamp
6. [ ] Admin ID is recorded for audit trail

### Mobile App Tests:

1. [ ] Reporter receives push notification when admin contacts them
2. [ ] Notification appears in NotificationsScreen
3. [ ] Tapping notification shows message details
4. [ ] Reporter receives notification when report status changes
5. [ ] Reporter receives notification when action is taken
6. [ ] Notification badge count updates correctly

## Security Considerations

### Firebase Security Rules

Add these rules to protect reports:

```javascript
// Firestore Security Rules
match /reports/{reportId} {
  // Anyone can create a report
  allow create: if request.auth != null;
  
  // Only admins can update reports
  allow update: if get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'super_admin', 'moderator'];
  
  // Users can read their own reports, admins can read all
  allow read: if request.auth != null && 
    (resource.data.reporter_id == request.auth.uid || 
     get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'super_admin', 'moderator']);
}

match /notifications/{notificationId} {
  // Only the user can read their notifications
  allow read: if request.auth != null && resource.data.user_id == request.auth.uid;
  
  // Admins can create notifications for users
  allow create: if request.auth != null && 
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'super_admin', 'moderator'];
  
  // Users can update their own notifications (mark as read)
  allow update: if request.auth != null && resource.data.user_id == request.auth.uid;
}
```

## Additional Features to Consider

### 1. Report History/Timeline
Track all actions taken on a report:

```javascript
// Add to reports collection
{
  ...report_data,
  timeline: [
    { action: 'created', by: 'user_id', at: timestamp },
    { action: 'investigated', by: 'admin_id', at: timestamp },
    { action: 'contacted', by: 'admin_id', message: '...', at: timestamp },
    { action: 'resolved', by: 'admin_id', details: '...', at: timestamp }
  ]
}
```

### 2. Reply to Admin
Allow reporters to reply to admin messages:

```kotlin
// In mobile app
fun replyToAdmin(reportId: String, message: String) {
    firestore.collection("report_messages").add(
        mapOf(
            "report_id" to reportId,
            "from_user_id" to currentUserId,
            "to_admin" to true,
            "message" to message,
            "created_at" to System.currentTimeMillis()
        )
    )
}
```

### 3. Admin Dashboard Statistics
Show report metrics:
- Total reports
- Pending reports
- Resolved reports
- Average resolution time
- Reports by category

## Summary

**Current Status**: ❌ Web dashboard only updates local state

**After Implementation**: ✅ Full integration
- Web dashboard updates Firebase
- Mobile app receives real-time notifications
- Complete audit trail
- Professional admin-user communication

**Where Messages Show**:
- Push notification on reporter's phone
- In-app Notifications screen
- Can include action buttons (View Report, Reply, etc.)

The integration is straightforward - just replace the local state updates with Firebase operations and add notification creation!
