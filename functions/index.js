require('dotenv').config();

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");
const emailService = require("./emailService");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ============================================================================
// NOTIFICATION TRIGGERS
// ============================================================================

exports.onOrderCreated = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const order = snap.data();
    const orderId = context.params.orderId;

    try {
      const buyerDoc = await db.collection("users").doc(order.buyerId).get();
      const buyerToken = buyerDoc.data()?.fcmToken;

      if (buyerToken) {
        await messaging.send({
          token: buyerToken,
          notification: {
            title: "Order Confirmed",
            body: `Your order #${orderId.substring(0, 8)} has been placed successfully`,
          },
          data: {
            orderId,
            type: "order_created",
          },
        });
      }

      const sellerDoc = await db.collection("users").doc(order.sellerId).get();
      const sellerToken = sellerDoc.data()?.fcmToken;

      if (sellerToken) {
        await messaging.send({
          token: sellerToken,
          notification: {
            title: "New Order",
            body: `You received a new order #${orderId.substring(0, 8)}`,
          },
          data: {
            orderId,
            type: "new_order",
          },
        });
      }

      console.log(`✅ Order notifications sent for ${orderId}`);
    } catch (error) {
      console.error(`❌ Error sending order notifications: ${error.message}`);
    }
  });

// ============================================================================
// ORDER STATUS UPDATE
// ============================================================================

exports.onOrderStatusChanged = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const oldOrder = change.before.data();
    const newOrder = change.after.data();
    const orderId = context.params.orderId;

    if (oldOrder.status === newOrder.status) return;

    try {
      const buyerDoc = await db.collection("users").doc(newOrder.buyerId).get();
      const buyerToken = buyerDoc.data()?.fcmToken;

      if (buyerToken) {
        const statusMessages = {
          processing: "Your order is being processed",
          shipped: "Your order has been shipped",
          delivered: "Your order has been delivered",
          cancelled: "Your order has been cancelled",
        };

        await messaging.send({
          token: buyerToken,
          notification: {
            title: "Order Status Updated",
            body: statusMessages[newOrder.status] || `Order status: ${newOrder.status}`,
          },
          data: {
            orderId,
            status: newOrder.status,
            type: "order_status_changed",
          },
        });
      }

      console.log(`✅ Status notification sent for ${orderId}`);
    } catch (error) {
      console.error(`❌ Error: ${error.message}`);
    }
  });

// ============================================================================
// PAYMENT NOTIFICATION
// ============================================================================

exports.onPaymentProcessed = functions.firestore
  .document("payments/{paymentId}")
  .onCreate(async (snap, context) => {
    const payment = snap.data();

    try {
      const sellerDoc = await db.collection("users").doc(payment.sellerId).get();
      const sellerToken = sellerDoc.data()?.fcmToken;

      if (sellerToken) {
        await messaging.send({
          token: sellerToken,
          notification: {
            title: "Payment Received",
            body: `Payment of PKR ${payment.amount} received`,
          },
        });
      }
    } catch (error) {
      console.error(`❌ Payment notification error: ${error.message}`);
    }
  });

// ============================================================================
// CHAT NOTIFICATION
// ============================================================================

exports.onChatMessageCreated = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const chatId = context.params.chatId;

    try {
      const chatDoc = await db.collection("chats").doc(chatId).get();
      const chat = chatDoc.data();

      const receiverId =
        message.senderId === chat.buyerId ? chat.sellerId : chat.buyerId;

      const userDoc = await db.collection("users").doc(receiverId).get();
      const token = userDoc.data()?.fcmToken;

      if (token) {
        await messaging.send({
          token,
          notification: {
            title: "New Message",
            body: message.content.substring(0, 100),
          },
          data: { chatId },
        });
      }
    } catch (error) {
      console.error(`❌ Chat error: ${error.message}`);
    }
  });

// ============================================================================
// EMAIL: ORDER CONFIRMATION (via EmailJS - COMPLETELY FREE)
// ============================================================================

