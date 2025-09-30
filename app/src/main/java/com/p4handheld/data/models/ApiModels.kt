package com.p4handheld.data.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("Username")
    val username: String,
    @SerializedName("Password")
    val password: String,
    @SerializedName("HandheldNotificationToken")
    val handheldNotificationToken: String? = null
)

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("token")
    val token: String? = null
)

data class UserContextResponse(
    @SerializedName("Menu")
    val menu: List<MenuItem>,
    @SerializedName("TrackGeoLocation")
    val trackGeoLocation: Boolean,

    @SerializedName("UserScanType")
    val userScanType: String,

    @SerializedName("TenantScanType")
    val tenantScanType: String
)

data class MenuItem(
    @SerializedName("Id")
    val id: String? = null,
    @SerializedName("Label")
    val label: String,
    @SerializedName("State")
    val state: String? = null,
    @SerializedName("StateParams")
    val stateParams: Any? = null,
    @SerializedName("Icon")
    val icon: String? = null,
    @SerializedName("Children")
    val children: List<MenuItem> = emptyList()
)

data class ApiError(
    override val message: String,
    val code: Int? = null
) : Throwable(message)

// Action View Models
enum class PromptType {
    @SerializedName("Text")
    TEXT,

    @SerializedName("Picker")
    PICKER,

    @SerializedName("Scan")
    SCAN,

    @SerializedName("GoToNewPage")
    GO_TO_NEW_PAGE,

    @SerializedName("Photo")
    PHOTO,

    @SerializedName("Sign")
    SIGN,

    @SerializedName("Date")
    DATE,

    @SerializedName("Confirm")
    CONFIRM,

    @SerializedName("Number")
    NUMBER
}

data class PromptResponse(
    @SerializedName("Prompt")
    val prompt: Prompt,
    @SerializedName("Messages")
    val messages: List<Message> = emptyList(),
    @SerializedName("ToolbarActions")
    val toolbarActions: List<ToolbarAction> = emptyList(),
    @SerializedName("CommitAllMessages")
    val commitAllMessages: Boolean = false,
    @SerializedName("CleanLastMessages")
    val cleanLastMessages: Number = 0
)

data class ToolbarAction(
    @SerializedName("\$id")
    val id: String? = null,
    @SerializedName("Action")
    val action: String,
    @SerializedName("Label")
    val label: String
)

data class Message(
    @SerializedName("Id")
    val id: String = "",

    @SerializedName("Title")
    val title: String = "",

    @SerializedName("Subtitle")
    val subtitle: String? = "",

    @SerializedName("Subtitle2")
    val subtitle2: String? = "",

    @SerializedName("ImageResource")
    val imageResource: String? = null,

    @SerializedName("Severity")
    val severity: String = "",

    @SerializedName("Localize")
    val localize: Boolean = false,

    @SerializedName("IsActionable")
    val isActionable: Boolean = false,

    @SerializedName("HandlerName")
    val handlerName: String? = null,

    @SerializedName("ActionName")
    val actionName: String? = null,

    @SerializedName("PromptValue")
    val promptValue: String? = null,

    @SerializedName("TaskId")
    val taskId: String? = null,

    var isCommitted: Boolean = false,
    val state: PromptResponse? = null
)


data class Prompt(
    @SerializedName("Value")
    val value: String = "",
    @SerializedName("ActionName")
    val actionName: String = "",
    @SerializedName("PromptType")
    val promptType: PromptType,
    @SerializedName("PromptPlaceholder")
    val promptPlaceholder: String = "",
    @SerializedName("Items")
    val items: List<PromptItem> = emptyList(),
    @SerializedName("DefaultValue")
    val defaultValue: String = ""
)

data class PromptItem(
    @SerializedName("Value")
    val value: String,
    @SerializedName("Label")
    val label: String,
    @SerializedName("Info1")
    val info1: String? = null,
    @SerializedName("Info2")
    val info2: String? = null
)

data class ProcessRequest(
    @SerializedName("PromptValue")
    val promptValue: String?,
    @SerializedName("ActionFor")
    val actionFor: String?
)

// ==================== USER MESSAGES / CHATS ====================

data class UserContact(
    @SerializedName("Id")
    val id: String,
    @SerializedName("LastSeen")
    val lastSeen: String? = null,
    @SerializedName("Username")
    val username: String,
    @SerializedName("IsOnline")
    val isOnline: Boolean = false,
    @SerializedName("NewMessages")
    val newMessages: Int = 0
)

data class UserChatMessage(
    @SerializedName("MessageId")
    val messageId: String,
    @SerializedName("Timestamp")
    val timestamp: String,
    @SerializedName("FromUserId")
    val fromUserId: String,
    @SerializedName("FromUsername")
    val fromUsername: String,
    @SerializedName("ToUserId")
    val toUserId: String,
    @SerializedName("ToUsername")
    val toUsername: String,
    @SerializedName("IsNew")
    val isNew: Boolean,
    @SerializedName("Message")
    val message: String
)
