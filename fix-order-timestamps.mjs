#!/usr/bin/env node

import admin from 'firebase-admin';
import { readFileSync } from 'fs';

// Initialize Firebase Admin
const serviceAccount = JSON.parse(
  readFileSync('./serviceAccountKey.json', 'utf8')
);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

console.log('\n🔧 Craftoria - Fix Order Timestamps');
console.log('====================================\n');

// Timestamp fields to convert
const TIMESTAMP_FIELDS = [
  'created_at',
  'updated_at',
  'order_placed_at',
  'processing_at',
  'shipped_at',
  'delivered_at',
  'cancelled_at',
  'estimated_delivery',
  'expected_delivery_date'
];

async function fixOrderTimestamps() {
  try {
    console.log('📦 Fetching all orders...\n');
    
    const ordersSnapshot = await db.collection('orders').get();
    
    if (ordersSnapshot.empty) {
      console.log('❌ No orders found');
      return;
    }

    console.log(`Found ${ordersSnapshot.docs.length} orders\n`);
    console.log('─'.repeat(60));

    let fixedCount = 0;
    let skippedCount = 0;
    let errorCount = 0;

    for (const orderDoc of ordersSnapshot.docs) {
      const orderId = orderDoc.id;
      const orderData = orderDoc.data();
      
      console.log(`\n📄 Order: ${orderId.substring(0, 8)}...`);
      
      const updates = {};
      let hasTimestamps = false;

      // Check each timestamp field
      for (const field of TIMESTAMP_FIELDS) {
        const value = orderData[field];
        
        if (value && value instanceof admin.firestore.Timestamp) {
          const milliseconds = value.toMillis();
          updates[field] = milliseconds;
          hasTimestamps = true;
          console.log(`  ✅ ${field}: Timestamp → ${milliseconds}`);
        } else if (value && typeof value === 'number') {
          console.log(`  ⏭️  ${field}: Already Long (${value})`);
        }
      }

      // Update document if it has Timestamp fields
      if (hasTimestamps) {
        try {
          await orderDoc.ref.update(updates);
          fixedCount++;
          console.log(`  ✅ Updated successfully`);
        } catch (error) {
          errorCount++;
          console.log(`  ❌ Update failed: ${error.message}`);
        }
      } else {
        skippedCount++;
        console.log(`  ⏭️  No Timestamp fields to fix`);
      }
    }

    console.log('\n' + '='.repeat(60));
    console.log('\n📊 Summary:');
    console.log(`  ✅ Fixed: ${fixedCount} orders`);
    console.log(`  ⏭️  Skipped: ${skippedCount} orders (already correct)`);
    console.log(`  ❌ Errors: ${errorCount} orders`);
    console.log('\n✅ Timestamp fix complete!\n');

    if (fixedCount > 0) {
      console.log('💡 Next Steps:');
      console.log('  1. Test Payment History screen - should show correct amounts');
      console.log('  2. Test Buyer Refund screen - should load without errors');
      console.log('  3. Verify real-time updates work when new orders are placed\n');
    }

  } catch (error) {
    console.error('\n❌ Error:', error.message);
    console.error(error.stack);
  } finally {
    process.exit(0);
  }
}

fixOrderTimestamps();
