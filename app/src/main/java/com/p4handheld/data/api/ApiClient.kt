package com.p4handheld.data.api

import android.content.Context
import com.p4handheld.GlobalConstants
import com.p4handheld.GlobalConstants.AppPreferences.TENANT_PREFS
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MessageResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.TranslationRequest
import com.p4handheld.data.models.TranslationResponse
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import com.p4handheld.data.models.UserContextResponse
import com.p4handheld.utils.CrashlyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    // JSON configuration for Kotlinx Serialization
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

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
        val prefs = appContext.getSharedPreferences(TENANT_PREFS, Context.MODE_PRIVATE)
        return prefs.getString("base_tenant_url", "https://app.p4warehouse.com") ?: throw Exception("Tenant url is not retrieved in api client!")
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val authPrefs = appContext.getSharedPreferences(GlobalConstants.AppPreferences.AUTH_PREFS, Context.MODE_PRIVATE)
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
                        val bodyJson = json.encodeToString(loginRequest)
                        val requestBody = bodyJson.toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("${getBaseUrl()}/api/Auth/Login?source=LoginHandheld")
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            val responseBody = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                // For successful login, responseBody is the token
                                authToken = responseBody
                                ApiResponse(true, LoginResponse(true, "Login successful", responseBody), response.code)
                            } else {
                                // For failed login, try to parse error response
                                try {
                                    val errorResponse = json.decodeFromString<LoginResponse>(responseBody)
                                    ApiResponse(false, errorResponse, response.code, errorResponse.message)
                                } catch (e: Exception) {
                                    ApiResponse(false, null, response.code, responseBody)
                                }
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
                            .addFormDataPart("file", "screenshot.png", screenshotJpeg.toRequestBody("image/png".toMediaType()))
                            .build()

                        val request = Request.Builder()
                            .url("${getBaseUrl()}/hh/userSession/UpdateScreen")
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
                        //                        @Serializable
                        data class SendMessageRequest(
                            val toUserId: String,
                            @SerialName("Message") val message: String
                        )

                        val sendMessageRequest = SendMessageRequest(toUserId, message)
                        val jsonBody = json.encodeToString(sendMessageRequest)
                        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url("${getBaseUrl()}/api/UserMessageApi/SendMessage")
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                ApiResponse(true, json.decodeFromString<MessageResponse>(body), response.code)
                            } else {
                                ApiResponse(false, null, response.code, body)
                            }
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun getCurrent(): ApiResponse<UserContextResponse> =
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
                                val userContext = json.decodeFromString<UserContextResponse>(body)
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
                        val jsonBody = json.encodeToString(processRequest)
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
                                ApiResponse(true, json.decodeFromString<PromptResponse>(body), response.code)
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
                                ApiResponse(true, json.decodeFromString<List<UserContact>>(body), response.code)
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
                                ApiResponse(true, json.decodeFromString<List<UserChatMessage>>(body), response.code)
                            else
                                ApiResponse(false, null, response.code, body)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun getTranslations(translationRequest: TranslationRequest): ApiResponse<TranslationResponse> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val url = "${getBaseUrl()}/api/Lang/GetTokens"
                        val jsonBody = json.encodeToString(translationRequest)
                        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                        val request = Request.Builder().url(url).post(requestBody).build()

                        client.newCall(request).execute().use { response ->
                            val body = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                ApiResponse(true, json.decodeFromString<TranslationResponse>(body), response.code)
                            } else {
                                ApiResponse(false, null, response.code, body)
                            }
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }

        override suspend fun preflightCheck(): ApiResponse<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val url = "${getBaseUrl()}/data/loginInfo"

                    val preflightClient = OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(5, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    preflightClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            ApiResponse(true, Unit, response.code)
                        } else {
                            val body = response.body?.string().orEmpty()
                            ApiResponse(false, null, response.code, body.ifEmpty { "Preflight check failed" })
                        }
                    }
                } catch (e: Exception) {
                    CrashlyticsHelper.recordException(
                        e, mapOf(
                            "api_method" to "preflightCheck",
                            "error_type" to e.javaClass.simpleName,
                            "base_url" to (runCatching { getBaseUrl() }.getOrNull() ?: "unknown")
                        )
                    )
                    ApiResponse(false, null, 0, e.message ?: "Network error during preflight check")
                }
            }

        override suspend fun logout(): ApiResponse<Unit> =
            withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val request = Request.Builder()
                            .url("${getBaseUrl()}/api/Auth/Logout")
                            .post("".toRequestBody("application/json".toMediaType()))
                            .build()

                        client.newCall(request).execute().use { response ->
                            val responseBody = response.body?.string().orEmpty()
                            if (response.isSuccessful) {
                                // Clear the auth token after successful logout
                                authToken = null
                                ApiResponse(true, Unit, response.code)
                            } else {
                                ApiResponse(false, null, response.code, responseBody)
                            }
                        }
                    } catch (e: Exception) {
                        CrashlyticsHelper.recordException(
                            e, mapOf(
                                "api_method" to "logout",
                                "error_type" to e.javaClass.simpleName,
                                "base_url" to (runCatching { getBaseUrl() }.getOrNull() ?: "unknown")
                            )
                        )
                        ApiResponse(false, null, 0, e.message ?: "Network error during logout")
                    }
                }
            }
    }
}
