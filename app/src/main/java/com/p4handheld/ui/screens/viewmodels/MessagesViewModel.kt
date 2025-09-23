package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MessagesUiState(
    val isLoadingContacts: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val contacts: List<UserContact> = emptyList(),
    val selectedContact: UserContact? = null,
    val messages: List<UserChatMessage> = emptyList(),
    val errorMessage: String? = null
)

class MessagesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingContacts = true, errorMessage = null)
            val result = ApiClient.apiService.getContacts()
            if (result.isSuccessful) {
                val contacts = result.body ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoadingContacts = false,
                    contacts = contacts
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingContacts = false,
                    errorMessage = result.errorMessage ?: "Failed to load contacts (code ${result.code})"
                )
            }
        }
    }

    fun selectContact(contact: UserContact) {
        _uiState.value = _uiState.value.copy(selectedContact = contact, messages = emptyList())
        loadMessages(contact.id)
    }

    fun loadMessages(contactId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMessages = true, errorMessage = null)
            val result = ApiClient.apiService.getMessages(contactId)
            if (result.isSuccessful) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMessages = false,
                    messages = result.body ?: emptyList()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingMessages = false,
                    errorMessage = result.errorMessage ?: "Failed to load messages (code ${result.code})"
                )
            }
        }
    }
}
