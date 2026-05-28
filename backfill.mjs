import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';
const serviceAccountPath = path.join(process.cwd(), 'serviceAccountKey.json');
const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
admin.initializeApp({ credential: admin.credential.cert(serviceAccount), projectId: serviceAccount.project_id });
const db = admin.firestore();
async function backfill() {
  try {
    const statuses = ['completed','Completed','COMPLETED','delivered','Delivered','DELIVERED'];
    let allOrders = [];
    for (const status of statuses) {
      const snap = await db.collection('orders').where('status','==',status).get();
      allOrders = allOrders.concat(snap.docs);
    }
    console.log('Found ' + allOrders.length + ' completed orders');
    const existing = await db.collection('payments').get();
    const existingOrderIds = new Set(existing.docs.map(d => d.data().order_id));
    const toProcess = allOrders.filter(o => !existingOrderIds.has(o.id));
    console.log('Orders needing payments: ' + toProcess.length);
    let created = 0;
    const batch = db.batch();
    for (const orderDoc of toProcess) {
      const o = orderDoc.data();
      const orderId = orderDoc.id;
      const sellerId = o.seller_id || o.sellerId || '';
      const sellerName = o.seller_name || o.sellerName || 'Seller';
      const amount = o.total_price || o.totalPrice || o.subtotal || 0;
      if (!sellerId) {
        console.log('Skipping order ' + orderId + ' - no seller_id found');
        continue;
      }
      const ref = db.collection('payments').doc();
      batch.set(ref, {
        id: ref.id,
        seller_id: sellerId,
        seller_name: sellerName,
        order_id: orderId,
        buyer_id: o.buyer_id || o.buyerId || '',
        buyer_name: o.buyer_name || o.buyerName || 'Buyer',
        amount: amount,
        payment_method: o.payment_method || o.paymentMethod || 'Cash on Delivery',
        status: 'completed',
        payment_date: Date.now(),
        items_count: o.quantity || 1,
        items_details: [{
          product_id: o.product_id || o.productId || '',
          product_title: o.product_title || o.productTitle || 'Product',
          quantity: o.quantity || 1,
          price: o.subtotal || o.product_price || 0,
          item_total: amount
        }],
        created_at: o.created_at || Date.now(),
        updated_at: Date.now(),
        co_seller_store_id: o.co_seller_store_id || o.coSellerStoreId || '',
        refund_amount: 0,
        refund_reason: '',
        involved_seller_ids: [sellerId],
        payment_splits: [],
        idempotency_key: 'backfill_' + orderId
      });
      console.log('Creating payment | order: ' + orderId + ' | seller: ' + sellerId + ' | PKR ' + amount);
      created++;
    }
    if (created > 0) {
      await batch.commit();
      console.log('');
      console.log('Done! Created ' + created + ' payments successfully');
    } else {
      console.log('Nothing to create');
    }
  } catch(e) { console.error(e); }
  finally { await admin.app().delete(); }
}
backfill();
