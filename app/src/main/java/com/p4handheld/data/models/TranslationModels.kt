package com.p4handheld.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslationRequest(
    @SerialName("keys")
    val keys: List<String>
)

@Serializable
data class TranslationResponse(
    @SerialName("translations")
    val translations: Map<String, String>
)

@Serializable
data class CachedTranslations(
    val translations: Map<String, String>,
    val lastUpdated: Long = System.currentTimeMillis()
)
