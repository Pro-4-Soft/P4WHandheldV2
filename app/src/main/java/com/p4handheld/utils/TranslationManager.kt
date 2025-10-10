package com.p4handheld.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.p4handheld.R
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.CachedTranslations
import com.p4handheld.data.models.TranslationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationManager private constructor(
    private val appContext: Context
) {

    companion object {
        private const val TAG = "TranslationManager"
        private const val PREFS_NAME = "translation_prefs"
        private const val KEY_CACHED_TRANSLATIONS = "cached_translations"

        @Volatile
        @SuppressLint("StaticFieldLeak") // Safe — we use application context only
        private var instance: TranslationManager? = null

        fun getInstance(context: Context): TranslationManager {
            return instance ?: synchronized(this) {
                instance ?: TranslationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private var cachedTranslations: CachedTranslations? = null

    init {
        loadCachedTranslations()
    }

    operator fun get(resourceId: Int): String {
        val key = appContext.resources.getResourceEntryName(resourceId)
        return cachedTranslations?.translations?.get(key)
            ?: appContext.getString(resourceId)
    }

    fun getString(key: String, fallback: String = key): String = cachedTranslations?.translations?.get(key) ?: fallback

    suspend fun loadTranslations(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val keys = getAllTranslationKeys()
            val response = ApiClient.apiService.getTranslations(TranslationRequest(keys))

            if (response.isSuccessful && response.body != null) {
                val newTranslations = CachedTranslations(
                    translations = response.body.translations,
                    lastUpdated = System.currentTimeMillis()
                )
                cacheTranslations(newTranslations)
                cachedTranslations = newTranslations
                Log.d(TAG, "Loaded ${newTranslations.translations.size} translations")
                Result.success(true)
            } else {
                Result.failure(Exception("Failed: ${response.errorMessage}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading translations", e)
            Result.failure(e)
        }
    }

    private fun loadCachedTranslations() {
        prefs.getString(KEY_CACHED_TRANSLATIONS, null)?.let {
            try {
                val type = object : TypeToken<CachedTranslations>() {}.type
                cachedTranslations = gson.fromJson(it, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading cached translations", e)
            }
        }
    }

    private fun cacheTranslations(data: CachedTranslations) {
        try {
            prefs.edit { putString(KEY_CACHED_TRANSLATIONS, gson.toJson(data)) }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving translations", e)
        }
    }

    fun getAllTranslationKeys(): List<String> {
        val ids = listOf(
            // Login Screen
            R.string.login_title,
            R.string.username_label,
            R.string.password_label,
            R.string.login_button,
            R.string.login_error_empty_fields,
            R.string.login_error_network,
            R.string.powered_by,
            R.string.copyright_text,
            R.string.tenant_button,
            R.string.app_logo_description,

            // Menu Screen
            R.string.menu_title,
            R.string.logout_button,

            // Action Screen
            R.string.action_title,
            R.string.scan_button,
            R.string.submit_button,
            R.string.cancel_button,

            // Chat Screen
            R.string.chat_title,
            R.string.send_button,
            R.string.message_placeholder,

            // Contacts Screen
            R.string.contacts_title,
            R.string.no_contacts,

            // Tenant Screen
            R.string.tenant_title,
            R.string.tenant_url_label,
            R.string.save_button,

            // Common
            R.string.loading,
            R.string.error,
            R.string.success,
            R.string.retry,
            R.string.ok,
            R.string.cancel,

            // Firebase Messages
            R.string.notification_new_message,
            R.string.notification_task_added,
            R.string.notification_task_removed,

            // DataWedge Messages
            R.string.profile_creation_failed,
            R.string.profile_switch_success,
            R.string.profile_already_switched,
            R.string.profile_switch_failed,

            // Location
            R.string.location_permission_required,
            R.string.location_permission_granted,

            // Scanner
            R.string.scanner_ready,
            R.string.scanner_error,
            R.string.scan_result
        )
        return ids.map { appContext.resources.getResourceEntryName(it) }
    }
}
