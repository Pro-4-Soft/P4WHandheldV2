package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.models.ApiError
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.repository.AuthRepository
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
    val httpStatusCode: Int? = null
)

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()
    val unauthorizedEvent = MutableSharedFlow<Unit>()

    init {
        loadMenuData()
    }

    private fun loadMenuData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // First try to get stored menu data
                val storedMenu = AuthRepository.menu

                if (storedMenu != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        menuItems = storedMenu,
                        currentMenuItems = storedMenu,
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
        viewModelScope.launch {
            try {
                authRepository.logout(getApplication())
            } catch (e: Exception) {
                // Log error but don't prevent logout
                // User should still be logged out locally even if API call fails
                println("MenuViewModel: Logout error: ${e.message}")
            }
        }
    }

    fun navigateToSubMenu(menuItem: MenuItem) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            menuStack = currentState.menuStack + listOf(currentState.currentMenuItems),
            breadcrumbStack = currentState.breadcrumbStack + listOf(menuItem.label),
            currentMenuItems = menuItem.children
        )
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
}
