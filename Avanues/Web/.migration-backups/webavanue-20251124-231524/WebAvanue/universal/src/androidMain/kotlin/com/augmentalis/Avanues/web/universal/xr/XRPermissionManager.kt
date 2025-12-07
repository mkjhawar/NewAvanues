package com.augmentalis.Avanues.web.universal.xr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Manages runtime permissions for WebXR features.
 * Handles camera permission for AR sessions and sensor access validation.
 *
 * Requirements:
 * - REQ-XR-002: Camera Permission Management
 * - REQ-XR-003: Motion Sensor Access
 *
 * @see <a href="/.ideacode-v2/features/012-add-webxr-support-to-webavanue-browser-to-enable-immersive-ar-vr-web-experiences/spec.md">WebXR Specification</a>
 */
class XRPermissionManager(private val context: Context) {

    companion object {
        /**
         * Request code for camera permission (AR sessions).
         * Used in Activity.onRequestPermissionsResult callback.
         */
        const val REQUEST_CAMERA_PERMISSION = 1001

        /**
         * Request code for all XR-related permissions (batch request).
         */
        const val REQUEST_XR_PERMISSIONS = 1002
    }

    /**
     * Permission state for a specific permission.
     */
    enum class PermissionState {
        GRANTED,
        DENIED,
        DENIED_PERMANENTLY
    }

    /**
     * Result of permission request with detailed state information.
     */
    data class PermissionResult(
        val permission: String,
        val state: PermissionState,
        val shouldShowRationale: Boolean = false
    )

    /**
     * Checks if camera permission is granted.
     * Required for AR (immersive-ar) sessions.
     *
     * @return true if camera permission is granted, false otherwise
     */
    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if all required sensors for VR/AR are available.
     * WebXR requires accelerometer and gyroscope for head tracking.
     *
     * @return true if required sensors are available
     */
    fun areRequiredSensorsAvailable(): Boolean {
        val packageManager = context.packageManager

        val hasAccelerometer = packageManager.hasSystemFeature(
            PackageManager.FEATURE_SENSOR_ACCELEROMETER
        )
        val hasGyroscope = packageManager.hasSystemFeature(
            PackageManager.FEATURE_SENSOR_GYROSCOPE
        )

        return hasAccelerometer && hasGyroscope
    }

    /**
     * Checks if optional sensors (magnetometer for compass) are available.
     *
     * @return true if magnetometer is available
     */
    fun hasMagnetometer(): Boolean {
        return context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_SENSOR_COMPASS
        )
    }

    /**
     * Checks if device supports OpenGL ES 3.0+ (required for WebGL 2.0).
     *
     * @return true if OpenGL ES 3.0 or higher is supported
     */
    fun isOpenGLES3Supported(): Boolean {
        val packageManager = context.packageManager
        val configurationInfo = packageManager.systemAvailableFeatures

        // Check for OpenGL ES 3.0 (0x00030000) or higher
        return configurationInfo.any { featureInfo ->
            featureInfo.name == null && featureInfo.reqGlEsVersion >= 0x00030000
        }
    }

    /**
     * Gets the current state of camera permission with rationale info.
     *
     * @param activity Activity context for checking shouldShowRequestPermissionRationale
     * @return PermissionResult with detailed state information
     */
    fun getCameraPermissionState(activity: Activity): PermissionResult {
        val isGranted = isCameraPermissionGranted()

        return if (isGranted) {
            PermissionResult(
                permission = Manifest.permission.CAMERA,
                state = PermissionState.GRANTED
            )
        } else {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )

            PermissionResult(
                permission = Manifest.permission.CAMERA,
                state = if (shouldShowRationale) {
                    PermissionState.DENIED
                } else {
                    PermissionState.DENIED_PERMANENTLY
                },
                shouldShowRationale = shouldShowRationale
            )
        }
    }

    /**
     * Requests camera permission for AR sessions.
     * Should be called when user initiates an immersive-ar session.
     *
     * Handle the result in Activity.onRequestPermissionsResult:
     * ```kotlin
     * override fun onRequestPermissionsResult(
     *     requestCode: Int,
     *     permissions: Array<out String>,
     *     grantResults: IntArray
     * ) {
     *     if (requestCode == XRPermissionManager.REQUEST_CAMERA_PERMISSION) {
     *         if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
     *             // Permission granted - start AR session
     *         } else {
     *             // Permission denied - show error to user
     *         }
     *     }
     * }
     * ```
     *
     * @param activity Activity context for requesting permission
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    /**
     * Checks if device meets all hardware requirements for WebXR.
     *
     * @return Pair<Boolean, List<String>> - success flag and list of missing requirements
     */
    fun checkXRCapabilities(): Pair<Boolean, List<String>> {
        val missingCapabilities = mutableListOf<String>()

        if (!areRequiredSensorsAvailable()) {
            missingCapabilities.add("Required sensors (accelerometer and gyroscope) not available")
        }

        if (!isOpenGLES3Supported()) {
            missingCapabilities.add("OpenGL ES 3.0+ not supported (required for WebGL 2.0)")
        }

        val packageManager = context.packageManager
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            missingCapabilities.add("No camera available for AR sessions")
        }

        return Pair(missingCapabilities.isEmpty(), missingCapabilities)
    }

    /**
     * Gets a user-friendly explanation for camera permission request.
     * Shown before requesting permission to improve grant rate.
     *
     * @param sessionMode "immersive-ar" or "immersive-vr"
     * @return User-friendly permission explanation
     */
    fun getCameraPermissionRationale(sessionMode: String): String {
        return when (sessionMode) {
            "immersive-ar" ->
                "Camera access is required to display augmented reality content. " +
                "This allows the website to overlay virtual objects on your real-world view."

            "immersive-vr" ->
                "This VR experience requires camera access for enhanced tracking features."

            else ->
                "Camera access is required for this immersive web experience."
        }
    }

    /**
     * Gets user-friendly error message when permission is denied.
     *
     * @param isPermanentlyDenied true if user selected "Don't ask again"
     * @return User-friendly error message
     */
    fun getPermissionDeniedMessage(isPermanentlyDenied: Boolean): String {
        return if (isPermanentlyDenied) {
            "Camera permission is required for AR experiences. " +
            "Please enable it in Settings > Apps > WebAvanue > Permissions."
        } else {
            "Camera permission is required for AR experiences. " +
            "Please grant permission to continue."
        }
    }
}
