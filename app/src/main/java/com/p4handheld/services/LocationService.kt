package com.p4handheld.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.p4handheld.GlobalConstants
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.utils.CrashlyticsHelper
import com.p4handheld.utils.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL_MS = GlobalConstants.LOCATION_UPDATE_INTERVAL_MS
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 101

        fun startService(context: Context) {
            val intent = Intent(context, LocationService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private var locationUpdateRunnable: Runnable? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var authRepository: AuthRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        authRepository = AuthRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LocationService started")

        startForegroundNotification()
        startLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationService destroyed")
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        //this is just 'plug' since Android enforce using notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startLocationUpdates() {
        locationUpdateRunnable = object : Runnable {
            override fun run() {
                serviceScope.launch {
                    updateLocation()
                }
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
        handler.post(locationUpdateRunnable!!)
    }

    private fun stopLocationUpdates() {
        locationUpdateRunnable?.let { handler.removeCallbacks(it) }
        locationUpdateRunnable = null
    }

    private suspend fun updateLocation() {
        try {
            Log.d(TAG, "Updating location...")

            if (!authRepository.shouldTrackLocation()) {
                Log.d(TAG, "Location tracking disabled for user")
                broadcastLocationStatus(LocationStatus.DISABLED)
                return
            }

            if (!PermissionChecker.hasLocationPermissions(applicationContext)) {
                Log.w(TAG, "Location permission not granted")
                broadcastLocationStatus(LocationStatus.DISABLED)
                return
            }

            if (!isLocationServicesEnabled()) {
                Log.w(TAG, "Location services disabled")
                broadcastLocationStatus(LocationStatus.UNAVAILABLE)
                return
            }

            val location = getCurrentLocation()
            if (location != null) {
                Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                broadcastLocationStatus(LocationStatus.AVAILABLE)

                val response = ApiClient.apiService.updateUserLocation(
                    lat = location.latitude,
                    lon = location.longitude
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Location updated successfully")
                }
            } else {
                Log.w(TAG, "Could not obtain location")
                broadcastLocationStatus(LocationStatus.UNAVAILABLE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating location", e)
            broadcastLocationStatus(LocationStatus.UNAVAILABLE)
            CrashlyticsHelper.recordException(
                e,
                mapOf(
                    "service_type" to "LocationService",
                    "location_tracking_enabled" to authRepository.shouldTrackLocation().toString(),
                    "has_permissions" to PermissionChecker.hasLocationPermissions(applicationContext).toString()
                )
            )
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            CrashlyticsHelper.recordException(
                e,
                mapOf("operation" to "getCurrentLocation", "service_type" to "LocationService")
            )
            null
        }
    }

    private fun broadcastLocationStatus(status: LocationStatus) {
        val intent = Intent(GlobalConstants.Intents.LOCATION_STATUS_CHANGED).apply {
            putExtra("locationStatus", status.toString())
            setPackage(packageName) // 👈 required on Android 13+ for internal broadcasts
        }
        sendBroadcast(intent)
        Log.d(TAG, "Broadcasted location status: $status")
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

enum class LocationStatus {
    AVAILABLE,
    UNAVAILABLE,
    DISABLED
}
