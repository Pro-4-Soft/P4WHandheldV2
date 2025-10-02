package com.p4handheld.data.models

import com.google.gson.annotations.SerializedName

data class P4WFirebaseNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val userChatMessage: UserChatMessage?,
    val eventType: P4WEventType = P4WEventType.UNKNOWN,
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