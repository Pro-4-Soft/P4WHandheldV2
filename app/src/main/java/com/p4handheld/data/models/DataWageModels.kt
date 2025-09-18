package com.p4handheld.data.models

data class DWOutputData(
    val data: String,
    val label: String
)

data class DWProfileUpdate(
    val isProfileUpdated: Boolean
)

data class DWProfileCreate(
    val isProfileCreated: Boolean
)

data class DWProfileSwitch(
    val isSwitchSuccess: Boolean,
    val status: String = ""
)

data class DWScannerState(
    val profileName: String,
    val statusStr: String
)

data class DWScannerSwitch(
    val isSuccess: Boolean
)

data class DWStatus(
    val isEnable: Boolean,
    val statusString: String
)