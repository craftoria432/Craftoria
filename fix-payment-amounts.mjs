#!/usr/bin/env node

/**
 * fix-payment-amounts.mjs
 * 
 * Backfills correct amounts for seller_payments records that have amount: 0
 * by reading the linked order's total_price, total_amount, or items sum.
 * 
 * Run once: node fix-payment-amounts.mjs
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

async function fixPaymentAmounts() {
  console.log('🔍 Finding payments with amount = 0...\n');

  const paymentsSnapshot = await db.collection('payments')
    .where('amount', '==', 0)
    .get();

  if (paymentsSnapshot.empty) {
    console.log('✅ No payments with amount = 0 found. All good!');
    return;
  }

  console.log(`📦 Found ${paymentsSnapshot.size} payments with amount = 0\n`);

  let fixed = 0;
  let failed = 0;

  for (const paymentDoc of paymentsSnapshot.docs) {
    const payment = paymentDoc.data();
    const orderId = payment.order_id;

    if (!orderId) {
      console.log(`⚠️  Payment ${paymentDoc.id} has no order_id, skipping`);
      failed++;
      continue;
    }

    try {
      // Fetch the linked order
      const orderDoc = await db.collection('orders').doc(orderId).get();

      if (!orderDoc.exists) {
        console.log(`⚠️  Order ${orderId} not found for payment ${paymentDoc.id}`);
        failed++;
        continue;
      }

      const order = orderDoc.data();
      let correctAmount = 0;

      // Try multiple fields in priority order
      if (order.total_price && order.total_price > 0) {
        correctAmount = order.total_price;
      } else if (order.total_amount && order.total_amount > 0) {
        correctAmount = order.total_amount;
      } else if (order.items && Array.isArray(order.items)) {
        // Sum from items
        correctAmount = order.items.reduce((sum, item) => {
          const price = item.negotiated_price || item.price || item.product_price || 0;
          const qty = item.quantity || 1;
          return sum + (price * qty);
        }, 0);
      }

      if (correctAmount > 0) {
        // Update the payment record
        await db.collection('payments').doc(paymentDoc.id).update({
          amount: correctAmount,
          updated_at: admin.firestore.FieldValue.serverTimestamp()
        });

        console.log(`✅ Fixed payment ${paymentDoc.id}: PKR 0 → PKR ${correctAmount.toFixed(2)}`);
        fixed++;
      } else {
        console.log(`⚠️  Could not determine amount for order ${orderId}, payment ${paymentDoc.id}`);
        failed++;
      }

    } catch (error) {
      console.error(`❌ Error fixing payment ${paymentDoc.id}:`, error.message);
      failed++;
    }
  }

  console.log(`\n📊 Summary:`);
  console.log(`   ✅ Fixed: ${fixed}`);
  console.log(`   ❌ Failed: ${failed}`);
  console.log(`\n🎉 Done! Payment amounts backfilled.`);
}

fixPaymentAmounts()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  });
