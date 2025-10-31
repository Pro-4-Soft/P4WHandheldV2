package com.p4handheld.ui.components

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.GlobalConstants
import com.p4handheld.data.api.ApiClient.apiService
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.services.LocationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopBarUiState(
    val hasUnreadMessages: Boolean = false,
    val isTrackingLocation: Boolean = false,
    val locationStatus: LocationStatus = LocationStatus.DISABLED,
    val username: String = "",
    val taskCount: Int = 0
)

class TopBarViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        @Volatile
        private var IsInitialized: Boolean = false
    }

    private val _uiState = MutableStateFlow(TopBarUiState())
    val uiState: StateFlow<TopBarUiState> = _uiState.asStateFlow()

    private val authRepository = AuthRepository(application.applicationContext)
    private var registered = false

    init {
        viewModelScope.launch {
            refreshFromStorage()
            if (!IsInitialized) {
                try {
                    var userId = authRepository.getUserId().toString()
                    val result = apiService.getAssignedTaskCount(userId)
                    _uiState.value = _uiState.value.copy(taskCount = result.body ?: 0)
                } catch (_: Exception) {
                }
                IsInitialized = true
            }
            registerReceiver()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED -> {
                    refreshFromStorage()
                    val eventType = intent.getStringExtra("eventType")
                    if (eventType == "TASKS_CHANGED") {
                        val taskAdded = intent.getStringExtra("taskAdded").toBoolean()
                        val newCount = if (taskAdded) _uiState.value.taskCount + 1 else _uiState.value.taskCount - 1
                        viewModelScope.launch {
                            _uiState.value = _uiState.value.copy(taskCount = newCount)
                        }
                    }
                }

                GlobalConstants.Intents.LOCATION_STATUS_CHANGED -> {
                    val statusString = intent.getStringExtra("locationStatus")
                    val statusEnum = LocationStatus.valueOf(statusString ?: "DISABLED")
                    _uiState.value = _uiState.value.copy(locationStatus = statusEnum)
                }
            }
        }
    }

    private fun registerReceiver() {
        if (!registered) {
            val appCtx = getApplication<Application>().applicationContext
            val intentFilter = IntentFilter().apply {
                addAction(GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED)
                addAction(GlobalConstants.Intents.LOCATION_STATUS_CHANGED)
            }
            ContextCompat.registerReceiver(appCtx, receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
        }
    }

    private fun refreshFromStorage() {
        val ctx = getApplication<Application>().applicationContext
        val authPrefs = ctx.getSharedPreferences(GlobalConstants.AppPreferences.AUTH_PREFS, Context.MODE_PRIVATE)
        val firebasePrefs = ctx.getSharedPreferences(GlobalConstants.AppPreferences.FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)

        val username = authPrefs.getString("username", "") ?: ""
        val tracking = authRepository.shouldTrackLocation()
        val hasUnreadMessages = firebasePrefs.getBoolean("has_unread_messages", false)
        val newMessages = authPrefs.getInt("newMessages", 0)
        val hasTasks = authPrefs.getBoolean("hasTasks", false)

        _uiState.value = _uiState.value.copy(
            username = username,
            isTrackingLocation = tracking,
            hasUnreadMessages = hasUnreadMessages || newMessages > 0
        )

        // If user has tasks, fetch the actual count from server
        if (hasTasks) {
            viewModelScope.launch {
                try {
                    val userId = authRepository.getUserId()
                    if (!userId.isNullOrEmpty()) {
                        val result = apiService.getAssignedTaskCount(userId)
                        if (result.isSuccessful && result.body != null) {
                            _uiState.value = _uiState.value.copy(taskCount = result.body)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TopBarViewModel", "Failed to fetch task count", e)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(taskCount = 0)
        }
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
