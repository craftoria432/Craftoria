import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';
const serviceAccountPath = path.join(process.cwd(), 'serviceAccountKey.json');
const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
admin.initializeApp({ credential: admin.credential.cert(serviceAccount), projectId: serviceAccount.project_id });
const db = admin.firestore();
async function inspect() {
  try {
    const snap = await db.collection('orders').limit(1).get();
    if (snap.empty) { console.log('No orders found'); return; }
    const doc = snap.docs[0];
    console.log('Order ID: ' + doc.id);
    console.log('Fields:');
    const data = doc.data();
    Object.keys(data).forEach(key => {
      console.log('  ' + key + ': ' + JSON.stringify(data[key]));
    });
  } catch(e) { console.error(e); }
  finally { await admin.app().delete(); }
}
inspect();
