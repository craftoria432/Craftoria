# Web Admin Seller Application Integration

## Current Issue
The web admin `SellerVerification.jsx` is designed to handle users with `role: 'seller'`, but our new implementation keeps users as `role: 'buyer'` with `seller_application_status: 'pending'` until admin approval.

## Required Changes

### 1. Update Web Admin Query
The current query only finds users with `role: 'seller'`:
```javascript
const q = query(collection(db, 'users'), where('role', '==', 'seller'));
```

**Need to change to:**
```javascript
// Get both seller applications and existing sellers
const q1 = query(collection(db, 'users'), 
  where('seller_application_status', '==', 'pending'));
const q2 = query(collection(db, 'users'), 
  where('role', '==', 'seller'));
```

### 2. Update Admin Actions
Current admin actions only update `verification_status`, but for pending applications, we need to:
1. Change `role` from `buyer` to `seller`
2. Update `seller_application_status` to `approved`
3. Set `verification_status` to `not_submitted`

### 3. Two-Stage Approval Process
1. **Stage 1: Application Approval** (New)
   - Admin reviews buyer's request to become seller
   - Approves/rejects the application
   - If approved: role changes to seller

2. **Stage 2: Identity Verification** (Existing)
   - Seller submits verification photo
   - Admin verifies identity
   - Approves/rejects verification

## Implementation Plan

### Option A: Separate Pages (Recommended)
- **Seller Applications**: Handle `buyer` → `seller` role changes
- **Seller Verifications**: Handle identity verification for existing sellers

### Option B: Combined Page (Current approach)
- Modify existing page to handle both application and verification stages
- Show different UI based on user status

## Database Structure After Integration

### Buyer Requesting to Become Seller
```javascript
{
  role: "buyer",
  seller_application_status: "pending",
  verification_status: "not_submitted",
  verified: false
}
```

### After Admin Approves Application
```javascript
{
  role: "seller",                    // Changed by admin
  seller_application_status: "approved",
  verification_status: "not_submitted",  // Ready for photo submission
  verified: false
}
```

### After Seller Submits Photo
```javascript
{
  role: "seller",
  seller_application_status: "approved",
  verification_status: "pending",    // Photo submitted, awaiting verification
  verified: false
}
```

### After Admin Verifies Identity
```javascript
{
  role: "seller",
  seller_application_status: "approved",
  verification_status: "approved",   // Identity verified
  verified: true
}
```