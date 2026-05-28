# Web Dashboard Notification Integration Guide

## Overview
This guide explains how to integrate notifications into web admin dashboard pages so that admin actions trigger notifications to users on the mobile app.

## Architecture

```
Web Admin Action
    ↓
Call notificationService function
    ↓
Cloud Function triggered (optional - for Firestore updates)
    ↓
Notification created in Firestore
    ↓
Mobile app receives notification in real-time
    ↓
Badge updates + notification displayed
```

## Setup

### 1. Import the Notification Service
```javascript
import { 
  notifyProductApproved, 
  notifyProductRejected,
  notifyVerificationApproved,
  notifyVerificationRejected,
  notifyAccountSuspended,
  notifyAccountReactivated,
  notifyStoreFlagged,
  notifyStoreFlagRemoved,
  notifyOrderStatusChanged,
  notifyUser,
  notifyAllSellers
} from '../services/notificationService';
```

### 2. Use in Your Handlers
```javascript
// Example: Product Approval
const handleApproveProduct = async (product) => {
  try {
    // Update Firestore
    await updateDoc(doc(db, 'products', product.id), {
      approval_status: 'approved',
      approved_at: serverTimestamp(),
      approved_by: currentUser.id,
      is_active: true
    });

    // Send notification to seller
    await notifyProductApproved(product.seller_id, product.title, product.id);
    
    toast.success('Product approved and seller notified');
  } catch (error) {
    console.error('Error approving product:', error);
    toast.error('Failed to approve product');
  }
};
```

## Integration Points

### ProductManagement Page

#### Approve Product
```javascript
const handleApproveProduct = async (product) => {
  try {
    await updateDoc(doc(db, 'products', product.id), {
      approval_status: 'approved',
      approved_at: serverTimestamp(),
      approved_by: currentUser.id,
      is_active: true
    });
    
    // ADD THIS:
    await notifyProductApproved(product.seller_id, product.title, product.id);
    
    toast.success('Product approved');
  } catch (error) {
    toast.error('Failed to approve product');
  }
};
```

#### Reject Product
```javascript
const handleRejectProduct = async (product, reason) => {
  try {
    await updateDoc(doc(db, 'products', product.id), {
      approval_status: 'rejected',
      rejected_at: serverTimestamp(),
      rejected_by: currentUser.id,
      rejection_reason: reason,
      is_active: false
    });
    
    // ADD THIS:
    await notifyProductRejected(product.seller_id, product.title, product.id, reason);
    
    toast.success('Product rejected');
  } catch (error) {
    toast.error('Failed to reject product');
  }
};
```

### SellerVerification Page

#### Approve Seller
```javascript
const handleApproveSeller = async (seller) => {
  try {
    await updateDoc(doc(db, 'users', seller.id), {
      seller_verification_status: 'approved',
      verified_at: serverTimestamp(),
      verified_by: currentUser.id
    });
    
    // ADD THIS:
    await notifyVerificationApproved(seller.id);
    
    toast.success('Seller verified');
  } catch (error) {
    toast.error('Failed to verify seller');
  }
};
```

#### Reject Seller
```javascript
const handleRejectSeller = async (seller, reason) => {
  try {
    await updateDoc(doc(db, 'users', seller.id), {
      seller_verification_status: 'rejected',
      rejected_at: serverTimestamp(),
      rejected_by: currentUser.id,
      verification_rejection_reason: reason
    });
    
    // ADD THIS:
    await notifyVerificationRejected(seller.id, reason);
    
    toast.success('Seller verification rejected');
  } catch (error) {
    toast.error('Failed to reject seller');
  }
};
```

### UserManagement Page

#### Suspend User
```javascript
const handleSuspendUser = async (user, reason) => {
  try {
    await updateDoc(doc(db, 'users', user.id), {
      is_suspended: true,
      suspension_reason: reason,
      suspended_at: serverTimestamp(),
      suspended_by: currentUser.id,
      suspension_until: serverTimestamp() + (30 * 24 * 60 * 60 * 1000)
    });
    
    // ADD THIS:
    await notifyAccountSuspended(user.id, reason);
    
    toast.success('User suspended');
  } catch (error) {
    toast.error('Failed to suspend user');
  }
};
```

#### Activate User
```javascript
const handleActivateUser = async (user) => {
  try {
    await updateDoc(doc(db, 'users', user.id), {
      is_suspended: false,
      activated_at: serverTimestamp(),
      activated_by: currentUser.id
    });
    
    // ADD THIS:
    await notifyAccountReactivated(user.id);
    
    toast.success('User activated');
  } catch (error) {
    toast.error('Failed to activate user');
  }
};
```

### CoSellerStores Page

