package com.p4handheld.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class P4WFirebaseNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val userChatMessage: UserChatMessage?,
    val eventType: P4WEventType = P4WEventType.UNKNOWN,
    val userId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val taskAdded: Boolean
)

@Serializable
enum class P4WEventType {
    @SerialName("UserChatMessage")
    USER_CHAT_MESSAGE,

    @SerialName("ScreenRequested")
    SCREEN_REQUESTED,

    @SerialName("TasksChanged")
    TASKS_CHANGED,

    UNKNOWN
}

@Serializable
data class MessageResponse(
    val success: Boolean,
    val message: String? = null,
    val messageId: String? = null
)