package com.p4handheld.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.p4handheld.R
import com.p4handheld.data.models.FirebaseMessage
import com.p4handheld.data.models.MessagePriority
import com.p4handheld.data.models.MessageType
import com.p4handheld.firebase.FirebaseManager
import com.p4handheld.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "p4w_notifications"
        private const val CHANNEL_NAME = "P4W Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for P4W Handheld App"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data payload: ${remoteMessage.data}")

        // Convert RemoteMessage to FirebaseMessage
        val firebaseMessage = convertToFirebaseMessage(remoteMessage)

        // Store message locally
        serviceScope.launch {
            try {
                FirebaseManager.getInstance(this@FirebaseMessagingService)
                    .storeMessage(firebaseMessage)
                Log.d(TAG, "Message stored locally: ${firebaseMessage.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error storing message locally", e)
            }
        }

        // Show notification if app is in background or message has notification payload
        if (remoteMessage.notification != null || shouldShowNotification(firebaseMessage)) {
            showNotification(firebaseMessage)
        }

        // Broadcast message to active components
        broadcastMessage(firebaseMessage)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Send token to server
        serviceScope.launch {
            try {
                FirebaseManager.getInstance(this@FirebaseMessagingService)
                    .updateTokenOnServer(token)
                Log.d(TAG, "Token sent to server successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
            }
        }
    }

    private fun convertToFirebaseMessage(remoteMessage: RemoteMessage): FirebaseMessage {
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        return FirebaseMessage(
            id = data["messageId"] ?: remoteMessage.messageId ?: generateMessageId(),
            title = notification?.title ?: data["title"] ?: "",
            body = notification?.body ?: data["body"] ?: "",
            data = data,
            messageType = parseMessageType(data["messageType"]),
            userId = data["userId"],
            timestamp = remoteMessage.sentTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
            isRead = false,
            priority = parseMessagePriority(data["priority"])
        )
    }

    private fun parseMessageType(type: String?): MessageType {
        return try {
            MessageType.valueOf(type?.uppercase() ?: "NOTIFICATION")
        } catch (e: IllegalArgumentException) {
            MessageType.NOTIFICATION
        }
    }

    private fun parseMessagePriority(priority: String?): MessagePriority {
        return try {
            MessagePriority.valueOf(priority?.uppercase() ?: "NORMAL")
        } catch (e: IllegalArgumentException) {
            MessagePriority.NORMAL
        }
    }

    private fun shouldShowNotification(message: FirebaseMessage): Boolean {
        return when (message.messageType) {
            MessageType.ALERT -> true
            MessageType.SYSTEM -> true
            MessageType.TASKS_CHANGED -> message.priority == MessagePriority.HIGH || message.priority == MessagePriority.URGENT
            MessageType.SCREEN_REQUESTED -> true
            MessageType.USER_CHAT_MESSAGE -> message.priority != MessagePriority.LOW
            MessageType.NOTIFICATION -> true
        }
    }

    private fun showNotification(message: FirebaseMessage) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("messageId", message.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(getNotificationPriority(message.priority))

        // Add action buttons based on message type
        when (message.messageType) {
            MessageType.USER_CHAT_MESSAGE -> {
                val shareLocationIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("action", "share_location")
                    putExtra("messageId", message.id)
                }
                val shareLocationPendingIntent = PendingIntent.getActivity(
                    this, 1, shareLocationIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    R.drawable.ic_location,
                    "Share Location",
                    shareLocationPendingIntent
                )
            }

            MessageType.TASKS_CHANGED -> {
                val viewTaskIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("action", "view_task")
                    putExtra("taskId", message.data["taskId"])
                }
                val viewTaskPendingIntent = PendingIntent.getActivity(
                    this, 2, viewTaskIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.addAction(
                    R.drawable.ic_task,
                    "View Task",
                    viewTaskPendingIntent
                )
            }

            else -> {
                // Default action - open messages
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(message.id.hashCode(), notificationBuilder.build())
    }

    private fun getNotificationPriority(priority: MessagePriority): Int {
        return when (priority) {
            MessagePriority.LOW -> NotificationCompat.PRIORITY_LOW
            MessagePriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            MessagePriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            MessagePriority.URGENT -> NotificationCompat.PRIORITY_MAX
        }
    }

    private fun broadcastMessage(message: FirebaseMessage) {
        val intent = Intent("com.p4handheld.FIREBASE_MESSAGE_RECEIVED").apply {
            putExtra("messageId", message.id)
            putExtra("messageType", message.messageType.name)
            putExtra("title", message.title)
            putExtra("body", message.body)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}
