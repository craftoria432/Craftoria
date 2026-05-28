# Refund Approval: Implementation Code Changes

## Overview

This document provides the exact code changes needed to implement proper payment status updates when a refund is approved.

---

## 1. Backend: Cloud Functions (functions/index.js)

### Function: Approve Refund

```javascript
/**
 * Approve a refund request (seller or admin action)
 * Updates both RefundRequest and SellerPayment records
 */
exports.approveRefund = functions.https.onCall(async (data, context) => {
    const { refundId, approverId, approverRole } = data;
    
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User not authenticated');
    }
    
    try {
        const db = admin.firestore();
        const refundRef = db.collection('refunds').doc(refundId);
        const refundDoc = await refundRef.get();
        
        if (!refundDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'Refund not found');
        }
        
        const refund = refundDoc.data();
        
        // Validate approver is seller or admin
        if (approverRole !== 'admin' && refund.seller_id !== approverId) {
            throw new functions.https.HttpsError('permission-denied', 'Not authorized to approve');
        }
        
        // ✅ STEP 1: Update RefundRequest
        const approvalStatus = approverRole === 'admin' 
            ? 'APPROVED_BY_ADMIN' 
            : 'APPROVED_BY_SELLER';
        
        await refundRef.update({
            status: approvalStatus,
            approved_by: approverId,
            approved_at: admin.firestore.FieldValue.serverTimestamp(),
            updated_at: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log(`✅ Refund ${refundId} approved by ${approverRole}`);
        
        // ✅ STEP 2: Update SellerPayment record
        const paymentRef = db.collection('payments').doc(refund.payment_id);
        const paymentDoc = await paymentRef.get();
        
        if (paymentDoc.exists) {
            await paymentRef.update({
                status: 'PROCESSING',  // ← KEY CHANGE: From COMPLETED to PROCESSING
                refund_amount: refund.refund_amount,
                refund_reason: refund.reason,
                updated_at: admin.firestore.FieldValue.serverTimestamp()
            });
            
            console.log(`✅ Payment ${refund.payment_id} status updated to PROCESSING`);
        }
        
        // ✅ STEP 3: Create notification for buyer
        await db.collection('notifications').add({
            user_id: refund.buyer_id,
            title: 'Refund Approved',
            description: `Your refund of PKR ${refund.refund_amount} has been approved and is being processed.`,
            category: 'PAYMENTS',
            action_type: 'REFUND_APPROVED',
            related_id: refundId,
            is_read: false,
            created_at: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log(`✅ Notification sent to buyer ${refund.buyer_id}`);
        
        return {
            success: true,
            message: 'Refund approved successfully',
            refundId: refundId,
            paymentId: refund.payment_id
        };
        
    } catch (error) {
        console.error('Error approving refund:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});
```

### Function: Complete Refund

```javascript
/**
 * Mark refund as completed (called after payment gateway confirms)
 * Updates both RefundRequest and SellerPayment records
 */
exports.completeRefund = functions.https.onCall(async (data, context) => {
    const { refundId, gatewayRefundId } = data;
    
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User not authenticated');
    }
    
    try {
        const db = admin.firestore();
        const refundRef = db.collection('refunds').doc(refundId);
        const refundDoc = await refundRef.get();
        
        if (!refundDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'Refund not found');
        }
        
        const refund = refundDoc.data();
        
        // ✅ STEP 1: Update RefundRequest
        await refundRef.update({
            status: 'COMPLETED',
            gateway_refund_id: gatewayRefundId,
            completed_at: admin.firestore.FieldValue.serverTimestamp(),
            updated_at: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log(`✅ Refund ${refundId} marked as COMPLETED`);
        
        // ✅ STEP 2: Update SellerPayment record
        const paymentRef = db.collection('payments').doc(refund.payment_id);
        const paymentDoc = await paymentRef.get();
        
        if (paymentDoc.exists) {
            await paymentRef.update({
                status: 'REFUNDED',  // ← KEY CHANGE: From PROCESSING to REFUNDED
                refund_date: admin.firestore.FieldValue.serverTimestamp(),
                updated_at: admin.firestore.FieldValue.serverTimestamp()
            });
            
            console.log(`✅ Payment ${refund.payment_id} status updated to REFUNDED`);
        }
        
        // ✅ STEP 3: Create notification for buyer
        await db.collection('notifications').add({
            user_id: refund.buyer_id,
            title: 'Refund Completed',
            description: `Your refund of PKR ${refund.refund_amount} has been successfully processed.`,
            category: 'PAYMENTS',
            action_type: 'REFUND_COMPLETED',
            related_id: refundId,
            is_read: false,
            created_at: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log(`✅ Notification sent to buyer ${refund.buyer_id}`);
        
        return {
            success: true,
            message: 'Refund completed successfully',
            refundId: refundId,
            paymentId: refund.payment_id
        };
        
    } catch (error) {
        console.error('Error completing refund:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});
```

