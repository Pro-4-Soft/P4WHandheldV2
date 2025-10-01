package com.p4handheld.ui.components

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    val hasNotifications: Boolean = false,
    val isTrackingLocation: Boolean = false,
    val username: String = ""
)

class TopBarViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TopBarUiState())
    val uiState: StateFlow<TopBarUiState> = _uiState.asStateFlow()

    private val authRepository = AuthRepository(application.applicationContext)
    private val firebaseManager = FirebaseManager.getInstance(application.applicationContext)
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.p4handheld.FIREBASE_MESSAGE_RECEIVED") {
                // On any FCM event, refresh flags; USER_CHAT_MESSAGE implies unread messages
                refreshFromManagers()
            }
        }
    }

    init {
        viewModelScope.launch {
            refreshFromManagers()
            registerReceiver()
        }
    }

    private fun registerReceiver() {
        if (!registered) {
            val appCtx = getApplication<Application>().applicationContext
            appCtx.registerReceiver(receiver, IntentFilter("com.p4handheld.FIREBASE_MESSAGE_RECEIVED"))
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
        val hasNotifs = firebaseManager.hasNotifications()
        _uiState.value = _uiState.value.copy(
            username = username,
            isTrackingLocation = tracking,
            hasUnreadMessages = hasUnread,
            hasNotifications = hasNotifs
        )
    }

    fun setUnreadMessagesCount(count: Int) {
        _uiState.value = _uiState.value.copy(hasUnreadMessages = count > 0)
    }

    fun setNotificationsCount(count: Int) {
        _uiState.value = _uiState.value.copy(hasNotifications = count > 0)
    }

    fun refreshTrackingFlag() {
        val tracking = authRepository.shouldTrackLocation()
        _uiState.value = _uiState.value.copy(isTrackingLocation = tracking)
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
