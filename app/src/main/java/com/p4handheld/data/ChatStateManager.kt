package com.p4handheld.data

/**
 * Singleton to track the current active chat screen state
 * Used to suppress notifications when user is viewing messages from a specific contact
 */
object ChatStateManager {
    private var currentActiveContactId: String? = null
    private var isChatScreenActive: Boolean = false
    
    /**
     * Set the current active contact when user enters a chat screen
     */
    fun setActiveChatContact(contactId: String?) {
        currentActiveContactId = contactId
        isChatScreenActive = contactId != null
    }
    
    /**
     * Clear the active chat state when user leaves chat screen
     */
    fun clearActiveChatContact() {
        currentActiveContactId = null
        isChatScreenActive = false
    }
    
    /**
     * Check if user is currently viewing chat with the specified contact
     */
    fun isViewingChatWith(contactId: String?): Boolean {
        return isChatScreenActive && currentActiveContactId == contactId
    }
    
    /**
     * Check if any chat screen is currently active
     */
    fun isChatScreenActive(): Boolean {
        return isChatScreenActive
    }
    
    /**
     * Get the current active contact ID
     */
    fun getCurrentActiveContactId(): String? {
        return currentActiveContactId
    }
}
