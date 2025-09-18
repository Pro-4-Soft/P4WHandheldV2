package com.p4handheld.data.models

import com.google.gson.annotations.SerializedName

data class FirebaseMessage(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val data: Map<String, String> = emptyMap(),
    val eventType: P4WEventType = P4WEventType.USER_CHAT_MESSAGE,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class P4WEventType {
    @SerializedName("UserChatMessage")
    USER_CHAT_MESSAGE,

    @SerializedName("ScreenRequested")
    SCREEN_REQUESTED,

    @SerializedName("TasksChanged")
    TASKS_CHANGED,

    UNKNOWN
}

data class FirebaseTokenRequest(
    val token: String,
    val userId: String,
    val deviceId: String,
    val eventTypes: List<String> = emptyList()
)

data class MessageResponse(
    val success: Boolean,
    val message: String? = null,
    val messageId: String? = null
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
            eventType = P4WEventType.valueOf(messageType.uppercase()),
            userId = userId,
            timestamp = timestamp
        )
    }
}