package com.p4handheld.data.api

import com.p4handheld.data.models.FirebaseTokenRequest
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MessageResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import com.p4handheld.data.models.UserContextResponse

interface ApiService {
    suspend fun login(source: String = "LoginWeb", loginRequest: LoginRequest): ApiResponse<LoginResponse>

    suspend fun getCurrentMenu(): ApiResponse<UserContextResponse>
    
    suspend fun processAction(pageKey: String, processRequest: ProcessRequest): ApiResponse<PromptResponse>

    suspend fun completeAction(pageKey: String): ApiResponse<PromptResponse>

    suspend fun updateUserLocation(lat: Double, lon: Double): ApiResponse<Unit>

    suspend fun updateFirebaseToken(request: FirebaseTokenRequest): ApiResponse<MessageResponse>

    suspend fun getContacts(): ApiResponse<List<UserContact>>

    suspend fun getMessages(contactId: String? = null): ApiResponse<List<UserChatMessage>>

    suspend fun sendMessage(toUserId: String, message: String): ApiResponse<MessageResponse>
}

data class ApiResponse<T>(
    val isSuccessful: Boolean,
    val body: T? = null,
    val code: Int = 200,
    val errorMessage: String? = null
)
