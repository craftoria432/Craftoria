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

console.log('\n🔍 Craftoria - Order Timestamp Check');
console.log('=====================================\n');

async function checkOrderTimestamps() {
  try {
    // Get a sample order
    const ordersSnapshot = await db.collection('orders')
      .limit(1)
      .get();

    if (ordersSnapshot.empty) {
      console.log('❌ No orders found');
      return;
    }

    const orderDoc = ordersSnapshot.docs[0];
    const orderData = orderDoc.data();

    console.log(`📦 Sample Order: ${orderDoc.id}\n`);
    console.log('Field Types:');
    console.log('─'.repeat(50));

    // Check each timestamp field
    const timestampFields = [
      'created_at',
      'updated_at',
      'order_placed_at',
      'processing_at',
      'shipped_at',
      'delivered_at',
      'cancelled_at'
    ];

    timestampFields.forEach(field => {
      const value = orderData[field];
      if (value !== undefined && value !== null) {
        const type = typeof value;
        const isTimestamp = value instanceof admin.firestore.Timestamp;
        const isNumber = typeof value === 'number';
        
        console.log(`\n${field}:`);
        console.log(`  Type: ${type}`);
        console.log(`  Is Timestamp: ${isTimestamp}`);
        console.log(`  Is Number: ${isNumber}`);
        console.log(`  Value: ${value}`);
        
        if (isTimestamp) {
          console.log(`  ⚠️  PROBLEM: This is a Timestamp object`);
          console.log(`  ✅ Should be: ${value.toMillis()} (Long)`);
        }
      }
    });

    console.log('\n' + '='.repeat(50));
    console.log('\n💡 Solution:');
    console.log('Run: node fix-order-timestamps.mjs');
    console.log('This will convert all Timestamp fields to Long (milliseconds)\n');

  } catch (error) {
    console.error('❌ Error:', error.message);
  } finally {
    process.exit(0);
  }
}

checkOrderTimestamps();
