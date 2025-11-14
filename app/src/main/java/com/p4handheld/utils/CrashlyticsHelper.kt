package com.p4handheld.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashlyticsHelper {

    private const val TAG = "CrashlyticsHelper"

    fun setUserId(userId: String) {
        try {
            FirebaseCrashlytics.getInstance().setUserId(userId)
            Log.d(TAG, "User ID set for Crashlytics: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
        }
    }

    fun setCustomKey(key: String, value: String) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
            Log.d(TAG, "Custom key set: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
            Log.d(TAG, "Custom key set: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }

    fun log(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log(message)
            Log.d(TAG, "Logged to Crashlytics: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log message", e)
        }
    }

    fun recordException(throwable: Throwable, context: Map<String, String>) {
        try {
            context.forEach { (key, value) ->
                FirebaseCrashlytics.getInstance().setCustomKey(key, value)
            }
            FirebaseCrashlytics.getInstance().recordException(throwable)
            Log.d(TAG, "Exception with context recorded to Crashlytics: ${throwable.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exception with context", e)
        }
    }

    fun setUserInfo(userId: String, email: String? = null, name: String? = null) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setUserId(userId)
            email?.let { crashlytics.setCustomKey("user_email", it) }
            name?.let { crashlytics.setCustomKey("user_name", it) }
            Log.d(TAG, "User info set for Crashlytics")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user info", e)
        }
    }

    fun clearUserInfo() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setUserId("")
            crashlytics.setCustomKey("user_email", "")
            crashlytics.setCustomKey("user_name", "")
            Log.d(TAG, "User info cleared from Crashlytics")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear user info", e)
        }
    }
}
