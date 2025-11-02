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
    }

    private val _uiState = MutableStateFlow(TopBarUiState())
    val uiState: StateFlow<TopBarUiState> = _uiState.asStateFlow()
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
                    _uiState.value = _uiState.value.copy(
                        taskCount = taskCount,
                        hasUnreadMessages = AuthRepository.newMessages > 0,
                        isTrackingLocation = AuthRepository.trackGeoLocation,
                        username = AuthRepository.username
                    )
                } catch (_: Exception) {
                }
                IsInitialized = true
                registerReceiver()
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED -> {
                    //refreshFromStorage()
                    val eventType = intent.getStringExtra("eventType")
                    if (eventType == P4WEventType.TASKS_CHANGED.toString()) {
                        val taskAdded = intent.getStringExtra("taskAdded").toBoolean()
                        val newCount = if (taskAdded) _uiState.value.taskCount + 1 else _uiState.value.taskCount - 1
                        viewModelScope.launch {
                            _uiState.value = _uiState.value.copy(taskCount = newCount)
                        }
                    }
                }

                GlobalConstants.Intents.LOCATION_STATUS_CHANGED -> {
                    val statusString = intent.getStringExtra("locationStatus")
                    val statusEnum = LocationStatus.valueOf(statusString ?: LocationStatus.DISABLED.toString())
                    _uiState.value = _uiState.value.copy(locationStatus = statusEnum)
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

//    override fun onCleared() {
//        super.onCleared()
//        if (registered) {
//            try {
//                val appCtx = getApplication<Application>().applicationContext
//                appCtx.unregisterReceiver(receiver)
//                Log.d("TopBarViewModel", "BroadcastReceiver unregistered successfully")
//            } catch (e: Exception) {
//                Log.e("TopBarViewModel", "Failed to unregister BroadcastReceiver", e)
//            } finally {
//                registered = false
//            }
//        }
//    }
}
