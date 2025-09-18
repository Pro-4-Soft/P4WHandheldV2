package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.repository.AuthRepository
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
    val tenant: String = "",
    val errorMessage: String? = null,
    val selectedMenuItem: MenuItem? = null
)

class MenuViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        loadMenuData()
    }

    private fun loadMenuData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // First try to get stored menu data
                val storedMenu = authRepository.getStoredMenuData()

                if (storedMenu != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        menuItems = storedMenu.menu,
                        currentMenuItems = storedMenu.menu,
                        errorMessage = null
                    )
                } else {
                    // If no stored data, try to fetch from API
                    val menuResult = authRepository.getUserContext()

                    if (menuResult.isSuccess) {
                        val menuResponse = menuResult.getOrNull()!!
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            menuItems = menuResponse.menu,
                            currentMenuItems = menuResponse.menu,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load menu: ${menuResult.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unexpected error: ${e.message}"
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
                breadcrumbStack = currentState.breadcrumbStack + listOf(menuItem.label)
            )
        } else {
            // Set selected menu item for action navigation
            _uiState.value = currentState.copy(
                selectedMenuItem = menuItem
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
                selectedMenuItem = null
            )
        }
    }

    fun resetToMainMenu() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            currentMenuItems = currentState.menuItems,
            menuStack = emptyList(),
            breadcrumbStack = emptyList(),
            selectedMenuItem = null
        )
    }
}
