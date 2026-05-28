import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';
const serviceAccountPath = path.join(process.cwd(), 'serviceAccountKey.json');
if (!fs.existsSync(serviceAccountPath)) { console.error('❌ serviceAccountKey.json not found'); process.exit(1); }
const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
admin.initializeApp({ credential: admin.credential.cert(serviceAccount), projectId: serviceAccount.project_id });
const db = admin.firestore();
async function fix() {
  try {
    const statuses = ['completed','Completed','COMPLETED','delivered','Delivered','DELIVERED'];
    let allOrders = [];
    for (const status of statuses) {
      const snap = await db.collection('orders').where('status','==',status).get();
      allOrders = allOrders.concat(snap.docs);
    }
    console.log('Found ' + allOrders.length + ' completed orders');
    let updated = 0; let noPayment = 0;
    for (const orderDoc of allOrders) {
      const payments = await db.collection('payments').where('order_id','==',orderDoc.id).get();
      if (payments.empty) { console.log('No payment for order: ' + orderDoc.id); noPayment++; continue; }
      for (const p of payments.docs) {
        if (p.data().status === 'pending' || p.data().status === 'PENDING') {
          await p.ref.update({ status: 'completed', payment_date: Date.now(), updated_at: Date.now() });
          console.log('Updated payment: ' + p.id);
          updated++;
        }
      }
    }
    console.log('Done! Updated: ' + updated + ' | No payment found: ' + noPayment);
  } catch(e) { console.error(e); }
  finally { await admin.app().delete(); }
}
fix();
