package com.p4handheld.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.p4handheld.R
import com.p4handheld.data.api.ApiClient
import com.p4handheld.data.models.P4WEventType
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.firebase.FirebaseManager
import com.p4handheld.scanner.DWCommunicationWrapper
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.navigation.AppNavigation
import com.p4handheld.ui.navigation.Screen
import com.p4handheld.ui.screens.LoadingScreen
import com.p4handheld.ui.screens.viewmodels.LoadingViewModel
import com.p4handheld.ui.screens.viewmodels.MainViewModel
import com.p4handheld.utils.PermissionChecker
import com.p4handheld.workers.LocationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// Main activity that handles UI initialization, observes ViewModel state, and interacts with DataWedge.
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private val loadingViewModel by viewModels<LoadingViewModel>()

    // Flags to track profile creation and initial configuration progression.
    private var isProfileCreated = false
    private var initialConfigInProgression = false

    private var screenRequestReceiverRegistered = false
    private val screenRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val eventType = intent.getStringExtra("eventType") ?: return
            if (eventType != P4WEventType.SCREEN_REQUESTED.name) return
            try {
                captureCurrentScreenJpeg()?.let { bytes ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        ApiClient.apiService.updateScreen(bytes)
                        Log.d("MainActivity", "UpdateScreen result: ${'$'}{res.isSuccessful} code=${'$'}{res.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to capture/upload screenshot", e)
            }
        }
    }

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
                    loadingViewModel = loadingViewModel,
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

        WorkManager.Companion.getInstance(this)
            .enqueueUniquePeriodicWork(
                "LocationWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

        val firebaseManager = FirebaseManager.Companion.getInstance(application)
        firebaseManager.initialize()

        // Initialize Firebase Crashlytics
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true

        // Register screen request receiver
        if (!screenRequestReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                screenRequestReceiver,
                IntentFilter("com.p4handheld.FIREBASE_MESSAGE_RECEIVED"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            screenRequestReceiverRegistered = true
        }
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
        if (screenRequestReceiverRegistered) {
            try {
                unregisterReceiver(screenRequestReceiver)
            } catch (_: Exception) {
            }
            screenRequestReceiverRegistered = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (!initialConfigInProgression)
            viewModel.setConfig()
    }

    private fun getStartDestination(): String {
        val sharedPreferences = getSharedPreferences("tenant_config", MODE_PRIVATE)
        val isConfigured = sharedPreferences.getBoolean("is_configured", false)
        val authPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        authPreferences.edit { putString("menu_json", null) }

        val authRepository = AuthRepository(this)
        val hasValidToken = authRepository.hasValidToken()

        return when {
            !isConfigured -> Screen.TenantSelect.route
            hasValidToken -> Screen.Menu.route
            else -> Screen.Login.route
        }
    }
}

//region Screenshot helpers
private fun ComponentActivity.captureCurrentScreenJpeg(): ByteArray? {
    val view = window?.decorView?.rootView ?: return null
    if (view.width == 0 || view.height == 0) return null
    return try {
        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        val output = ByteArrayOutputStream()
        output.toByteArray()
    } catch (e: Exception) {
        Log.e("MainActivity", "captureCurrentScreenJpeg failed", e)
        null
    }
}
//endregion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivityContent(
    viewModel: MainViewModel,
    loadingViewModel: LoadingViewModel,
    onProfileCreated: () -> Unit,
    onConfigurationComplete: () -> Unit,
    startDestination: String
) {
    val scanViewState by viewModel.scanViewStatus.observeAsState()
    val loadingState by loadingViewModel.uiState.collectAsState()
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

    // Show loading screen or main content
    if (loadingState.isLoading) {
        LoadingScreen(
            loadingText = loadingState.loadingText,
            progress = loadingState.progress,
            showProgress = loadingState.progress > 0f,
            showCustomGif = true
        )
    } else {
        Surface(
            modifier = Modifier.Companion.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                navController = navController,
                startDestination = startDestination
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Location permissions granted
            Toast.makeText(navController.context, "Location permissions granted", Toast.LENGTH_LONG).show()
            Log.d("MainActivity", "Location permissions granted")
        }
    }

    // Request location permissions when needed (after user context is available)
    LaunchedEffect(Unit) {
        // This will be triggered after navigation and login when user context becomes available
        if (PermissionChecker.shouldRequestLocationPermissions(navController.context)) {
            Log.d("MainActivity", "Requesting location permissions based on user context")
            permissionLauncher.launch(PermissionChecker.getLocationPermissions())
        }
    }
}