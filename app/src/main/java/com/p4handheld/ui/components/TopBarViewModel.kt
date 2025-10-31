package com.p4handheld.ui.components

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.GlobalConstants
import com.p4handheld.data.api.ApiClient.apiService
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.firebase.FirebaseManager
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
    private val firebaseManager = FirebaseManager.getInstance(application.applicationContext)
    private var registered = false


    init {
        updateLocationTrackingStatus()
        updateMessageNotificationStatus()
    }

    init {
        viewModelScope.launch {
            refreshFromStorage()
            if (!IsInitialized) {
                try {
                    var userId = authRepository.getUserId().toString()
                    val result = apiService.getAssignedTaskCount(userId)
                    _uiState.value = _uiState.value.copy(taskCount = result.body ?: 0)
                    firebaseManager.setTasksCount(result.body ?: 0)
                } catch (_: Exception) {
                }
                IsInitialized = true
            } else {
                // Ensure UI reflects current stored value even if already initialized in this process
                _uiState.value = _uiState.value.copy(taskCount = firebaseManager.getTasksCount())
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
        val username = ctx
            .getSharedPreferences(GlobalConstants.AppPreferences.AUTH_PREFS, Context.MODE_PRIVATE)
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

    private fun updateMessageNotificationStatus() {
        viewModelScope.launch {
            val hasUnread = firebaseManager.hasUnreadMessages()
            _uiState.value = _uiState.value.copy(hasUnreadMessages = hasUnread)
        }
    }

    private fun updateLocationTrackingStatus() {
        val isTracking = authRepository.shouldTrackLocation()
        _uiState.value = _uiState.value.copy(isTrackingLocation = isTracking)
    }
}
