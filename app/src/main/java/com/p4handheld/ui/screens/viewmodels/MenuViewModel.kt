package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.models.ApiError
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MenuUiState(
    val isLoading: Boolean = false,
    val menuItems: List<MenuItem> = emptyList(),
    val currentMenuItems: List<MenuItem> = emptyList(),
    val menuStack: List<List<MenuItem>> = emptyList(),
    val breadcrumbStack: List<String> = emptyList(),
    val errorMessage: String? = null,
    val selectedMenuItem: MenuItem? = null,
    val httpStatusCode: Int? = null,
    val isTrackingLocation: Boolean = false,
    val hasUnreadMessages: Boolean = false
)

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val firebaseManager = FirebaseManager.getInstance(application)
    val unauthorizedEvent = MutableSharedFlow<Unit>()
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        loadMenuData()
        updateLocationTrackingStatus()
        updateMessageNotificationStatus()
    }

    private fun loadMenuData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // First try to get stored menu data
                val storedMenu = authRepository.getStoredMenuData()

                if (storedMenu != null) {
                    _uiState.value = _uiState.value.copy(
                        menuItems = storedMenu.menu,
                        currentMenuItems = storedMenu.menu,
                    )
                } else {
                    // If no stored data, try to fetch from API
                    val menuResult = authRepository.getUserContext()

                    if (menuResult.isSuccess) {
                        val menuResponse = menuResult.getOrNull()!!
                        _uiState.value = _uiState.value.copy(
                            menuItems = menuResponse.menu,
                            currentMenuItems = menuResponse.menu,
                            isLoading = false
                        )
                    } else {
                        val exception = menuResult.exceptionOrNull()
                        if (exception is ApiError && exception.code == 401) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load menu: ${menuResult.exceptionOrNull()?.message}",
                                httpStatusCode = exception.code
                            )
                            unauthorizedEvent.emit(Unit)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to load menu: ${menuResult.exceptionOrNull()?.message}",
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Unexpected error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun refreshMenu() {
        loadMenuData()
    }

    fun navigateToMenuItem(menuItem: MenuItem) {
        val currentState = _uiState.value
        if (menuItem.children.isNotEmpty()) {
            // Navigate deeper into menu hierarchy
            _uiState.value = currentState.copy(
                currentMenuItems = menuItem.children,
                menuStack = currentState.menuStack + listOf(currentState.currentMenuItems),
                breadcrumbStack = currentState.breadcrumbStack + listOf(menuItem.label),
            )
        } else {
            // Set selected menu item for action navigation
            _uiState.value = currentState.copy(
                selectedMenuItem = menuItem,
            )
        }
    }

    fun navigateBack() {
        val currentState = _uiState.value
        if (currentState.menuStack.isNotEmpty()) {
            _uiState.value = currentState.copy(
                currentMenuItems = currentState.menuStack.last(),
                menuStack = currentState.menuStack.dropLast(1),
                breadcrumbStack = currentState.breadcrumbStack.dropLast(1),
            )
        }
    }

    fun resetToMainMenu() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            currentMenuItems = currentState.menuItems,
        )
    }

    private fun updateLocationTrackingStatus() {
        val isTracking = authRepository.shouldTrackLocation()
        _uiState.value = _uiState.value.copy(isTrackingLocation = isTracking)
    }

    private fun updateMessageNotificationStatus() {
        viewModelScope.launch {
            val hasUnread = firebaseManager.hasUnreadMessages()
            _uiState.value = _uiState.value.copy(hasUnreadMessages = hasUnread)
        }
    }

    fun refreshStatus() {
        updateLocationTrackingStatus()
        updateMessageNotificationStatus()
    }
}
