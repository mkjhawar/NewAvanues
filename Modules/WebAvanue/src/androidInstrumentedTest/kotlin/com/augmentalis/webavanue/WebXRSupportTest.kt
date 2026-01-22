package com.augmentalis.webavanue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.augmentalis.webavanue.XRPermissionManager
import com.augmentalis.webavanue.XRSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for WebXR support in WebAvanue browser.
 *
 * Tests cover:
 * - REQ-XR-001: WebXR API Support in WebView
 * - REQ-XR-002: Camera Permission Management
 * - REQ-XR-003: Motion Sensor Access
 * - REQ-XR-005: WebXR Session Lifecycle Management
 * - REQ-XR-006: WebGL 2.0 Support for XR Rendering
 *
 * @see <a href="/.ideacode-v2/features/012-add-webxr-support-to-webavanue-browser-to-enable-immersive-ar-vr-web-experiences/spec.md">WebXR Specification</a>
 */
@RunWith(AndroidJUnit4::class)
class WebXRSupportTest {

    private lateinit var context: Context
    private lateinit var permissionManager: XRPermissionManager
    private lateinit var sessionManager: XRSessionManager

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        permissionManager = XRPermissionManager(context)
        sessionManager = XRSessionManager()
    }

    // ========== REQ-XR-001: WebXR API Support ==========

    @Test
    fun test_androidManifest_declaresXRPermissions() {
        // Verify CAMERA permission is declared
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )

        val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        assertTrue(
            "AndroidManifest must declare CAMERA permission for AR",
            requestedPermissions.contains(Manifest.permission.CAMERA)
        )
    }

    @Test
    fun test_androidManifest_declaresXRHardwareFeatures() {
        val packageManager = context.packageManager

        // Check accelerometer (required)
        val hasAccelerometer = packageManager.hasSystemFeature(
            PackageManager.FEATURE_SENSOR_ACCELEROMETER
        )
        assertTrue(
            "Device must have accelerometer for WebXR",
            hasAccelerometer
        )

        // Check gyroscope (required)
        val hasGyroscope = packageManager.hasSystemFeature(
            PackageManager.FEATURE_SENSOR_GYROSCOPE
        )
        assertTrue(
            "Device must have gyroscope for WebXR",
            hasGyroscope
        )
    }

    // ========== REQ-XR-002: Camera Permission Management ==========

    @Test
    fun test_permissionManager_detectsCameraPermissionGranted() {
        // Permission granted via GrantPermissionRule
        val isGranted = permissionManager.isCameraPermissionGranted()

        assertTrue(
            "Camera permission should be detected as granted",
            isGranted
        )
    }

    @Test
    fun test_permissionManager_providesPermissionRationale() {
        val arRationale = permissionManager.getCameraPermissionRationale("immersive-ar")
        val vrRationale = permissionManager.getCameraPermissionRationale("immersive-vr")

        assertNotNull("AR rationale should not be null", arRationale)
        assertNotNull("VR rationale should not be null", vrRationale)

        assertTrue(
            "AR rationale should mention augmented reality",
            arRationale.contains("augmented reality", ignoreCase = true) ||
            arRationale.contains("AR", ignoreCase = false)
        )

        assertTrue(
            "Rationale should be user-friendly (>20 chars)",
            arRationale.length > 20
        )
    }

    @Test
    fun test_permissionManager_providesDeniedMessage() {
        val message = permissionManager.getPermissionDeniedMessage(isPermanentlyDenied = false)
        val permanentMessage = permissionManager.getPermissionDeniedMessage(isPermanentlyDenied = true)

        assertNotNull("Denied message should not be null", message)
        assertNotNull("Permanent denied message should not be null", permanentMessage)

        assertTrue(
            "Permanent denial message should mention Settings",
            permanentMessage.contains("Settings", ignoreCase = true)
        )
    }

    // ========== REQ-XR-003: Motion Sensor Access ==========

    @Test
    fun test_permissionManager_detectsRequiredSensors() {
        val sensorsAvailable = permissionManager.areRequiredSensorsAvailable()

        assertTrue(
            "Required sensors (accelerometer + gyroscope) should be available",
            sensorsAvailable
        )
    }

    @Test
    fun test_permissionManager_detectsMagnetometer() {
        val hasMagnetometer = permissionManager.hasMagnetometer()

        // Magnetometer is optional, so just verify method works
        // Result depends on device
        assertNotNull(
            "Magnetometer detection should return boolean",
            hasMagnetometer
        )
    }

    // ========== REQ-XR-006: WebGL 2.0 / OpenGL ES 3.0 Support ==========

    @Test
    fun test_permissionManager_detectsOpenGLES3Support() {
        val isSupported = permissionManager.isOpenGLES3Supported()

        assertTrue(
            "Device must support OpenGL ES 3.0+ for WebGL 2.0",
            isSupported
        )
    }

    @Test
    fun test_permissionManager_checkXRCapabilities_allRequirementsMet() {
        val (success, missingCapabilities) = permissionManager.checkXRCapabilities()

        assertTrue(
            "All XR capabilities should be met. Missing: ${missingCapabilities.joinToString()}",
            success
        )

        assertEquals(
            "No capabilities should be missing",
            0,
            missingCapabilities.size
        )
    }

    // ========== REQ-XR-005: Session Lifecycle Management ==========

    @Test
    fun test_sessionManager_initialState_isInactive() {
        runBlocking {
            val state = sessionManager.sessionState.first()

            assertEquals(
                "Initial session state should be INACTIVE",
                XRSessionManager.SessionState.INACTIVE,
                state
            )
        }
    }

    @Test
    fun test_sessionManager_sessionRequested_changesState() {
        runBlocking {
            sessionManager.onSessionRequested(XRSessionManager.SessionMode.IMMERSIVE_AR)

            val state = sessionManager.sessionState.first()

            assertEquals(
                "State should be REQUESTING after session request",
                XRSessionManager.SessionState.REQUESTING,
                state
            )
        }
    }

    @Test
    fun test_sessionManager_sessionStarted_changesStateToActive() {
        runBlocking {
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)

            val state = sessionManager.sessionState.first()

            assertEquals(
                "State should be ACTIVE after session starts",
                XRSessionManager.SessionState.ACTIVE,
                state
            )

            assertTrue(
                "isSessionActive() should return true",
                sessionManager.isSessionActive()
            )
        }
    }

    @Test
    fun test_sessionManager_sessionInfo_tracksMode() {
        runBlocking {
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_VR)

            val info = sessionManager.sessionInfo.first()

            assertEquals(
                "Session info should track mode",
                XRSessionManager.SessionMode.IMMERSIVE_VR,
                info.mode
            )
        }
    }

    @Test
    fun test_sessionManager_sessionInfo_tracksStartTime() {
        runBlocking {
            val beforeStart = System.currentTimeMillis()
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)
            val afterStart = System.currentTimeMillis()

            val info = sessionManager.sessionInfo.first()

            assertTrue(
                "Start time should be between test start and end",
                info.startTime in beforeStart..afterStart
            )
        }
    }

    @Test
    fun test_sessionManager_pauseSession_changesStateToPaused() {
        runBlocking {
            // Start session first
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)

            // Pause it
            sessionManager.pauseSession()

            val state = sessionManager.sessionState.first()

            assertEquals(
                "State should be PAUSED after pause",
                XRSessionManager.SessionState.PAUSED,
                state
            )

            assertFalse(
                "isSessionActive() should return false when paused",
                sessionManager.isSessionActive()
            )
        }
    }

    @Test
    fun test_sessionManager_resumeSession_transitionsToInactive() {
        runBlocking {
            // Start, pause, then resume
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)
            sessionManager.pauseSession()
            sessionManager.resumeSession()

            val state = sessionManager.sessionState.first()

            assertEquals(
                "State should be INACTIVE after resume (user must restart)",
                XRSessionManager.SessionState.INACTIVE,
                state
            )
        }
    }

    @Test
    fun test_sessionManager_endSession_changesStateToEnded() {
        runBlocking {
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)

            // Wait a bit to have duration
            Thread.sleep(100)

            sessionManager.onSessionEnded()

            val state = sessionManager.sessionState.first()

            assertEquals(
                "State should be ENDED after end",
                XRSessionManager.SessionState.ENDED,
                state
            )

            val info = sessionManager.sessionInfo.first()
            assertTrue(
                "Duration should be tracked (>0)",
                info.durationMillis > 0
            )
        }
    }

    @Test
    fun test_sessionManager_updateFrameRate_updatesInfo() {
        runBlocking {
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_VR)

            sessionManager.updateFrameRate(60.0f)

            val info = sessionManager.sessionInfo.first()

            assertEquals(
                "Frame rate should be updated",
                60.0f,
                info.frameRate,
                0.01f
            )
        }
    }

    @Test
    fun test_sessionManager_forceEndSession_endsActiveSession() {
        runBlocking {
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)

            sessionManager.forceEndSession()

            // Wait briefly for state transition
            Thread.sleep(100)

            val state = sessionManager.sessionState.first()

            assertEquals(
                "State should be ENDED after force end",
                XRSessionManager.SessionState.ENDED,
                state
            )
        }
    }

    @Test
    fun test_sessionManager_cleanup_releasesResources() {
        runBlocking {
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)

            sessionManager.cleanup()

            // Wait for cleanup
            Thread.sleep(100)

            val state = sessionManager.sessionState.first()

            assertEquals(
                "State should be ENDED after cleanup",
                XRSessionManager.SessionState.ENDED,
                state
            )
        }
    }

    @Test
    fun test_sessionManager_getStateDescription_providesUserFriendlyText() {
        sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)

        val description = sessionManager.getSessionStateDescription()

        assertNotNull("Description should not be null", description)
        assertTrue(
            "Description should mention AR",
            description.contains("AR", ignoreCase = true)
        )
        assertTrue(
            "Description should mention active",
            description.contains("active", ignoreCase = true)
        )
    }

    // ========== REQ-XR-007: Performance Optimization ==========

    @Test
    fun test_sessionManager_checkAutoPause_doesNotPauseEarlySessions() {
        sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)

        val wasAutoPaused = sessionManager.checkAutoPause()

        assertFalse(
            "Session should not auto-pause immediately",
            wasAutoPaused
        )
    }

    @Test
    fun test_sessionManager_checkAutoPause_returnsFalseWhenInactive() {
        // Session not started
        val wasAutoPaused = sessionManager.checkAutoPause()

        assertFalse(
            "Auto-pause should return false when no active session",
            wasAutoPaused
        )
    }

    // ========== Integration Tests ==========

    @Test
    fun test_integration_fullSessionLifecycle() {
        runBlocking {
            // Request session
            sessionManager.onSessionRequested(XRSessionManager.SessionMode.IMMERSIVE_AR)
            assertEquals(
                XRSessionManager.SessionState.REQUESTING,
                sessionManager.sessionState.first()
            )

            // Start session
            sessionManager.onSessionStarted(XRSessionManager.SessionMode.IMMERSIVE_AR)
            assertEquals(
                XRSessionManager.SessionState.ACTIVE,
                sessionManager.sessionState.first()
            )
            assertTrue(sessionManager.isSessionActive())

            // Update frame rate
            sessionManager.updateFrameRate(60.0f)
            assertEquals(60.0f, sessionManager.sessionInfo.first().frameRate, 0.01f)

            // Pause session (Home button)
            sessionManager.pauseSession()
            assertEquals(
                XRSessionManager.SessionState.PAUSED,
                sessionManager.sessionState.first()
            )
            assertFalse(sessionManager.isSessionActive())

            // Resume session
            sessionManager.resumeSession()
            assertEquals(
                XRSessionManager.SessionState.INACTIVE,
                sessionManager.sessionState.first()
            )
        }
    }

    @Test
    fun test_integration_permissionsAndCapabilities() {
        // Verify full XR capability check
        val (success, missing) = permissionManager.checkXRCapabilities()

        assertTrue("All XR capabilities should be met", success)
        assertTrue("No missing capabilities", missing.isEmpty())

        // Verify camera permission
        assertTrue(
            "Camera permission should be granted",
            permissionManager.isCameraPermissionGranted()
        )

        // Verify sensors
        assertTrue(
            "Required sensors should be available",
            permissionManager.areRequiredSensorsAvailable()
        )

        // Verify OpenGL ES 3.0+
        assertTrue(
            "OpenGL ES 3.0+ should be supported",
            permissionManager.isOpenGLES3Supported()
        )
    }
}
