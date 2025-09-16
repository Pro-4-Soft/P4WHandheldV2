package com.p4handheld.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.DataWedgeConstant.DW_POLLING_DELAY
import com.p4handheld.data.DataWedgeConstant.PROFILE_SWITCH
import com.p4handheld.data.ScanStateHolder
import com.p4handheld.scanner.ConfigurationManager
import com.p4handheld.scanner.QueryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel for MainActivity, responsible for managing DataWedge configurations and status checks.
class MainViewModel : ViewModel() {
    private val queryManager = QueryManager()
    private val configurationManager = ConfigurationManager()

    // LiveData to track the loading state.
    val isLoading = ScanStateHolder.isLoading

    // LiveData to observe the scan view status.
    val scanViewStatus = ScanStateHolder.scanViewStatus

    // Initiates a loop to check DataWedge status until it is ready.
    fun getStatus() {
        isLoading.value = ScanStateHolder.isDataWedgeReady.value == false
        viewModelScope.launch(Dispatchers.IO) {
            while (ScanStateHolder.isDataWedgeReady.value == false) {
                queryManager.getDataWedgeStatus() // Request DataWedge status.
                delay(DW_POLLING_DELAY) // Delay between status checks.
            }
        }
    }

    // Creates a DataWedge profile for the first activity.
    fun createProfile() {
        isLoading.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                configurationManager.createProfile()
            }
        }
    }

    // Sets the configuration for the DataWedge profile and registers for notifications.
    fun setConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            configurationManager.updateProfile1() // Update profile settings.
            configurationManager.registerForNotifications(
                arrayListOf(
                    PROFILE_SWITCH // Notification for profile switching.
                )
            )
        }
    }

    // Unregisters from DataWedge notifications.
    fun unregisterNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            configurationManager.unregisterForNotifications(
                arrayListOf(
                    PROFILE_SWITCH // Notification for profile switching.
                )
            )
        }
    }
}