package com.p4handheld.data

object DataWedgeConstant {

    // Lock object for synchronizing DataWedge Intent calls to ensure thread safety.
    val dwIntentCallLock = Any()

    // Custom event action for broadcasting intents within the application.
    const val APPLICATION_EVENT_ACTION = "com.p4handheld.ACTION"

    // Profile names used for configuring DataWedge profiles within the application.
    const val EXTRA_PROFILE_NAME_1 = "MultiActivityProfile_1"

    // Intent actions for DataWedge API operations.
    const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
    const val EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE"
    const val EXTRA_NOTIFICATIONS = "com.symbol.datawedge.api.NOTIFICATION"

    // General DataWedge actions used for communication.
    const val ACTION_DATAWEDGE: String = "com.symbol.datawedge.api.ACTION"
    const val ACTION_RESULT_NOTIFICATION: String = "com.symbol.datawedge.api.NOTIFICATION_ACTION"
    const val ACTION_RESULT: String = "com.symbol.datawedge.api.RESULT_ACTION"

    // Intent actions for registering and unregistering notifications.
    const val REGISTER_FOR_NOTIFICATION = "com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION"
    const val UNREGISTER_FOR_NOTIFICATION = "com.symbol.datawedge.api.UNREGISTER_FOR_NOTIFICATION"

    // Output constants for scanned data.
    const val OUTPUT_DATA_STRING = "com.symbol.datawedge.data_string"
    const val OUTPUT_DATA_LABEL_TYPE = "com.symbol.datawedge.label_type"

    // Intent actions and result actions for querying DataWedge status and enumerating scanners.
    const val EXTRA_GET_DATAWEDGE_STATUS = "com.symbol.datawedge.api.GET_DATAWEDGE_STATUS"
    const val RESULT_GET_DATAWEDGE_STATUS = "com.symbol.datawedge.api.RESULT_GET_DATAWEDGE_STATUS"

    // Command identifiers for specific DataWedge operations.
    const val COMMAND_IDENTIFIER = "COMMAND_IDENTIFIER"
    const val VALUE_COMMAND_IDENTIFIER_SET_CONFIG = "VALUE_COMMAND_IDENTIFIER_SET_CONFIG"
    const val VALUE_COMMAND_IDENTIFIER_CREATE_PROFILE = "COMMAND_IDENTIFIER_CREATE_PROFILE"
    const val VALUE_COMMAND_IDENTIFIER_SWITCH_SCANNER_EX = "COMMAND_IDENTIFIER_SWITCH_SCANNER_EX"
    const val VALUE_COMMAND_IDENTIFIER_SWITCH_PROFILE = "COMMAND_IDENTIFIER_SWITCH_PROFILE"

    // Notification identifiers for scanner status and profile switching events.
    const val PROFILE_SWITCH = "PROFILE_SWITCH"

    // Delay constants for various operations to prevent rapid polling or switching.
    const val DW_POLLING_DELAY = 500L
}