exports.sendOrderEmail = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const order = snap.data();
    const orderId = context.params.orderId;

    try {
      const buyerDoc = await db.collection("users").doc(order.buyerId).get();
      const buyerData = buyerDoc.data();
      const buyerEmail = buyerData?.email;
      const buyerName = buyerData?.name || "Customer";

      if (!buyerEmail) {
        console.log("❌ No email found for buyer");
        return;
      }

      // Send via EmailJS service
      const result = await emailService.sendOrderConfirmationEmail({
        customerEmail: buyerEmail,
        customerName: buyerName,
        orderId: orderId.substring(0, 8).toUpperCase(),
        orderDate: new Date().toLocaleDateString(),
        paymentMethod: order.paymentMethod || "N/A",
        totalPrice: order.totalPrice?.toString() || "0.00",
        deliveryAddress: order.deliveryAddress || "N/A"
      });

      if (result.success) {
        console.log(`✅ Email sent successfully to ${buyerEmail}`);

        // Log in Firestore for audit
        await db.collection("admin_activities").add({
          action: "ORDER_CONFIRMATION_EMAIL_SENT",
          orderId,
          buyerEmail,
          status: "success",
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });
      } else {
        console.warn(`⚠ Email failed but order completed: ${result.error}`);

        // Log failure
        await db.collection("admin_activities").add({
          action: "ORDER_CONFIRMATION_EMAIL_FAILED",
          orderId,
          buyerEmail,
          error: result.error,
          status: "failed",
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });
      }

    } catch (error) {
      console.error(`❌ Email error: ${error.message}`);

      // Log error
      await db.collection("admin_activities").add({
        action: "ORDER_CONFIRMATION_EMAIL_ERROR",
        orderId: context.params.orderId,
        error: error.message,
        status: "error",
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
    }
  });

// ============================================================================
// EXPORT EMAIL SERVICE FUNCTIONS
// ============================================================================

exports.sendOrderConfirmation = emailService.sendOrderConfirmation || functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }
  return await emailService.sendOrderConfirmationEmail(data);
});

exports.testEmailService = emailService.testEmailService || functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }
  return { configured: true, message: 'Email service is ready' };
});

exports.getEmailServiceStatus = emailService.getEmailServiceStatus || functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }
  return { ready: true };
});

exports.getEmailLogs = emailService.getEmailLogs || functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }
  return { success: true, logs: [] };
});

// ============================================================================
// EMAIL: SELLER APPROVAL NOTIFICATION
// ============================================================================

