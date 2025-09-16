package com.p4handheld.scanner

import android.content.Intent
import android.util.Log
import com.p4handheld.App
import com.p4handheld.data.DataWedgeConstant.ACTION_DATAWEDGE
import com.p4handheld.data.DataWedgeConstant.EXTRA_GET_DATAWEDGE_STATUS
import com.p4handheld.data.DataWedgeConstant.dwIntentCallLock

// Manager for sending broadcast queries to DataWedge to retrieve status and enumerate scanners.
class QueryManager {
    private val TAG = this.javaClass.canonicalName

    fun getDataWedgeStatus() {
        val intent = Intent(ACTION_DATAWEDGE).apply {
            putExtra(EXTRA_GET_DATAWEDGE_STATUS, "")
        }
        synchronized(dwIntentCallLock) {
            Log.d(TAG, "sendBroadcast [start]")
            App.getInstance().sendBroadcast(intent)
            Log.d(TAG, "sendBroadcast [end]")
        }
    }
}
