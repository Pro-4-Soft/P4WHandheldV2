package com.p4handheld.ui.screens.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.models.FirebaseGroup
import com.p4handheld.data.models.FirebaseMessage
import com.p4handheld.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FirebaseMessagesUiState(
    val messages: List<FirebaseMessage> = emptyList(),
    val groups: List<FirebaseGroup> = emptyList(),
    val selectedGroupId: String? = null,
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
        firebaseManager?.addMessageListener("messages_screen") { message ->
            refreshMessages()
        }
        
        _uiState.value = _uiState.value.copy(isInitialized = true)
        
        // Load initial data
        loadGroups()
        loadAllMessages()
    }
    
    fun loadGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val groups = firebaseManager?.getAvailableGroups() ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load groups: ${e.message}",
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
                    selectedGroupId = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load messages: ${e.message}"
                )
            }
        }
    }
    
    fun loadGroupMessages(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val messages = firebaseManager?.getGroupMessages(groupId) ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    selectedGroupId = groupId,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load group messages: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun subscribeToGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseManager?.subscribeToGroup(groupId) ?: false
                if (success) {
                    // Refresh groups to update subscription status
                    loadGroups()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to subscribe to group"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error subscribing to group: ${e.message}"
                )
            }
        }
    }
    
    fun unsubscribeFromGroup(groupId: String) {
        viewModelScope.launch {
            try {
                val success = firebaseManager?.unsubscribeFromGroup(groupId) ?: false
                if (success) {
                    // Refresh groups to update subscription status
                    loadGroups()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to unsubscribe from group"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error unsubscribing from group: ${e.message}"
                )
            }
        }
    }
    
    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            try {
                firebaseManager?.markMessageAsRead(messageId)
                refreshMessages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error marking message as read: ${e.message}"
                )
            }
        }
    }
    
    fun refreshMessages() {
        val currentGroupId = _uiState.value.selectedGroupId
        if (currentGroupId != null) {
            loadGroupMessages(currentGroupId)
        } else {
            loadAllMessages()
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
