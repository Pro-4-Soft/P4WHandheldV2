package com.p4handheld.ui.screens.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.models.FirebaseEventType
import com.p4handheld.data.models.FirebaseMessage
import com.p4handheld.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FirebaseMessagesUiState(
    val messages: List<FirebaseMessage> = emptyList(),
    val eventTypes: List<FirebaseEventType> = emptyList(),
    val selectedEventType: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isInitialized: Boolean = false
)

class FirebaseMessagesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FirebaseMessagesUiState())
    val uiState: StateFlow<FirebaseMessagesUiState> = _uiState.asStateFlow()

    private var firebaseManager: FirebaseManager? = null

    fun initialize(context: Context) {
        if (_uiState.value.isInitialized) return

        firebaseManager = FirebaseManager.getInstance(context)

        // Add message listener
        _uiState.value = _uiState.value.copy(isInitialized = true)

        // Load initial data
        loadEventTypes()
        loadAllMessages()
    }

    fun loadEventTypes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val eventTypes = firebaseManager?.getAvailableEventTypes() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    eventTypes = eventTypes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load event types: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun loadAllMessages() {
        viewModelScope.launch {
            try {
                val messages = firebaseManager?.getAllMessages() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    selectedEventType = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load messages: ${e.message}"
                )
            }
        }
    }


    fun subscribeToEvent(eventType: String) {
        viewModelScope.launch {
            try {
                val success = firebaseManager?.subscribeToEvent(eventType) ?: false
                if (success) {
                    // Refresh event types to update subscription status
                    loadEventTypes()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to subscribe to event"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error subscribing to event: ${e.message}"
                )
            }
        }
    }

    fun unsubscribeFromEvent(eventType: String) {
        viewModelScope.launch {
            try {
                val success = firebaseManager?.unsubscribeFromEvent(eventType) ?: false
                if (success) {
                    // Refresh event types to update subscription status
                    loadEventTypes()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to unsubscribe from event"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error unsubscribing from event: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        firebaseManager?.removeMessageListener("messages_screen")
    }
}
