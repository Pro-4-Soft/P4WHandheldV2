package com.p4handheld.data

import androidx.lifecycle.MutableLiveData

object ApiCallState {
    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: MutableLiveData<Boolean> get() = _isLoading
}