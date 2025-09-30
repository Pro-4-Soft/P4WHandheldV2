package com.p4handheld.ui.screens.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p4handheld.data.DataWedgeConstant
import com.p4handheld.data.ScanStateHolder
import com.p4handheld.scanner.ConfigurationManager
import com.p4handheld.scanner.QueryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    private val queryManager = QueryManager()
    private val configurationManager = ConfigurationManager()

    val isLoading = ScanStateHolder.isLoading

    val scanViewStatus = ScanStateHolder.scanViewStatus

    fun getStatus() {
        isLoading.value = ScanStateHolder.isDataWedgeReady.value == false
        viewModelScope.launch(Dispatchers.IO) {
            while (ScanStateHolder.isDataWedgeReady.value == false) {
                queryManager.getDataWedgeStatus() // Request DataWedge status.
                delay(DataWedgeConstant.DW_POLLING_DELAY) // Delay between status checks.
            }
        }
    }

    fun createProfile() {
        isLoading.value = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                configurationManager.createProfile()
            }
        }
    }

    fun setConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            configurationManager.updateProfile1() // Update profile settings.
            configurationManager.registerForNotifications(
                arrayListOf(
                    DataWedgeConstant.PROFILE_SWITCH // Notification for profile switching.
                )
            )
        }
    }

    fun unregisterNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            configurationManager.unregisterForNotifications(
                arrayListOf(
                    DataWedgeConstant.PROFILE_SWITCH // Notification for profile switching.
                )
            )
        }
    }
}