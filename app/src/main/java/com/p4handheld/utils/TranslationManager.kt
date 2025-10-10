package com.p4handheld.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.p4handheld.GlobalConstants
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
        private const val KEY_CACHED_TENANT = "cached_tenant"

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

    suspend fun loadTranslations() = withContext(Dispatchers.IO) {
        try {
            val currentTenant = getCurrentTenantName()
            if (currentTenant == null) {
                Log.d(TAG, "No tenant configured, skipping translation loading")
                return@withContext
            }

            val cachedTenant = getCachedTenant()

            // Check if we already have translations for this tenant (unless force reload)
            if (currentTenant == cachedTenant && cachedTranslations != null) {
                Log.d(TAG, "Using cached translations for tenant: $currentTenant")
                return@withContext
            }

            if (currentTenant != cachedTenant) {
                Log.d(TAG, "Tenant changed from '$cachedTenant' to '$currentTenant', reloading translations")
                clearCache()
            }

            Log.d(TAG, "Loading translations for tenant: $currentTenant")
            val keys = getAllTranslationKeys()
            val response = ApiClient.apiService.getTranslations(TranslationRequest(keys))

            if (response.isSuccessful && response.body != null) {
                val newTranslations = CachedTranslations(
                    translations = response.body.translations,
                    lastUpdated = System.currentTimeMillis()
                )
                cacheTranslations(newTranslations)
                cacheTenant(currentTenant)
                cachedTranslations = newTranslations
                Log.d(TAG, "Loaded ${newTranslations.translations.size} translations for tenant: $currentTenant")
            } else {
                Log.e(TAG, "Failed to load translations: ${response.errorMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading translations", e)
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

    private fun getCurrentTenantName(): String? {
        return try {
            val tenantPrefs = appContext.getSharedPreferences(GlobalConstants.AppPreferences.TENANT_PREFS, Context.MODE_PRIVATE)
            tenantPrefs.getString("tenant_name", null)
        } catch (e: Exception) {
            Log.w(TAG, "Error getting current tenant", e)
            null
        }
    }

    private fun getCachedTenant(): String? {
        return prefs.getString(KEY_CACHED_TENANT, null)
    }

    private fun cacheTenant(tenant: String?) {
        try {
            prefs.edit { putString(KEY_CACHED_TENANT, tenant) }
        } catch (e: Exception) {
            Log.e(TAG, "Error caching tenant", e)
        }
    }

    fun clearCache() {
        try {
            prefs.edit {
                remove(KEY_CACHED_TRANSLATIONS)
                remove(KEY_CACHED_TENANT)
            }
            cachedTranslations = null
            Log.d(TAG, "Translation cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    fun getAllTranslationKeys(): List<String> {
        return try {
            val stringClass = R.string::class.java
            val fields = stringClass.declaredFields
            val packageName = appContext.packageName
            
            val excludedPrefixes = listOf(
                "com.google.",
                "firebase_",
                "google_",
                "default_web_",
                "gcm_",
                "project_",
            )

            fields
                .filter {
                    it.type == Int::class.javaPrimitiveType &&
                            java.lang.reflect.Modifier.isStatic(it.modifiers) &&
                            java.lang.reflect.Modifier.isPublic(it.modifiers)
                }
                .mapNotNull { field ->
                    try {
                        val resId = field.getInt(null)
                        val key = appContext.resources.getResourceEntryName(resId)
                        val pkg = appContext.resources.getResourcePackageName(resId)
                        if (pkg != packageName) return@mapNotNull null
                        if (excludedPrefixes.any { key.startsWith(it) }) return@mapNotNull null

                        key
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping invalid string field: ${field.name}", e)
                        null
                    }
                }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get string resources via reflection", e)
            listOf(
                "login_title", "username_label", "password_label", "login_button",
                "ok", "cancel", "error", "loading"
            )
        }
    }
}
