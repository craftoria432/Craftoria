// sync-orders-to-payments.mjs
// Create payment records from existing orders

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
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function syncOrdersToPayments() {
  console.log('Craftoria - Order to Payment Sync');
  console.log('==================================\n');
  console.log('🔄 Syncing orders to payment records...\n');
  
  try {
    // Get all orders
    const ordersSnapshot = await db.collection('orders').get();
    console.log(`📦 Found ${ordersSnapshot.size} total orders\n`);
    
    let created = 0;
    let skipped = 0;
    let errors = 0;
    
    for (const orderDoc of ordersSnapshot.docs) {
      const order = orderDoc.data();
      const orderId = orderDoc.id;
      
      // Check if payment already exists for this order
      const existingPayment = await db.collection('payments')
        .where('order_id', '==', orderId)
        .limit(1)
        .get();
      
      if (!existingPayment.empty) {
        console.log(`⏭️  Payment already exists for order ${orderId.substring(0, 8)}...`);
        skipped++;
        continue;
      }
      
      // Extract buyer info
      const buyerId = order.buyer_id || order.buyerId;
      const buyerName = order.buyer_name || order.buyerName || 'Unknown Buyer';
      const sellerId = order.seller_id || order.sellerId;
      const sellerName = order.seller_name || order.sellerName || 'Unknown Seller';
      const totalAmount = order.total_amount || order.totalAmount || 0;
      const status = order.status || 'PENDING';
      
      if (!buyerId || !sellerId) {
        console.log(`⚠️  Order ${orderId.substring(0, 8)}... missing buyer_id or seller_id`);
        errors++;
        continue;
      }
      
      try {
        // Create payment record
        const paymentData = {
          order_id: orderId,
          buyer_id: buyerId,
          buyer_name: buyerName,
          seller_id: sellerId,
          seller_name: sellerName,
          amount: totalAmount,
          status: status === 'DELIVERED' || status === 'COMPLETED' ? 'COMPLETED' : 'PENDING',
          payment_method: 'COD', // Default to Cash on Delivery
          created_at: order.created_at || admin.firestore.FieldValue.serverTimestamp(),
          updated_at: admin.firestore.FieldValue.serverTimestamp(),
          synced_from_order: true // Flag to indicate this was synced
        };
        
        await db.collection('payments').add(paymentData);
        
        console.log(`✅ Created payment for order ${orderId.substring(0, 8)}...`);
        console.log(`   Buyer: ${buyerName} (${buyerId.substring(0, 8)}...)`);
        console.log(`   Amount: PKR ${totalAmount}`);
        console.log('');
        
        created++;
      } catch (error) {
        console.error(`❌ Error creating payment for order ${orderId}:`, error.message);
        errors++;
      }
    }
    
    console.log('\n' + '='.repeat(60));
    console.log('📊 SYNC SUMMARY');
    console.log('='.repeat(60));
    console.log(`✅ Created:  ${created} new payment records`);
    console.log(`⏭️  Skipped:  ${skipped} (already had payments)`);
    console.log(`❌ Errors:   ${errors}`);
    console.log('='.repeat(60));
    
    if (created > 0) {
      console.log('\n✅ Sync complete!');
      console.log('   Next steps:');
      console.log('   1. Run: node check-user-payments.mjs');
      console.log('   2. Test Payment History in the app');
      console.log('   3. Clear app cache and restart');
    } else if (skipped > 0) {
      console.log('\n✅ All orders already have payment records!');
    }
    
  } catch (error) {
    console.error('\n❌ Sync failed:', error.message);
  }
  
  process.exit(0);
}

syncOrdersToPayments();
