// check-payments.mjs
// Quick script to check if payments have buyer_id

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

async function checkPayments() {
  console.log('🔍 Checking payment records...\n');
  
  try {
    const paymentsSnapshot = await db.collection('seller_payments').limit(10).get();
    console.log(`📊 Checking first ${paymentsSnapshot.size} payments:\n`);
    
    let withBuyerId = 0;
    let withoutBuyerId = 0;
    
    paymentsSnapshot.docs.forEach((doc) => {
      const payment = doc.data();
      const hasBuyerId = !!payment.buyer_id;
      
      console.log(`Payment ${doc.id.substring(0, 8)}...`);
      console.log(`  Order ID: ${payment.order_id || 'N/A'}`);
      console.log(`  Buyer ID: ${payment.buyer_id || '❌ MISSING'}`);
      console.log(`  Buyer Name: ${payment.buyer_name || '❌ MISSING'}`);
      console.log(`  Amount: PKR ${payment.amount || 0}`);
      console.log(`  Status: ${payment.status || 'N/A'}`);
      console.log('');
      
      if (hasBuyerId) {
        withBuyerId++;
      } else {
        withoutBuyerId++;
      }
    });
    
    console.log('='.repeat(60));
    console.log('📊 SUMMARY');
    console.log('='.repeat(60));
    console.log(`✅ With buyer_id:    ${withBuyerId}`);
    console.log(`❌ Without buyer_id: ${withoutBuyerId}`);
    console.log('='.repeat(60));
    
    if (withoutBuyerId > 0) {
      console.log('\n⚠️  Some payments are missing buyer_id');
      console.log('   Run: node migrate-buyer-ids.mjs');
    } else {
      console.log('\n✅ All checked payments have buyer_id!');
      console.log('   If Payment History still shows PKR 0:');
      console.log('   1. Check if buyer_id matches current user UID');
      console.log('   2. Clear app cache and restart');
      console.log('   3. Deploy Firestore rules: firebase deploy --only firestore:rules');
    }
    
  } catch (error) {
    console.error('\n❌ Error:', error.message);
  }
  
  process.exit(0);
}

console.log('Craftoria - Payment Check Script');
console.log('=================================\n');
checkPayments();
