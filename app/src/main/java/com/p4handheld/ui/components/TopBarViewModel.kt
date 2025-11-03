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
import com.p4handheld.data.models.P4WEventType
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
        private val _persistentUiState = MutableStateFlow(TopBarUiState())
    }

    val uiState: StateFlow<TopBarUiState> = _persistentUiState.asStateFlow()
    private var registered = false

    init {
        viewModelScope.launch {
            if (!IsInitialized) {
                try {
                    var taskCount = 0
                    if (AuthRepository.hasTasks) {
                        val result = apiService.getAssignedTaskCount(AuthRepository.userId)
                        taskCount = result.body ?: 0
                    }
                    _persistentUiState.value = _persistentUiState.value.copy(
                        taskCount = taskCount,
                        hasUnreadMessages = AuthRepository.newMessages > 0,
                        isTrackingLocation = AuthRepository.trackGeoLocation,
                        username = AuthRepository.username
                    )
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
                    val eventType = intent.getStringExtra("eventType")
                    when (eventType) {
                        P4WEventType.TASKS_CHANGED.toString() -> {
                            val taskAdded = intent.getBooleanExtra("taskAdded", false)
                            val newCount = if (taskAdded) _persistentUiState.value.taskCount + 1 else maxOf(0, _persistentUiState.value.taskCount - 1)
                            _persistentUiState.value = _persistentUiState.value.copy(taskCount = newCount)
                            Log.d("TopBarViewModel", "Task count updated via broadcast: $newCount")
                        }

                        P4WEventType.USER_CHAT_MESSAGE.toString() -> {
                            // Update unread messages indicator
                            _persistentUiState.value = _persistentUiState.value.copy(hasUnreadMessages = true)
                            Log.d("TopBarViewModel", "Unread messages updated via broadcast")
                        }
                    }
                }

                GlobalConstants.Intents.LOCATION_STATUS_CHANGED -> {
                    val statusString = intent.getStringExtra("locationStatus")
                    val statusEnum = LocationStatus.valueOf(statusString ?: LocationStatus.DISABLED.toString())
                    _persistentUiState.value = _persistentUiState.value.copy(locationStatus = statusEnum)
                    Log.d("TopBarViewModel", "Location status updated via broadcast: $statusEnum")
                }
            }
        }
    }

    private fun registerReceiver() {
        if (!registered) {
            try {
                val appCtx = getApplication<Application>().applicationContext
                val intentFilter = IntentFilter().apply {
                    addAction(GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED)
                    addAction(GlobalConstants.Intents.LOCATION_STATUS_CHANGED)
                }
                ContextCompat.registerReceiver(appCtx, receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
                registered = true
                Log.d("TopBarViewModel", "BroadcastReceiver registered successfully")
            } catch (e: Exception) {
                Log.e("TopBarViewModel", "Failed to register BroadcastReceiver", e)
                registered = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (registered) {
            try {
                val appCtx = getApplication<Application>().applicationContext
                appCtx.unregisterReceiver(receiver)
                Log.d("TopBarViewModel", "BroadcastReceiver unregistered successfully")
            } catch (e: Exception) {
                Log.e("TopBarViewModel", "Failed to unregister BroadcastReceiver", e)
            } finally {
                registered = false
            }
        }
    }
}
