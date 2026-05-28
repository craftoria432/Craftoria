#!/usr/bin/env node

/**
 * Check Payment Data Script
 * 
 * This script analyzes payment and order data to identify issues:
 * - Payments with items_count = 0
 * - Payments with amount = 0
 * - Payment method distribution
 * - Order data completeness
 * 
 * Usage:
 *   node check-payment-data.mjs [buyerId]
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

async function checkPaymentData(buyerId = null) {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🔍 Payment Data Analysis');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  try {
    // Build query
    let query = db.collection('payments');
    if (buyerId) {
      query = query.where('buyer_id', '==', buyerId);
      console.log(`📊 Analyzing payments for buyer: ${buyerId}\n`);
    } else {
      console.log(`📊 Analyzing all payments\n`);
    }

    const paymentsSnapshot = await query.get();

    if (paymentsSnapshot.empty) {
      console.log('⚠️  No payments found\n');
      return;
    }

    console.log(`📦 Total Payments: ${paymentsSnapshot.size}\n`);

    // Analysis counters
    let zeroItemsCount = 0;
    let zeroAmountCount = 0;
    const paymentMethods = {};
    const statuses = {};
    let totalAmount = 0;
    let totalItems = 0;

    // Detailed issues
    const zeroItemsPayments = [];
    const zeroAmountPayments = [];

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📋 Payment Details:\n');

    for (const paymentDoc of paymentsSnapshot.docs) {
      const payment = paymentDoc.data();
      const paymentId = paymentDoc.id;

      // Count issues
      if (payment.items_count === 0) {
        zeroItemsCount++;
        zeroItemsPayments.push({
          id: paymentId,
          orderId: payment.order_id,
          amount: payment.amount
        });
      }

      if (payment.amount === 0) {
        zeroAmountCount++;
        zeroAmountPayments.push({
          id: paymentId,
          orderId: payment.order_id,
          itemsCount: payment.items_count
        });
      }

      // Count payment methods
      const method = payment.payment_method || 'Unknown';
      paymentMethods[method] = (paymentMethods[method] || 0) + 1;

      // Count statuses
      const status = payment.status || 'Unknown';
      statuses[status] = (statuses[status] || 0) + 1;

      // Totals
      totalAmount += payment.amount || 0;
      totalItems += payment.items_count || 0;

      // Print payment details
      console.log(`💳 ${paymentId.substring(0, 8)}... | Order: ${payment.order_id?.substring(0, 8)}...`);
      console.log(`   Amount: PKR ${payment.amount || 0}`);
      console.log(`   Items: ${payment.items_count || 0}`);
      console.log(`   Method: ${payment.payment_method || 'Unknown'}`);
      console.log(`   Status: ${payment.status || 'Unknown'}`);
      console.log('');
    }

    // Print summary
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 Summary:\n');

    console.log('💰 Financial:');
    console.log(`   Total Amount: PKR ${totalAmount.toFixed(2)}`);
    console.log(`   Average Amount: PKR ${(totalAmount / paymentsSnapshot.size).toFixed(2)}`);
    console.log('');

    console.log('📦 Items:');
    console.log(`   Total Items: ${totalItems}`);
    console.log(`   Average Items: ${(totalItems / paymentsSnapshot.size).toFixed(2)}`);
    console.log('');

    console.log('💳 Payment Methods:');
    Object.entries(paymentMethods).forEach(([method, count]) => {
      const percentage = ((count / paymentsSnapshot.size) * 100).toFixed(1);
      console.log(`   ${method}: ${count} (${percentage}%)`);
    });
    console.log('');

    console.log('📊 Status Distribution:');
    Object.entries(statuses).forEach(([status, count]) => {
      const percentage = ((count / paymentsSnapshot.size) * 100).toFixed(1);
      console.log(`   ${status}: ${count} (${percentage}%)`);
    });
    console.log('');

    // Issues
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('⚠️  Issues Found:\n');

    if (zeroItemsCount > 0) {
      console.log(`❌ Payments with 0 items: ${zeroItemsCount}`);
      console.log('   Details:');
      zeroItemsPayments.slice(0, 5).forEach(p => {
        console.log(`   - Payment ${p.id.substring(0, 8)}... (Order: ${p.orderId.substring(0, 8)}..., Amount: PKR ${p.amount})`);
      });
      if (zeroItemsPayments.length > 5) {
        console.log(`   ... and ${zeroItemsPayments.length - 5} more`);
      }
      console.log('');
    } else {
      console.log('✅ No payments with 0 items\n');
    }

    if (zeroAmountCount > 0) {
      console.log(`❌ Payments with 0 amount: ${zeroAmountCount}`);
      console.log('   Details:');
      zeroAmountPayments.slice(0, 5).forEach(p => {
        console.log(`   - Payment ${p.id.substring(0, 8)}... (Order: ${p.orderId.substring(0, 8)}..., Items: ${p.itemsCount})`);
      });
      if (zeroAmountPayments.length > 5) {
        console.log(`   ... and ${zeroAmountPayments.length - 5} more`);
      }
      console.log('');
    } else {
      console.log('✅ No payments with 0 amount\n');
    }

    // Recommendations
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('💡 Recommendations:\n');

    if (zeroItemsCount > 0) {
      console.log('🔧 Run fix-payment-items-count.mjs to fix 0 items issue');
    }

    if (zeroAmountCount > 0) {
      console.log('🔧 Run fix-payment-amounts.mjs to fix 0 amount issue');
    }

    if (zeroItemsCount === 0 && zeroAmountCount === 0) {
      console.log('✅ All payments look good! No fixes needed.');
    }

    console.log('');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

// Get buyer ID from command line args
const buyerId = process.argv[2] || null;

// Run the check
checkPaymentData(buyerId)
  .then(() => {
    console.log('✅ Analysis complete\n');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Analysis failed:', error);
    process.exit(1);
  });
