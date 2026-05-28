#!/usr/bin/env node

/**
 * ✅ CHECK FOR ORDERS WITHOUT PAYMENT RECORDS
 * 
 * This script checks which orders are missing payment records.
 * Run this first to diagnose the issue before running the fix script.
 * 
 * Usage:
 *   node check-missing-payments.mjs [buyerId]
 */

import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { readFileSync } from 'fs';

// Initialize Firebase Admin
const serviceAccount = JSON.parse(
  readFileSync('./app/serviceAccountKey.json', 'utf8')
);

initializeApp({
  credential: cert(serviceAccount)
});

const db = getFirestore();

async function checkMissingPayments(specificBuyerId = null) {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🔍 CHECK FOR MISSING PAYMENT RECORDS');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  try {
    // Get orders (optionally filtered by buyer)
    let ordersQuery = db.collection('orders');
    if (specificBuyerId) {
      console.log(`🎯 Filtering for buyer: ${specificBuyerId}\n`);
      ordersQuery = ordersQuery.where('buyer_id', '==', specificBuyerId);
    }

    const ordersSnapshot = await ordersQuery.get();
    console.log(`📦 Found ${ordersSnapshot.size} orders\n`);

    // Get all payments
    const paymentsSnapshot = await db.collection('seller_payments').get();
    const paymentsByOrderId = {};
    
    paymentsSnapshot.docs.forEach(doc => {
      const data = doc.data();
      const orderId = data.order_id;
      if (!paymentsByOrderId[orderId]) {
        paymentsByOrderId[orderId] = [];
      }
      paymentsByOrderId[orderId].push({
        id: doc.id,
        ...data
      });
    });

    console.log(`💳 Found ${paymentsSnapshot.size} payment records\n`);

    // Check each order
    let ordersWithPayments = 0;
    let ordersWithoutPayments = 0;
    const missingPaymentOrders = [];

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 ORDER ANALYSIS');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    ordersSnapshot.docs.forEach(doc => {
      const order = { id: doc.id, ...doc.data() };
      const payments = paymentsByOrderId[order.id] || [];

      if (payments.length > 0) {
        ordersWithPayments++;
        console.log(`✅ Order ${order.id.substring(0, 8)}: ${payments.length} payment(s)`);
        console.log(`   Buyer: ${order.buyer_name || 'Unknown'}`);
        console.log(`   Status: ${order.status || 'Unknown'}`);
        console.log(`   Amount: PKR ${order.total_price || order.product_price || 0}`);
        payments.forEach(p => {
          console.log(`   💰 Payment: PKR ${p.amount} (${p.status})`);
        });
      } else {
        ordersWithoutPayments++;
        missingPaymentOrders.push(order);
        console.log(`❌ Order ${order.id.substring(0, 8)}: NO PAYMENTS`);
        console.log(`   Buyer: ${order.buyer_name || 'Unknown'} (${order.buyer_id || 'Unknown'})`);
        console.log(`   Status: ${order.status || 'Unknown'}`);
        console.log(`   Amount: PKR ${order.total_price || order.product_price || 0}`);
        console.log(`   Created: ${order.created_at ? new Date(order.created_at.toMillis()).toLocaleString() : 'Unknown'}`);
      }
      console.log('');
    });

    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📈 SUMMARY');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`Total Orders: ${ordersSnapshot.size}`);
    console.log(`✅ Orders with payments: ${ordersWithPayments}`);
    console.log(`❌ Orders without payments: ${ordersWithoutPayments}`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    if (ordersWithoutPayments > 0) {
      console.log('⚠️  ISSUE DETECTED!');
      console.log(`${ordersWithoutPayments} order(s) are missing payment records.\n`);
      console.log('💡 To fix this, run:');
      console.log('   node create-missing-payments.mjs\n');

      // Show buyer breakdown
      const buyerOrderCounts = {};
      missingPaymentOrders.forEach(order => {
        const buyerId = order.buyer_id || 'Unknown';
        const buyerName = order.buyer_name || 'Unknown';
        const key = `${buyerName} (${buyerId.substring(0, 8)})`;
        buyerOrderCounts[key] = (buyerOrderCounts[key] || 0) + 1;
      });

      console.log('👥 Affected Buyers:');
      Object.entries(buyerOrderCounts).forEach(([buyer, count]) => {
        console.log(`   ${buyer}: ${count} order(s)`);
      });
      console.log('');
    } else {
      console.log('✅ All orders have payment records!');
      console.log('   No action needed.\n');
    }

  } catch (error) {
    console.error('❌ Check failed:', error);
    process.exit(1);
  }
}

// Get buyer ID from command line argument
const buyerId = process.argv[2];

// Run the check
checkMissingPayments(buyerId)
  .then(() => {
    console.log('✅ Check complete!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Check failed:', error);
    process.exit(1);
  });
