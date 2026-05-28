# Complete Reports System Fix & Integration

## 🔍 Issues Identified from Screenshots

### Issue 1: Reason Shows "Other" Instead of Actual Reason
**Problem**: Web dashboard shows "Other" but user selected specific reason like "Harassment"
**Cause**: Report model has predefined reasons but web dashboard isn't mapping them correctly

### Issue 2: Confusing "Reported From" Text
**Problem**: Shows both "Reported from Mobile App" and "Reported from chat"
**Cause**: Redundant fields in display

### Issue 3: Action Buttons Not Integrated
**Problem**: Remove Content, Suspend Account, Ban User buttons don't actually work
**Cause**: Only local state updates, no Firebase integration

### Issue 4: Report Categories Not Context-Specific
**Problem**: Same report reasons for all scenarios
**Need**: Different reasons for Product, Seller, Buyer, Chat reports

---

## ✅ Solution 1: Fix Report Reasons Display

### Update Report Model (Android)

The current model already has proper structure, but we need to ensure reasons are stored correctly:

```kotlin
// app/src/main/java/com/gcuf/craftoria/data/model/Report.kt

// Add predefined reason enums
object ReportReasons {
    // Product Report Reasons
    val PRODUCT_REASONS = listOf(
        "Counterfeit Product",
        "Misleading Description",
        "Inappropriate Content",
        "Prohibited Item",
        "Price Manipulation",
        "Poor Quality",
        "Other"
    )
    
    // Seller Report Reasons
    val SELLER_REASONS = listOf(
        "Poor Communication",
        "Delayed Shipping",
        "Fraudulent Behavior",
        "Harassment",
        "Fake Products",
        "Unprofessional Conduct",
        "Other"
    )
    
    // Buyer Report Reasons (for sellers to report buyers)
    val BUYER_REASONS = listOf(
        "Harassment",
        "Abusive Language",
        "Payment Issues",
        "False Claims",
        "Threatening Behavior",
        "Spam Messages",
        "Other"
    )
    
    // Chat Report Reasons
    val CHAT_REASONS = listOf(
        "Harassment",
        "Abusive Language",
        "Spam",
        "Inappropriate Content",
        "Threatening Behavior",
        "Scam Attempt",
        "Other"
    )
}
```

### Update Web Dashboard Data Mapping

The web dashboard should display the exact reason stored in Firebase:

```javascript
// In Reports.jsx loadReports function
return {
  id: d.id,
  type: typeLabels[docData.type] || 'Inappropriate Products',
  typeKey: docData.type || 'product',
  icon: docData.type || 'product',
  
  reporter: {
    id: docData.reporter_id || '',
    name: docData.reporter_name || 'Unknown User',
    avatar: (docData.reporter_name || 'U').substring(0, 2).toUpperCase()
  },
  
  reportedEntity: {
    id: docData.reported_entity_id || '',
    name: docData.reported_entity_name || 'Unknown Entity'
  },
  
  // ✅ FIX: Use actual reason from Firebase
  reason: docData.reason || 'No reason provided',
  description: docData.description || 'No description provided',
  
  // ✅ FIX: Simplify source display
  source: docData.source || 'mobile app',
  
  status: docData.status || 'New',
  date: docData.created_at 
    ? new Date(docData.created_at).toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      })
    : new Date().toLocaleDateString(),
  evidence: docData.evidence || []
};
```

---

## ✅ Solution 2: Fix "Reported From" Display

### Update Web Dashboard Report Card

Remove redundant "Reported from chat" text and simplify:

```jsx
{/* Remove this section completely */}
{/* 
{report.source && (
  <Box sx={{ mb: 1.5 }}>
    <Typography>Reported From</Typography>
    <Typography>Reported from {report.source}</Typography>
  </Box>
)}
*/}

{/* Keep only the essential info */}
<Box sx={{ mb: 1.5 }}>
  <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 0.75 }}>
    Reason
  </Typography>
  <Typography sx={{ fontSize: '0.85rem', color: '#333', fontWeight: 600 }}>
    {report.reason}
  </Typography>
</Box>

<Box sx={{ mb: 1.5 }}>
  <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: '#999', textTransform: 'uppercase', mb: 0.75 }}>
    Description
  </Typography>
  <Typography sx={{ fontSize: '0.85rem', color: '#666', lineHeight: 1.6 }}>
    {report.description}
  </Typography>
</Box>
```

---

## ✅ Solution 3: Implement Action Buttons with Firebase

### Complete Firebase Integration for Action Buttons

