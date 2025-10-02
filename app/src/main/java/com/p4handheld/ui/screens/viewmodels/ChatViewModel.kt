package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.UserChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.util.UUID

data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<UserChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val isSending: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadMessages(contactId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = ApiClient.apiService.getMessages(contactId)
            if (result.isSuccessful) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = result.body ?: emptyList()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "Failed to load messages (code ${result.code})"
                )
            }
        }
    }

    fun sendMessage(toUserId: String, message: String, after: (() -> Unit)? = null) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
            val result = ApiClient.apiService.sendMessage(toUserId, message)
            if (result.isSuccessful) {
                val context = getApplication<Application>()
                val username = context.getSharedPreferences("auth_prefs", Application.MODE_PRIVATE)
                    .getString("username", "Me") ?: "Me"

                val newMsg = UserChatMessage(
                    messageId = UUID.randomUUID().toString(),
                    fromUserId = "",//me
                    toUserId = toUserId,
                    fromUsername = username,
                    toUsername = "",
                    message = message,
                    timestamp = OffsetDateTime.now().toString(),
                    isNew = true
                )

                val updated = _uiState.value.messages + newMsg
                _uiState.value = _uiState.value.copy(isSending = false, messages = updated)
                after?.invoke()
            } else {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    errorMessage = result.errorMessage ?: "Failed to send (code ${result.code})"
                )
            }
        }
    }

    // Append a message received via FCM broadcast
    fun appendIncomingMessage(message: UserChatMessage) {
        val current = _uiState.value.messages
        _uiState.value = _uiState.value.copy(messages = current + message)
    }
}
