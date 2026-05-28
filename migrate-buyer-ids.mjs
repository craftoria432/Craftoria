// migrate-buyer-ids.mjs
// Script to add buyer_id to existing payment records

import admin from 'firebase-admin';
import { readFileSync, existsSync } from 'fs';

// Initialize Firebase Admin
let serviceAccount;
const serviceAccountPath = './serviceAccountKey.json';
const functionsServiceAccountPath = './functions/serviceAccountKey.json';

if (existsSync(serviceAccountPath)) {
  serviceAccount = JSON.parse(readFileSync(serviceAccountPath, 'utf8'));
} else if (existsSync(functionsServiceAccountPath)) {
  serviceAccount = JSON.parse(readFileSync(functionsServiceAccountPath, 'utf8'));
} else {
  console.error('❌ ERROR: serviceAccountKey.json not found!');
  console.log('\n📝 To get your service account key:');
  console.log('   1. Go to Firebase Console > Project Settings > Service Accounts');
  console.log('   2. Click "Generate New Private Key"');
  console.log('   3. Save as serviceAccountKey.json in project root');
  console.log('   4. Run this script again\n');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function migrateBuyerIds() {
  console.log('🚀 Starting buyer_id migration...\n');
  
  try {
    // Get all payments without buyer_id
    const paymentsSnapshot = await db.collection('seller_payments').get();
    console.log(`📊 Found ${paymentsSnapshot.size} total payment records\n`);
    
    let updated = 0;
    let skipped = 0;
    let errors = 0;
    
    for (const paymentDoc of paymentsSnapshot.docs) {
      const payment = paymentDoc.data();
      
      // Skip if already has buyer_id
      if (payment.buyer_id) {
        skipped++;
        continue;
      }
      
      // Get buyer_id from order
      if (payment.order_id) {
        try {
          const orderDoc = await db.collection('orders').doc(payment.order_id).get();
          
          if (orderDoc.exists) {
            const order = orderDoc.data();
            const buyerId = order.buyer_id || order.buyerId;
            const buyerName = order.buyer_name || order.buyerName;
            
            if (buyerId) {
              // Update payment with buyer info
              await paymentDoc.ref.update({
                buyer_id: buyerId,
                buyer_name: buyerName || 'Unknown Buyer',
                updated_at: admin.firestore.FieldValue.serverTimestamp()
              });
              
              console.log(`✅ Updated payment ${paymentDoc.id.substring(0, 8)}... with buyer_id: ${buyerId.substring(0, 8)}...`);
              updated++;
            } else {
              console.log(`⚠️  Order ${payment.order_id.substring(0, 8)}... has no buyer_id`);
              errors++;
            }
          } else {
            console.log(`⚠️  Order ${payment.order_id} not found`);
            errors++;
          }
        } catch (error) {
          console.error(`❌ Error processing payment ${paymentDoc.id}:`, error.message);
          errors++;
        }
      } else {
        console.log(`⚠️  Payment ${paymentDoc.id.substring(0, 8)}... has no order_id`);
        errors++;
      }
    }
    
    console.log('\n' + '='.repeat(60));
    console.log('📊 MIGRATION SUMMARY');
    console.log('='.repeat(60));
    console.log(`✅ Updated:  ${updated}`);
    console.log(`⏭️  Skipped:  ${skipped} (already had buyer_id)`);
    console.log(`❌ Errors:   ${errors}`);
    console.log('='.repeat(60));
    
    if (updated > 0) {
      console.log('\n✅ Migration complete!');
      console.log('   Next steps:');
      console.log('   1. Run: node check-payments.mjs (to verify)');
      console.log('   2. Clear app cache and restart');
      console.log('   3. Check Payment History screen');
    }
    
  } catch (error) {
    console.error('\n❌ Migration failed:', error.message);
  }
  
  process.exit(0);
}

console.log('Craftoria - Buyer ID Migration Script');
console.log('======================================\n');
migrateBuyerIds();
