package com.p4handheld.data

/**
 * Singleton to track the current active chat screen state
 * Used to suppress notifications when user is viewing messages from a specific contact
 */
object ChatStateManager {
    private var currentActiveContactId: String? = null
    private var isChatScreenActive: Boolean = false

    fun setActiveChatContact(contactId: String?) {
        currentActiveContactId = contactId
        isChatScreenActive = contactId != null
    }

    fun clearActiveChatContact() {
        currentActiveContactId = null
        isChatScreenActive = false
    }

    fun isViewingChatWith(contactId: String?): Boolean {
        return isChatScreenActive && currentActiveContactId == contactId
    }
}
