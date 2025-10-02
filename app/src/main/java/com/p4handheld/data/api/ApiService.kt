package com.p4handheld.data.api

import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MessageResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import com.p4handheld.data.models.UserContextResponse

interface ApiService {
    suspend fun login(source: String = "LoginHandheld", loginRequest: LoginRequest): ApiResponse<LoginResponse>

    suspend fun getCurrentMenu(): ApiResponse<UserContextResponse>

    suspend fun processAction(pageKey: String, processRequest: ProcessRequest, taskId: String? = null): ApiResponse<PromptResponse>

    suspend fun updateUserLocation(lat: Double, lon: Double): ApiResponse<Unit>

    suspend fun getContacts(): ApiResponse<List<UserContact>>

    suspend fun getMessages(contactId: String? = null): ApiResponse<List<UserChatMessage>>

    // Upload a screenshot of the current screen to the server
    suspend fun updateScreen(screenshotJpeg: ByteArray): ApiResponse<Unit>

    suspend fun sendMessage(toUserId: String, message: String): ApiResponse<MessageResponse>
}

data class ApiResponse<T>(
    val isSuccessful: Boolean,
    val body: T? = null,
    val code: Int = 200,
    val errorMessage: String? = null
)
