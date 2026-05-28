#!/usr/bin/env node

/**
 * FIX: Update payment status to "completed" for orders that are already completed
 * 
 * This script handles the case where:
 * 1. Order status is already "completed"
 * 2. Payment status is still "pending"
 * 
 * Run: node fix-completed-order-payment.mjs
 */

import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';

// Initialize Firebase
const serviceAccountPath = path.join(process.cwd(), 'serviceAccountKey.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error('❌ serviceAccountKey.json not found in current directory');
  console.error('   Please save your Firebase service account key there first');
  process.exit(1);
}

const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: serviceAccount.project_id,
});

const db = admin.firestore();

async function fixCompletedOrderPayments() {
  try {
    console.log('🔍 Finding all completed orders with pending payments...\n');

    // Get all completed orders
    const ordersSnapshot = await db
      .collection('orders')
      .where('status', '==', 'COMPLETED')
      .get();

    console.log(`📋 Found ${ordersSnapshot.size} completed orders\n`);

    let updatedCount = 0;

    for (const orderDoc of ordersSnapshot.docs) {
      const orderId = orderDoc.id;
      const orderData = orderDoc.data();

      // Find payments for this order
      const paymentsSnapshot = await db
        .collection('payments')
        .where('order_id', '==', orderId)
        .get();

      for (const paymentDoc of paymentsSnapshot.docs) {
        const paymentData = paymentDoc.data();

        // If payment is still pending, update it to completed
        if (paymentData.status === 'pending') {
          console.log(`📝 Order: ${orderId}`);
          console.log(`   Payment: ${paymentDoc.id}`);
          console.log(`   Current status: ${paymentData.status}`);

          // Update payment to completed
          await paymentDoc.ref.update({
            status: 'completed',
            payment_date: paymentData.payment_date || System.currentTimeMillis(),
            updated_at: Date.now(),
          });

          console.log(`   ✅ Updated to: completed\n`);
          updatedCount++;
        }
      }
    }

    console.log(`\n✨ Fixed ${updatedCount} payments`);
    console.log('✅ All completed orders now have completed payments');

  } catch (error) {
    console.error('❌ Error fixing payments:', error);
    process.exit(1);
  } finally {
    await admin.app().delete();
  }
}

fixCompletedOrderPayments();