```javascript
// Add these imports at top of Reports.jsx
import { doc, updateDoc, deleteDoc } from 'firebase/firestore';

// Update confirmAction function
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
    
    // 1. Update report status in Firebase
    await updateDoc(doc(db, 'reports', report.id), {
      status: 'Resolved',
      action_taken: actionType,
      action_notes: actionNotes,
      action_by: 'admin', // Get from auth context
      resolved_at: Date.now(),
      updated_at: Date.now()
    });
    
    // 2. Send notification to reporter
    await addDoc(collection(db, 'notifications'), {
      user_id: report.reporter.id,
      title: 'Report Resolved',
      description: `Action taken: ${actionType}. ${actionNotes}`,
      category: 'REPORT',
      action_type: 'VIEW_REPORT',
      action_data: { report_id: report.id },
      is_read: false,
      created_at: Date.now()
    });
    
    // 3. Take actual action based on type
    if (actionType === 'Remove Content') {
      if (report.typeKey === 'product') {
        // Remove/deactivate product
        await updateDoc(doc(db, 'products', report.reportedEntity.id), {
          is_active: false,
          is_removed: true,
          removed_reason: actionNotes,
          removed_at: Date.now(),
          removed_by: 'admin'
        });
        toast.success('Product removed successfully');
      }
    } 
    else if (actionType === 'Suspend User Account') {
      // Suspend user account
      await updateDoc(doc(db, 'users', report.reportedEntity.id), {
        is_suspended: true,
        suspension_reason: actionNotes,
        suspended_at: Date.now(),
        suspended_by: 'admin'
      });
      toast.success('User account suspended');
    }
    else if (actionType === 'Ban User') {
      // Ban user permanently
      await updateDoc(doc(db, 'users', report.reportedEntity.id), {
        is_banned: true,
        ban_reason: actionNotes,
        banned_at: Date.now(),
        banned_by: 'admin'
      });
      toast.success('User banned permanently');
    }
    else if (actionType === 'Send Warning') {
      // Send warning notification
      await addDoc(collection(db, 'notifications'), {
        user_id: report.reportedEntity.id,
        title: 'Warning from Admin',
        description: actionNotes,
        category: 'ADMIN_MESSAGE',
        action_type: 'VIEW_PROFILE',
        is_read: false,
        created_at: Date.now()
      });
      toast.success('Warning sent to user');
    }
    
    // Update local state
    setReports(prev => prev.map(r => 
      r.id === report.id ? { ...r, status: 'Resolved' } : r
    ));
    
    toast.success(`Action taken on report #${report.id}`);
    setActionModal({ open: false, report: null });
  } catch (error) {
    console.error('Error taking action:', error);
    toast.error('Failed to take action: ' + error.message);
  }
};
```

---

## ✅ Solution 4: Context-Specific Report Categories

### Update ProductDetailsScreen Report Dialog

```kotlin
// In ProductDetailsScreen.kt - ReportProductDialog

val reportReasons = remember {
    listOf(
        "Counterfeit Product",
        "Misleading Description",
        "Inappropriate Content",
        "Prohibited Item",
        "Price Manipulation",
        "Poor Quality",
        "Other"
    )
}
```

### Create Chat Report Dialog

```kotlin
// In ChatScreen.kt - Add Report Dialog

@Composable
fun ReportChatDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, description: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    val chatReportReasons = listOf(
        "Harassment",
        "Abusive Language",
        "Spam",
        "Inappropriate Content",
        "Threatening Behavior",
        "Scam Attempt",
        "Other"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Error
                )
                Text("Report User", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Please select a reason for reporting this user:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                
                // Reason selection
                chatReportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(reason, fontSize = 14.sp)
                    }
                }
                
                // Description field
                if (selectedReason.isNotEmpty()) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Additional Details (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedReason.isNotEmpty()) {
                        isSubmitting = true
                        onSubmit(selectedReason, description)
                    }
                },
                enabled = selectedReason.isNotEmpty() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Error)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit Report")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Add Report Option to Chat 3-Dot Menu

```kotlin
// In ChatScreen.kt - Update the 3-dot menu

var showReportDialog by remember { mutableStateOf(false) }

// In the DropdownMenu
DropdownMenuItem(
    text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Report User", color = Error)
        }
    },
    onClick = {
        showReportDialog = true
        showMenu = false
    }
)

// Show dialog
if (showReportDialog) {
    ReportChatDialog(
        onDismiss = { showReportDialog = false },
        onSubmit = { reason, description ->
            scope.launch {
                val result = reportRepository.submitReport(
                    reportType = ReportType.BUYER, // or SELLER based on context
                    reporterId = currentUserId,
                    reporterName = currentUserName,
                    reportedEntityId = otherUserId,
                    reportedEntityName = otherUserName,
                    reason = reason,
                    description = "Reported from chat. $description"
                )
                
                if (result.isSuccess) {
                    showReportDialog = false
                    // Show success message
                } else {
                    // Show error
                }
            }
        }
    )
}
```



