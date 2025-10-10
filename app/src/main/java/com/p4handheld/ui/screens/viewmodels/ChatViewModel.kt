package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.GlobalConstants
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.UserChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
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
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    val unauthorizedEvent = MutableSharedFlow<Unit>()
    private val pageSize = 50
    private var currentContactId: String? = null

    fun loadMessages(contactId: String) {
        viewModelScope.launch {
            currentContactId = contactId
            _uiState.value = ChatUiState(isLoading = true) // reset state
            val result = ApiClient.apiService.getMessages(contactId, skip = 0, take = pageSize)
            if (result.isSuccessful) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = result.body ?: emptyList(),
                    endReached = (result.body?.size ?: 0) < pageSize
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "Failed to load messages (code ${result.code})"
                )
            }
        }
    }

    fun loadMore() {
        val contactId = currentContactId ?: return
        val state = _uiState.value
        if (state.isLoadingMore || state.endReached) return
        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val skip = state.messages.size
            val result = ApiClient.apiService.getMessages(contactId, skip = skip, take = pageSize)
            if (result.isSuccessful) {
                val older = result.body ?: emptyList()
                val newEndReached = older.size < pageSize
                // Prepend older messages to the current list
                val combined = older + _uiState.value.messages
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    endReached = newEndReached,
                    messages = combined
                )
            } else {
                if (result.code == 401) {
                    unauthorizedEvent.emit(Unit)
                }
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    errorMessage = result.errorMessage ?: "Failed to load more (code ${result.code})"
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
                val username = context.getSharedPreferences(GlobalConstants.AppPreferences.AUTH_PREFS, Application.MODE_PRIVATE)
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
                if (result.code == 401) {
                    unauthorizedEvent.emit(Unit)
                }
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
