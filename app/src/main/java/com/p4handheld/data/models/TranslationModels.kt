package com.p4handheld.data.models

import com.google.gson.annotations.SerializedName

data class TranslationRequest(
    @SerializedName("keys")
    val keys: List<String>
)

data class TranslationResponse(
    @SerializedName("translations")
    val translations: Map<String, String>
)

data class CachedTranslations(
    val languageCode: String,
    val translations: Map<String, String>,
    val lastUpdated: Long = System.currentTimeMillis()
)
