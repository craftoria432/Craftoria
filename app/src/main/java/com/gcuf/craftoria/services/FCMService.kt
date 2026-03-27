package com.gcuf.craftoria.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gcuf.craftoria.MainActivity
import com.gcuf.craftoria.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID_CHAT = "chat_messages"
        private const val CHANNEL_ID_ORDERS = "order_updates"
        private const val CHANNEL_ID_GENERAL = "general_notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.let { data ->
            val type = data["type"] ?: return
            
            when (type) {
                "chat_message" -> handleChatMessage(data)
                "order_update" -> handleOrderUpdate(data)
                "negotiation" -> handleNegotiation(data)
                "product_shared" -> handleProductShared(data)
                else -> handleGeneralNotification(data)
            }
        }

        // Handle notification payload (if app is in foreground)
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Craftoria",
                message = notification.body ?: "",
                channelId = CHANNEL_ID_GENERAL
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Save token to Firestore
        saveFCMToken(token)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Chat messages channel
            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                enableLights(true)
            }

            // Order updates channel
            val orderChannel = NotificationChannel(
                CHANNEL_ID_ORDERS,
                "Order Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for order status updates"
                enableVibration(true)
            }

            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(orderChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    private fun handleChatMessage(data: Map<String, String>) {
        val chatId = data["chat_id"] ?: return
        val senderId = data["sender_id"] ?: return
        val senderName = data["sender_name"] ?: "Someone"
        val message = data["message"] ?: ""
        val messageType = data["message_type"] ?: "text"

        val displayMessage = when (messageType) {
            "image" -> "📷 Sent a photo"
            "product" -> "📦 Shared a product"
            "negotiation" -> "💰 Sent a negotiation request"
            else -> message
        }

        showChatNotification(chatId, senderId, senderName, displayMessage)
    }

    private fun handleOrderUpdate(data: Map<String, String>) {
        val orderId = data["order_id"] ?: return
        val status = data["status"] ?: return
        val title = "Order Update"
        val message = "Your order #${orderId.take(8)} is now $status"

        showOrderNotification(orderId, title, message)
    }

    private fun handleNegotiation(data: Map<String, String>) {
        val chatId = data["chat_id"] ?: return
        val senderName = data["sender_name"] ?: "Someone"
        val productName = data["product_name"] ?: "a product"
        val price = data["price"] ?: ""

        val message = "$senderName wants to negotiate for $productName at PKR $price"
        showChatNotification(chatId, "", senderName, message)
    }

    private fun handleProductShared(data: Map<String, String>) {
        val chatId = data["chat_id"] ?: return
        val senderName = data["sender_name"] ?: "Someone"
        val productName = data["product_name"] ?: "a product"

        val message = "$senderName shared $productName with you"
        showChatNotification(chatId, "", senderName, message)
    }

    private fun handleGeneralNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Craftoria"
        val message = data["message"] ?: ""
        
        showNotification(title, message, CHANNEL_ID_GENERAL)
    }

    private fun showChatNotification(
        chatId: String,
        senderId: String,
        senderName: String,
        message: String
    ) {
        val notificationId = chatId.hashCode()

        // Create intent to open chat
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chat_id", chatId)
            putExtra("sender_id", senderId)
            putExtra("open_chat", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun showOrderNotification(orderId: String, title: String, message: String) {
        val notificationId = orderId.hashCode()

        // Create intent to open orders
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("order_id", orderId)
            putExtra("open_orders", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ORDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun showNotification(title: String, message: String, channelId: String) {
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun saveFCMToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current user ID from SharedPreferences or Firebase Auth
                val sharedPrefs = getSharedPreferences("craftoria_prefs", Context.MODE_PRIVATE)
                val userId = sharedPrefs.getString("user_id", null) ?: return@launch

                // Save token to Firestore
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcm_token", token)
                    .await()

                Log.d(TAG, "FCM token saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token", e)
            }
        }
    }
}
