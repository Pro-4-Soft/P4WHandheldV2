package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.Message
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.Prompt
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.PromptType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActionUiState(
    val isLoading: Boolean = false,
    val currentPrompt: Prompt? = null,
    val currentResponse: PromptResponse? = null,
    val messageStack: List<Message> = emptyList(),
    val promptValue: String = "",
    val errorMessage: String? = null,
    val showSignature: Boolean = false,
    val capturedImage: String? = null
)

class ActionViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiClient.apiService
    private val _uiState = MutableStateFlow(ActionUiState())
    val uiState: StateFlow<ActionUiState> = _uiState.asStateFlow()

    fun initAction(pageKey: String, initialValue: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentPrompt = null,
                currentResponse = null,
                messageStack = emptyList(),
                errorMessage = null
            )

            try {
                val result = apiService.initAction(pageKey, initialValue)

                if (result.isSuccessful && result.body != null) {
                    val response = result.body
                    val newMessages = response.messages

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentPrompt = response.prompt,
                        currentResponse = response,
                        messageStack = _uiState.value.messageStack + newMessages,
                        showSignature = response.prompt.promptType == PromptType.SIGN
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.errorMessage ?: "Failed to initialize action"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun processAction(pageKey: String, promptValue: Any?, actionFor: String? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(isLoading = true)

            try {
                val processRequest = ProcessRequest(
                    promptValue = promptValue,
                    actionFor = actionFor ?: currentState.currentPrompt?.actionName ?: "",
                    context = currentState.currentResponse?.context,
                    toolbarActions = currentState.currentResponse?.toolbarActions
                )

                val result = apiService.processAction(pageKey, processRequest)

                if (result.isSuccessful && result.body != null) {
                    val response = result.body

                    // Handle GoToNewPage prompt type
                    if (response.prompt.promptType == PromptType.GO_TO_NEW_PAGE) {
                        // For now, just reinitialize with the new page
                        val selectedItemId = response.context?.get("SelectedItemId")?.toString()
                        initAction(response.prompt.defaultValue, selectedItemId)
                        return@launch
                    }

                    val newMessages = response.messages

                    val updatedMessageStack = currentState.messageStack + newMessages;

                    // Add divider if IsStart is true
                    val finalMessageStack = if (response.isStart && updatedMessageStack.lastOrNull()?.value != "divider"
                    ) {
                        val commitedMessages = updatedMessageStack.map { it.copy(isCommitted = true) }
                        commitedMessages + Message(
                            value = "divider",
                            severity = "Info",
                            isCommitted = true
                        )
                    } else {
                        updatedMessageStack
                    }

                    _uiState.value = currentState.copy(
                        isLoading = false,
                        currentPrompt = response.prompt,
                        currentResponse = response,
                        messageStack = finalMessageStack,
                        promptValue = "",
                        showSignature = response.prompt.promptType == PromptType.SIGN,
                        capturedImage = null
                    )
                } else {
                    val errorMessages = currentState.messageStack + Message(
                        value = result.errorMessage ?: "Process failed",
                        severity = "Error"
                    )

                    _uiState.value = currentState.copy(
                        isLoading = false,
                        messageStack = errorMessages
                    )
                }
            } catch (e: Exception) {
                val errorMessages = currentState.messageStack + Message(
                    value = "Error: ${e.message}",
                    severity = "Error"
                )

                _uiState.value = currentState.copy(
                    isLoading = false,
                    messageStack = errorMessages
                )
            }
        }
    }

    fun updatePromptValue(value: String) {
        _uiState.value = _uiState.value.copy(promptValue = value)
    }

    fun onMessageClick(message: Message, index: Int) {
        val currentState = _uiState.value

        if (message.isCommitted) return

        if (currentState.currentPrompt?.promptType == PromptType.LIST) {
            processAction("", message.id)
            return
        }

        // Restore to clicked message state
        val clickedState = message.state as? PromptResponse
        if (clickedState != null) {
            val truncatedMessages = currentState.messageStack.take(index)
            _uiState.value = currentState.copy(
                currentPrompt = clickedState.prompt,
                currentResponse = clickedState,
                messageStack = truncatedMessages
            )
        }
    }

    fun setCapturedImage(imageBase64: String?) {
        _uiState.value = _uiState.value.copy(capturedImage = imageBase64)
    }

    fun setShowSignature(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSignature = show)
    }
}
