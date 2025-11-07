package com.p4handheld.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.p4handheld.GlobalConstants.AppPreferences.FIREBASE_PREFS_NAME
import com.p4handheld.GlobalConstants.AppPreferences.TENANT_PREFS
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.api.ApiService
import com.p4handheld.data.models.ApiError
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.models.ScanType
import com.p4handheld.data.models.UserContextResponse
import com.p4handheld.firebase.FIREBASE_KEY_FCM_TOKEN
import com.p4handheld.ui.components.TopBarViewModel
import com.p4handheld.utils.CrashlyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AuthRepository(context: Context) {
    private val firebaseSharedPreferences: SharedPreferences = context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)
    private val tenantSharedPreferences: SharedPreferences = context.getSharedPreferences(TENANT_PREFS, Context.MODE_PRIVATE)
    private val apiService: ApiService

    companion object {
        var trackGeoLocation: Boolean = false
        var userScanType: ScanType = ScanType.ZEBRA_DATA_WEDGE
        var userId: String = ""
        var languageId: String = ""
        var hasTasks: Boolean = false
        var newMessages: Int = 0
        var menu: List<MenuItem>? = null
        var username: String = ""
        var isLoggedIn: Boolean = false
        var token: String = ""
    }

    // JSON configuration for Kotlinx Serialization
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    init {
        ApiClient.initialize(context.applicationContext)
        apiService = ApiClient.apiService
    }

    suspend fun login(username: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val fcmToken = firebaseSharedPreferences.getString(FIREBASE_KEY_FCM_TOKEN, null)
                val loginRequest = LoginRequest(username, password, fcmToken)
                val response = apiService.login(loginRequest = loginRequest)

                if (response.isSuccessful) {
                    isLoggedIn = true
                    AuthRepository.username = username
                    token = response.body?.token ?: ""

                    // Set user information in Crashlytics
                    CrashlyticsHelper.setUserInfo(username)
                    CrashlyticsHelper.log("User logged in successfully: $username")

                    Result.success(true)
                } else {
                    Result.failure(ApiError("${response.errorMessage}", response.code))
                }
            } catch (e: Exception) {
                Result.failure(ApiError("Network error: ${e.message}"))
            }
        }
    }

    suspend fun getUserContext(): Result<UserContextResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentUserContext()

                if (response.isSuccessful && response.body != null) {
                    val userContextResponse = response.body
                    storeUserContextData(userContextResponse)
                    Result.success(userContextResponse)
                } else {
                    Result.failure(ApiError("Failed to get menu: ${response.code}", response.code))
                }
            } catch (e: Exception) {
                Result.failure(ApiError("Network error: ${e.message}"))
            }
        }
    }

    private fun storeUserContextData(userContextResponse: UserContextResponse) {
        trackGeoLocation = userContextResponse.trackGeoLocation;
        userScanType = userContextResponse.userScanType;
        userId = userContextResponse.userId;
        languageId = userContextResponse.languageId;
        hasTasks = userContextResponse.hasTasks;
        newMessages = userContextResponse.newMessages;
        menu = userContextResponse.menu;
        TopBarViewModel.IsInitialized = false;

        CrashlyticsHelper.setUserId(userContextResponse.userId)
        CrashlyticsHelper.setCustomKey("track_geo_location", userContextResponse.trackGeoLocation)
        CrashlyticsHelper.setCustomKey("user_scan_type", userContextResponse.userScanType.toString())
        CrashlyticsHelper.log("User context data updated")
    }

    fun shouldTrackLocation(): Boolean = trackGeoLocation

    fun getStateParamsForPage(pageKey: String): String? {
        return menu
            ?.firstOrNull { it.state == pageKey }
            ?.stateParams.toString()
    }

    fun hasValidToken(): Boolean {
        return token.isNotEmpty() && isLoggedIn
    }

    suspend fun logout(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Reset companion object values
                isLoggedIn = false
                token = ""
                userId = ""
                username = ""
                trackGeoLocation = false
                hasTasks = false
                newMessages = 0
                menu = null

                TopBarViewModel.IsInitialized = false;
                CrashlyticsHelper.log("User logged out")

                // Try API logout - but don't prevent local logout if it fails
                val logoutResult = apiService.logout()
                if (logoutResult.isSuccessful || logoutResult.code == 401) {
                    Result.success(true)
                } else {
                    CrashlyticsHelper.recordException(
                        Exception("API logout failed: ${logoutResult.errorMessage}"),
                        mapOf("operation" to "api_logout_failed", "error_code" to logoutResult.code.toString())
                    )
                    // Return failure since API logout failed, even though local cleanup succeeded
                    Result.failure(ApiError("Server logout failed: ${logoutResult.errorMessage}", logoutResult.code))
                }
            } catch (e: Exception) {
                isLoggedIn = false
                token = ""
                userId = ""
                username = ""
                trackGeoLocation = false
                hasTasks = false
                newMessages = 0
                menu = null
                TopBarViewModel.IsInitialized = false;

                CrashlyticsHelper.clearUserInfo()
                CrashlyticsHelper.recordException(e, mapOf("operation" to "logout_exception"))
                Result.failure(e)
            }
        }
    }

    fun getBaseTenantUrl(): String? = tenantSharedPreferences.getString("base_tenant_url", null)
}
