// fix-corrupted-payments.mjs
// Deletes payments with no order_id and recreates them from real order data
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFileSync } from "fs";

const serviceAccount = JSON.parse(readFileSync("./serviceAccountKey.json", "utf8"));
initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

async function fixCorruptedPayments() {
  console.log("🔧 Fetching all payments and orders...");

  const [paymentsSnap, ordersSnap] = await Promise.all([
    db.collection("seller_payments").get(),
    db.collection("orders").get()
  ]);

  // Find corrupted payments (no order_id or empty order_id)
  const corrupted = paymentsSnap.docs.filter(d => {
    const orderId = d.data().order_id;
    return !orderId || orderId.trim() === "" || orderId === "N/A";
  });

  // Build set of order IDs that already have valid payments
  const ordersWithValidPayments = new Set(
    paymentsSnap.docs
      .filter(d => d.data().order_id && d.data().order_id.trim() !== "")
      .map(d => d.data().order_id)
  );

  console.log(`📦 Total orders: ${ordersSnap.size}`);
  console.log(`💳 Total payments: ${paymentsSnap.size}`);
  console.log(`🗑️  Corrupted payments (no order_id): ${corrupted.length}`);
  console.log(`✅ Orders already with valid payments: ${ordersWithValidPayments.size}\n`);

  // Find orders that need new payment records
  const ordersMissingPayments = ordersSnap.docs.filter(d => !ordersWithValidPayments.has(d.id));
  console.log(`❌ Orders missing valid payments: ${ordersMissingPayments.length}\n`);

  const deleteBatch = db.batch();
  const createBatch = db.batch();

  // Step 1: Delete corrupted payments
  if (corrupted.length > 0) {
    console.log("🗑️  Deleting corrupted payments:");
    corrupted.forEach(doc => {
      const d = doc.data();
      console.log(`   ${doc.id.substring(0,8)}... | Buyer: ${d.buyer_name} | Amount: PKR ${d.amount}`);
      deleteBatch.delete(doc.ref);
    });
  }

  // Step 2: Create correct payments for orders missing them
  let createCount = 0;
  if (ordersMissingPayments.length > 0) {
    console.log("\n✅ Creating payments for orders:");
    for (const doc of ordersMissingPayments) {
      const order = doc.data();
      const orderId = doc.id;

      // Determine amount
      let amount = 0;
      if (order.total_price > 0) amount = order.total_price;
      else if (order.total_amount > 0) amount = order.total_amount;
      else if (order.items?.length > 0) {
        amount = order.items.reduce((s, i) => s + ((i.price || 0) * (i.quantity || 1)), 0);
      } else if (order.product_price > 0) {
        amount = order.product_price * (order.quantity || 1);
      }

      // Determine items count
      let itemsCount = 1;
      if (order.items?.length > 0) {
        itemsCount = order.items.reduce((s, i) => s + (i.quantity || 1), 0);
      } else if (order.quantity) {
        itemsCount = order.quantity;
      }

      // Determine status
      const orderStatus = (order.status || "").toLowerCase();
      let paymentStatus = "pending";
      if (orderStatus === "completed" || orderStatus === "delivered") paymentStatus = "completed";
      else if (orderStatus === "cancelled") paymentStatus = "failed";
      else if (orderStatus === "processing" || orderStatus === "shipped") paymentStatus = "pending";

      const paymentRef = db.collection("seller_payments").doc();
      const now = Date.now();

      const payment = {
        id: paymentRef.id,
        order_id: orderId,
        buyer_id: order.buyer_id || "",
        buyer_name: order.buyer_name || "",
        seller_id: order.seller_id || "",
        seller_name: order.seller_name || "",
        amount: amount,
        payment_method: order.payment_method || "Cash on Delivery",
        status: paymentStatus,
        items_count: itemsCount,
        items_details: (order.items || []).map(i => ({
          product_id: i.product_id || "",
          product_title: i.product_title || "",
          quantity: i.quantity || 1,
          price: i.price || 0,
          item_total: (i.price || 0) * (i.quantity || 1)
        })),
        created_at: now,
        updated_at: now,
        co_seller_store_id: order.co_seller_store_id || "",
        store_name: order.seller_name || "",
        involved_seller_ids: [order.seller_id || ""].filter(Boolean),
        payment_splits: [],
        refund_amount: 0,
        refund_reason: "",
        refund_date: 0,
        transaction_id: "",
        idempotency_key: "",
        request_id: ""
      };

      console.log(`   Order ${orderId.substring(0,8)} → PKR ${amount} | Buyer: ${order.buyer_name} | Status: ${paymentStatus}`);
      createBatch.set(paymentRef, payment);
      createCount++;
    }
  }

  // Commit both batches
  if (corrupted.length > 0) {
    console.log(`\n💾 Deleting ${corrupted.length} corrupted payments...`);
    await deleteBatch.commit();
    console.log("✅ Deleted!");
  }

  if (createCount > 0) {
    console.log(`💾 Creating ${createCount} new payments...`);
    await createBatch.commit();
    console.log("✅ Created!");
  }

  console.log("\n============================================================");
  console.log(`🗑️  Deleted: ${corrupted.length} corrupted payments`);
  console.log(`✅ Created: ${createCount} new payments`);
  console.log("============================================================");
  console.log("\n✅ Done! Clear app cache and restart.");
  process.exit(0);
}

fixCorruptedPayments().catch(e => {
  console.error("❌", e.message);
  process.exit(1);
});
