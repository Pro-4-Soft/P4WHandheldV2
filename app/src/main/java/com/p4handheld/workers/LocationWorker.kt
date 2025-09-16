package com.p4handheld.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LocationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "LocationWorker"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(applicationContext)
    
    private val authRepository = AuthRepository(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "LocationWorker started")

            // Check if location tracking is enabled for this user
            if (!authRepository.shouldTrackLocation()) {
                Log.d(TAG, "Location tracking is disabled for this user")
                return@withContext Result.success()
            }

            if (!hasLocationPermissions()) {
                Log.w(TAG, "Location permissions not granted")
                return@withContext Result.failure()
            }

            val location = getCurrentLocation()
            if (location != null) {
                Log.d(TAG, "Location obtained: lat=${location.latitude}, lon=${location.longitude}")

                val response = ApiClient.apiService.updateUserLocation(
                    lat = location.latitude,
                    lon = location.longitude
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Location updated successfully")
                    return@withContext Result.success()
                } else {
                    Log.e(TAG, "Failed to update location: ${response.errorMessage}")
                    return@withContext Result.retry()
                }
            } else {
                Log.w(TAG, "Could not obtain location")
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationWorker", e)
            return@withContext Result.retry()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    //we do call for permission above (hasLocationPermissions)
    // ↑ ☻ so we can suppress warning of linter
    @Suppress("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            null
        }
    }
}
