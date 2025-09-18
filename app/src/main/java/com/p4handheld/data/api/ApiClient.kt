package com.p4handheld.data.api

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.p4handheld.data.models.FirebaseTokenRequest
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.models.MessageResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.UserContextResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object ApiClient {
    private var authToken: String? = null
    private var context: Context? = null

    fun initialize(appContext: Context) {
        context = appContext
    }

    private fun getBaseUrl(): String {
        val sharedPreferences = context?.getSharedPreferences("tenant_config", Context.MODE_PRIVATE)
        return "http://costa.pro4soft-demo.com:2020/";
        //return "http://10.0.2.2:2020/";//sharedPreferences?.getString("base_url", "http://10.0.2.2:2020/") ?: "http://10.0.2.2:2020/"
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()

                authToken?.let { token ->
                    requestBuilder.addHeader("Authenticationtoken", token)
                }

                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    val apiService: ApiService = object : ApiService {
        override suspend fun login(
            source: String,
            loginRequest: LoginRequest
        ): ApiResponse<LoginResponse> {
            return withContext(Dispatchers.IO) {
                try {
                    val jsonBody = JSONObject().apply {
                        put("Username", loginRequest.username)
                        put("Password", loginRequest.password)
                        put("HandheldNotificationToken", loginRequest.handheldNotificationToken)
                    }.toString()

                    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("${getBaseUrl()}api/Auth/Login?source=$source")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val isSuccessful = response.isSuccessful

                    if (isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        authToken = responseBody
                        val loginResponse = LoginResponse(
                            success = true,
                            message = "Good",
                            token = responseBody
                        )
                        ApiResponse(true, loginResponse, responseCode)
                    } else {
                        val errorBody = response.body?.string()
                        ApiResponse(false, null, responseCode, errorBody)
                    }
                } catch (e: Exception) {
                    ApiResponse(false, null, 0, e.message)
                }
            }
        }

        override suspend fun getCurrentMenu(): ApiResponse<UserContextResponse> {
            return withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("${getBaseUrl()}api/Auth/GetCurrent")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val isSuccessful = response.isSuccessful

                    if (isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()

                        // Parse JSON into MenuResponse
                        val userContextResponse = Gson().fromJson(responseBody, UserContextResponse::class.java)

                        // Find Handheld menu item
                        val handheldItem = userContextResponse.menu.firstOrNull { it.id == "Handheld" }

                        // Get its children (or empty list if not found)
                        val handheldChildren: List<MenuItem> = handheldItem?.children.orEmpty()

                        val handHeldMenuItems = UserContextResponse(
                            menu = handheldChildren,
                            trackGeoLocation = userContextResponse.trackGeoLocation,
                            userScanType = userContextResponse.userScanType,
                            tenantScanType = userContextResponse.tenantScanType
                        )
                        ApiResponse(true, handHeldMenuItems, responseCode)
                    } else {
                        val errorBody = response.body?.string()
                        ApiResponse(false, null, responseCode, errorBody)
                    }
                } catch (e: Exception) {
                    ApiResponse(false, null, 0, e.message)
                }
            }
        }

        override suspend fun initAction(
            pageKey: String,
            initialValue: String?
        ): ApiResponse<PromptResponse> {
            return withContext(Dispatchers.IO) {
                try {
                    val url = if (initialValue != null) {
                        "${getBaseUrl()}hh/$pageKey/init?initialValue=$initialValue"
                    } else {
                        "${getBaseUrl()}hh/$pageKey/init"
                    }

                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val isSuccessful = response.isSuccessful

                    if (isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        val promptResponse =
                            Gson().fromJson(responseBody, PromptResponse::class.java)
                        ApiResponse(true, promptResponse, responseCode)
                    } else {
                        val errorBody = response.body?.string()
                        ApiResponse(false, null, responseCode, errorBody)
                    }
                } catch (e: Exception) {
                    ApiResponse(false, null, 0, e.message)
                }
            }
        }

        override suspend fun processAction(
            pageKey: String,
            processRequest: ProcessRequest
        ): ApiResponse<PromptResponse> {
            return withContext(Dispatchers.IO) {
                try {
                    val jsonBody = Gson().toJson(processRequest)
                    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("${getBaseUrl()}hh/$pageKey/process")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val isSuccessful = response.isSuccessful

                    if (isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        val promptResponse =
                            Gson().fromJson(responseBody, PromptResponse::class.java)
                        ApiResponse(true, promptResponse, responseCode)
                    } else {
                        val errorBody = response.body?.string()
                        ApiResponse(false, null, responseCode, errorBody)
                    }
                } catch (e: Exception) {
                    ApiResponse(false, null, 0, e.message)
                }
            }
        }

        override suspend fun completeAction(pageKey: String): ApiResponse<PromptResponse> {
            return withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("${getBaseUrl()}hh/$pageKey/complete")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val isSuccessful = response.isSuccessful

                    if (isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        val promptResponse =
                            Gson().fromJson(responseBody, PromptResponse::class.java)
                        ApiResponse(true, promptResponse, responseCode)
                    } else {
                        val errorBody = response.body?.string()
                        ApiResponse(false, null, responseCode, errorBody)
                    }
                } catch (e: Exception) {
                    ApiResponse(false, null, 0, e.message)
                }
            }
        }

        override suspend fun updateUserLocation(lat: Double, lon: Double): ApiResponse<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    val url = "${getBaseUrl()}api/UserApi/UpdateGeoLocation?lon=${lon}&lat=${lat}"

                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    val isSuccessful = response.isSuccessful

                    if (isSuccessful) {
                        ApiResponse(true, null, responseCode)
                    } else {
                        val errorBody = response.body?.string()
                        ApiResponse(false, null, responseCode, errorBody)
                    }
                } catch (e: Exception) {
                    ApiResponse(false, null, 0, e.message)
                }
            }
        }

        override suspend fun updateFirebaseToken(request: FirebaseTokenRequest): ApiResponse<MessageResponse> {
            TODO("Not yet implemented")
        }
    }
}
