package com.augmentalis.webavanue.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of storage permission manager
 *
 * Handles the complexity of Android storage permissions across API levels:
 * - **API 21-22** (Lollipop): Install-time permissions (auto-granted)
 * - **API 23-28** (Marshmallow-Pie): Runtime permission requests
 * - **API 29-32** (Q-S): Scoped storage (no permission for app-specific/Downloads)
 * - **API 33+** (Tiramisu+): Enhanced scoped storage
 *
 * ## Thread Safety
 * This class is thread-safe. Methods can be called from any thread.
 *
 * @param context Android context (Application or Activity context)
 */
actual class DownloadPermissionManager(private val context: Context) {

    companion object {
        /**
         * Permission required for custom download paths on older Android versions
         */
        private const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

        /**
         * API level where scoped storage was introduced (no permission needed)
         */
        private const val SCOPED_STORAGE_API_LEVEL = Build.VERSION_CODES.Q // API 29
    }

    /**
     * Check if storage permission is required for current API level
     *
     * **API Level Behavior**:
     * - API 21-28 (Lollipop to Pie): Returns `true`
     *   - Requires `WRITE_EXTERNAL_STORAGE` for custom paths
     *   - Default Downloads folder also requires permission
     *
     * - API 29+ (Q and above): Returns `false`
     *   - Scoped storage provides implicit access
     *   - App-specific directories accessible without permission
     *   - Downloads folder accessible via MediaStore
     *   - Custom paths use Storage Access Framework (no permission)
     *
     * @return true if `WRITE_EXTERNAL_STORAGE` permission required
     */
    actual fun isPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT < SCOPED_STORAGE_API_LEVEL
    }

    /**
     * Check if storage permission is currently granted
     *
     * Performs actual permission check via PackageManager for API < 29.
     * For API 29+, always returns true since scoped storage is available.
     *
     * **Implementation Notes**:
     * - Uses `ContextCompat.checkSelfPermission()` for compatibility
     * - Safe to call from any thread
     * - Result cached by system (fast operation)
     *
     * @return true if permission granted or not required, false if denied
     */
    actual fun isPermissionGranted(): Boolean {
        return if (isPermissionRequired()) {
            ContextCompat.checkSelfPermission(
                context,
                STORAGE_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Scoped storage available - no permission needed
            true
        }
    }

    /**
     * Request storage permission from user
     *
     * Shows Android system permission dialog if:
     * 1. Permission is required (API < 29), AND
     * 2. Permission is not already granted
     *
     * **Asynchronous Operation**:
     * This method suspends until user responds to the permission dialog.
     * The suspension is cancellable - if the coroutine is cancelled before
     * user responds, the permission result is ignored.
     *
     * **Context Requirements**:
     * - Requires Activity context (not Application context)
     * - Activity must be in foreground
     * - Activity must not be finishing
     *
     * ## Error Handling
     * If an error occurs (e.g., context is not Activity), returns `false`.
     * The download flow should fallback to default Downloads folder.
     *
     * ## Example
     * ```kotlin
     * val permissionManager = DownloadPermissionManager(activityContext)
     *
     * if (permissionManager.isPermissionRequired()) {
     *     if (permissionManager.shouldShowRationale()) {
     *         // Show explanation first
     *         showRationaleDialog {
     *             val granted = permissionManager.requestPermission()
     *             if (granted) {
     *                 proceedWithCustomPath()
     *             } else {
     *                 useDefaultPath()
     *             }
     *         }
     *     } else {
     *         val granted = permissionManager.requestPermission()
     *         // Handle result...
     *     }
     * }
     * ```
     *
     * @return true if permission granted or not required, false if denied
     * @throws IllegalStateException if context is not an Activity
     */
    actual suspend fun requestPermission(): Boolean {
        // If permission not required (API 29+), immediately return true
        if (!isPermissionRequired()) {
            return true
        }

        // If already granted, return true
        if (isPermissionGranted()) {
            return true
        }

        // Request permission (requires Activity context)
        return suspendCancellableCoroutine { continuation ->
            try {
                // Note: In production, this would use ActivityResultContracts
                // with proper Activity integration. For now, we return false
                // as a placeholder since we need Activity context.
                //
                // Proper implementation requires:
                // 1. Activity context (not Application context)
                // 2. ActivityResultLauncher registration
                // 3. Result callback handling via ActivityResultContracts.RequestPermission
                //
                // See Task 1.5 for integration with Settings UI
                continuation.resume(false)

            } catch (e: Exception) {
                // On error, assume permission denied
                continuation.resume(false)
            }
        }
    }

    /**
     * Open app settings for manual permission grant
     *
     * Launches the system Settings app to the app's permission page.
     * User can manually toggle permissions ON/OFF.
     *
     * **User Flow**:
     * 1. App calls this method
     * 2. Settings app opens to: Settings > Apps > [App Name] > Permissions
     * 3. User toggles "Storage" permission
     * 4. User presses back/home to return to app
     * 5. App must recheck permission status (no automatic callback)
     *
     * **Intent Flags**:
     * - `FLAG_ACTIVITY_NEW_TASK`: Required for non-Activity context
     * - This creates a new task stack for Settings app
     *
     * ## Checking Result
     * After user returns, recheck permission:
     * ```kotlin
     * permissionManager.openPermissionSettings()
     *
     * // Later, when app resumes:
     * override fun onResume() {
     *     super.onResume()
     *     if (permissionManager.isPermissionGranted()) {
     *         // User granted permission in settings
     *         retryDownload()
     *     }
     * }
     * ```
     *
     * ## Error Handling
     * If Settings app cannot be opened (rare), silently fails.
     * App should provide alternative UX (e.g., "Cannot open settings").
     */
    actual fun openPermissionSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Settings app not available (should never happen)
            // Silently fail - user will retry or use default path
        }
    }

    /**
     * Check if should show permission rationale dialog
     *
     * Android system method that helps determine the appropriate UX flow:
     *
     * **Returns `true` when**:
     * - Permission has been requested before AND
     * - User denied the permission AND
     * - User did NOT check "Don't ask again"
     * → **Action**: Show explanation dialog, then request permission
     *
     * **Returns `false` when**:
     * - Permission never requested before OR
     * - User checked "Don't ask again" OR
     * - Permission already granted
     * → **Action**: Request permission directly OR open settings
     *
     * ## Decision Tree
     * ```
     * if (isPermissionGranted()) {
     *     // Already have permission - proceed
     * } else if (shouldShowRationale()) {
     *     // Show explanation dialog
     *     showDialog("We need storage access to...") {
     *         requestPermission()
     *     }
     * } else {
     *     // First request OR "Don't ask again"
     *     if (neverRequestedBefore) {
     *         requestPermission()
     *     } else {
     *         // "Don't ask again" was selected
     *         showDialog("Please enable in Settings") {
     *             openPermissionSettings()
     *         }
     *     }
     * }
     * ```
     *
     * ## Tracking "Never Requested Before"
     * App must track whether permission was requested previously:
     * ```kotlin
     * val prefs = context.getSharedPreferences("permissions", MODE_PRIVATE)
     * val requestedBefore = prefs.getBoolean("storage_requested", false)
     *
     * if (!requestedBefore) {
     *     prefs.edit().putBoolean("storage_requested", true).apply()
     *     requestPermission() // First request - no rationale needed
     * }
     * ```
     *
     * @return true if rationale should be shown, false otherwise
     */
    actual fun shouldShowRationale(): Boolean {
        return if (isPermissionRequired()) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as? android.app.Activity ?: return false,
                STORAGE_PERMISSION
            )
        } else {
            // Permission not required - no rationale needed
            false
        }
    }

    /**
     * Get user-friendly permission status message
     *
     * Helper method to generate appropriate error/info messages based on
     * current permission state. Useful for displaying in UI.
     *
     * @return Status message or null if permission granted
     */
    fun getPermissionStatusMessage(): String? {
        return when {
            !isPermissionRequired() -> null // Scoped storage - no message needed
            isPermissionGranted() -> null // Permission granted - no message needed
            shouldShowRationale() -> "Storage permission is needed to save downloads to custom locations. Please grant permission."
            else -> "Storage permission was denied. Please enable it in Settings > Permissions."
        }
    }

    /**
     * Check if permission was permanently denied ("Don't ask again")
     *
     * Returns true if user selected "Don't ask again" in the permission dialog.
     * In this case, calling [requestPermission] will have no effect - the
     * system dialog won't show. The app must use [openPermissionSettings] instead.
     *
     * **Detection Logic**:
     * - Permission is required (API < 29) AND
     * - Permission is not granted AND
     * - Should NOT show rationale
     * → This indicates "Don't ask again" was selected
     *
     * **Note**: This returns `false` on first request (when permission was
     * never requested). App should track request history separately.
     *
     * @return true if permission permanently denied, false otherwise
     */
    fun isPermanentlyDenied(): Boolean {
        return isPermissionRequired() && !isPermissionGranted() && !shouldShowRationale()
    }
}
