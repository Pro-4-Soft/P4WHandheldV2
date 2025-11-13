package com.p4handheld.scanner

import android.content.Intent
import android.util.Log
import androidx.core.os.bundleOf
import com.p4handheld.App
import com.p4handheld.data.DataWedgeConstant.ACTION_DATAWEDGE
import com.p4handheld.data.DataWedgeConstant.APPLICATION_EVENT_ACTION
import com.p4handheld.data.DataWedgeConstant.COMMAND_IDENTIFIER
import com.p4handheld.data.DataWedgeConstant.DISABLE_PLUGIN
import com.p4handheld.data.DataWedgeConstant.ENABLE_PLUGIN
import com.p4handheld.data.DataWedgeConstant.EXTRA_CREATE_PROFILE
import com.p4handheld.data.DataWedgeConstant.EXTRA_PROFILE_NAME_1
import com.p4handheld.data.DataWedgeConstant.EXTRA_SET_CONFIG
import com.p4handheld.data.DataWedgeConstant.REGISTER_FOR_NOTIFICATION
import com.p4handheld.data.DataWedgeConstant.SCANNER_INPUT_PLUGIN
import com.p4handheld.data.DataWedgeConstant.UNREGISTER_FOR_NOTIFICATION
import com.p4handheld.data.DataWedgeConstant.VALUE_COMMAND_IDENTIFIER_CREATE_PROFILE
import com.p4handheld.data.DataWedgeConstant.VALUE_COMMAND_IDENTIFIER_SET_CONFIG
import com.p4handheld.data.DataWedgeConstant.dwIntentCallLock
import com.p4handheld.data.models.Symbology

// Manager for handling DataWedge configurations and actions through broadcast intents.
class ConfigurationManager {
    private val TAG = this.javaClass.canonicalName

    // Sends a broadcast intent to the DataWedge API in a thread-safe manner.
    private fun sendBroadcast(intent: Intent) {
        synchronized(dwIntentCallLock) {
            App.getInstance().sendBroadcast(intent)
        }
    }

    // Creates a DataWedge profile based on the specified activity name.
    fun createProfile() {
        val intent = Intent(ACTION_DATAWEDGE).apply {
            putExtra(
                EXTRA_CREATE_PROFILE,
                EXTRA_PROFILE_NAME_1
            )
            putExtra("SEND_RESULT", "true")
            putExtra(COMMAND_IDENTIFIER, VALUE_COMMAND_IDENTIFIER_CREATE_PROFILE)
        }
        sendBroadcast(intent)
    }