exports.sendSellerApprovalEmail = functions.https.onRequest(async (req, res) => {
  // Enable CORS
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(204).send('');
    return;
  }

  if (req.method !== 'POST') {
    res.status(405).send('Method Not Allowed');
    return;
  }

  try {
    const { sellerEmail, sellerName } = req.body;

    if (!sellerEmail || !sellerName) {
      res.status(400).json({ 
        success: false, 
        error: 'Missing required fields: sellerEmail and sellerName' 
      });
      return;
    }

    // Send via EmailJS service
    const result = await emailService.sendSellerApprovalEmail({
      sellerEmail,
      sellerName
    });

    if (result.success) {
      console.log(`✅ Seller approval email sent to ${sellerEmail}`);

      // Log in Firestore for audit
      await db.collection("admin_activities").add({
        action: "SELLER_APPROVAL_EMAIL_SENT",
        sellerEmail,
        sellerName,
        status: "success",
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      res.status(200).json({ 
        success: true, 
        message: 'Seller approval email sent successfully' 
      });
    } else {
      console.warn(`⚠ Seller approval email failed: ${result.error}`);

      // Log failure
      await db.collection("admin_activities").add({
        action: "SELLER_APPROVAL_EMAIL_FAILED",
        sellerEmail,
        error: result.error,
        status: "failed",
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      res.status(500).json({ 
        success: false, 
        error: result.error || 'Failed to send email' 
      });
    }

  } catch (error) {
    console.error(`❌ Seller approval email error: ${error.message}`);

    // Log error
    await db.collection("admin_activities").add({
      action: "SELLER_APPROVAL_EMAIL_ERROR",
      error: error.message,
      status: "error",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// ============================================================================
// STORE RATING NOTIFICATIONS
// ============================================================================

/**
 * Notify seller when buyer submits a rating
 * Trigger: New document in store_ratings collection
 * Action: Create STORE_RATING notification for store owner
 */
exports.notifySellerOfRating = functions.firestore
  .document('store_ratings/{ratingId}')
  .onCreate(async (snap, context) => {
    try {
      const rating = snap.data();
      const ratingId = context.params.ratingId;

      // Extract rating data
      const storeId = rating.store_id;
      const buyerId = rating.buyer_id;
      const buyerName = rating.buyer_name || 'A buyer';
      const ratingValue = rating.rating || 0;
      const ratingReview = rating.review || '';

      console.log(`📊 New rating received for store: ${storeId}, rating: ${ratingValue}⭐`);

      // Validate required fields
      if (!storeId || !buyerId) {
        console.warn('⚠ Missing required fields in rating:', { storeId, buyerId });
        return;
      }

      // Get store owner ID from co_seller_stores
      const storeDoc = await db.collection('co_seller_stores').doc(storeId).get();

      if (!storeDoc.exists) {
        console.warn('⚠ Store not found:', storeId);
        return;
      }

      const storeOwnerId = storeDoc.data().owner_id;
      const storeName = storeDoc.data().store_name || 'Your Store';

      if (!storeOwnerId) {
        console.warn('⚠ Store owner ID not found for store:', storeId);
        return;
      }

      // Create notification for store owner
      const notification = {
        user_id: storeOwnerId,
        title: `New ${ratingValue}⭐ Rating from ${buyerName}`,
        description: ratingReview || `${buyerName} rated your store ${ratingValue} stars`,
        category: 'STORE_RATING',  // ✅ Seller-only category
        is_read: false,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
        action_type: 'VIEW_RATING',
        action_data: {
          store_id: storeId,
          rating_id: ratingId,
          buyer_id: buyerId
        },
        store_id: storeId,
        store_name: storeName,
        rating_value: ratingValue,
        rating_review: ratingReview,
        buyer_name: buyerName
      };

      // Add notification to Firestore
      const notifRef = await db.collection('notifications').add(notification);

      console.log('✅ Seller rating notification created:', notifRef.id);
      console.log('   Seller ID:', storeOwnerId);
      console.log('   Store:', storeName);
      console.log('   Rating:', `${ratingValue}⭐ from ${buyerName}`);

      // Send FCM notification to seller if they have a token
      const sellerDoc = await db.collection('users').doc(storeOwnerId).get();
      const sellerToken = sellerDoc.data()?.fcmToken;

      if (sellerToken) {
        await messaging.send({
          token: sellerToken,
          notification: {
            title: `New ${ratingValue}⭐ Rating`,
            body: `${buyerName} rated your store ${ratingValue} stars`
          },
          data: {
            type: 'store_rating',
            store_id: storeId,
            rating_id: ratingId
          }
        });
        console.log('✅ FCM notification sent to seller:', storeOwnerId);
      }

    } catch (error) {
      console.error('❌ Error in notifySellerOfRating:', error.message);
    }
  });

/**
 * Remind buyer to rate store after order delivery
 * Trigger: Order status changes to DELIVERED
 * Action: Create PROMOTIONS notification for buyer (if not already rated)
 */
exports.notifyBuyerToRateStore = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    try {
      const oldData = change.before.data();
      const newData = change.after.data();
      const orderId = context.params.orderId;

      // Only trigger when status changes to DELIVERED
      if (oldData.status !== 'DELIVERED' && newData.status === 'DELIVERED') {

        const buyerId = newData.buyer_id;
        const storeId = newData.store_id;
        const storeName = newData.store_name || 'Store';

        console.log(`📦 Order ${orderId} delivered. Checking if buyer should be reminded to rate...`);

        // Validate required fields
        if (!buyerId || !storeId) {
          console.warn('⚠ Missing required fields in order:', { buyerId, storeId });
          return;
        }

        // Check if buyer has already rated this store
        const existingRating = await db.collection('store_ratings')
          .where('store_id', '==', storeId)
          .where('buyer_id', '==', buyerId)
          .limit(1)
          .get();

        if (!existingRating.empty) {
          console.log(`✅ Buyer ${buyerId} already rated store ${storeId}. Skipping reminder.`);
          return;
        }

        // Create rating reminder notification
        const notification = {
          user_id: buyerId,
          title: `Rate ${storeName}`,
          description: `How was your experience with ${storeName}? Your feedback helps us improve.`,
          category: 'PROMOTIONS',  // ✅ Buyer engagement/feedback
          is_read: false,
          created_at: admin.firestore.FieldValue.serverTimestamp(),
          action_type: 'VIEW_RATING',
          action_data: {
            store_id: storeId,
            order_id: orderId
          },
          store_id: storeId,
          store_name: storeName,
          order_id: orderId
        };

        // Add notification to Firestore
        const notifRef = await db.collection('notifications').add(notification);

        console.log('✅ Buyer rating reminder created:', notifRef.id);
        console.log('   Buyer ID:', buyerId);
        console.log('   Store:', storeName);
        console.log('   Order:', orderId);

        // Send FCM notification to buyer if they have a token
        const buyerDoc = await db.collection('users').doc(buyerId).get();
        const buyerToken = buyerDoc.data()?.fcmToken;

        if (buyerToken) {
          await messaging.send({
            token: buyerToken,
            notification: {
              title: `Rate ${storeName}`,
              body: 'Your feedback helps us improve'
            },
            data: {
              type: 'rating_reminder',
              store_id: storeId,
              order_id: orderId
            }
          });
          console.log('✅ FCM notification sent to buyer:', buyerId);
        }

      } else if (oldData.status === 'DELIVERED' && newData.status !== 'DELIVERED') {
        console.log(`📦 Order ${orderId} status changed away from DELIVERED. No action needed.`);
      }

    } catch (error) {
      console.error('❌ Error in notifyBuyerToRateStore:', error.message);
    }
  });

console.log("✅ Cloud Functions initialized successfully");
console.log("✅ EmailJS service functions exported");
console.log("✅ Store Rating notification functions exported");
