package com.p4handheld.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LoginRequest(
    @SerialName("Username")
    val username: String,
    @SerialName("Password")
    val password: String,
    @SerialName("HandheldNotificationToken")
    val handheldNotificationToken: String? = null
)

@Serializable
data class LoginResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("message")
    val message: String? = null,
    @SerialName("token")
    val token: String? = null
)

@Serializable
data class UserContextResponse(
    @SerialName("Menu")
    val menu: List<MenuItem>,

    @SerialName("TrackGeoLocation")
    val trackGeoLocation: Boolean,

    @SerialName("UserScanType")
    val userScanType: ScanType,

    @SerialName("UserId")
    val userId: String,

    @SerialName("LanguageId")
    val languageId: String,

    @SerialName("NewMessages")
    val newMessages: Int = 0,

    @SerialName("HasTasks")
    val hasTasks: Boolean = false
)

@Serializable
enum class ScanType() {
    @SerialName("ZebraDataWedge")
    ZEBRA_DATA_WEDGE,

    @SerialName("Camera")
    CAMERA,

    @SerialName("LineFeed")
    LINE_FEED;
}

@Serializable
data class MenuItem(
    @SerialName("Id")
    val id: String? = null,
    @SerialName("Label")
    val label: String,
    @SerialName("State")
    val state: String? = null,
    @SerialName("StateParams")
    val stateParams: JsonElement? = null, // Changed from Any? to String? for serialization
    @SerialName("Icon")
    val icon: String? = null,
    @SerialName("Children")
    val children: List<MenuItem> = emptyList(),
)

data class ApiError(
    override val message: String,
    val code: Int? = null
) : Throwable(message)

// Action View Models
@Serializable
enum class PromptType {
    @SerialName("Text")
    TEXT,

    @SerialName("Picker")
    PICKER,

    @SerialName("Scan")
    SCAN,

    @SerialName("GoToNewPage")
    GO_TO_NEW_PAGE,

    @SerialName("Photo")
    PHOTO,

    @SerialName("Sign")
    SIGN,

    @SerialName("Date")
    DATE,

    @SerialName("Confirm")
    CONFIRM,

    @SerialName("Number")
    NUMBER,

    NOT_SELECTED
}

@Serializable
data class PromptResponse(
    @SerialName("Prompt")
    val prompt: Prompt,
    @SerialName("Messages")
    val messages: List<Message> = emptyList(),
    @SerialName("ToolbarActions")
    val toolbarActions: List<ToolbarAction> = emptyList(),
    @SerialName("CommitAllMessages")
    val commitAllMessages: Boolean = false,
    @SerialName("CleanLastMessages")
    val cleanLastMessages: Int = 0,
    @SerialName("Title")
    val title: String? = null
)

@Serializable
data class ToolbarAction(
    @SerialName("\$id")
    val id: String? = null,
    @SerialName("Action")
    val action: String,
    @SerialName("Label")
    val label: String
)

@Serializable
data class Message(
    @SerialName("Id")
    val id: String = "",

    @SerialName("Title")
    val title: String = "",

    @SerialName("Subtitle")
    val subtitle: String? = "",

    @SerialName("Subtitle2")
    val subtitle2: String? = "",

    @SerialName("ImageResource")
    val imageResource: String? = null,

    val showLargePicture: Boolean = false,

    @SerialName("Severity")
    val severity: String = "",

    @SerialName("Localize")
    val localize: Boolean = false,

    @SerialName("IsActionable")
    val isActionable: Boolean = false,

    @SerialName("HandlerName")
    val handlerName: String? = null,

    @SerialName("ActionName")
    val actionName: String? = null,

    @SerialName("PromptValue")
    val promptValue: String? = null,

    @SerialName("TaskId")
    val taskId: String? = null,

    var isCommitted: Boolean = false,
    val state: PromptResponse? = null
)


@Serializable
data class Prompt(
    @SerialName("Value")
    val value: String? = "",
    @SerialName("ActionName")
    val actionName: String? = "",
    @SerialName("PromptType")
    val promptType: PromptType? = PromptType.NOT_SELECTED,
    @SerialName("PromptPlaceholder")
    val promptPlaceholder: String? = "",
    @SerialName("Items")
    val items: List<PromptItem>? = emptyList(),
    @SerialName("DefaultValue")
    val defaultValue: String? = ""
)

@Serializable
data class PromptItem(
    @SerialName("Value")
    val value: String,
    @SerialName("Label")
    val label: String,
    @SerialName("Info1")
    val info1: String? = null,
    @SerialName("Info2")
    val info2: String? = null
)

@Serializable
data class ProcessRequest(
    @SerialName("PromptValue")
    val promptValue: String?,
    @SerialName("ActionFor")
    val actionFor: String?,
    @SerialName("StateParams")
    val stateParams: String? = null // Changed from Any? to String? for serialization
)

// ==================== USER MESSAGES / CHATS ====================

@Serializable
data class UserContact(
    @SerialName("Id")
    val id: String,
    @SerialName("LastSeen")
    val lastSeen: String? = null,
    @SerialName("Username")
    val username: String,
    @SerialName("IsOnline")
    val isOnline: Boolean = false,
    @SerialName("NewMessages")
    val newMessages: Int = 0
)

@Serializable
data class UserTask(
    @SerialName("Id")
    val id: String
)

@Serializable
data class UserChatMessage(
    @SerialName("MessageId")
    val messageId: String,
    @SerialName("Timestamp")
    val timestamp: String,
    @SerialName("FromUserId")
    val fromUserId: String,
    @SerialName("FromUsername")
    val fromUsername: String,
    @SerialName("ToUserId")
    val toUserId: String,
    @SerialName("ToUsername")
    val toUsername: String,
    @SerialName("IsNew")
    val isNew: Boolean,
    @SerialName("Message")
    val message: String
)

@Serializable
data class SendMessageRequest(
    val toUserId: String,
    @SerialName("Message") val message: String
)