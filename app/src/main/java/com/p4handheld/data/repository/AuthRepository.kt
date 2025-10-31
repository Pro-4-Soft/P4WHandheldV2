package com.p4handheld.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.p4handheld.GlobalConstants
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
import com.p4handheld.utils.CrashlyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthRepository(context: Context) {
    private val authSharedPreferences: SharedPreferences = context.getSharedPreferences(GlobalConstants.AppPreferences.AUTH_PREFS, Context.MODE_PRIVATE)
    private val firebaseSharedPreferences: SharedPreferences = context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)
    private val tenantSharedPreferences: SharedPreferences = context.getSharedPreferences(TENANT_PREFS, Context.MODE_PRIVATE)
    private val apiService: ApiService

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
                resetUserContextData()
                val fcmToken = firebaseSharedPreferences.getString(FIREBASE_KEY_FCM_TOKEN, null)
                val loginRequest = LoginRequest(username, password, fcmToken)
                val response = apiService.login(loginRequest = loginRequest)

                if (response.isSuccessful) {
                    authSharedPreferences.edit {
                        putBoolean("is_logged_in", true)
                            .putString("username", username)
                            .putString("token", response.body?.token)
                    }

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
                val response = apiService.getCurrent()

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
        authSharedPreferences.edit {
            putString("menu_json", json.encodeToString(userContextResponse.menu))
                .putBoolean("track_geo_location", userContextResponse.trackGeoLocation)
                .putString("user_scan_type", userContextResponse.userScanType.toString())
                .putString("userId", userContextResponse.userId)
                .putString("languageId", userContextResponse.languageId)
        }

        firebaseSharedPreferences.edit { putString("userId", userContextResponse.userId) }

        CrashlyticsHelper.setUserId(userContextResponse.userId)
        CrashlyticsHelper.setCustomKey("track_geo_location", userContextResponse.trackGeoLocation)
        CrashlyticsHelper.setCustomKey("user_scan_type", userContextResponse.userScanType.toString())
        CrashlyticsHelper.log("User context data updated")
    }

    fun getStoredUserContextData(): UserContextResponse? {
        val menuJson = authSharedPreferences.getString("menu_json", null)

        return if (menuJson != null) {
            try {
                val menuItems: List<MenuItem> = json.decodeFromString(menuJson)

                val userScanType = authSharedPreferences.getString("user_scan_type", null)
                UserContextResponse(
                    menu = menuItems,
                    trackGeoLocation = authSharedPreferences.getBoolean("track_geo_location", false),
                    userScanType = if (userScanType.isNullOrBlank()) ScanType.ZEBRA_DATA_WEDGE else enumValueOf<ScanType>(userScanType),
                    userId = authSharedPreferences.getString("userId", "") ?: "",
                    languageId = authSharedPreferences.getString("languageId", "") ?: "",
                )
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun shouldTrackLocation(): Boolean = authSharedPreferences.getBoolean("track_geo_location", false)

    fun getStateParamsForPage(pageKey: String): String? {
        return getStoredUserContextData()
            ?.menu
            ?.firstOrNull { it.state == pageKey }
            ?.stateParams.toString()
    }

    fun hasValidToken(): Boolean {
        val token = authSharedPreferences.getString("token", null)
        val isLoggedIn = authSharedPreferences.getBoolean("is_logged_in", false)
        return !token.isNullOrEmpty() && isLoggedIn
    }

    fun getEffectiveScanType(): ScanType {
        val userScanType = authSharedPreferences.getString("user_scan_type", null)
        return if (userScanType.isNullOrBlank()) ScanType.ZEBRA_DATA_WEDGE else enumValueOf<ScanType>(userScanType)
    }

    fun resetUserContextData() {
        authSharedPreferences.edit {
            putString("menu_json", null)
                .putBoolean("track_geo_location", false)
                .putString("user_scan_type", ScanType.ZEBRA_DATA_WEDGE.toString())
        }
        firebaseSharedPreferences.edit { putString("userId", null) }
    }

    suspend fun logout(context: Context): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val logoutResult = apiService.logout()
                authSharedPreferences.edit { clear() }

                try {
                    val firebaseManager = com.p4handheld.firebase.FirebaseManager.getInstance(context)
                    firebaseManager.clearDataOnLogout()
                } catch (e: Exception) {
                    CrashlyticsHelper.recordException(e, mapOf("operation" to "firebase_cleanup_on_logout"))
                }

                CrashlyticsHelper.clearUserInfo()
                CrashlyticsHelper.log("User logged out")

                if (logoutResult.isSuccessful) {
                    Result.success(true)
                } else {
                    CrashlyticsHelper.recordException(
                        Exception("API logout failed: ${logoutResult.errorMessage}"),
                        mapOf("operation" to "api_logout_failed", "error_code" to logoutResult.code.toString())
                    )
                    Result.success(true)
                }
            } catch (e: Exception) {
                authSharedPreferences.edit { clear() }
                CrashlyticsHelper.clearUserInfo()
                CrashlyticsHelper.recordException(e, mapOf("operation" to "logout_exception"))
                Result.failure(e)
            }
        }
    }

    fun getBaseTenantUrl(): String? = tenantSharedPreferences.getString("base_tenant_url", null)
    fun getUserId(): String? = authSharedPreferences.getString("userId", null)
}
