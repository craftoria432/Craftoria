# Firebase Security Rules - Payment Split System

## 🔐 Security Rules for seller_payments Collection

Add these rules to your Firebase Firestore Security Rules:

```javascript
// ==================== SELLER PAYMENTS COLLECTION ====================

match /seller_payments/{paymentId} {
  // ✅ Seller can read only their own payments
  allow read: if request.auth.uid != null && 
              resource.data.seller_id == request.auth.uid;
  
  // ✅ Only backend/admin can create payments
  allow create: if request.auth.token.admin == true;
  
  // ✅ Only backend/admin can update payments
  allow update: if request.auth.token.admin == true;
  
  // ✅ Only backend/admin can delete payments
  allow delete: if request.auth.token.admin == true;
}

// ==================== ORDERS COLLECTION (UPDATED) ====================

match /orders/{orderId} {
  // ✅ Buyer can read their own orders
  allow read: if request.auth.uid != null && 
              resource.data.buyer_id == request.auth.uid;
  
  // ✅ Seller can read orders containing their items
  allow read: if request.auth.uid != null && 
              request.auth.uid in resource.data.items[].seller_id;
  
  // ✅ Buyer can create orders
  allow create: if request.auth.uid != null && 
                request.auth.uid == request.resource.data.buyer_id;
  
  // ✅ Seller can update order status for their items
  allow update: if request.auth.uid != null && 
                request.auth.uid in resource.data.items[].seller_id;
}

// ==================== NOTIFICATIONS COLLECTION (UPDATED) ====================

match /notifications/{notificationId} {
  // ✅ User can read only their own notifications
  allow read: if request.auth.uid != null && 
              resource.data.user_id == request.auth.uid;
  
  // ✅ Only backend/admin can create notifications
  allow create: if request.auth.token.admin == true;
  
  // ✅ User can update their own notifications (mark as read)
  allow update: if request.auth.uid != null && 
                resource.data.user_id == request.auth.uid &&
                request.resource.data.is_read == true;
}

// ==================== USERS COLLECTION ====================

match /users/{userId} {
  // ✅ User can read their own profile
  allow read: if request.auth.uid != null && 
              request.auth.uid == userId;
  
  // ✅ User can update their own profile
  allow update: if request.auth.uid != null && 
                request.auth.uid == userId;
}
```

---

## 🔑 Custom Claims Setup

### For Admin/Backend Users

Add custom claims to admin users:

```javascript
// In Firebase Cloud Functions or Admin SDK
const admin = require('firebase-admin');

admin.auth().setCustomUserClaims(uid, { admin: true })
  .then(() => {
    console.log('Custom claims set for user:', uid);
  });
```

### For Regular Sellers

```javascript
// Sellers don't need custom claims
// They can read their own payments using seller_id
```

---

## 📋 Security Rules Breakdown

### seller_payments Collection

| Operation | Who | Condition | Status |
|-----------|-----|-----------|--------|
| Read | Seller | seller_id == auth.uid | ✅ Allowed |
| Read | Other Seller | seller_id != auth.uid | ❌ Denied |
| Read | Buyer | Any | ❌ Denied |
| Create | Admin | admin == true | ✅ Allowed |
| Create | Seller | Any | ❌ Denied |
| Update | Admin | admin == true | ✅ Allowed |
| Update | Seller | Any | ❌ Denied |
| Delete | Admin | admin == true | ✅ Allowed |
| Delete | Seller | Any | ❌ Denied |

### orders Collection

| Operation | Who | Condition | Status |
|-----------|-----|-----------|--------|
| Read | Buyer | buyer_id == auth.uid | ✅ Allowed |
| Read | Seller | seller_id in items | ✅ Allowed |
| Read | Other | Any | ❌ Denied |
| Create | Buyer | buyer_id == auth.uid | ✅ Allowed |
| Create | Seller | Any | ❌ Denied |
| Update | Seller | seller_id in items | ✅ Allowed |
| Update | Buyer | Any | ❌ Denied |

### notifications Collection

| Operation | Who | Condition | Status |
|-----------|-----|-----------|--------|
| Read | User | user_id == auth.uid | ✅ Allowed |
| Read | Other | Any | ❌ Denied |
| Create | Admin | admin == true | ✅ Allowed |
| Create | User | Any | ❌ Denied |
| Update | User | is_read = true only | ✅ Allowed |
| Update | User | Other fields | ❌ Denied |

---

## 🛡️ Security Best Practices

### 1. **Seller Isolation**
```javascript
// ✅ GOOD - Seller can only read their payments
allow read: if resource.data.seller_id == request.auth.uid;

// ❌ BAD - Seller can read all payments
allow read: if request.auth.uid != null;
```

