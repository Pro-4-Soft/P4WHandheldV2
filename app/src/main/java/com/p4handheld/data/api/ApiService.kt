package com.p4handheld.data.api

import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MenuResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse

interface ApiService {
    suspend fun login(source: String = "LoginWeb", loginRequest: LoginRequest): ApiResponse<LoginResponse>

    suspend fun getCurrentMenu(): ApiResponse<MenuResponse>

    suspend fun initAction(pageKey: String, initialValue: String? = null): ApiResponse<PromptResponse>

    suspend fun processAction(pageKey: String, processRequest: ProcessRequest): ApiResponse<PromptResponse>

    suspend fun completeAction(pageKey: String): ApiResponse<PromptResponse>
}

data class ApiResponse<T>(
    val isSuccessful: Boolean,
    val body: T? = null,
    val code: Int = 200,
    val errorMessage: String? = null
)
