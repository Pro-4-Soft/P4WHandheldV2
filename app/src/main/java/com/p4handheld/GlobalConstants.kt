package com.p4handheld

object GlobalConstants {
    const val DEFAULT_BASE_URL = "https://app.p4warehouse.com"
    const val LOCATION_UPDATE_INTERVAL_MS = 60_000L  // 1 minute

    object Intents {
        const val FIREBASE_MESSAGE_RECEIVED = "com.p4handheld.FIREBASE_MESSAGE_RECEIVED"
        const val LOCATION_STATUS_CHANGED = "com.p4handheld.LOCATION_STATUS_CHANGED"
    }
}