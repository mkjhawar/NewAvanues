package com.augmentalis.webavanue.feature.xr

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.augmentalis.webavanue.domain.model.BrowserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AndroidXRManager - Android implementation of CommonXRManager
 *
 * Uses ARCore for XR functionality on Android devices.
 * Coordinates session management, performance monitoring, and permissions.
 *
 * @param context Android Context
 * @param lifecycle Activity lifecycle for automatic cleanup
 */
class AndroidXRManager(
    private val context: Context,
    private val lifecycle: Lifecycle
) : CommonXRManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Component instances
    private val sessionManager = XRSessionManager()
    private val performanceMonitor = XRPerformanceMonitor(context, scope)

    // Combined state flow for UI
    private val _xrState = MutableStateFlow(XRState())
    override val xrState: StateFlow<XRState> = _xrState.asStateFlow()

    // Platform capabilities (ARCore on Android)
    override val capabilities: XRManagerCapabilities = XRManagerCapabilities(
        supportsAR = true,  // Most modern Android devices support ARCore
        supportsVR = false, // Requires specific VR hardware
        supportsWebXR = true,
        supportsHandTracking = false,  // Requires ARCore extensions
        supportsPlaneDetection = true,
        supportsLightEstimation = true,
        supportsDepthSensing = true  // Requires supported device
    )

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
                    Lifecycle.Event.ON_DESTROY -> dispose()
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
                    isSessionActive = (state == SessionState.ACTIVE),
                    sessionMode = when (info.mode) {
                        SessionMode.IMMERSIVE_AR -> "AR"
                        SessionMode.IMMERSIVE_VR -> "VR"
                        else -> null
                    },
                    sessionState = when (state) {
                        SessionState.ACTIVE -> "active"
                        SessionState.PAUSED -> "paused"
                        SessionState.REQUESTING -> "requesting"
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

    override fun updateSettings(settings: BrowserSettings) {
        _xrState.value = _xrState.value.copy(
            isXREnabled = settings.enableWebXR,
            isAREnabled = settings.enableAR,
            isVREnabled = settings.enableVR
        )
    }

    override fun isSupported(mode: SessionMode): Boolean {
        return when (mode) {
            SessionMode.IMMERSIVE_AR -> capabilities.supportsAR
            SessionMode.IMMERSIVE_VR -> capabilities.supportsVR
            SessionMode.INLINE -> true  // Always supported
            SessionMode.UNKNOWN -> false
        }
    }

    override suspend fun requestSession(config: XRSessionConfig): Boolean {
        if (!isSupported(config.mode)) {
            return false
        }

        // Request session via session manager
        sessionManager.onSessionRequested(config.mode)

        // Start performance monitoring
        if (config.mode != SessionMode.INLINE) {
            performanceMonitor.startMonitoring()
        }

        return true
    }

    override suspend fun endSession() {
        sessionManager.forceEndSession()
        performanceMonitor.stopMonitoring()
    }

    override fun pauseSession() {
        sessionManager.pauseSession()
        performanceMonitor.stopMonitoring()
    }

    override fun resumeSession() {
        sessionManager.resumeSession()
        if (_xrState.value.isSessionActive) {
            performanceMonitor.startMonitoring()
        }
    }

    override fun shouldAutoPause(): Boolean {
        return performanceMonitor.shouldAutoPause()
    }

    override fun dispose() {
        performanceMonitor.stopMonitoring()
        scope.cancel()
    }
}