    // Updates the configuration for profile 1 with specific symbology and plugin settings.
    fun updateProfile1() {
        Log.d(TAG, "updateProfile -> 1")

        // List of symbologies to enable in the barcode configuration.
        val symbologies = listOf(
            Symbology.CODE_39, Symbology.CODE_128, Symbology.QR_CODE, Symbology.UPCA, Symbology.UPCA
        )

        // Profile configuration bundle for updating DataWedge settings.
        val profileConfig = bundleOf(
            "PROFILE_NAME" to EXTRA_PROFILE_NAME_1,
            "PROFILE_ENABLED" to "true",
            "CONFIG_MODE" to "CREATE_IF_NOT_EXIST",
            "RESET_CONFIG" to "true",
            // Application array specifying which apps use this profile.
            "APP_LIST" to arrayOf(
                bundleOf(
                    "PACKAGE_NAME" to App.getInstance().packageName,
                    "ACTIVITY_LIST" to arrayOf("*"),
                ),
            ),
            // Plugin configuration for barcode, intent, and keystroke plugins.
            "PLUGIN_CONFIG" to listOf(
                bundleOf(
                    "PLUGIN_NAME" to "BARCODE",
                    "RESET_CONFIG" to "true",
                    "PARAM_LIST" to bundleOf(
                        "scanner_selection" to "auto",
                        "scanner_input_enabled" to "true",
                        "configure_all_scanners" to "true",
                        "decoder_code128" to symbologies.contains(Symbology.CODE_128).toString(),
                        "decoder_code39" to symbologies.contains(Symbology.CODE_39).toString(),
                        "decoder_ean13" to symbologies.contains(Symbology.EAN_13).toString(),
                        "decoder_upca" to symbologies.contains(Symbology.UPCA).toString(),
                        "decoder_qrcode" to symbologies.contains(Symbology.QR_CODE).toString(),
                    ),
                ),
                bundleOf(
                    "PLUGIN_NAME" to "INTENT",
                    "RESET_CONFIG" to "true",
                    "PARAM_LIST" to bundleOf(
                        "intent_output_enabled" to "true",
                        "intent_action" to APPLICATION_EVENT_ACTION,
                        "intent_delivery" to 2,
                    ),
                ),
                bundleOf(
                    "PLUGIN_NAME" to "KEYSTROKE",
                    "RESET_CONFIG" to "true",
                    "PARAM_LIST" to bundleOf(
                        "keystroke_output_enabled" to "false",
                    ),
                ),
            ),
        )

        val intent = Intent(ACTION_DATAWEDGE).apply {
            putExtra(EXTRA_SET_CONFIG, profileConfig)
            putExtra("SEND_RESULT", "true")
            putExtra(COMMAND_IDENTIFIER, VALUE_COMMAND_IDENTIFIER_SET_CONFIG)
        }
        sendBroadcast(intent)
    }

    // Registers for specified notifications from DataWedge.
    fun registerForNotifications(list: List<String>) {
        list.forEach {
            val b = bundleOf(
                "com.symbol.datawedge.api.APPLICATION_NAME" to App.getInstance().packageName,
                "com.symbol.datawedge.api.NOTIFICATION_TYPE" to it
            )
            val intent = Intent(ACTION_DATAWEDGE).apply {
                putExtra(REGISTER_FOR_NOTIFICATION, b)
            }
            sendBroadcast(intent)
        }
    }

    // Alternative method: Enable scanner using direct plugin control
    fun enableScannerDirect() {
        Log.d(TAG, "Enabling DataWedge scanner (direct plugin control)")
        Log.d(TAG, "Intent action: $ACTION_DATAWEDGE")
        Log.d(TAG, "Plugin control: $SCANNER_INPUT_PLUGIN = $ENABLE_PLUGIN")

        val intent = Intent(ACTION_DATAWEDGE).apply {
            putExtra(SCANNER_INPUT_PLUGIN, ENABLE_PLUGIN)
            putExtra("SEND_RESULT", "true")
        }
        sendBroadcast(intent)
    }

    // Alternative method: Disable scanner using direct plugin control
    fun disableScannerDirect() {
        Log.d(TAG, "Disabling DataWedge scanner (direct plugin control)")
        Log.d(TAG, "Intent action: $ACTION_DATAWEDGE")
        Log.d(TAG, "Plugin control: $SCANNER_INPUT_PLUGIN = $DISABLE_PLUGIN")

        val intent = Intent(ACTION_DATAWEDGE).apply {
            putExtra(SCANNER_INPUT_PLUGIN, DISABLE_PLUGIN)
            putExtra("SEND_RESULT", "true")
        }
        sendBroadcast(intent)
    }

    // Unregisters from specified notifications from DataWedge.
    fun unregisterForNotifications(list: List<String>) {
        list.forEach {
            val b = bundleOf(
                "com.symbol.datawedge.api.APPLICATION_NAME" to App.getInstance().packageName,
                "com.symbol.datawedge.api.NOTIFICATION_TYPE" to it
            )
            val intent = Intent(ACTION_DATAWEDGE).apply {
                putExtra(UNREGISTER_FOR_NOTIFICATION, b)
            }
            sendBroadcast(intent)
        }
    }
}