#### Flag Store
```javascript
const handleFlagStore = async (store, reason) => {
  try {
    await updateDoc(doc(db, 'stores', store.id), {
      is_flagged: true,
      flag_reason: reason,
      flagged_at: serverTimestamp(),
      flagged_by: currentUser.id
    });
    
    // ADD THIS:
    await notifyStoreFlagged(store.owner_id, store.name, reason);
    
    toast.success('Store flagged');
  } catch (error) {
    toast.error('Failed to flag store');
  }
};
```

#### Remove Flag
```javascript
const handleRemoveFlag = async (store) => {
  try {
    await updateDoc(doc(db, 'stores', store.id), {
      is_flagged: false,
      unflagged_at: serverTimestamp(),
      unflagged_by: currentUser.id
    });
    
    // ADD THIS:
    await notifyStoreFlagRemoved(store.owner_id, store.name);
    
    toast.success('Store flag removed');
  } catch (error) {
    toast.error('Failed to remove flag');
  }
};
```

### OrderOversight Page

#### Update Order Status
```javascript
const handleUpdateOrderStatus = async (order, newStatus) => {
  try {
    await updateDoc(doc(db, 'orders', order.id), {
      status: newStatus,
      updated_at: serverTimestamp(),
      updated_by: currentUser.id
    });
    
    // ADD THIS:
    await notifyOrderStatusChanged(order.seller_id, order.order_number, newStatus);
    
    toast.success('Order status updated');
  } catch (error) {
    toast.error('Failed to update order status');
  }
};
```

## Available Notification Functions

### User-Specific Notifications
```javascript
// Product notifications
notifyProductApproved(sellerId, productTitle, productId)
notifyProductRejected(sellerId, productTitle, productId, reason)

// Verification notifications
notifyVerificationApproved(sellerId)
notifyVerificationRejected(sellerId, reason)

// Account notifications
notifyAccountSuspended(userId, reason)
notifyAccountReactivated(userId)

// Store notifications
notifyStoreFlagged(ownerId, storeName, reason)
notifyStoreFlagRemoved(ownerId, storeName)

// Order notifications
notifyOrderStatusChanged(sellerId, orderNumber, newStatus)
```

### Custom Notifications
```javascript
// Send custom notification to specific user
notifyUser(userId, title, description, {
  category: 'SYSTEM',
  actionType: 'VIEW_PROFILE',
  actionData: { /* custom data */ }
})

// Send broadcast to all sellers
notifyAllSellers(title, description, {
  category: 'SYSTEM',
  actionType: 'VIEW_PROFILE',
  actionData: { /* custom data */ }
})
```

## Error Handling

Always wrap notification calls in try-catch:

```javascript
try {
  // Update Firestore
  await updateDoc(...);
  
  // Send notification
  await notifyProductApproved(sellerId, productTitle, productId);
  
  toast.success('Action completed and user notified');
} catch (error) {
  console.error('Error:', error);
  
  // Notification failure shouldn't block main operation
  if (error.message.includes('notification')) {
    toast.warning('Action completed but notification failed');
  } else {
    toast.error('Action failed');
  }
}
```

## Testing

### Test Locally
1. Make an admin action in web dashboard
2. Check Firestore notifications collection
3. Verify notification document created with correct user_id
4. Open mobile app and verify notification appears

### Test with Mobile App
1. Deploy Cloud Functions: `firebase deploy --only functions`
2. Make admin action in web dashboard
3. Check mobile app for real-time notification
4. Verify badge count updates
5. Tap notification to verify deep link works

## Deployment Checklist

- [ ] Import notificationService in all dashboard pages
- [ ] Add notification calls to all admin action handlers
- [ ] Test each notification type locally
- [ ] Deploy Cloud Functions
- [ ] Test end-to-end with mobile app
- [ ] Monitor Firestore for notification creation
- [ ] Gather user feedback

## Troubleshooting

### Notifications not appearing?
1. Check Firestore notifications collection
2. Verify user_id is correct
3. Check mobile app NotificationViewModel
4. Verify Cloud Functions deployed
5. Check browser console for errors

### Notifications appearing but no badge?
1. Check NotificationViewModel in mobile app
2. Verify badge count calculation
3. Check Firestore security rules
4. Verify real-time listener is active

### Cloud Functions not triggering?
1. Verify functions deployed: `firebase functions:list`
2. Check Cloud Functions logs
3. Verify Firestore document structure matches triggers
4. Check for errors in function code

## Next Steps

1. Update ProductManagement page
2. Update SellerVerification page
3. Update UserManagement page
4. Update CoSellerStores page
5. Update OrderOversight page
6. Deploy Cloud Functions
7. Test end-to-end
8. Monitor and gather feedback

## Support

For questions or issues:
1. Review this guide
2. Check notificationService.js for available functions
3. Check Cloud Functions logs
4. Verify Firestore structure
5. Check mobile app NotificationViewModel
