package com.p4handheld.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging


private const val TAG = "FirebaseManager"
const val FIREBASE_PREFS_NAME = "firebase_prefs"
const val FIREBASE_KEY_FCM_TOKEN = "fcm_token"

class FirebaseManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null

        fun getInstance(context: Context): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(FIREBASE_PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Initialize Firebase messaging and get FCM token
     */
    fun initialize() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting FCM token", e)
        }
    }

}
