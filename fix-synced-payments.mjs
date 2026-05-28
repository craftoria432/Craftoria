// fix-synced-payments.mjs
// Fixes items_count and payment_method for payments created by sync script
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFileSync } from "fs";

const serviceAccount = JSON.parse(readFileSync("./serviceAccountKey.json", "utf8"));
initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

async function fix() {
  console.log("🔧 Fetching all payments and orders...");
  const [paymentsSnap, ordersSnap] = await Promise.all([
    db.collection("seller_payments").get(),
    db.collection("orders").get()
  ]);

  // Build order map
  const orderMap = {};
  ordersSnap.docs.forEach(d => { orderMap[d.id] = d.data(); });

  // Find payments with 0 items (synced payments)
  const toFix = paymentsSnap.docs.filter(d => {
    const data = d.data();
    return !data.items_count || data.items_count === 0;
  });

  console.log(`📦 Found ${toFix.length} payments with 0 items\n`);

  const batch = db.batch();
  let fixed = 0;

  for (const doc of toFix) {
    const payment = doc.data();
    const order = orderMap[payment.order_id];
    if (!order) { console.log(`⏭️  No order for ${doc.id.substring(0,8)}`); continue; }

    // Calculate correct items_count
    let itemsCount = 0;
    if (order.items && order.items.length > 0) {
      itemsCount = order.items.reduce((sum, item) => sum + (item.quantity || 1), 0);
    } else if (order.quantity) {
      itemsCount = order.quantity;
    } else {
      itemsCount = 1;
    }

    // Get correct payment_method
    const paymentMethod = order.payment_method || "Cash on Delivery";

    // Get correct status based on order status
    const orderStatus = (order.status || "").toLowerCase();
    let status = payment.status;
    if (orderStatus === "completed" || orderStatus === "delivered") {
      status = "completed";
    } else if (orderStatus === "cancelled") {
      status = "failed";
    } else {
      status = "pending";
    }

    console.log(`✅ ${doc.id.substring(0,8)}: items=${itemsCount}, method=${paymentMethod}, status=${status}`);

    batch.update(doc.ref, {
      items_count: itemsCount,
      payment_method: paymentMethod,
      status: status,
      updated_at: Date.now()
    });
    fixed++;
  }

  if (fixed > 0) {
    console.log(`\n💾 Committing ${fixed} updates...`);
    await batch.commit();
    console.log("✅ Done! Restart app to see correct data.");
  } else {
    console.log("✅ Nothing to fix.");
  }

  process.exit(0);
}

fix().catch(e => { console.error("❌", e.message); process.exit(1); });
