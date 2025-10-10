package com.p4handheld.firebase

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.p4handheld.GlobalConstants
import com.p4handheld.R
import com.p4handheld.data.ChatStateManager
import com.p4handheld.data.models.P4WEventType
import com.p4handheld.data.models.P4WFirebaseNotification
import com.p4handheld.data.models.UserChatMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data payload: ${remoteMessage.data}")

        val p4wNotification = convertToP4WMessage(remoteMessage)

        if (isItMineUserMessageNotification(p4wNotification)) {
            return
        }

        // checck if user is in chat screen
        val shouldSuppressNotification = shouldSuppressNotificationForMessage(p4wNotification)

        if (!shouldSuppressNotification) {
            // Play internal sounds only, no system notifications
            playInternalSounds(p4wNotification)
            try {
                val manager = FirebaseManager.getInstance(applicationContext)
                if (p4wNotification.eventType == P4WEventType.USER_CHAT_MESSAGE) {
                    manager.setHasUnreadMessages(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update badge flags", e)
            }
        }

        broadcastMessage(p4wNotification)
    }

    //region Private functions
    private fun isItMineUserMessageNotification(p4wMessage: P4WFirebaseNotification): Boolean {
        val userId = applicationContext
            .getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .getString("userId", "") ?: ""
        return p4wMessage.eventType == P4WEventType.USER_CHAT_MESSAGE && p4wMessage.userChatMessage?.fromUserId == userId
    }

    private fun shouldSuppressNotificationForMessage(p4wMessage: P4WFirebaseNotification): Boolean {
        if (p4wMessage.eventType != P4WEventType.USER_CHAT_MESSAGE) {
            return false
        }

        val fromUserId = p4wMessage.userChatMessage?.fromUserId
        return fromUserId != null && ChatStateManager.isViewingChatWith(fromUserId)
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
            taskAdded = data["taskAdded"] == "true" || data["taskAdded"] == "True",
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun playInternalSounds(message: P4WFirebaseNotification) {
        if (message.eventType == P4WEventType.USER_CHAT_MESSAGE) {
            // Only play sound for user messages, no system notification
            playMessageSound()
        }
        if (message.eventType == P4WEventType.TASKS_CHANGED) {
            try {
                if (message.taskAdded) {
                    val mediaPlayerNewTask = MediaPlayer.create(this, R.raw.new_task)
                    mediaPlayerNewTask?.start()
                } else {
                    val mediaPlayerTaskRemoved = MediaPlayer.create(this, R.raw.task_removed)
                    mediaPlayerTaskRemoved?.start()
                }
                // Also refresh tasks count and store in prefs so badge is accurate next time UI reads it
                try {
                    val manager = FirebaseManager.getInstance(applicationContext)
                    // Fire and forget; this is in background context
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            manager.refreshTasksCountFromServer()
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playMessageSound() {
        try {
            val mediaPlayer = MediaPlayer.create(this, R.raw.new_message)
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play message sound", e)
        }
    }

    private fun broadcastMessage(message: P4WFirebaseNotification) {
        val intent = Intent(GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED).apply {
            putExtra("messageId", message.id)
            putExtra("eventType", message.eventType.name)
            putExtra("title", message.title)
            putExtra("body", message.body)
            message.userChatMessage?.let {
                val json = Gson().toJson(it)
                putExtra("payload", json)
            }
        }
        sendBroadcast(intent)
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    //endregion
}