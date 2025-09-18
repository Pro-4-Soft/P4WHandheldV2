package com.p4handheld.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.ApiError
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.models.UserContextResponse
import com.p4handheld.firebase.FIREBASE_KEY_FCM_TOKEN
import com.p4handheld.firebase.FIREBASE_PREFS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AuthRepository(context: Context) {
    private val authSharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val firebaseSharedPreferences: SharedPreferences =
        context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)

    private val apiService = ApiClient.apiService

    suspend fun login(username: String, password: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Get FCM token from SharedPreferences
                val fcmToken = firebaseSharedPreferences.getString(FIREBASE_KEY_FCM_TOKEN, null)

                val loginRequest = LoginRequest(username, password, fcmToken)
                val response = apiService.login(loginRequest = loginRequest)

                if (response.isSuccessful) {
                    // Store login success flag
                    authSharedPreferences
                        .edit()
                        .putBoolean("is_logged_in", true)
                        .putString("username", username)
                        .putString("token", response.body?.token)
                        .apply()
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
                    val menuResponse = response.body
                    // Store menu data
                    storeMenuData(menuResponse)
                    Result.success(menuResponse)
                } else {
                    Result.failure(ApiError("Failed to get menu: ${response.code}", response.code))
                }
            } catch (e: Exception) {
                Result.failure(ApiError("Network error: ${e.message}"))
            }
        }
    }

    private fun storeMenuData(menuResponse: UserContextResponse) {
        val menuArray = JSONArray()
        menuResponse.menu.forEach { menuItem ->
            val menuJson = JSONObject().apply {
                put("Id", menuItem.id)
                put("Label", menuItem.label)
                put("State", menuItem.state)
                put("StateParams", menuItem.stateParams)
                put("Icon", menuItem.icon)
                put("Children", JSONArray()) // Simplified for now
            }
            menuArray.put(menuJson)
        }

        authSharedPreferences.edit()
            .putString("menu_json", menuArray.toString())
            .putBoolean("track_geo_location", menuResponse.trackGeoLocation)
            .putString("user_scan_type", menuResponse.userScanType)
            .putString("tenant_scan_type", menuResponse.tenantScanType)
            .apply()
    }

    fun getStoredMenuData(): UserContextResponse? {
        val tenant = authSharedPreferences.getString("tenant", null)
        val menuJson = authSharedPreferences.getString("menu_json", null)

        return if (tenant != null && menuJson != null) {
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
                        children = emptyList() // Simplified for now
                    )
                    menuItems.add(menuItem)
                }

                UserContextResponse(
                    menu = menuItems,
                    trackGeoLocation = authSharedPreferences.getBoolean("track_geo_location", false),
                    userScanType = authSharedPreferences.getString("user_scan_type", "") ?: "",
                    tenantScanType = authSharedPreferences.getString("tenant_scan_type", "") ?: ""
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

    fun logout() {
        authSharedPreferences.edit().clear().apply()
    }
}
