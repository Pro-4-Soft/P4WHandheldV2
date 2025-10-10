package com.p4handheld.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.p4handheld.GlobalConstants
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.api.ApiService
import com.p4handheld.data.models.ApiError
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.models.ScanType
import com.p4handheld.data.models.UserContextResponse
import com.p4handheld.firebase.FIREBASE_KEY_FCM_TOKEN
import com.p4handheld.firebase.FIREBASE_PREFS_NAME
import com.p4handheld.utils.CrashlyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(context: Context) {
    private val authSharedPreferences: SharedPreferences = context.getSharedPreferences(GlobalConstants.AppPreferences.AUTH_PREFS, Context.MODE_PRIVATE)
    private val firebaseSharedPreferences: SharedPreferences = context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)

    private val apiService: ApiService

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
            putString("menu_json", Gson().toJson(userContextResponse.menu))
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
                val listType = object : TypeToken<List<MenuItem>>() {}.type
                val menuItems: List<MenuItem> = Gson().fromJson(menuJson, listType)

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

    fun getStateParamsForPage(pageKey: String): Any? {
        return getStoredUserContextData()
            ?.menu
            ?.firstOrNull { it.state == pageKey }
            ?.stateParams
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
                .putString("userId", null)
        }
        firebaseSharedPreferences.edit { putString("userId", null) }
    }

    fun logout() {
        authSharedPreferences.edit { clear() }

        CrashlyticsHelper.clearUserInfo()
        CrashlyticsHelper.log("User logged out")
    }
}
