package com.p4handheld.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.p4handheld.data.models.FirebaseEventSubscriptionRequest
import com.p4handheld.data.models.FirebaseEventType
import com.p4handheld.data.models.FirebaseMessage
import com.p4handheld.data.models.StoredFirebaseMessage
import com.p4handheld.data.models.toStoredMessage
import com.p4handheld.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class FirebaseManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseManager"
        private const val PREFS_NAME = "firebase_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_SUBSCRIBED_EVENTS = "subscribed_events"
        private const val KEY_MESSAGES = "stored_messages"
        private const val MAX_STORED_MESSAGES = 200

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
     * Subscribe to a Firebase event topic
     */
    suspend fun subscribeToEvent(eventType: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(eventType).await()
            Log.d(TAG, "Subscribed to event: $eventType")

            // Update local storage
            val subscribedEvents = getSubscribedEvents().toMutableSet()
            subscribedEvents.add(eventType)
            saveSubscribedEvents(subscribedEvents)

            // Notify server
            notifyServerEventSubscription(eventType, true)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to event: $eventType", e)
            false
        }
    }

    /**
     * Unsubscribe from a Firebase event topic
     */
    suspend fun unsubscribeFromEvent(eventType: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(eventType).await()
            Log.d(TAG, "Unsubscribed from event: $eventType")

            // Update local storage
            val subscribedEvents = getSubscribedEvents().toMutableSet()
            subscribedEvents.remove(eventType)
            saveSubscribedEvents(subscribedEvents)

            // Notify server
            notifyServerEventSubscription(eventType, false)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from event: $eventType", e)
            false
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
     * Send token to server
     */
    suspend fun updateTokenOnServer(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val userContext = authRepository.getUserContext()
                /*  if (userContext != null) {
                      val request = FirebaseTokenRequest(
                          token = token,
                          userId = "userContext.userId",
                          deviceId = getDeviceId(),
                          eventTypes = getSubscribedEvents().toList()
                      )

                      val response = ApiClient.apiService.updateFirebaseToken(request)
                      if (response.isSuccessful) {
                          Log.d(TAG, "Token updated on server successfully")
                      } else {
                          Log.e(TAG, "Failed to update token on server: ${response.code()}")
                      }
                  }*/
            } catch (e: Exception) {
                Log.e(TAG, "Error updating token on server", e)
            }
        }
    }

    /**
     * Get available event types
     */
    suspend fun getAvailableEventTypes(): List<FirebaseEventType> {
        return withContext(Dispatchers.IO) {
            // Define the specific event types you want
            val eventTypes = listOf(
                FirebaseEventType(
                    eventType = "user_chat_message",
                    eventName = "Chat Messages",
                    description = "Receive notifications when users send chat messages"
                ),
                FirebaseEventType(
                    eventType = "tasks_changed",
                    eventName = "Task Updates",
                    description = "Receive notifications when tasks are created, updated, or completed"
                ),
                FirebaseEventType(
                    eventType = "screen_requested",
                    eventName = "Screen Requests",
                    description = "Receive notifications when screen sharing or remote access is requested"
                )
            )

            val subscribedEvents = getSubscribedEvents()

            // Mark subscribed events
            eventTypes.map { eventType ->
                eventType.copy(isSubscribed = subscribedEvents.contains(eventType.eventType))
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
     * Get subscribed events
     */
    fun getSubscribedEvents(): Set<String> {
        val eventsJson = prefs.getString(KEY_SUBSCRIBED_EVENTS, "[]")
        return try {
            gson.fromJson(eventsJson, Array<String>::class.java).toSet()
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

    private fun saveSubscribedEvents(events: Set<String>) {
        val eventsJson = gson.toJson(events.toTypedArray())
        prefs.edit().putString(KEY_SUBSCRIBED_EVENTS, eventsJson).apply()
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

    private suspend fun notifyServerEventSubscription(eventType: String, subscribe: Boolean) {
        try {
            val token = getCurrentToken() ?: return
            val request = FirebaseEventSubscriptionRequest(
                token = token,
                eventType = eventType,
                subscribe = subscribe
            )

            //ApiClient.apiService.updateEventSubscription(request)
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying server of event subscription change", e)
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
