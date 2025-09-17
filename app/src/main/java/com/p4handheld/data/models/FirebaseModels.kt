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
    val groupId: String? = null,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val priority: MessagePriority = MessagePriority.NORMAL
)

enum class MessageType {
    @SerializedName("notification")
    NOTIFICATION,
    
    @SerializedName("alert")
    ALERT,
    
    @SerializedName("system")
    SYSTEM,
    
    @SerializedName("task_update")
    TASK_UPDATE,
    
    @SerializedName("location_request")
    LOCATION_REQUEST,
    
    @SerializedName("group_message")
    GROUP_MESSAGE
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

data class FirebaseGroup(
    val groupId: String,
    val groupName: String,
    val description: String? = null,
    val isSubscribed: Boolean = false
)

data class FirebaseTokenRequest(
    val token: String,
    val userId: String,
    val deviceId: String,
    val groups: List<String> = emptyList()
)

data class FirebaseSubscriptionRequest(
    val token: String,
    val groupId: String,
    val subscribe: Boolean = true
)

data class MessageResponse(
    val success: Boolean,
    val message: String? = null,
    val messageId: String? = null
)

data class GroupMessagesRequest(
    val groupId: String,
    val limit: Int = 50,
    val offset: Int = 0,
    val since: Long? = null
)

data class GroupMessagesResponse(
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
    val groupId: String?,
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
            groupId = groupId,
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
        groupId = groupId,
        userId = userId,
        timestamp = timestamp,
        isRead = isRead,
        priority = priority.name
    )
}