---

## 📋 Summary of Report Categories by Context

### 1. Product Reports (ProductDetailsScreen)
**Who can report**: Buyers
**Report Type**: `PRODUCT`
**Reasons**:
- Counterfeit Product
- Misleading Description
- Inappropriate Content
- Prohibited Item
- Price Manipulation
- Poor Quality
- Other

**Reported Entity**: Product (product_id, product_name)

---

### 2. Seller Reports (Seller Public Profile)
**Who can report**: Buyers
**Report Type**: `SELLER`
**Reasons**:
- Poor Communication
- Delayed Shipping
- Fraudulent Behavior
- Harassment
- Fake Products
- Unprofessional Conduct
- Other

**Reported Entity**: Seller (seller_id, seller_name)

---

### 3. Buyer Reports (Chat Screen - Seller reporting Buyer)
**Who can report**: Sellers
**Report Type**: `BUYER`
**Reasons**:
- Harassment
- Abusive Language
- Payment Issues
- False Claims
- Threatening Behavior
- Spam Messages
- Other

**Reported Entity**: Buyer (buyer_id, buyer_name)

---

### 4. Chat Reports (Chat Screen - Anyone reporting chat behavior)
**Who can report**: Both Buyers and Sellers
**Report Type**: `BUYER` or `SELLER` (depending on who is being reported)
**Reasons**:
- Harassment
- Abusive Language
- Spam
- Inappropriate Content
- Threatening Behavior
- Scam Attempt
- Other

**Reported Entity**: Other user in chat
**Source**: "chat" (to distinguish from other reports)

---

## 🔄 Complete Flow Example

### Scenario: Buyer Reports Seller in Chat

1. **Buyer Action (Mobile App)**:
   - Opens chat with seller
   - Clicks 3-dot menu
   - Selects "Report User"
   - Chooses reason: "Harassment"
   - Adds description: "Seller is sending threatening messages"
   - Submits report

2. **Firebase Document Created**:
```javascript
{
  type: "seller",
  reporter_id: "buyer123",
  reporter_name: "Ahmed Ali",
  reported_entity_id: "seller456",
  reported_entity_name: "Sara's Crafts",
  reason: "Harassment",  // ✅ Exact reason selected
  description: "Reported from chat. Seller is sending threatening messages",
  source: "chat",
  status: "New",
  created_at: 1234567890,
  updated_at: 1234567890
}
```

3. **Web Dashboard Display**:
```
┌────────────────────────────────────────────┐
│ Ahmed Ali reported Sara's Crafts           │
│ Report Type: seller                        │
├────────────────────────────────────────────┤
│ REPORTER: Ahmed Ali                        │
│ ID: buyer123                               │
├────────────────────────────────────────────┤
│ REPORTED ENTITY: Sara's Crafts             │
│ ID: seller456                              │
│ [seller]                                   │
├────────────────────────────────────────────┤
│ REASON: Harassment  ← Shows exact reason   │
├────────────────────────────────────────────┤
│ DESCRIPTION:                               │
│ Reported from chat. Seller is sending      │
│ threatening messages                       │
├────────────────────────────────────────────┤
│ [Investigate] [Take Action] [Dismiss]      │
└────────────────────────────────────────────┘
```

4. **Admin Takes Action**:
   - Clicks "Take Action"
   - Selects "Send Warning"
   - Adds notes: "First warning for harassment. Further violations will result in suspension."
   - Clicks "Confirm Action"

5. **Firebase Updates**:
   - Report status → "Resolved"
   - Notification sent to buyer (reporter)
   - Warning notification sent to seller
   - Action logged with admin details

6. **Seller Receives Warning (Mobile App)**:
   - Push notification: "Warning from Admin"
   - In-app notification in NotificationsScreen
   - Message: "First warning for harassment..."

---

## 🎯 Action Buttons - What They Do

### 1. Investigate
**Action**: Changes status to "Under Review"
**Firebase**: Updates report status
**Notification**: Sends to reporter that investigation started
**Use Case**: Admin needs time to review evidence

