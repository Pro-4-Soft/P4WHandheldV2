package com.p4handheld.data.api

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.p4handheld.data.models.LoginRequest
import com.p4handheld.data.models.LoginResponse
import com.p4handheld.data.models.MenuItem
import com.p4handheld.data.models.MessageResponse
import com.p4handheld.data.models.ProcessRequest
import com.p4handheld.data.models.PromptResponse
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import com.p4handheld.data.models.UserContextResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object ApiClient {
    private var authToken: String? = null
    private var context: Context? = null

    // This will guard all API calls
    // The alternative is to use global dispatcher per HttpClient Dispatcher
    // but since we have update location and screenshot its not okay to block user with background calls
    private val userRequestMutex = Mutex()

    fun initialize(appContext: Context) {
        context = appContext
    }

    private fun getBaseUrl(): String {
        val sharedPreferences = context?.getSharedPreferences("tenant_config", Context.MODE_PRIVATE)
        //return "v";
        //return "http://10.0.2.2:2020/";
        return sharedPreferences?.getString("base_url", "https:api.p4warehouse.com") ?: "http://costa.pro4soft-demo.com:2020/"
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val authPreferences = context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

                val token = authPreferences?.getString("token", null) ?: authToken

                if (token != null) {
                    requestBuilder.addHeader("Authenticationtoken", token)
                }

                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    val apiService: ApiService = object : ApiService {

        override suspend fun login(
            loginRequest: LoginRequest
        ): ApiResponse<LoginResponse> {
            return withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val jsonBody = JSONObject().apply {
                            put("Username", loginRequest.username)
                            put("Password", loginRequest.password)
                            put("HandheldNotificationToken", loginRequest.handheldNotificationToken)
                        }.toString()

                        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                        val request = Request.Builder()
                            .url("${getBaseUrl()}api/Auth/Login?source=LoginHandheld")
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
        }

        override suspend fun getAssignedTaskCount(userId: String): ApiResponse<Int> {
            userRequestMutex.withLock {
                return withContext(Dispatchers.IO) {
                    try {
                        val filter = "UserId eq $userId"
                        val url = "${getBaseUrl()}odata/UserTask?${'$'}filter=${URLEncoder.encode(filter, "UTF-8")}&${'$'}select=${URLEncoder.encode("UserTaskNumber", "UTF-8")}"
                        val request = Request.Builder()
                            .url(url)
                            .get()
                            .build()

                        val response = client.newCall(request).execute()
                        val responseCode = response.code
                        val isSuccessful = response.isSuccessful
                        if (isSuccessful) {
                            val responseBody = response.body?.string().orEmpty()
                            val count = try {
                                // OData usually wraps in {"value": [...]}
                                val obj = JSONObject(responseBody)
                                val arr = obj.optJSONArray("value") ?: JSONArray()
                                arr.length()
                            } catch (_: Exception) {
                                // Fallback if server returns a raw array
                                try {
                                    JSONArray(responseBody).length()
                                } catch (_: Exception) {
                                    0
                                }
                            }
                            ApiResponse(true, count, responseCode)
                        } else {
                            val errorBody = response.body?.string()
                            ApiResponse(false, null, responseCode, errorBody)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }
        }

        override suspend fun updateScreen(screenshotJpeg: ByteArray): ApiResponse<Unit> {
            userRequestMutex.withLock {
                return withContext(Dispatchers.IO) {
                    try {
                        val mediaType = "image/jpeg".toMediaType()
                        val fileBody: RequestBody = screenshotJpeg.toRequestBody(mediaType)
                        val multipart = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                name = "file",
                                filename = "screenshot.jpg",
                                body = fileBody
                            )
                            .build()

                        val request = Request.Builder()
                            .url("${getBaseUrl()}mobile/userSession/UpdateScreen")
                            .post(multipart)
                            .build()

                        val response = client.newCall(request).execute()
                        val responseCode = response.code
                        val isSuccessful = response.isSuccessful
                        if (isSuccessful) {
                            ApiResponse(true, Unit, responseCode)
                        } else {
                            val errorBody = response.body?.string()
                            ApiResponse(false, null, responseCode, errorBody)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }
        }

        override suspend fun sendMessage(toUserId: String, message: String): ApiResponse<MessageResponse> {
            return withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
                    try {
                        val jsonBody = JSONObject().apply {
                            put("toUserId", toUserId)
                            put("Message", message)
                        }.toString()

                        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

                        val request = Request.Builder()
                            .url("${getBaseUrl()}api/UserMessageApi/SendMessage")
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        val responseCode = response.code
                        val isSuccessful = response.isSuccessful

                        if (isSuccessful) {
                            val responseBody = response.body?.string().orEmpty()
                            val msg = Gson().fromJson(responseBody, MessageResponse::class.java)
                            ApiResponse(true, msg, responseCode)
                        } else {
                            val errorBody = response.body?.string()
                            ApiResponse(false, null, responseCode, errorBody)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }
        }

        override suspend fun getCurrentMenu(): ApiResponse<UserContextResponse> {
            return withContext(Dispatchers.IO) {
                userRequestMutex.withLock {
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

                            val userContextResponse = Gson().fromJson(responseBody, UserContextResponse::class.java)
                            val handheldItem = userContextResponse.menu.firstOrNull { it.id == "Handheld" }
                            val handheldChildrenMenu: List<MenuItem> = handheldItem?.children.orEmpty()

                            val handHeldMenuItems = UserContextResponse(
                                menu = handheldChildrenMenu,
                                trackGeoLocation = userContextResponse.trackGeoLocation,
                                userScanType = userContextResponse.userScanType,
                                tenantScanType = userContextResponse.tenantScanType,
                                userId = userContextResponse.userId
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
        }

        override suspend fun processAction(
            pageKey: String,
            processRequest: ProcessRequest,
            taskId: String?
        ): ApiResponse<PromptResponse> {
            userRequestMutex.withLock {
                return withContext(Dispatchers.IO) {
                    try {
                        val jsonBody = Gson().toJson(processRequest)
                        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                        val pageKeyUnderscore = pageKey.replace('.', '_');

                        val url = buildString {
                            append("${getBaseUrl()}mobile/$pageKeyUnderscore/process")
                            if (!taskId.isNullOrEmpty()) {
                                append("?taskId=$taskId")
                            }
                        }

                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        val responseCode = response.code
                        val isSuccessful = response.isSuccessful

                        if (isSuccessful) {
                            val responseBody = response.body?.string().orEmpty()
                            val promptResponse = Gson().fromJson(responseBody, PromptResponse::class.java)
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

        override suspend fun getContacts(): ApiResponse<List<UserContact>> {
            userRequestMutex.withLock {
                return withContext(Dispatchers.IO) {
                    try {
                        val request = Request.Builder()
                            .url("${getBaseUrl()}api/UserMessageApi/GetContacts")
                            .get()
                            .build()

                        val response = client.newCall(request).execute()
                        val responseCode = response.code
                        val isSuccessful = response.isSuccessful

                        if (isSuccessful) {
                            val responseBody = response.body?.string().orEmpty()
                            val contacts = Gson().fromJson(responseBody, Array<UserContact>::class.java).toList()
                            ApiResponse(true, contacts, responseCode)
                        } else {
                            val errorBody = response.body?.string()
                            ApiResponse(false, null, responseCode, errorBody)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }
        }

        override suspend fun getMessages(contactId: String?, skip: Int, take: Int): ApiResponse<List<UserChatMessage>> {
            userRequestMutex.withLock {
                return withContext(Dispatchers.IO) {
                    try {
                        val url = buildString {
                            append("${getBaseUrl()}api/UserMessageApi/GetMessages")
                            var hasQuery = false
                            if (!contactId.isNullOrEmpty()) {
                                append("?fromUserId=$contactId")
                                hasQuery = true
                            }
                            append(if (hasQuery) "&" else "?")
                            append("Skip=$skip")
                            append("&Take=$take")
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
                            val messages = Gson().fromJson(responseBody, Array<UserChatMessage>::class.java).toList()
                            ApiResponse(true, messages, responseCode)
                        } else {
                            val errorBody = response.body?.string()
                            ApiResponse(false, null, responseCode, errorBody)
                        }
                    } catch (e: Exception) {
                        ApiResponse(false, null, 0, e.message)
                    }
                }
            }
        }
    }
}
