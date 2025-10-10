package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.GlobalConstants.AppPreferences.TENANT_PREFS
import com.p4handheld.R
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.utils.Translations
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
            _uiState.value = _uiState.value.copy(errorMessage = Translations[getApplication(), R.string.login_error_empty_fields])
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
                    val menuResult = authRepository.getUserContext()

                    if (menuResult.isSuccess) {
                        // Initialize Firebase after successful login

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            errorMessage = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = menuResult.exceptionOrNull()?.message
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = loginResult.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = Translations.format(getApplication(), R.string.login_error_network, e.message ?: "")
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
        val sharedPreferences = context.getSharedPreferences(TENANT_PREFS, Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString("base_tenant_url", "") ?: ""
        val logoUrl = "${baseUrl}/data/logo"
        return logoUrl
    }
}
