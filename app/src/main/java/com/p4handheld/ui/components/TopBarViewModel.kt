package com.p4handheld.ui.components

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopBarUiState(
    val hasUnreadMessages: Boolean = false,
    val isTrackingLocation: Boolean = false,
    val username: String = "",
    val taskCount: Int = 0
)

class TopBarViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        @Volatile
        private var hasFetchedTasksOnce: Boolean = false
    }

    private val _uiState = MutableStateFlow(TopBarUiState())
    val uiState: StateFlow<TopBarUiState> = _uiState.asStateFlow()

    private val authRepository = AuthRepository(application.applicationContext)
    private val firebaseManager = FirebaseManager.getInstance(application.applicationContext)
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.p4handheld.FIREBASE_MESSAGE_RECEIVED") {
                refreshFromManagers()
                val eventType = intent.getStringExtra("eventType")
                if (eventType == "TASKS_CHANGED") {
                    viewModelScope.launch {
                        try {
                            val manager = firebaseManager
                            val newCount = manager.refreshTasksCountFromServer()
                            _uiState.value = _uiState.value.copy(taskCount = newCount)
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            refreshFromManagers()
            if (!hasFetchedTasksOnce) {
                try {
                    firebaseManager.ensureTasksCountInitialized()
                    _uiState.value = _uiState.value.copy(taskCount = firebaseManager.getTasksCount())
                } catch (_: Exception) {
                }
                hasFetchedTasksOnce = true
            } else {
                // Ensure UI reflects current stored value even if already initialized in this process
                _uiState.value = _uiState.value.copy(taskCount = firebaseManager.getTasksCount())
            }
            registerReceiver()
        }
    }

    private fun registerReceiver() {
        if (!registered) {
            val appCtx = getApplication<Application>().applicationContext
            ContextCompat.registerReceiver(appCtx, receiver, IntentFilter("com.p4handheld.FIREBASE_MESSAGE_RECEIVED"), ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
        }
    }

    private fun refreshFromManagers() {
        val ctx = getApplication<Application>().applicationContext
        val username = ctx
            .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("username", "") ?: ""
        val tracking = authRepository.shouldTrackLocation()
        val hasUnread = firebaseManager.hasUnreadMessages()
        _uiState.value = _uiState.value.copy(
            username = username,
            isTrackingLocation = tracking,
            hasUnreadMessages = hasUnread
        )
    }

    override fun onCleared() {
        super.onCleared()
        if (registered) {
            val appCtx = getApplication<Application>().applicationContext
            appCtx.unregisterReceiver(receiver)
            registered = false
        }
    }
}
