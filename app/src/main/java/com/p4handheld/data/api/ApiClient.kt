package com.p4handheld.data.api

import android.content.Context
import com.google.gson.Gson
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MessageResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import com.p4handheld.data.models.UserContextResponse
import com.p4handheld.utils.CrashlyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ApiClient {

    private var authToken: String? = null
    private lateinit var appContext: Context

    // Guards API calls to avoid overlapping user-initiated network operations
    private val userRequestMutex = Mutex()

    // Helper function to handle API exceptions with Crashlytics reporting
    private fun handleApiException(e: Exception): ApiResponse<Nothing> {
        CrashlyticsHelper.recordException(
            e, mapOf(
                "api_method" to "login",
                "error_type" to e.javaClass.simpleName,
                "base_url" to (runCatching { getBaseUrl() }.getOrNull() ?: "unknown")
            )
        )
        return ApiResponse(false, null, 0, e.message)
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getBaseUrl(): String {
        val prefs = appContext.getSharedPreferences("tenant_config", Context.MODE_PRIVATE)
        return prefs.getString("base_tenant_url", "https://app.p4warehouse.com") ?: throw Exception("Tenant url is not retrieved in api client!")
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val authPrefs = appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val token = authPrefs.getString("token", null) ?: authToken
                token?.let {
                    requestBuilder.addHeader("Authenticationtoken", it)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    val apiService: ApiService = object : ApiService {
        override suspend fun login(loginRequest: LoginRequest): ApiResponse<LoginResponse> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val bodyJson = JSONObject().apply {
                            put("Username", loginRequest.username)
                            put("Password", loginRequest.password)
                            put("HandheldNotificationToken", loginRequest.handheldNotificationToken)
                        }.toString()

                        val requestBody = bodyJson.toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("${getBaseUrl()}/api/Auth/Login?source=LoginHandheld")
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            val responseBody = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                authToken = responseBody
                                ApiResponse(true, LoginResponse(true, "Good", responseBody), response.code)
                            } else {
                                ApiResponse(false, null, response.code, responseBody)
                            }
                        }
                    } catch (e: Exception) {
                        handleApiException(e)
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun getAssignedTaskCount(userId: String): ApiResponse<Int> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val filter = URLEncoder.encode("UserId eq $userId", "UTF-8")
                        val select = URLEncoder.encode("UserTaskNumber", "UTF-8")
                        val url = "${getBaseUrl()}/odata/UserTask?\$filter=$filter&\$select=$select"

                        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                            val responseBody = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                val count = try {
                                    val obj = JSONObject(responseBody)
                                    val arr = obj.optJSONArray("value") ?: JSONArray()
                                    arr.length()
                                } catch (_: Exception) {
                                    try {
                                        JSONArray(responseBody).length()
                                    } catch (_: Exception) {
                                        0
                                    }
                                }
                                ApiResponse(true, count, response.code)
                            } else {
                                ApiResponse(false, null, response.code, responseBody)
                            }
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun updateScreen(screenshotJpeg: ByteArray): ApiResponse<Unit> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val multipart = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", "screenshot.jpg", screenshotJpeg.toRequestBody("image/jpeg".toMediaType()))
                            .build()

                        val request = Request.Builder()
                            .url("${getBaseUrl()}/mobile/userSession/UpdateScreen")
                            .post(multipart)
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful)
                                ApiResponse(true, Unit, response.code)
                            else
                                ApiResponse(false, null, response.code, response.body?.string())
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun sendMessage(toUserId: String, message: String): ApiResponse<MessageResponse> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val jsonBody = JSONObject().apply {
                            put("toUserId", toUserId)
                            put("Message", message)
                        }.toString()

                        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("${getBaseUrl()}/api/UserMessageApi/SendMessage")
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                ApiResponse(true, Gson().fromJson(body, MessageResponse::class.java), response.code)
                            } else {
                                ApiResponse(false, null, response.code, body)
                            }
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun getCurrentMenu(): ApiResponse<UserContextResponse> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val request = Request.Builder()
                            .url("${getBaseUrl()}/api/Auth/GetCurrent")
                            .get()
                            .build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                val userContext = Gson().fromJson(body, UserContextResponse::class.java)
                                val handheldMenu = userContext.menu.firstOrNull { it.id == "Handheld" }?.children.orEmpty()
                                val result = userContext.copy(menu = handheldMenu)
                                ApiResponse(true, result, response.code)
                            } else {
                                ApiResponse(false, null, response.code, body)
                            }
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun processAction(
            pageKey: String,
            processRequest: ProcessRequest,
            taskId: String?
        ): ApiResponse<PromptResponse> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val jsonBody = Gson().toJson(processRequest)
                        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                        val pageKeyUnderscore = pageKey.replace('.', '_')
                        val url = buildString {
                            append("${getBaseUrl()}/mobile/$pageKeyUnderscore/process")
                            if (!taskId.isNullOrEmpty()) append("?taskId=$taskId")
                        }

                        val request = Request.Builder().url(url).post(requestBody).build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful)
                                ApiResponse(true, Gson().fromJson(body, PromptResponse::class.java), response.code)
                            else
                                ApiResponse(false, null, response.code, body)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun updateUserLocation(lat: Double, lon: Double): ApiResponse<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val url = "${getBaseUrl()}/api/UserApi/UpdateGeoLocation?lon=$lon&lat=$lat"
                    val request = Request.Builder().url(url).get().build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful)
                            ApiResponse(true, Unit, response.code)
                        else
                            ApiResponse(false, null, response.code, response.body?.string())
                    }
                } catch (e: Exception) {
                    ApiResponse(false, null, 0, e.message)
                }
            }

        override suspend fun getContacts(): ApiResponse<List<UserContact>> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val request = Request.Builder()
                            .url("${getBaseUrl()}/api/UserMessageApi/GetContacts")
                            .get()
                            .build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful)
                                ApiResponse(true, Gson().fromJson(body, Array<UserContact>::class.java).toList(), response.code)
                            else
                                ApiResponse(false, null, response.code, body)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun getMessages(contactId: String?, skip: Int, take: Int): ApiResponse<List<UserChatMessage>> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val url = buildString {
                            append("${getBaseUrl()}/api/UserMessageApi/GetMessages")
                            var hasQuery = false
                            if (!contactId.isNullOrEmpty()) {
                                append("?fromUserId=$contactId")
                                hasQuery = true
                            }
                            append(if (hasQuery) "&" else "?")
                            append("Skip=$skip&Take=$take")
                        }

                        val request = Request.Builder().url(url).get().build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful)
                                ApiResponse(true, Gson().fromJson(body, Array<UserChatMessage>::class.java).toList(), response.code)
                            else
                                ApiResponse(false, null, response.code, body)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }
    }
}
