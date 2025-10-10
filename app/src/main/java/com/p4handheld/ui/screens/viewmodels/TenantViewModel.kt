package com.p4handheld.ui.screens.viewmodels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.GlobalConstants
import com.p4handheld.GlobalConstants.AppPreferences.TENANT_PREFS
import com.p4handheld.utils.TranslationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL

data class TenantConfig(
    val tenantName: String?,
    val baseApiUrl: String
)

data class TenantUiState(
    val isLoading: Boolean = false,
    val isConfigurationSaved: Boolean = false,
    val errorMessage: String? = null
)

class TenantViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(TenantUiState())
    val uiState: StateFlow<TenantUiState> = _uiState.asStateFlow()

    private val sharedPreferences = application.getSharedPreferences(TENANT_PREFS, Context.MODE_PRIVATE)

    fun saveTenantConfiguration(tenantName: String, baseUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                if (tenantName.isBlank()) {
                    throw IllegalArgumentException("Tenant name cannot be empty")
                }

                if (baseUrl.isBlank()) {
                    throw IllegalArgumentException("Base URL cannot be empty")
                }

                val normalizedUrl =
                    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                        "https://$baseUrl"
                    } else {
                        baseUrl
                    }

                try {
                    URL(normalizedUrl)
                } catch (_: Exception) {
                    throw IllegalArgumentException("Invalid URL format")
                }

                val tenantUri = replaceTenantNameForBaseUrl(normalizedUrl, tenantName)

                sharedPreferences.edit {
                    putString("tenant_name", tenantName)
                    putString("base_url", normalizedUrl)
                    putString("base_tenant_url", tenantUri)
                    putBoolean("is_configured", true)
                }

                TranslationManager.getInstance(getApplication()).loadTranslations()


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

    fun replaceTenantNameForBaseUrl(url: String, tenantName: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: return url

            val parts = host.split('.').toMutableList()
            if (parts.isNotEmpty()) {
                parts[0] = tenantName
            }

            // Rebuild URL with replaced host
            val newHost = parts.joinToString(".")
            java.net.URI(
                uri.scheme,
                uri.userInfo,
                newHost,
                uri.port,
                uri.path,
                uri.query,
                uri.fragment
            ).toString()
        } catch (_: Exception) {
            url
        }
    }

    fun getTenantConfig(): TenantConfig {
        val tenantName = sharedPreferences.getString("tenant_name", null)
        val baseApiUrl = sharedPreferences.getString("base_url", GlobalConstants.DEFAULT_BASE_URL) ?: GlobalConstants.DEFAULT_BASE_URL

        return TenantConfig(tenantName, baseApiUrl)
    }

    @Composable
    fun getLogoUrl(): String {
        val baseUrl = sharedPreferences.getString("base_tenant_url", "") ?: sharedPreferences.getString("base_url", "")
        val logoUrl = "${baseUrl}/data/logo"
        return logoUrl
    }
}