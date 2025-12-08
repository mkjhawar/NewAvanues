/**
 * ConsentDialogManager.kt - Manages consent dialog lifecycle and state
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/ui/ConsentDialogManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Manages consent dialog showing, hiding, and user response handling
 */

package com.augmentalis.learnapp.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.augmentalis.learnapp.detection.LearnedAppTracker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Consent Dialog Manager
 *
 * Manages lifecycle of consent dialog overlay.
 * Handles showing, hiding, and user responses.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val manager = ConsentDialogManager(context, learnedAppTracker)
 *
 * // Show dialog
 * manager.showConsentDialog("com.instagram.android", "Instagram")
 *
 * // Observe user responses
 * manager.consentResponses.collect { response ->
 *     when (response) {
 *         is ConsentResponse.Approved -> {
 *             // Start exploration
 *             startExploration(response.packageName)
 *         }
 *         is ConsentResponse.Declined -> {
 *             // Do nothing
 *         }
 *     }
 * }
 * ```
 *
 * ## Permissions Required
 *
 * - SYSTEM_ALERT_WINDOW permission for overlay
 *
 * @property context Application context
 * @property learnedAppTracker Tracker for marking apps as dismissed
 *
 * @since 1.0.0
 */
class ConsentDialogManager(
    private val context: Context,
    private val learnedAppTracker: LearnedAppTracker
) {

    /**
     * Coroutine scope for launching suspend functions
     */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Window manager for overlay
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Currently shown dialog view
     */
    private var currentDialogView: ComposeView? = null

    /**
     * Current package name being shown
     */
    private var currentPackageName: String? = null

    /**
     * Shared flow for consent responses
     */
    private val _consentResponses = MutableSharedFlow<ConsentResponse>(replay = 0)
    val consentResponses: SharedFlow<ConsentResponse> = _consentResponses.asSharedFlow()

    /**
     * Dialog visible state
     */
    private val isDialogVisible = mutableStateOf(false)

    /**
     * Show consent dialog
     *
     * Creates overlay dialog asking user permission to learn app.
     *
     * @param packageName Package name of app
     * @param appName Human-readable app name
     */
    suspend fun showConsentDialog(packageName: String, appName: String) {
        // Check if dialog already showing
        if (isDialogVisible.value) {
            hideConsentDialog()
        }

        // Check overlay permission
        if (!hasOverlayPermission()) {
            // Can't show overlay without permission
            // Emit declined response (fallback)
            _consentResponses.emit(
                ConsentResponse.Declined(
                    packageName = packageName,
                    reason = "Overlay permission not granted"
                )
            )
            return
        }

        currentPackageName = packageName

        // Create Compose view
        val composeView = ComposeView(context).apply {
            setContent {
                ConsentDialog(
                    appName = appName,
                    onApprove = { dontAskAgain ->
                        scope.launch {
                            handleApproval(packageName, dontAskAgain)
                        }
                    },
                    onDecline = { dontAskAgain ->
                        scope.launch {
                            handleDeclination(packageName, dontAskAgain)
                        }
                    }
                )
            }
        }

        // Create layout params for overlay
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        // Add view to window
        windowManager.addView(composeView, params)
        currentDialogView = composeView
        isDialogVisible.value = true
    }

    /**
     * Hide consent dialog
     *
     * Removes overlay from screen.
     */
    fun hideConsentDialog() {
        currentDialogView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View already removed or not attached
            }
        }

        currentDialogView = null
        currentPackageName = null
        isDialogVisible.value = false
    }

    /**
     * Handle user approval
     *
     * @param packageName Package name
     * @param dontAskAgain If true, don't show dialog again for this app
     */
    private suspend fun handleApproval(packageName: String, dontAskAgain: Boolean) {
        // Hide dialog
        hideConsentDialog()

        // Mark as learned if "don't ask again" checked
        if (dontAskAgain) {
            learnedAppTracker.markAsLearned(packageName)
        }

        // Emit approved response
        _consentResponses.emit(
            ConsentResponse.Approved(
                packageName = packageName,
                dontAskAgain = dontAskAgain
            )
        )
    }

    /**
     * Handle user declination
     *
     * @param packageName Package name
     * @param dontAskAgain If true, don't show dialog again for this app
     */
    private suspend fun handleDeclination(packageName: String, dontAskAgain: Boolean) {
        // Hide dialog
        hideConsentDialog()

        // Mark as dismissed
        if (dontAskAgain) {
            learnedAppTracker.markAsDismissed(packageName)
        }

        // Emit declined response
        _consentResponses.emit(
            ConsentResponse.Declined(
                packageName = packageName,
                reason = "User declined"
            )
        )
    }

    /**
     * Check if overlay permission is granted
     *
     * @return true if SYSTEM_ALERT_WINDOW permission granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true  // Permission not required on older Android versions
        }
    }

    /**
     * Check if dialog is currently visible
     *
     * @return true if dialog visible
     */
    fun isDialogShowing(): Boolean {
        return isDialogVisible.value
    }

    /**
     * Get current package name (if dialog showing)
     *
     * @return Package name or null
     */
    fun getCurrentPackage(): String? {
        return currentPackageName
    }

    /**
     * Cleanup (call in onDestroy)
     */
    fun cleanup() {
        hideConsentDialog()
    }
}

/**
 * Consent Response
 *
 * Sealed class representing user's response to consent dialog.
 *
 * @since 1.0.0
 */
sealed class ConsentResponse {

    /**
     * User approved learning
     *
     * @property packageName Package name
     * @property dontAskAgain If true, user checked "Don't ask again"
     */
    data class Approved(
        val packageName: String,
        val dontAskAgain: Boolean
    ) : ConsentResponse()

    /**
     * User declined learning
     *
     * @property packageName Package name
     * @property reason Reason for declination
     */
    data class Declined(
        val packageName: String,
        val reason: String
    ) : ConsentResponse()
}