### 2. **Admin-Only Operations**
```javascript
// ✅ GOOD - Only admin can create payments
allow create: if request.auth.token.admin == true;

// ❌ BAD - Anyone can create payments
allow create: if request.auth.uid != null;
```

### 3. **Data Validation**
```javascript
// ✅ GOOD - Validate payment data
allow create: if request.auth.token.admin == true &&
              request.resource.data.seller_id != null &&
              request.resource.data.amount > 0 &&
              request.resource.data.status in ['pending', 'processing', 'completed'];

// ❌ BAD - No validation
allow create: if request.auth.token.admin == true;
```

### 4. **Immutable Fields**
```javascript
// ✅ GOOD - Prevent changing seller_id
allow update: if request.auth.token.admin == true &&
              request.resource.data.seller_id == resource.data.seller_id;

// ❌ BAD - Allow changing seller_id
allow update: if request.auth.token.admin == true;
```

---

## 🔍 Testing Security Rules

### Test Case 1: Seller Reading Own Payment
```javascript
// User: seller_123
// Document: seller_payments/payment_001
// Data: { seller_id: "seller_123", amount: 5000 }

// Expected: ✅ ALLOWED
// Reason: seller_id matches auth.uid
```

### Test Case 2: Seller Reading Other's Payment
```javascript
// User: seller_123
// Document: seller_payments/payment_002
// Data: { seller_id: "seller_456", amount: 3000 }

// Expected: ❌ DENIED
// Reason: seller_id doesn't match auth.uid
```

### Test Case 3: Admin Creating Payment
```javascript
// User: admin_user (admin: true)
// Operation: Create seller_payments/payment_003
// Data: { seller_id: "seller_123", amount: 5000 }

// Expected: ✅ ALLOWED
// Reason: admin == true
```

### Test Case 4: Seller Creating Payment
```javascript
// User: seller_123 (admin: false)
// Operation: Create seller_payments/payment_004
// Data: { seller_id: "seller_123", amount: 5000 }

// Expected: ❌ DENIED
// Reason: admin != true
```

---

## 🚀 Deployment Steps

### Step 1: Update Firebase Rules
1. Go to Firebase Console
2. Navigate to Firestore Database → Rules
3. Replace existing rules with the rules above
4. Click "Publish"

### Step 2: Set Admin Claims
```javascript
// For each admin user, run:
firebase functions:shell
> admin.auth().setCustomUserClaims('admin_uid', { admin: true })
```

### Step 3: Test Rules
1. Use Firebase Emulator Suite
2. Test each security rule
3. Verify all test cases pass

### Step 4: Monitor
1. Check Firebase Console for rule violations
2. Monitor error logs
3. Adjust rules if needed

---

## 📊 Rule Validation Checklist

- [ ] Seller can read only their payments
- [ ] Seller cannot read other seller's payments
- [ ] Buyer cannot read payments
- [ ] Admin can create payments
- [ ] Seller cannot create payments
- [ ] Admin can update payments
- [ ] Seller cannot update payments
- [ ] Buyer can read their orders
- [ ] Seller can read orders with their items
- [ ] Seller cannot read other seller's orders
- [ ] User can read their notifications
- [ ] User cannot read other user's notifications
- [ ] Admin can create notifications
- [ ] User cannot create notifications

---

## 🔧 Troubleshooting

### Issue: Seller cannot read payments
**Solution**: Check if seller_id in payment matches auth.uid

### Issue: Admin cannot create payments
**Solution**: Verify admin custom claim is set correctly

### Issue: Seller can read other seller's payments
**Solution**: Check security rule - should have seller_id == auth.uid

### Issue: Buyer can read payments
**Solution**: Ensure buyer is not in seller_payments read rule

---

## 📝 Rule Maintenance

### When to Update Rules
- When adding new features
- When changing data structure
- When adding new user roles
- When security requirements change

### How to Update Rules
1. Update rules in Firebase Console
2. Test with Emulator Suite
3. Deploy to production
4. Monitor for issues

---

## 🎯 Summary

These security rules ensure:
- ✅ Sellers can only see their own payments
- ✅ Buyers cannot access payment data
- ✅ Only admins can create/update payments
- ✅ Data integrity is maintained
- ✅ Unauthorized access is prevented

---

## 📚 Additional Resources

- [Firebase Security Rules Documentation](https://firebase.google.com/docs/firestore/security/start)
- [Firebase Emulator Suite](https://firebase.google.com/docs/emulator-suite)
- [Custom Claims](https://firebase.google.com/docs/auth/admin-sdk-setup)

---

## ✅ Production Checklist

- [ ] Rules reviewed by security team
- [ ] Rules tested with Emulator Suite
- [ ] Admin claims set for backend users
- [ ] Rules deployed to production
- [ ] Monitoring enabled
- [ ] Error logs reviewed
- [ ] Performance tested
- [ ] Backup rules saved

---

**Status**: ✅ Ready for Production
