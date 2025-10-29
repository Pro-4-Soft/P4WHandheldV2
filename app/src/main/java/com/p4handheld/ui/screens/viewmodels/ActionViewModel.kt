package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.Message
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.Prompt
import com.p4handheld.data.models.PromptType
import com.p4handheld.data.models.ToolbarAction
import com.p4handheld.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActionUiState(
    val isLoading: Boolean = false,
    val pageTitle: String? = null,
    val pageKey: String? = null,
    val currentPrompt: Prompt = Prompt(),
    val toolbarActions: List<ToolbarAction> = emptyList(),
    val messageStack: List<Message> = emptyList(),
    val promptValue: String = "",
    val errorMessage: String? = null,
    val capturedImage: String? = null
)

class ActionViewModel(application: Application) : AndroidViewModel(application) {
    private val apiService = ApiClient.apiService
    private val authRepository = AuthRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(ActionUiState())
    private val baseTenantUrl = authRepository.getBaseTenantUrl()

    val uiState: StateFlow<ActionUiState> = _uiState.asStateFlow()
    val unauthorizedEvent = MutableSharedFlow<Unit>()

    fun processAction(promptValue: String? = null, actionFor: String? = null, taskId: String? = null) {
        viewModelScope.launch {
            setLoading(true)
            val currentState = _uiState.value
            val currentPageKey = _uiState.value.pageKey ?: ""
            _uiState.value = currentState.copy(
                isLoading = true,
                pageTitle = uiState.value.pageTitle,
                pageKey = uiState.value.pageKey
            )

            try {
                val processRequest = ProcessRequest(
                    promptValue = promptValue,
                    actionFor = actionFor ?: currentState.currentPrompt.actionName,
                    stateParams = authRepository.getStateParamsForPage(currentPageKey)
                )

                val result = apiService.processAction(currentPageKey, processRequest, taskId)
                if (result.isSuccessful && result.body != null) {
                    val response = result.body

                    if (response.prompt.promptType == PromptType.NOT_SELECTED) {
                        updateUiStateWithErrorMessage("Action cannot be processed because prompt type is not selected. ")
                        setLoading(false)
                        return@launch
                    }

                    if (response.prompt.promptType != PromptType.GO_TO_NEW_PAGE && response.prompt.actionName.isNullOrBlank()) {
                        updateUiStateWithErrorMessage("Action cannot be processed because action name is missed. ")
                        setLoading(false)
                        return@launch
                    }

                    // If we just sent an image (Photo/Sign), append a separate image message.
                    val newMessages = if (promptValue?.startsWith("data:image") == true) {
                        addTakenPictureToMessageStack(response.messages, promptValue)
                    } else {
                        response.messages.map { message ->
                            message.copy(
                                imageResource = if (message.imageResource?.isNotBlank() == true) {
                                    "$baseTenantUrl/resource/${message.imageResource}/medium"
                                } else {
                                    message.imageResource
                                }
                            )
                        }
                    }

                    val updatedMessageStack = currentState.messageStack.dropLast(response.cleanLastMessages) + newMessages

                    val finalMessageStack = tryCommitMessagesWithDivider(updatedMessageStack, response.commitAllMessages)

                    _uiState.value = currentState.copy(
                        isLoading = false,
                        currentPrompt = response.prompt,
                        messageStack = finalMessageStack,
                        pageTitle = if (response.title.isNullOrBlank()) currentState.pageTitle else "",
                        toolbarActions = response.toolbarActions
                    )
                } else {
                    if (result.code == 401) {
                        unauthorizedEvent.emit(Unit)
                        return@launch
                    }
                    updateUiStateWithErrorMessage(result.errorMessage ?: "Process failed")
                }
            } catch (e: Exception) {
                updateUiStateWithErrorMessage("${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    fun updatePageKey(newPageKey: String, pageTitle: String? = null) {
        _uiState.value = _uiState.value.copy(pageKey = newPageKey, pageTitle = pageTitle)
    }

    fun updatePromptValue(value: String) {
        _uiState.value = _uiState.value.copy(promptValue = value)
    }

    fun onMessageClick(message: Message, index: Int) {
        val currentState = _uiState.value

        if (message.isCommitted)
            return

        // If server requested navigation to a new page, process the message on that page directly
        if (currentState.currentPrompt.promptType == PromptType.GO_TO_NEW_PAGE) {
            _uiState.value = currentState.copy(
                messageStack = emptyList(),
                pageKey = message.handlerName
            )
            processAction(
                promptValue = message.promptValue ?: "",
                taskId = message.taskId
            )
            return
        }

        // Restore to clicked message state
        val clickedState = message.state
        val truncatedMessages = currentState.messageStack.take(index)

        if (message.isActionable) {
            _uiState.value = currentState.copy(messageStack = truncatedMessages)
            processAction(currentState.promptValue.ifBlank { message.promptValue ?: "" }, message.actionName ?: message.handlerName, taskId = message.taskId)
            return
        }

        if (clickedState != null) {
            _uiState.value = currentState.copy(
                currentPrompt = clickedState.prompt ?: Prompt(),
                messageStack = truncatedMessages
            )
        }
    }

    fun setCapturedImage(imageBase64: String?) {
        _uiState.value = _uiState.value.copy(capturedImage = imageBase64)
    }

    fun messageStackCleanUp() {
        _uiState.value = _uiState.value.copy(
            messageStack = emptyList(),
            pageTitle = _uiState.value.pageTitle,
            toolbarActions = _uiState.value.toolbarActions,
        )
    }

    //region Private methods
    private fun addTakenPictureToMessageStack(
        messages: List<Message>,
        imageResource: String
    ): List<Message> {
        val imageMsg = Message(
            imageResource = imageResource,
            showLargePicture = true,
            severity = "Success"
        )
        return messages + imageMsg
    }

    private fun updateUiStateWithErrorMessage(errorMessage: String) {
        val errorMessages = uiState.value.messageStack + Message(
            title = "Error: $errorMessage",
            severity = "Error"
        )

        _uiState.value = uiState.value.copy(
            isLoading = false,
            pageTitle = uiState.value.pageTitle,
            pageKey = uiState.value.pageKey,
            messageStack = errorMessages
        )
    }

    private fun tryCommitMessagesWithDivider(updatedMessageStack: List<Message>, commitAllMessages: Boolean): List<Message> {
        return if (commitAllMessages) {
            val commitedMessages = updatedMessageStack.map { it.copy(isCommitted = true) }
            commitedMessages + Message(
                title = "divider",
                severity = "Info",
                isCommitted = true
            )
        } else {
            updatedMessageStack
        }
    }

    private fun setLoading(state: Boolean) {
        _uiState.value = uiState.value.copy(
            isLoading = state,
            pageTitle = uiState.value.pageTitle,
            pageKey = uiState.value.pageKey,
            promptValue = ""
        )
    }
    //endregion
}
