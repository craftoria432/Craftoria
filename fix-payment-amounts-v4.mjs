// fix-payment-amounts-v4.mjs
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFileSync } from "fs";

const serviceAccount = JSON.parse(readFileSync("./serviceAccountKey.json", "utf8"));
initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

// Hardcoded fixes from v3 output — no Firestore reads needed
const fixes = [
  { id: "HIDw2TK9h768HRxxw1Ln", amount: 1230 },  // Ahmed  - qcr8ndhn
  { id: "ISEKlvhRxxxxxxxxxxxxxxx", amount: 1150 }, // Ahmed  - OY0ycjSW
  { id: "PhNTM08xxxxxxxxxxxxxxxX", amount: 1350 }, // Bilal  - 13tAlyWS
  { id: "aB07MIygxxxxxxxxxxxxxxx", amount: 1000 }, // Ahmed  - tpM0GB0H
  { id: "bjpNNaNHxxxxxxxxxxxxxxx", amount: 1150 }, // Bilal  - 3Bd2rw63
  { id: "ioUwWss4xxxxxxxxxxxxxxx", amount: 1150 }, // Haider - MmeP7hrx
  { id: "oOF7DfH5xxxxxxxxxxxxxxx", amount: 1000 }, // Bilal  - KNlW1mTK
  { id: "oZo6k6stxxxxxxxxxxxxxxx", amount: 1350 }, // Ahmed  - 7kT8bl60
];

async function fixPaymentAmounts() {
  console.log("Craftoria - Fix Payment Amounts v4");
  console.log("====================================\n");

  // Fetch all payments to get full document IDs
  console.log("🔍 Fetching payments to get full IDs...");
  const paymentsSnap = await db.collection("seller_payments").get();
  
  const zeroPayments = paymentsSnap.docs.filter(doc => {
    const amount = doc.data().amount;
    return !amount || amount === 0;
  });

  console.log(`📦 Found ${zeroPayments.length} zero-amount payments\n`);

  // Match by order_id since we know those from v3 output
  const orderAmountMap = {
    "qcr8ndhn": 1230,
    "OY0ycjSW": 1150,
    "13tAlyWS": 1350,
    "tpM0GB0H": 1000,
    "3Bd2rw63": 1150,
    "MmeP7hrx": 1150,
    "KNlW1mTK": 1000,
    "7kT8bl60": 1350,
  };

  let fixed = 0;

  // Update one by one with no timeout — fire and forget style
  for (const doc of zeroPayments) {
    const data = doc.data();
    const orderId = data.order_id || "";
    
    // Find matching amount by order ID prefix
    const matchKey = Object.keys(orderAmountMap).find(k => orderId.startsWith(k));
    
    if (!matchKey) {
      console.log(`⏭️  Skipping ${doc.id.substring(0, 8)} — no amount match`);
      continue;
    }

    const amount = orderAmountMap[matchKey];
    console.log(`🔄 Updating ${doc.id.substring(0, 8)} → PKR ${amount} (${data.buyer_name})...`);
    
    try {
      await doc.ref.update({ amount, updated_at: Date.now() });
      console.log(`   ✅ Done`);
      fixed++;
      // Small delay between writes to avoid overwhelming connection
      await new Promise(r => setTimeout(r, 500));
    } catch (e) {
      console.error(`   ❌ Failed: ${e.message}`);
    }
  }

  console.log(`\n✅ Fixed ${fixed} payments`);
  process.exit(0);
}

fixPaymentAmounts().catch(err => {
  console.error("Fatal:", err.message);
  process.exit(1);
});
