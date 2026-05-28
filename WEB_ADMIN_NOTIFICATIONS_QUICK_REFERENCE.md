# Web Admin Notifications - Quick Reference

## 🚀 QUICK START

### Import the Service
```javascript
import { 
  notifyVerificationApproved, 
  notifyProductRejected,
  notifyOrderStatusChanged 
} from '../services/webAdminNotificationService';
```

### Basic Usage
```javascript
// Approve seller verification
await notifyVerificationApproved(sellerId);

// Reject product with reason
await notifyProductRejected(sellerId, productTitle, productId, reason);

// Update order status (notify buyer)
await notifyOrderStatusChanged(buyerId, orderNumber, 'shipped', false);
```

## 📋 ALL AVAILABLE FUNCTIONS

### **Seller Management**
| Function | Parameters | Purpose |
|----------|------------|---------|
| `notifyApplicationApproved` | `(userId, welcomeMessage)` | New seller application approved |
| `notifyApplicationRejected` | `(userId, reason)` | New seller application rejected |
| `notifyVerificationApproved` | `(sellerId)` | Identity verification approved |
| `notifyVerificationRejected` | `(sellerId, reason)` | Identity verification rejected |

### **Product Management**
| Function | Parameters | Purpose |
|----------|------------|---------|
| `notifyProductApproved` | `(sellerId, productTitle, productId)` | Product approved by admin |
| `notifyProductRejected` | `(sellerId, productTitle, productId, reason)` | Product rejected by admin |

### **User Management**
| Function | Parameters | Purpose |
|----------|------------|---------|
| `notifyAccountSuspended` | `(userId, reason)` | User account suspended |
| `notifyAccountReactivated` | `(userId)` | User account reactivated |

### **Order Management**
| Function | Parameters | Purpose |
|----------|------------|---------|
| `notifyOrderStatusChanged` | `(userId, orderNumber, newStatus, isSellerNotification)` | Order status updated |

### **Store Management**
| Function | Parameters | Purpose |
|----------|------------|---------|
| `notifyStoreFlagged` | `(ownerId, storeName, reason)` | Store flagged by admin |
| `notifyStoreFlagRemoved` | `(ownerId, storeName)` | Store flag removed |

### **System-Wide**
| Function | Parameters | Purpose |
|----------|------------|---------|
| `broadcastPolicyUpdate` | `(policyType, summary)` | Policy update to all users |
| `broadcastSystemMaintenance` | `(maintenanceDate, duration, affectedServices)` | Maintenance notice to all |

## 🎯 PRIORITY LEVELS

| Priority | Color | Animation | Use Case |
|----------|-------|-----------|----------|
| `urgent` | Red | Fast pulse (800ms) | Account bans, critical issues |
| `high` | Deep Orange | Normal pulse (1200ms) | Approvals, rejections |
| `medium` | Orange | Normal pulse | Order updates, warnings |
| `low` | Blue | No pulse | Maintenance, policy updates |

## 💡 BEST PRACTICES

### **Error Handling**
```javascript
try {
  // Update database first
  await updateDoc(doc(db, 'users', userId), { status: 'approved' });
  
  // Then send notification
  await notifyVerificationApproved(userId);
  toast.success('Verification approved!');
} catch (error) {
  console.error('Error:', error);
  toast.error('Failed to approve verification');
}
```

### **Notification Wrapping**
```javascript
// Wrap notification calls to prevent blocking main action
try {
  await notifyUser(userId, title, message);
} catch (notifError) {
  console.error('Failed to send notification:', notifError);
  // Don't fail the main action
}
```

### **Order Status Notifications**
```javascript
// Notify both buyer and seller
const order = statusModal.order;

// Buyer notification
if (order.buyer_id) {
  await notifyOrderStatusChanged(order.buyer_id, order.id, newStatus, false);
}

// Seller notification  
if (order.seller_id) {
  await notifyOrderStatusChanged(order.seller_id, order.id, newStatus, true);
}
```

## 🔧 COMMON PATTERNS

### **Approval Flow**
```javascript
const handleApprove = async () => {
  try {
    // 1. Update database
    await updateDoc(doc(db, 'users', userId), {
      verification_status: 'approved',
      verified: true,
      verified_at: serverTimestamp()
    });
    
    // 2. Send notification
    await notifyVerificationApproved(userId);
    
    // 3. Show success
    toast.success('Seller verification approved!');
    
    // 4. Close modal
    setModal({ open: false, user: null });
  } catch (error) {
    toast.error('Failed to approve verification');
  }
};
```

### **Rejection Flow**
```javascript
const handleReject = async () => {
  if (!reason.trim()) {
    toast.error('Please provide a reason');
    return;
  }
  
  try {
    // 1. Update database
    await updateDoc(doc(db, 'products', productId), {
      status: 'rejected',
      rejection_reason: reason,
      rejected_at: serverTimestamp()
    });
    
    // 2. Send notification
    await notifyProductRejected(sellerId, productTitle, productId, reason);
    
    // 3. Show success
    toast.success('Product rejected');
    
    // 4. Reset form
    setReason('');
    setModal({ open: false, product: null });
  } catch (error) {
    toast.error('Failed to reject product');
  }
};
```

## 🎨 BADGE INTEGRATION

The notification system automatically integrates with the mobile app's badge system:

- **Real-time updates**: Badges update instantly when notifications are sent
- **Smart pulsing**: New notifications pulse for 30 seconds
- **Priority colors**: Badge colors match notification priority
- **Count display**: Shows exact number of unread notifications

## 📱 MOBILE APP INTEGRATION

Notifications sent from web admin automatically appear in the mobile app:

1. **Real-time delivery**: Instant notification delivery via Firebase
2. **Badge updates**: Notification badges update automatically
3. **Action buttons**: Notifications include relevant action buttons
4. **Priority styling**: High-priority notifications get special treatment

## 🚨 TROUBLESHOOTING

### **Notification Not Sent**
- Check user ID exists and is valid
- Verify Firebase Cloud Functions are deployed
- Check console for error messages
- Ensure notification service is imported correctly

### **Badge Not Updating**
- Verify notification was created in Firestore
- Check mobile app's real-time listeners
- Ensure badge manager is properly integrated

### **Wrong Priority/Color**
- Check priority parameter in notification call
- Verify badge manager priority mapping
- Ensure notification category is correct

## 📞 SUPPORT

For issues with the notification system:
1. Check console logs for errors
2. Verify Firebase Cloud Functions status
3. Test with a simple notification first
4. Check mobile app notification listeners