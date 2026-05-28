// fix-payment-amounts-v3.mjs
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFileSync } from "fs";

const serviceAccount = JSON.parse(readFileSync("./serviceAccountKey.json", "utf8"));
const app = initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

// Timeout wrapper — rejects if Firestore takes too long
function withTimeout(promise, ms = 8000) {
  return Promise.race([
    promise,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error(`Timed out after ${ms}ms`)), ms)
    ),
  ]);
}

async function fixPaymentAmounts() {
  console.log("Craftoria - Fix Payment Amounts v3");
  console.log("====================================\n");

  console.log("🔍 Fetching all payments...");
  const paymentsSnap = await withTimeout(db.collection("seller_payments").get(), 15000);

  const zeroPayments = paymentsSnap.docs.filter(doc => {
    const amount = doc.data().amount;
    return !amount || amount === 0;
  });

  console.log(`📦 Found ${zeroPayments.length} zero-amount payments\n`);

  if (zeroPayments.length === 0) {
    console.log("✅ Nothing to fix!");
    process.exit(0);
  }

  // Print all payment → order mappings first so we can see what we're working with
  console.log("📋 Payments to fix:");
  zeroPayments.forEach((doc, i) => {
    const d = doc.data();
    console.log(`  ${i + 1}. Payment ${doc.id.substring(0, 8)} → Order ${(d.order_id || "NONE").substring(0, 8)} | Buyer: ${d.buyer_name || "?"}`);
  });
  console.log("");

  // Fetch ALL orders at once — single round trip instead of N round trips
  console.log("📦 Fetching all orders in one request...");
  const ordersSnap = await withTimeout(db.collection("orders").get(), 15000);
  console.log(`✅ Fetched ${ordersSnap.size} orders\n`);

  // Build order map for instant lookup
  const orderMap = {};
  ordersSnap.docs.forEach(doc => {
    orderMap[doc.id] = doc.data();
  });

  let fixed = 0;
  let skipped = 0;
  let errors = 0;

  // Use a batch for all updates — single round trip
  const batch = db.batch();

  for (const paymentDoc of zeroPayments) {
    const payment = paymentDoc.data();
    const orderId = payment.order_id;

    if (!orderId) {
      console.log(`⏭️  Payment ${paymentDoc.id.substring(0, 8)} — no order_id, skipping`);
      skipped++;
      continue;
    }

    const order = orderMap[orderId];

    if (!order) {
      console.log(`⏭️  Payment ${paymentDoc.id.substring(0, 8)} — order ${orderId.substring(0, 8)} not found, skipping`);
      skipped++;
      continue;
    }

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
      console.log(`⚠️  Payment ${paymentDoc.id.substring(0, 8)} — order also has zero amount, skipping`);
      skipped++;
      continue;
    }

    console.log(`✅ Queuing fix: Payment ${paymentDoc.id.substring(0, 8)} → PKR ${correctAmount} (${payment.buyer_name || "?"})`);
    batch.update(paymentDoc.ref, {
      amount: correctAmount,
      updated_at: Date.now(),
    });
    fixed++;
  }

  if (fixed > 0) {
    console.log(`\n💾 Committing ${fixed} updates in one batch...`);
    await withTimeout(batch.commit(), 15000);
    console.log("✅ Batch committed successfully!");
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
  console.error("\n❌ Fatal error:", err.message);
  process.exit(1);
});
