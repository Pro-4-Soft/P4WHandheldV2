package com.p4handheld.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.*
import com.p4handheld.data.repositories.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class FirebaseManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "FirebaseManager"
        private const val PREFS_NAME = "firebase_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_SUBSCRIBED_GROUPS = "subscribed_groups"
        private const val KEY_MESSAGES = "stored_messages"
        private const val MAX_STORED_MESSAGES = 100
        
        @Volatile
        private var INSTANCE: FirebaseManager? = null
        
        fun getInstance(context: Context): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val authRepository = AuthRepository(context)
    private val gson = Gson()
    private val messageListeners = ConcurrentHashMap<String, (FirebaseMessage) -> Unit>()
    
    // In-memory cache for messages
    private val messagesCache = mutableListOf<FirebaseMessage>()
    
    init {
        loadStoredMessages()
    }
    
    /**
     * Initialize Firebase messaging and get FCM token
     */
    suspend fun initialize(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token obtained: $token")
            
            // Store token locally
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
            
            // Send token to server if user is authenticated
            if (authRepository.isLoggedIn()) {
                updateTokenOnServer(token)
            }
            
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token", e)
            null
        }
    }
    
    /**
     * Subscribe to a Firebase topic/group
     */
    suspend fun subscribeToGroup(groupId: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(groupId).await()
            Log.d(TAG, "Subscribed to group: $groupId")
            
            // Update local storage
            val subscribedGroups = getSubscribedGroups().toMutableSet()
            subscribedGroups.add(groupId)
            saveSubscribedGroups(subscribedGroups)
            
            // Notify server
            notifyServerSubscription(groupId, true)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to group: $groupId", e)
            false
        }
    }
    
    /**
     * Unsubscribe from a Firebase topic/group
     */
    suspend fun unsubscribeFromGroup(groupId: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(groupId).await()
            Log.d(TAG, "Unsubscribed from group: $groupId")
            
            // Update local storage
            val subscribedGroups = getSubscribedGroups().toMutableSet()
            subscribedGroups.remove(groupId)
            saveSubscribedGroups(subscribedGroups)
            
            // Notify server
            notifyServerSubscription(groupId, false)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from group: $groupId", e)
            false
        }
    }
    
    /**
     * Get messages for a specific group
     */
    suspend fun getGroupMessages(groupId: String, limit: Int = 50, offset: Int = 0): List<FirebaseMessage> {
        return withContext(Dispatchers.IO) {
            try {
                // First try to get from server
                val request = GroupMessagesRequest(
                    groupId = groupId,
                    limit = limit,
                    offset = offset
                )
                
                val response = ApiClient.apiService.getGroupMessages(request)
                if (response.isSuccessful) {
                    val groupMessages = response.body()?.messages ?: emptyList()
                    
                    // Update local cache with server messages
                    groupMessages.forEach { message ->
                        storeMessage(message)
                    }
                    
                    return@withContext groupMessages
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching group messages from server", e)
            }
            
            // Fallback to local cache
            messagesCache.filter { it.groupId == groupId }
                .sortedByDescending { it.timestamp }
                .drop(offset)
                .take(limit)
        }
    }
    
    /**
     * Get all messages (local cache)
     */
    fun getAllMessages(): List<FirebaseMessage> {
        return messagesCache.sortedByDescending { it.timestamp }
    }
    
    /**
     * Store a message locally
     */
    suspend fun storeMessage(message: FirebaseMessage) {
        withContext(Dispatchers.IO) {
            // Add to cache
            val existingIndex = messagesCache.indexOfFirst { it.id == message.id }
            if (existingIndex >= 0) {
                messagesCache[existingIndex] = message
            } else {
                messagesCache.add(0, message)
            }
            
            // Limit cache size
            if (messagesCache.size > MAX_STORED_MESSAGES) {
                messagesCache.removeAt(messagesCache.size - 1)
            }
            
            // Save to persistent storage
            saveMessagesToPrefs()
            
            // Notify listeners
            notifyMessageListeners(message)
        }
    }
    
    /**
     * Mark message as read
     */
    suspend fun markMessageAsRead(messageId: String) {
        withContext(Dispatchers.IO) {
            val messageIndex = messagesCache.indexOfFirst { it.id == messageId }
            if (messageIndex >= 0) {
                messagesCache[messageIndex] = messagesCache[messageIndex].copy(isRead = true)
                saveMessagesToPrefs()
            }
            
            // Notify server
            try {
                ApiClient.apiService.markMessageAsRead(messageId)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking message as read on server", e)
            }
        }
    }
    
    /**
     * Send token to server
     */
    suspend fun updateTokenOnServer(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val userContext = authRepository.getUserContext()
                if (userContext != null) {
                    val request = FirebaseTokenRequest(
                        token = token,
                        userId = userContext.userId,
                        deviceId = getDeviceId(),
                        groups = getSubscribedGroups().toList()
                    )
                    
                    val response = ApiClient.apiService.updateFirebaseToken(request)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Token updated on server successfully")
                    } else {
                        Log.e(TAG, "Failed to update token on server: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating token on server", e)
            }
        }
    }
    
    /**
     * Get available groups from server
     */
    suspend fun getAvailableGroups(): List<FirebaseGroup> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getFirebaseGroups()
                if (response.isSuccessful) {
                    val groups = response.body() ?: emptyList()
                    val subscribedGroups = getSubscribedGroups()
                    
                    // Mark subscribed groups
                    groups.map { group ->
                        group.copy(isSubscribed = subscribedGroups.contains(group.groupId))
                    }
                } else {
                    Log.e(TAG, "Failed to get groups from server: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting groups from server", e)
                emptyList()
            }
        }
    }
    
    /**
     * Add message listener
     */
    fun addMessageListener(key: String, listener: (FirebaseMessage) -> Unit) {
        messageListeners[key] = listener
    }
    
    /**
     * Remove message listener
     */
    fun removeMessageListener(key: String) {
        messageListeners.remove(key)
    }
    
    /**
     * Get subscribed groups
     */
    fun getSubscribedGroups(): Set<String> {
        val groupsJson = prefs.getString(KEY_SUBSCRIBED_GROUPS, "[]")
        return try {
            gson.fromJson(groupsJson, Array<String>::class.java).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * Get current FCM token
     */
    fun getCurrentToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }
    
    // Private helper methods
    
    private fun saveSubscribedGroups(groups: Set<String>) {
        val groupsJson = gson.toJson(groups.toTypedArray())
        prefs.edit().putString(KEY_SUBSCRIBED_GROUPS, groupsJson).apply()
    }
    
    private fun loadStoredMessages() {
        val messagesJson = prefs.getString(KEY_MESSAGES, "[]")
        try {
            val storedMessages = gson.fromJson(messagesJson, Array<StoredFirebaseMessage>::class.java)
            messagesCache.clear()
            messagesCache.addAll(storedMessages.map { it.toFirebaseMessage() })
        } catch (e: Exception) {
            Log.e(TAG, "Error loading stored messages", e)
        }
    }
    
    private fun saveMessagesToPrefs() {
        val storedMessages = messagesCache.map { it.toStoredMessage() }
        val messagesJson = gson.toJson(storedMessages.toTypedArray())
        prefs.edit().putString(KEY_MESSAGES, messagesJson).apply()
    }
    
    private suspend fun notifyServerSubscription(groupId: String, subscribe: Boolean) {
        try {
            val token = getCurrentToken() ?: return
            val request = FirebaseSubscriptionRequest(
                token = token,
                groupId = groupId,
                subscribe = subscribe
            )
            
            ApiClient.apiService.updateGroupSubscription(request)
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying server of subscription change", e)
        }
    }
    
    private fun notifyMessageListeners(message: FirebaseMessage) {
        messageListeners.values.forEach { listener ->
            try {
                listener(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error in message listener", e)
            }
        }
    }
    
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
}
