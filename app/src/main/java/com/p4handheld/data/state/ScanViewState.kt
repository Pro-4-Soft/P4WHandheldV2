package com.p4handheld.data.state

import com.p4handheld.data.models.DWOutputData
import com.p4handheld.data.models.DWProfileCreate
import com.p4handheld.data.models.DWProfileSwitch
import com.p4handheld.data.models.DWProfileUpdate
import com.p4handheld.data.models.DWScannerState
import com.p4handheld.data.models.DWScannerSwitch
import com.p4handheld.data.models.DWStatus

data class ScanViewState(
    val dwStatus: DWStatus? = null,
    val dwOutputData: DWOutputData? = null,
    val dwProfileCreate: DWProfileCreate? = null,
    val dwProfileUpdate: DWProfileUpdate? = null,
    val dwScannerState: DWScannerState? = null,
    val dwScannerSwitch: DWScannerSwitch? = null,
    val dwProfileSwitch: DWProfileSwitch? = null,
)