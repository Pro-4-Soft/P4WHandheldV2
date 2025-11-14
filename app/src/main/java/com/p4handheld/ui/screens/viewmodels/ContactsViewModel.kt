package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.UserContact
import com.p4handheld.ui.components.TopBarViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<UserContact> = emptyList(),
    val errorMessage: String? = null
)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = ApiClient.apiService.getContacts()
            if (result.isSuccessful) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    contacts = result.body ?: emptyList()
                )
                checkAndUpdateTopBarUnreadStatus(_uiState.value.contacts)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.errorMessage ?: "Failed to load contacts (code ${result.code})"
                )
            }
        }
    }

    // Increment unread messages counter for a single contact id
    fun incrementUnread(contactId: String) {
        val current = _uiState.value.contacts.toMutableList()
        val idx = current.indexOfFirst { it.id == contactId }
        if (idx >= 0) {
            val c = current[idx]
            current[idx] = c.copy(newMessages = c.newMessages + 1)
            _uiState.value = _uiState.value.copy(contacts = current)
            TopBarViewModel.PersistentUiState.value = TopBarViewModel.PersistentUiState.value.copy(hasUnreadMessages = true)
        } else {
            // Contact list might be outdated; try a refresh
            refresh()
        }
    }

    // Clear unread counter for a specific contact (e.g., when opening their chat)
    fun clearUnread(contactId: String) {
        val current = _uiState.value.contacts.toMutableList()
        val idx = current.indexOfFirst { it.id == contactId }
        if (idx >= 0) {
            val c = current[idx]
            if (c.newMessages != 0) {
                current[idx] = c.copy(newMessages = 0)
                _uiState.value = _uiState.value.copy(contacts = current)
            }
        }
    }

    fun checkAndUpdateTopBarUnreadStatus(contacts: List<UserContact>) {
        try {
            TopBarViewModel.PersistentUiState.value = TopBarViewModel.PersistentUiState.value.copy(hasUnreadMessages = contacts.any { it.newMessages > 0 })
        } catch (e: Exception) {
            android.util.Log.e("ContactsScreen", "Failed to update TopBar unread status", e)
        }
    }
}
