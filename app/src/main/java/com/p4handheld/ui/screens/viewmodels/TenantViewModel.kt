package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL

data class TenantConfig(
    val tenantName: String,
    val baseUrl: String
)

data class TenantUiState(
    val isLoading: Boolean = false,
    val isConfigurationSaved: Boolean = false,
    val errorMessage: String? = null
)

class TenantViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TenantUiState())
    val uiState: StateFlow<TenantUiState> = _uiState.asStateFlow()

    private val sharedPreferences =
        application.getSharedPreferences("tenant_config", Context.MODE_PRIVATE)

    fun saveTenantConfiguration(tenantName: String, baseUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // Validate inputs
                if (tenantName.isBlank()) {
                    throw IllegalArgumentException("Tenant name cannot be empty")
                }

                if (baseUrl.isBlank()) {
                    throw IllegalArgumentException("Base URL cannot be empty")
                }

                // Validate URL format
                val normalizedUrl =
                    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                        "https://$baseUrl"
                    } else {
                        baseUrl
                    }

                try {
                    URL(normalizedUrl)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid URL format")
                }

                // Save to SharedPreferences
                with(sharedPreferences.edit()) {
                    putString("tenant_name", tenantName)
                    putString("base_url", normalizedUrl)
                    putBoolean("is_configured", true)
                    apply()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConfigurationSaved = true,
                    errorMessage = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "An error occurred while saving configuration"
                )
            }
        }
    }

    fun getTenantConfig(): TenantConfig? {
        val tenantName = sharedPreferences.getString("tenant_name", null)
        val baseUrl = sharedPreferences.getString("base_url", null)
        val isConfigured = sharedPreferences.getBoolean("is_configured", false)

        return if (isConfigured && tenantName != null && baseUrl != null) {
            TenantConfig(tenantName, baseUrl)
        } else {
            null
        }
    }

    fun isTenantConfigured(): Boolean {
        return sharedPreferences.getBoolean("is_configured", false)
    }

    fun clearTenantConfiguration() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
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