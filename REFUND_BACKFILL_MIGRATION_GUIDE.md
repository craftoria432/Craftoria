# REFUND BACKFILL MIGRATION GUIDE

## Overview
This guide provides instructions for backfilling existing refunds with missing `buyer_name` and `requested_at` fields.

---

## Option 1: Firestore Console (Manual - Small Dataset)

### For Each Refund Document
1. Open Firestore Console → `refunds` collection
2. Click on a refund document with empty `buyer_name`
3. Click "Edit" button
4. Add/update fields:
   - `buyer_name`: Look up from the order or payment record
   - `requested_at`: Use `created_at` value if not available
5. Click "Save"

---

## Option 2: Cloud Function (Automated - Recommended)

### Create Migration Cloud Function

**File**: `functions/migrateRefundFields.js`

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

const db = admin.firestore();

exports.migrateRefundFields = functions.https.onCall(async (data, context) => {
  // Check admin privileges
  if (!context.auth || !context.auth.token.admin) {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Only admins can run migrations'
    );
  }

  try {
    const refundsRef = db.collection('refunds');
    const snapshot = await refundsRef.get();
    
    let updated = 0;
    let errors = 0;

    for (const doc of snapshot.docs) {
      const refund = doc.data();
      const updates = {};
      let needsUpdate = false;

      // Check if buyer_name is missing
      if (!refund.buyer_name || refund.buyer_name.trim() === '') {
        try {
          // Fetch buyer name from users collection
          const buyerDoc = await db.collection('users').doc(refund.buyer_id).get();
          const buyerName = buyerDoc.data()?.name || buyerDoc.data()?.full_name || 'Unknown Buyer';
          updates.buyer_name = buyerName;
          needsUpdate = true;
        } catch (e) {
          console.error(`Error fetching buyer ${refund.buyer_id}:`, e);
          errors++;
        }
      }

      // Check if requested_at is missing
      if (!refund.requested_at || refund.requested_at === 0) {
        // Use created_at as fallback
        updates.requested_at = refund.created_at || Date.now();
        needsUpdate = true;
      }

      // Update document if needed
      if (needsUpdate) {
        try {
          await db.collection('refunds').doc(doc.id).update(updates);
          updated++;
          console.log(`Updated refund ${doc.id}:`, updates);
        } catch (e) {
          console.error(`Error updating refund ${doc.id}:`, e);
          errors++;
        }
      }
    }

    return {
      success: true,
      message: `Migration complete. Updated: ${updated}, Errors: ${errors}`,
      updated,
      errors
    };
  } catch (error) {
    console.error('Migration failed:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});
```

### Deploy Function
```bash
cd functions
firebase deploy --only functions:migrateRefundFields
```

### Run Migration
```javascript
// From web console or admin panel
const migrateRefundFields = firebase.functions().httpsCallable('migrateRefundFields');
migrateRefundFields()
  .then(result => console.log('Migration result:', result.data))
  .catch(error => console.error('Migration error:', error));
```

---

## Option 3: Node.js Script (Local - Full Control)

### Create Script

**File**: `scripts/migrateRefunds.mjs`

```javascript
import admin from 'firebase-admin';
import fs from 'fs';

// Initialize Firebase
const serviceAccount = JSON.parse(fs.readFileSync('./serviceAccountKey.json', 'utf8'));
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://your-project.firebaseio.com'
});

const db = admin.firestore();

async function migrateRefunds() {
  console.log('🔄 Starting refund migration...');
  
  try {
    const refundsRef = db.collection('refunds');
    const snapshot = await refundsRef.get();
    
    let updated = 0;
    let skipped = 0;
    let errors = 0;

    console.log(`📊 Found ${snapshot.size} refunds to process`);

    for (const doc of snapshot.docs) {
      const refund = doc.data();
      const updates = {};
      let needsUpdate = false;

      // Check buyer_name
      if (!refund.buyer_name || refund.buyer_name.trim() === '') {
        try {
          const buyerDoc = await db.collection('users').doc(refund.buyer_id).get();
          const buyerName = buyerDoc.data()?.name || buyerDoc.data()?.full_name || 'Unknown Buyer';
          updates.buyer_name = buyerName;
          needsUpdate = true;
          console.log(`  ✅ Fetched buyer name: ${buyerName}`);
        } catch (e) {
          console.error(`  ❌ Error fetching buyer ${refund.buyer_id}:`, e.message);
          errors++;
        }
      }

      // Check requested_at
      if (!refund.requested_at || refund.requested_at === 0) {
        updates.requested_at = refund.created_at || Date.now();
        needsUpdate = true;
        console.log(`  ✅ Set requested_at to ${updates.requested_at}`);
      }

      // Update if needed
      if (needsUpdate) {
        try {
          await db.collection('refunds').doc(doc.id).update(updates);
          updated++;
          console.log(`✅ Updated refund ${doc.id}`);
        } catch (e) {
          console.error(`❌ Error updating refund ${doc.id}:`, e.message);
          errors++;
        }
      } else {
        skipped++;
      }
    }

    console.log('\n📈 Migration Summary:');
    console.log(`  ✅ Updated: ${updated}`);
    console.log(`  ⏭️  Skipped: ${skipped}`);
    console.log(`  ❌ Errors: ${errors}`);
    console.log('✅ Migration complete!');

  } catch (error) {
    console.error('❌ Migration failed:', error);
  } finally {
    process.exit(0);
  }
}

migrateRefunds();
```

### Run Script
```bash
node scripts/migrateRefunds.mjs
```

---

## Option 4: Firestore Batch Update (Advanced)

### Using Firestore Admin SDK

```javascript
const admin = require('firebase-admin');
const db = admin.firestore();

async function batchUpdateRefunds() {
  const batch = db.batch();
  const refundsRef = db.collection('refunds');
  const snapshot = await refundsRef.get();

  let count = 0;
  for (const doc of snapshot.docs) {
    const refund = doc.data();
    const updates = {};

    if (!refund.buyer_name) {
      const buyerDoc = await db.collection('users').doc(refund.buyer_id).get();
      updates.buyer_name = buyerDoc.data()?.name || 'Unknown';
    }

    if (!refund.requested_at) {
      updates.requested_at = refund.created_at;
    }

    if (Object.keys(updates).length > 0) {
      batch.update(doc.ref, updates);
      count++;

      // Firestore batch limit is 500
      if (count % 500 === 0) {
        await batch.commit();
        console.log(`Committed ${count} updates`);
      }
    }
  }

  if (count % 500 !== 0) {
    await batch.commit();
  }

  console.log(`Total updates: ${count}`);
}

batchUpdateRefunds().catch(console.error);
```

---

## Verification After Migration

### Check Firestore
```javascript
// Query refunds with missing fields
db.collection('refunds')
  .where('buyer_name', '==', '')
  .get()
  .then(snapshot => {
    console.log(`Refunds with empty buyer_name: ${snapshot.size}`);
  });

db.collection('refunds')
  .where('requested_at', '==', 0)
  .get()
  .then(snapshot => {
    console.log(`Refunds with empty requested_at: ${snapshot.size}`);
  });
```

### Check Web Dashboard
1. Navigate to Refunds page
2. Verify "Buyer" column now shows names
3. Verify "Requested" column now shows dates
4. Check a few refunds to confirm data accuracy

---

## Rollback Plan

If migration causes issues:

### Option 1: Restore from Backup
```bash
gcloud firestore export gs://your-bucket/backup-name
```

### Option 2: Manual Revert
```javascript
// Clear the migrated fields
db.collection('refunds').doc(refundId).update({
  buyer_name: '',
  requested_at: 0
});
```

---

## Recommended Approach

1. **First**: Deploy the mobile app fix (RefundModels.kt)
2. **Test**: Create new refunds and verify they have buyer_name and requested_at
3. **Then**: Run backfill migration for existing refunds
4. **Verify**: Check web dashboard displays all values correctly

---

## Timeline

- **Immediate**: Deploy mobile app fix
- **Within 24 hours**: Run backfill migration
- **Monitor**: Check for any issues in production

---

## Support

If migration fails:
1. Check Firestore logs for errors
2. Verify service account has proper permissions
3. Check that users collection has buyer records
4. Review error messages in console

---

**Status**: Ready to use when needed
**Last Updated**: May 10, 2026