### Function: Reject Refund

```javascript
/**
 * Reject a refund request (seller or admin action)
 * Keeps SellerPayment status as COMPLETED
 */
exports.rejectRefund = functions.https.onCall(async (data, context) => {
    const { refundId, approverId, approverRole, rejectionReason } = data;
    
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User not authenticated');
    }
    
    try {
        const db = admin.firestore();
        const refundRef = db.collection('refunds').doc(refundId);
        const refundDoc = await refundRef.get();
        
        if (!refundDoc.exists) {
            throw new functions.https.HttpsError('not-found', 'Refund not found');
        }
        
        const refund = refundDoc.data();
        
        // Validate approver
        if (approverRole !== 'admin' && refund.seller_id !== approverId) {
            throw new functions.https.HttpsError('permission-denied', 'Not authorized to reject');
        }
        
        // Check if this is the second rejection
        const newRejectionCount = (refund.rejection_count || 0) + 1;
        const canResubmit = newRejectionCount < 2;
        const finalDecision = newRejectionCount >= 2;
        
        // ✅ STEP 1: Update RefundRequest
        const rejectionStatus = approverRole === 'admin' 
            ? 'REJECTED_BY_ADMIN' 
            : 'REJECTED_BY_SELLER';
        
        await refundRef.update({
            status: rejectionStatus,
            rejection_count: newRejectionCount,
            can_resubmit: canResubmit,
            final_decision: finalDecision,
            approval_notes: rejectionReason,
            updated_at: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log(`✅ Refund ${refundId} rejected (count: ${newRejectionCount})`);
        
        // ✅ STEP 2: SellerPayment status remains COMPLETED (no change)
        // No update needed - payment stays as COMPLETED
        
        // ✅ STEP 3: Create notification for buyer
        const notificationTitle = finalDecision 
            ? 'Refund Request Denied' 
            : 'Refund Request Rejected';
        const notificationDesc = finalDecision
            ? 'Your refund request has been denied. You cannot submit another request for this order.'
            : 'Your refund request has been rejected. You can submit another request with more details.';
        
        await db.collection('notifications').add({
            user_id: refund.buyer_id,
            title: notificationTitle,
            description: notificationDesc,
            category: 'PAYMENTS',
            action_type: 'REFUND_REJECTED',
            related_id: refundId,
            is_read: false,
            created_at: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log(`✅ Notification sent to buyer ${refund.buyer_id}`);
        
        return {
            success: true,
            message: 'Refund rejected successfully',
            refundId: refundId,
            canResubmit: canResubmit,
            finalDecision: finalDecision
        };
        
    } catch (error) {
        console.error('Error rejecting refund:', error);
        throw new functions.https.HttpsError('internal', error.message);
    }
});
```

---

## 2. Mobile App: Update Payment History Display

### File: PaymentHistoryScreen.kt

