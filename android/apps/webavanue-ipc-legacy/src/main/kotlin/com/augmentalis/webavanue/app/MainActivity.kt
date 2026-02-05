package com.augmentalis.webavanue.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.augmentalis.webavanue.presentation.BrowserApp
import com.augmentalis.webavanue.ui.screen.theme.initializeThemeSystem
import com.augmentalis.webavanue.security.SecureStorage
import com.augmentalis.webavanue.feature.xr.XRManager

/**
 * MainActivity - Main entry point for WebAvanue browser
 *
 * Features:
 * - Hosts universal KMP BrowserApp with Voyager navigation
 * - Initializes repository with SQLDelight database
 * - Handles edge-to-edge display
 * - Cross-platform UI from universal module
 * - Manages WebView lifecycle (pause/resume) to prevent crashes
 * - Coordinates WebXR functionality via XRManager
 */
class MainActivity : ComponentActivity() {

    private lateinit var xrManager: XRManager

    // Camera permission launcher for WebXR AR sessions
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled by XRManager
        // This is a placeholder - actual integration would notify XRManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme system
        initializeThemeSystem(applicationContext)

        // Initialize NetworkChecker for WiFi-only download enforcement
        com.augmentalis.webavanue.platform.NetworkChecker.initialize(applicationContext)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // FIX: Use singleton repository from Application instead of creating new instance
        // Root cause: Creating new repository each time meant new TabViewModel with empty state
        // Solution: Get repository from WebAvanueApp which persists across app lifecycle
        val app = application as WebAvanueApp
        val repository = app.provideRepository()

        // Initialize secure storage for encrypted credential storage
        val secureStorage = SecureStorage(applicationContext)

        // FIX PERFORMANCE: Use SharedPreferences for synchronous download path access
        // Repository suspend functions are too slow for callback-based APIs
        // SharedPreferences synced from SettingsViewModel when path changes
        val downloadQueue = com.augmentalis.webavanue.feature.download.AndroidDownloadQueue(
            context = applicationContext,
            getDownloadPath = {
                // Read from SharedPreferences (synchronous, fast, no blocking)
                val prefs = applicationContext.getSharedPreferences("webavanue_download", android.content.Context.MODE_PRIVATE)
                prefs.getString("download_path", null)
                    ?: android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ).absolutePath
            }
        )

        // Initialize XR Manager with lifecycle awareness
        xrManager = XRManager(this, lifecycle)

        setContent {
            // Collect XR state for UI
            val xrState by xrManager.xrState.collectAsState()

            // BrowserApp includes AppTheme and Voyager Navigator
            BrowserApp(
                repository = repository,
                secureStorage = secureStorage,
                downloadQueue = downloadQueue,
                xrManager = xrManager,
                xrState = xrState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // FIX: Handle activity lifecycle to prevent WebView crashes
    // WebView needs to be paused when activity goes to background
    override fun onPause() {
        super.onPause()
        // WebView will handle pause internally via DisposableEffect in WebViewContainer
    }

    override fun onResume() {
        super.onResume()
        // WebView will handle resume internally via DisposableEffect in WebViewContainer
    }

    override fun onDestroy() {
        super.onDestroy()
        // No need to null out repository - it's managed by Application
    }

    // FIX: Prevent BadParcelableException when Home button is pressed
    // Voyager Navigator tries to serialize ViewModels which are not Parcelable
    // This prevents the crash by not saving Navigator state
    // TODO: Implement proper state management with Parcelize or AndroidX ViewModel
    override fun onSaveInstanceState(outState: Bundle) {
        // Don't save state to prevent serialization of non-parcelable ViewModels
        // This means navigation state is lost on process death, but prevents crash
        super.onSaveInstanceState(Bundle())
    }

    /**
     * Request camera permission for WebXR AR sessions.
     * Called by WebView when AR session is requested.
     */
    fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale in UI (handled by XRPermissionDialog)
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Request permission directly
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Check if camera permission is granted.
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
