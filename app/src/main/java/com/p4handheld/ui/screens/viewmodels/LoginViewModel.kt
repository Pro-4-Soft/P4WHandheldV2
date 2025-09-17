package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.firebase.FirebaseManager
import com.p4handheld.utils.PermissionChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter both username and password"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val loginResult = authRepository.login(username, password)

                if (loginResult.isSuccess) {
                    // After successful login, get menu data
                    val menuResult = authRepository.getCurrentMenu()

                    if (menuResult.isSuccess) {
                        // Initialize Firebase after successful login
                        try {
                            val firebaseManager = FirebaseManager.getInstance(getApplication())
                            firebaseManager.initialize()
                        } catch (e: Exception) {
                            // Log Firebase initialization error but don't fail login
                            android.util.Log.e("LoginViewModel", "Firebase initialization failed", e)
                        }

                        // Check if we should request location permissions based on user context
                        val shouldRequestPermissions = PermissionChecker.shouldRequestLocationPermissions(getApplication())

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Login successful but failed to load menu: ${menuResult.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Login failed: ${loginResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    @Composable
    fun getLogoUrl(): String {
        val context = LocalContext.current
        val sharedPreferences = context.getSharedPreferences("tenant_config", Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString("base_url", "") ?: ""
        val logoUrl = "${baseUrl}data/logo"
        return logoUrl
    }
}
