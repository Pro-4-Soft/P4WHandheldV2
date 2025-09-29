package com.p4handheld.data

import androidx.lifecycle.MutableLiveData
import com.p4handheld.data.state.ScanViewState

object ScanStateHolder {
    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: MutableLiveData<Boolean> get() = _isLoading

    private val _profileName = MutableLiveData<String>()
    val profileName: MutableLiveData<String> get() = _profileName

    private val _isDataWedgeReady: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDataWedgeReady: MutableLiveData<Boolean> get() = _isDataWedgeReady

    private val _scanViewStatus = MutableLiveData(ScanViewState())
    val scanViewStatus: MutableLiveData<ScanViewState> get() = _scanViewStatus
}