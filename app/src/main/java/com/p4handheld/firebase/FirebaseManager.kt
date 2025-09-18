package com.p4handheld.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

class FirebaseManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseManager"
        private const val PREFS_NAME = "firebase_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"

        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Initialize Firebase messaging and get FCM token
     */
    fun initialize(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.toString()
            Log.d(TAG, "FCM Token obtained: $token")

            // Store token locally
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()

            token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token", e)
            null
        }
    }
}
