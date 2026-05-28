#!/usr/bin/env node

/**
 * ✅ CREATE MISSING PAYMENT RECORDS FOR EXISTING ORDERS
 * 
 * This script creates payment records for orders that don't have them yet.
 * Run this to fix the "Payment History showing nothing" issue.
 * 
 * Usage:
 *   node create-missing-payments.mjs
 */

import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore, Timestamp } from 'firebase-admin/firestore';
import { readFileSync } from 'fs';

// Initialize Firebase Admin
const serviceAccount = JSON.parse(
  readFileSync('./app/serviceAccountKey.json', 'utf8')
);

initializeApp({
  credential: cert(serviceAccount)
});

const db = getFirestore();

async function createMissingPayments() {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('🔧 CREATE MISSING PAYMENT RECORDS');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

  try {
    // Step 1: Get all orders
    console.log('📦 Step 1: Fetching all orders...');
    const ordersSnapshot = await db.collection('orders').get();
    console.log(`✅ Found ${ordersSnapshot.size} orders\n`);

    // Step 2: Get all existing payments
    console.log('💳 Step 2: Fetching existing payments...');
    const paymentsSnapshot = await db.collection('payments').get();
    const existingPaymentOrderIds = new Set(
      paymentsSnapshot.docs.map(doc => doc.data().order_id)
    );
    console.log(`✅ Found ${paymentsSnapshot.size} existing payments\n`);

    // Step 3: Find orders without payments
    console.log('🔍 Step 3: Finding orders without payments...');
    const ordersWithoutPayments = [];
    
    ordersSnapshot.docs.forEach(doc => {
      const orderId = doc.id;
      if (!existingPaymentOrderIds.has(orderId)) {
        ordersWithoutPayments.push({
          id: orderId,
          ...doc.data()
        });
      }
    });

    console.log(`⚠️  Found ${ordersWithoutPayments.length} orders without payments\n`);

    if (ordersWithoutPayments.length === 0) {
      console.log('✅ All orders have payment records. Nothing to do!');
      return;
    }

    // Step 4: Create missing payments
    console.log('💰 Step 4: Creating missing payment records...\n');
    let createdCount = 0;
    let errorCount = 0;

    for (const order of ordersWithoutPayments) {
      try {
        console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
        console.log(`📝 Processing Order: ${order.id.substring(0, 8)}...`);
        console.log(`   Buyer: ${order.buyer_name || 'Unknown'}`);
        console.log(`   Status: ${order.status || 'Unknown'}`);

        // Determine items to process
        let itemsToProcess = [];
        
        if (order.items && order.items.length > 0) {
          // New format: items array
          console.log(`   Format: New (${order.items.length} items)`);
          itemsToProcess = order.items;
        } else if (order.product_id) {
          // Legacy format: single product
          console.log(`   Format: Legacy (single product)`);
          itemsToProcess = [{
            product_id: order.product_id,
            seller_id: order.seller_id,
            seller_name: order.seller_name || 'Unknown Seller',
            product_title: order.product_title || 'Unknown Product',
            product_image: order.product_image || '',
            quantity: order.quantity || 1,
            price: order.product_price || 0
          }];
        } else {
          console.log(`   ⚠️  SKIP: No items or product data`);
          errorCount++;
          continue;
        }

        // Group items by seller
        const itemsBySeller = {};
        itemsToProcess.forEach(item => {
          const sellerId = item.seller_id;
          if (!itemsBySeller[sellerId]) {
            itemsBySeller[sellerId] = [];
          }
          itemsBySeller[sellerId].push(item);
        });

        console.log(`   Sellers: ${Object.keys(itemsBySeller).length}`);

        // Get all involved seller IDs for access control
        const involvedSellerIds = Object.keys(itemsBySeller);

        // Create payment for each seller
        for (const [sellerId, sellerItems] of Object.entries(itemsBySeller)) {
          const sellerAmount = sellerItems.reduce((sum, item) => {
            return sum + (item.price * item.quantity);
          }, 0);

          const itemsCount = sellerItems.reduce((sum, item) => {
            return sum + item.quantity;
          }, 0);

          console.log(`   💵 Seller ${sellerId.substring(0, 8)}: PKR ${sellerAmount}`);

          // Determine payment status based on order status
          let paymentStatus = 'PENDING';
          if (order.status === 'COMPLETED' || order.status === 'DELIVERED') {
            paymentStatus = 'COMPLETED';
          } else if (order.status === 'CANCELLED') {
            paymentStatus = 'CANCELLED';
          }

          // Create payment record
          const paymentData = {
            seller_id: sellerId,
            seller_name: sellerItems[0].seller_name || 'Unknown Seller',
            order_id: order.id,
            co_seller_store_id: order.co_seller_store_id || '',
            store_name: order.seller_name || 'Unknown Store',
            buyer_id: order.buyer_id || '',
            buyer_name: order.buyer_name || 'Unknown Buyer',
            amount: sellerAmount,
            payment_method: order.payment_method || 'Cash on Delivery',
            status: paymentStatus,
            items_count: itemsCount,
            items_details: sellerItems.map(item => ({
              product_id: item.product_id,
              product_title: item.product_title,
              quantity: item.quantity,
              price: item.price,
              item_total: item.price * item.quantity
            })),
            involved_seller_ids: involvedSellerIds,
            created_at: order.created_at || Timestamp.now(),
            updated_at: Timestamp.now(),
            // Migration metadata
            migration_source: 'create-missing-payments-script',
            migration_timestamp: Timestamp.now()
          };

          // Add to Firestore
          const paymentRef = await db.collection('payments').add(paymentData);
          
          // Update with ID
          await paymentRef.update({ id: paymentRef.id });

          console.log(`   ✅ Payment created: ${paymentRef.id.substring(0, 8)}`);
          createdCount++;
        }

      } catch (error) {
        console.error(`   ❌ Error processing order ${order.id}:`, error.message);
        errorCount++;
      }
    }

    console.log('\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log('📊 MIGRATION SUMMARY');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`✅ Payments created: ${createdCount}`);
    console.log(`❌ Errors: ${errorCount}`);
    console.log(`📦 Orders processed: ${ordersWithoutPayments.length}`);
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n');

    if (createdCount > 0) {
      console.log('🎉 SUCCESS! Payment records have been created.');
      console.log('💡 Buyers should now see their payment history in the app.\n');
    }

  } catch (error) {
    console.error('❌ Migration failed:', error);
    process.exit(1);
  }
}

// Run the migration
createMissingPayments()
  .then(() => {
    console.log('✅ Migration complete!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Migration failed:', error);
    process.exit(1);
  });