```kotlin
@Composable
fun BuyerPaymentCard(payment: SellerPayment) {
    val statusDisplay = when {
        // ✅ NEW: Check for refund in progress
        payment.status == "processing" && payment.refundAmount > 0 -> {
            Pair("Refund Processing", Warning)
        }
        // ✅ NEW: Check for completed refund
        payment.status == "refunded" -> {
            Pair("Refunded", Success)
        }
        // Normal completed payment
        payment.status == "completed" -> {
            Pair("Completed", Success)
        }
        // Other statuses
        else -> Pair(payment.status.uppercase(), Primary)
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${payment.id.take(8).uppercase()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = formatDate(payment.getDisplayDate()),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (statusDisplay.second) {
                        Success -> Success.copy(alpha = 0.08f)
                        Warning -> Warning.copy(alpha = 0.08f)
                        else -> Primary.copy(alpha = 0.08f)
                    }
                ) {
                    Text(
                        text = statusDisplay.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusDisplay.second,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amount",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "PKR ${payment.amount.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            
            // ✅ NEW: Show refund amount if applicable
            if (payment.refundAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Warning.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Refund Amount",
                        fontSize = 12.sp,
                        color = Warning
                    )
                    Text(
                        text = "PKR ${payment.refundAmount.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Warning
                    )
                }
            }
            
            // ✅ NEW: Show refund reason if applicable
            if (payment.refundReason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reason: ${payment.refundReason}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

---

## 3. Mobile App: Verify My Orders Display

### File: MyOrdersScreen.kt (Already Correct)

The refund button logic in `OrderActionButtons` is already correct and will automatically show the right state based on the refund status:

```kotlin
when (refundState) {
    OrderRefundState.APPROVED, OrderRefundState.PROCESSING -> {
        // Show "Refund Processing" badge (blue)
        OutlinedButton(
            onClick = onViewDetails,
            modifier = Modifier.weight(1f).height(38.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3)),
            border = BorderStroke(0.5.dp, Color(0xFF2196F3)),
            shape = RoundedCornerShape(10.dp),
            enabled = false
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF2196F3)
                )
                Text(text = "Refund Processing", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    
    OrderRefundState.COMPLETED -> {
        // Show "Refund Done" badge (green)
        OutlinedButton(
            onClick = onViewDetails,
            modifier = Modifier.weight(1f).height(38.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Success),
            border = BorderStroke(0.5.dp, Success),
            shape = RoundedCornerShape(10.dp),
            enabled = false
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Success
                )
                Text(text = "Refund Done", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    
    // ... other states
}
```

---

## 4. Web Dashboard: Update Refund Management

### File: src/pages/RefundManagement.jsx (or similar)

```javascript
// When approving a refund from web dashboard
async function handleApproveRefund(refundId) {
    try {
        const response = await fetch('/api/refunds/approve', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                refundId: refundId,
                approverId: currentUser.uid,
                approverRole: currentUser.role  // 'admin' or 'seller'
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            // ✅ Refund approved - payment status now PROCESSING
            showNotification('Refund approved successfully');
            
            // Refresh refund list
            await loadRefunds();
            
            // Refresh payment history
            await loadPayments();
        }
    } catch (error) {
        console.error('Error approving refund:', error);
        showError('Failed to approve refund');
    }
}

// When completing a refund (after payment gateway callback)
async function handleCompleteRefund(refundId, gatewayRefundId) {
    try {
        const response = await fetch('/api/refunds/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                refundId: refundId,
                gatewayRefundId: gatewayRefundId
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            // ✅ Refund completed - payment status now REFUNDED
            showNotification('Refund completed successfully');
            
            // Refresh refund list
            await loadRefunds();
            
            // Refresh payment history
            await loadPayments();
        }
    } catch (error) {
        console.error('Error completing refund:', error);
        showError('Failed to complete refund');
    }
}
```

---

## 5. Firestore Security Rules Update

### File: firestore.rules

```javascript
// Allow buyers to see their payment history with refund info
match /payments/{paymentId} {
    allow read: if request.auth.uid == resource.data.buyer_id
                || request.auth.uid == resource.data.seller_id
                || hasRole('admin');
    
    // ✅ Allow backend to update payment status during refund
    allow update: if request.auth.uid == resource.data.seller_id
                  || hasRole('admin')
                  || isBackendService();
}

// Allow buyers to see refund requests
match /refunds/{refundId} {
    allow read: if request.auth.uid == resource.data.buyer_id
                || request.auth.uid == resource.data.seller_id
                || hasRole('admin');
    
    // ✅ Allow backend to update refund status
    allow update: if request.auth.uid == resource.data.seller_id
                  || hasRole('admin')
                  || isBackendService();
}
```

---

## Summary of Changes

| Component | Change | Impact |
|-----------|--------|--------|
| Cloud Functions | Add `approveRefund()` function | Updates payment status to PROCESSING |
| Cloud Functions | Add `completeRefund()` function | Updates payment status to REFUNDED |
| Cloud Functions | Add `rejectRefund()` function | Keeps payment status as COMPLETED |
| PaymentHistoryScreen | Update display logic | Shows "Refund Processing" and "Refunded" states |
| MyOrdersScreen | No changes needed | Already handles refund states correctly |
| Notifications | Send on approval and completion | Buyer gets real-time updates |
| Firestore Rules | Allow backend updates | Enables payment status changes |

---

## Testing Steps

1. **Submit Refund**: Buyer submits refund request
   - Verify: Payment shows "Completed", refund button shows "Request Refund"

2. **Approve Refund**: Seller/Admin approves refund
   - Verify: Payment shows "Refund Processing", refund button shows "Refund Processing"
   - Verify: Buyer receives notification

3. **Complete Refund**: Payment gateway confirms refund
   - Verify: Payment shows "Refunded", refund button shows "Refund Done"
   - Verify: Buyer receives notification

4. **Reject Refund**: Seller/Admin rejects refund
   - Verify: Payment shows "Completed", refund button shows "Resubmit Refund"
   - Verify: Buyer receives notification

5. **Real-time Updates**: Verify both screens update without page refresh
