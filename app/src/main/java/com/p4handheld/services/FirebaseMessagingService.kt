package com.p4handheld.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.p4handheld.R
import com.p4handheld.data.models.P4WEventType
import com.p4handheld.data.models.P4WFirebaseNotification
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.firebase.FirebaseManager
import com.p4handheld.ui.screens.MainActivity

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "p4w_notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data payload: ${remoteMessage.data}")

        val p4wMessage = convertToP4WMessage(remoteMessage)

        if (isItMineUserMessageNotification(p4wMessage)) {
            return
        }

        showOrNoiseNotification(p4wMessage)
        // Update badges in prefs so TopBarViewModel can reflect state
        try {
            val manager = FirebaseManager.getInstance(applicationContext)
            manager.setHasNotifications(true)
            if (p4wMessage.eventType == P4WEventType.USER_CHAT_MESSAGE) {
                manager.setHasUnreadMessages(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update badge flags", e)
        }
        broadcastMessage(p4wMessage)
    }

    //region Private functions

    private fun isItMineUserMessageNotification(p4wMessage: P4WFirebaseNotification): Boolean {
        val userId = applicationContext
            .getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .getString("userId", "") ?: ""
        return p4wMessage.eventType == P4WEventType.USER_CHAT_MESSAGE && p4wMessage.userChatMessage?.fromUserId == userId
    }

    private fun convertToP4WMessage(remoteMessage: RemoteMessage): P4WFirebaseNotification {
        val data = remoteMessage.data
        val notification = remoteMessage.notification
        return P4WFirebaseNotification(
            id = data["messageId"] ?: remoteMessage.messageId ?: generateMessageId(),
            title = notification?.title ?: data["title"] ?: "",
            body = notification?.body ?: data["body"] ?: "",
            userChatMessage = parseUserChatMessage(data["payload"]),
            eventType = parseEventType(data["eventType"]),
            userId = data["userId"],
            timestamp = remoteMessage.sentTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
    }

    private fun parseUserChatMessage(payloadStr: String?): UserChatMessage? {
        if (payloadStr == null || payloadStr.isEmpty()) {
            return null
        }
        return try {
            val gson = Gson()
            gson.fromJson(payloadStr, UserChatMessage::class.java)
        } catch (ex: JsonSyntaxException) {
            ex.printStackTrace()
            null
        }
    }

    private fun parseEventType(type: String?): P4WEventType {
        return when (type) {
            "UserChatMessage" -> P4WEventType.USER_CHAT_MESSAGE
            "ScreenRequested" -> P4WEventType.SCREEN_REQUESTED
            "TasksChanged" -> P4WEventType.TASKS_CHANGED
            else -> P4WEventType.UNKNOWN
        }
    }

    private fun showOrNoiseNotification(message: P4WFirebaseNotification) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (message.eventType == P4WEventType.USER_CHAT_MESSAGE) {
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

            notificationManager.notify(message.id.hashCode(), notificationBuilder.build())
            val mediaPlayer = MediaPlayer.create(this, R.raw.new_message)
            mediaPlayer.start()
        } else {
            try {
                val sound = if (message.eventType == P4WEventType.TASKS_CHANGED)
                    R.raw.new_task
                else
                    R.raw.task_removed
                val mediaPlayer = MediaPlayer.create(this, sound)
                mediaPlayer.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun broadcastMessage(message: P4WFirebaseNotification) {
        val intent = Intent("com.p4handheld.FIREBASE_MESSAGE_RECEIVED").apply {
            putExtra("messageId", message.id)
            putExtra("eventType", message.eventType.name)
            putExtra("title", message.title)
            putExtra("body", message.body)
            // Include payload as JSON if present so UI can parse and append
            message.userChatMessage?.let {
                val json = Gson().toJson(it)
                putExtra("payload", json)
            }
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for app notifications"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel) // 🔹 important
        }
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    //endregion
}
