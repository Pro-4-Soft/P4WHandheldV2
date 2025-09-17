package com.p4handheld.data.api

import com.p4handheld.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    suspend fun login(source: String = "LoginWeb", loginRequest: LoginRequest): ApiResponse<LoginResponse>

    suspend fun getCurrentMenu(): ApiResponse<UserContextResponse>

    suspend fun initAction(pageKey: String, initialValue: String? = null): ApiResponse<PromptResponse>

    suspend fun processAction(pageKey: String, processRequest: ProcessRequest): ApiResponse<PromptResponse>

    suspend fun completeAction(pageKey: String): ApiResponse<PromptResponse>
    
    suspend fun updateUserLocation(lat: Double, lon: Double): ApiResponse<Unit>
    
    // Firebase messaging endpoints
    @POST("api/firebase/token")
    suspend fun updateFirebaseToken(@Body request: FirebaseTokenRequest): Response<MessageResponse>
    
    @POST("api/firebase/groups")
    suspend fun getFirebaseGroups(): Response<List<FirebaseGroup>>
    
    @POST("api/firebase/subscription")
    suspend fun updateGroupSubscription(@Body request: FirebaseSubscriptionRequest): Response<MessageResponse>
    
    @POST("api/firebase/messages")
    suspend fun getGroupMessages(@Body request: GroupMessagesRequest): Response<GroupMessagesResponse>
    
    @PUT("api/firebase/messages/{messageId}/read")
    suspend fun markMessageAsRead(@Path("messageId") messageId: String): Response<MessageResponse>
}

data class ApiResponse<T>(
    val isSuccessful: Boolean,
    val body: T? = null,
    val code: Int = 200,
    val errorMessage: String? = null
)
