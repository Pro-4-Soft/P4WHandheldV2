package com.p4handheld.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
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
import org.json.JSONArray

class AuthRepository(context: Context) {
    private val authSharedPreferences: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val firebaseSharedPreferences: SharedPreferences = context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)

    private val apiService: ApiService

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
                val response = apiService.getCurrentMenu()

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
        }

        CrashlyticsHelper.setUserId(userContextResponse.userId)
        CrashlyticsHelper.setCustomKey("track_geo_location", userContextResponse.trackGeoLocation)
        CrashlyticsHelper.setCustomKey("user_scan_type", userContextResponse.userScanType.toString())
        CrashlyticsHelper.log("User context data updated")
    }

    fun getStoredMenuData(): UserContextResponse? {
        val menuJson = authSharedPreferences.getString("menu_json", null)

        return if (menuJson != null) {
            try {
                val menuArray = JSONArray(menuJson)
                val menuItems = mutableListOf<MenuItem>()

                for (i in 0 until menuArray.length()) {
                    val menuObj = menuArray.getJSONObject(i)
                    val menuItem = MenuItem(
                        id = if (menuObj.isNull("Id")) null else menuObj.getString("Id"),
                        label = menuObj.getString("Label"),
                        state = if (menuObj.isNull("State")) null else menuObj.getString("State"),
                        stateParams = if (menuObj.isNull("StateParams")) null else menuObj.get("StateParams"),
                        icon = if (menuObj.isNull("Icon")) null else menuObj.getString("Icon"),
                        children = emptyList()
                    )
                    menuItems.add(menuItem)
                }

                UserContextResponse(
                    menu = menuItems,
                    trackGeoLocation = authSharedPreferences.getBoolean("track_geo_location", false),
                    userScanType = ScanType.fromSerializedName(authSharedPreferences.getString("user_scan_type", "")),
                    userId = authSharedPreferences.getString("userId", "") ?: "",
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun shouldTrackLocation(): Boolean {
        return authSharedPreferences.getBoolean("track_geo_location", false)
    }

    fun isLoggedIn(): Boolean {
        return authSharedPreferences.getBoolean("is_logged_in", false)
    }

    fun getStateParamsForPage(pageKey: String): Any? {
        return getStoredMenuData()
            ?.menu
            ?.firstOrNull { it.state == pageKey }
            ?.stateParams
    }

    fun hasValidToken(): Boolean {
        val token = authSharedPreferences.getString("token", null)
        val isLoggedIn = authSharedPreferences.getBoolean("is_logged_in", false)
        return !token.isNullOrEmpty() && isLoggedIn
    }

    fun getStoredToken(): String? {
        return authSharedPreferences.getString("token", null)
    }

    fun getEffectiveScanType(): ScanType {
        val userContext = getStoredMenuData()
        return userContext?.userScanType ?: ScanType.ZEBRA_DATA_WEDGE;
    }

    fun logout() {
        authSharedPreferences.edit { clear() }

        CrashlyticsHelper.clearUserInfo()
        CrashlyticsHelper.log("User logged out")
    }
}
