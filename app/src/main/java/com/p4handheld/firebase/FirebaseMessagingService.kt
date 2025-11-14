package com.p4handheld.firebase

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.p4handheld.GlobalConstants
import com.p4handheld.R
import com.p4handheld.data.ChatStateManager
import com.p4handheld.data.models.P4WEventType
import com.p4handheld.data.models.P4WFirebaseNotification
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.repository.AuthRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    // JSON configuration for Kotlinx Serialization
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data payload: ${remoteMessage.data}")

        val p4wNotification = convertToP4WMessage(remoteMessage)

        if (ignoreNotification(p4wNotification)) {
            return
        }

        // check if user is in chat screen
        val shouldSuppressNotification = shouldSuppressNotificationForMessage(p4wNotification)
        if (!shouldSuppressNotification) {
            playInternalSounds(p4wNotification)
        }

        broadcastMessage(p4wNotification)
    }

    //region Private functions
    private fun ignoreNotification(p4wMessage: P4WFirebaseNotification): Boolean {
        val userId = AuthRepository.userId
        val isLoggedIn = AuthRepository.isLoggedIn
        val hasToken = AuthRepository.token.isNotEmpty()

        // If user is not logged in or doesn't have valid token, ignore all notifications
        if (!isLoggedIn || !hasToken) {
            Log.d(TAG, "User not logged in or no valid token, ignoring notification")
            return true
        }

        // Check if this is a chat message from the current user (should be ignored)
        val isItMineChatMessage = p4wMessage.eventType == P4WEventType.USER_CHAT_MESSAGE &&
                p4wMessage.userChatMessage?.fromUserId == userId

        if (isItMineChatMessage) {
            Log.d(TAG, "Ignoring notification from current user")
        }

        return isItMineChatMessage
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
            id = data["messageId"] ?: remoteMessage.messageId ?: "msg_${System.currentTimeMillis()}_${(1000..9999).random()}",
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
            json.decodeFromString<UserChatMessage>(payloadStr)
        } catch (ex: SerializationException) {
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
            try {
                val mediaPlayer = MediaPlayer.create(this, R.raw.new_message)
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play message sound", e)
            }
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun broadcastMessage(message: P4WFirebaseNotification) {
        val intent = Intent(GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED).apply {
            putExtra("messageId", message.id)
            putExtra("fromUserId", message.userChatMessage?.fromUserId)
            putExtra("eventType", message.eventType.name)
            putExtra("title", message.title)
            putExtra("body", message.body)
            putExtra("taskAdded", message.taskAdded)
            message.userChatMessage?.let {
                val jsonString = json.encodeToString(it)
                putExtra("payload", jsonString)
            }
        }
        sendBroadcast(intent)
    }

    //endregion
}