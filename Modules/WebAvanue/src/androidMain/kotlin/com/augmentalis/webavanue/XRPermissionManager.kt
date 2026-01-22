package com.augmentalis.webavanue

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Android implementation of XR permission management.
 *
 * Extends CommonPermissionManager with Android-specific permission APIs.
 */
class XRPermissionManager(private val context: Context) : CommonPermissionManager() {

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 1001
        const val REQUEST_XR_PERMISSIONS = 1002
    }

    override fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun areRequiredSensorsAvailable(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) &&
               pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)
    }

    override fun isOpenGLES3Supported(): Boolean {
        return context.packageManager.systemAvailableFeatures.any { featureInfo ->
            featureInfo.name == null && featureInfo.reqGlEsVersion >= 0x00030000
        }
    }

    override fun hasCamera(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    /**
     * Check if magnetometer is available
     */
    fun hasMagnetometer(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)
    }

    /**
     * Get camera permission state with rationale info
     */
    fun getCameraPermissionState(activity: Activity): PermissionResult {
        val isGranted = isCameraPermissionGranted()

        return if (isGranted) {
            PermissionResult(
                permissionName = Manifest.permission.CAMERA,
                state = PermissionState.GRANTED
            )
        } else {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )

            PermissionResult(
                permissionName = Manifest.permission.CAMERA,
                state = if (shouldShowRationale) PermissionState.DENIED else PermissionState.DENIED_PERMANENTLY,
                shouldShowRationale = shouldShowRationale
            )
        }
    }

    /**
     * Request camera permission for AR sessions
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }
}
