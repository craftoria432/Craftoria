# Firestore Index Deployment - Quick Fix

## Issue Fixed
The "FAILED PRECONDITION" error in the payments screen was caused by missing Firestore composite indexes.

## Required Index Added
```json
{
  "collectionGroup": "seller_payments",
  "queryScope": "COLLECTION", 
  "fields": [
    {
      "fieldPath": "co_seller_store_id",
      "order": "ASCENDING"
    },
    {
      "fieldPath": "created_at", 
      "order": "ASCENDING"
    }
  ]
}
```

## Deployment Commands

### 1. Deploy Indexes
```bash
firebase deploy --only firestore:indexes
```

### 2. Check Status
```bash
firebase firestore:indexes
```

### 3. Monitor in Console
- Go to Firebase Console > Firestore > Indexes
- Wait for index status to change from "Building" to "Enabled"
- Usually takes 2-5 minutes for small datasets

## Verification
After deployment, the payments screen should load without errors and display:
- Revenue summary cards
- Payment list with filtering
- No more "FAILED PRECONDITION" errors

## Alternative: Manual Creation
If CLI deployment fails, create manually in Firebase Console:
1. Go to Firestore > Indexes > Composite
2. Click "Create Index"
3. Collection: `seller_payments`
4. Add fields:
   - `co_seller_store_id` (Ascending)
   - `created_at` (Ascending)
5. Click "Create"

The payment system will be fully functional once the index is active.