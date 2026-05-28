// check-user-payments.mjs
// Check payments for a specific user UID

import admin from 'firebase-admin';
import { readFileSync, existsSync } from 'fs';
import readline from 'readline';

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
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Create readline interface for user input
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function askQuestion(query) {
  return new Promise(resolve => rl.question(query, resolve));
}

async function checkUserPayments() {
  console.log('Craftoria - User Payment Check');
  console.log('================================\n');
  
  const userId = await askQuestion('Enter the user UID (or press Enter to list all users): ');
  
  if (!userId.trim()) {
    // List all users
    console.log('\n📋 Listing all users...\n');
    try {
      const usersSnapshot = await db.collection('users').limit(20).get();
      
      console.log(`Found ${usersSnapshot.size} users:\n`);
      usersSnapshot.docs.forEach((doc, index) => {
        const user = doc.data();
        console.log(`${index + 1}. UID: ${doc.id}`);
        console.log(`   Name: ${user.name || 'N/A'}`);
        console.log(`   Email: ${user.email || 'N/A'}`);
        console.log(`   Role: ${user.role || 'N/A'}`);
        console.log('');
      });
      
      const selectedUserId = await askQuestion('\nEnter the UID from above to check payments: ');
      if (selectedUserId.trim()) {
        await checkPaymentsForUser(selectedUserId.trim());
      }
    } catch (error) {
      console.error('❌ Error listing users:', error.message);
    }
  } else {
    await checkPaymentsForUser(userId.trim());
  }
  
  rl.close();
  process.exit(0);
}

async function checkPaymentsForUser(userId) {
  console.log(`\n🔍 Checking payments for user: ${userId}\n`);
  
  try {
    // Get user info
    const userDoc = await db.collection('users').doc(userId).get();
    if (userDoc.exists) {
      const user = userDoc.data();
      console.log('👤 User Info:');
      console.log(`   Name: ${user.name || 'N/A'}`);
      console.log(`   Email: ${user.email || 'N/A'}`);
      console.log(`   Role: ${user.role || 'N/A'}`);
      console.log('');
    } else {
      console.log('⚠️  User not found in database\n');
    }
    
    // Check payments where this user is the buyer
    const paymentsSnapshot = await db.collection('payments')
      .where('buyer_id', '==', userId)
      .get();
    
    console.log(`📊 Found ${paymentsSnapshot.size} payments for this buyer:\n`);
    
    if (paymentsSnapshot.empty) {
      console.log('❌ No payments found for this user!');
      console.log('\nPossible reasons:');
      console.log('1. User has not made any purchases yet');
      console.log('2. Payments have different buyer_id format');
      console.log('3. User is a seller, not a buyer');
      
      // Check if user has any orders
      const ordersSnapshot = await db.collection('orders')
        .where('buyer_id', '==', userId)
        .get();
      
      console.log(`\n📦 Orders for this user: ${ordersSnapshot.size}`);
      
      if (ordersSnapshot.size > 0) {
        console.log('\n⚠️  User has orders but no payments!');
        console.log('   This indicates a data sync issue.');
      }
    } else {
      let totalAmount = 0;
      
      paymentsSnapshot.docs.forEach((doc, index) => {
        const payment = doc.data();
        console.log(`${index + 1}. Payment ${doc.id.substring(0, 8)}...`);
        console.log(`   Order ID: ${payment.order_id || 'N/A'}`);
        console.log(`   Amount: PKR ${payment.amount || 0}`);
        console.log(`   Status: ${payment.status || 'N/A'}`);
        console.log(`   Date: ${payment.created_at?.toDate?.() || 'N/A'}`);
        console.log('');
        
        totalAmount += payment.amount || 0;
      });
      
      console.log('='.repeat(60));
      console.log(`💰 Total Amount: PKR ${totalAmount}`);
      console.log('='.repeat(60));
    }
    
  } catch (error) {
    console.error('\n❌ Error:', error.message);
  }
}

checkUserPayments();
