// delete-synced-payments.mjs
// Deletes fake payment records created by sync-orders-to-payments.mjs
// These are identified by the "synced_from_order: true" flag the sync script added

import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFileSync } from "fs";

const serviceAccount = JSON.parse(readFileSync("./serviceAccountKey.json", "utf8"));
initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

async function deleteSyncedPayments() {
  console.log("🔍 Fetching all payments...");
  const paymentsSnap = await db.collection("seller_payments").get();

  // Synced payments are identified by synced_from_order: true
  const fakePayments = paymentsSnap.docs.filter(doc => doc.data().synced_from_order === true);
  const realPayments = paymentsSnap.docs.filter(doc => doc.data().synced_from_order !== true);

  console.log(`📦 Total payments: ${paymentsSnap.size}`);
  console.log(`🗑️  Fake (synced) payments to delete: ${fakePayments.length}`);
  console.log(`✅ Real payments to keep: ${realPayments.size}\n`);

  if (fakePayments.length === 0) {
    console.log("✅ No fake payments found — nothing to delete.");
    process.exit(0);
  }

  console.log("🗑️  Deleting fake payments:");
  fakePayments.forEach(doc => {
    const d = doc.data();
    console.log(`   ${doc.id.substring(0, 8)}... | Buyer: ${d.buyer_name} | Order: ${(d.order_id || "").substring(0, 8)}`);
  });

  const batch = db.batch();
  fakePayments.forEach(doc => batch.delete(doc.ref));

  console.log("\n💾 Committing deletions...");
  await batch.commit();
  console.log(`✅ Deleted ${fakePayments.length} fake payments.`);
  console.log(`✅ ${realPayments.length} real payments remain.`);
  console.log("\nRestart the app to see clean payment history.");
  process.exit(0);
}

deleteSyncedPayments().catch(e => {
  console.error("❌", e.message);
  process.exit(1);
});
