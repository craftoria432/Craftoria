#!/usr/bin/env node
/**
 * PAYMENT BACKFILL MIGRATION SCRIPT
 * 
 * Purpose: Create payment documents for existing orders that were completed
 * before the payment system code existed.
 * 
 * Usage:
 * 1. Save your Firebase service account key as firebaseServiceKey.json in this directory
 * 2. Run: node backfill-payments-migration.mjs
 * 3. Check Firestore console for new payment documents
 * 4. Verify payments appear in Seller Payments screen
 */

import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ─────────────────────────────────────────────────────────────────────
// CONFIGURATION
// ─────────────────────────────────────────────────────────────────────

const SERVICE_ACCOUNT_PATH = path.join(__dirname, 'firebaseServiceKey.json');
const DRY_RUN = false; // Set to true to preview changes without writing to Firestore

// ─────────────────────────────────────────────────────────────────────
// INITIALIZATION
// ─────────────────────────────────────────────────────────────────────

console.log('🔧 Payment Backfill Migration Script');
console.log('─'.repeat(60));

// Check if service account key exists
if (!fs.existsSync(SERVICE_ACCOUNT_PATH)) {
    console.error('❌ Service account key not found at:', SERVICE_ACCOUNT_PATH);
    console.error('📝 How to get your service account key:');
    console.error('   1. Go to Firebase Console → Project Settings → Service Accounts');
    console.error('   2. Click "Generate New Private Key"');
    console.error('   3. Save it as firebaseServiceKey.json in this directory');
    process.exit(1);
}

// Initialize Firebase Admin SDK
const serviceAccount = JSON.parse(fs.readFileSync(SERVICE_ACCOUNT_PATH, 'utf8'));
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: serviceAccount.project_id
});

const db = admin.firestore();

// ─────────────────────────────────────────────────────────────────────
// MIGRATION LOGIC
// ─────────────────────────────────────────────────────────────────────

