package com.p4handheld.ui.main

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.p4handheld.R
import com.p4handheld.scanner.DWCommunicationWrapper
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.navigation.AppNavigation
import com.p4handheld.utils.LocationPermissionHelper
import com.p4handheld.workers.LocationWorker
import java.util.concurrent.TimeUnit

// Main activity that handles UI initialization, observes ViewModel state, and interacts with DataWedge.
class MainActivity : ComponentActivity() {

    // ViewModel instance scoped to the activity lifecycle.
    private val viewModel by viewModels<MainViewModel>()

    // Flags to track profile creation and initial configuration progression.
    private var isProfileCreated = false
    private var initialConfigInProgression = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mark initial configuration as in progress.
        initialConfigInProgression = true

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Set up Compose UI
        setContent {
            HandheldP4WTheme {
                MainActivityContent(
                    viewModel = viewModel,
                    onProfileCreated = { isProfileCreated = true },
                    onConfigurationComplete = { initialConfigInProgression = false },
                    startDestination = getStartDestination()
                )
            }
        }

        // Register observers to listen for changes in ViewModel state.
        registerObservers()

        // Register broadcast receivers for DataWedge communication.
        DWCommunicationWrapper.registerReceivers()

        // Query DataWedge status to initialize the profile and settings.
        viewModel.getStatus()

        //location worker
        val workRequest = PeriodicWorkRequestBuilder<LocationWorker>(2, TimeUnit.MINUTES) // minimum is 15 min
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "LocationWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }

    // Sets up LiveData observers to update the UI based on ViewModel changes.
    private fun registerObservers() {

        viewModel.scanViewStatus.observe(this) { scanViewState ->
            // Handle profile creation status.
            scanViewState.dwProfileCreate?.let { dwProfileCreate ->
                if (dwProfileCreate.isProfileCreated) {
                    viewModel.setConfig()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.profile_creation_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Handle profile update status.
            scanViewState.dwProfileUpdate?.let { dwProfileUpdate ->
                if (dwProfileUpdate.isProfileUpdated) {
                    initialConfigInProgression = false
                }
            }

            // Handle DataWedge status and create profile if necessary.
            scanViewState.dwStatus?.let { dwStatus ->
                if (dwStatus.isEnable) {
                    if (!isProfileCreated) {
                        viewModel.createProfile()
                        isProfileCreated = true
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.datawedge_is, dwStatus.statusString),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Handle scanned output data - this is now handled in the Compose UI
            scanViewState.dwOutputData?.let { dwOutputData ->
                // The Compose UI will automatically update when this state changes
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receivers and notifications.
        DWCommunicationWrapper.unregisterReceivers()
        viewModel.unregisterNotifications()
    }

    override fun onResume() {
        super.onResume()
        // Set configuration if initial setup is complete.
        if (!initialConfigInProgression) viewModel.setConfig()
    }

    private fun getStartDestination(): String {
        val sharedPreferences = getSharedPreferences("tenant_config", Context.MODE_PRIVATE)
        val isConfigured = sharedPreferences.getBoolean("is_configured", false)

        return if (isConfigured) {
            com.p4handheld.ui.navigation.Screen.Login.route
        } else {
            com.p4handheld.ui.navigation.Screen.TenantSelect.route
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityContent(
    viewModel: MainViewModel,
    onProfileCreated: () -> Unit,
    onConfigurationComplete: () -> Unit,
    startDestination: String
) {
    val scanViewState by viewModel.scanViewStatus.observeAsState()
    val navController = rememberNavController()

    // Handle side effects based on scan view state changes
    DisposableEffect(scanViewState) {
        scanViewState?.dwProfileCreate?.let { dwProfileCreate ->
            if (dwProfileCreate.isProfileCreated) {
                onProfileCreated()
            }
        }

        scanViewState?.dwProfileUpdate?.let { dwProfileUpdate ->
            if (dwProfileUpdate.isProfileUpdated) {
                onConfigurationComplete()
            }
        }

        onDispose { }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavigation(
            navController = navController,
            startDestination = startDestination
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Location permissions granted
            Log.d("MainActivity", "Location permissions granted")
        }
    }

    // Request location permissions when needed (after user context is available)
    LaunchedEffect(Unit) {
        // This will be triggered after navigation and login when user context becomes available
        if (LocationPermissionHelper.shouldRequestLocationPermissions(navController.context)) {
            Log.d("MainActivity", "Requesting location permissions based on user context")
            permissionLauncher.launch(LocationPermissionHelper.getLocationPermissions())
        }
    }
}
