package com.p4handheld.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import com.p4handheld.GlobalConstants.AppPreferences.FIREBASE_PREFS_NAME
import com.p4handheld.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "FirebaseManager"
const val FIREBASE_KEY_FCM_TOKEN = "fcm_token"

class FirebaseManager(private val prefs: SharedPreferences) {
    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager(
                    context.applicationContext.getSharedPreferences(
                        FIREBASE_PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
                ).also { INSTANCE = it }
            }
        }
    }

    //region Firebase token
    fun initialize() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (!token.isNullOrEmpty()) {
                    Log.d(TAG, "Retrieve token successful: $token")
                    prefs.edit { putString(FIREBASE_KEY_FCM_TOKEN, token) }
                } else {
                    Log.w(TAG, "Token is null or empty")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting FCM token", e)
            }
    }
    //endregion

    //region Unread messages
    fun hasUnreadMessages(): Boolean = prefs.getBoolean("has_unread_messages", false)

    fun setHasUnreadMessages(hasUnread: Boolean) = prefs.edit { putBoolean("has_unread_messages", hasUnread) }
    //endregion

    //region Tasks badge helpers
    fun getTasksCount(): Int = prefs.getInt("tasks_count", 0)

    fun setTasksCount(count: Int) = prefs.edit { putInt("tasks_count", count) }

    @Volatile
    private var hasInitializedTaskCountOnce = false


    suspend fun refreshTasksCountFromServer(): Int = withContext(Dispatchers.IO) {
        val userId = prefs.getString("userId", null)
        val res = ApiClient.apiService.getAssignedTaskCount(userId.toString())
        if (res.isSuccessful && res.body != null) {
            setTasksCount(res.body)
            res.body
        } else {
            getTasksCount()
        }
    }
    //endregion

    //region Logout cleanup
    fun clearDataOnLogout() {
        try {
            prefs.edit {
                putString(FIREBASE_KEY_FCM_TOKEN, null)
                putString("userId", null)
                putBoolean("has_unread_messages", false)
                putInt("tasks_count", 0)
            }

            hasInitializedTaskCountOnce = false

            FirebaseMessaging.getInstance().deleteToken()
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token deleted successfully on logout")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete FCM token on logout", e)
                }

            Log.d(TAG, "Firebase data cleared on logout")
        } catch (e: Exception) {
            Log.e(TAG, "Error during FCM cleanup on logout", e)
        }
    }
    //endregion
}
