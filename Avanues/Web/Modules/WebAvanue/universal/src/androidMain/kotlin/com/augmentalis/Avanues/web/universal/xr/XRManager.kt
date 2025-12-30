package com.augmentalis.Avanues.web.universal.xr

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.augmentalis.webavanue.domain.model.BrowserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * XRManager - Central coordinator for all WebXR components.
 *
 * Coordinates:
 * - XRSessionManager (session lifecycle)
 * - XRPermissionManager (camera/sensor permissions)
 * - XRPerformanceMonitor (FPS, battery, thermal)
 * - XRCameraManager (camera lifecycle)
 *
 * Purpose:
 * - Single entry point for XR functionality
 * - Lifecycle-aware (Activity pause/resume/destroy)
 * - State management for UI components
 * - Auto-wiring of component interactions
 *
 * Usage in MainActivity:
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     private lateinit var xrManager: XRManager
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         xrManager = XRManager(this, lifecycle)
 *
 *         setContent {
 *             val xrState by xrManager.xrState.collectAsState()
 *             BrowserApp(xrState = xrState)
 *         }
 *     }
 * }
 * ```
 *
 * @param context Android Context
 * @param lifecycle Activity lifecycle for automatic cleanup
 */
class XRManager(
    private val context: Context,
    private val lifecycle: Lifecycle
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Component instances
    private val sessionManager = XRSessionManager()
    private val performanceMonitor = XRPerformanceMonitor(context, scope)

    // XR State exposed to UI
    data class XRState(
        val isSessionActive: Boolean = false,
        val sessionMode: String? = null,  // "AR", "VR", null
        val sessionState: String = "inactive",  // "active", "paused", "requesting", "inactive"
        val performanceMetrics: XRPerformanceMonitor.PerformanceMetrics = XRPerformanceMonitor.PerformanceMetrics(),
        val warnings: List<XRPerformanceMonitor.PerformanceWarning> = emptyList(),
        val permissionState: String = "UNKNOWN",  // String version of permission state
        val isXREnabled: Boolean = true,
        val isAREnabled: Boolean = true,
        val isVREnabled: Boolean = true
    )

    // Combined state flow for UI
    private val _xrState = MutableStateFlow(XRState())
    val xrState: StateFlow<XRState> = _xrState.asStateFlow()

    init {
        // Observe lifecycle for cleanup
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> performanceMonitor.stopMonitoring()
                    Lifecycle.Event.ON_RESUME -> {
                        if (_xrState.value.isSessionActive) {
                            performanceMonitor.startMonitoring()
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> performanceMonitor.stopMonitoring()
                    else -> {}
                }
            }
        })

        // Combine component states into XRState
        scope.launch {
            combine(
                sessionManager.sessionState,
                sessionManager.sessionInfo,
                performanceMonitor.metrics,
                performanceMonitor.warnings
            ) { state, info, metrics, warnings ->
                XRState(
                    isSessionActive = (state == XRSessionManager.SessionState.ACTIVE),
                    sessionMode = when (info.mode) {
                        XRSessionManager.SessionMode.IMMERSIVE_AR -> "AR"
                        XRSessionManager.SessionMode.IMMERSIVE_VR -> "VR"
                        else -> null
                    },
                    sessionState = when (state) {
                        XRSessionManager.SessionState.ACTIVE -> "active"
                        XRSessionManager.SessionState.PAUSED -> "paused"
                        XRSessionManager.SessionState.REQUESTING -> "requesting"
                        else -> "inactive"
                    },
                    performanceMetrics = metrics,
                    warnings = warnings,
                    permissionState = _xrState.value.permissionState,
                    isXREnabled = _xrState.value.isXREnabled,
                    isAREnabled = _xrState.value.isAREnabled,
                    isVREnabled = _xrState.value.isVREnabled
                )
            }.collect { newState ->
                _xrState.value = newState
            }
        }
    }

    /**
     * Update settings (called when user changes XR preferences).
     *
     * @param settings New browser settings
     */
    fun updateSettings(settings: BrowserSettings) {
        _xrState.value = _xrState.value.copy(
            isXREnabled = settings.enableWebXR,
            isAREnabled = settings.enableAR,
            isVREnabled = settings.enableVR
        )
    }

    /**
     * Check if auto-pause should occur.
     */
    fun shouldAutoPause(): Boolean {
        return performanceMonitor.shouldAutoPause()
    }
}
