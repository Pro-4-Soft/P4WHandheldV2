package com.p4handheld.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.p4handheld.data.repository.AuthRepository

object PermissionChecker {
    private const val TAG = "LocationPermissionHelper"

    fun shouldRequestLocationPermissions(context: Context): Boolean {
        val authRepository = AuthRepository(context)
        val shouldTrack = authRepository.shouldTrackLocation()
        val hasPermissions = hasLocationPermissions(context)

        Log.d(TAG, "Should track location: $shouldTrack, Has permissions: $hasPermissions")

        return shouldTrack && !hasPermissions
    }

    fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
