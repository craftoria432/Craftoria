#!/usr/bin/env node

/**
 * Fix Payment Items Count Script
 * 
 * This script fixes payments that have items_count = 0 by fetching
 * the actual item count from the corresponding order.
 * 
 * Usage:
 *   node fix-payment-items-count.mjs
 */

import admin from 'firebase-admin';
import { readFileSync } from 'fs';

// Initialize Firebase Admin
const serviceAccount = JSON.parse(
  readFileSync('./app/serviceAccountKey.json', 'utf8')
);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixPaymentItemsCounts() {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🔧 Fixing Payment Items Counts');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  try {
    // Get all payments with items_count = 0
    const paymentsSnapshot = await db.collection('seller_payments')
      .where('items_count', '==', 0)
      .get();

    if (paymentsSnapshot.empty) {
      console.log('✅ No payments found with items_count = 0');
      console.log('   All payments have correct item counts!\n');
      return;
    }

    console.log(`📊 Found ${paymentsSnapshot.size} payments with items_count = 0\n`);

    let fixedCount = 0;
    let errorCount = 0;

    for (const paymentDoc of paymentsSnapshot.docs) {
      const payment = paymentDoc.data();
      const paymentId = paymentDoc.id;
      const orderId = payment.order_id;

      console.log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
      console.log(`💳 Payment: ${paymentId.substring(0, 8)}...`);
      console.log(`📦 Order: ${orderId.substring(0, 8)}...`);

      try {
        // Fetch the order
        const orderDoc = await db.collection('orders').doc(orderId).get();

        if (!orderDoc.exists) {
          console.log(`⚠️  Order not found: ${orderId}`);
          errorCount++;
          continue;
        }

        const order = orderDoc.data();

        // Calculate correct items count
        let correctItemsCount = 0;

        if (order.items && Array.isArray(order.items) && order.items.length > 0) {
          // New format: sum quantities from items array
          correctItemsCount = order.items.reduce((sum, item) => sum + (item.quantity || 0), 0);
          console.log(`📦 New format: ${order.items.length} items, total quantity: ${correctItemsCount}`);
        } else if (order.quantity) {
          // Legacy format: use single quantity field
          correctItemsCount = order.quantity;
          console.log(`📦 Legacy format: quantity = ${correctItemsCount}`);
        } else {
          console.log(`⚠️  No items or quantity found in order`);
          errorCount++;
          continue;
        }

        // Update payment
        await paymentDoc.ref.update({
          items_count: correctItemsCount,
          updated_at: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log(`✅ Fixed: items_count = ${correctItemsCount}`);
        fixedCount++;

      } catch (error) {
        console.log(`❌ Error processing payment ${paymentId}:`, error.message);
        errorCount++;
      }
    }

    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 Summary:');
    console.log(`   ✅ Fixed: ${fixedCount} payments`);
    console.log(`   ❌ Errors: ${errorCount} payments`);
    console.log(`   📦 Total: ${paymentsSnapshot.size} payments`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  } catch (error) {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  }
}

// Run the fix
fixPaymentItemsCounts()
  .then(() => {
    console.log('✅ Script completed successfully\n');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
