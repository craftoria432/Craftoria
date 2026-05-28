// fix-payment-amounts-v2.mjs
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFileSync } from "fs";

const serviceAccount = JSON.parse(readFileSync("./serviceAccountKey.json", "utf8"));
initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

async function fixPaymentAmounts() {
  console.log("Craftoria - Fix Payment Amounts v2");
  console.log("====================================\n");

  // Fetch ALL payments, filter in memory (avoids Firestore index issues)
  console.log("🔍 Fetching all payments...");
  const paymentsSnap = await db.collection("seller_payments").get();
  
  const zeroPayments = paymentsSnap.docs.filter(doc => {
    const amount = doc.data().amount;
    return !amount || amount === 0;
  });

  console.log(`📦 Found ${zeroPayments.length} zero-amount payments to fix\n`);

  if (zeroPayments.length === 0) {
    console.log("✅ Nothing to fix!");
    process.exit(0);
  }

  let fixed = 0;
  let skipped = 0;
  let errors = 0;

  for (const paymentDoc of zeroPayments) {
    const payment = paymentDoc.data();
    const orderId = payment.order_id;

    if (!orderId) {
      console.log(`⏭️  Skipping ${paymentDoc.id.substring(0, 8)} — no order_id`);
      skipped++;
      continue;
    }

    try {
      console.log(`🔄 Processing payment ${paymentDoc.id.substring(0, 8)} → order ${orderId.substring(0, 8)}...`);
      
      const orderDoc = await db.collection("orders").doc(orderId).get();

      if (!orderDoc.exists) {
        console.log(`   ⏭️  Order not found, skipping`);
        skipped++;
        continue;
      }

      const order = orderDoc.data();

      // Determine correct amount
      let correctAmount = 0;
      if (order.total_price && order.total_price > 0) {
        correctAmount = order.total_price;
      } else if (order.total_amount && order.total_amount > 0) {
        correctAmount = order.total_amount;
      } else if (order.items && order.items.length > 0) {
        correctAmount = order.items.reduce((sum, item) => {
          return sum + ((item.price || 0) * (item.quantity || 1));
        }, 0);
      } else if (order.product_price && order.product_price > 0) {
        correctAmount = order.product_price * (order.quantity || 1);
      }

      if (correctAmount <= 0) {
        console.log(`   ⚠️  Order also has zero amount, skipping`);
        skipped++;
        continue;
      }

      await paymentDoc.ref.update({
        amount: correctAmount,
        updated_at: Date.now(),
      });

      console.log(`   ✅ Fixed: PKR 0 → PKR ${correctAmount} (buyer: ${payment.buyer_name || "unknown"})`);
      fixed++;

    } catch (err) {
      console.error(`   ❌ Error: ${err.message}`);
      errors++;
    }
  }

  console.log("\n============================================================");
  console.log("📊 SUMMARY");
  console.log("============================================================");
  console.log(`✅ Fixed:   ${fixed}`);
  console.log(`⏭️  Skipped: ${skipped}`);
  console.log(`❌ Errors:  ${errors}`);
  console.log("============================================================");
  
  if (fixed > 0) {
    console.log("\n✅ Done! Clear app cache and restart to see correct amounts.");
  }
  
  process.exit(0);
}

fixPaymentAmounts().catch(err => {
  console.error("Fatal error:", err);
  process.exit(1);
});
