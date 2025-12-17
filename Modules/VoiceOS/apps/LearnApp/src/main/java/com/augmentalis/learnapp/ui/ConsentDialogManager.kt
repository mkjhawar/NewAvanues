/**
 * ConsentDialogManager.kt - Manages consent dialog lifecycle and state
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ConsentDialogManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-10-24 (Migrated from Compose to widgets)
 *
 * Manages consent dialog showing, hiding, and user response handling.
 * Migrated from Compose to widget-based implementation for AccessibilityService compatibility.
 */

package com.augmentalis.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.provider.Settings
import com.augmentalis.learnapp.detection.LearnedAppTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * ## Migration Notes
 *
 * Previously used Compose with custom lifecycle owner (MyLifecycleOwner).
 * Now uses widget-based AlertDialog which works reliably in AccessibilityService context.
 * All Compose dependencies removed.
 *
 * @property context Application context
 * @property learnedAppTracker Tracker for marking apps as dismissed
 *
 * @since 1.0.0
 */
class ConsentDialogManager(
    private val context: AccessibilityService,
    private val learnedAppTracker: LearnedAppTracker
) {

    /**
     * Coroutine scope for launching suspend functions
     */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Widget-based consent dialog (replaces Compose version)
     */
    private val consentDialog = ConsentDialog(context)

    /**
     * Current package name being shown
     */
    private var currentPackageName: String? = null

    /**
     * Session cache of packages with consent decisions
     * Prevents re-prompting for apps where user already decided (this session)
     * FR-011: Prevent consent dialog from showing more than once per app per session
     */
    private val sessionConsentCache = mutableSetOf<String>()

    /**
     * Shared flow for consent responses
     */
    private val _consentResponses = MutableSharedFlow<ConsentResponse>(replay = 0)
    val consentResponses: SharedFlow<ConsentResponse> = _consentResponses.asSharedFlow()

    /**
     * Show consent dialog
     *
     * Displays dialog asking user permission to learn app.
     *
     * FR-003: Dialog must remain stable without flickering
     * FR-011: Prevent showing more than once per app per session
     *
     * @param packageName Package name of app
     * @param appName Human-readable app name
     */
    suspend fun showConsentDialog(packageName: String, appName: String) {
        // Check if user already made a decision for this package this session
        if (sessionConsentCache.contains(packageName)) {
            // Already decided - don't re-prompt (prevents flickering on continuous events)
            return
        }

        // Check if dialog already showing for the SAME package
        if (consentDialog.isShowing() && currentPackageName == packageName) {
            // Already showing for this app - don't hide/re-show (prevents flicker)
            return
        }

        // If showing for DIFFERENT package, hide old dialog first
        if (consentDialog.isShowing()) {
            hideConsentDialog()
        }

        // Check overlay permission
        if (!hasOverlayPermission()) {
            // Can't show overlay without permission
            // Emit declined response (fallback)
            sessionConsentCache.add(packageName)
            _consentResponses.emit(
                ConsentResponse.Declined(
                    packageName = packageName,
                    reason = "Overlay permission not granted"
                )
            )
            return
        }

        currentPackageName = packageName

        // Show widget-based dialog (automatically on main thread)
        withContext(Dispatchers.Main) {
            consentDialog.show(
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

    /**
     * Hide consent dialog
     *
     * Dismisses dialog from screen.
     */
    suspend fun hideConsentDialog() {
        withContext(Dispatchers.Main) {
            consentDialog.dismiss()
            currentPackageName = null
        }
    }

    /**
     * Handle user approval
     *
     * @param packageName Package name
     * @param dontAskAgain If true, don't show dialog again for this app
     */
    private suspend fun handleApproval(packageName: String, dontAskAgain: Boolean) {
        // Add to session cache to prevent re-prompting
        sessionConsentCache.add(packageName)

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
        // Add to session cache to prevent re-prompting
        sessionConsentCache.add(packageName)

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
        return consentDialog.isShowing()
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
     * Clear session consent cache
     *
     * Useful for testing or manual reset scenarios.
     * Note: This doesn't affect persistent "don't ask again" decisions.
     */
    fun clearSessionCache() {
        sessionConsentCache.clear()
    }

    /**
     * Check if package has a consent decision this session
     *
     * @param packageName Package name to check
     * @return true if user already made a decision for this package
     */
    fun hasSessionDecision(packageName: String): Boolean {
        return sessionConsentCache.contains(packageName)
    }

    /**
     * Cleanup (call in onDestroy)
     */
    fun cleanup() {
        scope.launch {
            hideConsentDialog()
        }
        sessionConsentCache.clear()
        scope.cancel()
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