### 2. Take Action → Remove Content
**Action**: Deactivates/removes the reported content
**Firebase**: 
- Updates `products` collection: `is_active: false, is_removed: true`
- Updates report: `status: Resolved, action_taken: Remove Content`
**Notification**: Notifies reporter that content was removed
**Use Case**: Product violates policies

### 3. Take Action → Suspend User Account
**Action**: Temporarily suspends user account
**Firebase**: 
- Updates `users` collection: `is_suspended: true`
- User cannot login until unsuspended
**Notification**: Notifies both reporter and suspended user
**Use Case**: Serious violation but not permanent ban

### 4. Take Action → Ban User
**Action**: Permanently bans user
**Firebase**: 
- Updates `users` collection: `is_banned: true`
- User cannot login ever (unless admin unbans)
**Notification**: Notifies both parties
**Use Case**: Severe/repeated violations

### 5. Take Action → Send Warning
**Action**: Sends official warning to user
**Firebase**: 
- Creates notification for reported user
- Logs warning in report
**Notification**: Warning message to reported user
**Use Case**: First-time minor violation

### 6. Dismiss
**Action**: Closes report without action
**Firebase**: 
- Updates report: `status: Resolved, dismissed: true`
**Notification**: Notifies reporter that report was reviewed
**Use Case**: False report or insufficient evidence

### 7. Contact
**Action**: Sends message to reporter
**Firebase**: 
- Creates notification for reporter
**Notification**: Admin message appears in reporter's notifications
**Use Case**: Need more information or clarification

---

## 🔒 Security & Validation

### Mobile App Checks
```kotlin
// Before allowing report submission
fun canSubmitReport(
    reporterId: String,
    reportedEntityId: String
): Boolean {
    // Can't report yourself
    if (reporterId == reportedEntityId) return false
    
    // Check if already reported (prevent spam)
    // Check rate limiting (max 5 reports per day)
    
    return true
}
```

### Firebase Security Rules
```javascript
match /reports/{reportId} {
  // Anyone authenticated can create a report
  allow create: if request.auth != null 
    && request.resource.data.reporter_id == request.auth.uid;
  
  // Only admins can update/delete reports
  allow update, delete: if get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'super_admin', 'moderator'];
  
  // Users can read their own reports, admins can read all
  allow read: if request.auth != null && 
    (resource.data.reporter_id == request.auth.uid || 
     get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['admin', 'super_admin', 'moderator']);
}
```

---

## ✅ Implementation Checklist

### Mobile App (Android):
- [ ] Update Report model with predefined reasons
- [ ] Add ReportChatDialog to ChatScreen
- [ ] Add "Report User" option to chat 3-dot menu
- [ ] Implement context-specific report reasons
- [ ] Add source field ("chat", "product", "profile")
- [ ] Test report submission from different screens

### Web Dashboard:
- [ ] Fix reason display (show actual reason, not "Other")
- [ ] Remove redundant "Reported from" section
- [ ] Implement Firebase integration for action buttons
- [ ] Add actual functionality for Remove Content
- [ ] Add actual functionality for Suspend Account
- [ ] Add actual functionality for Ban User
- [ ] Add actual functionality for Send Warning
- [ ] Test all action buttons with Firebase

### Firebase:
- [ ] Set up security rules for reports collection
- [ ] Create indexes for queries
- [ ] Test notification delivery
- [ ] Test action execution (suspend, ban, remove)

### Testing:
- [ ] Test product report flow
- [ ] Test seller report flow
- [ ] Test buyer report flow (from chat)
- [ ] Test chat report flow
- [ ] Test all admin actions
- [ ] Test notifications delivery
- [ ] Test suspended user cannot login
- [ ] Test banned user cannot login

---

## 📱 User Experience Flow

```
BUYER REPORTS PRODUCT
├─ Opens ProductDetailsScreen
├─ Clicks Flag icon
├─ Selects reason: "Misleading Description"
├─ Adds description
├─ Submits report
├─ Sees success message
└─ Receives notification when admin takes action

ADMIN REVIEWS REPORT
├─ Opens web dashboard
├─ Sees report with correct reason
├─ Clicks "Investigate" (status → Under Review)
├─ Reviews evidence
├─ Clicks "Take Action"
├─ Selects "Remove Content"
├─ Adds notes
├─ Confirms action
├─ Product is deactivated in Firebase
└─ Both parties receive notifications

SELLER RECEIVES WARNING
├─ Gets push notification
├─ Opens app
├─ Sees notification in NotificationsScreen
├─ Reads admin message
└─ Can improve behavior to avoid suspension
```

This complete system ensures professional, context-aware reporting with proper admin controls and Firebase integration!