async function backfillPayments() {
    try {
        console.log('📊 Starting migration...\n');
        
        // Step 1: Get all completed orders
        console.log('Step 1: Fetching completed orders...');
        const ordersSnapshot = await db
            .collection('orders')
            .where('status', 'in', ['Completed', 'completed', 'COMPLETED', 'Delivered', 'delivered', 'DELIVERED'])
            .get();
        
        console.log(`✅ Found ${ordersSnapshot.docs.length} completed/delivered orders\n`);
        
        if (ordersSnapshot.docs.length === 0) {
            console.log('✓ No orders to process. Migration complete!');
            await admin.app().delete();
            return;
        }
        
        // Step 2: Check which orders already have payments
        console.log('Step 2: Checking for existing payments...');
        const paymentsSnapshot = await db.collection('payments').get();
        const existingOrderIds = new Set(paymentsSnapshot.docs.map(doc => doc.data().order_id));
        
        console.log(`✅ Found ${paymentsSnapshot.docs.length} existing payment documents\n`);
        
        // Step 3: Identify orders needing payments
        const ordersNeedingPayments = ordersSnapshot.docs.filter(doc => {
            const orderId = doc.id;
            return !existingOrderIds.has(orderId);
        });
        
        console.log(`📌 Orders needing payments: ${ordersNeedingPayments.length}\n`);
        
        if (ordersNeedingPayments.length === 0) {
            console.log('✓ All completed orders already have payment documents!');
            await admin.app().delete();
            return;
        }
        
        // Step 4: Create payment documents
        console.log('Step 3: Creating payment documents...\n');
        
        let created = 0;
        let errors = 0;
        const batch = db.batch();
        
        for (const orderDoc of ordersNeedingPayments) {
            try {
                const order = orderDoc.data();
                const orderId = orderDoc.id;
                
                // Group items by seller (if multiple sellers in order)
                const sellers = new Map();
                
                if (order.items && Array.isArray(order.items)) {
                    // New format: items array
                    order.items.forEach(item => {
                        if (!sellers.has(item.sellerId)) {
                            sellers.set(item.sellerId, []);
                        }
                        sellers.get(item.sellerId).push(item);
                    });
                } else if (order.sellerId) {
                    // Legacy format: single seller
                    sellers.set(order.sellerId, [{
                        productId: order.productId || '',
                        sellerId: order.sellerId,
                        sellerName: order.sellerName || 'Unknown Seller',
                        productTitle: order.productTitle || 'Product',
                        quantity: order.quantity || 1,
                        price: order.productPrice || 0
                    }]);
                }
                
                // Create one payment document per seller
                sellers.forEach((items, sellerId) => {
                    const paymentRef = db.collection('payments').doc();
                    const amount = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
                    
                    const paymentData = {
                        id: paymentRef.id,
                        seller_id: sellerId,
                        seller_name: items[0]?.sellerName || 'Unknown Seller',
                        order_id: orderId,
                        co_seller_store_id: order.coSellerStoreId || '',
                        store_name: order.sellerName || '',
                        buyer_id: order.buyerId || '',
                        buyer_name: order.buyerName || 'Unknown Buyer',
                        amount: amount,
                        payment_method: order.paymentMethod || 'Cash on Delivery',
                        transaction_id: '',
                        status: 'completed', // Already completed since order is completed
                        payment_date: admin.firestore.Timestamp.now().toMillis(),
                        original_transaction_date: order.createdAt || admin.firestore.Timestamp.now().toMillis(),
                        items_count: items.reduce((sum, item) => sum + item.quantity, 0),
                        items_details: items.map(item => ({
                            product_id: item.productId || '',
                            product_title: item.productTitle || 'Product',
                            quantity: item.quantity || 1,
                            price: item.price || 0,
                            item_total: (item.price || 0) * (item.quantity || 1)
                        })),
                        created_at: admin.firestore.Timestamp.now().toMillis(),
                        updated_at: admin.firestore.Timestamp.now().toMillis(),
                        refund_amount: 0,
                        refund_reason: '',
                        refund_date: null,
                        involved_seller_ids: Array.from(sellers.keys()),
                        payment_splits: [],
                        idempotency_key: `backfill_${orderId}`,
                        request_id: ''
                    };
                    
                    if (DRY_RUN) {
                        console.log(`[DRY RUN] Would create payment for order ${orderId}, seller ${sellerId}:`);
                        console.log(`   Amount: PKR ${amount}`);
                        console.log(`   Items: ${paymentData.items_count}`);
                    } else {
                        batch.set(paymentRef, paymentData);
                        console.log(`✅ Prepared: Order ${orderId} → ${sellerId} (PKR ${amount})`);
                    }
                    
                    created++;
                });
            } catch (error) {
                console.error(`❌ Error processing order ${orderDoc.id}:`, error.message);
                errors++;
            }
        }
        
        // Step 5: Commit batch write
        if (!DRY_RUN && created > 0) {
            console.log(`\n📝 Writing ${created} payment documents to Firestore...`);
            await batch.commit();
            console.log('✅ Batch write complete!\n');
        } else if (DRY_RUN) {
            console.log(`\n[DRY RUN] Would create ${created} payment documents`);
        }
        
        // Summary
        console.log('─'.repeat(60));
        console.log('📊 MIGRATION SUMMARY');
        console.log('─'.repeat(60));
        console.log(`✅ Payments created: ${created}`);
        console.log(`❌ Errors: ${errors}`);
        console.log(`📌 Orders processed: ${ordersNeedingPayments.length}`);
        console.log('─'.repeat(60));
        
        if (!DRY_RUN) {
            console.log('\n✨ Migration complete! Verify in Firebase Console:');
            console.log('   1. Go to Firestore → payments collection');
            console.log('   2. Check for newly created payment documents');
            console.log('   3. Open Seller Payments screen in app');
            console.log('   4. Payments should now appear (may take 2-3 seconds)');
        }
        
    } catch (error) {
        console.error('❌ Fatal error during migration:', error);
        process.exit(1);
    } finally {
        await admin.app().delete();
    }
}

// ─────────────────────────────────────────────────────────────────────
// RUN MIGRATION
// ─────────────────────────────────────────────────────────────────────

console.log(`Mode: ${DRY_RUN ? '🔍 DRY RUN (no changes)' : '✏️ WRITE MODE (will modify Firestore)'}`);
console.log('');

backfillPayments().catch(error => {
    console.error('Script failed:', error);
    process.exit(1);
});
