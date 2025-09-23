package com.p4handheld.data.api

import com.p4handheld.data.models.FirebaseTokenRequest
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MessageResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.UserContextResponse
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact

interface ApiService {
    suspend fun login(source: String = "LoginWeb", loginRequest: LoginRequest): ApiResponse<LoginResponse>

    suspend fun getCurrentMenu(): ApiResponse<UserContextResponse>

    suspend fun initAction(pageKey: String, initialValue: String? = null): ApiResponse<PromptResponse>

    suspend fun processAction(pageKey: String, processRequest: ProcessRequest): ApiResponse<PromptResponse>

    suspend fun completeAction(pageKey: String): ApiResponse<PromptResponse>

    suspend fun updateUserLocation(lat: Double, lon: Double): ApiResponse<Unit>

    suspend fun updateFirebaseToken(request: FirebaseTokenRequest): ApiResponse<MessageResponse>

    // Chats
    suspend fun getContacts(): ApiResponse<List<UserContact>>

    /**
     * Returns messages for a conversation. If contactId is provided, fetch messages with that contact.
     * Some backends may return recent messages for all chats if contactId is null.
     */
    suspend fun getMessages(contactId: String? = null): ApiResponse<List<UserChatMessage>>
}

data class ApiResponse<T>(
    val isSuccessful: Boolean,
    val body: T? = null,
    val code: Int = 200,
    val errorMessage: String? = null
)
