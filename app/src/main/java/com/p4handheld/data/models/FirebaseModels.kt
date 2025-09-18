package com.p4handheld.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Firebase Cloud Messaging data models for P4WHandheldV2
 */

data class FirebaseMessage(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val data: Map<String, String> = emptyMap(),
    val messageType: MessageType = MessageType.NOTIFICATION,
    val eventType: String? = null,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val priority: MessagePriority = MessagePriority.NORMAL
)

enum class MessageType {
    @SerializedName("user_chat_message")
    USER_CHAT_MESSAGE,
    
    @SerializedName("tasks_changed")
    TASKS_CHANGED,
    
    @SerializedName("screen_requested")
    SCREEN_REQUESTED,
    
    @SerializedName("system")
    SYSTEM,
    
    @SerializedName("alert")
    ALERT,
    
    @SerializedName("notification")
    NOTIFICATION
}

enum class MessagePriority {
    @SerializedName("low")
    LOW,
    
    @SerializedName("normal")
    NORMAL,
    
    @SerializedName("high")
    HIGH,
    
    @SerializedName("urgent")
    URGENT
}

data class FirebaseEventType(
    val eventType: String,
    val eventName: String,
    val description: String? = null,
    val isSubscribed: Boolean = false
)

data class FirebaseTokenRequest(
    val token: String,
    val userId: String,
    val deviceId: String,
    val eventTypes: List<String> = emptyList()
)

data class FirebaseEventSubscriptionRequest(
    val token: String,
    val eventType: String,
    val subscribe: Boolean = true
)

data class MessageResponse(
    val success: Boolean,
    val message: String? = null,
    val messageId: String? = null
)

data class EventMessagesRequest(
    val eventType: String? = null,
    val limit: Int = 50,
    val offset: Int = 0,
    val since: Long? = null
)

data class EventMessagesResponse(
    val messages: List<FirebaseMessage>,
    val hasMore: Boolean = false,
    val totalCount: Int = 0
)

/**
 * Local storage model for Firebase messages
 */
data class StoredFirebaseMessage(
    val id: String,
    val title: String,
    val body: String,
    val data: String, // JSON string of data map
    val messageType: String,
    val eventType: String?,
    val userId: String?,
    val timestamp: Long,
    val isRead: Boolean,
    val priority: String
) {
    fun toFirebaseMessage(): FirebaseMessage {
        val dataMap = try {
            com.google.gson.Gson().fromJson(data, Map::class.java) as? Map<String, String> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap<String, String>()
        }
        
        return FirebaseMessage(
            id = id,
            title = title,
            body = body,
            data = dataMap,
            messageType = MessageType.valueOf(messageType.uppercase()),
            eventType = eventType,
            userId = userId,
            timestamp = timestamp,
            isRead = isRead,
            priority = MessagePriority.valueOf(priority.uppercase())
        )
    }
}

fun FirebaseMessage.toStoredMessage(): StoredFirebaseMessage {
    return StoredFirebaseMessage(
        id = id,
        title = title,
        body = body,
        data = com.google.gson.Gson().toJson(data),
        messageType = messageType.name,
        eventType = eventType,
        userId = userId,
        timestamp = timestamp,
        isRead = isRead,
        priority = priority.name
    )
}
