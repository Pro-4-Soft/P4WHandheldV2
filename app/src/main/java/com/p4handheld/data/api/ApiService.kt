package com.p4handheld.data.api

import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.TranslationRequest
import com.p4handheld.data.models.TranslationResponse
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import com.p4handheld.data.models.UserContextResponse
import kotlinx.serialization.Serializable

interface ApiService {
    suspend fun login(loginRequest: LoginRequest): ApiResponse<LoginResponse>

    suspend fun getCurrent(): ApiResponse<UserContextResponse>

    suspend fun processAction(pageKey: String, processRequest: ProcessRequest, taskId: String? = null): ApiResponse<PromptResponse>

    suspend fun updateUserLocation(lat: Double, lon: Double): ApiResponse<Unit>

    suspend fun getContacts(): ApiResponse<List<UserContact>>

    suspend fun getMessages(contactId: String? = null, skip: Int = 0, take: Int = 50): ApiResponse<List<UserChatMessage>>

    suspend fun updateScreen(screenshotJpeg: ByteArray): ApiResponse<Unit>

    suspend fun sendMessage(toUserId: String, message: String): ApiResponse<Unit>

    suspend fun getAssignedTaskCount(userId: String): ApiResponse<Int>

    suspend fun getTranslations(translationRequest: TranslationRequest): ApiResponse<TranslationResponse>

    suspend fun preflightCheck(): ApiResponse<Unit>

    suspend fun logout(): ApiResponse<Unit>
}

@Serializable
data class ApiResponse<T>(
    val isSuccessful: Boolean,
    val body: T? = null,
    val code: Int = 200,
    val errorMessage: String? = null
)
